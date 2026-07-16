package com.alisu.alauncher.gesture

import android.annotation.SuppressLint
import android.content.Context
import com.alisu.alauncher.MainActivity
import java.lang.reflect.InvocationTargetException

/**
 * Handler to expand notifications panel, using reflection like Lawnchair.
 */
class OpenNotificationsHandler(context: Context) : GestureHandler(context) {

    @SuppressLint("WrongConstant")
    override fun onTrigger(launcher: MainActivity) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManager.getMethod("expandNotificationsPanel")
            expandMethod.isAccessible = true
            expandMethod.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
