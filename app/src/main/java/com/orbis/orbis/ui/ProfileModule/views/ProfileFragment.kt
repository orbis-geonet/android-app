package com.orbis.orbis.ui.ProfileModule.views


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.databinding.FragmentProfileBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.FeedContent
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.profile.UserPictures
import com.orbis.orbis.models.user.User
import com.orbis.orbis.network.ApiInterface
import com.orbis.orbis.network.SwaggerApiClient
import com.orbis.orbis.ui.ProfileModule.adapter.PhotosAdapter
import com.orbis.orbis.ui.ProfileModule.adapter.ProfilePostsAdapter
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.commentModule.views.CommentDialogFragment
import com.orbis.orbis.ui.commentModule.views.CommentDialogue
import com.orbis.orbis.ui.eventsModule.views.EventDetailsDialogFragment
import com.orbis.orbis.ui.groupsModule.adapter.GroupPostsAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.groupsModule.views.GroupDetailsActivity
import com.orbis.orbis.ui.groupsModule.views.GroupListActivity
import com.orbis.orbis.ui.groupsModule.views.ReportDialog
import com.orbis.orbis.ui.homeModule.views.ProfileSearchActivity
import com.orbis.orbis.ui.messageModule.views.ChatActivity
import com.orbis.orbis.ui.newsFeedModule.adapter.StatusAdapter
import com.orbis.orbis.ui.storiesModule.views.StoryActivity
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.hideKeyboard
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

@AndroidEntryPoint
class ProfileFragment(private val displayName: String = "", private val userkey: String) :
    BaseFragment(), GroupPostsAdapter.PostCardInteraction,
    StatusAdapter.StatusCardInteraction, PhotosAdapter.UserInteraction {
    lateinit var binding: FragmentProfileBinding
    var current_position = 0
    private var isSearchVisible = false
    lateinit var profileViewModel: ProfileViewModel
    val posts: ArrayList<FeedContent> = ArrayList()
    var nextPagePost = ""
    var currentPagePost = ""
    lateinit var feedAdapter: ProfilePostsAdapter
    lateinit var groupViewModel: GroupViewModel
    var exoPlayer: SimpleExoPlayer? = null
    var videoPlayerPosition = 0
    private var currentPagePicture = 0
    private var pictureNextPage = 0
    val userPictures: ArrayList<UserPictures> = ArrayList()
    lateinit var photosAdapter: PhotosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        userPictures.clear()
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        setUpViewPager()
        profileViewModel.getUserProfile(userkey)
        profileViewModel.getUserPictures(currentPagePicture, userkey)
        profileViewModel.getUserIgPictures(0, userkey)
        profileViewModel.getUserFeedFirst(userkey)
        photosAdapter =
            PhotosAdapter(requireContext(), userPictures, this, false, requireActivity())
        feedAdapter = ProfilePostsAdapter(posts, this, this, requireContext())
        binding.profileRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.profileRecycler.adapter = photosAdapter
        setUpViewPager()
        binding.profileRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val total = layoutManager!!.itemCount
                val currentLastItem = layoutManager!!.findLastVisibleItemPosition()
                val firstVisibleItem = layoutManager!!.findFirstVisibleItemPosition()
                if (currentLastItem >= total - 5 && userPictures.size > 1) {
                    if (current_position == 0) {
                        Log.d("paginationStatus", "Loading Photo Page " + pictureNextPage)
                        if (pictureNextPage > currentPagePicture) {
                            profileViewModel.getUserPictures(pictureNextPage, userkey)
                            profileViewModel.getUserIgPictures(pictureNextPage, userkey)
                            currentPagePicture = pictureNextPage
                        }
                    } else {
                        if (exoPlayer != null && firstVisibleItem > videoPlayerPosition) {
                            exoPlayer?.pause()
                        }
                        Log.d(
                            "paginationStatus",
                            "Loading Post page " + nextPagePost + " Previous Page " + currentPagePost
                        )
                        if (currentLastItem >= total - 5 && posts.size > 1) {
                            if (nextPagePost.isEmpty() && currentPagePost != nextPagePost) {
                                profileViewModel.getUserFeed(nextPagePost, userkey)
                                currentPagePost = nextPagePost
                            }
                        }
                    }
                }
            }
        })

        binding.profileRefresh.setOnRefreshListener {
            profileViewModel.getUserProfile(userkey)
            if (current_position == 0) {
                currentPagePicture = 0
                pictureNextPage = 0
                val size = userPictures.size
                userPictures.clear()
                photosAdapter.notifyItemRangeRemoved(0, size)
                profileViewModel.getUserPictures(0, userkey)
                profileViewModel.getUserIgPictures(0, userkey)
            } else {
                if (exoPlayer != null) {
                    exoPlayer?.pause()
                }
                val size = posts.size
                posts.clear()
                feedAdapter.notifyItemRangeRemoved(0, size)
                profileViewModel.getUserFeedFirst(userkey)
            }
        }
