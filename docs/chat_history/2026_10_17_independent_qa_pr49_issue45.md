QA Verdict: PASS

Code-checkable items for issue #45 pass. Phone checks and CI are BLOCKED and are not passes. Known limits are listed below and are not passes.

Independent QA of draft PR #49 (https://github.com/stillwaters23/OFFGRID-Compositor/pull/49), head `cursor/issue45-broadcast-lock-d6d6` at `c677b87`, base `copilot/hr-release-gate-broadcast-lock` = PR #44 at `29a4e23`. This review did not change product code, did not merge, and did not push `main` or `final-build`.

## Diff

`git diff 29a4e23...HEAD --stat` is real and stacked. Merge-base of `29a4e23` and `HEAD` is `29a4e23`. PR #44's head is that same commit. Two commits sit on top: `ed408e8` and `c677b87`.

```
14 files changed, 463 insertions(+), 22 deletions(-)
```

That matches the PR claim of +463/−22 across 14 files. The diff is product code, tests, and the issue #45 notes. It does not include a manifest, a workflow, a secret, or a `.gguf`.

## Tests

Command, in `04 Build`, after a cold install of the same tools the workflow uses:

```
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
openjdk version "17.0.20.1" 2026-08-18
Android SDK platforms;android-36 and build-tools;36.0.0
./gradlew testDebugUnitTest assembleDebug
```

`BUILD SUCCESSFUL in 2m 11s`. `91 actionable tasks: 91 executed`. `assembleDebug` wrote `04 Build/app/build/outputs/apk/debug/app-debug.apk`.

XML under `test-results/testDebugUnitTest` (suite `tests` / `failures` / `errors` / `skipped` attributes, summed per class file):

| Module | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| app | 265 | 0 | 0 | 0 |
| live | 45 | 0 | 0 | 0 |
| Total | 310 | 0 | 0 | 0 |

310 test cases were parsed from those XML files. The claimed 310 / 0 failures / app 265 / live 45 matches this run. PR #44's baseline of 298 plus these 12 new tests is 310.

Each of the 12 tests named in `docs/chat_history/2026_10_17_issue45_broadcast_lock_build.md` ran and passed. Each one asserts the decision it is named for:

| Test | Asserts |
| --- | --- |
| `PairingSharePolicyTest.thePolicyNumbersMatchThePlatformConstants` (`pairing_share_policy_test.kt:27`) | `CALLBACK_API` equals `VANILLA_ICE_CREAM` and `VISIBLE` equals `SCREEN_RECORDING_STATE_VISIBLE` (and not-visible is 0). |
| `aVisibleShareOnAndroid15ClosesPairing` (`:33`) | `shouldClose(35, VISIBLE)` and `shouldClose(36, SCREEN_RECORDING_STATE_VISIBLE)` are true. |
| `notVisibleLeavesPairingOpen` (`:38`) | API 35/36 with not-visible or an unknown state 2 returns false. |
| `belowAndroid15DoesNothingEvenWhenTheStateSaysVisible` (`:45`) | API 34, 33, and Tiramisu with a visible state return false. |
| `theGuardIsRegisteredOnlyByPairing` (`:51`) | Pairing source contains `.addScreenRecordingCallback(` and `.removeScreenRecordingCallback(` and `shouldClose`. Remote preview source contains neither callback. Neither activity source contains `FLAG_SECURE`. The live manifest contains neither `RECORD_AUDIO` nor `DETECT_SCREEN_RECORDING`. |
| `LockGuardsTest.coldStartWhileTheShareIsVisibleAsksOnceAfterTheLockLifts` (`lock_guards_test.kt:42`) | Locked before resume and a locked resume ask 0 times. The unlock notify asks once. Five later notifies stay at 1 ask and 0 help lines. |
| `resumeAfterBackgroundingWhileLockedDoesNotAskOrShowHelp` (`:56`) | A locked resume and locked notifies ask 0 and help 0, then one ask on unlock. After an earlier ask, locked resume and unlock notifies add no help. One unlocked resume shows help once, and later notifies do not add more. |
| `aFreshInstallWhileUnlockedAsksOnce` (`:83`) | One ask on the unlocked resume. Eight later notifies stay at 1 ask and 0 help. |
| `cameraPermissionAlreadyGrantedNeverAsks` (`:94`) | Granted stays at 0 asks and 0 help across resume and lock notifies. |
| `everyLockSourceHoldsTheCameraPrompt` (`:105`) | For recording, manual, remote, armed, and resume-hold, `cameraPrompt` returns null while that state is locked. |
| `LockGestureMapTest.anUnlockHoldSwallowsTheRestOfTheGestureTheSameWayALockDoes` (`lock_gesture_map_test.kt:170`) | `swallowsRest` is true for `LOCK` and `UNLOCK`, and false for `NONE`. |
| `RemoteLockMessageTest.phone2HasNoCommandThatReopensThePanel` (`remote_lock_message_test.kt:26`) | `FIRE_ACTIONS` has no value containing `panel`, and does not contain `open` or `reopen`. |

