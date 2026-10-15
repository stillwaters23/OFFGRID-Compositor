# OFFGRID-Compositor: Chief Architect Review of the Premium Architecture Roadmap

> **Reviewed document:** `docs/PREMIUM_ARCHITECTURE_ROADMAP.md` (the council's PROPOSAL)
> **Code read:** `final-build` @ `be674e1` (every claim below was checked against the files named; line references are to that commit)
> **Target:** Samsung Galaxy S25 Ultra, Android 15 (One UI 7; the phone may already be on One UI 8 / Android 16 by now, which changes nothing below), no root, broadcast through Bigo Live screen share, Phone 2 over local Wi-Fi
> **Scope of this review:** documentation only. No app code was changed. Nothing here is approved until Royalty says go.
> **Rules honored:** no files deleted, underscores in new names, no steel/metal themes, no OS emoji in any UI proposal, the black/white System themes stay, `main` untouched.

---

## 0. How to read this in two minutes

- **Verdict: PASS WITH CHANGES.** The roadmap's priorities are right (protect the stream, then beauty, then Phone 2 mastery), its file and key names are almost all accurate, and the Broadcast Lock mechanism is real Android 15 API. It must not be handed to coding agents as-is, because (a) its render-bug diagnosis ranks a non-cause first and misses two real suspects, (b) two acceptance criteria contradict tests that already exist, (c) one Phase 1 item is impossible on the current GL context as written, and (d) the lock design leaves an on-air exposure window on every resume, not only at cold start.
- **The five flags that matter most:** §2.10 (Phone 2 cannot connect on a CI-green build today), §2.1 (resume exposure window in Broadcast Lock), §2.2 (screenshot PBO on an ES 2.0 context), §2.3 (lock gestures that reveal system bars), §2.4 (thermal and storage arithmetic for five-hour recording). §2.5 (Bigo content policy) is the one non-technical risk Royalty must accept knowingly.
- **The five factual corrections that matter most:** §3.1 (output selection cannot stretch), §3.2 (the stage is ~590×1280, not 1920-limited), §3.3 (the header still contains hidden reels and the existing test asserts it), §3.4 (the floating overlay already carries `FLAG_SECURE`), §3.5 ("What Bigo sees" cannot be built as named).
- **The five ideas I would add** (§4): Seam Weld, Invisible Hotkeys, Armed State plus Lock Drill, Geometry Attestation, Stage Monitor with a Chrome Flag.
- **Build order** (§5): render probe first, then the deck fit/fill fix, then Broadcast Lock with the resume gate, then the clean surface, then unlimited recording. Lumen and the IA move come after the stream is safe.

---

## 1. Verdict: PASS WITH CHANGES

### 1.1 What passes

1. **Priority order.** Phase 1 "protect the stream" before Phase 2 "Lumen" is the correct call. Every pixel on the S25 is on air during a Bigo share; nothing else matters until that is handled.
2. **Platform facts.** `WindowManager.addScreenRecordingCallback(Executor, Consumer<Integer>)` exists on API 35, returns the initial state, and needs `android.permission.DETECT_SCREEN_RECORDING` (protection level normal, no prompt). The project compiles against SDK 36 (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 33` in `app/build.gradle.kts`), so the API is available at compile time and needs the `Build.VERSION.SDK_INT >= 35` guard the roadmap already specifies. The "do not use `FLAG_SECURE` on the activity" warning is correct: it would black out the whole broadcast.
3. **Code reading is accurate where it counts.** Verified true at `be674e1`:
   - `CompositorEffect` targets `PREVIEW or VIDEO_CAPTURE` (one effect, up to two outputs).
   - `CompositorProcessor.paintStage()` sizes the stage from `slots.maxBy { it.width * it.height }` and takes that slot's `updateTransformMatrix` (lines 488-507); `presentCamera()` does the same for recorders (line 298); `onOutputSurface()` sets `pictureSize` the same way (line 250). Kotlin's `maxBy` returns the first maximum, so equal areas resolve by arrival order, exactly as claimed.
   - `MainActivity.bindCamera()` builds `ViewPort.Builder(Rational(previewView.width, previewView.height), rotation)` once per bind (line 316) and sets `faces.outputAspect` from the same numbers (line 275). `startCamera()` in `camera_bind.kt` only waits for a nonzero size.
   - `StagePainter.drawAr()` calls `drawCamera(texture, matrix, 0f, plan.arOpacity)` over the full target with no aspect math (lines 593-606). `ArDeckSurface` carries no video size.
   - `ArDeckPlayer` uses `REPEAT_MODE_ONE` and never calls `seekTo(0)`.
   - `onPause()` → `applyCapture()` → `CaptureSwitch.update(foreground=false)` emits `UNBIND_CAMERA`; `onResume()` emits `BIND_CAMERA`. Confirmed in `status_line.kt` and `audio/CaptureSwitch.kt`.
   - `control_bar`, `status_text`, `record_button` ("Record 5 seconds" default, `record_seconds` "Record %1$d s"), `play_button`, `playback_bar` all live in `res/layout/activity_main.xml` and are driven by `showMessage()`.
   - The pill is `TerminalPanel.pill` with a `LaserTitleView` showing `R.string.terminal_pill` (the string is `OFFGRID`; the brackets in the roadmap are descriptive).
   - `LiveActions.CLEAN_OUTPUT` hides `controlBar`, calls `panel.setHiddenForPlayback(true)`, and hides the overlay (`LiveIntegration.kt` lines 124-129). Absorbing it into Broadcast Lock is the right design.
   - `RemoteMessage`: `MAX_FIELD_LENGTH = 256`, `MAX_FRAME_SIZE = 1024`, `fire` allowlist with `ar_play`, `ar_pause`, `ar_next`, and the `fx.` prefix rule for `setting`. `RemoteSession`: `MAX_VIDEO_SIZE = 16 MiB`. `EncryptedChannel`: AES-GCM, strict `counter == receiveCounter` ("Replay or out-of-order frame"), rekey at `1 shl 20` frames.
   - `idleCurtainColor()` keeps a hand-written list of "active" keys (lines 329-332). Rex's critique #3 is correct and `FxKeys.AR_ON` is in that list today, so the deck already keeps the curtain up.
   - Emoji counts are exact: `📁` on 17 lines in `PanelTree.kt`, 39 in `EffectFolders.kt`, 4 in `FxKeys.kt`, 1 each in `PanelActions.kt`, `TerminalPanel.kt`, `FolderLayout.kt`, `microphone_effects_folder.kt`; `EmojiRainLayer.DEFAULT_SET = "❤️🔥✨😂🎉💎"`; `strings.xml` has two lines (`record_gallery_failed`, `effects_failed`).
   - `LookKeys.TITANIUM_FRAME` / `SAPPHIRE_GLASS` exist in `PanelLook.kt` and are toggles in the `display` folder. `FxKeys.MIC_BLOCKED` exists; a camera block does not (the camera toggle key is `fx.cam.on`, aliased as both `FxKeys.CAMERA_ON` and `MicKeys.CAM_ON`).
   - `FxKeys.RECORD_SECONDS` defaults to 30 via `NumberItem(..., 30)` in `EffectFolders.kt` line 693, and `recording_controls.kt` posts a stop task only when `recordSeconds() > 0`, so "0 = unlimited" already works at the data level.
   - CI (`.github/workflows/build-debug-apk.yml`): the slider guard greps `SeekBar|RangeSlider|material\.slider|<Slider|Slider\(` across `04 Build/*/src`; the permission allowlist is a Python set of short names checked against the merged manifest; `ACCESS_NETWORK_STATE` and Wi-Fi permissions are banned; the raw-mic and cloud-speech guards exist.
   - The top level of the panel is 8 feature folders plus 18 platform folders (26 entries). The folder contents in the roadmap's §9.1 table match `PanelTree.kt` and `EffectFolders.kt` item for item.
4. **Security posture of the Phone 2 proposal.** A new packet type on the existing paired, encrypted session, app-private storage, hash verification before the clip appears, and no new permissions is the right shape. `LocalNetwork` already refuses non-site-local addresses and discovery is NSD (no `NEARBY_WIFI_DEVICES`), although the discovery entry point is broken today for a different reason (§2.10).

### 1.2 Why not a clean PASS

1. The render-bug section ranks "largest output wins" as cause #1. In this code that path cannot stretch a picture (§3.1). Handing a coding agent a wrong first cause wastes the top-tier model budget and risks "fixed" without a device change.
2. Two acceptance criteria are false against existing tests and code (§3.3, §3.4).
3. The screenshot design (async PBO) is impossible on the ES 2.0 context the compositor creates (§2.2).
4. Broadcast Lock is specified for cold start but the exposure window on every resume-from-Bigo is larger and unaddressed (§2.1).
5. "Unlimited" recording has no storage or thermal arithmetic behind it (§2.4).
6. The Phone 2 link that Phase 3 and the remote lock rely on cannot be established on a build that passes today's CI (§2.10). The roadmap's "no new network permissions" is right, but the link bring-up is missing from its plan.
7. Several Phase estimates are in calendar weeks. For agent-built work the useful measure is files touched, invasiveness, and risk; I restate the order that way in §5.

### 1.3 Conditions for turning this into a PASS

- Replace §10.1 "Diagnosis (ranked)" with §3.1 and §3.2 of this review, and make the render probe the first task, not the fourth.
- Add the "hidden on every `onStart` until state is known" rule to §10.4 (§2.1 below).
- Replace the PBO screenshot with a synchronous `glReadPixels` on the stage FBO or make the ES 3.0 context upgrade an explicit, separate task (§2.2).
- Replace the two-finger-swipe-down lock gesture and the top-edge long-press with gestures that cannot summon system bars (§2.3).
- Add the storage/thermal budget and the "low brightness while sharing" rule to §10.3 (§2.4).
- Rewrite the two contradicted acceptance criteria (§3.3, §3.4) and rename the Phone 2 monitor (§3.5).

---

## 2. IMPOSSIBLE or RISKY items

Legend: **IMPOSSIBLE** = cannot work as written on this code or this platform. **RISKY** = can work but has a failure mode the roadmap does not state. **UNVERIFIED** = plausible, documented, not checkable without the device.

### 2.1 RISKY: the Broadcast Lock exposure window is on every resume, not only cold start

The callback reports that *our activities* are visible in a recording. While Royalty is in Bigo, OFFGRID is in the background and the state is `NOT_VISIBLE`. When she switches to OFFGRID mid-share, the activity resumes, draws, and *then* the callback flips to `VISIBLE` on the executor. Those first frames carry whatever the panel was last showing. The roadmap's cold-start rule (panel `INVISIBLE` in `onCreate` until state is known) does not cover this path.

**Required change:** hide every chrome view in `onStop()` (not just `onPause()`), keep it hidden on every `onStart()` until the synchronous return value of `addScreenRecordingCallback` is read, and only then reveal. The synchronous return value is the key: it is available before the first frame. Also persist "was locked when backgrounded" and treat it as locked until proven otherwise. See idea §4.3 (Armed State) for the Phone 2 side of this.

**UNVERIFIED on Samsung:** whether One UI's "Smart View", "Link to Windows" or DeX mirroring count as recording. If they do, the lock engages during mirroring, which is a safe false positive.

### 2.2 IMPOSSIBLE as written: "glReadPixels into a PBO (async, two frames later)"

`CompositorProcessor.initGl()` creates the context with `EGL_CONTEXT_CLIENT_VERSION 2` and `EGL_RENDERABLE_TYPE = EGL_OPENGL_ES2_BIT`; the manifest requires `glEsVersion 0x00020000`. Pixel buffer objects and `glMapBufferRange` are OpenGL ES 3.0. Options, in order of preference:

1. Synchronous `glReadPixels` from the stage `GlTarget` (not the window surface) once per tap. At the real stage size (~590×1280, see §3.2) this is roughly 3 MB and a few milliseconds; one dropped frame for a deliberate screenshot is acceptable. Encode PNG off the GL thread.
2. Upgrade the context to ES 3.0 (`EGL_OPENGL_ES3_BIT`, client version 3). ES 3 is backward compatible with the existing ES 2 shaders and the S25 Ultra supports 3.2, but this touches the one context everything shares and should be its own small PR with a fallback to ES 2.

The acceptance criterion "contains no panel pixels" is correct and still testable with option 1 because the panel is never drawn into the stage.

### 2.3 RISKY: the lock and open gestures summon system bars on air

- **Two-finger swipe down = lock.** The activity runs immersive with `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`. A downward swipe that starts near the top edge reveals the status bar (with Android 15's projection chip) and on One UI a two-finger status-bar swipe opens the full quick panel. Either is broadcast. Replace with a gesture that cannot start at an edge, for example a three-finger tap anywhere in the middle 60 percent of the stage, or a 1.5 s two-finger long-press.
- **Long-press top-left corner = open panel.** Fine when unlocked, but add `setSystemGestureExclusionRects` for the hit zone (the system caps exclusions at 200 dp per edge, which is enough), and make sure the coach mark never appears while `locked`.
- **Any accidental touch during a share** that shows a ripple, a focus ring, or a `PopupMenu` (`TopBar.showViewMenu` uses a stock `PopupMenu`) is on air. Under lock the stage must consume and discard touches except the unlock gesture.

### 2.4 RISKY: unlimited recording without the arithmetic

- **Storage.** `AvRecorder.VIDEO_BIT_RATE = 6_000_000` plus 128 kbps AAC is about **2.7 GB per hour**. Five hours is 13.5 GB per stream, plus the gallery copy if "Save to gallery" is on (`GallerySaver` writes a second file to Movies/OFFGRID). The 2 GB free-space floor is a stop, not a plan. State the per-hour cost in the Recording folder and default "Save to gallery" to off while segments are on, or save by moving, not copying.
- **Segment rollover is easier than the roadmap says.** `AvRecorder` sets `KEY_I_FRAME_INTERVAL = 1`, so a keyframe arrives every second. Rolling to a new `Mp4Writer` at the next keyframe needs no sync-frame request. The hard part is audio continuity: AAC frames are 1024 samples (about 21 ms), so align the audio cut to the last AAC frame before the video keyframe and carry the codec config into the new file. `Mp4Writer` is a thin `MediaMuxer` wrapper; add a `rollover()` rather than a second recorder.
- **Thermal.** The heat sources during a share are, in order: Bigo's own screen-capture encoder (full screen, outside our control), the OLED at high brightness, our H.264 recorder, the camera, the GL stage, the MediaPipe segmenter, and the Phone 2 preview encoder. Two cheap wins the roadmap misses: (1) **the S25 brightness can be at minimum during a share**: MediaProjection captures the composed frame, not the panel brightness, so the broadcast is unaffected and the largest controllable heat source drops; (2) cable charging, not wireless, during long streams. Put both in the Green Room checklist. `PowerManager.getThermalHeadroom` and `PerformanceHintManager` (ADPF) are both available on API 33+ and are unused today; the governor in Lab #12 is the right Phase 5 item.
- **Screen lock kills the projection on Android 15 QPR1+.** `keepScreenOn` is set on the root view, which holds only while OFFGRID is the top activity. While Royalty is in Bigo, OFFGRID's flag does nothing; whether Bigo holds the screen on while live is Bigo's behavior, not ours. The Green Room check "screen timeout ≥ 10 min" is correct and should stay.

### 2.5 RISKY (policy): looping clips while off camera on Bigo

Nothing in the roadmap violates Android, Play, or Samsung rules: `DETECT_SCREEN_RECORDING` is a normal permission, `SYSTEM_ALERT_WINDOW` is already granted, the app is sideloaded, and screen sharing another app through "Game Live" is a documented Bigo feature. The risk is Bigo's **content** rules, not its technical ones: hosts who broadcast pre-recorded or looping video with no live presence have been treated as "recorded content" violations on several live platforms, and the Game Live path is intended for games. The architecture is fine; Royalty should decide knowingly how long the deck runs alone and keep a live element (voice, status word, effects) in the loop. This review does not propose any technique to evade platform detection, and the roadmap correctly rejects virtualization and camera hooking.

### 2.6 RISKY: reverse playback by re-encoding

- **Not on the compositor thread.** The roadmap says "decode with MediaCodec to the GL thread". The GL thread is the stage; a 60 s re-encode would stall the broadcast. Use a worker with its own EGL context (or the `ByteBuffer` YUV path, no GL at all).
- **Memory.** Reversing a GOP means holding every frame of that GOP. At 1080p RGBA that is 8 MB per frame; a 2 s GOP at 30 fps is 480 MB. Use YUV `ByteBuffer` output (3 MB per 1080p frame) and a 720p proxy, or two passes (first re-encode with `KEY_I_FRAME_INTERVAL = 0`, all-intra, then reverse by seeking). Clips recorded by OFFGRID itself already have 1 s GOPs, so they are cheap to reverse.
- **Storage.** About 2× per clip; the roadmap's 4 GB deck budget with LRU archive is right. The `ArDeckCatalog` today stores `content://` URIs with persistable permissions; proxies must live in `filesDir/deck/`.

### 2.7 RISKY: the GL thread budget after Phase 2 and 3

Today the stage is ~590×1280 (§3.2), which is why everything fits in 16.6 ms. If the stage is lifted to 1080p and two deck decoders, the segmenter, the recorder, replay, the live preview encoder and the screenshot readback all share one thread, the budget breaks. Rule for every PR: measure with per-stage `SystemClock` timers behind a debug flag before adding a second decoder, and keep the Phone 2 preview encode at a lower cadence (every other frame) when thermal headroom passes 0.8.

### 2.8 RISKY: Lumen glow cost and burn-in on the panel

Glow is View work, not GL work, so it never touches the broadcast frame budget. Two cautions: (1) `BlurMaskFilter` is software and slow for large halos; on API 31+ use `RenderEffect.createBlurEffect` on the glyph layer, which is hardware and cacheable; (2) the 1.2 s "breath" on the active item re-invalidates the panel every frame while open. With the panel auto-dim at 20 s idle (D7) this is acceptable; without it, it is a battery drain during long unlocked sessions.

### 2.9 UNVERIFIED but likely fine

- Media3 1.11.1 is in use. `ExoPlayer.Builder.experimentalSetEnableMediaCodecVideoRendererPrewarming(true)` has existed since Media3 1.5, so the try/catch guard can go; compile will prove it. It helps media-item transitions; whether it tightens the `REPEAT_MODE_ONE` seam is a device question. Idea §4.1 removes the dependence on it.
- The callback behavior with Bigo's projection on One UI. Test it before Bigo with Samsung's own screen recorder, which also uses MediaProjection (Lock Drill, §4.3).

### 2.10 RISKY, possibly blocking: Phone 2 cannot find the S25 on a CI-green build today

`LocalNetwork.discover()` (`live/LocalNetwork.kt` lines 84-95) acquires a `WifiManager.MulticastLock` **before** starting NSD discovery. `MulticastLock.acquire()` requires `CHANGE_WIFI_MULTICAST_STATE`, which the CI allowlist **bans** and which Lane C removed from the manifest (`04 Build/archive/lane_c_wifi_permissions.xml`). The code catches the `SecurityException`, reports "Local network permission is missing." and returns without discovering. `RemotePreviewActivity.findCamera()` has no manual-address fallback. On a stock Android 15 build the Phone 2 link therefore cannot be established at all, which blocks every Phase 3 item and the remote lock/unlock path of Phase 1.3.

**Fix without any new permission (Phase 0 task, Fast model):** drop the `MulticastLock` and call `NsdManager.discoverServices` directly; platform NSD runs in the system mDNS stack and does not need an app-level multicast lock for service discovery. Add a manual path as the always-works fallback: the S25's Pairing folder shows `siteLocalAddress()` and the port next to the pairing key, and Phone 2 accepts a typed address. Acceptance: a Robolectric test that discovery proceeds with no `CHANGE_WIFI_MULTICAST_STATE` in the merged manifest, and a device check that Phone 2 connects on the real Wi-Fi.

**UNVERIFIED:** whether Samsung's mDNS behaves without the lock. The manual address path makes this a convenience issue instead of a blocker.

### 2.11 Small corrections that avoid wasted work

- Do not add `WAKE_LOCK` for the deck. The manifest removes Media3's `WAKE_LOCK` with `tools:node="remove"` and the player is built with `C.WAKE_MODE_NONE`; `keepScreenOn` covers the foreground case.
- The clicker "Sound style" list contains an option named "Metal". It is a sound, not a theme, but under the no-metal rule rename it ("Chime") when the Feedback folder is touched.
- The emoji CI guard needs an allowlist of typographic symbols the panel already uses: `⋮` (menu), `○ ●` (color editor), `█` (laser caret), `−` (minus). A bare `\p{So}` check will fail on `○●█`.

---

## 3. Factual errors versus the real code

### 3.1 The "largest output wins" path cannot stretch the picture

The roadmap's cause #1: the 16:9 `VIDEO_CAPTURE` output can "drive the picture on screen" and give the screen "the video's geometry and crop". Against the code:

- `bindCamera()` puts both use cases in one `UseCaseGroup` with one `ViewPort` (`FILL_CENTER`). CameraX crops every use case in the group to the viewport's aspect ratio, so the preview output and the video output arrive with the **same** aspect (the view's), not 16:9 versus 9:19.5.
- Even if they differed, `drawFinal()` paints the stage into every slot through `StagePainter.present()`, which is aspect-preserving (fit with bars, lines 201-212). The camera is drawn into the stage with the chosen slot's own `updateTransformMatrix`, which is correct for that slot's size.
- With the resolution selectors in use (Preview 1280×720 target, `Quality.HD` for video) and the viewport crop, both outputs are close to **590×1280**. Equal or near-equal areas mean `maxBy` picks by arrival order, exactly as the roadmap says, but the two candidates are the same shape.

So output selection can change *which resolution and which crop alignment* the stage inherits, which is worth fixing (PREVIEW master is still the right rule and is one line), but it cannot produce the stretched lion. It should not be ranked first and should not be where the top-tier model budget goes.

**Cause #2 (bind timing) is weaker than stated.** `onCreate` calls `WindowCompat.setDecorFitsSystemWindows(window, false)` and `hideSystemBars()`, and the theme uses `shortEdges`, so the `PreviewView` is edge-to-edge from its first layout; the ratio at first bind normally equals the final ratio. The first-launch path also does not double-bind: the permission callback's `startCamera()` is invalidated by `bindGeneration` when `onResume()`'s `applyCapture()` emits `BIND_CAMERA` a moment later. Rebinding on a real aspect change is still good hardening (fold-in, split screen) and should stay in the plan, with a lower rank.

**Cause #3 (`drawAr()` no aspect math) is real and unconditional.** It stretches every clip whose aspect differs from the stage, on every launch. It therefore cannot by itself explain "normal after switching apps", but it is the one certain defect in the chain and the fix (fit/fill from `onVideoSizeChanged`) is correct.

**Two suspects the roadmap missed, which do fit "first launch only, normal after a switch":**

1. **`PreviewView` in `COMPATIBLE` (TextureView) mode.** A `TextureView` stretches its buffer to the view bounds until `PreviewView` applies its correction matrix from the `SurfaceRequest`'s transformation info. With a `SurfaceProcessor` the request is flagged as already-transformed; if the correction lands late or against a stale view size on the first bind, the first frames are stretched to the view and the next bind (after the app switch) is clean. This is the only component in the chain that performs a non-uniform scale at all. The render probe should log `PreviewView` size, the `SurfaceRequest` resolution, and the `TextureView` transform at first frame.
2. **`OverlayRenderer.request()`** calls `setDefaultBufferSize` when the stage size changes; the first latched drawing after a size change is drawn full-frame with the new stage, so a single stretched overlay frame is possible when the second output arrives. Cosmetic, one frame, but it is a stretch that exists in the code.

**Recommended diagnosis order:** probe → `drawAr` fit/fill → PREVIEW master → `PreviewView` transform check (consider `PERFORMANCE` mode once Broadcast Lock lands) → rebind on aspect change.

### 3.2 `MAX_STAGE_SIDE = 1920` is not the limiter; the preview selector is

Rex's critique #2 proposes raising `MAX_STAGE_SIDE` to 2340 for a sharper stage. The stage is sized from the output slot, and the output slot is the camera stream cropped to the view aspect. `previewResolution()` targets **1280×720** (`RATIO_16_9` + `ResolutionStrategy(Size(1280, 720), CLOSEST_HIGHER_THEN_LOWER)`) and the recorder is `Quality.HD`. After the ~0.46 viewport crop, the stage is about **590×1280** and is upscaled roughly 2.4× to the 1440×3120 panel. Raising `MAX_STAGE_SIDE` changes nothing until the preview target is raised (1920×1080 gives a ~886×1920 stage) and the overlay, recorder (`RECORD_LONG_SIDE = 1280`), replay, and segmenter budgets are re-checked. This is also why laser words and the status label look soft on stream. It belongs in Phase 1.5 as "Stage resolution lift", with the thermal governor as the counterweight.

### 3.3 The header still contains drum reels, and the existing test asserts it

The roadmap (mandate 7, Phase 2.4 acceptance) says "header contains no `DrumReelView` (existing PR #38 test kept)". At `be674e1`, `TopBar` still constructs `widthReel` and `heightReel` (`DrumReelView`), adds them to the W/H `FrameLayout` at 1×1 with `visibility = GONE`, and shows the tap-to-type boxes on top. `panel_finish_test.kt` line 124 asserts `filterIsInstance<DrumReelView>().size >= 2` in the top bar. The acceptance criterion as written would **fail** the suite. Rewrite it as either "every `DrumReelView` in the top bar has `visibility == GONE` and zero size" (no test change) or "remove the reels from `TopBar` and update `panel_finish_test`" (test change, needs a note in the PR).

Also, `sizeFolder()` notes still say "Width and Height are also on the drum wheels at the top of the panel", which is no longer true for the user; the IA move list should include that copy fix.

### 3.4 The floating overlay already uses `FLAG_SECURE`

Mandate 20, Rex critique #6, and §10.4 say the floating overlay "is also captured by screen share". `OverlayWindowController.overlayFlags()` includes `FLAG_SECURE`, and the close target and dialogs set it too (Lane C QA recorded this as "panel/overlay not in recordings PASS"). A secure window's **content** is never captured by MediaProjection. However, SurfaceFlinger composes a secure layer on a non-secure output as an **opaque black rectangle** of the window's bounds, so the overlay would appear on Bigo as a black box, not as controls. The conclusion (hide it under lock) stands; the reasoning in the roadmap should be corrected so nobody tries to "fix" it by adding `FLAG_SECURE` to the panel, which would put a black box over the stage.

### 3.5 "What Bigo sees" cannot be built as named

Phone 2's monitor (`RemotePreviewEncoder` → `RemotePreviewDecoder`) carries the **GL stage**, not the S25 screen. Bigo captures the screen: stage plus any chrome. Without a MediaProjection of our own (a new system dialog and `FOREGROUND_SERVICE_MEDIA_PROJECTION`, which the allowlist and the roadmap both rule out), Phone 2 cannot see what Bigo sees. Rename it "Stage monitor" and add a **chrome flag** to the 1 Hz `State` packet (`panelVisible`, `topBarVisible`, `controlBarVisible`, `overlayVisible`, `locked`); Phone 2 draws the red frame when any chrome flag is true. That is honest and still catches every mistake that matters.

### 3.6 Smaller corrections

| Roadmap says | Code says |
|---|---|
| "Android 15 / SDK 35" target | `compileSdk 36`, `targetSdk 36`, `minSdk 33`. The API 35 runtime guard is still required. |
| `Themes › 📁 Profiles` | Folder id is `presets` (title "📁 Profiles"); the README still calls it "Presets". The migration table must map `presets`, not `profiles`. |
| "existing ~45 test files" | 44 in `app`, 5 in `live` (`RemoteSecurityTest`, `ar_deck_remote_test`, `live_manifest_test`, `pc_transport_test`, `PcOutputConfigTest`). |
| Phase 2.7 acceptance: "none of the legacy `Vibrator` calls" | Already true: the panel uses `performHapticFeedback` in 13 places and never `Vibrator`/`VibrationEffect`. The criterion is satisfied before the work starts; replace it with "every haptic goes through `HapticGrammar`". |
| `[OFFGRID]` pill | `R.string.terminal_pill` is `OFFGRID`; the ring is a `HoloRingView`. Cosmetic. |
| "Record 30 s" HUD | The default label at first draw is `record_five` ("Record 5 seconds") until `setRecordLabel()` runs; then "Record 30 s". Both are on air today. |
| `experimentalSetEnableMediaCodecVideoRendererPrewarming` "UNVERIFIED on 1.11.x" | Present since Media3 1.5; the project is on 1.11.1. |
| `handoff/20 OFFGRID Council/research/...` and `00 Rex Rules` | Neither path exists in the repository (`handoff/` contains only `qa/`). Create them in the process PR or point the cadence at `docs/`. |
| "`PanelFolder` already nests (`folders` + `items`, `parent`, `trail()`)" | Correct, and it also has `kind: FolderKind`, `tag`, `guide`, `notes`, `results()`. The proposed `category`/`scope`/`iconId` fields fit without breaking callers. |

---

## 4. Innovative ideas, ranked

Rules I held myself to: no root, no new permissions unless named, nothing drawn on the S25 while locked, no OS emoji, no metal, System black/white themes untouched, every idea anchored to code that exists at `be674e1`.

### 4.1 Seam Weld: a GL-side loop crossfade, no proxy files

**What.** Keep a short ring of the last N stage-resolution frames of the deck clip (N = 8 at 30 fps ≈ 266 ms) in `GlTarget`s, the same mechanism `StagePainter` already uses for echo copies (`echoes[]`, `copy()`, `ECHO_SHRINK`). When ExoPlayer reports `onPositionDiscontinuity(DISCONTINUITY_REASON_AUTO_TRANSITION)` (the repeat), draw the ring forward over the first N incoming frames with alpha falling 1 → 0. Every loop seam becomes a 266 ms dissolve from the clip's tail into its head.

**Why it is better.** The roadmap's Lab #6 (auto-trim at import) only works when a near-identical frame pair exists; Seam Weld works for any clip, needs no import step, no proxy, no storage, and no decoder change. Combined with Lab #6 it is invisible.

**Cost.** 8 × 590×1280 RGBA ≈ 24 MB of VRAM at today's stage size (half-res ring: 6 MB). One extra blit per frame during the 266 ms window, nothing otherwise. Files: `StagePainter.kt` (ring + blend), `ar_deck_player.kt` (discontinuity listener posting to the GL handler), `FxKeys.AR_WELD_MS` (0 = off). Unit-testable: alpha curve sums to 1, ring index math.

### 4.2 Invisible Hotkeys: control the S25 while locked, with nothing drawn

**What.** Under Broadcast Lock the stage consumes touches and draws nothing, but a small set of multi-finger gestures still fire commands with haptic confirmation only: three-finger tap = next clip, three-finger long-press = fire the last Moment, four-finger tap = deck play/pause, two-finger long-press 1.5 s = unlock (replacing the corner long-press). Each has a distinct `performHapticFeedback` pattern from `HapticGrammar`, so the hand knows what happened. Single-finger input is ignored entirely, which also protects against pocket and palm touches.

**Why.** Vesper's point in Round 1 is that any S25 menu during a share is a failure. This gives Royalty the five commands she actually needs on the S25 with zero on-air chrome, and it works even if Phone 2's battery dies. Gestures that start away from edges never summon system bars (§2.3).

**Cost.** One `GestureDetector` layer on the root `FrameLayout`, a `LockGestureMap` (pure Kotlin, unit-testable), and a Phone 2 toggle to disable it. No new views.

### 4.3 Armed State plus Lock Drill: close the exposure window and prove the lock before going live

**What.** A two-stage lock. **ARMED** is set from Phone 2 or the top bar before Royalty opens Bigo: it hides all chrome immediately and persists the state. **LIVE** is set by the recording callback. When OFFGRID resumes with ARMED persisted, nothing is drawn but the stage, so the callback's latency (§2.1) never matters. The Green Room (Lab #11) gains a **Lock Drill**: "Start Samsung's screen recorder from Quick Settings; OFFGRID must lock within one second and Phone 2 must show LIVE; stop it; OFFGRID returns to ARMED." Samsung's recorder uses MediaProjection, so this exercises the real Android 15 path without Bigo, on demand, in ten seconds.

**Why.** It turns the roadmap's biggest UNVERIFIED into a repeatable device check Royalty can run before every stream, and it removes the resume race by design rather than by speed.

**Cost.** Two states in `BroadcastLockStore`, one Phone 2 button, one checklist line. The drill needs no code beyond the lock itself.

### 4.4 Geometry Attestation: the app checks its own picture and heals it

**What.** The render probe becomes a permanent, silent self-check. After each bind, the compositor computes `previewAspect = previewView.w/h`, `slotAspect = main.w/h`, `stageAspect`, and `clipAspect` (from `VideoSize`), and sends them in the 1 Hz `State` packet. If `|previewAspect − slotAspect| > 1 percent` for more than three frames, the S25 schedules exactly one automatic `rebindCamera()` (debounced, generation-guarded like `bindGeneration` today) and logs it. Phone 2 shows a small amber glyph while the numbers disagree and a one-tap "Rebind" if the auto-heal did not fix it.

**Why.** The stretched-lion class of bug is a geometry mismatch that the app can detect numerically. Self-healing on the first frames beats asking Royalty to switch apps, and it gives Rex the exact numbers from the device without a logcat session.

**Cost.** A pure-Kotlin `StageGeometry` object (the roadmap already wants it for tests) plus 40 bytes in the `State` packet. No UI on the S25.

### 4.5 Stage Monitor with a Chrome Flag: honest confidence on Phone 2

**What.** The existing H.264 preview path to Phone 2 becomes the Stage Monitor. The S25 adds `panelVisible`, `topBarVisible`, `controlBarVisible`, `overlayVisible`, `locked`, `recording`, `freeBytes`, `thermalHeadroom` to the `State` packet. Phone 2 renders the stage edge-to-edge; a red frame plus a laser-lettered word appears the moment any chrome flag is true during LIVE, and a soft pulse when `thermalHeadroom > 0.8` or `freeBytes < 4 GB`.

**Why.** It is the buildable version of the council's #1 pick (§3.5) and it is more useful than a true screen mirror, because it tells Royalty *why* the frame is red.

**Cost.** Packet fields, one Phone 2 overlay, no new permission, no new encoder.

### 4.6 Quiet Hands: a Phone 2 mode with no light on Royalty's face

**What.** Phone 2's controller gets a "Quiet Hands" layout: the screen is true black with hairline zone borders at 10 percent glow; six large zones map to Sets and Moments; every zone confirms by a distinct haptic, not by light. A long-press reveals the labels for two seconds.

**Why.** A bright Phone 2 lights the host's face and shows in the camera as a glance-down. Streamers in PK battles run blind; the OP-1 and Procreate references in the roadmap both say the same thing: the hand learns the map. It also halves Phone 2's screen power during a five-hour stream.

**Cost.** Phone 2 UI only; uses the existing `fire` and `setting` messages.

### 4.7 Stage-first boot through a `ViewStub`

**What.** `activity_main.xml` keeps only `PreviewView`, the dark stage, and a `ViewStub` for `TerminalPanel`, the top bar, and the HUD. The stub inflates after the first stage frame and only if not locked.

**Why.** Stronger than `INVISIBLE`: an un-inflated panel cannot flash, cannot receive a stray `requestLayout`, and shaves cold start. It also makes `SurfaceCleanTest` trivial: before inflation the view tree has no chrome by construction.

**Cost.** Layout change plus moving `findViewById` calls behind the inflate callback; `LiveIntegration.install` and `DarkStage` take the panel lazily.

### 4.8 One-second segment rollover (already cheap)

**What.** Because `KEY_I_FRAME_INTERVAL = 1`, `AvRecorder` can roll to a new `Mp4Writer` at any keyframe within one second of the 30-minute mark, or on demand from Phone 2 ("cut here"). Each segment gets a sidecar line in a `session.txt` (start time, duration, size, sha256) so a corrupt file never hides the others.

**Why.** It makes Kade's "one corrupt file loses five hours" fear disappear with almost no new code, and "cut here" gives Royalty chapter marks for free.

### 4.9 Stage resolution lift with a thermal counterweight

**What.** Raise `previewResolution()` to 1920×1080 so the stage becomes ~886×1920, keep `RECORD_LONG_SIDE` at 1280 for the file, and let the thermal governor (Lab #12) drop the preview target back to 720p when headroom passes 0.9. Laser words, the status label, and glow edges on the stage become sharp on the broadcast.

**Why.** This is the real version of Rex's critique #2 (§3.2) and is the single most visible quality change for viewers.

### 4.10 Hardware glow cache

**What.** `GlowSpec` renders each glyph once per `(glyphId, hue, intensity, halo, sizePx)` into a hardware layer with `RenderEffect.createBlurEffect` (API 31+, the project's `minSdk` is 33), cached in an LRU keyed by that tuple, with a `BlurMaskFilter` fallback only for software canvases.

**Why.** The roadmap's D10 picks `BlurMaskFilter`, which is CPU-side and slow for the 14 dp halos Lumen wants. Hardware blur keeps the panel at 120 Hz on the S25 Ultra and makes the breath animation free.

### 4.11 Deck "Hold Frame" and "Black Guard"

**What.** Two small rules in `CompositorProcessor`: (1) when a clip ends without loop or a transfer swaps clips, keep the last decoded frame (`ArDeckSurface.hasFrame` already preserves it) rather than dropping to the curtain; (2) if the deck is on but `hasFrame` is false for more than 500 ms (decoder stall), raise a Phone 2 flag instead of letting the idle curtain fall over a live room.

**Why.** Rex's critique #3 in reverse: the curtain protects privacy, but a surprise black frame mid-stream costs the room.

---

## 5. Recommended build order and model tier

Difficulty is stated by files touched, invasiveness, and risk, not in calendar time. "Fast model" means a default coding model is sufficient with the acceptance tests as guardrails. "Top model" means multi-file GL, CameraX, lifecycle, media, or crypto reasoning where a wrong guess is costly.

### Phase 0: approvals and guardrails

| # | Item | Files | Tier | Notes |
|---|---|---|---|---|
| 0.1 | Royalty approves `DETECT_SCREEN_RECORDING` in the CI allowlist | `.github/workflows/build-debug-apk.yml` (`allowed` set, short name) | Fast | One line plus a comment. |
| 0.2 | Royalty approves the slider guard change | same | Fast | Ban `SeekBar\|RangeSlider\|material\.slider`; allow `BioSliderView`. Do it openly, as the roadmap says. |
| 0.3 | Emoji guard, warn mode, with the symbol allowlist from §2.11 | same | Fast | Flip to fail in Phase 2. |
| 0.4 | Phone 2 link bring-up: NSD without `MulticastLock`, manual address fallback in the Pairing folder (§2.10) | `live/LocalNetwork.kt`, `live/RemotePreviewActivity.kt`, `live/PairingActivity.kt`, `EffectFolders.liveRemote()` | Fast | Blocks Phase 3 and the remote lock path; no new permission. |
| 0.5 | Render probe behind a debug flag (log `PreviewView` size, `SurfaceRequest` resolution, each output size and targets, stage size, `VideoSize`, `TextureView` transform for the first 3 frames after each bind) | `CompositorProcessor.kt`, `MainActivity.kt`, `ar_deck_player.kt` | Fast | Diagnostic only, never on air. Ship this first and get one device log from Royalty before 1.1 is designed in detail. |

### Phase 1: protect the stream

| # | Item | Files | Tier | Why this order |
|---|---|---|---|---|
| 1.1 | Deck fit/fill from `onVideoSizeChanged`; no draw before first size; `FxKeys.AR_FIT` | `ar_deck_player.kt`, `ar_deck_surface.kt`, `StagePainter.kt`, `FxKeys.kt`, new `effects/stage_geometry.kt` | Top | The one certain stretch. Pure math is unit-testable. |
| 1.2 | PREVIEW output is master (`paintStage`, `presentCamera`, `onOutputSurface`) | `CompositorProcessor.kt` | Fast | Three one-line changes plus a test on arrival order. |
| 1.3 | Broadcast Lock: `BroadcastLockStore` with AUTO/MANUAL/REMOTE/ARMED, callback in `onStart`/`onStop`, chrome hidden on every `onStart` until state is known, absorb `CLEAN_OUTPUT`, overlay hidden under lock, `lock_on`/`lock_off`/`arm` in the `fire` allowlist | new `broadcast_lock_store.kt`, `MainActivity.kt`, `TerminalPanel.kt`, `TopBar.kt`, `LiveIntegration.kt`, `live/RemoteMessage.kt`, `AndroidManifest.xml` | Top | Lifecycle plus permission plus the resume race (§2.1). |
| 1.4 | Pristine surface: `control_bar` off the stage, pill removed from the stage, messages routed to the panel status line and Phone 2, stage-first boot via `ViewStub` (§4.7) | `activity_main.xml`, `MainActivity.kt`, `status_line.kt`, `TerminalPanel.kt` | Fast | Well-scoped visibility and routing edits with `SurfaceCleanTest` as the guard. |
| 1.5 | Rebind on aspect change (debounced, generation-guarded) and Geometry Attestation numbers in the `State` packet (§4.4) | `camera_bind.kt`, `MainActivity.kt`, `live/RemoteSession.kt` | Top | Touches CameraX binding; small but easy to get wrong. |
| 1.6 | Unlimited recording: `RECORD_SECONDS` default 0, 30-min rollover at the next keyframe, free-space guard, top-bar REC with elapsed time, camera block (`FxKeys.CAMERA_BLOCKED` through `CaptureSwitch`) | `recording_controls.kt`, `audio/AvRecorder.kt`, `audio/Mp4Writer.kt`, `audio/CaptureSwitch.kt`, `TopBar.kt`, `FxKeys.kt`, `GallerySaver.kt` | Top | MediaMuxer rollover with audio alignment is the risk; `CaptureSwitch` change is trivial and already unit-tested. |
| 1.7 | Screenshot via synchronous `glReadPixels` on the stage FBO (§2.2) | `CompositorProcessor.kt`, new `screenshot_capture.kt` | Fast | Once the PBO idea is dropped this is small. |
| 1.8 | Invisible Hotkeys under lock (§4.2) | `MainActivity.kt`, new `lock_gesture_map.kt`, `HapticGrammar` | Fast | Pure gesture map with tests; depends on 1.3. |
| 1.9 | Stage resolution lift (§4.9) behind a setting, default off until the governor exists | `MainActivity.kt` (`previewResolution()`), `CompositorProcessor.kt` | Fast | One selector change; measure frame time on device before defaulting on. |

### Phase 2: Lumen look and the new IA

| # | Item | Tier | Notes |
|---|---|---|---|
| 2.1 | Tokens, `GlowSpec`, hardware glow cache (§4.10) | Top (craft) | Visual bar; custom Views; contrast tests. |
| 2.2 | Glyph paths and packs, `FolderGlyphView` with baked lettering | Top (craft) | The `titleBounds ⊂ tabBounds` test is good. |
| 2.3 | Emoji strip (mechanical) and Particle Rain | Fast | Text edits plus the guard flip; `EmojiRainLayer` keeps user text input off by default. |
| 2.4 | `BioSliderView`, `GlowToggleView`, debounce pref writes (Rex #9) | Top | A11y `RangeInfo`, haptics, detents. Rewrite the header criterion per §3.3. |
| 2.5 | New tree and `folder_id_migration.kt` (map `presets`, `size`, `display`, `live_remote`, and the rest) | Fast | Data restructuring with `TreeInvariantsTest` and `FolderIdMigrationTest`; the strict tests make a fast model safe. |
| 2.6 | Archive the metal looks and the "Metal" clicker name | Fast | Nothing deleted. |

### Phase 3: Phone 2 master

| # | Item | Tier | Notes |
|---|---|---|---|
| 3.1 | Tree manifest and a data-driven Phone 2 tree | Fast | Serialization plus a parity test. |
| 3.2 | File transfer packets, priority writer, resume, hash verify | Top (security) | Extend `RemoteSecurityTest`; add a reconnect-and-resume path for counter desync (Rex #5). |
| 3.3 | Stage Monitor with chrome flag (§4.5), Health strip, Quiet Hands (§4.6), gestures and haptics on Phone 2 | Fast | UI over existing pipelines. |
| 3.4 | A/B crossfade (two players, two `ArDeckSurface`s), Seam Weld (§4.1) | Top | GL thread budget; release the idle decoder after the fade. |
| 3.5 | Reverse proxy builder on a worker with its own EGL context or YUV buffers (§2.6) | Top | Bounded memory; 720p proxy; progress to Phone 2. |
| 3.6 | Green Room with Lock Drill (§4.3), brightness and cable checks (§2.4) | Fast | Reads existing state. |

### Phase 4 and 5

Sets, Beat-synced glow, Ambient color match, Voice Character pad, Lumen Ring, Curtain call, Drop zones: all Fast except Sets (Top, because it snapshots `fx.*` plus deck state atomically and must not fight `idleCurtainColor()`). Thermal governor: Top. Voice cues on Phone 2: only with Royalty's approval, as the roadmap says.

---

## 6. Acceptance-criteria gaps

Criteria the roadmap should add or tighten before any PR is opened.

### Broadcast Lock

- [ ] **Resume gate:** with `ARMED` persisted or the callback returning `VISIBLE`, no chrome view is ever `VISIBLE` on any frame after `onStart()`, including resume from recents (Robolectric: drive `onStop()` → `onStart()` with the shadow returning `VISIBLE`).
- [ ] **Touch sink:** while locked, a single-finger tap, drag, and long-press on the stage produce no view change, no ripple, no `PopupMenu`, and no system-bar reveal (assert no `requestLayout`/`invalidate` on chrome views; assert the gesture exclusion rects are set).
- [ ] **Lock Drill:** documented device step using Samsung's screen recorder; expected: locked within 1 s, Phone 2 shows LIVE, unlock returns to ARMED.
- [ ] **Chrome flag parity:** the `State` packet's `panelVisible` equals the real `View.isShown` of `TerminalPanel` on every send (test by toggling and sampling).
- [ ] **Callback registration symmetry:** `addScreenRecordingCallback` in `onStart`, `removeScreenRecordingCallback` in `onStop`, guarded by `SDK_INT >= 35`; on API 33 and 34 the manual and remote paths still work (Robolectric SDK 33 run).

### Render fix

- [ ] **Probe log present** on the device before 1.1 is merged, with the five numbers for first launch and for a resume.
- [ ] **No non-uniform scale anywhere:** `StageGeometryTest` asserts `sx == sy` for every fit/fill result, and a Robolectric test asserts `PreviewView`'s `TextureView` transform matrix has equal X and Y scale once the first frame has been delivered.
- [ ] **Overlay resize:** after a stage size change, the first latched overlay frame is skipped or redrawn at the new size (no one-frame stretch).
- [ ] **Equal-area tie:** `StageGeometryTest` includes the real case (two outputs of equal size, both arrival orders) and asserts the PREVIEW slot wins.

### Recording

- [ ] **Storage arithmetic shown:** the Recording folder states "about 2.7 GB per hour at the current quality" from the real bit rates, not a constant.
- [ ] **Rollover alignment:** the audio cut lands within one AAC frame (1024 samples) of the video keyframe; the next file starts with codec config; total duration across segments equals the recording duration ± 1 frame.
- [ ] **Gallery policy:** with segments on, "Save to gallery" moves or links, never duplicates 13 GB.
- [ ] **Low-storage behavior:** at the floor, the current segment is finalized (playable) before stopping; Phone 2 receives the reason.

### Screenshot

- [ ] Readback comes from the stage FBO, not the window surface; the saved PNG size equals the stage size; no panel pixels; the GL thread stall is measured and under 10 ms at the current stage size.

### Deck

- [ ] **Seam Weld:** at the repeat discontinuity, the blend window is N frames, alpha monotone 1 → 0, and the ring never samples a frame newer than the seam.
- [ ] **No black frame:** with the deck on and a clip loaded, the stage never shows the idle curtain for more than one frame during loop, skip, or crossfade (assert `idleCurtainColor() == null` across a scripted transition).
- [ ] **Reverse proxy:** built off the compositor thread (assert the GL handler's queue depth stays under 2 during a build), peak memory under 300 MB for a 60 s 1080p source, output playable in `ArDeckPlayer`.

### Phone 2 link and transfer

- [ ] **Link without the banned permission:** discovery starts with no `CHANGE_WIFI_MULTICAST_STATE` in the merged manifest (Robolectric), and the manual address path connects when discovery fails (device check).
- [ ] **Head-of-line bound measured:** with a 300 MB transfer in flight, a `fire` round trip is under 50 ms on the test LAN (the roadmap's "one chunk-time" is about 13 ms at 20 MB/s; state the real budget).
- [ ] **Counter desync recovery:** kill the socket mid-transfer; the session re-pairs from the stored key, the transfer resumes at the acknowledged offset, and no frame is replayed (extend `RemoteSecurityTest`).
- [ ] **Rejected files are archived, not deleted,** per the no-deletion rule (`deck/rejected/`).

### IA and Lumen

- [ ] **Header criterion rewritten** (§3.3): hidden reels are either asserted `GONE` and zero-size or removed with the test updated in the same PR.
- [ ] **Migration covers `presets`** and the copy in `sizeFolder()` notes.
- [ ] **Emoji guard allowlist** covers `⋮ ○ ● █ −` and any glyph stand-ins introduced before `glyph_paths.kt` lands.
- [ ] **Contrast at glow 0 percent** is tested against both System themes (pure black panel in Night, pure white in Day), since Lumen glow must sit on top of either without changing them.
- [ ] **No metal anywhere:** a test scans folder titles, item titles, choice labels, and notes for "titanium", "steel", "metal", "chrome", "sapphire" (case-insensitive) and fails on a match outside the archive.

### Process

- [ ] Every PR against `final-build` links the roadmap section and this review section it implements, lists the model tier used, and attaches the CI run. Royalty merges; `main` is never touched.

---

*End of review. PASS WITH CHANGES. Nothing here is approved until Royalty says go.*
