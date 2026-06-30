package com.orbis.orbis.ui.placesModule.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.PagerAdapter
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.ui.ProfileModule.views.ProfileActivity
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.commentModule.views.CommentDialogFragment
import com.orbis.orbis.ui.groupsModule.views.GroupDetailsActivity
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*


internal class PostPagerAdapter(
    private val rootPos: Int,
    private val context: Context,
    private val check_in_list: ArrayList<FeedPost>,
    private val interaction: PlacePostsAdapter.PostCardInteraction
) : PagerAdapter() {


    // Layout Inflater
    var mLayoutInflater: LayoutInflater
    override fun getCount(): Int {
        Log.d("adapterItemCound", check_in_list.size.toString())
        // return the number of images
        return if (check_in_list.size <= 10) {
            check_in_list.size
        } else {
            10
        }
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as ConstraintLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        // inflating the item.xml
        val itemView: View = mLayoutInflater.inflate(R.layout.check_in_item_place, container, false)
        Log.d("sliderSizeAdapter", check_in_list.size.toString())
        // referencing the image view from the item.xml file
        val check_in_place_tv: TextView = itemView.findViewById(R.id.check_in_place_tv) as TextView
        val user_name_tv: TextView = itemView.findViewById(R.id.user_name_tv) as TextView
        val date_tv: TextView = itemView.findViewById(R.id.date_tv) as TextView
//        val location_name_tv: TextView = itemView.findViewById(R.id.location_name_tv) as TextView
        val check_in_name_tv: TextView = itemView.findViewById(R.id.check_in_name_tv) as TextView
        val likes_tv: TextView = itemView.findViewById(R.id.likes_tv) as TextView
        val comments_tv: TextView = itemView.findViewById(R.id.comments_tv) as TextView
        val comments_iv: ImageView = itemView.findViewById(R.id.comments_iv) as ImageView
        check_in_name_tv.text = check_in_list[position].user?.displayName
//        val location_pin_iv: ImageView = itemView.findViewById(R.id.location_pin_iv) as ImageView
        val chekc_in_user_icon_iv: ImageView =
            itemView.findViewById(R.id.chekc_in_user_icon_iv) as ImageView
        val likes_iv: ImageView = itemView.findViewById(R.id.likes_iv) as ImageView
        val user_menu_iv3: ImageView = itemView.findViewById(R.id.user_menu_iv) as ImageView

        user_name_tv.text = check_in_list[position].group?.name
        user_name_tv.post(Runnable {
            val lineCount: Int = user_name_tv.lineCount
            if (lineCount > 1) {
                user_name_tv.textSize = 12f
            }
            // Use lineCount here
        })
        date_tv.text = ViewUtils.convertTimeStampToFormatted(check_in_list[position].timestamp)
        user_menu_iv3.setOnClickListener {
            menuClick(user_menu_iv3, check_in_list[position], position)
        }

        chekc_in_user_icon_iv.setOnClickListener {
            if (check_in_list[position].user != null) {
                val intent = Intent(context, ProfileActivity::class.java)
                intent.putExtra("showMyProfile", false)
                intent.putExtra("displayName", check_in_list[position].user?.displayName)
                intent.putExtra("userKey", check_in_list[position].user?.userKey)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "User Deleted", Toast.LENGTH_SHORT).show()
            }
        }

        if (check_in_list[position].user != null) {
            ViewUtils.loadUserProfilePic(
                context,
                chekc_in_user_icon_iv,
                check_in_list[position].user?.imageName,
                check_in_list[position].user?.providerImageUrl
            )
        } else {
            chekc_in_user_icon_iv.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_user
                )
            )
        }

        likes_tv.text = check_in_list[position].likesCount.toString()
        comments_tv.text = check_in_list[position].commentsCount.toString()
        check_in_place_tv.text = check_in_list[position].place!!.name
        val user_shadow_circle: CircleImageView =
            itemView.findViewById(R.id.user_shadow_circle) as CircleImageView


        if (check_in_list[position].group != null) {
            user_shadow_circle.borderColor =
                Color.parseColor(check_in_list[position].group?.strokeColorHex)
            user_shadow_circle.borderWidth = 3
            ViewUtils.loadGroupPhoto(
                context,
                user_shadow_circle as ImageView,
                check_in_list[position].group?.imageName
            )
            user_shadow_circle.setOnClickListener {
                val intent = Intent(context, GroupDetailsActivity::class.java)
                intent.putExtra("data", check_in_list[position].group)
                intent.putExtra("location", Constants.location)
                context.startActivity(intent)
            }
            user_name_tv.setOnClickListener {
                val intent = Intent(context, GroupDetailsActivity::class.java)
                intent.putExtra("data", check_in_list[position].group)
                intent.putExtra("location", Constants.location)
                context.startActivity(intent)
            }
        }
        // Log.d("typePlace",check_in_list[position].place.type)
        // setting the image in the imageView
