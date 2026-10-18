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
