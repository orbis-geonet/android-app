package com.orbis.orbis.ui.subscriptionsModule.info

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.orbis.orbis.R
import com.orbis.orbis.databinding.DialogSubscriptionBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SubscriptionDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(
            @DrawableRes imageId: Int,
            message: String,
            confirmText: String
        ): SubscriptionDialogFragment {
            val args = Bundle()
            args.putInt("imageId", imageId)
            args.putString("message", message)
            args.putString("confirmText", confirmText)
            val fragment = SubscriptionDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var binding: DialogSubscriptionBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_subscription, null, false)
        initView()
        val alertDialog = AlertDialog.Builder(requireContext()).apply {
            setView(binding.root)
        }.create()
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return alertDialog
    }

    private fun initView() {
        val imageId = arguments?.getInt("imageId", 0) ?: 0
        val message = arguments?.getString("message", "") ?: ""
        val confirmText = arguments?.getString("confirmText", "") ?: ""
        binding.ivImage.setImageResource(imageId)
        binding.tvMessage.text = message
        binding.tvConfirm.text = confirmText
        binding.ivBack.setOnClickListener {
            dismiss()
        }
        binding.tvConfirm.setOnClickListener {
            dismiss()
        }
    }
}