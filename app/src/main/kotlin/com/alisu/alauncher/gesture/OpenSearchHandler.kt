package com.alisu.alauncher.gesture

import android.content.Context
import com.alisu.alauncher.MainActivity

/**
 * Handler to open the global search.
 */
class OpenSearchHandler(context: Context) : GestureHandler(context) {
    override fun onTrigger(launcher: MainActivity) {
        launcher.openGlobalSearch()
    }
}
