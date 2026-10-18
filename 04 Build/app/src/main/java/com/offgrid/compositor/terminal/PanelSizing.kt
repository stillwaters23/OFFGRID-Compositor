package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import androidx.core.animation.doOnEnd
import com.offgrid.compositor.R
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val MIN_WIDTH_DP = 280f
private const val MIN_HEIGHT_DP = 260f
private const val RESIZE_ZONE_DP = 24f
private const val STEP_DP = 40f
private const val CENTER_MS = 420L
private const val SNAP_DP = 12f

/** The widths the glass may take on this screen, in pixels: smallest readable, then the whole screen. */
internal fun TerminalPanel.widthRange(areaWidth: Int): Pair<Int, Int> {
    return PanelGeometry.sizeRange(areaWidth, dpi(MIN_WIDTH_DP))
}

/** The heights the glass may take: smallest readable, then the whole screen, edge to edge. */
internal fun TerminalPanel.heightRange(areaHeight: Int): Pair<Int, Int> {
    return PanelGeometry.sizeRange(areaHeight, dpi(MIN_HEIGHT_DP))
}

/** Where the glass's top left corner goes to sit in the exact middle of the screen. */
internal fun TerminalPanel.middleFor(glassWidth: Int, glassHeight: Int): Pair<Float, Float> {
    return (areaWidth() - glassWidth) / 2f to (areaHeight() - glassHeight) / 2f
}

internal fun TerminalPanel.sizeFor(percent: Int, range: Pair<Int, Int>): Int = PanelGeometry.sizeFor(percent, range)

internal fun TerminalPanel.percentFor(size: Float, range: Pair<Int, Int>): Int = PanelGeometry.percentFor(size, range)

/** The screen width. While the keyboard is open, the size from before it opened. */
internal fun TerminalPanel.areaWidth(): Int {
    val now = (parent as? View)?.width ?: 0
    return if (keyboardHeight > 0 && stableWidth > 0) stableWidth else now
}

/** The screen height. While the keyboard is open, the size from before it opened, so nothing jumps. */
internal fun TerminalPanel.areaHeight(): Int {
    val now = (parent as? View)?.height ?: 0
    return if (keyboardHeight > 0 && stableHeight > 0) maxOf(stableHeight, now) else now
}

internal fun TerminalPanel.resizeZoneAt(x: Float, y: Float): PanelResizeZone {
    if (minimized) return PanelResizeZone.NONE
    val screen = visibleWindowEdges ?: edges
    val glassLeft = (translationX + margin).roundToInt()
    val glassRight = glassLeft + width - margin * 2
    val glassBottom = (userY + height - margin).roundToInt()
    val gestureLeft = (screen.gestureLeft - glassLeft).coerceIn(0, screen.gestureLeft).toFloat()
    val gestureRight = (glassRight - (areaWidth() - screen.gestureRight))
        .coerceIn(0, screen.gestureRight).toFloat()
    val gestureBottom = (glassBottom - (areaHeight() - screen.gestureBottom))
        .coerceIn(0, screen.gestureBottom).toFloat()
    return PanelGeometry.resizeZone(
        x = x - margin,
        y = y - margin,
        width = (width - margin * 2).toFloat(),
        height = (height - margin * 2).toFloat(),
        depth = dp(RESIZE_ZONE_DP),
        gestureLeft = gestureLeft,
        gestureRight = gestureRight,
        gestureBottom = gestureBottom,
    )
}

internal fun TerminalPanel.startBottomResize(zone: PanelResizeZone, event: MotionEvent) {
    resizeZone = zone
    resizeDownX = event.rawX
    resizeDownY = event.rawY
    resizeStartWidth = (width - margin * 2).toFloat()
    resizeStartHeight = (height - margin * 2).toFloat()
    resizeMoved = false
    stopCentering()
    startResizeBarPulse()
    parent?.requestDisallowInterceptTouchEvent(true)
}

internal fun TerminalPanel.handleBottomResize(event: MotionEvent) {
    when (event.actionMasked) {
        MotionEvent.ACTION_MOVE -> {
            val dx = event.rawX - resizeDownX
            val dy = event.rawY - resizeDownY
            if (!resizeMoved && hypot(dx, dy) <= ViewConfiguration.get(context).scaledTouchSlop) return
            resizeMoved = true
            hideKeyboard()
            glass.holdMasks = true
            val zone = resizeZone ?: return
            val widthChange = PanelGeometry.resizeWidthDelta(zone, dx)
            resizeGlassTo(resizeStartWidth + widthChange, resizeStartHeight + dy)
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            if (resizeMoved) finishLiveSize() else glass.holdMasks = false
            stopResizeBarPulse()
            resizeZone = null
            resizeMoved = false
        }
    }
}

