package com.orbis.orbis.ui.placesModule.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.ui.placesModule.adapter.CheckinCreatePlaceAdapter
import com.orbis.orbis.utils.hideKeyboard
import com.orbis.orbis.databinding.FragmentSearchListBinding


class CheckInCreatePlaceDialogFragment : BaseBottomSheetFragment(),
    CheckinCreatePlaceAdapter.PlaceCardInteraction {
    private var mAdapter: CheckinCreatePlaceAdapter? = null
    private lateinit var binding: FragmentSearchListBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }
        binding = FragmentSearchListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.placesRv.setLayoutManager(LinearLayoutManager(requireContext()))
        val arrayList = ArrayList<GroupDetails>()

        mAdapter = CheckinCreatePlaceAdapter(this, requireContext(), arrayList)
        //mAdapter!!.setList(arrayList)
        binding.placesRv.setAdapter(mAdapter)
        binding.toolbar.titleTv.setText(resources.getString(R.string.claim))
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
        hideKeyboard(requireActivity())
    }

    override val layoutId: Int
        get() = R.layout.fragment_checkin_list
    override val pageTitle: String?
        get() = ""

    companion object {
        fun getInstance(): CheckInCreatePlaceDialogFragment {
            return CheckInCreatePlaceDialogFragment()
        }
    }


    override fun onItemClicked(position: Int) {
        TODO("Not yet implemented")
    }


}