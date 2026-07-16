package com.alisu.alauncher.widget

import android.appwidget.AppWidgetHost
import android.content.Context

object WidgetHostProvider {
    const val APPWIDGET_HOST_ID = 2048
    
    private var instance: AppWidgetHost? = null

    fun getAppWidgetHost(context: Context): AppWidgetHost {
        if (instance == null) {
            instance = AppWidgetHost(context.applicationContext, APPWIDGET_HOST_ID)
        }
        return instance!!
    }
}
