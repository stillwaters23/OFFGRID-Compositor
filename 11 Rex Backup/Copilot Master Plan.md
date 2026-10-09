# Copilot Master Plan

**Project:** OFFGRID Compositor (private repo `stillwaters23/OFFGRID-Compositor`)
**Owner:** Royalty (GitHub `stillwaters23`)
**Written by:** Grok Bot for Royalty, Friday Oct 9, 2026 (America/Chicago)
**Base branch for all work:** `final-build` (pull request #8)
**Who reads this:** GitHub Copilot coding agents. You start knowing nothing. Everything you need is in this file and in your own lane file next to it.

Lane files in this folder:

- `Lane A.md`: Panel UI
- `Lane B.md`: Camera, Mic and Recording
- `Lane C.md`: Live and Remote
- `Lane D QA.md`: independent review, started after A, B and C open pull requests

---

## 0. Quality Bar (Every Lane Must Meet This)

Write production-grade code, as if a top professional Android team wrote it. Lane D QA **fails any PR that falls below this bar**, even if every feature works.

1. **Clean architecture:** small, focused files with one job each (aim for under 400 lines per file; split anything bigger). Clear layers: UI views, state and settings, pipeline (camera, GL, audio), platform glue. No god classes.
2. **No dead code, no placeholders:** no `TODO`, `FIXME`, stub functions, commented-out code, unused parameters, unused resources, or "coming soon" text in the app. If something is out of scope, it is not in the code.
3. **Consistent naming:** Kotlin conventions, names that say what a thing is (`BackOrbView`, `MicEngine`, `PairingKey`), the same word for the same idea everywhere (follow the existing `fx.` key style and folder ids).
4. **Smooth 60 fps UI:** no allocation, blur, path rebuild or bitmap creation inside `onDraw` or per frame; cache masks and paths; no main-thread disk, network, Keystore or codec work. Check with `dumpsys gfxinfo` or a profiler and note results in the PR.
5. **Pixel-perfect spacing on an 8 dp grid:** sizes, paddings and gaps are multiples of 8 dp (4 dp half steps only inside icons and hairlines). Controls in a row share one center line and baseline.
6. **Real animations with easing:** every visible state change animates with a proper interpolator (for example `PathInterpolator(0.2f, 0f, 0f, 1f)` or decelerate) and a sensible duration (120 to 320 ms for UI), never a jump; all motion respects Remove animations (jumps only then).
7. **Unit tests for logic:** every piece of non-UI logic (math, state machines, settings, migrations, DSP, crypto, parsing) has unit tests; UI layout rules that matter (alignment, sizes) have Robolectric tests.
8. **No generic AI-looking UI or filler text:** no stock gradients, default Material widgets dropped in unstyled, emoji confetti, lorem ipsum, vague marketing lines, or over-explaining labels. Every word on screen is short, plain and specific (Terminal Plain Language Rule), and every element matches the OLED terminal look.
9. **Errors handled:** no silent catch-all; every failure the user can hit has a plain message and a safe state (face stays covered, recording file kept).

---

## 1. What the App Is

OFFGRID is Royalty's private native Android app for her Galaxy S25 Ultra (Android 15/16) and Galaxy A37 (Android 16). It is installed from a GitHub release, never from the Play Store. Royalty has no computer, so nothing may require a PC.

- It takes the **front camera** with CameraX, runs every frame through an **OpenGL ES compositor** (`CompositorProcessor`, `StagePainter`, `StageShaders`), and bakes **effects** (emoji rain, fireworks, portal, shatter, no-face modes, and more) into the picture.
- The same finished picture goes to the screen, the recorded video, and the replay buffer.
- On top floats a **Canvas-drawn HUD terminal panel** (`terminal/TerminalPanel.kt`) with Termux-style nested folders, a laser typewriter status line, typed 0 to 100 number boxes and coded power toggles. It is Royalty's private remote. **It is never in the video.**
- Royalty records and streams **without showing her face**: the face diamond (on by default) and the no-face camera modes (Blackout, Cloak void, Status label, Ghost) keep it hidden.

Code lives in `04 Build/` (Gradle project, module `app`, package `com.offgrid.compositor`). Kotlin, compileSdk 36, targetSdk 36, minSdk 33, CameraX 1.6.2, JUnit plus Robolectric for tests.

Key files:

| Area | Files |
|---|---|
| Camera screen and recording | `MainActivity.kt`, `CompositorProcessor.kt`, `CompositorEffect.kt`, `CompositorDrawer.kt`, `GlTools.kt`, `GallerySaver.kt`, `ReplayBuffer.kt` |
| GPU stage (no-face modes, freeze, echo, shatter) | `StagePainter.kt`, `StageShaders.kt`, `effects/StagePlan.kt`, `effects/StageMath.kt`, `effects/ShatterMesh.kt` |
| Drawn effects (Canvas on their own thread) | `OverlayRenderer.kt`, `effects/EffectEngine.kt`, `effects/EffectLayers.kt`, `effects/layers/*.kt` |
| Settings keys for effects | `effects/FxKeys.kt` (every key starts with `fx.`), read generically by `terminal/EffectSettings.kt` |
| Panel and folders | `terminal/TerminalPanel.kt` (2,238 lines), `PanelTree.kt`, `EffectFolders.kt`, `FolderLayout.kt`, `PanelMenu.kt`, `PanelRows.kt`, `SideDrawer.kt`, `PanelGlassDrawable.kt`, `PanelLook.kt`, `PowerToggleView.kt`, `ScreenEdges.kt` |
| Glue | `EffectsController.kt` (panel to effects), `FaceTracker.kt`, `SoundBoard.kt`, `Handshake.kt`, `PlatformLinks.kt` |
| Manifest and CI | `04 Build/app/src/main/AndroidManifest.xml`, `.github/workflows/build-debug-apk.yml` |
| Docs | `README.md`, `01 Brainstorm/Feature Scope.md`, `06 Prompts and Skills/*`, `08 Chat History/Agent Status Log.md`, `09 Grok Handoff/Grok Handoff.md` |

---

## 2. Current State and Where Claude Stopped

Claude Code (Agent 05) built pull request #8 "FINAL build: everything except voice (Do not merge, Rex reviews)", branch `final-build`, stacked on #7 (`step-2-4-presets-layouts`), #6 and #5. Claude ran out of weekly usage; it resets Saturday Oct 10, 10 PM CT. **Do not touch PRs #5 to #8 (no merge, no close, no edit).**

Head of `final-build`: `9ec834c` "Add a final build section to the Grok handoff notes" (Oct 9, 2:54 AM CT). CI (`Build debug app`) passed on that commit for both push and pull_request.

Done (milestones of the FINAL build prompt):

- **M1 coded toggles:** `PowerToggleView.kt`, a circle with the power symbol, Pure White (start) or Pure Black, optional glow off by default.
- **M2 size 100:** claimed done, but Royalty's phone test says it still does not fill the screen (see requirement A1 and the root cause in section 6).
- **M3 Rex's #7 review fixes:** drawer handle, camera-denied help, Move folders, FitText loop guard, settings migration.
- **M4 everything except voice:** GPU stage, no-face modes, face diamond (ML face finder on device), 17 drawn effects in `effects/layers/`, soundboard (phone speaker only), 18 platform folders with kick tools, success code and results, Record folder with any-length recording, gallery save, replay buffer, presets, Handshake codes (300 characters, Keystore AES-GCM).

Unfinished or unverified:

1. **M5 is not logged.** The last status log entry (Oct 9, 2:55 AM CT) says "Next: Milestone 5: final polish, lint, the final report in pull request #8, and the phone test list." The PR body marks M5 done, but there is no M5 commit and no M5 log entry. Treat M5 as **partly done**: README and PR report exist; on-device performance on the S25 Ultra and A37 (Glow 100 plus Pulse, overdraw, battery) was never verified.
2. **Size 100** still fails on the phone (A1).
3. **Glow 100** is weak (A3).
4. **Not built (deferred by rule, now partly unblocked because the mic is allowed):** audio-reactive panel (feature 16), audio or beat sync for the glow border (part of 17), radar ping and soundboard sounds in the video's audio.
5. **Still not built:** IP leak check (25, needs internet), OFFGRID to OFFGRID calls (14 and 34, Lane C covers the local two-phone part), gift or kick detection from another app's screen (not allowed, see Rex Ideas).
6. **Open TODOs from the log:** Royalty's final folder icon art (replace `FolderIconView.kt` drawing when it lands in `03 Design/References`), a new toggle design she may share, a Neon Pink theme idea, Step 2.5 camera and mic buttons (now requirement 10 in this plan).
7. **Rex voice and typed-to-speech (Step 7, features 8, 38, 39) stay paused.**

---

## 3. Standing Rules (Every Lane, Every Commit)

1. **Permissions:** CAMERA and **MICROPHONE (RECORD_AUDIO) are now allowed** (Royalty authorized the mic on Oct 9, 2026). Lane C may add the permissions listed in `Lane C.md` for the overlay, foreground service and local network. Nothing else without Royalty's go.
2. **No Rex voice, no TTS, no speech recognition.** `TextToSpeech`, `SpeechRecognizer` and `android.speech` stay banned. Step 7 stays paused. Do not touch `06 Prompts and Skills/Rex Voice/` or the `OFFGRID_App_Icons` repo.
3. **No sliders, ever.** Every number is a typed 0 to 100 box. Every switch is a coded power toggle. Colors are hex plus the R O Y G B I V, White and Black mixers.
4. **Native Kotlin and Canvas or OpenGL ES only.** No WebView, no HTML or CSS, no Three.js, no Compose rewrite of the panel.
5. **No Marvel, Iron Man, Stark, JARVIS or Avengers names or assets.** Original art only.
6. **The panel never appears in recordings, the replay buffer, or anything a viewer sees.** The overlay and remote panels follow the same rule.
7. **No secrets in code, docs or logs.** Pairing keys and codes live only in the Android Keystore or Keystore-sealed prefs. Never log a key.
8. **Look:** true-black OLED. **OLED Bioluminescent** is the start theme. The cursor is one solid laser-bright block, **white by default, cursor glow 0 by default**. Toggles stay the existing coded black or white power toggles (`PowerToggleView`). A toggle redesign is planned for a later phase only (section 12).
9. **Streaming platform rules:** never show the face by surprise, no more than 3 flashes per second, no auto-rejoin after a kick, no anti-cheat, root or integrity tricks, no reading other apps' screens or chats, no fake viewers or engagement.
10. **Plain words** in everything the app shows (Terminal Plain Language Rule: short sentences a 13 year old understands).
11. **Do not merge anything.** Do not touch PRs #5 to #8. Do not push to `main`.
12. **Title Case filenames with spaces** for docs. No em-dashes in docs.
13. **Log progress** in `08 Chat History/Agent Status Log.md` after every meaningful step: date and time in America/Chicago, your lane, what was done, the PR, the state, and the exact next thing. Append only. If two lanes append at the same time, the merge keeps both entries.

---

## 4. Build and CI

From `04 Build/`:

```
chmod +x ./gradlew
./gradlew assembleDebug --stacktrace
./gradlew testDebugUnitTest --stacktrace
./gradlew lintDebug        # 0 errors expected
```

You need JDK 17 and Android SDK platform 36 plus build-tools 36.0.0 (see the "Set up Android tools" step in `.github/workflows/build-debug-apk.yml`; copy it if your environment has no SDK).

CI workflow `Build debug app` runs: secret scan, mic guard, voice guard, slider guard, assembleDebug, unit tests, merged manifest permission check, then publishes the APK to the `step-2-preview` release **only on pushes to `final-build`**.

**Known CI gaps (Lane B fixes them first, see B0):** pull requests into `final-build` do not trigger CI, the guards only scan `app/src/main/java`, the voice guard bans `AudioRecord` (needed for mic effects), and the mic guard fails on `RECORD_AUDIO`. Until B0 lands, every lane must run the build, tests and the guard greps locally and paste the results in its PR.

---

## 5. Lane Plan

Three Copilots work at once, each on its own files, all branching from `final-build` and each opening its own PR **into `final-build`**:

| Lane | Title of PR | Scope |
|---|---|---|
| A | `Lane A: Panel UI (Do not merge, Rex reviews)` | Requirements 1 to 9 and 11 (existing toggles stay as they are) |
| B | `Lane B: Camera, Mic and Recording (Do not merge, Rex reviews)` | Requirement 10, CI and manifest, finish incomplete effects |
| C | `Lane C: Live and Remote (Do not merge, Rex reviews)` | Requirements 12 and 13, small floating overlay over other apps (Bigo) |
| D | `Lane D QA` (review only, no feature PR) | PASS, FAIL or BLOCKED for each lane PR, with evidence |

**Merge order for Rex:** A, then B, then C (each with a merge commit, not squash). After A and B are in `final-build`, Lane C merges `final-build` into its branch and adds the small integration hooks listed in `Lane C.md` (section C9).

### File Ownership (One Owner per File)

| Owner | Files |
|---|---|
| **Lane A** | everything in `app/src/main/java/com/offgrid/compositor/terminal/` (including `TerminalPanel.kt`, `PanelTree.kt`, `EffectFolders.kt`, `PanelGlassDrawable.kt`, `PanelLook.kt`, `FolderLayout.kt`, `PanelMenu.kt`, `PanelRows.kt`, `SettingRows.kt`, `SideDrawer.kt`, `ScreenEdges.kt`, `PowerToggleView.kt`, `Hud.kt`), `effects/FxKeys.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/colors.xml`, `app/src/main/res/layout/activity_main.xml`, panel tests in `app/src/test/.../terminal/` |
| **Lane B** | `MainActivity.kt`, `CompositorProcessor.kt`, `CompositorEffect.kt`, `CompositorDrawer.kt`, `GlTools.kt`, `StagePainter.kt`, `StageShaders.kt`, `OverlayRenderer.kt`, `ReplayBuffer.kt`, `SoundBoard.kt`, `GallerySaver.kt`, `EffectsController.kt`, `FaceTracker.kt`, `effects/` except `FxKeys.kt`, new package `audio/`, `app/src/main/AndroidManifest.xml`, `.github/workflows/build-debug-apk.yml`, new `app/src/main/res/values/strings_audio.xml`, `README.md`, tests in `app/src/test/.../effects/` and new `.../audio/` |
| **Lane C** | new Gradle module `04 Build/live/` (everything inside, with its own `AndroidManifest.xml`, resources and tests), `04 Build/settings.gradle.kts`, `04 Build/app/build.gradle.kts` (only to add `implementation(project(":live"))`), new `docs/10 Copilot Plan/Bigo Live Guide.md` |
| **Lane D** | review comments only; may add `docs/10 Copilot Plan/QA Reports/Lane X QA.md` in its own branch |
| **Everyone (append only)** | `08 Chat History/Agent Status Log.md` |

If you think you need a file you do not own, do not edit it. Write the exact change you need under **"Requests for other lanes"** in your PR description. Rex routes it.

### Cross Lane Contracts (Shared by String, so No Lane Waits on Another)

Settings flow through the existing generic path: Lane A defines a folder item with an `fx.` key, `EffectSettings` puts it in the snapshot, and Lane B reads it with `engine.settings.on(...)`, `.raw(...)`, `.choice(...)`. Lane B declares the same strings in `audio/MicKeys.kt`; Lane D checks they match exactly.

| Key | Type | Start | Meaning | Defined in UI by | Used by |
|---|---|---|---|---|---|
| `fx.cam.on` | toggle | on | Camera quick toggle in the top bar | A | B |
| `fx.mic.on` | toggle | off | Mic quick toggle in the top bar | A | B |
| `fx.mic.fx.on` | toggle | off | Microphone Effects master | A | B |
| `fx.mic.pitch` | number | 50 | 0 is deepest, 50 is natural, 100 is highest | A | B |
| `fx.mic.robot` | toggle | off | Robot voice | A | B |
| `fx.mic.robot.amount` | number | 50 | Robot strength | A | B |
| `fx.mic.echo` | toggle | off | Echo | A | B |
| `fx.mic.echo.delay` | number | 30 | 0 is 50 ms, 100 is 800 ms | A | B |
| `fx.mic.echo.amount` | number | 40 | Echo feedback | A | B |
| `fx.mic.radio` | toggle | off | Radio or walkie-talkie band filter | A | B |
| `fx.mic.cave` | toggle | off | Big room reverb | A | B |
| `fx.mic.cave.amount` | number | 40 | Reverb size | A | B |
| `fx.mic.gain` | number | 50 | Mic volume, 50 is unchanged | A | B |
| `fx.mic.gate` | number | 20 | Noise gate, 0 is off | A | B |
| `fx.mic.sounds_in_video` | toggle | on | Mix soundboard sounds into the recorded audio | A | B |
| `fx.mic.beat_glow` | toggle | off | Glow border and panel glow follow the mic beat | A | B |
| `fx.live.overlay` | toggle | off | Use the floating overlay over other apps | A (row in Live folder) | C |
| `fx.remote.role` | choice `off`, `camera`, `remote` | `off` | Two-phone role | A (row in Live folder) | C |

Lane A adds a top-level **📁 Live and Remote** folder that holds `fx.live.overlay`, `fx.remote.role`, plus three action rows whose `PanelAction` values Lane A adds to the enum: `LIVE_OPEN_OVERLAY_SETTINGS`, `LIVE_CLEAN_OUTPUT` and `REMOTE_OPEN_PAIRING`. Lane A also adds a public `TerminalPanel.setToggleSetting(key: String, on: Boolean)` so the app can turn a toggle back off (used by Lane B after Lane A merges, for example when the mic permission is denied). Lane C handles them in its integration hook (C9).

---

## 6. Root Causes and Flaw Review of PR #8

### 6.1 Why Width 100 and Height 100 Do Not Look Full

From reading `TerminalPanel.kt`, `PanelGlassDrawable.kt` and `MainActivity.kt` on `final-build` (not yet confirmed on the phone; Lane A must confirm with the on-device log in A1):

1. `onMeasure` maps 100 to the parent size, so the **glass** rectangle does equal the window. The view itself is `glass + 2 * GLOW_MARGIN_DP (18 dp)` and is translated by `-margin` (`moveGlassTo`).
2. Everything that makes the panel visible lives where the eye can't see it at that size: the outer glow is drawn only in the 18 dp margin outside the glass (off-screen), and the 1.25 to 2.5 dp edge stroke (`line.strokeWidth`) is centered on the glass edge, so half of it is off-screen and the rest sits on the last pixel row and column, hidden by the S25 Ultra's rounded display corners.
3. `refreshEdgeRoom()` then pads the content inward by `edges.top` and `max(edges.bottom, edges.gestureBottom)`. `MainActivity.screenEdges()` measures these with `getInsetsIgnoringVisibility(systemBars())` even though `hideSystemBars()` hides the bars, so there is always a dead band the height of the status bar plus cutout at the top and the nav or gesture bar at the bottom, plus 30% of the corner radius on the sides.
4. More inner gaps stack on top: the 6 dp frame inset, header padding, the detail pane margins (4, 4, 8, 10 dp), the 30 dp bottom padding kept for the resize corner (`GRIP_DP`) and `DETAIL_BOTTOM_DP` 30 dp.
5. The start look uses Opacity 50, so the camera shows through the glass.
6. The M2 test only measured the view's bounds in Robolectric (1152 by 2496), not what is drawn, so it passed while the phone still looked wrong.

Result on the phone: a half see-through box with no visible border and content floating inside dead bands, which reads as "does not fill the screen; it can barely be seen". Fix: Lane A, A1.

### 6.2 Why Glow 100 Is Weak

`ThemeStore.look()` sets `glowStrength = min(1, 0.7 * glow / 0.5)`, which already hits 1.0 at Glow 71, so 71 to 100 only adds `glowBoost`. The outer glow blur radius is fixed at `0.9 * margin` (about 16 dp) because the glow has to fit in the 18 dp margin, the masks are made at half size, and at full size the outer glow is off-screen. Fix: Lane A, A3.

### 6.3 Top Flaws Found in PR #8

1. **Size 100 chrome is off-screen or inset** (6.1). High impact; it is the first thing Royalty sees.
2. **Glow curve saturates at 71 and the bloom is clipped by an 18 dp margin** (6.2).
3. **CI blind spot:** `on.pull_request.branches` does not include `final-build`, so PRs into it get no CI; guards scan only `app/src/main/java`; the voice guard bans `AudioRecord` and the mic guard bans `RECORD_AUDIO`, which now block authorized work. Fix: Lane B, B0.
4. **Reporting gap and a god file:** the PR body marks M5 done, but there is no M5 commit or status log entry, and on-device performance was never checked. `TerminalPanel.kt` is 2,238 lines (94 KB) mixing header, sizing, drawer, menu, folders, ripples and keyboard logic, which makes parallel work conflict-prone. Fix: A12 (split), B5 (performance pass), and every lane logs.
5. **Recording and replay pipeline limits:** recording uses CameraX `Recorder`, which cannot carry processed audio, and `ReplayBuffer` allocates a new `ByteArray` for every encoded frame (up to about 50 MB of short-lived heap at 100 s), which risks GC stutter on the A37. Fix: Lane B, B3 and B5.

Other findings, handled in the lanes:

- OLED Bioluminescent text is hardcoded teal (`Themes.LASER_TEAL`) while edges follow Material You, so colors clash (A5).
- Three side-list modes plus a swipe drawer add complexity Royalty does not want (A6).
- Panel notes still say "This app has no mic" (A12).
- The 18 platform folders sit at Home level, which buries Camera and Effects in a long list (A11).
- `EffectSettings` caches keys once from `PanelTree`; orphaned `FxKeys` are not caught (A12 test).
- `startFiveSecondRecording` records any length (B5 rename).

---

## 7. Requirements Map (Royalty's Phone Test on PR #8)

| # | Royalty's point | Lane and section |
|---|---|---|
| 1 | Width and Height 100 must fill the screen edge to edge | A1 |
| 2 | Resize from bottom edges, no corner piece, thick bioluminescent border | A2 |
| 3 | Much stronger Glow 100 | A3 |
| 4 | Pixel-perfect top bar | A4 |
| 5 | White text on black theme, black Termux text on white theme | A5 |
| 6 | Remove the side panel | A6 |
| 7 | One large folder per row, bioluminescent scroll bar | A7 |
| 8 | Back orb at top left, professional top bar order | A4, A8 |
| 9 | UV black-light color scheme | A9 |
| 10 | Camera and mic toggles, mic allowed, Microphone Effects, audio in videos | A10 and A11 (UI), B0 to B4 (pipeline) |
| 11 | Effects folder with category subfolders | A11 |
| 12 | Using OFFGRID while live on Bigo, floating overlay, honest limits | C1 to C4 |
| 13 | Two-phone remote with a 100+ character key | C5 to C8 |
| Later | Toggle redesign and Toggle Demo screen | Section 12, after the app is fully built |
| New | OFFGRID took over the full screen on Bigo; must be a small floating overlay while live in another app | C1, C2 |

The full requirement text and acceptance checks for each lane follow in sections 8 to 11 (the same text as the lane files). The later toggle phase is section 12, and Rex Ideas are section 13.

---

## 8. Lane A: Panel UI

**Branch from:** `final-build`. **Branch name:** `lane-a-panel-ui`. **PR into:** `final-build`, titled `Lane A: Panel UI (Do not merge, Rex reviews)`.
**Read first:** `docs/10 Copilot Plan/Copilot Master Plan.md` sections 1 to 6 (what the app is, rules, build, ownership, contracts, root causes).
**Covers Royalty's phone-test items:** 1, 2, 3, 4, 5, 6, 7, 8, 9 and 11. The toggle redesign and Toggle Demo are **not** in this lane now (see the master plan's Later section). **Keep the existing toggles exactly as they are.**

