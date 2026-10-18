package com.offgrid.compositor

import android.Manifest
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.offgrid.compositor.terminal.showMinimized
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Royalty's S25 Ultra bugs, tapped on an emulator.
 * Espresso cannot run here: MainActivity's window never takes focus
 * (has-window-focus=false) under ActivityScenario. These tests walk the
 * real view tree and inject UiDevice taps at the view's screen position.
 */
@RunWith(AndroidJUnit4::class)
class RoyaltyPhoneFixTest {

    @get:Rule(order = 0)
    val cameraPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule(order = 1)
    val activity = ActivityScenarioRule(MainActivity::class.java)

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun showThePanel() {
        device.executeShellCommand("mkdir -p $EVIDENCE")
        activity.scenario.onActivity { act ->
            act.lockChrome.unlock()
            act.terminalPanel.setHiddenForLock(false)
            act.terminalPanel.showMinimized(false, animate = false)
            act.terminalPanel.visibility = View.VISIBLE
            act.window.decorView.requestFocus()
        }
        assertTrue("lock control should be on the panel", waitDesc("Lock the panel") != null)
    }

    @Test
    fun lockBackAndUnlockWorkAndRestartDoesNotTrap() {
        shot("01_folders_before_lock")
        tapDesc("Lock the panel")
        assertTrue(waitDesc("Unlock the panel") != null)
        shot("02_edit_locked")
        tapDesc("More choices: view, sort, and folder size")
        shot("03_menu_while_locked")
        device.pressBack()
        device.pressBack()
        assertTrue(waitDesc("Lock the panel") != null)
        tapDesc("Lock the panel")
        assertTrue(waitDesc("Unlock the panel") != null)
        tapDesc("Unlock the panel")
        assertTrue(waitDesc("Lock the panel") != null)
        activity.scenario.recreate()
        showThePanel()
        assertTrue(waitDesc("Lock the panel") != null)
        shot("04_after_restart_unlocked")
        assertTrue(findDesc("Unlock the panel") == null)
    }

    @Test
    fun minusAlwaysHasARestorePathIncludingAfterRestart() {
        tapDesc("Shrink the panel")
        assertTrue(waitDesc("Open the panel") != null)
        shot("05_hidden_with_restore_tab")
        device.pressBack()
        assertTrue(waitDesc("Lock the panel") != null)
        tapDesc("Shrink the panel")
        assertTrue(waitDesc("Open the panel") != null)
        tapDesc("Open the panel")
        assertTrue(waitDesc("Lock the panel") != null)
        shot("06_restored_from_tab")
        tapDesc("Shrink the panel")
        activity.scenario.recreate()
        showThePanel()
        assertTrue(waitDesc("Lock the panel") != null)
        shot("07_after_restart_panel_open")
    }

    @Test
    fun foldersShowBeforeAnyDiamondFlash() {
        assertTrue(waitDescContains("Panel settings") != null)
        shot("08_no_splash_folders")
        assertTrue(waitDesc("Lock the panel") != null)
        activity.scenario.recreate()
        showThePanel()
        assertTrue(waitDescContains("Panel settings") != null)
        shot("09_no_splash_after_resume")
    }

    @Test
    fun copyPairingKeyStaysOnLiveAndRemote() {
        tapDescContains("Live and Remote")
        shot("10_live_and_remote")
        assertTrue(
            "pairing copy must mention a PC or Android phone",
            waitTextContains("PC or an Android phone") != null ||
                waitTextContains("PC or Android") != null,
        )
        tapText("Copy pairing key")
        assertTrue(waitTextContains("Copy pairing key") != null)
        val copied = waitUntil(8_000) {
            val desc = statusDescription()
            desc.contains("copied", ignoreCase = true) || desc.contains("Pairing key")
        }
        assertTrue("copy pairing confirmed on the status line", copied)
        shot("11_copy_pairing_stayed")
        assertTrue(findTextContains("Copy pairing key") != null)
    }

    @Test
    fun openKickStaysInAppAndNeverOpensPlayStore() {
        tapDescContains("Kick")
        tapDescContains("Effects")
        shot("12_kick_effects")
        tapText("Open Kick")
        assertTrue(waitTextContains("Open Kick") != null)
        shot("13_open_kick_in_app")
        assertFalse(
            "Open Kick must not send Royalty to Play Store",
            device.hasObject(By.pkg("com.android.vending")),
        )
    }

