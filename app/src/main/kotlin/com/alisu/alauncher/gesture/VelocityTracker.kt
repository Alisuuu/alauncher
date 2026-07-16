package com.alisu.alauncher.gesture

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration

class VelocityTracker {
    private var lastX = 0f
    private var lastY = 0f
    private var lastTime = 0L
    private var velocityX = 0f
    private var velocityY = 0f
    private val maxVelocity: Float

    constructor(context: Context) {
        maxVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()
    }

    constructor(maxVelocity: Float) {
        this.maxVelocity = maxVelocity
    }

    fun addMovement(event: MotionEvent) {
        val x = event.x
        val y = event.y
        val now = event.eventTime

        if (lastTime != 0L) {
            val dt = (now - lastTime).toFloat()
            if (dt > 0) {
                velocityX = (x - lastX) / dt * 1000f
                velocityY = (y - lastY) / dt * 1000f
                velocityX = velocityX.coerceIn(-maxVelocity, maxVelocity)
                velocityY = velocityY.coerceIn(-maxVelocity, maxVelocity)
            }
        }

        lastX = x
        lastY = y
        lastTime = now
    }

    fun getXVelocity(): Float = velocityX
    fun getYVelocity(): Float = velocityY

    fun clear() {
        lastX = 0f
        lastY = 0f
        lastTime = 0L
        velocityX = 0f
        velocityY = 0f
    }
}
