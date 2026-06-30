package com.orbis.orbis.models.subscriptions

import com.google.gson.annotations.SerializedName

data class GetStripeResponse(
    @SerializedName("stripeAccountKey")
    val stripeAccountKey: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("userKey")
    val userKey: String,
    @SerializedName("businessType")
    val businessType: String,
    @SerializedName("country")
    val country: String,
    @SerializedName("fieldError")
    val fieldError: List<String>
)