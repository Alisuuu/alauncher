package com.alisu.alauncher

import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import coil.load
import com.alisu.alauncher.theme.AppTheme

internal fun MainActivity.applyClockStyleDefault(
    clockContainer: LinearLayout,
    mascotContainer: LinearLayout,
    ivMascot: ImageView,
    ivWeatherIcon: ImageView?,
    tcTime: TextClock,
    tcDate: TextClock,
    tvWeatherTemp: TextView?,
    tvWeatherCondition: TextView?,
    theme: AppTheme,
    cachedTemp: Int,
    cachedIcon: String,
    cachedCondition: String,
    hasSymbol: Boolean,
    hasCharacter: Boolean,
    view: View
) {
    val cardRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
    applyClockCardBackground(clockContainer, theme, cardRadius)
    clockContainer.setPadding(dpToPx(28), dpToPx(20), dpToPx(28), dpToPx(20))
    clockContainer.gravity = Gravity.CENTER
    clockContainer.orientation = LinearLayout.VERTICAL

    configureMascotAndWeather(
        mascotContainer, ivMascot, ivWeatherIcon, tvWeatherTemp, tvWeatherCondition,
        theme, cachedTemp, cachedIcon, cachedCondition,
        getShowMascot(), false, dpToPx(24)
    )

    applyTextClockStyle(tcTime, theme, 54f, -0.02f, theme.colors.textPrimary)
    applyTextClockStyle(tcDate, theme, 12f, 0.08f, adjustAlpha(theme.colors.textPrimary, 0.6f), isDate = true)

    clockContainer.removeAllViews()
    clockContainer.addView(mascotContainer)
    clockContainer.addView(tcTime)
    clockContainer.addView(tcDate)
}

internal fun MainActivity.applyClockStyleStacked(
    clockContainer: LinearLayout,
    mascotContainer: LinearLayout,
    ivMascot: ImageView,
    ivWeatherIcon: ImageView?,
    tcTime: TextClock,
    tcDate: TextClock,
    tvWeatherTemp: TextView?,
    tvWeatherCondition: TextView?,
    theme: AppTheme,
    cachedTemp: Int,
    cachedIcon: String,
    cachedCondition: String,
    hasSymbol: Boolean,
    hasCharacter: Boolean,
    view: View
) {
    val circleSize = dpToPx(220)
    clockContainer.layoutParams = LinearLayout.LayoutParams(circleSize, circleSize)
    clockContainer.gravity = Gravity.CENTER
    clockContainer.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))

    val ovalBg = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(adjustAlpha(theme.colors.surface, 0.5f))
        if (theme.shapes.showBorders) {
            setStroke(dpToPx(1.5f).toInt(), adjustAlpha(theme.colors.primary, 0.4f))
        }
    }
    clockContainer.background = ovalBg

    configureMascotAndWeather(
        mascotContainer, ivMascot, ivWeatherIcon, tvWeatherTemp, tvWeatherCondition,
        theme, cachedTemp, cachedIcon, cachedCondition,
        getShowMascot(), true, dpToPx(22)
    )

    applyTextClockStyle(tcTime, theme, 48f, -0.01f, theme.colors.textPrimary)
    applyTextClockStyle(tcDate, theme, 11f, 0.06f, adjustAlpha(theme.colors.textPrimary, 0.6f), isDate = true)

    clockContainer.removeAllViews()
    clockContainer.addView(tcTime)
    clockContainer.addView(tcDate)
    clockContainer.addView(mascotContainer)
}

