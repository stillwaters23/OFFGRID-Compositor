package com.offgrid.compositor.effects

/**
 * Where things land in the video when the camera stage moves the picture (final build, M4).
 * The GPU stage and the drawn effects both use this, so the face diamond stays on the face.
 * Shares go from 0 to 1, left to right and top to bottom.
 */
object StageMath {
    /** Where takeover looks when no face is seen: the middle, a little high, where a face usually is. */
    const val REST_X = 0.5f
    const val REST_Y = 0.42f

    /** The part of the camera picture the big box shows: its middle and how far it zooms in. */
    class Window {
        var x = 0.5f
        var y = 0.5f
        var zoom = 1f
    }

    /** Fills [into] with the takeover window for [face] (where the camera saw it) at [zoom]. */
    fun zoomWindow(face: FaceSpot?, zoom: Float, into: Window): Window {
        val z = zoom.coerceAtLeast(1f)
        val half = 0.5f / z
        into.zoom = z
        into.x = (face?.x ?: REST_X).coerceIn(half, 1f - half)
        into.y = (face?.y ?: REST_Y).coerceIn(half, 1f - half)
        return into
    }

    /** True when the big box shows the zoomed-in picture. Flip-flop puts the zoom in the small box. */
    fun bigIsZoomed(s: EffectSnapshot): Boolean = s.on(FxKeys.TAKEOVER_ON) && !s.on(FxKeys.TAKEOVER_FLIP)

    /** Where the camera's [face] shows in the finished video. Null stays null. */
    fun faceInVideo(face: FaceSpot?, s: EffectSnapshot): FaceSpot? {
        if (face == null || !bigIsZoomed(s)) return face
        val window = zoomWindow(face, EffectTimes.takeoverZoom(s), Window())
        return FaceSpot(
            x = (face.x - window.x) * window.zoom + 0.5f,
            y = (face.y - window.y) * window.zoom + 0.5f,
            size = face.size * window.zoom,
            at = face.at,
        )
    }
}
