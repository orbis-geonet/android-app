package com.orbis.orbis.utils.customViews

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.orbis.orbis.R
import com.orbis.orbis.databinding.DialogBlockGroupBinding
import com.orbis.orbis.databinding.DialogSelectSourceBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockGroupDialogFragment(private val isBlocked: Boolean = false) : DialogFragment() {

    private lateinit var binding: DialogBlockGroupBinding

    var onYes: () -> Unit = {}
    var onNo: () -> Unit = {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        val inflater = LayoutInflater.from(context)
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_block_group, null, false)
        initView()
        val alertDialog = AlertDialog.Builder(requireContext()).apply {
            setView(binding.root)
        }.create()
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return alertDialog
    }

    private fun initView() {

        if(isBlocked)
        {
            binding.tvTitle.text = resources.getString(R.string.group_is_blocked)
        }

        binding.galleryButton.setOnClickListener {
            dismiss()
            onYes()
        }
        binding.cameraButton.setOnClickListener {
            dismiss()
            onNo()
        }
    }
}