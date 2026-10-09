# Lane A: Panel UI

**Branch from:** `final-build`. **Branch name:** `lane-a-panel-ui`. **PR into:** `final-build`, titled `Lane A: Panel UI (Do not merge, Rex reviews)`.
**Read first:** `docs/10 Copilot Plan/Copilot Master Plan.md` sections 1 to 6 (what the app is, rules, build, ownership, contracts, root causes).
**Covers Royalty's phone-test items:** 1, 2, 3, 4, 5, 6, 7, 8, 9 and 11. The toggle redesign and Toggle Demo are **not** in this lane now (see the master plan's Later section). **Keep the existing toggles exactly as they are.**

## Files You Own

Everything in `04 Build/app/src/main/java/com/offgrid/compositor/terminal/`, plus `effects/FxKeys.kt`, `res/values/strings.xml`, `res/values/colors.xml`, `res/layout/activity_main.xml`, and the tests in `app/src/test/java/com/offgrid/compositor/terminal/`. New panel files go in `terminal/` (suggested: `TopBar.kt`, `BackOrbView.kt`, `QuickToggleView.kt`, `GlowScrollBar.kt`, `FullBleed.kt`, `NeonPalette.kt`). Split `TerminalPanel.kt`: it is 2,238 lines; move the top bar, sizing and resize code out into their own files as you go.

Do **not** edit `MainActivity.kt`, `CompositorProcessor.kt`, `EffectsController.kt`, the manifest, the CI workflow, or anything in `effects/` except `FxKeys.kt`. If you need a change there, list it under "Requests for other lanes" in your PR.

## Requirements and Acceptance Checks

### A1. Width 100 and Height 100 Fill the Screen, Truly Edge to Edge (Item 1)

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

### A2. Resize from the Bottom Edges and a Thick Bioluminescent Border (Item 2)

