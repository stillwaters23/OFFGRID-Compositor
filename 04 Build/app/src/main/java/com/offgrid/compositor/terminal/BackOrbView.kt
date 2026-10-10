package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.offgrid.compositor.R

/**
 * The round Back control at the top left of the panel.
 * Black fill, a bioluminescent ring, and a left arrow, all drawn in code.
 */
internal class BackOrbView(context: Context) : View(context), Styled {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Hud.INK }
    private val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = NeonPalette.BLACKLIGHT_WHITE
    }
    private val stamp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var accent = NeonPalette.CYAN_GLOW
    private var halo = NeonPalette.ELECTRIC_PURPLE
    private var glowOn = true
    var breath = 0.5f
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }
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
    }

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width, height)
        if (size <= 0) return
        val cx = width / 2f
        val cy = height / 2f
        val radius = size * BioluminescentPowerIcon.DISC_SHARE
        if (glowOn) {
            val layers = BioluminescentPowerIcon.layers(size, accent, halo, resources.displayMetrics.density)
            if (layers != null) {
                val pulse = 0.85f + 0.3f * breath
                BioluminescentPowerIcon.paintLayers(
                    canvas,
                    (width - size) / 2f,
                    (height - size) / 2f,
                    layers,
                    pulse,
                    stamp,
                )
            }
        }
        fill.color = if (isPressed) Hud.withAlpha(accent, 0.35f) else Hud.INK
        canvas.drawCircle(cx, cy, radius, fill)
        arrow.strokeWidth = radius * 0.16f
        arrow.color = if (glowOn) BioluminescentPowerIcon.hotCore(accent) else Hud.WHITE
        val arm = radius * 0.46f
        canvas.drawLine(cx + arm * 0.2f, cy - arm, cx - arm * 0.55f, cy, arrow)
        canvas.drawLine(cx - arm * 0.55f, cy, cx + arm * 0.2f, cy + arm, arrow)
    }

    override fun applyLook(look: PanelLook) {
        accent = look.edge
        halo = look.outerGlow
        glowOn = look.glowStrength > 0f
        arrow.color = look.text
        invalidate()
    }
}
