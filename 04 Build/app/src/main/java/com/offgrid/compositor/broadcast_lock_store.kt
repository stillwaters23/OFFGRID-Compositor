package com.offgrid.compositor

import android.content.Context

/**
 * Phase 1.3 Broadcast Lock (V2 section 7.3, approvals A1 and A4, rule R1).
 *
 * Bigo captures the whole S25 screen, so every bit of chrome (panel, top bar, HUD, floating
 * overlay) is on air. The lock hides all of it and leaves only the stage. Four sources can
 * hold the lock at once:
 *
 * - AUTO: Android 15 reports that OFFGRID is inside a screen recording or share (VISIBLE).
 *   It clears itself when the callback reports NOT_VISIBLE and never by hand.
 * - MANUAL: the S25 Lock glyph, the two-finger hold in the middle of the stage, or the old
 *   "Clean screen share" button, which this store absorbs.
 * - REMOTE: Phone 2 sent `lock_on`.
 * - ARMED: set from Phone 2 or the top bar before opening Bigo. Persisted, so a resume or a
 *   cold start draws the stage only, whatever the callback says and whenever it says it.
 *
 * `locked = recordingVisible || manualLocked || remoteLocked || armed || resumeHold`.
 *
 * Unlock (A4): refused while the callback reports VISIBLE. Otherwise it clears MANUAL, REMOTE,
 * ARMED, and the resume hold in one go. There is no "allow unlock during share" option.
 *
 * Leaving the screen: Android reports NOT_VISIBLE as soon as OFFGRID stops being drawn, which is
 * while Bigo is coming to the front with the share still running, before onStop. That report is
 * not the share ending. [reportRecording] holds a NOT_VISIBLE that lands while the activity is
 * not resumed and applies it on [onResume]; [onStart] replaces it with the fresh first value.
 *
 * Resume hold: [onStop] remembers whether the stage was locked when the app went to the
 * background. [onStart] turns that into a hold that keeps the chrome away until the activity
 * has drawn and the callback has had its chance to flip to VISIBLE. The activity releases the
 * hold with [releaseResumeHold] once that window has passed.
 *
 * Pure Kotlin apart from the small [LockPersistence] seam, so the state machine is unit-tested.
 */
internal enum class LockSource { AUTO, MANUAL, REMOTE, ARMED }

internal enum class UnlockResult {
    /** MANUAL, REMOTE, ARMED, and the resume hold were cleared. */
    UNLOCKED,

    /** The callback reports VISIBLE: no unlocking during an active share (A4). */
    REFUSED_SHARE_VISIBLE,

    /** Nothing was held by hand. AUTO is not cleared by an unlock. */
    NOTHING_TO_UNLOCK,
}

internal data class LockState(
    val recordingVisible: Boolean = false,
    val manualLocked: Boolean = false,
    val remoteLocked: Boolean = false,
    val armed: Boolean = false,
    val resumeHold: Boolean = false,
) {
    val locked: Boolean get() = recordingVisible || manualLocked || remoteLocked || armed || resumeHold

    /** The sources holding the lock right now, for the status line and Phone 2. */
    val sources: Set<LockSource>
        get() = buildSet {
            if (recordingVisible) add(LockSource.AUTO)
            if (manualLocked) add(LockSource.MANUAL)
            if (remoteLocked) add(LockSource.REMOTE)
            if (armed) add(LockSource.ARMED)
        }

    /** One word for Phone 2 and the status line. */
    val word: String
        get() = when {
            recordingVisible -> "live"
            manualLocked || remoteLocked || resumeHold -> "locked"
            armed -> "armed"
            else -> "unlocked"
        }
}

/** Where the lock keeps its four saved flags. The app uses SharedPreferences; tests use a map. */
internal interface LockPersistence {
    fun read(key: String, default: Boolean): Boolean
    fun write(key: String, value: Boolean)
}

internal class MemoryLockPersistence : LockPersistence {
    val values = HashMap<String, Boolean>()
    override fun read(key: String, default: Boolean): Boolean = values[key] ?: default
    override fun write(key: String, value: Boolean) {
        values[key] = value
    }
}

