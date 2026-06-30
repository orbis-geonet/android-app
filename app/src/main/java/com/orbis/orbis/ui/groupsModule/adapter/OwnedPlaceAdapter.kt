package com.orbis.orbis.ui.groupsModule.adapter

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
import com.orbis.orbis.databinding.ItemPlaceBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.placesModule.views.PlaceActivity
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.squareup.picasso.Picasso


class OwnedPlaceAdapter(private val context: Context, private val places: ArrayList<PlaceDetails>) :
    RecyclerView.Adapter<OwnedPlaceAdapter.ViewHolder>() {


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ItemPlaceBinding? = DataBindingUtil.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_place, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding?.data = places[position]
        if (places[position].dominantGroup != null)
            holder.binding?.shadowCircle?.borderColor =
                Color.parseColor(places[position].dominantGroup?.strokeColorHex)
            holder.binding?.placeImage?.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    ViewUtils.getPlaceIcon(places[position].type)!!
                )
            )

        holder.binding?.item?.setOnClickListener {
            val intent = Intent(context, PlaceActivity::class.java)
            intent.putExtra("data", places[position])
            context.startActivity(intent)
        }
        holder.binding?.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return places.size
    }
}