
package com.orbis.orbis.ui.authModule.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.databinding.FragmentRegisterBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.auth.SignUpBody
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.authModule.viewModel.AuthViewModel
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class RegisterFragment : BaseFragment() {
    private lateinit var binding: FragmentRegisterBinding
    private lateinit var viewModel: AuthViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private var keyboardListenersAttached = false
    private val context = this
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        binding.data = SignUpBody()
        binding.registerBtn.setOnClickListener {
            validateSignup()
        }
        val ss = SpannableString(getString(R.string.accept_terms))
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                val url = "https://orbis.social/tos"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }
        ss.setSpan(clickableSpan, 43, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)


        binding.termOfUse.text = ss
        binding.termOfUse.movementMethod = LinkMovementMethod.getInstance()
        setupObservers()
        //attachKeyboardListeners()
    }

    override fun onResume() {
        super.onResume()
        // hideKeyboard(requireActivity())
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    private fun setupObservers() {
        viewModel.userProfile.observe(viewLifecycleOwner) {
            PrefManager(requireContext()).saveUserName(it.displayName)
            viewModel.saveUserKey(it.userKey)
            viewModel.saveIdToken(it.idToken)
            viewModel.saveRefreshToken(it.refreshToken)
            PrefManager(requireContext()).deleteSocialLogin()
            profileViewModel.updateFcmToken()
            Toast.makeText(
                requireContext(),
                getString(R.string.register_success),
                Toast.LENGTH_SHORT
            ).show()
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

    private fun validateSignup() {
        hideKeyboard(requireActivity())
        when {
            binding.data!!.displayName.isEmpty() -> {
                binding.nameEt.error = getString(R.string.enter_name)
                binding.nameEt.isErrorEnabled = true
            }
            binding.data!!.email.isEmpty() -> {
                binding.emailEt.error = getString(R.string.enter_valid_email)
                binding.emailEt.isErrorEnabled = true
            }
            binding.data!!.password.isEmpty() -> {
                binding.passwordEt.error = getString(R.string.enter_valid_password)
                binding.passwordEt.isErrorEnabled = true
            }
            else -> {
                binding.nameEt.isErrorEnabled = false
                binding.emailEt.isErrorEnabled = false
                binding.passwordEt.isErrorEnabled = false
                viewModel.signup(binding.data)
            }
        }
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //hideKeyboard(requireActivity())
    }


    companion object {
        fun getInstance(): RegisterFragment {
            return RegisterFragment()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }


}