package com.orbis.orbis.ui.settingsModule.views

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Layout
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.*
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.BuildConfig
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.databinding.FragmentEditProfileBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.ProfileModule.views.UpdatePasswordDialog
import com.orbis.orbis.ui.homeModule.views.MapActivity
import com.orbis.orbis.utils.Utils
import com.orbis.orbis.utils.hideKeyboard
import com.orbis.orbis.utils.picker.Picker
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class EditProfileFragment(val userInfo: UserInfo) : BaseFragment(), DatePickerDialog.OnDateSetListener {
    lateinit var binding: FragmentEditProfileBinding
    lateinit var profileViewModel: ProfileViewModel
    private val picker = Picker()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_profile, container, false)
        initView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rateAppRl.setOnClickListener {
            val packageName = requireContext().packageName
            val uri: Uri = Uri.parse("market://details?id=$packageName")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            // To count with Play market backstack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
            }
        }
    }

    private fun initView()
    {
        picker.populate(fragment = this)

        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        downloadProfilePicture(userInfo.imageName)
        binding.data = userInfo

        binding.saveChange.setOnClickListener {
            profileViewModel.updateMyProfile(binding.data!!)
        }
        binding.changePhotoTv.setOnClickListener { onClickedChangePhoto() }
        binding.dateRl.setOnClickListener {
            openDatePicker()
        }
        binding.genderRl.setOnClickListener {
            genderClick(it)
        }
        binding.changePassword.setOnClickListener {
            val dialog = UpdatePasswordDialog(userInfo.email!!)
            dialog.show(requireActivity().supportFragmentManager, "update")
        }
        binding.inviteFriendsRl.setOnClickListener {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_TEXT, "https://orbis.social")
            startActivity(Intent.createChooser(i, "Invite to Orbis"))
        }
        binding.serviceTermsRl.setOnClickListener {
            val url = "https://orbis.social/privacy"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        binding.termsOfService.setOnClickListener {
            val url = "https://orbis.social/tos"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        binding.commentRl.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:")
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("orbis.invite@gmail.com"))
            try {
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.no_email_client),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.sponsorsRl.setOnClickListener {
            val url = "https://orbis.social/patrocinadores"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        binding.rateAppRl.setOnClickListener {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
                    )
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                    )
                )
            }
        }
        binding.userNameEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                binding.saveChange.visibility = View.VISIBLE
            }

        })
        binding.deleteMyAccount.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.account_delete))
                .setMessage(getString(R.string.delete_account_sure))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    profileViewModel.deleteMyProfile()
                }.setNegativeButton(getString(R.string.no)) { d, _ ->
                    d.dismiss()
                }.show()

        }
        setupObserver()
    }

    private fun setupObserver() {
        profileViewModel.deleteUser.observe(viewLifecycleOwner) {
            if (it) {
                PrefManager(requireContext()).saveUserName("")
                PrefManager(requireContext()).saveUserKey("")
                if (PrefManager(requireContext()).isSocialLogin()) {
                    FirebaseAuth.getInstance().signOut()

                }
                PrefManager(requireContext()).deleteSocialLogin()
                PrefManager(requireContext()).saveIdToken("")
                PrefManager(requireContext()).saveRefreshToken("")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.user_deleted),
                    Toast.LENGTH_LONG
                )
                    .show()
                goToMap()
            }
        }
        profileViewModel.myProfile.observe(viewLifecycleOwner) {
            Toast.makeText(
                requireContext(),
                getString(R.string.profile_updated),
                Toast.LENGTH_SHORT
            )
                .show()
            binding.data = userInfo
            binding.saveChange.visibility = View.GONE
        }
        profileViewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        profileViewModel.logout.observe(viewLifecycleOwner) {
            if (it) {

                PrefManager(requireContext()).saveUserName("")
                PrefManager(requireContext()).saveUserKey("")
                if (PrefManager(requireContext()).isSocialLogin()) {
                    FirebaseAuth.getInstance().signOut()

                }
                PrefManager(requireContext()).deleteSocialLogin()
                PrefManager(requireContext()).saveIdToken("")
                PrefManager(requireContext()).saveRefreshToken("")
                Log.d("userLogout", "successful")
                goToMap()
            }
        }
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance(TimeZone.getDefault())

        val dialog = DatePickerDialog(
            requireContext(), this,
            calendar[Calendar.YEAR], calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        )
        dialog.show()
    }

    fun goToMap() {
        val intent = Intent(requireContext(), MapActivity::class.java)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        startActivity(intent)
        requireActivity().finish()
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.logoutRl.setOnClickListener {
            if (PrefManager(requireContext()).isSocialLogin()) {
                FirebaseAuth.getInstance().signOut()
                profileViewModel.deleteTokenToServer()

            } else {
                profileViewModel.deleteTokenToServer()
            }
        }


        hideKeyboard(requireActivity())
    }

    private fun downloadProfilePicture(imageName: String?) {
       if(imageName != null)
       {
           val storage =
               Firebase.storage.getReference(
                   Constants.PROFILE_PICTURES + Utils.getImageUrl200(
                       imageName
                   )
               )
           storage.downloadUrl.addOnSuccessListener {
               Picasso.get().load(it).placeholder(R.drawable.ic_user).into(binding.userPicIv)
           }
       }
    }

    //region onClicked
    private fun onClickedChangePhoto()
    {
        picker.pickImage() { imageUri, bitmap, tag ->
            binding.data?.let { userInfo ->
                profileViewModel.uploadProfilePicture(requireContext(), imageUri, bitmap, userInfo)
                binding.userPicIv.setImageBitmap(bitmap)
            }
        }
    }
    //endregion

    companion object {
        fun getInstance(userInfo: UserInfo): EditProfileFragment {
            return EditProfileFragment(userInfo)
        }
    }
    private fun genderClick(user_menu_iv: View) {
        // When user click on the Button 1, create a PopupMenu.
        // And anchor Popup to the Button 2.
        val wrapper: ContextThemeWrapper = ContextThemeWrapper(context, R.style.PopupMenu)
        val popup = PopupMenu(wrapper, user_menu_iv, Gravity.START)

        val menu: Menu = popup.menu
        menu.add(getString(R.string.male))
        menu.add(getString(R.string.female))
        menu.add(getString(R.string.other))

        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val menuTitle = menuItem.title.toString()
            val spannableString = SpannableString(menuTitle)
            spannableString.setSpan(
                AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0,
                spannableString.length,
                0
            )
            menuItem.title = spannableString
        }
        // Register Menu Item Click event.
        popup.setOnMenuItemClickListener { item -> menuMainItemClicked(item) }
        // Show the PopupMenu.
        popup.show()
    }

    private fun menuMainItemClicked(item: MenuItem): Boolean {
        val title = item.title.toString()
        binding.data?.gender = title
        binding.genderTv.text = title
        binding.saveChange.visibility = View.VISIBLE
        return true
    }

    override fun onDateSet(p0: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        Log.d("dateOfBirth", dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
        val data = (monthOfYear + 1).toString() + "/" + dayOfMonth + "/" + year
        binding.dateTv.text = data
        binding.data?.dateOfBirth = data
        binding.saveChange.visibility = View.VISIBLE
    }

}