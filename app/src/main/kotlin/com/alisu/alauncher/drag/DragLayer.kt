package com.alisu.alauncher.drag

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd

class DragLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var floatingIcon: ImageView? = null
    private var originX: Float = 0f
    private var originY: Float = 0f
    private val cachedLocation = IntArray(2)
    private var locationCacheValid = false

    init {
        visibility = View.GONE
        isClickable = false
        isFocusable = false
        clipChildren = false
        clipToPadding = false
    }

    fun invalidateLocationCache() {
        locationCacheValid = false
    }

    fun startDrag(bitmap: Bitmap?, startX: Float, startY: Float, width: Int, height: Int) {
        originX = startX
        originY = startY
        endDrag()

        val iv = ImageView(context).apply {
            setImageBitmap(bitmap)
            scaleX = 1.25f
            scaleY = 1.25f
            alpha = 0.92f
        }
        floatingIcon = iv
        addView(iv, LayoutParams(width, height))
        invalidateLocationCache()
        moveTo(startX, startY)
        visibility = View.VISIBLE
    }

    fun moveTo(x: Float, y: Float) {
        floatingIcon?.apply {
            val halfW = width / 2f
            val halfH = height / 2f
            if (!locationCacheValid) {
                getLocationOnScreen(cachedLocation)
                locationCacheValid = true
            }
            translationX = x - halfW - cachedLocation[0]
            translationY = y - halfH - cachedLocation[1]
        }
    }

    fun endDrag() {
        floatingIcon?.let { removeView(it) }
        floatingIcon = null
        visibility = View.GONE
        locationCacheValid = false
    }

    fun snapBack(onFinished: () -> Unit = {}) {
        val icon = floatingIcon ?: run { onFinished(); return }
        getLocationOnScreen(cachedLocation)

        val targetX = originX - icon.width / 2f - cachedLocation[0]
        val targetY = originY - icon.height / 2f - cachedLocation[1]

        val animX = ObjectAnimator.ofFloat(icon, "translationX", icon.translationX, targetX)
        val animY = ObjectAnimator.ofFloat(icon, "translationY", icon.translationY, targetY)
        val animA = ObjectAnimator.ofFloat(icon, "alpha", icon.alpha, 0f)
        val animSX = ObjectAnimator.ofFloat(icon, "scaleX", icon.scaleX, 1f)
        val animSY = ObjectAnimator.ofFloat(icon, "scaleY", icon.scaleY, 1f)

        AnimatorSet().apply {
            playTogether(animX, animY, animA, animSX, animSY)
            duration = 200
            doOnEnd {
                endDrag()
                onFinished()
            }
            start()
        }
    }

    val isDragging: Boolean get() = floatingIcon != null
}
