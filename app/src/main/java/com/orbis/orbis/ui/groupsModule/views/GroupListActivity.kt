package com.orbis.orbis.ui.groupsModule.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.databinding.ActivityGroupListBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.groupsModule.adapter.GroupListAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.homeModule.views.MapActivity
import com.orbis.orbis.ui.newsFeedModule.views.NewsFeedActivity
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class GroupListActivity : BaseActivity(), GroupListAdapter.GroupsCardInteraction {
    private lateinit var locationManager: LocationManager
    lateinit var binding: ActivityGroupListBinding
    lateinit var mAdapter: GroupListAdapter
    lateinit var viewModel: GroupViewModel
    var showingRated = true
    val groups: ArrayList<GroupDetails> = ArrayList()
    var userKey: String? = ""
    var city: String? = ""
    var query = ""
    var searchPage = 0
    var prevSearchPage = -1
    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
            val total = layoutManager!!.itemCount
            val currentLastItem = layoutManager!!.findLastVisibleItemPosition()
            Log.d("paginationCalling", total.toString() + " " + currentLastItem)
            if (currentLastItem >= (total / 2)) {
                if (userKey.isNullOrEmpty()) {
                    if (showingRated) {

                        if (searchPage < 0) {
                            searchPage = 0
                        }
                        if (searchPage != prevSearchPage) {
                            Log.d("callingGroup", "withoutLocation")
                            viewModel.getGroupsWithoutLocation(searchPage)
                            prevSearchPage = searchPage
                        }
                    } else {
                        Log.d("callingGroup", "withLocation")
                        if (searchPage != prevSearchPage) {
                            viewModel.getGroups(
                                location!!.latitude,
                                location!!.longitude,
                                query,
                                searchPage
                            )
                            prevSearchPage = searchPage
                        }
                    }
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout for this fragment
        binding = DataBindingUtil.setContentView(this, R.layout.activity_group_list)
        initView()
    }

    private fun initView() {
        location = intent.getParcelableExtra("location")
        userKey = intent.getStringExtra("userKey")
        city = intent.getStringExtra("city")
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        viewModel = ViewModelProvider(this).get(GroupViewModel::class.java)
        mAdapter = GroupListAdapter(groups, this, this)
        binding.groupsRv.layoutManager = LinearLayoutManager(this)
        binding.groupsRv.adapter = mAdapter
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    showingRated = false
                    groups.clear()
                    searchPage = -1
                    query = ""
                    mAdapter.notifyDataSetChanged()
                    if (location != null && userKey.isNullOrEmpty()) {
                        viewModel.getRatedGroups(location!!.latitude, location!!.longitude)
                    } else if (!userKey.isNullOrEmpty()) {
                        val loc = location?.longitude.toString() + ";" + location?.latitude
                        viewModel.getUserGroups(userKey!!)
                    }
                    hideKeyboard(this@GroupListActivity)
                }
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                showingRated = false
                this@GroupListActivity.query = query
                searchPage = 0
                groups.clear()
                mAdapter.notifyDataSetChanged()

                if (!userKey.isNullOrEmpty()) {
                    viewModel.getGroupsNoLocation(query)
                } else if (location != null) {
                    viewModel.getGroups(location!!.latitude, location!!.longitude, query)
                } else {
                    viewModel.getGroupsNoLocation(query)
                }

                hideKeyboard(this@GroupListActivity)
                return false
            }

        })
        binding.newsFeed.setOnClickListener {
            val intent = Intent(this, NewsFeedActivity::class.java)
            intent.putExtra("location", location)
            intent.putExtra("city", city)
            startActivity(intent)

        }
        binding.groupsRv.addOnScrollListener(scrollListener)
        binding.logo.setOnClickListener {
            finish()
        }
        binding.toolbar.titleTv.text =
            resources.getString(R.string.groups)
        binding.toolbar.backArrowIv.setOnClickListener {
            if (userKey.isNullOrEmpty()) {
                goToMap()

            } else {
                finish()
            }

        }
        binding.addFab.setOnClickListener {
            if (PrefManager(this).getIdToken().isNullOrEmpty()) {
                startActivity(Intent(this, AuthActivity::class.java))
            } else {
                val intent = Intent(this, CreateGroupActivity::class.java)
                intent.putExtra("location", location)
                startActivity(intent)
            }
        }
        binding.swipeRefresh.setOnRefreshListener {
            clearGroups()
        }
        hideKeyboard(this)

        //viewModel.getCachedGroups(city ?: "", !userKey.isNullOrEmpty())
        viewModel.getUserGroups(userKey!!)
        setupObservers()

    }

    fun clearGroups(){
        groups.clear()
        viewModel.deleteCachedGroups(city!!, !userKey.isNullOrEmpty())
        mAdapter.notifyDataSetChanged()
        if (location != null && userKey.isNullOrEmpty())
            viewModel.getRatedGroups(location!!.latitude, location!!.longitude)
    }

    fun goToMap() {
        val intent = Intent(this, MapActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun contain(group: GroupDetails): Boolean {
        for (g in groups) {
            if (g.groupKey == group.groupKey) {
                return true
            }
        }
        return false
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) {
            binding.loading = it
        }
        viewModel.cachedGroups.observe(this) {
            //first session discards cache
            if (Constants.newRankGroupSession){
                clearGroups()
                Constants.newRankGroupSession = false
                return@observe
            }

            for (group in it) {
                if (!contain(group)) {
                    groups.add(group)
                }
            }

            if(it.size == 0){
                if (location != null && userKey.isNullOrEmpty()) {
                    viewModel.getRatedGroups(location!!.latitude, location!!.longitude)
                } else if (!userKey.isNullOrEmpty()) {
                    viewModel.getUserGroups(userKey!!)
                }
            }else
                mAdapter.notifyItemRangeInserted(0, groups.size)

            searchPage = it.size / 25
            prevSearchPage = -1
        }
        viewModel.ratedGroups.observe(this) {
            val index = groups.size
            for (group in it) {
                if (!contain(group)) {
                    groups.add(group)
                }
            }
            if (binding.swipeRefresh.isRefreshing) {
                binding.swipeRefresh.isRefreshing = false
            }
            mAdapter.notifyItemRangeInserted(index, groups.size)
            viewModel.cacheGroups(groups, city ?: "", !userKey.isNullOrEmpty())
            //searchPage = 0
            //prevSearchPage = -1
        }
        viewModel.groupList.observe(this) { items ->
            if (items != null && items.size > 0) {
                searchPage++
                for (group in items) {
                    if (!contain(group)) {
                        groups.add(group)
                    }
                }
                // viewModel.isLoading.postValue(true)
                mAdapter.notifyDataSetChanged()
                //mAdapter.notifyItemRangeInserted(index, groups.size)
                //viewModel.cacheGroups(groups, city ?: "", !userKey.isNullOrEmpty())
            }
            else{
                groups.clear()
                mAdapter.notifyDataSetChanged()
            }
            //downloadImages(0)

        }


    }

    fun contains(group: GroupDetails): Boolean {
        for (g in groups) {
            if (g.groupKey == group.groupKey) {
                return true
            }
        }
        return false
    }
    var location: Location? = null



    override fun onItemClicked(position: Int) {
        if (groups.isNotEmpty()) {
            val intent = Intent(this, GroupDetailsActivity::class.java)
            intent.putExtra("data", groups[position])
            intent.putExtra("location", location)
            startActivity(intent)
        }
    }
}