package com.alisu.alauncher.drag

import android.graphics.Bitmap
import android.util.Log
import android.view.View
import com.alisu.alauncher.R
import com.alisu.alauncher.model.AppInfo

class DragController(
    private val dragLayer: DragLayer,
    val dropTargets: MutableList<DropTarget> = mutableListOf(),
    var onDragStartCallback: (() -> Unit)? = null,
    var onDragEndCallback: (() -> Unit)? = null
) {
    private var activeItem: AppInfo? = null
    var sourceView: View? = null
        private set
    private var activeDropTarget: DropTarget? = null
    private var capturedBitmap: Bitmap? = null
    private var dragActive = false

    fun onDragStart(view: View, x: Float, y: Float) {
        Log.d("DragCtrl", "onDragStart: dragActive=$dragActive, view=$view, x=$x, y=$y")
        if (dragActive) {
            Log.w("DragCtrl", "onDragStart BLOCKED - already dragging")
            return
        }
        dragActive = true

        val iconContainer = view.findViewById<View>(R.id.fl_icon_container) ?: view
        val btnDelete = view.findViewById<View>(R.id.btn_delete_app)

        val originalVisibility = btnDelete?.visibility ?: View.GONE
        btnDelete?.visibility = View.GONE

        val bitmap = try {
            val bmp = Bitmap.createBitmap(
                iconContainer.width.coerceAtLeast(1),
                iconContainer.height.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bmp)
            iconContainer.draw(canvas)
            bmp
        } catch (e: Exception) {
            Log.e("DragCtrl", "Bitmap capture failed", e)
            null
        }

        if (btnDelete != null) {
            btnDelete.visibility = originalVisibility
        }

        view.alpha = 0f
        sourceView = view
        activeItem = view.tag as? AppInfo
        capturedBitmap = bitmap

        Log.d("DragCtrl", "onDragStart: item=${activeItem?.packageName}, bitmap=${bitmap != null}, targets=${dropTargets.size}")
        dragLayer.startDrag(bitmap, x, y, iconContainer.width, iconContainer.height)
        var p: android.view.ViewParent? = view.parent
        while (p != null) {
            p.requestDisallowInterceptTouchEvent(true)
            p = (p as? View)?.parent
        }
        for (target in dropTargets) {
            if (target is com.alisu.alauncher.workspace.WorkspaceLayout) {
                target.isDragActive = true
            }
        }
        onDragStartCallback?.invoke()
    }

    fun onDragMove(x: Float, y: Float) {
        dragLayer.moveTo(x, y)

        val item = activeItem
        if (item != null) {
            for (target in dropTargets) {
                if (target is com.alisu.alauncher.workspace.WorkspaceLayout) {
                    target.invalidateLocationCache()
                }
            }
            val newTarget = dropTargets.firstOrNull { it.containsPoint(x, y) && it.accepts(item) }
            if (newTarget != activeDropTarget) {
                Log.d("DragCtrl", "onDragMove: target changed ${activeDropTarget?.javaClass?.simpleName} -> ${newTarget?.javaClass?.simpleName}")
                activeDropTarget?.onDragExit(item)
                newTarget?.onDragEnter(item)
                activeDropTarget = newTarget
            }
            newTarget?.onDragOver(item, x, y)
        }
    }

    fun onDragEnd(x: Float, y: Float) {
        Log.d("DragCtrl", "onDragEnd: dragActive=$dragActive, target=$activeDropTarget, item=${activeItem?.packageName}")
        if (!dragActive) {
            Log.w("DragCtrl", "onDragEnd BLOCKED - not active")
            return
        }
        dragActive = false

        val item = activeItem
        val target = activeDropTarget

        activeItem?.let { target?.onDragExit(it) }
        activeDropTarget = null

        if (target != null && item != null) {
            Log.d("DragCtrl", "onDragEnd: DROP on ${target.javaClass.simpleName}")
            dragLayer.endDrag()
            sourceView?.alpha = 1f
            target.onDrop(item, x, y)
            cleanup()
        } else {
            Log.d("DragCtrl", "onDragEnd: CANCEL (no target)")
            val savedSource = sourceView
            dragLayer.endDrag()
            savedSource?.alpha = 1f
            cleanup()
        }
        onDragEndCallback?.invoke()
    }

    fun onDragCancel() {
        Log.d("DragCtrl", "onDragCancel: dragActive=$dragActive")
        if (!dragActive) {
            Log.w("DragCtrl", "onDragCancel BLOCKED - not active")
            return
        }
        dragActive = false

        sourceView?.alpha = 1f
        dragLayer.endDrag()
        cleanup()
        onDragEndCallback?.invoke()
    }

    private fun cleanup() {
        for (target in dropTargets) {
            if (target is com.alisu.alauncher.workspace.WorkspaceLayout) {
                target.isDragActive = false
            }
        }
        activeItem = null
        sourceView = null
        capturedBitmap?.recycle()
        capturedBitmap = null
    }

    val isDragging: Boolean get() = dragActive
}
