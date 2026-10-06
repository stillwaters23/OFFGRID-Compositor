package com.offgrid.compositor.terminal

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.text.style.UpdateAppearance

/** What kind of folder it is. Settings and premium folders use the special folder color. */
internal enum class FolderKind { SETTINGS, OFFICE, COLOR, PREMIUM, PLATFORM, EFFECTS, TESTS }

/** Something inside a folder, in the order it shows. */
internal sealed class PanelItem {
    /** How many items this counts as on the folder icon. */
    open val count: Int get() = 1
}

/** A typed whole number from 0 to 100. Never a slider. */
internal class NumberItem(val key: String, val label: String, val help: String) : PanelItem()

/** An on and off toggle. Every toggle uses the same art from 03 Design/References. */
internal class ToggleItem(
    val key: String,
    val label: String,
    val help: String,
    val default: Boolean,
    val isTest: Boolean,
) : PanelItem()

/** A button that does one thing, like Reset all. */
internal class ActionItem(val action: PanelAction, val label: String, val help: String) : PanelItem()

internal enum class PanelAction { RESET_ALL, CENTER_PANEL }

/** One ready-made theme. Tapping it applies it. */
internal class ThemeItem(val theme: Theme) : PanelItem()

/** The color editor for one part: hex code, six typed numbers, swatch, Use system color, and Reset. */
internal class ColorItem(val part: ColorPart) : PanelItem() {
    override val count: Int get() = 9
}

/** The mark next to a private test toggle (feature 37). */
internal enum class TestMark { NOT_TESTED, SUCCESS, FAILED }

internal class Results(val success: Int, val failed: Int)

/**
 * Read-only guidance lines (feature 19).
 * Each value comes from 02 Research or the scope docs, or it says UNCONFIRMED.
 */
internal class Guide(
    val canDo: String,
    val cantDo: String,
    val where: String,
    val browser: String,
    val vpn: String = VPN_LINE,
    val order: String = ORDER_LINE,
    val phone: String = PHONE_LINE,
)

internal class PanelFolder(
    val id: String,
    val title: String,
    val tag: String?,
    val kind: FolderKind,
    val folders: List<PanelFolder> = emptyList(),
    val items: List<PanelItem> = emptyList(),
    val guide: Guide? = null,
    val notes: List<String> = emptyList(),
) {
    var parent: PanelFolder? = null
        private set

    init {
        folders.forEach { it.parent = this }
    }

    val itemCount: Int get() = folders.size + items.sumOf { it.count }

    val label: String get() = if (tag == null) title else "$tag $title"

    /** Settings, the theme office, and the premium lane use the special folder color. */
    val special: Boolean get() = kind == FolderKind.SETTINGS || kind == FolderKind.OFFICE ||
        kind == FolderKind.COLOR || kind == FolderKind.PREMIUM

    /** The folders from the top level down to this one. */
    fun trail(): List<PanelFolder> {
        return generateSequence(this) { it.parent }.filter { it.parent != null }.toList().reversed()
    }

    fun topLevel(): PanelFolder = trail().first()

    /** Counts the marks next to the test toggles in this folder and its subfolders. */
    fun results(markOf: (PanelFolder, ToggleItem) -> TestMark): Results {
        var success = 0
        var failed = 0
        fun walk(folder: PanelFolder) {
            for (item in folder.items) {
                if (item !is ToggleItem || !item.isTest) continue
                when (markOf(folder, item)) {
                    TestMark.SUCCESS -> success++
                    TestMark.FAILED -> failed++
                    TestMark.NOT_TESTED -> Unit
                }
            }
            folder.folders.forEach { walk(it) }
        }
        walk(this)
        return Results(success, failed)
    }
}

/**
 * Every folder in the terminal panel.
 * The 18 platform folders follow 01 Brainstorm/Feature Scope.md, feature 13.
 * Platform folders hold no effects yet. Effects come in later steps.
 */
