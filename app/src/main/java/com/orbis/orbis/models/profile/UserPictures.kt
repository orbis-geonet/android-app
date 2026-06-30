package com.orbis.orbis.models.profile

import com.google.gson.annotations.SerializedName

data class UserPictures(
    @SerializedName("userKey") val userKey: String,
    @SerializedName("pictureUrl") val pictureUrl: ArrayList<String>,
    @SerializedName("pictureKey") val pictureKey: String = "",
    @SerializedName("type") val type: String = "",
)
