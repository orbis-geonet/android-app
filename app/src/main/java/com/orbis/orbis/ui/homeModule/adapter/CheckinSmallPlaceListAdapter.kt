package com.orbis.orbis.ui.homeModule.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.databinding.EachCheckinPlaceItemBinding
import com.orbis.orbis.databinding.EachCheckinPlaceItemSmallBinding
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.utils.ViewUtils


class CheckinSmallPlaceListAdapter(
    private val interaction: PlaceCardInteraction,
    private val context: Context,
    private val places: ArrayList<PlaceDetails>
) :
    RecyclerView.Adapter<CheckinSmallPlaceListAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.each_checkin_place_item_small, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding?.data = places[position]
        holder.binding?.cardId?.setOnClickListener {
            interaction.onItemClicked(position)
        }

        if (places[position].dominantGroup == null || places[position].dominantGroup?.imageName.isNullOrEmpty()) {
            holder.binding?.noGroupImage?.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    ViewUtils.getPlaceIcon(places[position].type)!!
                )
            )
            holder.binding?.shadowCircle4?.visibility = View.INVISIBLE
            holder.binding?.noGroupImage?.visibility = View.VISIBLE
            if (places[position].dominantGroup == null) {
                holder.binding?.noGroupCircle?.visibility = View.INVISIBLE
            } else {
                holder.binding?.noGroupCircle?.visibility = View.VISIBLE
            }
        } else {
            holder.binding?.shadowCircle4?.visibility = View.VISIBLE
            holder.binding?.noGroupCircle?.visibility = View.GONE
            holder.binding?.noGroupImage?.visibility = View.GONE
            ViewUtils.loadGroupPhoto(
                context,
                holder.binding?.shadowCircle4!! as ImageView,
                places[position].dominantGroup?.imageName
            )
        }
        holder.binding?.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return places.size
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachCheckinPlaceItemSmallBinding? = DataBindingUtil.bind(itemView)
    }

    interface PlaceCardInteraction {
        fun onItemClicked(position: Int)
    }
}
