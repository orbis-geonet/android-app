package com.orbis.orbis.ui.ProfileModule.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ItemFollowedPlaceBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.placesModule.views.PlaceActivity
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils


class FollowedPlaceAdapter(
    private val context: Context,
    private val places: ArrayList<PlaceDetails>,
    val interaction: FollowedPlaceInteraction
) :
    RecyclerView.Adapter<FollowedPlaceAdapter.ViewHolder>() {


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ItemFollowedPlaceBinding? = DataBindingUtil.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_followed_place, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding?.data = places[position]
        if (places[position].dominantGroup != null)
            holder.binding?.shadowCircle?.borderColor =
                Color.parseColor(places[position].dominantGroup?.strokeColorHex)
        if (places[position].imageName.isEmpty()) {
            holder.binding?.placeImage?.visibility = View.VISIBLE
            holder.binding?.placeImage?.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    ViewUtils.getPlaceIcon(places[position].type)!!
                )
            )
        } else {
            holder.binding?.placeImage?.visibility = View.GONE
            val ref =
                Firebase.storage.getReference(Constants.PLACE_PICTURES + Utils.getImageUrl200(places[position].imageName))
            GlideApp.with(context).load(ref).into(holder.binding?.shadowCircle!!)
        }
        holder.binding?.item?.setOnClickListener {
            val intent = Intent(context, PlaceActivity::class.java)
            intent.putExtra("data", places[position])
            context.startActivity(intent)
        }
        holder.binding?.unfollow?.setOnClickListener {
            interaction.unfollowPlace(position)
        }
        holder.binding?.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return places.size
    }

    interface FollowedPlaceInteraction {
        fun unfollowPlace(position: Int)
    }
}