package com.orbis.orbis.models.subscriptions

import com.google.gson.annotations.SerializedName

data class SubscriptionStatistic(
    @SerializedName("resultList")
    val resultList: List<Result>,
    @SerializedName("totalNumber")
    val totalNumber: Int,
    @SerializedName("totalAmount")
    val totalAmount: Float
)

data class Result(
    @SerializedName("columnName")
    val columnName: String,
    @SerializedName("number")
    val number: Int,
    @SerializedName("amount")
    val amount: Float
)