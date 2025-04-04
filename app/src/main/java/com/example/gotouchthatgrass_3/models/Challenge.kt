package com.example.gotouchthatgrass_3.models

// models/Challenge.kt
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val photoPath: String,
    val notes: String = "",
    val isSuccessful: Boolean = true,
    val date: Calendar = Calendar.getInstance()
)