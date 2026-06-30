package com.orbis.orbis.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.models.auth.UserInfo
import java.lang.reflect.Type

class SharedPrefsManager : UserSession {
    private var prefs: SharedPreferences? = null
    private var userPrefs: SharedPreferences? = null

    override var email: String?
        get() = userPrefs!!.getString(User.EMAIL, null)
        set(email) {
            userPrefs!!.edit().putString(User.EMAIL, email).apply()
        }


    override var accessToken: String?
        get() = userPrefs!!.getString(User.ACCESS_TOKEN, null)
        set(accessToken) {
            userPrefs!!.edit().putString(User.ACCESS_TOKEN, accessToken).apply()
        }

    override var userProfile: UserInfo?
        get() = getCustomObject(
            userPrefs!!, User.PROFILE,
            UserInfo::class.java
        )
        set(profile) {
            setCustomObject(userPrefs!!, User.PROFILE, profile)
        }

    override var isUserLoggedIn: Boolean?
        get() = userPrefs!!.getBoolean(User.IS_LOGGED_IN, false)
        set(isUserLoggedIn) {
            userPrefs!!.edit().putBoolean(User.IS_LOGGED_IN, isUserLoggedIn!!).apply()
        }

    fun setIsWalkthroughNeeded(isWalkttoughNeeded: Boolean?) {
        setBoolean(IS_WALKTHROUGH_NEEDED, isWalkttoughNeeded!!)
    }

    fun isWalkthroughNeeded(): Boolean? {
        return getBoolean(IS_WALKTHROUGH_NEEDED, true)
    }


    fun setAuthScreenNeeded(isAuthScreenNeeded: Boolean) {
        setBoolean(IS_AUTH_SCREEN_NEEDED, isAuthScreenNeeded)
    }

    fun isAuthScreenNeeded(): Boolean {
        return getBoolean(IS_AUTH_SCREEN_NEEDED, true)
    }


    fun setTempEmail(temp_email: String?) {
        setString(TEMP_EMAIL, temp_email)
    }

    fun getTempEmail(): String? {
        return getString(TEMP_EMAIL, "")
    }


    fun setTempPassword(temp_password: String?) {
        setString(TEMP_PASSWORD, temp_password)
    }

    fun getTempPassword(): String? {
        return getString(TEMP_PASSWORD, "")
    }


    override fun clearUserSession() {
        userPrefs!!.edit().clear().apply()
    }


    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return prefs!!.getBoolean(key, defValue)
    }

    fun getBoolean(key: String?): Boolean {
        return getBoolean(key, false)
    }

    fun setBoolean(key: String?, value: Boolean) {
        val editor: SharedPreferences.Editor = prefs!!.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getString(key: String?, defValue: String?): String? {
        return prefs!!.getString(key, defValue)
    }

    fun getString(key: String?): String? {
        return getString(key, null)
    }

    fun setString(key: String?, value: String?) {
        val editor: SharedPreferences.Editor = prefs!!.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun setUserLastLocation(lastLocation: Coordinates?) {
        setCustomObject(prefs!!, LAST_LOCATION, lastLocation)
    }

    fun getUserLastLocation(): Coordinates? {
        return getCustomObject(
            prefs!!, LAST_LOCATION,
            Coordinates::class.java
        )
    }


    companion object {
        private var uniqueInstance: SharedPrefsManager? = null
        val PREF_NAME = "sp_orbis"
        val USER_SSESION_PREF_NAME = "sp_user_session"
        val IS_WALKTHROUGH_NEEDED = "isWalkThroughNeeded"
        val IS_AUTH_SCREEN_NEEDED = "isAuthScreenNeeded"
        const val FCM_ID = "FCM_ID"
        const val TEMP_EMAIL = "temp_email"
        const val TEMP_PASSWORD = "temp_password"
        const val LAST_LOCATION = "last_location"
        const val LANG = "lang"
        const val NOTIFICATION_STATE = "notificationState"
        const val UNIT_STATE = "unitState"

        fun setCustomObject(prefs: SharedPreferences, key: String?, value: Any?) {
            prefs.edit()
                .putString(key, Gson().toJson(value))
                .apply()
        }

        fun <T> getCustomObject(prefs: SharedPreferences, key: String?, type: Type?): T {
            return Gson().fromJson(prefs.getString(key, null), type)
        }

        /**
         * Throws IllegalStateException if this class is not initialized
         * @return unique SharedPrefsManager instance
         */
        fun getInstance(): SharedPrefsManager? {
            checkNotNull(uniqueInstance) {
                "SharedPrefsManager is not initialized, call initialize(applicationContext) " +
                        "static method first"
            }
            return uniqueInstance
        }

        /**
         * Initialize this class using application Context,
         * should be called once in the beginning by any application Component
         *
         * @param appContext application context
         */
        fun initialize(appContext: Context?) {
            if (appContext == null) {
                throw NullPointerException("Provided application context is null")
            }
            if (uniqueInstance == null) {
                synchronized(SharedPrefsManager::class.java) {
                    if (uniqueInstance == null) {
                        uniqueInstance = SharedPrefsManager(appContext)
                    }
                }
            }
        }

    }

    constructor(appContext: Context) {
        prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        userPrefs = appContext.getSharedPreferences(
            USER_SSESION_PREF_NAME,
            Context.MODE_PRIVATE
        )
    }

    private interface User {
        companion object {
            const val PROFILE = "userProfile"
            const val ACCESS_TOKEN = "accessToken"
            const val EMAIL = "userEmail"
            const val IS_LOGGED_IN = "isUserLoggedIn"
        }
    }
}