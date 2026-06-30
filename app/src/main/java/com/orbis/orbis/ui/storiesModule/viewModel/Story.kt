package com.orbis.orbis.ui.storiesModule.viewModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Story(
    val url: String,
    val storyDate: Long,
    val storyLocation: String,
    val storyUsername: String,
    val userPicUrl: String,
    val userPostText: String
) : Parcelable {

    fun isVideo() =  url.contains(".mp4")
}