//        profileViewModel.getUserPictures(0, userkey)
        binding.connectBtn.setOnClickListener {
            Log.d("followButtonCheck", binding.connectBtn.text.toString())
            if (PrefManager(requireContext()).getIdToken().isNullOrEmpty()) {
                val intent = Intent(requireContext(), AuthActivity::class.java)
                intent.putExtra("goToLogin", true)
                startActivity(intent)
            } else {
                if (binding.connectBtn.text.toString() == getString(R.string.follow)) {
                    profileViewModel.followUser(userkey)
                } else {
                    profileViewModel.unfollowUser(userkey)
                }
            }


        }

        binding.groupCountTv.setOnClickListener {
            val intent = Intent(requireActivity(), GroupListActivity::class.java)
            intent.putExtra("userKey", userkey)
            intent.putExtra("location", Constants.location)
            intent.putExtra("city", userkey) //use mock city to save data in cache as user feed
            startActivity(intent)
        }
        binding.groupsTitle.setOnClickListener {
            val intent = Intent(requireActivity(), GroupListActivity::class.java)
            intent.putExtra("userKey", userkey)
            intent.putExtra("location", Constants.location)
            intent.putExtra("city", userkey)
            startActivity(intent)
        }

        binding.followersTv.setOnClickListener {
            val intent = Intent(requireActivity(), FollowingFollowerActivity::class.java)
            intent.putExtra("userKey", userkey)
            intent.putExtra("isFollower", true)
            startActivity(intent)
        }
        binding.followersTitle.setOnClickListener {
            val intent = Intent(requireActivity(), FollowingFollowerActivity::class.java)
            intent.putExtra("userKey", userkey)
            intent.putExtra("isFollower", true)
            startActivity(intent)
        }
        binding.followingTv.setOnClickListener {
            val intent = Intent(requireActivity(), FollowingFollowerActivity::class.java)
            intent.putExtra("userKey", userkey)
            intent.putExtra("isFollower", false)
            startActivity(intent)
        }
        binding.followingTitle.setOnClickListener {
            val intent = Intent(requireActivity(), FollowingFollowerActivity::class.java)
            intent.putExtra("userKey", userkey)
            intent.putExtra("isFollower", false)
            startActivity(intent)
        }
        binding.messageFab.setOnClickListener {
            if (PrefManager(requireContext()).getIdToken().isNullOrEmpty()) {
                val intent = Intent(requireContext(), AuthActivity::class.java)
                intent.putExtra("goToLogin", true)
                startActivity(intent)
            } else {
                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtra("user", binding.data)
                startActivity(intent)
            }
        }
        binding.settingsIv.setOnClickListener {
            menuMainClick(it)
        }
        setupObservers()
    }

    private fun menuMainClick(user_menu_iv: View) {
        // When user click on the Button 1, create a PopupMenu.
        // And anchor Popup to the Button 2.
        val wrapper: ContextThemeWrapper = ContextThemeWrapper(context, R.style.PopupMenu)
        val popup = PopupMenu(wrapper, user_menu_iv, Gravity.START)

        val menu: Menu = popup.menu
        menu.add(getString(R.string.report))
        menu.add(getString(R.string.block) + " " + binding.data?.displayName)

        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val menuTitle = menuItem.title.toString()
            val spannableString = SpannableString(menuTitle)
            spannableString.setSpan(
                AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0,
                spannableString.length,
                0
            )
            menuItem.title = spannableString
        }
        // Register Menu Item Click event.
        popup.setOnMenuItemClickListener { item -> menuMainItemClicked(item) }
        // Show the PopupMenu.
        popup.show()
    }

    private fun menuMainItemClicked(item: MenuItem): Boolean {
        if (PrefManager(requireContext()).getIdToken().isNullOrEmpty()){
            startActivity(Intent(context, AuthActivity::class.java).putExtra("goToLogin", true))
            return true
        }
        val title = item.title.toString()
        if (title == getString(R.string.report)) {
            reportUser()
        } else if (title == getString(R.string.block) + " " + binding.data?.displayName) {
            blockUser()
        }
        return true
    }

    private fun blockUser() {
        profileViewModel.blockUser(userkey)
    }

    private fun reportUser() {
        val dialog = ReportDialog("User", binding.data?.userKey!!)
        dialog.show(childFragmentManager, "report")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.userNameTv.text = displayName
        binding.backFab.setOnClickListener { requireActivity().onBackPressed() }
        hideKeyboard(requireActivity())
    }
    private fun setupObservers() {
        groupViewModel.attendEvent.observe(viewLifecycleOwner) {
            if (it) {
                getAttendees(pendingAttend)
                posts[pendingAttend].post?.attending = true
            }
            feedAdapter.notifyItemChanged(pendingAttend)
        }
        groupViewModel.unattendEvent.observe(viewLifecycleOwner) {
            if (it) {
                getAttendees(pendingAttend)
                posts[pendingAttend].post?.attending = false
            }
            feedAdapter.notifyItemChanged(pendingAttend)
        }
        profileViewModel.block.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), getString(R.string.user_blocked), Toast.LENGTH_SHORT)
                .show()
            requireActivity().finish()
        }
        profileViewModel.myProfile.observe(viewLifecycleOwner) {
            binding.data = it
            if (it.blocked) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.blocked_user))
                    .setMessage(getString(R.string.something_wrong_fetch))
                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                        requireActivity().finish()
                    }.show()
            }
            if (it.accountPrivate) {
                if (it.following) {
                    binding.privateView.visibility = View.GONE
                    binding.tabLayout.visibility = View.VISIBLE
                    binding.profileRefresh.visibility = View.VISIBLE
                    binding.connectBtn.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.border_black_btn)
                    binding.connectBtn.text = getString(R.string.following)
                    binding.connectBtn.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                } else {
                    binding.privateView.visibility = View.VISIBLE
                    binding.noItem.visibility = View.GONE
                    binding.tabLayout.visibility = View.GONE
                    binding.profileRefresh.visibility = View.GONE
                    binding.connectBtn.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.black_btn)
                    if (it.pending) {
                        binding.connectBtn.background =
                            ContextCompat.getDrawable(requireContext(), R.drawable.border_black_btn)
                        binding.connectBtn.text = getString(R.string.requested)
                        binding.connectBtn.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.black
                            )
                        )

                    } else {
                        binding.connectBtn.text = getString(R.string.follow)
                        binding.connectBtn.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.white
                            )
                        )
                    }

                }
            } else {
                binding.privateView.visibility = View.GONE
                binding.tabLayout.visibility = View.VISIBLE
                binding.profileRefresh.visibility = View.VISIBLE
                if (it.following) {
                    binding.connectBtn.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.border_black_btn)
                    binding.connectBtn.text = getString(R.string.following)
                    binding.connectBtn.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                } else {
                    binding.connectBtn.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.black_btn)
                    binding.connectBtn.text = getString(R.string.follow)
                    binding.connectBtn.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.white
                        )
                    )
                }
            }
            ViewUtils.loadUserProfilePic(
                requireContext(),
                binding.userPicIv,
                it.imageName,
                it.providerImageUrl
            )
        }
        profileViewModel.isLoading.observe(viewLifecycleOwner) {
            if (!it) {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.loading = it
                }, 2000)
            } else {
                binding.loading = it
            }
        }
        groupViewModel.isLoading.observe(viewLifecycleOwner) {
            if (!it) {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.loading = it
                }, 2000)
            } else {
                binding.loading = it
            }
        }
        profileViewModel.follow.observe(viewLifecycleOwner) {
            if (it) {
                profileViewModel.getUserProfile(userkey)
//                binding.connectBtn.background =
//                    ContextCompat.getDrawable(requireContext(), R.drawable.border_black_btn)
//                binding.connectBtn.setTextColor(
//                    ContextCompat.getColor(
//                        requireContext(),
//                        R.color.black
//                    )
//                )
//                if (binding.data?.pending!! && binding.data?.accountPrivate!!) {
//                    binding.connectBtn.text = "Requested"
//                }
//                else {
//                    binding.connectBtn.text = "Following"
//                    binding?.data?.totalFollowers = binding?.data?.totalFollowers!! + 1
//                    binding.followersTv.text = binding?.data?.totalFollowers.toString()
//                    binding.data?.following = true
//                }

            }
        }
        profileViewModel.unFollow.observe(viewLifecycleOwner) {
            if (it) {
                profileViewModel.getUserProfile(userkey)
//                binding.connectBtn.background =
//                    ContextCompat.getDrawable(requireContext(), R.drawable.black_btn)
//                binding.connectBtn.text = "Follow"
//                binding.connectBtn.setTextColor(
//                    ContextCompat.getColor(
//                        requireContext(),
//                        R.color.white
//                    )
//                )
//                if (!binding.data?.accountPrivate!!) {
//                    binding?.data?.totalFollowers = binding?.data?.totalFollowers!! - 1
//                    binding.followersTv.text = binding?.data?.totalFollowers.toString()
//                    binding.data?.following = false
//                }
            }
        }
        profileViewModel.userPictures.observe(viewLifecycleOwner) {
            Log.d("userPictureComes", it.size.toString())
            pictureNextPage++
            val ind = userPictures.size
            userPictures.addAll(it)
            photosAdapter.notifyItemRangeInserted(ind, userPictures.size)
            if (userPictures.isEmpty() && current_position == 0 && binding.privateView.visibility != View.VISIBLE) {
                binding.noItem.visibility = View.VISIBLE
                binding.profileRecycler.visibility = View.GONE
                binding.noPostTv.text = getString(R.string.no_publications)
            } else if (current_position == 0) {
                binding.noItem.visibility = View.GONE
                binding.profileRecycler.visibility = View.VISIBLE
            }
            if (binding.profileRefresh.isRefreshing && current_position == 0) {
                binding.profileRefresh.isRefreshing = false
            }
        }
        profileViewModel.igPictures.observe(viewLifecycleOwner) {
            val ind = userPictures.size
            userPictures.addAll(it)
            if (userPictures.isEmpty() && current_position == 0 && binding.privateView.visibility != View.VISIBLE) {
                binding.noItem.visibility = View.VISIBLE
                binding.profileRecycler.visibility = View.GONE
                binding.noPostTv.text = getString(R.string.no_publications)
            } else if (current_position == 0) {
                binding.noItem.visibility = View.GONE
                binding.profileRecycler.visibility = View.VISIBLE
            }
            photosAdapter.notifyItemRangeInserted(ind, userPictures.size)
        }
        profileViewModel.feed.observe(viewLifecycleOwner) {
            nextPagePost = it.nextPage ?: ""
            val index = posts.size
//            if (binding.swipeRefresh.isRefreshing) {
//                binding.swipeRefresh.isRefreshing = false
//            }
            if (!it.content.isNullOrEmpty()) {
                posts.addAll(it.content)
                if (posts.isEmpty() && current_position == 1) {
                    binding.noItem.visibility = View.VISIBLE
                    binding.noPostTv.text = getString(R.string.no_posts)
                    binding.profileRecycler.visibility = View.GONE
                } else if (current_position == 1) {
                    binding.noItem.visibility = View.GONE
                    binding.profileRecycler.visibility = View.VISIBLE
                }
                for (i in 0 until it.content.size) {
                    val ind = i + index

                    if (posts[ind].type == "POST") {
                        Log.d(
                            "postImageCheck",
                            posts[ind].post?.postKey!! + " " + posts[ind].post?.mediaUrls?.size + " " + ind
                        )

                        if (posts[ind].post?.type == "EVENT") {
                            getAttendees(ind)

                        } else if (posts[ind].post?.type == "VIDEO") {
                            downloadVideo(ind, posts[ind].post)
                        } else if (posts[ind].post?.type == "AUDIO") {
                            downloadAudio(ind, posts[ind].post)
                        }
                    }
                }
                Log.d("myFeedRecieved", it.content.size.toString())
                feedAdapter.notifyDataSetChanged()
            } else {
                if (posts.isEmpty() && current_position == 1) {
                    binding.noItem.visibility = View.VISIBLE
                    binding.noPostTv.text = getString(R.string.no_posts)
                    binding.profileRecycler.visibility = View.GONE
                } else if (current_position == 1) {
                    binding.noItem.visibility = View.GONE
                    binding.profileRecycler.visibility = View.VISIBLE
                }
            }
            if (binding.profileRefresh.isRefreshing && current_position == 1) {
                binding.profileRefresh.isRefreshing = false
            }
        }
    }

    private fun downloadProfilePicture(imageName: String) {
        val storage =
            Firebase.storage.getReference(
                Constants.PROFILE_PICTURES + Utils.getImageUrl200(
                    imageName
                )
            )
        storage.downloadUrl.addOnSuccessListener {
            Picasso.get().load(it).placeholder(R.drawable.ic_user).into(binding.userPicIv)
        }
    }

    private fun findIndex(feedPost: FeedPost): Int {
        for (i in 0 until posts.size) {
            if (posts[i].post?.postKey == feedPost.postKey) {
                return i
            }
        }
        return 0
    }


    private fun downloadAudio(i: Int, post: FeedPost?) {
        try {
            if (!posts[i].post?.mediaUrls.isNullOrEmpty()) {
                val storage =
                    Firebase.storage.getReference(Constants.POST_AUDIO_STORAGE + posts[i].post!!.mediaUrls[0])
                storage.downloadUrl.addOnSuccessListener {
                    try {
                        if (!isPostCreated) {
                            posts[i].post?.postAudio = it.toString()
                            feedAdapter.notifyItemChanged(i)
                        } else {
                            val ind = findIndex(post!!)
                            posts[ind].post?.postAudio = it.toString()
                            feedAdapter.notifyItemChanged(ind)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun downloadVideo(i: Int, post: FeedPost?) {
        try {
            val storage =
                Firebase.storage.getReference(Constants.POST_VIDEO_STORAGE + posts[i].post!!.mediaUrls[0])
            storage.downloadUrl.addOnSuccessListener {
                try {
                    if (!isPostCreated) {
                        posts[i].post?.postVideo = it.toString()
                        feedAdapter.notifyItemChanged(i)
                    } else {
                        val ind = findIndex(post!!)
                        posts[ind].post?.postVideo = it.toString()
                        feedAdapter.notifyItemChanged(ind)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                //viewHolder.binding?.playerView?.hideController()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    val isPostCreated = false


    private fun getAttendees(i: Int) {
        val token = "Bearer " + PrefManager(requireContext()).getIdToken()

        val api = SwaggerApiClient.getClient(requireContext()).create(ApiInterface::class.java)
        api.getAttendees(token, posts[i].post!!.postKey, 0, 4)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<User>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ArrayList<User>) {
                    posts[i].post?.attendedUsers = t
                    feedAdapter.notifyItemChanged(i)
                }

                override fun onError(e: Throwable) {

                }

            })

    }


    private fun setUpViewPager() {
        setupTabs()
        binding.rightButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_search
            )
        )
        binding.rightButton.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileSearchActivity::class.java))
        }
    }

    fun setupTabs() {
        binding.tabLayout.removeAllTabs()
        addNewTab(false, resources.getString(R.string.photos))
        addNewTab(true, resources.getString(R.string.posts))
        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                current_position = tab.position
                when (current_position) {
                    0 -> {
                        binding.profileRecycler.layoutManager =
                            GridLayoutManager(requireContext(), 3)
                        binding.profileRecycler.adapter = photosAdapter
                        if (userPictures.isEmpty()) {
                            Log.d("userPictureOtherProfile", "Not found")
                            binding.noItem.visibility = View.VISIBLE
                            binding.profileRecycler.visibility = View.GONE
                            binding.noPostTv.text = getString(R.string.no_publications)
                        } else {
                            binding.noItem.visibility = View.GONE
                            binding.profileRecycler.visibility = View.VISIBLE
                        }
                    }
                    1 -> {
                        binding.profileRecycler.layoutManager =
                            LinearLayoutManager(requireContext())
                        binding.profileRecycler.adapter = feedAdapter
                        if (posts.isEmpty()) {
                            binding.noItem.visibility = View.VISIBLE
                            binding.profileRecycler.visibility = View.GONE
                            binding.noPostTv.text = getString(R.string.no_posts)
                        } else {
                            binding.profileRecycler.visibility = View.VISIBLE
                            binding.noItem.visibility = View.GONE
                        }

                    }
                }

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    fun addNewTab(isDefaultSelected: Boolean?, title: String) {
        binding.tabLayout.addTab(
            binding.tabLayout.newTab().setText(title).setIcon(null),
            isDefaultSelected!!
        )
    }


    override fun onItemClicked(position: Int, feedPost: FeedPost) {
        TODO("Not yet implemented")
    }

    private fun showCommentSheet(postKey: String) {
        val commentDialogFragment = CommentDialogFragment(postKey, object : CommentDialogue {
            override fun onDismissDialogue() {

            }

            override fun updateCommentCount(postKey: String, count: Int) {
                for (i in 0 until posts.size) {
                    if (posts[i].post?.postKey == postKey) {
                        posts[i].post?.commentsCount = count
                        feedAdapter.notifyItemChanged(i)
                        break
                    }
                }
            }

        })
        commentDialogFragment.show(
            childFragmentManager,
            CommentDialogFragment::class.java.getSimpleName()
        )
    }

    override fun onCommentClicked(position: Int, feedPost: FeedPost) {
        showCommentSheet(feedPost.postKey)
    }

    override fun onLikeClicked(position: Int, feedPost: FeedPost) {
        groupViewModel.likePost(feedPost.postKey)
    }

    override fun onUnlikeClicked(position: Int, feedPost: FeedPost) {
        groupViewModel.unlikePost(feedPost.postKey)
    }

    var pendingAttend = -1
    override fun onEventAttend(position: Int, feedPost: FeedPost) {
        if (PrefManager(requireContext()).getIdToken().isNullOrEmpty()) {
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.putExtra("goToLogin", true)
            startActivity(intent)
        } else {
            pendingAttend = position
            groupViewModel.attendEvent(feedPost.postKey)
        }
    }

    override fun onEventNotAttend(position: Int, feedPost: FeedPost) {
        pendingAttend = position
        groupViewModel.unattendEvent(feedPost.postKey)
    }

    override fun onShare(postKey: String, type: String) {
        groupViewModel.sharePost(postKey, type)
    }

    override fun onPostDelete(position: Int, postKey: String, rootPos: Int) {

    }

    override fun onReport(postKey: String) {
        if (PrefManager(requireActivity()).getIdToken().isNullOrEmpty()) {
            val intent = Intent(requireActivity(), AuthActivity::class.java)
            intent.putExtra("goToLogin", true)
            startActivity(intent)
            return
        }
        val dialog = ReportDialog("Post", postKey)
        dialog.show(requireActivity().supportFragmentManager, "report")
    }

    override fun onPlayerStart(position: Int, player: SimpleExoPlayer) {
        exoPlayer = player
        videoPlayerPosition = position
    }

    override fun onMemberClick() {
        TODO("Not yet implemented")
    }

    override fun onPlaceTitleClick() {
        TODO("Not yet implemented")
    }

    override fun onMainMenuClick(imageView: ImageView) {
        TODO("Not yet implemented")
    }

    override fun onEventClick(position: Int, feedPost: FeedPost) {
        val eventDetails = EventDetailsDialogFragment(feedPost)
        eventDetails.show(childFragmentManager, "eventDetails")
    }

    override fun onCreateSubscriptionClick() {
        TODO("Not yet implemented")
    }

    override fun onSubscriptionActivityClick() {
        TODO("Not yet implemented")
    }

    override fun onSubscriptionClick() {
        TODO("Not yet implemented")
    }

    override fun onStatusClicked() {
        val intent = Intent(requireContext(), StoryActivity::class.java)
        startActivity(intent)
    }

    override fun onTitleClick(group: GroupDetails) {
        val intent = Intent(requireContext(), GroupDetailsActivity::class.java)
        intent.putExtra("data", group)
        intent.putExtra("location", Constants.location)
        startActivity(intent)
    }

    override fun removePhoto(position: Int) {
        TODO("Not yet implemented")
    }


}