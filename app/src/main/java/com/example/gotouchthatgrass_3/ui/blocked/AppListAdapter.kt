package com.example.gotouchthatgrass_3.ui.blocked

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gotouchthatgrass_3.databinding.ItemAppBinding

class AppListAdapter(private val onBlockClick: (AppItem) -> Unit) :
    ListAdapter<AppItem, AppListAdapter.ViewHolder>(AppDiffCallback()) {

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

    inner class ViewHolder(val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = getItem(position)
        with(holder.binding) {
            appName.text = app.label
            appIcon.setImageDrawable(app.icon)
            blockButton.setOnClickListener {
                onBlockClick(app)
            }
        }
    }
}

class AppDiffCallback : DiffUtil.ItemCallback<AppItem>() {
    override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
        return oldItem.packageName == newItem.packageName
    }

    override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
        return oldItem == newItem
    }
}