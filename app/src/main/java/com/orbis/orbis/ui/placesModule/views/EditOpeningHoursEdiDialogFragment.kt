package com.orbis.orbis.ui.placesModule.views

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentPlaceEditOpeningHoursBinding
import com.orbis.orbis.databinding.FragmentPlaceEditPhoneBinding
import com.orbis.orbis.databinding.FragmentPlaceOpeningHoursBinding
import com.orbis.orbis.models.BigDataSharConstants
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.place.PlaceUpdateBody
import com.orbis.orbis.models.place.WorkingHoursModel
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class EditOpeningHoursEdiDialogFragment : BaseBottomSheetFragment()
{
    //region variables
    override val layoutId: Int get() = R.layout.fragment_place_edit_opening_hours
    override val pageTitle: String? get() = getString(R.string.opening_hours)

    lateinit var binding: FragmentPlaceEditOpeningHoursBinding
    lateinit var viewModel: PlaceViewModel

    private var mTimePicker: TimePickerDialog? = null

    lateinit var placeKey: String
    var data: ArrayList<WorkingHoursModel> = ArrayList()

    var listener: (PlaceDetails) -> Unit = {}
    //endregion

    //region lifeCycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placeKey = arguments?.getString("placeKey")!!
        data = BigDataSharConstants.openingHourArray
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)

        initViews()
        setupObservers()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_place_edit_opening_hours, container, false)
        viewModel = ViewModelProvider(this)[PlaceViewModel::class.java]
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }

        return binding.root
    }

    companion object {
        fun newInstance(placeKey: String): EditOpeningHoursEdiDialogFragment {
            val args = Bundle()
            args.putString("placeKey", placeKey)
            val fragment = EditOpeningHoursEdiDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
    //endregion

    //region init
    private fun initViews()
    {
        setOnClicked()
        setCurrentTime()

        binding.toolbar.titleTv.text = getString(R.string.opening_hours)
    }

    private fun setOnClicked()
    {
        binding.updateButton.setOnClickListener {
            val placeUpdateBody = PlaceUpdateBody()
            placeUpdateBody.workingHours = getOpeningHourModel()
            viewModel.updatePlace(placeUpdateBody, placeKey)
        }

        binding.day1StartTextView.setOnClickListener { startTimePicking(binding.day1StartTextView, isStart = true) }
        binding.day2StartTextView.setOnClickListener { startTimePicking(binding.day2StartTextView, isStart = true) }
        binding.day3StartTextView.setOnClickListener { startTimePicking(binding.day3StartTextView, isStart = true) }
        binding.day4StartTextView.setOnClickListener { startTimePicking(binding.day4StartTextView, isStart = true) }
        binding.day5StartTextView.setOnClickListener { startTimePicking(binding.day5StartTextView, isStart = true) }
        binding.day6StartTextView.setOnClickListener { startTimePicking(binding.day6StartTextView, isStart = true) }
        binding.day7StartTextView.setOnClickListener { startTimePicking(binding.day7StartTextView, isStart = true) }

        binding.day1EndTextView.setOnClickListener { startTimePicking(binding.day1EndTextView, isStart = false) }
        binding.day2EndTextView.setOnClickListener { startTimePicking(binding.day2EndTextView, isStart = false) }
        binding.day3EndTextView.setOnClickListener { startTimePicking(binding.day3EndTextView, isStart = false) }
        binding.day4EndTextView.setOnClickListener { startTimePicking(binding.day4EndTextView, isStart = false) }
        binding.day5EndTextView.setOnClickListener { startTimePicking(binding.day5EndTextView, isStart = false) }
        binding.day6EndTextView.setOnClickListener { startTimePicking(binding.day6EndTextView, isStart = false) }
        binding.day7EndTextView.setOnClickListener { startTimePicking(binding.day7EndTextView, isStart = false) }

        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
    }

    private fun setupObservers() {
        viewModel.placeDetails.observe(viewLifecycleOwner) {
            listener(it)
            BigDataSharConstants.openingHourArray = it.workingHours
            dismiss()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        viewModel.error.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.error.postValue("")
            }
        }
    }

    private fun setCurrentTime()
    {
        for(day in data)
        {
            when(day.day)
            {
                "1" -> {
                    val timeSplit = day.time.split(" ")
                    binding.day1StartTextView.text = timeSplit[0]
                    binding.day1EndTextView.text = timeSplit[2]
                }
                "2" -> {
                    val timeSplit = day.time.split(" ")
                    binding.day2StartTextView.text = timeSplit[0]
                    binding.day2EndTextView.text = timeSplit[2]
                }
                "3" -> {
                    val timeSplit = day.time.split(" ")
                    binding.day3StartTextView.text = timeSplit[0]
                    binding.day3EndTextView.text = timeSplit[2]
                }
                "4" -> {
                    val timeSplit = day.time.split(" ")
                    binding.day4StartTextView.text = timeSplit[0]
                    binding.day4EndTextView.text = timeSplit[2]
                }
                "5" -> {
                    val timeSplit = day.time.split(" ")
                    binding.day5StartTextView.text = timeSplit[0]
                    binding.day5EndTextView.text = timeSplit[2]
                }
                "6" -> {
                    val timeSplit = day.time.split(" ")
                    binding.day6StartTextView.text = timeSplit[0]
                    binding.day6EndTextView.text = timeSplit[2]
                }
                "7" -> {
                    val timeSplit = day.time.split(" ")
                    binding.day7StartTextView.text = timeSplit[0]
                    binding.day7EndTextView.text = timeSplit[2]
                }
            }
        }
    }
    //endregion

    //region TimePickers
    private fun startTimePicking(clickedTextView: TextView, isStart: Boolean)
    {
        var hour = if(isStart) 9 else 22
        var minute = 0

        if(clickedTextView.text.isNotEmpty())
        {
            val timeArray = clickedTextView.text.split(":")
            hour = timeArray[0].toInt()
            minute = timeArray[1].toInt()
        }

        val selectedStartHour = TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
            clickedTextView.text = String.format("%02d:%02d", selectedHour, selectedMinute)
        }

        mTimePicker = TimePickerDialog(requireContext(), selectedStartHour, hour, minute, true)
        mTimePicker!!.setTitle(getString(R.string.select_start_time))
        mTimePicker!!.show()
    }

    private fun getOpeningHourModel() : ArrayList<WorkingHoursModel>
    {
        val openingHours = ArrayList<WorkingHoursModel>()

        if(binding.day1StartTextView.text.isNotEmpty() && binding.day1EndTextView.text.isNotEmpty())
        {
            val time = "${binding.day1StartTextView.text} - ${binding.day1EndTextView.text}"
            openingHours.add(WorkingHoursModel(day = "1", time = time))
        }

        if(binding.day2StartTextView.text.isNotEmpty() && binding.day2EndTextView.text.isNotEmpty())
        {
            val time = "${binding.day2StartTextView.text} - ${binding.day2EndTextView.text}"
            openingHours.add(WorkingHoursModel(day = "2", time = time))
        }

        if(binding.day3StartTextView.text.isNotEmpty() && binding.day3EndTextView.text.isNotEmpty())
        {
            val time = "${binding.day3StartTextView.text} - ${binding.day3EndTextView.text}"
            openingHours.add(WorkingHoursModel(day = "3", time = time))
        }

        if(binding.day4StartTextView.text.isNotEmpty() && binding.day4EndTextView.text.isNotEmpty())
        {
            val time = "${binding.day4StartTextView.text} - ${binding.day4EndTextView.text}"
            openingHours.add(WorkingHoursModel(day = "4", time = time))
        }

        if(binding.day5StartTextView.text.isNotEmpty() && binding.day5EndTextView.text.isNotEmpty())
        {
            val time = "${binding.day5StartTextView.text} - ${binding.day5EndTextView.text}"
            openingHours.add(WorkingHoursModel(day = "5", time = time))
        }

        if(binding.day6StartTextView.text.isNotEmpty() && binding.day6EndTextView.text.isNotEmpty())
        {
            val time = "${binding.day6StartTextView.text} - ${binding.day6EndTextView.text}"
            openingHours.add(WorkingHoursModel(day = "6", time = time))
        }

        if(binding.day7StartTextView.text.isNotEmpty() && binding.day7EndTextView.text.isNotEmpty())
        {
            val time = "${binding.day7StartTextView.text} - ${binding.day7EndTextView.text}"
            openingHours.add(WorkingHoursModel(day = "7", time = time))
        }

        return openingHours
    }
    //endregion
}