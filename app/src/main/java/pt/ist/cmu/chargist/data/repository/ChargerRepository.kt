package pt.ist.cmu.chargist.data.repository

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import pt.ist.cmu.chargist.data.api.ChargISTApi
import pt.ist.cmu.chargist.data.dao.ChargerDao
import pt.ist.cmu.chargist.data.model.Charger
import pt.ist.cmu.chargist.data.model.ChargerWithDetails
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.data.model.ConnectorType
import pt.ist.cmu.chargist.data.model.NearbyService
import pt.ist.cmu.chargist.data.model.PaymentSystem
import pt.ist.cmu.chargist.util.NetworkResult
import java.util.UUID

interface ChargerRepository {
    // Charger operations
    fun getAllChargers(): Flow<List<Charger>>
    fun getFavoriteChargers(): Flow<List<Charger>>
    fun getChargersInBounds(bounds: LatLngBounds): Flow<List<Charger>>
    suspend fun getChargerById(chargerId: String): NetworkResult<Charger>
    suspend fun createCharger(
        name: String,
        location: LatLng,
        imageUrl: String?,
        userId: String
    ): NetworkResult<Charger>
    suspend fun updateFavoriteStatus(chargerId: String, isFavorite: Boolean): NetworkResult<Charger>

    // Charging Slot operations
    fun getChargingSlotsForCharger(chargerId: String): Flow<List<ChargingSlot>>
    suspend fun createChargingSlot(
        chargerId: String,
        speed: ChargingSpeed,
        connectorType: ConnectorType,
        price: Double
    ): NetworkResult<ChargingSlot>
    suspend fun updateChargingSlot(
        slotId: String,
        speed: ChargingSpeed,
        isAvailable: Boolean,
        isDamaged: Boolean
    ): NetworkResult<ChargingSlot>
    suspend fun reportDamage(slotId: String, isDamaged: Boolean): NetworkResult<ChargingSlot>

    // Nearby Service operations
    fun getNearbyServicesForCharger(chargerId: String): Flow<List<NearbyService>>
    suspend fun addNearbyService(
        chargerId: String,
        name: String,
        type: String,
        distance: Int
    ): NetworkResult<NearbyService>

    // Combined data operations
    fun getChargerWithDetails(chargerId: String): Flow<NetworkResult<ChargerWithDetails>>
    suspend fun searchChargers(
        query: String? = null,
        chargingSpeed: ChargingSpeed? = null,
        isAvailable: Boolean? = null,
        maxPrice: Double? = null,
        sortBy: String? = null
    ): NetworkResult<List<Charger>>
}

