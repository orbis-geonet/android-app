package com.orbis.orbis.ui.subscriptionsModule.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.orbis.orbis.databinding.ItemStatisticsHeaderBinding

class HeaderAdapter(
    private val onBackClick: (Int) -> Unit,
    private val onNextClick: (Int) -> Unit,
) : ListAdapter<Header, HeaderViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HeaderViewHolder {
        val binding = ItemStatisticsHeaderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HeaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onBackClick, onNextClick)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Header>() {
            override fun areItemsTheSame(
                oldItem: Header,
                newItem: Header
            ): Boolean =
                oldItem.name == newItem.name

            override fun areContentsTheSame(
                oldItem: Header,
                newItem: Header
            ): Boolean =
                oldItem == newItem
        }
    }
}