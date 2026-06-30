package com.orbis.orbis.ui.authModule.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.helpers.PlaceIcon
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.place.PolygonPlaceDetails
import com.orbis.orbis.models.place.toPlaceDetails
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.placesModule.views.PlaceActivity
import com.orbis.orbis.utils.Utils

class PlacesPagerAdapter(private val context: Context, val places: ArrayList<PolygonPlaceDetails>) :
    RecyclerView.Adapter<PlacesPagerAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val placeNameView: TextView = view.findViewById(R.id.place_name_tv)
        val ratingView: TextView = view.findViewById(R.id.rating_tv)
        val ratingCountView: TextView = view.findViewById(R.id.rating_counts_tv)
        val descriptionView: TextView = view.findViewById(R.id.description_tv)

        val placeImageView: ImageView = view.findViewById(R.id.place_iv)
        val placeIconCircle: LinearLayout = view.findViewById(R.id.place_circle)
        val imagesContainer: ConstraintLayout = view.findViewById(R.id.place_images_container)
        val placeIconImage: ImageView = view.findViewById(R.id.place_icon)
        val placeImageLoader: LottieAnimationView = view.findViewById(R.id.loader)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.map_place_page, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val place = places[position]

        setPlaceImage(holder, place)
        setPlaceDescriptionVisibility(holder, place)

        holder.placeNameView.text = place.name
        holder.ratingView.text = place.averageRate.toString()
        holder.ratingCountView.text =
            if (place.countRates > 1000) "(${place.countRates / 1000.0} mil)" else "(${place.countRates})"
        holder.descriptionView.text = place.description

        holder.itemView.setOnClickListener {
            val watchingLocation = Location("OrbisWatchingLocation")
            watchingLocation.longitude = place.coordinates!!.longitude
            watchingLocation.latitude = place.coordinates.latitude
            Constants.location = watchingLocation
            val intent = Intent(context, PlaceActivity::class.java)
            intent.putExtra("data", place.toPlaceDetails())
            context.startActivity(intent)
        }
        holder.itemView.tag = "currentView"
    }

    override fun getItemCount(): Int = places.size
    fun setIconImage(holder: ViewHolder, place: PolygonPlaceDetails){
        val imageView = holder.placeIconImage
        imageView.setColorFilter(Color.WHITE)
        imageView.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                PlaceIcon.getIconByType(place.type)
            )
        )
    }

    private fun setPlaceDescriptionVisibility(holder: ViewHolder, place: PolygonPlaceDetails) {
        if (place.description.isEmpty())
            holder.descriptionView.visibility = View.GONE
        else
            holder.descriptionView.visibility = View.VISIBLE
    }

    fun setPlaceImage(holder: ViewHolder, place: PolygonPlaceDetails){
        val imageName = place.imageName
        if (imageName.isEmpty()){
            holder.imagesContainer.visibility = View.GONE
            return
        }
        holder.placeImageLoader.visibility = View.VISIBLE //start laoding animation
        val imageView = holder.placeImageView
        //setIconImage(holder, place)
        holder.imagesContainer.visibility = View.VISIBLE

        val storage =
            Firebase.storage.getReference(
                Constants.PLACE_PICTURES + Utils.getImageUrl200(
                    imageName
                )
            )
        /*storage.getBytes(10 * 1024 * 1024).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            imageView.setImageBitmap(bitmap)
            holder.placeImageLoader.visibility = View.GONE
        }.addOnFailureListener {
            holder.imagesContainer.visibility = View.GONE
        }*/
        GlideApp.with(context)
            .load(storage)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.itemView.post {
                        holder.imagesContainer.visibility = View.GONE
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.itemView.post {
                        imageView.setImageDrawable(resource)
                        holder.placeImageLoader.visibility = View.GONE
                    }
                    return true
                }
            }).submit()
    }

}