### Files You Own

Everything in `04 Build/app/src/main/java/com/offgrid/compositor/terminal/`, plus `effects/FxKeys.kt`, `res/values/strings.xml`, `res/values/colors.xml`, `res/layout/activity_main.xml`, and the tests in `app/src/test/java/com/offgrid/compositor/terminal/`. New panel files go in `terminal/` (suggested: `TopBar.kt`, `BackOrbView.kt`, `QuickToggleView.kt`, `GlowScrollBar.kt`, `FullBleed.kt`, `NeonPalette.kt`). Split `TerminalPanel.kt`: it is 2,238 lines; move the top bar, sizing and resize code out into their own files as you go.

Do **not** edit `MainActivity.kt`, `CompositorProcessor.kt`, `EffectsController.kt`, the manifest, the CI workflow, or anything in `effects/` except `FxKeys.kt`. If you need a change there, list it under "Requests for other lanes" in your PR.

### Requirements and Acceptance Checks

#### A1. Width 100 and Height 100 Fill the Screen, Truly Edge to Edge (Item 1)

Royalty: "Width/Height 100 still does NOT fill the screen; it can barely be seen."

Root cause found in code (see master plan section 6.1 for detail): the glass rectangle is sized to the parent, but everything the eye reads as "the panel" is pushed off-screen or inward:
- The edge line and outer glow live in the 18 dp `GLOW_MARGIN_DP` band **outside** the glass. At 100/100 the view is translated by `-margin`, so the whole glow is off-screen and the 1.25 to 2.5 dp edge stroke sits on the last pixel row and column, under the S25 Ultra's rounded display corners.
- `refreshEdgeRoom()` pads the content inward by the status bar plus cutout height and the gesture bar height using `getInsetsIgnoringVisibility`, even though `hideSystemBars()` hides both bars, plus 30% of the corner radius on the sides, plus the 6 dp frame inset, header padding, the detail pane margins (4, 4, 8, 10 dp) and a 30 dp bottom padding reserved for the resize corner.
- Opacity 50 (start look) lets the camera show through, so a borderless, inset, half see-through panel reads as "not full and barely visible".

