package com.orbis.orbis.ui.ProfileModule.adapter

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.AlignmentSpan
import android.text.style.ClickableSpan
import android.util.Log
import android.view.*
import android.webkit.URLUtil
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.*
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.FeedContent
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.groupsModule.adapter.AttendedUsersAdapter
import com.orbis.orbis.ui.groupsModule.adapter.GroupPostsAdapter
import com.orbis.orbis.ui.groupsModule.adapter.ImageSliderAdapter
import com.orbis.orbis.ui.groupsModule.adapter.ViewPagerAdapter
import com.orbis.orbis.ui.newsFeedModule.adapter.StatusAdapter
import com.orbis.orbis.ui.placesModule.views.PlaceActivity
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.customViews.ViewMoreTextView
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class ProfilePostsAdapter(
    private val list: ArrayList<FeedContent>,
    private val interaction: GroupPostsAdapter.PostCardInteraction,
    private val interaction2: StatusAdapter.StatusCardInteraction,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var countDownTimer: CountDownTimer? = null
    private var previousPlayer: SimpleExoPlayer? = null
    private var previousPlayButton: ImageView? = null

    // ─── View type constants ───────────────────────────────────────────────────

    companion object {
        private const val TYPE_IMAGE  = 0
        private const val TYPE_AUDIO  = 1
        private const val TYPE_EVENT  = 2
        private const val TYPE_SLIDER = 3
        private const val TYPE_VIDEO  = 4
        private const val TYPE_TEXT   = 5
    }

    // ─── Adapter overrides ────────────────────────────────────────────────────

    override fun getItemViewType(position: Int): Int {
        return when (list[position].type) {
            "POST" -> when (list[position].post?.type) {
                "AUDIO" -> TYPE_AUDIO
                "EVENT" -> TYPE_EVENT
                "VIDEO" -> TYPE_VIDEO
                "TEXT"  -> TYPE_TEXT
                else    -> TYPE_IMAGE
            }
            else -> TYPE_SLIDER
        }
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        fun inflate(layoutRes: Int) =
            LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return when (viewType) {
            TYPE_IMAGE  -> ViewHolderPost(inflate(R.layout.each_post_layout))
            TYPE_AUDIO  -> ViewHolderAudio(inflate(R.layout.each_post_audio_layout))
            TYPE_EVENT  -> ViewHolderEvent(inflate(R.layout.each_event_layout))
            TYPE_SLIDER -> ViewHolderSlider(inflate(R.layout.each_post_slider_layout))
            TYPE_VIDEO  -> ViewHolderVideo(inflate(R.layout.each_post_layout_video))
            TYPE_TEXT   -> ViewHolderText(inflate(R.layout.each_post_layout_text))
            else        -> ViewHolderPost(inflate(R.layout.each_post_layout))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_IMAGE  -> bindImage(holder as ViewHolderPost, position)
            TYPE_AUDIO  -> bindAudio(holder as ViewHolderAudio, position)
            TYPE_EVENT  -> bindEvent(holder as ViewHolderEvent, position)
            TYPE_SLIDER -> bindSlider(holder as ViewHolderSlider, position)
            TYPE_VIDEO  -> bindVideo(holder as ViewHolderVideo, position)
            TYPE_TEXT   -> bindText(holder as ViewHolderText, position)
        }
    }

    // ─── Bind helpers ─────────────────────────────────────────────────────────

    private fun bindImage(holder: ViewHolderPost, position: Int) {
        val b = holder.binding ?: return
        val post = list[holder.bindingAdapterPosition].post ?: return
        b.data = post

        bindPostText(b.postTv, post.details)
        bindAuthorHeader(
            userCircle   = b.userShadowCircle as ImageView,
            userNameTv   = b.userNameTv,
            shareIconIv  = b.shareIconIv,
            shareNameTv  = b.shareNameTv,
            post         = post,
            borderSetter = { b.userShadowCircle.borderWidth = it }
        )
        bindLocation(b.locationPinIv, b.locationNameTv, post)
        bindImageSlider(b.checkInViewpager, b.tabLayout, post, position)
        bindLikeButton(b.likesIv, b.likesTv, post, position)
        b.commentsTv.text = post.commentsCount.toString()
        b.commentsIv.setOnClickListener { interaction.onCommentClicked(position, post) }
        b.userMenuIv.setOnClickListener { menuClick(b.userMenuIv, post, position) }
        b.dateTv.text = ViewUtils.convertTimeStampToFormatted(post.timestamp)
        bindShareButtons(b.shareIconIv, b.shareNameTv, post)
        b.executePendingBindings()
    }

    private fun bindAudio(holder: ViewHolderAudio, position: Int) {
        val b = holder.binding ?: return
        val post = list[holder.bindingAdapterPosition].post ?: return
        b.data = post

        bindPostText(b.postTv, post.details)
        bindAuthorHeader(
            userCircle   = b.userShadowCircle as ImageView,
            userNameTv   = b.userNameTv,
            shareIconIv  = b.shareIconIv,
            shareNameTv  = b.shareNameTv,
            post         = post,
            borderSetter = { b.userShadowCircle.borderWidth = it }
        )
        bindLocation(b.locationPinIv, b.locationNameTv, post)
        bindAudioPlayer(holder, post)
        bindLikeButton(b.likesIv, b.likesTv, post, position)
        b.commentsTv.text = post.commentsCount.toString()
        b.commentsIv.setOnClickListener { interaction.onCommentClicked(position, post) }
        b.userMenuIv.setOnClickListener { menuClick(b.userMenuIv, post, position) }
        b.dateTv.text = ViewUtils.convertTimeStampToFormatted(post.timestamp)
        bindShareButtons(b.shareIconIv, b.shareNameTv, post)
        b.executePendingBindings()
    }

    private fun bindVideo(holder: ViewHolderVideo, position: Int) {
        val b = holder.binding ?: return
        val post = list[holder.bindingAdapterPosition].post ?: return
        b.data = post

        bindPostText(b.postTv, post.details)
        bindAuthorHeader(
            userCircle   = b.userShadowCircle as ImageView,
            userNameTv   = b.userNameTv,
            shareIconIv  = b.shareIconIv,
            shareNameTv  = b.shareNameTv,
            post         = post,
            borderSetter = { b.userShadowCircle.borderWidth = it }
        )
        bindLocation(b.locationPinIv, b.locationNameTv, post)
        bindVideoPlayer(holder, post, position)
        bindLikeButton(b.likesIv, b.likesTv, post, position)
        b.commentsTv.text = post.commentsCount.toString()
        b.commentsIv.setOnClickListener { interaction.onCommentClicked(position, post) }
        b.userMenuIv.setOnClickListener { menuClick(b.userMenuIv, post, position) }
        b.dateTv.text = ViewUtils.convertTimeStampToFormatted(post.timestamp)
        bindShareButtons(b.shareIconIv, b.shareNameTv, post)
        b.executePendingBindings()
    }

    private fun bindText(holder: ViewHolderText, position: Int) {
        val b = holder.binding ?: return
        val post = list[holder.bindingAdapterPosition].post ?: return
        b.data = post
        b.userShadowCircle.borderWidth = 3

        bindPostText(b.postTv, post.details)
        bindRichLink(holder, post)
        bindAuthorHeader(
            userCircle   = b.userShadowCircle as ImageView,
            userNameTv   = b.userNameTv,
            shareIconIv  = b.shareIconIv,
            shareNameTv  = b.shareNameTv,
            post         = post,
            borderSetter = { b.userShadowCircle.borderWidth = it }
        )
        bindLocation(b.locationPinIv, b.locationNameTv, post)
        bindLikeButton(b.likesIv, b.likesTv, post, position)
        b.commentsTv.text = post.commentsCount.toString()
        b.commentsIv.setOnClickListener { interaction.onCommentClicked(position, post) }
        b.userMenuIv.setOnClickListener { menuClick(b.userMenuIv, post, position) }
        b.dateTv.text = ViewUtils.convertTimeStampToFormatted(post.timestamp)
        bindShareButtons(b.shareIconIv, b.shareNameTv, post)
        b.executePendingBindings()
    }

    private fun bindEvent(holder: ViewHolderEvent, position: Int) {
        val b = holder.binding ?: return
        val post = list[holder.bindingAdapterPosition].post ?: return
        b.data = post

        if (!post.mediaUrls.isNullOrEmpty()) {
            val storage = Firebase.storage.getReference(
                Constants.EVENT_STORAGE + Utils.getImageUrl200(post.mediaUrls[0])
            )
            GlideApp.with(context).load(storage).into(b.eventIv)
        }

        if (post.details.isNullOrEmpty()) {
            b.eventTv.visibility = View.GONE
        } else {
            b.eventTv.visibility = View.VISIBLE
            b.eventTv.setContent(post.details)
            b.eventTv.setTextMaxLength(200)
            b.eventTv.setSeeMoreTextColor(R.color.view_more_blue)
        }

        if (!post.attendedUsers.isNullOrEmpty()) {
            b.confirmedTv.visibility = View.VISIBLE
            b.recyclerView.visibility = View.VISIBLE
            b.recyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            b.recyclerView.adapter = AttendedUsersAdapter(post.attendedUsers, context)
        } else {
            b.confirmedTv.visibility = View.GONE
            b.recyclerView.visibility = View.GONE
        }

        val (eventStart, eventEnd) = parsePlannedTimes(post)
        b.eventDateTv.text =
            "${eventStart.get(Calendar.DAY_OF_MONTH)} ${SimpleDateFormat("MMMM").format(eventStart.time)}"
        b.eventTimeTv.text =
            "${context.getString(R.string.from)}${eventStart.formatHHmm()}${context.getString(R.string.to)}${eventEnd.formatHHmm()}"

        if (!post.attending) {
            b.goBtn.text = context.getString(R.string.i_go)
            b.goBtn.backgroundTintList = context.getColorStateList(R.color.black_state)
        } else {
            b.goBtn.text = context.getString(R.string.i_going)
            b.goBtn.backgroundTintList = context.getColorStateList(R.color.grey_state)
        }

        b.userMenuIv2.setOnClickListener { menuClick(b.userMenuIv2, post, position) }
        b.goBtn.setOnClickListener {
            if (!post.attending) interaction.onEventAttend(position, post)
            else interaction.onEventNotAttend(position, post)
        }
        b.cardId.setOnClickListener { interaction.onEventClick(position, post) }
        b.executePendingBindings()
    }

    private fun bindSlider(holder: ViewHolderSlider, position: Int) {
        val b = holder.binding ?: return
        val slider = list[position].slider
        b.checkInViewpager.adapter =
            ViewPagerAdapter(position, context, slider, interaction, interaction2)
        setupTabs(position, b.tabLayout, b.checkInViewpager)
        b.executePendingBindings()
    }

    // ─── Repeated-block helpers ───────────────────────────────────────────────

    /**
     * Binds the author/group header. Shows group avatar + user corner badge when
     * the post belongs to a group; shows just the user avatar otherwise.
     */
    private fun bindAuthorHeader(
        userCircle: ImageView,
        userNameTv: TextView,
        shareIconIv: ImageView,
        shareNameTv: View,
        post: FeedPost,
        borderSetter: (Int) -> Unit
    ) {
        if (post.group != null) {
            shareNameTv.visibility = View.VISIBLE
            shareIconIv.visibility = View.VISIBLE
            borderSetter(3)
            userNameTv.text = post.group!!.name
            shrinkIfMultiline(userNameTv)
            userNameTv.setOnClickListener { interaction2.onTitleClick(post.group!!) }
            userCircle.setOnClickListener   { interaction2.onTitleClick(post.group!!) }
            ViewUtils.loadGroupPhoto(context, userCircle, post.group?.imageName)
            ViewUtils.loadUserProfilePic(context, shareIconIv, post.user?.imageName, post.user?.providerImageUrl)
        } else {
            borderSetter(0)
            shareNameTv.visibility = View.GONE
            shareIconIv.visibility = View.GONE
            userNameTv.text = post.user?.displayName?.takeIf { it.isNotEmpty() }
                ?: context.getString(R.string.orbis_user)
            shrinkIfMultiline(userNameTv)
            userNameTv.setOnClickListener { openProfile(post.user?.displayName, post.user?.userKey!!) }
            userCircle.setOnClickListener  { openProfile(post.user?.displayName, post.user?.userKey!!) }
            ViewUtils.loadUserProfilePic(context, userCircle, post.user?.imageName, post.user?.providerImageUrl)
        }
    }

    /** Shows/hides the location pin row and sets up the PlaceActivity click. */
    private fun bindLocation(pinIv: View, nameTv: View, post: FeedPost) {
        val hasLocation = post.place?.name?.isNotEmpty() == true
        pinIv.visibility = if (hasLocation) View.VISIBLE else View.GONE
        if (hasLocation) {
            nameTv.setOnClickListener {
                context.startActivity(
                    Intent(context, PlaceActivity::class.java).putExtra("data", post.place)
                )
            }
        }
    }

    /** Sets the expandable post-detail text, or hides the view when empty. */
    private fun bindPostText(tv: ViewMoreTextView, details: String?) {
        if (details.isNullOrEmpty()) {
            tv.visibility = View.GONE
        } else {
            tv.visibility = View.VISIBLE
            tv.setContent(details)
            tv.setTextMaxLength(200)
            tv.setSeeMoreTextColor(R.color.view_more_blue)
        }
    }

    /** Wires up the like/unlike toggle, count label, and tint. */
    private fun bindLikeButton(likeIv: ImageView, likesTv: TextView, post: FeedPost, position: Int) {
        likesTv.text = post.likesCount.toString()
        if (post.userLiked) {
            likeIv.setColorFilter(
                ContextCompat.getColor(context, R.color.black),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
            likeIv.setOnClickListener {
                if (PrefManager(context).getIdToken().isNullOrEmpty()) { redirectToLogin(); return@setOnClickListener }
                post.userLiked = false
                post.likesCount--
                notifyItemChanged(position)
                interaction.onUnlikeClicked(position, post)
            }
        } else {
            likeIv.clearColorFilter()
            likeIv.setOnClickListener {
                if (PrefManager(context).getIdToken().isNullOrEmpty()) { redirectToLogin(); return@setOnClickListener }
                post.userLiked = true
                post.likesCount++
                notifyItemChanged(position)
                interaction.onLikeClicked(position, post)
            }
        }
    }

    /** Sets the share-icon and share-name click handlers that open a user profile. */
    private fun bindShareButtons(shareIconIv: View, shareNameTv: View, post: FeedPost) {
        val openUser = View.OnClickListener {
            if (post.user != null) openProfile(post.user?.displayName, post.user?.userKey!!)
            else Toast.makeText(context, "No User!", Toast.LENGTH_SHORT).show()
        }
        shareIconIv.setOnClickListener(openUser)
        shareNameTv.setOnClickListener(openUser)
    }

    /** Sets up the image-slider ViewPager + dot tabs for posts with mediaUrls. */
    private fun bindImageSlider(
        viewPager: ViewPager,
        tabLayout: TabLayout,
        post: FeedPost,
        position: Int
    ) {
        if (!post.mediaUrls.isNullOrEmpty()) {
            tabLayout.visibility = if (post.mediaUrls.size > 1) View.VISIBLE else View.GONE
            try {
                viewPager.adapter = ImageSliderAdapter(context, post.mediaUrls)
                setupTabsImage(position, tabLayout, viewPager)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                viewPager.adapter = ImageSliderAdapter(context, ArrayList())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Wires up the ExoPlayer for a video post. */
    private fun bindVideoPlayer(holder: ViewHolderVideo, post: FeedPost, position: Int) {
        val b = holder.binding ?: return
        b.play.visibility = View.VISIBLE
        b.videoThumb.visibility = View.VISIBLE

        if (!post.mediaUrls.isNullOrEmpty()) {
            val thumbPath = post.mediaUrls[0].replaceAfter(".", "").replace(".", "") + "_400x400.jpg"
            loadVideoThumb(
                Firebase.storage.getReference(Constants.POST_VIDEO_STORAGE + thumbPath),
                b.videoThumb,
                context
            )
        }

        if (post.postVideo.isNullOrEmpty()) return

        b.playerView.visibility = View.VISIBLE
        val player = SimpleExoPlayer.Builder(context).build()
        b.playerView.player = player
        player.setMediaItem(MediaItem.fromUri(post.postVideo))
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    if (previousPlayer != null && previousPlayer != player) previousPlayer?.pause()
                    interaction.onPlayerStart(position, player)
                    previousPlayer = player
                }
            }
        })

        b.play.setOnClickListener {
            player.prepare()
            previousPlayer?.pause()
            interaction.onPlayerStart(position, player)
            previousPlayer = player
            player.playWhenReady = true
            b.play.visibility = View.GONE
            b.videoThumb.visibility = View.GONE
        }

        val controlView = b.playerView.findViewById<PlayerControlView>(R.id.exo_controller)
        val fullScreen = controlView?.findViewById<ImageButton>(R.id.exo_fullscreen) ?: return
        fullScreen.setOnClickListener {
            val fullScreenView = PlayerView(context)
            val dialog = object : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
                override fun onBackPressed() {
                    PlayerView.switchTargetView(player, fullScreenView, b.playerView)
                    super.onBackPressed()
                }
            }
            dialog.addContentView(
                fullScreenView,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
            dialog.show()
            PlayerView.switchTargetView(player, b.playerView, fullScreenView)
        }
    }

    /** Wires up the MediaPlayer for an audio post. */
    private fun bindAudioPlayer(holder: ViewHolderAudio, post: FeedPost) {
        val b = holder.binding ?: return
        if (post.postAudio.isNullOrEmpty()) return

        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(post.postAudio)
        mediaPlayer.setOnPreparedListener {
            b.duration.text = SimpleDateFormat("mm:ss").format(Date(mediaPlayer.duration.toLong()))
            b.musicProgress.max = mediaPlayer.duration

            b.playButton.setOnClickListener {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    countDownTimer?.cancel()
                    b.playButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_play_arrow_24))
                } else {
                    mediaPlayer.start()
                    previousPlayButton?.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_play_arrow_24)
                    )
                    b.playButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_pause_24))
                    previousPlayButton = b.playButton
                    seekbarUpdate((mediaPlayer.duration - mediaPlayer.currentPosition).toLong(), holder, mediaPlayer)
                }
            }
        }

        b.musicProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    seekbarUpdate((mediaPlayer.duration - mediaPlayer.currentPosition).toLong(), holder, mediaPlayer)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        mediaPlayer.prepareAsync()

        b.playButton.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                if (mediaPlayer.isPlaying) R.drawable.ic_baseline_pause_24
                else R.drawable.ic_baseline_play_arrow_24
            )
        )
    }

    /** Binds the rich-link preview card for TEXT posts, or hides it. */
    private fun bindRichLink(holder: ViewHolderText, post: FeedPost) {
        val b = holder.binding ?: return
        if (post.richLinkData == null) {
            b.richUrlView.visibility = View.GONE
            b.urlTv.visibility = View.GONE
            return
        }

        b.postTv.visibility = View.INVISIBLE
        b.urlTv.visibility = View.VISIBLE

        val url = post.details.split("\\s".toRegex()).firstOrNull { URLUtil.isValidUrl(it) } ?: ""
        val urlStart = post.details.indexOf(url).coerceAtLeast(0)
        val ss = SpannableString(post.details)
        ss.setSpan(object : ClickableSpan() {
            override fun onClick(v: View) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }, urlStart, (urlStart + url.length).coerceAtMost(post.details.length), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        b.urlTv.movementMethod = LinkMovementMethod.getInstance()
        b.urlTv.text = ss

        b.richUrlView.visibility = View.VISIBLE
        b.richUrlView.setOnClickListener {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        Picasso.get().load(post.richLinkData?.imageUrl).into(b.thumbnail)
        b.urlBase.text    = post.richLinkData?.canonicalUrl?.uppercase()
        b.titleUrl.text   = post.richLinkData?.title
        b.detailsUrl.text = post.richLinkData?.description
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    /** Reduces text size when a name wraps to more than one line. */
    private fun shrinkIfMultiline(tv: TextView) {
        tv.post { if (tv.lineCount > 1) tv.textSize = 12f }
    }

    private fun Calendar.formatHHmm(): String =
        "${get(Calendar.HOUR_OF_DAY)}:${get(Calendar.MINUTE).toString().padStart(2, '0')}"

    private fun parsePlannedTimes(post: FeedPost): Pair<Calendar, Calendar> {
        val start = Calendar.getInstance()
        val end   = Calendar.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            start.time = Date.from(Instant.parse(post.plannedTime))
            end.time   = Date.from(Instant.parse(post.plannedEndTime))
        } else {
            start.time = ViewUtils.convertTimeStampToDate(post.plannedTime)!!
            end.time   = ViewUtils.convertTimeStampToDate(post.plannedEndTime)!!
        }
        return start to end
    }

    private fun loadVideoThumb(
        thumbRef: StorageReference,
        imageView: ImageView,
        ctx: Context,
        retryCount: Int = 0
    ) {
        GlideApp.with(ctx).load(thumbRef)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    if (retryCount < 5) {
                        Handler(Looper.getMainLooper()).postDelayed(
                            { loadVideoThumb(thumbRef, imageView, ctx, retryCount + 1) }, 3000
                        )
                    }
                    return true
                }
                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    imageView.setImageDrawable(resource)
                    return true
                }
            })
            .into(imageView)
    }

    private fun seekbarUpdate(duration: Long, holder: ViewHolderAudio, player: MediaPlayer) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                try {
                    holder.binding?.musicProgress?.progress =
                        (player.duration - millisUntilFinished).toInt()
                    holder.binding?.duration?.text =
                        SimpleDateFormat("mm:ss").format(Date(millisUntilFinished))
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
            override fun onFinish() {
                holder.binding?.playButton?.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.ic_baseline_play_arrow_24)
                )
                holder.binding?.musicProgress?.progress = holder.binding?.musicProgress?.max ?: 0
                player.seekTo(0)
                player.pause()
            }
        }.start()
    }

    private fun menuClick(anchor: ImageView, post: FeedPost, position: Int) {
        val wrapper = ContextThemeWrapper(context, R.style.PopupMenu)
        val popup = PopupMenu(wrapper, anchor, Gravity.START)
        popup.inflate(R.menu.layout_post_menu)
        val menu = popup.menu
        if (post.user?.userKey == PrefManager(context).getUserKey()) {
            menu.add(context.getString(R.string.delete))
        }
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val spanned = SpannableString(item.title).apply {
                setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, length, 0)
            }
            item.title = spanned
        }
        popup.setOnMenuItemClickListener { menuItemClicked(it, post, position) }
        popup.show()
    }

    private fun menuItemClicked(item: MenuItem, post: FeedPost, position: Int): Boolean {
        when (item.title.toString()) {
            context.getString(R.string.share)  -> interaction.onShare(post.postKey, "POST")
            context.getString(R.string.delete) -> interaction.onPostDelete(position, post.postKey, position)
            context.getString(R.string.report) -> interaction.onReport(post.postKey)
        }
        return true
    }

    private fun openProfile(displayName: String?, userKey: String) {
        context.startActivity(
            Intent(context, ProfileActivity::class.java)
                .putExtra("showMyProfile", false)
                .putExtra("displayName", displayName)
                .putExtra("userKey", userKey)
        )
    }

    // ─── Tab helpers ──────────────────────────────────────────────────────────

    fun setupTabs(position: Int, tabLayout: TabLayout, viewPager: ViewPager) {
        tabLayout.removeAllTabs()
        repeat(minOf(10, list[position].slider.size)) { tabLayout.addTab(tabLayout.newTab().setIcon(null)) }
        tabLayout.getTabAt(0)?.select()
        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(p: Int, offset: Float, offsetPx: Int) {}
            override fun onPageSelected(p: Int) { tabLayout.selectTab(tabLayout.getTabAt(p)) }
            override fun onPageScrollStateChanged(state: Int) {}
        })
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab)   { viewPager.currentItem = tab.position }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    fun setupTabsImage(position: Int, tabLayout: TabLayout, viewPager: ViewPager) {
        tabLayout.removeAllTabs()
        repeat(minOf(10, list[position].post!!.mediaUrls.size)) { tabLayout.addTab(tabLayout.newTab().setIcon(null)) }
        tabLayout.getTabAt(0)?.select()
        tabLayout.setupWithViewPager(viewPager)
    }
    private fun redirectToLogin() {
        context.startActivity(
            Intent(context, AuthActivity::class.java).putExtra("goToLogin", true)
        )
    }

    // ─── ViewHolders ──────────────────────────────────────────────────────────

    inner class ViewHolderPost(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachPostLayoutBinding? = DataBindingUtil.bind(itemView)
    }

    inner class ViewHolderAudio(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachPostAudioLayoutBinding? = DataBindingUtil.bind(itemView)
    }

    inner class ViewHolderEvent(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachEventLayoutBinding? = DataBindingUtil.bind(itemView)
    }

    inner class ViewHolderSlider(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachPostSliderLayoutBinding? = DataBindingUtil.bind(itemView)
    }

    inner class ViewHolderVideo(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachPostLayoutVideoBinding? = DataBindingUtil.bind(itemView)
    }

    inner class ViewHolderText(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachPostLayoutTextBinding? = DataBindingUtil.bind(itemView)
    }
}