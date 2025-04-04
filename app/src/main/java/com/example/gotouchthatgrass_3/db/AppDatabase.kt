package com.example.gotouchthatgrass_3.db

// db/AppDatabase.kt

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gotouchthatgrass_3.models.BlockedApp
import com.example.gotouchthatgrass_3.models.Challenge
import com.example.gotouchthatgrass_3.util.Converters

@Database(entities = [BlockedApp::class, Challenge::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun challengeDao(): ChallengeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gotouchthatgrass_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}