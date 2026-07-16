package com.alisu.alauncher.theme

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class AppThemeColors(
    val background: Int,
    val surface: Int,
    val primary: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val glowColor: Int
)

@Serializable
data class AppThemeShapes(
    val cardCornerRadius: Int,
    val searchBarCornerRadius: Int,
    val showBorders: Boolean
)

@Serializable
data class AppThemeLayout(
    val columns: Int,
    val showAppName: Boolean
)

@Serializable
data class AppTheme(
    val id: String,
    val name: String,
    val author: String = "",
    val description: String = "",
    val colors: AppThemeColors,
    val shapes: AppThemeShapes,
    val layout: AppThemeLayout,
    val wallpaper: String? = null,
    val altBackdrop: String? = null,
    val cardXmlPath: String? = null,
    val cardTexture: String? = null,
    val texturePath: String? = null,
    val wallpaperCropX: Float = 50f,
    val wallpaperCropY: Float = 50f,
    val wallpaperScale: Float = 1f,
    @Transient val character: Bitmap? = null,
    val symbol: String? = null,
    @Transient val symbolBitmap: Bitmap? = null
) {
    companion object {
        val Fallback = AppTheme(
            id = "fallback",
            name = "Default Dark",
            author = "Alauncher",
            description = "Default dark theme",
            colors = AppThemeColors(
                background = Color.parseColor("#121212"),
                surface = Color.parseColor("#1E1E1E"),
                primary = Color.parseColor("#BB86FC"),
                textPrimary = Color.parseColor("#FFFFFF"),
                textSecondary = Color.parseColor("#888888"),
                glowColor = Color.parseColor("#BB86FC")
            ),
            shapes = AppThemeShapes(
                cardCornerRadius = 12,
                searchBarCornerRadius = 24,
                showBorders = false
            ),
            layout = AppThemeLayout(
                columns = 4,
                showAppName = true
            ),
            wallpaper = null,
            cardXmlPath = null,
            cardTexture = null,
            texturePath = null,
            character = null,
            symbol = null,
            symbolBitmap = null,
            wallpaperCropX = 50f,
            wallpaperCropY = 50f,
            wallpaperScale = 1f
        )
    }
}
