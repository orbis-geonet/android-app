package com.orbis.orbis.models.subscriptions

import com.google.gson.annotations.SerializedName

data class SubscriptionInfo(
    @SerializedName("orbisCommission")
    val orbisCommission: Float,
    @SerializedName("stripeCommission")
    val stripeCommission: Float,
    @SerializedName("stripeAdditionFee")
    val stripeAdditionFee: Float,
    @SerializedName("currencies")
    val currencies: List<String>
)