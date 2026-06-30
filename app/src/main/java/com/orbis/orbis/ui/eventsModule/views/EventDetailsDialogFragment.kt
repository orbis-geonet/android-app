package com.orbis.orbis.ui.eventsModule.views

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentEventDetailsBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.group.FeedContent
import com.orbis.orbis.models.group.FeedPost
import com.orbis.orbis.models.user.User
import com.orbis.orbis.network.ApiInterface
import com.orbis.orbis.network.SwaggerApiClient
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.eventsModule.adapter.EventUsersPhotoAdapter
import com.orbis.orbis.ui.eventsModule.viewModel.Users
import com.orbis.orbis.ui.groupsModule.adapter.AttendedUsersAdapter
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.placesModule.views.PlaceActivity
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

@AndroidEntryPoint
class EventDetailsDialogFragment(val post: FeedPost) : BottomSheetDialogFragment(),
    EventUsersPhotoAdapter.UserPhotoCardInteraction {
    lateinit var binding: FragmentEventDetailsBinding
    lateinit var viewModel: GroupViewModel
    var pendingAttend = -1
    lateinit var userAdaper: AttendedUsersAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_event_details, container, false)
        initView()
        return binding.root
    }

    private fun getAttendees(i: Int) {
        val token = "Bearer " + PrefManager(requireContext()).getIdToken()

        val api = SwaggerApiClient.getClient(requireContext()).create(ApiInterface::class.java)
        api.getAttendees(token, post.postKey, 0, 100)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<ArrayList<User>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(t: ArrayList<User>) {
                    try {
                        post.attendedUsers.clear()
                        post.attendedUsers.addAll(t)
                        binding.totalCount.text = t.size.toString()
                        userAdaper.notifyDataSetChanged()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onError(e: Throwable) {

                }

            })

    }

    private fun initView() {
        viewModel = ViewModelProvider(this).get(GroupViewModel::class.java)
        binding.data = post
        if (!post.mediaUrls.isNullOrEmpty()) {
            val storage =
                Firebase.storage.getReference(
                    Constants.EVENT_STORAGE + Utils.getImageUrl680(
                        post!!.mediaUrls[0]
                    )
                )
            GlideApp.with(requireContext()).load(storage).into(binding?.eventIv!!)
        }
        if (post.place != null) {
            binding.locationTv.setOnClickListener {
                val intent = Intent(context, PlaceActivity::class.java)
                intent.putExtra("data", post.place)
                requireContext().startActivity(intent)
            }
        }
        if (post.address.isNullOrEmpty()) {
            binding.locationTv.visibility = View.GONE
        } else {
            binding.locationTv.visibility = View.VISIBLE
            binding.locationTv.text = post.address
        }


        binding.eventTv.text = post.details
        if (post.details.isNullOrEmpty()) {
            binding.eventTv.visibility = View.GONE
        } else {
            binding.eventTv.visibility = View.VISIBLE
            binding.eventTv.setContent(post?.details)
            binding.eventTv.setTextMaxLength(200)
            binding.eventTv.setSeeMoreTextColor(R.color.view_more_blue)
        }

        binding.confirmedTv.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.VISIBLE
        binding.recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        userAdaper = AttendedUsersAdapter(post.attendedUsers, requireContext())
        binding.recyclerView.adapter = userAdaper

        val eventStart = Calendar.getInstance()
        val eventEnd = Calendar.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val start = Instant.parse(post.plannedTime)
            eventStart.time = Date.from(start)
            val end = Instant.parse(post.plannedEndTime)
            eventEnd.time = Date.from(end)
        } else {
            eventStart.time = ViewUtils.convertTimeStampToDate(post.plannedTime)!!
            eventEnd.time = ViewUtils.convertTimeStampToDate(post.plannedEndTime)!!
        }
        var placeHolder = eventStart.get(Calendar.DAY_OF_MONTH)
            .toString() + " " + SimpleDateFormat("MMMM").format(eventStart.time)
        binding.eventDateTv.text = placeHolder
        placeHolder =
            getString(R.string.from) + eventStart.get(Calendar.HOUR_OF_DAY) + ":" + eventStart.get(
                Calendar.MINUTE
            )
                .toString().padStart(2, '0') + getString(R.string.to) + eventEnd.get(
                Calendar.HOUR_OF_DAY
            ) + ":" + eventEnd.get(Calendar.MINUTE).toString().padStart(2, '0')
        binding.eventTimeTv.text = placeHolder
        if (!post.attending) {
            binding.goBtn.text = getString(R.string.i_go)
            binding.goBtn.backgroundTintList =
                requireContext().getColorStateList(R.color.black_state)
        } else {
            binding.goBtn.text = getString(R.string.i_going)
            binding.goBtn.backgroundTintList =
                requireContext().getColorStateList(R.color.grey_state)
        }

        binding.goBtn.setOnClickListener {
            Log.d("goButtonEvent", post.postKey)
            if (!post.attending) {
                viewModel.attendEvent(post.postKey)
            } else {
                viewModel.unattendEvent(post.postKey)
            }
        }
        viewModel.attendEvent.observe(this) {
            if (it) {
                getAttendees(pendingAttend)
                post.attending = true
                binding.goBtn.text = getString(R.string.i_going)
                binding.goBtn.backgroundTintList =
                    requireContext().getColorStateList(R.color.grey_state)
            }
        }
        viewModel.unattendEvent.observe(this) {
            if (it) {
                getAttendees(pendingAttend)
                post.attending = false
                binding.goBtn.text = getString(R.string.i_go)
                binding.goBtn.backgroundTintList =
                    requireContext().getColorStateList(R.color.black_state)
            }
        }
        binding.totalCount.text = post.attendedUsers.size.toString()
        getAttendees(0)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.toolbar.titleTv.setText(post.title)
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
        hideKeyboard(requireActivity())
    }

//    override val layoutId: Int
//        get() = R.layout.fragment_event_details
//    override val pageTitle: String?
//        get() = ""


    override fun onIconClicked() {
        Toast.makeText(requireContext(), "item clicked", Toast.LENGTH_SHORT).show()
    }


}