package com.example.gotouchthatgrass_3.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.gotouchthatgrass_3.service.AppBlockerService
import com.example.gotouchthatgrass_3.util.PreferenceManager

/**
 * Broadcast receiver that starts the AppBlockerService when the device boots up
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting AppBlockerService")
            
            // Check if app blocking is enabled in preferences
            val preferenceManager = PreferenceManager(context)
            if (preferenceManager.appBlockingEnabled) {
                // Start the app blocker service
                val serviceIntent = Intent(context, AppBlockerService::class.java)
                context.startService(serviceIntent)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}