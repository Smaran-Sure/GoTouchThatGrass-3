package com.example.gotouchthatgrass_3.db

// db/BlockedAppDao.kt
import android.database.Cursor
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
    fun insert(app: BlockedApp): Long

    @Update
    fun update(app: BlockedApp): Int

    @Delete
    fun delete(app: BlockedApp): Int

    @Query("UPDATE blocked_apps SET isCurrentlyBlocked = 0 WHERE isCurrentlyBlocked = 1")
    fun unblockAllApps(): Int

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    fun getAppByPackageName(packageName: String): BlockedApp?
}