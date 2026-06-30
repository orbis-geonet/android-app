package com.orbis.orbis.models.auth

import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.Constants

data class SignUpBody(
    @SerializedName("displayName") var displayName: String = "",
    @SerializedName("email") var email: String = "",
    @SerializedName("password") var password: String = "",
    @SerializedName("partnerKey") var partnerKey: String? = Constants.partnerKey
)
