package com.offgrid.compositor.terminal

import com.offgrid.compositor.effects.Fire
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.audio.voice.MicKeys
import com.offgrid.compositor.audio.voice.RexVoiceProfile

/**
 * The folders for the camera, the effects, recording, and each platform's kick tools and test
 * results (final build, M4). They live here so TerminalPanel stays focused on the panel itself.
 *
 * Every number is typed from 0 to 100. Every switch is a toggle. Never a slider.
 * The start value of each setting is right next to it.
 */
internal object EffectFolders {

    // Lane C hook
    fun liveRemote() = PanelFolder(
        id = "live_remote",
        title = "📁 Live and Remote",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            ToggleItem(FxKeys.AUTO_CURTAIN_ON, "Auto blackout when idle", "Turns the stream solid black, white, or your color when no effect or mic is on.", true, isTest = false),
            ChoiceItem(
                FxKeys.AUTO_CURTAIN_MODE,
                "Idle screen color",
                listOf(
                    Choice(FxKeys.CURTAIN_BLACK, "Solid black", "Pure black screen when idle."),
                    Choice(FxKeys.CURTAIN_WHITE, "Solid white", "Pure white screen when idle."),
                    Choice(FxKeys.CURTAIN_CUSTOM, "Custom color", "Uses the hex color below."),
                ),
                FxKeys.CURTAIN_BLACK,
            ),
            TextItem(FxKeys.AUTO_CURTAIN_HEX, "Idle color hex", "Type a 6-character color like #000000 or #FFFFFF.", "#000000", 7),
            ToggleItem(FxKeys.LIVE_OVERLAY, "Floating overlay", "Show the small OFFGRID controls above other apps.", false, isTest = false),
            ActionItem(PanelAction.LIVE_OPEN_OVERLAY_SETTINGS, "Overlay settings", "Allow OFFGRID to draw its small controls over other apps."),
            ActionItem(PanelAction.LIVE_CLEAN_OUTPUT, "Clean screen share", "Hide the record bar and shrink the panel to its bubble."),
            ChoiceItem(
                FxKeys.REMOTE_ROLE,
                "Remote role",
                listOf(
                    Choice(FxKeys.REMOTE_ROLE_OFF, "Off", "Turn off the two-phone link."),
                    Choice(FxKeys.REMOTE_ROLE_CAMERA, "Camera phone", "Send the finished picture to a paired phone."),
                    Choice(FxKeys.REMOTE_ROLE_REMOTE, "Remote phone", "Show a paired camera phone's finished picture."),
                ),
                FxKeys.REMOTE_ROLE_OFF,
            ),
            ActionItem(PanelAction.REMOTE_OPEN_PAIRING, "Pairing", "Make, paste, forget, or replace a secure local key."),
            ActionItem(PanelAction.REMOTE_OPEN_PREVIEW, "Remote preview", "Connect to the camera phone on this Wi-Fi network."),
            ToggleItem(FxKeys.LIVE_PC_ON, "Send to PC", "Send the finished picture to OBS on your local network.", false, isTest = false),
            ChoiceItem(
                FxKeys.LIVE_PC_PROTOCOL,
                "PC protocol",
                listOf(
                    Choice("srt", "SRT", "Send an encrypted SRT stream to a listener on your PC."),
                    Choice("rtmp", "RTMP", "Push to an RTMP server running on your PC."),
                ),
                "srt",
            ),
            TextItem(FxKeys.LIVE_PC_HOST, "PC LAN address", "Type the PC's local IP address. Public addresses are refused.", "", 45),
            TextItem(FxKeys.LIVE_PC_PORT, "PC port", "Type a port from 1 to 65535. SRT starts at 9000.", "9000", 5),
            ChoiceItem(
                FxKeys.LIVE_PC_RES,
                "PC video size",
                listOf(
                    Choice("720p", "720p", "Send 1280 by 720 video."),
                    Choice("1080p", "1080p", "Send 1920 by 1080 video."),
                ),
                "720p",
            ),
            TextItem(FxKeys.LIVE_PC_BITRATE, "PC video rate", "Type a video rate from 1,000,000 to 12,000,000 bits per second.", "6000000", 8),
            ActionItem(PanelAction.LIVE_SEND_TO_PC, "Start PC output", "Send the finished picture to OBS on this local network."),
        ),
        notes = listOf(
            "The controls and overlay never go into the finished picture.",
            "Collapse the panel to its bubble before sharing your screen.",
            "Normal Bigo camera live shows Bigo's camera, not OFFGRID's effects.",
            "For the PC route, OBS receives the finished picture over SRT or RTMP.",
            "Check Bigo and TikTok rules. Never fake viewers, gifts, or engagement.",
        ),
    )

    // ---- 📁 Camera: the no-face modes (features 9 to 12, 34) ----

    fun camera() = PanelFolder(
        id = "camera",
        title = "📁 Camera",
        tag = null,
        kind = FolderKind.EFFECTS,
        folders = listOf(
            faceDiamond(),
            bodyMask(),
            cloak(),
            statusLabel(),
            ghost(),
            takeover(),
            mirrorScan(),
        ),
        items = listOf(
            ToggleItem(FxKeys.CAMERA_ON, "Camera", "Turn the camera on or off. It starts on and sends camera video.", true, isTest = false),
            ChoiceItem(
                FxKeys.CAMERA_MODE,
                "Camera mode",
                listOf(
                    Choice(FxKeys.MODE_LIVE, "Live camera", "Viewers see your camera, with your effects on top."),
                    Choice(FxKeys.MODE_BLACKOUT, "Blackout", "Viewers see a solid black box."),
                    Choice(FxKeys.MODE_CLOAK, "Cloak void", "Viewers see empty space, like a dark tunnel into nothing."),
                    Choice(FxKeys.MODE_STATUS, "Status label", "Viewers see one word, like CLOAKED, on black."),
                    Choice(FxKeys.MODE_GHOST, "Ghost", "Viewers see the last picture, held still, or a blank box."),
                ),
                default = FxKeys.MODE_LIVE,
            ),
        ),
        notes = listOf(
            "Pick what viewers see. It is baked into the video.",
            "Use the top Camera toggle to stop or start camera video.",
            "Blackout, Cloak void, Status label, and Ghost never show your face.",
            "Body mask can cut your body out of the box. If the mask drops, the box goes black.",
            "Reset never changes the camera mode or the body look, so your face never shows by surprise.",
        ),
    )

    private fun bodyMask() = PanelFolder(
        id = "camera.body",
        title = "📁 Body mask",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            ChoiceItem(
                FxKeys.BODY_MODE,
                "Body look",
                listOf(
                    Choice(FxKeys.BODY_OFF, "Off", "Viewers see you, with your other effects."),
                    Choice(FxKeys.BODY_SEE, "See-through", "Your body is cut out of the box. The room around you stays."),
                    Choice(FxKeys.BODY_REPLACE, "Replace", "Your body is swapped for black, or for the void."),
                    Choice(FxKeys.BODY_CLOAK, "Body cloak", "The void covers your silhouette. The room stays."),
                    Choice(FxKeys.BODY_HOLO, "Hologram", "Your silhouette becomes light. Your face colors are not shown."),
                ),
                default = FxKeys.BODY_OFF,
            ),
            ChoiceItem(
                FxKeys.BODY_PLATE,
                "Replacement",
                listOf(
                    Choice(FxKeys.BODY_PLATE_BLACK, "Black", "Replace fills your body with black."),
                    Choice(FxKeys.BODY_PLATE_VOID, "Void", "Replace fills your body with the empty void."),
                ),
                default = FxKeys.BODY_PLATE_BLACK,
            ),
            NumberItem(
                FxKeys.BODY_EDGE,
                "Edge",
                "How sharp the cut is. 0 is soft. 100 is a hard edge.",
                40,
            ),
        ),
        notes = listOf(
            "Your phone finds your body with MediaPipe. The picture stays on the phone.",
            "Inside the video box, your body can be see-through or replaced.",
            "If the mask is missing or too old, the whole box goes black, so your face never shows.",
            "Reset does not turn the body look off.",
        ),
    )

    fun microphone() = PanelFolder(
        id = "microphone",
        title = "📁 Microphone Effects",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            ToggleItem(FxKeys.MIC_ON, "Mic", "Record microphone audio. It starts off.", false, isTest = false),
            ToggleItem(FxKeys.MIC_FX_ON, "Microphone Effects", "Turn the voice effects on or off.", false, isTest = false),
            NumberItem(FxKeys.MIC_PITCH, "Pitch", "50 keeps your natural pitch.", 50),
            ToggleItem(FxKeys.MIC_ROBOT_ON, "Robot", "Give your voice a robot sound.", false, isTest = false),
            NumberItem(FxKeys.MIC_ROBOT_AMOUNT, "Robot amount", "0 is natural. 100 is fully robotic.", 50),
            ToggleItem(FxKeys.MIC_ECHO_ON, "Echo", "Add an echo to your voice.", false, isTest = false),
            NumberItem(FxKeys.MIC_ECHO_DELAY, "Echo delay", "Set the time before the echo.", 50),
            NumberItem(FxKeys.MIC_ECHO_AMOUNT, "Echo amount", "Set how much the echo repeats.", 50),
            ToggleItem(FxKeys.MIC_RADIO_ON, "Radio", "Give your voice a radio sound.", false, isTest = false),
            ToggleItem(FxKeys.MIC_CAVE_ON, "Cave", "Add a cave echo to your voice.", false, isTest = false),
            NumberItem(FxKeys.MIC_CAVE_AMOUNT, "Cave amount", "0 is light. 100 is deep.", 50),
            NumberItem(FxKeys.MIC_GAIN, "Mic volume", "0 is silent. 50 is natural. 100 is loud.", 50),
            NumberItem(FxKeys.MIC_GATE, "Noise gate", "0 turns the gate off. Higher values cut quiet sounds.", 0),
            ToggleItem(FxKeys.MIC_SOUNDS_IN_VIDEO, "Sounds in video", "Include soundboard audio in video recordings.", true, isTest = false),
            ToggleItem(FxKeys.MIC_BEAT_GLOW, "Beat glow", "Let the audio pulse the glow border.", false, isTest = false),
            ToggleItem(MicKeys.BLOCKED, "Hard block", "Stops capture and prevents anything from reopening the mic.", false, isTest = false),
            ToggleItem(MicKeys.VOICE_SWAP, "Rex voice swap", "On-device pitch, formant color, and timbre EQ. No model or upload.", false, isTest = false),
            ToggleItem(MicKeys.BYPASS, "Bypass", "Passes clean, unprocessed voice through the safe output path.", false, isTest = false),
            NumberItem(MicKeys.WET, "Wet / dry", "0 is clean voice. 100 is the full Rex effect.", RexVoiceProfile.WET_PERCENT),
            NumberItem(MicKeys.PITCH, "Rex pitch", "50 is unchanged. 0 is lower. 100 is higher.", RexVoiceProfile.pitchPercent),
            NumberItem(MicKeys.FORMANT, "Formant shift", "50 is unchanged. Move the voice color lower or higher.", RexVoiceProfile.formantPercent),
            NumberItem(MicKeys.LOW_EQ, "Low EQ", "50 is unchanged. 0 cuts lows. 100 boosts lows.", RexVoiceProfile.lowPercent),
            NumberItem(MicKeys.MID_EQ, "Mid EQ", "50 is unchanged. 0 cuts mids. 100 boosts mids.", RexVoiceProfile.midPercent),
            NumberItem(MicKeys.HIGH_EQ, "High EQ", "50 is unchanged. 0 cuts highs. 100 boosts highs.", RexVoiceProfile.highPercent),
        ),
        notes = listOf(
            "Everything runs on this phone. Nothing goes to the cloud.",
            "Rex voice swap aims at Rex 01, Rex 02, and Rex 03. Rex.wav is not in this project. Pitch, formant, and EQ stay tunable.",
            "Capture begins only after you turn the mic on and allow microphone access.",
            "Hard block releases the recorder. Unblock does not turn the mic back on.",
            "Audio processing stays on this phone. No audio is uploaded.",
        ),
    )

    private fun faceDiamond() = PanelFolder(
        id = "camera.diamond",
        title = "📁 Face diamond",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            ToggleItem(FxKeys.DIAMOND_ON, "Face diamond", "A solid diamond covers your face in the video. It starts on.", true, isTest = false),
            ChoiceItem(
                FxKeys.DIAMOND_STYLE,
                "Diamond look",
                listOf(
                    Choice(FxKeys.DIAMOND_CLASSIC, "Classic", "Hot pink with a yellow middle, like the Step 1 test shape."),
                    Choice(FxKeys.DIAMOND_LASER, "Laser", "Black with a bright white glowing edge."),
                    Choice(FxKeys.DIAMOND_BLACK, "Solid black", "Plain black. Nothing shows through."),
                ),
                default = FxKeys.DIAMOND_CLASSIC,
            ),
            ToggleItem(FxKeys.DIAMOND_FOLLOW, "Follow my face", "The diamond moves with your face. Off keeps it in one place.", true, isTest = false),
            NumberItem(FxKeys.DIAMOND_SIZE, "Size", "How big the diamond is when it stays in one place. 0 is small. 100 is huge.", 60),
            NumberItem(FxKeys.DIAMOND_X, "Across", "Where it sits when it stays in one place. 0 is the left edge. 100 is the right edge.", 50),
            NumberItem(FxKeys.DIAMOND_Y, "Down", "Where it sits when it stays in one place. 0 is the top. 100 is the bottom.", 38),
            ToggleItem(
                FxKeys.FACE_FLIP_X,
                "Flip left and right",
                "Turn on if the diamond moves the wrong way when you move.",
                false,
                isTest = false,
            ),
        ),
        notes = listOf(
            "The diamond is solid, so no face shows through it.",
            "Your phone finds your face by itself. Face data stays on the phone.",
            "If no face is found, the diamond waits where you put it.",
        ),
    )

    private fun cloak() = PanelFolder(
        id = "camera.cloak",
        title = "📁 Cloak void",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            NumberItem(FxKeys.CLOAK_STRENGTH, "Strength", "How dark and empty the void is. 100 is the emptiest.", 60),
            NumberItem(FxKeys.CLOAK_FADE, "Fade time", "How long the cloak takes to cover you. 0 is a blink. 100 is 3 seconds.", 30),
            NumberItem(FxKeys.CLOAK_SHIMMER, "Shimmer", "How much the picture ripples as the cloak turns on and off.", 60),
        ),
        notes = listOf(
            "Pick Cloak void in Camera mode to turn it on.",
            "Viewers see empty space, not you and not your room.",
        ),
    )

    private fun statusLabel() = PanelFolder(
        id = "camera.status",
        title = "📁 Status label",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            TextItem(FxKeys.STATUS_TEXT, "Status word", "The word viewers see, like CLOAKED or INVISIBLE.", "CLOAKED", STATUS_MAX),
            NumberItem(FxKeys.STATUS_SIZE, "Word size", "0 is small. 100 fills the width.", 60),
            NumberItem(FxKeys.STATUS_FADE, "Fade time", "How long the word takes to show. 0 is a blink. 100 is 3 seconds.", 20),
        ),
        notes = listOf("Pick Status label in Camera mode to turn it on."),
    )

    private fun ghost() = PanelFolder(
        id = "camera.ghost",
        title = "📁 Ghost",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            ChoiceItem(
                FxKeys.GHOST_STYLE,
                "Ghost shows",
                listOf(
                    Choice(FxKeys.GHOST_FROZEN, "Last picture", "The last picture viewers saw, held still, like a frozen stream."),
                    Choice(FxKeys.GHOST_BLANK, "Blank box", "A plain black box."),
                ),
                default = FxKeys.GHOST_FROZEN,
            ),
        ),
        notes = listOf(
            "Pick Ghost in Camera mode to turn it on. GHOST ON shows on your screen.",
            "Video audio uses the mic when Mic is on, with Microphone Effects applied. Soundboard sounds are included when Sounds in video is on.",
            "The mic stays off unless you turn it on in Microphone Effects.",
            "If ghost was already on when the app opened, the box stays blank.",
        ),
    )

    private fun takeover() = PanelFolder(
        id = "camera.takeover",
        title = "📁 Takeover",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            ToggleItem(FxKeys.TAKEOVER_ON, "Takeover", "You fill the whole box, zoomed in on your face.", false, isTest = false),
            NumberItem(FxKeys.TAKEOVER_ZOOM, "Zoom", "How close it zooms in. 0 is no zoom. 100 is 2.5 times.", 40),
            ToggleItem(FxKeys.TAKEOVER_INSET, "Small box", "A small box in the corner shows the other picture.", true, isTest = false),
            NumberItem(FxKeys.TAKEOVER_INSET_SIZE, "Small box size", "0 is a fifth of the width. 100 is almost half.", 40),
            ToggleItem(FxKeys.TAKEOVER_FLIP, "Flip-flop", "Swaps the big box and the small box.", false, isTest = false),
        ),
        notes = listOf(
            "This works inside your own video. Other apps draw their own boxes on each viewer's phone.",
            "The big box shows you zoomed in. The small box shows your whole camera.",
        ),
    )

    private fun mirrorScan() = PanelFolder(
        id = "camera.mirror",
        title = "📁 Mirror plus scan",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            ToggleItem(FxKeys.MIRROR_ON, "Mirror plus scan", "Flips your video like a mirror and a radar scans your face.", false, isTest = false),
            NumberItem(FxKeys.MIRROR_SPEED, "Scan speed", "0 is slow. 100 is fast.", 40),
            NumberItem(FxKeys.MIRROR_BLIPS, "Blips", "How many blips light up. 0 is a few. 100 is many.", 40),
        ),
        notes = listOf(
            "A small SCAN ON note shows while it runs.",
            "On OFFGRID calls it will work on the other person's video too. Calls are not built yet.",
        ),
    )

    // ---- 📁 Effects: one folder per effect ----

    fun effects() = PanelFolder(
        id = "effects",
        title = "📁 Effects",
        tag = null,
        kind = FolderKind.EFFECTS,
        folders = listOf(
            emojiRain(),
            drawing(),
            glowBorder(),
            fireworks(),
            portal(),
            heroes(),
            coaster(),
            clouds(),
            beach(),
            timeFreeze(),
            illusions(),
            redSweep(),
            depthShift(),
            laserWriting(),
            radar(),
            echo(),
            quiet(),
            shatter(),
            soundboard(),
        ),
        notes = listOf(
            "Every effect is baked into the video viewers see.",
            "The panel is never in the video.",
            "Nothing flashes more than 3 times a second.",
        ),
    )

    private fun emojiRain() = effect(
        "emoji", "📁 Emoji rain",
        ToggleItem(FxKeys.EMOJI_ON, "Emoji rain", "Emojis fall down over your video.", false, isTest = false),
        NumberItem(FxKeys.EMOJI_DENSITY, "How many", "0 is a few. 100 is a storm.", 40),
        NumberItem(FxKeys.EMOJI_SPEED, "Speed", "0 is slow. 100 is fast.", 40),
        NumberItem(FxKeys.EMOJI_SIZE, "Size", "0 is small. 100 is big.", 45),
        TextItem(FxKeys.EMOJI_SET, "Emojis", "Type or paste the emojis to rain.", "❤️🔥✨😂🎉💎", EMOJI_MAX),
    )

    private fun drawing() = effect(
        "ink", "📁 Drawing",
        ToggleItem(FxKeys.INK_ON, "Draw on the video", "Draw with your finger on the camera picture.", false, isTest = false),
        NumberItem(FxKeys.INK_SIZE, "Line size", "0 is thin. 100 is thick.", 30),
        NumberItem(FxKeys.INK_GLOW, "Glow", "0 is a plain line. 100 is a bright neon glow.", 60),
        NumberItem(FxKeys.INK_HUE, "Color", "Goes around the rainbow. 0 is red. 33 is green. 50 is teal. 66 is blue.", 50),
        ToggleItem(FxKeys.INK_PIN, "Pin to my face", "Your drawing moves with your face. Off keeps it in place.", true, isTest = false),
        ActionItem(PanelAction.CLEAR_INK, "Clear drawing", "Wipes all your drawing off the video."),
        notes = listOf(
            "Tap − to make the panel small, then draw on the camera picture.",
            "Pinned lines stay on your face as you move.",
        ),
    )

    private fun glowBorder() = effect(
        "border", "📁 Glow border",
        ToggleItem(FxKeys.BORDER_ON, "Glow border", "A band of light travels around the edge of your video.", false, isTest = false),
        NumberItem(FxKeys.BORDER_SPEED, "Speed", "0 is slow. 100 is fast.", 35),
        NumberItem(FxKeys.BORDER_THICKNESS, "Thickness", "0 is thin. 100 is thick.", 30),
        NumberItem(FxKeys.BORDER_GLOW, "Glow", "0 is soft. 100 is big and bright.", 60),
        ChoiceItem(
            FxKeys.BORDER_COLOR,
            "Color",
            listOf(
                Choice(FxKeys.COLOR_PANEL, "Panel glow color", "The same color as your panel's glow."),
                Choice(FxKeys.COLOR_WHITE, "White", "Bright white."),
                Choice(FxKeys.COLOR_RAINBOW, "Rainbow", "Colors run along the band."),
                Choice(FxKeys.COLOR_HUE, "Pick a color", "The color number below."),
            ),
            default = FxKeys.COLOR_PANEL,
        ),
        NumberItem(FxKeys.BORDER_HUE, "Color number", "Used with Pick a color. 0 is red. 33 is green. 66 is blue.", 50),
    )

    private fun fireworks() = effect(
        "fireworks", "📁 Fireworks",
        ToggleItem(FxKeys.FIREWORKS_ON, "Fireworks on flips", "Fireworks burst whenever you flip a toggle.", false, isTest = false),
        NumberItem(FxKeys.FIREWORKS_SIZE, "Size", "0 is small bursts. 100 is huge.", 55),
        NumberItem(FxKeys.FIREWORKS_COUNT, "How many", "0 is one burst. 100 is eight.", 40),
        NumberItem(FxKeys.FIREWORKS_DURATION, "How long", "0 is under a second. 100 is 4 seconds.", 50),
        fire(Fire.FIREWORKS, "Fire now", "Bursts fireworks once, right now."),
        notes = listOf("Gifts can't be seen by this app yet, so a gift can't fire them."),
    )

    private fun portal() = effect(
        "portal", "📁 Portal",
        ToggleItem(FxKeys.PORTAL_ON, "Portal", "A swirling portal opens in your video.", false, isTest = false),
        ChoiceItem(
            FxKeys.PORTAL_STYLE,
            "Portal look",
            listOf(
                Choice(FxKeys.PORTAL_SCENE, "Window", "A glowing ring with another place inside."),
                Choice(FxKeys.PORTAL_WORMHOLE, "Wormhole", "A deep spinning tunnel."),
                Choice(FxKeys.PORTAL_VOID, "Black hole", "A dark hole with a shimmering edge."),
            ),
            default = FxKeys.PORTAL_SCENE,
        ),
        NumberItem(FxKeys.PORTAL_SIZE, "Size", "0 is small. 100 is huge.", 45),
        NumberItem(FxKeys.PORTAL_SPIN, "Spin", "0 is slow. 100 is fast.", 40),
        NumberItem(FxKeys.PORTAL_X, "Across", "0 is the left edge. 100 is the right edge.", 50),
        NumberItem(FxKeys.PORTAL_Y, "Down", "0 is the top. 100 is the bottom.", 50),
        NumberItem(FxKeys.PORTAL_GLOW, "Glow", "0 is soft. 100 is bright.", 60),
    )

    private fun heroes() = effect(
        "heroes", "📁 Flying heroes",
        ToggleItem(FxKeys.HEROES_ON, "Flying heroes", "Original OFFGRID heroes fly across your video.", false, isTest = false),
        ChoiceItem(
            FxKeys.HEROES_HERO,
            "Hero",
            listOf(
                Choice(FxKeys.HERO_NOVA, "Nova", "A caped flyer in teal and white with a star."),
                Choice(FxKeys.HERO_VOLT, "Volt", "A lightning-fast hero in yellow and black."),
                Choice(FxKeys.HERO_COMET, "Comet", "A glowing round helmet and a fiery trail."),
                Choice(FxKeys.HERO_PIXEL, "Pixel", "A blocky retro game hero in pink and blue."),
            ),
            default = FxKeys.HERO_NOVA,
        ),
        NumberItem(FxKeys.HEROES_COUNT, "How many", "0 is one hero. 100 is six.", 20),
        NumberItem(FxKeys.HEROES_SIZE, "Size", "0 is small. 100 is big.", 40),
        NumberItem(FxKeys.HEROES_SPEED, "Speed", "0 is slow. 100 is fast.", 40),
    )

    private fun coaster() = effect(
        "coaster", "📁 Roller coaster",
        ToggleItem(FxKeys.COASTER_ON, "Roller coaster", "A coaster rides through your video.", false, isTest = false),
        ChoiceItem(
            FxKeys.COASTER_VIEW,
            "View",
            listOf(
                Choice(FxKeys.COASTER_WATCH, "Watch it", "The track and cars ride across the bottom of your video."),
                Choice(FxKeys.COASTER_RIDE, "Ride it", "You ride in the front car. It fills the whole box."),
            ),
            default = FxKeys.COASTER_WATCH,
        ),
        NumberItem(FxKeys.COASTER_SPEED, "Speed", "0 is slow. 100 is fast.", 40),
        NumberItem(FxKeys.COASTER_SIZE, "Size", "0 is small. 100 is big.", 50),
        NumberItem(FxKeys.COASTER_CARS, "Cars", "0 is one car. 100 is eight.", 40),
    )

    private fun clouds() = effect(
        "clouds", "📁 Clouds",
        ToggleItem(FxKeys.CLOUDS_ON, "Flying through clouds", "You fly through the sky.", false, isTest = false),
        ChoiceItem(
            FxKeys.CLOUDS_PLACE,
            "Where",
            listOf(
                Choice(FxKeys.CLOUDS_FULL, "Whole box", "Only sky and clouds. You are not seen."),
                Choice(FxKeys.CLOUDS_AROUND, "Around me", "Clouds drift by the edges. You stay in the middle."),
            ),
            default = FxKeys.CLOUDS_FULL,
        ),
        NumberItem(FxKeys.CLOUDS_SPEED, "Speed", "0 is slow. 100 is fast.", 40),
        NumberItem(FxKeys.CLOUDS_THICKNESS, "Thickness", "0 is a few thin clouds. 100 is thick.", 50),
        NumberItem(FxKeys.CLOUDS_HEIGHT, "Height", "0 is low. 100 is high in a deep blue sky.", 50),
    )

    private fun beach() = effect(
        "beach", "📁 Beach",
        ToggleItem(FxKeys.BEACH_ON, "Beach", "An ocean beach fills your whole box. You are not seen.", false, isTest = false),
        NumberItem(FxKeys.BEACH_WAVES, "Waves", "0 is calm. 100 is big waves.", 50),
        NumberItem(FxKeys.BEACH_BRIGHTNESS, "Brightness", "0 is dusk. 100 is bright noon.", 60),
    )

    private fun timeFreeze() = effect(
        "freeze", "📁 Time freeze",
        NumberItem(FxKeys.FREEZE_LENGTH, "Freeze length", "0 is half a second. 100 is 10 seconds.", 30),
        NumberItem(FxKeys.FREEZE_GLITCH, "Glitch", "A short glitch on the way in and out. 0 is none.", 50),
        fire(Fire.TIME_FREEZE, "Freeze now", "Holds your video still, then picks back up like a glitch."),
        notes = listOf(
            "Viewers see a paused picture, not a dropped stream.",
            "Video audio uses the mic when Mic is on, with Microphone Effects applied on this phone. Soundboard sounds are included when Sounds in video is on.",
            "Mic audio follows the Microphone Effects settings.",
        ),
    )

    private fun illusions() = effect(
        "illusion", "📁 Optical illusions",
        ToggleItem(FxKeys.ILLUSION_ON, "Optical illusion", "An impossible shape or moving pattern in your video.", false, isTest = false),
        ChoiceItem(
            FxKeys.ILLUSION_KIND,
            "Illusion",
            listOf(
                Choice(FxKeys.ILLUSION_TUNNEL, "Tunnel", "Rings that seem to pull into the middle."),
                Choice(FxKeys.ILLUSION_TRIANGLE, "Impossible triangle", "A triangle that can't exist."),
                Choice(FxKeys.ILLUSION_STAIRS, "Endless stairs", "Stairs that climb forever."),
                Choice(FxKeys.ILLUSION_DRIFT, "Drift", "A pattern that seems to turn when it is still."),
            ),
            default = FxKeys.ILLUSION_TUNNEL,
        ),
        NumberItem(FxKeys.ILLUSION_SIZE, "Size", "0 is small. 100 is big.", 60),
        NumberItem(FxKeys.ILLUSION_SPEED, "Speed", "0 is slow. 100 is fast.", 30),
        NumberItem(FxKeys.ILLUSION_STRENGTH, "Strength", "0 is faint. 100 is bold.", 50),
        notes = listOf("Illusions move smoothly and never flash."),
    )

    private fun redSweep() = effect(
        "sweep", "📁 Red sweep",
        NumberItem(FxKeys.SWEEP_SPEED, "Roll speed", "0 is slow. 100 is fast.", 50),
        NumberItem(FxKeys.SWEEP_WIDTH, "Width", "How tall the carpet band is. 0 is thin. 100 is most of the box.", 60),
        NumberItem(FxKeys.SWEEP_LENGTH, "How long", "0 is 2 seconds. 100 is 20 seconds.", 70),
        fire(Fire.RED_SWEEP, "Sweep now", "Rolls the red carpet across your video once."),
        notes = listOf("Each platform folder has its own Red sweep on kick toggle."),
    )

    private fun depthShift() = effect(
        "depth", "📁 Depth shift",
        NumberItem(FxKeys.DEPTH_PUSH, "Push distance", "How far you come out toward the viewer.", 50),
        NumberItem(FxKeys.DEPTH_SPEED, "Push speed", "0 is slow. 100 is fast.", 50),
        NumberItem(FxKeys.DEPTH_HOLD, "Hold time", "0 is a blink. 100 is 3 seconds.", 30),
        NumberItem(FxKeys.DEPTH_SNAP, "Snap back speed", "0 is slow. 100 is fast.", 60),
        fire(Fire.DEPTH_SHIFT, "Push out now", "Your video pops out of a frame toward the viewer, then snaps back."),
    )

    private fun laserWriting() = effect(
        "write", "📁 Laser writing",
        TextItem(FxKeys.WRITE_TEXT, "Words", "What the laser writes in your video.", "OFFGRID", WRITE_MAX),
        NumberItem(FxKeys.WRITE_SPEED, "Writing speed", "0 is slow. 100 is fast.", 50),
        NumberItem(FxKeys.WRITE_SIZE, "Size", "0 is small. 100 is big.", 50),
        NumberItem(FxKeys.WRITE_GLOW, "Glow", "0 is soft. 100 is bright.", 70),
        NumberItem(FxKeys.WRITE_STAY, "Stay time", "How long the words stay. 0 is 1 second. 100 is 15 seconds.", 30),
        NumberItem(FxKeys.WRITE_HUE, "Color", "Goes around the rainbow. 50 is teal.", 50),
        fire(Fire.LASER_WRITE, "Write now", "The laser writes your words in your video."),
    )

    private fun radar() = effect(
        "radar", "📁 Radar sweep",
        ToggleItem(FxKeys.RADAR_ON, "Radar sweep", "A radar beam turns around your video and pings as it goes.", false, isTest = false),
        ChoiceItem(
            FxKeys.RADAR_MODE,
            "Radar mode",
            listOf(
                Choice(FxKeys.RADAR_FACE, "Face", "Blips lock onto your face."),
                Choice(FxKeys.RADAR_ROOM, "Room", "Blips scatter around the room. No face needed."),
            ),
            default = FxKeys.RADAR_FACE,
        ),
        NumberItem(FxKeys.RADAR_SPEED, "Speed", "0 is slow. 100 is fast.", 40),
        NumberItem(FxKeys.RADAR_SIZE, "Size", "0 is small. 100 fills the box.", 70),
        NumberItem(FxKeys.RADAR_GLOW, "Glow", "0 is soft. 100 is bright.", 60),
        NumberItem(FxKeys.RADAR_HUE, "Color", "Goes around the rainbow. 33 is green.", 35),
        notes = listOf("The ping plays on your phone when the soundboard is on. Face data stays on the phone."),
    )

    private fun echo() = effect(
        "echo", "📁 Echo trail",
        ToggleItem(FxKeys.ECHO_ON, "Echo trail", "Fading copies trail behind you as you move.", false, isTest = false),
        NumberItem(FxKeys.ECHO_LENGTH, "Trail length", "0 is a quarter second. 100 is 2.5 seconds.", 33),
        NumberItem(FxKeys.ECHO_COPIES, "Copies", "0 is 2 copies. 100 is 8.", 50),
        NumberItem(FxKeys.ECHO_FADE, "Strength", "0 is faint copies. 100 is strong.", 50),
        notes = listOf("Things that stand still stay sharp. Only movement leaves a trail."),
    )

    private fun quiet() = effect(
        "quiet", "📁 Quiet mode",
        NumberItem(FxKeys.QUIET_DARK, "Dark time", "How long it stays dark. 0 is half a second. 100 is 5 seconds.", 30),
        NumberItem(FxKeys.QUIET_FADE, "Fade speed", "0 is a slow fade. 100 is fast.", 50),
        NumberItem(FxKeys.QUIET_FLICKERS, "Flickers", "How many times the light flickers. 0 is once. 100 is six.", 20),
        NumberItem(FxKeys.QUIET_LIGHT, "Light size", "0 is tiny. 100 is big.", 30),
        ChoiceItem(
            FxKeys.QUIET_THEN,
            "Then fire",
            listOf(
                Choice(FxKeys.THEN_NOTHING, "Nothing", "The picture just comes back."),
                Choice(FxKeys.THEN_FIREWORKS, "Fireworks", "Fireworks burst when the light comes on."),
                Choice(FxKeys.THEN_SWEEP, "Red sweep", "The red carpet rolls in when the light comes on."),
                Choice(FxKeys.THEN_SHATTER, "Shatter", "The box shatters when the light comes on."),
                Choice(FxKeys.THEN_WRITE, "Laser writing", "The laser writes your words when the light comes on."),
            ),
            default = FxKeys.THEN_NOTHING,
        ),
        fire(Fire.QUIET, "Go quiet now", "The box goes still and dark, then a small light flickers on."),
    )

    private fun shatter() = effect(
        "shatter", "📁 Shatter",
        NumberItem(FxKeys.SHATTER_PIECES, "Pieces", "0 is 8 pieces. 100 is 80.", 40),
        NumberItem(FxKeys.SHATTER_FALL, "Fall speed", "0 is slow. 100 is fast.", 50),
        NumberItem(FxKeys.SHATTER_BROKEN, "Broken time", "How long it stays broken. 0 is a blink. 100 is 5 seconds.", 30),
        NumberItem(FxKeys.SHATTER_BACK, "Rebuild speed", "0 is slow. 100 is fast.", 50),
        ChoiceItem(
            FxKeys.SHATTER_BEHIND,
            "Behind the glass",
            listOf(
                Choice(FxKeys.BEHIND_BLACK, "Black", "Plain black behind the falling pieces."),
                Choice(FxKeys.BEHIND_VOID, "Cloak void", "Empty space behind the pieces."),
                Choice(FxKeys.BEHIND_BEACH, "Beach", "An ocean beach behind the pieces."),
            ),
            default = FxKeys.BEHIND_BLACK,
        ),
        fire(Fire.SHATTER, "Shatter now", "Your video cracks like glass, falls apart, then rebuilds."),
    )

    /** Feature 6. Sounds play on Royalty's phone only. Each effect has its own sound folder. */
    private fun soundboard() = PanelFolder(
        id = "effects.sound",
        title = "📁 Soundboard",
        tag = null,
        kind = FolderKind.EFFECTS,
        folders = SOUND_EFFECTS.map { (key, title) ->
            PanelFolder(
                id = "effects.sound.$key",
                title = "📁 $title sound",
                tag = null,
                kind = FolderKind.EFFECTS,
                items = listOf(
                    ChoiceItem(FxKeys.SOUND_PREFIX + key, "$title sound", SOUNDS, default = SOUND_START[key] ?: SOUND_OFF),
                    ActionItem(PanelAction.TRY_SOUND, "Try it", "Plays this sound once on your phone.", key),
                ),
                notes = listOf("Pick Off to mute just this effect."),
            )
        },
        items = listOf(
            ToggleItem(FxKeys.SOUND_ON, "Soundboard", "Each effect plays its own sound. It starts off.", false, isTest = false),
            NumberItem(FxKeys.SOUND_VOLUME, "Volume", "0 is silent. 100 is loud.", 60),
        ),
        notes = listOf(
            "Sounds play from your phone's speaker only.",
            "Sounds in video, in Microphone Effects, also puts these sounds in the recording.",
            "Voice audio follows the Microphone Effects settings.",
        ),
    )

    // ---- 📁 Record ----

    fun record() = PanelFolder(
        id = "record",
        title = "📁 Record",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            NumberItem(FxKeys.RECORD_SECONDS, "Record length", "Seconds per video. 0 keeps going until you tap Stop.", 30),
            ToggleItem(FxKeys.RECORD_GALLERY, "Save to gallery", "Each new video is also saved to your phone's gallery.", true, isTest = false),
            ActionItem(PanelAction.SAVE_TO_GALLERY, "Save last video to gallery", "Puts your last video in the gallery, in Movies › OFFGRID."),
            ToggleItem(FxKeys.REPLAY_ON, "Replay buffer", "Keeps the last moments of your video, ready to save. It starts off.", false, isTest = false),
            NumberItem(FxKeys.REPLAY_SECONDS, "Replay length", "Seconds to keep. 5 is the least. 100 is the most.", 30),
            ActionItem(PanelAction.SAVE_REPLAY, "Save replay", "Saves the kept moments as a video in your gallery."),
        ),
        notes = listOf(
            "Videos have your effects in them. The panel is never in them.",
            "The replay keeps audio while the mic is on. The radar ping is mixed into the video.",
            "Video audio uses the mic when Mic is on, with Microphone Effects and Rex voice swap applied.",
            "Soundboard sounds are included when Sounds in video is on.",
            "Hard block releases the mic. Processed voice can also be read from the voice output hook.",
            "Videos and voice processing stay on your phone. Nothing uploads.",
        ),
    )

    // ---- Each platform folder: kick tools, safe start, and test results ----

    /** The Effects folder inside a platform folder (features 21, 28, 29). */
    fun platformEffects(id: String, title: String) = PanelFolder(
        id = "$id.effects",
        title = "Effects",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            ToggleItem(
                FxKeys.SWEEP_GO_PREFIX + id,
                "Red sweep on kick",
                "When you tap I got kicked, a red carpet sweeps across your video.",
                true,
                isTest = false,
            ),
            ActionItem(PanelAction.KICKED, "I got kicked", "Tells you what to do next. It never rejoins for you.", id),
            ActionItem(PanelAction.OPEN_PLATFORM, "Open $title", "Opens $title, so you can ask to join again yourself.", id),
            ActionItem(
                PanelAction.SAFE_START,
                "Use safe start",
                "Covers your face with the diamond and turns on the red sweep here.",
                id,
            ),
        ),
        notes = listOf(
            "Your effects from 📁 Effects show here too, in the video viewers see.",
            "Never auto rejoin. Rejoining is always your own tap.",
            "Keep your face covered with the face diamond or a camera mode.",
        ),
    )

    /** The Tests folder inside a platform folder (features 15 and 37). */
    fun platformTests(id: String, tests: List<ToggleItem>) = PanelFolder(
        id = "$id.tests",
        title = "Tests",
        tag = null,
        kind = FolderKind.TESTS,
        items = tests,
        folders = listOf(
            PanelFolder(
                id = "$id.tests.code",
                title = "📁 Success code",
                tag = null,
                kind = FolderKind.TESTS,
                items = listOf(
                    ActionItem(PanelAction.TEST_CODE, "Show a test code", "Puts a 6 digit code in your video for 30 seconds.", id),
                    TextItem(
                        "${TestMarks.SEEN}$id",
                        "Code the viewer saw",
                        "Type the code a viewer read off their screen.",
                        "",
                        TEST_CODE_MAX,
                    ),
                ),
                notes = listOf(
                    "1) Tap Show a test code while you stream.",
                    "2) Ask a viewer on another phone which code they see.",
                    "3) Type it here. A match marks Effects show in the video as success.",
                ),
            ),
            PanelFolder(
                id = "$id.tests.results",
                title = "📁 Results",
                tag = null,
                kind = FolderKind.TESTS,
                items = tests.flatMap { test ->
                    listOf(
                        ChoiceItem(TestMarks.markKey(id, test.key), test.label, TEST_MARKS, default = TestMarks.NOT_TESTED),
                        TextItem(TestMarks.noteKey(id, test.key), "Note", "What happened, in a few words.", "", NOTE_MAX),
                    )
                },
                notes = listOf(
                    "Mark each test as success or failed, with a note.",
                    "Failed tests are flagged for more research.",
                    "Marks stay when you reset.",
                ),
            ),
        ),
        notes = listOf(
            "Private test toggles. Viewers never see them.",
            "A lit toggle means on. A dim toggle means off.",
            PanelTree.TEST_MARKERS_LINE,
        ),
    )

    // ---- Helpers ----

    private fun effect(id: String, title: String, vararg items: PanelItem, notes: List<String> = emptyList()) = PanelFolder(
        id = "effects.$id",
        title = title,
        tag = null,
        kind = FolderKind.EFFECTS,
        items = items.toList(),
        notes = notes.ifEmpty { listOf("This effect is baked into the video viewers see.") },
    )

    private fun fire(kind: Fire, label: String, help: String) = ActionItem(PanelAction.FIRE, label, help, kind.name)

    const val STATUS_MAX = 24
    const val EMOJI_MAX = 40
    const val WRITE_MAX = 60
    const val TEST_CODE_MAX = 6
    const val NOTE_MAX = 80

    const val SOUND_OFF = "off"

    /** The built-in sounds. They are made by the app itself, so no sound files are needed. */
    val SOUNDS = listOf(
        Choice(SOUND_OFF, "Off", "No sound for this effect."),
        Choice("chime", "Chime", "A soft bell."),
        Choice("pop", "Pop", "A short pop."),
        Choice("whoosh", "Whoosh", "Air rushing past."),
        Choice("glass", "Glass", "Glass breaking."),
        Choice("power", "Power", "Powering down and back up."),
        Choice("ping", "Ping", "A radar ping."),
        Choice("sparkle", "Sparkle", "Bright twinkles."),
        Choice("boom", "Boom", "A deep firework boom."),
    )

    /** The effects that can have a sound, by sound key, with their folder name. */
    val SOUND_EFFECTS = listOf(
        "fireworks" to "Fireworks",
        "sweep" to "Red sweep",
        "freeze" to "Time freeze",
        "quiet" to "Quiet mode",
        "shatter" to "Shatter",
        "depth" to "Depth shift",
        "write" to "Laser writing",
        "radar" to "Radar ping",
        "emoji" to "Emoji rain",
        "ink" to "Drawing",
        "toggle" to "Toggle flip",
    )

    private val SOUND_START = mapOf(
        "fireworks" to "boom",
        "sweep" to "whoosh",
        "freeze" to "power",
        "quiet" to "power",
        "shatter" to "glass",
        "depth" to "whoosh",
        "write" to "sparkle",
        "radar" to "ping",
        "emoji" to "pop",
        "ink" to SOUND_OFF,
        "toggle" to "chime",
    )

    private val TEST_MARKS = listOf(
        Choice(TestMarks.NOT_TESTED, "Not tested yet", "No result yet."),
        Choice(TestMarks.SUCCESS, "Baked in as success", "It worked. The date is saved."),
        Choice(TestMarks.FAILED, "Failed", "It did not work. Flagged for research. The date is saved."),
    )
}
