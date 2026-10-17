# Phase 1.2 clean surface and 1.3 Broadcast Lock, 2026-10-17

Branch `phase_1_2_and_1_3` from `final-build`. Pull request "Phase 1.2 clean surface + 1.3 Broadcast Lock" into `final-build`. Nothing merged, `main` untouched, no file deleted, no new permission beyond the approved `DETECT_SCREEN_RECORDING`.

Built against `docs/PREMIUM_ARCHITECTURE_ROADMAP_V2.md` sections 7.2 and 7.3 and the Chief Architect Review build order. A note on numbering: V2's own table calls the PREVIEW master 1.2 (shipped in PR #40), the Broadcast Lock 1.3, and the pristine surface 1.4 (section 7.2). This note and the PR use the task's words: "1.2" is the clean surface (V2 section 7.2) and "1.3" is the Broadcast Lock (V2 section 7.3).

## What changed, item by item

### 1.2 Clean broadcast surface (V2 section 7.2)

| Item | Change | Where |
|---|---|---|
| Floating `[OFFGRID]` pill | Never added to the view tree. `buildPill()` still builds it (code archived, nothing deleted) but the panel no longer calls `addView` on it, and `showMinimized` keeps it and its ring invisible. A shrunk panel is `GONE`, so no glass box is left on the stage. | `terminal/TerminalPanel.kt`, `terminal/PanelPlacement.kt` |
| Panel opens without the pill | A one-finger hold of 1.0 s in the top-left zone (the top-left 30 percent of the stage, inset 48 dp from both edges so it is never an edge gesture). Unlocked only. `setSystemGestureExclusionRects` covers that zone and the center lock zone. The panel also opens on every cold start, because a shrunk panel has no affordance on a clean stage. | `lock_gesture_map.kt`, `broadcast_lock_touch.kt`, `MainActivity.kt` |
| Coach mark | Once per install, on the first tap of the shrink button: one white-on-black line in the top-left zone, "Panel hidden. Hold one finger here for one second to bring it back." Six seconds, not a touch target. Never while recording, locked, or armed; a lock or a recording start removes it at once. | `coach_mark.kt`, `res/values/strings.xml` |
| `control_bar` status text and buttons | `control_bar` is `GONE` in the layout and nothing writes to it. `showMessage()` sends its words to the panel status line (`announce`) and to Phone 2 as `fx.status.line`. Permission errors become a panel notice button: "Try again" (asks for the camera again) or "Open Settings". | `status_line.kt`, `res/layout/activity_main.xml`, `terminal/TerminalPanel.kt` (`showNotice`) |
| "Record 30 s" HUD | Gone from the stage. The RECORDING words type in the panel status line, with the length when one is set. | `status_line.kt`, `recording_controls.kt` |
| Record and Play buttons | "Record / Stop" and "Play last video" are the first two rows of the Record folder. `EffectsController.onScreenAction` routes them to `toggleRecording()` and `showPlayback()`. | `terminal/EffectFolders.kt`, `terminal/PanelTree.kt`, `EffectsController.kt`, `MainActivity.kt` |
| Playback bar | `playback_bar` stays `GONE`. The saved video plays full stage; a tap on the stage or the system Back ends it and the panel comes back. The hint goes to the panel and Phone 2. | `playback_player.kt`, `broadcast_lock_touch.kt` |
| Toasts | `LiveIntegration.showStatus` sends the line to the panel and Phone 2; the Toast is only drawn while unlocked. | `LiveIntegration.kt` |

### 1.3 Broadcast Lock (V2 section 7.3, A1, A4, R1)

