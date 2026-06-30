package com.orbis.orbis.database.entities

import android.net.Uri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.posts.CommentModel
import com.orbis.orbis.models.story.StoryModel

class Converters {

    // Converter for FeedPost
    @TypeConverter
    fun fromFeedPost(value: FeedPost): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toFeedPost(value: String): FeedPost {
        return Gson().fromJson(value, FeedPost::class.java)
    }

    // Converter for ArrayList<FeedPost>
    @TypeConverter
    fun fromFeedPostList(value: ArrayList<FeedPost>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toFeedPostList(value: String): ArrayList<FeedPost> {
        val listType = object : TypeToken<ArrayList<FeedPost>>() {}.type
        return Gson().fromJson(value, listType)
    }

    // Converter for Uri
    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun toUri(uriString: String?): Uri? {
        return if (uriString == null) null else Uri.parse(uriString)
    }

    // Converter for GroupDetails
    @TypeConverter
    fun fromGroupDetails(value: GroupDetails?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toGroupDetails(value: String?): GroupDetails? {
        return Gson().fromJson(value, GroupDetails::class.java)
    }

    // Converter for PlaceDetails
    @TypeConverter
    fun fromPlaceDetails(value: PlaceDetails?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toPlaceDetails(value: String?): PlaceDetails? {
        return Gson().fromJson(value, PlaceDetails::class.java)
    }

    // Converter for ArrayList<StoryModel>
    @TypeConverter
    fun fromStoryModelList(value: ArrayList<StoryModel>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStoryModelList(value: String): ArrayList<StoryModel> {
        val listType = object : TypeToken<ArrayList<StoryModel>>() {}.type
        return Gson().fromJson(value, listType)
    }

    // Converter for ArrayList<CommentModel>
    @TypeConverter
    fun fromCommentModelList(value: ArrayList<CommentModel>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toCommentModelList(value: String): ArrayList<CommentModel> {
        val listType = object : TypeToken<ArrayList<CommentModel>>() {}.type
        return Gson().fromJson(value, listType)
    }
}