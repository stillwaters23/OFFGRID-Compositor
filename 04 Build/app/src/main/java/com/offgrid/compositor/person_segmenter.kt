package com.offgrid.compositor

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.framework.image.MediaImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.offgrid.compositor.effects.EffectEngine
import com.offgrid.compositor.effects.FxKeys
import java.nio.ByteOrder
import java.util.concurrent.Executor

/**
 * On-device person cutout for the video box (Lane E).
 *
 * MediaPipe Tasks Image Segmenter is the only model. It is the selfie segmenter, which
 * marks the person. Frames stay on the phone. Nothing is uploaded.
 *
 * The model loads and runs on [thread], never the UI thread. The GPU stage copies the
 * mask on its own drawing thread. The camera keeps asking for 30fps. A small analysis
 * picture and keep-only-latest mean a slow mask is dropped instead of piling up. If a
 * mask does not arrive inside two frames, [MaskGate] turns the box black.
 */
internal class PersonSegmenter(
    context: Context,
    private val engine: EffectEngine,
    private val masks: MaskBus,
) {
    private val appContext = context.applicationContext
    private val thread = HandlerThread("OffgridSegmenter")
    private val handler: Handler
    val executor: Executor

    @Volatile
    private var segmenter: ImageSegmenter? = null

    private var lastStampMs = Long.MIN_VALUE

    init {
        thread.start()
        handler = Handler(thread.looper)
        executor = Executor { command ->
            if (!handler.post(command)) {
                Log.w(TAG, "Segmentation thread is stopped")
            }
        }
        handler.post { openModel() }
    }

    /** Hands camera frames to this segmenter. [analysis] must use keep-only-latest. */
    fun attach(analysis: ImageAnalysis) {
        analysis.setAnalyzer(executor, this::analyze)
    }

    fun release() {
        handler.post {
            try {
                segmenter?.close()
            } catch (error: Exception) {
                Log.w(TAG, "The person segmenter did not close", error)
            }
            segmenter = null
            masks.clear()
            thread.quitSafely()
        }
    }

    private fun openModel() {
        if (onUiThread()) {
            Log.e(TAG, "Refusing to load the person segmenter on the UI thread")
            return
        }
        try {
            // CPU, not the GPU delegate. The compositor already owns a GL context on its
            // drawing thread. A second GL context from the segmenter fights that context.
            // The selfie model is small. On an S25 Ultra it finishes inside one 30fps frame.
            val base = BaseOptions.builder()
                .setModelAssetPath(MODEL)
                .setDelegate(Delegate.CPU)
                .build()
            val options = ImageSegmenter.ImageSegmenterOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(RunningMode.VIDEO)
                .setOutputConfidenceMasks(true)
                .setOutputCategoryMask(false)
                .build()
            segmenter = ImageSegmenter.createFromOptions(appContext, options)
            Log.i(TAG, "Person segmenter ready")
        } catch (error: Exception) {
            Log.e(TAG, "The person segmenter did not start. Body modes stay black.", error)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyze(image: ImageProxy) {
        try {
            if (onUiThread()) {
                Log.e(TAG, "Person segmentation refused to run on the UI thread")
                return
            }
            val running = segmenter ?: return
            if (!MaskGate.hidesFace(engine.settings.choice(FxKeys.BODY_MODE))) return
            val media = image.image ?: return
            val mpImage = MediaImageBuilder(media).build()
            try {
                val options = ImageProcessingOptions.builder()
                    .setRotationDegrees(image.imageInfo.rotationDegrees)
                    .build()
                val started = System.nanoTime()
                val result = running.segmentForVideo(mpImage, options, nextStampMs(image.imageInfo.timestamp))
                val took = System.nanoTime() - started
                if (!MaskGate.holdsFrameBudget(took)) {
                    Log.w(TAG, "Segmentation took ${took / 1_000_000} ms, past one 30fps frame")
                }
                val mask = result.confidenceMasks().orElse(null)?.firstOrNull()
                if (mask == null) {
                    Log.w(TAG, "Segmentation returned no person mask")
                    return
                }
                try {
                    publish(mask)
                } finally {
                    mask.close()
                }
            } finally {
                mpImage.close()
            }
        } catch (error: Exception) {
            // No new mask. The last one ages out and the box goes black.
            Log.e(TAG, "Person segmentation dropped", error)
        } finally {
            image.close()
        }
    }

    private fun publish(mask: MPImage) {
        val width = mask.width
        val height = mask.height
        if (width <= 0 || height <= 0) return
        val count = width * height
        val raw = ByteBufferExtractor.extract(mask)
        raw.order(ByteOrder.nativeOrder())
        // A sliced buffer is already sitting on the mask. Anything else is rewound.
        if (raw.position() == 0 || raw.remaining() < count) raw.position(0)
        val floats = FloatArray(count)
        val format = mask.containedImageProperties.firstOrNull()?.imageFormat
        if (format == MPImage.IMAGE_FORMAT_VEC32F1 || raw.remaining() >= count * 4) {
            if (raw.remaining() < count * 4) return
            raw.asFloatBuffer().get(floats)
        } else if (raw.remaining() >= count) {
            for (i in 0 until count) {
                floats[i] = (raw.get().toInt() and 0xFF) / 255f
            }
        } else {
            return
        }
        val packed = MaskPack.pack(floats, width, height) ?: return
        masks.publish(packed, width, height, System.nanoTime())
    }

    /** MediaPipe video mode needs a clock that only moves forward. */
    private fun nextStampMs(cameraNs: Long): Long {
        var stamp = cameraNs / 1_000_000L
        if (stamp <= lastStampMs) stamp = lastStampMs + FRAME_MS
        if (stamp <= lastStampMs) stamp = lastStampMs + 1
        lastStampMs = stamp
        return stamp
    }

    private fun onUiThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    private companion object {
        const val TAG = "OffgridSegment"
        const val MODEL = "selfie_segmenter.tflite"
        const val FRAME_MS = 1000L / MaskGate.TARGET_FPS
    }
}