| Item | Change | Where |
|---|---|---|
| Permission | `<uses-permission android:name="android.permission.DETECT_SCREEN_RECORDING" />` declared (normal level, no prompt, approved and already on the CI allowlist). | `app/src/main/AndroidManifest.xml` |
| Callback | `WindowManager.addScreenRecordingCallback(mainExecutor, consumer)` registered in `onStart`, removed in `onStop`, guarded by `SDK_INT >= VANILLA_ICE_CREAM` (API 35). Its synchronous return feeds the store before the first frame. A `SecurityException` is logged and leaves the auto lock off; manual, remote, and armed still work. | `broadcast_lock_chrome.kt` |
| State machine | `locked = recordingVisible || manualLocked || remoteLocked || armed || resumeHold`. Sources AUTO, MANUAL, REMOTE, ARMED. `unlock()` is refused while VISIBLE (A4); when allowed it clears MANUAL, REMOTE, ARMED, and the resume hold. AUTO clears itself on NOT_VISIBLE. MANUAL, REMOTE, ARMED, and `was_locked` persist in SharedPreferences `broadcast_lock`. Observable through listeners. | `broadcast_lock_store.kt` |
| Resume gate | `onStop` hides every chrome view and persists `was_locked`. Every `onStart` starts hidden, registers the callback, and keeps the chrome hidden until the first stage frame plus 250 ms (or 1.5 s when no frame arrives, camera denied or off). Only then, and only unlocked, does the panel fade in over 120 ms. VISIBLE at any moment hides it on the same frame with no animation. Cold start inside a share shows only the stage. | `broadcast_lock_chrome.kt`, `MainActivity.kt` (`onStart`, `onStop`, `onFirstFrame`) |
| Manual lock gesture | Two-finger hold of 1.5 s anywhere in the middle 60 percent of the stage. Unlocked: locks (MANUAL). Locked: asks to unlock; refused with a `REJECT` buzz and a Phone 2 line while a share is live. Haptics follow the panel's tick setting. No edge gesture anywhere. | `lock_gesture_map.kt`, `broadcast_lock_touch.kt` |
| Touch sink | Under lock `dispatchTouchEvent` consumes every single-finger touch before any view sees it: no ripple, no menu, no view change. | `MainActivity.kt`, `broadcast_lock_touch.kt` |
| Chrome hidden under lock (R1) | The whole panel is `GONE` while locked: top bar, camera and mic toggles and their blocks, every slider and toggle, the menu, the keyboard. Playback closes. The coach mark closes. | `terminal/TerminalPanel.kt` (`setHiddenForLock`, `applyHidden`), `MainActivity.onLockApplied` |
| Lock glyph | A code-drawn padlock in the top bar next to the ⋮ menu: tap locks now, long-press arms or disarms. Its word (unlocked, armed, locked, live) follows the store. The six fixed controls and the W/H boxes narrow together when the title would otherwise lose its room, so the bar still fits at 360 dp. | `terminal/lock_glyph_button.kt`, `terminal/TopBar.kt` |
| Panel rows | Live and Remote: "Lock now" (the old Clean output row, same `LIVE_CLEAN_OUTPUT` action) and "Arm the lock". | `terminal/EffectFolders.kt`, `terminal/PanelTree.kt` |
| CLEAN_OUTPUT absorbed | `LiveActions.CLEAN_OUTPUT` is `BroadcastLockStore.lock(MANUAL)`. One source of truth. | `LiveIntegration.kt` |
| Floating overlay | Locked removes the overlay window (`LiveService.ACTION_LOCK`); its secure flag would otherwise sit on the broadcast as a black box. A SHOW request while locked is dropped. Unlock lets it back when the toggle wants it. The overlay keeps its flag for non-broadcast use. | `live/LiveService.kt`, `live/LiveOverlayController.kt`, `LiveIntegration.kt` |
| Phone 2 | Fire allowlist adds `lock_on`, `lock_off`, `arm`. The camera phone sends `fx.lock.state` (unlocked, armed, locked, live) and `fx.status.line` to Phone 2 for display. Phone 2's Remote Preview has a Broadcast Lock strip: Lock now, Arm, and a hold-to-confirm 1 s Unlock. Unlock is refused during a share with the reason shown on both phones. `REMOTE` lock state persists. | `live/RemoteMessage.kt`, `live/RemotePreviewActivity.kt`, `LiveHost.kt`, `LiveIntegration.kt`, `live/live_actions.kt` |
| Link reliability | `RemoteSession` had one monitor for send and receive, so the camera phone could not push a lock state while its reader thread was blocked in `receive()`. Send and receive now have separate locks. | `live/RemoteSession.kt` |
| Hard limits | No `FLAG_SECURE` on the activity or panel (CI now fails on the string anywhere under `app/src/main`). No `WAKE_LOCK`. No edge gestures. | `.github/workflows/build-debug-apk.yml` |
| CI guards both ways | The merged manifest must contain `DETECT_SCREEN_RECORDING`; `broadcast_lock_chrome.kt` must call `addScreenRecordingCallback`; `FLAG_SECURE` under `app/src/main` fails the build. | `.github/workflows/build-debug-apk.yml` |

### Tests

- `BroadcastLockStoreTest` (new): every source locks; unlock refused while VISIBLE and allowed after; `lock_off` clears MANUAL, REMOTE, ARMED; AUTO self-clears; persistence of MANUAL, REMOTE, ARMED, `was_locked`; the resume hold; the cold-start VISIBLE path; listeners hear each change once.
- `LockGestureMapTest` (new): the center zone is the middle 60 percent in both axes; the panel-open zone is inset from the edges; the two-finger recognizer fires once after the hold, cancels on slop, on a lifted finger, and on a third finger; the one-finger panel recognizer never fires outside its zone.
- `EffectPanelTest.broadcastLockRowsLiveInThePanelNowThatTheStageHasNoBars` (new): the Lock now and Arm rows, and Record / Stop and Play last video first in the Record folder.
- `RemoteLockMessageTest` (new, live module): the lock words are on the fire allowlist and round-trip; the display keys round-trip; the lock words stay out of the generic Phone 2 effect buttons.
- `PanelUiTest.topBarControlsShareCenterLineAtPhoneWidths` (existing) still passes with the Lock glyph at 360, 412, and 480 dp.
- Local: `./gradlew testDebugUnitTest assembleDebug lintDebug` from `04 Build`. BUILD SUCCESSFUL. 286 unit tests (247 app, 39 live), 0 failures. Lint 0 errors. The CI guard scripts (secrets, raw mic, speech, sliders, emoji warn, permission allowlist on the merged manifest, callback grep, `FLAG_SECURE` grep) pass locally.

