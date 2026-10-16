# Phase 0 and Phase 1.1 progress, 2026-10-15

Branch: `phase_0_and_1_1` from `final-build`. Pull request: "Phase 0 guardrails + Phone 2 link + 1.1 render fix" into `final-build`. Not merged. `main` untouched.

Source of truth: the Chief Architect Review (PASS WITH CHANGES) overrides the council roadmap wherever they differ. This build covers the review's Section 5 items 0.1 to 0.5 and 1.1 only. No other Phase 1 item was started.

## What was done, item by item

### 0.1 CI permission allowlist: `DETECT_SCREEN_RECORDING` (Royalty approved)

- `.github/workflows/build-debug-apk.yml`: `DETECT_SCREEN_RECORDING` is on the allowed list with a comment that says who approved it, why (Broadcast Lock auto trigger, Android 15, normal protection level, no prompt), and that nothing else may be added without Royalty's approval.
- The manifest does not declare it yet. Phase 1.3 does. The allowlist is ready so that build does not fail CI.

### 0.2 CI slider guard: only `BioSliderView` (Royalty approved)

- The old guard banned every slider. The new guard still fails on `SeekBar`, `RangeSlider`, and `material.slider`, and now fails on any other class whose name ends in `Slider`, with `BioSliderView` as the single exception (word boundary, so the existing test name `...NoSliders` does not trip it).
- New minimal stub `04 Build/app/src/main/java/com/offgrid/compositor/terminal/bio_slider_view.kt`: a `View` with a 0 to 100 `value`, a hairline track, and a dot. It is not used anywhere yet. Phase 2.4 builds it out. The guard was checked by temporarily adding a banned class name (it failed) and removing it (it passed).

### 0.3 CI emoji guard, warn mode

- New workflow step "Check for OS emoji in the panel sources (warn mode)". It scans `04 Build/app/src/main` and `04 Build/live/src/main` (`.kt` and `.xml`), flags Unicode category So and U+1F000 to U+1FAFF, and keeps a small allowlist of typographic marks the panel already uses (menu dots, circles, block, minus, degrees).
- `EMOJI_GUARD_MODE: warn` prints per-file counts and GitHub warning annotations and never fails. Phase 2 flips it to `fail`. Today it reports 69 lines in 10 files, mostly the folder emoji in `EffectFolders.kt` and `PanelTree.kt`, matching the review's count.

### 0.4 Phone 2 link without the banned permission, plus a typed address (Royalty approved)

- `04 Build/live/src/main/java/com/offgrid/live/LocalNetwork.kt`: `RemoteServiceDiscovery.discover()` calls `NsdManager.discoverServices` directly. The `WifiManager.MulticastLock` is gone, so `CHANGE_WIFI_MULTICAST_STATE` is never needed. The failure text now tells the remote phone to type the address. `isSiteLocalAddress` stays (CI greps for it). `WifiDirectDiscovery` is untouched.
- New `04 Build/live/src/main/java/com/offgrid/live/manual_address.kt`: `ManualAddress.parse("192.168.1.5:9000")` accepts only a dotted IPv4 number on the private Wi-Fi (site-local, link-local, or loopback), fills in port 9000, never looks up a name, and refuses public addresses with a plain-words message. `ManualAddress.describe()` makes the line the camera phone shows.
- `RemotePreviewActivity`: a "Camera phone address" field and a "Connect to typed address" button under "Find camera phone". A bad address shows the reason in the status line. The connect path is the same `RemoteLink` as discovery.
- `PairingActivity`: shows "Camera phone address: 192.168.x.x:9000" (read off the main thread) under the key, or a line saying to join the same Wi-Fi when there is no private address yet.
- `LiveIntegration.startCameraRole()`: the ready toast includes the address and port. `EffectFolders.liveRemote()`: the Pairing and Remote preview help mention the typed address.
- `live/build.gradle.kts`: Robolectric 4.17 added for the live module's tests (with `isIncludeAndroidResources`).

### 0.5 Render probe, debug only

