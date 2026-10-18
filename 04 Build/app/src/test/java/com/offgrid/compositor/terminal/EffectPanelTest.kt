package com.offgrid.compositor.terminal

import android.content.Context
import com.offgrid.compositor.audio.voice.MicKeys
import com.offgrid.compositor.audio.voice.RexVoiceProfile
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.live.LiveActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Checks the final build's panel additions: look presets, effect start values, test marks
 * with the success code, and every folder's Reset. It runs on the computer that builds the
 * app, not on the phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EffectPanelTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun clear() {
        context.getSharedPreferences("terminal_panel", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("look_presets", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun store(): Pair<PanelPrefs, ThemeStore> {
        val prefs = PanelPrefs(context)
        return prefs to ThemeStore(context, prefs)
    }

    @Test
    fun profilesSaveLoadRenameArchiveAndRestore() {
        val (prefs, store) = store()
        val presets = LookPresets(context, prefs)
        store.setNumber(LookKeys.GLOW, 90)
        store.setChoice(LookKeys.TOGGLE_STYLE, ToggleStyle.BLACK.key)
        store.setNumber(LookKeys.WIDTH, 70)
        store.setChoice(FxKeys.CAMERA_MODE, FxKeys.MODE_BLACKOUT)
        store.setToggle(FxKeys.EMOJI_ON, false)
        prefs.setPosition(0.2f, 0.7f)
        assertEquals(LookPresets.Result.DONE, presets.save("Night"))
        // Change the profile settings, then load the saved folder back.
        store.setNumber(LookKeys.GLOW, 10)
        store.setChoice(LookKeys.TOGGLE_STYLE, ToggleStyle.WHITE.key)
        store.setNumber(LookKeys.WIDTH, 100)
        store.setToggle(FxKeys.EMOJI_ON, true)
        store.setChoice(FxKeys.CAMERA_MODE, FxKeys.MODE_LIVE)
        prefs.setPosition(0.8f, 0.3f)
        assertEquals(LookPresets.Result.DONE, presets.load("Night"))
        assertEquals(90, store.number(LookKeys.GLOW))
        assertEquals(ToggleStyle.BLACK, store.look().toggleStyle)
        assertEquals(70, store.number(LookKeys.WIDTH))
        assertFalse("profiles restore effect toggles", store.toggle(FxKeys.EMOJI_ON))
        assertEquals(FxKeys.MODE_BLACKOUT, store.choice(FxKeys.CAMERA_MODE))
        assertEquals(0.2f to 0.7f, prefs.position())
        assertEquals(LookPresets.Result.TAKEN, presets.save("Night"))
        assertEquals(LookPresets.Result.REPLACED, presets.save("Night", overwrite = true))
        assertEquals(LookPresets.Result.NO_NAME, presets.save("  "))
        presets.save("Day")
        assertEquals(listOf("Night", "Day"), presets.names())
        assertEquals(LookPresets.Result.DONE, presets.duplicate("Night", "Night Copy"))
        assertTrue(presets.names().contains("Night Copy"))
        assertEquals(LookPresets.Result.TAKEN, presets.rename("Night", "Day"))
        assertEquals(LookPresets.Result.DONE, presets.rename("Night", "Midnight"))
        assertEquals(listOf("Midnight", "Day", "Night Copy"), presets.names())
        assertEquals(LookPresets.Result.DONE, presets.load("Midnight"))
        assertEquals(LookPresets.Result.DONE, presets.archive("Day"))
        assertEquals(listOf("Day"), presets.archivedNames())
        assertEquals(LookPresets.Result.DONE, presets.restore("Day"))
        assertTrue(presets.names().contains("Day"))
        assertEquals(LookPresets.Result.DONE, presets.load("Day"))
        // Reset all never deletes presets.
        store.resetAll()
        assertTrue(LookPresets(context, PanelPrefs(context)).names().contains("Midnight"))
    }

    @Test
    fun profileImportRejectsBadJsonAndAcceptsOnlyTypedPreferenceEntries() {
        val prefs = PanelPrefs(context)
        val profiles = LookPresets(context, prefs)
        assertEquals(LookPresets.Result.BAD_FILE, profiles.importJson("Bad", "{"))
        assertEquals(
            LookPresets.Result.BAD_FILE,
            profiles.importJson("Bad", """{"text.handshake_code":"private"}"""),
        )
        assertEquals(
            LookPresets.Result.DONE,
            profiles.importJson("Clean", """{"toggle.fx.emoji.on":true,"position_x":0.4,"position_y":0.6}"""),
        )
        assertTrue(profiles.exportJson("Clean")?.contains("\"toggle.fx.emoji.on\":true") == true)
    }

    @Test
    fun effectsStartSafeAndQuiet() {
        val (_, store) = store()
        val start = EffectSettings.snapshot(store)
        assertEquals(FxKeys.MODE_LIVE, start.choice(FxKeys.CAMERA_MODE))
        assertEquals(FxKeys.BODY_OFF, start.choice(FxKeys.BODY_MODE))
        assertEquals(40, start.raw(FxKeys.BODY_EDGE))
        assertTrue("the face diamond starts on", start.on(FxKeys.DIAMOND_ON))
        assertTrue(start.on(FxKeys.DIAMOND_FOLLOW))
        assertFalse(start.on(FxKeys.EMOJI_ON))
        assertFalse(start.on(FxKeys.SOUND_ON))
        assertFalse(start.on(FxKeys.REPLAY_ON))
        assertEquals("CLOAKED", start.text(FxKeys.STATUS_TEXT))
        assertEquals(30, start.raw(FxKeys.RECORD_SECONDS))
        assertTrue("each platform's red sweep on kick starts on", start.on(FxKeys.SWEEP_GO_PREFIX + "tiktok"))
        // Every effect setting in a folder has a start value, so nothing reads as a blank.
        val defaults = PanelTree.defaults
        fun walk(folder: PanelFolder) {
            for (item in folder.items) {
                if (item is NumberItem && item.key.startsWith("fx.")) assertTrue(item.key, defaults.numbers.containsKey(item.key))
            }
            folder.folders.forEach { walk(it) }
        }
        walk(PanelTree.build())
    }

    @Test
    fun theTreeHasEveryEffectFolderAndNoSliders() {
        val home = PanelTree.build()
        val ids = mutableListOf<String>()
        fun walk(folder: PanelFolder) {
            ids += folder.id
            folder.folders.forEach { walk(it) }
        }
        walk(home)
        for (id in listOf("camera", "camera.diamond", "camera.body", "camera.cloak", "camera.status", "camera.ghost", "camera.takeover",
            "microphone", "effects",
            "camera.mirror", "effects.emoji", "effects.ink", "effects.border", "effects.fireworks", "effects.portal",
            "effects.heroes", "effects.coaster", "effects.clouds", "effects.beach", "effects.freeze", "effects.illusion",
            "effects.sweep", "effects.depth", "effects.write", "effects.radar", "effects.echo", "effects.quiet", "effects.shatter",
            "effects.sound", "record", "presets", "offgrid_to_offgrid.handshake", "tiktok.effects", "tiktok.tests.code",
            "tiktok.tests.results")) {
            assertTrue("missing folder $id", id in ids)
        }
        assertEquals("folder ids are unique", ids.size, ids.toSet().size)
        assertEquals(18, home.folders.count { it.kind == FolderKind.PLATFORM })
    }

    @Test
    fun liveAndRemoteFolderUsesTheRequiredKeysAndActions() {
        val folder = PanelTree.build().folders.single { it.id == "live_remote" }
        val keys = folder.items.mapNotNull {
            when (it) {
                is ToggleItem -> it.key
                is NumberItem -> it.key
                is ChoiceItem -> it.key
                is TextItem -> it.key
                else -> null
            }
        }.toSet()
        assertTrue(FxKeys.LIVE_OVERLAY in keys)
        assertTrue(FxKeys.REMOTE_ROLE in keys)
        assertTrue(FxKeys.LIVE_PC_ON in keys)
        assertTrue(FxKeys.LIVE_PC_PROTOCOL in keys)
        assertTrue(FxKeys.LIVE_PC_HOST in keys)
        assertTrue(FxKeys.LIVE_PC_PORT in keys)
        assertTrue(FxKeys.LIVE_PC_RES in keys)
        assertTrue(FxKeys.LIVE_PC_BITRATE in keys)
        val actions = folder.items.filterIsInstance<ActionItem>().map { it.action }
        assertTrue(PanelAction.LIVE_OPEN_OVERLAY_SETTINGS in actions)
        assertTrue(PanelAction.LIVE_CLEAN_OUTPUT in actions)
        assertTrue(PanelAction.REMOTE_OPEN_PAIRING in actions)
        assertTrue(PanelAction.COPY_PAIRING_KEY in actions)
        assertTrue(PanelAction.LIVE_SEND_TO_PC in actions)
        val copyHelp = folder.items.filterIsInstance<ActionItem>().single { it.action == PanelAction.COPY_PAIRING_KEY }.help
        assertTrue(copyHelp.contains("PC or an Android phone"))
        val pairingHelp = folder.items.filterIsInstance<ActionItem>().single { it.action == PanelAction.REMOTE_OPEN_PAIRING }.help
        assertTrue(pairingHelp.contains("PC or an Android phone"))
        assertTrue(PanelAction.REMOTE_OPEN_PREVIEW in actions)
        assertFalse(folder.items.filterIsInstance<ToggleItem>().single { it.key == FxKeys.LIVE_OVERLAY }.default)
        assertEquals("off", folder.items.filterIsInstance<ChoiceItem>().first { it.key == FxKeys.REMOTE_ROLE }.default)
        assertEquals(LiveActions.OPEN_OVERLAY, PanelAction.LIVE_OPEN_OVERLAY_SETTINGS.name)
        assertEquals(LiveActions.CLEAN_OUTPUT, PanelAction.LIVE_CLEAN_OUTPUT.name)
        assertEquals(LiveActions.OPEN_PAIRING, PanelAction.REMOTE_OPEN_PAIRING.name)
        assertEquals(LiveActions.COPY_PAIRING_KEY, PanelAction.COPY_PAIRING_KEY.name)
        assertEquals(LiveActions.OPEN_PREVIEW, PanelAction.REMOTE_OPEN_PREVIEW.name)
        assertEquals(LiveActions.SEND_TO_PC, PanelAction.LIVE_SEND_TO_PC.name)
    }

    @Test
    fun broadcastLockRowsLiveInThePanelNowThatTheStageHasNoBars() {
        val tree = PanelTree.build()
        val liveRemote = tree.folders.single { it.id == "live_remote" }.items.filterIsInstance<ActionItem>()
        assertTrue(liveRemote.any { it.action == PanelAction.LOCK_ARM })
        assertEquals("Lock now", liveRemote.single { it.action == PanelAction.LIVE_CLEAN_OUTPUT }.label)
        assertEquals(LiveActions.LOCK_ARM, PanelAction.LOCK_ARM.name)

        val record = tree.folders.single { it.id == "record" }.items.filterIsInstance<ActionItem>().map { it.action }
        assertEquals(listOf(PanelAction.RECORD_TOGGLE, PanelAction.PLAY_LAST), record.take(2))
    }

    @Test
    fun microphoneSettingsStartOffAndExposeTheRexTuningControls() {
        val defaults = PanelTree.defaults
        val mic = PanelTree.build().folders.first { it.id == "microphone" }
        val keys = mic.items.mapNotNull {
            when (it) {
                is NumberItem -> it.key
                is ToggleItem -> it.key
                else -> null
            }
        }

        assertTrue(MicKeys.ON in keys)
        assertTrue(MicKeys.BLOCKED in keys)
        assertTrue(MicKeys.VOICE_SWAP in keys)
        assertTrue(MicKeys.BYPASS in keys)
        assertTrue(listOf(MicKeys.WET, MicKeys.PITCH, MicKeys.FORMANT, MicKeys.LOW_EQ, MicKeys.MID_EQ, MicKeys.HIGH_EQ)
            .all { it in defaults.numbers })
        assertEquals(false, defaults.toggles[MicKeys.ON])
        assertEquals(false, defaults.toggles[MicKeys.BLOCKED])
        assertEquals(false, defaults.toggles[MicKeys.VOICE_SWAP])
        assertEquals(false, defaults.toggles[MicKeys.BYPASS])
        assertEquals(RexVoiceProfile.WET_PERCENT, defaults.numbers[MicKeys.WET])
        assertEquals(RexVoiceProfile.pitchPercent, defaults.numbers[MicKeys.PITCH])
        assertEquals(RexVoiceProfile.formantPercent, defaults.numbers[MicKeys.FORMANT])
        assertEquals(RexVoiceProfile.lowPercent, defaults.numbers[MicKeys.LOW_EQ])
        assertEquals(RexVoiceProfile.midPercent, defaults.numbers[MicKeys.MID_EQ])
        assertEquals(RexVoiceProfile.highPercent, defaults.numbers[MicKeys.HIGH_EQ])
        assertTrue(RexVoiceProfile.pitchPercent < 50)
    }

    @Test
    fun theSuccessCodeMarksTheTestWithTheDate() {
        val (_, store) = store()
        assertEquals(TestMarks.Check.NO_CODE, TestMarks.check(store, "tiktok", "123456"))
        val code = TestMarks.newCode()
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
        store.setText(TestMarks.SHOWN + "tiktok", code)
        val spaced = code.substring(0, 3) + " " + code.substring(3)
        assertEquals(TestMarks.Check.MATCH, TestMarks.check(store, "tiktok", spaced, day = "2026-10-09"))
        assertEquals(TestMark.SUCCESS, TestMarks.mark(store, "tiktok", TestMarks.CODE_TEST))
        assertEquals("2026-10-09", TestMarks.date(store, "tiktok", TestMarks.CODE_TEST))
        // A wrong code marks it failed and flags it.
        store.setText(TestMarks.SHOWN + "kick", "111111")
        assertEquals(TestMarks.Check.MISMATCH, TestMarks.check(store, "kick", "222222", day = "2026-10-09"))
        assertEquals(TestMark.FAILED, TestMarks.mark(store, "kick", TestMarks.CODE_TEST))
        // Marks stay through Reset all.
        store.resetAll()
        assertEquals(TestMark.SUCCESS, TestMarks.mark(store, "tiktok", TestMarks.CODE_TEST))
    }

    @Test
    fun aFolderResetPutsOnlyThatFolderBackAndNeverTheCameraMode() {
        val (_, store) = store()
        store.setChoice(FxKeys.CAMERA_MODE, FxKeys.MODE_CLOAK)
        store.setChoice(FxKeys.BODY_MODE, FxKeys.BODY_SEE)
        store.setNumber(FxKeys.BODY_EDGE, 99)
        store.setNumber(FxKeys.CLOAK_STRENGTH, 99)
        store.setNumber(FxKeys.EMOJI_SIZE, 99)
        store.setText(FxKeys.STATUS_TEXT, "AWAY")
        val camera = PanelTree.build().folders.first { it.id == "camera" }
        assertTrue(FolderReset.hasSettings(camera))
        FolderReset.reset(store, camera)
        assertEquals(60, store.number(FxKeys.CLOAK_STRENGTH))
        assertEquals("CLOAKED", store.text(FxKeys.STATUS_TEXT))
        assertEquals("the camera mode stays, so the face never shows", FxKeys.MODE_CLOAK, store.choice(FxKeys.CAMERA_MODE))
        assertEquals("the body look stays, so the face never shows", FxKeys.BODY_SEE, store.choice(FxKeys.BODY_MODE))
        assertEquals(40, store.number(FxKeys.BODY_EDGE))
        assertEquals("other folders keep their settings", 99, store.number(FxKeys.EMOJI_SIZE))
        // Reset all keeps the camera mode too.
        store.resetAll()
        assertEquals(FxKeys.MODE_CLOAK, store.choice(FxKeys.CAMERA_MODE))
        assertEquals(FxKeys.BODY_SEE, store.choice(FxKeys.BODY_MODE))
        assertNotEquals(99, store.number(FxKeys.EMOJI_SIZE))
    }
}
