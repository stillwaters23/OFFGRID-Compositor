package com.offgrid.live

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class LiveService : Service() {
    private lateinit var notification: LiveNotification
    private lateinit var overlay: OverlayWindowController

    override fun onCreate() {
        super.onCreate()
        notification = LiveNotification(this)
        notification.createChannel()
        overlay = OverlayWindowController(this, ::stopOverlayService)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_SHOW) {
            ACTION_SHOW, ACTION_HIDE -> startOverlayService(intent?.action == ACTION_HIDE)
            ACTION_OPEN -> LiveHostRegistry.host?.openApp() ?: openApp()
            ACTION_STOP -> stopOverlayService()
        }
        return START_STICKY
    }

    private fun startOverlayService(hide: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification.build())
        }
        overlay.show()
        if (hide) overlay.hide()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopOverlayService() {
        overlay.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        private const val NOTIFICATION_ID = 4201
    }
}