`theGuardIsRegisteredOnlyByPairing` is a source scan, and it passed because Gradle ran it with the live module as the working directory. It locks the registration scope. Blanking and `finish()` are in `PairingActivity.closeForShare` and are checked in the findings below. The camera tests drive `LockGuards.cameraPrompt` through the same ask / help / already-asked rules `MainActivity.followCameraPrompt` uses. The unlock test asserts the decision `LockTouch.fireLock` calls. The touch sink that consumes the rest of the gesture is cited below.

## Findings

| Item | Result | Evidence |
| --- | --- | --- |
| 1. Pairing screen-share guard | PASS (code) | `PairingSharePolicy.shouldClose` (`04 Build/live/src/main/java/com/offgrid/live/PairingSharePolicy.kt:23-24`) returns true only when `apiLevel >= 35` and `recordingState == 1` (`VISIBLE`). `PairingActivity.onStart` (`PairingActivity.kt:94-96`) calls `registerShareGuard`. That returns immediately below API 35 (`:112`). On API 35+ it calls `addScreenRecordingCallback` (`:122`), stores the consumer (`:123`), and closes on the initial state (`:124`). The later callback closes only for that same policy (`:116-118`) and ignores a stale consumer (`:117`). `SecurityException` is caught and logged (`:125-127`) and leaves the screen up. `onStop` (`:99-101`) calls `unregisterShareGuard` (`:130-135`), which removes the callback. `closeForShare` (`:139-153`) blanks `keyText` and `addressText`, clears selectable text, clears status and the pasted field, hides the decor view, and calls `finish()`. |
| 1b. Guard stays off Phone 2 Remote Preview | PASS | `RemotePreviewActivity.kt` has no `addScreenRecordingCallback` or `removeScreenRecordingCallback`. `LiveIntegration.launch` (`04 Build/app/src/main/java/com/offgrid/compositor/LiveIntegration.kt:237-243`) still refuses to open Pairing or Remote Preview while `lock.locked`. The guard class is only referenced from Pairing. |
| 1c. `DETECT_SCREEN_RECORDING` already inherited | PASS | The live manifest does not declare it (`04 Build/live/src/main/AndroidManifest.xml`, no `DETECT_SCREEN_RECORDING`). The app manifest already does (`04 Build/app/src/main/AndroidManifest.xml:24`). The merged debug manifest includes `android.permission.DETECT_SCREEN_RECORDING` and `com.offgrid.live.PairingActivity`. Pairing runs in the app process, so the live manifest does not need a second copy. This PR adds no permission. |
| 1d. MainActivity `onStart` rebuilds the lock when Pairing finishes | PASS (code) | `OffgridLiveTheme` is an opaque `Theme.Material.NoActionBar` (`04 Build/live/src/main/res/values/styles.xml:2-8`), and Pairing is started with `startActivity` (`LiveIntegration.kt:243`). Leaving that screen brings `MainActivity` back through `onStart`. `MainActivity.onStart` (`MainActivity.kt:267-269`) calls `lockChrome.onStart()`. That hides chrome, registers the camera-screen callback, and calls `store.onStart(initial)` with the synchronous first value (`broadcast_lock_chrome.kt:58-63`, `broadcast_lock_store.kt:209-217`). Seeing the stage lock again on the phone is BLOCKED. |
| 2. Camera permission once, after the lock lifts, never while locked | PASS (code) | `LockGuards.cameraPrompt` (`lock_guards.kt:21-32`) returns null when granted, not resumed, or locked. Otherwise it returns `ASK_ONCE` when not yet asked. A store notify after that returns null, so it does not call help. An unlocked resume after an ask returns `SHOW_HELP` once per resume. `onLockApplied` (`MainActivity.kt:306-311`) calls `followCameraPrompt(state.locked, fromLockNotify = true)`. `onResume` (`:319-326`) sets `resumed = true` and calls `followCameraPrompt(..., fromLockNotify = false)`. `followCameraPrompt` (`:334-347`) is the only new caller. `askCameraPermission` (`:351-355`) returns immediately when `mayShowPermissionDialog` is false, and sets `permissionAsked = true` only when it launches `CAMERA`. `onStart` notifies the lock before `onResume`, while `resumed` is still false, so a cold start inside a share does not ask. The five new `LockGuardsTest` methods cover cold start while visible, resume while locked, a fresh unlocked install, already granted, and every lock source. The Try again notice (`status_line.kt:202-203`) launches `CAMERA` from the panel. While locked, `LockTouch.dispatch` returns true for every touch (`broadcast_lock_touch.kt:73-76`) and the panel is hidden, so that notice is not reachable on a locked stage. |
| 3. Unlock hold swallows the rest of the gesture | PASS (code) | `LockGestureMap.swallowsRest` (`lock_gesture_map.kt:46-47`) is true for `LOCK` and `UNLOCK`, and false for `NONE`. `LockTouch.fireLock` (`broadcast_lock_touch.kt:111-114`) calls `cancelViews()` and sets `swallowRest = true` for both before `chrome.unlock()` (`:116`). `dispatch` returns true while `swallowRest` is set (`:78-80`) until the gesture ends. A new `DOWN` clears it (`:62`). |
| 4. TerminalPanel comment | PASS | `TerminalPanel.kt:391-393` says a one-finger hold near the top-left corner opens a shrunk panel. It does not say Phone 2 opens it. `RemoteMessage.FIRE_ACTIONS` (`RemoteMessage.kt:28-43`) is `shatter`, `freeze`, `quiet`, `fireworks`, `red_sweep`, `laser_writing`, `ar_play`, `ar_pause`, `ar_next`, `lock_on`, `lock_off`, and `arm`. The status log entry is an append. Older chat notes were left as they were. |
| 5. Build and tests | PASS locally. CI BLOCKED | Counts above. The workflow file is unchanged from `29a4e23`. |
| 6. Issue report files | PASS as documents of the author's claim | This QA re-checked the code and the tests. The author's `Verdict: HOLD` in the build note is their release call. This file is the independent gate. |

