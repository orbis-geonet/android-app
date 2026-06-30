package com.lassi.presentation.media

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.lassi.R
import com.lassi.common.utils.CropUtils
import com.lassi.common.utils.KeyUtils
import com.lassi.common.utils.KeyUtils.SELECTED_FOLDER
import com.lassi.common.utils.Logger
import com.lassi.data.common.Response
import com.lassi.data.media.MiItemMedia
import com.lassi.data.media.MiMedia
import com.lassi.databinding.FragmentMediaPickerBinding
import com.lassi.domain.common.SafeObserver
import com.lassi.domain.media.LassiConfig
import com.lassi.domain.media.MediaType
import com.lassi.presentation.common.LassiBaseViewModelFragment
import com.lassi.presentation.common.decoration.GridSpacingItemDecoration
import com.lassi.presentation.media.adapter.MediaAdapter
import com.lassi.presentation.mediadirectory.SelectedMediaViewModelFactory
import com.lassi.presentation.videopreview.VideoPreviewActivity
import java.io.File

class MediaFragment : LassiBaseViewModelFragment<SelectedMediaViewModel>() {

    private var _binding: FragmentMediaPickerBinding? = null
    private val binding get() = _binding!!

    private val mediaAdapter by lazy { MediaAdapter(this::onItemClick) }
    private var bucket: MiItemMedia? = null
    private val mediaPickerConfig = LassiConfig.getConfig()

    // The base class inflates the layout via getContentResource() — do NOT override onCreateView.
    override fun getContentResource() = R.layout.fragment_media_picker

    companion object {
        fun getInstance(bucket: MiItemMedia): MediaFragment {
            return MediaFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(SELECTED_FOLDER, bucket)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Bind FIRST, then set up views, then call super (which triggers initViews/initLiveDataObservers)
        _binding = FragmentMediaPickerBinding.bind(view)

        binding.rvMedia.setBackgroundColor(mediaPickerConfig.galleryBackgroundColor)
        binding.progressBar.indeterminateDrawable.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                mediaPickerConfig.progressBarColor,
                BlendModeCompat.SRC_ATOP
            )
        bucket?.bucketName?.let { bucketName ->
            viewModel.getSelectedMediaData(bucket = bucketName)
        }


        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getBundle() {
        super.getBundle()
        @Suppress("DEPRECATION")
        bucket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(SELECTED_FOLDER, MiItemMedia::class.java)
        } else {
            arguments?.getParcelable(SELECTED_FOLDER)
        }
    }

    override fun buildViewModel(): SelectedMediaViewModel {
        return ViewModelProvider(
            requireActivity(),
            SelectedMediaViewModelFactory(requireActivity())
        )[SelectedMediaViewModel::class.java]
    }

    // initViews is called by the base class — binding is not available here.
    // All view setup has been moved into onViewCreated above.
    override fun initViews() {
        super.initViews()
    }

    override fun initLiveDataObservers() {
        super.initLiveDataObservers()
        viewModel.fetchedMediaLiveData.observe(
            viewLifecycleOwner,
            SafeObserver(::handleFetchedData)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menuCamera)?.isVisible = false
        super.onPrepareOptionsMenu(menu)
    }

    private fun handleFetchedData(response: Response<ArrayList<MiMedia>>?) {
        binding.rvMedia.layoutManager = GridLayoutManager(context, mediaPickerConfig.gridSize)
        binding.rvMedia.adapter = mediaAdapter
        binding.rvMedia.addItemDecoration(GridSpacingItemDecoration(mediaPickerConfig.gridSize, 10))

        when (response) {
            is Response.Success -> {
                Logger.d("MediaFragment", "handleFetchedData SUCCESS size -> ${response.item.size}")
                mediaAdapter.setList(response.item)
            }
            is Response.Error -> {
                Logger.d("MediaFragment", "handleFetchedData ERROR")
            }
            else -> {}
        }
    }

    private fun onItemClick(selectedMedias: ArrayList<MiMedia>) {
        val config = LassiConfig.getConfig()
        when (config.mediaType) {
            MediaType.IMAGE -> {
                when {
                    config.maxCount == 1 && config.isCrop -> {
                        val uri = Uri.fromFile(selectedMedias[0].path?.let { File(it) })
                        CropUtils.beginCrop(requireActivity(), uri)
                    }
                    config.maxCount > 1 -> {
                        viewModel.addAllSelectedMedia(selectedMedias)
                    }
                    else -> {
                        viewModel.addAllSelectedMedia(selectedMedias)
                        setResultOk(selectedMedias)
                    }
                }
            }
            MediaType.VIDEO, MediaType.AUDIO, MediaType.DOC -> {
                if (config.maxCount > 1) {
                    viewModel.addAllSelectedMedia(selectedMedias)
                } else {
                    VideoPreviewActivity.startVideoPreview(activity, selectedMedias[0].path!!)
                }
            }
            else -> {}
        }
    }
    private fun setResultOk(selectedMedia: ArrayList<MiMedia>?) {
        val intent = Intent().apply {
            putExtra(KeyUtils.SELECTED_MEDIA, selectedMedia)
        }
        activity?.setResult(Activity.RESULT_OK, intent)
        activity?.finish()
    }
}