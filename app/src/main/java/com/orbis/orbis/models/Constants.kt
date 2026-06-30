package com.orbis.orbis.models

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.widget.Toast
import com.orbis.orbis.R
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.place.WorkingHoursModel
import com.orbis.orbis.models.story.StoryModel
import java.util.*

object Constants {
    val GROUP_PHOTO_STORAGE = "groupPictures/"
    val POST_PHOTO_STORAGE = "posts/images/"
    val subscription_PHOTO_STORAGE = "groups/subscription/images/"
    val POST_VIDEO_STORAGE = "posts/videos/"
    val POST_AUDIO_STORAGE = "posts/audios/"
    val EVENT_STORAGE = "events/images/"
    val PLACE_PICTURES = "placePictures/"
    val PROFILE_PICTURES = "profilePictures/"
    val USER_PICTURES = "user/pictures/"
    val CHAT_IMAGE = "chat/images/"
    val CHAT_VIDEO = "chat/videos/"
    var IS_CHECKIN = false
    var location: Location? = null
    var currentCountryName: String = "Brazil"
    var IS_PRIVATE = false
    var userImage: String = ""
    var userProfile: UserInfo? = null
    var placeTopTitle = "Feed"
    var appConfig: AppConfig? = null
    var storyAdInterval = 0
    val INTERSTITIAL_AD_ID = "ca-app-pub-6738139926979321/9431305642"
    val NATIVE_AD_ID = "ca-app-pub-6738139926979321/3111686251"
    var partnerKey: String? = null
    var lastcheckInPolygonCoordinateKey: String? = null
    var lastCheckinPlace: PlaceDetails? = null
    var isSelfCheckIn = false

    var newKMNewsFeedSession: Boolean = true
    var newMyFeedSession: Boolean = true
    var newRankGroupSession: Boolean = true

    fun toExternalURL(context: Context, url: String?)
    {
        if(url.isNullOrEmpty()) { return }

        var webpage = Uri.parse(url)

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            webpage = Uri.parse("http://$url")
        }

        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    fun sendPhoneNumberToDialer(context: Context, phoneNumber: String) {
        val dialIntent = Intent(Intent.ACTION_DIAL)
        dialIntent.data = Uri.parse("tel:$phoneNumber")

        if (dialIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(dialIntent)
        }
    }

    fun openGoogleMaps(context: Context, address: String) {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps") // Ensure Google Maps app is used

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        }
    }

    fun copyTextToClipboard(context: Context, textToCopy: String)
    {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", textToCopy)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(context, context.getString(R.string.text_copied_to_clipboard), Toast.LENGTH_LONG).show()
    }
}

class BigDataSharConstants {

    companion object
    {
        var storiesArray: ArrayList<StoryModel> = ArrayList()
        var openingHourArray: ArrayList<WorkingHoursModel> = ArrayList()
        var subscriptionKey: String = ""
        var isOneTimePurchase: Boolean = false
    }
}
