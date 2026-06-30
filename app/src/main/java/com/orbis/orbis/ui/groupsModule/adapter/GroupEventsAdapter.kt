package com.orbis.orbis.ui.groupsModule.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.orbis.orbis.R
import com.orbis.orbis.ui.commentModule.views.CommentDialogFragment
import com.orbis.orbis.ui.eventsModule.views.EventDetailsDialogFragment
import com.orbis.orbis.ui.groupsModule.viewModel.Groups
import java.util.*


class GroupEventsAdapter(private val interaction: EventCardInteraction?, context:Context) :
    RecyclerView.Adapter<GroupEventsAdapter.ViewHolder>() {
    private var list: ArrayList<Groups>? = null
    private var mContext:Context

    fun setList(list: ArrayList<Groups>?) {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_event_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(position)
    }

    override fun getItemCount(): Int {
        return list?.size!!
    }

    init {
        mContext=context
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardview: ConstraintLayout
        private val event_cl: ConstraintLayout
        private val go_btn: Button
        fun bindData(position: Int) {


            cardview.setOnClickListener { view: View? ->
                interaction?.onItemClicked()
            }
            go_btn.setOnClickListener { view: View? ->
                showEventDetailsSheet()
            }
        }

        init {
            cardview = itemView.findViewById(R.id.card_id)
            event_cl = itemView.findViewById(R.id.event_cl)
            go_btn = itemView.findViewById(R.id.go_btn)
        }
    }

    interface EventCardInteraction {
        fun onItemClicked()
    }

    private fun showEventDetailsSheet() {

    }

}
