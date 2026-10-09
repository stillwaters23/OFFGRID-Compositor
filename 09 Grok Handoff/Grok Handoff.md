# Grok Handoff

**For:** Grok Bot, when you come back online  
**From:** Agent 05, Claude Code  
**Owner:** Royalty  
**Written:** 2026-10-07 (America/Chicago)

Welcome back. This file catches you up on everything Royalty and Claude Code talked about and built while you were offline. Read it first. Then read the newest entries in `08 Chat History/Agent Status Log.md`.

---

## 1. The project in one breath

OFFGRID is an Android app for Royalty's Galaxy S25 Ultra (and her Galaxy A37). It takes the front camera, adds effects, and sends that video out to live and video call apps. On top of the camera sits a floating terminal panel. It is Royalty's private control. The panel is never painted into the saved video.

The code is in `04 Build`. It is Kotlin, CameraX, and OpenGL ES. No web pages, no WebView.

Your own design memo, `03 Design/References/Grok Bot Laser Typewriter UI Spec.md`, is still the law for the look: laser text, a real typewriter, a solid block cursor, and zero noise.

---

## 2. How Royalty works with us

- **One section at a time.** Finish one section, then stop and tell her to say "continue". She is on the $20 plan and does not want to run out of usage in the middle of a step.
- **She tests on her phone.** Every step ships a test build called `step-2-preview` on the GitHub releases page. She installs it on the S25 Ultra and tries it.
- **Phone notes wait.** Small changes she notices while testing are saved for after the app is finished. She will bring them up then.
- **She is new to building apps.** Write in plain, short words. Say what to tap, one step at a time.
- **She wants to be impressed, and she wants you impressed too.** Aim for premium and professional, but never complicated. Easy to use comes first.
- **When she says "decide", pick the best simple choice.** Then say what you picked and why, in one line.

---

## 3. The rules that never bend

- No sliders anywhere. Every setting is a typed number from 0 to 100 or a toggle.
- Panel settings holds only folders. No loose settings.
- No mic. No RECORD_AUDIO. No text to speech. The voice step (Rex voice, features 38 and 39) stays paused until Royalty says go.
- CAMERA is the only permission the app asks for.
- No keys, passwords, tokens, or stream keys in the repo, the chat, or any doc. Credentials only ever go in Android's secure storage.
- Nothing that needs a computer. Royalty does everything from her phone.
- No Marvel, no Iron Man, no Avengers, no Stark, no JARVIS.
- No web stack: no HTML, CSS, Three.js, WebView, or marquee.
- No anti-cheat, root, or integrity tricks.
- Never delete or rewrite old docs. Only add.
- Do not touch `06 Prompts and Skills/Rex Voice/`.
- Do not merge pull requests. Royalty and Rex (Agent 04) review them.
- Do not start Step 3 (emoji rain) or later until Royalty says go.
- Do not change account, email, or other repo settings.

---

## 4. What got built, step by step

### Step 2. Terminal panel (pull request #4, merged)

The first floating panel: dark glass, a side list, glowing folders for Panel settings, OFFGRID to OFFGRID, and the 18 platforms. Each platform has an Effects folder and a Tests folder with private test toggles. Laser typed text in plain words.

### Step 2.1. Panel polish and theme office (pull request #5, open)

Fixed the ghost text. Added the resize corner, saved size and place, and the Test diamond toggle (starts off). Added true black glass that follows the phone's colors, and themes, colors, and glow settings. Added Reset all, which asks first.

### Step 2.2. Premium panel polish (pull request #6, open, stacked on #5)

Panel settings became folders only. Width and Height became typed boxes from 0 to 100, with two-finger pinch and the corner drag kept in step. A center guide shows while dragging, and Center panel glides the panel to the middle. Touch ripples, a glowing edge on the row being typed in, and a soft haptic tick when something is saved. The titanium frame and sapphire glass look and the Titanium Sapphire theme. An independent check judged the liquid swirl inside the toggles, and it was removed. The toggles are Royalty's original red OFF and green ON pictures again.

### Step 2.3. Solid laser cursor (folded into pull request #6)

The cursor is one solid white block. Cursor glow, from 0 to 100, starts at 0.

### Step 2.4. Presets and layouts (pull request #7, open, stacked on #6)

Royalty tested 2.2 on her S25 Ultra and wrote a prompt with answers. That prompt became Step 2.4:

- **The start look (first open and Reset all):** OLED Bioluminescent. Plain true black, with no titanium, carbon, or sapphire. A solid white cursor with glow 0. Width 100 and Height 100 fill the screen inside the status bar and navigation bar, like a terminal window. Opacity 50, Glow strength 50, Font size 50, Typing speed 1, Corner roundness 4. Test diamond off, Scan line off, Center guide on.
- **Pulse:** the glow is steady at the start. Pulse is a toggle, off at first, with its own Pulse speed. A master Glow toggle sits above Glow strength.
- **Glow strength:** 0 is no glow, 50 is normal, and 100 is big and bright. 100 was too weak before. Now it is several times brighter, measured.
- **Titanium:** Titanium Sapphire stays a theme. Titanium frame and Sapphire glass are toggles in 📁 Display, off at first.
- **The swirl:** the glass swirl and its settings are gone everywhere.
- **Nine colors:** Red, Orange, Yellow, Green, Blue, Indigo, Violet, White, and Black. A hex code works both ways. Old six-number saves still load as the same color.
- **No cut words:** at Font size 0, 50, and 100, no word is split or ends in "…". A test walks every folder and the menu to prove it.
- **The ⋮ menu,** like the three dots in Samsung My Files:
  - View: List, Big list, Grid 2, Grid 3.
  - Sort: Important first, A to Z, Custom. Move folders unlocks dragging, and the new order is saved.
  - Folder size: from 0 to 100. 50 is normal.
  - Side list: Show, Swipe to open, or Hidden.
  - Everything is saved, and Reset all brings back the start.
