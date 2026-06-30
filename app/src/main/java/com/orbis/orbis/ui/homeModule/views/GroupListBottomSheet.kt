package com.orbis.orbis.ui.homeModule.views

import android.animation.ValueAnimator
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.orbis.orbis.R
import com.orbis.orbis.databinding.DialogGroupListBinding
import com.orbis.orbis.extensions.toDp
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.ui.authModule.views.AuthActivity
import com.orbis.orbis.ui.groupsModule.adapter.GroupListAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.groupsModule.views.CreateGroupActivity
import com.orbis.orbis.ui.groupsModule.views.GroupDetailsActivity
import com.orbis.orbis.ui.newsFeedModule.views.NewsFeedActivity
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupListBottomSheet : Fragment(), GroupListAdapter.GroupsCardInteraction {

    private var onExpandedCallback: (() -> Unit)? = null
    private var onCollapsedCallback: (() -> Unit)? = null
    private var onHiddenCallback: (() -> Unit)? = null
    private var onProcessCallback: (() -> Unit)? = null
    var fabToAnimate: View? = null

    companion object {
        fun newInstance(
            location: Location?,
            userKey: String?,
            city: String?,
            onExpanded: (() -> Unit)? = null,
            onCollapsed: (() -> Unit)? = null,
            onHidden: (() -> Unit)? = null,
            onProcess: (() -> Unit)? = null
        ): GroupListBottomSheet {
            val args = Bundle().apply {
                putParcelable("location", location)
                putString("userKey", userKey)
                putString("city", city)
            }

            return GroupListBottomSheet().apply {
                arguments = args
                onExpandedCallback = onExpanded
                onCollapsedCallback = onCollapsed
                onProcessCallback = onProcess
                onHiddenCallback = onHidden
            }
        }
    }

    private lateinit var binding: DialogGroupListBinding
    private lateinit var viewModel: GroupViewModel
    private lateinit var mAdapter: GroupListAdapter

    private var behavior: BottomSheetBehavior<View>? = null


    private val groups = ArrayList<GroupDetails>()

    private var location: Location? = null
    private var userKey: String? = null
    private var city: String? = null

    private var showingRated = true
    private var query = ""
    private var searchPage = 0
    private var prevSearchPage = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        location = arguments?.getParcelable("location")
        userKey  = arguments?.getString("userKey")
        city     = arguments?.getString("city")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_group_list, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        mAdapter  = GroupListAdapter(groups, this, requireContext())

        binding.groupsRv.layoutManager = LinearLayoutManager(requireContext())
        binding.groupsRv.adapter = mAdapter

        setupSearch()
        setupScroll()
        setupObservers()

        hide()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) {
            if (behavior?.state == BottomSheetBehavior.STATE_EXPANDED || behavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                hide()
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionsContainer) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = navBar.bottom //+ resources.getDimensionPixelSize(R.dimen.margin_bottom_24dp)
            v.layoutParams = params
            insets
        }
    }

    override fun onStart() {
        super.onStart()

        val sheetContainer = requireActivity().findViewById<FrameLayout>(R.id.bottom_sheet_container)
        val topOffset = 24.toDp()

        behavior = BottomSheetBehavior.from(sheetContainer)

        val screenHeight = resources.displayMetrics.heightPixels

        ViewCompat.setOnApplyWindowInsetsListener(sheetContainer) { _, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight = navBarInsets.bottom

            val isButtonNavigation = insets.isVisible(WindowInsetsCompat.Type.navigationBars()) &&
                    insets.getInsets(WindowInsetsCompat.Type.tappableElement()).bottom > 0

            behavior?.peekHeight = (screenHeight * 0.36).toInt() + navBarHeight
            behavior?.maxHeight = if (isButtonNavigation) {
                screenHeight + statusBarHeight + navBarHeight
            } else {
                screenHeight //- statusBarHeight
            }

            WindowInsetsCompat.CONSUMED
        }

        behavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {

                        // Keep a small gap from the status/top bar
                        bottomSheet.translationY = topOffset.toFloat()

                        binding.addFab.visibility = View.VISIBLE
                        binding.newsFeed.visibility = View.VISIBLE
                        binding.logo.visibility = View.VISIBLE
                        binding.searchLayout.visibility = View.VISIBLE

                        onExpandedCallback?.invoke()
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {

                        // Reset translation
                        bottomSheet.translationY = 0f

                        binding.searchLayout.visibility = View.GONE
                        fabToAnimate?.let { animateFabBottomMargin(it, 240) }

                        onCollapsedCallback?.invoke()
                    }

                    BottomSheetBehavior.STATE_HIDDEN -> {

                        bottomSheet.translationY = 0f

                        binding.searchLayout.visibility = View.GONE
                        fabToAnimate?.let { animateFabBottomMargin(it, 10) }

                        onHiddenCallback?.invoke()
                    }

                    BottomSheetBehavior.STATE_DRAGGING -> {
                        binding.addFab.visibility = View.GONE
                        binding.newsFeed.visibility = View.GONE
                        binding.logo.visibility = View.GONE

                        onProcessCallback?.invoke()
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                fabToAnimate?.visibility = View.GONE
            }
        })
    }

    fun updateLocation(newLocation: Location, newCity: String) {
        location = newLocation
        city = newCity

        // Refresh groups with the new location
        groups.clear()
        mAdapter.notifyDataSetChanged()
        searchPage = 0
        prevSearchPage = -1

        viewModel.getRatedGroups(location!!.latitude, location!!.longitude)
    }

    fun show() {
        behavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }
    fun expand() {
        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }
    fun hide()  {
        behavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }
    fun toggle() {
        behavior?.state = if (behavior?.state == BottomSheetBehavior.STATE_HIDDEN) {
            BottomSheetBehavior.STATE_COLLAPSED
        } else {
            BottomSheetBehavior.STATE_HIDDEN
        }
    }
    private fun animateFabBottomMargin(view: View, targetDp: Int, duration: Long = 50) {
        val targetPx = (targetDp * resources.displayMetrics.density).toInt()
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val startPx = params.bottomMargin

        ValueAnimator.ofInt(startPx, targetPx).apply {
            this.duration = duration
            addUpdateListener {
                params.bottomMargin = it.animatedValue as Int
                view.layoutParams = params
            }
            start()
        }
    }

    private fun setupSearch() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    showingRated = true
                    groups.clear()
                    searchPage = -1
                    query = ""
                    mAdapter.notifyDataSetChanged()

                    if (location != null && userKey.isNullOrEmpty()) {
                        viewModel.getRatedGroups(location!!.latitude, location!!.longitude)
                    }
                    hideKeyboard(requireActivity())
                }
                return false
            }

            override fun onQueryTextSubmit(q: String): Boolean {
                showingRated = false
                query = q
                searchPage = 0
                groups.clear()
                mAdapter.notifyDataSetChanged()

                if (userKey.isNullOrEmpty()) {
                    viewModel.getGroups(location!!.latitude, location!!.longitude, query)
                }
                hideKeyboard(requireActivity())
                return false
            }
        })
    }

    private fun setupScroll() {
        binding.groupsRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lm          = recyclerView.layoutManager as LinearLayoutManager
                val total       = lm.itemCount
                val lastVisible = lm.findLastVisibleItemPosition()

                if (lastVisible >= total - 3 && userKey.isNullOrEmpty()) {
                    if (searchPage != prevSearchPage) {
                        viewModel.getGroups(location!!.latitude, location!!.longitude, query, searchPage)
                        prevSearchPage = searchPage
                    }
                }
            }
        })
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.loading = it }

