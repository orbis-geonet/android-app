package com.orbis.orbis.ui.groupsModule.viewModel

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.orbis.orbis.models.ErrorResponse
import com.orbis.orbis.models.group.CreateGroupBody
import com.orbis.orbis.models.group.Feed
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.user.User
import com.orbis.orbis.repositories.AuthRepositories
import com.orbis.orbis.repositories.GroupRepositories
import com.orbis.orbis.repositories.SubscriptionRepositories
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
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepositories: GroupRepositories,
    private val authRepositories: AuthRepositories,
    private val subscriptionRepositories: SubscriptionRepositories,
) : ViewModel() {
    val groupDetails: MutableLiveData<GroupDetails> = MutableLiveData()
    val groupList: MutableLiveData<ArrayList<GroupDetails>> = MutableLiveData()
    val ratedGroups: MutableLiveData<ArrayList<GroupDetails>> = MutableLiveData()
    val cachedGroups: MutableLiveData<ArrayList<GroupDetails>> = MutableLiveData()
    val ownedPlaces: MutableLiveData<ArrayList<PlaceDetails>> = MutableLiveData()
    val groupMembers: MutableLiveData<ArrayList<User>> = MutableLiveData()
    val groupNMembers: MutableLiveData<Pair<GroupDetails, ArrayList<User>>> = MutableLiveData()
    val groupAdmins: MutableLiveData<ArrayList<User>> = MutableLiveData()
    val feed: MutableLiveData<Feed> = MutableLiveData()
    val error: MutableLiveData<String> = MutableLiveData()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val nextPage: MutableLiveData<String> = MutableLiveData()
    val attendEvent: MutableLiveData<Boolean> = MutableLiveData()
    val unattendEvent: MutableLiveData<Boolean> = MutableLiveData()
    val joinGroup: MutableLiveData<Boolean> = MutableLiveData()
    val leaveGroup: MutableLiveData<Boolean> = MutableLiveData()
    val blockGroup: MutableLiveData<Boolean> = MutableLiveData()
    val followGroup: MutableLiveData<Boolean> = MutableLiveData()
    val unfollowGroup: MutableLiveData<Boolean> = MutableLiveData()
    val deleteGroup: MutableLiveData<Boolean> = MutableLiveData()
    val deletePost: MutableLiveData<Boolean> = MutableLiveData()
    val addBan: MutableLiveData<Boolean> = MutableLiveData()
    val addAdmin: MutableLiveData<Boolean> = MutableLiveData()
    val adminRemoveAdmin: MutableLiveData<Boolean> = MutableLiveData()
    val sharePost: MutableLiveData<String> = MutableLiveData()
    val reportPost: MutableLiveData<String> = MutableLiveData()
    val location: MutableLiveData<Location> = MutableLiveData()
    val events: MutableLiveData<ArrayList<FeedPost>> = MutableLiveData()
    val activateSubscriptionLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val deactivateSubscriptionLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getEvents(groupKey: String, page: Int) {
        isLoading.postValue(true)
        groupRepositories.getEvents(groupKey, page)
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
                                        getEvents(groupKey, page)
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
                                            getEvents(groupKey, page)
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

    fun uploadGroupImage(
        context: Context,
        resultUri: Uri,
        selectedImage: Bitmap,
        createGroupBody: CreateGroupBody,
        isUpdate: Boolean = false,
        groupKey: String = ""
    ) {
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
        val path = "groupPictures/$imageName"
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
                createGroupBody.imageName = imageName
                if (isUpdate) {
                    updateGroup(createGroupBody, groupKey)
                } else {
                    createGroup(createGroupBody)
                }
                Log.d("downloadUrl", it!!.toString())
            }
        }.addOnFailureListener {
            isLoading.postValue(false)
            error.postValue(it.localizedMessage)
        }

    }

    private fun createGroup(createGroupBody: CreateGroupBody) {
        groupRepositories.createGroup(createGroupBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<GroupDetails> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: GroupDetails) {
                    groupDetails.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        createGroup(createGroupBody)
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
                                            createGroup(createGroupBody)
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

    fun getGroupsNoLocation(query: String) {
        isLoading.postValue(true)
        groupRepositories.getGroups(0.0, 0.0, query)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<GroupDetails>> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(t: ArrayList<GroupDetails>) {
                    isLoading.postValue(false)
                    groupList.postValue(t)
                }
                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    error.postValue(e.localizedMessage)
                }
                override fun onComplete() {}
            })
    }


    fun updateGroup(createGroupBody: CreateGroupBody, groupKey: String) {
        groupRepositories.updateGroup(createGroupBody, groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<GroupDetails> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: GroupDetails) {
                    groupDetails.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        updateGroup(createGroupBody, groupKey)
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
                                            updateGroup(createGroupBody, groupKey)
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

    fun getGroups(latitude: Double, longitude: Double, name: String, page: Int = 0) {
        isLoading.postValue(true)
        groupRepositories.getGroups(latitude, longitude, name, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<GroupDetails>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<GroupDetails>) {
                    isLoading.postValue(false)
                    groupList.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getGroups(latitude, longitude, name, page)
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
                                            getGroups(latitude, longitude, name, page)
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

    fun getGroupsWithoutLocation(page: Int = 0) {
        isLoading.postValue(true)
        groupRepositories.getGroupsWithoutLocation(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<GroupDetails>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<GroupDetails>) {
                    isLoading.postValue(false)
                    groupList.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getGroupsWithoutLocation(page)
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
                                            getGroupsWithoutLocation(page)
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

    fun getUserGroups(userKey: String, page: Int = 0) {
        isLoading.postValue(true)
        groupRepositories.getUserGroups(userKey, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<GroupDetails>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<GroupDetails>) {
                    isLoading.postValue(false)
                    groupList.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getUserGroups(userKey,  page)
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
                                            getUserGroups(userKey, page)
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

    fun getRatedGroups(latitude: Double, longitude: Double, page: Int = 0) {
        isLoading.postValue(true)
        groupRepositories.getRatedGroups(latitude, longitude, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<GroupDetails>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<GroupDetails>) {
                    isLoading.postValue(false)
                    ratedGroups.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getRatedGroups(latitude, longitude)
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
                                            getRatedGroups(latitude, longitude)
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

    fun getGroupFeedFirst(groupKey: String) {
        isLoading.postValue(true)

        groupRepositories.getGroupFeedFirst(groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Feed> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: Feed) {
                    nextPage.postValue(t.nextPage)
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
                                        getGroupFeedFirst(groupKey)
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
                                            getGroupFeedFirst(groupKey)
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

    fun getGroupFeed(
        groupKey: String,
        next: String
    ) {
        isLoading.postValue(true)

        groupRepositories.getGroupFeed(groupKey, next)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Feed> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: Feed) {
                    nextPage.postValue(t.nextPage)
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
                                        getGroupFeed(groupKey, next)
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
                                            getGroupFeed(groupKey, next)
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

    fun likePost(postKey: String) {
        isLoading.postValue(true)
        groupRepositories.likePost(postKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<FeedPost> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: FeedPost) {
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
                                        likePost(postKey)
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
                                            likePost(postKey)
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

    fun unlikePost(postKey: String) {
        isLoading.postValue(true)
        groupRepositories.unlikePost(postKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<FeedPost> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: FeedPost) {
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
                                        likePost(postKey)
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
                                            likePost(postKey)
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

    fun getGroupByKey(groupKey: String) {
        isLoading.postValue(true)
        groupRepositories.getGroupByKey(groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<GroupDetails> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: GroupDetails) {
                    isLoading.postValue(false)
                    groupDetails.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getGroupByKey(groupKey)
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
                                            getGroupByKey(groupKey)
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

    fun getGroupAndMembersByKey(groupKey: String) {
        isLoading.postValue(true)
        groupRepositories.getGroupByKey(groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<GroupDetails> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: GroupDetails) {
                    isLoading.postValue(false)
                    val group = t
                    groupRepositories.getGroupMembers(groupKey, 0)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : Observer<ArrayList<User>> {
                            override fun onSubscribe(d: Disposable) {

                            }

                            override fun onNext(t1: ArrayList<User>) {
                                isLoading.postValue(false)
                                val members = t1
                                groupNMembers.postValue(group to members)
                            }

                            override fun onError(e: Throwable) {
                                isLoading.postValue(false)
                                if (e is HttpException) {
                                    if (e.code() == 401) {
                                        if (authRepositories.isSocialLogin()) {
                                            FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                                ?.addOnSuccessListener {
                                                    authRepositories.saveIdToken(it.token!!)
                                                    getGroupMembers(groupKey, 0)
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
                                                        getGroupMembers(groupKey, 0)
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

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getGroupByKey(groupKey)
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
                                            getGroupByKey(groupKey)
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

    fun getGroupMembers(groupKey: String, page: Int) {
        isLoading.postValue(true)
        groupRepositories.getGroupMembers(groupKey, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<User>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<User>) {
                    isLoading.postValue(false)
                    groupMembers.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getGroupMembers(groupKey, page)
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
                                            getGroupMembers(groupKey, page)
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

    fun getGroupAdmins(groupKey: String) {
        isLoading.postValue(true)
        groupRepositories.getGroupAdmins(groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<User>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<User>) {
                    isLoading.postValue(false)
                    groupAdmins.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getGroupAdmins(groupKey)
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
                                            getGroupAdmins(groupKey)
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

    fun attendEvent(postKey: String) {
        isLoading.postValue(true)
        groupRepositories.attendEvent(postKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    Log.d("AttendApi", "success")
                    isLoading.postValue(false)
                    attendEvent.postValue(true)
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        Log.d("AttendApi", "Failed " + e.code())
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        attendEvent(postKey)
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
                                            attendEvent(postKey)
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

    fun unattendEvent(postKey: String) {
        isLoading.postValue(true)
        groupRepositories.unattendEvent(postKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    unattendEvent.postValue(true)
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        unattendEvent(postKey)
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
                                            unattendEvent(postKey)
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

    fun getPlaceOwnedByGroup(groupKey: String, latitude: Double, longitude: Double, page: Int) {
        isLoading.postValue(true)
        groupRepositories.getPlaceOwnedByGroup(groupKey, latitude, longitude, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<PlaceDetails>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<PlaceDetails>) {
                    isLoading.postValue(false)
                    ownedPlaces.postValue(t)
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getPlaceOwnedByGroup(groupKey, latitude, longitude, page)
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
                                            getPlaceOwnedByGroup(
                                                groupKey,
                                                latitude,
                                                longitude,
                                                page
                                            )

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

    fun joinGroup(groupKey: String) {
        isLoading.postValue(true)
        groupRepositories.joinGroup(groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    joinGroup.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        joinGroup(groupKey)
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
                                            joinGroup(groupKey)
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

    fun leaveGroup(groupKey: String) {
        isLoading.postValue(true)
        groupRepositories.leaveGroup(groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    leaveGroup.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        leaveGroup(groupKey)
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
                                            leaveGroup(groupKey)
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

    fun blockGroup(groupKey: String)
    {
        isLoading.postValue(true)
        groupRepositories.blockGroup(groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    blockGroup.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        leaveGroup(groupKey)
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
                                            leaveGroup(groupKey)
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

    fun unBlockGroup(groupKey: String)
    {
        isLoading.postValue(true)
        groupRepositories.unBlockGroup(groupKey)
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
                                        leaveGroup(groupKey)
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
                                            leaveGroup(groupKey)
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


    fun followGroup(groupKey: String) {
        isLoading.postValue(true)
        groupRepositories.followGroup(groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    followGroup.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        followGroup(groupKey)
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
                                            followGroup(groupKey)
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

    fun unfollowGroup(groupKey: String) {
        isLoading.postValue(true)
        groupRepositories.unfollowGroup(groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    unfollowGroup.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        unfollowGroup(groupKey)
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
                                            unfollowGroup(groupKey)
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

    fun deleteGroup(groupKey: String) {
        isLoading.postValue(true)
        groupRepositories.deleteGroup(groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    deleteGroup.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        deleteGroup(groupKey)
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
                                            deleteGroup(groupKey)
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

    fun sharePost(postKey: String, type: String) {
        isLoading.postValue(true)
        groupRepositories.sharePost(postKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ResponseBody> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ResponseBody) {
                    isLoading.postValue(false)

                    sharePost.postValue(t.string() + "?key=${postKey}____${type}")
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        sharePost(postKey,type)
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
                                            sharePost(postKey,type)
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

    fun deletePost(postKey: String) {
        isLoading.postValue(true)
        groupRepositories.deletePost(postKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    deletePost.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        deletePost(postKey)
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
                                            deletePost(postKey)
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


    fun reportPost(postKey: String, text: String) {
        isLoading.postValue(true)
        groupRepositories.reportPost(postKey, text)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ResponseBody> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ResponseBody) {
                    isLoading.postValue(false)
                    reportPost.postValue(t.string())
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        reportPost(postKey, text)
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
                                            reportPost(postKey, text)
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

    fun reportGroup(groupKey: String, text: String) {
        isLoading.postValue(true)
        groupRepositories.reportGroup(groupKey, text)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ResponseBody> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ResponseBody) {
                    isLoading.postValue(false)
                    reportPost.postValue(t.string())
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        reportGroup(groupKey, text)
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
                                            reportGroup(groupKey, text)
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

    fun addBan(groupKey: String, userKey: String) {
        isLoading.postValue(true)
        groupRepositories.addBan(groupKey, userKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    addBan.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        addBan(groupKey, userKey)
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
                                            addBan(groupKey, userKey)
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

    fun addAdmin(groupKey: String, userKey: String) {
        isLoading.postValue(true)
        groupRepositories.addAdmin(groupKey, userKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    addAdmin.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        addAdmin(groupKey, userKey)
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
                                            addAdmin(groupKey, userKey)
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

    fun adminRemoveAdmin(groupKey: String, userKey: String) {
        isLoading.postValue(true)
        groupRepositories.addAdmin(groupKey, userKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    adminRemoveAdmin.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        adminRemoveAdmin(groupKey, userKey)
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
                                            adminRemoveAdmin(groupKey, userKey)
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

    fun activateSubscription(groupKey: String) {
        isLoading.postValue(true)
        subscriptionRepositories.activateSubscription(groupKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        activateSubscription(groupKey)
                                    }?.addOnFailureListener {

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
                                            activateSubscription(groupKey)
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

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    if (t.isSuccessful) {
                        activateSubscriptionLiveData.postValue(true)
                    }
                }
            })
    }

    fun deactivateSubscription(groupKey: String, sure: Boolean) {
        isLoading.postValue(true)
        subscriptionRepositories.deactivateSubscription(groupKey, sure)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        deactivateSubscription(groupKey, sure)
                                    }?.addOnFailureListener {

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
                                            deactivateSubscription(groupKey, sure)
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

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    if (t.isSuccessful) {
                        deactivateSubscriptionLiveData.postValue(true)
                    }
                }
            })
    }

    fun getCachedGroups(city: String, userLoggedIn: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val groups = groupRepositories.getCachedGroups(city, userLoggedIn)
            cachedGroups.postValue(ArrayList(groups))
        }
    }

    fun cacheGroups(groups: ArrayList<GroupDetails>, city: String, loggedIn: Boolean) {
        applicationScope.launch {
            groupRepositories.cacheGroups(groups.toList(), city, loggedIn)
        }
    }

    fun deleteCachedGroups(city: String, loggedIn: Boolean) {
        applicationScope.launch {
            groupRepositories.deleteGroupsFromCache(city, loggedIn)
        }
    }
}