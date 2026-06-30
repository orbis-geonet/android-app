package com.orbis.orbis.ui.subscriptionsModule.list

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
import com.orbis.orbis.ui.subscriptionsModule.create.CreateSubscriptionViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class DeleteSubscriptionDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(message: String): DeleteSubscriptionDialogFragment {
            val args = Bundle()
            args.putString("message", message)
            val fragment = DeleteSubscriptionDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var binding: DialogDeleteSubscriptionBinding
    private lateinit var viewModel: CreateSubscriptionViewModel

    var onDeleteClick: () -> Unit = {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_delete_subscription, null, false)
        viewModel = ViewModelProvider(requireActivity())[CreateSubscriptionViewModel::class.java]
        initView()
        val alertDialog = AlertDialog.Builder(requireContext()).apply {
            setView(binding.root)
        }.create()
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return alertDialog
    }

    private fun initView() {
        val message = arguments?.getString("message", "") ?: ""
        binding.tvTitle.text = message
        binding.btnYes.setOnClickListener {
            dismiss()
            onDeleteClick()
        }
        binding.btnNo.setOnClickListener {
            dismiss()
        }
    }
}