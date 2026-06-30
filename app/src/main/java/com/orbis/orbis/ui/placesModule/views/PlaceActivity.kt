package com.orbis.orbis.ui.placesModule.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentPlaceBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.models.BigDataSharConstants
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.Constants.location
import com.orbis.orbis.models.group.FeedContent
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.place.PlaceUpdateBody
import com.orbis.orbis.models.place.RatePlaceBody
import com.orbis.orbis.models.place.toPlaceDetails
import com.orbis.orbis.models.user.User
import com.orbis.orbis.network.ApiInterface
import com.orbis.orbis.network.SwaggerApiClient
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.commentModule.views.CommentDialogFragment
import com.orbis.orbis.ui.commentModule.views.CommentDialogue
import com.orbis.orbis.ui.eventsModule.views.CreateEventDialogFragment
import com.orbis.orbis.ui.eventsModule.views.CreateEventDialogGroupFragment
import com.orbis.orbis.ui.eventsModule.views.EventDetailsDialogFragment
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.groupsModule.views.GroupDetailsActivity
import com.orbis.orbis.ui.groupsModule.views.ReportDialog
import com.orbis.orbis.ui.homeModule.views.MapActivity
import com.orbis.orbis.ui.newsFeedModule.adapter.StatusAdapter
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import com.orbis.orbis.ui.placesModule.adapter.GroupIconAdapter
import com.orbis.orbis.ui.placesModule.adapter.PlacePostsAdapter
import com.orbis.orbis.utils.PermissionUtil
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.hideKeyboard
import com.orbis.orbis.utils.picker.Picker
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlin.Exception
import kotlin.collections.ArrayList