Fix:
1. Add a **full-bleed mode** when Width and Height are both 100 (and for the Full screen option). In full-bleed the glass rect is exactly the window (0, 0, displayWidth, displayHeight) and the panel view has **no outside glow margin** (margin 0 in this mode).
2. Draw the border **inside** the glass, inset by half its stroke plus 1 dp, following the **display's own rounded corners**: read `WindowInsets.getRoundedCorner(position)` for each of the 4 corners in `TerminalPanel.onApplyWindowInsets` (override it in the panel; do not edit MainActivity) and build the border path with those radii and centers. Glow goes **inward** (inner bloom) when there is no room outside.
3. Content insets in full-bleed: use `getInsets(systemBars())` (visible bars only; 0 while bars are hidden) plus `displayCutout()` safe insets plus rounded-corner safe area for the top bar only. Remove the 30 dp grip padding (A2 removes the corner grip) and the detail pane side margins in full-bleed.
4. Full-bleed uses an **opaque OLED black skin** by default (new toggle "See-through at full size" in 📁 Size, off by default). Opacity still applies when not full-bleed.
5. When the user pulls the status bar down or the keyboard opens, nothing jumps (keep the existing stable-size logic).

Acceptance:
- Unit or Robolectric test: at 100/100 the glass rect equals the window bounds exactly, and the border path bounds are inside the display and inside every rounded-corner safe zone.
- Instrumented or screenshot test on an emulator profile with a cutout and rounded corners (for example a Pixel 9 Pro XL, API 35): the border pixels are lit at the midpoint of all 4 edges and at 45 degrees inside every corner.
- Debug log line on device: `OFFGRID size: glass=WxH display=WxH` (log only sizes, nothing private). Royalty sees the glowing border all the way around the S25 Ultra screen.

