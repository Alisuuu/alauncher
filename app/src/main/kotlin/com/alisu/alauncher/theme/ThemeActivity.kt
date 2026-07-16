package com.alisu.alauncher.theme

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alisu.alauncher.R
import coil.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThemeActivity : AppCompatActivity() {

    private lateinit var rvThemes: RecyclerView
    private lateinit var ivWallpaper: ImageView
    private lateinit var btnImport: View
    private lateinit var adapter: ThemeAdapter

    private val importThemeLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val themeId = withContext(Dispatchers.IO) {
                ThemeManager.extractZipTheme(this@ThemeActivity, uri)
            }
            if (themeId != null) {
                Toast.makeText(this@ThemeActivity, getString(R.string.theme_imported), Toast.LENGTH_SHORT).show()
                if (themeId == ThemeManager.currentTheme.value.id) {
                    ThemeManager.loadTheme(this@ThemeActivity, themeId)
                }
                refreshThemes()
            } else {
                Toast.makeText(this@ThemeActivity, getString(R.string.theme_import_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        System.gc()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContentView(R.layout.activity_themes)

        rvThemes = findViewById(R.id.rv_themes)
        ivWallpaper = findViewById(R.id.iv_themes_wallpaper)
        btnImport = findViewById(R.id.btn_themes_import)

        ViewCompat.setOnApplyWindowInsetsListener(rvThemes) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            val lp = btnImport.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                lp.bottomMargin = systemBars.bottom + (24 * resources.displayMetrics.density).toInt()
                btnImport.layoutParams = lp
            }
            insets
        }

        rvThemes.layoutManager = LinearLayoutManager(this)
        adapter = ThemeAdapter(
            onThemeSelected = { themeId ->
                getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .remove("card_corner_radius_override")
                    .apply()
                ThemeManager.loadTheme(this, themeId)
            },
            onThemeDelete = { themeId ->
                if (ThemeManager.deleteTheme(this, themeId)) {
                    Toast.makeText(this, getString(R.string.theme_deleted), Toast.LENGTH_SHORT).show()
                    refreshThemes()
                }
            }
        )
        rvThemes.adapter = adapter

        btnImport.setOnClickHaptic {
            importThemeLauncher.launch("*/*")
        }

        lifecycleScope.launch {
            ThemeManager.currentTheme.collectLatest { theme ->
                applyTheme(theme)
                adapter.setCurrentThemeId(theme.id)
            }
        }

        refreshThemes()
    }

    private fun refreshThemes() {
        lifecycleScope.launch {
            val themes = withContext(Dispatchers.IO) {
                val themeIds = ThemeManager.getAvailableThemes(this@ThemeActivity)
                themeIds.mapNotNull { ThemeManager.getThemeMetadata(this@ThemeActivity, it) }
            }
            adapter.submitList(themes)
        }
    }

    private fun applyTheme(theme: AppTheme) {
        if (theme.wallpaper != null) {
            ivWallpaper.load(theme.wallpaper) {
                bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                allowHardware(true)
                size(coil.size.ViewSizeResolver(ivWallpaper))
            }
        } else {
            ivWallpaper.setBackgroundColor(theme.colors.background)
        }

        findViewById<ImageView>(R.id.iv_themes_import_icon).setColorFilter(theme.colors.primary)
    }

    inner class ThemeAdapter(
        private val onThemeSelected: (String) -> Unit,
        private val onThemeDelete: (String) -> Unit
    ) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

        private var themes: List<AppTheme> = emptyList()
        private var currentThemeId: String = ""

        fun submitList(newList: List<AppTheme>) {
            val diffCallback = object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = themes.size
                override fun getNewListSize(): Int = newList.size
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return themes[oldItemPosition].id == newList[newItemPosition].id
                }
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return themes[oldItemPosition] == newList[newItemPosition]
                }
            }
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            themes = newList
            diffResult.dispatchUpdatesTo(this)
        }

        fun setCurrentThemeId(id: String) {
            if (currentThemeId != id) {
                val oldIdx = themes.indexOfFirst { it.id == currentThemeId }
                val newIdx = themes.indexOfFirst { it.id == id }
                currentThemeId = id
                if (oldIdx != -1) notifyItemChanged(oldIdx)
                if (newIdx != -1) notifyItemChanged(newIdx)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_theme, parent, false)
            return ThemeViewHolder(view)
        }

        override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
            holder.bind(themes[position])
        }

        override fun getItemCount(): Int = themes.size

        inner class ThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvName: TextView = itemView.findViewById(R.id.tv_item_theme_name)
            private val tvAuthor: TextView = itemView.findViewById(R.id.tv_item_theme_author)
            private val tvDescription: TextView = itemView.findViewById(R.id.tv_item_theme_description)
            private val ivCheck: ImageView = itemView.findViewById(R.id.iv_item_theme_check)
            private val btnDelete: ImageView = itemView.findViewById(R.id.btn_item_theme_delete)
            private val ivWallpaperPreview: ImageView = itemView.findViewById(R.id.iv_preview_wallpaper)
            private val ivTexturePreview: ImageView = itemView.findViewById(R.id.iv_preview_texture)
            private val containerWallpaper: View = itemView.findViewById(R.id.container_preview_wallpaper)
            private val containerTexture: View = itemView.findViewById(R.id.container_preview_texture)
            private val viewColorPrimary: View = itemView.findViewById(R.id.view_color_primary)
            private val viewColorBg: View = itemView.findViewById(R.id.view_color_background)
            private val viewColorSurface: View = itemView.findViewById(R.id.view_color_surface)
            private val viewColorText: View = itemView.findViewById(R.id.view_color_text)
            private val container: View = itemView.findViewById(R.id.item_theme_container)

            fun bind(theme: AppTheme) {
                tvName.text = theme.name
                tvAuthor.text = if (theme.author.isNotEmpty()) "by ${theme.author}" else ""
                tvAuthor.visibility = if (theme.author.isNotEmpty()) View.VISIBLE else View.GONE
                tvDescription.text = theme.description
                tvDescription.visibility = if (theme.description.isNotEmpty()) View.VISIBLE else View.GONE
                val isSelected = theme.id == currentThemeId
                ivCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

                btnDelete.visibility = if (theme.id != "base" && theme.id != "neon") View.VISIBLE else View.GONE
                btnDelete.setOnClickHaptic { onThemeDelete(theme.id) }

                itemView.setOnClickHaptic { onThemeSelected(theme.id) }

                if (theme.wallpaper != null) {
                    val previewSize = (64 * itemView.resources.displayMetrics.density).toInt()
                    ivWallpaperPreview.load(theme.wallpaper) {
                        size(previewSize, previewSize)
                        bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                        allowHardware(true)
                    }
                } else {
                    ivWallpaperPreview.setImageDrawable(null)
                    ivWallpaperPreview.setBackgroundColor(theme.colors.background)
                }

                var texPath = theme.cardXmlPath
                if (texPath == null && theme.texturePath != null) {
                    val texturesDir = java.io.File(itemView.context.filesDir, "themes/${theme.id}")
                    val textureFile = java.io.File(texturesDir, theme.texturePath)
                    if (textureFile.exists()) {
                        texPath = "file://${textureFile.absolutePath}"
                    }
                }

                val previewRadius = theme.shapes.cardCornerRadius.toFloat() * itemView.resources.displayMetrics.density
                val previewBg = GradientDrawable().apply {
                    cornerRadius = previewRadius
                    setColor(0x33FFFFFF)
                }
                containerWallpaper.background = previewBg

                if (texPath != null) {
                    val customDrawable = ThemeDrawableLoader.loadDrawable(itemView.context, texPath)
                    containerTexture.background = customDrawable ?: previewBg
                } else {
                    containerTexture.setBackgroundColor(theme.colors.surface)
                }

                if (theme.cardTexture != null) {
                    val resId = itemView.context.resources.getIdentifier(theme.cardTexture, "drawable", itemView.context.packageName)
                    if (resId != 0) {
                        ivTexturePreview.setImageResource(resId)
                        ivTexturePreview.alpha = 1.0f
                        ivTexturePreview.setColorFilter(adjustAlpha(theme.colors.primary, 0.35f))
                    } else {
                        ivTexturePreview.setImageDrawable(null)
                    }
                } else {
                    ivTexturePreview.setImageDrawable(null)
                }

                containerWallpaper.clipToOutline = true
                containerWallpaper.outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, previewRadius)
                    }
                }

                containerTexture.clipToOutline = true
                containerTexture.outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, previewRadius)
                    }
                }

                setupColorView(viewColorPrimary, theme.colors.primary)
                setupColorView(viewColorBg, theme.colors.background)
                setupColorView(viewColorSurface, theme.colors.surface)
                setupColorView(viewColorText, theme.colors.textPrimary)

                val bg = GradientDrawable().apply {
                    cornerRadius = theme.shapes.cardCornerRadius.toFloat() * itemView.resources.displayMetrics.density
                    setColor(if (isSelected) adjustAlpha(theme.colors.primary, 0.1f) else adjustAlpha(theme.colors.surface, 0.3f))
                    if (isSelected) {
                        setStroke((2 * itemView.resources.displayMetrics.density).toInt(), theme.colors.primary)
                    } else if (ThemeManager.currentTheme.value.shapes.showBorders) {
                        setStroke((1 * itemView.resources.displayMetrics.density).toInt(), adjustAlpha(theme.colors.textSecondary, 0.2f))
                    }
                }
                container.background = bg
                container.addRipple(adjustAlpha(theme.colors.primary, 0.15f))

                tvName.setTextColor(if (isSelected) theme.colors.primary else theme.colors.textPrimary)
                ivCheck.setColorFilter(theme.colors.primary)
                btnDelete.setColorFilter(theme.colors.textSecondary)
            }

            private fun setupColorView(view: View, color: Int) {
                val gd = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(1, 0x44FFFFFF.toInt())
                }
                view.background = gd
            }

            private fun adjustAlpha(color: Int, factor: Float): Int {
                val alpha = Math.round(android.graphics.Color.alpha(color) * factor)
                val red = android.graphics.Color.red(color)
                val green = android.graphics.Color.green(color)
                val blue = android.graphics.Color.blue(color)
                return android.graphics.Color.argb(alpha, red, green, blue)
            }
        }
    }

    private fun View.setOnClickHaptic(action: () -> Unit) {
        this.setOnClickListener { v ->
            v.animate().scaleX(0.94f).scaleY(0.94f)
                .setDuration(100)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    v.animate().scaleX(1.0f).scaleY(1.0f)
                        .setDuration(180)
                        .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                        .start()
                }.start()
            action()
        }
    }

    private fun View.addRipple(rippleColor: Int = 0x20FFFFFF) {
        val currentBg = this.background ?: return
        val rippleColorStateList = android.content.res.ColorStateList.valueOf(rippleColor)
        val rippleDrawable = android.graphics.drawable.RippleDrawable(rippleColorStateList, currentBg, currentBg)
        this.background = rippleDrawable
    }
}
