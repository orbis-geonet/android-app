package com.orbis.orbis.ui.settingsModule.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentPlaceListBinding
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.ui.ProfileModule.adapter.FollowedPlaceAdapter
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.groupsModule.adapter.GroupListAdapter
import com.orbis.orbis.ui.groupsModule.adapter.OwnedPlaceAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.Groups
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import com.orbis.orbis.ui.settingsModule.adapter.PlacesListAdapter
import com.orbis.orbis.ui.settingsModule.viewModel.Place
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectPlaceDialogFragment : BaseBottomSheetFragment(),
    FollowedPlaceAdapter.FollowedPlaceInteraction {
    lateinit var binding: FragmentPlaceListBinding

    lateinit var profileViewModel: ProfileViewModel
    lateinit var placeViewModel: PlaceViewModel
    val places: ArrayList<PlaceDetails> = ArrayList()
    val placesBackup: ArrayList<PlaceDetails> = ArrayList()
    lateinit var adapter: FollowedPlaceAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_place_list, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        placeViewModel = ViewModelProvider(this)[PlaceViewModel::class.java]
        profileViewModel.getMyFollowingPlace(0)
        binding.placesRv.layoutManager = LinearLayoutManager(requireContext())
        adapter = FollowedPlaceAdapter(requireContext(), places, this)
        binding.placesRv.adapter = adapter
        binding.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    places.clear()
                    places.addAll(placesBackup)
                    adapter.notifyDataSetChanged()
                    hideKeyboard(requireActivity())
                }
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                places.clear()
                for (place in placesBackup) {
                    if (place.name.lowercase().contains(query.lowercase()) || query.lowercase()
                            .contains(place.name.lowercase())
                    ) {
                        places.add(place)
                    }
                }
                adapter.notifyDataSetChanged()
                hideKeyboard(requireActivity())
                return false
            }

        })
        setupObservers()
    }

    private fun setupObservers() {
        profileViewModel.placeList.observe(viewLifecycleOwner) {
            places.clear()
            placesBackup.clear()
            places.addAll(it)
            placesBackup.addAll(it)
            adapter.notifyDataSetChanged()
        }
        profileViewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        placeViewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        placeViewModel.placeUnfollowed.observe(viewLifecycleOwner) {
            if (it) {
                profileViewModel.getMyFollowingPlace(0)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.toolbar.titleTv.setText(resources.getString(R.string.places))
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
        hideKeyboard(requireActivity())
    }

    override val layoutId: Int
        get() = R.layout.fragment_place_list
    override val pageTitle: String?
        get() = ""

    companion object {
        fun getInstance(): SelectPlaceDialogFragment {
            return SelectPlaceDialogFragment()
        }
    }


    override fun unfollowPlace(position: Int) {
        placeViewModel.unfollowPlace(places[position].placeKey)
    }


}