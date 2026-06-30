package com.orbis.orbis.helpers.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.orbis.orbis.R

class LatestDeviceNotification(base: Context?) : ContextWrapper(base) {

    var manager: NotificationManager? = null
        get() {
            if (field == null) {
                field = getSystemService(NOTIFICATION_SERVICE)
                        as NotificationManager
            }
            return field
        }
        private set

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description = "Orbis notifications"

                enableLights(true)
                enableVibration(true)

                lockscreenVisibility = Notification.VISIBILITY_PUBLIC

                setSound(soundUri, attributes)
            }

            manager?.createNotificationChannel(notificationChannel)
        }
    }

    fun getLatest(
        title: String?,
        body: String?,
        pendingIntent: PendingIntent?,
        sound: Uri?
    ): NotificationCompat.Builder {
        createChannel()
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(body)
            )
            .setSmallIcon(R.drawable.ic_noti)
            .setAutoCancel(true)
            .setSound(sound)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    companion object {
        const val CHANNEL_ID = "com.orbis.orbis"
        const val CHANNEL_NAME = "Orbis"
    }
}
