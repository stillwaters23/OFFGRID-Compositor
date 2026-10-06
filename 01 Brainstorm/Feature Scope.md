# Feature Scope

**Project:** OFFGRID-Compositor  
**Owner:** Royalty  
**Status:** Scope only. Nothing is built yet.  
**Target:** Royalty's private Android build first (Galaxy S25 Ultra and Galaxy A37). A PC and OBS version can come later.  
**Updated:** 2026-10-06 (America/Chicago)

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

- Provider options being compared: xAI TTS, ElevenLabs, Google Cloud Text-to-Speech, and Android's built-in TextToSpeech as a free offline fallback. Research is in progress and will be saved in `02 Research`.
- It has to be fast enough for live use, so the speech should start soon after Royalty presses send.
- The spoken audio mixes with the mic and the voice changer into the outgoing stream audio.
- Volume and speed are typed numbers, never sliders.
- Speech text stays on the phone. API keys are never kept in chat or in this repo.

### Rex voice preset
- This is a named preset in the voice picker called **Rex**.
- The style is warm, sharp, and concise, like a calm operator.
- It's built from a stock or designed voice tuned to that tone, not a clone of a real person's voice.
- Royalty's own voices can be added as more presets later.

---

## Rules carried over

- Android first. PC and OBS come later.
- Test with a dummy account and protect the paid big-gifter account.
- Credentials never go in chat or in this repo.
- Don't build anything until Royalty says go.
