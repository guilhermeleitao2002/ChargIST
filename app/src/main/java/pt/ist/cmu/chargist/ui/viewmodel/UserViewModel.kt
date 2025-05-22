package pt.ist.cmu.chargist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ist.cmu.chargist.data.model.User
import pt.ist.cmu.chargist.data.repository.AuthRepository
import pt.ist.cmu.chargist.util.NetworkResult

data class UserState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class UserViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(UserState())
    val userState: StateFlow<UserState> = _state

    init {
        viewModelScope.launch {
            repo.currentUser().collect { user ->
                _state.value = UserState(user = user)
            }
        }
    }

    fun signUp(username: String, email: String, pass: String) = request {
        repo.signUp(username, email, pass)
    }

    fun login(email: String, pass: String) = request {
        repo.login(email, pass)
    }

    fun logout() = viewModelScope.launch { repo.logout() }

    private fun request(call: suspend () -> NetworkResult<User>) {
        viewModelScope.launch {
            _state.value = UserState(isLoading = true)
            _state.value = when (val res = call()) {
                is NetworkResult.Success -> UserState(user = res.data)
                is NetworkResult.Error   -> UserState(error = res.message)
                else -> UserState()
            }
        }
    }
}
