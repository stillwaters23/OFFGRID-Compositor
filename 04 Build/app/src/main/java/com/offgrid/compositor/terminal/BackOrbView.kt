package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.offgrid.compositor.R
import android.view.View

internal class BackOrbView(context: Context) : View(context) {
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = Hud.dp(context, 2.5f)
        color = NeonPalette.UV_VIOLET
        setShadowLayer(Hud.dp(context, 7f), 0f, 0f, NeonPalette.ELECTRIC_PURPLE)
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Hud.INK }
    private val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = Hud.dp(context, 2f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = NeonPalette.BLACKLIGHT_WHITE
    }
    private val arrowPath = android.graphics.Path()
    private val radius = Hud.dp(context, 19f)
    var enabledState: Boolean = true
        set(value) {
            field = value
            alpha = if (value) 1f else 0.35f
            isEnabled = value
            invalidate()
        }

    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.terminal_back_label)
        arrowPath.moveTo(Hud.dp(context, 27f), Hud.dp(context, 24f))
        arrowPath.lineTo(Hud.dp(context, 19f), Hud.dp(context, 16f))
        arrowPath.lineTo(Hud.dp(context, 27f), Hud.dp(context, 8f))
        arrowPath.moveTo(Hud.dp(context, 19f), Hud.dp(context, 16f))
        arrowPath.lineTo(Hud.dp(context, 33f), Hud.dp(context, 16f))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        fill.color = if (isPressed) Hud.withAlpha(NeonPalette.UV_VIOLET, 0.35f) else Hud.INK
        canvas.drawCircle(cx, cy, radius, fill)
        canvas.drawCircle(cx, cy, radius, ring)
        canvas.drawPath(arrowPath, arrow)
    }
}
