package com.alisu.alauncher

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.alisu.alauncher.theme.AppTheme
import com.alisu.alauncher.theme.ThemeDrawableLoader
import com.alisu.alauncher.theme.ThemeManager

fun MainActivity.applyMusicPlayerTheme(
    theme: AppTheme,
    cardMusicPlayer: FrameLayout?,
    ivBackdrop: ImageView?,
    tvMusicPlayerTitle: TextView?,
    tvMusicPlayerStatus: TextView?,
    tvSongTitle: TextView?,
    tvSongArtist: TextView?,
    btnPlayPause: FrameLayout?,
    ivPlayPause: ImageView?,
    ivPrev: ImageView?,
    ivNext: ImageView?,
    ivAlbumArt: ImageView?,
    tvCurrentTime: TextView?,
    tvTotalTime: TextView?,
    viewProgressFill: View?,
    viewProgressThumb: View?,
    ivCompactToggle: ImageView? = null,
    ivQueue: ImageView? = null
) {
    val surfaceColor = theme.colors.surface
    val primaryColor = theme.colors.primary
    val textPrimary = theme.colors.textPrimary
    val textSecondary = theme.colors.textSecondary
    val bgColor = theme.colors.background

    cardMusicPlayer?.let { card ->
        val hasBackdrop = theme.altBackdrop != null && ivBackdrop != null
        if (hasBackdrop) {
            card.setPadding(0, 0, 0, 0)
            ivBackdrop!!.visibility = View.VISIBLE
            ivBackdrop!!.setImageURI(null)
            ivBackdrop!!.load(theme.altBackdrop) {
                crossfade(300)
                memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                diskCachePolicy(coil.request.CachePolicy.DISABLED)
            }
        } else {
            ivBackdrop?.visibility = View.GONE
        }

        val radius = dpToPx(theme.shapes.cardCornerRadius.toFloat())
        val innerCard = (card as? ViewGroup)?.let { vg ->
            for (i in 0 until vg.childCount) {
                val child = vg.getChildAt(i)
                if (child is android.widget.LinearLayout) return@let child
            }
            null
        } ?: (card as? ViewGroup)?.getChildAt(0) as? View
        val targetView = innerCard ?: card
        var textureApplied = false

        if (!hasBackdrop) {
            var texturePath = theme.cardXmlPath
            if (texturePath == null && theme.texturePath != null) {
                val texturesDir = java.io.File(card.context.filesDir, "themes/${theme.id}")
                val textureFile = java.io.File(texturesDir, theme.texturePath)
                if (textureFile.exists()) {
                    texturePath = "file://${textureFile.absolutePath}"
                }
            }
            if (texturePath != null) {
                val drawable = ThemeDrawableLoader.loadDrawable(card.context, texturePath)
                if (drawable != null) {
                    val layerDrawable = android.graphics.drawable.LayerDrawable(arrayOf(
                        drawable,
                        GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = radius
                            setColor(adjustAlpha(bgColor, 0.70f))
                        }
                    ))
                    targetView.background = layerDrawable
                    textureApplied = true
                }
            }

            if (!textureApplied && theme.cardTexture != null) {
                val resId = card.context.resources.getIdentifier(theme.cardTexture, "drawable", card.context.packageName)
                if (resId != 0) {
                    val textureDrawable = androidx.core.content.ContextCompat.getDrawable(card.context, resId)
                    if (textureDrawable != null) {
                        val layerDrawable = android.graphics.drawable.LayerDrawable(arrayOf(
                            textureDrawable,
                            GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = radius
                                setColor(adjustAlpha(bgColor, 0.70f))
                            }
                        ))
                        targetView.background = layerDrawable
                        textureApplied = true
                    }
                }
            }
        }

        if (!textureApplied) {
            if (hasBackdrop) {
                targetView.background = android.graphics.drawable.ColorDrawable(0x99000000.toInt())
            } else {
                val cardBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    colors = intArrayOf(
                        adjustAlpha(surfaceColor, 1.2f),
                        adjustAlpha(surfaceColor, 0.9f),
                        adjustAlpha(bgColor, 0.8f)
                    )
                    orientation = GradientDrawable.Orientation.TL_BR
                    if (theme.shapes.showBorders) {
                        setStroke(dpToPx(1f).toInt(), adjustAlpha(primaryColor, 0.2f))
                    }
                }
                targetView.background = cardBg
            }
        }

        targetView.clipToOutline = true
        targetView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, radius)
            }
        }

        card.clipToOutline = true
        card.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, radius)
            }
        }
    }

    tvMusicPlayerTitle?.setTextColor(adjustAlpha(textSecondary, 0.7f))
    tvMusicPlayerStatus?.setTextColor(primaryColor)

    tvSongTitle?.setTextColor(textPrimary)
    tvSongArtist?.setTextColor(adjustAlpha(textSecondary, 0.6f))
    tvCurrentTime?.setTextColor(adjustAlpha(textSecondary, 0.5f))
    tvTotalTime?.setTextColor(adjustAlpha(textSecondary, 0.5f))

    ivAlbumArt?.let { art ->
        val albumBorder = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = intArrayOf(
                adjustAlpha(surfaceColor, 0.8f),
                adjustAlpha(bgColor, 0.6f)
            )
            orientation = GradientDrawable.Orientation.TL_BR
            setStroke(dpToPx(2f).toInt(), adjustAlpha(primaryColor, 0.3f))
        }
        art.background = albumBorder
        art.clipToOutline = true
        art.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setOval(0, 0, v.width, v.height)
            }
        }
    }

    btnPlayPause?.let { btn ->
        val playBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = intArrayOf(
                primaryColor,
                adjustAlpha(primaryColor, 0.7f),
                adjustAlpha(primaryColor, 0.3f)
            )
            orientation = GradientDrawable.Orientation.TL_BR
        }
        btn.background = playBg
    }
    ivPlayPause?.setColorFilter(Color.WHITE)

    val controlBtnBg = {
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(adjustAlpha(surfaceColor, 0.5f))
            setStroke(dpToPx(1f).toInt(), adjustAlpha(primaryColor, 0.15f))
        }
    }
    ivPrev?.let {
        it.parent?.let { parent -> (parent as? View)?.background = controlBtnBg() }
        it.setImageResource(R.drawable.ic_music_skip_prev)
        it.setColorFilter(textPrimary)
    }
    ivNext?.let {
        it.parent?.let { parent -> (parent as? View)?.background = controlBtnBg() }
        it.setImageResource(R.drawable.ic_music_skip_next)
        it.setColorFilter(textPrimary)
    }

    val subtleBtnBg = {
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(adjustAlpha(surfaceColor, 0.3f))
            setStroke(dpToPx(1f).toInt(), adjustAlpha(textSecondary, 0.1f))
        }
    }
    viewProgressFill?.let { fill ->
        val progressBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(6f)
            setColor(adjustAlpha(primaryColor, 0.25f))
        }
        fill.background = progressBg
    }
    viewProgressThumb?.let { thumb ->
        val thumbBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(primaryColor)
        }
        thumb.background = thumbBg
    }

    ivCompactToggle?.setColorFilter(adjustAlpha(textSecondary, 0.5f))
    ivQueue?.let { q ->
        q.setColorFilter(adjustAlpha(textSecondary, 0.5f))
        q.parent?.let { parent -> (parent as? View)?.background = subtleBtnBg() }
    }
}

