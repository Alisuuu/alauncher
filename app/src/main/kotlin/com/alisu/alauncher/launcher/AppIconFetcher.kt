package com.alisu.alauncher.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.fetch.FetchResult
import coil.request.Options
import coil.size.pxOrElse

import android.os.Build

class AppIconFetcher(
    private val packageName: String,
    private val context: Context,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        return try {
            val pm = context.packageManager

            val selectedPack = prefsIconPack
            val shapeMode = prefsIconShapeMode
            IconPackManager.loadIconPack(context, selectedPack)

            val packIcon = IconPackManager.getIcon(context, packageName)
            val rawIcon = IconPackManager.getRawIcon(context, packageName)
                ?: pm.getApplicationIcon(packageName)

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val defaultIconSize = activityManager.launcherLargeIconSize
            val targetWidth = options.size.width.pxOrElse { defaultIconSize }
            val targetHeight = options.size.height.pxOrElse { defaultIconSize }

            val bitmap = when {
                packIcon != null -> {
                    drawableToBitmapRaw(packIcon, targetWidth, targetHeight)
                }
                shapeMode == "follow_pack" && IconPackManager.packMaskInfo != null -> {
                    val maskInfo = IconPackManager.packMaskInfo!!
                    drawableToBitmapWithPackMask(rawIcon, targetWidth, targetHeight, maskInfo)
                }
                else -> {
                    drawableToBitmapRaw(rawIcon, targetWidth, targetHeight)
                }
            }

            DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmapRaw(drawable: Drawable, width: Int, height: Int): Bitmap {
        val safeW = width.coerceIn(1, 512)
        val safeH = height.coerceIn(1, 512)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is android.graphics.drawable.AdaptiveIconDrawable) {
            val bg = drawable.background
            bg.setBounds(0, 0, safeW, safeH)
            bg.draw(canvas)

            val fg = drawable.foreground
            fg.setBounds(0, 0, safeW, safeH)
            fg.draw(canvas)
        } else {
            drawable.setBounds(0, 0, safeW, safeH)
            drawable.draw(canvas)
        }
        return bitmap
    }

    private fun drawableToBitmapWithPackMask(
        drawable: Drawable,
        width: Int,
        height: Int,
        maskInfo: IconPackMaskInfo
    ): Bitmap {
        val safeW = width.coerceIn(1, 512)
        val safeH = height.coerceIn(1, 512)
        val scale = maskInfo.scale

        val finalBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val finalCanvas = Canvas(finalBitmap)

        maskInfo.backDrawable?.let { back ->
            back.setBounds(0, 0, safeW, safeH)
            back.draw(finalCanvas)
        }

        val scaledW = (safeW * scale).toInt()
        val scaledH = (safeH * scale).toInt()
        val offsetX = (safeW - scaledW) / 2
        val offsetY = (safeH - scaledH) / 2

        val mask = maskInfo.maskDrawable
        if (mask != null) {
            val maskBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
            Canvas(maskBitmap).apply {
                mask.setBounds(0, 0, safeW, safeH)
                mask.draw(this)
            }

            val iconBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
            val iconCanvas = Canvas(iconBitmap)
            iconCanvas.save()
            iconCanvas.translate(offsetX.toFloat(), offsetY.toFloat())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is android.graphics.drawable.AdaptiveIconDrawable) {
                drawable.background.setBounds(0, 0, scaledW, scaledH)
                drawable.background.draw(iconCanvas)
                drawable.foreground.setBounds(0, 0, scaledW, scaledH)
                drawable.foreground.draw(iconCanvas)
            } else {
                drawable.setBounds(0, 0, scaledW, scaledH)
                drawable.draw(iconCanvas)
            }
            iconCanvas.restore()

            val result = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
            val resultCanvas = Canvas(result)
            resultCanvas.drawBitmap(iconBitmap, 0f, 0f, null)
            val clearPaint = Paint()
            clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            resultCanvas.drawBitmap(maskBitmap, 0f, 0f, clearPaint)

            finalCanvas.drawBitmap(result, 0f, 0f, null)
        } else {
            val iconBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
            val iconCanvas = Canvas(iconBitmap)
            iconCanvas.translate(offsetX.toFloat(), offsetY.toFloat())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is android.graphics.drawable.AdaptiveIconDrawable) {
                drawable.background.setBounds(0, 0, scaledW, scaledH)
                drawable.background.draw(iconCanvas)
                drawable.foreground.setBounds(0, 0, scaledW, scaledH)
                drawable.foreground.draw(iconCanvas)
            } else {
                drawable.setBounds(0, 0, scaledW, scaledH)
                drawable.draw(iconCanvas)
            }
            finalCanvas.drawBitmap(iconBitmap, 0f, 0f, null)
        }

        maskInfo.uponDrawable?.let { upon ->
            upon.setBounds(0, 0, safeW, safeH)
            upon.draw(finalCanvas)
        }

        return finalBitmap
    }

    companion object {
        @Volatile private var cachedPack: String? = null
        @Volatile private var cachedShape: String? = null
        @Volatile private var cachedShapeMode: String? = null
        fun refreshCache(context: Context) {
            val prefs = context.getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
            cachedPack = prefs.getString("icon_pack_package", "none") ?: "none"
            cachedShape = prefs.getString("icon_shape", "theme") ?: "theme"
            cachedShapeMode = prefs.getString("icon_shape_mode", "force_launcher") ?: "force_launcher"
        }
        fun invalidateCache() {
            cachedPack = null
            cachedShape = null
            cachedShapeMode = null
        }
    }
    private val prefsIconPack: String get() {
        if (cachedPack == null) refreshCache(context)
        return cachedPack ?: "none"
    }
    private val prefsIconShape: String get() {
        if (cachedShape == null) refreshCache(context)
        return cachedShape ?: "theme"
    }
    private val prefsIconShapeMode: String get() {
        if (cachedShapeMode == null) refreshCache(context)
        return cachedShapeMode ?: "force_launcher"
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme == "app-icon") {
                val packageName = data.host ?: ""
                return AppIconFetcher(packageName, context, options)
            }
            return null
        }
    }
}
