package com.orbis.orbis.ui.homeModule.views

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class SplashscreenActivity : BaseActivity() {
    lateinit var i: Intent
    val secondsDelayed = 0
    lateinit var profileViewModel: ProfileViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isTaskRoot) {
            finish()
            return
        }

        setContentView(R.layout.activity_splash)

        Log.d("langaugeDetect", PrefManager(this).getLanguage()!!)

        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        profileViewModel.refreshToken()
        i = Intent(this, MapActivity::class.java)

        if (PrefManager(this).getIdToken().isNullOrEmpty()) {
            PrefManager(this).saveUserKey("")
        }
        val type = intent.getStringExtra("type")
        val fromUserKey = intent.getStringExtra("fromUserKey")
        val contentKey = intent.getStringExtra("contentKey")
        Log.d(
            "SplashscreenTypeCheck",
            type + " " + contentKey + " " + PrefManager(this).isSocialLogin().toString()
        )
        profileViewModel.tokenError.observe(this) {
            if (it) {
                if (PrefManager(this).isSocialLogin()) {
                    FirebaseAuth.getInstance().signOut()
                    profileViewModel.deleteTokenToServer()

                } else {
                    profileViewModel.deleteTokenToServer()
                }
            }
        }
        profileViewModel.logout.observe(this) {
            if (it) {
                val intent = Intent(this, MapActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        profileViewModel.tokenRefreshed.observe(this) {
            Log.d("refreshedToken", PrefManager(this).getIdToken()!!)
            if (it) {
                if (type.isNullOrEmpty()) {
                    handelDynamicLink()
                } else {

                    val i = Intent(this, MapActivity::class.java)
                    i.putExtra("type", type)
                    i.putExtra("fromUserKey", fromUserKey)
                    i.putExtra("contentKey", contentKey)
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(i)
                    finish()
                }
            }
        }

    }


    private fun handelDynamicLink() {

        FirebaseDynamicLinks.getInstance().getDynamicLink(intent).addOnSuccessListener {
            if (it?.link != null) {
                val link = it.link.toString().split("/")
                try {
                    val id = link[link.size - 1].replace("?key","").split("___")
                    i.putExtra("id", id[0])
                    i.putExtra("type", id[1])
                } catch (e: Exception) {
                }
                goToMapActivity()
            } else {
                goToMapActivity()
            }
        }.addOnFailureListener {
            goToMapActivity()
        }
    }

    fun goToMapActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            val i = Intent(this, MapActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            finish()
        }, secondsDelayed * 1000L)

    }

}







