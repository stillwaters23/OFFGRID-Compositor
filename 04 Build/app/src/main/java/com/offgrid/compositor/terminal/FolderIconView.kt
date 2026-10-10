package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

/**
 * A premium glow folder (feature 5), drawn on the phone with no pictures.
 *
 * Two folder sheets stand behind the front one. The front folder has a sharp tab with
 * a cut corner, a dark carbon fiber body with a bright neon edge, and a small inset glass
 * screen. The number on the screen is how many items are inside, in laser bright letters.
 * Royalty's folder picture from chat guided the look. Her final folder art can replace it later.
 *
 * [compact] draws a small one with no words, for the small tag.
 * [special] folders (Panel settings and the theme office) use the special folder color.
 */
internal class FolderIconView(
    context: Context,
    private val count: Int,
    private val tag: String?,
    private val compact: Boolean,
    private val special: Boolean,
) : View(context), Styled {
    private val front = Path()
    private val backNear = Path()
    private val backFar = Path()
    private val screen = RectF()
    private var tagX = 0f
    private var tagBaseline = 0f
    private var tagSize = 0f
    private val body = Paint(Paint.ANTI_ALIAS_FLAG)
    private val weave = Paint(Paint.ANTI_ALIAS_FLAG)
    private val back = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glass = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tube = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.MITER
    }
    private val words = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val regularFace = Hud.mono(context)
    private val boldFace = Hud.monoBold(context)
    private val countText = count.toString()
    private val itemsWord = if (count == 1) "ITEM" else "ITEMS"
    private var accent = Hud.HOLO_CYAN
    private var titanium = true
    private var roundness = 0.04f
    private var glowStrength = 0.7f
    private var glowBoost = 0f

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun applyLook(look: PanelLook) {
        val next = if (special) look.specialFolder else look.folder
        val reshape = look.roundness != roundness
        val repaint = next != accent || look.titanium != titanium || look.glowStrength != glowStrength ||
            look.glowBoost != glowBoost
        accent = next
        titanium = look.titanium
        roundness = look.roundness
        glowStrength = look.glowStrength
        glowBoost = look.glowBoost
        if (reshape) buildShapes(width, height)
        if (reshape || repaint) {
            buildPaints(width, height)
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildShapes(w, h)
        buildPaints(w, h)
    }

    /** One folder outline: a body with a tab on top. The tab's right side is cut at an angle. */
    private fun folder(path: Path, left: Float, top: Float, right: Float, bottom: Float, w: Float) {
        val tabTop = top
        val bodyTop = top + (bottom - top) * 0.2f
        val tabRight = left + (right - left) * 0.42f
        val slope = (bodyTop - tabTop) * 0.9f
        val cut = w * 0.035f
        val round = (w * (0.01f + 0.08f * roundness)).coerceAtMost((bottom - bodyTop) / 3f)
        path.reset()
        path.moveTo(left, bodyTop)
        path.lineTo(left, tabTop + cut)
        path.lineTo(left + cut, tabTop)
        path.lineTo(tabRight, tabTop)
        path.lineTo(tabRight + slope, bodyTop)
        path.lineTo(right - round, bodyTop)
        path.quadTo(right, bodyTop, right, bodyTop + round)
        path.lineTo(right, bottom - round)
        path.quadTo(right, bottom, right - round, bottom)
        path.lineTo(left + round, bottom)
        path.quadTo(left, bottom, left, bottom - round)
        path.close()
    }

    private fun buildShapes(w: Int, h: Int) {
        if (w == 0 || h == 0) return
        val width = w.toFloat()
        val height = h.toFloat()
        val shiftX = width * if (compact) 0.06f else 0.045f
        val shiftY = height * if (compact) 0.07f else 0.055f
        val left = width * 0.05f
        val right = width * 0.95f - shiftX * 2f
        val top = height * 0.12f + shiftY * 2f
        val bottom = height * 0.95f
        folder(backFar, left + shiftX * 2f, top - shiftY * 2f, right + shiftX * 2f, bottom - shiftY * 2f, width)
        folder(backNear, left + shiftX, top - shiftY, right + shiftX, bottom - shiftY, width)
        folder(front, left, top, right, bottom, width)
        val bodyTop = top + (bottom - top) * 0.2f
        val bodyWidth = right - left
        val bodyHeight = bottom - bodyTop
        // The tab runs from the top edge down to the body. Its words sit inside it.
        val tabHeight = bodyTop - top
        tagSize = tabHeight * 0.62f
        tagBaseline = bodyTop - tabHeight * 0.2f
        tagX = left + bodyWidth * 0.2f
        screen.set(
            left + bodyWidth * 0.12f,
            bodyTop + bodyHeight * if (compact) 0.3f else 0.16f,
            right - bodyWidth * 0.12f,
            bottom - bodyHeight * if (compact) 0.3f else 0.14f,
        )
    }

    private fun buildPaints(w: Int, h: Int) {
        if (w == 0 || h == 0) return
        val height = h.toFloat()
        if (titanium) {
            body.shader = LinearGradient(
                0f, height * 0.2f, 0f, height,
                intArrayOf(0xF2262B30.toInt(), 0xF2111417.toInt(), 0xF21A1E22.toInt()),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP,
            )
            weave.shader = BitmapShader(carbonTileFor(context), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            weave.alpha = 150
            back.shader = null
            back.color = 0xE6101316.toInt()
        } else {
            body.shader = LinearGradient(
                0f, height * 0.12f, 0f, height * 0.95f,
                Hud.withAlpha(accent, 0.34f),
                Hud.withAlpha(accent, 0.07f),
                Shader.TileMode.CLAMP,
            )
            weave.shader = null
            back.shader = null
            back.color = Hud.withAlpha(accent, 0.08f)
        }
        glass.shader = LinearGradient(
            0f, screen.top, 0f, screen.bottom,
            Hud.withAlpha(Hud.blend(accent, Hud.INK, 0.72f), 0.95f),
            Hud.withAlpha(Hud.blend(accent, Hud.INK, 0.9f), 0.95f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        val unit = minOf(width, height).toFloat()
        val glow = 0.4f + 0.6f * glowStrength + 0.5f * glowBoost

        // The two folder sheets behind, dimmer the farther back they are.
        canvas.drawPath(backFar, back)
        edge(canvas, backFar, unit, 0.35f * glow, thin = true)
        canvas.drawPath(backNear, back)
        edge(canvas, backNear, unit, 0.55f * glow, thin = true)

        // The front folder: dark body, carbon weave, then the neon edge.
        canvas.drawPath(front, body)
        if (titanium) canvas.drawPath(front, weave)
        drawScreen(canvas, unit)
        edge(canvas, front, unit, glow, thin = compact)
        if (compact) return

        if (tag != null) {
            words.clearShadowLayer()
            words.typeface = boldFace
            words.textSize = tagSize
            words.color = Hud.lighten(accent, 0.2f)
            canvas.drawText(tag, tagX, tagBaseline, words)
        }
        val centerX = screen.centerX()
        words.typeface = boldFace
        words.textSize = screen.height() * 0.5f
        words.color = Hud.lighten(accent, 0.7f)
        words.setShadowLayer(height * 0.06f, 0f, 0f, Hud.withAlpha(accent, glow))
        canvas.drawText(countText, centerX, screen.top + screen.height() * 0.58f, words)
        words.clearShadowLayer()
        words.typeface = regularFace
        words.textSize = screen.height() * 0.2f
        words.color = Hud.withAlpha(accent, 0.9f)
        canvas.drawText(itemsWord, centerX, screen.top + screen.height() * 0.86f, words)
    }

    /** The small inset glass screen: deep glass, a dark top edge for depth, and a thin rim of light. */
    private fun drawScreen(canvas: Canvas, unit: Float) {
        canvas.drawRect(screen, glass)
        tube.style = Paint.Style.STROKE
        tube.strokeWidth = unit * 0.02f
        tube.color = Hud.withAlpha(Hud.INK, 0.7f)
        canvas.drawLine(screen.left, screen.top + tube.strokeWidth / 2f, screen.right, screen.top + tube.strokeWidth / 2f, tube)
        tube.strokeWidth = unit * 0.012f
        tube.color = Hud.withAlpha(accent, 0.65f)
        canvas.drawRect(screen, tube)
    }

    /** A neon edge: wide soft light, a bright colored line, and a white-hot center line. */
    private fun edge(canvas: Canvas, path: Path, unit: Float, strength: Float, thin: Boolean) {
        val scale = if (thin) 0.6f else 1f
        tube.style = Paint.Style.STROKE
        tube.strokeWidth = unit * 0.11f * scale
        tube.color = Hud.withAlpha(accent, 0.1f * strength)
        canvas.drawPath(path, tube)
        tube.strokeWidth = unit * 0.055f * scale
        tube.color = Hud.withAlpha(accent, 0.28f * strength)
        canvas.drawPath(path, tube)
        tube.strokeWidth = unit * 0.026f * scale
        tube.color = Hud.withAlpha(accent, 0.95f * strength)
        canvas.drawPath(path, tube)
        if (!thin) {
            tube.strokeWidth = unit * 0.01f
            tube.color = Hud.withAlpha(Hud.WHITE, 0.7f * strength)
            canvas.drawPath(path, tube)
        }
    }

    private companion object {
        fun carbonTileFor(context: Context) = PanelTextures.carbon(context.resources.displayMetrics.density)
    }
}