## Rule checks

| Rule | Result | Evidence |
| --- | --- | --- |
| Camera permission only for this change. No mic or voice code added | PASS for this diff | `git diff 29a4e23...HEAD` adds no `RECORD_AUDIO`, `AudioRecord`, `SpeechRecognizer`, `TextToSpeech`, microphone, or voice line. The ask path launches `Manifest.permission.CAMERA` (`MainActivity.kt:355`). The tree still contains the earlier mic path from the stack under PR #44. This PR did not add it. |
| No new permission from this PR | PASS | The 14-file diff has no `AndroidManifest.xml` and no `uses-permission`. |
| No sliders added | PASS | The diff has no `SeekBar`, `RangeSlider`, or `Slider(`. `04 Build/app/src/main` has no stock slider match. |
| No `FLAG_SECURE` in the app module | PASS | `04 Build/app/src/main` has no `FLAG_SECURE`, `setSecure`, or `0x2000`. The diff does not add one. The live overlay still has `FLAG_SECURE` from earlier work. The CI guard still refuses it under `04 Build/app/src/main` (`.github/workflows/build-debug-apk.yml:279-282`). |
| CI workflow and guards not weakened | PASS for the file. The run is BLOCKED | `git diff 29a4e23...HEAD -- .github` is empty. The callback greps (`:269-276`), the `FLAG_SECURE` grep (`:279-282`), and the permission allowlist (`:195-256`) are still in the file. |
| No secrets and no `.gguf` in this PR | PASS | The CI secret pattern matches no added line. `git ls-files '*.gguf'` is empty. |

### RECORD_AUDIO and DETECT_SCREEN_RECORDING

The build note says `RECORD_AUDIO` stays because CI requires it. An earlier look at `main` saw `tools:node="remove"` on `RECORD_AUDIO`. Both statements are about different branches. This PR changed neither manifest.

**This branch** (`c677b87`), source `04 Build/app/src/main/AndroidManifest.xml`:

- Line 7 declares `android.permission.RECORD_AUDIO` with no `tools:node`.
- Lines 9-11 remove `ACCESS_NETWORK_STATE`.
- Lines 13-15 remove `WAKE_LOCK`.
- Line 24 declares `android.permission.DETECT_SCREEN_RECORDING`.

Merged debug manifest from this run, `04 Build/app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml` (the same path family the workflow greps):

- Line 13: `<uses-permission android:name="android.permission.RECORD_AUDIO" />`
- Line 26: `<uses-permission android:name="android.permission.DETECT_SCREEN_RECORDING" />`

`processDebugMainManifest` has the same two entries. Neither node has `tools:node`. The merged file also contains `CAMERA`, `INTERNET`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `FOREGROUND_SERVICE_CAMERA`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`, and `com.offgrid.compositor.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. `PairingActivity` and `RemotePreviewActivity` are in that merged manifest. `ACCESS_NETWORK_STATE` and `WAKE_LOCK` are absent. The live source manifest declares neither `RECORD_AUDIO` nor `DETECT_SCREEN_RECORDING`.

**`main`** (`3e10d39`), source `04 Build/app/src/main/AndroidManifest.xml:6-9`:

```xml
<uses-permission
    android:name="android.permission.RECORD_AUDIO"
    tools:node="remove" />
```

