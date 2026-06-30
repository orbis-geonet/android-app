package com.orbis.orbis.ui.groupsModule.views

import android.location.Location
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentOwnedPlaceBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.ui.groupsModule.adapter.OwnedPlaceAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OwnedPlaceFragment : AppCompatActivity() {

    lateinit var binding: FragmentOwnedPlaceBinding
    lateinit var viewModel: GroupViewModel
    lateinit var mAdapter: OwnedPlaceAdapter
    val places: ArrayList<PlaceDetails> = ArrayList()
    val backupPlaces: ArrayList<PlaceDetails> = ArrayList()
    var currentPage = 0
    lateinit var location: Location
    private var showingSearchResult = false
    var groupKey = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout for this fragment

        binding = DataBindingUtil.setContentView(this, R.layout.fragment_owned_place)
        initView()
    }

    private fun initView() {
        viewModel = ViewModelProvider(this).get(GroupViewModel::class.java)
        groupKey = intent.getStringExtra("groupKey")!!
        location = intent.getParcelableExtra("location")!!
        val title = binding.toolbar.titleTv
        val backArrow = binding.toolbar.backArrowIv
        title.text = getString(R.string.owned_place)
        backArrow.setOnClickListener {
            finish()
        }
        mAdapter = OwnedPlaceAdapter(this, places)
        binding.placesRv.layoutManager = LinearLayoutManager(this)
        binding.placesRv.adapter = mAdapter
        setupObservers()
        viewModel.getPlaceOwnedByGroup(groupKey, location.latitude, location.longitude, currentPage)

        binding.placesRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val total = layoutManager!!.itemCount
                val currentLastItem = layoutManager!!.findLastVisibleItemPosition()
                if (currentLastItem == total - 10 && !showingSearchResult) {
                    viewModel.getPlaceOwnedByGroup(
                        groupKey,
                        location.latitude,
                        location.longitude,
                        currentPage
                    )
                }
            }
        })


        binding.searchPlace.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    places.clear()
                    showingSearchResult = false
                    places.addAll(backupPlaces)
                    mAdapter.notifyDataSetChanged()
                    hideKeyboard(this@OwnedPlaceFragment)
                }
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                showingSearchResult = true
                places.clear()
                for (place in backupPlaces) {
                    if (place.name.lowercase().contains(query.lowercase())) {
                        places.add(place)
                    }
                }
                mAdapter.notifyDataSetChanged()
                hideKeyboard(this@OwnedPlaceFragment)
                return false
            }

        })
    }

    private fun setupObservers() {
        viewModel.ownedPlaces.observe(this) {
            currentPage++
            val index = places.size
            places.addAll(it)
            backupPlaces.addAll(it)

            mAdapter.notifyItemRangeInserted(index, places.size)
        }
        viewModel.isLoading.observe(this) {
            binding.loading = it
        }
    }


}