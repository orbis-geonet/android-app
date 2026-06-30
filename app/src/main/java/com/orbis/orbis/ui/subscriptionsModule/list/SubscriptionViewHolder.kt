package com.orbis.orbis.ui.subscriptionsModule.list

import android.app.AlertDialog
import android.content.DialogInterface
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ItemSubscriptionBinding
import com.orbis.orbis.extensions.roundOffDecimal
import com.orbis.orbis.extensions.show
import com.orbis.orbis.models.subscriptions.Subscription
import com.orbis.orbis.ui.groupsModule.adapter.ImageSliderAdapter
import com.orbis.orbis.ui.subscriptionsModule.list.benefits.BenefitsAdapter

class SubscriptionViewHolder(
    private val binding: ItemSubscriptionBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        position: Int,
        item: Subscription,
        isAdmin: Boolean,
        onEditClick: (Int, Subscription) -> Unit,
        onDeleteClick: (Int, Subscription) -> Unit,
        onSubscribeClick: (Int, Subscription, Int) -> Unit,
        onUnsubscribeClick: (Int, Subscription) -> Unit,
        onPhotosClick: (ArrayList<String>) -> Unit,
        onQuantityClick: (Int) -> Unit
    ) {

        binding.tvName.text = item.name
        binding.tvDescription.text = item.description
        binding.tvPrice.text = item.price.roundOffDecimal.toString()
        binding.tvCurrency.text = item.currency
        val benefitsAdapter = BenefitsAdapter()
        benefitsAdapter.submitList(item.benefits)
        binding.rvBenefits.adapter = benefitsAdapter

        if (isAdmin) {
            binding.btnPositive.text = itemView.context.getString(R.string.edit)
        } else {
            if (item.isSubscriber && item.type != "ONE_TIME") {
                binding.btnPositive.text = itemView.context.getString(R.string.cancel)
            } else {
                binding.btnPositive.text = itemView.context.getString(R.string.subscribe)
            }
        }
        binding.btnNegative.show(isAdmin)

        binding.quantityEditText.text = item.quantity.toString()
        binding.quantityEditText.setOnClickListener { onQuantityClick(position) }
        binding.btnPositive.setOnClickListener {
            if (isAdmin) {
                onEditClick(bindingAdapterPosition, item)
            } else {
                if (item.isSubscriber && item.type != "ONE_TIME") {
                    onUnsubscribeClick(bindingAdapterPosition, item)
                } else
                {
                    val quantity = if(item.type == "ONE_TIME") { if(binding.quantityEditText.text.isEmpty() || binding.quantityEditText.text.toString().toInt() == 0) { 1 } else binding.quantityEditText.text.toString().toInt() } else 0
                    onSubscribeClick(bindingAdapterPosition, item, quantity)
                }
            }
        }
        binding.btnNegative.setOnClickListener {
            onDeleteClick(bindingAdapterPosition, item)
        }

        binding.tvPeriod.text = if(item.type == "UNLIMITED" && item.interval == "YEAR") { itemView.context.getString(R.string.yr) } else { itemView.context.getString(R.string.mon) }

        binding.tvPeriod.isVisible = item.type != "ONE_TIME"
        binding.tvPeriodSlash.isVisible = item.type != "ONE_TIME"

        binding.quantityLayout.isVisible = item.type == "ONE_TIME" && !isAdmin

        if(item.type == "INTERVAL")
        {
            binding.intervalPaymentExpTextView.text = "${itemView.context.getString(R.string.`in`)} ${item.period}x, ${itemView.context.getString(R.string.total)}: ${item.currency} ${(item.period * item.price).roundOffDecimal}"
        }

        binding.photosLayout.isVisible = item.imagesName?.isNotEmpty() ?: false
        binding.photosLayout.setOnClickListener { item.imagesName?.let { it1 -> onPhotosClick(it1) } }

//        binding.tvDescription.text = "en. 2023-09-28 06:36:12.761 5391-5656/com.orbis.orbis E/StorageUtil: error getting token java.util.concurrent.ExecutionException: com.google.firebase.internal.api.FirebaseNoSignedInUserException: Please sign in before trying to get a token.2023-09-28 06:36:13.861 5391-5656/com.orbis.orbis E/StorageUtil: error getting token java.util.concurrent.ExecutionException: com.google.firebase.internal.api.FirebaseNoSignedInUserException: Please sign in before trying to get a token2023-09-28 06:36:15.524 5391-5649/com.orbis.orbis E/StorageUtil: error getting token java.util.concurrent.ExecutionException: com.google.firebase.internal.api.FirebaseNoSignedInUserException: Please sign in before trying to get a token2023-09-28 06:36:15.539 5391-5647/com.orbis.orbis E/StorageUtil: error getting token java.util.concurrent.ExecutionException: com.google.firebase.internal.api.FirebaseNoSignedInUserException: Please sign in before trying to get a token."

    }
}