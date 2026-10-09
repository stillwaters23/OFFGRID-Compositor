# Lane 1 QA review — PR #27

**Reviewed head:** `1777d285` · **Required base:** `auric-base` · **Verdict:** CHANGES-NEEDED

This is an independent review of PR #27 against issue #26. The review used the PR head snapshot and reran the focused tests, browser test, typecheck, lint, build, full test suite, and dev/production browser smoke. No lane files were edited.

## Acceptance criteria

| Criterion | Verdict | Evidence |
|---|---|---|
| L1-1 | NOT-VERIFIED | `tests/e2e/l1-reels.e2e.mjs` verifies the demo's span reels have no `.aperture`, but does not check that other `DrumReel` usages (including Pavilion SoundDeck) retain the baseline aperture count. |
| L1-2 | PASS | E2E checks two strips per bank across 15 samples, 16px overhang, viewport containment, and samples the beam across a barrel gap. `shots/laser-strip.png`, `shots/laser-demo.png`. |
| L1-3 | PASS | E2E scale/width/mood matrix checks selected-row center alignment within 1px for all barrels in both banks. |
| L1-4 | PASS | E2E samples white-core RGB, bloom saturation 8px above center, and fade at the beam edge. `shots/laser-strip.png`. |
| L1-5 | NOT-VERIFIED | Unit coverage checks white-vs-bloom contrast, but the screenshot test does not sample selected-glyph pixels over the beam to confirm they remain white/legible. |
| L1-6 | PASS | `tests/l1-laser.test.ts` pins red default, enabled state, and full intensity; focused suite passes. |
| L1-7 | PASS | E2E emulates reduced motion and asserts no strip animations and immediate reel snap. |
| L1-8 | NOT-VERIFIED | Isolated rerun: normal median/p95 `16.7/16.8ms`; 4× throttle `16.7/16.7ms`. The first run measured `16.7/50ms` and `16.8/83.4ms`; the inconsistent p95 results do not establish a reliable pass. |
| L1-9 | PASS | E2E covers 24 scale/mood/width combinations and checks overflow, barrel bounds, selected glyph bounds, and strip alignment. |
| L1-10 | PASS | E2E checks D=3, W=2, M=11, Y=77 through 1280→320→768 and 390×844→844×390 resizes, and collects page/console errors. |
| L1-11 | NOT-VERIFIED | Unit mock verifies `click-01` and `[12,18,42]`; E2E verifies stillness, 3 drag rows, 5 keyboard rows, wheel ticks, and a fling. It does not assert exact wheel/fling tick counts against each changed row. |
| L1-12 | FAIL | `verifyTicks()` passes, but `npm test` fails 11/195 tests. The same 11 failures reproduce on `auric-base` and concern absent `.grok/skills/og` files/assets; the required full-suite gate is still failing. |
| L1-13 | PASS | Browser test visits `/laser-demo`, checks 15 samples/30 strips, verifies no navigation link and no page/console errors. |
| L1-14 | FAIL | Typecheck, lint, build, and L1 focused tests pass; `npm ci --ignore-scripts` fails because the lockfile omits `lru-cache@11.5.3`; full `npm test` fails as above. New laser CSS values meet the 8dp audit, with the 2px core documented as a hairline exception. |

## Write-target audit

Every changed path in PR #27 is permitted by issue #26; no deletions or out-of-scope paths were found.

| Changed path | Audit |
|---|---|
| `auric-web/package.json` | OK — scripts-only change permitted |
| `auric-web/screenshots/l1/laser-demo.png` | OK |
| `auric-web/screenshots/l1/laser-light.png` | OK |
| `auric-web/screenshots/l1/laser-strip.png` | OK |
| `auric-web/src/components/auric/drum-reel.tsx` | OK |
| `auric-web/src/components/auric/laser-strip.tsx` | OK |
| `auric-web/src/components/auric/span-reels.tsx` | OK |
| `auric-web/src/lib/laser.ts` | OK |
| `auric-web/src/lib/reel-geometry.ts` | OK |
| `auric-web/src/routeTree.gen.ts` | OK — generated route registration permitted |
| `auric-web/src/routes/laser-demo.tsx` | OK |
| `auric-web/src/styles.css` | OK — permitted reel/laser blocks and imports |
| `auric-web/src/styles/clicker.css` | OK — hook placeholder |
| `auric-web/src/styles/folders.css` | OK — hook placeholder |
| `auric-web/src/styles/reel-look.css` | OK — hook placeholder |
| `auric-web/tests/e2e/l1-reels.e2e.mjs` | OK |
| `auric-web/tests/l1-audio.test.ts` | OK |
| `auric-web/tests/l1-laser.test.ts` | OK |

## Global and command audit

- `npm ci --ignore-scripts`: FAIL on this PR and `auric-base`; lockfile is missing `lru-cache@11.5.3`. To continue validation, dependencies were installed in the isolated snapshot with `npm install --ignore-scripts --package-lock=false`.
- `npm run typecheck`: PASS. `npm run lint`: PASS with one pre-existing warning at `src/components/auric/termux-chrome.tsx:169`. `npm run build`: PASS.
- `npm test`: FAIL, 184 passed / 11 failed; failures are also present on `auric-base` and refer to missing `.grok/skills/og` content.
- Focused tests: PASS, 7/7. E2E isolated rerun: PASS, 1/1; perf is recorded under L1-8.
- `scripts/browser-smoke.mjs` against dev and production preview: pages returned HTTP 200 with no page errors or horizontal overflow; the script exited 2 for external Google Fonts / `grok.com` DNS failures, which also reproduce on `auric-base`.
- No new npm dependencies, paid APIs, or cloud services. Both moods and target widths are represented in the lane E2E matrix. Device haptics were not observed.
- Reporting requirement: FAIL. PR #27's description still says no implementation changes were made, although 18 paths changed, and it does not list each criterion with verdict/evidence.
- Reference/look grade: NEEDS-WORK / NOT FULLY VERIFIED. The candidate screenshots show the red strip in both moods (`shots/laser-demo.png`, `shots/laser-light.png`), but default equivalence for non-span reels was not independently proven.
