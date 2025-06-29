package pt.ist.cmu.chargist.data.repository

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import pt.ist.cmu.chargist.data.model.*
import pt.ist.cmu.chargist.util.NetworkResult
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import com.google.firebase.firestore.snapshots


class FirestoreChargerRepository(
    private val db: FirebaseFirestore
) : ChargerRepository {

    /* ---------- helpers ---------- */

    private val chargersCol get() = db.collection("chargers")
    private fun chargerDoc(id: String) = chargersCol.document(id)
    private fun slotsCol(chargerId: String) =
        chargerDoc(chargerId).collection("chargingSlots")
    private fun ratingsCol(chargerId: String) =
        chargerDoc(chargerId).collection("ratings")

    /* ---------- live lists ---------- */

    override fun getAllChargers(): Flow<List<Charger>> =
        chargersCol.snapshots().map { it.toObjects(Charger::class.java) }

    override fun getFavoriteChargers(): Flow<List<Charger>> =
        chargersCol.whereEqualTo("isFavorite", true)
            .snapshots()
            .map { it.toObjects(Charger::class.java) }

    override fun getFavoriteChargersForUser(userId: String): Flow<List<Charger>> =
        chargersCol.whereArrayContains("favoriteUsers", userId)
            .snapshots()
            .map { it.toObjects(Charger::class.java) }

    override fun getChargersInBounds(bounds: LatLngBounds): Flow<List<Charger>> =
        chargersCol.snapshots().map { qs ->
            qs.toObjects(Charger::class.java).filter {
                it.latitude  in bounds.southwest.latitude..bounds.northeast.latitude &&
                        it.longitude in bounds.southwest.longitude..bounds.northeast.longitude
            }
        }

    /* ---------- single‑doc helpers ---------- */

    override suspend fun getChargerById(chargerId: String): NetworkResult<Charger> =
        runCatching {
            chargerDoc(chargerId).get().await().toObject(Charger::class.java)
                ?: return NetworkResult.Error("Charger not found")
        }.fold(
            { NetworkResult.Success(it) },
            { NetworkResult.Error(it.message ?: "Fetch failed") }
        )

    /* ---------- create / mutate ---------- */
    override suspend fun createCharger(
        name: String,
        location: LatLng,
        imageData: String?,
        userId: String,
        chargerId: String,
        chargingSlots: List<ChargingSlot>,
        paymentSystems: List<PaymentSystem>
    ): NetworkResult<Charger> {
        val now = System.currentTimeMillis()
        val charger = Charger(
            id = chargerId,
            name = name,
            latitude = location.latitude,
            longitude = location.longitude,
            imageData = imageData,
            createdBy = userId,
            createdAt = now,
            updatedAt = now,
            favoriteUsers = emptyList(),
        )

        return runCatching {
            chargerDoc(chargerId).set(charger).await()

            for (slot in chargingSlots) {
                slotsCol(chargerId).document(slot.id).set(slot).await()
            }

            val paymentSystemsCol = chargerDoc(chargerId).collection("paymentSystems")
            for (paymentSystem in paymentSystems) {
                paymentSystemsCol.document(paymentSystem.id).set(paymentSystem).await()
            }

            NetworkResult.Success(charger)
        }.getOrElse { NetworkResult.Error(it.message ?: "Create failed") }
    }

    override suspend fun getAllChargersSync(): NetworkResult<List<Charger>> = try {
        val chargers = chargersCol.get().await().toObjects(Charger::class.java)
        NetworkResult.Success(chargers)
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "Failed to load chargers")
    }

    override suspend fun addFavorite(userId: String, chargerId: String): NetworkResult<Charger> = runCatching {
        chargerDoc(chargerId).update(
            mapOf(
                "favoriteUsers" to FieldValue.arrayUnion(userId),
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
        getChargerById(chargerId)
    }.getOrElse { NetworkResult.Error(it.message ?: "Add favorite failed") }

    override suspend fun removeFavorite(userId: String, chargerId: String): NetworkResult<Charger> = runCatching {
        chargerDoc(chargerId).update(
            mapOf(
                "favoriteUsers" to FieldValue.arrayRemove(userId),
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
        getChargerById(chargerId)
    }.getOrElse { NetworkResult.Error(it.message ?: "Remove favorite failed") }

    /* ---------- slots ---------- */

    override suspend fun getChargingSlotsForCharger(chargerId: String): List<ChargingSlot> =
        slotsCol(chargerId).get().await().toObjects(ChargingSlot::class.java)

    override suspend fun createChargingSlot(
        chargerId: String,
        speed: ChargingSpeed,
        connectorType: ConnectorType,
        price: Double
    ): NetworkResult<ChargingSlot> = runCatching {
        val slotId = UUID.randomUUID().toString()
        val slot = ChargingSlot(
            id            = slotId,
            chargerId     = chargerId,
            speed         = speed,
            connectorType = connectorType,
            price         = price,
            available   = true,
            damaged     = false,
            updatedAt     = System.currentTimeMillis()
        )
        slotsCol(chargerId).document(slotId).set(slot).await()
        NetworkResult.Success(slot)
    }.getOrElse { NetworkResult.Error(it.message ?: "Slot create failed") }


    // ---------- update charging slot ---------- */
    override suspend fun updateChargingSlot(
        slotId: String,
        speed: ChargingSpeed,
        available: Boolean,
        damaged: Boolean,
        connectorType: ConnectorType?,
        price: Double?
    ): NetworkResult<ChargingSlot> = runCatching {
        val indexOfSeparator = slotId.indexOf("_")
        if (indexOfSeparator == -1) {
            return NetworkResult.Error("Invalid slotId format: expected format <chargerId>_<speed>_<index>")
        }
        val extractedChargerId = slotId.substring(0, indexOfSeparator)

        val slotRef = slotsCol(extractedChargerId).document(slotId)

        val slotSnap = slotRef.get().await()
        val slot = slotSnap.toObject(ChargingSlot::class.java)
            ?: return NetworkResult.Error("Slot not found")

        if (slot.chargerId != extractedChargerId) {
            return NetworkResult.Error("Charger ID mismatch: extracted $extractedChargerId does not match slot's chargerId ${slot.chargerId}")
        }

        val updates = mutableMapOf<String, Any>(
            "speed" to speed,
            "available" to available,
            "damaged" to damaged,
            "updatedAt" to System.currentTimeMillis()
        )
        if (connectorType != null) updates["connectorType"] = connectorType
        if (price != null) updates["price"] = price

        slotRef.update(updates).await()

        val updatedSlot = slotRef.get().await().toObject(ChargingSlot::class.java)
            ?: return NetworkResult.Error("Failed to fetch updated slot")

        NetworkResult.Success(updatedSlot)
    }.getOrElse { NetworkResult.Error(it.message ?: "Update failed") }


    // ---------- report damage ---------- */
    override suspend fun reportDamage(
        slotId: String,
        isDamaged: Boolean,
        speed: ChargingSpeed
    ): NetworkResult<ChargingSlot> =
        updateChargingSlot(slotId, speed = speed,   // keep previous speed
            available = !isDamaged, damaged = isDamaged, connectorType = null, price = null)

    /* ---------- nearby services (stub) ---------- */

    override fun getNearbyServicesForCharger(chargerId: String): Flow<List<NearbyService>> =
        flow { emit(emptyList()) }

    override suspend fun addNearbyService(
        chargerId: String,
        name: String,
        type: String,
        distance: Int
    ) = TODO()

    /* ---------- ratings ---------- */

    override suspend fun addRating(chargerId: String, userId: String, stars: Int): NetworkResult<Rating> = runCatching {
        val now = System.currentTimeMillis()
        val ratingId = "${chargerId}_${userId}"

        val existingRating = ratingsCol(chargerId).document(ratingId).get().await()

        val rating = Rating(
            id = ratingId,
            chargerId = chargerId,
            userId = userId,
            stars = stars,
            createdAt = existingRating.toObject(Rating::class.java)?.createdAt ?: now,
            updatedAt = now
        )

        ratingsCol(chargerId).document(ratingId).set(rating).await()
        NetworkResult.Success(rating)
    }.getOrElse { NetworkResult.Error(it.message ?: "Failed to add rating") }

    override suspend fun getUserRating(chargerId: String, userId: String): NetworkResult<Rating?> = runCatching {
        val ratingId = "${chargerId}_${userId}"
        val doc = ratingsCol(chargerId).document(ratingId).get().await()
        NetworkResult.Success(doc.toObject(Rating::class.java))
    }.getOrElse { NetworkResult.Error(it.message ?: "Failed to get user rating") }

    override suspend fun getRatingStats(chargerId: String): NetworkResult<RatingStats> = runCatching {
        val ratings = ratingsCol(chargerId).get().await().toObjects(Rating::class.java)

        if (ratings.isEmpty()) {
            return NetworkResult.Success(RatingStats())
        }

        val histogram = ratings.groupingBy { it.stars }.eachCount()
        val average = ratings.map { it.stars }.average()

        NetworkResult.Success(RatingStats(
            averageRating = average,
            totalRatings = ratings.size,
            histogram = histogram
        ))
    }.getOrElse { NetworkResult.Error(it.message ?: "Failed to get rating stats") }

    /* ---------- details wrapper ---------- */

    override fun getChargerWithDetails(
        chargerId: String
    ): Flow<NetworkResult<ChargerWithDetails>> = flow {
        emit(NetworkResult.Loading)

        chargerDoc(chargerId).snapshots().collect { snapshot ->
            val charger = snapshot.toObject(Charger::class.java)
                ?: return@collect emit(NetworkResult.Error("Charger not found"))

            slotsCol(chargerId).snapshots().collect { slotsSnapshot ->
                val slots = slotsSnapshot.toObjects(ChargingSlot::class.java)

                val paymentSystems = chargerDoc(chargerId).collection("paymentSystems")
                    .get().await().toObjects(PaymentSystem::class.java)

                // Get rating stats
                val ratingStatsResult = getRatingStats(chargerId)
                val ratingStats = if (ratingStatsResult is NetworkResult.Success) {
                    ratingStatsResult.data
                } else RatingStats()

                // Get user's rating if logged in
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val userRating = if (userId != null) {
                    val userRatingResult = getUserRating(chargerId, userId)
                    if (userRatingResult is NetworkResult.Success) userRatingResult.data else null
                } else null

                emit(NetworkResult.Success(
                    ChargerWithDetails(
                        charger = charger,
                        chargingSlots = slots,
                        nearbyServices = emptyList(),
                        paymentSystems = paymentSystems,
                        ratingStats = ratingStats,
                        userRating = userRating
                    )
                ))
            }
        }
    }

    /* ---------- naive client‑side search ---------- */

    override suspend fun searchChargers(
        query: String?,
        chargingSpeed: ChargingSpeed?,
        isAvailable: Boolean?,
        maxPrice: Double?,
        sortBy: String?,
        paymentSystems: List<PaymentSystem>?,
        userLocation: LatLng?
    ): NetworkResult<List<Charger>> = try {
        var queryRef: Query = chargersCol

        if (!query.isNullOrBlank()) {
            queryRef = queryRef.whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
        }


        val chargers = queryRef.get().await().toObjects(Charger::class.java)
        println("Fetched ${chargers.size} chargers after name query")

        val filteredChargers = chargers.filter { charger ->
            val slots = slotsCol(charger.id).get().await().toObjects(ChargingSlot::class.java)
            println("Charger ${charger.name} has ${slots.size} slots: ${slots.map { it.price }}")

            if (slots.isEmpty()) {
                println("Excluding ${charger.name} due to no slots")
                return@filter false
            }

            val chargerPaymentSystems = chargerDoc(charger.id).collection("paymentSystems")
                .get().await().toObjects(PaymentSystem::class.java)
            println("Charger ${charger.name} has payment systems: ${chargerPaymentSystems.map { it.name }}")

            // Payment Systems filter
            val matchesPaymentSystems = paymentSystems == null || paymentSystems.any { selected ->
                chargerPaymentSystems.any { it.id == selected.id }
            }
            println("Charger ${charger.name} matchesPaymentSystems=$matchesPaymentSystems (paymentSystems=${paymentSystems?.joinToString { it.name }})")

            // Availability filter
            val matchesAvailability = isAvailable == null || slots.any { it.available == isAvailable }
            println("Charger ${charger.name} matchesAvailability=$matchesAvailability (isAvailable=$isAvailable)")

            // Price filter
            val matchesPrice = maxPrice == null || slots.any { slot ->
                val matches = slot.price <= maxPrice
                println("Slot price=${slot.price}, maxPrice=$maxPrice, matches=$matches")
                matches
            }
            println("Charger ${charger.name} matchesPrice=$matchesPrice (maxPrice=$maxPrice)")

            val matchesSpeed = chargingSpeed == null || slots.any { it.speed == chargingSpeed }
            println("Charger ${charger.name} matchesSpeed=$matchesSpeed (chargingSpeed=$chargingSpeed)")

            matchesAvailability && matchesPrice && matchesSpeed && matchesPaymentSystems
        }

        println("Filtered to ${filteredChargers.size} chargers")

        // Apply sorting
        val sortedChargers = when (sortBy?.lowercase()) {
            "distance" -> if (userLocation != null) {
                filteredChargers.sortedBy { charger ->
                    calculateDistance(userLocation, LatLng(charger.latitude, charger.longitude))
                }
            } else filteredChargers
            "travel_time" -> if (userLocation != null) {
                filteredChargers.sortedBy { charger ->
                    estimateTravelTime(
                        calculateDistance(userLocation, LatLng(charger.latitude, charger.longitude))
                    )
                }
            } else filteredChargers
            else -> filteredChargers
        }

        NetworkResult.Success(sortedChargers)
    } catch (t: Throwable) {
        NetworkResult.Error(t.message ?: "Search failed: ${t.message}")
    }

    private fun calculateDistance(from: LatLng, to: LatLng): Double {
        val earthRadius = 6371000.0 // Raio da Terra em metros
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c // Distância em metros
    }

    private fun estimateTravelTime(distanceMeters: Double): Double {
        val speedKmh = 50.0 // Velocidade média em km/h
        val speedMs = speedKmh / 3.6 // Conversão para m/s
        return distanceMeters / speedMs // Tempo em segundos
    }

    /* ---------- find charger by slot id ---------- */

    override suspend fun findChargerBySlotId(slotId: String): NetworkResult<Pair<Charger, ChargingSlot>> = runCatching {
        val indexOfSeparator = slotId.indexOf("_")
        if (indexOfSeparator == -1) {
            return NetworkResult.Error("Invalid slotId format: expected format <chargerId>_<speed>-<index>")
        }
        val extractedChargerId = slotId.substring(0, indexOfSeparator)

        val slotDoc = slotsCol(extractedChargerId).document(slotId).get().await()
        val slot = slotDoc.toObject(ChargingSlot::class.java)
            ?: return NetworkResult.Error("Slot not found")

        if (slot.chargerId != extractedChargerId) {
            return NetworkResult.Error("Charger ID mismatch: extracted $extractedChargerId does not match slot's chargerId ${slot.chargerId}")
        }

        val charger = chargerDoc(extractedChargerId).get().await().toObject(Charger::class.java)
            ?: return NetworkResult.Error("Charger not found")

        NetworkResult.Success(Pair(charger, slot))
    }.getOrElse { NetworkResult.Error(it.message ?: "Failed to find Charger") }
}