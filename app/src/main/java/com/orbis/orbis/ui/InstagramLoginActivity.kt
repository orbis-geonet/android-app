package com.orbis.orbis.ui

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ActivityInstagramLoginBinding

class InstagramLoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityInstagramLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_instagram_login)
        initView()
    }

    private fun initView() {
        val url = intent.getStringExtra("url")
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("https://orbis-v2.rj.r.appspot.com/igoauth/")) {
                    finish()
                }
                binding.loading = true
                view.settings.javaScriptEnabled = true
                view.settings.domStorageEnabled = true
                view.loadUrl(url)
                return false
            }

            override fun onLoadResource(view: WebView, url: String) {
                super.onLoadResource(view, url)
                if (view.progress < 100) {
                    binding.progressBar8.progress = view.progress
                } else {
                    binding.progressBar8.visibility = View.GONE
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }
        }
        binding.webView.loadUrl(url!!)
    }
}