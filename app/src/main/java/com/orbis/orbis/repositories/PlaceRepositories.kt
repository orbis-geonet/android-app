package com.orbis.orbis.repositories

import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.group.Feed
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.place.*
import com.orbis.orbis.models.posts.PostBody
import com.orbis.orbis.network.ApiInterface
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Response
import javax.inject.Inject

class PlaceRepositories @Inject constructor(
    private val apiInterface: ApiInterface,
    private val prefManager: PrefManager
) {
    fun createPlace(createPlaceBody: CreatePlaceBody): Single<PlaceDetails> {
        val token = "Bearer " + prefManager.getIdToken()
        return apiInterface.createPlace(token, createPlaceBody)
    }

    fun ratePlace(ratePlaceBody: RatePlaceBody): Single<RatePlaceModel> {
        val token = "Bearer " + prefManager.getIdToken()
        return apiInterface.ratePlace(token, ratePlaceBody)
    }

    fun getEvents(
        placeKey: String,
        page: Int
    ): Observable<ArrayList<FeedPost>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getEventBy(token, placeKey, false, page, 25)
    }

    fun findPlacesForMap(
        latitude: Double,
        longitude: Double,
        page: Int
    ): Observable<ArrayList<PlaceDetails>> {
        return apiInterface.findPlacesForMap(latitude, longitude, 50, page, 10)
    }

    fun findPolygonPlacesByGroupKeyForMap(
        latitude: Double,
        longitude: Double,
        groupKey: String,
        page: Int
    ): Observable<ArrayList<PolygonPlaceDetails>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.findPalindromePlacesByGroupKeyForMap(token, latitude, longitude, 25, groupKey, page, 500)
    }
    fun findPolygonPlacesForMap(
        latitude: Double,
        longitude: Double,
        page: Int
    ): Observable<ArrayList<PolygonPlaceDetails>> {
        return apiInterface.findPolygonPlacesForMap(latitude, longitude, 25, page, 20)
    }

    fun findGroupsForMap(
        latitude: Double,
        longitude: Double,
        page: Int
    ): Observable<ArrayList<String>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.findGroupsForMap(token, latitude, longitude, 25, page, 700)
    }

    fun getPlaceDetails(placeKey: String): Observable<PlaceDetails> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getPlaceDetails(token, placeKey)
    }
    fun getPolygonPlaceDetails(placeKey: String): Observable<PolygonPlaceDetails> {
        return apiInterface.getPolygonPlaceDetails(placeKey)
    }

    fun getPolygonPlaceStatus(checkInPolygonCoordinateKey: String): Observable<CheckInStatus> {
        return apiInterface.getPolygonPlaceStatus(checkInPolygonCoordinateKey)
    }

    fun getPlaces(
        latitude: Double,
        longitude: Double,
        name: String,
        page: Int,
        distance: Int
    ): Observable<ArrayList<PlaceDetails>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getPlaces(token, latitude, longitude, name, page, 25, distance)
    }

    fun createPost(postBody: PostBody): Single<FeedPost> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.createPost(token, postBody)

    }

    fun getPlaceFeedFirst(placeKey: String): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getPlaceFeedFirst(token, placeKey, 20)
    }

    fun getPlaceFeed(placeKey: String, nextPage: String): Observable<Feed> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getPlaceFeed(token, placeKey, 20, nextPage)
    }

    fun followPlace(placeKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.followPlace(token, placeKey)
    }

    fun unfollowPlace(placeKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.unfollowPlace(token, placeKey)
    }

    fun updatePlace(placeUpdateBody: PlaceUpdateBody, placeKey: String): Single<PlaceDetails> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.updatePlace(token, placeKey, placeUpdateBody)
    }

    fun getLocationInfo(latitude: Double, longitude: Double): Observable<LocationInfoModel> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getLocationInfo(token, latitude, longitude)
    }
}