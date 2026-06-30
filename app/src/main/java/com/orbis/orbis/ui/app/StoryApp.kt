package  com.orbis.orbis.ui.app

import android.app.Application
import android.util.Log
import android.webkit.URLUtil

import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.orbis.orbis.BuildConfig
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.preferences.SharedPrefsManager
import com.squareup.picasso.OkHttp3Downloader
import dagger.hilt.android.HiltAndroidApp
import com.squareup.picasso.Picasso
import io.branch.referral.Branch
import io.branch.referral.validators.IntegrationValidator
import java.util.*


@HiltAndroidApp
class StoryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // ANRWatchDog().start()
        SharedPrefsManager.initialize(this)
        val leastRecentlyUsedCacheEvictor = LeastRecentlyUsedCacheEvictor(90 * 1024 * 1024)
        val databaseProvider: DatabaseProvider = ExoDatabaseProvider(this)
        if (simpleCache == null) {
            simpleCache = SimpleCache(cacheDir, leastRecentlyUsedCacheEvictor, databaseProvider)
        }
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

//        Branch.enableTestMode()
        IntegrationValidator.validate(this)
        Branch.enableLogging()
        Branch.getAutoInstance(this)
    }


    companion object {
        var simpleCache: SimpleCache? = null
    }
}