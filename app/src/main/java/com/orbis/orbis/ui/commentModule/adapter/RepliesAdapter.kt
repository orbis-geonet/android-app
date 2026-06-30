package com.orbis.orbis.ui.commentModule.adapter


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.EachCommentLayoutBinding
import com.orbis.orbis.databinding.EachReplyLayoutBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.auth.UserProfile
import com.orbis.orbis.models.posts.ReplyModel
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.ui.commentModule.viewModel.Reply
import com.orbis.orbis.ui.settingsModule.viewModel.Place
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.collections.ArrayList


class RepliesAdapter(
    val context: Context,
    val replies: ArrayList<ReplyModel>,
    val parentPos: Int,
    val interaction: ReplyInteraction
) :
    RecyclerView.Adapter<RepliesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_reply_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reply = replies[position]
        val text = "<b>" + reply.user.displayName + "</b> " + reply.text
        holder.binding?.replyTv?.text = Html.fromHtml(text)
        holder.binding?.likesTv?.text = reply.likesCount.toString()
        ViewUtils.loadUserProfilePic(
            context,
            holder.binding?.userIconIv as ImageView,
            reply.user.imageName,
            reply.user.providerImageUrl
        )

        if (reply.userLiked) {
            holder.binding?.likesIv?.setOnClickListener {
                reply.userLiked = false
                reply.likesCount--
                notifyItemChanged(position)
                interaction.onUnLike(reply.commentKey, position, parentPos)
            }
            holder.binding?.likesIv?.setColorFilter(
                ContextCompat.getColor(
                    context,
                    R.color.black
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        } else {
            holder.binding?.likesIv?.setOnClickListener {
                reply.userLiked = true
                reply.likesCount++
                notifyItemChanged(position)
                interaction.onLike(reply.commentKey, position, parentPos)
            }
            holder.binding?.likesIv?.clearColorFilter()
        }
        holder.binding?.reloadIv?.setOnClickListener {
            interaction.onReply(parentPos)
        }
        holder.binding?.cardId?.setOnLongClickListener {
            menuClick(holder.binding.cardId, reply.commentKey, reply.user, position)
            true
        }
        holder.binding?.userIconIv?.setOnClickListener {
            val intent = Intent(context, ProfileActivity::class.java)
            intent.putExtra("userKey", reply.user.userKey)
            intent.putExtra("displayName", reply.user.displayName)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return replies.size
    }

    private fun menuClick(
        user_menu_iv: View,
        commentKey: String,
        user: UserProfile,
        position: Int
    ) {
        // When user click on the Button 1, create a PopupMenu.
        // And anchor Popup to the Button 2.
        val wrapper: ContextThemeWrapper = ContextThemeWrapper(context, R.style.PopupMenu)
        val popup = PopupMenu(wrapper, user_menu_iv, Gravity.START)


        val menu: Menu = popup.getMenu()
        if (user.userKey == PrefManager(context).getUserKey()) {
            menu.add("Delete")
        }
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val menuTitle = menuItem.title.toString()
            val spannableString = SpannableString(menuTitle)
            spannableString.setSpan(
                AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0,
                spannableString.length,
                0
            )
            menuItem.title = spannableString
        }
        // Register Menu Item Click event.
        popup.setOnMenuItemClickListener { item -> menuItemClicked(item, commentKey, position) }
        // Show the PopupMenu.
        popup.show()
    }

    // When user click on Menu Item.
    // @return true if event was handled.
    private fun menuItemClicked(item: MenuItem, commentKey: String, position: Int): Boolean {
        val title = item.title.toString()
        Log.d("postMenuClick", title)
        if (title == "Delete") {
            interaction.onDelete(commentKey, position, parentPos)
        }
        return true
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachReplyLayoutBinding? = DataBindingUtil.bind(itemView)
    }

    interface ReplyInteraction {
        fun onLike(commentKey: String, replyPosition: Int, parentPos: Int)
        fun onUnLike(commentKey: String, replyPosition: Int, parentPos: Int)
        fun onReply(parentPos: Int)
        fun onDelete(commentKey: String, replyPosition: Int, parentPos: Int)
    }
}
