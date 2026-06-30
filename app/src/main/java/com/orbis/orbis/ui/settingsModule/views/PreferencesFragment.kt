package com.orbis.orbis.ui.settingsModule.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.databinding.FragmentPreferencesBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.homeModule.views.SplashscreenActivity
import com.orbis.orbis.ui.settingsModule.viewModel.AppLanguages
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreferencesFragment : BaseFragment(), LanguageListener {
    lateinit var binding: FragmentPreferencesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_preferences, container, false)
        initView()
        return binding.root
    }

    lateinit var pref: PrefManager
    private fun initView() {
        pref = PrefManager(requireContext())
        getData()
        binding.resetSettingsRl.setOnClickListener {
            pref.saveUnit("")
            pref.saveLanguage("")
            pref.saveNotification(true)
            getData()
        }
        profileViewModel.myProfile.observe(viewLifecycleOwner) {
            PrefManager(requireContext()).saveLanguage(it.language!!)
            //setLanguage(it.language!!.lowercase())
            val intent = Intent(requireContext(), SplashscreenActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

    }

    fun getData() {
        Log.d("checkLanguage", pref.getLanguage()!!)
        val displayName = AppLanguages.displayNameFor(pref.getLanguage())
        binding.selectedLanguageTv.text = displayName ?: getString(R.string.device_language)

        if (pref.getNotification()) {
            btnStateToggle(binding.btnOn, true)
        } else {
            btnStateToggle(binding.btnOff, true)
        }
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)



        binding.btnOn.setOnClickListener {
            pref.saveNotification(true)
            btnStateToggle(binding.btnOn, true)
            btnStateToggle(binding.btnOff, false)
        }
        binding.btnOff.setOnClickListener {
            pref.saveNotification(false)
            btnStateToggle(binding.btnOn, false)
            btnStateToggle(binding.btnOff, true)
        }

        binding.selectedLanguageRl.setOnClickListener {
            showSelectLanguageSheet() }
        hideKeyboard(requireActivity())
    }
    private fun showSelectLanguageSheet() {
        val selectLanguageDialogFragment = SelectLanguageDialogFragment(this)
        selectLanguageDialogFragment.show(
            childFragmentManager,
            SelectLanguageDialogFragment::class.java.getSimpleName()
        )
    }

    companion object {
        fun getInstance() : PreferencesFragment {
            return PreferencesFragment()
        }
    }

    private fun btnStateToggle(btn: Button, selectedState: Boolean) {
        if (selectedState) {
            btn.isSelected = true
            btn.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
        } else {
            btn.isSelected = false
            btn.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("checkLang", pref.getLanguage().toString())
    }
    lateinit var profileViewModel: ProfileViewModel
    override fun onSelect(lang: String) {
        Constants.userProfile?.language = lang
        profileViewModel.updateMyProfile(Constants.userProfile!!)

    }


}

interface LanguageListener {
    fun onSelect(lang: String)
}