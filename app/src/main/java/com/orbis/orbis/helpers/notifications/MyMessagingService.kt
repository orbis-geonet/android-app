package com.orbis.orbis.helpers.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.repositories.AuthRepositories
import com.orbis.orbis.repositories.ProfileRepositories
import com.orbis.orbis.ui.homeModule.views.OnboardingActivity
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MyMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepositories: AuthRepositories

    @Inject
    lateinit var profileRepositories: ProfileRepositories

    private val disposable = CompositeDisposable()

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d("NOTIF_DEBUG", "=== onNewToken fired: $token")
        authRepositories.saveFcmToken(token)

        // Update token on server if user is logged in
        if (!authRepositories.getIdToken().isNullOrEmpty()) {
            disposable.add(
                profileRepositories.updateFcmToken(token)
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                        Log.d("FCM_TOKEN", "Token updated on server")
                    }, {
                        Log.e("FCM_TOKEN", "Failed to update token on server", it)
                    })
            )
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notifEnabled = PrefManager(this).getNotification()
        Log.d("NOTIF_DEBUG", "  getNotification() = $notifEnabled")

        if (notifEnabled) {
            sendNotification(remoteMessage)
        } else {
            Log.w("NOTIF_DEBUG", "  >>> Notification SUPPRESSED by PrefManager!")
        }

        val intent = Intent("com.orbis.NEW_NOTIFICATION")
        sendBroadcast(intent)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun sendNotification(remoteMessage: RemoteMessage) {

        val title =
            remoteMessage.notification?.title
                ?: remoteMessage.data["title"]
                ?: "Orbis"

        val body =
            remoteMessage.notification?.body
                ?: remoteMessage.data["body"]
                ?: "Notification"

        val type = remoteMessage.data["type"] ?: ""
        val contentKey = remoteMessage.data["contentKey"] ?: ""
        val fromUserKey = remoteMessage.data["fromUserKey"] ?: ""

        val intent = Intent(this, OnboardingActivity::class.java).apply {
            putExtra("type", type)
            putExtra("fromUserKey", fromUserKey)
            putExtra("contentKey", contentKey)

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            Random().nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val latestDeviceNotification = LatestDeviceNotification(this)
        Log.d("NOTIF_DEBUG", "  manager = ${latestDeviceNotification.manager}")
        Log.d("NOTIF_DEBUG", "  channel exists = ${
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                latestDeviceNotification.manager?.getNotificationChannel(LatestDeviceNotification.CHANNEL_ID) != null
            else true
        }")

        val notification = latestDeviceNotification.getLatest(
            title, body, pendingIntent, soundUri
        ).build()

        val notifId = Random().nextInt()
        Log.d("NOTIF_DEBUG", "  posting notification id=$notifId")
        latestDeviceNotification.manager?.notify(notifId, notification)
    }

    override fun onDestroy() {
        disposable.clear()
        super.onDestroy()
    }
}
