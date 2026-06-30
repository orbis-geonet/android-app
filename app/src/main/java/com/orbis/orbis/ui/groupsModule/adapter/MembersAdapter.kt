package com.orbis.orbis.ui.groupsModule.adapter

import android.content.Context
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.view.*
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.orbis.orbis.R
import com.orbis.orbis.databinding.ItemMemberBinding
import com.orbis.orbis.models.BigDataSharConstants
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.user.User
import com.orbis.orbis.utils.ViewUtils


class MembersAdapter(
    private val context: Context,
    private val users: ArrayList<User>,
    private val admins: ArrayList<User>,
    private val amIAdmin: Boolean,
    private val interaction: MemberInteraction
) :
    RecyclerView.Adapter<MembersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ItemMemberBinding? = DataBindingUtil.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_member, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding?.data = users[position]

        users[position].codes?.let{ codes ->
            holder.binding?.showCodesButton?.isVisible = codes.count() != 1
            holder.binding?.codeTextView?.isVisible = codes.count() == 1

            if(codes.count() == 1)
            {
                holder.binding?.codeTextView?.text = context.resources.getString(R.string.code, codes.first())
            }
            else
            {
                holder.binding?.showCodesButton?.setOnClickListener {
                    interaction.onCodeListClick(codes)
                }
            }
        }

//        //TODO testing only
//        if(position == 0)
//        {
//            codes.add(codes.first())
//        }

        if(BigDataSharConstants.isOneTimePurchase)
        {
            holder.binding?.textView6?.text =  users[position].user.displayName
        }
        else
        {
            holder.binding?.textView6?.text =  users[position].displayName
        }

        var isAdmin = false
        for (admin in admins) {
            if (admin.userKey == users[position].userKey) {
                isAdmin = true
                break
            }
        }
        ViewUtils.loadUserProfilePic(
            context,
            holder.binding?.shadowCircle2 as ImageView,
            users[position].imageName,
            users[position].providerImageUrl
        )
        if (isAdmin) {
            holder.binding.imageView5.visibility = View.VISIBLE
        } else {
            holder.binding.imageView5.visibility = View.GONE
        }
        holder.binding.item.setOnClickListener {
            interaction.onClick(position)
        }
        if (amIAdmin) {
            holder.binding.item.setOnLongClickListener {
                menuMainClick(it, isAdmin, position)
                true
            }
        }

        holder.binding.executePendingBindings()
    }

    private fun menuMainClick(user_menu_iv: View, isAdmin: Boolean, position: Int) {
        // When user click on the Button 1, create a PopupMenu.
        // And anchor Popup to the Button 2.
        val wrapper: ContextThemeWrapper = ContextThemeWrapper(context, R.style.PopupMenu)
        val popup = PopupMenu(wrapper, user_menu_iv, Gravity.START)

        val menu: Menu = popup.menu
        if (!isAdmin) {
            menu.add("Make Admin")
            menu.add("Ban & Block")
        } else if (admins.size > 1) {
            menu.add("Remove from Admin")
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
        popup.setOnMenuItemClickListener { item -> menuMainItemClicked(item, position) }
        // Show the PopupMenu.
        popup.show()
    }

    private fun menuMainItemClicked(item: MenuItem, position: Int): Boolean {
        val title = item.title.toString()
        if (title == "Make Admin") {
            interaction.addAdmin(position)
        } else if (title == "Ban & Block") {
            interaction.onBlock(position)
        } else if (title == "Remove from Admin") {
            interaction.adminRemoveAdmin(position)
        }

        return true
    }
    override fun getItemCount(): Int {
        return users.size
    }

    interface MemberInteraction {
        fun onBlock(position: Int)
        fun addAdmin(position: Int)
        fun adminRemoveAdmin(position: Int)
        fun onClick(position: Int)
        fun onCodeListClick(codeList: ArrayList<String>)
    }

}