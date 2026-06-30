package com.orbis.orbis.ui.storiesModule.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.orbis.orbis.models.story.StoryModel
import com.orbis.orbis.ui.storiesModule.views.StoryFragment
import com.orbis.orbis.ui.storiesModule.views.StoryInteraction

class StoryPagerAdapter constructor(
    fragmentManager: FragmentManager,
    private val storyList: ArrayList<StoryModel>,
    private val listener: StoryInteraction,


    ) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment =
        StoryFragment(storyList[position], listener, position)

    override fun getCount(): Int {
        return storyList.size
    }

    fun findFragmentByPosition(viewPager: ViewPager, position: Int): Fragment? {
        try {
            val f = instantiateItem(viewPager, position)
            return f as? Fragment
        } finally {
            finishUpdate(viewPager)
        }
    }
}