package com.orbis.orbis.ui.subscriptionsModule.statistics


data class Progress(
    val value: Int,
    val total: Int,
    val title: String,
    var isSelected: Boolean = false
)