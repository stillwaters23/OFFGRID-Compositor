package com.offgrid.compositor.terminal

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
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

internal data class DisplayCorner(val radius: Float, val centerX: Float, val centerY: Float)

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

    var fullBleed = false
        set(value) {
            field = value
            onBoundsChange(bounds)
            invalidateSelf()
        }
        var resizeGlow = 0f
            set(value) {
                field = value.coerceIn(0f, 1f)
                invalidateSelf()
            }

            var solidSkin = false
                set(value) {
                    field = value
                    updateShaders()
                    invalidateSelf()
                }

    var lightSkin = false
        set(value) {
            field = value
            updateShaders()
            invalidateSelf()
        }

        var uvTheme = false
            set(value) {
                field = value
                invalidateSelf()
            }

    var seeThroughAtFullSize = false
        set(value) {
            field = value
            updateShaders()
            invalidateSelf()
        }

    var borderThickness = 3f
        set(value) {
            field = value.coerceIn(2f, 6f)
            invalidateSelf()
        }

    private var displayCorners = List(4) { DisplayCorner(0f, 0f, 0f) }

    fun setDisplayCornerGeometry(corners: List<DisplayCorner>) {
        displayCorners = corners
        onBoundsChange(bounds)
        invalidateSelf()
    }

    var opacity = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateShaders()
            invalidateSelf()
        }

    var glow = 0.7f
        set(value) {
            field = value.coerceIn(0f, 1f)
            bloomLayers = PanelGeometry.glowLayers((field * 100f).toInt())
            invalidateSelf()
        }

    private var bloomLayers = PanelGeometry.glowLayers(70)
    private val shape = Path()
    private val border = Path()
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
        if (fullBleed) {
            shape.reset()
            roundedPath(shape, box, displayCorners, 0f)
            val inset = dp(1f) + dp(borderThickness) / 2f
            val edge = RectF(box).apply { inset(inset, inset) }
            border.reset()
            roundedPath(border, edge, displayCorners, inset)
            buildBrackets(box, 0f)
            updateShaders()
            return
        }
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

    private fun roundedPath(path: Path, box: RectF, corners: List<DisplayCorner>, inset: Float) {
        val maxRadius = minOf(box.width(), box.height()) / 2f
        val radii = FloatArray(4) { index -> (corners[index].radius - inset).coerceIn(0f, maxRadius) }
        val centersX = FloatArray(4) { index -> corners[index].centerX }
        val centersY = FloatArray(4) { index -> corners[index].centerY }
        val tl = radii[0]
        val tr = radii[1]
        val br = radii[2]
        val bl = radii[3]
        path.moveTo(if (tl > 0f) centersX[0] else box.left, box.top)
        path.lineTo(if (tr > 0f) centersX[1] else box.right, box.top)
        if (tr > 0f) {
            path.arcTo(RectF(centersX[1] - tr, centersY[1] - tr, centersX[1] + tr, centersY[1] + tr), 270f, 90f, false)
        }
        path.lineTo(box.right, if (br > 0f) centersY[2] else box.bottom)
        if (br > 0f) {
            path.arcTo(RectF(centersX[2] - br, centersY[2] - br, centersX[2] + br, centersY[2] + br), 0f, 90f, false)
        }
        path.lineTo(if (bl > 0f) centersX[3] else box.left, box.bottom)
        if (bl > 0f) {
            path.arcTo(RectF(centersX[3] - bl, centersY[3] - bl, centersX[3] + bl, centersY[3] + bl), 90f, 90f, false)
        }
        path.lineTo(box.left, if (tl > 0f) centersY[0] else box.top)
        if (tl > 0f) {
            path.arcTo(RectF(centersX[0] - tl, centersY[0] - tl, centersX[0] + tl, centersY[0] + tl), 180f, 90f, false)
        }
        path.close()
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
        fill.shader = if (fullBleed || solidSkin) {
            null
        } else if (holo) {
            LinearGradient(
                0f, top, 0f, bottom,
                Hud.withAlpha(0xFF0B3A44.toInt(), 0.35f + 0.55f * opacity),
                Hud.withAlpha(Hud.GLASS_BOTTOM, 0.45f + 0.5f * opacity),
                Shader.TileMode.CLAMP,
            )
        } else if (lightSkin) {
            LinearGradient(
                0f, top, 0f, bottom,
                Hud.withAlpha(0xFFFFFFFF.toInt(), opacity),
                Hud.withAlpha(0xFFECECF2.toInt(), opacity),
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
        if (fullBleed) {
            val skin = if (lightSkin) 0xFFFFFFFF.toInt() else Hud.INK
            fill.color = if (seeThroughAtFullSize) Hud.withAlpha(skin, opacity) else skin
            fill.shader = null
            canvas.drawPath(shape, fill)
            drawFullBleedBorder(canvas)
            return
        }
        if (solidSkin) {
            fill.color = if (lightSkin) 0xFFFFFFFF.toInt() else Hud.INK
            fill.shader = null
            canvas.drawPath(shape, fill)
            glowStroke(canvas, shape, NeonPalette.ELECTRIC_PURPLE, dp(borderThickness), 1f)
            return
        }
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
        line.blendMode = BlendMode.PLUS
        line.strokeWidth = dp(borderThickness + 4f)
        line.color = Hud.withAlpha(NeonPalette.ELECTRIC_PURPLE, 0.18f + 0.35f * resizeGlow)
        canvas.drawPath(shape, line)
        line.strokeWidth = dp(borderThickness)
        line.color = if (lightSkin) Hud.INK else NeonPalette.BLACKLIGHT_WHITE
        canvas.drawPath(shape, line)
        line.blendMode = BlendMode.SRC_OVER
    }

    private fun drawFullBleedBorder(canvas: Canvas) {
        val layers = bloomLayers
        val edgeColor = if (uvTheme) NeonPalette.HOT_MAGENTA else NeonPalette.ELECTRIC_PURPLE
        val originalAlpha = canvas.save()
        canvas.clipRect(bounds)
        if (layers.wide > 0f) {
            line.blendMode = BlendMode.PLUS
            line.strokeWidth = dp(32f + 32f * glow)
            val bloomColor = if (uvTheme) NeonPalette.UV_VIOLET else NeonPalette.CYAN_GLOW
            line.color = Hud.withAlpha(bloomColor, (0.08f + 0.2f * resizeGlow) * layers.wide)
            canvas.drawPath(border, line)
        }
        if (layers.mid > 0f) {
            line.strokeWidth = dp(12f + 8f * glow)
            line.color = Hud.withAlpha(edgeColor, (0.24f + 0.25f * resizeGlow) * layers.mid)
            canvas.drawPath(border, line)
        }
        if (layers.core > 0f) {
            line.strokeWidth = dp(borderThickness + 4f)
            line.color = Hud.withAlpha(edgeColor, 0.5f * layers.core)
            canvas.drawPath(border, line)
        }
        line.blendMode = BlendMode.SRC_OVER
        line.strokeWidth = dp(borderThickness)
        line.color = if (lightSkin) Hud.INK else NeonPalette.BLACKLIGHT_WHITE
        canvas.drawPath(border, line)
        canvas.restoreToCount(originalAlpha)
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
