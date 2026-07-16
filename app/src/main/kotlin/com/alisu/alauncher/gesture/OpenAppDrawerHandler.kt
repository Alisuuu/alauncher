package com.alisu.alauncher.gesture

import android.content.Context
import com.alisu.alauncher.MainActivity

/**
 * Handler to open the app library (drawer).
 */
class OpenAppDrawerHandler(context: Context) : GestureHandler(context) {
    override fun onTrigger(launcher: MainActivity) {
        // Encontra o ViewPager2 e muda para a página da biblioteca (index 2)
        // Como o MainActivity tem os métodos públicos, podemos chamá-los se existirem
        // No Alauncher, o MainActivity controla o ViewPager2
        launcher.openLibrary()
    }
}
