package com.alisu.alauncher.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alisu.alauncher.R
import com.alisu.alauncher.theme.ThemeManager
import com.alisu.alauncher.theme.AppTheme
import coil.load
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IconPackActivity : AppCompatActivity() {

    private lateinit var rvIconPacks: RecyclerView
    private lateinit var ivWallpaper: ImageView
    private lateinit var adapter: IconPackAdapter

    override fun onDestroy() {
        super.onDestroy()
        System.gc()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContentView(R.layout.activity_icon_packs)

        rvIconPacks = findViewById(R.id.rv_icon_packs)
        ivWallpaper = findViewById(R.id.iv_icon_packs_wallpaper)

        ViewCompat.setOnApplyWindowInsetsListener(rvIconPacks) { v, insets ->
            v.setPadding(v.paddingLeft, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, v.paddingRight, v.paddingBottom)
            insets
        }

        rvIconPacks.layoutManager = LinearLayoutManager(this)
        
        val prefs = getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
        val currentPack = prefs.getString("icon_pack_package", "none") ?: "none"

        adapter = IconPackAdapter(
            currentPackPackage = currentPack,
            onPackSelected = { packPkg ->
                prefs.edit().putString("icon_pack_package", packPkg).apply()
                IconPackManager.loadIconPack(this, packPkg)
                adapter.setCurrentPack(packPkg)
                Toast.makeText(this, getString(R.string.icon_pack_selected), Toast.LENGTH_SHORT).show()
            }
        )
        rvIconPacks.adapter = adapter

        lifecycleScope.launch {
            ThemeManager.currentTheme.collectLatest { theme ->
                applyTheme(theme)
            }
        }

        loadIconPacks()
    }

    private fun loadIconPacks() {
        val packs = ArrayList<IconPackItem>()
        packs.add(IconPackItem(getString(R.string.default_pack), "none"))

        val installed = IconPackManager.getInstalledIconPacks(this)
        for (p in installed) {
            packs.add(IconPackItem(p.label, p.packageName))
        }
        adapter.submitList(packs)
    }

    private fun applyTheme(theme: AppTheme) {
        val rootLayout = findViewById<View>(R.id.icon_packs_root)
        val overlay = findViewById<View>(R.id.view_icon_packs_overlay)

        rootLayout.setBackgroundColor(theme.colors.background)
        overlay.setBackgroundColor(adjustAlpha(theme.colors.background, 0.4f))

        if (theme.wallpaper != null) {
            ivWallpaper.load(theme.wallpaper) {
                bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                allowHardware(true)
                size(coil.size.ViewSizeResolver(ivWallpaper))
            }
        } else {
            ivWallpaper.setImageDrawable(null)
            ivWallpaper.setBackgroundColor(theme.colors.background)
        }

        adapter.setTheme(theme)
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    private fun View.setOnClickHaptic(action: () -> Unit) {
        this.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.92f).scaleY(0.92f)
                        .setDuration(120)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                }
                android.view.MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f)
                        .setDuration(180)
                        .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                        .start()
                    action()
                }
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f)
                        .setDuration(180)
                        .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                        .start()
                }
            }
            true
        }
    }
}

data class IconPackItem(
    val label: String,
    val packageName: String
)

