package com.orbis.orbis.ui.homeModule.manager

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polygon
import com.orbis.orbis.helpers.CoordinatesUtil
import com.orbis.orbis.models.place.PolygonPlaceDetails
import com.orbis.orbis.models.place.cancelAnimation
import com.orbis.orbis.models.place.isCircle
import android.graphics.Color
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.VisibleRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MapOverlayManager(private val map: GoogleMap) : OverlayManager {
    private val _places = ArrayList<PolygonPlaceDetails>()
    override val places: List<PolygonPlaceDetails> get() = _places

    private val placeOverlays: MutableMap<GroundOverlay, PolygonPlaceDetails> = mutableMapOf()
    private val associatedPolygons: HashMap<GroundOverlay, Polygon?> = hashMapOf()
    private val associatedOverlays: HashMap<Polygon, GroundOverlay> = hashMapOf()
    private val willBePlaceOverlays: ArrayList<PolygonPlaceDetails> = ArrayList()
    private val polygonPlacesKeys: ArrayList<String> = ArrayList()
    private val associatedPopupMarkers: HashMap<GroundOverlay, Marker?> = hashMapOf()

    override fun containsPlace(place: PolygonPlaceDetails): Boolean {
        // polygon places use key list for fast lookup (mirrors original logic)
        if (!place.isCircle())
            return polygonPlacesKeys.contains(place.placeKey)
        return _places.any { it.placeKey == place.placeKey }
    }

    override fun addPlaceToMemory(place: PolygonPlaceDetails) {
        _places.add(place)
        if (!place.isCircle() && place.polygonCenter != null)
            polygonPlacesKeys.addAll(place.places.map { it.placeKey })
    }

    override fun removePlaceFromMemory(place: PolygonPlaceDetails) {
        val target = _places.find { it.placeKey == place.placeKey } ?: return
        target.cancelAnimation()
        _places.remove(target)
        if (!target.isCircle())
            polygonPlacesKeys.removeAll(target.places.map { it.placeKey }.toSet())
    }

    override fun replacePlace(old: PolygonPlaceDetails, new: PolygonPlaceDetails) {
        removePlaceFromMemory(old)
        addPlaceToMemory(new)
        willBePlaceOverlays.add(new)
    }

    override fun registerOverlay(
        overlay: GroundOverlay,
        place: PolygonPlaceDetails
    ) {
        placeOverlays[overlay] = place
    }

    override fun registerPolygon(
        overlay: GroundOverlay,
        polygon: Polygon,
        place: PolygonPlaceDetails
    ) {
        associatedPolygons[overlay] = polygon
        associatedOverlays[polygon] = overlay
    }

    override fun findOverlay(place: PolygonPlaceDetails): GroundOverlay? = placeOverlays.entries.firstOrNull { it.value.placeKey == place.placeKey }?.key

    override fun getPlaceForOverlay(overlay: GroundOverlay): PolygonPlaceDetails? =
        placeOverlays[overlay]

    override fun getPolygonForOverlay(overlay: GroundOverlay): Polygon? =
        associatedPolygons[overlay]

    override fun getOverlayForPolygon(polygon: Polygon): GroundOverlay? =
        associatedOverlays[polygon]

    override fun removeOverlay(place: PolygonPlaceDetails) {
        val entry = placeOverlays.entries.find { it.value.placeKey == place.placeKey } ?: return
        val overlay = entry.key
        val polygon = associatedPolygons[overlay]
        val popupMarker = associatedPopupMarkers[overlay]

        polygon?.remove()
        overlay.remove()
        popupMarker?.remove()

        associatedPolygons.remove(overlay)
        associatedPopupMarkers.remove(overlay)
        if (polygon != null) associatedOverlays.remove(polygon)
        placeOverlays.remove(overlay)
        willBePlaceOverlays.removeAll { it.placeKey == place.placeKey }
    }

    override fun removeAll() {
        placeOverlays.keys.forEach { overlay ->
            associatedPolygons[overlay]?.remove()
            associatedPopupMarkers[overlay]?.remove()
            overlay.remove()
        }
        placeOverlays.clear()
        associatedPolygons.clear()
        associatedOverlays.clear()
        associatedPopupMarkers.clear()
        willBePlaceOverlays.clear()
        polygonPlacesKeys.clear()
        _places.clear()
    }
    fun registerPopupMarker(overlay: GroundOverlay, marker: Marker) {
        associatedPopupMarkers[overlay] = marker
    }

    /**
     * Called from onCameraIdle. Decides which overlays should be shown or
     * hidden based on current map bounds and visible radius.
     * @param lastUpdatedTime  epoch seconds of last camera move — owned by Fragment
     * @param onShouldCreate   called for places that are in bounds but not yet drawn
     * @param onShouldRemove   called for places that are out of bounds
     */
    override suspend fun updateInBoundsAsync(
        lastUpdatedTime: Long,
        cameraTarget: LatLng,
        onShouldCreate: (PolygonPlaceDetails) -> Unit,
        onShouldRemove: (PolygonPlaceDetails) -> Unit
    ): PolygonPlaceDetails? {
        val (bounds, visibleRadius, placeSnapshot) = withContext(Dispatchers.Main) {
            Triple(
                map.projection.visibleRegion.latLngBounds,
                getMapVisibleRadius(),
                _places.toList()
            )
        }

        val nowSec = System.currentTimeMillis() / 1000
        val shouldForceRemove = (nowSec - lastUpdatedTime) >= 2

        val (toCreate, toRemove) = withContext(Dispatchers.Default) {
            val createList = mutableListOf<PolygonPlaceDetails>()
            val removeList = mutableListOf<PolygonPlaceDetails>()

            for (place in placeSnapshot) {
                place.polygonCenter ?: continue
                val size = if (place.isCircle()) place.size else CoordinatesUtil.calculatePolygonRadius(place)

                if (visibleRadius >= size * 80) {
                    removeList.add(place)
                    continue
                }

                val inBounds = isPlaceInBounds(place, bounds)
                if (inBounds) {
                    if (place.dominantGroup != null && !willBePlaceOverlays.contains(place))
                        createList.add(place)
                } else if (shouldForceRemove) {
                    removeList.add(place)
                }
            }

            createList to removeList
        }

        withContext(Dispatchers.Main) {
            toCreate.forEach(onShouldCreate)
            toRemove.forEach(onShouldRemove)
        }

        // After removals are applied, find central polygon from remaining places
        return findCentralPolygon(cameraTarget)
    }

    private fun isPlaceInBounds(place: PolygonPlaceDetails, bounds: LatLngBounds): Boolean {
        return if (!place.isCircle()) {
            // sample every 100th point for performance
            place.polygonPoints.indices
                .filter { it % 100 == 0 }
                .any { bounds.contains(place.polygonPoints[it].toLatLng()) }
        } else {
            bounds.contains(place.coordinates!!.toLatLng())
        }
    }

    private fun getMapVisibleRadius(): Double {
        val visibleRegion: VisibleRegion = map.projection.visibleRegion
        val diagonalDistance = FloatArray(1)
        val farLeft = visibleRegion.farLeft
        val nearRight = visibleRegion.nearRight
        Location.distanceBetween(
            farLeft.latitude,
            farLeft.longitude,
            nearRight.latitude,
            nearRight.longitude,
            diagonalDistance
        )
        return (diagonalDistance[0] / 2).toDouble()
    }

    /**
     * Dims all overlays except the focused one.
     * The Fragment still owns the Circle and seekbar animation — those are pure UI.
     */
    override fun enterFocusMode(focusedPlace: PolygonPlaceDetails) {
        for ((overlay, place) in placeOverlays) {
            val polygon = associatedPolygons[overlay]
            if (place.placeKey != focusedPlace.placeKey) {
                overlay.isVisible = false
                overlay.isClickable = false
                polygon?.let {
                    changePolygonTransparency(it, 0.94f)
                    it.isClickable = false
                }
            } else {
                overlay.isClickable = false
                overlay.isVisible = false
                polygon?.let { changePolygonTransparency(it, 1f) }
            }
        }
    }

    /**
     * Restores all overlays to normal browsing state.
     * Called when camera moves or focus is dismissed.
     */
    override fun restoreAllVisibility() {
        for ((overlay, place) in placeOverlays) {
            val polygon = associatedPolygons[overlay]
            val placeColor = Color.parseColor(place.dominantGroup!!.strokeColorHex)
            val normalColor = Color.argb(
                (Color.alpha(placeColor) * 0.35).toInt(),
                Color.red(placeColor),
                Color.green(placeColor),
                Color.blue(placeColor)
            )
            overlay.isVisible = true
            overlay.isClickable = true
            polygon?.apply {
                isVisible = true
                fillColor = normalColor
                isClickable = true
            }
        }
    }
    fun hidePopupMarker(overlay: GroundOverlay) {
        associatedPopupMarkers[overlay]?.isVisible = false
    }
    fun getPopupMarker(overlay: GroundOverlay): Marker? = associatedPopupMarkers[overlay]
    fun findCentralPolygon(cameraTarget: LatLng): PolygonPlaceDetails? {
        return _places
            .filter { /*it.places.size > 10 &&*/ it.polygonCenter != null && placeOverlays.any { (_, p) -> p.placeKey == it.placeKey } }
            .minByOrNull { place ->
                CoordinatesUtil.computeDistance(cameraTarget, place.polygonCenter!!.toLatLng())
            }
    }

    fun showOnlyPopupFor(place: PolygonPlaceDetails) {
        for ((overlay, p) in placeOverlays) {
            val marker = associatedPopupMarkers[overlay] ?: continue
            marker.isVisible = p.placeKey == place.placeKey
        }
    }

    fun hideAllPopupMarkers() {
        associatedPopupMarkers.values.forEach { it?.isVisible = false }
    }

    fun hasPopupMarker(place: PolygonPlaceDetails): Boolean {
        val overlay = findOverlay(place) ?: return false
        return associatedPopupMarkers[overlay] != null
    }

    fun markAsWillBeOverlay(place: PolygonPlaceDetails) {
        if (!willBePlaceOverlays.contains(place))
            willBePlaceOverlays.add(place)
    }

    private fun changePolygonTransparency(poly: Polygon, transparency: Float) {
        val color = poly.fillColor
        val newAlpha = (Color.alpha(color) * (1 - transparency)).toInt()
        poly.fillColor = Color.argb(
            newAlpha,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}