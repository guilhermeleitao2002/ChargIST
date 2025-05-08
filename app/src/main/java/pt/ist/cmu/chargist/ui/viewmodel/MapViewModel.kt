package pt.ist.cmu.chargist.ui.viewmodel

import android.location.Location
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import pt.ist.cmu.chargist.data.model.Charger
import pt.ist.cmu.chargist.data.repository.ChargerRepository
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class MapState(
    val chargers: List<Charger> = emptyList(),
    val favoriteChargers: List<Charger> = emptyList(),
    val currentLocation: LatLng? = null,
    val hasLocationPermission: Boolean = false,        // ← NEW
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
        //loadFavoriteChargers()
        //viewModelScope.launch {
        //    getCurrentLocation()
        //}
    }

    fun loadFavoriteChargers2(userId: String) {
        viewModelScope.launch {
            try {
                chargerRepository.getFavoriteChargersForUser(userId).collectLatest { favoriteChargers ->
                    _mapState.value = _mapState.value.copy(
                        favoriteChargers = favoriteChargers
                    )
                }
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(
                    error = "Error loading favorite chargers2: ${e.message}"
                )
            }
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
                    error = "Error loading chargers aa: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun onLocationPermissionGranted() {
        if (!_mapState.value.hasLocationPermission) {
            _mapState.value = _mapState.value.copy(hasLocationPermission = true)
            viewModelScope.launch { getCurrentLocation() }
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
                    error = "Error loading chargers in bounds aa: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location = suspendCancellableCoroutine { cont ->
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location != null) cont.resume(location) else
                cont.resumeWithException(Exception("Location is null"))
        }.addOnFailureListener { e -> cont.resumeWithException(e) }
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

    fun toggleFavorite(charger: Charger, userId: String) {
        viewModelScope.launch {
            if (charger.favoriteUsers.contains(userId)) {
                chargerRepository.removeFavorite(userId, charger.id)
            } else {
                chargerRepository.addFavorite(userId, charger.id)
            }
        }
    }

    fun setLocationAndMoveCameraManually(location: LatLng) {
        viewModelScope.launch {
            _mapState.value = _mapState.value.copy(
                currentLocation = location
            )
        }
    }

    fun searchAddressUsingGeocoder(query: String) {
        viewModelScope.launch {
            try {
                _mapState.update { it.copy(isLoading = true, error = null) }

                val geocoder = Geocoder(context, Locale.getDefault())
                var addressList: List<Address>? = null

                // Handle API differences between Android versions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(query, 1) { addresses ->
                        addressList = addresses
                    }

                    // Wait a moment for the result
                    delay(1000)
                } else {
                    @Suppress("DEPRECATION")
                    addressList = geocoder.getFromLocationName(query, 1)
                }

                if (!addressList.isNullOrEmpty()) {
                    val address = addressList!![0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    _mapState.update {
                        it.copy(
                            currentLocation = latLng,
                            isLoading = false
                        )
                    }
                } else {
                    _mapState.update {
                        it.copy(
                            isLoading = false,
                            error = "Address not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _mapState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error searching address: ${e.message}"
                    )
                }
            }
        }
    }
}