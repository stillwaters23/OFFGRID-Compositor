# OFFGRID-Compositor: Step 2.1, Panel Polish and Theme Office

You are the Step 2.1 build agent (log yourself as **Agent 05, Claude Code**). Repo: https://github.com/stillwaters23/OFFGRID-Compositor (private, Android only, Kotlin).

## Where things stand
- Steps 0, 1 and 2 are merged into `main`. Step 2 was PR #4 (squash commit 3e10d39). Royalty phone-tested it and likes it.
- Rex (Agent 04) reviews your PR before anything merges. Royalty phone-tests after.
- Do not touch `06 Prompts and Skills/Rex Voice/`.

## Read first, before changing anything
1. `08 Chat History/Agent Status Log.md`
2. The Bootloader and Feature Scope in `06 Prompts and Skills`
3. The Step 2 code in `04 Build/app/src/main/java/com/offgrid/compositor/terminal/`
4. `03 Design/References` (toggle art)

## Royalty's phone-test feedback: fix and add these

### Bugs
1. **Ghost text.** Lines that haven't been typed yet show faintly behind the typewriter. Only typed letters may be visible, with nothing showing behind them. Keep the layout from jumping without drawing the hidden text. Keep the typewriter look, which Royalty likes.
2. **Resize.** The panel drags but can't be made smaller or bigger. Add a resize handle at the bottom-right corner, with sensible min and max sizes, and save the size like the position.

### Diamond
3. The yellow and pink diamond is only the Step 1 test shape. Add a **"Test diamond"** toggle in Panel settings, **off by default**. When it's off, nothing extra is painted. Keep the compositor files and the effect list working, because later effects use them.

### New default look (on first startup)
4. **No blue and no gold.** The panel is Termux-style **true black** (OLED black, #000000 glass).
5. Use **system colors only** for the default accents: follow the phone's Material You dynamic colors (Android 12 and newer), with a neutral fallback.
6. **OLED bioluminescent glow.** The edges and outer glow pulse softly, like living light.
7. **Laser cursor much brighter**, laser-jet bright, with a glow.
8. **Toggles.** Keep the same red OFF and green ON art and the same icon. Inside the circle, add a swirling, swishing **liquid bioluminescent glow** animation. Respect reduced-motion settings.

### Theme office (inside Panel settings)
Build folders and subfolders. They can go deeper when needed. Each folder's title says what it changes, for example "📁 Skin color" or "📁 Laser cursor color".

9. A **color folder for each part**:
   - Skin color (panel glass)
   - Edge color
   - Outer glow color
   - Typewriter text color
   - Laser cursor color
   - Toggle glow color
   - Folder icon color
   - Side list text color
   - Status line color
10. **Inside each color folder:**
    - A **Hex code** typed box, for example #00FF66. Check it and give a plain-word message if it's wrong.
    - **Red, Yellow, Green, Blue, Indigo, Violet**, each a typed number from 0 to 100. The six mix together into the color, and the hex box updates to match. Typing a hex updates the numbers as closely as possible.
    - A live color swatch.
    - A **"Use system color"** toggle (on by default) and a **"Reset"** button.
11. **📁 Glow effects folder:**
    - Edge glow on or off
    - Outer glow on or off
    - Pulse on or off
    - Pulse speed (0–100)
    - Glow strength (0–100)
    - Bioluminescent liquid swirl on or off
    - Swirl speed (0–100)
12. **📁 Toggle style folder:** liquid swirl on or off, glow color, and glow strength.
13. **📁 Themes folder:**
    - "OLED Bioluminescent" (the new default)
    - "Termux Black" (plain black, green text, no glow)
    - "System Colors"
    - "Holo Classic" (the old Step 2 cyan and gold look)

    Tapping one applies it. Every setting can still be changed afterward.
14. Add other sensible settings:
    - Panel corner roundness (0–100)
    - Font size (0–100)
    - Ring on or off
    - Hologram unfold on or off

    Also add a **"Reset all to default"** button with a plain-word confirm.

### Toggle explanation
15. In each platform's Tests folder, add one plain line: **"These are test markers for now. Real checks come in a later step."** This tells Royalty the toggles don't change anything inside live-stream apps yet.

## DO
- Use typed numbers 0–100 and toggles only.
- Save every setting on the phone and keep it after a restart.
- Use plain words everywhere.
- Keep the panel out of the saved video, as it is now.
- Keep 720x1280 at 30 fps working on the S25 Ultra and A37.
- New branch, PR, and CI passing (all guard checks).
- Re-publish `step-2-preview`.
- Plain-language phone test steps in the PR.
- Add a status-log entry recording that Step 2 passed the phone test and merged, plus your Step 2.1 entry.

## DO NOT
- **No sliders.**
- Do not merge.
- Do not start Step 3 (emoji rain) or later.
- No mic, voice, RECORD_AUDIO, text-to-speech, Rex voice, or features 38 and 39.
- No secrets, keys or tokens anywhere.
- Nothing that needs a computer to use.
- No Marvel, Iron Man, Avengers, Stark or JARVIS names or assets.
- No account, email or repo-settings changes.
- No anti-cheat, root-detection or app-integrity evasion.
- Do not delete or rewrite existing docs. Only add.
- Do not commit local-only files (`~/.gradle` init scripts, `.kotlin/` caches, screenshot test copies).

## Self-check before you report
- [ ] No ghost text behind the typewriter
- [ ] Panel resizes and remembers its size
- [ ] Test diamond is off by default and works when toggled on
- [ ] First startup looks black, with system accents, no blue, no gold, and a pulsing glow
- [ ] The cursor is very bright
- [ ] The toggle swirl animates and stops with reduced motion
- [ ] Every color folder has hex, the six 0–100 numbers, a swatch, Use system color, and Reset
- [ ] Bad input (150, "abc", a bad hex) gets a plain-word message and changes nothing
- [ ] The four themes apply
- [ ] Reset all works
- [ ] No sliders, mic, voice or secrets, and all CI guards pass
- [ ] `step-2-preview` re-published with the APK attached
- [ ] Status log updated

## Report back
Use plain words. Give a short list of what you built, the PR link, the build links, the install page, the phone test steps, the self-check table, and anything you were unsure about.
