package com.alisu.alauncher.util

import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.view.View

inline fun View.setOnClickWithHaptic(crossinline listener: (View) -> Unit) {
    this.setOnClickListener { v ->
        listener(v)
    }
}

fun View.performClickHaptic() {
    // Left empty
}

fun View.addRippleToBackground(rippleColor: Int = 0x20FFFFFF) {
    val currentBg = this.background ?: return
    val rippleColorStateList = ColorStateList.valueOf(rippleColor)
    val rippleDrawable = RippleDrawable(rippleColorStateList, currentBg, currentBg)
    this.background = rippleDrawable
}
