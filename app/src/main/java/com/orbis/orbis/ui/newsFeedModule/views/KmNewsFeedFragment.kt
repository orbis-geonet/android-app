package com.orbis.orbis.ui.newsFeedModule.views

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentKmNewsFeedBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.Feed
import com.orbis.orbis.models.group.FeedContent
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.story.StoryModel
import com.orbis.orbis.models.user.User
import com.orbis.orbis.network.ApiInterface
import com.orbis.orbis.network.SwaggerApiClient
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.commentModule.views.CommentDialogFragment
import com.orbis.orbis.ui.commentModule.views.CommentDialogue
import com.orbis.orbis.ui.eventsModule.views.EventDetailsDialogFragment
import com.orbis.orbis.ui.groupsModule.adapter.GroupPostsAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.groupsModule.views.GroupDetailsActivity
import com.orbis.orbis.ui.groupsModule.views.ReportDialog
import com.orbis.orbis.ui.homeModule.views.MapActivity
import com.orbis.orbis.ui.newsFeedModule.adapter.NewsPostsAdapter
import com.orbis.orbis.ui.newsFeedModule.adapter.StatusAdapter
import com.orbis.orbis.ui.newsFeedModule.viewModel.FeedViewModel
import com.orbis.orbis.ui.placesModule.views.CreatePostDialogFragment
import com.orbis.orbis.ui.storiesModule.adapter.StoryAdapter
import com.orbis.orbis.ui.storiesModule.views.StoryActivity

import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.IndexOutOfBoundsException
import java.util.Date

