package com.alisu.alauncher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

object WidgetPreviewLoader {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val previewCache = android.util.LruCache<Int, Bitmap>(20)

    fun loadPreview(
        context: Context,
        provider: AppWidgetProviderInfo,
        width: Int,
        height: Int,
        callback: (Bitmap?) -> Unit
    ) {
        val cached = previewCache.get(provider.provider.hashCode())
        if (cached != null) {
            callback(cached)
            return
        }

        executor.execute {
            val bitmap = generatePreview(context, provider, width, height)
            if (bitmap != null) {
                previewCache.put(provider.provider.hashCode(), bitmap)
            }
            mainHandler.post { callback(bitmap) }
        }
    }

    private fun generatePreview(
        context: Context,
        provider: AppWidgetProviderInfo,
        width: Int,
        height: Int
    ): Bitmap? {
        return try {
            val providerPkg = provider.provider.packageName
            val providerContext = context.createPackageContext(providerPkg, 0)

            if (provider.previewImage != 0) {
                val drawable = providerContext.getDrawable(provider.previewImage)
                if (drawable != null) {
                    val intrinsicWidth = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else width
                    val intrinsicHeight = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else height
                    val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                    drawable.draw(canvas)
                    return bitmap
                }
            }

            if (provider.previewLayout != 0) {
                val view = android.view.LayoutInflater.from(providerContext).inflate(provider.previewLayout, null, false)
                view.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.AT_MOST),
                    android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.AT_MOST)
                )
                val measuredWidth = if (view.measuredWidth > 0) view.measuredWidth else width
                val measuredHeight = if (view.measuredHeight > 0) view.measuredHeight else height
                view.layout(0, 0, measuredWidth, measuredHeight)
                val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                view.draw(canvas)
                return bitmap
            }

            val icon = providerContext.packageManager.getApplicationIcon(providerPkg)
            if (icon != null) {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val iconSize = (width * 0.3f).toInt()
                icon.setBounds((width - iconSize) / 2, (height - iconSize) / 2, (width + iconSize) / 2, (height + iconSize) / 2)
                icon.draw(canvas)
                return bitmap
            }

            generatePlaceholder(context, provider, width, height)
        } catch (e: Exception) {
            generatePlaceholder(context, provider, width, height)
        }
    }

    private fun generatePlaceholder(
        context: Context,
        provider: AppWidgetProviderInfo,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.parseColor("#1A1A2E")
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.color = Color.parseColor("#33FFFFFF")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(4f, 4f, width - 4f, height - 4f, paint)

        val gridSize = 20f
        paint.strokeWidth = 0.5f
        var x = gridSize
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
            x += gridSize
        }
        var y = gridSize
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
            y += gridSize
        }

        paint.color = Color.parseColor("#66FFFFFF")
        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        val label = provider.loadLabel(context.packageManager)
        canvas.drawText(label, width / 2f, height / 2f + 4f, paint)

        return bitmap
    }

    fun clearCache() {
        previewCache.evictAll()
    }
}
