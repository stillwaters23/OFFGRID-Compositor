# Independent QA — PR #56 (Royalty v0.3.3 phone fixes)

Tester: independent of the fixer. The fixer's log and CI screenshots were not treated as proof.

- Pull request: https://github.com/stillwaters23/OFFGRID-Compositor/pull/56
- Claimed head: `ced2b69d4afd58dc3b388a59d66d5c22e558ed70`
- Tree actually installed: `a3f0bf1b4c762750f8b7638502285ec47580f334` ("Record green emulator UI CI on ced2b69")
- App-code difference versus `ced2b69`: none. `git diff ced2b69` is two added lines in `docs/bugs/2026_10_17 v0.3.3 fix log.md` only.
- Debug APK built on this machine: `04 Build/app/build/outputs/apk/debug/app-debug.apk` (about 68 MB), `assembleDebug` succeeded.
- Device: Android emulator, API 35 (`sdk_gphone64_x86_64`, Android 15), AVD `qa_phone_35`, 1080×2400 at 420 dpi (Pixel-class phone). Nested KVM is broken on this VM (`kvm_arch_vcpu_create` BUG), so the emulator ran with `-accel off` (TCG). GPU was `swiftshader_indirect`, then `guest` after the framebuffer wedged.
- `settings get secure immersive_mode_confirmations` was empty for the whole run. The "Viewing full screen / Got it" hint was left armed. It never drew on this emulator.
- `adb shell wm size 1080x2340` made every window `isVisible=false` and dropped taps ("no touchable window"). Size was reset. All passing taps below are at the native 1080×2400.
- Taps were `adb shell input tap` / `input motionevent` / `input keyevent`. `performClick` was not used. `uiautomator dump` exits 137 (killed) whenever the folder panel is up, so hierarchy dumps were not usable. Coordinates came from `dumpsys activity top` view bounds and from pixels in screenshots.
- Date of this run: 2026-10-18.

Round 2 retested head `ffaec30` on the same phone. That section is at the bottom. Its overall verdict is **FAIL**, and it replaces the verdict below. The table in this section is Round 1 only.

Round 3 retested head `6c7936e` on the nested emulator. That section follows Round 2. Its phone verdict is **FAIL**. Round 1 and Round 2 stay in this file as written.

A calibration section follows Round 3. Same nested emulator. The environment verdict there is **UNFIT**.

Round 4 is the KVM retest of head `6c7936e`. Its overall verdict is **FAIL**: the Width reel moved from 100 to 96 and the click log stayed empty. The Round 3 cold-start ANR did not repeat on that runner.

Round 5 retests head `2fea30e` on the same kind of KVM runner. The first attempt (run 39150242408) is **BLOCKED**: the emulator left adb after the first Width swipe. The Round 5 retry is the current verdict. Device loss on that swipe was a runner flake. The completing job's phone verdict is **PASS**.

**Overall verdict (Round 1): FAIL.**

P1-7 still shows a splash before the folders. P1-2 and P1-3 do not deliver the kick behavior Royalty asked for. Several other items could not be finished because the emulator's GPU and input services died under load. A known product limitation is not counted as a pass.

## Verdict table

| Item | Verdict | What was shown on this device |
| --- | --- | --- |
| P0-1 Lock | PASS | Edit lock keeps the panel. Back unlocks. The menu opens while locked. Broadcast Lock shows a bottom Unlock strip that restores the panel. |
| P0-2 Minus | PASS | Minus hides the panel and leaves a visible OFFGRID tab. Tapping the tab restores the panel. |
| P1-1 Open Kick | BLOCKED | Kick was tapped. The next frame was black and the process died before Open Kick could be confirmed. Play Store was not observed. |
| P1-2 I got kicked | FAIL | No flood of "user kicked" lines. The action is still one status sentence plus an optional sweep on OFFGRID's own video. The switch was not removed. |
| P1-3 Red sweep | FAIL | The switch is still in the Kick Effects folder. It is documented as unable to change TikTok or any other app. No sweep was seen on this emulator (no usable front camera). |
| P1-4 Copy pairing key | PASS | Stays on Live and Remote. The status line finishes as "Pairing key made and copied. Paste it on a PC or an Android phone." |
| P1-5 PC or Android phone | PASS | The open folder says pairing works for a PC or an Android phone. |
| P1-6 Click sounds | BLOCKED | A swipe on the W control created no `com.offgrid.compositor` audio session. The emulator also has no PulseAudio driver, so a short blip could have been missed. |
| P1-7 Splash | FAIL | After force-stop and relaunch, the first drawn frames are the launcher-icon splash (white circle, black square, green dot) on black. Folders come after. |
| P1-8 White skin | BLOCKED | The skin screen was not reached before the emulator wedged. Not passed from CI. |
| P1-9 Black on black | BLOCKED | A locked swatch was not tapped, so the typed lock reason was not seen. Not passed from CI. |
| P1-10 Title fit | PASS | At the default size on this phone, "OFFGRID TERMINAL" wraps onto two lines and stays inside the header. |

## How the device was driven

1. Built the debug APK with `./gradlew assembleDebug` in `04 Build`.
2. Installed it with `adb install -r`.
3. First launch showed only the camera permission dialog: "Allow OFFGRID to take pictures and record video?" Tapped "While using the app". No microphone prompt appeared.
4. Walked the header and folders with `adb shell input tap`. Screenshots were `adb exec-out screencap -p`. One `adb shell screenrecord` of the lock walk was pulled after the recorder exited.

`MainActivity` does get window focus on a healthy boot. `dumpsys window` showed `mCurrentFocus` on `com.offgrid.compositor/.MainActivity`, and `dumpsys activity top` showed `mHasWindowFocus=true`, including right after a cold start once the first full frame was up. That is not a permanent "the activity never focuses" bug.

It does fail to answer the focus event when the main thread is stuck drawing. Two ANRs on this run had the subject:

`Input dispatching timed out (... MainActivity (server) is not responding. Waited 5005ms for FocusEvent(hasFocus=true)).`

A `SIGQUIT` trace during the hung instrumented test shows the main thread inside `HardwareRenderer.syncAndDrawFrame` → `DrawFrameTask.drawFrame`, waiting on a futex. `dumpsys gfxinfo` at the worst point reported 690/690 frames janky, median about 950 ms, GPU percentile pegged. Under that load, focus times out. When the framebuffer later went solid black while the window was still `isVisible=true` and `mDrawState=HAS_DRAWN`, that was the emulator GL context (`DisplaySurfaceGlContextHelper context was preempted`, `Failed to restore previous context: 12297`), not an app window that refused to draw.

## P0-1 — Lock — PASS

Steps:

1. On the folder panel, tap the lock glyph at (804, 73).
2. Read the status line.
3. Tap the menu glyph at (910, 73).
4. Press Back, then Back again.
5. Hold the lock glyph with `input motionevent DOWN` for 1.6 s, then `UP`.
6. Tap Unlock.

Result:

- After the tap, the padlock is closed and the panel stays. The status line types: "Panel is locked. Back, the menu, and Unlock still work. Tap the lock or press Back to unlock."
- The menu opens over the locked panel (Big tiles, List, Grid 2, Grid 3).
- The first Back closes that path and unlocks. Status: "The panel is unlocked. You can edit again." The open padlock is back.
- A long-press arms Broadcast Lock: the panel is gone, the stage is black, and the bottom strip reads LOCKED with an Unlock button. Tapping Unlock (center about 613, 2258) restores the panel and the same unlock sentence.
- `immersive_mode_confirmations` stayed empty. The Android 15 "Viewing full screen / Got it" card did not appear. CI artifact `02b_broadcast_lock_immersive.png` does show that card. This emulator did not.
- Screenrecord of this walk did not itself arm Broadcast Lock.
- Every cold start in this session came back on the unlocked folder panel. A force-stop that was issued while the edit lock was still held was not completed; the emulator died on later attempts. Nothing in this run left the app stuck locked.

Small defect, not a fail of P0-1: when the view menu is open it covers the right side of the status line, so "You can edit again." is hidden under the menu card until the menu closes. See `06_menu_overlaps_status.png`.

## P0-2 — Minus — PASS

Steps:

1. Tap the minus glyph at (1016, 73).
2. Screenshot.
3. Tap the center of the remaining control.

Result:

- The panel is replaced by a black stage and a centered tab. Measured bounds of the lit pixels: x 343–736, y 1100–1299. The tab reads OFFGRID.
- Tapping (539, 1199) restores the full folder panel. Status: "The panel is open again."
- The process stayed alive across hide and restore.

Not finished: hide, then force-stop, then reopen. `TerminalPanel.showMinimized` writes `prefs.minimized = false` on every save, so a restart should not stay hidden, but that path was not executed after the GPU wedged. The bug Royalty hit (no visible way back) is fixed on the path that was tapped.

## P1-1 — Open Kick — BLOCKED

Steps that were attempted:

1. From the folder panel, tap the Kick tile (right column, about 756, 1416).
2. The following `screencap` was 15,580 bytes, all black. `mCurrentFocus` was still MainActivity, then the process was gone and the launcher was showing.

Play Store was not opened in any frame that was captured. That is not the same as proving Open Kick stays inside OFFGRID. `PlatformLinks.open` has no `market://` or Play Store intent and returns MISSING when `com.kick.mobile` is absent. That is source, not a tap. Verdict stays BLOCKED.

## P1-2 — I got kicked — FAIL

Royalty asked for an admin-style kick sweep: a long flood of "user kicked" lines, or removal of switches that do nothing in other apps.

What the build does (`EffectsController.kicked`):

- If "Red sweep on kick" is on, it fires `Fire.RED_SWEEP` on OFFGRID's own video.
- It then types one line: "You were kicked from %s. Wait a moment. Then tap Open %s and ask to join again yourself. Never use auto rejoin. It can get you banned."

There is no flood of "user kicked" lines. The switch is still shown. The row text says the action never taps or rejoins another app. OFFGRID cannot write into TikTok, Bigo, MeetMe, or Kick comments. That limit does not pass this item. The Kick Effects screen was not reached on the device before the emulator died, so the fail is from the code path that ships in this APK, not from a screenshot of the row.

## P1-3 — Red sweep on kicked — FAIL

The toggle is still in `EffectFolders.platformEffects`. Its copy says the red carpet sweeps OFFGRID's own video and cannot change TikTok or other apps. The switch was not removed. This emulator has no usable front camera (`Camera LENS_FACING_FRONT verification failed`, status "This phone has no front camera." / "The front camera is starting."), so a sweep on the video could not be watched. A switch that the product itself says does nothing in the other app, and that was not seen doing anything here, does not pass.

## P1-4 — Copy pairing key — PASS

Steps:

1. Tap Live and Remote. The first tap only highlighted the tile. A second tap at (300, 1164) opened the folder.
2. Tap the gray "Copy pairing key" button. Its body is the gray bar whose center row is y ≈ 1232 (measured on `220_live2.png`).
3. Wait for the typewriter to finish. Screenshot.

Result:

- The screen stays on Live and Remote. Breadcrumb remains HOME › Live and Remote. "Copy pairing key" is still on the page.
- While the typewriter was still running, the status line ended at "…or an A". That was the animation, not clipping. The settled frame reads the full sentence: "Pairing key made and copied. Paste it on a PC or an Android phone."

Clipboard: the clip is created in `PairingWorkflow.copyCurrentKey` as plain text labeled "OFFGRID pairing key", marked `EXTRA_IS_SENSITIVE`, and cleared after 60 seconds. `adb shell dumpsys clipboard` returned an empty buffer both as the shell user and after `adb root`, which matches a sensitive clip the shell is not allowed to print. A follow-up paste into the idle-hex field was attempted; the process died and the framebuffer went black before a paste screenshot existed. The on-screen confirmation is the evidence for this pass. The shell never displayed the key bytes.

## P1-5 — PC or Android phone — PASS

