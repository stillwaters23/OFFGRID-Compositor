package com.offgrid.live

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

internal class LiveNotification(private val context: Context) {
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "OFFGRID overlay", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    fun build(): Notification {
        val open = action(1, LiveService.ACTION_OPEN)
        val hide = action(2, LiveService.ACTION_HIDE)
        val stop = action(3, LiveService.ACTION_STOP)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("OFFGRID overlay is on")
            .setContentText("The small control panel is ready.")
            .setOngoing(true)
            .addAction(0, "Open", open)
            .addAction(0, "Hide overlay", hide)
            .addAction(0, "Stop", stop)
            .build()
    }

    private fun action(requestCode: Int, action: String): PendingIntent =
        PendingIntent.getService(
            context,
            requestCode,
            Intent(context, LiveService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val CHANNEL_ID = "live_overlay"
    }
}
