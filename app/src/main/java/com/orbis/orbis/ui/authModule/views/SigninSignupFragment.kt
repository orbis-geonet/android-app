package com.orbis.orbis.ui.authModule.views


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.facebook.login.LoginManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.TabLayoutOnPageChangeListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseFragment
import com.orbis.orbis.databinding.FragmentSgininSginupBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.auth.ProfileUpdateBody
import com.orbis.orbis.ui.authModule.adapter.AuthPagerAdapter
import com.orbis.orbis.ui.authModule.viewModel.AuthViewModel
import com.orbis.orbis.ui.homeModule.views.MapActivity
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent


@AndroidEntryPoint
class SigninSignupFragment : BaseFragment() {
    lateinit var binding: FragmentSgininSginupBinding
    private lateinit var viewModel: AuthViewModel
    var current_position = 0
    val RC_GOOGLE_SIGN_IN = 114
    val RC_TWITTER_SIGN_IN = 124
    private var keyboardListenersAttached = false
    private val context = this
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_sginin_sginup, container, false)
        viewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
        setupSocialLogin()
        setupObservers()
        KeyboardVisibilityEvent.setEventListener(
            activity
        ) { isOpen ->
            if (isOpen) {

                //scroll to last view
                val lastChild: View =
                    binding.nestedScrollView.getChildAt(binding.nestedScrollView.getChildCount() - 1)
                val bottom: Int = lastChild.bottom + binding.nestedScrollView.getPaddingBottom()
                val sy: Int = binding.nestedScrollView.getScrollY()
                val sh: Int = binding.nestedScrollView.getHeight()
                val delta = bottom - (sy + sh)
                binding.nestedScrollView.smoothScrollBy(
                    0,
                    delta - binding.linearLayout3.height - 50
                )
            }
        }
        //attachKeyboardListeners()
        return binding.root

    }




    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        var haveAccount = getArguments()?.getBoolean("HaveAccount", false)
        setUpViewPager(haveAccount)
        hideKeyboard(requireActivity())
        binding.backArrowIv.setOnClickListener { requireActivity().onBackPressed() }
    }

    private fun setupSocialLogin() {
        val googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                viewModel.googleSignIn(task, requireActivity())
            }
        }

        binding.googleCardview.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
        val signInLauncher = registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
        ) { result ->
            this.onSignInResult(result)
        }
        binding.googleCardview.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
        }

        binding.facebookCardview.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                requireActivity(),
                listOf("email", "public_profile", "user_gender", "user_birthday")
            )
        }
        binding.twitterCardview.setOnClickListener {
            val providers = arrayListOf(
                AuthUI.IdpConfig.TwitterBuilder().build()
            )
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
            signInLauncher.launch(signInIntent)
        }


    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (response != null) {
            Log.d("emailGet", result.toString())
        }
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Log.d(
                    "SocialSignInInfo",
                    user.displayName + " " + user.email + " " + user.photoUrl + " " + user.uid
                )
                PrefManager(requireContext()).saveUserName(user.displayName!!)
                val profileUpdateBody =
                    ProfileUpdateBody(user?.displayName!!, user.photoUrl.toString())
                user.getIdToken(true).addOnCompleteListener { tok ->
                    if (tok.isSuccessful) {
                        viewModel.updateProfile(tok.result?.token!!, profileUpdateBody, true)
                    }
                }
            }
        } else {
            if (response != null && response?.error!!.errorCode == 3) {
                ViewUtils.showDialogue(
                    requireContext(),
                    getString(R.string.already_exists),
                    getString(R.string.email_already_have)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requireActivity().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        } else {
            requireActivity().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requireActivity().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            )
        } else {
            requireActivity().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )
        }
    }

    companion object {
        fun getInstance(): SigninSignupFragment {
            return SigninSignupFragment()
        }

    }

    private fun setUpViewPager(haveAccount: Boolean?) {
        val adapter = AuthPagerAdapter(
            childFragmentManager,
            FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        )
        binding.containerLayout.setAdapter(adapter)
        binding.containerLayout.addOnPageChangeListener(TabLayoutOnPageChangeListener(binding.tabLayout))
        setupTabs()
        if (haveAccount == true) {
            binding.containerLayout.currentItem = 0
        } else {
            binding.containerLayout.currentItem = 1
        }
    }

    fun setupTabs() {

        addNewTab(true, resources.getString(R.string.login))
        addNewTab(false, resources.getString(R.string.register))

        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                current_position = tab.position
                binding.containerLayout.currentItem = current_position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        binding.containerLayout.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position))
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    fun addNewTab(isDefaultSelected: Boolean?, title: String) {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(title).setIcon(null), isDefaultSelected!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("googleSignIn", "fragment")
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Log.d("googleSignIn", "found")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            viewModel.googleSignIn(task, requireActivity())
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun setupObservers() {
        viewModel.authError.observe(viewLifecycleOwner) { method ->
            when (method) {
                "password" -> {
                    val builder = MaterialAlertDialogBuilder(requireContext())

                    val dialogView = LayoutInflater.from(builder.context)
                        .inflate(R.layout.dialog_enter_password, null)
                    val passwordInput = dialogView.findViewById<EditText>(R.id.passwordEditText)

                    builder
                        .setTitle(getString(R.string.already_exist))
                        .setMessage("This email is registered with a password. Enter it to continue.")
                        .setView(dialogView)
                        .setPositiveButton("Sign In") { _, _ ->
                            val password = passwordInput.text.toString()
                            if (password.isNotEmpty()) {
                                viewModel.signInWithEmail(
                                    viewModel.pendingEmail.value ?: return@setPositiveButton,
                                    password
                                )
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                "wrong_password" -> {
                    Toast.makeText(requireContext(), "Wrong password, try again.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val methodName = when (method) {
                        "facebook.com" -> "Facebook"
                        "twitter.com" -> "Twitter"
                        else -> method.replace(".com", "")
                    }
                    ViewUtils.showDialogue(
                        requireContext(),
                        getString(R.string.already_exist),
                        getString(R.string.already_sign_up, methodName)
                    )
                }
            }
        }
        viewModel.errorResponse.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loading = it
        }
        viewModel.userProfile.observe(viewLifecycleOwner) {
            PrefManager(requireContext()).saveUserName(it.displayName)
            Toast.makeText(requireContext(), getString(R.string.logged_in_success), Toast.LENGTH_SHORT).show()
            Log.d("loginDetect", PrefManager(requireContext()).isSocialLogin().toString())
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                Log.d("NOTIF_DEBUG", "Post-login FCM token upload: $token")
                // call your repo to update the token
                // profileRepositories.updateFcmToken(token) — ideally via ViewModel
            }
            val i = Intent(requireActivity(), MapActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            requireActivity().finish()
        }

    }


}