internal fun MainActivity.applyClockStyleSymbolHorizontal(
    clockContainer: LinearLayout,
    mascotContainer: LinearLayout,
    ivMascot: ImageView,
    ivWeatherIcon: ImageView?,
    tcTime: TextClock,
    tcDate: TextClock,
    tvWeatherTemp: TextView?,
    tvWeatherCondition: TextView?,
    theme: AppTheme,
    cachedTemp: Int,
    cachedIcon: String,
    cachedCondition: String,
    hasSymbol: Boolean,
    hasCharacter: Boolean,
    view: View
) {
    val pillRadius = dpToPx(50f)
    val pillBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = pillRadius
        setColor(adjustAlpha(theme.colors.surface, 0.4f))
        if (theme.shapes.showBorders) {
            setStroke(dpToPx(1.5f).toInt(), adjustAlpha(theme.colors.primary, 0.35f))
        }
    }
    clockContainer.background = pillBg
    clockContainer.orientation = LinearLayout.HORIZONTAL
    clockContainer.gravity = Gravity.CENTER_VERTICAL
    clockContainer.setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12))

    configureMascotAndWeather(
        mascotContainer, ivMascot, ivWeatherIcon, tvWeatherTemp, tvWeatherCondition,
        theme, cachedTemp, cachedIcon, cachedCondition,
        getShowMascot(), false, dpToPx(22)
    )

    val divider = View(view.context).apply {
        layoutParams = LinearLayout.LayoutParams(dpToPx(1), dpToPx(28)).apply {
            setMargins(dpToPx(16), 0, dpToPx(16), 0)
        }
        setBackgroundColor(adjustAlpha(theme.colors.textSecondary, 0.2f))
    }

    applyTextClockStyle(tcTime, theme, 34f, -0.01f, theme.colors.textPrimary)
    applyTextClockStyle(tcDate, theme, 10f, 0.08f, adjustAlpha(theme.colors.textSecondary, 0.7f), isDate = true)

    val textSection = LinearLayout(view.context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.START
    }
    textSection.addView(tcTime)
    textSection.addView(tcDate)

    clockContainer.removeAllViews()
    clockContainer.addView(mascotContainer)
    clockContainer.addView(divider)
    clockContainer.addView(textSection)
}

internal fun MainActivity.applyClockStyleSymbolStacked(
    clockContainer: LinearLayout,
    mascotContainer: LinearLayout,
    ivMascot: ImageView,
    ivWeatherIcon: ImageView?,
    tcTime: TextClock,
    tcDate: TextClock,
    tvWeatherTemp: TextView?,
    tvWeatherCondition: TextView?,
    theme: AppTheme,
    cachedTemp: Int,
    cachedIcon: String,
    cachedCondition: String,
    hasSymbol: Boolean,
    hasCharacter: Boolean,
    view: View
) {
    val glowRadius = dpToPx(24f)
    val glowColor = adjustAlpha(theme.colors.primary, 0.6f)
    val glowOuter = adjustAlpha(theme.colors.primary, 0.15f)
    val glowBg = LayerDrawable(arrayOf(
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = glowRadius
            setColor(Color.TRANSPARENT)
            setStroke(dpToPx(6f).toInt(), glowOuter)
        },
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = glowRadius
        setColor(adjustAlpha(Color.BLACK, 0.35f))
            setStroke(dpToPx(1.5f).toInt(), glowColor)
        }
    ))
    clockContainer.background = glowBg
    clockContainer.setPadding(dpToPx(28), dpToPx(20), dpToPx(28), dpToPx(20))
    clockContainer.gravity = Gravity.CENTER
    clockContainer.orientation = LinearLayout.VERTICAL

    configureMascotAndWeather(
        mascotContainer, ivMascot, ivWeatherIcon, tvWeatherTemp, tvWeatherCondition,
        theme, cachedTemp, cachedIcon, cachedCondition,
        getShowMascot(), true, dpToPx(24)
    )

    applyTextClockStyle(tcTime, theme, 54f, -0.03f, theme.colors.textPrimary)
    tcTime.setShadowLayer(12f, 0f, 0f, glowColor)
    applyTextClockStyle(tcDate, theme, 11f, 0.1f, adjustAlpha(theme.colors.primary, 0.7f), isDate = true)

    clockContainer.removeAllViews()
    clockContainer.addView(mascotContainer)
    clockContainer.addView(tcTime)
    clockContainer.addView(tcDate)
}

