package com.offgrid.compositor.terminal

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Checks the panel's saved settings: the start look, saving and loading, the final build's
 * one-time settings move, Reset all, and folder order. It runs on the computer that builds
 * the app, not on the phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PanelSettingsTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private fun raw() = context.getSharedPreferences("terminal_panel", Context.MODE_PRIVATE)

    @Before
    fun clear() {
        raw().edit().clear().commit()
    }

    @Test
    fun aFreshInstallShowsTheStartLook() {
        val store = ThemeStore(context, PanelPrefs(context))
        val look = store.look()
        assertEquals(Theme.SYSTEM, look.base)
        assertEquals(ToggleStyle.WHITE, look.toggleStyle)
        assertFalse("toggle glow starts off", look.toggleGlow)
        assertEquals(100, store.number(LookKeys.WIDTH))
        assertEquals(100, store.number(LookKeys.HEIGHT))
        assertEquals(100, store.number(LookKeys.OPACITY))
        assertEquals(ThemeMode.AUTO, store.themeMode())
        assertEquals(50, store.number(LookKeys.GLOW))
        assertEquals(1, store.number(LookKeys.TYPING))
        assertEquals(4, store.number(LookKeys.CORNERS))
        assertFalse(store.toggle(LookKeys.FULL_SCREEN))
        assertFalse(store.toggle(LookKeys.SEE_THROUGH_FULL_SIZE))
        assertFalse(store.toggle(LookKeys.TEST_DIAMOND))
        assertTrue(store.toggle(LookKeys.PULSE))
        assertTrue(store.toggle(LookKeys.GLOW_ON))
        assertEquals(0f, look.cursorGlow, 0f)
        assertEquals(0xFFFFFFFF.toInt(), look.cursor)
    }

    @Test
    fun settingsSaveAndLoadBack() {
        val store = ThemeStore(context, PanelPrefs(context))
        store.setNumber(LookKeys.OPACITY, 73)
        store.setNumber(LookKeys.GLOW, 150)
        store.setToggle(LookKeys.SCAN_LINE, true)
        store.setChoice(LookKeys.TOGGLE_STYLE, ToggleStyle.BLACK.key)
        store.setText("test.text", "CLOAKED")
        val again = ThemeStore(context, PanelPrefs(context))
        assertEquals(73, again.number(LookKeys.OPACITY))
        assertEquals("numbers stay between 0 and 100", 100, again.number(LookKeys.GLOW))
        assertTrue(again.toggle(LookKeys.SCAN_LINE))
        assertEquals(ToggleStyle.BLACK, again.look().toggleStyle)
        assertEquals("CLOAKED", again.text("test.text"))
    }

    @Test
    fun olderBuildSettingsAreLetGoOnceAndTestMarksStay() {
        raw().edit()
            .putInt(PanelPrefs.LAYOUT, 2)
            .putInt("number.opacity", 85)
            .putInt("number.height", 32)
            .putString("text.base_theme", Theme.TITANIUM.key)
            .putBoolean("toggle.titanium_frame", true)
            .putBoolean("toggle.test.bigo_live.tests.burned_in", true)
            .commit()
        val prefs = PanelPrefs(context)
        assertEquals(PanelPrefs.LAYOUT_NOW, prefs.layoutVersion)
        val store = ThemeStore(context, prefs)
        assertEquals(Theme.SYSTEM, store.base())
        assertEquals(100, store.number(LookKeys.OPACITY))
        assertEquals(100, store.number(LookKeys.HEIGHT))
        assertFalse(store.toggle(LookKeys.TITANIUM_FRAME))
        assertTrue("private test marks stay", prefs.toggle("test.bigo_live.tests.burned_in", false))
        // It happens only once. Settings changed after the move are kept.
        store.setNumber(LookKeys.OPACITY, 30)
        assertEquals(30, ThemeStore(context, PanelPrefs(context)).number(LookKeys.OPACITY))
    }

    @Test
    fun retiredThemeSelectionMigratesToSystem() {
        raw().edit()
            .putInt(PanelPrefs.LAYOUT, PanelPrefs.LAYOUT_NOW)
            .putString("text.base_theme", "uv_black_light")
            .commit()

        val store = ThemeStore(context, PanelPrefs(context))

        assertEquals(Theme.SYSTEM, store.base())
        assertEquals("system", raw().getString("text.base_theme", null))
    }

    @Test
    fun resetAllBringsBackTheStartAndKeepsTestMarks() {
        val prefs = PanelPrefs(context)
        val store = ThemeStore(context, prefs)
        store.applyTheme(Theme.SYSTEM)
        store.setNumber(LookKeys.FONT_SIZE, 100)
        store.setChoice(LookKeys.TOGGLE_STYLE, ToggleStyle.BLACK.key)
        prefs.setToggle("test.tiktok.tests.chat_gifts", true)
        store.resetAll()
        val after = ThemeStore(context, PanelPrefs(context))
        assertEquals(Theme.SYSTEM, after.base())
        assertEquals(50, after.number(LookKeys.FONT_SIZE))
        assertEquals(ToggleStyle.WHITE, after.look().toggleStyle)
        assertEquals(FolderView.GRID_2, FolderSettings(PanelPrefs(context)).view)
        assertTrue(PanelPrefs(context).toggle("test.tiktok.tests.chat_gifts", false))
        assertEquals(PanelPrefs.LAYOUT_NOW, PanelPrefs(context).layoutVersion)
    }

    @Test
    fun folderOrderFollowsTheSort() {
        val home = PanelTree.build()
        val settings = FolderSettings(PanelPrefs(context))
        assertEquals(FolderSort.IMPORTANT, settings.sort)
        assertEquals(FolderView.GRID_2, settings.view)
        assertEquals(listOf("Panel settings"), settings.arrange(home).take(1).map { it.title })
        settings.sort = FolderSort.A_TO_Z
        val names = settings.arrange(home).map { FolderSettings.sortName(it) }
        assertEquals(names.sortedWith(String.CASE_INSENSITIVE_ORDER), names)
        // Custom: saved folders first, in their saved order, then any others.
        settings.sort = FolderSort.CUSTOM
        val panel = home.folders.first { it.id == "panel_settings" }
        val bigo = home.folders.first { it.id == "bigo_live" }
        settings.saveOrder(home, listOf(bigo, panel))
        assertEquals(listOf(bigo, panel), settings.arrange(home).take(2))
        assertEquals(home.folders.size, settings.arrange(home).size)
        assertEquals(home.folders.toSet(), settings.arrange(home).toSet())
    }

    @Test
    fun moveFoldersFromAToZKeepsEveryFolderInOrder() {
        val home = PanelTree.build()
        val settings = FolderSettings(PanelPrefs(context))
        assertFalse(settings.hasSavedOrder(home))
        settings.sort = FolderSort.A_TO_Z
        val before = HashMap<String, List<String>>()
        fun walk(folder: PanelFolder) {
            if (folder.folders.size > 1) before[folder.id] = settings.arrange(folder).map { it.id }
            folder.folders.forEach { walk(it) }
        }
        walk(home)
        settings.keepEveryOrder(home)
        settings.sort = FolderSort.CUSTOM
        assertTrue(settings.hasSavedOrder(home))
        fun check(folder: PanelFolder) {
            before[folder.id]?.let { assertEquals(folder.id, it, settings.arrange(folder).map { child -> child.id }) }
            folder.folders.forEach { check(it) }
        }
        check(home)
    }

    @Test
    fun everySettingHasOneStartValue() {
        val defaults = PanelTree.defaults
        assertEquals(ToggleStyle.WHITE.key, defaults.choices[LookKeys.TOGGLE_STYLE])
        assertEquals(false, defaults.toggles[LookKeys.TOGGLE_GLOW])
        assertEquals(false, defaults.toggles[LookKeys.FULL_SCREEN])
        assertEquals(false, defaults.toggles[LookKeys.SEE_THROUGH_FULL_SIZE])
    }

    @Test
    fun removedSidePanelSettingsAreDiscardedOnExistingInstall() {
        raw().edit()
            .putInt(PanelPrefs.LAYOUT, PanelPrefs.LAYOUT_NOW)
            .putString("text.side_mode", "list")
            .putBoolean("system_color.side_text", false)
            .putInt("custom.side_text", 0xFF00FF00.toInt())
            .putString("role.side_text", "NEUTRAL1_100")
            .putString("mix.side_text", "1,2,3")
            .commit()

        PanelPrefs(context)

        assertFalse(raw().contains("text.side_mode"))
        assertFalse(raw().contains("system_color.side_text"))
        assertFalse(raw().contains("custom.side_text"))
        assertFalse(raw().contains("role.side_text"))
        assertFalse(raw().contains("mix.side_text"))
    }
}