#### A2. Resize from the Bottom Edges and a Thick Bioluminescent Border (Item 2)

- Delete `ResizeGrip` (the little bottom-right corner piece) and all references, accessibility actions included (move "Bigger" and "Smaller" accessibility actions to the panel itself).
- Resize from the **bottom edge** (drag down or up changes height), the **bottom-left and bottom-right edge zones** (change width and height together), and the **left and right edges** of the lower half. Touch zones are 24 dp deep, inside the glass, and never inside the phone's gesture areas (`systemGestures` insets). The top bar stays the drag-to-move handle.
- While resizing, the edge being dragged brightens. Haptic tick on release (if Tick when saved is on).
- **Border look:** a thick neon outline like a glowing white rectangle on black (reference: the pink "AUTO" button and the glowing power icon in Royalty's references, but white by default): core stroke 3 dp (scales 2 to 6 dp with a new typed "Border thickness" 0 to 100 in 📁 Glow effects, start 50), white hot core, then a soft outer and inner bloom in the edge color. Default edge color in OLED Bioluminescent: white core with a UV-tinted bloom (see A9).

Acceptance: no corner grip anywhere (grep `ResizeGrip` returns nothing); dragging the bottom edge changes Height and the top box number updates live; dragging a bottom corner zone changes both; the Width and Height boxes, pinch and edge drag stay in step and are saved; border thickness 0, 50 and 100 render as 2, 3 and 6 dp in a test.

#### A3. Glow at 100 Is Much More Powerful (Item 3)

Root cause: `glowStrength = min(1, 0.7 * glow / 0.5)` saturates at Glow 71, so 71 to 100 only adds the small `glowBoost` layer; the outer glow blur radius is fixed at `0.9 * 18 dp` because it must fit inside the 18 dp margin; masks are blurred at half size; and at full size the outer glow is entirely off-screen.

Fix:
- Make the glow curve continuous and strong: 0 is off, 50 is today's normal look, 100 is roughly 3 times the light energy of 50.
- **Layered bloom:** at least 3 layers (tight core 2 to 4 dp, mid 12 to 20 dp, wide 32 to 64 dp radius at 100), each blurred once per size into ALPHA_8 masks (keep the mask cache), additive blend (`BlendMode.PLUS`) so overlapping layers really get brighter.
- The glow margin grows with Glow strength (for example 18 dp at 50, 48 dp at 100) when the panel is not full-bleed, so the bloom is never clipped; in full-bleed the wide layers are drawn inward.
- Pulse at Glow 100 must still hold 60 fps on the S25 Ultra and at least 50 fps on the A37 (draw only cached masks per frame; no new blur per frame).

Acceptance: a unit test of the glow curve (0, 50, 71, 100 are strictly increasing; 100 is at least 2.5 times 50 in summed layer alpha); a rendered-bitmap test shows lit pixels at least 40 dp outside the glass at 100 (non full-bleed); a frame-time note in the PR from a profile run or `dumpsys gfxinfo` on an emulator.

#### A4. Pixel Perfect Top Bar on a Strict Grid (Items 4 and 8)

Order from top-left to top-right, one row:

`[Back orb] [Title] ... [Camera] [Mic] [W box] [H box] [⋮] [−]`

- The 8 dp grid from the Quality Bar. Bar height 56 dp. Every control is a 40 dp tall visual inside a 48 dp touch target, vertically centered on the same center line. Number boxes 40 dp tall, 56 dp wide, text baseline aligned with the title baseline. Equal 8 dp gaps between all right-side controls. 16 dp outer padding left and right (plus safe insets in full-bleed). The − button's right edge is flush with the content edge, the Back orb's left edge is flush with the left content edge.
- The WIDTH and HEIGHT captions sit above their boxes in a 10 sp label, both captions on one baseline, or are removed in favor of "W" and "H" inside the boxes; pick one and apply to both.
- On narrow panels, hide in this order: captions, then W and H boxes (still in 📁 Size), then the title shrinks; Back, Camera, Mic, ⋮ and − never hide.
- Remove the drag grip dots (the whole bar background is the drag handle; taps on controls still work).

Acceptance: a layout test asserts every right-side control has the same top, the same height and equal gaps (within 1 px) at 3 widths (360 dp, 412 dp, full S25 Ultra width); a screenshot in the PR at Font size 0, 50, 100.

#### A5. Terminal Text Colors Follow the System Theme (Item 5)

- Typed text and status text: **white `#FFFFFF` on the black system theme** (dark mode) and **black `#000000` Termux-style monospace on the white system theme** (light mode), JetBrains Mono. Remove `LASER_TEAL` as the default for TEXT and STATUS in OLED Bioluminescent (keep teal only as an optional color).
- In light mode the panel skin becomes white (`#FFFFFF`) with black text; glow colors stay neon.
- Follow `Configuration.uiMode` changes live without restarting.
- A user's custom color in 📁 Colors still wins over the default.
- Settings migration: installs that still have the old teal default (not a custom pick) move to the new default once.

Acceptance: unit tests for both modes; theme switch while the app is open updates the text without a restart; custom colors survive the migration.

#### A6. Remove the Side Panel Entirely (Item 6)

- Delete the left side list, the `sideRule`, `SideDrawer.kt`, `SideMode`, the ⋮ menu's "Side list" choice, its saved preference (migrate it away), `SIDE_*` constants, side text color (`ColorPart.SIDE_TEXT`, migrate saved values away), and the related strings and tests.
- The main Termux-style folder page is the only navigation: Home grid, then folder detail with the path bar and the Back orb.

Acceptance: grep finds no `SideDrawer`, `sideList`, `SideMode`, `SIDE_TEXT`; Back from any folder depth returns one level at a time; the phone Back gesture does the same and exits only from Home.

#### A7. Grid View with One Large Folder per Row and a Bioluminescent Scroll Bar (Item 7)

- The default view for Home and every folder: **one large folder per row** (a full-width tile, at least 72 dp tall, big glowing folder icon on the left, title in the system text color, item count on the right). Keep List, Grid 2 and Grid 3 in the ⋮ menu as options; the new default is "Big tiles".
- A **visible scroll bar** on the panel's right side (left side in RTL): a 3 dp track at 15% alpha and a 6 dp wide, rounded **bioluminescent thumb** (neon core plus soft bloom) whose length matches the visible fraction. It follows the finger while scrolling and can be dragged directly (24 dp touch zone). Smooth motion: the thumb moves with the scroll position every frame, fades in within 120 ms when scrolling starts and fades out 900 ms after it stops; with Remove animations on, it stays visible and static.
- Apply the same scroll bar to the folder detail scroll view.

Acceptance: a test that the thumb position equals the scroll fraction (within 1 px) at top, middle and bottom; a test that dragging the thumb scrolls the content; screen recording in the PR.

#### A8. Back Button as a Glowing Orb at the Very Top Left (Item 8)

- Move Back out of the path bar into the top bar's first slot. A 40 dp circle with a left arrow inside, drawn in code (Canvas): black fill, a UV neon ring (2.5 dp) with a soft ambient bloom, like it sits under a black light. Pressed state fills the ring color at 35% alpha.
- On Home the orb is dimmed to 35% and disabled (still in place so the bar never shifts).
- Content description "Back", 48 dp touch target.

Acceptance: orb is the left-most control, aligned to the grid (A4); tapping it goes up one folder; on Home it is disabled; accessibility check passes.

#### A9. UV Black Light Color Scheme (Item 9)

Royalty's references (`/workspace/handoff/step-final-shots/` on Rex's box; descriptions here so you do not need them): neon glow-tube palettes on pure black (purple, pink, yellow, green and red families with glowing caps), a pink "AUTO" neon outline button and a pink UV Index ring app, a glowing white power symbol on black, neon pill swatches `#00E5FC`, `#0032FF`, `#FF00A6`, `#7B02B3`, a "Cyber" dot grid (magenta, cyan, blue, lime), and dark glass dashboards with purple glow.

