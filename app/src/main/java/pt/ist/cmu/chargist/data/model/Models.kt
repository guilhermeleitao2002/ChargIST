package pt.ist.cmu.chargist.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.IgnoreExtraProperties

enum class ChargingSpeed { FAST, MEDIUM, SLOW }
enum class ConnectorType  { CCS2, TYPE2 }

data class PaymentSystem(
    val id: String = "",
    val name: String = "",
    val iconResId: Int? = null
)

@IgnoreExtraProperties
@Entity(tableName = "chargers")
data class Charger(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageData: String? = null,
    val favoriteUsers: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long? = 0L,
    val paymentSystems: List<PaymentSystem> = emptyList(),
) {
    fun getLatLng(): LatLng = LatLng(latitude, longitude)
}

@Entity(
    tableName = "charging_slots",
    foreignKeys = [ForeignKey(
        entity = Charger::class,
        parentColumns = ["id"],
        childColumns = ["chargerId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ChargingSlot(
    @PrimaryKey val id: String = "",
    val chargerId: String = "",
    val speed: ChargingSpeed = ChargingSpeed.SLOW,
    val connectorType: ConnectorType = ConnectorType.CCS2,
    val available: Boolean = true,
    val damaged: Boolean = false,
    val price: Double = 0.0,
    val updatedAt: Long = 0L
)

@IgnoreExtraProperties
@Entity(
    tableName = "nearby_services",
    foreignKeys = [ForeignKey(
        entity = Charger::class,
        parentColumns = ["id"],
        childColumns = ["chargerId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class NearbyService(
    @PrimaryKey val id: String = "",
    val chargerId: String = "",
    val name: String = "",
    val type: String = "",
    val distance: Int = 0
)

@IgnoreExtraProperties
@Entity(
    tableName = "ratings",
    foreignKeys = [ForeignKey(
        entity = Charger::class,
        parentColumns = ["id"],
        childColumns = ["chargerId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Rating(
    @PrimaryKey val id: String = "",
    val chargerId: String = "",
    val userId: String = "",
    val stars: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class RatingStats(
    val averageRating: Double = 0.0,
    val totalRatings: Int = 0,
    val histogram: Map<Int, Int> = emptyMap()
)

data class ChargerWithDetails(
    val charger: Charger = Charger(),
    val chargingSlots: List<ChargingSlot> = emptyList(),
    val nearbyServices: List<NearbyService> = emptyList(),
    val paymentSystems: List<PaymentSystem> = emptyList(),
    val ratingStats: RatingStats = RatingStats(),
    val userRating: Rating? = null
)
