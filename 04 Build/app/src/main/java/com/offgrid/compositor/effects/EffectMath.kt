package com.offgrid.compositor.effects

import android.graphics.Color

/** Small math helpers shared by the effects. */
object EffectMath {
    /** A steady "random" number from 0 to 1 for [index] and [salt]. The same inputs give the same number. */
    fun hash(index: Int, salt: Int = 0): Float {
        var h = index * 374761393 + salt * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0x7FFFFFFF) / 2147483647f
    }

    /** A bright color around the color wheel. [hue] 0 to 1 goes red, yellow, green, blue, violet, red. */
    fun hueColor(hue: Float, saturation: Float = 1f, brightness: Float = 1f, alpha: Int = 255): Int {
        val wrapped = ((hue % 1f) + 1f) % 1f
        return Color.HSVToColor(alpha, floatArrayOf(wrapped * 360f, saturation.coerceIn(0f, 1f), brightness.coerceIn(0f, 1f)))
    }

    fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

    fun clamp01(value: Float): Float = value.coerceIn(0f, 1f)

    /** Smooth start and end, for gliding moves. */
    fun smooth(t: Float): Float {
        val x = clamp01(t)
        return x * x * (3f - 2f * x)
    }

    /** Starts fast, ends slow. */
    fun easeOut(t: Float): Float {
        val x = clamp01(t)
        return 1f - (1f - x) * (1f - x) * (1f - x)
    }

    /** Starts slow, ends fast. */
    fun easeIn(t: Float): Float {
        val x = clamp01(t)
        return x * x * x
    }

    /** How far through a span from [start] lasting [length] seconds [time] is, from 0 to 1. */
    fun progress(time: Float, start: Float, length: Float): Float =
        if (length <= 0f) 1f else clamp01((time - start) / length)
}

/**
 * How long the fired effects last, worked out from their typed numbers (final build, M4).
 * The camera picture and the drawn effects both use these, so they stay in step.
 *
 * Nothing here flashes more than 3 times a second. That limit can't be turned off.
 */
object EffectTimes {
    /** Time freeze: 0 is half a second, 100 is 10 seconds. */
    fun freezeSeconds(s: EffectSnapshot): Float = 0.5f + 9.5f * s.value(FxKeys.FREEZE_LENGTH)

    /** The glitch flicker on the way in and out of a freeze. */
    const val FREEZE_GLITCH_SECONDS = 0.34f

    class Quiet(val fade: Float, val dark: Float, val flicker: Float, val flickers: Int, val back: Float) {
        val total: Float get() = fade + dark + flicker + back
    }

    /** Quiet mode: fade to dark, stay dark, a small light flickers on, then the picture comes back. */
    fun quiet(s: EffectSnapshot): Quiet {
        val flickers = 1 + (s.value(FxKeys.QUIET_FLICKERS) * 5f + 0.5f).toInt()
        return Quiet(
            fade = 0.2f + 1.3f * (1f - s.value(FxKeys.QUIET_FADE)),
            dark = 0.5f + 4.5f * s.value(FxKeys.QUIET_DARK),
            // Each flicker takes at least a third of a second, so never more than 3 a second.
            flicker = flickers * MIN_FLASH_SECONDS,
            flickers = flickers,
            back = 0.4f,
        )
    }

    class Shatter(val crack: Float, val fall: Float, val broken: Float, val back: Float) {
        val total: Float get() = crack + fall + broken + back
    }

    /** Shatter: cracks spread, pieces fall, stay broken, then fly back together. */
    fun shatter(s: EffectSnapshot): Shatter = Shatter(
        crack = 0.6f,
        fall = 0.6f + 2.4f * (1f - s.value(FxKeys.SHATTER_FALL)),
        broken = 0.3f + 4.7f * s.value(FxKeys.SHATTER_BROKEN),
        back = 0.4f + 2.6f * (1f - s.value(FxKeys.SHATTER_BACK)),
    )

    /** Shatter's piece count: 0 is 8 pieces, 100 is 80. */
    fun shatterPieces(s: EffectSnapshot): Int = 8 + (72f * s.value(FxKeys.SHATTER_PIECES)).toInt()

    class Depth(val push: Float, val hold: Float, val snap: Float, val distance: Float) {
        val total: Float get() = push + hold + snap
    }

