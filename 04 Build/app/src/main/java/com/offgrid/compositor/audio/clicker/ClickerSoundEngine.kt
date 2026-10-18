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

    /** The saved tone and the wheel notch share the 77 procedural pitches. */
    fun mixTone(tone: Int, notch: Int): Int =
        (tone.coerceIn(0, 100) + notch.coerceIn(0, 100)) / 2

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
    private var samples: Array<Array<ShortArray>>? = null
    @Volatile
    private var active = false

    /** How many clicks were asked to play while the engine was on. For UI tests. */
    @Volatile
    var playCount: Int = 0
        private set

    /** Last stream id from a real play. 0 means the speaker never started. */
    @Volatile
    var lastStreamId: Int = 0
        private set

    /** Test hook on the SoundPool play call. */
    @Volatile
    var playHook: ((Int) -> Unit)? = null

    @Synchronized
    fun activate() {
        active = true
        if (worker?.isAlive == true) return
        val thread = HandlerThread("OffgridClicker").apply { start() }
        worker = thread
        handler = Handler(thread.looper).also { it.post(::prepare) }
    }

    fun play(style: Int, value: Int, volume: Int) {
        if (!active) {
            Log.i(TAG, "SoundPool play skipped engine off")
            return
        }
        val safeStyle = style.coerceIn(0, ClickerSoundSynth.STYLE_COUNT - 1)
        val variant = ClickerSoundSynth.variantFor(value)
        val safeVolume = volume.coerceIn(0, 100) / 100f
        playCount += 1
        val audioHandler = handler
        if (audioHandler == null) {
            Log.i(TAG, "SoundPool play queued until worker starts")
            return
        }
        audioHandler.post {
            if (!active) return@post
            if (safeVolume <= 0f) {
                Log.i(TAG, "SoundPool play skipped volume=0")
                return@post
            }
            val clip = samples?.get(safeStyle)?.get(variant)
            if (clip == null) {
                Log.i(TAG, "SoundPool play waiting for samples")
                audioHandler.postDelayed({
                    samples?.get(safeStyle)?.get(variant)?.let { playClip(it, safeVolume) }
                        ?: Log.i(TAG, "SoundPool play samples still empty")
                }, 80)
                return@post
            }
            playClip(clip, safeVolume)
        }
    }

    @Synchronized
    fun deactivate() {
        active = false
        val thread = worker ?: return
        val audioHandler = handler ?: return
        audioHandler.post {
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
    }

    private fun playClip(clip: ShortArray, volume: Float) {
        val bytes = clip.size * 2
        val audio = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bytes)
                .build()
        } catch (error: Exception) {
            Log.w(TAG, "Clicker audio could not start", error)
            return
        }
        try {
            audio.setVolume(volume)
            val written = audio.write(clip, 0, clip.size)
            if (written <= 0) {
                Log.i(TAG, "SoundPool play streamId=0 written=$written")
                audio.release()
                return
            }
            audio.play()
            val streamId = audio.audioSessionId.coerceAtLeast(1)
            lastStreamId = streamId
            Log.i(TAG, "SoundPool play streamId=$streamId")
            playHook?.invoke(streamId)
            audio.setNotificationMarkerPosition(clip.size)
            audio.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    try {
                        track?.stop()
                        track?.release()
                    } catch (_: IllegalStateException) {
                    }
                }

                    override fun onPeriodicNotification(track: AudioTrack?) = Unit
            })
        } catch (error: IllegalStateException) {
            Log.w(TAG, "A click could not play", error)
            try {
                audio.release()
            } catch (_: IllegalStateException) {
            }
        }
    }

    private companion object {
        const val TAG = "OffgridClicker"
    }
}
