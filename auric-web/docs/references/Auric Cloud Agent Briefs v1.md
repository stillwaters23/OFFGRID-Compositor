# Auric Timer: Cloud Agent Briefs v1

| | |
|---|---|
| **Date** | Fri Oct 9 2026, CT |
| **Status** | **DRAFT, awaiting Royalty review.** Do not launch any agent from this file until Royalty approves it. |
| **Project** | Auric Timer web app (Vite + TanStack Start + React 19, zustand, Tailwind v4) |
| **Source of truth** | Code: `Timer/Grok Build 4.6 Workspace/`. Spec: `Timer/01 Auric Time Crowned Brainstorm.md`. Also `AGENTS.md`, `AGENTS.project.md`, `README.md`, `AURIC Time Crowned The Ice Pavilion Bible.md` (the Bible). |
| **Visual targets** | `References/Auric Laser Strip Target.png` (laser row), `References/Auric Current Pill Band.jpg` (pill band to replace), `References/Auric Folder Icons Target.png` (folder look and 3D icons) |
| **Royalty's rule** | Instructions matter more than the model. Every brief has (a) a polished brief, (b) DO and DON'T rules, (c) a GO directive. |

---

## Contents

0. [Read-first: what the code actually does today](#0-read-first-what-the-code-actually-does-today)
1. [Build order and lane map](#1-build-order-and-lane-map)
2. [Write-target ownership (no overlap)](#2-write-target-ownership-no-overlap)
3. [Global rules (part of every brief)](#3-global-rules-part-of-every-brief)
4. [Lane 1: Wheel, Click, Laser Strip, S-1 resize fix](#lane-1-wheel-click-laser-strip-s-1-resize-fix)
5. [Lane 2: Reel Look Settings (laser color, number color with contrast guard, laser dimmer) and the state contract](#lane-2-reel-look-settings)
6. [Lane 3: Folders (size, layout menu, matte etched text, icon styles, long-press, snap-back, rename)](#lane-3-folders)
7. [Lane 4: Clicker Sounds](#lane-4-clicker-sounds)
8. [Lane 5: QA Review](#lane-5-qa-review)
9. [Open questions for Royalty](#open-questions-for-royalty)
10. [Proposals (not approved)](#proposals-not-approved)

---

## 0. Read-first: what the code actually does today

Every lane agent reads this section before touching code. File paths are relative to the workspace root (`Grok Build 4.6 Workspace/`).

| Area | Real location | Fact that matters |
|---|---|---|
| 3D wheel | `src/components/auric/drum-reel.tsx` | `DrumReel` uses a rAF loop with momentum (`vel *= 0.94`) and snaps to the nearest row. It renders 5 items (`VISIBLE = 5`, `ANGLE = 26`). Sizes are `lg / md / sm / plaque / hero / trio`, and the barrel width, radius, and font come from **fixed px constants**. Height is `calc(baseH px * var(--digit-scale))`. It also has keyboard, wheel, and pointer input plus `role="slider"`. |
| Pill band + arrows (to replace) | `<div className="aperture" />` in `drum-reel.tsx`; CSS `.aperture`, `.aperture::before/::after` in `src/styles.css` (about lines 562–609) | The blue pill is `.aperture`. The arrow markers are its `::before/::after` border triangles. The aperture height is `--reel-font * --digit-scale * 1.62`, so it does **not** track the cqw-clamped font. |
| Wheel banks | `src/components/auric/span-reels.tsx` | The HMS bank is 3 `trio` reels inside `.reel-console`. The Days/Weeks/Months/Years bank is 4 `sm` reels in a `grid-cols-4 gap-1`. It is mounted by `timer-view.tsx` (`onTick={() => reelFeel()}`). |
| Other wheels using `.aperture` | `pavilion.tsx` (SoundDeck `plaque`), `appearance-studio.tsx`, `alarm-view.tsx`, `remind-view.tsx`, `pit-wheel.tsx`, `spectrum-scroll.tsx`, `notice-bell.tsx`, `folder-stage.tsx` | These keep the aperture unless Royalty says otherwise (see Open questions). |
| Tick + haptic | `src/lib/audio.ts` `reelFeel()` | Plays `playReelTick(s.reelSound)` if `soundEnabled !== false`, then `navigator.vibrate([12,18,42])`. **It does not check `hapticEnabled`.** |
| Tick catalog | `src/lib/ticks.ts` | `TICK_FAMILIES = Click, Ice, Crystal, Silk, Gear, Bolt, Paper` × 77 = 539. `HOUSE_CLICK_ID = "click-01"` (1640 Hz square, 35 ms, gain 0.018). `verifyTicks()` exists. Note that the Bible lists "Pulse" as a family, but the code has "Click". **Code wins.** |
| Sound banks | `src/lib/banks.ts` | Remapped copies for stopwatch, icon, settings, and heartbeat (`sw-`, `ic-`, `se-`, `hb-` prefixes). |
| Existing sound pickers | `pavilion.tsx` `TickRoom` → `SoundDeck "The Wheel"`; `folder-stage.tsx` `SoundList kind="timer"` (module `timer-sounds`) | Both stay untouched. |
| Alarms | `src/lib/wake.ts`, `alarm-view.tsx`, `alarm-alert.tsx` | **Out of scope** until Lane 1 is accepted. |
| Settings state | `src/lib/store.ts` (1262 lines, zustand `persist`, key `auric-crowned-v21`, explicit `partialize` list) | `digitScale` defaults to 50 and is clamped to 0–100. `glowIntensity` defaults to 36. `customizationFolder` is a string union. New persisted fields **must** be added to `partialize` or they will not persist. |
| Root CSS var applier | `src/components/auric/glow.tsx` | Sets `--felt --chrome --digit --accent --glow --glow-intensity --digit-scale` (`0.85 + scale/100*0.45`) on `<html>`. |
| Appearance draft/apply | `src/lib/appearance.ts`, `appearance-studio.tsx` | Uses a draft, then Apply or Discard. `appearanceIssues()` already checks digit-on-felt ≥ 4.5. `toLight()` forces felt `#F6F1E8` and chrome `#E8E0D2`. |
| Contrast math | `src/lib/color.ts` | Has `relativeLuminance`, `contrastRatio`, and `ensureReadable`. Note that **`ensureReadable` can set digits to `#000000` on light felt.** |
| Existing laser keyframe | `src/styles.css` `@keyframes ice-laser` (rotate), used by `.ice-switch-laser` on `switch.tsx` | This is a rotating conic ring on switches. Do not change its behavior. |
| Live shell | `app.tsx` (`.tx-root`, `data-mood="light|dark"`), `session-shell.tsx` (renders `<FolderGrid />` after `~ $ ls folders`), `folder-grid.tsx`, `glass-folder.tsx`, `folder-stage.tsx`, `modules.ts` | Folders are `MODULES` entries. `FolderStage` switches on `ModuleId`. `SIZE_STEPS = [20..100]` in `folder-stage.tsx` controls **digitScale** (the Sizing module), **not** folder size. |
| Tests | `tests/auric.test.ts`, `tests/brainstorm.test.ts` via `npm run test:auric` (Node `--test`, `--experimental-strip-types`, `scripts/test-aliases.mjs`) | The test script lists files explicitly. Playwright and `scripts/browser-smoke.mjs` exist for rendered checks. |
| Spec S-1 | Brainstorm §11.5 / bug table | "Scroll wheels clip at the edges when text size increases. The wheel runs off the page. The wheel must resize with the font so it never disappears off-page." Royalty also reports that **window resizing** breaks the wheels. |
| Repo state | The unpacked folder | **It is not a git repository.** See Open question Q1. |

**Math finding (drives Lane 2):** At the selected row, the wheel center is about `#0A1730` in dark theme (relative luminance about 0.009) and about `#EBE4D8` in light theme (about 0.78). To pass 4.5:1 against the dark wheel, a color needs luminance ≥ 0.21. To pass 4.5:1 against the light wheel, it needs luminance ≤ 0.13. **No single number color can pass in both themes.** Lane 2 therefore stores one number color **per theme**. See Q3.

---

## 1. Build order and lane map

```
Phase A  Lane 1  Wheel + Click + Laser demo + S-1 fix  ──►  Lane 5 QA (L1)  ──►  Royalty accepts L1
Phase B  Lane 2  Reel Look Settings + State Contract   ──►  Lane 5 QA (L2)
         Lane 3  Folders  (may start once L2 milestone 2.0 "state contract" is merged)  ──►  Lane 5 QA (L3)
Phase C  Lane 4  Clicker Sounds                        ──►  Lane 5 QA (L4)
Out of scope until L1 is accepted: alarms, presets, wake.ts, anything else not listed here.
```

| Lane | Branch | One line |
|---|---|---|
| **L1** | `auric/l1-wheel-laser` | One continuous laser strip across the reels replaces the pill and arrows. The wheel feel and default click-01 tick + haptic are verified. S-1 resize/scale fix. |
| **L2** | `auric/l2-reel-look-settings` | Owns `store.ts`. Adds the laser color chooser, the per-theme number color chooser with a WCAG ≥ 4.5 guard, and the laser dimmer (toggle + 0–100). Ships the state contract that L3 uses. |
| **L3** | `auric/l3-folders` | Folder size 0–100 with an overflow guard, a corner ⋯ layout menu (grid / big / single) with animated transitions, matte laser-etched titles, per-folder icon style (3D / flat / etched), long-press for size or rename, and drag snap-back. |
| **L4** | `auric/l4-clicker-sounds` | A new "Clicker Sounds" folder with the 539 ticks in 7 styles and preview-on-tap. |
| **L5** | `auric/l5-qa` | A read-only reviewer. Grades each lane PR against its acceptance criteria with evidence. Never edits app code. |

L2 and L3 were merged where it made sense. Specs 2, 3, and 4 share one settings surface and one store slice, so they form one lane (L2) with three milestones. Specs 7–13 form one lane (L3). That way no two lanes write the same file.

---

## 2. Write-target ownership (no overlap)

**Single owners for shared files.** Other lanes use only the listed hooks.

| Shared file | Owner | Hook other lanes use (read-only for them) |
|---|---|---|
| `src/styles.css` | **L1** | L1 adds exactly three `@import` lines directly after `@import "tailwindcss";`, for `./styles/reel-look.css` (L2), `./styles/folders.css` (L3), and `./styles/clicker.css` (L4). L1 creates each file with only an ownership header comment. After L1 merges, **nobody edits `styles.css`** without a Royalty-approved change. |
| `package.json` (scripts only) | **L1** | L1 changes `test:auric` to run `tests/*.test.ts` (glob). It also adds `"test:e2e": "node --test tests/e2e/"`. Later lanes add files only. Dependencies: no lane adds packages without Royalty approval. |
| `src/lib/laser.ts` (new) | **L1** | Exports `LASER_DEFAULT`, `LASER_PRESETS`, `LASER_BLOOM_PEAK_ALPHA`, `laserVars({color, intensity, enabled})`, and `laserBloomComposite(bgHex, laserHex, intensity)`. |
| `src/lib/store.ts` | **L2** | L2 milestone 2.0 adds every new persisted field and setter for L2 **and L3** (see L2 → State contract). L3 and L4 call selectors and setters only. |
| `src/components/auric/glow.tsx` | **L2** | It is the only place that writes the new root CSS vars: `--laser-color`, `--laser-core`, `--laser-bloom-a`, `--laser-spread`, `--laser-on`, `--reel-number`, `--folder-size`. L1 styles consume them with defaults. L3 consumes `--folder-size`. |
| `src/lib/appearance.ts` | **L2** | `AppearanceDraft` gains the laser and number fields. |
| `src/lib/modules.ts`, `src/components/auric/folder-stage.tsx` | **L4** | L3 must not edit these. L3 treats module ids generically. |
| `src/components/auric/session-shell.tsx` | **L3** | Minimal diff: the corner ⋯ button and the mount of the layout sheet only. |
| `src/lib/audio.ts`, `src/lib/haptics.ts`, `src/lib/ticks.ts`, `src/lib/banks.ts` | **L1** (audio.ts only if a defect is proven) | L4 imports `playReelTick`, `TICKS`, and `TICK_FAMILIES`. L3 imports `hapticTick`. |
| `src/lib/color.ts`, `pavilion.tsx`, `wake.ts`, `alarm-*.tsx`, `timer-view.tsx` | **Nobody** in v1 | Read-only. |

**Permanently forbidden for all lanes:** `public/__grok/`, `server/`, `scripts/grok-pwa-*`, `scripts/with-app-env.mjs`, `vite.config.ts`, `tsconfig.json`, `AGENTS.md`, `AGENTS.project.md`, `startup.sh`, `migrations/`, `.grok/`, `.vercel/`, `public/brand/`, the Bible, `README.md`, the Brainstorm spec, `artifacts/qa/REBUILD-REPORT.md`, `artifacts/qa/*.json`, and `package-lock.json`.

---

## 3. Global rules (part of every brief)

Paste this block verbatim at the top of every lane prompt.

> **GLOBAL RULES: AURIC v1**
> 1. **Never delete work.** Archive instead of deleting. To retire code or CSS, move it to `archive/2026-10-09-<lane>/` with a `README.md` that says what moved, from where, and why. Never `git push --force`, never delete a branch (local or remote), never rewrite pushed history, and never delete files outside `archive/` moves.
> 2. **Own branch, own PR.** Work only on your lane branch, cut from the agreed base. Open a **draft PR**. Never merge your own PR, and never push to the base branch.
> 3. **Stay in your write targets.** You may read anything. Write only to your PERMITTED list. If you need a change in another lane's file, write it as a "Hook request" in your PR description and stop. Do not make the change.
> 4. **60 fps.** Animate only `transform` and `opacity`. No layout-thrashing reads inside rAF. No animated `filter: blur`, `box-shadow`, or `backdrop-filter`. A static bloom is fine. Median frame interval ≤ 17.5 ms and p95 ≤ 25 ms during the lane's motion test in Chromium on the CI box. Report the numbers.
> 5. **8dp grid.** New spacing, sizes, radii, and offsets are multiples of 8 px. 4 px half-steps are allowed only for hairlines, borders, and insets ≤ 4 px, with a comment. Tap targets are ≥ 44 × 44 px (Bible §14).
> 6. **Unit tests.** Every new pure function has `node --test` tests in `tests/<lane>-*.test.ts`. Rendered behavior has Playwright checks in `tests/e2e/<lane>-*.e2e.mjs`. `npm run typecheck`, `npm run lint`, `npm test`, and `npm run build` must all pass, and the PR shows their output.
> 7. **No generic AI-looking UI.** No default shadcn cards, no purple-gradient hero blobs, no emoji icons, no stock "glassmorphism" presets, no lorem ipsum, no placeholder avatars. Use the existing Auric tokens (`--felt --chrome --digit --accent --glow`, `.tx-*` classes), the Bible's vocabulary, and the reference images.
> 8. **Keep existing behavior unless the spec says to change it.** Defaults must look and behave like today except where your lane's spec says otherwise. Old persisted state (`auric-crowned-v21`) must hydrate without loss.
> 9. **Reduced motion.** Under `prefers-reduced-motion: reduce`, decorative animation stops and transitions become instant. Function stays the same.
> 10. **Both themes, all widths.** Verify `data-mood="dark"` and `"light"` at 320, 390, 768, and 1280 px wide. No horizontal overflow (`scrollWidth ≤ clientWidth`).
> 11. **Costs and scope.** No paid APIs, no image-generation tools, no new cloud services, and no new npm dependencies without Royalty approval. Art is SVG, CSS, or canvas. Alarms and presets are out of scope.
> 12. **Report honestly.** Your PR description lists every acceptance criterion with PASS, FAIL, or NOT-VERIFIED plus evidence (test name, screenshot path under `screenshots/<lane>/`, or perf numbers). Do not claim device or haptic results you could not observe.

---

## Lane 1: Wheel, Click, Laser Strip, S-1 resize fix

**Branch:** `auric/l1-wheel-laser` · **Phase:** A (first; everything else waits for Royalty to accept it) · **Suggested model/effort (proposal only):** strongest available coding model, effort **high** (3D transforms, layout measurement, perf).

### (a) Brief

You are making the Auric wheel feel finished. Four deliverables, in order:

1. **Laser strip.** Replace the blue pill band (`.aperture`) and its arrow markers (`::before/::after`) on the timer's span reels with **one continuous laser beam** across the selected row. Match `References/Auric Laser Strip Target.png`:
   - very bright and laser-like
   - a thin **white-hot core** (#FFFFFF, 2 px at default size)
   - a **saturated colored bloom** (default red) with a **soft, wide falloff**
   - **continuous** across all reels in a bank, crossing the gaps between barrels
   - **extending past the outer wheel edges** (at least 16 px each side) and fading to transparent
   - selected digits glowing white on top of the beam

   Build it as **one** element per bank, positioned in `span-reels.tsx` over the bank, made of stacked CSS gradients and a static `box-shadow` or pre-blurred gradient (no animated blur, no WebGL needed). Use `mix-blend-mode: screen`, or z-order the glyphs above the beam, so the core never paints over digit pixels in a way that hurts legibility. Do this for **both** banks: the HMS `trio` bank and the Days/Weeks/Months/Years `sm` bank.

   Read its color and strength from CSS vars with defaults: `--laser-color` (default from `LASER_DEFAULT`), `--laser-core`, `--laser-bloom-a`, `--laser-spread`, `--laser-on`. L2 writes them later. You only ship defaults plus `src/lib/laser.ts`.

   Add a `selector?: "aperture" | "none"` prop to `DrumReel`, **defaulting to `"aperture"`**, so every other wheel in the app (pavilion plaque decks, alarm, remind, spectrum, etc.) looks exactly as it does today. `SpanReels` passes `"none"` and renders `<LaserStrip />`. Keep the `.aperture` CSS (other wheels still use it).
2. **Laser demo.** Add a dev-only demo route `src/routes/laser-demo.tsx` showing both banks at digitScale 0, 50, and 100, in light and dark mood, with all 5 preset colors and intensity 0, 50, and 100 side by side. It renders through the real `SpanReels` and is not a mock. It is excluded from navigation. Royalty reviews the look here.
3. **Click + haptic.** Verify that `reelFeel()` fires exactly once per row change while dragging, flinging, using the mouse wheel, and using the keyboard. It must use the default `click-01` (`HOUSE_CLICK_ID`). Do not change the sound recipe. Add unit tests with mocked `AudioContext` and `navigator.vibrate`. Only fix `audio.ts` if a test proves a defect, and document it. (Known: `reelFeel` ignores `hapticEnabled`. **Do not change this.** It is an open question.)
4. **S-1 resize fix.** The wheels must survive (i) **window resize / rotation** live, and (ii) **digitScale 0–100**, without clipping, running off the page, losing the selected value, or misaligning the laser. Likely root causes in `drum-reel.tsx`:
   - fixed-px `barrelW`, `radius`, and `baseH` that ignore the real container width
   - `fontSize` derived from a constant, not a measured width
   - `paint()` not re-running on resize
   - aperture/row height computed from `--reel-font * --digit-scale` instead of the clamped font

   Use **one** `ResizeObserver` per reel to measure the barrel. Derive `radius`, row pitch, and font from the measured box. Re-`paint()` on resize, coalesced to one rAF. Clamp height with `min()`/`clamp()` so the bank never exceeds the viewport. Expose the **measured row center** so `LaserStrip` aligns to it.

### (b) DO / DON'T

**DO**
- Read `drum-reel.tsx`, `span-reels.tsx`, the `.aperture`, `.barrel`, `.reel-*` blocks in `styles.css`, `glow.tsx`, `audio.ts`, and `ticks.ts` before writing anything.
- Keep `DrumReel`'s public props backward compatible. Add only the optional `selector` prop.
- Keep momentum, snap, keyboard (`Arrow*`, `PageUp/Down`, `Home/End`), wheel, ARIA `role="slider"`, and `prefers-reduced-motion` snapping exactly as they are.
- Put all laser math (preset list, intensity → vars mapping, bloom composite for L2's contrast guard) in pure functions in `src/lib/laser.ts`, with tests.
- Make the strip **static**, with no animation by default. Any shimmer must be opacity-only and disabled under reduced motion.
- Check light mood: the beam must still read as a laser on the light wheel (bloom saturation holds, and the core may gain a thin colored edge).
- Add the three `@import` hooks and the empty owned CSS files for L2, L3, and L4 (see §2).

**DON'T**
- Don't delete `.aperture` or the arrow CSS. Other wheels use them. If something is truly retired, archive it.
- Don't touch `store.ts`, `glow.tsx`, `appearance*.ts(x)`, `modules.ts`, `folder-*.tsx`, `pavilion.tsx`, `wake.ts`, or any alarm or preset code.
- Don't add canvas or WebGL unless CSS fails the perf or look target. If you do, it must be a single canvas per bank, drawn only on resize or settings change, never per frame.
- Don't change tick recipes, `HOUSE_CLICK_ID`, or the vibrate pattern.
- Don't use `setInterval`, and don't read layout inside the pointer-move path beyond what exists today.

### Acceptance criteria (testable)

| # | Criterion | How it's verified |
|---|---|---|
| L1-1 | The span reels render **no** `.aperture` element and no arrow markers. Every other `DrumReel` usage still renders `.aperture` (count unchanged vs. baseline). | e2e DOM query on the timer and pavilion SoundDeck |
| L1-2 | Exactly **one** laser element per bank. Its left edge is ≥ 16 px left of the first barrel and its right edge ≥ 16 px right of the last barrel, but still inside the viewport. It is visible in the gaps between barrels. | e2e bbox check + screenshot `screenshots/l1/laser-*.png` |
| L1-3 | The beam's vertical center equals each reel's selected-row center within **±1 px** for all 4 calendar reels and all 3 HMS reels, at digitScale 0/50/100 and widths 320/390/768/1280. | e2e geometry test |
| L1-4 | The core pixel at the beam center outside glyphs is near white (each channel ≥ 240). The bloom pixel 8 px above the core is saturated laser color (HSV S ≥ 0.6 at default). Bloom alpha falls to < 0.05 by `--laser-spread`. | e2e screenshot pixel sampling |
| L1-5 | Selected digits stay legible over the beam: glyph pixels at the core line are white or near white, not laser-tinted below 4.5:1 vs. the bloom composite. | pixel sample + `laserBloomComposite` unit test |
| L1-6 | Defaults with no settings set: beam on, red `LASER_DEFAULT`, intensity 100. All vars fall back correctly when `glow.tsx` sets nothing. | unit + e2e |
| L1-7 | `prefers-reduced-motion: reduce`: no running animations on the strip (`getAnimations().length === 0`), and the reel still snaps instantly. | e2e with emulated media |
| L1-8 | **Perf:** a 3 s fling on the DWMY bank with the strip visible has median frame ≤ 17.5 ms and p95 ≤ 25 ms (Chromium, no CPU throttle). Repeat at 4× CPU throttle and report the numbers. | e2e rAF timing script; numbers in PR |
| L1-9 | **S-1, scale:** at digitScale 0, 50, and 100 and widths 320/390/768/1280: `scrollWidth ≤ clientWidth`, every barrel bbox is inside the viewport, and every selected glyph bbox is inside its barrel (no clipping). | e2e matrix (24 cases × 2 moods) |
| L1-10 | **S-1, live resize:** set values D=3 W=2 M=11 Y=77, then resize 1280→320→768 and rotate 390×844 → 844×390. Values are unchanged, the laser stays aligned (L1-3), and there are no console errors. | e2e |
| L1-11 | **Click:** dragging across N rows calls `reelFeel` N times. Holding still calls it 0 times. Keyboard ArrowUp ×5 calls it 5 times. It calls `playReelTick("click-01")` by default and `navigator.vibrate([12,18,42])`. | unit tests with mocks |
| L1-12 | `verifyTicks()` returns `[]`. The existing `tests/auric.test.ts` and `tests/brainstorm.test.ts` still pass unmodified. | `npm test` |
| L1-13 | `/laser-demo` renders all combinations with no console errors. It is not linked from the app UI. | browser-smoke + manual |
| L1-14 | Global gates: typecheck, lint, test, and build pass. 8dp audit of new CSS. Both moods checked. | PR checklist |

### Write targets

- **PERMITTED:**
  - `src/components/auric/drum-reel.tsx`
  - `src/components/auric/span-reels.tsx`
  - **new** `src/components/auric/laser-strip.tsx`
  - **new** `src/lib/laser.ts`
  - **new** `src/lib/reel-geometry.ts` (pure measure → radius/pitch/font math)
  - `src/styles.css` (the reel/laser blocks + the 3 `@import` lines)
  - **new** `src/styles/reel-look.css`, `src/styles/folders.css`, `src/styles/clicker.css` (header comment only)
  - **new** `src/routes/laser-demo.tsx`
  - `src/routeTree.gen.ts` (generated only)
  - `src/lib/audio.ts` (only if a test proves a defect)
  - `package.json` (`scripts` only)
  - **new** `tests/l1-*.test.ts`, `tests/e2e/l1-*.e2e.mjs`
  - `screenshots/l1/`
  - `archive/2026_10_09_l1/`
- **FORBIDDEN:** everything else, especially `store.ts`, `glow.tsx`, `appearance.ts`, `appearance-studio.tsx`, `pavilion.tsx`, `modules.ts`, `folder-*.tsx`, `session-shell.tsx`, `wake.ts`, `alarm-*.tsx`, `timer-view.tsx` (presets), and the global forbidden list in §2.

### (c) GO

> **GO, Lane 1.** Create branch `auric/l1-wheel-laser` from the agreed base. Read §0, §2, §3 and the files listed above. Write a 10-line plan in the draft PR description first. Then implement in this order, one commit each: geometry + S-1 fix → laser strip + `laser.ts` → CSS hooks → demo route → click/haptic tests. Run every gate, fill the acceptance table with evidence, and stop. Do not start any other lane's work.

---

## Lane 2: Reel Look Settings

*Specs 2 (laser color), 3 (number color + contrast guard), and 4 (laser dimmer), plus the shared **state contract** for Lane 3.*

**Branch:** `auric/l2-reel-look-settings` · **Phase:** B (after L1 is accepted) · **Suggested model/effort (proposal only):** strong coding model, effort **medium-high** (state, migration, color math).

### (a) Brief

Add a **"Laser & Numbers"** folder to the Customizations studio (`appearance-studio.tsx` `FOLDERS`, which shows in both the Themes module and the Pavilion Cut room). It has three sections, and every change goes through the existing **draft → Apply / Discard** flow.

- **2A, Laser color.**
  - Five preset chips: **Red (default)**, `#00E5FC`, `#0032FF`, `#FF00A6`, `#7B02B3`, all from `LASER_PRESETS` in `laser.ts`.
  - A custom hex field (reuse `normalizeHex` / `isValidHex` from `color.ts`) with live preview on a mini span-reel pinned at the top (the Bible's "live witness").
  - Invalid hex is refused with a message. Any valid opaque hex is allowed. The laser is decorative, so no contrast rule applies to it, but the number guard (2B) re-checks against the laser bloom.
- **2B, Number color + contrast guard.**
  - Numbers are the reel numerals at the selected row plus the reel readout plaque.
  - They get a chooser with **one color per theme**: `numberColor.dark` and `numberColor.light`. `null` means "follow palette digit".
  - New pure module `src/lib/contrast-guard.ts`:
    - `wheelBackgroundSamples(palette)`: replicates the `.barrel` and `.reel-console` background stops behind the selected row (oklab `color-mix` math, with a comment citing the CSS lines it mirrors)
    - `checkNumberColor(hex, theme, ctx)`: returns `{ ok, ratio, worstAgainst, message }`
  - **Rules:**
    - The color must be a valid opaque hex.
    - It must not be black or near-black (relative luminance < **0.012**, proposed; see Q4).
    - It needs ≥ **4.5:1** against **every** wheel background sample for that theme (both themes are computed with `committedPalette({ mode: "dark" | "light" })`) **and** against `laserBloomComposite(bg, laserColor, intensity)` at the current laser settings.
  - A failing color is **blocked**: the Apply button is disabled, the store setter refuses it, and the UI shows a clear sentence, e.g. *"#1A1712 is too dark on the dark wheel: 1.4:1, needs 4.5:1. Pick a lighter color."*
  - On hydration, a stored color that now fails (palette or laser changed) silently falls back to `null` and shows a one-time toast.
  - **Reel numerals must never render `#000000`**, even if `ensureReadable` returns black for `--digit`. `--reel-number` is always guard-approved.
- **2C, Laser dimmer.**
  - A metal switch for **Laser on/off** (reuse `switch.tsx`) and a **platinum slider 0–100** for intensity, persisted.
  - Intensity is linear via `laserVars()`. **0** = core at 20% opacity with no bloom. **100** = the full L1 target.
  - Off = no beam. The selected digits keep their white glow, and a 1 px neutral hairline marks the row (proposed; see Q6).
  - The slider follows the 8dp grid, is ≥ 44 px tall, and supports keyboard arrows.
- **2.0, State contract (first commit; L3 depends on it).** Add to `store.ts` (state + `partialize` + setters with clamps), `AppearanceDraft`, and `draftsDiffer`:

  | Field | Type | Default | Setter rule |
  |---|---|---|---|
  | `laserEnabled` | boolean | `true` | toggle |
  | `laserIntensity` | number | `100` | `boundedPercent` |
  | `laserColor` | hex | `LASER_DEFAULT` | `normalizeHex`, else refuse |
  | `numberColor` | `{ dark: string \| null; light: string \| null }` | `{ dark: null, light: null }` | `checkNumberColor` must pass, else refuse with `{ ok: false, reason }` |
  | `folderSizeAll` | number | `50` | `boundedPercent` |
  | `folderSizes` | `Record<string, number>` | `{}` | per-folder override, `boundedPercent`. `null` removes the override |
  | `folderLayout` | `"grid" \| "big" \| "single"` | `"big"` (must render like today's single-column list; see Q8) | enum |
  | `folderIconStyle` | `Record<string, "3d" \| "flat" \| "etched">` | `{}` (missing = `"flat"` = today's glyph) | enum |
  | `folderNames` | `Record<string, string>` | `{}` (missing = `spec.pkg`) | trim; 1–24 chars; empty refused |
  | `folderOrder` | `string[]` | `[]` (missing = current sort) | unknown ids dropped |

  `glow.tsx` writes `--laser-*`, `--reel-number` (resolved for the active mood), and `--folder-size` (the global value; per-folder values are applied inline by L3).

  "Restore readable appearance" resets `numberColor` to `{null, null}` and leaves the laser and folder settings alone.

### (b) DO / DON'T

**DO**
- Land **2.0 first** as its own commit and push it so L3 can branch from it. Then 2A → 2B → 2C.
- Reuse `contrastRatio` and `relativeLuminance` from `color.ts`, and `committedPalette` from `appearance.ts`. Note that `committedPalette` caches its last result, so compare by value.
- Write migration tests: a v21 snapshot without the new keys hydrates with the defaults above and loses nothing.
- Show live contrast ratios next to each number swatch (e.g. "Dark 12.8:1 ✓ · Light 1.2:1 ✗") in the existing `.tx-*` and `panel-glass` vocabulary.
- Mirror every UI block in the store (defense in depth): a devtools write through `useAuric.setState` of a failing color still never reaches `--reel-number`.

**DON'T**
- Don't edit `styles.css` (use `src/styles/reel-look.css`), `drum-reel.tsx`, `span-reels.tsx`, `laser.ts` (send L1 a hook request), `color.ts`, `modules.ts`, `folder-*.tsx`, or anything in L3/L4.
- Don't change `ensureReadable` or the existing `appearanceIssues` checks. Add your guard alongside them.
- Don't rename the persist key or remove existing `partialize` fields.
- Don't let the laser color pass through the number-contrast rules as a requirement. Only the number color is gated.

### Acceptance criteria (testable)

| # | Criterion | Verified by |
|---|---|---|
| L2-1 | Five presets render in this order: Red, `#00E5FC`, `#0032FF`, `#FF00A6`, `#7B02B3`. Choosing one and pressing Apply updates `--laser-color` and the live beam within one frame. | e2e |
| L2-2 | Custom hex accepts `#RGB` and `#RRGGBB` and refuses `#GGG`, `red`, and the empty string with a visible message. Discard restores the previous color. | unit + e2e |
| L2-3 | `checkNumberColor` unit table: `#000000` is refused in **both** themes (black rule). `#111111` is refused. `#FFFFFF` passes dark and fails light. `#1F2A44` passes light and fails dark. Exact ratios are asserted to 2 decimals for 6+ colors. | `tests/l2-contrast-guard.test.ts` |
| L2-4 | The guard uses the **actual** wheel samples. Changing palette felt or chrome changes the ratios. A unit test pins `wheelBackgroundSamples(BLUE_DIAMOND)` and the light variant to expected hex values (±1 per channel). | unit |
| L2-5 | A failing color: Apply is disabled, the message names the theme, the ratio, and the 4.5:1 need, and the store setter returns `{ ok: false }` with state unchanged. | unit + e2e |
| L2-6 | Laser re-check: a number color that passes on the plain wheel but fails on the bloom composite at the current laser settings is refused with a laser-specific message. | unit |
| L2-7 | `--reel-number` is never `#000000` or near-black in either mood, for every `CURATED` palette × both moods. | unit loop |
| L2-8 | The dimmer toggle hides the beam (L1 element `opacity 0` or not rendered) and the hairline remains. Intensity 0/50/100 maps to the `laserVars` outputs pinned in tests. | unit + e2e screenshots |
| L2-9 | **Persistence:** set laser `#FF00A6`, intensity 40, off, and number dark `#EAF3FF`, then reload. All four are restored. The v21 migration test passes. | e2e reload + unit |
| L2-10 | State contract: all 10 fields exist, are clamped and validated, and are in `partialize`, with tests for each setter's edge cases (−5→0, 150→100, `NaN`→default, empty name refused, 25-char name refused). | unit |
| L2-11 | Existing appearance tests and behavior are unchanged: the Apply/Discard/Restore flows still pass, and the default look equals baseline (screenshot diff of the timer ≤ 0.5% pixels outside the laser row). | e2e + existing tests |
| L2-12 | Global gates pass. 8dp audit. Both moods. 320 px width shows no overflow in the new folder. | PR checklist |

### Write targets

- **PERMITTED:**
  - `src/lib/store.ts`
  - `src/lib/appearance.ts`
  - `src/components/auric/glow.tsx`
  - `src/components/auric/appearance-studio.tsx`
  - **new** `src/components/auric/reel-look-folder.tsx`
  - **new** `src/lib/contrast-guard.ts`
  - `src/styles/reel-look.css`
  - **new** `tests/l2-*.test.ts`, `tests/e2e/l2-*.e2e.mjs`
  - `screenshots/l2/`
  - `archive/2026-10-09-l2/`
- **FORBIDDEN:** `src/styles.css`, `drum-reel.tsx`, `span-reels.tsx`, `laser-strip.tsx`, `laser.ts`, `color.ts`, `modules.ts`, `folder-stage.tsx`, `folder-grid.tsx`, `glass-folder.tsx`, `session-shell.tsx`, `pavilion.tsx`, `audio.ts`, `wake.ts`, `package.json`, and the §2 global list.

### (c) GO

> **GO, Lane 2.** Branch `auric/l2-reel-look-settings` from the base **after L1 is merged**. Commit 2.0 (state contract + tests) first, push, and post "2.0 ready" in the PR. Then do 2A, 2B, and 2C as separate commits. Fill the acceptance table with evidence and stop. Anything you need from L1's files goes in the PR as a hook request.

---

## Lane 3: Folders

*Specs 7–13: size 0–100 + overflow guard, the ⋯ layout menu, matte etched text, icon styles, long-press size, snap-back, inline rename.*

**Branch:** `auric/l3-folders` · **Phase:** B (starts from L2 commit 2.0) · **Suggested model/effort (proposal only):** strongest coding model, effort **high** (gesture arbitration, physics, layout matrix).

**Visual target:** `References/Auric Folder Icons Target.png`. It shows:
- a frosted icy-glass folder list with a tabbed folder shape, wavy liquid-glass texture, and white glow edges
- a 3D hardware-style icon on the left of each row (message device, phone, chip board, purple glowing gear, green battery)
- a bright monospace UPPERCASE title, a status subline, and a chevron on the right
- a thin glowing scrollbar on the right

The top bar in that image (red BACK pill, NOTIFICATIONS title, minus / red-X circles, glass bell) is **context only**. It is not in this lane (see Proposals).

### (a) Brief

Restyle and extend the folder list rendered by `folder-grid.tsx` / `glass-folder.tsx` inside `session-shell.tsx`, toward the reference. Keep every current behavior: tap opens the folder via `enterModule`, the lock/unlock state and lockmark stay, `playClick` on open stays, and installed folders sort first.

- **7, Size 0–100 + overflow guard.**
  - Each folder renders at `folderSizes[id] ?? folderSizeAll`.
  - Put all geometry in a pure `src/lib/folder-layout.ts` `folderMetrics(size, containerWidth, layout)` that returns icon px, title px, row height, and columns. **Everything is clamped to the container:** `min-width: 0`, `max-width: 100%`, titles ellipsize (the full name stays in `aria-label`), icons shrink before text, and the chevron and lock never get clipped.
  - **Size 0** is still ≥ 44 px tall and readable. **Size 100** never exceeds the container width.
  - Use container queries on `.tx-folder-grid` (`container-type: inline-size`).
  - Note: the existing `SIZE_STEPS` in `folder-stage.tsx` belongs to digitScale. **Do not repurpose it.**
- **8, Layout menu.**
  - A **⋯** button (≥ 44 px) in the top-right corner of the folders panel (next to `~ $ ls folders` in `session-shell.tsx`) opens a sheet, `folder-layout-sheet.tsx`.
  - The sheet offers:
    - **Grid** (multiple folders per row; 2 columns at < 600 px, 3 at ≥ 600 px)
    - **Big** (one per row, large; matches today at size 50)
    - **Single** (one big folder fills the panel, with prev/next and horizontal swipe)
  - It also has the "All folders" size slider and the per-folder icon-style picker.
  - Layout changes animate with FLIP (transform + opacity, **320 ms**, interruptible, Bible "nothing is abrupt") and are instant under reduced motion.
- **9, Matte laser-etched text.**
  - Folder titles and sublines on the folder surface look **etched and matte**: a bright solid fill (ice white in dark mood; in light mood, `--tx-ink`-family ink that passes ≥ 4.5:1 on the folder surface), a 1 px inset "etch" shadow, and at most **one** halo layer (blur ≤ 4 px, alpha ≤ 0.35).
  - **No gloss gradients and no `background-clip:text` sheen.**
  - Titles are monospace uppercase per the reference.
- **10, Icon style per folder: `3d` / `flat` / `etched`.**
  - `flat` = today's `Glyph` SVG (default, unchanged).
  - `3d` = hardware-style icons per the reference: layered SVG with bevel, depth shadow, and an emissive accent. Hand-built SVG in `folder-icons-3d.tsx`, one per existing glyph kind. No raster AI art.
  - `etched` = no icon tile. The glyph is engraved into the folder surface with the same matte etched treatment as spec 9.
  - Chosen per folder in the layout sheet, and persisted in `folderIconStyle`.
- **11, Long-press → size.** Long-press on the folder **body** jumps straight to that folder's 0–100 size slider (an inline popover anchored to the folder). It fires `hapticTick()` once and does **not** open the folder.
- **12, Snap-back.**
  - Folders can be dragged to **reorder** (persisted in `folderOrder`; see Q9). If a dragged folder is released, or is moving, near any screen edge, it **springs back inside the safe area** using a pure spring in `src/lib/folder-physics.ts` (critically damped or slightly under-damped, settle < 500 ms).
  - The safe area = viewport minus `env(safe-area-inset-*)` minus 8 px. The folder's bbox is **clamped every frame** so it never leaves the safe area, even mid-spring. Overshoot is allowed only inward.
- **13, Inline rename.**
  - Long-press on the folder's **label text** opens inline rename: an `<input>` replaces the title, gets focus with the text selected, and raises the mobile keyboard.
  - **Enter** or **blur** saves. **Esc** cancels. Empty or whitespace-only names are refused (the input shakes and keeps focus, with the message "Name can't be empty"). Max 24 chars.
  - It never opens the folder. It stores `folderNames[id]`, and the `aria-label` uses the new name. A "Reset name" action lives in the layout sheet.

**Gesture hit zones (explicit; specs 11, 12, 13 must not conflict):**

| Zone | Exact bounds | Tap (< 500 ms, moved < 8 px) | Long-press (≥ 500 ms, moved < 8 px) | Drag (moved ≥ 8 px before 500 ms) |
|---|---|---|---|---|
| **Label** | The bbox of `.tx-folder-name` (title text only, not the subline), padded to a minimum 44 px tall and +8 px horizontally, clamped inside the tile | Opens folder (unchanged) | **Rename** (spec 13) | Reorder drag (spec 12) |
| **Body** | The whole tile **minus** the Label zone. Includes the icon, subline, lockmark, chevron, and empty glass | Opens folder (unchanged) | **Size slider** + `hapticTick()` (spec 11) | Reorder drag (spec 12) |
| **Inside an open rename input or size popover** | The control's own bbox | Normal control input | None | None (drag disabled while the control is open) |

Timing constants live in `folder-layout.ts`: `LONG_PRESS_MS = 500` and `MOVE_TOLERANCE_PX = 8`. A long-press **suppresses** the following click so the folder never opens. Keyboard parity: `F2` on a focused folder = rename, and `Shift+F10` or the context-menu key = size control. Under reduced motion, snap-back jumps instantly into the safe area.

### (b) DO / DON'T

**DO**
- Branch from L2's 2.0 commit and use only its store fields and setters.
- Write pure, tested modules for `folderMetrics`, `hitZone(point, tileRect, labelRect)`, `classifyGesture(events)`, `springStep(state, dt)`, and `clampToSafeArea(rect, viewport, insets)`.
- Build the liquid-glass texture as a **static** asset (an inline SVG pattern or layered gradients rendered once). Only the edge glow may "breathe", using opacity-only animation that stops under reduced motion and `data-glow="zero"`.
- Make the thin glowing scrollbar with `scrollbar-width: thin` and `::-webkit-scrollbar` in `folders.css`.
- Handle module ids generically (`string` keys) so L4's new `clicker-sounds` folder gets size, style, and rename for free.
- Test both moods. The reference is dark, and light mood must stay readable at ≥ 4.5:1.

**DON'T**
- Don't edit `store.ts`, `glow.tsx`, `modules.ts`, `folder-stage.tsx`, `styles.css`, `drum-reel.tsx`, or anything in L1/L2/L4's targets. Use hook requests.
- Don't remove the `flat` glyphs, the lockmark, or the chevron. Archive any retired SVG.
- Don't animate `filter`, `box-shadow`, or `backdrop-filter`. Don't run `feTurbulence` per frame.
- Don't use paid image generation or raster AI icons for the 3D style.
- Don't let a long-press or drag also fire `enterModule`.

### Acceptance criteria (testable)

| # | Criterion | Verified by |
|---|---|---|
| L3-1 | **Overflow matrix:** sizes **0, 50, 100** × layouts grid/big/single × widths **320**, 360, 390, 768, 1280 × both moods. `scrollWidth ≤ clientWidth`. Every tile bbox is inside its container. Title, chevron, and lockmark each have a visible bbox inside the tile and are not covered (`elementFromPoint` at their centers returns them or their descendants). Every button is ≥ 44 × 44. | `tests/e2e/l3-overflow.e2e.mjs` (90 cases) |
| L3-2 | `folderMetrics` unit table at sizes 0/50/100 and widths 320/390/1280: outputs are clamped, columns are right, and the icon shrinks before text. | unit |
| L3-3 | Default state (no new settings) renders like today: screenshot diff of the folder list at 390 px ≤ 1% pixels, with the old glyphs and order. | e2e |
| L3-4 | The ⋯ button opens the sheet. Switching grid→big→single animates with transform/opacity only (no layout-property animations, checked with `getAnimations()`). Duration is 320 ± 40 ms. It is instant under reduced motion. Perf is median ≤ 17.5 ms, p95 ≤ 25 ms. | e2e + perf |
| L3-5 | Etched text: computed `text-shadow` has ≤ 2 layers with halo blur ≤ 4 px. No `background-clip: text`. Title contrast is ≥ 4.5:1 vs. the folder surface sample in both moods. | e2e computed-style + unit |
| L3-6 | Icon style: each folder can be set to 3d, flat, or etched independently, and the choice persists across reload. `flat` is pixel-identical to baseline. `3d` and `etched` render for every `glyph` kind in `ModuleSpec`. | e2e + screenshots `screenshots/l3/icons-*.png` |
| L3-7 | **Long-press body → size:** after 500 ms on the icon, the chevron, and empty glass (3 tests), the size slider opens anchored to that folder, `navigator.vibrate` is called once with 7, and `enterModule` is not called. Moving the slider changes only that folder's size. | e2e with `vibrate` spy |
| L3-8 | **Long-press label → rename:** after 500 ms on the title text, the input appears, focused, with all text selected. `enterModule` is not called and the size slider does not open. | e2e |
| L3-9 | Rename: Enter saves, blur saves, Esc restores the old name, and `"   "` is refused with the message while focus stays. A 25-char name is refused or truncated at 24. The new name persists across reload and is used in `aria-label`. Reset restores `spec.pkg`. | e2e + unit |
| L3-10 | Hit-zone unit tests: points just inside and just outside the padded label rect classify correctly. Tap < 500 ms in either zone opens the folder. Moving ≥ 8 px before 500 ms classifies as a drag. | unit |
| L3-11 | **Snap-back, four edges:** drag a folder 40 px past the **top**, **bottom**, **left**, and **right** viewport edges (4 separate tests, at 320 px and 390 px widths). After release the folder is fully inside the safe area within 500 ms. On **every frame** sampled during the spring, its bbox stays inside the viewport. | `tests/e2e/l3-snapback.e2e.mjs` |
| L3-12 | `springStep` / `clampToSafeArea` unit tests: convergence < 500 ms simulated, no outward overshoot, and safe-area insets respected (mocked insets 0 and 44 px). | unit |
| L3-13 | Reorder persists (`folderOrder`) across reload. Unknown or new module ids append in default order. | unit + e2e |
| L3-14 | Keyboard parity: F2 = rename, Shift+F10 = size, Enter = open. All are announced to assistive tech with sensible labels. | e2e |
| L3-15 | Global gates pass. 8dp audit. Reduced-motion run of L3-4, L3-11. | PR checklist |

### Write targets

- **PERMITTED:**
  - `src/components/auric/folder-grid.tsx`
  - `src/components/auric/glass-folder.tsx`
  - `src/components/auric/session-shell.tsx` (⋯ button + sheet mount only)
  - **new** `src/components/auric/folder-layout-sheet.tsx`
  - **new** `src/components/auric/folder-icons-3d.tsx`
  - **new** `src/components/auric/folder-size-popover.tsx`
  - **new** `src/components/auric/folder-rename.tsx`
  - **new** `src/lib/folder-layout.ts`
  - **new** `src/lib/folder-physics.ts`
  - **new** `src/lib/folder-gestures.ts`
  - `src/styles/folders.css`
  - **new** `tests/l3-*.test.ts`, `tests/e2e/l3-*.e2e.mjs`
  - `screenshots/l3/`
  - `archive/2026-10-09-l3/`
- **FORBIDDEN:** `src/styles.css`, `store.ts`, `glow.tsx`, `appearance*.ts(x)`, `modules.ts`, `folder-stage.tsx`, `drum-reel.tsx`, `span-reels.tsx`, `laser*.ts(x)`, `pavilion.tsx`, `notice-bell.tsx`, `audio.ts`, `haptics.ts` (import only), `package.json`, and the §2 global list.

### (c) GO

> **GO, Lane 3.** Branch `auric/l3-folders` from L2's "2.0 ready" commit. Post a 10-line plan in the draft PR. Then build in this order, one commit each: pure modules + unit tests → size + overflow guard (spec 7) → ⋯ layout sheet + transitions (8) → etched text (9) → icon styles (10) → gestures: hit zones, long-press size (11), rename (13) → drag + snap-back (12). Run the full e2e matrix, fill the acceptance table with evidence, and stop. Rebase onto L2's final branch only if Royalty asks.

---

## Lane 4: Clicker Sounds

**Branch:** `auric/l4-clicker-sounds` · **Phase:** C (after L2 and L3 are merged) · **Suggested model/effort (proposal only):** mid-tier coding model, effort **medium** (mostly UI on existing data).

### (a) Brief

Give the wheel's 539 tick sounds their own folder, **"Clicker Sounds"**, in the folder list.

- **Register it:** add a `"clicker-sounds"` entry to `MODULES` in `modules.ts` (pkg `clicker-sounds`, glyph `"pulses"`, tier per Q10) and a `case "clicker-sounds"` in `folder-stage.tsx` `interior()`. Both are add-only edits.
- **The folder:**
  - Seven style tabs in `TICK_FAMILIES` order: **Click, Ice, Crystal, Silk, Gear, Bolt, Paper**.
  - Each tab lists its 77 cuts (`TICKS` filtered by family) as ≥ 44 px rows with the name, a "Crown cut" badge where `premium`, and a check on the current `reelSound`.
  - **Tap = preview + select:** `playReelTick(id)` plus `setReelSound(id)`. This matches the existing Pavilion "The Wheel" deck. Rapid taps retrigger cleanly.
  - The default and first row is `click-01`, marked "House".
  - Only the active tab's 77 rows render.
  - A one-line footer, "Ticks play on every wheel notch.", sets context.
- **Keep untouched:** the existing `timer-sounds` module, the Pavilion `TickRoom`, `ticks.ts`, `banks.ts`, and `audio.ts`.

### (b) DO / DON'T

**DO**
- Import `TICKS`, `TICK_FAMILIES`, `HOUSE_CLICK_ID`, and `playReelTick`. Do not copy the data.
- Keep tab state local. Keep the selected sound in the existing `reelSound` field (no store change).
- Make the tab strip scroll horizontally inside its own container at 320 px, without page overflow.
- Use the L3 folder vocabulary (etched titles, glass rows) via `.tx-*` classes plus `src/styles/clicker.css`.

**DON'T**
- Don't touch `store.ts`, `audio.ts`, `ticks.ts`, `banks.ts`, `pavilion.tsx`, `styles.css`, the L3 files, or the `timer-sounds` `SoundList`.
- Don't add new glyph kinds (that needs `glass-folder.tsx`, owned by L3). Use `"pulses"` and propose a new glyph in the PR.
- Don't autoplay anything on open. Sound only plays on an explicit tap.

### Acceptance criteria (testable)

| # | Criterion | Verified by |
|---|---|---|
| L4-1 | The folder "Clicker Sounds" appears in the folder list and opens via tap. Existing modules and their order are unchanged otherwise. | e2e |
| L4-2 | Seven tabs in exact order. Each shows exactly 77 rows. The total reachable is 539. Ids match `TICKS`. | unit + e2e |
| L4-3 | Tapping a row calls `playReelTick(id)` once and `setReelSound(id)`, and the check moves. The next reel notch on the timer uses that sound (spy on `playReelTick`). | e2e with spies |
| L4-4 | Default state: `click-01` is checked and labeled House. `tickById` legacy ids still resolve (existing tests pass). | unit |
| L4-5 | Opening the folder plays no sound. 10 rapid taps produce 10 previews with no errors. | e2e |
| L4-6 | At 320/390/768 px in both moods there is no horizontal page overflow, rows are ≥ 44 px, and the tab strip scrolls internally. | e2e |
| L4-7 | Size, icon style, and rename from L3 work on this folder. | e2e |
| L4-8 | `timer-sounds` and Pavilion "The Wheel" behave exactly as at baseline. | e2e |
| L4-9 | Global gates pass. 8dp audit. | PR checklist |

### Write targets

- **PERMITTED:**
  - `src/lib/modules.ts` (add one entry)
  - `src/components/auric/folder-stage.tsx` (add one case + import)
  - **new** `src/components/auric/clicker-sounds-folder.tsx`
  - `src/styles/clicker.css`
  - **new** `tests/l4-*.test.ts`, `tests/e2e/l4-*.e2e.mjs`
  - `screenshots/l4/`
  - `archive/2026-10-09-l4/`
- **FORBIDDEN:** everything else, including `store.ts`, `audio.ts`, `ticks.ts`, `banks.ts`, `pavilion.tsx`, `glass-folder.tsx`, `folder-grid.tsx`, `styles.css`, `package.json`, and the §2 global list.

### (c) GO

> **GO, Lane 4.** Branch `auric/l4-clicker-sounds` from the base after L2 and L3 are merged. Make the two add-only registrations, build the folder, run the gates, fill the acceptance table, and stop.

---

## Lane 5: QA Review

**Branch:** `auric/l5-qa` · **Runs after each lane PR** · **Suggested model/effort (proposal only):** strong reasoning model, effort **high**. Prefer a different model from the lane it reviews.

### (a) Brief

You are the independent reviewer. For the lane PR you are pointed at:
1. Check out its branch read-only.
2. Run `npm ci --ignore-scripts`, `typecheck`, `lint`, `test`, `build`, and the lane's e2e suite plus `scripts/browser-smoke.mjs` (dev and production preview).
3. Verify **every** acceptance criterion in that lane's table independently. Re-derive the evidence; don't trust the PR's claims.
4. Audit the write-target diff: every changed path must be on the lane's PERMITTED list. Any deletion outside an `archive/` move is a FAIL.
5. Audit the global rules: perf numbers, 8dp, reduced motion, both moods, the 320 px width, no generic-AI UI, no new dependencies, and baseline behavior kept.
6. Look at the reference image next to the lane's screenshots and grade "matches target" as PASS, NEEDS-WORK, or FAIL with specific notes.

Write `artifacts/qa/lanes/<lane>/REVIEW.md` and `review.json` (criterion → PASS/FAIL/NOT-VERIFIED + evidence path), with screenshots under `artifacts/qa/lanes/<lane>/shots/`. Then post one summary review comment on the lane PR with the verdict and links. **Comment only. Never approve, request merge, or merge.**

### (b) DO / DON'T

**DO:** Reproduce from a clean checkout. Record exact commands and outputs. Mark anything you cannot observe (real-device haptics, iOS keyboard) as NOT-VERIFIED, not PASS. Diff default-state screenshots against the baseline commit.

**DON'T:** Edit `src/`, tests, or any lane file. Don't push to lane branches, fix bugs yourself, open new PRs against lane branches, close PRs, or edit existing `artifacts/qa/*` files.

### Acceptance criteria (for QA itself)

| # | Criterion |
|---|---|
| Q-1 | Every criterion in the reviewed lane has a verdict and evidence. No blanks. |
| Q-2 | The write-target audit lists every changed path with OK or VIOLATION. |
| Q-3 | Perf, 8dp, reduced-motion, both-mood, 320 px, and deletion audits are all present. |
| Q-4 | The verdict is one of: **ACCEPT-READY**, **CHANGES-NEEDED** (numbered list), or **BLOCKED** (reason). |

### Write targets

- **PERMITTED:** `artifacts/qa/lanes/` (new files only) and one PR review comment per review round.
- **FORBIDDEN:** everything else.

### (c) GO

> **GO, Lane 5.** Given a lane PR URL, create `auric/l5-qa` if it doesn't exist, run the full review above, write the report files, post the single summary comment, and stop.

---

## Open questions for Royalty

1. **Repo:** the unpacked source is **not a git repo**. Which GitHub repo and base branch should lanes branch from? (See Proposal P1.)
2. **Laser scope:** should the laser strip replace the pill on the **timer span reels only** (HMS + D/W/M/Y, the current plan), or also on the settings and plaque wheels (Pavilion sound decks, alarm, remind, spectrum)?
3. **Number color per theme:** one color can't pass 4.5:1 on both the dark wheel (about `#0A1730`) and the light wheel (about `#EBE4D8`). Are separate **Dark** and **Light** number colors OK? Or should the wheel stay dark glass in light mode so one color works?
4. **"Never black" threshold:** is refusing anything with relative luminance < 0.012 (darker than about `#1C1C1C`) right? This still allows deep navy and similar dark inks on the light wheel.
5. **Exact default red** for the laser: `#FF1A1A`, or a specific hex you have in mind?
6. **Laser off:** keep a faint 1 px hairline on the selected row so it's still findable, or show nothing at all?
7. **Number color scope:** only the selected row + readout (off-center rows stay as faded depth cues), or every visible numeral?
8. **Folder layout default:** is today's single-column list the "Big" option at size 50? Or do you want a fourth "Classic" option that keeps today's look exactly?
9. **Drag purpose:** dragging a folder **reorders** the list in this draft. Is that what you want, or should folders be free-placed?
10. **Folder size:** per-folder sizes with an "All folders" default (this draft), or one global size only? And should the "Clicker Sounds" folder be **free** or a **Crown** (paid) module like "Timer Sounds"?
11. **Layout icon:** three dots (⋯) or three dashes (☰) for the corner menu?
12. **Haptics:** `reelFeel()` vibrates even when the Haptics setting is Off. Fix that in Lane 1, or leave it (the current plan keeps behavior)?

---

## Proposals (not approved)

*Ideas only. Nothing here is in any lane's scope until Royalty approves it.*

- **P1, Repo bootstrap:** before any lane starts, initialize a repo from the unpacked workspace with one baseline commit tagged `baseline-2026-10-09` (no code changes), so every lane diff and QA screenshot diff has a clean reference.
- **P2, Notifications header restyle:** use the reference's top bar (red glowing BACK pill, title, minus / red-X circles, glass bell) for `notice-bell.tsx` in a later lane.
- **P3, New "clicker" glyph:** a dedicated folder glyph for Clicker Sounds (requires an L3-owned `glass-folder.tsx` change after L4).
- **P4, Laser on other wheels:** if Q2 is "yes", a follow-up lane rolls `selector="none"` + `LaserStrip` out to the other `DrumReel` uses.
- **P5, Haptics respect:** make `reelFeel()` honor `hapticEnabled` (see Q12).
- **P6, Bible sync:** the Bible says the tick families include "Pulse", but the code ships "Click". After v1, update the Bible to match the code. A doc-only change, needing Royalty's sign-off on the Bible.
- **P7, Laser-tinted folder titles:** let the etched folder titles pick up a faint `--laser-color` halo so the whole app shares one accent.
- **P8, Timer presets and alarms:** resume these only after Lane 1 is accepted, as separate lanes.
