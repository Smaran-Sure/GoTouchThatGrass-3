package com.example.gotouchthatgrass_3

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gotouchthatgrass_3.databinding.ActivitySettingsBinding
import com.example.gotouchthatgrass_3.util.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializeSettings()
        setupListeners()
    }

    private fun initializeSettings() {
        // Initialize notification switch
        binding.notificationsSwitch.isChecked = preferenceManager.notificationsEnabled

        // Initialize notification time
        updateNotificationTimeText()

        // Initialize streak count
        binding.currentStreakText.text = getString(R.string.current_streak, preferenceManager.streak)
    }

    private fun updateNotificationTimeText() {
        val timeString = preferenceManager.notificationTime
        try {
            val parts = timeString.split(":")
            val hour = parts[0].toInt()
            val minute = parts[0].toInt()

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            binding.notificationTimeText.text = timeFormat.format(calendar.time)
        } catch (e: Exception) {
            binding.notificationTimeText.text = timeString
        }
    }

    private fun setupListeners() {
        // Notification switch
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.notificationsEnabled = isChecked
            binding.notificationTimeLayout.isEnabled = isChecked
        }

        // Notification time picker
        binding.notificationTimeLayout.setOnClickListener {
            if (!binding.notificationsSwitch.isChecked) return@setOnClickListener

            val timeString = preferenceManager.notificationTime
            val parts = timeString.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    preferenceManager.notificationTime = "$selectedHour:$selectedMinute"
                    updateNotificationTimeText()
                },
                hour,
                minute,
                false
            ).show()
        }

        // Reset streak button
        binding.resetStreakButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_streak_title))
                .setMessage(getString(R.string.reset_streak_message))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    preferenceManager.resetStreak()
                    binding.currentStreakText.text = getString(R.string.current_streak, 0)
                    Toast.makeText(this, getString(R.string.streak_reset), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }

        // About button
        binding.aboutButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.about_title))
                .setMessage(getString(R.string.about_message))
                .setPositiveButton(getString(R.string.ok), null)
                .show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}