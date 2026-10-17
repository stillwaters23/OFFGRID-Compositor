HR Verdict: PASS WITH BLOCKED ITEMS

Independent HR release gate, replacing the earlier gate run that stopped mid-review. This note is docs only. Product code was not changed. Nothing was merged. `final-build` and `main` were not pushed. No release was published.

Reviewed together: draft PR #44 (`copilot/hr-release-gate-broadcast-lock` at `29a4e23`, into `final-build`) and draft PR #49 (`cursor/issue45-broadcast-lock-d6d6` at `c677b87`, stacked on #44, issue #45). Combined diff `origin/final-build...HEAD`: 14 files, +646 / −21. PR #46 is an empty commit, failed QA in PR #48, and is out of scope. Independent QA on #49 is PR #50. This gate re-read the code and re-ran the build. It does not treat that QA note as the result.

## Recommendation

NO-GO for merging right now.

The code review found no FAIL. Merge is allowed only when every condition in "Exact conditions for GO" is true. The order Royalty uses is draft PR #44 into `final-build`, then draft PR #49 retargeted onto `final-build`. Neither PR goes into `main`.

## Build and tests

Command, in `04 Build`:

```
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ANDROID_HOME set to the Android SDK used below
./gradlew testDebugUnitTest assembleDebug
```

JDK used by the Gradle daemon: OpenJDK 17.0.20.1 (2026-08-18), build 17.0.20.1+1-1-24.04-Ubuntu. `compileSdk` and `targetSdk` are 36. Installed platform: `platforms;android-36` revision 2. AGP 8.13.2 installed Android SDK Build-Tools 35.0.0 during this run and packaged with that. `build-tools;36.0.0` is also installed, matching the workflow's sdkmanager line. `minSdk` is 33.

```
BUILD SUCCESSFUL in 2m 9s
91 actionable tasks: 91 executed
```

XML `test-results` for that run:

| Module | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `:app:testDebugUnitTest` | 265 | 0 | 0 | 0 |
| `:live:testDebugUnitTest` | 45 | 0 | 0 | 0 |
| Total | 310 | 0 | 0 | 0 |

HTML reports: app 265 tests, 0 failures, 0 ignored; live 45 tests, 0 failures, 0 ignored.

`@Test` methods: 291 on `origin/final-build`, 310 on this head. The 19 added methods are the PR #44 set (7) and the issue #45 set (12). All 19 ran in this XML and passed. Debug APK: `04 Build/app/build/outputs/apk/debug/app-debug.apk`.

SDK 36 constant check, from `platforms/android-36/android.jar`: `SCREEN_RECORDING_STATE_NOT_VISIBLE` is 0 and `SCREEN_RECORDING_STATE_VISIBLE` is 1. `PairingSharePolicy.VISIBLE` is 1. `PairingSharePolicyTest.thePolicyNumbersMatchThePlatformConstants` passed on that jar.

## Findings

