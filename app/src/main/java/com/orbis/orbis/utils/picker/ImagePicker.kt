package com.orbis.orbis.utils.picker

import android.Manifest.permission.READ_MEDIA_AUDIO
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.lassi.common.utils.KeyUtils
import com.lassi.data.media.MiMedia
import com.lassi.domain.media.LassiOption
import com.lassi.domain.media.MediaType
import com.lassi.presentation.builder.Lassi
import com.lassi.presentation.cropper.CropImageView
import com.orbis.orbis.R
import com.orbis.orbis.extensions.realUri
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.AppSpecificStorageConfiguration
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.orbis.orbis.utils.ProgressDialog

class Picker
{

    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    private var checkAudioPermission: ActivityResultLauncher<Array<String>>? = null

    private var singleCompletion: ((Uri, Bitmap, Int) -> Unit)? = null
    private var multipleCompletion: ((List<Uri>) -> Unit)? = null
    private var compressionListener: CompressionListener? = null

    private var videoCompletion: ((Uri) -> Unit)? = null
    private var isMultiplePicking = false
    private var isVideo = false
    private var hasToCrop = true
    private var tag = 0
    private var activity: AppCompatActivity? = null
    private var fragment: Fragment? = null

    fun populate(activity: AppCompatActivity? = null, fragment: Fragment? = null)
    {
        this.activity = activity
        this.fragment = fragment
        val context = (activity ?: fragment!!.requireActivity())

        if(activity == null && fragment == null) { return }
        activityResultLauncher = (activity ?: fragment!!).registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK)
            {
                val selectedMedia = result.data?.getSerializableExtra(KeyUtils.SELECTED_MEDIA) as ArrayList<MiMedia>

                if (!selectedMedia.isNullOrEmpty())
                {
                    if(isMultiplePicking)
                    {
                        multipleCompletion?.invoke(selectedMedia.filter { x -> x.path != null }.map { x -> x.path!!.toUri().realUri() })
                    }
                    else if (isVideo)
                    {
                        selectedMedia[0].path?.let { videoUri ->
                            compressVideo(context, videoUri.toUri().realUri())
                        }
                    }
                    else
                    {
                        selectedMedia[0].path?.let { imageUri ->
                            Glide.with(activity ?: fragment!!.requireActivity())
                            .asBitmap()
                            .load(imageUri)
                            .apply(if (hasToCrop) RequestOptions.circleCropTransform() else RequestOptions.noTransformation())
                            .into(object : CustomTarget<Bitmap>()
                            {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?)
                                {
                                    singleCompletion?.invoke(imageUri.toUri().realUri(), resource, tag)
                                }
                                override fun onLoadCleared(placeholder: Drawable?) { }
                            })
                        }
                    }
                }
            }
        }
        checkAudioPermission = (activity ?: fragment!!).registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[READ_MEDIA_AUDIO] != true)
            {
                AlertDialog.Builder(context)
                    .setMessage(R.string.music_audio_permission)
                    .setPositiveButton("Settings") { dialog, which ->
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    fun pickImage(tag: Int = 0, crop: Boolean = true, singleCompletion: (Uri, Bitmap, Int) -> Unit)
    {
        if(activity == null && fragment == null) { return }

        this.singleCompletion = singleCompletion
        this.isMultiplePicking = false
        this.isVideo = false
        this.tag = tag

        val lassi = Lassi(activity ?: fragment!!.requireContext())
            .with(LassiOption.CAMERA_AND_GALLERY)
            .setMediaType(MediaType.IMAGE)
            .setCompressionRation(10)

        if(crop) {
            lassi.setCropType(CropImageView.CropShape.OVAL)
            //lassi.enableFlip()
            //lassi.enableRotate()
            lassi.setCropAspectRatio(1, 1)
            lassi.enableActualCircleCrop()
            hasToCrop = true
        }
        else{
            lassi.disableCrop()
            hasToCrop = false
        }

        showPopup(lassi)
    }

    fun pickMultipleImage(multipleCompletion: (List<Uri>) -> Unit)
    {
        if(activity == null && fragment == null) { return }

        this.multipleCompletion = multipleCompletion
        this.isMultiplePicking = true
        this.isVideo = false

        val lassi = Lassi(activity ?: fragment!!.requireContext())
            .with(LassiOption.CAMERA_AND_GALLERY)
            .setMediaType(MediaType.IMAGE)
            .setMaxCount(10)
            .setCompressionRation(10)

        showPopup(lassi)
    }

    fun pickVideo(videoCompletion: (Uri) -> Unit, compressionListener: CompressionListener)
    {
        if(activity == null && fragment == null) { return }

        this.videoCompletion = videoCompletion
        this.compressionListener = compressionListener
        this.isMultiplePicking = false
        this.isVideo = true

        val lassi = Lassi(activity ?: fragment!!.requireContext())
            .setMediaType(MediaType.VIDEO)
            .setMaxTime(20 * 60)

        showPopup(lassi, isVideo = true)
    }

    private fun showPopup(lassi: Lassi, isVideo: Boolean = false)
    {
        if (Build.VERSION.SDK_INT >= 33)
        {
            if(!showMediaPermission()) { return }
        }

        val selectSourceDialogFragment = SelectSourceDialogFragment().apply {
            onCameraClick = {
                lassi.with(LassiOption.CAMERA)
                activityResultLauncher?.launch(lassi.build())
            }
            onGalleryClick = {
                lassi.with(LassiOption.CAMERA_AND_GALLERY)
                activityResultLauncher?.launch(lassi.build())
            }
        }
        selectSourceDialogFragment.show(activity?.supportFragmentManager ?: fragment!!.requireActivity().supportFragmentManager, "SelectSourceDialogFragment")
    }

    @RequiresApi(33)
    private fun showMediaPermission() : Boolean
    {
        val context = (activity ?: fragment!!.requireActivity())

        if (ContextCompat.checkSelfPermission(context, READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
            checkAudioPermission?.launch(arrayOf(READ_MEDIA_AUDIO))
        else
            return true

        return false
    }

    private fun compressVideo(context: Activity, videoUri: Uri)
    {
        compressionListener!!.onStart(0)

        VideoCompressor.start(
                context = context,
                uris = listOf(videoUri),
                isStreamable = false,
                // OR AND NOT BOTH
                appSpecificStorageConfiguration = AppSpecificStorageConfiguration(
                    videoName = "compressed_video",
                ),
                configureWith = Configuration(
                    quality = VideoQuality.LOW,
                    disableAudio = false,
                    keepOriginalResolution = false,
                    isMinBitrateCheckEnabled = false
                ),
                listener = object : CompressionListener {
                    override fun onCancelled(index: Int)
                    {
                        compressionListener!!.onCancelled(index)
                    }

                    override fun onFailure(index: Int, failureMessage: String)
                    {
                        compressionListener!!.onFailure(index, failureMessage)
                    }

                    override fun onProgress(index: Int, percent: Float)
                    {
                        compressionListener!!.onProgress(index, percent)
                    }

                    override fun onStart(index: Int)
                    {

                    }

                    override fun onSuccess(index: Int, size: Long, path: String?)
                    {
                        context.runOnUiThread {
                            if (path != null)
                            {
                                videoCompletion?.invoke(path.toUri().realUri())
                            }
                        }
                        compressionListener!!.onSuccess(index, size, path)
                    }
                }
            )
        }
}