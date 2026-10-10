package com.offgrid.compositor.terminal

import android.view.View.MeasureSpec
import android.widget.LinearLayout
import com.offgrid.compositor.ui.wheel.LaserStrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SizeReelBankTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun widthAndHeightShareOneLaserAndKeepTheirValuesWhenResized() {
        var width = 40
        var height = 77
        val bank = bank(
            width,
            height,
            onChange = { key, value, _ ->
                if (key == LookKeys.WIDTH) width = value else height = value
            },
        )
        measure(bank, 640)
        assertEquals(40, bank.widthReel.value)
        assertEquals(77, bank.heightReel.value)
        assertTrue(bank.widthReel.glyphFits())
        assertTrue(bank.heightReel.glyphFits())
        assertTrue(bank.measuredWidth <= 640)
        assertTrue(bank.sharesOneLaser())

        bank.widthReel.setFontScale(1.45f)
        bank.heightReel.setFontScale(1.45f)
        measure(bank, 280)
        assertEquals(40, bank.widthReel.value)
        assertEquals(77, bank.heightReel.value)
        assertTrue(bank.widthReel.glyphFits())
        assertTrue(bank.heightReel.glyphFits())
        assertTrue(bank.measuredWidth <= 280)
        assertTrue(bank.sharesOneLaser())
    }

    @Test
    fun typedNumberIsTheAccessibleFallback() {
        var saved = -1
        var announced = false
        val bank = bank(10, 20, onChange = { key, value, announce ->
            if (key == LookKeys.WIDTH) {
                saved = value
                announced = announce
            }
        })
        measure(bank, 480)
        bank.widthBox.setText("15")
        assertEquals(15, saved)
        assertEquals(15, bank.widthReel.value)
        assertEquals(20, bank.heightReel.value)
        bank.widthBox.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
        assertTrue(announced)
        assertTrue(bank.widthReel.contentDescription!!.contains("15"))
        assertTrue(bank.widthReel.isFocusable)
    }

    @Test
    fun dimmerZeroDrawsNoLaserAndFullDimmerKeepsAWhiteCore() {
        val off = LaserStrip.strokes(LaserStrip.DEFAULT_COLOR, 0, 2.5f)
        val on = LaserStrip.strokes(0xFFFF7B8B.toInt(), 100, 2.5f)
        assertTrue(off.isEmpty())
        assertTrue(on.any { it.core && LaserStrip.channel(it.color, 16) >= LaserStrip.CORE_MIN_CHANNEL })
        assertEquals(0xFF, LaserStrip.channel(on.first { !it.core }.color, 16))
    }

    private fun bank(
        width: Int,
        height: Int,
        onChange: (String, Int, Boolean) -> Unit,
    ) = SizeReelBank(
        context,
        NumberItem(LookKeys.WIDTH, "Width", "0 is the narrowest panel."),
        NumberItem(LookKeys.HEIGHT, "Height", "0 is the shortest panel."),
        width,
        height,
        laserSource = { LaserStrip.DEFAULT_COLOR to 100 },
        onChange = onChange,
        onNotch = {},
        onProblem = {},
    )

    private fun measure(bank: LinearLayout, widthPx: Int) {
        val width = MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY)
        val height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        bank.measure(width, height)
        bank.layout(0, 0, bank.measuredWidth, bank.measuredHeight)
    }
}
