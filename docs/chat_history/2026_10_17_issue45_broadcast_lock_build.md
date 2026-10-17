# Issue #45: Broadcast Lock finish (stacked on PR #44)

Branch: `cursor/issue45-broadcast-lock-d6d6`, cut from `copilot/hr-release-gate-broadcast-lock` at `29a4e23` (PR #44).
Draft PR: https://github.com/stillwaters23/OFFGRID-Compositor/pull/49
PR #46 (`8877a4b`) was an empty commit and is not this work. Nothing was merged. `main` and `final-build` were not pushed.

## Per item

| Item | File:line | Test |
| --- | --- | --- |
| Pairing closes only when the API is at least 35 and the recording state is visible | `04 Build/live/src/main/java/com/offgrid/live/PairingSharePolicy.kt:23` | `PairingSharePolicyTest.thePolicyNumbersMatchThePlatformConstants`, `aVisibleShareOnAndroid15ClosesPairing`, `notVisibleLeavesPairingOpen`, `belowAndroid15DoesNothingEvenWhenTheStateSaysVisible` |
| Pairing registers the official callback in `onStart`, removes it in `onStop`, blanks the key and address, then finishes. `SecurityException` is logged and the screen stays up | `04 Build/live/src/main/java/com/offgrid/live/PairingActivity.kt:94` (`onStart`), `:98` (`onStop`), `:111` (`registerShareGuard`), `:122` (`addScreenRecordingCallback`), `:132` (`removeScreenRecordingCallback`), `:125` (`SecurityException`), `:139` (`closeForShare`) | `PairingSharePolicyTest.theGuardIsRegisteredOnlyByPairing` |
| The guard is not added to Phone 2's preview. That activity runs on Phone 2's own screen and shows a fingerprint, not the 128-character key | `04 Build/live/src/main/java/com/offgrid/live/RemotePreviewActivity.kt` (no callback). Pairing is the only registration | `PairingSharePolicyTest.theGuardIsRegisteredOnlyByPairing` |
| No new permission. `DETECT_SCREEN_RECORDING` is already declared by the app module and is in the merged manifest. The live manifest does not declare it and does not need a second copy, because Pairing runs in the app process | App manifest `04 Build/app/src/main/AndroidManifest.xml:24`. Live manifest has no `DETECT_SCREEN_RECORDING`. Merged debug manifest includes `android.permission.DETECT_SCREEN_RECORDING` and `com.offgrid.live.PairingActivity` | `PairingSharePolicyTest.theGuardIsRegisteredOnlyByPairing` (live manifest has neither `DETECT_SCREEN_RECORDING` nor `RECORD_AUDIO`) |
| When Pairing finishes, MainActivity's `onStart` rebuilds the lock | `04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:267-269` calls `lockChrome.onStart()`, which registers the camera-screen callback and calls `store.onStart` (`broadcast_lock_chrome.kt:58-63`) | No new test. Existing `BroadcastLockStoreTest.onStartReplacesAHeldNotVisibleWithTheFreshFirstValue`. Seeing the stage lock again after Pairing closes is phone-only |
| Camera permission is asked once, after the lock lifts, and never while locked. Store notifications do not ask again and do not call `showPermissionHelp` | `04 Build/app/src/main/java/com/offgrid/compositor/lock_guards.kt:21` (`cameraPrompt`). `MainActivity.kt:310` (`onLockApplied`), `:326` (`onResume`), `:334` (`followCameraPrompt`), `:351` (`askCameraPermission` still refuses while locked) | `LockGuardsTest.coldStartWhileTheShareIsVisibleAsksOnceAfterTheLockLifts`, `resumeAfterBackgroundingWhileLockedDoesNotAskOrShowHelp`, `aFreshInstallWhileUnlockedAsksOnce`, `cameraPermissionAlreadyGrantedNeverAsks`, `everyLockSourceHoldsTheCameraPrompt` |
| A successful two-finger unlock swallows the rest of the gesture, the same way a lock does | `04 Build/app/src/main/java/com/offgrid/compositor/lock_gesture_map.kt:46` (`swallowsRest`). `04 Build/app/src/main/java/com/offgrid/compositor/broadcast_lock_touch.kt:111-113` sets `swallowRest` and cancels views for both `LOCK` and `UNLOCK` | `LockGestureMapTest.anUnlockHoldSwallowsTheRestOfTheGestureTheSameWayALockDoes` |
| The panel comment no longer says Phone 2 can reopen a shrunk panel. No such command exists. Chat-history notes were not rewritten | `04 Build/app/src/main/java/com/offgrid/compositor/terminal/TerminalPanel.kt:391-393` | `RemoteLockMessageTest.phone2HasNoCommandThatReopensThePanel` |

## Test output

Command, in `04 Build`:

```
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
Android SDK platform 36 (platforms;android-36, build-tools;36.0.0)
./gradlew testDebugUnitTest assembleDebug
```

JDK:

```
openjdk version "17.0.20.1" 2026-08-18
OpenJDK Runtime Environment (build 17.0.20.1+1-1-24.04-Ubuntu)
```

Successful run:

```
BUILD SUCCESSFUL in 21s
91 actionable tasks: 19 executed, 72 up-to-date
```

Gradle did not print a one-line test total on success. The HTML reports written by that run say:

- `:app:testDebugUnitTest`: 265 tests, 0 failures, 0 ignored
- `:live:testDebugUnitTest`: 45 tests, 0 failures, 0 ignored

XML `test-results` for the same run: **310 tests, 0 failures, 0 errors, 0 skipped**. PR #44's baseline was 298. These 12 tests are the new ones named in the table. `assembleDebug` wrote `04 Build/app/build/outputs/apk/debug/app-debug.apk`.

An earlier run in this same session, before `@ConscryptMode(Mode.OFF)` was added to `PairingSharePolicyTest`, failed `:live:testDebugUnitTest` with 45 tests and 3 failures (`RemoteSecurityTest` handshake tests, `InvalidKeyException`). Robolectric had installed Conscrypt for the whole test JVM. The annotation matches `LocalNetworkTest`. The 21s run above is after that fix. The 3 handshake tests passed in it.

## CI

Not green. PR #44 workflow run `38994549173` ("Build debug app") is `action_required` and lasted 0 seconds. It did not compile or test. This PR targets `copilot/hr-release-gate-broadcast-lock`. That branch is not in the `pull_request` list of `.github/workflows/build-debug-apk.yml`, so the workflow does not run on this PR. The workflow file was not edited.

## Known limits (not passes)

- Below API 35 the official callback does not exist. The guard does nothing. The unit test covers that decision. The Samsung Galaxy S25 Ultra is Android 15 (API 35), which is the phone check below.
- If the user already tapped Copy key, the clipboard can still hold the key until the existing 60-second clear. This guard blanks the screen. It does not wipe a clipboard that was filled earlier.
- If `addScreenRecordingCallback` throws `SecurityException`, Pairing stays open and the refusal is logged. The merged manifest does include `DETECT_SCREEN_RECORDING`, so this is the failure path.
- `RECORD_AUDIO` is still declared on the app manifest. CI requires that declaration to stay. This change did not touch it, did not add `tools:node="remove"`, and added no microphone or voice code. The live manifest still has no `RECORD_AUDIO`.
- No `FLAG_SECURE` was added. The app module still has none. No slider, no new permission, no secret, no `.gguf`.

## Phone-only checks for Royalty (Samsung Galaxy S25 Ultra, Android 15)

These are BLOCKED until they are run on the phone. They are not a pass.

1. Install the debug APK from this draft PR. Do not install a release build, and do not merge.
2. Fresh install, no screen share. Open OFFGRID. Confirm the camera permission dialog appears once. Answer it. Leave and reopen the app. Confirm it does not ask again in a loop. If you denied it, the panel may show the help line while unlocked. Confirm that line is not repeated in a stack.
3. Start a Bigo screen share that is already capturing the phone, then cold-start OFFGRID. Confirm the camera permission dialog does not appear while the stage is locked (panel hidden). End the share. Confirm the dialog appears once, and that lock-word updates do not ask again or stack help lines.
4. With the camera permission not yet granted, open OFFGRID, let a share lock the stage, send the app to the background, then come back while the share is still on. Confirm there is no camera dialog and no help line while locked. End the share. Confirm the dialog appears once.
5. Unlock the stage. Open Pairing and tap Make pairing key so the 128-character key and the Wi-Fi address are on screen. Start a Bigo screen share with Pairing still in front. Confirm the key and the address disappear and Pairing closes. On a second device watching that share, confirm the key and the address were not visible.
6. After Pairing closes, confirm OFFGRID's camera screen is back and the stage stays locked for the rest of the share (panel hidden, no pairing text). End the share. Confirm the stage unlocks and the panel returns.
7. With no share running, hold two fingers still in the middle of the stage for 1.5 seconds to lock, then do it again to unlock. On the unlock, keep the fingers down and move them. Confirm that leftover gesture does not tap a row or open the panel. The lock hold should behave the same way.
8. On Phone 2, open Remote preview. Confirm it still connects and that this build did not add a share-close to that screen. Phone 2 is not the broadcast.
