package com.orbis.orbis.ui.storiesModule.views

import android.animation.Animator
import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ActivityStoryBinding
import com.orbis.orbis.models.BigDataSharConstants
import com.orbis.orbis.models.story.StoryModel
import com.orbis.orbis.ui.newsFeedModule.viewModel.FeedViewModel
import com.orbis.orbis.ui.storiesModule.adapter.StoryPagerAdapter
import com.orbis.orbis.ui.storiesModule.utils.CubeOutTransformer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoryActivity : AppCompatActivity(), StoryInteraction {
    private var stories: ArrayList<StoryModel> = ArrayList()
    lateinit var viewModel: FeedViewModel
    lateinit var binding: ActivityStoryBinding
    var currentPage = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_story)

//        stories = intent.getParcelableArrayListExtra("data")!!
        stories = BigDataSharConstants.storiesArray

        currentPage = intent.getIntExtra("currentPage", 0)
//        Log.d("storyActivityCheck", stories[currentPage].posts[0].type)
        initView()
    }

    private fun initView() {
        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]
        val pager = StoryPagerAdapter(supportFragmentManager, stories, this)
        binding.viewPager.adapter = pager
        binding.viewPager.currentItem = currentPage
        binding.viewPager.offscreenPageLimit = 0
        binding.viewPager.setPageTransformer(
            true,
            CubeOutTransformer()
        )
    }

    private var prevDragPosition = 0
    private fun fakeDrag(forward: Boolean) {
        if (prevDragPosition == 0 && binding.viewPager.beginFakeDrag()) {
            ValueAnimator.ofInt(0, binding.viewPager.width).apply {
                duration = 400L
                interpolator = FastOutSlowInInterpolator()
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator) {}

                    override fun onAnimationEnd(animation: Animator) {
                        removeAllUpdateListeners()
                        try {
                            if (binding.viewPager.isFakeDragging) {
                                binding.viewPager.endFakeDrag()
                            }
                            prevDragPosition = 0
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        removeAllUpdateListeners()
                        if (binding.viewPager.isFakeDragging) {
                            binding.viewPager.endFakeDrag()
                        }
                        prevDragPosition = 0
                    }

                    override fun onAnimationStart(p0: Animator) {}
                })
                addUpdateListener {
                    try {
                        if (!binding.viewPager.isFakeDragging) return@addUpdateListener
                        val dragPosition: Int = it.animatedValue as Int
                        val dragOffset: Float =
                            ((dragPosition - prevDragPosition) * if (forward) -1 else 1).toFloat()
                        prevDragPosition = dragPosition
                        binding.viewPager.fakeDragBy(dragOffset)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.start()
        }
    }

    override fun nextGroup() {
        Log.d("oldNew", "nextGroupRequested ${stories.size} ${binding.viewPager.currentItem}")
        if (binding.viewPager.currentItem < (stories.size - 1)) {
            try {
                Log.d("oldNew", "nextGroupRequesting")
                binding.viewPager.currentItem++
                Log.d("oldNew", "nextGroupShowed ${binding.viewPager.currentItem}")
            } catch (e: Exception) {
                Log.d("oldNew", "exception occured")
                e.printStackTrace()
            }
        }

    }

    override fun prevGroup() {
        if (binding.viewPager.currentItem > 0) {
            binding.viewPager.currentItem--
        }
    }

    override fun seenStory(postKey: String) {
        viewModel.seenStory(postKey)
    }


}

interface StoryInteraction {
    fun nextGroup()
    fun prevGroup()
    fun seenStory(postKey: String)
}