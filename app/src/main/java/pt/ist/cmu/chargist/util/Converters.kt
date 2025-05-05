package pt.ist.cmu.chargist.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
}