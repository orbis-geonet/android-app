package com.orbis.orbis.ui.subscriptionsModule.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentPlaceEditAddressBinding
import com.orbis.orbis.databinding.FragmentSubscriptionImageBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.place.PlaceUpdateBody
import com.orbis.orbis.ui.groupsModule.adapter.ImageSliderAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SubscriptionImageDialogFragment : BottomSheetDialogFragment()
{
    //region variables
    lateinit var binding: FragmentSubscriptionImageBinding
    lateinit var imagesName: ArrayList<String>
    //endregion

    //region lifeCycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagesName = arguments?.getStringArrayList("imagesName")!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)

        initViews()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_subscription_image, container, false)

        return binding.root
    }

    companion object {
        fun newInstance(imagesName: ArrayList<String>): SubscriptionImageDialogFragment {
            val args = Bundle()
            args.putStringArrayList("imagesName", imagesName)
            val fragment = SubscriptionImageDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
    //endregion

    //region init
    private fun initViews()
    {
        setOnClicked()

        binding.toolbar.titleTv.text = getString(R.string.photos)

        if(imagesName.isNotEmpty())
        {
            try {
                val adapter = ImageSliderAdapter(requireContext(), imagesName, location = Constants.subscription_PHOTO_STORAGE)
                binding.checkInViewpager.adapter = adapter
                setupTabsImage(
                    binding.tabLayout,
                    binding.checkInViewpager
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupTabsImage(tab_layout: TabLayout, check_in_viewpager: ViewPager) {
        tab_layout.removeAllTabs()
        tab_layout.addTab(tab_layout.newTab().setIcon(null))
        tab_layout.getTabAt(0)?.select()
        tab_layout.setupWithViewPager(check_in_viewpager)
    }

    private fun setOnClicked()
    {
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
    }

    //endregion
}
