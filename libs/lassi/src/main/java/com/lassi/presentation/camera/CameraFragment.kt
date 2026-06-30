package com.lassi.presentation.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.lassi.R
import com.lassi.common.extenstions.hide
import com.lassi.common.extenstions.invisible
import com.lassi.common.extenstions.show
import com.lassi.common.utils.CropUtils
import com.lassi.common.utils.KeyUtils
import com.lassi.common.utils.ToastUtils
import com.lassi.data.common.VideoRecord
import com.lassi.data.media.MiMedia
import com.lassi.databinding.ActivityCameraBinding
import com.lassi.domain.common.SafeObserver
import com.lassi.domain.media.LassiConfig
import com.lassi.domain.media.MediaType
import com.lassi.presentation.cameraview.audio.Audio
import com.lassi.presentation.cameraview.audio.Flash
import com.lassi.presentation.cameraview.audio.Mode
import com.lassi.presentation.cameraview.controls.CameraListener
import com.lassi.presentation.cameraview.controls.CameraOptions
import com.lassi.presentation.cameraview.controls.PictureResult
import com.lassi.presentation.cameraview.controls.VideoResult
import com.lassi.presentation.common.LassiBaseViewModelFragment
import com.lassi.presentation.videopreview.VideoPreviewActivity
import java.io.File

class CameraFragment : LassiBaseViewModelFragment<CameraViewModel>(), View.OnClickListener {

    // ViewBinding: the base class inflates the layout via getContentResource(),
    // so we bind to the already-inflated view in onViewCreated using .bind(view).
    private var _binding: ActivityCameraBinding? = null
    private val binding get() = _binding!!

