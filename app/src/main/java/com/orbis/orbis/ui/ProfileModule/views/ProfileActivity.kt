package com.orbis.orbis.ui.ProfileModule.views

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.utils.FragmentUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        var intent = intent
        val code = intent?.data
        if (code != null) {
            Log.d("newIntentOnCreate", code.toString())
            Handler(Looper.getMainLooper()).postDelayed({
                launchFragmentWithNoAnimation(
                    R.id.container,
                    MyProfileFragment(),
                    FragmentUtils.FragmentLaunchMode.REPLACE,
                    false
                )
            }, 1000)
        } else {
            var fragment: Fragment?
            val userKey = intent.getStringExtra("userKey")
            //Log.d("userKeyFound", userKey!! + " " + PrefManager(this).getUserKey())
            if (!userKey.isNullOrEmpty() && userKey == PrefManager(this).getUserKey() && !PrefManager(
                    this
                ).getIdToken().isNullOrEmpty()
            ) {
                fragment = MyProfileFragment()
            } else if (!userKey.isNullOrEmpty()) {
                val displayName = intent.getStringExtra("displayName")
                fragment = ProfileFragment(displayName!!, userKey)
            } else {
                fragment = MyProfileFragment()
            }
//        if (intent != null)
//            fragment = ProfileFragment()

            launchFragmentWithNoAnimation(
                R.id.container,
                fragment,
                FragmentUtils.FragmentLaunchMode.REPLACE,
                false
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("newIntent", "profile")
    }
}