class ChargerRepositoryImpl(
    private val chargerDao: ChargerDao,
    private val api: ChargISTApi,
    private val connectivityManager: ConnectivityManager
) : ChargerRepository {

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    // Charger operations
    override fun getAllChargers(): Flow<List<Charger>> {
        return chargerDao.getAllChargers()
    }

    override fun getFavoriteChargers(): Flow<List<Charger>> {
        return chargerDao.getFavoriteChargers()
    }

    override fun getChargersInBounds(bounds: LatLngBounds): Flow<List<Charger>> {
        return chargerDao.getChargersInBounds(
            bounds.southwest.latitude,
            bounds.northeast.latitude,
            bounds.southwest.longitude,
            bounds.northeast.longitude
        )
    }

    override suspend fun getChargerById(chargerId: String): NetworkResult<Charger> {
        // First try to get from local database
        val localCharger = chargerDao.getChargerById(chargerId)

        // If we have network, fetch from API to ensure data is fresh
        return if (isNetworkAvailable()) {
            when (val apiResult = api.getCharger(chargerId)) {
                is NetworkResult.Success -> {
                    // Update local database
                    chargerDao.insertCharger(apiResult.data)
                    apiResult
                }
                is NetworkResult.Error -> {
                    // If API failed but we have local data, return that
                    if (localCharger != null) {
                        NetworkResult.Success(localCharger)
                    } else {
                        apiResult
                    }
                }
                NetworkResult.Loading -> NetworkResult.Loading
            }
        } else {
            // If no network, return local data or error
            if (localCharger != null) {
                NetworkResult.Success(localCharger)
            } else {
                NetworkResult.Error("No network connection and charger not in local cache")
            }
        }
    }

    override suspend fun createCharger(
        name: String,
        location: LatLng,
        imageUrl: String?,
        userId: String
    ): NetworkResult<Charger> {
        val chargerId = UUID.randomUUID().toString()
        val charger = Charger(
            id = chargerId,
            name = name,
            latitude = location.latitude,
            longitude = location.longitude,
            imageUrl = imageUrl,
            createdBy = userId
        )

        return if (isNetworkAvailable()) {
            when (val apiResult = api.createCharger(charger)) {
                is NetworkResult.Success -> {
                    chargerDao.insertCharger(apiResult.data)
                    apiResult
                }
                is NetworkResult.Error -> {
                    // If API call fails, store locally anyway and sync later
                    chargerDao.insertCharger(charger)
                    NetworkResult.Success(charger) // Return success but will need syncing later
                }
                NetworkResult.Loading -> NetworkResult.Loading
            }
        } else {
            // If no network, store locally only
            chargerDao.insertCharger(charger)
            NetworkResult.Success(charger) // Will need syncing later
        }
    }

    override suspend fun updateFavoriteStatus(chargerId: String, isFavorite: Boolean): NetworkResult<Charger> {
        // Update locally first for quick feedback
        chargerDao.updateFavoriteStatus(chargerId, isFavorite)

        return if (isNetworkAvailable()) {
            when (val apiResult = api.updateFavoriteStatus(chargerId, isFavorite)) {
                is NetworkResult.Success -> {
                    chargerDao.insertCharger(apiResult.data)
                    apiResult
                }
                is NetworkResult.Error -> {
                    // API call failed but we already updated locally
                    val charger = chargerDao.getChargerById(chargerId)
                    if (charger != null) {
                        NetworkResult.Success(charger)
                    } else {
                        apiResult
                    }
                }
                NetworkResult.Loading -> NetworkResult.Loading
            }
        } else {
            // If no network, we already updated locally
            val charger = chargerDao.getChargerById(chargerId)
            return if (charger != null) {
                NetworkResult.Success(charger)
            } else {
                NetworkResult.Error("No network connection and charger not found locally")
            }
        }
    }

    // Charging Slot operations
    override fun getChargingSlotsForCharger(chargerId: String): Flow<List<ChargingSlot>> {
        return chargerDao.getChargingSlotsForCharger(chargerId)
    }

    override suspend fun createChargingSlot(
        chargerId: String,
        speed: ChargingSpeed,
        connectorType: ConnectorType,
        price: Double
    ): NetworkResult<ChargingSlot> {
        val slotId = UUID.randomUUID().toString()
        val chargingSlot = ChargingSlot(
            id = slotId,
            chargerId = chargerId,
            speed = speed,
            connectorType = connectorType,
            price = price,
            isAvailable = true,
            isDamaged = false
        )

        return if (isNetworkAvailable()) {
            when (val apiResult = api.createChargingSlot(chargerId, chargingSlot)) {
                is NetworkResult.Success -> {
                    chargerDao.insertChargingSlot(apiResult.data)
                    apiResult
                }
                is NetworkResult.Error -> {
                    // If API call fails, store locally anyway
                    chargerDao.insertChargingSlot(chargingSlot)
                    NetworkResult.Success(chargingSlot)
                }
                NetworkResult.Loading -> NetworkResult.Loading
            }
        } else {
            // If no network, store locally only
            chargerDao.insertChargingSlot(chargingSlot)
            NetworkResult.Success(chargingSlot)
        }
    }

    override suspend fun updateChargingSlot(
        slotId: String,
        speed: ChargingSpeed,
        isAvailable: Boolean,
        isDamaged: Boolean
    ): NetworkResult<ChargingSlot> {
        val existingSlot = chargerDao.getChargingSlotById(slotId) ?: return NetworkResult.Error("Charging slot not found")

        val updatedSlot = existingSlot.copy(
            speed = speed,
            isAvailable = isAvailable,
            isDamaged = isDamaged,
            updatedAt = System.currentTimeMillis()
        )

        // Update locally first
        chargerDao.updateChargingSlot(updatedSlot)

        return if (isNetworkAvailable()) {
            when (val apiResult = api.updateChargingSlot(slotId, updatedSlot)) {
                is NetworkResult.Success -> {
                    chargerDao.updateChargingSlot(apiResult.data)
                    apiResult
                }
                is NetworkResult.Error -> {
                    // We already updated locally
                    NetworkResult.Success(updatedSlot)
                }
                NetworkResult.Loading -> NetworkResult.Loading
            }
        } else {
            // If no network, we already updated locally
            NetworkResult.Success(updatedSlot)
        }
    }

    override suspend fun reportDamage(slotId: String, isDamaged: Boolean): NetworkResult<ChargingSlot> {
        val existingSlot = chargerDao.getChargingSlotById(slotId) ?: return NetworkResult.Error("Charging slot not found")

        // Update locally first
        val updatedSlot = existingSlot.copy(isDamaged = isDamaged, updatedAt = System.currentTimeMillis())
        chargerDao.updateChargingSlot(updatedSlot)

        return if (isNetworkAvailable()) {
            when (val apiResult = api.reportDamage(slotId, isDamaged)) {
                is NetworkResult.Success -> {
                    chargerDao.updateChargingSlot(apiResult.data)
                    apiResult
                }
                is NetworkResult.Error -> {
                    // We already updated locally
                    NetworkResult.Success(updatedSlot)
                }
                NetworkResult.Loading -> NetworkResult.Loading
            }
        } else {
            // If no network, we already updated locally
            NetworkResult.Success(updatedSlot)
        }
    }

    // Nearby Service operations
    override fun getNearbyServicesForCharger(chargerId: String): Flow<List<NearbyService>> {
        return chargerDao.getNearbyServicesForCharger(chargerId)
    }

    override suspend fun addNearbyService(
        chargerId: String,
        name: String,
        type: String,
        distance: Int
    ): NetworkResult<NearbyService> {
        val serviceId = UUID.randomUUID().toString()
        val service = NearbyService(
            id = serviceId,
            chargerId = chargerId,
            name = name,
            type = type,
            distance = distance
        )

        return if (isNetworkAvailable()) {
            when (val apiResult = api.addNearbyService(chargerId, service)) {
                is NetworkResult.Success -> {
                    chargerDao.insertNearbyService(apiResult.data)
                    apiResult
                }
                is NetworkResult.Error -> {
                    // If API call fails, store locally anyway
                    chargerDao.insertNearbyService(service)
                    NetworkResult.Success(service)
                }
                NetworkResult.Loading -> NetworkResult.Loading
            }
        } else {
            // If no network, store locally only
            chargerDao.insertNearbyService(service)
            NetworkResult.Success(service)
        }
    }

    // Combined data operations
    override fun getChargerWithDetails(chargerId: String): Flow<NetworkResult<ChargerWithDetails>> = flow {
        emit(NetworkResult.Loading)

        // Get charger, charging slots, and nearby services from local database
        val chargerResult = getChargerById(chargerId)

        if (chargerResult is NetworkResult.Success) {
            val charger = chargerResult.data
            val chargingSlots = getChargingSlotsForCharger(chargerId).first()
            val nearbyServices = getNearbyServicesForCharger(chargerId).first()

            // Simulated payment systems for now
            val paymentSystems = listOf(
                PaymentSystem("1", "Credit Card"),
                PaymentSystem("2", "Mobile Payment")
            )

            val chargerWithDetails = ChargerWithDetails(
                charger = charger,
                chargingSlots = chargingSlots,
                nearbyServices = nearbyServices,
                paymentSystems = paymentSystems
            )

            emit(NetworkResult.Success(chargerWithDetails))
        } else if (chargerResult is NetworkResult.Error) {
            emit(NetworkResult.Error(chargerResult.message))
        }
    }

    override suspend fun searchChargers(
        query: String?,
        chargingSpeed: ChargingSpeed?,
        isAvailable: Boolean?,
        maxPrice: Double?,
        sortBy: String?
    ): NetworkResult<List<Charger>> {
        // For simplicity, just filter locally for now
        // In a real app, this would be a server-side query
        val allChargers = getAllChargers().first()

        // Basic filtering
        var filteredChargers = allChargers

        if (!query.isNullOrBlank()) {
            filteredChargers = filteredChargers.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }

        if (chargingSpeed != null) {
            // This is simplified. In a real app, we'd need to join with charging slots
            val chargersWithSpeed = mutableSetOf<String>()
            for (charger in allChargers) {
                val slots = getChargingSlotsForCharger(charger.id).first()
                if (slots.any { it.speed == chargingSpeed }) {
                    chargersWithSpeed.add(charger.id)
                }
            }
            filteredChargers = filteredChargers.filter { it.id in chargersWithSpeed }
        }

        // Apply sorting if specified
        val sortedChargers = when (sortBy) {
            "name" -> filteredChargers.sortedBy { it.name }
            "recent" -> filteredChargers.sortedByDescending { it.createdAt }
            // For distance sorting, we'd need current location
            else -> filteredChargers
        }

        return NetworkResult.Success(sortedChargers)
    }
}