internal fun TerminalPanel.startResizeBarPulse() {
    val previous = resizePulseAnimator
    resizePulseAnimator = null
    previous?.cancel()
    if (!ValueAnimator.areAnimatorsEnabled()) {
        glass.resizeBarPulse = 1.6f
        return
    }
    resizePulseAnimator = ValueAnimator.ofFloat(1f, 1.6f).apply {
        duration = 180L
        interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
        addUpdateListener { glass.resizeBarPulse = it.animatedValue as Float }
        doOnEnd {
            if (resizePulseAnimator === this && resizeZone != null) {
                resizePulseAnimator = ValueAnimator.ofFloat(1.2f, 1.6f).apply {
                    duration = 1_200L
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener { glass.resizeBarPulse = it.animatedValue as Float }
                    start()
                }
            }
        }
        start()
    }
}

internal fun TerminalPanel.stopResizeBarPulse() {
    val pulse = glass.resizeBarPulse
    val previous = resizePulseAnimator
    resizePulseAnimator = null
    previous?.cancel()
    if (!ValueAnimator.areAnimatorsEnabled()) {
        glass.resizeBarPulse = 1f
        return
    }
    resizePulseAnimator = ValueAnimator.ofFloat(pulse, 1f).apply {
        duration = 220L
        interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
        addUpdateListener { glass.resizeBarPulse = it.animatedValue as Float }
        doOnEnd {
            glass.resizeBarPulse = 1f
            resizePulseAnimator = null
        }
        start()
    }
}

/** Sets a new glass size in pixels. It never goes past the screen, or below the smallest size. */
internal fun TerminalPanel.resizeGlassTo(newWidth: Float, newHeight: Float) {
    if (parent !is View) return
    val widths = widthRange(areaWidth())
    val heights = heightRange(areaHeight())
    val roomWidth = (areaWidth() - (translationX + margin)).coerceIn(widths.first.toFloat(), widths.second.toFloat())
    val roomHeight = (areaHeight() - (userY + margin)).coerceIn(heights.first.toFloat(), heights.second.toFloat())
    val glassWidth = PanelGeometry.resizeWidth(0f, newWidth, widths.first.toFloat(), roomWidth)
    val glassHeight = PanelGeometry.resizeHeight(0f, newHeight, heights.first.toFloat(), roomHeight)
    liveSize = glassWidth to glassHeight
    showSizeNumbers(percentFor(glassWidth, widths), percentFor(glassHeight, heights))
    requestLayout()
}

