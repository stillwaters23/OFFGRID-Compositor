package com.offgrid.compositor.terminal

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.offgrid.compositor.shouldHideSystemBars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoyaltyLockHideTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun clearPrefs() {
        context.getSharedPreferences("terminal_panel", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("broadcast_lock", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun lockKeepsThePanelVisibleAndBackUnlocks() {
        val panel = attachedPanel()
        assertFalse(panel.editLocked)
        assertEquals(View.VISIBLE, panel.visibility)
        panel.toggleEditLock()
        assertTrue(panel.editLocked)
        assertEquals(View.VISIBLE, panel.visibility)
        assertEquals(View.VISIBLE, panel.editLockVeil.visibility)
        assertTrue(panel.backCallback.isEnabled)
        panel.backCallback.handleOnBackPressed()
        assertFalse(panel.editLocked)
        assertEquals(View.GONE, panel.editLockVeil.visibility)
    }

    @Test
    fun lockIsNotPersistedAcrossANewPanel() {
        val first = attachedPanel()
        first.toggleEditLock()
        assertTrue(first.editLocked)
        val second = attachedPanel()
        assertFalse(second.editLocked)
        assertEquals(View.VISIBLE, second.visibility)
        assertFalse(second.minimized)
    }

    @Test
    fun minusKeepsARestoreTabAndBackBringsThePanelBack() {
        val panel = attachedPanel()
        panel.showMinimized(true, animate = false)
        assertTrue(panel.minimized)
        assertEquals(View.VISIBLE, panel.visibility)
        assertEquals(View.VISIBLE, panel.pill.visibility)
        assertEquals(View.GONE, panel.expanded.visibility)
        assertTrue(panel.backCallback.isEnabled)
        panel.backCallback.handleOnBackPressed()
        assertFalse(panel.minimized)
        assertEquals(View.VISIBLE, panel.expanded.visibility)
        assertEquals(View.GONE, panel.pill.visibility)
    }

    @Test
    fun minusIsNotPersistedAcrossANewPanel() {
        val first = attachedPanel()
        first.showMinimized(true, animate = false)
        assertTrue(first.prefs.minimized == false)
        val second = attachedPanel()
        assertFalse(second.minimized)
        assertEquals(View.VISIBLE, second.expanded.visibility)
    }

    @Test
    fun systemBarsStayUpUnlessBroadcastLockHidThePanel() {
        assertFalse(shouldHideSystemBars(false))
        assertTrue(shouldHideSystemBars(true))
    }

    @Test
    fun topBarTitleDoesNotKeepRequestingLayout() {
        val panel = attachedPanel()
        assertFalse(panel.topBar.isLayoutRequested)
        panel.topBar.measure(
            View.MeasureSpec.makeMeasureSpec(panel.topBar.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(panel.topBar.height, View.MeasureSpec.EXACTLY),
        )
        panel.topBar.layout(0, 0, panel.topBar.width, panel.topBar.height)
        assertFalse("title textSize in onLayout must not request another pass", panel.topBar.isLayoutRequested)
    }

    private fun attachedPanel(): TerminalPanel {
        val panel = TerminalPanel(context)
        val host = FrameLayout(context)
        host.addView(panel)
        host.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, 1080, 1920)
        return panel
    }
}
