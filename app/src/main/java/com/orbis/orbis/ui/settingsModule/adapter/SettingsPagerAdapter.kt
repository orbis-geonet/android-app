package com.orbis.orbis.ui.settingsModule.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.orbis.orbis.models.auth.UserInfo

import com.orbis.orbis.ui.settingsModule.views.EditProfileFragment
import com.orbis.orbis.ui.settingsModule.views.PreferencesFragment
import com.orbis.orbis.ui.settingsModule.views.SocialFragment


internal class SettingsPagerAdapter(fm: FragmentManager, behavior: Int, val userInfo: UserInfo) :
    FragmentStatePagerAdapter(fm, behavior) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> EditProfileFragment.getInstance(userInfo)
            1 -> SocialFragment.getInstance(userInfo)
            2 -> PreferencesFragment.getInstance()
            else -> throw IllegalArgumentException("Not supported Yet.")
        }
    }
    override fun getCount(): Int {
        return 3
    }


}
