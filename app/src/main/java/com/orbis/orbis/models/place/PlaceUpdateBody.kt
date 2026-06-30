package com.orbis.orbis.models.place

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.Coordinates

data class PlaceUpdateBody(
    @SerializedName("coordinates") var coordinates: Coordinates? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("type") var type: String? = null,
    @SerializedName("source") var source: String? = null,
    @SerializedName("address") var address: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("categoryKey") var categoryKey: String? = null,
    @SerializedName("cityKey") var cityKey: String? = null,
    @SerializedName("countryKey") var countryKey: String? = null,
    @SerializedName("phone") var phone: String? = null,
    @SerializedName("dominantGroupKey") var dominantGroupKey: String? = null,
    @SerializedName("googlePlaceId") var googlePlaceId: String? = null,
    @SerializedName("deleted") var deleted: String? = null,
    @SerializedName("imageName") var imageName: String? = null,
    @SerializedName("website") var website: String = "",
    @SerializedName("workingHours") var workingHours: ArrayList<WorkingHoursModel>? = null
)
