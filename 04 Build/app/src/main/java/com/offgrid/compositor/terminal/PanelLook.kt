package com.offgrid.compositor.terminal

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources

/** The seven parts of the panel that each have their own color folder, most used first. */
internal enum class ColorPart(val key: String, val title: String) {
    CURSOR("cursor", "Laser cursor color"),
    TEXT("text", "Typewriter text color"),
    EDGE("edge", "Edge color"),
    OUTER_GLOW("outer_glow", "Outer glow color"),
    SKIN("skin", "Skin color"),
    FOLDER("folder", "Folder icon color"),
    STATUS("status", "Status line color"),
}

/**
 * The phone's own colors (Material You, Android 12 and newer).
 * If the phone can't give one, a plain gray is used instead.
 */
internal enum class SystemRole(private val resId: Int, private val fallback: Int) {
    ACCENT1_100(android.R.color.system_accent1_100, 0xFFE4E4E4.toInt()),
    ACCENT1_200(android.R.color.system_accent1_200, 0xFFC8C8C8.toInt()),
    ACCENT1_300(android.R.color.system_accent1_300, 0xFFACACAC.toInt()),
    ACCENT1_400(android.R.color.system_accent1_400, 0xFF919191.toInt()),
    ACCENT2_100(android.R.color.system_accent2_100, 0xFFE4E4E4.toInt()),
    ACCENT2_200(android.R.color.system_accent2_200, 0xFFC8C8C8.toInt()),
    ACCENT3_200(android.R.color.system_accent3_200, 0xFFC8C8C8.toInt()),
    ACCENT3_300(android.R.color.system_accent3_300, 0xFFACACAC.toInt()),
    NEUTRAL1_50(android.R.color.system_neutral1_50, 0xFFF2F2F2.toInt()),
    NEUTRAL1_100(android.R.color.system_neutral1_100, 0xFFE4E4E4.toInt()),
    NEUTRAL1_1000(android.R.color.system_neutral1_1000, 0xFF000000.toInt()),
    NEUTRAL2_900(android.R.color.system_neutral2_900, 0xFF1B1B1B.toInt()),
    SYSTEM_TEXT(0, 0xFFFFFFFF.toInt());

    fun resolve(context: Context): Int {
        if (this == SYSTEM_TEXT) {
            val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
            return if (night) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        }
        return try {
            context.getColor(resId) or 0xFF000000.toInt()
        } catch (error: Resources.NotFoundException) {
            fallback
        }
    }
}

/** One part's color in a theme: follow the phone, or a fixed color. */
internal class PartSpec(val system: Boolean, val role: SystemRole, val custom: Int)

/** A ready-made look. Tapping it in the Themes folder applies it. */
internal enum class Theme(val key: String, val title: String, val blurb: String) {
    SYSTEM("system", "System", "Pure black and white follow the selected theme mode.");

    companion object {
        fun from(key: String?): Theme = entries.firstOrNull { it == SYSTEM && it.key == key } ?: SYSTEM
    }
}

internal enum class ThemeMode(val key: String) {
    AUTO("auto"),
    DAY("day"),
    NIGHT("night");

    companion object {
        fun from(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: AUTO
    }
}

/** Keys for the look settings. Typed numbers are 0 to 100. */
internal object LookKeys {
    const val THEME_MODE = "theme_mode"
    const val OPACITY = "opacity"
    const val TYPING = "typing"
    const val FONT_SIZE = "font_size"
    const val CORNERS = "corners"
    const val GLOW = "glow"
    const val PULSE_SPEED = "pulse_speed"

    /** Step 2.3: the soft glow around the laser cursor. 0 means a solid block only. */
    const val CURSOR_GLOW = "cursor_glow"

    /** Panel size as a share of the room it may use. 0 is the smallest readable size. */
    const val WIDTH = "width"
    const val HEIGHT = "height"
    const val FULL_SCREEN = "full_screen"
    const val SEE_THROUGH_FULL_SIZE = "see_through_full_size"
    const val BORDER_THICKNESS = "border_thickness"

