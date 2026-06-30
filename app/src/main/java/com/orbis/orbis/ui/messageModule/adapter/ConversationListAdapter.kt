package com.orbis.orbis.ui.messageModule.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.databinding.EachMessagesRowLayoutBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.message.ConversationModel
import com.orbis.orbis.ui.messageModule.views.ChatActivity
import com.orbis.orbis.utils.ViewUtils
import com.squareup.picasso.Picasso

class ConversationListAdapter(val context: Context, val messages: ArrayList<ConversationModel>) :
    RecyclerView.Adapter<ConversationListAdapter.ViewHolder>() {


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachMessagesRowLayoutBinding? = DataBindingUtil.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.each_messages_row_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding?.data = messages[position]
        ViewUtils.loadUserProfilePic(
            context,
            holder.binding?.userIconIv!! as ImageView,
            messages[position].sender?.imageName,
            messages[position].sender?.providerImageUrl
        )
        if (!messages[position].lastMessage?.isRead!! && messages[position].lastMessage?.senderId!! != PrefManager(
                context
            ).getUserKey()!!
        ) {
            holder.binding?.haveNewTv?.visibility = View.VISIBLE
        } else {
            holder.binding?.haveNewTv?.visibility = View.GONE
        }
        holder.binding?.cardId?.setOnClickListener {
            messages[position].lastMessage?.isRead = true
            notifyItemChanged(position)
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("data", messages[position])
            context.startActivity(intent)
        }
        if (messages[position].lastMessage?.type == "TEXT") {
            holder.binding?.messageTv?.text = messages[position].lastMessage?.message
        } else if (messages[position].lastMessage?.type == "IMAGE") {
            holder.binding?.messageTv?.text = "Sent an image"
        } else if (messages[position].lastMessage?.type == "VIDEO") {
            holder.binding?.messageTv?.text = "Sent an video"
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }
}