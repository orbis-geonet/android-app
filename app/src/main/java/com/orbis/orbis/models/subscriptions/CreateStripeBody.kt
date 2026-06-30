package com.orbis.orbis.models.subscriptions

import com.google.gson.annotations.SerializedName

data class CreateStripeBody(
    @SerializedName("country")
    val country: String,
    @SerializedName("businessType")
    val businessType: String
)