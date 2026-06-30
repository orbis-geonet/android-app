package com.orbis.orbis.ui.subscriptionsModule.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.orbis.orbis.databinding.ItemStatisticsProgressBinding

class ProgressAdapter : ListAdapter<Progress, ProgressViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProgressViewHolder {
        val binding = ItemStatisticsProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, this)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Progress>() {
            override fun areItemsTheSame(
                oldItem: Progress,
                newItem: Progress
            ): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(
                oldItem: Progress,
                newItem: Progress
            ): Boolean =
                oldItem == newItem
        }
    }
}