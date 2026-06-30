package com.orbis.orbis.models.message

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import com.orbis.orbis.models.auth.UserInfo
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class ConversationModel(
    var lastMessage: MessageModel? = null,
    var participants: ArrayList<String> = ArrayList(),
    var timestamp: Long = 0,
    var sender: UserInfo? = null
) : Parcelable