- Delete `ResizeGrip` (the little bottom-right corner piece) and all references, accessibility actions included (move "Bigger" and "Smaller" accessibility actions to the panel itself).
- Resize from the **bottom edge** (drag down or up changes height), the **bottom-left and bottom-right edge zones** (change width and height together), and the **left and right edges** of the lower half. Touch zones are 24 dp deep, inside the glass, and never inside the phone's gesture areas (`systemGestures` insets). The top bar stays the drag-to-move handle.
- While resizing, the edge being dragged brightens. Haptic tick on release (if Tick when saved is on).
- **Border look:** a thick neon outline like a glowing white rectangle on black (reference: the pink "AUTO" button and the glowing power icon in Royalty's references, but white by default): core stroke 3 dp (scales 2 to 6 dp with a new typed "Border thickness" 0 to 100 in 📁 Glow effects, start 50), white hot core, then a soft outer and inner bloom in the edge color. Default edge color in OLED Bioluminescent: white core with a UV-tinted bloom (see A9).

Acceptance: no corner grip anywhere (grep `ResizeGrip` returns nothing); dragging the bottom edge changes Height and the top box number updates live; dragging a bottom corner zone changes both; the Width and Height boxes, pinch and edge drag stay in step and are saved; border thickness 0, 50 and 100 render as 2, 3 and 6 dp in a test.

### A3. Glow at 100 Is Much More Powerful (Item 3)

Root cause: `glowStrength = min(1, 0.7 * glow / 0.5)` saturates at Glow 71, so 71 to 100 only adds the small `glowBoost` layer; the outer glow blur radius is fixed at `0.9 * 18 dp` because it must fit inside the 18 dp margin; masks are blurred at half size; and at full size the outer glow is entirely off-screen.

Fix:
- Make the glow curve continuous and strong: 0 is off, 50 is today's normal look, 100 is roughly 3 times the light energy of 50.
- **Layered bloom:** at least 3 layers (tight core 2 to 4 dp, mid 12 to 20 dp, wide 32 to 64 dp radius at 100), each blurred once per size into ALPHA_8 masks (keep the mask cache), additive blend (`BlendMode.PLUS`) so overlapping layers really get brighter.
- The glow margin grows with Glow strength (for example 18 dp at 50, 48 dp at 100) when the panel is not full-bleed, so the bloom is never clipped; in full-bleed the wide layers are drawn inward.
- Pulse at Glow 100 must still hold 60 fps on the S25 Ultra and at least 50 fps on the A37 (draw only cached masks per frame; no new blur per frame).

Acceptance: a unit test of the glow curve (0, 50, 71, 100 are strictly increasing; 100 is at least 2.5 times 50 in summed layer alpha); a rendered-bitmap test shows lit pixels at least 40 dp outside the glass at 100 (non full-bleed); a frame-time note in the PR from a profile run or `dumpsys gfxinfo` on an emulator.

### A4. Pixel Perfect Top Bar on a Strict Grid (Items 4 and 8)

Order from top-left to top-right, one row:

`[Back orb] [Title] ... [Camera] [Mic] [W box] [H box] [⋮] [−]`

- The 8 dp grid from the Quality Bar. Bar height 56 dp. Every control is a 40 dp tall visual inside a 48 dp touch target, vertically centered on the same center line. Number boxes 40 dp tall, 56 dp wide, text baseline aligned with the title baseline. Equal 8 dp gaps between all right-side controls. 16 dp outer padding left and right (plus safe insets in full-bleed). The − button's right edge is flush with the content edge, the Back orb's left edge is flush with the left content edge.
- The WIDTH and HEIGHT captions sit above their boxes in a 10 sp label, both captions on one baseline, or are removed in favor of "W" and "H" inside the boxes; pick one and apply to both.
- On narrow panels, hide in this order: captions, then W and H boxes (still in 📁 Size), then the title shrinks; Back, Camera, Mic, ⋮ and − never hide.
- Remove the drag grip dots (the whole bar background is the drag handle; taps on controls still work).

Acceptance: a layout test asserts every right-side control has the same top, the same height and equal gaps (within 1 px) at 3 widths (360 dp, 412 dp, full S25 Ultra width); a screenshot in the PR at Font size 0, 50, 100.

### A5. Terminal Text Colors Follow the System Theme (Item 5)

- Typed text and status text: **white `#FFFFFF` on the black system theme** (dark mode) and **black `#000000` Termux-style monospace on the white system theme** (light mode), JetBrains Mono. Remove `LASER_TEAL` as the default for TEXT and STATUS in OLED Bioluminescent (keep teal only as an optional color).
- In light mode the panel skin becomes white (`#FFFFFF`) with black text; glow colors stay neon.
- Follow `Configuration.uiMode` changes live without restarting.
- A user's custom color in 📁 Colors still wins over the default.
- Settings migration: installs that still have the old teal default (not a custom pick) move to the new default once.

Acceptance: unit tests for both modes; theme switch while the app is open updates the text without a restart; custom colors survive the migration.

### A6. Remove the Side Panel Entirely (Item 6)

- Delete the left side list, the `sideRule`, `SideDrawer.kt`, `SideMode`, the ⋮ menu's "Side list" choice, its saved preference (migrate it away), `SIDE_*` constants, side text color (`ColorPart.SIDE_TEXT`, migrate saved values away), and the related strings and tests.
- The main Termux-style folder page is the only navigation: Home grid, then folder detail with the path bar and the Back orb.

Acceptance: grep finds no `SideDrawer`, `sideList`, `SideMode`, `SIDE_TEXT`; Back from any folder depth returns one level at a time; the phone Back gesture does the same and exits only from Home.

### A7. Grid View with One Large Folder per Row and a Bioluminescent Scroll Bar (Item 7)

- The default view for Home and every folder: **one large folder per row** (a full-width tile, at least 72 dp tall, big glowing folder icon on the left, title in the system text color, item count on the right). Keep List, Grid 2 and Grid 3 in the ⋮ menu as options; the new default is "Big tiles".
- A **visible scroll bar** on the panel's right side (left side in RTL): a 3 dp track at 15% alpha and a 6 dp wide, rounded **bioluminescent thumb** (neon core plus soft bloom) whose length matches the visible fraction. It follows the finger while scrolling and can be dragged directly (24 dp touch zone). Smooth motion: the thumb moves with the scroll position every frame, fades in within 120 ms when scrolling starts and fades out 900 ms after it stops; with Remove animations on, it stays visible and static.
- Apply the same scroll bar to the folder detail scroll view.

Acceptance: a test that the thumb position equals the scroll fraction (within 1 px) at top, middle and bottom; a test that dragging the thumb scrolls the content; screen recording in the PR.

### A8. Back Button as a Glowing Orb at the Very Top Left (Item 8)

- Move Back out of the path bar into the top bar's first slot. A 40 dp circle with a left arrow inside, drawn in code (Canvas): black fill, a UV neon ring (2.5 dp) with a soft ambient bloom, like it sits under a black light. Pressed state fills the ring color at 35% alpha.
- On Home the orb is dimmed to 35% and disabled (still in place so the bar never shifts).
- Content description "Back", 48 dp touch target.

Acceptance: orb is the left-most control, aligned to the grid (A4); tapping it goes up one folder; on Home it is disabled; accessibility check passes.

### A9. UV Black Light Color Scheme (Item 9)

Royalty's references (`/workspace/handoff/step-final-shots/` on Rex's box; descriptions here so you do not need them): neon glow-tube palettes on pure black (purple, pink, yellow, green and red families with glowing caps), a pink "AUTO" neon outline button and a pink UV Index ring app, a glowing white power symbol on black, neon pill swatches `#00E5FC`, `#0032FF`, `#FF00A6`, `#7B02B3`, a "Cyber" dot grid (magenta, cyan, blue, lime), and dark glass dashboards with purple glow.

