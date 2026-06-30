package com.orbis.orbis.ui.homeModule.manager

import android.content.Context
import android.content.Intent
import android.location.Location
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentMapBinding
import com.orbis.orbis.helpers.PlaceIcon
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.place.PolygonPlaceDetails
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.authModule.adapter.PlacesPagerAdapter
import com.orbis.orbis.ui.groupsModule.views.GroupDetailsActivity
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.utils.Utils
import kotlin.math.abs

class MapPlacesPagerController(
    private val context: Context,
    private val binding: FragmentMapBinding,
    private val placeViewModel: PlaceViewModel,
    private val mLastLocation: () -> Location?,
    private val onBackPressed: () -> Unit,
) {
    var canReAdjustViewPagerHeight = false
    var lastSlidedPosition = 0

    fun setup() {
        setCardGroupPosition()
        setupPagerTransformer()
        setupPagerPadding()
        setupPageChangeCallback()
    }

    private fun setupPagerTransformer() {
        binding.mapPlacesPager.mapPlacesPager2.setPageTransformer { page, position ->
            page.translationX = -70 * position
            page.scaleY = 0.85f + (1 - abs(position)) * 0.15f
        }
        binding.mapPlacesPager.mapPlacesPager2.offscreenPageLimit = 1
    }

    private fun setupPagerPadding() {
        val recyclerView = binding.mapPlacesPager.mapPlacesPager2.getChildAt(0) as RecyclerView
        recyclerView.clipToPadding = false
        recyclerView.clipChildren = false
        recyclerView.setPadding(40, 0, 40, 0)
        recyclerView.setHasFixedSize(true)
    }

    private fun setupPageChangeCallback() {
        binding.mapPlacesPager.mapPlacesPager2.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                placeViewModel.changeFocusedPlace(position)
                lastSlidedPosition = position
                adjustViewPagerHeightAt(position)
                //onPageSelected(position)
                super.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                canReAdjustViewPagerHeight = state == ViewPager2.SCROLL_STATE_IDLE
            }
        })
    }

    fun loadPlaces(places: ArrayList<PolygonPlaceDetails>, changed: Boolean) {
        if (changed) {
            binding.mapPlacesPager.mapPlacesPager2.adapter?.notifyDataSetChanged()
        } else {
            binding.mapPlacesPager.mapPlacesPager2.adapter =
                PlacesPagerAdapter(context, places)
        }
    }

    fun moveToCurrentFocus() {
        val focusedPlaces = placeViewModel.polygonFocusedPlaces.value.orEmpty()
        val newSelectedIndex = focusedPlaces.indexOfFirst { it.isFocusSelected }.takeIf { it != -1 } ?: 0
        binding.mapPlacesPager.mapPlacesPager2.setCurrentItem(newSelectedIndex, true)
        binding.mapPlacesPager.mapPlacesPager2.adapter?.notifyItemChanged(newSelectedIndex)
    }

    fun adjustViewPagerHeightAt(position: Int) {
        val recyclerView =
            binding.mapPlacesPager.mapPlacesPager2.getChildAt(0) as? RecyclerView ?: return
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
        val view = viewHolder?.itemView

        if (view != null) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(recyclerView.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val measuredHeight = view.measuredHeight
            val layoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams
            val totalHeight =
                measuredHeight + (layoutParams?.topMargin ?: 0) + (layoutParams?.bottomMargin ?: 0)

            if (binding.mapPlacesPager.mapPlacesPager2.layoutParams.height != totalHeight) {
                binding.mapPlacesPager.mapPlacesPager2.layoutParams =
                    (binding.mapPlacesPager.mapPlacesPager2.layoutParams as ConstraintLayout.LayoutParams)
                        .also { lp -> lp.height = totalHeight }
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (canReAdjustViewPagerHeight && view == null)
                adjustViewPagerHeightAt(position)
        }, 450)
    }

    fun showPolygonGroupCards(placeDetails: PolygonPlaceDetails) {
        binding.mapPolygonGroupCards.groupName.text = placeDetails.dominantGroup!!.name
        binding.mapPolygonGroupCards.groupName.minimumWidth = 60
        binding.mapPolygonGroupCards.groupName.filters =
            arrayOf(android.text.InputFilter.LengthFilter(50))

        setCardGroupImage(placeDetails)

        binding.mapPolygonGroupCards.titlePolygonCardview.setOnClickListener {
            val intent = Intent(context, GroupDetailsActivity::class.java)
            intent.putExtra("data", placeDetails.dominantGroup)
            intent.putExtra("location", mLastLocation())
            context.startActivity(intent)
        }

        binding.mapPolygonGroupCards.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.mapPolygonGroupCards.root.visibility = View.VISIBLE
    }

    fun setCardGroupPosition() {
        val screenHeightInPixels = context.resources.displayMetrics.heightPixels
        val topMargin = screenHeightInPixels / 9
        val layoutParams = binding.mapPolygonGroupCards.root.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = topMargin
        binding.mapPolygonGroupCards.root.layoutParams = layoutParams
    }

    fun setCardGroupImage(placeDetails: PolygonPlaceDetails) {
        val storage = Firebase.storage.getReference(
            Constants.GROUP_PHOTO_STORAGE + Utils.getImageUrl200(
                placeDetails.dominantGroup!!.imageName
            )
        )

        GlideApp.with(context)
            .load(storage)
            .circleCrop()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.mapPolygonGroupCards.groupImage.setImageDrawable(
                        ContextCompat.getDrawable(context, PlaceIcon.getIconByType(placeDetails.type))
                    )
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.mapPolygonGroupCards.groupImage.setImageDrawable(resource)
                    return true
                }
            }).submit()

        binding.mapPolygonGroupCards.groupImage.borderColor =
            placeDetails.dominantGroup.strokeColorHex.toColorInt()
    }
}