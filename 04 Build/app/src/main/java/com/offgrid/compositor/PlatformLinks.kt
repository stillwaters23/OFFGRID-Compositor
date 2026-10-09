package com.offgrid.compositor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens a platform's own app with one tap, so Royalty can ask to join again herself
 * (feature 28, final build M4). It never taps, joins, or rejoins for her.
 *
 * App ids come from each app's public store page. When an app isn't on the phone, or its
 * id is UNCONFIRMED, the phone's app store opens a search for it instead.
 */
internal object PlatformLinks {
    /** Platform folder id to the app's id on the phone. Ids not listed here are UNCONFIRMED. */
    val PACKAGES = mapOf(
        "bigo_live" to "sg.bigo.live",
        "tagged" to "com.taggedapp",
        "meetme" to "com.myyearbook.m",
        "tiktok" to "com.zhiliaoapp.musically",
        "twitch" to "tv.twitch.android.app",
        "youtube_live" to "com.google.android.youtube",
        "telegram" to "org.telegram.messenger",
        "tango" to "com.sgiggle.production",
        "instagram" to "com.instagram.android",
        "messenger" to "com.facebook.orca",
        "whatsapp" to "com.whatsapp",
        "zoom" to "us.zoom.videomeetings",
        "google_meet" to "com.google.android.apps.tachyon",
        "discord" to "com.discord",
        "microsoft_teams" to "com.microsoft.teams",
    )

    /** What happened, so the panel can say it in plain words. */
    enum class Opened { APP, STORE, NOTHING }

    fun open(context: Context, id: String, title: String): Opened {
        val app = PACKAGES[id]?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (app != null) {
            app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (start(context, app)) return Opened.APP
        }
        val query = Uri.encode(title)
        val store = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query&c=apps"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (start(context, store)) return Opened.STORE
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$query&c=apps"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (start(context, web)) Opened.STORE else Opened.NOTHING
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (error: ActivityNotFoundException) {
        false
    } catch (error: SecurityException) {
        false
    }
}
