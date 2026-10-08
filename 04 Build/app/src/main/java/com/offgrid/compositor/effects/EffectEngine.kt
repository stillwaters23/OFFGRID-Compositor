package com.offgrid.compositor.effects

import android.graphics.Canvas
import android.os.SystemClock
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Every effect setting at one moment, read once when a setting changes (final build, M4).
 * Effects read this from the drawing threads. It never changes after it is made.
 */
class EffectSnapshot(
    private val numbers: Map<String, Int>,
    private val toggles: Map<String, Boolean>,
    private val choices: Map<String, String>,
    private val texts: Map<String, String>,
) {
    fun on(key: String): Boolean = toggles[key] == true

    /** The typed number, 0 to 100. */
    fun raw(key: String): Int = (numbers[key] ?: 50).coerceIn(0, 100)

    /** The typed number as 0 to 1. */
    fun value(key: String): Float = raw(key) / 100f

    fun choice(key: String): String = choices[key].orEmpty()

    fun text(key: String): String = texts[key].orEmpty()

    /** True when [key]'s choice is [value]. */
    fun chose(key: String, value: String): Boolean = choices[key] == value

    companion object {
        val EMPTY = EffectSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }
}

/** One-shot effects that are fired, like a burst of fireworks or a shatter. */
enum class Fire {
    FIREWORKS,
    RED_SWEEP,
    TIME_FREEZE,
    QUIET,
    SHATTER,
    DEPTH_SHIFT,
    LASER_WRITE,

    /** Feature 37: a one-time test code drawn into the video. Its argument is the code. */
    TEST_CODE,
}

/** A fired effect: what, when (engine seconds), and an optional word, like the laser's text. */
class Fired(val kind: Fire, val at: Float, val arg: String?, val id: Long)

/**
 * Where the face is, from the phone's own face finder, as shares of the video from 0 to 1.
 * [x] and [y] are the middle, [size] is the face's width. It stays on the phone.
 */
class FaceSpot(val x: Float, val y: Float, val size: Float, val at: Float)

/** One ink line drawn with a finger, as points from 0 to 1 across and down the video. */
class InkStroke(
    val points: FloatArray,
    val count: Int,
    /** Ink pinned to the face keeps its place on the face. Null means fixed on the screen. */
    val faceAnchor: FaceSpot?,
    val size: Float,
    val glow: Float,
    val color: Int,
)

/** What every effect gets for one picture. */
class EffectFrame(
    val width: Int,
    val height: Int,
    /** Engine seconds. Every effect moves by this clock, so a frame is the same for the same time. */
    val time: Float,
    val settings: EffectSnapshot,
    val fired: List<Fired>,
    /** The face right now, or null when no face was seen in the last half second. */
    val face: FaceSpot?,
    val ink: List<InkStroke>,
    /** The panel's edge color, for effects that follow the panel's look. */
    val panelColor: Int,
    /** True when the phone asks for less motion. Effects still show, but calmer. */
    val reducedMotion: Boolean,
    /** When the camera mode last changed, in engine seconds, so a mode can fade in. */
    val modeChangedAt: Float = -100f,
    /** The camera mode before the last change. */
    val previousMode: String = FxKeys.MODE_LIVE,
) {
    /** The most recent firing of [kind], or null. */
    fun last(kind: Fire): Fired? = fired.lastOrNull { it.kind == kind }

    /** Seconds since the most recent firing of [kind], or null if it never fired. */
    fun since(kind: Fire): Float? = last(kind)?.let { time - it.at }
}

/**
 * One effect drawn on top of the camera picture, into the video viewers see.
 * It draws with Android's Canvas, on the phone's graphics chip, and never into the panel.
 */
interface OverlayLayer {
    /** True when this effect draws anything in [frame]. */
    fun isActive(frame: EffectFrame): Boolean

    /** Draws this effect for [frame]. The canvas is [EffectFrame.width] by [EffectFrame.height]. */
    fun draw(canvas: Canvas, frame: EffectFrame)

    /** True when this effect covers the whole picture with no see-through parts, like the beach. */
    fun coversAll(frame: EffectFrame): Boolean = false
}

/**
 * Holds what every effect needs: the settings, the fired effects, the face, and the ink.
 * The panel and the camera screen change it on the main thread. The drawing threads read it.
 */
class EffectEngine {
    private val startNanos = SystemClock.elapsedRealtimeNanos()
    private val fired = CopyOnWriteArrayList<Fired>()
    private val strokes = CopyOnWriteArrayList<InkStroke>()
    private var nextId = 1L

    @Volatile
    var settings: EffectSnapshot = EffectSnapshot.EMPTY

    @Volatile
    var face: FaceSpot? = null

    @Volatile
    var panelColor: Int = 0xFF2BF5E6.toInt()

    /** When the camera mode last changed, in engine seconds, so the cloak can shimmer in and out. */
    @Volatile
    var modeChangedAt: Float = -100f
        private set

    /** The camera mode before the last change. */
    @Volatile
    var previousMode: String = FxKeys.MODE_LIVE
        private set

    /** Seconds since the engine started. */
    fun now(): Float = (SystemClock.elapsedRealtimeNanos() - startNanos) / 1_000_000_000f

    fun update(next: EffectSnapshot) {
        val first = settings === EffectSnapshot.EMPTY
        val before = settings.choice(FxKeys.CAMERA_MODE).ifEmpty { FxKeys.MODE_LIVE }
        val after = next.choice(FxKeys.CAMERA_MODE).ifEmpty { FxKeys.MODE_LIVE }
        // The first settings after the app opens never fade, so a saved no-face mode covers
        // the very first picture and the face never shows while it fades in.
        if (!first && before != after) {
            previousMode = before
            modeChangedAt = now()
        }
        settings = next
    }

    fun fire(kind: Fire, arg: String? = null) {
        val at = now()
        synchronized(this) {
            fired += Fired(kind, at, arg, nextId++)
        }
        // Old firings are let go after a while. Nothing lasts this long.
        fired.removeAll { at - it.at > KEEP_SECONDS }
    }

    fun firedNow(): List<Fired> = fired.toList()

    fun addStroke(stroke: InkStroke) {
        strokes += stroke
        while (strokes.size > MAX_STROKES) strokes.removeAt(0)
    }

    fun clearInk() {
        strokes.clear()
    }

    fun ink(): List<InkStroke> = strokes.toList()

    /** The face, if the phone saw one in the last half second. */
    fun freshFace(time: Float): FaceSpot? = face?.takeIf { time - it.at < FACE_FRESH_SECONDS }

    /**
     * A picture's worth of everything effects need. The face is where it shows in the video,
     * after the takeover zoom, so the face diamond stays on the face.
     */
    fun frame(width: Int, height: Int, reducedMotion: Boolean, time: Float = now()): EffectFrame {
        val shown = settings
        return EffectFrame(
            width = width,
            height = height,
            time = time,
            settings = shown,
            fired = fired.toList(),
            face = StageMath.faceInVideo(freshFace(time), shown),
            ink = strokes.toList(),
            panelColor = panelColor,
            reducedMotion = reducedMotion,
            modeChangedAt = modeChangedAt,
            previousMode = previousMode,
        )
    }

    companion object {
        const val KEEP_SECONDS = 120f
        const val MAX_STROKES = 400
        const val FACE_FRESH_SECONDS = 0.5f
    }
}
