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

/**
 * A typed whole number from 0 to 100. Never a slider.
 * [default] is the start value for settings no theme sets, like effects. Theme settings
 * take their start value from the theme.
 */
internal class NumberItem(val key: String, val label: String, val help: String, val default: Int? = null) : PanelItem()

/** An on and off toggle. Every toggle is the same coded power toggle (PowerToggleView). */
internal class ToggleItem(
    val key: String,
    val label: String,
    val help: String,
    val default: Boolean,
    val isTest: Boolean,
) : PanelItem()

/** One choice in a pick-one list. */
internal class Choice(val value: String, val label: String, val help: String)

/** What shows at the front of each choice: nothing, or a live toggle in that style. */
internal enum class ChoicePreview { NONE, TOGGLE }

/** Pick one of a few choices, like the toggle style or the camera mode. The choice is saved. */
internal class ChoiceItem(
    val key: String,
    val label: String,
    val choices: List<Choice>,
    val default: String,
    val preview: ChoicePreview = ChoicePreview.NONE,
) : PanelItem()

/** A short typed text, like a status word or a message. Letters, numbers, and emoji. Never a slider. */
internal class TextItem(
    val key: String,
    val label: String,
    val help: String,
    val default: String,
    val maxLength: Int,
) : PanelItem()

/** A button that does one thing, like Reset all. [arg] says what for, like a platform or an effect. */
internal class ActionItem(
    val action: PanelAction,
    val label: String,
    val help: String,
    val arg: String? = null,
) : PanelItem()

internal enum class PanelAction {
    RESET_ALL,
    CENTER_PANEL,

    /** Final build: puts one folder's settings back to the start. Its argument is the folder id. */
    RESET_FOLDER,

    /** Fires a one-shot effect. Its argument is the effect's name, like SHATTER. */
    FIRE,
    CLEAR_INK,
    TRY_SOUND,
    SAVE_TO_GALLERY,
    SAVE_REPLAY,

    /** Platform tools. Their argument is the platform id. */
    KICKED,
    OPEN_PLATFORM,
    SAFE_START,
    TEST_CODE,

    /** OFFGRID to OFFGRID handshake codes (feature 14). */
    HANDSHAKE_SHOW,
    HANDSHAKE_NEW,
    HANDSHAKE_ADD,
    HANDSHAKE_BLOCK,
    HANDSHAKE_CHECK,
}

/** Final build: the saved looks (theme, colors, glow, cursor, toggle style, size), with save, load, rename, delete. */
internal class PresetsItem : PanelItem()

/** One ready-made theme. Tapping it applies it. */
internal class ThemeItem(val theme: Theme) : PanelItem()

/** The color editor for one part: hex code, nine typed numbers, swatch, Use system color, and Reset. */
internal class ColorItem(val part: ColorPart) : PanelItem() {
    override val count: Int get() = 12
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

/** Start values for settings no theme sets, read from the items that declare them. */
internal class SettingDefaults(
    val numbers: Map<String, Int>,
    val toggles: Map<String, Boolean>,
    val choices: Map<String, String>,
    val texts: Map<String, String>,
)

/**
 * Every folder in the terminal panel.
 * The 18 platform folders follow 01 Brainstorm/Feature Scope.md, feature 13.
 */
internal object PanelTree {
    /** The start value of every setting in the tree, by key. Themes still win for their own settings. */
    val defaults: SettingDefaults by lazy {
        val numbers = HashMap<String, Int>()
        val toggles = HashMap<String, Boolean>()
        val choices = HashMap<String, String>()
        val texts = HashMap<String, String>()
        fun walk(folder: PanelFolder) {
            for (item in folder.items) {
                when (item) {
                    is NumberItem -> item.default?.let { numbers[item.key] = it }
                    is ToggleItem -> if (!item.isTest) toggles[item.key] = item.default
                    is ChoiceItem -> choices[item.key] = item.default
                    is TextItem -> texts[item.key] = item.default
                    else -> Unit
                }
            }
            folder.folders.forEach { walk(it) }
        }
        walk(build())
        SettingDefaults(numbers, toggles, choices, texts)
    }

