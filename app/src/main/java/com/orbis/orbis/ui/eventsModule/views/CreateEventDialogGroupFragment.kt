package com.orbis.orbis.ui.eventsModule.views


import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.GoogleMap
import com.orbis.orbis.BuildConfig
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentCreateEventBinding
import com.orbis.orbis.databinding.FragmentCreateEventGroupBinding
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.listeners.OnPlaceSelect
import com.orbis.orbis.listeners.SearchClick
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.Constants.location
import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.posts.PostBody
import com.orbis.orbis.ui.app.GlideApp
import com.orbis.orbis.ui.groupsModule.adapter.ImageSliderAdapterLocal
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import com.orbis.orbis.ui.placesModule.adapter.CheckinCreatePlaceAdapter
import com.orbis.orbis.ui.placesModule.views.SearchPlaceDialogFragment
import com.orbis.orbis.ui.settingsModule.viewModel.Place
import com.orbis.orbis.utils.PermissionUtil
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.hideKeyboard
import com.orbis.orbis.utils.picker.Picker
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class CreateEventDialogGroupFragment(
    val groupDetails: GroupDetails,
    val onPostCreate: OnPlaceCreate
) : BaseBottomSheetFragment() {

    private val picker = Picker()
    lateinit var viewModel: PlaceViewModel
    lateinit var mAdapter: CheckinCreatePlaceAdapter
    private var arrowOpen = false
    private val myCalendar: Calendar = Calendar.getInstance()
    private val nowCalender: Calendar = Calendar.getInstance()
    private val endCalendar: Calendar = Calendar.getInstance()
    lateinit var binding: FragmentCreateEventGroupBinding
    var mTimePicker: TimePickerDialog? = null
    var currentPhotoPath: String = ""

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        Log.e("tttttttttttt", "createImageFile")
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            Log.e("tttttttt", absolutePath.toString() + " ttttttttttt afasdf")
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    var selectedPic: Uri? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_create_event_group,
            container,
            false
        )
        initView()
        return binding.root
    }

    val groupList: java.util.ArrayList<GroupDetails> = java.util.ArrayList()
    private fun initView()
    {
        picker.populate(fragment = this)

        viewModel = ViewModelProvider(this)[PlaceViewModel::class.java]
        binding.data = PostBody(
            Coordinates(Constants?.location!!.longitude, Constants.location!!.latitude),
            false
        )

        ViewUtils.loadGroupPhoto(requireContext(), binding.imageIv, groupDetails.imageName)
        binding.placeNameTv.text = groupDetails.name
        viewModel.postCreated.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), "Event Created!", Toast.LENGTH_LONG).show()
            onPostCreate.onPostCreate(it)
            dismiss()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        viewModel.error.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
    }

    var isDateSelected: Boolean = false
    var isStartTimeSelected: Boolean = false
    var isEndTimeSelected: Boolean = false


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.placesRv.layoutManager = LinearLayoutManager(requireContext())

        val arrayList = ArrayList<GroupDetails>()

        binding.createEventBtn.setOnClickListener {
            binding.data?.subType = "EVENT"
            validateData()
        }

        binding.mediaBgCl.setOnClickListener {
            onClickedPickPhoto()
        }

        binding.closeIv.setOnClickListener {
            binding.photoCl.visibility = View.GONE
            binding.mediaBgCl.visibility = View.VISIBLE
        }

        binding.toolbar.titleTv.setText(resources.getText(R.string.event))

        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }

        val date = OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            endCalendar.set(Calendar.YEAR, year)
            endCalendar.set(Calendar.MONTH, monthOfYear)
            endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            isDateSelected = true
            updateLabel()
        }

        binding.eventDateEt.setOnClickListener {
            DatePickerDialog(
                requireContext(), date,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val selectedStartHour = TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            myCalendar.set(Calendar.MINUTE, selectedMinute)
            if (binding.eventStartEt.text.isNullOrEmpty() || binding.eventEndEt.text.isNullOrEmpty()
                || compareTime(binding.eventStartEt.text.toString(), binding.eventEndEt.text.toString())
            ) {
                binding.eventStartEt.setText("$selectedHour:${selectedMinute.toString().padStart(2, '0')}")
            }
            isStartTimeSelected = true
        }

        binding.eventStartEt.setOnClickListener {
            val hour = myCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = myCalendar.get(Calendar.MINUTE)
            mTimePicker = TimePickerDialog(requireContext(), selectedStartHour, hour, minute, true)
            mTimePicker?.setTitle("Select Start Time")
            mTimePicker?.show()
        }

        val selectedEndHour = TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
            endCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            endCalendar.set(Calendar.MINUTE, selectedMinute)
            if (binding.eventStartEt.text.isNullOrEmpty() || binding.eventEndEt.text.isNullOrEmpty()
                || compareTime(binding.eventStartEt.text.toString(), binding.eventEndEt.text.toString())
            ) {
                binding.eventEndEt.setText("$selectedHour:${selectedMinute.toString().padStart(2, '0')}")
            }
            isEndTimeSelected = true
            Log.d("startEndTimeCheck", "${myCalendar.timeInMillis} ${endCalendar.timeInMillis}")
        }

        binding.eventEndEt.setOnClickListener {
            val hour = myCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = myCalendar.get(Calendar.MINUTE)
            mTimePicker = TimePickerDialog(requireContext(), selectedEndHour, hour, minute, true)
            mTimePicker?.setTitle(getString(R.string.select_start_time))
            mTimePicker?.show()
        }

        hideKeyboard(requireActivity())
    }


    private fun validateData() {

        if (binding.data?.title.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_title), Toast.LENGTH_SHORT)
                .show()
        } else if (binding.data?.address.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_addess), Toast.LENGTH_SHORT)
                .show()
        } else if (!isDateSelected || !isStartTimeSelected || !isEndTimeSelected) {
            Toast.makeText(
                requireContext(),
                getString(R.string.select_event_time),
                Toast.LENGTH_SHORT
            ).show()
        } else if (binding.data?.details.isNullOrEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.enter_event_details),
                Toast.LENGTH_SHORT
            )
                .show()
        } else if (myCalendar.before(nowCalender)) {
            Toast.makeText(
                requireContext(),
                getString(R.string.start_time_past_error),
                Toast.LENGTH_SHORT
            )
                .show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val start = Instant.from(myCalendar.time.toInstant()).toString()
                val end = Instant.from(endCalendar.time.toInstant()).toString()
                binding.data?.plannedTime = start
                binding.data?.plannedEndTime = end
            }
            binding.data?.groupKey = groupDetails.groupKey
            viewModel.postBody = binding.data
            if (selectedPic != null) {
                viewModel.uploadEventImage(requireContext(), selectedPic!!)
            } else {
                viewModel.createPost()
            }
        }
    }

    private fun toggleDropdown() {
        if (!arrowOpen) {
            binding.toolbar.backArrowIv.setImageResource(R.drawable.ic_up_arrow)
            binding.searchPostAsCl.visibility = View.VISIBLE
            binding.selectUserCl.elevation = 2f
        } else {
            binding.toolbar.backArrowIv.setImageResource(R.drawable.ic_down_arrow)
            binding.searchPostAsCl.visibility = View.GONE
            binding.selectUserCl.elevation = 0f
        }
        arrowOpen = !arrowOpen
    }

    private fun updateLabel() {
        val myFormat = "MM/dd/yy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.eventDateEt.setText(sdf.format(myCalendar.getTime()))
    }

    fun compareTime(starTime: String, endTime: String): Boolean {
        val sdf = SimpleDateFormat("HH:mm")
        val starTime = sdf.parse(starTime)
        val endTime = sdf.parse(endTime)

        if (starTime.compareTo(endTime) < 0) {
            Toast.makeText(
                context,
                getString(R.string.end_time_should_greater),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    override val layoutId: Int
        get() = R.layout.fragment_create_event_group
    override val pageTitle: String?
        get() = ""



    //region onClicked
    private fun onClickedPickPhoto()
    {
        picker.pickImage(crop = false) { imageUri, bitmap, tag ->
            selectedPic = imageUri
            GlideApp.with(requireContext()).load(imageUri).into(binding.photoIv)
            binding.photoCl.visibility = View.VISIBLE
            binding.mediaBgCl.visibility = View.GONE
        }
    }
    //endregion
}