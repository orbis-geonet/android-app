package com.orbis.orbis.ui.groupsModule.adapter


import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.EachGroupRowLayoutBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.squareup.picasso.Picasso
import java.util.*


class GroupListAdapter(
    private val groups: ArrayList<GroupDetails>,
    val interaction: GroupsCardInteraction,
    val context: Context,
) :
    RecyclerView.Adapter<GroupListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_group_row_layout, parent, false)
        )
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.binding?.data = groups[position]
        ViewUtils.loadGroupPhoto(
            context,
            holder.binding?.shadowCircle as ImageView,
            groups[position].imageName
        )

        Log.d("groupRankCheck", groups[position].name + " " + groups[position].rankDiffType)
        Utils.populateRankIcon(context, groups[position].rankDiffType, holder.binding?.statusIv)
        holder.binding?.cardId?.setOnClickListener {
            interaction.onItemClicked(position)
        }

        holder.binding.executePendingBindings()
    }


    override fun getItemCount(): Int {
        return groups.size
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachGroupRowLayoutBinding? = DataBindingUtil.bind(itemView)
    }




    interface GroupsCardInteraction {
        fun onItemClicked(position: Int)
    }
}
