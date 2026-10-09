package com.offgrid.compositor.terminal

import android.content.Context
import com.offgrid.compositor.effects.FxKeys
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
        for (id in listOf("camera", "camera.diamond", "camera.cloak", "camera.status", "camera.ghost", "camera.takeover",
            "camera.mirror", "effects", "effects.emoji", "effects.ink", "effects.border", "effects.fireworks", "effects.portal",
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
        store.setNumber(FxKeys.CLOAK_STRENGTH, 99)
        store.setNumber(FxKeys.EMOJI_SIZE, 99)
        store.setText(FxKeys.STATUS_TEXT, "AWAY")
        val camera = PanelTree.build().folders.first { it.id == "camera" }
        assertTrue(FolderReset.hasSettings(camera))
        FolderReset.reset(store, camera)
        assertEquals(60, store.number(FxKeys.CLOAK_STRENGTH))
        assertEquals("CLOAKED", store.text(FxKeys.STATUS_TEXT))
        assertEquals("the camera mode stays, so the face never shows", FxKeys.MODE_CLOAK, store.choice(FxKeys.CAMERA_MODE))
        assertEquals("other folders keep their settings", 99, store.number(FxKeys.EMOJI_SIZE))
        // Reset all keeps the camera mode too.
        store.resetAll()
        assertEquals(FxKeys.MODE_CLOAK, store.choice(FxKeys.CAMERA_MODE))
        assertNotEquals(99, store.number(FxKeys.EMOJI_SIZE))
    }
}
