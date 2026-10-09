package com.offgrid.compositor.terminal

/** Retired built-in looks stay here for reference and migration; only System is offered in the UI. */
internal enum class ArchivedTheme(val key: String, val title: String, val blurb: String) {
    OLED("oled", "OLED Bioluminescent", "True black glass with phone-aware text and bioluminescent edges."),
    TITANIUM("titanium_sapphire", "Titanium Sapphire", "Titanium frame, sapphire glass, and phone-aware text."),
    TERMUX("termux", "Termux Black", "Plain black. Green text. No glow."),
    HOLO("holo", "Holo Classic", "The Step 2 look. Cyan and gold lines."),
    UV_BLACK_LIGHT("uv_black_light", "UV Black Light", "Black glass with ultraviolet neon colors.");
}

internal object ArchivedThemes {
    private fun phone(role: SystemRole) = PartSpec(system = true, role = role, custom = 0xFF000000.toInt())
    private fun fixed(color: Int, role: SystemRole) = PartSpec(system = false, role = role, custom = color)

    private val roles = mapOf(
        ColorPart.SKIN to SystemRole.NEUTRAL1_1000,
        ColorPart.EDGE to SystemRole.ACCENT1_200,
        ColorPart.OUTER_GLOW to SystemRole.ACCENT1_400,
        ColorPart.TEXT to SystemRole.ACCENT1_100,
        ColorPart.CURSOR to SystemRole.ACCENT1_300,
        ColorPart.FOLDER to SystemRole.ACCENT1_300,
        ColorPart.STATUS to SystemRole.ACCENT1_200,
    )

    private fun role(part: ColorPart) = roles.getValue(part)
    private val laserParts = setOf(ColorPart.TEXT, ColorPart.STATUS)

    private fun spec(
        parts: Map<ColorPart, PartSpec>,
        opacity: Int,
        corners: Int,
        glow: Int,
        scanLine: Boolean,
        ring: Boolean,
        unfold: Boolean,
        edgeGlow: Boolean,
        outerGlow: Boolean,
        pulse: Boolean,
        titanium: Boolean = false,
        sapphire: Boolean = false,
        touchGlow: Boolean = true,
        typingGlow: Boolean = true,
        pulseSpeed: Int = 50,
        glowOn: Boolean = true,
    ) = ThemeSpec(
        parts = parts,
        numbers = mapOf(
            LookKeys.OPACITY to opacity,
            LookKeys.CORNERS to corners,
            LookKeys.GLOW to glow,
            LookKeys.PULSE_SPEED to pulseSpeed,
            LookKeys.CURSOR_GLOW to 0,
        ),
        toggles = mapOf(
            LookKeys.SCAN_LINE to scanLine,
            LookKeys.RING to ring,
            LookKeys.UNFOLD to unfold,
            LookKeys.EDGE_GLOW to edgeGlow,
            LookKeys.OUTER_GLOW to outerGlow,
            LookKeys.PULSE to pulse,
            LookKeys.GLOW_ON to glowOn,
            LookKeys.TITANIUM_FRAME to titanium,
            LookKeys.SAPPHIRE_GLASS to sapphire,
            LookKeys.TOUCH_GLOW to touchGlow,
            LookKeys.TYPING_GLOW to typingGlow,
        ),
    )

