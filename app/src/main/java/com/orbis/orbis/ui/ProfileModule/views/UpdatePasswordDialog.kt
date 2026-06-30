package com.orbis.orbis.ui.ProfileModule.views

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
import com.orbis.orbis.databinding.LayoutEditPlaceDescriptionBinding

import com.orbis.orbis.databinding.LayoutReportDialogBinding
import com.orbis.orbis.databinding.LayoutUpdatePasswordBinding
import com.orbis.orbis.models.auth.PasswordUpdate
import com.orbis.orbis.ui.authModule.viewModel.AuthViewModel
import com.orbis.orbis.ui.groupsModule.viewModel.GroupViewModel
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdatePasswordDialog(val email: String) : DialogFragment() {
    lateinit var binding: LayoutUpdatePasswordBinding
    private lateinit var viewModel: ProfileViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        viewModel.passwordUpdated.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.password_updated),
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

        binding =
            DataBindingUtil.inflate(inflater, R.layout.layout_update_password, null, false)
        val data = PasswordUpdate(email)
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        binding.data = data

        binding.cancel.setOnClickListener { dismiss() }
        binding.share.setOnClickListener {
            validateData()
        }

        return AlertDialog.Builder(requireContext()).apply {
            setView(binding.root)
        }.create()
    }

    private fun validateData() {
        if (binding.data?.oldPassword?.isNotEmpty()!! && binding.data?.newPassword?.isNotEmpty()!!) {
            if (binding.data?.newPassword.toString() == binding.confirmNewPassword.editText?.text.toString()) {
                viewModel.updatePassword(binding.data!!)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.password_not_same),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.enter_password), Toast.LENGTH_SHORT)
                .show()
        }
    }

}