    fun build(): PanelFolder {
        val top = listOf(
            settingsFolder(),
            EffectFolders.camera(),
            EffectFolders.effects(),
            EffectFolders.record(),
            offgridFolder(),
        ) + PLATFORMS.mapIndexed { index, platform -> platformFolder(index + 1, platform) }
        return PanelFolder(id = "home", title = "HOME", tag = null, kind = FolderKind.SETTINGS, folders = top)
    }

    /**
     * Panel settings. Every control sits inside a folder. Nothing is loose here.
     * The theme office is the Themes, Colors, and Glow effects folders.
     */
    private fun settingsFolder() = PanelFolder(
        id = "panel_settings",
        title = "Panel settings",
        tag = null,
        kind = FolderKind.SETTINGS,
        folders = listOf(
            sizeFolder(),
            displayFolder(),
            glowFolder(),
            colorsFolder(),
            themesFolder(),
            presetsFolder(),
            resetFolder(),
        ),
        notes = listOf(
            "Every setting is in a folder. Tap a folder to open it.",
            "Type a number from 0 to 100. Then tap Done.",
            "A lit toggle means on. A dim toggle means off.",
            "No sliders here. Only typed numbers and toggles.",
        ),
    )

    private fun sizeFolder() = PanelFolder(
        id = "size",
        title = "📁 Size",
        tag = null,
        kind = FolderKind.OFFICE,
        items = listOf(
            NumberItem(LookKeys.WIDTH, "Width", "0 is the narrowest panel. 100 fills the screen from side to side."),
            NumberItem(LookKeys.HEIGHT, "Height", "0 is the shortest panel. 100 fills the screen from top to bottom."),
            ActionItem(PanelAction.CENTER_PANEL, "Center panel", "Moves the panel to the middle of the screen."),
            NumberItem(LookKeys.OPACITY, "Opacity", "How solid the panel looks. 0 is clear. 50 is half see-through. 100 is solid."),
            NumberItem(LookKeys.FONT_SIZE, "Font size", "How big the words are. 50 is normal."),
            NumberItem(LookKeys.TYPING, "Typing speed", "How fast the laser types. 1 is slow. 100 is very fast."),
            NumberItem(LookKeys.CORNERS, "Corner roundness", "0 is square. 100 is very round."),
            ToggleItem(
                LookKeys.CENTER_GUIDE,
                "Center guide",
                "A soft cross shows the middle while you drag.",
                true,
                isTest = false,
            ),
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
            ToggleItem(LookKeys.LASER_TYPING, "Laser typing", "Off shows the text all at once.", true, isTest = false),
            ToggleItem(LookKeys.SCAN_LINE, "Scan line", "A soft line sweeps across the panel.", false, isTest = false),
            ToggleItem(LookKeys.RING, "Ring", "A soft ring turns around the open folder.", true, isTest = false),
            ToggleItem(
                LookKeys.UNFOLD,
                "Hologram unfold",
                "Folders open like a hologram. Off opens them at once.",
                true,
                isTest = false,
            ),
            ToggleItem(LookKeys.TICK, "Tick when saved", "Your phone ticks softly when a setting is saved.", true, isTest = false),
            ToggleItem(
                LookKeys.TITANIUM_FRAME,
                "Titanium frame",
                "Dark titanium and carbon fiber around the glass. It starts off.",
                false,
                isTest = false,
            ),
            ToggleItem(
                LookKeys.SAPPHIRE_GLASS,
                "Sapphire glass",
                "Thick, deep teal glass behind open folders. It starts off.",
                false,
                isTest = false,
            ),
        ),
        notes = listOf(
            "These change what the panel shows.",
            "The tick follows your phone's own touch feel setting.",
        ),
    )

