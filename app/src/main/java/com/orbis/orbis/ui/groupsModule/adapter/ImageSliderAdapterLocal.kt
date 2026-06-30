package com.orbis.orbis.ui.groupsModule.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.models.Constants
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.commentModule.views.CommentDialogFragment
import com.orbis.orbis.utils.Utils
import com.squareup.picasso.Picasso
import com.stfalcon.imageviewer.StfalconImageViewer
import java.io.File
import java.util.*


class ImageSliderAdapterLocal(
    private val context: Context,
    private val images: ArrayList<String>,
) : PagerAdapter() {


    override fun getCount(): Int {
        // return the number of images
        return images.size
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
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
        GlideApp.with(context)
            .load(File(images[position]))
            .into(imageLayout)

        imageLayout.setOnClickListener {
            StfalconImageViewer.Builder(context, images) { imageView, imagePath ->
                Glide.with(context)
                    .load(File(imagePath))
                    .into(imageView)
            }
                .withStartPosition(position)
                .allowZooming(true)
                .allowSwipeToDismiss(true)
                .show()
        }
        // Adding the View
        container.addView(itemView)
        return itemView
    }


    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as ConstraintLayout)
    }


}