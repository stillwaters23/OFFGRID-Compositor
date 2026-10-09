package com.offgrid.compositor.terminal

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.offgrid.compositor.effects.FxKeys

/** What kind of folder it is. It picks the glow color. */
internal enum class FolderKind { SETTINGS, PREMIUM, PLATFORM, EFFECTS, TESTS }

/** A typed whole number from 0 to 100. Never a slider. */
internal class NumberItem(val key: String, val label: String, val help: String, val default: Int)

/** An on and off toggle. Every toggle uses the same art from 03 Design/References. */
internal class ToggleItem(
    val key: String,
    val label: String,
    val help: String,
    val default: Boolean,
    val isTest: Boolean,
)

private fun colorsFolder() = PanelFolder(
    id = "colors",
    title = "Colors",
    tag = null,
    kind = FolderKind.SETTINGS,
    colors = listOf(
        ColorItem(
            PanelTree.BORDER_COLOR,
            "Border color",
            "Type a hex color or tap a neon swatch.",
            NeonPalette.ELECTRIC_PURPLE,
        ),
    ),
    notes = listOf(
        "UV Violet #7B02B3",
        "Electric Purple #B026FF",
        "Hot Magenta #FF00A6",
        "Neon Pink #FF2BD6",
        "Cyan Glow #00E5FC",
        "Deep Blue #0032FF",
        "Toxic Lime #B6FF00",
        "Blacklight White #F4ECFF",
    ),
)

private fun themesFolder() = PanelFolder(
    id = "themes",
    title = "Themes",
    tag = null,
    kind = FolderKind.SETTINGS,
    choices = listOf(
        ChoiceItem(
            PanelTree.THEME,
            "Theme",
            "Choose the panel glow palette.",
            listOf("OLED Bioluminescent", "UV Black Light"),
            "OLED Bioluminescent",
        ),
    ),
)

internal class ChoiceItem(
    val key: String,
    val label: String,
    val help: String,
    val options: List<String>,
    val default: String,
)

internal class ColorItem(val key: String, val label: String, val help: String, val default: Int)

enum class PanelAction {
    RESET_ALL,
    RESET_FOLDER,
    LIVE_OPEN_OVERLAY_SETTINGS,
    LIVE_CLEAN_OUTPUT,
    REMOTE_OPEN_PAIRING,
}

