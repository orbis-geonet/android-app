package com.orbis.orbis.ui.subscriptionsModule.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.orbis.orbis.databinding.ItemSubscriptionBinding
import com.orbis.orbis.models.subscriptions.Subscription

class SubscriptionsListAdapter(
    private val isAdmin: Boolean,
    private val onEditClick: (Int, Subscription) -> Unit,
    private val onDeleteClick: (Int, Subscription) -> Unit,
    private val onSubscribeClick: (Int, Subscription, Int) -> Unit,
    private val onUnsubscribeClick: (Int, Subscription) -> Unit,
    private val onPhotosClick: (ArrayList<String>) -> Unit,
    private val onQuantityClick: (Int) -> Unit
) : ListAdapter<Subscription, SubscriptionViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubscriptionViewHolder {
        val binding =
            ItemSubscriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubscriptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(position, item, isAdmin, onEditClick, onDeleteClick, onSubscribeClick, onUnsubscribeClick, onPhotosClick, onQuantityClick)
    }

    fun update(position: Int, isSubscriber: Boolean) {
        val list = currentList.toMutableList()
        list[position].isSubscriber = isSubscriber
        notifyItemChanged(position)
    }

    fun updateQuantity(position: Int, quantity: Int) {
        val list = currentList.toMutableList()
        list[position].quantity = quantity
        notifyItemChanged(position)
    }

    fun remove(position: Int) {
        val list = currentList.toMutableList()
        list.removeAt(position)
        submitList(list)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Subscription>() {
            override fun areItemsTheSame(
                oldItem: Subscription,
                newItem: Subscription
            ): Boolean =
                oldItem.subscriptionKey == newItem.subscriptionKey

            override fun areContentsTheSame(
                oldItem: Subscription,
                newItem: Subscription
            ): Boolean =
                oldItem == newItem
        }
    }
}