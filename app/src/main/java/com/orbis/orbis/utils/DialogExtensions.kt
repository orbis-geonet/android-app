package com.orbis.orbis.utils


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.orbis.orbis.R


inline fun Context.alert(title: CharSequence? = null, message: CharSequence? = null, func: AlertDialogHelper.() -> Unit): AlertDialog {
    return AlertDialogHelper(this, title, message).apply {
        func()
    }.create()
}

inline fun Context.alert(titleResource: Int = 0, messageResource: Int = 0, func: AlertDialogHelper.() -> Unit): AlertDialog {
    val title = if (titleResource == 0) null else getString(titleResource)
    val message = if (messageResource == 0) null else getString(messageResource)
    return AlertDialogHelper(this, title, message).apply {
        func()
    }.create()
}

@SuppressLint("InflateParams")
class AlertDialogHelper(context: Context, title: CharSequence?, message: CharSequence?) {

    private val dialogView: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.dialog_info, null)
    }

    private val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            .setView(dialogView)

    private val title: TextView by lazy {
        dialogView.findViewById<TextView>(R.id.dialogInfoTitleTextView)
    }


    private val positiveButton: Button by lazy {
        dialogView.findViewById<Button>(R.id.dialogInfoPositiveButton)
    }

    private var dialog: AlertDialog? = null

    var cancelable: Boolean = true

    init {
        this.title.text = title
    }


    fun positiveButton(text: CharSequence, func: (() -> Unit)? = null) {

        with(positiveButton) {
            this.text = text
            setClickListenerToDialogButton(func)
        }
    }



    fun onCancel(func: () -> Unit) {
        builder.setOnCancelListener { func() }
    }

    fun create(): AlertDialog {
        title.goneIfTextEmpty()
        positiveButton.goneIfTextEmpty()

        dialog = builder
                .setCancelable(cancelable)
                .create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog!!
    }

    private fun TextView.goneIfTextEmpty() {
        visibility = if (text.isNullOrEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun Button.setClickListenerToDialogButton(func: (() -> Unit)?) {
        setOnClickListener {
            func?.invoke()
            dialog?.dismiss()
        }
    }

}