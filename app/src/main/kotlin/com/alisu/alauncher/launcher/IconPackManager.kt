package com.alisu.alauncher.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStreamReader

data class IconPackMaskInfo(
    val maskDrawable: Drawable?,
    val backDrawable: Drawable?,
    val uponDrawable: Drawable?,
    val scale: Float
)

object IconPackManager {

    private var currentIconPackPackage: String? = null
    private val iconMap = HashMap<String, String>()
    private var cachedIconPackContext: Context? = null
    private val rawIconInfoCache = HashMap<String, Pair<Int, android.content.pm.ApplicationInfo>?>()

    var packMaskInfo: IconPackMaskInfo? = null
        private set

    fun getInstalledIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val list = ArrayList<IconPackInfo>()
        
        val intents = listOf(
            Intent("com.novalauncher.THEME"),
            Intent("org.adw.launcher.THEMES"),
            Intent("com.dlto.launcher.theme")
        )
        
        val packages = HashSet<String>()
        for (intent in intents) {
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                if (packages.add(pkg)) {
                    val label = ri.loadLabel(pm).toString()
                    list.add(IconPackInfo(label, pkg))
                }
            }
        }
        return list
    }

    fun loadIconPack(context: Context, iconPackPackage: String?) {
        if (iconPackPackage == currentIconPackPackage) return
        currentIconPackPackage = iconPackPackage
        iconMap.clear()
        cachedIconPackContext = null
        packMaskInfo = null
        
        if (iconPackPackage == null || iconPackPackage.isEmpty() || iconPackPackage == "none") {
            return
        }

        try {
            val iconPackContext = context.createPackageContext(iconPackPackage, Context.CONTEXT_IGNORE_SECURITY)
            cachedIconPackContext = iconPackContext
            val resources = iconPackContext.resources
            
            val xmlId = resources.getIdentifier("appfilter", "xml", iconPackPackage)
            val parser = if (xmlId != 0) {
                resources.getXml(xmlId)
            } else {
                try {
                    val factory = XmlPullParserFactory.newInstance()
                    val p = factory.newPullParser()
                    p.setInput(InputStreamReader(iconPackContext.assets.open("appfilter.xml")))
                    p
                } catch (e: Exception) {
                    null
                }
            }

            if (parser != null) {
                var maskName: String? = null
                var backName: String? = null
                var uponName: String? = null
                var scale = 0.80f

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "iconmask" -> {
                                maskName = parser.getAttributeValue(null, "img1")
                            }
                            "iconback" -> {
                                backName = parser.getAttributeValue(null, "img1")
                            }
                            "iconupon" -> {
                                uponName = parser.getAttributeValue(null, "img1")
                            }
                            "scale" -> {
                                val factor = parser.getAttributeValue(null, "factor")
                                if (factor != null) {
                                    scale = factor.toFloatOrNull() ?: 0.80f
                                }
                            }
                            "item" -> {
                                val component = parser.getAttributeValue(null, "component")
                                val drawableName = parser.getAttributeValue(null, "drawable")
                                if (component != null && drawableName != null) {
                                    val startIdx = component.indexOf("{")
                                    val endIdx = component.indexOf("/")
                                    if (startIdx != -1 && endIdx != -1) {
                                        val pkg = component.substring(startIdx + 1, endIdx)
                                        iconMap[pkg] = drawableName
                                    }
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }

                if (maskName != null || backName != null || uponName != null) {
                    val mask = loadPackDrawable(iconPackContext, iconPackPackage, maskName)
                    val back = loadPackDrawable(iconPackContext, iconPackPackage, backName)
                    val upon = loadPackDrawable(iconPackContext, iconPackPackage, uponName)
                    packMaskInfo = IconPackMaskInfo(mask, back, upon, scale)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPackDrawable(context: Context, packPackage: String, name: String?): Drawable? {
        if (name == null) return null
        return try {
            val resId = context.resources.getIdentifier(name, "drawable", packPackage)
            if (resId != 0) {
                context.resources.getDrawable(resId, null)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getIcon(context: Context, packageName: String): Drawable? {
        val iconPackPackage = currentIconPackPackage ?: return null
        if (iconPackPackage == "none") return null
        val drawableName = iconMap[packageName] ?: return null
        
        return try {
            val iconPackCtx = cachedIconPackContext ?: context.createPackageContext(iconPackPackage, Context.CONTEXT_IGNORE_SECURITY).also {
                cachedIconPackContext = it
            }
            val resId = iconPackCtx.resources.getIdentifier(drawableName, "drawable", iconPackPackage)
            if (resId != 0) {
                iconPackCtx.resources.getDrawable(resId, null)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getPreviewIcons(context: Context, iconPackPackage: String, limit: Int = 4): List<Drawable> {
        val list = ArrayList<Drawable>()
        if (iconPackPackage == "none") return list
        
        try {
            val iconPackContext = context.createPackageContext(iconPackPackage, Context.CONTEXT_IGNORE_SECURITY)
            val resources = iconPackContext.resources
            
            val standardNames = listOf(
                "chrome", "phone", "camera", "settings", 
                "gmail", "playstore", "messages", "browser", 
                "contacts", "music", "gallery", "calendar"
            )
            
            for (name in standardNames) {
                val resId = resources.getIdentifier(name, "drawable", iconPackPackage)
                if (resId != 0) {
                    val drawable = resources.getDrawable(resId, null)
                    if (drawable != null) {
                        list.add(drawable)
                        if (list.size >= limit) break
                    }
                }
            }
            
            if (list.size < limit) {
                val xmlId = resources.getIdentifier("appfilter", "xml", iconPackPackage)
                val parser = if (xmlId != 0) {
                    resources.getXml(xmlId)
                } else {
                    try {
                        val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
                        val p = factory.newPullParser()
                        p.setInput(java.io.InputStreamReader(iconPackContext.assets.open("appfilter.xml")))
                        p
                    } catch (e: Exception) {
                        null
                    }
                }

                if (parser != null) {
                    var eventType = parser.eventType
                    var parsedCount = 0
                    while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && list.size < limit && parsedCount < 100) {
                        if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "item") {
                            val drawableName = parser.getAttributeValue(null, "drawable")
                            if (drawableName != null) {
                                val resId = resources.getIdentifier(drawableName, "drawable", iconPackPackage)
                                if (resId != 0) {
                                    val drawable = resources.getDrawable(resId, null)
                                    if (drawable != null && !list.contains(drawable)) {
                                        list.add(drawable)
                                    }
                                }
                            }
                            parsedCount++
                        }
                        eventType = parser.next()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getRawIcon(context: Context, packageName: String): Drawable? {
        val pm = context.packageManager
        try {
            val cached = rawIconInfoCache[packageName]
            val info = if (rawIconInfoCache.containsKey(packageName)) {
                cached
            } else {
                val intent = pm.getLaunchIntentForPackage(packageName)
                val resolveInfo = intent?.let { pm.resolveActivity(it, 0) }
                val result = if (resolveInfo != null && resolveInfo.activityInfo.iconResource != 0) {
                    Pair(resolveInfo.activityInfo.iconResource, resolveInfo.activityInfo.applicationInfo)
                } else {
                    null
                }
                rawIconInfoCache[packageName] = result
                result
            }
            
            if (info != null) {
                val appResources = pm.getResourcesForApplication(info.second)
                val density = context.resources.displayMetrics.densityDpi
                return appResources.getDrawableForDensity(info.first, density, null)
            }
        } catch (e: Exception) {
            // Ignore and fallback
        }
        return null
    }
}

data class IconPackInfo(
    val label: String,
    val packageName: String
)
