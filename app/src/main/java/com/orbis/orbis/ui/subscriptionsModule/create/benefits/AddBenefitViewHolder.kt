package com.orbis.orbis.ui.subscriptionsModule.create.benefits

import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.databinding.ItemSubscriptionBenefitAddBinding

class AddBenefitViewHolder(
    private val binding: ItemSubscriptionBenefitAddBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: String, adapter: BenefitsAdapter) {
        binding.etBenefit.setText(item)
        binding.etBenefit.doAfterTextChanged {
            adapter.setData(bindingAdapterPosition, it.toString())
        }
        binding.ivDelete.setOnClickListener {
            adapter.remove(bindingAdapterPosition)
        }
    }
}