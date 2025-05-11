package pt.ist.cmu.chargist.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.data.model.ConnectorType
import pt.ist.cmu.chargist.data.model.PaymentSystem

/**
 * Type converters for Room database
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromChargingSpeed(value: ChargingSpeed): String {
        return value.name
    }

    @TypeConverter
    fun toChargingSpeed(value: String): ChargingSpeed {
        return ChargingSpeed.valueOf(value)
    }

    @TypeConverter
    fun fromConnectorType(value: ConnectorType): String {
        return value.name
    }

    @TypeConverter
    fun toConnectorType(value: String): ConnectorType {
        return ConnectorType.valueOf(value)
    }

    @TypeConverter
    fun fromPaymentSystemList(value: List<PaymentSystem>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPaymentSystemList(value: String): List<PaymentSystem> {
        val listType = object : TypeToken<List<PaymentSystem>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
    }

    @TypeConverter
    fun fromLong(value: Long?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLong(value: String?): Long? {
        return value?.toLongOrNull() ?: null
    }

    @TypeConverter
    fun fromTimestamp(value: com.google.firebase.Timestamp?): Long? {
        return value?.toDate()?.time
    }

    @TypeConverter
    fun toTimestamp(value: Long?): com.google.firebase.Timestamp? {
        return value?.let { com.google.firebase.Timestamp(it / 1000, ((it % 1000) * 1000000).toInt()) }
    }

    @TypeConverter
    fun fromChargingSlotList(value: List<ChargingSlot>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toChargingSlotList(value: String): List<ChargingSlot> {
        val listType = object : TypeToken<List<ChargingSlot>>() {}.type
        return gson.fromJson(value, listType)
    }
}