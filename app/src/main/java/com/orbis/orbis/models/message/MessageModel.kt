package com.orbis.orbis.models.message

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageModel(
    var conversationId: String = "",
    var isRead: Boolean = false,
    var message: String = "",
    var mediaUrl: String = "",
    var senderId: String = "",
    var timestamp: Long = 0,
    var type: String = "",
    var videoUrl: String = "",

    ) : Parcelable
