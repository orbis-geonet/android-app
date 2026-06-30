package com.orbis.orbis.ui.homeModule.manager

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.maps.android.ui.IconGenerator
import com.orbis.orbis.R
import com.orbis.orbis.helpers.AnimationsUtil
import com.orbis.orbis.helpers.CoordinatesUtil
import com.orbis.orbis.helpers.PlaceIcon
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.place.PolygonPlaceDetails
import com.orbis.orbis.models.place.computeSize
import com.orbis.orbis.models.place.getCenterLatLng
import com.orbis.orbis.models.place.getNormalColor
import com.orbis.orbis.models.place.getPlaceColor
import com.orbis.orbis.models.place.getTransparentColor
import com.orbis.orbis.models.place.isCircle
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.utils.Utils
import io.getstream.avatarview.AvatarView
import androidx.core.graphics.toColorInt
import kotlin.math.cos
import kotlin.math.pow
import androidx.core.graphics.createBitmap
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.user.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import androidx.core.graphics.scale

/**
 * Handles all polygon/overlay/marker rendering logic.
 *
 * Responsibilities:
 *  - Creating and animating GroundOverlays + Polygons for map places
 *  - Drawing focused place markers (with selection state, scaling, bounce anim)
 *  - Precomputing image drawables for sub-places (cache warming)
 *  - Drawing the enclosing circle on overlay click
 */
