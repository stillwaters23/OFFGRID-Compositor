package com.offgrid.compositor.terminal

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.random.Random

/**
 * Dark frosted glass with cyan and gold glow lines.
 * Two corners are cut, two are softly rounded. Our own design.
 *
 * [opacity] only changes the glass. The glow lines stay, so the panel
 * can always be found, even at 0.
 */
internal class GlassPanelDrawable(
    private val density: Float,
    private val cutDp: Float = 16f,
    private val holo: Boolean = false,
) : Drawable() {

    var opacity = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateShaders()
            invalidateSelf()
        }

    var glow = 0.7f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidateSelf()
        }

    private val shape = Path()
    private val inner = Path()
    private val brackets = Path()
    private val ticks = Path()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grain = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private fun dp(value: Float) = value * density

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val box = RectF(bounds)
        box.inset(dp(1.5f), dp(1.5f))
        val cut = dp(cutDp).coerceAtMost(minOf(box.width(), box.height()) / 3f)
        shape.reset()
        cutCorners(shape, box, cut, dp(6f))
        val innerBox = RectF(box)
        innerBox.inset(dp(4.5f), dp(4.5f))
        inner.reset()
        cutCorners(inner, innerBox, cut * 0.75f, dp(4f))
        buildBrackets(box, cut)
        updateShaders()
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

    private fun buildBrackets(box: RectF, cut: Float) {
        val arm = minOf(dp(22f), box.width() / 5f, box.height() / 3f)
        brackets.reset()
        // Gold corner brackets on the two rounded corners.
        brackets.moveTo(box.right - arm, box.top)
        brackets.lineTo(box.right - dp(6f), box.top)
        brackets.quadTo(box.right, box.top, box.right, box.top + dp(6f))
        brackets.lineTo(box.right, box.top + arm)
        brackets.moveTo(box.left, box.bottom - arm)
        brackets.lineTo(box.left, box.bottom - dp(6f))
        brackets.quadTo(box.left, box.bottom, box.left + dp(6f), box.bottom)
        brackets.lineTo(box.left + arm, box.bottom)
        // A gold dash on the top edge, just after the cut corner.
        val dashStart = box.left + cut + dp(10f)
        brackets.moveTo(dashStart, box.top)
        brackets.lineTo(minOf(dashStart + dp(38f), box.right - arm - dp(8f)), box.top)
        // Small cyan ticks down the left edge, like a ruler.
        ticks.reset()
        val tickTop = box.top + box.height() * 0.32f
        val tickBottom = box.top + box.height() * 0.68f
        var y = tickTop
        var index = 0
        while (y <= tickBottom) {
            val length = if (index % 4 == 0) dp(7f) else dp(4f)
            ticks.moveTo(box.left + dp(2.5f), y)
            ticks.lineTo(box.left + dp(2.5f) + length, y)
            y += dp(7f)
            index++
        }
    }

    private fun updateShaders() {
        val b = bounds
        if (b.isEmpty) return
        val top = b.top.toFloat()
        val bottom = b.bottom.toFloat()
        fill.shader = if (holo) {
            LinearGradient(
                0f, top, 0f, bottom,
                Hud.withAlpha(0xFF0B3A44.toInt(), 0.35f + 0.55f * opacity),
                Hud.withAlpha(Hud.GLASS_BOTTOM, 0.45f + 0.5f * opacity),
                Shader.TileMode.CLAMP,
            )
        } else {
            LinearGradient(
                0f, top, 0f, bottom,
                Hud.withAlpha(Hud.GLASS_TOP, opacity),
                Hud.withAlpha(Hud.GLASS_BOTTOM, opacity),
                Shader.TileMode.CLAMP,
            )
        }
        grain.shader = BitmapShader(grainBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        grain.alpha = (255 * (0.25f + 0.75f * opacity)).toInt()
        sheen.shader = LinearGradient(
            0f, top, 0f, top + (bottom - top) * 0.35f,
            Hud.withAlpha(0xFFFFFFFF.toInt(), 0.04f + 0.06f * opacity),
            0x00FFFFFF,
            Shader.TileMode.CLAMP,
        )
    }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        canvas.drawPath(shape, fill)
        canvas.drawPath(shape, grain)
        canvas.drawPath(shape, sheen)
        glowStroke(canvas, shape, Hud.CYAN, dp(1.3f), 0.92f)
        line.strokeWidth = dp(1f)
        line.color = Hud.withAlpha(Hud.CYAN, 0.16f + 0.1f * glow)
        canvas.drawPath(inner, line)
        line.strokeWidth = dp(1f)
        line.color = Hud.withAlpha(Hud.CYAN, 0.4f)
        canvas.drawPath(ticks, line)
        glowStroke(canvas, brackets, Hud.GOLD, dp(2.2f), 1f)
    }

    /** A soft glow: wide faint strokes under one bright thin stroke. */
    private fun glowStroke(canvas: Canvas, path: Path, color: Int, width: Float, coreAlpha: Float) {
        if (glow > 0f) {
            line.strokeWidth = width + dp(6f)
            line.color = Hud.withAlpha(color, 0.07f * glow)
            canvas.drawPath(path, line)
            line.strokeWidth = width + dp(2.8f)
            line.color = Hud.withAlpha(color, 0.2f * glow)
            canvas.drawPath(path, line)
        }
        line.strokeWidth = width
        line.color = Hud.withAlpha(color, coreAlpha)
        canvas.drawPath(path, line)
    }

    override fun getOutline(outline: Outline) {
        outline.setRoundRect(bounds, dp(6f))
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        /** Faint white specks that make the glass look frosted. */
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
