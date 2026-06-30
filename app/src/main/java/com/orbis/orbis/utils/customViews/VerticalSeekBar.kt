package com.orbis.orbis.utils.customViews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.ContextCompat
import com.orbis.orbis.R

/**
 * Vertical Progress Bar
 */
open class VerticalSeekBar : AppCompatSeekBar {
    private var thumbDrawable: Drawable? = null
    private var mChangeListener: OnSeekBarChangeListener? = null
    constructor(context: Context) : super(context) {
        initView(context, null)
    }
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        initView(context, attrs)
    }
    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.verticalSeekBar,
                0,
                0
            )
            thumbDrawable = typedArray.getDrawable(R.styleable.verticalSeekBar_thumb)
            // Set Custom Thumb
            thumb = thumbDrawable
        } else
            // Set Custom Thumb
            thumb = ContextCompat.getDrawable(context, R.drawable.ic_thumb)

        // Set Custom Progress Bar Drawable
        progressDrawable = ContextCompat.getDrawable(context, R.drawable.vertical_progress_selector)

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int){
        super.onSizeChanged(h, w, oldh, oldw)
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(c: Canvas) {
        c.rotate(-90f)
        c.translate(-height.toFloat(), 0f)
        super.onDraw(c)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
            progress = max - (max * event.y / height).toInt()
            onSizeChanged(width, height, 0, 0)
                mChangeListener?.onStartTrackingTouch(this)
            }
            MotionEvent.ACTION_MOVE -> {
                progress = max - (max * event.y / height).toInt()
                onSizeChanged(width, height, 0, 0)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP-> {
                progress = max - (max * event.y / height).toInt()
                onSizeChanged(width, height, 0, 0)
                mChangeListener?.onStopTrackingTouch(this)
            }
        }
        return true
    }

    @Synchronized
    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        onSizeChanged(width, height, 0, 0)
    }
    @Synchronized
    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        super.setOnSeekBarChangeListener(l)
        mChangeListener=l
    }
}