/** Saves the size a finger chose as Width and Height, from 0 to 100, and says so. */
internal fun TerminalPanel.finishLiveSize() {
    val live = liveSize
    glass.holdMasks = false
    if (live != null) {
        val widthPercent = percentFor(live.first, widthRange(areaWidth()))
        val heightPercent = percentFor(live.second, heightRange(areaHeight()))
        store.setNumber(LookKeys.WIDTH, widthPercent)
        store.setNumber(LookKeys.HEIGHT, heightPercent)
        liveSize = null
        showSizeNumbers(widthPercent, heightPercent)
        requestLayout()
        post { savePosition() }
        say(context.getString(R.string.terminal_size_saved, widthPercent, heightPercent))
        if (look.tick) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

/** For screen readers: the Bigger and Smaller actions on the panel. */
internal fun TerminalPanel.resizeBy(step: Float) {
    if (minimized) return
    resizeGlassTo((width - margin * 2) + step, (height - margin * 2) + step)
    finishLiveSize()
}

/** A typed Width or Height, from the top boxes or the Size folder. */
internal fun TerminalPanel.setSizePercent(key: String, value: Int, announce: Boolean) {
    store.setNumber(key, value)
    liveSize = null
    showSizeNumbers(store.number(LookKeys.WIDTH), store.number(LookKeys.HEIGHT))
    if (announce) {
        requestLayout()
        val label = context.getString(if (key == LookKeys.WIDTH) R.string.terminal_width else R.string.terminal_height)
        say(context.getString(R.string.terminal_number_saved, label, value))
        post { savePosition() }
    }
}

/** Shows Width and Height in every box that shows them, except one being typed in. */
internal fun TerminalPanel.showSizeNumbers(widthPercent: Int, heightPercent: Int) {
    topBar.setDimensions(widthPercent, heightPercent)
    sizeBank?.show(LookKeys.WIDTH, widthPercent)
    sizeBank?.show(LookKeys.HEIGHT, heightPercent)
    for ((key, row) in sizeReels) {
        row.show(if (key == LookKeys.WIDTH) widthPercent else heightPercent)
    }
    for ((key, box) in sizeRows) {
        val value = if (key == LookKeys.WIDTH) widthPercent else heightPercent
        if (!box.hasFocus() && box.value != value) box.show(value)
    }
}

/** Center panel: glides the panel to the middle of the screen, then saves the place. */
internal fun TerminalPanel.centerPanel() {
    val area = parent as? View ?: return
    stopCentering()
    val fromX = translationX + margin
    val fromY = userY + margin
    if (area.width == 0) return
    val (toX, toY) = middleFor(width - margin * 2, height - margin * 2)
    if (!ValueAnimator.areAnimatorsEnabled()) {
        moveGlassTo(toX, toY)
        savePosition()
    } else {
        centerAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = CENTER_MS
            interpolator = DecelerateInterpolator(1.4f)
            addUpdateListener {
                val t = it.animatedValue as Float
                moveGlassTo(fromX + (toX - fromX) * t, fromY + (toY - fromY) * t)
            }
            doOnEnd {
                if (centerAnim === it) {
                    centerAnim = null
                    savePosition()
                }
            }
            start()
        }
    }
    say(context.getString(R.string.terminal_centered))
}

internal fun TerminalPanel.stopCentering() {
    val running = centerAnim ?: return
    centerAnim = null
    running.cancel()
    savePosition()
}

/** Moves the glass so its top left corner is at x, y. The glass always stays on the screen. */
internal fun TerminalPanel.moveGlassTo(x: Float, y: Float) {
    if (parent !is View) return
    val glassWidth = (width - margin * 2).toFloat()
    val glassHeight = (height - margin * 2).toFloat()
    val glassX = x.coerceIn(0f, (areaWidth() - glassWidth).coerceAtLeast(0f))
    val glassY = y.coerceIn(0f, (areaHeight() - glassHeight).coerceAtLeast(0f))
    translationX = glassX - margin
    userY = glassY - margin
    translationY = userY - keyboardLift
    refreshEdgeRoom()
    refreshCoveredBars()
}

/**
 * Where the glass reaches an edge of the screen, its words and buttons move in, out of
 * the bars, the camera hole, and the round corners. The resize edge also stays out of
 * the Back swipe area. The glass itself still fills the screen.
 */
internal fun TerminalPanel.refreshEdgeRoom() {
    if (parent !is View || minimized) {
        if (edgeRoom.left != 0 || edgeRoom.top != 0 || edgeRoom.right != 0 || edgeRoom.bottom != 0) {
            edgeRoom.set(0, 0, 0, 0)
            applyContentPadding()
        }
        return
    }
    val areaWidth = areaWidth()
    val areaHeight = areaHeight()
    if (areaWidth == 0 || areaHeight == 0) return
    val glassLeft = (translationX + margin).roundToInt()
    val glassTop = (userY + margin).roundToInt()
    val glassRight = glassLeft + width - margin * 2
    val glassBottom = glassTop + height - margin * 2
    // A round screen corner cuts in about this far. The buttons stay clear of it.
    val screen = if (fullBleed) visibleWindowEdges ?: edges else edges
    val inset = PanelGeometry.contentInset(
        glassLeft = glassLeft,
        glassTop = glassTop,
        glassRight = glassRight,
        glassBottom = glassBottom,
        areaWidth = areaWidth,
        areaHeight = areaHeight,
        edgeTop = screen.top,
        edgeBottom = screen.bottom,
        edgeLeft = screen.left,
        edgeRight = screen.right,
        gestureBottom = screen.gestureBottom,
        cornerRadius = screen.cornerRadius,
        fullBleed = fullBleed,
    )
    if (edgeRoom.left == inset.left && edgeRoom.top == inset.top &&
        edgeRoom.right == inset.right && edgeRoom.bottom == inset.bottom
    ) {
        return
    }
    edgeRoom.set(inset.left, inset.top, inset.right, inset.bottom)
    applyContentPadding()
}

/** The glow margin, the titanium frame, and any room for the screen's edges. */
internal fun TerminalPanel.applyContentPadding() {
    val base = margin + glass.frameInset.roundToInt()
    val left = base + edgeRoom.left
    val top = base + edgeRoom.top
    val right = base + edgeRoom.right
    val bottom = base + edgeRoom.bottom
    if (paddingLeft != left || paddingTop != top || paddingRight != right || paddingBottom != bottom) {
        setPadding(left, top, right, bottom)
    }
}

internal fun TerminalPanel.savePosition() {
    val areaWidth = areaWidth()
    val areaHeight = areaHeight()
    if (areaWidth > 0 && areaHeight > 0) {
        prefs.setPosition((translationX + margin) / areaWidth, (userY + margin) / areaHeight)
    }
}
