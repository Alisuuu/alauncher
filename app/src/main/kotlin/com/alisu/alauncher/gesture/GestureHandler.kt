package com.alisu.alauncher.gesture

import android.content.Context
import com.alisu.alauncher.MainActivity

/**
 * Base class for gesture actions, inspired by Lawnchair.
 */
abstract class GestureHandler(val context: Context) {
    abstract fun onTrigger(launcher: MainActivity)
}

class NoOpGestureHandler(context: Context) : GestureHandler(context) {
    override fun onTrigger(launcher: MainActivity) {
        // Do nothing
    }
}
