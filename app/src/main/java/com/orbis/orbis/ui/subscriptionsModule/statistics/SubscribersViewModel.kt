package com.orbis.orbis.ui.subscriptionsModule.statistics

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.orbis.orbis.models.ErrorResponse
import com.orbis.orbis.models.user.User
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
class SubscribersViewModel @Inject constructor(
    private val authRepositories: AuthRepositories,
    private val subscriptionRepositories: SubscriptionRepositories,
) : ViewModel() {

    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val tokenError: MutableLiveData<Boolean> = MutableLiveData(false)
    val error: MutableLiveData<String> = MutableLiveData()
    val membersLiveData: MutableLiveData<List<User>> = MutableLiveData()

    fun getSubscribers(groupKey: String, subscriptionKey: String, page: Int) {
        isLoading.postValue(true)
        subscriptionRepositories.getSubscribers(groupKey, subscriptionKey, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<User>> {
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
                                        getSubscribers(groupKey, subscriptionKey, page)
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
                                            getSubscribers(groupKey, subscriptionKey, page)
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

                override fun onSuccess(t: ArrayList<User>) {
                    isLoading.postValue(false)
                    membersLiveData.postValue(t)
                }
            })
    }

    fun getPurchases(groupKey: String, purchaseKey: String, page: Int) {
        isLoading.postValue(true)
        subscriptionRepositories.getPurchases(groupKey, purchaseKey, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<User>> {
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
                                        getPurchases(groupKey, purchaseKey, page)
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
                                            getPurchases(groupKey, purchaseKey, page)
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

                override fun onSuccess(t: ArrayList<User>) {
                    isLoading.postValue(false)
                    membersLiveData.postValue(t)
                }
            })
    }
}