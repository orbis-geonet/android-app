package com.orbis.orbis.ui.homeModule.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import io.branch.referral.Branch

class OnboardingActivity : BaseActivity() {
    val TAG = "fromUserKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        val type = intent.getStringExtra("type")
        val fromUserKey = intent.getStringExtra("fromUserKey")
        val contentKey = intent.getStringExtra("contentKey")
        Log.d("OnboardingTypeCheck", type + " " + contentKey)
        val prefManager = PrefManager(this)
        if (prefManager.isFirstLaunch()) {
            prefManager.saveFirstLaunch()
            //start_btn.setOnClickListener {}
        }
        val homeIntent = Intent(this, SplashscreenActivity::class.java)
        homeIntent.putExtra("type", type)
        homeIntent.putExtra("fromUserKey", fromUserKey)
        homeIntent.putExtra("contentKey", contentKey)
        startActivity(homeIntent)
        finish()
        initBranchIO()
    }

    private fun initBranchIO()
    {
        PrefManager(this).initPartnerKey()

        Branch.sessionBuilder(this).withCallback { branchUniversalObject, linkProperties, error ->
            if (error != null)
            {
                Log.e(TAG, "branch init failed. Caused by -" + error.message)
            }
            else
            {
                Log.i(TAG, "branch init complete!")
                if (branchUniversalObject != null)
                {
                    branchUniversalObject.contentMetadata.customMetadata["partnerKey"]?.let { partnerKey ->
                        PrefManager(this).savePartnerKey(partnerKey)
                    }
                }
            }
        }.withData(this.intent.data).init()
    }

    private fun getBranchIODeepLink()
    {
        Log.e(TAG, "onNewIntent")
        Branch.sessionBuilder(this).withCallback { referringParams, error ->
            if (error != null) {
                Log.e(TAG, error.message)
            } else if (referringParams != null) {
                Log.i(TAG, referringParams.toString())
            }
        }.init()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getBranchIODeepLink()
        Log.d("newIntentFound", "found")
        val code = intent?.data?.getQueryParameter("code").toString()
        Log.d("clientSecretMain", code)
        if (!code.isNullOrEmpty()) {
            val homeIntent = Intent(this, ProfileActivity::class.java)
            startActivity(homeIntent)
            finish()
        }

    }


}