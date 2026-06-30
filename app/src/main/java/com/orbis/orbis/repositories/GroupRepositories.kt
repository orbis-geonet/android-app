package com.orbis.orbis.repositories

import com.orbis.orbis.database.dao.GroupDao
import com.orbis.orbis.database.entities.toDomain
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.CreateGroupBody
import com.orbis.orbis.models.group.Feed
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.group.toEntity
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.user.User
import com.orbis.orbis.network.ApiInterface
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class GroupRepositories @Inject constructor(
    private val apiInterface: ApiInterface,
    private val prefManager: PrefManager,
    private val groupDao: GroupDao
) {

    fun getEvents(
        placeKey: String,
        page: Int
    ): Observable<ArrayList<FeedPost>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getEventByGroup(token, placeKey, false, page, 25)
    }

    fun createGroup(createGroupBody: CreateGroupBody): Single<GroupDetails> {
        val token = prefManager.getIdToken()
        return apiInterface.createGroup("Bearer $token", createGroupBody)
    }

    fun updateGroup(createGroupBody: CreateGroupBody, groupKey: String): Single<GroupDetails> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.updateGroup(token, groupKey, createGroupBody)
    }

    fun getRecommendedGroup(
        latitude: Double,
        longitude: Double
    ): Observable<ArrayList<GroupDetails>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        val location = Constants.location?.longitude.toString() + ";" + Constants.location?.latitude
        return apiInterface.getRecommendedGroup(token, location, latitude, longitude, 5)
    }

    fun getGroups(
        latitude: Double,
        longitude: Double,
        name: String,
        page: Int = 0
    ): Observable<ArrayList<GroupDetails>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getGroups(token, latitude, longitude, name, 100, page)
    }

    fun getGroupsWithoutLocation(
        page: Int = 0
    ): Observable<ArrayList<GroupDetails>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getGroupsWithoutLocation(token, 50, page)
    }

    fun getUserGroups(
        userKey: String,
        page: Int = 0,
    ): Observable<ArrayList<GroupDetails>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getUserGroups(token, userKey, 25, page)
    }

    fun getRatedGroups(
        latitude: Double,
        longitude: Double,
        page: Int = 0
    ): Observable<ArrayList<GroupDetails>> {
        return apiInterface.getRatedGroups(latitude, longitude, 1, 25, page, 50)
    }

    fun getGroupFeed(
        groupKey: String,
        nextPage: String
    ): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getGroupFeed(token, groupKey, nextPage, 20)
    }

    fun getGroupFeedFirst(
        groupKey: String
    ): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getGroupFeedFirst(token, groupKey, 20)
    }

    fun getGroupByKey(groupKey: String): Observable<GroupDetails> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getGroupByKey(token, groupKey)
    }

    fun likePost(postKey: String): Single<FeedPost> {
        val token = "Bearer " + prefManager.getIdToken()
        return apiInterface.likePost(token, postKey)
    }

    fun unlikePost(postKey: String): Single<FeedPost> {
        val token = "Bearer " + prefManager.getIdToken()
        return apiInterface.unlikePost(token, postKey)
    }

    fun getGroupMembers(groupKey: String, page: Int): Observable<ArrayList<User>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getGroupMembers(token, groupKey, page, 100)
    }

    fun getGroupAdmins(groupKey: String): Observable<ArrayList<User>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getGroupAdmins(token, groupKey, 100, 0)
    }

    fun addBan(groupKey: String, userKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.addBan(token, groupKey, userKey)
    }

    fun blockGroup(groupKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.blockGroup(token, groupKey)
    }

    fun unBlockGroup(groupKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.unBlockGroup(token, groupKey)
    }

    fun addAdmin(groupKey: String, userKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.addAdmin(token, groupKey, userKey)
    }

    fun adminRemoveAdmin(groupKey: String, userKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.adminRemoveAdmin(token, groupKey, userKey)
    }

    fun attendEvent(postKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.attendEvent(token, postKey)
    }

    fun unattendEvent(postKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.unattendEvent(token, postKey)
    }

    fun getPlaceOwnedByGroup(
        groupKey: String,
        latitude: Double,
        longitude: Double,
        page: Int
    ): Observable<ArrayList<PlaceDetails>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        val location = longitude.toString() + ";" + latitude
        return apiInterface.getPlaceOwnedByGroup(token, location, groupKey, page, 50)
    }

    fun joinGroup(groupKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.joinGroup(token, groupKey)
    }

    fun leaveGroup(groupKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.leaveGroup(token, groupKey)
    }

    fun followGroup(groupKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.followGroup(token, groupKey)
    }

    fun unfollowGroup(groupKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.unfollowGroup(token, groupKey)
    }

    fun deleteGroup(groupKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.deleteGroup(token, groupKey)
    }

    fun sharePost(postKey: String): Single<ResponseBody> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.sharePost(token, postKey)
    }

    fun deletePost(postKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.deletePost(token, postKey)
    }

    fun getPost(postKey: String): Single<FeedPost> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getPost(token, postKey)
    }

    fun reportPost(postKey: String, text: String): Single<ResponseBody> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.reportPost(
            token,
            postKey,
            text.toRequestBody("text/plain".toMediaTypeOrNull())
        )
    }

    fun reportGroup(groupKey: String, text: String): Single<ResponseBody> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.reportGroup(
            token,
            groupKey,
            text.toRequestBody("text/plain".toMediaTypeOrNull())
        )
    }

    fun getCachedGroups(city: String, loggedIn: Boolean): List<GroupDetails> {
        return groupDao.getCachedGroups(city, loggedIn).map { it.toDomain() }
    }

    fun cacheGroups(groups: List<GroupDetails>, city: String, loggedIn: Boolean) {
        groupDao.insertGroups(groups.map { it.toEntity(city, loggedIn) })
    }

    fun deleteGroupsFromCache(city: String, loggedIn: Boolean) {
        groupDao.deleteCachedGroups(city, loggedIn)
    }

    fun modifyGroup(group : GroupDetails, city: String, loggedIn: Boolean) {
        groupDao.updateGroup(group.toEntity(city, loggedIn))
    }

    fun modifyGroups(groups : List<GroupDetails>, city: String, loggedIn: Boolean) {
        groupDao.updateGroups(groups.map { it.toEntity(city, loggedIn) })
    }

}