package com.orbis.orbis.database.entities

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.orbis.orbis.models.group.FeedContent
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.posts.CommentModel
import com.orbis.orbis.models.story.StoryModel
import java.util.Date

@Entity(tableName = "feed_table")
data class FeedEntity(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "next_page") var nextPage: String,
    @ColumnInfo(name = "from_nearby") var fromNearby: Boolean = true,
    @ColumnInfo(name = "city") var city: String = "",
    @ColumnInfo(name = "content") var content: String = ""
)