fun MainActivity.reconnectMediaSession() {
    val views = mediaUiViews ?: return
    val msm = try {
        getSystemService(android.content.Context.MEDIA_SESSION_SERVICE) as? android.media.session.MediaSessionManager
    } catch (e: Exception) { null } ?: return

    val listenerComponent = android.content.ComponentName(applicationContext, MediaNotificationListener::class.java)

    mediaCallback?.let { mediaActiveController?.unregisterCallback(it) }
    mediaActiveController = null

    val sessions = try {
        msm.getActiveSessions(listenerComponent)
    } catch (e: SecurityException) { null }

    if (sessions.isNullOrEmpty()) {
        val t = ThemeManager.currentTheme.value
        views.tvSongTitle.text = getString(R.string.no_music)
        views.tvSongTitle.setTextColor(t.colors.textPrimary)
        views.tvSongArtist.text = getString(R.string.open_music_app)
        views.tvSongArtist.setTextColor(adjustAlpha(t.colors.textSecondary, 0.6f))
        views.tvMusicPlayerStatus.text = getString(R.string.no_session)
        views.tvMusicPlayerStatus.setTextColor(adjustAlpha(t.colors.textSecondary, 0.5f))
        views.ivPlayPause.setImageResource(R.drawable.ic_music_play_premium)
        views.ivPlayPause.setColorFilter(Color.WHITE)
        views.ivAlbumArt.setAlbumArt(t)
        views.viewProgressThumb?.visibility = View.GONE
        views.tvCurrentTime.text = getString(R.string.time_default)
        views.tvTotalTime.text = getString(R.string.time_default)
        views.ivAlbumArt.tag = "stopped"
        views.ivAlbumArt.animate().cancel()
        views.ivAlbumArt.rotation = 0f
        views.cardMusicPlayer?.let { card ->
            card.tag = "stopped"
            card.animate().cancel()
            card.alpha = 1f
        }
        stopMediaProgressTracking()
        return
    }

    val controller = sessions.firstOrNull { it.packageName != packageName }
    if (controller == null) {
        val t = ThemeManager.currentTheme.value
        views.tvSongTitle.text = getString(R.string.no_music)
        views.tvSongTitle.setTextColor(t.colors.textPrimary)
        views.tvSongArtist.text = getString(R.string.open_music_app)
        views.tvSongArtist.setTextColor(adjustAlpha(t.colors.textSecondary, 0.6f))
        views.tvMusicPlayerStatus.text = getString(R.string.no_session)
        views.tvMusicPlayerStatus.setTextColor(adjustAlpha(t.colors.textSecondary, 0.5f))
        views.ivPlayPause.setImageResource(R.drawable.ic_music_play_premium)
        views.ivPlayPause.setColorFilter(Color.WHITE)
        views.ivAlbumArt.setAlbumArt(t)
        views.viewProgressThumb?.visibility = View.GONE
        views.tvCurrentTime.text = getString(R.string.time_default)
        views.tvTotalTime.text = getString(R.string.time_default)
        stopMediaProgressTracking()
        return
    }

    mediaActiveController = controller

    mediaCallback = object : android.media.session.MediaController.Callback() {
        override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
            updateMediaPlayerUI()
            if (state?.state == android.media.session.PlaybackState.STATE_PLAYING) {
                startMediaProgressTracking()
            } else {
                stopMediaProgressTracking()
            }
        }

        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            updateMediaPlayerUI()
        }

        override fun onSessionDestroyed() {
            mediaActiveController = null
            stopMediaProgressTracking()
            val v = mediaUiViews ?: return
            val t = ThemeManager.currentTheme.value
            v.tvSongTitle.text = getString(R.string.no_music)
            v.tvSongTitle.setTextColor(t.colors.textPrimary)
            v.tvSongArtist.text = getString(R.string.open_music_app)
            v.tvSongArtist.setTextColor(adjustAlpha(t.colors.textSecondary, 0.6f))
            v.tvMusicPlayerStatus.text = getString(R.string.no_session)
            v.tvMusicPlayerStatus.setTextColor(adjustAlpha(t.colors.textSecondary, 0.5f))
            v.ivPlayPause.setImageResource(R.drawable.ic_music_play_premium)
            v.ivPlayPause.setColorFilter(Color.WHITE)
            v.ivAlbumArt.setAlbumArt(t)
            v.viewProgressThumb?.visibility = View.GONE
            v.tvCurrentTime.text = getString(R.string.time_default)
            v.tvTotalTime.text = getString(R.string.time_default)
            v.ivAlbumArt.tag = "stopped"
            v.ivAlbumArt.animate().cancel()
            v.ivAlbumArt.rotation = 0f
            v.cardMusicPlayer?.let { card ->
                card.tag = "stopped"
                card.animate().cancel()
                card.alpha = 1f
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                reconnectMediaSession()
            }, 2000)
        }
    }

    controller.registerCallback(mediaCallback!!, android.os.Handler(android.os.Looper.getMainLooper()))
    updateMediaPlayerUI()

    val state = controller.playbackState
    if (state?.state == android.media.session.PlaybackState.STATE_PLAYING) {
        startMediaProgressTracking()
    }
}

