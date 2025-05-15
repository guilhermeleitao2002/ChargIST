package pt.ist.cmu.chargist.data.repository

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import pt.ist.cmu.chargist.data.model.*
import pt.ist.cmu.chargist.util.NetworkResult
import java.util.UUID

class FirestoreChargerRepository(
    private val db: FirebaseFirestore
) : ChargerRepository {

    /* ---------- helpers ---------- */

    private val chargersCol get() = db.collection("chargers")
    private fun chargerDoc(id: String) = chargersCol.document(id)
    private fun slotsCol(chargerId: String) =
        chargerDoc(chargerId).collection("chargingSlots")

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
    /* only the createCharger function shown – keep the rest of the file unchanged */

    override suspend fun createCharger(
        name: String,
        location: LatLng,
        imageData: String?,
        userId: String,
        chargingSlots: List<ChargingSlot>,
        paymentSystems: List<PaymentSystem>
    ): NetworkResult<Charger> {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val charger = Charger(
            id = id,
            name = name,
            latitude = location.latitude,
            longitude = location.longitude,
            imageData = imageData,
            createdBy = userId,
            createdAt = now,
            updatedAt = now,
            favoriteUsers = emptyList(),
            chargingSlots = chargingSlots
        )

        return runCatching {
            // Create the charger document
            chargerDoc(id).set(charger).await()

            // Create individual slot documents in the subcollection
            for (slot in chargingSlots) {
                slotsCol(id).document(slot.id).set(slot).await()
            }

            // Create payment systems subcollection
            val paymentSystemsCol = chargerDoc(id).collection("paymentSystems")
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

    override suspend fun updateFavoriteStatus(
        chargerId: String,
        isFavorite: Boolean
    ): NetworkResult<Charger> = TODO("Replaced by addFavorite/removeFavorite")

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

    override fun getChargingSlotsForCharger(chargerId: String): Flow<List<ChargingSlot>> =
        slotsCol(chargerId).snapshots().map { it.toObjects(ChargingSlot::class.java) }

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
            isAvailable   = true,
            isDamaged     = false,
            updatedAt     = System.currentTimeMillis()
        )
        slotsCol(chargerId).document(slotId).set(slot).await()
        NetworkResult.Success(slot)
    }.getOrElse { NetworkResult.Error(it.message ?: "Slot create failed") }

    override suspend fun updateChargingSlot(
        slotId: String,
        speed: ChargingSpeed,
        isAvailable: Boolean,
        isDamaged: Boolean
    ): NetworkResult<ChargingSlot> = runCatching {
        // we need chargerId → look it up once
        val slotSnap = db.collectionGroup("chargingSlots")
            .whereEqualTo("id", slotId).limit(1).get().await()
        val doc = slotSnap.documents.firstOrNull()
            ?: return NetworkResult.Error("Slot not found")

        val updates = mapOf(
            "speed"       to speed,
            "isAvailable" to isAvailable,
            "isDamaged"   to isDamaged,
            "updatedAt"   to System.currentTimeMillis()
        )
        doc.reference.update(updates).await()
        NetworkResult.Success(doc.reference.get().await().toObject(ChargingSlot::class.java)!!)
    }.getOrElse { NetworkResult.Error(it.message ?: "Update failed") }

    override suspend fun reportDamage(
        slotId: String,
        isDamaged: Boolean
    ): NetworkResult<ChargingSlot> =
        updateChargingSlot(slotId, speed = ChargingSpeed.SLOW,   // keep previous speed
            isAvailable = !isDamaged, isDamaged = isDamaged)

    /* ---------- nearby services (stub) ---------- */

    override fun getNearbyServicesForCharger(chargerId: String): Flow<List<NearbyService>> =
        flow { emit(emptyList()) }

    override suspend fun addNearbyService(
        chargerId: String,
        name: String,
        type: String,
        distance: Int
    ) = TODO()

    /* ---------- details wrapper ---------- */

    override fun getChargerWithDetails(
        chargerId: String
    ): Flow<NetworkResult<ChargerWithDetails>> = flow {
        emit(NetworkResult.Loading)
        when (val base = getChargerById(chargerId)) {
            is NetworkResult.Error   -> emit(base)
            is NetworkResult.Success -> {
                val slots = getChargingSlotsForCharger(chargerId)
                    .map { it.sortedBy { s -> s.speed } }
                    .first()

                // Fetch payment systems from subcollection
                val paymentSystems = chargerDoc(chargerId).collection("paymentSystems")
                    .get().await().toObjects(PaymentSystem::class.java)

                emit(
                    NetworkResult.Success(
                        ChargerWithDetails(
                            charger        = base.data,
                            chargingSlots  = slots,
                            nearbyServices = emptyList(),
                            paymentSystems = paymentSystems  // Now including payment systems
                        )
                    )
                )
            }
            else -> {}
        }
    }

    /* ---------- naive client‑side search ---------- */

    override suspend fun searchChargers(
        query: String?,
        chargingSpeed: ChargingSpeed?,
        isAvailable: Boolean?,
        maxPrice: Double?,
        sortBy: String?
    ): NetworkResult<List<Charger>> = try {
        // Explicitly declare queryRef as a Query to avoid type mismatch
        var queryRef: Query = chargersCol

        // Apply name query filter
        if (!query.isNullOrBlank()) {
            queryRef = queryRef.whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
        }

        // Fetch chargers
        val chargers = queryRef.get().await().toObjects(Charger::class.java)
        println("Fetched ${chargers.size} chargers after name query")

        // Apply filters involving subcollections client-side
        val filteredChargers = chargers.filter { charger ->
            val slots = slotsCol(charger.id).get().await().toObjects(ChargingSlot::class.java)
            println("Charger ${charger.name} has ${slots.size} slots: ${slots.map { it.price }}")

            // Exclude chargers with no slots
            if (slots.isEmpty()) {
                println("Excluding ${charger.name} due to no slots")
                return@filter false
            }

            // Availability filter
            val matchesAvailability = isAvailable == null || slots.any { it.isAvailable == isAvailable }
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

            matchesAvailability && matchesPrice && matchesSpeed
        }

        println("Filtered to ${filteredChargers.size} chargers")

        // Apply sorting
        val sortedChargers = when (sortBy?.lowercase()) {
            "name" -> filteredChargers.sortedBy { it.name }
            "distance" -> filteredChargers // Requires location-based sorting
            else -> filteredChargers
        }

        NetworkResult.Success(sortedChargers)
    } catch (t: Throwable) {
        NetworkResult.Error(t.message ?: "Search failed: ${t.message}")
    }
}