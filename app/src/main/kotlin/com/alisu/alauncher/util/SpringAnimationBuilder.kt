package com.alisu.alauncher.util

import android.animation.ValueAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SpringAnimationBuilder(private val stiffness: Float = 300f, private val damping: Float = 0.85f) {

    private var startValue: Float = 0f
    private var endValue: Float = 1f
    private var velocity: Float = 0f
    private var duration: Long = 300

    fun setValues(start: Float, end: Float): SpringAnimationBuilder {
        startValue = start
        endValue = end
        return this
    }

    fun setVelocity(v: Float): SpringAnimationBuilder {
        velocity = v
        return this
    }

    fun build(view: View, property: (View, Float) -> Unit): ValueAnimator {
        val omega = Math.sqrt((stiffness - damping * damping).toDouble()).toFloat()
        val frames = computeSpringFrames(omega)

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            addUpdateListener { animation ->
                val t = animation.animatedFraction
                val springValue = evaluateSpring(t, omega)
                val finalValue = startValue + (endValue - startValue) * springValue
                property(view, finalValue)
            }
        }
        return animator
    }

    private fun evaluateSpring(t: Float, omega: Float): Float {
        val decay = exp((-damping * t * 10).toDouble()).toFloat()
        val oscillation = sin((omega * t * 2 * PI).toFloat())
        return (1f - decay * oscillation).coerceIn(0f, 1f)
    }

    private fun computeSpringFrames(omega: Float): Int {
        val threshold = 0.001f
        var t = 0f
        var frames = 0
        while (frames < 120) {
            val decay = exp((-damping * t * 10).toDouble()).toFloat()
            if (decay < threshold) break
            t += 0.016f
            frames++
        }
        duration = (frames * 16L).coerceIn(100, 500)
        return frames
    }

    companion object {
        fun scaleDown(view: View, factor: Float = 0.92f, onComplete: (() -> Unit)? = null) {
            SpringAnimationBuilder().setValues(1f, factor).build(view) { v, value ->
                v.scaleX = value
                v.scaleY = value
            }.apply {
                doOnEnd { onComplete?.invoke() }
                start()
            }
        }

        fun scaleUp(view: View, onComplete: (() -> Unit)? = null) {
            SpringAnimationBuilder().setValues(view.scaleX, 1f).build(view) { v, value ->
                v.scaleX = value
                v.scaleY = value
            }.apply {
                doOnEnd { onComplete?.invoke() }
                start()
            }
        }

        fun fadeIn(view: View, duration: Long = 200) {
            view.alpha = 0f
            view.animate()
                .alpha(1f)
                .setDuration(duration)
                .start()
        }

        fun fadeOut(view: View, duration: Long = 200, onComplete: (() -> Unit)? = null) {
            view.animate()
                .alpha(0f)
                .setDuration(duration)
                .withEndAction { onComplete?.invoke() }
                .start()
        }
    }
}
