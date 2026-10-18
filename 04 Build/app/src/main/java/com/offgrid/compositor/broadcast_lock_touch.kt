package com.offgrid.compositor

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.min

/**
 * Phase 1.3: every touch on the camera screen passes through here first (V2 section 7.3).
 *
 * Locked, this is the touch sink: nothing under the stage gets a finger, so no row, toggle,
 * or drawing can react on air. The one thing that still works is the two-finger hold of
 * [LockGestureMap.HOLD_MS] in the middle of the stage, which asks to unlock. Unlocked, the same
 * hold locks, and a one-finger hold of [LockGestureMap.PANEL_OPEN_HOLD_MS] near the top-left
 * corner opens a shrunk panel. Neither starts at a screen edge.
 *
 * Unlocked, touches flow to the views as before while the recognizers watch. When a hold fires,
 * the views get ACTION_CANCEL so a panel pinch or an ink stroke ends cleanly, and the rest of
 * that touch is swallowed.
 */
internal class LockTouch(
    private val chrome: BroadcastLockChrome,
    slopPx: Float,
    private val panelOpenInsetPx: Int,
    private val panelHidden: () -> Boolean,
    private val playbackShowing: () -> Boolean,
    private val cancelViews: () -> Unit,
    private val openPanel: () -> Unit,
    private val closePlayback: () -> Unit,
    private val tick: () -> Boolean,
    private val haptic: (Int) -> Unit,
    private val unlockHit: (x: Float, y: Float) -> Boolean = { _, _ -> false },
) {
    private val main = Handler(Looper.getMainLooper())
    private val lockHold = FingerHoldRecognizer(2, slopPx) { x, y, w, h -> LockGestureMap.inCenterZone(x, y, w, h) }
    private val panelHold = FingerHoldRecognizer(1, slopPx) { x, y, w, h ->
        LockGestureMap.inPanelOpenZone(x, y, w, h, panelOpenInsetPx)
    }
    private val lockTimer = Runnable { fireLock() }
    private val panelTimer = Runnable { firePanelOpen() }
    private val xs = FloatArray(MAX_POINTERS)
    private val ys = FloatArray(MAX_POINTERS)
    private var swallowRest = false
    private var tapDownX = 0f
    private var tapDownY = 0f
    private var tapDownAt = 0L
    private val tapSlop = slopPx

    /** True when the activity must not pass [event] to its views. */
    fun dispatch(event: MotionEvent, width: Int, height: Int): Boolean {
        val locked = chrome.store.locked
        val kind = kindOf(event)
        if (kind == null) return locked
        val count = min(event.pointerCount, MAX_POINTERS)
        for (index in 0 until count) {
            xs[index] = event.getX(index)
            ys[index] = event.getY(index)
        }
        if (kind == TouchKind.DOWN) swallowRest = false
        when (lockHold.feed(kind, count, xs, ys, width, height)) {
            HoldEvent.HOLD_STARTED -> {
                main.removeCallbacks(lockTimer)
                main.postDelayed(lockTimer, LockGestureMap.HOLD_MS)
                if (tick()) haptic(HapticFeedbackConstants.CLOCK_TICK)
            }
            HoldEvent.HOLD_CANCELLED -> main.removeCallbacks(lockTimer)
            HoldEvent.NONE -> Unit
        }
        if (locked) {
            // Unlock strip must still receive taps. The sink would recreate Royalty's freeze:
            // Unlock is on screen, but no finger can reach it.
            if (unlockHit(event.x, event.y)) return false
            // The sink. A finger down here never reaches a view, and never draws.
            if (kind == TouchKind.UP || kind == TouchKind.CANCEL) main.removeCallbacks(panelTimer)
            return true
        }
        if (swallowRest) {
            if (kind == TouchKind.UP || kind == TouchKind.CANCEL) swallowRest = false
            return true
        }
        if (playbackShowing()) {
            // Playback: a short tap on the stage goes back to the camera. Nothing else is under it.
            trackTap(kind, event)
            return true
        }
        if (panelHidden()) {
            when (panelHold.feed(kind, count, xs, ys, width, height)) {
                HoldEvent.HOLD_STARTED -> {
                    main.removeCallbacks(panelTimer)
                    main.postDelayed(panelTimer, LockGestureMap.PANEL_OPEN_HOLD_MS)
                }
                HoldEvent.HOLD_CANCELLED -> main.removeCallbacks(panelTimer)
                HoldEvent.NONE -> Unit
            }
        } else {
            main.removeCallbacks(panelTimer)
        }
        return false
    }

    fun release() {
        main.removeCallbacks(lockTimer)
        main.removeCallbacks(panelTimer)
    }

    private fun fireLock() {
        lockHold.fired()
        main.removeCallbacks(panelTimer)
        if (chrome.store.locked) {
            chrome.unlock()
        } else {
            cancelViews()
            swallowRest = true
            chrome.lock(LockSource.MANUAL)
        }
    }

    private fun firePanelOpen() {
        panelHold.fired()
        if (chrome.store.locked) return
        cancelViews()
        swallowRest = true
        if (tick()) haptic(HapticFeedbackConstants.CONFIRM)
        openPanel()
    }

    private fun trackTap(kind: TouchKind, event: MotionEvent) {
        when (kind) {
            TouchKind.DOWN -> {
                tapDownX = event.x
                tapDownY = event.y
                tapDownAt = SystemClock.uptimeMillis()
            }
            TouchKind.UP -> {
                val still = kotlin.math.hypot(event.x - tapDownX, event.y - tapDownY) <= tapSlop
                val quick = SystemClock.uptimeMillis() - tapDownAt <= TAP_MS
                if (still && quick && event.pointerCount == 1) closePlayback()
            }
            else -> Unit
        }
    }

    private fun kindOf(event: MotionEvent): TouchKind? = when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> TouchKind.DOWN
        MotionEvent.ACTION_POINTER_DOWN -> TouchKind.POINTER_DOWN
        MotionEvent.ACTION_MOVE -> TouchKind.MOVE
        MotionEvent.ACTION_POINTER_UP -> TouchKind.POINTER_UP
        MotionEvent.ACTION_UP -> TouchKind.UP
        MotionEvent.ACTION_CANCEL -> TouchKind.CANCEL
        else -> null
    }

    companion object {
        private const val MAX_POINTERS = 4
        private const val TAP_MS = 350L

        fun slopFor(configuration: ViewConfiguration): Float = configuration.scaledTouchSlop * 2f
    }
}
