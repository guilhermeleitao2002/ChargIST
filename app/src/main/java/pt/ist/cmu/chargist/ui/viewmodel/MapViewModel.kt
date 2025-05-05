package pt.ist.cmu.chargist.ui.viewmodel

import android.location.Location
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pt.ist.cmu.chargist.data.model.Charger
import pt.ist.cmu.chargist.data.repository.ChargerRepository
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class MapState(
    val chargers: List<Charger> = emptyList(),
    val favoriteChargers: List<Charger> = emptyList(),
    val currentLocation: LatLng? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MapViewModel(
    private val chargerRepository: ChargerRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val context: Context
) : ViewModel() {

    private val _mapState = MutableStateFlow(MapState(isLoading = true))
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()

    init {
        loadChargers()
        loadFavoriteChargers()
        viewModelScope.launch {
            getCurrentLocation()
        }
    }

    private fun loadChargers() {
        viewModelScope.launch {
            try {
                chargerRepository.getAllChargers().collectLatest { chargers ->
                    _mapState.value = _mapState.value.copy(
                        chargers = chargers,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(
                    error = "Error loading chargers: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun loadFavoriteChargers() {
        viewModelScope.launch {
            try {
                chargerRepository.getFavoriteChargers().collectLatest { favoriteChargers ->
                    _mapState.value = _mapState.value.copy(
                        favoriteChargers = favoriteChargers
                    )
                }
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(
                    error = "Error loading favorite chargers: ${e.message}"
                )
            }
        }
    }

    fun loadChargersInBounds(bounds: LatLngBounds) {
        viewModelScope.launch {
            try {
                chargerRepository.getChargersInBounds(bounds).collectLatest { chargers ->
                    _mapState.value = _mapState.value.copy(
                        chargers = chargers,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(
                    error = "Error loading chargers in bounds: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private suspend fun getCurrentLocation() {
        try {
            val location = getLastLocationSuspend()
            _mapState.value = _mapState.value.copy(
                currentLocation = LatLng(location.latitude, location.longitude)
            )
        } catch (e: Exception) {
            _mapState.value = _mapState.value.copy(
                error = "Error getting current location: ${e.message}"
            )
        }
    }

    private suspend fun getLastLocationSuspend(): Location = suspendCoroutine { continuation ->
        try {
            // Check for permission first
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(location)
                        } else {
                            continuation.resumeWithException(Exception("Location is null"))
                        }
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            } else {
                continuation.resumeWithException(SecurityException("Location permission not granted"))
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    fun toggleFavorite(charger: Charger) {
        viewModelScope.launch {
            chargerRepository.updateFavoriteStatus(charger.id, !charger.isFavorite)
        }
    }
}