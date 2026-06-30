package com.orbis.orbis.helpers

import android.animation.Animator
import android.animation.FloatEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.orbis.orbis.models.place.PolygonPlaceDetails
import com.orbis.orbis.models.place.isCircle
import com.orbis.orbis.utils.ViewUtils
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.abs
import androidx.core.graphics.scale

object AnimationsUtil {
    private val MILLIS_IN_SECOND = 1000L
    private val SECONDS_IN_MINUTE = 60
    private val MINUTES_IN_HOUR = 60
    private val HOURS_IN_DAY = 24
    private val DAYS_IN_YEAR = 365 //I know this value is more like 365.24...

    val MILLISECONDS_IN_DAY =
        MILLIS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR * HOURS_IN_DAY
    val MILLISECONDS_IN_HOUR = 3600000F
    fun getDataDiffFromNow(lastCheckInTimestamp: Long): Long {
        val nowDate = Calendar.getInstance().time
        val elapsedTime = abs(lastCheckInTimestamp - nowDate.time)
        return elapsedTime
    }

    fun createPulseAnimationSlow(
        groundOverlay: GroundOverlay,
        placeDetails: PolygonPlaceDetails, isOpen: Boolean = true,
        onAnimationRepeat: () -> Unit
    ): ValueAnimator {
        // Prep the animator
        val radiusHolder = PropertyValuesHolder.ofFloat(
            "radius",
            placeDetails.size.toFloat() * 0.5F,
            placeDetails.size.toFloat() * 2.2F,
            placeDetails.size.toFloat() * 2
        )
        var isOpenLocal = isOpen
        val transparencyHolder = PropertyValuesHolder.ofFloat("transparency", -1f, 1f)
        val valueAnimator = ValueAnimator()
        valueAnimator.repeatCount = 0
        valueAnimator.repeatMode = ValueAnimator.RESTART
        valueAnimator.setValues(radiusHolder, transparencyHolder)
        valueAnimator.duration = 3000
        valueAnimator.setEvaluator(FloatEvaluator())
        valueAnimator.interpolator = AccelerateDecelerateInterpolator()
        valueAnimator.addUpdateListener { valueAnimator ->
            val animatedRadius = valueAnimator.getAnimatedValue("radius") as Float
            val animatedAlpha = valueAnimator.getAnimatedValue("transparency") as Float
            groundOverlay.setDimensions(animatedRadius)
            // placeMarker.groundOverlay.setTransparency(animatedAlpha)
        }
        valueAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {

            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {
                placeDetails.animator?.cancel()
                placeDetails.animator = createPulseAnimation(groundOverlay, placeDetails)
                if (placeDetails.animator != null)
                    startPulseAnimation(placeDetails, placeDetails.animator!!)
                onAnimationRepeat()
            }

        })
        return valueAnimator
    }

    fun createPulseAnimationSlowDiminish(
        groundOverlay: GroundOverlay,
        placeDetails: PolygonPlaceDetails
    ): ValueAnimator {
        // Prep the animator
        var radiusX2 = placeDetails.size.times(2.0F).toFloat()
        val radiusHolder = PropertyValuesHolder.ofFloat(
            "radius",
            placeDetails.previousSize.toFloat() * 2.0F,
            placeDetails.size.toFloat() * 2.0F,
            radiusX2
        )
        val transparencyHolder = PropertyValuesHolder.ofFloat("transparency", -1f, 1f)
        val valueAnimator = ValueAnimator()
        valueAnimator.repeatCount = 0
        valueAnimator.repeatMode = ValueAnimator.RESTART
        valueAnimator.setValues(radiusHolder, transparencyHolder)
        valueAnimator.duration = 2000
        valueAnimator.setEvaluator(FloatEvaluator())
        valueAnimator.interpolator = AccelerateDecelerateInterpolator()
        valueAnimator.addUpdateListener { valueAnimator ->

            val animatedRadius = valueAnimator.getAnimatedValue("radius") as Float
            val animatedAlpha = valueAnimator.getAnimatedValue("transparency") as Float
            // Log.d("animatedRadius", placeDetails.placeKey + " " + animatedRadius.toString())
            radiusX2 = animatedRadius
            groundOverlay.setDimensions(animatedRadius)
            // placeMarker.groundOverlay.setTransparency(animatedAlpha)
        }
        valueAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                if (placeDetails.previousSize > 400F) {
                    groundOverlay.zIndex = 600F
                }

            }

            override fun onAnimationEnd(animation: Animator) {
                Log.d("animationEnded", "ended")
                groundOverlay.setDimensions(radiusX2)
                placeDetails.animator = createPulseAnimation(groundOverlay, placeDetails)
                Handler(Looper.getMainLooper()).postDelayed({
                    if (placeDetails.animator != null) {
                        startPulseAnimation(placeDetails, placeDetails.animator!!)
                    }
                }, 1000)
            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {
                placeDetails.animator?.cancel()
                Log.d("animatorFinal", radiusX2.toString())
            }

        })
        return valueAnimator
    }
    fun createPulseAnimationCeaseExist(
        groundOverlay: GroundOverlay
    ): ValueAnimator {
        var radiusX2 = groundOverlay.width
        val radiusHolder = PropertyValuesHolder.ofFloat(
            "radius",
            radiusX2,
            0f
        )
        val transparencyHolder = PropertyValuesHolder.ofFloat("transparency", -1f, 1f)
        val valueAnimator = ValueAnimator()
        valueAnimator.repeatCount = 0
        valueAnimator.repeatMode = ValueAnimator.RESTART
        valueAnimator.setValues(radiusHolder, transparencyHolder)
        valueAnimator.duration = 2000
        valueAnimator.setEvaluator(FloatEvaluator())
        valueAnimator.interpolator = AccelerateDecelerateInterpolator()
        valueAnimator.addUpdateListener { valueAnimator ->
            val animatedRadius = valueAnimator.getAnimatedValue("radius") as Float
            radiusX2 = animatedRadius
            groundOverlay.setDimensions(animatedRadius)
        }
        return valueAnimator
    }
    fun createPolygonAppearingAnimation(polygon: Polygon, centroid: LatLng, polygonPoints: List<LatLng>) : ValueAnimator{
        val animator = ValueAnimator.ofFloat(0.0f, 1f)
        animator.duration = 1000
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val interpolatedPoints = polygonPoints.map { point ->
                LatLng(
                    centroid.latitude + (point.latitude - centroid.latitude) * fraction,
                    centroid.longitude + (point.longitude - centroid.longitude) * fraction
                )
            }
            polygon!!.points = interpolatedPoints
        }
        return animator
    }
    fun createPopupMarkerAppearingAnimation(
        marker: Marker,
        originalBitmap: Bitmap,
        isInFocusMode: () -> Boolean
    ): ValueAnimator {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 100
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val targetWidth = (originalBitmap.width * fraction).toInt().coerceAtLeast(1)
            val targetHeight = (originalBitmap.height * fraction).toInt().coerceAtLeast(1)
            val scaled = originalBitmap.scale(targetWidth, targetHeight)
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(scaled))
            if (!marker.isVisible) marker.isVisible = true
            if (isInFocusMode()) marker.isVisible = false
        }
        return animator
    }
    fun createMovingCenterAnimation(
        groundOverlay: GroundOverlay,
        toPlace: PolygonPlaceDetails
    ): ValueAnimator {
        val startingPosition = groundOverlay.position
        val endingPosition = toPlace.polygonCenter
        val initialRadius = groundOverlay.width / 2
        val latHolder = PropertyValuesHolder.ofFloat("lat", startingPosition.latitude.toFloat(), endingPosition!!.latitude.toFloat())
        val lngHolder = PropertyValuesHolder.ofFloat("lng", startingPosition.longitude.toFloat(), endingPosition.longitude.toFloat())
        val radiusHolder = PropertyValuesHolder.ofFloat("radius", initialRadius, toPlace.size.toFloat() * 2)

        val valueAnimator = ValueAnimator()
        valueAnimator.repeatMode = ValueAnimator.RESTART
        valueAnimator.setValues(latHolder, lngHolder, radiusHolder)
        valueAnimator.duration = 2000
        valueAnimator.setEvaluator(FloatEvaluator())
        valueAnimator.interpolator = AccelerateDecelerateInterpolator()
        valueAnimator.addUpdateListener { valueAnimator ->
            val animatedLat = valueAnimator.getAnimatedValue("lat") as Float
            val animatedLng = valueAnimator.getAnimatedValue("lng") as Float
            val animatedLatLng = LatLng(animatedLat.toDouble(), animatedLng.toDouble())
            val animatedRadius = valueAnimator.getAnimatedValue("radius") as Float
            groundOverlay.setDimensions(animatedRadius)
            groundOverlay.position = animatedLatLng
        }
        return valueAnimator
    }
    fun createPopupAnimation(
        groundOverlay: GroundOverlay,
        placeDetails: PolygonPlaceDetails
    ): ValueAnimator {
        val radiusHolder =
            PropertyValuesHolder.ofFloat("radius", 0F, placeDetails.size.toFloat() * 2)
//        val transparencyHolder = PropertyValuesHolder.ofFloat("transparency", 0f, if(isGroundOverlayClicked2) 0.9f else 0.0f)

        val valueAnimator = ValueAnimator()
        valueAnimator.repeatMode = ValueAnimator.RESTART
        valueAnimator.setValues(radiusHolder)
        valueAnimator.duration = 2000
        valueAnimator.setEvaluator(FloatEvaluator())
        valueAnimator.interpolator = AccelerateDecelerateInterpolator()
        valueAnimator.addUpdateListener { valueAnimator ->
            val animatedRadius = valueAnimator.getAnimatedValue("radius") as Float
            groundOverlay.setDimensions(animatedRadius)
        }
        return valueAnimator
    }
    fun createPulseAnimation(
        groundOverlay: GroundOverlay,
        placeDetails: PolygonPlaceDetails
    ): ValueAnimator? {
        var lastCheckinDate = ViewUtils.convertTimeStampToDate(placeDetails.lastCheckInTimestamp)
        if (!placeDetails.isCircle()) {
            val checkins = placeDetails.places.mapNotNull { ViewUtils.convertTimeStampToDate(it.lastCheckInTimestamp) }
            lastCheckinDate = checkins.maxByOrNull { it }
        }

        val checkinTime = lastCheckinDate?.time ?: return null

        val tz = TimeZone.getDefault()
        val cal = GregorianCalendar.getInstance(tz)
        val offsetInMillis = tz.getOffset(cal.timeInMillis)

        val diff = getDataDiffFromNow(checkinTime) - offsetInMillis

        if (diff < MILLISECONDS_IN_DAY) {
            val radiusHolder = PropertyValuesHolder.ofFloat(
                "radius",
                placeDetails.size.toFloat() * 2,
                placeDetails.size.toFloat() * 1.8F,
                placeDetails.size.toFloat() * 2
            )

            val diffHours = diff / MILLISECONDS_IN_HOUR
            val pulseRate = (500F + (diffHours * 20.83)).toLong()
            Log.d("differenceOfTime", placeDetails.placeKey + " " + diffHours + " " + pulseRate)

            val transparencyHolder = PropertyValuesHolder.ofFloat("transparency", 0f, 1f)
            val valueAnimator = ValueAnimator()
            valueAnimator.repeatMode = ValueAnimator.RESTART
            valueAnimator.setValues(radiusHolder, transparencyHolder)
            valueAnimator.duration = pulseRate
            valueAnimator.setEvaluator(FloatEvaluator())
            valueAnimator.interpolator = AccelerateDecelerateInterpolator()
            valueAnimator.addUpdateListener { valueAnimator ->
                val animatedRadius = valueAnimator.getAnimatedValue("radius") as Float
                val animatedAlpha = valueAnimator.getAnimatedValue("transparency") as Float
                groundOverlay.setDimensions(animatedRadius)
            }

            return valueAnimator
        } else {
            return null
        }
    }

    fun startPulseAnimation(
        placeDetails: PolygonPlaceDetails,
        valueAnimator: ValueAnimator,
        delay: Long = 0L
    ) {
        var lastCheckinDate = ViewUtils.convertTimeStampToDate(placeDetails.lastCheckInTimestamp)
        if (!placeDetails.isCircle()){
            val checkins = placeDetails.places.map { ViewUtils.convertTimeStampToDate(it.lastCheckInTimestamp)!! }
            lastCheckinDate = checkins.maxByOrNull { it }
        }

        val tz = TimeZone.getDefault()
        val cal = GregorianCalendar.getInstance(tz)
        val offsetInMillis = tz.getOffset(cal.timeInMillis)

        val diff = (getDataDiffFromNow(lastCheckinDate?.time!!) - offsetInMillis)
        if (diff < MILLISECONDS_IN_DAY) {
            val pulseLongTime = MILLISECONDS_IN_DAY - diff
            valueAnimator.repeatCount = (pulseLongTime.toInt() / valueAnimator.duration.toInt())
            valueAnimator.startDelay = delay
            valueAnimator.start()
        }
    }

    fun createBounceAnimationFocusedPlace(
        marker: Marker,
        placeDetails: PolygonPlaceDetails
    ): ValueAnimator? {
        // Prep the animator
        var lastCheckinDate = ViewUtils.convertTimeStampToDate(placeDetails.lastCheckInTimestamp)
        val tz = TimeZone.getDefault()
        val cal = GregorianCalendar.getInstance(tz)
        val offsetInMillis = tz.getOffset(cal.timeInMillis)

        val diff = (getDataDiffFromNow(lastCheckinDate?.time!!) - offsetInMillis)
        if (diff < MILLISECONDS_IN_DAY) {
            val scaleHolder = PropertyValuesHolder.ofFloat(
                "scale",
                1F,
                1.08F,
                1F
            )
            val valueAnimator = ValueAnimator()
            valueAnimator.repeatMode = ValueAnimator.RESTART
            valueAnimator.setValues(scaleHolder)
            valueAnimator.duration = 1450
            valueAnimator.setEvaluator(FloatEvaluator())
            valueAnimator.interpolator = BounceInterpolator()
            valueAnimator.addUpdateListener { valueAnimator ->
                val animatedScale = valueAnimator.getAnimatedValue("scale") as Float
                marker.setAnchor(0.5f, (0.5f * animatedScale))
            }

            return valueAnimator
        } else {
            return null
        }
    }

    fun startBounceAnimationFocusedPlace(
        placeDetails: PolygonPlaceDetails,
        valueAnimator: ValueAnimator,
        delay: Long = 0L
    ) {
        var lastCheckinDate = ViewUtils.convertTimeStampToDate(placeDetails.lastCheckInTimestamp)

        val tz = TimeZone.getDefault()
        val cal = GregorianCalendar.getInstance(tz)
        val offsetInMillis = tz.getOffset(cal.timeInMillis)

        val diff = (getDataDiffFromNow(lastCheckinDate?.time!!) - offsetInMillis)
        if (diff < MILLISECONDS_IN_DAY) {
            val pulseLongTime = MILLISECONDS_IN_DAY - diff
            valueAnimator.repeatCount = (pulseLongTime.toInt() / valueAnimator.duration.toInt())
            valueAnimator.startDelay = delay
            valueAnimator.start()
        }
    }
}