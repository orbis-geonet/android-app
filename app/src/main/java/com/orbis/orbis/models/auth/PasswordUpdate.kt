package com.orbis.orbis.models.auth

import com.google.gson.annotations.SerializedName

data class PasswordUpdate(
    @SerializedName("email") var email: String = "",
    @SerializedName("oldPassword") var oldPassword: String = "",
    @SerializedName("newPassword") var newPassword: String = "",
)
