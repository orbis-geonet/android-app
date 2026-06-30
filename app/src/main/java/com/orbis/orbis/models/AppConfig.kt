package com.orbis.orbis.models

import com.google.gson.annotations.SerializedName

data class AppConfig(
    var feedAdsFrequency: Int = 0,
    var interstitialAdTimeout: Int = 0,
    @field:JvmField var isAdsEnabled: Boolean = false,
    var mapInitialZoom: Double = 0.0,
    var storyAdsFrequency: Int = 0,


    )
