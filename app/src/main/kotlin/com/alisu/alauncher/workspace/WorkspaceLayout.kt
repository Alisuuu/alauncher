package com.alisu.alauncher.workspace

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.LAYER_TYPE_HARDWARE
import android.view.View.LAYER_TYPE_NONE
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alisu.alauncher.drag.DropTarget
import com.alisu.alauncher.model.AppInfo

class WorkspaceLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr), DropTarget {

    interface OnDropListener {
        fun onDrop(item: AppInfo, targetIdx: Int)
        fun onDragOver(item: AppInfo, targetIdx: Int)
        fun onDragExit(item: AppInfo)
    }

    var onDropListener: OnDropListener? = null
    private var lastDragOverIdx: Int = -1
    private val cachedLocation = IntArray(2)
    private var locationCacheValid = false
    private var hwLayersEnabled = false
    var isDragActive = false

    override fun onInterceptTouchEvent(e: android.view.MotionEvent?): Boolean {
        if (isDragActive) return false
        return super.onInterceptTouchEvent(e)
    }

    fun setHardwareLayersEnabled(enabled: Boolean) {
        if (hwLayersEnabled == enabled) return
        hwLayersEnabled = enabled
        val layerType = if (enabled) LAYER_TYPE_HARDWARE else LAYER_TYPE_NONE
        for (i in 0 until childCount) {
            getChildAt(i)?.setLayerType(layerType, null)
        }
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state == SCROLL_STATE_IDLE) {
            setHardwareLayersEnabled(false)
        }
    }

    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        if (!hwLayersEnabled && (dx != 0 || dy != 0)) {
            setHardwareLayersEnabled(true)
        }
    }

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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        locationCacheValid = false
        if (h > 0 && h != oldh) {
            post {
                adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun accepts(item: AppInfo): Boolean = true

    override fun onDrop(item: AppInfo, x: Float, y: Float) {
        val targetIdx = getTargetIdx(x, y)
        if (targetIdx != -1) {
            onDropListener?.onDrop(item, targetIdx)
        }
        lastDragOverIdx = -1
    }

    override fun onDragOver(item: AppInfo, x: Float, y: Float) {
        val targetIdx = getTargetIdx(x, y)
        if (targetIdx != lastDragOverIdx) {
            lastDragOverIdx = targetIdx
            if (targetIdx != -1) {
                onDropListener?.onDragOver(item, targetIdx)
            }
        }
    }

    private fun getTargetIdx(x: Float, y: Float): Int {
        val columns = (layoutManager as? GridLayoutManager)?.spanCount ?: 4
        val rows = try {
            context.resources.getInteger(com.alisu.alauncher.R.integer.desktop_grid_rows)
        } catch (_: Exception) {
            5
        }
        if (width <= 0 || height <= 0) return -1

        val cellWidth = width / columns
        val cellHeight = height / rows

        val loc = getCachedLocation()
        val targetCell = CellPosition.fromPixel(x, y, loc[0], loc[1], cellWidth, cellHeight, columns, rows)
        return targetCell?.toLinearIndex(columns) ?: -1
    }

    override fun containsPoint(x: Float, y: Float): Boolean {
        if (visibility != View.VISIBLE) return false
        val loc = getCachedLocation()
        return x >= loc[0] && x <= loc[0] + width && y >= loc[1] && y <= loc[1] + height
    }

    override fun onDragEnter(item: AppInfo) {
        val theme = com.alisu.alauncher.theme.ThemeManager.currentTheme.value
        setBackgroundColor(adjustAlpha(theme.colors.primary, 0.05f))
    }

    override fun onDragExit(item: AppInfo) {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        onDropListener?.onDragExit(item)
        lastDragOverIdx = -1
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(android.graphics.Color.alpha(color) * factor)
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        return android.graphics.Color.argb(alpha, red, green, blue)
    }
}
