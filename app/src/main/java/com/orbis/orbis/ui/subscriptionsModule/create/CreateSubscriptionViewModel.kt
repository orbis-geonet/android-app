package com.orbis.orbis.ui.subscriptionsModule.create

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
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.orbis.orbis.models.ErrorResponse
import com.orbis.orbis.models.subscriptions.*
import com.orbis.orbis.repositories.AuthRepositories
import com.orbis.orbis.repositories.SubscriptionRepositories
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CreateSubscriptionViewModel @Inject constructor(
    private val authRepositories: AuthRepositories,
    private val subscriptionRepositories: SubscriptionRepositories,
) : ViewModel() {

    val isLoading: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    val error: MutableLiveData<String> = MutableLiveData()
    val stripeCreatedLiveData: MutableLiveData<CreateStripeResponse> = MutableLiveData()
    val stripeStatusLiveData: MutableLiveData<String> = MutableLiveData()
    val subscriptionInfoLiveData: MutableLiveData<SubscriptionInfo> = MutableLiveData()
    val subscriptionCreatedLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val imageUploadedLiveData: MutableLiveData<Boolean> = MutableLiveData()

    fun createStripe(requestBody: CreateStripeBody) {
        isLoading.postValue(true)
        subscriptionRepositories.createStripe(requestBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<CreateStripeResponse> {
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
                                        createStripe(requestBody)
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
                                            createStripe(requestBody)
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

                override fun onSuccess(t: CreateStripeResponse) {
                    isLoading.postValue(false)
                    stripeCreatedLiveData.postValue(t)
                }
            })
    }

    fun getStripe() {
        isLoading.postValue(true)
        subscriptionRepositories.getStripe()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<GetStripeResponse> {
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
                                        getStripe()
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
                                            getStripe()
                                        }

                                        override fun onError(e: Throwable) {

                                        }

                                    })
                            }
                        } else {
                            isLoading.postValue(false)
                            stripeStatusLiveData.postValue("")
                        }
                    } else {
                        isLoading.postValue(false)
                    }
                }

                override fun onSuccess(t: GetStripeResponse) {
                    isLoading.postValue(false)
                    stripeStatusLiveData.postValue(t.status)
                }
            })
    }

    fun updateStripe() {
        isLoading.postValue(true)
        subscriptionRepositories.updateStripe()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<CreateStripeResponse> {
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
                                        updateStripe()
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
                                            updateStripe()
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

                override fun onSuccess(t: CreateStripeResponse) {
                    isLoading.postValue(false)
                    stripeCreatedLiveData.postValue(t)
                }
            })
    }

    fun getSubscriptionInfo() {
        isLoading.postValue(true)
        subscriptionRepositories.getSubscriptionInfo()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<SubscriptionInfo> {
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
                                        getSubscriptionInfo()
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
                                            getSubscriptionInfo()
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

                override fun onSuccess(t: SubscriptionInfo) {
                    isLoading.postValue(false)
                    subscriptionInfoLiveData.postValue(t)
                }
            })
    }

    var requestBody: CreateSubscriptionBody? = null
    fun uploadSubscriptionImage(context: Context, uris: ArrayList<Uri>, position: Int)
    {
        if (position == (uris.size))
        {
            isLoading.postValue(false)
            imageUploadedLiveData.postValue(true)
        }
        else
        {
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
            val path = "groups/subscription/images/$imageName"
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
                requestBody?.imagesName?.add(imageName)
                uploadSubscriptionImage(context, uris, position + 1)
            }.addOnFailureListener {
                uploadSubscriptionImage(context, uris, position + 1)
                error.postValue(it.localizedMessage)
            }
        }
    }

    fun createSubscription(groupKey: String, requestBody: CreateSubscriptionBody) {
        isLoading.postValue(true)
        subscriptionRepositories.createSubscription(groupKey, requestBody)
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
                                        createSubscription(groupKey, requestBody)
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
                                            createSubscription(groupKey, requestBody)
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
                        subscriptionCreatedLiveData.postValue(true)
                    }
                }
            })
    }

    fun editSubscription(groupKey: String, requestBody: CreateSubscriptionBody) {
        isLoading.postValue(true)
        subscriptionRepositories.editSubscription(groupKey, requestBody)
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
                                        editSubscription(groupKey, requestBody)
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
                                            editSubscription(groupKey, requestBody)
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
                        subscriptionCreatedLiveData.postValue(true)
                    }
                }
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
}