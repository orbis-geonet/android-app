package com.orbis.orbis.ui.homeModule.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ItemPendingFollowerBinding
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.utils.ViewUtils
import com.squareup.picasso.Picasso

class FollowerRequestAdapter(
    private val requests: ArrayList<UserInfo>,
    private val context: Context,
    private val listener: FollowerInteraction
) :
    RecyclerView.Adapter<FollowerRequestAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ItemPendingFollowerBinding? = DataBindingUtil.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_pending_follower, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding?.item = requests[position]
        ViewUtils.loadUserProfilePic(
            context,
            holder.binding?.userPic as ImageView,
            requests[position].imageName,
            requests[position].providerImageUrl
        )
        holder.binding.accept.setOnClickListener {
            listener.acceptRequest(position)
        }
    }

    override fun getItemCount(): Int {
        return requests.size
    }

    interface FollowerInteraction {
        fun acceptRequest(position: Int)
    }
}