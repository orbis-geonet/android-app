package com.orbis.orbis.ui.authModule.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

import com.orbis.orbis.ui.authModule.views.LoginFragment
import com.orbis.orbis.ui.authModule.views.RegisterFragment


internal class AuthPagerAdapter(fm: FragmentManager, behavior: Int) :
    FragmentStatePagerAdapter(fm, behavior) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> LoginFragment.getInstance()
            1 -> RegisterFragment.getInstance()
            else -> throw IllegalArgumentException("Not supported Yet.")
        }
    }
    override fun getCount(): Int {
        return 2
    }


}
