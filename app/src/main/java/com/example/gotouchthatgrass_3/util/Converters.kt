package com.example.gotouchthatgrass_3.util

// util/Converters.kt
import androidx.room.TypeConverter
import java.util.*

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Calendar? {
        return value?.let {
            Calendar.getInstance().apply {
                timeInMillis = it
            }
        }
    }

    @TypeConverter
    fun calendarToTimestamp(calendar: Calendar?): Long? {
        return calendar?.timeInMillis
    }
}