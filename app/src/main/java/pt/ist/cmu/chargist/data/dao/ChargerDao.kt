package pt.ist.cmu.chargist.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pt.ist.cmu.chargist.data.model.Charger
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.NearbyService

@Dao
interface ChargerDao {
    // Charger operations
    /*@Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharger(charger: Charger)

    @Update
    suspend fun updateCharger(charger: Charger)

    @Query("SELECT * FROM chargers WHERE id = :chargerId")
    suspend fun getChargerById(chargerId: String): Charger?

    @Query("SELECT * FROM chargers")
    fun getAllChargers(): Flow<List<Charger>>

    @Query("SELECT * FROM chargers WHERE isFavorite = 1")
    fun getFavoriteChargers(): Flow<List<Charger>>

    @Query("UPDATE chargers SET isFavorite = :isFavorite WHERE id = :chargerId")
    suspend fun updateFavoriteStatus(chargerId: String, isFavorite: Boolean)

    // Charging Slot operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChargingSlot(chargingSlot: ChargingSlot)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChargingSlots(chargingSlots: List<ChargingSlot>)

    @Update
    suspend fun updateChargingSlot(chargingSlot: ChargingSlot)

    @Query("SELECT * FROM charging_slots WHERE chargerId = :chargerId")
    fun getChargingSlotsForCharger(chargerId: String): Flow<List<ChargingSlot>>

    @Query("SELECT * FROM charging_slots WHERE id = :slotId")
    suspend fun getChargingSlotById(slotId: String): ChargingSlot?

    // Nearby Service operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNearbyService(nearbyService: NearbyService)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNearbyServices(nearbyServices: List<NearbyService>)

    @Query("SELECT * FROM nearby_services WHERE chargerId = :chargerId")
    fun getNearbyServicesForCharger(chargerId: String): Flow<List<NearbyService>>

    // Complex queries
    @Query("SELECT * FROM chargers WHERE " +
            "(:minLat IS NULL OR latitude >= :minLat) AND " +
            "(:maxLat IS NULL OR latitude <= :maxLat) AND " +
            "(:minLng IS NULL OR longitude >= :minLng) AND " +
            "(:maxLng IS NULL OR longitude <= :maxLng)")
    fun getChargersInBounds(
        minLat: Double?,
        maxLat: Double?,
        minLng: Double?,
        maxLng: Double?
    ): Flow<List<Charger>>

    @Transaction
    @Query("SELECT c.* FROM chargers c " +
            "INNER JOIN charging_slots cs ON c.id = cs.chargerId " +
            "WHERE cs.speed = :speed AND cs.isAvailable = 1 " +
            "GROUP BY c.id")
    fun getAvailableChargersBySpeed(speed: String): Flow<List<Charger>>*/
}