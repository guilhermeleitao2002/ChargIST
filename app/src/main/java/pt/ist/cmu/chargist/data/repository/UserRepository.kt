package pt.ist.cmu.chargist.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import pt.ist.cmu.chargist.data.dao.UserDao
import pt.ist.cmu.chargist.data.model.User
import pt.ist.cmu.chargist.util.NetworkResult
import java.util.UUID
import androidx.core.content.edit

interface UserRepository {
    suspend fun createUser(username: String): NetworkResult<User>
    suspend fun getUserById(userId: String): NetworkResult<User>
    suspend fun getCurrentUser(): NetworkResult<User>
    fun getAllUsers(): Flow<List<User>>
    suspend fun setCurrentUser(user: User)
    suspend fun logout()
}

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val sharedPreferences: SharedPreferences
) : UserRepository {

    companion object {
        private const val CURRENT_USER_ID = "current_user_id"
    }

    override suspend fun createUser(username: String): NetworkResult<User> {
        return try {
            val userId = UUID.randomUUID().toString()
            val user = User(userId, username)
            userDao.insert(user)
            setCurrentUser(user)
            NetworkResult.Success(user)
        } catch (e: Exception) {
            NetworkResult.Error("Error creating user: ${e.message}")
        }
    }

    override suspend fun getUserById(userId: String): NetworkResult<User> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                NetworkResult.Success(user)
            } else {
                NetworkResult.Error("User not found")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Error fetching user: ${e.message}")
        }
    }

    override suspend fun getCurrentUser(): NetworkResult<User> {
        val currentUserId = sharedPreferences.getString(CURRENT_USER_ID, null)
        return if (currentUserId != null) {
            getUserById(currentUserId)
        } else {
            NetworkResult.Error("No user logged in")
        }
    }

    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
    }

    override suspend fun setCurrentUser(user: User) {
        sharedPreferences.edit() { putString(CURRENT_USER_ID, user.id) }
    }

    override suspend fun logout() {
        sharedPreferences.edit() { remove(CURRENT_USER_ID) }
    }
}