fun MainActivity.formatMediaTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

fun MainActivity.updateMediaPlayerUI() {
    val views = mediaUiViews ?: return
    val controller = mediaActiveController ?: return
    val state = controller.playbackState
    val meta = controller.metadata
    val theme = ThemeManager.currentTheme.value

    applyMusicPlayerTheme(
        theme, views.cardMusicPlayer, views.ivMusicCardBackdrop,
        null, views.tvMusicPlayerStatus, views.tvSongTitle, views.tvSongArtist,
        null, views.ivPlayPause, null, null,
        views.ivAlbumArt, views.tvCurrentTime, views.tvTotalTime,
        views.viewProgressFill, views.viewProgressThumb,
        views.ivCompactToggle, views.ivQueue
    )

    if (state == null) {
        views.ivPlayPause.setImageResource(R.drawable.ic_music_play_premium)
        views.ivPlayPause.setColorFilter(Color.WHITE)
        views.tvMusicPlayerStatus.text = getString(R.string.music_stopped)
        views.tvMusicPlayerStatus.setTextColor(adjustAlpha(theme.colors.textSecondary, 0.5f))
        views.viewProgressThumb?.visibility = View.GONE
        views.ivAlbumArt.setAlbumArt(theme)
        views.ivAlbumArt.tag = "stopped"
        views.ivAlbumArt.animate().cancel()
        views.ivAlbumArt.animate()
            .rotation(0f)
            .setDuration(500)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
        views.cardMusicPlayer?.let { card ->
            card.tag = "stopped"
            card.animate().cancel()
            card.alpha = 1f
        }
        views.flAlbumArt?.let { art ->
            for (i in 0 until art.childCount) {
                val child = art.getChildAt(i)
                if (child is VisualizerView) {
                    child.stopAnimating()
                    break
                }
            }
        }
        stopMediaProgressTracking()
        return
    }

    val isPlaying = state.state == android.media.session.PlaybackState.STATE_PLAYING
    val newIconRes = if (isPlaying) R.drawable.ic_music_pause_premium else R.drawable.ic_music_play_premium

    if (views.ivPlayPause.tag != newIconRes) {
        views.ivPlayPause.animate()
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setDuration(100)
            .withEndAction {
                views.ivPlayPause.setImageResource(newIconRes)
                views.ivPlayPause.setColorFilter(Color.WHITE)
                views.ivPlayPause.tag = newIconRes
                views.ivPlayPause.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                    .start()
            }
            .start()
    } else {
        views.ivPlayPause.setImageResource(newIconRes)
        views.ivPlayPause.setColorFilter(Color.WHITE)
    }

    views.ivAlbumArt.let { album ->
        if (isPlaying) {
            if (album.tag != "rotating") {
                album.tag = "rotating"
                album.animate()
                    .rotationBy(360f)
                    .setDuration(15000)
                    .setInterpolator(android.view.animation.LinearInterpolator())
                    .withEndAction {
                        if (album.tag == "rotating") {
                            album.rotation = 0f
                            album.animate()
                                .rotationBy(360f)
                                .setDuration(15000)
                                .setInterpolator(android.view.animation.LinearInterpolator())
                                .start()
                        }
                    }
                    .start()
            }
            views.flAlbumArt?.let { art ->
                var vis: VisualizerView? = null
                for (i in 0 until art.childCount) {
                    val child = art.getChildAt(i)
                    if (child is VisualizerView) { vis = child; break }
                }
                if (vis == null) {
                    vis = VisualizerView(art.context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        alpha = 0.5f
                    }
                    art.addView(vis)
                }
                vis.startAnimating()
            }
        } else {
            album.tag = "stopped"
            album.animate().cancel()
            album.animate()
                .rotation(0f)
                .setDuration(500)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
            views.flAlbumArt?.let { art ->
                for (i in 0 until art.childCount) {
                    val child = art.getChildAt(i)
                    if (child is VisualizerView) { child.stopAnimating(); break }
                }
            }
        }
    }

    views.cardMusicPlayer?.let { card ->
        if (isPlaying) {
            if (card.tag != "pulsing") {
                card.animate().cancel()
                (card.getTag(R.id.tag_pulse_animator) as? android.animation.ValueAnimator)?.cancel()
                card.tag = "pulsing"
                val pulse = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 2200
                    repeatCount = android.animation.ValueAnimator.INFINITE
                    interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                    addUpdateListener { anim ->
                        val t = anim.animatedFraction
                        val alpha = 1f - 0.10f * (Math.sin(t * Math.PI * 2).toFloat() * 0.5f + 0.5f)
                        card.alpha = alpha.coerceIn(0.85f, 1f)
                    }
                    start()
                }
                card.setTag(R.id.tag_pulse_animator, pulse)
                card.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {}
                    override fun onViewDetachedFromWindow(v: View) {
                        pulse.cancel()
                        v.removeOnAttachStateChangeListener(this)
                    }
                })
            }
        } else {
            card.tag = "stopped"
            (card.getTag(R.id.tag_pulse_animator) as? android.animation.ValueAnimator)?.cancel()
            card.setTag(R.id.tag_pulse_animator, null)
            card.animate().cancel()
            card.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    if (!isPlaying) {
        stopMediaProgressTracking()
    }

    views.viewProgressFill?.let { fill ->
        val progressBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(6f)
            setColor(adjustAlpha(theme.colors.primary, 0.3f))
        }
        fill.background = progressBg
    }
    views.viewProgressThumb?.let { thumb ->
        val thumbBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(theme.colors.primary)
        }
        thumb.background = thumbBg
    }

    if (meta != null) {
        val title = meta.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
        val artist = meta.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
        val newTitle = title ?: getString(R.string.unknown_track)
        val newArtist = artist ?: getString(R.string.unknown_artist)

        if (views.tvSongTitle.text != newTitle) {
            views.tvSongTitle.animate().cancel()
            views.tvSongTitle.animate().alpha(0f).setDuration(150).withEndAction {
                views.tvSongTitle.text = newTitle
                views.tvSongTitle.animate().alpha(1f).setDuration(200).start()
            }.start()
        }
        if (views.tvSongArtist.text != newArtist) {
            views.tvSongArtist.animate().cancel()
            views.tvSongArtist.animate().alpha(0f).setDuration(150).withEndAction {
                views.tvSongArtist.text = newArtist
                views.tvSongArtist.animate().alpha(1f).setDuration(200).start()
            }.start()
        }

        val albumArt = meta.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)

        val trackChanged = views.tvSongTitle.text != newTitle
        views.ivAlbumArt.let { art ->
            if (albumArt != null) {
                if (trackChanged && art.drawable != null) {
                    art.animate().alpha(0f).setDuration(120).withEndAction {
                        art.setImageBitmap(albumArt)
                        art.animate().alpha(1f).setDuration(150).start()
                    }.start()
                } else {
                    art.setImageBitmap(albumArt)
                }
            } else {
                art.setAlbumArt(theme)
            }
        }
    } else {
        views.tvSongTitle.text = getString(R.string.unknown_track)
        views.tvSongArtist.text = getString(R.string.unknown_artist)
    }

    val position = state.position
    val duration = if (meta != null) meta.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) else 0L
    views.tvCurrentTime.text = formatMediaTime(position)
    views.tvTotalTime.text = if (duration > 0) formatMediaTime(duration) else "0:00"

    val progress = if (duration > 0) ((position.toFloat() / duration.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
    views.viewProgressFill.let { fill ->
        val parent = fill.parent as? View
        parent?.post {
            val totalWidth = parent.width
            val targetWidth = totalWidth * progress / 100
            val currentWidth = fill.width
            if (Math.abs(targetWidth - currentWidth) > 2) {
                val animator = android.animation.ValueAnimator.ofInt(currentWidth, targetWidth)
                animator.duration = 300
                animator.interpolator = android.view.animation.DecelerateInterpolator()
                animator.addUpdateListener { anim ->
                    val value = anim.animatedValue as Int
                    val lp = fill.layoutParams
                    lp.width = value
                    fill.layoutParams = lp
                }
                animator.start()
            } else {
                val lp = fill.layoutParams
                lp.width = targetWidth
                fill.layoutParams = lp
            }
            views.viewProgressThumb?.let { thumb ->
                if (thumb.visibility != View.VISIBLE) {
                    thumb.visibility = View.VISIBLE
                    thumb.scaleX = 0f
                    thumb.scaleY = 0f
                    thumb.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(250)
                        .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                        .start()
                }
                val thumbX = (totalWidth * progress / 100).toFloat() - dpToPx(7f)
                if (Math.abs(thumb.translationX - thumbX) > 2f) {
                    thumb.animate()
                        .translationX(thumbX)
                        .setDuration(300)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                } else {
                    thumb.translationX = thumbX
                }
            }
        }
    }

    views.tvMusicPlayerStatus.text = if (isPlaying) getString(R.string.playing)
    else if (state.state == android.media.session.PlaybackState.STATE_PAUSED) getString(R.string.paused)
    else getString(R.string.stopped)
    views.tvMusicPlayerStatus.setTextColor(if (isPlaying) theme.colors.primary else adjustAlpha(theme.colors.textSecondary, 0.5f))
}

fun MainActivity.startMediaProgressTracking() {
    stopMediaProgressTracking()
    mediaProgressHandler = android.os.Handler(android.os.Looper.getMainLooper())
    mediaProgressRunnable = object : Runnable {
        override fun run() {
            val controller = mediaActiveController
            val state = controller?.playbackState
            if (state?.state == android.media.session.PlaybackState.STATE_PLAYING) {
                updateMediaPlayerUI()
                mediaProgressHandler?.postDelayed(this, 500)
            } else {
                stopMediaProgressTracking()
                updateMediaPlayerUI()
            }
        }
    }
    mediaProgressHandler?.postDelayed(mediaProgressRunnable!!, 500)
}

private fun ImageView.setAlbumArt(theme: AppTheme) {
    if (theme.character != null) {
        setImageBitmap(theme.character)
    } else {
        setImageResource(R.drawable.bg_music_album_placeholder)
    }
}

fun MainActivity.stopMediaProgressTracking() {
    mediaProgressRunnable?.let { mediaProgressHandler?.removeCallbacks(it) }
    mediaProgressRunnable = null
}

fun MainActivity.sendMediaTransportCommand(action: Int) {
    val controller = mediaActiveController ?: return
    when (action) {
        0 -> controller.transportControls.play()
        1 -> controller.transportControls.pause()
        2 -> controller.transportControls.skipToPrevious()
        3 -> controller.transportControls.skipToNext()
    }
    if (action == 2 || action == 3) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val ctrl = mediaActiveController ?: return@postDelayed
            if (ctrl.playbackState?.state != android.media.session.PlaybackState.STATE_PLAYING) {
                ctrl.transportControls.play()
            }
        }, 400)
    }
}

