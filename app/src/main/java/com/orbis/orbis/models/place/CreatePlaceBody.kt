package com.orbis.orbis.models.place

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.Coordinates
import kotlinx.parcelize.Parcelize

@Parcelize
data class CreatePlaceBody(
    @SerializedName("coordinates") var coordinates: Coordinates? = null,
    @SerializedName("userCoordinates") var userCoordinates: Coordinates? = null,
    @SerializedName("groupCreatedKey") var groupCreatedKey: String = "",
    @SerializedName("name") var name: String = "",
    @SerializedName("type") var type: String = "",
    @SerializedName("address") var address: String = "",

) : Parcelable
