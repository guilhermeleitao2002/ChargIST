package pt.ist.cmu.chargist.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.IgnoreExtraProperties

/* ───────────── enums ───────────── */

enum class ChargingSpeed { FAST, MEDIUM, SLOW }
enum class ConnectorType  { CCS2, TYPE2 }

/* ───────────── simple model ───────────── */

data class PaymentSystem(val id: String = "", val name: String = "")

/* ───────────── Firestore‑ / Room‑backed entities ─────────────
   → every property has a default value        (Firestore needs this)
   → @IgnoreExtraProperties ignores extra db fields
   → Room annotations kept for local caching
───────────────────────────────────────────────────────────────── */

@IgnoreExtraProperties
@Entity(tableName = "chargers")
data class Charger(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageData: String? = null,
    val isFavorite: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun getLatLng(): LatLng = LatLng(latitude, longitude)
}

@IgnoreExtraProperties
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
    val isAvailable: Boolean = true,
    val isDamaged: Boolean = false,
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
    val type: String = "",      // e.g. "FOOD", "TOILET", "AIR_WATER"
    val distance: Int = 0       // metres
)

/* ───────────── composite for UI ───────────── */

data class ChargerWithDetails(
    val charger: Charger              = Charger(),
    val chargingSlots: List<ChargingSlot> = emptyList(),
    val nearbyServices: List<NearbyService> = emptyList(),
    val paymentSystems: List<PaymentSystem> = emptyList()
)
