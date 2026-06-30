package com.orbis.orbis.ui.placesModule.views

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.github.piasy.rxandroidaudio.AudioRecorder
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.tabs.TabLayout
import com.nguyencse.URLEmbeddedTask
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseBottomSheetFragment
import com.orbis.orbis.databinding.FragmentCreatePostBinding
import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.listeners.OnPlaceCreate
import com.orbis.orbis.listeners.OnPlaceSelect
import com.orbis.orbis.listeners.SearchClick
import com.orbis.orbis.models.Constants
import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.models.auth.UserInfo
import com.orbis.orbis.models.group.GroupDetails
import com.orbis.orbis.models.place.PlaceDetails
import com.orbis.orbis.models.posts.PostBody
import com.orbis.orbis.models.posts.RichLinkData
import com.orbis.orbis.ui.ProfileModule.views.ProfileViewModel
import com.orbis.orbis.ui.groupsModule.adapter.ImageSliderAdapterLocal
import com.orbis.orbis.ui.groupsModule.viewModel.CheckIn
import com.orbis.orbis.ui.homeModule.views.SelectCheckInPlaceDialogFragment
import com.orbis.orbis.ui.placesModule.PlaceViewModel
import com.orbis.orbis.ui.placesModule.adapter.CheckinCreatePlaceAdapter
import com.orbis.orbis.utils.PermissionUtil
import com.orbis.orbis.utils.ViewUtils
import com.orbis.orbis.utils.ViewUtils.Companion.changeStrokeColor
import com.orbis.orbis.utils.ViewUtils.Companion.changeViewColor
import com.orbis.orbis.utils.hideKeyboard
import com.orbis.orbis.utils.picker.Picker
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class CreatePostDialogFragment : BaseBottomSheetFragment(),
    CheckinCreatePlaceAdapter.PlaceCardInteraction, SearchClick, OnPlaceSelect {

    companion object {
        private const val TAG = "CreatePostDialog"

        fun newInstance(
            group: GroupDetails? = null,
            placeDetails: PlaceDetails? = null,
            isCheckin: Boolean = false,
            location: Location? = null,
            restrictGroup: Boolean = false,
            restrictPage: Boolean = false,
        ): CreatePostDialogFragment {
            val args = Bundle().apply {
                putParcelable("group", group)
                putParcelable("placeDetails", placeDetails)
                putBoolean("isCheckin", isCheckin)
                putParcelable("location", location)
                putBoolean("restrictGroup", restrictGroup)
                putBoolean("restrictPage", restrictPage)
            }
            return CreatePostDialogFragment().also { it.arguments = args }
        }
    }

    private val picker = Picker()
    private var group: GroupDetails? = null
    private var placeDetails: PlaceDetails? = null
    private var isCheckin: Boolean = false
    private var location: Location? = null
    private var restrictGroup: Boolean = false
    private var restrictPage: Boolean = false
    var listener: OnPlaceCreate? = null

    // ── UI / Adapters ─────────────────────────────────────────────────────────
    private var mAdapter: CheckinCreatePlaceAdapter? = null
    private var arrowOpen = false
    private var photoSliderOpen = false
    lateinit var binding: FragmentCreatePostBinding
    var profileImage: Uri? = null

    // ── ViewModels ────────────────────────────────────────────────────────────
    lateinit var viewModel: PlaceViewModel
    lateinit var profileViewModel: ProfileViewModel

    // ── Location ──────────────────────────────────────────────────────────────
    private lateinit var locationManager: LocationManager
    private lateinit var checkLocationPermission: ActivityResultLauncher<Array<String>>

    // ── Media – images / video ────────────────────────────────────────────────
    private val pickedImages: ArrayList<String> = ArrayList()
    private val pickedUris: ArrayList<Uri> = ArrayList()
    lateinit var player: SimpleExoPlayer
    private lateinit var imageSliderAdapter: ImageSliderAdapterLocal

    // ── Audio recording ───────────────────────────────────────────────────────
    private lateinit var audioPermission: ActivityResultLauncher<Array<String>>
    private lateinit var mAudioRecorder: AudioRecorder
    private lateinit var audioPlayer: MediaPlayer
    private var audioFile: File? = null
    private var isRecording = false
    private var countDownTimer: CountDownTimer? = null

    val groupList: ArrayList<GroupDetails> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            group        = getParcelable("group")
            placeDetails = getParcelable("placeDetails")
            isCheckin    = getBoolean("isCheckin", false)
            location     = getParcelable("location")
            restrictGroup = getBoolean("restrictGroup", false)
            restrictPage  = getBoolean("restrictPage", false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_post, container, false)
        viewModel        = ViewModelProvider(this)[PlaceViewModel::class.java]
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        profileViewModel.getMyProfile()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, nav.bottom)
            insets
        }

        initPermissionLaunchers()
        initView()
        setupObservers()
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.toolbar.titleTv.text = ""
        binding.placesRv.layoutManager = LinearLayoutManager(requireContext())
        mAdapter = CheckinCreatePlaceAdapter(this, requireContext(), groupList)
        binding.placesRv.adapter = mAdapter

        if (location != null) {
            viewModel.findRecommendedGroup(location!!.latitude, location!!.longitude)
        }

        viewModel.recommendedGroups.observe(viewLifecycleOwner) {
            groupList.addAll(it)
            mAdapter?.notifyDataSetChanged()
        }

        profileViewModel.myProfile.observe(viewLifecycleOwner) { user ->
            if (group == null && placeDetails == null) {
                ViewUtils.loadUserProfilePic(requireContext(), binding.profileImage, user.imageName, user.providerImageUrl)
                ViewUtils.loadUserProfilePic(requireContext(), binding.imageIv, user.imageName, user.providerImageUrl)
                setupPostAsUser(user)
                binding.userBox.setOnClickListener {
                    setupPostAsUser(user)
                    toggleDropdown()
                }
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String) = false
            override fun onQueryTextSubmit(query: String): Boolean {
                showSearchPlaceSheet(query)
                return false
            }
        })

        // If a place was pre-selected (launched from map), lock the check-in button
        if (placeDetails != null) {
            btnStateToggle(binding.checkIdBtn, selectedState = true, enable = false)
        }

        binding.checkIdBtn.setOnClickListener {
            if (!restrictPage) {
                if (!isCheckin) {
                    btnStateToggle(binding.checkIdBtn, true)
                    btnStateToggle(binding.videoBtn, false)
                    showCheckInPlaceSheet()
                    isCheckin = true
                } else {
                    binding.placeTv.callOnClick()
                }
            } else if (isIn1km()) {
                isCheckin = !isCheckin
                btnStateToggle(binding.checkIdBtn, isCheckin)
                if (isCheckin) binding.postBody?.placeKey = placeDetails?.placeKey
            } else {
                if (!isCheckin) {
                    btnStateToggle(binding.checkIdBtn, true)
                    btnStateToggle(binding.videoBtn, false)
                    isCheckin = true
                    val currentPlace = binding.place
                    if (currentPlace != null) onPlaceSelect(currentPlace)
                    else showCheckInPlaceSheet()
                } else {
                    binding.placeTv.callOnClick()
                }
            }
        }

        binding.selectUserCl.setOnClickListener { toggleDropdown() }
        binding.closeIv.setOnClickListener { removeCurrentPhoto() }
        binding.toolbar.backArrowIv.setOnClickListener { dismiss() }
        hideKeyboard(requireActivity())
    }

    private fun removeCurrentPhoto() {
        val currentIndex = binding.checkInViewpager.currentItem

        if (currentIndex < pickedImages.size) {
            pickedImages.removeAt(currentIndex)
        }

        if (currentIndex < pickedUris.size) {
            pickedUris.removeAt(currentIndex)
        }

        if (pickedImages.isEmpty()) {
            hidePhotoSlider()
            return
        }

        imageSliderAdapter.notifyDataSetChanged()
        setupTabs(binding.tabLayout, binding.checkInViewpager)

        val newIndex = minOf(currentIndex, pickedImages.lastIndex)
        binding.checkInViewpager.currentItem = newIndex
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up media resources to avoid leaks
        stopAndReleaseAudioPlayer()
        if (::player.isInitialized) {
            player.release()
        }
        countDownTimer?.cancel()
    }
    private fun initPermissionLaunchers() {
        // Location permission
        checkLocationPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                val locGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val locNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                Constants.location = locGps ?: locNet
            }
            handleDialogComponents()
        }

        // FIXED: audio permission now actually starts recording after grant
        audioPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
                startAudioRecording()
            } else {
                Toast.makeText(requireContext(), "Microphone permission required to record audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initView() {
        picker.populate(fragment = this)
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val permissionUtil = PermissionUtil()
        checkLocationPermission.launch(permissionUtil.locationPermissions)

        // Simple click listeners that don't depend on mAudioRecorder being ready
        binding.placeTv.setOnClickListener {
            // Don't allow clearing the place if it was pre-selected from the map
            if (placeDetails != null) return@setOnClickListener
            btnStateToggle(binding.checkIdBtn, false)
            binding.placeTv.visibility = View.GONE
            binding.pinIv.visibility  = View.GONE
            binding.postBody?.placeKey = null
            binding.place = null
            isCheckin = false
        }

        binding.videoBtn.setOnClickListener {
            pickedUris.clear()
            onClickedVideoPicker()
        }

        binding.addPhotoBtn.setOnClickListener {
            // This listener is overridden in onActivityCreated; kept minimal here.
            btnStateToggle(binding.videoBtn, false)
            onClickedPhotoPick()
        }

        binding.postBtn.setOnClickListener {
            viewModel.postBody = binding.postBody
            validateData()
        }

        setupAudioControls()
    }

    private fun setupAudioControls() {
        binding.audioRecord.setOnLongClickListener {
            if (::mAudioRecorder.isInitialized) {
                checkAudioPermissionThenRecord()
            }
            true
        }
        binding.audioRecord.setOnClickListener {
            Toast.makeText(requireContext(),
                "Press and hold to record audio", Toast.LENGTH_SHORT).show()
        }

        // FIXED: only stop when we actually started recording
        binding.audioRecord.setOnTouchListener { v, event ->
            v.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP && isRecording) {
                isRecording = false
                setSmallMic()
                try {
                    mAudioRecorder.stopRecord()
                    prepareAudioPlayer()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping recorder", e)
                }
            }
            false
        }

        binding.playButton.setOnClickListener {
            if (audioPlayer.isPlaying) {
                audioPlayer.pause()
                countDownTimer?.cancel()
                binding.playButton.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_play_arrow_24)
                )
            } else {
                audioPlayer.start()
                binding.playButton.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_pause_24)
                )
                seekbarUpdate((audioPlayer.duration - audioPlayer.currentPosition).toLong())
            }
        }

        binding.musicProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioPlayer.seekTo(progress)
                    seekbarUpdate((audioPlayer.duration - audioPlayer.currentPosition).toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        binding.closeVideo.setOnClickListener { hideVideoLayout() }
        binding.closeAudio.setOnClickListener { hideAudioLayout() }
    }
    private fun handleDialogComponents() {
        if (restrictGroup) binding.arrowIv.visibility = View.GONE

        // FIXED: initialize mAudioRecorder here so it's ready before any touch
        try {
            mAudioRecorder = AudioRecorder.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecorder init failed", e)
        }
        setSmallMic()

        audioFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "${UUID.randomUUID()}.m4a"
        )

        player      = SimpleExoPlayer.Builder(requireContext()).build()
        audioPlayer = MediaPlayer()

        val coords = Coordinates(location?.longitude ?: 0.0, location?.latitude ?: 0.0)
        binding.postBody = PostBody(coords, isCheckin)

        if (placeDetails != null) {
            binding.placeTv.visibility = View.VISIBLE
            binding.pinIv.visibility   = View.VISIBLE
            binding.postBody?.placeKey = placeDetails!!.placeKey
            placeDetails!!.dominantGroup?.let { dg ->
                binding.placeNameTv.text      = dg.name
                binding.postBody?.groupKey    = dg.groupKey
                binding.group                 = dg
                downloadGroupPic(dg.imageName)
            } ?: run {
                binding.postBody?.groupKey = null
            }
            binding.place = placeDetails
        } else {
            binding.placeTv.visibility = View.GONE
            binding.pinIv.visibility   = View.GONE
        }

        group?.let { g ->
            binding.placeNameTv.text   = g.name
            binding.group              = g
            downloadGroupPic(g.imageName)
            binding.postBody?.groupKey = g.groupKey
            binding.postBody?.placeKey = null
        }
    }

    private fun checkAudioPermissionThenRecord() {
        val permissionUtil = PermissionUtil()
        if (permissionUtil.verifyPermissions(requireContext(), permissionUtil.audioPermissions)) {
            startAudioRecording()
        } else {
            audioPermission.launch(permissionUtil.audioPermissionsExt)
        }
    }

    private fun startAudioRecording() {
        audioFile?.takeIf { it.exists() }?.delete()
        hidePhotoSlider()
        hideVideoLayout()

        try {
            mAudioRecorder.prepareRecord(
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.OutputFormat.MPEG_4,
                MediaRecorder.AudioEncoder.AAC,
                audioFile
            )
            mAudioRecorder.setOnErrorListener { Log.e(TAG, "Recorder error: $it") }
            mAudioRecorder.startRecord()
            isRecording = true   // FIXED: set flag so ACTION_UP knows we're recording
            setLargeMic()
            btnStateToggle(binding.addPhotoBtn, false)
            btnStateToggle(binding.videoBtn, false)
        } catch (e: Exception) {
            isRecording = false
            Log.e(TAG, "Failed to start recording", e)
            Toast.makeText(requireContext(), "Could not start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prepareAudioPlayer() {
        try {
            stopAndReleaseAudioPlayer()
            audioPlayer = MediaPlayer()
            audioPlayer.setDataSource(audioFile!!.absolutePath)
            audioPlayer.prepare()   // synchronous — safe for local files

            val durationText = SimpleDateFormat("mm:ss", Locale.getDefault())
                .format(Date(audioPlayer.duration.toLong()))
            binding.duration.text        = durationText
            binding.musicProgress.max   = audioPlayer.duration
            binding.postBody?.type      = "AUDIO"
            showAudioLayout()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare audio player", e)
            Toast.makeText(requireContext(), "Could not play recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAndReleaseAudioPlayer() {
        if (::audioPlayer.isInitialized) {
            try {
                if (audioPlayer.isPlaying) audioPlayer.stop()
                audioPlayer.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing audioPlayer", e)
            }
        }
    }

    private fun seekbarUpdate(duration: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                try {
                    binding.musicProgress.progress =
                        (audioPlayer.duration - millisUntilFinished).toInt()
                    binding.duration.text = SimpleDateFormat("mm:ss", Locale.getDefault())
                        .format(Date(millisUntilFinished))
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Seekbar tick error", e)
                }
            }
            override fun onFinish() {
                binding.playButton.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_play_arrow_24)
                )
                binding.musicProgress.progress = binding.musicProgress.max
                try {
                    audioPlayer.seekTo(0)
                    audioPlayer.pause()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Seekbar finish error", e)
                }
            }
        }.start()
    }

    private fun setupObservers() {
        viewModel.postCreated.observe(viewLifecycleOwner) { post ->
            if (isCheckin) {
                Constants.lastCheckinPlace = post.place
                Constants.lastcheckInPolygonCoordinateKey = post.checkInPolygonCoordinateKey
                Constants.isSelfCheckIn = restrictPage
            }
            listener?.onPostCreate(post)
            dismiss()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.loading = it }
        viewModel.error.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.error.postValue("")
            }
        }
    }

    private fun validateData() {
        val hasMedia  = binding.addPhotoBtn.isSelected || binding.videoBtn.isSelected
        val hasText   = !binding.postBody?.details.isNullOrEmpty()
        val hasAudio  = binding.postBody?.type == "AUDIO"

        if (!hasMedia && !hasText && !hasAudio && !isCheckin) {
            Toast.makeText(requireContext(), "You cannot post nothing", Toast.LENGTH_SHORT).show()
            return
        }

        if (isCheckin) binding.postBody?.coordinates = binding.place?.coordinates
        binding.postBody?.checkin = isCheckin

        when (binding.postBody?.type) {
            "IMAGE" -> {
                binding.postBody?.subType = "IMAGE"
                viewModel.uploadPostImage(requireContext(), pickedUris, 0)
            }
            "VIDEO" -> {
                binding.postBody?.subType = "VIDEO"
                viewModel.uploadVideoPost(requireContext(), pickedUris[0])
            }
            "AUDIO" -> {
                binding.postBody?.subType = "AUDIO"
                audioFile?.let { viewModel.uploadAudioPost(it) }
            }
            else -> submitTextPost()
        }
    }

    private fun submitTextPost() {
        val details = binding.postBody?.details ?: ""
        val url = details.split("\\s".toRegex())
            .map { it.trim() }
            .firstOrNull { URLUtil.isValidUrl(it) }
            .orEmpty()

        if (url.isNotEmpty()) {
            URLEmbeddedTask { data ->
                Log.d(TAG, "URL preview found: $data")
                binding.postBody?.richLinkData = RichLinkData(
                    data.host, data.description ?: "", data.thumbnailURL, url, data.title
                )
                dispatchTextPost(details)
            }.execute(url)
        } else {
            dispatchTextPost(details)
        }
    }

    private fun dispatchTextPost(details: String) {
        binding.postBody?.type    = "TEXT"
        binding.postBody?.subType = "TEXT"
        if (details.isNotBlank()) {
            if (binding.postBody?.checkin == true) viewModel.createCheckinPost()
            else viewModel.createPost()
        } else {
            binding.postBody?.subType = ""
            viewModel.createCheckinPost()
        }
    }
    private fun showPhotoSlider() {
        binding.closeIv.visibility    = View.VISIBLE
        binding.photoSliderCl.visibility = View.VISIBLE
        binding.postEt.setLines(2)
        binding.audioRecord.visibility = View.GONE
        setupTabs(binding.tabLayout, binding.checkInViewpager)
    }

    private fun hidePhotoSlider() {
        binding.closeIv.visibility       = View.GONE
        binding.photoSliderCl.visibility = View.GONE
        photoSliderOpen = false
        btnStateToggle(binding.addPhotoBtn, false)
        binding.audioRecord.visibility   = View.VISIBLE
        binding.postEt.setLines(9)
        binding.postBody?.type = ""

    }

    private fun showVideoLayout() {
        binding.audioRecord.visibility = View.GONE
        btnStateToggle(binding.addPhotoBtn, false)
        btnStateToggle(binding.videoBtn, true)
        binding.closeVideo.visibility  = View.VISIBLE
        binding.videoView.visibility   = View.VISIBLE
    }

    private fun hideVideoLayout() {
        binding.postEt.setLines(9)
        btnStateToggle(binding.videoBtn, false)
        binding.closeVideo.visibility  = View.GONE
        binding.videoView.visibility   = View.GONE
        binding.audioRecord.visibility = View.VISIBLE
        binding.postBody?.type = ""
    }

    private fun showAudioLayout() {
        binding.audioRecord.visibility = View.GONE
        btnStateToggle(binding.addPhotoBtn, false)
        btnStateToggle(binding.videoBtn, false)
        binding.closeAudio.visibility  = View.VISIBLE
        binding.audioLayput.visibility = View.VISIBLE
    }

    private fun hideAudioLayout() {
        binding.audioRecord.visibility = View.VISIBLE
        audioFile?.takeIf { it.exists() }?.deleteRecursively()
        binding.postEt.setLines(9)
        binding.closeAudio.visibility  = View.GONE
        binding.audioLayput.visibility = View.GONE
        binding.postBody?.type = ""
    }

    private fun hideAudioLayoutFull() {
        binding.audioRecord.visibility = View.GONE
        audioFile?.takeIf { it.exists() }?.deleteRecursively()
        binding.postEt.setLines(9)
        binding.closeAudio.visibility  = View.GONE
        binding.audioLayput.visibility = View.GONE
    }

    private fun setLargeMic() {
        binding.audioRecord.layoutParams = binding.audioRecord.layoutParams.apply {
            height = 180; width = 180
        }
        binding.audioRecord.requestLayout()
        binding.audioRecord.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.black)
        binding.audioRecord.imageTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.white)
        binding.audioRecord.customSize = 180
    }

    private fun setSmallMic() {
        binding.audioRecord.layoutParams = binding.audioRecord.layoutParams.apply {
            height = 100; width = 100
        }
        binding.audioRecord.requestLayout()
        binding.audioRecord.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.white)
        binding.audioRecord.imageTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.black)
        binding.audioRecord.customSize = 100
    }

    private fun toggleDropdown() {
        if (restrictGroup) return
        if (!arrowOpen) {
            binding.audioRecord.visibility   = View.GONE
            binding.arrowIv.setImageResource(R.drawable.ic_up_arrow)
            binding.searchPostAsCl.visibility = View.VISIBLE
            binding.selectUserCl.elevation    = 2f
        } else {
            if (binding.postBody?.type.isNullOrEmpty()) {
                binding.audioRecord.visibility = View.VISIBLE
            }
            binding.arrowIv.setImageResource(R.drawable.ic_down_arrow)
            binding.searchPostAsCl.visibility = View.GONE
            binding.selectUserCl.elevation    = 0f
        }
        arrowOpen = !arrowOpen
    }
    private fun onClickedPhotoPick() {
        picker.pickMultipleImage { imageUris ->
            for (uri in imageUris) {
                uri.path?.let { pickedImages.add(it); pickedUris.add(uri) }
            }
            imageSliderAdapter = ImageSliderAdapterLocal(requireContext(), pickedImages)
            binding.checkInViewpager.adapter = imageSliderAdapter
            showPhotoSlider()
            hideVideoLayout()
            hideAudioLayoutFull()
            btnStateToggle(binding.addPhotoBtn, true)
            binding.postBody?.type = "IMAGE"
        }
    }

    private fun onClickedVideoPicker() {
        picker.pickVideo({ videoUri ->
            toggleCompressionView(false)
            hidePhotoSlider()
            hideAudioLayout()
            btnStateToggle(binding.videoBtn, true)
            pickedUris.add(videoUri)
            binding.postEt.setLines(2)
            val mediaItem = MediaItem.fromUri(videoUri)
            binding.videoView.player = player
            player.setMediaItem(mediaItem)
            player.prepare()
            binding.postBody?.type = "VIDEO"
            showVideoLayout()
        }, object : CompressionListener {
            override fun onCancelled(index: Int) { toggleCompressionView(false) }
            override fun onFailure(index: Int, failureMessage: String) { toggleCompressionView(false) }
            override fun onStart(index: Int) { toggleCompressionView(true) }
            override fun onSuccess(index: Int, size: Long, path: String?) {}
            override fun onProgress(index: Int, percent: Float) {
                view?.post {
                    binding.compressionProgressBar.progress    = percent.toInt()
                    binding.compressionProgressTextView.text   = "${percent.toInt()}%"
                }
            }
        })
    }

    private fun toggleCompressionView(isCompressing: Boolean) {
        view?.post {
            binding.compressionProgressBar.progress  = 0
            binding.compressionProgressTextView.text = "0%"
            binding.videoCompressionContainer.isVisible = isCompressing
            binding.mediaBgCl.isVisible              = !isCompressing
            binding.postEt.isVisible                 = !isCompressing
            binding.audioRecord.isVisible            = !isCompressing
        }
    }

    private fun isIn1km(): Boolean {
        return try {
            val results = FloatArray(1)
            Location.distanceBetween(
                location!!.latitude, location!!.longitude,
                placeDetails!!.coordinates!!.latitude, placeDetails!!.coordinates!!.longitude,
                results
            )
            results[0] < 1000
        } catch (e: Exception) { false }
    }

    private fun setupPostAsUser(user: UserInfo) {
        ViewUtils.loadUserProfilePic(requireContext(), binding.profileImage, user.imageName, user.providerImageUrl)
        ViewUtils.loadUserProfilePic(requireContext(), binding.imageIv, user.imageName, user.providerImageUrl)
        binding.imageIv.borderWidth   = 0
        binding.profileImage.borderWidth = 0
        binding.userBox.visibility    = View.VISIBLE
        binding.postBody?.groupKey    = null
        binding.postBody?.placeKey    = null
        val name = PrefManager(requireContext()).getUserName()?.takeIf { it.isNotEmpty() } ?: "User"
        binding.placeNameTv.text = name
        binding.groupNameTv.text = name
    }

    private fun downloadGroupPic(imageName: String) {
        ViewUtils.loadGroupPhoto(requireContext(), binding.imageIv, imageName)
    }

    private fun showCheckInPlaceSheet() {
        val loc = location ?: run {
            Toast.makeText(requireContext(), "Location not available. Please enable location permissions.", Toast.LENGTH_SHORT).show()
            return
        }
        val fragment = SelectCheckInPlaceDialogFragment(this, loc)
        fragment.show(requireActivity().supportFragmentManager, CheckInCreatePlaceDialogFragment::class.java.simpleName)
    }

    private lateinit var searchPlaceDialogFragment: SearchPlaceDialogFragment
    private fun showSearchPlaceSheet(query: String) {
        val fm = requireActivity().supportFragmentManager
        if (fm.findFragmentByTag(SearchPlaceDialogFragment::class.java.simpleName) == null) {
            searchPlaceDialogFragment = SearchPlaceDialogFragment(this, query)
            searchPlaceDialogFragment.show(fm, SearchPlaceDialogFragment::class.java.simpleName)
        }
    }

    fun setupTabs(tabLayout: TabLayout, viewPager: ViewPager) {
        tabLayout.removeAllTabs()
        repeat(pickedImages.size) { tabLayout.addTab(tabLayout.newTab().setIcon(null)) }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(p: Int, offset: Float, px: Int) {}
            override fun onPageSelected(position: Int) { tabLayout.selectTab(tabLayout.getTabAt(position)) }
            override fun onPageScrollStateChanged(state: Int) {}
        })
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { viewPager.currentItem = tab.position }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        tabLayout.getTabAt(0)?.select()
    }

    private fun btnStateToggle(btn: Button, selectedState: Boolean, enable: Boolean = true) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.black)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.hint_color)
        val disabledColor = ContextCompat.getColor(requireContext(), R.color.likes_color)

        if (!enable) {
            btn.isSelected = false
            btn.isEnabled  = false
            btn.setTextColor(disabledColor)
            btn.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(disabledColor)
            changeStrokeColor(binding.mediaBgCl, disabledColor)
            changeViewColor(binding.separator1View, disabledColor)
            changeViewColor(binding.separator2View, disabledColor)
            return
        }

        btn.isSelected = selectedState
        btn.isEnabled  = true
        val color = if (selectedState) activeColor else inactiveColor
        btn.setTextColor(color)
        btn.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(color)
        changeStrokeColor(binding.mediaBgCl, inactiveColor)
        changeViewColor(binding.separator1View, inactiveColor)
        changeViewColor(binding.separator2View, inactiveColor)
    }


    override val layoutId: Int get() = R.layout.fragment_create_post
    override val pageTitle: String? get() = ""

    override fun onItemClicked(position: Int) {
        binding.imageIv.borderWidth   = 5
        binding.group                 = groupList[position]
        binding.placeNameTv.text      = groupList[position].name
        binding.postBody?.groupKey    = groupList[position].groupKey
        if (!isCheckin) binding.postBody?.placeKey = null
        ViewUtils.loadGroupPhoto(requireContext(), binding.imageIv, groupList[position].imageName)
        toggleDropdown()
    }

    override fun onGroupSearchClick(groupDetails: GroupDetails) {
        binding.group              = groupDetails
        binding.placeNameTv.text   = groupDetails.name
        binding.postBody?.groupKey = groupDetails.groupKey
        if (!isCheckin) binding.postBody?.placeKey = null
        ViewUtils.loadGroupPhoto(requireContext(), binding.imageIv, groupDetails.imageName)
        toggleDropdown()
    }

    override fun onPlaceSelect(placeDetails: PlaceDetails) {
        isCheckin = true
        binding.placeTv.visibility = View.VISIBLE
        binding.pinIv.visibility   = View.VISIBLE
        binding.postBody?.placeKey = placeDetails.placeKey
        placeDetails.dominantGroup?.let { dg ->
            binding.placeNameTv.text   = dg.name
            binding.postBody?.groupKey = dg.groupKey
            binding.group              = dg
            downloadGroupPic(dg.imageName)
        }
        binding.place = placeDetails
    }
}