    const val TEST_DIAMOND = "test_diamond"
    const val SCAN_LINE = "scan_line"
    const val LASER_TYPING = "laser_typing"
    const val RING = "ring"
    const val UNFOLD = "unfold"
    const val EDGE_GLOW = "edge_glow"
    const val OUTER_GLOW = "outer_glow"
    const val PULSE = "pulse"

    /** Step 2.4: all the panel's glow on or off. Glow strength sets how bright. */
    const val GLOW_ON = "glow_on"

    /** Step 2.2 premium look and touch feel. */
    const val TITANIUM_FRAME = "titanium_frame"
    const val SAPPHIRE_GLASS = "sapphire_glass"
    const val TOUCH_GLOW = "touch_glow"
    const val TYPING_GLOW = "typing_glow"
    const val TICK = "tick"
    const val CENTER_GUIDE = "center_guide"

    /** Final build: the toggle style (Pure White or Pure Black) and the optional toggle glow. */
    const val TOGGLE_STYLE = "toggle_style"
    const val TOGGLE_GLOW = "toggle_glow"
}

/** Everything a theme sets. Typing speed, font size, laser typing, and the test diamond are not theme settings. */
internal class ThemeSpec(
    val parts: Map<ColorPart, PartSpec>,
    val numbers: Map<String, Int>,
    val toggles: Map<String, Boolean>,
)

internal object Themes {
    /** Step 2.3: the laser cursor is a solid pure white block by default. */
    const val CURSOR_WHITE = 0xFFFFFFFF.toInt()

    private fun phone(role: SystemRole) = PartSpec(system = true, role = role, custom = 0xFF000000.toInt())

    fun spec(theme: Theme): ThemeSpec = when (theme) {
        Theme.SYSTEM -> ThemeSpec(
            parts = mapOf(
                ColorPart.SKIN to phone(SystemRole.NEUTRAL2_900),
                ColorPart.EDGE to phone(SystemRole.ACCENT1_200),
                ColorPart.OUTER_GLOW to phone(SystemRole.ACCENT1_300),
                ColorPart.TEXT to phone(SystemRole.SYSTEM_TEXT),
                ColorPart.CURSOR to phone(SystemRole.ACCENT1_200),
                ColorPart.FOLDER to phone(SystemRole.ACCENT2_200),
                ColorPart.STATUS to phone(SystemRole.SYSTEM_TEXT),
            ),
            numbers = mapOf(
                LookKeys.OPACITY to 100, LookKeys.CORNERS to 6, LookKeys.GLOW to 32,
                LookKeys.PULSE_SPEED to 42,
                LookKeys.CURSOR_GLOW to 0,
            ),
            toggles = mapOf(
                LookKeys.SCAN_LINE to false, LookKeys.RING to true, LookKeys.UNFOLD to true,
                LookKeys.EDGE_GLOW to true, LookKeys.OUTER_GLOW to true, LookKeys.PULSE to true,
                LookKeys.GLOW_ON to true,
                LookKeys.TITANIUM_FRAME to false, LookKeys.SAPPHIRE_GLASS to false,
                LookKeys.TOUCH_GLOW to true, LookKeys.TYPING_GLOW to true,
            ),
        )
    }

