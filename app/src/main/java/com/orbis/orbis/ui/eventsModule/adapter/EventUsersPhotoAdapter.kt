package com.orbis.orbis.ui.eventsModule.adapter


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
import com.orbis.orbis.ui.eventsModule.viewModel.Users
import com.orbis.orbis.ui.groupsModule.viewModel.Groups
import com.orbis.orbis.utils.ViewUtils
import java.util.*


class EventUsersPhotoAdapter(private val interaction: UserPhotoCardInteraction?, context:Context) :
    RecyclerView.Adapter<EventUsersPhotoAdapter.ViewHolder>() {
    private var list: ArrayList<Users>? = null
    private var mContext:Context

    fun setList(list: ArrayList<Users>?) {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_user_photo_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(position)
    }

    override fun getItemCount(): Int {
        return 10
    }

    init {
        mContext=context
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardview: ConstraintLayout
        private val user_photo_iv: ImageView
        fun bindData(position: Int) {


            cardview.setOnClickListener { view: View? ->
                interaction?.onIconClicked()
            }
        }

        init {
            cardview = itemView.findViewById(R.id.card_id)
            user_photo_iv = itemView.findViewById(R.id.user_photo_iv)
        }
    }

    interface UserPhotoCardInteraction {
        fun onIconClicked()
    }
}
