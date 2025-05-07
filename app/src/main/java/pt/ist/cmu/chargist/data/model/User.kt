package pt.ist.cmu.chargist.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties            // ← Firestore
@Entity(tableName = "users")      // ← Room
data class User(

    /** Firestore / Room primary‑key (uid from FirebaseAuth) */
    @PrimaryKey
    val id: String = "",

    /** Human‑readable handle */
    val username: String = "",

    /** Epoch‑ms when this document was created */
    val createdAt: Long = System.currentTimeMillis()
)
