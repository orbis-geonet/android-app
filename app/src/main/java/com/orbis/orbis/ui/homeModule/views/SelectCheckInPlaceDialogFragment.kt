package com.orbis.orbis.ui.homeModule.views

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentCheckinListBinding
import com.orbis.orbis.listeners.OnPlaceSelect
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.ui.homeModule.adapter.CheckinSmallPlaceListAdapter
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectCheckInPlaceDialogFragment(
    val listener: OnPlaceSelect,
    private val mLastLocation: Location
) : BaseBottomSheetFragment(), CheckinSmallPlaceListAdapter.PlaceCardInteraction {
    lateinit var viewModel: PlaceViewModel
    lateinit var binding: FragmentCheckinListBinding
    lateinit var mAdapter: CheckinSmallPlaceListAdapter
    val places: ArrayList<PlaceDetails> = ArrayList()
    var searchTerm = ""
    var showingMain = true
    var currentPage = 0
    var prevPage = 0
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
        viewModel = ViewModelProvider(this)[PlaceViewModel::class.java]
        binding.placesRv.layoutManager = LinearLayoutManager(requireContext())
        mAdapter = CheckinSmallPlaceListAdapter(this, requireContext(), places)
        binding.placesRv.adapter = mAdapter
        viewModel.getPlaces(
            mLastLocation.latitude,
            mLastLocation.longitude,
            searchTerm,
            currentPage
        )
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
                    currentPage
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
        binding.createBtn.visibility = View.GONE
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
        hideKeyboard(requireActivity())
    }

    private fun showCreatePostSheet(position: Int) {
        listener.onPlaceSelect(places[position])
        dismiss()
    }


    override val layoutId: Int
        get() = R.layout.fragment_checkin_list
    override val pageTitle: String?
        get() = ""


    override fun onItemClicked(position: Int) {
        showCreatePostSheet(position)
        //Toast.makeText(requireContext(), "item clicked", Toast.LENGTH_SHORT).show()
    }


}