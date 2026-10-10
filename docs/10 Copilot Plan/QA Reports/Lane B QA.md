# Lane B QA

**Lane B QA: FAIL**

Reviewed head: `f0a014b` (`copilot/lane-b-camera-mic`), base `final-build` at `9ec834c`. Checked 2026-10-09 14:19 UTC. Follow-up PRs #17 and #19 are WIP shells at `247ab8b`; no Round 2 fix commit has landed on this lane head.

**CI:** [Current-head run 37934992092](https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/37934992092) for `f0a014b` was **cancelled** while Gradle was building. Secret, voice, and slider guards passed before cancellation. Local `assembleDebug testDebugUnitTest lintDebug` is **BLOCKED**: Gradle cannot resolve `com.android.application:8.13.2` from Google Maven, Maven Central, or the Gradle Plugin Portal.

## Quality Bar

1. **FAIL:** `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:1-948` exceeds the Round 2 limit of 400 lines. Split permission, capture, recording, and status responsibilities.
2. **FAIL:** the permission allowlist is guaranteed to reject the generated AndroidX permission and does not test its deliberate-failure case. See B0 below.
3. **FAIL:** `MicEngine` can leave `AudioRecord` unreleased after a read error. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/audio/MicEngine.kt:84-103`.
4. **BLOCKED:** frame-time profiling and recorded lip-sync/device checks are absent; the changed compositor path is at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/CompositorProcessor.kt:1-731`.
5. **BLOCKED:** no 8 dp/rendered layout evidence for changed UI integration is supplied; capture controls are at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:751-779`.
6. **BLOCKED:** no device evidence verifies recording transitions under Remove animations; recording controls are in `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:300-400`.
7. **FAIL:** DSP/state tests exist, but no tests cover mic-read error release or denied-permission toggle-off. Existing capture tests cover normal start/release and missing permission: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/test/java/com/offgrid/compositor/audio/CaptureSwitchTest.kt:11-64`.
8. **BLOCKED:** no recorded sample or device screenshots verify audio/status copy; recorder audio setup is at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/audio/AvRecorder.kt:1-244`.
9. **BLOCKED:** no device evidence verifies encoder/permission recovery; mic permission handling is at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:139-145`.

## Lane B Requirements

- **B0 FAIL:** `final-build` is in the PR trigger at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/.github/workflows/build-debug-apk.yml:10-16`, and the voice/slider guards were updated. However, the allowlist only accepts `android.permission.*` names at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/.github/workflows/build-debug-apk.yml:115-140`; it will reject `com.offgrid.compositor.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` from androidx.core 1.16. The manifest also no longer removes `ACCESS_NETWORK_STATE` (see B1), which CameraX contributes. Small fix: restore the manifest removal and explicitly allow the exact generated permission if it is required; run and link the deliberate extra-permission failure test.
- **B1 FAIL:** `ACCESS_NETWORK_STATE` removal and `xmlns:tools` were deleted; the manifest currently declares only CAMERA and RECORD_AUDIO at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/AndroidManifest.xml:1-5`. Restore `xmlns:tools` and `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />`. On mic denial, the callback only sets an error state at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:139-145`; also store mic-off and call `terminalPanel.setToggleSetting(MicKeys.MIC_ON, false)` after Lane A restores that API. The record-bar Settings guidance still needs device verification.
- **B2 BLOCKED:** state-machine code and tests exist at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/audio/CaptureSwitch.kt:23-49` and `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/test/java/com/offgrid/compositor/audio/CaptureSwitchTest.kt:17-52`, but no device/log evidence confirms hardware release/rebind or a recording spanning camera-off and mic-off. The CI run did not finish.
- **B3 FAIL:** standalone video recording with processed audio is implemented, but replay is still picture-only: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/ReplayBuffer.kt:13-21`. Add replay audio when the mic is on and test the muxed output. Also mix the radar ping. **Mic leak:** when `read <= 0`, `MicEngine.listen` sets `running = false` and returns (`/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/audio/MicEngine.kt:95-109`); then `stop()` returns before release (`/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/audio/MicEngine.kt:84-92`). Make release idempotent on both paths and add a test asserting exactly one release.
- **B4 FAIL:** `BeatDetector.level` is public (`/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/audio/BeatDetector.kt:9-12`), but the public panel-glow hook and wiring are missing. Lane A must expose a hook; Lane B/C integration must connect it. Video-border beat behavior does not meet the separate panel-glow request.
- **B5 BLOCKED:** the fixed-size sample arena and tests exist at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/SampleArena.kt:1-66` and `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/test/java/com/offgrid/compositor/SampleArenaTest.kt:1-68`, but profiling and device checks are missing. `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:1-948` still exceeds the architecture limit.

## Rules

- **Permissions FAIL:** ACCESS_NETWORK_STATE is not removed and the allowlist rejects the AndroidX-generated permission. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/AndroidManifest.xml:1-5`; `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/.github/workflows/build-debug-apk.yml:130-140`.
- **No TTS PASS; no sliders PASS:** the current-head CI guard steps passed before the build was cancelled.
- **No WebView PASS; no Marvel names PASS:** source greps returned no matches.
- **Panel not in recordings PASS:** the replay path explicitly describes the picture-only compositor output, never the panel: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/ReplayBuffer.kt:14-20`.
- **No secrets PASS:** the current-head CI secret scan passed before build cancellation; no saved secrets were reported.
- **No unapproved deletions PASS:** no unrelated file deletions identified in the Lane B diff.
- **Start look BLOCKED:** no target-device screenshot evidence.
- **Platform safety BLOCKED:** face coverage, flash rate, and recording behavior need target-device verification.

## Files Outside Lane B Ownership

None identified in the Lane B PR diff; the append-only status log is shared. Requests involving `terminal/` and `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/effects/FxKeys.kt` remain Lane A-owned.

## Needs Royalty's Phone

Check the mic permission denial/recovery flow, camera/mic privacy indicators, camera-off and mic-off segments in a saved clip, a 20-second Robot plus Echo sample and lip sync, fallback behavior, and frame times. Keep verdict FAIL until the manifest, allowlist, mic release, denial toggle, replay audio, tests, and MainActivity split are fixed.
