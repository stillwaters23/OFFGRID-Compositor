@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.offgrid.compositor

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * Headless clip player for the AR Video Deck.
 *
 * There is no player view and no chrome. One clip repeats ([Player.REPEAT_MODE_ONE])
 * into a [Surface] the GL stage already owns. The player lives on the drawing thread's
 * looper, the same one that samples the texture, so a frame is never consumed off-thread.
 */
internal class ArDeckPlayer(
    context: Context,
    private val handler: Handler,
    /** The clip's real picture size (width, height, pixel width to height ratio). Drawing thread. */
    private val onVideoSize: ((Int, Int, Float) -> Unit)? = null,
) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private val sizeListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            onVideoSize?.invoke(videoSize.width, videoSize.height, videoSize.pixelWidthHeightRatio)
        }
    }
    private var surface: Surface? = null
    private var loop = true

    fun attachSurface(next: Surface) {
        onGl {
            surface = next
            ensurePlayer().setVideoSurface(next)
        }
    }

    fun setLoop(on: Boolean) {
        loop = on
        onGl { player?.repeatMode = repeatMode(on) }
    }

    fun playUri(uri: String) {
        if (!ArDeckCatalog.isLocal(uri)) return
        onGl {
            val exo = ensurePlayer()
            exo.repeatMode = repeatMode(loop)
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    fun resume() {
        onGl { player?.play() }
    }

    fun pause() {
        onGl { player?.pause() }
    }

    /** Drops the picture target and releases the player. Safe on the drawing thread. */
    fun releaseAndJoin() {
        onGl {
            player?.setVideoSurface(null)
            player?.release()
            player = null
            surface = null
        }
    }

    private fun onGl(block: () -> Unit) {
        if (Looper.myLooper() == handler.looper) block() else handler.post(block)
    }

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        val source = DefaultMediaSourceFactory(DefaultDataSource.Factory(appContext))
        return ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(source)
            .setLooper(handler.looper)
            .setWakeMode(C.WAKE_MODE_NONE)
            .build()
            .also { created ->
                created.addListener(sizeListener)
                created.repeatMode = repeatMode(loop)
                created.playWhenReady = false
                surface?.let { created.setVideoSurface(it) }
                player = created
            }
    }

    companion object {
        fun repeatMode(loop: Boolean): Int =
            if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }
}
