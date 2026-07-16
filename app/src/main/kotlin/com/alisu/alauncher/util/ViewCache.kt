package com.alisu.alauncher.util

import android.util.SparseArray
import android.view.View

class ViewCache(private val maxSize: Int = 16) {

    private class CacheEntry(val maxSize: Int) {
        val views: Array<View?> = arrayOfNulls(maxSize)
        var count: Int = 0
    }

    private val cache = SparseArray<CacheEntry>()

    fun getView(layoutId: Int): View? {
        val entry = cache.get(layoutId) ?: return null
        if (entry.count > 0) {
            val idx = entry.count - 1
            val view = entry.views[idx]
            entry.views[idx] = null
            entry.count--
            return view
        }
        return null
    }

    fun recycleView(layoutId: Int, view: View) {
        var entry = cache.get(layoutId)
        if (entry == null) {
            entry = CacheEntry(maxSize)
            cache.put(layoutId, entry)
        }
        if (entry.count < maxSize) {
            entry.views[entry.count] = view
            entry.count++
        }
    }

    fun clear() {
        for (i in 0 until cache.size()) {
            val entry = cache.valueAt(i)
            for (j in 0 until entry.count) {
                entry.views[j] = null
            }
            entry.count = 0
        }
        cache.clear()
    }
}
