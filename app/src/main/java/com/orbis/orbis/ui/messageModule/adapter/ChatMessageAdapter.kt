package com.orbis.orbis.ui.messageModule.adapter

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.*
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.message.MessageModel
import com.orbis.orbis.ui.app.GlideApp
import com.squareup.picasso.Picasso
import com.stfalcon.imageviewer.StfalconImageViewer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ChatMessageAdapter(
    val context: Context,
    val chats: ArrayList<MessageModel>,
    val myKey: String
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var previousPlayer: SimpleExoPlayer? = null
    private var previousPlayButton: ImageView? = null
    override fun getItemViewType(position: Int): Int {
        val item = chats[position]
        if (item.senderId == myKey) {
            when (item.type) {
                "TEXT" -> {
                    return 1
                }
                "IMAGE" -> {
                    return 2
                }
                "VIDEO" -> {
                    return 3
                }
            }
        } else {
            when (item.type) {
                "TEXT" -> {
                    return 4
                }
                "IMAGE" -> {
                    return 5
                }
                "VIDEO" -> {
                    return 6
                }
            }
        }
        return 0

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            return ViewHolderMyText(
                LayoutInflater.from(context).inflate(R.layout.right_message_item, parent, false)
            )
        }
        if (viewType == 2) {
            return ViewHolderMyImage(
                LayoutInflater.from(context)
                    .inflate(R.layout.right_message_image_item, parent, false)
            )
        }
        if (viewType == 3) {
            return ViewHolderMyVideo(
                LayoutInflater.from(context)
                    .inflate(R.layout.right_message_video_item, parent, false)
            )
        }
        if (viewType == 5) {
            return ViewHolderSenderImage(
                LayoutInflater.from(context)
                    .inflate(R.layout.left_message_image_item, parent, false)
            )
        }
        if (viewType == 6) {
            return ViewHolderSenderVideo(
                LayoutInflater.from(context)
                    .inflate(R.layout.left_message_video_item, parent, false)
            )
        }
        return ViewHolderSenderText(
            LayoutInflater.from(context).inflate(R.layout.left_message_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == 1) {
            val viewHolder = holder as ViewHolderMyText
            val sdf = SimpleDateFormat("hh:mm aa")
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = chats[position].timestamp
            val time = sdf.format(calendar.time)
            viewHolder.binding?.txtTime?.text = time
            viewHolder.binding?.txtMessage?.text = chats[position].message
        }
        if (holder.itemViewType == 2) {
            val viewHolder = holder as ViewHolderMyImage
            val sdf = SimpleDateFormat("hh:mm aa")
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = chats[position].timestamp
            val time = sdf.format(calendar.time)
            viewHolder.binding?.txtTime?.text = time
            if (chats[position].mediaUrl.isNotEmpty()) {
                val userImage =
                    Firebase.storage.getReference(
                        Constants.CHAT_IMAGE +
                                chats[position].mediaUrl
                    )
                GlideApp.with(context).load(userImage).into(holder.binding?.imageMsg!!)
                holder.binding.imageMsg.setOnClickListener {
                    val images = listOf(userImage)
                    val viewer = StfalconImageViewer.Builder(context, images) { imageView, s ->
                        GlideApp.with(context).load(s).into(imageView)
                    }.withStartPosition(0).allowZooming(true).allowSwipeToDismiss(true)
                    viewer.show()
                }
            } else {
                holder.binding?.imageMsg?.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_logo
                    )
                )
            }


        } else if (holder.itemViewType == 3) {
            val viewHolder = holder as ViewHolderMyVideo
            val sdf = SimpleDateFormat("hh:mm aa")
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = chats[position].timestamp
            val time = sdf.format(calendar.time)
            viewHolder.binding?.txtTime?.text = time
            val thumbPath =
                chats[position].mediaUrl.replaceAfter(".", "").replace(".", "") + "_200x200.jpg"
            val thumbImage =
                Firebase.storage.getReference(
                    Constants.CHAT_VIDEO + thumbPath
                )
            GlideApp.with(context).load(thumbImage).into(holder.binding?.imageMsg!!)

            viewHolder.binding?.play2?.visibility = View.VISIBLE
            viewHolder.binding?.imageMsg?.visibility = View.VISIBLE
            if (chats[position].videoUrl.isNotEmpty()) {

                val player = SimpleExoPlayer.Builder(context).build()
                viewHolder.binding?.playerView?.player = player
                val mediaItem = MediaItem.fromUri(chats[position].videoUrl)
                player.setMediaItem(mediaItem)
                player.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        if (isPlaying) {
                            viewHolder.binding?.playerView?.visibility = View.VISIBLE
                            if (previousPlayer != null && previousPlayer != player) {
                                previousPlayer?.pause()
                            }
                            //interaction.onPlayerStart(position, player)
                            previousPlayer = player
                        }
                    }
                })

                viewHolder.binding?.play2?.setOnClickListener {
                    player.prepare()
                    if (previousPlayer != null) {
                        previousPlayer?.pause()
                    }
                    //interaction.onPlayerStart(position, player)
                    previousPlayer = player
                    player.playWhenReady = true
                    viewHolder.binding.play2.visibility = View.GONE
                    viewHolder.binding.imageMsg.visibility = View.GONE
                    viewHolder.binding.playerView.visibility = View.VISIBLE
                }
                val controlView: PlayerControlView? =
                    viewHolder.binding?.playerView?.findViewById(R.id.exo_controller)
                val fullScreen = controlView?.findViewById(R.id.exo_fullscreen) as ImageButton
                fullScreen.setOnClickListener {
                    val fullScreenPlayerView = PlayerView(context)
                    val dialog = object :
                        Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
                        override fun onBackPressed() {
                            PlayerView.switchTargetView(
                                player,
                                fullScreenPlayerView,
                                viewHolder.binding?.playerView
                            )
                            super.onBackPressed()
                        }
                    }
                    dialog.addContentView(
                        fullScreenPlayerView,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    dialog.show()
                    PlayerView.switchTargetView(
                        player,
                        viewHolder.binding?.playerView,
                        fullScreenPlayerView
                    )
                }
            }
        } else if (holder.itemViewType == 4) {
            val viewHolder = holder as ViewHolderSenderText
            val sdf = SimpleDateFormat("hh:mm aa")
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = chats[position].timestamp
            val time = sdf.format(calendar.time)
            viewHolder.binding?.txtTime?.text = time
            viewHolder.binding?.txtMessage?.text = chats[position].message
        }
        if (holder.itemViewType == 5) {
            val viewHolder = holder as ViewHolderSenderImage
            val sdf = SimpleDateFormat("hh:mm aa")
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = chats[position].timestamp
            val time = sdf.format(calendar.time)
            viewHolder.binding?.txtTime?.text = time
            if (chats[position].mediaUrl.isNotEmpty()) {
                val userImage =
                    Firebase.storage.getReference(
                        Constants.CHAT_IMAGE +
                                chats[position].mediaUrl
                    )
                GlideApp.with(context).load(userImage).into(holder.binding?.imageMsg!!)
                holder.binding.imageMsg.setOnClickListener {
                    val images = listOf(userImage)
                    val viewer = StfalconImageViewer.Builder(context, images) { imageView, s ->
                        GlideApp.with(context).load(s).into(imageView)
                    }.withStartPosition(0).allowZooming(true).allowSwipeToDismiss(true)
                    viewer.show()
                }
            } else {
                holder.binding?.imageMsg?.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_logo
                    )
                )
            }
        } else if (holder.itemViewType == 6) {
            val viewHolder = holder as ViewHolderSenderVideo
            val sdf = SimpleDateFormat("hh:mm aa")
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = chats[position].timestamp
            val time = sdf.format(calendar.time)
            viewHolder.binding?.txtTime?.text = time
            val thumbPath =
                chats[position].mediaUrl.replaceAfter(".", "").replace(".", "") + "_200x200.jpg"
            val thumbImage =
                Firebase.storage.getReference(
                    Constants.CHAT_VIDEO + thumbPath
                )
            GlideApp.with(context).load(thumbImage).into(holder.binding?.imageMsg!!)
            viewHolder.binding?.play2?.visibility = View.VISIBLE
            viewHolder.binding?.imageMsg?.visibility = View.VISIBLE
            if (chats[position].videoUrl.isNotEmpty()) {

                val player = SimpleExoPlayer.Builder(context).build()
                viewHolder.binding?.playerView?.player = player
                val mediaItem = MediaItem.fromUri(chats[position].videoUrl)
                player.setMediaItem(mediaItem)
                player.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        if (isPlaying) {
                            viewHolder.binding?.playerView?.visibility = View.VISIBLE
                            if (previousPlayer != null && previousPlayer != player) {
                                previousPlayer?.pause()
                            }
                            //interaction.onPlayerStart(position, player)
                            previousPlayer = player
                        }
                    }
                })

                viewHolder.binding?.play2?.setOnClickListener {
                    player.prepare()
                    if (previousPlayer != null) {
                        previousPlayer?.pause()
                    }
                    //interaction.onPlayerStart(position, player)
                    previousPlayer = player
                    player.playWhenReady = true
                    viewHolder.binding.play2.visibility = View.GONE
                    viewHolder.binding.imageMsg.visibility = View.GONE
                    viewHolder.binding.playerView.visibility = View.VISIBLE
                }
                val controlView: PlayerControlView? =
                    viewHolder.binding?.playerView?.findViewById(R.id.exo_controller)
                val fullScreen = controlView?.findViewById(R.id.exo_fullscreen) as ImageButton
                fullScreen.setOnClickListener {
                    val fullScreenPlayerView = PlayerView(context)
                    val dialog = object :
                        Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
                        override fun onBackPressed() {
                            PlayerView.switchTargetView(
                                player,
                                fullScreenPlayerView,
                                viewHolder.binding?.playerView
                            )
                            super.onBackPressed()
                        }
                    }
                    dialog.addContentView(
                        fullScreenPlayerView,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    dialog.show()
                    PlayerView.switchTargetView(
                        player,
                        viewHolder.binding?.playerView,
                        fullScreenPlayerView
                    )
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return chats.size
    }

    class ViewHolderMyText(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: RightMessageItemBinding? = DataBindingUtil.bind(itemView)
    }

    class ViewHolderMyImage(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: RightMessageImageItemBinding? = DataBindingUtil.bind(itemView)
    }

    class ViewHolderMyVideo(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: RightMessageVideoItemBinding? = DataBindingUtil.bind(itemView)
    }

    class ViewHolderSenderText(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: LeftMessageItemBinding? = DataBindingUtil.bind(itemView)
    }

    class ViewHolderSenderImage(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: LeftMessageImageItemBinding? = DataBindingUtil.bind(itemView)
    }

    class ViewHolderSenderVideo(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: LeftMessageVideoItemBinding? = DataBindingUtil.bind(itemView)
    }
}