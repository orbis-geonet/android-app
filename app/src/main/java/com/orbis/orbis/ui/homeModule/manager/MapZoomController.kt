package com.orbis.orbis.ui.homeModule.manager

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap

class MapZoomController(
    private val mapProvider: () -> GoogleMap?,
    private val leftSeekBar: SeekBar,
    private val rightSeekBar: SeekBar
) {

    private var isDragging = false

    private var currentLeftProgress = 0
    private var currentRightProgress = 0
    private var previousLeftProgress = 0
    private var previousRightProgress = 0

    private var currentZoom = 10f

    private val middleOfProgress = 100
    private val zoomSensitive = 0.03f
    private var currentAnimator: ValueAnimator? = null

    fun init() {
        currentLeftProgress = leftSeekBar.progress
        currentRightProgress = rightSeekBar.progress
        previousLeftProgress = leftSeekBar.progress
        previousRightProgress = rightSeekBar.progress

        leftSeekBar.setOnSeekBarChangeListener(createListener(isLeft = true))
        rightSeekBar.setOnSeekBarChangeListener(createListener(isLeft = false))
    }

    private fun createListener(isLeft: Boolean): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                currentZoom = mapProvider()?.cameraPosition?.zoom ?: 10f
                isDragging = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isDragging = false
                seekBar.progress = middleOfProgress
                mapProvider()?.moveCamera(CameraUpdateFactory.zoomTo(currentZoom))
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val other = if (isLeft) rightSeekBar else leftSeekBar

                if (other.progress != progress) {
                    other.progress = progress
                }

                if (isDragging) {
                    handleZoom(progress, isLeft)
                } else {
                    if (isLeft) currentLeftProgress = progress
                    else currentRightProgress = progress
                }
            }
        }
    }

    private fun handleZoom(progress: Int, isLeft: Boolean) {
        if (isLeft) {
            currentLeftProgress = progress

            if (currentLeftProgress > previousLeftProgress) zoomIn()
            else zoomOut()

            previousLeftProgress = currentLeftProgress
        } else {
            currentRightProgress = progress

            if (currentRightProgress > previousRightProgress) zoomIn()
            else zoomOut()

            previousRightProgress = currentRightProgress
        }
    }

    private fun zoomIn() {
        currentZoom += zoomSensitive
        mapProvider()?.moveCamera(CameraUpdateFactory.zoomTo(currentZoom))
    }

    private fun zoomOut() {
        currentZoom -= zoomSensitive
        mapProvider()?.moveCamera(CameraUpdateFactory.zoomTo(currentZoom))
    }

    fun enable() {
        leftSeekBar.isEnabled = true
        rightSeekBar.isEnabled = true
    }

    fun disable() {
        leftSeekBar.isEnabled = false
        rightSeekBar.isEnabled = false
    }


    fun moveSeekbarsUp() {
        animateSeekbars(from = 0, to = 140, duration = 1000L)
    }

    fun moveSeekbarsDown() {
        animateSeekbars(from = 140, to = 0, duration = 500L)
    }

    private fun animateSeekbars(from: Int, to: Int, duration: Long) {
        currentAnimator?.cancel()

        currentAnimator = ValueAnimator.ofInt(from, to).apply {
            this.duration = duration

            addUpdateListener { animator ->
                val value = animator.animatedValue as Int

                updateBottomMargin(leftSeekBar, value)
                updateBottomMargin(rightSeekBar, value)
            }

            start()
        }
    }

    private fun updateBottomMargin(view: View, margin: Int) {
        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = margin
        view.layoutParams = params
    }

}