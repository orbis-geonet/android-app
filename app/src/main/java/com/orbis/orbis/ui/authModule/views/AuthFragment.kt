package com.orbis.orbis.ui.authModule.views


import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.utils.FragmentUtils
import com.orbis.orbis.utils.hideKeyboard
import com.orbis.orbis.databinding.FragmentAuthBinding


class AuthFragment : BaseFragment() {

    lateinit var binding: FragmentAuthBinding

    var current_position = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.toolbar.backArrowIv.setOnClickListener { requireActivity().onBackPressed() }
        binding.toolbar.titleTv.setText("")
        binding.wantToSignUpBtn.setOnClickListener {
            Log.d("signUpClicked", "clicked")
            val fragment = SigninSignupFragment()
            val args = Bundle()
            args.putBoolean("HaveAccount", false)
            fragment.arguments = args
            FragmentUtils.launchFragmentWithDefaultAnimation(
                requireActivity(),
                R.id.container,
                fragment,
                FragmentUtils.FragmentLaunchMode.REPLACE,
                true
            ) }
        binding.alreadyHaveBtn.setOnClickListener {

            val fragment = SigninSignupFragment()
            val args = Bundle()
            args.putBoolean("HaveAccount", true)
            fragment.arguments = args
            FragmentUtils.launchFragmentWithDefaultAnimation(
                requireActivity(),
                R.id.container,
                fragment,
                FragmentUtils.FragmentLaunchMode.REPLACE,
                true
            )
        }
        hideKeyboard(requireActivity())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requireActivity().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            )
        } else {
            requireActivity().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )
        }
    }



    companion object {
        fun getInstance(): AuthFragment {
            return AuthFragment()
        }

    }

}