- **Order of folders:** Panel settings first, then OFFGRID to OFFGRID, then the 18 platforms. Inside Panel settings: 📁 Size, 📁 Display, 📁 Glow effects, 📁 Colors, 📁 Themes, 📁 Reset. The most used settings are at the top of each folder.
- **Extras:**
  - Folders slide smoothly when the view changes, with no motion when Remove animations is on.
  - A tick when a dragged folder lands.
  - The menu closes on a tap outside or Back.
  - The Record bar fades away while the full panel covers it, so its words never show through the glass.
  - The swipe handle keeps the middle of the left edge for itself, so the phone's Back swipe still works everywhere else.

---

## 5. Waiting for later (not built yet)

- **Camera and mic buttons (asked for next, as Step 2.5).** Royalty wants a camera button and a mic button right next to −. Each is a white symbol inside a glowing white ring on black, like a power button. 📁 Camera and 📁 Sound folders change what happens when they are on or off.
  - Camera off would really stop the camera, so her face never shows. A live stream or call can't send a see-through picture, because every app shows a box. Viewers would see black, a background picture, or her effects only. She is choosing which.
  - The mic needs the mic permission. That stays off until she says go, so for now the mic button can only show "mic off."
- **Neon ideas.** She shared a pink UV Index app and neon palettes, like #00E5FC, #0032FF, #FF00A6, and #7B02B3. They could become a Neon Pink theme and one-tap colors in 📁 Colors later.
- **New toggle design.** Royalty will share a new toggle switch design. Use it when she sends it.
- **App icons.** Royalty made a new repo, `OFFGRID_App_Icons`, to test app icons on black and white backgrounds first. Nobody touches that repo until she says so.
- **Folder icon art.** When her final folder art lands in `03 Design/References`, it replaces the drawn folder (`FolderIconView.kt`).
- **Step 3, emoji rain,** and every later step wait for her go.
- **Voice** stays paused.
- **Phone notes** from her testing come after the app is finished.

---

## 6. Where things are

| What | Where |
|---|---|
| The plan and every feature | `01 Brainstorm/Feature Scope.md` |
| Research papers | `02 Research/` |
| Design memos and toggle art | `03 Design/References/` |
| The app code | `04 Build/app/src/main/java/com/offgrid/compositor/` |
| The panel code | `.../terminal/` (TerminalPanel.kt, PanelMenu.kt, FolderLayout.kt, SideDrawer.kt) |
| Plain language and safety rules | `06 Prompts and Skills/` |
| Who did what, and what is next | `08 Chat History/Agent Status Log.md` |
| This handoff | `09 Grok Handoff/` |

The pull requests stack like this: #5 is on the first build branch, #6 is on #5, and #7 is on #6. Each one shows the plain test steps for Royalty.

---

## 7. What would help most from you

1. Read pull request #7's test steps and the Step 2.4 release notes. Tell Royalty, in plain words, anything that looks hard to follow.
2. Hold the look to your Laser Typewriter memo. The start look is black glass, laser text, and a solid white cursor.
3. When Royalty shares the new toggle design or the app icons, help her pick the cleanest one on both a black and a white background.

## 8. Added after the final build (pull request #8)

The final build (pull request #8, branch `final-build`, stacked on #7) builds everything except voice. This section is new. Nothing above was changed.

- **How the picture is made:**
  - The camera goes into `CompositorProcessor`.
  - `StagePainter` changes the picture on the GPU, following `effects/StagePlan.kt`: modes, freeze, quiet, echo, takeover, depth shift, and shatter.
  - The drawn effects in `effects/layers/` are painted with Canvas on their own thread by `OverlayRenderer`, then baked in.
  - The same finished picture goes to the screen, the video, and the replay buffer.
  - The panel is a normal view on top, so it is never in the video.
- **Settings:**
  - Every effect setting starts with `fx.` (`effects/FxKeys.kt`).
  - Its folder and start value are in `terminal/EffectFolders.kt`.
  - `EffectsController` reads them into the effect engine whenever a setting changes.
- **Face safety:**
  - The face diamond starts on and follows the face, using the phone's own face finder (`FaceTracker`).
  - If the effects can't run, or the diamond isn't drawn yet, the picture stays black whenever a no-face mode or the diamond is on.
  - No reset changes the camera mode.
- **Tests:** unit tests are in `04 Build/app/src/test`. They cover:
  - the stage math, including no more than 3 flickers a second
  - every drawn effect
  - presets, test marks, and folder resets
  - face math, sounds, and handshake codes
- **Still not built, and why:**
  - Voice (paused).
  - Anything that needs the mic: the audio-reactive panel, and audio or heartbeat sync.
  - The IP leak check (needs the internet permission).
  - OFFGRID-to-OFFGRID calls (need a server).