@AndroidEntryPoint
class PlaceActivity : AppCompatActivity(),
    GroupIconAdapter.GroupIconCardInteraction,
    PlacePostsAdapter.PostCardInteraction, StatusAdapter.StatusCardInteraction, OnPlaceCreate,
    PlaceDescriptionDialog.OnEditDescription {

    private val picker = Picker()
    lateinit var placeDetails: PlaceDetails
    lateinit var binding: FragmentPlaceBinding
    private var mGroupIconAdapter: GroupIconAdapter? = null
    lateinit var mAdapter: PlacePostsAdapter
    private val posts: ArrayList<FeedContent> = ArrayList()
    private val postsBackup: ArrayList<FeedContent> = ArrayList()
    lateinit var placeViewModel: PlaceViewModel
    lateinit var groupViewModel: GroupViewModel
    var currentPage = ""
    var nextPage = ""
    var eventPage = 0
    var eventPagePrev = 0
    var isPostCreated = false
    var exoPlayer: SimpleExoPlayer? = null
    var videoPlayerPosition = 0
    var isEventSelected: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.fragment_place)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }
        initView()
    }

    fun goToMap() {
        val intent = Intent(this, MapActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    private fun isIn1km(): Boolean {
        try {
            val results: FloatArray = FloatArray(1)
            Location.distanceBetween(
                location!!.latitude,
                location!!.longitude,
                placeDetails.coordinates?.latitude!!,
                placeDetails.coordinates?.longitude!!,
                results
            )
            if (results[0] < 1000) {
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    private fun initView()
    {
        picker.populate(activity = this)

        Constants.placeTopTitle = "Feed"
        placeViewModel = ViewModelProvider(this)[PlaceViewModel::class.java]
        groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        placeDetails = intent.getParcelableExtra("data")!!
        binding.data = placeDetails
        Log.d("openedPlaceKey", placeDetails.placeKey)
        placeViewModel.getPlaceFeedFirst(placeDetails?.placeKey!!)
        placeViewModel.getNewPlace(placeDetails?.placeKey!!)
        binding.placeMenu.setOnClickListener {
            val wrapper: ContextThemeWrapper = ContextThemeWrapper(this, R.style.PopupMenu)
            val popup = PopupMenu(wrapper, it, Gravity.START)

            val menu: Menu = popup.getMenu()
            popup.inflate(R.menu.layout_group_menu)
            popup.setOnMenuItemClickListener { item -> menuMainItemClicked(item) }
            // Show the PopupMenu.
            popup.show()
        }

        binding.swipeRefresh.setOnRefreshListener {
            posts.clear()
            postsBackup.clear()
            posts.add(FeedContent("", null, ArrayList(), null, placeDetails))
            postsBackup.add(FeedContent("", null, ArrayList(), null, placeDetails))
            binding.eventFab.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_event
                )
            )
            if (isIn1km()) {
                binding.logoCardview.visibility = View.VISIBLE
                binding.addFab2.visibility = View.GONE
                binding.logoFab.layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT
                binding.logoFab.layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
                binding.logoFab.requestLayout()
                binding.logoFab.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_checkin_plus
                    )
                )
            } else {
                binding.logoCardview.visibility = View.GONE
                binding.addFab2.visibility = View.VISIBLE

            }
            isEventSelected = false
            Constants.placeTopTitle = getString(R.string.feed)
            mAdapter.notifyDataSetChanged()
            placeViewModel.getNewPlace(placeDetails?.placeKey!!)
            placeViewModel.getPlaceFeedFirst(placeDetails?.placeKey ?: "")
        }

        if (isIn1km()) {
            binding.logoCardview.visibility = View.VISIBLE
            binding.addFab2.visibility = View.GONE
            binding.logoFab.layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT
            binding.logoFab.layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
            binding.logoFab.requestLayout()
            binding.logoFab.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_checkin_plus
                )
            )
        } else {
            binding.logoCardview.visibility = View.GONE
            binding.addFab2.visibility = View.VISIBLE

        }
        binding.addFab2.setOnClickListener {
            if (isEventSelected) {
                if (PrefManager(this).getIdToken().isNullOrEmpty()) {
                    val intent = Intent(this, AuthActivity::class.java)
                    intent.putExtra("goToLogin", true)
                    startActivity(intent)
                } else {
                    val creatEvent = CreateEventDialogFragment(placeDetails, this)
                    creatEvent.show(supportFragmentManager, "createEvent")
                }
            } else {
                if (PrefManager(this).getIdToken().isNullOrEmpty()) {
                    val intent = Intent(this, AuthActivity::class.java)
                    intent.putExtra("goToLogin", true)
                    startActivity(intent)
                } else {
                    showCreatePostSheet()
                }

            }
        }
        binding.logoCardview.setOnClickListener {
            if (PrefManager(this).getIdToken().isNullOrEmpty()) {
                val intent = Intent(this, AuthActivity::class.java)
                intent.putExtra("goToLogin", true)
                startActivity(intent)
            }else
                showCreatePostSheet()
        }
        binding.backFab.setOnClickListener { goToMap() }
