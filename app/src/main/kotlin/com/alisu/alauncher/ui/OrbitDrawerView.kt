package com.alisu.alauncher.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.alisu.alauncher.model.AppInfo
import com.alisu.alauncher.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class OrbitDrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var apps: List<AppInfo> = emptyList()
    private var iconBitmaps: MutableMap<String, Bitmap> = mutableMapOf()
    private var loadJob: Job? = null

    private var nodes: List<SphereNode> = emptyList()
    private var scale = 0f

    // ── Quaternion state + Y/X velocities ──────────────────────────
    private var qw = 1f; private var qx = 0f; private var qy = 0f; private var qz = 0f
    private var velY = 0f   // angular velocity around Y (horizontal)
    private var velX = 0f   // angular velocity around X (vertical, clamped)
    private var isDragging = false
    private var pointerId = -1

    private var sphereRadius = 0f
    private var viewCenterX = 0f
    private var viewCenterY = 0f

    private var cachedDensity = 0f
    private var iconContainerSize = 0f

    // ── Themed card ─────────────────────────────────────────────────
    private val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private var cardCornerRadius = 0f
    private var showCardBorder = true
    private var cardTexture: Drawable? = null
    private var iconShapePref = "theme"

    private val tmpRectF = RectF()

    private val gestureDetector: GestureDetector
    private var launchCallback: ((String) -> Unit)? = null
    private var velocityTracker: VelocityTracker? = null

    private var glowColor = Color.argb(40, Color.red(Color.parseColor("#BB86FC")), Color.green(Color.parseColor("#BB86FC")), Color.blue(Color.parseColor("#BB86FC")))
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 600
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            scale = animatedValue as Float
            invalidate()
        }
    }

    data class SphereNode(
        val app: AppInfo,
        val baseX: Float,
        val baseY: Float,
        val baseZ: Float
    ) {
        var screenX = 0f
        var screenY = 0f
        var depth = 1f
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                var closest: SphereNode? = null
                var closestDist = Float.MAX_VALUE
                val hitRadius = dpToPx(36f)

                for (node in nodes) {
                    val dx = e.x - (viewCenterX + node.screenX)
                    val dy = e.y - (viewCenterY + node.screenY)
                    val dist = sqrt(dx * dx + dy * dy)
                    val scaledHit = hitRadius * node.depth
                    if (dist < scaledHit && dist < closestDist && node.depth > 0.3f) {
                        closestDist = dist
                        closest = node
                    }
                }
                closest?.let {
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    launchCallback?.invoke(it.app.packageName)
                }
                return true
            }
        })
    }

    fun setApps(appList: List<AppInfo>) {
        apps = appList
        loadIcons()
        buildSphere()
        scale = 0f
        animator.cancel()
        animator.start()
    }

    fun setOnAppLaunchCallback(callback: (String) -> Unit) {
        launchCallback = callback
    }

    fun applyTheme(theme: AppTheme) {
        cachedDensity = resources.displayMetrics.density

        val pr = Color.red(theme.colors.primary)
        val pg = Color.green(theme.colors.primary)
        val pb = Color.blue(theme.colors.primary)

        glowColor = Color.argb(50, pr, pg, pb)
        val glowMid = Color.argb(20, pr, pg, pb)
        if (width > 0 && height > 0) {
            glowPaint.shader = RadialGradient(
                viewCenterX, viewCenterY, max(width, height) * 0.5f,
                intArrayOf(glowColor, glowMid, Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
            )
        }

        val prefs = context.getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE)
        val iconSizePref = prefs.getString("icon_size", "medium") ?: "medium"
        val containerDp = when (iconSizePref) { "small" -> 54f; "large" -> 74f; else -> 64f }
        val newSize = dpToPx(containerDp)
        val sizeChanged = newSize != iconContainerSize
        iconContainerSize = newSize
        val oldShape = iconShapePref
        iconShapePref = prefs.getString("icon_shape", "theme") ?: "theme"
        val shapeChanged = oldShape != iconShapePref
        cardCornerRadius = theme.shapes.cardCornerRadius.toFloat() * cachedDensity
        showCardBorder = theme.shapes.showBorders
        cardBgPaint.color = adjustAlpha(theme.colors.surface, 0.75f)
        cardBorderPaint.strokeWidth = 1f * cachedDensity
        cardBorderPaint.color = adjustAlpha(theme.colors.primary, 0.40f)

        if (theme.cardTexture != null) {
            val resId = try {
                val id = context.resources.getIdentifier(theme.cardTexture, "drawable", context.packageName)
                if (id != 0) id else context.resources.getIdentifier("card_texture_aot", "drawable", context.packageName)
            } catch (_: Exception) { 0 }
            cardTexture = if (resId != 0) {
                val d = context.getDrawable(resId)
                d?.colorFilter = android.graphics.PorterDuffColorFilter(adjustAlpha(theme.colors.primary, 0.55f), android.graphics.PorterDuff.Mode.SRC_ATOP)
                d
            } else null
        } else if (theme.texturePath != null) {
            val texturesDir = java.io.File(context.filesDir, "themes/${theme.id}")
            val textureFile = java.io.File(texturesDir, theme.texturePath)
            cardTexture = if (textureFile.exists()) {
                val d = com.alisu.alauncher.theme.ThemeDrawableLoader.loadDrawable(context, "file://${textureFile.absolutePath}")
                d?.colorFilter = android.graphics.PorterDuffColorFilter(adjustAlpha(theme.colors.primary, 0.55f), android.graphics.PorterDuff.Mode.SRC_ATOP)
                d
            } else null
        } else cardTexture = null

        val useMono = prefs.getBoolean("monochromatic_icons", true)
        if (useMono) {
            val r = pr / 255f; val g = pg / 255f; val b = pb / 255f
            iconPaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                0.2126f * r, 0.7152f * r, 0.0722f * r, 0f, 0f,
                0.2126f * g, 0.7152f * g, 0.0722f * g, 0f, 0f,
                0.2126f * b, 0.7152f * b, 0.0722f * b, 0f, 0f,
                0f,          0f,          0f,          1f, 0f
            )))
        } else {
            iconPaint.colorFilter = null
        }

        if ((sizeChanged || shapeChanged) && apps.isNotEmpty()) {
            loadIcons()
        }

        invalidate()
    }

    private fun loadIcons() {
        loadJob?.cancel()
        val appListCopy = ArrayList(apps)
        val pm = context.packageManager
        val px = iconContainerSize.toInt().coerceAtLeast(1)

        loadJob = CoroutineScope(Dispatchers.IO).launch {
            val newBitmaps = mutableMapOf<String, Bitmap>()
            for (app in appListCopy) {
                if (!isActive) break
                try {
                    val info = pm.getApplicationInfo(app.packageName, 0)
                    val d = com.alisu.alauncher.launcher.IconPackManager.getIcon(context, app.packageName)
                        ?: com.alisu.alauncher.launcher.IconPackManager.getRawIcon(context, app.packageName)
                        ?: pm.getApplicationIcon(info)
                    val bmp = drawableToBitmap(d, px)
                    if (bmp != null) {
                        newBitmaps[app.packageName] = bmp
                    }
                } catch (_: Exception) {}
            }
            if (isActive) {
                withContext(Dispatchers.Main) {
                    iconBitmaps.clear()
                    iconBitmaps.putAll(newBitmaps)
                    invalidate()
                }
            }
        }
    }

    private fun drawableToBitmap(d: Drawable, size: Int): Bitmap? {
        try {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val pad = (size * 0.05f).toInt()
            val safeW = size - 2 * pad
            val safeH = size - 2 * pad

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && d is android.graphics.drawable.AdaptiveIconDrawable) {
                val path = android.graphics.Path()
                val w = safeW.toFloat()
                val h = safeH.toFloat()

                when (iconShapePref) {
                    "circle" -> {
                        path.addCircle(w / 2f, h / 2f, w / 2f, android.graphics.Path.Direction.CW)
                    }
                    "square" -> {
                        path.addRect(0f, 0f, w, h, android.graphics.Path.Direction.CW)
                    }
                    "squircle" -> {
                        val r = w * 0.42f
                        path.addRoundRect(0f, 0f, w, h, r, r, android.graphics.Path.Direction.CW)
                    }
                    else -> {
                        val r = w * 0.24f
                        path.addRoundRect(0f, 0f, w, h, r, r, android.graphics.Path.Direction.CW)
                    }
                }

                c.save()
                c.translate(pad.toFloat(), pad.toFloat())
                c.clipPath(path)

                val bg = d.background
                bg.setBounds(0, 0, safeW, safeH)
                bg.draw(c)

                val fg = d.foreground
                fg.setBounds(0, 0, safeW, safeH)
                fg.draw(c)

                c.restore()
            } else {
                d.setBounds(pad, pad, size - pad, size - pad)
                d.draw(c)
            }
            return bmp
        } catch (_: Exception) { return null }
    }

    private fun buildSphere() {
        val n = apps.size
        if (n == 0) return
        val list = mutableListOf<SphereNode>()
        val golden = (1f + sqrt(5f)) / 2f

        for (i in 0 until n) {
            val theta = acos(1f - 2f * (i + 0.5f) / n).toFloat()
            val phi = 2f * PI.toFloat() * i / golden
            list.add(SphereNode(
                apps[i],
                sin(theta) * cos(phi),
                sin(theta) * sin(phi),
                cos(theta)
            ))
        }
        nodes = list
    }

    // ── Quaternion helpers ──────────────────────────────────────────

    private fun qNorm(w: Float, x: Float, y: Float, z: Float): FloatArray {
        val len = sqrt(w * w + x * x + y * y + z * z)
        return if (len > 0f) floatArrayOf(w / len, x / len, y / len, z / len) else floatArrayOf(1f, 0f, 0f, 0f)
    }

    private fun qMul(a: FloatArray, b: FloatArray): FloatArray = floatArrayOf(
        a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3],
        a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2],
        a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1],
        a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0]
    )

    private fun qFromAxisAngle(ax: Float, ay: Float, az: Float, angle: Float): FloatArray {
        val half = angle * 0.5f
        val s = sin(half)
        val len = sqrt(ax * ax + ay * ay + az * az)
        if (len < 1e-6f) return floatArrayOf(1f, 0f, 0f, 0f)
        val il = 1f / len
        return floatArrayOf(cos(half), ax * il * s, ay * il * s, az * il * s)
    }

    /** Aplica quat (w,x,y,z) ao vetor (vx,vy,vz) */
    private fun qRot(q: FloatArray, vx: Float, vy: Float, vz: Float): FloatArray {
        val ux = q[1]; val uy = q[2]; val uz = q[3]
        val s  = q[0]
        val dot = ux * vx + uy * vy + uz * vz
        val cx = uy * vz - uz * vy
        val cy = uz * vx - ux * vz
        val cz = ux * vy - uy * vx
        val uu = ux * ux + uy * uy + uz * uz
        return floatArrayOf(
            vx + 2f * (s * cx + dot * ux - uu * vx),
            vy + 2f * (s * cy + dot * uy - uu * vy),
            vz + 2f * (s * cz + dot * uz - uu * vz)
        )
    }

    /** Mapeia toque (centrado, normalizado) pra esfera unitária */
    private fun touchToSphere(sx: Float, sy: Float): FloatArray {
        val r = max(1f, sphereRadius * scale)
        val x = sx / r; val y = sy / r
        val d = x * x + y * y
        return if (d < 1f) floatArrayOf(x, y, sqrt(1f - d))
        else floatArrayOf(x / sqrt(d), y / sqrt(d), 0f)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewCenterX = w / 2f
        viewCenterY = h / 2f
        sphereRadius = min(w, h) * 0.52f
        glowPaint.shader = RadialGradient(
            viewCenterX, viewCenterY, max(w, h) * 0.5f,
            intArrayOf(glowColor, Color.argb(Color.alpha(glowColor) / 2, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
    }

    // ── Draw ────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (nodes.isEmpty() || scale <= 0f) return

        canvas.drawCircle(viewCenterX, viewCenterY, max(viewCenterX, viewCenterY) * scale, glowPaint)

        val cx = viewCenterX
        val cy = viewCenterY
        val effR = sphereRadius * scale

        // ── Momentum + idle (só Y e X) ──────────────────────────────
        if (!isDragging) {
            val f = 0.96f
            velY *= f; velX *= f
            if (abs(velY) < 1e-5f && abs(velX) < 1e-5f) {
                velY = 0f; velX = 0f
                val idle = qFromAxisAngle(0f, 1f, 0f, 0.005f)
                val nq = qMul(idle, qNorm(qw, qx, qy, qz))
                qw = nq[0]; qx = nq[1]; qy = nq[2]; qz = nq[3]
            } else {
                val dy = qFromAxisAngle(0f, 1f, 0f, velY)
                val dx = qFromAxisAngle(1f, 0f, 0f, velX)
                val nq = qMul(qMul(dy, dx), qNorm(qw, qx, qy, qz))
                qw = nq[0]; qx = nq[1]; qy = nq[2]; qz = nq[3]
            }
        }

        val rot = qNorm(qw, qx, qy, qz)

        for (node in nodes) {
            val r = qRot(rot, node.baseX * effR, node.baseY * effR, node.baseZ * effR)
            node.screenX = r[0]
            node.screenY = r[1]
            node.depth = ((r[2] + effR) / (2f * effR)).coerceIn(0.35f, 1f)
        }

        nodes.sortedBy { it.depth }.forEach { node ->
            val d = node.depth
            val iconSize = iconContainerSize * (0.5f + d * 0.5f)
            val half = iconSize / 2f
            val alpha = (50f + d * 160f).toInt().coerceIn(50, 230)

            val left = cx + node.screenX - half
            val top = cy + node.screenY - half

            val bitmap = iconBitmaps[node.app.packageName]

            if (d > 0.4f) {
                val sa = (d * 50).toInt()
                shadowPaint.color = Color.argb(sa, 0, 0, 0)
                canvas.drawOval(
                    cx + node.screenX - half * 0.35f,
                    cy + node.screenY + half * 0.3f,
                    cx + node.screenX + half * 0.35f,
                    cy + node.screenY + half * 0.5f,
                    shadowPaint
                )
            }

            if (d <= 0.2f || bitmap == null || bitmap.isRecycled) return@forEach

            canvas.save()
            val path = android.graphics.Path()
            if (iconShapePref == "circle") {
                path.addOval(left, top, left + iconSize, top + iconSize, android.graphics.Path.Direction.CW)
            } else {
                val shapeRadius = when (iconShapePref) {
                    "square" -> 0f
                    "squircle" -> dpToPx(18f) * (0.5f + d * 0.5f)
                    else -> cardCornerRadius * (0.5f + d * 0.5f)
                }
                path.addRoundRect(left, top, left + iconSize, top + iconSize, shapeRadius, shapeRadius, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(path)

            iconPaint.alpha = alpha
            tmpRectF.set(left, top, left + iconSize, top + iconSize)
            canvas.drawBitmap(bitmap, null, tmpRectF, iconPaint)
            canvas.restore()
        }

        if (!isDragging) invalidate()
    }

    // ── Touch (arcball no arrasto, momentum Y/X na soltura) ─────────

    private var lastX = 0f
    private var lastY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointerId = event.getPointerId(0)
                lastX = event.x; lastY = event.y
                isDragging = true
                velY = 0f; velX = 0f
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(event)
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerId < 0) return@onTouchEvent true
                val idx = event.findPointerIndex(pointerId)
                if (idx < 0) return@onTouchEvent true
                val sx = event.getX(idx) - viewCenterX
                val sy = event.getY(idx) - viewCenterY
                val lx = lastX - viewCenterX
                val ly = lastY - viewCenterY

                val from = touchToSphere(lx, ly)
                val to   = touchToSphere(sx, sy)

                val ax = from[1] * to[2] - from[2] * to[1]
                val ay = from[2] * to[0] - from[0] * to[2]
                val az = from[0] * to[1] - from[1] * to[0]
                val dot = from[0] * to[0] + from[1] * to[1] + from[2] * to[2]
                val angle = acos(dot.coerceIn(-1f, 1f))

                if (angle > 1e-4f) {
                    val dq = qFromAxisAngle(ax, ay, az, angle)
                    val nq = qMul(dq, qNorm(qw, qx, qy, qz))
                    qw = nq[0]; qx = nq[1]; qy = nq[2]; qz = nq[3]
                }

                lastX = event.getX(idx); lastY = event.getY(idx)
                velocityTracker?.addMovement(event)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.computeCurrentVelocity(1000)
                val r = max(1f, sphereRadius * scale)
                val toAngular = 1f / (r * 60f) // px/s → rad/frame
                velY = (velocityTracker?.xVelocity ?: 0f) * toAngular * 0.8f
                velX = (velocityTracker?.yVelocity ?: 0f) * toAngular * 0.6f
                velY = velY.coerceIn(-0.05f, 0.05f)
                velX = velX.coerceIn(-0.03f, 0.03f)
                velocityTracker?.recycle()
                velocityTracker = null
                isDragging = false
                pointerId = -1
                invalidate()
            }
        }
        return gestureDetector.onTouchEvent(event)
    }

    private fun dpToPx(dp: Float): Float = dp * (if (cachedDensity > 0f) cachedDensity else resources.displayMetrics.density)

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loadJob?.cancel()
    }

    companion object {
        fun adjustAlpha(color: Int, factor: Float): Int {
            val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }
    }
}
