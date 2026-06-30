package com.orbis.orbis.ui.authModule.viewModel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.lassi.domain.common.SingleLiveEvent
import com.orbis.orbis.models.ErrorResponse
import com.orbis.orbis.models.auth.*
import com.orbis.orbis.repositories.AuthRepositories
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val authRepositories: AuthRepositories) :
    ViewModel() {
    val authError: SingleLiveEvent<String> = SingleLiveEvent()
    val userProfile: SingleLiveEvent<UserProfile> = SingleLiveEvent()
    val errorResponse: SingleLiveEvent<ErrorResponse> = SingleLiveEvent()
    val pendingEmail: SingleLiveEvent<String> = SingleLiveEvent()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    fun saveUserInfoToLocal(userProfile: UserProfile) {
        authRepositories.saveUserName(userProfile.displayName)
        authRepositories.saveUserKey(userProfile.userKey)
        authRepositories.saveEmail(userProfile.email)
    }

    fun login(loginBody: LoginBody?) {
        isLoading.postValue(true)
        authRepositories.login(loginBody!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<UserProfile> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: UserProfile) {
                    isLoading.postValue(false)

                    userProfile.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        try {
                            val body = e.response()?.errorBody()
                            val gson = Gson()
                            val adapter: TypeAdapter<ErrorResponse> =
                                gson.getAdapter(ErrorResponse::class.java)
                            val errorBody = adapter.fromJson(body?.string())
                            errorResponse.postValue(errorBody)
                        } catch (e: Exception) {

                        }
                    }
                }

            })
    }

    fun signup(signUpBody: SignUpBody?) {
        isLoading.postValue(true)
        authRepositories.signup(signUpBody!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<UserProfile> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: UserProfile) {
                    isLoading.postValue(false)
                    userProfile.postValue(t)
                    authRepositories.savePartnerKey(null)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        try {
                            val body = e.response()?.errorBody()
                            val gson = Gson()
                            val adapter: TypeAdapter<ErrorResponse> =
                                gson.getAdapter(ErrorResponse::class.java)
                            val errorBody = adapter.fromJson(body?.string())
                            errorResponse.postValue(errorBody)
                        } catch (e: Exception) {

                        }
                    }
                }

            })
    }

    fun updateProfile(token: String, profileUpdateBody: ProfileUpdateBody, socialLogin: Boolean) {
        isLoading.postValue(true)
        authRepositories.updateSocialProfile("Bearer $token", profileUpdateBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<UserProfile> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: UserProfile) {
                    isLoading.postValue(false)
                    authRepositories.saveIdToken(token)
                    authRepositories.saveUserKey(t.userKey)
                    if (socialLogin) {
                        authRepositories.saveSocialLogin()
                        authRepositories.savePartnerKey(null)
                    }
                    saveUserInfoToLocal(t)
                    userProfile.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        try {
                            val body = e.response()?.errorBody()
                            val gson = Gson()
                            val adapter: TypeAdapter<ErrorResponse> =
                                gson.getAdapter(ErrorResponse::class.java)
                            val errorBody = adapter.fromJson(body?.string())
                            errorResponse.postValue(errorBody)
                        } catch (e: Exception) {

                        }
                    }
                }

            })

    }
    fun getProfileSocialSignIn(token: String) {
        isLoading.postValue(true)
        authRepositories.getMySocialProfile("Bearer $token")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<UserProfile> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: UserProfile) {
                    isLoading.postValue(false)
                    authRepositories.saveIdToken(token)
                    authRepositories.saveUserKey(t.userKey)
                    authRepositories.saveSocialLogin()
                    authRepositories.savePartnerKey(null)
                    saveUserInfoToLocal(t)
                    userProfile.postValue(t)
                }

                override fun onError(e: Throwable) {
                    isLoading.postValue(false)
                    if (e is HttpException) {
                        try {
                            val body = e.response()?.errorBody()
                            val gson = Gson()
                            val adapter: TypeAdapter<ErrorResponse> =
                                gson.getAdapter(ErrorResponse::class.java)
                            val errorBody = adapter.fromJson(body?.string())
                            errorResponse.postValue(errorBody)
                        } catch (e: Exception) {

                        }
                    }
                }

            })

    }

    fun signInWithEmail(email: String, password: String) {
        isLoading.postValue(true)
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    user?.getIdToken(true)?.addOnCompleteListener { tok ->
                        if (tok.isSuccessful) {
                            getProfileSocialSignIn(tok.result?.token!!)
                        } else {
                            isLoading.postValue(false)
                        }
                    }
                } else {
                    isLoading.postValue(false)
                    authError.postValue("wrong_password")
                }
            }
    }

    fun googleSignIn(task: Task<GoogleSignInAccount>, activity: Activity) {
        try {

            val account = task.getResult(ApiException::class.java)!!
            Log.d("emailGoogle", account.email!! + " called")

            FirebaseAuth.getInstance().fetchSignInMethodsForEmail(account.email!!)
                .addOnCompleteListener {
                    isLoading.postValue(true)
                    var isNewUser = true
                    if (it.isSuccessful) {
                        for (method in it.result!!.signInMethods!!) {
                            if (!method.contains("google")) {
                                isNewUser = false
                                pendingEmail.postValue(account.email)  // store it for the dialog
                                authError.postValue(method)
                                Log.d("methodFound", method)
                                break
                            }
                        }
                        if (isNewUser) {
                            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                            FirebaseAuth.getInstance().signInWithCredential(credential)
                                .addOnCompleteListener(activity) { task ->
                                    if (task.isSuccessful) {
                                        val isNewFirebaseUser = task.result?.additionalUserInfo?.isNewUser ?: false
                                        val user = FirebaseAuth.getInstance().currentUser
                                        val profileUpdateBody = ProfileUpdateBody(
                                            user?.displayName!!,
                                            user.photoUrl.toString()
                                        )
                                        user.getIdToken(true).addOnCompleteListener { tok ->
                                            if (tok.isSuccessful) {
                                                if (isNewFirebaseUser){
                                                    Log.d("emailGoogle", "new user")
                                                    updateProfile(
                                                        tok.result?.token!!,
                                                        profileUpdateBody, true
                                                    )
                                                } else{
                                                    Log.d("emailGoogle", "existing user")
                                                    getProfileSocialSignIn(tok.result?.token!!)
                                                }

                                            } else {
                                                isLoading.postValue(false)
                                            }
                                        }
                                    } else {
                                        isLoading.postValue(false)
                                    }
                                }
                        } else {
                            isLoading.postValue(false)
                        }
                    } else {
                        isLoading.postValue(false)
                    }
                }

        } catch (e: ApiException) {
            // Google Sign In failed, update UI appropriately
            Log.w("TAG", "Google sign in failed " + e.localizedMessage, e)
        }
    }

    fun saveIdToken(token: String) {
        authRepositories.saveIdToken(token)
    }

    fun saveUserKey(key: String) {
        authRepositories.saveUserKey(key)
    }

    fun getUserKey(): String? {
        return authRepositories.getUserKey()
    }

    fun getIdToken(): String? {
        return authRepositories.getIdToken()
    }

    fun saveRefreshToken(token: String) {
        authRepositories.saveRefreshToken(token)
    }

    fun saveRefreshEmail(email: String) {
        authRepositories.saveEmail(email)
    }

    fun saveFcm(token: String) {
        authRepositories.saveFcmToken(token)
    }

    fun getFcm(): String? {
        return authRepositories.getFcmToken()
    }

    fun getEmail(): String? {
        return authRepositories.getRefreshEmail()
    }

    private fun uploadFcmTokenIfNeeded() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModelScope.launch {
                try {
                    authRepositories.saveFcmToken(token)
                    Log.d("NOTIF_DEBUG", "FCM token uploaded post-login")
                } catch (e: Exception) {
                    Log.e("NOTIF_DEBUG", "FCM token upload failed", e)
                }
            }
        }
    }


}