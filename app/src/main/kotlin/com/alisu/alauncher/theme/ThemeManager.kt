package com.alisu.alauncher.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream

object ThemeManager {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @kotlinx.serialization.Serializable
    data class ThemeJson(
        val name: String = "Unnamed Theme",
        val author: String = "",
        val description: String = "",
        val colors: ThemeColorsJson? = null,
        val shapes: ThemeShapesJson? = null,
        val layout: ThemeLayoutJson? = null,
        val wallpaperPath: String = "",
        val characterPath: String = "",
        val cardTexture: String = "grid",
        val useCardTexture: Boolean = true,
        val cardXmlPath: String? = null,
        val texturePath: String = "",
        val symbolPath: String = "",
        val wallpaperCropX: Double = 50.0,
        val wallpaperCropY: Double = 50.0,
        val wallpaperScale: Double = 1.0,
        val altBackdropPath: String = ""
    )

    @kotlinx.serialization.Serializable
    data class ThemeColorsJson(
        val background: String = "#121212",
        val surface: String = "#1E1E1E",
        val primary: String = "#BB86FC",
        val textPrimary: String = "#FFFFFF",
        val textSecondary: String = "#888888",
        val glowColor: String = "#BB86FC"
    )

    @kotlinx.serialization.Serializable
    data class ThemeShapesJson(
        val cardCornerRadius: Int = 12,
        val searchBarCornerRadius: Int = 24,
        val showBorders: Boolean = false
    )

    @kotlinx.serialization.Serializable
    data class ThemeLayoutJson(
        val columns: Int = 4,
        val showAppName: Boolean = true
    )

    private val _currentTheme = MutableStateFlow(AppTheme.Fallback)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    fun getAvailableThemes(context: Context): List<String> {
        val themes = mutableListOf("base")
        val themesDir = java.io.File(context.filesDir, "themes")
        if (themesDir.exists() && themesDir.isDirectory) {
            themesDir.listFiles { file -> file.isDirectory }?.forEach { dir ->
                if (java.io.File(dir, "theme.json").exists()) {
                    if (!themes.contains(dir.name)) {
                        themes.add(dir.name)
                    }
                }
            }
        }
        return themes
    }

    fun getThemeMetadata(context: Context, themeId: String): AppTheme? {
        return if (themeId == "base") {
            parseThemeFromAssets(context, themeId)
        } else {
            parseThemeFromFiles(context, themeId)
        }
    }

    fun deleteTheme(context: Context, themeId: String): Boolean {
        if (themeId == "base") return false
        val themesDir = java.io.File(context.filesDir, "themes/$themeId")
        if (themesDir.exists()) {
            val deleted = themesDir.deleteRecursively()
            if (deleted && _currentTheme.value.id == themeId) {
                loadTheme(context, "base")
            }
            return deleted
        }
        return false
    }

