# Lane C QA

**Lane C QA: FAIL**

Reviewed head: `4e9254e` (`copilot/lane-c-live-remote`), base `final-build` at `9ec834c`. Checked 2026-10-09 14:19 UTC. No Round 2 code commit is present on this head; the posted fix list still needs implementation.

**CI:** No build on head `4e9254e`. The older run [37933713124](https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/37933713124) for `d5b2852` was cancelled while Gradle was building. Local `assembleDebug testDebugUnitTest lintDebug` is **BLOCKED**: Gradle cannot resolve `com.android.application:8.13.2` from Google Maven, Maven Central, or the Gradle Plugin Portal. The `final-build` base run [37901783024](https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/37901783024) succeeded, but it does not validate this PR.

## Quality Bar

1. **FAIL:** `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/build.gradle.kts` applies AGP 8.13.2 independently although the root already declares the application plugin at that version; the module must use the root-declared library plugin. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/build.gradle.kts:1-4` and `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/build.gradle.kts:1-4`.
2. **BLOCKED:** no completed current-head build/lint validates unused code/resources; several module classes have no application caller, including `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/PairingWorkflow.kt:12-59`.
3. **FAIL:** role, pairing, preview, and action contracts are not reachable from the UI; `REMOTE_OPEN_PAIRING` is not handled. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:230-245`.
4. **BLOCKED:** no overlay/preview frame-time evidence or device profiling is supplied; preview render code is at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/RemotePreviewEncoder.kt:13-151`.
5. **BLOCKED:** no emulator evidence verifies the required 8 dp layout or controls; overlay drawing is at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/OverlayPanelView.kt:58-213`.
6. **BLOCKED:** no emulator evidence verifies eased animations or Remove animations behavior; overlay drawing/input is in `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/OverlayPanelView.kt:58-213`.
7. **FAIL:** crypto/security unit tests exist at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/test/java/com/offgrid/live/RemoteSecurityTest.kt:14-169`, but app role/action wiring required to exercise pairing and preview is missing.
8. **BLOCKED:** the guide exists at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/docs/10 Copilot Plan/Bigo Live Guide.md:1-73`, but no overlay, bubble, pairing, or remote screenshots demonstrate the UI.
9. **BLOCKED:** device tests for service lifecycle, permission denial, and safe reconnect are absent; service behavior is at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/LiveService.kt:20-40`.

## Lane C Requirements

- **C1 BLOCKED:** overlay window uses `FLAG_SECURE` and `FLAG_NOT_TOUCH_MODAL` at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/OverlayWindowController.kt:126-134`, but no emulator evidence shows move/resize/bubble/close, outside touch pass-through, or state persistence.
- **C2 FAIL:** the service starts with only `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` (`/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/LiveService.kt:29-40`). Add camera/microphone FGS types only while visible recording/streaming; review `START_STICKY` so it cannot restart without an activity/host. Android 14-16 service tests are absent.
- **C3 BLOCKED:** clean-output code hides the app control bar and panel at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:237-243`, but no screen-share/emulator evidence verifies the output and restoration flow.
- **C4 BLOCKED:** the guide is present at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/docs/10 Copilot Plan/Bigo Live Guide.md:1-73`, but no device checklist or evidence validates its tap-by-tap route.
- **C5 FAIL:** the role model exists at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/RemoteRole.kt:6-23`, but the app does not connect roles to service/server/preview flows. Add UI routing; current action routing is only `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:230-245`.
- **C6 FAIL:** `PairingWorkflow` is implemented at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/PairingWorkflow.kt:12-59` but no application file instantiates it; the pairing action is unhandled at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt:230-245`. Wire make/paste/forget/rotate into a pairing screen and test panel access.
- **C7 FAIL:** crypto and local-network tests exist at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/test/java/com/offgrid/live/RemoteSecurityTest.kt:14-169`, but the app cannot reach pairing or establish the role-driven session. Wire the session lifecycle and test the complete app path.
- **C8 FAIL:** remote preview encoder/decoder and link classes exist but are unreachable from the app. `LiveHost` only exposes a `FrameSink` to `setLiveSurface` (`/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/LiveHost.kt:50-57`); no role/action launches the preview or remote control path.
- **C9 FAIL:** Lane C added 139 lines to `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt`, `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/CompositorProcessor.kt`, and `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/EffectsController.kt`, above the under-40-line limit; the B/C merge simulation conflicts in the first two files. Replace the copied draw path with Lane B’s extra-output API, move logic to Lane C-owned files, and merge `final-build` normally after Lane B's changes land.

## Missed Round 1 Findings / Round 2 Scope

- **AGP:** module plugin version duplication is a build blocker; declare `com.android.library` with `apply false` in the root and omit its version in the module.
- **Reachability:** `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/CameraRemoteServer.kt`, `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/RemoteLink.kt`, `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/RemotePreviewEncoder.kt`, `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/RemotePreviewDecoder.kt`, and `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/java/com/offgrid/live/PairingWorkflow.kt` have declarations but no application call sites. Add the requested panel actions and routing; current `MainActivity.onLiveAction` handles only overlay settings and clean output at lines 230-245.
- **Manifest:** `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/AndroidManifest.xml:9-10` duplicates CAMERA and RECORD_AUDIO already declared by Lane B. Remove the duplicate RECORD_AUDIO declaration without removing Lane B's permission.
- **Round 2 scope:** the latest fix list also asks for Send to PC/OBS output and additional panel rows/actions; none is implemented on reviewed head `4e9254e`. Add those changes only within the newly authorized ownership limits.

## Rules

- **Permissions BLOCKED:** the Lane C manifest declares the intended overlay/network allowlist, but duplicates CAMERA and RECORD_AUDIO; the merged-manifest allowlist has not run on this head. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/live/src/main/AndroidManifest.xml:3-17`.
- **No TTS PASS; no sliders PASS:** the older CI run passed these guards before its build was cancelled; source greps also returned no matches.
- **No WebView PASS; no Marvel names PASS:** source greps returned no matches.
- **Panel/overlay not in recordings PASS:** `FLAG_SECURE` protects the overlay; no code sends the overlay UI to the remote frame surface. Verify on device before using screen share.
- **No secrets PASS:** no key material literals were found; pairing keys are implemented in the Keystore-backed path. The requested log-capture test is still needed.
- **No unapproved deletions PASS:** no deletions of unrelated files identified in the Lane C diff.
- **Start look BLOCKED:** no target-device screenshot verifies the OLED/terminal appearance.
- **Platform safety BLOCKED:** no emulator/device evidence verifies visibility, camera/mic FGS timing, flash limit, or app-switch safety.

## Files Outside Lane C Ownership

`/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/MainActivity.kt`, `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/CompositorProcessor.kt`, and `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/EffectsController.kt` are Lane B-owned integration files; the permitted integration-hook exception is limited to fewer than 40 lines total, but the additions total 139 lines. `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/LiveHost.kt` is the app-side adapter requested by C9. The status log is shared and append-only.

## Needs Royalty's Phone

Verify overlay permission prompts, secure screenshots, move/resize/bubble/close, app switching and service lifecycle on Android 14-16; verify the local two-phone pairing, role switching, remote controls and preview latency; and test the Send to PC/OBS path when implemented. Keep verdict FAIL until Gradle, reachability, hook-size/conflict, permission, and acceptance-test failures are fixed.
