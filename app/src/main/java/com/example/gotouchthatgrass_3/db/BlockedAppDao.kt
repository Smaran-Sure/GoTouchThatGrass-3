package com.example.gotouchthatgrass_3.db

// db/BlockedAppDao.kt
import androidx.lifecycle.LiveData

import androidx.room.*
import com.example.gotouchthatgrass_3.models.BlockedApp

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps")
    fun getAllBlockedApps(): LiveData<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps WHERE isCurrentlyBlocked = 1")

    fun getCurrentlyBlockedApps(): LiveData<List<BlockedApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: BlockedApp): Long

    @Update
    suspend fun update(app: BlockedApp): Int

    @Delete
    suspend fun delete(app: BlockedApp): Int

    @Query("UPDATE blocked_apps SET isCurrentlyBlocked = 0 WHERE isCurrentlyBlocked = 1")
    suspend fun unblockAllApps(): Int

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): BlockedApp?
}