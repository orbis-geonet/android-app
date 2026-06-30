package com.orbis.orbis.models.place

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class CheckInStatus (
    @Expose @SerializedName("status") val status: String = "NEW"
) : Parcelable