package com.alisu.alauncher.gesture

import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration

abstract class SwipeDetector(context: Context) {

    enum class Direction { UP, DOWN, LEFT, RIGHT }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maxVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()

    private var activePointerId = -1
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var velocityTracker: VelocityTracker? = null

    open fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                downX = ev.x
                downY = ev.y
                lastX = ev.x
                lastY = ev.y
                dragging = false
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                if (activePointerId == -1) return false
                velocityTracker?.addMovement(ev)

                val dx = ev.x - downX
                val dy = ev.y - downY
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (!dragging && dist > touchSlop) {
                    dragging = true
                    val direction = getDirection(dx, dy)
                    onDragStart(direction)
                }

                if (dragging) {
                    val deltaX = ev.x - lastX
                    val deltaY = ev.y - lastY
                    lastX = ev.x
                    lastY = ev.y
                    onDrag(deltaX, deltaY, ev)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    velocityTracker?.computeCurrentVelocity(1000, maxVelocity)
                    val vx = velocityTracker?.xVelocity ?: 0f
                    val vy = velocityTracker?.yVelocity ?: 0f
                    onDragEnd(vx, vy)
                }
                velocityTracker?.recycle()
                velocityTracker = null
                activePointerId = -1
                dragging = false
            }
        }
        return dragging
    }

    open fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!dragging) return false

        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(ev)
                val deltaX = ev.x - lastX
                val deltaY = ev.y - lastY
                lastX = ev.x
                lastY = ev.y
                onDrag(deltaX, deltaY, ev)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.computeCurrentVelocity(1000, maxVelocity)
                val vx = velocityTracker?.xVelocity ?: 0f
                val vy = velocityTracker?.yVelocity ?: 0f
                onDragEnd(vx, vy)
                velocityTracker?.recycle()
                velocityTracker = null
                activePointerId = -1
                dragging = false
            }
        }
        return true
    }

    private fun getDirection(dx: Float, dy: Float): Direction {
        return if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) Direction.RIGHT else Direction.LEFT
        } else {
            if (dy > 0) Direction.DOWN else Direction.UP
        }
    }

    protected open fun onDragStart(direction: Direction) {}
    protected abstract fun onDrag(deltaX: Float, deltaY: Float, event: MotionEvent)
    protected open fun onDragEnd(velocityX: Float, velocityY: Float) {}

    fun isDragging(): Boolean = dragging
}
