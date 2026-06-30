package com.orbis.orbis.models.group

import android.net.Uri
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.orbis.orbis.database.entities.FeedEntity
import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.posts.CommentModel
import com.orbis.orbis.models.posts.RichLinkData
import com.orbis.orbis.models.story.StoryModel
import com.orbis.orbis.models.user.User
import kotlinx.parcelize.Parcelize

data class Feed(
    @SerializedName("nextPage") val nextPage: String,
    @SerializedName("content") val content: ArrayList<FeedContent>
)

data class FeedContent(
    @SerializedName("type") var type: String = "",
    @SerializedName("post") var post: FeedPost? = null,
    @SerializedName("slider") var slider: ArrayList<FeedPost> = ArrayList(),
    var group: GroupDetails? = null,
    var place: PlaceDetails? = null,
    var placeUploadPhoto: Uri? = null,
    var showNoItem: Boolean? = null,
    var loaded: Boolean = false,
    var stories: ArrayList<StoryModel> = ArrayList(),
    var comments: ArrayList<CommentModel> = ArrayList(),
    var noItemTitle: String = ""
)

@Parcelize
data class FeedPost(
    @SerializedName("coordinates") val coordinates: Coordinates? = null,
    @SerializedName("postKey") val postKey: String = "",
    @SerializedName("checkInPolygonCoordinateKey") val checkInPolygonCoordinateKey: String = "",
    @SerializedName("timestamp") val timestamp: String = "",
    @SerializedName("type") var type: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("details") val details: String = "",
    @SerializedName("address") val address: String = "",
    @SerializedName("richLinkData") var richLinkData: RichLinkData? = null,
    @SerializedName("group") var group: GroupDetails? = null,
    @SerializedName("signedUrls") val signedUrls: ArrayList<String> = ArrayList(),
    @SerializedName("mediaUrls") val mediaUrls: ArrayList<String> = ArrayList(),
    var postVideo: String = "",
    var postAudio: String = "",
    @SerializedName("place") var place: PlaceDetails? = null,
    @SerializedName("shareLink") val shareLink: String = "",
    @SerializedName("plannedTime") val plannedTime: String = "",
    @SerializedName("plannedEndTime") val plannedEndTime: String = "",
    @SerializedName("attending") var attending: Boolean = false,
    @SerializedName("user") var user: UserInfo? = null,
    @SerializedName("commentsCount") var commentsCount: Int = 0,
    @SerializedName("likesCount") var likesCount: Int = 0,
    @SerializedName("confirmedCount") val confirmedCount: Int = 0,
    @SerializedName("userLiked") var userLiked: Boolean = false,
    @SerializedName("seen") var seen: Boolean = false,
    var attendedUsers: ArrayList<User> = ArrayList()
) : Parcelable