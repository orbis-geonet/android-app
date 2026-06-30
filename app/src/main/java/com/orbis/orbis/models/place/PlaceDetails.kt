package com.orbis.orbis.models.place

import android.animation.ValueAnimator
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.orbis.orbis.helpers.CoordinatesUtil
import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.models.group.GroupDetails
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class PlaceDetails(
    @Expose @SerializedName("coordinates") val coordinates: Coordinates? = null,
    @Expose @SerializedName("name") val name: String = "",
    @Expose @SerializedName("placeKey") val placeKey: String = "",
    @Expose @SerializedName("checkInPolygonCoordinateKey") val checkInPolygonCoordinateKey: String = "",
    @Expose @SerializedName("type") val type: String = "",
    @Expose @SerializedName("source") val source: String = "",
    @Expose @SerializedName("address") val address: String = "",
    @Expose @SerializedName("description") var description: String = "",
    @Expose @SerializedName("imageName") var imageName: String = "",
    @Expose @SerializedName("lastCheckInTimestamp") val lastCheckInTimestamp: String = "",
    @Expose @SerializedName("dominantGroupKey") val dominantGroupKey: String = "",
    @Expose @SerializedName("size") var size: Double = 0.0,
    @Expose @SerializedName("previousSize") var previousSize: Double = 0.0,
    @Expose @SerializedName("dominantGroup") val dominantGroup: GroupDetails? = null,
    @Expose @SerializedName("competingGroups") val competingGroups: ArrayList<GroupDetails> = ArrayList(),
    @Expose @SerializedName("following") var following: Boolean = false,
    @Expose @SerializedName("website") var website: String = "",
    @Expose @SerializedName("phone") var phone: String = "",
    @Expose @SerializedName("averageRate") var averageRate: Float = 0.0F,
    @Expose @SerializedName("totalRate") var totalRate: Float = 0.0F,
    @Expose @SerializedName("countRates") var countRates: Int = 0,
    @Expose @SerializedName("userRate") var userRate: Int = 0,
    @Expose @SerializedName("canEdit") var canEdit: Boolean = false,
    @Expose @SerializedName("workingHours") val workingHours: ArrayList<WorkingHoursModel> = ArrayList(),
    @Transient var touch: Boolean = false,
) : Parcelable {
    @IgnoredOnParcel
    @Transient
    var animator: ValueAnimator? = null
}

@Parcelize
class WorkingHoursModel(
    @Expose @SerializedName("day") val day: String = "",
    @Expose @SerializedName("time") val time: String = ""
) : Parcelable

@Parcelize
class RatePlaceModel(
    @Expose @SerializedName("placeKey") val placeKey: String = "",
    @Expose @SerializedName("averageRate") val averageRate: Float = 0.0F,
    @Expose @SerializedName("totalRate") val totalRate: Float = 0.0F,
    @Expose @SerializedName("countRates") val countRates: Int = 0
) : Parcelable

fun PlaceDetails.toPolygonPlaceDetails(): PolygonPlaceDetails {
    return PolygonPlaceDetails(
        coordinates = this.coordinates,
        name = this.name,
        placeKey = this.placeKey,
        type = this.type,
        source = this.source,
        address = this.address,
        description = this.description,
        imageName = this.imageName,
        lastCheckInTimestamp = this.lastCheckInTimestamp,
        dominantGroupKey = this.dominantGroupKey,
        size = this.size,
        previousSize = this.previousSize,
        dominantGroup = this.dominantGroup,
        competingGroups = this.competingGroups,
        following = this.following,
        website = this.website,
        phone = this.phone,
        averageRate = this.averageRate,
        totalRate = this.totalRate,
        countRates = this.countRates,
        userRate = this.userRate,
        canEdit = this.canEdit,
        workingHours = this.workingHours,
        polygonCenter = this.coordinates,
        polygonPoints = CoordinatesUtil.getCircleOuterPoints(
            this.coordinates ?: Coordinates(),
            this.size
        ),
        places = arrayListOf(
            PolygonPlaceDetails(
                name = name,
                description = description,
                coordinates = coordinates,
                imageName = imageName,
                countRates = countRates,
                totalRate = totalRate,
                averageRate = averageRate,
                type = type,
                size = size,
                placeKey = placeKey,
                isFocusSelected = true,
                dominantGroup = dominantGroup
            )
        ),
        touch = this.touch,
        isFocusSelected = true
    )
}

