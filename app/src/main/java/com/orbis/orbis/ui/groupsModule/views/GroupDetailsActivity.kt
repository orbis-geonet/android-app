package com.orbis.orbis.ui.groupsModule.views

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ActivityGroupDetailsBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.FeedContent
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.user.User
import com.orbis.orbis.network.ApiInterface
import com.orbis.orbis.network.SwaggerApiClient
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.commentModule.views.CommentDialogFragment
import com.orbis.orbis.ui.commentModule.views.CommentDialogue
import com.orbis.orbis.ui.eventsModule.views.CreateEventDialogGroupFragment
import com.orbis.orbis.ui.eventsModule.views.EventDetailsDialogFragment
import com.orbis.orbis.ui.groupsModule.adapter.GroupPostsAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.homeModule.views.MapActivity
import com.orbis.orbis.ui.placesModule.views.CreatePostDialogFragment
import com.orbis.orbis.ui.subscriptionsModule.create.CreateSubscriptionActivity
import com.orbis.orbis.ui.subscriptionsModule.info.SubscriptionDialogFragment
import com.orbis.orbis.ui.subscriptionsModule.list.SubscriptionsListActivity
import com.orbis.orbis.ui.subscriptionsModule.statistics.StatisticsActivity
import com.orbis.orbis.utils.customViews.BlockGroupDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


