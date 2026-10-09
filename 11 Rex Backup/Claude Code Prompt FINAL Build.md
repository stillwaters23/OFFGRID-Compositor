# OFFGRID-Compositor: FINAL Full Build (one continuous run)

**Note for Royalty:** Paste this whole file into a fresh Claude Code chat. Attach your phone screenshots from Oct 6 and 7 (the `Screenshot_2026100x_OFFGRID` ones show the size and toggle problems) plus your toggle reference pictures.

---

## Who you are working for
You are building for Royalty (GitHub `stillwaters23`). Repo: `stillwaters23/OFFGRID-Compositor`, a private native Android app written in Kotlin with CameraX, an OpenGL compositor, and a HUD terminal panel with folders drawn on Canvas. Royalty records and streams without showing their face. Effects get baked into the recorded video. The panel never does.

This is the **FINAL full build in one go**. There are no phone-test stops between milestones. Keep working until everything below is done. If you hit a usage limit, write exactly where you stopped in the `08 Chat History` status log, then pick up from that spot when the limit resets. Rex (Agent 04) reviews before anything merges to main. Royalty does the tweaks afterward. Move into the house first, sort later.

## Branch plan
1. PRs #5 (Step 2.1), #6 (Step 2.2 + 2.3) and #7 (Step 2.4) are open and stacked: #7 on #6, #6 on #5.
2. Do NOT merge them yourself. Make one new branch, `final-build`, starting from #7's branch head, so it includes all of 2.1 through 2.4.
3. Push often. Open **one PR** from `final-build`, titled `FINAL build: everything except voice (Do not merge, Rex reviews)`.
4. In the PR body, note that the merge order is #5, then #6, then #7, then this one, using merge commits (not squash) to avoid conflicts.
5. Keep publishing the debug APK to the `step-2-preview` release from `final-build` only, so an older branch can't overwrite it.

## Must keep (standing rules)
- [ ] The only permission is CAMERA. `RECORD_AUDIO` stays removed with `tools:node="remove"`. No mic, no voice, no TTS, no speech code.
- [ ] Step 7 (Rex voice) stays **paused**. Skip it and leave only a doc stub. Don't touch the `Rex Voice` or `OFFGRID_App_Icons` folders.
- [ ] **No sliders, ever.** Settings are typed 0 to 100 boxes plus on/off toggles. Colors are hex plus R O Y G B I V + White + Black mixers, 0 to 100 each.
- [ ] True black Termux-style OLED look in system colors. OLED Bioluminescent is the start theme. No blue panels and no gold edges at startup. Keep the Glow master toggle, Pulse as its own toggle, and Glow strength above 50 for extra brightness.
- [ ] The cursor is one solid laser-bright color, white by default and changeable. Cursor glow defaults to 0. Gradients are optional and off.
- [ ] Native Kotlin/Canvas only. No WebView, no Three.js. No Marvel, Iron Man, Stark or JARVIS names or assets. The HUD style is original.
- [ ] The panel never appears in recorded video. Test diamond is off by default.
- [ ] Follow streaming platform rules: no face shown, nothing that risks a ban.
- [ ] No secrets in code. CI guards (secrets, mic, voice, sliders) must all pass.
- [ ] After every milestone, append where you stopped to the `08 Chat History` status log.

---

## Milestones (do them in order, check each one yourself)

