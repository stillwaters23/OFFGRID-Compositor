package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.offgrid.compositor.R
import com.offgrid.compositor.ui.wheel.DrumReelView
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
import org.robolectric.annotation.GraphicsMode

/**
 * Lane A finish checks: coded power toggles, edge to edge size 100, the top bar,
 * and terminal text that follows the black and white themes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PanelFinishTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun clearPanelSettings() {
        context.getSharedPreferences("terminal_panel", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun powerIconIsDrawnInCodeAndLeavesTheCornersClear() {
        val size = 80
        val layers = BioluminescentPowerIcon.layers(
            size,
            NeonPalette.CYAN_GLOW,
            NeonPalette.ELECTRIC_PURPLE,
            context.resources.displayMetrics.density,
        )
        assertTrue(layers != null)
        val core = layers!!.core
        assertEquals(size, core.width)
        assertEquals(0, Color.alpha(core.getPixel(1, 1)))
        var ringAlpha = 0
        for (y in 0 until size) ringAlpha = maxOf(ringAlpha, Color.alpha(core.getPixel(size / 2, y)))
        assertTrue("power ring alpha was $ringAlpha", ringAlpha > 0)
        val hot = BioluminescentPowerIcon.hotCore(NeonPalette.CYAN_GLOW)
        assertTrue(Color.red(hot) > Color.red(NeonPalette.CYAN_GLOW))
        assertTrue(Color.green(hot) > Color.green(NeonPalette.CYAN_GLOW))
        assertTrue(Color.blue(hot) >= Color.blue(NeonPalette.CYAN_GLOW))

        val toggle = PowerToggleView(context)
        val quick = QuickToggleView(context, microphone = false, initialOn = true)
        assertFalse(toggle.javaClass.name.contains("Image"))
        assertFalse(quick.javaClass.name.contains("Image"))
        toggle.setWillNotDraw(false)
        toggle.setOn(true)
        val box = Hud.dpi(context, 52f)
        val bitmap = Bitmap.createBitmap(box, box, Bitmap.Config.ARGB_8888)
        toggle.measure(
            View.MeasureSpec.makeMeasureSpec(box, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(box, View.MeasureSpec.EXACTLY),
        )
        toggle.layout(0, 0, box, box)
        toggle.draw(Canvas(bitmap))
        assertTrue(hasInk(bitmap))
    }

    @Test
    fun widthAndHeightOneHundredFillTheWindowAndClearTheScreenEdges() {
        val panel = TerminalPanel(context)
        val width = Hud.dpi(context, 412f)
        val height = Hud.dpi(context, 892f)
        panel.onScreenEdges(
            ScreenEdges(
                top = Hud.dpi(context, 48f),
                bottom = Hud.dpi(context, 24f),
                left = 0,
                right = Hud.dpi(context, 8f),
                gestureBottom = Hud.dpi(context, 24f),
                cornerRadius = Hud.dpi(context, 40f),
            ),
        )
        val parent = FrameLayout(context)
        parent.addView(panel)
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, width, height)
        panel.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        panel.layout(0, 0, panel.measuredWidth, panel.measuredHeight)

        assertEquals(width, panel.measuredWidth)
        assertEquals(height, panel.measuredHeight)
        assertEquals(0, panel.margin)
        assertTrue(panel.fullBleed)
        assertTrue(panel.paddingTop >= Hud.dpi(context, 48f))
        assertTrue(panel.paddingBottom >= Hud.dpi(context, 24f))
        assertTrue(panel.paddingLeft > 0)
        assertTrue(panel.paddingRight >= Hud.dpi(context, 8f))
        assertFalse(findViews(panel).any { it.javaClass.simpleName.contains("SideDrawer") })
        assertEquals(1, panel.body.childCount)
    }

    @Test
    fun topBarUsesTabularDigitsAndThemeText() {
        val panel = TerminalPanel(context)
        assertTrue(panel.topBar.widthInput.fontFeatureSettings.orEmpty().contains("tnum"))
        assertTrue(panel.topBar.heightInput.fontFeatureSettings.orEmpty().contains("tnum"))
        assertEquals(panel.backOrb, panel.topBar.getChildAt(0))
        assertTrue(findViews(panel.topBar).filterIsInstance<DrumReelView>().size >= 2)

        panel.store.setThemeMode(ThemeMode.NIGHT)
        panel.refreshLook()
        assertEquals(0xFF000000.toInt(), panel.look.skin)
        assertEquals(0xFFFFFFFF.toInt(), panel.look.text)
        assertEquals(0xFFFFFFFF.toInt(), panel.statusLine.currentTextColor)
        val nightTitle = title(panel)
        assertEquals(0xFFFFFFFF.toInt(), nightTitle.currentTextColor)
        val nightCaption = caption(panel, "W")
        assertEquals(0xFFFFFFFF.toInt(), nightCaption.currentTextColor)

        panel.store.setThemeMode(ThemeMode.DAY)
        panel.refreshLook()
        assertEquals(0xFFFFFFFF.toInt(), panel.look.skin)
        assertEquals(0xFF000000.toInt(), panel.look.text)
        assertEquals(0xFF000000.toInt(), panel.statusLine.currentTextColor)
        assertEquals(0xFF000000.toInt(), title(panel).currentTextColor)
        assertEquals(0xFF000000.toInt(), caption(panel, "H").currentTextColor)
        assertNotEquals(panel.look.text, panel.look.edge)
    }

    private fun title(panel: TerminalPanel): TextView {
        val label = context.getString(R.string.terminal_title)
        return findViews(panel.topBar).filterIsInstance<TextView>().first { it.text == label }
    }

    private fun caption(panel: TerminalPanel, letter: String): TextView {
        return findViews(panel.topBar).filterIsInstance<TextView>().first { it.text == letter }
    }

    private fun hasInk(bitmap: Bitmap): Boolean {
        for (y in 0 until bitmap.height step 2) {
            for (x in 0 until bitmap.width step 2) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0) return true
            }
        }
        return false
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
