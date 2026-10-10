package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.offgrid.compositor.audio.voice.MicState
import com.offgrid.compositor.R

/** A compact top-bar mic switch. Blocked capture is unmistakably crossed out in red. */
internal class MicControlView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val stand = Path()
    private var state = MicState.OFF

    init {
        isClickable = true
        isFocusable = true
        foreground = pressGlow(context, Hud.HOLO_CYAN)
        setMicState(MicState.OFF)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun setMicState(value: MicState) {
        state = value
        contentDescription = context.getString(
            when (value) {
                MicState.ON -> R.string.mic_toggle_on
                MicState.OFF -> R.string.mic_toggle_off
                MicState.BLOCKED -> R.string.mic_toggle_blocked
            },
        )
        isSelected = value == MicState.ON
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val unit = minOf(width, height) * 0.34f
        paint.strokeWidth = (unit * 0.11f).coerceAtLeast(Hud.dp(context, 1.5f))
        paint.color = when (state) {
            MicState.ON -> Hud.HOLO_LASER
            MicState.OFF -> Hud.HOLO_CYAN
            MicState.BLOCKED -> 0xFFFF4545.toInt()
        }
        canvas.drawRoundRect(
            cx - unit * 0.28f, cy - unit * 0.62f, cx + unit * 0.28f, cy + unit * 0.23f,
            unit * 0.28f, unit * 0.28f, paint,
        )
        stand.reset()
        stand.moveTo(cx - unit * 0.52f, cy - unit * 0.02f)
        stand.quadTo(cx - unit * 0.52f, cy + unit * 0.7f, cx, cy + unit * 0.7f)
        stand.quadTo(cx + unit * 0.52f, cy + unit * 0.7f, cx + unit * 0.52f, cy - unit * 0.02f)
        canvas.drawPath(stand, paint)
        canvas.drawLine(cx, cy + unit * 0.7f, cx, cy + unit * 1.02f, paint)
        canvas.drawLine(cx - unit * 0.38f, cy + unit * 1.02f, cx + unit * 0.38f, cy + unit * 1.02f, paint)
        if (state == MicState.BLOCKED) {
            paint.strokeWidth *= 1.3f
            canvas.drawLine(cx - unit * 0.95f, cy - unit * 0.92f, cx + unit * 0.95f, cy + unit * 1.0f, paint)
        }
    }
}
