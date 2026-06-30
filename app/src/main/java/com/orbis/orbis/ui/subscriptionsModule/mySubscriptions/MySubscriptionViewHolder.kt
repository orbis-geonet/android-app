package com.orbis.orbis.ui.subscriptionsModule.mySubscriptions

import android.content.Context
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ItemSubscriptionProfileBinding
import com.orbis.orbis.extensions.roundOffDecimalString
import com.orbis.orbis.models.subscriptions.Subscription

class MySubscriptionViewHolder(
    private val binding: ItemSubscriptionProfileBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(context: Context, item: Subscription, onCancelClick: (Int, Subscription) -> Unit, onCodeListClick: (ArrayList<String>) -> Unit) {
        binding.tvGroup.text = item.groupName
        binding.tvSubscription.text = "${item.name} - ${item.price.roundOffDecimalString}"
        binding.btnCancel.setOnClickListener {
            onCancelClick(bindingAdapterPosition, item)
        }

        item.codes?.let { codes ->
            binding.showCodesButton.isVisible = codes.count() != 1
            binding.codeTextView.isVisible = codes.count() == 1

            if(codes.count() == 1)
            {
                binding?.codeTextView?.text =  context.resources.getString(R.string.code, codes.first())
            }
            else
            {
                binding.showCodesButton.setOnClickListener {
                    onCodeListClick(codes)
                }
            }
        }
    }
}