class IconPackAdapter(
    private var currentPackPackage: String,
    private val onPackSelected: (String) -> Unit
) : RecyclerView.Adapter<IconPackAdapter.ViewHolder>() {

    private val items = ArrayList<IconPackItem>()
    private var theme: AppTheme? = null

    fun submitList(newItems: List<IconPackItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setCurrentPack(pkg: String) {
        currentPackPackage = pkg
        notifyDataSetChanged()
    }

    fun setTheme(newTheme: AppTheme) {
        theme = newTheme
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_icon_pack, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, item.packageName == currentPackPackage, theme, onPackSelected)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.item_icon_pack_container)
        private val checkDot: View = itemView.findViewById(R.id.iv_icon_pack_check_dot)
        private val appIcon: ImageView = itemView.findViewById(R.id.iv_icon_pack_app_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_icon_pack_name)
        private val tvPkg: TextView = itemView.findViewById(R.id.tv_icon_pack_package)
        private val previews = listOf<ImageView>(
            itemView.findViewById(R.id.iv_preview_1),
            itemView.findViewById(R.id.iv_preview_2),
            itemView.findViewById(R.id.iv_preview_3),
            itemView.findViewById(R.id.iv_preview_4)
        )

        fun bind(
            item: IconPackItem,
            isSelected: Boolean,
            theme: AppTheme?,
            onSelected: (String) -> Unit
        ) {
            val context = itemView.context
            val pm = context.packageManager

            tvName.text = item.label
            if (item.packageName == "none") {
                tvPkg.text = context.getString(R.string.default_look)
                appIcon.setImageResource(R.drawable.ic_palette)
                if (theme != null) {
                    appIcon.setColorFilter(theme.colors.primary)
                } else {
                    appIcon.setColorFilter(Color.WHITE)
                }
            } else {
                tvPkg.text = item.packageName
                appIcon.clearColorFilter()
                try {
                    appIcon.setImageDrawable(pm.getApplicationIcon(item.packageName))
                } catch (e: Exception) {
                    appIcon.setImageResource(R.drawable.ic_palette)
                }
            }

            if (theme != null) {
                checkDot.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (isSelected) {
                        setColor(theme.colors.primary)
                    } else {
                        setColor(Color.TRANSPARENT)
                        setStroke(dpToPx(context, 2f).toInt(), adjustAlpha(theme.colors.textSecondary, 0.5f))
                    }
                }
                tvName.setTextColor(if (isSelected) theme.colors.primary else theme.colors.textPrimary)
                tvName.typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                tvPkg.setTextColor(adjustAlpha(theme.colors.textSecondary, 0.7f))

                val cardBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = theme.shapes.cardCornerRadius.toFloat() * itemView.resources.displayMetrics.density
                    if (isSelected) {
                        setColor(adjustAlpha(theme.colors.primary, 0.15f))
                        if (theme.shapes.showBorders) {
                            setStroke(dpToPx(context, 1f).toInt(), theme.colors.primary)
                        }
                    } else {
                        setColor(adjustAlpha(theme.colors.surface, 0.2f))
                        if (theme.shapes.showBorders) {
                            setStroke(dpToPx(context, 1f).toInt(), adjustAlpha(theme.colors.textSecondary, 0.1f))
                        }
                    }
                }
                container.background = cardBg
                
                val rippleColorStateList = android.content.res.ColorStateList.valueOf(adjustAlpha(theme.colors.primary, 0.15f))
                container.background = android.graphics.drawable.RippleDrawable(rippleColorStateList, cardBg, cardBg)
            }

            val previewDrawables = if (item.packageName == "none") {
                val systemPkgs = listOf("com.android.chrome", "com.android.phone", "com.android.camera", "com.android.settings")
                val list = ArrayList<Drawable>()
                for (spkg in systemPkgs) {
                    try {
                        list.add(pm.getApplicationIcon(spkg))
                    } catch (_: Exception) {}
                }
                list
            } else {
                IconPackManager.getPreviewIcons(context, item.packageName)
            }

            for (idx in 0 until previews.size) {
                if (idx < previewDrawables.size) {
                    previews[idx].visibility = View.VISIBLE
                    previews[idx].setImageDrawable(previewDrawables[idx])
                } else {
                    previews[idx].visibility = View.GONE
                }
            }

            container.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        v.animate().scaleX(0.95f).scaleY(0.95f)
                            .setDuration(120)
                            .setInterpolator(android.view.animation.DecelerateInterpolator())
                            .start()
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        v.isPressed = false
                        v.animate().scaleX(1.0f).scaleY(1.0f)
                            .setDuration(180)
                            .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                            .start()
                        onSelected(item.packageName)
                    }
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        v.animate().scaleX(1.0f).scaleY(1.0f)
                            .setDuration(180)
                            .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                            .start()
                    }
                }
                true
            }
        }

        private fun adjustAlpha(color: Int, factor: Float): Int {
            val alpha = Math.round(Color.alpha(color) * factor)
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)
            return Color.argb(alpha, red, green, blue)
        }

        private fun dpToPx(context: Context, dp: Float): Float = dp * context.resources.displayMetrics.density
    }
}
