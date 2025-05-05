package pt.ist.cmu.chargist.data.api

import pt.ist.cmu.chargist.data.model.Charger
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.NearbyService
import pt.ist.cmu.chargist.data.model.User
import pt.ist.cmu.chargist.util.NetworkResult
import java.io.IOException

/**
 * Wrapper for ChargISTApiService that handles errors and network responses
 */
class ChargISTApi(private val apiService: ChargISTApiService) {
    // User operations
    suspend fun createUser(user: User): NetworkResult<User> {
        return try {
            val response = apiService.createUser(user)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun getUser(userId: String): NetworkResult<User> {
        return try {
            val response = apiService.getUser(userId)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    // Charger operations
    suspend fun getChargers(
        latitude: Double? = null,
        longitude: Double? = null,
        radiusKm: Double? = null
    ): NetworkResult<List<Charger>> {
        return try {
            val response = apiService.getChargers(latitude, longitude, radiusKm)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun getCharger(chargerId: String): NetworkResult<Charger> {
        return try {
            val response = apiService.getCharger(chargerId)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun createCharger(charger: Charger): NetworkResult<Charger> {
        return try {
            val response = apiService.createCharger(charger)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun updateCharger(chargerId: String, charger: Charger): NetworkResult<Charger> {
        return try {
            val response = apiService.updateCharger(chargerId, charger)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun updateFavoriteStatus(chargerId: String, isFavorite: Boolean): NetworkResult<Charger> {
        return try {
            val response = apiService.updateFavoriteStatus(chargerId, isFavorite)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    // Charging Slot operations
    suspend fun getChargingSlots(chargerId: String): NetworkResult<List<ChargingSlot>> {
        return try {
            val response = apiService.getChargingSlots(chargerId)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun createChargingSlot(chargerId: String, chargingSlot: ChargingSlot): NetworkResult<ChargingSlot> {
        return try {
            val response = apiService.createChargingSlot(chargerId, chargingSlot)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun updateChargingSlot(slotId: String, chargingSlot: ChargingSlot): NetworkResult<ChargingSlot> {
        return try {
            val response = apiService.updateChargingSlot(slotId, chargingSlot)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun reportDamage(slotId: String, isDamaged: Boolean): NetworkResult<ChargingSlot> {
        return try {
            val response = apiService.reportDamage(slotId, isDamaged)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    // Nearby Services operations
    suspend fun getNearbyServices(chargerId: String): NetworkResult<List<NearbyService>> {
        return try {
            val response = apiService.getNearbyServices(chargerId)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun addNearbyService(chargerId: String, service: NearbyService): NetworkResult<NearbyService> {
        return try {
            val response = apiService.addNearbyService(chargerId, service)
            NetworkResult.Success(response)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun removeNearbyService(serviceId: String): NetworkResult<Unit> {
        return try {
            apiService.removeNearbyService(serviceId)
            NetworkResult.Success(Unit)
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.message}")
        }
    }
}