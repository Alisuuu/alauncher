package com.alisu.alauncher.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class BackgroundViewPool(
    private val context: Context,
    private val layoutResId: Int,
    private val poolSize: Int = 12
) {
    private val pool = ConcurrentLinkedQueue<View>()
    private val isInitialized = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init() {
        if (isInitialized.getAndSet(true)) return
        Thread {
            val inflater = LayoutInflater.from(context.applicationContext)
            for (i in 0 until poolSize) {
                val view = inflater.inflate(layoutResId, null, false)
                pool.offer(view)
            }
        }.start()
    }

    fun obtain(parent: ViewGroup): View {
        return pool.poll() ?: run {
            val inflater = LayoutInflater.from(parent.context)
            inflater.inflate(layoutResId, parent, false)
        }
    }

    fun recycle(view: View) {
        if (pool.size < poolSize) {
            pool.offer(view)
        }
    }

    fun size(): Int = pool.size
}
