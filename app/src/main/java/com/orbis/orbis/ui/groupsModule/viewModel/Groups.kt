package com.orbis.orbis.ui.groupsModule.viewModel

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.utils.ViewUtils.Companion.convertTimeStampToDate
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
class Groups(

    @SerializedName("groupKey")
    @Expose
    var groupKey: String? = null,

    @SerializedName(" placeKey")
    @Expose
    var placeKey: String? = null,

    @SerializedName("placeCoordinates")
    @Expose
    var placeCoordinates: Coordinates? = null,

    @SerializedName("name")
    @Expose
    var name: String? = null,

    @SerializedName("location")
    @Expose
    var location: Coordinates? = null,

    @SerializedName("description")
    @Expose
    var description: String? = null,

    @SerializedName("imageName")
    @Expose
    var image: String? = null,

    @SerializedName("colorIndex")
    @Expose
    var colorIndex: Int? = 0,

    @SerializedName("solidColorHex")
    @Expose
    var solidColorHex: String? = null,

    @SerializedName("strokeColorHex")
    @Expose
    var strokeColorHex: String? = null,

    @SerializedName("os")
    @Expose
    var os: String? = null,

    @SerializedName("placesCount")
    @Expose
    var placesCount: Int? = 0,

    @SerializedName("adminsCount")
    @Expose
    var adminsCount: Int? = 0,

    @SerializedName("followersCount")
    @Expose
    var followersCount: Int? = 0,

    @SerializedName("membersCount")
    @Expose
    var membersCount: Int? = 0,

    @SerializedName("isAdmin")
    @Expose
    var isAdmin: Boolean? = null,

    @SerializedName("isMember")
    @Expose
    var isMember: Boolean? = null,

    @SerializedName("isFollower")
    @Expose
    var isFollower: Boolean? = null,

    @SerializedName("deleted")
    @Expose
    var deleted: Boolean? = null,

//    @SerializedName("admins")
//    @Expose
//    var admins: ArrayList<BasicProfile>? = null,
//
//    @SerializedName("followers")
//    @Expose
//    var followers: ArrayList<BasicProfile>? = null,
//
//    @SerializedName("members")
//    @Expose
//    var members: ArrayList<BasicProfile>? = null,

    @SerializedName("validCheckins")
    @Expose
    var validCheckins: Int? = 0,

    @SerializedName("timestamp")
    @Expose
    var timestamp: String? = null,

    @SerializedName("rank")
    @Expose
    var rank: Int? = 0,

    @SerializedName("rankDiff")
    @Expose
    var rankDiff: Int? = 0,

    @SerializedName("percentage")
    @Expose
    var percentage: String? = null
) : Parcelable, Comparable<Groups> {


    constructor(description: String?, name: String?, image: String?) : this() {
        this.name = name
        this.description = description
        this.image = image
    }

    constructor(name: String?, description: String?, image: String?, colorIndex: Int?) : this() {
        this.name = name
        this.description = description
        this.image = image
        this.colorIndex = colorIndex
    }

    constructor(
        name: String?,
        location: Coordinates?,
        description: String?,
        colorIndex: Int?,
        strokeColorHex: String?
    ) : this() {
        this.name = name
        this.location = location
        this.description = description
        this.colorIndex = colorIndex
        this.strokeColorHex = strokeColorHex
    }

    override fun compareTo(other: Groups): Int {
        try {
            val data1 = convertTimeStampToDate(this.timestamp)
            val data2 = convertTimeStampToDate(other.timestamp)

            if (data1 == null || data2 == null)
                return 1

            return when {
                data1.before(data2) -> 1
                data1 == data2 -> 0
                else -> -1
            }
        } catch (e: Exception) {
            return 1
        }

    }

    fun compareToByPercentage(other: Groups): Int {
        try {
            val data1 = this.percentage?.toFloat()
            val data2 = other.percentage?.toFloat()

            if (data1 == null || data2 == null)
                return 1

            return when {
                data1 < data2 -> 1
                data1 == data2 -> 0
                else -> -1
            }
        } catch (e: Exception) {
            return 1
        }

    }

    companion object {
        @JvmStatic
        fun sortGroupsListByTimeStamp(myList: MutableList<Groups>): MutableList<Groups> {
            myList.sortWith { o1, o2 -> o1.compareTo(o2) }
            return myList
        }

        @JvmStatic
        fun sortGroupsListByPercentage(myList: MutableList<Groups>): MutableList<Groups> {
            myList.sortWith { o1, o2 -> o1.compareToByPercentage(o2) }
            return myList
        }
    }

}