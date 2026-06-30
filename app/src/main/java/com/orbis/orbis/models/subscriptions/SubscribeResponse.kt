package com.orbis.orbis.models.subscriptions

import com.google.gson.annotations.SerializedName

data class SubscribeResponse(
    @SerializedName("clientSecret")
    val clientSecret: String,
    @SerializedName("publicToken")
    val publicToken: String,
    var position: Int = 0,
)