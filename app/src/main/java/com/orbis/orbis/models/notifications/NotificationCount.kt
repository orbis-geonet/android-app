package com.orbis.orbis.models.notifications

import com.google.gson.annotations.SerializedName

data class NotificationCount(
    @SerializedName("notifications") val notifications: Int,
    @SerializedName("pendingRequests") val pendingRequests: Int,
)
