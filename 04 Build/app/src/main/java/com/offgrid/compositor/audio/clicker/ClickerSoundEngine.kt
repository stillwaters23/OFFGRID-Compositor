package com.offgrid.compositor.audio.clicker

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Seven code-generated timbres, each with 77 precomputed pitch variants. */
internal object ClickerSoundSynth {
    const val STYLE_COUNT = 7
    const val VARIANT_COUNT = 77
    const val SAMPLE_RATE = 48000

    fun variantFor(value: Int): Int =
        ((value.coerceIn(0, 100) * (VARIANT_COUNT - 1) + 50) / 100).coerceIn(0, VARIANT_COUNT - 1)

    fun make(style: Int, variant: Int): ShortArray {
        require(style in 0 until STYLE_COUNT)
        require(variant in 0 until VARIANT_COUNT)
        val length = (SAMPLE_RATE * CLIP_SECONDS).toInt()
        val pitch = 680.0 + variant * 8.5
        return ShortArray(length) { index ->
            val t = index.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * (if (style == 5) 34.0 else 58.0)) *
                minOf(1.0, index / 48.0, (length - index) / 48.0)
            val wave = when (style) {
                0 -> sin(2 * PI * pitch * t)
                1 -> (sin(2 * PI * pitch * t) + 0.32 * sin(2 * PI * pitch * 2.2 * t))
                2 -> if (sin(2 * PI * pitch * t) >= 0) 1.0 else -1.0
                3 -> sin(2 * PI * (pitch - 900 * t) * t) * (1.0 - t / CLIP_SECONDS)
                4 -> sin(2 * PI * pitch * t) + 0.25 * sin(2 * PI * pitch * 1.503 * t)
                5 -> (sin(2 * PI * pitch * t) + 0.4 * sin(2 * PI * pitch * 3.1 * t)) * 0.55
                else -> sin(2 * PI * pitch * t) * 0.42
            } * envelope * MASTER_LEVEL
            (wave.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private const val CLIP_SECONDS = 0.008
    private const val MASTER_LEVEL = 0.42
}

/** Streams short synthesized ticks on a worker thread, never on the panel's UI thread. */
internal class ClickerSoundEngine {
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private var track: AudioTrack? = null
    private var samples: Array<Array<ShortArray>>? = null
    @Volatile
    private var active = false

    @Synchronized
    fun activate() {
        active = true
        if (worker?.isAlive == true) {
            handler?.post { if (track == null) track = createTrack() }
            return
        }
        val thread = HandlerThread("OffgridClicker").apply { start() }
        worker = thread
        handler = Handler(thread.looper).also { it.post(::prepare) }
    }

    fun play(style: Int, value: Int, volume: Int) {
        if (!active) return
        val safeStyle = style.coerceIn(0, ClickerSoundSynth.STYLE_COUNT - 1)
        val variant = ClickerSoundSynth.variantFor(value)
        val safeVolume = volume.coerceIn(0, 100) / 100f
        handler?.post {
            if (!active) return@post
            if (safeVolume <= 0f) return@post
            val audio = track ?: createTrack()?.also { track = it } ?: return@post
            val clip = samples?.get(safeStyle)?.get(variant) ?: return@post
            audio.setVolume(safeVolume)
            try {
                var offset = 0
                while (offset < clip.size) {
                    val written = audio.write(clip, offset, clip.size - offset, AudioTrack.WRITE_BLOCKING)
                    if (written <= 0) {
                        releaseTrack()
                        return@post
                    }
                    offset += written
                }
                if (audio.playState != AudioTrack.PLAYSTATE_PLAYING) audio.play()
            } catch (error: IllegalStateException) {
                Log.w(TAG, "A click could not play", error)
                releaseTrack()
            }
        }
    }

    @Synchronized
    fun deactivate() {
        active = false
        val thread = worker ?: return
        val audioHandler = handler ?: return
        audioHandler.post {
            releaseTrack()
            synchronized(this) {
                if (!active && worker === thread) {
                    worker = null
                    handler = null
                    thread.quitSafely()
                }
            }
        }
    }

    private fun prepare() {
        if (samples == null) {
            samples = Array(ClickerSoundSynth.STYLE_COUNT) { style ->
                Array(ClickerSoundSynth.VARIANT_COUNT) { variant -> ClickerSoundSynth.make(style, variant) }
            }
        }
        track = createTrack()
    }

    private fun createTrack(): AudioTrack? = try {
        val minimum = AudioTrack.getMinBufferSize(
            ClickerSoundSynth.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(ClickerSoundSynth.SAMPLE_RATE / 1000 * 2 * 8)
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(ClickerSoundSynth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setBufferSizeInBytes(minimum)
            .build()
    } catch (error: Exception) {
        Log.w(TAG, "Clicker audio could not start", error)
        null
    }

    private fun releaseTrack() {
        track?.let {
            try {
                it.pause()
                it.flush()
                it.release()
            } catch (error: IllegalStateException) {
                Log.w(TAG, "Clicker audio could not stop cleanly", error)
            }
        }
        track = null
    }

    private companion object {
        const val TAG = "OffgridClicker"
    }
}
