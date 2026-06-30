package com.orbis.orbis.ui.messageModule.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.orbis.orbis.R
import com.orbis.orbis.databinding.PickerBottomSheetBinding

class PickerBottomSheet(val listener: PickerInteraction) : BottomSheetDialogFragment() {
    lateinit var binding: PickerBottomSheetBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.picker_bottom_sheet, container, false)
        binding.photo.setOnClickListener {
            listener.chosenItem(1)
        }
        binding.video.setOnClickListener {
            listener.chosenItem(2)
        }
        return binding.root
    }

    interface PickerInteraction {
        fun chosenItem(position: Int)
    }
}