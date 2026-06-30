package com.orbis.orbis.models

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Coordinates(
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("latitude") val latitude: Double = 0.0,
) : Parcelable
{
    fun toLatLng() : LatLng
    {
        return LatLng(latitude, longitude)
    }
}

