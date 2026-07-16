package com.alisu.alauncher.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class PendingWidgetHostView(context: Context) : FrameLayout(context) {

    private val previewImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    /** Layout exibido quando o widget falha ao carregar (null bitmap no showPreview) */
    private val errorLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        visibility = GONE
    }

    private var isLoading = true

    init {
        addView(previewImageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Ícone de erro (símbolo ⚠ desenhado em texto)
        val ivError = TextView(context).apply {
            text = "⚠"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTextColor(0xFFFF6B6B.toInt())
            gravity = Gravity.CENTER
        }

        val tvError = TextView(context).apply {
            text = context.getString(com.alisu.alauncher.R.string.widget_unavailable)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(0xFFAAAAAA.toInt())
            gravity = Gravity.CENTER
            val px4 = (4 * resources.displayMetrics.density).toInt()
            setPadding(0, px4, 0, 0)
        }

        errorLayout.addView(ivError)
        errorLayout.addView(tvError)
        addView(errorLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        setBackgroundColor(Color.parseColor("#1A1A2E"))
    }

    /**
     * Exibe o bitmap de preview quando disponível.
     * Se [bitmap] for null, exibe o estado de erro em vez de ficar preso no loading.
     */
    fun showPreview(bitmap: Bitmap?) {
        if (bitmap != null) {
            previewImageView.setImageBitmap(bitmap)
            errorLayout.visibility = GONE
            isLoading = false
        } else {
            // Bitmap null = falha ao obter preview → mostrar estado de erro
            previewImageView.setImageDrawable(null)
            errorLayout.visibility = VISIBLE
            isLoading = false
        }
    }

    fun showLoading() {
        isLoading = true
        errorLayout.visibility = GONE
        previewImageView.setImageDrawable(LoadingDrawable())
    }

    fun isLoading(): Boolean = isLoading

    private inner class LoadingDrawable : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33FFFFFF
        }

        override fun draw(canvas: Canvas) {
            val bounds = bounds
            val centerX = bounds.exactCenterX()
            val centerY = bounds.exactCenterY()
            val radius = minOf(bounds.width(), bounds.height()) / 4f

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawArc(
                centerX - radius, centerY - radius,
                centerX + radius, centerY + radius,
                0f, 270f, false, paint
            )
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
