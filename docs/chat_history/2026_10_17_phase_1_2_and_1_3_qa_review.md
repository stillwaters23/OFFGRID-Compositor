# Phase 1.2 and 1.3 independent QA review, 2026-10-17

Reviewer: independent QA agent (did not write this code). Pull request: [#42](https://github.com/stillwaters23/OFFGRID-Compositor/pull/42), branch `phase_1_2_and_1_3` into `final-build`. Reviewed head before QA: `bc54d10`. QA patches pushed to the same branch: `fa1379a`..`c0afd2c` (10 commits), then this note. Nothing merged, `main` untouched, no file deleted.

Judged against: `docs/PREMIUM_ARCHITECTURE_ROADMAP_V2.md` (approved; §4.2, §7.2, §7.3, §10, §13), `docs/ARCHITECT_REVIEW.md` (§2.1, §2.3, §4.2, §4.3, §4.7, §6), and the lessons in `docs/chat_history/2026_10_15_phase_0_qa_review.md`. Line numbers are at the QA head `c0afd2c` unless a commit is named. `S/` is `04 Build/app/src/main/java/com/offgrid/compositor/`, `L/` is `04 Build/live/src/main/java/com/offgrid/live/`.

## Overall verdict

**FAIL as submitted. PASS WITH PATCHES at the QA head. Phone tests still pending (BLOCKED items below).**

The state machine, A4, the gestures, the Phone 2 commands, and the `RemoteSession` lock split are built as V2 asks. The stealth surface was not. QA found and fixed:

- two crashes that would close the app (one on every cold start and every unlock with the floating overlay on, one when Phone 2 unlocks while Bigo is in front);
- one auto-lock defect that put the overlay back over Bigo, as a black box on air, whenever Royalty left OFFGRID during a share;
- six ways for a window to stay on the stage under the lock (overlay dialogs, panel pop-ups, coach mark, toasts, permission dialogs from Phone 2 settings, an OFFGRID screen launched from Phone 2);
- three CI guards that a bad commit would pass.

How it was checked:

- Every changed line of the PR diff was read against V2.
- The Android 15 screen-recording callback was checked in AOSP `android15-release` source and the official Android 15 behavior-changes page.
- `ActiveServices.java` was read for the foreground-service crash.
- The CI guard steps were run locally against the real tree and against 8 simulated bad trees.
- `./gradlew testDebugUnitTest assembleDebug lintDebug` was run before the patches (286 tests, 0 failures; lint 0 errors, 140 warnings in the app and 30 in live) and after them (291 tests, 0 failures; lint 0 errors, the same 140 and 30 warnings, none new).
- The merged manifest's permissions were listed.

CI "Build debug app":

- Green on `bc54d10` ([run 38989324786](https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/38989324786)).
- The run on the final QA head (this note on top of `c0afd2c`, same code) is linked from the PR description.

On-device reproduction of the crash on an API 35 emulator: **BLOCKED** in the QA VM. The emulator never booted: the guest did not start, and adb showed the device offline. The crash verdict rests on the AOSP source cited below, and the crash is test 1 on the phone list.

---

## Pass 1: Stealth (nothing OFFGRID-branded or UI chrome on the stage while locked, on resume, or on a cold start during a share)

| Item | Verdict | Evidence |
|---|---|---|
| Callback registered in onStart, removed in onStop | **PASS** | `S/broadcast_lock_chrome.kt:58-66` `onStart` → `register()` → `store.onStart(initial)`; `:72-80` `onStop` → `unregister()`; `S/MainActivity.kt:260-268`. |
| Guarded below API 35 | **PASS** | `register()` returns null below `VANILLA_ICE_CREAM` (`broadcast_lock_chrome.kt:178`); `unregister()` is behind the same check (`:211-212`); `SecurityException` caught (`:193`). Null means no AUTO lock, while MANUAL, REMOTE, and ARMED still work (`broadcast_lock_store_test.kt` `manualRemoteAndArmedWorkWithoutTheCallbackOnOlderPhones`). No API 33/34 device was available. |
| Synchronous first value | **PASS** | `broadcast_lock_chrome.kt:190` uses the return value of `addScreenRecordingCallback` and feeds `store.onStart` before any frame. AOSP `ScreenRecordingCallbackController.register()` returns `uidHasRecordedActivity(uid)`, which checks `ActivityRecord.isVisibleRequested()` in the recorded container. In `onStart` the activity is already visible-requested, so the first value is correct on a cold start and on a resume inside a share (V2 §7.3 step 2). |
| Stale reports after removal | **FAIL, patched, now PASS** | Client `ScreenRecordingCallbacks.notifyCallbacks` posts each report with `executor.execute(...)`, so a report queued before `removeScreenRecordingCallback` still ran after `onStop` and could change the lock. The consumer now ignores itself once it is not the live registration, and `register()` drops an earlier one first (`broadcast_lock_chrome.kt:179-188`). Commit `81c5b39`. |
| AUTO through leaving the app mid share | **FAIL, patched, now PASS** | AOSP `onProcessActivityVisibilityChanged(uid, false)` sends NOT_VISIBLE as soon as OFFGRID stops being drawn, which is while Bigo comes to the front, before `onStop` and possibly before `onPause`. The PR treated that as the share ending. AUTO cleared, `onStop` saved "was locked" as false, `LiveIntegration.onPause` brought the floating overlay back over Bigo (a FLAG_SECURE black box on air), and the panel could fade in during the exit animation. The existing test modelled the wrong order (VISIBLE, onStop, then NOT_VISIBLE). Fix in `81c5b39`: NOT_VISIBLE settles 500 ms (`broadcast_lock_chrome.kt:203-206`, `:234`). It clears AUTO only while resumed and otherwise waits for `onResume`. `onStart` replaces it with the fresh first value (`S/broadcast_lock_store.kt:158-182`, `:209-216`; wiring in `MainActivity.kt:312`, `:330`). Five new tests in `broadcast_lock_store_test.kt` use the real platform order. |
| Resume gate timing | **PASS** | Hidden in `onCreate`, `onStart`, and `onStop` (`broadcast_lock_chrome.kt:52-80`). Revealed only after the first stage frame plus `GRACE_MS` 250 ms (`:89-95`, `:228`), or after the `FALLBACK_MS` 1.5 s fallback with no camera (`:65`, `:231`). Then only if unlocked, with a 120 ms fade (`:137-170`). The first frame comes once per bind: `CompositorProcessor.kt:212` resets the flag, and `:649-651` posts to main. A held lock (`KEY_WAS_LOCKED`, `broadcast_lock_store.kt:201`) becomes a hold until the gate opens. |
| Overlay window removed under the lock | **FAIL, patched, now PASS** | The window is removed by `ACTION_LOCK` (`L/LiveService.kt:35`, `:50-57`). The three overlay dialogs (size, full screen, camera mode) are separate FLAG_SECURE overlay windows that `close()` did not touch. They are now tracked and dismissed with the window (`L/OverlayWindowController.kt:30`, `:78-80`, `:332-340`). Commit `42404ff`. The lock flag lived on the service instance and was lost when the service stopped itself; it now lives in `L/LiveOverlayController.kt:18` and gates SHOW, HIDE, and the permission dialog (`:22`, `:43`, `:48`; `LiveService.kt:69`). Commit `fa1379a`. |
| Foreground service crash (unlock and cold start with the overlay on) | **FAIL, patched, now PASS** | `unlockOverlay` started the service with UNLOCK, and `showOverlayIfWanted` then queued `startForegroundService(SHOW)`. UNLOCK ran `stopSelf()`. AOSP `ActiveServices` `stopSelf()` (start id -1) skips the last-start check, and `bringDownServiceLocked` with `fgRequired` posts `SERVICE_FOREGROUND_CRASH_MSG` ("did not then call startForeground"). `store.onStart` notifies on every start, so this hit every cold start and every unlock with the overlay setting on and the permission granted. Fix in `fa1379a`: unlock only clears the flag (`LiveOverlayController.kt:79-87`), and the service uses `stopSelf(startId)` (`LiveService.kt:36`, `:55`, `:74`). |
| Background crash on a Phone 2 unlock | **FAIL, patched, now PASS** | `lock_off` from Phone 2 while Bigo is in front reaches `openOverlay` from the background. When an app targets Android 15 (this app targets 36), the overlay-permission exemption needs a visible overlay window, and the lock has removed it. The result is `ForegroundServiceStartNotAllowedException` on the main thread ([Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15), "Restrictions on starting foreground services while an app holds the SYSTEM_ALERT_WINDOW permission"). Guarded in `c0afd2c` (`LiveOverlayController.kt:52-63`); the overlay returns on OFFGRID's next pause. |
| Panel, menu, keyboard hidden while locked | **PASS after patch** | `TerminalPanel.applyHidden` (`S/terminal/TerminalPanel.kt:532-542`) hides the keyboard, closes the menu, and sets the panel GONE; it is the only place the panel's visibility is written. Not covered before: the top bar's view-mode `PopupMenu` (`S/terminal/TopBar.kt:216-223`) and the color `PopupWindow` (`S/terminal/NeonColorRow.kt:92-96`). Both are windows of their own that survived the GONE, and their touches skip the touch sink. They are now tracked and dismissed (`S/terminal/PanelPopups.kt`, `TerminalPanel.kt:537`). Commit `fba0e89`. |
| Coach mark never visible while locked | **FAIL, patched, now PASS** | It hides on lock or arm (`MainActivity.kt:299-301`) but stayed through `onStop` for its 6 s timer, so a quick leave and return, or a resume into a share, drew it on the first frames. It is now hidden in `onStop` (`MainActivity.kt:268`). Commit `b093be4`. |
| Toasts never visible while locked or on air | **FAIL, patched, now PASS** | `LiveIntegration.showStatus` checked the lock but not whether OFFGRID was in front. Text toasts are drawn by SystemUI over any app, so link lines (often carrying the LAN address) toasted over Bigo during a share. It now toasts only when resumed and unlocked, and a lock or `onPause` cancels it (`S/LiveIntegration.kt:383-397`, `:98`, `:119`). Commit `ca26b3e`. |
| System dialogs from Phone 2 settings while locked | **FAIL, patched, now PASS** | Phone 2 may write any `fx.*` setting (`L/RemoteMessage.kt:11-12`, pre-existing). `fx.live.overlay=true` opened the overlay or its permission `AlertDialog`; it is now refused while locked (`LiveIntegration.kt:246-252`). `fx.remote.role=remote` launched `RemotePreviewActivity` over the stage; `launch()` now refuses while locked (`:237-244`). Both in commit `296ec84`. `fx.mic.on=true` opened the system mic-permission dialog through `applyCapture`; it now waits until the lock is off (`S/status_line.kt:110`). Commit `1cb973a`. |
| No FLAG_SECURE on the activity or panel | **PASS** | `grep` finds no secure flag in any form under `04 Build/app/src/main`. The overlay keeps its own (`OverlayWindowController.kt` `overlayFlags`, `showDialog`), as V2 allows. |
| Android 15 callback semantics match V2 §7.3 and §13 | **PASS, with one V2 gap** | The register and return value, the `DETECT_SCREEN_RECORDING` normal permission, NOT_VISIBLE on visibility loss, and overlay windows not being counted (they are not activities) all match AOSP. The gap: V2 says AUTO "self-clears on NOT_VISIBLE", but NOT_VISIBLE also means "OFFGRID left the screen". That is the leaving defect above. |

## Pass 2: Lock state machine and Phone 2

| Item | Verdict | Evidence |
|---|---|---|
| A4: unlock refused while VISIBLE, at the store | **PASS** | `S/broadcast_lock_store.kt:188-197`; the refusal is at `:190`, before anything is cleared. Every path below goes through it. |
| A4 from the two-finger hold | **PASS** | `S/broadcast_lock_touch.kt:105-110` → `BroadcastLockChrome.unlock()` (`broadcast_lock_chrome.kt:113-124`, REJECT buzz plus a status line, no view). |
| A4 from the glyph | **PASS** | `TopBar.kt:105-106` → `TerminalPanel.kt:813-814` → `MainActivity.kt:203-204` → `lockChrome.lock` / `toggleArmed` (`broadcast_lock_chrome.kt:98-111`). Note: under any lock the panel and glyph are GONE, so the glyph can only lock or arm. Its "hold to disarm" and the "Tap again to disarm" text on the Arm row cannot be reached while armed. Unlocking goes through the hold or Phone 2. |
| A4 from Phone 2 `lock_off` | **PASS** | `S/LiveHost.kt:73-74` posts to main → `LiveIntegration.kt:139-143` (`REFUSED_SHARE_VISIBLE` → "Unlock refused" on Phone 2). |
| A4 from the overlay expand and the panel Arm row | **PASS** | `LiveIntegration.kt:230-234` (`restoreCleanOutput`), `:195-199` (`LOCK_ARM`). |
| AUTO self-clears | **PASS after patch** | While in front: NOT_VISIBLE → 500 ms → `reportRecording(false)` clears AUTO (`store_test` `theShareEndingWhileOffgridIsInFrontClearsTheAutoLock`). After leaving: it waits for the next `onStart`/`onResume` (the leaving row in pass 1). Unlock never clears AUTO (`autoLockHoldsWhileVisibleAndClearsItselfWhenNotVisible`). |
| Persistence | **PASS** | MANUAL, REMOTE, and ARMED are written on every change (`broadcast_lock_store.kt:228-230`), and "was locked" is written in `onStop` (`:201`). AUTO is never persisted (`autoIsNeverPersisted`). A cold start while armed is locked with no callback (`aColdStartWhileArmedIsLockedWithoutAnyCallback`). |
| `RemoteSession` two-lock split | **PASS** | `L/RemoteSession.kt:22-23` separate monitors. `sendControl`/`sendVideo` take only `sendLock` (`:25-40`) and `receive` takes only `receiveLock` (`:42-62`). They never nest, so there is no lock-order deadlock. Each direction has its own `EncryptedChannel` (`:142-143`), so a send and a receive never touch shared cipher state. This also fixes a pre-existing stall: `receive()` used to hold the session monitor during its blocking read. Phone 2 sends run on their own single-thread executor (`LiveIntegration.kt:45`, `:147-158`); a late send after `close()` no longer throws (`:155`, commit `ca26b3e`). |
| Allowlisted commands only | **PASS** | `fire` only for `FIRE_ACTIONS` plus `lock_on`, `lock_off`, `arm` (`L/RemoteMessage.kt:11`, `:28-50`); `LiveHost.fire` routes `LOCK_ACTIONS` and drops anything unknown (`LiveHost.kt:68-79`). Settings remain `fx.*` only (pre-existing). `fx.lock.state` and `fx.status.line` are display-only on Phone 2. An echo written back to the S25 lands in panel prefs that the lock store never reads (the store has its own `broadcast_lock` file, `broadcast_lock_store.kt:92-100`). |
| No edge gestures | **PASS** | Lock: the middle 60 percent (`S/lock_gesture_map.kt:19`, `:31-36`). Panel open: the top-left zone inset 48 dp from both edges (`:44-48`; `MainActivity.kt:229`). The gesture-exclusion rects (`broadcast_lock_chrome.kt:127-136`) sit outside the system gesture insets, so they are harmless. Volume keys and the system back edge are system UI, out of scope. |
| Touch sink while locked | **PASS** | `broadcast_lock_touch.kt:52-75`: every touch is swallowed while locked; only the two-finger hold is watched. `MainActivity.kt:273-278` routes all touches through it before any view. Pop-up windows bypassed it; they are now dismissed on lock (pass 1). |

## Pass 3: Regressions and safety

| Item | Verdict | Evidence |
|---|---|---|
| No new permission beyond the allowlist | **PASS** | The only manifest change is `DETECT_SCREEN_RECORDING` (`04 Build/app/src/main/AndroidManifest.xml:24`), already on the allowlist. Merged manifest: `CAMERA`, `RECORD_AUDIO`, `INTERNET`, `DETECT_SCREEN_RECORDING`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `FOREGROUND_SERVICE_CAMERA`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`, and the AndroidX `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. CI guards it both ways (`.github/workflows/build-debug-apk.yml:236-239`). |
| No secrets | **PASS** | CI secrets scan run locally: "No saved secrets found." |
| No deleted files | **PASS** | `git diff --diff-filter=D origin/final-build...HEAD` is empty before and after QA. QA added one file (`PanelPopups.kt`) and this note. |
| Black/white System themes intact | **PASS** | No theme, color, or `PanelLook` change; the only resource change is one string (`coach_panel_open`). The glyph draws with `look.edge`/`look.decor` (`S/terminal/lock_glyph_button.kt`). The coach mark is white on black. |
| No steel/metal | **PASS** | No "steel/titanium/metal/sapphire/brushed" in any added line; "chrome" appears only as "UI chrome" in code names and comments. |
| MediaPipe cloak/ghost | **PASS (code); phone check pending** | `person_segmenter.kt`, `FaceTracker`, and the mask path are untouched by the PR. |
| Record / Stop, Play last video | **PASS (code); phone check pending** | They are rows in the Record folder (`S/terminal/EffectFolders.kt`, `PanelAction.RECORD_TOGGLE`/`PLAY_LAST`) → `EffectsController.onScreenAction` → `MainActivity.kt:288-295`. Playback stops on lock (`:299-300`) and on `onPause`. |
| Video Deck | **PASS (code); phone check pending** | `EffectsController` only gains `onScreenAction`; the deck path is untouched and `resume()` still restarts the deck. |
| Floating overlay, unlocked | **PASS after patches (code); phone check pending** | Unlocked, SHOW, HIDE, move, size, and close are unchanged. Under the lock it is removed and nothing reopens it (pass 1). The crash on unlock is fixed (`fa1379a`). |
| CI guards test the rule | **FAIL, patched, now PASS** | Simulated bad trees at `bc54d10`: deleting the `addScreenRecordingCallback` call while the KDoc still named it **passed**; deleting `removeScreenRecordingCallback` **passed**; `window.setFlags(8192, 8192)` **passed**; `SurfaceView.setSecure(true)` **passed**. Fixed in `50ebc4a` (`build-debug-apk.yml:268-282`), which requires both calls and refuses `FLAG_SECURE`, `setSecure(true)`, and 8192 or 0x2000 in `setFlags`/`addFlags`. Re-simulated, 8 of 8 correct: the real tree PASS; a missing permission, the add call removed, the remove call removed, `FLAG_SECURE`, `8192`, `0x2000`, and `setSecure(true)` all FAIL. |
| CI "Build debug app" green | **PASS on `bc54d10`; QA head linked from the PR** | [Run 38989324786](https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/38989324786) success. Local on `c0afd2c`: BUILD SUCCESSFUL, 291 tests, 0 failures, lint 0 errors. |
| Items the progress note lists as not done | **BLOCKED (not built)** | `SurfaceCleanTest`, a `ResumeGateTest` on the activity, the SDK 33 Robolectric run, Chrome flag parity / the `State` packet, the ViewStub stage-first boot, the REC glyph. The PR also says the panel can be opened from Phone 2; no such command exists. |

---

## What QA patched (same branch, small commits)

1. `fa1379a` Overlay: the lock flag is process-wide, unlock no longer starts the service just to stop it, and the service uses `stopSelf(startId)`. This fixes the "did not then call startForeground" crash on every cold start and every unlock with the overlay on.
2. `296ec84` Phone 2 settings: `fx.live.overlay` and `fx.remote.role=remote` cannot open the overlay, its permission dialog, or an OFFGRID screen while locked.
3. `42404ff` Overlay: its size, full-screen, and camera-mode dialogs close with the overlay window.
4. `81c5b39` Auto lock: NOT_VISIBLE settles 500 ms and clears AUTO only while OFFGRID is in front; stale callback reports are ignored. Five tests use the real platform order.
5. `b093be4` Coach mark: removed in `onStop`.
6. `fba0e89` Panel pop-ups: the view-mode menu and the color pop-up close when the panel hides (`PanelPopups.kt`).
7. `ca26b3e` Toasts: only while OFFGRID is in front and unlocked, cancelled on lock and on pause; a late Phone 2 send after `close()` no longer throws.
8. `1cb973a` Mic: no permission dialog while locked.
9. `50ebc4a` CI: the guards require the add and remove calls (not a mention) and refuse every form of the secure flag.
10. `c0afd2c` Overlay: a Phone 2 unlock with Bigo in front no longer crashes on Android 15's background foreground-service rule.

## What the build agent got wrong and how to avoid it

- **It read "NOT_VISIBLE" as "the share ended".** On Android 15 it also means "this app is no longer on screen", and that report arrives before `onStop`. The test that "proves" the resume gate fed the events in an order the platform never uses. *Avoid it:* before relying on a platform callback, read its implementation (here `ScreenRecordingCallbackController` and the client `ScreenRecordingCallbacks`), write down every cause of each value, and order test events the way the platform delivers them, with a citation.
- **It assumed removing a callback stops it.** The platform posts each report to the executor, so one can run after removal. *Avoid it:* every callback that changes state checks that it is still the live registration.
- **It started a service only to stop it, next to a `startForegroundService` that was still pending.** A bare `stopSelf()` brings the service down under a pending SHOW, and Android kills the app. It also kept the lock flag on a service instance that stops itself. *Avoid it:* never call `stopSelf()` without a start id in a service that also receives `startForegroundService`; keep state that must outlive the service in the process; reason about every queued start, not just the current one.
- **It checked the main window and forgot the other windows.** The panel GONE does not hide a `PopupMenu`, a `PopupWindow`, overlay `AlertDialog`s, system toasts, or system permission dialogs. *Avoid it:* for a stealth rule, grep the whole module for every API that creates a window (`PopupMenu(`, `PopupWindow(`, `AlertDialog`, `Dialog(`, `Toast.`, `addView(` on a `WindowManager`, `startActivity(`, `launch(` of a permission contract) and list each hit in the PR as "hidden under lock" or "cannot happen while locked".
- **It did not follow the Phone 2 setting paths.** Phone 2 can write any `fx.*` setting, and three of them open windows on the S25. *Avoid it:* for every remote entry point (`fire`, `writeSetting`), trace each key to what it does on screen, with the S25 in front and with Bigo in front.
- **It did not check the background case on Android 15.** A Phone 2 command can arrive while OFFGRID is in the background, where `startForegroundService` throws. *Avoid it:* for every call reachable from Phone 2, check the [Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15) for background limits (foreground services, activity starts, toasts).
- **Its CI guards matched text, not code.** A KDoc mention satisfied the "callback must be registered" guard, and the secure-flag ban caught only one spelling. *Avoid it:* (the Phase 0 lesson again) run each guard against a small table of bad trees; for "must call X", match `\.X\(`; for "must not set Y", ban the constant, its raw value, and every API that sets it.
- **It claimed features that are not there.** The PR says the panel can be opened from Phone 2 (there is no such command), and the Arm row says "Tap again to disarm" while the panel is hidden whenever the lock is armed. *Avoid it:* for each user-facing claim, name the code path that makes it true, or drop the claim.

## Remaining phone tests (Royalty, S25 Ultra and Phone 2, no PC)

Samsung's own **Screen recorder** (Quick panel) records the whole screen exactly as a Bigo share would capture it, and the result can be watched on the S25 afterwards. Use it for every "on air" check, then confirm once in a real Bigo share.

1. **No crash with the overlay on (patch 1):** in OFFGRID turn on the floating overlay and allow "display over other apps". Force stop OFFGRID (Settings › Apps › OFFGRID › Force stop) and open it three times. Then lock with the Lock glyph and unlock with the two-finger hold, three times. OFFGRID never closes, and "OFFGRID keeps stopping" never appears.
2. **Cold start inside a recording:** start Screen recorder, then open OFFGRID from the app icon. Stop the recording and watch it: only the stage, from the first frame. No panel, overlay, coach mark, or toast.
3. **Leave and come back during a recording (patch 4):** with the overlay on and Screen recorder running, open OFFGRID, then switch to Bigo (or any app) for 10 s, then back to OFFGRID. In the recording: the floating overlay never appears over the other app, and OFFGRID shows only the stage on return. Repeat with the Home gesture instead of switching apps.
4. **A4 during a recording:** while recording, do the two-finger 1.5 s hold in the middle. The phone buzzes and nothing appears; on Phone 2 press Hold to unlock: Phone 2 says "Unlock refused". Stop the recording with OFFGRID in front: about half a second later the panel fades in (if nothing else holds the lock).
5. **Windows close on lock (patches 3 and 6):** unlocked, open the overlay's camera-mode list, then press Lock now on Phone 2: the list disappears. Open the top bar's view-mode menu (or a color's info pop-up), then press Lock now on Phone 2: it disappears, and touching where it was does nothing.
6. **Phone 2 cannot open things while locked (patches 2 and 8):** with the S25 locked, on Phone 2 turn the mic on and switch the floating overlay on. Nothing appears on the S25. Unlock: the overlay returns when you next leave OFFGRID.
7. **Phone 2 unlock with Bigo in front (patch 10):** lock the S25, switch the S25 to Bigo with no share running, then Hold to unlock on Phone 2. OFFGRID does not crash (open it to check that it is still running and unlocked).
8. **No toasts over Bigo (patch 7):** with Phone 2 linked and the S25 on Bigo, turn Phone 2's Wi-Fi off for 20 s and back on. No OFFGRID message pops up on the S25.
9. **Coach mark (patch 5):** on a fresh install, shrink the panel. The one-time hint appears; press Home within 6 s and come back: it is gone.
10. **Arm across a cold start:** arm from Phone 2, force stop OFFGRID, open it: only the stage; Phone 2 shows "armed". Hold to unlock on Phone 2: the panel returns.
11. **Regression spot checks:** Camera modes Cloak and Ghost follow the body; Record / Stop then Play last video (tap the stage to return); the Video Deck plays and resumes after switching apps; the floating overlay opens, moves, resizes, and closes while unlocked; System Day (pure white) and Night (pure black) panels look as before.
12. **Not testable on the S25:** API 33/34 behavior (no AUTO lock, everything else works) needs an older phone; the code guard is checked above.
