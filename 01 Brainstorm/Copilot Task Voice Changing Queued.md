> # STATUS: QUEUED, NOT ACTIVE.
> **Do not start until Royalty/Rex activate it AND the research report is attached.**

# Copilot Task: Voice Changing (Queued)

**Project:** OFFGRID-Compositor
**Written:** 2026-10-17 (America/Chicago)
**For:** a future Copilot coding agent
**Read first:** `01 Brainstorm/Voice Changing Brainstorm.md`

---

## 1. The task

Implement **non-root** voice changing in OFFGRID-Compositor, as described in `01 Brainstorm/Voice Changing Brainstorm.md`.

- No root, no Magisk, no LSPosed, no Xposed.
- Write our own clean code. Don't decompile or copy MicUp or any other app.

## 2. Gate: wait for the research report

- Don't start until the research report (`Voice_Changer_Architecture_Proposal.md`) is attached to this task or added to the repo.
- Implement **only** the architecture the report recommends.
- If the report says a part can't be done without root, stop and report it. Don't work around it.

## 3. Where the code goes

- App module: `04 Build/app`
- Existing package root: **`com.offgrid.compositor`** (source in `04 Build/app/src/main/java/com/offgrid/compositor/`)
- Create a new package: **`com.offgrid.compositor.voice`** (or `com.offgrid.compositor.voicefx`), in `04 Build/app/src/main/java/com/offgrid/compositor/voice/`.

## 4. Voice Effects storage layout

Create the in-app storage for Voice Effects:

```
voice_effects/
  clips/           source clips (Rex 01, Rex 02, Rex 03)
  presets/*.json   one small JSON file per effect preset
```

### Preset schema

Each preset holds typed 0 to 100 values and toggles only. Define the exact fields from the research report. The general shape:

```json
{
  "name": "Example preset",
  "version": 1,
  "values": {
    "example_value": 50
  },
  "toggles": {
    "example_toggle": false
  }
}
```

- Every number is a whole number from 0 to 100. Reject or clamp anything outside that range.
- Write the schema down in the PR and test it.

## 5. UI screen

- Add a Voice Effects screen.
- Typed number boxes from 0 to 100 and toggles only. **No sliders.** A slider is an instant FAIL.
- Match the existing terminal panel style and toggles in `04 Build/app/src/main/java/com/offgrid/compositor/terminal/`.

## 6. Permissions

- Add `RECORD_AUDIO`, and any other permission, **only** if the research report lists it.
- Request every dangerous permission at runtime, when it's needed.
- The current manifest removes `RECORD_AUDIO` on purpose (`tools:node="remove"`). Change that only as the report directs.

## 7. Hard rules

- No secrets, keys, tokens, or credentials in code, docs, or commits. See `06 Prompts and Skills/Credentials Security Rule.md`.
- No `.gguf` files.
- Don't commit large model files.
- Never commit to `main` or `final-build`.
- New branch names use underscores, not dashes.

## 8. Tests

- Add unit tests for the preset schema (loading, saving, the 0 to 100 range, and toggles) and for the storage layout.
- Run `./gradlew testDebugUnitTest assembleDebug` from `04 Build/` and report the real result.

## 9. Pull request

- Open the PR against **`final-build`**.
- **Never merge it.**
- The PR body must list the **real** changed files and the **real** test results, honestly.
- A previous Copilot PR (#46) was rejected for an empty commit with a false description. An empty or misleading PR is rejected.

## 10. Build gates

The work moves through these gates, in order:

1. **Build**
2. **Theme review**
3. **Independent QA**
4. **HR release gate** (see `06 Prompts and Skills/Agent Lanes and HR Review.md`)
5. **Royalty merges**

No step is skipped, and only Royalty merges.
