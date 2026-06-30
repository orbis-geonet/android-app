package com.orbis.orbis.ui.subscriptionsModule.mySubscriptions

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.orbis.orbis.databinding.ItemSubscriptionProfileBinding
import com.orbis.orbis.models.subscriptions.Subscription

class MySubscriptionsAdapter(
    private val onCancelClick: (Int, Subscription) -> Unit,
    private val onCodeListClick: (ArrayList<String>) -> Unit,
    private val context: Context,
) : ListAdapter<Subscription, MySubscriptionViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MySubscriptionViewHolder {
        val binding = ItemSubscriptionProfileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MySubscriptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MySubscriptionViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(context, item, onCancelClick, onCodeListClick)
    }

    fun addList(newList: List<Subscription>)
    {
        val list = currentList.toMutableList()
        list.addAll(newList)
        submitList(list)
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