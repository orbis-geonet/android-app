package com.orbis.orbis.ui.subscriptionsModule.list.benefits

import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.databinding.ItemSubscriptionBenefitBinding

class BenefitViewHolder(
    private val binding: ItemSubscriptionBenefitBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: String) {
        binding.tvBenefit.text = item
    }
}