package com.orbis.orbis.ui.placesModule.views

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentSearchListBinding
import com.orbis.orbis.listeners.SearchClick
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.homeModule.adapter.CheckinPlaceListAdapter
import com.orbis.orbis.ui.placesModule.adapter.CheckinCreatePlaceAdapter
import com.orbis.orbis.ui.settingsModule.viewModel.Place
import com.orbis.orbis.utils.PermissionUtil
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class SearchPlaceDialogFragment(private val listener: SearchClick, private val search: String) :
    BaseBottomSheetFragment(),
    CheckinCreatePlaceAdapter.PlaceCardInteraction {
    val groupList = ArrayList<GroupDetails>()
    lateinit var viewModel: GroupViewModel
    lateinit var mAdapter: CheckinCreatePlaceAdapter
    private lateinit var checkLocationPermission: ActivityResultLauncher<Array<String>>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var binding: FragmentSearchListBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search_list, container, false)
        viewModel = ViewModelProvider(this).get(GroupViewModel::class.java)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        binding.placesRv.layoutManager = LinearLayoutManager(requireContext())
        val view = binding.root

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }

        mAdapter = CheckinCreatePlaceAdapter(this, requireContext(), groupList)
        //mAdapter!!.setList(arrayList)
        if (search.isNotEmpty()) {
            binding.searchGroup.setQuery(search, false)
        }
        binding.placesRv.adapter = mAdapter
        view.findViewById<TextView>(R.id.title_tv).text = resources.getString(R.string.search)
        view.findViewById<ImageView>(R.id.back_arrow_iv).setOnClickListener { dismiss() }
        viewModel.groupList.observe(viewLifecycleOwner, {
            groupList.clear()
            groupList.addAll(it)
            mAdapter.notifyDataSetChanged()
        })
        viewModel.isLoading.observe(viewLifecycleOwner, {
            binding.loading = it
        })
        hideKeyboard(requireActivity())
        checkLocationPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                fetchLocation()
            } else {

                // Permission was denied. Display an error message.
            }
        }
        fetchLocation()
        // Inflate the layout for this fragment

        return binding.root
    }

    private fun fetchLocation() {
        binding.loading = true

        val hasPermission = ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            doSearch(null, null, search)
            setupSearchView(null, null)
            checkLocationPermission.launch(PermissionUtil().locationPermissions)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                val lat = location?.latitude
                val lon = location?.longitude
                doSearch(lat, lon, search)
                setupSearchView(lat, lon)
            }
            .addOnFailureListener {
                doSearch(null, null, search)
                setupSearchView(null, null)
            }
    }
    private fun doSearch(lat: Double?, lon: Double?, query: String) {
        if (lat != null && lon != null) {
            viewModel.getGroups(lat, lon, query)
        } else {
            viewModel.getGroupsNoLocation(query)
        }
    }

    private fun setupSearchView(lat: Double?, lon: Double?) {
        binding.searchGroup.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String) = false
            override fun onQueryTextSubmit(query: String): Boolean {
                doSearch(lat, lon, query)
                return false
            }
        })
    }


    override val layoutId: Int
        get() = R.layout.fragment_checkin_list
    override val pageTitle: String?
        get() = ""


    override fun onItemClicked(position: Int) {
        listener.onGroupSearchClick(groupList[position])
        dismiss()
    }


}