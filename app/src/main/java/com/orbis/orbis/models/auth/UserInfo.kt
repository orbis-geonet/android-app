package com.orbis.orbis.models.auth

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.Coordinates
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserInfo(
    @SerializedName("coordinates") val coordinates: Coordinates? = null,
    @SerializedName("userKey") val userKey: String? = null,
    @SerializedName("displayName") var displayName: String = "",
    @SerializedName("imageName") var imageName: String? = null,
    @SerializedName("dateOfBirth") var dateOfBirth: String = "",
    @SerializedName("gender") var gender: String = "",
    @SerializedName("providerImageUrl") var providerImageUrl: String = "",
    @SerializedName("accountPrivate") var accountPrivate: Boolean = false,
    @SerializedName("seen") val seen: Boolean = false,
    @SerializedName("superAdmin") val superAdmin: Boolean = false,
    @SerializedName("email") var email: String? = null,
    @SerializedName("language") var language: String? = null,
    @SerializedName("following") var following: Boolean = false,
    @SerializedName("pending") val pending: Boolean = false,
    @SerializedName("blocked") val blocked: Boolean = false,
    @SerializedName("totalFollowing") val totalFollowing: Int = 0,
    @SerializedName("followedGroups") val followedGroups: Int = 0,
    @SerializedName("followedPlaces") val followedPlaces: Int = 0,
    @SerializedName("totalFollowers") var totalFollowers: Int = 0,
    @SerializedName("groupAdminCount") val groupAdminCount: Int = 0,
    @SerializedName("groupMemberCount") val groupMemberCount: Int = 0,
) : Parcelable
