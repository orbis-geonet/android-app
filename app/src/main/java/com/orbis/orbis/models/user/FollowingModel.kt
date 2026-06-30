package com.orbis.orbis.models.user

import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.auth.UserInfo

data class FollowingModel(
    @SerializedName("user") val user: UserInfo,
    @SerializedName("type") val type: String,
)
