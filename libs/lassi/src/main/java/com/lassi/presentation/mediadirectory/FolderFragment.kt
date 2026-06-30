package com.lassi.presentation.mediadirectory

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.lassi.R
import com.lassi.common.extenstions.hide
import com.lassi.common.extenstions.safeObserve
import com.lassi.common.extenstions.show
import com.lassi.data.common.Response
import com.lassi.data.media.MiItemMedia
import com.lassi.databinding.FragmentMediaPickerBinding
import com.lassi.domain.media.LassiConfig
import com.lassi.domain.media.LassiOption
import com.lassi.domain.media.MediaType
import com.lassi.presentation.common.LassiBaseViewModelFragment
import com.lassi.presentation.common.decoration.GridSpacingItemDecoration
import com.lassi.presentation.media.MediaFragment
import com.lassi.presentation.mediadirectory.adapter.FolderAdapter

class FolderFragment : LassiBaseViewModelFragment<FolderViewModel>() {

    private var _binding: FragmentMediaPickerBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = FolderFragment()
    }

    private var needsStorage = true

    private val photoPermission = mutableListOf(Manifest.permission.READ_MEDIA_IMAGES)
    private val vidPermission = mutableListOf(Manifest.permission.READ_MEDIA_VIDEO)
    private val audioPermission = mutableListOf(Manifest.permission.READ_MEDIA_AUDIO)

    private val permissionSettingResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            requestPermission()
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
            if (map.entries.all { it.value }) {
                viewModel.checkInsert()
            } else {
                showPermissionDisableAlert()
            }
        }

    private val folderAdapter by lazy { FolderAdapter(this::onItemClick) }

    override fun getContentResource() = R.layout.fragment_media_picker

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMediaPickerBinding.bind(view)

        val config = LassiConfig.getConfig()
        binding.rvMedia.setBackgroundColor(config.galleryBackgroundColor)
        binding.rvMedia.layoutManager = GridLayoutManager(context, config.gridSize)
        binding.rvMedia.adapter = folderAdapter
        binding.rvMedia.addItemDecoration(GridSpacingItemDecoration(config.gridSize, 10))
        binding.progressBar.indeterminateDrawable.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                config.progressBarColor,
                BlendModeCompat.SRC_ATOP
            )
        requestPermission()


        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun buildViewModel(): FolderViewModel {
        return ViewModelProvider(
            requireActivity(),
            FolderViewModelFactory(requireActivity())
        )[FolderViewModel::class.java]
    }

    override fun initLiveDataObservers() {
        super.initLiveDataObservers()

        viewModel.fetchMediaFolderLiveData.safeObserve(viewLifecycleOwner) { response ->
            when (response) {
                is Response.Loading -> {
                    binding.tvNoDataFound.visibility = View.GONE
                    binding.progressBar.show()
                }
                is Response.Success -> {}
                is Response.Error -> {
                    binding.progressBar.hide()
                    response.throwable.printStackTrace()
                }
            }
        }

        viewModel.getMediaItemList().observe(viewLifecycleOwner) {
            binding.progressBar.hide()
            if (!it.isNullOrEmpty()) {
                folderAdapter.setList(it)
            }
        }

        viewModel.emptyList.observe(viewLifecycleOwner) {
            binding.tvNoDataFound.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val config = LassiConfig.getConfig()
        menu.findItem(R.id.menuCamera)?.isVisible =
            (config.mediaType == MediaType.IMAGE || config.mediaType == MediaType.VIDEO) &&
                    (config.lassiOption == LassiOption.CAMERA_AND_GALLERY ||
                            config.lassiOption == LassiOption.CAMERA)
        super.onPrepareOptionsMenu(menu)
    }

    private fun requestPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when (LassiConfig.getConfig().mediaType) {
                    MediaType.IMAGE -> {
                        needsStorage = needsStorage && ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.READ_MEDIA_IMAGES
                        ) != PackageManager.PERMISSION_GRANTED
                        requestPermissionLauncher.launch(photoPermission.toTypedArray())
                    }
                    MediaType.VIDEO -> {
                        needsStorage = needsStorage && ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.READ_MEDIA_VIDEO
                        ) != PackageManager.PERMISSION_GRANTED
                        requestPermissionLauncher.launch(vidPermission.toTypedArray())
                    }
                    MediaType.AUDIO -> {
                        needsStorage = needsStorage && ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.READ_MEDIA_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED
                        requestPermissionLauncher.launch(audioPermission.toTypedArray())
                    }
                    else -> {}
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun showPermissionDisableAlert() {
        val msg = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            getString(R.string.storage_permission_rational)
        } else {
            when (LassiConfig.getConfig().mediaType) {
                MediaType.IMAGE, MediaType.VIDEO ->
                    getString(R.string.read_media_images_video_permission_rational)
                MediaType.AUDIO ->
                    getString(R.string.read_media_audio_permission_rational)
                else -> return
            }
        }
        showPermissionAlert(msg)
    }

    private fun showPermissionAlert(msg: String) {
        AlertDialog.Builder(requireContext(), R.style.dialogTheme)
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity?.packageName, null)
                }
                permissionSettingResult.launch(intent)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .create()
            .also { it.setCancelable(false) }
            .show()
    }

    private fun onItemClick(bucket: MiItemMedia) {
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.setCustomAnimations(
                R.anim.right_in, R.anim.right_out,
                R.anim.right_in, R.anim.right_out
            )
            ?.add(R.id.ftContainer, MediaFragment.getInstance(bucket))
            ?.addToBackStack(MediaFragment::class.java.simpleName)
            ?.commitAllowingStateLoss()
    }
}