    /** Final build: save the look you have now and bring it back later. */
    private fun presetsFolder() = PanelFolder(
        id = "presets",
        title = "📁 Presets",
        tag = null,
        kind = FolderKind.OFFICE,
        items = listOf(PresetsItem()),
        notes = listOf(
            "A preset keeps your look: theme, colors, glow, cursor, toggle style, and size.",
            "Type a name, then tap Save. Tap a preset to load, rename, or delete it.",
            "Effects and the camera mode are not in presets.",
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
        folders = listOf(toggleStyleFolder()),
        items = Theme.entries.map { ThemeItem(it) },
        notes = listOf(
            "Tap a theme to use it.",
            "You can still change every setting after.",
            "OLED means the screen's black parts turn fully off.",
        ),
    )

    /** Final build, milestone 1: the coded power toggles, in Pure White or Pure Black. */
    private fun toggleStyleFolder() = PanelFolder(
        id = "toggle_style",
        title = "📁 Toggle style",
        tag = null,
        kind = FolderKind.OFFICE,
        items = listOf(
            ChoiceItem(
                LookKeys.TOGGLE_STYLE,
                "Toggle style",
                ToggleStyle.entries.map { Choice(it.key, it.title, it.help) },
                default = ToggleStyle.WHITE.key,
                preview = ChoicePreview.TOGGLE,
            ),
            ToggleItem(
                LookKeys.TOGGLE_GLOW,
                "Toggle glow",
                "A soft halo in your glow color around toggles that are on. It starts off.",
                false,
                isTest = false,
            ),
        ),
        notes = listOf(
            "Every toggle is a circle with a power symbol.",
            "A lit toggle means on. A dim toggle means off.",
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
            "Open one to type a hex code, or mix nine colors.",
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
            "Or mix the nine colors below, each from 0 to 100.",
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
            ToggleItem(LookKeys.GLOW_ON, "Glow", "All the panel's glow, on or off.", true, isTest = false),
            NumberItem(LookKeys.GLOW, "Glow strength", "0 is no glow. 50 is normal. 100 is big and bright."),
            ToggleItem(LookKeys.PULSE, "Pulse", "The glow breathes in and out. Off keeps it steady.", false, isTest = false),
            NumberItem(LookKeys.PULSE_SPEED, "Pulse speed", "How fast the glow breathes. 0 is slow. 100 is fast."),
            ToggleItem(LookKeys.EDGE_GLOW, "Edge glow", "A soft glow along the panel's edge.", true, isTest = false),
            ToggleItem(LookKeys.OUTER_GLOW, "Outer glow", "A soft glow outside the panel.", true, isTest = false),
            ToggleItem(LookKeys.TOUCH_GLOW, "Touch glow", "Light ripples where you touch the panel's edge.", true, isTest = false),
            ToggleItem(LookKeys.TYPING_GLOW, "Typing glow", "The row you are typing in glows softly.", true, isTest = false),
        ),
        notes = listOf(
            "Glow turns all the glow on or off. Glow strength sets how bright.",
            "Pulse makes the glow breathe. Pulse speed sets how fast.",
        ),
    )

    private fun offgridFolder() = PanelFolder(
        id = "offgrid_to_offgrid",
        title = "OFFGRID to OFFGRID",
        tag = null,
        kind = FolderKind.PREMIUM,
        folders = listOf(handshakeFolder()),
        guide = Guide(
            canDo = "The premium lane. Both sides see every effect. Your handshake code is ready.",
            cantDo = "No calls yet. Calls need a server, which is not built.",
            where = "The OFFGRID app on both phones.",
            browser = "None needed.",
            vpn = "UNCONFIRMED for this lane. VPN means virtual private network. It hides where you are.",
            order = "UNCONFIRMED for this lane.",
            phone = "Dummy phone 1 and dummy phone 2.",
        ),
        notes = listOf("Calls are not built yet. The handshake codes are."),
    )

    /** Feature 14: the secret codes two OFFGRID phones swap before a call. Kept in the phone's locked storage. */
    private fun handshakeFolder() = PanelFolder(
        id = "offgrid_to_offgrid.handshake",
        title = "📁 Handshake",
        tag = null,
        kind = FolderKind.PREMIUM,
        items = listOf(
            ActionItem(PanelAction.HANDSHAKE_SHOW, "Copy my code", "Copies your 300 character code, to send to a trusted person."),
            ActionItem(PanelAction.HANDSHAKE_NEW, "Make a new code", "Your old code stops working. Do this after every call."),
            ActionItem(PanelAction.HANDSHAKE_ADD, "Trust a copied code", "Copy a person's code first, then tap here to trust it."),
            ActionItem(PanelAction.HANDSHAKE_BLOCK, "Block a copied code", "Copy a person's code first, then tap here. It never works again."),
            ActionItem(PanelAction.HANDSHAKE_CHECK, "Check a copied code", "Says if a copied code is trusted, blocked, or new."),
        ),
        notes = listOf(
            "Codes are kept locked on this phone. They are never saved in the app's files as plain text.",
            "Only a fingerprint of other people's codes is kept, never the code itself.",
            "Never paste a code in a chat or a public place.",
        ),
    )

    private fun platformFolder(number: Int, platform: Platform): PanelFolder {
        return PanelFolder(
            id = platform.id,
            title = platform.title,
            tag = number.toString().padStart(2, '0'),
            kind = FolderKind.PLATFORM,
            guide = platform.guide,
            folders = listOf(
                EffectFolders.platformEffects(platform.id, platform.title),
                EffectFolders.platformTests(platform.id, TEST_TOGGLES),
            ),
        )
    }

    /** Tells Royalty the test toggles are her own markers. The success code is the real check. */
    const val TEST_MARKERS_LINE = "These toggles are your own markers. 📁 Success code is the real check."

    /** The example tests from feature 15. Mark each one in 📁 Results. */
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
        canDo = "Your effects are baked into your video. A phone stream key is UNCONFIRMED.",
        cantDo = "UNCONFIRMED.",
        where = "UNCONFIRMED.",
        browser = "UNCONFIRMED.",
    )

