package com.offgrid.compositor.audio

/** Takes finished mic audio, 48 kHz mono 16-bit, one block at a time on the mic thread. */
fun interface PcmSink {
    fun onPcm(block: ShortArray, frames: Int)
}