internal object PanelTree {
    fun build(): PanelFolder {
        val top = listOf(settingsFolder(), offgridFolder()) +
            PLATFORMS.mapIndexed { index, platform -> platformFolder(index + 1, platform) }
        return PanelFolder(id = "home", title = "HOME", tag = null, kind = FolderKind.SETTINGS, folders = top)
    }

    /**
     * Panel settings. Every control sits inside a folder. Nothing is loose here.
     * The theme office is the Themes, Colors, Glow effects, and Toggle style folders.
     */
    private fun settingsFolder() = PanelFolder(
        id = "panel_settings",
        title = "Panel settings",
        tag = null,
        kind = FolderKind.SETTINGS,
        folders = listOf(
            sizeFolder(),
            displayFolder(),
            themesFolder(),
            colorsFolder(),
            glowFolder(),
            toggleStyleFolder(),
            resetFolder(),
        ),
        notes = listOf(
            "Every setting is in a folder. Tap a folder to open it.",
            "Type a number from 0 to 100. Then tap Done.",
            "Green toggle means on. Red toggle means off.",
            "No sliders here. Only typed numbers and toggles.",
        ),
    )

    private fun sizeFolder() = PanelFolder(
        id = "size",
        title = "📁 Size",
        tag = null,
        kind = FolderKind.OFFICE,
        items = listOf(
            NumberItem(LookKeys.WIDTH, "Width", "0 is the narrowest panel. 100 is the widest."),
            NumberItem(LookKeys.HEIGHT, "Height", "0 is the shortest panel. 100 is the tallest."),
            ActionItem(PanelAction.CENTER_PANEL, "Center panel", "Moves the panel to the middle of the screen."),
            ToggleItem(
                LookKeys.CENTER_GUIDE,
                "Center guide",
                "A soft cross shows the middle while you drag.",
                true,
                isTest = false,
            ),
            NumberItem(LookKeys.OPACITY, "Opacity", "How solid the panel looks. 0 is clear. 100 is solid."),
            NumberItem(LookKeys.FONT_SIZE, "Font size", "How big the words are. 50 is normal."),
            NumberItem(LookKeys.CORNERS, "Corner roundness", "0 is square. 100 is very round."),
            NumberItem(LookKeys.TYPING, "Typing speed", "How fast the laser types."),
        ),
        notes = listOf(
            "Width and Height are also at the top of the panel.",
            "Pinch with two fingers, or drag the corner lines, to resize.",
        ),
    )

    private fun displayFolder() = PanelFolder(
        id = "display",
        title = "📁 Display",
        tag = null,
        kind = FolderKind.OFFICE,
        items = listOf(
            ToggleItem(
                LookKeys.TEST_DIAMOND,
                "Test diamond",
                "Paints the yellow test shape into the video. It starts off.",
                false,
                isTest = false,
            ),
            ToggleItem(LookKeys.SCAN_LINE, "Scan line", "A soft line sweeps across the panel.", true, isTest = false),
            ToggleItem(LookKeys.LASER_TYPING, "Laser typing", "Off shows the text all at once.", true, isTest = false),
            ToggleItem(LookKeys.RING, "Ring", "A soft ring turns around the open folder.", true, isTest = false),
            ToggleItem(
                LookKeys.UNFOLD,
                "Hologram unfold",
                "Folders open like a hologram. Off opens them at once.",
                true,
                isTest = false,
            ),
            ToggleItem(
                LookKeys.TITANIUM_FRAME,
                "Titanium frame",
                "Dark titanium and carbon fiber around the glass.",
                true,
                isTest = false,
            ),
            ToggleItem(
                LookKeys.SAPPHIRE_GLASS,
                "Sapphire glass",
                "Thick, deep glass behind open folders.",
                true,
                isTest = false,
            ),
            ToggleItem(LookKeys.TICK, "Tick when saved", "Your phone ticks softly when a number is saved.", true, isTest = false),
        ),
        notes = listOf(
            "These change what the panel shows.",
            "The tick follows your phone's own touch feel setting.",
        ),
    )

