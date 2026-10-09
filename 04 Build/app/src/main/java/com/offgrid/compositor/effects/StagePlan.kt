package com.offgrid.compositor.effects

/**
 * What the camera stage does to one picture, worked out from the time and the settings
 * (final build, M4). It is plain math with no drawing, so tests can check it, and the GPU
 * stage only follows it.
 *
 * One plan is reused for every picture, so making a plan makes no garbage.
 */
class StagePlan {
    /** Mirror plus scan flips the camera left and right. */
    var mirror = false
        private set

    /** The id of the freeze or quiet firing that holds a still picture, or 0 for live. */
    var holdId = 0L
        private set

    /** Time freeze glitch: 0 is none, 1 is the strongest. Never more than one flicker at a time. */
    var glitch = 0f
        private set

    /** A steady number that changes the glitch's look from one flicker to the next. */
    var glitchSeed = 0f
        private set

    /** Quiet mode darkens the picture: 1 is normal, near 0 is dark. */
    var brightness = 1f
        private set

    /** The camera mode now and the one before, and how far the change has gone, 0 to 1. */
    var mode = FxKeys.MODE_LIVE
        private set
    var previousMode = FxKeys.MODE_LIVE
        private set
    var modeMix = 1f
        private set

    /** Cloak: how dark and empty the void is, and how much it ripples while changing, 0 to 1. */
    var cloakStrength = 0.5f
        private set
    var cloakShimmer = 0.5f
        private set

    /** Ghost mode: when it started, to know which still picture to keep. */
    var ghostSince = 0f
        private set

    /** Ghost mode shows the last picture viewers saw (true) or a blank box (false). */
    var ghostFrozen = true
        private set

    var echoOn = false
        private set
    var echoCopies = 0
        private set
    var echoSeconds = 1f
        private set

    /** How strong the echo copies are, 0 to 1. */
    var echoStrength = 0f
        private set

    var takeoverOn = false
        private set
    var takeoverFlip = false
        private set
    var insetOn = false
        private set
    var insetShare = 0.3f
        private set
    val window = StageMath.Window()

    /** Depth shift: 0 is flat, 1 is pushed all the way out. [depthFrame] is how much the frame shows. */
    var depth = 0f
        private set
    var depthFrame = 0f
        private set
    var depthDistance = 0f
        private set

    /** Shatter: the id of the firing, 0 when not shattered. */
    var shatterId = 0L
        private set

    /** Seconds since the shatter fired, and its timing. */
    var shatterTime = 0f
        private set
    var shatterTimes: EffectTimes.Shatter? = null
        private set
    var shatterPieces = 0
        private set
    var shatterBehind = FxKeys.BEHIND_BLACK
        private set
    var impactX = 0.5f
        private set
    var impactY = 0.45f
        private set

    /** Whether the cloak void or beach are needed behind something this picture. */
    val needsVoid: Boolean
        get() = mode == FxKeys.MODE_CLOAK || (modeMix < 1f && previousMode == FxKeys.MODE_CLOAK) ||
            (shatterId != 0L && shatterBehind == FxKeys.BEHIND_VOID)

    /** True when the camera picture goes out exactly as it came in, with nothing done to it. */
    val plain: Boolean
        get() = !mirror && holdId == 0L && brightness >= 1f && mode == FxKeys.MODE_LIVE && modeMix >= 1f &&
            !echoOn && !takeoverOn && depth <= 0f && depthFrame <= 0f && shatterId == 0L

    /**
     * Works out the plan for the picture at [time]. [face] is where the camera sees the face
     * (or null), [modeChangedAt] and [previousMode] come from the effect engine.
     */
    fun update(
        time: Float,
        settings: EffectSnapshot,
        fired: List<Fired>,
        face: FaceSpot?,
        modeChangedAt: Float,
        previousMode: String,
    ): StagePlan {
        mirror = settings.on(FxKeys.MIRROR_ON)
        planMode(time, settings, modeChangedAt, previousMode)
        planHold(time, settings, fired)
        planEcho(settings)
        planTakeover(settings, face)
        planDepth(time, settings, fired)
        planShatter(time, settings, fired)
        return this
    }

    private fun planMode(time: Float, s: EffectSnapshot, changedAt: Float, before: String) {
        mode = s.choice(FxKeys.CAMERA_MODE).ifEmpty { FxKeys.MODE_LIVE }
        previousMode = before
        val length = EffectTimes.modeChangeSeconds(s, before, mode)
        modeMix = EffectMath.progress(time, changedAt, length)
        ghostSince = if (mode == FxKeys.MODE_GHOST) changedAt else 0f
        ghostFrozen = !s.chose(FxKeys.GHOST_STYLE, FxKeys.GHOST_BLANK)
        cloakStrength = s.value(FxKeys.CLOAK_STRENGTH)
        cloakShimmer = s.value(FxKeys.CLOAK_SHIMMER)
    }