fun MainActivity.showMusicQueueDialog() {
    val controller = mediaActiveController ?: return
    val queue = controller.queue ?: return
    if (queue.isEmpty()) return

    val theme = ThemeManager.currentTheme.value
    val density = resources.displayMetrics.density
    val cornerPx = (theme.shapes.cardCornerRadius * density).toInt()

    val items = queue.mapIndexed { index, item ->
        "${item.description.title ?: "Unknown"}"
    }.toTypedArray()

    val dialog = android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
    val container = FrameLayout(this).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setOnClickListener { dialog.dismiss() }
    }

    val cardBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = cornerPx.toFloat()
        setColor(adjustAlpha(theme.colors.surface, 0.95f))
        if (theme.shapes.showBorders) setStroke((1f * density).toInt(), adjustAlpha(theme.colors.primary, 0.15f))
    }

    val card = FrameLayout(this).apply {
        layoutParams = FrameLayout.LayoutParams(
            (320f * density).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = android.view.Gravity.CENTER }
        setPadding((16f * density).toInt(), (16f * density).toInt(), (16f * density).toInt(), (8f * density).toInt())
        background = cardBg
        elevation = (8f * density)
    }

    val innerLayout = android.widget.LinearLayout(this).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    TextView(this).apply {
        text = getString(R.string.music_queue_title)
        textSize = 16f
        setTextColor(theme.colors.textPrimary)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        layoutParams = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, (12f * density).toInt()) }
        innerLayout.addView(this)
    }

    val listView = android.widget.ListView(this).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        divider = null
        dividerHeight = 0
        adapter = object : android.widget.BaseAdapter() {
            private var cachedBg: GradientDrawable? = null
            override fun getCount() = items.size
            override fun getItem(pos: Int) = items[pos]
            override fun getItemId(pos: Int) = pos.toLong()
            override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
                val itemBg = cachedBg ?: GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = (cornerPx * 0.6f).coerceAtLeast(6f)
                    setColor(adjustAlpha(theme.colors.primary, 0.08f))
                }.also { cachedBg = it }
                val tv = (convertView as? TextView) ?: TextView(this@showMusicQueueDialog).apply {
                    textSize = 14f
                    maxLines = 1
                    layoutParams = android.widget.AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                tv.text = items[pos]
                tv.setTextColor(theme.colors.textPrimary)
                tv.background = itemBg
                val topPad = if (pos > 0) (14f * density).toInt() else (10f * density).toInt()
                tv.setPadding((12f * density).toInt(), topPad, (12f * density).toInt(), (10f * density).toInt())
                return tv
            }
        }
        setOnItemClickListener { _, _, pos, _ ->
            controller.transportControls.skipToQueueItem(queue[pos].queueId)
            dialog.dismiss()
        }
    }
    innerLayout.addView(listView)

    // Close button
    TextView(this).apply {
        text = getString(R.string.close)
        textSize = 14f
        setTextColor(theme.colors.primary)
        gravity = android.view.Gravity.CENTER_HORIZONTAL
        setPadding(0, (12f * density).toInt(), 0, (4f * density).toInt())
        layoutParams = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setOnClickListener { dialog.dismiss() }
        innerLayout.addView(this)
    }

    // Se a fila for muito longa, limita altura
    if (items.size > 8) {
        listView.layoutParams = listView.layoutParams.apply {
            height = (350f * density).toInt()
        }
    }

    card.addView(innerLayout)
    container.addView(card)
    dialog.setContentView(container)
    dialog.show()
}

