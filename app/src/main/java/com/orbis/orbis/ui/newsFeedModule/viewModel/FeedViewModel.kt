package com.orbis.orbis.ui.newsFeedModule.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.orbis.orbis.database.entities.FeedEntity
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.ErrorResponse
import com.orbis.orbis.models.group.Feed
import com.orbis.orbis.models.group.FeedContent
import com.orbis.orbis.models.posts.CommentModel
import com.orbis.orbis.models.posts.CreateCommentBody
import com.orbis.orbis.models.story.StoryModel
import com.orbis.orbis.repositories.AuthRepositories
import com.orbis.orbis.repositories.FeedRepositories
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observer
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepositories: FeedRepositories,
    private val authRepositories: AuthRepositories
) : ViewModel() {
    val feed: MutableLiveData<Feed> = MutableLiveData()
    val cachedFeed: MutableLiveData<Feed> = MutableLiveData()
    val error: MutableLiveData<String> = MutableLiveData()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val newsStories: MutableLiveData<ArrayList<StoryModel>> = MutableLiveData()
    val nearbyStories: MutableLiveData<ArrayList<StoryModel>> = MutableLiveData()
    val comments: MutableLiveData<ArrayList<CommentModel>> = MutableLiveData()
    val commentPosted: MutableLiveData<Boolean> = MutableLiveData()
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun likeComment(postKey: String, commentKey: String) {
        isLoading.postValue(true)
        feedRepositories.likeComment(postKey, commentKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        likeComment(postKey, commentKey)
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
                                            likeComment(postKey, commentKey)
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

    fun unlikeComment(postKey: String, commentKey: String) {
        isLoading.postValue(true)
        feedRepositories.unlikeComment(postKey, commentKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        unlikeComment(postKey, commentKey)
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
                                            unlikeComment(postKey, commentKey)
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

    fun deleteComment(postKey: String, commentKey: String) {
        isLoading.postValue(true)
        feedRepositories.deleteComment(postKey, commentKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        deleteComment(postKey, commentKey)
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
                                            deleteComment(postKey, commentKey)
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

    fun postComment(postKey: String, createCommentBody: CreateCommentBody) {
        feedRepositories.postComment(postKey, createCommentBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ResponseBody> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ResponseBody) {
                    isLoading.postValue(false)
                    commentPosted.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        postComment(postKey, createCommentBody)
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
                                            postComment(postKey, createCommentBody)
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

    fun seenStory(postKey: String) {
        feedRepositories.seenStory(postKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        seenStory(postKey)
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
                                            seenStory(postKey)
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

    fun getNearbyStories(page: Int, latitude: Double, longitude: Double, seen: Boolean) {
        isLoading.postValue(true)
        feedRepositories.getNearbyStories(page, latitude, longitude, seen)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<StoryModel>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<StoryModel>)
                {
                    //for (story in t)
                    //    story.posts.sortBy { it.timestamp }

                    nearbyStories.postValue(t)
                    isLoading.postValue(false)

                    if(page == 0 && !seen)
                    {
                        getNearbyStories(0, latitude, longitude, true)
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
                                        getNearbyStories(page, latitude, longitude, seen)
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
                                            getNearbyStories(page, latitude, longitude, seen)
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

    fun getNewsStories(page: Int, seen: Boolean) {
        isLoading.postValue(true)
        feedRepositories.getNewsStories(page, seen)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<StoryModel>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<StoryModel>)
                {
                    newsStories.postValue(t)
                    isLoading.postValue(false)

                    if(page == 0 && !seen)
                    {
                        getNewsStories(0, true)
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
                                        getNewsStories(page, seen)
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
                                            getNewsStories(page, seen)
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


    fun getCommentsByPost(postKey: String, page: Int) {
        isLoading.postValue(true)
        feedRepositories.getCommentsByPost(postKey, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<CommentModel>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<CommentModel>) {
                    comments.postValue(t)
                    isLoading.postValue(false)
                }


                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getCommentsByPost(postKey, page)
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
                                            getCommentsByPost(postKey, page)
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

    fun getMyFeedFirst() {
        isLoading.postValue(true)
        feedRepositories.getNewsFeedFirst()
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
                                        getMyFeedFirst()
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
                                            getMyFeedFirst()
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

    fun getMyFeed(nextPage: String) {
        isLoading.postValue(true)
        feedRepositories.getNewsFeed(nextPage)
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
                                        getMyFeed(nextPage)
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
                                            getMyFeed(nextPage)
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

    fun getCachedMyFeedContent(city: String) {
        applicationScope.launch {
            val t = feedRepositories.getCachedMyFeed(city)
            cachedFeed.postValue(convertFeedEntitiesToFeed(t))
        }
    }

    fun deleteCachedMyFeedContent(city: String) {
        applicationScope.launch {
            feedRepositories.deleteMyCachedFeed(city)
        }
    }

    fun insertCachedMyFeedContent(feed: Feed, city: String) {
        val newData = convertFeedToFeedEntityMyFeed(feed, city)
        applicationScope.launch {
            feedRepositories.cacheFeed(newData)
        }
    }

    fun restartCachedMyFeedContentFromAPI(city: String) {
        deleteCachedNearByFeedContent(city)

        feedRepositories.getNewsFeedFirst()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Feed> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(t: Feed) {
                    insertCachedMyFeedContent(t, city)
                }
                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    //handleNetworkError(e, longitude, latitude, currentCity)
                }
                override fun onComplete() {}

            })
    }

    fun getNearbyFeedFirst(longitude: Double, latitude: Double) {
        isLoading.postValue(true)
        feedRepositories.getNearbyFeedFirst(longitude, latitude)
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
                    //handleNetworkError(e, longitude, latitude, currentCity)
                }

                override fun onComplete() {

                }

            })
    }

    fun getNearbyFeed(longitude: Double, latitude: Double, nextPage: String) {
        isLoading.postValue(true)
        feedRepositories.getNearbyFeed(longitude, latitude, nextPage)
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
                    Log.d("myFeedReceived", "error " + e.localizedMessage)
                    isLoading.postValue(false)
                }

                override fun onComplete() {

                }

            })
    }

    fun getCachedNearByFeedContent(city: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val t = feedRepositories.getCachedNearByFeed(city)
            cachedFeed.postValue(convertFeedEntitiesToFeed(t))
        }
    }

    fun deleteCachedNearByFeedContent(city: String) {
        applicationScope.launch {
            feedRepositories.deleteNearByCachedFeed(city)
        }
    }

    fun insertCachedNearByFeedContent(feed: Feed, city: String) {
        val newData = convertFeedToFeedEntity(feed, city)
        viewModelScope.launch(Dispatchers.IO) {
            feedRepositories.cacheFeed(newData)
        }
    }

    // use this function on subtle feed changes as adding/deleting posts
    fun restartCachedNearByFeedContentFromAPI(longitude: Double, latitude: Double, city: String) {
        deleteCachedNearByFeedContent(city)

        feedRepositories.getNearbyFeedFirst(longitude, latitude)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Feed> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(t: Feed) {
                    insertCachedNearByFeedContent(t, city)
                }
                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    //handleNetworkError(e, longitude, latitude, currentCity)
                }
                override fun onComplete() {}

            })
    }

    private fun convertFeedEntitiesToFeed(feedEntities: List<FeedEntity>): Feed {
        if (feedEntities.isEmpty()){
            return Feed(
                nextPage = "noCacheStored",
                content = ArrayList())
        }
        val retrievedContents = ArrayList<FeedContent>()
        for (feedEntity in feedEntities) {
            val listType = object : TypeToken<ArrayList<FeedContent>>() {}.type
            val contents = Gson().fromJson<ArrayList<FeedContent>>(feedEntity.content, listType)
            retrievedContents.addAll(contents)
        }
        return Feed(
            nextPage = feedEntities.last().nextPage,
            content = retrievedContents
        )
    }
    private fun convertFeedToFeedEntity(feed: Feed, currentCity: String): FeedEntity {
        return FeedEntity(
            nextPage = feed.nextPage,
            city = currentCity,
            content = Gson().toJson(feed.content)
        )
    }
    private fun convertFeedToFeedEntityMyFeed(feed: Feed, currentCity: String): FeedEntity {
        return FeedEntity(
            nextPage = feed.nextPage,
            city = currentCity,
            fromNearby = false,
            content = Gson().toJson(feed.content)
        )
    }
}