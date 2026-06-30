package com.orbis.orbis.models.group

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.orbis.orbis.database.entities.GroupEntity
import com.orbis.orbis.models.Coordinates
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupDetails(
    @SerializedName("groupKey") val groupKey: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("location") val location: Coordinates? = null,
    @SerializedName("description") val description: String = "",
    @SerializedName("imageName") var imageName: String = "",
    @SerializedName("imageLink") var imageLink: String = "",
    @SerializedName("colorIndex") val colorIndex: String = "",
    @SerializedName("solidColorHex") val solidColorHex: String = "",
    @SerializedName("strokeColorHex") val strokeColorHex: String = "#FFFFFF",
    @SerializedName("timestamp") val timestamp: String = "",
    @SerializedName("placesCount") val placesCount: Int = 0,
    @SerializedName("rank") val rank: Int = 0,
    @SerializedName("rankDiff") val rankDiff: Int = 0,
    @SerializedName("rankDiffType") val rankDiffType: String = "",
    @SerializedName("deleted") val deleted: String = "",
    @SerializedName("shareLink") val shareLink: String = "",
    @SerializedName("adminsCount") val adminsCount: Int = 0,
    @SerializedName("followersCount") val followersCount: Int = 0,
    @SerializedName("membersCount") var membersCount: Int = 0,
    @SerializedName("isAdmin") val isAdmin: Boolean = false,
    @SerializedName("isMember") var isMember: Boolean = false,
    @SerializedName("isFollower") var isFollower: Boolean = false,
    @SerializedName("validCheckins") val validCheckins: Int = 0,
    @SerializedName("percentage") val percentage: Float = 100f,
    @SerializedName("isMainAdmin") var isMainAdmin: Boolean = false,
    @SerializedName("hasSubscription") var hasSubscription: Boolean = false,
    @SerializedName("isSubscriptionActivate") var isSubscriptionActivate: Boolean = false,
    @SerializedName("isSubscriber") var isSubscriber: Boolean = false,
    @SerializedName("isBlockedByUser") var isBlockedByUser: Boolean = false,
) : Parcelable


fun GroupDetails.toEntity(fromCity: String, fromLoggedInUser: Boolean): GroupEntity{
    return GroupEntity(
        groupKey = this.groupKey,
        name = this.name,
        location = Gson().toJson(this.location),
        description = this.description,
        imageName = this.imageName,
        imageLink = this.imageLink,
        colorIndex = this.colorIndex,
        solidColorHex = this.solidColorHex,
        strokeColorHex = this.strokeColorHex,
        timestamp = this.timestamp,
        placesCount = this.placesCount,
        rank = this.rank,
        rankDiff = this.rankDiff,
        rankDiffType = this.rankDiffType,
        deleted = this.deleted,
        shareLink = this.shareLink,
        adminsCount = this.adminsCount,
        followersCount = this.followersCount,
        membersCount = this.membersCount,
        isAdmin = this.isAdmin,
        isMember = this.isMember,
        isFollower = this.isFollower,
        validCheckins = this.validCheckins,
        percentage = this.percentage,
        isMainAdmin = this.isMainAdmin,
        hasSubscription = this.hasSubscription,
        isSubscriptionActivate = this.isSubscriptionActivate,
        isSubscriber = this.isSubscriber,
        isBlockedByUser = this.isBlockedByUser,
        fromLoggedUser = fromLoggedInUser,
        city = fromCity
    )
}