internal class PrefsLockPersistence(context: Context) : LockPersistence {
    private val prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    override fun read(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun write(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    private companion object {
        const val FILE_NAME = "broadcast_lock"
    }
}

internal class BroadcastLockStore(private val persistence: LockPersistence) {

    var state: LockState = LockState(
        manualLocked = persistence.read(KEY_MANUAL, false),
        remoteLocked = persistence.read(KEY_REMOTE, false),
        armed = persistence.read(KEY_ARMED, false),
        resumeHold = persistence.read(KEY_WAS_LOCKED, false),
    )
        private set

    /** Called on every change with the new state, on the thread that made the change. */
    var onChanged: ((LockState) -> Unit)? = null

    /** More listeners: the activity's chrome, the overlay, and Phone 2 each follow the lock. */
    private val listeners = ArrayList<(LockState) -> Unit>()

    fun addListener(listener: (LockState) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (LockState) -> Unit) {
        listeners -= listener
    }

    val locked: Boolean get() = state.locked

    /** Between [onResume] and [onPause]. */
    private var foreground = false

    /** A NOT_VISIBLE that came while the activity was not in front. Applied on [onResume]. */
    private var clearPending = false

    /** Locks from [source]. AUTO goes through [setRecordingVisible]; the others are sticky. */
    fun lock(source: LockSource) {
        when (source) {
            LockSource.AUTO -> setRecordingVisible(true)
            LockSource.MANUAL -> change(state.copy(manualLocked = true))
            LockSource.REMOTE -> change(state.copy(remoteLocked = true))
            LockSource.ARMED -> change(state.copy(armed = true))
        }
    }

    fun arm() = lock(LockSource.ARMED)

    /** The Android 15 callback, or its synchronous first value. AUTO follows it exactly. */
    fun setRecordingVisible(visible: Boolean) {
        if (visible == state.recordingVisible) return
        change(state.copy(recordingVisible = visible))
    }

    /**
     * The live callback, after the activity's settle delay. VISIBLE locks at once. NOT_VISIBLE
     * clears AUTO only while the activity is in front; otherwise it waits for [onResume].
     */
    fun reportRecording(visible: Boolean) {
        if (visible) {
            clearPending = false
            setRecordingVisible(true)
            return
        }
        if (!foreground) {
            if (state.recordingVisible) clearPending = true
            return
        }
        clearPending = false
        setRecordingVisible(false)
    }

    fun onResume() {
        foreground = true
        if (clearPending) {
            clearPending = false
            setRecordingVisible(false)
        }
    }

    fun onPause() {
        foreground = false
    }

    /**
     * Clears every lock set by hand, by Phone 2, or by arming. Refused while the callback
     * reports VISIBLE (A4). AUTO is never cleared here.
     */
    fun unlock(): UnlockResult {
        val now = state
        if (now.recordingVisible) return UnlockResult.REFUSED_SHARE_VISIBLE
        if (!now.manualLocked && !now.remoteLocked && !now.armed && !now.resumeHold) {
            return UnlockResult.NOTHING_TO_UNLOCK
        }
        change(now.copy(manualLocked = false, remoteLocked = false, armed = false, resumeHold = false))
        return UnlockResult.UNLOCKED
    }

    /** The activity is going to the background: remember whether the stage was locked. */
    fun onStop() {
        foreground = false
        persistence.write(KEY_WAS_LOCKED, state.locked)
    }

    /**
     * The activity is coming to the front. [initialVisible] is the synchronous return value of
     * `addScreenRecordingCallback`, or null on a phone without the callback (API 33 and 34).
     * A lock that was held when the app left stays as a hold until [releaseResumeHold].
     */
    fun onStart(initialVisible: Boolean?) {
        val wasLocked = persistence.read(KEY_WAS_LOCKED, false)
        clearPending = false
        val next = state.copy(
            recordingVisible = initialVisible ?: false,
            resumeHold = state.resumeHold || wasLocked,
        )
        if (next != state) change(next) else notify(state)
    }

    /** The resume window passed without a VISIBLE report, so the hold alone no longer locks. */
    fun releaseResumeHold() {
        if (!state.resumeHold) return
        change(state.copy(resumeHold = false))
    }

    private fun change(next: LockState) {
        if (next == state) return
        state = next
        persistence.write(KEY_MANUAL, next.manualLocked)
        persistence.write(KEY_REMOTE, next.remoteLocked)
        persistence.write(KEY_ARMED, next.armed)
        notify(next)
    }

    private fun notify(now: LockState) {
        onChanged?.invoke(now)
        for (listener in listeners.toList()) listener(now)
    }

    companion object {
        const val KEY_MANUAL = "manual_locked"
        const val KEY_REMOTE = "remote_locked"
        const val KEY_ARMED = "armed"
        const val KEY_WAS_LOCKED = "was_locked_when_backgrounded"

        /** Phone 2 `fire` values that drive the lock (RemoteMessage allowlist). */
        const val FIRE_LOCK_ON = "lock_on"
        const val FIRE_LOCK_OFF = "lock_off"
        const val FIRE_ARM = "arm"
    }
}
