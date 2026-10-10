package com.offgrid.live

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.app.AlertDialog
import androidx.core.content.ContextCompat

object LiveOverlayController {
    fun askForPermission(activity: Activity) {
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
        if (!Settings.canDrawOverlays(context)) return
        ContextCompat.startForegroundService(
            context,
            Intent(context, LiveService::class.java).setAction(LiveService.ACTION_SHOW),
        )
    }

    fun hideOverlay(context: Context) {
        if (!Settings.canDrawOverlays(context)) return
        ContextCompat.startForegroundService(
            context,
            Intent(context, LiveService::class.java).setAction(LiveService.ACTION_HIDE),
        )
    }
}
