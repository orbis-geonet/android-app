package com.orbis.orbis.ui.storiesModule.views

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.orbis.orbis.R
import com.orbis.orbis.models.Constants

class FullscreenAdActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_ad)
        loadAd()
    }

    private var mInterstitialAd: InterstitialAd? = null
    private fun loadAd() {
        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("onAdFailedToLoad", adError?.toString() ?: "")
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("onAdLoaded", "Ad was loaded.")
                    Constants.storyAdInterval = 0
                    mInterstitialAd = interstitialAd
                    mInterstitialAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdClicked() {
                                // Called when a click is recorded for an ad.
                                Log.d("onAdClicked", "Ad was clicked.")
                            }

                            override fun onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                Log.d(
                                    "onAdDismissedFullScreenContent",
                                    "Ad dismissed fullscreen content."
                                )
                                mInterstitialAd = null
                                finish()


                            }

                            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                // Called when ad fails to show.
                                Log.e(
                                    "onAdFailedToShowFullScreenContent",
                                    "Ad failed to show fullscreen content."
                                )
                                mInterstitialAd = null
                            }

                            override fun onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                Log.d("onAdImpression", "Ad recorded an impression.")
                            }

                            override fun onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                object : CountDownTimer(3000, 3000) {
                                    override fun onTick(millisUntilFinished: Long) {

                                    }

                                    @SuppressLint("RestrictedApi")
                                    override fun onFinish() {
                                        Log.d("closingAd", "closing")
                                        finish()

                                    }

                                }.start()
                                Log.d(
                                    "onAdShowedFullScreenContent",
                                    "Ad showed fullscreen content."
                                )
                            }
                        }
                    showAd()
                }
            })
    }

    private fun showAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)

        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.")
        }
    }
}