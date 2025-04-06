package com.example.gotouchthatgrass_3.ui.stats

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentStatsBinding
import com.example.gotouchthatgrass_3.models.Challenge
import com.example.gotouchthatgrass_3.util.PreferenceManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private lateinit var statsViewModel: StatsViewModel
    private lateinit var adapter: ChallengeHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        statsViewModel = ViewModelProvider(this).get(StatsViewModel::class.java)

        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        displayCurrentStreak()
        observeChallenges()
    }

    private fun setupRecyclerView() {
        adapter = ChallengeHistoryAdapter()
        binding.challengeHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.challengeHistoryRecyclerView.adapter = adapter
    }

    private fun displayCurrentStreak() {
        val preferenceManager = PreferenceManager(requireContext())
        binding.streakCountText.text = preferenceManager.streak.toString()

        // Get the last challenge date
        val timestamp = preferenceManager.lastChallengeTimestamp
        if (timestamp > 0) {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val dateString = dateFormat.format(Date(timestamp))
            binding.lastChallengeText.text = getString(R.string.last_challenge_date, dateString)
        } else {
            binding.lastChallengeText.text = getString(R.string.no_challenges_yet)
        }
    }

    private fun observeChallenges() {
        statsViewModel.getAllChallenges().observe(viewLifecycleOwner) { challenges ->
            if (challenges.isEmpty()) {
                binding.noDataText.visibility = View.VISIBLE
                binding.challengeHistoryRecyclerView.visibility = View.GONE
            } else {
                binding.noDataText.visibility = View.GONE
                binding.challengeHistoryRecyclerView.visibility = View.VISIBLE
                adapter.submitList(challenges)

                // Update total challenges count
                binding.totalChallengesText.text = challenges.size.toString()

                // Calculate success rate
                val successfulChallenges = challenges.count { it.isSuccessful }
                val successRate = if (challenges.isNotEmpty()) {
                    (successfulChallenges * 100 / challenges.size)
                } else {
                    0
                }
                binding.successRateText.text = "$successRate%"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ChallengeHistoryAdapter : androidx.recyclerview.widget.ListAdapter<Challenge, ChallengeHistoryAdapter.ChallengeViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Challenge>() {
        override fun areItemsTheSame(oldItem: Challenge, newItem: Challenge): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Challenge, newItem: Challenge): Boolean {
            return oldItem == newItem
        }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = com.example.gotouchthatgrass_3.databinding.ItemChallengeHistoryBinding.inflate(inflater, parent, false)
        return ChallengeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChallengeViewHolder(
        private val binding: com.example.gotouchthatgrass_3.databinding.ItemChallengeHistoryBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(challenge: Challenge) {
            // Format date
            val dateFormat = SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault())
            binding.challengeDateText.text = dateFormat.format(Date(challenge.timestamp))

            // Set status
            binding.statusText.text = if (challenge.isSuccessful) {
                binding.statusText.setTextColor(binding.root.context.getColor(R.color.success_green))
                binding.root.context.getString(R.string.success)
            } else {
                binding.statusText.setTextColor(binding.root.context.getColor(R.color.failure_red))
                binding.root.context.getString(R.string.failure)
            }

            // Set notes if any
            if (challenge.notes.isNotEmpty()) {
                binding.notesText.visibility = View.VISIBLE
                binding.notesText.text = challenge.notes
            } else {
                binding.notesText.visibility = View.GONE
            }
            
            // Handle the challenge image safely
            try {
                if (challenge.photoPath.isNotEmpty()) {
                    val imgFile = File(challenge.photoPath)
                    if (imgFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        binding.challengeImage.setImageBitmap(bitmap)
                    } else {
                        // Set a placeholder if the file doesn't exist
                        binding.challengeImage.setImageResource(R.drawable.ic_image)
                    }
                } else {
                    // Set a placeholder if no photo path
                    binding.challengeImage.setImageResource(R.drawable.ic_image)
                }
            } catch (e: Exception) {
                // If anything goes wrong, use a placeholder
                binding.challengeImage.setImageResource(R.drawable.ic_image)
            }
        }
    }
}