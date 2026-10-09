package com.offgrid.compositor.effects.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.FaceSpot
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.InkStroke
import com.offgrid.compositor.effects.OverlayLayer

/**
 * Finger ink (feature 1): every line drawn with a finger, as a smooth glowing neon line.
 * Soft wide glow strokes go under a bright core with a white-hot middle.
 *
 * Ink pinned to the face moves and grows with the face. If the face is gone for now,
 * the ink stays where it was drawn. It shows any time there is ink, even with Ink off.
 */
class InkLayer : OverlayLayer {
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val batch = Path()
    private var boxW = 0f
    private var boxH = 0f
    private var scale = 1f
    private var shiftX = 0f
    private var shiftY = 0f

    override fun isActive(frame: EffectFrame): Boolean {
        val ink = frame.ink
        for (i in ink.indices) {
            if (pairsOf(ink[i]) > 0) return true
        }
        return false
    }

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val ink = frame.ink
        if (ink.isEmpty() || frame.width <= 0 || frame.height <= 0) return
        val flip = frame.settings.on(FxKeys.FACE_FLIP_X)
        // Lines that look the same are drawn together, so many lines cost a few draws, not hundreds.
        batch.rewind()
        var look: InkStroke? = null
        for (i in ink.indices) {
            val stroke = ink[i]
            if (pairsOf(stroke) == 0) continue
            val current = look
            if (current != null && !sameLook(current, stroke)) {
                drawBatch(canvas, current, frame.width)
                batch.rewind()
            }
            addStroke(stroke, frame, flip)
            look = stroke
        }
        look?.let { drawBatch(canvas, it, frame.width) }
    }

    /** Adds [stroke] to the batch as a smooth line through the middles of its points. */
    private fun addStroke(stroke: InkStroke, frame: EffectFrame, flip: Boolean) {
        val pairs = pairsOf(stroke)
        val anchor = stroke.faceAnchor
        val face = frame.face
        boxW = frame.width.toFloat()
        boxH = frame.height.toFloat()
        if (anchor != null && face != null && anchor.size > 0f) {
            scale = face.size / anchor.size
            shiftX = faceX(face, flip) - faceX(anchor, flip) * scale
            shiftY = face.y - anchor.y * scale
        } else {
            scale = 1f
            shiftX = 0f
            shiftY = 0f
        }
        val p = stroke.points
        batch.moveTo(px(p, 0), py(p, 0))
        when (pairs) {
            // A single touch is a round dot.
            1 -> batch.lineTo(px(p, 0) + DOT_NUDGE_PX, py(p, 0))
            2 -> batch.lineTo(px(p, 1), py(p, 1))
            else -> {
                for (i in 1 until pairs - 1) {
                    val x = px(p, i)
                    val y = py(p, i)
                    batch.quadTo(x, y, (x + px(p, i + 1)) / 2f, (y + py(p, i + 1)) / 2f)
                }
                batch.lineTo(px(p, pairs - 1), py(p, pairs - 1))
            }
        }
    }

    /** Point [i] across, in pixels: face + (point - anchor) * (face size / anchor size). */
    private fun px(p: FloatArray, i: Int): Float = boxW * (shiftX + p[2 * i] * scale)

    private fun py(p: FloatArray, i: Int): Float = boxH * (shiftY + p[2 * i + 1] * scale)

    private fun drawBatch(canvas: Canvas, look: InkStroke, width: Int) {
        val core = width * (MIN_SIZE + (MAX_SIZE - MIN_SIZE) * EffectMath.clamp01(look.size))
        val glow = EffectMath.clamp01(look.glow)
        val color = look.color or OPAQUE
        if (glow > 0f) {
            for (pass in GLOW_PASSES) {
                pen.strokeWidth = core * (1f + (pass[0] - 1f) * glow)
                pen.color = EffectMath.withAlpha(color, pass[1] * glow)
                canvas.drawPath(batch, pen)
            }
        }
        pen.strokeWidth = core
        pen.color = mix(color, Color.WHITE, CORE_WHITE * glow)
        canvas.drawPath(batch, pen)
        pen.strokeWidth = core * HOT_SHARE
        pen.color = EffectMath.withAlpha(mix(color, Color.WHITE, HOT_WHITE), HOT_ALPHA * (HOT_MIN + (1f - HOT_MIN) * glow))
        canvas.drawPath(batch, pen)
    }

    private fun mix(from: Int, to: Int, t: Float): Int {
        val k = EffectMath.clamp01(t)
        val r = Color.red(from) + ((Color.red(to) - Color.red(from)) * k).toInt()
        val g = Color.green(from) + ((Color.green(to) - Color.green(from)) * k).toInt()
        val b = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * k).toInt()
        return Color.rgb(r, g, b)
    }

    private companion object {
        /** Line width: 0.4% to 3% of the video width. */
        const val MIN_SIZE = 0.004f
        const val MAX_SIZE = 0.03f
        const val OPAQUE = 0xFF000000.toInt()
        const val CORE_WHITE = 0.18f
        const val HOT_SHARE = 0.42f
        const val HOT_WHITE = 0.8f
        const val HOT_ALPHA = 0.9f
        const val HOT_MIN = 0.35f
        const val DOT_NUDGE_PX = 0.01f

        /** Each glow pass: width as a multiple of the core, then strength. Wide, then tight. */
        val GLOW_PASSES = arrayOf(
            floatArrayOf(6.5f, 0.06f),
            floatArrayOf(3.8f, 0.12f),
            floatArrayOf(2.1f, 0.28f),
        )

        fun pairsOf(stroke: InkStroke): Int = minOf(stroke.count, stroke.points.size) / 2

        fun sameLook(a: InkStroke, b: InkStroke): Boolean =
            a.color == b.color && a.size == b.size && a.glow == b.glow

        /** The face's place across, turned around when the face finder sees the picture mirrored. */
        fun faceX(face: FaceSpot, flip: Boolean): Float = if (flip) 1f - face.x else face.x
    }
}
