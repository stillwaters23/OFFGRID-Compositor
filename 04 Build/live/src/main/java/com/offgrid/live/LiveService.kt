package com.offgrid.live

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.content.ContextCompat

class LiveService : Service() {
    private lateinit var notification: LiveNotification
    private lateinit var overlay: OverlayWindowController
    private var overlayVisible = false

    /** Phase 1.3: true while the Broadcast Lock keeps the overlay window off the screen. */
    private var overlayLocked = false
    private var streaming = false
    private var streamCamera = false
    private var streamMicrophone = false

    override fun onCreate() {
        super.onCreate()
        notification = LiveNotification(this)
        notification.createChannel()
        overlay = OverlayWindowController(this, ::onOverlayClosed)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_SHOW) {
            ACTION_SHOW, ACTION_HIDE -> startOverlayService(intent?.action == ACTION_HIDE)
            ACTION_STREAM_START -> if (intent != null) startStream(intent)
            ACTION_STREAM_STOP -> stopStream()
            ACTION_OPEN -> LiveHostRegistry.host?.openApp() ?: openApp()
            ACTION_STOP -> onNotificationStop()
            ACTION_LOCK -> lockOverlay()
            ACTION_UNLOCK -> unlockOverlay()
        }
        return START_NOT_STICKY
    }

    /**
     * Phase 1.3: the Broadcast Lock takes the overlay window off the screen. Its FLAG_SECURE
     * would otherwise paint a black box over the broadcast. The setting stays on, and SHOW is
     * ignored until UNLOCK, so nothing of it comes back while the stage is on air.
     */
    private fun lockOverlay() {
        overlayLocked = true
        overlay.close()
        overlayVisible = false
        if (!streaming) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /** The lock is off. The camera screen sends SHOW again itself when the overlay setting is on. */
    private fun unlockOverlay() {
        overlayLocked = false
        if (!streaming && !overlayVisible) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startOverlayService(hide: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification.build(),
                foregroundTypes(),
            )
        } else {
            startForeground(NOTIFICATION_ID, notification.build())
        }
        if (overlayLocked) {
            // A SHOW that arrives while locked is dropped: the window stays off the air. The
            // foreground call above satisfies startForegroundService before the service stops.
            if (!streaming) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return
        }
        overlayVisible = Settings.canDrawOverlays(this)
        overlay.show()
        if (hide) overlay.hide()
    }

    private fun startStream(intent: Intent) {
        streaming = true
        streamCamera = intent.getBooleanExtra(EXTRA_CAMERA, false)
        streamMicrophone = intent.getBooleanExtra(EXTRA_MICROPHONE, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification.build(), foregroundTypes())
        } else {
            startForeground(NOTIFICATION_ID, notification.build())
        }
    }

    private fun foregroundTypes(): Int = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
        (if (streaming && streamCamera) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0) or
        (if (streaming && streamMicrophone) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0)

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopOverlayService() {
        overlay.close()
        overlayVisible = false
        if (streaming) return
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onOverlayClosed() {
        overlayVisible = false
        overlay.close()
        LiveHostRegistry.host?.writeSetting("fx.live.overlay", "false")
        if (!streaming) stopOverlayService()
    }

    private fun stopStream() {
        streaming = false
        streamCamera = false
        streamMicrophone = false
        if (overlayVisible) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            }
        } else {
            stopOverlayService()
        }
    }

    private fun onNotificationStop() {
        streaming = false
        streamCamera = false
        streamMicrophone = false
        onOverlayClosed()
    }

    private fun openApp() {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
    }

    override fun onDestroy() {
        if (::overlay.isInitialized) overlay.close()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "com.offgrid.live.SHOW"
        const val ACTION_OPEN = "com.offgrid.live.OPEN"
        const val ACTION_HIDE = "com.offgrid.live.HIDE"
        const val ACTION_STOP = "com.offgrid.live.STOP"
        const val ACTION_STREAM_START = "com.offgrid.live.STREAM_START"
        const val ACTION_STREAM_STOP = "com.offgrid.live.STREAM_STOP"
        const val ACTION_LOCK = "com.offgrid.live.LOCK"
        const val ACTION_UNLOCK = "com.offgrid.live.UNLOCK"
        const val EXTRA_CAMERA = "camera"
        const val EXTRA_MICROPHONE = "microphone"
        private const val NOTIFICATION_ID = 4201

        fun startStreaming(context: android.app.Activity, camera: Boolean, microphone: Boolean) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, LiveService::class.java)
                    .setAction(ACTION_STREAM_START)
                    .putExtra(EXTRA_CAMERA, camera)
                    .putExtra(EXTRA_MICROPHONE, microphone),
            )
        }

        fun stopStreaming(context: Context) {
            context.startService(Intent(context, LiveService::class.java).setAction(ACTION_STREAM_STOP))
        }
    }
}
