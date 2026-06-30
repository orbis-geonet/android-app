package com.orbis.orbis.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment(), BaseFeatures {
    private var progressView: View? = null
    val baseActivity: BaseActivity?
        get() = if (activity is BaseActivity) {
            activity as BaseActivity?
        } else {
            throw IllegalArgumentException("Your fragment is not overriding BaseActivity.")
        }

    //
    private var persistingView: View? = null

    private fun persistingView(view: View): View {
        val root = persistingView
        if (root == null) {
            persistingView = view
            return view
        } else {
            (root.parent as? ViewGroup)?.removeView(root)
            return root
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val p = if (persistingView == null) onCreatePersistentView(
            inflater,
            container,
            savedInstanceState
        ) else persistingView // prevent inflating
        if (p != null) {
            return persistingView(p)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    protected open fun onCreatePersistentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (persistingView != null) {
            onPersistentViewCreated(view, savedInstanceState)
        }
    }

    protected open fun onPersistentViewCreated(view: View, savedInstanceState: Bundle?) {

    }
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(layoutId, container, false)
//        ButterKnife.bind(this, view)
//        baseActivity?.setUpToolbar(view.findViewById(R.id.toolbar))
//        setToolbarTitle(pageTitle)
//        return view
//
//    }


    override fun setToolbarTitle(title: String?) {

        baseActivity?.setToolbarTitle(title)
    }

    override fun setToolbarColor(color: Int?) {
        baseActivity?.setToolbarColor(color)
    }

    override fun setToolbarTitleColor(color: Int?) {
        baseActivity?.setToolbarTitleColor(color)
    }

    override fun setBackButtonColor(color: Int?) {
        baseActivity?.setBackButtonColor(color)
    }

    override fun showBackButton() {
        baseActivity?.showBackButton()
    }

    override fun hideBackButton() {
        baseActivity?.hideBackButton()
    }

    override fun showToolbar() {
        baseActivity?.showToolbar()
    }

    override fun hideToolbar() {

        baseActivity?.hideToolbar()
    }


    override fun backButtonClickListener(listener: View.OnClickListener?) {
        baseActivity?.backButtonClickListener(listener)
    }

//    protected fun showProgressBar(parent: ViewGroup) {
//        if (progressView == null) {
//            progressView =
//                LayoutInflater.from(parent.context).inflate(R.layout.loading_layout, null)
//            progressView!!.layoutParams = parent.layoutParams
//            parent.addView(progressView)
//        }
//    }
//
//    protected fun hideProgressBar(parent: ViewGroup) {
//        if (progressView != null) {
//            parent.removeView(progressView)
//            progressView = null
//        }
//    }
}