- Add a `NeonPalette` with these UV colors: UV Violet `#7B02B3`, Electric Purple `#B026FF`, Hot Magenta `#FF00A6`, Neon Pink `#FF2BD6`, Cyan Glow `#00E5FC`, Deep Blue `#0032FF`, Toxic Lime `#B6FF00`, Blacklight White `#F4ECFF`.
- **OLED Bioluminescent stays the start theme**, now with a white core border and Electric Purple plus Cyan bloom by default, white text (A5) and the white solid cursor with glow 0.
- Add one new theme **UV Black Light** (magenta and violet bloom, cyan accents) and add the palette as one-tap swatches in 📁 Colors (hex still typed, mixers still 0 to 100).
- Never more than 3 flashes per second in any glow animation.

Acceptance: start look unchanged in rules (black, white cursor, glow 0 cursor, white toggles); new theme applies and Reset all returns to OLED Bioluminescent.

#### A10. Camera and Mic Quick Toggles in the Top Bar (UI Half of Item 10)

- Two coded icons right after the title, before the W box: a camera icon and a mic icon, 40 dp visuals in 48 dp targets.
- **Off:** red `#FF2D55` icon with a diagonal slash. **On:** green `#30FF7A` icon with a soft glow. Animate the slash in or out in 150 ms (none with Remove animations).
- They write the settings `fx.cam.on` (start on) and `fx.mic.on` (start off) through `ThemeStore.setToggle`, then fire `onSettingChanged` so Lane B reacts. The panel never touches the camera or mic itself.
- The status line types a plain line, for example "Mic is on. Your voice goes into the video."

Acceptance: tapping each flips color and slash and saves; state survives restart; a test asserts the keys are written exactly as `fx.cam.on` and `fx.mic.on`.

#### A11. Folder Reorganization (Item 11, Plus the Microphone Effects Folder UI)

New Home order (Big tiles):

1. 📁 Camera (no-face modes: Camera mode, Face diamond, Cloak void, Status label, Ghost, Takeover, Mirror plus scan)
2. 📁 Effects
   - 📁 Camera Effects: Time freeze, Echo trail, Depth shift, Shatter, Quiet mode
   - 📁 Screen Effects: Beach, Clouds, Portal, Optical illusions, Roller coaster
   - 📁 Particle Effects: Emoji rain, Fireworks, Flying heroes
   - 📁 Light and Laser Effects: Glow border, Laser writing, Radar sweep, Red sweep, Drawing
   - 📁 Microphone Effects (new; items below)
   - 📁 Sound Effects (the old Soundboard)
3. 📁 Record
4. 📁 Live and Remote (new; overlay and two-phone rows, see master plan contracts)
5. 📁 Platforms (the 18 platform folders move inside one folder, important first)
6. 📁 OFFGRID to OFFGRID
7. 📁 Panel settings (Size, Display, Glow effects, Colors, Themes, Presets, Reset)

📁 Microphone Effects items, all with the exact keys from the master plan contracts: Microphone Effects master toggle, Pitch (0 to 100, 50 natural), Robot toggle plus amount, Echo toggle plus delay and amount, Radio toggle, Cave toggle plus amount, Mic volume, Noise gate, Sounds in video toggle, Beat glow toggle, and a note: "Everything runs on this phone. Nothing goes to the cloud." Plus Reset this folder.

- Keep every existing `fx.` key and folder id stable so saved settings and presets keep working; only parents change. Add a migration for saved "open folder" and custom folder orders.
- Each category folder shows its item count and a one-line plain description.

Acceptance: a test walks the tree and asserts every old `fx.` key is still reachable exactly once, the new mic keys exist with the contract strings, and no word splits at Font size 0, 50 and 100 (reuse the existing no-split test).

#### A12. Panel Flaw Fixes from the Review

- Split `TerminalPanel.kt` into focused files (top bar, sizing and resize, glow and ripples, folder navigation). Target under 1,000 lines for `TerminalPanel.kt`.
- `EffectSettings` caches keys from `PanelTree.build()` once; keep it, but add a test that every `fx.` key in `FxKeys` appears in the tree (catches orphaned keys).
- Remove the stale "No mic" and "This app has no mic" notes from folders (Ghost, Time freeze, Soundboard, Record) and say what the mic does now, in plain words.

### Done Means

- Meets the Quality Bar (master plan section 0).
- `./gradlew assembleDebug testDebugUnitTest lintDebug` pass; slider, voice and secret greps are clean.
- PR description has screenshots or a short screen recording for A1, A2, A3, A4, A7, A8, A10, a checklist of A1 to A12 with evidence, and "Requests for other lanes" if any.
- Status log entry appended.


---


## 9. Lane B: Camera, Mic and Recording

**Branch from:** `final-build`. **Branch name:** `lane-b-camera-mic`. **PR into:** `final-build`, titled `Lane B: Camera, Mic and Recording (Do not merge, Rex reviews)`.
**Read first:** `docs/10 Copilot Plan/Copilot Master Plan.md` sections 1 to 6.
**Covers Royalty's phone-test item:** 10 (camera and mic toggles wired to the pipeline, mic permission, Microphone Effects processing, audio in recordings), plus CI fixes and finishing effects Claude left incomplete.

Royalty **now authorizes the microphone** (Oct 9, 2026). Rex voice and typed-to-speech (Step 7) stay paused: no `TextToSpeech`, no `SpeechRecognizer`, no `android.speech`.

### Files You Own

`MainActivity.kt`, `CompositorProcessor.kt`, `CompositorEffect.kt`, `CompositorDrawer.kt`, `GlTools.kt`, `StagePainter.kt`, `StageShaders.kt`, `OverlayRenderer.kt`, `ReplayBuffer.kt`, `SoundBoard.kt`, `GallerySaver.kt`, `EffectsController.kt`, `FaceTracker.kt`, everything in `effects/` **except** `FxKeys.kt`, a new package `audio/`, `app/src/main/AndroidManifest.xml`, `.github/workflows/build-debug-apk.yml`, a new `res/values/strings_audio.xml`, `README.md`, and tests in `app/src/test/.../effects/` and a new `.../audio/`.

Do **not** edit anything in `terminal/`, `FxKeys.kt`, `strings.xml`, or the `live` module. The panel UI for your features (top-bar toggles, 📁 Microphone Effects folder) is built by Lane A. You read its settings by the exact key strings in the master plan contracts; declare them in `audio/MicKeys.kt`.

### Requirements and Acceptance Checks

#### B0. CI and Guards First (Do This in Your First Commit)

In `.github/workflows/build-debug-apk.yml`:
- Add `final-build` to `on.pull_request.branches` so every lane PR into `final-build` runs CI.
- Mic guard: replace "no RECORD_AUDIO" with an **allowlist check** on the merged manifest: allowed permissions are exactly `CAMERA`, `RECORD_AUDIO`, plus Lane C's list (`SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CAMERA`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, `INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `NEARBY_WIFI_DEVICES`, `CHANGE_WIFI_MULTICAST_STATE`). Any other permission fails the build. Print the list.
- Voice guard: keep banning `TextToSpeech|SpeechRecognizer|android\.speech`; **stop banning `AudioRecord`** (mic effects need it).
- Run all greps over every module: `04 Build/*/src` instead of only `app/src/main/java`.
- Keep the secret scan and slider guard. Keep publishing the APK only on pushes to `final-build`.
- Update the release notes text: the mic is now allowed and used for videos.

Acceptance: CI runs on your PR into `final-build`; a deliberate test of the allowlist (documented in the PR) fails on an extra permission.

#### B1. Manifest and Permission Flow

- Remove the `RECORD_AUDIO` `tools:node="remove"` entry and add `<uses-permission android:name="android.permission.RECORD_AUDIO" />`. Keep `ACCESS_NETWORK_STATE` removed.
- Ask for the mic **only when Royalty turns the mic toggle on** (or first records with mic on), with a plain explanation line on the panel first. If denied: the mic toggle turns back off (call `TerminalPanel.setToggleSetting("fx.mic.on", false)`, which Lane A adds; until Lane A is merged, keep the mic off in your own state and add that one call when you merge `final-build` in), and the record bar says how to allow it in Settings.
- Update the CameraX pipeline comments ("Audio stays off") and README sections that say there is no mic.

#### B2. Camera and Mic Toggles Drive the Pipeline (Item 10)

