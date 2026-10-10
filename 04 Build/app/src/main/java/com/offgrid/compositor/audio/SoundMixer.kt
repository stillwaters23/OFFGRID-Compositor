package com.offgrid.compositor.audio

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Mixes the soundboard's own sounds into the recorded audio. Sounds arrive at their own sample rate
 * and are played at the recording rate. Sounds are only kept while a recording is listening.
 */
class SoundMixer(private val outputRate: Int = VoiceFx.SAMPLE_RATE) {
    private class Playing(val samples: ShortArray, val inputRate: Int, val gain: Float) {
        var position = 0.0
    }

    private val waiting = ConcurrentLinkedQueue<Playing>()
    private val playing = ArrayList<Playing>()

    @Volatile
    var listening = false

    /** Any thread. */
    fun add(samples: ShortArray, inputRate: Int, gain: Float) {
        if (listening && samples.isNotEmpty()) waiting.add(Playing(samples, inputRate, gain))
    }

    /** Mixing thread only. Adds the sounds into [block] and clips at full scale. */
    fun mixInto(block: ShortArray, frames: Int) {
        while (true) playing += waiting.poll() ?: break
        if (playing.isEmpty()) return
        val iterator = playing.iterator()
        while (iterator.hasNext()) {
            val sound = iterator.next()
            val step = sound.inputRate.toDouble() / outputRate
            for (i in 0 until frames) {
                val index = sound.position.toInt()
                if (index >= sound.samples.size - 1) break
                val mix = (sound.position - index).toFloat()
                val value = sound.samples[index] + (sound.samples[index + 1] - sound.samples[index]) * mix
                block[i] = (block[i] + value * sound.gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                sound.position += step
            }
            if (sound.position.toInt() >= sound.samples.size - 1) iterator.remove()
        }
    }

    fun clear() {
        waiting.clear()
        playing.clear()
    }
}
