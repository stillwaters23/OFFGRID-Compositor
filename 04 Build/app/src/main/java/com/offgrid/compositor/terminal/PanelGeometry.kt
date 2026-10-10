package com.offgrid.compositor.terminal

import kotlin.math.pow
import kotlin.math.roundToInt

internal enum class PanelResizeZone { NONE, BOTTOM_EDGE, BOTTOM_LEFT, BOTTOM_RIGHT }

/** How far the words and buttons move in from a screen edge, in pixels. */
internal data class PanelContentInset(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal object PanelGeometry {
    const val RESIZE_BAR_HEIGHT_DP = 6f
    const val RESIZE_BAR_INSET_DP = 16f
    const val RESIZE_BAR_CAP_RADIUS_DP = 3f

    /** Share of a round screen corner that the words and buttons stay out of. */
    const val CONTENT_CORNER_SHARE = 0.3f

    fun isFullBleed(width: Int, height: Int, fullScreen: Boolean): Boolean =
        fullScreen || (width == 100 && height == 100)

    fun sizeRange(available: Int, minimum: Int): Pair<Int, Int> {
        val maximum = available.coerceAtLeast(0)
        return minOf(minimum.coerceAtLeast(0), maximum) to maximum
    }

    fun sizeFor(percent: Int, range: Pair<Int, Int>): Int {
        val minimum = range.first.coerceAtLeast(0)
        val maximum = maxOf(minimum, range.second)
        return minimum + ((maximum - minimum) * percent.coerceIn(0, 100) / 100f).roundToInt()
    }

    fun percentFor(size: Float, range: Pair<Int, Int>): Int {
        if (range.second <= range.first) return 100
        return ((size - range.first) / (range.second - range.first) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }

    fun borderStrokeDp(value: Int): Float {
        val amount = value.coerceIn(0, 100)
        return if (amount <= 50) 2f + amount / 50f else 3f + (amount - 50) * 3f / 50f
    }

    fun borderInsetDp(value: Int): Float = borderStrokeDp(value) / 2f + 1f

    fun borderCornerRadius(radius: Float, inset: Float): Float = (radius - inset).coerceAtLeast(0f)

    fun resizeHeight(start: Float, dy: Float, minimum: Float, maximum: Float): Float =
        (start + dy).coerceIn(minimum.coerceAtLeast(0f), maximum.coerceAtLeast(minimum))

    fun resizeWidth(start: Float, dx: Float, minimum: Float, maximum: Float): Float =
        (start + dx).coerceIn(minimum.coerceAtLeast(0f), maximum.coerceAtLeast(minimum))

    fun resizeWidthDelta(zone: PanelResizeZone, dx: Float): Float =
        if (zone == PanelResizeZone.BOTTOM_EDGE) 0f else dx

    fun resizeBarGlowRadiusDp(value: Int): Float {
        val amount = value.coerceIn(0, 100)
        return if (amount <= 50) 24f * amount / 50f else 24f + 8f * (amount - 50) / 50f
    }

    fun resizeBarLayerAlpha(value: Int, layer: Int): Float {
        val base = when (layer) {
            0 -> 0.2f
            1 -> 0.16f
            else -> 0.12f
        }
        val amount = value.coerceIn(0, 100)
        val scale = if (amount <= 50) amount / 50f else 1f + 1.5f * (amount - 50) / 50f
        return base * scale
    }

    fun resizeBarLayerAlphaSum(value: Int): Float =
        resizeBarLayerAlpha(value, 0) + resizeBarLayerAlpha(value, 1) + resizeBarLayerAlpha(value, 2)

    fun pulseScale(breath: Float): Float = 0.85f + 0.3f * breath.coerceIn(0f, 1f)

    fun glowStrength(value: Int): Float {
        val amount = value.coerceIn(0, 100) / 100f
        return if (amount <= 0.5f) 1.4f * amount else 0.7f + 0.3f * ((amount - 0.5f) * 2f).pow(1.4f)
    }

    fun glowBoost(value: Int): Float = ((value.coerceIn(0, 100) - 50).coerceAtLeast(0) / 50f)

    fun glowBloom(value: Int): Float {
        val aboveNormal = ((value.coerceIn(0, 100) - 50).coerceAtLeast(0) / 50f)
        return 1.5f * aboveNormal * aboveNormal
    }

    fun resizeZone(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        depth: Float,
        gestureLeft: Float = 0f,
        gestureRight: Float = 0f,
        gestureBottom: Float = 0f,
    ): PanelResizeZone {
        if (width <= 0f || height <= 0f || depth <= 0f) return PanelResizeZone.NONE
        val left = gestureLeft.coerceIn(0f, width)
        val right = (width - gestureRight).coerceIn(left, width)
        val bottom = (height - gestureBottom).coerceIn(0f, height)
        if (x < left || x >= right || y < bottom - depth || y >= bottom) {
            return PanelResizeZone.NONE
        }
        return when {
            x < left + depth -> PanelResizeZone.BOTTOM_LEFT
            x >= right - depth -> PanelResizeZone.BOTTOM_RIGHT
            else -> PanelResizeZone.BOTTOM_EDGE
        }
    }

    /**
     * Words and buttons stay clear of the status bar, the camera cutout, the gesture
     * bar, and the round corners. The glass itself still reaches the screen edge.
     * [edgeTop] already includes the taller of the status bar and the camera cutout.
     */
    fun contentInset(
        glassLeft: Int,
        glassTop: Int,
        glassRight: Int,
        glassBottom: Int,
        areaWidth: Int,
        areaHeight: Int,
        edgeTop: Int,
        edgeBottom: Int,
        edgeLeft: Int,
        edgeRight: Int,
        gestureBottom: Int,
        cornerRadius: Int,
        fullBleed: Boolean,
    ): PanelContentInset {
        val corner = (cornerRadius.coerceAtLeast(0) * CONTENT_CORNER_SHARE).roundToInt()
        val topSafe = if (fullBleed) maxOf(edgeTop, corner) else edgeTop.coerceAtLeast(0)
        val bottomSafe = if (fullBleed) {
            maxOf(edgeBottom, gestureBottom, corner)
        } else {
            maxOf(edgeBottom, gestureBottom)
        }
        val leftSafe = maxOf(edgeLeft, corner)
        val rightSafe = maxOf(edgeRight, corner)
        return PanelContentInset(
            left = (leftSafe - glassLeft).coerceAtLeast(0),
            top = (topSafe - glassTop).coerceAtLeast(0),
            right = (glassRight - (areaWidth - rightSafe)).coerceAtLeast(0),
            bottom = (glassBottom - (areaHeight - bottomSafe)).coerceAtLeast(0),
        )
    }

    /** The thick bottom bar stays inside the round corner as well as the 16 dp side inset. */
    fun resizeBarEndInsetPx(cornerRadiusPx: Float, density: Float): Float {
        val floor = RESIZE_BAR_INSET_DP * density.coerceAtLeast(0f)
        return maxOf(floor, cornerRadiusPx.coerceAtLeast(0f))
    }

    fun scrollThumbHeight(height: Int, total: Int, minimumHeight: Int): Float {
        if (height <= 0 || total <= height) return height.toFloat()
        return (height * height.toFloat() / total)
            .coerceAtLeast(minimumHeight.toFloat())
            .coerceAtMost(height.toFloat())
    }

    fun scrollThumbTop(height: Int, total: Int, scrollY: Int, thumbHeight: Float): Float {
        if (height <= 0 || total <= height) return 0f
        val travel = (height - thumbHeight).coerceAtLeast(0f)
        val maxScroll = (total - height).coerceAtLeast(1)
        return travel * scrollY.coerceIn(0, maxScroll) / maxScroll
    }
}
