package com.orbis.orbis.ui.subscriptionsModule.statistics


data class FilterType(
    val name: String,
    val value: Type,
    var isSelected: Boolean = false
) {
    enum class Type(val value: String) {
        MONTH("MONTH"),
        TRIMESTER("TRIMESTER"),
        SEMESTER("SEMESTER"),
        YEAR("YEAR"),
    }
}