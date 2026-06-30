package com.lassi.presentation.cropper

import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lassi.R
import com.lassi.common.utils.FilePickerUtils
import com.lassi.common.utils.KeyUtils
import com.lassi.common.utils.Logger
import com.lassi.data.media.MiMedia
import com.lassi.databinding.CropImageActivityBinding
import com.lassi.domain.media.LassiConfig
import com.yalantis.ucrop.UCrop
import java.io.File

/**
 * Built-in activity for image cropping.
 * Delegates all crop UI to UCrop (com.github.yalantis:ucrop:2.2.10).
 *
 * Required in AndroidManifest.xml inside <application>:
 *   <activity
 *       android:name="com.yalantis.ucrop.UCropActivity"
 *       android:theme="@style/Theme.AppCompat.Light.NoActionBar"/>

 */
open class CropImageActivity : AppCompatActivity() {

    private lateinit var binding: CropImageActivityBinding

    private val logTag = "LassiCropImageActivity"

    /** URI of the image to be cropped, received via Intent extras. */
    private var cropImageUri: Uri? = null

    /** Options passed in via the intent bundle (kept for toolbar theming). */
    private var cropImageOptions: CropImageOptions? = null

    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CropImageActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = ""
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { setResultCancel() }

        // Read the URI and options forwarded by the picker
        val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)

        cropImageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle?.getParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle?.getParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
        }

        cropImageOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle?.getParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS, CropImageOptions::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle?.getParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS)
        }

        applyToolbarTheme()

        if (savedInstanceState == null) {
            val uri = cropImageUri
            if (uri == null || uri == Uri.EMPTY) {
                // No URI supplied — open the gallery picker
                openGallery()
            } else {
                // URI already provided — go straight to UCrop
                startUCrop(uri)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun startUCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))

        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(500, 500)
            .withOptions(buildUCropOptions())
            .start(this)
    }

    private fun buildUCropOptions(): UCrop.Options = UCrop.Options().apply {
        setFreeStyleCropEnabled(false)        // keep it locked to circle/square
        setHideBottomControls(false)
        setCircleDimmedLayer(true)            // renders the oval/circle overlay
        setShowCropGrid(true)

        with(LassiConfig.getConfig()) {
            setToolbarColor(toolbarColor)
            //setStatusBarColor(statusBarColor)
            setToolbarWidgetColor(toolbarResourceColor)
            setActiveControlsWidgetColor(toolbarResourceColor)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        when {
            requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK -> {
                val sourceUri = data?.data
                if (sourceUri != null) {
                    cropImageUri = sourceUri
                    startUCrop(sourceUri)
                } else {
                    Toast.makeText(this, R.string.crop_image_activity_no_permissions, Toast.LENGTH_SHORT).show()
                    setResultCancel()
                }
            }

            requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_CANCELED -> {
                setResultCancel()
            }

            requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK -> {
                val resultUri = data?.let { UCrop.getOutput(it) }
                if (resultUri != null) {
                    deliverResult(resultUri)
                } else {
                    Log.e(logTag, "UCrop returned null output URI")
                    setResultCancel()
                }
            }

            requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_CANCELED -> {
                setResultCancel()
            }

            requestCode == UCrop.RESULT_ERROR -> {
                val error = data?.let { UCrop.getError(it) }
                Log.e(logTag, "UCrop error: $error")
                Toast.makeText(this, error?.message ?: getString(R.string.crop_image_activity_no_permissions), Toast.LENGTH_SHORT).show()
                setResultCancel()
            }
        }
    }

    /**
     * Notifies the MediaStore about the new file so it gets a proper ID,
     * then returns the [MiMedia] result to the calling activity.
     */
    private fun deliverResult(uri: Uri) {
        uri.path?.let { path ->
            FilePickerUtils.notifyGalleryUpdateNewFile(
                this, path,
                onFileScanComplete = { scannedUri, imagePath ->
                    onFileScanComplete(scannedUri, imagePath)
                }
            )
        } ?: run {
            returnMedia(MiMedia(path = uri.toString(), doesUri = true))
        }
    }

    private fun onFileScanComplete(uri: Uri?, imagePath: String?) {
        uri?.let { returnUri ->
            contentResolver.query(returnUri, null, null, null, null)
        }?.use { cursor ->
            if (!cursor.moveToFirst()) return@use
            try {
                val idIndex   = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                if (idIndex == -1 || nameIndex == -1) return@use

                val id   = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex)
                val path = if (dataIndex != -1) cursor.getString(dataIndex) else imagePath

                if (path == null) {
                    Logger.e(logTag, "Could not resolve image path")
                    setResultCancel()
                    return@use
                }

                returnMedia(MiMedia(id, name, path, 0))
            } catch (e: Exception) {
                Logger.e(logTag, "onFileScanComplete error: $e")
                setResultCancel()
            }
        } ?: run {
            imagePath?.let {
                returnMedia(MiMedia(path = it, doesUri = false))
            } ?: setResultCancel()
        }
    }

    private fun returnMedia(media: MiMedia) {
        val intent = Intent().apply { putExtra(KeyUtils.MEDIA_PREVIEW, media) }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun setResultCancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun applyToolbarTheme() {
        with(LassiConfig.getConfig()) {
            binding.toolbar.background = ColorDrawable(toolbarColor)
            binding.toolbar.setTitleTextColor(toolbarResourceColor)

            val upArrow = ContextCompat.getDrawable(this@CropImageActivity, R.drawable.ic_back_white)
            upArrow?.setColorFilter(toolbarResourceColor, PorterDuff.Mode.SRC_ATOP)
            supportActionBar?.setHomeAsUpIndicator(upArrow)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = statusBarColor
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
        setResultCancel()
    }
}