- Add a `NeonPalette` with these UV colors: UV Violet `#7B02B3`, Electric Purple `#B026FF`, Hot Magenta `#FF00A6`, Neon Pink `#FF2BD6`, Cyan Glow `#00E5FC`, Deep Blue `#0032FF`, Toxic Lime `#B6FF00`, Blacklight White `#F4ECFF`.
- **OLED Bioluminescent stays the start theme**, now with a white core border and Electric Purple plus Cyan bloom by default, white text (A5) and the white solid cursor with glow 0.
- Add one new theme **UV Black Light** (magenta and violet bloom, cyan accents) and add the palette as one-tap swatches in 📁 Colors (hex still typed, mixers still 0 to 100).
- Never more than 3 flashes per second in any glow animation.

Acceptance: start look unchanged in rules (black, white cursor, glow 0 cursor, white toggles); new theme applies and Reset all returns to OLED Bioluminescent.

### A10. Camera and Mic Quick Toggles in the Top Bar (UI Half of Item 10)

- Two coded icons right after the title, before the W box: a camera icon and a mic icon, 40 dp visuals in 48 dp targets.
- **Off:** red `#FF2D55` icon with a diagonal slash. **On:** green `#30FF7A` icon with a soft glow. Animate the slash in or out in 150 ms (none with Remove animations).
- They write the settings `fx.cam.on` (start on) and `fx.mic.on` (start off) through `ThemeStore.setToggle`, then fire `onSettingChanged` so Lane B reacts. The panel never touches the camera or mic itself.
- The status line types a plain line, for example "Mic is on. Your voice goes into the video."

Acceptance: tapping each flips color and slash and saves; state survives restart; a test asserts the keys are written exactly as `fx.cam.on` and `fx.mic.on`.

### A11. Folder Reorganization (Item 11, Plus the Microphone Effects Folder UI)

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

### A12. Panel Flaw Fixes from the Review

- Split `TerminalPanel.kt` into focused files (top bar, sizing and resize, glow and ripples, folder navigation). Target under 1,000 lines for `TerminalPanel.kt`.
- `EffectSettings` caches keys from `PanelTree.build()` once; keep it, but add a test that every `fx.` key in `FxKeys` appears in the tree (catches orphaned keys).
- Remove the stale "No mic" and "This app has no mic" notes from folders (Ghost, Time freeze, Soundboard, Record) and say what the mic does now, in plain words.

## Done Means

- Meets the Quality Bar (master plan section 0).
- `./gradlew assembleDebug testDebugUnitTest lintDebug` pass; slider, voice and secret greps are clean.
- PR description has screenshots or a short screen recording for A1, A2, A3, A4, A7, A8, A10, a checklist of A1 to A12 with evidence, and "Requests for other lanes" if any.
- Status log entry appended.
