# Agent Status Log

**Project:** OFFGRID-Compositor  
**Owner:** Royalty  
**Updated:** 2026-10-06 (America/Chicago)

This is a running log. When any agent finishes or stops, it adds an entry here so Royalty and Claude can see where everyone left off.

Each entry has the date and time in America/Chicago, the agent number, the Bootloader step, what was done, the pull request or commit, the state, and the exact next thing to pick up.

States: passed, waiting for review, blocked, or waiting for Royalty's phone test.

---

## 2026-10-06 3:08 AM CT — Agent 01

- **Bootloader step:** Step 0. Project setup
- **What was done:** Built the Android app in `04 Build`. Kotlin, CameraX, and OpenGL ES. It shows a live front-camera preview. Agent 04 returned PASS. Royalty's phone test passed. The camera works.
- **PR / commit:** Pull request #2, merged. Commit `c639731`.
- **State:** passed
- **Next:** Nothing left on Step 0. The next open step was Step 1 research.

## 2026-10-06 2:54 AM CT — Agent 02

- **Bootloader step:** Step 1. Outgoing video compositor, research part
- **What was done:** Wrote `02 Research/Outgoing Video Compositor Research.md`. Agent 04 returned PASS. The paper was merged. No code was built in that step.
- **PR / commit:** Pull request #1, merged. Commit `3ca5e3c`.
- **State:** passed
- **Next:** The build part of Step 1, after Royalty says go. Agent 03 picked that up.

## 2026-10-06 3:12 AM CT — Agent 04 (Rex)

- **Bootloader step:** Not a build step. Rex voice training files.
- **What was done:** Rex voice training audio is being uploaded to `06 Prompts and Skills/Rex Voice/` (Rex 01.wav, Rex 02.wav, Rex 03.wav, plus Rex.md, the tagged script).
- **PR / commit:** On main. Latest voice commit `38d2c01`.
- **State:** The voice step is still paused.
- **Next:** Do not start Step 7. Wait until Royalty says go.

## 2026-10-06 3:29 AM CT — Agent 03

- **Bootloader step:** Step 1. Outgoing video compositor, build part
- **What was done:** Extended the Step 0 app in `04 Build`. One CameraX effect paints the camera, then an empty effect list, then a bright yellow diamond. The live view shows that painted picture. A Record 5 seconds button saves a short video on the phone with the mic off. Play last video plays it back. No stream library, no stream key, and no upload. Feature Scope now goes through feature 39 (read-aloud button, and speak as Rex). Both wait for the paused voice step. The Bootloader now says to write this log when an agent finishes or stops.
- **PR / commit:** Pull request #3, not merged. Commit `7e6604b`.
- **State:** Waiting for Royalty's phone test
- **Next:** Rex (Agent 04) reviews pull request #3 and does not merge it. Royalty installs the step-1-preview build, taps Record 5 seconds, taps Play last video, and checks that the yellow diamond is in the video. Do not start Step 2 or Step 7.

## 2026-10-06 4:32 AM CT — Claude Code (Step 2 build lane, lane number not given)

- **Bootloader step:** Step 1. Outgoing video compositor, build part
- **What was done:** Recorded that Royalty's phone test of the step-1-preview build passed. The yellow diamond showed in the saved 5 second video. Agent 04 returned PASS.
- **PR / commit:** Pull request #3.
- **State:** passed
- **Next:** Nothing left to test on Step 1.

## 2026-10-06 4:32 AM CT — Claude Code (Step 2 build lane, lane number not given)

- **Bootloader step:** Step 1. Outgoing video compositor, build part
- **What was done:** Recorded that pull request #3 was merged into main at 3:40 AM CT, as commit `be02dd7`. Step 1 is done.
- **PR / commit:** Pull request #3, merged. Commit `be02dd7`.
- **State:** passed
- **Next:** Step 2 build, Terminal panel and controls. Claude Code picked that up.

## 2026-10-06 4:37 AM CT — Claude Code (Step 2 build lane, lane number not given)

