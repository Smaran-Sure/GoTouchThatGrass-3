package com.example.gotouchthatgrass_3.models

// models/BlockedApp.kt
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isCurrentlyBlocked: Boolean = true,
    val blockStartTime: Long = System.currentTimeMillis()
)

