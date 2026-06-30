package com.orbis.orbis.models.auth

import com.google.gson.annotations.SerializedName

data class FacebookProfile(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("first_name") val first_name: String,
    @SerializedName("last_name") val last_name: String,
    @SerializedName("email") val email: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("birthday") val birthday: String?,
    @SerializedName("picture") val picture: Picture,
)

data class Picture(
    @SerializedName("data") val data: Data
)

data class Data(
    @SerializedName("height") val height: Int,
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
)
