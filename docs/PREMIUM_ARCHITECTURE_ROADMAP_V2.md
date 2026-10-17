# OFFGRID-Compositor: Premium Architecture Roadmap V2

> **STATUS: APPROVED.** Royalty and Gemini approved the Premium Architecture Roadmap on Thu Oct 15, 2026, on the condition that every correction in `docs/ARCHITECT_REVIEW.md` is integrated first. This document is that integration. It is the build document. **Approval covers the plan, not any merge:** every change still arrives as a PR against `final-build` with CI evidence, and only Royalty merges.
> **Supersedes:** `docs/PREMIUM_ARCHITECTURE_ROADMAP.md` (V1, the council's proposal). V1 is kept unchanged as the council record (research, minutes, debate log, sources, scripture). Where V1 and V2 differ, **V2 wins.** Coding agents build from V2 only.
> **Code read:** `final-build` @ `be674e1` (every file, line, key, and constant named below was checked at that commit; see `docs/ARCHITECT_REVIEW.md` for the verification trail).
> **Target:** Samsung Galaxy S25 Ultra · Android 15 (API 35 runtime guard; the project compiles with `compileSdk 36`, `targetSdk 36`, `minSdk 33`) · CameraX 1.6.2 + OpenGL ES 2.0 compositor · Media3 1.11.1 · Phone 2 over local Wi-Fi · Broadcast through Bigo Live screen share · No root.
> **Rules that never relax:** no files deleted (archive instead); underscores, not hyphens, in new file and branch names; no steel, titanium, chrome, sapphire, or metal looks; no OS emoji in any UI; the black/white System themes stay exactly as they are; `main` is never touched; PRs go against `final-build`; Royalty alone merges.

---

## 0. Integration ledger: what V2 changes from V1

Every row cites the review section that required it. Agents may skip this table; Royalty and reviewers should not.

| # | V1 said | V2 says | Review |
|---|---|---|---|
| 1 | Stretched-lion cause #1 is "largest output wins" | Output selection cannot stretch (one `UseCaseGroup`, one `ViewPort`, aspect-preserving `present()`). PREVIEW master stays as a one-line hardening. The one certain stretch is `StagePainter.drawAr()`. Two missed suspects added: `PreviewView` `COMPATIBLE` (TextureView) transform timing and `OverlayRenderer` resize. Render probe ships first | §3.1 |
| 2 | Raise `MAX_STAGE_SIDE` to 2340 for a sharper stage | The stage is ~590×1280 because `previewResolution()` targets 1280×720. The lift is a preview-selector change (`1920×1080` → ~886×1920 stage), behind a setting, with the thermal governor as counterweight | §3.2, §4.9 |
| 3 | Broadcast Lock gate is a cold-start rule | The exposure window exists on **every resume**. Chrome is hidden in `onStop()`, stays hidden on every `onStart()` until the synchronous return of `addScreenRecordingCallback` is read, and an **ARMED** state is persisted so a resume draws stage only | §2.1, §4.3 |
| 4 | Screenshot via `glReadPixels` into a PBO, async | The EGL context is ES 2.0; PBOs need ES 3.0. Synchronous `glReadPixels` from the stage `GlTarget` once per tap, PNG encoded off the GL thread. ES 3.0 upgrade is a separate optional task | §2.2 |
| 5 | Lock = two-finger swipe down; unlock = top-right corner long-press 1.5 s | Edge gestures summon system bars on air. Lock and unlock are a **two-finger long-press 1.5 s in the middle 60 percent of the stage**. Single-finger input is a no-op under lock. `setSystemGestureExclusionRects` for any hit zone | §2.3, §4.2 |
| 6 | Unlock policy A4 open ("allow unlock during share?") | **Decided: NO.** Auto-lock holds while the callback reports VISIBLE. The "Allow unlock during share" option does not exist | A4 |
| 7 | Phone 2 connects over existing NSD discovery | `LocalNetwork.discover()` acquires a `MulticastLock`, which needs the CI-banned `CHANGE_WIFI_MULTICAST_STATE`, so discovery fails on every CI-green build and `findCamera()` has no fallback. Phase 0 removes the lock and adds a typed-address fallback | §2.10 |
| 8 | Floating overlay "is also captured by screen share" | `OverlayWindowController.overlayFlags()` already sets `FLAG_SECURE`; its content is never captured, but it composes as an **opaque black box** on the broadcast. Conclusion unchanged (hide under lock); nobody adds `FLAG_SECURE` to the panel | §3.4 |
| 9 | Phone 2 "What Bigo sees" monitor | Renamed **Stage Monitor**: Phone 2 sees the GL stage, not the screen. A chrome flag in the 1 Hz `State` packet draws the red frame when any chrome is visible | §3.5, §4.5 |
| 10 | "Unlimited" recording with a 2 GB floor | 6 Mbps + 128 kbps ≈ **2.7 GB per hour**; shown in the Recording folder from the real bit rates. Rollover at the next keyframe (`KEY_I_FRAME_INTERVAL = 1` gives one every second). Gallery save moves or links, never duplicates. Brightness-to-minimum and cable charging in the Green Room | §2.4, §4.8 |
| 11 | Header acceptance: "contains no `DrumReelView`" | `TopBar` still holds two hidden reels and `panel_finish_test.kt:124` asserts `>= 2`. Criterion rewritten: every `DrumReelView` in the top bar is `GONE` and zero-size | §3.3 |
| 12 | `Themes › Profiles` migrates as `profiles` | Folder id is `presets`. Migration maps `presets` → `sets` | §3.6 |
| 13 | Reverse proxy decodes "to the GL thread" | Built on a worker with its own EGL context or YUV `ByteBuffer`s, 720p proxy, bounded memory | §2.6 |
| 14 | Glow bitmaps via `BlurMaskFilter` | `RenderEffect.createBlurEffect` hardware layer cache (API 31+; `minSdk` is 33), `BlurMaskFilter` only for software canvases | §2.8, §4.10 |
| 15 | Emoji guard: fail on `\p{So}` | Allowlist `⋮ ○ ● █ −` (menu, color editor, laser caret, minus) or `○●█` break the build | §2.11 |
| 16 | Haptics acceptance "none of the legacy `Vibrator` calls" | Already true today; replaced with "every haptic goes through `HapticGrammar`" | §3.6 |
| 17 | Media3 prewarming "UNVERIFIED on 1.11.x, guard with try/catch" | Present since Media3 1.5; the guard goes. Seam Weld removes the dependence on it anyway | §2.9, §4.1 |
| 18 | Phases in calendar weeks; 1.1 is the render fix | Build order adopted **exactly** from review §5: probe and link bring-up in Phase 0, then deck fit/fill, PREVIEW master, Broadcast Lock, pristine surface. Difficulty by files, invasiveness, risk | §5 |
| 19 | (not stated) | Do not add `WAKE_LOCK` (manifest removes it; player uses `C.WAKE_MODE_NONE`). Rename the "Metal" clicker sound to "Chime". `handoff/20 OFFGRID Council/research/` and `00 Rex Rules` do not exist; cadence output goes under `docs/council/` | §2.11, §3.6 |
| 20 | (not stated) | Bigo **content** policy on looping clips while off camera is a knowing decision for Royalty; the architecture is unchanged | §2.5 |
| 21 | Controls on the S25 when unlocked | **New rule:** mic-block and camera-block toggles and all sliders are allowed on the S25 but are **invisible during screen share**; the live control surface is Phone 2 | Approval note |

---

## 1. Approved decisions (recorded)

| # | Decision | Status | Lands in |
|---|---|---|---|
| **A1** | `DETECT_SCREEN_RECORDING` is added to the CI permission allowlist (`.github/workflows/build-debug-apk.yml`, `allowed` set, short name, with the comment "Broadcast Lock auto trigger (Royalty approved 2026-10-15)") | **APPROVED** | Phase 0, row 0.1 |
| **A2** | The CI "no sliders" guard changes openly: ban `SeekBar`, `RangeSlider`, `com.google.android.material.slider`; allow **only** `BioSliderView`. A test commit containing a `SeekBar` must fail CI | **APPROVED** | Phase 0, row 0.2 |
| **A3** | The 12-folder IA (V1 §9.4) and the archive list (Titanium frame, Sapphire glass, Record length, Clip id) | **APPROVED**, with the V2 corrections in §6 | Phase 2, row 2.5 and 2.6 |
| **A4** | Unlock policy: **no unlocking during an active share.** Auto-lock holds while the screen-recording callback reports `SCREEN_RECORDING_STATE_VISIBLE`. Manual and remote locks can be cleared only when the callback reports `NOT_VISIBLE` (or on API 33/34 where no callback exists) | **APPROVED** | Phase 1, row 1.3 |
| **R1** | Mic-block and camera-block toggles and sliders are **allowed** on the S25 (top bar and panel) but are **invisible during screen share**: everything that is chrome hides under Broadcast Lock. **All controls live on Phone 2** during a share | **APPROVED** (Royalty's note) | Phase 1 rows 1.3, 1.4, 1.6; Phase 3 |
| A5 | Voice cues on Phone 2 (cloud API, Keystore key) | Open | Lab #8, Phase 5 |
| A6 | Optional emulator CI job | Open | Phase 5 |

---

## 2. Executive summary (phone scan)

- **The broadcast surface is the whole S25 screen.** Bigo captures everything the S25 shows: the HUD bar (`control_bar`), the pill, the top bar, the panel, and the floating overlay (as a black box, §4.2). Broadcast Lock hides every bit of chrome, including the mic/camera-block toggles and every slider (R1). Phone 2 is the control surface while live.
- **The stretched-lion bug, correctly ranked.** The one certain stretch is `StagePainter.drawAr()`, which draws every deck clip full-frame with no aspect math. Output selection (`maxBy`) cannot stretch because both outputs share one `ViewPort` and `present()` preserves aspect. The "first launch only" symptom points at `PreviewView`'s TextureView transform timing on the first bind and, cosmetically, `OverlayRenderer` resize. A render probe ships first and produces the five numbers from the device before the fix is designed in detail (§7.1).
- **Broadcast Lock is real Android 15 API and now closes the resume window.** `WindowManager.addScreenRecordingCallback` + `DETECT_SCREEN_RECORDING` (normal permission, API 35, approved A1). Chrome is hidden in `onStop()` and on every `onStart()` until the synchronous initial state is read; an **ARMED** state set before opening Bigo guarantees a resume draws stage only (§7.3). **No unlocking during a share** (A4).
- **Phone 2 can connect again before anything else is built.** Today's NSD path needs a banned permission; Phase 0 removes the `MulticastLock` and adds a typed-address fallback (§7.5). Then Phone 2 becomes master: folders, glow, deck commands, encrypted resumable transfer, the **Stage Monitor** with a chrome flag, and Quiet Hands.
- **Unlimited recording with the arithmetic shown:** ~2.7 GB per hour at today's bit rates, one-second keyframes make segment rollover cheap, gallery save never duplicates, and the Green Room tells Royalty to drop the panel brightness to minimum (the projection does not see brightness) and charge by cable.
- **Lumen design system** as approved: true OLED black, bespoke closed-contour vector glyphs, laser lettering baked into the folder shape, desaturated bioluminescent glow with per-icon control, `BioSliderView` and left-to-right `GlowToggleView`. Glow is hardware-blurred and cached. No metal, no emoji. The System black/white themes are untouched.
- **Build order (§9) is the review's §5, adopted exactly:** Phase 0 guardrails, link bring-up, render probe → Phase 1 protect the stream → Phase 2 Lumen and the 12-folder IA → Phase 3 Phone 2 master → Phase 4–5 signature features and hardening. Each row carries a Fast-model or Top-model tier.

---

## 3. Mandate checklist (updated)

| # | Royalty's mandate | Where it is covered in V2 | Status |
|---|---|---|---|
| 1 | No default OS emojis; bespoke glowing vector icons | §5.3, §9 Phase 2.2–2.3, emoji guard with allowlist (§5.6) | Covered |
| 2 | Folder titles in laser lettering baked into folder geometry | §5.4, Phase 2.2 | Covered |
| 3 | OLED black, customizable glow, per-folder swappable icons, glows on/off | §5.1–§5.3, `LOOK › Icons` | Covered |
| 4 | Sliders replace typed 0–100; fluid left-to-right toggles | §5.5, A2 approved, Phase 2.4. **Invisible under lock (R1)** | Covered |
| 5 | Clean broadcast surface | §7.2, Phase 1.4 | Covered |
| 6 | Unlimited recording, Record and Screenshot icons, mic-block and camera-block | §7.4, Phase 1.6, 1.7. Camera block is new (`FxKeys.CAMERA_BLOCKED`). **All invisible under lock (R1); Phone 2 carries them live** | Covered |
| 7 | No laser reels in the header; W/H tap-to-type boxes | §5.5, criterion rewritten per §3.3 of the review (§10 "IA and Lumen") | Covered |
| 8 | Fix the stretched-lion bug | §7.1 (corrected diagnosis), Phase 0.5, 1.1, 1.2, 1.5 | Covered |
| 9 | Broadcast Lock; S25 opens straight to the clean loop surface | §7.3, Phase 1.3; A1, A4 approved | Covered |
| 10 | Phone 2 master controller incl. import/send, loop/reverse/skip/crossfade | §7.5–§7.7, Phase 0.4, Phase 3 | Covered |
| 11 | AR Video Deck: headless ExoPlayer, seamless looping | §7.7 (Seam Weld, Hold Frame, Black Guard), Phase 3.4 | Covered |
| 12 | Keep System themes; no metal; $777 luxury | §5, "Metal" clicker renamed, no-metal test (§10) | Covered |
| 13 | Folder renames, own folders, item-level move list | §6 (A3 approved), V1 §9.3 move list stands with the `presets` correction | Covered |
| 14–15 | Research team, council minutes | V1 §4–§7, §18 (unchanged record) | Covered |
| 16 | Rex picks the model per task | §9 tiers, §12 | Covered |
| 17 | KJV scripture foundation | V1 §16 (unchanged) | Covered |
| 18 | Autonomous continuous improvement | §12 | Covered |
| 19 | Nothing merged without Royalty; `main` untouched | Header, §12 | Covered |
| 20 | MediaPipe cloak/ghost and floating overlay kept | `CAMERA › Body & Cloak`; overlay hidden under lock with the `FLAG_SECURE` fact corrected (§4.2) | Covered |
| 21 | Innovation Lab, top 5 ranked, handoff-ready criteria | §8, §10 | Covered |

---

## 4. Platform facts and hard limits

### 4.1 Verified at `be674e1` (build on these)

- `CompositorEffect` targets `PREVIEW or VIDEO_CAPTURE`; `paintStage()` (lines 488–507), `presentCamera()` (298), and `onOutputSurface()` (250) all pick `slots.maxBy { it.width * it.height }`; `maxBy` returns the first maximum, so ties resolve by arrival order.
- `MainActivity.bindCamera()` builds one `UseCaseGroup` with one `ViewPort(Rational(previewView.width, previewView.height), rotation)` (line 316), `FILL_CENTER`; `faces.outputAspect` uses the same numbers (275). `previewResolution()` targets `1280×720` (`RATIO_16_9`, `CLOSEST_HIGHER_THEN_LOWER`); the recorder is `Quality.HD`. After the viewport crop both outputs are about **590×1280**.
- `CompositorProcessor.initGl()` creates the EGL context with `EGL_CONTEXT_CLIENT_VERSION 2` and `EGL_OPENGL_ES2_BIT`; the manifest requires `glEsVersion 0x00020000`. `MAX_STAGE_SIDE = 1920f`, `RECORD_LONG_SIDE = 1280f`.
- `StagePainter.present()` is aspect-preserving fit (lines 201–212). `StagePainter.drawAr()` calls `drawCamera(texture, matrix, 0f, plan.arOpacity)` over the full target (593–606). `ArDeckSurface` carries no video size.
- `ArDeckPlayer`: headless `ExoPlayer`, `REPEAT_MODE_ONE`, `C.WAKE_MODE_NONE`, never `seekTo(0)`. Media3 1.11.1.
- `PreviewView` is in `COMPATIBLE` (TextureView) mode, `fillCenter`, `keepScreenOn` on the root (`res/layout/activity_main.xml`).
- Lifecycle: `onPause()` → `applyCapture()` → `CaptureSwitch.update(foreground=false)` → `UNBIND_CAMERA`; `onResume()` → `BIND_CAMERA`; `bindGeneration` invalidates stale binds.
- `LiveActions.CLEAN_OUTPUT` hides `controlBar`, calls `panel.setHiddenForPlayback(true)`, hides the overlay (`LiveIntegration.kt` 124–129).
- `OverlayWindowController.overlayFlags()` includes `FLAG_SECURE`; the close target and dialogs set it too.
- `RemoteMessage`: `MAX_FIELD_LENGTH = 256`, `MAX_FRAME_SIZE = 1024`, `fire` allowlist (`ar_play`, `ar_pause`, `ar_next`, …), `fx.` prefix rule for `setting`. `RemoteSession.MAX_VIDEO_SIZE = 16 MiB`. `EncryptedChannel`: AES-GCM, strict in-order counters, rekey at `1 shl 20`.
- `LocalNetwork.discover()` (lines 84–95) acquires a `WifiManager.MulticastLock` before NSD, catches the `SecurityException`, and returns with "Local network permission is missing." `RemotePreviewActivity.findCamera()` has no manual path. `LocalNetwork` refuses non-site-local addresses.
- `AvRecorder.VIDEO_BIT_RATE = 6_000_000`, AAC 128 kbps, `KEY_I_FRAME_INTERVAL = 1`. `Mp4Writer` is a thin `MediaMuxer` wrapper. `GallerySaver` copies to `Movies/OFFGRID`. `FxKeys.RECORD_SECONDS` defaults to 30 (`EffectFolders.kt` 693); `recording_controls.kt` posts the stop task only when `recordSeconds() > 0`.
- `idleCurtainColor()` keeps a hand-written list of active keys (lines 329–332) that includes `FxKeys.AR_ON`.
- `TopBar` constructs `widthReel`/`heightReel` (`DrumReelView`) at 1×1 and `GONE` under the W/H tap-to-type boxes; `panel_finish_test.kt:124` asserts `filterIsInstance<DrumReelView>().size >= 2`. `TopBar.showViewMenu` uses a stock `PopupMenu`.
- Keys: `FxKeys.MIC_BLOCKED` exists; camera on/off is `fx.cam.on` (`FxKeys.CAMERA_ON` = `MicKeys.CAM_ON`); there is no camera block. `LookKeys.TITANIUM_FRAME`, `LookKeys.SAPPHIRE_GLASS`, `LookKeys.GLOW_ON`, `LookKeys.THEME_MODE` exist. The Profiles folder id is `presets`.
- The panel uses `performHapticFeedback` in 13 places and never `Vibrator`. The manifest removes Media3's `WAKE_LOCK` with `tools:node="remove"`.
- CI (`.github/workflows/build-debug-apk.yml`): secrets scan; raw-mic guard; cloud-speech guard (`TextToSpeech|SpeechRecognizer|android\.speech`); slider guard `SeekBar|RangeSlider|material\.slider|<Slider|Slider\(` across `04 Build/*/src`; merged-manifest permission allowlist (short names) that bans `ACCESS_NETWORK_STATE` and all Wi-Fi permissions including `CHANGE_WIFI_MULTICAST_STATE` and `NEARBY_WIFI_DEVICES`.
- Tests: 44 files in `app`, 5 in `live`. Emoji: `📁` on 17 lines in `PanelTree.kt`, 39 in `EffectFolders.kt`, 4 in `FxKeys.kt`, 1 each in `PanelActions.kt`, `TerminalPanel.kt`, `FolderLayout.kt`, `microphone_effects_folder.kt`; `EmojiRainLayer.DEFAULT_SET`; two `strings.xml` lines. The panel also uses the typographic symbols `⋮ ○ ● █ −`.

### 4.2 Hard limits (design around these; do not argue with them in PRs)

| Limit | Fact | Consequence in V2 |
|---|---|---|
| **GL version** | The shared EGL context is **OpenGL ES 2.0**. PBOs and `glMapBufferRange` are ES 3.0 | Screenshot is synchronous `glReadPixels` (§7.4). An ES 3.0 upgrade is an optional, separate PR with ES 2 fallback |
| **Callback semantics** | `addScreenRecordingCallback` reports whether **our activities** are in a recording. It returns the initial state synchronously. While OFFGRID is in the background during a Bigo share the state is `NOT_VISIBLE`; it flips to `VISIBLE` after we resume and draw | Chrome hidden in `onStop()` and on every `onStart()` until the synchronous value is read; ARMED persisted (§7.3). The callback cannot tell us Bigo is sharing while we are backgrounded; that is fine |
| **`FLAG_SECURE`** | A secure window's content is never captured, but SurfaceFlinger composes it on the non-secure projection output as an **opaque black rectangle** | The overlay (already secure) must **hide** under lock, not rely on `FLAG_SECURE`. `FLAG_SECURE` is never added to the activity or the panel: it would put a black box over the stage |
| **System bars** | Immersive mode uses `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`; a swipe from an edge reveals the status bar (with the Android 15 projection chip) and on One UI a two-finger top swipe opens the full quick panel, all on air | No edge gestures for lock, unlock, or hotkeys. Gestures live in the middle 60 percent of the stage. `setSystemGestureExclusionRects` (capped at 200 dp per edge) on any remaining hit zone (§7.3) |
| **MediaProjection and brightness** | The projection captures the composed frame, not panel brightness | The S25 panel can sit at **minimum brightness during a share** with no effect on the broadcast; the largest controllable heat source drops (§7.9) |
| **Screen lock** | Android 15 QPR1+ stops a projection when the screen locks. `keepScreenOn` holds only while OFFGRID is the top activity | Green Room check: screen timeout ≥ 10 min. Do not add `WAKE_LOCK` |
| **Multicast lock** | `MulticastLock.acquire()` needs `CHANGE_WIFI_MULTICAST_STATE`, banned by CI and removed by Lane C | Discovery runs without the lock; typed-address fallback (§7.5). No new network permission |
| **GL thread budget** | One GL thread does camera, deck, overlay latch, stage, recorder, replay, live outputs, remote preview encode | Rule for every PR touching the stage: per-stage `SystemClock` timers behind a debug flag; measure before adding a decoder; Phone 2 preview encode drops to every other frame when thermal headroom passes 0.8 (§7.8) |
| **Phone 2 cannot see the screen** | Phone 2 receives the GL stage (`RemotePreviewEncoder`), not the S25 screen. A second MediaProjection would need a system dialog and `FOREGROUND_SERVICE_MEDIA_PROJECTION`, both ruled out | "What Bigo sees" is renamed **Stage Monitor** with a chrome flag (§7.6) |
| **UNVERIFIED on Samsung** | Whether One UI's Smart View, Link to Windows, or DeX mirroring count as recording (safe false positive if so); whether Samsung's mDNS resolves without the lock (the manual path makes this a convenience issue); the callback with Bigo's projection on One UI | Lock Drill with Samsung's own screen recorder (§7.9) exercises the real MediaProjection path before Bigo |

### 4.3 Policy note (non-technical risk, accepted knowingly)

Nothing here violates Android, Play, or Samsung rules: `DETECT_SCREEN_RECORDING` is a normal permission, `SYSTEM_ALERT_WINDOW` is already granted, the app is sideloaded, and screen sharing another app through Game Live is a documented Bigo feature. The risk is Bigo's **content** rules: hosts broadcasting pre-recorded or looping video with no live presence have been treated as recorded-content violations on several live platforms, and Game Live is intended for games. Royalty decides how long the deck runs alone and keeps a live element (voice, status word, effects) in the loop. V2 proposes no technique to evade platform detection; virtualization and camera hooking remain rejected.

---

## 5. Lumen design system (approved, with corrections)

V1 §8 stands as the visual specification (tokens, signature hues, glow model, icon system, laser lettering, controls, top bar). The following corrections and rules override it.

### 5.1 Tokens and themes

Tokens per V1 §8.1 in `terminal/lumen_tokens.kt`. `canvas #000000`, `surface #07090A`, 1 dp hairline that never disappears, `accent.live #FF3B5C` reserved for Record, Broadcast Lock, mic block, camera block. **`Theme.SYSTEM` with Auto/Day/Night is untouched:** pure black panel in Night, pure white in Day. Lumen glow sits on top of either. Contrast at glow 0 percent is tested against both System themes (§10).

### 5.2 Glow engine (corrected)

- `GlowSpec(hue, intensity, halo, enabled)` resolves global × category × per-icon, in `LookKeys.GLOW_*` and `icon.<folderId>.glow.*`.
- **Rendering (changed from V1 D10):** each glyph is rendered once per `(glyphId, hue, intensity, halo, sizePx)` into a hardware layer with **`RenderEffect.createBlurEffect`** (API 31+; `minSdk` is 33) and cached in an LRU keyed by that tuple. `BlurMaskFilter` is the fallback for software canvases only. Hardware blur keeps the panel at 120 Hz and makes the 1.2 s breath on the active item free.
- Breath only on the active item; idle items ≤ 30 percent; panel auto-dims after 20 s idle (V1 D7). Under Broadcast Lock the panel is not drawn, so glow costs nothing on air.

### 5.3 Icon system

Per V1 §8.3: strip every OS emoji (the counts in §4.1), `terminal/glyph_paths.kt` on a 24-unit grid with closed outer contours, 12 packs, per-folder swap in the Glyph Drawer, Particle Rain replaces Emoji Rain with user text input off by default. The CI emoji guard carries an **allowlist** for `⋮ ○ ● █ −` and any glyph stand-in introduced before `glyph_paths.kt` lands.

### 5.4 Laser lettering baked into the folder shape

Per V1 §8.4: `FolderGlyphView` builds one `Path` (tab + body), title via `Paint.getTextPath` clipped to the tab band, stroked and glowed in one pass; `FolderIconView` archived. Acceptance: `titleBounds ⊂ tabBounds`.

### 5.5 Controls

- **`BioSliderView`** per V1 §8.5 (hairline track, glowing fill, 20 dp bead, value halo only while dragging, detents every 5 with `CLOCK_TICK`, double-tap reset, long-press keypad, `RangeInfo` for TalkBack). Allowed by CI under A2. Pref writes are applied in memory immediately and debounced to disk at 150 ms.
- **`GlowToggleView`** per V1 §8.5: knob travels left → right, state by position + fill + glyph, never glow alone. `TOGGLE_ON`/`TOGGLE_OFF` haptics with `CLOCK_TICK` fallback.
- **Reels:** `DrumReelView`/`SizeReelBank` only in `PANEL › Size & Position` and `STAGE › Canvas`. The header keeps the PR #38 W/H tap-to-type boxes. The two hidden reels inside `TopBar` stay `GONE` and zero-size until a dedicated cleanup PR removes them and updates `panel_finish_test.kt` in the same change.
- **R1 rule:** every control on the S25, including `BioSliderView`, `GlowToggleView`, the mic-block and camera-block toggles, and the top bar, is chrome and is **hidden under Broadcast Lock**. Phone 2 carries the same controls live.

### 5.6 Top bar (final layout, unlocked only)

`[back orb] [title] … [W box] [H box] [CAMERA] [MIC] [REC 00:00] [SCREENSHOT] [LOCK]`, each a Lumen glyph.
- Camera: tap = on/off; long-press = **camera block** (`FxKeys.CAMERA_BLOCKED`, red slash).
- Mic: tap = on/off; long-press = hard block (`FxKeys.MIC_BLOCKED`).
- REC: start/stop; elapsed time here only.
- Screenshot: one tap (§7.4).
- Lock: engages Broadcast Lock (MANUAL).
- **The whole top bar is hidden under lock.** The clicker "Sound style" option named "Metal" is renamed "Chime" when the Feedback folder is touched.

---

## 6. Folder information architecture (A3 approved)

The 12-folder tree in V1 §9.4, the grouping principle in §9.2, the 50-row item move list in §9.3, and the data model in §9.6 are approved and stand, with these corrections:

1. **Migration ids.** `Themes › Profiles` has folder id **`presets`** (title "Profiles"). `folder_id_migration.kt` maps `presets` → `sets`, plus `size` → `panel.size`, `display` → `panel.surface`, `live_remote` → `cast`, and the rest.
2. **Copy fix.** `sizeFolder()` notes still say "Width and Height are also on the drum wheels at the top of the panel"; the IA PR corrects the copy.
3. **Archive list (approved):** Titanium frame, Sapphire glass (`terminal/archived_looks.kt`, hidden, default off), Record length (`FxKeys.RECORD_SECONDS` kept, default 0), Clip id (replaced by the Library list). Old builders move to `terminal/archived_tree_v1.kt`. Nothing is deleted.
4. **New items required by V2:**
   - `BROADCAST & REMOTE › Broadcast Lock`: Lock now · Arm · Auto-lock on screen share (default on; cannot be turned off while VISIBLE) · Gesture help. No "Allow unlock during share" item (A4).
   - `BROADCAST & REMOTE › Phone 2`: role · pairing (shows **this phone's site-local address and port** next to the pairing key) · remote preview · trust codes · revoke.
   - `RECORD & CAPTURE › Recording`: save to gallery (move/link) · save last · segment length (30 min) · a computed line "about 2.7 GB per hour at the current quality".
   - `STAGE › Canvas`: output aspect preset · deck fit/fill (`FxKeys.AR_FIT`) · stage resolution lift (default off until the governor lands) · safe-zone guides (never on air).
   - `SYSTEM › Diagnostics`: test diamond (refuses to turn on under lock) · platform tests · success codes · results · **render probe** (debug flag).
   - `PANEL › Feedback`: clicker sounds with "Chime" in place of "Metal".
5. **Scope table (V1 §9.5) corrected for A4 and R1:**

| Area | S25 (Phone 1) | Phone 2 (master) |
|---|---|---|
| Broadcast Lock | Lock glyph (unlocked) · two-finger long-press 1.5 s in the center zone to lock, and to unlock **only when not VISIBLE** · Arm | Lock / Arm / unlock (hold-to-confirm 1 s; refused while VISIBLE, with the reason shown) |
| Mic block, camera block, REC, Screenshot, all sliders and toggles | Available when unlocked; **invisible during screen share** | Full, live |
| Video Deck | Full when unlocked; hidden under lock; Invisible Hotkeys for next / play-pause / last Moment | Full + import/send + thumbnails |
| Effects / Camera / Voice / Text / Look | When unlocked | Full |
| Sets | Recall | Save + recall |
| Pairing, revoke, trust codes, permissions, PC Output, Diagnostics, Reset all | **S25 only** | — |
| Platform Guides | S25 | Read-only mirror |
| Stage Monitor, Health strip, Green Room, Lock Drill | — | **Phone 2 only** |

Acceptance for the IA is in §10.

---

## 7. Engineering specs (handoff-ready, corrected)

### 7.1 Bug: stretched stage/lion on first launch

**Symptom (Royalty):** on first launch the stage/lion renders very wide and stretched; after switching apps and back it is normal.

**What cannot be the cause.** Output selection. `bindCamera()` puts both use cases in one `UseCaseGroup` with one `ViewPort(FILL_CENTER)`, so CameraX crops the preview and video outputs to the **same** aspect; `drawFinal()` paints the stage into every slot through the aspect-preserving `present()`; the camera is drawn with the chosen slot's own `updateTransformMatrix`. With the 1280×720 preview target and `Quality.HD`, both outputs are ~590×1280. `maxBy` can change which resolution and crop alignment the stage inherits, which is worth one line of hardening, but it cannot produce a non-uniform scale. **Bind timing** is weaker than V1 stated: `setDecorFitsSystemWindows(false)`, `hideSystemBars()`, and `shortEdges` make the `PreviewView` edge-to-edge from its first layout, and the permission-callback bind is invalidated by `bindGeneration` when `onResume()` emits `BIND_CAMERA`.

**Diagnosis, ranked:**

1. **`StagePainter.drawAr()` has no aspect math (certain, unconditional).** Every clip whose aspect differs from the stage is stretched on every launch. It cannot alone explain "normal after a switch", but it is the one certain defect.
2. **`PreviewView` in `COMPATIBLE` (TextureView) mode (fits the symptom).** A `TextureView` stretches its buffer to the view bounds until `PreviewView` applies its correction matrix from the `SurfaceRequest`'s transformation info. With a `SurfaceProcessor` the request is flagged as already transformed; if the correction lands late or against a stale size on the first bind, the first frames are stretched to the view and the next bind is clean. This is the only component in the chain that performs a non-uniform scale at all.
3. **`OverlayRenderer.request()` resize (cosmetic, one frame).** It calls `setDefaultBufferSize` on a stage size change; the first latched drawing after the change is drawn full-frame with the new stage.
4. **PREVIEW master (hardening).** Equal-area outputs resolve by arrival order; make the PREVIEW slot the stage's master so resolution and crop alignment are deterministic.
5. **Rebind on real aspect change (hardening).** Fold-in, split screen, and multi-window change the view aspect after bind.

**Step 0, the render probe (Phase 0.5, Fast, ship first).** Behind a debug flag, never on air, for the first 3 frames after each bind log: `PreviewView` size, `SurfaceRequest` resolution, each output size and targets, stage size, clip `VideoSize`, and the `TextureView` transform matrix. Royalty produces one device log for a first launch and one for a resume before 1.1 is designed in detail. Idea §8 (Geometry Attestation) later makes the probe permanent and silent.

**Fix (Phase 1.1, Top):**
- `ar_deck_player.kt`: `Player.Listener.onVideoSizeChanged(videoSize)` posts `(width × pixelWidthHeightRatio, height)` to the GL handler → `ArDeckSurface.videoW/H`, `hasSize`.
- New pure `effects/stage_geometry.kt`: `stageSize(previewW, previewH, maxSide)`, `fitRect(src, dst)`, `fillCrop(src, dst)`; every result has `sx == sy`.
- `StagePainter.drawAr()`: fit via `glViewport(sub-rect)`, fill via a centered crop scale multiplied into the texture matrix (`Matrix.scaleM` around 0.5). `FxKeys.AR_FIT`, default **fill**. No draw until `hasSize`.

**Fix (Phase 1.2, Fast):** in `paintStage()`, `presentCamera()`, and `onOutputSurface()`, `val main = slots.firstOrNull { it.surfaceOutput.targets and CameraEffect.PREVIEW != 0 } ?: slots.maxBy { it.width * it.height }`. Recorders `present()` into their own size with **fill**, not fit, so recordings have no bars.

**Fix (Phase 1.5, Top):** `previewView.addOnLayoutChangeListener`: if the aspect differs from the bound aspect by > 1 percent, `rebindCamera()` debounced 150 ms and generation-guarded; update `faces.outputAspect` on every rebind. Store `boundW/boundH`. Geometry Attestation numbers (`previewAspect`, `slotAspect`, `stageAspect`, `clipAspect`) go into the `State` packet; if `|previewAspect − slotAspect| > 1 percent` for more than three frames, exactly one automatic rebind.

**PreviewView check (inside 1.1 or 1.5, by probe result):** a Robolectric test asserts the `TextureView` transform has equal X and Y scale once the first frame is delivered; consider `PERFORMANCE` (SurfaceView) mode once Broadcast Lock lands, measured, never blind. `OverlayRenderer`: skip or redraw the first latched frame after a size change.

### 7.2 Pristine broadcast surface

Bigo captures the whole screen, so any View over `PreviewView` is on air.

| Remove from the surface | Where it is today | Change |
|---|---|---|
| Pill | `TerminalPanel.pill` (`FolderIconView` + `LaserTitleView(R.string.terminal_pill)`, string `OFFGRID`) | The pill view usage goes (code archived). The panel opens by a 1.0 s long-press in the top-left zone **with `setSystemGestureExclusionRects` on the zone**, the top-bar glyph, or Phone 2. Coach mark once, never while recording, locked, or ARMED |
| Status line | `control_bar › status_text`, `showMessage()` in `status_line.kt` (`camera_off`, `camera_starting`, `camera_ready`, `recording_now`, …) | `control_bar` is `GONE` permanently. Messages route to the panel status line and to Phone 2. Permission errors show in the panel only |
| "Record 5 seconds" / "Record 30 s" HUD (`record_five` at first draw, `record_seconds` after `setRecordLabel()`) | `record_button`, `play_button`, `action_button` in `control_bar` | Gone from the surface. Record moves to the top bar; "Play last" to `RECORD & CAPTURE › Recording` |
| Playback bar | `playback_bar` | A panel sheet, off stage |

**Stage-first boot via `ViewStub` (idea §8 Stage-first boot, part of Phase 1.4):** `activity_main.xml` keeps `PreviewView`, `DarkStage`, and a `ViewStub` for `TerminalPanel`, the top bar, and the HUD. The stub inflates after the first stage frame and only if not locked or ARMED. Before inflation the view tree has no chrome by construction, which makes `SurfaceCleanTest` trivial. `findViewById` calls move behind the inflate callback; `LiveIntegration.install` and `DarkStage` take the panel lazily.

### 7.3 Broadcast Lock (A1, A4, R1)

**Mechanism.** `WindowManager.addScreenRecordingCallback(Executor, Consumer<Integer>)` returns the initial state synchronously and calls back on change; `SCREEN_RECORDING_STATE_VISIBLE` means our activities are in a recording. `<uses-permission android:name="android.permission.DETECT_SCREEN_RECORDING"/>`, normal, API 35; CI allowlist entry approved (A1). Register in `onStart`, remove in `onStop`, guarded by `Build.VERSION.SDK_INT >= 35`. On API 33/34 the manual, remote, and ARMED paths work without it.

**State machine (`broadcast_lock_store.kt`, new, persisted, observable):**

```
sources:  AUTO    (callback VISIBLE, autoLockEnabled is always true)
          MANUAL  (S25 Lock glyph, center two-finger long-press 1.5 s)
          REMOTE  (Phone 2 lock_on)
          ARMED   (set from Phone 2 or the top bar before opening Bigo; persisted)
locked  = recordingVisible || manualLocked || remoteLocked || armed
unlock  = allowed only when recordingVisible == false (A4: never during a share)
          clears MANUAL + REMOTE + ARMED; AUTO clears itself when the callback reports NOT_VISIBLE
          S25: center two-finger long-press 1.5 s  ·  Phone 2: hold-to-confirm 1 s (refused while VISIBLE, reason shown)
persist: manualLocked, remoteLocked, armed, wasLockedWhenBackgrounded
```

**Resume gate (every `onStart`, not only cold start):**
1. `onStop()`: hide every chrome view (panel, top bar, HUD, overlay); persist `wasLockedWhenBackgrounded = locked`.
2. `onCreate()`: chrome is not inflated (`ViewStub`) or `INVISIBLE`; the stage draws first.
3. `onStart()`: `val state = windowManager.addScreenRecordingCallback(mainExecutor, cb)` (API ≥ 35). If `state == VISIBLE` or `armed` or `wasLockedWhenBackgrounded` or any persisted lock → **locked**: nothing but the stage is drawn; the deck auto-plays the last clip in loop if Deck is on; effects keep running.
4. Only if `NOT_VISIBLE` and no lock source → reveal chrome with a 120 ms fade after the first stage frame.
5. Callback flips to `VISIBLE` at any time → lock on the same frame, no animation.
6. The synchronous return value is the key: it is available before the first frame, so the callback's latency never matters; ARMED makes even that irrelevant on a planned stream.

**Gestures (non-edge, A4):**
- Lock and unlock: **two-finger long-press 1.5 s anywhere in the middle 60 percent of the stage**, with a haptic ramp. Unlock is refused (haptic `REJECT`, nothing drawn) while VISIBLE.
- Under lock the stage **consumes and discards all single-finger input**: no ripple, no focus ring, no `PopupMenu`, no view change, no system-bar reveal. This also protects against pocket and palm touches.
- **Invisible Hotkeys (Phase 1.8):** three-finger tap = next clip; three-finger long-press = fire the last Moment; four-finger tap = deck play/pause. Each has a distinct `HapticGrammar` pattern and draws nothing. A Phone 2 toggle disables them. Pure `lock_gesture_map.kt`, unit-tested.
- Panel-open long-press (unlocked only) and any remaining hit zone get `setSystemGestureExclusionRects`.

**Also hidden under lock:** the floating overlay (`live/OverlayWindowController.kt`, `LiveOverlayController.kt`). It already carries `FLAG_SECURE`, so its content is not captured, but it composes as an **opaque black box** on the broadcast; hiding it is the fix. **Never add `FLAG_SECURE` to the activity or panel.** The overlay stays for non-broadcast use (mandate 20).

**R1:** mic-block, camera-block, REC, Screenshot, and every slider and toggle are chrome and are hidden under lock. Phone 2 carries `rec_start`, `rec_stop`, `shot`, and the `fx.mic.blocked` / `fx.cam.blocked` settings live.

**Absorb the existing clean mode:** `LiveActions.CLEAN_OUTPUT` becomes `BroadcastLockStore.lock(MANUAL)`; one source of truth.

**Files:** new `broadcast_lock_store.kt`, new `lock_gesture_map.kt`, `MainActivity.kt`, `TerminalPanel.kt`, `TopBar.kt`, `LiveIntegration.kt`, `live/RemoteMessage.kt` (allowlist `lock_on`, `lock_off`, `arm`), `AndroidManifest.xml`, workflow (A1).

### 7.4 Unlimited recording, segments, screenshot, camera block

- **Storage arithmetic, shown.** `VIDEO_BIT_RATE 6_000_000` + AAC 128 kbps ≈ **2.7 GB per hour**; five hours ≈ 13.5 GB. The Recording folder computes and shows "about N GB per hour" from the real bit rates. The 2 GB free-space floor is a stop, not a plan: `StatFs` every 10 s; at the floor, finalize the current segment (playable), stop, and tell the panel and Phone 2 why.
- **Gallery policy.** `GallerySaver` copies to `Movies/OFFGRID` today. With segments on, "Save to gallery" **moves or links**, never duplicates 13 GB. Default off while segments are on.
- **Segments at the next keyframe.** `KEY_I_FRAME_INTERVAL = 1` already yields a keyframe every second, so no sync-frame request is needed. `Mp4Writer.rollover()` (not a second recorder) starts a new `MediaMuxer` at the 30-minute mark's next keyframe, or on demand from Phone 2 ("cut here"). Audio continuity: align the audio cut to the last AAC frame (1024 samples ≈ 21 ms) before the video keyframe and carry codec config into the new file. Names `OFFGRID_yyyyMMdd_HHmmss_partNN.mp4`; a `session.txt` sidecar per session (start, duration, size, sha256 per segment) so a corrupt file never hides the others.
- **Unlimited by default.** `FxKeys.RECORD_SECONDS` default **0**; the `NumberItem` is archived from the tree; the existing `recordSeconds() > 0` stop timer stays dormant.
- **Top bar.** REC (`accent.live`, elapsed `mm:ss`/`h:mm:ss`), Screenshot, Lock, next to Camera and Mic. Hidden under lock (R1).
- **Screenshot (ES 2.0).** On the GL thread after `paintStage`, **synchronous `glReadPixels`** from the stage `GlTarget` (not the window surface), once per tap: ~3 MB at ~590×1280, a few milliseconds, one dropped frame for a deliberate action. PNG encoded off the GL thread; saved via `MediaStore` to `Pictures/OFFGRID`. The panel is never drawn into the stage, so the PNG contains no chrome. No PBO. (Optional separate PR: ES 3.0 context with `EGL_OPENGL_ES3_BIT`, ES 2 fallback.)
- **Camera block.** New `FxKeys.CAMERA_BLOCKED` (`fx.cam.blocked`) mirrors `MIC_BLOCKED`; `CaptureSwitch.update()` treats blocked as "camera not wanted" and refuses to bind. Long-press the Camera toggle; Phone 2 sets it as a setting. Invisible under lock (R1).
- **Do not add `WAKE_LOCK`.**
- **Files:** `recording_controls.kt`, `audio/AvRecorder.kt`, `audio/Mp4Writer.kt`, `audio/CaptureSwitch.kt`, `TopBar.kt`, `FxKeys.kt`, `GallerySaver.kt`, new `screenshot_capture.kt`.

### 7.5 Phone 2 link bring-up (Phase 0.4, blocks Phase 3 and the remote lock path)

- **Problem.** `LocalNetwork.discover()` acquires a `MulticastLock` first; that needs `CHANGE_WIFI_MULTICAST_STATE`, which CI bans and Lane C removed. The `SecurityException` is caught and discovery never starts. `RemotePreviewActivity.findCamera()` has no other path.
- **Fix, no new permission.** Drop the `MulticastLock` and call `NsdManager.discoverServices` directly; platform NSD runs in the system mDNS stack. Add the always-works fallback: the S25's Pairing folder shows `siteLocalAddress()` and the port next to the pairing key; Phone 2's controller accepts a typed address and port. `LocalNetwork` keeps refusing non-site-local addresses.
- **Acceptance.** Robolectric: discovery proceeds with no `CHANGE_WIFI_MULTICAST_STATE` in the merged manifest. Device: Phone 2 connects on the real Wi-Fi by discovery, and by typed address with discovery disabled.
- **Files:** `live/LocalNetwork.kt`, `live/RemotePreviewActivity.kt`, `live/PairingActivity.kt`, `EffectFolders.liveRemote()`.

### 7.6 Phone 2 master protocol

**Today:** `RemoteMessage(key, type, value)`; `fire` allowlist or `setting` with an `fx.*` key; fields ≤ 256 chars, frames ≤ 1024 B; `RemoteSession` `Video` packets ≤ 16 MiB inside `EncryptedChannel`.

**Additions:**

| Packet / message | Direction | Content | Limits |
|---|---|---|---|
| `fire`: existing + `lock_on`, `lock_off`, `arm`, `rec_start`, `rec_stop`, `rec_cut`, `shot`, `ar_prev`, `ar_reverse`, `ar_crossfade`, `set_recall`, `hotkeys_on`, `hotkeys_off` | P2 → P1 | existing format | allowlist |
| `setting`: `fx.*` (incl. `fx.mic.blocked`, `fx.cam.blocked`, `fx.deck.*`, `fx.glow.*`), `look.*` via a new prefix rule | P2 → P1 | existing format | **explicit per-key allowlist with type and range checks** (Rex #4), not prefix-only |
| `RemotePacket.TreeManifest` | P1 → P2 | ids, titles, iconIds, item types, ranges (JSON, gzip) | ≤ 256 KiB, once per pairing and on change |
| `RemotePacket.State` | P1 → P2 | `locked`, `armed`, `recordingVisible`, `panelVisible`, `topBarVisible`, `controlBarVisible`, `overlayVisible`, `recording`, `recElapsed`, `freeBytes`, `fps`, `thermalHeadroom`, `battery`, deck clip/pos, `previewAspect`, `slotAspect`, `stageAspect`, `clipAspect` | 1 Hz, ≤ 1 KiB |
| `RemotePacket.FileOffer` | P2 → P1 | fileId, name, size, mime, sha256 | mp4/webm/mkv, ≤ 512 MiB |
| `RemotePacket.FileChunk` | P2 → P1 | fileId, offset, bytes | 256 KiB |
| `RemotePacket.FileAck` | P1 → P2 | nextOffset / done / rejected(reason) | resumable |
| `RemotePacket.Thumb` | P1 → P2 | clipId, JPEG 160 px | ≤ 24 KiB |

- **Scheduler:** one writer, priority `control > state > thumb > chunk`, at most one chunk in flight before re-checking control. The budget is stated and measured: a `fire` round trip under **50 ms** with a 300 MB transfer in flight on the test LAN (one 256 KiB chunk is ≈ 13 ms at 20 MB/s).
- **Resilience (Rex #5):** `EncryptedChannel` kills the session on any counter desync. Add reconnect-and-resume: re-pair from the stored key, resume the transfer at the acknowledged offset, never replay a frame. Extend `RemoteSecurityTest`.
- **Storage:** `filesDir/deck/incoming/<fileId>.part` → hash verify → `filesDir/deck/<clipId>.mp4` → `ArDeckCatalog` refresh → thumbnail back. Rejected files go to `deck/rejected/`, never deleted. No storage permission.
- **Stage Monitor (renamed from "What Bigo sees").** The existing H.264 path (`RemotePreviewEncoder` → `RemotePreviewDecoder`) carries the GL stage edge-to-edge on Phone 2. A **red frame plus a laser-lettered word** appears the moment any chrome flag in `State` is true while `recordingVisible`; a soft pulse when `thermalHeadroom > 0.8` or `freeBytes < 4 GB`. It tells Royalty *why* the frame is red. Preview encode drops to every other frame when thermal headroom passes 0.8.
- **Quiet Hands.** A Phone 2 layout that is true black with hairline zone borders at 10 percent glow; six large zones for Sets and Moments; every zone confirms by a distinct haptic; a long-press reveals labels for two seconds. No light on Royalty's face, half the Phone 2 screen power over five hours.
- **Health strip, gestures, haptics on Phone 2** per V1 Lab #5, #7, #13.
- **Files:** `live/RemoteSession.kt`, `live/RemoteMessage.kt`, new `live/file_transfer.kt`, new `live/tree_manifest.kt`, `live/RemotePreviewActivity.kt` → `live/remote_controller_activity.kt` + fragments, `ar_deck_catalog.kt`.

### 7.7 AR Video Deck: loop, crossfade, reverse, skip, guards

- **Loop:** keep `REPEAT_MODE_ONE`. `experimentalSetEnableMediaCodecVideoRendererPrewarming(true)` has existed since Media3 1.5 and the project is on 1.11.1; no try/catch guard. It helps media-item transitions; whether it tightens the repeat seam is a device question, which Seam Weld makes moot.
- **Seam Weld (GL-side loop crossfade, no proxy files).** Keep a ring of the last N stage-resolution frames of the clip (N = 8 at 30 fps ≈ 266 ms) in `GlTarget`s, the mechanism `StagePainter` already uses for echoes (`echoes[]`, `copy()`, `ECHO_SHRINK`). On `onPositionDiscontinuity(DISCONTINUITY_REASON_AUTO_TRANSITION)` draw the ring forward over the first N incoming frames with alpha 1 → 0. Every seam becomes a 266 ms dissolve from tail into head, for any clip, with no import step. Cost ≈ 24 MB VRAM at today's stage size (6 MB at half res), one extra blit per frame during the window. `FxKeys.AR_WELD_MS` (0 = off). Combined with the auto-trim loop finder (V1 Lab #6) the seam is invisible.
- **Skip:** playlist from `ArDeckCatalog` order; `ar_next`/`ar_prev` → `seekToNextMediaItem()`/`seekToPreviousMediaItem()`.
- **Crossfade:** two players (A/B), two `ArDeckSurface`s, `alphaA = 1 − t`, `alphaB = t` over `crossfadeMs` (default 600). Release the idle decoder ≤ 100 ms after the fade. Measure the GL budget first (§7.8).
- **Reverse (corrected):** ExoPlayer cannot play backwards. Build `deck/<clipId>_rev.mp4` on a **worker with its own EGL context or the YUV `ByteBuffer` path**, never on the compositor thread. Bound memory: YUV output (3 MB per 1080p frame), a **720p proxy**, GOP-sized batches, or two passes (all-intra re-encode, then reverse by seeking). OFFGRID's own recordings already have 1 s GOPs and are cheap to reverse. Clips ≤ 60 s for v1. Storage ≈ 2× per clip; deck budget 4 GB with LRU **archive** of proxies. Proxies live in `filesDir/deck/`.
- **Hold Frame and Black Guard.** (1) When a clip ends without loop or a transfer swaps clips, keep the last decoded frame (`ArDeckSurface.hasFrame` already preserves it) instead of dropping to the curtain. (2) If the deck is on but `hasFrame` is false for more than 500 ms, raise a Phone 2 flag instead of letting the idle curtain fall over a live room. Derive "is anything active" from `StagePlan` plus deck state instead of the hand-written list in `idleCurtainColor()` (Rex #3); Sets must not fight it.
- **Files:** `ar_deck_player.kt` (→ `ArDeckPlayerPair`), `ar_deck_surface.kt`, `ar_deck_catalog.kt`, new `ar_deck_reverser.kt`, `CompositorProcessor.kt`, `StagePainter.kt`.

### 7.8 Stage resolution and the GL thread budget

- **Lift (Phase 1.9, behind a setting, default off).** Raise `previewResolution()` to 1920×1080 so the stage becomes ~886×1920; keep `RECORD_LONG_SIDE` at 1280 for the file. Laser words, the status label, and glow edges on the stage become sharp on the broadcast (today the ~590×1280 stage is upscaled ~2.4× to the 1440×3120 panel). Raising `MAX_STAGE_SIDE` alone changes nothing. Re-check overlay, recorder, replay, and segmenter budgets; the thermal governor drops the target back to 720p when headroom passes 0.9.
- **Budget rule for every PR that touches the stage:** per-stage `SystemClock` timers behind a debug flag; measure before adding a second decoder; skip the replay draw while recording if needed.

### 7.9 Thermal, battery, Green Room, Lock Drill

- **Heat sources in order:** Bigo's own screen-capture encoder (outside our control), the OLED at high brightness, our H.264 recorder, the camera, the GL stage, the MediaPipe segmenter, the Phone 2 preview encoder.
- **Two cheap wins:** (1) **S25 brightness at minimum during a share**: MediaProjection captures the composed frame, not panel brightness, so the broadcast is unaffected. (2) **Cable charging, not wireless**, during long streams. Both are Green Room lines.
- **Governor (Phase 5, Top):** `PowerManager.getThermalHeadroom` and `PerformanceHintManager` (ADPF), both API 33+ and unused today. Step-down order: glow halo → Phone 2 preview cadence → second decoder → segmenter rate → stage resolution target.
- **Green Room (Phase 3.6):** lock armed, mic not blocked, battery > 30 percent, free space > 10 GB (with the per-hour cost shown), thermal OK, deck clip loaded, Bigo pinned, screen timeout ≥ 10 min, brightness minimum, cable connected.
- **Lock Drill:** "Start Samsung's screen recorder from Quick Settings; OFFGRID must lock within one second and Phone 2 must show LIVE; stop it; OFFGRID returns to ARMED." Samsung's recorder uses MediaProjection, so this exercises the real Android 15 path without Bigo, in ten seconds, before every stream.

---

## 8. Innovation Lab (merged and re-ranked)

V1's 15 Lab ideas stand. The review's 11 are added. Rankings below are the approved order; each idea is anchored to code at `be674e1`.

| Rank | Idea | Source | Phase (per §9) |
|---|---|---|---|
| 1 | **Armed State + Lock Drill**: two-stage lock, resume race closed by design, provable on demand with Samsung's recorder | Review §4.3 | 1.3, 3.6 |
| 2 | **Seam Weld**: GL ring crossfade at the repeat discontinuity, no proxy files | Review §4.1 | 3.4 |
| 3 | **Stage Monitor with a Chrome Flag**: the buildable "What Bigo sees" | Review §4.5 (replaces V1 #2) | 3.3 |
| 4 | **Sets**: instant scene recall with a 300 ms crossfade | V1 #1 | 4 |
| 5 | **Geometry Attestation**: the app checks its own aspect numbers and heals with one guarded rebind | Review §4.4 | 1.5 |
| 6 | **Invisible Hotkeys**: multi-finger, non-edge, nothing drawn, haptic confirmation | Review §4.2 | 1.8 |
| 7 | **Green Room** with brightness, cable, and Lock Drill lines | V1 #11 + review §2.4 | 3.6 |
| 8 | **Thermal governor** | V1 #12 | 5 |
| 9 | **Stage resolution lift** with the governor as counterweight | Review §4.9 | 1.9 |
| 10 | **One-second segment rollover** + `session.txt` + "cut here" | Review §4.8 | 1.6 |
| 11 | **Quiet Hands** Phone 2 layout | Review §4.6 | 3.3 |
| 12 | **Smart loop auto-trim** at import | V1 #6 | 3 |
| 13 | **Hardware glow cache** (`RenderEffect`) | Review §4.10 | 2.1 |
| 14 | **Deck Hold Frame / Black Guard** | Review §4.11 | 3.4 (acceptance "No black frame") |
| 15 | **Stage-first boot via `ViewStub`** | Review §4.7 | 1.4 |
| 16 | Beat-synced glow | V1 #3 | 4 |
| 17 | Ambient color match | V1 #4 | 4 |
| 18 | Haptic confirmations on Phone 2 | V1 #5 | 3.3 |
| 19 | Gesture cues from Phone 2 | V1 #7 | 3.3 |
| 20 | Voice Character pad | V1 #9 | 4 |
| 21 | Lumen Ring (radial quick menu, unlocked only, center zone) | V1 #10 | 4 |
| 22 | Curtain call | V1 #13 | 4 |
| 23 | Clip drop zones on Phone 2 | V1 #14 | 4 |
| 24 | Glow Studio generator | V1 #15 | 2 |
| 25 | Voice cues on Phone 2 (cloud only) | V1 #8 | 5, **A5 open** |

**Acceptance pattern for every Lab item** (filled in on the PR before coding): a one-sentence user-visible behavior; a unit test for the pure logic; no new permission or an explicit approval link; no pixel added to the S25 stage while locked (`SurfaceCleanTest`); ≤ 1 ms added on the GL thread, measured.

---

## 9. Build order and model tier (adopted exactly from `docs/ARCHITECT_REVIEW.md` §5)

Difficulty is stated by files touched, invasiveness, and risk, not in calendar time. "Fast model" means a default coding model is sufficient with the acceptance tests as guardrails. "Top model" means multi-file GL, CameraX, lifecycle, media, or crypto reasoning where a wrong guess is costly. Rows 0.1 and 0.2 are **approved** (A1, A2); the agent's task is to make the change as written.

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

*Section references inside the tables (§2.x, §3.x, §4.x) point to `docs/ARCHITECT_REVIEW.md`; the same material is restated in §7 of this document. Hold Frame / Black Guard (§8 rank 14) ships inside row 3.4 under the "No black frame" acceptance criterion; the smart loop auto-trim (V1 Lab #6) rides with 3.4 as well.*

### 9.1 Agent and model assignments

Rex Standing Rule 1: research current agent and model options at kickoff and pick per task. Rule 2: OFFGRID builds in Claude Code always run in **Ultracode** mode. Tier mapping: **Top** → Claude Code in Ultracode, top reasoning tier; **Fast** → the default coding agent (Copilot coding agent or equivalent) on its own lane branch; research and docs → the research subagent; PR review gate → a *different* agent than the author, top tier. Rex re-confirms availability on the day of kickoff.

---

## 10. Acceptance criteria (complete)

Criteria carried from V1 are kept unless corrected here; corrected or added criteria are marked **(V2)**.

### Phase 0

- [ ] **(V2)** The merged-manifest allowlist contains `DETECT_SCREEN_RECORDING` with the approval comment; the step fails if the permission is missing from the manifest once 1.3 lands (guard both ways).
- [ ] **(V2)** The slider guard bans `SeekBar|RangeSlider|material\.slider` and allows `BioSliderView`; a test commit with a `SeekBar` fails CI.
- [ ] **(V2)** The emoji guard runs in warn mode with the allowlist `⋮ ○ ● █ −`; `○●█` in `PanelTree.kt`/`EffectFolders.kt` do not trip it.
- [ ] **(V2)** Discovery starts with no `CHANGE_WIFI_MULTICAST_STATE` in the merged manifest (Robolectric); Phone 2 connects by discovery and by typed address on the device.
- [ ] **(V2)** Probe log present on the device, with the five numbers for a first launch and for a resume, before 1.1 is merged.

### Render fix

- [ ] **(V2)** `StageGeometryTest`: `sx == sy` for every fit/fill result; a 1920×1080 clip on a 1080×2340 stage gives visible U range ≈ 0.26 centered with fill, and a full-width 608 px tall rect with fit.
- [ ] **(V2)** `StageGeometryTest`: two outputs of equal size in both arrival orders → the PREVIEW slot wins.
- [ ] **(V2)** Robolectric: `PreviewView`'s `TextureView` transform matrix has equal X and Y scale once the first frame is delivered.
- [ ] **(V2)** `OverlayRenderer`: after a stage size change, the first latched overlay frame is skipped or redrawn at the new size.
- [ ] `BindTimingTest`: a layout change from 1440×2900 to 1440×3120 triggers exactly one rebind.
- [ ] Royalty's end-of-build check: 10 cold starts (including first install with the permission dialog) show an undistorted stage and clip.

### Pristine surface

- [ ] `SurfaceCleanTest`: after `onCreate` and the first frame, no visible View except `PreviewView`, `DarkStage`, and (if unlocked) the inflated panel/top bar; `control_bar.visibility == GONE` in every `ButtonMode`.
- [ ] No visible TextView on the stage contains "OFFGRID", "Record", or "Camera is off".
- [ ] Locked or ARMED: **only** `PreviewView`/`DarkStage` are visible; the `ViewStub` is not inflated.

### Broadcast Lock

- [ ] `BroadcastLockStoreTest`: all combinations of (auto, manual, remote, armed) → expected `locked`; **(V2)** unlock is refused while `recordingVisible` (A4); `lock_off` clears MANUAL, REMOTE, and ARMED only.
- [ ] **(V2) Resume gate:** with ARMED persisted or the callback returning VISIBLE, no chrome view is ever VISIBLE on any frame after `onStart()`, including resume from recents (Robolectric: `onStop()` → `onStart()` with the shadow returning VISIBLE).
- [ ] **(V2) Touch sink:** while locked, single-finger tap, drag, and long-press on the stage produce no view change, no ripple, no `PopupMenu`, no system-bar reveal; gesture exclusion rects are set.
- [ ] **(V2) Non-edge gestures:** the lock/unlock recognizer rejects any gesture whose start point lies outside the middle 60 percent of the stage.
- [ ] **(V2) Callback symmetry:** `addScreenRecordingCallback` in `onStart`, `removeScreenRecordingCallback` in `onStop`, guarded by `SDK_INT >= 35`; on API 33/34 manual, remote, and ARMED paths work (Robolectric SDK 33 run).
- [ ] **(V2) Chrome flag parity:** `State.panelVisible` equals `TerminalPanel.isShown` on every send.
- [ ] **(V2) R1:** under lock, the top bar (Camera, Mic, REC, Screenshot, Lock), every `BioSliderView`, and every `GlowToggleView` are not visible; the same settings are reachable from Phone 2.
- [ ] Phone 2 `lock_on` hides the S25 panel within 1 frame of receipt.
- [ ] **(V2) Lock Drill** documented; expected: locked within 1 s of Samsung's recorder starting, Phone 2 shows LIVE, stopping returns to ARMED.
- [ ] Royalty's end-of-build test with Bigo Game Live: open OFFGRID during the share → no folders visible on the broadcast, ever.

### Recording and screenshot

- [ ] **(V2)** The Recording folder shows "about 2.7 GB per hour" computed from the real bit rates.
- [ ] `RecordingSegmentsTest`: simulated 95 min → 4 files (30/30/30/5); **(V2)** the audio cut lands within one AAC frame (1024 samples) of the video keyframe; the next file starts with codec config; total duration across segments equals the recording duration ± 1 frame.
- [ ] **(V2) Gallery policy:** with segments on, "Save to gallery" moves or links, never duplicates.
- [ ] **(V2) Low storage:** at the floor the current segment is finalized (playable) before stopping; Phone 2 receives the reason.
- [ ] Unlimited by default: fresh install has `RECORD_SECONDS == 0` and no auto-stop runnable posted.
- [ ] **(V2) Screenshot:** readback comes from the stage FBO, not the window surface; PNG size equals the stage size; no panel pixels; the GL thread stall is measured and under 10 ms at the current stage size; no PBO or ES 3.0 call on the ES 2.0 context.
- [ ] `CameraBlockTest`: while blocked, `BIND_CAMERA` is never emitted.

### Deck

- [ ] `DeckLoopTest`: `REPEAT_MODE_ONE` set; no manual `seekTo(0)`.
- [ ] **(V2) Seam Weld:** at the repeat discontinuity, the blend window is N frames, alpha monotone 1 → 0, and the ring never samples a frame newer than the seam.
- [ ] `CrossfadeTest`: alpha curve sums to 1 ± 0.01; idle decoder released ≤ 100 ms after the fade.
- [ ] **(V2) No black frame:** with the deck on and a clip loaded, the stage never shows the idle curtain for more than one frame during loop, skip, or crossfade (`idleCurtainColor() == null` across a scripted transition).
- [ ] **(V2) Reverse proxy:** built off the compositor thread (GL handler queue depth stays under 2 during a build), peak memory under 300 MB for a 60 s 1080p source, output playable in `ArDeckPlayer`; `ReverserTest` on a 30-frame synthetic clip.

### Phone 2 link, protocol, transfer

- [ ] `FileTransferTest`: 200 MiB over a loopback pair with a forced disconnect at 37 percent resumes at the right offset; sha256 matches; wrong hash → rejected and the `.part` is archived to `deck/rejected/`.
- [ ] **(V2) Head-of-line bound:** with a 300 MB transfer in flight, a `fire` round trip is under 50 ms on the test LAN.
- [ ] **(V2) Counter desync recovery:** kill the socket mid-transfer; the session re-pairs from the stored key, the transfer resumes at the acknowledged offset, no frame is replayed (extend `RemoteSecurityTest`).
- [ ] `RemoteSecurityTest`: unknown packet types rejected; `FileChunk` without an accepted `Offer` rejected; non-video MIME rejected; **(V2)** a `setting` with an unknown key or an out-of-range value is rejected.
- [ ] Tree parity: the Phone 2 tree equals the S25 tree (ids, order).
- [ ] No new permissions for Phase 3 (CI allowlist unchanged after 0.1).

### IA and Lumen

- [ ] `TreeInvariantsTest`: every setting key in the v1 tree appears exactly once in the v2 tree or the archive list; depth ≤ 3; no group > 9 items; no title contains an emoji code point outside the allowlist; every folder has a `category` and an `iconId`.
- [ ] `FolderIdMigrationTest`: all old ids map, **(V2)** including `presets` → `sets`; unknown ids fall back to HOME.
- [ ] Test diamond cannot turn on while `BroadcastLockStore.locked == true`.
- [ ] Old prefs file loads without loss (Robolectric fixture from `be674e1` defaults).
- [ ] **(V2) Header criterion:** every `DrumReelView` in the top bar has `visibility == GONE` and zero size (no test change), or the reels are removed with `panel_finish_test.kt` updated in the same PR.
- [ ] **(V2)** `sizeFolder()` notes no longer mention drum wheels at the top of the panel.
- [ ] `FolderGlyphView.titleBounds ⊂ tabBounds`; long-title abbreviation test.
- [ ] Contrast: every ink/glow combination ≥ 4.5:1; **(V2)** tested at glow 0 percent against both System themes (pure black Night, pure white Day).
- [ ] Glow cache: draw-count test shows one blur per `(glyphId, hue, intensity, halo, sizePx)`.
- [ ] Slider a11y `RangeInfo` test; **(V2)** pref writes debounced (one disk write per 150 ms of dragging).
- [ ] **(V2) Every haptic goes through `HapticGrammar`** (replaces "no legacy `Vibrator` calls", which is already true).
- [ ] **(V2) No metal anywhere:** a test scans folder titles, item titles, choice labels, and notes for "titanium", "steel", "metal", "chrome", "sapphire" (case-insensitive) and fails on a match outside the archive.

### Process

- [ ] Every PR against `final-build` links the V2 section and the review section it implements, lists the model tier used, and attaches the CI run. Royalty merges; `main` is never touched.

---

## 11. Master test plan

| Layer | What | Where | Gate |
|---|---|---|---|
| Unit (JVM) | `StageGeometryTest`, `BroadcastLockStoreTest`, `LockGestureMapTest`, `TreeInvariantsTest`, `FolderIdMigrationTest`, `RecordingSegmentsTest`, `FileTransferTest`, `PriorityTest`, `CrossfadeTest`, `SeamWeldTest`, `ReverserTest`, contrast tests, no-metal scan, existing 44 + 5 test files | CI `testDebugUnitTest` | Must pass |
| Robolectric | `SurfaceCleanTest`, `ResumeGateTest` (SDK 35 shadow and SDK 33 run), `BindTimingTest`, `CameraBlockTest`, `PreviewViewTransformTest`, `DiscoveryWithoutMulticastTest`, slider a11y | CI | Must pass |
| Static guards | Permission allowlist (+`DETECT_SCREEN_RECORDING`), slider guard (revised, `BioSliderView` only), emoji guard with allowlist, raw-mic and cloud-speech guards, secrets scan | CI | Must pass |
| Lint | `lintDebug` | CI | Must pass |
| Emulator (optional, A6) | Cold-start screenshot at API 35, no panel pixels when the recording shadow is VISIBLE | CI optional | Advisory |
| Device (Royalty, end of build) | Probe logs (first launch + resume); 10 cold starts; Lock Drill; Bigo Game Live share with OFFGRID opened mid-share; 2-hour stream with segments; Phone 2 connect by discovery and typed address; 300 MB transfer with a `fire` round trip measured; crossfade and reverse; lock from both phones, unlock refused while VISIBLE | S25 Ultra + Phone 2 | Royalty's sign-off |
| Soak | 6 h stream with deck loop + recording at minimum brightness on cable; fps, thermal, memory, free space | Royalty, Phase 5 | Royalty's sign-off |

Standing rule: no phone tests between rounds. Rex and QA judge from code and CI; Royalty tests at the end of each phase.

---

## 12. Process

- **Cadence:** weekly research sweep → `docs/council/yyyy_mm_dd_research_sweep.md` (V1 named `handoff/20 OFFGRID Council/research/`, which does not exist; `handoff/` holds only `qa/`); bi-weekly council MD with debates logged; monthly design review against Lumen tokens.
- **Gates for every change:** Plan (Rex task card: goal, files, acceptance, tests, tier) → Build (own lane branch, underscores, no deletions, PR against `final-build`) → Prove (CI green plus real test output; "passed" never claimed without CI) → Review (a different agent) → Approve (**Royalty alone merges**) → Learn (post-merge note on model, cost, outcome; capability grid updated only with Royalty's approval; the grid lives under `docs/council/` until a rules folder exists).
- **Guardrails that never relax:** no virtualization or camera hooking, no root, no new permission without approval (only `DETECT_SCREEN_RECORDING` is approved), no on-device AI beyond MediaPipe, no secrets in Git, AGP 8.13.2 unchanged, no `WAKE_LOCK`, no `FLAG_SECURE` on the activity or panel, no edge gestures.

---

## 13. Open items and remaining approvals

| Item | Status | Mitigation |
|---|---|---|
| Samsung One UI behavior of the recording callback with Bigo's projection | UNVERIFIED | Lock Drill with Samsung's own recorder before every stream; ARMED makes the callback's timing irrelevant on a planned stream |
| Smart View / Link to Windows / DeX counted as recording | UNVERIFIED | Safe false positive (locks) |
| Samsung mDNS without the multicast lock | UNVERIFIED | Typed-address fallback always works |
| Overlay windows counted by the callback | UNVERIFIED | Hidden under lock regardless |
| Prewarming effect on the repeat seam | Device question | Seam Weld removes the dependence |
| A5 voice cues on Phone 2 (cloud API, Keystore key) | Open | Phase 5 only with approval |
| A6 optional emulator CI job | Open | Advisory job, Phase 5 |
| Real-user card sort of the 12-folder tree | Not done | Optional 15-person sort via Royalty's audience after Phase 2 |

---

*V2 is APPROVED as the build document for OFFGRID-Compositor. V1 remains the council record. Every PR cites its V2 section; Royalty alone merges; `main` is never touched.*
