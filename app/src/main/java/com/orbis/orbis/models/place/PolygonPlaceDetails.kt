package com.orbis.orbis.models.place

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Parcelable
import androidx.core.graphics.toColorInt
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.orbis.orbis.helpers.CoordinatesUtil
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.utils.Utils
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class PolygonPlaceDetails(
    @Expose @SerializedName("coordinates") val coordinates: Coordinates? = null,
    @Expose @SerializedName("name") val name: String = "",
    @Expose @SerializedName("placeKey") val placeKey: String = "",
    @Expose @SerializedName("palindromeKey") val palindromeKey: String = "",
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
    @Expose @SerializedName("places") val places: ArrayList<PolygonPlaceDetails> = ArrayList(),
    @Expose @SerializedName("polygonPoints") val polygonPoints: ArrayList<Coordinates> = ArrayList(),
    @Expose @SerializedName("polygonCenter") val polygonCenter: Coordinates? = null,
    @Transient var touch: Boolean = false,
    @Transient var sizeCalculated: Boolean = false,
    @Transient var isFocusSelected: Boolean = false,
) : Parcelable {
    @IgnoredOnParcel
    @Transient
    var animator: ValueAnimator? = null
}

fun PolygonPlaceDetails.toPlaceDetails(): PlaceDetails {
    return PlaceDetails(
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
        touch = this.touch
    )
}
fun PolygonPlaceDetails.getPlaceColor(): Int {
    return this.dominantGroup?.strokeColorHex?.toColorInt() ?: Color.CYAN
}

fun PolygonPlaceDetails.getNormalColor(): Int {
    val placeColor = this.dominantGroup?.strokeColorHex?.toColorInt() ?: Color.CYAN
    val alpha = Color.alpha(placeColor)
    return Color.argb(
        //normal color has 35% transparency and is the color to display on normal case of polygon
        (alpha * 0.35).toInt(),
        Color.red(placeColor),
        Color.green(placeColor),
        Color.blue(placeColor)
    )
}

fun PolygonPlaceDetails.getTransparentColor(): Int {
    val placeColor = this.dominantGroup?.strokeColorHex?.toColorInt() ?: Color.CYAN
    val alpha = Color.alpha(placeColor)
    return Color.argb(
        //color when other polygon is clicked
        (alpha * 0.05).toInt(), //5% transparency
        Color.red(placeColor),
        Color.green(placeColor),
        Color.blue(placeColor)
    )
}
fun PolygonPlaceDetails.isCircle(): Boolean {
    //Circles have exactly one place inside
    return this.places.size == 1
}

fun PolygonPlaceDetails.computeSize() {
    if (this.sizeCalculated)
        return

    if (this.isCircle()){
        this.size = CoordinatesUtil.calculateCircleRadius(this.polygonPoints)
    }else{
        val centerLatLng = LatLng(
            this.polygonCenter!!.latitude,
            this.polygonCenter.longitude
        )
        val polygonLatLng = CoordinatesUtil.coordinatesToLatLng(this.polygonPoints)
        this.size = CoordinatesUtil.calculateShortestDistance(polygonLatLng,centerLatLng)
    }
    this.sizeCalculated = true
}

fun PolygonPlaceDetails.cancelAnimation() {
    this.animator?.end()
    this.animator?.cancel()
    //this.animator = null
}
fun PolygonPlaceDetails.getCenterLatLng(): LatLng =
    LatLng(polygonCenter!!.latitude, polygonCenter.longitude)