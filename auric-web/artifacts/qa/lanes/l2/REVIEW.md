# Lane 2 QA review — PR #29

**Reviewed head:** `a51baaed` · **Required base:** `auric-base` · **Verdict:** CHANGES-NEEDED

Independent review against issue #28. The PR adds the L2 settings/state contract, but L1 is still unmerged and its files are absent from this branch. This means the settings UI can be reviewed, while end-to-end changes to the actual L1 beam cannot be confirmed.

## Acceptance criteria

| Criterion | Verdict | Evidence |
|---|---|---|
| L2-1 | FAIL | Presets and Apply update `--laser-color` in E2E, but this PR branch has no L1 `LaserStrip`; the test does not prove an actual live beam updates. |
| L2-2 | PASS | E2E accepts `#RGB`/`#RRGGBB`, shows an error for `#GGG`, `red`, and empty input, and checks Discard restores the previous value. |
| L2-3 | PASS | `tests/l2-contrast-guard.test.ts` passes the focused suite and checks the color/theme guard examples and ratios. |
| L2-4 | PASS | Focused unit suite checks wheel background samples and palette-dependent contrast calculations. |
| L2-5 | PASS | E2E verifies a failing color shows a theme/4.5:1 alert and disables Apply; unit tests verify the setter refuses it without changing state. |
| L2-6 | PASS | Unit tests exercise laser-bloom contrast rejection. |
| L2-7 | PASS | Unit loop checks resolved reel numbers against black and near-black across curated palettes and both moods. |
| L2-8 | NOT-VERIFIED | E2E checks persisted laser enabled state and intensity interaction, but cannot verify the actual beam is hidden or the hairline remains without L1's strip integrated. |
| L2-9 | PASS | E2E reload restores magenta, intensity 40, off, and dark number color; unit migration test hydrates v21 data without losing existing state. |
| L2-10 | PASS | State tests cover all ten persisted fields, bounds, invalid colors/layout/style, name limits, order normalization, and v21 hydration. |
| L2-11 | PASS | Apply/Discard and Restore unit/UI paths pass. Independently captured timer comparison at 390px is **0.350%** changed pixels against `auric-base` (`shots/base-timer-base.png` vs. `shots/l2-timer-base.png`), within 0.5%. L1 is absent from both sides. |
| L2-12 | FAIL | `npm ci` and full `npm test` fail as described below; new `.tx-reel-look` font size `20px` in `src/styles/reel-look.css` violates the 8dp rule. E2E does check both moods at 320/390/768/1280 without overflow. |

## Write-target audit

All 12 changed paths are permitted by issue #28. No deletions or out-of-scope paths were found.

| Changed path | Audit |
|---|---|
| `auric-web/screenshots/l2/laser-settings.png` | OK |
| `auric-web/screenshots/l2/number-contrast.png` | OK |
| `auric-web/src/components/auric/appearance-studio.tsx` | OK |
| `auric-web/src/components/auric/glow.tsx` | OK |
| `auric-web/src/components/auric/reel-look-folder.tsx` | OK |
| `auric-web/src/lib/appearance.ts` | OK |
| `auric-web/src/lib/contrast-guard.ts` | OK |
| `auric-web/src/lib/store.ts` | OK |
| `auric-web/src/styles/reel-look.css` | OK |
| `auric-web/tests/e2e/l2-reel-look.e2e.mjs` | OK |
| `auric-web/tests/l2-contrast-guard.test.ts` | OK |
| `auric-web/tests/l2-state.test.ts` | OK |

## Global and command audit

- `npm ci --ignore-scripts`: FAIL; `package-lock.json` omits `lru-cache@11.5.3`. The same failure was reproduced on `auric-base`; validation continued in the isolated snapshot with `npm install --ignore-scripts --package-lock=false`.
- `npm run typecheck`: PASS. `npm run lint`: PASS with one pre-existing warning at `src/components/auric/termux-chrome.tsx:169`. `npm run build`: PASS.
- `npm test`: FAIL, 184/195 passed; 11 failures reference missing `.grok/skills/og` content/assets and reproduce on `auric-base`.
- Focused unit tests: PASS, 13/13. L2 Playwright E2E: PASS, 1/1.
- Dev and production `scripts/browser-smoke.mjs`: pages return HTTP 200, no page errors or horizontal overflow; command exits 2 due external DNS failures, also present on baseline.
- Reporting requirement: FAIL. PR #29's description still says no implementation changes are included, despite 12 changed paths, and provides no per-criterion verdict/evidence table.
- No new npm dependencies, paid APIs, or cloud services. Reduced-motion behavior for the settings UI was not independently verified; no device result is claimed.
- Reference/look grade: NEEDS-WORK. `shots/laser-settings.png` and `shots/number-contrast.png` show the added settings; the target live-beam behavior cannot be graded until L1 is present.