| Item | Result | file:line | Evidence |
| --- | --- | --- | --- |
| PR #44: Back is swallowed while locked | PASS (code). Phone step 7 is BLOCKED | `04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:171`, `:197`, `:208`, `:307`. `04 Build/app/src/main/java/com/offgrid/compositor/lock_guards.kt:9` | `lockBackCallback` handles Back by returning immediately. It is registered after the panel and playback callbacks, so the dispatcher reaches it first. It is enabled at startup from the stored lock state and again in `onLockApplied` whenever `LockState.locked` is true (AUTO, MANUAL, REMOTE, ARMED, resume hold). The panel's own Back callback turns off when `setHiddenForLock` sets the panel to GONE (`TerminalPanel.kt:524`, `:567`). Tests: `LockGuardsTest.backIsSwallowedForEveryLockSource`, `backIsFreeWhenUnlocked`, `theShareEndingDoesNotUnlockWhileArmed`. |
| PR #44: no camera permission dialog while locked | PASS (code). Phone steps 2–4 are BLOCKED | `lock_guards.kt:12`, `:21`. `MainActivity.kt:310`, `:326`, `:334`, `:351` | `mayShowPermissionDialog` is false while locked. `cameraPrompt` returns nothing when permission is granted, the activity is not resumed, or the stage is locked. `askCameraPermission` checks the store again and sets `permissionAsked` only after that check, then launches the dialog. `onResume` no longer launches it unconditionally. The mic ask is unchanged and still requires `resumed && !lockStore.locked` (`04 Build/app/src/main/java/com/offgrid/compositor/status_line.kt:110`). The Try again control sits on the panel, which is GONE while locked. |
| PR #44: hold outcome is fixed when the fingers land | PASS (code). Phone steps 8–9 are BLOCKED | `04 Build/app/src/main/java/com/offgrid/compositor/broadcast_lock_touch.kt:65`, `:107`. `04 Build/app/src/main/java/com/offgrid/compositor/lock_gesture_map.kt:36` | `lockedAtHoldStart` is stored on `HOLD_STARTED`, which is the second finger's pointer-down, the start of the two-finger hold. `fireLock` calls `holdOutcome(lockedAtStart, lockedNow)`. A difference yields `NONE`, so a hold that began locked cannot lock again if Phone 2 unlocks or the share ends during the 1.5 s, and a hold that began unlocked cannot unlock a lock that arrived under the fingers. Test: `LockGestureMapTest.aHoldMeansWhatItMeantWhenTheFingersLanded`. Corner and edge rejection: `everyCornerAndEdgeOfTheStageRejectsALockStart`. |
| Issue #45 item 1: Pairing closes on a visible share, API 35+ | PASS (code). Phone steps 5–6 are BLOCKED | `04 Build/live/src/main/java/com/offgrid/live/PairingSharePolicy.kt:18`, `:21`, `:23`. `04 Build/live/src/main/java/com/offgrid/live/PairingActivity.kt:94`, `:99`, `:111`, `:117`, `:122`, `:125`, `:130`, `:139` | `shouldClose` is true only when the API is at least 35 and the state equals `SCREEN_RECORDING_STATE_VISIBLE` (1). `onStart` registers `addScreenRecordingCallback` on the main executor. `onStop` clears the consumer before `removeScreenRecordingCallback`, so a queued report cannot close a later visit. The initial return value is applied after the consumer is installed. A visible state blanks the key, address, status, and pasted key, turns selectable text off, sets the decor view invisible, and `finish()`es. Async key and address callbacks return once `closedForShare` is set. `LiveIntegration.launch` still refuses to open Pairing while locked (`LiveIntegration.kt:237`). Tests: `thePolicyNumbersMatchThePlatformConstants`, `aVisibleShareOnAndroid15ClosesPairing`, `notVisibleLeavesPairingOpen`. `theGuardIsRegisteredOnlyByPairing` is a source scan. It passed. It does not launch the activity. The live Bigo capture is the phone step. |
| Issue #45 item 1: below API 35 | LIMIT. Not a pass | `PairingActivity.kt:112`. `PairingSharePolicy.kt:23` | The issue requires the guard to do nothing below API 35. The code returns before registering, and `shouldClose` is false. Test: `belowAndroid15DoesNothingEvenWhenTheStateSaysVisible`. `minSdk` is 33, so API 33 and 34 can still show the key during a capture. The Samsung Galaxy S25 Ultra is Android 15 (API 35). This limit is not protection and is not a pass. |
| Issue #45 item 1b: guard stays off Remote preview | PASS (code). Phone step 10 is BLOCKED | `04 Build/live/src/main/java/com/offgrid/live/RemotePreviewActivity.kt` (no callback). Test: `pairing_share_policy_test.kt:54` | Remote preview runs on Phone 2 and shows a fingerprint, not the 128-character key. The source scan found no `addScreenRecordingCallback` there. |
| Issue #45 item 1c: no new permission | PASS | `04 Build/app/src/main/AndroidManifest.xml:6`, `:24`. `04 Build/live/src/main/AndroidManifest.xml` (no `DETECT_SCREEN_RECORDING`). Merged debug manifest lists the same uses-permission set as the workflow allowlist | Manifests and `.github/workflows/build-debug-apk.yml` are identical to `origin/final-build`. `DETECT_SCREEN_RECORDING` was already declared on the app module. The live manifest does not declare it. Pairing runs in the app process, and the merged manifest contains `com.offgrid.live.PairingActivity` plus `android.permission.DETECT_SCREEN_RECORDING`. No permission was added by this diff. |
| Issue #45 item 1d: MainActivity rebuilds the lock when Pairing finishes | PASS (code). Phone step 6 is BLOCKED | `MainActivity.kt:267`. `04 Build/app/src/main/java/com/offgrid/compositor/broadcast_lock_chrome.kt:58` | `onStart` calls `lockChrome.onStart()`, which hides chrome, registers the camera-screen callback, and calls `store.onStart` with the synchronous recording value. A VISIBLE value locks. The resume gate keeps the panel hidden until the gate opens. When Pairing calls `finish()`, MainActivity reaches `onStart` while Pairing is still stopping. The two callbacks belong to two windows. Pairing has already blanked and hidden its decor view. |
| Issue #45 item 2: camera permission once, after the lock lifts | PASS (code). Phone steps 2–4 are BLOCKED | `lock_guards.kt:21`. `MainActivity.kt:310`, `:326`, `:334`, `:351` | A store notification may return `ASK_ONCE` when the lock lifts while resumed and the dialog has not been asked. The same notification returns nothing once `alreadyAsked` is true, so it never calls `showPermissionHelp`. A resume while still locked returns nothing. Tests that ran: `coldStartWhileTheShareIsVisibleAsksOnceAfterTheLockLifts`, `resumeAfterBackgroundingWhileLockedDoesNotAskOrShowHelp`, `aFreshInstallWhileUnlockedAsksOnce`, `cameraPermissionAlreadyGrantedNeverAsks`, `everyLockSourceHoldsTheCameraPrompt`. |
| Issue #45 item 3: unlock hold swallows the rest of the gesture | PASS (code). Phone step 8 is BLOCKED | `lock_gesture_map.kt:46`. `broadcast_lock_touch.kt:111` | `swallowsRest` is true for `LOCK` and `UNLOCK`. `fireLock` sets `swallowRest` and cancels view touches before it locks or unlocks, on the main thread, so the next move is swallowed. `NONE` does not swallow. That matches the written rule: a hold whose meaning changed is a no-op, and the successful unlock path still swallows. Test: `anUnlockHoldSwallowsTheRestOfTheGestureTheSameWayALockDoes`. |
| Issue #45 item 4: panel comment | PASS | `04 Build/app/src/main/java/com/offgrid/compositor/terminal/TerminalPanel.kt:391` | The comment says a shrunk panel is off the stage and a one-finger hold near the top-left opens it. It does not say Phone 2 reopens the panel. `RemoteMessage.FIRE_ACTIONS` (`RemoteMessage.kt:28`) has no panel, open, or reopen word. Test: `RemoteLockMessageTest.phone2HasNoCommandThatReopensThePanel`. Historical chat notes were left as they were. |
| Issue #45 item 5: local build | PASS (this run). CI is BLOCKED | This file, "Build and tests" | 310 tests, 0 failures, 0 errors, 0 skipped. `assembleDebug` succeeded. |
| Sliders | PASS | Diff against `origin/final-build` | No `SeekBar`, `RangeSlider`, or new `Slider` type. The only slider class in app sources remains `BioSliderView`, which the workflow already allows. |
| FLAG_SECURE in the app module | PASS | `04 Build/app/src/main` has no `FLAG_SECURE` and no `setSecure(true)`. Workflow `:279` | The diff adds none. The overlay flag stays in the live module (`OverlayWindowController.kt`), which is the pre-existing final-build design and is outside the app-module guard. |
| CI workflow not weakened | PASS (file). The run itself is BLOCKED | `.github/workflows/build-debug-apk.yml:12`, `:227`, `:269`, `:279` | The workflow file matches `origin/final-build`. It still requires the screen-recording callback calls, refuses `FLAG_SECURE` in `04 Build/app/src/main`, and fails the job if `RECORD_AUDIO` is missing. |
| Secrets and `.gguf` | PASS | Scan of `04 Build`, `.github`, and `docs/chat_history` | No saved-secret assignment of the workflow's pattern. No `.gguf` file in the tree. |
| `RECORD_AUDIO` | Informational. Not a fail | `AndroidManifest.xml:6`. Workflow `:227`. Merged debug manifest contains `android.permission.RECORD_AUDIO` | final-build's CI fails the build if `RECORD_AUDIO` is absent. This diff does not touch that line and adds no microphone or voice code. Royalty has since approved voice scope. The declaration stays. |
| CI "Build debug app" | BLOCKED. Not a pass | Run `38994549173` | Conclusion `action_required`. Status `completed`. `jobs` is empty. `createdAt` and `updatedAt` are both `2026-10-17T02:20:40Z`. The run never compiled. PR #49's base `copilot/hr-release-gate-broadcast-lock` is not in the workflow `pull_request` branch list (`build-debug-apk.yml:12`), so that workflow does not run on #49. `gh pr checks` reports no checks on either head. |
| Samsung Galaxy S25 Ultra | BLOCKED. Not a pass | Phone steps below | No phone was available in this review. |
| Clipboard after Copy key | LIMIT. Not a pass | `PairingActivity.kt:65` (copy still uses the existing 60-second clear). `PairingActivity.kt:139` blanks the screen | A key copied before the share starts can remain on the clipboard until that clear. The guard blanks the screen. It does not wipe an earlier clipboard. A Bigo viewer sees pixels, so this is not by itself a picture of the key. It is still not a pass. |
| `SecurityException` on the Pairing callback | LIMIT. Not a pass | `PairingActivity.kt:125` | The catch logs and leaves Pairing up, which is what issue #45 asked for. The merged manifest does include `DETECT_SCREEN_RECORDING`, so this is the refusal path. The log line says "The auto lock is off", which is the camera-screen wording. That string does not change the control flow. |