    private fun resetFolder() = PanelFolder(
        id = "reset",
        title = "📁 Reset",
        tag = null,
        kind = FolderKind.OFFICE,
        items = listOf(
            ActionItem(PanelAction.RESET_ALL, "Reset all to default", "Puts every panel setting back to the start."),
        ),
        notes = listOf(
            "Reset all asks first. Nothing changes until you say yes.",
            "Your private test marks stay.",
        ),
    )

    private fun themesFolder() = PanelFolder(
        id = "themes",
        title = "📁 Themes",
        tag = null,
        kind = FolderKind.OFFICE,
        items = Theme.entries.map { ThemeItem(it) },
        notes = listOf(
            "Tap a theme to use it.",
            "You can still change every setting after.",
            "OLED means the screen's black parts turn fully off.",
        ),
    )

    private fun colorsFolder() = PanelFolder(
        id = "colors",
        title = "📁 Colors",
        tag = null,
        kind = FolderKind.OFFICE,
        folders = ColorPart.entries.map { colorFolder("colors", it) },
        notes = listOf(
            "Each folder changes the color of one part.",
            "Open one to type a hex code, or mix six colors.",
        ),
    )

    private fun colorFolder(parentId: String, part: ColorPart) = PanelFolder(
        id = "$parentId.${part.key}",
        title = "📁 ${part.title}",
        tag = null,
        kind = FolderKind.COLOR,
        items = listOf(ColorItem(part)) + PART_EXTRAS[part].orEmpty(),
        notes = listOf(
            PART_HELP.getValue(part),
            "Type a hex code like #00FF66, then tap Done.",
            "Or mix the six colors below, each from 0 to 100.",
            "Use system color follows your phone's own colors.",
        ),
    )

    /** Extra settings that live next to a color. The cursor's soft glow is off at the start. */
    private val PART_EXTRAS = mapOf(
        ColorPart.CURSOR to listOf(
            NumberItem(LookKeys.CURSOR_GLOW, "Cursor glow", "A soft glow around the cursor. 0 is a solid block only."),
        ),
    )

    private val PART_HELP = mapOf(
        ColorPart.SKIN to "This colors the panel glass.",
        ColorPart.EDGE to "This colors the thin line around the panel.",
        ColorPart.OUTER_GLOW to "This colors the soft glow outside the panel.",
        ColorPart.TEXT to "This colors the typed guidance text.",
        ColorPart.CURSOR to "This colors the laser cursor. It is one solid color.",
        ColorPart.TOGGLE_GLOW to "This colors the swirl inside each toggle.",
        ColorPart.FOLDER to "This colors the folder icons.",
        ColorPart.SIDE_TEXT to "This colors the folder names.",
        ColorPart.STATUS to "This colors the status line at the top.",
    )

    private fun glowFolder() = PanelFolder(
        id = "glow_effects",
        title = "📁 Glow effects",
        tag = null,
        kind = FolderKind.OFFICE,
        items = listOf(
            ToggleItem(LookKeys.EDGE_GLOW, "Edge glow", "A soft glow along the panel's edge.", true, isTest = false),
            ToggleItem(LookKeys.OUTER_GLOW, "Outer glow", "A soft glow outside the panel.", true, isTest = false),
            ToggleItem(LookKeys.PULSE, "Pulse", "The glow breathes in and out.", true, isTest = false),
            NumberItem(LookKeys.PULSE_SPEED, "Pulse speed", "How fast the glow breathes."),
            NumberItem(LookKeys.GLOW, "Glow strength", "How bright the glow is. 0 turns it off."),
            ToggleItem(
                LookKeys.SWIRL,
                "Bioluminescent liquid swirl",
                "Light flows around the edge, like living sea light.",
                true,
                isTest = false,
            ),
            NumberItem(LookKeys.SWIRL_SPEED, "Swirl speed", "How fast all the liquid light moves."),
            ToggleItem(LookKeys.TOUCH_GLOW, "Touch glow", "Light ripples where you touch the panel's edge.", true, isTest = false),
            ToggleItem(LookKeys.TYPING_GLOW, "Typing glow", "The box you are typing in glows softly.", true, isTest = false),
        ),
        notes = listOf(
            "Bioluminescent means it glows like living sea light.",
            "Pulse makes the glow breathe slowly.",
        ),
    )

