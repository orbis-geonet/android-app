package com.orbis.orbis.models.user

import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.place.PlaceDetails

data class FollowingPlaceModel(
    @SerializedName("place") val place: PlaceDetails,
    @SerializedName("type") val type: String,
)
