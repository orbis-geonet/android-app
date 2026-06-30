package com.orbis.orbis.helpers

import android.graphics.Color
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.databinding.BindingAdapter
import de.hdodenhof.circleimageview.CircleImageView

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("app:imageBackground")
    public fun setImageBackground(imageView: ImageView, color: String) {
        imageView.setBackgroundColor(Color.parseColor(color))
    }


    @JvmStatic
    @BindingAdapter("app:civ_border_color")
    public fun setBorderColor(imageView: CircleImageView, color: String?) {
        if (color != null) {
            try
            {
                imageView.borderColor = Color.parseColor(color)
            } catch (e: IllegalArgumentException) { }
        }
    }
    @JvmStatic
    @BindingAdapter("app:cardBackgroundColor")
    public fun setCardBackground(cardView: CardView, color: String?) {
        if (color != null) {
            cardView.setCardBackgroundColor(Color.parseColor(color))
        } else {
            cardView.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
        }
    }
}