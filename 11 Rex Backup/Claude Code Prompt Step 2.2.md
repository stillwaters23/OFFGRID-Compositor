# OFFGRID-Compositor: Step 2.1 → Step 2.2, Premium Panel Polish ($777)

You are **Agent 05, Claude Code**. Repo: https://github.com/stillwaters23/OFFGRID-Compositor (private, **Android only, Kotlin**).

## Where things stand
- Steps 0, 1, 2, and 2.1 are done. **PR #5 is open** (Step 2.1). Royalty phone-tested 2.1 and likes the direction. This job is the next polish on top of 2.1.
- If PR #5 is not merged yet: **base your branch on the PR #5 head** (`ccr-c441a76e-hgt5wa` / latest Step 2.1 commit), not old main. Open a new PR for Step 2.2. Do not merge anything.
- Rex (Agent 04) reviews. Royalty phone-tests after.
- Do not touch `06 Prompts and Skills/Rex Voice/`.

## Read first, before changing anything
1. `08 Chat History/Agent Status Log.md`
2. Bootloader + Feature Scope in `06 Prompts and Skills`
3. All of `04 Build/app/src/main/java/com/offgrid/compositor/terminal/`
4. Design refs in `03 Design/References` (toggle art + any new vibe images Royalty added)
5. This whole prompt. Follow it exactly. No drift.

## Royalty's phone feedback (must fix)

### 1. Toggle swirl: real liquid or remove it
- The current ON/OFF toggle draws a **fake overlap** swirl on top of the art. Royalty does not want that.
- **Try once** to bake a real bioluminescent liquid glow **inside** the circular orb of the existing red OFF / green ON art (same icons, same size). Soft living light that swirls and swishes inside the glass, brighter than now.
- If you cannot make it look truly **inside** the art (no fake layer sitting on top of the pill), **remove the swirl effect completely** and leave the original toggle pictures as they are.
- Keep red OFF / green ON. Respect "Remove animations". No roundness baked onto the art as a fake animated frame.

### 2. Less roundness
- Royalty does not like the extra roundness Claude added to animated effects and folder icons.
- Default **Corner roundness starts lower** (near square, about 5 or less is fine if it already is; do not bump it up).
- Folder icons and chrome should feel **sharp / premium**, not soft-blob rounded.
- Roundness stays a typed 0–100 setting, but the default look is flatter and harder.

### 3. No loose settings. Everything lives in a folder
- Inside **Panel settings**, every control must sit in its own folder. **No loose rows** at the root of Panel settings.
- Put existing loose items into folders, for example:
  - 📁 Size (Opacity, Font size, Corner roundness, Typing speed, Width %, Height %, and any size-related toggles)
  - 📁 Display (Test diamond, Scan line, Laser typing, Ring, Hologram unfold)
  - 📁 Themes (already exists)
  - 📁 Colors (already exists)
  - 📁 Glow effects (already exists)
  - 📁 Toggle style (already exists)
  - 📁 Reset (Reset all to default + confirm)
- Folders can nest deeper when needed. Titles stay plain words with 📁.

### 4. Size control: typed Width % and Height %, pinch, and center
- Near the **minimize (−) button at the top**, add two typed boxes: **Width** 0–100 and **Height** 0–100.
  - 0 = smallest usable size (still readable, still a small rectangle; never invisible).
  - 100 = as large as allowed (not necessarily full screen if that hides the camera; keep a sensible max).
- Panel **starts perfectly centered** on first open.
- Drag still works. Resize corner still works. **Pinch with two fingers** also resizes:
  - Pinch up/down → height
  - Pinch sideways → width
  - Diagonal → both
  - While pinching, the Width % and Height % numbers update live.
- Always keep the typed numbers and the pinch/corner resize in sync and saved.
- Optional but wanted: a **"Center panel"** control in 📁 Size that snaps it back to the middle.

### 5. Touch edge glow
- When a finger touches an edge or chrome of the panel, that spot lights up with a short **bioluminescent glow** ripple, then fades. Soft living light, not a cartoon flash.
- Stops when Remove animations is on.

### 6. Premium "$777" look (from Royalty's Gemini vibe reference)
Royalty wants the panel to feel like **dark brushed titanium + woven carbon fiber chrome**, with **thick inset sapphire glass** panes, and **Laser Jet Bright** titles.

Translate that into **Android Canvas / OpenGL / Kotlin** drawing. **Do not build a web page. Do not use Three.js, HTML, CSS, WebGL-in-a-WebView, or marquee tags.** Match the *feel* on native Android.

