package com.alisu.alauncher.gesture

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration

class AllAppsSwipeController(
    private val context: Context,
    private val callback: Callback
) {

    interface Callback {
        fun onSwipeUp()
        fun onSwipeDown()
        fun onSwipeLeft()
        fun onSwipeRight()
        fun isAllAppsOpen(): Boolean
        fun canOpenAllApps(): Boolean
        fun isDesktopPage(): Boolean
        fun isWidgetsPage(): Boolean
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val triggerVelocity = 800f
    private val triggerDistance = touchSlop * 3

    private var startX = 0f
    private var startY = 0f
    private var totalDeltaY = 0f
    private var totalDeltaX = 0f
    private var tracking = false
    private var activated = false
    private var isHorizontalSwipe = false

    var onNavigate: ((direction: String) -> Unit)? = null

    fun processEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                totalDeltaY = 0f
                totalDeltaX = 0f
                tracking = true
                activated = false
                isHorizontalSwipe = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!tracking) return
                val dy = event.y - startY
                val dx = event.x - startX
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (!activated && dist > touchSlop) {
                    val isVertical = Math.abs(dy) > Math.abs(dx) * 1.5f
                    val isHorizontal = Math.abs(dx) > Math.abs(dy) * 1.5f

                    if (isVertical) {
                        if (dy < 0 && callback.canOpenAllApps()) {
                            activated = true
                        } else if (dy > 0 && callback.isAllAppsOpen()) {
                            activated = true
                        }
                    } else if (isHorizontal) {
                        isHorizontalSwipe = true
                        if (dx < 0 && callback.isDesktopPage()) {
                            activated = true
                        } else if (dx > 0 && callback.isWidgetsPage()) {
                            activated = true
                        }
                    }
                }

                if (activated) {
                    if (isHorizontalSwipe) {
                        totalDeltaX = event.x - startX
                    } else {
                        totalDeltaY += (event.y - (startY + totalDeltaY))
                        totalDeltaY = event.y - startY
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activated) {
                    if (isHorizontalSwipe) {
                        if (totalDeltaX < -triggerDistance) {
                            callback.onSwipeLeft()
                        } else if (totalDeltaX > triggerDistance) {
                            callback.onSwipeRight()
                        }
                    } else {
                        val vy = totalDeltaY / 16f * 1000f
                        val isFling = Math.abs(vy) > triggerVelocity
                        val isPastThreshold = Math.abs(totalDeltaY) > triggerDistance

                        if (callback.isAllAppsOpen()) {
                            if (totalDeltaY > 0 && (isFling || isPastThreshold)) {
                                callback.onSwipeDown()
                            }
                        } else {
                            if (totalDeltaY < 0 && (isFling || isPastThreshold)) {
                                callback.onSwipeUp()
                            }
                        }
                    }
                }
                tracking = false
                activated = false
                isHorizontalSwipe = false
                totalDeltaY = 0f
                totalDeltaX = 0f
            }
        }
    }
}
