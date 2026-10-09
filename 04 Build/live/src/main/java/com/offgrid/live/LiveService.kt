package com.offgrid.live

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class LiveService : Service(), OverlayPanelView.Listener {
    private lateinit var windowManager: WindowManager
    private lateinit var preferences: OverlayPrefs
    private var panel: OverlayPanelView? = null
    private var params: LayoutParams? = null
    private var sizeEntry: EditText? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        preferences = OverlayPrefs(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_SHOW) {
            ACTION_SHOW -> {
                startForeground(NOTIFICATION_ID, notification())
                showOverlay()
            }
            ACTION_HIDE -> {
                preferences.isBubble = true
                updateOverlay()
            }
            ACTION_STOP -> stopOverlay()
            ACTION_OPEN -> LiveHostRegistry.host?.openApp()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun moveBy(dx: Int, dy: Int) {
        val layout = params ?: return
        val limits = screenSize()
        layout.x = (layout.x + dx).coerceIn(0, max(0, limits.first - layout.width))
        layout.y = (layout.y + dy).coerceIn(safeTop(), max(safeTop(), limits.second - layout.height))
        updatePosition()
    }

    override fun resizeBy(dx: Int, dy: Int) {
        val layout = params ?: return
        val limits = screenSize()
        layout.width = (layout.width + dx).coerceIn(dp(160), limits.first)
        layout.height = (layout.height + dy).coerceIn(dp(144), limits.second - safeTop())
        preferences.widthPercent = (layout.width * 100 / limits.first).coerceIn(15, 100)
        preferences.heightPercent = (layout.height * 100 / limits.second).coerceIn(15, 100)
        currentWidth = preferences.widthPercent
        currentHeight = preferences.heightPercent
        updatePosition()
    }

    override fun action(name: String) {
        when (name) {
            "bubble" -> {
                preferences.isBubble = true
                updateOverlay()
            }
            "expand" -> {
                preferences.isBubble = false
                updateOverlay()
            }
            "snap" -> snapToEdge()
            "close" -> stopOverlay()
            "open" -> LiveHostRegistry.host?.openApp()
            "full-screen" -> confirmFullScreen()
            "mode" -> chooseMode()
            "toggle-camera" -> toggleSetting("fx.cam.on", true)
            "toggle-mic" -> toggleSetting("fx.mic.on", false)
            else -> {
                LiveHostRegistry.host?.fire(name)
            }
        }
    }

    override fun openSizeEntry(axis: String) {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "0 to 100"
            setSingleLine(true)
        }
        sizeEntry = input
        val dialog = AlertDialog.Builder(this)
            .setTitle("Overlay $axis size")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Set") { _, _ ->
                val value = input.text.toString().toIntOrNull()
                if (value == null || value !in 0..100) {
                    Toast.makeText(this, "Enter a number from 0 to 100.", Toast.LENGTH_SHORT).show()
                } else {
                    setPercentSize(axis, value.coerceAtLeast(15))
                }
            }
            .create()
        dialog.window?.setType(LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.window?.addFlags(LayoutParams.FLAG_SECURE)
        makeFocusableForInput()
        dialog.setOnDismissListener {
            sizeEntry = null
            windowManager.updateViewLayout(panel, params?.apply { flags = overlayFlags() })
            inputWindowService().hideSoftInputFromWindow(input.windowToken, 0)
        }
        dialog.show()
        dialog.window?.setType(LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.window?.addFlags(LayoutParams.FLAG_SECURE)
        input.requestFocus()
        inputWindowService().showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            stopOverlay()
            return
        }
        if (panel == null) {
            val screen = screenSize()
            val width = if (preferences.fullScreen) screen.first else max(dp(160), screen.first * preferences.widthPercent / 100)
            val height = if (preferences.fullScreen) screen.second - safeTop() else max(dp(144), screen.second * preferences.heightPercent / 100)
            currentWidth = preferences.widthPercent
            currentHeight = preferences.heightPercent
            val overlay = OverlayPanelView(this, this, preferences.isBubble)
            val startX = preferences.x.takeIf(Float::isFinite)?.toInt()
                ?: if (preferences.fullScreen) 0 else screen.first - width
            val startY = preferences.y.takeIf(Float::isFinite)?.toInt() ?: safeTop()
            val layout = LayoutParams(
                if (preferences.isBubble) dp(BUBBLE_DP) else width,
                if (preferences.isBubble) dp(BUBBLE_DP) else height,
                LayoutParams.TYPE_APPLICATION_OVERLAY,
                overlayFlags(),
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.LEFT
                x = startX.coerceIn(0, max(0, screen.first - this.width))
                y = startY.coerceIn(safeTop(), max(safeTop(), screen.second - this.height))
            }
            try {
                windowManager.addView(overlay, layout)
                panel = overlay
                params = layout
            } catch (_: WindowManager.BadTokenException) {
                stopOverlay()
            } catch (_: SecurityException) {
                stopOverlay()
            }
        } else {
            updateOverlay()
        }
    }

    private fun updateOverlay() {
        val view = panel ?: return
        val layout = params ?: return
        val screen = screenSize()
        view.bubble = preferences.isBubble
        if (preferences.isBubble) {
            layout.width = dp(BUBBLE_DP)
            layout.height = dp(BUBBLE_DP)
        } else {
            layout.width = if (preferences.fullScreen) screen.first else max(dp(160), screen.first * preferences.widthPercent / 100)
            layout.height = if (preferences.fullScreen) screen.second - safeTop() else max(dp(144), screen.second * preferences.heightPercent / 100)
        }
        layout.flags = overlayFlags()
        view.invalidate()
        try {
            windowManager.updateViewLayout(view, layout)
        } catch (_: IllegalArgumentException) {
            stopOverlay()
        }
    }

    private fun stopOverlay() {
        panel?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        panel = null
        params = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun overlayFlags(): Int =
        LayoutParams.FLAG_NOT_FOCUSABLE or
            LayoutParams.FLAG_NOT_TOUCH_MODAL or
            LayoutParams.FLAG_SECURE or
            LayoutParams.FLAG_LAYOUT_IN_SCREEN

    private fun updatePosition() {
        val layout = params ?: return
        val view = panel ?: return
        preferences.x = layout.x.toFloat()
        preferences.y = layout.y.toFloat()
        try {
            windowManager.updateViewLayout(view, layout)
        } catch (_: IllegalArgumentException) {
            stopOverlay()
        }
    }

    private fun snapToEdge() {
        val layout = params ?: return
        val screen = screenSize()
        layout.x = if (layout.x + layout.width / 2 < screen.first / 2) 0 else screen.first - layout.width
        layout.y = layout.y.coerceIn(safeTop(), screen.second - layout.height)
        updatePosition()
    }

    private fun setPercentSize(axis: String, value: Int) {
        if (axis == "W") preferences.widthPercent = value else preferences.heightPercent = value
        currentWidth = preferences.widthPercent
        currentHeight = preferences.heightPercent
        updateOverlay()
    }

    private fun confirmFullScreen() {
        if (preferences.fullScreen) {
            preferences.fullScreen = false
            updateOverlay()
            return
        }
        val dialog = AlertDialog.Builder(this)
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
        AlertDialog.Builder(this)
            .setTitle("Camera mode")
            .setItems(arrayOf("Front camera", "Back camera")) { _, which ->
                LiveHostRegistry.host?.writeSetting("fx.camera.mode", if (which == 0) "front" else "back")
            }
            .setNegativeButton("Cancel", null)
            .create().also {
                it.window?.setType(LayoutParams.TYPE_APPLICATION_OVERLAY)
                it.window?.addFlags(LayoutParams.FLAG_SECURE)
                it.show()
                it.window?.setType(LayoutParams.TYPE_APPLICATION_OVERLAY)
                it.window?.addFlags(LayoutParams.FLAG_SECURE)
            }
    }

    private fun toggleSetting(key: String, default: Boolean) {
        val host = LiveHostRegistry.host ?: return
        val current = host.readSetting(key)?.toBooleanStrictOrNull() ?: default
        host.writeSetting(key, (!current).toString())
        panel?.invalidate()
    }

    private fun notification(): Notification {
        val open = PendingIntent.getService(this, 1, serviceIntent(ACTION_OPEN), pendingIntentFlags())
        val hide = PendingIntent.getService(this, 2, serviceIntent(ACTION_HIDE), pendingIntentFlags())
        val stop = PendingIntent.getService(this, 3, serviceIntent(ACTION_STOP), pendingIntentFlags())
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("OFFGRID overlay is on")
            .setContentText("The small control panel is ready.")
            .setOngoing(true)
            .addAction(0, "Open", open)
            .addAction(0, "Hide overlay", hide)
            .addAction(0, "Stop", stop)
            .build()
    }

    private fun serviceIntent(action: String) = Intent(this, LiveService::class.java).setAction(action)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "OFFGRID overlay", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    private fun screenSize(): Pair<Int, Int> =
        resources.displayMetrics.widthPixels to resources.displayMetrics.heightPixels

    private fun safeTop(): Int = dp(32)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun makeFocusableForInput() {
        params?.let { layout ->
            layout.flags = layout.flags and LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            panel?.let { windowManager.updateViewLayout(it, layout) }
        }
    }

    private fun inputWindowService() = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0

    override fun onDestroy() {
        panel?.let { runCatching { windowManager.removeView(it) } }
        panel = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "com.offgrid.live.SHOW"
        const val ACTION_OPEN = "com.offgrid.live.OPEN"
        const val ACTION_HIDE = "com.offgrid.live.HIDE"
        const val ACTION_STOP = "com.offgrid.live.STOP"
        private const val CHANNEL_ID = "live_overlay"
        private const val NOTIFICATION_ID = 4201
        private const val BUBBLE_DP = 56

        @Volatile
        var currentWidth = 40
            private set

        @Volatile
        var currentHeight = 35
            private set
    }
}
