package com.example.gotouchthatgrass_3.util

// util/PreferenceManager.kt

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var appBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_BLOCKING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_APP_BLOCKING_ENABLED, value).apply()

    var lastChallengeTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CHALLENGE_TIMESTAMP, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_CHALLENGE_TIMESTAMP, value).apply()

    var streak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_STREAK, value).apply()

    var notificationTime: String
        get() = prefs.getString(KEY_NOTIFICATION_TIME, "18:00") ?: "18:00"
        set(value) = prefs.edit().putString(KEY_NOTIFICATION_TIME, value).apply()

    fun resetStreak() {
        prefs.edit().putInt(KEY_STREAK, 0).apply()
    }

    companion object {
        private const val PREFS_NAME = "gotouchthatgrass_prefs"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_APP_BLOCKING_ENABLED = "app_blocking_enabled"
        private const val KEY_LAST_CHALLENGE_TIMESTAMP = "last_challenge_timestamp"
        private const val KEY_STREAK = "streak"
        private const val KEY_NOTIFICATION_TIME = "notification_time"
    }
}