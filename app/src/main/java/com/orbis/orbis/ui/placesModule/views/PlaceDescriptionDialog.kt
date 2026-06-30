package com.orbis.orbis.ui.placesModule.views

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.orbis.orbis.R
import com.orbis.orbis.databinding.LayoutEditPlaceDescriptionBinding

import com.orbis.orbis.databinding.LayoutReportDialogBinding
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaceDescriptionDialog(private val listener: OnEditDescription) : DialogFragment() {
    lateinit var binding: LayoutEditPlaceDescriptionBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val inflater = LayoutInflater.from(context)
        binding =
            DataBindingUtil.inflate(inflater, R.layout.layout_edit_place_description, null, false)
        binding.cancel.setOnClickListener { dismiss() }
        binding.share.setOnClickListener {
            listener.descriptionEdited(binding.editTextTextPersonName.text.toString())
            dismiss()
        }

        return AlertDialog.Builder(requireContext()).apply {
            setView(binding.root)
        }.create()
    }

    interface OnEditDescription {
        fun descriptionEdited(description: String)
    }
}