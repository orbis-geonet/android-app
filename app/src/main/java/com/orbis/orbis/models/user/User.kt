package com.orbis.orbis.models.user

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.Coordinates
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @SerializedName("coordinates") val coordinates: Coordinates,
    @SerializedName("userKey") val userKey: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("imageName") val imageName: String? = null,
    @SerializedName("providerImageUrl") val providerImageUrl: String = "",
    @SerializedName("accountPrivate") val accountPrivate: Boolean,
    @SerializedName("deleted") val deleted: Boolean,
    @SerializedName("seen") val seen: Boolean,
    @SerializedName("codes") val codes: ArrayList<String>? = ArrayList(),
    @SerializedName("user") val user: UserModel
) : Parcelable

@Parcelize
data class UserModel(
    @SerializedName("displayName") val displayName: String,
) : Parcelable