A limit in this table is not a pass. It is also not a FAIL against the written issue #45 requirements. A phone step that shows the pairing key or the Wi-Fi address on the shared picture is a stop, and the verdict becomes FAIL.

## Races and lifecycle checked

The hold timer and motion events share the main looper. `fireLock` records the swallow flag before `chrome.unlock()` or `chrome.lock()`, so the panel cannot appear under the fingers and then receive the rest of that gesture. The next `ACTION_DOWN` clears the swallow flag. An unlock that the store refuses because the share is still visible leaves the touch sink in place, because `locked` is still true (`broadcast_lock_touch.kt:73`).

`followCameraPrompt` during `onStart` sees `resumed == false`, because `onResume` sets `resumed` only after `lockChrome.onResume()`. A lock notification in that window cannot launch the dialog. The resume that follows asks once if the stage is unlocked and the dialog has not been asked. Later notifications do not ask again and do not show help.

`onLockApplied` runs after `hideChrome` or `showChrome` (`broadcast_lock_chrome.kt:145`). Enabling the Back swallow and hiding the panel happen on the same main-thread call, before the activity is drawing the next frame the user can tap.

## Exact conditions for GO

All of these are required. Any one missing stays NO-GO.

1. This verdict is still PASS WITH BLOCKED ITEMS. No row above is FAIL. Any later edit to product code voids this note.
2. A "Build debug app" run on the combined tree finishes with conclusion `success`. Run `38994549173` does not count. The run has to be one the workflow actually starts: a pull request into `final-build`, or a push to a branch already listed in `build-debug-apk.yml`. The job must use JDK 17 and SDK platform 36 and must pass `testDebugUnitTest` and `assembleDebug`.
3. Royalty runs phone steps 1 through 11 below on a Samsung Galaxy S25 Ultra (Android 15), using the debug APK from commit `c677b87` or a later commit that contains it and changes no product code. Every expected result happens. A missed or contradicted step is a stop. A skipped step is not a pass.
4. Merge draft PR #44 into `final-build` first. Retarget draft PR #49 onto that updated `final-build`, then merge #49. Do not merge #49 ahead of #44. Do not merge either PR into `main`.
5. Royalty knows that a push to `final-build` runs this same workflow, and the workflow's last step publishes the `step-2-preview` prerelease when the push is to `final-build` (`build-debug-apk.yml:298`). This review did not publish a release. If that prerelease must stay untouched, stop before the merge.
6. The three limits (below API 35, clipboard, `SecurityException`) stay limits. They are not reclassified as passes. They block GO only if a phone step shows the key or the address on the shared picture.

