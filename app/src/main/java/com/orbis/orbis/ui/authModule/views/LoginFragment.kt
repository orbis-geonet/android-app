package com.orbis.orbis.ui.authModule.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.databinding.FragmentLoginBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.auth.LoginBody
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.authModule.viewModel.AuthViewModel
import com.orbis.orbis.ui.homeModule.views.MapActivity
import com.orbis.orbis.ui.homeModule.views.MapFragment
import com.orbis.orbis.ui.newsFeedModule.views.NewsFeedActivity
import com.orbis.orbis.utils.FragmentUtils
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment() {
    private lateinit var binding: FragmentLoginBinding
    private lateinit var viewModel: AuthViewModel
    private lateinit var profileViewModel: ProfileViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        binding.data = LoginBody()
        binding.loginBtn.setOnClickListener(View.OnClickListener {
            login()

//            val groupIntent = Intent(requireContext(), MapActivity::class.java)
//            startActivity(groupIntent)
//            requireActivity().finish()
        }

        )
        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(requireContext(), ForgotPasswordActivity::class.java))

        }
        hideKeyboard(requireActivity())
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        // hideKeyboard(requireActivity())
    }

    private fun setupObservers() {
        viewModel.userProfile.observe(viewLifecycleOwner) {
            Log.d("refreshToken", it.refreshToken)
            Log.d("idToken", it.idToken)
            Log.d("userName", it.displayName)
            PrefManager(requireContext()).saveUserName(it.displayName)
            viewModel.saveUserKey(it.userKey)
            viewModel.saveIdToken(it.idToken)
            viewModel.saveRefreshToken(it.refreshToken)
            viewModel.saveRefreshEmail(binding.data!!.email)
            Toast.makeText(requireContext(), "Logged In Successfully", Toast.LENGTH_SHORT).show()
            profileViewModel.updateFcmToken()
            requireActivity().finish()

        }
        viewModel.errorResponse.observe(viewLifecycleOwner) {
            if (it.message.isNotEmpty()) {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
    }

    private fun login() {
        if (binding.data!!.email.isEmpty()) {
            binding.usernameEt.error = getString(R.string.enter_valid_username)
            binding.usernameEt.isErrorEnabled = true
        } else if (binding.data!!.password.isEmpty()) {
            binding.emailEt.error = getString(R.string.enter_password)
            binding.emailEt.isErrorEnabled = true
        } else {
            hideKeyboard(requireActivity())
            PrefManager(requireContext()).deleteSocialLogin()
            viewModel.login(binding.data)
        }
    }


    companion object {
        fun getInstance(): LoginFragment {
            return LoginFragment()
        }
    }

}