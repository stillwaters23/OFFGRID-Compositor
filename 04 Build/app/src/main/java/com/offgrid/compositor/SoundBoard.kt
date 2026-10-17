package com.offgrid.compositor

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.offgrid.compositor.audio.MicKeys
import com.offgrid.compositor.audio.SoundMixer
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.EffectSnapshot
import com.offgrid.compositor.effects.FxKeys
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * The soundboard (feature 6, final build M4). Each effect can play its own short sound on
 * Royalty's phone speaker. The sounds are made by the app itself, so there are no sound files.
 *
 * It only plays sound out. It never listens. While a recording runs, and Sounds in video is on,
 * the same sounds are also handed to [videoMix], so they are heard in the video.
 */
internal class SoundBoard {
    private val thread = HandlerThread("OffgridSounds").apply { start() }
    private val handler = Handler(thread.looper)
    private val made = HashMap<String, ShortArray>()

    /** Collects the sounds for the recorded audio. It only keeps them while a recording listens. */
    val videoMix = SoundMixer()

    /** Sounds still playing. All of them are let go when the soundboard closes. */
    private val playing = HashSet<AudioTrack>()

    /**
     * Puts the radar ping in the recorded audio whenever the radar or the mirror scan is on.
     * The phone speaker still follows the soundboard switch.
     */
    fun playRadar(settings: EffectSnapshot) {
        if (!RadarPing.intoVideo(settings.on(FxKeys.RADAR_ON), settings.on(FxKeys.MIRROR_ON))) return
        val chosen = settings.choice(FxKeys.SOUND_PREFIX + RADAR)
        val name = RadarPing.soundName(chosen)
        val typed = settings.value(FxKeys.SOUND_VOLUME)
        val gain = if (typed > 0f) typed else RadarPing.QUIET_GAIN
        val speaker = settings.on(FxKeys.SOUND_ON) && chosen != OFF && typed > 0f
        handler.post {
            val samples = made.getOrPut(name) { SoundShapes.make(name) }
            if (samples.isEmpty()) return@post
            videoMix.add(samples, SoundShapes.RATE, gain)
            if (speaker) playSpeaker(samples, typed)
        }
    }

    /** Plays [effect]'s chosen sound if the soundboard is on. [force] plays it even when off, for Try it. */
    fun play(effect: String, settings: EffectSnapshot, force: Boolean = false) {
        if (!force && !settings.on(FxKeys.SOUND_ON)) return
        val sound = settings.choice(FxKeys.SOUND_PREFIX + effect)
        if (sound.isEmpty() || sound == OFF) return
        val volume = settings.value(FxKeys.SOUND_VOLUME)
        if (volume <= 0f) return
        val intoVideo = settings.on(MicKeys.SOUNDS_IN_VIDEO, true)
        handler.post { playNow(sound, volume, intoVideo) }
    }

    fun release() {
        handler.post {
            playing.forEach { it.release() }
            playing.clear()
            thread.quitSafely()
        }
    }

    private fun playNow(sound: String, volume: Float, intoVideo: Boolean) {
        val samples = made.getOrPut(sound) { SoundShapes.make(sound) }
        if (samples.isEmpty()) return
        if (intoVideo) videoMix.add(samples, SoundShapes.RATE, volume)
        playSpeaker(samples, volume)
    }

    private fun playSpeaker(samples: ShortArray, volume: Float) {
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SoundShapes.RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            playing += track
            track.write(samples, 0, samples.size)
            track.setVolume(volume)
            track.setNotificationMarkerPosition(samples.size)
            track.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(done: AudioTrack) {
                        playing -= done
                        done.release()
                    }

                    override fun onPeriodicNotification(track: AudioTrack) = Unit
                },
                handler,
            )
            track.play()
        } catch (error: Exception) {
            Log.w(TAG, "A sound could not play", error)
            track?.let {
                playing -= it
                it.release()
            }
        }
    }

    private companion object {
        const val TAG = "OffgridSounds"
        const val OFF = "off"
        const val RADAR = "radar"
    }
}

/** The built-in sounds, made from simple waves and soft noise. Tests check their length and loudness. */
internal object SoundShapes {
    const val RATE = 22050

    fun make(name: String): ShortArray = when (name) {
        "chime" -> build(0.9f) { t -> (sin(2 * PI * 880 * t) + 0.5 * sin(2 * PI * 1320 * t)) * decay(t, 3.5) * 0.6 }
        "pop" -> build(0.12f) { t -> (sin(2 * PI * (420 - 1800 * t) * t) + noise(t) * 0.3) * decay(t, 40.0) }
        "whoosh" -> build(0.8f) { t -> noise(t) * swell(t, 0.8) * 0.7 }
        "glass" -> build(0.7f) { t ->
            val ring = sin(2 * PI * 2630 * t) + sin(2 * PI * 3170 * t) * 0.7 + sin(2 * PI * 4410 * t) * 0.5
            (ring * 0.35 + noise(t) * decay(t, 30.0)) * decay(t, 6.0)
        }
        "power" -> build(1.0f) { t ->
            val pitch = if (t < 0.5) 520 - 700 * t else 170 + 900 * (t - 0.5)
            sin(2 * PI * pitch * t) * 0.5 * swell(t, 1.0)
        }
        "ping" -> build(0.9f) { t -> (sin(2 * PI * 1480 * t) * decay(t, 7.0) + sin(2 * PI * 1480 * (t - 0.18)) * 0.35 * decay(t - 0.18, 7.0) * step(t, 0.18)) * 0.7 }
        "sparkle" -> build(0.7f) { t ->
            var sum = 0.0
            for (k in 0 until 5) {
                val start = k * 0.12
                sum += sin(2 * PI * (1800 + 400 * k) * t) * decay(t - start, 18.0) * step(t, start)
            }
            sum * 0.35
        }
        "boom" -> build(1.1f) { t -> (sin(2 * PI * (70 - 20 * t) * t) + noise(t) * 0.5 * decay(t, 12.0)) * decay(t, 3.0) * 0.9 }
        else -> ShortArray(0)
    }

    private inline fun build(seconds: Float, wave: (Double) -> Double): ShortArray {
        val count = (seconds * RATE).toInt()
        val out = ShortArray(count)
        val fadeOut = (RATE * FADE_SECONDS).toInt()
        for (i in 0 until count) {
            val t = i.toDouble() / RATE
            // A short fade at both ends, so no sound ever clicks.
            val edge = minOf(1.0, i / (RATE * FADE_SECONDS), (count - i).toDouble() / fadeOut)
            val v = (wave(t) * edge).coerceIn(-1.0, 1.0)
            out[i] = (v * Short.MAX_VALUE * LEVEL).toInt().toShort()
        }
        return out
    }

    private fun decay(t: Double, speed: Double): Double = if (t < 0) 0.0 else exp(-t * speed)

    private fun swell(t: Double, length: Double): Double = sin(PI * (t / length).coerceIn(0.0, 1.0))

    private fun step(t: Double, at: Double): Double = if (t >= at) 1.0 else 0.0

    /** Steady noise: the same every time, from the time alone. */
    private fun noise(t: Double): Double = EffectMath.hash((t * RATE).toInt(), NOISE_SALT) * 2.0 - 1.0

    private const val FADE_SECONDS = 0.004
    private const val LEVEL = 0.8
    private const val NOISE_SALT = 91
}
