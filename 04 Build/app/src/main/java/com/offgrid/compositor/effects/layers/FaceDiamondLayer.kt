package com.offgrid.compositor.effects.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import kotlin.math.floor

/**
 * The face diamond (features 9 to 11): a tall diamond that hides the face.
 * Classic is the Step 1 look, a hot pink diamond with a yellow one inside.
 * Laser is black with a bright white glowing edge. Black is solid black.
 *
 * With Follow on and a face in view, it sits on the face and grows with it.
 * Otherwise it stays where the X, Y, and Size settings put it.
 * The inside is always solid, so no part of the face shows through.
 */
class FaceDiamondLayer : OverlayLayer {
    private val solid = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val glint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            0f,
            0f,
            1f,
            intArrayOf(Color.WHITE, EffectMath.withAlpha(Color.WHITE, 0.35f), 0),
            floatArrayOf(0f, 0.3f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val shape = Path()

    override fun isActive(frame: EffectFrame): Boolean = frame.settings.on(FxKeys.DIAMOND_ON)

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val s = frame.settings
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val face = frame.face
        val cx: Float
        val cy: Float
        val width: Float
        if (s.on(FxKeys.DIAMOND_FOLLOW) && face != null) {
            cx = w * if (s.on(FxKeys.FACE_FLIP_X)) 1f - face.x else face.x
            cy = h * face.y
            width = w * face.size * FACE_GROW
        } else {
            cx = w * s.value(FxKeys.DIAMOND_X)
            cy = h * s.value(FxKeys.DIAMOND_Y)
            width = w * (MIN_WIDTH + (MAX_WIDTH - MIN_WIDTH) * s.value(FxKeys.DIAMOND_SIZE))
        }
        if (width <= 0f) return
        val height = width * TALL
        when {
            s.chose(FxKeys.DIAMOND_STYLE, FxKeys.DIAMOND_LASER) -> drawLaser(canvas, cx, cy, width, height, frame)
            s.chose(FxKeys.DIAMOND_STYLE, FxKeys.DIAMOND_BLACK) -> {
                diamond(cx, cy, width, height)
                solid.color = Color.BLACK
                canvas.drawPath(shape, solid)
            }
            else -> {
                diamond(cx, cy, width, height)
                solid.color = CLASSIC_PINK
                canvas.drawPath(shape, solid)
                diamond(cx, cy, width * INNER, height * INNER)
                solid.color = CLASSIC_YELLOW
                canvas.drawPath(shape, solid)
            }
        }
    }

    /** Black inside, a white-hot edge, soft glow outside, and one slow glint running round the edge. */
    private fun drawLaser(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float, frame: EffectFrame) {
        diamond(cx, cy, width, height)
        val core = width * LASER_CORE
        for (pass in GLOW_PASSES) {
            edge.strokeWidth = core * pass[0]
            edge.color = EffectMath.withAlpha(Color.WHITE, pass[1])
            canvas.drawPath(shape, edge)
        }
        solid.color = Color.BLACK
        canvas.drawPath(shape, solid)
        edge.strokeWidth = core
        edge.color = Color.WHITE
        canvas.drawPath(shape, edge)

        // The glint goes round once every few seconds. It glides, it never blinks.
        val lap = if (frame.reducedMotion) GLINT_SECONDS * 2f else GLINT_SECONDS
        val u = (frame.time / lap - floor(frame.time / lap)) * 4f
        val side = u.toInt().coerceIn(0, 3)
        val f = u - side
        val x0 = cornerX(side, cx, width)
        val y0 = cornerY(side, cy, height)
        val x1 = cornerX(side + 1, cx, width)
        val y1 = cornerY(side + 1, cy, height)
        val radius = core * GLINT_SIZE
        canvas.save()
        canvas.translate(EffectMath.lerp(x0, x1, f), EffectMath.lerp(y0, y1, f))
        canvas.scale(radius, radius)
        canvas.drawCircle(0f, 0f, 1f, glint)
        canvas.restore()
    }

    private fun diamond(cx: Float, cy: Float, width: Float, height: Float) {
        shape.rewind()
        shape.moveTo(cx, cy - height / 2f)
        shape.lineTo(cx + width / 2f, cy)
        shape.lineTo(cx, cy + height / 2f)
        shape.lineTo(cx - width / 2f, cy)
        shape.close()
    }

    private companion object {
        /** The Step 1 test diamond colors. */
        const val CLASSIC_PINK = 0xFFFF148C.toInt()
        const val CLASSIC_YELLOW = 0xFFFFEB14.toInt()
        const val INNER = 0.8f

        /** Height is this many times the width. */
        const val TALL = 1.45f

        /** With Follow on, the diamond is this many face widths wide. */
        const val FACE_GROW = 1.9f

        /** Without a face: 20% to 80% of the video width. */
        const val MIN_WIDTH = 0.2f
        const val MAX_WIDTH = 0.8f

        const val LASER_CORE = 0.012f
        const val GLINT_SECONDS = 4f
        const val GLINT_SIZE = 9f

        /** Each glow pass: width as a multiple of the edge, then strength. Wide, then tight. */
        val GLOW_PASSES = arrayOf(
            floatArrayOf(9f, 0.08f),
            floatArrayOf(5f, 0.16f),
            floatArrayOf(2.6f, 0.38f),
        )

        /** Corner [i] going round from the top: top, right, bottom, left. */
        fun cornerX(i: Int, cx: Float, width: Float): Float = when (i % 4) {
            1 -> cx + width / 2f
            3 -> cx - width / 2f
            else -> cx
        }

        fun cornerY(i: Int, cy: Float, height: Float): Float = when (i % 4) {
            0 -> cy - height / 2f
            2 -> cy + height / 2f
            else -> cy
        }
    }
}
