package com.orbis.orbis.models.place

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
class LocationInfoModel(
    @Expose @SerializedName("suburb") val suburb: String?,
    @Expose @SerializedName("cityDistrict") val cityDistrict: String?,
    @Expose @SerializedName("city") val city: String,
    @Expose @SerializedName("municipality") val municipality: String?,
    @Expose @SerializedName("county") val county: String?,
    @Expose @SerializedName("stateDistrict") val stateDistrict: String?,
    @Expose @SerializedName("state") val state: String?,
    @Expose @SerializedName("country") val country: String?,
    ) : Parcelable
