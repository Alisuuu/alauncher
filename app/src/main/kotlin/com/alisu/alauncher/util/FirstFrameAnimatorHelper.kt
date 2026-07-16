package com.alisu.alauncher.util

import android.animation.ValueAnimator
import android.view.View

object FirstFrameAnimatorHelper {

    private const val MAX_FIRST_FRAME_MS = 16L

    fun adjustStartDuration(animator: ValueAnimator, view: View) {
        var frameCount = 0
        val listener = object : ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                frameCount++
                if (frameCount == 1) {
                    val elapsed = System.currentTimeMillis() - (animation.startDelay + (animation.currentPlayTime - animation.startDelay))
                    if (elapsed > MAX_FIRST_FRAME_MS) {
                        animation.currentPlayTime = 0
                    }
                } else if (frameCount >= 2) {
                    animation.removeUpdateListener(this)
                }
            }
        }
        animator.addUpdateListener(listener)
    }

    fun setHardwareLayerForAnimation(view: View, enable: Boolean) {
        view.setLayerType(
            if (enable) View.LAYER_TYPE_HARDWARE else View.LAYER_TYPE_NONE,
            null
        )
    }
}
