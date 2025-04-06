package com.example.gotouchthatgrass_3.ui.blocked

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.gotouchthatgrass_3.db.AppDatabase
import com.example.gotouchthatgrass_3.models.BlockedApp
import kotlinx.coroutines.launch

class BlockedAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getDatabase(application)
    private val blockedAppDao = appDatabase.blockedAppDao()

    // LiveData of all blocked apps
    val blockedApps: LiveData<List<BlockedApp>> = blockedAppDao.getAllBlockedApps()


    /**
     * Adds an app to the block list
     *
     * @param packageName The package name of the app to block
     * @param appName The display name of the app
     */
    fun blockApp(packageName: String, appName: String) {
        viewModelScope.launch {
            val blockedApp = BlockedApp(
                packageName = packageName,
                appName = appName,
                isCurrentlyBlocked = true,
                blockStartTime = System.currentTimeMillis()
            )
            blockedAppDao.insert(blockedApp)
        }
    }

    /**
     * Removes an app from the block list
     *
     * @param packageName The package name of the app to unblock
     */
    fun unblockApp(packageName: String) {
        viewModelScope.launch {
            blockedAppDao.getAppByPackageName(packageName)?.let { app ->
                val updatedApp = app.copy(isCurrentlyBlocked = false)
                blockedAppDao.update(updatedApp)
            }
        }
    }

    /**
     * Unblocks all apps in the block list
     */
    fun unblockAllApps() {
        viewModelScope.launch {
            blockedAppDao.unblockAllApps()
        }
    }

    /**
     * Checks if an app is currently blocked
     *
     * @param packageName The package name to check
     * @return true if the app is blocked, false otherwise
     */
    suspend fun isAppBlocked(packageName: String): Boolean {
        val app = blockedAppDao.getAppByPackageName(packageName)
        return app?.isCurrentlyBlocked == true
    }
}