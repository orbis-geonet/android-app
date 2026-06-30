package com.orbis.orbis.utils

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import java.text.SimpleDateFormat
import java.util.*

import android.annotation.TargetApi
import android.content.ContextWrapper
import android.os.Build

@Suppress("DEPRECATION")
fun ContextWrapper.wrap(language: String): ContextWrapper {
    val config = baseContext.resources.configuration
    val sysLocale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        this.getSystemLocale()
    } else {
        this.getSystemLocaleLegacy()
    }

    if (!language.isEmpty() && sysLocale.language != language) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.setSystemLocale(locale)
        } else {
            this.setSystemLocaleLegacy(locale)
        }
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        val context = baseContext.createConfigurationContext(config)
        ContextWrapper(context)
    } else {
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        ContextWrapper(baseContext)
    }

}

@Suppress("DEPRECATION")
fun ContextWrapper.getSystemLocaleLegacy(): Locale {
    val config = baseContext.resources.configuration
    return config.locale
}

@TargetApi(Build.VERSION_CODES.N)
fun ContextWrapper.getSystemLocale(): Locale {
    val config = baseContext.resources.configuration
    return config.locales[0]
}


@Suppress("DEPRECATION")
fun ContextWrapper.setSystemLocaleLegacy(locale: Locale) {
    val config = baseContext.resources.configuration
    config.locale = locale
}

@TargetApi(Build.VERSION_CODES.N)
fun ContextWrapper.setSystemLocale(locale: Locale) {
    val config = baseContext.resources.configuration
    config.setLocale(locale)
}

fun hideKeyboard(activity: Activity) {
    val view = activity.currentFocus
    if (view != null) {
        val inputManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager?.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}