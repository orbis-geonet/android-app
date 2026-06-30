package com.orbis.orbis.ui.groupsModule.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ItemConfimedUserEventBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.user.User
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.squareup.picasso.Picasso

class AttendedUsersAdapter(private val users: ArrayList<User>, private val context: Context) :
    RecyclerView.Adapter<AttendedUsersAdapter.ViewHolder>() {


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ItemConfimedUserEventBinding? = DataBindingUtil.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_confimed_user_event, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        ViewUtils.loadUserProfilePic(
            context,
            holder.binding?.shareIconIv6!! as ImageView,
            users[position].imageName,
            users[position].providerImageUrl
        )
        if (position > 0) {
            val param = holder.binding?.shareIconIv6?.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(-24, 0, 0, 0)
            holder.binding.shareIconIv6.layoutParams = param
        }
        holder.binding?.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return users.size
    }
}