@AndroidEntryPoint
class KmNewsFeedFragment(val location: Location?, val city: String) : Fragment(),
    GroupPostsAdapter.PostCardInteraction,
    StatusAdapter.StatusCardInteraction, OnPlaceCreate {
    lateinit var binding: FragmentKmNewsFeedBinding
    lateinit var viewModel: FeedViewModel
    lateinit var groupViewModel: GroupViewModel
    val posts: ArrayList<FeedContent> = ArrayList()
    var currentPage = ""
    var nextPage = ""
    var storyPage = 0
    lateinit var feedAdapter: NewsPostsAdapter
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_km_news_feed, container, false)
        initView()
        return binding.root
    }

    private val PAGE_SIZE = 20

    private fun downloadAudio(i: Int, post: FeedPost?) {
        if (i >= posts.size)
            return
        try {
            if (!posts[i].post?.mediaUrls.isNullOrEmpty()) {
                val storage =
                    Firebase.storage.getReference(Constants.POST_AUDIO_STORAGE + posts[i].post!!.mediaUrls[0])
                storage.downloadUrl.addOnSuccessListener {
                    if (!isPostCreated) {
                        try {
                            posts[i].post?.postAudio = it.toString()
                            feedAdapter.notifyItemChanged(i)
                        } catch (e: IndexOutOfBoundsException) {
                            val ind = findIndex(post!!)
                            posts[ind].post?.postAudio = it.toString()
                            feedAdapter.notifyItemChanged(ind)
                        }

                    } else {
                        val ind = findIndex(post!!)
                        posts[ind].post?.postAudio = it.toString()
                        feedAdapter.notifyItemChanged(ind)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    private fun downloadVideo(i: Int, post: FeedPost?) {
        if (i >= posts.size)
            return
        try {
            val storage =
                Firebase.storage.getReference(Constants.POST_VIDEO_STORAGE + posts[i].post!!.mediaUrls[0])
            storage.downloadUrl.addOnSuccessListener {
                if (!isPostCreated) {
                    posts[i].post?.postVideo = it.toString()
                    feedAdapter.notifyItemChanged(i)
                } else {
                    val ind = findIndex(post!!)
                    posts[ind].post?.postVideo = it.toString()
                    feedAdapter.notifyItemChanged(ind)
                }

                //viewHolder.binding?.playerView?.hideController()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }


    }




    private fun getAttendees(i: Int) {
        if (i >= posts.size)
            return

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


    override fun onItemClicked(position: Int, feedPost: FeedPost) {

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

    var pendingDelete = -1
    var rootPosDelete = -1
    override fun onPostDelete(position: Int, postKey: String, rootPos: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_delete_post))
            .setMessage(getString(R.string.delete_sure_post))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                pendingDelete = position
                rootPosDelete = rootPos
                groupViewModel.deletePost(postKey)
            }.setNegativeButton(getString(R.string.no)) { d, _ ->
                d.dismiss()
            }.show()
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

    override fun onResume() {
        super.onResume()
        //viewModel.getNearbyFeedFirst(location!!.longitude, location.latitude)
    }

    var exoPlayer: SimpleExoPlayer? = null
    var videoPlayerPosition = 0
    override fun onPlayerStart(position: Int, player: SimpleExoPlayer) {
        exoPlayer = player
        videoPlayerPosition = position
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

    override fun onMemberClick() {

    }

    override fun onPlaceTitleClick() {

    }

    override fun onMainMenuClick(imageView: ImageView) {

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
        intent.putExtra("location", location)
        startActivity(intent)
    }

    private fun refreshStory() {
        posts[0].stories.clear()
        //feedAdapter.notifyItemChanged(0)
        if (location != null) {
            viewModel.getNearbyStories(storyPage, location.latitude, location.longitude, false)
        }
    }

    private fun initView() {
        binding.loading = true
        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]

        groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        posts.add(FeedContent())

        sharedPreferences = requireContext().getSharedPreferences("feed", Context.MODE_PRIVATE)

        binding.feedRv.layoutManager = LinearLayoutManager(requireContext())
        feedAdapter = NewsPostsAdapter(posts, this, this, requireContext())
        binding.feedRv.adapter = feedAdapter

        setupObservers()

        viewModel.getCachedNearByFeedContent(city)

        binding.swipeRefresh.setOnRefreshListener {
            //binding.loading = true
            val size = posts.size
            posts.clear()
            nextPage = ""
            storyPage = 0
            feedAdapter.notifyItemRangeRemoved(0, size)
            posts.add(FeedContent())
            refreshStory()
            if (location != null){
                viewModel.getNearbyFeedFirst(location.longitude, location.latitude)
                viewModel.deleteCachedNearByFeedContent(city)
            }
        }
        binding.feedRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val total = layoutManager!!.itemCount
                val currentLastItem = layoutManager!!.findLastVisibleItemPosition()
                val firstVisibleItem = layoutManager!!.findFirstVisibleItemPosition()
                Log.d(
                    "paginationCalling",
                    total.toString() + " " + currentLastItem + " " + videoPlayerPosition
                )
                if (exoPlayer != null && firstVisibleItem > videoPlayerPosition) {
                    exoPlayer?.pause()
                }
                if (currentLastItem >= (total * 0.70).toInt() && posts.size > 1) {
                    if (currentPage != nextPage && location != null) {
                        viewModel.getNearbyFeed(location.longitude, location.latitude, nextPage)
                        currentPage = nextPage
                    }
                }
                if (currentLastItem >= (total * 0.97).toInt() && posts.size % PAGE_SIZE == 1)
                    binding.loading = true
            }
        })
        binding.backFab.setOnClickListener {
            requireActivity().finish()
        }
        binding.addFab.setOnClickListener {
            if (PrefManager(requireContext()).getIdToken().isNullOrEmpty()) {
                val intent = Intent(requireContext(), AuthActivity::class.java)
                intent.putExtra("goToLogin", true)
                startActivity(intent)
            } else {
                showCreatePostSheet()
            }
        }
        binding.orbisFab.setOnClickListener {
            goToMap()
        }
        refreshStory()
    }

    private fun showCreatePostSheet() {
        val createPostDialogFragment = CreatePostDialogFragment.newInstance(location = location)
        createPostDialogFragment.listener = this
        createPostDialogFragment.show(
            requireActivity().supportFragmentManager,
            CreatePostDialogFragment::class.java.getSimpleName()
        )
    }

    private fun setupObservers() {
        viewModel.nearbyStories.observe(viewLifecycleOwner) {
            if (posts.isEmpty())
                posts.add(FeedContent())
            posts[0].stories.addAll(it)
            storyPage++
            feedAdapter.notifyItemChanged(0)

            if (!it.isNullOrEmpty() && location != null)
                viewModel.getNearbyStories(storyPage, location.latitude, location.longitude, false)
        }
        viewModel.cachedFeed.observe(viewLifecycleOwner) {
            if (Constants.newKMNewsFeedSession){
                clearFeed()
                Constants.newKMNewsFeedSession = false
                return@observe
            }

            nextPage = it.nextPage
            if (nextPage == "noCacheStored"){
                viewModel.getNearbyFeedFirst(location!!.longitude, location.latitude)
                return@observe
            }

            val index = if (posts.size != 0) posts.size else 1

            if (binding.swipeRefresh.isRefreshing)
                binding.swipeRefresh.isRefreshing = false

            val adInd = posts.size + Constants.appConfig?.feedAdsFrequency!!
            posts.addAll(it.content)

            for (i in 0 until it.content.size - 2) {
                val ind = i + index
                val post = posts[i]
                if (post.type == "POST") {
                    try {
                        when (post.post?.type) {
                            "EVENT" -> getAttendees(ind)
                            "VIDEO" -> downloadVideo(ind, post.post)
                            "AUDIO" -> downloadAudio(ind, post.post)
                        }
                    } catch (e: Exception) {
                        Log.d("myFeedReceived", e.localizedMessage)
                    }
                }
            }

            if (Constants.appConfig?.isAdsEnabled!!) {
                for (i in adInd until posts.size step Constants.appConfig?.feedAdsFrequency!!) {
                    Log.d("addingAdFor", "$i ${it.content.size}")
                    posts.add(i, FeedContent("AD"))
                }
            }
            binding.loading = false
            feedAdapter.notifyDataSetChanged()
        }

        viewModel.feed.observe(viewLifecycleOwner) {
            if (it == null || feedIsEmpty(it)) return@observe

            nextPage = it.nextPage ?: ""
            val index = if (posts.size != 0) posts.size else 1

            if (binding.swipeRefresh.isRefreshing) {
                binding.swipeRefresh.isRefreshing = false
            }
            var adInd = posts.size + Constants.appConfig?.feedAdsFrequency!!
            posts.addAll(it.content)

            for (i in 0 until it.content.size - 2) {
                val ind = i + index
                val post = posts[ind]
                if (post.type == "POST") {
                    try {
                        when (post.post?.type) {
                            "EVENT" -> getAttendees(ind)
                            "VIDEO" -> downloadVideo(ind, post.post)
                            "AUDIO" -> downloadAudio(ind, post.post)
                        }
                    } catch (e: Exception) {
                        Log.d("myFeedReceived", e.localizedMessage)
                    }
                }
            }

            if (Constants.appConfig?.isAdsEnabled!!) {
                for (i in adInd until posts.size step Constants.appConfig?.feedAdsFrequency!!) {
                    Log.d("addingAdFor", "$i ${it.content.size}")
                    posts.add(i, FeedContent("AD"))
                }
            }
            Log.d("myFeedReceived", "kmNewsFeed: " + it.content.size.toString())

            if (posts.size == 0 && it.content.size == 0){
                binding.noItem.visibility = View.VISIBLE
                binding.noPostTv.text = requireContext().getString(R.string.empty_feed_description)
            }

            if (it.content.isNotEmpty())
                viewModel.insertCachedNearByFeedContent(it, city)

            binding.loading = false
            feedAdapter.notifyDataSetChanged()
            //feedAdapter.notifyItemRangeInserted(index, it.content.size)*/
        }
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




        groupViewModel.sharePost.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                Log.d("shareIntent", it.toString())
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.orbis_post_share))
                shareIntent.putExtra(Intent.EXTRA_TEXT, it)
                startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_one)))
            }
        }
        groupViewModel.deletePost.observe(viewLifecycleOwner) {
            if (it) {
                Log.d(
                    "PlacePostType",
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
                feedAdapter.notifyDataSetChanged()
                binding.feedRv.scrollToPosition(rootPosDelete)
                if (location != null)
                    viewModel.restartCachedNearByFeedContentFromAPI(location.longitude, location.latitude, city)
                viewModel.restartCachedMyFeedContentFromAPI(city)
            }
        }
    }

    private fun feedIsEmpty(f: Feed): Boolean {
        if (f.content == null) {
            if (posts.size == 0) {
                binding.noItem.visibility = View.VISIBLE
                binding.noPostTv.text = requireContext().getString(R.string.empty_feed_description)
            }
            binding.swipeRefresh.isRefreshing = false
            binding.loading = false
            return true
        }
        return false
    }

    fun clearFeed(){
        posts.clear()
        viewModel.deleteCachedNearByFeedContent(city)
        feedAdapter.notifyDataSetChanged()
        if (location != null)
            viewModel.getNearbyFeedFirst(location.longitude, location.latitude)
    }

    override fun onPlaceCreate(placeDetails: PlaceDetails) {

    }

    fun goToMap() {
        val intent = Intent(requireContext(), MapActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    var isPostCreated = false
    override fun onPostCreate(feedPost: FeedPost) {
        if (feedPost.type != "CHECK_IN") {
            isPostCreated = true
            val feed = FeedContent()
            feed.type = "POST"
            feed.post = feedPost
            posts.add(1, feed)

            feedAdapter.notifyItemInserted(1)
            binding.feedRv.scrollToPosition(0)
            posts[0].showNoItem = false
            feedAdapter.notifyItemChanged(0)
            if (posts[1].post?.type == "VIDEO") {
                downloadVideo(1, feedPost)
            } else if (posts[1].post?.type == "AUDIO") {
                downloadAudio(1, feedPost)
            }
        } else if (feedPost.type == "CHECK_IN") {
            Constants.IS_CHECKIN = true
            val intent = Intent(requireContext(), MapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            requireActivity().finish()
        }
        posts[0].showNoItem = false
        feedAdapter.notifyItemChanged(0)
        if (location != null)
            viewModel.restartCachedNearByFeedContentFromAPI(location.longitude, location.latitude, city)
        viewModel.restartCachedMyFeedContentFromAPI(city)
    }

}