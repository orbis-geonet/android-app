package com.orbis.orbis.ui.storiesModule.viewModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StoryGroup(val groupName: String, val groupPicUrl: String, val stories: ArrayList<Story>) : Parcelable