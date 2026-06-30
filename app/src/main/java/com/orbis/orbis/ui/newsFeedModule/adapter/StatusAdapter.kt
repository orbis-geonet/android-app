package com.orbis.orbis.ui.newsFeedModule.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.devlomi.circularstatusview.CircularStatusView
import com.orbis.orbis.R
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.ui.groupsModule.viewModel.Groups
import java.util.*


class StatusAdapter(private val interaction: StatusCardInteraction?, context:Context) :
    RecyclerView.Adapter<StatusAdapter.ViewHolder>() {
    private var list: ArrayList<Groups>? = null
    private var mContext:Context

    fun setList(list: ArrayList<Groups>?) {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_status_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

    }

    override fun getItemCount(): Int {
        return list?.size!!
    }

    init {
        mContext=context
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    interface StatusCardInteraction {
        fun onStatusClicked()
        fun onTitleClick(group: GroupDetails)
    }
}
