package com.orbis.orbis.ui.commentModule.adapter


import android.content.Context
import android.content.Intent
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.databinding.EachCommentLayoutBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.helpers.TimeAgo
import com.orbis.orbis.models.auth.UserProfile
import com.orbis.orbis.models.posts.CommentModel
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.utils.ViewUtils
import org.ocpsoft.prettytime.PrettyTime
import java.util.*


class CommentsAdapter(
    val context: Context,
    val comments: ArrayList<CommentModel>,
    val interaction: CommentInteraction
) :
    RecyclerView.Adapter<CommentsAdapter.ViewHolder>(), RepliesAdapter.ReplyInteraction {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.each_comment_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]
        val date = ViewUtils.convertTimeStampToDate(comments[position].timestamp)
        val calender = Calendar.getInstance()
        calender.time = date!!
        calender.timeZone = TimeZone.getDefault()
        val p = PrettyTime()
        holder.binding?.dateTimeTv?.text =
            TimeAgo.DateDifference(calender.timeInMillis, context)
                .replace(context.getString(R.string.online), "")
        holder.binding?.commentTv?.text = comment.text
        holder.binding?.userNameTv?.text = comment.user.displayName
        holder.binding?.likesTv?.text = comment.likesCount.toString()
        ViewUtils.loadUserProfilePic(
            context,
            holder.binding?.userIconIv as ImageView,
            comment.user.imageName,
            comment.user.providerImageUrl
        )
        if (comment.selectedForReply) {
            holder.binding?.cardId?.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.micBackground
                )
            )
        } else {
            holder.binding?.cardId?.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.white
                )
            )
        }
        if (comment.replies.isNotEmpty()) {
            holder.binding?.repliesRv?.layoutManager = LinearLayoutManager(context)
            holder.binding?.repliesRv?.adapter =
                RepliesAdapter(context, comment.replies, position, this)
            holder.binding?.repliesRv?.visibility = View.VISIBLE
            holder.binding?.replyBarCl?.visibility = View.VISIBLE
        } else {
            holder.binding?.repliesRv?.visibility = View.GONE
            holder.binding?.replyBarCl?.visibility = View.GONE
        }
        if (comment.userLiked) {
            holder.binding?.likesIv?.setOnClickListener {
                comment.userLiked = false
                comment.likesCount--
                notifyItemChanged(position)
                interaction.onUnLike(position, comment.commentKey)
                //interaction.onUnlikeClicked(position, post)
            }
            holder.binding?.likesIv?.setColorFilter(
                ContextCompat.getColor(
                    context,
                    R.color.black
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        } else {
            holder.binding?.likesIv?.setOnClickListener {
                comment.userLiked = true
                comment.likesCount++
                notifyItemChanged(position)
                interaction.onLike(position, comment.commentKey)
                // interaction.onLikeClicked(position, post)
            }
            holder.binding?.likesIv?.clearColorFilter()
        }
        holder.binding?.reloadIv?.setOnClickListener {
            interaction.onReply(position)
        }

        holder.binding?.userIconIv?.setOnClickListener {
            val intent = Intent(context, ProfileActivity::class.java)
            intent.putExtra("userKey", comment.user.userKey)
            intent.putExtra("displayName", comment.user.displayName)
            context.startActivity(intent)
        }
        holder.binding?.cardId?.setOnLongClickListener {
            menuClick(
                holder.binding.cardId,
                comments[position].commentKey,
                comments[position].user,
                position
            )
            true
        }
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
            interaction.onDelete(commentKey)
            comments.removeAt(position)
            notifyItemChanged(position)
        }
        return true
    }

    override fun getItemCount(): Int {
        return comments.size
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachCommentLayoutBinding? = DataBindingUtil.bind(itemView)
    }

    interface CommentInteraction {
        fun onLike(position: Int, commentKey: String)
        fun onUnLike(position: Int, commentKey: String)
        fun onReply(position: Int)
        fun onDelete(commentKey: String)
    }

    override fun onLike(commentKey: String, replyPosition: Int, parentPos: Int) {
        interaction.onLike(parentPos, commentKey)
    }

    override fun onUnLike(commentKey: String, replyPosition: Int, parentPos: Int) {
        interaction.onUnLike(parentPos, commentKey)
    }

    override fun onReply(parentPos: Int) {
        interaction.onReply(parentPos)
    }

    override fun onDelete(commentKey: String, replyPosition: Int, parentPos: Int) {
        comments[parentPos].replies.removeAt(replyPosition)
        notifyItemChanged(parentPos)
        interaction.onDelete(commentKey)
    }

}
