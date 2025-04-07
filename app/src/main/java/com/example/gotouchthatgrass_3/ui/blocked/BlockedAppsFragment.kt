package com.example.gotouchthatgrass_3.ui.blocked

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentBlockedAppsBinding
import com.example.gotouchthatgrass_3.service.AppBlockerService

class BlockedAppsFragment : Fragment() {

    private var _binding: FragmentBlockedAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BlockedAppsViewModel
    private lateinit var appAdapter: AppListAdapter
    private lateinit var blockedAppAdapter: BlockedAppAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(BlockedAppsViewModel::class.java)
        _binding = FragmentBlockedAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply styles to UI elements
        binding.blockedAppsTitle.setTextAppearance(R.style.TextAppearance_App_Headline)
        binding.availableAppsTitle.setTextAppearance(R.style.TextAppearance_App_Headline)
        binding.noBlockedAppsText.setTextAppearance(R.style.TextAppearance_App_Subtitle)
        
        // Set up button style
        binding.btnUnblockAll.setBackgroundColor(resources.getColor(R.color.colorSecondary, null))
        binding.btnUnblockAll.setTextColor(resources.getColor(R.color.white, null))
        
        setupAppList()
        setupBlockedAppsList()
        setupSearch()

        // Start app blocker service
        requireActivity().startService(Intent(requireContext(), AppBlockerService::class.java))

        binding.btnUnblockAll.setOnClickListener {
            viewModel.unblockAllApps()
            Toast.makeText(context, getString(R.string.unblock_all), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAppList() {
        appAdapter = AppListAdapter { app ->
            viewModel.blockApp(app.packageName, app.label.toString())
        }

        binding.recyclerViewApps.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = appAdapter
        }

        // Load installed apps
        val packageManager = requireContext().packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && // Non-system apps
                        app.packageName != requireContext().packageName && // Not our app
                        packageManager.getLaunchIntentForPackage(app.packageName) != null // Has launcher
            }
            .map { app ->
                AppItem(
                    packageName = app.packageName,
                    label = packageManager.getApplicationLabel(app),
                    icon = packageManager.getApplicationIcon(app.packageName)
                )
            }
            .sortedBy { it.label.toString().lowercase() }

        appAdapter.submitList(installedApps)
    }

    private fun setupBlockedAppsList() {
        blockedAppAdapter = BlockedAppAdapter { app ->
            viewModel.unblockApp(app.packageName)
        }

        binding.recyclerViewBlockedApps.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = blockedAppAdapter
        }

        viewModel.blockedApps.observe(viewLifecycleOwner) { blockedApps ->
            val packageManager = requireContext().packageManager
            val blockedAppItems = blockedApps.map { app ->
                try {
                    BlockedAppItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        icon = packageManager.getApplicationIcon(app.packageName)
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    // App was uninstalled but still in blocked list
                    viewModel.unblockApp(app.packageName)
                    null
                }
            }.filterNotNull()

            blockedAppAdapter.submitList(blockedAppItems)

            // Update UI visibility based on blocked apps list
            if (blockedAppItems.isEmpty()) {
                binding.blockedAppsTitle.visibility = View.GONE
                binding.recyclerViewBlockedApps.visibility = View.GONE
                binding.btnUnblockAll.visibility = View.GONE
                binding.noBlockedAppsText.visibility = View.VISIBLE
            } else {
                binding.blockedAppsTitle.visibility = View.VISIBLE
                binding.recyclerViewBlockedApps.visibility = View.VISIBLE
                binding.btnUnblockAll.visibility = View.VISIBLE
                binding.noBlockedAppsText.visibility = View.GONE
            }
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                appAdapter.filter(newText ?: "")
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Helper classes for the adapters
data class AppItem(
    val packageName: String,
    val label: CharSequence,
    val icon: android.graphics.drawable.Drawable
)

data class BlockedAppItem(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable
)

// Using the standalone AppListAdapter class from AppListAdapter.kt

// Using the standalone BlockedAppAdapter class from BlockedAppAdapter.kt