`main` does not declare `DETECT_SCREEN_RECORDING`. A `processDebugManifest` of `main` with the same JDK 17 and SDK 36 produced this warning:

`uses-permission#android.permission.RECORD_AUDIO was tagged at AndroidManifest.xml:7 to remove other declarations but no other declaration present`

The merged debug manifest on `main` (`processDebugManifest/AndroidManifest.xml`) contains `CAMERA` (line 11), the AndroidX signature permission (line 27), and `ACCESS_NETWORK_STATE` (line 28). It contains no `RECORD_AUDIO` and no `DETECT_SCREEN_RECORDING`.

**CI on this branch** (unchanged by this PR), `.github/workflows/build-debug-apk.yml`:

- Lines 227-229 fail the job when the merged manifest does not contain `android.permission.RECORD_AUDIO`.
- Lines 236-238 fail the job when it does not contain `android.permission.DETECT_SCREEN_RECORDING`.

**CI on `main`**, `.github/workflows/build-debug-apk.yml`:

- Lines 76-81 fail if Java sources mention `RECORD_AUDIO`.
- Lines 109-111 fail if the merged manifest contains `android.permission.RECORD_AUDIO`.

So the build note is right about this branch's workflow: that workflow requires `RECORD_AUDIO` to remain in the merged manifest. The earlier QA observation is right about `main`: `main` strips `RECORD_AUDIO` with `tools:node="remove"`, and `main`'s workflow fails if the merged manifest still has it. This PR did not add the declaration, did not add `tools:node="remove"`, and did not edit the workflow. `DETECT_SCREEN_RECORDING` is on this branch's merged manifest and absent on `main`. This PR did not add that permission either. It was already on the app manifest at `29a4e23`.

## Not counted as passes

- Below API 35 the official callback does not exist. `registerShareGuard` returns at `PairingActivity.kt:112`, and `shouldClose` is false. The unit test covers that decision. It is not a pass of a share on an older phone.
- A key already copied can stay on the clipboard until the existing 60-second clear (`PairingWorkflow` posts that clear). `closeForShare` blanks the screen. It does not wipe an earlier clipboard copy. That limit is not a pass.
- A `SecurityException` from `addScreenRecordingCallback` leaves Pairing open and logs the refusal (`PairingActivity.kt:125-127`). The merged manifest does include `DETECT_SCREEN_RECORDING`, so this is the failure path. It is not a pass that Pairing always closes.
- CI has not run on this work. PR #44 run `38994549173` ("Build debug app") is `action_required` and lasted 0 seconds. `pull_request` on `.github/workflows/build-debug-apk.yml:12-19` lists `final-build`, `main`, `ccr-c441a76e-hgt5wa`, `step-2-2-premium-panel`, and `step-2-4-presets-layouts`. It does not list `copilot/hr-release-gate-broadcast-lock`, so that workflow does not run on PR #49. Local tests passing is not a green CI run.
- The Samsung Galaxy S25 Ultra steps below were not run.

## Phone steps for Royalty (BLOCKED)

Samsung Galaxy S25 Ultra, Android 15. These are not a pass.

1. Install the debug APK built from draft PR #49 (`cursor/issue45-broadcast-lock-d6d6`). Do not install a release build. Do not merge.
2. Fresh install, no screen share. Open OFFGRID. Confirm the camera permission dialog appears once. Answer it. Leave and reopen the app. Confirm it does not ask again in a loop. If you denied it, confirm the panel help line, while unlocked, is not repeated in a stack.
3. Start a Bigo screen share that is already capturing the phone, then cold-start OFFGRID. Confirm the camera permission dialog does not appear while the stage is locked and the panel is hidden. End the share. Confirm the dialog appears once, and that lock-word updates do not ask again or stack help lines.
4. With the camera permission not yet granted, open OFFGRID, let a share lock the stage, send the app to the background, then come back while the share is still on. Confirm there is no camera dialog and no help line while locked. End the share. Confirm the dialog appears once.
5. Unlock the stage. Open Pairing and tap Make pairing key so the 128-character key and the Wi-Fi address are on screen. Start a Bigo screen share with Pairing still in front. Confirm the key and the address disappear and Pairing closes. On a second device watching that share, confirm the key and the address were not visible.
6. After Pairing closes, confirm OFFGRID's camera screen is back and the stage stays locked for the rest of the share (panel hidden, no pairing text). End the share. Confirm the stage unlocks and the panel returns.
7. With no share running, hold two fingers still in the middle of the stage for 1.5 seconds to lock, then do it again to unlock. On the unlock, keep the fingers down and move them. Confirm that leftover gesture does not tap a row or open the panel. The lock hold should behave the same way.
8. On Phone 2, open Remote preview. Confirm it still connects and that this build did not add a share-close to that screen. Phone 2 is not the broadcast.
