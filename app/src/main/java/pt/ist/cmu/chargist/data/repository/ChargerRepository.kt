package pt.ist.cmu.chargist.data.repository

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.flow.Flow
import pt.ist.cmu.chargist.data.model.*
import pt.ist.cmu.chargist.data.model.PaymentSystem
import pt.ist.cmu.chargist.util.NetworkResult

/** Contract that Firestore (and future mocks) must implement */
interface ChargerRepository {

    /* ─────── chargers ─────── */
    fun getAllChargers(): Flow<List<Charger>>
    fun getFavoriteChargers(): Flow<List<Charger>>
    fun getChargersInBounds(bounds: LatLngBounds): Flow<List<Charger>>

    suspend fun getChargerById(id: String): NetworkResult<Charger>
    suspend fun getAllChargersSync(): NetworkResult<List<Charger>>

    fun getFavoriteChargersForUser(userId: String):Flow<List<Charger>>

    suspend fun addFavorite(userId: String, chargerId: String): NetworkResult<Charger>
    suspend fun removeFavorite(userId: String, chargerId: String): NetworkResult<Charger>


    /**
     * Create a new charger.
     *
     * `imageData` is an **optional Base‑64 JPEG** string that we embed directly
     * in the document.  If you switch back to Cloud Storage one day, keep the
     * same field name but store a https URL instead.
     */
    suspend fun createCharger(
        name: String,
        location: LatLng,
        imageData: String?,
        userId: String,
        chargerId: String,
        chargingSlots: List<ChargingSlot> = emptyList(),
        paymentSystems: List<PaymentSystem> = emptyList()
    ): NetworkResult<Charger>

    // function calls to firebase
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean): NetworkResult<Charger>
    suspend fun deleteCharger(chargerId: String): NetworkResult<Unit>
    suspend fun getChargingSlotsForCharger(chargerId: String): List<ChargingSlot>
    suspend fun findChargerBySlotId(slotId: String): NetworkResult<Pair<Charger, ChargingSlot>>

    suspend fun createChargingSlot(
        chargerId: String,
        speed: ChargingSpeed,
        connectorType: ConnectorType,
        price: Double
    ): NetworkResult<ChargingSlot>

    suspend fun updateChargingSlot(
        slotId: String,
        speed: ChargingSpeed,
        available: Boolean,
        damaged: Boolean,
        connectorType: ConnectorType? = null,
        price: Double? = null
    ): NetworkResult<ChargingSlot>

    suspend fun reportDamage(slotId: String, isDamaged: Boolean, speed: ChargingSpeed): NetworkResult<ChargingSlot>

    /* ─────── nearby services ─────── */
    fun getNearbyServicesForCharger(chargerId: String): Flow<List<NearbyService>>
    suspend fun addNearbyService(
        chargerId: String, name: String, type: String, distance: Int
    ): NetworkResult<NearbyService>

    /* ─────── composition / search ─────── */
    fun getChargerWithDetails(chargerId: String): Flow<NetworkResult<ChargerWithDetails>>

    suspend fun searchChargers(
        query: String? = null,
        chargingSpeed: ChargingSpeed? = null,
        isAvailable: Boolean? = null,
        maxPrice: Double? = null,
        sortBy: String? = null,
        paymentSystems: List<PaymentSystem>? = emptyList(),
        userLocation: LatLng? = null
    ): NetworkResult<List<Charger>>
}

