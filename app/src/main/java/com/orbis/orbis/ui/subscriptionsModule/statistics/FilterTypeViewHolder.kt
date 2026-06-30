package com.orbis.orbis.ui.subscriptionsModule.statistics

import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.databinding.ItemStatisticsTypeBinding

class FilterTypeViewHolder(
    private val binding: ItemStatisticsTypeBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: FilterType, onItemClick: (FilterType) -> Unit, adapter: FilterTypesAdapter) {
        binding.tvType.text = item.name
        binding.tvType.isSelected = item.isSelected
        binding.root.setOnClickListener {
            adapter.currentList.map { it.isSelected = it.name == item.name }
            adapter.notifyDataSetChanged()
            onItemClick(item)
        }
    }
}