package pt.ist.cmu.chargist.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "users")
data class User(

    @PrimaryKey
    val id: String = "",
    val username: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
