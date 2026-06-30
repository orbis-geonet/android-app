package com.orbis.orbis.ui.subscriptionsModule

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val items: ArrayList<Fragment> = arrayListOf()
    private val titles: ArrayList<String> = arrayListOf()

    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment = items[position]

    fun getPageTitle(position: Int): CharSequence = titles[position]

    fun add(fragment: Fragment, title: String) {
        items.add(fragment)
        titles.add(title)
    }
}