package com.offgrid.live

sealed interface PcSample {
    data class VideoConfig(val sps: ByteArray, val pps: ByteArray) : PcSample
    data class Video(val nalus: List<ByteArray>, val timeUs: Long, val keyframe: Boolean) : PcSample
    data class AudioConfig(val asc: ByteArray) : PcSample
    data class Audio(val frame: ByteArray, val timeUs: Long) : PcSample
}

interface PcTransport : AutoCloseable {
    fun write(sample: PcSample)
}
