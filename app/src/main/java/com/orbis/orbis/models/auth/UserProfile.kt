package com.orbis.orbis.models.auth

import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.Coordinates

data class UserProfile(
    @SerializedName("coordinates") val coordinates: Coordinates,
    @SerializedName("userKey") val userKey: String,
    @SerializedName("providerImageUrl") val providerImageUrl: String,
    @SerializedName("unit") val unit: String,
    @SerializedName("imageName") val imageName: String,
    @SerializedName("language") val language: String,
    @SerializedName("dateOfBirth") val dateOfBirth: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("accountPrivate") val accountPrivate: Boolean,
    @SerializedName("shareLink") val shareLink: String,
    @SerializedName("superAdmin") val superAdmin: String,
    @SerializedName("email") val email: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("activeServerTimestamp") val activeServerTimestamp: String,
    @SerializedName("idToken") val idToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("partnerKey") val partnerKey: String,
)

