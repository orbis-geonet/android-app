package com.orbis.orbis.base

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.orbis.orbis.R

abstract class BaseBottomSheetFragment : BottomSheetDialogFragment(),
    BaseFeatures {
    private val progressView: View? = null
    val baseActivity: BaseActivity?
        get() = if (activity is BaseActivity) {
            activity as BaseActivity?
        } else {
            throw IllegalArgumentException("Your fragment is not overriding BaseActivity.")
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(layoutId, container, false)

        return view
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle)

    }
    //To display dialog in full screen mode "expand dialog"
    override fun setupDialog(dialog: Dialog, style: Int) {
        //  super.setupDialog(dialog, style);
        val view = View.inflate(context, layoutId, null)
        dialog.setContentView(view)

        // Handle navigation bar insets properly
        var isGesture: Boolean = false
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            isGesture = navInsets.bottom == 0
            insets
        }
        //dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
        val layoutParams = bottomSheet!!.layoutParams
        if (layoutParams != null) {
            val padding = if (isGesture) 50 else 170
            layoutParams.height = Resources.getSystem().displayMetrics.heightPixels - padding
        }
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        bottomSheet.layoutParams = layoutParams
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
//        behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
    }

    abstract val layoutId: Int
    abstract val pageTitle: String?

    override fun setToolbarTitle(title: String?) {
        baseActivity!!.setToolbarTitle(title)
    }

    override fun setToolbarColor(color: Int?) {
        baseActivity!!.setToolbarColor(color)
    }

    override fun setToolbarTitleColor(color: Int?) {
        baseActivity!!.setToolbarTitleColor(color)
    }

    override fun setBackButtonColor(color: Int?) {
        baseActivity!!.setBackButtonColor(color)
    }

    override fun showBackButton() {
        baseActivity!!.showBackButton()
    }

    override fun hideBackButton() {
        baseActivity!!.hideBackButton()
    }

    override fun hideToolbar() {}
    override fun showToolbar() {}
    override fun backButtonClickListener(listener: View.OnClickListener?) {
        baseActivity!!.backButtonClickListener(listener)
    }
}

