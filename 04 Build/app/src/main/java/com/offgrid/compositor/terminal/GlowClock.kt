package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.os.SystemClock
import android.view.Choreographer
import android.view.View

/**
 * One shared clock for the living light: the panel's pulse. Step 2.4 removed the swirl.
 * It only runs while something on screen needs it, only about 30 times a second, and
 * only while the panel was just touched. After [IDLE_MS] with no touch it stops, so
 * the main thread can go idle (Espresso, input focus, no perpetual DrawFrame).
 * When the phone's "Remove animations" setting is on, nothing joins, so nothing moves.
 */
internal object GlowClock : Choreographer.FrameCallback {
    private val watchers = LinkedHashSet<View>()
    private var running = false
    private var startNanos = 0L
    private var idleAt = 0L

    /** Seconds since the clock started. Views read this when they draw. */
    var seconds = 0f
        private set

    /** True only while a pulse is actually posting frames. */
    var isLive = false
        private set

    /** False when the phone asks for reduced motion. */
    val motionAllowed: Boolean get() = ValueAnimator.areAnimatorsEnabled()

    fun join(view: View) {
        if (!motionAllowed) return
        watchers.add(view)
    }

    fun leave(view: View) {
        watchers.remove(view)
        if (watchers.isEmpty()) stop(idle = true)
    }

    /** A finger or a look change. Restarts the pulse for a short while, then it rests. */
    fun nudge() {
        if (!motionAllowed || watchers.isEmpty()) return
        idleAt = SystemClock.uptimeMillis() + IDLE_MS
        if (!running) {
            running = true
            isLive = true
            startNanos = System.nanoTime() - (seconds * 1e9f).toLong()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        if (!motionAllowed || watchers.isEmpty() || SystemClock.uptimeMillis() >= idleAt) {
            stop(idle = true)
            return
        }
        seconds = (frameTimeNanos - startNanos) / 1e9f
        watchers.toList().forEach { it.invalidate() }
        Choreographer.getInstance().postFrameCallbackDelayed(this, TICK_MS)
    }

    private fun stop(idle: Boolean) {
        running = false
        isLive = false
        if (idle) idleAt = 0L
        Choreographer.getInstance().removeFrameCallback(this)
        watchers.toList().forEach { watcher ->
            watcher.post {
                (watcher as? MotionHost)?.onMotionIdle()
                watcher.invalidate()
            }
        }
    }

    /** The wait before the next tick. With the frame it lands on, that is about 30 ticks a second. */
    private const val TICK_MS = 25L

    /** After this, the pulse and the rings rest so the activity can go idle. */
    const val IDLE_MS = 2_200L
}

/** A view that owns glow motion and must park its rings when [GlowClock] rests. */
internal interface MotionHost {
    fun onMotionIdle()
}
