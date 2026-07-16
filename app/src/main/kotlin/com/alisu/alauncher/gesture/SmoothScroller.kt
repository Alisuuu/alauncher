package com.alisu.alauncher.gesture

import android.content.Context
import android.view.ViewConfiguration
import android.widget.OverScroller

class SmoothScroller(context: Context) {
    private val scroller = OverScroller(context)
    private val minSnapVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maxSnapVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity

    var currX: Int = 0
        private set
    var currY: Int = 0
        private set
    var isFinished: Boolean = true
        private set

    fun fling(startX: Int, startY: Int, velocityX: Int, velocityY: Int, minX: Int, maxX: Int, minY: Int, maxY: Int) {
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
        isFinished = false
    }

    fun smoothScrollTo(targetX: Int, targetY: Int, duration: Int) {
        scroller.startScroll(currX, currY, targetX - currX, targetY - currY, duration)
        isFinished = false
    }

    fun computeScrollOffset(): Boolean {
        if (scroller.isFinished) {
            isFinished = true
            return false
        }
        val result = scroller.computeScrollOffset()
        currX = scroller.currX
        currY = scroller.currY
        isFinished = scroller.isFinished
        return result
    }

    fun forceFinished() {
        scroller.forceFinished(true)
        isFinished = true
    }

    fun abortAnimation() {
        scroller.abortAnimation()
        isFinished = true
    }

    val velocity: Int
        get() = scroller.currVelocity.toInt()
}
