package com.orbis.orbis.ui.ProfileModule.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.databinding.ActivityFollowingFollowerBinding
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.ui.ProfileModule.adapter.FollowersFollowingAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowingFollowerActivity : BaseActivity(), FollowersFollowingAdapter.UserInteraction {
    lateinit var binding: ActivityFollowingFollowerBinding
    lateinit var profileViewModel: ProfileViewModel
    val users: ArrayList<UserInfo> = ArrayList()
    lateinit var adapter: FollowersFollowingAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_following_follower)

        initView()
    }

    private fun initView() {
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        val title = binding.toolbar.titleTv
        val backArrow = binding.toolbar.backArrowIv
        backArrow.setOnClickListener {
            finish()
        }
        val userKey: String? = intent.getStringExtra("userKey")
        val isFollower: Boolean = intent.getBooleanExtra("isFollower", false)
        val isMyFollower: Boolean = intent.getBooleanExtra("isMyFollower", false)
        if (userKey.isNullOrEmpty()) {
            if (isFollower) {
                profileViewModel.getMyFollower(0)
                title.text = getString(R.string.followers)
            } else {
                title.text = getString(R.string.following)
                profileViewModel.getMyFollowing(0)
            }
        } else {
            if (isFollower) {
                profileViewModel.getUserFollowers(userKey, 0)
                title.text = getString(R.string.followers)
            } else {
                title.text = getString(R.string.following)
                profileViewModel.getUserFollowing(userKey, 0)
            }
        }
        binding.membersRv.layoutManager = LinearLayoutManager(this)
        adapter = FollowersFollowingAdapter(this, users, this, isMyFollower)
        binding.membersRv.adapter = adapter

        setupObservers()

    }

    private fun setupObservers() {
        profileViewModel.isLoading.observe(this) {
            binding.loading = it
        }
        profileViewModel.userList.observe(this) {
            val ind = users.size
            users.addAll(it)
            adapter.notifyItemRangeInserted(ind, users.size)
        }
        profileViewModel.block.observe(this) {
            users.removeAt(pendingBlock)
            adapter.notifyItemRemoved(pendingBlock)
        }

    }

    var pendingBlock: Int = -1
    override fun onBlock(position: Int) {
        pendingBlock = position
        profileViewModel.blockUser(users[position].userKey!!)
    }

    override fun removeFollower(position: Int) {
        pendingBlock = position
        profileViewModel.blockUser(users[position].userKey!!)
    }
}