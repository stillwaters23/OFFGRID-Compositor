package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

/**
 * A big glow-stick folder (feature 5). The number on its face is how many items are inside.
 * [compact] draws a small one with no words, for the side list.
 */
internal class FolderIconView(
    context: Context,
    private val accent: Int,
    private val count: Int,
    private val tag: String?,
    private val compact: Boolean,
    private val showCount: Boolean = true,
) : View(context) {
    private val outline = Path()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tube = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val words = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val regularFace = Hud.mono(context)
    private val boldFace = Hud.monoBold(context)
    private val countText = count.toString()
    private val itemsWord = if (count == 1) "ITEM" else "ITEMS"
    private var lipY = 0f

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        val width = w.toFloat()
        val height = h.toFloat()
        val left = width * 0.07f
        val right = width * 0.93f
        val body = Path().apply {
            addRoundRect(RectF(left, height * 0.26f, right, height * 0.9f), width * 0.07f, width * 0.07f, Path.Direction.CW)
        }
        val tab = Path().apply {
            moveTo(left, height * 0.3f)
            lineTo(left, height * 0.19f)
            quadTo(left, height * 0.12f, left + width * 0.06f, height * 0.12f)
            lineTo(width * 0.41f, height * 0.12f)
            lineTo(width * 0.5f, height * 0.27f)
            lineTo(left, height * 0.27f)
            close()
        }
        outline.reset()
        outline.op(body, tab, Path.Op.UNION)
        lipY = height * 0.4f
        fill.shader = LinearGradient(
            0f, height * 0.12f, 0f, height * 0.9f,
            Hud.withAlpha(accent, 0.34f),
            Hud.withAlpha(accent, 0.07f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        val unit = minOf(width, height).toFloat()
        canvas.drawPath(outline, fill)
        // A glow stick: wide soft light, a bright colored tube, then a white-hot core.
        tube.strokeWidth = unit * 0.13f
        tube.color = Hud.withAlpha(accent, 0.12f)
        canvas.drawPath(outline, tube)
        tube.strokeWidth = unit * 0.07f
        tube.color = Hud.withAlpha(accent, 0.3f)
        canvas.drawPath(outline, tube)
        tube.strokeWidth = unit * 0.035f
        tube.color = Hud.withAlpha(accent, 0.95f)
        canvas.drawPath(outline, tube)
        tube.strokeWidth = unit * 0.014f
        tube.color = Hud.withAlpha(0xFFFFFFFF.toInt(), 0.7f)
        canvas.drawPath(outline, tube)
        // The front flap.
        tube.strokeWidth = unit * 0.02f
        tube.color = Hud.withAlpha(accent, 0.45f)
        canvas.drawLine(width * 0.07f, lipY, width * 0.93f, lipY, tube)
        if (compact) return

        if (tag != null) {
            words.typeface = boldFace
            words.textSize = height * 0.11f
            words.color = Hud.withAlpha(accent, 0.95f)
            canvas.drawText(tag, width * 0.26f, height * 0.235f, words)
        }
        if (showCount) {
            words.typeface = boldFace
            words.textSize = height * 0.28f
            words.color = Hud.withAlpha(0xFFFFFFFF.toInt(), 0.95f)
            words.setShadowLayer(height * 0.08f, 0f, 0f, accent)
            canvas.drawText(countText, width / 2f, height * 0.69f, words)
            words.clearShadowLayer()
            words.typeface = regularFace
            words.textSize = height * 0.1f
            words.color = Hud.withAlpha(accent, 0.9f)
            canvas.drawText(itemsWord, width / 2f, height * 0.83f, words)
        }
    }
}
