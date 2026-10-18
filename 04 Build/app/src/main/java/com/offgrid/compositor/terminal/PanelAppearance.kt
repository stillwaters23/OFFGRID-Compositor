package com.offgrid.compositor.terminal

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import kotlin.math.PI
import kotlin.math.cos

private const val STATUS_SP = 11f
private const val GUIDE_SP = 10.5f
private const val STILL_BREATH = 0.8f
private const val BREATH_SLOW_S = 6f
private const val BREATH_FAST_S = 1.2f

/** Reads the saved theme settings again, then repaints the whole panel. */
internal fun TerminalPanel.refreshLook() {
    look = store.look()
    syncClicker()
    applyLook()
}

internal fun TerminalPanel.applyLook() {
    val current = look
    glass.look = current
    glass.fullBleed = fullBleed
    glass.seeThroughAtFullSize = seeThroughAtFullSize
    paneGlass.look = current
    // The titanium frame is drawn inside the glass edge, so the content starts inside it.
    applyContentPadding()
    restyle(this, current)
    // The words dim a little with the glass, but never fade away.
    val contentAlpha = if (fullBleed && !seeThroughAtFullSize) 1f else 0.7f + 0.3f * current.opacity
    expanded.alpha = contentAlpha
    pill.alpha = contentAlpha
    scanLine.isVisible = current.scanLine && !minimized
    pillRing.isVisible = minimized && current.ring
    pillRing.spinning = minimized && current.ring && GlowClock.isLive
    headingRing?.let {
        it.isVisible = current.ring
        it.spinning = current.ring && GlowClock.isLive
    }
    styleTyper(statusLine, current.status, STATUS_SP)
    guidance?.let { styleGuidance(it) }
    RecordBarLook.apply(current, barViews, barTexts, barButtons)
    guides?.applyLook(current)
    mainFolders.tick = current.tick
    subFolders?.tick = current.tick
    val host = focusHost
    val box = focusBox
    if (host != null && box != null) {
        host.foreground = if (current.typingGlow) typingGlow(host, box, current) else null
    }
    refreshClock()
    invalidate()
}

/** Walks every view in the panel and repaints the ones that follow the theme. */
internal fun TerminalPanel.restyle(view: View, current: PanelLook) {
    if (view is Styled) view.applyLook(current)
    if (view is TextView) {
        (view.tag as? StyleTag)?.let { applyTextStyle(view, it, current) }
    }
    if (view is ViewGroup) {
        for (index in 0 until view.childCount) {
            restyle(view.getChildAt(index), current)
        }
    }
}

internal fun TerminalPanel.styleTyper(typer: LaserTypeView, color: Int, baseSp: Float) {
    val current = look
    typer.setTextColor(color)
    typer.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSp * current.fontScale)
    typer.cursorColor = current.cursor
    typer.cursorGlow = current.cursorGlow
    typer.glowStrength = current.glowStrength
    typer.glowBoost = current.glowBoost
    typer.speed = current.lettersPerSecond
    typer.animateTyping = current.laserTyping
    typer.invalidate()
}

internal fun TerminalPanel.styleGuidance(typer: LaserTypeView) {
    val current = look
    styleTyper(typer, current.text, GUIDE_SP)
    typer.background = if (current.sapphire) {
        // The typed guidance sits on its own thick sapphire pane.
        PanelGlassDrawable(density, 0f, pane = true).apply { look = current }
    } else {
        GradientDrawable().apply {
            val background = if (current.base == Theme.SYSTEM) current.skin else
                Hud.withAlpha(Hud.blend(current.skin, Hud.INK, 0.6f), 0.55f)
            setColor(background)
            cornerRadius = chromeRadius(context, current)
            setStroke(dpi(1f), if (current.base == Theme.SYSTEM) current.text else Hud.withAlpha(current.text, 0.22f))
        }
    }
}

/** The glow moves only while it is on screen, and never when the phone asks for less motion. */
internal fun TerminalPanel.refreshClock() {
    val current = look
    val glowing = current.glowStrength > 0f && (current.outerGlowOn || current.edgeGlow)
    val want = shownOnScreen && glowing && current.pulse && GlowClock.motionAllowed
    if (want && !ticking) {
        ticking = true
        GlowClock.join(this)
    } else if (!want && ticking) {
        ticking = false
        GlowClock.leave(this)
    }
}

/** 0 to 1, rising and falling smoothly like slow breathing. Speed 0 is about 6 seconds a breath. */
internal fun TerminalPanel.breathAt(seconds: Float, speed: Float): Float {
    val period = BREATH_SLOW_S - (BREATH_SLOW_S - BREATH_FAST_S) * speed
    return 0.5f - 0.5f * cos(seconds / period * 2f * PI.toFloat())
}
