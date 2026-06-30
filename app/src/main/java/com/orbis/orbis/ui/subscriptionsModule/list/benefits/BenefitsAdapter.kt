package com.orbis.orbis.ui.subscriptionsModule.list.benefits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.orbis.orbis.databinding.ItemSubscriptionBenefitBinding

class BenefitsAdapter : ListAdapter<String, BenefitViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BenefitViewHolder {
        val binding = ItemSubscriptionBenefitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BenefitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BenefitViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(
                oldItem: String,
                newItem: String
            ): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(
                oldItem: String,
                newItem: String
            ): Boolean =
                oldItem == newItem
        }
    }
}