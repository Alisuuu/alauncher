package com.alisu.alauncher.gesture

import android.content.Context
import android.graphics.Canvas
import android.widget.EdgeEffect

class EdgeEffectHelper(context: Context) {
    private val edgeEffectLeft = EdgeEffect(context)
    private val edgeEffectRight = EdgeEffect(context)

    var isEnabled = true

    fun onPullLeft(delta: Float, displacement: Float) {
        if (!isEnabled) return
        edgeEffectLeft.onPull(-delta / 100f, displacement)
    }

    fun onPullRight(delta: Float, displacement: Float) {
        if (!isEnabled) return
        edgeEffectRight.onPull(delta / 100f, displacement)
    }

    fun onRelease() {
        edgeEffectLeft.onRelease()
        edgeEffectRight.onRelease()
    }

    fun onAbsorb(velocity: Int) {
        if (!isEnabled) return
        edgeEffectLeft.onAbsorb(velocity)
        edgeEffectRight.onAbsorb(velocity)
    }

    fun draw(canvas: Canvas): Boolean {
        var needsRedraw = false
        if (!edgeEffectLeft.isFinished) {
            canvas.save()
            canvas.rotate(270f)
            canvas.translate(-canvas.height.toFloat(), 0f)
            needsRedraw = edgeEffectLeft.draw(canvas)
            canvas.restore()
        }
        if (!edgeEffectRight.isFinished) {
            canvas.save()
            canvas.rotate(90f)
            canvas.translate(0f, -canvas.width.toFloat())
            needsRedraw = needsRedraw || edgeEffectRight.draw(canvas)
            canvas.restore()
        }
        return needsRedraw
    }

    fun isFinished(): Boolean = edgeEffectLeft.isFinished && edgeEffectRight.isFinished
}
