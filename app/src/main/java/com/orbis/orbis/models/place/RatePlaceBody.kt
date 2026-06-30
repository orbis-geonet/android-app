package com.orbis.orbis.models.place

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RatePlaceBody(
    @SerializedName("placeKey") var placeKey: String,
    @SerializedName("rate") var rate: Int
) : Parcelable
