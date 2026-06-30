package com.orbis.orbis.models.posts

import android.os.Parcelable
import com.google.firebase.auth.UserInfo
import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.auth.UserProfile
import com.orbis.orbis.models.group.FeedPost
import kotlinx.parcelize.Parcelize


data class CommentModel(
    @SerializedName("commentKey") val commentKey: String,
    @SerializedName("text") val text: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("createTimestamp") val createTimestamp: String,
    @SerializedName("deleted") val deleted: Boolean,
    @SerializedName("userLiked") var userLiked: Boolean,
    @SerializedName("likesCount") var likesCount: Int,
    @SerializedName("post") val post: FeedPost,
    @SerializedName("replies") val replies: ArrayList<ReplyModel>,
    @SerializedName("user") val user: UserProfile,
    var selectedForReply: Boolean = false
)


data class ReplyModel(
    @SerializedName("commentKey") val commentKey: String,
    @SerializedName("replyToKey") val replyToKey: String,
    @SerializedName("user") val user: UserProfile,
    @SerializedName("text") val text: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("createTimestamp") val createTimestamp: String,
    @SerializedName("deleted") val deleted: Boolean,
    @SerializedName("likesCount") var likesCount: Int,
    @SerializedName("userLiked") var userLiked: Boolean,

    )
