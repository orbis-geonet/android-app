package com.orbis.orbis.models.subscriptions

import com.google.gson.annotations.SerializedName

data class CreateStripeResponse(
    @SerializedName("stripeAccountKey")
    val stripeAccountKey: String,
    @SerializedName("setupAccountUrl")
    val setupAccountUrl: String,
)