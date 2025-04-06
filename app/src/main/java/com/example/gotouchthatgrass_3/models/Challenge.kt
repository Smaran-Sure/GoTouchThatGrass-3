package com.example.gotouchthatgrass_3.models

// models/Challenge.kt
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var timestamp: Long = System.currentTimeMillis(),
    var photoPath: String = "", // Made photoPath have a default empty string value
    var notes: String = "",
    var isSuccessful: Boolean = true
    // Removed dateTimestamp field as it was redundant with timestamp
)