# OFFGRID-Agents

Brainstorm notes from Royalty (2026-10-06). Label: **OFFGRID-Agents** (dash for GitHub path rules). Suggested repo path: `01 Brainstorm/OFFGRID-Agents.md`.

## Why this exists
After Gemini and Claude credits wind down, the long-term build shifts to **Kai 9000**: our own agent app. OFFGRID-Compositor stays the live effects app. Kai 9000 is the chat / back-office / computer brain.

## Product shape
- Chat UI in the style of ChatGPT and Claude (threads, clear replies, a back office for settings and tools).
- Merge in **Grok Bot–style computer abilities** (desktop, browser, files, connectors) where it makes sense on phone and later on the portable PC.
- One product family with OFFGRID: same owner rules, same safety locks, same status-log habit.

## Voice
- After a chat reply, Kai 9000 can **read the message aloud**.
- First path: Grok / Rex TTS via API key stored only in Android secure storage (never in the repo).
- Later path: **train our own model voice** over time (target on the order of **3 million+ characters** of training text). The app folds that training in across updates so Rex is not required forever.
- Short term: type text → Rex reads it. Long term: our voice, baked into the app.

## Self-improve (with guardrails)
- Periodic patch / update cycle.
- Optional **auto-update + self-improve** inside strict guardrails:
  - Nothing important deleted without a clear rule.
  - Bugs get fixed; quality improves over time.
  - Every change is **logged and baked into the APK / update trail** so Royalty can watch what changed.
  - If an update is good, leave it. If it is wrong, change the code so that mistake does not repeat, and keep that lesson in the logs.
- Human review stays in charge (Rex / Agent 04 style checks before risky merges).

## Safety (standing rules)
- No secrets, API keys, or passwords in GitHub, chat docs, or the APK as plain text. Keys live only in Android secure storage on the phone.
- No account / email changes by agents.
- No anti-cheat / root / integrity evasion.
- Voice cloning only for **our** voices we deliberately train (Rex first, then our own). No cloning other people’s voices. No ElevenLabs for this plan unless Royalty says otherwise later.
- Dummy phones for risky tests. Big-gifter / paid accounts stay protected.

## Build order (rough)
1. Finish OFFGRID-Compositor Step 2.x polish (panel, themes, cursor), then later Bootloader steps. **Pause before Step 7 voice until Royalty says go.**
2. Spec Kai 9000 chat + back office (this file grows into Design / Feature Scope).
3. Wire Grok API for chat + Rex read-aloud (secure key storage).
4. Computer abilities on the portable PC path.
5. Own-voice training pipeline and bake-in updates.
6. Guardrailed auto-update + in-APK change logs.

## Open questions for later
- Phone-only first, or phone + Tiny PC from day one?
- Which model APIs besides Grok?
- Where training audio / text is stored (Drive vs device vs private repo assets)?
- What exactly “autonomous optimization” is allowed to change without a human tap.

## Status
Brainstorm only. Not implementation. Do not start Kai 9000 code until Royalty gives a go after OFFGRID Step 2.x is in a good place.
