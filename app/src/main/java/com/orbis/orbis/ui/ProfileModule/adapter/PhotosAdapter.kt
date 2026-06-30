package com.orbis.orbis.ui.ProfileModule.adapter

import android.app.Activity
import android.content.Context
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.databinding.EachPhotoItemBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.profile.UserPictures
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.utils.Utils
import com.stfalcon.imageviewer.StfalconImageViewer


class PhotosAdapter(
    val context: Context,
    val userPictures: ArrayList<UserPictures>,
    val interaction: UserInteraction,
    var isFromMy: Boolean = false,
    var activity: Activity
) :
    RecyclerView.Adapter<PhotosAdapter.ViewHolder>() {


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: EachPhotoItemBinding? = DataBindingUtil.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.each_photo_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (userPictures.get(position).type == "INSTAGRAM") {
            GlideApp.with(context).load(userPictures[position].pictureUrl[0])
                .into(holder.binding?.photoItemIv!!)

            holder.binding.photoItemIv.setOnClickListener { v ->
                val images = ArrayList<String>()
                images.add(userPictures[position].pictureUrl[0])
                val viewer = StfalconImageViewer.Builder(context, images) { imageView, s ->
                    GlideApp.with(context).load(s).into(imageView)
                }.withStartPosition(0).allowZooming(true).allowSwipeToDismiss(true)
                viewer.show()
            }
        } else {
            val storage =
                Firebase.storage.getReference(
                    Constants.USER_PICTURES + Utils.getImageUrl400(
                        userPictures.get(position).pictureUrl[0]
                    )
                )
            GlideApp.with(context).load(storage).into(holder.binding?.photoItemIv!!)
            holder.binding.photoItemIv.setOnClickListener { v ->
                val images = ArrayList<StorageReference>()
                images.add(storage)
                val viewer =
                    StfalconImageViewer.Builder(context, images) { imageView, s ->
                        GlideApp.with(context).load(s).into(imageView)
                    }.withStartPosition(0).allowZooming(true).allowSwipeToDismiss(true)
                viewer.show()
            }


        }
        if (isFromMy) {
            holder.binding?.photoItemIv?.setOnLongClickListener {
                holdMenu(it, position)
                true
            }
        }
    }

    private fun holdMenu(user_menu_iv: View, position: Int) {
        // When user click on the Button 1, create a PopupMenu.
        // And anchor Popup to the Button 2.
        val wrapper: ContextThemeWrapper = ContextThemeWrapper(context, R.style.PopupMenu)
        val popup = PopupMenu(wrapper, user_menu_iv, Gravity.START)

        val menu: Menu = popup.menu
        menu.add(context.getString(R.string.remove_photo))
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
        popup.setOnMenuItemClickListener { item -> menuMainItemClicked(item, position) }
        // Show the PopupMenu.
        popup.show()
    }

    private fun menuMainItemClicked(item: MenuItem, position: Int): Boolean {
        val title = item.title.toString()
        if (title == context.getString(R.string.remove_photo)) {
            interaction.removePhoto(position)
        }

        return true
    }

    override fun getItemCount(): Int {
        return userPictures.size
    }

    interface UserInteraction {
        fun removePhoto(position: Int)
    }

}