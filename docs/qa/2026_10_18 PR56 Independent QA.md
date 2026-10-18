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

**Overall verdict: FAIL.**

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
