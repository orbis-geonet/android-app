package com.orbis.orbis.ui.subscriptionsModule.statistics

import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.databinding.ItemStatisticsHeaderBinding

class HeaderViewHolder(
    private val binding: ItemStatisticsHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: Header, onBackClick: (Int) -> Unit, onNextClick: (Int) -> Unit) {
        binding.tvTitle.text = item.name
        binding.tvTotalSubscriptions.text = item.totalSubscriptions.toString()
        binding.tvTotalRevenue.text = item.totalRevenue.toString()
        binding.ivArrowLeft.setOnClickListener {
            onBackClick(bindingAdapterPosition)
        }
        binding.ivArrowRight.setOnClickListener {
            onNextClick(bindingAdapterPosition)
        }
    }
}