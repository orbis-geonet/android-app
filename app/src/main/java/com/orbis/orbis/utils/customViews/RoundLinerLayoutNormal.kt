package com.orbis.orbis.utils.customViews

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.orbis.orbis.R
import com.orbis.orbis.utils.ViewUtils.Companion.applyShadow

class RoundLinerLayoutNormal @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var currElevation: Int = 0
    private var shadowRadius: Int = 0
    public var shadowColor: Int = 0
    public var shadowBkgColor: Int = Color.WHITE
    private var shadowGravity: Int = Gravity.CENTER

    private var insetLeft: Int = 0
    private var insetTop: Int = 0
    private var insetRight: Int = 0
    private var insetBottom: Int = 0

    private var dx: Float = 0F
    private var dy: Float = 0F

    fun updateShadowBkgColor(shadowBkgColor: Int){
        this.shadowBkgColor=shadowBkgColor
        applyShadowBackground()
    }
    fun updateShadowColor(shadowColor: Int){
        this.shadowColor=shadowColor
        applyShadowBackground()
    }

    fun updateCurrElevation(currElevation: Int){
        this.currElevation=currElevation
        applyShadowBackground()
    }

    init {
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.RoundLinerLayoutNormal)
            currElevation = ta.getDimensionPixelSize(
                R.styleable.RoundLinerLayoutNormal_rll_elevation,
                context.resources.getDimensionPixelSize(R.dimen.elevation_5dp)
            )
            shadowRadius = ta.getDimensionPixelSize(
                R.styleable.RoundLinerLayoutNormal_rll_shadow_radius,
                context.resources.getDimensionPixelSize(R.dimen.radius_5dp)
            )

            shadowColor = ta.getColor(
                R.styleable.RoundLinerLayoutNormal_rll_shadow_color,
                ContextCompat.getColor(getContext(),
                    R.color.shadowColor)
            )

            shadowBkgColor = ta.getColor(
                R.styleable.RoundLinerLayoutNormal_rll_shadow_bkg_color,
                Color.WHITE
            )

            insetLeft =
                ta.getDimensionPixelSize(
                    R.styleable.RoundLinerLayoutNormal_rll_inset_left,
                    currElevation
                )
            insetTop =
                ta.getDimensionPixelSize(
                    R.styleable.RoundLinerLayoutNormal_rll_inset_top,
                    currElevation
                )
            insetRight =
                ta.getDimensionPixelSize(
                    R.styleable.RoundLinerLayoutNormal_rll_inset_right,
                    currElevation
                )
            insetBottom =
                ta.getDimensionPixelSize(
                    R.styleable.RoundLinerLayoutNormal_rll_inset_bottom,
                    currElevation
                )

            shadowGravity =
                ta.getInteger(
                    R.styleable.RoundLinerLayoutNormal_rll_shadow_gravity,
                    shadowGravity
                )

            dx = ta.getDimensionPixelSize(R.styleable.RoundLinerLayoutNormal_rll_shadow_dx, 0)
                .toFloat()
            dy = ta.getDimensionPixelSize(R.styleable.RoundLinerLayoutNormal_rll_shadow_dy, 0)
                .toFloat()

            ta.recycle()
        }

        applyShadowBackground()
    }

    private fun applyShadowBackground() {
        applyShadow(
            shadowBkgColor,
            shadowRadius.toFloat(),
            shadowColor,
            currElevation,
            shadowGravity,
            insetLeft, insetTop, insetRight, insetBottom, dx, dy
        )
    }

    /***
     * Updates the shadow attributes.
     *
     * @param bkgColor the color of background layer which is below the shadow
     * @param shadowRadius shadow radius
     * @param shadowColorValue color of shadow
     * @param elevationValue shadow elevation
     * @param shadowGravity gravity of shadow, see [View's Gravity](android.view.Gravity)
     * @param insetLeftValue number of pixels to add to the left bound of shadow drawable
     * @param insetTopValue number of pixels to add to the top bound of shadow drawable
     * @param insetRightValue number of pixels to add to the right bound of shadow drawable
     * @param insetBottomValue number of pixels to add to the bottom bound of shadow drawable
     * @param dx shadow layer dx
     * @param dy shadow layer dy
     */
    fun updateShadowBackground(
        bkgColor: Int = shadowBkgColor,
        shadowRadius: Int = this.shadowRadius,
        shadowColorValue: Int = shadowColor,
        elevationValue: Int = currElevation,
        shadowGravity: Int = this.shadowGravity,
        insetLeftValue: Int = this.insetLeft,
        insetTopValue: Int = this.insetTop,
        insetRightValue: Int = this.insetRight,
        insetBottomValue: Int = this.insetBottom,
        dx: Float = this.dx,
        dy: Float = this.dy
    ) {

        this.shadowBkgColor = bkgColor
        this.shadowRadius = shadowRadius
        this.shadowColor = shadowColorValue
        this.currElevation = elevationValue
        this.shadowGravity = shadowGravity
        this.insetLeft = insetLeftValue
        this.insetTop = insetTopValue
        this.insetRight = insetRightValue
        this.insetBottom = insetBottomValue
        this.dx = dx
        this.dy = dy

        applyShadowBackground()
    }


}