    private val cameraMode: Mode
        get() = if (LassiConfig.getConfig().mediaType == MediaType.VIDEO) Mode.VIDEO else Mode.PICTURE

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val allGranted = grants.values.all { it }
            if (allGranted && !binding.cameraView.isOpened) {
                binding.cameraView.open()
            } else {
                showPermissionDisableAlert()
            }
        }

    private val permissionSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            initCamera()
        }

    override fun buildViewModel(): CameraViewModel =
        ViewModelProvider(this)[CameraViewModel::class.java]

    // The base class calls this to inflate the layout — do NOT override onCreateView.
    override fun getContentResource() = R.layout.activity_camera

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ActivityCameraBinding.bind(view)

        with(binding) {
            ivCaptureImage.setOnClickListener(this@CameraFragment)
            ivFlipCamera.setOnClickListener(this@CameraFragment)
            ivFlash.setOnClickListener(this@CameraFragment)
            cameraView.setLifecycleOwner(this@CameraFragment)
            cameraView.addCameraListener(object : CameraListener() {
                override fun onCameraOpened(options: CameraOptions) {
                    cameraView.mode = cameraMode
                }

                override fun onPictureTaken(result: PictureResult) {
                    super.onPictureTaken(result)
                    viewModel.onPictureTaken(result.data)
                }

                override fun onVideoTaken(video: VideoResult) {
                    super.onVideoTaken(video)
                    stopVideoRecording()
                    VideoPreviewActivity.startVideoPreview(activity, video.file.absolutePath)
                }
            })
        }

        initCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        if (checkPermissions(binding.cameraView.audio)) {
            binding.cameraView.open()
        }
    }

    override fun onStop() {
        super.onStop()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun initViews() {
        super.initViews()
    }

    override fun initLiveDataObservers() {
        super.initLiveDataObservers()

        viewModel.startVideoRecord.observe(this, SafeObserver(this::handleVideoRecord))

        viewModel.cropImageLiveData.observe(this, SafeObserver { uri ->
            val config = LassiConfig.getConfig()
            if (config.isCrop && config.maxCount <= 1) {
                CropUtils.beginCrop(requireActivity(), uri)
            } else {
                val media = MiMedia().apply { path = uri.path }
                setResultOk(arrayListOf(media))
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_capture_image -> handleCaptureClick()
            R.id.iv_flip_camera   -> toggleCamera()
            R.id.iv_flash         -> handleFlashClick()
        }
    }

    private fun handleCaptureClick() {
        if (cameraMode == Mode.PICTURE) {
            if (binding.cameraView.isTakingPicture || binding.cameraView.isTakingVideo) return
            binding.cameraView.takePicture()
        } else {
            if (!binding.cameraView.isTakingVideo) {
                viewModel.startVideoRecording()
            } else {
                viewModel.stopVideoRecording()
            }
        }
    }

    private fun handleFlashClick() {
        val flashAvailable = requireContext()
            .packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        if (!flashAvailable) return

        if (cameraMode == Mode.PICTURE) {
            when (binding.cameraView.flash) {
                Flash.AUTO -> flashOn()
                Flash.ON   -> flashOff()
                else       -> flashAuto()
            }
        } else {
            when (binding.cameraView.flash) {
                Flash.OFF   -> flashTorch()
                Flash.TORCH -> flashOff()
                else        -> flashAuto()
            }
        }
    }

    private fun toggleCamera() {
        if (binding.cameraView.isTakingPicture || binding.cameraView.isTakingVideo) return
        binding.cameraView.toggleFacing()
    }

    private fun handleVideoRecord(videoRecord: VideoRecord<File>) {
        when (videoRecord) {
            is VideoRecord.Start -> startVideoRecording(videoRecord.item)
            is VideoRecord.Timer -> binding.tvTimer.text = videoRecord.item
            is VideoRecord.End   -> stopVideoRecording()
            is VideoRecord.Error -> showVideoError(videoRecord.minVideoTime)
        }
    }

    private fun startVideoRecording(videoFile: File) {
        binding.cameraView.takeVideo(videoFile)
        binding.ivFlipCamera.invisible()
        binding.tvTimer.show()
    }

    private fun stopVideoRecording() {
        if (binding.cameraView.isTakingVideo) {
            binding.cameraView.stopVideo()
            binding.ivFlipCamera.show()
            binding.tvTimer.hide()
        }
    }

    private fun showVideoError(minVideoTime: String) {
        ToastUtils.showToast(
            requireContext(),
            getString(R.string.min_video_recording_time_error, minVideoTime)
        )
    }

    private fun flashOn() {
        binding.cameraView.flash = Flash.ON
        binding.ivFlash.setImageResource(R.drawable.ic_flash_on_white)
    }

    private fun flashTorch() {
        binding.cameraView.flash = Flash.TORCH
        binding.ivFlash.setImageResource(R.drawable.ic_flash_on_white)
    }

    private fun flashOff() {
        binding.cameraView.flash = Flash.OFF
        binding.ivFlash.setImageResource(R.drawable.ic_flash_off_white)
    }

    private fun flashAuto() {
        binding.cameraView.flash = Flash.AUTO
        binding.ivFlash.setImageResource(R.drawable.ic_flash_auto_white)
    }

    private fun checkPermissions(audio: Audio): Boolean {
        binding.cameraView.checkPermissionsManifestOrThrow(audio)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        val ctx = requireContext()

        val cameraGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

        val storageGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED

        val audioGranted = if (LassiConfig.getConfig().mediaType == MediaType.VIDEO && audio == Audio.ON) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return cameraGranted && storageGranted && audioGranted
    }

    private fun requestForPermissions() {
        val ctx = requireContext()
        val permissions = mutableListOf(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val needsAudio = LassiConfig.getConfig().mediaType == MediaType.VIDEO
                && binding.cameraView.audio == Audio.ON
                && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED

        if (needsAudio) permissions.add(Manifest.permission.RECORD_AUDIO)

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun initCamera() {
        if (checkPermissions(binding.cameraView.audio)) {
            binding.cameraView.open()
        } else {
            requestForPermissions()
        }
    }

    private fun showPermissionDisableAlert() {
        val messageRes = when {
            LassiConfig.getConfig().mediaType == MediaType.VIDEO &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
                R.string.camera_audio_storage_permission_rational

            LassiConfig.getConfig().mediaType == MediaType.VIDEO ->
                R.string.camera_audio_permission_rational

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                R.string.camera_permission_rational

            else ->
                R.string.camera_storage_permission_rational
        }

        AlertDialog.Builder(requireContext(), R.style.dialogTheme)
            .setMessage(messageRes)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity?.packageName, null)
                }
                permissionSettingsLauncher.launch(intent)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .create()
            .also { it.setCancelable(false) }
            .show()
    }


    private fun setResultOk(selectedMedia: ArrayList<MiMedia>?) {
        val intent = Intent().putExtra(KeyUtils.SELECTED_MEDIA, selectedMedia)
        activity?.setResult(Activity.RESULT_OK, intent)
        activity?.finish()
    }
}