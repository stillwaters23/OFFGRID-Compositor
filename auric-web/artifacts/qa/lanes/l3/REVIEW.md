# Lane 3 QA review — PR #31

**Reviewed head:** `ff8f9e498f9d6687dccc7a825b6cb7abf8b31d5e` · **Required starting point:** L2 state-contract commit · **Verdict:** CHANGES-NEEDED

Independent review against issue #30. The refreshed PR head was fetched read-only and its current unit/build/E2E checks were rerun. No lane files were edited.

## Acceptance criteria

| Criterion | Verdict | Evidence |
|---|---|---|
| L3-1 | PASS | Current `l3-overflow.e2e.mjs` runs the 90-case size/layout/width/mood matrix and checks tile/control bounds, hit testing, tap-target size, and overflow. E2E passed. |
| L3-2 | PASS | `tests/l3-layout.test.ts` passes the size/width/layout table, clamp, column, and shrink-order checks. |
| L3-3 | FAIL | Independent 390px default-list screenshot comparison against `auric-base`: common area pixel delta **42.341%** (target ≤1%). Baseline and current PR captures: `shots/base-folder-list.png`, `shots/l3-folder-list.png`. |
| L3-4 | FAIL | Transform/opacity-only animation and reduced-motion checks pass, but the current-head Playwright run measured FLIP frame intervals **median 66.60ms / p95 66.80ms** (target ≤17.5/25ms); the test fails. |
| L3-5 | PASS | Current icon/etched E2E checks both moods, computed title contrast ≥4.5:1, at most two shadow layers with ≤4px blur, and no `background-clip:text`; E2E passed. |
| L3-6 | PASS | Current E2E iterates all module glyphs through flat/3D/etched, checks flat against its own baseline (≤1%), and tests 3D persistence. Independent captures: `shots/folder-3d.png`, `shots/folder-etched.png`. |
| L3-7 | PASS | E2E long-presses icon, chevron, and the remaining folder body zone; confirms size controls open, three haptics are recorded, folder does not open, and the timer-only size persists. |
| L3-8 | PASS | E2E long-presses the label, verifies rename input focus and selected text, then saves the new label. |
| L3-9 | PASS | E2E covers Enter, blur, Escape, empty-name refusal/message/focus, 25-character truncation, persistence, aria-label update, and reset-name action. |
| L3-10 | PASS | Unit tests cover label hit-zone boundary points, 499/500ms tap/long-press classification, and the 8px drag threshold. |
| L3-11 | PASS | Current snap-back E2E checks top/bottom/left/right at 320/390px, samples every animation frame for safe-area containment, and verifies settle under 500ms. |
| L3-12 | PASS | `tests/l3-physics.test.ts` passes sub-500ms monotonic spring convergence and safe-area clamp checks with 0px/44px simulated insets. |
| L3-13 | PASS | Current E2E reorders folders and verifies order after reload; unit hydration checks unknown/new module IDs append. |
| L3-14 | PASS | Current E2E verifies F2 rename, Shift+F10 size, and Enter open through named accessible controls. |
| L3-15 | FAIL | Current FLIP performance fails; CSS contains multiple off-grid values; full `npm test` and clean install also fail as documented below. |

## Integration and write-target audit

Issue #30 requires Lane 3 to start from L2's 2.0 commit and use its shared store fields/setters. PR #31 instead targets `auric-base` and persists folder preferences separately under `auric-folder-settings-v1`; the Lane 2 Zustand fields/setters are not present. This is an integration-contract **FAIL**, even though the sidecar preference tests pass.

Every changed path in PR #31 is on issue #30's permitted list. No deletions or out-of-scope paths were found.

| Changed path | Audit |
|---|---|
| `auric-web/screenshots/l3/icons-3d.png` | OK |
| `auric-web/screenshots/l3/icons-etched.png` | OK |
| `auric-web/src/components/auric/folder-grid.tsx` | OK |
| `auric-web/src/components/auric/folder-icons-3d.tsx` | OK |
| `auric-web/src/components/auric/folder-layout-sheet.tsx` | OK |
| `auric-web/src/components/auric/folder-rename.tsx` | OK |
| `auric-web/src/components/auric/folder-size-popover.tsx` | OK |
| `auric-web/src/components/auric/glass-folder.tsx` | OK |
| `auric-web/src/components/auric/session-shell.tsx` | OK — sheet mount only |
| `auric-web/src/lib/folder-gestures.ts` | OK |
| `auric-web/src/lib/folder-layout.ts` | OK |
| `auric-web/src/lib/folder-physics.ts` | OK |
| `auric-web/src/styles/folders.css` | OK |
| `auric-web/tests/e2e/l3-folders.e2e.mjs` | OK |
| `auric-web/tests/e2e/l3-overflow.e2e.mjs` | OK |
| `auric-web/tests/e2e/l3-snapback.e2e.mjs` | OK |
| `auric-web/tests/l3-gestures.test.ts` | OK |
| `auric-web/tests/l3-layout.test.ts` | OK |
| `auric-web/tests/l3-physics.test.ts` | OK |

## Global and command audit

- Current-head `npm run typecheck`: PASS. `npm run lint`: PASS with one pre-existing warning at `src/components/auric/termux-chrome.tsx:169`. `npm run build`: PASS.
- Current-head focused unit tests: PASS, 6/6.
- Current-head Playwright: **6 passed, 1 failed**. The failure is the FLIP performance test (median/p95 `66.60/66.80ms`). Overflow, long-press/rename/keyboard, icons/contrast, snap-back, and reorder tests passed.
- Full `npm test` on the lane snapshot: FAIL, 184/195 passed; 11 failures reference missing `.grok/skills/og` files/assets and reproduce on `auric-base`.
- `npm ci --ignore-scripts`: FAIL because the lockfile omits `lru-cache@11.5.3`; also reproduced on `auric-base`. Isolated validation used `npm install --ignore-scripts --package-lock=false`.
- 8dp audit: FAIL. Current `folders.css` adds values including a 44px menu width and font sizes 14, 11, 18, 22, and 12px; these are not 8px multiples and have no allowed half-step comment. 2px inset/outline treatments are not the reason for failure.
- Both moods and widths 320/360/390/768/1280 are independently covered by the overflow matrix. Reduced-motion layout and snap-back checks pass; no device haptic claim is made.
- No new npm dependencies, paid APIs, or cloud services.
- Reporting requirement: FAIL. The PR description records selected checks, but not every criterion with evidence; its prior local perf numbers differ from this independent current-head rerun.
- Browser-smoke dev/production: pages returned HTTP 200 with no page errors or overflow; the script exits 2 for external resource DNS failures also seen on baseline.
- Reference/look grade: **NEEDS-WORK**. The 3D and etched treatments are present (`shots/folder-3d.png`, `shots/folder-etched.png`), but the default-state screenshot misses its ≤1% target by a wide margin and the full reference match is not demonstrated.