//        viewModel.cachedGroups.observe(viewLifecycleOwner) {
//            if (it.isEmpty()) {
//                if (location != null && userKey.isNullOrEmpty()) {
//                    viewModel.getRatedGroups(location!!.latitude, location!!.longitude)
//                }
//            } else {
//                groups.clear()
//                groups.addAll(it)
//                mAdapter.notifyDataSetChanged()
//                searchPage    = it.size / 25
//                prevSearchPage = -1
//            }
//        }

        viewModel.ratedGroups.observe(viewLifecycleOwner) {
            val index = groups.size
            for (group in it) {
                if (groups.none { g -> g.groupKey == group.groupKey }) groups.add(group)
            }
            mAdapter.notifyItemRangeInserted(index, groups.size)
        }

        viewModel.groupList.observe(viewLifecycleOwner) { items ->
            if (!items.isNullOrEmpty()) {
                searchPage++
                for (group in items) {
                    if (groups.none { g -> g.groupKey == group.groupKey }) groups.add(group)
                }
                mAdapter.notifyDataSetChanged()
            }
        }

        binding.addFab.setOnClickListener {
            if (PrefManager(requireContext()).getIdToken().isNullOrEmpty()) {
                startActivity(Intent(requireContext(), AuthActivity::class.java))
            } else {
                val intent = Intent(requireContext(), CreateGroupActivity::class.java)
                intent.putExtra("location", location)
                startActivity(intent)
            }
        }

        binding.logo.setOnClickListener {
            hide()
        }

        binding.newsFeed.setOnClickListener {
            val intent = Intent(requireContext(), NewsFeedActivity::class.java)
            intent.putExtra("location", location)
            intent.putExtra("city", city)
            startActivity(intent)
        }
    }

    override fun onItemClicked(position: Int) {
        val selected = groups[position]
        startActivity(
            android.content.Intent(requireContext(), GroupDetailsActivity::class.java).apply {
                putExtra("data", selected)
                putExtra("location", location)
            }
        )
        hide()
    }

    fun getState(): Int? = behavior?.state
}