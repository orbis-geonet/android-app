package com.orbis.orbis.ui.placesModule.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.PagerAdapter
import com.orbis.orbis.R
import com.orbis.orbis.ui.groupsModule.viewModel.CheckIn
import java.util.*


internal class ImageViewPagerAdapter(context: Context, check_in_list: ArrayList<CheckIn>?) :
    PagerAdapter() {
    // Context object
    var context: Context

    // Array of images
    var check_in_list: ArrayList<CheckIn>? = null

    // Layout Inflater
    var mLayoutInflater: LayoutInflater
    override fun getCount(): Int {
        // return the number of images
        return check_in_list?.size!!
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as ConstraintLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        // inflating the item.xml
        val itemView: View = mLayoutInflater.inflate(R.layout.image_item, container, false)

        // referencing the image view from the item.xml file
        val photo_iv: ImageView = itemView.findViewById(R.id.photo_iv) as ImageView

        // setting the image in the imageView
//        imageView.setImageResource(images[position])

        // Adding the View
        Objects.requireNonNull(container).addView(itemView)
        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as ConstraintLayout)
    }

    // Viewpager Constructor
    init {
        this.context = context
        this.check_in_list = check_in_list
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }
}