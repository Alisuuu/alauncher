package com.alisu.alauncher

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.alisu.alauncher.theme.AppTheme
import com.alisu.alauncher.theme.ThemeManager
import coil.load
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PermissionsActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE) }

    private lateinit var rootLayout: View
    private lateinit var ivWallpaper: ImageView
    private lateinit var viewOverlay: View
    private lateinit var scrollContent: LinearLayout

    private var cachedCardBg: GradientDrawable? = null
    private var lastAppliedThemeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_permissions)

        rootLayout = findViewById(R.id.permissions_root)
        ivWallpaper = findViewById(R.id.iv_permissions_wallpaper)
        viewOverlay = findViewById(R.id.view_permissions_overlay)
        scrollContent = findViewById(R.id.ll_permissions_content)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sv_permissions_content)) { v, insets ->
            v.setPadding(v.paddingLeft, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, v.paddingRight, v.paddingBottom)
            insets
        }

        lifecycleScope.launch {
            ThemeManager.currentTheme.collectLatest { theme ->
                applyTheme(theme)
                rebuildPermissionsList()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rebuildPermissionsList()
    }

    private fun applyTheme(theme: AppTheme) {
        val changed = lastAppliedThemeId != theme.id
        lastAppliedThemeId = theme.id

        if (changed) {
            cachedCardBg = null
        }

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

    private fun rebuildPermissionsList() {
        scrollContent.removeAllViews()
        val theme = ThemeManager.currentTheme.value
        val radius = dpToPx(theme.shapes.cardCornerRadius.toFloat())

        if (cachedCardBg == null) {
            cachedCardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(adjustAlpha(theme.colors.surface, 0.6f))
                if (theme.shapes.showBorders) {
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.3f))
                }
            }
        }

        val itemBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(adjustAlpha(theme.colors.surface, 0.2f))
            if (theme.shapes.showBorders) {
                setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.textSecondary, 0.15f))
            }
        }

        val permissions = listOf(
            PermissionInfo(
                title = getString(R.string.perm_default_launcher),
                description = getString(R.string.perm_default_launcher_desc),
                granted = isDefaultLauncher(),
                icon = R.drawable.ic_home,
                action = { openDefaultLauncherSettings() }
            ),
            PermissionInfo(
                title = getString(R.string.perm_notifications),
                description = getString(R.string.perm_notifications_desc),
                granted = isNotificationListenerEnabled(),
                icon = R.drawable.ic_music_note,
                action = { openNotificationListenerSettings() }
            )
        )

        val cardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cachedCardBg
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    outline.setRoundRect(0, 0, v.width, v.height, radius)
                }
            }
        }

        val grantedCount = permissions.count { it.granted }
        val total = permissions.size

        val summaryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(12))
        }

        val tvSummary = TextView(this).apply {
            text = getString(R.string.permissions_granted, grantedCount, total)
            textSize = 13f
            setTextColor(theme.colors.textSecondary)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        summaryLayout.addView(tvSummary)

        val tvStatus = TextView(this).apply {
            text = if (grantedCount == total) getString(R.string.all_good) else getString(R.string.attention)
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(if (grantedCount == total) Color.parseColor("#4CAF50") else Color.parseColor("#FF9800"))
        }
        summaryLayout.addView(tvStatus)
        cardContainer.addView(summaryLayout)

        for (perm in permissions) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(8) }
                background = itemBg
                if (perm.action != null) {
                    isClickable = true
                    isFocusable = true
                    addRipple(adjustAlpha(theme.colors.primary, 0.15f))
                    setOnClickHaptic { perm.action.invoke() }
                }
            }

            val ivIcon = ImageView(this).apply {
                setImageResource(perm.icon)
                setColorFilter(if (perm.granted) theme.colors.primary else theme.colors.textSecondary)
                layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20)).apply {
                    marginEnd = dpToPx(12)
                }
            }
            row.addView(ivIcon)

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvPermTitle = TextView(this).apply {
                text = perm.title
                textSize = 14f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(theme.colors.textPrimary)
            }
            textCol.addView(tvPermTitle)

            val tvPermDesc = TextView(this).apply {
                text = perm.description
                textSize = 11f
                setTextColor(adjustAlpha(theme.colors.textSecondary, 0.8f))
            }
            textCol.addView(tvPermDesc)
            row.addView(textCol)

            val tvPermStatus = TextView(this).apply {
                text = if (perm.granted) getString(R.string.granted) else getString(R.string.denied)
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(if (perm.granted) Color.parseColor("#4CAF50") else Color.parseColor("#FF5252"))
            }
            row.addView(tvPermStatus)

            cardContainer.addView(row)
        }

        scrollContent.addView(cardContainer)
    }

    // ── Permission checks ──────────────────────────────────────────────

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return res != null && packageName == res.activityInfo.packageName
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, MediaNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(cn.flattenToString()) == true
    }

    // ── Permission actions ─────────────────────────────────────────────

    private fun openDefaultLauncherSettings() {
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            } catch (e2: Exception) {
                Toast.makeText(this, getString(R.string.perm_open_settings_manually), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openNotificationListenerSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.perm_open_notifications), Toast.LENGTH_SHORT).show()
        }
    }

    // ── Utilities ──────────────────────────────────────────────────────

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private data class PermissionInfo(
        val title: String,
        val description: String,
        val granted: Boolean,
        val icon: Int,
        val action: (() -> Unit)?
    )
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
