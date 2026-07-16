package com.alisu.alauncher.ui

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import com.alisu.alauncher.R
import com.alisu.alauncher.model.AppInfo
import com.alisu.alauncher.theme.AppTheme
import com.alisu.alauncher.theme.ThemeDrawableLoader
import com.alisu.alauncher.gesture.GestureController
import com.alisu.alauncher.util.BackgroundViewPool
import androidx.constraintlayout.widget.ConstraintLayout
import coil.load

class AppAdapter(
    private var apps: List<AppInfo>,
    private var theme: AppTheme,
    private val onDeleteClick: ((AppInfo) -> Unit)? = null,
    private val onAppLongClick: ((AppInfo, RecyclerView.ViewHolder) -> Boolean)? = null,
    private val onAppClick: (AppInfo) -> Unit,
    private val gestureController: GestureController? = null
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    constructor(apps: List<AppInfo>, theme: AppTheme, onAppClick: (AppInfo) -> Unit) : this(
        apps, theme, null, null, onAppClick, null
    )

    init {
        setHasStableIds(false)
    }

    fun initViewPool(context: Context) {
        if (viewPool == null) {
            viewPool = BackgroundViewPool(context.applicationContext, R.layout.item_app, 12)
            viewPool?.init()
        }
    }

    private var isEditMode: Boolean = false
    private var dragOverPosition: Int = -1
    var isDragging: Boolean = false
        private set
    var isStaggeredFadeEnabled: Boolean = false
    var isDesktopGrid: Boolean = false

    private var viewPool: BackgroundViewPool? = null

    private val monoTransformationCache = mutableMapOf<Int, MonochromaticTransformation>()
    private val resIdCache = mutableMapOf<String, Int>()

    var cachedIconSizePref: String? = null
    var cachedUseMonochromatic: Boolean? = null
    var cachedIconShapePref: String? = null
    var cachedIconShapeModePref: String? = null
    var cellHeight: Int = 0

    fun ensurePrefsCached(context: Context) {
        if (cachedIconSizePref == null || cachedUseMonochromatic == null || cachedIconShapePref == null || cachedIconShapeModePref == null) {
            val prefs = context.getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
            cachedIconSizePref = prefs.getString("icon_size", "medium") ?: "medium"
            cachedUseMonochromatic = prefs.getBoolean("monochromatic_icons", true)
            cachedIconShapePref = prefs.getString("icon_shape", "theme") ?: "theme"
            cachedIconShapeModePref = prefs.getString("icon_shape_mode", "force_launcher") ?: "force_launcher"
        }
    }

    fun clearPrefsCache() {
        cachedIconSizePref = null
        cachedUseMonochromatic = null
        cachedIconShapePref = null
    }

    fun setDragging(dragging: Boolean) {
        if (isDragging != dragging) {
            isDragging = dragging
            if (!isEditMode) {
                notifyDataSetChanged()
            }
        }
    }

    fun setDragOverPosition(position: Int) {
        if (dragOverPosition != position) {
            val oldPos = dragOverPosition
            dragOverPosition = position
            if (oldPos != -1) notifyItemChanged(oldPos)
            if (dragOverPosition != -1) notifyItemChanged(dragOverPosition)
        }
    }

    fun updateData(newApps: List<AppInfo>) {
        clearPrefsCache()
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = apps.size
            override fun getNewListSize(): Int = newApps.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return apps[oldItemPosition].packageName == newApps[newItemPosition].packageName
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return apps[oldItemPosition] == newApps[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        apps = newApps
        diffResult.dispatchUpdatesTo(this)
    }

    fun setTheme(newTheme: AppTheme) {
        theme = newTheme
        monoTransformationCache.clear()
        resIdCache.clear()
        clearPrefsCache()
        notifyDataSetChanged()
    }

    fun updateTheme(newTheme: AppTheme) = setTheme(newTheme)

    fun refresh() {
        clearPrefsCache()
        notifyDataSetChanged()
    }

    fun isEmptyPosition(position: Int): Boolean {
        if (position < 0 || position >= apps.size) return true
        return apps[position].packageName.isEmpty()
    }

    fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        notifyDataSetChanged()
    }

    fun getEditMode(): Boolean = isEditMode

    fun getCachedMonoTransformation(primaryColor: Int, surfaceColor: Int): MonochromaticTransformation {
        if (monoTransformationCache.size > 100) monoTransformationCache.clear()
        return monoTransformationCache.getOrPut(primaryColor) {
            MonochromaticTransformation(primaryColor, surfaceColor)
        }
    }

    fun getCachedResId(name: String, packageName: String, context: Context): Int {
        if (resIdCache.size > 200) resIdCache.clear()
        return resIdCache.getOrPut(name) {
            context.resources.getIdentifier(name, "drawable", packageName)
        }
    }

    fun setHardwareLayersForVisible(rv: RecyclerView, enable: Boolean) {
        val layerType = if (enable) android.view.View.LAYER_TYPE_HARDWARE else android.view.View.LAYER_TYPE_NONE
        for (i in 0 until rv.childCount) {
            rv.getChildAt(i)?.setLayerType(layerType, null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = viewPool?.obtain(parent) ?: LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app, theme, isEditMode, position == dragOverPosition, onDeleteClick, onAppLongClick, onAppClick, gestureController, this, position)

        if (isStaggeredFadeEnabled && !isEditMode) {
            holder.itemView.alpha = 0f
            holder.itemView.translationY = 20f
            val delay = (position % 12) * 30L
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setStartDelay(delay)
                .start()
        }
    }

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= apps.size) return RecyclerView.NO_ID
        val app = apps[position]
        val baseId = if (app.packageName.isEmpty()) 0L else app.packageName.hashCode().toLong()
        return (baseId shl 32) or (position.toLong() and 0xFFFFFFFFL)
    }

    override fun getItemCount(): Int = apps.size

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: ConstraintLayout = itemView.findViewById(R.id.ll_item_container)
        private val iconContainer: View = itemView.findViewById(R.id.fl_icon_container)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.iv_app_icon)
        private val ivCardTexture: ImageView = itemView.findViewById(R.id.iv_card_texture)
        private val tvAppName: TextView = itemView.findViewById(R.id.tv_app_name)
        private val btnDeleteApp: ImageView = itemView.findViewById(R.id.btn_delete_app)
        private var folderPreviewContainer: FrameLayout? = null

        private var cachedPrefs: android.content.SharedPreferences? = null
        private val reusableDrawable = GradientDrawable()
        private var cachedOutlineProvider: ViewOutlineProvider? = null
        private var lastOutlineRadius = 0f

        fun bind(
            app: AppInfo,
            theme: AppTheme,
            isEditMode: Boolean,
            isDragOver: Boolean,
            onDeleteClick: ((AppInfo) -> Unit)?,
            onAppLongClick: ((AppInfo, RecyclerView.ViewHolder) -> Boolean)?,
            onAppClick: (AppInfo) -> Unit,
            gestureController: GestureController?,
            adapter: AppAdapter,
            position: Int
        ) {
            val context = itemView.context
            container.background = null
            if (cachedPrefs == null) {
                cachedPrefs = context.getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
            }
            val prefs = cachedPrefs!!
            val isPlaceholder = app.packageName.startsWith("placeholder_")

            var targetFolderId = ""
            var folderSize = 1
            var cellIndex = 0

            if (app.packageName.startsWith("folder_") && app.packageName != "folder_action_add") {
                targetFolderId = app.packageName
                folderSize = prefs.getInt("folder_size_$targetFolderId", 1)
                cellIndex = 0
            } else if (isPlaceholder) {
                val lastUnderscore = app.packageName.lastIndexOf('_')
                val extractedFolderId = if (lastUnderscore != -1) app.packageName.substring(12, lastUnderscore) else ""
                val placeholderIdx = if (lastUnderscore != -1) app.packageName.substring(lastUnderscore + 1).toIntOrNull() ?: 1 else 1
                val extractedFolderSize = prefs.getInt("folder_size_$extractedFolderId", 1)

                if (extractedFolderId.isNotEmpty() && (extractedFolderSize == 2 || extractedFolderSize == 4)) {
                    targetFolderId = extractedFolderId
                    folderSize = extractedFolderSize
                    cellIndex = placeholderIdx
                } else {
                    itemView.visibility = View.GONE
                    return
                }
            }

            val isFolder = app.packageName.startsWith("folder_")
            val isFolderActionAdd = app.packageName == "folder_action_add"
            adapter.ensurePrefsCached(context)
            val useMonochromatic = adapter.cachedUseMonochromatic ?: true

            val cellHeight = adapter.cellHeight

            if (itemView.layoutParams != null && cellHeight > 0 && itemView.layoutParams.height != cellHeight) {
                itemView.layoutParams = itemView.layoutParams.apply { height = cellHeight }
            }

            val density = context.resources.displayMetrics.density
            val iconSizePref = adapter.cachedIconSizePref ?: "medium"
            var containerSizeDp = when (iconSizePref) {
                "small" -> 54f
                "large" -> 74f
                else -> 64f
            }

            val iconSizeDp = (containerSizeDp - 12f).coerceAtLeast(24f)
            val iconSizePx = (iconSizeDp * density).toInt()
            val containerSizePx = (containerSizeDp * density).toInt()

            if (iconContainer.layoutParams.width != containerSizePx || iconContainer.layoutParams.height != containerSizePx) {
                iconContainer.layoutParams = iconContainer.layoutParams.apply {
                    width = containerSizePx
                    height = containerSizePx
                }
            }
            if (ivCardTexture.layoutParams.width != containerSizePx || ivCardTexture.layoutParams.height != containerSizePx) {
                ivCardTexture.layoutParams = ivCardTexture.layoutParams.apply {
                    width = containerSizePx
                    height = containerSizePx
                }
            }
            if (ivAppIcon.layoutParams.width != containerSizePx || ivAppIcon.layoutParams.height != containerSizePx) {
                ivAppIcon.layoutParams = ivAppIcon.layoutParams.apply {
                    width = containerSizePx
                    height = containerSizePx
                }
            }

            if (folderSize == 2 || folderSize == 4) {
                val pxZero = 0
                val pxOuter = (4f * density).toInt()
                val pxV = (8f * density).toInt()
                when {
                    folderSize == 2 && cellIndex == 0 -> itemView.setPadding(pxOuter, pxV, pxZero, pxV)
                    folderSize == 2 && cellIndex == 1 -> itemView.setPadding(pxZero, pxV, pxOuter, pxV)
                    folderSize == 4 && cellIndex == 0 -> itemView.setPadding(pxOuter, pxV, pxZero, 0)
                    folderSize == 4 && cellIndex == 1 -> itemView.setPadding(pxZero, pxV, pxOuter, 0)
                    folderSize == 4 && cellIndex == 2 -> itemView.setPadding(pxOuter, 0, pxZero, pxV)
                    folderSize == 4 && cellIndex == 3 -> itemView.setPadding(pxZero, 0, pxOuter, pxV)
                    else -> itemView.setPadding(pxOuter, pxV, pxOuter, pxV)
                }
            } else {
                val pxH = (4f * density).toInt()
                val pxV = (8f * density).toInt()
                itemView.setPadding(pxH, pxV, pxH, pxV)
            }

            val isEmpty = app.packageName.isEmpty()
            if (isEmpty) {
                val showEmpty = isEditMode || adapter.isDragging
                itemView.visibility = if (showEmpty) View.VISIBLE else View.INVISIBLE
                ivAppIcon.visibility = View.GONE
                tvAppName.visibility = View.GONE
                val radiusPx = theme.shapes.cardCornerRadius.toFloat() * density
                if (isDragOver) {
                    reusableDrawable.apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radiusPx
                        setColor(adjustAlpha(theme.colors.primary, 0.25f))
                        setStroke((2f * density).toInt(), theme.colors.primary)
                    }
                } else if (showEmpty) {
                    reusableDrawable.apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radiusPx
                        setColor(adjustAlpha(theme.colors.surface, 0.3f))
                        if (theme.shapes.showBorders) setStroke((1f * density).toInt(), adjustAlpha(theme.colors.primary, 0.1f))
                    }
                }
                iconContainer.background = reusableDrawable
                btnDeleteApp.visibility = View.GONE
                return
            }

            itemView.visibility = View.VISIBLE
            val radiusPx = when (adapter.cachedIconShapePref ?: "theme") {
                "circle" -> containerSizePx / 2f
                "square" -> 0f
                "squircle" -> 18f * density
                else -> theme.shapes.cardCornerRadius.toFloat() * density
            }

            if (isDragOver) {
                reusableDrawable.apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radiusPx
                    setColor(adjustAlpha(theme.colors.primary, 0.15f))
                    setStroke((2f * density).toInt(), theme.colors.primary)
                }
                iconContainer.background = reusableDrawable
                ivAppIcon.setImageDrawable(null)
                ivCardTexture.visibility = View.GONE
                tvAppName.visibility = View.GONE
                btnDeleteApp.visibility = View.GONE
                return
            }

            val isBottomRow = adapter.isDesktopGrid && position >= 16 && position < 20
            if (theme.layout.showAppName && !isPlaceholder && !isFolderActionAdd && !isBottomRow) {
                tvAppName.visibility = View.VISIBLE
                tvAppName.text = app.label
                tvAppName.setTextColor(theme.colors.textPrimary)
            } else tvAppName.visibility = View.GONE

            if (isFolderActionAdd) {
                if (folderPreviewContainer != null) {
                    container.removeView(folderPreviewContainer)
                    folderPreviewContainer = null
                }
                ivAppIcon.visibility = View.VISIBLE
                ivAppIcon.setImageResource(R.drawable.ic_add)
                ivAppIcon.setColorFilter(theme.colors.primary, android.graphics.PorterDuff.Mode.SRC_IN)
            } else if (isFolder || isPlaceholder) {
                val savedApps = prefs.getString("folder_apps_$targetFolderId", "") ?: ""
                val folderPackages = savedApps.split(",").filter { it.isNotEmpty() }
                val isEnlarged = folderSize == 2 || folderSize == 4

                if (!isEnlarged || (folderSize == 2 && cellIndex == 1) || (folderSize == 4 && cellIndex == 3)) {
                    ivAppIcon.visibility = View.GONE
                    var previewContainer = folderPreviewContainer
                    if (previewContainer == null || previewContainer.parent == null) {
                        if (folderPreviewContainer != null) container.removeView(folderPreviewContainer)
                        previewContainer = FrameLayout(context).apply {
                            tag = "folder_preview"
                            layoutParams = ConstraintLayout.LayoutParams(containerSizePx, containerSizePx).apply {
                                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                            }
                        }
                        folderPreviewContainer = previewContainer
                        container.addView(previewContainer)
                    } else {
                        if (previewContainer.layoutParams.width != containerSizePx || previewContainer.layoutParams.height != containerSizePx) {
                            previewContainer.layoutParams = previewContainer.layoutParams.apply {
                                width = containerSizePx
                                height = containerSizePx
                            }
                        }
                    }
                    val dropCount = if (!isEnlarged) 0 else (if (folderSize == 2) 1 else 3)
                    val displayPackages = folderPackages.drop(dropCount)
                    setupFolderPreview(previewContainer, displayPackages, useMonochromatic, theme, adapter)
                } else {
                    if (folderPreviewContainer != null) {
                        container.removeView(folderPreviewContainer)
                        folderPreviewContainer = null
                    }
                    ivAppIcon.visibility = View.VISIBLE
                    ivAppIcon.clearColorFilter()
                    if (cellIndex < folderPackages.size) {
                        val pkg = folderPackages[cellIndex]
                        val shape = adapter.cachedIconShapePref ?: "theme"
                        val shapeMode = adapter.cachedIconShapeModePref ?: "force_launcher"
                        val shapeClip = if (shapeMode == "force_launcher") ShapeClippingTransformation(shape, radiusPx) else null
                        ivAppIcon.load("app-icon://$pkg") {
                            size(containerSizePx, containerSizePx)
                            if (useMonochromatic) {
                                val transforms = mutableListOf<coil.transform.Transformation>(adapter.getCachedMonoTransformation(theme.colors.primary, theme.colors.surface))
                                if (shapeClip != null) transforms.add(shapeClip)
                                transformations(transforms)
                            } else if (shapeClip != null) {
                                transformations(shapeClip)
                            }
                        }
                    } else {
                        ivAppIcon.setImageDrawable(null)
                        ivAppIcon.setImageResource(R.drawable.ic_add)
                        ivAppIcon.setColorFilter(adjustAlpha(theme.colors.primary, 0.35f), android.graphics.PorterDuff.Mode.SRC_IN)
                    }
                }
            } else {
                if (folderPreviewContainer != null) {
                    container.removeView(folderPreviewContainer)
                    folderPreviewContainer = null
                }
                ivAppIcon.visibility = View.VISIBLE
                ivAppIcon.clearColorFilter()
                val shape = adapter.cachedIconShapePref ?: "theme"
                val shapeMode = adapter.cachedIconShapeModePref ?: "force_launcher"
                val shapeClip = if (shapeMode == "force_launcher") ShapeClippingTransformation(shape, radiusPx) else null
                ivAppIcon.load("app-icon://${app.packageName}") {
                    size(containerSizePx, containerSizePx)
                    if (useMonochromatic) {
                        val transforms = mutableListOf<coil.transform.Transformation>(adapter.getCachedMonoTransformation(theme.colors.primary, theme.colors.surface))
                        if (shapeClip != null) transforms.add(shapeClip)
                        transformations(transforms)
                    } else if (shapeClip != null) {
                        transformations(shapeClip)
                    }
                }
            }

            val rLeftTop = if (folderSize == 4 && cellIndex == 0) radiusPx else if (folderSize == 4) 0f else if (folderSize == 2 && cellIndex == 0) radiusPx else if (folderSize == 2) 0f else radiusPx
            val rRightTop = if (folderSize == 4 && cellIndex == 1) radiusPx else if (folderSize == 4) 0f else if (folderSize == 2 && cellIndex == 1) radiusPx else if (folderSize == 2) 0f else radiusPx
            val rRightBottom = if (folderSize == 4 && cellIndex == 3) radiusPx else if (folderSize == 4) 0f else if (folderSize == 2 && cellIndex == 1) radiusPx else if (folderSize == 2) 0f else radiusPx
            val rLeftBottom = if (folderSize == 4 && cellIndex == 2) radiusPx else if (folderSize == 4) 0f else if (folderSize == 2 && cellIndex == 0) radiusPx else if (folderSize == 2) 0f else radiusPx

            val appCardBg = GradientDrawable().apply {
                val shape = adapter.cachedIconShapePref ?: "theme"
                if (shape == "circle") {
                    this.shape = GradientDrawable.OVAL
                } else {
                    this.shape = GradientDrawable.RECTANGLE
                    cornerRadii = floatArrayOf(rLeftTop, rLeftTop, rRightTop, rRightTop, rRightBottom, rRightBottom, rLeftBottom, rLeftBottom)
                }
                setColor(android.graphics.Color.TRANSPARENT)
            }
            iconContainer.background = null
            val rippleColorStateList = ColorStateList.valueOf(adjustAlpha(theme.colors.primary, 0.25f))
            iconContainer.background = RippleDrawable(rippleColorStateList, appCardBg, appCardBg)
            iconContainer.isDuplicateParentStateEnabled = false

            if (lastOutlineRadius != radiusPx) {
                lastOutlineRadius = radiusPx
                cachedOutlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        val w = if (view.width > 0) view.width else containerSizePx
                        val h = if (view.height > 0) view.height else containerSizePx
                        val shape = adapter.cachedIconShapePref ?: "theme"
                        if (shape == "circle") {
                            outline.setOval(0, 0, w, h)
                        } else {
                            outline.setRoundRect(0, 0, w, h, radiusPx)
                        }
                    }
                }
            }
            iconContainer.outlineProvider = cachedOutlineProvider
            iconContainer.clipToOutline = true
            iconContainer.invalidateOutline()
            folderPreviewContainer?.outlineProvider = cachedOutlineProvider
            folderPreviewContainer?.clipToOutline = true
            folderPreviewContainer?.invalidateOutline()

            val finalCardTexture = theme.cardTexture
            if (finalCardTexture != null) {
                ivCardTexture.visibility = View.VISIBLE
                val resId = adapter.getCachedResId(finalCardTexture, context.packageName, context)
                if (resId != 0) {
                    ivCardTexture.setImageResource(resId)
                    if (finalCardTexture == "card_texture_aot") ivCardTexture.clearColorFilter()
                    else ivCardTexture.setColorFilter(adjustAlpha(theme.colors.primary, 0.55f))
                } else ivCardTexture.visibility = View.GONE
            } else if (theme.texturePath != null) {
                val texturesDir = java.io.File(context.filesDir, "themes/${theme.id}")
                val textureFile = java.io.File(texturesDir, theme.texturePath)
                if (textureFile.exists()) {
                    val texPath = "file://${textureFile.absolutePath}"
                    val drawable = ThemeDrawableLoader.loadDrawable(context, texPath)
                    if (drawable != null) {
                        ivCardTexture.visibility = View.VISIBLE
                        ivCardTexture.setImageDrawable(drawable)
                        ivCardTexture.setColorFilter(adjustAlpha(theme.colors.primary, 0.55f))
                    } else {
                        ivCardTexture.visibility = View.GONE
                    }
                } else {
                    ivCardTexture.visibility = View.GONE
                }
            } else ivCardTexture.visibility = View.GONE

            if (isEditMode && app.packageName.isNotEmpty() && !isFolderActionAdd && !isPlaceholder) {
                if (container.animation == null) {
                    val rotate = RotateAnimation(-1.5f, 1.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                        duration = 130
                        repeatCount = Animation.INFINITE
                        repeatMode = Animation.REVERSE
                    }
                    container.startAnimation(rotate)
                }
                btnDeleteApp.visibility = View.VISIBLE
                btnDeleteApp.setOnClickListener {
                    onDeleteClick?.invoke(app)
                }
            } else {
                container.clearAnimation()
                btnDeleteApp.visibility = View.GONE
            }

            container.tag = app
            if (gestureController != null) {
                val isDraggable = app.packageName.isNotEmpty() && app.packageName != "com.alisu.alauncher.drawer_trigger" && app.packageName != "folder_action_add" && !isPlaceholder
                container.setOnTouchListener { v, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        gestureController.setDragCandidate(isDraggable, v)
                    }
                    gestureController.onTouch(v, event)
                }
                container.setOnClickListener {
                    if (isEditMode) return@setOnClickListener
                    if (isFolder || isPlaceholder) {
                        val isMiniPreview = (folderSize == 2 && cellIndex == 1) || (folderSize == 4 && cellIndex == 3)
                        val isEnlarged = folderSize == 2 || folderSize == 4
                        if (!isEnlarged || isMiniPreview) {
                            onAppClick(AppInfo(label = "", packageName = targetFolderId))
                        } else {
                            val savedApps = prefs.getString("folder_apps_$targetFolderId", "") ?: ""
                            val folderPackages = savedApps.split(",").filter { it.isNotEmpty() }
                            if (cellIndex < folderPackages.size) com.alisu.alauncher.launcher.AppLoader.launchApp(context, folderPackages[cellIndex])
                            else onAppClick(AppInfo(label = "", packageName = targetFolderId))
                        }
                    } else onAppClick(app)
                }
            } else {
                container.setOnTouchListener { v, event ->
                    val ic = v.findViewById<View>(R.id.fl_icon_container)
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            v.isPressed = true
                            ic?.isPressed = true
                            v.animate().scaleX(0.92f).scaleY(0.92f)
                                .setDuration(120)
                                .setInterpolator(android.view.animation.DecelerateInterpolator())
                                .start()
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                v.background?.setHotspot(event.x, event.y)
                                v.foreground?.setHotspot(event.x, event.y)
                                if (ic != null) {
                                    val relX = event.x - ic.left
                                    val relY = event.y - ic.top
                                    ic.background?.setHotspot(relX, relY)
                                }
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.isPressed = false
                            ic?.isPressed = false
                            v.animate().scaleX(1.0f).scaleY(1.0f)
                                .setDuration(180)
                                .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                                .start()
                            if (event.actionMasked == MotionEvent.ACTION_UP) v.performClick()
                        }
                    }
                    true
                }
                container.setOnClickListener {
                    onAppClick(app)
                }
            }
        }

        private fun setupFolderPreview(
            previewContainer: FrameLayout,
            folderPackages: List<String>,
            useMonochromatic: Boolean,
            theme: AppTheme,
            adapter: AppAdapter
        ) {
            val context = previewContainer.context
            val numApps = folderPackages.size
            val mono = adapter.getCachedMonoTransformation(theme.colors.primary, theme.colors.surface)

            if (numApps == 0) {
                previewContainer.removeAllViews()
                val iconSizePx = dpToPx(context, 20f).toInt()
                val miniIv = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(iconSizePx, iconSizePx, Gravity.CENTER)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageResource(R.drawable.ic_folder)
                    setColorFilter(adjustAlpha(theme.colors.primary, 0.4f), android.graphics.PorterDuff.Mode.SRC_IN)
                }
                previewContainer.addView(miniIv)
                return
            }

            val neededViews = when {
                numApps == 1 -> 1
                numApps == 2 -> 2
                else -> 4
            }

            while (previewContainer.childCount > neededViews) {
                previewContainer.removeViewAt(previewContainer.childCount - 1)
            }

            val iconSizePx = when {
                numApps == 1 -> dpToPx(context, 44f).toInt()
                numApps == 2 -> dpToPx(context, 32f).toInt()
                else -> dpToPx(context, 24f).toInt()
            }

            val gapPx = dpToPx(context, 1f).toInt()

            val density = context.resources.displayMetrics.density
            val containerSizeDp = when (adapter.cachedIconSizePref ?: "medium") {
                "small" -> 54f
                "large" -> 74f
                else -> 64f
            }
            val containerSizePx = (containerSizeDp * density).toInt()
            val shape = adapter.cachedIconShapePref ?: "theme"
            val containerRadiusPx = when (shape) {
                "circle" -> containerSizePx / 2f
                "square" -> 0f
                "squircle" -> 18f * density
                else -> theme.shapes.cardCornerRadius.toFloat() * density
            }

            val folderBg = GradientDrawable().apply {
                if (shape == "circle") {
                    this.shape = GradientDrawable.OVAL
                } else {
                    this.shape = GradientDrawable.RECTANGLE
                    cornerRadius = containerRadiusPx
                }
                setColor(adjustAlpha(theme.colors.surface, 0.45f))
                if (theme.shapes.showBorders) {
                    setStroke((1f * density).toInt(), adjustAlpha(theme.colors.primary, 0.25f))
                }
            }
            previewContainer.background = folderBg

            val radiusPx = when (shape) {
                "circle" -> iconSizePx / 2f
                "square" -> 0f
                "squircle" -> 18f * density * (iconSizePx.toFloat() / containerSizePx)
                else -> theme.shapes.cardCornerRadius.toFloat() * density * (iconSizePx.toFloat() / containerSizePx)
            }
            val shapeMode = adapter.cachedIconShapeModePref ?: "force_launcher"
            val shapeClip = if (shapeMode == "force_launcher") ShapeClippingTransformation(shape, radiusPx) else null

            if (numApps == 1) {
                val miniIv = if (previewContainer.childCount > 0) {
                    previewContainer.getChildAt(0) as? ImageView
                } else {
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        previewContainer.addView(this)
                    }
                }
                miniIv?.let { iv ->
                    iv.layoutParams = FrameLayout.LayoutParams(iconSizePx, iconSizePx, Gravity.CENTER)
                    iv.load("app-icon://${folderPackages[0]}") {
                        size(iconSizePx, iconSizePx)
                        allowHardware(true)
                        memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        if (useMonochromatic) {
                            val transforms = mutableListOf<coil.transform.Transformation>(mono)
                            if (shapeClip != null) transforms.add(shapeClip)
                            transformations(transforms)
                        } else if (shapeClip != null) {
                            transformations(shapeClip)
                        }
                    }
                }
            } else {
                // 2 ou 4 ícones: usar LinearLayout com grid
                previewContainer.removeAllViews()
                val gridLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                }

                val rows = if (numApps <= 2) 1 else 2
                val cols = if (numApps <= 2) numApps else 2
                var idx = 0

                for (r in 0 until rows) {
                    val row = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            if (r > 0) setMargins(0, gapPx, 0, 0)
                        }
                    }
                    for (c in 0 until cols) {
                        if (idx >= numApps) break
                        val miniIv = ImageView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
                                if (c > 0) setMargins(gapPx, 0, 0, 0)
                            }
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }
                        miniIv.load("app-icon://${folderPackages[idx]}") {
                            size(iconSizePx, iconSizePx)
                            allowHardware(true)
                            memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            if (useMonochromatic) {
                                val transforms = mutableListOf<coil.transform.Transformation>(mono)
                                if (shapeClip != null) transforms.add(shapeClip)
                                transformations(transforms)
                            } else if (shapeClip != null) {
                                transformations(shapeClip)
                            }
                        }
                        row.addView(miniIv)
                        idx++
                    }
                    gridLayout.addView(row)
                }
                previewContainer.addView(gridLayout)
            }
        }

        private fun dpToPx(context: Context, dp: Float): Float {
            return dp * context.resources.displayMetrics.density
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