    private val PLATFORMS = listOf(
        Platform(
            "bigo_live",
            "Bigo Live",
            Guide(
                canDo = "Your effects are baked into your video. A phone stream key is UNCONFIRMED.",
                cantDo = "You can't stop a host from kicking you. Never auto rejoin. It can get you banned.",
                where = "The app on your phone. Website: bigo.tv",
                browser = "UNCONFIRMED.",
            ),
        ),
        Platform(
            "tagged",
            "Tagged",
            Guide(
                canDo = "Tagged Live works in the app and on the website.",
                cantDo = "Hosts and their helpers can kick you. Modified apps break the rules.",
                where = "App or website. Effects use the website route. Web address: UNCONFIRMED.",
                browser = "UNCONFIRMED. A safe phone browser is checked in Step 10.",
            ),
        ),
        Platform(
            "meetme",
            "MeetMe",
            Guide(
                canDo = "Tap plus, then Guest, to ask to join. The streamer must accept.",
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
                canDo = "You can ask to join a host's live. The host decides.",
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
                canDo = "Kick's help says phone apps can go live with a stream key.",
                cantDo = "Never paste your stream key in chat. Other limits: UNCONFIRMED.",
                where = "Copy the stream key from the Kick dashboard in a browser. Exact address: UNCONFIRMED.",
                browser = "UNCONFIRMED.",
            ),
        ),
        Platform(
            "youtube_live",
            "YouTube Live",
            Guide(
                canDo = "YouTube takes live video from phone streaming apps. " +
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
                canDo = "Battles are by invite, up to 4 people.",
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
