# Voice Effects and Typed to Speech Research

**Project:** OFFGRID-Compositor  
**Date:** 2026-10-06 (America/Chicago)  
**Status:** Research only. Nothing is built or installed.  
**Decision:** ElevenLabs is excluded from every voice and typed-to-speech option. Royalty doesn't want their synthetic voices.

---

## Key finding

Voice effects have to be built into the app and mixed into the outgoing stream audio. In camera mode, Bigo takes the mic directly, so no outside voice changer app can reach it. The Voicemod SDK has reached end of life. AI voice conversion isn't fast enough to run live on an Android phone yet, so the plan uses real-time sound processing instead.

## Live voice effects

- **Main engine: [Superpowered](https://superpowered.com/licensing).** Real-time pitch shifting, voice tone (formant) shifting, and auto-tune. Free for a private app under 1,000 installs.
- **Free open-source backup: [SoundTouch](https://www.surina.net/soundtouch/).** Pitch and tempo shifting (LGPL license).
- **Audio capture:** Google's Oboe library at 48 kHz (see the [Oboe LiveEffect sample](https://github.com/google/oboe/tree/main/samples/LiveEffect)).

### Presets
| Preset | Recipe |
|---|---|
| Alien | Pitch up, plus ring modulation or flanger |
| Black hat hacker | Pitch down, voice tone down, bitcrush and distortion |
| Robot | Ring modulation and a fixed-pitch effect |
| Chipmunk | Big pitch up |
| Monster | Big pitch down, voice tone down |
| Smooth R&B crooner | Style-matched warm voice with light auto-tune, not a copy of a real singer |
| Old western cowboy | Style-matched low drawl, not a copy of a real actor |

### Legal note on celebrity voices
Copying a real person's voice can be illegal. Examples are Midler v. Ford, Waits v. Frito-Lay, and Tennessee's ELVIS Act. So celebrity-inspired presets are styles, not clones.

## Typed to speech

1. **Main: [xAI text-to-speech](https://docs.x.ai/developers/model-capabilities/audio/text-to-speech).** Streams live over a WebSocket and has a low-latency setting. It includes a stock voice named **rex** ("male, confident, clear"), which is the Rex preset. About $15 per million characters ([xAI pricing](https://docs.x.ai/developers/pricing)).
2. **Backup: [Google Cloud Text-to-Speech](https://cloud.google.com/text-to-speech/pricing), Chirp 3 HD voices.** The first 1 million characters per month are free, then $30 per million. No pitch control.
3. **Free offline backup: Android's built-in TextToSpeech.**

ElevenLabs is not used.

### Security
- API keys never go inside the app file, the chat, or this repo. Use a small proxy server or short-lived tokens.

## Audio mix and output

The chain goes mic, then the voice effect, then a mixer with typed-to-speech (the mic is turned down 6 to 10 dB while speech plays), then a limiter. From there it's encoded to AAC audio and sent by RTMP using Bigo's OBS stream key.

- Bigo's stream key may only be available to official broadcasters. Check this first.
- Until the key is confirmed, you can test on a PC with OBS and the free [Graillon](https://www.auburnsounds.com/products/Graillon.html) voice plugin.

## Rules

- Don't build anything until Royalty says go.
- Test with a dummy account and protect the paid big-gifter account.
- Credentials never go in chat or in this repo.