    suspend fun init(context: Context) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
            val themeId = prefs.getString("theme_id", "base") ?: "base"
            loadTheme(context, themeId)
        }
    }

    fun loadTheme(context: Context, themeId: String) {
        val oldTheme = _currentTheme.value
        val theme = if (themeId == "base") {
            parseThemeFromAssets(context, themeId)
        } else {
            parseThemeFromFiles(context, themeId)
        }
        if (theme != null) {
            ThemeDrawableLoader.clearCache()
            com.alisu.alauncher.ui.MonochromaticTransformation.clearCache()
            val prefs = context.getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
            val overrideRadius = prefs.getInt("card_corner_radius_override", -1)
            val finalTheme = if (overrideRadius >= 0) {
                theme.copy(shapes = theme.shapes.copy(cardCornerRadius = overrideRadius))
            } else {
                theme
            }
            _currentTheme.value = finalTheme
            prefs.edit().putString("theme_id", themeId).apply()
        }
    }

    fun updateCardCornerRadius(context: Context, radius: Int) {
        val prefs = context.getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("card_corner_radius_override", radius).apply()
        
        val current = _currentTheme.value
        if (current.shapes.cardCornerRadius != radius) {
            ThemeDrawableLoader.clearCache()
            com.alisu.alauncher.ui.MonochromaticTransformation.clearCache()
            _currentTheme.value = current.copy(
                shapes = current.shapes.copy(cardCornerRadius = radius)
            )
        }
    }

    private fun parseThemeJson(jsonString: String, id: String, cardXmlPath: String?, wallpaper: String?, altBackdrop: String?, character: Bitmap?, symbol: String?, symbolBitmap: Bitmap?): AppTheme? {
        return try {
            val t = json.decodeFromString<ThemeJson>(jsonString)

            val colors = AppThemeColors(
                background = parseHexColor(t.colors?.background, Color.parseColor("#121212")),
                surface = parseHexColor(t.colors?.surface, Color.parseColor("#1E1E1E")),
                primary = parseHexColor(t.colors?.primary, Color.parseColor("#BB86FC")),
                textPrimary = parseHexColor(t.colors?.textPrimary, Color.parseColor("#FFFFFF")),
                textSecondary = parseHexColor(t.colors?.textSecondary, Color.parseColor("#888888")),
                glowColor = parseHexColor(t.colors?.glowColor, Color.parseColor("#BB86FC"))
            )

            val shapes = AppThemeShapes(
                cardCornerRadius = t.shapes?.cardCornerRadius ?: 12,
                searchBarCornerRadius = t.shapes?.searchBarCornerRadius ?: 24,
                showBorders = t.shapes?.showBorders ?: false
            )

            val layout = AppThemeLayout(
                columns = t.layout?.columns ?: 4,
                showAppName = t.layout?.showAppName ?: true
            )

            val cardTexture = if (t.useCardTexture) "card_texture_${t.cardTexture}" else null
            val finalWallpaper = wallpaper ?: t.wallpaperPath.takeIf { it.isNotEmpty() }
            val finalSymbol = symbol ?: t.symbolPath.takeIf { it.isNotEmpty() }
            val texturePath = t.texturePath.takeIf { it.isNotEmpty() }

            AppTheme(
                id = id,
                name = t.name,
                author = t.author,
                description = t.description,
                colors = colors,
                shapes = shapes,
                layout = layout,
                wallpaper = finalWallpaper,
                altBackdrop = altBackdrop,
                cardXmlPath = cardXmlPath,
                cardTexture = cardTexture,
                texturePath = texturePath,
                character = character,
                symbol = finalSymbol,
                symbolBitmap = symbolBitmap,
                wallpaperCropX = t.wallpaperCropX.toFloat(),
                wallpaperCropY = t.wallpaperCropY.toFloat(),
                wallpaperScale = t.wallpaperScale.toFloat()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseThemeFromAssets(context: Context, themeId: String): AppTheme? {
        return try {
            val jsonPath = "themes/$themeId/theme.json"
            val jsonString = context.assets.open(jsonPath).bufferedReader().use { it.readText() }

            val assetXmlPath = "themes/$themeId/texture.xml"
            val cardXmlPath = try { context.assets.open(assetXmlPath).close(); "file:///android_asset/$assetXmlPath" } catch (e: Exception) { null }

            val t = json.decodeFromString<ThemeJson>(jsonString)
            val wallpaper = t.wallpaperPath.takeIf { it.isNotEmpty() }?.let { "file:///android_asset/$it" }
            val altBackdrop = t.altBackdropPath.takeIf { it.isNotEmpty() }?.let { "file:///android_asset/$it" }
            val character = t.characterPath.takeIf { it.isNotEmpty() }?.let {
                try { loadDownscaledBitmap(context.assets.open(it), 512) } catch (_: Exception) { null }
            }
            val symbolBitmap = t.symbolPath.takeIf { it.isNotEmpty() }?.let {
                try { loadDownscaledBitmap(context.assets.open(it), 512) } catch (_: Exception) { null }
            }
            val symbol = t.symbolPath.takeIf { it.isNotEmpty() }?.let { "file:///android_asset/$it" }

            parseThemeJson(jsonString, themeId, cardXmlPath, wallpaper, altBackdrop, character, symbol, symbolBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseThemeFromFiles(context: Context, themeId: String): AppTheme? {
        return try {
            val themesDir = java.io.File(context.filesDir, "themes/$themeId")
            val jsonFile = java.io.File(themesDir, "theme.json")
            if (!jsonFile.exists()) return null

            val jsonString = jsonFile.readText()
            val t = json.decodeFromString<ThemeJson>(jsonString)

            val cardXmlPath = t.cardXmlPath?.let { fileName ->
                java.io.File(themesDir, fileName).takeIf { it.exists() }?.let { "file://${it.absolutePath}" }
            }
            val wallpaper = t.wallpaperPath.takeIf { it.isNotEmpty() }?.let { path ->
                java.io.File(themesDir, path).takeIf { it.exists() }?.let { "file://${it.absolutePath}?t=${System.currentTimeMillis()}" }
            }
            val altBackdrop = t.altBackdropPath.takeIf { it.isNotEmpty() }?.let { path ->
                java.io.File(themesDir, path).takeIf { it.exists() }?.let { "file://${it.absolutePath}?t=${System.currentTimeMillis()}" }
            }
            val character = t.characterPath.takeIf { it.isNotEmpty() }?.let { path ->
                java.io.File(themesDir, path).takeIf { it.exists() }?.let { file ->
                    try { loadDownscaledBitmap(file.inputStream(), 512) } catch (_: Exception) { null }
                }
            }
            val symbolBitmap = t.symbolPath.takeIf { it.isNotEmpty() }?.let { path ->
                java.io.File(themesDir, path).takeIf { it.exists() }?.let { file ->
                    try { loadDownscaledBitmap(file.inputStream(), 512) } catch (_: Exception) { null }
                }
            }
            val symbol = t.symbolPath.takeIf { it.isNotEmpty() }?.let { path ->
                java.io.File(themesDir, path).takeIf { it.exists() }?.let { "file://${it.absolutePath}" }
            }

            parseThemeJson(jsonString, themeId, cardXmlPath, wallpaper, altBackdrop, character, symbol, symbolBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseHexColor(hex: String?, default: Int): Int {
        if (hex.isNullOrEmpty()) return default
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            default
        }
    }

    private fun loadDownscaledBitmap(inputStream: InputStream, maxDimension: Int): Bitmap? {
        return try {
            val buffered = if (inputStream is java.io.BufferedInputStream) inputStream
                          else java.io.BufferedInputStream(inputStream, 16 * 1024)
            buffered.mark(16 * 1024)

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(buffered, null, options)

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            var inSampleSize = 1
            if (srcWidth > maxDimension || srcHeight > maxDimension) {
                val halfWidth = srcWidth / 2
                val halfHeight = srcHeight / 2
                while ((halfWidth / inSampleSize) >= maxDimension && (halfHeight / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            buffered.reset()

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val sampledBitmap = BitmapFactory.decodeStream(buffered, null, decodeOptions)

            if (sampledBitmap != null) {
                val width = sampledBitmap.width
                val height = sampledBitmap.height
                val scale = maxDimension.toFloat() / Math.max(width, height)
                if (scale < 1f) {
                    val dstWidth = (width * scale).toInt()
                    val dstHeight = (height * scale).toInt()
                    val scaled = Bitmap.createScaledBitmap(sampledBitmap, dstWidth, dstHeight, true)
                    if (scaled !== sampledBitmap) {
                        sampledBitmap.recycle()
                    }
                    scaled
                } else {
                    sampledBitmap
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { inputStream.close() } catch (_: Exception) {}
        }
    }

    private fun makeTransparent(bitmap: Bitmap, threshold: Int = 230): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val corners = intArrayOf(
            pixels[0],
            pixels[width - 1],
            pixels[(height - 1) * width],
            pixels[(height - 1) * width + (width - 1)]
        )
        val avgR = corners.map { Color.red(it) }.average().toInt()
        val avgG = corners.map { Color.green(it) }.average().toInt()
        val avgB = corners.map { Color.blue(it) }.average().toInt()
        val isDarkBg = avgR < 50 && avgG < 50 && avgB < 50

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val a = Color.alpha(color)
            if (a > 0) {
                if (isDarkBg) {
                    if (r < 40 && g < 40 && b < 40) {
                        pixels[i] = Color.TRANSPARENT
                    }
                } else {
                    if (r > threshold && g > threshold && b > threshold) {
                        pixels[i] = Color.TRANSPARENT
                    }
                }
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()
        return result
    }

    fun extractZipTheme(context: Context, zipUri: android.net.Uri): String? {
        val resolver = context.contentResolver
        var tempFile: java.io.File? = null
        try {
            tempFile = java.io.File.createTempFile("theme_import", ".zip", context.cacheDir)
            resolver.openInputStream(zipUri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val tempExtractDir = java.io.File(context.cacheDir, "temp_theme_extract")
            if (tempExtractDir.exists()) tempExtractDir.deleteRecursively()
            tempExtractDir.mkdirs()

            java.util.zip.ZipInputStream(tempFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val destFile = java.io.File(tempExtractDir, entry.name)
                    if (!destFile.canonicalPath.startsWith(tempExtractDir.canonicalPath)) {
                        throw SecurityException("Invalid zip entry path: " + entry.name)
                    }
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        destFile.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val themeJsonFile = java.io.File(tempExtractDir, "theme.json")
            if (!themeJsonFile.exists()) {
                val subDirs = tempExtractDir.listFiles { file -> file.isDirectory }
                var nestedJsonFile: java.io.File? = null
                var nestedDir: java.io.File? = null
                if (subDirs != null) {
                    for (dir in subDirs) {
                        val possibleJson = java.io.File(dir, "theme.json")
                        if (possibleJson.exists()) {
                            nestedJsonFile = possibleJson
                            nestedDir = dir
                            break
                        }
                    }
                }
                if (nestedJsonFile == null || nestedDir == null) {
                    return null
                }

                processThemeDirectory(context, nestedDir)

                val jsonString = nestedJsonFile.readText()
                val themeId = json.decodeFromString<ThemeJson>(jsonString).name.takeIf { it.isNotBlank() } ?: "custom_theme"
                if (themeId.isEmpty()) return null

                val finalThemeDir = java.io.File(context.filesDir, "themes/$themeId")
                if (finalThemeDir.exists()) finalThemeDir.deleteRecursively()
                val renamed = nestedDir.renameTo(finalThemeDir)
                if (!renamed) {
                    nestedDir.copyRecursively(finalThemeDir, overwrite = true)
                    nestedDir.deleteRecursively()
                }
                return themeId
            } else {
                processThemeDirectory(context, tempExtractDir)

                val jsonString = themeJsonFile.readText()
                val themeId = json.decodeFromString<ThemeJson>(jsonString).name.takeIf { it.isNotBlank() } ?: "custom_theme"
                if (themeId.isEmpty()) return null

                val finalThemeDir = java.io.File(context.filesDir, "themes/$themeId")
                if (finalThemeDir.exists()) finalThemeDir.deleteRecursively()
                val renamed = tempExtractDir.renameTo(finalThemeDir)
                if (!renamed) {
                    tempExtractDir.copyRecursively(finalThemeDir, overwrite = true)
                    tempExtractDir.deleteRecursively()
                }
                return themeId
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            tempFile?.delete()
        }
    }

    private fun processThemeDirectory(context: Context, themeDir: java.io.File) {
        val jsonFile = java.io.File(themeDir, "theme.json")
        if (!jsonFile.exists()) return
        try {
            val jsonString = jsonFile.readText()
            val t = json.decodeFromString<ThemeJson>(jsonString)

            for (path in listOf(t.characterPath, t.symbolPath)) {
                if (path.isNotEmpty()) {
                    val file = java.io.File(themeDir, path)
                    if (file.exists()) compressImageIfNeeded(file, 70L * 1024L, isPng = true)
                }
            }
            if (t.wallpaperPath.isNotEmpty()) {
                val wallpaperFile = java.io.File(themeDir, t.wallpaperPath)
                if (wallpaperFile.exists()) {
                    val isPng = t.wallpaperPath.lowercase().endsWith(".png")
                    compressImageIfNeeded(wallpaperFile, 1024L * 1024L, isPng = isPng)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun compressImageIfNeeded(file: java.io.File, maxSizeBytes: Long, isPng: Boolean) {
        if (!file.exists()) return
        val fileSize = file.length()
        if (fileSize <= maxSizeBytes) return

        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.absolutePath, options)
            
            var scale = 1
            val maxDim = if (isPng) 1024 else 2048
            while (options.outWidth / scale > maxDim || options.outHeight / scale > maxDim) {
                scale *= 2
            }
            
            options.inJustDecodeBounds = false
            options.inSampleSize = scale
            var bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return

            var quality = 90
            val outputStream = java.io.ByteArrayOutputStream()
            val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            
            bitmap.compress(format, quality, outputStream)
            
            if (isPng) {
                var width = bitmap.width
                var height = bitmap.height
                while (outputStream.size() > maxSizeBytes && width > 100 && height > 100) {
                    width = (width * 0.8f).toInt()
                    height = (height * 0.8f).toInt()
                    val scaledBmp = Bitmap.createScaledBitmap(bitmap, width, height, true)
                    if (bitmap != scaledBmp) {
                        bitmap.recycle()
                        bitmap = scaledBmp
                    }
                    outputStream.reset()
                    bitmap.compress(format, 100, outputStream)
                }
            } else {
                while (outputStream.size() > maxSizeBytes && quality > 40) {
                    quality -= 10
                    outputStream.reset()
                    bitmap.compress(format, quality, outputStream)
                }
                var width = bitmap.width
                var height = bitmap.height
                while (outputStream.size() > maxSizeBytes && width > 300 && height > 300) {
                    width = (width * 0.8f).toInt()
                    height = (height * 0.8f).toInt()
                    val scaledBmp = Bitmap.createScaledBitmap(bitmap, width, height, true)
                    if (bitmap != scaledBmp) {
                        bitmap.recycle()
                        bitmap = scaledBmp
                    }
                    outputStream.reset()
                    bitmap.compress(format, quality, outputStream)
                }
            }

            file.outputStream().use { out ->
                outputStream.writeTo(out)
            }
            bitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
