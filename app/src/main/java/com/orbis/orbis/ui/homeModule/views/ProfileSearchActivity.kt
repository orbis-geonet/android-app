package com.orbis.orbis.ui.homeModule.views

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.databinding.ActivityProfileSearchBinding
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.ui.ProfileModule.adapter.FollowersFollowingAdapter
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileSearchActivity : BaseActivity() {
    lateinit var binding: ActivityProfileSearchBinding
    lateinit var viewModel: ProfileViewModel
    val users: ArrayList<UserInfo> = ArrayList()
    lateinit var usersAdapter: FollowersFollowingAdapter
    var currentPage = 0
    var searchTerm = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile_search)
        initView()
    }

    private fun initView() {
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        val title = binding.toolbar.titleTv
        val backArrow = binding.toolbar.backArrowIv
        title.text = "Users"
        backArrow.setOnClickListener {
            finish()
        }
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
        usersAdapter = FollowersFollowingAdapter(this, users)
        binding.usersRv.layoutManager = LinearLayoutManager(this)
        binding.usersRv.adapter = usersAdapter
        setupObservers()
        binding.usersRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val total = layoutManager!!.itemCount
                val currentLastItem = layoutManager!!.findLastVisibleItemPosition()
                Log.d("paginationCalling", total.toString() + " " + currentLastItem)
                if (currentLastItem == total - 10 && searchTerm.isNotEmpty()) {
                    viewModel.searchUser(searchTerm, currentPage)
                }
            }
        })
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    val ind = users.size
                    users.clear()
                    usersAdapter.notifyItemRangeRemoved(0, ind)
                    hideKeyboard(this@ProfileSearchActivity)
                }
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                searchTerm = query
                users.clear()
                currentPage = 0
                viewModel.searchUser(query, 0)
                hideKeyboard(this@ProfileSearchActivity)
                return false
            }

        })
    }

    private fun setupObservers() {
        viewModel.userList.observe(this) {
            currentPage++
            val ind = users.size
            users.addAll(it)
            usersAdapter.notifyItemRangeInserted(ind, users.size)
        }
        viewModel.isLoading.observe(this) {
            binding.loading = it
        }
    }
}