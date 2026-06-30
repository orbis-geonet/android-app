package com.orbis.orbis.repositories

import android.util.Log
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.IGConnectModel
import com.orbis.orbis.models.auth.PasswordUpdate
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.group.Feed
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.notifications.NotificationCount
import com.orbis.orbis.models.notifications.NotificationModel
import com.orbis.orbis.models.profile.UserPictures
import com.orbis.orbis.models.user.FollowingModel
import com.orbis.orbis.models.user.FollowingPlaceModel
import com.orbis.orbis.models.user.UserPicturesBody
import com.orbis.orbis.network.ApiInterface
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class ProfileRepositories @Inject constructor(
    private val apiInterface: ApiInterface,
    private val prefManager: PrefManager
) {

    fun getMyProfile(): Observable<UserInfo> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }

        return apiInterface.getMyProfile(token)

    }

    fun updateFcmToken(fcmToken: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }

        return apiInterface.updateFcmToken(
            token,
            fcmToken.toRequestBody("text/plain".toMediaTypeOrNull())
        )
    }

    fun deleteFcmToken(fcmToken: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }

        return apiInterface.deleteFcmToken(
            token,
            fcmToken.toRequestBody("text/plain".toMediaTypeOrNull())
        )
    }

    fun getUserProfile(userKey: String): Observable<UserInfo> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getUserProfile(token, userKey)

    }

    fun searchUser(name: String, page: Int): Observable<ArrayList<UserInfo>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.searchUser(token, name, page, 50)
    }

    fun deleteUserPicture(pictureKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.deletePhoto(token, pictureKey)
    }

    fun updateProfile(userInfo: UserInfo): Observable<UserInfo> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.updateMyProfile(token, userInfo)
    }

    fun getMyPictures(page: Int): Observable<ArrayList<UserPictures>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getMyPictures(token, page, 50)
    }

    fun uploadUserPictures(pictures: ArrayList<String>): Single<UserPictures> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        val body = UserPicturesBody(pictures)
        return apiInterface.uploadProfilePicture(token, body)
    }

    fun getMyIgPictures(page: Int): Observable<ArrayList<UserPictures>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getMyIgPictures(token, page, 50)
    }

    fun getUserIgPictures(page: Int, userKey: String): Observable<ArrayList<UserPictures>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getUserIgPictures(token, userKey, page, 50)
    }

    fun getUserPictures(userKey: String, page: Int): Observable<ArrayList<UserPictures>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getUserPictures(token, userKey, page, 50)
    }

    fun getMyFeedFirst(): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getMyFeedFirst(token, 20)
    }

    fun getMyFeed(nextPage: String): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getMyFeed(token, nextPage, 20)
    }

    fun getUserFeedFirst(userKey: String): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getUserFeedFirst(token, userKey, 20)
    }

    fun getUserFeed(userKey: String, nextPage: String): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getUserFeed(token, userKey, nextPage, 20)
    }


    fun followUser(userKey: String): Single<UserInfo> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.followUser(token, userKey)
    }

    fun unfollowUser(userKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.unfollowUser(token, userKey)
    }

    fun igConnect(): Observable<IGConnectModel> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.igConnect(token)
    }

    fun getMyFollowers(page: Int): Observable<ArrayList<UserInfo>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getMyFollowers(token, page, 100)
    }

    fun getUserFollowers(userKey: String, page: Int): Observable<ArrayList<UserInfo>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getUserFollowers(token, userKey, page, 100)
    }

    fun getMyFollowing(page: Int): Observable<ArrayList<FollowingModel>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getMyFollowing(token, page, 100)
    }

    fun getUserFollowing(userKey: String, page: Int): Observable<ArrayList<FollowingModel>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getUserFollowing(token, userKey, page, 100)
    }

    fun getMyAdminGroups(page: Int): Observable<ArrayList<GroupDetails>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getMyAdminGroups(token, page, 100)
    }

    fun getMyFollowingPlace(page: Int): Observable<ArrayList<FollowingPlaceModel>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getMyFollowingPlace(token, page, 100)
    }

    fun reportUser(userKey: String, text: String): Single<ResponseBody> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.reportUser(
            token,
            userKey,
            text.toRequestBody("text/plain".toMediaTypeOrNull())
        )
    }

    fun reportPlace(placeKey: String, text: String): Single<ResponseBody> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.reportPlace(
            token,
            placeKey,
            text.toRequestBody("text/plain".toMediaTypeOrNull())
        )
    }

    fun blockUser(userKey: String): Single<ResponseBody> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.blockUser(
            token,
            userKey
        )
    }

    fun deleteMyProfile(): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.deleteMyProfile(token)

    }

    fun updatePassword(passwordUpdate: PasswordUpdate): Single<ResponseBody> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.updatePassword(token, passwordUpdate)
    }

    fun getNotifications(page: Int): Observable<ArrayList<NotificationModel>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getNotifications(token, page, 100)
    }

    fun getPendingFollowers(page: Int): Observable<ArrayList<UserInfo>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getPendingFollowers(token, page, 100)
    }

    fun getUnreadCount(): Observable<NotificationCount> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getUnreadCount(token)
    }

    fun seenPost(keys: ArrayList<String>): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.seenPost(token, keys)
    }

    fun acceptFollow(userKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.acceptFollow(token, userKey)
    }
}