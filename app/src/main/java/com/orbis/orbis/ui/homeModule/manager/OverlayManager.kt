package com.orbis.orbis.ui.homeModule.manager

import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.orbis.orbis.models.place.PolygonPlaceDetails

interface OverlayManager {
    val places: List<PolygonPlaceDetails>

    fun containsPlace(place: PolygonPlaceDetails): Boolean
    fun addPlaceToMemory(place: PolygonPlaceDetails)
    fun removePlaceFromMemory(place: PolygonPlaceDetails)
    fun replacePlace(old: PolygonPlaceDetails, new: PolygonPlaceDetails)

    fun registerOverlay(overlay: GroundOverlay, place: PolygonPlaceDetails)
    fun registerPolygon(overlay: GroundOverlay, polygon: Polygon, place: PolygonPlaceDetails)
    fun removeOverlay(place: PolygonPlaceDetails)
    fun removeAll()
    fun findOverlay(place: PolygonPlaceDetails): GroundOverlay?
    fun getPlaceForOverlay(overlay: GroundOverlay): PolygonPlaceDetails?
    fun getPolygonForOverlay(overlay: GroundOverlay): Polygon?
    fun getOverlayForPolygon(polygon: Polygon): GroundOverlay?

    suspend fun updateInBoundsAsync(lastUpdatedTime: Long, cameraTarget: LatLng, onShouldCreate: (PolygonPlaceDetails) -> Unit, onShouldRemove: (PolygonPlaceDetails) -> Unit): PolygonPlaceDetails?
    fun enterFocusMode(focusedPlace: PolygonPlaceDetails)
    fun restoreAllVisibility()
}