internal fun MainActivity.applyClockStyleBracket(
    clockContainer: LinearLayout,
    mascotContainer: LinearLayout,
    ivMascot: ImageView,
    ivWeatherIcon: ImageView?,
    tcTime: TextClock,
    tcDate: TextClock,
    tvWeatherTemp: TextView?,
    tvWeatherCondition: TextView?,
    theme: AppTheme,
    cachedTemp: Int,
    cachedIcon: String,
    cachedCondition: String,
    hasSymbol: Boolean,
    hasCharacter: Boolean,
    view: View
) {
    val termBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
        setColor(adjustAlpha(Color.BLACK, 0.7f))
        setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.4f))
    }
    clockContainer.background = termBg
    clockContainer.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
    clockContainer.gravity = Gravity.CENTER

    val promptPrefix = TextView(view.context).apply {
        text = getString(R.string.terminal_prompt)
        textSize = 10f
        setTextColor(adjustAlpha(theme.colors.primary, 0.8f))
        typeface = android.graphics.Typeface.MONOSPACE
        setPadding(0, 0, 0, dpToPx(4))
    }

    configureMascotAndWeather(
        mascotContainer, ivMascot, ivWeatherIcon, tvWeatherTemp, tvWeatherCondition,
        theme, cachedTemp, cachedIcon, cachedCondition,
        getShowMascot(), false, dpToPx(18)
    )

    val terminalTime = TextView(view.context).apply {
        text = getString(R.string.terminal_time)
        textSize = 10f
        setTextColor(adjustAlpha(theme.colors.primary, 0.6f))
        typeface = android.graphics.Typeface.MONOSPACE
        setPadding(0, dpToPx(6), 0, dpToPx(2))
    }

    applyTextClockStyle(tcTime, theme, 40f, 0f, adjustAlpha(theme.colors.primary, 0.95f))
    tcTime.typeface = android.graphics.Typeface.MONOSPACE
    applyTextClockStyle(tcDate, theme, 9f, 0.08f, adjustAlpha(theme.colors.primary, 0.5f), isDate = true)
    tcDate.format12Hour = getString(R.string.date_format_clock)
    tcDate.format24Hour = getString(R.string.date_format_clock)

    val cursor = TextView(view.context).apply {
        text = "_"
        textSize = 40f
        setTextColor(theme.colors.primary)
        typeface = android.graphics.Typeface.MONOSPACE
    }

    val timeRow = LinearLayout(view.context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    timeRow.addView(tcTime)
    timeRow.addView(cursor)

    clockContainer.removeAllViews()
    clockContainer.addView(promptPrefix)
    clockContainer.addView(mascotContainer)
    clockContainer.addView(terminalTime)
    clockContainer.addView(timeRow)
    clockContainer.addView(tcDate)
}

internal fun MainActivity.applyClockStyleMinimal(
    clockContainer: LinearLayout,
    mascotContainer: LinearLayout,
    ivMascot: ImageView,
    ivWeatherIcon: ImageView?,
    tcTime: TextClock,
    tcDate: TextClock,
    tvWeatherTemp: TextView?,
    tvWeatherCondition: TextView?,
    theme: AppTheme,
    cachedTemp: Int,
    cachedIcon: String,
    cachedCondition: String,
    hasSymbol: Boolean,
    hasCharacter: Boolean,
    view: View
) {
    clockContainer.background = null
    clockContainer.outlineProvider = null
    clockContainer.clipToOutline = false
    clockContainer.setPadding(0, 0, 0, 0)
    clockContainer.gravity = Gravity.CENTER

    mascotContainer.visibility = View.GONE

    applyTextClockStyle(tcTime, theme, 64f, -0.03f, Color.WHITE)
    tcTime.setShadowLayer(24f, 0f, 2f, adjustAlpha(Color.BLACK, 0.8f))

    applyTextClockStyle(tcDate, theme, 13f, 0.1f, adjustAlpha(Color.WHITE, 0.55f), isDate = true)
    tcDate.setShadowLayer(16f, 0f, 1f, adjustAlpha(Color.BLACK, 0.6f))

    clockContainer.addView(tcTime)
    clockContainer.addView(tcDate)
}

internal fun MainActivity.applyClockCardBackground(container: LinearLayout, theme: AppTheme, radius: Float) {
    val cardBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius
        setColor(adjustAlpha(theme.colors.surface, 0.45f))
        if (theme.shapes.showBorders) {
            setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.3f))
        }
    }
    container.background = cardBg
    container.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
    container.clipToOutline = false
}

internal fun MainActivity.applyMascotToView(ivMascot: ImageView, theme: AppTheme, sizePx: Int) {
    ivMascot.clearColorFilter()
    val bmp = theme.symbolBitmap
    if (bmp != null) {
        ivMascot.visibility = View.VISIBLE
        ivMascot.setImageBitmap(bmp)
    } else if (theme.symbol != null) {
        ivMascot.visibility = View.VISIBLE
        ivMascot.setImageDrawable(null)
        ivMascot.load(theme.symbol) {
            memoryCachePolicy(coil.request.CachePolicy.DISABLED)
            diskCachePolicy(coil.request.CachePolicy.DISABLED)
        }
    } else {
        ivMascot.visibility = View.GONE
    }
}

