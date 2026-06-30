package com.orbis.orbis.ui.authModule.views

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentForgotPasswordInfoBinding

class ForgotPasswordInfoFragment : Fragment() {
    lateinit var binding: FragmentForgotPasswordInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_forgot_password_info,
            container,
            false
        )
        initView()
        return binding.root
    }

    private fun initView() {
        binding.email = ""
        val navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.forgotPasswordNav) as NavHostFragment
        binding.forgotPasswordButton.setOnClickListener {
            if (binding.email!!.isEmpty() || !binding.email!!.contains("@")) {
                binding.usernameEt.error = requireActivity().getString(R.string.enter_valid_email)
            } else {

                FirebaseAuth.getInstance().sendPasswordResetEmail(binding.email!!)
                    .addOnCompleteListener {

                        if (it.isSuccessful) {
                            navHostFragment.navController.navigate(R.id.forgotPasswordFinishFragment)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                it.exception?.localizedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
        binding.emailEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                imm.hideSoftInputFromWindow(v.windowToken, 0)
                v.clearFocus()
                true
            } else {
                false
            }
        }
        binding.backButton.setOnClickListener {
            requireActivity().finish()
        }

    }

}