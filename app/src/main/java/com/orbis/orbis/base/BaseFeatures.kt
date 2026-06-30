package com.orbis.orbis.base

import android.view.View

interface BaseFeatures {
    fun setToolbarTitle(title: String?)
    fun setToolbarTitleColor(color: Int?)
    fun setToolbarColor(color: Int?)
    fun showBackButton()
    fun setBackButtonColor(color: Int?)
    fun hideBackButton()
    fun hideToolbar()
    fun showToolbar()
    fun backButtonClickListener(listener: View.OnClickListener?)
}
