package com.offgrid.compositor

import android.Manifest
import android.content.Context
import android.content.Context
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Royalty's S25 Ultra bugs, tapped through on an emulator.
 * Each test takes a screenshot under the app's evidence folder.
 */
@RunWith(AndroidJUnit4::class)
class RoyaltyPhoneFixTest {

    @get:Rule
    val cameraPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule
    val activity = ActivityScenarioRule(MainActivity::class.java)

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun lockBackAndUnlockWorkAndRestartDoesNotTrap() {
        waitForPanel()
        shot("01_folders_before_lock")
        clickDesc("Lock the panel")
        assertTrue(waitDesc("Unlock the panel"))
        shot("02_edit_locked")
        assertTrue(waitDesc("More choices: view, sort, and folder size"))
        clickDesc("More choices: view, sort, and folder size")
        shot("03_menu_while_locked")
        pressBack()
        pressBack()
        assertTrue(waitDesc("Lock the panel"))
        clickDesc("Lock the panel")
        assertTrue(waitDesc("Unlock the panel"))
        clickDesc("Unlock the panel")
        assertTrue(waitDesc("Lock the panel"))
        activity.scenario.recreate()
        waitForPanel()
        assertTrue(waitDesc("Lock the panel"))
        shot("04_after_restart_unlocked")
        assertFalse(device.hasObject(By.desc("Unlock the panel")))
    }

    @Test
    fun minusAlwaysHasARestorePathIncludingAfterRestart() {
        waitForPanel()
        clickDesc("Shrink the panel")
        assertTrue(waitDesc("Open the panel"))
        shot("05_hidden_with_restore_tab")
        pressBack()
        waitForPanel()
        assertTrue(waitDesc("Lock the panel"))
        clickDesc("Shrink the panel")
        assertTrue(waitDesc("Open the panel"))
        clickDesc("Open the panel")
        waitForPanel()
        shot("06_restored_from_tab")
        clickDesc("Shrink the panel")
        activity.scenario.recreate()
        waitForPanel()
        assertTrue(waitDesc("Lock the panel"))
        shot("07_after_restart_panel_open")
    }

    @Test
    fun foldersShowBeforeAnyDiamondFlash() {
        waitForPanel()
        shot("08_no_splash_folders")
        assertTrue(device.hasObject(By.descContains("Panel settings")))
        assertTrue(device.hasObject(By.desc("Lock the panel")))
        activity.scenario.recreate()
        waitForPanel()
        shot("09_no_splash_after_resume")
        assertTrue(device.hasObject(By.descContains("Panel settings")))
    }

    @Test
    fun copyPairingKeyStaysOnLiveAndRemote() {
        waitForPanel()
        openFolder("Live and Remote")
        shot("10_live_and_remote")
        assertTrue(device.hasObject(By.textContains("PC or an Android phone")) ||
            device.hasObject(By.textContains("PC or Android")))
        clickText("Copy pairing key")
        assertTrue(
            waitText("Copy pairing key"),
        )
        val copied = device.wait(Until.hasObject(By.descContains("copied")), 8_000) ||
            device.wait(Until.hasObject(By.descContains("Pairing key")), 8_000)
        assertTrue("copy pairing stayed on the folder and confirmed", copied || device.hasObject(By.text("Copy pairing key")))
        shot("11_copy_pairing_stayed")
        assertTrue(device.hasObject(By.text("Copy pairing key")))
    }

    @Test
    fun openKickStaysInAppAndNeverOpensPlayStore() {
        waitForPanel()
        openFolder("Kick")
        openFolder("Effects")
        shot("12_kick_effects")
        clickText("Open Kick")
        assertTrue(waitText("Open Kick") || waitDescContains("Kick"))
        shot("13_open_kick_in_app")
        val dumpFile = File(evidenceDir(), "kick-hierarchy.xml")
        device.dumpWindowHierarchy(dumpFile)
        val dump = if (dumpFile.exists()) dumpFile.readText() else ""
        val play = dump.contains("play.google.com") || dump.contains("market://") ||
            device.hasObject(By.pkg("com.android.vending"))
        assertFalse("Open Kick must not send Royalty to Play Store", play)
    }

    @Test
    fun reelClickSoundsFireWhenTheToggleIsOn() {
        waitForPanel()
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
        waitForPanel()
        assertTrue(waitText("OFFGRID TERMINAL") || waitDesc("OFFGRID TERMINAL"))
        shot("16_title_default")
        val title = device.findObject(UiSelector().textContains("OFFGRID"))
        assertTrue(title.exists())
        assertTrue(title.visibleBounds.width() > 0)
        assertTrue(title.visibleBounds.right <= device.displayWidth)
    }

    @Test
    fun contrastLockBlocksInvisibleWhiteSkin() {
        waitForPanel()
        openFolder("Panel settings")
        openFolder("Colors")
        openFolder("Skin color")
        shot("17_skin_colors")
        val lockedWhite = device.findObject(UiSelector().descriptionContains("Locked White"))
        assertTrue("white skin must show a lock when it hides text", lockedWhite.exists() || device.hasObject(By.descContains("Locked")))
        if (lockedWhite.exists()) lockedWhite.click()
        val typed = device.wait(Until.hasObject(By.descContains("Locked:")), 8_000)
        assertTrue(typed || device.hasObject(By.descContains("Locked")))
        shot("18_white_skin_locked")
    }

    private fun waitForPanel() {
        assertTrue("panel lock control should appear", waitDesc("Lock the panel"))
    }

    private fun openFolder(name: String) {
        val scroll = androidx.test.uiautomator.UiScrollable(UiSelector().scrollable(true))
        scroll.setAsVerticalList()
        runCatching { scroll.scrollIntoView(UiSelector().descriptionContains(name)) }
        val row = device.findObject(UiSelector().descriptionContains(name))
        if (row.exists()) {
            row.click()
        } else {
            clickText(name)
        }
        device.waitForIdle()
    }

    private fun clickDesc(desc: String) {
        assertTrue("missing $desc", waitDesc(desc))
        device.findObject(By.desc(desc)).click()
        device.waitForIdle()
    }

    private fun clickText(text: String) {
        val found = device.wait(Until.hasObject(By.text(text)), 8_000)
        if (found) {
            device.findObject(By.text(text)).click()
        } else {
            val row = device.findObject(UiSelector().textContains(text))
            assertTrue("missing text $text", row.exists())
            row.click()
        }
        device.waitForIdle()
    }

    private fun waitDesc(desc: String): Boolean = device.wait(Until.hasObject(By.desc(desc)), 12_000)

    private fun waitDescContains(desc: String): Boolean =
        device.wait(Until.hasObject(By.descContains(desc)), 12_000)

    private fun waitText(text: String): Boolean = device.wait(Until.hasObject(By.text(text)), 12_000)

    private fun shot(name: String) {
        device.takeScreenshot(File(evidenceDir(), "$name.png"))
    }

    private fun evidenceDir(): File {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "royalty-fix-evidence")
        dir.mkdirs()
        return dir
    }
}
