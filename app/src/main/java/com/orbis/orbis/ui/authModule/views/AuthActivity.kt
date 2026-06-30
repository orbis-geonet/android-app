package com.orbis.orbis.ui.authModule.views

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.databinding.ActivityAuthBinding
import com.orbis.orbis.ui.authModule.viewModel.AuthViewModel
import com.orbis.orbis.ui.homeModule.views.MapActivity
import com.orbis.orbis.utils.FragmentUtils
import com.orbis.orbis.utils.ViewUtils
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AuthActivity : BaseActivity() {
    lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_auth)
        val goToLogin = intent.getBooleanExtra("goToLogin", false)
        if (goToLogin) {
            launchFragmentWithDefaultAnimation(
                R.id.container,
                SigninSignupFragment(),
                FragmentUtils.FragmentLaunchMode.REPLACE,
                false
            )
        } else {
            launchFragmentWithNoAnimation(
                R.id.container,
                AuthFragment(),
                FragmentUtils.FragmentLaunchMode.REPLACE,
                false
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}