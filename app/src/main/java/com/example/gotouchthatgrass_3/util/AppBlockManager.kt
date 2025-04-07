package com.example.gotouchthatgrass_3.util

// util/AppBlockManager.kt

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.gotouchthatgrass_3.MainActivity
import com.example.gotouchthatgrass_3.db.AppDatabase
import com.example.gotouchthatgrass_3.models.BlockedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

class AppBlockManager(private val context: Context) {
    private val appDatabase = AppDatabase.getDatabase(context)
    private val blockedAppDao = appDatabase.blockedAppDao()
    private val preferenceManager = PreferenceManager(context)

    suspend fun addAppToBlockList(packageName: String, appName: String) {
        val blockedApp = BlockedApp(
            packageName = packageName, 
            appName = appName,
            isCurrentlyBlocked = true,
            blockStartTime = System.currentTimeMillis()
        )
        blockedAppDao.insert(blockedApp)
    }

    suspend fun removeAppFromBlockList(packageName: String) {
        blockedAppDao.getAppByPackageName(packageName)?.let {
            blockedAppDao.delete(it)
        }
    }

    suspend fun unblockAllApps() {
        blockedAppDao.unblockAllApps()
    }

    suspend fun isAppBlocked(packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val app = blockedAppDao.getAppByPackageName(packageName)
            app?.isCurrentlyBlocked ?: false
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun getCurrentForegroundApp(): String? {
        if (!hasUsageStatsPermission()) {
            return null
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - TimeUnit.MINUTES.toMillis(5),
            time
        )

        if (stats.isEmpty()) {
            return null
        }

        // Find the last used app
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    fun launchMainActivity() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}