package com.orbis.orbis.repositories

import com.orbis.orbis.database.dao.FeedDao
import com.orbis.orbis.database.entities.FeedEntity
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.group.Feed
import com.orbis.orbis.models.place.LocationInfoModel
import com.orbis.orbis.models.posts.CommentModel
import com.orbis.orbis.models.posts.CreateCommentBody
import com.orbis.orbis.models.story.StoryModel
import com.orbis.orbis.network.ApiInterface
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class FeedRepositories @Inject constructor(
    private val apiInterface: ApiInterface,
    private val prefManager: PrefManager,
    private val feedDao: FeedDao
) {
    fun getNewsFeedFirst(): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getNewsFeedFirst(token, 20, 50)

    }

    fun likeComment(postKey: String, commentKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.likeComment(token, postKey, commentKey)
    }

    fun unlikeComment(postKey: String, commentKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.unlikeComment(token, postKey, commentKey)
    }

    fun deleteComment(postKey: String, commentKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.deleteComment(token, postKey, commentKey)
    }

    fun postComment(postKey: String, createCommentBody: CreateCommentBody): Single<ResponseBody> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.postComment(token, postKey, createCommentBody)

    }

    fun seenStory(postKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }

        return apiInterface.seenStory(
            token,
            postKey
        )
    }

    fun getNewsStories(page: Int, seen: Boolean): Observable<ArrayList<StoryModel>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getNewsStories(token, seen, page, 20)
    }

    fun getNearbyStories(
        page: Int,
        latitude: Double,
        longitude: Double,
        seen: Boolean
    ): Observable<ArrayList<StoryModel>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getNearbyStories(token, latitude, longitude, seen, page, 250, 50)
    }

    fun getNewsFeed(nextPage: String): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getNewsFeed(token, nextPage, 20, 50)

    }

    fun getNearbyFeedFirst(longitude: Double, latitude: Double): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getNearbyFeedFirst(token, longitude, latitude, 50, 20)
    }

    fun getCachedNearByFeed(city: String): List<FeedEntity> {
        return feedDao.getAllCachedFeedsFromNearByOnCity(city)
    }

    fun getCachedMyFeed(city: String): List<FeedEntity> {
        return feedDao.getAllCachedFeedsFromUser(city)
    }

    fun cacheFeed(feedList: FeedEntity) {
        feedDao.insertFeed(feedList)
    }

    fun modifyCachedFeed(feedList: FeedEntity) {
        feedDao.updateFeed(feedList)
    }

    fun isPageContained(page: String): Boolean {
        return feedDao.isPageContained(page)
    }

    fun deleteNearByCachedFeed(city: String) {
        return feedDao.deleteAllNearByFeeds(city)
    }

    fun deleteMyCachedFeed(city: String) {
        return feedDao.deleteAllMyFeeds(city)
    }

    fun getCommentsByPost(postKey: String, page: Int): Observable<ArrayList<CommentModel>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getCommentsByPost(token, postKey, page, 100)
    }

    fun getNearbyFeed(longitude: Double, latitude: Double, nextPage: String): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getNearbyFeed(token, longitude, latitude, nextPage, 50, 20)
    }

    fun getLocationInfo(latitude: Double, longitude: Double): Observable<LocationInfoModel> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getLocationInfo(token, latitude, longitude)
    }

}