package com.alisu.alauncher.widget

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object WidgetSizeCalculator {

    private const val CELL_SIZE_DP = 70
    private const val CELL_SPACING_DP = 4

    data class WidgetSize(
        val cellsX: Int,
        val cellsY: Int,
        val widthPx: Int,
        val heightPx: Int
    )

    fun calculateSize(context: Context, provider: AppWidgetProviderInfo): WidgetSize {
        val density = context.resources.displayMetrics.density
        val cellSizePx = (CELL_SIZE_DP * density).toInt()
        val cellSpacingPx = (CELL_SPACING_DP * density).toInt()

        val minWidth = provider.minWidth
        val minHeight = provider.minHeight

        val cellsX = max(1, ceil(
            (minWidth + CELL_SPACING_DP).toFloat() / (CELL_SIZE_DP + CELL_SPACING_DP)
        ).toInt())

        val cellsY = max(1, ceil(
            (minHeight + CELL_SPACING_DP).toFloat() / (CELL_SIZE_DP + CELL_SPACING_DP)
        ).toInt())

        val maxColumns = 5
        val maxRows = 6
        val clampedX = min(cellsX, maxColumns)
        val clampedY = min(cellsY, maxRows)

        val widthPx = (clampedX * cellSizePx) + ((clampedX - 1) * cellSpacingPx)
        val heightPx = (clampedY * cellSizePx) + ((clampedY - 1) * cellSpacingPx)

        return WidgetSize(clampedX, clampedY, widthPx, heightPx)
    }

    fun calculateCellsForMinSize(minWidthDp: Int, minHeightDp: Int): Pair<Int, Int> {
        val cellsX = max(1, ceil(
            (minWidthDp + CELL_SPACING_DP).toFloat() / (CELL_SIZE_DP + CELL_SPACING_DP)
        ).toInt())
        val cellsY = max(1, ceil(
            (minHeightDp + CELL_SPACING_DP).toFloat() / (CELL_SIZE_DP + CELL_SPACING_DP)
        ).toInt())
        return Pair(cellsX, cellsY)
    }
}
