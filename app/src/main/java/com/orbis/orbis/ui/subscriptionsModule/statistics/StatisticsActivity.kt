package com.orbis.orbis.ui.subscriptionsModule.statistics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.databinding.ActivityStatisticsBinding
import com.orbis.orbis.ui.subscriptionsModule.ViewPagerAdapter
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class StatisticsActivity : BaseActivity() {

    companion object {
        fun open(context: Context, groupKey: String) {
            val intent = Intent(context, StatisticsActivity::class.java)
            intent.putExtra("groupKey", groupKey)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    private var groupKey = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupKey = intent?.getStringExtra("groupKey") ?: ""
        setupBinding()
        initView()
    }

    private fun initView() {
        setupToolbar()
        setupViewPager()
        setupTabLayout()
    }

    private fun setupBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_statistics)
    }

    private fun setupToolbar() {
        binding.toolbar.titleTv.text =
            resources.getString(R.string.activity)
        binding.toolbar.backArrowIv.setOnClickListener {
            finish()
        }
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        val managementFragment = ManagementFragment.newInstance(groupKey)
        val subscribersFragment = SubscribersFragment.newInstance(groupKey)
        viewPagerAdapter.add(managementFragment, getString(R.string.management))
        viewPagerAdapter.add(subscribersFragment, getString(R.string.subscribers))
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.isSaveEnabled = false
        binding.viewPager.isUserInputEnabled = false
        val limit = if (viewPagerAdapter.itemCount > 1) viewPagerAdapter.itemCount - 1 else 1
        binding.viewPager.offscreenPageLimit = limit
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })
        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager
        ) { tab, position ->
            tab.text = viewPagerAdapter.getPageTitle(position)
        }.attach()
    }
}