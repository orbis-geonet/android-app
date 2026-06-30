package com.orbis.orbis.helpers

enum class PlaceType(private val rawValue: String) {
    SHOPPING("Shopping"), PARK("Park"),
    BAR("Bar"), RESTAURANT("Restaurant"),
    SCHOOL("School"), LOCATION("Location"),
    TWO_BUILDINGS(">Building 2"), SPORTS_CENTER("Sports Center"),
    CASTLE("Castle"), HOUSE_2("House 2"),
    MUSIC("Music"), FAST_FOOD("Fast Food"),
    HOUSE("House"), BUILDING("Building"), BEACH("Beach");

    open fun rawValue(): String {
        return rawValue
    }


}