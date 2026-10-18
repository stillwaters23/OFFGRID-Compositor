package com.offgrid.compositor

import android.Manifest
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.offgrid.compositor.terminal.showMinimized
import java.io.FileInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Royalty's S25 Ultra bugs, tapped on an emulator the way the app ships.
 *
 * The app may go immersive (Broadcast Lock). Tests never skip that and never
 * dismiss Android 15's "Viewing full screen / Got it" hint to make a tap work.
 * If that hint covers Lock, Minus, menu, Back, or Unlock, the test fails: the
 * app must keep those controls tappable.
 *
 * P0 lock and minus use UiDevice.click at the control's on-screen center.
 * performClick() is not enough for those. Off-screen chrome is an app bug.
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
            act.applySystemBars()
            act.window.decorView.requestFocus()
        }
        val lock = waitDesc("Lock the panel")
        if (lock == null) dumpUi("setup_missing_lock")
        assertTrue("lock control should be on the panel. seen=${visibleLabels()}", lock != null)
        assertTappableOnScreen(lock!!, "Lock the panel")
    }

    @Test
    fun activityReachesIdleSoEspressoCanSync() {
        val start = SystemClock.uptimeMillis()
        Espresso.onIdle()
        val elapsed = SystemClock.uptimeMillis() - start
        assertTrue("Espresso must sync to an idle activity. took=${elapsed}ms", elapsed < 20_000)
        shot("00_activity_idle")
    }

    @Test
    fun lockBackAndUnlockWorkAndRestartDoesNotTrap() {
        shot("01_folders_before_lock")
        assertChromeNotCoveredByFullscreenHint()
        tapOnScreen(requireDesc("Lock the panel"), "Lock the panel")
        assertTrue("lock tap must show Unlock", waitDesc("Unlock the panel") != null)
        shot("02_edit_locked")
        tapOnScreen(requireDesc("More choices: view, sort, and folder size"), "menu")
        shot("03_menu_while_locked")
        assertStatusClearOfMenu()
        device.pressBack()
        device.waitForIdle()
        SystemClock.sleep(300)
        device.pressBack()
        device.waitForIdle()
        SystemClock.sleep(300)
        assertTrue("Back must unlock. hint=${hintBounds()}", waitDesc("Lock the panel") != null)
        tapOnScreen(requireDesc("Lock the panel"), "Lock the panel")
        assertTrue(waitDesc("Unlock the panel") != null)
        tapOnScreen(requireDesc("Unlock the panel"), "Unlock the panel")
        assertTrue(waitDesc("Lock the panel") != null)
        activity.scenario.recreate()
        showThePanel()
        assertTrue(waitDesc("Lock the panel") != null)
        shot("04_after_restart_unlocked")
        assertTrue(findDesc("Unlock the panel") == null)
    }

    @Test
    fun broadcastLockImmersiveUnlockIsOnScreen() {
        val lock = requireDesc("Lock the panel")
        longPressOnScreen(lock, "Lock the panel")
        val unlock = waitDesc("Unlock")
        assertTrue("Broadcast Lock must leave an on-screen Unlock. hint=${hintBounds()}", unlock != null)
        shot("02b_broadcast_lock_immersive")
        assertTappableOnScreen(unlock!!, "Unlock strip")
        tapOnScreen(unlock, "Unlock strip")
        assertTrue("Unlock strip must restore the panel", waitDesc("Lock the panel") != null)
        shot("02c_after_broadcast_unlock")
    }

    @Test
    fun minusAlwaysHasARestorePathIncludingAfterRestart() {
        assertChromeNotCoveredByFullscreenHint()
        tapOnScreen(requireDesc("Shrink the panel"), "Shrink the panel")
        val restore = waitDesc("Open the panel")
        assertTrue("minus must leave a restore tab", restore != null)
        assertTappableOnScreen(restore!!, "Open the panel")
        shot("05_hidden_with_restore_tab")
        device.pressBack()
        device.waitForIdle()
        SystemClock.sleep(300)
        assertTrue("Back must restore the panel. hint=${hintBounds()}", waitDesc("Lock the panel") != null)
        tapOnScreen(requireDesc("Shrink the panel"), "Shrink the panel")
        tapOnScreen(requireDesc("Open the panel"), "Open the panel")
        assertTrue(waitDesc("Lock the panel") != null)
        shot("06_restored_from_tab")
        tapOnScreen(requireDesc("Shrink the panel"), "Shrink the panel")
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
        tapOnScreen(requireText("Copy pairing key"), "Copy pairing key")
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
        tapOnScreen(requireText("Open Kick"), "Open Kick")
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
        tapOnScreen(requireText("I got kicked"), "I got kicked")
        assertFalse(device.hasObject(By.pkg("com.android.vending")))
        assertTrue(waitTextContains("I got kicked") != null)
    }

    @Test
    fun reelClickSoundsFireWhenTheToggleIsOn() {
        device.executeShellCommand("logcat -c")
        val hooked = java.util.concurrent.atomic.AtomicInteger(0)
        activity.scenario.onActivity { act ->
            act.terminalPanel.clickerSounds.playHook = { id -> hooked.set(id) }
        }
        tapFolder("Panel settings")
        tapFolder("Clicker Sounds")
        shot("14_clicker_sounds")
        repeat(6) {
            activity.scenario.onActivity { it.terminalPanel.playTestClick() }
            if (waitUntil(2_000) { lastClickStreamId() > 0 }) return@repeat
        }
        val streamId = lastClickStreamId()
        assertTrue("SoundPool play hook must fire. hooked=${hooked.get()} stream=$streamId", streamId > 0 && hooked.get() > 0)
        val log = shellOutput("logcat -d -s OffgridClicker:I")
        assertTrue("logcat must show SoundPool play. log=$log", log.contains("SoundPool play streamId="))
        shot("15_click_played")
    }

    @Test
    fun titleFitsOnScreen() {
        val title = waitTextContains("OFFGRID TERMINAL")
        assertTrue(title != null)
        shot("16_title_default")
        assertTappableOnScreen(title!!, "title")
        val loc = IntArray(2)
        title.getLocationOnScreen(loc)
        assertTrue(title.width > 0)
        assertTrue(loc[0] + title.width <= device.displayWidth)
    }

    @Test
    fun contrastLockBlocksInvisibleWhiteSkin() {
        tapFolder("Panel settings")
        tapFolder("Colors")
        tapFolder("Skin color")
        shot("17_skin_colors")
        val lockedWhite = waitDescContains("Locked White")
        assertTrue("White skin must show the lock. seen=${visibleLabels()}", lockedWhite != null)
        tapOnScreen(lockedWhite!!, "Locked White")
        val typed = waitUntil(8_000) {
            statusDescription().contains("Locked:") && statusDescription().contains("white")
        }
        assertTrue("Termux must type the white-skin lock reason: ${statusDescription()}", typed)
        shot("18_white_skin_locked")
    }

    @Test
    fun contrastLockBlocksBlackTypewriterOnBlack() {
        tapFolder("Panel settings")
        tapFolder("Themes")
        tapFolder("Theme mode")
        tapOnScreen(requireText("Night"), "Night")
        backTowardHome()
        tapFolder("Panel settings")
        tapFolder("Colors")
        tapFolder("Typewriter text color")
        val lockedBlack = waitDescContains("Locked Black")
        assertTrue("Black typewriter on black must show the lock. seen=${visibleLabels()}", lockedBlack != null)
        tapOnScreen(lockedBlack!!, "Locked Black")
        val typed = waitUntil(8_000) {
            statusDescription().contains("Locked:") && statusDescription().contains("black")
        }
        assertTrue("Termux must type the black-on-black reason: ${statusDescription()}", typed)
        shot("18b_black_text_locked")
    }

    @Test
    fun viewMenuDoesNotCoverTheStatusLine() {
        tapOnScreen(requireDesc("More choices: view, sort, and folder size"), "menu")
        assertTrue(waitTextContains("Done") != null)
        assertStatusClearOfMenu()
        shot("06b_menu_clears_status")
        device.pressBack()
        device.waitForIdle()
    }

    /** Home list is long: Kick starts below the fold. A user swipe is allowed; silent scroll is not. */
    private fun tapFolder(name: String) {
        val label = "$name folder"
        var view = waitUntilView(2_000) { findOnScreen { findDescContains(label) } }
        if (view == null) {
            repeat(20) {
                swipePanel(up = true)
                view = findOnScreen { findDescContains(label) }
                if (view != null) return@repeat
            }
        }
        if (view == null) {
            repeat(20) {
                swipePanel(up = false)
                view = findOnScreen { findDescContains(label) }
                if (view != null) return@repeat
            }
        }
        if (view == null) dumpUi("offscreen_${name.replace(' ', '_')}")
        assertTrue(
            "$label is off-screen on a phone-sized display after swiping the folder list. " +
                "That is an app layout bug. seen=${visibleLabels()}",
            view != null,
        )
        tapOnScreen(view!!, label)
        SystemClock.sleep(500)
    }

    private fun tapOnScreen(view: View, name: String) {
        val (x, y) = assertTappableOnScreen(view, name)
        device.click(x, y)
        device.waitForIdle()
        SystemClock.sleep(400)
    }

    private fun longPressOnScreen(view: View, name: String) {
        val (x, y) = assertTappableOnScreen(view, name)
        device.swipe(x, y, x, y, 160)
        device.waitForIdle()
        SystemClock.sleep(500)
    }

    private fun assertTappableOnScreen(view: View, name: String): Pair<Int, Int> {
        val loc = IntArray(2)
        var w = 0
        var h = 0
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            view.getLocationOnScreen(loc)
            w = view.width
            h = view.height
        }
        val box = Rect(loc[0], loc[1], loc[0] + w, loc[1] + h)
        val x = box.centerX()
        val y = box.centerY()
        assertTrue(
            "$name is off-screen at $box display=${device.displayWidth}x${device.displayHeight}",
            view.isShown && box.width() > 4 && box.height() > 4 &&
                x in 1 until device.displayWidth && y in 1 until device.displayHeight,
        )
        val hint = hintBounds()
        if (hint != null) {
            assertFalse(
                "$name at $box is covered by the Android 15 fullscreen hint $hint. " +
                    "The app hid system bars while this control must stay tappable.",
                hint.contains(x, y) || Rect.intersects(hint, box),
            )
        }
        return x to y
    }

    private fun assertChromeNotCoveredByFullscreenHint() {
        val lock = requireDesc("Lock the panel")
        assertTappableOnScreen(lock, "Lock the panel")
        val minus = findDesc("Shrink the panel")
        if (minus != null) assertTappableOnScreen(minus, "Shrink the panel")
    }

    private fun hintBounds(): Rect? {
        val node = device.findObject(By.text("Got it"))
            ?: device.findObject(By.textContains("Got it"))
            ?: device.findObject(By.textContains("Viewing full screen"))
            ?: return null
        return node.visibleBounds
    }

    private fun requireDesc(desc: String): View {
        val view = waitDesc(desc)
        if (view == null) dumpUi("missing_${desc.replace(Regex("[^A-Za-z0-9]+"), "_").take(40)}")
        assertTrue("missing $desc. seen=${visibleLabels()} hint=${hintBounds()}", view != null)
        return view!!
    }

    private fun requireText(text: String): View {
        val view = waitTextContains(text)
        if (view == null) dumpUi("missing_${text.replace(Regex("[^A-Za-z0-9]+"), "_").take(40)}")
        assertTrue("missing text $text. seen=${visibleLabels()}", view != null)
        return view!!
    }

    private fun swipePanel(up: Boolean) {
        val w = device.displayWidth
        val h = device.displayHeight
        val start = if (up) (h * 0.62f).toInt() else (h * 0.38f).toInt()
        val end = if (up) (h * 0.48f).toInt() else (h * 0.52f).toInt()
        device.swipe(w / 2, start, w / 2, end, 18)
        device.waitForIdle()
        SystemClock.sleep(180)
    }

    private fun waitDesc(desc: String): View? =
        waitUntilView { findOnScreen { findDesc(desc) } }

    private fun waitDescContains(desc: String): View? =
        waitUntilView { findOnScreen { findDescContains(desc) } }

    private fun waitTextContains(text: String): View? =
        waitUntilView { findOnScreen { findTextContains(text) } }

    private fun waitUntilView(find: () -> View?): View? = waitUntilView(15_000, find)

    private fun waitUntilView(ms: Long, find: () -> View?): View? {
        var found: View? = null
        waitUntil(ms) {
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
            found = walk(act.window.decorView, match)
        }
        return found
    }

    private fun onScreen(view: View): Boolean {
        val rect = Rect()
        return view.isShown && view.getGlobalVisibleRect(rect) && rect.width() > 4 && rect.height() > 4
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

    private fun lastClickStreamId(): Int {
        var id = 0
        activity.scenario.onActivity { id = it.terminalPanel.lastClickStreamId() }
        return id
    }

    private fun backTowardHome() {
        repeat(8) {
            if (findOnScreen { findDescContains("Panel settings folder") } != null) return
            device.pressBack()
            device.waitForIdle()
            SystemClock.sleep(250)
        }
    }

    private fun assertStatusClearOfMenu() {
        var status = Rect()
        var menu = Rect()
        var statusShown = false
        activity.scenario.onActivity { act ->
            statusShown = act.terminalPanel.statusLine.isShown
            act.terminalPanel.statusLine.getGlobalVisibleRect(status)
            act.terminalPanel.menu.getGlobalVisibleRect(menu)
        }
        assertTrue("status line must stay on screen while the view menu is open", statusShown && status.height() > 4)
        val overlap = Rect(status)
        val covers = overlap.intersect(menu) && overlap.width() > 8 && overlap.height() > 8
        assertFalse(
            "view menu $menu must not cover the status line $status",
            covers || status.bottom > menu.top + 8 && status.right > menu.centerX(),
        )
    }

    private fun shellOutput(command: String): String {
        val stream = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        stream.use {
            return FileInputStream(it.fileDescriptor).bufferedReader().readText()
        }
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
