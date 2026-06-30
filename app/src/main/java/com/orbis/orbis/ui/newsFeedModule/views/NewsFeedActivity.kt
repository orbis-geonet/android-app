package com.orbis.orbis.ui.newsFeedModule.views

import android.content.Intent
import android.location.Location
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.databinding.ActivityNewsFeedBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.newsFeedModule.adapter.FeedVPAdapter
import com.orbis.orbis.ui.newsFeedModule.viewModel.FeedViewModel
import com.orbis.orbis.ui.placesModule.views.CreatePostDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewsFeedActivity : BaseActivity(), OnPlaceCreate {
    lateinit var binding: ActivityNewsFeedBinding
    lateinit var vpAdapter: FeedVPAdapter
    lateinit var viewModel: FeedViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_news_feed)
        initView()
    }

    lateinit var location: Location
    lateinit var city: String
    private fun initView() {
        location = intent.getParcelableExtra("location")!!
        city = intent.getStringExtra("city")!!
        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]
        vpAdapter = FeedVPAdapter(
            supportFragmentManager,
            FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
            location,
            city
        )

        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                if (position == 0) {
                    binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
                } else {
                    binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1))
                }
            }

            override fun onPageScrollStateChanged(state: Int) {

            }

        })
        binding.viewPager.adapter = vpAdapter
        binding.viewPager.currentItem = 1
        setupTabs()

    }


    fun setupTabs() {
        //addNewTab(false, resources.getString(R.string.my_feed))
        //addNewTab(false, "Nearby")

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) {
                    if (PrefManager(this@NewsFeedActivity).getIdToken().isNullOrEmpty()) {
                        val intent = Intent(this@NewsFeedActivity, AuthActivity::class.java)
                        intent.putExtra("goToLogin", true)
                        startActivity(intent)
                        binding.viewPager.currentItem = 1
                        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1))
                    } else {
                        binding.viewPager.currentItem = 0
                    }
                } else {
                    binding.viewPager.currentItem = 1
                }

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })
    }

    fun addNewTab(isDefaultSelected: Boolean, title: String) {
        binding.tabLayout.addTab(
            binding.tabLayout.newTab().setText(title).setIcon(null),
            isDefaultSelected
        )
    }

    override fun onPlaceCreate(placeDetails: PlaceDetails) {

    }

    override fun onPostCreate(feedPost: FeedPost) {

    }
}