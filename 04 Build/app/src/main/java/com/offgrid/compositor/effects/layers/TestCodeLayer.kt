package com.offgrid.compositor.effects.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.EffectTimes
import com.offgrid.compositor.effects.Fire
import com.offgrid.compositor.effects.Fired
import com.offgrid.compositor.effects.OverlayLayer
import kotlin.math.min

/**
 * The test code (feature 37, a private test tool). For a short while after it is fired, the
 * code shows in the top right corner: big bold white digits on black, with a teal edge and
 * the words TEST CODE above. It is made to stay readable after the stream squeezes the video.
 */
class TestCodeLayer : OverlayLayer {
    private val digits = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT
        letterSpacing = DIGIT_SPACING
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
        letterSpacing = LABEL_SPACING
    }
    private val back = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val metrics = Paint.FontMetrics()
    private val box = RectF()

    override fun isActive(frame: EffectFrame): Boolean {
        val fired = lastCode(frame) ?: return false
        if (fired.arg.isNullOrBlank()) return false
        val since = frame.time - fired.at
        return since >= 0f && since < EffectTimes.TEST_CODE_SECONDS
    }

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val fired = lastCode(frame) ?: return
        val code = fired.arg ?: return
        if (code.isBlank()) return
        val t = frame.time - fired.at
        if (t < 0f || t >= EffectTimes.TEST_CODE_SECONDS) return
        val w = frame.width.toFloat()
        if (w <= 0f || frame.height <= 0) return
        // A short soft fade in and out. It is never a flash.
        val fade = min(EffectMath.smooth(t / FADE_SECONDS), EffectMath.smooth((EffectTimes.TEST_CODE_SECONDS - t) / FADE_SECONDS))

        val unit = min(w, frame.height.toFloat())
        digits.textSize = unit * DIGIT_SIZE
        val widest = unit * MAX_DIGITS_WIDTH
        val measured = digits.measureText(code)
        if (measured > widest) digits.textSize *= widest / measured
        label.textSize = unit * LABEL_SIZE
        val pad = unit * PAD
        val margin = unit * MARGIN
        val digitsWidth = digits.measureText(code)
        val labelWidth = label.measureText(LABEL)
        val inner = maxOf(digitsWidth, labelWidth)

        label.getFontMetrics(metrics)
        val labelHeight = metrics.descent - metrics.ascent
        val labelAscent = metrics.ascent
        digits.getFontMetrics(metrics)
        val digitCap = -metrics.ascent * CAP_SHARE

        val right = w - margin
        val top = margin + unit * TOP_EXTRA
        box.set(right - inner - 2f * pad, top, right, top + pad + labelHeight + pad * GAP + digitCap + pad)
        val corner = unit * CORNER
        back.color = EffectMath.withAlpha(Color.BLACK, BACK_ALPHA * fade)
        canvas.drawRoundRect(box, corner, corner, back)
        edge.strokeWidth = unit * EDGE
        edge.color = EffectMath.withAlpha(TEAL, fade)
        canvas.drawRoundRect(box, corner, corner, edge)

        val textRight = right - pad
        label.color = EffectMath.withAlpha(LABEL_COLOR, fade)
        canvas.drawText(LABEL, textRight, top + pad - labelAscent, label)
        digits.alpha = (255 * fade).toInt()
        canvas.drawText(code, textRight, box.bottom - pad, digits)
    }

    private companion object {
        const val LABEL = "TEST CODE"
        const val TEAL = 0xFF2BF5E6.toInt()
        const val LABEL_COLOR = 0xFFE6FFFC.toInt()
        const val DIGIT_SIZE = 0.1f
        const val DIGIT_SPACING = 0.06f
        const val MAX_DIGITS_WIDTH = 0.6f
        const val LABEL_SIZE = 0.034f
        const val LABEL_SPACING = 0.18f
        const val PAD = 0.03f
        const val GAP = 0.6f
        const val MARGIN = 0.035f
        const val TOP_EXTRA = 0.02f
        const val CORNER = 0.018f
        const val EDGE = 0.006f
        const val BACK_ALPHA = 0.94f
        const val CAP_SHARE = 0.78f
        const val FADE_SECONDS = 0.3f

        /** The newest test code firing, found without making a list iterator. */
        fun lastCode(frame: EffectFrame): Fired? {
            val fired = frame.fired
            for (i in fired.size - 1 downTo 0) {
                if (fired[i].kind == Fire.TEST_CODE) return fired[i]
            }
            return null
        }
    }
}
