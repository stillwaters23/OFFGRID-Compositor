package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.TextPaint
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView

/**
 * Words that always fit (Step 2.4). Royalty saw words cut off at Font size 100.
 *
 * Text here wraps only between words, never inside one, and never ends in "…".
 * If one word is wider than the room it has, the text gets a little smaller, just enough
 * for that word to fit, down to [MIN_SHARE] of its size. With [fitWholeLine] on, the
 * whole text is kept on one line the same way, for titles.
 *
 * The size the panel asks for, from Font size, is always the starting point, so the
 * text grows back when there is room again.
 */
internal open class FitTextView(context: Context) : AppCompatTextView(context) {
    /** The size the panel asked for, in pixels. Fitting only ever makes the text smaller than this. */
    private var askedSize = textSize
    private var fitting = false
    private val probe = TextPaint()

    /** Keep the whole text on one line, made smaller if needed, instead of wrapping. */
    var fitWholeLine = false
        set(value) {
            field = value
            maxLines = if (value) 1 else Int.MAX_VALUE
            requestLayout()
        }

    init {
        breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
        hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        if (!fitting) askedSize = textSize
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        val room = MeasureSpec.getSize(widthMeasureSpec) - compoundPaddingLeft - compoundPaddingRight
        if (mode != MeasureSpec.UNSPECIFIED && room > 0) {
            val fitted = fittedSize(room.toFloat())
            if (fitted != textSize) {
                fitting = true
                super.setTextSize(TypedValue.COMPLEX_UNIT_PX, fitted)
                fitting = false
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun fittedSize(room: Float): Float {
        probe.set(paint)
        probe.textSize = askedSize
        val text = text?.toString().orEmpty()
        val needed = if (fitWholeLine) probe.measureText(text) else widestWord(text)
        if (needed <= room || needed <= 0f) return askedSize
        return (askedSize * room / needed * SAFETY).coerceAtLeast(askedSize * MIN_SHARE)
    }

    private fun widestWord(text: String): Float {
        var widest = 0f
        var start = 0
        while (start < text.length) {
            while (start < text.length && text[start].isWhitespace()) start++
            var end = start
            while (end < text.length && !text[end].isWhitespace()) end++
            if (end > start) widest = maxOf(widest, probe.measureText(text, start, end))
            start = end
        }
        return widest
    }

    private companion object {
        /** Text never shrinks below this share of the size Font size asks for. */
        const val MIN_SHARE = 0.6f

        /** A hair of room, so rounding never pushes a word onto two lines. */
        const val SAFETY = 0.98f
    }
}
