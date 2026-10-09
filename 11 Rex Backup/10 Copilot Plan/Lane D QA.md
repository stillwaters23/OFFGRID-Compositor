# Lane D QA

**Start only after Lanes A, B and C have opened their PRs into `final-build`.** Do not write features. Do not merge. Do not touch PRs #5 to #8.
**Read first:** `docs/10 Copilot Plan/Copilot Master Plan.md` (all of it) and `Lane A.md`, `Lane B.md`, `Lane C.md`.

## Job

For each lane PR, check every requirement in that lane's file against the code, the CI run and the evidence in the PR, and return one verdict per requirement and one overall:

- **PASS:** met, with evidence (file and line, test name and result, CI link, screenshot).
- **FAIL:** not met, with the exact gap and the smallest fix.
- **BLOCKED:** cannot be checked (for example needs Royalty's phone), with what is needed.

Post the report as a PR review comment (COMMENT, never APPROVE or merge) and, if you have a branch, as `docs/10 Copilot Plan/QA Reports/Lane X QA.md`.

## Checks for Every Lane

0. **Quality Bar (master plan section 0): FAIL the PR if any point is missed.** Check file sizes and focus, grep for `TODO|FIXME|stub|lorem|coming soon` and commented-out code, naming consistency, per-frame allocations in `onDraw` and render loops, 8 dp grid spacing in layout code, easing on every visible state change, unit tests for all logic, plain specific on-screen text with no generic AI-looking UI, and handled errors.
1. Branch is from `final-build` and the PR targets `final-build`; title matches the plan.
2. Only owned files changed (compare the file list with the ownership table). Any other file is a FAIL unless listed and approved under "Requests for other lanes".
3. CI green; `assembleDebug`, `testDebugUnitTest`, `lintDebug` pass.
4. Rules: no sliders (`SeekBar|RangeSlider|material.slider|Slider(`), no `TextToSpeech|SpeechRecognizer|android.speech`, no WebView, no Three.js, no banned names (Marvel, Iron Man, Stark, JARVIS, Avengers), no secrets or keys, no em-dashes in new docs, Title Case doc names.
5. The panel, overlay and remote UI never reach the video path (trace what draws into the compositor and encoders).
6. Start look intact: OLED Bioluminescent, true black, white solid cursor, cursor glow 0, the existing coded power toggles unchanged.
7. Merged manifest permissions equal the allowlist in Lane B, B0.
8. Contract keys in the master plan are spelled identically in Lane A's `FxKeys.kt` and Lane B's `audio/MicKeys.kt`.
9. Status log entry exists.

## Lane Specific Spot Checks

- **A:** glass equals window at 100/100 and the border is drawn inside rounded corners (A1); no `ResizeGrip`, no `SideDrawer` (A2, A6); glow curve test (A3); top bar alignment test (A4); text colors per uiMode (A5); scroll thumb tests (A7); every old `fx.` key still reachable (A11); existing toggles unchanged (the toggle redesign is a later phase, so any toggle redesign in a lane PR is a FAIL).
- **B:** CI triggers on PRs into `final-build` (B0); mic asked only on toggle (B1); camera unbinds on `fx.cam.on` off (B2); MP4 has audio and video tracks and the DSP tests pass (B3); replay buffer memory capped (B5).
- **C:** overlay never full screen by default, bubble works, `FLAG_SECURE` set (C1); FGS types correct and never started from background (C2); key 128 characters from `SecureRandom`, never logged, Keystore sealed (C6); HKDF and GCM tests, replay rejection, local-only binding (C7); hooks under 40 lines and marked (C9); the Bigo guide is honest about the virtual camera limit.

## Report Format

```
Lane X QA: OVERALL PASS | FAIL | BLOCKED
A1 PASS  evidence: ...
A2 FAIL  gap: ...  fix: ...
...
Rules: PASS
Files outside ownership: none
```
