package pt.ist.cmu.chargist.data.repository

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import pt.ist.cmu.chargist.data.model.*
import pt.ist.cmu.chargist.util.NetworkResult
import java.util.UUID

class FirestoreChargerRepository(
    private val db: FirebaseFirestore
) : ChargerRepository {

    /* ───────── helpers ───────── */

    private val chargersCol get() = db.collection("chargers")
    private fun chargerDoc(id: String) = chargersCol.document(id)

    /* ───────── live lists ───────── */

    override fun getAllChargers(): Flow<List<Charger>> =
        chargersCol.snapshots()
            .map { qs -> qs.toObjects(Charger::class.java) }

    override fun getFavoriteChargers(): Flow<List<Charger>> =
        chargersCol.whereEqualTo("isFavorite", true)
            .snapshots()
            .map { qs -> qs.toObjects(Charger::class.java) }

    override fun getChargersInBounds(bounds: LatLngBounds): Flow<List<Charger>> =
        chargersCol.snapshots()
            .map { qs ->
                qs.toObjects(Charger::class.java)
                    .filter {
                        it.latitude  in bounds.southwest.latitude..bounds.northeast.latitude &&
                                it.longitude in bounds.southwest.longitude..bounds.northeast.longitude
                    }
            }

    /* ───────── single fetch ───────── */

    override suspend fun getChargerById(chargerId: String): NetworkResult<Charger> =
        runCatching {
            chargerDoc(chargerId).get().await().toObject(Charger::class.java)
                ?: return NetworkResult.Error("Charger not found")
        }.fold(
            onSuccess = { NetworkResult.Success(it) },
            onFailure = { NetworkResult.Error(it.message ?: "Failed to fetch") }
        )

    /* ───────── create ───────── */

    override suspend fun createCharger(
        name: String,
        location: LatLng,
        imageUrl: String?,
        userId: String
    ): NetworkResult<Charger> {
        val id = UUID.randomUUID().toString()
        val charger = Charger(
            id = id,
            name = name,
            latitude = location.latitude,
            longitude = location.longitude,
            imageUrl = imageUrl,
            createdBy = userId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return runCatching {
            chargerDoc(id).set(charger).await()
            NetworkResult.Success(charger)
        }.getOrElse { NetworkResult.Error(it.message ?: "Failed to create") }
    }

    /* ───────── update favourite ───────── */

    override suspend fun updateFavoriteStatus(
        chargerId: String,
        isFavorite: Boolean
    ): NetworkResult<Charger> = runCatching {
        chargerDoc(chargerId).update("isFavorite", isFavorite).await()
        getChargerById(chargerId)          // fetch updated doc
    }.getOrElse { NetworkResult.Error(it.message ?: "Failed to update") }

    /* ───────── slots – live list ───────── */

    override fun getChargingSlotsForCharger(chargerId: String): Flow<List<ChargingSlot>> =
        chargerDoc(chargerId).collection("chargingSlots")
            .snapshots()
            .map { qs -> qs.toObjects(ChargingSlot::class.java) }

    /* ── the four CRUD helpers still TODO – won’t be called for now ───────── */

    override suspend fun createChargingSlot(
        chargerId: String, speed: ChargingSpeed,
        connectorType: ConnectorType, price: Double
    ) = TODO()

    override suspend fun updateChargingSlot(
        slotId: String, speed: ChargingSpeed,
        isAvailable: Boolean, isDamaged: Boolean
    ) = TODO()

    override suspend fun reportDamage(slotId: String, isDamaged: Boolean) =
        TODO()

    /* ───────── nearby services – live list ───────── */

    override fun getNearbyServicesForCharger(chargerId: String): Flow<List<NearbyService>> =
        chargerDoc(chargerId).collection("nearbyServices")
            .snapshots()
            .map { qs -> qs.toObjects(NearbyService::class.java) }

    override suspend fun addNearbyService(
        chargerId: String, name: String, type: String, distance: Int
    ) = TODO()

    /* ───────── getChargerWithDetails (✅  implemented) ───────── */

    override fun getChargerWithDetails(
        chargerId: String
    ): Flow<NetworkResult<ChargerWithDetails>> = flow {
        emit(NetworkResult.Loading)

        try {
            /* 1. fetch the base charger */
            val chargerSnap = chargerDoc(chargerId).get().await()
            val charger = chargerSnap.toObject(Charger::class.java)
                ?: return@flow emit(NetworkResult.Error("Charger not found"))

            /* 2. fetch slots & (optionally) services */
            val slots = chargerDoc(chargerId)
                .collection("chargingSlots")
                .get().await()
                .toObjects(ChargingSlot::class.java)

            // If you’re not ready to use services yet, keep it empty
            val services = emptyList<NearbyService>()

            /* 3. wrap & emit */
            val details = ChargerWithDetails(
                charger         = charger,
                chargingSlots   = slots,
                nearbyServices  = services,
                paymentSystems  = emptyList()     // not stored yet
            )
            emit(NetworkResult.Success(details))

        } catch (t: Throwable) {
            emit(NetworkResult.Error(t.message ?: "Failed to fetch details"))
        }
    }

    /* ───────── very naïve search – client side ───────── */

    override suspend fun searchChargers(
        query: String?,
        chargingSpeed: ChargingSpeed?,
        isAvailable: Boolean?,
        maxPrice: Double?,
        sortBy: String?
    ): NetworkResult<List<Charger>> = try {
        /* fetch all – cheap because we keep only charger docs */
        val chargers = chargersCol.get().await().toObjects(Charger::class.java)

        val filtered = chargers.filter { c ->
            (query.isNullOrBlank() || c.name.contains(query, true))
        }
        NetworkResult.Success(filtered)
    } catch (t: Throwable) {
        NetworkResult.Error(t.message ?: "Search failed")
    }
}
