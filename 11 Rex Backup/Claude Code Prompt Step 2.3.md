# OFFGRID-Compositor: Step 2.3, Solid Laser Jet Cursor

You are **Agent 05, Claude Code**. Repo: https://github.com/stillwaters23/OFFGRID-Compositor (private, Android only, Kotlin).

## When to run this
**Only after Step 2.2 is merged** (or after Rex says go). Do not start this while 2.2 is still building. Base your branch on the latest `main` that includes 2.2.

## Where things stand
- Steps 0 → 2.2 are done. This is a small polish pass on the typewriter cursor only, plus its color controls.
- Rex (Agent 04) reviews. Royalty phone-tests after.
- Do not touch `06 Prompts and Skills/Rex Voice/`.

## Read first
1. `08 Chat History/Agent Status Log.md`
2. The Step 2.2 terminal code, especially `LaserTypeView.kt` and cursor / theme color code
3. This whole prompt. No drift.

## The bug Royalty found
The cursor looks soft and multi-shade (a glowing blur of whites). That is wrong for the **system preset**.

Royalty wants:
- A **solid Laser Jet Bright** cursor by default.
- **One solid color** (default: pure white `#FFFFFF`).
- Soft bioluminescent **gradients / multi-shade glow around the cursor are optional**, not the preset.
- Cursor color is fully changeable: same pattern as other colors (hex + Red / Yellow / Green / Blue / Indigo / Violet typed 0–100), under **📁 Colors → 📁 Laser cursor color** (already exists; make sure the **preset** uses solid fill).

## DO
1. **Default cursor draw:**
   - Opaque solid block (█), one flat color, no soft multi-tone fill inside the block.
   - Very bright. Pure white by default.
   - Optional thin outer glow may exist only if a setting turns it on. Default = **glow off** (or glow strength 0).
2. In **📁 Laser cursor color** (or **📁 Cursor style** if you split it):
   - Hex + six 0–100 mixers (existing pattern).
   - **Cursor glow** toggle or Glow strength 0–100, **default 0**.
   - Plain words. Bad input refused.
3. Keep reduced-motion behavior: blink may stay; soft glow stays off when Remove animations is on.
4. New branch, PR, CI green (secrets / mic / voice / sliders guards).
5. Re-publish `step-2-preview`.
6. Plain phone test steps in the PR.
7. Status log entry (Agent 05, Step 2.3).

## DO NOT
- Do not merge.
- Do not start Step 3 or later.
- No mic, voice, RECORD_AUDIO, TTS, features 38–39.
- No secrets. No sliders. No Marvel / Iron Man / Avengers / Stark / JARVIS.
- No account or email changes. No anti-cheat evasion.
- Do not delete or rewrite docs. Only add.
- Do not undo Step 2.2 work (folders, size %, pinch, toggle fix, premium chrome).
- Do not make the soft multi-shade glow the default again.

## Phone test (short)
1. Uninstall old OFFGRID. Install from `step-2-preview`.
2. Watch the cursor while text types: solid bright block, one color, not a soft white smear.
3. Open 📁 Colors → 📁 Laser cursor color. Set hex `#00FF66`. Cursor turns solid green.
4. Turn cursor glow on (or raise glow strength). Soft outer glow appears. Turn it back to 0 / off. Solid block only.
5. Reset all / OLED Bioluminescent: cursor returns to solid white, glow off.

## Self-check
- [ ] Default cursor is solid one-color Laser Jet Bright (white)
- [ ] No multi-shade fill inside the block by default
- [ ] Outer glow is optional and defaults off / 0
- [ ] Color still changeable with hex + six 0–100
- [ ] 2.2 features still work
- [ ] No sliders / mic / voice / secrets; CI passes
- [ ] Status log updated

## Report
Plain words. What you changed, PR link, install page, self-check table, anything unsure.