class VisualizerView(context: android.content.Context) : View(context) {
    private val barCount = 7
    private val barHeights = FloatArray(barCount) { 0f }
    private val phases = FloatArray(barCount) { i -> i * 0.9f }
    private var time = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 120
        style = Paint.Style.FILL
    }
    private var isAnimating = false

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (height > 0) {
                time += 0.15f
                for (i in barHeights.indices) {
                    val wave = (Math.sin((time + phases[i]).toDouble()) * 0.5 + 0.5).toFloat()
                    barHeights[i] = wave * height * 0.65f + height * 0.1f
                }
                invalidate()
            }
            if (isAnimating) handler.postDelayed(this, 80)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!isAnimating || height <= 0 || width <= 0) return
        val barWidth = (width.toFloat() / (barCount * 2 + 1)).coerceAtLeast(3f)
        val gap = barWidth
        val halfW = barWidth / 2f
        for (i in barHeights.indices) {
            val cx = gap + i * (barWidth + gap) + halfW
            val barHeight = barHeights[i].coerceIn(height * 0.05f, height * 0.85f)
            canvas.drawRoundRect(
                cx - halfW, height - barHeight,
                cx + halfW, height.toFloat(),
                halfW, halfW, paint
            )
        }
    }

    override fun onDetachedFromWindow() {
        stopAnimating()
        super.onDetachedFromWindow()
    }

    fun startAnimating() {
        if (isAnimating) return
        time = 0f
        isAnimating = true
        handler.post(updateRunnable)
    }

    fun stopAnimating() {
        isAnimating = false
        handler.removeCallbacks(updateRunnable)
        for (i in barHeights.indices) barHeights[i] = 0f
        invalidate()
    }

    fun destroy() {
        stopAnimating()
        handler.removeCallbacksAndMessages(null)
    }
}
