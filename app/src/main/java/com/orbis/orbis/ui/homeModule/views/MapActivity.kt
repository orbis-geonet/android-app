package com.orbis.orbis.ui.homeModule.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.models.AppConfig
import com.orbis.orbis.models.Constants
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.groupsModule.views.GroupDetailsActivity
import com.orbis.orbis.ui.messageModule.views.ChatActivity
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import com.orbis.orbis.ui.placesModule.views.PlaceActivity
import com.orbis.orbis.utils.FragmentUtils
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MapActivity : BaseActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var  appUpdateManager: AppUpdateManager

    // setup a listener
    val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // After the update is downloaded, show a notification
            // and request user confirmation to restart the app.
            popupSnackbarForCompleteUpdate()
        }
    }
    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_CANCELED) {
            Log.d("log_update", result.resultCode.toString())
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        appUpdateManager.unregisterListener(listener)

        val snackbar = Snackbar.make(
            findViewById(R.id.container),
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        )

        snackbar.setAction("RESTART") {
            appUpdateManager.completeUpdate()
        }

        snackbar.setActionTextColor(resources.getColor(R.color.white, theme))
        snackbar.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightNavigationBars = true
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        window.navigationBarColor = Color.TRANSPARENT

        appUpdateManager = AppUpdateManagerFactory.create(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val w: Window = window
            w.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        setWindowFlag(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContentView(R.layout.activity_map)
        launchFragmentWithNoAnimation(
            R.id.container,
            MapFragment(),
            FragmentUtils.FragmentLaunchMode.REPLACE,
            false
        )
        val placeViewModel = ViewModelProvider(this)[PlaceViewModel::class.java]
        val groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        val db = Firebase.firestore
        var query = db.collection("appConfig")
        query.get().addOnSuccessListener {
            for (doc in it.documents) {
                var appConfig = doc.toObject(AppConfig::class.java)
                Constants.appConfig = appConfig
                Log.d(
                    "checkAppConfig",
                    appConfig?.feedAdsFrequency.toString() + " " + appConfig?.isAdsEnabled
                )
            }
        }
        MobileAds.initialize(this) {}
        val type = intent.getStringExtra("type")
        val fromUserKey = intent.getStringExtra("fromUserKey")
        val contentKey = intent.getStringExtra("contentKey")
        Log.d("MapTypeCheck", type + " " + contentKey)

        sharedPreferences = this.getSharedPreferences("MAP", Context.MODE_PRIVATE)

        if (!type.isNullOrEmpty()) {
            if (type == "POST" || type == "COMMENT" || type == "REPORT_POST") {
                val intent = Intent(this, PostDetailsActivity::class.java)
                intent.putExtra("postKey", contentKey)
                startActivity(intent)
            } else if (type == "CHECK_IN" || type == "REPORT_PLACE") {
                placeViewModel.getNewPlace(contentKey!!)
                placeViewModel.newAddedPlace.observe(this) {
                    val intent = Intent(this@MapActivity, PlaceActivity::class.java)
                    intent.putExtra("data", it)
                    startActivity(intent)
                }
            } else if (type == "MESSAGE") {
                val messageIntent = Intent(this@MapActivity, ChatActivity::class.java)
                messageIntent.putExtra("userKey", fromUserKey)
                startActivity(messageIntent)
            } else if (type == "FOLLOWER" || type == "REPORT_USER") {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra(
                    "displayName",
                    ""
                )
                intent.putExtra("userKey", fromUserKey)
                startActivity(intent)
            } else if (type == "REPORT_GROUP") {
                groupViewModel.getGroupByKey(contentKey!!)
                groupViewModel.groupDetails.observe(this) {
                    val intent = Intent(this@MapActivity, GroupDetailsActivity::class.java)
                    intent.putExtra("data", it)
                    startActivity(intent)
                }
            } else if (type == "FOLLOW_REQUEST") {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra(
                    "displayName",
                    ""
                )
                intent.putExtra("userKey", fromUserKey)
                startActivity(intent)

            }
        }

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {

                Log.d("update_availability", "is_available")

                if (appUpdateInfo.updatePriority() >= 4 && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    // Priority is high and immediate update is allowed
                    requestImmediateUpdate(appUpdateInfo)
                }
                // for flexible updates.
                if (appUpdateInfo.updatePriority() <= 3 && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    requestFlexibleUpdate(appUpdateInfo)
                }
            } else {
                Log.d("update_availability", "not_available")

            }
        }.addOnFailureListener {
            Log.e("update_availability", "Failed to check updates")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
    private fun requestImmediateUpdate(appUpdateInfo: AppUpdateInfo) {

        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            activityResultLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        )
    }

    // this code can be used in future when we dont want to span users with updates all the time.
    private fun requestFlexibleUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.registerListener(listener)
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            activityResultLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        )
    }

    private fun setWindowFlag(bits: Int, on: Boolean) {
        val win = window
        val winParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activityResultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
                }
            }
    }
}