class PolygonRenderer(
    private val context: Context,
    private val map: GoogleMap,
    private val overlayManager: MapOverlayManager,
    private val isInFocusMode: () -> Boolean,
    private val areClicksDisabled: () -> Boolean,
    private val onLoadingDone: () -> Unit,
    private val onGroundOverlayClick: (GroundOverlay) -> Unit,
    private val onMarkerClick: (Marker) -> Unit,
    private val onPopupDataNeeded: (groupKey: String) -> Unit
) {
    var lastPlaceCreatedKey = ""
    var focusPolygonMarkers: MutableMap<Marker, PolygonPlaceDetails> = mutableMapOf()
    private val pendingPopups: MutableMap<String, Pair<LatLng, PolygonPlaceDetails>> = mutableMapOf()

    /**
     * Entry point called for each new place that needs to be rendered on the map.
     * Loads the dominant group image, then creates the overlay + polygon pair.
     */
    suspend fun createGroundOverlayAndPolygon(placeDetails: PolygonPlaceDetails) {
        if (placeDetails.dominantGroup == null){
            Log.d("PolygonRenderer", "No group associated to polygon ${placeDetails.palindromeKey}")
            return
        }

        val polygonData = withContext(Dispatchers.Default) {
            placeDetails.computeSize()
            createNewPolygonData(placeDetails)
        }

        val iconGenerator = IconGenerator(context)
        loadGroupImageSuspend(placeDetails, iconGenerator)

        withContext(Dispatchers.Main) {
            val polygonBundle = polygonData?.let {
                createNewPolygonFromData(it, placeDetails)
            }

            createNewGroundOverlay(placeDetails, iconGenerator)?.let { groundOverlay ->
                Handler(Looper.getMainLooper()).postDelayed({ onLoadingDone() }, 500)

                polygonBundle?.let { (newPolygon, polygonAnimation) ->
                    newPolygon.isVisible = !isInFocusMode()
                    polygonAnimation.start()
                    overlayManager.registerPolygon(groundOverlay, newPolygon, placeDetails)
                    // popup marker registration happens later in resolvePopupWithGroupDetails
                }

                val isCheckIn = placeDetails.places.map { it.placeKey }.contains(lastPlaceCreatedKey)
                if (isCheckIn) handleCheckInSituation(groundOverlay)

                startOverlayAnimations(groundOverlay, placeDetails)
                overlayManager.registerOverlay(groundOverlay, placeDetails)
            }
        }
    }

    /**
     * Warms the Glide cache for images of all sub-places inside a polygon place.
     * Call this proactively (e.g. on camera move start) so images are ready when needed.
     */
    fun precomputePlacesImageDrawables() {
        for(place in overlayManager.places){
            place.places
                .map { it.imageName }
                .filter { it.isNotEmpty() }
                .forEach { imageName ->
                    val storageRef = Firebase.storage.getReference(
                        Constants.PLACE_PICTURES + Utils.getImageUrl200(imageName)
                    )
                    GlideApp.with(context)
                        .load(storageRef)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(noOpRequestListener<Drawable>())
                        .submit()
                }
        }
    }


    /**
     * Draws a circular marker for a place in focus mode.
     *
     * @param place       The place to draw the marker for.
     * @param unique      True when this is the only place in the focused group
     *                    (triggers larger scale and no bounce animation).
     * @param zoomLevel   Current map zoom, used to detect marker proximity.
     * @param focusedPlaces All currently focused places (needed for proximity check).
     * @param onMarkerCreated Callback invoked with the created Marker so the caller
     *                        can register it in its own map.
     */
    fun drawFocusedPlaceMarker(
        place: PolygonPlaceDetails,
        unique: Boolean,
        zoomLevel: Float,
        focusedPlaces: List<PolygonPlaceDetails>
    ) {
        val inflatedView = android.view.View.inflate(context, R.layout.custom_group_marker, null)
        val iconGenerator = IconGenerator(context)
        val imageView = inflatedView.findViewById<ImageView>(R.id.markerImage)
        val imageContainer = inflatedView.findViewById<LinearLayout>(R.id.markerCircle)

        val existingMarkers = focusPolygonMarkers

        val placeColor = (place.dominantGroup?.strokeColorHex ?: Color.BLUE.toString()).toColorInt()
        val closeToAnother = isTooCloseToAnotherMarker(place, zoomLevel, focusedPlaces)
        val sizePx = 64.dpToPx()
        val scale = when {
            unique           -> 0.95f
            closeToAnother   -> 0.47f
            else             -> 0.65f
        }

        imageContainer.background = buildMarkerBackground(place, placeColor, sizePx)

        imageView.setColorFilter(Color.WHITE)
        imageView.setImageDrawable(
            ContextCompat.getDrawable(context, PlaceIcon.getIconByType(place.type))
        )

        iconGenerator.setBackground(null)
        iconGenerator.setContentView(inflatedView)

        val rawBitmap = iconGenerator.makeIcon()
        val scaledBitmap = bitmapResizer(
            rawBitmap,
            (rawBitmap.width * scale).toInt(),
            (rawBitmap.height * scale).toInt()
        )

        val position = LatLng(place.coordinates!!.latitude, place.coordinates.longitude)
        val markerOptions = MarkerOptions()
            .position(position)
            .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap))
            .anchor(0.5f, 0.5f)

        val marker = map.addMarker(markerOptions) ?: return
        marker.tag = place

        if (!unique) {
            val anim = AnimationsUtil.createBounceAnimationFocusedPlace(marker, place)
            anim?.let { AnimationsUtil.startBounceAnimationFocusedPlace(place, it) }
        }

        focusPolygonMarkers[marker] = place
    }

    /**
     * Draws the translucent enclosing circle shown when an overlay is clicked.
     * Returns the Circle so the caller can hold a reference for removal.
     */
    fun drawEnclosingCircle(placeDetails: PolygonPlaceDetails): Circle {
        var originalColor = placeDetails.dominantGroup!!.strokeColorHex
        originalColor = if (originalColor.length > 7) {
            "#3D${originalColor.substring(3)}"
        } else {
            "#3D${originalColor.replace("#", "")}"
        }

        val circleSpecs = if (placeDetails.isCircle()) {
            Pair(placeDetails.coordinates!!.toLatLng(), placeDetails.size)
        } else {
            val points = placeDetails.places.mapNotNull { it.coordinates?.toLatLng() }
            CoordinatesUtil.minimumEnclosingCircle(ArrayList(points))
        }

        val radius = circleSpecs.second * 1.50
        return map.addCircle(
            CircleOptions()
                .center(circleSpecs.first)
                .radius(radius)
                .strokeWidth(1f)
                .strokeColor(Color.parseColor(originalColor))
                .fillColor(Color.parseColor(originalColor))
                .clickable(false)
        )
    }

    fun clearPolygonMarkers(){
        focusPolygonMarkers.keys.forEach { it.remove() }
        focusPolygonMarkers.clear()
    }

    data class PolygonComputation(
        val centroid: LatLng,
        val smoothed: List<LatLng>
    )

    private fun createNewPolygonData(placeDetails: PolygonPlaceDetails): PolygonComputation? {
        if (placeDetails.isCircle()) return null

        val raw = CoordinatesUtil.coordinatesToLatLng(placeDetails.polygonPoints)
        val smoothed = CoordinatesUtil.bspline(
            CoordinatesUtil.chaikin(raw, 2).toMutableList()
        )
        val centroid = CoordinatesUtil.calculateCentroid(smoothed)

        return PolygonComputation(centroid, smoothed)
    }

    private fun createNewPolygonFromData(
        data: PolygonComputation,
        placeDetails: PolygonPlaceDetails
    ): Pair<Polygon, ValueAnimator> {

        val options = PolygonOptions()
            .add(data.centroid)
            .strokeWidth(0f)
            .strokeColor(placeDetails.getPlaceColor())
            .zIndex(900F - placeDetails.size.toLong())
            .fillColor(if (isInFocusMode()) placeDetails.getTransparentColor() else placeDetails.getNormalColor())
            .clickable(!isInFocusMode() && !areClicksDisabled())

        val polygon = map.addPolygon(options)

        val anim = AnimationsUtil.createPolygonAppearingAnimation(
            polygon, data.centroid, data.smoothed
        )

        return Pair(polygon, anim)
    }

    fun requestPopupIfNeeded(place: PolygonPlaceDetails) {
        if (overlayManager.hasPopupMarker(place)) return

        val groupKey = place.dominantGroup?.groupKey ?: return
        if (pendingPopups.containsKey(groupKey)) return

        val northPoint = if (!place.isCircle()) {
            val raw = CoordinatesUtil.coordinatesToLatLng(place.polygonPoints)
            raw.maxByOrNull { it.latitude } ?: place.getCenterLatLng()
        } else {
            place.computeSize()
            val center = place.getCenterLatLng()
            val radiusInDegrees = place.size / 111320.0
            LatLng(center.latitude + radiusInDegrees, center.longitude)
        }

        pendingPopups[groupKey] = Pair(northPoint, place)
        onPopupDataNeeded(groupKey)
    }

    private fun addGroupPopupMarker(
        northPoint: LatLng,
        placeDetails: PolygonPlaceDetails,
        groupDetails: GroupDetails,
        members: List<User>
    ) {
        val markerView = android.view.LayoutInflater.from(context)
            .inflate(R.layout.map_popup, null, false)

        val placeColor = placeDetails.getPlaceColor()
        val bg = ContextCompat.getDrawable(context, R.drawable.bg_group_popup)!!
            .mutate() as GradientDrawable
        bg.setStroke(3.dpToPx(), placeColor)
        markerView.background = bg
        markerView.alpha = 0.95f

        markerView.findViewById<TextView>(R.id.title).text = groupDetails.name
        markerView.findViewById<TextView>(R.id.groupDescription).text = groupDetails.description
        markerView.findViewById<TextView>(R.id.members).text = "${groupDetails.membersCount} Members"

        val avatarMembers = members.filter { !it.imageName.isNullOrEmpty() }.take(3)
        val avatarViews = listOf(
            markerView.findViewById(R.id.avatar1),
            markerView.findViewById(R.id.avatar2),
            markerView.findViewById<ImageView>(R.id.avatar3)
        )

        // Set placeholders immediately on all three
        avatarViews.forEach { it.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_user)) }

        var loadsDone = 0

        fun onAvatarProcessed() {
            loadsDone++
            if (loadsDone == avatarMembers.size) {
                renderAndPlace(markerView, northPoint, placeDetails)
            }
        }

        if (avatarMembers.isEmpty()) {
            renderAndPlace(markerView, northPoint, placeDetails)
            return
        }

        avatarViews.forEachIndexed { index, imageView ->
            val user = avatarMembers.getOrNull(index)
            if (user?.imageName == null) {
                onAvatarProcessed()
                return@forEachIndexed
            }

            val storageRef = Firebase.storage.getReference(Constants.PROFILE_PICTURES + Utils.getImageUrl200(user.imageName))

            GlideApp.with(context)
                .load(storageRef)
                .circleCrop()
                .listener(object : RequestListener<Drawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        runOnMain { onAvatarProcessed() }
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        runOnMain {
                            imageView.setImageDrawable(resource)
                            onAvatarProcessed()
                        }
                        return true
                    }
                })
                .submit()
        }
    }

    private fun renderAndPlace(
        markerView: android.view.View,
        northPoint: LatLng,
        placeDetails: PolygonPlaceDetails
    ) {
        val bitmap = renderMarkerView(markerView)

        val offsetDegrees = (placeDetails.size / 111_000) * 0.1
        val adjustedNorth = LatLng(northPoint.latitude - offsetDegrees, northPoint.longitude)
        val overlay = overlayManager.findOverlay(placeDetails) ?: return

        val existingMarker = overlayManager.getPopupMarker(overlay)
        if (existingMarker != null) {
            existingMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap))
            return
        }

        val marker = map.addMarker(
            MarkerOptions()
                .position(adjustedNorth)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .anchor(0.5f, 1.0f)
                .zIndex(1100f)
                .visible(false)
        ) ?: return

        marker.tag = placeDetails
        overlayManager.registerPopupMarker(overlay, marker)

        AnimationsUtil.createPopupMarkerAppearingAnimation(marker, bitmap, isInFocusMode).start()
        overlayManager.hideAllPopupMarkers()
    }

    private fun renderMarkerView(markerView: android.view.View): Bitmap {
        val density = context.resources.displayMetrics.density
        val scale = density.coerceAtLeast(2f)
        val logicalWidthPx = 220.dpToPx()

        markerView.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(logicalWidthPx, android.view.View.MeasureSpec.AT_MOST),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)
        markerView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

        val hiBitmap = createBitmap((markerView.measuredWidth * scale).toInt(), (markerView.measuredHeight * scale).toInt())
        val canvas = Canvas(hiBitmap)
        canvas.scale(scale, scale)
        markerView.draw(canvas)

        val finalBitmap = hiBitmap.scale(markerView.measuredWidth, markerView.measuredHeight)
        hiBitmap.recycle()
        return finalBitmap
    }

    fun resolvePopupWithGroupDetails(groupDetails: GroupDetails, members: List<User>) {
        val placeKey = groupDetails.groupKey
        val pending = pendingPopups.remove(placeKey) ?: return
        val (northPoint, placeDetails) = pending
        addGroupPopupMarker(northPoint, placeDetails, groupDetails, members)
    }

    private suspend fun loadGroupImageSuspend(
        placeDetails: PolygonPlaceDetails,
        iconGenerator: IconGenerator
    ) = suspendCancellableCoroutine<Unit> { cont ->

        loadGroupImage(placeDetails, iconGenerator) {
            if (cont.isActive) cont.resume(Unit)
        }
    }

    private fun createNewGroundOverlay(
        placeDetails: PolygonPlaceDetails,
        iconGenerator: IconGenerator
    ): GroundOverlay? {
        val diameter = placeDetails.size.toFloat() * 2.0f
        return map.addGroundOverlay(
            GroundOverlayOptions()
                .position(placeDetails.getCenterLatLng(), diameter)
                .image(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon()))
                .zIndex(1000F - placeDetails.size.toLong())
                .clickable(!isInFocusMode() && !areClicksDisabled())
                .visible(!isInFocusMode())
        )
    }

    private fun startOverlayAnimations(groundOverlay: GroundOverlay, placeDetails: PolygonPlaceDetails) {
        val pulseAnim = AnimationsUtil.createPulseAnimation(groundOverlay, placeDetails)
        val popupAnim = AnimationsUtil.createPopupAnimation(groundOverlay, placeDetails)
        placeDetails.animator = pulseAnim ?: popupAnim
        popupAnim.start()
        if (pulseAnim != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                AnimationsUtil.startPulseAnimation(placeDetails, pulseAnim)
            }, 2000L)
        }
    }
    private fun handleCheckInSituation(groundOverlay: GroundOverlay){
        Handler(Looper.getMainLooper()).postDelayed({
            onGroundOverlayClick(groundOverlay)
            Handler(Looper.getMainLooper()).postDelayed({
                for (entry in focusPolygonMarkers.entries){
                    if (entry.value.placeKey == lastPlaceCreatedKey)
                        onMarkerClick(entry.key)
                }
                lastPlaceCreatedKey = ""
            }, 150)
        }, 2000)
    }

    private fun loadGroupImage(
        placeDetails: PolygonPlaceDetails,
        iconGenerator: IconGenerator,
        onReady: () -> Unit,
    ) {
        val storageRef = Firebase.storage.getReference(
            Constants.GROUP_PHOTO_STORAGE + placeDetails.dominantGroup?.imageName?.let {
                Utils.getImageUrl200(it)
            }
        )

        GlideApp.with(context)
            .load(storageRef)
            .circleCrop()
            .format(DecodeFormat.PREFER_ARGB_8888)
            .listener(object : RequestListener<Drawable> {

                override fun onLoadFailed(
                    e: GlideException?, model: Any?,
                    target: Target<Drawable>?, isFirstResource: Boolean
                ): Boolean {
                    runOnMain {
                        val view = android.view.View.inflate(context, R.layout.custom_group_marker, null)
                        view.findViewById<ImageView>(R.id.markerImage).setImageDrawable(
                            ContextCompat.getDrawable(context, PlaceIcon.getIconByType(placeDetails.type))
                        )
                        iconGenerator.setBackground(null)
                        iconGenerator.setContentView(view)
                        onReady()
                    }
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable?, model: Any?,
                    target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean
                ): Boolean {
                    runOnMain {
                        val view = android.view.View.inflate(context, R.layout.custom_map_places_v2, null)
                        val iconImage = view.findViewById<AvatarView>(R.id.ivMapPic)
                        iconImage.avatarBorderWidth = 7
                        iconImage.avatarBorderColor = placeDetails.getPlaceColor()
                        iconImage.setImageDrawable(resource)
                        iconGenerator.setBackground(null)
                        iconGenerator.setContentView(view)
                        onReady()
                    }
                    return true
                }
            }).submit()
    }

    private fun buildMarkerBackground(
        place: PolygonPlaceDetails,
        placeColor: Int,
        sizePx: Int
    ): Drawable {
        val base = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setSize(sizePx, sizePx)
            setColor(placeColor)
            setStroke(2.dpToPx(), placeColor)
        }

        if (!place.isFocusSelected) return base

        val strokeValue = 7
        val outline = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
            setSize(sizePx + 2 * strokeValue, sizePx + 2 * strokeValue)
        }
        return LayerDrawable(arrayOf(outline, base)).apply {
            setLayerInset(1,
                strokeValue.dpToPx(), strokeValue.dpToPx(),
                strokeValue.dpToPx(), strokeValue.dpToPx()
            )
        }
    }

    private fun isTooCloseToAnotherMarker(
        place: PolygonPlaceDetails,
        zoomLevel: Float,
        focusedPlaces: List<PolygonPlaceDetails>,
    ): Boolean {
        val metersPerPixel = 156543.03392 *
                cos(Math.toRadians(place.coordinates!!.latitude)) /
                2.0.pow(zoomLevel.toDouble())
        val epsilon = 48 * metersPerPixel

        return focusedPlaces.any { other ->
            other.placeKey != place.placeKey &&
                    focusedTwoPlaceMarkerDistance(place, other) < epsilon
        }
    }

    private fun focusedTwoPlaceMarkerDistance(
        a: PolygonPlaceDetails,
        b: PolygonPlaceDetails
    ): Double {
        val latLngA = LatLng(a.coordinates!!.latitude, a.coordinates.longitude)
        val latLngB = LatLng(b.coordinates!!.latitude, b.coordinates.longitude)
        return CoordinatesUtil.computeDistance(latLngA, latLngB)
    }

    private fun bitmapResizer(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val result = createBitmap(newWidth, newHeight)
        val ratioX = newWidth / bitmap.width.toFloat()
        val ratioY = newHeight / bitmap.height.toFloat()
        val midX = newWidth / 2.0f
        val midY = newHeight / 2.0f
        val matrix = Matrix().apply { setScale(ratioX, ratioY, midX, midY) }
        val canvas = Canvas(result).apply { setMatrix(matrix) }
        canvas.drawBitmap(bitmap, midX - bitmap.width / 2, midY - bitmap.height / 2, Paint(Paint.FILTER_BITMAP_FLAG))
        return result
    }

    private fun Int.dpToPx(): Int =
        (this * context.resources.displayMetrics.density).toInt()

    private fun runOnMain(block: () -> Unit) =
        Handler(Looper.getMainLooper()).post(block)

    private fun <T> noOpRequestListener(): RequestListener<T> = object : RequestListener<T> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<T>?, isFirstResource: Boolean) = false
        override fun onResourceReady(resource: T?, model: Any?, target: Target<T>?, dataSource: DataSource?, isFirstResource: Boolean) = true
    }
}