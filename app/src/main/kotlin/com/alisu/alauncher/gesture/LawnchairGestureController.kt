package com.alisu.alauncher.gesture

import android.content.Context
import android.view.HapticFeedbackConstants
import com.alisu.alauncher.MainActivity

/**
 * Controller inspired by Lawnchair that manages high-level gestures and their actions.
 */
class LawnchairGestureController(private val launcher: MainActivity) {

    private val context: Context = launcher

    // Handlers
    private var swipeUpHandler: GestureHandler = OpenAppDrawerHandler(context)
    private var swipeDownHandler: GestureHandler = OpenSearchHandler(context)
    private var doubleTapHandler: GestureHandler = SleepHandler(context)

    fun onSwipeUp() {
        triggerHandler(swipeUpHandler)
    }

    fun onSwipeDown() {
        triggerHandler(swipeDownHandler)
    }

    fun onDoubleTap() {
        triggerHandler(doubleTapHandler)
    }

    private fun triggerHandler(handler: GestureHandler) {
        if (handler is NoOpGestureHandler) return
        
        handler.onTrigger(launcher)
        
        // Haptic feedback
        launcher.window.decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Updates the handler for a specific gesture. 
     * This can be called from settings to change gesture mappings.
     */
    fun setSwipeUpHandler(handler: GestureHandler) { swipeUpHandler = handler }
    fun setSwipeDownHandler(handler: GestureHandler) { swipeDownHandler = handler }
    fun setDoubleTapHandler(handler: GestureHandler) { doubleTapHandler = handler }
}
