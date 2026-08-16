package com.loadpredictor.data.local

import androidx.room.TypeConverter
import com.loadpredictor.domain.model.SimSlot

/**
 * Room TypeConverters for custom types such as [SimSlot].
 */
class Converters {
    @TypeConverter
    fun fromSimSlot(simSlot: SimSlot): String {
        return simSlot.name
    }

    @TypeConverter
    fun toSimSlot(value: String): SimSlot {
        return runCatching { SimSlot.valueOf(value) }.getOrDefault(SimSlot.SIM_1)
    }
}
