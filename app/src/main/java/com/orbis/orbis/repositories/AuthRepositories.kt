package com.orbis.orbis.repositories

import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.auth.LoginBody
import com.orbis.orbis.models.auth.ProfileUpdateBody
import com.orbis.orbis.models.auth.SignUpBody
import com.orbis.orbis.models.auth.UserProfile
import com.orbis.orbis.network.ApiInterface
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import javax.inject.Inject

class AuthRepositories @Inject constructor(
    private val apiInterface: ApiInterface,
    private val prefManager: PrefManager
) {
    fun login(loginBody: LoginBody): Single<UserProfile> {
        return apiInterface.login(loginBody)
    }

    fun signup(signUpBody: SignUpBody): Single<UserProfile> {
        return apiInterface.signup(signUpBody)
    }

    fun updateSocialProfile(
        token: String,
        profileUpdateBody: ProfileUpdateBody
    ): Single<UserProfile> {
        return apiInterface.updateProfile(token, profileUpdateBody)
    }

    fun getMySocialProfile(
        token: String
    ): Single<UserProfile> {
        return apiInterface.getMySocialProfile(token)
    }

    fun refreshToken(): Single<ResponseBody> {
        return apiInterface.refreshToken(getRefreshToken()!!.toRequestBody("text/plain".toMediaTypeOrNull()))
    }


    fun saveIdToken(token: String) {
        prefManager.saveIdToken(token)
    }

    fun getIdToken(): String? {
        return prefManager.getIdToken()
    }

    fun saveUserKey(key: String) {
        prefManager.saveUserKey(key)
    }

    fun getUserKey(): String? {
        return prefManager.getUserKey()
    }

    fun saveRefreshToken(token: String) {
        prefManager.saveRefreshToken(token)
    }

    fun saveEmail(email: String) {
        prefManager.saveEmail(email)
    }

    fun getRefreshToken(): String? {
        return prefManager.getRefreshToken()
    }

    fun getRefreshEmail(): String? {
        return prefManager.getRefreshEmail()
    }

    fun saveSocialLogin() {
        prefManager.saveSocialLogin()
    }

    fun deleteSocialLogin() {
        prefManager.saveSocialLogin()
    }

    fun isSocialLogin(): Boolean {
        return prefManager.isSocialLogin()
    }

    fun saveFcmToken(token: String) {
        prefManager.saveFcm(token)
    }

    fun getFcmToken(): String? {
        return prefManager.getFcm()
    }

    fun saveUserName(name: String) {
        prefManager.saveUserName(name)
    }

    fun getUserName(): String? {
        return prefManager.getUserName()
    }

    fun savePartnerKey(key: String?) {
        return prefManager.savePartnerKey(key)
    }
}