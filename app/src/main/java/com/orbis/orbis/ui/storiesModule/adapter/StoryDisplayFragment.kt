import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import android.text.format.DateFormat
import android.view.MotionEvent
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
//import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentStoryDisplayBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.story.StoryModel
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.storiesModule.adapter.StoriesProgressView

import com.orbis.orbis.ui.storiesModule.utils.OnSwipeTouchListener
import com.orbis.orbis.ui.storiesModule.utils.hide
import com.orbis.orbis.ui.storiesModule.utils.show
import com.orbis.orbis.ui.storiesModule.views.PageViewOperator
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class StoryDisplayFragment(val position: Int, val storyModels: ArrayList<StoryModel>) : Fragment(),
    StoriesProgressView.StoriesListener {


    private var simpleExoPlayer: SimpleExoPlayer? = null
    private lateinit var mediaDataSourceFactory: DataSource.Factory
    private var pageViewOperator: PageViewOperator? = null
    private var counter = 0
    private var prevStory = 0
    private var pressTime = 0L
    private var limit = 500L
    private var onResumeCalled = false
    private var onVideoPrepared = false
    lateinit var binding: FragmentStoryDisplayBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_story_display, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.storyDisplayVideo.useController = false
        Log.d(
            "checkStoryCount",
            position.toString() + " groupName:" + storyModels[position].group.name + " totalPosts: " + storyModels[position].posts.size + " counter: " + counter.toString()
        )
        binding.storyDisplayNick.text = storyModels[position].group.name
        updateStory()
        setUpUi()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.pageViewOperator = context as PageViewOperator
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        onResumeCalled = true
        if (storyModels[position].posts[counter].type == "VIDEO" && !onVideoPrepared) {
            simpleExoPlayer?.playWhenReady = false
            return
        }

        simpleExoPlayer?.seekTo(5)
        simpleExoPlayer?.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        simpleExoPlayer?.playWhenReady = false
        binding.storiesProgressView?.abandon()
    }

    override fun onComplete() {
        simpleExoPlayer?.release()
        pageViewOperator?.nextPageView()
    }

    override fun onPrev() {
        if (prevStory > 0) {
            counter = prevStory - 1
            Log.d("startingNext", counter.toString())
            updateStory()
        }
    }

    override fun onNext() {
        if (prevStory + 1 < storyModels[position].posts.size) {
            counter = prevStory + 1
            Log.d("startingNext", counter.toString())
            updateStory()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        simpleExoPlayer?.release()
    }

    private fun updateStory() {
        pauseCurrentStory()
        simpleExoPlayer?.stop()
        binding.storyDisplayVideoProgress.visibility = View.VISIBLE
        if (storyModels[position].posts[counter].type == "VIDEO") {
            binding.storyDisplayVideo.show()
            binding.storyDisplayImage.hide()
            binding.storyDisplayVideoProgress.show()
            prevStory = counter
            binding.storiesProgressView?.startStories(counter)
            //initializePlayer()
        } else {
            binding.storyDisplayVideo.hide()
            binding.storyDisplayVideoProgress.hide()
            binding.storyDisplayImage.show()
            downloadStoryImage(storyModels[position].posts[counter])
            // Glide.with(this).load(stories[counter].url).into(storyDisplayImage)
        }

        val calendar = Calendar.getInstance()
        calendar.time =
            ViewUtils.convertTimeStampToDate(storyModels[position].posts[counter].timestamp)!!
        binding.storyDisplayTime.text = DateFormat.format("MM/dd/yyyy", calendar)
            .toString() + " at " + DateFormat.format("hh:mm aa", calendar).toString()

        binding.storyUsername.text = storyModels[position].posts[counter].user?.displayName
        //Glide.with(this).load(stories[counter].userPicUrl).circleCrop().into(storyUserProfilePicture)

        if (storyModels[position].posts[counter].title.isNotEmpty()) {
            if (storyModels[position].posts[counter].title.length > 250) {

                binding.postTv.visibility = View.GONE
                binding.fullScreenPostTv.text = storyModels[position].posts[counter].title
                binding.fullScreenPostTv.visibility = View.VISIBLE
//                storyDisplayImage.hide()
//                storyDisplayVideo.hide()
            } else {
                binding.postTv.text = storyModels[position].posts[counter].title
                binding.postTv.visibility = View.VISIBLE
                binding.fullScreenPostTv.visibility = View.GONE
            }


        } else {
            binding.fullScreenPostTv.visibility = View.GONE
            binding.postTv.visibility = View.GONE
        }
    }

    private fun downloadStoryImage(feedPost: FeedPost) {

        if (feedPost.mediaUrls.isNotEmpty()) {
            binding.storyDisplayVideoProgress.visibility = View.VISIBLE
            val storage =
                Firebase.storage.getReference(
                    Constants.POST_PHOTO_STORAGE + Utils.getImageUrl680(
                        feedPost.mediaUrls[0]
                    )
                )
            GlideApp.with(requireContext()).load(storage)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        prevStory = counter
                        onNext()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.storyDisplayImage
                        prevStory = counter
                        binding.storiesProgressView?.startStories(counter)
                        binding.storyDisplayVideoProgress.visibility = View.GONE
                        return true
                    }

                }).submit()
        } else {
            Log.e("emptyStoryFound", "empty")
            prevStory = counter
            onNext()
        }

    }

