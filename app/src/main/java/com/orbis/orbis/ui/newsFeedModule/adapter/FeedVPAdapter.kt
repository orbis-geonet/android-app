package com.orbis.orbis.ui.newsFeedModule.adapter

import android.location.Location
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.orbis.orbis.ui.newsFeedModule.views.KmNewsFeedFragment
import com.orbis.orbis.ui.newsFeedModule.views.MyFeedFragment

class FeedVPAdapter(fm: FragmentManager, behavior: Int, val location: Location, val city: String) :
    FragmentStatePagerAdapter(fm, behavior) {
    override fun getCount(): Int = 2

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> MyFeedFragment.newInstance(location, city)
            1 -> KmNewsFeedFragment(location, city)
            else -> throw IllegalArgumentException("Unexpected adapter position: $position")
        }
    }

}