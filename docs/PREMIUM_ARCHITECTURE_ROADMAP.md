# OFFGRID-Compositor: Premium Architecture Roadmap

> **STATUS: PROPOSAL. NOT APPROVED.** Nothing in this file is approved, merged, or built. Every item waits for Royalty's go.
> **Repo:** `github.com/stillwaters23/OFFGRID-Compositor` · **Branch read:** `final-build` @ `be674e1` (Merge PR #38, `fix_ui_and_camera`; includes `0691c56`, the headless Media3 AR Video Deck scaffold)
> **Target:** Samsung Galaxy S25 Ultra · Android 15 / SDK 35 · CameraX + OpenGL ES compositor · Phone 2 remote over local Wi-Fi · Broadcast through Bigo Live screen share
> **Date:** Thu Oct 15, 2026 (CT) · **Chair:** Rex · **Written by:** Grok Bot, from the real code at `be674e1` plus cited web research
> **Honesty note:** The roundtable below is a **simulated** council. The seats are roles, not real people. Their arguments rest on the code read at `be674e1` and on the sources cited in [§18 Sources](#18-sources). Where something could not be checked (on-device behavior, Samsung One UI quirks), it is marked **UNVERIFIED**.
> **Merge rule:** `main` stays untouched. Coding agents open PRs against `final-build` from their own lane branches. **Only Royalty merges.**

---

## 1. Executive summary (phone scan)

- **The broadcast surface is the whole screen.** Bigo's screen share records everything shown on the S25. That includes the HUD bar (`control_bar`: "Camera is off…", "Record 30 s"), the `[OFFGRID]` pill, and the floating overlay. So the cleanup is a layout change, not a shader change. Proposed **Broadcast Lock** hides every bit of UI chrome.
- **The stretched-lion bug has three causes in the code, ranked.** (1) The stage's size and transform come from *whichever output is biggest* (`outputs.maxBy{w*h}`), so the 16:9 VIDEO_CAPTURE output can drive the picture on screen, and which output wins depends on arrival order at cold start. (2) The CameraX `ViewPort` ratio is taken once at the first bind, before the layout settles. (3) AR clips are drawn full-frame with no aspect correction. Fix: the PREVIEW output becomes master, the camera rebinds when the layout changes, and the deck uses its real video size. See [§10.1](#101-bug-stretchedstagelion-on-first-launch).
- **Android 15 auto-lock is real and documented by Google.** `WindowManager.addScreenRecordingCallback()` + `DETECT_SCREEN_RECORDING` (protection level **normal**, API 35) reports when *our own activities* are being recorded. It's the auto trigger. A manual Broadcast Lock (on S25 and from Phone 2) is the always-works fallback. **The CI allowlist must add `DETECT_SCREEN_RECORDING`, and that needs Royalty's approval.**
- **New design system, "Lumen".** True OLED black (`#000000`) canvas, bespoke vector glyphs (zero OS emoji), laser lettering baked into each folder's shape, glow color/strength/on-off per icon and globally, bioluminescent sliders and left-to-right toggles. No steel, no titanium, no metal. The System black/white themes stay as they are.
- **Folders are rebuilt around what Royalty does on stream.** 12 top-level folders (11 categories plus Sets), each with its own subfolders, max depth 3. Today's "Size" folder mixes panel geometry with font size and typing speed, "Display" mixes a broadcast-visible test shape with panel chrome, and "Titanium frame" breaks the no-metal rule. 40+ item moves are listed one by one in [§9](#9-folder-information-architecture).
- **Phone 2 becomes master controller.** Folders, glow toggles, deck commands (play/pause/loop/reverse/skip/crossfade), and **chunked, encrypted, resumable video transfer** to the S25 over the existing paired channel. No new network permissions.
- **Recording becomes unlimited,** with Record and Screenshot icons in the top bar next to Camera and Mic, safe file segments, and nothing drawn on the video.
- **Process.** Research and review on a fixed cadence, Rex picks the agent and model per task, and every change goes through a PR into `final-build` with CI evidence. Royalty alone merges. `main` is never touched.

---

## 2. CEO Mandate Checklist

| # | Royalty's mandate | Where it is covered | Status |
|---|---|---|---|
| 1 | No default OS emojis; bespoke glowing vector icons | §8.3 Icon system · §9.3 move list (Emoji rain → Particle Rain) · Phase 2 | **Covered** (also strips 📁 from ~64 code lines and the `DEFAULT_SET` emoji string) |
| 2 | Folder titles in laser lettering baked into folder geometry | §8.4 Laser lettering · Phase 2 (`FolderIconView`, `LaserText`) | **Covered** |
| 3 | OLED black + customizable bioluminescent glow (color, %), per-folder swappable icons, many icons per folder, every icon customizable, glows on/off | §8.1 tokens · §8.2 glow engine · §8.3 icon packs · §9.4 `LOOK › Icons` | **Covered** |
| 4 | Sliders replace 0–100 typed inputs; fluid left-to-right toggles | §8.5 controls · Debate D2 (CI "no sliders" guard must change) · Phase 2 | **Covered.** Needs Royalty approval to change the CI guard |
| 5 | Clean broadcast surface: no `[OFFGRID]` badge, no "Camera is off" text, no "Record 30 s" HUD | §10.2 · Phase 1 | **Covered** |
| 6 | Unlimited recording via a top-bar Record icon next to Mic and Camera, plus a Screenshot icon; mic-block and camera-block toggles | §10.3 · Phase 1 | **Covered.** Mic hard-block exists today; **camera-block is a new key (gap today, proposed)** |
| 7 | No laser reels in the header; W/H tap-to-type keypad boxes; reels fine in inner folders, uncrowded | §8.5 · §9.4 `PANEL › Size & Position` | **Covered** (keeps the PR #38 header boxes; keeps `SizeReelBank` inside folders only) |
| 8 | Fix the stretched-lion first-launch render bug | §10.1 · Phase 1 | **Covered** (diagnosis from code; device check by Royalty at the end) |
| 9 | Broadcast Lock on screen share (Android 15 callback); S25 opens straight to the clean loop surface | §10.4 · Debate D3 · Phase 1 | **Covered.** Permission allowlist needs approval |
| 10 | Phone 2 master controller: folders, glow toggles, import/send videos over Wi-Fi, loop/reverse/skip/crossfade | §10.5 · §10.6 · Phase 3 | **Covered** |
| 11 | AR Video Deck: headless ExoPlayer, seamless looping | §10.6 · Phase 1 (aspect) · Phase 3 (A/B crossfade) | **Covered** |
| 12 | Keep white/black System themes; no steel/metal themes; no AI-slop; $777 luxury, nostalgic, executive | §8 · Debate D1 · §9.3 (archive Titanium frame / Sapphire glass) | **Covered** |
| 13 | Folder renames, own folders and subfolders, item-level move list (e.g. misplaced page layout/size) | §9.1–§9.6 | **Covered** |
| 14 | Research team: streaming apps, the world's best apps, reviews, color psychology, UX; sources; feature-borrowing table | §4 · §5 Steal Like an Artist · §18 | **Covered** |
| 15 | Collaborative council minutes, rounds of up to 7, Rex chairs and takes notes, in-character roles | §3 · §6 · §7 | **Covered** (simulated, labeled as such) |
| 16 | Rex picks the model per task and lists assignments | §12.7 | **Covered** |
| 17 | KJV scripture foundation | §16 | **Covered** (verified wording) |
| 18 | Autonomous continuous improvement plan | §14 | **Covered** |
| 19 | Nothing merged without Royalty's go; `main` untouched; PRs against `final-build` by coding agents | Header · §12 · §14 | **Covered** |
| 20 | MediaPipe cloak/ghost modes and floating overlay kept | §9.4 `CAMERA › Body & Cloak` · §10.4 (overlay hides under lock) · Rex critique #6 | **Covered**. One open risk: the floating overlay is *also* captured by screen share. Handling is proposed |
| 21 | *(Added by Royalty)* Innovation Lab: 10–15 original features, top 5 ranked; handoff-ready acceptance criteria | §11 · acceptance boxes in §9.6, §10.x, §11, §12 | **Covered** (15 ideas, top 5 ranked) |

**Known gaps (stated plainly):**
- On-device behavior of `addScreenRecordingCallback` on Samsung One UI 7/8 with Bigo's capture is **UNVERIFIED**. It's documented for AOSP and needs Royalty's end-of-build test.
- Camera **block** (distinct from camera off) doesn't exist yet. It's proposed as a new `FxKeys.CAMERA_BLOCKED`.
- **Reverse playback** isn't native to ExoPlayer. It's proposed as a reversed proxy file made at import (§10.6), so the first reverse of a new clip takes a short processing wait.
- No real users were card-sorted. The folder tree is backed by research, not by a study (§14 proposes a 15-person card sort via Royalty's audience, optional).

---

## 3. How the council worked

**Format.** Royalty asked for a big roundtable. Rex chairs, mostly listens, and takes notes. Members speak in **rounds of up to 7**, and each speaker must build on or challenge someone before them, never repeat them. The Research Team speaks first and hands findings to Design. Design hands specs to Engineering. Engineering hands risks to QA and Security. The streamers critique every round from the user's seat. Disagreements are logged in §7 and closed by consensus. Rex breaks ties only when consensus fails.

**The question every seat answered:** *"If my account and reputation depended on this tool being flawless, intuitive and undetectable, how would I build it?"*
"Undetectable" here means **viewers see zero tool chrome.** The broadcast looks like a pristine, intentional production. It does **not** mean evading any platform's rules or detection systems. App virtualization and camera hooking were rejected (ban and security risk) and are not proposed anywhere.

### 3.1 Seats (25)

| Seat | Role | Lens |
|---|---|---|
| **Rex** | Chair | Notes, tie-breaks, model assignment, merge discipline |
| **Nova** | Elite streamer: Bigo top-tier host, gift economy | "Does it make me look expensive on camera?" |
| **Vesper** | Elite streamer: multi-platform (Bigo/TikTok/Kick), dual-phone rig | "Can I run it blind, mid-PK battle?" |
| **Kade** | Elite streamer: long IRL/talk sessions, 4–6 h | "Will it survive hour five?" |
| **R1 Ilse** | Research: information architecture | Menu depth, grouping, card sorting |
| **R2 Marco** | Research: competitive apps and reviews | What users love and hate in streaming apps |
| **R3 Juno** | Research: design excellence (award-winning apps) | Halide, Darkroom, Things, TE, Linear, Procreate, Arc |
| **R4 Sade** | Research: color science and dark UI | Color psychology, OLED, contrast |
| **D1 Theo** | UI psychology | Attention, cognitive load, trust |
| **D2 Aria** | Color psychology and palette | Glow hues, saturation, status signals |
| **D3 Felix** | Iconography and vector design | Glyph grid, strokes, glow-ready geometry |
| **D4 Mira** | Motion design | Timing, easing, unfold, continuity |
| **D5 Lux** | Typography and laser lettering | JetBrains Mono, baked titles |
| **D6 Hana** | Haptics and sound micro-interactions | Click engine, vibration grammar |
| **E1 Ozan** | EGL/GL rendering | Stage, viewport, transforms, glow cost |
| **E2 Priya** | CameraX | UseCaseGroup, ViewPort, effects, outputs |
| **E3 Leon** | Lifecycle and state | Activity lifecycle, prefs, migrations |
| **E4 Ravi** | Wi-Fi remote protocol | `RemoteSession`, `EncryptedChannel`, chunking |
| **E5 Noor** | Media pipeline | Media3 ExoPlayer, MediaCodec, recording |
| **E6 Kenji** | Android platform and app-state detection | Screen-recording callback, permissions |
| **E7 Bea** | Performance and thermal | 60 fps budget, ADPF, battery |
| **Q1 Dana** | QA lead | Test matrix, regressions |
| **Q2 Sol** | CI and release | GitHub Actions guards, allowlists |
| **X1 Ivo** | Security and privacy | Pairing, transfer integrity, data at rest |
| **A1 Reese** | Accessibility | Contrast, touch targets, reduced motion |

---

## 4. Research Team findings (R1–R4)

> The Research Team ran real web research on Thu Oct 15, 2026 and handed this packet to Design and Engineering before Round 2. Source numbers like [S12] point to [§18](#18-sources).

### 4.1 Streaming and compositor apps: what users love and hate (R2 Marco)

| App | Users love | Users hate | Lesson for OFFGRID |
|---|---|---|---|
| **PRISM Live Studio** (Naver) | Free, beginner-friendly, VTuber mode, multi-platform [S6] | Crashes and freezes after updates ("lucky if I can stream for an hour"), dropped streams, audio failing, previously free features moved behind paid plans, blurry imported images in the scene editor [S5][S6][S7]. Open GitHub issues on low frame rates and audio capture [S8] | **Stability beats features.** One crash mid-stream costs the room. Every release needs a soak test (§13). |
| **Streamlabs Mobile** | Multistream, overlays, Disconnect Protection [S10] | Drops "around the 30 min mark, then the 60 min mark", phone gets "astoundingly hot", Bluetooth audio breaks, logins break, $20/mo for a "barely functioning app" [S9][S10] | **Hour-five reliability and thermal budget** are product features (E7 Bea). |
| **Larix Broadcaster** | Pro protocols (SRT/RTMP/NDI), saved connections, fast and simple preview [S11][S12] | Learning curve, settings overwhelming, can't drag-place overlays on the preview (everything through parameter menus) [S11][S13] | **Direct manipulation on the stage**, deep settings hidden by default. |
| **CameraFi Live** | Overlays: image, text, video, chat, web [S14] | Widgets moved to paid and watermarked across the whole widget [S14] | **Never stamp anything on the user's picture.** This matches mandate 5. |
| **OBS Blade** (open source, Flutter) | Phone as a focused "cockpit" for a host, autodiscovery on the LAN, side-by-side on tablet, stream stats [S15][S16] | (Few complaints; scope is control only) | **The Phone 2 model:** control surface plus health stats, same network, nothing through the cloud. |
| **StreamYard** | "Simple, intuitive, low learning curve", reusable branded studios [S17] | Price; less granular than OBS [S17] | **Saved "sets"** (brand plus layout) beat endless knobs. See the Innovation Lab. |
| **OBS Studio** (desktop reference) | Industry-standard IA: General, Appearance, Stream, Output, Audio, Video, Hotkeys, Accessibility, Advanced [S18] | Its own docs warn: "Do not touch anything else in Advanced unless you absolutely know what you're doing" [S18] | **Group by function, quarantine Advanced.** |
| **CapCut** | Captions, easy mobile editor [S20] | "Transition organization is a nightmare", no search, lists that keep reshuffling, pro and free items mixed together [S20] | **Stable ordering, favorites, no mixed categories.** |
| **Blackmagic Camera (Android)** | Pro HUD: exposure, focus, fps, audio around the record area; swipe hides the HUD for clean framing [S21] | Overwhelming at first for beginners [S21] | **One gesture to clean the screen** is a pro expectation. |
| **Bigo Live (Game Live / screen share)** | Pick "Game Live", grant screen capture, leave Bigo, switch apps. A small Bigo overlay confirms it's broadcasting [S22] | (n/a) | Bigo captures **the whole screen**, so whatever OFFGRID shows is on air. |

### 4.2 Menu and folder information architecture (R1 Ilse)

- **Flat vs deep has no universal winner.** Broad menus help discovery but overwhelm. Deep menus bury content and disorient. Use clear hubs and orientation cues (breadcrumbs) [S23]. The "3-click rule" is false. Clear, progressively specific paths matter more than click counts [S24].
- **Progressive disclosure:** defer advanced or rare features to a secondary screen. "If you have so many features that you need 3 or more levels, consider simplifying… at least chunk your advanced features into groups that make sense" [S25].
- **Card sorting** reveals users' mental models (open sort to discover groups, at least 15 users). **Tree testing** checks the result [S26][S27].
- **Hick's law:** decision time grows with the log of the number of equally likely choices. In real menus, layout, familiarity, and visual search often matter more [S28a][S28b]. **Split menus** (frequent items promoted to the top) cut selection time 17–58% in the classic study [S28c].
- **Pro app patterns:** OBS groups by *function* (Audio, Video, Output…) [S18]. Lightroom mobile groups edits as **Light, Color, Effects, Detail, Optics**, with **Presets as a separate top tool** [S19]. Blackmagic keeps live controls on the viewfinder edge and hides them with a swipe [S21]. Arc gives each **Space its own color, theme, and icon**, with folders inside folders [S29a][S29b].

**R1's handoff to Design:** (a) group by *function* the way a streamer thinks, not by code history; (b) max **3 levels**; (c) a **Favorites/Quick row** at the top (split menu); (d) a breadcrumb laser trail; (e) one quarantined **System** folder for Advanced, Diagnostics, and Reset.

### 4.3 World-class app craft (R3 Juno)

- **Halide (Apple Design Award 2022):** "stay out of the way". Controls appear only while needed (focus loupe while dragging), gestures copy physical dials, everything within thumb reach [S30].
- **Darkroom (ADA 2020):** dark, photo-first canvas, minimal controls, simple symbols plus highlight colors [S31a][S31b].
- **Things 3 (ADA):** purposeful animation, the "Magic Plus" button you *drag* to where the new item belongs [S32].
- **Teenage Engineering OP-1:** four color-coded encoders mapped to on-screen elements. Color does the labeling, playful minimalism [S33].
- **Linear:** the 2024 redesign cut visual noise, raised hierarchy, and aligned icons and labels to the pixel, "something that you'll feel after a few minutes". Themes are generated from 3 variables (base, accent, contrast) [S34a][S34b].
- **Procreate QuickMenu:** a six-button radial menu you trigger with a gesture and flick toward; customizable, with multiple profiles [S35].
- **Spotify Encore:** shared foundation tokens (color, type, spacing, motion) under platform components; the accent is reserved for primary actions [S36].
- **Bang & Olufsen app (UX Design Award):** restrained dark premium UI; simplified "Beosonic" sound shaping instead of a raw EQ [S37a][S37b].
- **Rolex (digital):** restraint *is* the statement. One accent held "almost entirely in reserve", hairline borders, no ornament [S38].
- **Arc:** glowing gradient borders, per-Space color/icon, theme intensity control [S29a][S29c].

### 4.4 Color science, OLED and dark UI (R4 Sade)

- **Material dark guidance** recommends `#121212` over pure black for general apps and warns that saturated colors "vibrate" on dark surfaces, so use desaturated tones and keep WCAG AA 4.5:1 [S39]. *Logged as Debate D1, since Royalty mandates OLED black.*
- **OLED power:** dark mode saves about 3–9% of total power at typical 30–50% brightness and about 39–47% at 100% brightness [S40a][S40b]. A pure-black canvas helps most when the S25 runs at full brightness on stream.
- **Color psychology** (peer-reviewed review): effects are real but depend on context and learned associations [S41a]. Blue raised trust over red in three studies [S41b]. For luxury, **lower saturation reads as higher status and heritage**, and high saturation helps only when uniqueness is the goal [S41c].
- **Haptics (Android):** use `performHapticFeedback` constants, keep frequent haptics subtle and important ones stronger, stay consistent with the system, and skip the legacy vibrate APIs [S42a][S42b].

**R4's handoff to Design:** a black canvas; glow hues desaturated by default with a "Vivid" option; one reserved accent for live/danger states; contrast checked at every glow level.

### 4.5 Android platform facts (E6 Kenji, verified in official docs)

- **Screen recording detection (Android 15, API 35):** `WindowManager.addScreenRecordingCallback(Executor, Consumer<Integer>)` returns the current state and calls back on change. States: `SCREEN_RECORDING_STATE_VISIBLE` / `SCREEN_RECORDING_STATE_NOT_VISIBLE`. *"An app is considered visible in screen recording if any activities owned by the registering process's UID are being recorded."* Requires `android.permission.DETECT_SCREEN_RECORDING`, protection level **normal** (install-time, no prompt). The official sample registers in `onStart` and unregisters in `onStop` [S1][S2][S3][S4].
- **Android 15 QPR1+** shows a status-bar chip during any media projection and auto-stops projection on screen lock [S1]. *Implication: the S25 must not auto-lock mid-stream. `keepScreenOn` is already set in `activity_main.xml`.*
- **Media3 looping:** use `REPEAT_MODE_ONE` (no manual seek-to-0). For clipped or multi-item transitions, video renderer prewarming reduces freezes [S43a][S43b][S43c].

---

## 5. Steal Like an Artist

> Every adaptation stays inside OFFGRID's language: **OLED black, bioluminescent glow, laser type, bespoke vector glyphs. No steel, no metal, no emoji.**

| # | App | Feature | Why it works | How OFFGRID adapts it | Phase |
|---|---|---|---|---|---|
| 1 | Halide [S30] | Controls appear only while touched (focus loupe) | Zero clutter at rest, full power on demand | Sliders show a glowing value halo only while dragging. Folder detail chrome fades after 2 s idle | 2 |
| 2 | Blackmagic [S21] | One swipe hides the whole HUD | Instant clean frame | **Two-finger swipe down = Broadcast Lock** on the S25. Same command from Phone 2 | 1 |
| 3 | Procreate [S35] | Radial QuickMenu, flick to select, profiles | Muscle memory, no menu diving | **Lumen Ring:** long-press the stage (with lock off) for 6 glowing glyphs around the thumb; flick to fire. Profiles = Sets | 4 |
| 4 | Things 3 [S32] | Drag the + to where the item belongs | Placement is the command | Drag a deck clip glyph onto a folder or a Set slot on Phone 2 to file it | 3 |
| 5 | TE OP-1 [S33] | Color-coded encoders mapped to screen | Color does the labeling | Each top-level category has one **signature glow hue**; its sliders and toggles inherit it | 2 |
| 6 | Linear [S34a] | 3-variable theme generator (base/accent/contrast) | Infinite themes that never clash | **Glow Studio:** pick Hue + Intensity + Halo; every token is derived from those three | 2 |
| 7 | Arc [S29a][S29c] | Per-Space color, icon, intensity slider | Personal ownership (the IKEA effect) | Per-folder glyph, glow hue, and intensity; global master on top | 2 |
| 8 | Spotify Encore [S36] | Shared tokens; accent reserved for primary actions | Consistency at scale | `LumenTokens.kt` is the only source of color, motion, and type. The red "live" accent is reserved for Record and Broadcast Lock | 2 |
| 9 | Lightroom [S19] | Edits grouped as Light / Color / Effects…; Presets separate | Function-first mental model | Category tree §9; **Sets** live as a top-level tool, not buried | 2 |
| 10 | B&O Beosonic [S37a] | Simple sound shaping instead of a raw EQ | Luxury = fewer, smarter controls | **Voice Character pad:** one 2-axis pad (warm↔bright, close↔room) drives Low/Mid/High EQ plus reverb | 4 |
| 11 | Rolex [S38] | One accent held in reserve; hairlines | Restraint reads as expensive | Glow is **off on idle items, lit on active**. Hairline 1 dp edges. No drop shadows, no gradients except the glow | 2 |
| 12 | Darkroom [S31b] | Content-first dark canvas, simple symbols | The picture is the hero | The panel never covers the face zone by default (top-right anchor). Glyphs only, no badges | 2 |
| 13 | OBS Blade [S15] | Phone cockpit plus stream health | Host stays in flow | Phone 2 **Health strip:** fps, thermal, battery, recording time, link quality | 3 |
| 14 | StreamYard [S17] | Reusable branded studio | One tap to "my look" | **Sets:** camera, effects, deck clip, glow, and voice saved as one recallable scene | 4 |
| 15 | Android haptics [S42a] | System constants, subtle-to-strong ladder | Feels native and premium | `HapticGrammar.kt`: TICK for slider detents, CONFIRM for lock on, REJECT for blocked actions | 2 |

---

## 6. Meeting minutes (simulated roundtable)

### Round 1: "What breaks a reputation?" (users and research, 7 seats)

1. **Nova (streamer):** "My viewers tip the *look*. One floating `[OFFGRID]` pill or a 'Camera is off' line and I look like I'm running a gadget, not a show. The surface has to look intentional at all times, even in the three seconds while I fumble."
2. **Vesper (streamer):** "Building on Nova: in a PK battle I'm not looking at the S25. I'm on Phone 2 or on the guest. If I have to open folders on the S25 *while Bigo is capturing*, the whole room watches me dig through menus. Any S25 menu during a share is a failure."
3. **Kade (streamer):** "Both right, but my killer is hour five. Streamlabs users report drops at the 30- and 60-minute marks and a hot phone [S9]. A 30-second recording cap is the wrong instinct. I want to hit record once and forget it until the stream ends."
4. **R2 Marco (research):** "The reviews back all three. PRISM's top complaints are crashes after updates and features moving behind paywalls [S5][S6]. CameraFi watermarks widgets [S14]. Users forgive missing features. They don't forgive a broken or branded broadcast."
5. **R1 Ilse (research):** "Vesper's point is IA. Today the S25's top level is 8 feature folders plus **18 platform folders**, so 26 items before you reach anything. NN/g says use clear hubs and progressive disclosure, and chunk anything past 3 levels [S23][S25]. Platform guides are reference material, not live controls. They belong one level down."
6. **R3 Juno (research):** "To make 'expensive' concrete: Halide and Blackmagic both treat a clean frame as one gesture away [S30][S21]. Rolex and B&O read as luxury through restraint: one accent, hairlines, no ornament [S38][S37a]. 'Steel', 'titanium', and 'sapphire' look like 2014 skeuomorphism. Royalty already banned metal."
7. **R4 Sade (research):** "One caution before Design runs with neon: Material warns that saturated color vibrates on black [S39], and luxury research says *lower* saturation reads as higher status [S41c]. Glow should default to desaturated bioluminescence (sea-firefly cyan, deep teal, soft amber) with 'Vivid' as a choice, not the default."

**Rex notes (R1):** The surface is the product. Recording cap is gone. S25 menus disappear during a share. Platform guides drop a level. Default glow is desaturated.

### Round 2: "What does $777 look like?" (design, 7 seats)

1. **D1 Theo (UI psychology):** "Taking Ilse's hubs: a streamer under stress uses *recognition, not recall*. Eleven category glyphs, each with a fixed position and its own hue, so the hand learns the map. A **Quick row** (split menu [S28c]) holds the 5 controls used every stream."
2. **D2 Aria (color):** "Building on Theo and Sade: each category gets a **signature hue** at 60% saturation (TE-style color-as-label [S33]). Red is reserved for *on-air* states only: Record, Broadcast Lock, mic block. Royalty can override any hue, but red stays reserved by default so 'danger' always means danger."
3. **D3 Felix (icons):** "No OS emoji anywhere. That means the `📁` title prefixes on ~64 code lines, the `❤️🔥✨😂🎉💎` default set in `EmojiRainLayer.DEFAULT_SET`, and every emoji in strings. Glyphs sit on a 24-unit grid with 1.75-unit strokes, rounded joins, and a **closed outer contour** so the glow shader has a clean edge to bloom. Each folder holds a *pack* of 8–12 alternates the user can swap."
4. **D4 Mira (motion):** "Felix's closed contours let me animate the glow itself: a 1.2 s 'breath' at 4% amplitude on the active item only. Everything else sits still (Rolex restraint). Folder open stays a hologram unfold at 220 ms with an emphasized-decelerate curve. Reduced Motion drops it to a 90 ms fade."
5. **D5 Lux (typography):** "Laser lettering *inside* the folder shape: the title is drawn as a stroked path clipped to the folder's tab, in the same glow pass, not as a TextView hung below. JetBrains Mono Bold stays (it's licensed and bundled) with +6% tracking for titles. That's the nostalgic terminal soul."
6. **D6 Hana (haptics):** "Mira's motion needs a body. Slider detents at every 5% use `CLOCK_TICK`, toggle on/off use `TOGGLE_ON`/`TOGGLE_OFF` (API 34+), lock engaged uses `CONFIRM`, a blocked action uses `REJECT`, per Android's subtle-to-strong ladder [S42a]. The existing ClickerSoundEngine stays opt-in and never plays into the mic path."
7. **A1 Reese (accessibility):** "Push-back on all of us: glow can't be the *only* state signal. Toggles need position (knob left/right) **and** fill. Touch targets 48 dp minimum. Text keeps 4.5:1 against black at the *lowest* glow setting [S39]. Also, 'glow %' at 0 must still leave a readable 1 dp hairline."

**Rex notes (R2):** Signature hue per category, red reserved. Closed-contour vector glyph packs. Title baked into folder geometry. Haptic grammar. A11y floor at glow 0%.

### Round 3: "Make it flawless" (engineering, 7 seats)

1. **E1 Ozan (GL):** "I traced Royalty's stretched-lion bug. `paintStage()` sizes the stage from `slots.maxBy { it.width * it.height }` and uses **that slot's** `updateTransformMatrix`. But `CompositorEffect` targets `PREVIEW or VIDEO_CAPTURE`, so up to two outputs with different crops and aspects exist. If the 16:9 video output wins, or arrives first at cold start, the screen gets the video's geometry. Fix: the PREVIEW slot is master for the stage, always. Recorders get their own crop."
2. **E2 Priya (CameraX):** "Building on Ozan: `bindCamera()` builds the `ViewPort` from `previewView.width/height` **once**. On first launch, bind runs from the permission callback, before the immersive layout settles. A trip to the background calls `unbindCamera()`/`startCamera()` through `applyCapture()`, so the second bind sees final dimensions. That's exactly 'normal after switching apps'. Fix: bind after layout and window focus, and rebind when the view's aspect changes."
3. **E5 Noor (media):** "Third cause if the 'lion' is a deck clip: `StagePainter.drawAr()` calls `drawCamera(texture, matrix, …)` over the full target. There's no video-size awareness, so a 16:9 lion clip on a 9:19.5 stage is stretched. Add a `Player.Listener.onVideoSizeChanged` hook and fit/fill math. For mandate 10: A/B players give true crossfade. Reverse needs a reversed proxy file; ExoPlayer can't play backwards."
4. **E3 Leon (lifecycle):** "For Broadcast Lock, the state lives in one place: `BroadcastLockStore` (persisted, observable). Sources are the screen-recording callback, the manual toggle, and Phone 2. **Cold start renders the stage first, panel hidden, until lock state is known.** No flash of folders, ever."
5. **E6 Kenji (platform):** "Confirmed in docs: `addScreenRecordingCallback` needs `DETECT_SCREEN_RECORDING`, a *normal* permission, API 35 [S2][S4]. Caveat: it reports when **our activities** are visible in a recording. When Royalty opens OFFGRID while Bigo is sharing, we flip to VISIBLE as our window appears, which is exactly the trigger we want. It can't tell us Bigo is sharing while we're in the background. That's fine. Samsung behavior is UNVERIFIED."
6. **E4 Ravi (protocol):** "Phone 2 as master: today `RemoteMessage` caps a frame at 1024 bytes and every field at 256 chars, with a fixed action allowlist. Good for commands. For video import, add a **separate packet type** `RemotePacket.FileChunk` (256 KiB chunks, SHA-256 per file, resumable offsets) on the same encrypted session, interleaved so control messages never wait behind a chunk more than one chunk-time."
7. **E7 Bea (performance):** "All of this inside 16.6 ms on the GL thread. Glow on the *panel* is View/Canvas work, separate from the stage, so cache glow bitmaps per (glyph, hue, intensity) and redraw only on change. On the *stage*, two deck decoders plus camera plus the segmenter is heavy; set an ADPF hint session and drop to one decoder after a crossfade completes."

**Rex notes (R3):** Three-part render fix. Lock store with stage-first cold start. Chunked transfer as a new packet type. Thermal discipline.

### Round 4: "Prove it, protect it" (QA, CI, security, plus streamer rebuttal, 7 seats)

1. **Q1 Dana (QA):** "Every item gets a pass/fail criterion I can check *from CI*, because the standing rule is no phone tests between rounds. So: pure-Kotlin `StageGeometry` with unit tests for every output combination; Robolectric for the lock state machine; a cold-start emulator run as an *optional* job."
2. **Q2 Sol (CI):** "Two guards change and both need Royalty's sign-off. (a) The permission allowlist adds `DETECT_SCREEN_RECORDING`. (b) The 'Check there are no sliders' step greps `SeekBar|RangeSlider|material.slider|<Slider|Slider\(` and fails the build. Royalty's new mandate *requires* sliders. We don't rename the class to sneak past the grep. We change the guard openly: ban stock `SeekBar`/Material sliders, allow only the bespoke `BioSlider` view."
3. **X1 Ivo (security):** "On Ravi's transfer: accept files **only** from a paired, encrypted session, only video MIME (mp4/webm/mkv), max 512 MB per file, written to app-private storage (`filesDir/deck/`). No storage permission. The hash is verified before the file appears in the deck. Broadcast Lock *unlock* from Phone 2 is allowed because the channel is paired and encrypted, but on the S25 itself unlock needs a deliberate gesture, so a viewer-visible accidental unlock can't happen."
4. **Vesper (streamer):** "Rebuttal to Ivo: if unlock on the S25 is a secret 3-second gesture, *I* will forget it at 2 a.m. Make it a long-press on the top-right corner for 1.5 s with a haptic ramp, plus a one-time coach mark shown only when *not* recording."
5. **Kade (streamer):** "Rebuttal to Bea and Noor: infinite recording at hour five means file size and heat. Split into seamless segments every 30 minutes so one corrupt file never loses the whole stream. And show the elapsed time in the top bar, not on the video."
6. **Nova (streamer):** "On Felix's packs: give me *Sets*. One tap, and my camera look, glow color, deck clip, and voice all switch together. That's what StreamYard got right [S17]. I'd pay $777 for that alone."
7. **A1 Reese (accessibility):** "Agree with Vesper's long-press, and add: every gesture has a visible alternative when not broadcasting (a Lock glyph in the top bar). And the Phone 2 app gets the same contrast and target rules."

**Rex notes (R4):** Two CI guard changes need Royalty's approval. Transfer hardening. Unlock gesture 1.5 s plus top-bar glyph. 30-min segments. Sets go to the Innovation Lab, top 5.

### Round 5: Consensus vote (Rex reads back, all 25 seats)

All 10 debates in §7 were closed. Votes: 25/25 on Broadcast Lock design, the render fix, and the IA tree. 23/25 on true black (`#000000`), with R4 and A1 dissenting and accepting it with a contrast floor (D1). 25/25 on "change the CI guard openly, with Royalty's approval" (D2).

### 6.1 Rex's chair notes (summary)

- The broadcast is the **entire S25 screen**. Every pixel OFFGRID draws while Bigo shares is on air. That one fact drives Broadcast Lock, the HUD removal, and the folder lockout.
- Root causes beat patches: the render bug is **output selection plus viewport timing plus clip aspect**, not "something in onResume".
- Ship in an order that protects the stream first (Phase 1), then makes it beautiful (Phase 2), then makes Phone 2 the master (Phase 3).
- Two approvals are needed before Phase 1 can be CI-green: the `DETECT_SCREEN_RECORDING` allowlist entry and (for Phase 2) the slider guard change.
- I assign agents and models per task in §12.7. Royalty merges. `main` is never touched.

---

## 7. Debate log (major decisions)

| ID | Decision | Proposed by | Challenged by | Resolution (consensus) |
|---|---|---|---|---|
| **D1** | Canvas color | D2 Aria: `#000000` true OLED black (Royalty's mandate) | R4 Sade / A1 Reese: Material advises `#121212`, saturated color vibrates [S39] | **True black canvas** for the stage and panel background (mandate plus OLED power at full brightness [S40a]). *Surfaces* (folder cards) use `#07090A` with a 1 dp hairline so depth exists. Glow desaturated by default. Text contrast ≥ 4.5:1 at every glow level |
| **D2** | Sliders vs CI "no sliders" guard | D1 Theo: bioluminescent sliders per mandate 3 | Q2 Sol: the CI guard fails any `Slider(` match; renaming to dodge it is dishonest | **Change the guard openly** (needs Royalty's approval): ban `SeekBar`, `RangeSlider`, `com.google.android.material.slider`; allow `BioSliderView`. Update the `NumberItem` doc ("Never a slider") and the `Panel settings` notes ("No sliders here…") |
| **D3** | Auto vs manual Broadcast Lock | E6 Kenji: auto via the screen-recording callback | Vesper: "If the API misses Bigo on Samsung, I'm exposed" | **Both.** Auto (API 35 callback) + manual top-bar Lock glyph + two-finger swipe down + Phone 2 command. Any source can lock. Unlock needs a deliberate action. Default on cold start: stage first, panel hidden until state is known |
| **D4** | Menu depth | R1 Ilse: max 3 levels | D1 Theo: "Effects has 19 sub-effects; 3 levels will force a long list" | **Max 3 levels** (Category › Group › Item). Effects split into 4 groups of 3–7. A Quick row on top. Long lists get a search field on Phone 2 only |
| **D5** | Unlock from Phone 2 | X1 Ivo: allow (paired, encrypted) | Kade: "What if someone grabs Phone 2?" | **Allowed**, but Phone 2 shows a hold-to-confirm (1 s) and the S25 shows nothing until unlocked. Pairing can be revoked from the S25 System folder |
| **D6** | Transfer channel | E4 Ravi: same encrypted session, new packet type | E7 Bea: a big transfer will lag control commands | **Same session, 256 KiB chunks, priority queue** (control messages jump the queue). Transfers pause automatically while a crossfade or effect fire is in flight |
| **D7** | Glow cost and burn-in | D4 Mira: breathing glow on active items | E7 Bea / A1 Reese: static bright glow on OLED risks burn-in and battery drain | Breath only on the active item. Idle items glow ≤ 30%. Panel auto-dims after 20 s idle. All of it is off under Broadcast Lock (the panel isn't visible then anyway) |
| **D8** | `[OFFGRID]` pill replacement | D1 Theo: tiny dim orb | Nova: "Anything visible is on air" | **No visible pill.** The panel opens by long-pressing the top-left corner (1.0 s), the top-bar glyph when not locked, or Phone 2. A one-time coach mark (never during recording or lock) |
| **D9** | Infinite recording storage | E5 Noor: one file until stop | Kade: one corrupt file loses five hours | **30-min seamless segments** (`OFFGRID_yyyyMMdd_HHmm_partN.mp4`), free-space guard (stop and alert Phone 2 at < 2 GB), elapsed time in the top bar only |
| **D10** | Icon format | D3 Felix: VectorDrawable XML | E1 Ozan: VectorDrawables can't carry per-icon glow cheaply | **Path data in Kotlin (`GlyphPaths.kt`) rendered through `Path` + cached glow bitmap** (`BlurMaskFilter` once per hue/intensity). Also exported as AVD for launcher/notification use |

---

## 8. The "Lumen" design system (mandates 1–4, 7, 12)

### 8.1 Tokens (single source: new `terminal/lumen_tokens.kt`)

| Token | Value (default) | Notes |
|---|---|---|
| `canvas` | `#000000` | Stage and panel background. True OLED black (D1) |
| `surface` | `#07090A` | Folder cards. Almost black, gives depth without gray |
| `hairline` | glow hue at 22% alpha, 1 dp | Never disappears, even at glow 0% (A1) |
| `ink.primary` | `#EAF6F4` | ≥ 15:1 on canvas |
| `ink.secondary` | `#9FB3B0` | ≥ 7:1 on canvas |
| `glow.hue` | per category (below), user-overridable | |
| `glow.intensity` | 0–100%, default 55% | Global master × per-icon value |
| `glow.halo` | 0–100%, default 40% | Blur radius 2–14 dp |
| `accent.live` | `#FF3B5C` (reserved) | Record, Broadcast Lock, mic block only |
| `motion.unfold` | 220 ms, emphasized decelerate | Reduced Motion: 90 ms fade |
| `motion.breath` | 1.2 s, ±4% intensity | Active item only (D7) |
| `type.title` | JetBrains Mono Bold, +6% tracking | Laser lettering |
| `type.body` | JetBrains Mono Regular | |
| `grid` | 8 dp; touch targets ≥ 48 dp | Matches agent rules ("8dp grid") |

**Signature hues** (desaturated about 60% by default, "Vivid" lifts to 100%):

| Category | Hue | Name |
|---|---|---|
| VIDEO DECK | `#4FE3D0` | Sea-firefly cyan |
| EFFECTS | `#9B8CFF` | Jellyfish violet |
| CAMERA | `#6FD3FF` | Abyss blue |
| TEXT & TITLES | `#F4E3A1` | Candle amber |
| VOICE & SOUND | `#7CF2A6` | Foxfire green |
| RECORD & CAPTURE | `#FF6F86` | Coral (near the reserved red, not equal to it) |
| STAGE | `#B9F3FF` | Ice |
| BROADCAST & REMOTE | `#FFB86B` | Ember |
| LOOK | follows the user's chosen master hue | |
| PANEL | `#C7D2D0` | Pearl |
| SYSTEM | `#8E9A99` | Slate (lowest emphasis) |

**System themes stay exactly as approved:** `Theme.SYSTEM` with Auto/Day/Night still gives the pure black panel (night) and pure white panel (day). Lumen glow sits *on top* of either. In Day mode, glow turns into a soft colored edge on white, never steel or gray chrome.

### 8.2 Glow engine

- **One glow model everywhere:** `GlowSpec(hue, intensity, halo, enabled)`. It resolves as global master × category × per-icon override, held in the new `LookKeys.GLOW_*` keys plus per-icon `icon.<folderId>.glow.*`.
- **Rendering:** Views draw glyphs via `Path`. The glow is a cached `Bitmap` per `(glyphId, hue, intensity, halo, sizePx)` made once with `BlurMaskFilter`, so draws are blits (E7). The existing `PanelGlow.kt` / `PanelGlassDrawable.kt` become consumers of `GlowSpec`.
- **Toggles:** global "Glow" on/off (exists: Glow effects › Glow), per-category on/off, per-icon on/off.
- **Never metal:** `LookKeys.TITANIUM_FRAME` and `LookKeys.SAPPHIRE_GLASS` are archived (kept in code under `terminal/archived_looks.kt`, hidden from the tree, default off). Nothing is deleted.

### 8.3 Icon system (mandate 1)

- **Strip all OS emoji:** the `📁` prefixes on ~64 code lines (`PanelTree.kt` 17, `EffectFolders.kt` 39, `FxKeys.kt` 4, plus 1 each in `PanelActions.kt`, `TerminalPanel.kt`, `FolderLayout.kt`, `microphone_effects_folder.kt`), every emoji in `strings.xml` (e.g. `record_gallery_failed` mentions "📁 Record"), and `EmojiRainLayer.DEFAULT_SET = "❤️🔥✨😂🎉💎"`.
- **Replace with `terminal/glyph_paths.kt`:** a 24-unit grid, 1.75-unit stroke, rounded caps and joins, closed outer contour. Ships with **12 packs × 8–12 glyphs** (Deck, Effects, Camera, Text, Voice, Record, Stage, Broadcast, Look, Panel, System, Particles).
- **Per-folder swap:** each folder keeps an `iconId`. Long-press a folder (lock off) → *Glyph Drawer* with the folder's pack plus "All glyphs". Pick one, set its hue/intensity/glow on-off, and it's saved to `icon.<folderId>.*`.
- **Particle Rain** (renamed from Emoji Rain) draws vector glyphs (spark, heart-glyph, star-glyph, diamond-glyph, crown, flame) from `glyph_paths.kt`. User text input for emoji stays possible but is off by default, labeled "Custom characters (uses phone font)".

### 8.4 Laser lettering baked into folder geometry (mandate 2)

- `FolderIconView` today draws the folder shape and the title sits elsewhere. **Proposal:** a new `FolderGlyphView` (keeping `FolderIconView` archived) builds one `Path`: tab + body. The title is converted to a path (`Paint.getTextPath`) and **clipped into the tab band**, then stroked and glowed in the same pass. Long titles auto-fit (existing `FitText.kt` logic) down to 9 sp, then abbreviate with a full-name tooltip.
- **Acceptance:** no title View sits outside the folder bounds (unit test on `FolderGlyphView.titleBounds ⊂ tabBounds`). Glow applies to title and outline together.

### 8.5 Controls (mandates 3 and 7)

- **`BioSliderView`** replaces typed 0–100 `NumberItem` rows:
  - A 4 dp track in hairline. The fill is a glowing bioluminescent stream that "flows" left-to-right (a subtle 2 s shimmer only while dragging).
  - The thumb is a 20 dp glowing bead and the value halo appears only while dragging (Halide, [S30]).
  - Detents every 5 with `CLOCK_TICK` haptics. Double-tap resets to the default. Long-press opens the numeric keypad for exact entry, so typing stays possible but is no longer the main way in.
  - TalkBack: `AccessibilityNodeInfo.RangeInfo`, ±1 / ±10 actions.
- **`GlowToggleView`** replaces `PowerToggleView` as the default style (the old style stays selectable under LOOK › Toggles):
  - A pill 52×28 dp; the knob travels **left → right** with 180 ms spring motion; the fill lights from left to right behind the knob.
  - On/off is shown by **position + fill + glyph**, never glow alone (A1). Haptics `TOGGLE_ON`/`TOGGLE_OFF` (API 34+), with a `CLOCK_TICK` fallback.
- **Laser scroll wheel (`DrumReelView` / `SizeReelBank`) stays only where a discrete wheel fits:** PANEL › Size & Position (W/H fine-tune) and STAGE › Canvas (aspect presets). **Not in the header.** The header keeps the PR #38 W/H tap-to-type boxes (`TopBar.dimensionInput`).
- **Text inputs** remain only for real text: status word, laser words, idle hex, PC address/port.

### 8.6 Top bar (final layout)

`[back orb] [title] … [W box] [H box] [CAMERA] [MIC] [REC 00:00] [SCREENSHOT] [LOCK]`. Each bracket is a bespoke Lumen glyph in the app, never an emoji.
- **Camera:** tap = on/off; long-press = **camera block** (new `FxKeys.CAMERA_BLOCKED`: unbinds and refuses rebind until cleared, red slash).
- **Mic:** tap = on/off; long-press = **hard block** (existing `FxKeys.MIC_BLOCKED` / `mic_hard_block.kt`).
- **REC:** tap start/stop; elapsed time shown here only.
- **Screenshot:** one tap, saves the *final composited stage* (not the UI) to `Pictures/OFFGRID`.
- **Lock:** Broadcast Lock on (§10.4).
- The top bar itself is hidden under Broadcast Lock.

---

## 9. Folder Information Architecture

> Read from `PanelTree.kt` (`build()`, `settingsFolder()` …) and `EffectFolders.kt` at `be674e1`. Setting keys (`LookKeys.*`, `FxKeys.*`, `MicKeys.*`) **do not change**, so saved preferences survive the move. Only folder ids, titles, and positions change.

### 9.1 Today: every folder and item (before)

| Folder (current title) | id | Items (in order) |
|---|---|---|
| **HOME** | `home` | Panel settings · Camera · AR Video Deck · Microphone Effects · Effects · Record · Live and Remote · OFFGRID to OFFGRID · 18 platform folders |
| Panel settings › 📁 Size | `size` | Width · Height · Full screen · See-through at full size · Center panel · Opacity · Font size · Typing speed · Corner roundness · Center guide |
| Panel settings › 📁 Display | `display` | **Test diamond** (paints into video) · Laser typing · Scan line · Ring · Hologram unfold · Touch feedback · **Titanium frame** · **Sapphire glass** · Border thickness |
| Panel settings › 📁 Glow effects | `glow_effects` | Glow · Glow strength · Pulse · Pulse speed · Edge glow · Outer glow · Touch glow · Typing glow |
| Panel settings › 📁 Colors | `colors` | 9 color folders: Laser cursor · Dark theme number · Light theme number · Typewriter text · Edge · Outer glow · Skin · Folder icon · Status line (each: hex, 9 numbers, swatch, system color, reset; cursor adds Cursor glow) |
| Panel settings › 📁 Auric wheel | `auric_wheel` | Laser color · Laser dimmer |
| Panel settings › 📁 Clicker Sounds | `clicker_sounds` | Clicker sounds · Sound style · Tone variation · Volume |
| Panel settings › 📁 Themes | `themes` | Theme: System · 📁 Theme mode · 📁 Toggle style (style, toggle glow) · 📁 Profiles |
| Panel settings › 📁 Effects Library | `effects_library` | External link folders by category |
| Panel settings › 📁 Reset | `reset` | Reset all to default |
| 📁 Camera | `camera` | Camera · Camera mode · 📁 Body mask (mode, plate, edge) · 📁 Face diamond (on, style, follow face, size, across, down, flip) · 📁 Cloak void (strength, fade, shimmer) · 📁 **Status label** (word, size, fade) · 📁 Ghost (style) · 📁 Takeover (on, zoom, small box, box size, flip-flop) · 📁 Mirror plus scan (on, scan speed, blips) |
| 📁 AR Video Deck | `ar_video_deck` | Pick clip · Play · Loop · Opacity · Clip id (typed) |
| 📁 Microphone Effects | `microphone` | Mic · Microphone Effects · Pitch · Robot · Robot amount · Echo · Echo delay · Echo amount · Radio · Cave · Cave amount · Mic volume · Noise gate · Sounds in video · **Beat glow** · Hard block · Rex voice swap · Bypass · Wet/dry · Rex pitch · Formant shift · Low EQ · Mid EQ · High EQ (24 flat items) |
| 📁 Effects | `effects` | Emoji rain · Drawing · Glow border · Fireworks · Portal · Flying heroes · Roller coaster · Clouds · Beach · Time freeze · Optical illusions · Red sweep · Depth shift · **Laser writing** · Radar sweep · Echo trail · Quiet mode · Shatter · **Soundboard** (19 subfolders, one flat list) |
| 📁 Record | `record` | **Record length (30 s default)** · Save to gallery · Save last video · Replay buffer · Replay length · Save replay |
| 📁 Live and Remote | `live_remote` | **Auto blackout when idle · Idle mode · Idle color hex** · Floating overlay · Overlay settings · Clean screen share · Remote role · Pairing · Remote preview · Send to PC · PC protocol · PC LAN address · PC port · PC resolution · PC video rate · Start PC output (16 flat items) |
| OFFGRID to OFFGRID | `offgrid_to_offgrid` | 📁 Handshake (copy code, new code, trust, block, check) |
| 18 platforms | `bigo_live` … `microsoft_teams` | Bigo Live, Tagged, MeetMe, SuperLive, TikTok, Twitch, Kick, YouTube Live, Trovo, Telegram, Tango, Instagram, Messenger, WhatsApp, Zoom, Google Meet, Discord, Microsoft Teams. Each: Guide · Effects (sweep go, I got kicked, Open app, Safe start) · Tests (test code, seen, marks, note, Success code, Results) |

**Problems found (R1 + D1):**
1. The top level has 26 entries, with 18 of them reference folders. Hick's law cost on every visit.
2. `Size` mixes **panel geometry** (W/H, full screen, center, corners) with **typography** (font size, typing speed) and **surface** (opacity).
3. `Display` mixes a **broadcast-visible** item (Test diamond paints into the video) with panel chrome and metal looks.
4. `Live and Remote` mixes **stage idle-curtain** controls, **screen-share overlay**, **Phone 2 link**, and **PC output** (dormant until the PC arrives).
5. `Camera › Status label` is text, not camera. `Effects › Laser writing` is text. `Effects › Soundboard` is audio.
6. `Microphone Effects` is 24 flat items with no groups.
7. `Beat glow` (a visual) lives in the mic folder.
8. Diagnostics (platform tests, success codes, results) sit at the top level inside each platform folder.
9. `Record length` contradicts the infinite-recording mandate.
10. `Clip id` asks the user to type an id.

### 9.2 Grouping principle

By **what it does on stream**, in the order a streamer reaches for it:
**Show** (Deck, Effects, Camera, Text) → **Sound** (Voice) → **Capture** (Record) → **Frame** (Stage) → **Go live** (Broadcast & Remote) → **Look & Panel** (customization) → **System** (rare and advanced, quarantined like OBS "Advanced" [S18]).

### 9.3 Item-level move list (every misplaced item)

| # | Item (current location) | Move to | Why |
|---|---|---|---|
| 1 | Width, Height (Size) | PANEL › Size & Position | Panel geometry |
| 2 | Full screen, See-through at full size (Size) | PANEL › Size & Position | Geometry |
| 3 | Center panel, Center guide, Corner roundness (Size) | PANEL › Size & Position | Geometry |
| 4 | Opacity (Size) | PANEL › Surface | Surface look, not size |
| 5 | Font size, Typing speed (Size) | PANEL › Type | Typography, not size |
| 6 | Laser typing (Display) | PANEL › Type | Typography behavior |
| 7 | Scan line, Ring, Hologram unfold, Border thickness (Display) | PANEL › Surface | Panel chrome |
| 8 | Touch feedback (Display) | PANEL › Feedback | Haptics belong with sounds |
| 9 | **Test diamond** (Display) | SYSTEM › Diagnostics | Paints into the broadcast, so it must be quarantined. Default off; refuses to turn on under Broadcast Lock |
| 10 | **Titanium frame, Sapphire glass** (Display) | **ARCHIVE** (hidden, code kept) | Breaks the no-metal mandate |
| 11 | Glow, Glow strength, Pulse, Pulse speed, Edge/Outer/Touch/Typing glow (Glow effects) | LOOK › Glow | Look customization |
| 12 | Colors › 9 parts (Colors) | LOOK › Colors | Look |
| 13 | Auric wheel › Laser color, dimmer | PANEL › Wheel | Panel control styling |
| 14 | Clicker Sounds (4 items) | PANEL › Feedback | Feedback |
| 15 | Themes › Theme System, Theme mode | LOOK › Theme | Look |
| 16 | Themes › Toggle style | LOOK › Toggles | Look |
| 17 | Themes › Profiles | **SETS** (new top level; Profiles evolve into Sets) | Promoted. Most-used recall feature [S17][S19] |
| 18 | Effects Library (links) | SYSTEM › Effects Library | Reference |
| 19 | Reset all | SYSTEM › Reset (plus per-folder Reset at the bottom of each folder, already exists via `RESET_FOLDER`) | Destructive, so quarantined |
| 20 | Camera, Camera mode (Camera) | CAMERA › Camera | Stays |
| 21 | Face diamond (Camera) | CAMERA › Face | Group |
| 22 | Body mask, Cloak void, Ghost (Camera) | CAMERA › Body & Cloak (MediaPipe, preserved) | Group. Mandate 20 |
| 23 | Takeover, Mirror plus scan (Camera) | CAMERA › Framing | Group |
| 24 | **Status label** (Camera) | TEXT & TITLES › Status Word | It's text |
| 25 | Pick clip, Play, Loop, Opacity (AR Video Deck) | VIDEO DECK › Library / Playback / Mix | Split by job |
| 26 | **Clip id** (typed) | ARCHIVE (replaced by the Library list) | No typing ids |
| 27 | Mic, Mic volume, Noise gate, Hard block, Sounds in video (Microphone) | VOICE & SOUND › Mic | Input |
| 28 | Microphone Effects, Pitch, Robot(+amt), Echo(+delay, amt), Radio, Cave(+amt), Bypass, Wet/dry (Microphone) | VOICE & SOUND › Voice FX | FX |
| 29 | Rex voice swap, Rex pitch, Formant shift (Microphone) | VOICE & SOUND › Rex Voice | Own feature |
| 30 | Low/Mid/High EQ (Microphone) | VOICE & SOUND › EQ (plus the Voice Character pad, Innovation Lab #9) | Group |
| 31 | **Beat glow** (Microphone) | EFFECTS › Reactive | It's a visual effect driven by audio |
| 32 | **Soundboard** (Effects) | VOICE & SOUND › Soundboard | Audio |
| 33 | **Laser writing** (Effects) | TEXT & TITLES › Laser Words | Text |
| 34 | Emoji rain (Effects) | EFFECTS › Ambient › **Particle Rain** (renamed, vector glyphs) | Mandate 1 |
| 35 | Drawing (Effects) | EFFECTS › Ink (renamed) | Shorter, clearer |
| 36 | Glow border, Portal, Clouds, Beach, Optical illusions, Radar sweep, Echo trail (Effects) | EFFECTS › Ambient | Long-running looks |
| 37 | Fireworks, Time freeze, Red sweep, Depth shift, Quiet mode, Shatter (Effects) | EFFECTS › Moments | One-shot "fire now" effects |
| 38 | Flying heroes, Roller coaster (Effects) | EFFECTS › Worlds | Character/scene overlays |
| 39 | **Record length** (Record) | ARCHIVE (default 0 = unlimited; key kept) | Mandate 6 |
| 40 | Save to gallery, Save last video (Record) | RECORD & CAPTURE › Recording | Group |
| 41 | Replay buffer, Replay length, Save replay (Record) | RECORD & CAPTURE › Replay | Group |
| 42 | — (new) | RECORD & CAPTURE › Screenshot (format, location) | Mandate 6 |
| 43 | **Auto blackout when idle, Idle mode, Idle color hex** (Live and Remote) | STAGE › Idle Curtain | It's the stage picture, not remote |
| 44 | Clean screen share, Floating overlay, Overlay settings (Live and Remote) | BROADCAST & REMOTE › Screen Share | Group |
| 45 | — (new) Broadcast Lock, Auto-lock on screen share | BROADCAST & REMOTE › Broadcast Lock | Mandate 9 |
| 46 | Remote role, Pairing, Remote preview (Live and Remote) | BROADCAST & REMOTE › Phone 2 | Group |
| 47 | Handshake (OFFGRID to OFFGRID) | BROADCAST & REMOTE › Phone 2 › Trust Codes | Same trust system |
| 48 | Send to PC, PC protocol/address/port/res/rate, Start PC output | BROADCAST & REMOTE › PC Output (Later) | Dormant until the PC arrives; collapsed by default |
| 49 | 18 platform folders (top level) | BROADCAST & REMOTE › Platform Guides › (Bigo Live pinned first, then A–Z) | Reference, one level down |
| 50 | Platform › Tests, Success code, Results | SYSTEM › Diagnostics › Platform Tests | Quarantine dev tools |

### 9.4 Proposed tree (after)

```
HOME  (Quick row: Lock · REC · Deck Play/Pause · Next Clip · Glow On/Off · [user-pinned ×3])
├── 01 VIDEO DECK                 (sea-firefly cyan)
│   ├── Library        clip grid (local + received from Phone 2), import via file picker, rename, delete→archive
│   ├── Playback       play/pause · loop · reverse · speed · skip next/prev
│   ├── Transitions    crossfade time · cut · fade-to-black
│   └── Mix            opacity · layer (over/under camera) · fit/fill · position
├── 02 EFFECTS                    (jellyfish violet)
│   ├── Ambient        Glow border · Portal · Clouds · Beach · Optical illusions · Radar sweep · Echo trail · Particle Rain
│   ├── Moments        Shatter · Time freeze · Red sweep · Depth shift · Fireworks · Quiet mode  (each with FIRE)
│   ├── Worlds         Flying heroes · Roller coaster
│   ├── Ink            draw on video · line · glow · color · pin to face · clear
│   └── Reactive       Beat glow · (new) Beat-synced glow, Innovation Lab #3
├── 03 CAMERA                     (abyss blue)
│   ├── Camera         on/off · mode · camera block
│   ├── Face           Face diamond (style, follow, size, across, down, flip)
│   ├── Body & Cloak   Body mask · Cloak void · Ghost       (MediaPipe, preserved)
│   └── Framing        Takeover · Mirror plus scan
├── 04 TEXT & TITLES              (candle amber)
│   ├── Status Word    word · size · fade
│   └── Laser Words    words · speed · size · glow · stay · color · WRITE NOW
├── 05 VOICE & SOUND              (foxfire green)
│   ├── Mic            mic · volume · noise gate · hard block · sounds in video
│   ├── Voice FX       master · pitch · robot · echo · radio · cave · bypass · wet/dry
│   ├── Rex Voice      swap · pitch · formant
│   ├── EQ             low · mid · high · (new) Voice Character pad
│   └── Soundboard     sounds · try · volume
├── 06 RECORD & CAPTURE           (coral)
│   ├── Recording      save to gallery · save last · segment length (30 min)
│   ├── Replay         buffer · length · save replay
│   └── Screenshot     format · save location
├── 07 STAGE                      (ice)
│   ├── Canvas         output aspect preset (laser reel ok here) · fit/fill · safe-zone guides (never on air)
│   └── Idle Curtain   auto blackout · mode · custom hex
├── 08 BROADCAST & REMOTE         (ember)
│   ├── Broadcast Lock lock now · auto-lock on screen share · unlock gesture help
│   ├── Screen Share   clean screen share · floating overlay · overlay settings
│   ├── Phone 2        role · pairing · remote preview · trust codes · revoke
│   ├── Platform Guides  Bigo Live (pinned) · Discord … YouTube Live (A–Z)
│   └── PC Output (Later)  send to PC · protocol · address · port · res · rate · start
├── 09 SETS                       (user hue)   saved scenes: camera+effects+deck+glow+voice
├── 10 LOOK                       (user hue)
│   ├── Theme          System (Auto/Day/Night)
│   ├── Glow           master on/off · hue · intensity · halo · pulse · edge/outer/touch/typing
│   ├── Colors         9 parts
│   ├── Icons          per-folder glyph · packs · per-icon hue/intensity/glow on-off
│   └── Toggles        style · toggle glow
├── 11 PANEL                      (pearl)
│   ├── Size & Position  W · H (laser reel ok) · full screen · see-through · center · center guide · corners
│   ├── Type           font size · typing speed · laser typing
│   ├── Surface        opacity · border · scan line · ring · hologram unfold
│   ├── Wheel          laser color · dimmer
│   └── Feedback       touch feedback · clicker sounds (style, tone, volume)
└── 12 SYSTEM                     (slate, lowest emphasis)
    ├── Diagnostics    test diamond · platform tests · success codes · results · render probe
    ├── Effects Library
    ├── Reset          reset all
    └── About          version · build SHA
```

*(Twelve entries: the 11 categories plus SETS, which was promoted from Themes › Profiles.)* Depth is **at most 3** (Category › Group › Item; effect-detail folders like "Portal" are the item's own sheet). The biggest group is Ambient with 8, inside the 7±2 comfort zone.

### 9.5 What lives where: Phone 1 (S25) vs Phone 2

| Area | S25 (Phone 1) | Phone 2 (master) | Why |
|---|---|---|---|
| Broadcast Lock | Lock (glyph, 2-finger swipe); unlock = 1.5 s corner long-press | Lock/unlock (hold-to-confirm 1 s) | Either can protect; unlock is deliberate (D5) |
| Video Deck | Full when unlocked; **hidden under lock** | **Full** + import/send + thumbnails | The S25 is on air |
| Effects / Moments fire | When unlocked | **Full** (big fire pads) | Vesper: run it blind from Phone 2 |
| Camera, Voice, Text | When unlocked | Full | Same |
| Record / Screenshot | Top bar when unlocked | Start/stop + elapsed + free space | Kade |
| Look / Glow toggles | Full | Quick glow on/off, master hue/intensity | Mandate 10 |
| Sets | Recall | **Save + recall** | Nova |
| Pairing / revoke / trust codes | **S25 only** | — | Security (X1) |
| Permissions, PC Output, Diagnostics, Reset all | **S25 only** | — | Destructive or dev, local only |
| Platform Guides | S25 | Read-only mirror | Reference |
| Health strip (fps, thermal, battery, link) | — | **Phone 2 only** | Never on air |

### 9.6 Implementation: nested-folder data model

`PanelFolder` already nests (`folders` + `items`, `parent`, `trail()`). Extend it, don't replace it:

```kotlin
// terminal/PanelTree.kt (proposal)
internal enum class Category(val hueToken: String, val order: Int) {
    VIDEO_DECK("hue.deck", 1), EFFECTS("hue.fx", 2), CAMERA("hue.cam", 3), TEXT("hue.text", 4),
    VOICE("hue.voice", 5), RECORD("hue.rec", 6), STAGE("hue.stage", 7), BROADCAST("hue.cast", 8),
    SETS("hue.user", 9), LOOK("hue.user", 10), PANEL("hue.panel", 11), SYSTEM("hue.system", 12)
}
internal enum class Scope { BOTH, PHONE1_ONLY, PHONE2_ONLY }

internal class PanelFolder(
    val id: String,                 // new stable ids: "deck", "deck.library", "fx.ambient", ...
    val title: String,              // plain words, no emoji; drawn as laser lettering
    val category: Category,
    val iconId: String,             // default glyph; user override in icon.<id>.glyph
    val scope: Scope = Scope.BOTH,
    val lockedWhenBroadcasting: Boolean = true,
    val folders: List<PanelFolder> = emptyList(),
    val items: List<PanelItem> = emptyList(),
    val notes: List<String> = emptyList(),
    val guide: Guide? = null,
)
// `tag` and FolderKind are kept and derived from category for old callers, then archived after Phase 2.
```

- **Migration:** a new `terminal/folder_id_migration.kt` maps old id → new id (e.g. `size` → `panel.size`, `display` → `panel.surface`, `live_remote` → `cast`). It's applied to the saved "last open folder", per-folder resets (`FolderReset.kt`), and test marks. Setting keys are untouched.
- **Builders:** `PanelTree.build()` returns the new tree. The old builders move to `terminal/archived_tree_v1.kt` (archived, not deleted) so a hidden System switch can show the old layout for one release.
- **Phone 2:** `RemotePreviewActivity` gets a read-only *tree manifest* (ids, titles, iconIds, item types, ranges) sent once after pairing (chunked if > 1 KiB, §10.5). Phone 2 renders the same tree from data, so the two can never drift.

**Acceptance criteria (IA):**
- [ ] Unit test `TreeInvariantsTest`: every setting key in the v1 tree appears **exactly once** in the v2 tree or in the archive list; depth ≤ 3; no group > 9 items; no title contains an emoji code point (regex `\p{So}|\p{Cs}`); every folder has a `category` and an `iconId`.
- [ ] `FolderIdMigrationTest`: all 40+ old ids map to a new one; unknown ids fall back to HOME.
- [ ] Test diamond can't turn on while `BroadcastLockStore.locked == true`.
- [ ] Old prefs file loads without loss (Robolectric fixture from `be674e1` defaults).

---

## 10. Engineering specs (handoff-ready)

### 10.1 Bug: stretched stage/lion on first launch

**Symptom (Royalty):** on first launch the stage/lion renders very wide and stretched. After switching to another app and back, it's normal.

**What the code does at `be674e1`:**

| Step | Code | Observation |
|---|---|---|
| A | `CompositorEffect(… PREVIEW or VIDEO_CAPTURE …)` | One effect serves **two** targets. CameraX may hand the processor one shared output or **two outputs with different sizes and crops** (`onOutputSurface` is called per output; sizes are logged `"Output WxH targets=…"`). |
| B | `CompositorProcessor.paintStage()` → `val main = slots.maxBy { it.width * it.height }` then `main.surfaceOutput.updateTransformMatrix(updatedTransform, textureTransform)` | The **stage size and camera transform come from the largest output**, whatever its target is. The result is then `present()`-ed (fit, no stretch) into every slot. If the VIDEO_CAPTURE output (16:9, from `Quality.HD`) is the largest, or arrives first, the screen gets the video's geometry and crop. With equal areas, `maxBy` returns the **first** in list order, i.e. arrival order. |
| C | `MainActivity.bindCamera()` → `ViewPort.Builder(Rational(previewView.width, previewView.height), rotation)` and `faces.outputAspect = previewView.width / previewView.height` | The view ratio is captured **once, at bind time.** `startCamera()` only waits for `width != 0`. |
| D | First launch path | `onResume` → camera permission dialog → `permissionLauncher` callback → `startCamera()`. The window is still settling (permission dialog focus, `hideSystemBars()` immersive transition, `shortEdges` cutout). |
| E | Return from another app | `onPause` → `applyCapture()` → `CaptureAction.UNBIND_CAMERA`; `onResume` → `BIND_CAMERA` → **fresh bind with final view size and fresh outputs**. This matches "normal after switching apps". |
| F | `StagePainter.drawAr()` → `drawCamera(texture, matrix, 0f, plan.arOpacity)` on the full target | The AR clip is drawn **edge to edge with no aspect correction.** Any clip whose aspect ≠ the stage aspect is stretched, every time. If "the lion" is a deck clip, this is a second, independent stretch. |

**Diagnosis (ranked):**
1. **Output selection (B):** the screen geometry must come from the **PREVIEW** output, never "largest output".
2. **Bind timing (C/D/E):** first bind uses a not-yet-final view ratio. The rebind after resume heals it.
3. **Clip aspect (F):** deck clips need fit/fill math from the real `VideoSize`.

**Fix (proposal, exact):**

1. `CompositorProcessor.kt`:
   ```kotlin
   // replace: val main = slots.maxBy { it.width * it.height }
   val main = slots.firstOrNull { it.surfaceOutput.targets and CameraEffect.PREVIEW != 0 }
       ?: slots.maxBy { it.width * it.height }
   ```
   Apply the same in `presentCamera()` for `drawRecorders(main…)`. Recorders keep using the stage picture but `present()` into their own size with **fill** (crop), not fit, so the recording has no bars.
   Extract the math to a pure `effects/stage_geometry.kt`:
   `fun stageSize(previewW, previewH, maxSide): Size`, `fun fitRect(src, dst)`, `fun fillCrop(src, dst)`. These are unit-testable.
2. `camera_bind.kt` / `MainActivity.kt`:
   - `startCamera()` waits for `previewView.isLaidOut && hasWindowFocus()` (post to `onWindowFocusChanged(true)` if not).
   - Store `boundW/boundH` in `bindCamera()`.
   - Add `previewView.addOnLayoutChangeListener { … }`: if the new aspect differs from the bound aspect by > 1%, call `rebindCamera()` (debounced 150 ms).
   - Update `faces.outputAspect` on every rebind.
3. `ar_deck_player.kt` + `StagePainter.kt`:
   - Add `Player.Listener.onVideoSizeChanged(videoSize)`. Post `(width × pixelWidthHeightRatio, height)` to the GL thread → `ArDeckSurface.videoW/H`.
   - `drawAr()` computes `fitRect`/`fillCrop` (new `FxKeys.AR_FIT`, default **fill**). It applies fit with `glViewport(sub-rect)` and fill by multiplying a centered crop scale into the texture matrix (`Matrix.scaleM` around 0.5). Until the first `onVideoSizeChanged` arrives, the clip isn't drawn (`hasSize == false`), so the first frame is never stretched.
4. **Render probe (diagnostic, never on air):** SYSTEM › Diagnostics › Render probe logs `previewView WxH, ViewPort ratio, each output WxH+targets, stage WxH, clip VideoSize` for the first 3 frames after each bind.

**Acceptance criteria:**
- [ ] `StageGeometryTest`: for outputs {preview 1080×2340, video 720×1280} in **both arrival orders**, the stage aspect equals the preview aspect (±0.5%).
- [ ] `StageGeometryTest`: a 1920×1080 clip on a 1080×2340 stage. With fill, the visible U range = stageAspect / clipAspect = 0.4615 / 1.7778 ≈ 0.26, centered (V range 1.0). With fit, the clip rect is full width and 1080×(1080/1920) ≈ 608 px tall, centered. The scale is always uniform.
- [ ] `BindTimingTest` (Robolectric): a layout change from 1440×2900 to 1440×3120 triggers exactly one rebind.
- [ ] Royalty's end-of-build device check: 10 cold starts (including first install with the permission dialog) show an undistorted stage and clip.

### 10.2 Pristine broadcast surface (mandate 5)

Bigo captures the **whole screen** [S22], so any View over `PreviewView` is broadcast.

| Remove from the surface | Where it is today | Change |
|---|---|---|
| `[OFFGRID]` floating badge | `TerminalPanel` minimized pill: `FolderIconView` + `LaserTitleView(text = R.string.terminal_pill)` | Delete the pill *view usage* (keep the code in the archive). The panel opens via a 1.0 s top-left corner long-press, the top-bar glyph, or Phone 2 (D8). Coach mark once, never while recording or locked |
| "Camera is off…" and every status line | `activity_main.xml › control_bar › status_text`, driven by `showMessage()` / `status_line.kt` (`R.string.camera_off`, `camera_starting`, `camera_ready`, `recording_now` …) | `control_bar` becomes `GONE` permanently on the stage. Messages route to `PanelStatus` (the panel's `LaserTypeView` line) and to Phone 2 as toasts. Error states that need action (permission denied) show in the panel only |
| "Record 30 s" / "Record 5 seconds" HUD | `record_button` (`R.string.record_seconds` / `record_five`), `play_button`, `action_button` in `control_bar` | Gone from the surface. Record moves to the top bar (§8.6); "Play last" moves to RECORD & CAPTURE › Recording |
| Playback bar | `playback_bar` | Playback becomes a panel sheet (off-stage) |

**Acceptance criteria:**
- [ ] `SurfaceCleanTest` (Robolectric): after `onCreate` + first frame, no visible View except `PreviewView`, `DarkStage`, and (if unlocked) `TerminalPanel`/`TopBar`; `control_bar.visibility == GONE` in every `ButtonMode`.
- [ ] No string resource shown on the stage contains "OFFGRID", "Record", or "Camera is off" (scan of visible TextViews).
- [ ] Locked: **only** `PreviewView`/`DarkStage` are visible.

### 10.3 Unlimited recording + screenshot (mandate 6)

- **Files:** `recording_controls.kt`, `audio/AvRecorder.kt`, `audio/Mp4Writer.kt`, `TopBar.kt`, `FxKeys.kt`, `GallerySaver.kt`, new `screenshot_capture.kt`.
- `FxKeys.RECORD_SECONDS` default **0** (unlimited). The `NumberItem` is archived from the tree; the existing `recordSeconds() > 0` stop timer stays dormant.
- **Segments:** `AvRecorder` rolls to a new MP4 every 30 min at the next keyframe (`SEGMENT_MINUTES`, setting in RECORD & CAPTURE › Recording) with continuous audio. Files are named `OFFGRID_yyyyMMdd_HHmmss_partNN.mp4`.
- **Free-space guard:** check `StatFs` every 10 s; at < 2 GB, finish the segment, stop, and alert the panel plus Phone 2.
- **Top bar:** `QuickToggleView` for REC (red `accent.live`, elapsed `mm:ss`/`h:mm:ss`), Screenshot glyph, Lock glyph, next to the existing Camera and Mic toggles.
- **Screenshot:** on the GL thread after `paintStage`, read the final stage target with `glReadPixels` into a PBO (async, two frames later). Encode PNG off-thread and save via `MediaStore` to `Pictures/OFFGRID` (no permission on API 29+). UI is never in it.
- **Camera block:** new `FxKeys.CAMERA_BLOCKED` mirrors `MIC_BLOCKED`. `CaptureSwitch.update()` treats blocked as "camera not wanted" and refuses to bind; long-press the Camera toggle.

**Acceptance criteria:**
- [ ] `RecordingSegmentsTest`: simulated 95 min → 4 files (30/30/30/5), no gap in presentation timestamps across a boundary > 1 frame.
- [ ] Unlimited by default: a fresh install has `RECORD_SECONDS == 0` and no auto-stop runnable posted.
- [ ] Screenshot test: the captured bitmap size equals the stage size and contains no panel pixels (render with the panel visible; assert the stage-only buffer).
- [ ] `CameraBlockTest`: while blocked, `BIND_CAMERA` is never emitted.

### 10.4 Broadcast Lock and dual-device stealth (mandate 9)

**Mechanism (verified in docs):** Android 15 `WindowManager.addScreenRecordingCallback(executor, consumer)` returns the initial state and calls back on change. VISIBLE means **our activities are in a recording**. It needs `<uses-permission android:name="android.permission.DETECT_SCREEN_RECORDING"/>`, protection level **normal**, API 35 [S1][S2][S4]. The official sample registers in `onStart` and removes in `onStop` [S1].

**State machine (`broadcast_lock_store.kt`, new):**
```
sources: AUTO_RECORDING (callback), MANUAL (S25 glyph / 2-finger swipe), REMOTE (Phone 2)
locked = autoLockEnabled && recordingVisible  ||  manualLocked  ||  remoteLocked
unlock: S25 corner long-press 1.5 s (clears MANUAL+REMOTE; AUTO stays while recording is visible
        unless "Allow unlock during share" is on) · Phone 2 hold-to-confirm
persist: manualLocked, remoteLocked, autoLockEnabled (default ON)
```

**Cold start rule ("opens straight into the clean looping video"):**
1. `onCreate`: `terminalPanel` and `TopBar` start **INVISIBLE**. The stage is drawn first.
2. `onStart`: `val state = windowManager.addScreenRecordingCallback(mainExecutor, cb)` (API ≥ 35, guarded by `Build.VERSION.SDK_INT`). If VISIBLE or persisted lock → **locked**: panel stays hidden, deck auto-plays the last clip in loop (if Deck is on), effects keep running.
3. If NOT_VISIBLE and no manual lock → reveal the panel with a 120 ms fade *after the first stage frame*.
4. The callback flips to VISIBLE at any time → instant lock (same frame, no animation).

**Also hidden under lock:** the floating overlay window (`live/OverlayWindowController.kt`, `LiveOverlayController.kt`). It draws over Bigo too, so it's on air. Overlay windows aren't activities, so whether the callback counts them is **UNVERIFIED**. The lock hides them regardless. The overlay is kept for non-broadcast use (mandate 20).

**Absorb the existing clean mode:** `LiveIntegration` already has `LiveActions.CLEAN_OUTPUT` (hides `controlBar`, sets `panel.setHiddenForPlayback(true)`, hides the overlay). Broadcast Lock becomes the single source of truth, and `CLEAN_OUTPUT` simply calls `BroadcastLockStore.lock(MANUAL)`.

**Do not use `FLAG_SECURE`:** it would black out OFFGRID in Bigo's capture, which is the opposite of what Royalty wants.

**Files:** `MainActivity.kt` (onStart/onStop registration), new `broadcast_lock_store.kt`, `TerminalPanel.kt` (visibility gate), `TopBar.kt` (Lock glyph), `LiveIntegration.kt` (overlay hide), `live/RemoteMessage.kt` (allowlist `lock_on`, `lock_off`), `AndroidManifest.xml` (permission), `.github/workflows/build-debug-apk.yml` (**allowlist `DETECT_SCREEN_RECORDING`, which needs Royalty's approval**).

**Acceptance criteria:**
- [ ] `BroadcastLockStoreTest`: all 8 combinations of (auto, manual, remote) → expected `locked`; unlock rules hold.
- [ ] `ColdStartLockTest` (Robolectric, SDK 35 shadow): initial VISIBLE → the panel is never VISIBLE for any frame; a deck clip is playing if Deck was on.
- [ ] Manifest-merger CI step passes with the new allowlist entry; the step fails if the permission is missing (guard both ways).
- [ ] Phone 2 `lock_on` hides the S25 panel within 1 frame of receipt.
- [ ] Royalty's end-of-build test on the S25 with Bigo Game Live: open OFFGRID during the share → no folders visible on the broadcast, ever.

### 10.5 Phone 2 master protocol (mandate 10)

**Today:** `RemoteMessage(key,type,value)`: the type is `fire` (allowlisted actions incl. `ar_play`, `ar_pause`, `ar_next`) or `setting` with an `fx.*` key; fields ≤ 256 chars, frames ≤ 1024 B. `RemoteSession` carries `Video` packets (≤ 16 MiB) inside `EncryptedChannel` (strict in-order counters, rekeying).

**Additions (proposal):**

| Packet / message | Direction | Content | Limits |
|---|---|---|---|
| `fire` actions + `lock_on`, `lock_off`, `rec_start`, `rec_stop`, `shot`, `ar_prev`, `ar_reverse`, `ar_crossfade`, `set_recall` | P2 → P1 | existing format | allowlist |
| `setting` keys `fx.deck.*`, `fx.glow.*`, `look.glow.*` (via a new `look.` prefix rule) | P2 → P1 | existing format | 256 chars |
| `RemotePacket.TreeManifest` | P1 → P2 | tree ids/titles/iconIds/item types/ranges (JSON, gzip) | ≤ 256 KiB, sent once per pairing + on change |
| `RemotePacket.State` | P1 → P2 | lock, rec time, free space, fps, thermal, battery, deck clip/pos | 1 Hz, ≤ 1 KiB |
| `RemotePacket.FileOffer` | P2 → P1 | fileId, name, size, mime, sha256 | mp4/webm/mkv, ≤ 512 MiB |
| `RemotePacket.FileChunk` | P2 → P1 | fileId, offset, bytes | 256 KiB |
| `RemotePacket.FileAck` | P1 → P2 | fileId, nextOffset / done / rejected(reason) | resumable |
| `RemotePacket.Thumb` | P1 → P2 | clipId, JPEG 160 px | ≤ 24 KiB |

- **Scheduler:** one writer queue with priority `control > state > thumb > chunk`. At most one chunk in flight before re-checking control. A chunk (256 KiB at ~20 MB/s on local Wi-Fi) is ≈ 13 ms, so control latency stays ≤ 1 chunk-time (D6).
- **Storage:** `filesDir/deck/incoming/<fileId>.part` → hash verify → rename to `filesDir/deck/<clipId>.mp4` → `ArDeckCatalog` refresh → thumbnail back to P2. No storage permission (X1).
- **Files:** `live/RemoteSession.kt`, `live/RemoteMessage.kt`, new `live/file_transfer.kt`, new `live/tree_manifest.kt`, `live/RemotePreviewActivity.kt` (becomes a full controller; split into `remote_controller_activity.kt` + fragments), `ar_deck_catalog.kt`.

**Acceptance criteria:**
- [ ] `FileTransferTest`: a 200 MiB file over a loopback pair with a forced disconnect at 37% resumes at the right offset; the sha256 matches; a wrong hash → rejected and the `.part` is archived to `deck/rejected/`.
- [ ] `PriorityTest`: during a transfer, a `fire` message is delivered in < 1 chunk-time (mocked clock).
- [ ] `RemoteSecurityTest` (exists) extended: unknown packet types are rejected; a FileChunk without a prior accepted Offer is rejected; non-video MIME is rejected.
- [ ] No new permissions (CI allowlist unchanged for this phase).

### 10.6 AR Video Deck: seamless loop, reverse, skip, crossfade (mandate 11)

- **Loop:** keep `REPEAT_MODE_ONE` (correct per Media3 guidance [S43a][S43c]). Add `experimentalSetEnableMediaCodecVideoRendererPrewarming(true)` for item transitions [S43b] (**UNVERIFIED** on Media3 1.11.x; guard with try/catch).
- **Perfect-loop helper:** at import, detect the best loop seam (Innovation Lab #6) and store `loopStartMs/loopEndMs` → `MediaItem.ClippingConfiguration`.
- **Skip:** the deck keeps a playlist (`ArDeckCatalog` order). `ar_next/ar_prev` → `seekToNextMediaItem()` / `seekToPreviousMediaItem()`.
- **Crossfade:** **two players** (A/B) with two OES textures (`ArDeckSurface` ×2). The incoming player prepares and holds the first frame, then `StagePainter.drawAr` mixes `alphaA = 1−t, alphaB = t` over `crossfadeMs` (default 600 ms). After the fade, release the decoder on the idle player (E7).
- **Reverse:** ExoPlayer can't play backwards. At import (and on demand), build `deck/<clipId>_rev.mp4`: decode with `MediaCodec` to the GL thread, re-encode in reverse frame order in GOP-sized batches (bounded memory), with a progress glyph on Phone 2. The Reverse toggle swaps to the reversed file at the mirrored position (`dur − pos`). Limit: clips ≤ 60 s for v1.
- **Files:** `ar_deck_player.kt` (→ `ArDeckPlayerPair`), `ar_deck_surface.kt`, `ar_deck_catalog.kt`, new `ar_deck_reverser.kt`, `CompositorProcessor.kt`, `StagePainter.kt`, `StageShaders.kt` (no shader change needed for alpha; existing `uAlpha`).

**Acceptance criteria:**
- [ ] `DeckLoopTest`: `REPEAT_MODE_ONE` set; no manual `seekTo(0)` calls.
- [ ] `CrossfadeTest`: the mix alpha curve sums to 1 ± 0.01 at every step; the idle decoder is released ≤ 100 ms after the fade ends.
- [ ] `ReverserTest` (instrumented-optional / unit on frame order): output frame timestamps are strictly increasing and the frame order is reversed for a 30-frame synthetic clip.
- [ ] Deck aspect uses `VideoSize` (see §10.1).

---

## 11. Innovation Lab (beyond the mandates)

> Rules: no root, Android 15, phone-only, Bigo screen-share safe (nothing visible on the S25 that isn't meant for viewers), no app virtualization or camera hooking, no new on-device AI beyond MediaPipe.

| # | Idea | Why it wows streamers | Feasibility / effort | Phase |
|---|---|---|---|---|
| 1 | **Sets: instant scene recall.** Save camera mode + effects + deck clip + glow + voice + text as one named Set; recall in one tap (S25 Quick row or Phone 2 pad), with a 300 ms crossfade between Sets | One tap changes the whole show. StreamYard's best-loved idea, done on a phone [S17] | Medium. Extends `PresetsItem`/Profiles (`PresetRows.kt`, `PanelPrefs.kt`) to include `fx.*` + deck state; 1–2 weeks | 4 |
| 2 | **Phone 2 "What Bigo sees" monitor.** P2 shows the exact composited stage (existing `RemotePreviewEncoder`/`Decoder` H.264 path) with a red LOCK/ON AIR frame | Confidence: you see the broadcast without touching the S25 | Low–medium; the pipeline exists. Add the lock-state overlay on P2 only; 3–5 days | 3 |
| 3 | **Beat-synced glow.** Panel and stage glow (Glow border, Portal rim) pulse on the `BeatDetector` beat with ±12% intensity | The room feels the music; premium "living" light | Low. `BeatDetector` + `BeatPulse` exist; route to `GlowSpec` and `GlowBorderLayer`; 3 days | 4 |
| 4 | **Ambient color match.** Glow hue follows the dominant color of the current deck clip (sampled at 4×4 px from the deck texture once per second, smoothed) | The UI and stage edges harmonize with every clip automatically. Looks art-directed | Low–medium; GL readback of a tiny mip; 4 days | 4 |
| 5 | **Haptic confirmations on Phone 2.** Distinct patterns: lock on (CONFIRM), effect fired (CLOCK_TICK ×2), transfer done (long soft), error (REJECT) | Vesper runs the show blind; your hand *knows* it worked | Low; `HapticGrammar.kt` shared module; 2 days | 3 |
| 6 | **Smart clip auto-trim for perfect loops.** At import, find the pair of frames (within the first/last 1.5 s) with the lowest pixel difference and set loop in/out there | No visible "jump" at the loop seam. The lion breathes forever | Medium; offline decode of edge GOPs at 64 px, SSIM-lite; 1 week | 3 |
| 7 | **Gesture cues from Phone 2.** Swipe up = next clip, two-finger tap = fire last Moment, pinch = crossfade, hold = lock | Muscle-memory control without looking | Low; P2 gesture layer mapping to existing commands; 3 days | 3 |
| 8 | **Voice cues on Phone 2 (cloud only, opt-in).** Phone 2 listens for a wake phrase and sends commands. Must use a cloud API per the "API only" rule; **never** the S25 mic, which is on air | Hands-free on stage | Medium; needs an API key in the Keystore and Royalty's approval; CI blocks `SpeechRecognizer` today, so it lives in a separate Phone 2 module, **approval required** | 5 (approval) |
| 9 | **Voice Character pad.** One 2D pad (warm↔bright × close↔room) maps to Low/Mid/High EQ + reverb, B&O-style [S37a] | Pro sound in one gesture | Low–medium; `VoiceSettings` mapping; 4 days | 4 |
| 10 | **Lumen Ring (radial quick menu).** Long-press the stage (unlocked) → 6 glyphs; flick to fire; per-Set ring profiles [S35] | Fastest possible control on the S25 itself | Medium; new `lumen_ring_view.kt`; 1 week | 4 |
| 11 | **Pre-show checklist ("Green Room").** Before going live, P2 runs checks: lock armed, mic not blocked, battery > 30%, free space > 10 GB, thermal OK, deck clip loaded, Bigo pinned | Never go live broken. Kade's hour-five insurance | Low; reads existing state; 3 days | 3 |
| 12 | **Thermal-aware quality governor.** As thermal status rises (`PowerManager.getThermalHeadroom`), it steps down: glow halo → second decoder → segmenter rate → stage max side, invisibly, in that order | Streams survive hour five without a crash | Medium; ADPF hint + `addThermalStatusListener`; 1 week | 5 |
| 13 | **Signature "curtain call".** One command fades the stage to the idle curtain with a laser-lettered sign-off (user text) and a soft glow sweep, then auto-stops recording | An expensive-looking ending every time | Low; Quiet mode + Laser words + curtain exist; 3 days | 4 |
| 14 | **Clip "drop zones" on Phone 2.** Drag a clip thumbnail onto Set slots or onto "Play next" (Things-style drag-to-place [S32]) | Planning the show feels like DJing | Medium; P2 UI; 1 week | 4 |
| 15 | **Glow Studio generator.** Pick 3 values (hue, intensity, halo); every token derives from them (Linear-style [S34a]); share as a 6-char code between two OFFGRID phones via existing trust codes | Endless personal themes that never clash | Low–medium; 4 days | 2 |

**Council top 5 picks (ranked):**
1. **#2 Phone 2 "What Bigo sees" monitor.** Protects reputation; the pipeline already exists. (24/25 votes)
2. **#1 Sets.** The single biggest "wow" per tap. (23/25)
3. **#6 Smart loop auto-trim.** It makes the Video Deck look flawless. (21/25)
4. **#11 Green Room checklist.** Prevents the mistakes that cost rooms. (20/25)
5. **#12 Thermal governor.** Hour-five survival. (19/25)

**Acceptance criteria pattern for each Lab item** (agents must fill this in on the PR before coding):
- [ ] A user-visible behavior statement in one sentence.
- [ ] A unit test for the pure logic (selection, mapping, math).
- [ ] No new permission, or an explicit approval link.
- [ ] No pixel added to the S25 stage while locked (assert in `SurfaceCleanTest`).
- [ ] A frame-time budget: ≤ 1 ms added on the GL thread (measured in the existing debug stats).

---

## 12. CONSENSUS and phased ROADMAP

### 12.1 Consensus (what all seats agreed)

1. **The S25 screen *is* the broadcast.** Broadcast Lock (auto via the Android 15 callback + manual + Phone 2) hides all chrome; cold start is stage-first.
2. **Fix the root causes of the render bug:** the PREVIEW output is master, the camera rebinds on layout change, and deck clips use their real video size.
3. **Lumen design system:** OLED black, bespoke closed-contour vector glyphs, laser lettering baked into folders, desaturated bioluminescent glow with per-icon control, sliders and left-to-right toggles. **No metal, no emoji.** System black/white themes kept.
4. **Function-first IA, 12 top-level folders, max depth 3,** with a Quick row. Every setting key is preserved and old folder ids are migrated.
5. **Phone 2 is the master controller,** with an encrypted, chunked, resumable transfer and a live "What Bigo sees" monitor; destructive and pairing actions stay S25-only.
6. **Ship protection first, beauty second, mastery third.** Every change goes through a PR against `final-build` with CI evidence. Royalty merges. `main` untouched.

### 12.2 Phase 0: approvals and guardrails (1–2 days)

| Item | Files | Acceptance criteria |
|---|---|---|
| Royalty approves the `DETECT_SCREEN_RECORDING` allowlist | `.github/workflows/build-debug-apk.yml` | The allowlist contains it with the comment "Broadcast Lock auto trigger (Royalty approved <date>)" |
| Royalty approves the slider-guard change (needed for Phase 2) | same | The guard bans `SeekBar|RangeSlider|material\.slider` and allows `BioSliderView`; a test commit with a `SeekBar` fails CI |
| Add CI: emoji guard (fail on `\p{So}` in `PanelTree.kt`, `EffectFolders.kt`, `strings.xml` after Phase 2) | workflow | A guard step exists, set to *warn* in Phase 1 and *fail* from Phase 2 |
| Archive folder convention | `04 Build/archive/` | Archived code moves there with a note file; nothing is deleted |

### 12.3 Phase 1: protect the stream (top priority, about 1–1.5 weeks)

| # | Item | Files / classes | Acceptance (summary; full in §10) |
|---|---|---|---|
| **1.1** | **Render bug fix** (PREVIEW master, rebind on layout, AR VideoSize fit/fill) | `CompositorProcessor.kt`, new `effects/stage_geometry.kt`, `camera_bind.kt`, `MainActivity.kt`, `ar_deck_player.kt`, `ar_deck_surface.kt`, `StagePainter.kt`, `FxKeys.kt` (`AR_FIT`) | `StageGeometryTest`, `BindTimingTest` green |
| **1.2** | **Pristine surface** (remove pill, `control_bar` off-stage, messages → panel/P2) | `activity_main.xml`, `TerminalPanel.kt`, `status_line.kt`, `MainActivity.kt` | `SurfaceCleanTest` green |
| **1.3** | **Broadcast Lock** (store, callback, cold-start gate, manual + P2 commands, overlay hide) | new `broadcast_lock_store.kt`, `MainActivity.kt`, `TerminalPanel.kt`, `TopBar.kt`, `LiveIntegration.kt`, `live/RemoteMessage.kt`, `AndroidManifest.xml`, workflow | `BroadcastLockStoreTest`, `ColdStartLockTest` green; manifest check green |
| **1.4** | **Unlimited recording + top-bar REC / Screenshot / Lock + camera block** | `recording_controls.kt`, `audio/AvRecorder.kt`, `audio/Mp4Writer.kt`, `TopBar.kt`, `FxKeys.kt`, `audio/CaptureSwitch.kt`, new `screenshot_capture.kt`, `GallerySaver.kt` | `RecordingSegmentsTest`, `CameraBlockTest`, screenshot test green |
| **1.5** | **Deck loop hardening** (prewarm guard, no stretch before first size, auto-play last clip under lock) | `ar_deck_player.kt`, `CompositorProcessor.kt` | `DeckLoopTest` green |

### 12.4 Phase 2: Lumen look and the new IA (about 2–3 weeks)

| # | Item | Files / classes | Acceptance |
|---|---|---|---|
| 2.1 | Tokens + glow engine | new `terminal/lumen_tokens.kt`, `terminal/glow_spec.kt`; `PanelGlow.kt`, `PanelGlassDrawable.kt`, `PanelLook.kt`, `LookPresets.kt` | Contrast test: every ink/glow combination ≥ 4.5:1; glow bitmaps are cached (draw count test) |
| 2.2 | Glyphs + packs; strip emoji; Particle Rain | new `terminal/glyph_paths.kt`, `terminal/glyph_drawer_view.kt`; `FolderIconView.kt` (archived) → new `terminal/folder_glyph_view.kt`; `EmojiRainLayer.kt`; `strings.xml`; `PanelTree.kt`; `EffectFolders.kt`; `FxKeys.kt` | Emoji guard set to *fail* and green; `titleBounds ⊂ tabBounds` test |
| 2.3 | Laser lettering baked into folders | `folder_glyph_view.kt`, `LaserText.kt`, `FitText.kt` | Title-path test; long-title abbreviation test |
| 2.4 | BioSlider + GlowToggle; NumberItem → slider; keep header W/H boxes; reels only in PANEL › Size and STAGE › Canvas | new `terminal/bio_slider_view.kt`, `terminal/glow_toggle_view.kt`; `PanelRows.kt`, `SettingRows.kt`, `WheelNumberRow.kt`, `PowerToggleView.kt` (selectable legacy), `TopBar.kt` (unchanged boxes) | Slider a11y RangeInfo test; header contains no `DrumReelView` (existing PR #38 test kept) |
| 2.5 | New folder tree + migration | `PanelTree.kt`, `EffectFolders.kt`, new `terminal/folder_id_migration.kt`, archived `terminal/archived_tree_v1.kt`, `FolderReset.kt`, `PanelNavigation.kt` | `TreeInvariantsTest`, `FolderIdMigrationTest` green |
| 2.6 | Archive the metal looks | `PanelLook.kt`, `PanelGlassDrawable.kt` → `terminal/archived_looks.kt` | No metal toggle reachable from the tree |
| 2.7 | Haptic grammar + motion tokens | new `terminal/haptic_grammar.kt`; `wheel_click.kt`, `PanelPlacement.kt` | Constants used, none of the legacy `Vibrator` calls |

### 12.5 Phase 3: Phone 2 master (about 2–3 weeks)

| # | Item | Files / classes | Acceptance |
|---|---|---|---|
| 3.1 | Tree manifest + P2 renders the same tree | new `live/tree_manifest.kt`; `RemotePreviewActivity.kt` → new `live/remote_controller_activity.kt` | The P2 tree equals the S25 tree (ids/order) in a test |
| 3.2 | Chunked encrypted file transfer | new `live/file_transfer.kt`; `RemoteSession.kt`; `ar_deck_catalog.kt` | `FileTransferTest`, `PriorityTest` green |
| 3.3 | Deck commands: play/pause/loop/reverse/skip/crossfade (A/B) | `ar_deck_player.kt` → `ArDeckPlayerPair`, new `ar_deck_reverser.kt`, `StagePainter.kt` | `CrossfadeTest`, `ReverserTest` green |
| 3.4 | "What Bigo sees" monitor + Health strip + haptics + gestures (Lab #2, #5, #7) | `RemotePreviewDecoder.kt`, controller activity, `haptic_grammar.kt` | P2 shows the lock frame within 1 s of a state change |
| 3.5 | Smart loop auto-trim + Green Room (Lab #6, #11) | new `ar_deck_loop_finder.kt`, new `live/green_room.kt` | Loop finder unit test on synthetic frames |

### 12.6 Phase 4–5: signature features and hardening

- **Phase 4:** Sets (#1), Beat-synced glow (#3), Ambient color match (#4), Voice Character pad (#9), Lumen Ring (#10), Curtain call (#13), Drop zones (#14).
- **Phase 5:** Thermal governor (#12), 6-hour soak, voice cues on P2 (#8, **only with approval**), optional emulator cold-start CI job, PC/OBS route when the PC arrives (existing `pc_*`, `srt_caller.kt`, `rtmp_*` kept dormant).

### 12.7 Rex's agent and model assignments

> Rex Standing Rule 1: research current agent and model options at kickoff and pick per task. Rule 2: OFFGRID builds in Claude Code always run in **Ultracode** mode. Assignments below are by difficulty; Rex re-confirms availability on the day of kickoff.

| Task | Difficulty | Agent / tool | Model tier | Why |
|---|---|---|---|---|
| 1.1 Render bug (GL + CameraX) | Hard | Claude Code, **Ultracode** | Top reasoning tier (Opus-class) | Multi-file GL/CameraX reasoning, threading |
| 1.3 Broadcast Lock | Hard | Claude Code, **Ultracode** | Top tier | Lifecycle + permissions + cold-start gate |
| 1.2 Surface cleanup | Medium | GitHub Copilot coding agent (own PR branch) | Default high-quality coding model | Well-scoped layout/visibility edits |
| 1.4 Recording + top bar | Medium–hard | Claude Code, **Ultracode** | Top tier | MediaCodec segmenting, timestamps |
| 1.5 Deck hardening | Medium | Copilot coding agent | Default | Small, scoped |
| 2.1–2.3 Lumen tokens, glyphs, lettering | Hard (craft) | Claude Code, **Ultracode** | Top tier | Visual quality bar, custom Views |
| 2.2 Emoji strip (mechanical part) | Easy | Copilot coding agent | Default / fast | Mechanical text edits + CI guard |
| 2.4 BioSlider / GlowToggle | Medium–hard | Claude Code, **Ultracode** | Top tier | Custom View, a11y, haptics |
| 2.5 Tree + migration | Medium | Copilot coding agent | Default | Data restructuring with strict tests |
| 3.2 File transfer | Hard (security) | Claude Code, **Ultracode** | Top tier | Crypto session, resumability |
| 3.3 A/B crossfade + reverser | Hard | Claude Code, **Ultracode** | Top tier | Media pipeline |
| Research refresh, docs, council MDs | Medium | Grok Bot research subagent | — | Web research + synthesis |
| PR review gate | Medium | Rex + QA review pass (second agent, not the author) | Top tier | Independent review |

---

## 13. Master test plan

| Layer | What | Where it runs | Gate |
|---|---|---|---|
| Unit (JVM) | `StageGeometryTest`, `BroadcastLockStoreTest`, `TreeInvariantsTest`, `FolderIdMigrationTest`, `RecordingSegmentsTest`, `FileTransferTest`, `PriorityTest`, `CrossfadeTest`, `ReverserTest`, contrast tests, existing ~45 test files | CI `testDebugUnitTest` | Must pass |
| Robolectric | `SurfaceCleanTest`, `ColdStartLockTest`, `BindTimingTest`, `CameraBlockTest`, slider a11y | CI | Must pass |
| Static guards | Permission allowlist (+`DETECT_SCREEN_RECORDING`), slider guard (revised), emoji guard, existing raw-mic / cloud-speech guards | CI | Must pass |
| Lint | `lintDebug` | CI | Must pass |
| Emulator (optional, Phase 5) | Cold-start screenshot at API 35, assert no panel pixels when the recording shadow is VISIBLE | CI optional job | Advisory |
| Device (Royalty, end of build only) | 10 cold starts; Bigo Game Live share with OFFGRID opened mid-share; 2-hour stream with recording segments; Phone 2 transfer of a 300 MB clip; crossfade/reverse; lock/unlock from both phones | S25 Ultra + Phone 2 | Royalty's sign-off |
| Soak | 6 h stream with deck loop + recording; watch fps, thermal, memory | Royalty, Phase 5 | Royalty's sign-off |

Per the standing rules: **no phone tests between rounds.** Rex and QA judge from code and CI; Royalty tests at the very end.

---

## 14. Autonomous continuous improvement (process proposal)

**Cadence**
- **Weekly research sweep (Grok Bot research subagent, about 30 min):** new reviews and complaints for PRISM, Streamlabs, Larix, CameraFi, OBS Blade, and Bigo; Android/CameraX/Media3 release notes; Samsung One UI changes touching MediaProjection or the screen-recording callback. Output: `handoff/20 OFFGRID Council/research/yyyy_mm_dd_research_sweep.md`.
- **Bi-weekly council (this format, rounds of ≤ 7):** a short MD with debates logged, plus a proposed backlog delta. Status labels: planned / attempted / completed / verified.
- **Monthly design review:** screenshot audit against Lumen tokens (contrast, glow, emoji guard, alignment on the 8 dp grid). Linear's lesson: pay design debt in sweeps, not crumbs [S34b].

**Gates (every change)**
1. **Plan:** Rex writes a task card (goal, files, acceptance criteria, tests, model/agent pick).
2. **Build:** a coding agent works only on its own lane branch (underscores in new names, no deletions, archive instead), and opens a **PR against `final-build`**.
3. **Prove:** CI green (unit + Robolectric + lint + guards) plus a checklist comment with the real test output. "Passed" is never claimed without CI evidence.
4. **Review:** a *different* agent reviews (security, performance, a11y checklist). Rex reads the diff summary.
5. **Approve:** **Royalty alone merges into `final-build`.** Nothing touches `main` without Royalty's explicit go. Agents never merge.
6. **Learn:** a post-merge note records what worked, the model used, cost and time, which updates the agent/model capability grid in `00 Rex Rules` (only with Royalty's approval).

**Rex picks the model per task:** Hard (GL, CameraX, crypto, media) → Claude Code in Ultracode, top tier. Medium/mechanical → Copilot coding agent. Research/docs → Grok Bot subagents. Re-check options at each kickoff (Standing Rule 1).

**Guardrails that never relax:** no app virtualization or camera hooking, no root, no new permission without Royalty's approval, no on-device AI beyond MediaPipe, no secrets in Git, AGP 8.13.2 unchanged.

---

## 15. Rex's own critique (what a senior engineer would still flag)

1. **One GL thread does everything:** camera, AR deck, overlay latch, stage, recorder, replay, live outputs, remote preview encode. Recording + replay + P2 preview + crossfade can blow 16.6 ms. *Measure first* (per-stage GPU timers), then skip the replay draw while recording if needed.
2. **`MAX_STAGE_SIDE = 1920f`** while the S25 Ultra panel is 3120 px tall. The stage is upscaled on screen, so laser lettering and glow edges *on the stage* (Laser Words, Status Word) look soft in the broadcast. Proposal: 2340 for the stage when thermal allows (the governor steps it down).
3. **`idleCurtainColor()` keeps a hand-written list of "active" keys.** Any new feature (Sets, A/B crossfade, Lab items) can be forgotten, and the curtain will drop to black over live content. Derive "is anything active" from `StagePlan` + deck state instead.
4. **Remote settings are prefix-allowlisted (`fx.*`)**, and `LiveIntegration` writes any such key from Phone 2 straight into settings. Replace with an explicit per-key allowlist with type and range checks (a 0–100 int, a known choice). Defense in depth even on a paired channel.
5. **`EncryptedChannel` requires strict in-order counters.** Any desync kills the session. With file transfer adding load, write a reconnect-and-resume path (session resumption, transfer offsets) and a test for it.
6. **The floating overlay (`SYSTEM_ALERT_WINDOW`) draws on top of Bigo and is captured by the share.** Keep it (mandate 20), but it must hide under Broadcast Lock, and its own content must follow the Lumen no-emoji rule.
7. **Android 15 QPR1 stops a projection when the screen locks** [S1]. `keepScreenOn` only applies while OFFGRID is on top. Add a Green Room check: "Screen timeout ≥ 10 min or Bigo keeps the screen on".
8. **`LiveActions.CLEAN_OUTPUT` already exists** (it hides `controlBar` and the panel). Broadcast Lock must *absorb* it (one source of truth), not run beside it, or the two will fight.
9. **Slider drags write prefs on every frame** if wired naively. Apply in memory immediately and debounce disk writes (150 ms) to avoid jank.
10. **The earlier council fixes (selfie flip, typewriter color, W/H boxes) are in `be674e1` as `65acdea`.** The old local branch `fix_council_b3a7fd9` on the box is now obsolete. Keep it as an archive only; nobody should push it.
11. **PreviewView is in `COMPATIBLE` (TextureView) mode.** That's one extra composition copy. Investigate `PERFORMANCE` (SurfaceView) once Broadcast Lock is in. Measure latency and battery; don't switch blind.
12. **Reverse playback by re-encoding** costs storage (about 2× per clip). Add a deck storage budget (default 4 GB) with LRU archive of reversed proxies (archived, not deleted, until Royalty clears them).
13. **Test-diamond and diagnostics paint into the video.** The lock must hard-refuse them, and CI should assert `testShapeOn == false` whenever locked.
14. **Accessibility of a pure-black, glow-only UI:** without A1's floor (position + fill + glyph, 1 dp hairline at glow 0%), the panel becomes unusable in bright rooms. This is a must, not a nice-to-have.

---

## 16. Scriptural Foundation for Building to Perfection (KJV)

| Verse (KJV, verified) | Build principle |
|---|---|
| **Proverbs 27:17**: "Iron sharpeneth iron; so a man sharpeneth the countenance of his friend." | Every PR is reviewed by a *different* agent. Council seats must build on and challenge each other. |
| **Proverbs 15:22**: "Without counsel purposes are disappointed: but in the multitude of counsellors they are established." | The 25-seat council and its debate log come before any code. |
| **Proverbs 24:3–4**: "Through wisdom is an house builded; and by understanding it is established: And by knowledge shall the chambers be filled with all precious and pleasant riches." | Research first (§4), understanding (root-cause diagnosis, §10.1), then fill the "chambers" (folders) with precious, well-placed items (§9). |
| **Luke 14:28**: "For which of you, intending to build a tower, sitteth not down first, and counteth the cost, whether he have sufficient to finish it?" | Phase 0 approvals, effort estimates per item, and a thermal/storage budget before features. |
| **Exodus 31:3–5**: "And I have filled him with the spirit of God, in wisdom, and in understanding, and in knowledge, and in all manner of workmanship, To devise cunning works, to work in gold, and in silver, and in brass, And in cutting of stones, to set them, and in carving of timber, to work in all manner of workmanship." | Craftsmanship bar: bespoke glyphs, baked laser lettering, pixel alignment, no generic look. |
| **Proverbs 22:29**: "Seest thou a man diligent in his business? he shall stand before kings; he shall not stand before mean men." | Diligence: tests for every change, CI evidence, a 6-hour soak before calling it done. |
| **1 Corinthians 14:40**: "Let all things be done decently and in order." | Folder IA: function-first categories, max depth 3, every item in its right place (§9.3). |
| **Colossians 3:23**: "And whatsoever ye do, do it heartily, as to the Lord, and not unto men;" | Build to the standard of the work itself, not just to pass review. |

*(Wording checked against the KJV text via bible-api.com, translation=kjv [S44].)*

---

## 17. Approvals needed and open gaps

| # | Needs Royalty's decision | Blocks |
|---|---|---|
| A1 | Add `DETECT_SCREEN_RECORDING` to the CI permission allowlist | Phase 1.3 auto-lock (manual lock works without it) |
| A2 | Change the CI "no sliders" guard to allow only `BioSliderView` | Phase 2.4 |
| A3 | Approve the 12-folder IA (§9.4) and the archive list (Titanium frame, Sapphire glass, Record length, Clip id) | Phase 2.5 |
| A4 | Unlock policy: allow unlocking during an active share? (default **no**: auto-lock holds while the recording is visible) | Phase 1.3 |
| A5 | Voice cues on Phone 2 (cloud API, Keystore key) | Lab #8 |
| A6 | Optional emulator CI job (longer CI time) | Phase 5 |

**Open gaps:** Samsung One UI behavior of the recording callback with Bigo (UNVERIFIED); whether overlay windows count as "activities being recorded" (UNVERIFIED, mitigated by hiding them under lock); Media3 prewarming API availability on 1.11.x (UNVERIFIED, guarded); no real-user card sort yet.

---

## 18. Sources

**Android platform**
- [S1] Android 15 features: Screen recording detection, media projection chip and auto-stop, app screen sharing: https://developer.android.com/about/versions/15/features#screen-recording-detection
- [S2] `WindowManager` reference (`addScreenRecordingCallback`, `SCREEN_RECORDING_STATE_*`): https://developer.android.com/reference/android/view/WindowManager
- [S3] AOSP `ScreenRecordingCallbacks.java`: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/ScreenRecordingCallbacks.java
- [S4] `Manifest.permission.DETECT_SCREEN_RECORDING` (Added in API 35, Protection level: normal): https://developer.android.com/reference/android/Manifest.permission#DETECT_SCREEN_RECORDING
- [S42a] Haptics design principles: https://developer.android.com/develop/ui/views/haptics/haptics-principles
- [S42b] Add haptic feedback to events: https://developer.android.com/develop/ui/views/haptics/haptic-feedback
- [S43a] Media3 issue: audio stutters with repeat-one: https://github.com/androidx/media/issues/3016
- [S43b] Media3 issue: seamless transitions to clipped items with a second decoder: https://github.com/androidx/media/issues/1400
- [S43c] ExoPlayer blog: Repeat modes in ExoPlayer: https://medium.com/google-exoplayer/repeat-modes-in-exoplayer-19dd85f036d3

**Streaming apps and reviews**
- [S5] PRISM Live, App Store ratings and reviews: https://apps.apple.com/us/app/prism-live-streaming-app/id1319056339?see-all=reviews
- [S6] PRISM Live Studio, Google Play: https://play.google.com/store/apps/details?id=com.prism.live&hl=en_US
- [S7] r/PrismLive "System issues prism +": https://www.reddit.com/r/PrismLive/comments/1nz6v6q/system_issues_prism/
- [S8] naver/prism-live-studio GitHub issues: https://github.com/naver/prism-live-studio/issues
- [S9] r/Twitch "Better alternative to streamlabs mobile?": https://www.reddit.com/r/Twitch/comments/1ekgsfr/better_alternative_to_streamlabs_mobile/
- [S10] r/streamlabs "Streamlabs for mobile is atrocious for multistream": https://www.reddit.com/r/streamlabs/comments/1cops95/streamlabs_for_mobile_is_atrocious_for/
- [S11] Larix Broadcaster, Google Play: https://play.google.com/store/apps/details?id=com.wmspanel.larix_broadcaster&hl=en_US
- [S12] Larix FAQ (Softvelum): https://softvelum.com/larix/faq/
- [S13] Larix review (Japanese, no drag-and-drop overlay placement): https://monosuki-tech.hateblo.jp/entry/larix-broadcaster/
- [S14] CameraFi: Widget transition to paid feature (watermark): https://blog.camerafi.com/2024/12/notice-widget-transition-to-paid-feature.html
- [S15] OBS Blade GitHub (Kounex/obs_blade): https://github.com/Kounex/obs_blade
- [S16] OBS Blade site: https://obs-blade.kounex.com/
- [S17] StreamYard Branding: https://streamyard.com/branding · Capterra reviews: https://www.capterra.com/p/213044/StreamYard/
- [S18] OBS Studio settings pages (source): https://github.com/obsproject/obs-studio/blob/fb4d98bf/frontend/settings/OBSBasicSettings.hpp · OBS overview guide: https://obsproject.com/kb/obs-studio-overview
- [S19] Lightroom mobile edit panel (Android): https://helpx.adobe.com/lightroom-cc/using/edit-panel-android.html · Adobe blog, new mobile interface: https://blog.adobe.com/en/publish/2023/07/26/adobe-photoshop-lightroom-has-new-mobile-interface-make-editing-even-easier-on-go
- [S20] CapCut users on clutter: https://www.reddit.com/r/CapCut/comments/1oytums/transition_organization_is_a_nightmare/ · https://www.reddit.com/r/CapCut/comments/1qtgkme/i_uninstalled_capcut_today/
- [S21] Blackmagic Camera for Android hands-on (9to5Google): https://9to5google.com/2024/07/31/blackmagic-camera-for-android-hands-on/ · Videomaker review: https://www.videomaker.com/reviews/software/blackmagic-camera-review-todays-best-camera-app-for-professionals/?amp=1
- [S22] Bigo Live: How to play and stream games (screen share flow): https://www.bigo.tv/blog/stream-games-bigo-live

**UX and IA research**
- [S23] NN/g Flat vs. Deep Hierarchies: https://www.nngroup.com/articles/flat-vs-deep-hierarchy/
- [S24] NN/g The 3-Click Rule Is False: https://www.nngroup.com/articles/3-click-rule/
- [S25] NN/g Progressive Disclosure: https://www.nngroup.com/articles/progressive-disclosure/
- [S26] NN/g Card Sorting: https://www.nngroup.com/articles/card-sorting-definition/
- [S27] NN/g Findability testing (tree testing, closed card sort): https://www.nngroup.com/articles/navigation-ia-tests/
- [S28a] Interaction Design Foundation, Hick's law: https://www.interaction-design.org/literature/topics/hick-s-law
- [S28b] "How Relevant is Hick's Law for HCI?" (CHI 2020): https://dlnext.acm.org/doi/fullHtml/10.1145/3313831.3376878
- [S28c] Sears & Shneiderman, "Split menus": https://doi.org/10.1145/174630.174632

**Design excellence**
- [S29a] The Verge, Arc review: https://www.theverge.com/23462235/arc-web-browser-review
- [S29b] Arc Help, Spaces: https://resources.arc.net/hc/en-us/articles/19228064149143-Spaces-Distinct-Browsing-Areas
- [S29c] TidBITS on Arc (theme picker, intensity, noise): https://tidbits.com/2023/05/01/arc-will-change-the-way-you-work-on-the-web/
- [S30] Apple, Behind the Design: Halide Mark II: https://developer.apple.com/news/?id=x6bv1a36
- [S31a] Apple Design Awards 2020: https://developer.apple.com/design/awards/2020/
- [S31b] Apple, Behind the Design: Darkroom: https://developer.apple.com/news/?id=nxnjx66e
- [S32] MacStories, Things 3: https://www.macstories.net/reviews/things-3-beauty-and-delight-in-a-task-manager/
- [S33] Teenage Engineering OP-1 layout guide: https://teenage.engineering/guides/op-1/original/layout
- [S34a] Linear, How we redesigned the Linear UI (part II): https://linear.app/now/how-we-redesigned-the-linear-ui
- [S34b] Linear, A design reset (part I): https://linear.app/now/a-design-reset
- [S35] Procreate Handbook, QuickMenu: https://help.procreate.com/procreate/handbook/interface-gestures/quickmenu
- [S36] Figma blog, Spotify's design system (Encore): https://www.figma.com/blog/creating-coherence-how-spotifys-design-system-goes-beyond-platforms/
- [S37a] UX Design Awards, Bang & Olufsen App, Beosonic: https://ux-design-awards.com/winners/bang-olufsen-app-beosonic
- [S37b] B&O support, dark mode in the app: https://support.bang-olufsen.com/hc/en-us/articles/42745855018257-Does-the-Bang-OIufsen-app-support-dark-mode
- [S38] Rolex digital design system analysis (shadcn.io): https://www.shadcn.io/design/rolex

**Color and dark UI**
- [S39] Material Design, Building a Material Dark Theme on Android: https://m3.material.io/blog/android-dark-theme-tutorial
- [S40a] Purdue Research, Dark mode battery study: https://www.purdue.edu/research/features/stories/dark-mode-may-not-save-your-phones-battery-life-as-much-as-you-think-but-there-are-a-few-silver-linings/
- [S40b] Dash et al., "How much battery does dark mode save?" (MobiSys '21): https://dl.acm.org/doi/pdf/10.1145/3458864.3467682
- [S41a] Elliot, "Color and psychological functioning: a review" (PMC): https://pmc.ncbi.nlm.nih.gov/articles/PMC4383146/
- [S41b] "Trustworthy Blue or Untrustworthy Red" (J. Marketing Theory and Practice): https://doi.org/10.1080/10696679.2019.1616560
- [S41c] "The Color of Status: Color Saturation, Brand Heritage, and Perceived Status of Luxury Brands" (J. Consumer Research): https://doi.org/10.1093/jcr/ucaf029

**Scripture**
- [S44] bible-api.com (KJV): https://bible-api.com/proverbs+27:17?translation=kjv (same endpoint used for each verse)

**Code read** (`final-build` @ `be674e1`): `CompositorProcessor.kt`, `StagePainter.kt`, `CompositorEffect.kt`, `MainActivity.kt`, `camera_bind.kt`, `status_line.kt`, `recording_controls.kt`, `ar_deck_player.kt`, `ar_deck_surface.kt`, `OverlayRenderer.kt`, `DarkStage.kt`, `terminal/PanelTree.kt`, `terminal/EffectFolders.kt`, `terminal/TopBar.kt`, `terminal/TerminalPanel.kt`, `terminal/PanelLook.kt`, `effects/layers/EmojiRainLayer.kt`, `live/RemoteMessage.kt`, `live/RemoteSession.kt`, `live/EncryptedChannel.kt`, `LiveIntegration.kt`, `res/layout/activity_main.xml`, `res/values/strings.xml`, `res/values/themes.xml`, `AndroidManifest.xml`, `.github/workflows/build-debug-apk.yml`.

---

*End of PROPOSAL. Nothing here is approved until Royalty says go.*
