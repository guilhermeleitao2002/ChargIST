package pt.ist.cmu.chargist.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pt.ist.cmu.chargist.data.model.*
import pt.ist.cmu.chargist.data.repository.ChargerRepository
import pt.ist.cmu.chargist.data.repository.ImageStorageRepository
import pt.ist.cmu.chargist.util.NetworkResult
import java.util.UUID
import pt.ist.cmu.chargist.data.repository.AuthRepository
import pt.ist.cmu.chargist.data.repository.NearbyPlacesRepository

/* ───────────── state holders ───────────── */

data class ChargerDetailState(
    val chargerWithDetails: ChargerWithDetails? = null,
    val allChargers: List<Charger> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChargerCreationState(
    val name: String = "",
    val location: LatLng? = null,
    val imageUri: Uri? = null,
    val fastPositions: Int = 0,
    val mediumPositions: Int = 0,
    val slowPositions: Int = 0,
    val paymentSystems: List<PaymentSystem> = emptyList(),
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
    val error: String? = null,
    val paymentSystems: List<PaymentSystem>? = null,
    val userLocation: LatLng? = null)

/* ───────────── VM ───────────── */

class ChargerViewModel(
    private val chargerRepository: ChargerRepository,
    private val authRepository: AuthRepository,
    private val imageRepository: ImageStorageRepository
) : ViewModel() {

    /* ---------- state ---------- */

    private val _detail = MutableStateFlow(ChargerDetailState())
    val chargerDetailState: StateFlow<ChargerDetailState> = _detail.asStateFlow()

    private val _create = MutableStateFlow(ChargerCreationState())
    val chargerCreationState: StateFlow<ChargerCreationState> = _create.asStateFlow()

    private val _search = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _search.asStateFlow()

    private val _allChargers = MutableStateFlow<List<Charger>>(emptyList())
    val allChargers: StateFlow<List<Charger>> = _allChargers.asStateFlow()

    private val nearbyPlacesRepository = NearbyPlacesRepository()

    /* ---------- details ---------- */

    fun loadChargerDetails(id: String) = viewModelScope.launch {
        _detail.value = ChargerDetailState(isLoading = true)
        chargerRepository.getChargerWithDetails(id).collectLatest { result ->
            _detail.value = when (result) {
                is NetworkResult.Success -> ChargerDetailState(result.data, isLoading = false)
                is NetworkResult.Error -> ChargerDetailState(error = result.message)
                NetworkResult.Loading -> ChargerDetailState(isLoading = true)
            }
        }
    }

    fun toggleFavorite(userId: String) = viewModelScope.launch {
        val currentDetails = _detail.value.chargerWithDetails ?: return@launch
        val charger = currentDetails.charger

        // Create a new list of favorite users with the update
        val newFavoriteUsers = if (charger.favoriteUsers.contains(userId)) {
            charger.favoriteUsers - userId
        } else {
            charger.favoriteUsers + userId
        }

        // Update the UI immediately with optimistic update
        val updatedCharger = charger.copy(favoriteUsers = newFavoriteUsers)
        val updatedDetails = currentDetails.copy(charger = updatedCharger)
        _detail.value = _detail.value.copy(chargerWithDetails = updatedDetails)

        // Then perform the actual network request in the background
        if (newFavoriteUsers.contains(userId)) {
            chargerRepository.addFavorite(userId, charger.id)
        } else {
            chargerRepository.removeFavorite(userId, charger.id)
        }
    }

    /* ---------- mutators for the “add charger” screen ---------- */

    fun updateChargerCreationName(name: String) { _create.update { it.copy(name = name) } }
    fun updateChargerCreationLocation(loc: LatLng) { _create.update { it.copy(location = loc) } }
    fun updateChargerCreationImage(uri: Uri) { _create.update { it.copy(imageUri = uri) } }
    fun resetChargerCreation() { _create.value = ChargerCreationState() }

    /* ---------- create charger ---------- */
    fun createCharger() = viewModelScope.launch {
        var s = _create.value

        /* 1 · quick validation ------------------------------------------------ */
        if (s.name.isBlank()) { _create.value = s.copy(error = "Name cannot be empty"); return@launch }
        if (s.location == null) { _create.value = s.copy(error = "Please select a location"); return@launch }
        if (s.paymentSystems.isEmpty()) { _create.value = s.copy(error = "Please select at least one payment method"); return@launch }

        _create.value = s.copy(isSubmitting = true, error = null)

        /* 2 · who is the current user?  -------------------------------------- */
        val user = authRepository.currentUser().first()
        if (user == null) {
            _create.value = s.copy(
                isSubmitting = false,
                error = "You need to sign-in first"
            )
            return@launch
        }
        val uid = user.id

        /* 3 · deterministic id for the document & picture -------------------- */
        val chargerId = UUID.randomUUID().toString()

        /* 4 · encode picture (if any) ---------------------------------------- */
        val base64: String? = try {
            s.imageUri?.let { imageRepository.encodeImage(it) }
        } catch (t: Throwable) {
            _create.value = s.copy(isSubmitting = false,
                error = "Image encode failed: ${t.message}")
            return@launch
        }

        /* 5 · create charging slots ------------------------------------------ */
        val slots = mutableListOf<ChargingSlot>()

        // Add fast charging slots
        for (i in 1..s.fastPositions) {
            val slotId = "${chargerId}_fast_$i"
            slots.add(ChargingSlot(
                id = slotId,
                chargerId = chargerId,
                speed = ChargingSpeed.FAST,
                connectorType = ConnectorType.CCS2,
                price = 0.50,
                isAvailable = true,
                isDamaged = false,
                updatedAt = System.currentTimeMillis()
            ))
        }

        // Add medium charging slots
        for (i in 1..s.mediumPositions) {
            val slotId = "${chargerId}_medium_$i"
            slots.add(ChargingSlot(
                id = slotId,
                chargerId = chargerId,
                speed = ChargingSpeed.MEDIUM,
                connectorType = ConnectorType.CCS2,
                price = 0.35,
                isAvailable = true,
                isDamaged = false,
                updatedAt = System.currentTimeMillis()
            ))
        }

        // Add slow charging slots
        for (i in 1..s.slowPositions) {
            val slotId = "${chargerId}_slow_$i"
            slots.add(ChargingSlot(
                id = slotId,
                chargerId = chargerId,
                speed = ChargingSpeed.SLOW,
                connectorType = ConnectorType.TYPE2,
                price = 0.25,
                isAvailable = true,
                isDamaged = false,
                updatedAt = System.currentTimeMillis()
            ))
        }

        /* 6 · write to Firestore --------------------------------------------- */
        when (val res = chargerRepository.createCharger(
            name = s.name,
            location = s.location!!,
            imageData = base64,
            userId = uid,
            chargingSlots = slots,
            paymentSystems = s.paymentSystems,  // Pass payment systems to repository
            chargerId = chargerId
        )) {
            is NetworkResult.Success -> _create.value = ChargerCreationState(isSuccess = true)
            is NetworkResult.Error -> _create.value = s.copy(isSubmitting = false, error = res.message)
            else -> {}
        }
    }

    /* ---------- search ---------- */
    fun updateSearchQuery(q: String) { _search.update { it.copy(query = q) } }
    fun updateSearchChargingSpeed(s: ChargingSpeed?) { _search.update { it.copy(chargingSpeed = s) } }
    fun updateSearchAvailability(a: Boolean?) { _search.update { it.copy(isAvailable = a) } }
    fun updateSearchMaxPrice(p: Double?) { _search.update { it.copy(maxPrice = p) } }
    fun updateSearchSortBy(sort: String) { _search.update { it.copy(sortBy = sort) } }
    fun updateSearchPaymentSystems(systems: List<PaymentSystem>?) { _search.update { it.copy(paymentSystems = systems) } }
    fun updateUserLocation(location: LatLng?) { _search.update { it.copy(userLocation = location) } }

    fun searchChargers() = viewModelScope.launch {
        val st = _search.value
        _search.value = st.copy(isLoading = true, error = null)

        when (val res = chargerRepository.searchChargers(
            query = st.query.takeIf { it.isNotBlank() },
            chargingSpeed = st.chargingSpeed,
            isAvailable = st.isAvailable,
            maxPrice = st.maxPrice,
            sortBy = st.sortBy,
            paymentSystems = st.paymentSystems,
            userLocation = st.userLocation
        )) {
            is NetworkResult.Success -> {
                // Fetch slots for each charger in the search results
                val resultsWithSlots = res.data.map { charger ->
                    val slots = chargerRepository.getChargingSlotsForCharger(charger.id)
                    ChargerWithDetails(charger, slots, emptyList(), emptyList())
                }
                _search.value = st.copy(
                    searchResults = resultsWithSlots,
                    isLoading = false
                )
            }
            is NetworkResult.Error -> _search.value = st.copy(error = res.message, isLoading = false)
            else -> {}
        }
    }

    fun loadAllChargers() = viewModelScope.launch {
        when (val result = chargerRepository.getAllChargersSync()) {
            is NetworkResult.Success -> {
                _detail.update { it.copy(allChargers = result.data) }
            }
            is NetworkResult.Error -> {
                _detail.update { it.copy(error = result.message) }
            }
            else -> {}
        }
    }

    /* ---------- slots shortcuts ---------- */
    fun updateFastPositions(count: Int) { _create.update { it.copy(fastPositions = count) } }
    fun updateMediumPositions(count: Int) { _create.update { it.copy(mediumPositions = count) } }
    fun updateSlowPositions(count: Int) { _create.update { it.copy(slowPositions = count) } }

    fun addChargingSlot(chargerId: String, speed: ChargingSpeed, connector: ConnectorType, price: Double) =
        viewModelScope.launch {
            if (chargerRepository.createChargingSlot(chargerId, speed, connector, price) is NetworkResult.Success)
                loadChargerDetails(chargerId)
        }

    fun reportSlotDamage(slotId: String, damaged: Boolean, speed: ChargingSpeed) =
        viewModelScope.launch {
            val res = chargerRepository.reportDamage(slotId, damaged, speed)
            if (res is NetworkResult.Success) loadChargerDetails(res.data.chargerId)
        }

    /* ---------- nearby places ---------- */

    fun loadNearbyPlaces(location: LatLng) {
        val currentDetails = _detail.value.chargerWithDetails ?: return

        viewModelScope.launch {
            try {
                val places = nearbyPlacesRepository.getNearbyPlaces(location)

                // Convert NearbyPlace objects to NearbyService objects
                val services = places.map { place ->
                    NearbyService(
                        id = place.id,
                        chargerId = currentDetails.charger.id,
                        name = place.name,
                        type = place.placeType.uppercase(),
                        distance = place.distance
                    )
                }

                // Update state with the new nearby services
                if (services.isNotEmpty()) {
                    val updatedDetails = currentDetails.copy(
                        nearbyServices = services
                    )
                    _detail.value = _detail.value.copy(chargerWithDetails = updatedDetails)
                }
            } catch (e: Exception) {
                Log.e("ChargerViewModel", "Error loading nearby places: ${e.message}")
            }
        }
    }

    /* ---------- payment systems ---------- */
    fun updatePaymentSystems(systems: List<PaymentSystem>) {
        // Log the current state before update
        Log.d("ChargerViewModel", "Current payment systems: ${_create.value.paymentSystems.joinToString(", ") { it.name }}")

        // Log what we're updating to
        Log.d("ChargerViewModel", "Updating to: ${systems.joinToString(", ") { it.name }}")

        // Update the state
        _create.update { it.copy(paymentSystems = systems) }

        // Log the state after update to confirm it worked
        Log.d("ChargerViewModel", "After update: ${_create.value.paymentSystems.joinToString(", ") { it.name }}")
    }

    fun togglePaymentSystem(system: PaymentSystem, isSelected: Boolean) {
        val currentSystems = _create.value.paymentSystems.toMutableList()

        if (isSelected && !currentSystems.any { it.id == system.id }) {
            // Add the system if it's not already there
            currentSystems.add(system)
        } else if (!isSelected) {
            // Remove the system
            currentSystems.removeAll { it.id == system.id }
        }

        Log.d("ChargerViewModel", "Toggled ${system.name} to $isSelected. Now have ${currentSystems.size} systems")

        // Update the state with the new list
        _create.update { it.copy(paymentSystems = currentSystems) }
    }

    /* ---------- slots ---------- */
    suspend fun getChargingSlotsForCharger(chargerId: String): List<ChargingSlot> =
        chargerRepository.getChargingSlotsForCharger(chargerId)

    fun loadChargerBySlotId(slotId: String) = viewModelScope.launch {
        _detail.value = ChargerDetailState(isLoading = true)
        when (val result = chargerRepository.findChargerBySlotId(slotId)) {
            is NetworkResult.Success -> {
                val (charger, slot) = result.data
                loadChargerDetails(charger.id)
            }
            is NetworkResult.Error -> {
                _detail.value = ChargerDetailState(error = result.message)
            }
            else -> {}
        }
    }

    fun deleteCharger(chargerId: String) {
        viewModelScope.launch {
            chargerRepository.deleteCharger(chargerId)
            // Optional: Add error handling or UI feedback here
        }
    }

}