package com.orbis.orbis.ui.settingsModule.views


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.TabLayoutOnPageChangeListener
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.ui.settingsModule.adapter.SettingsPagerAdapter
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import com.orbis.orbis.databinding.FragmentSettingsBinding


@AndroidEntryPoint
class SettingsFragment(val userInfo: UserInfo) : BaseFragment() {

    private lateinit var binding: FragmentSettingsBinding

    var current_position = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.toolbar.backArrowIv.setOnClickListener { requireActivity().onBackPressed() }
        setUpViewPager()
        hideKeyboard(requireActivity())
    }

    private fun setUpViewPager() {
            val adapter = SettingsPagerAdapter(
                childFragmentManager,
                FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
                userInfo
            )
        binding.containerLayout.setAdapter(adapter)
        binding.containerLayout.addOnPageChangeListener(TabLayoutOnPageChangeListener(binding.tabLayout))
        binding.toolbar.titleTv.setText(resources.getString(R.string.settings))
        binding.toolbar.backArrowIv.setOnClickListener { requireActivity().onBackPressed() }
        setupTabs()
    }

    fun setupTabs() {
        addNewTab(true, resources.getString(R.string.profile))
        addNewTab(false, resources.getString(R.string.social))
        addNewTab(false, resources.getString(R.string.preferences))

        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                current_position = tab.position
                binding.containerLayout.currentItem = current_position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    fun addNewTab(isDefaultSelected: Boolean?, title: String) {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(title).setIcon(null), isDefaultSelected!!)
    }

    companion object {
        fun getInstance(userInfo: UserInfo): SettingsFragment {
            return SettingsFragment(userInfo)
        }

    }

}