    private fun toggleStyleFolder() = PanelFolder(
        id = "toggle_style",
        title = "📁 Toggle style",
        tag = null,
        kind = FolderKind.OFFICE,
        folders = listOf(colorFolder("toggle_style", ColorPart.TOGGLE_GLOW)),
        items = listOf(
            ToggleItem(LookKeys.TOGGLE_SWIRL, "Liquid swirl", "Light swirls inside each toggle's ball.", true, isTest = false),
            NumberItem(LookKeys.TOGGLE_GLOW, "Glow strength", "How bright the light inside each toggle is."),
        ),
        notes = listOf(
            "These change the light inside each toggle.",
            "The red and green toggle art stays the same.",
        ),
    )

    private fun offgridFolder() = PanelFolder(
        id = "offgrid_to_offgrid",
        title = "OFFGRID to OFFGRID",
        tag = null,
        kind = FolderKind.PREMIUM,
        guide = Guide(
            canDo = "The premium lane. Both sides see every effect. Calls come in Step 12.",
            cantDo = "No calls yet. Nothing in this folder is built.",
            where = "The OFFGRID app on both phones.",
            browser = "None needed.",
            vpn = "UNCONFIRMED for this lane. VPN means virtual private network. It hides where you are.",
            order = "UNCONFIRMED for this lane.",
            phone = "Dummy phone 1 and dummy phone 2.",
        ),
        notes = listOf("Empty for now."),
    )

    private fun platformFolder(number: Int, platform: Platform): PanelFolder {
        return PanelFolder(
            id = platform.id,
            title = platform.title,
            tag = number.toString().padStart(2, '0'),
            kind = FolderKind.PLATFORM,
            guide = platform.guide,
            folders = listOf(
                PanelFolder(
                    id = "${platform.id}.effects",
                    title = "Effects",
                    tag = null,
                    kind = FolderKind.EFFECTS,
                    notes = listOf(
                        "Empty for now. Effects come in later steps.",
                        "Every effect will show in the video viewers see.",
                    ),
                ),
                PanelFolder(
                    id = "${platform.id}.tests",
                    title = "Tests",
                    tag = null,
                    kind = FolderKind.TESTS,
                    items = TEST_TOGGLES,
                    notes = listOf(
                        "Private test toggles. Viewers never see them.",
                        "Green means on. Red means off.",
                        TEST_MARKERS_LINE,
                    ),
                ),
            ),
        )
    }

    /** Tells Royalty the test toggles don't change anything inside live-stream apps yet. */
    const val TEST_MARKERS_LINE = "These are test markers for now. Real checks come in a later step."

    /** The example tests from feature 15. They hold no checks yet. */
    private val TEST_TOGGLES = listOf(
        ToggleItem("burned_in", "Effects show in the video viewers see", "", false, isTest = true),
        ToggleItem("outside_video", "The app takes video from OFFGRID", "", false, isTest = true),
        ToggleItem("chat_gifts", "Chat and gifts connect", "", false, isTest = true),
        ToggleItem("stream_key", "A stream key works on the phone", "", false, isTest = true),
    )

    private class Platform(val id: String, val title: String, val guide: Guide)

    private val UNKNOWN = Guide(
        canDo = "UNCONFIRMED. No research yet.",
        cantDo = "UNCONFIRMED.",
        where = "UNCONFIRMED.",
        browser = "UNCONFIRMED.",
    )

    private val KEY_NOT_CHECKED = Guide(
        canDo = "No effects yet. A phone stream key is UNCONFIRMED.",
        cantDo = "UNCONFIRMED.",
        where = "UNCONFIRMED.",
        browser = "UNCONFIRMED.",
    )

