package com.offgrid.compositor.terminal

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

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
    val guide: Guide? = null,
    val notes: List<String> = emptyList(),
) {
    var parent: PanelFolder? = null
        private set

    init {
        folders.forEach { it.parent = this }
    }

    val itemCount: Int get() = folders.size + numbers.size + toggles.size

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

    fun build(): PanelFolder {
        val top = listOf(settingsFolder(), offgridFolder()) +
            PLATFORMS.mapIndexed { index, platform -> platformFolder(index + 1, platform) }
        return PanelFolder(id = "home", title = "HOME", tag = null, kind = FolderKind.SETTINGS, folders = top)
    }

    private fun settingsFolder() = PanelFolder(
        id = "panel_settings",
        title = "Panel settings",
        tag = null,
        kind = FolderKind.SETTINGS,
        numbers = listOf(
            NumberItem(OPACITY, "Opacity", "How solid the panel looks. 0 is clear. 100 is solid.", 50),
            NumberItem(GLOW, "Glow", "How bright the glow lines are.", 70),
            NumberItem(TYPING, "Typing speed", "How fast the laser types.", 50),
        ),
        toggles = listOf(
            ToggleItem(SCAN_LINE, "Scan line", "A soft line sweeps across the panel.", true, isTest = false),
            ToggleItem(LASER_TYPING, "Laser typing", "Off shows the text all at once.", true, isTest = false),
        ),
        notes = listOf(
            "Type a number from 0 to 100. Then tap Done.",
            "Green toggle means on. Red toggle means off.",
            "No sliders here. Only typed numbers and toggles.",
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
                out.setSpan(ForegroundColorSpan(Hud.LASER_BRIGHT), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.setSpan(StyleSpan(Typeface.BOLD), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.append(' ')
            }
            out.append(value)
        }
        return out
    }
}
