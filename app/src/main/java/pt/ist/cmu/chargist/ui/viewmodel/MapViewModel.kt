package pt.ist.cmu.chargist.ui.viewmodel

import android.content.Context
import android.location.Location
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    val searchedLocation: LatLng? = null,
    val hasLocationPermission: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val autocompleteSuggestions: List<AutocompletePrediction> = emptyList()
)

class MapViewModel(
    private val chargerRepository: ChargerRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val context: Context
) : ViewModel() {
    private val _mapState = MutableStateFlow(MapState(isLoading = true))
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()

    private val _focusRequests = MutableSharedFlow<LatLng>(replay = 1, extraBufferCapacity = 1)
    val focusRequests = _focusRequests.asSharedFlow()

    private val placesClient: PlacesClient by lazy {
        Places.initialize(context, "AIzaSyAU_8dfYDNi471YCS6ja-gZ8Clv4iM7jB4") // Substitua pela sua API Key
        Places.createClient(context)
    }

    init {
        loadChargers()
    }

    fun loadFavoriteChargers2(userId: String) {
        viewModelScope.launch {
            try {
                chargerRepository.getFavoriteChargersForUser(userId).collectLatest { favoriteChargers ->
                    _mapState.value = _mapState.value.copy(favoriteChargers = favoriteChargers)
                }
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(error = "Error loading favorite chargers2: ${e.message}")
            }
        }
    }

    internal fun loadChargers() {
        viewModelScope.launch {
            try {
                chargerRepository.getAllChargers().collectLatest { chargers ->
                    _mapState.value = _mapState.value.copy(chargers = chargers, isLoading = false)
                }
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(error = "Error loading chargers aa: ${e.message}", isLoading = false)
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
                    _mapState.value = _mapState.value.copy(chargers = chargers, isLoading = false)
                }
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(error = "Error loading chargers in bounds aa: ${e.message}", isLoading = false)
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
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
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
            _mapState.value = _mapState.value.copy(currentLocation = location)
        }
    }

    fun searchAddressUsingPlaceId(placeId: String) {
        viewModelScope.launch {
            try {
                _mapState.update { it.copy(isLoading = true, error = null) }

                val placeRequest = FetchPlaceRequest.builder(placeId, listOf(Place.Field.LAT_LNG))
                    .build()
                placesClient.fetchPlace(placeRequest).addOnSuccessListener { fetchResponse ->
                    val latLng = fetchResponse.place.latLng
                    if (latLng != null) {
                        _mapState.update {
                            it.copy(searchedLocation = latLng, isLoading = false)
                        }
                        focusOn(latLng)
                    } else {
                        _mapState.update { it.copy(isLoading = false, error = "No coordinates found") }
                    }
                }.addOnFailureListener { e ->
                    Log.e("MapViewModel", "Failed to fetch place: ${e.message}")
                    _mapState.update { it.copy(isLoading = false, error = "Failed to fetch place: ${e.message}") }
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error searching address: ${e.message}")
                _mapState.update { it.copy(isLoading = false, error = "Error searching address: ${e.message}") }
            }
        }
    }

    fun getAutocompleteSuggestions(query: String) {
        viewModelScope.launch {
            if (query.isNotEmpty()) {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .setCountries("PT")
                    .setTypesFilter(listOf(PlaceTypes.ADDRESS))
                    .build()

                placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                    Log.d("MapViewModel", "Suggestions received: ${response.autocompletePredictions.size}")
                    _mapState.update {
                        it.copy(autocompleteSuggestions = response.autocompletePredictions)
                    }
                }.addOnFailureListener { e ->
                    Log.e("MapViewModel", "Failed to fetch suggestions: ${e.message}")
                    _mapState.update { it.copy(error = "Failed to fetch suggestions: ${e.message}") }
                }
            } else {
                _mapState.update { it.copy(autocompleteSuggestions = emptyList()) }
            }
        }
    }

    fun focusOn(target: LatLng) {
        _focusRequests.tryEmit(target)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun markFocusConsumed() {
        _focusRequests.resetReplayCache()
    }

    fun updateError(error: String) {
        _mapState.value = _mapState.value.copy(error = error, isLoading = false)
    }
}