- `fx.cam.on` **off:** unbind the camera use cases so the hardware is released (the green privacy dot goes away). The screen and any recording show OLED black with the drawn effects still running (drive the compositor from a 30 fps timer with a black input when the camera is off) so a stream never shows a frozen face. Status line: "Camera is off. Viewers see black and your effects." **On:** rebind and resume. Face safety rules still apply: if the camera comes back on with the face diamond or a no-face mode active, the first frame is already covered.
- `fx.mic.on` **on:** recordings include audio (B3). **Off:** no audio track or a silent track (pick one and document it); `AudioRecord` is stopped and released (the orange mic dot goes away).
- Toggling during a recording: camera off keeps recording with black frames; mic off writes silence for the rest of the clip. Never crash, never drop the file.

Acceptance: logcat or a unit test of the state machine shows camera unbind and rebind and `AudioRecord` start and release on each toggle; a recorded test clip with a camera-off segment and a mic-off segment plays in the gallery.

#### B3. Audio in Recordings, with Microphone Effects Applied

CameraX `Recorder.withAudioEnabled()` records the raw mic and cannot apply effects. Build our own path:

- New `audio/MicEngine.kt`: `AudioRecord` at 48 kHz mono 16-bit (`VOICE_RECOGNITION` or `CAMCORDER` source, whichever gives less processing; document the choice), a processing thread, and a lock-free ring buffer.
- New `audio/VoiceFx.kt`: on-device DSP only, no cloud, no ML voice cloning:
  - Pitch shift (0 to 100, 50 natural, about one octave down to one octave up) with a granular or phase-vocoder method that keeps duration.
  - Robot (ring modulator around 30 to 120 Hz plus light bit crush), amount 0 to 100.
  - Echo (delay 50 to 800 ms, feedback 0 to 0.85).
  - Radio (band-pass 300 Hz to 3.4 kHz plus soft clip).
  - Cave (Schroeder or Freeverb style reverb), amount 0 to 100.
  - Mic volume (gain) and noise gate.
  - Each stage is skipped when off. Total added latency under 60 ms.
- New `audio/AvRecorder.kt`: H.264 video from a `MediaCodec` input surface that the compositor draws into (the same way `ReplayBuffer` gets a surface today, see `CompositorProcessor.setReplay`) plus AAC-LC 128 kbps audio from the processed mic, muxed with `MediaMuxer` into MP4 with correct A/V sync (audio timestamps from `AudioRecord` frame counts, video from the stage's presentation times). Use it for all recordings (mic on or off) so one path is tested; keep the CameraX `VideoCapture` fallback only if `AvRecorder` fails to start, and say so on the status line.
- `fx.mic.sounds_in_video` on: mix the soundboard's generated sounds into the recorded audio (SoundBoard already synthesizes them; expose PCM). Also mix the radar ping (finishing feature 17's audio part).
- The **replay buffer** also keeps audio when the mic is on.
- Live monitoring through the speaker is **off** (feedback risk); only with wired or Bluetooth headphones, as a toggle request to Lane A if wanted.
- The panel is still never in the video (the compositor never sees the panel).

Acceptance: unit tests for each DSP stage (pitch 50 is near-identity, echo delay matches the typed value within 5 ms, gate silences low noise, no NaN or clipping beyond full scale); an instrumented or Robolectric test that `AvRecorder` writes an MP4 with one video and one audio track; manual check list in the PR: record 20 s with Robot plus Echo on, play it back, voice is changed and in sync (lip sync within 80 ms).

#### B4. Beat Glow (Optional Inside This Lane, Mic Required)

- `audio/BeatDetector.kt`: RMS envelope plus onset detection on the mic stream; exposes a 0 to 1 level and beat events at no more than 8 per second.
- When `fx.mic.beat_glow` is on, the **glow border layer** in the video pulses with the beat (cap brightness changes to 3 flashes per second). The panel's own glow pulsing to audio is a later integration (needs a small public hook in Lane A's code; list it under "Requests for other lanes").

#### B5. Finish What Claude Left Incomplete

- Verify every effect in 📁 Effects actually renders on device-size frames and is reachable after Lane A's reorg (keys unchanged). Fix any that do nothing.
- Radar ping and soundboard into the video's audio (B3).
- `ReplayBuffer`: stop allocating a new `ByteArray` per encoded frame (100 s at 4 Mbps is about 50 MB of short-lived heap). Use a pooled ring of direct buffers or a capped byte arena, and cap total memory with a clear limit (for example 64 MB) that lowers the kept seconds and says so.
- `MainActivity.startFiveSecondRecording` is misnamed (records any length); rename and keep behavior.
- M5 performance pass: profile Glow 100 plus Pulse plus 3 effects on an emulator, note frame times in the PR, and remove any per-frame allocation you find in `OverlayRenderer` and the layers.

### Done Means

- Meets the Quality Bar (master plan section 0).
- `./gradlew assembleDebug testDebugUnitTest lintDebug` pass; CI green on the PR; the merged manifest permission list printed in the CI log matches the allowlist.
- No `TextToSpeech`, `SpeechRecognizer` or `android.speech` anywhere.
- PR description: checklist B0 to B5 with evidence, a short recorded sample description, "Requests for other lanes" (for example the mic-denied setter), and the status log entry.


---


## 10. Lane C: Live and Remote

**Branch from:** `final-build`. **Branch name:** `lane-c-live-remote`. **PR into:** `final-build`, titled `Lane C: Live and Remote (Do not merge, Rex reviews)`.
**Read first:** `docs/10 Copilot Plan/Copilot Master Plan.md` sections 1 to 6.
**Covers Royalty's phone-test items:** 12 (using OFFGRID while live on Bigo) and 13 (two-phone remote), plus Royalty's Bigo finding: today OFFGRID takes over the full screen, so it can't be used alongside Bigo.

### Files You Own

A new Gradle **library module** `04 Build/live/` (namespace `com.offgrid.live`) with its own `AndroidManifest.xml`, resources, code and tests; `04 Build/settings.gradle.kts` (add `include(":live")`); `04 Build/app/build.gradle.kts` (only the line `implementation(project(":live"))`); and a new doc `docs/10 Copilot Plan/Bigo Live Guide.md`.

The library's manifest merges into the app, so you declare your own permissions and services there without touching the app manifest (Lane B owns it). The `live` module must not depend on `app` classes; talk to the app through the small interfaces in C9. Read and write panel settings by key through `SharedPreferences` only via an interface the app passes in (C9), never by guessing internal file names.

### The Honest Android Picture (Stock Android 15 and 16, Samsung S25 Ultra, No Root)

1. **No virtual camera for third-party apps.** A normal app cannot register as a camera that Bigo (or any app) can pick. That needs root plus hooking modules, which trips Samsung Knox for good, breaks Samsung Wallet and Secure Folder, and risks Bigo bans. **Not allowed in this project.**
2. **Only one app can use the camera at a time.** When Bigo goes live with its own camera, Bigo owns the front camera. OFFGRID cannot get frames while Bigo has it, and Android blocks camera access from the background anyway.
3. **A floating overlay (draw over other apps) is visible only on Royalty's own screen.** In Bigo's normal camera live, viewers see Bigo's camera, **not** the overlay. So the overlay is a control surface, not a way to put effects into Bigo's camera stream.
4. **The working path to get OFFGRID's effects to Bigo viewers on one phone is Bigo's screen-sharing live mode** (Bigo's "Game" live or any mode that shares the screen with Android's MediaProjection), if Royalty's Bigo account and region offer it: OFFGRID runs full screen showing the finished picture (effects baked in, panel hidden or secure), and Bigo shares the screen. Mic: Bigo usually records the mic itself in that mode, so OFFGRID's voice effects will not reach Bigo viewers (Android does not let two apps share a processed mic). Voice effects work in OFFGRID's own recordings.
5. **Second-device path:** run OFFGRID on one phone and Bigo on another phone pointed at the first phone's screen, or use the two-phone remote (C6 to C8) so the camera phone stays clean and full screen while the remote phone holds every control.
6. **Bigo's rules:** using screen share to show a camera-like feed may be judged differently from games. Royalty must check Bigo's current community guidelines before relying on it, use a dummy account first (standing rule), and never use anything that fakes viewers, gifts or engagement. The app shows this as a plain note in 📁 Live and Remote and in the Bigo platform folder.

Write all of this, in plain words with tap-by-tap steps, in `docs/10 Copilot Plan/Bigo Live Guide.md`.

### Requirements and Acceptance Checks

#### C1. Floating Overlay Mode (Draw Over Other Apps)

When Royalty is live in another app, OFFGRID must run as a **small floating overlay**, never full screen unless she chooses it.

