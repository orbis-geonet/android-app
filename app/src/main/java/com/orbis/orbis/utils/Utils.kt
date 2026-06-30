package com.orbis.orbis.utils

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.orbis.orbis.R
import com.orbis.orbis.base.BaseActivity
import com.orbis.orbis.models.Constants
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


/**
 * Webkul Software.
 *
 * Kotlin
 *
 * @author Webkul <support@webkul.com>
 * @category Webkul
 * @package com.webkul.mobikul
 * @copyright 2010-2018 Webkul Software Private Limited (https://webkul.com)
 * @license https://store.webkul.com/license.html ASL Licence
 * @link https://store.webkul.com/license.html
 */

class Utils {

    companion object {
        fun downloadProfilePicture(imageName: String?, iv: de.hdodenhof.circleimageview.CircleImageView) {
            if (imageName != null) {
                val storage =
                    Firebase.storage.getReference(
                        Constants.PROFILE_PICTURES + getImageUrl200(
                            imageName
                        )
                    )
                storage.downloadUrl.addOnSuccessListener {
                    Constants.userImage = it.toString()
                    Picasso.get().load(it).placeholder(R.drawable.ic_user).into(iv)
                }
            }
        }

        fun formatTime(millis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

            return when {
                hours == 0L && minutes <= 1L -> String.format(
                    "Online"
                )

                hours == 0L && minutes > 1L -> String.format(
                    "Online $minutes minutes ago"
                )

                else -> "Online $hours minutes ago"
            }
        }

        fun getPath(context: Context, uri: Uri): String? {
            val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }

                    // TODO handle non-primary volumes
                } else if (isDownloadsDocument(uri)) {
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                    return getDataColumn(context, contentUri, null, null)
                } else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(
                        split[1]
                    )
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                return getDataColumn(context, uri, null, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
            return null
        }