Required visual upgrades:
- Outer frame: dark metal / carbon-fiber look (procedural noise + gradients is fine; no Marvel assets).
- Inner panes: deep sapphire-glass feel (blurred dark teal glass with real inset depth / inner shadow).
- Titles and typewriter text: **Laser Jet Bright** cyan/teal core fill with a harsh multi-layer bioluminescent glow (much brighter than 2.1).
- Keep true-black OLED option and system-color option. OLED Bioluminescent remains a start theme unless Royalty set another.
- No Marvel / Iron Man / Avengers / Stark / JARVIS names or assets. Original design only.

**Out of scope for this step (do not build yet):**
- 3D platform mascots (Yeti, etc.)
- 3D gift icons / dotted waveform graphs / reel-to-reel hardware
Those belong to later platform-hub steps. For Step 2.2, upgrade the **panel chrome, glass, glow, folders, toggles, and size controls** only.

### 7. Folder icons
Royalty will provide replacement folder icon art. If new art files are already in `03 Design/References` or attached in this chat/repo for folders, use them. If not, improve the current wireframe folders toward the premium look (sharper, less round, glowing) and leave a clear TODO in the status log for the real art drop.

## Extra premium polish (add these; they fit "$777 each")
1. **Focus glow** on the setting currently being typed (edge of that row lights softly).
2. **Haptic tick** (short vibrate) when a typed value is accepted, only if the phone allows it; never ask for new dangerous permissions. CAMERA stays the only app permission.
3. **Snap guides** while dragging: soft center crosshair that fades, to help re-center.
4. **Reduced motion path** still solid: every swirl, pulse, and edge touch glow freezes when Remove animations is on.
5. Keep **plain words** everywhere. Bad input (150, abc, bad hex) gets a plain message and changes nothing.

## DO
- Base on Step 2.1 code. Keep compositor + Test diamond toggle working (off by default).
- Typed numbers 0–100 and toggles only. **No sliders.**
- Save every setting across restart.
- Panel stays out of the saved video.
- 720×1280 @ 30 fps, S25 Ultra + A37.
- New branch, new PR, CI green (all guards: secrets, mic, voice, sliders).
- Re-publish `step-2-preview`.
- Plain-language phone test steps in the PR.
- Status log entry: Step 2.1 phone feedback received + Step 2.2 stop point. Log as Agent 05.

## DO NOT (hard lock — no drift)
- Do not merge.
- Do not start Step 3 (emoji rain) or later.
- No mic, RECORD_AUDIO, TTS, Rex voice, features 38–39.
- No secrets, keys, tokens.
- Nothing that needs a computer to use.
- No Marvel / Iron Man / Avengers / Stark / JARVIS.
- No account, email, or other-repo settings changes.
- No anti-cheat / root / integrity evasion.
- Do not delete or rewrite existing docs. Only add.
- **No web stack** (no HTML/CSS/Three.js/WebView UI for the panel).
- **No fake toggle overlay** left behind if the real in-art swirl fails — remove it.
- **No loose settings** outside folders in Panel settings.
- Do not commit local-only caches (`.kotlin/`, `~/.gradle` init scripts, scratch screenshot trees).

## Self-check (you must run this before reporting)
- [ ] Toggle: either real in-orb liquid glow, or swirl fully removed. No fake overlap.
- [ ] Default look is less round / sharper chrome.
- [ ] Panel settings has **zero** loose controls; everything is in a folder.
- [ ] Width % and Height % typed boxes exist near the top − button; 0–100; saved; synced with pinch and corner resize.
- [ ] First open is centered. Pinch works. Edge touch glow works (and stops with Remove animations).
- [ ] Laser Jet Bright titles / typewriter glow are clearly brighter.
- [ ] Titanium / carbon / sapphire-glass feel is visible on the panel chrome (native drawing).
- [ ] Test diamond still off by default; compositor still works when on.
- [ ] No sliders, mic, voice, secrets; CI guards all pass.
- [ ] `step-2-preview` re-published with APK.
- [ ] Status log updated (additions only).
- [ ] Off-device screenshots or checks prove: no ghost text, no fake toggle overlay, folders-only settings tree.

If any checkbox fails, fix it before you open or update the PR.

## Report format
Plain words. Short list of what you built, what you removed, PR link, build links, install page, phone test steps, self-check table, and anything you were unsure about.