    private val PLATFORMS = listOf(
        Platform(
            "bigo_live",
            "Bigo Live",
            Guide(
                canDo = "No effects yet. They come in later steps. A phone stream key is UNCONFIRMED.",
                cantDo = "You can't stop a host from kicking you. Never auto rejoin. It can get you banned.",
                where = "The app on your phone. Website: bigo.tv",
                browser = "UNCONFIRMED.",
            ),
        ),
        Platform(
            "tagged",
            "Tagged",
            Guide(
                canDo = "No effects yet. Tagged Live works in the app and on the website.",
                cantDo = "Hosts and their helpers can kick you. Modified apps break the rules.",
                where = "App or website. Effects use the website route. Web address: UNCONFIRMED.",
                browser = "UNCONFIRMED. A safe phone browser is checked in Step 10.",
            ),
        ),
        Platform(
            "meetme",
            "MeetMe",
            Guide(
                canDo = "No effects yet. Tap plus, then Guest, to ask to join. The streamer must accept.",
                cantDo = "If you get kicked, you can't rejoin that stream. Modified apps break the rules.",
                where = "The app on your phone. meetme.com is the app's company site. " +
                    "meetme.video is a separate browser site. Effects use the website route.",
                browser = "UNCONFIRMED. A safe phone browser is checked in Step 10.",
            ),
        ),
        Platform("superlive", "SuperLive", UNKNOWN),
        Platform(
            "tiktok",
            "TikTok",
            Guide(
                canDo = "No effects yet. You can ask to join a host's live. The host decides.",
                cantDo = "TikTok's rules ban apps that tap or join for you.",
                where = "The app on your phone. Website: tiktok.com",
                browser = "UNCONFIRMED.",
            ),
        ),
        Platform("twitch", "Twitch", KEY_NOT_CHECKED),
        Platform(
            "kick",
            "Kick",
            Guide(
                canDo = "No effects yet. Kick's help says phone apps can go live with a stream key.",
                cantDo = "Never paste your stream key in chat. Other limits: UNCONFIRMED.",
                where = "Copy the stream key from the Kick dashboard in a browser. Exact address: UNCONFIRMED.",
                browser = "UNCONFIRMED.",
            ),
        ),
        Platform(
            "youtube_live",
            "YouTube Live",
            Guide(
                canDo = "No effects yet. YouTube takes live video from phone streaming apps. " +
                    "A phone stream key is UNCONFIRMED.",
                cantDo = "The Go Live camera in the YouTube app can't show OFFGRID effects. " +
                    "Going live there needs 50 subscribers and a verified channel.",
                where = "The app on your phone. Web address: UNCONFIRMED.",
                browser = "UNCONFIRMED.",
            ),
        ),
        Platform("trovo", "Trovo", KEY_NOT_CHECKED),
        Platform("telegram", "Telegram", UNKNOWN),
        Platform(
            "tango",
            "Tango",
            Guide(
                canDo = "No effects yet. Battles are by invite, up to 4 people.",
                cantDo = "Bots are not allowed. Host helpers can kick viewers.",
                where = "The app on your phone. Hosts can also use Tango Web. Website: tango.me",
                browser = "UNCONFIRMED.",
            ),
        ),
        Platform(
            "instagram",
            "Instagram",
            Guide(
                canDo = "Share your screen in an Instagram video call. Effects there: UNCONFIRMED.",
                cantDo = "The Live stream key needs a computer, so it is not used. " +
                    "Outside camera effects ended in January 2025.",
                where = "The app on your phone.",
                browser = "None needed.",
            ),
        ),
        Platform(
            "messenger",
            "Messenger",
            Guide(
                canDo = "Share your screen in a Messenger call. Effects there: UNCONFIRMED.",
                cantDo = "Messenger has no stream key. The fake camera route needs a computer.",
                where = "The app on your phone.",
                browser = "None needed.",
            ),
        ),
        Platform(
            "whatsapp",
            "WhatsApp",
            Guide(
                canDo = "Share your screen in a WhatsApp video call. Effects there: UNCONFIRMED.",
                cantDo = "No live streams and no stream key. Never use a modified WhatsApp. It gets banned.",
                where = "The app on your phone.",
                browser = "None needed.",
            ),
        ),
        Platform(
            "zoom",
            "Zoom",
            Guide(
                canDo = "Share your screen in the Zoom app. Later, OFFGRID may join meetings you host. " +
                    "Effects there: UNCONFIRMED.",
                cantDo = "Zoom's stream key needs the computer app. Other people's meetings need Zoom's review.",
                where = "Zoom app now. Later, OFFGRID itself, with Zoom's own code kit.",
                browser = "None needed.",
                phone = "Dummy phone 1 or dummy phone 2. Use a dummy Zoom account.",
            ),
        ),
        Platform(
            "google_meet",
            "Google Meet",
            Guide(
                canDo = "Share your screen in the Meet app. Pick a single app so alerts stay hidden. " +
                    "Effects there: UNCONFIRMED.",
                cantDo = "The fake camera route at meet.google.com needs a computer.",
                where = "The app on your phone.",
                browser = "None needed.",
            ),
        ),
        Platform(
            "discord",
            "Discord",
            Guide(
                canDo = "Use Go Live to share your screen in the Discord app. Effects there: UNCONFIRMED.",
                cantDo = "Discord has no stream key. The fake camera route needs a computer.",
                where = "The app on your phone.",
                browser = "None needed.",
            ),
        ),
        Platform(
            "microsoft_teams",
            "Microsoft Teams",
            Guide(
                canDo = "Share your screen in the Teams app. Effects there: UNCONFIRMED.",
                cantDo = "Free meetings stop at 60 minutes. The fake camera route needs a computer. " +
                    "Skype is retired, so use Teams Free.",
                where = "The app on your phone.",
                browser = "None needed.",
            ),
        ),
    )
}

