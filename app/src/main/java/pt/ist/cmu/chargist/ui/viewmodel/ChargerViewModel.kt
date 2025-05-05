package pt.ist.cmu.chargist.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pt.ist.cmu.chargist.data.model.ChargerWithDetails
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.data.model.ConnectorType
import pt.ist.cmu.chargist.data.repository.ChargerRepository
import pt.ist.cmu.chargist.data.repository.UserRepository
import pt.ist.cmu.chargist.util.NetworkResult

data class ChargerDetailState(
    val chargerWithDetails: ChargerWithDetails? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChargerCreationState(
    val name: String = "",
    val location: LatLng? = null,
    val imageUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

data class SearchState(
    val query: String = "",
    val chargingSpeed: ChargingSpeed? = null,
    val isAvailable: Boolean? = null,
    val maxPrice: Double? = null,
    val sortBy: String = "distance",
    val searchResults: List<ChargerWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChargerViewModel(
    private val chargerRepository: ChargerRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _chargerDetailState = MutableStateFlow(ChargerDetailState(isLoading = false))
    val chargerDetailState: StateFlow<ChargerDetailState> = _chargerDetailState.asStateFlow()

    private val _chargerCreationState = MutableStateFlow(ChargerCreationState())
    val chargerCreationState: StateFlow<ChargerCreationState> = _chargerCreationState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    fun loadChargerDetails(chargerId: String) {
        viewModelScope.launch {
            _chargerDetailState.value = ChargerDetailState(isLoading = true)

            chargerRepository.getChargerWithDetails(chargerId).collectLatest { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _chargerDetailState.value = ChargerDetailState(
                            chargerWithDetails = result.data,
                            isLoading = false
                        )
                    }
                    is NetworkResult.Error -> {
                        _chargerDetailState.value = ChargerDetailState(
                            error = result.message,
                            isLoading = false
                        )
                    }
                    NetworkResult.Loading -> {
                        _chargerDetailState.value = ChargerDetailState(isLoading = true)
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentCharger = _chargerDetailState.value.chargerWithDetails?.charger ?: return@launch

            val result = chargerRepository.updateFavoriteStatus(currentCharger.id, !currentCharger.isFavorite)

            if (result is NetworkResult.Success) {
                // Update the current state with the new favorite status
                val updatedCharger = result.data
                val currentDetails = _chargerDetailState.value.chargerWithDetails

                if (currentDetails != null) {
                    _chargerDetailState.value = _chargerDetailState.value.copy(
                        chargerWithDetails = currentDetails.copy(charger = updatedCharger)
                    )
                }
            }
        }
    }

    fun updateChargerCreationName(name: String) {
        _chargerCreationState.value = _chargerCreationState.value.copy(name = name)
    }

    fun updateChargerCreationLocation(location: LatLng) {
        _chargerCreationState.value = _chargerCreationState.value.copy(location = location)
    }

    fun updateChargerCreationImage(imageUri: Uri) {
        _chargerCreationState.value = _chargerCreationState.value.copy(imageUri = imageUri)
    }

    fun createCharger() {
        viewModelScope.launch {
            val state = _chargerCreationState.value

            if (state.name.isBlank()) {
                _chargerCreationState.value = state.copy(error = "Name cannot be empty")
                return@launch
            }

            if (state.location == null) {
                _chargerCreationState.value = state.copy(error = "Please select a location")
                return@launch
            }

            _chargerCreationState.value = state.copy(isSubmitting = true, error = null)

            // Get current user
            val userResult = userRepository.getCurrentUser()
            if (userResult !is NetworkResult.Success) {
                _chargerCreationState.value = state.copy(
                    isSubmitting = false,
                    error = "Please create a username first"
                )
                return@launch
            }

            // Create charger
            val imageUrl = state.imageUri?.toString() // In a real app, would upload the image first
            val result = chargerRepository.createCharger(
                name = state.name,
                location = state.location,
                imageUrl = imageUrl,
                userId = userResult.data.id
            )

            when (result) {
                is NetworkResult.Success -> {
                    _chargerCreationState.value = ChargerCreationState(isSuccess = true)
                }
                is NetworkResult.Error -> {
                    _chargerCreationState.value = state.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
                NetworkResult.Loading -> {
                    // Already set loading state above
                }
            }
        }
    }

    fun resetChargerCreation() {
        _chargerCreationState.value = ChargerCreationState()
    }

    fun updateSearchQuery(query: String) {
        _searchState.value = _searchState.value.copy(query = query)
    }

    fun updateSearchChargingSpeed(speed: ChargingSpeed?) {
        _searchState.value = _searchState.value.copy(chargingSpeed = speed)
    }

    fun updateSearchAvailability(isAvailable: Boolean?) {
        _searchState.value = _searchState.value.copy(isAvailable = isAvailable)
    }

    fun updateSearchMaxPrice(maxPrice: Double?) {
        _searchState.value = _searchState.value.copy(maxPrice = maxPrice)
    }

    fun updateSearchSortBy(sortBy: String) {
        _searchState.value = _searchState.value.copy(sortBy = sortBy)
    }

    fun searchChargers() {
        viewModelScope.launch {
            val state = _searchState.value
            _searchState.value = state.copy(isLoading = true, error = null)

            val result = chargerRepository.searchChargers(
                query = state.query.takeIf { it.isNotBlank() },
                chargingSpeed = state.chargingSpeed,
                isAvailable = state.isAvailable,
                maxPrice = state.maxPrice,
                sortBy = state.sortBy
            )

            when (result) {
                is NetworkResult.Success -> {
                    // For simplicity, we're getting full details immediately
                    // In a real app, you might want to get basic info for the list
                    // and fetch details only when a user selects a charger
                    val detailedResults = result.data.map { charger ->
                        val detailsResult = chargerRepository.getChargerWithDetails(charger.id).collectLatest {
                            // This would collect the latest charger details
                        }
                        // For now, we're making a simplified version just for the example
                        ChargerWithDetails(
                            charger = charger,
                            chargingSlots = emptyList(),
                            nearbyServices = emptyList(),
                            paymentSystems = emptyList()
                        )
                    }

                    _searchState.value = state.copy(
                        searchResults = detailedResults,
                        isLoading = false
                    )
                }
                is NetworkResult.Error -> {
                    _searchState.value = state.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                NetworkResult.Loading -> {
                    // Already set loading state above
                }
            }
        }
    }

    fun addChargingSlot(
        chargerId: String,
        speed: ChargingSpeed,
        connectorType: ConnectorType,
        price: Double
    ) {
        viewModelScope.launch {
            val result = chargerRepository.createChargingSlot(
                chargerId = chargerId,
                speed = speed,
                connectorType = connectorType,
                price = price
            )

            if (result is NetworkResult.Success) {
                // Refresh charger details
                loadChargerDetails(chargerId)
            }
        }
    }

    fun reportSlotDamage(slotId: String, isDamaged: Boolean) {
        viewModelScope.launch {
            val result = chargerRepository.reportDamage(slotId, isDamaged)

            if (result is NetworkResult.Success) {
                val slot = result.data
                // Refresh charger details
                loadChargerDetails(slot.chargerId)
            }
        }
    }
}