- **Bootloader step:** Step 2. Terminal panel and controls (Build)
- **What was done:** Built the floating terminal panel on top of the Step 1 app in `04 Build`. It is dark glass with cyan and gold glow lines. Drag the top bar to move it, or shrink it to a small tag. A side list and a grid of glowing folders show how many items are inside: Panel settings, OFFGRID to OFFGRID, and the 18 platform folders. Each platform folder holds an empty Effects folder and a Tests folder with 4 private test toggles. Each platform folder shows the guidance lines and the RESULTS line, filled from 02 Research or marked UNCONFIRMED. Text is laser typed in plain words. Opacity, Glow, and Typing speed are typed numbers from 0 to 100. Every toggle uses the red and green art from 03 Design/References. No sliders. The compositor and the yellow diamond test are unchanged. No mic and no voice. GitHub built the app and published it as step-2-preview.
- **PR / commit:** Pull request #4, not merged. Commit `259933a`.
- **State:** waiting for review
- **Next:** Rex (Agent 04) reviews pull request #4 and does not merge it. Then Royalty installs step-2-preview on the Galaxy S25 Ultra and the Galaxy A37 and runs the test steps in pull request #4, including the yellow diamond video check. Do not start Step 3 or Step 7.

## 2026-10-06 6:49 AM CT — Agent 05, Claude Code

- **Bootloader step:** Step 2. Terminal panel and controls (Build)
- **What was done:** Recorded that Royalty phone-tested the step-2-preview build and likes it. Pull request #4 was merged into main at 5:29 AM CT, as squash commit `3e10d39`. Step 2 is done. Royalty's phone-test feedback became Step 2.1.
- **PR / commit:** Pull request #4, merged. Commit `3e10d39`.
- **State:** passed
- **Next:** Step 2.1, Panel polish and theme office. Agent 05 picked that up.

## 2026-10-06 6:49 AM CT — Agent 05, Claude Code