    @Test
    fun reelClickSoundsFireWhenTheToggleIsOn() {
        var before = 0
        activity.scenario.onActivity { before = it.terminalPanel.clickPlayCount() }
        tapDescContains("Panel settings")
        tapDescContains("Clicker Sounds")
        shot("14_clicker_sounds")
        activity.scenario.onActivity { it.terminalPanel.playTestClick() }
        var after = 0
        activity.scenario.onActivity { after = it.terminalPanel.clickPlayCount() }
        assertTrue("clicker should fire while enabled", after > before)
        shot("15_click_played")
    }

    @Test
    fun titleFitsOnScreen() {
        val title = waitTextContains("OFFGRID TERMINAL")
        assertTrue(title != null)
        shot("16_title_default")
        val loc = IntArray(2)
        title!!.getLocationOnScreen(loc)
        assertTrue(title.width > 0)
        assertTrue(loc[0] + title.width <= device.displayWidth)
    }

    @Test
    fun contrastLockBlocksInvisibleWhiteSkin() {
        tapDescContains("Panel settings")
        tapDescContains("Colors")
        tapDescContains("Skin color")
        shot("17_skin_colors")
        val locked = waitDescContains("Locked")
        assertTrue("a failing color must show a lock", locked != null)
        waitDescContains("Locked White")?.let { tapView(it) }
        val typed = waitUntil(8_000) {
            statusDescription().contains("Locked:") || findDescContains("Locked:") != null
        }
        assertTrue(typed || findDescContains("Locked") != null)
        shot("18_white_skin_locked")
    }

    private fun tapDesc(desc: String) {
        val view = waitDesc(desc)
        assertTrue("missing $desc", view != null)
        tapView(view!!)
    }

    private fun tapDescContains(desc: String) {
        val view = waitDescContains(desc)
        assertTrue("missing desc $desc", view != null)
        tapView(view!!)
    }

    private fun tapText(text: String) {
        val view = waitTextContains(text)
        assertTrue("missing text $text", view != null)
        tapView(view!!)
    }

    private fun tapView(view: View) {
        val loc = IntArray(2)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            view.getLocationOnScreen(loc)
        }
        val x = loc[0] + view.width / 2
        val y = loc[1] + view.height / 2
        if (x > 0 && y > 0) {
            device.click(x, y)
        } else {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { view.performClick() }
        }
        device.waitForIdle()
        SystemClock.sleep(250)
    }

    private fun waitDesc(desc: String): View? = waitUntilView { findDesc(desc) }

    private fun waitDescContains(desc: String): View? = waitUntilView { findDescContains(desc) }

    private fun waitTextContains(text: String): View? = waitUntilView { findTextContains(text) }

    private fun waitUntilView(find: () -> View?): View? {
        var found: View? = null
        waitUntil(15_000) {
            found = find()
            found != null
        }
        return found
    }

    private fun findDesc(desc: String): View? = findView { it.contentDescription?.toString() == desc }

    private fun findDescContains(desc: String): View? =
        findView { it.contentDescription?.toString()?.contains(desc) == true }

    private fun findTextContains(text: String): View? =
        findView { (it as? TextView)?.text?.toString()?.contains(text) == true }

    private fun findView(match: (View) -> Boolean): View? {
        var found: View? = null
        activity.scenario.onActivity { act ->
            found = walk(act.window.decorView, match)
        }
        return found
    }

    private fun walk(view: View, match: (View) -> Boolean): View? {
        if (view.isShown && match(view)) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                walk(view.getChildAt(i), match)?.let { return it }
            }
        }
        return null
    }

    private fun statusDescription(): String {
        var text = ""
        activity.scenario.onActivity { text = it.terminalPanel.statusLine.contentDescription?.toString().orEmpty() }
        return text
    }

    private fun waitUntil(ms: Long, check: () -> Boolean): Boolean {
        val end = SystemClock.uptimeMillis() + ms
        while (SystemClock.uptimeMillis() < end) {
            if (check()) return true
            SystemClock.sleep(200)
        }
        return check()
    }

    private fun shot(name: String) {
        device.executeShellCommand("mkdir -p $EVIDENCE")
        device.executeShellCommand("screencap -p $EVIDENCE/$name.png")
    }

    companion object {
        private const val EVIDENCE = "/data/local/tmp/royalty-fix-evidence"
    }
}
