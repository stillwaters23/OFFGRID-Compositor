package com.offgrid.compositor

import android.animation.ValueAnimator
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import java.util.function.Consumer

/**
 * Phase 1.3: what the Broadcast Lock does to the camera screen (V2 section 7.3).
 *
 * - Registers `WindowManager.addScreenRecordingCallback` in onStart and removes it in onStop.
 *   The synchronous first value feeds [BroadcastLockStore.onStart], so a cold start or a resume
 *   inside a share locks before the first frame.
 * - The resume gate: onStop hides every bit of chrome. On the next onStart it stays hidden until
 *   the stage has drawn its first frame plus a short grace, or a fallback timer passes when the
 *   camera never starts (denied, or Camera off). Only then, and only unlocked, does the panel
 *   fade in over [FADE_MS]. A VISIBLE report at any moment hides it at once, with no fade.
 * - Locked means: the panel is GONE (its menu and keyboard shut with it), the saved video stops
 *   playing, and the stage draws alone. The overlay and Phone 2 follow through [LiveIntegration].
 *
 * Hard limits kept here: no secure-window flag on this window (CI greps for it), no WAKE_LOCK,
 * no edge gestures.
 */
internal class BroadcastLockChrome(
    private val activity: MainActivity,
    val store: BroadcastLockStore,
) {
    private val main = Handler(Looper.getMainLooper())
    private var callback: Consumer<Int>? = null
    private var gateOpen = false
    private var frameSeen = false
    private var chromeShown = false
    private val revealTask = Runnable { openGate() }
    private val fallbackTask = Runnable { openGate() }
    private val notVisibleTask = Runnable { store.reportRecording(false) }
    private val listener: (LockState) -> Unit = { state -> apply(state) }

    /** True while a share or recording is reported VISIBLE. For the gestures' refusal buzz. */
    val shareVisible: Boolean get() = store.state.recordingVisible

    init {
        store.addListener(listener)
    }

    /** onCreate: nothing shows until the gate opens. */
    fun onCreate() {
        hideChrome()
        activity.terminalPanel.setLockWord(store.state.word)
    }

    fun onStart() {
        gateOpen = false
        frameSeen = false
        hideChrome()
        val initial = register()
        store.onStart(initial)
        main.removeCallbacks(fallbackTask)
        main.postDelayed(fallbackTask, FALLBACK_MS)
    }

    fun onResume() = store.onResume()

    fun onPause() = store.onPause()

    fun onStop() {
        main.removeCallbacks(revealTask)
        main.removeCallbacks(fallbackTask)
        main.removeCallbacks(notVisibleTask)
        unregister()
        store.onStop()
        gateOpen = false
        hideChrome()
    }

    fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        unregister()
        store.removeListener(listener)
    }

    /** The stage drew its first frame after a bind. The chrome may follow after a short grace. */
    fun onFirstStageFrame() {
        if (frameSeen || gateOpen) return
        frameSeen = true
        main.removeCallbacks(fallbackTask)
        main.removeCallbacks(revealTask)
        main.postDelayed(revealTask, GRACE_MS)
    }

    /** The two-finger hold, the Lock glyph, Phone 2, and the folder buttons all end up here. */
    fun lock(source: LockSource) {
        store.lock(source)
        buzz(HapticFeedbackConstants.CONFIRM)
    }

    fun toggleArmed() {
        if (store.state.armed) {
            if (unlock() == UnlockResult.UNLOCKED) activity.sayStatus("The lock is disarmed.")
        } else {
            store.arm()
            buzz(HapticFeedbackConstants.CONFIRM)
        }
    }

    /** A4: refused while a share is live. The refusal is a buzz and a Phone 2 line, never a view. */
    fun unlock(): UnlockResult {
        val result = store.unlock()
        when (result) {
            UnlockResult.UNLOCKED -> buzz(HapticFeedbackConstants.CONFIRM)
            UnlockResult.REFUSED_SHARE_VISIBLE -> {
                buzz(HapticFeedbackConstants.REJECT)
                activity.sayStatus("A screen share is live. The lock stays on until it ends.")
            }
            UnlockResult.NOTHING_TO_UNLOCK -> Unit
        }
        return result
    }

    /** The lock and panel-open zones, so the system's own gestures leave them alone where it can. */
    fun applyGestureExclusion(root: View, panelOpenInsetPx: Int) {
        if (root.width == 0 || root.height == 0) return
        val center = LockGestureMap.centerZone(root.width, root.height)
        val corner = LockGestureMap.panelOpenZone(root.width, root.height, panelOpenInsetPx)
        root.systemGestureExclusionRects = listOf(
            Rect(center[0], center[1], center[2], center[3]),
            Rect(corner[0], corner[1], corner[2], corner[3]),
        )
    }

    private fun openGate() {
        main.removeCallbacks(revealTask)
        main.removeCallbacks(fallbackTask)
        gateOpen = true
        store.releaseResumeHold()
        apply(store.state)
    }

    private fun apply(state: LockState) {
        activity.terminalPanel.setLockWord(state.word)
        if (state.locked || !gateOpen) hideChrome() else showChrome()
        activity.onLockApplied(state)
    }

    private fun hideChrome() {
        val panel = activity.terminalPanel
        panel.animate().cancel()
        panel.alpha = 1f
        panel.setHiddenForLock(true)
        chromeShown = false
    }

    private fun showChrome() {
        if (chromeShown) return
        chromeShown = true
        val panel = activity.terminalPanel
        panel.setHiddenForLock(false)
        if (ValueAnimator.areAnimatorsEnabled()) {
            panel.alpha = 0f
            panel.animate().alpha(1f).setDuration(FADE_MS).start()
        } else {
            panel.alpha = 1f
        }
    }

    /**
     * Android 15 (API 35) reports whether this window is inside a screen recording or share.
     * The permission is normal level, so there is no prompt. Older phones get null: no AUTO lock,
     * the manual, remote, and armed locks still work.
     */
    private fun register(): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null
        unregister()
        val manager = activity.windowManager
        // The platform posts each report to the executor, so one queued before
        // removeScreenRecordingCallback can still run after it. Only the live consumer counts.
        val consumer = object : Consumer<Int> {
            override fun accept(state: Int) {
                if (callback !== this) return
                onRecordingState(state == WindowManager.SCREEN_RECORDING_STATE_VISIBLE)
            }
        }
        return try {
            val initial = manager.addScreenRecordingCallback(ContextCompat.getMainExecutor(activity), consumer)
            callback = consumer
            initial == WindowManager.SCREEN_RECORDING_STATE_VISIBLE
        } catch (error: SecurityException) {
            Log.w(TAG, "The screen recording callback was refused. The auto lock is off.", error)
            null
        }
    }

    /**
     * VISIBLE hides the chrome at once. NOT_VISIBLE waits [NOT_VISIBLE_SETTLE_MS]: it also fires
     * when OFFGRID leaves the screen with the share still running, and onPause lands in that time.
     */
    private fun onRecordingState(visible: Boolean) {
        main.removeCallbacks(notVisibleTask)
        if (visible) store.reportRecording(true) else main.postDelayed(notVisibleTask, NOT_VISIBLE_SETTLE_MS)
    }

    private fun unregister() {
        val consumer = callback ?: return
        callback = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            runCatching { activity.windowManager.removeScreenRecordingCallback(consumer) }
        }
    }

    private fun buzz(kind: Int) {
        if (!activity.terminalPanel.look.tick) return
        activity.window.decorView.performHapticFeedback(kind)
    }

    companion object {
        private const val TAG = "OffgridLock"

        /** The chrome fades in over this long once the gate opens unlocked. */
        const val FADE_MS = 120L

        /** After the first stage frame, the callback gets this long to say VISIBLE before the chrome shows. */
        const val GRACE_MS = 250L

        /** With no camera frame at all (denied, or Camera off), the chrome shows after this long. */
        const val FALLBACK_MS = 1_500L

        /** How long a NOT_VISIBLE report waits before it may clear the auto lock. */
        const val NOT_VISIBLE_SETTLE_MS = 500L
    }
}
