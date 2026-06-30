package com.orbis.orbis.ui.groupsModule.views

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.orbis.orbis.R

import com.orbis.orbis.databinding.LayoutReportDialogBinding
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportDialog(private val type: String, private val key: String) : DialogFragment() {
    lateinit var binding: LayoutReportDialogBinding
    lateinit var viewModel: GroupViewModel
    lateinit var profileViewModel: ProfileViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel.reportPost.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        profileViewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        profileViewModel.reportUser.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
        profileViewModel.reportPlace.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.place_reported),
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
        }
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val inflater = LayoutInflater.from(context)
        viewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_report_dialog, null, false)

        binding.textView7.text = if (type != "Group") "Report $type" else "Report Tribe"
        binding.cancel.setOnClickListener { dismiss() }
        binding.report.setOnClickListener {
            if (type == "Post") {
                val text = binding.editTextTextPersonName.text.toString()
                if (text.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.write_to_report),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.reportPost(key, binding.editTextTextPersonName.text.toString())
                }

            } else if (type == "Group") {
                val text = binding.editTextTextPersonName.text.toString()
                if (text.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.write_to_report),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.reportGroup(key, binding.editTextTextPersonName.text.toString())
                }
            } else if (type == "User") {
                val text = binding.editTextTextPersonName.text.toString()
                if (text.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.write_to_report),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    profileViewModel.reportUser(key, binding.editTextTextPersonName.text.toString())
                }
            } else if (type == "Place") {
                val text = binding.editTextTextPersonName.text.toString()
                if (text.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.write_to_report),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    profileViewModel.reportPlace(
                        key,
                        binding.editTextTextPersonName.text.toString()
                    )
                }
            }
        }

        return AlertDialog.Builder(requireContext()).apply {
            setView(binding.root)
        }.create()
    }
}