//        imageView.setImageResource(images[position])

        comments_iv.setOnClickListener {
            interaction.onCommentClicked(position, check_in_list[position])
        }

        checkLikeState(position, likes_iv, likes_tv)

        // Adding the View
        container.addView(itemView)
        return itemView
    }

    private fun checkLikeState(position: Int, likesIv: ImageView, likesTv: TextView) {
        if (check_in_list[position].userLiked) {
            likesIv.setOnClickListener {
                if (PrefManager(context).getIdToken().isNullOrEmpty()) { redirectToLogin(); return@setOnClickListener }
                check_in_list[position].userLiked = false
                check_in_list[position].likesCount--
                likesTv.text = check_in_list[position].likesCount.toString()
                interaction.onUnlikeClicked(position, check_in_list[position])
                checkLikeState(position, likesIv, likesTv)
            }
            likesIv.setColorFilter(
                ContextCompat.getColor(context, R.color.black),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        } else {
            likesIv.setOnClickListener {
                if (PrefManager(context).getIdToken().isNullOrEmpty()) { redirectToLogin(); return@setOnClickListener }
                check_in_list[position].userLiked = true
                check_in_list[position].likesCount++
                likesTv.text = check_in_list[position].likesCount.toString()
                interaction.onLikeClicked(position, check_in_list[position])
                checkLikeState(position, likesIv, likesTv)
            }
            likesIv.clearColorFilter()
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as ConstraintLayout)
    }
    private fun redirectToLogin() {
        context.startActivity(
            Intent(context, AuthActivity::class.java).putExtra("goToLogin", true)
        )
    }

    private fun menuClick(user_menu_iv: ImageView, post: FeedPost, position: Int) {
        // When user click on the Button 1, create a PopupMenu.
        // And anchor Popup to the Button 2.
        val wrapper: ContextThemeWrapper = ContextThemeWrapper(context, R.style.PopupMenu)
        val popup = PopupMenu(wrapper, user_menu_iv, Gravity.START)
        popup.inflate(R.menu.layout_post_menu)

        val menu: Menu = popup.getMenu()
        val isLoggedIn = !PrefManager(context).getIdToken().isNullOrEmpty()
        menu.findItem(R.id.menuItem_share)?.isVisible  = isLoggedIn
        menu.findItem(R.id.menuItem_report)?.isVisible = isLoggedIn
        if (post.user != null && post.user?.userKey == PrefManager(context).getUserKey()) {
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
        popup.setOnMenuItemClickListener { item -> menuItemClicked(item, post, position) }
        // Show the PopupMenu.
        popup.show()
    }

    // When user click on Menu Item.
    // @return true if event was handled.
    private fun menuItemClicked(item: MenuItem, post: FeedPost, position: Int): Boolean {
        val title = item.title.toString()
        if (title == "Share") {
            interaction.onShare(post.postKey)
        } else if (title == "Delete") {
            interaction.onPostDelete(position, post.postKey, rootPos)
        } else if (title == "Report") {
            if (PrefManager(context).getIdToken().isNullOrEmpty()) { redirectToLogin(); return true}
            interaction.onReport(post.postKey)
        }
        return true
    }


    // Viewpager Constructor
    init {
        mLayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }
}