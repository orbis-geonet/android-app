package com.orbis.orbis.ui.subscriptionsModule.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.orbis.orbis.databinding.ItemStatisticsTypeBinding

class FilterTypesAdapter(
    private val onItemClick: (FilterType) -> Unit,
) : ListAdapter<FilterType, FilterTypeViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FilterTypeViewHolder {
        val binding = ItemStatisticsTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FilterTypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterTypeViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClick, this)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FilterType>() {
            override fun areItemsTheSame(
                oldItem: FilterType,
                newItem: FilterType
            ): Boolean =
                oldItem.name == newItem.name

            override fun areContentsTheSame(
                oldItem: FilterType,
                newItem: FilterType
            ): Boolean =
                oldItem == newItem
        }
    }
}