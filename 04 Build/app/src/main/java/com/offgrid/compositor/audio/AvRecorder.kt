package com.offgrid.compositor.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Records the finished picture and the audio into one MP4: H.264 video, AAC-LC audio at 128 kbps.
 *
 * The compositor draws into [surface] and calls [frameDrawn] for every picture. The first picture
 * is time zero for the video, and the audio clock starts at the same moment. Audio time comes from
 * counting frames, so the sound never drifts from its own length. The mic goes in through [micSink].
 * While the mic is off, the audio track holds silence, so the file always has the same two tracks.
 */
class AvRecorder(
    private val file: File,
    val width: Int,
    val height: Int,
    private val sounds: SoundMixer?,
    private val onFinished: (Boolean) -> Unit,
    sink: MuxerSink = FileMuxerSink(file.absolutePath),
) {
    private val writer = Mp4Writer(sink)
    private val micRing = ShortRing(MIC_RING_FRAMES)
    private val feeder = AudioFeeder(micRing, sounds)
    private val videoThread = HandlerThread("OffgridAvVideo").apply { start() }
    private val videoCodec: MediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    private val audioCodec: MediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    private val firstFrame = CountDownLatch(1)
    private val videoDone = CountDownLatch(1)
    private val stopping = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    @Volatile
    private var firstStampUs = NOT_SET

    @Volatile
    private var anchorNanos = 0L
    private var videoTrack = -1
    private var audioTrack = -1

    /** The video encoder's input. The compositor draws every finished picture into it. */
    lateinit var surface: Surface
        private set

    /** Gives the recorded audio the processed mic. */
    val micSink = PcmSink { block, frames -> micRing.write(block, frames) }

    private val videoCallback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) = Unit

        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            try {
                val buffer = codec.getOutputBuffer(index)
                val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                if (buffer != null && !isConfig && info.size > 0 && videoTrack >= 0) {
                    info.presentationTimeUs = maxOf(0L, info.presentationTimeUs - firstStampUs)
                    writer.write(videoTrack, buffer, info)
                }
                codec.releaseOutputBuffer(index, false)
            } catch (error: IllegalStateException) {
                Log.w(TAG, "A video piece was dropped", error)
            }
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) videoDone.countDown()
        }

        override fun onError(codec: MediaCodec, error: MediaCodec.CodecException) {
            Log.e(TAG, "The video encoder stopped", error)
            videoDone.countDown()
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            videoTrack = writer.addTrack(format)
        }
    }

    init {
        try {
            videoCodec.setCallback(videoCallback, Handler(videoThread.looper))
            videoCodec.configure(videoFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            surface = videoCodec.createInputSurface()
            audioCodec.configure(audioFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoCodec.start()
            audioCodec.start()
        } catch (error: RuntimeException) {
            closeCodecs()
            throw error
        }
        Thread({ runAudio() }, "OffgridAvAudio").start()
    }

    /** Call on the drawing thread for every picture, with the time given to the picture. */
    fun frameDrawn(stampNanos: Long) {
        if (firstStampUs != NOT_SET) return
        firstStampUs = stampNanos / NANOS_PER_MICRO
        anchorNanos = System.nanoTime()
        firstFrame.countDown()
    }

    /** Stops after the last picture was drawn. The result arrives through the finished callback, on another thread. */
    fun stop() {
        if (!stopping.compareAndSet(false, true)) return
        firstFrame.countDown()
        try {
            videoCodec.signalEndOfInputStream()
        } catch (error: IllegalStateException) {
            Log.w(TAG, "The video encoder was already closed", error)
            videoDone.countDown()
        }
    }

    private fun runAudio() {
        var ok = false
        try {
            firstFrame.await()
            sounds?.listening = true
            ok = encodeAudio() && videoDone.await(FINISH_WAIT_SECONDS, TimeUnit.SECONDS)
        } catch (error: InterruptedException) {
            Log.w(TAG, "Audio was interrupted", error)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Audio encoding failed", error)
        } finally {
            sounds?.listening = false
            sounds?.clear()
            val whole = writer.finish() && ok
            closeCodecs()
            onFinished(whole)
        }
    }

    /** Feeds the clock-paced blocks to the AAC encoder until stopped. Returns false if the first picture never came. */
    private fun encodeAudio(): Boolean {
        if (firstStampUs == NOT_SET) return false
        val block = ShortArray(AAC_FRAMES)
        val bytes = ByteBuffer.allocate(AAC_FRAMES * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val info = MediaCodec.BufferInfo()
        var framesDone = 0L
        while (!stopping.get()) {
            val due = (System.nanoTime() - anchorNanos) * SAMPLE_RATE / NANOS_PER_SECOND - framesDone
            if (due < AAC_FRAMES || !feeder.fill(block, due)) {
                drainAudio(info)
                Thread.sleep(POLL_MS)
                continue
            }
            bytes.clear()
            bytes.asShortBuffer().put(block)
            queueAudio(bytes, AAC_FRAMES * 2, framesDone * MICROS_PER_SECOND / SAMPLE_RATE, 0)
            framesDone += AAC_FRAMES
            drainAudio(info)
        }
        queueAudio(bytes, 0, framesDone * MICROS_PER_SECOND / SAMPLE_RATE, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        while (!drainAudio(info)) Thread.sleep(POLL_MS)
        return true
    }

    private fun queueAudio(bytes: ByteBuffer, size: Int, timeUs: Long, flags: Int) {
        val deadline = System.nanoTime() + FINISH_WAIT_SECONDS * NANOS_PER_SECOND
        while (System.nanoTime() < deadline) {
            val index = audioCodec.dequeueInputBuffer(INPUT_WAIT_US)
            if (index < 0) continue
            val input = audioCodec.getInputBuffer(index) ?: return
            input.clear()
            bytes.position(0)
            bytes.limit(size)
            input.put(bytes)
            audioCodec.queueInputBuffer(index, 0, size, timeUs, flags)
            return
        }
        Log.w(TAG, "The audio encoder did not take a block")
    }

    /** Writes every finished audio piece. Returns true once the end of the audio was reached. */
    private fun drainAudio(info: MediaCodec.BufferInfo): Boolean {
        while (true) {
            val index = audioCodec.dequeueOutputBuffer(info, 0)
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> audioTrack = writer.addTrack(audioCodec.outputFormat)
                index < 0 -> return false
                else -> {
                    val buffer = audioCodec.getOutputBuffer(index)
                    val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    if (buffer != null && !isConfig && info.size > 0 && audioTrack >= 0) writer.write(audioTrack, buffer, info)
                    audioCodec.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return true
                }
            }
        }
    }

    private fun closeCodecs() {
        if (!closed.compareAndSet(false, true)) return
        for (codec in listOf(videoCodec, audioCodec)) {
            try {
                codec.stop()
            } catch (error: IllegalStateException) {
                Log.w(TAG, "An encoder was already stopped", error)
            }
            codec.release()
        }
        if (::surface.isInitialized) surface.release()
        videoThread.quitSafely()
    }

    private fun videoFormat(): MediaFormat =
        MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

    private fun audioFormat(): MediaFormat =
        MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AAC_FRAMES * 2)
        }

    private companion object {
        const val TAG = "OffgridAv"
        const val SAMPLE_RATE = VoiceFx.SAMPLE_RATE
        const val AAC_FRAMES = 1024
        const val VIDEO_BIT_RATE = 6_000_000
        const val AUDIO_BIT_RATE = 128_000
        const val FRAME_RATE = 30
        const val MIC_RING_FRAMES = SAMPLE_RATE
        const val NOT_SET = Long.MIN_VALUE
        const val NANOS_PER_MICRO = 1_000L
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val MICROS_PER_SECOND = 1_000_000L
        const val POLL_MS = 4L
        const val INPUT_WAIT_US = 10_000L
        const val FINISH_WAIT_SECONDS = 3L
    }
}