    /** Original settings are retained, but are never applied by ThemeStore or shown as choices. */
    val definitions: Map<ArchivedTheme, ThemeSpec> by lazy {
        mapOf(
            ArchivedTheme.OLED to spec(
                ColorPart.entries.associateWith {
                    when (it) {
                        in laserParts -> phone(SystemRole.SYSTEM_TEXT)
                        ColorPart.CURSOR -> fixed(Themes.CURSOR_WHITE, role(it))
                        else -> phone(role(it))
                    }
                },
                opacity = 50, corners = 4, glow = 50,
                scanLine = false, ring = true, unfold = true, edgeGlow = true,
                outerGlow = true, pulse = false,
            ),
            ArchivedTheme.TITANIUM to spec(
                mapOf(
                    ColorPart.SKIN to fixed(0xFF020809.toInt(), role(ColorPart.SKIN)),
                    ColorPart.EDGE to fixed(0xFF19E3D6.toInt(), role(ColorPart.EDGE)),
                    ColorPart.OUTER_GLOW to fixed(0xFF00BFB3.toInt(), role(ColorPart.OUTER_GLOW)),
                    ColorPart.TEXT to phone(SystemRole.SYSTEM_TEXT),
                    ColorPart.CURSOR to fixed(Themes.CURSOR_WHITE, role(ColorPart.CURSOR)),
                    ColorPart.FOLDER to fixed(0xFF36E9FF.toInt(), role(ColorPart.FOLDER)),
                    ColorPart.STATUS to phone(SystemRole.SYSTEM_TEXT),
                ),
                opacity = 85, corners = 4, glow = 57,
                scanLine = true, ring = true, unfold = true, edgeGlow = true,
                outerGlow = true, pulse = true, titanium = true, sapphire = true,
            ),
            ArchivedTheme.TERMUX to spec(
                mapOf(
                    ColorPart.SKIN to fixed(0xFF000000.toInt(), role(ColorPart.SKIN)),
                    ColorPart.EDGE to fixed(0xFF1E8C3A.toInt(), role(ColorPart.EDGE)),
                    ColorPart.OUTER_GLOW to fixed(0xFF00FF41.toInt(), role(ColorPart.OUTER_GLOW)),
                    ColorPart.TEXT to fixed(Hud.HOLO_LASER, role(ColorPart.TEXT)),
                    ColorPart.CURSOR to fixed(Hud.HOLO_LASER, role(ColorPart.CURSOR)),
                    ColorPart.FOLDER to fixed(Hud.HOLO_LASER, role(ColorPart.FOLDER)),
                    ColorPart.STATUS to fixed(Hud.HOLO_LASER, role(ColorPart.STATUS)),
                ),
                opacity = 92, corners = 0, glow = 0,
                scanLine = false, ring = false, unfold = false, edgeGlow = false,
                outerGlow = false, pulse = false, touchGlow = false, typingGlow = false, glowOn = false,
            ),
            ArchivedTheme.HOLO to spec(
                mapOf(
                    ColorPart.SKIN to fixed(Hud.HOLO_GLASS_TOP, role(ColorPart.SKIN)),
                    ColorPart.EDGE to fixed(Hud.HOLO_CYAN, role(ColorPart.EDGE)),
                    ColorPart.OUTER_GLOW to fixed(Hud.HOLO_CYAN, role(ColorPart.OUTER_GLOW)),
                    ColorPart.TEXT to fixed(Hud.HOLO_LASER, role(ColorPart.TEXT)),
                    ColorPart.CURSOR to fixed(Hud.HOLO_LASER, role(ColorPart.CURSOR)),
                    ColorPart.FOLDER to fixed(Hud.HOLO_CYAN, role(ColorPart.FOLDER)),
                    ColorPart.STATUS to fixed(Hud.HOLO_LASER, role(ColorPart.STATUS)),
                ),
                opacity = 50, corners = 55, glow = 50,
                scanLine = true, ring = true, unfold = true, edgeGlow = true,
                outerGlow = false, pulse = false,
            ),
            ArchivedTheme.UV_BLACK_LIGHT to spec(
                mapOf(
                    ColorPart.SKIN to fixed(0xFF000000.toInt(), role(ColorPart.SKIN)),
                    ColorPart.EDGE to fixed(NeonPalette.UV_VIOLET, role(ColorPart.EDGE)),
                    ColorPart.OUTER_GLOW to fixed(NeonPalette.ELECTRIC_PURPLE, role(ColorPart.OUTER_GLOW)),
                    ColorPart.TEXT to phone(SystemRole.SYSTEM_TEXT),
                    ColorPart.CURSOR to fixed(NeonPalette.BLACKLIGHT_WHITE, role(ColorPart.CURSOR)),
                    ColorPart.FOLDER to fixed(NeonPalette.CYAN_GLOW, role(ColorPart.FOLDER)),
                    ColorPart.STATUS to phone(SystemRole.SYSTEM_TEXT),
                ),
                opacity = 92, corners = 4, glow = 100,
                scanLine = false, ring = true, unfold = true, edgeGlow = true,
                outerGlow = true, pulse = false,
            ),
        )
    }
}
