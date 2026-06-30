package com.orbis.orbis.network

import com.orbis.orbis.models.IGConnectModel
import com.orbis.orbis.models.auth.*
import com.orbis.orbis.models.group.CreateGroupBody
import com.orbis.orbis.models.group.Feed
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.notifications.NotificationCount
import com.orbis.orbis.models.notifications.NotificationModel
import com.orbis.orbis.models.place.*
import com.orbis.orbis.models.posts.CommentModel
import com.orbis.orbis.models.posts.CreateCommentBody
import com.orbis.orbis.models.posts.PostBody
import com.orbis.orbis.models.profile.UserPictures
import com.orbis.orbis.models.story.StoryModel
import com.orbis.orbis.models.subscriptions.*
import com.orbis.orbis.models.user.FollowingModel
import com.orbis.orbis.models.user.FollowingPlaceModel
import com.orbis.orbis.models.user.User
import com.orbis.orbis.models.user.UserPicturesBody
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiInterface {

    //Auth Apis
    @POST("/login")
    fun login(@Body loginBody: LoginBody): Single<UserProfile>

    @POST("/signup")
    fun signup(@Body signUpBody: SignUpBody): Single<UserProfile>

    @POST("/profile/me")
    fun updateProfile(
        @Header("Authorization") token: String,
        @Body profileUpdateBody: ProfileUpdateBody
    ): Single<UserProfile>

    @GET("profile/me")
    fun getMySocialProfile(
        @Header("Authorization") token: String
    ): Single<UserProfile>

    @POST("/refresh")
    fun refreshToken(@Body requestBody: RequestBody): Single<ResponseBody>

    @PUT("profile/me/fcmToken")
    fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body requestBody: RequestBody
    ): Single<Response<Void>>

    @HTTP(method = "DELETE", path = "profile/me/fcmToken", hasBody = true)
    fun deleteFcmToken(
        @Header("Authorization") token: String,
        @Body requestBody: RequestBody
    ): Single<Response<Void>>
    //Group Apis

    @POST("/groups")
    fun createGroup(
        @Header("Authorization") token: String,
        @Body createGroupBody: CreateGroupBody
    ): Single<GroupDetails>

    @PUT("/groups/{groupKey}")
    fun updateGroup(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Body createGroupBody: CreateGroupBody
    ): Single<GroupDetails>

    @GET("/groups/{groupKey}")
    fun getGroupByKey(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String
    ): Observable<GroupDetails>

    @GET("/groups/{groupKey}/members")
    fun getGroupMembers(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<User>>

    //Recommended Group
    @GET("groups/recommended")
    fun getRecommendedGroup(
        @Header("Authorization") token: String,
        @Header("Orbis-Coordinates") coordinates: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("size") size: Int
    ): Observable<ArrayList<GroupDetails>>

    //Get Groups
    @GET("groups")
    fun getGroups(
        @Header("Authorization") token: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("name") name: String,
        @Query("size") size: Int,
        @Query("page") page: Int
    ): Observable<ArrayList<GroupDetails>>

    @GET("groups")
    fun getGroupsWithoutLocation(
        @Header("Authorization") token: String,
        @Query("size") size: Int,
        @Query("page") page: Int
    ): Observable<ArrayList<GroupDetails>>

    @GET("groups/{groupKey}/admins")
    fun getGroupAdmins(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Query("size") size: Int,
        @Query("page") page: Int
    ): Observable<ArrayList<User>>

    @PUT("groups/{groupKey}/banned/{userKey}")
    fun addBan(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Path("userKey") userKey: String,

        ): Single<Response<Void>>

    @PUT("groups/{groupKey}/block")
    fun blockGroup(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String
    ): Single<Response<Void>>

    @DELETE("groups/{groupKey}/unblock")
    fun unBlockGroup(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String
    ): Single<Response<Void>>


    @PUT("groups/{groupKey}/admins/{userKey}")
    fun addAdmin(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Path("userKey") userKey: String,
    ): Single<Response<Void>>

    @DELETE("groups/:groupKey/admins/:userKey")
    fun adminRemoveAdmin(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Path("userKey") userKey: String,
    ): Single<Response<Void>>

    //Get Rated Group
    @GET("groups/rating")
    fun getRatedGroups(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("timePeriod") timePeriod: Int,
        @Query("size") size: Int,
        @Query("page") page: Int,
        @Query("distance") distance: Int
    ): Observable<ArrayList<GroupDetails>>

    //Get Group Feed
    @GET("feed/group/{groupKey}")
    fun getGroupFeed(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Query("nextPage") nextPage: String,
        @Query("size") size: Int
    ): Observable<Feed>

    //Get Group Feed Without next Key
    @GET("feed/group/{groupKey}")
    fun getGroupFeedFirst(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Query("size") size: Int
    ): Observable<Feed>

    //Place Apis
    @POST("places")
    fun createPlace(
        @Header("Authorization") token: String,
        @Body createPlaceBody: CreatePlaceBody
    ): Single<PlaceDetails>

    @POST("places/rate")
    fun ratePlace(
        @Header("Authorization") token: String,
        @Body ratePlaceBody: RatePlaceBody
    ): Single<RatePlaceModel>

    //Get Place For Map
    @GET("places/map")
    fun findPlacesForMap(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("distance") distance: Int,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<PlaceDetails>>

    @GET("polygon-calculations/polygons")
    fun findPalindromePlacesByGroupKeyForMap(
        @Header("Authorization") token: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("distance") distance: Int,
        @Query("groupKey") groupKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<PolygonPlaceDetails>>
    @GET("polygon-calculations/polygons-page")
    fun findPolygonPlacesForMap(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("distance") distance: Int,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<PolygonPlaceDetails>>

    @GET("groups/map")
    fun findGroupsForMap(
        @Header("Authorization") token: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("distance") distance: Int,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<String>>

    @GET("places")
    fun getPlaces(
        @Header("Authorization") token: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("name") name: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("distance") distance: Int,
    ): Observable<ArrayList<PlaceDetails>>

    @GET("places-info")
    fun getLocationInfo(
        @Header("Authorization") token: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): Observable<LocationInfoModel>

    //Place Owned By Group
    @GET("places")
    fun getPlaceOwnedByGroup(
        @Header("Authorization") token: String,
        @Header("Orbis-Coordinates") coordinates: String,
        @Query("ownedByGroupKey") ownedByGroupKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<PlaceDetails>>

    //Get Place Details
    @GET("places/{placeKey}")
    fun getPlaceDetails(
        @Header("Authorization") token: String,
        @Path("placeKey") placeKey: String
    ): Observable<PlaceDetails>
    @GET("polygon-calculations/polygons/{placeKey}")
    fun getPolygonPlaceDetails(
        @Path("placeKey") placeKey: String
    ): Observable<PolygonPlaceDetails>

    @GET("polygon-calculations/{checkInPolygonCoordinateKey}")
    fun getPolygonPlaceStatus(
        @Path("checkInPolygonCoordinateKey") checkInPolygonCoordinateKey: String
    ): Observable<CheckInStatus>

    @PUT("groups/{groupKey}/members")
    fun joinGroup(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String
    ): Single<Response<Void>>

    @DELETE("groups/{groupKey}/members")
    fun leaveGroup(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String
    ): Single<Response<Void>>

    @PUT("groups/{groupKey}/followers")
    fun followGroup(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String
    ): Single<Response<Void>>

    @DELETE("groups/{groupKey}/followers")
    fun unfollowGroup(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String
    ): Single<Response<Void>>

    @DELETE("groups/{groupKey}")
    fun deleteGroup(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String
    ): Single<Response<Void>>

    @POST("groups/{groupKey}/report")
    fun reportGroup(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Body requestBody: RequestBody
    ): Single<ResponseBody>

    @GET("profile/{userKey}/groupFollower")
    fun getUserGroups(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String,
        @Query("size") size: Int,
        @Query("page") page: Int
    ): Observable<ArrayList<GroupDetails>>
    //Events

    @GET("events/{postKey}/attendees")
    fun getAttendees(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Single<ArrayList<User>>


    @PUT("events/{postKey}/attend")
    fun attendEvent(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String
    ): Single<Response<Void>>

    @DELETE("events/{postKey}/attend")
    fun unattendEvent(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String
    ): Single<Response<Void>>

    //Posts
    @PUT("posts/{postKey}/like")
    fun likePost(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String
    ): Single<FeedPost>

    @DELETE("posts/{postKey}/like")
    fun unlikePost(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String
    ): Single<FeedPost>

    @PUT("posts/{postKey}/comments/{commentKey}/like")
    fun likeComment(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String,
        @Path("commentKey") commentKey: String
    ): Single<Response<Void>>

    @DELETE("posts/{postKey}/comments/{commentKey}/like")
    fun unlikeComment(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String,
        @Path("commentKey") commentKey: String
    ): Single<Response<Void>>

    @DELETE("posts/{postKey}/comments/{commentKey}")
    fun deleteComment(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String,
        @Path("commentKey") commentKey: String
    ): Single<Response<Void>>

    @PUT("posts/{postKey}/share")
    fun sharePost(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String
    ): Single<ResponseBody>

    @DELETE("posts/{postKey}")
    fun deletePost(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String
    ): Single<Response<Void>>


    @GET("posts/{postKey}")
    fun getPost(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String
    ): Single<FeedPost>

    @POST("posts/{postKey}/report")
    fun reportPost(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String,
        @Body requestBody: RequestBody
    ): Single<ResponseBody>

    @POST("posts")
    fun createPost(
        @Header("Authorization") token: String,
        @Body postBody: PostBody
    ): Single<FeedPost>

    //Feed
    @GET("feed/news")
    fun getNewsFeedFirst(
        @Header("Authorization") token: String,
        @Query("size") size: Int,
        @Query("distance") distance: Int
    ): Observable<Feed>

    @GET("feed/news")
    fun getNewsFeed(
        @Header("Authorization") token: String,
        @Query("nextPage") nextPage: String,
        @Query("size") size: Int,
        @Query("distance") distance: Int
    ): Observable<Feed>

    @GET("feed/near")
    fun getNearbyFeedFirst(
        @Header("Authorization") token: String,
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("distance") distance: Int,
        @Query("size") size: Int
    ): Observable<Feed>

    @GET("feed/near")
    fun getNearbyFeed(
        @Header("Authorization") token: String,
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("nextPage") nextPage: String,
        @Query("distance") distance: Int,
        @Query("size") size: Int
    ): Observable<Feed>

    @GET("feed/place/{placeKey}")
    fun getPlaceFeedFirst(
        @Header("Authorization") token: String,
        @Path("placeKey") placeKey: String,
        @Query("size") size: Int
    ): Observable<Feed>

    @GET("feed/place/{placeKey}")
    fun getPlaceFeed(
        @Header("Authorization") token: String,
        @Path("placeKey") placeKey: String,
        @Query("size") size: Int,
        @Query("nextPage") nextPage: String,
    ): Observable<Feed>

    @PUT("places/{placeKey}/follow")
    fun followPlace(
        @Header("Authorization") token: String,
        @Path("placeKey") placeKey: String
    ): Single<Response<Void>>

    @DELETE("places/{placeKey}/follow")
    fun unfollowPlace(
        @Header("Authorization") token: String,
        @Path("placeKey") placeKey: String
    ): Single<Response<Void>>


    @PUT("places/{placeKey}")
    fun updatePlace(
        @Header("Authorization") token: String,
        @Path("placeKey") placeKey: String,
        @Body placeUpdateBody: PlaceUpdateBody
    ): Single<PlaceDetails>


    //Profile APIs
    @GET("profile/me")
    fun getMyProfile(
        @Header("Authorization") token: String,
    ): Observable<UserInfo>

    @POST("profile/me")
    fun updateMyProfile(
        @Header("Authorization") token: String,
        @Body userInfo: UserInfo
    ): Observable<UserInfo>

    @GET("profile/me/pictures")
    fun getMyPictures(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Observable<ArrayList<UserPictures>>

    @GET("profile/{userKey}/pictures")
    fun getUserPictures(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Observable<ArrayList<UserPictures>>

    @GET("feed/user")
    fun getMyFeedFirst(
        @Header("Authorization") token: String,
        @Query("size") size: Int
    ): Observable<Feed>

    @GET("feed/user")
    fun getMyFeed(
        @Header("Authorization") token: String,
        @Query("nextPage") nextPage: String,
        @Query("size") size: Int
    ): Observable<Feed>

    @GET("feed/user/{userKey}")
    fun getUserFeedFirst(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String,
        @Query("size") size: Int
    ): Observable<Feed>

    @GET("feed/user/{userKey}")
    fun getUserFeed(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String,
        @Query("nextPage") nextPage: String,
        @Query("size") size: Int
    ): Observable<Feed>

    @GET("profile/{userKey}")
    fun getUserProfile(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String
    ): Observable<UserInfo>

    @PUT("profile/{userKey}/follow")
    fun followUser(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String
    ): Single<UserInfo>

    @DELETE("profile/{userKey}/follow")
    fun unfollowUser(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String
    ): Single<Response<Void>>

    @POST("ig/connect")
    fun igConnect(
        @Header("Authorization") token: String,
    ): Observable<IGConnectModel>

    @GET("profile/me/igpictures")
    fun getMyIgPictures(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Observable<ArrayList<UserPictures>>

    @GET("profile/{userKey}/igpictures")
    fun getUserIgPictures(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Observable<ArrayList<UserPictures>>


    @GET("profile/me/followers")
    fun getMyFollowers(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<UserInfo>>

    @GET("profile/{userKey}/followers")
    fun getUserFollowers(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<UserInfo>>
    //profile/me/following?type=USER

    @GET("profile/me/following?type=USER")
    fun getMyFollowing(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Observable<ArrayList<FollowingModel>>

    @GET("profile/{userKey}/following?type=USER")
    fun getUserFollowing(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Observable<ArrayList<FollowingModel>>


    @POST("profile/{userKey}/report")
    fun reportUser(
        @Header("Authorization") token: String,
        @Path("userKey") groupKey: String,
        @Body requestBody: RequestBody
    ): Single<ResponseBody>

    @POST("places/{placeKey}/report")
    fun reportPlace(
        @Header("Authorization") token: String,
        @Path("placeKey") placeKey: String,
        @Body requestBody: RequestBody
    ): Single<ResponseBody>

    @PUT("profile/{userKey}/block")
    fun blockUser(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String,
    ): Single<ResponseBody>

    @GET("profile/me/admin")
    fun getMyAdminGroups(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Observable<ArrayList<GroupDetails>>

    @GET("profile/me/following?type=PLACE")
    fun getMyFollowingPlace(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Observable<ArrayList<FollowingPlaceModel>>

    @POST("password")
    fun updatePassword(
        @Header("Authorization") token: String,
        @Body passwordUpdate: PasswordUpdate
    ): Single<ResponseBody>

    @DELETE("profile/me")
    fun deleteMyProfile(
        @Header("Authorization") token: String,
    ): Single<Response<Void>>

    @POST("profile/me/pictures")
    fun uploadProfilePicture(
        @Header("Authorization") token: String,
        @Body userPicturesBody: UserPicturesBody
    ): Single<UserPictures>

    @GET("profile")
    fun searchUser(
        @Header("Authorization") token: String,
        @Query("name") name: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<UserInfo>>

    @DELETE("profile/me/pictures/{pictureKey}")
    fun deletePhoto(
        @Header("Authorization") token: String,
        @Path("pictureKey") pictureKey: String
    ): Single<Response<Void>>

    @GET("notifications")
    fun getNotifications(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<NotificationModel>>

    @GET("profile/me/followers/pending")
    fun getPendingFollowers(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<UserInfo>>

    @GET("notifications/unreadcount")
    fun getUnreadCount(
        @Header("Authorization") token: String
    ): Observable<NotificationCount>

    @POST("notifications/seen")
    fun seenPost(
        @Header("Authorization") token: String,
        @Body keys: ArrayList<String>
    ): Single<Response<Void>>

    @PUT("profile/followers/{userKey}/accept")
    fun acceptFollow(
        @Header("Authorization") token: String,
        @Path("userKey") userKey: String
    ): Single<Response<Void>>

    @GET("feed/news/stories")
    fun getNewsStories(
        @Header("Authorization") token: String,
        @Query("seen") seen: Boolean,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<StoryModel>>

    @GET("feed/near/stories")
    fun getNearbyStories(
        @Header("Authorization") token: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("seen") seen: Boolean,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("distance") distance: Int
    ): Observable<ArrayList<StoryModel>>

    @PUT("feed/stories/{postKey}/seen")
    fun seenStory(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String
    ): Single<Response<Void>>

    @GET("posts/{postKey}/comments")
    fun getCommentsByPost(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<CommentModel>>

    @POST("posts/{postKey}/comments")
    fun postComment(
        @Header("Authorization") token: String,
        @Path("postKey") postKey: String,
        @Body createCommentBody: CreateCommentBody
    ): Single<ResponseBody>

    @GET("places/{key}/events")
    fun getEventBy(
        @Header("Authorization") token: String,
        @Path("key") key: String,
        @Query("pastEvents") pastEvents: Boolean,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<FeedPost>>

    @GET("groups/{key}/events")
    fun getEventByGroup(
        @Header("Authorization") token: String,
        @Path("key") key: String,
        @Query("pastEvents") pastEvents: Boolean,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Observable<ArrayList<FeedPost>>

    // Subscriptions APIs
    @POST("groups/subscription/{groupKey}")
    fun createSubscription(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Body requestBody: CreateSubscriptionBody,
    ): Single<Response<Void>>

    @PUT("groups/subscription/{groupKey}")
    fun editSubscription(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Body requestBody: CreateSubscriptionBody,
    ): Single<Response<Void>>

    @DELETE("groups/subscription/{groupKey}/{subscriptionKey}")
    fun deleteSubscription(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Path("subscriptionKey") subscriptionKey: String,
    ): Single<Response<Void>>

    @GET("groups/subscription/info")
    fun getSubscriptionInfo(
        @Header("Authorization") token: String,
    ): Single<SubscriptionInfo>

    @POST("stripe")
    fun createStripe(
        @Header("Authorization") token: String,
        @Body requestBody: CreateStripeBody,
    ): Single<CreateStripeResponse>

    @GET("stripe")
    fun getStripe(
        @Header("Authorization") token: String,
    ): Single<GetStripeResponse>

    @PUT("stripe")
    fun updateStripe(
        @Header("Authorization") token: String,
    ): Single<CreateStripeResponse>

    @POST("groups/subscription/{groupKey}/activate")
    fun activateSubscription(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
    ): Single<Response<Void>>

    @POST("groups/subscription/{groupKey}/deactivate")
    fun deactivateSubscription(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Query("sure") sure: Boolean,
    ): Single<Response<Void>>

    @GET("groups/subscription/{groupKey}")
    fun getSubscriptions(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Single<ArrayList<Subscription>>

    @GET("groups/subscription/{groupKey}/subscribers")
    fun getSubscribers(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Query("subscriptionKey") subscriptionKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Single<ArrayList<User>>

    @GET("/groups/purchases/{groupKey}")
    fun getPurchases(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Query("purchaseKey") purchaseKey: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Single<ArrayList<User>>



    @GET("groups/subscription/{groupKey}/{subscriptionKey}/statistic")
    fun getSubscriptionStatistic(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Path("subscriptionKey") subscriptionKey: String,
        @Query("type") type : String,
    ): Single<SubscriptionStatistic>

    @GET("groups/purchases/{groupKey}/{subscriptionKey}/statistic")
    fun getPurchaseKeyStatistic(
        @Header("Authorization") token: String,
        @Path("groupKey") groupKey: String,
        @Path("subscriptionKey") subscriptionKey: String,
        @Query("type") type : String,
    ): Single<SubscriptionStatistic>

    @GET("profile/subscriptions")
    fun getMySubscriptions(
        @Header("Authorization") token: String,
    ): Single<ArrayList<Subscription>>

    @GET("/profile/purchases")
    fun getMyPurchases(
        @Header("Authorization") token: String,
    ): Single<ArrayList<Subscription>>

    @POST("profile/subscription/{subscriptionKey}/subscribe")
    fun subscribeSubscription(
        @Header("Authorization") token: String,
        @Path("subscriptionKey") subscriptionKey: String,
    ): Single<SubscribeResponse>

    @POST("profile/purchase/{subscriptionKey}/buy")
    fun purchaseSubscription(
        @Header("Authorization") token: String,
        @Path("subscriptionKey") subscriptionKey: String,
        @Query("number") quantity: Int,
    ): Single<SubscribeResponse>



    @POST("profile/subscription/{subscriptionKey}/unsubscribe")
    fun unsubscribeSubscription(
        @Header("Authorization") token: String,
        @Path("subscriptionKey") subscriptionKey: String,
    ): Single<Response<Void>>
}