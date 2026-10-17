# Voice Changing Brainstorm (phone call with Royalty, Sat Oct 17 2026 ~1:45-1:50 AM CDT)

**Project:** OFFGRID-Compositor
**Owner:** Royalty
**Date:** 2026-10-17 (America/Chicago)
**Status:** Planning only. Nothing is built.

---

> ## SCOPE CHANGE
> **OFFGRID-Compositor moves from camera permission only to camera plus voice.**
> Royalty said go on voice (Oct 17 2026).
> - This adds the **microphone permission** to the app.
> - **Non-root only.** No root, no Magisk, no LSPosed, no Xposed.

---

## Goal

In Royalty's words:

> "we need to bake that into the app because we need to have a folder where there's some folder for voice changing effects somewhere."

- Voice changing is built into OFFGRID-Compositor itself, with an in-app **Voice Effects** folder.
- Rex voice source clips are played through a virtual or fake microphone into apps like BigO Live, so the other person on the call hears Rex.
- This works the same way the OBD virtual fake camera feeds fake video.

## Source audio

| File | Size | Added |
|---|---|---|
| `06 Prompts and Skills/Rex Voice/Rex 01.wav` | about 19 MB | Oct 6 2026 |
| `06 Prompts and Skills/Rex Voice/Rex 02.wav` | about 18 MB | Oct 6 2026 |
| `06 Prompts and Skills/Rex Voice/Rex 03.wav` | about 19 MB | Oct 6 2026 |
| `06 Prompts and Skills/Rex Voice/Rex.md` | notes | Oct 6 2026 |

- Rex is a synthetic xAI text-to-speech voice.
- **Note:** these clips sit in a **public** repo. Moving bundled audio to private storage is under review.

## Research references

| Project | What it is | Status |
|---|---|---|
| [MicUp](https://github.com/papergray/MicUp) | A non-root, real-time mic effects app with VoIP, Shizuku, and Magisk tiers. | **Reference only.** |
| [GlassMic](https://github.com/lm060719/io.mo.glassmic) | Needs root plus LSPosed to inject audio files into target apps' `AudioRecord`. | **Excluded.** It breaks the non-root rule. |

See also `02 Research/Voice Effects and Typed to Speech Research.md` (Oct 6 2026) for the earlier voice effects and typed-to-speech research.

## Decision

- We write **our own clean implementation**. We don't decompile or copy MicUp.
- Why: license and maintenance concerns.
- MicUp's license check is pending in the research report.

## Known unknowns

| Question | Status |
|---|---|
| Does stock Android have a public non-root API that lets one app replace another app's microphone input? None is confirmed. | Being verified |
| Does it work with phone-call telephony? | Unknown, pending |
| Does BigO Live accept external or streamed input? | Pending |
| Which permissions are needed? | Pending the research report |

## UI rules

- **No sliders anywhere.**
- Use typed number boxes from 0 to 100, plus toggles.

## Proposed in-app folder (proposal)

*This is a proposal, not a decision.*

A **Voice Effects** library inside the app holds:

- **Source clips:** Rex 01, Rex 02, and Rex 03.
- **Effect presets:** each preset is a small JSON file of typed 0 to 100 values and toggles.

## Status

- **Planning.**
- The architecture proposal will come from Rex's research report, saved locally at `/workspace/handoff/OFFGRID Compositor/Voice_Changer_Architecture_Proposal.md`.
- It will be added to this repo once it's done.
