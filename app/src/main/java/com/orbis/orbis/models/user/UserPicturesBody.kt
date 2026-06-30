package com.orbis.orbis.models.user

import com.google.gson.annotations.SerializedName

data class UserPicturesBody(
    @SerializedName("pictureUrl") val pictureUrl: ArrayList<String> = ArrayList()
)
