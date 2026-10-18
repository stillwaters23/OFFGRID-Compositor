package com.offgrid.compositor.terminal

/**
 * WCAG contrast lock for panel colors. A color that would hide text or controls
 * cannot be applied. The typewriter says why it is locked.
 */
internal object ContrastGuard {
    const val MIN_RATIO = NumberColorGuard.MIN_CONTRAST

    fun ratio(foreground: Int, background: Int): Double =
        NumberColorGuard.contrastRatio(foreground, background)

    fun readable(foreground: Int, background: Int): Boolean = ratio(foreground, background) >= MIN_RATIO

    fun lockReason(part: ColorPart, color: Int, skin: Int, text: Int, status: Int): String? {
        return when (part) {
            ColorPart.SKIN -> skinReason(color, text, status)
            ColorPart.TEXT -> textReason(color, skin, "typewriter text")
            ColorPart.STATUS -> textReason(color, skin, "status text")
            ColorPart.NUMBER_DARK, ColorPart.NUMBER_LIGHT ->
                if (readable(color, skin)) null else textReason(color, skin, "numbers")
            ColorPart.FOLDER ->
                if (readable(color, skin)) null else textReason(color, skin, "folder icons")
            ColorPart.EDGE ->
                if (readable(color, skin)) null else textReason(color, skin, "edges")
            ColorPart.CURSOR, ColorPart.OUTER_GLOW -> null
        }
    }

    fun accepts(part: ColorPart, color: Int, skin: Int, text: Int, status: Int): Boolean =
        lockReason(part, color, skin, text, status) == null

    private fun skinReason(skin: Int, text: Int, status: Int): String? {
        val opposite = if (NumberColorGuard.luminance(text) < 0.35) {
            0xFFFFFFFF.toInt()
        } else {
            0xFF000000.toInt()
        }
        val checks = listOf(
            text to "typewriter text",
            status to "status text",
            opposite to "typewriter text",
        )
        for ((ink, label) in checks) {
            if (!readable(ink, skin)) {
                return "Locked: ${nameOf(ink)} $label on a ${nameOf(skin)} background can't be read."
            }
        }
        return null
    }

    private fun textReason(color: Int, skin: Int, label: String): String? {
        if (readable(color, skin)) return null
        return "Locked: ${nameOf(color)} $label on a ${nameOf(skin)} background can't be read."
    }

    private fun nameOf(color: Int): String {
        val rgb = color and 0x00FFFFFF
        return when (rgb) {
            0x000000 -> "black"
            0xFFFFFF -> "white"
            else -> if (NumberColorGuard.luminance(color) < 0.35) "dark" else "light"
        }
    }
}
