package pt.ist.cmu.chargist.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
//import pt.ist.cmu.chargist.data.dao.ChargerDao
//import pt.ist.cmu.chargist.data.dao.UserDao
import pt.ist.cmu.chargist.data.model.Charger
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.NearbyService
import pt.ist.cmu.chargist.data.model.User
import pt.ist.cmu.chargist.util.Converters

@Database(
    entities = [
        User::class,
        Charger::class,
        ChargingSlot::class,
        NearbyService::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChargISTDatabase : RoomDatabase() {
//    abstract fun userDao(): UserDao
//    abstract fun chargerDao(): ChargerDao
}