package com.orbis.orbis.ui.subscriptionsModule.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.orbis.orbis.models.ErrorResponse
import com.orbis.orbis.models.subscriptions.SubscribeResponse
import com.orbis.orbis.models.subscriptions.Subscription
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
import javax.inject.Inject

@HiltViewModel
class SubscriptionsListViewModel @Inject constructor(
    private val authRepositories: AuthRepositories,
    private val subscriptionRepositories: SubscriptionRepositories,
) : ViewModel() {

    var isDataLoading = false
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val tokenError: MutableLiveData<Boolean> = MutableLiveData(false)
    val error: MutableLiveData<String> = MutableLiveData()
    val subscriptionsLiveData: MutableLiveData<List<Subscription>> = MutableLiveData()
    val subscribeLiveData: MutableLiveData<SubscribeResponse> = MutableLiveData()
    val unsubscribeLiveData: MutableLiveData<Int> = MutableLiveData()
    val deleteSubscriptionLiveData: MutableLiveData<Int> = MutableLiveData()

    fun getSubscriptions(groupKey: String, page: Int) {
        isDataLoading = true
        isLoading.postValue(true)
        subscriptionRepositories.getSubscriptions(groupKey, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<Subscription>> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onError(e: Throwable) {
                    isDataLoading = false
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        if (e.code() == 401) {
                            if (authRepositories.isSocialLogin()) {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        authRepositories.saveIdToken(it.token!!)
                                        getSubscriptions(groupKey, page)
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
                                            getSubscriptions(groupKey, page)
                                        }

                                        override fun onError(e: Throwable) {
                                            tokenError.postValue(true)
                                        }

                                    })
                            }
                        } else {
                            isDataLoading = false
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
                        isDataLoading = false
                        isLoading.postValue(false)
                        error.postValue(e.localizedMessage)
                    }
                }

                override fun onSuccess(t: ArrayList<Subscription>) {
                    isDataLoading = false
                    isLoading.postValue(false)
                    subscriptionsLiveData.postValue(t)
                }
            })
    }

    fun subscribeSubscription(position: Int, subscriptionKey: String) {
        isLoading.postValue(true)
        subscriptionRepositories.subscribeSubscription(subscriptionKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<SubscribeResponse> {
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
                                        subscribeSubscription(position, subscriptionKey)
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
                                            subscribeSubscription(position, subscriptionKey)
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

                override fun onSuccess(t: SubscribeResponse) {
                    isLoading.postValue(false)
                    t.position = position
                    subscribeLiveData.postValue(t)
                }
            })
    }

    fun purchaseSubscription(position: Int, subscriptionKey: String, quantity: Int) {
        isLoading.postValue(true)
        subscriptionRepositories.purchaseSubscription(subscriptionKey, quantity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<SubscribeResponse> {
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
                                        purchaseSubscription(position, subscriptionKey, quantity)
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
                                            purchaseSubscription(position, subscriptionKey, quantity)
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

                override fun onSuccess(t: SubscribeResponse) {
                    isLoading.postValue(false)
                    t.position = position
                    subscribeLiveData.postValue(t)
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
                        unsubscribeLiveData.postValue(position)
                    }
                }
            })
    }

    fun deleteSubscription(position: Int, groupKey: String, subscriptionKey: String) {
        isLoading.postValue(true)
        subscriptionRepositories.deleteSubscription(groupKey, subscriptionKey)
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
                                        deleteSubscription(position, groupKey, subscriptionKey)
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
                                            deleteSubscription(position, groupKey, subscriptionKey)
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
                        deleteSubscriptionLiveData.postValue(position)
                    }
                }
            })
    }
}