    private fun planHold(time: Float, s: EffectSnapshot, fired: List<Fired>) {
        holdId = 0L
        glitch = 0f
        brightness = 1f
        // The newest of a running freeze or quiet holds the picture.
        var newest: Fired? = null
        for (event in fired) {
            val running = when (event.kind) {
                Fire.TIME_FREEZE -> time - event.at < EffectTimes.freezeSeconds(s)
                Fire.QUIET -> time - event.at < EffectTimes.quiet(s).total
                else -> false
            }
            if (running && time >= event.at && (newest == null || event.at >= newest.at)) newest = event
        }
        val event = newest ?: return
        val since = time - event.at
        if (event.kind == Fire.TIME_FREEZE) {
            holdId = event.id
            val length = EffectTimes.freezeSeconds(s)
            val window = EffectTimes.FREEZE_GLITCH_SECONDS
            val strength = s.value(FxKeys.FREEZE_GLITCH)
            // One flicker on the way in (only for freezes long enough to keep flickers apart)
            // and one on the way out, so never more than 3 flickers a second.
            val inGlitch = length >= FREEZE_IN_GLITCH_MIN && since < window
            val outGlitch = since > length - window
            if (strength > 0f && (inGlitch || outGlitch)) {
                val local = if (inGlitch) since / window else (since - (length - window)) / window
                glitch = strength * (1f - kotlin.math.abs(local * 2f - 1f))
                glitchSeed = (event.id % 97).toFloat() + if (inGlitch) 0f else 0.5f
            }
        } else {
            val quiet = EffectTimes.quiet(s)
            val stillUntil = quiet.fade + quiet.dark + quiet.flicker
            if (since < stillUntil) {
                holdId = event.id
                brightness = QUIET_DARK + (1f - QUIET_DARK) * (1f - EffectMath.smooth(since / quiet.fade))
            } else {
                // The light came back on: the live picture brightens back up.
                brightness = QUIET_DARK + (1f - QUIET_DARK) * EffectMath.smooth((since - stillUntil) / quiet.back)
            }
        }
    }

    private fun planEcho(s: EffectSnapshot) {
        echoOn = s.on(FxKeys.ECHO_ON)
        echoCopies = if (echoOn) EffectTimes.echoCopies(s) else 0
        echoSeconds = EffectTimes.echoSeconds(s)
        echoStrength = 0.2f + 0.8f * s.value(FxKeys.ECHO_FADE)
    }

    private fun planTakeover(s: EffectSnapshot, face: FaceSpot?) {
        takeoverOn = s.on(FxKeys.TAKEOVER_ON)
        takeoverFlip = s.on(FxKeys.TAKEOVER_FLIP)
        insetOn = s.on(FxKeys.TAKEOVER_INSET)
        insetShare = EffectTimes.insetShare(s)
        StageMath.zoomWindow(face, EffectTimes.takeoverZoom(s), window)
    }

    private fun planDepth(time: Float, s: EffectSnapshot, fired: List<Fired>) {
        depth = 0f
        depthFrame = 0f
        val event = fired.lastOrNull { it.kind == Fire.DEPTH_SHIFT } ?: return
        val times = EffectTimes.depth(s)
        val since = time - event.at
        if (since < 0f || since >= times.total) return
        depthDistance = times.distance
        // The picture settles into a frame, pushes out past it, holds, then snaps back.
        val settle = times.push * SETTLE_SHARE
        when {
            since < settle -> {
                depthFrame = EffectMath.smooth(since / settle)
            }
            since < times.push -> {
                depthFrame = 1f
                depth = EffectMath.easeOut((since - settle) / (times.push - settle))
            }
            since < times.push + times.hold -> {
                depthFrame = 1f
                depth = 1f
            }
            else -> {
                val back = (since - times.push - times.hold) / times.snap
                depth = 1f - EffectMath.easeIn(back * 1.6f)
                depthFrame = 1f - EffectMath.smooth((back - 0.55f) / 0.45f)
            }
        }
    }

    private fun planShatter(time: Float, s: EffectSnapshot, fired: List<Fired>) {
        shatterId = 0L
        val event = fired.lastOrNull { it.kind == Fire.SHATTER } ?: return
        val times = EffectTimes.shatter(s)
        val since = time - event.at
        if (since < 0f || since >= times.total) return
        shatterId = event.id
        shatterTime = since
        shatterTimes = times
        shatterPieces = EffectTimes.shatterPieces(s)
        shatterBehind = s.choice(FxKeys.SHATTER_BEHIND).ifEmpty { FxKeys.BEHIND_BLACK }
        // A tapped spot comes as "x,y" in shares. Anything else breaks from the middle.
        val spot = event.arg?.split(',')
        val x = spot?.getOrNull(0)?.toFloatOrNull()
        val y = spot?.getOrNull(1)?.toFloatOrNull()
        impactX = (x ?: 0.5f).coerceIn(0.05f, 0.95f)
        impactY = (y ?: 0.45f).coerceIn(0.05f, 0.95f)
    }

    companion object {
        /** Quiet mode's darkest. Not pure black, so the box still looks switched on. */
        const val QUIET_DARK = 0.04f

        /** Freezes shorter than this only glitch on the way out. */
        const val FREEZE_IN_GLITCH_MIN = 1f

        /** Depth shift: the share of the push spent settling into the frame. */
        const val SETTLE_SHARE = 0.3f
    }
}