internal class PanelActionItem(
    val action: PanelAction,
    val label: String,
    val help: String,
)

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
    val numbers: List<NumberItem> = emptyList(),
    val toggles: List<ToggleItem> = emptyList(),
    val choices: List<ChoiceItem> = emptyList(),
    val colors: List<ColorItem> = emptyList(),
    val actions: List<PanelActionItem> = emptyList(),
    val guide: Guide? = null,
    val notes: List<String> = emptyList(),
) {
    var parent: PanelFolder? = null
        private set

    init {
        folders.forEach { it.parent = this }
    }

    val itemCount: Int get() = folders.size + numbers.size + toggles.size + choices.size + colors.size + actions.size

    val label: String get() = if (tag == null) title else "$tag $title"

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
            for (toggle in folder.toggles) {
                if (!toggle.isTest) continue
                when (markOf(folder, toggle)) {
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
    const val OPACITY = "opacity"
    const val GLOW = "glow"
    const val TYPING = "typing"
    const val SCAN_LINE = "scan_line"
    const val LASER_TYPING = "laser_typing"
    const val WIDTH = "panel.width"
    const val HEIGHT = "panel.height"
    const val FULL_SCREEN = "panel.full_screen"
    const val SEE_THROUGH_FULL_SIZE = "panel.see_through_full_size"
    const val BORDER_THICKNESS = "panel.border_thickness"
    const val TICK_ON_SAVE = "panel.tick_on_save"
    const val THEME = "panel.theme"
    const val BORDER_COLOR = "panel.border_color"

    fun build(): PanelFolder {
        val top = listOf(
            cameraFolder(),
            effectsFolder(),
            recordFolder(),
            liveRemoteFolder(),
            platformsFolder(),
            offgridFolder(),
            settingsFolder(),
        )
        return PanelFolder(id = "home", title = "HOME", tag = null, kind = FolderKind.SETTINGS, folders = top)
    }

    private fun settingsFolder() = PanelFolder(
        id = "panel_settings",
        title = "Panel settings",
        tag = null,
        kind = FolderKind.SETTINGS,
        folders = listOf(
            PanelFolder(
                "size",
                "Size",
                null,
                FolderKind.SETTINGS,
                numbers = listOf(
                    NumberItem(WIDTH, "Width", "Panel width from 0 to 100 percent.", 80),
                    NumberItem(HEIGHT, "Height", "Panel height from 0 to 100 percent.", 60),
                ),
                toggles = listOf(
                    ToggleItem(FULL_SCREEN, "Full screen", "Use the full window.", false, false),
                    ToggleItem(SEE_THROUGH_FULL_SIZE, "See-through at full size", "Show the camera at full size.", false, false),
                ),
            ),
            PanelFolder(
                "display",
                "Display",
                null,
                FolderKind.SETTINGS,
                numbers = listOf(
                    NumberItem(OPACITY, "Opacity", "How solid the panel looks. 0 is clear. 100 is solid.", 50),
                    NumberItem(TYPING, "Typing speed", "How fast the laser types.", 50),
                ),
                toggles = listOf(
                    ToggleItem(SCAN_LINE, "Scan line", "A soft line sweeps across the panel.", true, false),
                    ToggleItem(LASER_TYPING, "Laser typing", "Off shows the text all at once.", true, false),
                    ToggleItem(TICK_ON_SAVE, "Tick when saved", "Play a short tick when a setting is saved.", true, false),
                ),
            ),
            PanelFolder(
                "glow_effects",
                "Glow effects",
                null,
                FolderKind.SETTINGS,
                numbers = listOf(
                    NumberItem(GLOW, "Glow", "How bright the glow lines are.", 50),
                    NumberItem(BORDER_THICKNESS, "Border thickness", "The border width from 2 to 6 dp.", 50),
                ),
            ),
            colorsFolder(),
            themesFolder(),
            simpleFolder("presets", "Presets", kind = FolderKind.SETTINGS),
            resetFolder(),
        ),
        notes = listOf(
            "Type a number from 0 to 100. Then tap Done.",
            "Green toggle means on. Red toggle means off.",
        ),
    )

    private fun cameraFolder() = PanelFolder(
        id = "camera",
        title = "Camera",
        tag = null,
        kind = FolderKind.EFFECTS,
        choices = listOf(
            ChoiceItem(FxKeys.CAMERA_MODE, "Camera mode", "Choose the camera presentation.", listOf("Normal", "Mirror"), "Normal"),
        ),
        toggles = listOf(
            ToggleItem(FxKeys.CAMERA_ON, "Camera", "Camera output is on.", true, false),
            ToggleItem(FxKeys.FACE_DIAMOND, "Face diamond", "Show the face diamond.", false, false),
            ToggleItem(FxKeys.CLOAK_VOID, "Cloak void", "Hide the face in a dark void.", false, false),
            ToggleItem(FxKeys.STATUS_LABEL, "Status label", "Show the camera status label.", false, false),
            ToggleItem(FxKeys.GHOST, "Ghost", "Show a trailing camera image.", false, false),
            ToggleItem(FxKeys.TAKEOVER, "Takeover", "Apply the takeover effect.", false, false),
            ToggleItem(FxKeys.MIRROR_SCAN, "Mirror plus scan", "Mirror the image and add a scan.", false, false),
        ),
        notes = listOf("Camera controls the picture and face effects."),
    )

    private fun recordFolder() = PanelFolder(
        id = "record",
        title = "Record",
        tag = null,
        kind = FolderKind.EFFECTS,
        toggles = listOf(ToggleItem(FxKeys.MIC_ON, "Mic", "Mic audio goes into the recording.", false, false)),
        notes = listOf("Choose what goes into a recording."),
    )

    private fun effectsFolder() = PanelFolder(
        id = "effects",
        title = "Effects",
        tag = null,
        kind = FolderKind.EFFECTS,
        folders = listOf(
            simpleFolder(
                "camera_effects",
                "Camera Effects",
                listOf(
                    ToggleItem(FxKeys.TIME_FREEZE, "Time freeze", "Freeze the camera image.", false, false),
                    ToggleItem(FxKeys.ECHO_TRAIL, "Echo trail", "Leave a trail behind movement.", false, false),
                    ToggleItem(FxKeys.DEPTH_SHIFT, "Depth shift", "Shift the image depth.", false, false),
                    ToggleItem(FxKeys.SHATTER, "Shatter", "Break the image into pieces.", false, false),
                    ToggleItem(FxKeys.QUIET_MODE, "Quiet mode", "Reduce visual movement.", false, false),
                ),
                "Camera effects change the video image.",
            ),
            simpleFolder(
                "screen_effects",
                "Screen Effects",
                listOf(
                    ToggleItem(FxKeys.SCREEN_BEACH, "Beach", "Add a beach scene.", false, false),
                    ToggleItem(FxKeys.SCREEN_CLOUDS, "Clouds", "Add moving clouds.", false, false),
                    ToggleItem(FxKeys.SCREEN_PORTAL, "Portal", "Open a portal effect.", false, false),
                    ToggleItem(FxKeys.SCREEN_OPTICAL_ILLUSIONS, "Optical illusions", "Add an optical illusion.", false, false),
                    ToggleItem(FxKeys.SCREEN_ROLLER_COASTER, "Roller coaster", "Add a moving ride scene.", false, false),
                ),
                "Screen effects change the background.",
            ),
            simpleFolder(
                "particle_effects",
                "Particle Effects",
                listOf(
                    ToggleItem(FxKeys.PARTICLE_EMOJI_RAIN, "Emoji rain", "Drop emoji across the image.", false, false),
                    ToggleItem(FxKeys.PARTICLE_FIREWORKS, "Fireworks", "Add fireworks to the image.", false, false),
                    ToggleItem(FxKeys.PARTICLE_FLYING_HEROES, "Flying heroes", "Add flying figures to the image.", false, false),
                ),
                "Particles move across the image.",
            ),
            simpleFolder(
                "light_laser_effects",
                "Light and Laser Effects",
                listOf(
                    ToggleItem(FxKeys.GLOW_BORDER, "Glow border", "Add a glowing video border.", false, false),
                    ToggleItem(FxKeys.LASER_WRITING, "Laser writing", "Write with a laser effect.", false, false),
                    ToggleItem(FxKeys.RADAR_SWEEP, "Radar sweep", "Sweep a radar line across the image.", false, false),
                    ToggleItem(FxKeys.RED_SWEEP, "Red sweep", "Sweep a red line across the image.", false, false),
                    ToggleItem(FxKeys.DRAWING, "Drawing", "Draw over the image.", false, false),
                ),
                "Light effects add lines and color.",
            ),
            microphoneEffectsFolder(),
            simpleFolder(
                "sound_effects",
                "Sound Effects",
                listOf(ToggleItem(FxKeys.SOUNDBOARD_ON, "Soundboard", "Play sound effects in the video.", false, false)),
                "Soundboard audio can be included in recordings.",
            ),
        ),
        notes = listOf("Picture, light, particle, microphone and sound effects."),
    )

    private fun microphoneEffectsFolder() = PanelFolder(
        id = "microphone_effects",
        title = "Microphone Effects",
        tag = null,
        kind = FolderKind.EFFECTS,
        numbers = listOf(
            NumberItem(FxKeys.MIC_PITCH, "Pitch", "50 is natural. Lower is deeper.", 50),
            NumberItem(FxKeys.MIC_ROBOT_AMOUNT, "Robot amount", "How strong the robot voice sounds.", 50),
            NumberItem(FxKeys.MIC_ECHO_DELAY, "Echo delay", "50 ms at 0. 800 ms at 100.", 30),
            NumberItem(FxKeys.MIC_ECHO_AMOUNT, "Echo amount", "How much echo is heard.", 40),
            NumberItem(FxKeys.MIC_CAVE_AMOUNT, "Cave amount", "How large the room sounds.", 40),
            NumberItem(FxKeys.MIC_GAIN, "Mic volume", "50 leaves the mic unchanged.", 50),
            NumberItem(FxKeys.MIC_GATE, "Noise gate", "0 turns the noise gate off.", 20),
        ),
        toggles = listOf(
            ToggleItem(FxKeys.MIC_EFFECTS_ON, "Microphone effects", "Turn microphone effects on or off.", false, false),
            ToggleItem(FxKeys.MIC_ROBOT, "Robot", "Change the voice to a robot sound.", false, false),
            ToggleItem(FxKeys.MIC_ECHO, "Echo", "Repeat the voice after a short delay.", false, false),
            ToggleItem(FxKeys.MIC_RADIO, "Radio", "Use a narrow radio sound.", false, false),
            ToggleItem(FxKeys.MIC_CAVE, "Cave", "Add a large-room echo.", false, false),
            ToggleItem(FxKeys.MIC_SOUNDS_IN_VIDEO, "Sounds in video", "Mix sound effects into recorded audio.", true, false),
            ToggleItem(FxKeys.MIC_BEAT_GLOW, "Beat glow", "Make the border follow the mic beat.", false, false),
        ),
        notes = listOf(
            "Everything runs on this phone. Nothing goes to the cloud.",
        ),
        actions = listOf(PanelActionItem(PanelAction.RESET_FOLDER, "Reset this folder", "Restore microphone effects.")),
    )

    private fun liveRemoteFolder() = PanelFolder(
        id = "live_remote",
        title = "Live and Remote",
        tag = null,
        kind = FolderKind.EFFECTS,
        toggles = listOf(
            ToggleItem(FxKeys.LIVE_OVERLAY, "Live overlay", "Show the panel over other apps.", false, false),
        ),
        choices = listOf(
            ChoiceItem(
                FxKeys.REMOTE_ROLE,
                "Remote role",
                "Choose how this phone connects.",
                listOf("off", "camera", "remote"),
                "off",
            ),
        ),
        actions = listOf(
            PanelActionItem(PanelAction.LIVE_OPEN_OVERLAY_SETTINGS, "Overlay settings", "Open overlay settings."),
            PanelActionItem(PanelAction.LIVE_CLEAN_OUTPUT, "Clean output", "Hide private panel content."),
            PanelActionItem(PanelAction.REMOTE_OPEN_PAIRING, "Remote pairing", "Open remote pairing."),
        ),
        notes = listOf("Control the live overlay and phone pairing."),
    )

    private fun resetFolder() = PanelFolder(
        id = "reset",
        title = "Reset",
        tag = null,
        kind = kind,
        actions = listOf(PanelActionItem(PanelAction.RESET_ALL, "Reset all", "Restore the start settings.")),
    )

    private fun platformsFolder() = PanelFolder(
        id = "platforms",
        title = "Platforms",
        tag = null,
        kind = FolderKind.PLATFORM,
        folders = PLATFORMS.mapIndexed { index, platform -> platformFolder(index + 1, platform) },
        notes = listOf("Platform folders keep their settings separate."),
    )

    private fun simpleFolder(
        id: String,
        title: String,
        toggles: List<ToggleItem> = emptyList(),
        description: String? = null,
        kind: FolderKind = FolderKind.EFFECTS,
    ) = PanelFolder(
        id = id,
        title = title,
        tag = null,
        kind = kind,
        toggles = toggles,
        notes = description?.let(::listOf) ?: emptyList(),
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
                    toggles = TEST_TOGGLES,
                    notes = listOf(
                        "Private test toggles. Viewers never see them.",
                        "Green means on. Red means off.",
                        "Real checks come in a later step.",
                    ),
                ),
            ),
        )
    }

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
                out.setSpan(ForegroundColorSpan(Hud.TEXT), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.setSpan(StyleSpan(Typeface.BOLD), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.append(' ')
            }
            out.append(value)
        }
        return out
    }
}
