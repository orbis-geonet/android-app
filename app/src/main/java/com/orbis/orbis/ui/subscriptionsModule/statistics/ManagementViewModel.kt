package com.orbis.orbis.ui.subscriptionsModule.statistics

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.orbis.orbis.models.ErrorResponse
import com.orbis.orbis.models.subscriptions.Subscription
import com.orbis.orbis.models.subscriptions.SubscriptionStatistic
import com.orbis.orbis.repositories.AuthRepositories
import com.orbis.orbis.repositories.SubscriptionRepositories
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class ManagementViewModel @Inject constructor(
    private val authRepositories: AuthRepositories,
    private val subscriptionRepositories: SubscriptionRepositories,
) : ViewModel() {

    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val tokenError: MutableLiveData<Boolean> = MutableLiveData(false)
    val error: MutableLiveData<String> = MutableLiveData()
    val subscriptionsLiveData: MutableLiveData<List<Subscription>> = MutableLiveData()
    val statisticLiveData: MutableLiveData<SubscriptionStatistic> = MutableLiveData()

    fun getSubscriptions(groupKey: String, page: Int) {
        isLoading.postValue(true)
        subscriptionRepositories.getSubscriptions(groupKey, page)
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
                    subscriptionsLiveData.postValue(t)
                }
            })
    }

    fun getSubscriptionStatistic(groupKey: String, subscriptionKey: String, type: String) {
        isLoading.postValue(true)
        subscriptionRepositories.getSubscriptionStatistic(groupKey, subscriptionKey, type)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<SubscriptionStatistic> {
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
                                        getSubscriptionStatistic(groupKey, subscriptionKey, type)
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
                                            getSubscriptionStatistic(
                                                groupKey,
                                                subscriptionKey,
                                                type
                                            )
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

                override fun onSuccess(t: SubscriptionStatistic) {
                    isLoading.postValue(false)
                    statisticLiveData.postValue(t)
                }
            })
    }

    fun getPurchaseKeyStatistic(groupKey: String, subscriptionKey: String, type: String) {
        isLoading.postValue(true)
        subscriptionRepositories.getPurchaseKeyStatistic(groupKey, subscriptionKey, type)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<SubscriptionStatistic> {
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
                                        getPurchaseKeyStatistic(groupKey, subscriptionKey, type)
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
                                            getPurchaseKeyStatistic(
                                                groupKey,
                                                subscriptionKey,
                                                type
                                            )
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

                override fun onSuccess(t: SubscriptionStatistic) {
                    isLoading.postValue(false)
                    statisticLiveData.postValue(t)
                }
            })
    }

}