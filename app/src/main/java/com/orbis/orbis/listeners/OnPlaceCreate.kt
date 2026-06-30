package com.orbis.orbis.listeners

import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.place.PlaceDetails

interface OnPlaceCreate {
    public fun onPlaceCreate(placeDetails: PlaceDetails)
    public fun onPostCreate(feedPost: FeedPost)

}