private const val VPN_LINE = "ProtonVPN in Stealth mode. VPN means virtual private network. " +
    "It hides where you are. Use a Philippines server for Philippines streams. Other countries: UNCONFIRMED."

private const val ORDER_LINE = "1) Make the Cloaked number with the VPN off. 2) Turn on Stealth. 3) Open the app."

private const val PHONE_LINE = "Dummy phone 1 or dummy phone 2. Use the dummy account only."

private const val IP_LINE = "UNCONFIRMED. IP means your internet address. This check comes in a later step."

/**
 * Builds the typed guidance panel text, one "> LABEL: value" line at a time.
 * Follows 06 Prompts and Skills/Folder Guidance Panel.md and the Terminal Plain Language Rule.
 */
internal object PanelText {
    fun guidance(folder: PanelFolder, results: Results): CharSequence {
        val lines = mutableListOf<Pair<String?, String>>()
        lines += "FOLDER" to folder.trail().joinToString(" › ") { it.title }
        val guide = folder.guide
        if (guide != null) {
            lines += "CAN DO" to guide.canDo
            lines += "CAN'T DO" to guide.cantDo
            lines += "WHERE" to guide.where
            lines += "BROWSER" to guide.browser
            lines += "VPN" to guide.vpn
            lines += "ORDER" to guide.order
            lines += "PHONE" to guide.phone
            lines += "IP CHECK" to IP_LINE
        }
        folder.notes.forEach { lines += null to it }
        if (guide != null || folder.kind == FolderKind.TESTS) {
            lines += "RESULTS" to "${results.success} baked in as success | " +
                "${results.failed} failed (flagged for research)"
        }
        return render(lines)
    }

    fun line(text: String): CharSequence = render(listOf(null to text))

    private fun render(lines: List<Pair<String?, String>>): CharSequence {
        val out = SpannableStringBuilder()
        lines.forEachIndexed { index, (label, value) ->
            if (index > 0) out.append('\n')
            out.append("> ")
            if (label != null) {
                val start = out.length
                out.append(label).append(':')
                out.setSpan(LabelSpan(), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.setSpan(StyleSpan(Typeface.BOLD), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.append(' ')
            }
            out.append(value)
        }
        return out
    }

    /** Labels like "CAN DO:" draw a little brighter than the text, in the current theme's color. */
    private class LabelSpan : CharacterStyle(), UpdateAppearance {
        override fun updateDrawState(paint: TextPaint) {
            PanelLook.current?.let { paint.color = it.label }
        }
    }
}
