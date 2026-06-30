package com.orbis.orbis.ui.ProfileModule.adapter

import android.content.Context
import android.content.Intent
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
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ItemUserBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.ui.messageModule.views.ChatActivity
import com.orbis.orbis.utils.ViewUtils


class FollowersFollowingAdapter(
    private val context: Context,
    private val users: ArrayList<UserInfo>,
    private val interaction: UserInteraction? = null,
    private val isMyList: Boolean = false,
    private val showMessage: Boolean = false
) :
    RecyclerView.Adapter<FollowersFollowingAdapter.ViewHolder>() {


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ItemUserBinding? = DataBindingUtil.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_user, parent, false))
    }

    private fun menuMainClick(user_menu_iv: View, position: Int) {
        // When user click on the Button 1, create a PopupMenu.
        // And anchor Popup to the Button 2.
        val wrapper: ContextThemeWrapper = ContextThemeWrapper(context, R.style.PopupMenu)
        val popup = PopupMenu(wrapper, user_menu_iv, Gravity.START)

        val menu: Menu = popup.menu
        menu.add(context.getString(R.string.remove))
        menu.add(context.getString(R.string.block))
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
        if (title == context.getString(R.string.block)) {
            interaction?.onBlock(position)
        } else if (title == context.getString(R.string.remove)) {
            interaction?.removeFollower(position)
        }

        return true
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(users[position] == null) { return }
        holder.binding?.data = users[position]

        if (isMyList) {
            holder.binding?.item?.setOnLongClickListener {
                menuMainClick(it, position)
                true
            }
        }
        if (showMessage) {
            holder.binding?.messageUser?.visibility = View.VISIBLE
            holder.binding?.messageUser?.setOnClickListener {
                if (users[position].userKey != PrefManager(context).getUserKey()!!) {
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("user", users[position])
                    context.startActivity(intent)
                }
            }
        } else {
            holder.binding?.messageUser?.visibility = View.GONE
        }
        holder.binding?.item?.setOnClickListener {
            val intent = Intent(context, ProfileActivity::class.java)
            intent.putExtra(
                "displayName",
                users[position].displayName
            )
            intent.putExtra("showMyProfile", false)
            intent.putExtra("userKey", users[position].userKey)
            context.startActivity(intent)
        }
        ViewUtils.loadUserProfilePic(
            context,
            holder.binding?.shadowCircle2 as ImageView,
            users[position].imageName,
            users[position].providerImageUrl
        )

        holder.binding?.executePendingBindings()
    }

    interface UserInteraction {
        fun onBlock(position: Int)
        fun removeFollower(position: Int)
    }

    override fun getItemCount(): Int {
        return users.size
    }


}