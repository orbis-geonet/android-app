package com.orbis.orbis.ui.placesModule.views

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentCreatePlaceBinding
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.listeners.SearchClick
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.CreatePlaceBody
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import com.orbis.orbis.ui.placesModule.adapter.CheckinCreatePlaceAdapter
import com.orbis.orbis.ui.placesModule.adapter.PlacesTypesListAdapter
import com.orbis.orbis.utils.PermissionUtil
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreatePlaceDialogFragment : BaseBottomSheetFragment(), CheckinCreatePlaceAdapter.PlaceCardInteraction, PlacesTypesListAdapter.PlaceCardInteraction, SearchClick {

    companion object {
        fun newInstance(location: Location): CreatePlaceDialogFragment{
            val args = Bundle()
            args.putParcelable("location", location)
            val fragment = CreatePlaceDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    var listener: OnPlaceCreate? = null
    lateinit var mLastLocation: Location

    lateinit var mAdapter: CheckinCreatePlaceAdapter
    private var mPlaceTypeAdapter: PlacesTypesListAdapter? = null
    private var arrowOpen = false
    lateinit var viewModel: PlaceViewModel
    private lateinit var checkLocationPermission: ActivityResultLauncher<Array<String>>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var binding: FragmentCreatePlaceBinding
    lateinit var searchPlaceDialogFragment: SearchPlaceDialogFragment
    val groupList: ArrayList<GroupDetails> = ArrayList()
    var selectedType = 0
    val places_typesS = arrayOf(
        "LOCATION",
        "BUILDING",
        "BAR",
        "HOUSE_2",
        "CASTLE",
        "SPORTS_CENTER",
        "FAST_FOOD",
        "SHOPPING",
        "RESTAURANT",
        "MUSIC",
        "BEACH",
        "SCHOOL",
        "TWO_BUILDINGS",
        "PARK",
        "HOUSE"
    )
    val places_types = intArrayOf(
        R.drawable.ic_group_1,
        R.drawable.ic_group_2,
        R.drawable.ic_group_3,
        R.drawable.ic_group_4,
        R.drawable.ic_group_5,
        R.drawable.ic_group_6,
        R.drawable.ic_group_7,
        R.drawable.ic_group_8,
        R.drawable.ic_group_9,
        R.drawable.ic_group_10,
        R.drawable.ic_group_11,
        R.drawable.ic_group_12,
        R.drawable.ic_group_13,
        R.drawable.ic_group_14,
        R.drawable.ic_group_15

    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_create_place, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }
        viewModel = ViewModelProvider(this).get(PlaceViewModel::class.java)
        fetchLocation()
        return binding.root
    }

    private fun fetchLocation() {
        mLastLocation = arguments?.getParcelable("location") as? Location ?: viewModel.lastLocationSelected ?: Constants.location ?: Location("")

        viewModel.findRecommendedGroup(
            mLastLocation.latitude,
            mLastLocation.longitude
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.data = CreatePlaceBody()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fetchLocation()
        binding.placesRv.setLayoutManager(LinearLayoutManager(requireContext()))

        mAdapter = CheckinCreatePlaceAdapter(this, requireContext(), groupList)
        binding.placesRv.setAdapter(mAdapter)
        binding.toolbar.titleTv.setText("")

        viewModel.recommendedGroups.observe(viewLifecycleOwner) {
            val nonRepeatedItems = it.filterNot { newItem ->
                groupList.any { existingItem -> existingItem.groupKey == newItem.groupKey }
            }
            groupList.addAll(nonRepeatedItems)
            mAdapter.notifyDataSetChanged()
        }
        viewModel.isLoading.observe(viewLifecycleOwner, {
            binding.loading = it
        })
        viewModel.error.observe(viewLifecycleOwner, {
            if (!it.isNullOrEmpty()) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.error.postValue("")
            }
        })

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {

                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                //dismiss()
                showSearchPlaceSheet(query)
                return true
            }

        })

        binding.selectUserCl.setOnClickListener {
            toggleDropdown()

        }


        binding.createPlaceBtn.setOnClickListener {
            //dismiss()
            showCreatePlaceMapSheet()
        }
        binding.placesTypeRv.setLayoutManager(GridLayoutManager(requireContext(), 4))
        mPlaceTypeAdapter = PlacesTypesListAdapter(this, requireContext())
        mPlaceTypeAdapter!!.setList(places_types)
        binding.placesTypeRv.setAdapter(mPlaceTypeAdapter)

        binding.toolbar.titleTv.setText(resources.getString(R.string.create_place))
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
        hideKeyboard(requireActivity())
    }

    private fun toggleDropdown() {
        if (!arrowOpen) {
            binding.arrowIv.setImageResource(R.drawable.ic_up_arrow)
            binding.searchPostAsCl.visibility = View.VISIBLE
            binding.selectUserCl.elevation = 2f
        } else {
            binding.arrowIv.setImageResource(R.drawable.ic_down_arrow)
            binding.searchPostAsCl.visibility = View.GONE
            binding.selectUserCl.elevation = 0f
        }
        arrowOpen = !arrowOpen
    }

    override val layoutId: Int
        get() = R.layout.fragment_create_place
    override val pageTitle: String?
        get() = ""



    override fun onPlaceTypeClicked(position: Int) {
        selectedType = places_types[position]
        binding.data?.type = places_typesS[position]
    }

    private fun showSearchPlaceSheet(query: String) {
        val fragmentManager = requireActivity().supportFragmentManager
        val existingFragment = fragmentManager.findFragmentByTag(SearchPlaceDialogFragment::class.java.simpleName)

        if (existingFragment == null) {
            searchPlaceDialogFragment = SearchPlaceDialogFragment(this, query)
            searchPlaceDialogFragment.show(fragmentManager, SearchPlaceDialogFragment::class.java.simpleName)
        }


    }

    private fun showCreatePlaceMapSheet() {
        if (binding.data?.name.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please enter a place name", Toast.LENGTH_SHORT).show()
        } else if (binding.data?.groupCreatedKey.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select a tribe", Toast.LENGTH_SHORT).show()
        } else if (binding.data?.type.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select a type", Toast.LENGTH_SHORT).show()
        } else {
            dismiss()
            val createPlaceMapDialogFragment =
                CreatePlaceMapDialogFragment.newInstance(binding.data!!, selectedType, mLastLocation)
            createPlaceMapDialogFragment.listener = listener
            createPlaceMapDialogFragment.show(
                requireActivity().supportFragmentManager,
                CreatePlaceMapDialogFragment::class.java.getSimpleName()
            )
        }

    }

    override fun onItemClicked(position: Int) {
        binding.data?.groupCreatedKey = groupList[position].groupKey
        binding.placeNameTv.text = groupList[position].name
        toggleDropdown()
        binding.strokeColorHex = groupList[position].strokeColorHex
        ViewUtils.loadGroupPhoto(requireContext(), binding.imageIv, groupList[position].imageName)
    }

    override fun onGroupSearchClick(groupDetails: GroupDetails) {
        searchPlaceDialogFragment.dismiss()
        binding.data?.groupCreatedKey = groupDetails.groupKey
        binding.placeNameTv.text = groupDetails.name
        toggleDropdown()
        binding.strokeColorHex = groupDetails.strokeColorHex
        ViewUtils.loadGroupPhoto(requireContext(), binding.imageIv, groupDetails.imageName)
    }
}