package com.offgrid.live

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.app.AlertDialog
import androidx.core.content.ContextCompat

object LiveOverlayController {
    /**
     * Phase 1.3: true while the Broadcast Lock holds. It lives with the process, not with
     * [LiveService], because the service stops itself after a lock and a fresh instance would
     * forget it. Every SHOW, HIDE, and permission dialog checks it, whoever asks.
     */
    @Volatile
    var broadcastLocked = false
        private set

    fun askForPermission(activity: Activity) {
        if (broadcastLocked) return
        if (Settings.canDrawOverlays(activity)) {
            openOverlay(activity)
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("Allow the OFFGRID overlay")
            .setMessage("Turn on display over other apps so the small controls can sit above Bigo. Bigo's camera and the viewer's picture do not change.")
            .setNegativeButton("Not now", null)
            .setPositiveButton("Open settings") { _, _ ->
                activity.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${activity.packageName}"),
                    ),
                )
            }
            .show()
    }

    fun openOverlay(context: Context) {
        if (broadcastLocked || !Settings.canDrawOverlays(context)) return
        startOverlayService(context, LiveService.ACTION_SHOW)
    }

    fun hideOverlay(context: Context) {
        if (broadcastLocked || !Settings.canDrawOverlays(context)) return
        startOverlayService(context, LiveService.ACTION_HIDE)
    }

    /**
     * Android 15 lets an overlay app start a foreground service from the background only while
     * one of its overlay windows is visible, and the lock has just removed it. A Phone 2 unlock
     * with Bigo in front lands here; the overlay then waits for OFFGRID's next pause instead.
     */
    private fun startOverlayService(context: Context, action: String) {
        try {
            ContextCompat.startForegroundService(context, Intent(context, LiveService::class.java).setAction(action))
        } catch (_: IllegalStateException) {
            Unit
        }
    }

    /**
     * Phase 1.3: the Broadcast Lock removes the overlay window entirely. [hideOverlay] only
     * shrinks it to a bubble, and a bubble with FLAG_SECURE is still a black box on a broadcast.
     * A plain start is enough: the service either removes its window or was not running.
     */
    fun lockOverlay(context: Context) {
        broadcastLocked = true
        if (!Settings.canDrawOverlays(context)) return
        runCatching {
            context.startService(Intent(context, LiveService::class.java).setAction(LiveService.ACTION_LOCK))
        }
    }

    /**
     * The lock is off. The caller follows with [openOverlay] when the overlay setting is on.
     * No service start here: starting the service only to stop it again would bring it down
     * under a pending SHOW from [openOverlay], and Android kills an app whose
     * startForegroundService never reaches startForeground.
     */
    @Suppress("UNUSED_PARAMETER")
    fun unlockOverlay(context: Context) {
        broadcastLocked = false
    }
}
