package com.orbis.orbis.ui.placesModule.adapter


import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.devlomi.circularstatusview.CircularStatusView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.EachGroupIconItemBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.ui.groupsModule.viewModel.Groups
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.squareup.picasso.Picasso
import java.util.*


class GroupIconAdapter(
    private val interaction: GroupIconCardInteraction?,
    private val context: Context,
    private val items: ArrayList<GroupDetails>
) :
    RecyclerView.Adapter<GroupIconAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_group_icon_item, parent, false)
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding?.data = items[position]
        holder.binding?.cardId?.setOnClickListener {
            interaction?.onIconClicked(items[position])
        }
        ViewUtils.loadGroupPhoto(
            context,
            holder.binding?.groupImage as ImageView,
            items[position].imageName
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachGroupIconItemBinding? = DataBindingUtil.bind(itemView)
    }

    interface GroupIconCardInteraction {
        fun onIconClicked(groupDetails: GroupDetails)
    }
}
