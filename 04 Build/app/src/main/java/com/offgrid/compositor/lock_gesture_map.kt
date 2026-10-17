package com.offgrid.compositor

import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Phase 1.3: the gestures that work on the stage without drawing anything (V2 section 7.3).
 *
 * No gesture starts at a screen edge: an edge swipe reveals the status bar with the Android 15
 * projection chip, and on One UI a two-finger top swipe opens the whole quick panel, all on air.
 * Lock and unlock are a two-finger hold of [HOLD_MS] anywhere in the middle [CENTER_SHARE] of
 * the stage. The panel, now that the pill is gone, opens with a one-finger hold of
 * [PANEL_OPEN_HOLD_MS] in the top-left zone, inset from the edges, unlocked only.
 *
 * Phase 1.8 (Invisible Hotkeys) adds the three- and four-finger gestures here.
 */
internal object LockGestureMap {
    /** The middle share of the stage, in each axis, where a lock gesture may start. */
    const val CENTER_SHARE = 0.6f

    /** Two fingers held still this long lock, or unlock when no share is visible. */
    const val HOLD_MS = 1500L

    /** One finger held still this long in the top-left zone opens the panel (unlocked only). */
    const val PANEL_OPEN_HOLD_MS = 1000L

    /** The top-left zone reaches this share of the width and of the height. */
    const val PANEL_OPEN_SHARE = 0.3f

    /**
     * What a two-finger hold does when its timer runs out. The hold means what it meant when the
     * fingers landed: a hold that began locked asks to unlock, one that began unlocked locks. If
     * the lock changed under the fingers (Phone 2, or the share ending), nothing more happens,
     * so the hold never turns an unlock into a lock.
     */
    fun holdOutcome(lockedAtStart: Boolean, lockedNow: Boolean): HoldOutcome = when {
        lockedAtStart != lockedNow -> HoldOutcome.NONE
        lockedNow -> HoldOutcome.UNLOCK
        else -> HoldOutcome.LOCK
    }

    /** Left, top, right, bottom of the center zone, in pixels. */
    fun centerZone(width: Int, height: Int): IntArray {
        val insetX = ((1f - CENTER_SHARE) / 2f * width).roundToInt()
        val insetY = ((1f - CENTER_SHARE) / 2f * height).roundToInt()
        return intArrayOf(insetX, insetY, width - insetX, height - insetY)
    }

    fun inCenterZone(x: Float, y: Float, width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        val zone = centerZone(width, height)
        return x >= zone[0] && x <= zone[2] && y >= zone[1] && y <= zone[3]
    }

    /** Left, top, right, bottom of the panel-open zone: the top-left corner, inset from both edges. */
    fun panelOpenZone(width: Int, height: Int, edgeInsetPx: Int): IntArray {
        val right = (width * PANEL_OPEN_SHARE).toInt()
        val bottom = (height * PANEL_OPEN_SHARE).toInt()
        return intArrayOf(edgeInsetPx, edgeInsetPx, right, bottom)
    }

    fun inPanelOpenZone(x: Float, y: Float, width: Int, height: Int, edgeInsetPx: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        val zone = panelOpenZone(width, height, edgeInsetPx)
        return x >= zone[0] && x <= zone[2] && y >= zone[1] && y <= zone[3]
    }
}

/** The touch phases the recognizers care about, free of android.view so tests stay on the JVM. */
internal enum class TouchKind { DOWN, POINTER_DOWN, MOVE, POINTER_UP, UP, CANCEL }

internal enum class HoldOutcome { LOCK, UNLOCK, NONE }

internal enum class HoldEvent { NONE, HOLD_STARTED, HOLD_CANCELLED }

/**
 * Recognizes [fingers] fingers that all land inside [zone] and then stay still.
 *
 * The caller starts a timer on [HoldEvent.HOLD_STARTED], drops it on [HoldEvent.HOLD_CANCELLED],
 * and calls [fired] when the timer runs out so the rest of the same touch is ignored. A hold
 * that starts with the wrong finger count, or outside the zone, never starts: a third finger,
 * a lifted finger, or a slide past [slopPx] cancels it.
 */
internal class FingerHoldRecognizer(
    private val fingers: Int,
    private val slopPx: Float,
    private val zone: (x: Float, y: Float, width: Int, height: Int) -> Boolean,
) {
    var holding = false
        private set

    /** True once the timer fired for this touch. Cleared when every finger is up. */
    var consumed = false
        private set

    private val startX = FloatArray(MAX_POINTERS)
    private val startY = FloatArray(MAX_POINTERS)
    private var armedCount = 0

    fun feed(kind: TouchKind, count: Int, xs: FloatArray, ys: FloatArray, width: Int, height: Int): HoldEvent {
        when (kind) {
            TouchKind.DOWN -> {
                consumed = false
                holding = false
                return tryStart(count, xs, ys, width, height)
            }
            TouchKind.POINTER_DOWN -> {
                if (holding) return cancel()
                return tryStart(count, xs, ys, width, height)
            }
            TouchKind.MOVE -> {
                if (!holding) return HoldEvent.NONE
                if (count != armedCount) return cancel()
                for (index in 0 until minOf(count, MAX_POINTERS)) {
                    if (hypot(xs[index] - startX[index], ys[index] - startY[index]) > slopPx) return cancel()
                }
                return HoldEvent.NONE
            }
            TouchKind.POINTER_UP -> return if (holding) cancel() else HoldEvent.NONE
            TouchKind.UP, TouchKind.CANCEL -> {
                val event = if (holding) cancel() else HoldEvent.NONE
                consumed = false
                return event
            }
        }
    }

    /** The timer ran out: the gesture is done, and the rest of this touch means nothing. */
    fun fired() {
        holding = false
        consumed = true
    }

    private fun tryStart(count: Int, xs: FloatArray, ys: FloatArray, width: Int, height: Int): HoldEvent {
        if (consumed || count != fingers || count > MAX_POINTERS) return HoldEvent.NONE
        for (index in 0 until count) {
            if (!zone(xs[index], ys[index], width, height)) return HoldEvent.NONE
            startX[index] = xs[index]
            startY[index] = ys[index]
        }
        holding = true
        armedCount = count
        return HoldEvent.HOLD_STARTED
    }

    private fun cancel(): HoldEvent {
        holding = false
        return HoldEvent.HOLD_CANCELLED
    }

    private companion object {
        const val MAX_POINTERS = 4
    }
}
