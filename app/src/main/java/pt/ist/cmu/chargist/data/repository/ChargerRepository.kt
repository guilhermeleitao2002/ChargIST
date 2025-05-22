package pt.ist.cmu.chargist.data.repository

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.flow.Flow
import pt.ist.cmu.chargist.data.model.*
import pt.ist.cmu.chargist.data.model.PaymentSystem
import pt.ist.cmu.chargist.util.NetworkResult

interface ChargerRepository {

    fun getAllChargers(): Flow<List<Charger>>
    fun getFavoriteChargers(): Flow<List<Charger>>
    fun getChargersInBounds(bounds: LatLngBounds): Flow<List<Charger>>

    suspend fun getChargerById(id: String): NetworkResult<Charger>
    suspend fun getAllChargersSync(): NetworkResult<List<Charger>>

    fun getFavoriteChargersForUser(userId: String):Flow<List<Charger>>

    suspend fun addFavorite(userId: String, chargerId: String): NetworkResult<Charger>
    suspend fun removeFavorite(userId: String, chargerId: String): NetworkResult<Charger>

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

    fun getNearbyServicesForCharger(chargerId: String): Flow<List<NearbyService>>
    suspend fun addNearbyService(
        chargerId: String, name: String, type: String, distance: Int
    ): NetworkResult<NearbyService>

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

