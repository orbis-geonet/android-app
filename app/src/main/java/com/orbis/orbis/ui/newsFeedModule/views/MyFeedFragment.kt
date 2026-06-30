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
import com.orbis.orbis.databinding.FragmentMyFeedBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.models.Constants
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

@AndroidEntryPoint
class MyFeedFragment : Fragment(),
    GroupPostsAdapter.PostCardInteraction,
    StatusAdapter.StatusCardInteraction,
    OnPlaceCreate {

    companion object {
        private const val TAG = "MyFeedFragment"
        private const val ARG_LOCATION = "arg_location"
        private const val ARG_CITY = "arg_city"
        private const val PAGE_SIZE = 20

        fun newInstance(location: Location, city: String): MyFeedFragment {
            return MyFeedFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_LOCATION, location)
                    putString(ARG_CITY, city)
                }
            }
        }
    }

    private lateinit var location: Location
    private lateinit var city: String

    private var _binding: FragmentMyFeedBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: FeedViewModel
    private lateinit var groupViewModel: GroupViewModel

    private val posts: ArrayList<FeedContent> = ArrayList()

    private var currentPage = ""
    private var nextPage = ""
    private lateinit var feedAdapter: NewsPostsAdapter
    private lateinit var sharedPreferences: SharedPreferences

    private var pendingAttend = -1
    private var pendingDelete = -1
    private var rootPosDelete = -1
    private var isPostCreated = false
    private lateinit var postKey: String

    var exoPlayer: SimpleExoPlayer? = null
    var videoPlayerPosition = 0

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        val loc = args?.getParcelable<Location>(ARG_LOCATION)
        val c = args?.getString(ARG_CITY)

        if (loc == null || c == null) {
            Log.e(TAG, "Required arguments missing — finishing activity.")
            activity?.finish()
            return
        }

        location = loc
        city = c
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Guard: if onCreate() bailed out, loc/city are uninitialised — do nothing.
        if (!::location.isInitialized || !::city.isInitialized) return null

        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_my_feed, container, false)
        initView()
        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        if (posts.size == 1) {
            viewModel.getMyFeedFirst()
        }
        refreshStory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        exoPlayer?.release()
        exoPlayer = null
    }

    // =========================================================================
    // Initialisation
    // =========================================================================

    private fun initView() {
        val b = binding ?: return  // defensive; should never be null here

        b.loading = true
        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]
        groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]

        val topFeed = FeedContent()
        posts.add(topFeed)

        sharedPreferences = requireContext().getSharedPreferences("feed", Context.MODE_PRIVATE)

        b.myFeedRv.layoutManager = LinearLayoutManager(requireContext())
        feedAdapter = NewsPostsAdapter(posts, this, this, requireContext())
        b.myFeedRv.adapter = feedAdapter
        b.myFeedRv.setHasFixedSize(true)

        setupObserver()

        if (posts.size == 0) {
            viewModel.getCachedMyFeedContent(city)
        }

        b.swipeRefresh.setOnRefreshListener {
            val size = posts.size
            posts.clear()
            feedAdapter.notifyItemRangeRemoved(0, size)
            posts.add(FeedContent())
            viewModel.getMyFeedFirst()
            viewModel.deleteCachedMyFeedContent(city)
            refreshStory()
        }

        b.myFeedRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return

                val total = layoutManager.itemCount
                val currentLastItem = layoutManager.findLastVisibleItemPosition()
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                Log.d(TAG, "scroll total=$total last=$currentLastItem videoPos=$videoPlayerPosition")
                if (exoPlayer != null && firstVisibleItem > videoPlayerPosition) {
                    exoPlayer?.pause()
                }
                if (currentLastItem >= (total * 0.70).toInt() && posts.size > 1) {
                    if (currentPage != nextPage && nextPage.isNotEmpty()) {
                        try {
                            viewModel.getMyFeed(nextPage)
                            currentPage = nextPage
                        } catch (e: Exception) {
                            Log.e(TAG, "Pagination error fetching page $nextPage", e)
                        }
                    }
                }

                if (currentLastItem >= total - 2 && posts.size % PAGE_SIZE == 1) {
                    binding?.loading = true
                }
            }
        })

        b.backFab.setOnClickListener { requireActivity().finish() }
        b.addFab.setOnClickListener { showCreatePostSheet() }
        b.orbisFab.setOnClickListener {
            val intent = Intent(requireActivity(), MapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    // =========================================================================
    // Observers
    // =========================================================================

    private fun setupObserver() {
        viewModel.cachedFeed.observe(viewLifecycleOwner) {
            if (Constants.newMyFeedSession) {
                clearFeed()
                Constants.newMyFeedSession = false
                return@observe
            }

            nextPage = it.nextPage ?: ""

            if (nextPage == "noCacheStored") {
                viewModel.getMyFeedFirst()
                return@observe
            }

            val index = posts.size.coerceAtLeast(1)

            binding?.swipeRefresh?.isRefreshing = false

            val appConfig = Constants.appConfig
            if (!it.content.isNullOrEmpty() && appConfig != null) {
                val adInd = posts.size + appConfig.feedAdsFrequency
                posts.addAll(it.content)

                for (i in it.content.indices) {
                    val ind = i + index
                    if (ind >= posts.size) break
                    val post = posts[ind]
                    if (post.type == "POST") {
                        when (post.post?.type) {
                            "EVENT" -> getAttendees(ind)
                            "VIDEO" -> downloadVideo(ind, post.post)
                            "AUDIO" -> downloadAudio(ind, post.post)
                        }
                    }
                }

                if (appConfig.isAdsEnabled) {
                    var i = adInd
                    while (i < posts.size) {
                        Log.d(TAG, "addingAdFor index=$i contentSize=${it.content.size}")
                        posts.add(i, FeedContent("AD"))
                        i += appConfig.feedAdsFrequency
                    }
                }

                Log.d(TAG, "myFeed from cache: ${it.content.size}")
                feedAdapter.notifyDataSetChanged()
            }
            binding?.loading = false
        }

        viewModel.feed.observe(viewLifecycleOwner) {
            nextPage = it.nextPage ?: ""

            val index = posts.size.coerceAtLeast(1)

            binding?.swipeRefresh?.isRefreshing = false

            val appConfig = Constants.appConfig
            if (!it.content.isNullOrEmpty() && appConfig != null) {
                val adInd = posts.size + appConfig.feedAdsFrequency
                posts.addAll(it.content)

                for (i in it.content.indices) {
                    val ind = i + index
                    if (ind >= posts.size) break
                    val post = posts[ind]
                    if (post.type == "POST") {
                        when (post.post?.type) {
                            "EVENT" -> getAttendees(ind)
                            "VIDEO" -> downloadVideo(ind, post.post)
                            "AUDIO" -> downloadAudio(ind, post.post)
                        }
                    }
                }

                if (appConfig.isAdsEnabled) {
                    var i = adInd
                    while (i < posts.size) {
                        Log.d(TAG, "addingAdFor index=$i contentSize=${it.content.size}")
                        posts.add(i, FeedContent("AD"))
                        i += appConfig.feedAdsFrequency
                    }
                }

                Log.d(TAG, "myFeed: ${it.content.size}")
                feedAdapter.notifyDataSetChanged()
                viewModel.insertCachedMyFeedContent(it, city)
            }
            binding?.loading = false
        }

        groupViewModel.attendEvent.observe(viewLifecycleOwner) {
            if (it && pendingAttend in posts.indices) {
                getAttendees(pendingAttend)
                posts[pendingAttend].post?.attending = true
                feedAdapter.notifyItemChanged(pendingAttend)
            }
        }

        groupViewModel.unattendEvent.observe(viewLifecycleOwner) {
            if (it && pendingAttend in posts.indices) {
                getAttendees(pendingAttend)
                posts[pendingAttend].post?.attending = false
                feedAdapter.notifyItemChanged(pendingAttend)
            }
        }

        groupViewModel.sharePost.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                Log.d(TAG, "shareIntent: $it")
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.orbis_post_share))
                    putExtra(Intent.EXTRA_TEXT, it)
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_one)))
            }
        }

        viewModel.newsStories.observe(viewLifecycleOwner) {
            if (posts.isNotEmpty()) {
                posts[0].stories.addAll(it)
                feedAdapter.notifyItemChanged(0)
            }
        }

        groupViewModel.deletePost.observe(viewLifecycleOwner) { deleted ->
            if (!deleted) return@observe

            if (rootPosDelete !in posts.indices) {
                Log.w(TAG, "deletePost: rootPosDelete=$rootPosDelete out of range (size=${posts.size})")
                return@observe
            }

            Log.d(TAG, "PlacePostType: ${posts[rootPosDelete].type} rootPos=$rootPosDelete pendingDelete=$pendingDelete")

            if (posts[rootPosDelete].type == "POST") {
                posts.removeAt(rootPosDelete)
            } else {
                val slider = posts[rootPosDelete].slider
                if (slider.size <= 1) {
                    posts.removeAt(rootPosDelete)
                } else if (pendingDelete in slider.indices) {
                    slider.removeAt(pendingDelete)
                } else {
                    Log.w(TAG, "deletePost: pendingDelete=$pendingDelete out of slider range (size=${slider.size})")
                }
            }

            feedAdapter.notifyDataSetChanged()

            if (posts.size > rootPosDelete) {
                binding?.myFeedRv?.scrollToPosition(rootPosDelete)
            }

            viewModel.restartCachedMyFeedContentFromAPI(city)
            viewModel.restartCachedNearByFeedContentFromAPI(
                location.longitude, location.latitude, city
            )
        }
    }

    // =========================================================================
    // Feed helpers
    // =========================================================================

    fun clearFeed() {
        posts.clear()
        viewModel.deleteCachedMyFeedContent(city)
        feedAdapter.notifyDataSetChanged()
        viewModel.getMyFeedFirst()
    }

    private fun refreshStory() {
        if (posts.isEmpty()) return
        posts[0].stories.clear()
        viewModel.getNewsStories(0, false)
    }

    private fun findIndex(feedPost: FeedPost): Int {
        for (i in posts.indices) {
            if (posts[i].post?.postKey == feedPost.postKey) return i
        }
        return -1
    }

    private fun showCreatePostSheet() {
        val createPostDialogFragment = CreatePostDialogFragment.newInstance(location = location)
        createPostDialogFragment.listener = this
        createPostDialogFragment.show(
            requireActivity().supportFragmentManager,
            CreatePostDialogFragment::class.java.simpleName
        )
    }

    private fun showCommentSheet(postKey: String) {
        val commentDialogFragment = CommentDialogFragment(postKey, object : CommentDialogue {
            override fun onDismissDialogue() {}

            override fun updateCommentCount(postKey: String, count: Int) {
                for (i in posts.indices) {
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
            CommentDialogFragment::class.java.simpleName
        )
    }

    private fun downloadAudio(i: Int, post: FeedPost?) {
        if (i !in posts.indices) return
        try {
            val mediaUrls = posts[i].post?.mediaUrls
            if (mediaUrls.isNullOrEmpty()) return

            Firebase.storage
                .getReference(Constants.POST_AUDIO_STORAGE + mediaUrls[0])
                .downloadUrl
                .addOnSuccessListener { uri ->
                    try {
                        if (!isPostCreated) {
                            if (i in posts.indices) {
                                posts[i].post?.postAudio = uri.toString()
                                feedAdapter.notifyItemChanged(i)
                            }
                        } else {
                            val ind = post?.let { findIndex(it) } ?: -1
                            if (ind != -1 && ind in posts.indices) {
                                posts[ind].post?.postAudio = uri.toString()
                                feedAdapter.notifyItemChanged(ind)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "downloadAudio callback error at index $i", e)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "downloadAudio failed for index $i", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "downloadAudio setup error at index $i", e)
        }
    }

    private fun downloadVideo(i: Int, post: FeedPost?) {
        if (i !in posts.indices) return
        try {
            val mediaUrls = posts[i].post?.mediaUrls
            if (mediaUrls.isNullOrEmpty()) return

            Firebase.storage
                .getReference(Constants.POST_VIDEO_STORAGE + mediaUrls[0])
                .downloadUrl
                .addOnSuccessListener { uri ->
                    try {
                        if (!isPostCreated) {
                            if (i in posts.indices) {
                                posts[i].post?.postVideo = uri.toString()
                                feedAdapter.notifyItemChanged(i)
                            }
                        } else {
                            val ind = post?.let { findIndex(it) } ?: -1
                            if (ind != -1 && ind in posts.indices) {
                                posts[ind].post?.postVideo = uri.toString()
                                feedAdapter.notifyItemChanged(ind)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "downloadVideo callback error at index $i", e)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "downloadVideo failed for index $i", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "downloadVideo setup error at index $i", e)
        }
    }

    private fun getAttendees(i: Int) {
        if (i !in posts.indices) return
        val postKey = posts[i].post?.postKey ?: run {
            Log.w(TAG, "getAttendees: null postKey at index $i")
            return
        }

        val token = "Bearer " + PrefManager(requireContext()).getIdToken()
        val api = SwaggerApiClient.getClient(requireContext()).create(ApiInterface::class.java)

        api.getAttendees(token, postKey, 0, 4)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<User>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onSuccess(t: ArrayList<User>) {
                    if (i in posts.indices) {
                        posts[i].post?.attendedUsers = t
                        feedAdapter.notifyItemChanged(i)
                    }
                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "getAttendees error for index $i", e)
                }
            })
    }

    // =========================================================================
    // GroupPostsAdapter.PostCardInteraction
    // =========================================================================

    override fun onItemClicked(position: Int, feedPost: FeedPost) {}

    override fun onCommentClicked(position: Int, feedPost: FeedPost) {
        showCommentSheet(feedPost.postKey)
    }

    override fun onLikeClicked(position: Int, feedPost: FeedPost) {
        groupViewModel.likePost(feedPost.postKey)
    }

    override fun onUnlikeClicked(position: Int, feedPost: FeedPost) {
        groupViewModel.unlikePost(feedPost.postKey)
    }

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
        this.postKey = postKey
        groupViewModel.sharePost(postKey, type)
    }

    override fun onPostDelete(position: Int, postKey: String, rootPos: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_delete_post))
            .setMessage(getString(R.string.delete_sure_post))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                pendingDelete = position
                rootPosDelete = rootPos
                groupViewModel.deletePost(postKey)
            }
            .setNegativeButton(getString(R.string.no)) { d, _ -> d.dismiss() }
            .show()
    }

    override fun onReport(postKey: String) {
        if (PrefManager(requireActivity()).getIdToken().isNullOrEmpty()) {
            val intent = Intent(requireActivity(), AuthActivity::class.java)
            intent.putExtra("goToLogin", true)
            startActivity(intent)
            return
        }
        ReportDialog("Post", postKey).show(requireActivity().supportFragmentManager, "report")
    }

    override fun onPlayerStart(position: Int, player: SimpleExoPlayer) {
        exoPlayer = player
        videoPlayerPosition = position
    }

    override fun onEventClick(position: Int, feedPost: FeedPost) {
        EventDetailsDialogFragment(feedPost).show(childFragmentManager, "eventDetails")
    }

    override fun onCreateSubscriptionClick() {
        Log.w(TAG, "onCreateSubscriptionClick: not yet implemented")
    }

    override fun onSubscriptionActivityClick() {
        Log.w(TAG, "onSubscriptionActivityClick: not yet implemented")
    }

    override fun onSubscriptionClick() {
        Log.w(TAG, "onSubscriptionClick: not yet implemented")
    }

    // =========================================================================
    // StatusAdapter.StatusCardInteraction
    // =========================================================================

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

    // =========================================================================
    // OnPlaceCreate
    // =========================================================================

    override fun onPlaceCreate(placeDetails: PlaceDetails) {
        // intentionally empty — this fragment doesn't handle place creation
    }

    override fun onPostCreate(feedPost: FeedPost) {
        Log.d(TAG, "onPostCreate type=${feedPost.type}")

        if (feedPost.type == "CHECK_IN") {
            Constants.IS_CHECKIN = true
            val intent = Intent(requireContext(), MapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            requireActivity().finish()
            return
        }

        isPostCreated = true
        val feed = FeedContent().apply {
            type = "POST"
            post = feedPost
        }
        posts.add(1, feed)
        feedAdapter.notifyItemInserted(1)
        binding?.myFeedRv?.scrollToPosition(0)

        if (posts.isNotEmpty()) {
            posts[0].showNoItem = false
            feedAdapter.notifyItemChanged(0)
        }

        when (feedPost.type) {
            "VIDEO" -> downloadVideo(1, feedPost)
            "AUDIO" -> downloadAudio(1, feedPost)
        }

        viewModel.restartCachedMyFeedContentFromAPI(city)
        viewModel.restartCachedNearByFeedContentFromAPI(
            location.longitude, location.latitude, city
        )
    }

    fun goBack() {
        requireActivity().finish()
    }

    override fun onMemberClick() {}
    override fun onPlaceTitleClick() {}
    override fun onMainMenuClick(imageView: ImageView) {}
}