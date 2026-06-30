package com.orbis.orbis.ui.homeModule.views

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ActivityPostDetailsBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.FeedContent
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.posts.CreateCommentBody
import com.orbis.orbis.models.user.User
import com.orbis.orbis.network.ApiInterface
import com.orbis.orbis.network.SwaggerApiClient
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.commentModule.adapter.CommentsAdapter
import com.orbis.orbis.ui.commentModule.views.CommentDialogFragment
import com.orbis.orbis.ui.commentModule.views.CommentDialogue
import com.orbis.orbis.ui.eventsModule.views.EventDetailsDialogFragment
import com.orbis.orbis.ui.groupsModule.adapter.GroupPostsAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.groupsModule.views.GroupDetailsActivity
import com.orbis.orbis.ui.groupsModule.views.ReportDialog
import com.orbis.orbis.ui.newsFeedModule.adapter.DetailsPostsAdapter
import com.orbis.orbis.ui.newsFeedModule.adapter.StatusAdapter
import com.orbis.orbis.ui.newsFeedModule.viewModel.FeedViewModel
import com.orbis.orbis.ui.placesModule.PlaceViewModel

import com.orbis.orbis.ui.storiesModule.views.StoryActivity
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.IndexOutOfBoundsException

@AndroidEntryPoint
class PostDetailsActivity : AppCompatActivity(), GroupPostsAdapter.PostCardInteraction,
    StatusAdapter.StatusCardInteraction, CommentsAdapter.CommentInteraction {
    lateinit var binding: ActivityPostDetailsBinding
    val posts: ArrayList<FeedContent> = ArrayList()
    lateinit var feedAdapter: DetailsPostsAdapter

    lateinit var viewModel: FeedViewModel
    lateinit var groupViewModel: GroupViewModel
    lateinit var placeViewModel: PlaceViewModel
    lateinit var profileViewModel: ProfileViewModel
    val isPostCreated = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_post_details)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = Color.TRANSPARENT
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }
        initView()
    }

    private fun initView() {
        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]
        groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        placeViewModel = ViewModelProvider(this)[PlaceViewModel::class.java]
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        val postKey = intent.getStringExtra("postKey")!!
        placeViewModel.getPost(postKey)
        binding.imageView7.setOnClickListener {
            finish()
        }
        binding.detailsRecycler.layoutManager = LinearLayoutManager(this)
        feedAdapter = DetailsPostsAdapter(posts, this, this, this, this)
        binding.detailsRecycler.adapter = feedAdapter
        placeViewModel.getPost.observe(this) {
            posts.clear()
            posts.add(FeedContent("POST", it))
            downloadContent()
            Log.d("postFetched", it.type)
            if (it.type == "EVENT") {
                binding.constraintLayout2.visibility = View.GONE
            } else {
                viewModel.getCommentsByPost(it.postKey, 0)
            }
            feedAdapter.notifyDataSetChanged()
        }
        viewModel.comments.observe(this) {
            if (posts.size == 1) {
                val feed = FeedContent()
                feed.comments.addAll(it)
                posts.add(feed)
            } else if (posts.size > 1) {
                posts[posts.size - 1].comments.clear()
                posts[posts.size - 1].comments.addAll(it)
            }
            feedAdapter.notifyItemChanged(posts.size - 1)
        }
        viewModel.commentPosted.observe(this) {

            binding.userNameEt.setText("")
            for (i in 0 until posts[posts.size - 1].comments.size) {
                posts[posts.size - 1].comments[i].selectedForReply = false
            }
            if (it) {
                posts[posts.size - 1].comments.clear()
                viewModel.getCommentsByPost(postKey, 0)
            }
        }
        viewModel.isLoading.observe(this) {
            binding.loading = it
        }
        binding.sendIv.setOnClickListener {
            createComment()
        }
        if (Constants.userImage.isNotEmpty()) {
            Picasso.get().load(Constants.userImage).into(binding.userIconIv)
        }
        profileViewModel.getMyProfile()
        profileViewModel.myProfile.observe(this) {
            ViewUtils.loadUserProfilePic(
                this,
                binding.userIconIv,
                it.imageName,
                it.providerImageUrl
            )
        }
        groupViewModel.attendEvent.observe(this) {
            if (it) {
                getAttendees(pendingAttend)
                posts[pendingAttend].post?.attending = true
            }
            feedAdapter.notifyItemChanged(pendingAttend)
        }
        groupViewModel.unattendEvent.observe(this) {
            if (it) {
                getAttendees(pendingAttend)
                posts[pendingAttend].post?.attending = false
            }
            feedAdapter.notifyItemChanged(pendingAttend)
        }
        groupViewModel.isLoading.observe(this) {
            binding.loading = it
        }

    }

    private fun createComment() {
        val text = binding.userNameEt.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_comment), Toast.LENGTH_SHORT).show()
        } else {
            binding.loading = true
            val body = CreateCommentBody(text)
            for (comment in posts[posts.size - 1].comments) {
                if (comment.selectedForReply) {
                    body.replyToKey = comment.commentKey
                }
            }
            viewModel.postComment(posts[0].post?.postKey!!, body)
        }
    }

    private fun downloadContent() {
        val ind = 0
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


    private fun downloadAudio(i: Int, post: FeedPost?) {
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



    var needVideoRetry = false
    var tryCount = 0


    private fun downloadVideo(i: Int, post: FeedPost?) {
        try {
            val thumbPath =
                posts[i].post!!.mediaUrls[0].replaceAfter(".", "").replace(".", "") + "_400x400.jpg"
            //  Log.d("videoThumbPath",thumbPath)
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
        val token = "Bearer " + PrefManager(this).getIdToken()

        val api = SwaggerApiClient.getClient(this).create(ApiInterface::class.java)
        api.getAttendees(token, posts[i].post!!.postKey, 0, 4)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<User>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ArrayList<User>) {
                    posts[0].post?.attendedUsers = t
                    feedAdapter.notifyDataSetChanged()
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
        if (PrefManager(this).getIdToken().isNullOrEmpty()) {
            val intent = Intent(this, AuthActivity::class.java)
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
        MaterialAlertDialogBuilder(this)
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
            supportFragmentManager,
            CommentDialogFragment::class.java.getSimpleName()
        )
    }

    fun goBack() {
        finish()

    }

    override fun onMemberClick() {

    }

    override fun onPlaceTitleClick() {

    }

    override fun onMainMenuClick(imageView: ImageView) {

    }

    override fun onEventClick(position: Int, feedPost: FeedPost) {
        val eventDetails = EventDetailsDialogFragment(feedPost)
        eventDetails.show(supportFragmentManager, "eventDetails")
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
        val intent = Intent(this, StoryActivity::class.java)
        startActivity(intent)
    }

    override fun onTitleClick(group: GroupDetails) {
        val intent = Intent(this, GroupDetailsActivity::class.java)
        intent.putExtra("data", group)
        intent.putExtra("location", Constants.location)
        startActivity(intent)
    }

    override fun onLike(position: Int, commentKey: String) {
        viewModel.likeComment(posts[0].post?.postKey!!, commentKey)
    }

    override fun onUnLike(position: Int, commentKey: String) {
        viewModel.unlikeComment(posts[0].post?.postKey!!, commentKey)
    }

    override fun onReply(position: Int) {
        val prev = posts[posts.size - 1].comments[position].selectedForReply
        for (i in 0 until posts[posts.size - 1].comments.size) {
            posts[posts.size - 1].comments[i].selectedForReply = false
        }
        posts[posts.size - 1].comments[position].selectedForReply = !prev
        feedAdapter.notifyItemChanged(1)
    }

    override fun onDelete(commentKey: String) {
        viewModel.deleteComment(posts[0].post?.postKey!!, commentKey)
    }

}