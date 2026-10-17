# Independent QA: PR #46 Broadcast Lock follow-up

QA Verdict: FAIL

Reviewer: independent QA gate. This review did not change product code.
Date: 2026-10-17.
Pull request: https://github.com/stillwaters23/OFFGRID-Compositor/pull/46
Issue: https://github.com/stillwaters23/OFFGRID-Compositor/issues/45
Head reviewed: `8877a4b36eef40b539675967b22117b5d261cd6e` on `copilot/copilothr-release-gate-broadcast-lock`.
PR base on GitHub: `final-build` (`541588cb9208e06a9a3292a91af70446abfbd05d`).
Stack the issue required: `copilot/hr-release-gate-broadcast-lock` at `29a4e23c513a36a92fd1bbb2f331c4d0c35ed4c9` (PR #44). That commit was fetched and read only as a citation check. It is not the tree under test.

The head commit is an empty "Initial plan" commit. Its tree hash equals its parent `3e10d39edd73ff813682bc752551e191a7b8ddb6` (main, 2026-10-06). GitHub reports 0 changed files, 0 additions, 0 deletions. None of the files the description cites were added. A missing implementation is a failure. Phone-only behavior stays BLOCKED and is not counted as a pass.

## Findings

| Item | Result | Evidence |
| --- | --- | --- |
| Branch starts from PR #44 head `29a4e23` and stacks on `copilot/hr-release-gate-broadcast-lock` | FAIL | Head parent is `3e10d39` (main, 2026-10-06). `git merge-base HEAD origin/final-build` is `3e10d39`. `git diff --stat origin/final-build...HEAD` is empty. GitHub `baseRefName` is `final-build`, not the PR #44 branch. |
| PR contains the work the description lists | FAIL | `git show --stat 8877a4b` lists no files. `git rev-parse HEAD^{tree}` and `3e10d39^{tree}` are both `bcc062887aa84eb8e8f62e5c28b15750df8bd837`. |
| 1. `PairingActivity` screen-share guard (API 35+, register in `onStart`, remove in `onStop`, hide and `finish()` on `SCREEN_RECORDING_STATE_VISIBLE`, no-op below API 35, catch `SecurityException`) | FAIL | `04 Build/settings.gradle.kts:18` includes only `:app`. There is no `04 Build/live/` tree and no `PairingActivity.kt`. No `addScreenRecordingCallback` anywhere under `04 Build`. |
| 1. When Pairing finishes, `MainActivity.onStart` rebuilds the lock | FAIL | This tree's `MainActivity` has no lock and no `onStart` override. `04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:136-147` resumes straight into the camera permission request. |
| 1. Guard stays on Pairing only, with the Phone 2 choice written in the report | FAIL | `RemotePreviewActivity.kt` is absent. `docs/chat_history/2026_10_17_hr_release_gate_broadcast_lock.md` is absent. |
| 1. Pure decision function and JVM tests | FAIL | Zero `*Test*.kt` files under `04 Build`. Gradle task `:app:testDebugUnitTest` is `NO-SOURCE`. |
| 1. Add no new permission; stop if the live module cannot see `DETECT_SCREEN_RECORDING` | FAIL for the feature, rule held | The live module does not exist, so there is nothing to inherit the permission. Source manifest `04 Build/app/src/main/AndroidManifest.xml:5-9` declares `CAMERA` and a `tools:node="remove"` for `RECORD_AUDIO`. It does not declare `DETECT_SCREEN_RECORDING`. The built merged manifest (`app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml:11`) has `CAMERA` and no `DETECT_SCREEN_RECORDING`. The empty commit added no permission. |
| 2. Camera permission is asked once, only after the lock lifts, never while locked | FAIL | `MainActivity.kt:136-147` launches `Manifest.permission.CAMERA` from `onResume` whenever it has not been asked yet. There is no `LockGuards`, no `askCameraPermission`, and no `onLockApplied`. |
| 2. Tests: visible-share cold start, resume while locked, unlocked fresh install, repeated store notifies, already granted | FAIL | `04 Build/app/src/test/java/com/offgrid/compositor/lock_guards_test.kt` does not exist. The description's range `33-62` has no file to point at. |
| 3. Successful two-finger unlock sets `swallowRest = true` | FAIL | `04 Build/app/src/main/java/com/offgrid/compositor/broadcast_lock_touch.kt` does not exist. |
| 3. `TerminalPanel` comment no longer claims a Phone 2 command reopens the panel | FAIL | On this head, `04 Build/app/src/main/java/com/offgrid/compositor/terminal/TerminalPanel.kt:391-395` is `showMinimized`, `minimized = value`, `prefs.minimized = value`, and `hideKeyboard()`. There is no Phone 2 comment in that file. The cited lines were not edited (`git diff 3e10d39 HEAD` is empty). |
| 4. `./gradlew testDebugUnitTest assembleDebug` on JDK 17, SDK 36, count at least the prior 298, 0 failures | FAIL | The command ran and the APK assembled. The test task executed 0 tests because it is `NO-SOURCE`. That is not 304 tests, and it is not an increase from PR #44's reported 298. See the build summary below. |
| 4. Say plainly that PR #44's "Build debug app" workflow is `action_required` and has not run. Do not call CI green | PASS as a statement in the PR text | Run `38994549173` on head `29a4e23`, event `pull_request`, conclusion `action_required`, jobs empty: https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/38994549173. An earlier run `38994224272` has the same conclusion. `GET .../commits/29a4e23/check-runs` returned `total_count: 0`. This QA run also does not call CI green. PR #46's own workflow run `39001748265` is `action_required` with no jobs: https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/39001748265. |
| 5. Update `docs/chat_history/2026_10_17_hr_release_gate_broadcast_lock.md` (Pairing row, new defect, test count, `Verdict: RELEASE` or `Verdict: HOLD`) | FAIL | That file is not in this tree. |
| 5. Append one line to `08 Chat History/Agent Status Log.md` | FAIL | The log's last entry is still the Step 2 note at `08 Chat History/Agent Status Log.md:63-70` (2026-10-06). The empty commit does not touch the file. |
| 5. PR description gives PASS/FAIL per issue item with test names, the test count, build output, and file:line | FAIL | The description asserts the work landed and cites line ranges. It does not give a PASS/FAIL row per issue item, and the cited files are not in the diff. |
| Description: `PairingActivity.kt:26-59,156-160` registers the callback, hides, finishes, and unregisters | FAIL | File absent on this head. On the stack base the same path is 158 lines and those ranges are the pairing form and color constants, not a recording callback. `onCreate` builds the key `TextView` at lines 30-31 and the address `TextView` at lines 35-36. `onDestroy` at lines 113-116 only closes the workflow. Lines 156-157 are `const val UV`. There is no `onStart` and no `onStop`. |
| Description: the close policy is a pure function and is tested | FAIL | No such function and no test class on this head. `git grep` of `addScreenRecordingCallback` / `SCREEN_RECORDING_STATE` / `VANILLA_ICE_CREAM` on PR #44's `04 Build` hits `broadcast_lock_chrome.kt` in the app module only, not `PairingActivity`. |
| Description: guard stays scoped to Pairing because `RemotePreviewActivity` does not display the pairing secret | FAIL for this PR | Neither activity is in this diff. On the stack base, `RemotePreviewActivity.kt:86-89` keeps the key in `pairingKey` and shows the fingerprint only (`"Pairing fingerprint: $fingerprint"`). That does not put a guard into PR #46. |
| Description: no permission was added, and `DETECT_SCREEN_RECORDING` is in the merged manifest | FAIL | No permission was added (empty diff). The merged debug manifest from this build does not contain `DETECT_SCREEN_RECORDING` (merged manifest lines 11, 27, 28). |
| Description: camera request is claimed once, and `lock_guards_test.kt:33-62` covers the five cases | FAIL | File absent here. On the stack base the file ends at line 39. Lines 33-38 are `theShareEndingDoesNotUnlockWhileArmed`, which checks `swallowsBack` and `mayShowPermissionDialog` for an armed state. They do not cover a visible-share cold start, a locked resume, a fresh install, repeated store notifications, or an already-granted permission. |
| Description: unlock swallows the rest of the gesture at `broadcast_lock_touch.kt:107-122` | FAIL | File absent here. On the stack base, `fireLock` at lines 107-118 sets `swallowRest = true` only in the `HoldOutcome.LOCK` branch (lines 112-115). The `HoldOutcome.UNLOCK` branch at line 111 is `chrome.unlock()` and does not set `swallowRest`. |
| Description: Phone 2 panel-reopen comment corrected at `TerminalPanel.kt:391-395` | FAIL | This head's lines 391-395 are `showMinimized`, quoted above. On the stack base, lines 391-394 still say a shrunk panel opens again from "a one-finger hold near the top-left corner, or Phone 2". |
| Description: 304 tests, 0 failures, build successful | FAIL | Build of this head succeeded. Tests executed: 0. Failures: 0. The 304 figure was not produced by this tree. |
| Description: Bigo check on the Samsung Galaxy S25 Ultra is still outstanding | BLOCKED | No phone was available in this session. The steps are below. This head also has no Pairing screen to put on air (`versionName = "0.3"` at `04 Build/app/build.gradle.kts:17`, launcher activity is `MainActivity` only, manifest lines 28-37). |

## Build and test summary

Command, from `04 Build`:

```text
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ANDROID_HOME=/home/ubuntu/android-sdk
ANDROID_SDK_ROOT=/home/ubuntu/android-sdk
./gradlew testDebugUnitTest assembleDebug
```

Toolchain actually used:

- `java -version`: openjdk version "17.0.20.1" 2026-08-18 (Ubuntu OpenJDK 17.0.20.1+1-1-24.04).
- Android SDK platform installed: `platforms/android-36`. Build-tools `36.0.0` were installed before the run. Gradle then also installed build-tools `35.0.0`.
- `04 Build/app/build.gradle.kts:8` `compileSdk = 36`. Line 15 `targetSdk = 36`. Line 28 `sourceCompatibility = JavaVersion.VERSION_17`.
- Gradle wrapper: 8.14.5 (`04 Build/gradle/wrapper/gradle-wrapper.properties`).

Result:

```text
> Task :app:compileDebugUnitTestKotlin NO-SOURCE
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes NO-SOURCE
> Task :app:testDebugUnitTest NO-SOURCE
> Task :app:assembleDebug

BUILD SUCCESSFUL in 1m 22s
40 actionable tasks: 40 executed
```

Manifest warning during `:app:processDebugMainManifest`:

```text
04 Build/app/src/main/AndroidManifest.xml:7:5-9:31 Warning:
uses-permission#android.permission.RECORD_AUDIO was tagged at AndroidManifest.xml:7 to remove other declarations but no other declaration present
```

Counts:

| | |
| --- | --- |
| Tests executed | 0 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Test result directory | none (`testDebugUnitTest` was `NO-SOURCE`) |
| APK | `04 Build/app/build/outputs/apk/debug/app-debug.apk` (about 18 MB) |

The description's "304 tests, 0 failures" does not match this run. Zero failures here means the test task had no sources.

Merged debug permissions (`app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml`):

- Line 11: `android.permission.CAMERA`
- Line 27: `com.offgrid.compositor.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (AndroidX library)
- Line 28: `android.permission.ACCESS_NETWORK_STATE` (library merge)
- `RECORD_AUDIO`: absent from the merged manifest
- `DETECT_SCREEN_RECORDING`: absent from the merged manifest

## Standing rules on this head

| Rule | Result | Evidence |
| --- | --- | --- |
| Camera permission only. No `RECORD_AUDIO`, mic, or voice code added in this PR | PASS | The commit changes no file. Java/Kotlin sources under `04 Build/app/src/main/java` have no `RECORD_AUDIO`, `withAudioEnabled`, `TextToSpeech`, `SpeechRecognizer`, `AudioRecord`, or `android.speech`. The source manifest's only live permission is `CAMERA` at `AndroidManifest.xml:5`. Lines 6-9 remove `RECORD_AUDIO` if a library adds it. The merged manifest has no `RECORD_AUDIO`. |
| No new permissions in any `AndroidManifest` | PASS | `git diff 3e10d39 HEAD` is empty, so no manifest changed. The only app manifest is `04 Build/app/src/main/AndroidManifest.xml`. |
| No sliders (`SeekBar`, `Slider`, typed 0 to 100 boxes and toggles only) | PASS | No `SeekBar`, `RangeSlider`, `<Slider`, or `Slider(` under `04 Build/app/src`. The Step 2 workflow still rejects those names at `.github/workflows/build-debug-apk.yml:93-97`. |
| No `FLAG_SECURE` in the app module | PASS | No `FLAG_SECURE`, `setSecure(`, or `0x2000` under `04 Build/app/src/main`. |
| No weakened CI guard or workflow | PASS | `git diff --stat 3e10d39 HEAD -- .github/workflows` is empty. The GitHub pull-request file list is empty, so merging this head into `final-build` does not edit `.github/workflows/build-debug-apk.yml`. This head's own workflow file is the 2026-10-06 Step 2 workflow (triggers at lines 3-9, no `FLAG_SECURE` check). That is the file inherited from parent `3e10d39`. This commit did not delete a guard. |
| No secrets, keys, tokens, or `.gguf` files | PASS | The workflow secret pattern, run on `04 Build` and `.github` with `.gradle` and `build` skipped, reported 0 hits. `git ls-files` has no `.gguf`, `.pem`, `id_rsa`, `.keystore`, or `.jks`. |

These rule passes do not make the missing Broadcast Lock work a pass.

## Phone steps for Royalty (BLOCKED)

These need the Samsung Galaxy S25 Ultra on Android 15. This session has no phone, so each step is BLOCKED. They also cannot pass against this head: the debug APK built here is the 2026-10-06 Step 2 camera app (`versionName` 0.3). It has no Pairing screen, no broadcast lock, and no screen-recording callback.

1. Screen: GitHub pull request #46, the debug APK from a green "Build debug app" run. Action: confirm the run is green and download that APK. Expected: a run whose head contains `PairingActivity` and the new tests. Current expected result on this head: the workflow run `39001748265` is `action_required` and never built an APK. Do not install the local Step 2 APK as a stand-in.
2. Screen: Android Settings, Apps. Action: if an older OFFGRID is installed, uninstall it, then install the PR #46 debug APK and open it. Expected: the app opens on the camera screen and asks for the camera only.
3. Screen: OFFGRID camera screen, with a screen share already visible (Bigo sharing this phone) before the app is opened. Action: cold-start the app. Expected: no system camera-permission dialog while the share is visible. The stage stays locked. The dialog appears once after the share ends and the lock lifts, and it does not appear again on later lock notifications.
4. Screen: OFFGRID camera screen, permission not yet granted, lock clear (no share). Action: fresh install, or clear the app's storage, then open it. Expected: the camera permission dialog appears once. Denying it shows the help once, not a stream of help messages on every lock update.
5. Screen: OFFGRID camera screen, already locked by an active share. Action: leave the app and come back. Expected: the permission dialog stays hidden for the whole time the lock is on.
6. Screen: OFFGRID camera screen, unlocked. Action: press and hold two fingers in the center until the stage locks, and keep the fingers down. Expected: the rest of that gesture does not reach the panel or the stage.
7. Screen: OFFGRID camera screen, locked by the two-finger hold, share not visible. Action: press and hold two fingers in the center until it unlocks, and keep the fingers down. Expected: the rest of that gesture is swallowed the same way as after a lock. A finger still down does not drag the panel or draw.
8. Screen: Pairing (Make pairing key), key and Wi-Fi address visible as selectable text. Action: start a Bigo screen share while Pairing is the front activity. On a second phone, watch the Bigo view. Expected: the key and the address never appear for the viewer. Pairing hides that text and closes. The camera screen underneath is locked for the whole share.
9. Screen: OFFGRID camera screen after Pairing has closed itself. Action: stop the Bigo share. Expected: the lock releases through the normal unlock path. The panel and controls come back only when the lock is clear. The pairing key is not left on screen.
10. Screen: Phone 2, OFFGRID remote preview. Action: connect and look at every line on that phone's own screen during the share. Expected: the remote screen does not show the camera phone's pairing key. The share guard is not required on that activity.

## Verdict

QA Verdict: FAIL

PR #46 at `8877a4b` does not implement issue #45. The head is an empty commit on main from 2026-10-06, the cited sources are not in the tree, the unit-test task ran 0 tests, and the release-gate report and status-log line were not updated. The local JDK 17 / SDK 36 build assembled a Step 2 debug APK and did not fail a test, because there were no tests to run. Standing rules were held by that empty diff. The Samsung Galaxy S25 Ultra checks remain BLOCKED and are not a pass.
