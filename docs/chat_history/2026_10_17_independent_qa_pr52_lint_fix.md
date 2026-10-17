# Independent QA: PR #52 lint fix, 2026-10-17

Reviewer: independent QA. This note is docs only. Product code was left unchanged. Nothing was merged. `final-build` and `main` were not pushed. No release was published.

Subject: draft [PR #52](https://github.com/stillwaters23/OFFGRID-Compositor/pull/52), branch `cursor/ci_build_broadcast_lock-194e` into `final-build`, head `aff33d0836ac2dcd410f71b37b99300d997a1c4f`. That commit adds `@SuppressLint("MissingPermission")` on `registerShareGuard()` and `unregisterShareGuard()` in `04 Build/live/src/main/java/com/offgrid/live/PairingActivity.kt`. [HR gate PR #51](https://github.com/stillwaters23/OFFGRID-Compositor/pull/51) required product code to stay unchanged, so this one product change was checked on its own.

## Verdict

**PASS.** All six checks pass.

Recommended merge path for Royalty: merge draft PR #52 into `final-build`. That fast-forward is draft PR #44, then draft PR #49, plus `aff33d0`. Leave PR #49 unmerged on its own: its tree fails live-module lint. Leave `main` untouched. A push to `final-build` publishes the `step-2-preview` prerelease through `.github/workflows/build-debug-apk.yml`, so that merge is the preview release. The Samsung Galaxy S25 Ultra steps in the HR gate were outside this review.

## Results

| # | Check | Result |
| --- | --- | --- |
| 1 | `aff33d0` changes only the two annotations and the comments, with no behavior change | **PASS** |
| 2 | The merged debug app manifest declares `android.permission.DETECT_SCREEN_RECORDING`, and the live module manifest does not | **PASS** |
| 3 | The suppression hides no other real lint issue in those two functions | **PASS** |
| 4 | PR #52 is the #49 head `c677b87` plus `aff33d0` only, so merging #52 equals merging #44 then #49 with the fix. #49 without the fix fails lint on `final-build` | **PASS** |
| 5 | `./gradlew testDebugUnitTest lintDebug assembleDebug` in `04 Build` | **PASS** |
| 6 | APK artifact of run 39072651171 | **PASS** |

## 1. What `aff33d0` changes

**PASS.**

`git show aff33d0` touches one file: `04 Build/live/src/main/java/com/offgrid/live/PairingActivity.kt` (7 insertions, 3 deletions). The parent is `c677b87d653d7f594717fccd8c2c249ad2a48554`.

The diff is:

- `import android.annotation.SuppressLint`
- a KDoc rewrite on `registerShareGuard()` (the same sentences, plus two sentences that the live manifest must not redeclare the permission and that lint only sees this module)
- `@SuppressLint("MissingPermission")` on `registerShareGuard()`
- `@SuppressLint("MissingPermission")` on `unregisterShareGuard()`

Stripping the import and the two annotations leaves only the KDoc text. The Kotlin of both function bodies is unchanged.

Bytecode check, debug classes from the two trees (`javap -c`, constant-pool indexes normalized):

| Method | Instructions | Match |
| --- | --- | --- |
| `registerShareGuard()` | 46 | identical |
| `unregisterShareGuard()` | 42 | identical |

The pre-fix class file has no `SuppressLint`. The post-fix class file has it. Android's `android.annotation.SuppressLint` is `@Retention(RetentionPolicy.CLASS)` (`platforms/android-36/android-stubs-src.jar`). The runtime permission check does not read it. `SecurityException` is still caught in `registerShareGuard()`, and `removeScreenRecordingCallback` is still inside `runCatching`.

## 2. Where the permission is declared

**PASS.**

The merged debug app manifest declares it. The live module manifest does not.

App source (`04 Build/app/src/main/AndroidManifest.xml:24`), which the merger copies in with its comment:

```xml
    <!-- Phase 1.3 Broadcast Lock (Royalty approved 2026-10-15). Normal protection level, no prompt.
         Android 15 tells OFFGRID when its screen is inside a recording or a share, so every bit of
         chrome hides at once and only the stage is on air. It never reads the recording. -->
    <uses-permission android:name="android.permission.DETECT_SCREEN_RECORDING" />
```

Merged debug manifest after `assembleDebug`, at `04 Build/app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml:26`. The same comment sits above the element, so this entry came from the app module:

```xml
    <!--
         Phase 1.3 Broadcast Lock (Royalty approved 2026-10-15). Normal protection level, no prompt.
         Android 15 tells OFFGRID when its screen is inside a recording or a share, so every bit of
         chrome hides at once and only the stage is on air. It never reads the recording.
    -->
    <uses-permission android:name="android.permission.DETECT_SCREEN_RECORDING" />
```

The package copy at `04 Build/app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml:26` is the same element. `aapt dump permissions` on `04 Build/app/build/outputs/apk/debug/app-debug.apk` lists `android.permission.DETECT_SCREEN_RECORDING` with the other app permissions.

CI run [39072651171](https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/39072651171) printed the same name from the merged manifest it found under `04 Build/app/build`, and the step "Check the built app only asks for allowed permissions" succeeded with "DETECT_SCREEN_RECORDING is declared for the Broadcast Lock."

Live source permissions (`04 Build/live/src/main/AndroidManifest.xml:3-10`) are `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `FOREGROUND_SERVICE_CAMERA`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`, `CAMERA`, and `INTERNET`:

```xml
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERNET" />
```

The live module's own merged debug manifest (`04 Build/live/build/intermediates/merged_manifest/debug/processDebugManifest/AndroidManifest.xml:7-14`) repeats that list and has no `DETECT_SCREEN_RECORDING` entry:

```xml
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERNET" />
```

`PairingSharePolicyTest.theGuardIsRegisteredOnlyByPairing` also asserts that the live source manifest text has no `DETECT_SCREEN_RECORDING`. That test is about the source file. The merged files above are the build output.

## 3. What the suppression covers

**PASS.** The annotation names one lint id, `MissingPermission`. The only findings of that id in the live module are the two callback calls, and both require only `android.permission.DETECT_SCREEN_RECORDING`.

Platform annotations in `platforms/android-36/data/annotations.zip` (`android/view/annotations.xml`):

- `WindowManager.addScreenRecordingCallback` has `@RequiresPermission("android.permission.DETECT_SCREEN_RECORDING")`
- `WindowManager.removeScreenRecordingCallback` has the same single permission

`DETECT_SCREEN_RECORDING` appears twice in that file, once on each method.

Calls inside the two functions:

- `registerShareGuard()`: SDK version check, `unregisterShareGuard()`, `windowManager`, a `Consumer`, `ContextCompat.getMainExecutor`, `addScreenRecordingCallback`, `PairingSharePolicy.shouldClose`, `closeForShare()`, `Log.w` in the `SecurityException` catch.
- `unregisterShareGuard()`: clear `shareGuard`, SDK version check, `removeScreenRecordingCallback` inside `runCatching`.

The permission-bearing calls are the two callback methods. `closeForShare()` is a separate method, so its findings stay on that method.

Pre-fix tree `c677b87`, `:live:lintDebug` text report, two errors and no others:

```text
PairingActivity.kt:122: Error: Missing permissions required by WindowManager.addScreenRecordingCallback: android.permission.DETECT_SCREEN_RECORDING [MissingPermission]
PairingActivity.kt:134: Error: Missing permissions required by WindowManager.removeScreenRecordingCallback: android.permission.DETECT_SCREEN_RECORDING [MissingPermission]
```

Those lines are the two call sites before the annotations were inserted. CI run [39072057401](https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/39072057401) on the same SHA reported "Lint found 2 errors and 30 warnings" and printed the line 122 error as the first failure. The console prints only the first failure. The text report has both.

Post-fix `lintDebug` on `aff33d0`:

| Module | Result |
| --- | --- |
| `:live` | 0 errors, 30 warnings |
| `:app` | 0 errors, 140 warnings |

Every `PairingActivity.kt` warning in the live report is `SetTextI18n`, on lines 70, 80, 81, 89, 166, 180, and 181. `registerShareGuard()` is lines 114–131 and `unregisterShareGuard()` is lines 134–140. No issue is reported inside either function. The `SetTextI18n` warnings in the same file still report, so the annotation is limited to `MissingPermission` on those two methods.

The offline pre-fix rerun footer said 29 warnings. The online post-fix report has 30. The extra online warning is `GradleDependency` on `live/build.gradle.kts:35` (`androidx.core:core-ktx` 1.16.0 versus 1.19.1). It is outside these functions. CI's "30 warnings" on `c677b87` matches the online count.

## 4. Tree equality with #44 and #49

**PASS.**

Recorded heads, confirmed against the open PRs and the fetched branch tips:

| PR | Branch | Head | Tree |
| --- | --- | --- | --- |
| #44 | `copilot/hr-release-gate-broadcast-lock` | `29a4e23c513a36a92fd1bbb2f331c4d0c35ed4c9` | `0ca57a2e5e1b9f49ebb46b3617569bf7f03bb035` |
| #49 | `cursor/issue45-broadcast-lock-d6d6` | `c677b87d653d7f594717fccd8c2c249ad2a48554` | `20593ff33645ed3a7b14ccd65628bfb0412110f1` |
| #52 | `cursor/ci_build_broadcast_lock-194e` | `aff33d0836ac2dcd410f71b37b99300d997a1c4f` | `d221ae183d9afc349ad0c077a500f385f7de2049` |

`origin/final-build` is `541588cb9208e06a9a3292a91af70446abfbd05d`. It is an ancestor of all three heads. `29a4e23` is an ancestor of `c677b87`. `aff33d0`'s only parent is `c677b87`. `git diff --name-status c677b87 aff33d0` is the one `PairingActivity.kt` modification. `git log aff33d0..origin/copilot/hr-release-gate-broadcast-lock` and `git log aff33d0..origin/cursor/issue45-broadcast-lock-d6d6` are empty: neither PR branch has a commit outside #52.

`git merge-tree --write-tree` (no commit, no worktree change):

| Merge | Result tree | Equals |
| --- | --- | --- |
| `final-build` + #44 | `0ca57a2e5e1b9f49ebb46b3617569bf7f03bb035` | #44 |
| #44 + #49 | `20593ff33645ed3a7b14ccd65628bfb0412110f1` | #49 |
| #49 + #52 | `d221ae183d9afc349ad0c077a500f385f7de2049` | #52 |
| `final-build` + #52 | `d221ae183d9afc349ad0c077a500f385f7de2049` | #52 |

Each merge is a fast-forward. A `--no-ff` merge commit would still carry the descendant tree. Merging #52 into `final-build` produces the same tree as merging #44, then #49, then `aff33d0`.

#49 without `aff33d0` fails lint on `final-build`. Run [39072057401](https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/39072057401) is the "Build debug app" pull request run of this branch at `c677b87` (the #49 tree) into `final-build`. Conclusion: failure. The failed step is "Run Android lint" (`:live:lintDebug`). PR #49's own base is `copilot/hr-release-gate-broadcast-lock`, which is outside the workflow's `pull_request` branch list, so that PR does not start this workflow. The failure is the #49 tree when it is built as a pull request into `final-build`. The local text report on that tree is the two `MissingPermission` errors in section 3.

The green run on the fixed tree is [39072651171](https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/39072651171): `headSha` `aff33d0836ac2dcd410f71b37b99300d997a1c4f`, event `pull_request`, conclusion success. "Run Android lint" succeeded. "Attach the app file to the preview release" was skipped, which is correct for a pull request.

## 5. Local Gradle run

**PASS.**

Command, in `04 Build`:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug
```

`BUILD SUCCESSFUL in 2m 41s`. 115 actionable tasks, 115 executed. JDK `21.0.12.1` (this VM). CI used Temurin `17.0.20-1`. Source and target stay JVM 17. `compileSdk` 36, platform `android-36`, AGP 8.13.2.

XML totals from `*/build/test-results/testDebugUnitTest/TEST-*.xml`:

| Module | Tests | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- |
| `:app` | 265 | 0 | 0 | 0 |
| `:live` | 45 | 0 | 0 | 0 |
| Total | **310** | **0** | **0** | **0** |

Lint text-report footers: live `0 errors, 30 warnings`; app `0 errors, 140 warnings`. `assembleDebug` wrote `04 Build/app/build/outputs/apk/debug/app-debug.apk`.

## 6. APK artifact of run 39072651171

**PASS.**

The Actions artifact is named **`OFFGRID Preview`**. The file inside it is **`OFFGRID Preview.apk`**.

- Artifact id `12116135603`
- Zip size 30,698,384 bytes
- Upload path `dist/OFFGRID Preview.apk`, copied from `04 Build/app/build/outputs/apk/debug/app-debug.apk` by the "Name the app file" step
- Workflow: `.github/workflows/build-debug-apk.yml` (`name: OFFGRID Preview`, `path: dist/OFFGRID Preview.apk`)

## Merge path

Merge draft PR #52 into `final-build`. That is one fast-forward to `aff33d0`: PR #44, then PR #49, plus the lint-only fix. Merging PR #49 into `final-build` without `aff33d0` fails `:live:lintDebug`. Leave `main` alone. The workflow publishes `step-2-preview` on a push to `final-build`, so Royalty should treat that merge as the preview release. This review did not run the phone steps from PR #51.
