package pt.ist.cmu.chargist.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pt.ist.cmu.chargist.data.model.*
import pt.ist.cmu.chargist.data.repository.ChargerRepository
import pt.ist.cmu.chargist.data.repository.ImageStorageRepository
import pt.ist.cmu.chargist.data.repository.UserRepository
import pt.ist.cmu.chargist.util.NetworkResult
import java.util.UUID
import android.util.Base64

/* ───────────── state holders ───────────── */

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

/* ───────────── VM ───────────── */

class ChargerViewModel(
    private val chargerRepository: ChargerRepository,
    private val userRepository:    UserRepository,
    private val imageRepository:   ImageStorageRepository
) : ViewModel() {

    /* ---------- state ---------- */

    private val _detail = MutableStateFlow(ChargerDetailState())
    val  chargerDetailState: StateFlow<ChargerDetailState> = _detail.asStateFlow()

    private val _create = MutableStateFlow(ChargerCreationState())
    val  chargerCreationState: StateFlow<ChargerCreationState> = _create.asStateFlow()

    private val _search = MutableStateFlow(SearchState())
    val  searchState: StateFlow<SearchState> = _search.asStateFlow()

    /* ---------- details ---------- */

    fun loadChargerDetails(id: String) = viewModelScope.launch {
        _detail.value = ChargerDetailState(isLoading = true)
        chargerRepository.getChargerWithDetails(id).collectLatest { result ->
            _detail.value = when (result) {
                is NetworkResult.Success -> ChargerDetailState(result.data, false)
                is NetworkResult.Error   -> ChargerDetailState(error = result.message)
                NetworkResult.Loading    -> ChargerDetailState(isLoading = true)
            }
        }
    }

    fun toggleFavorite() = viewModelScope.launch {
        val charger = _detail.value.chargerWithDetails?.charger ?: return@launch
        when (val res = chargerRepository.updateFavoriteStatus(charger.id, !charger.isFavorite)) {
            is NetworkResult.Success -> {
                _detail.update { it.copy(chargerWithDetails = it.chargerWithDetails?.copy(charger = res.data)) }
            }
            else -> {}
        }
    }

    /* ---------- mutators for the “add charger” screen ---------- */

    fun updateChargerCreationName(name: String)              { _create.update { it.copy(name = name) } }
    fun updateChargerCreationLocation(loc: LatLng)           { _create.update { it.copy(location = loc) } }
    fun updateChargerCreationImage(uri: Uri)                 { _create.update { it.copy(imageUri = uri) } }
    fun resetChargerCreation()                               { _create.value = ChargerCreationState() }

    /* ---------- create charger ---------- */

    /* ---------- create charger ---------- */
    fun createCharger() = viewModelScope.launch {
        var s = _create.value

        /* 1 · quick validation ------------------------------------------------ */
        if (s.name.isBlank())   { _create.value = s.copy(error = "Name cannot be empty"); return@launch }
        if (s.location == null) { _create.value = s.copy(error = "Please select a location"); return@launch }

        _create.value = s.copy(isSubmitting = true, error = null)

        /* 2 · who is the current user?  -------------------------------------- */
        // 1st choice: the profile row (if you still keep usernames)
        val uid: String = when (val u = userRepository.getCurrentUser()) {
            is NetworkResult.Success -> u.data.id          // profile found → ok
            else -> {                                       // no profile yet → fall back to Firebase
                val firebaseUid = com.google.firebase.auth.FirebaseAuth
                    .getInstance().currentUser?.uid
                if (firebaseUid == null) {                  // not even signed‑in
                    _create.value = s.copy(isSubmitting = false,
                        error = "You need to sign‑in first")
                    return@launch
                }
                firebaseUid                                  // good enough for now
            }
        }

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

        /* 5 · write to Firestore --------------------------------------------- */
        when (val res = chargerRepository.createCharger(
            name      = s.name,
            location  = s.location,
            imageData = base64,
            userId    = uid
        )) {
            is NetworkResult.Success -> _create.value = ChargerCreationState(isSuccess = true)
            is NetworkResult.Error   -> _create.value = s.copy(isSubmitting = false,
                error = res.message)
            else -> {}   // Loading is impossible here – createCharger() is suspending
        }
    }


    /* ---------- search (unchanged) ---------- */
    fun updateSearchQuery(q: String)                        { _search.update { it.copy(query = q) } }
    fun updateSearchChargingSpeed(s: ChargingSpeed?)        { _search.update { it.copy(chargingSpeed = s) } }
    fun updateSearchAvailability(a: Boolean?)               { _search.update { it.copy(isAvailable = a) } }
    fun updateSearchMaxPrice(p: Double?)                    { _search.update { it.copy(maxPrice = p) } }
    fun updateSearchSortBy(sort: String)                    { _search.update { it.copy(sortBy = sort) } }

    fun searchChargers() = viewModelScope.launch {
        val st = _search.value
        _search.value = st.copy(isLoading = true, error = null)

        when (val res = chargerRepository.searchChargers(
            query = st.query.takeIf { it.isNotBlank() },
            chargingSpeed = st.chargingSpeed,
            isAvailable   = st.isAvailable,
            maxPrice      = st.maxPrice,
            sortBy        = st.sortBy
        )) {
            is NetworkResult.Success -> _search.value = st.copy(
                searchResults = res.data.map {
                    ChargerWithDetails(it, emptyList(), emptyList(), emptyList())
                },
                isLoading = false
            )
            is NetworkResult.Error   -> _search.value = st.copy(error = res.message, isLoading = false)
            else -> {}
        }
    }

    /* ---------- slots shortcuts ---------- */

    fun addChargingSlot(chargerId: String, speed: ChargingSpeed, connector: ConnectorType, price: Double) =
        viewModelScope.launch {
            if (chargerRepository.createChargingSlot(chargerId, speed, connector, price) is NetworkResult.Success)
                loadChargerDetails(chargerId)
        }

    fun reportSlotDamage(slotId: String, damaged: Boolean) =
        viewModelScope.launch {
            val res = chargerRepository.reportDamage(slotId, damaged)
            if (res is NetworkResult.Success) loadChargerDetails(res.data.chargerId)
        }
}
