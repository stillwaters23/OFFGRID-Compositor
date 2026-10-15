package com.offgrid.compositor.terminal

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
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
import java.lang.reflect.Modifier
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PanelUiTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun clearPanelSettings() {
        context.getSharedPreferences("terminal_panel", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun fullBleedPanelMeasuresToTheWindow() {
        val panel = TerminalPanel(context)
        val width = Hud.dpi(context, 360f)
        val height = Hud.dpi(context, 720f)
        val parent = FrameLayout(context)
        parent.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, width, height)
        parent.addView(panel)

        panel.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        panel.layout(0, 0, panel.measuredWidth, panel.measuredHeight)

        assertEquals(width, panel.measuredWidth)
        assertEquals(height, panel.measuredHeight)
        assertEquals(0, panel.margin)
        assertTrue(panel.fullBleed)
        assertTrue(panel.glass.fullBleed)
    }

    @Test
    fun topBarControlsShareCenterLineAtPhoneWidths() {
        val panel = TerminalPanel(context)
        val orb = BackOrbView(context)
        val bar = TopBar(
            context,
            panel.prefs,
            width = 100,
            height = 100,
            backOrb = orb,
            onShrink = {},
            onToggle = { _, _, _ -> },
            onDimensionChanged = { _, _ -> },
            onViewModeChanged = {},
        )
        val density = context.resources.displayMetrics.density

        listOf(360, 412, 480).forEach { widthDp ->
            val width = (widthDp * density).roundToInt()
            listOf(0, Hud.dpi(context, 24f)).forEach { insetTop ->
                val parent = FrameLayout(context)
                (bar.parent as? ViewGroup)?.removeView(bar)
                val params = FrameLayout.LayoutParams(width, Hud.dpi(context, 56f)).apply {
                    topMargin = insetTop
                }
                parent.addView(bar, params)
                parent.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(insetTop + Hud.dpi(context, 80f), MeasureSpec.EXACTLY),
                )
                parent.layout(0, 0, width, insetTop + Hud.dpi(context, 80f))

                val title = bar.getChildAt(1)
                assertTrue("$widthDp dp title has layout width", title.measuredWidth > 0)
                val visible = (0 until bar.childCount).map(bar::getChildAt).filter { it.visibility == View.VISIBLE }
                val controlCenter = visible.first().top + visible.first().height / 2 + insetTop
                visible.forEachIndexed { index, child ->
                    assertEquals("$widthDp dp control $index height", Hud.dpi(context, 40f), child.height)
                    assertTrue(
                        "$widthDp dp control $index center",
                        abs(child.top + child.height / 2 + insetTop - controlCenter) <= 1,
                    )
                }
                visible.zipWithNext().forEachIndexed { index, (first, second) ->
                    assertEquals("$widthDp dp control gap $index", Hud.dpi(context, 8f), second.left - first.right)
                }
                assertEquals(orb, bar.getChildAt(0))
                assertEquals(bar.width - bar.paddingRight, visible.last().right)
                val boxesVisible = (bar.widthInput.parent as View).visibility == View.VISIBLE &&
                    (bar.heightInput.parent as View).visibility == View.VISIBLE
                assertTrue(boxesVisible)
                assertEquals(View.VISIBLE, bar.widthInput.visibility)
                assertEquals(View.VISIBLE, bar.heightInput.visibility)
                assertEquals(View.GONE, bar.widthReel.visibility)
                assertEquals(View.GONE, bar.heightReel.visibility)
            }
        }
    }

    @Test
    fun homeHasNoDockedFolderListAndBackOrbReplacesTheTextRow() {
        val panel = TerminalPanel(context)
        assertEquals(1, panel.body.childCount)
        assertTrue(panel.body.getChildAt(0) is FrameLayout)
        assertTrue("the glowing bottom resize edge is visible", panel.glass.resizeBarVisible)
        assertEquals(panel.backOrb, panel.topBar.getChildAt(0))
        panel.refreshBack()
        assertFalse(panel.backOrb.isEnabled)
        assertFalse(findViews(panel).any { it.javaClass.simpleName.contains("ResizeGrip") })

        val folder = panel.home.folders.single { it.id == "microphone" }
        panel.openFolder = folder
        panel.buildDetail(folder)
        panel.refreshBack()
        assertTrue(panel.backOrb.isEnabled)
        val words = findViews(panel.detailContent).filterIsInstance<TextView>().map { it.text.toString() }
        assertFalse(words.contains(context.getString(com.offgrid.compositor.R.string.terminal_back)))
        assertFalse(words.contains("‹ BACK"))
        panel.backOrb.performClick()
        assertEquals(null, panel.openFolder)
        assertFalse(panel.backOrb.isEnabled)
    }

    @Test
    fun scrollingRevealsTheGlowThumb() {
        val scroll = ScrollView(context)
        val child = object : View(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                setMeasuredDimension(Hud.dpi(context, 120f), Hud.dpi(context, 1000f))
            }
        }
        scroll.addView(child, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            Hud.dpi(context, 1000f),
        ))
        val width = Hud.dpi(context, 120f)
        val height = Hud.dpi(context, 100f)
        scroll.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        scroll.layout(0, 0, width, height)
        val thumb = GlowScrollBar(context, scroll)
        thumb.measure(
            MeasureSpec.makeMeasureSpec(Hud.dpi(context, 24f), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        thumb.layout(0, 0, Hud.dpi(context, 24f), height)

        scroll.scrollTo(0, Hud.dpi(context, 80f))
        assertTrue(scroll.scrollY > 0)
        thumb.noteScroll()

        assertEquals(View.VISIBLE, thumb.visibility)
    }

    @Test
    fun everyFxContractKeyIsReachableExactlyOnce() {
        val keys = FxKeys::class.java.fields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .mapNotNull { it.get(null) as? String }
            .filter { it.startsWith("fx.") && !it.endsWith(".") }
        val found = ArrayList<String>()
        fun walk(folder: PanelFolder) {
            folder.items.forEach { item ->
                when (item) {
                    is NumberItem -> found += item.key
                    is ToggleItem -> found += item.key
                    is ChoiceItem -> found += item.key
                    is TextItem -> found += item.key
                    else -> Unit
                }
            }
            folder.folders.forEach(::walk)
        }
        walk(PanelTree.build())

        keys.forEach { key -> assertEquals("$key appears once", 1, found.count { it == key }) }
        val generated = found.filter { it.startsWith("fx.") && it !in keys }
        assertTrue(generated.all { it.startsWith(FxKeys.SWEEP_GO_PREFIX) || it.startsWith(FxKeys.SOUND_PREFIX) })
    }

    @Test
    fun cameraAndMicrophoneContractRowsHaveTheExpectedDefaultsAndPrivacyNote() {
        val root = PanelTree.build()
        val camera = root.folders.single { it.id == "camera" }
        val microphone = root.folders.single { it.id == "microphone" }
        val cameraToggle = camera.items.filterIsInstance<ToggleItem>().single { it.key == FxKeys.CAMERA_ON }
        val microphoneToggle = microphone.items.filterIsInstance<ToggleItem>().single { it.key == FxKeys.MIC_ON }
        val expectedMicKeys = FxKeys::class.java.fields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .mapNotNull { it.get(null) as? String }
            .filter { it.startsWith("fx.mic.") }
            .toSet()

        assertTrue(cameraToggle.default)
        assertFalse(microphoneToggle.default)
        assertEquals(expectedMicKeys, microphone.items.mapNotNull {
            when (it) {
                is NumberItem -> it.key
                is ToggleItem -> it.key
                is ChoiceItem -> it.key
                is TextItem -> it.key
                else -> null
            }
        }.toSet())
        assertTrue(microphone.notes.contains("Everything runs on this phone. Nothing goes to the cloud."))
        assertEquals(FolderView.GRID_2, FolderSettings(PanelPrefs(context)).view)
    }

    @Test
    fun systemTextColorsFollowTheSelectedUiMode() {
        val base = context.resources.configuration
        fun configured(mode: Int): Context {
            val configuration = Configuration(base)
            configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
            return context.createConfigurationContext(configuration)
        }
        val day = SystemRole.SYSTEM_TEXT.resolve(configured(Configuration.UI_MODE_NIGHT_NO))
        val night = SystemRole.SYSTEM_TEXT.resolve(configured(Configuration.UI_MODE_NIGHT_YES))

        assertNotEquals(day, night)
        assertEquals(SystemRole.SYSTEM_TEXT, Themes.spec(Theme.SYSTEM).parts.getValue(ColorPart.TEXT).role)
        assertEquals(Theme.SYSTEM, Theme.from("oled"))
        val dayStore = ThemeStore(configured(Configuration.UI_MODE_NIGHT_NO), PanelPrefs(context))
        val nightStore = ThemeStore(configured(Configuration.UI_MODE_NIGHT_YES), PanelPrefs(context))
        assertEquals(0xFFFFFFFF.toInt(), dayStore.color(ColorPart.SKIN))
        assertEquals(0xFF000000.toInt(), dayStore.color(ColorPart.TEXT))
        assertEquals(0xFF000000.toInt(), nightStore.color(ColorPart.SKIN))
        assertEquals(0xFFFFFFFF.toInt(), nightStore.color(ColorPart.TEXT))
    }

    @Test
    fun systemThemeUsesPureBlackAndWhiteAndModePersists() {
        val store = ThemeStore(context, PanelPrefs(context))
        store.setThemeMode(ThemeMode.NIGHT)
        assertEquals(0xFF000000.toInt(), store.color(ColorPart.SKIN))
        assertEquals(0xFFFFFFFF.toInt(), store.color(ColorPart.TEXT))
        assertEquals(0xFFFFFFFF.toInt(), store.color(ColorPart.STATUS))

        store.setThemeMode(ThemeMode.DAY)
        assertEquals(0xFFFFFFFF.toInt(), store.color(ColorPart.SKIN))
        assertEquals(0xFF000000.toInt(), store.color(ColorPart.TEXT))
        assertEquals(0xFF000000.toInt(), store.color(ColorPart.STATUS))
        assertEquals(ThemeMode.DAY, ThemeStore(context, PanelPrefs(context)).themeMode())
    }

    @Test
    fun customTypewriterTextColorOverridesSystemDayNightAndLook() {
        val store = ThemeStore(context, PanelPrefs(context))
        store.setThemeMode(ThemeMode.NIGHT)
        assertEquals(0xFFFFFFFF.toInt(), store.color(ColorPart.TEXT))

        val neon = 0xFF39FF14.toInt()
        store.setCustom(ColorPart.TEXT, neon, RainbowMix.split(neon))
        assertEquals(neon or 0xFF000000.toInt(), store.color(ColorPart.TEXT))
        assertEquals(neon or 0xFF000000.toInt(), store.look().text)

        // Status stays on system day/night until its own swatch is customized.
        assertEquals(0xFFFFFFFF.toInt(), store.color(ColorPart.STATUS))

        val statusNeon = 0xFFFF00FF.toInt()
        store.setCustom(ColorPart.STATUS, statusNeon, RainbowMix.split(statusNeon))
        assertEquals(statusNeon or 0xFF000000.toInt(), store.look().status)
    }

    @Test
    fun themePickerContainsOnlySystemAndModePickerContainsAutoDayNight() {
        val themes = PanelTree.build().folders.single { it.id == "panel_settings" }
            .folders.single { it.id == "themes" }
        assertEquals(listOf(Theme.SYSTEM), themes.items.filterIsInstance<ThemeItem>().map { it.theme })
        assertEquals(ArchivedTheme.entries.toSet(), ArchivedThemes.definitions.keys)
        assertTrue(ArchivedThemes.definitions.values.all { it.parts.keys.containsAll(ColorPart.entries) })
        val mode = themes.folders.single { it.id == "theme_mode" }.items.single() as ChoiceItem
        assertEquals(listOf("auto", "day", "night"), mode.choices.map { it.value })
    }

    @Test
    fun effectLibraryContainsEveryCuratedHttpsLinkInNonEmptyCategories() {
        assertTrue(EffectLinks.categories.isNotEmpty())
        assertTrue(EffectLinks.categories.all { it.links.isNotEmpty() })
        val links = EffectLinks.categories.flatMap { it.links }
        assertEquals(12, links.size)
        assertTrue(links.all { it.url.startsWith("https://") })
        assertTrue(links.all { android.net.Uri.parse(it.url).host?.isNotBlank() == true })
        assertEquals(EffectLinks.LICENSE_NOTE, PanelTree.build().folders.single {
            it.id == "panel_settings"
        }.folders.single { it.id == "effects_library" }.notes.single())
    }

    @Test
    fun neonPaletteHasThirtyColorsAndASeparateCyberGroup() {
        assertEquals(31, NeonPalette.swatches.size)
        assertEquals(4, NeonPalette.swatches.count { it.cyber })
        assertTrue(NeonPalette.swatches.filterNot { it.cyber }.all {
            RainbowMix.hex(it.color).matches(Regex("#[0-9A-F]{6}"))
        })
    }

    @Test
    fun setToggleSettingPersistsAndRefreshesTheVisibleRowAndQuickToggle() {
        val panel = TerminalPanel(context)
        val folder = panel.home.folders.single { it.id == "microphone" }
        panel.openFolder = folder
        panel.buildDetail(folder)
        val changed = mutableListOf<String>()
        panel.onSettingChanged = { changed += it }

        panel.setToggleSetting(FxKeys.MIC_ON, true)

        val rowToggles = ArrayList<PowerToggleView>()
        collectViews(panel.detailContent, rowToggles)
        assertTrue(panel.store.toggle(FxKeys.MIC_ON))
        assertTrue(rowToggles.first().isOn)
        assertTrue(findViews(panel.topBar).filterIsInstance<QuickToggleView>()
            .any { it.contentDescription == "Microphone on" })
        assertTrue(findViews(panel.detailContent).filterIsInstance<TextView>()
            .any { it.text == context.getString(com.offgrid.compositor.R.string.terminal_folder_reset) })
        assertEquals(listOf(FxKeys.MIC_ON), changed)
    }

    private fun collectViews(root: View, result: MutableList<PowerToggleView>) {
        if (root is PowerToggleView) result += root
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) collectViews(root.getChildAt(index), result)
        }
    }

    private fun findViews(root: View): List<View> {
        val result = ArrayList<View>()
        fun walk(view: View) {
            result += view
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) walk(view.getChildAt(index))
            }
        }
        walk(root)
        return result
    }
}