- New `04 Build/app/src/main/java/com/offgrid/compositor/render_probe.kt`: `RenderProbe`, enabled only when the app is debuggable (`ApplicationInfo.FLAG_DEBUGGABLE`), logcat tag `OffgridProbe`. It logs the PreviewView size and view port at each bind, the SurfaceRequest resolution, every output's size and targets (and whether it is the PREVIEW output), the stage size when it changes, the AR clip's `VideoSize`, and for the first 3 frames after each bind the output and stage sizes plus the PreviewView's `TextureView` transform matrix (scaleX, scaleY, skew, translation) sampled on the main thread.
- Wired into `CompositorProcessor` (input, outputs, stage, preview frames), `MainActivity` (bind, TextureView sample), and `ar_deck_player.kt` (`onVideoSizeChanged`, on the drawing-thread looper via `setLooper`). Release builds get a no-op object; the probe never draws.

### 1.1 Render and stretch fix

- New `04 Build/app/src/main/java/com/offgrid/compositor/effects/stage_geometry.kt`: the shared, pure-Kotlin geometry (`StageGeometry`): `pickMain` (PREVIEW output is the master; biggest only when there is no PREVIEW output), `stageSize` (master shape under the long-side cap), `fillCrop` and `fitRect` (uniform scale by construction), `cropMatrix`, `aspectDiffers` (1 percent band), and `RebindGate` (one rebind per real shape change). This is the "Seam Weld idea" for geometry: preview, video, replay, live outputs, and the AR clip are all cut by the same rule.
- `CompositorProcessor`: the PREVIEW output is the master for the stage size, the picture size, and the recorders, in both arrival orders. `OutputSlot.isPreview` added.
- AR deck: `ArDeckPlayer` reports `onVideoSizeChanged`; `ArDeckSurface` keeps the real clip size (width times pixel ratio, height); the clip is not drawn until both a frame and the size are known; the size is forgotten when a new clip loads. `StagePainter.drawAr` draws the clip with fill (centered crop multiplied into the texture matrix, default) or fit (centered box with the clip's shape). New `FxKeys.AR_FIT` with panel choice "Clip shape" in AR Video Deck (Fill the stage, Show the whole clip). `StagePlan.arFit` follows it.
- `StagePainter.present()` uses the same `fillCrop` rule as before, now from the helper.
- `OverlayRenderer`: a drawing made for an older stage size is skipped at `latch()` instead of being stretched over the new stage for one frame.
- `MainActivity` and `camera_bind.kt`: `startCamera()` waits for a laid-out view; `bindCamera()` records the bound view size; a `PreviewView` layout change whose shape differs by more than 1 percent schedules exactly one rebind, debounced 150 ms, only while resumed with the camera on and permitted. `faces.outputAspect` is refreshed by the rebind as before.

## Files changed

Workflow: `.github/workflows/build-debug-apk.yml`.

App module (`04 Build/app/src/main/java/com/offgrid/compositor/`): `CompositorProcessor.kt`, `LiveIntegration.kt`, `MainActivity.kt`, `OverlayRenderer.kt`, `StagePainter.kt`, `ar_deck_player.kt`, `ar_deck_surface.kt`, `camera_bind.kt`, `effects/FxKeys.kt`, `effects/StagePlan.kt`, `terminal/EffectFolders.kt`; new `effects/stage_geometry.kt`, `render_probe.kt`, `terminal/bio_slider_view.kt`.

App tests (`04 Build/app/src/test/java/com/offgrid/compositor/`): `ar_deck_test.kt` (extended); new `effects/stage_geometry_test.kt`, `render_probe_test.kt`.

Live module (`04 Build/live/`): `build.gradle.kts`, `src/main/java/com/offgrid/live/LocalNetwork.kt`, `PairingActivity.kt`, `RemotePreviewActivity.kt`; new `manual_address.kt`; new test `src/test/java/com/offgrid/live/local_network_test.kt`.

No file was deleted. No theme, color, or asset changed. No secret added.

## Tests

- `StageGeometryTest` (12 tests): PREVIEW wins in both arrival orders and on an equal-area tie; the stage keeps the preview shape (within 0.5 percent) under the 1920 cap; a 1920x1080 clip on a 1080x2340 stage gives fill U about 0.26 centered with V = 1, and fit full width about 608 px tall centered; `sx == sy` for every fit and fill; `cropMatrix` maps the texture onto the crop; `aspectDiffers` uses a 1 percent band; a layout change from 1440x2900 to 1440x3120 rebinds exactly once and repeats do not.
- `RenderProbeTest` (5 tests): exactly three frame lines per bind, the main-thread TextureView sample is carried with the bind and frame number, the stage line repeats only on a size change, every number from the review is logged, a release build logs nothing.
- `LocalNetworkTest` (8 tests, Robolectric SDK 35): discovery starts with one NSD listener and no failure callback while the merged test manifest has no `CHANGE_WIFI_MULTICAST_STATE` or `ACCESS_WIFI_STATE`; the discovery source has no `MulticastLock` or `WifiManager`; `ManualAddress` accepts a private address with and without a port, refuses public addresses, names, garbage, and bad ports, and never resolves a name. The class runs with `@ConscryptMode(OFF)` so Robolectric does not swap the JVM's JCA provider under the X25519 handshake tests in the same module.
- `ArDeckTest`: the new "Clip shape" choice defaults to fill and the plan follows fit.
- Local run from `04 Build`: `./gradlew testDebugUnitTest assembleDebug lintDebug`: 252 unit tests, 0 failures. The CI guard steps (secrets, mic path, voice, sliders, emoji warn, site-local) were run locally with the same scripts and pass.

## Known limits

- Phone 2 discovery without the multicast lock is UNVERIFIED on Samsung's mDNS stack (the review says so too). The typed address makes it a convenience issue, not a blocker.
- The roadmap's `BindTimingTest` is covered at the `RebindGate` level (pure Kotlin). `MainActivity` cannot be built under Robolectric in this repo (CameraX, MediaPipe, GL), so the 150 ms debounce and the resumed/permitted guard are code-reviewed, not unit tested.
- The PreviewView `TextureView` transform check (review §3.2 suspect 1) is observed by the probe, not changed. If the device log shows a non-uniform scale in the first frames, the next step is `PreviewView.ImplementationMode.PERFORMANCE` once Broadcast Lock lands (review order).
- The Seam Weld GL loop crossfade (review §4.1) is Phase 3.4 and was not started. Only the geometry "seam weld" (one shared rule) is in this build.
- `BioSliderView` is a stub. It draws a value and takes none from touch. Phase 2.4 builds the real control.
- `DETECT_SCREEN_RECORDING` is allowed by CI but not declared or used yet (Phase 1.3).
- The emoji guard warns only. The 69 flagged lines are left as they are until the Phase 2 panel work.

## How Royalty phone-tests this build

Install the debug app from the CI run (Build debug app artifact) on the S25 Ultra.

1. Stretch check, cold start: force stop OFFGRID, open it, allow the camera if asked. The stage must not look wide or stretched on the first frames. Repeat 5 times. Then switch to another app and back: still normal.
2. Probe log (debug build only): with the phone on USB, run `adb logcat -s OffgridProbe` and open OFFGRID. Send me the lines for one cold start and one resume. They look like `bind 1 preview=1440x3120 viewPort=6:13 rotation=0`, `surfaceRequest=1280x720`, `output=...x... targets=... preview=true`, `main=... previewMaster=true stage=...`, and three `frame n/3 ... scaleX=... scaleY=...` lines. Equal scaleX and scaleY means no stretch in the view.
3. AR Video Deck: pick a landscape clip (16:9). Play. With "Clip shape: Fill the stage" the clip covers the stage with its sides cut, nothing squashed. Switch to "Show the whole clip": the whole clip sits in a centered band with the selfie above and below. Pick a portrait clip and repeat. Pause, then Play: the clip is never shown stretched, even for a moment.
4. Phone 2 link, discovery: on the S25, Live and Remote, Remote role: Camera phone. On Phone 2 (same Wi-Fi), Remote role: Remote phone, Remote preview, "Find camera phone". Report whether it connects and how long it takes.
5. Phone 2 link, typed address: on the S25 open Pairing and read the "Camera phone address" line (like 192.168.1.5:9000). On Phone 2 type it into "Camera phone address" and tap "Connect to typed address". The preview should appear. Also try a wrong address (8.8.8.8) and confirm it is refused with a message.
6. Rebind on shape change (optional): open OFFGRID in split screen, then back to full screen. The picture should come back undistorted after a short pause.
