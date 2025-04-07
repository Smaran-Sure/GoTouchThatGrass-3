package com.example.gotouchthatgrass_3

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.gotouchthatgrass_3.databinding.ActivityGrassDetectionBinding
import com.example.gotouchthatgrass_3.db.AppDatabase
import com.example.gotouchthatgrass_3.models.Challenge
import com.example.gotouchthatgrass_3.util.AppBlockManager
import com.example.gotouchthatgrass_3.util.GrassDetector
import com.example.gotouchthatgrass_3.util.NotificationHelper
import com.example.gotouchthatgrass_3.util.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GrassDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGrassDetectionBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var grassDetector: GrassDetector
    private lateinit var appBlockManager: AppBlockManager
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var appDatabase: AppDatabase

    private var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGrassDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        grassDetector = GrassDetector(this)
        appBlockManager = AppBlockManager(this)
        preferenceManager = PreferenceManager(this)
        appDatabase = AppDatabase.getDatabase(this)

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        binding.confirmButton.setOnClickListener {
            verifyGrassAndUnblock()
        }

        binding.retryButton.setOnClickListener {
            binding.previewImage.visibility = View.GONE
            binding.confirmButton.visibility = View.GONE
            binding.retryButton.visibility = View.GONE
            binding.viewFinder.visibility = View.VISIBLE
            binding.captureButton.visibility = View.VISIBLE
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Camera setup failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Create temporary file to store the image
        val photoFile = File(
            getOutputDirectory(),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    this@GrassDetectionActivity.photoFile = photoFile
                    showPreview(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@GrassDetectionActivity,
                        "Failed to capture image: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun showPreview(photoFile: File) {
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        binding.previewImage.setImageBitmap(bitmap)

        // Update UI
        binding.viewFinder.visibility = View.GONE
        binding.captureButton.visibility = View.GONE
        binding.previewImage.visibility = View.VISIBLE
        binding.confirmButton.visibility = View.VISIBLE
        binding.retryButton.visibility = View.VISIBLE
    }

    private fun verifyGrassAndUnblock() {
        binding.progressBar.visibility = View.VISIBLE
        binding.confirmButton.isEnabled = false
        binding.retryButton.isEnabled = false
        binding.instructionText.text = "Analyzing your photo..."

        lifecycleScope.launch {
            val photoFile = this@GrassDetectionActivity.photoFile ?: return@launch

            // Get bitmap for analysis
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(photoFile.absolutePath)
            }

            // Check if the image contains grass using both color detection and ML
            val containsGrass = grassDetector.isGrassInImage(bitmap)

            if (containsGrass) {
                // Save successful challenge to database
                saveChallenge(photoFile.absolutePath, true)

                // Unblock all apps
                appBlockManager.unblockAllApps()

                // Update last challenge timestamp and streak
                updateStreak()

                // Show success animation or feedback
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.instructionText.text = "Success! You've touched grass!"
                    binding.instructionText.setBackgroundColor(resources.getColor(R.color.success_green, null))
                }

                // Delay to show the success message
                delay(1500)

                Toast.makeText(
                    this@GrassDetectionActivity,
                    "Great job! Your apps have been unblocked.",
                    Toast.LENGTH_LONG
                ).show()

                // Return to main activity
                startActivity(Intent(this@GrassDetectionActivity, MainActivity::class.java))
                finish()
            } else {
                binding.progressBar.visibility = View.GONE
                binding.confirmButton.isEnabled = true
                binding.retryButton.isEnabled = true
                binding.instructionText.text = "We couldn't detect grass. Try again!"
                binding.instructionText.setBackgroundColor(resources.getColor(R.color.failure_red, null))

                Toast.makeText(
                    this@GrassDetectionActivity,
                    "We couldn't detect grass in your photo. Please try again!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun saveChallenge(photoPath: String, successful: Boolean) {
        try {
            val challenge = Challenge(
                photoPath = photoPath.ifEmpty { "" },  // Ensure photoPath is never null
                isSuccessful = successful,
                notes = ""
            )

            withContext(Dispatchers.IO) {
                appDatabase.challengeDao().insert(challenge)
            }
        } catch (e: Exception) {
            // Log the error but don't crash
            e.printStackTrace()
        }
    }

    private fun updateStreak() {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)

        val lastChallengeTime = preferenceManager.lastChallengeTimestamp
        val lastChallengeCalendar = Calendar.getInstance().apply {
            timeInMillis = lastChallengeTime
        }
        val lastChallengeDay = lastChallengeCalendar.get(Calendar.DAY_OF_YEAR)

        // Set current time as last challenge timestamp
        preferenceManager.lastChallengeTimestamp = System.currentTimeMillis()

        // Update streak
        if (lastChallengeTime == 0L ||
            (today - lastChallengeDay == 1 && calendar.get(Calendar.YEAR) == lastChallengeCalendar.get(Calendar.YEAR)) ||
            (lastChallengeCalendar.get(Calendar.YEAR) < calendar.get(Calendar.YEAR) &&
                    lastChallengeCalendar.get(Calendar.DAY_OF_YEAR) == lastChallengeCalendar.getActualMaximum(Calendar.DAY_OF_YEAR) &&
                    today == 1)
        ) {
            // Increment streak (either first challenge, consecutive day, or consecutive across year boundary)
            val newStreak = preferenceManager.streak + 1
            preferenceManager.streak = newStreak

            // Check for milestone (every 7 days)
            if (newStreak > 0 && newStreak % 7 == 0) {
                NotificationHelper(this).sendStreakMilestoneNotification(newStreak)
            }
        } else if (today != lastChallengeDay || calendar.get(Calendar.YEAR) != lastChallengeCalendar.get(Calendar.YEAR)) {
            // Reset streak if not consecutive
            preferenceManager.streak = 1
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}