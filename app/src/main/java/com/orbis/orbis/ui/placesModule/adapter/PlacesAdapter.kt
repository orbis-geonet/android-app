package com.orbis.orbis.ui.placesModule.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.orbis.orbis.R


class PlacesAdapter(applicationContext: Context, places: IntArray) :
    BaseAdapter() {
    var context: Context
    var places: IntArray
    override fun getCount(): Int {
        return places.size
    }

    override fun getItem(position: Int): Any? {
        return places.get(position)
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var convertView = convertView
        if (convertView == null) {
            convertView =
                LayoutInflater.from(context).inflate(R.layout.each_place_item, parent, false)
        }
        val imageView: ImageView
        imageView = convertView!!.findViewById<View>(R.id.place_iv) as ImageView
        imageView.setImageResource(places.get(position))
        return convertView
    }


    init {
        context = applicationContext
        this.places = places
    }
}