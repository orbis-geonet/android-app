package com.orbis.orbis.ui.settingsModule.views

import android.os.Bundle
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.utils.FragmentUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val userInfo: UserInfo? = intent.getParcelableExtra("data")
        if (userInfo != null) {
            launchFragmentWithNoAnimation(
                R.id.container,
                SettingsFragment(userInfo),
                FragmentUtils.FragmentLaunchMode.REPLACE,
                false
            )
        }
    }
}