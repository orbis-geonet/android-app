package com.orbis.orbis.models.subscriptions

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Subscription(
    @SerializedName("subscriptionKey")
    val subscriptionKey: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("groupKey")
    val groupKey: String,
    @SerializedName("groupName")
    val groupName: String?,
    @SerializedName("price")
    val price: Float,
    @SerializedName("originalPrice")
    val originalPrice: Float,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("benefit")
    val benefits: List<String>,
    @SerializedName("isSubscriber")
    var isSubscriber: Boolean,
    @SerializedName("imagesName") val imagesName: ArrayList<String>? = ArrayList(),
    @SerializedName("type") val type: String,
    @SerializedName("interval") val interval: String,
    @SerializedName("period") val period: Int,
    @SerializedName("codes") val codes: ArrayList<String>? = ArrayList(),
    @SerializedName("quantity") var quantity: Int = 1,

) : Parcelable