    /** Settings no theme touches, with their start values. */
    val PERSONAL_NUMBERS = mapOf(
        LookKeys.TYPING to 1, LookKeys.FONT_SIZE to 50, LookKeys.WIDTH to 100, LookKeys.HEIGHT to 100,
        LookKeys.BORDER_THICKNESS to 50,
    )
    val PERSONAL_TOGGLES = mapOf(
        LookKeys.LASER_TYPING to true, LookKeys.TEST_DIAMOND to false,
        LookKeys.TICK to true, LookKeys.CENTER_GUIDE to true,
        LookKeys.FULL_SCREEN to false, LookKeys.SEE_THROUGH_FULL_SIZE to false,
    )
}

/** One part's saved state. */
internal class PartState(val system: Boolean, val role: SystemRole, val custom: Int, val mix: IntArray?)

/** Everything the panel needs to draw itself, worked out once per change. */
internal data class PanelLook(
    val base: Theme,
    val skin: Int,
    val edge: Int,
    val outerGlow: Int,
    val text: Int,
    val cursor: Int,
    val folder: Int,
    val secondaryText: Int,
    val status: Int,
    val label: Int,
    val decor: Int,
    val specialFolder: Int,
    val opacity: Float,
    /** 0 to 1. How bright the glow is. Glow strength 50 is 0.7, the normal look. 71 and up is full. */
    val glowStrength: Float,
    /** 0 to 1. Extra glow above Glow strength 50, so 100 is big and bright. 0 at 50 and below. */
    val glowBoost: Float,
    val edgeGlow: Boolean,
    val outerGlowOn: Boolean,
    val pulse: Boolean,
    val pulseSpeed: Float,
    val roundness: Float,
    val fontScale: Float,
    val ring: Boolean,
    val unfold: Boolean,
    val scanLine: Boolean,
    val laserTyping: Boolean,
    val typingSpeed: Float,
    /** Dark brushed titanium and woven carbon fiber around the glass. */
    val titanium: Boolean,
    /** Thick, deep sapphire glass inside the panel. */
    val sapphire: Boolean,
    /** The dark tint of the sapphire glass panes. */
    val glassTint: Int,
    /** 0 to 1. The optional soft glow around the cursor. 0 is a solid block only. */
    val cursorGlow: Float,
    val touchGlow: Boolean,
    val typingGlow: Boolean,
    val tick: Boolean,
    val centerGuide: Boolean,
    /** Pure White (the start) or Pure Black power toggles. */
    val toggleStyle: ToggleStyle = ToggleStyle.WHITE,
    /** A soft halo around toggles that are on. It starts off. */
    val toggleGlow: Boolean = false,
    val borderThickness: Int = 50,
    val glowBloom: Float = 0f,
    val glowValue: Int = 50,
) {
    /** Holo Classic keeps the Step 2 cut corners, gold brackets, and tinted glass. */
    val holo: Boolean get() = false

    /** Letters per second for the laser typewriter. */
    val lettersPerSecond: Float get() = 12f + typingSpeed * 138f

    companion object {
        /** The look in use right now. Text spans read it when they draw. */
        var current: PanelLook? = null
    }
}

/** Reads and writes the theme settings. Unsaved values come from the current theme. */
internal class ThemeStore(private val context: Context, private val prefs: PanelPrefs) {

    fun base(): Theme = Theme.from(prefs.text(BASE))

    fun themeMode(): ThemeMode = ThemeMode.from(prefs.text(THEME_MODE))

    fun setThemeMode(mode: ThemeMode) {
        prefs.write { text(THEME_MODE, mode.key) }
    }

    private fun spec(): ThemeSpec = Themes.spec(base())

    fun number(key: String): Int {
        val default = spec().numbers[key] ?: Themes.PERSONAL_NUMBERS[key] ?: PanelTree.defaults.numbers[key] ?: 50
        return prefs.number(key, default)
    }

    fun toggle(key: String): Boolean {
        val default = spec().toggles[key] ?: Themes.PERSONAL_TOGGLES[key] ?: PanelTree.defaults.toggles[key] ?: false
        return prefs.toggle(key, default)
    }

    fun setNumber(key: String, value: Int) = prefs.setNumber(key, value)

    fun setToggle(key: String, value: Boolean) = prefs.setToggle(key, value)

    /** The saved choice for a pick-one setting, or its start value. */
    fun choice(key: String): String = prefs.text(key) ?: PanelTree.defaults.choices[key].orEmpty()

