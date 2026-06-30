package com.orbis.orbis.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.*
import android.graphics.drawable.shapes.RoundRectShape
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.type.DateTime
import com.orbis.orbis.R
import com.orbis.orbis.models.Constants
import com.orbis.orbis.ui.app.GlideApp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*



class ViewUtils {

    companion object {
        fun Context.setAppLocale(language: String): Context {
            val locale = Locale(language)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            return createConfigurationContext(config)
        }

        fun loadGroupPhoto(context: Context, imageView: ImageView, imageName: String? = null) {
            if (imageName.isNullOrEmpty()) {
                GlideApp.with(context)
                    .load(R.drawable.ic_user)
                    .placeholder(R.drawable.ic_user)
                    .into(imageView)
            } else {
                val storage =
                    Firebase.storage.getReference(
                        Constants.GROUP_PHOTO_STORAGE + Utils.getImageUrl200(
                            imageName
                        )
                    )
                GlideApp.with(context)
                    .load(storage)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_user)
                    .into(imageView)
            }

        }

        fun loadUserProfilePic(
            context: Context,
            imageView: ImageView,
            imageName: String?,
            providerImage: String?
        ) {
            if (!imageName.isNullOrEmpty()) {
                val storage =
                    Firebase.storage.getReference(
                        Constants.PROFILE_PICTURES + Utils.getImageUrl200(
                            imageName
                        )
                    )
                GlideApp.with(context).load(storage).placeholder(R.drawable.ic_user).into(imageView)
            } else if (!providerImage.isNullOrEmpty()) {
                val uri = Uri.parse(providerImage)
                var load = ""
                if (providerImage.contains("fbsbx")!!) {
                    val asid = uri.getQueryParameter("asid")
                    load =
                        "https://graph.facebook.com/" + asid + "/picture?width=600&height=600"
                } else {
                    load = providerImage
                }
                GlideApp.with(context).load(load).placeholder(R.drawable.ic_user).into(imageView)
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_user))
            }
        }

        val TWO_SPACES = " "
        fun checkSeeMore(textView: TextView, text: String, maxLines: Int) {
            textView.text = text
            textView.post {
                // Past the maximum number of lines we want to display.
                if (textView.lineCount > maxLines) {
                    val lastCharShown = textView.layout.getLineVisibleEnd(maxLines - 1)
                    textView.maxLines = maxLines
                    val moreString: String = "view more"
                    val suffix: String = TWO_SPACES.toString() + moreString

                    // 3 is a "magic number" but it's just basically the length of the ellipsis we're going to insert
                    val actionDisplayText: String =
                        text.substring(0, lastCharShown - suffix.length - 3)
                            .toString() + "..." + suffix
                    val truncatedSpannableString = SpannableString(actionDisplayText)
                    val startIndex = actionDisplayText.indexOf(moreString)
                    truncatedSpannableString.setSpan(
                        ForegroundColorSpan(Color.parseColor("#00b9ff")),
                        startIndex,
                        startIndex + moreString.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    textView.text = truncatedSpannableString
                }
            }
        }
        fun getPlaceIcon(type: String): Int? {
            Log.d("getPlaceIcon", type)
            when (type) {
                "LOCATION" -> {
                    return R.drawable.ic_group_1
                }
                "BUILDING" -> {
                    return R.drawable.ic_group_2
                }
                "BAR" -> {
                    return R.drawable.ic_group_3
                }
                "HOUSE_2" -> {
                    return R.drawable.ic_group_4
                }
                "CASTLE" -> {
                    return R.drawable.ic_group_5
                }
                "SPORTS_CENTER" -> {
                    return R.drawable.ic_group_6
                }
                "FAST_FOOD" -> {
                    return R.drawable.ic_group_7
                }
                "SHOPPING" -> {
                    return R.drawable.ic_group_8
                }
                "RESTAURANT" -> {
                    return R.drawable.ic_group_9
                }
                "MUSIC" -> {
                    return R.drawable.ic_group_10
                }
                "BEACH" -> {
                    return R.drawable.ic_group_11
                }
                "SCHOOL" -> {
                    return R.drawable.ic_group_12
                }
                "TWO_BUILDINGS" -> {
                    return R.drawable.ic_group_13
                }
                "PARK" -> {
                    return R.drawable.ic_group_14
                }
                "HOUSE" -> {
                    return R.drawable.ic_group_15
                }
            }
            return R.drawable.ic_group_1
        }

        /**
         * This method converts dp unit to equivalent device specific value in pixels.
         *
         * @param dp      A value in dp(Device independent pixels) unit. Which we need to convert into pixels
         * @param context Context to get resources and device specific display metrics
         * @return An integer value to represent Pixels equivalent to dp according to device
         */
        @JvmStatic
        fun convertDpToPixel(dp: Float, context: Context): Int {
            val resources: Resources = context.resources
            val metrics: DisplayMetrics = resources.displayMetrics
            return (dp * (metrics.densityDpi / 160f)).toInt()
        }

        @SuppressLint("SimpleDateFormat")
        fun convertTimeStampToFormatted(timestamp: String): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                val inst: Instant = Instant.parse(timestamp)
                val time: LocalDateTime = inst.atZone(ZoneId.of(TimeZone.getDefault().id))
                    .toLocalDateTime()

                Log.d("timeZoneCheck", TimeZone.getDefault().toString())

                return if (time.minute.toString().length == 1) {
                    time.dayOfMonth
                        .toString() + "/" + time.monthValue + "/" + time.year
                        .toString()
                        .substring(2) + " at " + time.hour + ":" + time.minute + "0"
                } else {
                    time.dayOfMonth
                        .toString() + "/" + time.monthValue + "/" + time.year
                        .toString()
                        .substring(2) + " at " + time.hour + ":" + time.minute

                }
            }
            else
            {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date: Date? = inputFormat.parse(timestamp)
                if(date != null)
                {
                    val outputFormat = SimpleDateFormat("dd/MM/yy 'at' HH:mm")
                    return outputFormat.format(date)
                }
                else
                {
                    return timestamp
                }
            }
        }

        /***
         * Applies the shadow effect
         */
        @SuppressLint("SimpleDateFormat")
        fun convertTimeStampToDate(dateStr: String?): Date? {
            try {
                val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                val date: Date = dateFormat.parse(dateStr)
                return date
            } catch (e: Exception) {
                try {
                    val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
                    val date: Date = dateFormat.parse(dateStr)
                    return date
                } catch (e: Exception) {
                    try {
                        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm.SS'Z'")
                        val date: Date = dateFormat.parse(dateStr)
                        return date
                    } catch (e: Exception) {
                        try {
                            val dateFormat: DateFormat =
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                            val date: Date = dateFormat.parse(dateStr)
                            return date
                        } catch (e: Exception) {
                            Log.d("SimpleDateFormat4", e.stackTraceToString())
                            return null
                        }
                        Log.d("SimpleDateFormat4", e.stackTraceToString())
                        return null
                    }
                    Log.d("SimpleDateFormat3", e.stackTraceToString())
                    return null
                }
                Log.d("SimpleDateFormat", e.stackTraceToString())
                return null
            }

        }

        fun View.applyShadow(
            backgroundColorValue: Int,
            cornerRadiusValue: Float,
            shadowColorValue: Int,
            elevationValue: Int,
            shadowGravity: Int,
            insetLeft: Int = elevationValue,
            insetTop: Int = elevationValue,
            insetRight: Int = elevationValue,
            insetBottom: Int = elevationValue,
            dx: Float = 0F,
            dy: Float = 0F
        ) {
            background = generateBackgroundWithShadow(
                this, backgroundColorValue, cornerRadiusValue, shadowColorValue, elevationValue,
                shadowGravity, insetLeft, insetTop, insetRight, insetBottom, dx, dy
            )
        }

        /***
         * Generates a shadow drawable
         */
        private fun generateBackgroundWithShadow(
            view: View,
            backgroundColorValue: Int,
            cornerRadiusValue: Float,
            shadowColorValue: Int,
            elevationValue: Int,
            shadowGravity: Int,
            insetLeft: Int = elevationValue,
            insetTop: Int = elevationValue,
            insetRight: Int = elevationValue,
            insetBottom: Int = elevationValue,
            dx: Float = 0F,
            dy: Float = 0F

        ): Drawable {
            val outerRadius = FloatArray(8) { cornerRadiusValue }

            val backgroundPaint = Paint()
            backgroundPaint.style = Paint.Style.FILL
            backgroundPaint.setShadowLayer(cornerRadiusValue, 0f, 0f, 0)

            val shapeDrawablePadding = Rect()
            shapeDrawablePadding.left = elevationValue
            shapeDrawablePadding.right = elevationValue
            val DY: Int
            when (shadowGravity) {
                Gravity.CENTER -> {
                    shapeDrawablePadding.top = elevationValue
                    shapeDrawablePadding.bottom = elevationValue
                    DY = 0
                }
                Gravity.TOP -> {
                    shapeDrawablePadding.top = elevationValue * 2
                    shapeDrawablePadding.bottom = elevationValue
                    DY = -1 * elevationValue / 7
                }
                Gravity.BOTTOM -> {
                    shapeDrawablePadding.top = elevationValue
                    shapeDrawablePadding.bottom = elevationValue * 2
                    DY = elevationValue / 7
                }
                else -> {
                    shapeDrawablePadding.top = elevationValue
                    shapeDrawablePadding.bottom = elevationValue * 2
                    DY = elevationValue / 7
                }
            }
            val shapeDrawable = ShapeDrawable()
            shapeDrawable.setPadding(shapeDrawablePadding)

            shapeDrawable.paint.color = backgroundColorValue
            shapeDrawable.paint.setShadowLayer(
                cornerRadiusValue / 7,
                dx,
                DY.toFloat(),
                shadowColorValue
            )

            view.setLayerType(View.LAYER_TYPE_SOFTWARE, shapeDrawable.paint)

            shapeDrawable.shape = RoundRectShape(outerRadius, null, null)

            val drawable = LayerDrawable(arrayOf<Drawable>(shapeDrawable))
            drawable.setLayerInset(0, insetLeft, insetTop, insetRight, insetBottom)

            return drawable
        }

        fun changeViewColor(v: View, color: Int) {
            val drawable = v.background as ColorDrawable
            drawable.color = color
        }

        fun changeStrokeColor(v: View, color: Int) {
            val drawable = v.background as GradientDrawable
            drawable.setStroke(1,color)
        }

        fun colorDrawable(view: View, color: Int) {
            val wrappedDrawable = DrawableCompat.wrap(view.background)
            if (wrappedDrawable != null) {
                DrawableCompat.setTint(wrappedDrawable.mutate(), color)
                setBackgroundDrawable(view, wrappedDrawable)
            }
        }
        fun colorTintImageView(view: ImageView, color: Int) {
            view.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        }

        fun setBackgroundDrawable(view: View, drawable: Drawable?) {
            view.background = drawable
        }

        fun togglePasswordEye(editText: TextInputEditText, imageView: ImageView, context: Context) {
            val method = editText.transformationMethod
            if (method is PasswordTransformationMethod) {
                imageView.setColorFilter(
                    ContextCompat.getColor(context, R.color.icons_grey_color),
                    android.graphics.PorterDuff.Mode.SRC_ATOP
                )
                editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else if (method is HideReturnsTransformationMethod) {
                imageView.setColorFilter(
                    ContextCompat.getColor(context, R.color.black),
                    android.graphics.PorterDuff.Mode.SRC_ATOP
                )
                editText.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            editText.setSelection(editText.text?.length!!)
        }

        fun showDialogue(context: Context, title: String, message: String) {
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok") { it, i ->
                    it.dismiss()
                }
                .show()
        }

    }




}