internal fun MainActivity.applyClockStyleCharacterCard(
    clockContainer: LinearLayout,
    mascotContainer: LinearLayout,
    ivMascot: ImageView,
    ivWeatherIcon: ImageView?,
    tcTime: TextClock,
    tcDate: TextClock,
    tvWeatherTemp: TextView?,
    tvWeatherCondition: TextView?,
    theme: AppTheme,
    cachedTemp: Int,
    cachedIcon: String,
    cachedCondition: String,
    hasSymbol: Boolean,
    hasCharacter: Boolean,
    view: View
) {
    val cardRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
    applyClockCardBackground(clockContainer, theme, cardRadius)
    clockContainer.setPadding(dpToPx(28), dpToPx(20), dpToPx(28), dpToPx(20))
    clockContainer.gravity = Gravity.CENTER
    clockContainer.orientation = LinearLayout.VERTICAL
    clockContainer.layoutParams = (clockContainer.layoutParams as LinearLayout.LayoutParams).apply {
        width = LinearLayout.LayoutParams.MATCH_PARENT
        height = LinearLayout.LayoutParams.WRAP_CONTENT
        leftMargin = dpToPx(16)
        rightMargin = dpToPx(16)
        topMargin = dpToPx(48)
        bottomMargin = dpToPx(16)
    }

    configureMascotAndWeather(
        mascotContainer, ivMascot, ivWeatherIcon, tvWeatherTemp, tvWeatherCondition,
        theme, cachedTemp, cachedIcon, cachedCondition,
        getShowMascot(), true, dpToPx(24)
    )

    applyTextClockStyle(tcTime, theme, 52f, -0.02f, theme.colors.textPrimary)
    applyTextClockStyle(tcDate, theme, 12f, 0.06f, adjustAlpha(theme.colors.textPrimary, 0.6f), isDate = true)

    clockContainer.removeAllViews()
    clockContainer.addView(mascotContainer)
    clockContainer.addView(tcTime)
    clockContainer.addView(tcDate)
}

internal fun MainActivity.applyClockStyleCharacterSplit(
    clockContainer: LinearLayout,
    mascotContainer: LinearLayout,
    ivMascot: ImageView,
    ivWeatherIcon: ImageView?,
    tcTime: TextClock,
    tcDate: TextClock,
    tvWeatherTemp: TextView?,
    tvWeatherCondition: TextView?,
    theme: AppTheme,
    cachedTemp: Int,
    cachedIcon: String,
    cachedCondition: String,
    hasSymbol: Boolean,
    hasCharacter: Boolean,
    view: View
) {
    clockContainer.orientation = LinearLayout.HORIZONTAL
    clockContainer.gravity = Gravity.CENTER_VERTICAL
    clockContainer.layoutParams = (clockContainer.layoutParams as LinearLayout.LayoutParams).apply {
        width = LinearLayout.LayoutParams.WRAP_CONTENT
        height = LinearLayout.LayoutParams.WRAP_CONTENT
        leftMargin = dpToPx(24)
        rightMargin = dpToPx(24)
        topMargin = dpToPx(48)
        bottomMargin = dpToPx(16)
    }
    clockContainer.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
    clockContainer.background = null

    configureMascotAndWeather(
        mascotContainer, ivMascot, ivWeatherIcon, tvWeatherTemp, tvWeatherCondition,
        theme, cachedTemp, cachedIcon, cachedCondition,
        getShowMascot(), true, dpToPx(20)
    )

    applyTextClockStyle(tcTime, theme, 38f, -0.01f, theme.colors.textPrimary)
    applyTextClockStyle(tcDate, theme, 11f, 0.04f, adjustAlpha(theme.colors.textPrimary, 0.6f), isDate = true)

    val rightCol = LinearLayout(view.context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.START
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = dpToPx(16)
        }
    }
    rightCol.addView(tcTime)
    rightCol.addView(tcDate)

    clockContainer.removeAllViews()
    clockContainer.addView(mascotContainer)
    clockContainer.addView(rightCol)
}

internal fun MainActivity.applyMascotVisibility(
    container: LinearLayout,
    ivMascot: ImageView,
    hasSymbol: Boolean,
    hasCharacter: Boolean
) {
    if (!hasSymbol && !hasCharacter) {
        container.visibility = View.GONE
    }
}

internal fun MainActivity.applyTextClockStyle(
    tc: TextClock,
    theme: AppTheme,
    textSizeSp: Float,
    letterSpacing: Float,
    color: Int,
    isDate: Boolean = false
) {
    tc.textSize = textSizeSp
    tc.letterSpacing = letterSpacing
    tc.setTextColor(color)
    tc.typeface = null
    tc.setShadowLayer(0f, 0f, 0f, 0)
    if (isDate) {
        tc.format12Hour = getString(R.string.date_format_display)
        tc.format24Hour = getString(R.string.date_format_display)
    }
}

