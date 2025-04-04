package com.example.gotouchthatgrass_3.service


import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gotouchthatgrass_3.GrassDetectionActivity
import com.example.gotouchthatgrass_3.MainActivity
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.db.AppDatabase
import com.example.gotouchthatgrass_3.util.AppBlockManager
import com.example.gotouchthatgrass_3.util.PreferenceManager
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class AppBlockerService : Service() {

    private val TAG = "AppBlockerService"
    private lateinit var appBlockManager: AppBlockManager
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var appDatabase: AppDatabase
    private lateinit var serviceScope: CoroutineScope
    private var monitoringJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        appBlockManager = AppBlockManager(this)
        preferenceManager = PreferenceManager(this)
        appDatabase = AppDatabase.getDatabase(this)
        serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // Create a notification channel for foreground service
        createNotificationChannel()

        // Acquire wake lock to prevent service from being killed
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        // Create foreground notification
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start monitoring apps
        startAppMonitoring()

        // If service gets killed, restart it
        return START_STICKY
    }

    private fun startAppMonitoring() {
        // Cancel any existing monitoring job
        monitoringJob?.cancel()

        // Start a new monitoring job
        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Check if usage stats permission is granted
                    if (!appBlockManager.hasUsageStatsPermission()) {
                        Log.w(TAG, "Usage stats permission not granted")
                        delay(MONITOR_INTERVAL)
                        continue
                    }

                    // Get the current foreground app
                    val foregroundApp = appBlockManager.getCurrentForegroundApp()
                    foregroundApp?.let { packageName ->
                        // Check if the app is in our block list
                        val blockedAppDao = appDatabase.blockedAppDao()
                        val blockedApp = blockedAppDao.getAppByPackageName(packageName)

                        if (blockedApp != null && blockedApp.isCurrentlyBlocked) {
                            Log.d(TAG, "Blocked app detected: $packageName")

                            // Check if user has completed a challenge today
                            val challengeCompleted = hasCompletedChallengeToday()

                            if (!challengeCompleted) {
                                // Launch grass detection activity
                                val intent = Intent(this@AppBlockerService, GrassDetectionActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    putExtra("BLOCKED_APP_PACKAGE", packageName)
                                }
                                startActivity(intent)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in app monitoring: ${e.message}")
                }

                // Check every few seconds
                delay(MONITOR_INTERVAL)
            }
        }
    }

    private suspend fun hasCompletedChallengeToday(): Boolean {
        val challengeDao = appDatabase.challengeDao()
        val lastChallenge = challengeDao.getLastSuccessfulChallenge() ?: return false

        // Check if the last challenge was completed today
        val lastChallengeTime = lastChallenge.timestamp
        val currentTime = System.currentTimeMillis()

        // Convert to calendar to check if it's the same day
        val lastChallengeCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = lastChallengeTime
        }
        val currentCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = currentTime
        }

        return lastChallengeCalendar.get(java.util.Calendar.YEAR) == currentCalendar.get(java.util.Calendar.YEAR) &&
                lastChallengeCalendar.get(java.util.Calendar.DAY_OF_YEAR) == currentCalendar.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Blocker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service that monitors app usage"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GoTouchThatGrass")
            .setContentText("Monitoring app usage")
//            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GoTouchThatGrass:AppBlockerWakeLock"
        ).apply {
            acquire(TimeUnit.HOURS.toMillis(8)) // 8 hour max
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")

        // Cancel all coroutines
        monitoringJob?.cancel()
        serviceScope.cancel()

        // Release wake lock
        releaseWakeLock()

        super.onDestroy()

        // Restart service if it was killed
        val restartIntent = Intent(applicationContext, AppBlockerService::class.java)
        applicationContext.startService(restartIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // No binding
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "app_blocker_channel"
        private const val MONITOR_INTERVAL = 3000L // 3 seconds
    }
}