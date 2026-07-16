package com.alisu.alauncher.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

object ThemeDrawableLoader {

    private val cache = LruCache<String, Drawable>(16)

    fun loadDrawable(context: Context, path: String): Drawable? {
        cache.get(path)?.let { return it }

        return try {
            if (path.endsWith(".xml")) {
                loadXmlDrawable(context, path)
            } else if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                loadImageDrawable(context, path)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadXmlDrawable(context: Context, path: String): Drawable? {
        return try {
            val inputStream: InputStream = if (path.startsWith("file:///android_asset/")) {
                context.assets.open(path.removePrefix("file:///android_asset/"))
            } else if (path.startsWith("file://")) {
                FileInputStream(File(path.removePrefix("file://")))
            } else {
                return null
            }

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            val drawable = Drawable.createFromXml(context.resources, parser, context.theme)
            if (drawable != null) {
                cache.put(path, drawable)
            }
            drawable
        } catch (e: Exception) {
            null
        }
    }

    private fun loadImageDrawable(context: Context, path: String): Drawable? {
        val filePath = if (path.startsWith("file://")) path.removePrefix("file://") else path
        val bitmap = BitmapFactory.decodeFile(filePath)
        if (bitmap != null) {
            val drawable = BitmapDrawable(context.resources, bitmap).apply {
                setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            }
            cache.put(path, drawable)
            return drawable
        }
        return null
    }

    fun clearCache() {
        cache.evictAll()
    }
}