private fun MainActivity.configureMascotAndWeather(
    mascotContainer: LinearLayout,
    ivMascot: ImageView,
    ivWeatherIcon: ImageView?,
    tvWeatherTemp: TextView?,
    tvWeatherCondition: TextView?,
    theme: AppTheme,
    cachedTemp: Int,
    cachedIcon: String,
    cachedCondition: String,
    showWeather: Boolean,
    isVertical: Boolean,
    weatherIconSizePx: Int
) {
    // 1. Configure Mascot
    ivMascot.clearColorFilter()
    val bmp = theme.symbolBitmap
    if (bmp != null) {
        ivMascot.visibility = View.VISIBLE
        ivMascot.setImageBitmap(bmp)
    } else if (theme.symbol != null) {
        ivMascot.visibility = View.VISIBLE
        ivMascot.setImageDrawable(null)
        ivMascot.load(theme.symbol) {
            memoryCachePolicy(coil.request.CachePolicy.DISABLED)
            diskCachePolicy(coil.request.CachePolicy.DISABLED)
        }
    } else {
        ivMascot.visibility = View.GONE
    }

    // 2. Configure Weather
    if (showWeather) {
        ivWeatherIcon?.visibility = View.VISIBLE
        ivWeatherIcon?.layoutParams = ivWeatherIcon?.layoutParams?.apply {
            width = weatherIconSizePx
            height = weatherIconSizePx
        }
        if (cachedIcon.isNotEmpty()) {
            ivWeatherIcon?.scaleType = ImageView.ScaleType.FIT_CENTER
            ivWeatherIcon?.load(cachedIcon) {
                allowHardware(false)
            }
        } else {
            ivWeatherIcon?.setImageResource(R.drawable.ic_cloud)
            ivWeatherIcon?.setColorFilter(theme.colors.primary)
        }

        tvWeatherTemp?.visibility = View.VISIBLE
        tvWeatherCondition?.visibility = View.VISIBLE

        if (cachedTemp != -999) {
            tvWeatherTemp?.text = "${cachedTemp}°C"
            tvWeatherCondition?.text = if (cachedCondition.isNotEmpty()) cachedCondition else "Weather"
        } else {
            tvWeatherTemp?.text = "--°"
            tvWeatherCondition?.text = "Loading..."
        }
        tvWeatherTemp?.setTextColor(theme.colors.textPrimary)
        tvWeatherCondition?.setTextColor(adjustAlpha(theme.colors.textSecondary, 0.8f))
    } else {
        ivWeatherIcon?.visibility = View.GONE
        tvWeatherTemp?.visibility = View.GONE
        tvWeatherCondition?.visibility = View.GONE
    }

    // 3. Configure Divider and Container Orientation
    val divider = if (mascotContainer.childCount > 1) mascotContainer.getChildAt(1) else null
    val showMascot = ivMascot.visibility == View.VISIBLE
    val showWeatherInfo = showWeather && tvWeatherTemp?.visibility == View.VISIBLE

    if (isVertical) {
        mascotContainer.orientation = LinearLayout.VERTICAL
        mascotContainer.gravity = Gravity.CENTER
        divider?.visibility = View.GONE
        
        tvWeatherTemp?.gravity = Gravity.CENTER
        tvWeatherCondition?.gravity = Gravity.CENTER
        
        ivWeatherIcon?.layoutParams = (ivWeatherIcon?.layoutParams as? LinearLayout.LayoutParams)?.apply {
            topMargin = dpToPx(4)
            bottomMargin = dpToPx(2)
            marginStart = 0
            marginEnd = 0
        }
    } else {
        mascotContainer.orientation = LinearLayout.HORIZONTAL
        mascotContainer.gravity = Gravity.CENTER_VERTICAL
        
        tvWeatherTemp?.gravity = Gravity.START
        tvWeatherCondition?.gravity = Gravity.START
        
        if (showMascot && showWeatherInfo) {
            divider?.visibility = View.VISIBLE
            divider?.setBackgroundColor(adjustAlpha(theme.colors.textSecondary, 0.2f))
        } else {
            divider?.visibility = View.GONE
        }
    }

    // Apply pill background to weather/mascot container
    val weatherBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(24f)
        setColor(adjustAlpha(Color.BLACK, 0.4f))
        if (theme.shapes.showBorders) {
            setStroke(dpToPx(1f).toInt(), adjustAlpha(Color.BLACK, 0.5f))
        }
    }
    mascotContainer.background = weatherBg
    mascotContainer.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))

    // Update main container visibility
    val hasContent = showMascot || showWeatherInfo
    mascotContainer.visibility = if (hasContent) View.VISIBLE else View.GONE
}
