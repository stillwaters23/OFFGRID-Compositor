package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.view.Choreographer
import android.view.View

/**
 * One shared clock for the living light: the panel's pulse. Step 2.4 removed the swirl.
 * It only runs while something on screen needs it, and only about 30 times a second.
 * The phone skips the frames in between, which saves battery.
 * When the phone's "Remove animations" setting is on, nothing joins, so nothing moves.
 */
internal object GlowClock : Choreographer.FrameCallback {
    private val watchers = LinkedHashSet<View>()
    private var running = false
    private var startNanos = 0L

    /** Seconds since the clock started. Views read this when they draw. */
    var seconds = 0f
        private set

    /** False when the phone asks for reduced motion. */
    val motionAllowed: Boolean get() = ValueAnimator.areAnimatorsEnabled()

    fun join(view: View) {
        if (!motionAllowed) return
        watchers.add(view)
        if (!running) {
            running = true
            startNanos = System.nanoTime() - (seconds * 1e9f).toLong()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun leave(view: View) {
        watchers.remove(view)
        if (watchers.isEmpty() && running) {
            running = false
            Choreographer.getInstance().removeFrameCallback(this)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        if (!motionAllowed) {
            watchers.toList().forEach { it.invalidate() }
            watchers.clear()
            running = false
            return
        }
        seconds = (frameTimeNanos - startNanos) / 1e9f
        watchers.toList().forEach { it.invalidate() }
        Choreographer.getInstance().postFrameCallbackDelayed(this, TICK_MS)
    }

    /** The wait before the next tick. With the frame it lands on, that is about 30 ticks a second. */
    private const val TICK_MS = 25L
}
