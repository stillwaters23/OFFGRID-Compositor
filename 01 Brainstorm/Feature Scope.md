# Feature Scope

**Project:** OFFGRID-Compositor  
**Owner:** Royalty  
**Status:** Scope only. Nothing is built yet.  
**Target:** Royalty's private Android build first (Galaxy S25 Ultra and Galaxy A37). A PC and OBS version can come later.  
**Updated:** 2026-10-06 (America/Chicago). ElevenLabs removed from all voice options.

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
13. **Terminal panel (new).** The draggable, see-through master control panel with five platform folders: Bigo Live, Tagged, MeetMe, SuperLive, and TikTok.
14. **Self-call and secure handshake (new, folder 8).** Phones swap 300-character codes before an OFFGRID call. The codes rotate after every call, and blocked people's codes stop working.

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
