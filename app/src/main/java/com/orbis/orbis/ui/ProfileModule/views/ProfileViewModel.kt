package com.orbis.orbis.ui.ProfileModule.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.ErrorResponse
import com.orbis.orbis.models.IGConnectModel
import com.orbis.orbis.models.auth.PasswordUpdate
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.group.Feed
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.message.ConversationModel
import com.orbis.orbis.models.message.MessageModel
import com.orbis.orbis.models.notifications.NotificationCount
import com.orbis.orbis.models.notifications.NotificationModel
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.profile.UserPictures
import com.orbis.orbis.models.subscriptions.Subscription
import com.orbis.orbis.models.user.FollowingModel
import com.orbis.orbis.models.user.FollowingPlaceModel
import com.orbis.orbis.repositories.AuthRepositories
import com.orbis.orbis.repositories.ProfileRepositories
import com.orbis.orbis.repositories.SubscriptionRepositories
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observer
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepositories: AuthRepositories,
    private val profileRepositories: ProfileRepositories,
    private val subscriptionRepositories: SubscriptionRepositories,
) : ViewModel() {
    val myProfile: MutableLiveData<UserInfo> = MutableLiveData()
    val userPictures: MutableLiveData<ArrayList<UserPictures>> = MutableLiveData()
    val igPictures: MutableLiveData<ArrayList<UserPictures>> = MutableLiveData()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val error: MutableLiveData<String> = MutableLiveData()
    val feed: MutableLiveData<Feed> = MutableLiveData()
    val follow: MutableLiveData<Boolean> = MutableLiveData(false)
    val unFollow: MutableLiveData<Boolean> = MutableLiveData(false)
    val deleteUser: MutableLiveData<Boolean> = MutableLiveData(false)
    val deletePicture: MutableLiveData<Boolean> = MutableLiveData(false)
    val instagramLogin: MutableLiveData<IGConnectModel> = MutableLiveData()
    val userList: MutableLiveData<ArrayList<UserInfo>> = MutableLiveData()
    val groupList: MutableLiveData<ArrayList<GroupDetails>> = MutableLiveData()
    val unreadCount: MutableLiveData<NotificationCount> = MutableLiveData()
    val notifications: MutableLiveData<ArrayList<NotificationModel>> = MutableLiveData()
    var newPicture: MutableLiveData<UserPictures> = MutableLiveData()
    var mySubscriptions: MutableLiveData<List<Subscription>> = MutableLiveData()
    var cancelSubscription: MutableLiveData<Int> = MutableLiveData()
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

    fun uploadChatImage(context: Context, uri: Uri, conversationId: String, user: UserInfo) {
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
        val path = Constants.CHAT_IMAGE + imageName
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
            val db = Firebase.firestore
            val time = System.currentTimeMillis()
            val messageModel = MessageModel(
                conversationId,
                false,
                "",
                imageName,
                authRepositories.getUserKey()!!,
                time,
                "IMAGE"
            )
            val participant: ArrayList<String> = ArrayList()
            participant.add(authRepositories.getUserKey()!!)
            participant.add(user.userKey!!)
            val conversationModel1 = ConversationModel(messageModel, participant, time, null)
            db.collection("conversation").document(conversationId).set(conversationModel1)
                .addOnSuccessListener {
                    db.collection("chatMessages").add(
                        messageModel
                    ).addOnSuccessListener {
                        isLoading.postValue(false)
                    }.addOnFailureListener {
                        isLoading.postValue(false)
                        error.postValue(it.localizedMessage)
                    }
                }.addOnFailureListener {
                    isLoading.postValue(false)
                    error.postValue(it.localizedMessage)
                }


        }.addOnFailureListener {

            error.postValue(it.localizedMessage)
        }
    }

    fun uploadChatImageNew(context: Context, uri: Uri, toUser: UserInfo) {
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
        val path = Constants.CHAT_IMAGE + imageName
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
            val db = Firebase.firestore
            val id = db.collection("conversation").document().id
            val time = System.currentTimeMillis()
            val messageModel = MessageModel(
                id,
                false,
                "",
                imageName,
                authRepositories.getUserKey()!!,
                time,
                "IMAGE"
            )
            val participant: ArrayList<String> = ArrayList()
            participant.add(authRepositories.getUserKey()!!)
            participant.add(toUser.userKey!!)
            val conversationModel1 = ConversationModel(messageModel, participant, time, null)
            db.collection("conversation").document(id).set(conversationModel1)
                .addOnSuccessListener {
                    db.collection("chatMessages").add(messageModel).addOnSuccessListener {
                        conversationModel1.sender = toUser
                        conversationModelData.postValue(conversationModel1)
                        isLoading.postValue(false)
                    }
                }
            Log.d("checkNewChatId", id)


        }.addOnFailureListener {
            error.postValue(it.localizedMessage)
        }
    }

    fun uploadChatVideo(context: Context, uri: Uri, conversationId: String, user: UserInfo) {
        isLoading.postValue(true)
        val cR = context.contentResolver
        val mime = MimeTypeMap.getSingleton()
        var type = ""
        try {
            type = mime.getExtensionFromMimeType(cR.getType(uri)).toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (type.isEmpty()) {
            type = "mp4"
        }
        Log.d("typeCHeck", type + " found")
        val videoName = UUID.randomUUID().toString() + "." + type
        val path = Constants.CHAT_VIDEO + videoName
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
            val db = Firebase.firestore
            val messageModel = MessageModel(
                conversationId,
                false,
                "",
                videoName,
                authRepositories.getUserKey()!!,
                System.currentTimeMillis(),
                "VIDEO"
            )

            val participant: ArrayList<String> = ArrayList()
            participant.add(authRepositories.getUserKey()!!)
            participant.add(user.userKey!!)
            val time = System.currentTimeMillis()
            val conversationModel1 = ConversationModel(messageModel, participant, time, null)
            db.collection("conversation").document(conversationId).set(conversationModel1)
                .addOnSuccessListener {
                    db.collection("chatMessages").add(messageModel).addOnSuccessListener {
                        isLoading.postValue(false)
                    }
                }.addOnFailureListener {
                    isLoading.postValue(false)
                }

        }.addOnFailureListener {
            error.postValue(it.localizedMessage)
        }
    }

    fun uploadChatVideoNew(context: Context, uri: Uri, toUser: UserInfo) {
        isLoading.postValue(true)
        val cR = context.contentResolver
        val mime = MimeTypeMap.getSingleton()
        var type = ""
        try {
            type = mime.getExtensionFromMimeType(cR.getType(uri)).toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (type.isEmpty()) {
            type = "mp4"
        }
        Log.d("typeCHeck", type + " found")
        val videoName = UUID.randomUUID().toString() + "." + type
        val path = Constants.CHAT_VIDEO + videoName
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
            val db = Firebase.firestore
            val id = db.collection("conversation").document().id
            val time = System.currentTimeMillis()
            val messageModel = MessageModel(
                id,
                false,
                "",
                videoName,
                authRepositories.getUserKey()!!,
                time,
                "VIDEO"
            )
            val participant: ArrayList<String> = ArrayList()
            participant.add(authRepositories.getUserKey()!!)
            participant.add(toUser.userKey!!)
            val conversationModel1 = ConversationModel(messageModel, participant, time, null)
            db.collection("conversation").document(id).set(conversationModel1)
                .addOnSuccessListener {
                    db.collection("chatMessages").add(messageModel).addOnSuccessListener {
                        conversationModel1.sender = toUser
                        conversationModelData.postValue(conversationModel1)
                        isLoading.postValue(false)
                    }
                }

        }.addOnFailureListener {
            error.postValue(it.localizedMessage)
        }
    }

    fun updateFcmToken() {
        isLoading.postValue(true)
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            authRepositories.saveFcmToken(it)
            uploadTokenToServer(it)
        }.addOnFailureListener {
            isLoading.postValue(false)
            error.postValue("Couldn't Update Token")
        }

    }

    val tokenRefreshed: MutableLiveData<Boolean> = MutableLiveData(false)
    val tokenError: MutableLiveData<Boolean> = MutableLiveData(false)
    fun refreshToken() {
        if (authRepositories.isSocialLogin()) {
            if (FirebaseAuth.getInstance().currentUser == null) {
                tokenError.postValue(true)
                return
            }
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                ?.addOnSuccessListener {
                    authRepositories.saveIdToken(it.token!!)
                    tokenRefreshed.postValue(true)
                }?.addOnFailureListener {
                    tokenError.postValue(true)
                }?.addOnCanceledListener {
                    tokenError.postValue(true)
                }?.addOnCompleteListener {
                    if (!it.isSuccessful) {
                        tokenError.postValue(true)
                    }
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
                        tokenRefreshed.postValue(true)
                    }

                    override fun onError(e: Throwable) {
                        tokenError.postValue(true)

                    }

                })
        }
    }

    private fun uploadTokenToServer(token: String) {
        profileRepositories.updateFcmToken(token)
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
                                        uploadTokenToServer(token)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            uploadTokenToServer(token)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)

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

    val logout: MutableLiveData<Boolean> = MutableLiveData(false)
    fun deleteTokenToServer() {
        val token = authRepositories.getFcmToken()
        profileRepositories.deleteFcmToken(token!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    authRepositories.saveIdToken("")
                    authRepositories.saveRefreshToken("")
                    authRepositories.deleteSocialLogin()
                    authRepositories.saveUserName("")
                    authRepositories.saveUserKey("")
                    if (authRepositories.isSocialLogin()) {
                        FirebaseAuth.getInstance().signOut()

                    }
                    authRepositories.deleteSocialLogin()
                    logout.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        deleteTokenToServer()
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            deleteTokenToServer()
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun getMyProfile() {
        isLoading.postValue(true)
        profileRepositories.getMyProfile()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<UserInfo> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: UserInfo) {
                    isLoading.postValue(false)
                    Constants.userProfile = t
                    myProfile.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getMyProfile()
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getMyProfile()
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun getNotifications(page: Int) {
        isLoading.postValue(true)
        profileRepositories.getNotifications(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<NotificationModel>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<NotificationModel>) {
                    notifications.postValue(t)
                    isLoading.postValue(false)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getNotifications(page)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getNotifications(page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun getPendingFollowers(page: Int) {
        isLoading.postValue(true)
        profileRepositories.getPendingFollowers(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<UserInfo>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<UserInfo>) {
                    userList.postValue(t)
                    isLoading.postValue(false)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getPendingFollowers(page)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getPendingFollowers(page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun seenPost(keys: ArrayList<String>) {
        isLoading.postValue(true)
        profileRepositories.seenPost(keys)
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
                                        seenPost(keys)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            seenPost(keys)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    val followAccept: MutableLiveData<Boolean> = MutableLiveData(false)
    fun acceptFollow(userKey: String) {
        isLoading.postValue(true)
        profileRepositories.acceptFollow(userKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    isLoading.postValue(false)
                    followAccept.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        acceptFollow(userKey)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            acceptFollow(userKey)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun getUnreadCount() {
        isLoading.postValue(true)
        profileRepositories.getUnreadCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<NotificationCount> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: NotificationCount) {
                    unreadCount.postValue(t)
                    isLoading.postValue(false)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getUnreadCount()
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getUnreadCount()
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun updateMyProfile(userInfo: UserInfo) {
        isLoading.postValue(true)
        profileRepositories.updateProfile(userInfo)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<UserInfo> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: UserInfo) {
                    isLoading.postValue(false)
                    myProfile.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        updateMyProfile(userInfo)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            updateMyProfile(userInfo)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun getMyAdminGroups(page: Int) {
        isLoading.postValue(true)
        profileRepositories.getMyAdminGroups(page)
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
                                        getMyAdminGroups(page)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getMyAdminGroups(page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun uploadProfilePicture(
        context: Context,
        resultUri: Uri,
        selectedImage: Bitmap,
        userInfo: UserInfo
    ) {
        Log.e("ttttttdsa", "uploadProfilePicture")

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
        val path = Constants.PROFILE_PICTURES + imageName
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
                userInfo.imageName = imageName
                updateMyProfile(userInfo)
                Log.e("ttttttttt", it.toString())
                Log.d("downloadUrl", it!!.toString())
            }
        }.addOnFailureListener {
            isLoading.postValue(false)
            Log.e("tttttttttasdfasd", it.message.toString())
            error.postValue(it.localizedMessage)
        }

    }

    fun uploadUserPicture(
        context: Context,
        resultUri: Uri,
        selectedImage: Bitmap,
        userInfo: UserInfo
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
        val path = Constants.USER_PICTURES + imageName
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
                isLoading.postValue(true)
                userInfo.imageName = imageName
                val images: ArrayList<String> = ArrayList()
                images.add(imageName)
                // val pic = UserPictures(userInfo.userKey!!, images)
//                val userPicture: ArrayList<UserPictures> = ArrayList()
//                userPicture.add(pic)
//                userPictures.postValue(userPicture)
                val pictures: ArrayList<String> = ArrayList()
                pictures.add(it.toString())
                uploadUserPictureToServer(images)
            }
        }.addOnFailureListener {
            isLoading.postValue(false)
            error.postValue(it.localizedMessage)
            it.printStackTrace()
        }

    }

    private fun uploadUserPictureToServer(images: ArrayList<String>) {
        isLoading.postValue(true)
        profileRepositories.uploadUserPictures(images)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<UserPictures> {
                override fun onSubscribe(d: Disposable) {

                }


                override fun onError(e: Throwable) {

                }

                override fun onSuccess(t: UserPictures) {
                    isLoading.postValue(false)
                    newPicture.postValue(t)
                }

            })
    }

    fun getMyPictures(page: Int) {
        isLoading.postValue(true)
        profileRepositories.getMyPictures(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<UserPictures>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<UserPictures>) {
                    isLoading.postValue(false)
                    userPictures.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getMyPictures(page)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getMyPictures(page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun getUserIgPictures(page: Int, userKey: String) {
        isLoading.postValue(true)
        profileRepositories.getUserIgPictures(page, userKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<UserPictures>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<UserPictures>) {
                    isLoading.postValue(false)
                    igPictures.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getUserIgPictures(page, userKey)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getUserIgPictures(page, userKey)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
                                        }

                                    })
                            }
                        } else {
                            igPictures.postValue(ArrayList())
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

    fun getMyIgPictures(page: Int) {
        isLoading.postValue(true)
        profileRepositories.getMyIgPictures(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<UserPictures>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<UserPictures>) {
                    isLoading.postValue(false)
                    igPictures.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getMyIgPictures(page)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getMyIgPictures(page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun getUserPictures(page: Int, userKey: String) {
        isLoading.postValue(true)
        profileRepositories.getUserPictures(userKey, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<UserPictures>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: ArrayList<UserPictures>) {
                    isLoading.postValue(false)
                    userPictures.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getUserPictures(page, userKey)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getUserPictures(page, userKey)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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
        profileRepositories.getMyFeedFirst()
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
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            tokenError.postValue(true)
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

    fun getUserFeedFirst(userKey: String) {
        isLoading.postValue(true)
        profileRepositories.getUserFeedFirst(userKey)
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
                                        getUserFeedFirst(userKey)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getUserFeedFirst(userKey)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun getUserFeed(nextPage: String, userKey: String) {
        isLoading.postValue(true)
        profileRepositories.getUserFeed(userKey, nextPage)
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
                                        getUserFeed(nextPage, userKey)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getUserFeed(nextPage, userKey)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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
        profileRepositories.getMyFeed(nextPage)
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
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            tokenError.postValue(true)
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

    fun getUserProfile(userKey: String) {
        isLoading.postValue(true)
        profileRepositories.getUserProfile(userKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<UserInfo> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: UserInfo) {
                    isLoading.postValue(false)
                    myProfile.postValue(t)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getUserProfile(userKey)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getUserProfile(userKey)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    val conversationModelData: MutableLiveData<ConversationModel> = MutableLiveData()
    fun getUserProfileMessage(conversationModel: ConversationModel) {
        var userKey = ""
        if (conversationModel.participants[0] == authRepositories.getUserKey()) {
            userKey = conversationModel.participants[1]
        } else {
            userKey = conversationModel.participants[0]
        }
        isLoading.postValue(true)
        profileRepositories.getUserProfile(userKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<UserInfo> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: UserInfo) {
                    isLoading.postValue(false)
//                    conversationModel.sender = t
                    if(t.userKey != "6312003754d6c817b676806b")
                    {
                        conversationModel.sender = t
                    }
                    else
                    {
                        conversationModel.sender = UserInfo(userKey = userKey)
                    }
                    conversationModelData.postValue(conversationModel)
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getUserProfileMessage(conversationModel)
                                    }?.addOnFailureListener { tokenError.postValue(true) }
                            } else {
                                authRepositories.refreshToken()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<ResponseBody> {
                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onSuccess(t: ResponseBody) {
                                            authRepositories.saveIdToken(t.string())
                                            getUserProfileMessage(conversationModel)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            conversationModel.sender = UserInfo(userKey = userKey)
                            conversationModelData.postValue(conversationModel)
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

    fun followUser(userKey: String) {
        isLoading.postValue(true)
        profileRepositories.followUser(userKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<UserInfo> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: UserInfo) {
                    Log.d("followingDone", "done")
                    isLoading.postValue(false)
                    follow.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        followUser(userKey)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            followUser(userKey)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun unfollowUser(userKey: String) {
        isLoading.postValue(true)
        profileRepositories.unfollowUser(userKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Response<Void>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: Response<Void>) {
                    Log.d("unfollowDone", "done")
                    isLoading.postValue(false)
                    unFollow.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        unfollowUser(userKey)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            unfollowUser(userKey)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun igConnect() {
        isLoading.postValue(true)
        profileRepositories.igConnect()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<IGConnectModel> {
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
                                        igConnect()
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            igConnect()
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

                override fun onNext(t: IGConnectModel) {
                    isLoading.postValue(false)
                    instagramLogin.postValue(t)
                }

                override fun onComplete() {

                }

            })
    }

    fun getMyFollower(page: Int) {
        isLoading.postValue(true)
        profileRepositories.getMyFollowers(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<UserInfo>> {
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
                                        getMyFollower(page)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getMyFollower(page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

                override fun onNext(t: ArrayList<UserInfo>) {
                    isLoading.postValue(false)
                    userList.postValue(t)
                }

                override fun onComplete() {

                }

            })
    }

    fun searchUser(name: String, page: Int) {
        isLoading.postValue(true)
        profileRepositories.searchUser(name, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<UserInfo>> {
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
                                        searchUser(name, page)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            searchUser(name, page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

                override fun onNext(t: ArrayList<UserInfo>) {
                    isLoading.postValue(false)
                    userList.postValue(t)
                }

                override fun onComplete() {

                }

            })
    }

    fun getUserFollowers(userKey: String, page: Int) {
        isLoading.postValue(true)
        profileRepositories.getUserFollowers(userKey, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<UserInfo>> {
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
                                        getUserFollowers(userKey, page)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getUserFollowers(userKey, page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

                override fun onNext(t: ArrayList<UserInfo>) {
                    isLoading.postValue(false)
                    userList.postValue(t)
                }

                override fun onComplete() {

                }

            })
    }

    fun getUserFollowing(userKey: String, page: Int) {
        isLoading.postValue(true)
        profileRepositories.getUserFollowing(userKey, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<FollowingModel>> {
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
                                        getUserFollowing(userKey, page)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getUserFollowing(userKey, page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

                override fun onNext(t: ArrayList<FollowingModel>) {
                    isLoading.postValue(false)
                    val users: ArrayList<UserInfo> = ArrayList()
                    for (foll in t) {
                        users.add(foll.user)
                    }
                    userList.postValue(users)

                }

                override fun onComplete() {

                }

            })
    }

    fun getMyFollowing(page: Int) {
        isLoading.postValue(true)
        profileRepositories.getMyFollowing(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<FollowingModel>> {
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
                                        getMyFollower(page)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getMyFollower(page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

                override fun onNext(t: ArrayList<FollowingModel>) {
                    isLoading.postValue(false)
                    val users: ArrayList<UserInfo> = ArrayList()
                    for (foll in t) {
                        users.add(foll.user)
                    }
                    userList.postValue(users)

                }

                override fun onComplete() {

                }

            })
    }

    val placeList: MutableLiveData<ArrayList<PlaceDetails>> = MutableLiveData()
    fun getMyFollowingPlace(page: Int) {
        isLoading.postValue(true)
        profileRepositories.getMyFollowingPlace(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ArrayList<FollowingPlaceModel>> {
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
                                        getMyFollowingPlace(page)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getMyFollowingPlace(page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

                override fun onNext(t: ArrayList<FollowingPlaceModel>) {
                    isLoading.postValue(false)
                    val users: ArrayList<PlaceDetails> = ArrayList()
                    for (foll in t) {
                        users.add(foll.place)
                    }
                    placeList.postValue(users)

                }

                override fun onComplete() {

                }

            })
    }

    val reportUser: MutableLiveData<String> = MutableLiveData()
    fun reportUser(userKey: String, text: String) {
        isLoading.postValue(true)
        profileRepositories.reportUser(userKey, text)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ResponseBody> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ResponseBody) {
                    isLoading.postValue(false)
                    reportUser.postValue(t.string())
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        reportUser(userKey, text)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            reportUser(userKey, text)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    val reportPlace: MutableLiveData<String> = MutableLiveData()
    fun reportPlace(placeKey: String, text: String) {
        isLoading.postValue(true)
        profileRepositories.reportPlace(placeKey, text)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ResponseBody> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ResponseBody) {
                    isLoading.postValue(false)
                    reportPlace.postValue(t.string())
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        reportPlace(placeKey, text)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            reportPlace(placeKey, text)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    val block: MutableLiveData<String> = MutableLiveData()
    fun blockUser(userKey: String) {
        isLoading.postValue(true)
        profileRepositories.blockUser(userKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ResponseBody> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ResponseBody) {
                    isLoading.postValue(false)
                    block.postValue(t.string())
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        blockUser(userKey)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            blockUser(userKey)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    val passwordUpdated: MutableLiveData<Boolean> = MutableLiveData(false)
    fun updatePassword(passwordUpdate: PasswordUpdate) {
        isLoading.postValue(true)
        profileRepositories.updatePassword(passwordUpdate)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ResponseBody> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ResponseBody) {
                    isLoading.postValue(false)
                    passwordUpdated.postValue(true)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        updatePassword(passwordUpdate)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            updatePassword(passwordUpdate)

                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

    fun deleteMyPicture(pictureKey: String) {
        isLoading.postValue(true)
        profileRepositories.deleteUserPicture(pictureKey)
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
                                        deleteMyPicture(pictureKey)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            deleteMyPicture(pictureKey)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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
                    deletePicture.postValue(true)
                }

            })
    }

    fun deleteMyProfile() {
        isLoading.postValue(true)
        profileRepositories.deleteMyProfile()
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
                                        deleteMyProfile()
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            deleteMyProfile()
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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
                    deleteUser.postValue(true)
                }

            })
    }

    fun getMySubscriptions() {
        isLoading.postValue(true)
        subscriptionRepositories.getMySubscriptions()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<Subscription>> {
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
                                        getMySubscriptions()
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getMySubscriptions()
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

                override fun onSuccess(t: ArrayList<Subscription>) {
                    isLoading.postValue(false)
                    mySubscriptions.postValue(t)
                }
            })
    }

    fun getMyPurchases() {
        isLoading.postValue(true)
        subscriptionRepositories.getMyPurchases()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<Subscription>> {
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
                                        getMyPurchases()
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            getMyPurchases()
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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

                override fun onSuccess(t: ArrayList<Subscription>) {
                    isLoading.postValue(false)
                    mySubscriptions.postValue(t)
                }
            })
    }


    fun unsubscribeSubscription(position: Int, subscriptionKey: String) {
        isLoading.postValue(true)
        subscriptionRepositories.unsubscribeSubscription(subscriptionKey)
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
                                        unsubscribeSubscription(position, subscriptionKey)
                                    }?.addOnFailureListener {
                                        tokenError.postValue(true)
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
                                            unsubscribeSubscription(position, subscriptionKey)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
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
                        cancelSubscription.postValue(position)
                    }
                }
            })
    }
}