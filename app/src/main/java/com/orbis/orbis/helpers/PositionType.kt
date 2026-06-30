package com.orbis.orbis.helpers

enum class PositionType(private val rawValue: String) {
    TOPCENTER("TopCenter"), BOTTOMCENTER("BottomCenter"),
    LEFTCENTER("LeftCenter"), RIGHTCENTER("RightCenter"),
    TOPLEFT("TopLeft"), BOTTOMRIGHT("BottomRight");

    open fun rawValue(): String {
        return rawValue
    }
}