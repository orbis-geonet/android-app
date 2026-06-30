package com.orbis.orbis.ui.storiesModule.views

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentStoryBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.story.StoryModel
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.commentModule.views.CommentDialogFragment
import com.orbis.orbis.ui.commentModule.views.CommentDialogue
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.groupsModule.views.GroupDetailsActivity
import com.orbis.orbis.ui.newsFeedModule.viewModel.FeedViewModel
import com.orbis.orbis.ui.placesModule.views.PlaceActivity
import com.orbis.orbis.ui.storiesModule.utils.OnSwipeTouchListener
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import dagger.hilt.android.AndroidEntryPoint
import pt.tornelas.segmentedprogressbar.SegmentedProgressBarListener
import java.util.*

@AndroidEntryPoint
class StoryFragment(
    val storyModel: StoryModel,
    val listener: StoryInteraction,
    val groupPosition: Int
) : Fragment(), DialogueListener {
    lateinit var binding: FragmentStoryBinding
    var currentStory = 0
    var forcePause = false
    var postDownloaded = false
    lateinit var viewModel: FeedViewModel
    lateinit var groupViewModel: GroupViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_story, container, false)
        groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]
        initView()
        if (Constants.appConfig?.isAdsEnabled!!) {
            loadAd()
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        Log.d("oldNew", "onResumed " + showingAd)
        if (isVisible && !showingAd) {
            binding.loading = true
            for (i in 0 until storyModel.posts.size) {
                if (!storyModel.posts[i].seen) {
                    currentStory = i
                    break
                }
            }
            setupNewStory(currentStory)
            binding.spb.pause()
        }
        if (showingAd) {
            showingAd = false
        }
    }

    private var mInterstitialAd: InterstitialAd? = null
    private fun loadAd() {
        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            requireContext(),
            Constants.INTERSTITIAL_AD_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError?.toString()?.let { Log.d("onAdFailedToLoad", it) }
                    mInterstitialAd = null
                    loadAd()
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("onAdLoaded", "Ad was loaded.")
                    Constants.storyAdInterval = 0
                    mInterstitialAd = interstitialAd
                    mInterstitialAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdClicked() {
                                // Called when a click is recorded for an ad.
                                Log.d("onAdClicked", "Ad was clicked.")
                            }

                            override fun onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                Log.d(
                                    "onAdDismissedFullScreenContent",
                                    "Ad dismissed fullscreen content."
                                )
                                mInterstitialAd = null
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (storyModel.posts[currentStory].type == "VIDEO") {
                                        if (player != null) {
                                            player?.play()
                                        }
                                    }
                                    binding.spb.start()

                                }, 100)


                            }

                            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                // Called when ad fails to show.
                                Log.e(
                                    "onAdFailedToShowFullScreenContent",
                                    "Ad failed to show fullscreen content."
                                )
                                mInterstitialAd = null
                            }

                            override fun onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                Log.d("onAdImpression", "Ad recorded an impression.")
                            }

                            override fun onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                object : CountDownTimer(3000, 3000) {
                                    override fun onTick(millisUntilFinished: Long) {

                                    }

                                    @SuppressLint("RestrictedApi")
                                    override fun onFinish() {
                                        requireActivity().runOnUiThread {
                                            Log.d("closingAd", "closing")

                                        }

                                    }

                                }.start()
                                Log.d(
                                    "onAdShowedFullScreenContent",
                                    "Ad showed fullscreen content."
                                )
                            }
                        }

                    if(shouldOfShowAd)
                    {
                        shouldOfShowAd = false
                        showAd()
                    }
                }
            })
    }

    var showingAd = false
    var shouldOfShowAd = false
    private fun showAd() {
        if (mInterstitialAd != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (storyModel.posts[currentStory].type == "VIDEO") {
                    if (player != null) {
                        player?.pause()
                    }
                }
                binding.spb.pause()
            }, 100)
            mInterstitialAd?.show(requireActivity())
            showingAd = true
            loadAd()
        } else {
            shouldOfShowAd = true
            loadAd()
            Log.d("TAG", "The interstitial ad wasn't ready yet.")
        }
    }

    private fun initView() {
        Log.d("inititalizingNewGroup", "init for" + groupPosition)
        currentStory = 0
        setupSegments()

        val nextTouch = object : OnSwipeTouchListener(requireActivity()) {
            override fun onSwipeTop() {

            }

            override fun onSwipeBottom() {
                if (player != null) {
                    player?.release()
                }
                requireActivity().finish()
            }

            override fun onClick(view: View) {
                if (currentStory >= storyModel.posts.size - 1) {
                    if (player != null) {
                        player?.release()
                    }
                    listener.nextGroup()
                } else {
                    if (player != null) {
                        player?.release()
                    }
                    binding.spb.next()
                }
            }

            override fun onLongClick() {
                // hideStoryOverlay()
            }

            override fun onTouchView(view: View, event: MotionEvent): Boolean {
                super.onTouchView(view, event)
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // pressTime = System.currentTimeMillis()
                        commentShowing = true
                        if (player != null) {
                            player?.pause()
                        }
                        binding.spb.pause()
                        binding.storyOverlay.visibility = View.GONE
                        return false
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isVisible) {
                            if (storyModel.posts[currentStory].type == "VIDEO") {
                                if (player != null) {
                                    player?.play()
                                }
                            } else {
                                if (player != null) {
                                    player?.pause()
                                    player?.release()
                                }
                            }
                            binding.spb.start()
                            commentShowing = false
                            binding.storyOverlay.visibility = View.VISIBLE
                        }
                        //showStoryOverlay()
                        //resumeCurrentStory()
                        // return /limit < System.currentTimeMillis() - pressTime
                    }
                }
                return false
            }
        }
        val prevTouch = object : OnSwipeTouchListener(requireActivity()) {
            override fun onSwipeTop() {
            }

            override fun onSwipeBottom() {
                if (player != null) {
                    player?.release()
                }
                requireActivity().finish()
            }

            override fun onClick(view: View) {
                if (player != null) {
                    player?.release()
                }
                if (currentStory == 0) {
                    listener.prevGroup()
                } else {
                    binding.spb.previous()
                }
            }

            override fun onLongClick() {
                // hideStoryOverlay()
            }

            override fun onTouchView(view: View, event: MotionEvent): Boolean {
                super.onTouchView(view, event)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // pressTime = System.currentTimeMillis()
                        commentShowing = true
                        if (player != null) {
                            player?.pause()
                        }
                        binding.spb.pause()
                        binding.storyOverlay.visibility = View.GONE
                        return false
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isVisible) {
                            if (storyModel.posts[currentStory].type == "VIDEO") {
                                if (player != null) {
                                    player?.play()
                                }
                            } else {
                                if (player != null) {
                                    player?.pause()
                                    player?.release()
                                }
                            }
                            binding.spb.start()
                            commentShowing = false
                            binding.storyOverlay.visibility = View.VISIBLE
                        }
                        //showStoryOverlay()
                        //resumeCurrentStory()
                        // return /limit < System.currentTimeMillis() - pressTime
                    }

                }
                return false
            }
        }
        binding.next.setOnTouchListener(nextTouch)
        binding.previous.setOnTouchListener(prevTouch)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener.seenStory(storyModel.posts[currentStory].postKey)
    }

    private fun setupSegments() {
        binding.spb.reset()
        binding.spb.segmentCount = storyModel.posts.size
        if (binding.spb.listener != null) {
            binding.spb.listener?.onFinished()
        }

        binding.spb.listener = object : SegmentedProgressBarListener {
            override fun onPage(oldPageIndex: Int, newPageIndex: Int) {
                listener.seenStory(storyModel.posts[currentStory].postKey)
                Log.d(
                    "oldNew",
                    oldPageIndex.toString() + " " + newPageIndex.toString() + " " + commentShowing.toString()
                )
                if (oldPageIndex == newPageIndex && !commentShowing) {
                    if (newPageIndex == storyModel.posts.size - 1) {
                        if (player != null) {
                            player?.pause()
                            player?.release()
                        }
                        listener.nextGroup()
                    } else {
                        binding.spb.next()
                    }
                } else if (oldPageIndex >= 0) {
                    binding.spb.start()
                    binding.spb.pause()
                    currentStory = newPageIndex
                    setupNewStory(newPageIndex)
                    viewModel.seenStory(storyModel.posts[currentStory].postKey)
                }


            }

            override fun onFinished() {
                Log.d(
                    "oldNew", "finished $currentStory ${storyModel.posts.size}"
                )
                if (currentStory >= storyModel.posts.size - 1) {
                    if (player != null) {
                        player?.pause()
                        player?.release()
                    }
                    listener.nextGroup()
                }
//                if (currentStory >= stories[currentPage].posts.size - 1) {
//                    fakeDrag(true)
//                    currentPage++
//                    setupNewStory(currentPage)
//                }
                // All segments animated and finished animation
            }

        }
    }
    private fun setupNewStory(position: Int) {
        Constants.storyAdInterval++
        if (Constants.storyAdInterval >= Constants.appConfig?.storyAdsFrequency!!) {
            showAd()
        }
        Log.d("oldNew", storyModel.posts[position].type + " " + position)
        if (storyModel.posts[position].type == "IMAGE") {
            if (isVisible) {
                if (player != null) {
                    player?.pause()
                    player?.release()
                }
                binding.playerView.visibility = View.GONE
                binding.postImage.visibility = View.VISIBLE
                downloadStoryImage(position)
            }
        } else if (storyModel.posts[position].type == "VIDEO") {
            if (isVisible) {
                binding.playerView.visibility = View.VISIBLE
                binding.postImage.visibility = View.GONE
                downloadStoryVideo(position)
            }
        } else {
            if (isVisible) {
                binding.spb.start()
                binding.loading = false
            }
        }
        val calendar = Calendar.getInstance()
        calendar.time =
            ViewUtils.convertTimeStampToDate(storyModel.posts[position].timestamp)!!
        binding.storyDisplayTime.text = DateFormat.format("MM/dd/yyyy", calendar)
            .toString() + " at " + DateFormat.format("hh:mm aa", calendar).toString()
        if (storyModel.posts[position].place != null) {
            Log.d(
                "placeDataCheckOnStory",
                storyModel.posts[position].place?.placeKey + " " + storyModel.posts[position].place?.name
            )
            binding.location.visibility = View.VISIBLE
            binding.location.text = storyModel.posts[position].place?.name
            binding.location.setOnClickListener {
                val intent = Intent(requireContext(), PlaceActivity::class.java)
                intent.putExtra("data", storyModel.posts[position].place)
                startActivity(intent)
            }
        } else {
            binding.location.visibility = View.GONE
        }
        binding.storyUsername.text = storyModel.posts[position].user?.displayName
        binding.groupName.text = storyModel.group.name
        if (storyModel.posts[position].details.isNotEmpty()) {
            binding.postTv.setContent(storyModel.posts[position].details)
            binding.postTv.setTextMaxLength(200)
            binding.postTv.setSeeMoreTextColor(R.color.view_more_blue)
        } else {
            binding.postTv.visibility = View.GONE
        }
        binding.likesTv.text = storyModel.posts[position].likesCount.toString()
        binding.commentsTv.text = storyModel.posts[position].commentsCount.toString()
        if (isVisible) {
            downloadUserImage(position)
            downloadGroupPhoto()
        }
        binding.groupPic.setOnClickListener {
            val intent = Intent(requireContext(), GroupDetailsActivity::class.java)
            intent.putExtra("data", storyModel.group)
            intent.putExtra("location", Constants.location)
            startActivity(intent)
        }
        binding.groupName.setOnClickListener {
            val intent = Intent(requireContext(), GroupDetailsActivity::class.java)
            intent.putExtra("data", storyModel.group)
            intent.putExtra("location", Constants.location)
            startActivity(intent)
        }
        binding.storyUserProfilePicture.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            intent.putExtra(
                "displayName",
                storyModel.posts[position].user?.displayName
            )
            intent.putExtra("userKey", storyModel.posts[position].user?.userKey)
            startActivity(intent)
        }
        binding.storyUsername.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            intent.putExtra(
                "displayName",
                storyModel.posts[position].user?.displayName
            )
            intent.putExtra("userKey", storyModel.posts[position].user?.userKey)
            startActivity(intent)
        }
        if (storyModel.posts[position].userLiked){
            binding.likesIv.setOnClickListener {
                if (PrefManager(requireContext()).getIdToken().isNullOrEmpty()) { redirectToLogin(); return@setOnClickListener }
                groupViewModel.likePost(storyModel.posts[position].postKey)
                storyModel.posts[position].userLiked = false
                storyModel.posts[position].likesCount--
                binding.likesTv.text = storyModel.posts[position].likesCount.toString()
            }
        }else{
            binding.likesIv.setOnClickListener {
                if (PrefManager(requireContext()).getIdToken().isNullOrEmpty()) { redirectToLogin(); return@setOnClickListener }
                groupViewModel.likePost(storyModel.posts[position].postKey)
                storyModel.posts[position].userLiked = true
                storyModel.posts[position].likesCount++
                binding.likesTv.text = storyModel.posts[position].likesCount.toString()
            }
        }

        binding.commentsTv.setOnClickListener {
            showCommentSheet(storyModel.posts[position].postKey!!)
        }
        binding.commentsIv.setOnClickListener {
            showCommentSheet(storyModel.posts[position].postKey!!)
        }
    }

    var commentShowing = false
    private fun showCommentSheet(postKey: String) {

        val commentDialogFragment = CommentDialogFragment(postKey, object : CommentDialogue {
            override fun onDismissDialogue() {
                if (storyModel.posts[currentStory].type == "VIDEO") {
                    if (player != null) {
                        player?.play()
                    }
                }
                binding.spb.start()
                commentShowing = false
                Log.d("dialogDismissCheck", "dismissed")
            }

            override fun updateCommentCount(postKey: String, count: Int) {

            }

        })
        commentDialogFragment.show(
            childFragmentManager,
            CommentDialogFragment::class.java.getSimpleName()
        )
        commentShowing = true
        Handler(Looper.getMainLooper()).postDelayed({
            if (storyModel.posts[currentStory].type == "VIDEO") {
                if (player != null) {
                    player?.pause()
                }
            }
            binding.spb.pause()
        }, 100)
    }

    private fun redirectToLogin() {
        startActivity(Intent(context, AuthActivity::class.java).putExtra("goToLogin", true))
    }

    private fun downloadGroupPhoto() {
        binding.groupPic.borderWidth = 0
        ViewUtils.loadGroupPhoto(requireContext(), binding.groupPic, storyModel.group.imageName)
    }

    private fun downloadUserImage(position: Int) {
        binding.playerView.visibility = View.GONE
        binding.postImage.visibility = View.VISIBLE
        if (storyModel.posts[position].user != null) {
            ViewUtils.loadUserProfilePic(
                requireContext(),
                binding.storyUserProfilePicture,
                storyModel.posts[position].user?.imageName,
                storyModel.posts[position].user?.providerImageUrl
            )
        }
    }

    private fun downloadStoryVideo(position: Int) {
        binding.spb.start()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.spb.pause()
        }, 100)
        binding.loading = true
        if (storyModel.posts[position].postVideo.isNotEmpty()) {
            Log.d("alreadyVideoStoryFor", position.toString())
            binding.playerView.visibility = View.VISIBLE
            binding.postImage.visibility = View.GONE
            if (player != null) {
                player?.pause()
                player?.release()
            }
            player = SimpleExoPlayer.Builder(requireContext()).build()
            player?.playWhenReady = true
            binding.playerView.player = player

            val mediaItem = MediaItem.fromUri(storyModel.posts[position].postVideo)
            player?.setMediaItem(mediaItem)
            player?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    binding.loading = false
                    if (isVisible) {
                        if (isPlaying && !commentShowing && position == currentStory) {
                            binding.spb.timePerSegmentMs = player?.duration!!
                            forcePause = true
                            Log.d("oldNew", "setting position video $position")
                            binding.spb.setPosition(position)

                            binding.playerView.visibility = View.VISIBLE
                            binding.postImage.visibility = View.GONE
//                            Handler(Looper.getMainLooper()).postDelayed({
//                                if (isVisible)
//                                    forcePause = false
//                                    binding.spb.pause()
//                            }, 3500)
                        } else {
                            if (!commentShowing && position == currentStory)
                                binding.spb.setPosition(position)
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    binding.spb.start()
                    binding.loading = false
                }
            })
            player?.prepare()
            binding.playerView.hideController()
            postDownloaded = true

        } else if (storyModel.posts[position].mediaUrls.isNotEmpty()) {
            Log.d("downloadingVideoStoryFor", position.toString())
            val storage =
                Firebase.storage.getReference(
                    Constants.POST_VIDEO_STORAGE + storyModel.posts[position].mediaUrls[0]
                )

            storage.downloadUrl.addOnSuccessListener {
                storyModel.posts[position].postVideo = it.toString()
                binding.playerView.visibility = View.VISIBLE
                binding.postImage.visibility = View.GONE
                if (player != null) {
                    player?.pause()
                    player?.release()
                }
                player = SimpleExoPlayer.Builder(requireContext()).build()
                player?.playWhenReady = true
                binding.playerView.player = player
                val mediaItem = MediaItem.fromUri(storyModel.posts[position].postVideo)
                player?.setMediaItem(mediaItem)
                player?.addListener(object : Player.Listener {

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        binding.loading = false
                        if (isPlaying && !commentShowing && position == currentStory) {
                            binding.spb.timePerSegmentMs = player?.duration!!
                            forcePause = true
                            Log.d("oldNew", "setting position video $position")

                            binding.spb.setPosition(position)
                            binding.playerView.visibility = View.VISIBLE
                            binding.postImage.visibility = View.GONE
//                            Handler(Looper.getMainLooper()).postDelayed({
//                                if (isVisible)
//                                    forcePause = false
//                                    binding.spb.pause()
//                            }, 3500)
                        } else {
                            if (!commentShowing && position == currentStory)
                                binding.spb.setPosition(position)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        binding.spb.start()
                        binding.loading = false
                    }
                })
                player?.prepare()
                binding.playerView.hideController()
                postDownloaded = true

            }
        } else {
            postDownloaded = true
            Handler(Looper.getMainLooper()).postDelayed({
                binding.spb.start()
            }, 110)

            binding.loading = false
        }

    }

    override fun onPause() {
        super.onPause()
        if (player != null) {
            player?.pause()
        }
        binding.spb.pause()

    }

    var player: SimpleExoPlayer? = null
    override fun onDestroy() {
        binding.spb.listener?.onFinished()
        super.onDestroy()
        if (player != null) {
            player?.release()
        }
    }
    private var forcePosition = false
    private fun downloadStoryImage(position: Int) {
        binding.spb.start()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.spb.pause()
        }, 100)

        binding.postImage.setImageDrawable(null)
        if (player != null) {
            player?.release()
        }
        binding.loading = true
        if (storyModel.posts[position].mediaUrls.isNotEmpty()) {
            val storage =
                Firebase.storage.getReference(
                    Constants.POST_PHOTO_STORAGE + Utils.getImageUrl680(
                        storyModel.posts[position].mediaUrls[0]
                    )
                )
            GlideApp.with(requireContext()).load(storage).listener(object :
                RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    requireActivity().runOnUiThread {
                        if (player != null) {
                            player?.release()
                        }
                        if (position == currentStory) {
                            binding.loading = false
                            forcePause = true
                            binding.spb.timePerSegmentMs = 4000L
                            Log.d("oldNew", "setting position $position")
                            binding.spb.setPosition(position)
                            Handler(Looper.getMainLooper()).postDelayed({
                                binding.spb.start()
                            }, 110)
                            binding.playerView.visibility = View.GONE
                            binding.postImage.visibility = View.VISIBLE
                        }
                    }
                    postDownloaded = true
                    binding.loading = false
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    requireActivity().runOnUiThread {
                        if (position == currentStory) {
                            binding.postImage.setImageDrawable(resource)
                            binding.loading = false
                            Log.d("downloadedStoryFor", position.toString())
                            postDownloaded = true
                            forcePause = true
                            binding.spb.timePerSegmentMs = 4000L
                            Log.d("oldNew", "setting position $position")
                            binding.spb.setPosition(position)
                            Handler(Looper.getMainLooper()).postDelayed({
                                binding.spb.start()
                            }, 110)

                            binding.playerView.visibility = View.GONE
                            binding.postImage.visibility = View.VISIBLE
                        }
                    }

                    return true
                }

            }
            ).submit()
        } else {

            postDownloaded = true
            binding.loading = false
            Log.d("oldNew", "setting position $position")
            binding.spb.timePerSegmentMs = 4000L
            binding.spb.setPosition(position)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.spb.start()
            }, 110)
        }

    }

    override fun onDismissDialogue() {
        if (storyModel.posts[currentStory].type == "VIDEO") {
            if (player != null) {
                player?.play()
            }
        }
        binding.spb.start()
        commentShowing = false
        Log.d("dialogDismissCheck", "dismissed")
    }


}

interface DialogueListener {
    fun onDismissDialogue()
}