## Phone test for Royalty (S25 only, no PC, no adb)

Install the CI "OFFGRID Preview" APK from the PR's Build debug app run (uninstall the old one first if the phone refuses the update). Allow the camera.

1. Clean stage. Open OFFGRID. The panel opens over the camera. Tap the shrink button (the minus). The panel leaves and nothing stays on the stage except the camera picture. Pass: no pill, no text, no bar anywhere.
2. Coach mark. Right after that first shrink, one white-on-black line sits in the top-left area for about six seconds and then goes away. Shrink again later: it does not come back.
3. Bring the panel back. With the panel hidden, hold one finger still near the top-left of the picture (a thumb's width in from the corner) for one second. The panel opens. A swipe from the very edge does nothing; that is on purpose.
4. Lock by hand. Hide the panel. Put two fingers in the middle of the picture and hold still for one and a half seconds. A short buzz means locked. Now tap, swipe, and poke the stage: nothing moves, nothing lights up, no menu. Two-finger hold again for one and a half seconds: a buzz, and the panel can be opened again.
5. Lock glyph. Open the panel. In the top bar the padlock sits next to the ⋮ dots. Tap it: the panel vanishes (locked). Two-finger hold to unlock. Open the panel, long-press the padlock: it arms (the panel hides and stays hidden). Two-finger hold to unlock; the arm clears.
6. Resume gate. Open the panel, press Home, come back. The camera picture shows first and the panel fades in a moment later. Nothing flashes before the picture.
7. Auto lock with the phone's own recorder. Pull down Quick Settings, start Screen recorder, come back to OFFGRID. The panel is gone and stays gone. Try the two-finger hold: a reject buzz, nothing opens. Stop the recorder from the notification: the panel comes back on its own a moment after the picture (only the recorder had locked it). Repeat with Lock now tapped first: after the recorder stops the panel stays hidden until a two-finger hold.
8. Cold start inside a recording. Start the Screen recorder, then close OFFGRID from Recents and open it again. Only the camera picture shows. Stop the recorder.
9. Bigo or any share app. Start a stream or share, switch to OFFGRID. Locked, clean. Phone 2 "Hold to unlock" is refused with the reason "Unlock refused. A screen share is live on the S25." End the share: the lock lifts by itself unless Lock now or Arm was used.
10. Phone 2 (optional, same Wi-Fi). Pair as before. On Phone 2's preview screen the Broadcast Lock strip shows "S25 lock: unlocked". Tap Lock now: the S25 locks and the line says locked. Hold Unlock for one second: unlocked. Tap Arm: armed. Status lines from the S25 (camera starting, recording, permission words) show on Phone 2.
11. Overlay. Turn on the floating overlay in Live and Remote, leave OFFGRID. Come back and lock (two-finger hold). The floating bubble is gone. Unlock: it returns.
12. Record and play. In the Record folder tap Record / Stop, then again to stop, then Play last video. The video fills the stage, the panel is off it. Tap the picture once: back to the camera. Nothing from the panel is in the video.

## Unfinished, with reasons

- **REC glyph with elapsed time in the top bar.** V2 section 7.2 says Record moves to the top bar; section 7.4 defines that glyph (REC, elapsed `mm:ss`, Screenshot). Seven fixed controls plus the title do not fit at 360 dp, and 7.4 brings REC together with unlimited recording. In this PR Record / Stop is the first row of the Record folder; the glyph lands with 7.4.
- **Stage-first boot through a `ViewStub`.** `TerminalPanel` is also the settings store and is read at `onCreate`, so inflating it later needs the store split out first (V2 lists this under 1.4). The panel is `GONE` in the layout and the reveal gate keeps it hidden until the stage has drawn, which gives the same screen; the view-tree guarantee by construction waits for the split.
- **`SurfaceCleanTest` and the Robolectric `PreviewView` scale test.** `MainActivity` cannot run under Robolectric in this repo. The store, the recognizers, the panel rows, and the protocol words are unit tested; the screen itself is on Royalty's list above.
- **Invisible Hotkeys** (three- and four-finger taps) are Phase 1.8 and were not started.
- **Phone tests** above are pending Royalty. Samsung's screen recording callback timing and the share app's behavior are UNVERIFIED on the device (V2 section 13).
