package com.lassi.common.utils

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

object DrawableUtils {
    fun changeIconColor(context: Context, @DrawableRes drawableRes: Int, color: Int): Drawable? {
        val iconDrawable = ContextCompat.getDrawable(context, drawableRes)
        iconDrawable?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        return iconDrawable
    }
}