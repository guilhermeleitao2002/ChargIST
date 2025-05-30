package pt.ist.cmu.chargist.data.repository

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.ist.cmu.chargist.data.model.User
import pt.ist.cmu.chargist.util.NetworkResult

interface AuthRepository {
    suspend fun signUp(username: String, email: String, pass: String): NetworkResult<User>
    suspend fun login(email: String, pass: String): NetworkResult<User>
    suspend fun logout()
    fun currentUser(): Flow<User?>
}

class FirebaseAuthRepository(
    private val context: Context,
    private val auth: FirebaseAuth,
    db: FirebaseFirestore
) : AuthRepository {

    private val users = db.collection("users")

    override fun currentUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fb ->
            val u = fb.currentUser
            if (u == null) trySend(null) else {
                users.document(u.uid)
                    .addSnapshotListener { snap, _ ->
                        trySend(snap?.toObject(User::class.java))
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun isPlayServicesAvailable(): Boolean =
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == CommonStatusCodes.SUCCESS

    private fun playServicesError() =
        NetworkResult.Error("GooglePlayServices is missing on this device.")

    private fun Throwable.asNetworkError(): NetworkResult.Error = when (this) {
        is ApiException,
        is SecurityException -> playServicesError()
        else                 -> NetworkResult.Error(message ?: "Operation failed")
    }

    override suspend fun signUp(username: String, email: String, pass: String)
            : NetworkResult<User> {

        if (!isPlayServicesAvailable()) return playServicesError()

        return runCatching {
            auth.createUserWithEmailAndPassword(email, pass).await()
            val uid  = auth.currentUser!!.uid
            val user = User(id = uid, username = username)
            users.document(uid).set(user).await()
            NetworkResult.Success(user)
        }.getOrElse { it.asNetworkError() }
    }

    override suspend fun login(email: String, pass: String): NetworkResult<User> {

        if (!isPlayServicesAvailable()) return playServicesError()

        return runCatching {
            auth.signInWithEmailAndPassword(email, pass).await()
            val doc = users.document(auth.currentUser!!.uid).get().await()
            NetworkResult.Success(doc.toObject(User::class.java)!!)
        }.getOrElse { it.asNetworkError() }
    }

    override suspend fun logout() { auth.signOut() }
}