    fun setChoice(key: String, value: String) = prefs.write { text(key, value) }

    /** The saved text for a typed text setting, or its start value. */
    fun text(key: String): String = prefs.text(key) ?: PanelTree.defaults.texts[key].orEmpty()

    fun setText(key: String, value: String) = prefs.write { text(key, value) }

    fun part(part: ColorPart): PartState {
        val spec = spec().parts.getValue(part)
        val role = prefs.text(ROLE + part.key)?.let { saved -> SystemRole.entries.firstOrNull { it.name == saved } }
        return PartState(
            system = prefs.toggle(SYSTEM + part.key, spec.system),
            role = role ?: spec.role,
            custom = prefs.color(CUSTOM + part.key, spec.custom),
            mix = RainbowMix.decode(prefs.text(MIX + part.key)),
        )
    }

    fun systemColor(part: ColorPart): Int = part(part).role.resolve(context)

    fun color(part: ColorPart): Int {
        if (base() == Theme.SYSTEM) {
            val dark = when (themeMode()) {
                ThemeMode.AUTO -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                    Configuration.UI_MODE_NIGHT_YES
                ThemeMode.DAY -> false
                ThemeMode.NIGHT -> true
            }
            when (part) {
                ColorPart.SKIN -> return if (dark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                ColorPart.TEXT, ColorPart.STATUS -> return if (dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                else -> Unit
            }
        }
        val state = part(part)
        return if (state.system) state.role.resolve(context) else state.custom
    }

    /** The nine numbers to show for this part. */
    fun mix(part: ColorPart): IntArray {
        val state = part(part)
        return if (!state.system && state.mix != null) state.mix else RainbowMix.split(color(part))
    }

    /** Turns "Use system color" on or off. Turning it off keeps the color that is showing now. */
    fun setSystem(part: ColorPart, on: Boolean) {
        val showing = color(part)
        prefs.write {
            toggle(SYSTEM + part.key, on)
            if (!on) {
                color(CUSTOM + part.key, showing)
                text(MIX + part.key, RainbowMix.encode(RainbowMix.split(showing)))
            }
        }
    }

    fun setCustom(part: ColorPart, color: Int, mix: IntArray) {
        prefs.write {
            toggle(SYSTEM + part.key, false)
            color(CUSTOM + part.key, color or 0xFF000000.toInt())
            text(MIX + part.key, RainbowMix.encode(mix))
        }
    }

    /** Puts one part back to the current theme's color. */
    fun resetPart(part: ColorPart) {
        prefs.write { clearPart(part) }
    }

    private fun PanelPrefs.Writer.clearPart(part: ColorPart) {
        removeToggle(SYSTEM + part.key)
        removeColor(CUSTOM + part.key)
        text(ROLE + part.key, null)
        text(MIX + part.key, null)
    }

    /** Applies a theme. Every setting can still be changed afterward. */
    fun applyTheme(theme: Theme) {
        val next = Themes.spec(theme)
        prefs.write {
            text(BASE, theme.key)
            ColorPart.entries.forEach { clearPart(it) }
            next.numbers.keys.forEach { removeNumber(it) }
            next.toggles.keys.forEach { removeToggle(it) }
        }
    }

    fun resetAll() = prefs.resetAll()

    /** Puts these settings back to their start values, by letting go of the saved ones. */
    fun clear(numbers: Collection<String>, toggles: Collection<String>, texts: Collection<String>) {
        prefs.write {
            numbers.forEach { removeNumber(it) }
            toggles.forEach { removeToggle(it) }
            texts.forEach { text(it, null) }
        }
    }

    /** A few colors to preview a theme before it is applied: glass, edge, glow, and text. */
    fun preview(theme: Theme): IntArray {
        val parts = Themes.spec(theme).parts
        fun pick(part: ColorPart): Int {
            val spec = parts.getValue(part)
            return if (spec.system) spec.role.resolve(context) else spec.custom
        }
        return intArrayOf(pick(ColorPart.SKIN), pick(ColorPart.EDGE), pick(ColorPart.OUTER_GLOW), pick(ColorPart.TEXT))
    }

    fun look(): PanelLook {
        val base = base()
        val edge = color(ColorPart.EDGE)
        val folder = color(ColorPart.FOLDER)
        val text = color(ColorPart.TEXT)
        // Font size 50 is normal. 0 is three quarters as big. 100 is almost half again as big.
        val fontValue = number(LookKeys.FONT_SIZE)
        val fontScale = if (fontValue <= 50) 0.75f + 0.25f * fontValue / 50f else 1f + 0.45f * (fontValue - 50) / 50f
        val glowOn = toggle(LookKeys.GLOW_ON)
        val look = PanelLook(
            base = base,
            skin = color(ColorPart.SKIN),
            edge = edge,
            outerGlow = color(ColorPart.OUTER_GLOW),
            text = text,
            cursor = color(ColorPart.CURSOR),
            folder = folder,
            secondaryText = if (base == Theme.SYSTEM) text else Hud.withAlpha(text, 0.78f),
            status = color(ColorPart.STATUS),
            label = if (base == Theme.SYSTEM) text else Hud.lighten(text, 0.55f),
            decor = edge,
            specialFolder = folder,
            opacity = number(LookKeys.OPACITY) / 100f,
            glowStrength = if (glowOn) PanelGeometry.glowStrength(number(LookKeys.GLOW)) else 0f,
            glowBoost = if (glowOn) PanelGeometry.glowBoost(number(LookKeys.GLOW)) else 0f,
            borderThickness = number(LookKeys.BORDER_THICKNESS),
            glowBloom = if (glowOn) PanelGeometry.glowBloom(number(LookKeys.GLOW)) else 0f,
            glowValue = number(LookKeys.GLOW),
            edgeGlow = toggle(LookKeys.EDGE_GLOW),
            outerGlowOn = toggle(LookKeys.OUTER_GLOW),
            pulse = toggle(LookKeys.PULSE),
            pulseSpeed = number(LookKeys.PULSE_SPEED) / 100f,
            roundness = number(LookKeys.CORNERS) / 100f,
            fontScale = fontScale,
            ring = toggle(LookKeys.RING),
            unfold = toggle(LookKeys.UNFOLD),
            scanLine = toggle(LookKeys.SCAN_LINE),
            laserTyping = toggle(LookKeys.LASER_TYPING),
            typingSpeed = number(LookKeys.TYPING) / 100f,
            titanium = toggle(LookKeys.TITANIUM_FRAME),
            sapphire = toggle(LookKeys.SAPPHIRE_GLASS),
            glassTint = glassTint(text, edge),
            cursorGlow = number(LookKeys.CURSOR_GLOW) / 100f,
            touchGlow = toggle(LookKeys.TOUCH_GLOW),
            typingGlow = toggle(LookKeys.TYPING_GLOW),
            tick = toggle(LookKeys.TICK),
            centerGuide = toggle(LookKeys.CENTER_GUIDE),
            toggleStyle = ToggleStyle.from(choice(LookKeys.TOGGLE_STYLE)),
            toggleGlow = toggle(LookKeys.TOGGLE_GLOW),
        )
        PanelLook.current = look
        return look
    }

    /** Sapphire glass takes the more colorful of the text and edge colors, deep and dark. */
    private fun glassTint(text: Int, edge: Int): Int {
        val source = if (Hud.saturation(text) >= Hud.saturation(edge)) text else edge
        return Hud.blend(source, Hud.INK, 0.84f)
    }

    private companion object {
        const val BASE = "base_theme"
        const val SYSTEM = "system_color."
        const val ROLE = "role."
        const val CUSTOM = "custom."
        const val MIX = "mix."
    }
}
