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
import pt.ist.cmu.chargist.util.NetworkResult
import java.util.UUID
import android.util.Base64
import pt.ist.cmu.chargist.data.repository.AuthRepository

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
    val fastPositions: Int = 0,
    val mediumPositions: Int = 0,
    val slowPositions: Int = 0,
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
    private val authRepository:    AuthRepository,
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

    fun toggleFavorite(userId: String) = viewModelScope.launch {
        val charger = _detail.value.chargerWithDetails?.charger ?: return@launch
        if (charger.favoriteUsers.contains(userId)) {
            chargerRepository.removeFavorite(userId, charger.id)
        } else {
            chargerRepository.addFavorite(userId, charger.id)
        }
    }

    /* ---------- mutators for the “add charger” screen ---------- */

    fun updateChargerCreationName(name: String)              { _create.update { it.copy(name = name) } }
    fun updateChargerCreationLocation(loc: LatLng)           { _create.update { it.copy(location = loc) } }
    fun updateChargerCreationImage(uri: Uri)                 { _create.update { it.copy(imageUri = uri) } }
    fun resetChargerCreation()                               { _create.value = ChargerCreationState() }

    /* ---------- create charger ---------- */
    fun createCharger() = viewModelScope.launch {
        var s = _create.value

        /* 1 · quick validation ------------------------------------------------ */
        if (s.name.isBlank())   { _create.value = s.copy(error = "Name cannot be empty"); return@launch }
        if (s.location == null) { _create.value = s.copy(error = "Please select a location"); return@launch }

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
            chargingSlots = slots
        )) {
            is NetworkResult.Success -> _create.value = ChargerCreationState(isSuccess = true)
            is NetworkResult.Error -> _create.value = s.copy(isSubmitting = false, error = res.message)
            else -> {}
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
    fun updateFastPositions(count: Int) { _create.update { it.copy(fastPositions = count) } }
    fun updateMediumPositions(count: Int) { _create.update { it.copy(mediumPositions = count) } }
    fun updateSlowPositions(count: Int) { _create.update { it.copy(slowPositions = count) } }

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
