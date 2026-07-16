package com.alisu.alauncher.gesture

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import com.alisu.alauncher.R
import com.alisu.alauncher.drag.DragController
import com.alisu.alauncher.model.AppInfo

class GestureController(
    private val context: Context,
    private val dragController: DragController,
    private val callbacks: Callbacks
) : View.OnTouchListener {

    interface Callbacks {
        fun onLongPressOnEmpty(rawX: Float, rawY: Float)
        fun onLongPressOnIcon(view: View, item: AppInfo)
        fun onSwipeUp()
        fun onSwipeDown()
        fun onSwipeLeft()
        fun onSwipeRight()
        fun onDoubleTap()
        fun isEditMode(): Boolean
        fun isAllAppsOpen(): Boolean
        fun canOpenAllApps(): Boolean
        fun isDesktopPage(): Boolean
        fun isWidgetsPage(): Boolean
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val handler = Handler(Looper.getMainLooper())

    private val velocityTracker = com.alisu.alauncher.gesture.VelocityTracker(context)
    private val allAppsSwipe = AllAppsSwipeController(context, object : AllAppsSwipeController.Callback {
        override fun onSwipeUp() = callbacks.onSwipeUp()
        override fun onSwipeDown() = callbacks.onSwipeDown()
        override fun onSwipeLeft() = callbacks.onSwipeLeft()
        override fun onSwipeRight() = callbacks.onSwipeRight()
        override fun isAllAppsOpen() = callbacks.isAllAppsOpen()
        override fun canOpenAllApps() = callbacks.canOpenAllApps()
        override fun isDesktopPage() = callbacks.isDesktopPage()
        override fun isWidgetsPage() = callbacks.isWidgetsPage()
    })

    var state = TouchState.IDLE
        private set
    private var downX = 0f
    private var downY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastMoveRawX = 0f
    private var lastMoveRawY = 0f
    private var dragTargetView: View? = null
    private var isDragCandidate = false
    private var pointerCount = 1
    private var doubleTapPending = false
    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f

    private val longPressTimeout = (ViewConfiguration.getLongPressTimeout() * 0.75f).toLong()

    private val longPressRunnable = Runnable {
        Log.d("Gesture", "longPressRunnable: state=$state isDragCandidate=$isDragCandidate isEditMode=${callbacks.isEditMode()}")
        if (state != TouchState.CLICKING) return@Runnable

        val view = dragTargetView
        view?.isPressed = false
        if (view is ViewGroup) view.findViewById<View>(R.id.fl_icon_container)?.isPressed = false
        view?.animate()?.scaleX(1.0f)?.scaleY(1.0f)
            ?.setDuration(180)
            ?.setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
            ?.start()
        val item = view?.tag as? AppInfo

        if (view != null && isDragCandidate && item != null && callbacks.isEditMode()) {
            Log.d("Gesture", "longPressRunnable: START DRAG via longPress")
            state = TouchState.DRAGGING
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            requestDisallowIntercept(view)
            dragController.onDragStart(view, lastMoveRawX, lastMoveRawY)
        } else if (view != null && isDragCandidate && item != null) {
            Log.d("Gesture", "longPressRunnable: SHOW POPUP")
            state = TouchState.POPUP
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            callbacks.onLongPressOnIcon(view, item)
        } else {
            Log.d("Gesture", "longPressRunnable: EMPTY AREA")
            state = TouchState.IDLE
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            callbacks.onLongPressOnEmpty(lastMoveRawX, lastMoveRawY)
        }
    }

    fun setDragCandidate(draggable: Boolean, view: View? = null) {
        isDragCandidate = draggable
        if (view != null) dragTargetView = view
    }

    fun onPopupDismissed() {
        Log.d("Gesture", "onPopupDismissed: $state -> IDLE")
        state = TouchState.IDLE
    }

    fun onPopupOpened() {
        Log.d("Gesture", "onPopupOpened: $state -> POPUP")
        state = TouchState.POPUP
    }

    fun reset() {
        Log.d("Gesture", "reset: state=$state dragging=${dragController.isDragging}")
        handler.removeCallbacks(longPressRunnable)
        if (state == TouchState.DRAGGING && dragController.isDragging) {
            dragController.onDragCancel()
        }
        state = TouchState.IDLE
        dragTargetView = null
        isDragCandidate = false
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        velocityTracker.addMovement(event)

        if (state == TouchState.DRAGGING) {
            if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                Log.d("Gesture", "onTouch: CANCEL during DRAG -> endDrag")
                dragController.onDragEnd(lastMoveRawX, lastMoveRawY)
                state = TouchState.IDLE
                v.isPressed = false
                if (v is ViewGroup) v.findViewById<View>(R.id.fl_icon_container)?.isPressed = false
                v.animate().scaleX(1.0f).scaleY(1.0f)
                    .setDuration(180)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                    .start()
            }
            return true
        }

        if (!callbacks.isEditMode()) {
            allAppsSwipe.processEvent(event)
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDown(v, event)
            MotionEvent.ACTION_POINTER_DOWN -> pointerCount = event.pointerCount
            MotionEvent.ACTION_MOVE -> handleMove(v, event)
            MotionEvent.ACTION_UP -> handleUp(v, event)
            MotionEvent.ACTION_CANCEL -> {
                Log.d("Gesture", "onTouch: CANCEL")
                cancelLongPress()
                reset()
                v.isPressed = false
                if (v is ViewGroup) v.findViewById<View>(R.id.fl_icon_container)?.isPressed = false
                v.animate().scaleX(1.0f).scaleY(1.0f)
                    .setDuration(180)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                    .start()
                return false
            }
        }
        return state != TouchState.IDLE
    }

    private fun handleDown(v: View, event: MotionEvent) {
        if (state == TouchState.POPUP) return

        val now = System.currentTimeMillis()
        val dx = event.rawX - lastTapX
        val dy = event.rawY - lastTapY
        if (now - lastTapTime < 300 && dx * dx + dy * dy < touchSlop * touchSlop * 4) {
            doubleTapPending = true
            lastTapTime = 0
            callbacks.onDoubleTap()
            return
        }
        lastTapTime = now
        lastTapX = event.rawX
        lastTapY = event.rawY

        state = TouchState.CLICKING
        downX = event.x
        downY = event.y
        downRawX = event.rawX
        downRawY = event.rawY
        lastMoveRawX = event.rawX
        lastMoveRawY = event.rawY
        dragTargetView = v
        pointerCount = 1

        Log.d("Gesture", "handleDown: CLICKING isDragCandidate=$isDragCandidate edit=${callbacks.isEditMode()} dragging=${dragController.isDragging}")
        
        v.isPressed = true
        if (v is ViewGroup) v.findViewById<View>(R.id.fl_icon_container)?.isPressed = true
        v.animate().scaleX(0.92f).scaleY(0.92f)
            .setDuration(120)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            v.background?.setHotspot(event.x, event.y)
            v.foreground?.setHotspot(event.x, event.y)
            if (v is ViewGroup) {
                val iconContainer = v.findViewById<View>(R.id.fl_icon_container)
                if (iconContainer != null) {
                    val relX = event.x - iconContainer.left
                    val relY = event.y - iconContainer.top
                    iconContainer.background?.setHotspot(relX, relY)
                }
            }
        }
        
        handler.postDelayed(longPressRunnable, longPressTimeout)
    }

    private fun handleMove(v: View, event: MotionEvent) {
        if (doubleTapPending) return

        lastMoveRawX = event.rawX
        lastMoveRawY = event.rawY

        val dx = event.x - downX
        val dy = event.y - downY
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (state == TouchState.CLICKING && dist > touchSlop) {
            cancelLongPress()
            v.isPressed = false
            if (v is ViewGroup) v.findViewById<View>(R.id.fl_icon_container)?.isPressed = false
            v.animate().scaleX(1.0f).scaleY(1.0f)
                .setDuration(180)
                .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                .start()
            val isEditMode = callbacks.isEditMode()
            Log.d("Gesture", "handleMove: past slop edit=$isEditMode candidate=$isDragCandidate")
            if (isEditMode && isDragCandidate && !dragController.isDragging) {
                state = TouchState.DRAGGING
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                requestDisallowIntercept(v)
                dragController.onDragStart(v, event.rawX, event.rawY)
            } else {
                state = TouchState.SCROLLING
            }
        } else if (state == TouchState.CLICKING) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                v.background?.setHotspot(event.x, event.y)
                v.foreground?.setHotspot(event.x, event.y)
                if (v is ViewGroup) {
                    val iconContainer = v.findViewById<View>(R.id.fl_icon_container)
                    if (iconContainer != null) {
                        val relX = event.x - iconContainer.left
                        val relY = event.y - iconContainer.top
                        iconContainer.background?.setHotspot(relX, relY)
                    }
                }
            }
        }
    }

    private fun handleUp(v: View, event: MotionEvent) {
        if (doubleTapPending) {
            doubleTapPending = false
            return
        }

        v.isPressed = false
        if (v is ViewGroup) v.findViewById<View>(R.id.fl_icon_container)?.isPressed = false
        v.animate().scaleX(1.0f).scaleY(1.0f)
            .setDuration(180)
            .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
            .start()

        Log.d("Gesture", "handleUp: state=$state")
        when (state) {
            TouchState.CLICKING -> {
                cancelLongPress()
                state = TouchState.IDLE
                v.performClick()
            }
            TouchState.DRAGGING -> {
                dragController.onDragEnd(event.rawX, event.rawY)
                state = TouchState.IDLE
            }
            else -> {
                state = TouchState.IDLE
            }
        }
    }

    private fun cancelLongPress() {
        handler.removeCallbacks(longPressRunnable)
    }

    private fun requestDisallowIntercept(v: View) {
        var parent = v.parent
        while (parent is android.view.ViewParent) {
            parent.requestDisallowInterceptTouchEvent(true)
            parent = (parent as? View)?.parent
        }
    }
}
