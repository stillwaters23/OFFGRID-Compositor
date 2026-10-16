package com.offgrid.compositor

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Phase 0.5 render probe. A silent, debug-only self-check for the stretch bug.
 *
 * On a debuggable build it writes one logcat line (tag [TAG]) for each of the numbers the
 * Architect Review asks for: the PreviewView size and view port at bind, the SurfaceRequest
 * resolution, every output's size and targets, the stage size, the clip's VideoSize, and the
 * PreviewView's TextureView transform for the first [FRAMES_PER_BIND] frames after each bind.
 *
 * On a release build [enabled] is false and every call returns at once. The probe never draws
 * and never changes the picture. Read it with: adb logcat -s OffgridProbe
 */
internal class RenderProbe(
    val enabled: Boolean,
    private val sink: (String) -> Unit = { Log.d(TAG, it) },
    private val onMain: (Runnable) -> Unit = MAIN_POST,
) {
    private val lock = Any()
    private var bind = 0
    private var framesLeft = 0

    /** Sampled on the main thread for each probed frame: the TextureView transform and sizes. */
    @Volatile
    var previewSampler: (() -> String)? = null

    /** The bind just started. [generation] is MainActivity.bindGeneration. Main thread. */
    fun onBind(generation: Int, previewWidth: Int, previewHeight: Int, viewPort: String, rotation: Int) {
        if (!enabled) return
        synchronized(lock) {
            bind = generation
            framesLeft = FRAMES_PER_BIND
        }
        emit("bind $generation preview=${previewWidth}x$previewHeight viewPort=$viewPort rotation=$rotation")
    }

    /** The camera stream the SurfaceRequest asked for. Drawing thread. */
    fun onInput(width: Int, height: Int) {
        if (!enabled) return
        emit("bind ${current()} surfaceRequest=${width}x$height")
    }

    /** One CameraX output surface arrived. Drawing thread. */
    fun onOutput(width: Int, height: Int, targets: Int, preview: Boolean) {
        if (!enabled) return
        emit("bind ${current()} output=${width}x$height targets=$targets preview=$preview")
    }

    /** The stage was sized from the main output. Drawing thread. Quiet unless the size changed. */
    fun onStage(mainWidth: Int, mainHeight: Int, mainPreview: Boolean, stageWidth: Int, stageHeight: Int) {
        if (!enabled) return
        val key = "${mainWidth}x$mainHeight/$mainPreview/${stageWidth}x$stageHeight"
        synchronized(lock) {
            if (key == lastStage) return
            lastStage = key
        }
        emit("bind ${current()} main=${mainWidth}x$mainHeight previewMaster=$mainPreview stage=${stageWidth}x$stageHeight")
    }

    /** The AR clip's real size from Media3. Drawing thread. */
    fun onClipSize(width: Int, height: Int, pixelRatio: Float) {
        if (!enabled) return
        emit("bind ${current()} videoSize=${width}x$height pixelRatio=$pixelRatio")
    }

    /**
     * One finished frame reached a preview output. Logs only the first [FRAMES_PER_BIND]
     * frames after each bind, and asks the main thread for the TextureView transform each time.
     * Drawing thread.
     */
    fun onPreviewFrame(outputWidth: Int, outputHeight: Int, stageWidth: Int, stageHeight: Int) {
        if (!enabled) return
        val index: Int
        val generation: Int
        synchronized(lock) {
            if (framesLeft <= 0) return
            index = FRAMES_PER_BIND - framesLeft + 1
            framesLeft--
            generation = bind
        }
        emit("bind $generation frame $index/$FRAMES_PER_BIND output=${outputWidth}x$outputHeight stage=${stageWidth}x$stageHeight")
        onMain(Runnable {
            val sample = previewSampler?.invoke() ?: return@Runnable
            emit("bind $generation frame $index/$FRAMES_PER_BIND $sample")
        })
    }

    private var lastStage = ""

    private fun current(): Int = synchronized(lock) { bind }

    private fun emit(line: String) {
        try {
            sink(line)
        } catch (error: RuntimeException) {
            // A log line is never worth a crash.
        }
    }

    companion object {
        const val TAG = "OffgridProbe"
        const val FRAMES_PER_BIND = 3

        private val MAIN_POST: (Runnable) -> Unit = { task -> Handler(Looper.getMainLooper()).post(task) }

        /** True on a debuggable (debug) build, never on a release build. */
        fun isDebugBuild(context: Context?): Boolean =
            context != null && (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        fun forContext(context: Context?): RenderProbe = RenderProbe(isDebugBuild(context))
    }
}
