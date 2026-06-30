package com.orbis.orbis.ui.homeModule.views

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentCheckinListBinding
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.ui.homeModule.adapter.CheckinPlaceListAdapter
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import com.orbis.orbis.ui.placesModule.views.CreatePlaceDialogFragment
import com.orbis.orbis.ui.placesModule.views.CreatePostDialogFragment
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CheckInPlaceDialogFragment : BaseBottomSheetFragment(), CheckinPlaceListAdapter.PlaceCardInteraction, OnPlaceCreate {

    companion object {
        fun newInstance(mLastLocation: Location): CheckInPlaceDialogFragment {
            val args = Bundle()
            args.putParcelable("mLastLocation", mLastLocation)
            val fragment = CheckInPlaceDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    lateinit var mLastLocation: Location
    var listener: OnPlaceCreate? = null

    lateinit var viewModel: PlaceViewModel
    lateinit var binding: FragmentCheckinListBinding
    lateinit var mAdapter: CheckinPlaceListAdapter
    val places: ArrayList<PlaceDetails> = ArrayList()
    var searchTerm = ""
    var currentPage = 0
    var prevPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLastLocation = arguments?.getParcelable("mLastLocation") ?: viewModel.lastLocationSelected ?: Constants.location ?: Location("")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_checkin_list, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        viewModel = ViewModelProvider(this).get(PlaceViewModel::class.java)
        if (viewModel.lastLocationSelected != null)
            mLastLocation = viewModel.lastLocationSelected!!
        binding.placesRv.layoutManager = LinearLayoutManager(requireContext())
        mAdapter = CheckinPlaceListAdapter(this, requireContext(), places)
        binding.placesRv.adapter = mAdapter
        viewModel.getPlaces(
            mLastLocation.latitude,
            mLastLocation.longitude,
            searchTerm,
            currentPage
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }
        setupObservers()
        binding.placesRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val total = layoutManager!!.itemCount
                val currentLastItem = layoutManager!!.findLastVisibleItemPosition()
                val firstVisibleItem = layoutManager!!.findFirstVisibleItemPosition()
                if (currentLastItem == total - 15) {
                    if (currentPage != prevPage) {
                        viewModel.getPlaces(
                            mLastLocation.latitude,
                            mLastLocation.longitude,
                            searchTerm,
                            currentPage
                        )
                    }
                }
            }
        })
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    places.clear()
                    currentPage = 0
                    searchTerm = ""
                    mAdapter.notifyDataSetChanged()
                    viewModel.getPlaces(
                        mLastLocation.latitude,
                        mLastLocation.longitude,
                        searchTerm,
                        currentPage
                    )
                    hideKeyboard(requireActivity())
                }
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                places.clear()
                currentPage = 0
                searchTerm = query
                mAdapter.notifyDataSetChanged()
                viewModel.getPlaces(
                    mLastLocation.latitude,
                    mLastLocation.longitude,
                    searchTerm,
                    currentPage,
                    1
                )
                hideKeyboard(requireActivity())
                return false
            }

        })
    }

    private fun setupObservers() {
        viewModel.mapPlaces.observe(viewLifecycleOwner) {
            prevPage = currentPage
            currentPage++
            val index = places.size
            places.addAll(it)
            mAdapter.notifyItemRangeInserted(index, places.size)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        binding.toolbar.titleTv.setText(resources.getString(R.string.claim))
        binding.createBtn.setOnClickListener {
            dismiss()
            showCreatePlaceSheet()
        }
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
        hideKeyboard(requireActivity())
    }

    private fun showCreatePostSheet(position: Int) {
        val createPostDialogFragment =
            CreatePostDialogFragment.newInstance(placeDetails = places[position], isCheckin = true, location = mLastLocation)
        createPostDialogFragment.listener = this
        createPostDialogFragment.show(
            requireActivity().supportFragmentManager,
            CreatePostDialogFragment::class.java.getSimpleName()
        )
    }

    private fun showCreatePlaceSheet() {
        val createPlaceDialogFragment = CreatePlaceDialogFragment.newInstance(mLastLocation)
        createPlaceDialogFragment.listener = listener
        createPlaceDialogFragment.show(
            requireActivity().supportFragmentManager,
            CreatePlaceDialogFragment::class.java.getSimpleName()
        )
    }

    override val layoutId: Int
        get() = R.layout.fragment_checkin_list
    override val pageTitle: String?
        get() = ""


    override fun onItemClicked(position: Int) {
        showCreatePostSheet(position)
    }

    override fun onPlaceCreate(placeDetails: PlaceDetails) {}

    override fun onPostCreate(feedPost: FeedPost) {
        listener?.onPostCreate(feedPost)
        dismiss()
    }


}