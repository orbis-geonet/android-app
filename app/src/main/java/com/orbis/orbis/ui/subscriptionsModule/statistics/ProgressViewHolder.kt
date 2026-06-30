package com.orbis.orbis.ui.subscriptionsModule.statistics

import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.databinding.ItemStatisticsProgressBinding

class ProgressViewHolder(
    private val binding: ItemStatisticsProgressBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: Progress, adapter: ProgressAdapter) {
        val percentage = (item.value.toDouble() / item.total) * 100
        binding.progressBar.max = 100
        binding.progressBar.progress = percentage.toInt()
        binding.progressBar.alpha = if (item.isSelected) 1f else 0.5f
        binding.tvTitle.text = if (item.isSelected) item.title else ""
        binding.root.setOnClickListener {
            adapter.currentList.map { it.isSelected = false }
            item.isSelected = true
            adapter.notifyDataSetChanged()
        }
    }
}