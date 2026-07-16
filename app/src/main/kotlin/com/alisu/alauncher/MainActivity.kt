package com.alisu.alauncher

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextClock
import android.widget.ScrollView
import android.widget.FrameLayout
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.LauncherApps
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.DynamicAnimation
// ── Novos componentes da arquitetura de home screen ──────────────────────────
import com.alisu.alauncher.drag.DragController
import com.alisu.alauncher.drag.DragLayer
import com.alisu.alauncher.gesture.GestureController
import com.alisu.alauncher.gesture.LawnchairGestureController
import com.alisu.alauncher.popup.PopupController
import com.alisu.alauncher.workspace.WorkspaceLayout
import com.alisu.alauncher.dock.DockLayout
import com.alisu.alauncher.workspace.CellPosition
// ── MediaPlayerExt.kt ─────────────────────────────────────────────────────────
import com.alisu.alauncher.applyMusicPlayerTheme
import com.alisu.alauncher.reconnectMediaSession
import com.alisu.alauncher.formatMediaTime
import com.alisu.alauncher.updateMediaPlayerUI
import com.alisu.alauncher.startMediaProgressTracking
import com.alisu.alauncher.stopMediaProgressTracking
import com.alisu.alauncher.sendMediaTransportCommand
// ─────────────────────────────────────────────────────────────────────────────
import com.alisu.alauncher.launcher.AppIconFetcher
import com.alisu.alauncher.launcher.AppLoader
import com.alisu.alauncher.model.AppInfo
import com.alisu.alauncher.theme.AppTheme
import com.alisu.alauncher.theme.ThemeDrawableLoader
import com.alisu.alauncher.theme.ThemeManager
import com.alisu.alauncher.ui.AppAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Collections

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    // Main Layout Views
    internal lateinit var rootLayout: View
    internal lateinit var ivWallpaper: ImageView
    // vpHomePages removido - agora usa FrameLayout com views separadas
    internal lateinit var llPageIndicator: LinearLayout
    internal var llDockContainer: LinearLayout? = null
    internal var rvDockApps: RecyclerView? = null

    // Global Search Views
    internal lateinit var clGlobalSearchContainer: View
    internal lateinit var etGlobalSearch: EditText
    internal lateinit var btnCloseGlobalSearch: View
    internal lateinit var rvGlobalSearchResults: RecyclerView
    internal lateinit var tvGlobalSearchEmpty: TextView

    // Welcome Overlay Views
    private lateinit var clWelcomeOverlay: View
    private lateinit var ivWelcomeLogo: ImageView
    private lateinit var tvWelcomeTitle: TextView
    private lateinit var btnWelcomeSetDefault: View
    private lateinit var tvWelcomeDefaultLabel: TextView
    private lateinit var ivWelcomeDefaultIcon: ImageView
    private lateinit var btnWelcomeStart: View
    private lateinit var tvWelcomeStartLabel: TextView

    // View References for Pager Pages (inflated dynamically)
    internal var desktopViewInstance: View? = null
    internal var libraryViewInstance: View? = null
    internal var widgetsViewInstance: View? = null

    // Cache para referências de views do desktop widget (evita perda ao desanexar em Minimal Style)
    private var lastDesktopViewRef: View? = null
    private var desktopMascotContainer: LinearLayout? = null
    private var desktopIvMascot: ImageView? = null
    private var desktopIvWeatherIcon: ImageView? = null
    private var desktopTcTime: TextClock? = null
    private var desktopTcDate: TextClock? = null
    private var desktopTvWeatherTemp: TextView? = null
    private var desktopTvWeatherCondition: TextView? = null

    // Adapters & Datasets
    // homePagerAdapter removido - agora usa FrameLayout com views separadas
    internal var dockAdapter: AppAdapter? = null
    internal var desktopAdapter: AppAdapter? = null
    internal var libraryAdapter: AppAdapter? = null
    internal var globalSearchAdapter: AppAdapter? = null
    private var globalGestureDetector: GestureDetector? = null

    // ── Arquitetura de Home Screen (Fase 1 & 2) ──────────────────────────────
    /** Camada de drag — hospeda o ícone flutuante durante arraste. */
    private lateinit var dragLayer: DragLayer
    /** Orquestra o ciclo de vida do drag (start/move/end/cancel). */
    private lateinit var dragController: DragController
    /** Máquina de estados de toque — distingue clique, scroll e drag. */
    private lateinit var gestureController: GestureController
    private lateinit var popupController: PopupController
    private lateinit var lawnchairGestureController: LawnchairGestureController
    // ─────────────────────────────────────────────────────────────────────────

    private val REQUEST_PICK_APPWIDGET = 100
    private val REQUEST_CREATE_APPWIDGET = 101
    private val REQUEST_BIND_APPWIDGET = 102
    private val REQUEST_BIND_PIN_APPWIDGET = 103
    internal lateinit var appWidgetManager: AppWidgetManager
    internal lateinit var appWidgetHost: AppWidgetHost
    private var pendingPinRequest: LauncherApps.PinItemRequest? = null
    private var pendingWidgetId: Int = -1
    /** ID do widget aguardando retorno de REQUEST_CREATE_APPWIDGET (configure activity). */
    private var pendingConfigWidgetId: Int = -1
    private var pendingBindWidgetId: Int = -1

    @Volatile internal var allAppsList = listOf<AppInfo>()
    private val appsListMutex = java.util.concurrent.locks.ReentrantReadWriteLock()
    private var homeAppsPackages = mutableListOf<String>()
    private var dockAppsPackages = mutableListOf<String>()
    internal var alertDialog: AlertDialog? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isAppsLoaded = false
    private var dragStartPos = -1
    private val searchDebounceHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchDebounceRunnable: Runnable? = null
    internal var lastWeatherFetchTime = 0L

    // ── MediaSession Player ─────────────────────────────────────────────
    internal var mediaActiveController: android.media.session.MediaController? = null
    internal var mediaCallback: android.media.session.MediaController.Callback? = null
    internal var mediaProgressHandler: android.os.Handler? = null
    internal var mediaProgressRunnable: Runnable? = null
    internal var mediaUiViews: MediaPlayerViews? = null
    internal var mediaSessionManager: android.media.session.MediaSessionManager? = null
    internal var mediaSessionListener: android.media.session.MediaSessionManager.OnActiveSessionsChangedListener? = null

    internal data class MediaPlayerViews(
        val tvSongTitle: TextView,
        val tvSongArtist: TextView,
        val tvMusicPlayerStatus: TextView,
        val ivPlayPause: ImageView,
        val ivAlbumArt: ImageView,
        val tvCurrentTime: TextView,
        val tvTotalTime: TextView,
        val viewProgressFill: View,
        val viewProgressThumb: View,
        val cardMusicPlayer: FrameLayout?,
        val flProgressBar: FrameLayout?,
        val ivCompactToggle: ImageView?,
        val btnQueue: FrameLayout?,
        val ivQueue: ImageView?,
        val flAlbumArt: FrameLayout?,
        val ivMusicCardBackdrop: ImageView?
    )

    internal val prefs by lazy { getSharedPreferences("alauncher_prefs", Context.MODE_PRIVATE) }
    private var lastDrawerStyle = ""

    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "weather_location", "show_mascot" -> {
                lastWeatherFetchTime = 0L
                fetchWeatherData()
                reconnectMediaSession()
                desktopViewInstance?.let { view ->
                    updateDesktopClockAndMascot(view, ThemeManager.currentTheme.value)
                }
            }
            "clock_style" -> {
                reconnectMediaSession()
                desktopViewInstance?.let { view ->
                    updateDesktopClockAndMascot(view, ThemeManager.currentTheme.value)
                }
            }
            "monochromatic_icons", "icon_size", "grid_columns", "show_app_name", "icon_shape", "icon_shape_mode" -> {
                AppIconFetcher.invalidateCache()
                applyTheme(ThemeManager.currentTheme.value)
            }
        }
    }

    inner class OneSecondLongPressListener(
        private val getCoordinates: (MotionEvent) -> Pair<Float, Float>,
        private val onClick: () -> Unit
    ) : View.OnTouchListener {
        private val handler = android.os.Handler(android.os.Looper.getMainLooper())
        private var downX = 0f
        private var downY = 0f
        private var isLongPressActive = false
        private var hasTriggered = false
        private val touchSlop = android.view.ViewConfiguration.get(this@MainActivity).scaledTouchSlop

        private val longPressRunnable = Runnable {
            if (isLongPressActive && !hasTriggered) {
                hasTriggered = true
                if (desktopAdapter?.getEditMode() != true) {
                    val coords = lastCoords ?: Pair(downX, downY)
                    showDesktopContextMenu(coords.first.toInt(), coords.second.toInt())
                }
            }
        }

        private var lastCoords: Pair<Float, Float>? = null

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (desktopAdapter?.getEditMode() == true) {
                cancel()
                return false
            }

            val coords = getCoordinates(event)
            lastCoords = coords

            lastTouchX = coords.first
            lastTouchY = coords.second

            val isRecyclerView = v is RecyclerView
            if (isRecyclerView) {
                val rv = v as RecyclerView
                val child = rv.findChildViewUnder(event.x, event.y)
                if (child != null) {
                    val pos = rv.getChildAdapterPosition(child)
                    val adapter = rv.adapter as? AppAdapter
                    val isEmptyCell = adapter?.isEmptyPosition(pos) ?: false
                    if (!isEmptyCell) {
                        cancel()
                        return false
                    }
                }
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    isLongPressActive = true
                    hasTriggered = false
                    handler.postDelayed(longPressRunnable, 1000L)
                    if (isRecyclerView) return false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isLongPressActive) {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (Math.hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                            cancel()
                        }
                    }
                    if (isRecyclerView) return false
                }
                MotionEvent.ACTION_UP -> {
                    val wasActive = isLongPressActive
                    val triggered = hasTriggered
                    cancel()
                    if (triggered) return true
                    if (isRecyclerView) return false
                    if (wasActive) {
                        onClick()
                        return true
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    cancel()
                    if (isRecyclerView) return false
                }
            }
            return true
        }

        private fun cancel() {
            handler.removeCallbacks(longPressRunnable)
            isLongPressActive = false
        }
    }

    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (!isTaskRoot) {
                finish()
                return
            }
        }

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isReady }
        super.onCreate(savedInstanceState)
        
        // Inicializar ImageLoader do Coil com suporte ao AppIconFetcher e limites rígidos de RAM
        val imageLoader = coil.ImageLoader.Builder(applicationContext)
            .components {
                add(com.alisu.alauncher.launcher.AppIconFetcher.Factory(applicationContext))
            }
            .memoryCache {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val cachePercent = if (activityManager.isLowRamDevice) 0.08 else 0.18
                coil.memory.MemoryCache.Builder(applicationContext)
                    .maxSizePercent(cachePercent)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .allowHardware(true)
            .build()
        coil.Coil.setImageLoader(imageLoader)
        
        // Habilitar Edge-to-Edge nativo
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_main)
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)

        // Inicializar AppWidget Host e Manager
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = com.alisu.alauncher.widget.WidgetHostProvider.getAppWidgetHost(this)
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Inicializar Welcome Overlay
        clWelcomeOverlay = findViewById(R.id.cl_welcome_overlay)
        ivWelcomeLogo = findViewById(R.id.iv_welcome_logo)
        tvWelcomeTitle = findViewById(R.id.tv_welcome_title)
        btnWelcomeSetDefault = findViewById(R.id.btn_welcome_set_default)
        tvWelcomeDefaultLabel = findViewById(R.id.tv_welcome_default_label)
        ivWelcomeDefaultIcon = findViewById(R.id.iv_welcome_default_icon)
        btnWelcomeStart = findViewById(R.id.btn_welcome_start)
        tvWelcomeStartLabel = findViewById(R.id.tv_welcome_start_label)

        // Inicializar Views Principais
        rootLayout = findViewById(R.id.root_layout)
        ivWallpaper = findViewById(R.id.iv_wallpaper)

        // ── Inicializar camada de drag e controladores (Fase 1 & 2) ──────────
        dragLayer = findViewById(R.id.drag_layer)
        dragController = DragController(
            dragLayer = dragLayer
        ).apply {
            onDragStartCallback = {
                if (currentPage != 1) {
                    navigateToPage(1)
                }
                enableEditMode()
                desktopAdapter?.setDragging(true)

                val rvDesktop = desktopViewInstance?.findViewById<com.alisu.alauncher.workspace.WorkspaceLayout>(R.id.rv_home_desktop_apps)
                if (rvDesktop != null) {
                    if (!dragController.dropTargets.contains(rvDesktop)) {
                        dragController.dropTargets.add(rvDesktop)
                    }
                    if (rvDesktop.onDropListener == null) {
                        rvDesktop.onDropListener = object : com.alisu.alauncher.workspace.WorkspaceLayout.OnDropListener {
                            override fun onDrop(item: AppInfo, targetIdx: Int) {
                                Log.d("DragDrop", "onDropWorkspace: item=${item.packageName} target=$targetIdx")
                                desktopAdapter?.setDragOverPosition(-1)
                                handleDropOnWorkspace(item, targetIdx)
                            }
                            override fun onDragOver(item: AppInfo, targetIdx: Int) {
                                desktopAdapter?.setDragOverPosition(targetIdx)
                            }
                            override fun onDragExit(item: AppInfo) {
                                desktopAdapter?.setDragOverPosition(-1)
                            }
                        }
                    }
                    rvDesktop.invalidateLocationCache()
                    rvDesktop.post {
                        rvDesktop.invalidateLocationCache()
                    }
                }

                val rvDock = rvDockApps as? com.alisu.alauncher.dock.DockLayout
                if (rvDock != null) {
                    if (!dragController.dropTargets.contains(rvDock)) {
                        dragController.dropTargets.add(rvDock)
                    }
                    if (rvDock.onDropListener == null) {
                        rvDock.onDropListener = object : com.alisu.alauncher.dock.DockLayout.OnDropListener {
                            override fun onDrop(item: AppInfo, targetIdx: Int) {
                                handleDropOnDock(item, targetIdx)
                            }
                        }
                    }
                    rvDock.invalidateLocationCache()
                    rvDock.post {
                        rvDock.invalidateLocationCache()
                    }
                }
            }
            onDragEndCallback = {
                desktopAdapter?.setDragging(false)
            }
        }
        lawnchairGestureController = LawnchairGestureController(this)
        gestureController = GestureController(
            context = this,
            dragController = dragController,
            callbacks = object : GestureController.Callbacks {
                override fun onLongPressOnEmpty(rawX: Float, rawY: Float) {
                    if (desktopAdapter?.getEditMode() != true) {
                        showDesktopContextMenu(rawX.toInt(), rawY.toInt())
                    }
                }
                override fun onLongPressOnIcon(view: View, item: AppInfo) {
                    if (currentPage == 2) {
                        showAppContextMenu(item, "library", view)
                    } else {
                        val contextType = if (view.parent is com.alisu.alauncher.dock.DockLayout) "dock" else "desktop"
                        showAppContextMenu(item, contextType, view)
                    }
                }
                override fun onSwipeUp() = lawnchairGestureController.onSwipeUp()
                override fun onSwipeDown() {
                    if (currentPage == 2) {
                        navigateToPage(1)
                    } else {
                        lawnchairGestureController.onSwipeDown()
                    }
                }
                override fun onSwipeLeft() {}
                override fun onSwipeRight() {
                    if (currentPage == 0) {
                        navigateToPage(1)
                    }
                }
                override fun onDoubleTap() = lawnchairGestureController.onDoubleTap()
                override fun isEditMode() = desktopAdapter?.getEditMode() == true
                override fun isAllAppsOpen() = currentPage == 2
                override fun canOpenAllApps() = currentPage == 1
                override fun isDesktopPage() = currentPage == 1
                override fun isWidgetsPage() = currentPage == 0
            }
        )
        popupController = PopupController(gestureController)

        // Configura o clique do botão de confirmação do modo de edição
        findViewById<LinearLayout>(R.id.ll_edit_confirm).setOnClickHaptic {
            disableEditMode()
        }
        // ─────────────────────────────────────────────────────────────────────

        // Os listeners de toque e clique de 1 segundo serão configurados em setupGlobalGestures()

    // vpHomePages removido - agora usa FrameLayout com views separadas
        llPageIndicator = findViewById(R.id.ll_page_indicator)

        // Inicializar Pesquisa Global
        clGlobalSearchContainer = findViewById(R.id.cl_global_search_container)
        etGlobalSearch = findViewById(R.id.et_global_search)
        btnCloseGlobalSearch = findViewById(R.id.btn_close_global_search)
        rvGlobalSearchResults = findViewById(R.id.rv_global_search_results)
        tvGlobalSearchEmpty = findViewById(R.id.tv_global_search_empty)

        // Inicializar Gerenciador de Temas (em background para evitar ANR)
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            ThemeManager.init(this@MainActivity)
        }

        // Inicializar Splash Transition Icon
        val splashTransitionIcon = findViewById<ImageView>(R.id.splash_transition_icon)
        splashTransitionIcon.setImageResource(R.drawable.ic_launcher_foreground)
        splashTransitionIcon.colorFilter = null

        // AndroidX SplashScreen exit animation (mesmo padrão do te)
        splashScreen.setOnExitAnimationListener { provider ->
            splashTransitionIcon.visibility = View.VISIBLE
            splashTransitionIcon.alpha = 1f
            splashTransitionIcon.scaleX = 1f
            splashTransitionIcon.scaleY = 1f

            provider.remove()

            val isInitialRun = prefs.getBoolean("is_first_launch", true)
            if (!isInitialRun) {
                // Ensure the indicator is invisible but laid out to get its position
                llPageIndicator.visibility = View.INVISIBLE

                llPageIndicator.post {
                    // Get indicator position on screen (target center)
                    val loc = IntArray(2)
                    llPageIndicator.getLocationOnScreen(loc)
                    val targetX = loc[0] + (llPageIndicator.width / 2f)
                    val targetY = loc[1] + (llPageIndicator.height / 2f)

                    // Get current icon position on screen (current center)
                    val startLoc = IntArray(2)
                    splashTransitionIcon.getLocationOnScreen(startLoc)
                    val currentX = startLoc[0] + (splashTransitionIcon.width / 2f)
                    val currentY = startLoc[1] + (splashTransitionIcon.height / 2f)

                    // Calculate translation deltas
                    val deltaX = targetX - currentX
                    val deltaY = targetY - currentY

                    splashTransitionIcon.animate()
                        .translationX(deltaX)
                        .translationY(deltaY)
                        .scaleX(0.28f) // Escala aumentada para ~35dp (partindo de 128dp)
                        .scaleY(0.28f)
                        .setDuration(1000) // Reduzido ligeiramente para ser mais responsivo após o carregamento
                        .setInterpolator(android.view.animation.DecelerateInterpolator(1.8f))
                        .withEndAction {
                            splashTransitionIcon.visibility = View.GONE
                            llPageIndicator.visibility = View.VISIBLE
                            llPageIndicator.alpha = 0f
                            // Fade-in mais lento para o indicador (600ms)
                            llPageIndicator.animate().alpha(1f).setDuration(500).start()
                        }
                        .start()
                }
            } else {
                splashTransitionIcon.animate()
                    .alpha(0f)
                    .scaleX(0.5f)
                    .scaleY(0.5f)
                    .setDuration(400)
                    .withEndAction {
                        splashTransitionIcon.visibility = View.GONE
                    }
                    .start()
            }
        }

        // Migrate dock items to desktop once
        migrateDockToDesktop()

        // Carregar aplicativos sob demanda (Dual-stage Loading)
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()

            // 1. Prepara e infla as páginas IMEDIATAMENTE (síncrono na Main)
            setupViewPager()

            // 2. Reaplica o tema básico (já chama refreshDesktopGrid internamente)
            applyTheme(ThemeManager.currentTheme.value)

            // Verifica LeakCanary em builds de debug
            try {
                val leakCanaryConfig = Class.forName("leakcanary.LeakCanary").getDeclaredField("config").get(null)
                android.util.Log.d("Alauncher", "LeakCanary está ativo e monitorando vazamentos.")
            } catch (e: Exception) {
                // LeakCanary não está presente (esperado em builds que não sejam debug)
            }

            // 3. Carrega a lista de pacotes salvos na home (operação rápida, sem IO real)
            loadHomeAppsPackages()
            loadDockAppsPackages()

            // 4. Atualiza grids com os pacotes carregados
            refreshDesktopGrid()
            setupDockAppsGrid()

            // 5. Inicia o carregamento de todos os aplicativos em segundo plano
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val fullList = AppLoader.loadInstalledApps(this@MainActivity)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    allAppsList = fullList
                    isAppsLoaded = true

                    populateDefaultDesktop()

                    // 5. Configura a dock e os resultados de busca com os dados completos
                    setupDockAppsGrid()
                    setupGlobalSearchGrid()
                    
                    // 6. Atualiza a biblioteca se ela já estiver inflada
                    libraryViewInstance?.let { view ->
                        if (isOrbitalDrawer()) setupOrbitalDrawer(view) else setupAppLibrary(view)
                    }
                    
                    // 7. Garante o tempo mínimo de Splash de 1.2 segundos
                    val elapsed = System.currentTimeMillis() - startTime
                    val remaining = 1200L - elapsed
                    if (remaining > 0) {
                        kotlinx.coroutines.delay(remaining)
                    }
                    
                    // 8. Marca como pronto para dismiss do splash
                    isReady = true
                }
            }
        }

        // Interceptar botão de voltar usando callback tradicional
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val folderOverlay = findViewById<View>(R.id.cl_folder_overlay)
                if (folderOverlay.visibility == View.VISIBLE) {
                    closeFolderOverlay()
                } else if (clGlobalSearchContainer.visibility == View.VISIBLE) {
                    closeGlobalSearch()
                } else if (desktopAdapter?.getEditMode() == true) {
                    disableEditMode()
                } else if (currentPage != 1) {
                    navigateToPage(1)
                } else if (!isDefaultLauncher()) {
                    // Se não for o launcher padrão, o botão voltar deve fechar o app
                    finish()
                }
            }
        })

        // Configurar gestos gerais na tela
        setupGlobalGestures()

        // Configurar busca global com debounce
        btnCloseGlobalSearch.setOnClickHaptic { closeGlobalSearch() }
        etGlobalSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchDebounceRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
                val query = s?.toString() ?: ""
                searchDebounceRunnable = Runnable { filterGlobalSearch(query) }
                searchDebounceHandler.postDelayed(searchDebounceRunnable!!, 200)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Observar mudanças do tema dinâmico
        lifecycleScope.launch {
            ThemeManager.currentTheme.collectLatest { theme ->
                applyTheme(theme)
            }
        }

        // Listener global para mudanças de MediaSession
        mediaSessionManager = try {
            getSystemService(Context.MEDIA_SESSION_SERVICE) as? android.media.session.MediaSessionManager
        } catch (e: Exception) { null }
        val listenerComponent = android.content.ComponentName(applicationContext, MediaNotificationListener::class.java)
        mediaSessionListener = android.media.session.MediaSessionManager.OnActiveSessionsChangedListener {
            reconnectMediaSession()
        }
        try {
            mediaSessionManager?.addOnActiveSessionsChangedListener(mediaSessionListener!!, listenerComponent)
        } catch (e: SecurityException) {
            mediaSessionListener = null
        }

        // Se for o primeiro lançamento, mostra a tela de boas-vindas
        if (prefs.getBoolean("is_first_launch", true)) {
            showWelcomeOverlay()
        }

        handlePinRequest(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePinRequest(intent)
        
        // Se receber um Home intent (pressionar botão home), reseta o estado da UI
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (clGlobalSearchContainer.visibility == View.VISIBLE) {
                closeGlobalSearch()
            }
            if (currentPage != 1) {
                navigateToPage(1)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (isOrbitalDrawer()) {
            try {
                val orbitView = libraryPage.findViewById<com.alisu.alauncher.ui.OrbitDrawerView>(R.id.orbit_drawer_view)
                orbitView?.applyTheme(ThemeManager.currentTheme.value)
            } catch (_: Exception) {}
        }
        
        val selectedPack = prefs.getString("icon_pack_package", "none") ?: "none"
        var iconPackChanged = false
        if (selectedPack != lastLoadedIconPack) {
            lastLoadedIconPack = selectedPack
            iconPackChanged = true
            AppIconFetcher.invalidateCache()
            try {
                coil.Coil.imageLoader(this).memoryCache?.clear()
                coil.Coil.imageLoader(this).diskCache?.clear()
            } catch (_: Exception) {}
            
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                com.alisu.alauncher.launcher.IconPackManager.loadIconPack(this@MainActivity, selectedPack)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    applyTheme(ThemeManager.currentTheme.value)
                    val folderOverlay = findViewById<View>(R.id.cl_folder_overlay)
                    if (folderOverlay.visibility == View.VISIBLE) {
                        val folderId = folderOverlay.tag as? String
                        if (folderId != null) {
                            showFolderDialog(folderId)
                        }
                    }
                }
            }
        }
        
        // Verifica se ainda somos o launcher padrão
        val isDefault = isDefaultLauncher()
        val wasDefault = prefs.getBoolean("was_default_launcher", false)
        
        if (!isDefault && wasDefault) {
            prefs.edit().putBoolean("was_default_launcher", false).apply()
        } else if (isDefault) {
            prefs.edit().putBoolean("was_default_launcher", true).apply()
        }

        if (::libraryPage.isInitialized) {
            val currentDrawerStyle = prefs.getString("drawer_style", "standard") ?: "standard"
            if (currentDrawerStyle != lastDrawerStyle) {
                lastDrawerStyle = currentDrawerStyle
                libraryPage.removeAllViews()
                libraryPage.background = null
                libraryPage.tag = null
                libraryViewInstance = null
                
                if (currentPage == 2) {
                    val inflater = LayoutInflater.from(this)
                    val layoutId = if (isOrbitalDrawer()) R.layout.page_orbital_drawer else R.layout.page_app_library
                    inflater.inflate(layoutId, libraryPage, true)
                    libraryViewInstance = libraryPage
                    
                    if (!isOrbitalDrawer()) {
                        val childRoot = libraryPage.getChildAt(0)
                        childRoot?.background = null

                        val r = dpToPx(28f)
                        libraryPage.background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            setColor(android.graphics.Color.parseColor("#CC0E0E12"))
                            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                        }
                    }
                    if (isOrbitalDrawer()) setupOrbitalDrawer(libraryPage) else setupAppLibrary(libraryPage)
                }
            }
        }

        lifecycleScope.launch {
            if (allAppsList.isEmpty()) {
                allAppsList = AppLoader.loadInstalledApps(this@MainActivity)
                isAppsLoaded = true
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                loadHomeAppsPackages()
                loadDockAppsPackages()
            }

            setupDockAppsGrid()
            refreshDesktopGrid()
            libraryAdapter?.updateData(allAppsList)

            rootLayout.postDelayed({ fetchWeatherData() }, 500)
        }

        // Re-tenta registrar listener de sessões ativas (se permissão foi concedida)
        if (mediaSessionListener == null && mediaSessionManager != null) {
            val listenerComponent = android.content.ComponentName(applicationContext, MediaNotificationListener::class.java)
            val newListener = android.media.session.MediaSessionManager.OnActiveSessionsChangedListener {
                reconnectMediaSession()
            }
            try {
                mediaSessionManager?.addOnActiveSessionsChangedListener(newListener, listenerComponent)
                mediaSessionListener = newListener
            } catch (_: SecurityException) {}
        }
        reconnectMediaSession()
    }
    override fun onPause() {
        super.onPause()
        try {
            appWidgetHost.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Re-infla páginas com layout específico da orientação (land/port)
        handleOrientationChange()
        applyTheme(ThemeManager.currentTheme.value)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            coil.Coil.imageLoader(this).memoryCache?.clear()
            com.alisu.alauncher.ui.MonochromaticTransformation.clearCache()
            com.alisu.alauncher.ui.ShapeClippingTransformation.clearCache()
            com.alisu.alauncher.theme.ThemeDrawableLoader.clearCache()
            com.alisu.alauncher.widget.WidgetPreviewLoader.clearCache()
            android.database.sqlite.SQLiteDatabase.releaseMemory()
        }
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL) {
            System.gc()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
        alertDialog?.dismiss()
        alertDialog = null

        // Parar progress tracking do MediaSession
        stopMediaProgressTracking()
        mediaProgressHandler = null

        // Remover callbacks pendentes
        searchDebounceRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
        searchDebounceRunnable = null

        // Remover callback do MediaController
        mediaCallback?.let { cb -> mediaActiveController?.unregisterCallback(cb) }
        mediaActiveController = null
        mediaCallback = null

        // Remover listener do MediaSession para evitar leak da Activity
        mediaSessionListener?.let { listener ->
            try {
                mediaSessionManager?.removeOnActiveSessionsChangedListener(listener)
            } catch (_: Exception) {}
            mediaSessionListener = null
        }
        mediaSessionManager = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (requestCode == REQUEST_PICK_APPWIDGET) {
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (resultCode == RESULT_OK && appWidgetId != -1) {
                val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                if (appWidgetInfo?.configure != null) {
                    pendingConfigWidgetId = appWidgetId
                    val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = appWidgetInfo.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    startActivityForResult(configureIntent, REQUEST_CREATE_APPWIDGET)
                } else {
                    addNewWidget(appWidgetId)
                }
            } else if (resultCode == RESULT_CANCELED && appWidgetId != -1) {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
            // Recupera o ID do extra OU do campo pendingConfigWidgetId (caso data seja null)
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                ?.takeIf { it != -1 }
                ?: pendingConfigWidgetId
            pendingConfigWidgetId = -1

            if (resultCode == RESULT_OK && appWidgetId != -1) {
                addNewWidget(appWidgetId)
            } else if (appWidgetId != -1) {
                // RESULT_CANCELED: alguns widgets (ex: Google SmartspaceWidget) retornam
                // CANCELED mesmo após configuração bem-sucedida. Verificamos se o bind
                // está ativo antes de deletar.
                val boundInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                if (boundInfo != null) {
                    android.util.Log.w("Widget", "onActivityResult CREATE CANCELED mas widget $appWidgetId está bound — adicionando mesmo assim.")
                    addNewWidget(appWidgetId)
                } else {
                    android.util.Log.i("Widget", "onActivityResult CREATE CANCELED widget $appWidgetId sem bind — deletando.")
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                }
            }
        } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
            // Recupera o ID do extra OU do campo pendingConfigWidgetId (caso data seja null)
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                ?.takeIf { it != -1 }
                ?: pendingConfigWidgetId
            pendingConfigWidgetId = -1

            if (resultCode == RESULT_OK && appWidgetId != -1) {
                addNewWidget(appWidgetId)
            } else if (appWidgetId != -1) {
                // RESULT_CANCELED: alguns widgets (ex: Google SmartspaceWidget) retornam
                // CANCELED mesmo após configuração bem-sucedida. Verificamos se o bind
                // está ativo antes de deletar.
                val boundInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                if (boundInfo != null) {
                    android.util.Log.w("Widget", "onActivityResult CREATE CANCELED mas widget $appWidgetId está bound — adicionando mesmo assim.")
                    addNewWidget(appWidgetId)
                } else {
                    android.util.Log.i("Widget", "onActivityResult CREATE CANCELED widget $appWidgetId sem bind — deletando.")
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                }
            }
        } else if (requestCode == REQUEST_BIND_APPWIDGET) {
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                ?.takeIf { it != -1 }
                ?: pendingBindWidgetId
            pendingBindWidgetId = -1
            android.util.Log.i("Widget", "onActivityResult: REQUEST_BIND_APPWIDGET retornado. resultCode = $resultCode, appWidgetId = $appWidgetId")
            if (resultCode == RESULT_OK && appWidgetId != -1) {
                val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                android.util.Log.i("Widget", "onActivityResult BIND: appWidgetInfo = $appWidgetInfo")
                if (appWidgetInfo?.configure != null) {
                    pendingConfigWidgetId = appWidgetId
                    val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = appWidgetInfo.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    android.util.Log.i("Widget", "onActivityResult BIND: Iniciando activity de configuracao ${appWidgetInfo.configure.className}")
                    startActivityForResult(configureIntent, REQUEST_CREATE_APPWIDGET)
                } else {
                    addNewWidget(appWidgetId)
                }
            } else if (resultCode == RESULT_CANCELED && appWidgetId != -1) {
                // Mesmo raciocínio: verificar bind antes de deletar
                val boundInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                android.util.Log.i("Widget", "onActivityResult BIND: BIND_CANCELED mas boundInfo = $boundInfo")
                if (boundInfo != null) {
                    android.util.Log.w("Widget", "onActivityResult BIND CANCELED mas widget $appWidgetId está bound — adicionando mesmo assim.")
                    addNewWidget(appWidgetId)
                } else {
                    android.util.Log.i("Widget", "onActivityResult BIND CANCELED widget $appWidgetId sem bind — deletando.")
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                }
            }
        } else if (requestCode == REQUEST_BIND_PIN_APPWIDGET) {
            val pinRequest = pendingPinRequest
            val appWidgetId = pendingWidgetId
            pendingPinRequest = null
            pendingWidgetId = -1
            if (resultCode == RESULT_OK && pinRequest != null && appWidgetId != -1) {
                val extras = Bundle().apply {
                    putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                pinRequest.accept(extras)
                addNewWidget(appWidgetId)
                Toast.makeText(this, getString(R.string.widget_added), Toast.LENGTH_SHORT).show()
            } else if (appWidgetId != -1) {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }
    }

    private fun handlePinRequest(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action != LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT && action != LauncherApps.ACTION_CONFIRM_PIN_APPWIDGET) {
            return
        }
        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return
        val pinItemRequest = launcherApps.getPinItemRequest(intent) ?: return
        if (!pinItemRequest.isValid) return

        val theme = ThemeManager.currentTheme.value
        val context = this
        val builder = AlertDialog.Builder(context)

        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
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
            textSize = 20f
            setTextColor(theme.colors.textPrimary)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(12))
        }

        val tvMessage = TextView(context).apply {
            textSize = 14f
            setTextColor(theme.colors.textSecondary)
            setPadding(0, 0, 0, dpToPx(24))
        }

        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val btnCancel = TextView(context).apply {
            text = getString(R.string.cancel)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(theme.colors.textSecondary)
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            isClickable = true
            isFocusable = true
        }

        val btnAdd = TextView(context).apply {
            text = getString(R.string.add)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(theme.colors.primary)
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            isClickable = true
            isFocusable = true
        }

        buttonsLayout.addView(btnCancel)
        buttonsLayout.addView(btnAdd)
        dialogLayout.addView(tvTitle)
        dialogLayout.addView(tvMessage)
        dialogLayout.addView(buttonsLayout)

        var dialog: AlertDialog? = null

        btnCancel.setOnClickHaptic {
            dialog?.dismiss()
        }

        if (pinItemRequest.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            val shortcutInfo = pinItemRequest.shortcutInfo ?: return
            val label = shortcutInfo.shortLabel ?: shortcutInfo.longLabel ?: "Shortcut"
            
            tvTitle.text = getString(R.string.add_shortcut)
            tvMessage.text = getString(R.string.add_shortcut_msg, label)
            
            btnAdd.setOnClickHaptic {
                dialog?.dismiss()
                pinItemRequest.accept()
                val appInfo = AppInfo(
                    label = label.toString(),
                    packageName = shortcutInfo.activity?.packageName ?: shortcutInfo.`package`
                )
                addAppShortcutToHome(appInfo)
            }
        } else if (pinItemRequest.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET) {
            val providerInfo = pinItemRequest.getAppWidgetProviderInfo(context) ?: return
            val label = providerInfo.loadLabel(packageManager)
            
            tvTitle.text = getString(R.string.add_widget)
            tvMessage.text = getString(R.string.add_widget_msg, label)
            
            btnAdd.setOnClickHaptic {
                dialog?.dismiss()
                val appWidgetId = appWidgetHost.allocateAppWidgetId()
                val success = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)
                if (success) {
                    val extras = Bundle().apply {
                        putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    pinItemRequest.accept(extras)
                    addNewWidget(appWidgetId)
                    Toast.makeText(context, getString(R.string.widget_added), Toast.LENGTH_SHORT).show()
                } else {
                    pendingPinRequest = pinItemRequest
                    pendingWidgetId = appWidgetId
                    val intentBind = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
                    }
                    startActivityForResult(intentBind, REQUEST_BIND_PIN_APPWIDGET)
                }
            }
        }

        builder.setView(dialogLayout)
        dialog = builder.create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun addNewWidget(appWidgetId: Int) {
        android.util.Log.i("Widget", "addNewWidget: Adicionando widget ID = $appWidgetId")
        val saved = prefs.getString("saved_widget_ids", "") ?: ""
        val list = saved.split(",").filter { it.isNotEmpty() }.toMutableList()
        list.add(appWidgetId.toString())
        prefs.edit().putString("saved_widget_ids", list.joinToString(",")).apply()
        
        createWidgetView(appWidgetId)
    }

    private fun createWidgetView(appWidgetId: Int) {
        android.util.Log.i("Widget", "createWidgetView: Iniciando renderização do widget ID = $appWidgetId")
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (appWidgetInfo == null) {
            android.util.Log.e("Widget", "createWidgetView: getAppWidgetInfo($appWidgetId) retornou null!")
            return
        }

        // Criar um ContextThemeWrapper baseado no applicationContext (que não possui o AppCompatViewInflater da Activity)
        // usando o tema do aplicativo, para evitar que os componentes do widget sejam inflados como subclasses AppCompat
        // (como AppCompatImageView), mantendo os atributos de tema do launcher necessários para o widget.
        val themedContext = android.view.ContextThemeWrapper(applicationContext, R.style.Theme_ComposeEmptyActivity)

        val size = com.alisu.alauncher.widget.WidgetSizeCalculator.calculateSize(this, appWidgetInfo)
        val screenPadding = dpToPx(32)
        // Bug 3 fix: widthPixels pode retornar 0 antes do layout; usa fallback de 360dp
        val rawWidth = resources.displayMetrics.widthPixels
        val safeScreenWidth = if (rawWidth > 0) rawWidth
                              else (resources.configuration.screenWidthDp * resources.displayMetrics.density).toInt().takeIf { it > 0 }
                              ?: (360 * resources.displayMetrics.density).toInt()
        val availableWidth = (safeScreenWidth - screenPadding).coerceAtLeast(dpToPx(100))
        val widgetWidth = size.widthPx.coerceAtMost(availableWidth).coerceAtLeast(dpToPx(80))
        val widgetHeight = size.heightPx.coerceAtMost(dpToPx(400)).coerceAtLeast(dpToPx(40))

        val cardLayout = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                val currentTheme = ThemeManager.currentTheme.value
                cornerRadius = dpToPx(currentTheme.shapes.cardCornerRadius.toFloat())
                setColor(adjustAlpha(currentTheme.colors.surface, 0.4f))
                if (currentTheme.shapes.showBorders) {
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(currentTheme.colors.primary, 0.15f))
                }
            }
            background = bg
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }

        val pendingView = com.alisu.alauncher.widget.PendingWidgetHostView(this).apply {
            showLoading()
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, widgetHeight)
        }
        cardLayout.addView(pendingView)

        com.alisu.alauncher.widget.WidgetPreviewLoader.loadPreview(this, appWidgetInfo, availableWidth, widgetHeight) { bitmap ->
            if (bitmap != null) {
                pendingView.showPreview(bitmap)
            }
        }

        // Bug 2 fix: o sistema Android pode ainda não ter processado o bind quando
        // createWidgetView é chamado imediatamente após onActivityResult.
        // Tentativa inicial: se falhar, agenda retry de 800ms antes de mostrar erro.
        val boundInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (boundInfo == null) {
            android.util.Log.w("Widget", "createWidgetView: widget $appWidgetId não está bound; tentando retry em 800ms.")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
                if (info != null) {
                    try {
                        val hostView = appWidgetHost.createView(themedContext, appWidgetId, info)
                        hostView.setAppWidget(appWidgetId, info)
                        hostView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, widgetHeight)
                        cardLayout.removeView(pendingView)
                        cardLayout.addView(hostView)
                    } catch (e: Exception) {
                        android.util.Log.e("Widget", "createWidgetView retry: erro no widget $appWidgetId", e)
                        pendingView.showPreview(null)
                    }
                } else {
                    android.util.Log.e("Widget", "createWidgetView retry: widget $appWidgetId ainda sem bind após retry.")
                    pendingView.showPreview(null)
                }
            }, 800L)
        } else {
            try {
                val hostView = appWidgetHost.createView(themedContext, appWidgetId, appWidgetInfo)
                hostView.setAppWidget(appWidgetId, appWidgetInfo)
                hostView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, widgetHeight)
                cardLayout.removeView(pendingView)
                cardLayout.addView(hostView)
            } catch (e: Exception) {
                android.util.Log.e("Widget", "createWidgetView: erro ao criar view do widget $appWidgetId — tentando retry em 800ms", e)
                // Retry após 800ms: o AppWidgetService pode estar processando o RemoteViews
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        val info2 = appWidgetManager.getAppWidgetInfo(appWidgetId)
                        if (info2 != null) {
                            val hostView2 = appWidgetHost.createView(themedContext, appWidgetId, info2)
                            hostView2.setAppWidget(appWidgetId, info2)
                            hostView2.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, widgetHeight)
                            cardLayout.removeView(pendingView)
                            cardLayout.addView(hostView2)
                        } else {
                            pendingView.showPreview(null)
                        }
                    } catch (e2: Exception) {
                        android.util.Log.e("Widget", "createWidgetView retry final: falha no widget $appWidgetId", e2)
                        pendingView.showPreview(null)
                    }
                }, 800L)
            }
        }

        cardLayout.setOnLongClickListener {
            showRemoveWidgetDialog(appWidgetId, cardLayout)
            true
        }

        val container = widgetsViewInstance?.findViewById<LinearLayout>(R.id.ll_android_widgets_container)
        if (container != null) {
            container.addView(cardLayout)
        } else {
            android.util.Log.w("Widget", "createWidgetView: widgetsViewInstance ou container é null — widget $appWidgetId não foi adicionado à UI")
        }
    }

    private fun showRemoveWidgetDialog(appWidgetId: Int, cardView: View) {
        val theme = ThemeManager.currentTheme.value
        val context = this
        
        // Root LinearLayout of the popup
        val popupView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            
            background = getPopupBackground(theme)
            
            elevation = dpToPx(8f)
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }
        
        val popupWindow = android.widget.PopupWindow(
            popupView,
            dpToPx(180),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // Focusable
        ).apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            elevation = dpToPx(8f)
        }
        
        // Helper to add menu items
        fun addMenuItem(title: String, iconRes: Int, onClick: () -> Unit) {
            val itemLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
                
                val ripple = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.TRANSPARENT)
                }
                background = ripple
                
                isClickable = true
                isFocusable = true
                
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.setBackgroundColor(adjustAlpha(theme.colors.primary, 0.15f))
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.setBackgroundColor(Color.TRANSPARENT)
                        }
                    }
                    false
                }
            }
            
            val ivIcon = ImageView(context).apply {
                setImageResource(iconRes)
                layoutParams = LinearLayout.LayoutParams(dpToPx(16), dpToPx(16)).apply {
                    setMargins(0, 0, dpToPx(12), 0)
                }
                setColorFilter(theme.colors.primary)
            }
            itemLayout.addView(ivIcon)
            
            val tvLabel = TextView(context).apply {
                text = title
                textSize = 13f
                setTextColor(theme.colors.textPrimary)
                typeface = android.graphics.Typeface.DEFAULT
            }
            itemLayout.addView(tvLabel)
            
            itemLayout.setOnClickHaptic {
                popupWindow.dismiss()
                onClick()
            }
            
            popupView.addView(itemLayout)
        }
        
        addMenuItem(getString(R.string.remove_widget), R.drawable.ic_close) {
            removeWidget(appWidgetId, cardView)
        }
        
        // Calculate and show the popup window safely on screen
        try {
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val popupWidth = popupView.measuredWidth
            val popupHeight = popupView.measuredHeight
            
            val location = IntArray(2)
            cardView.getLocationOnScreen(location)
            val viewX = location[0]
            val viewY = location[1]
            val viewWidth = cardView.width
            val viewHeight = cardView.height
            
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val margin = dpToPx(16)
            
            var x = viewX + (viewWidth - popupWidth) / 2
            x = x.coerceIn(margin, screenWidth - popupWidth - margin)
            
            val y = if (viewY - popupHeight - dpToPx(8) > margin) {
                viewY - popupHeight - dpToPx(4)
            } else {
                viewY + viewHeight + dpToPx(4)
            }
            
            popupWindow.showAtLocation(rootLayout, Gravity.NO_GRAVITY, x, y)
        } catch (e: Exception) {
            popupWindow.showAtLocation(rootLayout, Gravity.CENTER, 0, 0)
        }
    }

    private fun removeWidget(appWidgetId: Int, cardView: View) {
        val container = widgetsViewInstance?.findViewById<LinearLayout>(R.id.ll_android_widgets_container)
        container?.removeView(cardView)
        
        appWidgetHost.deleteAppWidgetId(appWidgetId)
        
        val saved = prefs.getString("saved_widget_ids", "") ?: ""
        val list = saved.split(",").filter { it.isNotEmpty() && it != appWidgetId.toString() }
        prefs.edit().putString("saved_widget_ids", list.joinToString(",")).apply()
        
        android.widget.Toast.makeText(this, getString(R.string.widget_removed), android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showWelcomeOverlay() {
        val theme = ThemeManager.currentTheme.value
        clWelcomeOverlay.visibility = View.VISIBLE
        clWelcomeOverlay.alpha = 0f
        clWelcomeOverlay.animate().alpha(1f).setDuration(300).start()
        
        applyWelcomeOverlayTheme(theme)
        
        btnWelcomeSetDefault.setOnClickHaptic {
            openHomeSettings()
        }
        
        btnWelcomeStart.setOnClickHaptic {
            prefs.edit().putBoolean("is_first_launch", false).apply()
            clWelcomeOverlay.animate().alpha(0f).translationY(-dpToPx(50f)).setDuration(300).withEndAction {
                clWelcomeOverlay.visibility = View.GONE
            }.start()
        }
    }

    private fun applyWelcomeOverlayTheme(theme: AppTheme) {
        if (clWelcomeOverlay.visibility != View.VISIBLE) return
        
        ivWelcomeLogo.setImageResource(R.drawable.ic_launcher_foreground)
        ivWelcomeLogo.colorFilter = null
        
        tvWelcomeTitle.setTextColor(theme.colors.textPrimary)
        
        val defaultBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
            setColor(adjustAlpha(theme.colors.primary, 0.15f))
            if (theme.shapes.showBorders) {
                setStroke(dpToPx(1.5f).toInt(), theme.colors.primary)
            }
        }
        btnWelcomeSetDefault.background = defaultBg
        tvWelcomeDefaultLabel.setTextColor(theme.colors.textPrimary)
        ivWelcomeDefaultIcon.setColorFilter(theme.colors.primary)
        
        val startBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
            setColor(theme.colors.primary)
        }
        btnWelcomeStart.background = startBg
        tvWelcomeStartLabel.setTextColor(Color.BLACK)
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val res = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        return res != null && packageName == res.activityInfo.packageName
    }

    private fun openHomeSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }

    internal fun showCustomWidgetPickerDialog() {
        val theme = ThemeManager.currentTheme.value
        val context = this
        val builder = AlertDialog.Builder(context)

        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(dpToPx(16), dpToPx(48), dpToPx(16), dpToPx(24))
            }
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                setColor(adjustAlpha(theme.colors.surface, 0.88f))
                if (theme.shapes.showBorders) {
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.3f))
                }
            }
        }

        val tvTitle = TextView(context).apply {
            text = getString(R.string.choose_widget)
            textSize = 22f
            setTextColor(theme.colors.textPrimary)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        dialogLayout.addView(tvTitle)

        val providers = appWidgetManager.installedProviders

        val tvSubtitle = TextView(context).apply {
            text = "${providers.size} widgets disponíveis"
            textSize = 12f
            setTextColor(adjustAlpha(theme.colors.textSecondary, 0.7f))
            setPadding(0, dpToPx(2), 0, dpToPx(16))
        }
        dialogLayout.addView(tvSubtitle)

        val scrollView = NestedScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                weight = 1f
            }
        }

        val listLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val pm = packageManager
        val groupedProviders = providers.groupBy { it.provider.packageName }
        val sortedGroups = groupedProviders.mapNotNull { (pkg, providerList) ->
            val appInfo = allAppsList.find { it.packageName == pkg }
            val appLabel = appInfo?.label ?: try {
                val app = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(app).toString()
            } catch (_: Exception) {
                pkg
            }
            Triple(pkg, appLabel, providerList)
        }.sortedBy { it.second.lowercase() }

        for ((pkg, appLabel, providerList) in sortedGroups) {
            val appHeader = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dpToPx(6), 0, dpToPx(6))
                }
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                    setColor(adjustAlpha(theme.colors.surface, 0.4f))
                    if (theme.shapes.showBorders) {
                        setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.15f))
                    }
                }
            }

            val ivIcon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
            }
            
            val iconShapeKey = prefs.getString("icon_shape", "theme") ?: "theme"
            val iconShapeMode = prefs.getString("icon_shape_mode", "force_launcher") ?: "force_launcher"
            val useMonochromatic = prefs.getBoolean("monochromatic_icons", false)
            val sizePx = dpToPx(36)
            val density = resources.displayMetrics.density
            val radiusPx = when (iconShapeKey) {
                "circle" -> sizePx / 2f
                "square" -> 0f
                "squircle" -> 18f * density * (36f / 60f)
                else -> theme.shapes.cardCornerRadius.toFloat() * density * (36f / 60f)
            }
            val shapeClip = if (iconShapeMode == "force_launcher") com.alisu.alauncher.ui.ShapeClippingTransformation(iconShapeKey, radiusPx) else null

            ivIcon.load("app-icon://$pkg") {
                placeholder(R.drawable.ic_settings)
                error(R.drawable.ic_settings)
                size(sizePx, sizePx)
                allowHardware(true)
                if (useMonochromatic) {
                    val transforms = mutableListOf<coil.transform.Transformation>(com.alisu.alauncher.ui.MonochromaticTransformation(theme.colors.primary, theme.colors.surface))
                    if (shapeClip != null) transforms.add(shapeClip)
                    transformations(transforms)
                } else if (shapeClip != null) {
                    transformations(shapeClip)
                }
            }

            val textContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = dpToPx(12)
                }
            }

            val tvName = TextView(context).apply {
                text = appLabel
                textSize = 15f
                setTextColor(theme.colors.textPrimary)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val tvWidgetCount = TextView(context).apply {
                text = "${providerList.size} ${if (providerList.size == 1) "widget" else "widgets"}"
                textSize = 11f
                setTextColor(adjustAlpha(theme.colors.textSecondary, 0.7f))
            }

            textContainer.addView(tvName)
            textContainer.addView(tvWidgetCount)

            val tvArrow = TextView(context).apply {
                text = "▶"
                textSize = 11f
                setTextColor(theme.colors.textSecondary)
                setPadding(dpToPx(8), 0, dpToPx(8), 0)
            }

            appHeader.addView(ivIcon)
            appHeader.addView(textContainer)
            appHeader.addView(tvArrow)

            val widgetsContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(0, 0, 0, 0)
            }

            for (provider in providerList) {
                val label = provider.loadLabel(pm)
                val itemView = LayoutInflater.from(context).inflate(R.layout.item_widget_picker, widgetsContainer, false)
                val ivWidgetIcon = itemView.findViewById<ImageView>(R.id.iv_widget_icon)
                val tvWidgetName = itemView.findViewById<TextView>(R.id.tv_widget_name)
                val tvSize = itemView.findViewById<TextView>(R.id.tv_widget_size)
                val flPreview = itemView.findViewById<FrameLayout>(R.id.fl_widget_preview)

                ivWidgetIcon.visibility = View.GONE

                tvWidgetName.text = label
                tvWidgetName.setTextColor(theme.colors.textPrimary)

                val minWidth = provider.minWidth
                val minHeight = provider.minHeight
                val (cellsX, cellsY) = com.alisu.alauncher.widget.WidgetSizeCalculator.calculateCellsForMinSize(minWidth, minHeight)
                val totalCells = cellsX * cellsY
                val sizeLabel = when {
                    totalCells <= 1 -> getString(R.string.widget_block_1)
                    else -> getString(R.string.widget_blocks, cellsX, cellsY)
                }
                tvSize.text = "$sizeLabel · ${getString(R.string.widget_cells, cellsX, cellsY)}"
                tvSize.setTextColor(theme.colors.textSecondary)

                val itemBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat() * 0.8f)
                    setColor(adjustAlpha(theme.colors.surface, 0.25f))
                    if (theme.shapes.showBorders) {
                        setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.1f))
                    }
                }
                itemView.background = itemBg

                // Indentation to show nesting hierarchy
                (itemView.layoutParams as? LinearLayout.LayoutParams)?.apply {
                    setMargins(dpToPx(16), dpToPx(4), 0, dpToPx(4))
                }

                // Soft preview background card
                val previewBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(4f)
                    setColor(adjustAlpha(theme.colors.textSecondary, 0.05f))
                }
                flPreview.background = previewBg
                flPreview.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))

                val previewWidth = dpToPx(200)
                val previewHeight = dpToPx(80)

                com.alisu.alauncher.widget.WidgetPreviewLoader.loadPreview(context, provider, previewWidth, previewHeight) { bitmap ->
                    flPreview.removeAllViews()
                    if (bitmap != null) {
                        flPreview.visibility = View.VISIBLE
                        val previewIv = ImageView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }
                        previewIv.setImageBitmap(bitmap)
                        flPreview.addView(previewIv)
                    } else {
                        flPreview.visibility = View.GONE
                    }
                }

                itemView.setOnClickHaptic {
                    alertDialog?.dismiss()
                    bindCustomWidget(provider)
                }

                widgetsContainer.addView(itemView)
            }

            appHeader.setOnClickHaptic {
                if (widgetsContainer.visibility == View.VISIBLE) {
                    widgetsContainer.visibility = View.GONE
                    tvArrow.text = "▶"
                } else {
                    widgetsContainer.visibility = View.VISIBLE
                    tvArrow.text = "▼"
                }
            }

            listLayout.addView(appHeader)
            listLayout.addView(widgetsContainer)
        }

        scrollView.addView(listLayout)
        dialogLayout.addView(scrollView)

        val btnClose = TextView(context).apply {
            text = getString(R.string.cancel)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(theme.colors.primary)
            gravity = Gravity.END
            setPadding(0, dpToPx(16), dpToPx(8), 0)
            isClickable = true
            isFocusable = true
            setOnClickHaptic {
                alertDialog?.dismiss()
            }
        }
        dialogLayout.addView(btnClose)

        builder.setView(dialogLayout)
        val dialog = builder.create()
        alertDialog = dialog
        val savedNavColor = window.navigationBarColor
        val savedStatusColor = window.statusBarColor
        window.navigationBarColor = theme.colors.background
        window.statusBarColor = theme.colors.background
        dialog.setOnDismissListener {
            if (alertDialog == dialog) {
                alertDialog = null
            }
            window.navigationBarColor = savedNavColor
            window.statusBarColor = savedStatusColor
            System.gc()
        }
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            navigationBarColor = theme.colors.background
            statusBarColor = theme.colors.background
        }
    }

    private fun bindCustomWidget(provider: AppWidgetProviderInfo) {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        android.util.Log.i("Widget", "bindCustomWidget: ID alocado = $appWidgetId. Provedor: ${provider.provider.className}")
        
        // Calculate default dimensions for options bundle
        val size = com.alisu.alauncher.widget.WidgetSizeCalculator.calculateSize(this, provider)
        val density = resources.displayMetrics.density
        val minWidthDp = (size.widthPx / density).toInt()
        val minHeightDp = (size.heightPx / density).toInt()
        
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeightDp)
        }

        val success = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider, options)
        android.util.Log.i("Widget", "bindCustomWidget: bindAppWidgetIdIfAllowed sucesso = $success")

        if (success) {
            launchWidgetConfigureOrAdd(appWidgetId, provider)
        } else {
            // Bug 1 fix: tenta ACTION_APPWIDGET_BIND em TODOS os níveis de API.
            // Antes, no Android 12+ a exception era capturada e o ID era deletado
            // imediatamente, sem dar chance ao usuário de conceder permissão.
            // Agora tratamos SecurityException e ActivityNotFoundException separadamente:
            //  - SecurityException → o sistema negou antes de mostrar a UI (raro); informa o usuário.
            //  - ActivityNotFoundException → não há activity para tratar o intent (improvável, mas possível em ROMs customizadas).
            //  - Qualquer outro erro inesperado → fallback genérico.
            // Em todos os casos de sucesso na abertura da Activity, o resultado
            // é tratado em onActivityResult(REQUEST_BIND_APPWIDGET).
            val bindIntent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)
            }
            try {
                pendingBindWidgetId = appWidgetId
                startActivityForResult(bindIntent, REQUEST_BIND_APPWIDGET)
                // Não deletamos o ID aqui — onActivityResult(RESULT_CANCELED) faz isso se o usuário negar.
            } catch (e: SecurityException) {
                android.util.Log.e("Widget", "bindCustomWidget: SecurityException ao solicitar bind do widget $appWidgetId", e)
                Toast.makeText(
                    this,
                    getString(R.string.widget_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            } catch (e: android.content.ActivityNotFoundException) {
                android.util.Log.e("Widget", "bindCustomWidget: ActivityNotFoundException ao solicitar bind do widget $appWidgetId", e)
                Toast.makeText(
                    this,
                    getString(R.string.widget_permission_unavailable),
                    Toast.LENGTH_LONG
                ).show()
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            } catch (e: Exception) {
                android.util.Log.e("Widget", "bindCustomWidget: erro inesperado ao solicitar bind do widget $appWidgetId", e)
                Toast.makeText(
                    this,
                    getString(R.string.widget_add_failed),
                    Toast.LENGTH_LONG
                ).show()
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }
    }

    private fun launchWidgetConfigureOrAdd(appWidgetId: Int, provider: AppWidgetProviderInfo) {
        if (provider.configure != null) {
            val configureIntent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = provider.configure
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            try {
                startActivityForResult(configureIntent, REQUEST_CREATE_APPWIDGET)
            } catch (e: Exception) {
                addNewWidget(appWidgetId)
            }
        } else {
            addNewWidget(appWidgetId)
        }
    }

    private fun adjustGridItems(oldList: List<String>, oldCols: Int, oldRows: Int, newCols: Int, newRows: Int): List<String> {
        val newSize = newCols * newRows
        val newList = MutableList(newSize) { "" }
        val usedRows = minOf(oldRows, newRows)
        val usedCols = minOf(oldCols, newCols)
        for (r in 0 until usedRows) {
            for (c in 0 until usedCols) {
                val oldIdx = r * oldCols + c
                val newIdx = r * newCols + c
                if (oldIdx < oldList.size && newIdx < newList.size) {
                    newList[newIdx] = oldList[oldIdx]
                }
            }
        }
        return newList
    }

    private fun migrateDockToDesktop() {
        val hasMigrated = prefs.getBoolean("dock_migrated_to_desktop_v3", false)
        if (!hasMigrated) {
            val savedDock = prefs.getString("dock_apps_packages", null)
            val savedHome = prefs.getString("home_apps_packages", null)
            
            val columns = 4
            val rows = 5
            val targetSize = columns * rows
            
            val homeList = if (!savedHome.isNullOrEmpty()) {
                savedHome.split(",").toMutableList()
            } else {
                MutableList(targetSize) { "" }
            }
            while (homeList.size < targetSize) homeList.add("")
            while (homeList.size > targetSize) homeList.removeAt(homeList.size - 1)

            val dockApps = if (!savedDock.isNullOrEmpty()) {
                savedDock.split(",").toMutableList()
            } else {
                val defaultApps = getDockApps()
                defaultApps.map { it.packageName }.toMutableList()
            }
            while (dockApps.size < 4) dockApps.add("")
            
            val lastRowStart = (rows - 1) * columns
            
            for (i in 0 until columns) {
                val targetIdx = lastRowStart + i
                if (targetIdx < targetSize) {
                    val dockPkg = dockApps[i]
                    if (dockPkg.isNotEmpty()) {
                        val existingItem = homeList[targetIdx]
                        if (existingItem.isNotEmpty() && existingItem != dockPkg) {
                            val emptyIdx = homeList.indexOf("")
                            if (emptyIdx != -1 && emptyIdx < lastRowStart) {
                                homeList[emptyIdx] = existingItem
                            }
                        }
                        homeList[targetIdx] = dockPkg
                    }
                }
            }
            
            prefs.edit()
                .putString("home_apps_packages", homeList.joinToString(","))
                .putBoolean("dock_migrated_to_desktop", true)
                .putBoolean("dock_migrated_to_desktop_v2", true)
                .putBoolean("dock_migrated_to_desktop_v3", true)
                .apply()
        }
    }

    private fun loadHomeAppsPackages() {
        val theme = ThemeManager.currentTheme.value
        val columns = getColumns(theme)
        val rows = try { resources.getInteger(R.integer.desktop_grid_rows) } catch (_: Exception) { 5 }
        val targetSize = columns * rows

        val saved = prefs.getString("home_apps_packages", null)
        val loadedList = if (!saved.isNullOrEmpty()) {
            saved.split(",").toMutableList()
        } else {
            mutableListOf()
        }

        if (loadedList.size != targetSize) {
            val lastCols = prefs.getInt("last_grid_columns", -1)
            val lastRows = prefs.getInt("last_grid_rows", -1)
            val oldSize = if (lastCols > 0 && lastRows > 0) lastCols * lastRows else -1
            val migratedList = if (lastCols != -1 && lastCols != columns && lastRows != -1 && loadedList.size == oldSize) {
                adjustGridItems(loadedList, lastCols, lastRows, columns, rows)
            } else {
                val items = loadedList.filter { it.isNotEmpty() }
                val newList = MutableList(targetSize) { "" }
                items.forEachIndexed { index, pkg ->
                    if (index < targetSize) newList[index] = pkg
                }
                newList
            }
            homeAppsPackages = migratedList.toMutableList()
            saveHomeAppsList(homeAppsPackages)
        } else {
            homeAppsPackages = loadedList.toMutableList()
        }
        prefs.edit()
            .putInt("last_grid_columns", columns)
            .putInt("last_grid_rows", rows)
            .apply()
    }

    private fun saveHomeAppsList(list: List<String>) {
        val listString = list.joinToString(",")
        prefs.edit().putString("home_apps_packages", listString).apply()
    }

    private fun populateDefaultDesktop() {
        val theme = ThemeManager.currentTheme.value
        val columns = getColumns(theme)
        val rows = try { resources.getInteger(R.integer.desktop_grid_rows) } catch (_: Exception) { 5 }
        val targetSize = columns * rows
        if (homeAppsPackages.size != targetSize) return
        if (homeAppsPackages.any { it.isNotEmpty() }) return

        val folderSocialId = "folder_social_default"
        val folderGamesId = "folder_games_default"

        val socialApps = getAppsByCategory(getString(R.string.cat_social))
        val gamesApps = getAppsByCategory(getString(R.string.cat_games))

        if (socialApps.isNotEmpty()) {
            prefs.edit()
                .putString("folder_name_$folderSocialId", getString(R.string.cat_social))
                .putString("folder_apps_$folderSocialId", socialApps.joinToString(",") { it.packageName })
                .putInt("folder_size_$folderSocialId", 1)
                .apply()
        }
        if (gamesApps.isNotEmpty()) {
            prefs.edit()
                .putString("folder_name_$folderGamesId", getString(R.string.cat_games))
                .putString("folder_apps_$folderGamesId", gamesApps.joinToString(",") { it.packageName })
                .putInt("folder_size_$folderGamesId", 1)
                .apply()
        }

        val phoneApp = allAppsList.find { app ->
            app.packageName == "com.google.android.dialer" || app.packageName == "com.xiaomi.phone"
        } ?: allAppsList.find { app ->
            val pkg = app.packageName.lowercase()
            (pkg.contains("dialer") || pkg.contains("phone")) &&
                !pkg.contains("remotecontroller") && !pkg.contains("overlay") &&
                !pkg.contains("auto_generated") && !pkg.contains("carriers")
        }
        val messagesApp = allAppsList.find { app ->
            val pkg = app.packageName.lowercase()
            pkg.contains("mms") || pkg.contains("messaging") || pkg.contains("sms") ||
                pkg.contains("messages")
        }
        val contactsApp = allAppsList.find { app ->
            val pkg = app.packageName.lowercase()
            (pkg.contains("contacts") || pkg.contains("contatos")) &&
                app.packageName != phoneApp?.packageName
        }
        val cameraApp = allAppsList.find { app ->
            val pkg = app.packageName.lowercase()
            pkg.contains("camera") || pkg.contains("miui.camera")
        }

        val penultimateRowStart = (rows - 2) * columns
        val lastRowStart = (rows - 1) * columns

        if (socialApps.isNotEmpty()) homeAppsPackages[penultimateRowStart] = folderSocialId
        if (gamesApps.isNotEmpty()) homeAppsPackages[penultimateRowStart + 1] = folderGamesId

        if (phoneApp != null) homeAppsPackages[lastRowStart] = phoneApp.packageName
        if (messagesApp != null) homeAppsPackages[lastRowStart + 1] = messagesApp.packageName
        if (contactsApp != null && contactsApp.packageName != phoneApp?.packageName) {
            homeAppsPackages[lastRowStart + 2] = contactsApp.packageName
        }
        if (cameraApp != null) homeAppsPackages[lastRowStart + 3] = cameraApp.packageName

        saveHomeAppsList(homeAppsPackages)
        refreshDesktopGrid()
    }

    private fun saveDockAppsList(list: List<String>) {
        prefs.edit().putString("dock_apps_packages", list.joinToString(",")).apply()
    }

    private fun loadDockAppsPackages() {
        dockAppsPackages = mutableListOf()
    }

    private fun enableEditMode() {
        if (desktopAdapter?.getEditMode() == true) {
            Log.d("EditMode", "enableEditMode: already active")
            return
        }
        Log.d("EditMode", "enableEditMode: enabling")
        desktopAdapter?.setEditMode(true)

        rootLayout.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

        val llConfirm = findViewById<LinearLayout>(R.id.ll_edit_confirm)
        llConfirm.visibility = View.VISIBLE
        llConfirm.alpha = 0f
        llConfirm.translationY = 40f
        llConfirm.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    private fun disableEditMode() {
        if (desktopAdapter?.getEditMode() != true) {
            Log.d("EditMode", "disableEditMode: already inactive")
            return
        }
        Log.d("EditMode", "disableEditMode: disabling")
        desktopAdapter?.setEditMode(false)

        gestureController.reset()

        val llConfirm = findViewById<LinearLayout>(R.id.ll_edit_confirm)
        llConfirm.animate()
            .alpha(0f)
            .translationY(20f)
            .setDuration(200)
            .withEndAction {
                llConfirm.visibility = View.GONE
                llConfirm.translationY = 0f
            }
            .start()
    }

    private fun getDesktopAppsList(appsList: List<AppInfo>, theme: AppTheme): List<AppInfo> {
        val desktopApps = MutableList<AppInfo>(homeAppsPackages.size) { AppInfo("", "") }
        for (i in homeAppsPackages.indices) {
            val pkg = homeAppsPackages[i]
            if (pkg.isNotEmpty()) {
                if (pkg.startsWith("folder_")) {
                    val folderName = prefs.getString("folder_name_$pkg", getString(R.string.folder)) ?: getString(R.string.folder)
                    desktopApps[i] = AppInfo(label = folderName, packageName = pkg)
                } else if (pkg.startsWith("placeholder_")) {
                    desktopApps[i] = AppInfo(label = "", packageName = pkg)
                } else {
                    // É um app normal, busca na lista carregada
                    val app = appsList.find { it.packageName == pkg }
                    if (app != null) {
                        desktopApps[i] = app
                    } else {
                        // App não encontrado (desinstalado?), limpamos o slot
                        homeAppsPackages[i] = ""
                    }
                }
            }
        }
        return desktopApps
    }

    private var lastLoadedIconPack: String? = null
    internal var currentPage = 1
    internal lateinit var container: FrameLayout
    internal lateinit var desktopPage: View
    internal lateinit var widgetsPage: View
    internal lateinit var libraryPage: FrameLayout

    private fun isOrbitalDrawer(): Boolean =
        prefs.getString("drawer_style", "standard") == "orbital"

    private fun inflateLibraryPage(inflater: LayoutInflater): View {
        return if (isOrbitalDrawer()) {
            inflater.inflate(R.layout.page_orbital_drawer, container, false)
        } else {
            inflater.inflate(R.layout.page_app_library, container, false)
        }
    }

    private fun setupViewPager() {
        container = findViewById(R.id.pages_container)

        val inflater = LayoutInflater.from(this)
        widgetsPage = inflater.inflate(R.layout.page_widgets, container, false)
        desktopPage = inflater.inflate(R.layout.page_home_desktop, container, false)
        libraryPage = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        container.addView(widgetsPage)
        container.addView(desktopPage)
        container.addView(libraryPage)

        widgetsPage.visibility = View.GONE
        desktopPage.visibility = View.VISIBLE
        libraryPage.visibility = View.GONE

        widgetsViewInstance = widgetsPage
        setupWidgetsPage(widgetsPage)

        desktopViewInstance = desktopPage
        setupHomeDesktop(desktopPage)

        libraryViewInstance = null

        currentPage = 1
    }

    private fun handleOrientationChange() {
        if (!::container.isInitialized) return
        if (container.childCount > 0) container.removeAllViews()

        val savedPage = currentPage
        val inflater = LayoutInflater.from(this)

        widgetsPage = inflater.inflate(R.layout.page_widgets, container, false)
        desktopPage = inflater.inflate(R.layout.page_home_desktop, container, false)
        libraryPage = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        container.addView(widgetsPage)
        container.addView(desktopPage)
        container.addView(libraryPage)

        widgetsViewInstance = widgetsPage
        setupWidgetsPage(widgetsPage)

        desktopViewInstance = desktopPage
        setupHomeDesktop(desktopPage)

        libraryViewInstance = null
        if (savedPage == 2) {
            val layoutId = if (isOrbitalDrawer()) R.layout.page_orbital_drawer else R.layout.page_app_library
            inflater.inflate(layoutId, libraryPage, true)
            libraryViewInstance = libraryPage
            if (!isOrbitalDrawer()) {
                val childRoot = libraryPage.getChildAt(0)
                childRoot?.background = null

                val libraryRadius = dpToPx(28f)
                val libraryBg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    setColor(android.graphics.Color.parseColor("#CC0E0E12"))
                    cornerRadii = floatArrayOf(libraryRadius, libraryRadius, libraryRadius, libraryRadius, 0f, 0f, 0f, 0f)
                }
                libraryPage.background = libraryBg
            }
            if (isOrbitalDrawer()) setupOrbitalDrawer(libraryPage) else setupAppLibrary(libraryPage)
        }

        // Garante que apenas a página atual fique visível, sem sobreposição
        widgetsPage.visibility = if (savedPage == 0) View.VISIBLE else View.GONE
        desktopPage.visibility = if (savedPage == 1) View.VISIBLE else View.GONE
        libraryPage.visibility = if (savedPage == 2) View.VISIBLE else View.GONE

        widgetsPage.translationX = 0f
        desktopPage.translationX = 0f
        desktopPage.alpha = 1f
        libraryPage.translationY = 0f

        currentPage = savedPage
    }

    private val springXAnimations = java.util.WeakHashMap<View, SpringAnimation>()
    private val springYAnimations = java.util.WeakHashMap<View, SpringAnimation>()

    internal fun View.springAnimateX(
        targetX: Float,
        stiffness: Float = 250f,
        dampingRatio: Float = 0.75f,
        endAction: (() -> Unit)? = null
    ) {
        springXAnimations[this]?.cancel()
        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val anim = SpringAnimation(this, DynamicAnimation.TRANSLATION_X, targetX).apply {
            spring = SpringForce().apply {
                this.stiffness = stiffness
                this.dampingRatio = dampingRatio
                this.finalPosition = targetX
            }
            addEndListener { _, _, _, _ ->
                setLayerType(View.LAYER_TYPE_NONE, null)
                endAction?.invoke()
            }
        }
        springXAnimations[this] = anim
        anim.start()
    }

    internal fun View.springAnimateY(
        targetY: Float,
        stiffness: Float = 250f,
        dampingRatio: Float = 0.75f,
        endAction: (() -> Unit)? = null
    ) {
        springYAnimations[this]?.cancel()
        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val anim = SpringAnimation(this, DynamicAnimation.TRANSLATION_Y, targetY).apply {
            spring = SpringForce().apply {
                this.stiffness = stiffness
                this.dampingRatio = dampingRatio
                this.finalPosition = targetY
            }
            addEndListener { _, _, _, _ ->
                setLayerType(View.LAYER_TYPE_NONE, null)
                endAction?.invoke()
            }
        }
        springYAnimations[this] = anim
        anim.start()
    }

    internal fun navigateToPage(page: Int) {
        if (page == currentPage) return
        if (currentPage == 2 && !isOrbitalDrawer()) {
            libraryPage.findViewById<EditText>(R.id.et_library_search)?.setText("")
        }

        widgetsPage.isClickable = page == 0
        widgetsPage.isEnabled = page == 0
        desktopPage.isClickable = page == 1
        desktopPage.isEnabled = page == 1
        libraryPage.isClickable = page == 2
        libraryPage.isEnabled = page == 2

        when (page) {
            0 -> {
                if (currentPage == 2) {
                    libraryPage.springAnimateY(
                        targetY = -container.height.toFloat(),
                        stiffness = SpringForce.STIFFNESS_MEDIUM,
                        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                    ) {
                        libraryPage.visibility = View.GONE
                    }
                }
                widgetsPage.bringToFront()
                widgetsPage.translationX = -container.width.toFloat()
                widgetsPage.visibility = View.VISIBLE
                widgetsPage.springAnimateX(
                    targetX = 0f,
                    stiffness = 180f,
                    dampingRatio = 0.72f
                ) {
                    disableEditMode()
                }
                desktopPage.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withLayer()
                    .start()
            }
            1 -> {
                val fromDrawer = currentPage == 2
                if (fromDrawer) {
                    libraryPage.springAnimateY(
                        targetY = container.height.toFloat(),
                        stiffness = SpringForce.STIFFNESS_MEDIUM,
                        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                    ) {
                        libraryPage.visibility = View.GONE
                        libraryPage.translationY = 0f
                    }
                } else if (currentPage == 0) {
                    widgetsPage.springAnimateX(
                        targetX = -container.width.toFloat(),
                        stiffness = SpringForce.STIFFNESS_MEDIUM,
                        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                    ) {
                        widgetsPage.visibility = View.GONE
                    }
                }
                desktopPage.bringToFront()
                desktopPage.springAnimateX(
                    targetX = 0f,
                    stiffness = 180f,
                    dampingRatio = 0.72f
                )
                desktopPage.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .withLayer()
                    .start()
            }
            2 -> {
                if (libraryPage.childCount == 0) {
                    val inflater = LayoutInflater.from(this)
                    val layoutId = if (isOrbitalDrawer()) R.layout.page_orbital_drawer else R.layout.page_app_library
                    inflater.inflate(layoutId, libraryPage, true)
                    libraryViewInstance = libraryPage
                    
                    if (!isOrbitalDrawer()) {
                        val childRoot = libraryPage.getChildAt(0)
                        childRoot?.background = null

                        val libraryRadius = dpToPx(28f)
                        val libraryBg = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            setColor(android.graphics.Color.parseColor("#CC0E0E12"))
                            cornerRadii = floatArrayOf(libraryRadius, libraryRadius, libraryRadius, libraryRadius, 0f, 0f, 0f, 0f)
                        }
                        libraryPage.background = libraryBg
                    } else {
                        libraryPage.background = null
                    }
                    applyLibraryInsets()
                }

                if (libraryPage.tag != "initialized") {
                    if (isOrbitalDrawer()) setupOrbitalDrawer(libraryPage) else setupAppLibrary(libraryPage)
                }
                widgetsPage.springAnimateX(
                    targetX = -container.width.toFloat(),
                    stiffness = SpringForce.STIFFNESS_MEDIUM,
                    dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                ) {
                    widgetsPage.visibility = View.GONE
                }
                libraryPage.bringToFront()
                libraryPage.translationY = container.height.toFloat()
                libraryPage.visibility = View.VISIBLE
                desktopPage.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withLayer()
                    .start()
                libraryPage.springAnimateY(
                    targetY = 0f,
                    stiffness = 135f,
                    dampingRatio = 0.65f
                ) {
                    disableEditMode()
                }
            }
        }
        currentPage = page
    }

    private fun setupPageIndicator() {
        llPageIndicator.visibility = View.GONE
    }

    private fun updatePageIndicator(position: Int) {
        // Indicadores desativados
    }

    // Retorna dinamicamente os 4 aplicativos da Dock (Ligação, Mensagem, Câmera, Navegador)
    private fun getDockApps(): List<AppInfo> {
        val dockApps = mutableListOf<AppInfo>()
        
        // 1. Telefone/Ligação
        val phoneApp = allAppsList.find { app ->
            val pkg = app.packageName.lowercase()
            pkg.contains("dialer") || pkg.contains("phone") || pkg.contains("telephony") || pkg.contains("contacts")
        } ?: allAppsList.firstOrNull()
        phoneApp?.let { dockApps.add(it) }

        // 2. Mensagens
        val msgApp = allAppsList.find { app ->
            val pkg = app.packageName.lowercase()
            pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("sms") || pkg.contains("message")
        } ?: allAppsList.getOrNull(1)
        msgApp?.let { dockApps.add(it) }

        // 3. Câmera
        val cameraApp = allAppsList.find { app ->
            val pkg = app.packageName.lowercase()
            pkg.contains("camera")
        } ?: allAppsList.getOrNull(2)
        cameraApp?.let { dockApps.add(it) }

        // 4. Navegador
        val browserApp = allAppsList.find { app ->
            val pkg = app.packageName.lowercase()
            pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("internet") || pkg.contains("firefox")
        } ?: allAppsList.getOrNull(3)
        browserApp?.let { dockApps.add(it) }

        // Fallback caso algum não seja encontrado para garantir 4 ícones
        while (dockApps.size < 4 && allAppsList.isNotEmpty()) {
            val fallback = allAppsList.firstOrNull { !dockApps.contains(it) }
            if (fallback != null) dockApps.add(fallback) else break
        }
        
        return dockApps.take(4)
    }

    private fun setupDockAppsGrid() {
        val currentTheme = ThemeManager.currentTheme.value

        // Se as referências às views não existem, não há o que atualizar agora
        if (rvDockApps == null) return

        if (dockAppsPackages.isEmpty() && allAppsList.isNotEmpty()) {
            val defaultApps = getDockApps()
            dockAppsPackages = defaultApps.map { it.packageName }.toMutableList()
            saveDockAppsList(dockAppsPackages)
        }

        val dockApps = mutableListOf<AppInfo>()
        for (pkg in dockAppsPackages) {
            val app = allAppsList.find { it.packageName == pkg }
            if (app != null) {
                dockApps.add(app)
            } else {
                dockApps.add(AppInfo("", ""))
            }
        }

        val dockTheme = currentTheme.copy(
            layout = currentTheme.layout.copy(showAppName = false)
        )

        while (dockApps.size < 4) {
            dockApps.add(AppInfo("", ""))
        }

        if (dockAdapter == null) {
            dockAdapter = AppAdapter(
                apps = dockApps,
                theme = dockTheme,
                onDeleteClick = null,
                onAppLongClick = null,
                onAppClick = { app ->
                    if (app.packageName.isNotEmpty()) {
                        AppLoader.launchApp(this, app.packageName)
                    }
                },
                gestureController = null
            )
        } else {
            dockAdapter?.updateTheme(dockTheme)
            dockAdapter?.updateData(dockApps)
        }
        rvDockApps?.adapter = dockAdapter

        val lm = rvDockApps?.layoutManager as? GridLayoutManager
        if (lm == null || lm.spanCount != dockApps.size) {
            rvDockApps?.layoutManager = GridLayoutManager(this, dockApps.size)
        }

        // Garante que a Dock não seja um DropTarget e não receba drops
        val dockLayout = rvDockApps as? com.alisu.alauncher.dock.DockLayout
        if (dockLayout != null) {
            dragController.dropTargets.remove(dockLayout)
            dockLayout.onDropListener = null
        }
    }

    private fun handleDropOnWorkspace(item: AppInfo, targetIdx: Int) {
        val sourceView = dragController.sourceView
        if (sourceView == null) return

        if (targetIdx < 0 || targetIdx >= homeAppsPackages.size) return
        val targetPkg = homeAppsPackages[targetIdx]
        if (targetPkg.startsWith("placeholder_")) {
            Toast.makeText(this, getString(R.string.cannot_drop_folder), Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Se veio do próprio WorkspaceLayout (reordenação interna)
        if (sourceView.parent is com.alisu.alauncher.workspace.WorkspaceLayout) {
            val sourceRv = sourceView.parent as com.alisu.alauncher.workspace.WorkspaceLayout
            val sourceIdx = sourceRv.getChildAdapterPosition(sourceView)
            if (sourceIdx != RecyclerView.NO_POSITION && sourceIdx != targetIdx) {
                // Lógica de "Smart Shift" estilo Lawnchair: 
                // Se o alvo está ocupado, empurra os itens até o primeiro vazio.
                if (homeAppsPackages[targetIdx].isNotEmpty()) {
                    val emptyIdx = findNearestEmptyIdx(targetIdx)
                    if (emptyIdx != -1) {
                        shiftItems(targetIdx, emptyIdx)
                    } else {
                        // Se não há vazios, faz swap simples (comportamento de segurança)
                        val temp = homeAppsPackages[sourceIdx]
                        homeAppsPackages[sourceIdx] = homeAppsPackages[targetIdx]
                        homeAppsPackages[targetIdx] = temp
                    }
                } else {
                    // Alvo vazio: apenas move
                    homeAppsPackages[targetIdx] = homeAppsPackages[sourceIdx]
                    homeAppsPackages[sourceIdx] = ""
                }
                saveHomeAppsList(homeAppsPackages)
                refreshDesktopGrid()
            }
        }
        // 2. Se veio do DockLayout (move da Dock para o Workspace)
        else if (sourceView.parent is com.alisu.alauncher.dock.DockLayout) {
            val sourceRv = sourceView.parent as com.alisu.alauncher.dock.DockLayout
            val sourceIdx = sourceRv.getChildAdapterPosition(sourceView)
            if (sourceIdx != RecyclerView.NO_POSITION) {
                val dockPkg = dockAppsPackages[sourceIdx]
                if (homeAppsPackages[targetIdx].isNotEmpty()) {
                    val emptyIdx = findNearestEmptyIdx(targetIdx)
                    if (emptyIdx != -1) {
                        shiftItems(targetIdx, emptyIdx)
                        homeAppsPackages[targetIdx] = dockPkg
                        dockAppsPackages[sourceIdx] = ""
                    } else {
                        // Swap se estiver cheio
                        val temp = homeAppsPackages[targetIdx]
                        homeAppsPackages[targetIdx] = dockPkg
                        dockAppsPackages[sourceIdx] = temp
                    }
                } else {
                    homeAppsPackages[targetIdx] = dockPkg
                    dockAppsPackages[sourceIdx] = ""
                }

                saveHomeAppsList(homeAppsPackages)
                saveDockAppsList(dockAppsPackages)
                refreshDesktopGrid()
                setupDockAppsGrid()
            }
        }
        // 3. Se veio da Biblioteca (Drawer) ou Search
        else {
            if (homeAppsPackages[targetIdx].isNotEmpty()) {
                val emptyIdx = findNearestEmptyIdx(targetIdx)
                if (emptyIdx != -1) {
                    shiftItems(targetIdx, emptyIdx)
                    homeAppsPackages[targetIdx] = item.packageName
                } else {
                    // Sem espaço: substitui o item atual e move o antigo para o primeiro vazio global
                    val oldPkg = homeAppsPackages[targetIdx]
                    homeAppsPackages[targetIdx] = item.packageName
                    val firstEmpty = homeAppsPackages.indexOfFirst { it.isEmpty() }
                    if (firstEmpty != -1) {
                        homeAppsPackages[firstEmpty] = oldPkg
                    }
                }
            } else {
                homeAppsPackages[targetIdx] = item.packageName
            }
            saveHomeAppsList(homeAppsPackages)
            refreshDesktopGrid()
        }
    }

    private fun findNearestEmptyIdx(targetIdx: Int): Int {
        var distance = 1
        while (distance < homeAppsPackages.size) {
            val right = targetIdx + distance
            val left = targetIdx - distance
            if (right < homeAppsPackages.size && homeAppsPackages[right].isEmpty()) return right
            if (left >= 0 && homeAppsPackages[left].isEmpty()) return left
            distance++
        }
        return -1
    }

    private fun shiftItems(targetIdx: Int, emptyIdx: Int) {
        if (targetIdx < emptyIdx) {
            // Empurra para a direita
            for (i in emptyIdx downTo targetIdx + 1) {
                homeAppsPackages[i] = homeAppsPackages[i - 1]
            }
        } else {
            // Empurra para a esquerda
            for (i in emptyIdx until targetIdx) {
                homeAppsPackages[i] = homeAppsPackages[i + 1]
            }
        }
        homeAppsPackages[targetIdx] = ""
    }

    private fun handleDropOnDock(item: AppInfo, targetIdx: Int) {
        Log.d("DragDrop", "handleDropOnDock: item=${item.packageName} target=$targetIdx")
        val sourceView = dragController.sourceView
        if (sourceView == null) return

        // Protege o trigger da gaveta de ser substituído ou movido
        val targetPkg = dockAppsPackages.getOrNull(targetIdx) ?: ""
        if (targetPkg == "com.alisu.alauncher.drawer_trigger" || item.packageName == "com.alisu.alauncher.drawer_trigger") {
            return
        }

        // 1. Se veio da própria Dock (reordenação na Dock)
        if (sourceView.parent is com.alisu.alauncher.dock.DockLayout) {
            val sourceRv = sourceView.parent as com.alisu.alauncher.dock.DockLayout
            val sourceIdx = sourceRv.getChildAdapterPosition(sourceView)
            if (sourceIdx != RecyclerView.NO_POSITION && sourceIdx != targetIdx) {
                val temp = dockAppsPackages[sourceIdx]
                dockAppsPackages[sourceIdx] = dockAppsPackages[targetIdx]
                dockAppsPackages[targetIdx] = temp
                saveDockAppsList(dockAppsPackages)
                setupDockAppsGrid()
            }
        }
        // 2. Se veio do WorkspaceLayout (move do Workspace para a Dock)
        else if (sourceView.parent is com.alisu.alauncher.workspace.WorkspaceLayout) {
            val sourceRv = sourceView.parent as com.alisu.alauncher.workspace.WorkspaceLayout
            val sourceIdx = sourceRv.getChildAdapterPosition(sourceView)
            if (sourceIdx != RecyclerView.NO_POSITION) {
                val workspacePkg = homeAppsPackages[sourceIdx]
                val dockPkg = dockAppsPackages[targetIdx]

                dockAppsPackages[targetIdx] = workspacePkg
                homeAppsPackages[sourceIdx] = dockPkg

                saveHomeAppsList(homeAppsPackages)
                saveDockAppsList(dockAppsPackages)
                refreshDesktopGrid()
                setupDockAppsGrid()
            }
        }
        // 3. Se veio da Biblioteca (Drawer)
        else {
            val targetPkg = dockAppsPackages[targetIdx]
            if (targetPkg.isNotEmpty()) {
                android.widget.Toast.makeText(this, getString(R.string.remove_dock_first), android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            dockAppsPackages[targetIdx] = item.packageName
            saveDockAppsList(dockAppsPackages)
            setupDockAppsGrid()
        }
    }

    private fun setupGlobalSearchGrid() {
        val currentTheme = ThemeManager.currentTheme.value
        globalSearchAdapter = AppAdapter(
            apps = allAppsList,
            theme = currentTheme,
            onDeleteClick = null,
            onAppLongClick = { app, holder ->
                showAppContextMenu(app, "search", holder.itemView)
                true
            },
            onAppClick = { app ->
                AppLoader.launchApp(this, app.packageName)
                closeGlobalSearch()
            }
        )
        rvGlobalSearchResults.adapter = globalSearchAdapter
        rvGlobalSearchResults.layoutManager = GridLayoutManager(this, 4)
    }

    private fun filterGlobalSearch(query: String) {
        val filtered = if (query.isEmpty()) {
            allAppsList
        } else {
            allAppsList.filter { it.label.contains(query, ignoreCase = true) }
        }
        globalSearchAdapter?.updateData(filtered)

        if (filtered.isEmpty()) {
            tvGlobalSearchEmpty.visibility = View.VISIBLE
            rvGlobalSearchResults.visibility = View.GONE
        } else {
            tvGlobalSearchEmpty.visibility = View.GONE
            rvGlobalSearchResults.visibility = View.VISIBLE
        }
    }

    fun openGlobalSearch() {
        clGlobalSearchContainer.visibility = View.VISIBLE
        clGlobalSearchContainer.alpha = 0f
        clGlobalSearchContainer.animate().alpha(1f).setDuration(200).start()
        etGlobalSearch.setText("")
        etGlobalSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(etGlobalSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        filterGlobalSearch("")
    }

    fun openLibrary() {
        Log.d("Drawer", "openLibrary: current=$currentPage")
        if (currentPage != 2) {
            navigateToPage(2)
            if (!isOrbitalDrawer()) {
                val rvLibrary = findViewById<RecyclerView>(R.id.rv_library_apps)
                rvLibrary?.scrollToPosition(0)
                animateDragHandle(R.id.drag_handle_library)
                applyLibraryInsets()
            }
        }
    }

    private fun applyLibraryInsets() {
        val target = if (libraryPage.childCount > 0) libraryPage.getChildAt(0) else libraryPage
        val insets = libraryPage.rootWindowInsets ?: return
        val statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
        libraryPage.setPadding(0, 0, 0, 0)
        if (isOrbitalDrawer()) {
            target.setPadding(0, statusBarHeight, 0, 0)
        } else {
            target.setPadding(dpToPx(16), statusBarHeight, dpToPx(16), target.paddingBottom)
        }
    }

    private fun animateDragHandle(handleId: Int) {
        val handle = findViewById<View>(handleId) ?: return
        handle.scaleX = 0f
        handle.scaleY = 0f
        handle.pivotX = handle.layoutParams.width / 2f
        handle.pivotY = handle.layoutParams.height / 2f
        handle.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(100)
            .setDuration(500)
            .setInterpolator(android.view.animation.OvershootInterpolator(2f))
            .start()
    }

    private fun closeGlobalSearch() {
        clGlobalSearchContainer.animate().alpha(0f).setDuration(150).withEndAction {
            clGlobalSearchContainer.visibility = View.GONE
        }.start()
        etGlobalSearch.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etGlobalSearch.windowToken, 0)
    }

    private fun showAppContextMenu(app: AppInfo, contextType: String, anchorView: View, holder: RecyclerView.ViewHolder? = null) {
        val theme = ThemeManager.currentTheme.value
        val context = this

        val popupView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = getPopupBackground(theme)
            elevation = dpToPx(8f)
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }

        val popupWindow = android.widget.PopupWindow(
            popupView,
            dpToPx(200),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            elevation = dpToPx(8f)
            setOnDismissListener { gestureController.onPopupDismissed() }
        }

        fun addMenuItem(title: String, iconRes: Int, onClick: () -> Unit) {
            val itemView = LayoutInflater.from(context).inflate(R.layout.item_context_menu, popupView, false)
            val ivIcon = itemView.findViewById<ImageView>(R.id.iv_menu_icon)
            val tvLabel = itemView.findViewById<TextView>(R.id.tv_menu_label)

            ivIcon.setImageResource(iconRes)
            ivIcon.setColorFilter(theme.colors.primary)
            tvLabel.text = title
            tvLabel.setTextColor(theme.colors.textPrimary)

            itemView.setOnClickListener {
                popupWindow.dismiss()
                onClick()
            }

            popupView.addView(itemView)
        }

        val isFolder = app.packageName.startsWith("folder_")
        
        // Opções específicas da Biblioteca (Gaveta)
        if (contextType == "library" && !isFolder) {
            addMenuItem(getString(R.string.add_to_home), R.drawable.ic_add) {
                addAppShortcutToHome(app)
            }
        }
        
        if (contextType == "desktop" || contextType == "dock") {
            if (contextType == "desktop") {
                val removeTitle = if (isFolder) getString(R.string.delete_folder) else getString(R.string.remove_shortcut)
                addMenuItem(removeTitle, R.drawable.ic_close) {
                    val index = homeAppsPackages.indexOf(app.packageName)
                    if (index != -1) {
                        homeAppsPackages[index] = ""
                    }
                    if (isFolder) {
                        for (i in homeAppsPackages.indices) {
                            if (homeAppsPackages[i].startsWith("placeholder_${app.packageName}_")) {
                                homeAppsPackages[i] = ""
                            }
                        }
                        prefs.edit().remove("folder_name_${app.packageName}")
                                   .remove("folder_apps_${app.packageName}")
                                   .remove("folder_size_${app.packageName}").apply()
                    }
                    saveHomeAppsList(homeAppsPackages)
                    refreshDesktopGrid()
                    val msg = if (isFolder) getString(R.string.folder_deleted) else getString(R.string.shortcut_removed)
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
            if (isFolder) {
                addMenuItem(getString(R.string.rename_folder), R.drawable.ic_palette) {
                    showRenameFolderDialog(app.packageName) { newName ->
                        loadAndSetupApps()
                        refreshDesktopGrid()
                    }
                }
            } else {
                addMenuItem(getString(R.string.rename_shortcut), R.drawable.ic_palette) {
                    showRenameShortcutDialog(app, contextType)
                }
            }

            
            // Modo Edição
            addMenuItem(getString(R.string.edit_mode), R.drawable.ic_check) {
                enableEditMode()
            }
        }
        
        if (!isFolder) {
            // App Info
            addMenuItem(getString(R.string.app_info), R.drawable.ic_settings) {
                openAppDetailsSettings(app.packageName)
            }
            
            // Uninstall
            if (app.packageName != packageName) {
                addMenuItem(getString(R.string.uninstall_app), R.drawable.ic_close) {
                    uninstallApp(app.packageName)
                }
            }
        }
        
        // Calculate and show the popup window safely on screen
        try {
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val popupWidth = popupView.measuredWidth
            val popupHeight = popupView.measuredHeight
            
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            val viewX = location[0]
            val viewY = location[1]
            val viewWidth = anchorView.width
            val viewHeight = anchorView.height
            
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val margin = dpToPx(16)
            
            var x = viewX + (viewWidth - popupWidth) / 2
            x = x.coerceIn(margin, screenWidth - popupWidth - margin)
            
            val y = if (viewY - popupHeight - dpToPx(8) > margin) {
                viewY - popupHeight - dpToPx(4)
            } else {
                viewY + viewHeight + dpToPx(4)
            }
            
            popupWindow.showAtLocation(rootLayout, Gravity.NO_GRAVITY, x, y)
        } catch (e: Exception) {
            popupWindow.showAtLocation(rootLayout, Gravity.CENTER, 0, 0)
        }
        // Transita para estado POPUP para bloquear novos gestos enquanto o menu está aberto
        gestureController.onPopupOpened()
    }

    private fun showDesktopContextMenu(x: Int, y: Int) {
        val theme = ThemeManager.currentTheme.value
        val context = this
        
        val popupView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = getPopupBackground(theme)
            elevation = dpToPx(8f)
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }
        
        val popupWindow = android.widget.PopupWindow(
            popupView,
            dpToPx(200),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            elevation = dpToPx(8f)
            // Notifica o GestureController: estado POPUP ativo
            setOnDismissListener { gestureController.onPopupDismissed() }
        }
        
        fun addMenuItem(title: String, iconRes: Int, onClick: () -> Unit) {
            val itemLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                isClickable = true
                isFocusable = true
                
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.setBackgroundColor(adjustAlpha(theme.colors.primary, 0.15f))
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.setBackgroundColor(Color.TRANSPARENT)
                        }
                    }
                    false
                }
            }
            
            val ivIcon = ImageView(context).apply {
                setImageResource(iconRes)
                layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(18)).apply {
                    setMargins(0, 0, dpToPx(12), 0)
                }
                setColorFilter(theme.colors.primary)
            }
            itemLayout.addView(ivIcon)
            
            val tvLabel = TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(theme.colors.textPrimary)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            itemLayout.addView(tvLabel)
            
            itemLayout.setOnClickHaptic {
                popupWindow.dismiss()
                onClick()
            }
            
            popupView.addView(itemLayout)
        }
        
        addMenuItem(getString(R.string.settings), R.drawable.ic_settings) {
            val intent = Intent(context, SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        
        addMenuItem(getString(R.string.create_folder), R.drawable.ic_folder) {
            showCreateFolderDialog()
        }
        
        addMenuItem(getString(R.string.organize_screen), R.drawable.ic_check) {
            enableEditMode()
        }
        
        try {
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val popupWidth = popupView.measuredWidth
            val popupHeight = popupView.measuredHeight
            
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val margin = dpToPx(16)
            
            var posX = x - popupWidth / 2
            posX = posX.coerceIn(margin, screenWidth - popupWidth - margin)
            
            var posY = y - popupHeight / 2
            posY = posY.coerceIn(margin, screenHeight - popupHeight - margin)
            
            popupWindow.showAtLocation(rootLayout, Gravity.NO_GRAVITY, posX, posY)
        } catch (e: Exception) {
            popupWindow.showAtLocation(rootLayout, Gravity.CENTER, 0, 0)
        }
        // Transita para estado POPUP para bloquear novos gestos enquanto o menu está aberto
        gestureController.onPopupOpened()
    }

    private fun getFolderApps(folderId: String): List<AppInfo> {
        val saved = prefs.getString("folder_apps_$folderId", "") ?: ""
        if (saved.isEmpty()) return emptyList()
        val packages = saved.split(",").filter { it.isNotEmpty() }
        
        return packages.mapNotNull { pkg ->
            allAppsList.find { it.packageName == pkg } ?: try {
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                AppInfo(label = label, packageName = pkg)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun detectDeviceCategories(): List<String> {
        val categories = mutableSetOf<String>()
        val pm = packageManager
        
        for (app in allAppsList) {
            val pkg = app.packageName
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                
                // Check official Android category (API 26+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    when (appInfo.category) {
                        android.content.pm.ApplicationInfo.CATEGORY_GAME -> { categories.add(getString(R.string.cat_games)); continue }
                        android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> { categories.add(getString(R.string.cat_social)); continue }
                        android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> { categories.add(getString(R.string.cat_productivity)); continue }
                        android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
                        android.content.pm.ApplicationInfo.CATEGORY_VIDEO,
                        android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> { categories.add(getString(R.string.cat_media)); continue }
                        android.content.pm.ApplicationInfo.CATEGORY_MAPS -> { categories.add(getString(R.string.cat_tools)); continue }
                    }
                }
                
                // Fallback keywords
                val pkgLower = pkg.lowercase()
                val labelLower = app.label.lowercase()
                
                if (pkgLower.contains("game") || pkgLower.contains("play") || pkgLower.contains("toy") || pkgLower.contains("arcade") || 
                    labelLower.contains("jogo") || labelLower.contains("game")) {
                    categories.add(getString(R.string.cat_games))
                } else if (pkgLower.contains("whatsapp") || pkgLower.contains("facebook") || pkgLower.contains("instagram") || 
                           pkgLower.contains("twitter") || pkgLower.contains("tiktok") || pkgLower.contains("messenger") || 
                           pkgLower.contains("telegram") || pkgLower.contains("social") || pkgLower.contains("discord") ||
                           labelLower.contains("social") || labelLower.contains("chat") || labelLower.contains("messenger")) {
                    categories.add(getString(R.string.cat_social))
                } else if (pkgLower.contains("music") || pkgLower.contains("video") || pkgLower.contains("player") || 
                           pkgLower.contains("camera") || pkgLower.contains("gallery") || pkgLower.contains("youtube") || 
                           pkgLower.contains("netflix") || pkgLower.contains("spotify") || pkgLower.contains("sound") ||
                           labelLower.contains("música") || labelLower.contains("video") || labelLower.contains("camera")) {
                    categories.add(getString(R.string.cat_media))
                } else if (pkgLower.contains("bank") || pkgLower.contains("finance") || pkgLower.contains("wallet") || 
                           pkgLower.contains("pay") || pkgLower.contains("card") || pkgLower.contains("nubank") || 
                           pkgLower.contains("inter") || pkgLower.contains("itau") || pkgLower.contains("bradesco") ||
                           labelLower.contains("banco") || labelLower.contains("cartão") || labelLower.contains("finança")) {
                    categories.add(getString(R.string.cat_finance))
                } else if (pkgLower.contains("tool") || pkgLower.contains("utility") || pkgLower.contains("calculator") || 
                           pkgLower.contains("clock") || pkgLower.contains("calendar") || pkgLower.contains("setting") || 
                           pkgLower.contains("file") || pkgLower.contains("cleaner") || pkgLower.contains("browser") ||
                           pkgLower.contains("chrome") || labelLower.contains("ferramenta") || labelLower.contains("config") ||
                           labelLower.contains("calculadora") || labelLower.contains("relógio")) {
                    categories.add(getString(R.string.cat_tools))
                } else if (pkgLower.contains("office") || pkgLower.contains("note") || pkgLower.contains("drive") || 
                           pkgLower.contains("sheet") || pkgLower.contains("document") || pkgLower.contains("pdf") || 
                           pkgLower.contains("mail") || pkgLower.contains("gmail") || labelLower.contains("nota") ||
                           labelLower.contains("documento") || labelLower.contains("produtividade")) {
                    categories.add(getString(R.string.cat_productivity))
                }
            } catch (e: Exception) {}
        }
        
        val order = listOf(getString(R.string.cat_games), getString(R.string.cat_tools), getString(R.string.cat_social), getString(R.string.cat_productivity), getString(R.string.cat_media), getString(R.string.cat_finance))
        return order.filter { categories.contains(it) }
    }

    /**
     * Returns all installed apps that belong to the given category name.
     * Uses the same detection logic as detectDeviceCategories().
     */
    private fun getAppsByCategory(categoryName: String): List<AppInfo> {
        val pm = packageManager
        val result = mutableListOf<AppInfo>()

        for (app in allAppsList) {
            val pkg = app.packageName
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                var matched = false

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    matched = when (categoryName) {
                        getString(R.string.cat_games) -> appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_GAME
                        getString(R.string.cat_social) -> appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_SOCIAL
                        getString(R.string.cat_productivity) -> appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY
                        getString(R.string.cat_media) -> appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_AUDIO ||
                                   appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_VIDEO ||
                                   appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_IMAGE
                        getString(R.string.cat_tools) -> appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_MAPS
                        else -> false
                    }
                }

                if (!matched) {
                    val pkgLower = pkg.lowercase()
                    val labelLower = app.label.lowercase()
                    matched = when (categoryName) {
                        getString(R.string.cat_games) -> pkgLower.contains("game") || pkgLower.contains("play") ||
                                   pkgLower.contains("toy") || pkgLower.contains("arcade") ||
                                   labelLower.contains("jogo") || labelLower.contains("game")
                        getString(R.string.cat_social) -> pkgLower.contains("whatsapp") || pkgLower.contains("facebook") ||
                                     pkgLower.contains("instagram") || pkgLower.contains("twitter") ||
                                     pkgLower.contains("tiktok") || pkgLower.contains("messenger") ||
                                     pkgLower.contains("telegram") || pkgLower.contains("social") ||
                                     pkgLower.contains("discord") || labelLower.contains("social") ||
                                     labelLower.contains("chat") || labelLower.contains("messenger")
                        getString(R.string.cat_media) -> pkgLower.contains("music") || pkgLower.contains("video") ||
                                   pkgLower.contains("player") || pkgLower.contains("camera") ||
                                   pkgLower.contains("gallery") || pkgLower.contains("youtube") ||
                                   pkgLower.contains("netflix") || pkgLower.contains("spotify") ||
                                   pkgLower.contains("sound") || labelLower.contains("música") ||
                                   labelLower.contains("video") || labelLower.contains("camera")
                        getString(R.string.cat_finance) -> pkgLower.contains("bank") || pkgLower.contains("finance") ||
                                     pkgLower.contains("wallet") || pkgLower.contains("pay") ||
                                     pkgLower.contains("card") || pkgLower.contains("nubank") ||
                                     pkgLower.contains("inter") || pkgLower.contains("itau") ||
                                     pkgLower.contains("bradesco") || labelLower.contains("banco") ||
                                     labelLower.contains("cartão") || labelLower.contains("finança")
                        getString(R.string.cat_tools) -> pkgLower.contains("tool") || pkgLower.contains("utility") ||
                                        pkgLower.contains("calculator") || pkgLower.contains("clock") ||
                                        pkgLower.contains("calendar") || pkgLower.contains("setting") ||
                                        pkgLower.contains("file") || pkgLower.contains("cleaner") ||
                                        pkgLower.contains("browser") || pkgLower.contains("chrome") ||
                                        labelLower.contains("ferramenta") || labelLower.contains("config") ||
                                        labelLower.contains("calculadora") || labelLower.contains("relógio")
                        getString(R.string.cat_productivity) -> pkgLower.contains("office") || pkgLower.contains("note") ||
                                         pkgLower.contains("drive") || pkgLower.contains("sheet") ||
                                         pkgLower.contains("document") || pkgLower.contains("pdf") ||
                                         pkgLower.contains("mail") || pkgLower.contains("gmail") ||
                                         labelLower.contains("nota") || labelLower.contains("documento") ||
                                         labelLower.contains("produtividade")
                        else -> false
                    }
                }

                if (matched) result.add(app)
            } catch (e: Exception) {}
        }
        return result
    }

    /**
     * Creates a folder directly from a category, auto-detecting all matching apps.
     * No manual app picker shown.
     */
    private fun createFolderFromCategory(categoryName: String) {
        val appsInCategory = getAppsByCategory(categoryName)
        if (appsInCategory.isEmpty()) {
            android.widget.Toast.makeText(this, getString(R.string.no_apps_for_category, categoryName), android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val folderId = "folder_" + System.currentTimeMillis()
        prefs.edit()
            .putString("folder_name_$folderId", categoryName)
            .putString("folder_apps_$folderId", appsInCategory.joinToString(",") { it.packageName })
            .apply()

        // Remove apps from home grid if they were pinned there
        for (app in appsInCategory) {
            val index = homeAppsPackages.indexOf(app.packageName)
            if (index != -1) homeAppsPackages[index] = ""
        }

        val firstEmpty = homeAppsPackages.indexOf("")
        if (firstEmpty != -1) {
            homeAppsPackages[firstEmpty] = folderId
        } else {
            android.widget.Toast.makeText(this, getString(R.string.no_free_space), android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        saveHomeAppsList(homeAppsPackages)
        loadAndSetupApps()
        refreshDesktopGrid()
        android.widget.Toast.makeText(
            this,
            getString(R.string.folder_created, categoryName, appsInCategory.size),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun showCreateFolderDialog() {
        val theme = ThemeManager.currentTheme.value
        val context = this
        val builder = AlertDialog.Builder(context)

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
            text = getString(R.string.new_folder)
            textSize = 18f
            setTextColor(theme.colors.textPrimary)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(4))
        }
        dialogLayout.addView(tvTitle)

        val tvSubtitle = TextView(context).apply {
            text = getString(R.string.create_folder_subtitle)
            textSize = 12f
            setTextColor(adjustAlpha(theme.colors.textSecondary, 0.8f))
            setPadding(0, 0, 0, dpToPx(14))
        }
        dialogLayout.addView(tvSubtitle)

        val detectedCategories = detectDeviceCategories()
        val suggestions = if (detectedCategories.isNotEmpty()) detectedCategories else
            listOf(getString(R.string.cat_games), getString(R.string.cat_tools), getString(R.string.cat_social), getString(R.string.cat_productivity), getString(R.string.cat_media), getString(R.string.cat_finance))

        var folderDialog: AlertDialog? = null

        // ── Category chips (auto-create folder on tap) ──────────────────────
        val chipsScrollView = android.widget.HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(16) }
            isHorizontalScrollBarEnabled = false
        }
        val chipsContainer = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }

        for (name in suggestions) {
            val chip = TextView(context).apply {
                text = name
                textSize = 13f
                setTextColor(theme.colors.textPrimary)
                setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
            setColor(adjustAlpha(Color.BLACK, 0.7f))
                    setColor(adjustAlpha(theme.colors.primary, 0.12f))
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.35f))
                }
                addRipple(adjustAlpha(theme.colors.primary, 0.25f))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = dpToPx(8) }
                isClickable = true
                isFocusable = true
                // Auto-create folder when tapping a category chip
                setOnClickHaptic {
                    folderDialog?.dismiss()
                    createFolderFromCategory(name)
                }
            }
            chipsContainer.addView(chip)
        }
        chipsScrollView.addView(chipsContainer)
        dialogLayout.addView(chipsScrollView)

        // ── Divider ──────────────────────────────────────────────────────────
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
            ).apply { bottomMargin = dpToPx(14) }
            setBackgroundColor(adjustAlpha(theme.colors.primary, 0.12f))
        }
        dialogLayout.addView(divider)

        val tvCustomLabel = TextView(context).apply {
            text = getString(R.string.custom_name)
            textSize = 12f
            setTextColor(adjustAlpha(theme.colors.textSecondary, 0.7f))
            setPadding(0, 0, 0, dpToPx(6))
        }
        dialogLayout.addView(tvCustomLabel)

        // ── Custom name input ────────────────────────────────────────────────
        val etInput = EditText(context).apply {
            setHint(getString(R.string.custom_name_hint))
            setTextColor(theme.colors.textPrimary)
            setHintTextColor(adjustAlpha(theme.colors.textSecondary, 0.5f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                setColor(adjustAlpha(theme.colors.surface, 0.5f))
                setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.2f))
            }
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
        }
        dialogLayout.addView(etInput)

        // ── Buttons ──────────────────────────────────────────────────────────
        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dpToPx(12), 0, 0)

            val btnCancel = TextView(context).apply {
                text = getString(R.string.cancel)
                setTextColor(theme.colors.textSecondary)
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                setOnClickHaptic { folderDialog?.dismiss() }
            }
            addView(btnCancel)

            val btnNext = TextView(context).apply {
                text = getString(R.string.select_apps)
                setTextColor(theme.colors.primary)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                setOnClickHaptic {
                    val folderName = etInput.text.toString().trim()
                    if (folderName.isNotEmpty()) {
                        folderDialog?.dismiss()
                        showFolderAppPicker(null, folderName)
                    } else {
                        android.widget.Toast.makeText(context, getString(R.string.invalid_name), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            addView(btnNext)
        }
        dialogLayout.addView(buttonsLayout)

        builder.setView(dialogLayout)
        val dialog = builder.create()
        folderDialog = dialog
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showFolderAppPicker(folderId: String?, newFolderName: String? = null) {
        val theme = ThemeManager.currentTheme.value
        val context = this
        val builder = AlertDialog.Builder(context)
        
        val titleText = if (folderId == null) getString(R.string.select_apps_for, newFolderName ?: "") else getString(R.string.edit_folder_apps)
        val initialSelected = if (folderId != null) {
            val saved = prefs.getString("folder_apps_$folderId", "") ?: ""
            saved.split(",").filter { it.isNotEmpty() }.toMutableSet()
        } else {
            mutableSetOf<String>()
        }
        
        val selectedPackages = initialSelected.toMutableSet()
        
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
            text = titleText
            textSize = 18f
            setTextColor(theme.colors.textPrimary)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(12))
        }
        dialogLayout.addView(tvTitle)
        
        val sortedApps = allAppsList.sortedBy { it.label.lowercase() }
        
        val rvApps = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(350f).toInt()
            )
            layoutManager = LinearLayoutManager(context)
        }
        dialogLayout.addView(rvApps)
        
        val density = context.resources.displayMetrics.density
        val dpToPxVal = { dp: Float -> dp * density }
        
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                val ivIcon: ImageView = view.findViewById(1)
                val tvName: TextView = view.findViewById(2)
                val checkBox: android.widget.CheckBox = view.findViewById(3)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val ll = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        dpToPxVal(52f).toInt()
                    )
                    setPadding(dpToPxVal(12f).toInt(), 0, dpToPxVal(12f).toInt(), 0)
                    isClickable = true
                    isFocusable = true
                    val typedValue = android.util.TypedValue()
                    if (parent.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)) {
                        background = ContextCompat.getDrawable(parent.context, typedValue.resourceId)
                    }
                }
                
                val iv = ImageView(parent.context).apply {
                    id = 1
                    layoutParams = LinearLayout.LayoutParams(dpToPxVal(36f).toInt(), dpToPxVal(36f).toInt()).apply {
                        marginEnd = dpToPxVal(12f).toInt()
                    }
                }
                ll.addView(iv)
                
                val tv = TextView(parent.context).apply {
                    id = 2
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setTextColor(theme.colors.textPrimary)
                    textSize = 15f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                ll.addView(tv)
                
                val cb = android.widget.CheckBox(parent.context).apply {
                    id = 3
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    buttonTintList = android.content.res.ColorStateList.valueOf(theme.colors.primary)
                    isClickable = false
                    isFocusable = false
                }
                ll.addView(cb)
                
                return ViewHolder(ll)
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = holder as ViewHolder
                val app = sortedApps[position]
                item.tvName.text = app.label
                
                val shape = prefs.getString("icon_shape", "theme") ?: "theme"
                val shapeMode = prefs.getString("icon_shape_mode", "force_launcher") ?: "force_launcher"
                val useMonochromatic = prefs.getBoolean("monochromatic_icons", false)
                val context = item.itemView.context
                val density = context.resources.displayMetrics.density
                val sizePx = dpToPxVal(36f).toInt()
                val radiusPx = when (shape) {
                    "circle" -> sizePx / 2f
                    "square" -> 0f
                    "squircle" -> 18f * density * (36f / 60f)
                    else -> theme.shapes.cardCornerRadius.toFloat() * density * (36f / 60f)
                }
                val shapeClip = if (shapeMode == "force_launcher") com.alisu.alauncher.ui.ShapeClippingTransformation(shape, radiusPx) else null

                item.ivIcon.load("app-icon://${app.packageName}") {
                    size(sizePx, sizePx)
                    allowHardware(true)
                    memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    if (useMonochromatic) {
                        val transforms = mutableListOf<coil.transform.Transformation>(com.alisu.alauncher.ui.MonochromaticTransformation(theme.colors.primary, theme.colors.surface))
                        if (shapeClip != null) transforms.add(shapeClip)
                        transformations(transforms)
                    } else if (shapeClip != null) {
                        transformations(shapeClip)
                    }
                }
                
                item.checkBox.isChecked = selectedPackages.contains(app.packageName)
                
                item.itemView.setOnClickHaptic {
                    if (selectedPackages.contains(app.packageName)) {
                        selectedPackages.remove(app.packageName)
                        item.checkBox.isChecked = false
                    } else {
                        selectedPackages.add(app.packageName)
                        item.checkBox.isChecked = true
                    }
                }
            }

            override fun getItemCount(): Int = sortedApps.size
        }
        
        rvApps.adapter = adapter
        
        var pickerDialog: AlertDialog? = null
        
        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dpToPx(16), 0, 0)
            
            val btnCancel = TextView(context).apply {
                text = getString(R.string.cancel)
                setTextColor(theme.colors.textSecondary)
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                setOnClickHaptic {
                    pickerDialog?.dismiss()
                }
            }
            addView(btnCancel)
            
            val btnConfirm = TextView(context).apply {
                text = getString(R.string.confirm)
                setTextColor(theme.colors.primary)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                setOnClickHaptic {
                    if (selectedPackages.isEmpty()) {
                        android.widget.Toast.makeText(context, getString(R.string.select_at_least_one), android.widget.Toast.LENGTH_SHORT).show()
                        return@setOnClickHaptic
                    }
                    
                    pickerDialog?.dismiss()
                    
                    val finalFolderId = folderId ?: ("folder_" + System.currentTimeMillis())
                    
                    if (folderId == null) {
                        prefs.edit().putString("folder_name_$finalFolderId", newFolderName).apply()
                    }
                    
                    prefs.edit().putString("folder_apps_$finalFolderId", selectedPackages.joinToString(",")).apply()
                    
                    for (pkg in selectedPackages) {
                        val index = homeAppsPackages.indexOf(pkg)
                        if (index != -1) {
                            homeAppsPackages[index] = ""
                        }
                    }
                    
                    if (folderId == null) {
                        val firstEmptyIndex = homeAppsPackages.indexOf("")
                        if (firstEmptyIndex != -1) {
                            homeAppsPackages[firstEmptyIndex] = finalFolderId
                        } else {
                            android.widget.Toast.makeText(context, getString(R.string.no_free_space), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    saveHomeAppsList(homeAppsPackages)
                    loadAndSetupApps()
                    refreshDesktopGrid()
                    
                    if (folderId != null) {
                        showFolderDialog(finalFolderId)
                    }
                }
            }
            addView(btnConfirm)
        }
        dialogLayout.addView(buttonsLayout)
        
        builder.setView(dialogLayout)
        val dialog = builder.create()
        pickerDialog = dialog
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.decorView?.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

        dialog.setOnDismissListener {
            dialog.window?.decorView?.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
            rvApps.adapter = null
            if (pickerDialog == dialog) {
                pickerDialog = null
            }
            System.gc()
        }
    }

    private fun showRenameFolderDialog(folderId: String, onRenamed: (String) -> Unit) {
        val theme = ThemeManager.currentTheme.value
        val context = this
        val builder = AlertDialog.Builder(context)
        val currentName = prefs.getString("folder_name_$folderId", getString(R.string.folder)) ?: getString(R.string.folder)
        
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
            text = getString(R.string.rename_folder)
            textSize = 18f
            setTextColor(theme.colors.textPrimary)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(12))
        }
        dialogLayout.addView(tvTitle)
        
        val etInput = EditText(context).apply {
            setText(currentName)
            setSelection(currentName.length)
            setTextColor(theme.colors.textPrimary)
            setHintTextColor(adjustAlpha(theme.colors.textSecondary, 0.5f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                setColor(adjustAlpha(theme.colors.surface, 0.5f))
                setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.2f))
            }
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
        }
        dialogLayout.addView(etInput)
        
        var renameDialog: AlertDialog? = null
        
        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dpToPx(16), 0, 0)
            
            val btnCancel = TextView(context).apply {
                text = getString(R.string.cancel)
                setTextColor(theme.colors.textSecondary)
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                setOnClickHaptic {
                    renameDialog?.dismiss()
                }
            }
            addView(btnCancel)
            
            val btnSave = TextView(context).apply {
                text = getString(R.string.save)
                setTextColor(theme.colors.primary)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                setOnClickHaptic {
                    val newName = etInput.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        prefs.edit().putString("folder_name_$folderId", newName).apply()
                        onRenamed(newName)
                        renameDialog?.dismiss()
                    }
                }
            }
            addView(btnSave)
        }
        dialogLayout.addView(buttonsLayout)
        
        builder.setView(dialogLayout)
        val dialog = builder.create()
        renameDialog = dialog
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showFolderDialog(folderId: String) {
        val theme = ThemeManager.currentTheme.value
        val folderOverlay = findViewById<View>(R.id.cl_folder_overlay)
        folderOverlay.tag = folderId
        // Cantos arredondados apenas na parte de cima
        val folderRadius = dpToPx(28f)
        val folderBg = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(android.graphics.Color.parseColor("#CC0E0E12"))
            cornerRadii = floatArrayOf(folderRadius, folderRadius, folderRadius, folderRadius, 0f, 0f, 0f, 0f)
        }
        folderOverlay.background = folderBg
        val tvFolderName = findViewById<TextView>(R.id.tv_folder_name)
        val ivFolderEditName = findViewById<ImageView>(R.id.iv_folder_edit_name)
        val rvFolderApps = findViewById<RecyclerView>(R.id.rv_folder_apps)

        val folderName = prefs.getString("folder_name_$folderId", getString(R.string.folder)) ?: getString(R.string.folder)

        tvFolderName.text = folderName
        tvFolderName.setTextColor(theme.colors.textPrimary)
        ivFolderEditName.setColorFilter(theme.colors.primary)

        val folderApps = getFolderApps(folderId).toMutableList()
        folderApps.add(AppInfo(label = getString(R.string.folder_add), packageName = "folder_action_add"))

        val folderAdapter = AppAdapter(
            apps = folderApps,
            theme = theme.copy(layout = theme.layout.copy(showAppName = true)),
            onAppClick = { clickedApp ->
                if (clickedApp.packageName == "folder_action_add") {
                    showFolderAppPicker(folderId)
                } else {
                    AppLoader.launchApp(this@MainActivity, clickedApp.packageName)
                    closeFolderOverlay()
                }
            }
        )
        rvFolderApps.layoutManager = GridLayoutManager(this, 4)
        rvFolderApps.adapter = folderAdapter

        ivFolderEditName.setOnClickListener {
            showRenameFolderDialog(folderId) { newName ->
                tvFolderName.text = newName
                loadAndSetupApps()
                refreshDesktopGrid()
            }
        }

        var folderStartY = 0f
        var folderTracking = false
        var folderActivated = false
        val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop

        folderOverlay.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    folderStartY = event.rawY
                    folderTracking = true
                    folderActivated = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!folderTracking) return@setOnTouchListener false
                    val dy = event.rawY - folderStartY
                    if (!folderActivated && dy > touchSlop) {
                        if (!rvFolderApps.canScrollVertically(-1)) {
                            folderActivated = true
                        }
                    }
                    if (folderActivated) {
                        val clampedTranslation = (folderOverlay.translationY + (event.rawY - folderStartY)).coerceAtLeast(0f)
                        folderOverlay.translationY = clampedTranslation
                        folderStartY = event.rawY
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (folderTracking) {
                        if (folderActivated) {
                            if (folderOverlay.translationY > container.height * 0.3f) {
                                closeFolderOverlay()
                            } else {
                                folderOverlay.springAnimateY(
                                    targetY = 0f,
                                    stiffness = 200f,
                                    dampingRatio = 0.7f
                                )
                            }
                        }
                        folderTracking = false
                        folderActivated = false
                    }
                    false
                }
                else -> false
            }
        }

        if (folderOverlay.visibility != View.VISIBLE) {
            desktopPage.animate().alpha(0f).setDuration(200L).start()
            folderOverlay.translationY = container.height.toFloat()
            folderOverlay.visibility = View.VISIBLE
            folderOverlay.springAnimateY(
                targetY = 0f,
                stiffness = 140f,
                dampingRatio = 0.65f
            )

            val dragHandle = findViewById<View>(R.id.drag_handle_folder)
            dragHandle?.let { handle ->
                handle.scaleX = 0f
                handle.scaleY = 0f
                handle.pivotX = handle.layoutParams.width / 2f
                handle.pivotY = handle.layoutParams.height / 2f
                handle.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(150)
                    .setDuration(500)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                    .start()
            }
        }
    }

    private fun closeFolderOverlay() {
        val folderOverlay = findViewById<View>(R.id.cl_folder_overlay)
        desktopPage.animate().alpha(1f).setDuration(200L).start()
        folderOverlay.springAnimateY(
            targetY = container.height.toFloat(),
            stiffness = SpringForce.STIFFNESS_MEDIUM,
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        ) {
            folderOverlay.visibility = View.GONE
            folderOverlay.translationY = 0f
        }
    }

    private fun openAppDetailsSettings(packageName: String) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, getString(R.string.cannot_open_app_settings), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, getString(R.string.cannot_uninstall), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRenameShortcutDialog(app: AppInfo, contextType: String) {
        val theme = ThemeManager.currentTheme.value
        val context = this
        val builder = AlertDialog.Builder(context)
        
        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
            
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                setColor(theme.colors.surface)
                if (theme.shapes.showBorders) {
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.3f))
                }
            }
            background = bg
        }
        
        val tvTitle = TextView(context).apply {
            text = getString(R.string.rename_shortcut)
            textSize = 18f
            setTextColor(theme.colors.textPrimary)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(12))
        }
        dialogLayout.addView(tvTitle)
        
        val etInput = EditText(context).apply {
            setText(app.label)
            setSelection(app.label.length)
            setTextColor(theme.colors.textPrimary)
            setHintTextColor(adjustAlpha(theme.colors.textSecondary, 0.5f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                setColor(adjustAlpha(theme.colors.surface, 0.5f))
                if (theme.shapes.showBorders) {
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.textSecondary, 0.2f))
                }
            }
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
        }
        dialogLayout.addView(etInput)
        
        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dpToPx(16), 0, 0)
            
            val btnCancel = TextView(context).apply {
                text = getString(R.string.cancel)
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(theme.colors.textSecondary)
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                isClickable = true
                isFocusable = true
                setOnClickHaptic {
                    alertDialog?.dismiss()
                }
            }
            addView(btnCancel)
            
            val btnSave = TextView(context).apply {
                text = getString(R.string.save)
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(theme.colors.primary)
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                isClickable = true
                isFocusable = true
                setOnClickHaptic {
                    val newName = etInput.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        prefs.edit().putString("custom_label_${app.packageName}", newName).apply()
                        alertDialog?.dismiss()
                        loadAndSetupApps()
                    }
                }
            }
            addView(btnSave)
        }
        dialogLayout.addView(buttonsLayout)
        
        builder.setView(dialogLayout)
        val dialog = builder.create()
        alertDialog = dialog
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun loadHomeAppsList(): MutableList<String> {
        val theme = ThemeManager.currentTheme.value
        val columns = getColumns(theme)
        val rows = try { resources.getInteger(R.integer.desktop_grid_rows) } catch (_: Exception) { 5 }
        val targetSize = columns * rows
        val saved = prefs.getString("home_apps_packages", null)
        
        if (saved.isNullOrEmpty()) {
            return MutableList(targetSize) { "" }
        }

        val list = saved.split(",").toMutableList()
        if (list.size != targetSize) {
            // Reajusta o tamanho mantendo o conteúdo
            if (list.size > targetSize) return list.take(targetSize).toMutableList()
            while (list.size < targetSize) list.add("")
        }
        return list
    }

    private fun loadAndSetupApps() {
        lifecycleScope.launch {
            allAppsList = com.alisu.alauncher.launcher.AppLoader.loadInstalledApps(this@MainActivity)
            homeAppsPackages = loadHomeAppsList()
            applyTheme(ThemeManager.currentTheme.value)
        }
    }

    private fun addAppShortcutToHome(app: AppInfo) {
        if (app.packageName.isEmpty()) return

        // Verifica se já existe (opcional, mas evita duplicados indesejados se você preferir)
        // if (homeAppsPackages.contains(app.packageName)) {
        //    android.widget.Toast.makeText(this, "O atalho já existe!", android.widget.Toast.LENGTH_SHORT).show()
        //    return
        // }

        val firstEmptyIndex = homeAppsPackages.indexOfFirst { it.isEmpty() }
        if (firstEmptyIndex == -1) {
            android.widget.Toast.makeText(this, getString(R.string.no_space_home), android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // Atualiza a lista
        homeAppsPackages[firstEmptyIndex] = app.packageName
        saveHomeAppsList(homeAppsPackages)
        
        // Sincroniza visualmente
        val updatedApps = getDesktopAppsList(allAppsList, ThemeManager.currentTheme.value)
        desktopAdapter?.updateData(updatedApps)

        if (clGlobalSearchContainer.visibility == View.VISIBLE) {
            closeGlobalSearch()
        }

        // Vai para a Home
        navigateToPage(1)
        
        // Força o RecyclerView a reconstruir a lista com o novo item
        refreshDesktopGrid()

        android.widget.Toast.makeText(this, getString(R.string.added_to_home, app.label), android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun createSpanSizeLookup(recyclerView: RecyclerView, columns: Int): GridLayoutManager.SpanSizeLookup {
        return object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }
    }

    private fun deleteAppShortcut(appToDelete: AppInfo) {
        var targetPkg = appToDelete.packageName
        if (targetPkg.startsWith("placeholder_")) {
            val lastUnderscore = targetPkg.lastIndexOf('_')
            val folderId = if (lastUnderscore != -1) targetPkg.substring(12, lastUnderscore) else ""
            if (folderId.isNotEmpty()) {
                targetPkg = folderId
            }
        }
        val index = homeAppsPackages.indexOf(targetPkg)
        if (index != -1) {
            homeAppsPackages[index] = ""
        }
        // Clear placeholders
        for (i in homeAppsPackages.indices) {
            if (homeAppsPackages[i].startsWith("placeholder_${targetPkg}_")) {
                homeAppsPackages[i] = ""
            }
        }
        if (targetPkg.startsWith("folder_")) {
            prefs.edit().remove("folder_name_$targetPkg")
                       .remove("folder_apps_$targetPkg")
                       .remove("folder_size_$targetPkg").apply()
        }
        saveHomeAppsList(homeAppsPackages)
        refreshDesktopGrid()
    }

    private fun handleAppLongClick(app: AppInfo, holder: RecyclerView.ViewHolder): Boolean {
        val isFolder = app.packageName.startsWith("folder_")
        val isPlaceholder = app.packageName.startsWith("placeholder_")
        
        if (desktopAdapter?.getEditMode() == true) {
            return false
        }

        var targetApp = app
        if (isPlaceholder) {
            val lastUnderscore = app.packageName.lastIndexOf('_')
            val folderId = if (lastUnderscore != -1) app.packageName.substring(12, lastUnderscore) else ""
            if (folderId.isNotEmpty()) {
                val folderName = prefs.getString("folder_name_$folderId", getString(R.string.folder)) ?: getString(R.string.folder)
                targetApp = AppInfo(label = folderName, packageName = folderId)
            }
        }
        showAppContextMenu(targetApp, "desktop", holder.itemView, holder)
        return true
    }

    private fun handleAppClick(app: AppInfo) {
        if (app.packageName.startsWith("folder_")) {
            showFolderDialog(app.packageName)
        } else {
            com.alisu.alauncher.launcher.AppLoader.launchApp(this@MainActivity, app.packageName)
        }
    }

    private fun realignGridAfterFolderMove(folderId: String, newIndex: Int) {
        val size = prefs.getInt("folder_size_$folderId", 1)
        if (size == 1) return
        
        val columns = getColumns(ThemeManager.currentTheme.value)
        val rows = try { resources.getInteger(R.integer.desktop_grid_rows) } catch (_: Exception) { 5 }
        val targetSize = columns * rows
        
        val tempGrid = homeAppsPackages.toMutableList()
        for (i in tempGrid.indices) {
            if (tempGrid[i].startsWith("placeholder_${folderId}_")) {
                tempGrid[i] = ""
            }
        }
        
        val r = newIndex / columns
        val c = newIndex % columns
        
        fun pushItemToEmptySlot(idx: Int) {
            if (idx < 0 || idx >= targetSize) return
            val itemToPush = tempGrid[idx]
            if (itemToPush.isNotEmpty() && !itemToPush.startsWith("placeholder_")) {
                val firstEmpty = tempGrid.indexOf("")
                if (firstEmpty != -1) {
                    tempGrid[firstEmpty] = itemToPush
                    tempGrid[idx] = ""
                } else {
                    tempGrid[idx] = ""
                }
            }
        }
        
        if (size == 2) {
            val targetRight = newIndex + 1
            if (c + 1 >= columns) {
                val adjustedIndex = newIndex - 1
                if (adjustedIndex >= 0) {
                    Collections.swap(tempGrid, newIndex, adjustedIndex)
                    homeAppsPackages.clear()
                    homeAppsPackages.addAll(tempGrid)
                    realignGridAfterFolderMove(folderId, adjustedIndex)
                    return
                }
            }
            pushItemToEmptySlot(targetRight)
            tempGrid[targetRight] = "placeholder_${folderId}_1"
        } else if (size == 4) {
            var adjustedIndex = newIndex
            var adjR = r
            var adjC = c
            
            if (c + 1 >= columns) {
                adjC = columns - 2
            }
            if (r + 1 >= rows) {
                adjR = rows - 2
            }
            adjustedIndex = adjR * columns + adjC
            
            if (adjustedIndex != newIndex) {
                Collections.swap(tempGrid, newIndex, adjustedIndex)
                homeAppsPackages.clear()
                homeAppsPackages.addAll(tempGrid)
                realignGridAfterFolderMove(folderId, adjustedIndex)
                return
            }
            
            val idxRight = adjustedIndex + 1
            val idxBottom = adjustedIndex + columns
            val idxCorner = adjustedIndex + columns + 1
            
            pushItemToEmptySlot(idxRight)
            pushItemToEmptySlot(idxBottom)
            pushItemToEmptySlot(idxCorner)
            
            tempGrid[idxRight] = "placeholder_${folderId}_1"
            tempGrid[idxBottom] = "placeholder_${folderId}_2"
            tempGrid[idxCorner] = "placeholder_${folderId}_3"
        }
        
        homeAppsPackages.clear()
        homeAppsPackages.addAll(tempGrid)
        saveHomeAppsList(homeAppsPackages)
    }

    private fun refreshDesktopGrid(view: View? = null) {
        val desktopView = view ?: desktopViewInstance ?: desktopPage
        if (desktopView == null) return
        desktopViewInstance = desktopView
        
        val rvDesktopApps = desktopView.findViewById<com.alisu.alauncher.workspace.WorkspaceLayout>(R.id.rv_home_desktop_apps) ?: return
        val theme = ThemeManager.currentTheme.value
        val columns = getColumns(theme)
        
        var lm = rvDesktopApps.layoutManager as? GridLayoutManager
        if (lm == null || lm.spanCount != columns) {
            lm = object : GridLayoutManager(this, columns) {
                override fun canScrollVertically(): Boolean = false
            }
            rvDesktopApps.layoutManager = lm
        }
        lm.spanSizeLookup = createSpanSizeLookup(rvDesktopApps, columns)

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val appsList = if (allAppsList.isNotEmpty()) allAppsList else com.alisu.alauncher.launcher.AppLoader.loadInstalledApps(this@MainActivity)
            val desktopApps = getDesktopAppsList(appsList, theme)
            val desktopTheme = theme.copy(
                layout = theme.layout.copy(showAppName = getShowAppName(theme))
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val currentEditMode = desktopAdapter?.getEditMode() ?: false
                var adapter = rvDesktopApps.adapter as? AppAdapter
                if (adapter == null) {
                    adapter = AppAdapter(
                        apps = desktopApps,
                        theme = desktopTheme,
                        onDeleteClick = { appToDelete ->
                            deleteAppShortcut(appToDelete)
                        },
                        onAppLongClick = { app, holder ->
                            handleAppLongClick(app, holder)
                        },
                        onAppClick = { app ->
                            handleAppClick(app)
                        },
                        gestureController = gestureController
                    ).apply {
                        isDesktopGrid = true
                    }
                    rvDesktopApps.adapter = adapter
                } else {
                    adapter.updateTheme(desktopTheme)
                    adapter.updateData(desktopApps)
                }

                val currentAdapter = adapter
                val gridRows = try { resources.getInteger(R.integer.desktop_grid_rows) } catch (_: Exception) { 5 }
                if (rvDesktopApps.height > 0) {
                    currentAdapter.cellHeight = rvDesktopApps.height / gridRows
                } else {
                    rvDesktopApps.post {
                        if (rvDesktopApps.height > 0) {
                            currentAdapter.cellHeight = rvDesktopApps.height / gridRows
                        }
                    }
                }

                adapter.setEditMode(currentEditMode)
                desktopAdapter = adapter
                
                // Registra o DropTarget e configura o listener (apenas uma vez)
                if (!dragController.dropTargets.contains(rvDesktopApps)) {
                    dragController.dropTargets.add(rvDesktopApps)
                }
                if (rvDesktopApps.onDropListener == null) {
                    rvDesktopApps.onDropListener = object : com.alisu.alauncher.workspace.WorkspaceLayout.OnDropListener {
                        override fun onDrop(item: AppInfo, targetIdx: Int) {
                            desktopAdapter?.setDragOverPosition(-1)
                            handleDropOnWorkspace(item, targetIdx)
                        }

                        override fun onDragOver(item: AppInfo, targetIdx: Int) {
                            desktopAdapter?.setDragOverPosition(targetIdx)
                        }

                        override fun onDragExit(item: AppInfo) {
                            desktopAdapter?.setDragOverPosition(-1)
                        }
                    }
                }
            }
        }
    }

    private fun setupHomeDesktop(view: View) {
        if (view.tag == "initialized") {
            refreshDesktopGrid(view)
            return
        }
        view.tag = "initialized"
        
        val currentTheme = ThemeManager.currentTheme.value
        
        llDockContainer = view.findViewById(R.id.ll_dock_container)
        rvDockApps = view.findViewById(R.id.rv_home_apps)
        
        setupDockAppsGrid()

        // Conecta o GestureController à view do desktop e à dock.
        view.setOnTouchListener(gestureController)

        val desktopRoot = view.findViewById<View>(R.id.ll_desktop_root)
        desktopRoot.setOnTouchListener(gestureController)
        rvDockApps?.setOnTouchListener(gestureController)

        val columns = getColumns(currentTheme)
        val desktopTheme = currentTheme.copy(
            layout = currentTheme.layout.copy(showAppName = getShowAppName(currentTheme))
        )
        val rvDesktopApps = view.findViewById<RecyclerView>(R.id.rv_home_desktop_apps)
        rvDesktopApps.setOnTouchListener(gestureController)

        updateDesktopClockAndMascot(view, currentTheme)
        
        refreshDesktopGrid(view)
    }

    private fun updateDesktopClockAndMascot(view: View, theme: AppTheme) {
        val clockContainer = view.findViewById<LinearLayout>(R.id.ll_clock_container) ?: return
        val mascotContainer = view.findViewById<LinearLayout>(R.id.fl_mascot_container) ?: return
        val ivMascot = view.findViewById<ImageView>(R.id.iv_mascot) ?: return
        val ivWeatherIcon = view.findViewById<ImageView>(R.id.iv_weather_icon)
        val tcTime = view.findViewById<TextClock>(R.id.tc_time) ?: return
        val tcDate = view.findViewById<TextClock>(R.id.tc_date) ?: return
        val tvWeatherTemp = view.findViewById<TextView>(R.id.tv_weather_temp)
        val tvWeatherCondition = view.findViewById<TextView>(R.id.tv_weather_condition)

        val clockStyle = prefs.getString("clock_style", "default") ?: "default"

        // Status bar height dinâmico (notch, punch-hole, etc)
        val statusBarHeight = try {
            val rootInsets = window.decorView.rootWindowInsets
            if (rootInsets != null && rootInsets.systemWindowInsetTop > 0) {
                rootInsets.systemWindowInsetTop
            } else {
                val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resId > 0) resources.getDimensionPixelSize(resId) else dpToPx(48)
            }
        } catch (_: Exception) {
            dpToPx(48)
        }

        if (clockStyle == "hidden") {
            clockContainer.visibility = View.GONE
            view.setPadding(view.paddingLeft, statusBarHeight + dpToPx(16), view.paddingRight, view.paddingBottom)
            return
        }

        view.setPadding(view.paddingLeft, 0, view.paddingRight, view.paddingBottom)
        clockContainer.visibility = View.VISIBLE

        // Reset background, orientation, padding and outline/clip settings before applying style
        clockContainer.background = null
        clockContainer.outlineProvider = null
        clockContainer.clipToOutline = false
        clockContainer.orientation = LinearLayout.VERTICAL
        clockContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        clockContainer.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16))
        clockContainer.gravity = android.view.Gravity.CENTER
        clockContainer.clipChildren = false
        clockContainer.clipToPadding = false

        val parentLp = clockContainer.layoutParams as LinearLayout.LayoutParams
        parentLp.topMargin = statusBarHeight + dpToPx(8)
        clockContainer.layoutParams = parentLp

        // Fallback: se insets ainda não disponíveis, ajusta post-layout
        if (statusBarHeight <= dpToPx(10)) {
            clockContainer.post {
                try {
                    val realTop = window.decorView.rootWindowInsets?.systemWindowInsetTop ?: 0
                    if (realTop > 0) {
                        val lp = clockContainer.layoutParams as LinearLayout.LayoutParams
                        lp.topMargin = realTop + dpToPx(8)
                        clockContainer.layoutParams = lp
                    }
                } catch (_: Exception) {}
            }
        }

        mascotContainer.orientation = LinearLayout.HORIZONTAL
        mascotContainer.visibility = View.VISIBLE
        tvWeatherTemp?.visibility = View.VISIBLE
        tvWeatherCondition?.visibility = View.VISIBLE
        ivWeatherIcon?.visibility = View.VISIBLE

        // Desanexa filhos dos pais intermedários antes de reorganizar
        clockContainer.removeAllViews()
        (mascotContainer.parent as? ViewGroup)?.removeView(mascotContainer)
        (tcTime.parent as? ViewGroup)?.removeView(tcTime)
        (tcDate.parent as? ViewGroup)?.removeView(tcDate)

        // Load weather data
        val cachedTemp = prefs.getInt("last_weather_temp", -999)
        val cachedIcon = prefs.getString("last_weather_icon", "") ?: ""
        val cachedCondition = prefs.getString("last_weather_condition", "") ?: ""

        // Load mascot/symbol
        val hasSymbol = theme.symbol != null
        val hasCharacter = theme.character != null

        when (clockStyle) {
            "default" -> applyClockStyleDefault(clockContainer, mascotContainer, ivMascot, ivWeatherIcon, tcTime, tcDate, tvWeatherTemp, tvWeatherCondition, theme, cachedTemp, cachedIcon, cachedCondition, hasSymbol, hasCharacter, view)
            "stacked" -> applyClockStyleStacked(clockContainer, mascotContainer, ivMascot, ivWeatherIcon, tcTime, tcDate, tvWeatherTemp, tvWeatherCondition, theme, cachedTemp, cachedIcon, cachedCondition, hasSymbol, hasCharacter, view)
            "symbol_horizontal" -> applyClockStyleSymbolHorizontal(clockContainer, mascotContainer, ivMascot, ivWeatherIcon, tcTime, tcDate, tvWeatherTemp, tvWeatherCondition, theme, cachedTemp, cachedIcon, cachedCondition, hasSymbol, hasCharacter, view)
            "symbol_stacked" -> applyClockStyleSymbolStacked(clockContainer, mascotContainer, ivMascot, ivWeatherIcon, tcTime, tcDate, tvWeatherTemp, tvWeatherCondition, theme, cachedTemp, cachedIcon, cachedCondition, hasSymbol, hasCharacter, view)
            "bracket" -> applyClockStyleBracket(clockContainer, mascotContainer, ivMascot, ivWeatherIcon, tcTime, tcDate, tvWeatherTemp, tvWeatherCondition, theme, cachedTemp, cachedIcon, cachedCondition, hasSymbol, hasCharacter, view)
            "minimal" -> applyClockStyleMinimal(clockContainer, mascotContainer, ivMascot, ivWeatherIcon, tcTime, tcDate, tvWeatherTemp, tvWeatherCondition, theme, cachedTemp, cachedIcon, cachedCondition, hasSymbol, hasCharacter, view)
            "character_card" -> applyClockStyleCharacterCard(clockContainer, mascotContainer, ivMascot, ivWeatherIcon, tcTime, tcDate, tvWeatherTemp, tvWeatherCondition, theme, cachedTemp, cachedIcon, cachedCondition, hasSymbol, hasCharacter, view)
            "character_split" -> applyClockStyleCharacterSplit(clockContainer, mascotContainer, ivMascot, ivWeatherIcon, tcTime, tcDate, tvWeatherTemp, tvWeatherCondition, theme, cachedTemp, cachedIcon, cachedCondition, hasSymbol, hasCharacter, view)
            else -> applyClockStyleDefault(clockContainer, mascotContainer, ivMascot, ivWeatherIcon, tcTime, tcDate, tvWeatherTemp, tvWeatherCondition, theme, cachedTemp, cachedIcon, cachedCondition, hasSymbol, hasCharacter, view)
        }

        val isPortrait = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        val lp = clockContainer.layoutParams as LinearLayout.LayoutParams
        lp.topMargin = statusBarHeight + dpToPx(8)
        lp.leftMargin = 0
        lp.rightMargin = 0
        
        if (clockStyle == "stacked") {
            val circleSize = dpToPx(220)
            lp.width = circleSize
            lp.height = circleSize
        } else if (isPortrait) {
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT
            lp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        }
        clockContainer.layoutParams = lp

        // Fallback: se insets ainda não disponíveis, ajusta post-layout
        if (statusBarHeight <= dpToPx(10)) {
            clockContainer.post {
                try {
                    val realTop = window.decorView.rootWindowInsets?.systemWindowInsetTop ?: 0
                    if (realTop > 0) {
                        val postLp = clockContainer.layoutParams as LinearLayout.LayoutParams
                        postLp.topMargin = realTop + dpToPx(8)
                        clockContainer.layoutParams = postLp
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // ================= CONFIGURAÇÃO DA BIBLIOTECA (PAGE 1) =================
    private fun setupAppLibrary(view: View) {
        val currentTheme = ThemeManager.currentTheme.value
        val etSearch = view.findViewById<EditText>(R.id.et_library_search)
        val llSearchBar = view.findViewById<LinearLayout>(R.id.ll_library_search_bar)
        val ivSearchIcon = view.findViewById<ImageView>(R.id.iv_library_search_icon)
        val ivClearSearch = view.findViewById<ImageView>(R.id.iv_library_clear_search)
        val rvLibraryApps = view.findViewById<RecyclerView>(R.id.rv_library_apps)

        val targetView = if (view is FrameLayout && view.childCount > 0) view.getChildAt(0) else view
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(targetView) { v, insets ->
            val statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(dpToPx(16), statusBarHeight, dpToPx(16), v.paddingBottom)
            insets
        }

        val columns = try {
            resources.getInteger(R.integer.app_drawer_grid_columns)
        } catch (_: Exception) {
            4
        }
        val libraryTheme = currentTheme.copy(
            layout = currentTheme.layout.copy(showAppName = getShowAppName(currentTheme))
        )

        val searchBarBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(currentTheme.shapes.searchBarCornerRadius.toFloat())
            setColor(adjustAlpha(currentTheme.colors.surface, 0.7f))
            if (currentTheme.shapes.showBorders) {
                setStroke(dpToPx(1f).toInt(), adjustAlpha(currentTheme.colors.primary, 0.2f))
            }
        }
        llSearchBar.background = searchBarBg
        etSearch.setTextColor(currentTheme.colors.textPrimary)
        etSearch.setHintTextColor(adjustAlpha(currentTheme.colors.textSecondary, 0.5f))
        ivSearchIcon.setColorFilter(currentTheme.colors.primary)
        ivClearSearch.imageTintList = android.content.res.ColorStateList.valueOf(currentTheme.colors.textSecondary)

        if (view.tag == "initialized") {
            val lm = rvLibraryApps.layoutManager as? GridLayoutManager
            if (lm == null || lm.spanCount != columns) {
                rvLibraryApps.layoutManager = GridLayoutManager(this, columns)
            }
            return
        }
        view.tag = "initialized"

        libraryAdapter = AppAdapter(
            apps = allAppsList,
            theme = libraryTheme,
            onDeleteClick = null,
            onAppLongClick = { app, holder ->
                showAppContextMenu(app, "library", holder.itemView)
                true
            },
            onAppClick = { app ->
                AppLoader.launchApp(this@MainActivity, app.packageName)
            },
            gestureController = gestureController
        )
        libraryAdapter?.initViewPool(this)
        rvLibraryApps.adapter = libraryAdapter
        rvLibraryApps.layoutManager = GridLayoutManager(this@MainActivity, columns)
        rvLibraryApps.setItemViewCacheSize(20)
        val pool = RecyclerView.RecycledViewPool()
        pool.setMaxRecycledViews(0, columns * 5)
        rvLibraryApps.setRecycledViewPool(pool)

        rvLibraryApps.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var isScrolling = false
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isScrolling = false
                    libraryAdapter?.setHardwareLayersForVisible(rv, false)
                }
            }
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (!isScrolling && (dx != 0 || dy != 0)) {
                    isScrolling = true
                    libraryAdapter?.setHardwareLayersForVisible(rv, true)
                }
            }
        })

        ivClearSearch.setOnClickHaptic {
            etSearch.setText("")
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                ivClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                searchDebounceRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
                val query = s?.toString() ?: ""
                searchDebounceRunnable = Runnable { filterLibrary(query) }
                searchDebounceHandler.postDelayed(searchDebounceRunnable!!, 200)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Gesto de arrastar para baixo na alça para fechar a gaveta
        val dragHandle = view.findViewById<View>(R.id.drag_handle_library)
        if (dragHandle != null) {
            var dragStartY = 0f
            var dragTracking = false
            var dragActivated = false
            val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop
            val velTracker = android.view.VelocityTracker.obtain()
            dragHandle.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartY = event.rawY
                        dragTracking = true
                        dragActivated = false
                        velTracker.clear()
                        velTracker.addMovement(event)
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!dragTracking) return@setOnTouchListener false
                        velTracker.addMovement(event)
                        velTracker.computeCurrentVelocity(1000)
                        val dy = event.rawY - dragStartY
                        if (!dragActivated && dy > touchSlop) {
                            dragActivated = true
                        }
                        if (dragActivated) {
                            libraryPage.translationY = (libraryPage.translationY + dy).coerceAtLeast(0f).coerceAtMost(container.height.toFloat())
                        }
                        dragStartY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (dragTracking && dragActivated) {
                            val velY = velTracker.yVelocity
                            if (libraryPage.translationY > container.height * 0.15f || velY > 500f) {
                                navigateToPage(1)
                            } else {
                                libraryPage.springAnimateY(targetY = 0f, stiffness = 180f, dampingRatio = 0.72f)
                            }
                        }
                        dragTracking = false
                        dragActivated = false
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupOrbitalDrawer(view: View) {
        if (view.tag == "initialized") return
        view.tag = "initialized"

        val sbHeight = try {
            val insets = window.decorView.rootWindowInsets
            if (insets != null && insets.systemWindowInsetTop > 0) insets.systemWindowInsetTop
            else {
                val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resId > 0) resources.getDimensionPixelSize(resId) else dpToPx(48).toInt()
            }
        } catch (_: Exception) { dpToPx(48).toInt() }
        view.setPadding(0, sbHeight, 0, 0)
        if (sbHeight <= dpToPx(10)) {
            view.post {
                try {
                    val realTop = window.decorView.rootWindowInsets?.systemWindowInsetTop ?: 0
                    if (realTop > 0) view.setPadding(0, realTop, 0, 0)
                } catch (_: Exception) {}
            }
        }

        val theme = ThemeManager.currentTheme.value
        val orbitView = view.findViewById<com.alisu.alauncher.ui.OrbitDrawerView>(R.id.orbit_drawer_view)
        orbitView?.let {
            it.applyTheme(theme)
            it.setApps(allAppsList)
            it.setOnAppLaunchCallback { packageName ->
                AppLoader.launchApp(this@MainActivity, packageName)
            }
            }

        val dragHandle = view.findViewById<View>(R.id.drag_handle_area)
        if (dragHandle != null) {
            var dragStartY = 0f
            var dragTracking = false
            var dragActivated = false
            val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop
            val velTracker = android.view.VelocityTracker.obtain()
            dragHandle.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartY = event.rawY
                        dragTracking = true
                        dragActivated = false
                        velTracker.clear()
                        velTracker.addMovement(event)
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!dragTracking) return@setOnTouchListener false
                        velTracker.addMovement(event)
                        velTracker.computeCurrentVelocity(1000)
                        val dy = event.rawY - dragStartY
                        if (!dragActivated && dy > touchSlop) {
                            dragActivated = true
                        }
                        if (dragActivated) {
                            libraryPage.translationY = (libraryPage.translationY + dy).coerceAtLeast(0f).coerceAtMost(container.height.toFloat())
                        }
                        dragStartY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (dragTracking && dragActivated) {
                            val velY = velTracker.yVelocity
                            if (libraryPage.translationY > container.height * 0.15f || velY > 500f) {
                                navigateToPage(1)
                            } else {
                                libraryPage.springAnimateY(targetY = 0f, stiffness = 180f, dampingRatio = 0.72f)
                            }
                        }
                        dragTracking = false
                        dragActivated = false
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun filterLibrary(query: String) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val filteredList = if (query.isEmpty()) {
                allAppsList
            } else {
                allAppsList.filter { it.label.contains(query, ignoreCase = true) }
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                libraryAdapter?.updateData(filteredList)
            }
        }
    }

    private var lastWidgetSetupTime = 0L

    private fun setupWidgetsPage(view: View) {
        val now = System.currentTimeMillis()
        if (now - lastWidgetSetupTime < 200) return
        lastWidgetSetupTime = now
        val currentTheme = ThemeManager.currentTheme.value

        val panelFrame = view.findViewById<View>(R.id.v_widgets_handle)?.parent as? View
        if (panelFrame != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(panelFrame) { v, insets ->
                v.setPadding(v.paddingLeft, insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).top, v.paddingRight, v.paddingBottom)
                insets
            }
            panelFrame.requestApplyInsets()
        }

        val handle = view.findViewById<View>(R.id.v_widgets_handle)
        if (handle != null) {
            var dragStartX = 0f
            var dragTracking = false
            var dragActivated = false
            val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop
            val velTracker = android.view.VelocityTracker.obtain()
            handle.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartX = event.rawX
                        dragTracking = true
                        dragActivated = false
                        velTracker.clear()
                        velTracker.addMovement(event)
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!dragTracking) return@setOnTouchListener false
                        velTracker.addMovement(event)
                        velTracker.computeCurrentVelocity(1000)
                        val dx = event.rawX - dragStartX
                        if (!dragActivated && dx > touchSlop) {
                            dragActivated = true
                        }
                        if (dragActivated) {
                            view.translationX = (view.translationX + dx).coerceAtLeast(0f)
                        }
                        dragStartX = event.rawX
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (dragTracking && dragActivated) {
                            val velX = velTracker.xVelocity
                            if (view.translationX > container.width * 0.15f || velX > 500f) {
                                navigateToPage(1)
                            } else {
                                view.springAnimateX(targetX = 0f, stiffness = 180f, dampingRatio = 0.72f)
                            }
                        }
                        dragTracking = false
                        dragActivated = false
                        true
                    }
                    else -> false
                }
            }
        }

        val svContent = view as? ScrollView ?: view.findViewById<ScrollView>(R.id.sv_widgets_content)
        if (svContent != null) {
            var swipeStartX = 0f
            var swipeStartY = 0f
            var swipeActive = false
            val swipeSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop
            svContent.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        swipeStartX = event.rawX
                        swipeStartY = event.rawY
                        swipeActive = false
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - swipeStartX
                        val dy = kotlin.math.abs(event.rawY - swipeStartY)
                        if (!swipeActive && dx > swipeSlop && dx > dy * 2f) {
                            swipeActive = true
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        if (swipeActive) {
                            view.translationX = (event.rawX - swipeStartX).coerceAtLeast(0f)
                            true
                        } else false
                    }
                    MotionEvent.ACTION_UP -> {
                        if (swipeActive) {
                            if (view.translationX > container.width * 0.15f) {
                                navigateToPage(1)
                            } else {
                                view.springAnimateX(targetX = 0f, stiffness = 180f, dampingRatio = 0.72f)
                            }
                            swipeActive = false
                        }
                        false
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        if (swipeActive) {
                            view.springAnimateX(targetX = 0f, stiffness = 180f, dampingRatio = 0.72f)
                            swipeActive = false
                        }
                        false
                    }
                    else -> false
                }
            }
        }

        val tvTitle = view.findViewById<TextView>(R.id.tv_widgets_title)
        val btnAddWidget = view.findViewById<LinearLayout>(R.id.btn_add_android_widget)
        val tvAddWidgetLabel = view.findViewById<TextView>(R.id.tv_add_widget_label)
        val ivAddWidgetIcon = view.findViewById<ImageView>(R.id.iv_add_widget_icon)
        val widgetsContainer = view.findViewById<LinearLayout>(R.id.ll_android_widgets_container)

        val cardNotes = view.findViewById<LinearLayout>(R.id.card_widget_notes)
        val tvNotesTitle = view.findViewById<TextView>(R.id.tv_widget_notes_title)
        val etNotes = view.findViewById<EditText>(R.id.et_widget_notes)

        val cardMusicPlayer = view.findViewById<FrameLayout>(R.id.card_music_player)
        val tvMusicPlayerTitle = view.findViewById<TextView>(R.id.tv_music_player_title)
        val tvMusicPlayerStatus = view.findViewById<TextView>(R.id.tv_music_player_status)
        val tvSongTitle = view.findViewById<TextView>(R.id.tv_song_title)
        val tvSongArtist = view.findViewById<TextView>(R.id.tv_song_artist)
        val btnPlayPause = view.findViewById<FrameLayout>(R.id.btn_play_pause)
        val ivPlayPause = view.findViewById<ImageView>(R.id.iv_play_pause)
        val ivPrev = view.findViewById<ImageView>(R.id.iv_prev)
        val ivNext = view.findViewById<ImageView>(R.id.iv_next)
        val ivAlbumArt = view.findViewById<ImageView>(R.id.iv_album_art)
        val tvCurrentTime = view.findViewById<TextView>(R.id.tv_current_time)
        val tvTotalTime = view.findViewById<TextView>(R.id.tv_total_time)
        val viewProgressFill = view.findViewById<View>(R.id.view_progress_fill)
        val viewProgressThumb = view.findViewById<View>(R.id.view_progress_thumb)
        val flProgressBar = view.findViewById<FrameLayout>(R.id.fl_progress_bar)
        val ivCompactToggle = view.findViewById<ImageView>(R.id.iv_compact_toggle)
        val btnQueue = view.findViewById<FrameLayout>(R.id.btn_queue)
        val ivQueue = view.findViewById<ImageView>(R.id.iv_queue)
        val flAlbumArt = view.findViewById<FrameLayout>(R.id.fl_album_art)
        val ivMusicCardBackdrop = view.findViewById<ImageView>(R.id.iv_music_card_backdrop)

        if (view.tag != "initialized") {
            viewProgressFill?.let { fill ->
                fill.scaleX = 0f
                fill.pivotX = 0f
                fill.animate()
                    .scaleX(1f)
                    .setStartDelay(200)
                    .setDuration(600)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
                    .start()
            }
        }

        applyMusicPlayerTheme(currentTheme, cardMusicPlayer, ivMusicCardBackdrop, tvMusicPlayerTitle, tvMusicPlayerStatus,
            tvSongTitle, tvSongArtist, btnPlayPause, ivPlayPause, ivPrev, ivNext,
            ivAlbumArt, tvCurrentTime, tvTotalTime,
            viewProgressFill, viewProgressThumb, ivCompactToggle, ivQueue)

        if (tvSongTitle != null && tvSongArtist != null && tvMusicPlayerStatus != null &&
            ivPlayPause != null && ivAlbumArt != null && tvCurrentTime != null &&
            tvTotalTime != null && viewProgressFill != null && viewProgressThumb != null) {
            mediaUiViews = MediaPlayerViews(
                tvSongTitle = tvSongTitle!!,
                tvSongArtist = tvSongArtist!!,
                tvMusicPlayerStatus = tvMusicPlayerStatus!!,
                ivPlayPause = ivPlayPause!!,
                ivAlbumArt = ivAlbumArt!!,
                tvCurrentTime = tvCurrentTime!!,
                tvTotalTime = tvTotalTime!!,
                viewProgressFill = viewProgressFill!!,
                viewProgressThumb = viewProgressThumb!!,
                cardMusicPlayer = cardMusicPlayer,
                flProgressBar = flProgressBar,
                ivCompactToggle = ivCompactToggle,
                btnQueue = btnQueue,
                ivQueue = ivQueue,
                flAlbumArt = flAlbumArt,
                ivMusicCardBackdrop = ivMusicCardBackdrop
            )
        }

        reconnectMediaSession()

        flAlbumArt?.let { art ->
            val albumRow = art.parent as? View
            albumRow?.visibility = View.VISIBLE
        }

        // ── Seek na barra de progresso ──
        flProgressBar?.setOnTouchListener { v, event ->
            val controller = mediaActiveController ?: return@setOnTouchListener false
            val meta = controller.metadata ?: return@setOnTouchListener false
            val duration = meta.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)
            if (duration <= 0) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val fraction = (event.x / v.width).coerceIn(0f, 1f)
                    val position = (duration * fraction).toLong()
                    val fill = viewProgressFill ?: return@setOnTouchListener false
                    val lp = fill.layoutParams
                    lp.width = (v.width * fraction).toInt()
                    fill.layoutParams = lp
                    viewProgressThumb?.let { thumb ->
                        thumb.visibility = View.VISIBLE
                        thumb.translationX = (v.width * fraction).toFloat() - dpToPx(7f)
                    }
                    tvCurrentTime?.text = formatMediaTime(position)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val fraction = (event.x / v.width).coerceIn(0f, 1f)
                    controller.transportControls.seekTo((duration * fraction).toLong())
                    true
                }
                else -> false
            }
        }

        // ── Tap no título abre app de música ──
        val openMusicApp = {
            val pkg = mediaActiveController?.packageName
            if (pkg != null) {
                try {
                    val intent = packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) startActivity(intent)
                } catch (_: Exception) {}
            }
        }
        tvSongTitle?.setOnClickListener { openMusicApp() }
        tvSongArtist?.setOnClickListener { openMusicApp() }

        // ── Fila (queue dialog) ──
        btnQueue?.setOnClickListener {
            showMusicQueueDialog()
        }

        tvTitle?.setTextColor(currentTheme.colors.textPrimary)
        tvNotesTitle?.setTextColor(currentTheme.colors.primary)
        etNotes?.setTextColor(currentTheme.colors.textPrimary)
        etNotes?.setHintTextColor(adjustAlpha(currentTheme.colors.textSecondary, 0.5f))

        val btnAddWidgetBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(currentTheme.shapes.cardCornerRadius.toFloat())
            setColor(adjustAlpha(currentTheme.colors.primary, 0.15f))
            if (currentTheme.shapes.showBorders) {
                setStroke(dpToPx(1.5f).toInt(), currentTheme.colors.primary)
            }
        }
        btnAddWidget?.background = btnAddWidgetBg
        tvAddWidgetLabel?.setTextColor(currentTheme.colors.textPrimary)
        ivAddWidgetIcon?.setColorFilter(currentTheme.colors.primary)

        val radius = dpToPx(currentTheme.shapes.cardCornerRadius.toFloat())
        val cardBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(adjustAlpha(currentTheme.colors.surface, 0.65f))
            if (currentTheme.shapes.showBorders) {
                setStroke(dpToPx(1f).toInt(), adjustAlpha(currentTheme.colors.primary, 0.2f))
            }
        }
        cardNotes?.background = cardBg

        val savedNote = prefs.getString("quick_notes_text", "")
        if (etNotes?.text?.toString() != savedNote) {
            etNotes?.setText(savedNote)
        }

        if (view.tag == "initialized") return

        view.findViewById<View>(R.id.v_widgets_close_overlay)?.setOnClickHaptic {
            navigateToPage(1)
        }

        btnAddWidget?.setOnClickHaptic {
            showCustomWidgetPickerDialog()
        }

        btnPlayPause?.setOnClickHaptic {
            btnPlayPause.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(80)
                .withEndAction {
                    btnPlayPause.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .setInterpolator(android.view.animation.OvershootInterpolator(3f))
                        .start()
                }
                .start()

            val controller = mediaActiveController
            if (controller != null) {
                val state = controller.playbackState
                if (state?.state == android.media.session.PlaybackState.STATE_PLAYING) {
                    sendMediaTransportCommand(1)
                } else {
                    sendMediaTransportCommand(0)
                }
            }
        }
        ivPrev?.setOnClickHaptic {
            ivPrev.animate().translationX(-8f).setDuration(80).withEndAction {
                ivPrev.animate().translationX(0f).setDuration(120).start()
            }.start()
            sendMediaTransportCommand(2)
        }
        ivNext?.setOnClickHaptic {
            ivNext.animate().translationX(8f).setDuration(80).withEndAction {
                ivNext.animate().translationX(0f).setDuration(120).start()
            }.start()
            sendMediaTransportCommand(3)
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                prefs.edit().putString("quick_notes_text", s.toString()).apply()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etNotes?.addTextChangedListener(watcher)
        etNotes?.tag = watcher

        val savedWidgets = prefs.getString("saved_widget_ids", "") ?: ""
        if (savedWidgets.isNotEmpty()) {
            val list = savedWidgets.split(",").filter { it.isNotEmpty() }
            for (idStr in list) {
                val id = idStr.toIntOrNull()
                if (id != null) createWidgetView(id)
            }
        }

        view.tag = "initialized"
    }

    // ================= GESTOS GLOBAIS =================
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var swipeTracking = false
    private var swipeActivated = false
    private var swipeIsHorizontal = false
    private var swipeTotalX = 0f
    private var swipeTotalY = 0f

    private fun canLibraryScrollUp(): Boolean {
        if (isOrbitalDrawer()) return true // orbital gerencia o próprio toque, não deixa fechar por swipe
        val libView = libraryViewInstance ?: return false
        val rv = libView.findViewById<RecyclerView>(R.id.rv_library_apps) ?: return false
        return rv.canScrollVertically(-1)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val folderOverlay = findViewById<View>(R.id.cl_folder_overlay)
        if (folderOverlay.visibility == View.VISIBLE) {
            return super.dispatchTouchEvent(ev)
        }
        if (ev.action == MotionEvent.ACTION_DOWN) {
            lastTouchX = ev.rawX
            lastTouchY = ev.rawY
            swipeStartX = ev.rawX
            swipeStartY = ev.rawY
            swipeTracking = true
            swipeActivated = false
            swipeIsHorizontal = false
            swipeTotalX = 0f
            swipeTotalY = 0f
        }

        if (dragController.isDragging) {
            swipeTracking = false
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    dragController.onDragMove(ev.rawX, ev.rawY)
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Log.d("DragMain", "dispatchTouchEvent: UP/CANCEL ending drag")
                    dragController.onDragEnd(ev.rawX, ev.rawY)
                    gestureController.reset()
                    return true
                }
            }
        }

        if (swipeTracking && !desktopAdapter?.getEditMode().orFalse()) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - swipeStartX
                    val dy = ev.rawY - swipeStartY
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val slop = android.view.ViewConfiguration.get(this).scaledTouchSlop

                    if (!swipeActivated && dist > slop * 2) {
                        val isH = Math.abs(dx) > Math.abs(dy) * 1.5f
                        val isV = Math.abs(dy) > Math.abs(dx) * 1.5f
                        if (isH) {
                            swipeIsHorizontal = true
                            swipeActivated = true
                        } else if (isV) {
                            swipeIsHorizontal = false
                            swipeActivated = true
                        }
                    }
                    if (swipeActivated) {
                        swipeTotalX = dx
                        swipeTotalY = dy
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (swipeActivated) {
                        val triggerDist = android.view.ViewConfiguration.get(this).scaledTouchSlop * 3
                        if (swipeIsHorizontal) {
                            if (swipeTotalX > triggerDist && currentPage == 1) {
                                navigateToPage(0)
                            } else if (swipeTotalX < -triggerDist && currentPage == 0) {
                                navigateToPage(1)
                            }
                        } else {
                            if (swipeTotalY < -triggerDist && currentPage == 1) {
                                navigateToPage(2)
                                if (!isOrbitalDrawer()) {
                                    findViewById<RecyclerView>(R.id.rv_library_apps)?.scrollToPosition(0)
                                    animateDragHandle(R.id.drag_handle_library)
                                    applyLibraryInsets()
                                }
                            } else if (swipeTotalY > triggerDist && currentPage == 2 && !canLibraryScrollUp()) {
                                navigateToPage(1)
                            }
                        }
                    }
                    swipeTracking = false
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun Boolean?.orFalse(): Boolean = this ?: false

    private fun setupGlobalGestures() {
        // O GestureController agora é o único responsável pelos gestos.
        // Ele detecta Swipes e Double Tap via GestureDetector interno.
        ivWallpaper.setOnTouchListener(gestureController)
    }

    private fun applyTheme(theme: AppTheme) {
        // 0. Welcome Overlay
        applyWelcomeOverlayTheme(theme)

        // 1. Wallpaper via Coil (async, não bloqueia)
        if (theme.wallpaper != null) {
            ivWallpaper.load(theme.wallpaper) {
                size(coil.size.ViewSizeResolver(ivWallpaper))
                bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                allowHardware(true)
                crossfade(true)
            }
        } else {
            val fallbackBg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(theme.colors.background, theme.colors.surface)
            )
            ivWallpaper.setImageDrawable(fallbackBg)
        }

        // 2. Dock Fixo Permanente (Sem Fundo)
        llDockContainer?.background = null

        // 2.5. Botão de Confirmação do Modo de Edição
        val llEditConfirm = findViewById<LinearLayout>(R.id.ll_edit_confirm)
        val confirmBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
            setColor(adjustAlpha(theme.colors.surface, 0.90f))
            if (theme.shapes.showBorders) {
                setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.4f))
            }
        }
        llEditConfirm.background = confirmBg
        findViewById<ImageView>(R.id.iv_edit_confirm_icon).setColorFilter(theme.colors.primary)
        // Evitar sobreposição da barra de navegação
        if (llEditConfirm.tag == null) {
            llEditConfirm.tag = true
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(llEditConfirm) { v, insets ->
                val bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
                (v.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.bottomMargin = dpToPx(32).toInt() + bottom
                insets
            }
            llEditConfirm.requestApplyInsets()
        }

        // 3. Barra de Busca Global
        val searchBarBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(theme.shapes.searchBarCornerRadius.toFloat())
            setColor(adjustAlpha(theme.colors.surface, 0.90f))
            if (theme.shapes.showBorders) {
                setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.3f))
            }
        }
        val searchBarContainer = findViewById<LinearLayout>(R.id.ll_global_search_bar)
        searchBarContainer.background = searchBarBg
        etGlobalSearch.setTextColor(theme.colors.textPrimary)
        etGlobalSearch.setHintTextColor(adjustAlpha(theme.colors.textSecondary, 0.5f))
        
        val closeBtnFrame = findViewById<View>(R.id.btn_close_global_search) as? ViewGroup
        val closeIcon = closeBtnFrame?.getChildAt(0) as? ImageView
        closeIcon?.setColorFilter(theme.colors.primary)

        // 4. Se o desktop view estiver inflado, atualiza
        desktopViewInstance?.let { view ->
            updateDesktopClockAndMascot(view, theme)
            refreshDesktopGrid()
        }

        // 4.1. Se o widgets view estiver inflado, atualiza
        widgetsViewInstance?.let { view ->
            setupWidgetsPage(view)
        }

        // 5. Se a biblioteca view estiver inflada, atualiza
        libraryViewInstance?.let { view ->
            if (isOrbitalDrawer()) {
                val orbitView = view.findViewById<com.alisu.alauncher.ui.OrbitDrawerView>(R.id.orbit_drawer_view)
                orbitView?.applyTheme(theme)
                orbitView?.setApps(allAppsList)
                return@let
            }
            val rvLibraryApps = view.findViewById<RecyclerView>(R.id.rv_library_apps) ?: return@let
            val columns = try {
                resources.getInteger(R.integer.app_drawer_grid_columns)
            } catch (_: Exception) {
                4
            }
            
            val lm = rvLibraryApps.layoutManager as? GridLayoutManager
            if (lm == null || lm.spanCount != columns) {
                rvLibraryApps.layoutManager = GridLayoutManager(this, columns)
            }
            
            val libraryTheme = theme.copy(
                layout = theme.layout.copy(showAppName = getShowAppName(theme))
            )
            libraryAdapter?.updateTheme(libraryTheme)
            libraryAdapter?.updateData(allAppsList)
            libraryAdapter?.refresh()

            val etSearch = view.findViewById<EditText>(R.id.et_library_search)
            val llSearchBar = view.findViewById<LinearLayout>(R.id.ll_library_search_bar)
            val ivSearchIcon = view.findViewById<ImageView>(R.id.iv_library_search_icon)
            val ivClearSearch = view.findViewById<ImageView>(R.id.iv_library_clear_search)
            llSearchBar.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.searchBarCornerRadius.toFloat())
                setColor(adjustAlpha(theme.colors.surface, 0.7f))
                if (theme.shapes.showBorders) {
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.2f))
                }
            }
            etSearch.setTextColor(theme.colors.textPrimary)
            etSearch.setHintTextColor(adjustAlpha(theme.colors.textSecondary, 0.5f))
            ivSearchIcon.setColorFilter(theme.colors.primary)
            ivClearSearch.imageTintList = android.content.res.ColorStateList.valueOf(theme.colors.textSecondary)
            
            if (view.tag == "initialized") return@let
            
            setupAppLibrary(view)
        }

        // 6. Atualizar Dock e Bolinhas Indicadoras (deferred para não bloquear UI)
        rootLayout.post {
            setupPageIndicator()
            setupDockAppsGrid()
            dockAdapter?.refresh()
        }
        
        // 7. Atualizar resultados de busca global (deferred)
        rootLayout.post {
            globalSearchAdapter?.updateTheme(theme)
            globalSearchAdapter?.refresh()
            tvGlobalSearchEmpty.setTextColor(theme.colors.textSecondary)
        }
    }

    private fun showDesktopOptionsDialog(theme: AppTheme) {
        val context = this
        val builder = AlertDialog.Builder(context)
        
        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
            
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                setColor(theme.colors.surface)
                if (theme.shapes.showBorders) {
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.3f))
                }
            }
            background = bg
        }
        
        val tvTitle = TextView(context).apply {
            text = getString(R.string.desktop)
            textSize = 20f
            setTextColor(theme.colors.textPrimary)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(16f).toInt())
        }
        dialogLayout.addView(tvTitle)
        
        // Botão Organizar Ícones
        val btnEditOpt = createSettingRow(context, getString(R.string.organize_grid), getString(R.string.organize_grid_sub), R.drawable.ic_settings, theme).apply {
            setOnClickHaptic {
                alertDialog?.dismiss()
                enableEditMode()
            }
        }
        dialogLayout.addView(btnEditOpt)

        // Botão Temas
        val btnThemesOpt = createSettingRow(context, getString(R.string.look_themes), getString(R.string.look_themes_sub), R.drawable.ic_palette, theme).apply {
            setOnClickHaptic {
                alertDialog?.dismiss()
                showThemeSelectorDialog(theme)
            }
        }
        dialogLayout.addView(btnThemesOpt)
        
        // Botão Ajustes de Layout
        val btnSettingsOpt = createSettingRow(context, getString(R.string.layout_settings), getString(R.string.layout_settings_sub), R.drawable.ic_settings, theme).apply {
            setOnClickHaptic {
                alertDialog?.dismiss()
                val intent = Intent(context, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
        dialogLayout.addView(btnSettingsOpt)
        
        val btnClose = TextView(context).apply {
            text = getString(R.string.close)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(theme.colors.primary)
            gravity = Gravity.END
            setPadding(0, dpToPx(16), dpToPx(8), 0)
            isClickable = true
            isFocusable = true
            setOnClickHaptic {
                alertDialog?.dismiss()
            }
        }
        dialogLayout.addView(btnClose)
        
        builder.setView(dialogLayout)
        val dialog = builder.create()
        alertDialog = dialog
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showThemeSelectorDialog(theme: AppTheme) {
        val intent = Intent(this, com.alisu.alauncher.theme.ThemeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun showSettingsDialog(theme: AppTheme) {
        val context = this
        val builder = AlertDialog.Builder(context)
        
        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
            
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                setColor(theme.colors.surface)
                if (theme.shapes.showBorders) {
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.3f))
                }
            }
            background = bg
        }
        
        val tvTitle = TextView(context).apply {
            text = getString(R.string.launcher_settings)
            textSize = 20f
            setTextColor(theme.colors.textPrimary)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(16f).toInt())
        }
        dialogLayout.addView(tvTitle)

        // Opção 1: Mostrar Nomes dos Apps
        val showNames = getShowAppName(theme)
        val namesLayout = createSettingRow(context, getString(R.string.show_names), if (showNames) getString(R.string.yes) else getString(R.string.no), R.drawable.ic_settings, theme)
        namesLayout.setOnClickHaptic {
            prefs.edit().putBoolean("show_app_name", !showNames).apply()
            
            applyTheme(ThemeManager.currentTheme.value)
            alertDialog?.dismiss()
            showSettingsDialog(ThemeManager.currentTheme.value)
        }
        dialogLayout.addView(namesLayout)

        // Opção 2: Exibir Clima
        val showMascot = getShowMascot()
        val mascotLayout = createSettingRow(context, getString(R.string.show_weather), if (showMascot) getString(R.string.enabled) else getString(R.string.disabled), R.drawable.ic_cloud, theme)
        mascotLayout.setOnClickHaptic {
            prefs.edit().putBoolean("show_mascot", !showMascot).apply()
            
            applyTheme(ThemeManager.currentTheme.value)
            alertDialog?.dismiss()
            showSettingsDialog(ThemeManager.currentTheme.value)
        }
        dialogLayout.addView(mascotLayout)
        
        val btnClose = TextView(context).apply {
            text = getString(R.string.close)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(theme.colors.primary)
            gravity = Gravity.END
            setPadding(0, dpToPx(16), dpToPx(8), 0)
            isClickable = true
            isFocusable = true
            setOnClickHaptic {
                alertDialog?.dismiss()
            }
        }
        dialogLayout.addView(btnClose)
        
        builder.setView(dialogLayout)
        val dialog = builder.create()
        alertDialog = dialog
        
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun createSettingRow(
        context: Context, 
        title: String, 
        value: String, 
        iconRes: Int, 
        theme: AppTheme
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(8))
            }
            
            val itemBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
                setColor(adjustAlpha(theme.colors.surface, 0.6f))
                if (theme.shapes.showBorders) {
                    setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.textSecondary, 0.15f))
                }
            }
            background = itemBg
            isClickable = true
            isFocusable = true
            
            val ivIcon = ImageView(context).apply {
                setImageResource(iconRes)
                layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20)).apply {
                    setMargins(0, 0, dpToPx(12), 0)
                }
                setColorFilter(theme.colors.primary)
            }
            addView(ivIcon)
            
            val tvTitle = TextView(context).apply {
                text = title
                textSize = 15f
                setTextColor(theme.colors.textPrimary)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(tvTitle)
            
            val tvValue = TextView(context).apply {
                text = value
                textSize = 14f
                setTextColor(theme.colors.primary)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(dpToPx(8), 0, 0, 0)
            }
            addView(tvValue)
        }
    }

    private fun getColumns(theme: AppTheme): Int {
        val defaultCols = try {
            resources.getInteger(R.integer.desktop_grid_columns)
        } catch (_: Exception) {
            theme.layout.columns
        }
        return prefs.getInt("grid_columns", defaultCols)
    }

    private fun getShowAppName(theme: AppTheme): Boolean {
        return prefs.getBoolean("show_app_name", theme.layout.showAppName)
    }

    internal fun getShowMascot(): Boolean {
        return prefs.getBoolean("show_mascot", true)
    }

    private fun getDockColumns(): Int {
        return try {
            resources.getInteger(R.integer.dock_grid_columns)
        } catch (_: Exception) {
            4
        }
    }

    internal fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    internal fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getPopupBackground(theme: AppTheme): android.graphics.drawable.Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
            setColor(adjustAlpha(theme.colors.surface, 0.92f))
            if (theme.shapes.showBorders) {
                setStroke(dpToPx(1f).toInt(), adjustAlpha(theme.colors.primary, 0.3f))
            }
        }
    }

    internal fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    internal fun fetchWeatherData() {
        val showWeather = prefs.getBoolean("show_mascot", true)
        if (!showWeather) return

        val now = System.currentTimeMillis()
        if (now - lastWeatherFetchTime < 30 * 60 * 1000) return

        val cachedTemp = prefs.getInt("last_weather_temp", Int.MIN_VALUE)
        if (cachedTemp != Int.MIN_VALUE) {
            desktopViewInstance?.let { view ->
                updateDesktopClockAndMascot(view, ThemeManager.currentTheme.value)
            }
        }

        val location = prefs.getString("weather_location", "Sao Paulo") ?: "Sao Paulo"
        val apiKey = "8c37f920b06a4386ad9142920252711"
        val urlString = "https://api.weatherapi.com/v1/current.json?key=$apiKey&q=${java.net.URLEncoder.encode(location, "UTF-8")}&lang=pt"

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val respCode = connection.responseCode
                if (respCode == 200) {
                    lastWeatherFetchTime = now
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    val current = json.getJSONObject("current")
                    val temp = current.optDouble("temp_c", 0.0)
                    val condition = current.getJSONObject("condition")
                    val condText = condition.optString("text", "")
                    var iconUrl = condition.optString("icon", "")
                    if (iconUrl.startsWith("//")) {
                        iconUrl = "https:$iconUrl"
                    }
                    iconUrl = iconUrl.replace("/64x64/", "/128x128/")

                    prefs.edit()
                        .putInt("last_weather_temp", temp.toInt())
                        .putString("last_weather_icon", iconUrl)
                        .putString("last_weather_condition", condText)
                        .apply()

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        desktopViewInstance?.let { view ->
                            updateDesktopClockAndMascot(view, ThemeManager.currentTheme.value)
                        }
                    }
                } else {
                    android.util.Log.e("Alauncher", "Erro ao carregar clima: HTTP $respCode")
                }
            } catch (e: Exception) {
                android.util.Log.e("Alauncher", "Erro de conexão ao buscar clima", e)
            }
        }
    }


    internal fun View.setOnClickHaptic(action: () -> Unit) {
        this.setOnClickListener { v ->
            action()
        }
    }

    internal fun View.addRipple(rippleColor: Int = 0x20FFFFFF) {
        val currentBg = this.background ?: return
        val rippleColorStateList = android.content.res.ColorStateList.valueOf(rippleColor)
        val rippleDrawable = android.graphics.drawable.RippleDrawable(rippleColorStateList, currentBg, currentBg)
        this.background = rippleDrawable
    }
}