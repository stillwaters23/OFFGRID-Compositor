package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
import android.view.View.VISIBLE
import android.view.ViewConfiguration
import android.view.View.OnTouchListener
import android.widget.EditText
import android.widget.ScrollView
import androidx.core.view.isVisible
import com.offgrid.compositor.R
import kotlin.math.hypot

private const val SNAP_DP = 12f
private const val EDGE_DP = 8f
private const val COVER_SLACK_DP = 2f
private const val COVER_MS = 180L
private const val GRID_FADE_MS = 220L
private const val CLOSE_MS = 220L
private const val OPEN_MS = 420L

/**
 * Drag to move the panel. A short tap counts as a normal tap.
 * With Center guide on, a soft cross shows the middle, and the panel clicks into the
 * middle when it comes close.
 */
internal fun TerminalPanel.dragHandle(): OnTouchListener {
    var downX = 0f
    var downY = 0f
    var startX = 0f
    var startY = 0f
    var dragging = false
    var snappedX = false
    var snappedY = false
    val slop = ViewConfiguration.get(context).scaledTouchSlop
    return OnTouchListener { view, event ->
        if (fullBleed) return@OnTouchListener true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                stopCentering()
                downX = event.rawX
                downY = event.rawY
                startX = translationX + margin
                startY = userY + margin
                dragging = false
                snappedX = false
                snappedY = false
                view.isPressed = true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!dragging && hypot(dx, dy) > slop) {
                    dragging = true
                    view.isPressed = false
                    parent?.requestDisallowInterceptTouchEvent(true)
                    if (look.centerGuide) {
                        guides?.setMiddle(areaWidth() / 2f, areaHeight() / 2f)
                        guides?.showGuides()
                    }
                }
                if (dragging) {
                    var x = startX + dx
                    var y = startY + dy
                    if (look.centerGuide) {
                        val glassWidth = width - margin * 2
                        val glassHeight = height - margin * 2
                        val (middleX, middleY) = middleFor(glassWidth, glassHeight)
                        val near = dp(SNAP_DP)
                        val nowX = kotlin.math.abs(x - middleX) < near
                        val nowY = kotlin.math.abs(y - middleY) < near
                        if (nowX) x = middleX
                        if (nowY) y = middleY
                        if ((nowX && !snappedX) || (nowY && !snappedY)) {
                            if (look.tick) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        snappedX = nowX
                        snappedY = nowY
                        guides?.setSnapped(nowX, nowY)
                    }
                    moveGlassTo(x, y)
                }
            }
            MotionEvent.ACTION_UP -> {
                view.isPressed = false
                if (dragging) savePosition() else view.performClick()
                dragging = false
                guides?.hideGuides()
            }
            MotionEvent.ACTION_CANCEL -> {
                view.isPressed = false
                if (dragging) savePosition()
                dragging = false
                guides?.hideGuides()
            }
        }
        true
    }
}

/**
 * A bar under the open panel fades away while the glass covers it, and comes back when
 * the panel moves off it, shrinks to its tag, or hides. Taps there already go to the panel.
 */
internal fun TerminalPanel.refreshCoveredBars() {
    if (barViews.isEmpty()) return
    val open = isAttachedToWindow && visibility == VISIBLE && !minimized && !barNeedsTap
    val here = IntArray(2)
    val there = IntArray(2)
    if (open) getLocationInWindow(here)
    val glassLeft = here[0] + margin
    val glassTop = here[1] + margin
    val glassRight = here[0] + width - margin
    val glassBottom = here[1] + height - margin
    for (bar in barViews) {
        var covered = false
        if (open && bar.height > 0) {
            bar.getLocationInWindow(there)
            val across = minOf(glassRight, there[0] + bar.width) - maxOf(glassLeft, there[0])
            val down = minOf(glassBottom, there[1] + bar.height) - maxOf(glassTop, there[1])
            // Only a bar the glass hides completely fades. A bar that peeks out stays,
            // so a button you can tap is always a button you can see.
            val slack = dpi(COVER_SLACK_DP)
            covered = across >= bar.width - slack && down >= bar.height - slack
        }
        if (covered == bar in coveredBars) continue
        if (covered) coveredBars += bar else coveredBars -= bar
        bar.importantForAccessibility =
            if (covered) IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else IMPORTANT_FOR_ACCESSIBILITY_AUTO
        val target = if (covered) 0f else 1f
        bar.animate().cancel()
        if (ValueAnimator.areAnimatorsEnabled()) {
            bar.animate().alpha(target).setDuration(COVER_MS).start()
        } else {
            bar.alpha = target
        }
    }
}

/**
 * Lifts the panel so its bottom clears the keyboard. A very tall panel can't lift that far.
 * Then the open folder scrolls the box being typed in up above the keyboard.
 */
internal fun TerminalPanel.fitAboveKeyboard() {
    val area = parent as? View ?: return
    val visibleBottom = area.height - keyboardHeight - dp(EDGE_DP)
    val glassTop = userY + margin
    val glassBottom = userY + height - margin
    keyboardLift = if (keyboardHeight > 0 && glassBottom > visibleBottom) {
        (glassBottom - visibleBottom).coerceAtMost((glassTop - edges.top).coerceAtLeast(0f))
    } else {
        0f
    }
    translationY = userY - keyboardLift
    refreshCoveredBars()
    val cover = if (keyboardHeight > 0) (glassBottom - keyboardLift - visibleBottom).coerceAtLeast(0f) else 0f
    if (cover.toInt() != keyboardCover) {
        keyboardCover = cover.toInt()
        setDetailPadding()
    }
    if (keyboardCover > 0) post { keepFocusAboveKeyboard() }
}

internal fun TerminalPanel.keepFocusAboveKeyboard() {
    val focused = findFocus() as? EditText ?: return
    val area = parent as? View ?: return
    if (keyboardHeight <= 0) return
    val spot = IntArray(2)
    area.getLocationOnScreen(spot)
    val keyboardTop = spot[1] + area.height - keyboardHeight - dp(EDGE_DP)
    focused.getLocationOnScreen(spot)
    val hidden = spot[1] + focused.height - keyboardTop
    if (hidden > 0f) {
        // The box's own list scrolls: an open folder, or the ⋮ menu card.
        val list = generateSequence(focused.parent) { it.parent }.filterIsInstance<ScrollView>().firstOrNull() ?: detailScroll
        list.smoothScrollBy(0, hidden.toInt())
    }
}

internal fun TerminalPanel.showMinimized(value: Boolean, animate: Boolean, save: Boolean = true) {
    if (value && fullBleed) {
        fullBleed = false
        margin = glowMargin
        glass.margin = margin.toFloat()
        glass.fullBleed = false
        applyContentPadding()
    }
    minimized = value
    glass.resizeBarVisible = !value
    // Never persist a hidden panel. A restart, or Back, always has a way out.
    if (save) prefs.minimized = false
    // Royalty's own tap decides from now on.
    if (save) steppedAside = false
    if (value) {
        hideKeyboard()
        menu.close(animate = false)
    }
    expanded.isVisible = !value
    pill.isVisible = value && !hiddenForLock && !hiddenForPlayback
    pillRing.isVisible = value && look.ring
    pillRing.spinning = value && look.ring
    refreshEdgeRoom()
    scanLine.isVisible = !value && look.scanLine
    applyHidden()
    requestLayout()
    if (animate && !value) {
        post { hologramIn(this, margin.toFloat(), margin.toFloat(), isDetail = false) }
        say(context.getString(R.string.terminal_panel_open))
    } else if (value) {
        say(context.getString(R.string.terminal_panel_hidden))
    }
}
