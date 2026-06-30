package com.orbis.orbis.models.auth

import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.Constants

data class ProfileUpdateBody(
    @SerializedName("displayName") var displayName: String?,
    @SerializedName("providerImageUrl") var providerImageUrl: String?,
    @SerializedName("dateOfBirth") var dateOfBirth: String? = "",
    @SerializedName("gender") var gender: String? = "",
    @SerializedName("partnerKey") var partnerKey: String? = Constants.partnerKey
)
