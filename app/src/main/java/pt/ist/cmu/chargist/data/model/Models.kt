package pt.ist.cmu.chargist.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

// Charging speed enumeration
enum class ChargingSpeed {
    FAST, MEDIUM, SLOW
}

// Connector type enumeration
enum class ConnectorType {
    CCS2, TYPE2
}

// Payment system model
data class PaymentSystem(
    val id: String,
    val name: String
)

//// User entity
//@Entity(tableName = "users")
//data class User(
//    @PrimaryKey val id: String,
//    val username: String,
//    val createdAt: Long = System.currentTimeMillis()
//)

// Charger entity
@Entity(tableName = "chargers")
data class Charger(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String?,
    val isFavorite: Boolean = false,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getLatLng(): LatLng = LatLng(latitude, longitude)
}

// Charging Slot entity with foreign key to Charger
@Entity(
    tableName = "charging_slots",
    foreignKeys = [
        ForeignKey(
            entity = Charger::class,
            parentColumns = ["id"],
            childColumns = ["chargerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChargingSlot(
    @PrimaryKey val id: String,
    val chargerId: String,
    val speed: ChargingSpeed,
    val connectorType: ConnectorType,
    val isAvailable: Boolean = true,
    val isDamaged: Boolean = false,
    val price: Double,
    val updatedAt: Long = System.currentTimeMillis()
)

// NearbyService entity with foreign key to Charger
@Entity(
    tableName = "nearby_services",
    foreignKeys = [
        ForeignKey(
            entity = Charger::class,
            parentColumns = ["id"],
            childColumns = ["chargerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NearbyService(
    @PrimaryKey val id: String,
    val chargerId: String,
    val name: String,
    val type: String, // e.g., "FOOD", "TOILET", "AIR_WATER"
    val distance: Int // distance in meters from charger
)

// Charger with all related information (for UI display)
data class ChargerWithDetails(
    val charger: Charger,
    val chargingSlots: List<ChargingSlot>,
    val nearbyServices: List<NearbyService>,
    val paymentSystems: List<PaymentSystem>
)