//        progress_horizontal.withAnimation(1000)
        hideKeyboard(this)
        posts.clear()
        postsBackup.clear()
        posts.add(FeedContent("", null, ArrayList(), null, placeDetails))
        postsBackup.addAll(posts)
        mAdapter =
            PlacePostsAdapter(posts, this, this, this, this, supportFragmentManager, this)
        binding.postsRv.layoutManager = LinearLayoutManager(this)
        binding.postsRv.adapter = mAdapter
        binding.postsRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
                if (currentLastItem >= total - 5 && posts.size > 1) {
                    if (currentPage != nextPage && !isEventSelected) {
                        placeViewModel.getPlaceFeed(placeDetails?.placeKey!!, nextPage)
                        currentPage = nextPage
                    } else if (eventPagePrev < eventPage && isEventSelected) {
                        placeViewModel.getEvents(placeDetails.placeKey, eventPage)
                        eventPagePrev = eventPage
                    }
                }
            }
        })
        binding.eventFab.setOnClickListener {
            if (!isEventSelected) {
                binding.eventFab.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_news
                    )
                )
                binding.logoCardview.visibility = View.GONE
                binding.addFab2.visibility = View.VISIBLE
                isEventSelected = true
                Constants.placeTopTitle = getString(R.string.event)
                posts.subList(1, posts.size).clear()
                eventPage = 0
                placeViewModel.getEvents(placeDetails.placeKey, eventPage)

            } else {
                binding.eventFab.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_event
                    )
                )
                if (isIn1km()) {
                    binding.logoCardview.visibility = View.VISIBLE
                    binding.addFab2.visibility = View.GONE
                    binding.logoFab.layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT
                    binding.logoFab.layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
                    binding.logoFab.requestLayout()
                    binding.logoFab.setImageDrawable(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_checkin_plus
                        )
                    )
                } else {
                    binding.logoCardview.visibility = View.GONE
                    binding.addFab2.visibility = View.VISIBLE
                }
                isEventSelected = false
                Constants.placeTopTitle = getString(R.string.feed)
                posts.subList(1, posts.size).clear()
                mAdapter.notifyDataSetChanged()
                placeViewModel.getNewPlace(placeDetails.placeKey)
                placeViewModel.getPlaceFeedFirst(placeDetails.placeKey)
            }
        }

        setupObservers()

    }

    private fun menuMainItemClicked(item: MenuItem): Boolean {
        val dialog = ReportDialog("Place", placeDetails.placeKey)
        dialog.show(supportFragmentManager, "report")
        return true
    }

    private var resultUri: Uri? = null

    private fun setupObservers() {
        groupViewModel.sharePost.observe(this) {
            if (!it.isNullOrEmpty()) {
                Log.d("shareIntent", it.toString())
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.orbis_post_share))
                shareIntent.putExtra(Intent.EXTRA_TEXT, it)
                startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_one)))
            }
        }
        placeViewModel.events.observe(this) {
            if (it.isNotEmpty()) {
                posts[0].showNoItem = false
                eventPage++
                for (event in it) {
                    val feed = FeedContent("POST", event)
                    posts.add(feed)
                    getAttendees(posts.size - 1)
                }
            } else if (posts.size == 1) {
                posts[0].showNoItem = true
                posts[0].noItemTitle = getString(R.string.no_event_place)
            }
            mAdapter.notifyDataSetChanged()
        }
        groupViewModel.deletePost.observe(this) {
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
                mAdapter.notifyDataSetChanged()
                if (posts.size > rootPosDelete) {
                    binding.postsRv.scrollToPosition(rootPosDelete)
                }
                if (posts.size == 1) {
                    posts[0].noItemTitle = getString(R.string.no_post_place)
                    posts[0].showNoItem = true
                    mAdapter.notifyItemChanged(0)
                }

            }
        }
        placeViewModel.newAddedPlace.observe(this) {
            binding.data = it
            posts[0].place = it
            placeDetails = it
            mAdapter.notifyItemChanged(0)
        }
        placeViewModel.placeUnfollowed.observe(this) {
            if (it) {
                placeDetails?.following = false
                posts[0].place = placeDetails
                mAdapter.notifyItemChanged(0)
            }
        }
        placeViewModel.placeFollowed.observe(this) {
            if (it) {
                placeDetails?.following = true
                posts[0].place = placeDetails
                mAdapter.notifyItemChanged(0)
            }
        }
        placeViewModel.isLoading.observe(this) {
            binding.loading = it
        }

        groupViewModel.isLoading.observe(this) {
            binding.loading = it
        }
        groupViewModel.attendEvent.observe(this) {
            if (it) {
                getAttendees(pendingAttend)
                posts[pendingAttend].post?.attending = true
            }
            mAdapter.notifyItemChanged(pendingAttend)
        }
        groupViewModel.unattendEvent.observe(this) {
            if (it) {
                getAttendees(pendingAttend)
                posts[pendingAttend].post?.attending = false
            }
            mAdapter.notifyItemChanged(pendingAttend)
        }
        placeViewModel.placeDetails.observe(this) {

            if (it.imageName.isNotEmpty()) {
                posts[0].place?.imageName = ""
                posts[0].place?.imageName = it.imageName
//                Toast.makeText(this, getString(R.string.place_picture_uploaded), Toast.LENGTH_LONG)
//                    .show()
            }
            if (it.description.isNotEmpty()) {
                posts[0].place?.description = it.description
            }
            mAdapter.notifyItemChanged(0)
        }

        placeViewModel.error.observe(this) {
            if (!it.isNullOrEmpty()) {
                Toast.makeText(this@PlaceActivity, it, Toast.LENGTH_SHORT).show()
            }
        }
        placeViewModel.feed.observe(this) {
            try {
                binding.swipeRefresh.isRefreshing = false
                Log.d("nextPageKey", it.nextPage)
                if (it != null) {
                    nextPage = it.nextPage
                    Log.d("nextPageKeySaved", nextPage)

                    val index = if (posts.size != 0) {
                        posts.size
                    } else {
                        1
                    }
                    for (p in it.content) {
//                        if (!posts[0].downloadedGroupPhoto.isNullOrEmpty()) {
//                            p.downloadedGroupPhoto = posts[0].downloadedGroupPhoto
//                        }
                        posts.add(p)
                    }
                    postsBackup.addAll(posts.subList(1, posts.size))
                    //downloadGroupPhoto()
                    for (i in 0 until it.content.size) {
                        val ind = i + index


                        if (posts[ind].type == "POST") {

                            if (posts[ind].post?.type == "EVENT") {
                                getAttendees(ind)

                            } else if (posts[ind].post?.type == "VIDEO") {
                                downloadVideo(ind, posts[ind].post)

                            } else if (posts[ind].post?.type == "AUDIO") {
                                downloadAudio(ind, posts[ind].post)
                            }
                        }
                    }

                    mAdapter.notifyItemRangeInserted(index, posts.size)
                    if (posts.size == 1) {
                        Log.d("feedIsEmpty", "picked")
                        posts[0].showNoItem = true
                        posts[0].noItemTitle = getString(R.string.no_post_place)
                    } else if (posts.size > 1) {
                        posts[0].showNoItem = false
                    }
                    mAdapter.notifyItemChanged(0)

                } else {
                    if (posts.size == 1) {
                        Log.d("feedIsEmpty", "picked")
                        posts[0].showNoItem = true
                        posts[0].noItemTitle = getString(R.string.no_post_place)
                    } else if (posts.size > 1) {
                        posts[0].showNoItem = false
                    }

                    mAdapter.notifyItemChanged(0)
                }


            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("feedIsEmpty", posts.size.toString())
                if (posts.size == 1) {
                    Log.d("feedIsEmpty", "picked")
                    posts[0].showNoItem = true
                    posts[0].noItemTitle = getString(R.string.no_post_place)
                } else if (posts.size > 1) {
                    posts[0].showNoItem = false
                }
                mAdapter.notifyItemChanged(0)
            }
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

    private fun findIndex(feedPost: FeedPost): Int {
        for (i in 0 until posts.size) {
            if (posts[i].post?.postKey == feedPost.postKey) {
                return i
            }
        }
        return 0
    }


    var tryCount = 0

    private fun downloadVideo(i: Int, post: FeedPost?) {
        try {

            //  Log.d("videoThumbPath",thumbPath)
            val storage =
                Firebase.storage.getReference(Constants.POST_VIDEO_STORAGE + posts[i].post!!.mediaUrls[0])
            storage.downloadUrl.addOnSuccessListener {
                if (!isPostCreated) {
                    try {
                        posts[i].post?.postVideo = it.toString()
                        mAdapter.notifyItemChanged(i)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    try {
                        val ind = findIndex(post!!)
                        posts[ind].post?.postVideo = it.toString()
                        mAdapter.notifyItemChanged(ind)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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

    private fun showCreatePostSheet() {
        try {
            val createPostDialogFragment =
                CreatePostDialogFragment.newInstance(
                    placeDetails = placeDetails,
                    isCheckin = false,
                    location = location,
                    restrictPage = true
                )
            createPostDialogFragment.listener = this
            createPostDialogFragment.show(
                supportFragmentManager,
                CreatePostDialogFragment::class.java.getSimpleName()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    override fun onIconClicked(group: GroupDetails) {
        val intent = Intent(this, GroupDetailsActivity::class.java)
        intent.putExtra("data", group)
        intent.putExtra("location", location)
        startActivity(intent)
    }


    override fun onPlayerStart(position: Int, player: SimpleExoPlayer) {
        exoPlayer = player
        videoPlayerPosition = position
    }

    override fun onMemberClick() {

    }

    override fun onPlaceTitleClick() {

    }

    override fun onMainMenuClick(imageView: ImageView) {

    }

    override fun followPlace() {
        if (PrefManager(this@PlaceActivity).getIdToken().isNullOrEmpty()) {
            val intent = Intent(this@PlaceActivity, AuthActivity::class.java)
            intent.putExtra("goToLogin", true)
            startActivity(intent)
        } else {
            if (placeDetails?.following!!) {
                placeViewModel.unfollowPlace(placeDetails?.placeKey!!)
            } else {
                placeViewModel.followPlace(placeDetails?.placeKey!!)
            }
        }
    }

    override fun uploadPlacePhoto() {
        onClickedPickPhoto()
    }

    //region onClicked
    private fun onClickedPickPhoto()
    {
        picker.pickImage(0, false) { imageUri, bitmap, tag ->
            resultUri = imageUri

            Log.d("placeImageUpload", resultUri.toString())
            Toast.makeText(this@PlaceActivity,"Uploading Place Image.....", Toast.LENGTH_LONG).show()
            placeViewModel.uploadPlaceImage(this@PlaceActivity, resultUri!!, bitmap, placeDetails.placeKey)

        }
    }
    //endregion

    override fun editDescription() {
        val dialog = PlaceDescriptionDialog(this)
        dialog.show(supportFragmentManager, "report")
    }



    override fun onPlaceCreate(placeDetails: PlaceDetails) {

    }
    var needVideoRetry = false
    override fun onPostCreate(feedPost: FeedPost) {

        Log.d("postTypeDetect", feedPost.type)
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
            when (posts[1].post?.type) {
                "VIDEO" -> {
                    needVideoRetry = true
                    downloadVideo(1, feedPost)
                }
                "AUDIO" -> {
                    downloadAudio(1, feedPost)
                }
            }

        } else if (feedPost.type == "CHECK_IN") {
            Constants.IS_CHECKIN = true
            mAdapter.notifyDataSetChanged()
            Log.e("eee post", "checkin =>${feedPost.place!!.following}")
            val intent = Intent(this, MapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
        placeViewModel.followPlace(placeDetails.placeKey!!)
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
        groupViewModel.likePost(feedPost.postKey)
    }

    override fun onUnlikeClicked(position: Int, feedPost: FeedPost) {
        groupViewModel.unlikePost(feedPost.postKey)
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
            groupViewModel.attendEvent(feedPost.postKey)
        }
    }

    override fun onEventClick(position: Int, feedPost: FeedPost) {
        val eventDetails = EventDetailsDialogFragment(feedPost)
        eventDetails.show(supportFragmentManager, "eventDetails")
    }

    override fun onEventNotAttend(position: Int, feedPost: FeedPost) {
        Log.d("unattendCalled", "called")
        pendingAttend = position
        groupViewModel.unattendEvent(feedPost.postKey)
    }

    override fun onShare(postKey: String) {
        groupViewModel.sharePost(postKey, "")

    }


    var pendingDelete = -1
    var rootPosDelete = -1
    var pendingDeleteKey = ""
    override fun onPostDelete(position: Int, postKey: String, rootPos: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.confirm_delete_post))
            .setMessage(getString(R.string.delete_sure_post))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                pendingDelete = position
                rootPosDelete = rootPos
                pendingDeleteKey = postKey
                groupViewModel.deletePost(postKey)
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

    override fun onStatusClicked() {

    }

    override fun onTitleClick(group: GroupDetails) {
        val intent = Intent(this, GroupDetailsActivity::class.java)
        intent.putExtra("data", group)
        intent.putExtra("location", location)
        startActivity(intent)
    }

    override fun descriptionEdited(description: String) {
        val placeUpdateBody = PlaceUpdateBody()
        placeUpdateBody.description = description
        placeViewModel.updatePlace(placeUpdateBody, placeDetails.placeKey)
    }

    override fun onRateClicked()
    {
        if (PrefManager(this@PlaceActivity).getIdToken().isNullOrEmpty()) {
            val intent = Intent(this@PlaceActivity, AuthActivity::class.java)
            intent.putExtra("goToLogin", true)
            startActivity(intent)
        }
        else
        {
            try
            {
                val ratePlaceDialogFragment = RatePlaceDialogFragment.newInstance(placeKey = placeDetails.placeKey)
                ratePlaceDialogFragment.show(supportFragmentManager, RatePlaceDialogFragment::class.java.simpleName)
                ratePlaceDialogFragment.listener = {
                    placeDetails.averageRate = it.averageRate
                    placeDetails.totalRate = it.totalRate
                    placeDetails.countRates = it.countRates

                    binding.data = placeDetails
                    posts[0].place = placeDetails
                    mAdapter.notifyItemChanged(0)
                }
            }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onWebSiteClicked()
    {
        if(placeDetails.website.isEmpty())
        {
            if (PrefManager(this@PlaceActivity).getIdToken().isNullOrEmpty())
            {
                val intent = Intent(this@PlaceActivity, AuthActivity::class.java)
                intent.putExtra("goToLogin", true)
                startActivity(intent)

                return
            }
        }

        try
        {
            val editWebsiteDialogFragment = EditWebsiteDialogFragment.newInstance(
                placeDetails.placeKey,
                placeDetails.website,
                placeDetails.canEdit
            )
            editWebsiteDialogFragment.show(supportFragmentManager, EditWebsiteDialogFragment::class.java.simpleName)
            editWebsiteDialogFragment.listener = { onPlaceEdited(it) }
        }
        catch (e: Exception) { e.printStackTrace() }
    }

    override fun onPhoneClicked()
    {
        if(placeDetails.phone.isEmpty())
        {
            if (PrefManager(this@PlaceActivity).getIdToken().isNullOrEmpty())
            {
                val intent = Intent(this@PlaceActivity, AuthActivity::class.java)
                intent.putExtra("goToLogin", true)
                startActivity(intent)

                return
            }
        }

        try
        {
            val editPhoneDialogFragment = EditPhoneDialogFragment.newInstance(
                placeDetails.placeKey,
                placeDetails.phone,
                placeDetails.canEdit
            )
            editPhoneDialogFragment.show(supportFragmentManager, EditPhoneDialogFragment::class.java.simpleName)
            editPhoneDialogFragment.listener = { onPlaceEdited(it) }
        }
        catch (e: Exception) { e.printStackTrace() }
    }

    override fun onAddressClicked()
    {
        if(placeDetails.address.isEmpty())
        {
            if (PrefManager(this@PlaceActivity).getIdToken().isNullOrEmpty())
            {
                val intent = Intent(this@PlaceActivity, AuthActivity::class.java)
                intent.putExtra("goToLogin", true)
                startActivity(intent)

                return
            }
        }

        try
        {
            val editAddressDialogFragment = EditAddressDialogFragment.newInstance(
                placeDetails.placeKey,
                placeDetails.address,
                placeDetails.canEdit
            )
            editAddressDialogFragment.show(supportFragmentManager, EditAddressDialogFragment::class.java.simpleName)
            editAddressDialogFragment.listener = { onPlaceEdited(it) }
        }
        catch (e: Exception) { e.printStackTrace() }
    }

    override fun onOpeningTimeClicked()
    {
        Log.d("AbdullahTag", placeDetails.workingHours.isEmpty().toString())

        if(placeDetails.workingHours.isEmpty())
        {
            if (PrefManager(this@PlaceActivity).getIdToken().isNullOrEmpty())
            {
                val intent = Intent(this@PlaceActivity, AuthActivity::class.java)
                intent.putExtra("goToLogin", true)
                startActivity(intent)

                return
            }
        }

        try
        {
            val openingHoursDialogFragment = OpeningHoursDialogFragment.newInstance(
                placeDetails.placeKey,
                placeDetails.canEdit
            )
            BigDataSharConstants.openingHourArray = placeDetails.workingHours
            openingHoursDialogFragment.show(supportFragmentManager, OpeningHoursDialogFragment::class.java.simpleName)
            openingHoursDialogFragment.listener = { onPlaceEdited(it) }
        }
        catch (e: Exception) { e.printStackTrace() }
    }

    private fun onPlaceEdited(placeDetails: PlaceDetails)
    {
        this.placeDetails = placeDetails

        binding.data = placeDetails
        posts[0].place = placeDetails
        mAdapter.notifyItemChanged(0)
    }
}