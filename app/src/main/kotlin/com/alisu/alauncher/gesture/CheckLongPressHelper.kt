package com.alisu.alauncher.gesture

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

class CheckLongPressHelper(
    private val view: View,
    private val listener: OnLongClickListener
) {
    interface OnLongClickListener {
        fun onLongClick(view: View)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val longPressTimeout = (ViewConfiguration.getLongPressTimeout() * 0.75f).toLong()
    private val touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop

    private var downX = 0f
    private var downY = 0f
    private var longPressTriggered = false

    private val longPressRunnable = Runnable {
        if (!longPressTriggered) {
            longPressTriggered = true
            listener.onLongClick(view)
        }
    }

    fun onTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancel()
                downX = event.x
                downY = event.y
                longPressTriggered = false
                handler.postDelayed(longPressRunnable, longPressTimeout)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (dx * dx + dy * dy > touchSlop * touchSlop) {
                    cancel()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancel()
            }
        }
    }

    fun cancel() {
        handler.removeCallbacks(longPressRunnable)
    }

    fun hasTriggered(): Boolean = longPressTriggered
}
