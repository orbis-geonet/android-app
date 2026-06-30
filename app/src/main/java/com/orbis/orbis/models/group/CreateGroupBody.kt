package com.orbis.orbis.models.group

import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.Coordinates

data class CreateGroupBody(
    @SerializedName("name") var name: String = "",
    @SerializedName("location") var location: Coordinates? = null,
    @SerializedName("description") var description: String = "",
    @SerializedName("imageName") var imageName: String = "",
    @SerializedName("colorIndex") var colorIndex: Int = -1,
    @SerializedName("strokeColorHex") var strokeColorHex: String = "",
    @SerializedName("os") var os: String = "android",
)