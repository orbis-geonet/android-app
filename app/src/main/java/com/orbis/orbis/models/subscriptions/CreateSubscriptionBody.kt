package com.orbis.orbis.models.subscriptions

import com.google.gson.annotations.SerializedName

data class CreateSubscriptionBody(
    @SerializedName("subscriptionKey") val subscriptionKey: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Float,
    @SerializedName("originalPrice") val originalPrice: Float,
    @SerializedName("currency") val currency: String,
    @SerializedName("type") val type: String,
    @SerializedName("interval") val interval: String,
    @SerializedName("period") val period: Int,
    @SerializedName("description") val description: String,
    @SerializedName("benefit") val benefit: List<String>,
    @SerializedName("imagesName") val imagesName: ArrayList<String> = ArrayList()
)