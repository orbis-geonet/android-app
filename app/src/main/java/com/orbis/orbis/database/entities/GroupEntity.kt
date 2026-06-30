package com.orbis.orbis.database.entities

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.models.group.GroupDetails

@Entity(tableName = "groups_table")
data class GroupEntity(
    @PrimaryKey(autoGenerate = false) var groupKey: String,
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "location") var location: String = "",
    @ColumnInfo(name = "description") var description: String = "",
    @ColumnInfo(name = "imageName") var imageName: String = "",
    @ColumnInfo(name = "imageLink") var imageLink: String = "",
    @ColumnInfo(name = "colorIndex") var colorIndex: String = "",
    @ColumnInfo(name = "solidColorHex") var solidColorHex: String = "",
    @ColumnInfo(name = "strokeColorHex") var strokeColorHex: String = "#FFFFFF",
    @ColumnInfo(name = "timestamp") var timestamp: String = "",
    @ColumnInfo(name = "placesCount") var placesCount: Int = 0,
    @ColumnInfo(name = "rank") var rank: Int = 0,
    @ColumnInfo(name = "rank_diff") var rankDiff: Int = 0,
    @ColumnInfo(name = "rank_diff_type") var rankDiffType: String = "",
    @ColumnInfo(name = "deleted") var deleted: String = "",
    @ColumnInfo(name = "share_link") var shareLink: String = "",
    @ColumnInfo(name = "admins_count") var adminsCount: Int = 0,
    @ColumnInfo(name = "followers_count") var followersCount: Int = 0,
    @ColumnInfo(name = "members_count") var membersCount: Int = 0,
    @ColumnInfo(name = "is_admin") var isAdmin: Boolean = false,
    @ColumnInfo(name = "is_member") var isMember: Boolean = false,
    @ColumnInfo(name = "is_follower") var isFollower: Boolean = false,
    @ColumnInfo(name = "valid_checkins") var validCheckins: Int = 0,
    @ColumnInfo(name = "percentage") var percentage: Float = 100f,
    @ColumnInfo(name = "is_main_admin") var isMainAdmin: Boolean = false,
    @ColumnInfo(name = "has_subscription") var hasSubscription: Boolean = false,
    @ColumnInfo(name = "is_subscription_active") var isSubscriptionActivate: Boolean = false,
    @ColumnInfo(name = "is_suscriber") var isSubscriber: Boolean = false,
    @ColumnInfo(name = "is_blocked_by_user") var isBlockedByUser: Boolean = false,

    @ColumnInfo(name = "from_logged_user") var fromLoggedUser: Boolean = false,
    @ColumnInfo(name = "city") var city: String
)

fun GroupEntity.toDomain(): GroupDetails{
    val coordinates: Coordinates = try {
        val type = object : TypeToken<Coordinates>() {}.type
        Gson().fromJson(this.location, type)
    } catch (e: Exception) {
        Coordinates() // Fallback to default coordinates if parsing fails
    }

    return GroupDetails(
        groupKey = this.groupKey,
        name = this.name,
        location = coordinates,
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
        isBlockedByUser = this.isBlockedByUser
    )
}