### M1. New coded toggles (replace the red/green pictures)
- [ ] Delete the image-based red/green toggle art and every reference to it.
- [ ] Draw toggles in code (Canvas or vector): a **circle ⭕ with the standard power on/off symbol inside**.
- [ ] Two styles: **Pure Black** (#000000) and **Pure White** (#FFFFFF). On is lit or filled, Off is dim or outlined, so the state is obvious at a glance.
- [ ] A setting in Panel settings > Themes (or Toggles) picks Black or White. Default is Pure White on the OLED black theme.
- [ ] Optional toggle glow, **off by default**, that follows the current glow color.
- [ ] Sharp at every size and density, with a smooth short on/off animation. Minimum 48dp touch target.
- **Check:** no PNG/JPG toggle assets remain, every toggle in the app uses the new view, and the choice survives an app restart.

### M2. True full size at 100
- [ ] Width 100 and Height 100, and the Full screen option, must fill the screen **edge to edge**. Royalty tested and it is still not 100% at its biggest size (see the screenshots).
- [ ] Handle the status bar, nav or gesture bar, camera cutout and rounded corners correctly. Use the real window insets and draw behind the bars when full.
- [ ] Keep the resize corner and the − / ⋮ buttons out of the rounded screen corners and the gesture area.
- [ ] The panel doesn't jump when the keyboard opens or when the status bar is pulled down.
- **Check:** at 100/100, measure the panel bounds against the display bounds in a unit or instrumented test, and log them.

### M3. Fixes from Rex's PR #7 review
- [ ] **Side list drawer:** the 16dp handle (`SideDrawer.kt`, around lines 69 to 71) swallows taps on the left edge of side rows while the drawer is open. Make it unclickable while open, or put it below the sheet.
- [ ] **Camera-denied help:** at full size the Record bar fades and can hide the "Try again" / "Open settings" button (`TerminalPanel.kt`, around 1052 to 1085). Don't fade the bar while it shows an action, or start minimized when the camera isn't live.
- [ ] **Move folders:** when it's turned on from A to Z, save the order for **every** folder, not just home and the open one (`TerminalPanel.kt`, around 1339 to 1343). Add an Undo, and confirm before overwriting a saved custom order.
- [ ] **FitText loop:** `onMeasure` calls `setTextSize` (`FitText.kt`, around 45 to 57). Guard against an endless re-layout (only change the size when it actually differs, and don't re-request layout from inside measure).
- [ ] Never break a word in the middle. If a word still doesn't fit at the smallest size, wrap the whole word onto its own line.
- [ ] Let the list scroll while Move folders is on (drag only from a handle or after a long press).
- [ ] Don't show "New folder order saved" when a drag didn't change anything.
- [ ] Add a settings migration so the new start look applies when installing over an older build. Reset all still works.

### M4. Finish the rest of the roadmap (everything except voice)
Read the repo's plan, README, `09 Grok Handoff/Grok Handoff.md` and the status log, and build every remaining step **except Step 7 voice**. At minimum:
- [ ] **No-face camera modes:** cloak and ghost modes, plus the face diamond, all baked into the recording and never showing the real face.
- [ ] **Stream effects:** emoji rain and the other planned effects. Each one is a folder with on/off toggles and typed 0 to 100 settings.
- [ ] **Platform folders:** the 18 streaming platform folders, each holding safe defaults and notes that follow that platform's rules.
- [ ] **Record, play and export:** record, play the last video, save to the gallery, with the panel never in the video.
- [ ] **Presets:** save, load, rename and delete looks (theme, colors, glow, cursor, toggle style, size).
- [ ] Any other remaining step in the repo roadmap. If something is unclear, decide sensibly and write it down under Open questions in the PR.

### M5. Polish and final check
- [ ] Smooth on a Samsung S25 Ultra and a Galaxy A37. Glow 100 with Pulse on shouldn't lag. Watch for overdraw and battery drain.
- [ ] Every setting is in a folder, every folder has a Reset, and Reset all resets everything.
- [ ] Rotation, backgrounding and full screen keep their state.
- [ ] Update the README and status log with a feature list and how to use it.

---

## Do
- Read the whole repo first, then plan, then build in milestone order.
- Commit small and often with clear messages. Push after each milestone.
- Write unit tests for color math, settings save/load, migration, presets and folder ordering.
- Keep each file focused. Split `TerminalPanel.kt` if it keeps growing.
- Log your progress in the `08 Chat History` status log after each milestone.

## Don't
- Don't merge anything, and don't touch main directly.
- Don't add sliders, mic, voice, TTS, WebView, Three.js, or Marvel names or assets.
- Don't start Step 7 voice.
- Don't touch `Rex Voice` or `OFFGRID_App_Icons`.
- Don't put secrets or keys in code.
- Don't stop to wait for phone tests. Keep going until everything is done.

## Self-check before every push
- [ ] `./gradlew assembleDebug` and the unit tests pass.
- [ ] The merged manifest has CAMERA only, with no RECORD_AUDIO.
- [ ] grep finds no SeekBar or Slider, no mic, speech or TTS code, no secrets, and no banned names.
- [ ] The panel isn't in a recorded test video (check the compositor path).
- [ ] The status log is updated.

## Final report (post it in the PR body and the status log)
1. What got built, milestone by milestone, with a checkmark for each.
2. Anything skipped or partly done, and why.
3. Open questions and the decisions you made.
4. A phone-test checklist for Royalty: uninstall first, then check the toggles, size 100, the drawer, camera denied, Move folders, no-face modes, effects, record and play, and presets.
5. The install link: the `step-2-preview` release.

**Do not merge. Rex (Agent 04) reviews, then Royalty tests and gives the go.**
