package com.orbis.orbis.ui.ProfileModule.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.BuildConfig
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.databinding.FragmentMyProfileBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.group.FeedContent
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails
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
import com.orbis.orbis.ui.homeModule.views.MapActivity
import com.orbis.orbis.ui.homeModule.views.ProfileSearchActivity
import com.orbis.orbis.ui.newsFeedModule.adapter.StatusAdapter
import com.orbis.orbis.ui.placesModule.views.CreatePostDialogFragment
import com.orbis.orbis.ui.settingsModule.views.SettingsActivity
import com.orbis.orbis.ui.storiesModule.views.StoryActivity

import com.orbis.orbis.utils.*
import com.orbis.orbis.utils.picker.Picker
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MyProfileFragment() : BaseFragment(), OnPlaceCreate, GroupPostsAdapter.PostCardInteraction,
    StatusAdapter.StatusCardInteraction, PhotosAdapter.UserInteraction {
    val userPictures: ArrayList<UserPictures> = ArrayList()
    lateinit var photosAdapter: PhotosAdapter
    private var currentPage = 0
    private var currentPagePicture = 0
    private var resultUri: Uri? = null
    var current_position = 0
    private var isSearchVisible = false
    lateinit var binding: FragmentMyProfileBinding
    lateinit var profileViewModel: ProfileViewModel
    var pendingLogin = false
    var pendingRefresh = false

    private val picker = Picker()

    val posts: ArrayList<FeedContent> = ArrayList()
    var nextPagePost : String? = ""
    var currentPagePost: String? = ""
    lateinit var feedAdapter: ProfilePostsAdapter
    lateinit var groupViewModel: GroupViewModel
    var exoPlayer: SimpleExoPlayer? = null
    var videoPlayerPosition = 0
    var pendingAttend = -1
    var pendingDelete = -1
    var rootPosDelete = -1
    var isPostCreated = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_my_profile, container, false)
        initView()
        return binding.root
    }

    private fun showCreatePostSheet() {
        val createPostDialogFragment = CreatePostDialogFragment.newInstance(location = Constants.location)
        createPostDialogFragment.listener = this
        createPostDialogFragment.show(
            requireActivity().supportFragmentManager,
            CreatePostDialogFragment::class.java.getSimpleName()
        )
    }


    private fun initView()
    {
        picker.populate(fragment = this)

        userPictures.clear()
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]

        profileViewModel.getMyProfile()
        profileViewModel.getMyPictures(0)
        Handler(Looper.getMainLooper()).postDelayed({
            pendingLogin = false
            profileViewModel.getMyFeedFirst()
            profileViewModel.igConnect()

        }, 2000)
        Handler(Looper.getMainLooper()).postDelayed({
            pendingLogin = false
            profileViewModel.getMyFeedFirst()
        }, 2000)
        photosAdapter = PhotosAdapter(requireContext(), userPictures, this, true, requireActivity())
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
                            profileViewModel.getMyPictures(pictureNextPage)
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
                            if (nextPagePost?.isEmpty() == true && currentPagePost != nextPagePost) {
                                profileViewModel.getMyFeed(nextPagePost!!)
                                currentPagePost = nextPagePost
                            }
                        }
                    }
                }
            }
        })
        binding.profileRefresh.setOnRefreshListener {
            profileViewModel.getMyProfile()
            if (current_position == 0) {
                currentPagePicture = 0
                pictureNextPage = 0
                val size = userPictures.size
                userPictures.clear()
                pendingLogin = false
                if (isConnected) {
                    profileViewModel.igConnect()
                }
            } else {
                if (exoPlayer != null) {
                    exoPlayer?.pause()
                }
                val size = posts.size
                posts.clear()
                feedAdapter.notifyDataSetChanged()
                profileViewModel.getMyFeedFirst()
            }
        }
        binding.groupCountTv.setOnClickListener {
            val intent = Intent(requireActivity(), GroupListActivity::class.java)
            intent.putExtra("userKey", binding.data?.userKey)
            intent.putExtra("location", Constants.location)
            intent.putExtra("city", "my_groups") //use mock city to save data in cache as my feed
            startActivity(intent)
        }
        binding.groupsTitle.setOnClickListener {
            val intent = Intent(requireActivity(), GroupListActivity::class.java)
            intent.putExtra("userKey", binding.data?.userKey)
            intent.putExtra("location", Constants.location)
            intent.putExtra("city", "my_groups")
            startActivity(intent)
        }
        binding.addPhotoCardview.setOnClickListener {
            onClickedUploadUserPicture()
        }
        binding.addFab.setOnClickListener {
            showCreatePostSheet()
        }

        binding.followersTv.setOnClickListener {
            val intent = Intent(requireActivity(), FollowingFollowerActivity::class.java)
            intent.putExtra("userKey", "")
            intent.putExtra("isFollower", true)

            startActivity(intent)
        }
        binding.followersTitle.setOnClickListener {
            val intent = Intent(requireActivity(), FollowingFollowerActivity::class.java)
            intent.putExtra("userKey", "")
            intent.putExtra("isFollower", true)
            startActivity(intent)
        }
        binding.followingTv.setOnClickListener {
            val intent = Intent(requireActivity(), FollowingFollowerActivity::class.java)
            intent.putExtra("userKey", "")
            intent.putExtra("isFollower", false)
            startActivity(intent)
        }
        binding.followingTitle.setOnClickListener {
            val intent = Intent(requireActivity(), FollowingFollowerActivity::class.java)
            intent.putExtra("userKey", "")
            intent.putExtra("isFollower", false)
            startActivity(intent)
        }
        binding.connectBtn.setOnClickListener {
            if (!isConnected) {
                pendingLogin = true
                profileViewModel.igConnect()
            }
        }

        setupObservers()
    }

    var isUploadPending = false
    var pictureNextPage = 0
    var isConnected = false
    lateinit var userInfo: UserInfo
    private fun setupObservers() {
        profileViewModel.deletePicture.observe(viewLifecycleOwner) {
            if (it && pendingPicDelete >= 0) {
                userPictures.removeAt(pendingPicDelete)
                photosAdapter.notifyItemRemoved(pendingPicDelete)
                if (posts.isEmpty() && current_position == 0) {
                    binding.noItem.visibility = View.VISIBLE
                    binding.noPostTv.text =
                        requireContext().getString(R.string.connect_with_instagram_or_add_photos_straight_from_orbis)
                } else if (current_position == 0) {
                    binding.noItem.visibility = View.GONE
                }
            }
        }
        profileViewModel.myProfile.observe(viewLifecycleOwner) {
            userInfo = it
            binding.settingsIv.setOnClickListener {
                val intent = Intent(requireContext(), SettingsActivity::class.java)
                intent.putExtra("data", binding.data)
                startActivity(intent)
            }
            val prefManager = PrefManager(requireContext())
            prefManager.saveUserName(it.displayName)
            prefManager.saveUserKey(it.userKey!!)
            binding.data = it
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
                    if (posts.size > rootPosDelete) {
                        binding.profileRecycler.scrollToPosition(rootPosDelete)
                    }

                }
                if (posts.isEmpty() && current_position == 1) {
                    binding.noItem.visibility = View.VISIBLE
                    binding.noPostTv.text = getString(R.string.no_posts)
                } else if (current_position == 1) {
                    binding.noItem.visibility = View.GONE
                }

            }
        }
        profileViewModel.instagramLogin.observe(viewLifecycleOwner) {
            if (it.status == "NOT_CONNECTED") {
                isConnected = false
                if (pendingLogin) {
                    val uri =
                        Uri.parse(it.authLink) // missing 'http://' will cause crashed

                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
                if (failureCheck) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.instagram_connect_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (it.status == "CONNECTED") {
                isConnected = true
                binding.noItem.visibility = View.GONE
                binding.connectBtn.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.border_black_btn)
                binding.connectBtn.text = getString(R.string.connected_instagram)
                binding.connected.visibility = View.VISIBLE
                binding.connectBtn.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.black
                    )
                )
            }
            failureCheck = false
            userPictures.clear()
            pictureNextPage = 0
            currentPagePicture = 0
            profileViewModel.getMyPictures(0)

        }
        profileViewModel.userPictures.observe(viewLifecycleOwner) {
            Log.d("userPictureComes", it.size.toString())
            pictureNextPage++
            val ind = userPictures.size
            for (pic in it) {
                if (!contain(pic)) {
                    userPictures.add(pic)
                }
            }
            photosAdapter.notifyDataSetChanged()
            if (userPictures.isEmpty() && current_position == 0) {
                binding.noItem.visibility = View.VISIBLE
                binding.noPostTv.text =
                    requireContext().getString(R.string.connect_with_instagram_or_add_photos_straight_from_orbis)
            } else if (current_position == 0) {
                binding.noItem.visibility = View.GONE
            }
            if (binding.profileRefresh.isRefreshing && current_position == 0) {
                binding.profileRefresh.isRefreshing = false
            }
            if (isConnected) {
                profileViewModel.getMyIgPictures(pictureNextPage - 1)
            }
        }
        profileViewModel.newPicture.observe(viewLifecycleOwner) {
            userPictures.add(0, it)
            photosAdapter.notifyItemInserted(0)
            binding.profileRecycler.scrollToPosition(0)
            isUploadPending = false
            binding.noItem.visibility = View.GONE
            isUploadPending = false
        }
        profileViewModel.igPictures.observe(viewLifecycleOwner) {
            Log.d("igPicturesCome", it.size.toString())
            val ind = userPictures.size
            for (pic in it) {
                if (!contain(pic)) {
                    userPictures.add(pic)
                }
            }
            Log.d("igPicturesComeIndex", ind.toString())
            if (userPictures.isEmpty() && current_position == 0) {
                binding.noItem.visibility = View.VISIBLE
                binding.noPostTv.text =
                    requireContext().getString(R.string.connect_with_instagram_or_add_photos_straight_from_orbis)
            } else if (current_position == 0) {
                binding.noItem.visibility = View.GONE
            }
            photosAdapter.notifyDataSetChanged()
            Log.d("igPicturesComeFinal", userPictures.size.toString())
        }
        profileViewModel.feed.observe(viewLifecycleOwner) {
            nextPagePost = it.nextPage
            val index = posts.size
//            if (binding.swipeRefresh.isRefreshing) {
//                binding.swipeRefresh.isRefreshing = false
//            }
            if (!it.content.isNullOrEmpty()) {
                posts.addAll(it.content)
                if (posts.isEmpty()) {
                    binding.noItem.visibility = View.VISIBLE
                    binding.noPostTv.text = "No Posts"
                } else {
                    binding.noItem.visibility = View.GONE
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
                feedAdapter.notifyItemRangeInserted(index, posts.size)
            } else {
                if (posts.isEmpty()) {
                    binding.noItem.visibility = View.VISIBLE
                    binding.noPostTv.text = getString(R.string.no_posts)
                } else {
                    binding.noItem.visibility = View.GONE
                }
            }
            if (binding.profileRefresh.isRefreshing && current_position == 1) {
                binding.profileRefresh.isRefreshing = false
            }
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



    override fun onPause() {
        super.onPause()
    }
    var failureCheck = false
    override fun onResume() {
        super.onResume()
        Log.d("profileResumed", "resumed")
        if (!isUploadPending) {
            if (pendingRefresh) {
                pendingRefresh = false
                profileViewModel.getMyIgPictures(0)
            }
            if (!pendingRefresh) {
                pendingRefresh = false
                profileViewModel.getMyProfile()
            }
            if (pendingLogin) {
                pendingLogin = false
                binding.loading = true
                Handler(Looper.getMainLooper()).postDelayed({
                    failureCheck = true
                    profileViewModel.igConnect()
                }, 2000)

            }
        }
    }

    fun contain(item: UserPictures): Boolean {
        for (pic in userPictures) {
            if (pic.pictureKey == item.pictureKey) {
                return true
            }
        }
        return false
    }

    fun goToMap() {
        val intent = Intent(requireContext(), MapActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.backFab.setOnClickListener {
            requireActivity().finish()
        }
        binding.userPicIv.setOnClickListener { v ->
            // Display popup attached to the button as a position anchor
            displayPopupWindow(v!!)
        }
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {

                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                val groupIntent = Intent(requireContext(), ProfileSearchActivity::class.java)
                startActivity(groupIntent)
                return false
            }

        })


//        message_fab.setOnClickListener { FragmentUtils.launchFragmentWithDefaultAnimation(
//            requireActivity(),
//            R.id.container,
//            ProfileFragment(),
//            FragmentUtils.FragmentLaunchMode.REPLACE,
//            true
//        ) }


        hideKeyboard(requireActivity())
    }
    private fun displayPopupWindow(anchorView: View) {
        val popup = PopupWindow(requireContext())
        val layout: View = layoutInflater.inflate(R.layout.popup_take_photo, null)
        popup.contentView = layout
        // Set content width and height
        popup.height = WindowManager.LayoutParams.WRAP_CONTENT
        popup.width = WindowManager.LayoutParams.WRAP_CONTENT
        // Closes the popup window when touch outside of it - when looses focus
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT)
        )
        var take_photo_cl=layout.findViewById<ConstraintLayout>(R.id.take_photo_cl)
        take_photo_cl.setOnClickListener {
            popup.dismiss()
            onClickedChangePhoto()
        }
        popup.showAsDropDown(anchorView)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //  super.onActivityResult(requestCode, resultCode, data)
        if (requestCode === CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode === Activity.RESULT_OK) {
                resultUri = result?.uri
                Glide.with(requireContext())
                    .asBitmap()
                    .load(resultUri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            binding.userPicIv.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // this is called when imageView is cleared on lifecycle call or for
                            // some other reason.
                            // if you are referencing the bitmap somewhere else too other than this imageView
                            // clear it here as you can no longer have the bitmap
                        }
                    })
            } else if (resultCode === CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result?.error
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == ChoosePhoto.SELECT_PICTURE_CAMERA) {
//            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) showAlertDialog()
//        }
    }

    private fun setUpViewPager() {
        setupTabs()
        binding.searchFab.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileSearchActivity::class.java))
        }
    }

    fun setupTabs() {
        binding.tabLayout.removeAllTabs()
        addNewTab(true, resources.getString(R.string.photos))
        addNewTab(false, resources.getString(R.string.posts))
        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                current_position = tab.position
                when (current_position) {
                    0 -> {
                        binding.searchCl.visibility = View.GONE
                        binding.addFab.visibility = View.GONE

                        binding.addPhotoCardview.visibility = View.VISIBLE
                        binding.backFab.visibility = View.VISIBLE

                        binding.profileRecycler.layoutManager =
                            GridLayoutManager(requireContext(), 3)
                        binding.profileRecycler.adapter = photosAdapter
                        if (userPictures.isEmpty()) {
                            Log.d("userPictureOtherProfile", "Not found")
                            binding.noItem.visibility = View.VISIBLE
                            binding.noPostTv.text =
                                requireContext().getString(R.string.connect_with_instagram_or_add_photos_straight_from_orbis)
                        } else {
                            binding.noItem.visibility = View.GONE
                        }
                    }
                    1 -> {


                        binding.addPhotoCardview.visibility = View.GONE
                        binding.addFab.visibility = View.VISIBLE
                        binding.searchFab.visibility = View.VISIBLE
                        binding.profileRecycler.layoutManager =
                            LinearLayoutManager(requireContext())
                        binding.profileRecycler.adapter = feedAdapter
                        if (posts.isEmpty()) {
                            binding.noItem.visibility = View.VISIBLE
                            binding.noPostTv.text = "No Posts"
                        } else {
                            binding.noItem.visibility = View.GONE
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        binding.searchFab.setOnClickListener {
            binding.searchCl.visibility = View.VISIBLE
            binding.backFab.visibility = View.GONE
            binding.addFab.visibility = View.GONE
            binding.searchFab.visibility = View.GONE
            isSearchVisible = true
        }

        binding.closeIv.setOnClickListener {
            binding.searchCl.visibility = View.GONE
            binding.backFab.visibility = View.VISIBLE
            binding.addFab.visibility = View.VISIBLE
            binding.searchFab.visibility = View.VISIBLE
            isSearchVisible = false
        }
        if (isPostCreated) {
            isPostCreated = false
            binding.tabLayout.getTabAt(1)?.select()
        }
    }

    fun addNewTab(isDefaultSelected: Boolean?, title: String) {
        binding.tabLayout.addTab(
            binding.tabLayout.newTab().setText(title).setIcon(null),
            isDefaultSelected!!
        )
    }

    companion object {
        fun getInstance(): MyProfileFragment {
            return MyProfileFragment()
        }

    }



    override fun onPlaceCreate(placeDetails: PlaceDetails) {
        //setUpViewPager(isConnected)
    }

    override fun onPostCreate(feedPost: FeedPost) {
        Log.d("postCreationOnProfile", "done")
        isPostCreated = true
        if (feedPost.type != "CHECK_IN") {
            val feedContent = FeedContent("POST", feedPost)
            posts.add(0, feedContent)
            feedAdapter.notifyItemInserted(0)
            binding.profileRecycler.scrollToPosition(0)
            if (feedPost.type == "VIDEO") {
                Handler(Looper.getMainLooper()).postDelayed({
                    downloadVideo(0, feedPost)
                }, 1000)

            } else if (feedPost.type == "AUDIO") {
                downloadAudio(0, feedPost)
            }
        } else if (feedPost.type == "CHECK_IN") {
            Constants.IS_CHECKIN = true
            val intent = Intent(requireContext(), MapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            requireActivity().finish()
        }
        binding.noItem.visibility = View.GONE

        // setUpViewPager(isConnected)
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

    var pendingPicDelete = -1
    override fun removePhoto(position: Int) {
        pendingPicDelete = position
        profileViewModel.deleteMyPicture(userPictures[position].pictureKey)
    }

    //region onClicked
    private fun onClickedChangePhoto()
    {
        val myTag = 2
        picker.pickImage(myTag) { imageUri, bitmap, tag ->
            if(myTag == tag)
            {
                binding.data?.let { userInfo ->
                    profileViewModel.uploadProfilePicture(requireContext(), imageUri, bitmap, userInfo)
                    binding.userPicIv.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun onClickedUploadUserPicture()
    {
        val myTag = 3
        picker.pickImage(tag = myTag, crop = false) { imageUri, bitmap, tag ->
            if(myTag == tag)
            {
                binding.data?.let { userInfo ->
                    isUploadPending = true
                    profileViewModel.uploadUserPicture(requireContext(), imageUri, bitmap, userInfo)
                }
            }
        }
    }
    //endregion
}