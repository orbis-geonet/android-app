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
import com.orbis.orbis.databinding.FragmentPlaceEditAddressBinding
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.place.PlaceUpdateBody
import com.orbis.orbis.models.place.RatePlaceModel
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditAddressDialogFragment : BottomSheetDialogFragment()
{
    //region variables
    lateinit var binding: FragmentPlaceEditAddressBinding
    lateinit var viewModel: PlaceViewModel

    lateinit var placeKey: String
    lateinit var data: String
    var canEdit: Boolean = false
    var listener: (PlaceDetails) -> Unit = {}
    //endregion

    //region lifeCycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placeKey = arguments?.getString("placeKey")!!
        data = arguments?.getString("data")!!
        canEdit = arguments?.getBoolean("canEdit")!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)

        initViews()
        setupObservers()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_place_edit_address, container, false)
        viewModel = ViewModelProvider(this)[PlaceViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }

        binding.data = data

        return binding.root
    }

    companion object {
        fun newInstance(placeKey: String, data: String, canEdit: Boolean): EditAddressDialogFragment {
            val args = Bundle()
            args.putString("placeKey", placeKey)
            args.putString("data", data)
            args.putBoolean("canEdit", canEdit)
            val fragment = EditAddressDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
    //endregion

    //region init
    private fun initViews()
    {
        setOnClicked()

        binding.toolbar.titleTv.text = getString(R.string.hint_address)

        binding.addressTextView.isVisible = !canEdit
        binding.addressLayout.isVisible = canEdit
        binding.updateButton.isVisible = canEdit
    }

    private fun setOnClicked()
    {
        binding.updateButton.setOnClickListener {
            val placeUpdateBody = PlaceUpdateBody()
            placeUpdateBody.address = binding.addressEditText.text.toString()
            viewModel.updatePlace(placeUpdateBody, placeKey)
        }
        binding.openButton.setOnClickListener {
            Constants.openGoogleMaps(requireContext(), data)
        }
        binding.copyAddressImageView.setOnClickListener {
            Constants.copyTextToClipboard(requireContext(), data)
        }
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
    }

    private fun setupObservers() {
        viewModel.placeDetails.observe(viewLifecycleOwner) {
            listener(it)
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
    //endregion
}
