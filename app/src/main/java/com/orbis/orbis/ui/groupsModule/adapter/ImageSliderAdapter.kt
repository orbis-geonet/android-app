package com.orbis.orbis.ui.groupsModule.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.models.Constants
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.utils.Utils
import com.stfalcon.imageviewer.StfalconImageViewer
import java.util.logging.Handler
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ImageSliderAdapter(
    private val context: Context,
    private val images: ArrayList<String>,
    private val location: String = Constants.POST_PHOTO_STORAGE
) : PagerAdapter() {
    val storages: HashMap<Int, StorageReference> = HashMap()

    override fun getCount(): Int {
        // return the number of images
        return images.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as ConstraintLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        // inflating the item.xml
        val itemView: View =
            LayoutInflater.from(context).inflate(R.layout.item_slider_image, container, false)
        val imageLayout = itemView.findViewById(R.id.imageLayout) as ImageView
        val counter = itemView.findViewById(R.id.counter) as TextView
        val placeholder = (position + 1).toString() + "/" + images.size
        counter.text = placeholder
        if (images.size > 1) {
            counter.visibility = View.VISIBLE
        } else {
            counter.visibility = View.GONE
        }
        val storage = Firebase.storage.getReference(location + Utils.getImageUrl680(images[position]))
        Log.d("checkingGlideLoad", "loadFor " + position)
        loadImage(storage, imageLayout)
        storages.put(position, storage)
        imageLayout.setOnClickListener {
            val imageRefs: ArrayList<StorageReference> = ArrayList()
            for (stor in storages) {
                imageRefs.add(stor.value)
            }
            val viewer = StfalconImageViewer.Builder(context, imageRefs) { imageView, s ->
                GlideApp.with(context).load(s).into(imageView)
            }.withStartPosition(position).allowZooming(true).allowSwipeToDismiss(true)
            viewer.show()
        }
        // Adding the View
        container.addView(itemView)
        return itemView
    }

    private fun loadImage(storage: StorageReference, imageLayout: ImageView) {

        GlideApp.with(context)
            .load(storage)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .skipMemoryCache(false)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    android.os.Handler(Looper.getMainLooper()).postDelayed({
                        loadImage(storage, imageLayout)
                    }, 1000)

                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    imageLayout.setImageDrawable(resource)
                    return true
                }

            }).into(imageLayout)
    }


    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as ConstraintLayout)
    }


}