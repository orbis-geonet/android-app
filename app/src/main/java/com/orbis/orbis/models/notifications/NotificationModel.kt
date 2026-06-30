package com.orbis.orbis.models.notifications

import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails

data class NotificationModel(
    @SerializedName("notificationKey") val notificationKey: String,
    @SerializedName("title") val title: String,
    @SerializedName("details") val details: String,
    @SerializedName("type") val type: String,
    @SerializedName("place") val place: PlaceDetails?,
    @SerializedName("seen") var seen: Boolean,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("fromUser") val fromUser: UserInfo?,
    @SerializedName("group") var group: GroupDetails?,
    @SerializedName("post") val post: FeedPost?,
)
