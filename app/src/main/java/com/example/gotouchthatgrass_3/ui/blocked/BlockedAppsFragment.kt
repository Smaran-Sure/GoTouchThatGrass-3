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

        setupAppList()
        setupBlockedAppsList()
        setupSearch()

        // Start app blocker service
        requireActivity().startService(Intent(requireContext(), AppBlockerService::class.java))

        binding.btnUnblockAll.setOnClickListener {
            viewModel.unblockAllApps()
            Toast.makeText(context, "All apps unblocked", Toast.LENGTH_SHORT).show()
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

// Adapter for the app list
class AppListAdapter(private val onBlockClick: (AppItem) -> Unit) :
    androidx.recyclerview.widget.ListAdapter<AppItem, AppListAdapter.ViewHolder>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<AppItem>() {
            override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
                return oldItem.packageName == newItem.packageName
            }

            override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
                return oldItem == newItem
            }
        }
    ) {

    private var fullList = listOf<AppItem>()

    override fun submitList(list: List<AppItem>?) {
        super.submitList(list)
        list?.let { fullList = it }
    }

    fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            fullList
        } else {
            fullList.filter {
                it.label.toString().lowercase().contains(query.lowercase())
            }
        }
        super.submitList(filteredList)
    }

    inner class ViewHolder(val binding: com.example.gotouchthatgrass_3.databinding.ItemAppBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = com.example.gotouchthatgrass_3.databinding.ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = getItem(position)
        holder.binding.apply {
            appName.text = app.label
            // Add a package name TextView if needed or remove this line
            appIcon.setImageDrawable(app.icon)
            blockButton.setOnClickListener { onBlockClick(app) }
        }
    }
}

// Adapter for the blocked apps list
class BlockedAppAdapter(private val onUnblockClick: (BlockedAppItem) -> Unit) :
    androidx.recyclerview.widget.ListAdapter<BlockedAppItem, BlockedAppAdapter.ViewHolder>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<BlockedAppItem>() {
            override fun areItemsTheSame(oldItem: BlockedAppItem, newItem: BlockedAppItem): Boolean {
                return oldItem.packageName == newItem.packageName
            }

            override fun areContentsTheSame(oldItem: BlockedAppItem, newItem: BlockedAppItem): Boolean {
                return oldItem == newItem
            }
        }
    ) {

    inner class ViewHolder(val binding: com.example.gotouchthatgrass_3.databinding.ItemBlockedAppBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = com.example.gotouchthatgrass_3.databinding.ItemBlockedAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = getItem(position)
        holder.binding.apply {
            appName.text = app.appName
            appIcon.setImageDrawable(app.icon)
            unblockButton.setOnClickListener { onUnblockClick(app) }
        }
    }
}