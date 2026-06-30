package com.orbis.orbis.models.posts

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.Coordinates
import kotlinx.parcelize.Parcelize

data class PostBody(
    @SerializedName("coordinates") var coordinates: Coordinates?,
    @SerializedName("checkin") var checkin: Boolean,
    @SerializedName("title") var title: String = "",
    @SerializedName("details") var details: String = "",
    @SerializedName("richLinkData") var richLinkData: RichLinkData? = null,
    @SerializedName("mediaUrls") var mediaUrls: ArrayList<String> = ArrayList(),
    @SerializedName("plannedTime") var plannedTime: String = "",
    @SerializedName("plannedEndTime") var plannedEndTime: String = "",
    @SerializedName("groupKey") var groupKey: String? = null,
    @SerializedName("placeKey") var placeKey: String? = null,
    @SerializedName("address") var address: String = "",
    @SerializedName("type") var type: String = "",
    var subType: String = ""
    )

@Parcelize
data class RichLinkData(
    @SerializedName("canonicalUrl") var canonicalUrl: String = "",
    @SerializedName("description") var description: String = "",
    @SerializedName("imageUrl") var imageUrl: String = "",
    @SerializedName("originalUrl") var originalUrl: String = "",
    @SerializedName("title") var title: String = ""
) : Parcelable