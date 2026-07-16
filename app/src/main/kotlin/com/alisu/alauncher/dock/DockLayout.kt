package com.alisu.alauncher.dock

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alisu.alauncher.drag.DropTarget
import com.alisu.alauncher.model.AppInfo

class DockLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr), DropTarget {

    interface OnDropListener {
        fun onDrop(item: AppInfo, targetIdx: Int)
    }

    var onDropListener: OnDropListener? = null
    private val cachedLocation = IntArray(2)
    private var locationCacheValid = false

    fun invalidateLocationCache() {
        locationCacheValid = false
    }

    private fun getCachedLocation(): IntArray {
        if (!locationCacheValid) {
            getLocationOnScreen(cachedLocation)
            locationCacheValid = true
        }
        return cachedLocation
    }

    override fun accepts(item: AppInfo): Boolean {
        return !item.packageName.startsWith("widget_")
    }

    override fun onDrop(item: AppInfo, x: Float, y: Float) {
        val columns = (layoutManager as? GridLayoutManager)?.spanCount ?: 4
        if (width <= 0) return

        val cellWidth = width / columns
        val loc = getCachedLocation()
        val relativeX = x - loc[0]

        val targetIdx = (relativeX / cellWidth).toInt().coerceIn(0, columns - 1)
        onDropListener?.onDrop(item, targetIdx)
    }

    override fun containsPoint(x: Float, y: Float): Boolean {
        if (visibility != View.VISIBLE) return false
        val loc = getCachedLocation()
        return x >= loc[0] && x <= loc[0] + width && y >= loc[1] && y <= loc[1] + height
    }

    override fun onDragEnter(item: AppInfo) {
        val theme = com.alisu.alauncher.theme.ThemeManager.currentTheme.value
        setBackgroundColor(adjustAlpha(theme.colors.primary, 0.08f))
    }

    override fun onDragExit(item: AppInfo) {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(android.graphics.Color.alpha(color) * factor)
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        return android.graphics.Color.argb(alpha, red, green, blue)
    }
}
