package com.orbis.orbis.ui.placesModule.adapter


import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.EachGroupCreatePostLayoutBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.ui.settingsModule.viewModel.Place
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.customViews.RoundLinerLayoutNormal
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.collections.ArrayList


class CheckinCreatePlaceAdapter(
    private val interaction: PlaceCardInteraction,
    private val context: Context,
    private val list: ArrayList<GroupDetails>
) :
    RecyclerView.Adapter<CheckinCreatePlaceAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_group_create_post_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding?.data = list[position]


        holder.binding?.cardId?.setOnClickListener {
            interaction.onItemClicked(position)
        }
        ViewUtils.loadGroupPhoto(
            context,
            holder.binding?.profileImage as ImageView,
            list[position].imageName
        )
        holder.binding?.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return list.size
    }



    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachGroupCreatePostLayoutBinding? = DataBindingUtil.bind(itemView)
    }

    interface PlaceCardInteraction {
        fun onItemClicked(position: Int)
    }
}
