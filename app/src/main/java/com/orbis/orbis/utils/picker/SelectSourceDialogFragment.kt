package com.orbis.orbis.utils.picker

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.orbis.orbis.R
import com.orbis.orbis.databinding.DialogDeleteSubscriptionBinding
import com.orbis.orbis.databinding.DialogSelectSourceBinding
import com.orbis.orbis.ui.subscriptionsModule.create.CreateSubscriptionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectSourceDialogFragment : DialogFragment() {

    private lateinit var binding: DialogSelectSourceBinding

    var onCameraClick: () -> Unit = {}
    var onGalleryClick: () -> Unit = {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        val inflater = LayoutInflater.from(context)
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_select_source, null, false)
        initView()
        val alertDialog = AlertDialog.Builder(requireContext()).apply {
            setView(binding.root)
        }.create()
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return alertDialog
    }

    private fun initView() {
        binding.galleryButton.setOnClickListener {
            dismiss()
            onGalleryClick()
        }
        binding.cameraButton.setOnClickListener {
            dismiss()
            onCameraClick()
        }
    }
}