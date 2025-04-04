package com.example.gotouchthatgrass_3.util

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.*

class DailyReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val preferenceManager = PreferenceManager(context)
    private val notificationHelper = NotificationHelper(context)

    override fun doWork(): Result {
        // Check if notifications are enabled
        if (!preferenceManager.notificationsEnabled) {
            return Result.success()
        }

        // Get current time and compare with scheduled notification time
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val notificationTimeParts = preferenceManager.notificationTime.split(":")
        val notificationHour = notificationTimeParts[0].toInt()
        val notificationMinute = notificationTimeParts[1].toInt()

        // Check if it's time to send notification (within a 15 min window)
        if (isTimeToNotify(currentHour, currentMinute, notificationHour, notificationMinute)) {
            // Check if user has already completed challenge today
            val lastChallengeTime = preferenceManager.lastChallengeTimestamp
            val lastChallengeCalendar = Calendar.getInstance().apply {
                timeInMillis = lastChallengeTime
            }

            val isSameDay = calendar.get(Calendar.DAY_OF_YEAR) == lastChallengeCalendar.get(Calendar.DAY_OF_YEAR) &&
                    calendar.get(Calendar.YEAR) == lastChallengeCalendar.get(Calendar.YEAR)

            // Only send notification if user hasn't completed challenge today
            if (!isSameDay) {
                notificationHelper.sendDailyChallengeReminder()
            }
        }

        return Result.success()
    }

    private fun isTimeToNotify(currentHour: Int, currentMinute: Int, targetHour: Int, targetMinute: Int): Boolean {
        // Convert times to minutes since midnight for easier comparison
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        val targetTimeInMinutes = targetHour * 60 + targetMinute

        // Check if current time is within 15 minutes of target time
        return Math.abs(currentTimeInMinutes - targetTimeInMinutes) <= 15
    }
}