## Phone steps for Royalty (BLOCKED)

Device: Samsung Galaxy S25 Ultra, Android 15. Install the debug APK only. Do not merge first. Do not install a release build.

1. Screen: the phone's installer. Action: install the debug APK from draft PR #49, commit `c677b87`. Expected: OFFGRID installs. No GitHub release is created by the install.
2. Screen: OFFGRID camera screen, fresh install, no share. Action: open OFFGRID. Expected: the system camera permission dialog appears once. Leave the app and open it again. Expected: the dialog does not return in a loop. If the permission was denied, the panel shows one help line while unlocked, and that line is not stacked.
3. Screen: Bigo is already capturing the phone. Action: force-stop OFFGRID if it is open, then cold-start it. Expected: while the panel is gone, the camera permission dialog does not appear. End the Bigo share. Expected: the dialog appears once. Lock updates after that do not ask again and do not stack help lines.
4. Screen: camera permission still not granted, OFFGRID open, a Bigo share has hidden the panel. Action: press Home, then open OFFGRID again while the share continues. Expected: no camera dialog and no help line while the panel stays hidden. End the share. Expected: one camera dialog if it has never been asked.
5. Screen: OFFGRID unlocked, Pairing open, after tapping Make pairing key, with the 128-character key and the Wi-Fi address visible. Action: start a Bigo share while Pairing stays in front. If the paste field's keyboard is open, leave it open. Expected: the key and the address disappear, Pairing closes, and a second device watching the share never sees the key, the address, or the keyboard still showing the key.
6. Screen: OFFGRID camera screen immediately after Pairing closes, share still running. Expected: the panel stays hidden and no pairing text is on the picture for the rest of the share. End the share. Expected: the stage unlocks and the panel comes back. The panel does not flash on during the share.
7. Screen: OFFGRID camera screen with the panel hidden by a share or by a two-finger hold. Action: press the phone Back key. Expected: OFFGRID stays on the camera screen. The launcher does not replace the picture.
8. Screen: OFFGRID camera screen, unlocked, no share. Action: hold two fingers still in the middle of the stage for 1.5 seconds, then do it again to unlock. On the unlock, keep the fingers down and move them. Expected: the first hold hides the panel. The leftover movement after the unlock does not tap a row, open a folder, or open the panel.
9. Screen: OFFGRID locked by a two-finger hold, no share, fingers still down, before the 1.5 seconds end. Action: from Phone 2, send unlock. Expected: when the hold timer ends, the stage does not lock again. The leftover fingers do not tap a row.
10. Screen: Phone 2, Remote preview, connected. Action: use Remote preview as usual, including while Phone 2's own screen is shared if that is available. Expected: Remote preview stays open. This build does not close it on a share.
11. Screen: OFFGRID with the panel hidden by the lock. Action: turn the mic on from the panel before the lock, or have Phone 2 turn the mic on while locked, with microphone permission not yet granted. Expected: the microphone permission dialog does not appear while the panel is hidden.
