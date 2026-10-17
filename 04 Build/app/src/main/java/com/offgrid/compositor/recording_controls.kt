package com.offgrid.compositor

import android.util.Log
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.offgrid.compositor.effects.FxKeys
import java.io.File

private const val RECORD_TAG = "OffgridCompositor"
private const val RECORD_TEMP_NAME = "recording-now.mp4"
private const val RECORD_FALLBACK_EXTRA_MS = 3_000L
private val RECORD_KEPT_ERRORS = setOf(
    VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED,
    VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED,
    VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE,
    VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE,
)

internal fun MainActivity.toggleRecording() {
        if (recording) {
            // While recording, the same button is Stop.
            requestStop()
            return
        }
        val painter = processor ?: return
        val temp = File(filesDir, RECORD_TEMP_NAME)
        if (temp.exists() && !temp.delete()) {
            showMessage(R.string.record_failed, MainActivity.ButtonMode.LIVE)
            return
        }
        stopRequested = false
        diamondInVideo = terminalPanel.testDiamondOn
        val session = avSession ?: AvSession(painter, mic.engine, effects.videoSounds).also { avSession = it }
        if (session.start(temp) { whole -> onRecordingFinished(temp, whole) }) {
            beginRecording()
            val seconds = recordSeconds()
            if (seconds > 0) {
                val stopTask = Runnable { requestStop() }
                stopRecordingTask = stopTask
                mainHandler.postDelayed(stopTask, seconds * 1000L)
            }
        } else {
            startFallbackRecording(temp)
        }
    }

internal fun MainActivity.beginRecording() {
        recording = true
        // The coach mark never stays up while recording (Phase 1.2).
        coachMark.hide()
        // RECORDING mode types the timed words itself when a length is set (Phase 1.2).
        showMessage(R.string.recording_now, MainActivity.ButtonMode.RECORDING)
    }

    /** Records with the camera's own recorder when [AvSession] would not start. It has no sound. */
internal fun MainActivity.startFallbackRecording(temp: File) {
        val capture = videoCapture
        if (capture == null) {
            showMessage(R.string.record_failed, MainActivity.ButtonMode.LIVE)
            return
        }
        beginRecording()
        sayStatus(getString(R.string.record_no_sound))
        val options = FileOutputOptions.Builder(temp).build()
        val pending = capture.output.prepareRecording(this, options)
        val started = pending.start(ContextCompat.getMainExecutor(this)) { event ->
            onRecordEvent(event, temp)
        }
        activeRecording = started
        val seconds = recordSeconds()
        if (seconds > 0) {
            // A safety stop in case the recorder never says it started.
            val stopTask = Runnable { requestStop() }
            stopRecordingTask = stopTask
            mainHandler.postDelayed(stopTask, seconds * 1000L + RECORD_FALLBACK_EXTRA_MS)
        }
    }

    /** The typed record length in seconds. 0 means record until Stop. */
internal fun MainActivity.recordSeconds(): Int = effects.engine.settings.raw(FxKeys.RECORD_SECONDS)

internal fun MainActivity.requestStop() {
        if (stopRequested) {
            return
        }
        stopRequested = true
        if (avSession?.active == true) avSession?.stop() else activeRecording?.stop()
    }

internal fun MainActivity.onRecordEvent(event: VideoRecordEvent, temp: File) {
        when (event) {
            is VideoRecordEvent.Start -> {
                stopRecordingTask?.let { mainHandler.removeCallbacks(it) }
                stopRecordingTask = null
                val seconds = recordSeconds()
                if (seconds > 0) {
                    val stopTask = Runnable { requestStop() }
                    stopRecordingTask = stopTask
                    mainHandler.postDelayed(stopTask, seconds * 1000L)
                }
            }
            is VideoRecordEvent.Finalize -> {
                stopRecordingTask?.let { mainHandler.removeCallbacks(it) }
                stopRecordingTask = null
                activeRecording = null
                recording = false
                // Some errors still leave a whole, playable video, like a full phone or the camera
                // closing when the app goes to the background. Those videos are kept.
                val kept = !event.hasError() || (event.error in RECORD_KEPT_ERRORS && temp.exists() && temp.length() > 0L)
                if (event.hasError()) Log.e(RECORD_TAG, "Recording ended with code ${event.error}, kept: $kept")
                finishRecording(temp, kept)
            }
            else -> Unit
        }
    }

internal fun MainActivity.onRecordingFinished(temp: File, whole: Boolean) {
        stopRecordingTask?.let { mainHandler.removeCallbacks(it) }
        stopRecordingTask = null
        recording = false
        finishRecording(temp, whole && temp.exists() && temp.length() > 0L)
    }

    /** Moves a finished video into place and says so, or says it failed. */
internal fun MainActivity.finishRecording(temp: File, kept: Boolean) {
        if (!kept) {
            if (temp.exists()) {
                temp.delete()
            }
            showMessage(R.string.record_failed, MainActivity.ButtonMode.LIVE)
            return
        }
        val saved = recordingFile()
        if (saved.exists()) {
            saved.delete()
        }
        val moved = temp.renameTo(saved)
        if (!moved) {
            temp.copyTo(saved, overwrite = true)
            temp.delete()
        }
        showMessage(R.string.record_saved, MainActivity.ButtonMode.LIVE)
        if (effects.engine.settings.on(FxKeys.RECORD_GALLERY)) {
            GallerySaver.saveLater(this, saved) { name ->
                // Only change the words if they still say the video was saved.
                if (isDestroyed || shownMessage != R.string.record_saved) return@saveLater
                showMessage(
                    if (name != null) R.string.record_saved_gallery else R.string.record_gallery_failed,
                    MainActivity.ButtonMode.LIVE,
                )
            }
        }
    }

internal fun MainActivity.stopRecording() {
        stopRecordingTask?.let { mainHandler.removeCallbacks(it) }
        stopRecordingTask = null
        requestStop()
    }


