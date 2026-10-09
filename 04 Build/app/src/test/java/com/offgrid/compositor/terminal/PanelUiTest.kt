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
        val bar = TopBar(
            context,
            PanelPrefs(context),
            width = 100,
            height = 100,
            onShrink = {},
            onToggle = { _, _, _ -> },
            onDimensionChanged = { _, _ -> },
            onViewModeChanged = {},
        )
        val density = context.resources.displayMetrics.density
        val barHeight = Hud.dpi(context, 48f)

        listOf(360, 412, 480).forEach { widthDp ->
            val width = (widthDp * density).roundToInt()
            repeat(2) {
                bar.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(barHeight, MeasureSpec.EXACTLY),
                )
                bar.layout(0, 0, width, barHeight)
            }

            val title = bar.getChildAt(0)
            assertTrue("$widthDp dp title remains visible", title.measuredWidth > 0)
            for (index in 0 until bar.childCount) {
                val child = bar.getChildAt(index)
                if (child.visibility != View.VISIBLE) continue
                val center = child.top + child.height / 2
                assertTrue("$widthDp dp child $index is centered", abs(center - barHeight / 2) <= 1)
            }
            val boxesVisible = (bar.widthInput.parent as View).visibility == View.VISIBLE &&
                (bar.heightInput.parent as View).visibility == View.VISIBLE
            assertEquals(widthDp >= 480, boxesVisible)
        }
    }

    @Test
    fun scrollingRevealsTheGlowThumb() {
        val scroll = ScrollView(context)
        scroll.addView(View(context), FrameLayout.LayoutParams(
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
        assertEquals(keys.toSet(), found.filter { it.startsWith("fx.") }.toSet())
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
        assertEquals(FolderView.BIG_LIST, FolderSettings(PanelPrefs(context)).view)
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
        assertEquals(SystemRole.SYSTEM_TEXT, Themes.spec(Theme.OLED).parts.getValue(ColorPart.TEXT).role)
        assertEquals(SystemRole.SYSTEM_TEXT, Themes.spec(Theme.SYSTEM).parts.getValue(ColorPart.TEXT).role)
        assertEquals(Theme.UV_BLACK_LIGHT, Theme.from(Theme.UV_BLACK_LIGHT.key))
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