- Permission `SYSTEM_ALERT_WINDOW`: ask through `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` with a plain explanation first; if not granted, the overlay row says how to turn it on.
- A window of type `TYPE_APPLICATION_OVERLAY` hosting a new **mini panel** (`live/OverlayPanelView.kt`, Canvas-drawn, same OLED look: black glass, white text, UV glow border, coded power toggles; no sliders):
  - **Move:** drag the top bar.
  - **Resize live:** drag the bottom edge or bottom corners (same feel as Lane A's A2); typed W and H boxes 0 to 100 in its own small settings.
  - **Shrink:** − collapses it to a **bubble** (56 dp glowing orb with the OFFGRID mark) that can be dragged and snaps to the nearest screen edge; tap the bubble to expand; drag it onto a bottom "×" target to close the overlay.
  - **Never covers Bigo full screen unless chosen:** default size 40 by 35 percent of the screen, top right; a "Full screen" option exists but is off and asks to confirm.
  - Quick controls inside: Camera and Mic state, Fire buttons for one-shot effects (Shatter, Freeze, Quiet, Fireworks, Red sweep, Laser writing), camera mode picker, Open OFFGRID, and Close overlay.
  - Touch outside the overlay passes to Bigo (`FLAG_NOT_TOUCH_MODAL`); the keyboard works for typed boxes (toggle `FLAG_NOT_FOCUSABLE` only while a box is being typed in).
  - Mark the overlay window **secure** (`FLAG_SECURE`) so that if any app captures the screen, the overlay is black instead of leaking controls; document that it then appears as a black rectangle in a screen share, so Royalty should collapse it to the bubble while screen sharing.
  - Remember position, size and bubble state.
- Respect gesture areas and rounded corners; never sit under the status bar.

Acceptance: overlay appears over another app (test with any app on an emulator), can be moved, resized, collapsed to a bubble, expanded and closed; taps outside reach the app below; state survives restart; screenshots in the PR.

#### C2. Foreground Service

- `live/LiveService.kt`, a foreground service with a persistent notification ("OFFGRID overlay is on", actions: Open, Hide overlay, Stop).
- Types: `specialUse` for the overlay control (with the `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` explanation), plus `camera` and `microphone` **only** while OFFGRID itself is recording or streaming through the screen-share path, started while OFFGRID's activity is visible (Android 14+ while-in-use rules). Permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `FOREGROUND_SERVICE_CAMERA`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`.
- The service stops cleanly when the overlay closes and nothing is recording.

Acceptance: the service survives switching to another app; the notification actions work; no crash on Android 14, 15 and 16 emulators when started from the activity; the service never starts camera or mic from the background.

#### C3. Screen Share Friendly Full Screen Output

- A "Clean output" mode in OFFGRID (action row in 📁 Live and Remote; Lane A adds the row, you implement the behavior in C9): hides the record bar and collapses the panel to the overlay bubble so the full-screen picture shows only what viewers should see. One tap on the bubble brings controls back.

Acceptance: in Clean output, a screenshot of the device shows only the finished picture plus the bubble (or nothing if the bubble is hidden).

#### C4. Bigo Folder Notes

- Ask Lane A (in "Requests for other lanes") to add the plain Bigo notes from the guide to the Bigo platform folder; you write the exact text in the guide.

#### C5. Two Phone Remote: Roles

- Two phones both run OFFGRID. One is **Camera** (runs the camera, effects and recording), the other is **Remote** (shows the live preview and controls effects). Role is the `fx.remote.role` setting (`off`, `camera`, `remote`).

#### C6. Pairing With a 100+ Character Key

- Camera phone taps "Make pairing key": generate **128 characters** from `SecureRandom` over a 64-symbol URL-safe alphabet (768 bits). Show it once with a **Copy** button (clipboard marked sensitive with `ClipDescription.EXTRA_IS_SENSITIVE`; clear the clipboard after 60 seconds if it still holds the key).
- Remote phone taps "Paste pairing key"; validate length (at least 100) and alphabet; show a short 6-character fingerprint on both phones so Royalty can confirm they match.
- Store the key only Keystore-sealed (reuse the AES-GCM approach in `Handshake.kt`; copy the pattern into the `live` module). Never log it, never put it in a notification, never put it in the repo. "Forget pairing" deletes it; "New key" rotates it.

Acceptance: unit tests: generated keys are 128 characters, alphabet-only, and two keys never match in 10,000 tries; validation rejects 99 characters; logs contain no key material (test with a log capture).

#### C7. Local Network Only, Encrypted

- Discovery and transport, local only, no cloud servers, no relays:
  1. **Same Wi-Fi:** `NsdManager` service `_offgrid._tcp`, TCP socket.
  2. **Wi-Fi Direct** fallback (`WifiP2pManager`) when there is no shared Wi-Fi; `NEARBY_WIFI_DEVICES` with `neverForLocation`.
- Session crypto: each side makes an ephemeral X25519 key pair (Android `KeyPairGenerator("XDH")`, API 33+), exchanges public keys plus 32-byte random nonces, then derives the session key with **HKDF-SHA256(ikm = X25519 shared secret, salt = nonceA || nonceB, info = "OFFGRID remote v1" plus the pairing key)**, so the pairing key authenticates the session and each session has forward secrecy. Confirm with an HMAC-SHA256 over the transcript keyed by a key derived from the pairing key; on mismatch drop the connection and say "Keys don't match".
- Every message is AES-256-GCM with a 96-bit nonce made from a per-direction counter; reject replays and out-of-order counters. Rekey every 2^20 messages or 30 minutes.
- Permissions: `INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `NEARBY_WIFI_DEVICES` (in the `live` manifest). Nothing ever connects outside the local network: bind only to site-local addresses and refuse public IPs.

Acceptance: unit tests for HKDF vectors (RFC 5869 test case 1), GCM round trip, replay rejection, wrong-key rejection; an instrumented loopback test (two endpoints in one process) passes control messages and frames.

#### C8. Remote Preview and Control

- Camera phone sends the **finished picture** (effects baked in, never the panel) as H.264, 540p, 24 fps, about 1.5 Mbps, through the encrypted channel; target under 250 ms glass-to-glass on the same Wi-Fi. Audio preview off by default.
- Remote phone shows the preview full width with a compact control panel (reuse `OverlayPanelView`'s look): Camera and Mic toggles, camera mode, Fire buttons, and effect toggles. Control messages are `{key, type, value}` for `fx.` keys plus fire actions; the camera phone applies them through the same settings path as its own panel.
- If the link drops: the camera phone keeps running safely (no settings change), the remote says "Lost the camera phone. Trying again." and retries.

Acceptance: loopback test passes settings and fire actions; manual two-emulator or two-phone check list in the PR.

#### C9. Integration Hooks (After A and B Merge Into the Final Build Branch)

The `live` module exposes:

```kotlin
interface OffgridHost {
    fun readSetting(key: String): String?
    fun writeSetting(key: String, value: String)
    fun fire(action: String)
    fun openApp()
    fun frameSurfaceSink(): FrameSink   // for the remote preview encoder
}
```

When Lanes A and B are in `final-build`, merge `final-build` into your branch and add the minimal hooks (under 40 lines, clearly marked `// Lane C hook`): implement `OffgridHost` in a new app-side file `LiveHost.kt`, start or stop the service and overlay from `MainActivity` on the 📁 Live and Remote rows and on `onPause` when the overlay toggle is on, and add one `CompositorProcessor` output surface for the remote encoder (mirror how `setReplay` works). Note each hook in the PR so Rex sees exactly what touched Lane B's files.

### Done Means

- Meets the Quality Bar (master plan section 0).
- `./gradlew assembleDebug testDebugUnitTest lintDebug` pass with the new module; CI green.
- Merged manifest has only the allowlisted permissions (see Lane B, B0).
- PR has the Bigo guide, screenshots of overlay and bubble, the crypto test results, a two-device check list, and a status log entry.


---


## 11. Lane D QA

**Start only after Lanes A, B and C have opened their PRs into `final-build`.** Do not write features. Do not merge. Do not touch PRs #5 to #8.
**Read first:** `docs/10 Copilot Plan/Copilot Master Plan.md` (all of it) and `Lane A.md`, `Lane B.md`, `Lane C.md`.

### Job

For each lane PR, check every requirement in that lane's file against the code, the CI run and the evidence in the PR, and return one verdict per requirement and one overall:

- **PASS:** met, with evidence (file and line, test name and result, CI link, screenshot).
- **FAIL:** not met, with the exact gap and the smallest fix.
- **BLOCKED:** cannot be checked (for example needs Royalty's phone), with what is needed.

Post the report as a PR review comment (COMMENT, never APPROVE or merge) and, if you have a branch, as `docs/10 Copilot Plan/QA Reports/Lane X QA.md`.

### Checks for Every Lane

0. **Quality Bar (master plan section 0): FAIL the PR if any point is missed.** Check file sizes and focus, grep for `TODO|FIXME|stub|lorem|coming soon` and commented-out code, naming consistency, per-frame allocations in `onDraw` and render loops, 8 dp grid spacing in layout code, easing on every visible state change, unit tests for all logic, plain specific on-screen text with no generic AI-looking UI, and handled errors.
1. Branch is from `final-build` and the PR targets `final-build`; title matches the plan.
2. Only owned files changed (compare the file list with the ownership table). Any other file is a FAIL unless listed and approved under "Requests for other lanes".
3. CI green; `assembleDebug`, `testDebugUnitTest`, `lintDebug` pass.
4. Rules: no sliders (`SeekBar|RangeSlider|material.slider|Slider(`), no `TextToSpeech|SpeechRecognizer|android.speech`, no WebView, no Three.js, no banned names (Marvel, Iron Man, Stark, JARVIS, Avengers), no secrets or keys, no em-dashes in new docs, Title Case doc names.
5. The panel, overlay and remote UI never reach the video path (trace what draws into the compositor and encoders).
6. Start look intact: OLED Bioluminescent, true black, white solid cursor, cursor glow 0, the existing coded power toggles unchanged.
7. Merged manifest permissions equal the allowlist in Lane B, B0.
8. Contract keys in the master plan are spelled identically in Lane A's `FxKeys.kt` and Lane B's `audio/MicKeys.kt`.
9. Status log entry exists.

### Lane Specific Spot Checks

- **A:** glass equals window at 100/100 and the border is drawn inside rounded corners (A1); no `ResizeGrip`, no `SideDrawer` (A2, A6); glow curve test (A3); top bar alignment test (A4); text colors per uiMode (A5); scroll thumb tests (A7); every old `fx.` key still reachable (A11); existing toggles unchanged (the toggle redesign is a later phase, so any toggle redesign in a lane PR is a FAIL).
- **B:** CI triggers on PRs into `final-build` (B0); mic asked only on toggle (B1); camera unbinds on `fx.cam.on` off (B2); MP4 has audio and video tracks and the DSP tests pass (B3); replay buffer memory capped (B5).
- **C:** overlay never full screen by default, bubble works, `FLAG_SECURE` set (C1); FGS types correct and never started from background (C2); key 128 characters from `SecureRandom`, never logged, Keystore sealed (C6); HKDF and GCM tests, replay rejection, local-only binding (C7); hooks under 40 lines and marked (C9); the Bigo guide is honest about the virtual camera limit.

### Report Format

```
Lane X QA: OVERALL PASS | FAIL | BLOCKED
A1 PASS  evidence: ...
A2 FAIL  gap: ...  fix: ...
...
Rules: PASS
Files outside ownership: none
```


---


## 12. Later: App Icons and Toggle Lab

**Not in any current lane. Do not build this now.** Royalty wants it after the app is fully built. Until then every lane keeps the existing coded toggles (`PowerToggleView.kt`) exactly as they are. Royalty will first test toggle demos in the app icons area (her `OFFGRID_App_Icons` repo, which nobody touches until she says so), and only after she picks a favorite does anything get built into the app.

Planned direction, recorded so nothing is lost:
### Toggle Redesign and Toggle Demo Screen (Planned)

Royalty says the current coded toggles (a circle with a power symbol, `PowerToggleView.kt`) missed. When this phase starts (not before), replace them app-wide:

- **Shape:** a pill or oval **track** with a round **knob** that slides **left for off** and **right for on**. Inside the knob is a clean power on/off icon (the standard IEC power symbol: an open circle with a vertical line), drawn in code.
- **Glow:** the knob ring and the track get a **bioluminescent glow** (soft neon bloom in the glow color, layered like A3). On is bright; off is dim. No swirls, no swishes, no liquid, no gradients that move.
- **Variants:** **Pure Black** (black track and knob, white or neon icon and glow) and **Pure White** (white track and knob, black icon, neon glow). **Default is Follow system:** white toggles on the black (dark) system theme and black toggles on the white (light) system theme, switching live with `Configuration.uiMode`, with the glow color taken from the phone's system accent colors (Material You `system_accent1_*`, fallback Electric Purple `#B026FF`). The user can override this in 📁 Themes, then 📁 Toggle style: Follow system (start), Pure Black, or Pure White, plus a glow color choice of System accent (start) or any color. Keep the Toggle glow setting.
- Motion: the knob slides in 160 ms with a slight ease out; with Remove animations on it jumps. 48 dp minimum touch target; the track is at least 52 by 30 dp at Font size 50.
- Drawn in Canvas only, sharp at every density, no image assets. Every toggle in the app uses it (grep that no old toggle drawing remains).
- **Toggle Demo screen**, reachable from Panel settings, then 📁 Themes, then "Toggle Demo": two pages side by side or swipeable, one **black page** and one **white page**, each showing many variations of this style in a grid: glow strength (0, 25, 50, 75, 100), track thickness (thin, normal, thick), knob size (small, normal, large), and glow colors (white, Electric Purple, Cyan Glow, Hot Magenta, Toxic Lime, UV Violet). Every sample is live: tap to flip it on and off. Each sample has a "Use this" button; choosing one saves its parameters (style, glow strength, thickness, knob size, glow color) as the app-wide toggle look, survives restart, is included in Presets, and Reset all returns to the start look (Follow system, system accent glow, normal size).
- Typed 0 to 100 boxes for the chosen toggle's glow, thickness and knob size in 📁 Themes, then 📁 Toggle style, so Royalty can fine tune. No sliders.

Acceptance (when this phase starts): no `PowerToggleView` circle-only drawing left (replaced or rewritten in place); a Robolectric test renders on and off for both variants and checks the knob x position (left when off, right when on); the Toggle Demo opens from 📁 Themes, shows both pages, and "Use this" changes every toggle in the app immediately; screenshots of both demo pages in the PR.


---


## 13. Rex Ideas (Optional, Bold, Inside the Rules)

These are optional. Build them only after your lane's required items pass, and only in your own files. Each must keep every standing rule: no face shown, panel never in the video, no more than 3 flashes per second, on-device only, no sliders.

1. **Hologram mode (Lane B, new layer).** Royalty appears as a glowing particle silhouette that follows her body but never shows her face. Use on-device person segmentation (ML Kit Selfie Segmentation with the bundled model, or a small TFLite model in the APK; no cloud) to get a body mask, then draw only particles and a neon rim inside the mask over black or the cloak void. The camera picture itself is never drawn. The head region is always filled with particles, never pixels. Typed settings: particle count, glow, drift, hue. If segmentation fails, fall back to Blackout (face safety rule).
2. **Viewer-triggered effects (Lane C plus Lane B).** A tap on the **remote phone** fires emoji rain, shatter or any Fire action instantly (C8 control messages already carry this). Reading a key word from Bigo's chat is **not allowed**: OFFGRID cannot read another app's screen or chat without an accessibility-service scrape, which breaks platform rules and risks bans. Safe alternative: Royalty (or a helper holding the remote phone) watches chat and taps a big "Chat said it" button that fires the chosen effect, and a typed "Trigger word" note reminds them which word maps to which effect.
3. **Beat-reactive glow (Lane B, B4).** Glow border, laser writing glow and the panel glow pulse with the mic's beat. Cap brightness swings at 3 per second. Panel side needs a tiny hook from Lane A later.
4. **Cinematic go-live intro (Lane A for the panel, Lane B for the video).** On "Go live" (a new action), the panel boots like a terminal: black, a blinking white cursor, typed boot lines ("OFFGRID ONLINE", "FACE SHIELD: ON", "MIC: ON") at Typing speed, then a UV glow sweep runs around the border and the panel collapses to its bubble. In the video, an optional 3 second intro layer (glow sweep plus the laser-written word OFFGRID) plays once. Skippable with one tap, off under Remove animations.
5. **AR-style 3D floating emojis (Lane B, new layer).** Emojis orbit Royalty in a tilted ellipse that follows the face tracker's position and size, drawn in Canvas with fake depth (scale, blur and alpha by z, emojis behind the head are drawn before the face diamond, so the face stays covered). Typed settings: how many, orbit size, speed, tilt. No 3D engine, no Three.js.
6. **Ghost cursor sign-off.** When Royalty ends a recording, the laser writes her chosen sign-off word and the glow border fades out, baked into the last 2 seconds.

---

## 14. How to Start (Every Copilot)

1. Read this file and your lane file completely. Reading other lanes is for context only; do not work on them.
2. `git checkout final-build && git checkout -b <your lane branch>`.
3. Build and run the tests once before changing anything; note the baseline in your PR.
4. Work in small commits with clear messages. Push often.
5. Open your PR into `final-build` early as a draft with the exact title from section 5, then mark it ready when your Done Means list is met.
6. Append to `08 Chat History/Agent Status Log.md` after each meaningful step.
7. Never merge. Rex (Agent 04) reviews, Lane D checks, Royalty phone tests and gives the go.
