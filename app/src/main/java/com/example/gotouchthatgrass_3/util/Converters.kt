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
    
    // Additional converters for handling other types if needed
    @TypeConverter
    fun fromString(value: String?): Date? {
        return value?.let { Date(it.toLongOrNull() ?: 0) }
    }

    @TypeConverter
    fun dateToString(date: Date?): String? {
        return date?.time?.toString()
    }
}