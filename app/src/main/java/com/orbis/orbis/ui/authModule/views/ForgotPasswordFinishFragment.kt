package com.orbis.orbis.ui.authModule.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.NavHostFragment
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentForgotPasswordFinishBinding


class ForgotPasswordFinishFragment : Fragment() {
    lateinit var binding: FragmentForgotPasswordFinishBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_forgot_password_finish,
            container,
            false
        )
        initView()
        return binding.root
    }

    private fun initView() {
        val navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.forgotPasswordNav) as NavHostFragment
        binding.backButton.setOnClickListener {
            navHostFragment.navController.popBackStack()
        }
        binding.understoodButton.setOnClickListener {
            requireActivity().finish()
        }
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            })

    }


}