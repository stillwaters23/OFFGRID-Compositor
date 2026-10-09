package com.offgrid.compositor

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.offgrid.compositor.audio.AvRecorder
import com.offgrid.compositor.audio.MicEngine
import com.offgrid.compositor.audio.SoundMixer
import java.io.File
import java.io.IOException

/** One recording through [AvRecorder]: the picture from the compositor, the mic with its effects, and the soundboard. */
internal class AvSession(
    private val processor: CompositorProcessor,
    private val mic: MicEngine,
    private val sounds: SoundMixer,
) {
    private val main = Handler(Looper.getMainLooper())
    private var recorder: AvRecorder? = null

    val active: Boolean get() = recorder != null

    /**
     * Starts recording to [file]. [onFinished] runs on the main thread with whether the file is whole.
     * Returns false when the phone's video or audio encoder would not start.
     */
    fun start(file: File, onFinished: (Boolean) -> Unit): Boolean {
        val size = processor.recordingSize
        val made = try {
            AvRecorder(file, size.width, size.height, sounds) { whole -> main.post { finish(whole, onFinished) } }
        } catch (error: IOException) {
            Log.e(TAG, "The recorder did not start", error)
            return false
        } catch (error: RuntimeException) {
            Log.e(TAG, "The recorder did not start", error)
            return false
        }
        recorder = made
        mic.addSink(made.micSink)
        processor.startRecorder(made)
        return true
    }

    /** Ends the picture feed. The recorder finishes the file and then calls back. */
    fun stop() {
        if (recorder != null) processor.stopRecorder()
    }

    private fun finish(whole: Boolean, onFinished: (Boolean) -> Unit) {
        recorder?.let { mic.removeSink(it.micSink) }
        recorder = null
        onFinished(whole)
    }

    private companion object {
        const val TAG = "OffgridAv"
    }
}
