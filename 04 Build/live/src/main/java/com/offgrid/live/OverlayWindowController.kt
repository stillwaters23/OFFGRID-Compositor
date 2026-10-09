package com.offgrid.live

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max

internal class OverlayWindowController(
    private val service: Service,
    private val onClose: () -> Unit,
) : OverlayPanelView.Listener {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val preferences = OverlayPrefs(service)
    private var panel: OverlayPanelView? = null
    private var params: LayoutParams? = null
    private var closeTarget: TextView? = null

    fun show() {
        if (!Settings.canDrawOverlays(service)) {
            onClose()
            return
        }
        if (panel != null) {
            updateOverlay()
            return
        }
        val screen = screenSize()
        val width = if (preferences.fullScreen) screen.first else max(dp(160), screen.first * preferences.widthPercent / 100)
        val height = if (preferences.fullScreen) screen.second - safeTop() else max(dp(224), screen.second * preferences.heightPercent / 100)
        val view = OverlayPanelView(service, this, preferences.isBubble)
        updateControlState(view)
        val defaultX = if (preferences.fullScreen) 0 else screen.first - width - dp(8)
        val defaultY = safeTop()
        val layout = LayoutParams(
            if (preferences.isBubble) dp(BUBBLE_DP) else width,
            if (preferences.isBubble) dp(BUBBLE_DP) else height,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            overlayFlags(),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            val margin = if (preferences.fullScreen) 0 else dp(8)
            x = preferences.x.takeIf(Float::isFinite)?.toInt()?.coerceIn(
                margin,
                max(margin, screen.first - this.width - margin),
            ) ?: defaultX.coerceIn(margin, max(margin, screen.first - this.width - margin))
            y = preferences.y.takeIf(Float::isFinite)?.toInt()
                ?.coerceIn(safeTop(), max(safeTop(), screen.second - this.height))
                ?: defaultY
        }
        try {
            windowManager.addView(view, layout)
            panel = view
            params = layout
        } catch (_: WindowManager.BadTokenException) {
            onClose()
        } catch (_: SecurityException) {
            onClose()
        }
    }

    fun hide() = animateBubble(true)

    fun close() {
        removeCloseTarget()
        panel?.let {
            try {
                windowManager.removeView(it)
            } catch (_: IllegalArgumentException) {
                Unit
            }
        }
        panel = null
        params = null
    }

    override fun moveBy(dx: Int, dy: Int) {
        val layout = params ?: return
        val limits = screenSize()
        if (preferences.fullScreen) return
        val margin = dp(8)
        layout.x = (layout.x + dx).coerceIn(margin, max(margin, limits.first - layout.width - margin))
        layout.y = (layout.y + dy).coerceIn(safeTop(), max(safeTop(), limits.second - layout.height - dp(8)))
        updatePosition()
    }

    override fun resizeBy(dx: Int, dy: Int, fromLeftCorner: Boolean) {
        val layout = params ?: return
        val limits = screenSize()
        val previousRight = layout.x + layout.width
        layout.width = (layout.width + if (fromLeftCorner) -dx else dx).coerceIn(dp(160), limits.first)
        layout.height = (layout.height + dy).coerceIn(dp(224), limits.second - safeTop())
        if (fromLeftCorner) layout.x = previousRight - layout.width
        layout.x = layout.x.coerceIn(dp(8), max(dp(8), limits.first - layout.width - dp(8)))
        layout.y = layout.y.coerceIn(safeTop(), max(safeTop(), limits.second - layout.height - dp(8)))
        preferences.widthPercent = (layout.width * 100 / limits.first).coerceIn(15, 100)
        preferences.heightPercent = (layout.height * 100 / limits.second).coerceIn(15, 100)
        panel?.let {
            updateControlState(it)
            it.invalidate()
        }
        updatePosition()
    }

    override fun bubbleDrag(showTarget: Boolean) {
        if (!showTarget) {
            removeCloseTarget()
            return
        }
        val view = TextView(service).apply {
            text = "×"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xfff4f4f4.toInt())
            setBackgroundColor(0xff25142f.toInt())
        }
        val layout = LayoutParams(
            dp(64),
            dp(64),
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_NOT_TOUCH_MODAL or
                LayoutParams.FLAG_NOT_TOUCHABLE or
                LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(96)
        }
        try {
            windowManager.addView(view, layout)
            closeTarget = view
        } catch (_: WindowManager.BadTokenException) {
            closeTarget = null
        } catch (_: SecurityException) {
            closeTarget = null
        }
    }

    override fun closeTargetContains(x: Float, y: Float): Boolean {
        val screen = screenSize()
        return x in (screen.first / 2f - dp(56))..(screen.first / 2f + dp(56)) &&
            y in (screen.second - dp(160)).toFloat()..(screen.second - dp(48)).toFloat()
    }

    override fun action(name: String) {
        when (name) {
            "bubble" -> animateBubble(true)
            "expand" -> animateBubble(false)
            "snap" -> snapToEdge()
            "close" -> onClose()
            "open" -> LiveHostRegistry.host?.openApp()
            "full-screen" -> confirmFullScreen()
            "mode" -> chooseMode()
            "toggle-camera" -> toggleSetting("fx.cam.on", true)
            "toggle-mic" -> toggleSetting("fx.mic.on", false)
            else -> LiveHostRegistry.host?.fire(name)
        }
    }

    override fun openSizeEntry(axis: String) {
        val input = EditText(service).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "0 to 100"
            setSingleLine(true)
        }
        val dialog = AlertDialog.Builder(service)
            .setTitle("Overlay $axis size")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Set") { _, _ ->
                val value = input.text.toString().toIntOrNull()
                if (value == null || value !in 0..100) {
                    Toast.makeText(service, "Enter a number from 0 to 100.", Toast.LENGTH_SHORT).show()
                } else {
                    setPercentSize(axis, value)
                }
            }
            .create()
        dialog.window?.setType(LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.window?.addFlags(LayoutParams.FLAG_SECURE)
        makeFocusableForInput()
        dialog.setOnDismissListener {
            val view = panel
            val layout = params
            if (view != null && layout != null) {
                val screen = screenSize()
                layout.flags = overlayFlags()
                layout.x = layout.x.coerceIn(dp(8), max(dp(8), screen.first - layout.width - dp(8)))
                layout.y = layout.y.coerceIn(safeTop(), max(safeTop(), screen.second - layout.height - dp(8)))
                windowManager.updateViewLayout(view, layout)
            }
            inputWindowService().hideSoftInputFromWindow(input.windowToken, 0)
        }
        dialog.show()
        dialog.window?.setType(LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.window?.addFlags(LayoutParams.FLAG_SECURE)
        input.requestFocus()
        inputWindowService().showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun updateOverlay() {
        val view = panel ?: return
        val layout = params ?: return
        val screen = screenSize()
        view.bubble = preferences.isBubble
        updateControlState(view)
        removeCloseTarget()
        if (preferences.isBubble) {
            layout.width = dp(BUBBLE_DP)
            layout.height = dp(BUBBLE_DP)
        } else {
            layout.width = if (preferences.fullScreen) screen.first else max(dp(160), screen.first * preferences.widthPercent / 100)
            layout.height = if (preferences.fullScreen) screen.second - safeTop() else max(dp(224), screen.second * preferences.heightPercent / 100)
        }
        val margin = if (preferences.fullScreen) 0 else dp(8)
        layout.x = layout.x.coerceIn(margin, max(margin, screen.first - layout.width - margin))
        layout.y = layout.y.coerceIn(safeTop(), max(safeTop(), screen.second - layout.height))
        layout.flags = overlayFlags()
        view.invalidate()
        try {
            windowManager.updateViewLayout(view, layout)
        } catch (_: IllegalArgumentException) {
            onClose()
        }
    }

    private fun updatePosition() {
        val view = panel ?: return
        val layout = params ?: return
        preferences.x = layout.x.toFloat()
        preferences.y = layout.y.toFloat()
        try {
            windowManager.updateViewLayout(view, layout)
        } catch (_: IllegalArgumentException) {
            onClose()
        }
    }

    private fun snapToEdge() {
        val layout = params ?: return
        val screen = screenSize()
        layout.x = if (layout.x + layout.width / 2 < screen.first / 2) dp(8) else screen.first - layout.width - dp(8)
        layout.y = layout.y.coerceIn(safeTop(), screen.second - layout.height - dp(8))
        updatePosition()
    }

    private fun setPercentSize(axis: String, value: Int) {
        if (axis == "W") preferences.widthPercent = value else preferences.heightPercent = value
        updateOverlay()
    }

    private fun animateBubble(bubble: Boolean) {
        val view = panel ?: return
        if (!ValueAnimator.areAnimatorsEnabled()) {
            preferences.isBubble = bubble
            updateOverlay()
            return
        }
        view.animate().cancel()
        view.animate()
            .alpha(0f)
            .setDuration(160)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                preferences.isBubble = bubble
                updateOverlay()
                view.alpha = 0f
                view.animate()
                    .alpha(1f)
                    .setDuration(160)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun updateControlState(view: OverlayPanelView) {
        val host = LiveHostRegistry.host
        view.cameraEnabled = host?.readSetting("fx.cam.on")?.toBooleanStrictOrNull() ?: true
        view.microphoneEnabled = host?.readSetting("fx.mic.on")?.toBooleanStrictOrNull() ?: false
        view.fullScreenEnabled = preferences.fullScreen
        view.widthLabel = "W"
        view.heightLabel = "H"
    }

    private fun confirmFullScreen() {
        if (preferences.fullScreen) {
            preferences.fullScreen = false
            updateOverlay()
            return
        }
        val dialog = AlertDialog.Builder(service)
            .setTitle("Full screen")
            .setMessage("This covers Bigo. Use it only when you choose screen sharing.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Use full screen") { _, _ ->
                preferences.fullScreen = true
                updateOverlay()
            }
            .create()
        dialog.window?.setType(LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.window?.addFlags(LayoutParams.FLAG_SECURE)
        dialog.show()
        dialog.window?.setType(LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.window?.addFlags(LayoutParams.FLAG_SECURE)
    }

    private fun chooseMode() {
        val dialog = AlertDialog.Builder(service)
            .setTitle("Camera mode")
            .setItems(arrayOf("Live", "Blackout", "Cloak", "Status", "Ghost")) { _, which ->
                val mode = arrayOf("live", "blackout", "cloak", "status", "ghost")[which]
                LiveHostRegistry.host?.writeSetting("fx.camera.mode", mode)
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.window?.setType(LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.window?.addFlags(LayoutParams.FLAG_SECURE)
        dialog.show()
        dialog.window?.setType(LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.window?.addFlags(LayoutParams.FLAG_SECURE)
    }

    private fun toggleSetting(key: String, default: Boolean) {
        val host = LiveHostRegistry.host ?: return
        val current = host.readSetting(key)?.toBooleanStrictOrNull() ?: default
        host.writeSetting(key, (!current).toString())
        panel?.let {
            updateControlState(it)
            it.invalidate()
        }
    }

    private fun removeCloseTarget() {
        closeTarget?.let {
            try {
                windowManager.removeView(it)
            } catch (_: IllegalArgumentException) {
                Unit
            }
        }
        closeTarget = null
    }

    private fun overlayFlags(): Int =
        LayoutParams.FLAG_NOT_FOCUSABLE or
            LayoutParams.FLAG_NOT_TOUCH_MODAL or
            LayoutParams.FLAG_SECURE or
            LayoutParams.FLAG_LAYOUT_IN_SCREEN

    private fun makeFocusableForInput() {
        val view = panel ?: return
        val layout = params ?: return
        layout.flags = layout.flags and LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        windowManager.updateViewLayout(view, layout)
    }

    private fun inputWindowService() = service.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private fun screenSize(): Pair<Int, Int> =
        service.resources.displayMetrics.widthPixels to service.resources.displayMetrics.heightPixels

    private fun safeTop(): Int = dp(32)

    private fun dp(value: Int): Int = (value * service.resources.displayMetrics.density).toInt()

    private companion object {
        const val BUBBLE_DP = 56
    }
}
