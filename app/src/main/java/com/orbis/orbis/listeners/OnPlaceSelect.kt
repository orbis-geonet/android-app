package com.orbis.orbis.listeners

import com.orbis.orbis.models.place.PlaceDetails

interface OnPlaceSelect {
    public fun onPlaceSelect(placeDetails: PlaceDetails)
}