package pt.ist.cmu.chargist.data.repository

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import pt.ist.cmu.chargist.data.model.*
import pt.ist.cmu.chargist.util.NetworkResult
import java.util.UUID

class FirestoreChargerRepository(
    private val db: FirebaseFirestore
) : ChargerRepository {

    /* ------------ helpers ------------ */

    private val chargersCol get() = db.collection("chargers")
    private fun chargerDoc(id: String) = chargersCol.document(id)

    /* ------------ live flows ------------ */

    override fun getAllChargers(): Flow<List<Charger>> =
        chargersCol.snapshots().map { qs -> qs.documents.mapNotNull { it.toObject(Charger::class.java) } }

    override fun getFavoriteChargers(): Flow<List<Charger>> =
        chargersCol.whereEqualTo("isFavorite", true)
            .snapshots()
            .map { qs -> qs.documents.mapNotNull { it.toObject(Charger::class.java) } }

    override fun getChargersInBounds(bounds: LatLngBounds): Flow<List<Charger>> =
        chargersCol   // Firestore can’t query rectangles; fetch all & filter client‑side
            .snapshots()
            .map { qs ->
                qs.documents.mapNotNull { it.toObject(Charger::class.java) }
                    .filter { it.latitude in bounds.southwest.latitude..bounds.northeast.latitude &&
                            it.longitude in bounds.southwest.longitude..bounds.northeast.longitude }
            }

    /* ------------ single fetch ------------ */

    override suspend fun getChargerById(chargerId: String): NetworkResult<Charger> =
        runCatching {
            chargerDoc(chargerId).get().await().toObject(Charger::class.java)
                ?: return NetworkResult.Error("Charger not found")
        }.fold(
            onSuccess = { NetworkResult.Success(it) },
            onFailure = { NetworkResult.Error(it.message ?: "Failed to fetch") }
        )

    /* ------------ create ------------ */

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
            createdBy = userId
        )
        return runCatching {
            chargerDoc(id).set(charger).await()
            NetworkResult.Success(charger)
        }.getOrElse { NetworkResult.Error(it.message ?: "Failed to create") }
    }

    /* ------------ toggle favourite ------------ */

    override suspend fun updateFavoriteStatus(
        chargerId: String,
        isFavorite: Boolean
    ): NetworkResult<Charger> = runCatching {
        chargerDoc(chargerId).update("isFavorite", isFavorite).await()
        getChargerById(chargerId)          // fetch the updated doc
    }.getOrElse { NetworkResult.Error(it.message ?: "Failed to update") }

    /* ------------ the rest (fill in as needed) ------------ */

    /* Slots */
    override fun getChargingSlotsForCharger(chargerId: String): Flow<List<ChargingSlot>> =
        chargerDoc(chargerId).collection("chargingSlots")
            .snapshots()
            .map { qs -> qs.toObjects(ChargingSlot::class.java) }

    override suspend fun createChargingSlot(
        chargerId: String,
        speed: ChargingSpeed,
        connectorType: ConnectorType,
        price: Double
    ) = TODO("same pattern → add doc to sub‑collection")

    override suspend fun updateChargingSlot(
        slotId: String, speed: ChargingSpeed, isAvailable: Boolean, isDamaged: Boolean
    ) = TODO()

    override suspend fun reportDamage(slotId: String, isDamaged: Boolean) = TODO()

    /* Nearby services */
    override fun getNearbyServicesForCharger(chargerId: String): Flow<List<NearbyService>> =
        chargerDoc(chargerId).collection("nearbyServices")
            .snapshots()
            .map { qs -> qs.toObjects(NearbyService::class.java) }

    override suspend fun addNearbyService(
        chargerId: String, name: String, type: String, distance: Int
    ) = TODO()

    /* Combined / search – client‑side for now */
    override fun getChargerWithDetails(chargerId: String)
            : Flow<NetworkResult<ChargerWithDetails>> = TODO()

    override suspend fun searchChargers(
        query: String?, chargingSpeed: ChargingSpeed?, isAvailable: Boolean?,
        maxPrice: Double?, sortBy: String?
    ) = TODO()
}
