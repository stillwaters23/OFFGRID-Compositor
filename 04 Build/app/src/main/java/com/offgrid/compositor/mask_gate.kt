package com.offgrid.compositor

import com.offgrid.compositor.effects.FxKeys

/**
 * Decides whether a person mask is safe to draw (Lane E).
 *
 * A body mode hides the face by cutting the person out of the video box. If the mask is
 * missing, the wrong size, or older than two frames at 30fps, the picture must go black.
 * A late or empty mask is never treated as "no person", because that would show the face.
 *
 * This is plain math. The segmenter and the GPU stage both follow it, and tests check it
 * without a phone.
 */
internal enum class MaskVerdict {
    /** The mask is fresh. The body look may use it. */
    APPLY,

    /** The mask cannot be trusted. Draw black, so the face never shows. */
    FAIL_CLOSED,
}

/**
 * One person mask, already packed for the GPU. [bytes] is one byte per pixel, 0 to 255,
 * with the first row at the bottom so it matches an OpenGL texture. [publishedAtNs] is
 * [System.nanoTime] when the segmenter finished this mask.
 */
internal class MaskFrame(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val publishedAtNs: Long,
)

/** The newest mask. The segmenter publishes it. The drawing thread reads it. */
internal class MaskBus {
    @Volatile
    private var latest: MaskFrame? = null

    fun publish(bytes: ByteArray, width: Int, height: Int, publishedAtNs: Long) {
        latest = MaskFrame(bytes.copyOf(), width, height, publishedAtNs)
    }

    fun latest(): MaskFrame? = latest

    fun clear() {
        latest = null
    }
}

internal object MaskGate {
    /** The picture rate the camera asks for, and the rate the mask must keep up with. */
    const val TARGET_FPS = 30

    /** One frame at [TARGET_FPS], in nanoseconds. */
    const val FRAME_NS = 1_000_000_000L / TARGET_FPS

    /**
     * A mask from the frame before is still fresh, so one frame of pipeline delay does not
     * flash black. Older than two frames means segmentation dropped.
     */
    const val STALE_AFTER_NS = FRAME_NS * 2

    /** True when [bodyMode] cuts the person out, so a missing mask must hide the picture. */
    fun hidesFace(bodyMode: String): Boolean = when (bodyMode) {
        FxKeys.BODY_SEE, FxKeys.BODY_REPLACE, FxKeys.BODY_CLOAK, FxKeys.BODY_HOLO -> true
        else -> false
    }

    /**
     * [nowNs] is [System.nanoTime] on the drawing thread, the same clock as [MaskFrame.publishedAtNs].
     */
    fun verdict(frame: MaskFrame?, nowNs: Long): MaskVerdict {
        if (frame == null) return MaskVerdict.FAIL_CLOSED
        if (frame.width <= 0 || frame.height <= 0) return MaskVerdict.FAIL_CLOSED
        val pixels = frame.width.toLong() * frame.height.toLong()
        if (pixels <= 0L || pixels > frame.bytes.size) return MaskVerdict.FAIL_CLOSED
        if (frame.publishedAtNs > nowNs) return MaskVerdict.FAIL_CLOSED
        val age = nowNs - frame.publishedAtNs
        if (age > STALE_AFTER_NS) return MaskVerdict.FAIL_CLOSED
        return MaskVerdict.APPLY
    }

    /** True when one segmentation finished inside a single frame, so 30fps can hold. */
    fun holdsFrameBudget(inferenceNs: Long): Boolean = inferenceNs in 0..FRAME_NS

    /** Segmentation and GPU copies must not run on the UI thread. */
    fun workStaysOffUiThread(onUiThread: Boolean): Boolean = !onUiThread
}

/**
 * Packs a confidence mask for the GPU. MediaPipe's first row is the top of the picture.
 * OpenGL's first row is the bottom, so this flips it. Values outside 0 to 1, and NaN,
 * become 0 so a bad sample never counts as the person.
 */
internal object MaskPack {
    fun pack(confidence: FloatArray, width: Int, height: Int): ByteArray? {
        if (width <= 0 || height <= 0) return null
        val count = width.toLong() * height.toLong()
        if (count <= 0L || count > confidence.size || count > Int.MAX_VALUE) return null
        val out = ByteArray(count.toInt())
        for (y in 0 until height) {
            val src = y * width
            val dst = (height - 1 - y) * width
            for (x in 0 until width) {
                val sample = confidence[src + x]
                val clamped = if (sample.isNaN()) 0f else sample.coerceIn(0f, 1f)
                out[dst + x] = (clamped * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
            }
        }
        return out
    }
}
