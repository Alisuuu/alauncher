package com.alisu.alauncher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.LruCache
import coil.size.Size
import coil.transform.Transformation

class MonochromaticTransformation(
    private val primaryColor: Int,
    private val surfaceColor: Int
) : Transformation {

    override val cacheKey: String = "monochromatic_$primaryColor"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val key = "${input.hashCode()}_$primaryColor"
        bitmapCache.get(key)?.let { return it }

        val width = input.width
        val height = input.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val r = Color.red(primaryColor) / 255f
        val g = Color.green(primaryColor) / 255f
        val b = Color.blue(primaryColor) / 255f

        val matrix = floatArrayOf(
            0.2126f * r, 0.7152f * r, 0.0722f * r, 0f, 0f,
            0.2126f * g, 0.7152f * g, 0.0722f * g, 0f, 0f,
            0.2126f * b, 0.7152f * b, 0.0722f * b, 0f, 0f,
            0f,          0f,          0f,          1f, 0f
        )

        paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix))
        canvas.drawBitmap(input, 0f, 0f, paint)

        bitmapCache.put(key, output)
        return output
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is MonochromaticTransformation &&
               primaryColor == other.primaryColor &&
               surfaceColor == other.surfaceColor
    }

    override fun hashCode(): Int {
        var result = primaryColor
        result = 31 * result + surfaceColor
        return result
    }

    companion object {
        private const val MAX_CACHE_SIZE_KB = 5 * 1024 // 5MB
        private val bitmapCache = object : LruCache<String, Bitmap>(MAX_CACHE_SIZE_KB) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
        }

        fun clearCache() {
            bitmapCache.evictAll()
        }
    }
}

class ShapeClippingTransformation(
    private val shape: String,
    private val radiusPx: Float
) : Transformation {

    override val cacheKey: String = "shape_clip_${shape}_${radiusPx}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val key = "${input.hashCode()}_${shape}_${radiusPx}"
        bitmapCache.get(key)?.let { return it }

        val width = input.width
        val height = input.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        if (shape == "circle") {
            canvas.drawOval(0f, 0f, width.toFloat(), height.toFloat(), paint)
        } else {
            val rectF = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rectF, radiusPx, radiusPx, paint)
        }

        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(input, 0f, 0f, paint)

        bitmapCache.put(key, output)
        return output
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ShapeClippingTransformation &&
               shape == other.shape &&
               radiusPx == other.radiusPx
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + radiusPx.hashCode()
        return result
    }

    companion object {
        private const val MAX_CACHE_SIZE_KB = 5 * 1024 // 5MB
        private val bitmapCache = object : LruCache<String, Bitmap>(MAX_CACHE_SIZE_KB) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
        }

        fun clearCache() {
            bitmapCache.evictAll()
        }
    }
}
