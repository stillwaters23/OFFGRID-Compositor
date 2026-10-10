package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import androidx.core.animation.doOnEnd

private const val EDGE_TOUCH_DP = 26f
private const val RIPPLE_MS = 650L

/**
 * Touch glow: a touch on the frame, the title bar, or near the edge lights that spot
 * with a short ripple of living light, then fades. Never with less motion asked for.
 */
internal fun TerminalPanel.touchGlow(x: Float, y: Float) {
    if (!look.touchGlow || !ValueAnimator.areAnimatorsEnabled()) return
    glassArea(glassBox)
    val band = dp(EDGE_TOUCH_DP)
    val nearEdge = x - glassBox.left < band || glassBox.right - x < band ||
        y - glassBox.top < band || glassBox.bottom - y < band
    val onTitleBar = !minimized && y < glassBox.top + glass.frameInset + edgeRoom.top + topBar.height
    if (!nearEdge && !onTitleBar && !minimized) return
    val ripple = TerminalPanel.Ripple(x, y)
    ripples += ripple
    ValueAnimator.ofFloat(0f, 1f).apply {
        duration = RIPPLE_MS
        interpolator = DecelerateInterpolator(1.6f)
        addUpdateListener {
            ripple.progress = it.animatedValue as Float
            invalidate()
        }
        doOnEnd {
            ripples.remove(ripple)
            invalidate()
        }
        start()
    }
}

/** Each ripple: a soft bloom that grows and fades, and the nearby edge line burning brighter. */
internal fun TerminalPanel.drawRipples(canvas: Canvas) {
    if (ripples.isEmpty()) return
    val color = look.edge
    if (rippleBloom == null || rippleLight == null || rippleColor != color) {
        rippleColor = color
        fun gradient() = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(Hud.withAlpha(Hud.lighten(color, 0.6f), 0.9f), Hud.withAlpha(color, 0.5f), Hud.withAlpha(color, 0f)),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        rippleBloom = gradient()
        rippleLight = gradient()
    }
    val bloom = rippleBloom ?: return
    val light = rippleLight ?: return
    ripplePaint.shader = bloom
    rippleEdge.shader = light
    rippleEdge.strokeWidth = dp(2.4f)
    val corner = chromeRadius(context, look) * 3f
    glassArea(glassBox)
    for (ripple in ripples) {
        val fade = (1f - ripple.progress)
        val radius = dp(12f) + dp(52f) * ripple.progress
        ripplePaint.alpha = (255f * fade * fade * 0.85f).toInt()
        canvas.save()
        canvas.translate(ripple.x, ripple.y)
        canvas.scale(radius, radius)
        canvas.drawCircle(0f, 0f, 1f, ripplePaint)
        canvas.restore()
        rippleMatrix.setScale(radius * 1.6f, radius * 1.6f)
        rippleMatrix.postTranslate(ripple.x, ripple.y)
        light.setLocalMatrix(rippleMatrix)
        rippleEdge.alpha = (255f * fade).toInt()
        canvas.drawRoundRect(glassBox, corner, corner, rippleEdge)
    }
}

/** Typing glow: the edge of the setting being typed in lights softly. Its row draws it. */
internal fun TerminalPanel.showTypingGlow(now: View?) {
    val box = if (now is EditText && look.typingGlow && isDescendant(now)) now else null
    val next = box?.parent as? View
    if (next === focusHost && box === focusBox) return
    focusHost?.foreground = null
    focusHost = next
    focusBox = box
    if (next != null) next.foreground = typingGlow(next, box, look)
}

internal fun TerminalPanel.typingGlow(host: View, box: EditText, current: PanelLook) =
    FocusGlowDrawable(density, current.edge, chromeRadius(context, current), box, wholeRow = host is NumberRow)

internal fun TerminalPanel.isDescendant(view: View): Boolean {
    var parentView = view.parent
    while (parentView != null) {
        if (parentView === this) return true
        parentView = parentView.parent
    }
    return false
}
