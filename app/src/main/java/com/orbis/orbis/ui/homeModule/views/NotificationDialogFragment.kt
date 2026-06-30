package com.orbis.orbis.ui.homeModule.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentNotificationBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.notifications.NotificationModel
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.homeModule.adapter.FollowerRequestAdapter
import com.orbis.orbis.ui.homeModule.adapter.NotificationNewsAdapter
import com.orbis.orbis.ui.newsFeedModule.viewModel.FeedViewModel
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import com.orbis.orbis.ui.placesModule.views.PlaceActivity
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationDialogFragment(val showRequests: Boolean = false) : BaseBottomSheetFragment(),
    NotificationNewsAdapter.NotificationItemClick, FollowerRequestAdapter.FollowerInteraction {
    lateinit var binding: FragmentNotificationBinding
    lateinit var profileViewModel: ProfileViewModel
    private val notifcations: ArrayList<NotificationModel> = ArrayList()
    private val requests: ArrayList<UserInfo> = ArrayList()
    lateinit var notificationNewsAdapter: NotificationNewsAdapter
    lateinit var followerRequestAdapter: FollowerRequestAdapter
    lateinit var placeViewModel: PlaceViewModel
    var currentPage = 0
    var nextPage = 0
    val requestedGroups: HashMap<String, Boolean> = HashMap()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_notification, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        placeViewModel = ViewModelProvider(this)[PlaceViewModel::class.java]
        notificationNewsAdapter = NotificationNewsAdapter(notifcations, requireContext(), this)
        followerRequestAdapter = FollowerRequestAdapter(requests, requireContext(), this)
        binding.notificationRv.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationRv.adapter = notificationNewsAdapter
        setupObservers()
        profileViewModel.getNotifications(0)
        if (Constants.IS_PRIVATE) {
            profileViewModel.getPendingFollowers(0)
        }
        if (Constants.IS_PRIVATE) {
            setupTabs()
        } else {
            binding.tabLayout.visibility = View.GONE
        }
        binding.notificationRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val total = layoutManager!!.itemCount
                val currentLastItem = layoutManager!!.findLastVisibleItemPosition()
                val firstVisibleItem = layoutManager!!.findFirstVisibleItemPosition()

                if (currentLastItem >= total - 5 && notifcations.size > 0) {
                    if (currentPage != nextPage) {
                        profileViewModel.getNotifications(nextPage)
                        if (Constants.IS_PRIVATE) {
                            profileViewModel.getPendingFollowers(nextPage)
                        }
                        currentPage = nextPage
                    }
                }
            }
        })
    }

    var currentTab = 0
    fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                if (currentTab == 0) {
                    binding.notificationRv.adapter = notificationNewsAdapter
                    if (notifcations.size == 0 && currentTab == 0) {
                        binding.noNotificatioIv.visibility = View.VISIBLE
                        binding.noNotificationTv.visibility = View.VISIBLE
                    } else if (currentTab == 0) {
                        binding.noNotificatioIv.visibility = View.GONE
                        binding.noNotificationTv.visibility = View.GONE
                    }
                } else if (currentTab == 1) {
                    binding.notificationRv.adapter = followerRequestAdapter
                    if (requests.size == 0 && currentTab == 1) {
                        binding.noNotificatioIv.visibility = View.VISIBLE
                        binding.noNotificationTv.visibility = View.VISIBLE
                    } else if (currentTab == 1) {
                        binding.noNotificatioIv.visibility = View.GONE
                        binding.noNotificationTv.visibility = View.GONE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                currentTab = tab.position
            }
        })
        if (showRequests) {
            binding.tabLayout.getTabAt(1)?.select()
            requests.clear()
            profileViewModel.getPendingFollowers(0)
        }
    }

    private fun setupObservers() {
        profileViewModel.notifications.observe(viewLifecycleOwner) {
            val ind = notifcations.size
            notifcations.addAll(it)
            notificationNewsAdapter.notifyItemRangeInserted(ind, notifcations.size)
            for (i in ind until notifcations.size) {
                if (notifcations[i].group != null && !notifcations[i].group?.imageName.isNullOrEmpty()) {
                    if (requestedGroups[notifcations[i].group?.groupKey!!] == null || !requestedGroups[notifcations[i].group?.groupKey!!]!!) {
                        requestedGroups[notifcations[i].group?.groupKey!!] = true
                        notifcations[i].post?.postKey?.let { postKey ->
                            placeViewModel.getPost(postKey)
                        }
                    }
                }
            }
            if (notifcations.size == 0 && currentTab == 0) {
                binding.noNotificatioIv.visibility = View.VISIBLE
                binding.noNotificationTv.visibility = View.VISIBLE
            } else if (currentTab == 0) {
                binding.noNotificatioIv.visibility = View.GONE
                binding.noNotificationTv.visibility = View.GONE
            }
            nextPage = currentPage + 1
        }
        profileViewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        profileViewModel.userList.observe(viewLifecycleOwner) {
            val ind = requests.size
            requests.addAll(it)
            followerRequestAdapter.notifyItemRangeInserted(ind, notifcations.size)
            if (requests.size == 0 && currentTab == 1) {
                binding.noNotificatioIv.visibility = View.VISIBLE
                binding.noNotificationTv.visibility = View.VISIBLE
            } else if (currentTab == 1) {
                binding.noNotificatioIv.visibility = View.GONE
                binding.noNotificationTv.visibility = View.GONE
            }
            if (requests.size > 0) {
                binding.tabLayout.getTabAt(1)?.orCreateBadge?.number = requests.size
            } else {
                binding.tabLayout.getTabAt(1)?.removeBadge()
            }

        }
        profileViewModel.followAccept.observe(viewLifecycleOwner) {
            if (it) {
                requests.removeAt(pendingAccept)
                followerRequestAdapter.notifyItemRemoved(pendingAccept)
            }
        }
        placeViewModel.getPost.observe(viewLifecycleOwner) {
            var count = 0
            for (i in 0 until notifcations.size) {
                if (notifcations[i].group != null && notifcations[i].group?.groupKey!! == it.group?.groupKey!!) {
                    count++
                    notifcations[i].group = it.group
                    notificationNewsAdapter.notifyItemChanged(i)
                }
            }
            Log.d("assigningGroup", it.group?.name + " " + count.toString())

        }
    }




    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
        binding.toolbar.titleTv.text = resources.getString(R.string.notification)
        hideKeyboard(requireActivity())
    }

    override val layoutId: Int
        get() = R.layout.fragment_notification
    override val pageTitle: String?
        get() = ""

    companion object {
        fun getInstance(): NotificationDialogFragment {
            return NotificationDialogFragment()
        }
    }

    override fun notificationClick(position: Int) {
        Log.d("notificationClick", position.toString())
        setAsSeen(notifcations[position].notificationKey)
        val intent = Intent(requireContext(), PostDetailsActivity::class.java)
        intent.putExtra("postKey", notifcations[position].post?.postKey)

        startActivity(intent)
        requireActivity().overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun setAsSeen(key: String) {
        val keys: ArrayList<String> = ArrayList()
        keys.add(key)
        profileViewModel.seenPost(keys)
    }

    override fun onCheckinClick(position: Int) {
        setAsSeen(notifcations[position].notificationKey)
        val intent = Intent(requireContext(), PlaceActivity::class.java)
        intent.putExtra("data", notifcations[position].place)
        startActivity(intent)
    }

    override fun setAsSeen(position: Int) {
        setAsSeen(notifcations[position].notificationKey)
        notifcations[position].seen = true
        notificationNewsAdapter.notifyItemChanged(position)
    }

    var pendingAccept = -1
    override fun acceptRequest(position: Int) {
        if (position < requests.size) {
            pendingAccept = position
            profileViewModel.acceptFollow(requests[position].userKey!!)
        }
    }


}