    /** Depth shift: push out toward the viewer, hold, then snap back. */
    fun depth(s: EffectSnapshot): Depth = Depth(
        push = 0.15f + 0.85f * (1f - s.value(FxKeys.DEPTH_SPEED)),
        hold = 0.1f + 2.9f * s.value(FxKeys.DEPTH_HOLD),
        snap = 0.08f + 0.9f * (1f - s.value(FxKeys.DEPTH_SNAP)),
        distance = 0.1f + 0.6f * s.value(FxKeys.DEPTH_PUSH),
    )

    /** Red sweep: how long it stays. 0 is 2 seconds, 100 is 20. The start, 70, is well past a normal kick animation. */
    fun sweepSeconds(s: EffectSnapshot): Float = 2f + 18f * s.value(FxKeys.SWEEP_LENGTH)

    /** Fireworks: how long a burst lasts. */
    fun fireworksSeconds(s: EffectSnapshot): Float = 0.8f + 3.2f * s.value(FxKeys.FIREWORKS_DURATION)

    /** Laser writing: seconds per letter, and how long the words stay after. */
    fun writeLetterSeconds(s: EffectSnapshot): Float = 0.06f + 0.6f * (1f - s.value(FxKeys.WRITE_SPEED))

    fun writeStaySeconds(s: EffectSnapshot): Float = 1f + 14f * s.value(FxKeys.WRITE_STAY)

    fun writeSeconds(s: EffectSnapshot, text: String): Float =
        text.length * writeLetterSeconds(s) + writeStaySeconds(s) + WRITE_FADE_SECONDS

    const val WRITE_FADE_SECONDS = 1f

    /** Cloak: how long the shimmer takes to cover or uncover the box. 0 is a tenth of a second, 100 is 3 seconds. */
    fun cloakSeconds(s: EffectSnapshot): Float = 0.1f + 2.9f * s.value(FxKeys.CLOAK_FADE)

    /** Status label: how long the word takes to fade in. */
    fun statusSeconds(s: EffectSnapshot): Float = 0.1f + 2.9f * s.value(FxKeys.STATUS_FADE)

    /** Blackout and going back to the live camera fade this fast. */
    const val QUICK_FADE_SECONDS = 0.15f

    /** How long a change from [from] to [to] camera mode takes. Ghost is always instant, like a frozen stream. */
    fun modeChangeSeconds(s: EffectSnapshot, from: String, to: String): Float = when {
        from == FxKeys.MODE_GHOST || to == FxKeys.MODE_GHOST -> 0f
        to == FxKeys.MODE_CLOAK -> cloakSeconds(s)
        to == FxKeys.MODE_STATUS -> statusSeconds(s)
        from == FxKeys.MODE_CLOAK -> cloakSeconds(s)
        from == FxKeys.MODE_STATUS -> statusSeconds(s)
        else -> QUICK_FADE_SECONDS
    }

    /** Echo trail: how far back the copies reach. 0 is a quarter second, 100 is 2.5 seconds. */
    fun echoSeconds(s: EffectSnapshot): Float = 0.25f + 2.25f * s.value(FxKeys.ECHO_LENGTH)

    /** Echo trail: how many copies, 2 to [MAX_ECHO_COPIES]. */
    fun echoCopies(s: EffectSnapshot): Int = 2 + (s.value(FxKeys.ECHO_COPIES) * (MAX_ECHO_COPIES - 2) + 0.5f).toInt()

    const val MAX_ECHO_COPIES = 8

    /** Takeover: how far the big box zooms in. 0 is no zoom, 100 is 2.5 times. */
    fun takeoverZoom(s: EffectSnapshot): Float = 1f + 1.5f * s.value(FxKeys.TAKEOVER_ZOOM)

    /** Takeover: the small box's width as a share of the video's width. 0 is a fifth, 100 is almost half. */
    fun insetShare(s: EffectSnapshot): Float = 0.2f + 0.25f * s.value(FxKeys.TAKEOVER_INSET_SIZE)

    /** Feature 37: a test code stays in the video this long. */
    const val TEST_CODE_SECONDS = 30f

    /** The shortest light flash allowed. 3 flashes a second at most. */
    const val MIN_FLASH_SECONDS = 1f / 3f + 0.01f
}
