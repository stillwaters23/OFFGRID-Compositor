package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Phase 1.3: the Lock glyph in the panel's top bar, drawn in code so no OS emoji is on screen.
 *
 * A padlock: the body is a rounded square, the shackle an arc. Unlocked draws the shackle
 * lifted and open on one side. Armed draws it closed with a dot in the body. Locked and live
 * draw it closed and solid. A tap locks now. A hold arms the lock, or disarms it. The panel
 * is hidden while locked, so this glyph never needs to unlock: that is the two-finger hold on
 * the stage, or Phone 2.
 */
internal class LockGlyphButton(context: Context) : View(context), Styled {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val box = RectF()
    private var edgeColor = Hud.WHITE
    private var decorColor = Hud.WHITE
    private var tick = true

    /** "unlocked", "armed", "locked", or "live". */
    var word: String = "unlocked"
        set(value) {
            if (field == value) return
            field = value
            contentDescription = describe(value)
            invalidate()
        }

    var onTap: (() -> Unit)? = null
    var onHold: (() -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        isLongClickable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = describe(word)
        setOnClickListener {
            if (tick) performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onTap?.invoke()
        }
        setOnLongClickListener {
            if (tick) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onHold?.invoke()
            true
        }
    }

    override fun applyLook(look: PanelLook) {
        edgeColor = look.edge
        decorColor = look.decor
        tick = look.tick
        foreground = pressGlow(context, look.edge)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width, height) * 0.62f
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        val closed = word != "unlocked"
        val bodyTop = top + size * 0.42f
        val line = Hud.dp(context, 1.4f)
        stroke.strokeWidth = line
        stroke.color = Hud.withAlpha(edgeColor, if (closed) 1f else 0.7f)
        fill.color = if (closed) Hud.withAlpha(edgeColor, 0.28f) else 0
        box.set(left + size * 0.12f, bodyTop, left + size * 0.88f, top + size)
        val corner = Hud.dp(context, 2f)
        if (closed) canvas.drawRoundRect(box, corner, corner, fill)
        canvas.drawRoundRect(box, corner, corner, stroke)
        // The shackle: an arc over the body. Open lifts it and leaves the right leg off the body.
        val shackleLeft = left + size * 0.27f
        val shackleRight = left + size * 0.73f
        val lift = if (closed) 0f else size * 0.14f
        val arcTop = top + size * 0.02f - lift
        box.set(shackleLeft, arcTop, shackleRight, arcTop + size * 0.5f)
        stroke.strokeWidth = line * 1.15f
        canvas.drawArc(box, 180f, 180f, false, stroke)
        canvas.drawLine(shackleLeft, arcTop + size * 0.25f, shackleLeft, bodyTop, stroke)
        val rightLegEnd = if (closed) bodyTop else bodyTop - lift - size * 0.06f
        canvas.drawLine(shackleRight, arcTop + size * 0.25f, shackleRight, rightLegEnd, stroke)
        if (word == "armed") {
            fill.color = decorColor
            canvas.drawCircle(left + size / 2f, bodyTop + (size - (bodyTop - top)) / 2f, Hud.dp(context, 2.2f), fill)
        } else if (word == "live") {
            stroke.strokeWidth = line
            stroke.color = decorColor
            val center = bodyTop + (size - (bodyTop - top)) / 2f
            canvas.drawLine(left + size * 0.36f, center, left + size * 0.64f, center, stroke)
        }
    }

    private fun describe(value: String): String = when (value) {
        "live" -> "Broadcast Lock. A share is live and the lock holds. Tap for nothing. Hold to arm."
        "locked" -> "Unlock the panel"
        "armed" -> "Broadcast Lock is armed. Tap to lock the panel. Hold to disarm."
        else -> "Lock the panel"
    }
}
