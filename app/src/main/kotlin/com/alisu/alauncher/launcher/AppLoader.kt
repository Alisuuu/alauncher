package com.alisu.alauncher.launcher

import android.content.Context
import android.content.Intent
import com.alisu.alauncher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppLoader {

    fun loadAppsForPackages(context: Context, packages: List<String>): List<AppInfo> {
        val pm = context.packageManager
        val apps = mutableListOf<AppInfo>()
        val prefs = context.getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
        val allPrefs = prefs.all
        for (pkg in packages) {
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                val customLabel = allPrefs["custom_label_$pkg"] as? String
                val label = customLabel ?: pm.getApplicationLabel(info).toString()
                apps.add(AppInfo(label, pkg))
            } catch (_: Exception) {
            }
        }
        return apps
    }

    suspend fun loadInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        val appsList = mutableListOf<AppInfo>()

        val prefs = context.getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
        val allPrefs = prefs.all
        val myPackage = context.packageName

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName == myPackage) continue
            val customLabel = allPrefs["custom_label_$packageName"] as? String
            val label = customLabel ?: resolveInfo.loadLabel(packageManager).toString()
            appsList.add(AppInfo(label, packageName))
        }

        appsList.sortBy { it.label.lowercase() }
        appsList
    }

    fun launchApp(context: Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (_: Exception) {
        }
    }
}
