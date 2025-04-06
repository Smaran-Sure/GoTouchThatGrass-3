package com.example.gotouchthatgrass_3.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.gotouchthatgrass_3.GrassDetectionActivity
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentHomeBinding
import com.example.gotouchthatgrass_3.util.PreferenceManager
import com.example.gotouchthatgrass_3.ui.home.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        preferenceManager = PreferenceManager(requireContext())

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupClickListeners()
        observeBlockedApps()
    }

    private fun setupUI() {
        updateStreak()
        checkChallengeStatus()
    }

    private fun updateStreak() {
        val streak = preferenceManager.streak
        binding.streakText.text = resources.getQuantityString(
            R.plurals.day_streak, streak, streak
        )
    }

    private fun checkChallengeStatus() {
        val lastChallengeTime = preferenceManager.lastChallengeTimestamp

        if (lastChallengeTime > 0) {
            val lastChallengeCalendar = Calendar.getInstance().apply {
                timeInMillis = lastChallengeTime
            }
            val today = Calendar.getInstance()

            val isSameDay = lastChallengeCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                    lastChallengeCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)

            if (isSameDay) {
                // Challenge completed today
                binding.statusText.text = getString(R.string.challenge_completed_today)
                binding.statusIcon.setImageResource(R.drawable.ic_check_circle)
                binding.startChallengeButton.text = getString(R.string.challenge_again)

                // Format time
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeString = timeFormat.format(Date(lastChallengeTime))
                binding.lastChallengeText.text = getString(R.string.last_challenge_time, timeString)
                binding.lastChallengeText.visibility = View.VISIBLE
            } else {
                // Need to complete challenge today
                binding.statusText.text = getString(R.string.challenge_needed_today)
                binding.statusIcon.setImageResource(R.drawable.ic_warning)
                binding.startChallengeButton.text = getString(R.string.start_challenge)
                binding.lastChallengeText.visibility = View.GONE
            }
        } else {
            // First time user
            binding.statusText.text = getString(R.string.welcome_message)
            binding.statusIcon.setImageResource(R.drawable.ic_info)
            binding.startChallengeButton.text = getString(R.string.start_first_challenge)
            binding.lastChallengeText.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.startChallengeButton.setOnClickListener {
            startActivity(Intent(requireContext(), GrassDetectionActivity::class.java))
        }
    }

    private fun observeBlockedApps() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.getBlockedApps().observe(viewLifecycleOwner) { apps ->
                if (apps.isEmpty()) {
                    binding.blockedAppsContainer.visibility = View.GONE
                    binding.noBlockedAppsText.visibility = View.VISIBLE
                } else {
                    binding.blockedAppsContainer.visibility = View.VISIBLE
                    binding.noBlockedAppsText.visibility = View.GONE
                    binding.blockedAppsCount.text = apps.size.toString()

                    // Display the first 3 app names
                    val displayNames = apps.take(3).joinToString(", ") { it.appName }
                    val remainingCount = if (apps.size > 3) apps.size - 3 else 0

                    if (remainingCount > 0) {
                        binding.blockedAppsNames.text = getString(
                            R.string.blocked_apps_list_with_more,
                            displayNames,
                            remainingCount
                        )
                    } else {
                        binding.blockedAppsNames.text = displayNames
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStreak()
        checkChallengeStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}