//    private fun initializePlayer() {
//        if (simpleExoPlayer == null) {
//            simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(requireContext())
//        } else {
//            simpleExoPlayer?.release()
//            simpleExoPlayer = null
//            simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(requireContext())
//        }
//
//        mediaDataSourceFactory = CacheDataSourceFactory(
//            StoryApp.simpleCache,
//            DefaultHttpDataSourceFactory(
//                Util.getUserAgent(
//                    context,
//                    Util.getUserAgent(requireContext(), getString(R.string.app_name))
//                )
//            )
//        )
//        val mediaSource = ProgressiveMediaSource.Factory(mediaDataSourceFactory).createMediaSource(
//            Uri.parse(stories[counter].url)
//        )
//        simpleExoPlayer?.prepare(mediaSource, false, false)
//        if (onResumeCalled) {
//            simpleExoPlayer?.playWhenReady = true
//        }
//
//        storyDisplayVideo.setShutterBackgroundColor(Color.BLACK)
//        storyDisplayVideo.player = simpleExoPlayer
//
//        simpleExoPlayer?.addListener(object : Player.EventListener {
//            override fun onPlayerError(error: ExoPlaybackException?) {
//                super.onPlayerError(error)
//                storyDisplayVideoProgress.hide()
//                if (counter == stories.size.minus(1)) {
//                    pageViewOperator?.nextPageView()
//                } else {
//                    storiesProgressView?.skip()
//                }
//            }
//
//            override fun onLoadingChanged(isLoading: Boolean) {
//                super.onLoadingChanged(isLoading)
//                if (isLoading) {
//                    storyDisplayVideoProgress.show()
//                    pressTime = System.currentTimeMillis()
//                    pauseCurrentStory()
//                } else {
//                    storyDisplayVideoProgress.hide()
//                    storiesProgressView?.getProgressWithIndex(counter)
//                        ?.setDuration(simpleExoPlayer?.duration ?: 8000L)
//                    onVideoPrepared = true
//                    resumeCurrentStory()
//                }
//            }
//        })
//    }

    private fun setUpUi() {
        val touchListener = object : OnSwipeTouchListener(requireActivity()) {
            override fun onSwipeTop() {
                Toast.makeText(activity, "onSwipeTop", Toast.LENGTH_LONG).show()
            }

            override fun onSwipeBottom() {
                Toast.makeText(activity, "onSwipeBottom", Toast.LENGTH_LONG).show()
                requireActivity().finish()
            }

            override fun onClick(view: View) {
                when (view) {
                    binding.next -> {
                        prevStory = counter
                        onNext()
                    }
                    binding.previous -> {
                        prevStory = counter
                        onPrev()
                    }
                }
            }

            override fun onLongClick() {
                hideStoryOverlay()
            }

            override fun onTouchView(view: View, event: MotionEvent): Boolean {
                super.onTouchView(view, event)
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        pressTime = System.currentTimeMillis()
                        pauseCurrentStory()
                        return false
                    }
                    MotionEvent.ACTION_UP -> {
                        showStoryOverlay()
                        resumeCurrentStory()
                        return limit < System.currentTimeMillis() - pressTime
                    }
                }
                return false
            }
        }
        binding.previous.setOnTouchListener(touchListener)
        binding.next.setOnTouchListener(touchListener)

        binding.storiesProgressView?.setStoriesCountDebug(
            storyModels[position].posts.size, counter
        )
        binding.storiesProgressView?.setAllStoryDuration(4000L)
        binding.storiesProgressView?.setStoriesListener(this)

        //Glide.with(this).load(storyGroup.groupPicUrl).circleCrop().into(storyDisplayProfilePicture)

    }

    private fun showStoryOverlay() {
        if (binding.storyOverlay.alpha != 0F) return

        binding.storyOverlay.animate()
            .setDuration(100)
            .alpha(1F)
            .start()
    }

    private fun hideStoryOverlay() {
        if (binding.storyOverlay.alpha != 1F) return

        binding.storyOverlay.animate()
            .setDuration(200)
            .alpha(0F)
            .start()
    }


    fun pauseCurrentStory() {
        simpleExoPlayer?.playWhenReady = false
        binding.storiesProgressView?.pause()
    }

    fun resumeCurrentStory() {
        if (onResumeCalled) {
            simpleExoPlayer?.playWhenReady = true
            showStoryOverlay()
            binding.storiesProgressView?.resume()
        }
    }


}