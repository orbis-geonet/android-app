package com.orbis.orbis.preferences

import com.orbis.orbis.models.auth.UserInfo


interface UserSession {
    var email: String?
    var accessToken: String?
    var userProfile: UserInfo?
    var isUserLoggedIn: Boolean?
    fun clearUserSession()
}
