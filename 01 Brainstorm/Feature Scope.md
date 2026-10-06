# Feature Scope

**Project:** OFFGRID-Compositor  
**Owner:** Royalty  
**Status:** Scope only. Nothing is built yet.  
**Target:** Royalty's private Android build first (Galaxy S25 Ultra and Galaxy A37). A PC and OBS version can come later.  
**Updated:** 2026-10-06 (America/Chicago). Features up to 25. ElevenLabs removed from all voice options.

---

## Core design rule (governing principle)

**Every effect is seen by the other side.**

- In every platform folder (Bigo Live, Tagged, MeetMe, SuperLive, TikTok, Twitch, Kick, YouTube Live, Trovo, Telegram, and Tango), every effect must render into the outgoing video so the other person sees it on their end. Nothing in those folders stays local-only.
- Each platform folder is its own lane, with only what that platform allows.
- **OFFGRID-to-OFFGRID calling is the exception and the premium lane.** There, everything is visible to everyone on both sides: all effects, toggles, the terminal UI, audio-reactive pulses, everything.
- The private platform test toggles (feature 15) are testing tools, not effects, so they don't break this rule.

---

## Terminal communication rule

Everything the terminal says to Royalty, including guidance lines, IP leak alerts, status messages, and spoken text, is written like it's talking to a 13-year-old who's new to the cyber world. That means simple words, short sentences, and acronyms spelled out with a one-line meaning. For example, instead of "WebRTC leak detected," it says "Your real internet address is showing. Turn on Stealth mode now." The full rule is in `06 Prompts and Skills/Terminal Plain Language Rule.md`.

---

## Ongoing feature list

1. **Drawing effects.** Draw on yourself during a live stream. The ink can stay fixed on the screen or be pinned to the face so it moves with you. It has to be baked into the outgoing video so the PK opponent and viewers see it.
2. **Emoji rain.** Raining emojis over your box, drawn into the outgoing video as the top layer.
3. **Floating overlay panel.** A control panel that floats over the stream to fire effects.
4. **Typed number inputs, no sliders.** Every numeric setting is a typed number field. Never use sliders.
5. **Termux-style nested folders.** Folders nested inside folders, Termux-style. Bioluminescent glow-stick folders, not skinny folders.
6. **Soundboard toggle (new).** Each effect can have its own sound.
7. **Replay buffer (new).** Keeps the last 30 seconds of the outgoing stream.
8. **Typed-to-speech (new).** Royalty types text and it's spoken out loud in the outgoing stream. It includes a Rex voice preset.
9. **Ghost mode (new).** Makes Royalty invisible to everyone. Details to come.
10. **Cloaking effect (new).** Predator-style cloak. Royalty's video box looks like empty void to the other person.
11. **Camera blackout and cloaking modes (new).** Pick solid black, the cloak void, or a status label like "CLOAKED."
12. **Screen takeover and flip-flop switch (new).** Make Royalty's video fill the screen, and swap who's big and who's small.
13. **Terminal panel (new).** The draggable, see-through master control panel with platform folders for Bigo Live, Tagged, MeetMe, SuperLive, TikTok, Twitch, Kick, YouTube Live, Trovo, Telegram, and Tango.
14. **Self-call and secure handshake (new, folder 8).** Phones swap 300-character codes before an OFFGRID call. The codes rotate after every call, and blocked people's codes stop working.
15. **Platform test toggles (new, private).** Test toggles in every platform folder that check what each platform allows.
16. **Audio-reactive terminal UI (new).** The terminal's folders, toggles, and glow pulse to music and voices, baked into the outgoing video so viewers see it, with a typed 0 to 100 sensitivity.
17. **Traveling glow border (new).** A glowing border loops around the video box, synced to Royalty's heartbeat or the audio pulse.
18. **Fireworks bursts (new).** Fireworks burst when a toggle flips or a gift lands.
19. **Folder guidance panel (new).** Every platform and effect folder shows a laser typewriter guidance panel: what you can do, app or web, browser, VPN setup, and which phone to use.
20. **Portals and wormholes (new).** A swirling portal or wormhole opens inside Royalty's video box.
21. **Flying superheroes (new).** Original animated heroes fly around inside the video box, picked from a set.
22. **Roller coaster (new).** An animated coaster track and cars ride through the video box.
23. **Flying through clouds (new).** An aerial view soaring through clouds.
24. **Beach theme (new).** Pure ocean beach scenery fills the whole box, with no person in frame.
25. **IP leak check (new).** Shows what IP and country the other side sees, so Royalty can confirm ProtonVPN Stealth is masking the real IP. A leak sets off a red flash and a sound alert right away.

