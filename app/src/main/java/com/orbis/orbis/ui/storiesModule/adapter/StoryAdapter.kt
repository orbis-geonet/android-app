package com.orbis.orbis.ui.storiesModule.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.EachStatusLayoutBinding
import com.orbis.orbis.models.BigDataSharConstants
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.story.StoryModel

import com.orbis.orbis.ui.storiesModule.views.StoryActivity
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.squareup.picasso.Picasso

class StoryAdapter(val context: Context, val stories: ArrayList<StoryModel>) :
    RecyclerView.Adapter<StoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.each_status_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding?.circularStatusView?.setPortionsColor(Color.parseColor(stories[position].group.strokeColorHex))
        var unseenCount = 0
        for (post in stories[position].posts) {
            if (!post.seen) {
                unseenCount++
            }
        }
        if (unseenCount == 0) {
            unseenCount = 1
        }
        holder.binding?.circularStatusView?.setPortionsCount(unseenCount)


        holder.binding?.cardId?.setOnClickListener {
            val intent = Intent(context, StoryActivity::class.java)
            Log.d("storyClick", stories.size.toString())

            BigDataSharConstants.storiesArray.clear()
            BigDataSharConstants.storiesArray.addAll(stories)
//            intent.putExtra("data", stories)

            intent.putExtra("currentPage", position)
            context.startActivity(intent)
        }
        ViewUtils.loadGroupPhoto(
            context,
            holder.binding?.storyThumb as ImageView,
            stories[position].group.imageName
        )
    }

    override fun getItemCount(): Int {
        return stories.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachStatusLayoutBinding? = DataBindingUtil.bind(itemView)
    }
}