package com.orbis.orbis.extensions

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.MimeTypeMap
import androidx.constraintlayout.widget.Group
import com.google.android.gms.common.util.CollectionUtils
import com.orbis.orbis.models.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

fun View.show(show: Boolean): View {
    if (show) {
        this.show()
    } else {
        this.hide()
    }
    return this
}

fun View.inVisible(inVisible: Boolean): View {
    if (inVisible) {
        this.inVisible()
    } else {
        this.show()
    }
    return this
}

fun View.show(): View {
    this.visibility = View.VISIBLE
    if (this is Group) {
        this.requestLayout()
    }
    return this
}

fun View.hide(): View {
    this.visibility = View.GONE
    if (this is Group) {
        this.requestLayout()
    }
    return this
}

fun View.inVisible(): View {
    this.visibility = View.INVISIBLE
    if (this is Group) {
        this.requestLayout()
    }
    return this
}

fun Int.toDp() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Int.toPx() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Uri.realUri(): Uri
{
    return if (this.scheme == null)
    {
        Uri.fromFile(this.path?.let { File(it) })
    } else
    {
        this
    }
}


fun Uri.getMimeType(context: Context): String
{
    val extension = if (this.scheme == ContentResolver.SCHEME_CONTENT)
    {
        //If scheme is a content
        val mime = MimeTypeMap.getSingleton()
        mime.getExtensionFromMimeType(context.contentResolver.getType(this))
    }
    else
    {
        //If scheme is a File
        //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
        MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(this.path?.let { File(it) }).toString())
    }
    return extension ?: ""
}

var Int.compactFormat : String
    get()
    {
        if (this < 1000) return "" + this
        val exp = (ln(this.toDouble()) / ln(1000.0)).toInt()
        return String.format("%.1f %c", this / 1000.0.pow(exp.toDouble()), "kMGTPE"[exp - 1])
    }
set(value) {}

var Float.roundOffDecimal: Float
    get()
    {
        val df = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
        df.roundingMode = RoundingMode.CEILING
        return df.format(this).toFloat()
    }
    set(value) {}

var Float.roundOffDecimalString: String
    get()
    {
        val decimalFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
        return decimalFormat.format(this)
    }
    set(value) {}