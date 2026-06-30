package com.orbis.orbis.ui.homeModule.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ItemNotificationNewsBinding
import com.orbis.orbis.helpers.TimeAgo
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.notifications.NotificationModel
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.ui.groupsModule.views.GroupDetailsActivity
import com.orbis.orbis.ui.homeModule.views.NotificationDialogFragment
import com.orbis.orbis.ui.homeModule.views.PostDetailsActivity
import com.orbis.orbis.ui.messageModule.views.MessageActivity
import com.orbis.orbis.ui.placesModule.views.PlaceActivity
import com.orbis.orbis.utils.ViewUtils
import com.squareup.picasso.Picasso
import org.ocpsoft.prettytime.PrettyTime
import java.util.*
import kotlin.collections.ArrayList


class NotificationNewsAdapter(
    private val notifications: ArrayList<NotificationModel>,
    val context: Context,
    val listener: NotificationItemClick
) :
    RecyclerView.Adapter<NotificationNewsAdapter.ViewHolder>() {


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ItemNotificationNewsBinding? = DataBindingUtil.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_notification_news, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding?.data = notifications[position]

        if (notifications[position].group != null) {
            if (notifications[position].group?.imageName.isNullOrEmpty()) {
                ViewUtils.loadGroupPhoto(
                    context,
                    holder.binding?.userPic as ImageView,
                    notifications[position].group?.imageName
                )
            } else if (notifications[position].fromUser != null) {
                ViewUtils.loadUserProfilePic(
                    context,
                    holder.binding?.userPic as ImageView,
                    notifications[position].fromUser?.imageName,
                    notifications[position].fromUser?.providerImageUrl
                )
            }
            Log.d("notificationGroup", notifications[position].group?.strokeColorHex!!)
            holder.binding?.userPic?.borderWidth = 3
            holder.binding?.userPic?.borderColor =
                Color.parseColor(notifications[position].group?.strokeColorHex)
        } else if (notifications[position].fromUser != null) {
            ViewUtils.loadUserProfilePic(
                context,
                holder.binding?.userPic as ImageView,
                notifications[position].fromUser?.imageName,
                notifications[position].fromUser?.providerImageUrl
            )
        }
        val date = ViewUtils.convertTimeStampToDate(notifications[position].timestamp)
        val calender = Calendar.getInstance()
        calender.time = date!!
        calender.timeZone = TimeZone.getDefault()

        holder.binding?.timeAgo?.text = TimeAgo.DateDifference(calender.timeInMillis, context)
            .replace(context.getString(R.string.online), "")
        holder.binding?.item?.setOnClickListener {
            listener.setAsSeen(position)
            val type = notifications[position].type
            if (type == "POST" || type == "COMMENT" || type == "REPORT_POST") {
                listener.notificationClick(position)
            } else if (notifications[position].type == "CHECK_IN" || type == "REPORT_PLACE") {
                listener.onCheckinClick(position)
            } else if (type == "MESSAGE") {
                val messageIntent = Intent(context, MessageActivity::class.java)
                context.startActivity(messageIntent)
            } else if (type == "FOLLOWER" || type == "REPORT_USER") {
                val intent = Intent(context, ProfileActivity::class.java)
                intent.putExtra(
                    "displayName",
                    ""
                )
                intent.putExtra("userKey", notifications[position].fromUser?.userKey)
                context.startActivity(intent)
            } else if (type == "REPORT_GROUP") {
                val intent = Intent(context, GroupDetailsActivity::class.java)
                intent.putExtra("data", notifications[position].group)
                context.startActivity(intent)
            }
        }
        if (!notifications[position].seen) {
            holder.binding?.item?.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.default_trans_dot_color
                )
            )
        } else {
            holder.binding?.item?.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        }
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    interface NotificationItemClick {
        fun notificationClick(position: Int)
        fun onCheckinClick(position: Int)
        fun setAsSeen(position: Int)
    }
}