On the opened Live and Remote folder, the notes include: "Pairing works for a PC or an Android phone on the same Wi-Fi." The copy button's own line says "Send it to a PC or an Android phone, then enter the pairing address and port there." The settled copy confirmation repeats "a PC or an Android phone."

Source still has a later toggle titled "Send to PC" (`FxKeys.LIVE_PC_ON`) whose description mentions OBS or the Android phone link. That row was below the fold and was not scrolled to. The pairing text Royalty complained about is no longer PC-only.

## P1-6 — Click sounds — BLOCKED

Steps:

1. With the folder panel up and the clicker left at its default (on), `dumpsys audio` was saved.
2. `input swipe 531 40 531 120 300` was sent twice across the W control (the W reel's frame is 468,21–594,126).
3. `dumpsys audio` was saved again.

Result: the only `USAGE_ASSISTANCE_SONIFICATION` players are system `SoundPool`s (uid 1000). No new player and no `AudioTrack` for `com.offgrid.compositor` appeared. An earlier view dump showed `DrumReelView` as GONE inside that frame; the visible control is the number `EditText`. The emulator log says `Could not init pa audio driver`, so this VM has no PulseAudio output. A click that started and ended inside one audio dump interval would also be easy to miss. This item is not a pass.

`ClickerSoundEngine` is `USAGE_ASSISTANCE_SONIFICATION` and `playWheelClick` returns immediately when the toggle is off. The instrumented test calls `playTestClick()` and counts `clickPlayCount`. That counter was not treated as hearing the click.

## P1-7 — Splash — FAIL

Steps (repeated):

1. `adb shell am force-stop com.offgrid.compositor`
2. `adb shell am start -n com.offgrid.compositor/.MainActivity`
3. `screencap` about every 2 seconds.

Result, on more than one cold start:

- Several frames are the system splash: black window, a white circle, a black square, a green circle. Cropped bounds on one capture: x 349–730, y 1009–1390. Center pixel of the green disc is about (57, 255, 106), which is the launcher green `#39FF6A`.
- `ic_launcher.xml` is a black square and that green circle. Android 12 draws it on a circular splash background. `Theme.Offgrid` sets `android:windowBackground` to black and does not set a splash-screen theme that skips the icon.
- The next non-splash frame is the folder panel. No yellow face diamond and no camera preview were in those frames.
- One `am start -W` returned `Status: timeout` after 22,436 ms even though the activity did come up.

Royalty's expected result is no splash flash, and the first presented frame should be the folders. The icon splash is a splash. The yellow diamond being absent does not clear the item.

## P1-8 — White skin — BLOCKED

Panel settings → Colors → Skin color was not opened. The emulator lost the framebuffer and then the process while earlier screens were still in progress.

From `ContrastGuard`, a skin is locked only when the current typewriter text or status text fails 4.5:1 against it. In day mode the text is black, so a white skin passes and is not locked. That is consistent with CI image `18_white_skin_locked.png`, which shows an unlocked white circle, a locked black circle, and the status line still saying "Skin color is open" rather than a lock reason. Those CI pixels were not reproduced here, so they are not a pass.

## P1-9 — Black on black — BLOCKED

The typewriter-text color folder was not opened, and no locked swatch was tapped. The string the code would type is "Locked: black typewriter text on a black background can't be read." (`ContrastGuard.textReason` / `skinReason`). It was not seen on the status line in this run. CI `18_white_skin_locked.png` also does not show that sentence. Not a pass.

## P1-10 — Title — PASS

On this 1080×2400 phone, at the default font, `FitTextView` for the title measures 127,21–235,126. The words draw as two lines, OFFGRID over TERMINAL, and sit between the back orb and the camera button. Nothing is cut off the left edge. Other font sizes were not changed (the size folder was not opened).

## Break attempts

| Attempt | Result |
| --- | --- |
| Rapid Back plus a late menu tap | The menu can open after the unlock sentence has already started, and it covers the status line. The panel does not get stuck locked. |
| Long-press lock, then more taps while the GPU was saturated | Input service disappeared (`Can't find service: input`). ANR on FocusEvent. Not a stuck lock; the emulator's system server dropped the input service. |
| Rotate | Not completed. The activity is portrait-locked, and rotation was not applied before the display died. |
| Lock, then force-stop | Not completed as its own step. Cold starts that were done came up unlocked. |
| Hide, then force-stop | Not completed. Hide then tap-to-restore did work. |
| Dark skin | Not reached. |
| UI Automator dump | `uiautomator dump` exit 137. Pulling the xml afterwards returns a stale or missing file. Do not use it against this panel on this emulator. |

## Instrumented suite — did not pass

Command, after `./gradlew connectedDebugAndroidTest` had already hung for more than 25 minutes inside `ConnectedDevice.getSupportsPrivacySandbox` (`adb shell` never returned):

```
adb install -r -t 04 Build/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r -e class com.offgrid.compositor.RoyaltyPhoneFixTest \
  com.offgrid.compositor.test/androidx.test.runner.AndroidJUnitRunner
```

The runner reported `numtests=11` and started `foldersShowBeforeAnyDiamondFlash` (`INSTRUMENTATION_STATUS_CODE: 1`). It stayed on that test for more than 25 minutes. The instrumentation thread was in `Instrumentation.startActivitySync` → `ActivityScenario.launch`, waiting for the activity to go idle. The main thread was inside `DrawFrameTask.drawFrame`. No test reached `INSTRUMENTATION_STATUS_CODE: 0` or `-2`. The process was then force-stopped so the machine could be used to write this report. The runner's last line is `INSTRUMENTATION_RESULT: shortMsg=Process crashed.` That crash is the force-stop, not an app assertion failure.

CI run 39100881397 reported 11/11 on API 35 and API 36. That result was not reproduced on this emulator. The suite's own click test calls `playTestClick()` rather than a reel swipe, and its contrast test can pass on any content description that already contains "Locked", so a green CI run would still not prove P1-6, P1-8, or P1-9 by itself.

## Rules

| Rule | Result |
| --- | --- |
| Camera permission only | The first-run dialog is camera only. The merged manifest also requests `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE`, `INTERNET`, `DETECT_SCREEN_RECORDING`, `SYSTEM_ALERT_WINDOW`, foreground-service types, and `POST_NOTIFICATIONS`. No `TextToSpeech`, `SpeechRecognizer`, or `RecognizerIntent` in `app/src/main`. The manifest is not camera-only. |
| No sliders | No `SeekBar`, `RangeSlider`, or Material slider in the UI. Numbers on screen are typed. `BioSliderView` exists as an unused class in `bio_slider_view.kt`. It is not constructed anywhere else. |
| No secrets committed | No API keys or passwords in the app sources. `EncryptedChannel` holds an AES key in memory. `PairingKey` is generated at runtime. The clipboard label is the words "OFFGRID pairing key", not a key value. |

## CI screenshots (run 39100881397)

Artifacts `royalty-ui-api-35` and `royalty-ui-api-36` were downloaded. Both sets are 1440×2560 (the CI phone), not this run's 1080×2400.

They match what this device showed for:

- The folder home, two-line title, open lock, W/H at 100.
- The edit-lock sentence.
- The centered OFFGRID restore tab on a black stage.
- Live and Remote copy, including "PC or an Android phone" and "Pairing key made and copied…".
- Yellow folder glyphs still beside the names (out of scope, still there).

They do not match, or were not reproduced here:

- Broadcast-lock frames in CI include the "Viewing full screen / Got it" hint. This emulator never drew it.
- `18_white_skin_locked.png` does not show a typed "Locked:" reason. The status line is still "Skin color is open." The white swatch has no lock. This run never opened that screen.
- `13_open_kick_in_app.png` shows the in-app "will not send you to the Play Store" sentence. This run never got a frame of that screen.

## Evidence files

Saved under `/opt/cursor/artifacts/qa-pr56/` (and the working copies under `/tmp/qa-evidence/`):

| File | What it shows |
| --- | --- |
| `01_camera_permission.png` | Camera-only permission dialog. |
| `02_splash_icon_on_reopen.png` | Icon splash after relaunch. |
| `03_folders_after_splash.png` | Folders, the first real UI frame after that splash. |
| `04_edit_lock.png` | Edit lock, panel still up, full unlock sentence. |
| `05_back_unlocks.png` | Back unlocked the panel. |
| `06_menu_overlaps_status.png` | View menu covering the status line. |
| `07_broadcast_lock_unlock_strip.png` | LOCKED + Unlock, no immersive hint. |
| `08_after_broadcast_unlock.png` | Panel restored from that Unlock tap. |
| `09_minus_restore_tab.png` | OFFGRID tab after Minus. |
| `10_restored_from_tab.png` | Panel back, "The panel is open again." |
| `11_live_pairing_text.png` | PC or Android phone copy, before the tap. |
| `12_copy_pairing_stayed.png` | Full "Pairing key made and copied…" sentence, still on the folder. |
| `walkthrough_lock.mp4` | `adb shell screenrecord` of the lock walk, 1080×2400, about 2 min 50 s, about 0.31 fps because the GPU was missing almost every frame. |

## Bugs and mistakes, with steps

1. **Splash icon before the folders (P1-7).** Force-stop OFFGRID, start `MainActivity`, screenshot every 2 seconds. The first non-black frames are the launcher icon splash. Folders follow. Reproduced on several cold starts, including `300_boot_1` through `300_boot_6` then the folder frame.

2. **Kick flood was not built (P1-2, P1-3).** Open the Kick Effects folder in source or, when the device survives, in the app. "I got kicked" posts one sentence. "Red sweep on kick" is still a toggle aimed at OFFGRID's own video. Neither is the flood Royalty asked for, and neither was removed.

3. **FocusEvent ANR while the main thread is in a GPU draw.** Use the app until frames are about a second long (easy on TCG). Send a tap or a Back key. `dumpsys` / ANR subject: waited 5000 ms for `FocusEvent(hasFocus=true)`. Trace: `DrawFrameTask.drawFrame`. Seen for pids 10245 and 3198 on 2026-10-18. On a fresh boot, focus is granted normally, so this is overload, not a missing `requestFocus`.

4. **View menu covers the status line.** Lock or unlock so a sentence is on the status line, then open the ⋮ menu. The menu card paints over the right half of that sentence (`06_menu_overlaps_status.png`).

5. **`am start -W` can time out even though the activity appears.** One launch reported `Status: timeout`, `WaitTime: 22436`, and the folders still drew afterward.

6. **Instrumented tests do not finish on this emulator.** `am instrument` of `RoyaltyPhoneFixTest` sat in `startActivitySync` for over 25 minutes on test 1 of 11, because the main thread never left `syncAndDrawFrame`. Gradle `connectedDebugAndroidTest` separately hung in `getSupportsPrivacySandbox` before it installed the test APK.

7. **Microphone is still in the manifest.** Runtime UI asked only for the camera. `RECORD_AUDIO` and `FOREGROUND_SERVICE_MICROPHONE` are still merged permissions. No speech recognizer is in the sources.

8. **Unused `BioSliderView`.** Not on screen. It is still a slider-shaped view class in the tree.

9. **Emulator limits that affected this report, and are not app bugs.** 1080×2340 override dropped all touches. UI Automator dump was SIGKILL 137. No front camera. No PulseAudio. GL context loss turned later screenshots black while the window was still focused. Bluetooth aborted repeatedly (`SIGABRT` in `bt_stack_manage`).

## Round 2 — head ffaec30

Retest of the fixer's FAIL list. The Round 1 section above is unchanged except for the pointer at the top.

- Head built and installed: `ffaec301d83433693f408eb0003386bbdc7c9a12` ("Lift Theme mode and keep lock reason on one tap"), from a detached worktree at `/tmp/offgrid-ffaec30`. The QA branch was not used as the build tree.
- Debug APK: `04 Build/app/build/outputs/apk/debug/app-debug.apk`, `assembleDebug` succeeded. Android test APK: `assembleDebugAndroidTest` succeeded, then `adb install -r -t`.
- Same device as Round 1: AVD `qa_phone_35`, API 35, 1080×2400 at 420 dpi, `emulator -accel off` (nested KVM is broken). `settings get secure immersive_mode_confirmations` stayed empty. `hide_error_dialogs` was 0 so the ANR dialog stayed on screen. Animator, window, and transition scales were 0.
- Taps were `adb shell input tap` and `input keyevent`. `performClick` was not used. `uiautomator dump` still dies with 137 while the panel is up.
- CI run 39117461291 was not treated as evidence. Its artifacts were not downloaded.
- Date: 2026-10-18.

**Overall verdict: FAIL.**

Splash, white-skin lock text, the view menu, the merged microphone permissions, slider classes, the edit lock, and the title fit are fixed or still holding on this phone. The activity can go idle, and the instrument suite now finishes. The phone still shows "OFFGRID isn't responding" on a cold start. Click sounds do not log a play. Minus does not leave a tappable restore tab the test can see. The one-tap black typewriter lock was not shown. Copy pairing was not shown.

### Verdict table

| Item | Verdict | What was shown on this device |
| --- | --- | --- |
| (1) Splash / diamond | PASS | Cold and warm frames after force-stop have no green disc, no white launcher circle, and no yellow diamond. First content is the folder grid. A black lead-in comes first, and the ANR dialog is often already up. |
| (2) Idle, ANR, instrument | FAIL | `activityReachesIdleSoEspressoCanSync` passed, and a debuggerd dump showed the main thread in `MessageQueue.nativePollOnce`. The suite finished (14 tests, 742.7 s) and did not hang. Cold start still ANRs: waited 5003 ms for `FocusEvent(hasFocus=true)`. |
| (3) White skin | PASS | Status line types "Locked: white typewriter text on a white background can't be read." The white swatch shows a lock. Hex stayed `#000000`. |
| (4) Night + one black tap | FAIL | Night was set by hand: "Theme mode is now Night." The one tap on black typewriter text, and a reason that stays, were not reached. The instrumented test never left the home grid. |
| (5) Click sounds | FAIL | The width reel moved 100 → 93. logcat has no `SoundPool play streamId=` and no `OffgridClicker` play line. |
| (6) Manifest | PASS | Merged debug manifest has no `RECORD_AUDIO` and no `FOREGROUND_SERVICE_MICROPHONE`. |
| (7) View menu vs status | PASS | Menu open, status fully readable above the card: "Choose how folders look." |
| (8) No slider classes | PASS | No `SeekBar`, `RangeSlider`, or `BioSliderView` under `04 Build/app/src`. Size uses typed boxes and drum wheels. |
| Lock (edit) | PASS | Closed padlock and the lock sentence. Back / menu path returns "The panel is unlocked. You can edit again." Instrumented lock test passed. |
| Broadcast Lock | FAIL | Not walked by hand this round. Instrumented test: "Broadcast Lock must leave an on-screen Unlock. hint=null". |
| Minus | FAIL | Stage below the status bar is black. A small OFFGRID chip sits in the status-bar band. The test did not find "Open the panel". |
| Copy pairing key | FAIL | `copyPairingKeyStaysOnLiveAndRemote` stayed on the home grid. Copy was not completed. Clipboard was not checked. |
| PC or Android text | FAIL | No still shows the sentence. The mention test returned code 0 after a home-grid screenshot. The copy test's same wait failed on the home grid. |
| Title fit | PASS | "OFFGRID TERMINAL" wraps on two lines inside the header, below the status bar. `titleFitsOnScreen` passed. |
| P1-2 Kick flood | DEFERRED | Out of scope. Not a pass. The kick-toggle test failed ("Red sweep must say it cannot change TikTok"). |
| P1-3 Red sweep | DEFERRED | Out of scope. Not a pass. Same suite failure. The "Test diamond" toggle was not turned on. |

### (1) Splash — PASS

Force-stop, then start `MainActivity`, then screenshot.

- `cold_0` through `cold_2`: about 8,000 non-black pixels, all in the status bar. Green pixels 0. Yellow pixels 0.
- `warm_0` through `warm_4` after another force-stop: black stage plus the "OFFGRID isn't responding" dialog. Green 0. Yellow 0. No launcher icon.
- `warm_5`: folder grid, status "The front camera is starting.", ANR dialog on top of the folders.

`foldersShowBeforeAnyDiamondFlash` passed (code 0). Shots `08_no_splash_folders.png` and `09_no_splash_after_resume.png` are the folder grid.

### (2) Idle and ANR — FAIL

The constant-redraw failure from Round 1 is not what this run showed. `activityReachesIdleSoEspressoCanSync` passed. `debuggerd -b` of the app process, taken while the ANR dialog was up, has the main thread in `android::android_os_MessageQueue_nativePollOnce`.

The suite no longer hangs. Command:

`adb shell am instrument -w -r -e class com.offgrid.compositor.RoyaltyPhoneFixTest com.offgrid.compositor.test/androidx.test.runner.AndroidJUnitRunner`

Result: Time 742.746 s, Tests run 14, Failures 7, `INSTRUMENTATION_CODE: -1`, process exit 0. Log: `/tmp/qa-evidence/r2_instrument.txt`. Round 1 sat in `startActivitySync` for more than 25 minutes.

The ANR is still real, so this item fails.

- ANR subject in `/tmp/qa-evidence/r2/anr_cold.txt`: `Input dispatching timed out (... MainActivity (server) is not responding. Waited 5003ms for FocusEvent(hasFocus=true)).`
- `warm_0.png` is the dialog on a black stage, before the folders. `warm_5.png` is the same dialog over the folder grid.
- `logcat_tail.txt` records HWUI Davey frames from 808 ms to 3116 ms.
- Tapping Wait at (330, 1366) returns focus for about a second. The dialog comes back. Taps outside the dialog are eaten.

### (3) White skin — PASS

Evidence is the instrumented screenshot `18_white_skin_locked.png`, not a hand walk. `contrastLockBlocksInvisibleWhiteSkin` passed (code 0).

The status line reads: "Locked: white typewriter text on a white background can't be read." The breadcrumb is Colors → Skin color. The white swatch in the UV row has a lock. The hex field stayed `#000000`. The panel skin on that shot is still black.

### (4) Night, then one black tap — FAIL

Hand path that did complete:

1. Open Panel settings.
2. Open Theme mode (left tile under the ANR dialog).
3. Tap Night at about (420, 1170).

`night_top.png` shows "Theme mode is now Night." on a black panel. The header is still readable.

The rest of the requested path did not complete. Colors sat under the ANR dialog. Taps meant for Colors opened Display and Auric wheel instead. One tap on the black typewriter swatch was not made, so a staying "Locked:" reason for black-on-black was not seen.

`contrastLockBlocksBlackTypewriterOnBlack` failed before any of that. Assertion: "Theme mode folder is off-screen on a phone-sized display after swiping the folder list." The `seen=` tree is the home grid (Panel settings, Camera, AR Video Deck, …). Panel settings never opened in that test. `offscreen_Theme_mode.png` is that home grid. This is a navigation miss while the dialog is up, and it is also a fail of the one-tap check.

### (5) Click sounds — FAIL

Size folder drum wheels are on screen (`size2_wheels.png`, value 100, typed boxes 100, no slider). A finger-down swipe on the width reel changed the value. Later headers show W 93 (`night_top.png`, `18_white_skin_locked.png`).

Cleared logcat across those swipes, including while `mCurrentFocus` was `MainActivity`, never contained `SoundPool play streamId=`, an `OffgridClicker` play line, or "Clicker audio could not start". An `OffgridClicker` thread exists in the ANR trace. That is not a play log.

`reelClickSoundsFireWhenTheToggleIsOn` failed the same way as the Theme mode test: "Clicker Sounds folder is off-screen", `seen=` still the home grid. The manual notch without the log line is the fail.

The emulator has no PulseAudio. The required evidence was the log line, which did not appear.

### (6) Manifest — PASS

Merged file: `04 Build/app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`.

Permissions present: `CAMERA`, `INTERNET`, `DETECT_SCREEN_RECORDING`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `FOREGROUND_SERVICE_CAMERA`, `POST_NOTIFICATIONS`, and the app's dynamic-receiver permission.

`RECORD_AUDIO` is absent. `FOREGROUND_SERVICE_MICROPHONE` is absent.

### (7) View menu — PASS

`theme_open.png`: status line "> Choose how folders look." sits fully above the menu card. The card lists List, Big tiles, Grid 2 (selected), Grid 3, sort, and folder size. The card does not cover that sentence.

`viewMenuDoesNotCoverTheStatusLine` failed with "view menu must open". The hand screenshot is the evidence for this item.

### (8) Sliders — PASS

Search of `04 Build/app/src` for `SeekBar`, `RangeSlider`, and `BioSliderView` returned no matches. `bio_slider_view.kt` is not in this tree. The Size folder is two drum wheels plus typed number boxes (`size2_wheels.png`).

### Reconfirm

**Edit lock — PASS.** Tap of the lock at about (801, 202) closed the padlock. Status: "Panel is locked. Back, the menu, and Unlock still work. Tap the lock or press Back to unlock." (`lock_tap_top.png`). A later Back path opened the padlock and typed "The panel is unlocked. You can edit again." (`unlock3_top.png`). `lockBackAndUnlockWorkAndRestartDoesNotTrap` passed (code 0).

**Broadcast Lock — FAIL.** Not repeated by hand. `broadcastLockImmersiveUnlockIsOnScreen` failed: "Broadcast Lock must leave an on-screen Unlock. hint=null". `immersive_mode_confirmations` was still empty, and the "Viewing full screen / Got it" card still never drew. Round 1 had a visible Unlock strip. This round did not show one.

**Minus — FAIL.** After the minus tap at about (1009, 202), `minus.png` is black from about y=180 to the nav hint (brightest stage pixel about (7, 0, 12)). A glowing chip with a folder glyph and the word OFFGRID occupies the status-bar band, y 0–123 (`minus_top_crop.png`). `minusAlwaysHasARestorePathIncludingAfterRestart` failed: "minus must leave a restore tab" (`Open the panel` was null). A later shot, `minus_restore_top.png`, says "The panel is open again." That frame came after a tap at the old tab point plus Back. Those two inputs were not separated, so the chip was not shown to be the restore control.

**Copy pairing key — FAIL.** `copyPairingKeyStaysOnLiveAndRemote` failed. The seen tree is the home grid and the status is "This phone has no front camera." `10_live_and_remote.png` is that home grid. The copy row was not tapped. The clipboard was not read.

**PC or Android text — FAIL.** `pairingCopyMentionsPcOrAndroidPhone` returned code 0, so its wait reported the string. `shot()` runs before that wait, and `10b_pairing_pc_or_phone.png` is still the home grid. The copy test uses the same wait and failed while the home grid was still showing. No frame in this round shows "PC or an Android phone".

**Title fit — PASS.** The header in `lock_tap_top.png`, `theme_open.png`, and `16_title_default.png` shows "OFFGRID TERMINAL" on two lines, under the status bar, with camera, microphone, W, H, lock, menu, and minus beside it. `titleFitsOnScreen` passed. Early blank-header frames were the ANR dim or the pre-inset black lead-in.

### Suite, one run

| Test | Code |
| --- | --- |
| lockBackAndUnlockWorkAndRestartDoesNotTrap | 0 pass |
| broadcastLockImmersiveUnlockIsOnScreen | -2 fail |
| minusAlwaysHasARestorePathIncludingAfterRestart | -2 fail |
| foldersShowBeforeAnyDiamondFlash | 0 pass |
| copyPairingKeyStaysOnLiveAndRemote | -2 fail |
| pairingCopyMentionsPcOrAndroidPhone | 0 pass (no still of the sentence) |
| openKickStaysInAppAndNeverOpensPlayStore | 0 pass (not used to change the P1-1 verdict) |
| kickTogglesStayOnOffgridAndNeverClaimTikTok | -2 fail (P1-2 and P1-3 stay DEFERRED) |
| reelClickSoundsFireWhenTheToggleIsOn | -2 fail |
| viewMenuDoesNotCoverTheStatusLine | -2 fail |
| titleFitsOnScreen | 0 pass |
| activityReachesIdleSoEspressoCanSync | 0 pass |
| contrastLockBlocksBlackTypewriterOnBlack | -2 fail |
| contrastLockBlocksInvisibleWhiteSkin | 0 pass |

`FAILURES!!! Tests run: 14, Failures: 7`. The run ended. It did not hang.

### New bugs

1. **FocusEvent ANR on launch.** Force-stop, start `MainActivity`. Within a few seconds the system dialog "OFFGRID isn't responding" is up, including before the folders draw (`warm_0.png`) and on top of them (`warm_5.png`). Subject: waited 5001–5003 ms for `FocusEvent(hasFocus=true)`. Wait dismisses it for about a second, then it returns. The main thread can be idle in the looper while the dialog is up. The suite finishing does not clear this.

2. **Minus has no restore tab the UI test can find.** Tap minus. The stage is black. A small OFFGRID chip is drawn in the status-bar band. Content description "Open the panel" was null. Restoring the panel was not isolated to a tap on that chip.

3. **Width reel notches do not log a click.** Swipe the Size width wheel off 100 (100 → 93). No `SoundPool play streamId=` line.

4. **Black-on-black one tap was not shown.** Night can be selected. The instrumented Theme mode path never left the home grid, and the hand path never opened Colors for the single black typewriter tap.

5. **Broadcast Lock left no on-screen Unlock in the suite.** `hint=null`. Not hand-walked this round.

6. **Copy pairing never left the home grid in its test.** The live page, the copy sentence, and the clipboard were not shown.

### Evidence

Stills and the recording are under `/opt/cursor/artifacts/qa-pr56-r2/`. Working copies are under `/tmp/qa-evidence/r2/`.

| File | What it shows |
| --- | --- |
| `warm_0.png` | ANR dialog on a black stage. No splash icon. |
| `warm_5.png` | Folder grid under the same dialog. |
| `18_white_skin_locked.png` | White-skin lock reason and locked white swatch. |
| `night_top.png` | "Theme mode is now Night." Width box reads 93. |
| `theme_open.png` | View menu below the full status line. |
| `lock_tap_top.png` | Edit-lock sentence, closed padlock. |
| `unlock3_top.png` | Unlock sentence, open padlock. |
| `minus.png` / `minus_top_crop.png` | Black stage, OFFGRID chip in the status bar. |
| `minus_restore_top.png` | "The panel is open again." after a mixed tap and Back. |
| `size2_wheels.png` | Drum wheels at 100. No slider. |
| `16_title_default.png` | Title fits. ANR dialog is also in the frame. |
| `10b_pairing_pc_or_phone.png` | Home grid. Not the pairing sentence. |
| `r2_lock_menu.mp4` | 12.4 s, 1080×2400. The captured frames are the Android launcher at 11:09, not the panel. The lock and menu evidence is the stills. |

P1-2 and P1-3 are DEFERRED. They are not passes.

## Round 3 — head 6c7936e

Retest of every Round 2 item on the new head. Round 1 and Round 2 above are unchanged.

- Head built and installed: `6c7936e5da9935305904b712fed10e9af287bcfa` ("Prove ten force-stop cold starts on the host, not in-process"). PR #56's head is that same commit. The build tree was a detached worktree at `/tmp/offgrid-6c7936e`. The QA branch was not the build tree.
- Debug APK: `04 Build/app/build/outputs/apk/debug/app-debug.apk`, 71,299,854 bytes, `assembleDebug` succeeded. Android test APK: `app-debug-androidTest.apk`, 1,508,121 bytes, `assembleDebugAndroidTest` succeeded, then installed with `adb install -r -t`.
- Same phone class as Round 2: AVD `qa_phone_35`, API 35, emulator log `Setting display: 0 configuration to: 1080x2400, dpi: 420x420`. Nested KVM is broken, so the emulator ran TCG (`-accel off`). Guest renderer fell back to lavapipe. `Could not init pa audio driver` is in the emulator log.
- The Android 15 card "Viewing full screen / Got it" drew during Broadcast Lock. `immersive_mode_confirmations` was armed. Round 2 never drew that card. A saved `settings get` of the three animation scales was not kept with the stills. The typewriter caret is moving in the folder screenshots.
- Taps were `adb shell input tap`, `input swipe`, and `input motionevent`. `performClick` was not used.
- CI run 39124419053 was not used as evidence.
- Date: 2026-10-18.

**Overall verdict: FAIL.**

The splash, the view menu, the merged microphone permissions, slider classes, the edit lock, Broadcast Lock's on-screen Unlock, the large Minus restore tab, and the two-line title (once the header is drawn) hold on this phone. Cold start still raises "OFFGRID isn't responding" on 8 of 10 force-stop launches. The instrument suite did not finish. A reel notch still logs no click. Night was set, and the one black typewriter tap was not. White skin was not reopened. Copy pairing and the PC-or-Android sentence were not shown. The cold home grid draws with no header.

### Verdict table

| Item | Verdict | What was shown on this device |
| --- | --- | --- |
| (1) Splash / diamond | PASS | Ten early frames after force-stop: green 0, yellow 0, lit pixels only in the status bar. The next content frame is the folder grid. No launcher icon and no yellow diamond. |
| (2) Cold start ANR + suite | FAIL | 8 of 10 force-stop cold starts logged `Waited 5002–5005ms for FocusEvent(hasFocus=true)` and wrote an ANR trace. `first_frame` at 53.0–56.6 s. The dialog "OFFGRID isn't responding" is on the folder grid. The suite started 15 tests, finished 0, and died with `Process crashed`. |
| (3) White skin | BLOCKED | Colors opened. The Skin color tile is on that page. Skin color was not opened, so the typed `Locked:` reason was not shown on this head. |
| (4) Night + one black tap | FAIL | Hand tap set Night: "Theme mode is now Night." Colors then opened and showed the Typewriter text color tile. That folder was not opened. The next frame is the launcher. No `Locked:` black reason. |
| (5) Click sounds | FAIL | Width reel moved 100 → 99. Status: "Width is now 99." No `SoundPool play streamId=` line in the Round 3 logs. |
| (6) Manifest | PASS | Merged debug manifest has no `RECORD_AUDIO` and no `FOREGROUND_SERVICE_MICROPHONE`. |
| (7) View menu vs status | PASS | Menu open. Status "> Choose how folders look." sits fully above the card. |
| (8) No slider classes | PASS | No `SeekBar`, `RangeSlider`, or `BioSliderView` under `04 Build/app/src`. Size is drum wheels and typed boxes. The folder says "No sliders here." |
| Lock (edit) | PASS | Closed padlock. Status: "Panel is locked. Back, the menu, and Unlock still work. Tap the lock or press Back to unlock." A later frame: "The panel is unlocked. You can edit again." with the open padlock. |
| Broadcast Lock | PASS | Hand walk. Long-press leaves a black stage, the "Viewing full screen" card, and a bottom strip reading LOCKED with Unlock. Tapping Unlock restores the panel: "The panel is open again." |
| Minus | PASS | Large OFFGRID tab below the status bar, with "Panel hidden. Tap the OFFGRID tab, or press Back." Tapping the tab restores the Size folder: "The panel is open again." |
| Copy pairing key | FAIL | The Live and Remote tap path opened Effects ("Effects is open."). Copy was not tapped. The clipboard was not read. |
| PC or Android text | FAIL | No frame shows "PC or an Android phone." |
| Title fit | PASS | Once the header is drawn, "OFFGRID TERMINAL" wraps on two lines inside the header, below the status bar. Cold home frames omit that header. See the new bug below. |
| P1-2 Kick flood | DEFERRED | Out of scope. Kick was not opened. |
| P1-3 Red sweep | DEFERRED | Out of scope. The "Test diamond" toggle was not turned on. |

### (1) Splash — PASS

Force-stop, then start `MainActivity`, then screenshot. Ten early frames (`c1_early.png` through `c10_early.png`), each 1080×2400:

- Green pixels: 0.
- Yellow pixels: 0.
- Lit pixels below the status bar: the nav hint only (708 sampled).

`c1_late.png` is the folder grid in the day theme. Status: "The front camera is starting." No launcher-icon splash and no yellow diamond. `relaunch.png` is the same grid in Night, still with no splash.

### (2) Ten cold starts and the suite — FAIL

Each of the ten starts was a force-stop cold start. Summary file: `/tmp/qa-evidence/r3/cold2/summary.txt`.

| Start | ANR logged | Focus processing | New ANR file | first_frame |
| --- | --- | --- | --- | --- |
| 1 | yes, 5005 ms | 5349 ms | `anr_2026-10-18-12-12-06-860` | 53.9 s |
| 2 | yes, 5005 ms | 5361 ms | `anr_2026-10-18-12-13-50-392` | |
| 3 | yes, 5005 ms | | `trace_18`, `trace_19` (ANR line at 7.3 s) | 56.6 s |
| 4 | yes, 5002 ms | 5179 ms | `anr_2026-10-18-12-16-37-606` | 55.3 s |
| 5 | yes, 5004 ms | 8655 ms | `anr_2026-10-18-12-18-20-100` | |
| 6 | yes, 5003 ms | 8970 ms | `anr_2026-10-18-12-19-40-006` | |
| 7 | no | 3621 ms | | |
| 8 | no | 3727 ms | | 53.0 s |
| 9 | yes, 5005 ms | 7525 ms | `anr_2026-10-18-12-23-50-267` | |
| 10 | yes, 5003 ms | 8027 ms | `anr_2026-10-18-12-25-19-730` | |

Subject, from `c1.log` and the same line in the other ANR logs: `Input dispatching timed out (... MainActivity (server) is not responding. Waited 5005ms for FocusEvent(hasFocus=true)).` Starts 7 and 8 processed focus in under 5 seconds and did not log that line. Eight of ten did.

`00_anr.png` is the dialog "OFFGRID isn't responding" (Close app / Wait) on top of the folder grid. The same dialog is in `lock.png` and `theme2.png`. `c3_anr.png` is the black lead-in (green 0, yellow 0). The ANR for start 3 is the log line, not that black still.

The suite was started three times:

`adb shell am instrument -w -r -e class com.offgrid.compositor.RoyaltyPhoneFixTest com.offgrid.compositor.test/androidx.test.runner.AndroidJUnitRunner`

- Run 1 (`/tmp/qa-evidence/r3_instrument.txt`): `numtests=15`, test 1 `contrastLockBlocksBlackTypewriterOnBlack` status code 1, then `EXIT:255`. No result line.
- Run 2 and run 3: `INSTRUMENTATION_RESULT: shortMsg=Process crashed.` `INSTRUMENTATION_CODE: 0`.

Zero tests finished. The emulator log after the relaunch records `Failed to find ColorBuffer` / `Failed to bind to texture`, and `log_crash.txt` has system ANRs in Phone and Google search while OFFGRID's own ANR was also filed. That crash is why the suite has no pass/fail counts. It is not a green suite.

Home-grid guard, from the test source at this head (`RoyaltyPhoneFixTest.kt`): `onHomeGrid()` is true when both "AR Video Deck folder" and "Panel settings folder" are on screen. `assertOpenedFolder` fails unless the wait succeeds and `onHomeGrid()` is false, with the message "must have opened $name, not the home grid." Copy pairing, the PC-or-Android test, white skin, black typewriter, clicker, and Kick all call it. A folder test cannot pass while that home grid is still showing. The suite died before any test reached the assertion, so this round did not watch the guard fire on the device. The source at `6c7936e` is what was installed.

`tenColdStartsDoNotShowAnr` also requires `/data/local/tmp/offgrid-cold-starts.log` to contain `cold_ok=10` before its own ten starts. That test never started. The ten starts in the table above are the hand runs.

### (3) White skin — BLOCKED

`colors3.png` is Panel settings → Colors, status "Colors is open." The Skin color tile is on the page. It was not opened. No status line on this head reads "Locked: white typewriter text on a white background can't be read." Round 2's still is not evidence for this commit. The white-skin test is in the suite that crashed before it ran.

### (4) Night, then one black tap — FAIL

Hand path that completed:

1. Panel settings → Theme mode.
2. Tap Night.

`night.png`: "Theme mode is now Night." The Night row is selected ("Use a black panel and white text."). The header is readable.

Then Colors opened (`colors3.png`). The Typewriter text color tile is visible. That folder was not opened, so one tap on black typewriter text was not made. `type.png`, the next saved frame, is the Android launcher. No `Locked:` reason for black-on-black was typed.

### (5) Click sounds — FAIL

`vid2/f_03.png` shows the Size folder with both drum wheels on 100. After a swipe on the width reel, `wheel.png` shows the header W box at 99, the wheel highlight on 99, and the status line "Width is now 99."

A search of the Round 3 logcat captures for `SoundPool play streamId=` returned no lines. `OffgridClicker` appears only as a thread in the ANR CPU list (`c1.log`, `c4.log`). The emulator has no PulseAudio driver. The required evidence was the play log, and it is absent.

### (6) Manifest — PASS

Merged file from this build: `04 Build/app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`.

Permissions present: `CAMERA`, `INTERNET`, `DETECT_SCREEN_RECORDING`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `FOREGROUND_SERVICE_CAMERA`, `POST_NOTIFICATIONS`, and the app's dynamic-receiver permission.

`RECORD_AUDIO` is absent. `FOREGROUND_SERVICE_MICROPHONE` is absent.

### (7) View menu — PASS

`menu2.png`: status line "> Choose how folders look." is fully above the menu card. The card lists List, Big tiles, Grid 2 (selected), Grid 3, sort, and folder size. The card does not cover that sentence.

`theme2.png` is a later frame of the same menu with the ANR dialog over the card. The status line above the card reads "The panel is unlocked. You can edit again."

### (8) Sliders — PASS

Search of `04 Build/app/src` for `SeekBar`, `RangeSlider`, and `BioSliderView` returned no matches. No file name contains `slider` or `seekbar`. `wheel.png` is two drum wheels, typed number boxes, and toggles. Panel settings says "No sliders here. Only typed numbers and toggles."

### Reconfirm

**Edit lock — PASS.** `lock.png`: closed padlock, status "Panel is locked. Back, the menu, and Unlock still work. Tap the lock or press Back to unlock." The ANR dialog is also in that frame. `theme2.png`: open padlock, status "The panel is unlocked. You can edit again."

**Broadcast Lock — PASS.** Hand walk, not only a test. After a long-press on the lock, `bcast.png` shows a black stage, the system card "Viewing full screen / To exit, swipe down from the top of your screen / Got it", and a bottom strip with LOCKED and an Unlock button. A tap on Unlock produced `unlock.png`: the Size folder is back and the status line is "The panel is open again."

**Minus — PASS.** `minus_top.png`: the status-bar clock is above a large OFFGRID tab. The tab has a yellow inner stroke and a cyan outer stroke. The coach mark reads "Panel hidden. Tap the OFFGRID tab, or press Back." A tap on the tab produced `restored.png`: Size folder, status "The panel is open again." The restore is that tap.

**Copy pairing key — FAIL.** After Broadcast Lock, the walk returned to the home grid (`live.png`, "Back at the folder list.", Effects highlighted). The following open is `live3.png`: "Effects is open." Live and Remote was not opened. "Copy pairing key" was not tapped. The clipboard was not read.

**PC or Android text — FAIL.** No still in this round contains "PC or an Android phone."

**Title fit — PASS, with a cold-start hole.** `a_settings.png`, `lock.png`, `night.png`, `wheel.png`, and `live.png` show "OFFGRID TERMINAL" on two lines inside the header, under the status bar, beside camera, microphone, W, H, lock, menu, and minus. The words are not clipped. `c1_late_top.png` and `relaunch_top.png` are the home grid after a cold start with the status line and the folders, and with no header at all. The two-line wrap holds on every frame that draws the header. The missing header is the new bug below.

### Home-grid tests

The false pass from Round 2 was a folder assertion returning success while the screenshot was still the home grid. At `6c7936e`, `assertOpenedFolder` requires both a successful open wait and `!onHomeGrid()`. `onHomeGrid()` stays true while Panel settings and AR Video Deck are both on screen. The assertion message is "must have opened $name, not the home grid." Copy pairing, PC-or-Android, white skin, black typewriter, the clicker folder, and Kick all go through it. Those tests cannot pass from the home grid. This round did not get a finished instrument result, so the guard was confirmed in the installed source and was not watched failing a live run.

### New bugs

1. **FocusEvent ANR on cold start, 8 of 10.** Force-stop, start `MainActivity`. The window does not answer `FocusEvent(hasFocus=true)` within 5 seconds. ANR traces are listed in the table above. `first_frame` landed at 53–57 seconds on the starts that recorded it. The dialog "OFFGRID isn't responding" sits on the folder grid (`00_anr.png`). Starts 7 and 8 answered in 3621 ms and 3727 ms and did not log the ANR.

2. **The instrument suite does not finish on this head.** Three `am instrument` runs of `RoyaltyPhoneFixTest`. The first named test 1 of 15 and then exited 255. The next two returned `Process crashed` with no test results.

3. **Width reel notches do not log a click.** Swipe the Size width wheel off 100 (100 → 99, "Width is now 99."). No `SoundPool play streamId=` line.

4. **Black-on-black one tap was not shown.** Night can be selected. Colors can be opened. The Typewriter text color folder was not opened, and the process left the screen for the launcher (`type.png`).

5. **White skin was not reopened on this head.** The Skin color tile is visible from Colors. The lock reason was not typed.

6. **Copy pairing still does not get a still.** The tap meant for Live and Remote opened Effects. No copy sentence and no clipboard read.

7. **Cold home grid omits the header.** After force-stop, the folder grid and the status line draw with no "OFFGRID TERMINAL" header (`c1_late_top.png`, `relaunch_top.png`). Opening a folder and backing out brings the two-line header back (`live.png`).

### Evidence

Stills and the recordings are under `/opt/cursor/artifacts/qa-pr56-r3/`. Working copies are under `/tmp/qa-evidence/r3/`.

| File | What it shows |
| --- | --- |
| `c1_early.png` | Black stage, status-bar icons, nav hint. Green 0, yellow 0. |
| `c1_late.png` | Folder grid. No splash icon. No header on this cold frame. |
| `00_anr.png` | "OFFGRID isn't responding" over the folder grid. |
| `lock.png` | Edit-lock sentence, closed padlock, two-line title. ANR dialog also in frame. |
| `theme2.png` | Unlock sentence, open padlock, view menu, ANR dialog. |
| `menu2.png` | "> Choose how folders look." fully above the view menu. |
| `night.png` | "Theme mode is now Night." |
| `colors3.png` | Colors open. Typewriter text color and Skin color tiles. Neither opened. |
| `type.png` | Launcher, after the typewriter path left the app. |
| `wheel.png` | Width 99. Drum wheels and typed boxes. No slider. |
| `minus_top.png` | Large OFFGRID restore tab below the status bar. |
| `restored.png` | "The panel is open again." after the tab tap. |
| `bcast.png` | LOCKED, Unlock, and the "Viewing full screen" card. |
| `unlock.png` | Panel restored after the Unlock tap. |
| `live3.png` | Effects opened. Live and Remote was not. |
| `a_settings.png` | Two-line title inside the header. "No sliders here." |
| `r3_panel.mp4` | 159.7 s, 1080×2400. Folder grid under the ANR dialog. This is the panel, not the launcher. |
| `r3_panel2.mp4` | 179.4 s, 1080×2400. Size folder, drum wheels, then the later panel walk. |

P1-2 and P1-3 stay DEFERRED. They are not passes.

## Round 3 calibration — same emulator

This section only separates the device from the app. No app code was changed. Head on the device was still `6c7936e`. The AVD, flags, size, density, animation scales, and immersive hint match Round 3.

**Environment verdict: UNFIT.**

Settings and an empty activity do not start in under 2 seconds. When Android records a Displayed time, it is 13–29 seconds, and several starts had no Displayed line within 40 seconds. System UI, Phone, the keyboard, and search ANR during those starts. A 53-second OFFGRID first frame on this emulator is not evidence that a normal phone would do the same.

### 1. Acceleration, GPU, CPU, RAM

| Check | Result |
| --- | --- |
| `emulator -accel-check` | Exit 0. Prints `KVM (version 12) is installed and usable.` |
| `/dev/kvm` | Present, mode `crw-rw-rw-`, major 10 minor 232. |
| KVM vCPU | Not usable. `KVM_GET_API_VERSION` ioctl returns `EINVAL`. `dmesg` has `kernel BUG at arch/x86/kvm/x86.c:702` in `kvm_arch_vcpu_create`. Nested KVM on this host faults when a vCPU is created. |
| Process that was measured | `qemu-system-x86_64-headless -avd qa_phone_35 -no-snapshot -no-window -gpu guest -no-boot-anim -no-metrics -accel off -memory 3072 -cores 2 -port 5554` |
| Hardware acceleration inside that process | Off. The log says `x86_64 emulation may not work without hardware acceleration` and TCG rejects AVX and F16C. |
| GPU | `-gpu guest` was requested. The system image cannot do guest rendering. Fallback: `vulkan_mode_selected:lavapipe`, `gles_mode_selected:swangle`. |
| CPU cores given to the AVD | 2. Guest `nproc` is 2. Host has 4. |
| RAM given to the AVD | 3072 MB. Guest `MemTotal` is 3,048,516 kB. |
| Display | `1080x2400`, 420 dpi. Confirmed with `wm size` and `wm density`. |
| Boot | `Boot completed in 235099 ms`. |
| Animations | `animator_duration_scale=1`, `window_animation_scale=1`, `transition_animation_scale=1`. |
| Immersive hint | `settings get secure immersive_mode_confirmations` is `null`. |

### 2. Baseline cold starts

`am start -W` on this image prints `LaunchState: UNKNOWN (0)` and `WaitTime`. It does not print `TotalTime`. On the traced OFFGRID start, that WaitTime returned in 4.2 s, and `OffgridStartup: first_frame` was 52 s after the process fork. WaitTime is not time-to-first-frame here. The numbers below are ActivityTaskManager `Displayed` times, which are.

Fresh-boot `am start -W` WaitTime, force-stop then start, no FocusEvent on the target window:

| Start | Settings WaitTime | Empty WaitTime | OFFGRID WaitTime |
| --- | --- | --- | --- |
| 1 | 16157 ms | 5007 ms | 5929 ms |
| 2 | 3862 ms | 5297 ms | 5127 ms |
| 3 | 3900 ms | 3960 ms | 4449 ms |
| 4 | 3481 ms | 6051 ms | 5141 ms |
| 5 | 3934 ms | 5949 ms | 3799 ms |

A second set with `am start -W -S` was the same shape: Settings 3584–6776 ms, Empty 3436–5904 ms, OFFGRID 3394–4023 ms. Still no `TotalTime` line.

Displayed times after the emulator had been up long enough to ANR system processes. Each start was force-stop, then `am start -S`. The watch stopped at 40 s for the baselines.

| Start | Settings `Displayed` | Empty `Displayed` | Notes |
| --- | --- | --- | --- |
| 1 | none within 40 s | none within 40 s | Phone ANR (`failed to complete startup`). Search and System UI ANR on the empty start. |
| 2 | none within 40 s | none within 40 s | System UI ANR. Empty start also ANR'd the keyboard and search. |
| 3 | none within 40 s | none within 40 s | System UI and keyboard ANR on the empty start. |
| 4 | `+18s193ms` | none within 40 s | Keyboard ANR during Settings. |
| 5 | `+28s624ms` | `+13s224ms` | System UI `FocusEvent(hasFocus=true)` took 2648 ms and was logged as Application Not Responding. That FocusEvent is System UI, not Settings. |

No baseline Displayed time was under 2 seconds. The ones Android did report are 13 s, 18 s, and 29 s.

### 3. OFFGRID cold starts and one startup trace

`am start -W` WaitTime for five force-stop starts is the table above (3.8–5.9 s) and does not match the first frame.

Traced start, simpleperf attached for the first 18 s (`simpleperf record --app com.offgrid.compositor -g --duration 18 -f 100 --trace-offcpu --call-graph dwarf -e cpu-clock`):

| Marker | Time |
| --- | --- |
| `am_proc_start` pid 5813 | 13:50:07.066 |
| `OffgridStartup: onCreate_done` | 13:50:55.737 (48.7 s after the fork) |
| `OffgridStartup: first_frame` | 13:50:59.302 (52.2 s) |
| `OffgridStartup: heavy_ready` | 13:51:05.312 (58.2 s) |
| `am start -W` WaitTime on that same start | 4224 ms |

A later start, after the baseline Displayed run, logged `Displayed com.offgrid.compositor/.MainActivity ... +1m2s512ms` and `onCreate_done`. The emulator's qemu process exited during the next start (`adb: no devices/emulators found`), so starts 2–5 of that set were not recorded. The renderer log already had `Failed to find ColorBuffer` / `Failed to bind to texture`.

Main thread for pid 5813, tid 5813, first 18 s (1,741 cpu-clock samples, 18.9 s of thread time). About 96% of the self time is one kernel address. `kptr_restrict` blocked kallsyms, so that address stays unnamed. `--trace-offcpu` puts the off-cpu sample on that kernel frame. The user stack under it is the interpreter. The hottest Java path, about two thirds of the launch samples:

`android.app.ActivityThread.main` → `android.os.Looper.loop` → `android.app.servertransaction.LaunchActivityItem.execute` → `android.app.ActivityThread.handleLaunchActivity` → `android.app.ActivityThread.performLaunchActivity` → `android.app.Instrumentation.callActivityOnCreate` → `com.offgrid.compositor.MainActivity.onCreate` → `androidx.appcompat.app.AppCompatActivity.setContentView` → `androidx.appcompat.app.AppCompatDelegateImpl.setContentView` → `android.view.LayoutInflater.inflate` → `android.view.LayoutInflater.rInflate` → `android.view.LayoutInflater.createViewFromTag` → `androidx.appcompat.app.AppCompatViewInflater.createView` → `androidx.appcompat.app.AppCompatViewInflater.createTextView` → `androidx.appcompat.widget.AppCompatTextView.<init>` → `art::ClassLinker::VerifyClass` → `art::verifier::MethodVerifier::VerifyMethod`

The other large branch under the same inflate is `androidx.appcompat.widget.AppCompatTextHelper.loadFromAttributes` → `androidx.appcompat.widget.AppCompatTextHelper.updateTypefaceAndStyle` → `androidx.core.content.res.ResourcesCompat.loadFont` → `androidx.core.graphics.TypefaceCompat.createFromResourcesFamilyXml`.

Log lines from that main thread while those samples were taken:

- `Verification of void androidx.activity.ComponentActivity.<init>() took 121.199ms`
- `Verification of void com.offgrid.compositor.MainActivity.startAfterFirstFrame() took 117.947ms`
- `Verification of void com.offgrid.compositor.MainActivity.bindCamera$app_debug(...) took 193.868ms`
- `Verification of java.lang.String com.offgrid.compositor.MainActivity.describePreviewView$app_debug() took 116.837ms`
- `Verification of void com.offgrid.compositor.MainActivity.onCreate(android.os.Bundle) took 129.027ms`
- `Choreographer: Skipped 52 frames`

Inside the process, the other big sample owners over those 18 s are `Jit thread pool` (25.8%), `Profile Saver` (20.5%), and `HeapTaskDaemon` (20.0%). The main thread is 26.1%.

### 4. Instrument run that ended in `Process crashed`

Saved log: `/tmp/qa-evidence/r3/log_crash.txt`, matching `/tmp/qa-evidence/r3_instrument2.txt` and `r3_instrument3.txt` (`INSTRUMENTATION_RESULT: shortMsg=Process crashed`).

There is no `AndroidRuntime: FATAL EXCEPTION` for `com.offgrid.compositor`. tombstoned did not keep a file:

`tombstoned: received crash request for pid 2601` (the app process) → `found intercept fd 513 for pid 2601 and type kDebuggerdJavaBacktrace` → `grid.compositor: Wrote stack traces to tombstoned` → `skipping tombstone file creation due to intercept`.

What ActivityManager recorded, and then acted on:

```
ANR in com.offgrid.compositor
PID: 2601
Reason: Process ProcessRecord{6c1ee7d 2601:com.offgrid.compositor/u0a209} failed to complete startup
```

Then `Killing 2601:com.offgrid.compositor/u0a209 (adj 0): bg anr`, `Zygote: Process 2601 exited due to signal 9 (Killed)`, and `Crash of app com.offgrid.compositor running instrumentation ComponentInfo{com.offgrid.compositor.test/androidx.test.runner.AndroidJUnitRunner}` followed by `Force stopping com.offgrid.compositor`.

On the way there the main thread logged `Choreographer: Skipped 250 frames` and ART verification of `androidx.test.runner.MonitoringInstrumentation` methods (148 ms, 100 ms, 350 ms). The same minute has startup ANRs for Phone, Google search, and System UI. CPU pressure in that ANR dump is `some avg10=85.15`.

### How the rule applies

Baselines do not start in under 2 seconds, so this run does not support "the phone is fine and only OFFGRID is slow." Baselines do take tens of seconds, or they never display within 40 seconds, and the emulator ANRs System UI, Phone, the keyboard, and search while those starts are in progress. One of those ANRs is a System UI `FocusEvent(hasFocus=true)`.

That is the UNFIT case. Round 3's 53–57 s `first_frame` and its FocusEvent ANRs were measured on this same unfit emulator. The trace still shows OFFGRID's main thread inside `MainActivity.onCreate` inflating `AppCompatTextView` and verifying classes for the first 18 seconds of a 52-second first frame. That work is real. It was not timed on a device where Settings and an empty activity start in under 2 seconds.

## Round 4 — head 6c7936e on a KVM runner

Independent walk on GitHub Actions, not on the nested emulator from Round 3. The app tree is still `6c7936e5da9935305904b712fed10e9af287bcfa`. The QA branch only holds this report, the workflow, and the tap script. App code was not changed.

- Run: https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/39146533836 (commit `4c23355`). The emulator step ran from 16:49:42Z to 17:08:20Z. It finished. It did not hang.
- Earlier hang: run 39135791813 sat on "Boot API 35 with KVM and walk the panel" for 1h41m and was cancelled when the timeout harness landed. Run 39143358574 then finished in 27 minutes and showed harness false-fails (a Locked status hid an open folder, the immersive cling hid Unlock from a plain dump, a short reel swipe stayed at 100, and `timeout wait` failed because `wait` is a shell builtin). Those script bugs are fixed in `4c23355`. The screenshots below are from the re-run.
- Host: `ubuntu-latest`, 4 cores, 15 GiB RAM, `/dev/kvm` mode 0666, `emulator -accel-check` printed "KVM (version 12) is installed and usable."
- Guest: API 35, `sdk_gphone64_x86_64`, 1080×2400 at 420 dpi, animation scales 1/1/1, `immersive_mode_confirmations` empty, 2 cores, about 3 GiB RAM. GLES: SwiftShader 4.0.0.1. Profile Pixel 7.
- The job `timeout-minutes` is 60. Every adb command in the walk is wrapped in `timeout`. Boot completion is polled with a cap. `PROGRESS` lines go to the job log and to `evidence/progress.log`. Artifacts upload with `if: always()`.
- Taps are `adb shell input tap`, `input swipe`, and a long-press, at centers from `uiautomator dump`. When the "Viewing full screen" cling was the only dumped window, Unlock came from `dumpsys activity top` (`unlock_button` bounds). `performClick` was not used.
- I looked at the PNGs in the artifact `independent-qa-api-35`. The table follows those pictures and the dumps, not the fixer's CI.

**Overall verdict: FAIL.**

Splash, cold start, white skin, Night plus one black tap, the merged manifest, the view menu, slider classes, edit lock, Broadcast Lock, Minus, copy pairing, the PC-or-Android sentence, and the title all passed on this runner. The Width reel did change, and the independent log taken right after that change has no `SoundPool play` line. That one item fails the round.

### Verdict table

| Item | Verdict | What was shown on this device |
| --- | --- | --- |
| (1) Splash / diamond | PASS | `cold/offgrid_01_early.png` is black with the status bar. Yellow samples on that frame: 0. `cold/offgrid_01.png` is the folder grid. Status: "The front camera is starting." |
| (2) Cold start + suite | PASS | Ten force-stop starts of Settings, of an empty activity, and of OFFGRID. No ANR dialog and no `ANR in` line. OFFGRID `Displayed` is 2.8–3.6 s, in the same band as Settings after its first start. `RoyaltyPhoneFixTest` finished 15 tests, 0 failures, in the same job. |
| (3) White skin | PASS | Skin color is open. Status: "Locked: white typewriter text on a white background can't be read." Lock glyphs sit on the swatches. Hex box shows `#FFFFFF`. |
| (4) Night + one black tap | PASS | Night is selected. Status: "Theme mode is now Night." One tap on the locked black swatch leaves "Locked: black typewriter text on a black background can't be read." The same sentence is still the status on the later frame. |
| (5) Click sounds | FAIL | Width reel moved 100 → 96. Status: "Width is now 96." `OffgridClicker` log read at 17:01:44Z, after that change, is empty. |
| (6) Manifest | PASS | Merged debug manifest from `6c7936e` has no `RECORD_AUDIO` and no `FOREGROUND_SERVICE_MICROPHONE`. |
| (7) View menu vs status | PASS | Menu open under "> Choose how folders look." Status bounds (14, 283, 1066, 399). Card bounds (231, 413, 1019, 1962). Overlap height 0. |
| (8) No slider classes | PASS | Source grep for `SeekBar`, `RangeSlider`, and `BioSliderView` is absent. Panel settings says "No sliders here. Only typed numbers and toggles." |
| Lock (edit) | PASS | Status: "Panel is locked. Back, the menu, and Unlock still work." Then: "The panel is unlocked. You can edit again." The folder grid stays up both times. |
| Broadcast Lock | PASS | Long-press hides the panel. The cling "Viewing full screen / Got it" is at the top and was not tapped. Bottom strip reads LOCKED and Unlock at (524, 2134, 738, 2242). Tapping Unlock restores the folder grid: "Back at the folder list." |
| Minus | PASS | The panel hides to a large OFFGRID tab at (126, 262, 563, 441), below the status bar, with "Panel hidden. Tap the OFFGRID tab, or press Back." Tapping it restores the grid: "The panel is open again." |
| Copy pairing key | PASS | Live and Remote stays open. Status: "Pairing key made and copied. Paste it on a PC or an Android phone." The Copy pairing key button is still on that page. A system clipboard chip is on the screenshot. `dumpsys clipboard` was empty. |
| PC or Android text | PASS | The open folder says "Pairing works for a PC or an Android phone on the same Wi-Fi." The button help says "Send it to a PC or an Android phone." |
| Title fit | PASS | The cold home grid draws folders and omits the header. With Panel settings open, "OFFGRID TERMINAL" is two lines at (137, 157, 241, 262), inside 1080×2400. |
| P1-2 Kick flood | DEFERRED | Out of scope. Kick was not opened. |
| P1-3 Red sweep | DEFERRED | Out of scope. |

### (1) Splash — PASS

`cold/offgrid_01_early.png` (1080×2400) is a black frame, status icons, and the gesture bar. The yellow-pixel sample on that frame is 0. `cold/offgrid_01.png` is the home grid of folders. No launcher-icon splash and no yellow diamond before the folders.

### (2) Ten cold starts and the suite — PASS

Each start was force-stop, then `am start -W`. `ActivityTaskManager` `Displayed` times, all `Status: ok`, all `LaunchState: COLD`, no ANR dialog:

| Series | Displayed (ms) |
| --- | --- |
| Settings | 7785, 3272, 3736, 3619, 2942, 3183, 2584, 3012, 3561, 3142 |
| Empty activity `com.qa.empty/.EmptyActivity` | 1905, 1982, 2163, 2809, 2015, 1930, 1453, 1830, 1515, 1485 |
| OFFGRID `MainActivity` | 3552, 2812, 3027, 2795, 2970, 2937, 2922, 2855, 2965, 2827 |

Settings' first start is 7.8 s. The rest are a few seconds. The empty activity is about 1.5–2.8 s. OFFGRID is about 2.8–3.6 s, the same band as Settings, with no "isn't responding" dialog. The captured `OffgridStartup` line on these starts is `onCreate_done`. A `first_frame` line was not in that slice.

The rule from calibration: baselines under 2 s with OFFGRID far behind would blame the app, and baselines of tens of seconds or ANR would mark the runner unfit. This runner is neither. KVM is usable, and OFFGRID displays in a few seconds beside Settings. Cold start passes.

The fixer's `RoyaltyPhoneFixTest` ran once in the same job after the walk: 15 tests, 0 failures, 0 errors, 0 skipped, 174.6 s (`TEST-emulator-5554 - 15-_app-.xml`). The host log was seeded with `cold_ok=10` only after the ten OFFGRID starts above had no ANR. That suite result is a second data point from this job. The pictures in this section are from the independent taps.

### (3) White skin — PASS

`steps/white_after_tap.png`. Skin color is the open folder (breadcrumb ends in Skin color, heading "Skin color"). The status line is "Locked: white typewriter text on a white background can't be read." The hex box is `#FFFFFF`. Lock glyphs are on the swatches.

### (4) Night, then one black tap — PASS

`steps/night_chosen.png`: the panel is dark, Night's radio is selected, status "Theme mode is now Night."

`steps/black_after_2.png`: Typewriter text color is open, status "Locked: black typewriter text on a black background can't be read." The dump taken right after the tap (`black_after_1`) carries the same sentence, and this later frame still shows it. The hex box is still `#FFFFFF`.

### (5) Click sounds — FAIL

`steps/reel_before.png`: Size is open, both drums on 100, header W 100 H 100, status "Size is open."

The first swipe traveled up the Width drum and the wheel stayed at 100 (`steps/reel_after.png`, status "Width is now 100."). The second swipe traveled down the same drum. `steps/reel_after_2.png` shows the Width drum and the box under it on 96, header W 96, status "Width is now 96." Height stays 100.

`adb logcat -c` ran at 17:01:29Z, before either swipe. The read `adb logcat -d -v time -s OffgridClicker:I` ran at 17:01:44Z, about three seconds after the wheel showed 96. `evidence/reel_clicker.log` is empty. No `SoundPool play` line, including the skipped and queued forms, was in that buffer.

The suite's own reel test, later in the same job, did log `OffgridClicker: SoundPool play streamId=129` (and 137, 145, 153, 161, 169) at 17:06:16Z. That is the instrumented test, not this gesture.

### (6) Manifest — PASS

`evidence/static_checks.txt`, from the merged debug manifest of `6c7936e`:

- `android.permission.RECORD_AUDIO=absent`
- `android.permission.FOREGROUND_SERVICE_MICROPHONE=absent`

Permissions that are present include `CAMERA` and `FOREGROUND_SERVICE_CAMERA`.

### (7) View menu — PASS

`steps/menu_open.png`. Status "> Choose how folders look." The card under it has VIEW (List, Big tiles, Grid 2, Grid 3), SORT, FOLDER SIZE, and Done. Measured status bottom is y=399. Measured card top is y=413. They do not overlap.

### (8) Sliders — PASS

`static_checks.txt` records `slider_grep=absent` for `SeekBar`, `RangeSlider`, and `BioSliderView` under the app source. `steps/title_fit_pass.png` shows the Panel settings guidance: "No sliders here. Only typed numbers and toggles."

### Reconfirm

- Edit lock. `steps/lock_locked.png` keeps the folder grid and says the panel is locked. `steps/lock_unlocked.png` says "The panel is unlocked. You can edit again."
- Broadcast Lock. `steps/broadcast_armed.png` is a black stage, the armed immersive cling at the top, and LOCKED plus Unlock at the bottom. The cling does not cover the strip. `steps/broadcast_unlocked.png` is the folder grid again.
- Minus. `steps/minus_hidden.png` is the large OFFGRID tab and the "Panel hidden" note. `steps/minus_restored.png` is the folder grid, status "The panel is open again."
- Copy pairing key and the PC-or-Android sentence are the Live and Remote frames above. The app stayed on that folder.

### Title

`cold/offgrid_01.png` is the home grid with no "OFFGRID TERMINAL" header. `steps/title_fit_pass.png` opens Panel settings, and the header draws "OFFGRID TERMINAL" on two lines at (137, 157, 241, 262). That box sits inside 1080×2400, under the status bar, and the words are fully on screen.

### Non-blocking startup note

On the unfit nested emulator, the startup trace before the first frame showed main-thread work in `AppCompatTextView` class verification (`art::ClassLinker::VerifyClass` / `art::verifier::MethodVerifier::VerifyMethod` inside `AppCompatTextView.<init>`, reached from `setContentView`) and in `ResourcesCompat.getFont` / `loadFont` / `TypefaceCompat.createFromResourcesFamilyXml` via `AppCompatTextHelper.updateTypefaceAndStyle`. This KVM run did not repeat that trace. The note is recorded here and is not a Round 4 failure. On this runner OFFGRID's `Displayed` time is a few seconds and there is no ANR dialog.

### Evidence

Artifact `independent-qa-api-35` from run 39146533836. Stills used above live under `docs/qa/independent_qa/evidence/steps/` and `evidence/cold/`. Also in that artifact: `verdict.json`, `progress.log`, `kvm.txt`, `device.txt`, `static_checks.txt`, `reel_clicker.log` (empty), five `video/qa_r*.mp4` clips, and the instrument reports. `walk_exit=1` because the click item failed. `instrument_exit=0`.

## Round 5 — head 2fea30e on a KVM runner

Retest of every Round 4 item on the click-sound head. The fixer’s CI is not evidence. App code was not changed.

- Head built and installed: `2fea30ec71f29cfce4c20531d66f6ee15a759b70` ("Fire clicker on real drum drag and settle"). `evidence/static_checks.txt` records that SHA.
- Run: https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/39150242408 (QA commit `b54694b`). The emulator step ran from 17:37:25Z to 17:56:43Z. It finished. It did not hang.
- Host: 4 cores, 15 GiB, `/dev/kvm` mode 0666, `emulator -accel-check` printed "KVM (version 12) is installed and usable."
- Guest: API 35, `sdk_gphone64_x86_64`, 1080×2400 at 420 dpi, animation scales 1/1/1, `immersive_mode_confirmations` empty, SwiftShader 4.0.0.1.
- The click check issues a real `input swipe` up and down on Width and on Height, and runs `logcat -d -s OffgridClicker` after each swipe. A pass needs `SoundPool play streamId=` with a non-zero id in that read.
- I looked at the PNGs in artifact `independent-qa-api-35`. Frames taken after 17:49:42Z are 0-byte files.

**Overall verdict: BLOCKED.**

Splash, cold start, white skin, Night plus one black tap, the merged manifest, slider classes, and the title passed on the frames taken before the emulator dropped. The Width-up swipe was sent while both drums showed 100. The next `uiautomator dump` did not return a hierarchy, and the following screencap got `adb: device offline`. `emulator-5554` was then "not found". The click log was read after that, so an empty log is not a click result. Width down, Height up, Height down, copy pairing, Broadcast Lock, Minus, edit lock, and the view menu were not seen on a live screen. The instrument suite did not start on a device.

### Verdict table

| Item | Verdict | What was shown on this device |
| --- | --- | --- |
| (1) Splash / diamond | PASS | `cold/offgrid_01_early.png` is black with the status bar. Yellow samples on that frame: 0. `cold/offgrid_01.png` is the folder grid. Status: "The front camera is starting." |
| (2) Cold start | PASS | Ten force-stop starts of Settings, of an empty activity, and of OFFGRID. No ANR dialog. OFFGRID `Displayed` is 2.5–2.9 s. Settings is 2.4–4.6 s. The empty activity is 1.3–1.7 s. |
| (3) White skin | PASS | Skin color is open. Status: "Locked: white typewriter text on a white background can't be read." Lock glyphs sit on the swatches. Hex box shows `#FFFFFF`. Header W 100 H 100. |
| (4) Night + one black tap | PASS | Night is selected. Status: "Theme mode is now Night." The later typewriter frame still says "Locked: black typewriter text on a black background can't be read." |
| (5) Click sounds | BLOCKED | Before the swipe, both drums and the header are 100 (`steps/reel_width_up_before.png`). `input swipe 277 1488 277 1249 350` was sent at 17:48:30Z. The after-frame is 0 bytes. `logcat -s OffgridClicker` ran after `adb: device offline` and is empty. The other three swipes were not sent. |
| (6) Manifest | PASS | Merged debug manifest of `2fea30e` has no `RECORD_AUDIO` and no `FOREGROUND_SERVICE_MICROPHONE`. |
| (7) View menu vs status | BLOCKED | The menu was not opened. `steps/menu_open.png` was not produced. `steps/menu_before.png` is 0 bytes, taken after the device left adb. |
| (8) No slider classes | PASS | Source grep for `SeekBar`, `RangeSlider`, and `BioSliderView` is absent. Panel settings says "No sliders here. Only typed numbers and toggles." |
| Lock (edit) | BLOCKED | The lock was not tapped. `steps/lock_before.png` is 0 bytes. |
| Broadcast Lock | BLOCKED | The long-press was not shown. `steps/broadcast_before.png` is 0 bytes. |
| Minus | BLOCKED | Shrink was not shown. `steps/minus_before.png` is 0 bytes. |
| Copy pairing key | BLOCKED | Live and Remote was not opened. `steps/copy_pairing_blocked.png` is 0 bytes. The home probe ran after `emulator-5554` was not found. |
| PC or Android text | BLOCKED | Same gap as copy pairing. No frame shows the pairing sentence. |
| Title fit | PASS | With Panel settings open, "OFFGRID TERMINAL" is two lines at (137, 157, 241, 262), inside 1080×2400. The cold home grid omits that header. |
| P1-2 Kick flood | DEFERRED | Out of scope. Kick was not opened. |
| P1-3 Red sweep | DEFERRED | Out of scope. |

### (1) Splash — PASS

`cold/offgrid_01_early.png` is a black 1080×2400 frame with status icons and the gesture bar. The yellow-pixel sample is 0. `cold/offgrid_01.png` is the folder grid. No launcher-icon splash and no yellow diamond.

### (2) Ten cold starts — PASS

Each start was force-stop, then `am start -W`. No ANR dialog and no `ANR in` line. `Status: ok` on all thirty.

| Series | Displayed (ms) | LaunchState |
| --- | --- | --- |
| Settings | 4415, 4648, 3530, 2916, 3632, 3196, 2611, 2370, 2555, 3038 | COLD, except starts 2, 5, and 9 which are WARM |
| Empty activity | 1319, 1601, 1441, 1577, 1529, 1502, 1677, 1638, 1300, 1271 | all COLD |
| OFFGRID | 2906, 2609, 2804, 2710, 2628, 2597, 2536, 2583, 2532, 2516 | all COLD |

`cold/settings_01.png` is the Settings list. `cold/empty_01.png` is a blank activity titled "QA Empty". `cold/offgrid_01.png` is the folder grid. OFFGRID’s few seconds sit next to Settings. This runner is fit to time the app.

The instrument suite did not produce a result. `connectedDebugAndroidTest` failed in 7 seconds with `emulator-5554` not found. That is the dead emulator, not a test verdict.

### (3) White skin — PASS

`steps/white_after_tap.png`. Skin color is open. The status line is "Locked: white typewriter text on a white background can't be read." The hex box is `#FFFFFF`. Lock glyphs are on the swatches. The header still shows W 100 and H 100.

### (4) Night, then one black tap — PASS

`steps/night_chosen.png`: Night’s radio is selected, status "Theme mode is now Night."

`steps/black_after_2.png`: Typewriter text color is open, status "Locked: black typewriter text on a black background can't be read." The script’s first dump after the tap carried the same sentence.

### (5) Click sounds — BLOCKED

`steps/reel_width_up_before.png` at 17:48:28Z: Size is open, both drums on 100, the boxes under them on 100, header W 100 H 100, status "Size is open." The panel chrome is the normal header. That is the last good frame.

At 17:48:30Z the script cleared logcat and sent `adb shell input swipe 277 1488 277 1249 350` (finger moving up the Width drum). `uiautomator dump` then ran out its 25 second cap twice and never printed a hierarchy. The pull of that dump did not leave a file. At 17:49:42Z `screencap` printed `adb: device offline`. `steps/reel_width_up.png` is 0 bytes. At 17:50:01Z `logcat -d -s OffgridClicker` ran against that offline adb and returned nothing. At 17:50:21Z the job log says `error: device 'emulator-5554' not found`.

Width down, Height up, and Height down were skipped because the Width reel was no longer in a dump. No `SoundPool play` line was observed on a live device, and none was shown to be absent on a live device.

Screen recordings `qa_r1.mp4` through `qa_r3.mp4` end at 17:47:32Z, about a minute before the swipe. Clip 4 was not pulled.

### (6) Manifest — PASS

`evidence/static_checks.txt` for `2fea30e`:

- `android.permission.RECORD_AUDIO=absent`
- `android.permission.FOREGROUND_SERVICE_MICROPHONE=absent`

### (7) View menu — BLOCKED

The walk reached the menu step only after the emulator was gone. There is no open-menu frame to compare with the status line.

### (8) Sliders — PASS

`slider_grep=absent` in `static_checks.txt`. `steps/title_fit_pass.png` shows "No sliders here. Only typed numbers and toggles."

### Reconfirm — BLOCKED

Edit lock, Broadcast Lock, Minus, copy pairing, and the PC-or-Android sentence all run after the click check. Their screenshots are 0 bytes and their dumps were not saved. The before-swipe Size frame still shows the header lock, menu, and minus glyphs, and the folder grid from the cold start is intact, but those controls were not tapped on this run.

### Title — PASS

`cold/offgrid_01.png` is the home grid with no "OFFGRID TERMINAL" header. `steps/title_fit_pass.png` opens Panel settings. The header draws "OFFGRID TERMINAL" on two lines at (137, 157, 241, 262), inside 1080×2400.

### Non-blocking startup note

Unchanged from Round 4. On the unfit nested emulator, startup main-thread work before the first frame included `AppCompatTextView` class verification during `setContentView` and `ResourcesCompat.loadFont`. This run did not repeat that trace. It is not a Round 5 failure.

### Evidence

Artifact `independent-qa-api-35` from run 39150242408. Usable stills are the cold-start PNGs and the step PNGs through `reel_width_up_before.png`. `progress.log` has the swipe command, the two dump timeouts, `adb: device offline`, and `emulator-5554` not found. `reel_clicker.log` contains only the header `Width up 100 -> None`. `logcat.txt` is empty because the end-of-job `logcat -d` hit its 120 second cap on a missing device. `walk_exit=1`. `instrument_exit=1`.

## Round 5 retry — device loss is a runner flake

Current verdict. Head under test: `2fea30ec71f29cfce4c20531d66f6ee15a759b70`. QA workflow commit: `718671e`.

Run: https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/39152608207

Three jobs, `fail-fast: false`, logs uploaded with `if: always()`:

| Job | SHA | Result | What the swipe did |
| --- | --- | --- | --- |
| candidate-1 | `2fea30e` | walk_exit=0, instrument_exit=0 | `adb get-state` stayed `device` through Width up, Width down, Height up, and Height down. Full walk finished. |
| candidate-2 | `2fea30e` | walk_exit=1, instrument_exit=1 | Swipe at 18:19:55Z. `get-state` was `device` at 18:19:56Z and again at 18:21:08Z. `logcat -d` then printed `adb: device offline`. |
| control-6c7936e | `6c7936e` | walk_exit=1, instrument_exit=1 | `get-state` stayed `device` through all four drum swipes. The click log stayed empty, which matches Round 4 on this older head. |

The app-linked rule was: both `2fea30e` jobs lose the device at the drum swipe, and `6c7936e` does not. That pattern did not happen. One `2fea30e` job finished the walk. The control survived the same swipe. The loss on candidate-2 is a runner flake. Phone verdicts below are from candidate-1, artifact `independent-qa-candidate-1`.

### Why candidate-2 is a flake

`input swipe 277 1488 277 1249 350` ran at 18:19:55Z. The continuous `adb logcat -v time` stream, started at boot, shows three click lines from pid 7945 and then ordinary system chatter. There is no `FATAL EXCEPTION` and no `ANR in` anywhere in that stream.

```
10-18 18:19:55.975 I/OffgridClicker( 7945): SoundPool play streamId=121
10-18 18:19:56.061 I/OffgridClicker( 7945): SoundPool play streamId=129
10-18 18:19:56.724 I/OffgridClicker( 7945): SoundPool play streamId=137
```

The last lines of the stream, about two seconds later:

```
10-18 18:19:57.749 I/AiAiEcho( 1401): #postPredictionTargets: Sending updates to UISurface home with targets# 0 (types=[])
10-18 18:19:58.317 D/EGL_emulation( 7945): app_time_stats: avg=121.97ms min=112.21ms max=133.87ms count=8
10-18 18:19:58.440 D/BufferPoolAccessor2.0(  514): bufferpool2 0x79a50b6b8cb8 : 2(7776000 size) total buffers - 1(3888000 size) used buffers - 274/276 (recycle/alloc) - 8/276 (fetch/transfer)
```

`AudioFlinger` lines in that stream are from boot (`18:09:57`), when the audio HAL came up. Host `dmesg` grep in `host-diagnostics.txt` is `no oom lines`. Emulator stderr is one munmap warning (`cannnot unmap ptr ... protected range`). The qemu process was still sampled at 18:21:38Z (rss about 7.2 GiB on a 15989 MB host) and was gone by the 18:21:48Z sample, the same window as `adb: device offline`. The Width dump after the hang did not contain the reel, so the script recorded `100 -> None` and skipped the other three swipes. The plays above were already in the stream.

The control on `6c7936e` kept `get-state: device` for Width up (`100 -> 100`, empty `OffgridClicker` log), Width down (`100 -> 99`, empty), Height up (`100 -> 100`, empty), and Height down (`100 -> 98`, empty). Later on that same control job the Minus restore tap failed and the edit-lock and menu dumps were missing. That gap is after the swipes, on the older head, and is the same class of runner drop.

### Phone verdicts from candidate-1

**Overall phone verdict: PASS.** P1-2 and P1-3 stay DEFERRED.

| Item | Verdict | Evidence on candidate-1 |
| --- | --- | --- |
| Splash | PASS | `cold/offgrid_01_early.png` is a black 1080×2400 frame with status icons and the gesture bar. Yellow samples: 0. |
| Ten cold starts | PASS | No ANR dialog and no `ANR in` line. OFFGRID Displayed times sit next to Settings. |
| White skin | PASS | "Locked: white typewriter text on a white background can't be read." Hex `#FFFFFF`. |
| Night, then one black tap | PASS | Black panel. "Locked: black typewriter text on a black background can't be read." The sentence stayed. |
| Click sounds | PASS | Width up 100→98, Width down 98→100, Height up 100→96, Height down 96→100. Each gesture added new `SoundPool play` lines. Header, drums, and number boxes stayed laid out. |
| Manifest | PASS | `RECORD_AUDIO` absent. `FOREGROUND_SERVICE_MICROPHONE` absent. HEAD=`2fea30e`. |
| View menu | PASS | Status (14, 283, 1066, 399), card (231, 413, 1019, 1962), overlap 0. "Choose how folders look." sits above the menu. |
| Sliders | PASS | `slider_grep=absent`. The Panel settings note says "No sliders here. Only typed numbers and toggles." |
| Edit lock | PASS | "Panel is locked. Back, the menu, and Unlock still work." The folder grid stayed up. |
| Broadcast Lock | PASS | Unlock at (524, 2134, 738, 2242). The immersive cling stayed on screen. LOCKED and Unlock are at the bottom. |
| Minus | PASS | Restore tab (126, 262, 563, 441). "Panel hidden. Tap the OFFGRID tab, or press Back." |
| Copy pairing | PASS | "Pairing key made and copied. Paste it on a PC or an Android phone." Live and Remote stayed open. `clipboard_dump=False`. |
| PC or Android text | PASS | "Pairing works for a PC or an Android phone on the same Wi-Fi." |
| Title fit | PASS | "OFFGRID TERMINAL" at (137, 157, 241, 262) on 1080×2400 with Panel settings open. |
| P1-2 Kick flood | DEFERRED | Out of scope. |
| P1-3 Red sweep | DEFERRED | Out of scope. |

`verdict.json` repeats `SoundPool play streamId=129` in every gesture's detail string. The filtered `logcat -d` buffer is cumulative, and the script quotes the first play line it finds. The per-gesture sections of `reel_clicker.log` show the new ids:

- Width up, 100→98, status "Width is now 98.": streamId 129 at 18:21:33.607, 137 at 18:21:34.483.
- Width down, 98→100, status "Width is now 100.": 145, 153, 161 at 18:21:44.
- Height up, 100→96, status "Height is now 96.": 169, 177, 185, 193 from 18:21:49.312 to 18:21:50.595.
- Height down, 96→100, status "Height is now 100.": 201, 209, 217, 225 from 18:21:54.804 to 18:21:55.108.

`adb get-state` after each of those four swipes, and after each dump, was `device`.

### (2) Ten cold starts — numbers

Each start was force-stop, then `am start -W`. `Status: ok` on all thirty. Displayed times below are the `TotalTime` values.

| Series | Displayed (ms) | LaunchState |
| --- | --- | --- |
| Settings | 5332, 5553, 3142, 4311, 3366, 2801, 3333, 2980, 3303, 3634 | COLD, except start 9 which is WARM |
| Empty activity | 1848, 2363, 2171, 1893, 1719, 2232, 1887, 2068, 3148, 1543 | all COLD |
| OFFGRID | 4626, 3319, 3096, 3167, 3021, 3101, 3189, 3075, 2819, 2861 | all COLD |

OFFGRID’s few seconds sit in the same band as Settings. This runner is fit to time the app.

The instrument suite on this job finished. `RoyaltyPhoneFixTest` reports `tests="16" failures="0" errors="0"` in 188 seconds. `instrument_exit=0`.

### Non-blocking startup note

Unchanged. On the unfit nested emulator, startup main-thread work before the first frame included `AppCompatTextView` class verification during `setContentView` and `ResourcesCompat.loadFont`. This KVM run did not repeat that trace. It is not a failure.

### Evidence

Artifacts `independent-qa-candidate-1`, `independent-qa-candidate-2`, and `independent-qa-control-6c7936e` from run 39152608207. Candidate-1 stills used for the phone verdict include `cold/offgrid_01_early.png`, `steps/title_fit_pass.png`, `steps/white_skin_pass.png`, `steps/night_black_pass.png`, the four `steps/reel_*.png` frames, `steps/broadcast_armed.png`, `steps/minus_hidden.png`, `steps/copy_after.png`, `steps/menu_open.png`, and `steps/lock_locked.png`. Candidate-2 diagnostics are `logcat-stream.txt`, `device-state.log`, `progress.log`, `emulator-memory.log`, `emulator-process.stderr`, and `host-diagnostics.txt`.

