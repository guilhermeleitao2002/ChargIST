package pt.ist.cmu.chargist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.ist.cmu.chargist.data.model.User
import pt.ist.cmu.chargist.data.repository.UserRepository
import pt.ist.cmu.chargist.util.NetworkResult

data class UserState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _userState = MutableStateFlow(UserState())
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _userState.value = UserState(isLoading = true)

            when (val result = userRepository.getCurrentUser()) {
                is NetworkResult.Success -> {
                    _userState.value = UserState(user = result.data)
                }
                is NetworkResult.Error -> {
                    _userState.value = UserState(error = result.message)
                }
                NetworkResult.Loading -> {
                    // Already set loading state above
                }
            }
        }
    }

    fun createUser(username: String) {
        viewModelScope.launch {
            _userState.value = UserState(isLoading = true)

            when (val result = userRepository.createUser(username)) {
                is NetworkResult.Success -> {
                    _userState.value = UserState(user = result.data)
                }
                is NetworkResult.Error -> {
                    _userState.value = UserState(error = result.message)
                }
                NetworkResult.Loading -> {
                    // Already set loading state above
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _userState.value = UserState()
        }
    }
}