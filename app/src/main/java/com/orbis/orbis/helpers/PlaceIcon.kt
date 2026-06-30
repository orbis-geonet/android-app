package com.orbis.orbis.helpers

import android.annotation.SuppressLint
import android.os.Parcelable
import com.orbis.orbis.R

import kotlinx.parcelize.Parcelize


@SuppressLint("ParcelCreator")
@Parcelize
class PlaceIcon(var type: PlaceType, var icon: Int) : Parcelable {

    companion object {
        fun getIconByType(placeType: String): Int {
            when (placeType) {
                PlaceType.LOCATION.name -> {
                    return R.drawable.ic_group_1
                }
                PlaceType.BUILDING.name -> {
                    return R.drawable.ic_group_2
                }
                PlaceType.BAR.name -> {
                    return R.drawable.ic_group_3
                }
                PlaceType.HOUSE.name -> {
                    return R.drawable.ic_group_15
                }
                PlaceType.CASTLE.name -> {
                    return R.drawable.ic_group_5
                }
                PlaceType.SPORTS_CENTER.name -> {
                    return R.drawable.ic_group_6
                }
                PlaceType.FAST_FOOD.name -> {
                    return R.drawable.ic_group_7
                }
                PlaceType.SHOPPING.name -> {
                    return R.drawable.ic_group_8
                }
                PlaceType.RESTAURANT.name -> {
                    return R.drawable.ic_group_9
                }
                PlaceType.MUSIC.name -> {
                    return R.drawable.ic_group_10
                }
                PlaceType.BEACH.name -> {
                    return R.drawable.ic_group_11
                }
                PlaceType.SCHOOL.name -> {
                    return R.drawable.ic_group_12
                }
                PlaceType.TWO_BUILDINGS.name -> {
                    return R.drawable.ic_group_13
                }
                PlaceType.HOUSE_2.name -> {
                    return R.drawable.ic_group_4
                }
                PlaceType.PARK.name -> {
                    return R.drawable.ic_group_14
                }
                else -> return R.drawable.ic_group_1
            }
        }
    }
}

