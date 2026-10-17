# HR release gate: Broadcast Lock (finish audit)

Auditor: independent HR release-gate auditor (did not build or QA this work).
Base: `final-build` at `541588c` (PR #42). Working branch created from it. `main` was not touched.
Method: read the PR #42 diff (`git diff 541588c^1 541588c`), then read the store, chrome, touch sink, gesture map, MainActivity, LiveIntegration, LiveHost, live module (service, overlay, RemoteMessage, RemoteSession, RemotePreviewActivity), manifests and the CI workflow. Ran `./gradlew testDebugUnitTest` for the app and live modules. Phone-only items are marked BLOCKED.

## Per-area result

| Area | Result | Evidence |
| --- | --- | --- |
| Lock state machine (AUTO / MANUAL / REMOTE / ARMED, persistence) | PASS | `broadcast_lock_store.kt` `locked`, `change()` persists manual/remote/armed; AUTO never persisted; 22 tests in `broadcast_lock_store_test.kt` |
| Rule A4, unlock refused while VISIBLE on every path | PASS | One gate: `BroadcastLockStore.unlock()` (store.kt `if (now.recordingVisible) return REFUSED`). Callers: touch hold (`broadcast_lock_touch.kt` fireLock to `chrome.unlock`), Phone 2 `LiveIntegration.onLockCommand` LOCK_OFF, ARMED disarm (`chrome.toggleArmed`, `LiveIntegration.onAction` LOCK_ARM), overlay expand (`restoreCleanOutput`). Resume hold is released by `releaseResumeHold` and never bypasses VISIBLE because `onStart` reads the synchronous callback value first |
| Lead 1: persisted `was_locked` vs a MANUAL lock | PASS | MANUAL is held by `manualLocked` (persisted), not by the hold. The hold only adds time, and `unlock()` clears it. A stale `was_locked` after a crash holds the chrome until gate open (at most 1.5 s, `broadcast_lock_chrome.kt` FALLBACK_MS). Harmless |
| Lead 2: 500 ms debounce vs onStop | PASS | `onStop` cancels `notVisibleTask`, so AUTO stays through the background (intended: patch 4, documented in store.kt KDoc "Leaving the screen"). `onStart` replaces it with the fresh synchronous value (`store.onStart(initial)`), covered by `onStartReplacesAHeldNotVisibleWithTheFreshFirstValue` |
| Lead 3: gate startup, multi-window | PASS | `onStart` arms `fallbackTask` 1.5 s, so a camera already bound (no new first frame) still opens the gate. `onFirstStageFrame` is called from the processor's first-frame hook (`MainActivity.kt` near `lockChrome.onFirstStageFrame()`). Camera off: DarkStage shows and the fallback opens the gate |
| Lead 4: config changes, process death | PASS | Manifest: `screenOrientation=portrait`, no `configChanges`, so a recreate is possible. State lives in `PrefsLockPersistence`; the new store rebuilds manual/remote/armed/hold from prefs; `onDestroy` removes the listeners. Covered by the cold start tests |
| Lead 5: panel under the lock | PASS | `TerminalPanel.setHiddenForLock` sets GONE, closes keyboard, menu and `PanelPopups.dismissAll()`. `hiddenFromStage` includes `minimized`, so `showChrome` does not revive a shrunk panel. The open-panel hold is skipped when locked (`broadcast_lock_touch.kt` sink returns true first; `firePanelOpen` re-checks) |
| Lead 6: Back while locked | FAIL, fixed | With the panel GONE, `backCallback` and `playbackBackCallback` are disabled, so Back finished the activity and put the launcher on air. Fixed with `lockBackCallback` (`MainActivity.kt`, `LockGuards.swallowsBack`) |
| Lead 7: other windows (dialogs, toasts, permissions, startActivity) | FAIL (one), fixed | Toasts: `LiveIntegration.showStatus` only when resumed and unlocked. Overlay dialogs close with the window (`OverlayWindowController.close`). `startActivity`: `LiveIntegration.launch` refuses when locked (Pairing, RemotePreview). `pickArVideo` is panel-only, and the panel is GONE under the lock; Phone 2 cannot fire it (`RemoteMessage.FIRE_ACTIONS` has no such word). Mic dialog is gated (QA patch 6). **Camera permission dialog was not gated**: `MainActivity.onResume` launched it while locked. Fixed (`askCameraPermission`, `LockGuards.mayShowPermissionDialog`) |
| Lead 8: coach mark | PASS | `onShrunkByTap` allows only when not recording, not locked, not armed; hides on lock/arm (`onLockApplied`), stop, destroy, panel open. Recording start hides it through the panel path; recording a clip also requires the panel, which hides the mark on open |
| Lead 9: recognizers | PASS | Re-read `FingerHoldRecognizer`, zones, pointer clamp. Matches the tests |
| Two-finger hold intent | FAIL, fixed | `fireLock` decided lock versus unlock when the timer ran out. A hold begun while locked would re-lock if Phone 2 unlocked (or a share ended) during the 1.5 s. Fixed: `LockGestureMap.holdOutcome(lockedAtStart, lockedNow)` |
| `PairingActivity` | PASS | `live_actions.kt` OPEN_PAIRING only reaches it through `LiveIntegration.launch`, which refuses while locked and tells Phone 2. The activity has no lifecycle hooks, toasts or windows of its own. Return lands on MainActivity, whose `onStart` rebuilds the lock. BLOCKED (phone only): whether Pairing is on air when a share starts while it is in front |
| Gesture-map unit test | PASS after additions | Existing: zones, two-finger start, outside-zone, one finger, slide/lift/third finger, fired hold, panel-open zone. Added: hold outcome table (lock, unlock, flip either way), all four corners and one-finger-on-edge rejection, unlock-hold cancel. Refusal-while-VISIBLE and armed unlock are state-machine rules and are tested in `broadcast_lock_store_test.kt` (`unlockIsRefusedWhileTheShareIsVisible`, `theShareEndingReturnsToArmedNotToUnlocked`) |
| Live module tests in CI | PASS with a limit | CI runs `./gradlew testDebugUnitTest` at the root, which covers `:app` and `:live` (workflow "Build the debug app"). `remote_lock_message_test.kt` covers only the message allowlist and round trip. The REMOTE lock, unlock refused while VISIBLE and disarm messages run in `LiveIntegration.onLockCommand`, which needs Android classes, so they are covered at the store level and BLOCKED for an on-device check |
| Overlay and Android 15 FGS limits | PASS | `LiveOverlayController.broadcastLocked` is process-wide; `startOverlayService` catches the background start `IllegalStateException`; `LiveService.lockOverlay` uses `stopSelf(startId)`. Overlay `FLAG_SECURE` is in the live module only, by design; CI refuses it in `04 Build/app/src/main` |
| Phone 2 | PASS | Allowlist: `lock_on`, `lock_off`, `arm` plus the old fire words (`RemoteMessage.FIRE_ACTIONS`). Display keys `fx.lock.state`, `fx.status.line`. Unlock is a 1 s hold. No edge gestures anywhere (`lock_gesture_map.kt` center and inset zones; gesture exclusion set in the chrome) |
| Stealth on stage (locked, sharing, cold start, resume) | PASS, with one BLOCKED | Panel GONE, overlay window removed, toasts and dialogs gated, coach mark off in `onStop`, keyboard shut. BLOCKED (phone): that Android 15's own screen-share chip is not drawn by OFFGRID is outside the app's control |
| CI guards meaningful | PASS | The workflow greps for calls (`\.addScreenRecordingCallback\(`, `\.removeScreenRecordingCallback\(`), not mentions, and refuses `FLAG_SECURE`, `setSecure(true)` and the raw 8192 / 0x2000 flags in `04 Build/app/src/main`. It also requires DETECT_SCREEN_RECORDING |
| Permissions, secrets, deletions | PASS with a note | PR #42 added only `DETECT_SCREEN_RECORDING` (normal level, owner approved, required by the spec). `RECORD_AUDIO` is older than PR #42, is gated by the mic hard block tests and was not touched. No secrets (CI secret scan, spot check). PR #42 deleted no files |
| No regressions | PASS by tests, BLOCKED for visuals | All 298 unit tests pass (themes, MediaPipe, record/play, Video Deck, panel). Visual checks of System black and white, no steel, cloak/ghost and the overlay when unlocked need the S25 |
| Builder and QA open items | CLOSED or BLOCKED | Every phone-only item in the QA report stays BLOCKED: real Bigo share on the S25 Ultra (Android 15) |

## Defects found and fixed

1. Back exits the app while locked. Fix: `lockBackCallback`, enabled from `onLockApplied` and at startup. Tests: `LockGuardsTest.backIsSwallowedForEveryLockSource`, `backIsFreeWhenUnlocked`, `theShareEndingDoesNotUnlockWhileArmed`.
2. Camera permission dialog opens over a locked stage. Fix: `askCameraPermission` waits for the unlock and is called again from `onLockApplied`. Tests: `LockGuardsTest.noPermissionDialogWhileLockedForAnySource`.
3. A two-finger hold started locked can lock again when the lock changes under the fingers. Fix: `holdOutcome`. Tests: `LockGestureMapTest.aHoldMeansWhatItMeantWhenTheFingersLanded`.
4. Extra gesture-map coverage (corners, edge finger, cancel). Tests: `everyCornerAndEdgeOfTheStageRejectsALockStart`, `anUnlockHoldStartsLikeALockHoldAndKeepsHoldingStill`.

## What earlier agents got wrong or missed

- Builder and QA: missed the Back key (the panel's back callback goes off with the panel) and the camera permission dialog (QA gated only the mic dialog). Neither looked at the lock changing during a two-finger hold.
- Prior HR auditor: left leads 6 and 7 open; both led to real defects. Its leads 1 to 5, 8 and 9 held up.
- Docs claim "tests cover REMOTE lock and disarm in the live module". They cover the message format only; the behavior is covered in the store tests.

## Test count and CI

- Tests: 298 (291 at merge, plus 7 new). 0 failures, 0 errors, run locally with `./gradlew testDebugUnitTest` in `04 Build`.
- CI: the "Build debug app" workflow (`.github/workflows/build-debug-apk.yml`) runs on this PR. Its run link is on the PR checks tab; I could not read it from this session, so it is not quoted here.

## Verdict

Verdict: HOLD. Three real defects were fixed and tested (298 of 298 unit tests pass), but the PR's CI run has not been confirmed green and the phone-only checks (a real Bigo share on the S25 Ultra, Android 15) are BLOCKED. Release when the "Build debug app" run is green and Royalty has run the phone tests.
