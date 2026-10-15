package com.offgrid.compositor.effects

import kotlin.math.abs

/**
 * The one place the stage's geometry is worked out (Phase 1.1, the "Seam Weld" of the picture
 * path: every surface is cut from the same cloth, so no seam shows between the screen, the
 * video, and the AR clip).
 *
 * Pure Kotlin, no Android types, so every rule is unit tested:
 *
 *  - [pickMain]: the PREVIEW output is the master for the stage. Never "the biggest output",
 *    because two equal-area outputs would otherwise be picked by arrival order.
 *  - [stageSize]: the stage keeps the master's shape, capped on its long side.
 *  - [fillCrop]: the centered part of a picture that has the target's shape (no bars, no stretch).
 *  - [fitRect]: the centered box in the target that has the picture's shape (bars, no stretch).
 *  - [aspectDiffers] and [RebindGate]: when the view's shape really changed, rebind once.
 *
 * Shares run 0 to 1. Every rect is (x, y, width, height). The scale of a fit or a fill is the
 * same on both axes by construction; [StageGeometryTest] checks that.
 */
object StageGeometry {
    /** A picture size in pixels. */
    data class Size(val width: Int, val height: Int) {
        val aspect: Float get() = aspectOf(width, height)
    }

    /** A box in shares of its container: left, top (or bottom in GL), width, height. */
    data class Rect(val x: Float, val y: Float, val width: Float, val height: Float) {
        val right: Float get() = x + width
        val top: Float get() = y + height
    }

    /** The result of a fit or fill: [rect] plus the uniform [scale] from source pixels to target pixels. */
    data class Placement(val rect: Rect, val scale: Float)

    /** One CameraX output, as much of it as the geometry needs. */
    data class Slot(val width: Int, val height: Int, val preview: Boolean) {
        val area: Long get() = width.toLong() * height.toLong()
    }

    /** Width over height, or 0 when a side is not known yet. */
    fun aspectOf(width: Int, height: Int): Float =
        if (width <= 0 || height <= 0) 0f else width.toFloat() / height.toFloat()

    /**
     * The output that sets the stage: the first PREVIEW output, in arrival order. Only when there
     * is no PREVIEW output (headless recording) does the biggest one win. Null for no outputs.
     */
    fun pickMain(slots: List<Slot>): Slot? =
        slots.firstOrNull { it.preview } ?: slots.maxByOrNull { it.area }

    /** Same rule for any slot type, so the compositor's own slots can use it without copying. */
    inline fun <T> pickMain(slots: List<T>, isPreview: (T) -> Boolean, width: (T) -> Int, height: (T) -> Int): T? =
        slots.firstOrNull(isPreview) ?: slots.maxByOrNull { width(it).toLong() * height(it).toLong() }

    /**
     * The stage picture's size for a master output of [mainWidth] by [mainHeight]: the same
     * shape, never longer than [maxSide] on its long side, never smaller than 2 by 2.
     */
    fun stageSize(mainWidth: Int, mainHeight: Int, maxSide: Float): Size {
        val longest = maxOf(mainWidth, mainHeight).toFloat()
        val scale = if (longest <= 0f) 1f else minOf(1f, maxSide / longest)
        return Size(maxOf(2, (mainWidth * scale).toInt()), maxOf(2, (mainHeight * scale).toInt()))
    }

    /**
     * Fill: the centered part of a [srcWidth] by [srcHeight] picture that has the shape of a
     * [dstWidth] by [dstHeight] target, as shares of the source (a texture rect). The target is
     * covered edge to edge and the scale is uniform. Unknown sizes give the whole picture.
     */
    fun fillCrop(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): Placement {
        val src = aspectOf(srcWidth, srcHeight)
        val dst = aspectOf(dstWidth, dstHeight)
        if (src == 0f || dst == 0f) return Placement(WHOLE, 1f)
        var w = 1f
        var h = 1f
        if (src > dst) w = dst / src else h = src / dst
        // Scale from source pixels to target pixels along the axis that is kept whole.
        val scale = if (src > dst) dstHeight.toFloat() / srcHeight else dstWidth.toFloat() / srcWidth
        return Placement(Rect((1f - w) / 2f, (1f - h) / 2f, w, h), scale)
    }

    /**
     * Fit: the centered box inside a [dstWidth] by [dstHeight] target that has the shape of a
     * [srcWidth] by [srcHeight] picture, as shares of the target (a position rect). The whole
     * picture shows, the rest of the target is left alone, and the scale is uniform.
     */
    fun fitRect(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): Placement {
        val src = aspectOf(srcWidth, srcHeight)
        val dst = aspectOf(dstWidth, dstHeight)
        if (src == 0f || dst == 0f) return Placement(WHOLE, 1f)
        var w = 1f
        var h = 1f
        if (src > dst) h = dst / src else w = src / dst
        val scale = if (src > dst) dstWidth.toFloat() / srcWidth else dstHeight.toFloat() / srcHeight
        return Placement(Rect((1f - w) / 2f, (1f - h) / 2f, w, h), scale)
    }

    /**
     * The 4 by 4 column-major matrix that maps the whole texture (0..1) onto [crop]. Multiply a
     * SurfaceTexture transform by this (transform × crop) to sample only the cropped part.
     */
    fun cropMatrix(crop: Rect): FloatArray = floatArrayOf(
        crop.width, 0f, 0f, 0f,
        0f, crop.height, 0f, 0f,
        0f, 0f, 1f, 0f,
        crop.x, crop.y, 0f, 1f,
    )

    /** True when two shapes differ by more than [tolerance] (1 percent by default) of the first. */
    fun aspectDiffers(width1: Int, height1: Int, width2: Int, height2: Int, tolerance: Float = ASPECT_TOLERANCE): Boolean {
        val a = aspectOf(width1, height1)
        val b = aspectOf(width2, height2)
        if (a == 0f || b == 0f) return false
        return abs(a - b) > a * tolerance
    }

    /**
     * Decides when a view size change needs the camera bound again. [bound] records the size
     * the camera was bound at. [onLayout] answers true exactly once for a new shape that differs
     * from the bound shape by more than [tolerance], and false for repeats of that same shape
     * until the next [bound]. A size change that keeps the shape (or a zero size) never rebinds.
     */
    class RebindGate(private val tolerance: Float = ASPECT_TOLERANCE) {
        var boundWidth = 0
            private set
        var boundHeight = 0
            private set
        private var askedWidth = 0
        private var askedHeight = 0

        fun bound(width: Int, height: Int) {
            boundWidth = width
            boundHeight = height
            askedWidth = 0
            askedHeight = 0
        }

        fun onLayout(width: Int, height: Int): Boolean {
            if (width <= 0 || height <= 0 || boundWidth <= 0 || boundHeight <= 0) return false
            if (!aspectDiffers(boundWidth, boundHeight, width, height, tolerance)) return false
            if (width == askedWidth && height == askedHeight) return false
            askedWidth = width
            askedHeight = height
            return true
        }
    }

    const val ASPECT_TOLERANCE = 0.01f
    val WHOLE = Rect(0f, 0f, 1f, 1f)
}
