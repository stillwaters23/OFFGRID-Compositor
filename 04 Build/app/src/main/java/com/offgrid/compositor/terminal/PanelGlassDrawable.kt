package com.offgrid.compositor.terminal

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.Drawable
import kotlin.random.Random

/**
 * The panel's glass.
 *
 * The glass sits [margin] inside the drawable's edges. That margin is room for the
 * outer glow. The soft glows are blurred once into small gray masks, then tinted
 * each frame, so the pulse and the swirl stay cheap on the phone.
 *
 * [pane] draws a lighter inner panel with no outer glow, for open folders.
 */
internal class PanelGlassDrawable(
    private val density: Float,
    private val margin: Float,
    private val pane: Boolean = false,
) : Drawable() {

    var look: PanelLook? = null
        set(value) {
            val reshape = field?.roundness != value?.roundness || field?.holo != value?.holo
            field = value
            if (reshape) rebuildShape()
            updateFill()
            invalidateSelf()
        }

    /** 0 to 1. How bright the breathing glow is right now. */
    var breath = 0.8f

    /** Degrees. Turns the liquid swirl of light around the edge. */
    var swirlAngle = 0f

    /** While the panel is being resized, keep the old glow masks stretched instead of redrawing them. */
    var holdMasks = false
        set(value) {
            field = value
            if (!value) invalidateSelf()
        }

    private val glass = RectF()
    private val shape = Path()
    private val brackets = Path()
    private val ticks = Path()
    private val inner = Path()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grain = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val maskTarget = RectF()
    private val masks = LinkedHashMap<String, Masks>()
    private var swirlShader: SweepGradient? = null
    private var swirlKey = 0L
    private val swirlMatrix = Matrix()

    private class Masks(val outer: Bitmap, val edge: Bitmap)

    private fun dp(value: Float) = value * density

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        rebuildShape()
        updateFill()
    }

    /** The glass edges inside this drawable, for touch checks. */
    fun glassRect(out: RectF) {
        out.set(glass)
    }

    private fun corner(): Float {
        val roundness = look?.roundness ?: 0.4f
        val most = minOf(glass.width(), glass.height()) / 2f
        return (roundness * dp(30f)).coerceAtMost(most).coerceAtLeast(0f)
    }

    private fun rebuildShape() {
        val b = bounds
        if (b.isEmpty) return
        glass.set(b.left + margin, b.top + margin, b.right - margin, b.bottom - margin)
        shape.reset()
        inner.reset()
        brackets.reset()
        ticks.reset()
        if (glass.width() <= 0f || glass.height() <= 0f) return
        val holo = look?.holo == true
        val size = corner()
        if (holo) {
            cutCorners(shape, glass, size, dp(6f))
            val innerBox = RectF(glass)
            innerBox.inset(dp(4.5f), dp(4.5f))
            cutCorners(inner, innerBox, size * 0.75f, dp(4f))
            if (!pane) buildHoloMarks(size)
        } else {
            shape.addRoundRect(glass, size, size, Path.Direction.CW)
        }
    }

    private fun cutCorners(path: Path, box: RectF, cut: Float, round: Float) {
        path.moveTo(box.left + cut, box.top)
        path.lineTo(box.right - round, box.top)
        path.quadTo(box.right, box.top, box.right, box.top + round)
        path.lineTo(box.right, box.bottom - cut)
        path.lineTo(box.right - cut, box.bottom)
        path.lineTo(box.left + round, box.bottom)
        path.quadTo(box.left, box.bottom, box.left, box.bottom - round)
        path.lineTo(box.left, box.top + cut)
        path.close()
    }

    /** Holo Classic's gold corner brackets and the little ruler ticks. */
    private fun buildHoloMarks(cut: Float) {
        val box = glass
        val arm = minOf(dp(22f), box.width() / 5f, box.height() / 3f)
        brackets.moveTo(box.right - arm, box.top)
        brackets.lineTo(box.right - dp(6f), box.top)
        brackets.quadTo(box.right, box.top, box.right, box.top + dp(6f))
        brackets.lineTo(box.right, box.top + arm)
        brackets.moveTo(box.left, box.bottom - arm)
        brackets.lineTo(box.left, box.bottom - dp(6f))
        brackets.quadTo(box.left, box.bottom, box.left + dp(6f), box.bottom)
        brackets.lineTo(box.left + arm, box.bottom)
        val dashStart = box.left + cut + dp(10f)
        brackets.moveTo(dashStart, box.top)
        brackets.lineTo(minOf(dashStart + dp(38f), box.right - arm - dp(8f)), box.top)
        var y = box.top + box.height() * 0.32f
        var index = 0
        while (y <= box.top + box.height() * 0.68f) {
            val length = if (index % 4 == 0) dp(7f) else dp(4f)
            ticks.moveTo(box.left + dp(2.5f), y)
            ticks.lineTo(box.left + dp(2.5f) + length, y)
            y += dp(7f)
            index++
        }
    }

    private fun updateFill() {
        val current = look ?: return
        if (glass.isEmpty) return
        val opacity = current.opacity
        fill.shader = null
        if (pane) {
            if (current.holo) {
                fill.shader = LinearGradient(
                    0f, glass.top, 0f, glass.bottom,
                    Hud.withAlpha(0xFF0B3A44.toInt(), 0.35f + 0.55f * opacity),
                    Hud.withAlpha(Hud.HOLO_GLASS_BOTTOM, 0.45f + 0.5f * opacity),
                    Shader.TileMode.CLAMP,
                )
            } else {
                fill.color = Hud.withAlpha(Hud.blend(current.skin, current.edge, 0.07f), 0.5f + 0.45f * opacity)
            }
        } else if (current.holo) {
            fill.shader = LinearGradient(
                0f, glass.top, 0f, glass.bottom,
                Hud.withAlpha(current.skin, opacity),
                Hud.withAlpha(Hud.blend(current.skin, Hud.INK, 0.75f), opacity),
                Shader.TileMode.CLAMP,
            )
        } else {
            fill.color = Hud.withAlpha(current.skin, opacity)
        }
        if (current.holo && !pane) {
            grain.shader = BitmapShader(grainBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            grain.alpha = (255 * (0.25f + 0.75f * opacity)).toInt()
            sheen.shader = LinearGradient(
                0f, glass.top, 0f, glass.top + glass.height() * 0.35f,
                Hud.withAlpha(Hud.WHITE, 0.04f + 0.06f * opacity),
                0x00FFFFFF,
                Shader.TileMode.CLAMP,
            )
        }
    }

    /** Blurs the glow shapes once for this size. Kept for the last two sizes, like panel and tag. */
    private fun masksFor(): Masks? {
        val b = bounds
        if (b.isEmpty || glass.isEmpty || pane) return null
        val key = "${b.width()}x${b.height()}:${look?.roundness}:${look?.holo}"
        masks[key]?.let { return it }
        if (holdMasks) return masks.values.lastOrNull()
        val outer = Bitmap.createBitmap(b.width(), b.height(), Bitmap.Config.ALPHA_8)
        val edge = Bitmap.createBitmap(b.width(), b.height(), Bitmap.Config.ALPHA_8)
        val local = Path(shape)
        local.offset(-b.left.toFloat(), -b.top.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Hud.WHITE }
        paint.maskFilter = BlurMaskFilter((margin * 0.9f).coerceAtLeast(1f), BlurMaskFilter.Blur.OUTER)
        Canvas(outer).drawPath(local, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(2.5f)
        paint.maskFilter = BlurMaskFilter(dp(5f), BlurMaskFilter.Blur.NORMAL)
        Canvas(edge).drawPath(local, paint)
        val made = Masks(outer, edge)
        masks[key] = made
        while (masks.size > 2) {
            val oldest = masks.keys.first()
            masks.remove(oldest)
        }
        return made
    }

    /** A slow wheel of brighter and dimmer light that turns around the edge. */
    private fun swirl(color: Int): Shader {
        val cx = glass.centerX()
        val cy = glass.centerY()
        val key = color.toLong() xor (cx.toBits().toLong() shl 32) xor cy.toBits().toLong()
        val shader = swirlShader?.takeIf { swirlKey == key } ?: SweepGradient(
            cx,
            cy,
            intArrayOf(
                Hud.withAlpha(color, 1f),
                Hud.withAlpha(color, 0.25f),
                Hud.withAlpha(color, 0.85f),
                Hud.withAlpha(color, 0.15f),
                Hud.withAlpha(color, 0.7f),
                Hud.withAlpha(color, 1f),
            ),
            floatArrayOf(0f, 0.22f, 0.45f, 0.63f, 0.82f, 1f),
        ).also {
            swirlShader = it
            swirlKey = key
        }
        swirlMatrix.setRotate(swirlAngle, cx, cy)
        shader.setLocalMatrix(swirlMatrix)
        return shader
    }

    private fun drawMask(canvas: Canvas, mask: Bitmap, color: Int, alpha: Float, swirl: Boolean) {
        maskPaint.shader = if (swirl) swirl(color) else null
        maskPaint.color = color
        maskPaint.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        maskTarget.set(bounds)
        canvas.drawBitmap(mask, null, maskTarget, maskPaint)
    }

    override fun draw(canvas: Canvas) {
        val current = look ?: return
        if (bounds.isEmpty || glass.isEmpty) return
        val strength = current.glowStrength
        val breathing = if (current.pulse) breath else 0.8f
        val glow = masksFor()
        if (glow != null && current.outerGlowOn && strength > 0f) {
            drawMask(canvas, glow.outer, current.outerGlow, strength * (0.35f + 0.65f * breathing), current.swirl)
        }
        canvas.drawPath(shape, fill)
        if (current.holo && !pane) {
            canvas.drawPath(shape, grain)
            canvas.drawPath(shape, sheen)
        }
        if (glow != null && current.edgeGlow && strength > 0f) {
            drawMask(canvas, glow.edge, current.edge, strength * (0.4f + 0.6f * breathing), current.swirl)
        }
        line.strokeWidth = dp(if (pane) 1f else 1.25f)
        line.color = Hud.withAlpha(current.edge, if (pane) 0.5f else 0.92f)
        canvas.drawPath(shape, line)
        if (current.holo) {
            line.strokeWidth = dp(1f)
            line.color = Hud.withAlpha(current.edge, 0.16f + 0.1f * strength)
            if (!pane) canvas.drawPath(inner, line)
            line.color = Hud.withAlpha(current.edge, 0.4f)
            canvas.drawPath(ticks, line)
            holoStroke(canvas, brackets, current.decor, strength)
        }
    }

    /** Holo Classic's glow: soft wide strokes under one bright thin stroke. */
    private fun holoStroke(canvas: Canvas, path: Path, color: Int, strength: Float) {
        if (strength > 0f) {
            line.strokeWidth = dp(8f)
            line.color = Hud.withAlpha(color, 0.07f * strength)
            canvas.drawPath(path, line)
            line.strokeWidth = dp(5f)
            line.color = Hud.withAlpha(color, 0.2f * strength)
            canvas.drawPath(path, line)
        }
        line.strokeWidth = dp(2.2f)
        line.color = color
        canvas.drawPath(path, line)
    }

    override fun getOutline(outline: Outline) {
        val r = Rect()
        glass.roundOut(r)
        outline.setRoundRect(r, corner())
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        /** Faint white specks that make Holo Classic's glass look frosted. */
        val grainBitmap: Bitmap by lazy {
            val size = 96
            val random = Random(23)
            val pixels = IntArray(size * size) {
                val speck = random.nextInt(0, 22)
                (speck shl 24) or 0x00FFFFFF
            }
            Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
        }
    }
}