        /**
         * Get the value of the data column for this Uri. This is useful for
         * MediaStore Uris, and other file-based ContentProviders.
         *
         * @param context       The context.
         * @param uri           The Uri to query.
         * @param selection     (Optional) Filter used in the query.
         * @param selectionArgs (Optional) Selection arguments used in the query.
         * @return The value of the _data column, which is typically a file path.
         */
        fun getDataColumn(
            context: Context, uri: Uri?, selection: String?,
            selectionArgs: Array<String>?
        ): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(
                column
            )
            try {
                cursor = context.contentResolver.query(
                    uri!!, projection, selection, selectionArgs,
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val column_index: Int = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(column_index)
                }
            } finally {
                if (cursor != null) cursor.close()
            }
            return null
        }


        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }


        fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
            val bytes = ByteArrayOutputStream()
            inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(
                inContext.contentResolver,
                inImage,
                "Title",
                null
            )
            return Uri.parse(path)
        }

        fun getImageUrl200(imageName: String): String {
            var image_200x200 = imageName
            if (imageName.endsWith(".jpeg"))
                image_200x200 = imageName.substring(0, imageName.length - 5) + "_200x200.jpeg"
            else if (imageName.endsWith(".png"))
                image_200x200 = imageName.substring(0, imageName.length - 4) + "_200x200.png"
            else if (imageName.endsWith(".jpg"))
                image_200x200 = imageName.substring(0, imageName.length - 4) + "_200x200.jpg"
            else
                image_200x200 = imageName + "_200x200.jpeg"

            return image_200x200
        }

        fun getImageUrl400(imageName: String): String {
            var image_400x400 = imageName
            if (imageName.endsWith(".jpeg"))
                image_400x400 = imageName.substring(0, imageName.length - 5) + "_400x400.jpeg"
            else if (imageName.endsWith(".png"))
                image_400x400 = imageName.substring(0, imageName.length - 4) + "_400x400.png"
            else if (imageName.endsWith(".jpg"))
                image_400x400 = imageName.substring(0, imageName.length - 4) + "_400x400.jpg"
            else
                image_400x400 = imageName + "_400x400.jpeg"

            return image_400x400
        }

        fun getImageUrl680(imageName: String): String {
            var image_400x400 = imageName
            if (imageName.endsWith(".jpeg"))
                image_400x400 = imageName.substring(0, imageName.length - 5) + "_680x680.jpeg"
            else if (imageName.endsWith(".png"))
                image_400x400 = imageName.substring(0, imageName.length - 4) + "_680x680.png"
            else if (imageName.endsWith(".jpg"))
                image_400x400 = imageName.substring(0, imageName.length - 4) + "_680x680.jpg"
            else
                image_400x400 = imageName + "_680x680.jpeg"

            return image_400x400
        }

        fun populateRankIcon(context: Context, rankDiffType: String, statusIv: ImageView?) {
            if (rankDiffType == "FIRE") {
                statusIv?.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_fire))
            } else if (rankDiffType == "RISING") {
                statusIv?.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_arrow_up
                    )
                )
            } else if (rankDiffType == "FALLING") {
                statusIv?.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_arrow_down
                    )
                )
            } else if (rankDiffType == "LEVEL") {
                statusIv?.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_minus))
            } else if (rankDiffType == "LEVEL") {
                statusIv?.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_minus))
            } else {
                statusIv?.setImageDrawable(null)

            }
        }

        @JvmStatic
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels

        @JvmStatic
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val screenDensity = Resources.getSystem().displayMetrics.density

        fun disableUserInteraction(context: Context) {
            try {
                (context as BaseActivity).window.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
            } catch (e: Exception) {
            }
        }

        fun enableUserInteraction(context: Context) {
            try {
                (context as BaseActivity).window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            } catch (e: Exception) {
            }
        }


        fun showKeyboard(view: View) {
            view.requestFocus()
            if (!isHardKeyboardAvailable(view)) {
                val inputMethodManager =
                    view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(view, 0)
            }
        }

        private fun isHardKeyboardAvailable(view: View): Boolean {
            return view.context.resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS
        }

        fun hideKeyboard(activity: Activity) {
            try {
                val inputManager =
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (activity.currentFocus!!.windowToken != null)
                    inputManager.hideSoftInputFromWindow(
                        activity.currentFocus!!.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
            } catch (e: Exception) {
            }
        }

        fun hideKeyboard(view: View) {
            try {
                val inputManager =
                    view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (view.windowToken != null)
                    inputManager.hideSoftInputFromWindow(
                        view.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
            } catch (e: Exception) {
            }
        }

        fun getMd5String(stringToConvert: String): String {
            try {
                // Create MD5 Hash
                val digest = java.security.MessageDigest
                    .getInstance("MD5")
                digest.update(stringToConvert.toByteArray())
                val messageDigest = digest.digest()

                // Create Hex String
                val hexString = StringBuilder()
                for (aMessageDigest in messageDigest) {
                    var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
                    while (h.length < 2)
                        h = "0$h"
                    hexString.append(h)
                }
                return hexString.toString()

            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
            return ""
        }


        fun convertDpToPixel(dp: Float, context: Context): Float {
            val resources = context.resources
            val metrics = resources.displayMetrics
            return dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }

        fun isVoiceAvailable(context: Context): Boolean {
            return context.packageManager.queryIntentActivities(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                0
            ).size > 0
        }


        fun setSpinnerError(spinner: Spinner, errorString: String) {
            val selectedView = spinner.selectedView
            if (selectedView != null && selectedView is TextView) {
                spinner.requestFocus()
                selectedView.setTextColor(Color.RED)
                selectedView.text = errorString
                spinner.performClick()
            }
        }


        fun generateRandomPassword(): String {
            val random = SecureRandom()
            val letters = "abcdefghjklmnopqrstuvwxyzABCDEFGHJKMNOPQRSTUVWXYZ1234567890"
            val numbers = "1234567890"
            val specialChars = "!@#$%^&*_=+-/"
            var pw = ""
            for (i in 0..7) {
                val index = (random.nextDouble() * letters.length).toInt()
                pw += letters.substring(index, index + 1)
            }
            val indexA = (random.nextDouble() * numbers.length).toInt()
            pw += numbers.substring(indexA, indexA + 1)
            val indexB = (random.nextDouble() * specialChars.length).toInt()
            pw += specialChars.substring(indexB, indexB + 1)
            return pw
        }


        fun isValidEmailId(email: String): Boolean {
            return Pattern.compile(
                "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                        + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                        + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                        + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$"
            ).matcher(email).matches()
        }
    }
}