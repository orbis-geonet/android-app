package com.orbis.orbis.ui.commentModule.viewModel

import java.util.*

class Comment(
    var name: String,
    var image: Int,
    var text: String,
    var time: String,
    var like_count: Int,
    var replies: ArrayList<Reply>
)
