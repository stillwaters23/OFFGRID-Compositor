package com.offgrid.compositor.terminal

/**
 * The parts of the screen the panel's words and buttons stay out of (final build, milestone 2).
 *
 * The glass itself fills the screen edge to edge at Width 100 and Height 100, under the
 * status bar, the camera hole, and the gesture bar. Only what is inside it moves in, by
 * these amounts, wherever the glass reaches that edge. All values are pixels.
 *
 * [top], [bottom], [left], and [right] are the bars and the camera hole, measured the same
 * whether the bars show or hide, so pulling down the status bar never moves the panel.
 * The gesture values are where a swipe means Back or Home. Dragging the resize edge
 * there would fight the phone, so the corner stays out. [cornerRadius] is how round the
 * screen's own corners are.
 */
data class ScreenEdges(
    val top: Int = 0,
    val bottom: Int = 0,
    val left: Int = 0,
    val right: Int = 0,
    val gestureLeft: Int = 0,
    val gestureRight: Int = 0,
    val gestureBottom: Int = 0,
    val cornerRadius: Int = 0,
)
