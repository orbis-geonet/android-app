package com.orbis.orbis.base

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.os.Bundle
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import kotlin.jvm.JvmOverloads
import com.orbis.orbis.utils.FragmentUtils.FragmentLaunchMode
import com.orbis.orbis.utils.FragmentUtils
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.utils.wrap
import java.util.*

abstract class BaseActivity : AppCompatActivity(), BaseFeatures {
    private var toolbar: Toolbar? = null
    private var tvToolbarTitle: TextView? = null
    private var ivBackButton: ImageView? = null

    override fun attachBaseContext(newBase: Context) {
        var lang: String?
        lang = if( PrefManager(newBase).getLanguage() == "dd")
            Locale.getDefault().language
        else
            PrefManager(newBase).getLanguage()!!.lowercase()
        super.attachBaseContext(
            ContextWrapper(newBase).wrap(
                lang.toString()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightNavigationBars = true
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        window.navigationBarColor = Color.TRANSPARENT

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
    }

    @JvmOverloads
    fun launchFragmentWithDefaultAnimation(
        containerId: Int,
        fragment: Fragment?,
        mode: FragmentLaunchMode?,
        addToBackStack: Boolean? = false
    ) {
        FragmentUtils.launchFragmentWithDefaultAnimation(
            this,
            containerId,
            fragment,
            mode,
            addToBackStack
        )
    }

    fun launchFragmentWithNoAnimation(
        containerId: Int,
        fragment: Fragment?,
        mode: FragmentLaunchMode?,
        addToBackStack: Boolean?
    ) {
        FragmentUtils.launchFragmentWithNoAnimation(
            this,
            containerId,
            fragment,
            mode,
            addToBackStack
        )
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count <= 1) {
            super.onBackPressed()
        } else {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    override fun setToolbarTitle(title: String?) {
        if (tvToolbarTitle != null) {
            tvToolbarTitle!!.text = title
        }
    }

    override fun setToolbarTitleColor(@ColorRes color: Int?) {
        if (tvToolbarTitle != null) {
            tvToolbarTitle!!.setTextColor(ContextCompat.getColor(this, color!!))
        }
    }

    override fun setBackButtonColor(@ColorRes color: Int?) {
        if (ivBackButton != null) {
            ivBackButton!!.setColorFilter(ContextCompat.getColor(this, color!!))
        }
    }

    override fun setToolbarColor(color: Int?) {
        if (toolbar != null) {
            toolbar!!.setBackgroundResource(color!!)
        }
    }

    override fun showBackButton() {
        if (ivBackButton != null) {
            ivBackButton!!.visibility = View.VISIBLE
        }
    }

    override fun hideBackButton() {
        if (ivBackButton != null) {
            ivBackButton!!.visibility = View.GONE
        }
    }

    override fun showToolbar() {
        if (toolbar != null) {
            toolbar!!.visibility = View.VISIBLE
        }
    }

    override fun hideToolbar() {
        if (toolbar != null) {
            toolbar!!.visibility = View.GONE
        }
    }

    override fun backButtonClickListener(listener: View.OnClickListener?) {}
}