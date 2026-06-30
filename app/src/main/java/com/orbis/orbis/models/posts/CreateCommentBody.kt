package com.orbis.orbis.models.posts

import com.google.gson.annotations.SerializedName

data class CreateCommentBody(
    @SerializedName("text") var text: String,
    @SerializedName("replyToKey") var replyToKey: String? = null,
)