@AndroidEntryPoint
class GroupDetailsActivity : AppCompatActivity(),
    GroupPostsAdapter.PostCardInteraction, OnPlaceCreate {
    lateinit var mAdapter: GroupPostsAdapter
    lateinit var binding: ActivityGroupDetailsBinding
    lateinit var viewModel: GroupViewModel

    val posts: ArrayList<FeedContent> = ArrayList()
    val postsBackup: ArrayList<FeedContent> = ArrayList()
    val admins: ArrayList<User> = ArrayList()
    var currentPage = ""
    var nextPage = ""
    var location: Location? = null
    lateinit var groupDetails: GroupDetails
    var isEventSelected: Boolean = false
    var eventPage = 0
    var eventPagePrev = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_group_details)
        groupDetails = intent.getParcelableExtra("data")!!
        location = intent.getParcelableExtra("location")
        if (location == null) {
            location = Constants.location
        }
        Constants.placeTopTitle = "Feed"
        binding.data = groupDetails
        viewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        posts.add(FeedContent("", null, ArrayList(), groupDetails))
        postsBackup.add(FeedContent("", null, ArrayList(), groupDetails))
        binding.postsRv.layoutManager = LinearLayoutManager(this)
        mAdapter =
            GroupPostsAdapter(posts, this, this, groupDetails.isAdmin, supportFragmentManager)
        binding.postsRv.adapter = mAdapter
        binding.postsRv.isNestedScrollingEnabled = false
        viewModel.getGroupFeedFirst(groupDetails.groupKey)
        viewModel.getGroupAdmins(groupDetails.groupKey)
        initView()
        setupObservers()
        downloadedGroupPic = false
        binding.postsRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val total = layoutManager!!.itemCount
                val currentLastItem = layoutManager.findLastVisibleItemPosition()
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
                Log.d(
                    "paginationCalling",
                    total.toString() + " " + currentLastItem + " " + videoPlayerPosition
                )
                if (exoPlayer != null && firstVisibleItem > videoPlayerPosition) {
                    exoPlayer?.pause()
                }
                if (currentLastItem >= total - 5 && posts.size > 1) {
                    if (currentPage != nextPage && !isEventSelected) {
                        viewModel.getGroupFeed(groupDetails.groupKey, nextPage)
                        currentPage = nextPage
                    } else if (eventPagePrev < eventPage && isEventSelected) {
                        viewModel.getEvents(groupDetails.groupKey, eventPage)
                        eventPagePrev = eventPage
                    }
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            val size = posts.size
            posts.clear()
            binding.eventFab.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_event
                )
            )
            checkMembership()
            isEventSelected = false
            Constants.placeTopTitle = getString(R.string.feed)
            posts.add(FeedContent("", null, ArrayList(), groupDetails))
            mAdapter.notifyDataSetChanged()
            viewModel.getGroupFeedFirst(groupDetails.groupKey)
            viewModel.getGroupAdmins(groupDetails.groupKey)
        }
        binding.backFab.setOnClickListener {
            goBack()
        }
    }

    var downloadedGroupPic = false


    private fun menuMainClick(user_menu_iv: ImageView) {
        val wrapper = ContextThemeWrapper(this, R.style.PopupMenu)
        val popup = PopupMenu(wrapper, user_menu_iv, Gravity.START)
        val menu = popup.menu

        val isLoggedIn = !PrefManager(this).getIdToken().isNullOrEmpty()

        if (groupDetails.isMember) {
            menu.add(getString(R.string.leave))
        }

        if (isLoggedIn) {
            if (groupDetails.isFollower) {
                menu.add(getString(R.string.unfollow))
            } else {
                menu.add(getString(R.string.follow))
            }
        }

        if (groupDetails.isAdmin) {
            menu.add(getString(R.string.delete))
            menu.add(getString(R.string.edit))
        }

        if (groupDetails.isMainAdmin && groupDetails.hasSubscription && Constants.currentCountryName == "Brazil") {
            if (groupDetails.isSubscriptionActivate) {
                menu.add(getString(R.string.deactivate_subscription))
            } else {
                menu.add(getString(R.string.activate_subscription))
            }
        }

        popup.inflate(R.menu.layout_group_menu)

        if (isLoggedIn) {
            menu.add(getString(R.string.block))
        }

        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val spannableString = SpannableString(menuItem.title.toString()).apply {
                setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, length, 0)
            }
            menuItem.title = spannableString
        }

        popup.setOnMenuItemClickListener { item -> menuMainItemClicked(item) }
        popup.show()
    }

    private fun menuMainItemClicked(item: MenuItem): Boolean {
        val title = item.title.toString()
        if (title == getString(R.string.leave)) {
            leaveGroup()
        } else if (title == getString(R.string.follow)) {
            followGroup()
        } else if (title == getString(R.string.unfollow)) {
            unfollowGroup()
        } else if (title == getString(R.string.delete)) {
            deleteGroup()
        } else if (title == getString(R.string.report)) {
            reportGroup()
        } else if (title == getString(R.string.edit)) {
            val intent = Intent(this, EditGroupActivity::class.java)
            intent.putExtra("group", groupDetails)
            startActivity(intent)
        } else if (title == getString(R.string.activate_subscription)) {
            activateSubscription()
        } else if (title == getString(R.string.deactivate_subscription)) {
            deactivateSubscription()
        } else if (title == getString(R.string.block)) {
            blockGroup()
        }

        return true
    }

    private fun blockGroup()
    {
        val blockGroupDialogFragment = BlockGroupDialogFragment().apply {
            onYes = {
                viewModel.blockGroup(groupDetails.groupKey)
            }
        }
        blockGroupDialogFragment.show(supportFragmentManager, "BlockGroupDialogFragment")
    }

    private fun reportGroup() {
        if (PrefManager(this).getIdToken().isNullOrEmpty()) {
            val intent = Intent(this, AuthActivity::class.java)
            intent.putExtra("goToLogin", true)
            startActivity(intent)
            return
        }
        val dialog = ReportDialog("Group", groupDetails.groupKey)
        dialog.show(supportFragmentManager, "report")
    }

    private fun deleteGroup() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.confirm_delete_group))
            .setMessage(getString(R.string.sure_delete_group))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.deleteGroup(groupDetails.groupKey)
            }.setNegativeButton(getString(R.string.no)) { d, _ ->
                d.dismiss()
            }.show()
    }

    private fun unfollowGroup() {
        viewModel.unfollowGroup(groupDetails.groupKey)
    }

    private fun followGroup() {
        viewModel.followGroup(groupDetails.groupKey)
    }

    private fun leaveGroup() {
        if (groupDetails.isAdmin && admins.size == 1) {
            Toast.makeText(
                this,
                getString(R.string.assign_new_admin_before_leave),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.are_you_sure))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    viewModel.leaveGroup(groupDetails.groupKey)
                }.setNegativeButton(getString(R.string.no)) { d, _ ->
                    d.dismiss()
                }.show()
        }

    }

    private fun activateSubscription() {
        viewModel.activateSubscription(groupDetails.groupKey)
    }

    private fun deactivateSubscription() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.are_you_sure))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.deactivateSubscription(groupDetails.groupKey, true)
            }.setNegativeButton(getString(R.string.no)) { d, _ ->
                d.dismiss()
            }.show()
    }

    private fun initView() {
        if(groupDetails.isBlockedByUser)
        {
            val blockGroupDialogFragment = BlockGroupDialogFragment(true).apply {
                onYes = {
                    viewModel.unBlockGroup(groupDetails.groupKey)
                }
                onNo = {
                    goBack()
                }
            }
            blockGroupDialogFragment.show(supportFragmentManager, "BlockGroupDialogFragment")
        }

        checkMembership()

        binding.eventFab.setOnClickListener {
            if (!isEventSelected) {
                binding.eventFab.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_news
                    )
                )
                if (groupDetails.isMember) {
                    binding.addFab.setImageDrawable(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_plus
                        )
                    )
                    binding.addFab.setOnClickListener {
                        if (PrefManager(this).getIdToken().isNullOrEmpty()) {
                            val intent = Intent(this, AuthActivity::class.java)
                            intent.putExtra("goToLogin", true)
                            startActivity(intent)
                        } else {
                            val creatEvent = CreateEventDialogGroupFragment(groupDetails, this)
                            creatEvent.show(supportFragmentManager, "createEvent")
                        }

                        //showCreatePostSheet()
                    }
                } else {
                    binding.addFab.setOnClickListener {
                        if (PrefManager(this).getIdToken().isNullOrEmpty()) {
                            val intent = Intent(this, AuthActivity::class.java)
                            intent.putExtra("goToLogin", true)
                            startActivity(intent)
                        } else {
                            viewModel.joinGroup(groupDetails.groupKey)
                        }
                    }
                }
                isEventSelected = true
                Constants.placeTopTitle = getString(R.string.events)
                posts.subList(1, posts.size).clear()
                eventPage = 0
                viewModel.getEvents(groupDetails.groupKey, eventPage)

            } else {
                binding.eventFab.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_event
                    )
                )
                checkMembership()
                isEventSelected = false
                Constants.placeTopTitle = getString(R.string.feed)
                posts.subList(1, posts.size).clear()
                mAdapter.notifyDataSetChanged()
                viewModel.getGroupFeedFirst(groupDetails.groupKey)
                viewModel.getGroupAdmins(groupDetails.groupKey)
            }
        }

    }

    private fun checkMembership() {
        if (groupDetails.isMember) {
            binding.addFab.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_plus
                )
            )
            binding.addFab.setOnClickListener {
                showCreatePostSheet()
            }

        } else {
            binding.addFab.setOnClickListener {
                if (PrefManager(this).getIdToken().isNullOrEmpty()) {
                    val intent = Intent(this, AuthActivity::class.java)
                    intent.putExtra("goToLogin", true)
                    startActivity(intent)
                } else {
                    viewModel.joinGroup(groupDetails.groupKey)
                }
            }
        }
    }

    private fun showCreatePostSheet() {
        val createPostDialogFragment =
            CreatePostDialogFragment.newInstance(group = groupDetails,location = location, restrictGroup = true)
        createPostDialogFragment.listener = this
        createPostDialogFragment.show(
            supportFragmentManager,
            CreatePostDialogFragment::class.java.getSimpleName()
        )
    }

    fun postContains(post: FeedContent): Boolean {
        for (p in posts) {
            if (p.type == "POST" && post.type == "POST") {
                if (p.post!!.postKey == post.post!!.postKey) {
                    return true
                }
            } else if (p.type == "SLIDER" && post.type == "SLIDER") {
                if (p.slider[0].postKey == post.slider[0].postKey) {
                    return true
                }
            }
        }
        return false
    }

    private fun findIndex(feedPost: FeedPost): Int {
        for (i in 0 until posts.size) {
            if (posts[i].post?.postKey == feedPost.postKey) {
                return i
            }
        }
        return 0
    }

    var groupDetailsCalled = false

    @SuppressLint("MissingPermission")
    private fun setupObservers() {
        viewModel.events.observe(this) {
            if (it.isNotEmpty()) {
                posts[0].showNoItem = false
                eventPage++
                val ind = posts.size
                for (event in it) {
                    val feed = FeedContent("POST", event)
                    if (!postContains(feed)) {
                        posts.add(feed)
                        getAttendees(posts.size - 1)
                    }
                }
            } else if (posts.size <= 1) {
                posts[0].showNoItem = true
                posts[0].noItemTitle = getString(R.string.no_event_group)
            }
            mAdapter.notifyDataSetChanged()
        }
        viewModel.feed.observe(this) { it ->
            if (binding.swipeRefresh.isRefreshing) {
                binding.swipeRefresh.isRefreshing = false
            }
            if (!groupDetailsCalled) {
                viewModel.getGroupByKey(groupDetails.groupKey)
                groupDetailsCalled = true
            }
            try {
                Log.d("nextPageKey", it.nextPage)
                if (it != null) {
                    nextPage = it.nextPage
                    Log.d("nextPageKeySaved", nextPage)

                    val index = if (posts.size != 0) {
                        posts.size
                    } else {
                        1
                    }

                    posts.addAll(it.content)
                    postsBackup.clear()
                    postsBackup.addAll(posts)
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

                    mAdapter.notifyDataSetChanged()
                    if (posts.size <= 1) {
                        Log.d("feedIsEmpty", "picked")
                        posts[0].showNoItem = true
                        posts[0].noItemTitle = getString(R.string.no_post_group)
                    } else {
                        posts[0].showNoItem = false
                    }

                } else {
                    if (posts.size <= 1) {
                        Log.d("feedIsEmpty", "picked")
                        posts[0].showNoItem = true
                        posts[0].noItemTitle = getString(R.string.no_post_group)
                    } else {
                        posts[0].showNoItem = false
                    }
                }


            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("feedIsEmpty", posts.size.toString())
                if (posts.size <= 1) {
                    Log.d("feedIsEmpty", "picked")
                    posts[0].showNoItem = true
                    posts[0].noItemTitle = getString(R.string.no_post_group)
                } else {
                    posts[0].showNoItem = false
                }
            }
        }
        viewModel.groupDetails.observe(this) {
            groupDetails = it
            posts[0].group = groupDetails
            mAdapter.notifyDataSetChanged()
            initView()
            checkMembership()
        }
        viewModel.groupAdmins.observe(this) {
            admins.clear()
            admins.addAll(it)
        }
        viewModel.isLoading.observe(this) {
            if (!it) {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.loading = it
                }, 2000)
            } else {
                binding.loading = it
            }
        }
        viewModel.attendEvent.observe(this) {
            if (it) {
                getAttendees(pendingAttend)
                posts[pendingAttend].post?.attending = true
            }
            mAdapter.notifyItemChanged(pendingAttend)
        }
        viewModel.unattendEvent.observe(this) {
            if (it) {
                getAttendees(pendingAttend)
                posts[pendingAttend].post?.attending = false
            }
            mAdapter.notifyItemChanged(pendingAttend)
        }
        viewModel.joinGroup.observe(this) {
            if (it) {
                groupDetails.isFollower = true
                groupDetails.isMember = true

                checkMembership()

                posts[0].group!!.membersCount++
                mAdapter.notifyItemChanged(0)

                mAdapter.notifyItemChanged(0)

                viewModel.joinGroup.value = false
            }
        }
        viewModel.leaveGroup.observe(this) {
            if (it) {
                goBack()
            }
        }
        viewModel.blockGroup.observe(this) {
            if(it)
            {
                goBack()
            }
        }
        viewModel.followGroup.observe(this) {
            if (it) {
                groupDetails.isFollower = true
            }
        }
        viewModel.unfollowGroup.observe(this) {
            if (it) {
                groupDetails.isFollower = false
            }
        }
        viewModel.deleteGroup.observe(this) {
            if (it) {
                Toast.makeText(this, getString(R.string.group_deeted), Toast.LENGTH_SHORT).show()
                goBack()
            }
        }
        viewModel.sharePost.observe(this) {
            if (!it.isNullOrEmpty()) {
                Log.d("shareIntent", it.toString())
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.orbis_post_share))
                shareIntent.putExtra(Intent.EXTRA_TEXT, it)
                startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_one)))
            }
        }
        viewModel.deletePost.observe(this) {
            if (it) {
                Log.d(
                    "GroupPostType",
                    posts[rootPosDelete].type + " " + rootPosDelete + " " + pendingDelete
                )
                if (posts[rootPosDelete].type == "POST") {
                    posts.removeAt(rootPosDelete)
                } else {
                    if (posts[rootPosDelete].slider.size <= 1) {
                        posts.removeAt(rootPosDelete)
                    } else {
                        posts[rootPosDelete].slider.removeAt(pendingDelete)
                    }
                }
                mAdapter.notifyDataSetChanged()
                if (posts.size > rootPosDelete) {
                    binding.postsRv.scrollToPosition(rootPosDelete)
                }
                if (posts.size == 1) {
                    posts[0].noItemTitle = getString(R.string.no_post_group)
                    posts[0].showNoItem = true
                }

            }
        }
        viewModel.activateSubscriptionLiveData.observe(this) {
            mAdapter.setSubscriptionActivatedView(true)
        }
        viewModel.deactivateSubscriptionLiveData.observe(this) {
            mAdapter.setSubscriptionActivatedView(false)
        }
    }

    private fun downloadAudio(i: Int, post: FeedPost?) {
        try {
            if (!posts[i].post?.mediaUrls.isNullOrEmpty()) {
                val storage =
                    Firebase.storage.getReference(Constants.POST_AUDIO_STORAGE + posts[i].post!!.mediaUrls[0])
                storage.downloadUrl.addOnSuccessListener {
                    if (!isPostCreated) {
                        posts[i].post?.postAudio = it.toString()
                        mAdapter.notifyItemChanged(i)
                    } else {
                        val ind = findIndex(post!!)
                        posts[ind].post?.postAudio = it.toString()
                        mAdapter.notifyItemChanged(ind)
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
                        mAdapter.notifyItemChanged(i)
                    } else {
                        val ind = findIndex(post!!)
                        posts[ind].post?.postVideo = it.toString()
                        mAdapter.notifyItemChanged(ind)
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


    private fun getAttendees(i: Int) {
        val token = "Bearer " + PrefManager(this).getIdToken()

        val api = SwaggerApiClient.getClient(this).create(ApiInterface::class.java)
        api.getAttendees(token, posts[i].post!!.postKey, 0, 4)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<User>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ArrayList<User>) {
                    try {
                        posts[i].post?.attendedUsers?.clear()
                        posts[i].post?.attendedUsers?.addAll(t)
                        mAdapter.notifyItemChanged(i)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onError(e: Throwable) {

                }

            })

    }


    fun goBack() {
        finish()
        //navHostFragment.navController.popBackStack()
        //requireActivity().supportFragmentManager.popBackStack()
//        FragmentUtils.launchFragmentWithReverseAnimation(
//            requireActivity(),
//            R.id.container,
//            GroupListFragment(),
//            FragmentUtils.FragmentLaunchMode.REPLACE,
//            true
//        )
    }

    private fun showCommentSheet(postKey: String) {
        val commentDialogFragment = CommentDialogFragment(postKey, object : CommentDialogue {
            override fun onDismissDialogue() {

            }

            override fun updateCommentCount(postKey: String, count: Int) {
                for (i in 0 until posts.size) {
                    if (posts[i].post?.postKey == postKey) {
                        posts[i].post?.commentsCount = count
                        mAdapter.notifyItemChanged(i)
                        break
                    }
                }
            }

        })
        commentDialogFragment.show(
            supportFragmentManager,
            CommentDialogFragment::class.java.getSimpleName()
        )
    }

    override fun onItemClicked(position: Int, feedPost: FeedPost) {

    }

    override fun onCommentClicked(position: Int, feedPost: FeedPost) {
        showCommentSheet(feedPost.postKey)
    }

    override fun onLikeClicked(position: Int, feedPost: FeedPost) {
        viewModel.likePost(feedPost.postKey)
    }

    override fun onUnlikeClicked(position: Int, feedPost: FeedPost) {
        viewModel.unlikePost(feedPost.postKey)
    }

    var pendingAttend = -1
    override fun onEventAttend(position: Int, feedPost: FeedPost) {
        Log.d("attendCalled", "called")
        if (PrefManager(this).getIdToken().isNullOrEmpty()) {
            val intent = Intent(this, AuthActivity::class.java)
            intent.putExtra("goToLogin", true)
            startActivity(intent)
        } else {
            pendingAttend = position
            viewModel.attendEvent(feedPost.postKey)
        }
    }

    override fun onEventNotAttend(position: Int, feedPost: FeedPost) {
        Log.d("unattendCalled", "called")
        pendingAttend = position
        viewModel.unattendEvent(feedPost.postKey)
    }

    override fun onShare(postKey: String, type: String) {
        viewModel.sharePost(postKey, type)
    }

    var pendingDelete = -1
    var rootPosDelete = -1
    override fun onPostDelete(position: Int, postKey: String, rootPos: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.confirm_delete_post))
            .setMessage(getString(R.string.delete_sure_post))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                pendingDelete = position
                rootPosDelete = rootPos
                viewModel.deletePost(postKey)
            }.setNegativeButton(getString(R.string.no)) { d, _ ->
                d.dismiss()
            }.show()

    }

    override fun onReport(postKey: String) {
        if (PrefManager(this).getIdToken().isNullOrEmpty()) {
            val intent = Intent(this, AuthActivity::class.java)
            intent.putExtra("goToLogin", true)
            startActivity(intent)
            return
        }
        val dialog = ReportDialog("Post", postKey)
        dialog.show(supportFragmentManager, "report")
    }


    var exoPlayer: SimpleExoPlayer? = null
    var videoPlayerPosition = 0
    override fun onPlayerStart(position: Int, player: SimpleExoPlayer) {
        exoPlayer = player
        videoPlayerPosition = position
    }

    override fun onMemberClick() {
        val intent = Intent(this, MembersListFragment::class.java)
        intent.putExtra("groupKey", groupDetails.groupKey)
        intent.putExtra("isAdmin", groupDetails.isAdmin)
        startActivity(intent)
    }

    override fun onPlaceTitleClick() {
        val intent = Intent(this, OwnedPlaceFragment::class.java)
        intent.putExtra("groupKey", groupDetails.groupKey)
        intent.putExtra("location", location)
        startActivity(intent)
    }

    override fun onMainMenuClick(imageView: ImageView) {
        menuMainClick(imageView)
    }

    override fun onEventClick(position: Int, feedPost: FeedPost) {
        val eventDetails = EventDetailsDialogFragment(feedPost)
        eventDetails.show(supportFragmentManager, "eventDetails")
    }

    override fun onCreateSubscriptionClick() {
        if (groupDetails.hasSubscription) {
            SubscriptionsListActivity.open(this, groupDetails.groupKey, groupDetails.isMainAdmin)
        } else {
            CreateSubscriptionActivity.open(this, groupDetails.groupKey, null)
        }
    }

    override fun onSubscriptionActivityClick() {
        StatisticsActivity.open(this, groupDetails.groupKey)
    }

    override fun onSubscriptionClick() {
        SubscriptionsListActivity.open(this, groupDetails.groupKey, groupDetails.isMainAdmin)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == CreateSubscriptionActivity.ACTIVITY_RESULT_CODE) {
                mAdapter.setSubscriptionCreatedView(true)
            } else if (requestCode == SubscriptionsListActivity.ACTIVITY_RESULT_CODE) {
                mAdapter.setSubscriptionSubscriberView(true)
                SubscriptionDialogFragment.newInstance(
                    R.drawable.ic_subscription_2,
                    getString(R.string.congratulations_you_successfully_subscribed),
                    getString(R.string.ok)
                ).show(supportFragmentManager, "SubscriptionDialogFragment")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (exoPlayer != null) {
            exoPlayer?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (exoPlayer != null) {
            exoPlayer?.release()
        }
    }

    override fun onPlaceCreate(placeDetails: PlaceDetails) {

    }

    var isPostCreated = false
    override fun onPostCreate(feedPost: FeedPost) {

        if (feedPost.type != "CHECK_IN") {
            isPostCreated = true
            val feed = FeedContent()
            feed.type = "POST"
            feed.post = feedPost
            posts.add(1, feed)
            postsBackup.add(1, feed)
            mAdapter.notifyItemInserted(1)
            binding.postsRv.scrollToPosition(0)
            posts[0].showNoItem = false
            mAdapter.notifyItemChanged(0)
            if (posts[1].post?.type == "VIDEO") {
                downloadVideo(1, feedPost)
            } else if (posts[1].post?.type == "AUDIO") {
                downloadAudio(1, feedPost)
            }
        } else if (feedPost.type == "CHECK_IN") {
            Constants.IS_CHECKIN = true
            val intent = Intent(this, MapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
        posts[0].showNoItem = false
        mAdapter.notifyItemChanged(0)
    }
}