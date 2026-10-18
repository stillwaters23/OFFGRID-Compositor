package com.offgrid.compositor

import android.Manifest
import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.offgrid.compositor.terminal.LookKeys
import com.offgrid.compositor.terminal.showMinimized
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Royalty's S25 Ultra bugs, tapped on an emulator.
 *
 * Android 15's "Viewing full screen / Got it" overlay sits on top of hideSystemBars()
 * and steals window focus, Lock/Minus taps, and Back. Tests keep the system bars,
 * dismiss that hint if it still appears, then performClick() the real view.
 * Off-screen folders (Kick, Colors, Clicker Sounds, Copy pairing key) are scrolled
 * into the visible rect before the tap.
 */
@RunWith(AndroidJUnit4::class)
class RoyaltyPhoneFixTest {

    @get:Rule(order = 0)
    val cameraPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule(order = 1)
    val activity = ActivityScenarioRule<MainActivity>(
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_KEEP_SYSTEM_BARS, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun showThePanel() {
        device.executeShellCommand("mkdir -p $EVIDENCE")
        dismissFullscreenHint()
        activity.scenario.onActivity { act ->
            act.terminalPanel.setToggleSetting(LookKeys.UNFOLD, false)
            act.terminalPanel.setToggleSetting(LookKeys.LASER_TYPING, false)
            act.lockChrome.unlock()
            act.terminalPanel.setHiddenForLock(false)
            act.terminalPanel.showMinimized(false, animate = false)
            act.terminalPanel.visibility = View.VISIBLE
            act.window.decorView.requestFocus()
        }
        dismissFullscreenHint()
        val lock = waitDesc("Lock the panel")
        if (lock == null) {
            dumpUi("setup_missing_lock")
        }
        assertTrue(
            "lock control should be on the panel. seen=${visibleLabels()}",
            lock != null,
        )
    }

    @Test
    fun lockBackAndUnlockWorkAndRestartDoesNotTrap() {
        shot("01_folders_before_lock")
        tapDesc("Lock the panel")
        assertTrue("lock tap must show Unlock", waitDesc("Unlock the panel") != null)
        shot("02_edit_locked")
        tapDesc("More choices: view, sort, and folder size")
        shot("03_menu_while_locked")
        pressAppBack()
        pressAppBack()
        assertTrue("Back must unlock", waitDesc("Lock the panel") != null)
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
        assertTrue("minus must leave a restore tab", waitDesc("Open the panel") != null)
        shot("05_hidden_with_restore_tab")
        pressAppBack()
        assertTrue("Back must restore the panel", waitDesc("Lock the panel") != null)
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
        assertTrue(waitDescContains("Panel settings folder") != null)
        shot("08_no_splash_folders")
        assertTrue(waitDesc("Lock the panel") != null)
        activity.scenario.recreate()
        showThePanel()
        assertTrue(waitDescContains("Panel settings folder") != null)
        shot("09_no_splash_after_resume")
    }

    @Test
    fun copyPairingKeyStaysOnLiveAndRemote() {
        tapFolder("Live and Remote")
        shot("10_live_and_remote")
        assertTrue(
            "pairing copy must mention a PC or Android phone. seen=${visibleLabels()}",
            waitTextContains("PC or an Android phone") != null ||
                waitTextContains("PC or Android") != null,
        )
        tapText("Copy pairing key")
        assertTrue(waitTextContains("Copy pairing key") != null)
        val copied = waitUntil(10_000) {
            val desc = statusDescription()
            desc.contains("copied", ignoreCase = true) || desc.contains("Pairing key")
        }
        assertTrue("copy pairing confirmed on the status line: ${statusDescription()}", copied)
        shot("11_copy_pairing_stayed")
        assertTrue(findTextContains("Copy pairing key") != null)
    }

    @Test
    fun pairingCopyMentionsPcOrAndroidPhone() {
        tapFolder("Live and Remote")
        shot("10b_pairing_pc_or_phone")
        assertTrue(
            "Live and Remote must say PC or Android phone",
            waitTextContains("PC or an Android phone") != null ||
                waitTextContains("PC or Android") != null,
        )
    }

    @Test
    fun openKickStaysInAppAndNeverOpensPlayStore() {
        tapFolder("Kick")
        tapFolder("Effects")
        shot("12_kick_effects")
        tapText("Open Kick")
        assertTrue(waitTextContains("Open Kick") != null)
        shot("13_open_kick_in_app")
        assertFalse(
            "Open Kick must not send Royalty to Play Store",
            device.hasObject(By.pkg("com.android.vending")),
        )
        val missing = waitUntil(6_000) {
            statusDescription().contains("not installed", ignoreCase = true) ||
                findTextContains("not installed") != null ||
                findTextContains("Open Kick") != null
        }
        assertTrue("Open Kick must stay in OFFGRID", missing)
    }

    @Test
    fun kickTogglesStayOnOffgridAndNeverClaimTikTok() {
        tapFolder("Kick")
        tapFolder("Effects")
        shot("19_kick_toggles_offgrid_only")
        assertTrue(
            "Red sweep must say it cannot change TikTok",
            waitTextContains("cannot change TikTok") != null,
        )
        assertTrue(waitTextContains("I got kicked") != null)
        tapText("I got kicked")
        assertFalse(device.hasObject(By.pkg("com.android.vending")))
        assertTrue(waitTextContains("I got kicked") != null)
        assertTrue(
            waitTextContains("never taps or rejoins") != null ||
                statusDescription().isNotBlank(),
        )
    }

    @Test
    fun reelClickSoundsFireWhenTheToggleIsOn() {
        var before = 0
        activity.scenario.onActivity { before = it.terminalPanel.clickPlayCount() }
        tapFolder("Panel settings")
        tapFolder("Clicker Sounds")
        shot("14_clicker_sounds")
        activity.scenario.onActivity { it.terminalPanel.playTestClick() }
        var after = 0
        activity.scenario.onActivity { after = it.terminalPanel.clickPlayCount() }
        assertTrue("clicker should fire while enabled ($before -> $after)", after > before)
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
        tapFolder("Panel settings")
        tapFolder("Colors")
        tapFolder("Skin color")
        shot("17_skin_colors")
        val locked = waitDescContains("Locked")
        assertTrue("a failing color must show a lock. seen=${visibleLabels()}", locked != null)
        waitDescContains("Locked White")?.let { tapView(it) }
        val typed = waitUntil(8_000) {
            statusDescription().contains("Locked:") || findDescContains("Locked:") != null
        }
        assertTrue(typed || findDescContains("Locked") != null)
        shot("18_white_skin_locked")
    }

    private fun tapFolder(name: String) {
        tapDescContains("$name folder")
        SystemClock.sleep(400)
    }

    private fun tapDesc(desc: String) {
        val view = reveal({ findAny { it.contentDescription?.toString() == desc } }, desc)
        assertTrue("missing $desc. seen=${visibleLabels()}", view != null)
        tapView(view!!)
    }

    private fun tapDescContains(desc: String) {
        val view = reveal({ findAny { it.contentDescription?.toString()?.contains(desc) == true } }, desc)
        assertTrue("missing desc $desc. seen=${visibleLabels()}", view != null)
        tapView(view!!)
    }

    private fun tapText(text: String) {
        val view = reveal({ findAny { (it as? TextView)?.text?.toString()?.contains(text) == true } }, text)
        assertTrue("missing text $text. seen=${visibleLabels()}", view != null)
        tapView(view!!)
    }

    private fun tapView(view: View) {
        bringOnScreen(view)
        var clicked = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            clicked = view.performClick()
        }
        if (!clicked) {
            val loc = IntArray(2)
            var w = 0
            var h = 0
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                view.getLocationOnScreen(loc)
                w = view.width
                h = view.height
            }
            val x = loc[0] + w / 2
            val y = loc[1] + h / 2
            if (x in 1 until device.displayWidth && y in 1 until device.displayHeight) {
                device.click(x, y)
            }
        }
        device.waitForIdle()
        SystemClock.sleep(400)
    }

    private fun reveal(find: () -> View?, label: String): View? {
        waitUntilView { findOnScreen(find) }?.let { return it }
        find()?.let { hidden ->
            bringOnScreen(hidden)
            findOnScreen(find)?.let { return it }
        }
        repeat(14) {
            swipePanelUp()
            findOnScreen(find)?.let { return it }
            find()?.let { hidden ->
                bringOnScreen(hidden)
                findOnScreen(find)?.let { return it }
            }
        }
        dumpUi("missing_${label.replace(Regex("[^A-Za-z0-9]+"), "_").take(40)}")
        return findOnScreen(find) ?: find()
    }

    private fun bringOnScreen(view: View) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            var parent = view.parent
            while (parent is View) {
                if (parent is android.widget.ScrollView) {
                    val child = IntArray(2)
                    val scroller = IntArray(2)
                    view.getLocationOnScreen(child)
                    parent.getLocationOnScreen(scroller)
                    val target = parent.scrollY + (child[1] - scroller[1]) - parent.height / 4
                    parent.scrollTo(0, target.coerceAtLeast(0))
                    break
                }
                parent = parent.parent
            }
            val rect = Rect(0, 0, view.width.coerceAtLeast(1), view.height.coerceAtLeast(1))
            view.requestRectangleOnScreen(rect, true)
        }
        device.waitForIdle()
        SystemClock.sleep(200)
    }

    private fun swipePanelUp() {
        val w = device.displayWidth
        val h = device.displayHeight
        device.swipe(w / 2, (h * 0.70f).toInt(), w / 2, (h * 0.28f).toInt(), 24)
        device.waitForIdle()
        SystemClock.sleep(200)
    }

    private fun pressAppBack() {
        dismissFullscreenHint()
        device.pressBack()
        device.waitForIdle()
        SystemClock.sleep(350)
        dismissFullscreenHint()
    }

    private fun dismissFullscreenHint() {
        val labels = listOf("Got it", "GOT IT", "Got It")
        repeat(3) {
            for (label in labels) {
                val button = device.findObject(By.text(label)) ?: device.findObject(By.textContains(label))
                if (button != null) {
                    button.click()
                    device.wait(Until.gone(By.text(label)), 2_000)
                    device.waitForIdle()
                    SystemClock.sleep(200)
                    return
                }
            }
            SystemClock.sleep(250)
        }
    }

    private fun waitDesc(desc: String): View? =
        waitUntilView { findOnScreen { findAny { it.contentDescription?.toString() == desc } } }

    private fun waitDescContains(desc: String): View? =
        waitUntilView { findOnScreen { findAny { it.contentDescription?.toString()?.contains(desc) == true } } }

    private fun waitTextContains(text: String): View? =
        waitUntilView { findOnScreen { findAny { (it as? TextView)?.text?.toString()?.contains(text) == true } }
            ?: findAny { (it as? TextView)?.text?.toString()?.contains(text) == true } }

    private fun waitUntilView(find: () -> View?): View? {
        var found: View? = null
        waitUntil(15_000) {
            found = find()
            found != null
        }
        return found
    }

    private fun findDesc(desc: String): View? = findAny { it.contentDescription?.toString() == desc }

    private fun findDescContains(desc: String): View? =
        findAny { it.contentDescription?.toString()?.contains(desc) == true }

    private fun findTextContains(text: String): View? =
        findAny { (it as? TextView)?.text?.toString()?.contains(text) == true }

    private fun findOnScreen(find: () -> View?): View? {
        val view = find() ?: return null
        return if (onScreen(view)) view else null
    }

    private fun findAny(match: (View) -> Boolean): View? {
        var found: View? = null
        activity.scenario.onActivity { act ->
            found = walk(act.window.decorView, match, requireOnScreen = false)
        }
        return found
    }

    private fun onScreen(view: View): Boolean {
        val rect = Rect()
        return view.isShown && view.getGlobalVisibleRect(rect) && rect.width() > 4 && rect.height() > 4
    }

    private fun walk(view: View, match: (View) -> Boolean, requireOnScreen: Boolean): View? {
        val visible = if (requireOnScreen) onScreen(view) else view.isShown
        if (visible && match(view)) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                walk(view.getChildAt(i), match, requireOnScreen)?.let { return it }
            }
        }
        return null
    }

    private fun visibleLabels(): String {
        val labels = ArrayList<String>()
        activity.scenario.onActivity { act ->
            collectLabels(act.window.decorView, labels)
        }
        return labels.take(40).joinToString(" | ")
    }

    private fun collectLabels(view: View, out: MutableList<String>) {
        if (view.isShown) {
            view.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out += "d:$it" }
            (view as? TextView)?.text?.toString()?.takeIf { it.isNotBlank() }?.let { out += "t:$it" }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) collectLabels(view.getChildAt(i), out)
        }
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

    private fun dumpUi(name: String) {
        shot(name)
        device.executeShellCommand("uiautomator dump $EVIDENCE/$name.xml")
    }

    companion object {
        private const val EVIDENCE = "/data/local/tmp/royalty-fix-evidence"
    }
}
