package com.orbis.orbis.ui.subscriptionsModule.create.benefits

import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.databinding.ItemSubscriptionBenefitMoreBinding

class MoreBenefitViewHolder(
    private val binding: ItemSubscriptionBenefitMoreBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(adapter: BenefitsAdapter) {
        binding.root.setOnClickListener {
            adapter.add()
        }
    }
}