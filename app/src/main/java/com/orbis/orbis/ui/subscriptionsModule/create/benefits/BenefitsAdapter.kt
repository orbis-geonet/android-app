package com.orbis.orbis.ui.subscriptionsModule.create.benefits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.databinding.ItemSubscriptionBenefitAddBinding
import com.orbis.orbis.databinding.ItemSubscriptionBenefitMoreBinding

class BenefitsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ADD = 0
        private const val TYPE_MORE = 1
    }

    private val items: ArrayList<String> = ArrayList()

    init {
        for (i in 0..3) {
            items.add("")
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_MORE -> {
                val binding =
                    ItemSubscriptionBenefitMoreBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                return MoreBenefitViewHolder(binding)
            }
            else -> {
                val binding =
                    ItemSubscriptionBenefitAddBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                return AddBenefitViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder.itemViewType) {
            TYPE_ADD -> {
                val viewHolder = holder as AddBenefitViewHolder
                viewHolder.bind(item, this)
            }
            TYPE_MORE -> {
                val viewHolder = holder as MoreBenefitViewHolder
                viewHolder.bind(this)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size - 1) TYPE_MORE else TYPE_ADD
    }

    fun add() {
        items.add("")
        notifyItemInserted(items.size - 1)
    }

    fun remove(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun setData(position: Int, value: String) {
        items[position] = value
    }

    fun setData(items: List<String>) {
        if (items.isEmpty()) return
        this.items.clear()
        this.items.addAll(items)
        this.items.add("") // empty item for TYPE_MORE
        notifyDataSetChanged()
    }

    fun getData() = items.filter { it.isNotEmpty() }
}