package pt.ist.cmu.chargist.data.repository


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
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
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : AuthRepository {

    private val users = db.collection("users")

    override fun currentUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) trySend(null) else {
                users.document(firebaseUser.uid)
                    .addSnapshotListener { snap, _ ->
                        trySend(snap?.toObject<User>())
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signUp(username: String, email: String, pass: String): NetworkResult<User> =
        runCatching {
            auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = auth.currentUser!!.uid
            val user = User(id = uid, username = username)
            users.document(uid).set(user).await()
            NetworkResult.Success(user)
        }.getOrElse { NetworkResult.Error(it.message ?: "Sign‑up failed") }

    override suspend fun login(email: String, pass: String): NetworkResult<User> =
        runCatching {
            auth.signInWithEmailAndPassword(email, pass).await()
            val doc = users.document(auth.currentUser!!.uid).get().await()
            NetworkResult.Success(doc.toObject<User>()!!)
        }.getOrElse { NetworkResult.Error(it.message ?: "Login failed") }

    override suspend fun logout() { auth.signOut() }
}
