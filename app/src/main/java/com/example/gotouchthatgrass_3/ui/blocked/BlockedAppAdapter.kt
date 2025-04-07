package com.example.gotouchthatgrass_3.ui.blocked

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gotouchthatgrass_3.databinding.ItemBlockedAppBinding

class BlockedAppAdapter(private val onUnblockClick: (BlockedAppItem) -> Unit) :
    ListAdapter<BlockedAppItem, BlockedAppAdapter.ViewHolder>(BlockedAppDiffCallback()) {

    inner class ViewHolder(val binding: ItemBlockedAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBlockedAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val blockedApp = getItem(position)
        with(holder.binding) {
            appName.text = blockedApp.appName
            appIcon.setImageDrawable(blockedApp.icon)
            unblockButton.setOnClickListener {
                onUnblockClick(blockedApp)
            }
        }
    }
}

class BlockedAppDiffCallback : DiffUtil.ItemCallback<BlockedAppItem>() {
    override fun areItemsTheSame(oldItem: BlockedAppItem, newItem: BlockedAppItem): Boolean {
        return oldItem.packageName == newItem.packageName
    }

    override fun areContentsTheSame(oldItem: BlockedAppItem, newItem: BlockedAppItem): Boolean {
        return oldItem == newItem
    }
}