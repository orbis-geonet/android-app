package com.orbis.orbis.ui.placesModule

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.orbis.orbis.extensions.getMimeType
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.ErrorResponse
import com.orbis.orbis.models.group.Feed
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.*
import com.orbis.orbis.models.posts.PostBody
import com.orbis.orbis.repositories.AuthRepositories
import com.orbis.orbis.repositories.GroupRepositories
import com.orbis.orbis.repositories.PlaceRepositories
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observer
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONException
import retrofit2.HttpException
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@HiltViewModel
class PlaceViewModel @Inject constructor(
    private val groupRepositories: GroupRepositories,
    private val authRepositories: AuthRepositories,
    private val placeRepositories: PlaceRepositories
) : ViewModel() {
    val recommendedGroups: MutableLiveData<ArrayList<GroupDetails>> = MutableLiveData()
    val error: MutableLiveData<String> = MutableLiveData()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val placeDetails: MutableLiveData<PlaceDetails> = MutableLiveData()
    val mapPlaces: MutableLiveData<ArrayList<PlaceDetails>> = MutableLiveData()
    val mapPolygonPlaces: MutableLiveData<ArrayList<PolygonPlaceDetails>> = MutableLiveData()
    val mapGroups: MutableLiveData<ArrayList<String>> = MutableLiveData()
    val newAddedPlace: MutableLiveData<PlaceDetails> = MutableLiveData()
    val newAddedPolygonPlace: MutableLiveData<PolygonPlaceDetails> = MutableLiveData()
    val updatedPolygon: MutableLiveData<Pair<PolygonPlaceDetails, ArrayList<PolygonPlaceDetails?>>> = MutableLiveData() //Pair(oldPolygon, newPolygon)
    val polygonsInUpdate = HashMap<PolygonPlaceDetails, ArrayList<PolygonPlaceDetails?>>() //parent -> subplaces updates
    val updatedCircle: MutableLiveData<Pair<PolygonPlaceDetails, PolygonPlaceDetails?>> = MutableLiveData() //Pair(oldCircle, newCircle)
    val polygonFocusedPlaces: MutableLiveData<ArrayList<PolygonPlaceDetails>> = MutableLiveData()
    val newFocusedChanges: MutableLiveData<Pair<PolygonPlaceDetails, PolygonPlaceDetails>?> = MutableLiveData() //Pair(oldFocus, newFocus)
    val locationInfo: MutableLiveData<LocationInfoModel?> = MutableLiveData()
    val feed: MutableLiveData<Feed> = MutableLiveData()
    val placeFollowed: MutableLiveData<Boolean> = MutableLiveData()
    val placeUnfollowed: MutableLiveData<Boolean> = MutableLiveData()
    val getPost: MutableLiveData<FeedPost> = MutableLiveData()
    val events: MutableLiveData<ArrayList<FeedPost>> = MutableLiveData()
    val allCities: MutableLiveData<HashMap<String, LatLng>> = MutableLiveData()
    var changedFocus = false
    var locationSharedOrSelected: Boolean = false
    var lastLocationSelected: Location? = null
    private val _checkInProgress = MutableStateFlow(1)
    val checkInProgress: StateFlow<Int> = _checkInProgress

    suspend fun startCheckInLoading() {
        for (i in 1..15) {
            _checkInProgress.update { i }
            delay(55L)
        }
        for (i in 16..75) {
            _checkInProgress.update { i }
            delay(26L)
        }

        // Slow down and progress from 76% to 100% in 7 seconds
        for (i in 76..99) {
            _checkInProgress.update { i }
            delay(300L)
        }
    }
    fun completeCheckInProgressBar(){
        _checkInProgress.update { 100 }
    }
    fun restartCheckInProgressBar(){
        _checkInProgress.value = 1
    }

    fun getEvents(placeKey: String, page: Int) {
        isLoading.postValue(true)
        placeRepositories.getEvents(placeKey, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<FeedPost>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<FeedPost>) {
                    isLoading.postValue(false)
                    events.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getEvents(placeKey, page)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            getEvents(placeKey, page)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {

                }

            })
    }
    fun buildCities(inputStream: InputStream){
        data class City(
            val n: String, // name
            val c: String, // country
            val l: Double, // latitude
            val g: Double  // longitude
        )

        try {
            //this scope runs on IO thread guaranteeing that the UI remains working with or without cities data
            viewModelScope.launch(Dispatchers.IO){
                val cities = HashMap<String, LatLng>()
                val reader = InputStreamReader(inputStream)

                val gson = Gson()
                val cityListType = object : TypeToken<List<City>>() {}.type
                val cityList: List<City> = gson.fromJson(reader, cityListType)

                for (city in cityList) {
                    val cityLocation = LatLng(city.l, city.g)
                    cities["${city.n}, ${city.c}"] = cityLocation
                }

                reader.close()
                allCities.postValue(cities)
            }
        } catch (e: JSONException) {
            Log.e("Map", "Error parsing cities JSON: ${e.message}")
        }
    }

    fun getPost(postKey: String) {
        isLoading.postValue(true)
        groupRepositories.getPost(postKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<FeedPost> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: FeedPost) {
                    isLoading.postValue(false)

                    getPost.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getPost(postKey)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            getPost(postKey)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

            })
    }

    fun findRecommendedGroup(latitude: Double, longitude: Double) {
        isLoading.postValue(true)
        groupRepositories.getGroups(latitude, longitude, "")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<GroupDetails>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<GroupDetails>) {
                    isLoading.postValue(false)
                    recommendedGroups.postValue(t.take(5) as ArrayList<GroupDetails>)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        findRecommendedGroup(latitude, longitude)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            findRecommendedGroup(latitude, longitude)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {

                }


            })
    }

    fun createPlace(createPlaceBody: CreatePlaceBody) {
        isLoading.postValue(true)
        placeRepositories.createPlace(createPlaceBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<PlaceDetails> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: PlaceDetails) {
                    isLoading.postValue(false)
                    placeDetails.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        createPlace(createPlaceBody)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            isLoading.postValue(false)
                                            authRepositories.saveIdToken(t.string())
                                            createPlace(createPlaceBody)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

            })
    }

    val placeRated: MutableLiveData<RatePlaceModel> = MutableLiveData()
    fun ratePlace(ratePlaceBody: RatePlaceBody) {
        isLoading.postValue(true)
        placeRepositories.ratePlace(ratePlaceBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<RatePlaceModel>
            {
                override fun onSubscribe(d: Disposable) { }

                override fun onSuccess(t: RatePlaceModel)
                {
                    isLoading.postValue(false)
                    placeRated.postValue(t)
                }

                override fun onError(e: Throwable)
                {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        ratePlace(ratePlaceBody)
                                    }
                            }
                            else
                            {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody>
                                    {
                                        override fun onSubscribe(d: Disposable)  { }
                                        override fun onError(e: Throwable) { }
                                        override fun onSuccess(t: ResponseBody)
                                        {
                                            isLoading.postValue(false)
                                            authRepositories.saveIdToken(t.string())
                                            ratePlace(ratePlaceBody)
                                        }
                                    })
                            }
                        }
                        else
                        {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {
                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }
            })
    }

    fun findPlacesForMap(latitude: Double, longitude: Double, page: Int = 0) {
        isLoading.postValue(true)
        placeRepositories.findPlacesForMap(latitude, longitude, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<PlaceDetails>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<PlaceDetails>) {
                    isLoading.postValue(false)
                    mapPlaces.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        findPlacesForMap(latitude, longitude)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            findPlacesForMap(latitude, longitude)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {

                }

            })
    }

    fun findGroupsForMap(latitude: Double, longitude: Double, page: Int = 0) {
        placeRepositories.findGroupsForMap(latitude, longitude, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<String>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<String>) {
                    mapGroups.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        findGroupsForMap(latitude, longitude)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            findGroupsForMap(latitude, longitude)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {

                }

            })
    }

    fun clearMapPolygonPlaces(){
        mapPolygonPlaces.postValue(arrayListOf())
    }
    fun findPolygonPlacesForMapTest(inputStream: InputStream){
        val result = ArrayList<PolygonPlaceDetails>()
        val reader = InputStreamReader(inputStream)
        val gson = Gson()
        val polygonListType = object : TypeToken<List<PolygonPlaceDetails>>() {}.type
        val polygonList: List<PolygonPlaceDetails> = gson.fromJson(reader, polygonListType)

        result.addAll(polygonList)
        reader.close()

        mapPolygonPlaces.postValue(result)
    }
    fun findPolygonPlacesForMap(latitude: Double, longitude: Double, page: Int = 0) {
        isLoading.postValue(true)
        placeRepositories.findPolygonPlacesForMap(latitude, longitude, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<PolygonPlaceDetails>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<PolygonPlaceDetails>) {
                    mapPolygonPlaces.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        findPolygonPlacesForMap(latitude, longitude, page)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            findPolygonPlacesForMap(latitude, longitude, page)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else if (e.code() == 404){
                            mapPolygonPlaces.postValue(arrayListOf())
                        }
                        else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {

                }

            })
    }

    fun updatePolygon(parentPlace: PolygonPlaceDetails, polygonSubPlace: PolygonPlaceDetails){
        if (polygonsInUpdate[parentPlace].isNullOrEmpty())
            polygonsInUpdate[parentPlace] = arrayListOf()
        placeRepositories.getPolygonPlaceDetails(polygonSubPlace.placeKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<PolygonPlaceDetails> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: PolygonPlaceDetails) {
                    polygonsInUpdate[parentPlace]!!.add(t)
                    if (polygonsInUpdate[parentPlace]!!.size == parentPlace.places.size)
                        updatedPolygon.postValue(Pair(parentPlace, polygonsInUpdate[parentPlace]!!))
                }

                override fun onError(e: Throwable) {
                    if (e is EOFException){
                        polygonsInUpdate[parentPlace]!!.add(null)
                        if (polygonsInUpdate[parentPlace]!!.size == parentPlace.places.size)
                            updatedPolygon.postValue(Pair(parentPlace, polygonsInUpdate[parentPlace]!!))
                    }
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        updatePolygon(parentPlace, polygonSubPlace)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            updatePolygon(parentPlace, polygonSubPlace)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {}

            })
    }
    fun updateCircle(polygonSubPlace: PolygonPlaceDetails){
        placeRepositories.getPolygonPlaceDetails(polygonSubPlace.placeKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<PolygonPlaceDetails> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: PolygonPlaceDetails) {
                    updatedCircle.postValue(Pair(polygonSubPlace, t))
                }

                override fun onError(e: Throwable) {
                    //easy way to catch when with code 200 we have a null value
                    if (e is EOFException){
                        updatedCircle.postValue(Pair(polygonSubPlace, null))
                    }
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        updateCircle(polygonSubPlace)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            updateCircle(polygonSubPlace)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {}

            })
    }

    fun getUpdatedFocusedPolygonPlace(placeKey: String){
        placeRepositories.getPolygonPlaceDetails(placeKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<PolygonPlaceDetails> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(t: PolygonPlaceDetails) {
                    val focusList = polygonFocusedPlaces.value!!
                    for (i in focusList.indices){
                        if (focusList[i].placeKey == placeKey){
                            t.isFocusSelected = true
                            focusList[i] = t
                            break;
                        }
                    }
                    changedFocus = false
                    polygonFocusedPlaces.postValue(focusList)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getUpdatedFocusedPolygonPlace(placeKey)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) { }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            getUpdatedFocusedPolygonPlace(placeKey)
                                        }

                                        override fun onError(e: Throwable) { }

                                    })
                            }
                        } else {
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) { }
                        }
                    } else {
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() { }

            })
    }

    fun getNewPlace(placeKey: String) {
        isLoading.postValue(true)
        placeRepositories.getPlaceDetails(placeKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<PlaceDetails> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: PlaceDetails) {
                    isLoading.postValue(false)
                    newAddedPlace.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getNewPlace(placeKey)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            getNewPlace(placeKey)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {

                }

            })
    }
    fun askPolygonMapUpdate(checkInPolygonCoordinateKey: String, place: PlaceDetails){
        placeRepositories.getPolygonPlaceStatus(checkInPolygonCoordinateKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<CheckInStatus> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: CheckInStatus) {
                    if (t.status == "DONE")
                        newAddedPlace.postValue(place)
                    else
                        askPolygonMapUpdate(checkInPolygonCoordinateKey, place)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        askPolygonMapUpdate(checkInPolygonCoordinateKey, place)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            askPolygonMapUpdate(checkInPolygonCoordinateKey, place)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {

                }

            })
    }
    fun findPolygonPlace(placeKey: String) {
        isLoading.postValue(true)
        placeRepositories.getPolygonPlaceDetails(placeKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<PolygonPlaceDetails> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: PolygonPlaceDetails) {
                    isLoading.postValue(false)
                    newAddedPolygonPlace.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        findPolygonPlace(placeKey)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            findPolygonPlace(placeKey)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {

                }

            })
    }

    fun getPlaces(
        latitude: Double,
        longitude: Double,
        name: String,
        page: Int,
        distance: Int = 1
    ) {
        isLoading.postValue(true)
        placeRepositories.getPlaces(latitude, longitude, name, page, distance)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<PlaceDetails>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<PlaceDetails>) {
                    isLoading.postValue(false)
                    mapPlaces.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getPlaces(latitude, longitude, name, page)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            getPlaces(latitude, longitude, name, page)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {

                }

            })
    }

    fun uploadEventImage(context: Context, uri: Uri) {
        isLoading.postValue(true)
        val cR = context.contentResolver
        val mime = MimeTypeMap.getSingleton()
        var type = mime.getExtensionFromMimeType(cR.getType(uri))
        Log.d("typeCHeck", type + " found")
        if (type.isNullOrEmpty()) {
            type = "jpg"
        }
        val selectedImage = getCapturedImage(context, uri)
        val baos = ByteArrayOutputStream()
        if (type == "png") {
            selectedImage.compress(Bitmap.CompressFormat.PNG, 100, baos)
        } else {
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        }
        val data = baos.toByteArray()
        val imageName = UUID.randomUUID().toString() + "." + type
        val path = "${Constants.EVENT_STORAGE}$imageName"
        Log.d("uploadPath", path)
        var metadata = storageMetadata {
            contentType = when {
                type.contains("png") -> {
                    "image/png"
                }
                type.contains("tiff") -> {
                    "image/tiff"
                }
                type.contains("webp") -> {
                    "image/webp"
                }
                else -> {
                    "image/jpeg"
                }
            }
        }

        val storage = Firebase.storage.reference.child(path)
        val uploadTask = storage.putBytes(data, metadata)
        uploadTask.addOnSuccessListener {
            postBody?.mediaUrls?.add(imageName)
            createPost()
        }.addOnFailureListener {
            error.postValue(it.localizedMessage)
        }
    }

    val fileUpload: MutableLiveData<Boolean> = MutableLiveData(false)
    var postBody: PostBody? = null
    fun uploadPostImage(context: Context, uris: ArrayList<Uri>, position: Int) {
        if (position == (uris.size)) {
            isLoading.postValue(false)
            if (postBody?.checkin!!) {
                createCheckinPost()
            } else {
                createPost()
            }

        } else {
            isLoading.postValue(true)
            val cR = context.contentResolver
            val mime = MimeTypeMap.getSingleton()
            var type = mime.getExtensionFromMimeType(cR.getType(uris[position]))
            Log.d("typeCHeck", type + " found")
            if (type.isNullOrEmpty()) {
                type = "jpg"
            }
            val selectedImage = getCapturedImage(context, uris[position])
            val baos = ByteArrayOutputStream()
            if (type == "png") {
                selectedImage.compress(Bitmap.CompressFormat.PNG, 100, baos)
            } else {
                selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            }
            val data = baos.toByteArray()
            val imageName = UUID.randomUUID().toString() + "." + type
            val path = "posts/images/$imageName"
            Log.d("uploadPath", path)
            var metadata = storageMetadata {
                contentType = when {
                    type.contains("png") -> {
                        "image/png"
                    }
                    type.contains("tiff") -> {
                        "image/tiff"
                    }
                    type.contains("webp") -> {
                        "image/webp"
                    }
                    else -> {
                        "image/jpeg"
                    }
                }
            }

            val storage = Firebase.storage.reference.child(path)
            val uploadTask = storage.putBytes(data, metadata)
            uploadTask.addOnSuccessListener {
                postBody?.mediaUrls?.add(imageName)
                uploadPostImage(context, uris, position + 1)
            }.addOnFailureListener {
                uploadPostImage(context, uris, position + 1)
                error.postValue(it.localizedMessage)
            }
        }

    }

    fun uploadAudioPost(file: File) {
        isLoading.postValue(true)
        val audioName = file.name
        val path = "posts/audios/$audioName"
        Log.d("uploadPath", path)
        var metadata = storageMetadata {
            contentType = "audio/wav"
        }
        val storage = Firebase.storage.reference.child(path)
        val uploadTask = storage.putFile(Uri.fromFile(file), metadata)
        uploadTask.addOnSuccessListener {
            postBody?.mediaUrls?.add(audioName)
            file.delete()
            if (postBody?.checkin!!) {
                createCheckinPost()
            } else {
                createPost()
            }
        }.addOnFailureListener {
            error.postValue(it.localizedMessage)
        }


    }

    fun uploadVideoPost(context: Context, uri: Uri) {
        isLoading.postValue(true)
        val cR = context.contentResolver
        val mime = MimeTypeMap.getSingleton()
        var type = uri.getMimeType(context)

        if (type.isEmpty()) {
            type = "mp4"
        }
        Log.d("typeCHeck", type + " found")
        val videoName = UUID.randomUUID().toString() + "." + type
        val path = "posts/videos/$videoName"
        Log.d("uploadPath", path)
        val storage = Firebase.storage.reference.child(path)
        var metadata = storageMetadata {
            contentType = when {
                type.contains("mp4") -> {
                    "video/mp4"
                }
                type.contains("mov") -> {
                    "video/mov"
                }
                else -> {
                    "video/mp4"
                }
            }
        }

        val uploadTask = storage.putFile(uri, metadata)
        uploadTask.addOnSuccessListener {
            postBody?.mediaUrls?.add(videoName)
            if (postBody?.checkin!!) {
                createCheckinPost()
            } else {
                createPost()
            }
        }.addOnFailureListener {
            error.postValue(it.localizedMessage)
        }
    }

    val postCreated: MutableLiveData<FeedPost> = MutableLiveData()
    fun createPost(checkinPost: FeedPost? = null) {
        postBody?.type = postBody?.subType!!
        postBody?.checkin = false
        isLoading.postValue(true)
        placeRepositories.createPost(postBody!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<FeedPost> {
                override fun onSubscribe(p0: Disposable) {

                }

                override fun onSuccess(p0: FeedPost) {
                    isLoading.postValue(false)
                    if (checkinPost != null) {
                        postCreated.postValue(checkinPost)
                    } else {
                        postCreated.postValue(p0)
                    }
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        createPost()
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            createPost()
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

            })


    }

    fun createCheckinPost() {
        postBody?.type = "CHECK_IN"
        isLoading.postValue(true)
        placeRepositories.createPost(postBody!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<FeedPost> {
                override fun onSubscribe(p0: Disposable) {

                }

                override fun onSuccess(p0: FeedPost) {
                    followPlace(postBody?.placeKey!!)
                    if (!postBody?.subType.isNullOrEmpty()) {
                        createPost(p0)
                    } else {
                        isLoading.postValue(false)
                        postCreated.postValue(p0)
                    }
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        createCheckinPost()
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            createCheckinPost()
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

            })
    }
    fun getPlaceFeedFirst(placeKey: String) {
        isLoading.postValue(true)
        placeRepositories.getPlaceFeedFirst(placeKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Feed> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: Feed) {
                    isLoading.postValue(false)
                    feed.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getPlaceFeedFirst(placeKey)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            getPlaceFeedFirst(placeKey)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {

                }

            })
    }

    fun getPlaceFeed(placeKey: String, nextPage: String) {
        isLoading.postValue(true)
        placeRepositories.getPlaceFeed(placeKey, nextPage)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Feed> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: Feed) {
                    isLoading.postValue(false)
                    feed.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getPlaceFeed(placeKey, nextPage)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            getPlaceFeed(placeKey, nextPage)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() {

                }

            })
    }

    fun getLocationInfo(latitude: Double, longitude: Double)
    {
        placeRepositories.getLocationInfo(latitude, longitude)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<LocationInfoModel>
            {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(t: LocationInfoModel) {
                    locationInfo.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    locationInfo.postValue(null)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getLocationInfo(latitude, longitude)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            getLocationInfo(latitude, longitude)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onComplete() { }
            })
    }
    private fun getCapturedImage(context: Context, selectedPhotoUri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, selectedPhotoUri)
            ImageDecoder.decodeBitmap(source)

        } else {
            MediaStore.Images.Media.getBitmap(
                context.contentResolver,
                selectedPhotoUri
            )
        }
    }

    fun followPlace(placeKey: String) {
        isLoading.postValue(true)
        placeRepositories.followPlace(placeKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    placeFollowed.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        followPlace(placeKey)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            followPlace(placeKey)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

            })
    }

    fun unfollowPlace(placeKey: String) {
        isLoading.postValue(true)
        placeRepositories.unfollowPlace(placeKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    placeUnfollowed.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        unfollowPlace(placeKey)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            unfollowPlace(placeKey)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

            })
    }

    fun updatePlace(placeUpdateBody: PlaceUpdateBody, placeKey: String) {
        isLoading.postValue(true)
        placeRepositories.updatePlace(placeUpdateBody, placeKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<PlaceDetails> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: PlaceDetails) {
                    isLoading.postValue(false)
                    placeDetails.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        updatePlace(placeUpdateBody, placeKey)
                                    }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            updatePlace(placeUpdateBody, placeKey)
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            try {
                                val body = e.response()?.errorBody()
                                val gson = Gson()
                                val adapter: TypeAdapter<ErrorResponse> =
                                    gson.getAdapter(ErrorResponse::class.java)
                                val errorBody = adapter.fromJson(body?.string())
                                error.postValue(errorBody.message)
                            } catch (e: Exception) {

                            }
                        }
                    } else {
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

            })

    }

    fun uploadPlaceImage(
        context: Context,
        resultUri: Uri,
        selectedImage: Bitmap,
        placeKey: String
    ) {
        isLoading.postValue(true)
        val placeUpdateBody = PlaceUpdateBody()
        isLoading.postValue(true)
        val cR = context.contentResolver
        val mime = MimeTypeMap.getSingleton()
        var type = mime.getExtensionFromMimeType(cR.getType(resultUri))
        Log.d("typeCHeck", type + " found")
        if (type.isNullOrEmpty()) {
            type = "jpg"
        }
        val baos = ByteArrayOutputStream()
        if (type == "png") {
            selectedImage.compress(Bitmap.CompressFormat.PNG, 100, baos)
        } else {
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        }
        val data = baos.toByteArray()
        val imageName = UUID.randomUUID().toString() + "." + type
        val path = Constants.PLACE_PICTURES + imageName
        Log.d("uploadPath", path)
        var metadata = storageMetadata {
            contentType = when {
                type.contains(".png") -> {
                    "image/png"
                }
                type.contains(".tiff") -> {
                    "image/tiff"
                }
                type.contains(".webp") -> {
                    "image/webp"
                }
                else -> {
                    "image/jpeg"
                }
            }


        }

        val storage = Firebase.storage.reference.child(path)
        val uploadTask = storage.putBytes(data, metadata)
        uploadTask.addOnSuccessListener {
            storage.downloadUrl.addOnSuccessListener {
                placeUpdateBody.imageName = imageName
                updatePlace(placeUpdateBody, placeKey)
            }
        }.addOnFailureListener {
            isLoading.postValue(false)
            error.postValue(it.localizedMessage)
        }

    }
    fun clearFocusedPolygonPLaces(){
        changedFocus = false
        polygonFocusedPlaces.postValue(arrayListOf())
        newFocusedChanges.postValue(null)
    }
    fun setFocusedListPolygon(places: ArrayList<PolygonPlaceDetails>){
        if (polygonFocusedPlaces.value!!.size == 0)
            polygonFocusedPlaces.postValue(places)
        //block updates from UI without using clearFocusedPolygonPLaces() first
    }
    fun updateFocusedPolygon(){
        val focusedList = polygonFocusedPlaces.value!!
        for (place in focusedList){
            if (place.isFocusSelected)
                getUpdatedFocusedPolygonPlace(place.placeKey)
        }
    }
    fun changeFocusedPlace(position: Int){
        val focusedList = polygonFocusedPlaces.value!!
        if(focusedList[position].isFocusSelected)
            return

        var oldPlace : PolygonPlaceDetails? = null
        for (i in 0 until focusedList.size){
            val currPlace = focusedList[i]
            if (currPlace.isFocusSelected)
                oldPlace = currPlace
            focusedList[i].isFocusSelected = i == position
        }
        changedFocus = true
        polygonFocusedPlaces.postValue(focusedList)

        if (oldPlace != null){
            oldPlace.isFocusSelected = false
            newFocusedChanges.postValue(Pair(oldPlace, focusedList[position]))
        }
    }
    fun changeFocusedPlace(selectedPlace: PolygonPlaceDetails) {
        if (selectedPlace.isFocusSelected)
            return
        val focusedList = polygonFocusedPlaces.value!!
        val oldPlace = focusedList.find { it.isFocusSelected }
        focusedList.forEach { it.isFocusSelected = it.placeKey == selectedPlace.placeKey }
        changedFocus = true
        polygonFocusedPlaces.postValue(focusedList)

        if (oldPlace != null){
            oldPlace.isFocusSelected = false
            selectedPlace.isFocusSelected = true
            newFocusedChanges.postValue(Pair(oldPlace, selectedPlace))
        }

    }

    fun setLastCitySelected(loc: LatLng){
        val customLocation = Location("OrbisCustomCity")
        customLocation.latitude = loc.latitude
        customLocation.longitude = loc.longitude
        lastLocationSelected = customLocation
    }
    fun setLastCitySelected(loc: Location){
        lastLocationSelected = loc
    }

}