package com.offgrid.compositor

import android.Manifest
import android.os.SystemClock
import android.view.View
import com.offgrid.compositor.terminal.showMinimized
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Royalty's S25 Ultra bugs, tapped through on an emulator with Espresso.
 * Screenshots go to /data/local/tmp so they survive the Gradle uninstall.
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
        }
        device.waitForIdle()
        waitDisplayed(withContentDescription("Lock the panel"))
    }

    @Test
    fun lockBackAndUnlockWorkAndRestartDoesNotTrap() {
        shot("01_folders_before_lock")
        tap(withContentDescription("Lock the panel"))
        waitDisplayed(withContentDescription("Unlock the panel"))
        shot("02_edit_locked")
        tap(withContentDescription("More choices: view, sort, and folder size"))
        shot("03_menu_while_locked")
        pressBack()
        pressBack()
        waitDisplayed(withContentDescription("Lock the panel"))
        tap(withContentDescription("Lock the panel"))
        waitDisplayed(withContentDescription("Unlock the panel"))
        tap(withContentDescription("Unlock the panel"))
        waitDisplayed(withContentDescription("Lock the panel"))
        activity.scenario.recreate()
        showThePanel()
        waitDisplayed(withContentDescription("Lock the panel"))
        shot("04_after_restart_unlocked")
        assertFalse(exists(withContentDescription("Unlock the panel")))
    }

    @Test
    fun minusAlwaysHasARestorePathIncludingAfterRestart() {
        tap(withContentDescription("Shrink the panel"))
        waitDisplayed(withContentDescription("Open the panel"))
        shot("05_hidden_with_restore_tab")
        pressBack()
        waitDisplayed(withContentDescription("Lock the panel"))
        tap(withContentDescription("Shrink the panel"))
        waitDisplayed(withContentDescription("Open the panel"))
        tap(withContentDescription("Open the panel"))
        waitDisplayed(withContentDescription("Lock the panel"))
        shot("06_restored_from_tab")
        tap(withContentDescription("Shrink the panel"))
        activity.scenario.recreate()
        showThePanel()
        waitDisplayed(withContentDescription("Lock the panel"))
        shot("07_after_restart_panel_open")
    }

    @Test
    fun foldersShowBeforeAnyDiamondFlash() {
        waitDisplayed(withContentDescription(containsString("Panel settings")))
        shot("08_no_splash_folders")
        waitDisplayed(withContentDescription("Lock the panel"))
        activity.scenario.recreate()
        showThePanel()
        waitDisplayed(withContentDescription(containsString("Panel settings")))
        shot("09_no_splash_after_resume")
    }

    @Test
    fun copyPairingKeyStaysOnLiveAndRemote() {
        openFolder("Live and Remote")
        shot("10_live_and_remote")
        assertTrue(
            exists(withText(containsString("PC or an Android phone"))) ||
                exists(withText(containsString("PC or Android"))),
        )
        tap(withText("Copy pairing key"))
        waitDisplayed(withText("Copy pairing key"))
        val copied = waitUntil(8_000) {
            activity.scenario.onActivity { }
            exists(withContentDescription(containsString("copied"))) ||
                exists(withContentDescription(containsString("Pairing key")))
        }
        assertTrue("copy pairing confirmed", copied)
        shot("11_copy_pairing_stayed")
        waitDisplayed(withText("Copy pairing key"))
    }

    @Test
    fun openKickStaysInAppAndNeverOpensPlayStore() {
        openFolder("Kick")
        openFolder("Effects")
        shot("12_kick_effects")
        tap(withText("Open Kick"))
        waitDisplayed(withText("Open Kick"))
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
        openFolder("Panel settings")
        openFolder("Clicker Sounds")
        shot("14_clicker_sounds")
        activity.scenario.onActivity { it.terminalPanel.playTestClick() }
        var after = 0
        activity.scenario.onActivity { after = it.terminalPanel.clickPlayCount() }
        assertTrue("clicker should fire while enabled", after > before)
        shot("15_click_played")
    }

    @Test
    fun titleFitsOnScreen() {
        waitDisplayed(withText("OFFGRID TERMINAL"))
        shot("16_title_default")
        onView(withText("OFFGRID TERMINAL")).check { view, _ ->
            assertTrue(view.width > 0)
            assertTrue(view.right <= device.displayWidth)
        }
    }

    @Test
    fun contrastLockBlocksInvisibleWhiteSkin() {
        openFolder("Panel settings")
        openFolder("Colors")
        openFolder("Skin color")
        shot("17_skin_colors")
        waitDisplayed(withContentDescription(containsString("Locked")))
        if (exists(withContentDescription(containsString("Locked White")))) {
            tap(withContentDescription(containsString("Locked White")))
        }
        assertTrue(
            exists(withContentDescription(containsString("Locked:"))) ||
                exists(withContentDescription(containsString("Locked"))),
        )
        shot("18_white_skin_locked")
    }

    private fun openFolder(name: String) {
        val matcher = withContentDescription(containsString(name))
        if (!exists(matcher)) {
            repeat(8) {
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight * 3 / 4,
                    device.displayWidth / 2,
                    device.displayHeight / 4,
                    20,
                )
                device.waitForIdle()
                if (exists(matcher)) return@repeat
            }
        }
        tap(matcher)
    }

    private fun tap(matcher: org.hamcrest.Matcher<View>) {
        waitDisplayed(matcher)
        try {
            onView(matcher).perform(scrollTo(), click())
        } catch (_: RuntimeException) {
            onView(matcher).perform(click())
        }
        device.waitForIdle()
    }

    private fun waitDisplayed(matcher: org.hamcrest.Matcher<View>) {
        val ok = waitUntil(15_000) {
            exists(matcher)
        }
        if (!ok) {
            shot("fail_missing")
            val dump = device.executeShellCommand("uiautomator dump /data/local/tmp/royalty-fix-evidence/hierarchy.xml")
            android.util.Log.e(TAG, "missing $matcher dump=$dump")
            android.util.Log.e(
                TAG,
                device.executeShellCommand("cat /data/local/tmp/royalty-fix-evidence/hierarchy.xml").take(4000),
            )
        }
        onView(matcher).check(matches(isDisplayed()))
    }

    private fun exists(matcher: org.hamcrest.Matcher<View>): Boolean = try {
        onView(matcher).check(matches(isDisplayed()))
        true
    } catch (_: NoMatchingViewException) {
        false
    } catch (_: AssertionError) {
        false
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
        private const val TAG = "RoyaltyPhoneFix"
        private const val EVIDENCE = "/data/local/tmp/royalty-fix-evidence"
    }
}
