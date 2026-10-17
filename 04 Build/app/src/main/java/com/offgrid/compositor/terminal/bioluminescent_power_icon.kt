package com.offgrid.compositor.terminal

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * The panel's on and off control, drawn in code.
 *
 * A black disc holds a power symbol. Lit controls get a near white core, a saturated
 * accent ring, and a wide soft halo. The blurred layers are built once per size and
 * color, then stamped each frame, so the glow does not blur again while the panel moves.
 */
internal object BioluminescentPowerIcon {
    /** Disc radius as a share of the square bitmap, leaving room for the halo. */
    const val DISC_SHARE = 0.30f

    private const val HALO_DP = 14f
    private const val MID_BLUR_DP = 6f
    private const val CACHE_LIMIT = 8

    class Layers(
        val halo: Bitmap,
        val mid: Bitmap,
        val core: Bitmap,
    )

    private val cache = object : LinkedHashMap<String, Layers>(CACHE_LIMIT, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Layers>?) = size > CACHE_LIMIT
    }

    /** Accent pulled most of the way to white, so the core reads as hot light. */
    fun hotCore(accent: Int): Int {
        fun channel(value: Int) = (value + (255 - value) * 0.85f).toInt().coerceIn(0, 255)
        val red = channel((accent shr 16) and 0xFF)
        val green = channel((accent shr 8) and 0xFF)
        val blue = channel(accent and 0xFF)
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }

    fun layers(sizePx: Int, accent: Int, haloColor: Int, density: Float): Layers? {
        if (sizePx < 8) return null
        val key = "$sizePx:$accent:$haloColor:${density.toRawBits()}"
        cache[key]?.let { return it }
        val halo = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val mid = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val core = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val radius = sizePx * DISC_SHARE
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        paint.strokeWidth = (2.5f * density).coerceAtLeast(1f)
        paint.color = haloColor
        paint.alpha = 90
        paint.maskFilter = BlurMaskFilter((HALO_DP * density).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        Canvas(halo).drawCircle(cx, cy, radius, paint)
        paint.color = accent
        paint.alpha = 180
        paint.maskFilter = BlurMaskFilter((MID_BLUR_DP * density).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        Canvas(mid).drawCircle(cx, cy, radius, paint)
        paint.color = hotCore(accent)
        paint.alpha = 255
        paint.maskFilter = null
        Canvas(core).drawCircle(cx, cy, radius, paint)
        val made = Layers(halo, mid, core)
        cache[key] = made
        return made
    }

    fun paintLayers(canvas: Canvas, left: Float, top: Float, layers: Layers, lit: Float, paint: Paint) {
        val amount = lit.coerceIn(0f, 1f)
        if (amount <= 0f) return
        val dest = RectF(left, top, left + layers.core.width, top + layers.core.height)
        paint.alpha = (90f * amount).toInt().coerceIn(0, 255)
        canvas.drawBitmap(layers.halo, null, dest, paint)
        paint.alpha = (170f * amount).toInt().coerceIn(0, 255)
        canvas.drawBitmap(layers.mid, null, dest, paint)
        paint.alpha = (255f * amount).toInt().coerceIn(0, 255)
        canvas.drawBitmap(layers.core, null, dest, paint)
        paint.alpha = 255
    }

    /** The power symbol: an open ring with a stem in the gap. */
    fun drawPowerSymbol(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint, box: RectF) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = radius * 0.22f
        val arc = radius * 0.62f
        box.set(cx - arc, cy - arc + radius * 0.08f, cx + arc, cy + arc + radius * 0.08f)
        canvas.drawArc(box, 305f, 290f, false, paint)
        canvas.drawLine(cx, cy - radius * 0.78f, cx, cy - radius * 0.08f, paint)
    }
}
