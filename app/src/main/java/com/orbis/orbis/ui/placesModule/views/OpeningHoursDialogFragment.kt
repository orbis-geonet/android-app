package com.orbis.orbis.ui.placesModule.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.orbis.orbis.R
import com.orbis.orbis.databinding.FragmentPlaceOpeningHoursBinding
import com.orbis.orbis.models.BigDataSharConstants
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.place.WorkingHoursModel
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OpeningHoursDialogFragment : BottomSheetDialogFragment()
{
    //region variables
    lateinit var binding: FragmentPlaceOpeningHoursBinding
    lateinit var viewModel: PlaceViewModel

    lateinit var placeKey: String
    var data: ArrayList<WorkingHoursModel> = ArrayList()
    var canEdit: Boolean = false
    var listener: (PlaceDetails) -> Unit = {}
    //endregion

    //region lifeCycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placeKey = arguments?.getString("placeKey")!!
        canEdit = arguments?.getBoolean("canEdit")!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)

        data = BigDataSharConstants.openingHourArray

        initViews()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_place_opening_hours, container, false)
        viewModel = ViewModelProvider(this)[PlaceViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }

        return binding.root
    }

    companion object {
        fun newInstance(placeKey: String, canEdit: Boolean): OpeningHoursDialogFragment {
            val args = Bundle()
            args.putString("placeKey", placeKey)
            args.putBoolean("canEdit", canEdit)
            val fragment = OpeningHoursDialogFragment()
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

        binding.toolbar.titleTv.text = getString(R.string.schedule)
        binding.updateButton.isVisible = canEdit
    }

    private fun setOnClicked()
    {
        binding.updateButton.setOnClickListener {
            try
            {
                val editOpeningHoursEdiDialogFragment = EditOpeningHoursEdiDialogFragment.newInstance(
                    placeKey
                )
                editOpeningHoursEdiDialogFragment.show(requireActivity().supportFragmentManager, EditOpeningHoursEdiDialogFragment::class.java.simpleName)
                editOpeningHoursEdiDialogFragment.listener = {
                    data = it.workingHours
                    setCurrentTime()
                    listener(it)
                }
            }
            catch (e: Exception) { e.printStackTrace() }
        }

        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
    }

    private fun setCurrentTime()
    {
        binding.day1Layout.isVisible = false
        binding.day2Layout.isVisible = false
        binding.day3Layout.isVisible = false
        binding.day4Layout.isVisible = false
        binding.day5Layout.isVisible = false
        binding.day6Layout.isVisible = false
        binding.day7Layout.isVisible = false

        for(day in data)
        {
            when(day.day)
            {
                "1" -> {
                    binding.day1Layout.isVisible = true
                    binding.day1TextView.text = day.time
                }
                "2" -> {
                    binding.day2Layout.isVisible = true
                    binding.day2TextView.text = day.time
                }
                "3" -> {
                    binding.day3Layout.isVisible = true
                    binding.day3TextView.text = day.time
                }
                "4" -> {
                    binding.day4Layout.isVisible = true
                    binding.day4TextView.text = day.time
                }
                "5" -> {
                    binding.day5Layout.isVisible = true
                    binding.day5TextView.text = day.time
                }
                "6" -> {
                    binding.day6Layout.isVisible = true
                    binding.day6TextView.text = day.time
                }
                "7" -> {
                    binding.day7Layout.isVisible = true
                    binding.day7TextView.text = day.time
                }
            }
        }
    }
    //endregion
}