Related design references are in `03 Design/References`. The toggles there should all look exactly the same as each other, and the typewriter style follows the Grok Bot Laser Typewriter UI Spec.

---

## 6. Soundboard toggle

**What it does:** each visual effect can play its own sound when it fires.

- Effects covered: drawing, emoji rain, toggles, the typewriter, and any future effect.
- One master soundboard toggle turns all effect sounds on or off.
- Each effect has its own settings:
  - Pick a sound (built-in list, and possibly Royalty's own sound files later)
  - Mute just that effect
  - Volume as a typed number, never a slider
- Open question for later: should the sounds go out in the stream audio so viewers hear them, or play only on Royalty's phone? Decide before building.

## 7. Replay buffer

**What it does:** keeps the last 30 seconds of the outgoing stream on the phone, so Royalty can clip a moment without streaming it again.

- It always keeps a rolling window of the most recent 30 seconds of the finished outgoing video, with the effects already in it.
- A clip button saves the current 30 seconds as a video file on the phone.
- Clips stay on the phone only. No automatic upload anywhere.
- The buffer length is a typed number (default 30 seconds), not a slider.
- Keep the storage and battery cost small, and test on the Galaxy A37.

## 8. Typed-to-speech, with a Rex voice preset

**What it does:** Royalty types text, and it plays as spoken audio mixed into the outgoing Bigo stream, so the opponent and viewers hear it.

- Main provider: xAI text-to-speech API, streamed live, using the stock Rex voice (about $15 per million characters).
- Backup: Google Cloud Text-to-Speech (first 1 million characters per month free).
- Free offline backup: Android's built-in TextToSpeech.
- ElevenLabs is not used anywhere in this project. Royalty doesn't want their synthetic voices.
- Full research is in `02 Research/Voice Effects and Typed to Speech Research.md`.
- It has to be fast enough for live use, so the speech should start soon after Royalty presses send.
- The spoken audio mixes with the mic and the voice changer into the outgoing stream audio.
- Volume and speed are typed numbers, never sliders.
- Speech text stays on the phone. API keys are never kept in chat or in this repo.

### Rex voice preset
- This is a named preset in the voice picker called **Rex**.
- The style is warm, sharp, and concise, like a calm operator.
- It uses xAI's stock Rex voice, not a clone of a real person's voice.
- Royalty's own voices can be added as more presets later.

## 9. Ghost mode

**What it does:** makes Royalty invisible to everyone. Royalty's description is short so far, so more details are needed before this is designed.

## 10. Cloaking effect (Predator style)

**What it does:** while Royalty is in someone's video box during a call or battle, the box looks completely empty to the other person, like looking through a wormhole into nothing. They see a void or absence, not Royalty's face and not the room.

- This is different from ghost mode. Ghost mode makes Royalty invisible to everyone. Cloaking makes the video box itself look like empty space to the viewer.
- Inspired by the Predator cloak. It can have a shimmer or ripple edge when it turns on and off.
- It has to be baked into the outgoing video so the other person actually sees it.
- Strength and fade time are typed numbers, never sliders.
- Scope only. Nothing is built yet.

## 11. Camera blackout and cloaking modes

**What it does:** gives Royalty a picker of selectable modes for the outgoing camera feed, instead of one fixed effect.

1. **Blackout.** The outgoing feed is solid black.
2. **Cloaked.** The feed shows the void, using the Predator-style cloak from section 10.
3. **Status label.** Instead of video, the box shows a status word like "CLOAKED" or "INVISIBLE."

- These sit alongside ghost mode (section 9) and the Predator cloak (section 10).
- Each mode is baked into the outgoing video so the other person sees it.
- The status label text can be edited. Sizes and fade times are typed numbers, never sliders.
- Scope only. Nothing is built yet.

## 12. Screen takeover and flip-flop switch

**What it does:** lets Royalty control the size of the video boxes, and swap who is big and who is small during a call.

1. **Screen takeover.** Resize the date box so Royalty's video fills the whole screen and the other person shrinks to a small box.
2. **Flip-flop switch.** One switch swaps who is in the big box and who is in the small box. Royalty can flip it back and forth during a call.

- The controls live in the terminal panel's folder structure, like Windows or Samsung internal storage folders. Each folder has an on/off toggle.
- Sizes are typed numbers from 0 to 100, never sliders, the same as emoji rain and the other features.
- Feasibility note: Bigo and other apps draw the battle and call layout on each viewer's own phone, so OFFGRID can't resize the other person's box on their screen. Full takeover works inside an OFFGRID-to-OFFGRID call, or inside Royalty's own outgoing video (for example, Royalty fills the frame with the opponent shown as a small inset). Confirm the approach before building.
- Scope only. Nothing is built yet.

## 13. Terminal panel (master control UI)

**What it does:** this is the master control panel for every feature. Royalty uses it during a call or stream to fire effects in real time.

- **Draggable:** the terminal box can be moved anywhere on the screen during a call.
- **See-through:** about 50% opacity, so it doesn't fully block the view. The opacity is a typed number from 0 to 100.
- **Low-key:** it's small enough that it looks like Royalty isn't doing anything on the phone while in the date box.
- **Platform folders:** each platform gets its own folder, and each folder holds only the effects and features that platform allows. That means every folder has its own set of toggles, typed 0 to 100 number inputs, and on/off switches.
  1. Bigo Live
  2. Tagged
  3. MeetMe
  4. SuperLive
  5. TikTok
  6. Twitch
  7. Kick
  8. YouTube Live
  9. Trovo
  10. Telegram
  11. Tango
- **Real time:** tapping a folder item or flipping a switch triggers that effect right away on the outgoing stream.
- The folder style follows feature 5 (Termux-style nested folders with bioluminescent glow-stick folders) and the identical toggles in `03 Design/References`.
- Each platform's research is in `02 Research` or on Rex's computer, and that research decides what goes in its folder.
- Scope only. Nothing is built yet.

## 14. Self-call and secure handshake (folder 8)

**What it does:** a mutual check for OFFGRID-to-OFFGRID calls, so only phones Royalty has approved can connect.

- Each phone generates its own 300-character code.
- Both phones swap codes with each other before the call connects. If either code doesn't match, the call doesn't connect.
- The codes are saved on the phone inside the Termux-style terminal.
- The codes rotate automatically to new ones after every call ends.
- If Royalty blocks or deletes someone, that person's codes stop working. Because the codes keep changing, they can't reach Royalty again.
- It lives in the terminal panel as folder 8.
- Design note for later: use real, standard encryption (key pairs that change every call) rather than plain text codes, and keep the keys in Android's secure key storage, never in chat or this repo.
- Scope only. Nothing is built yet.

## 15. Platform test toggles (private testing only)

**What it does:** every platform folder in the terminal panel gets a set of test toggles that check what that platform allows. They're for Royalty's private testing only and don't ship as user-facing features.

- Folders: Bigo Live, Tagged, MeetMe, SuperLive, TikTok, Twitch, Kick, YouTube Live, Trovo, Telegram, and Tango.
- Example tests in each folder:
  - Does burned-in video work?
  - Does the site or app accept an outside video feed?
  - Do chat and gifts connect?
  - Does a stream key exist?
- Each test is an on/off toggle, with typed 0 to 100 inputs where needed, matching the terminal panel design. Never sliders.
- Test results decide which effects go into each platform's real folder.
- Test only with the dummy account and never the paid big-gifter account.
- Scope only. Nothing is built yet.

## 16. Audio-reactive terminal UI

**What it does:** the folders, toggles, and glow effects in the terminal panel pulse and react to sound, so the whole UI becomes a live instrument instead of a static menu.

- The pulsing folders, toggles, and glow are baked into the outgoing video, so viewers and the person on the other end see the audio-reactive effects too, not just Royalty.
- It reacts to sound: music, Royalty's voice, and the other person's voice.
- It has an on/off toggle and a sensitivity setting as a typed number from 0 to 100, never a slider, matching the terminal panel design.
- Sound sources: the phone's mic picks up the room, music, and Royalty's voice. The other person's voice is picked up by the mic when it's playing on speaker, or comes in directly during an OFFGRID-to-OFFGRID call.
- Scope only. Nothing is built yet.

## 17. Traveling glow border

**What it does:** a glowing border travels in a loop around the edge of the video box, synced to Royalty's heartbeat or to the audio pulse from feature 16.

- It's baked into the outgoing video, following the core design rule, so viewers see it.
- On outside platforms, the border frames Royalty's box as the other person sees it, because OFFGRID can only draw inside Royalty's own video. In an OFFGRID-to-OFFGRID call, it can truly loop around the other person's box.
- Sync source picker: heartbeat or audio pulse. Heartbeat needs a heart-rate source, like a Galaxy Watch through Samsung Health or Android's Health Connect.
- On/off toggle. Speed, thickness, and glow strength are typed numbers from 0 to 100, never sliders.
- Scope only. Nothing is built yet.

## 18. Fireworks bursts

**What it does:** fireworks burst on the other person's screen when Royalty flips a toggle or when a gift lands.

- Triggers: any toggle flip, or a gift landing. This is especially useful on Tango, where gift events can trigger effects. On platforms without gift events, fireworks fire from toggles only.
- It's baked into the outgoing video, following the core design rule, so viewers see it.
- On/off toggle. Burst size, count, and duration are typed numbers from 0 to 100, never sliders.
- Scope only. Nothing is built yet.

## 19. Folder guidance panel (expanded from the platform status prompt)

**What it does:** every platform folder and every effect folder in the terminal panel shows a guidance panel in the OFFGRID laser typewriter font, typed out letter by letter with the same hard block cursor. It's read-only guidance, not a control.

- Platform folders: Bigo Live, Tagged, MeetMe, SuperLive, TikTok, Twitch, Kick, YouTube Live, Trovo, Telegram, and Tango. Every effect folder gets one too.
- Each guidance panel tells Royalty:
  1. **What you can and can't do** in that folder.
  2. **Where you can and can't do it:** the native app or the website only, with the exact web address. For example, meetme.com belongs to the MeetMe app's company, but meetme.video is a separate browser-only site.
  3. **Which Android browser to use** (Chrome, Firefox, Brave, or Kiwi Browser) and where to download it. Kiwi Browser was reportedly discontinued in early 2025, so confirm it's still maintained and safe before listing it.
  4. **VPN setup required:** ProtonVPN in Stealth mode, with a Philippines exit server for PH streams. Order matters: create the Cloaked number with the VPN off first, then turn on Stealth mode before opening the app.
  5. **Which phone to use:** dummy phone 1, dummy phone 2, S24, S25 Ultra, or A37.
  6. **IP check:** the live result from the IP leak check (feature 25), green when the real IP is masked and red if it leaks.
- The values come from the research in `02 Research` and the results of the platform test toggles (feature 15), so each panel is filled in only after testing confirms it.
- The full per-folder template is in `06 Prompts and Skills/Folder Guidance Panel.md`.
- Scope only. Nothing is built yet.

## 20. Portals and wormholes

**What it does:** a swirling portal or wormhole opens inside Royalty's video box, giving the illusion of looking through to another place or disappearing into a void.

- Styles: a portal to another scene, a wormhole swirl, and a disappear-into-the-void transition that ties into the cloak (feature 10).
- It's baked into the outgoing video, following the core design rule, so viewers see it.
- On/off toggle. Size, spin speed, position, and glow strength are typed numbers from 0 to 100, never sliders.
- Scope only. Nothing is built yet.

## 21. Flying superheroes

**What it does:** animated superhero figures appear and fly around inside the video box during a stream or call. Royalty picks from a set of different flying heroes.

- Hero picker with several flying heroes to choose from.
- The heroes are original OFFGRID characters, not Marvel, DC, or other trademarked heroes, to avoid copyright problems and takedowns.
- It's baked into the outgoing video, following the core design rule, so viewers see it.
- On/off toggle. Number of heroes, size, and flight speed are typed numbers from 0 to 100, never sliders.
- Scope only. Nothing is built yet.

## 22. Roller coaster

**What it does:** an animated roller coaster track and cars appear and move inside Royalty's video box during a stream or call, so the viewer feels like they're riding or watching a coaster.

- Views: watching the coaster go by, or a rider's-eye view where the box follows the track.
- It's baked into the outgoing video, following the core design rule, so viewers see it.
- On/off toggle. Speed, size, and number of cars are typed numbers from 0 to 100, never sliders.
- Scope only. Nothing is built yet.

## 23. Flying through clouds

**What it does:** Royalty's outgoing video shows an aerial view flying through clouds, as if the camera is soaring above them.

- It can fill the background behind Royalty or take over the whole box.
- It's baked into the outgoing video, following the core design rule, so viewers see it.
- On/off toggle. Flight speed, cloud thickness, and height are typed numbers from 0 to 100, never sliders.
- Scope only. Nothing is built yet.

## 24. Beach theme

**What it does:** Royalty's outgoing video shows only a beautiful ocean beach, with sand, waves, and sky. It's pure scenery: Royalty isn't visible and no person is in frame. The beach fills the whole video box.

- Royalty's camera image is fully replaced by the beach, not blended in as a background.
- It's baked into the outgoing video, following the core design rule, so viewers see it.
- On/off toggle. Wave motion and brightness are typed numbers from 0 to 100, never sliders.
- Scope only. Nothing is built yet.

## 25. IP leak check

**What it does:** when Royalty enters a stream, OFFGRID checks what IP address the outside world sees, and shows it next to Royalty's real IP status, so Royalty can confirm ProtonVPN Stealth is masking the real IP before or during the stream.

- It shows the public IP and country the other side would see, and flags it red if it matches Royalty's real IP or the wrong country (for example, not the Philippines for a PH stream).
- Checks to include:
  1. Public IP and country, through the same network path the stream uses.
  2. DNS leak check, which servers look up web addresses for the phone.
  3. WebRTC leak check, for platforms used in the browser, since browser video calls can expose a real IP.
  4. IPv6 leak check, since some VPN setups only cover IPv4.
- **Instant alert:** the moment any check finds a problem (real IP showing, wrong country, DNS leak, WebRTC leak, or IPv6 leak), OFFGRID alerts Royalty right away with a red flash on the terminal panel and a sound cue, not just a quiet indicator. The alert names which check failed, and it fires both before and during the stream.
- The alert is for Royalty only. Like the private test toggles, it's a tool rather than an effect, so it stays on Royalty's screen and phone speaker and is never put into the outgoing video or stream audio. The alert sound and its volume are set like the soundboard (feature 6), with a typed volume number.
- It runs once before the stream opens and again on a timer during the stream. The recheck interval is a typed number, never a slider.
- Feasibility note: OFFGRID can't read what a platform's servers log. It confirms the address the internet sees from the phone on the same connection, which is what the platform also sees. It uses well-known IP-check services or a small self-hosted check, and stores no IP history outside the phone.
- The result also shows as an `IP CHECK` line in the folder guidance panel (feature 19).
- Scope only. Nothing is built yet.

## Potential future feature: floating text overlay on calls

**What it does:** during a video call, Royalty types a short message like "I love you" and it floats up as real text on the other person's screen, not burned into the video.

- This only works when both people are using OFFGRID for the call. It would be a custom video call with a side channel that sends the text (for example LiveKit or Stream).
- It can't be done inside Instagram, WhatsApp, FaceTime, Messenger, Meet, or Zoom calls. Those apps don't let outside apps into their calls, Meta ended third-party camera effects in January 2025, and Android overlays only show on your own phone.
- Fallbacks that do work elsewhere: burn the text into the outgoing video (like the Bigo effects), or screen share while the overlay shows.
- Message length (for example 1 to 80 characters) and how long it stays on screen (for example 1 to 30 seconds) are typed numbers, never sliders.
- Research is in `/workspace/research/Remote Text Overlay Research.md` on Rex's computer. Not built yet.

## Voice effects (live voice changer)

**What it does:** changes Royalty's mic voice in real time, built into the app and mixed into the outgoing stream audio.

- Main engine: Superpowered (real-time pitch and voice tone shifting; free for a private app under 1,000 installs).
- Free open-source backup: SoundTouch.
- Presets: alien, black hat hacker, robot, chipmunk, monster, smooth R&B crooner, and old western cowboy.
- The crooner and cowboy are style-matched voices, not copies of real celebrities.
- ElevenLabs is not used.
- Every setting is a typed number, never a slider.

---

## Rules carried over

- Android first. PC and OBS come later.
- Test with a dummy account and protect the paid big-gifter account.
- Credentials never go in chat or in this repo.
- Don't build anything until Royalty says go.
