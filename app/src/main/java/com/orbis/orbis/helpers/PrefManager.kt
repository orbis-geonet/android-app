package com.orbis.orbis.helpers

import android.content.Context
import android.content.SharedPreferences
import com.orbis.orbis.models.Constants
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class PrefManager {
    var mPrefs: SharedPreferences
    private val prefName = "orbis"
    private val idTokenKey = "idToken"
    private val userKey = "userKey"
    private val refreshTokenKey = "refreshToken"
    private val refreshEmailKey = "email"
    private val firstLaunch = "firstLaunch"
    private val isSocialLogin = "isSocialLogin"
    private val userName = "userName"
    private val languageKey = "langKey"
    private val unitKey = "unitKey"
    private val notiKey = "notiKey"
    private val fcmKey = "fcmKey"
    private val countryName = "countryName"
    private val partnerKey = "partnerKey"

    @Inject
    constructor(context: Context) {
        mPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
    }

    fun saveFirstLaunch() {
        mPrefs.edit().putBoolean(firstLaunch, false).apply()
    }

    fun isFirstLaunch(): Boolean {
        return mPrefs.getBoolean(firstLaunch, true);
    }

    fun saveIdToken(token: String) {
        mPrefs.edit().putString(idTokenKey, token).apply()
    }

    fun getIdToken(): String? {
        return mPrefs.getString(idTokenKey, "")
    }

    fun saveUserName(name: String) {
        mPrefs.edit().putString(userName, name).apply()
    }

    fun getUserName(): String? {
        return mPrefs.getString(userName, "")
    }

    fun saveUserKey(key: String) {
        mPrefs.edit().putString(userKey, key).apply()
    }

    fun getUserKey(): String? {
        return mPrefs.getString(userKey, "")
    }

    fun saveRefreshToken(token: String) {
        mPrefs.edit().putString(refreshTokenKey, token).apply()
    }
    fun saveEmail(email: String) {
        mPrefs.edit().putString(refreshEmailKey, email).apply()
    }

    fun getRefreshToken(): String? {
        return mPrefs.getString(refreshTokenKey, "")
    }

    fun getRefreshEmail(): String? {
        return mPrefs.getString(refreshEmailKey, "")
    }

    fun saveSocialLogin() {
        mPrefs.edit().putBoolean(isSocialLogin, true).apply()
    }

    fun deleteSocialLogin() {
        mPrefs.edit().putBoolean(isSocialLogin, false).apply()
    }

    fun isSocialLogin(): Boolean {
        return mPrefs.getBoolean(isSocialLogin, false)
    }

    fun saveLanguage(language: String) {
        mPrefs.edit().putString(languageKey, language).apply()
    }

    fun getLanguage(): String? {
        return mPrefs.getString(languageKey, "dd")
    }

    fun saveUnit(unit: String) {
        mPrefs.edit().putString(unitKey, unit).apply()
    }

    fun getUnit(): String? {
        return mPrefs.getString(unitKey, "Miles")
    }

    fun saveNotification(isEnable: Boolean) {
        mPrefs.edit { putBoolean(notiKey, isEnable) }
    }

    fun getNotification(): Boolean {
        return mPrefs.getBoolean(notiKey, true)
    }

    fun saveFcm(token: String) {
        mPrefs.edit().putString(fcmKey, token).apply()
    }

    fun getFcm(): String? {
        return mPrefs.getString(fcmKey, "")
    }

    fun getCountryName() {
//        Constants.currentCountryName = mPrefs.getString(countryName, null)
    }

    fun saveCountryName(name: String) {
        mPrefs.edit().putString(countryName, name).apply()
    }

    fun initPartnerKey() {
        Constants.partnerKey = mPrefs.getString(partnerKey, null)
    }

    fun savePartnerKey(key: String?) {
        Constants.partnerKey = key
        mPrefs.edit().putString(partnerKey, key).apply()
    }
}