package com.orbis.orbis.models.story

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import kotlinx.parcelize.Parcelize

@Parcelize
data class StoryModel(
    @SerializedName("storyKey") val storyKey: String,
    @SerializedName("group") val group: GroupDetails,
    @SerializedName("posts") val posts: ArrayList<FeedPost>
) : Parcelable
