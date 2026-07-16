package com.alisu.alauncher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.CompoundButtonCompat
import androidx.lifecycle.lifecycleScope
import com.alisu.alauncher.theme.AppTheme
import com.alisu.alauncher.theme.ThemeManager
import coil.dispose
import coil.load
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred

class SettingsActivity : AppCompatActivity() {

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
    }

    private lateinit var rootLayout: View
    private lateinit var ivWallpaper: ImageView
    private lateinit var viewOverlay: View

    private lateinit var tvSecThemes: TextView
    private lateinit var cardThemesContainer: LinearLayout
    private lateinit var btnManageThemes: LinearLayout
    private lateinit var ivManageThemesIcon: ImageView
    private lateinit var tvManageThemesTitle: TextView
    private lateinit var tvManageThemesSub: TextView
    private lateinit var ivManageThemesArrow: ImageView

    private lateinit var tvSecDesktop: TextView
    private lateinit var cardDesktopSettings: LinearLayout

    private lateinit var btnManageIconPacks: View
    private lateinit var tvIconPacksTitle: TextView
    private lateinit var tvIconPacksSub: TextView
    private lateinit var ivIconPacksIcon: ImageView
    private lateinit var ivIconPacksArrow: ImageView

    private lateinit var tvLabelClockStyle: TextView
    private lateinit var llClockStyleSelector: LinearLayout

    private lateinit var tvLabelDrawerStyle: TextView
    private lateinit var llDrawerStyleSelector: LinearLayout

    private lateinit var tvLabelShowAppNames: TextView
    private lateinit var tvSubShowAppNames: TextView
    private lateinit var swShowAppNames: SwitchCompat

    private lateinit var tvLabelShowMascot: TextView
    private lateinit var tvSubShowMascot: TextView
    private lateinit var swShowMascot: SwitchCompat

    private lateinit var tvLabelWeatherLocation: TextView
    private lateinit var tvSubWeatherLocation: TextView
    private lateinit var rlWeatherLocation: RelativeLayout

    private lateinit var tvLabelSetDefaultLauncher: TextView
    private lateinit var tvSubSetDefaultLauncher: TextView
    private lateinit var rlSetDefaultLauncher: RelativeLayout
    private lateinit var ivArrowSetDefaultLauncher: ImageView

    private lateinit var tvLabelResetLauncher: TextView
    private lateinit var tvSubResetLauncher: TextView
    private lateinit var rlResetLauncher: RelativeLayout
    private lateinit var ivResetLauncherIcon: ImageView

    private lateinit var tvSecIcons: TextView
    private lateinit var cardIconSettings: LinearLayout
    private lateinit var tvLabelIconSize: TextView
    private lateinit var llIconSizeSelector: LinearLayout
    private lateinit var tvLabelIconShape: TextView
    private lateinit var llIconShapeSelector: LinearLayout
    private lateinit var tvLabelMonochromatic: TextView
    private lateinit var tvSubMonochromatic: TextView
    private lateinit var swMonochromatic: SwitchCompat

    private lateinit var divCardCornerRadius: View
    private lateinit var llCardCornerRadiusContainer: LinearLayout
    private lateinit var ivCardCornerRadiusIcon: ImageView
    private lateinit var tvLabelCardCornerRadius: TextView
    private lateinit var tvValueCardCornerRadius: TextView
    private lateinit var sbCardCornerRadius: SeekBar

    private lateinit var cardPermissions: LinearLayout

    private lateinit var cardThemeShowcase: LinearLayout
    private lateinit var ivThemeShowcaseWallpaper: ImageView
    private lateinit var viewThemeShowcaseOverlay: View
    private lateinit var ivThemeShowcaseSymbol: ImageView
    private var shakeAnimator: android.animation.ValueAnimator? = null


    private val clockStyleOptions: Array<String> by lazy {
        arrayOf(
            "Glass Card", getString(R.string.clock_strip),
            "Terminal", "Character Card", "Character Split", getString(R.string.clock_hidden)
        )
    }
    private val clockStyleKeys = arrayOf(
        "default", "symbol_horizontal",
        "bracket", "character_card", "character_split", "hidden"
    )

    private val iconSizeOptions: Array<String> by lazy {
        arrayOf(getString(R.string.size_small), getString(R.string.size_medium), getString(R.string.size_large))
    }
    private val iconSizeKeys = arrayOf("small", "medium", "large")

    private val iconShapeOptions: Array<String> by lazy {
        arrayOf(getString(R.string.shape_default), getString(R.string.shape_circle), getString(R.string.shape_square), getString(R.string.shape_squircle))
    }
    private val iconShapeKeys = arrayOf("theme", "circle", "square", "squircle")

    private val iconShapeModeOptions: Array<String> by lazy {
        arrayOf(getString(R.string.icon_shape_mode_force), getString(R.string.icon_shape_mode_follow), getString(R.string.icon_shape_mode_original))
    }
    private val iconShapeModeKeys = arrayOf("force_launcher", "follow_pack", "original")

    private val drawerStyleOptions: Array<String> by lazy {
        arrayOf(getString(R.string.drawer_standard), getString(R.string.drawer_orbital))
    }
    private val drawerStyleKeys = arrayOf("standard", "orbital")

    private var cachedItemBg: GradientDrawable? = null
    private var cachedSwitchThumb: ColorStateList? = null
    private var cachedSwitchTrack: ColorStateList? = null
    private var lastAppliedThemeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_settings)

        rootLayout = findViewById(R.id.settings_root)
        ivWallpaper = findViewById(R.id.iv_settings_wallpaper)
        viewOverlay = findViewById(R.id.view_settings_overlay)

        tvSecThemes = findViewById(R.id.tv_sec_themes)
        cardThemesContainer = findViewById(R.id.card_themes_container)
        btnManageThemes = findViewById(R.id.btn_manage_themes)
        ivManageThemesIcon = findViewById(R.id.iv_manage_themes_icon)
        tvManageThemesTitle = findViewById(R.id.tv_manage_themes_title)
        tvManageThemesSub = findViewById(R.id.tv_manage_themes_sub)
        ivManageThemesArrow = findViewById(R.id.iv_manage_themes_arrow)

        btnManageIconPacks = findViewById(R.id.btn_manage_icon_packs)
        tvIconPacksTitle = findViewById(R.id.tv_icon_packs_title)
        tvIconPacksSub = findViewById(R.id.tv_icon_packs_sub)
        ivIconPacksIcon = findViewById(R.id.iv_icon_packs_icon)
        ivIconPacksArrow = findViewById(R.id.iv_icon_packs_arrow)

        tvSecDesktop = findViewById(R.id.tv_sec_desktop)
        cardDesktopSettings = findViewById(R.id.card_desktop_settings)

        tvLabelClockStyle = findViewById(R.id.tv_label_clock_style)
        llClockStyleSelector = findViewById(R.id.ll_clock_style_selector)

        tvLabelDrawerStyle = findViewById(R.id.tv_label_drawer_style)
        llDrawerStyleSelector = findViewById(R.id.ll_drawer_style_selector)

        tvLabelShowAppNames = findViewById(R.id.tv_label_show_app_names)
        tvSubShowAppNames = findViewById(R.id.tv_sub_show_app_names)
        swShowAppNames = findViewById(R.id.sw_show_app_names)

        tvLabelShowMascot = findViewById(R.id.tv_label_show_mascot)
        tvSubShowMascot = findViewById(R.id.tv_sub_show_mascot)
        swShowMascot = findViewById(R.id.sw_show_mascot)

        tvLabelWeatherLocation = findViewById(R.id.tv_label_weather_location)
        tvSubWeatherLocation = findViewById(R.id.tv_sub_weather_location)
        rlWeatherLocation = findViewById(R.id.rl_weather_location)

        tvLabelSetDefaultLauncher = findViewById(R.id.tv_label_set_default_launcher)
        tvSubSetDefaultLauncher = findViewById(R.id.tv_sub_set_default_launcher)
        rlSetDefaultLauncher = findViewById(R.id.rl_set_default_launcher)
        ivArrowSetDefaultLauncher = findViewById(R.id.iv_arrow_set_default_launcher)

        tvLabelResetLauncher = findViewById(R.id.tv_label_reset_launcher)
        tvSubResetLauncher = findViewById(R.id.tv_sub_reset_launcher)
        rlResetLauncher = findViewById(R.id.rl_reset_launcher)
        ivResetLauncherIcon = findViewById(R.id.iv_reset_launcher_icon)

        tvSecIcons = findViewById(R.id.tv_sec_icons)
        cardIconSettings = findViewById(R.id.card_icon_settings)
        tvLabelIconSize = findViewById(R.id.tv_label_icon_size)
        llIconSizeSelector = findViewById(R.id.ll_icon_size_selector)
        tvLabelIconShape = findViewById(R.id.tv_label_icon_shape)
        llIconShapeSelector = findViewById(R.id.ll_icon_shape_selector)
        tvLabelMonochromatic = findViewById(R.id.tv_label_monochromatic)
        tvSubMonochromatic = findViewById(R.id.tv_sub_monochromatic)
        swMonochromatic = findViewById(R.id.sw_monochromatic)

        divCardCornerRadius = findViewById(R.id.div_card_corner_radius)
        llCardCornerRadiusContainer = findViewById(R.id.ll_card_corner_radius_container)
        ivCardCornerRadiusIcon = findViewById(R.id.iv_card_corner_radius_icon)
        tvLabelCardCornerRadius = findViewById(R.id.tv_label_card_corner_radius)
        tvValueCardCornerRadius = findViewById(R.id.tv_value_card_corner_radius)
        sbCardCornerRadius = findViewById(R.id.sb_card_corner_radius)

        cardPermissions = findViewById(R.id.card_permissions)

        cardThemeShowcase = findViewById(R.id.card_theme_showcase)
        ivThemeShowcaseWallpaper = findViewById(R.id.iv_theme_showcase_wallpaper)
        viewThemeShowcaseOverlay = findViewById(R.id.view_theme_showcase_overlay)
        ivThemeShowcaseSymbol = findViewById(R.id.iv_theme_showcase_symbol)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sv_settings_content)) { v, insets ->
            v.setPadding(v.paddingLeft, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, v.paddingRight, v.paddingBottom)
            insets
        }

        btnManageThemes.setOnClickHaptic {
            startActivity(Intent(this, com.alisu.alauncher.theme.ThemeActivity::class.java))
        }
        btnManageIconPacks.setOnClickHaptic {
            startActivity(Intent(this, com.alisu.alauncher.launcher.IconPackActivity::class.java))
        }
        findViewById<View>(R.id.btn_manage_permissions).setOnClickHaptic {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }

        setupControls()
        setupVisualSelectors()

        updateIconPackSummary()
        lifecycleScope.launch {
            ThemeManager.currentTheme.collectLatest { theme ->
                applyTheme(theme)
            }
        }
    }

    override fun onDestroy() {
        shakeAnimator?.cancel()
        shakeAnimator = null
        sbCardCornerRadius.setOnSeekBarChangeListener(null)
        swShowAppNames.setOnCheckedChangeListener(null)
        swShowMascot.setOnCheckedChangeListener(null)
        swMonochromatic.setOnCheckedChangeListener(null)
        ivThemeShowcaseSymbol.dispose()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        updateIconPackSummary()
    }

    private fun setupVisualSelectors() {
        val theme = ThemeManager.currentTheme.value

        // ── Clock Style Selector ──
        val currentClockStyle = prefs.getString("clock_style", "default") ?: "default"
        val currentClockIdx = clockStyleKeys.indexOf(currentClockStyle).coerceAtLeast(0)
        buildClockStyleSelector(theme, currentClockIdx)

        // ── Icon Size Selector ──
        val currentSize = prefs.getString("icon_size", "medium") ?: "medium"
        val currentSizeIdx = iconSizeKeys.indexOf(currentSize).coerceAtLeast(0)
        buildIconSizeSelector(theme, currentSizeIdx)

        // ── Icon Shape Selector ──
        val currentShape = prefs.getString("icon_shape", "theme") ?: "theme"
        val currentShapeIdx = iconShapeKeys.indexOf(currentShape).coerceAtLeast(0)
        buildIconShapeSelector(theme, currentShapeIdx)

        // ── Icon Shape Mode Selector ──
        val currentShapeMode = prefs.getString("icon_shape_mode", "force_launcher") ?: "force_launcher"
        val currentShapeModeIdx = iconShapeModeKeys.indexOf(currentShapeMode).coerceAtLeast(0)
        buildIconShapeModeSelector(theme, currentShapeModeIdx)

        // ── Drawer Style Selector ──
        val currentDrawer = prefs.getString("drawer_style", "standard") ?: "standard"
        val currentDrawerIdx = drawerStyleKeys.indexOf(currentDrawer).coerceAtLeast(0)
        buildDrawerStyleSelector(theme, currentDrawerIdx)
    }

    private fun buildClockStyleSelector(theme: AppTheme, selectedIndex: Int) {
        llClockStyleSelector.removeAllViews()

        for (i in clockStyleOptions.indices) {
            val isActive = i == selectedIndex

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(6))
                layoutParams = LinearLayout.LayoutParams(dpToPx(80), dpToPx(64)).apply {
                    setMargins(dpToPx(4), 0, dpToPx(4), 0)
                }
            }

            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                if (isActive) {
                    setColor(adjustAlpha(theme.colors.primary, 0.25f))
                    setStroke(dpToPx(2f).toInt(), theme.colors.primary)
                } else {
                    setColor(adjustAlpha(theme.colors.surface, 0.3f))
                    if (theme.shapes.showBorders) {
                        setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.textSecondary, 0.1f))
                    }
                }
            }
            card.background = cardBg
            card.addRipple(adjustAlpha(theme.colors.primary, 0.15f))

            val preview = createClockStylePreview(this, i, theme)
            card.addView(preview, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 1f
            ))

            val tvName = TextView(this).apply {
                text = clockStyleOptions[i]
                textSize = 9f
                setTextColor(if (isActive) theme.colors.primary else adjustAlpha(theme.colors.textSecondary, 0.8f))
                gravity = Gravity.CENTER
                maxLines = 1
            }
            card.addView(tvName, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))

            card.setOnClickHaptic {
                prefs.edit().putString("clock_style", clockStyleKeys[i]).apply()
                buildClockStyleSelector(theme, i)
            }

            llClockStyleSelector.addView(card)
        }
    }

    private fun createClockStylePreview(context: Context, styleIdx: Int, theme: AppTheme): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val accent = adjustAlpha(theme.colors.primary, 0.7f)
        val surface = adjustAlpha(theme.colors.surface, 0.5f)

        when (styleIdx) {
            0 -> { // Glass Card
                container.addView(makeMiniBar(context, dpToPx(32), dpToPx(8), surface))
                container.addView(makeSpacer(context, 4))
                container.addView(makeMiniBar(context, dpToPx(28), dpToPx(6), accent))
            }
            1 -> { // Faixa Horizontal
                container.addView(makeMiniBar(context, dpToPx(40), dpToPx(10), surface))
            }
            2 -> { // Terminal
                val termBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(2f)
                    setColor(adjustAlpha(Color.BLACK, 0.6f))
                    setStroke(dpToPx(1f).toInt(), accent)
                }
                container.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(18))
                    background = termBg
                })
            }
            3 -> { // Character Card
                val characterCardBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(3f)
                    setColor(surface)
                    setStroke(dpToPx(1f).toInt(), accent)
                }
                val inner = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                    background = characterCardBg
                }
                inner.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(10), dpToPx(10))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(accent)
                    }
                })
                inner.addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(dpToPx(18), LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dpToPx(4) }
                    addView(makeMiniBar(context, dpToPx(18), dpToPx(3), theme.colors.textPrimary))
                    addView(makeSpacer(context, 2))
                    addView(makeMiniBar(context, dpToPx(12), dpToPx(2), theme.colors.textSecondary))
                })
                container.addView(inner)
            }
            4 -> { // Character Split
                val inner = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                inner.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(14), dpToPx(14))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.TRANSPARENT)
                        setStroke(dpToPx(1f).toInt(), accent)
                    }
                })
                inner.addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(dpToPx(18), LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dpToPx(6) }
                    addView(makeMiniBar(context, dpToPx(18), dpToPx(4), theme.colors.textPrimary))
                    addView(makeSpacer(context, 2))
                    addView(makeMiniBar(context, dpToPx(12), dpToPx(2), theme.colors.textSecondary))
                })
                container.addView(inner)
            }
            5 -> { // Escondido
                val dash = TextView(context).apply {
                    text = "—"
                    textSize = 18f
                    setTextColor(adjustAlpha(theme.colors.textSecondary, 0.3f))
                }
                container.addView(dash)
            }
        }
        return container
    }

    private fun makeMiniBar(ctx: Context, w: Int, h: Int, color: Int, strokeColor: Int = 0): View {
        return View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(w, h)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(4f)
                setColor(color)
                if (strokeColor != 0) setStroke(dpToPx(1f).toInt(), strokeColor)
            }
        }
    }

    private fun makeSpacer(ctx: Context, hDp: Int): View {
        return View(ctx).apply { layoutParams = LinearLayout.LayoutParams(1, dpToPx(hDp)) }
    }

    private fun buildIconSizeSelector(theme: AppTheme, selectedIndex: Int) {
        val sizeViews = listOf(
            findViewById<View>(R.id.btn_size_small),
            findViewById<View>(R.id.btn_size_medium),
            findViewById<View>(R.id.btn_size_large)
        )

        for (i in sizeViews.indices) {
            val btn = sizeViews[i] ?: continue
            val isActive = i == selectedIndex

            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                if (isActive) {
                    setColor(adjustAlpha(theme.colors.primary, 0.25f))
                    setStroke(dpToPx(2f).toInt(), theme.colors.primary)
                } else {
                    setColor(adjustAlpha(theme.colors.surface, 0.3f))
                    if (theme.shapes.showBorders) {
                        setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.textSecondary, 0.1f))
                    }
                }
            }
            btn.background = cardBg
            btn.addRipple(adjustAlpha(theme.colors.primary, 0.15f))

            // Color the preview square inside
            val innerLayout = (btn as? ViewGroup)?.getChildAt(0) as? ViewGroup
            val previewView = innerLayout?.getChildAt(0)
            previewView?.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                setColor(adjustAlpha(theme.colors.primary, 0.6f))
            }

            val idx = i
            btn.setOnClickHaptic {
                prefs.edit().putString("icon_size", iconSizeKeys[idx]).apply()
                buildIconSizeSelector(theme, idx)
            }
        }
    }

    private fun buildIconShapeSelector(theme: AppTheme, selectedIndex: Int) {
        val shapeViews = listOf(
            findViewById<View>(R.id.btn_shape_theme),
            findViewById<View>(R.id.btn_shape_circle),
            findViewById<View>(R.id.btn_shape_square),
            findViewById<View>(R.id.btn_shape_squircle)
        )
        val cornerRadii = floatArrayOf(dpToPx(10f).toFloat(), 999f, 0f, dpToPx(14f).toFloat())

        for (i in shapeViews.indices) {
            val btn = shapeViews[i] ?: continue
            val isActive = i == selectedIndex

            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                if (isActive) {
                    setColor(adjustAlpha(theme.colors.primary, 0.25f))
                    setStroke(dpToPx(2f).toInt(), theme.colors.primary)
                } else {
                    setColor(adjustAlpha(theme.colors.surface, 0.3f))
                    if (theme.shapes.showBorders) {
                        setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.textSecondary, 0.1f))
                    }
                }
            }
            btn.background = cardBg
            btn.addRipple(adjustAlpha(theme.colors.primary, 0.15f))

            val innerLayout = (btn as? ViewGroup)?.getChildAt(0) as? ViewGroup
            val previewView = innerLayout?.getChildAt(0)
            previewView?.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadii[i]
                setColor(adjustAlpha(theme.colors.primary, 0.6f))
            }

            val idx = i
            btn.setOnClickHaptic {
                prefs.edit().putString("icon_shape", iconShapeKeys[idx]).apply()
                buildIconShapeSelector(theme, idx)
            }
        }
    }

    private fun buildIconShapeModeSelector(theme: AppTheme, selectedIndex: Int) {
        val shapeModeViews = listOf(
            findViewById<View>(R.id.btn_shape_mode_force),
            findViewById<View>(R.id.btn_shape_mode_follow),
            findViewById<View>(R.id.btn_shape_mode_original)
        )

        for (i in shapeModeViews.indices) {
            val btn = shapeModeViews[i] ?: continue
            val isActive = i == selectedIndex

            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                if (isActive) {
                    setColor(adjustAlpha(theme.colors.primary, 0.25f))
                    setStroke(dpToPx(2f).toInt(), theme.colors.primary)
                } else {
                    setColor(adjustAlpha(theme.colors.surface, 0.3f))
                    if (theme.shapes.showBorders) {
                        setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.textSecondary, 0.1f))
                    }
                }
            }
            btn.background = cardBg
            btn.addRipple(adjustAlpha(theme.colors.primary, 0.15f))

            val innerLayout = (btn as? ViewGroup)?.getChildAt(0) as? ViewGroup
            val iconView = innerLayout?.getChildAt(0) as? ImageView
            iconView?.setColorFilter(if (isActive) theme.colors.primary else adjustAlpha(theme.colors.textSecondary, 0.5f))

            val idx = i
            btn.setOnClickHaptic {
                prefs.edit().putString("icon_shape_mode", iconShapeModeKeys[idx]).apply()
                buildIconShapeModeSelector(theme, idx)
            }
        }
    }

    private fun buildDrawerStyleSelector(theme: AppTheme, selectedIndex: Int) {
        llDrawerStyleSelector.removeAllViews()

        for (i in drawerStyleOptions.indices) {
            val isActive = i == selectedIndex

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(6))
                layoutParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(72)).apply {
                    setMargins(dpToPx(4), 0, dpToPx(4), 0)
                }
            }

            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                if (isActive) {
                    setColor(adjustAlpha(theme.colors.primary, 0.25f))
                    setStroke(dpToPx(2f).toInt(), theme.colors.primary)
                } else {
                    setColor(adjustAlpha(theme.colors.surface, 0.3f))
                    if (theme.shapes.showBorders) {
                        setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.textSecondary, 0.1f))
                    }
                }
            }
            card.background = cardBg
            card.addRipple(adjustAlpha(theme.colors.primary, 0.15f))

            val preview = createDrawerStylePreview(this, i, theme)
            card.addView(preview, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 1f
            ))

            val tvName = TextView(this).apply {
                text = drawerStyleOptions[i]
                textSize = 9f
                setTextColor(if (isActive) theme.colors.primary else adjustAlpha(theme.colors.textSecondary, 0.8f))
                gravity = Gravity.CENTER
                maxLines = 1
            }
            card.addView(tvName, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))

            card.setOnClickHaptic {
                prefs.edit().putString("drawer_style", drawerStyleKeys[i]).apply()
                buildDrawerStyleSelector(theme, i)
            }

            llDrawerStyleSelector.addView(card)
        }
    }

    private fun createDrawerStylePreview(context: Context, styleIdx: Int, theme: AppTheme): View {
        val accent = adjustAlpha(theme.colors.primary, 0.7f)
        val surface = adjustAlpha(theme.colors.surface, 0.5f)

        return when (styleIdx) {
            0 -> { // Standard Grid (3x3 Centered)
                val grid = FrameLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(50), dpToPx(50))
                }
                val iconSize = dpToPx(8f)
                val gap = dpToPx(4f)
                val startX = dpToPx(9f)
                val startY = dpToPx(9f)
                for (row in 0..2) {
                    for (col in 0..2) {
                        val idx = row * 3 + col
                        val xVal = startX + col * (iconSize + gap)
                        val yVal = startY + row * (iconSize + gap)
                        val dot = View(context).apply {
                            layoutParams = FrameLayout.LayoutParams(iconSize.toInt(), iconSize.toInt()).apply {
                                leftMargin = xVal.toInt()
                                topMargin = yVal.toInt()
                            }
                            background = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = dpToPx(2f)
                                setColor(if (idx % 2 == 0) accent else surface)
                            }
                        }
                        grid.addView(dot)
                    }
                }
                grid
            }
            1 -> { // Orbital
                val container = FrameLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(50), dpToPx(50))
                }
                val orbitView = View(context).apply {
                    layoutParams = FrameLayout.LayoutParams(dpToPx(36), dpToPx(36), Gravity.CENTER)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.TRANSPARENT)
                        setStroke(dpToPx(1.5f).toInt(), accent)
                    }
                }
                container.addView(orbitView)
                val cx = dpToPx(25f)
                val cy = dpToPx(25f)
                val radius = dpToPx(16f)
                for (i in 0..5) {
                    val angle = (i * 60f)
                    val rad = Math.toRadians(angle.toDouble())
                    val dotX = (cx + radius * kotlin.math.cos(rad).toFloat()).toInt()
                    val dotY = (cy + radius * kotlin.math.sin(rad).toFloat()).toInt()
                    val dotSize = if (i % 2 == 0) dpToPx(7f).toInt() else dpToPx(4f).toInt()
                    val dot = View(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dotSize, dotSize)
                        x = (dotX - dotSize / 2f)
                        y = (dotY - dotSize / 2f)
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(if (i % 2 == 0) accent else surface)
                        }
                    }
                    container.addView(dot)
                }
                container
            }
            else -> View(context)
        }
    }

    private fun setupControls() {
        swShowAppNames.isChecked = prefs.getBoolean("show_app_name", true)
        swShowAppNames.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_app_name", isChecked).apply()
        }

        swShowMascot.isChecked = prefs.getBoolean("show_mascot", true)
        swShowMascot.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_mascot", isChecked).apply()
        }

        updateWeatherLocationSubtext()
        rlWeatherLocation.setOnClickHaptic { showWeatherLocationDialog() }

        swMonochromatic.isChecked = prefs.getBoolean("monochromatic_icons", true)
        swMonochromatic.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("monochromatic_icons", isChecked).apply()
        }

        val currentRadius = ThemeManager.currentTheme.value.shapes.cardCornerRadius
        sbCardCornerRadius.progress = currentRadius
        tvValueCardCornerRadius.text = getString(R.string.card_corner_radius_value, currentRadius)

        sbCardCornerRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvValueCardCornerRadius.text = getString(R.string.card_corner_radius_value, progress)
                    ThemeManager.updateCardCornerRadius(this@SettingsActivity, progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        rlSetDefaultLauncher.setOnClickHaptic { openHomeSettings() }
        rlResetLauncher.setOnClickHaptic { resetLauncherState() }
    }

    private fun applyTheme(theme: AppTheme) {
        val themeChanged = lastAppliedThemeId != theme.id
        lastAppliedThemeId = theme.id

        if (themeChanged) {
            cachedItemBg = null
            cachedSwitchThumb = null
            cachedSwitchTrack = null

            if (theme.wallpaper != null) {
                ivWallpaper.load(theme.wallpaper) {
                    bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                    allowHardware(true)
                }
            } else {
                ivWallpaper.setBackgroundColor(theme.colors.background)
            }
            viewOverlay.setBackgroundColor(adjustAlpha(theme.colors.background, 0.5f))
        }
        tvSecThemes.setTextColor(adjustAlpha(theme.colors.textSecondary, 0.8f))
        tvSecDesktop.setTextColor(adjustAlpha(theme.colors.textSecondary, 0.8f))
        tvSecIcons.setTextColor(adjustAlpha(theme.colors.textSecondary, 0.8f))

        val cardRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
        val borderSize = dpToPx(1f).toInt()
        val borderColor = adjustAlpha(theme.colors.primary, 0.3f)
        val cardBgColor = adjustAlpha(theme.colors.surface, 0.6f)
        val showBorders = theme.shapes.showBorders

        fun createCardBg() = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cardRadius
            setColor(cardBgColor)
            if (showBorders) {
                setStroke(borderSize, borderColor)
            }
        }

        cardThemesContainer.background = createCardBg()
        cardDesktopSettings.background = createCardBg()
        cardIconSettings.background = createCardBg()
        cardPermissions.background = createCardBg()

        // ── Theme Showcase Card ──
        cardThemeShowcase.background = createCardBg()
        cardThemeShowcase.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                if (view.width > 0 && view.height > 0) {
                    outline.setRoundRect(0, 0, view.width, view.height, cardRadius)
                }
            }
        }
        val showcaseImage = theme.altBackdrop ?: theme.wallpaper
        if (showcaseImage != null) {
            ivThemeShowcaseWallpaper.load(showcaseImage) {
                bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                allowHardware(true)
                crossfade(300)
            }
        } else {
            ivThemeShowcaseWallpaper.setBackgroundColor(theme.colors.background)
        }
        val overlayGradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                adjustAlpha(theme.colors.background, 0.1f),
                adjustAlpha(theme.colors.background, 0.5f),
                adjustAlpha(theme.colors.background, 0.85f)
            )
        )
        viewThemeShowcaseOverlay.background = overlayGradient
        if (theme.symbol != null) {
            ivThemeShowcaseSymbol.load(theme.symbol) {
                crossfade(300)
                listener(
                    onSuccess = { _, _ ->
                        startMysteriousShake(ivThemeShowcaseSymbol)
                    }
                )
            }
            ivThemeShowcaseSymbol.visibility = View.VISIBLE
        } else {
            ivThemeShowcaseSymbol.visibility = View.GONE
        }

        if (cachedItemBg == null) {
            cachedItemBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cardRadius
                setColor(adjustAlpha(theme.colors.surface, 0.2f))
                if (theme.shapes.showBorders) {
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.textSecondary, 0.15f))
                }
            }
        }
        val themeRippleColor = adjustAlpha(theme.colors.primary, 0.15f)
        listOf(
            btnManageThemes,
            btnManageIconPacks,
            findViewById<View>(R.id.btn_manage_permissions),
            rlWeatherLocation,
            rlSetDefaultLauncher,
            rlResetLauncher
        ).forEach { view ->
            val bgClone = cachedItemBg?.constantState?.newDrawable()?.mutate()
            if (bgClone != null) {
                view.background = bgClone
                view.addRipple(themeRippleColor)
            }
        }
        tvManageThemesTitle.setTextColor(theme.colors.textPrimary)
        tvManageThemesSub.setTextColor(theme.colors.textSecondary)
        ivManageThemesIcon.setColorFilter(theme.colors.primary)
        ivManageThemesArrow.setColorFilter(theme.colors.textSecondary)

        tvIconPacksTitle.setTextColor(theme.colors.textPrimary)
        tvIconPacksSub.setTextColor(theme.colors.textSecondary)
        ivIconPacksIcon.setColorFilter(theme.colors.primary)
        ivIconPacksArrow.setColorFilter(theme.colors.textSecondary)

        tvLabelClockStyle.setTextColor(theme.colors.textPrimary)

        tvLabelShowAppNames.setTextColor(theme.colors.textPrimary)
        tvSubShowAppNames.setTextColor(theme.colors.textSecondary)
        tvLabelShowMascot.setTextColor(theme.colors.textPrimary)
        tvSubShowMascot.setTextColor(theme.colors.textSecondary)
        tvLabelWeatherLocation.setTextColor(theme.colors.textPrimary)
        tvSubWeatherLocation.setTextColor(theme.colors.textSecondary)
        tvLabelSetDefaultLauncher.setTextColor(theme.colors.textPrimary)
        tvSubSetDefaultLauncher.setTextColor(theme.colors.textSecondary)
        ivArrowSetDefaultLauncher.setColorFilter(theme.colors.textSecondary)
        tvLabelResetLauncher.setTextColor(theme.colors.textPrimary)
        tvSubResetLauncher.setTextColor(theme.colors.textSecondary)
        ivResetLauncherIcon.setColorFilter(theme.colors.textSecondary)
        findViewById<ImageView>(R.id.iv_clock_style_icon)?.setColorFilter(theme.colors.primary)
        findViewById<ImageView>(R.id.iv_drawer_style_icon)?.setColorFilter(theme.colors.primary)
        findViewById<ImageView>(R.id.iv_icon_size_icon)?.setColorFilter(theme.colors.primary)
        findViewById<ImageView>(R.id.iv_icon_shape_icon)?.setColorFilter(theme.colors.primary)
        tvLabelIconSize.setTextColor(theme.colors.textPrimary)
        tvLabelIconShape.setTextColor(theme.colors.textPrimary)
        tvLabelMonochromatic.setTextColor(theme.colors.textPrimary)
        tvSubMonochromatic.setTextColor(theme.colors.textSecondary)

        tvLabelDrawerStyle.setTextColor(theme.colors.textPrimary)

        tvLabelCardCornerRadius.setTextColor(theme.colors.textPrimary)
        tvValueCardCornerRadius.setTextColor(theme.colors.primary)
        ivCardCornerRadiusIcon.setColorFilter(theme.colors.primary)

        val primaryColorStateList = ColorStateList.valueOf(theme.colors.primary)
        val trackColorStateList = ColorStateList.valueOf(adjustAlpha(theme.colors.primary, 0.3f))
        sbCardCornerRadius.progressTintList = primaryColorStateList
        sbCardCornerRadius.thumbTintList = primaryColorStateList
        sbCardCornerRadius.progressBackgroundTintList = trackColorStateList
        sbCardCornerRadius.progress = theme.shapes.cardCornerRadius
        tvValueCardCornerRadius.text = getString(R.string.card_corner_radius_value, theme.shapes.cardCornerRadius)

        findViewById<TextView>(R.id.tv_permissions_title).setTextColor(theme.colors.textPrimary)
        findViewById<TextView>(R.id.tv_permissions_sub).setTextColor(theme.colors.textSecondary)
        findViewById<ImageView>(R.id.iv_permissions_icon).setColorFilter(theme.colors.primary)
        findViewById<ImageView>(R.id.iv_permissions_arrow).setColorFilter(theme.colors.textSecondary)

        if (cachedSwitchThumb == null) {
            cachedSwitchThumb = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                intArrayOf(theme.colors.primary, Color.parseColor("#888888"))
            )
            cachedSwitchTrack = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                intArrayOf(adjustAlpha(theme.colors.primary, 0.6f), Color.parseColor("#444444"))
            )
        }
        val thumbTint = cachedSwitchThumb!!
        val trackTint = cachedSwitchTrack!!

        listOf(swShowAppNames, swShowMascot, swMonochromatic).forEach { sw ->
            CompoundButtonCompat.setButtonTintList(sw, thumbTint)
            sw.thumbTintList = thumbTint
            sw.trackTintList = trackTint
        }


        setupVisualSelectors()
    }

    private fun updateWeatherLocationSubtext() {
        val loc = prefs.getString("weather_location", "Sao Paulo") ?: "Sao Paulo"
        tvSubWeatherLocation.text = "${getString(R.string.current_city)} $loc"
    }

    private fun showWeatherLocationDialog() {
        val theme = ThemeManager.currentTheme.value
        val context = this
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)

        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                setColor(theme.colors.surface)
                if (theme.shapes.showBorders) {
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.3f))
                }
            }
        }

        val tvTitle = TextView(context).apply {
            text = getString(R.string.configure_location)
            textSize = 18f
            setTextColor(theme.colors.textPrimary)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(4))
        }
        dialogLayout.addView(tvTitle)

        val tvSubtitle = TextView(context).apply {
            text = getString(R.string.weather_dialog_subtitle)
            textSize = 13f
            setTextColor(adjustAlpha(theme.colors.textSecondary, 0.8f))
            setPadding(0, 0, 0, dpToPx(14))
        }
        dialogLayout.addView(tvSubtitle)

        val input = EditText(context).apply {
            val current = prefs.getString("weather_location", "Sao Paulo") ?: "Sao Paulo"
            setText(current)
            setSelection(current.length)
            setTextColor(theme.colors.textPrimary)
            setHintTextColor(adjustAlpha(theme.colors.textSecondary, 0.5f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                setColor(adjustAlpha(theme.colors.surface, 0.5f))
                setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.2f))
            }
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            setSingleLine(true)
        }
        dialogLayout.addView(input)

        var dialogInstance: androidx.appcompat.app.AlertDialog? = null

        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dpToPx(16), 0, 0)

            val btnCancel = TextView(context).apply {
                text = getString(R.string.cancel)
                setTextColor(theme.colors.textSecondary)
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                isClickable = true
                isFocusable = true
                setOnClickHaptic { dialogInstance?.dismiss() }
            }
            addView(btnCancel)

            val btnSave = TextView(context).apply {
                text = getString(R.string.save)
                setTextColor(theme.colors.primary)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                isClickable = true
                isFocusable = true
                setOnClickHaptic {
                    val newLoc = input.text.toString().trim()
                    if (newLoc.isNotEmpty()) {
                        prefs.edit().putString("weather_location", newLoc).apply()
                        updateWeatherLocationSubtext()
                        dialogInstance?.dismiss()
                    } else {
                        Toast.makeText(context, getString(R.string.invalid_city), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            addView(btnSave)
        }
        dialogLayout.addView(buttonsLayout)

        builder.setView(dialogLayout)
        val dialog = builder.create()
        dialogInstance = dialog
        dialog.apply {
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.statusBarColor = Color.TRANSPARENT
                it.navigationBarColor = Color.TRANSPARENT
            }
        }.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun resetLauncherState() {
        try {
            val pm = packageManager
            val componentName = android.content.ComponentName(this, MainActivity::class.java)
            pm.setComponentEnabledSetting(componentName, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(componentName, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP)
            Toast.makeText(this, getString(R.string.system_state_reset), Toast.LENGTH_LONG).show()
            finishAffinity()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.reset_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openHomeSettings() {
        try { startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS)) }
        catch (e: Exception) {
            try { startActivity(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
            catch (e2: Exception) {
                startActivity(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME); flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
        }
    }

    private fun startMysteriousShake(view: View) {
        val random = java.util.Random()
        lifecycleScope.launch {
            delay(1500L)
            while (isActive) {
                val duration = 400L + random.nextInt(600)
                val intensity = 1.5f + random.nextFloat()
                
                val anim = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                    this.duration = duration
                    interpolator = android.view.animation.DecelerateInterpolator()
                    addUpdateListener {
                        val p = animatedValue as Float
                        val decay = 1f - p
                        val wave = Math.sin(p * Math.PI * 16).toFloat()
                        view.translationX = wave * intensity * decay * 2f
                        view.translationY = Math.cos(p * Math.PI * 13).toFloat() * intensity * decay * 1.2f
                        view.rotation = Math.sin(p * Math.PI * 11).toFloat() * intensity * decay * 1.5f
                    }
                }
                shakeAnimator = anim
                
                val deferred = CompletableDeferred<Unit>()
                anim.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        view.translationX = 0f
                        view.translationY = 0f
                        view.rotation = 0f
                        deferred.complete(Unit)
                    }
                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        deferred.complete(Unit)
                    }
                })
                anim.start()
                deferred.await()
                
                val delay = 2000L + random.nextInt(4000)
                delay(delay)
            }
        }
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
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

    private fun updateIconPackSummary() {
        val selectedPackPkg = prefs.getString("icon_pack_package", "none") ?: "none"
        if (selectedPackPkg == "none") {
                tvIconPacksSub.text = getString(R.string.none)
        } else {
            try {
                val pm = packageManager
                val info = pm.getApplicationInfo(selectedPackPkg, 0)
                tvIconPacksSub.text = pm.getApplicationLabel(info).toString()
            } catch (e: Exception) {
            tvIconPacksSub.text = getString(R.string.none)
            }
        }
    }

}