- **Bootloader step:** Step 2.1. Panel polish and theme office (Royalty's Step 2 phone-test feedback)
- **What was done:** Fixed the ghost text. Only typed letters show now, and the words still never jump. Added a resize corner. The panel keeps its size and place after a restart. The yellow Step 1 diamond is now a Test diamond toggle in Panel settings. It starts off, and off paints nothing extra. The compositor files and the effect list are kept. The new first look is true black glass in the phone's own colors, with no blue and no gold. The edge and outer glow breathe softly. The laser cursor is much brighter. Each toggle keeps the red and green art and has a swirl of light inside its ball. All motion stops when the phone's Remove animations setting is on. Built the theme office in Panel settings: Themes (OLED Bioluminescent, Termux Black, System Colors, Holo Classic), Colors (9 color folders, each with a hex code box, Red, Yellow, Green, Blue, Indigo, and Violet from 0 to 100, a swatch, Use system color, and Reset), Glow effects, and Toggle style. Added Corner roundness, Font size, Ring, Hologram unfold, and Reset all to default, which asks first. Bad input gets a plain message and changes nothing. Each Tests folder now says the test markers don't check anything yet. Every setting is saved on the phone. No sliders, no mic, no voice. A new unit test checks the color mix math. GitHub built the app and published it again as step-2-preview.
- **PR / commit:** Pull request #5, not merged. Commits `e892d2f` and `d119dff`.
- **State:** waiting for review
- **Next:** Rex (Agent 04) reviews pull request #5 and does not merge it. Then Royalty installs step-2-preview on the Galaxy S25 Ultra and the Galaxy A37 and runs the test steps in pull request #5. Do not start Step 3 or Step 7.

## 2026-10-06 4:09 PM CT — Agent 05, Claude Code

- **Bootloader step:** Step 2.1. Panel polish and theme office
- **What was done:** Recorded that Royalty phone-tested the Step 2.1 build and likes the direction. Her phone feedback became Step 2.2, Premium Panel Polish. Pull request #5 is still open and not merged, so Step 2.2 is built on its head.
- **PR / commit:** Pull request #5, not merged. Commit `1e84330`.
- **State:** waiting for review
- **Next:** Step 2.2. Agent 05 picked that up.

## 2026-10-06 4:09 PM CT — Agent 05, Claude Code

- **Bootloader step:** Step 2.2. Premium Panel Polish (Royalty's Step 2.1 phone-test feedback)
- **What was done:** Tried once to bake a liquid glow inside the toggle balls. An independent check found it did not look truly inside the art, so the swirl was removed completely, with its settings (📁 Toggle style and 📁 Toggle glow color). The toggles are the original red OFF and green ON pictures again. Corner roundness now starts at 4, and the folder icons and chrome are sharp. Panel settings now holds only folders: 📁 Size, 📁 Display, 📁 Themes, 📁 Colors, 📁 Glow effects, and 📁 Reset. Nothing is loose. Width and Height are typed boxes from 0 to 100 next to the − button and in 📁 Size. They stay in step with the new two-finger pinch and the resize corner, and they are saved. The panel opens in the exact middle the first time, and Center panel in 📁 Size glides it back. A touch on the edge lights a short ripple. The panel has a dark titanium frame, a carbon fiber top bar, and thick sapphire glass panes, all drawn on the phone. Titles and typed text are Laser Jet Bright teal with a harsh glow. New theme Titanium Sapphire; OLED Bioluminescent stays the start theme. The folder icons were redrawn sharper. Extra polish: the row being typed in glows at its edge, a soft tick when a value is saved (no new permission; CAMERA stays the only one), a center guide while dragging, and the pulse, the edge swirl, the ripples, and the cursor glow all stop with Remove animations. Test diamond still starts off. No sliders, no mic, no voice. GitHub built the app and published it again as step-2-preview.
- **PR / commit:** Pull request #6, not merged. It is stacked on pull request #5. Commits `956899c`, `f5b9972`, and `74deffb`.
- **State:** waiting for review
- **Next:** Rex (Agent 04) reviews pull request #6 and does not merge it. Then Royalty installs step-2-preview on the Galaxy S25 Ultra and the Galaxy A37 and runs the test steps in pull request #6. Do not start Step 3 or Step 7.
- **TODO, folder art:** Royalty will send her final folder icon art. When it lands in `03 Design/References`, use it in place of the drawn folder icon (`FolderIconView.kt` in `04 Build`). Until then the drawn folder stands in.

## 2026-10-06 4:09 PM CT — Agent 05, Claude Code

- **Bootloader step:** Step 2.3. Solid Laser Jet Cursor (folded into Step 2.2)
- **What was done:** Royalty asked to apply Step 2.3 along the way, so it is in the same pull request as Step 2.2. The Step 2.3 prompt said to wait until 2.2 merged, but Royalty's later chat message asked for it now. The laser cursor is now one solid, opaque block in one color, pure white (#FFFFFF) by default, with no soft multi-shade fill. 📁 Colors → 📁 Laser cursor color keeps the hex code and the six 0 to 100 mixers and adds Cursor glow, from 0 to 100, 0 by default. Above 0, a soft outer glow shows. With Remove animations on, the blink stays and the glow stays off. Reset all and the OLED Bioluminescent theme bring back the solid white cursor with the glow at 0. No Step 2.2 work was undone.
- **PR / commit:** Pull request #6, not merged. It is stacked on pull request #5. Commits `956899c`, `f5b9972`, and `74deffb`.
- **State:** waiting for review
- **Next:** Same as Step 2.2. Rex reviews pull request #6. Royalty runs the cursor steps in it.

## 2026-10-07 12:08 PM CT — Agent 05, Claude Code

- **Bootloader step:** Step 2.2. Premium Panel Polish (with Step 2.3)
- **What was done:** Step 2.2 phone feedback received. Royalty tested the Step 2.2 build on her Galaxy S25 Ultra and wants to keep going, one section at a time. Small changes she notices on the phone wait until the app is finished. She asked for a plain start look: OLED black with a solid white Laser Jet cursor, like before the titanium skin, and the titanium look kept as a choice. She filled in the Step 2.4 prompt with her answers: full screen at Width and Height 100, Typing speed 1, Font size 50, Opacity and Glow 50, a stronger Glow 100, no cut words at Font 100, Pulse off with its own toggle, the swirl removed, ROYGBIV plus White and Black, important folders first, and a ⋮ menu like Samsung My Files. Pull request #6 is still open and not merged, so Step 2.4 is built on its head.
- **PR / commit:** Pull request #6, not merged. Commit `f75089e`.
- **State:** waiting for review
- **Next:** Step 2.4. Agent 05 picked that up.

## 2026-10-07 12:08 PM CT — Agent 05, Claude Code

- **Bootloader step:** Step 2.4. Presets and Layouts (Royalty's Step 2.2 phone-test feedback)
- **What was done:** Step 2.4 stop point. The start look, for the first open and Reset all, is plain OLED Bioluminescent: true black, a solid white cursor with glow 0, and Width and Height 100 filling the screen inside the system bars. Opacity, Glow strength, and Font size are 50, Typing speed 1, and Corner roundness 4. Test diamond and Scan line are off, and Center guide is on. Pulse is off with its own toggle and Pulse speed, and a master Glow toggle was added. Titanium frame and Sapphire glass are toggles in 📁 Display, off at first. Titanium Sapphire stays a theme. Glow strength 100 is now big and bright, while the middle of the glass stays dark. The glass swirl and its settings are removed. Colors are now nine: Red, Orange, Yellow, Green, Blue, Indigo, Violet, White, and Black. Hex codes work both ways, and old six-number saves still load. Words never split or end in "…" at Font size 0, 50, or 100. A new ⋮ menu holds the view (List, Big list, Grid 2, Grid 3), the sort (Important first, A to Z, Custom, with Move folders to drag), Folder size, and the side list (Show, Swipe to open, Hidden), all saved. Folders are in important-first order. The Record bar fades while the full panel covers it. Three independent reviewers checked the work, and every real finding was fixed. Grok handoff notes were added in `09 Grok Handoff/`. No sliders, no mic, no voice. CAMERA is still the only permission. GitHub built the app and published it again as step-2-preview.
- **PR / commit:** Pull request #7, not merged. It is stacked on pull request #6. Commits `6d3b82a` and `2c797fb`.
- **State:** waiting for review
- **Next:** Rex (Agent 04) reviews pull request #7 and does not merge it. Then Royalty installs step-2-preview on the Galaxy S25 Ultra and runs the test steps in pull request #7. Do not start Step 3 or Step 7.
- **Asked for next (not built):** Royalty asked for a camera button and a mic button next to −, each a white symbol in a glowing white ring on black, with 📁 Camera and 📁 Sound folders. Camera off would really stop the camera. A call can't send a see-through picture, so viewers would see black, a background picture, or effects only. The mic needs the mic permission, which stays off until Royalty says go. Proposed as Step 2.5, waiting for her two answers.
- **Later:** a new toggle design Royalty will share. App icons in her new `OFFGRID_App_Icons` repo, tested on black and white backgrounds first. Nobody touches that repo yet.

## 2026-10-08 3:51 PM CT — Agent 05, Claude Code

- **Bootloader step:** FINAL full build (everything except voice). Milestone 1 of 5: new coded toggles.
- **What was done:** Started the final build on a new branch, `final-build`, made from pull request #7's head, so it holds Steps 2.1 to 2.4. The red and green picture toggles and both picture files are gone. Every toggle is now drawn in code: a circle with the power symbol inside. There are two styles, Pure White (the start) and Pure Black. On is lit or filled. Off is dim or outlined. They are sharp at every size, fade between on and off in a short moment (none with Remove animations), and are 52 dp wide, above the 48 dp touch target. A new 📁 Toggle style folder inside 📁 Themes picks the style and has an optional Toggle glow, off at the start. The choice is kept after a restart. The test build now publishes only from `final-build`. The pictures in `03 Design/References` were kept as design history.
- **PR / commit:** Branch `final-build`, not merged. The pull request opens when the build is done.
- **State:** waiting for review
- **Next:** Milestone 2, true full size at Width 100 and Height 100.

## 2026-10-08 3:55 PM CT — Agent 05, Claude Code

- **Bootloader step:** FINAL full build. Milestone 2 of 5: true full size at 100.
- **What was done:** Width 100 and Height 100 now fill the screen edge to edge, under the status bar, the camera hole, and the gesture bar. Only the words and buttons move in, so the title bar sits below the status bar, the − and ⋮ buttons stay clear of the round screen corners, and the resize corner stays out of the Back swipe and gesture areas. The bars are measured as if they always show, so pulling down the status bar never moves the panel. The keyboard never shrinks Height 100. A test measured the panel against the display at 100 and 100: both are 1152 by 2496 pixels on an S25 Ultra-sized screen.
- **PR / commit:** Branch `final-build`, not merged.
- **State:** waiting for review
- **Next:** Milestone 3, the fixes from Rex's pull request #7 review.

## 2026-10-08 4:09 PM CT — Agent 05, Claude Code

- **Bootloader step:** FINAL full build. Milestone 3 of 5: fixes from Rex's pull request #7 review.
- **What was done:**
  - The side list drawer's handle now sits under the open sheet, so a tap on a row's left edge opens that folder.
  - When the camera needs a tap (Try again or Open settings), the record bar never fades, and the panel moves aside to its tag so the button can be tapped. It comes back when the camera is live.
  - Turning on Move folders from A to Z saves the order of every folder, not just two. It asks first before replacing a custom order you already saved. A new Undo last move button in the ⋮ menu puts the last move back.
  - With Move folders on, a swipe still scrolls. A folder moves after a long press, or right away from its grip handle. A drag that changes nothing says nothing.
  - Words never split. A very long word shrinks just enough to fit on its own line. Fitting never asks for a second layout from inside measuring, so it can't loop.
  - Installing over an older build now lets the old settings go once, so the new start look shows. The private test marks stay.
  - New unit tests, in the repo, check the start look, saving and loading, that one-time settings move, Reset all, and folder order. GitHub runs them on every push.
- **PR / commit:** Branch `final-build`, not merged.
- **State:** waiting for review
- **Next:** Milestone 4, the effects and the rest of the roadmap (everything except voice).

## 2026-10-08 9:40 PM CT — Agent 05, Claude Code (stop point, session limit)

- **Bootloader step:** FINAL full build. Milestone 4 of 5: effects and the rest of the roadmap. Partly done.
- **What was done so far in M4:**
  - A GPU camera stage now sits between the camera and the video. It does mirror, time freeze with a glitch, quiet mode, echo trail, takeover with a flip-flop small box, blackout, the cloak void, ghost (last picture or blank), depth shift, and shatter. When nothing is on, the camera goes out exactly as before.
  - A saved no-face mode covers the very first picture with no fade, and ghost never freezes a live face.
  - Drawn effects are painted with Android's Canvas on their own thread and baked into the picture under the panel, so viewers see them and never the panel.
  - First drawn effects are in `effects/layers`: emoji rain, fireworks, glow border, red sweep, laser writing, ink, face diamond, radar, optical illusions, quiet light, and the test code.
  - The network permission a video library asked for is removed. CAMERA is the only permission.
  - Unit tests check the stage math, including no more than 3 flickers a second.
- **Where it stopped:** the helper agents hit the session limit before the beach, clouds, status label, flying heroes, roller coaster, and portal were drawn. I was writing the panel folders for 📁 Camera, 📁 Effects, 📁 Record, the platform kick tools, and the test results, in a new file so `TerminalPanel.kt` stops growing. Nothing in the panel turns the new effects on yet.
- **PR / commit:** Branch `final-build`, not merged.
- **State:** blocked on session limit, then continuing
- **Next:** finish the missing drawn effects, the effect folders, and the camera screen wiring, then M5.

## 2026-10-09 2:55 AM CT — Agent 05, Claude Code

- **Bootloader step:** FINAL full build. Milestone 4 of 5: effects and the rest of the roadmap. Done.
- **What was done:**
  - The panel has folders for every effect:
    - 📁 Camera: camera mode, face diamond, cloak void, status label, ghost, takeover, mirror plus scan.
    - 📁 Effects: 19 folders, each with toggles, typed 0 to 100 numbers, Fire now buttons, and a soundboard.
    - 📁 Record: record length with Stop, save to gallery, and a replay buffer.
  - All 17 drawn effects are on in the app, and each was checked in rendered pictures:
    - emoji rain, drawing, glow border, fireworks
    - portal, flying heroes, roller coaster, clouds, beach
    - red sweep, laser writing, radar, optical illusions
    - quiet light, test code, status label, face diamond
  - The camera stage adds the cloak void, ghost, blackout, time freeze, quiet mode, echo trail, takeover, depth shift, and shatter.
  - Each of the 18 platform folders has:
    - Red sweep on kick, and I got kicked (it never rejoins)
    - Open the app, and Use safe start
    - a Success code check, and Results with notes and dates
  - Panel settings has look presets: save, load, rename, delete.
  - OFFGRID to OFFGRID has handshake codes, locked by the phone's key store.
  - Every folder has its own Reset. No reset changes the camera mode or the test marks.
  - The face diamond starts on and follows the face, using the phone's own face finder. The screen now shows what viewers get, not a mirror.
  - Safety:
    - If the effects can't run, the video stays black while a no-face mode or the diamond is on.
    - The video stays black until the diamond is drawn.
  - An independent review found 8 real problems, and all 8 are fixed.
  - 39 unit tests pass. They include a check that no effect flashes more than 3 times a second.
  - CAMERA is still the only permission.
- **PR / commit:** Pull request #8 (`final-build`), not merged.
- **State:** waiting for review
- **Next:** Milestone 5: final polish, lint, the final report in pull request #8, and the phone test list.

## 2026-10-09 8:41 AM CT — Lane A base sync

- **Bootloader step:** Lane A. Panel UI
- **What was done:** Integrated the current `final-build` history into the Lane A branch and kept the final-build files and existing features. The PR had been forked from the Step 2 ancestor, so this is a merge commit rather than a rewritten rebase. Follow-up work is removing the side drawer and stale mic notes, adding layout tests, and verifying CI.
- **PR / commit:** Pull request #15, draft, targeting `final-build`.
- **State:** in progress
- **Next:** Resolve the remaining panel UI requests, run tests and build, and keep the PR unmerged for Rex's review.

## 2026-10-09 9:05 AM CT — Lane A review follow-up

- **What was done:** Removed side-panel navigation and its saved settings, corrected the recording notes, restored full-bleed sizing and the visible Back control/scroll thumb in the final-build panel, and added sizing/layout tests.
- **Validation:** The latest `final-build` Actions build passed. Local targeted Gradle tests could not start because Android Gradle Plugin 8.13.2 could not be resolved.
- **PR / commit:** Pull request #15, draft, targeting `final-build`.
- **State:** merge resolution and validation in progress
- **Next:** Finish final review, commit the merge, and keep the PR unmerged for Rex's review.

## 2026-10-09 9:10 AM CT — Lane A merge complete

- **What was done:** Published merge commit `77beeb9` with `final-build` as its second parent. The side panel is removed; full-bleed sizing, insets-aware border geometry, Back navigation, custom scroll thumbs, and sizing/layout tests are retained in the final-build panel.
- **Validation:** Secret scan and CodeQL reported no findings. The latest `final-build` Actions run passed. No Actions build ran for this PR head because the workflow's pull-request branch filter excludes `final-build`; local Gradle tests could not resolve Android Gradle Plugin 8.13.2.
- **Requests for other lanes:** Add `final-build` to the build workflow's pull-request branch filter so this PR can receive a CI run.
- **PR / commit:** Pull request #15, draft, targeting `final-build`.
- **State:** open and unmerged for Rex's review

## 2026-10-09 9:43 AM CT: Lane A round 2 fixes

- **What was done:** Restored the panel top bar and camera/microphone quick toggles, added the `fx.cam.on` and `fx.mic.*` rows, corrected the recording audio notes, removed the resize grip, added bottom-edge resize zones, strengthened the high-glow bloom, and split panel behavior into focused files. `TerminalPanel.kt` is under 1,000 lines.
- **Tests added:** Full-bleed measurement, panel geometry and resize zones, border inset, glow curve, scroll thumb geometry, responsive top-bar layout, microphone contract coverage, and toggle persistence/UI refresh.
- **Validation:** `git diff --check` passes and no grip, slider, TODO, FIXME, or em-dash matches remain in owned panel sources. Local Gradle tests cannot configure because Android Gradle Plugin 8.13.2 is unavailable from the configured repositories. The latest successful PR build is from the earlier head, not these round 2 changes. The older failed `final-build` run stopped while downloading Android SDK command-line tools.
- **Device evidence:** No screenshot or `dumpsys gfxinfo` result is available from this environment.
- **Validation follow-up:** Commit `f88aed9` is published. Code review reported no findings; CodeQL had no analyzable code in scope. No build has run for this head.
- **PR:** Pull request #15 remains draft, based on `final-build`, and unmerged.
- **State:** published for CI and Rex review

## 2026-10-09 11:10 PM CT: Lane A panel finish

- **What was done:** Camera, microphone, settings, and Back controls are drawn in code as bioluminescent power buttons. Glow layers are cached bitmaps, so frames do not blur again. Width and height 100 fill the window. Content padding clears the status bar, camera cutout, gesture bar, and 30 percent of the corner radius. The top bar is 56 dp with 8 dp gaps and 40 dp controls. Width and height fields use tabular digits. The bottom resize edge is a 6 dp cached glow. The side drawer source is archived at `04 Build/archive/side_drawer.kt` and is not in the view tree. Night theme text is white on black. Day theme text is black on white. The debug workflow pull request filter now includes `final-build` so this pull request can run CI. Android Gradle Plugin stays at 8.13.2.
- **Tests:** `./gradlew assembleDebug testDebugUnitTest --offline` from `04 Build` with `ANDROID_HOME` set. BUILD SUCCESSFUL in 14s. 71 unit tests, 0 failures, 0 errors, 0 skipped. Classes: EffectPanelTest 6, PanelGeometryTest 14, PanelUiTest 12, PanelFinishTest 3, PanelSettingsTest 9, StageTest 9, RainbowMixTest 12, CameraToolsTest 5, EffectLayersTest 1.
- **PR:** Pull request #15 remains draft, based on `final-build`, and unmerged.
- **State:** local build and tests passed. CI result is recorded in the next entry.
