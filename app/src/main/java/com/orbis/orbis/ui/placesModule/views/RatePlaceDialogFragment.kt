package com.orbis.orbis.ui.placesModule.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentRatePlaceBinding
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.models.place.RatePlaceBody
import com.orbis.orbis.models.place.RatePlaceModel
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RatePlaceDialogFragment : BottomSheetDialogFragment()
{
    //region variables
    lateinit var binding: FragmentRatePlaceBinding
    lateinit var viewModel: PlaceViewModel

    lateinit var placeKey: String
    var listener: (RatePlaceModel) -> Unit = {}

    //endregion

    //region lifeCycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placeKey = arguments?.getString("placeKey")!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)

        initViews()
        setupObservers()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_rate_place, container, false)
        viewModel = ViewModelProvider(this)[PlaceViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }

        return binding.root
    }

    companion object {
        fun newInstance(placeKey: String): RatePlaceDialogFragment {
            val args = Bundle()
            args.putString("placeKey", placeKey)
            val fragment = RatePlaceDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
    //endregion

    //region init
    private fun initViews()
    {
        binding.toolbar.titleTv.text = getString(R.string.textView_reviews)
        setOnClicked()
    }

    private fun setOnClicked()
    {
        binding.updateButton.setOnClickListener {
            viewModel.ratePlace(RatePlaceBody(placeKey, binding.ratingBar.rating.toInt()))
        }
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
    }

    private fun setupObservers() {
        viewModel.placeRated.observe(viewLifecycleOwner) {
            listener(it)
            this.dismiss()
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
