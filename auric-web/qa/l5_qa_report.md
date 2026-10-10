# Lane 5 integration QA

The first run is the section below and is unchanged. [Run 2](#run-2) is the rerun on the fixed lane heads. Run 2 verdict: **FAIL**.

## Run 1

**Verdict: FAIL**

The four lane heads combine and the wheel, laser, reel look, folders, and clicker sounds run together. The combined `npm test` gate is 1 failure and 0 skipped. Two Playwright end-to-end tests fail. Those three failures are the blockers.

Scratch integration (local only, not pushed): `d521a24d5215537c9483b0407e994ff275d6ef2e`

Built on `origin/auric-base` `e39aa55`, then merged in order:

| Lane | PR | Head merged |
|---|---|---|
| L1 | #27 | `b3708d33b78ab552f988480166fac5abde11e510` |
| L2 | #29 | `a79ba616d593ece0f30d6dfb8b4118317e40b45a` |
| L3 | #31 | `3036364dbdf640c604cd2a853f1f7cfc88042f9b` |
| L4 | #33 | `69d57b4f06f079cf93cc1b2ad773e9ddc332062b` |

## Lane verdicts

| Lane | Verdict | Blocking issue |
|---|---|---|
| L1 | FAIL | `tests/l1_acceptance.test.ts` forbids `src/styles/folders.css` and `src/styles/clicker.css`. L3 and L4 ship those files. `npm test` fails that assertion. |
| L2 | PASS | Unit tests, the reel-look Playwright test, and the live laser handoff passed. Settings screenshots move 4–7% against their own files because the beam shimmer is not frozen. |
| L3 | FAIL | Icon-style Playwright test waits for 15 folder names. The combined grid has 16 after L4 adds `clicker-sounds`. Default folder list versus `auric-base` is 2.5939% changed pixels (gate is 1%). |
| L4 | FAIL | Playwright waits for the button `clicker-sounds unlocked`. Lane 3 applies the stored name, so the button is `House Clicks unlocked`. |

## What works together

A browser probe on the integration dev server opened the renamed clicker tile, previewed `click-02` at 1346 Hz, and the timer wheel notch played that same frequency. The timer mounted two `.laser-strip` elements at L2's default `#FF0000`. Setting the laser to `#00E5FC` updated the live strip gradient. The reel-look settings witness rendered one beam and one hairline. Page errors: none.

FLIP layout gate from the passing L3 test: mean **15.830 ms**, median **16.700 ms**, p95 **16.700 ms**, max **16.800 ms**. Mean is under 16.7 ms and p95 is under 25 ms.

## Merge conflicts resolved in the scratch tree

1. `auric-web/src/components/auric/laser_strip.tsx` was an add/add. Lane 1 exports a measured wheel strip that takes `hostRef` and `bankRef`. Lane 2 exports a no-prop settings witness. The resolution keeps both: refs render the wheel strip, and a call with no props renders the witness. Archived at `qa/archive/laser_strip_add_add_conflict.tsx` and `qa/archive/laser_strip_resolution.tsx`.
2. `auric-web/scripts/test-aliases.mjs` differed by `await` on `module.register()`. This runtime is Node v22.14.0 and `registerHooks` is undefined, so the resolution keeps Lane 4's `await`. Archived at `qa/archive/test_aliases_content_conflict.mjs` and `qa/archive/test_aliases_resolution.mjs`.

`package-lock.json` auto-merged. `npm ci --ignore-scripts` on the scratch tree exited 0. The same command on `auric-base` still fails: `Missing: lru-cache@11.5.3 from lock file`.

## Check results

| Check | Exit | Result |
|---|---|---|
| `npm ci --ignore-scripts` on the scratch tree | 0 | PASS |
| `npm ci --ignore-scripts` on `auric-base` | 1 | FAIL, pre-existing lockfile gap, fixed by the lane lockfiles |
| `npm run typecheck` | 0 | PASS |
| `npm run lint` | 0 | PASS, one existing warning in `termux-chrome.tsx:169` |
| `npm run build` | 0 | PASS |
| `npm test` | 1 | FAIL, 296 tests, 295 pass, 1 fail, 0 skipped |
| Lane 2/3/4 unit tests (not wired into `npm test`) | 0 | PASS, 34 tests, 0 fail, 0 skipped |
| `node --test scripts/l4_click_voice.test.mjs` | 0 | PASS, 1 test, 0 skipped |
| `node tests/e2e/l1_reels.e2e.mjs` | 0 | PASS |
| `AURIC_E2E_URL=... node --test tests/e2e/l2_reel_look.e2e.mjs` | 0 | PASS, 1 test, 0 skipped |
| `L3_E2E=1 node --test tests/e2e/l3_*.e2e.mjs` | 1 | FAIL, 7 tests, 6 pass, 1 fail, 0 skipped |
| `node --test tests/e2e/l4_clicker_sounds.e2e.mjs` | 1 | FAIL, 1 test, 0 skipped |
| Integration probe | 0 | PASS |
| FLIP 60fps gate | n/a (inside the passing L3 test) | PASS, mean 15.830 ms, p95 16.700 ms |
| Pixel diff versus lane screenshot files | n/a | L1 strip and light 0%. L1 demo 0.1884%. L2 settings 4.19–6.93% (shimmer). L3 icon shots grew 1469px to 1567px. L4 shot was not recaptured. |
| Pixel diff versus `auric-base` default captures | n/a | Folder list 2.5939%. Timer 25.0149%. |

`npm test` runner totals, in order: scripts 199/199, app-data and auth 55/55, `test:auric` 31/31, `test:l1` 10/11. The single failure is `new L1 files use underscores and the empty lane hooks are archived` at `tests/l1_acceptance.test.ts:48`: `styles/folders.css` exists (`true !== false`). The same assertion also lists `styles/clicker.css`.

Lane 2 and Lane 3 Playwright tests skip unless `AURIC_E2E_URL` and `L3_E2E` are set. Both were set, so those tests ran. Zero tests were skipped.

## Issues

### L1 — blocking

`tests/l1_acceptance.test.ts` requires these paths to be absent:

- `src/styles/folders.css`
- `src/styles/clicker.css`

Lane 1's archive note says Lanes 2, 3, and 4 can add their own stylesheets. Lane 3 added `src/styles/folders.css`. Lane 4 added `src/styles/clicker.css`. On Lane 1 alone the assertion passes. In the combined tree `npm test` fails. The wheel and laser end-to-end script still passed, including median and p95 frame intervals of 16.7 ms at normal and 4× throttle.

### L3 — blocking

`tests/e2e/l3_folders.e2e.mjs` test `L3 icon styles cover every glyph and etched text stays matte and readable` times out at line 360. It waits until `.tx-folder-grid .tx-folder-name` equals a hardcoded list of 15 ids. The probe counted 16 names. The extra name is `clicker-sounds` / `House Clicks`, added by Lane 4. The flat in-test pixel compare earlier in that test had already passed (the failure is the later wait). The FLIP test in the same file passed.

Committed `screenshots/l3/icons_3d.png` and `icons_etched.png` are 324×1469. The integration recapture is 324×1567, one extra row.

Default folder list at 390×844 versus `auric-base`: **2.5939%** changed pixels (8538/329160). Lane 3's gate for that picture is 1%.

### L4 — blocking

`tests/e2e/l4_clicker_sounds.e2e.mjs` times out on `getByRole('button', { name: 'clicker-sounds unlocked' })`. The init script stores `folderNames["clicker-sounds"] = "House Clicks"` under `auric-folder-settings-v1`. Lane 3's grid uses that name in the accessible label, so the button is `House Clicks unlocked`. On Lane 4 alone the older grid ignores that name and the selector matches. The probe clicked `House Clicks unlocked` and the rest of the clicker path worked: etched style, size 72, preview frequency 1346, wheel notch frequency 1346, `reelSound=click-02`.

### L2 — not blocking

Reel-look unit coverage is inside the 34 passing lane unit tests. The Playwright test passed with motion median 16.70 ms and p95 16.70 ms, and apply landed `#AABBCC` on the beam in 1 frame. The probe showed PaletteApplier driving the Lane 1 strip.

Regenerated settings screenshots versus the committed Lane 2 files:

- `beam_on_dark.png` 6.9252%
- `laser_settings.png` 6.8316%
- `number_contrast.png` 4.1928%

The red overlay sits on the breathing beam and on text edges. The beam animation is not frozen, so those files are not a stable pixel baseline.

Timer versus `auric-base` at 390×844 is 25.0149% (82339/329160). `auric-base` has zero `.laser-strip` elements. The integration timer has two, and the changed rows concentrate in the reel block (y 288–579). That is the Lane 1 strip landing on the timer, not a Lane 2 settings failure.

## Raw output

### npm ci on the scratch tree

```
Command: cd /tmp/auric_l5_scratch/auric-web && npm ci --ignore-scripts
Exit: 0

npm warn deprecated recharts@2.15.4: 1.x and 2.x branches are no longer active. Bump to Recharts v3 to receive latest features and bugfixes. See https://github.com/recharts/recharts/wiki/3.0-migration-guide
npm warn deprecated eslint@9.39.5: This version is no longer supported. Please see https://eslint.org/version-support for other options.

added 425 packages, and audited 426 packages in 5s

94 packages are looking for funding
  run `npm fund` for details

2 high severity vulnerabilities

To address all issues, run:
  npm audit fix

Run `npm audit` for details.
npm notice
npm notice New major version of npm available! 10.9.7 -> 12.2.0
npm notice Changelog: https://github.com/npm/cli/releases/tag/v12.2.0
npm notice To update run: npm install -g npm@12.2.0
npm notice
```

### npm ci on auric-base

```
npm error code EUSAGE
npm error
npm error `npm ci` can only install packages when your package.json and package-lock.json or npm-shrinkwrap.json are in sync. Please update your lock file with `npm install` before continuing.
npm error
npm error Missing: lru-cache@11.5.3 from lock file
npm error
npm error Clean install a project
npm error
npm error Usage:
npm error npm ci
npm error
npm error Options:
npm error [--install-strategy <hoisted|nested|shallow|linked>] [--legacy-bundling]
npm error [--global-style] [--omit <dev|optional|peer> [--omit <dev|optional|peer> ...]]
npm error [--include <prod|dev|optional|peer> [--include <prod|dev|optional|peer> ...]]
npm error [--strict-peer-deps] [--foreground-scripts] [--ignore-scripts] [--no-audit]
npm error [--no-bin-links] [--no-fund] [--dry-run]
npm error [-w|--workspace <workspace-name> [-w|--workspace <workspace-name> ...]]
npm error [-ws|--workspaces] [--include-workspace-root] [--install-links]
npm error
npm error aliases: clean-install, ic, install-clean, isntall-clean
npm error
npm error Run "npm help ci" for more info
npm error A complete log of this run can be found in: /home/ubuntu/.npm/_logs/2026-10-10T13_26_53_137Z-debug-0.log
```

### npm run typecheck

```

> typecheck
> tsc --noEmit
```

### npm run lint

```

> lint
> eslint .


/tmp/auric_l5_scratch/auric-web/src/components/auric/termux-chrome.tsx
  169:17  warning  Fast refresh only works when a file only exports components. Use a new file to share constants or functions between components  react-refresh/only-export-components

✖ 1 problem (0 errors, 1 warning)
```

### npm run build

```

> build
> node scripts/with-app-env.mjs vite build && npm run db:migrate

[nitro:vercel] ℹ Using nodejs22.x runtime.
[nitro:vercel] ℹ Using web entry format.
vite v8.2.2 building client environment for production...
transforming...
✓ 2543 modules transformed.
rendering chunks...
computing gzip size...
.vercel/output/static/assets/routes-BRfX0JUo.css      16.71 kB │ gzip:   3.56 kB
.vercel/output/static/assets/styles-VzALeqHu.css     108.21 kB │ gzip:  20.71 kB
.vercel/output/static/assets/laser_demo-C3V3nE6f.js    0.37 kB │ gzip:   0.28 kB
.vercel/output/static/assets/index-BXP4NBXz.js       402.08 kB │ gzip: 125.71 kB
.vercel/output/static/assets/routes-UY1lxRu1.js      636.33 kB │ gzip: 180.75 kB

✓ built in 694ms
[plugin builtin:vite-reporter] 
(!) Some chunks are larger than 500 kB after minification. Consider:
- Using dynamic import() to code-split the application
- Use build.rolldownOptions.output.codeSplitting to improve chunking: https://rolldown.rs/reference/OutputOptions.codeSplitting
- Adjust chunk size limit for this warning via build.chunkSizeWarningLimit.
vite v8.2.2 building ssr environment for production...
transforming...
✓ 138 modules transformed.
rendering chunks...
computing gzip size...
node_modules/.nitro/vite/services/ssr/assets/routes-BRfX0JUo.css                      16.71 kB │ gzip:  3.56 kB
node_modules/.nitro/vite/services/ssr/assets/styles-VzALeqHu.css                     108.21 kB │ gzip: 20.71 kB
node_modules/.nitro/vite/services/ssr/assets/start-5Z2QO8AU.js                         0.14 kB │ gzip:  0.13 kB
node_modules/.nitro/vite/services/ssr/assets/empty-plugin-adapters-D9UWiqvJ.js         0.22 kB │ gzip:  0.16 kB
node_modules/.nitro/vite/services/ssr/assets/laser_demo-9E0Ls1JT.js                    0.57 kB │ gzip:  0.36 kB
node_modules/.nitro/vite/services/ssr/assets/_tanstack-start-manifest_v-rM6qflNp.js    0.73 kB │ gzip:  0.36 kB
node_modules/.nitro/vite/services/ssr/assets/router-CFk4ky3a.js                       15.16 kB │ gzip:  5.06 kB
node_modules/.nitro/vite/services/ssr/index.js                                        62.55 kB │ gzip: 15.91 kB
node_modules/.nitro/vite/services/ssr/assets/routes-C8NFJC6R.js                      333.81 kB │ gzip: 82.36 kB

✓ built in 232ms

[nitro] ◐ Building [Nitro] (preset: vercel, compatibility: 2026-10-10)
[nitro] ✔ Generated public .vercel/output/static
vite v8.2.2 building nitro environment for production...
transforming...
✓ 2505 modules transformed.
ℹ Tracing dependencies:
- tslib (2.8.1)
✔ Traced 1 dependencies (4 files) in 37ms.
ℹ Ensure your production environment matches the builder OS and architecture (linux-x64) to avoid native module issues.
rendering chunks...
computing gzip size...
.vercel/output/functions/__server.func/_ssr/start-5Z2QO8AU.mjs                         0.14 kB │ gzip:   0.13 kB
.vercel/output/functions/__server.func/_ssr/empty-plugin-adapters-D9UWiqvJ.mjs         0.23 kB │ gzip:   0.16 kB
.vercel/output/functions/__server.func/_libs/clsx.mjs                                  0.55 kB │ gzip:   0.31 kB
.vercel/output/functions/__server.func/_tanstack-start-manifest_v-rM6qflNp.mjs         0.79 kB │ gzip:   0.39 kB
.vercel/output/functions/__server.func/_ssr/laser_demo-9E0Ls1JT.mjs                    0.87 kB │ gzip:   0.49 kB
.vercel/output/functions/__server.func/_chunks/ssr-renderer.mjs                        0.96 kB │ gzip:   0.49 kB
.vercel/output/functions/__server.func/_runtime.mjs                                    1.58 kB │ gzip:   0.74 kB
.vercel/output/functions/__server.func/_libs/react-is.mjs                              1.67 kB │ gzip:   0.65 kB
.vercel/output/functions/__server.func/_libs/radix-ui__primitive.mjs                   1.83 kB │ gzip:   0.72 kB
.vercel/output/functions/__server.func/_libs/prop-types.mjs                            2.27 kB │ gzip:   0.84 kB
.vercel/output/functions/__server.func/_libs/d3-path.mjs                               3.25 kB │ gzip:   1.16 kB
.vercel/output/functions/__server.func/_libs/d3-interpolate.mjs                        5.03 kB │ gzip:   1.61 kB
.vercel/output/functions/__server.func/_libs/radix-ui__react-context+react.mjs         5.74 kB │ gzip:   1.67 kB
.vercel/output/functions/__server.func/_libs/zustand.mjs                               6.74 kB │ gzip:   2.09 kB
.vercel/output/functions/__server.func/_libs/d3-array.mjs                              8.06 kB │ gzip:   2.25 kB
.vercel/output/functions/__server.func/_libs/eventemitter3.mjs                         8.31 kB │ gzip:   2.05 kB
.vercel/output/functions/__server.func/_libs/h3-v2+rou3.mjs                            9.83 kB │ gzip:   3.05 kB
.vercel/output/functions/__server.func/_libs/d3-color.mjs                             10.38 kB │ gzip:   3.57 kB
.vercel/output/functions/__server.func/_libs/d3-format.mjs                            10.56 kB │ gzip:   3.02 kB
.vercel/output/functions/__server.func/_libs/lucide-react.mjs                         10.95 kB │ gzip:   2.86 kB
.vercel/output/functions/__server.func/_libs/tanstack__history.mjs                    12.26 kB │ gzip:   3.48 kB
.vercel/output/functions/__server.func/_ssr/router-CFk4ky3a.mjs                       15.30 kB │ gzip:   5.12 kB
.vercel/output/functions/__server.func/_libs/fast-equals.mjs                          15.91 kB │ gzip:   4.13 kB
.vercel/output/functions/__server.func/_libs/@radix-ui/react-compose-refs+[...].mjs   16.79 kB │ gzip:   4.66 kB
.vercel/output/functions/__server.func/_libs/h3+rou3+srvx.mjs                         22.57 kB │ gzip:   6.12 kB
.vercel/output/functions/__server.func/_libs/decimal.js-light.mjs                     23.31 kB │ gzip:   6.89 kB
.vercel/output/functions/__server.func/_libs/d3-shape.mjs                             24.60 kB │ gzip:   5.02 kB
.vercel/output/functions/__server.func/index.mjs                                      33.28 kB │ gzip:  10.38 kB
.vercel/output/functions/__server.func/_libs/react-smooth.mjs                         37.86 kB │ gzip:   8.15 kB
.vercel/output/functions/__server.func/_libs/d3-scale+[...].mjs                       58.23 kB │ gzip:  12.12 kB
.vercel/output/functions/__server.func/_ssr/ssr.mjs                                   60.95 kB │ gzip:  15.86 kB
.vercel/output/functions/__server.func/_libs/@radix-ui/react-dialog+[...].mjs         85.32 kB │ gzip:  20.04 kB
.vercel/output/functions/__server.func/_libs/tailwind-merge.mjs                       88.13 kB │ gzip:  15.66 kB
.vercel/output/functions/__server.func/_libs/@tanstack/router-core+[...].mjs         123.11 kB │ gzip:  25.92 kB
.vercel/output/functions/__server.func/_libs/zod.mjs                                 159.58 kB │ gzip:  34.87 kB
.vercel/output/functions/__server.func/_libs/lodash.mjs                              161.64 kB │ gzip:  29.40 kB
.vercel/output/functions/__server.func/_ssr/routes-C8NFJC6R.mjs                      356.40 kB │ gzip:  82.67 kB
.vercel/output/functions/__server.func/_libs/recharts+[...].mjs                      476.34 kB │ gzip:  89.79 kB
.vercel/output/functions/__server.func/_libs/@tanstack/react-router+[...].mjs        653.45 kB │ gzip: 137.30 kB

✓ built in 387ms
ℹ Generated .vercel/output/nitro.json

[nitro] ✔ You can preview this build using npx vite preview
[nitro] ✔ You can deploy this build using npx nitro deploy --prebuilt

> db:migrate
> node scripts/migrate.mjs

[migrate] DATABASE_URL not set — skipping (the PGLite fallback migrates itself).
```

### npm test

```

> test
> node --test 'scripts/**/*.test.mjs' && node --experimental-strip-types --test src/lib/app-data/app-data.test.ts src/lib/app-data/readiness-schedule.test.ts src/lib/auth/gate-identity.test.ts src/lib/auth/sign-in-gate.test.ts && npm run test:auric && npm run test:l1

TAP version 13
# Subtest: non-canvas app with no custom card gets a soft BRAND NOTE (utility exception)
ok 1 - non-canvas app with no custom card gets a soft BRAND NOTE (utility exception)
  ---
  duration_ms: 1.484331
  ...
# Subtest: non-canvas app with a compliant card is silent
ok 2 - non-canvas app with a compliant card is silent
  ---
  duration_ms: 2.172838
  ...
# Subtest: card file without site.json card=custom warns
ok 3 - card file without site.json card=custom warns
  ---
  duration_ms: 5.591883
  ...
# Subtest: card file with missing site.json warns for card=custom
ok 4 - card file with missing site.json warns for card=custom
  ---
  duration_ms: 5.120115
  ...
# Subtest: card file with invalid site.json warns for card=custom
ok 5 - card file with invalid site.json warns for card=custom
  ---
  duration_ms: 0.856292
  ...
# Subtest: oversized card warns for non-canvas apps too
ok 6 - oversized card warns for non-canvas apps too
  ---
  duration_ms: 0.971416
  ...
# Subtest: canvas app with no card warns 'missing' and missing og:type
ok 7 - canvas app with no card warns 'missing' and missing og:type
  ---
  duration_ms: 0.485294
  ...
# Subtest: canvas card without type still warns for og:type
ok 8 - canvas card without type still warns for og:type
  ---
  duration_ms: 0.62402
  ...
# Subtest: oversized card warns on the scraper budget (jpg and legacy png)
ok 9 - oversized card warns on the scraper budget (jpg and legacy png)
  ---
  duration_ms: 2.534104
  ...
# Subtest: compliant game (custom card + site.json type + x-banner) is silent
ok 10 - compliant game (custom card + site.json type + x-banner) is silent
  ---
  duration_ms: 0.995499
  ...
# Subtest: __root.tsx og:type no longer satisfies the canvas gate
ok 11 - __root.tsx og:type no longer satisfies the canvas gate
  ---
  duration_ms: 1.255702
  ...
# Subtest: oversized x-banner warns on the same scraper budget as og.jpg
ok 12 - oversized x-banner warns on the same scraper budget as og.jpg
  ---
  duration_ms: 1.260712
  ...
# Subtest: legacy png + site.json type still needs the X feed card
ok 13 - legacy png + site.json type still needs the X feed card
  ---
  duration_ms: 6.080337
  ...
# Subtest: siteDeclaresOgTypeGame reads the site contract
ok 14 - siteDeclaresOgTypeGame reads the site contract
  ---
  duration_ms: 0.093048
  ...
# Subtest: a fresh og-pending marker silences the whole gate view
ok 15 - a fresh og-pending marker silences the whole gate view
  ---
  duration_ms: 0.332614
  ...
# Subtest: a stale og-pending marker is ignored and the warnings return
ok 16 - a stale og-pending marker is ignored and the warnings return
  ---
  duration_ms: 0.284206
  ...
# Subtest: the marker goes stale on the clock, not on the file
ok 17 - the marker goes stale on the clock, not on the file
  ---
  duration_ms: 0.335036
  ...
# Subtest: no marker means nothing is in flight
ok 18 - no marker means nothing is in flight
  ---
  duration_ms: 0.216912
  ...
# Subtest: parseBrandCheckArgs takes --game, --placeholder-ok and --root, and rejects the rest
ok 19 - parseBrandCheckArgs takes --game, --placeholder-ok and --root, and rejects the rest
  ---
  duration_ms: 0.208228
  ...
# Subtest: cli: a game missing its card reports JSON and exits 1
ok 20 - cli: a game missing its card reports JSON and exits 1
  ---
  duration_ms: 42.289169
  ...
# Subtest: cli: a compliant game passes
ok 21 - cli: a compliant game passes
  ---
  duration_ms: 38.451435
  ...
# Subtest: cli: the task's own marker is reported, not honoured
ok 22 - cli: the task's own marker is reported, not honoured
  ---
  duration_ms: 117.729034
  ...
# Subtest: cli: a bad argument fails with JSON on stderr
ok 23 - cli: a bad argument fails with JSON on stderr
  ---
  duration_ms: 53.298051
  ...
# Subtest: cli: a non-game with no card fails too — the pass exists to produce one
ok 24 - cli: a non-game with no card fails too — the pass exists to produce one
  ---
  duration_ms: 49.427154
  ...
# Subtest: cli: --placeholder-ok is how a plain-utility pass reports no card as expected
ok 25 - cli: --placeholder-ok is how a plain-utility pass reports no card as expected
  ---
  duration_ms: 55.244053
  ...
# Subtest: cli: a non-game with a compliant card passes
ok 26 - cli: a non-game with a compliant card passes
  ---
  duration_ms: 32.659353
  ...
# Subtest: SKILL.md and AGENTS.md name the marker path and bound this script uses
ok 27 - SKILL.md and AGENTS.md name the marker path and bound this script uses
  ---
  duration_ms: 0.37238
  ...
# Subtest: the sections that own the brand-task prohibition never affirm a wait
ok 28 - the sections that own the brand-task prohibition never affirm a wait
  ---
  duration_ms: 0.68338
  ...
# Subtest: SKILL.md tells the pass to self-check with the flag this CLI accepts
ok 29 - SKILL.md tells the pass to self-check with the flag this CLI accepts
  ---
  duration_ms: 1.743251
  ...
# Subtest: the shipped brand identity is Auric with the three launcher icons
ok 30 - the shipped brand identity is Auric with the three launcher icons
  ---
  duration_ms: 2.236453
  ...
# Subtest: the manifest and install page use the brand name, not the host slug
ok 31 - the manifest and install page use the brand name, not the host slug
  ---
  duration_ms: 0.504231
  ...
# Subtest: a workspace without brand.json falls back to the host slug
ok 32 - a workspace without brand.json falls back to the host slug
  ---
  duration_ms: 0.369237
  ...
# Subtest: normalizes whitespace before hashing
ok 33 - normalizes whitespace before hashing
  ---
  duration_ms: 1.228914
  ...
# Subtest: nullish and blank body text normalize to the empty string
ok 34 - nullish and blank body text normalize to the empty string
  ---
  duration_ms: 0.152869
  ...
# Subtest: body text prefix is normalized and capped
ok 35 - body text prefix is normalized and capped
  ---
  duration_ms: 0.098556
  ...
# Subtest: identical runs do not diverge
ok 36 - identical runs do not diverge
  ---
  duration_ms: 1.526385
  ...
# Subtest: status change diverges
ok 37 - status change diverges
  ---
  duration_ms: 0.273293
  ...
# Subtest: title change diverges
ok 38 - title change diverges
  ---
  duration_ms: 0.103785
  ...
# Subtest: title comparison is skipped when either side lacks one
ok 39 - title comparison is skipped when either side lacks one
  ---
  duration_ms: 0.134602
  ...
# Subtest: canvas disappearing diverges; canvas appearing does not
ok 40 - canvas disappearing diverges; canvas appearing does not
  ---
  duration_ms: 0.097792
  ...
# Subtest: horizontal overflow appearing diverges; clearing does not
ok 41 - horizontal overflow appearing diverges; clearing does not
  ---
  duration_ms: 0.206973
  ...
# Subtest: new console errors diverge
ok 42 - new console errors diverge
  ---
  duration_ms: 0.302705
  ...
# Subtest: errors already present in the baseline do not re-diverge
ok 43 - errors already present in the baseline do not re-diverge
  ---
  duration_ms: 0.115676
  ...
# Subtest: body text collapse diverges
ok 44 - body text collapse diverges
  ---
  duration_ms: 0.067154
  ...
# Subtest: body text dropping to zero reports collapse
ok 45 - body text dropping to zero reports collapse
  ---
  duration_ms: 0.051666
  ...
# Subtest: zero-length baseline reports growth via the hash branch, not collapse
ok 46 - zero-length baseline reports growth via the hash branch, not collapse
  ---
  duration_ms: 0.046504
  ...
# Subtest: hash mismatch with a trivial length delta and same page start is tolerated
ok 47 - hash mismatch with a trivial length delta and same page start is tolerated
  ---
  duration_ms: 0.037576
  ...
# Subtest: hash mismatch with a trivial length delta but changed page start diverges
ok 48 - hash mismatch with a trivial length delta but changed page start diverges
  ---
  duration_ms: 0.044056
  ...
# Subtest: prefix check is skipped when either side lacks a prefix
ok 49 - prefix check is skipped when either side lacks a prefix
  ---
  duration_ms: 0.077658
  ...
# Subtest: length-delta tolerance boundary: 50 tolerated, 51 diverges (500-char baseline)
ok 50 - length-delta tolerance boundary: 50 tolerated, 51 diverges (500-char baseline)
  ---
  duration_ms: 0.052133
  ...
# Subtest: length-delta floor dominates small baselines: 20 tolerated, 21 diverges (100-char baseline)
ok 51 - length-delta floor dominates small baselines: 20 tolerated, 21 diverges (100-char baseline)
  ---
  duration_ms: 0.043695
  ...
# Subtest: collapse boundary: exactly half is 'changed', one below is 'collapsed'
ok 52 - collapse boundary: exactly half is 'changed', one below is 'collapsed'
  ---
  duration_ms: 0.045416
  ...
# Subtest: missing baseline viewport diverges
ok 53 - missing baseline viewport diverges
  ---
  duration_ms: 0.042508
  ...
# Subtest: empty or null baseline diverges for every viewport
ok 54 - empty or null baseline diverges for every viewport
  ---
  duration_ms: 0.053864
  ...
# Subtest: degenerate current verdict fails closed
ok 55 - degenerate current verdict fails closed
  ---
  duration_ms: 0.124506
  ...
# Subtest: multiple signals across viewports all accumulate
ok 56 - multiple signals across viewports all accumulate
  ---
  duration_ms: 0.059079
  ...
# Subtest: baselineComparison round-trips a valid baseline
ok 57 - baselineComparison round-trips a valid baseline
  ---
  duration_ms: 0.084357
  ...
# Subtest: baselineComparison fails closed on malformed or wrong-shape baselines
ok 58 - baselineComparison fails closed on malformed or wrong-shape baselines
  ---
  duration_ms: 0.239118
  ...
# Subtest: parseSmokeArgs defaults
ok 59 - parseSmokeArgs defaults
  ---
  duration_ms: 0.111537
  ...
# Subtest: parseSmokeArgs consumes --baseline without shifting positionals
ok 60 - parseSmokeArgs consumes --baseline without shifting positionals
  ---
  duration_ms: 0.049646
  ...
# Subtest: parseSmokeArgs rejects a trailing --baseline flag
ok 61 - parseSmokeArgs rejects a trailing --baseline flag
  ---
  duration_ms: 0.042241
  ...
# Subtest: parseSmokeArgs supports the --baseline= form
ok 62 - parseSmokeArgs supports the --baseline= form
  ---
  duration_ms: 0.041417
  ...
# Subtest: parseSmokeArgs rejects unknown flags instead of treating them as positionals
ok 63 - parseSmokeArgs rejects unknown flags instead of treating them as positionals
  ---
  duration_ms: 0.079812
  ...
# Subtest: parseSmokeArgs env fallback and flag precedence
ok 64 - parseSmokeArgs env fallback and flag precedence
  ---
  duration_ms: 0.047253
  ...
# Subtest: derivedPaths handles .png, .PNG, and extensionless inputs
ok 65 - derivedPaths handles .png, .PNG, and extensionless inputs
  ---
  duration_ms: 0.110854
  ...
# Subtest: exitCodeFor: clean run exits 0
ok 66 - exitCodeFor: clean run exits 0
  ---
  duration_ms: 0.075833
  ...
# Subtest: exitCodeFor: errors in any single viewport exit 2
ok 67 - exitCodeFor: errors in any single viewport exit 2
  ---
  duration_ms: 0.041604
  ...
# Subtest: exitCodeFor: navigation failure in any viewport exits 1, beating errors
ok 68 - exitCodeFor: navigation failure in any viewport exits 1, beating errors
  ---
  duration_ms: 0.04226
  ...
# Subtest: exitCodeFor: 4xx boundary and missing status
ok 69 - exitCodeFor: 4xx boundary and missing status
  ---
  duration_ms: 0.04033
  ...
# Subtest: exitCodeFor: no viewport data is a failure
ok 70 - exitCodeFor: no viewport data is a failure
  ---
  duration_ms: 0.717796
  ...
# Subtest: browser-smoke wires the guard and verdict helpers
ok 71 - browser-smoke wires the guard and verdict helpers
  ---
  duration_ms: 0.264498
  ...
# Subtest: browser-smoke file I/O only touches guarded paths
ok 72 - browser-smoke file I/O only touches guarded paths
  ---
  duration_ms: 0.281152
  ...
# Subtest: the flag predicate matches src/lib/auth
ok 73 - the flag predicate matches src/lib/auth
  ---
  duration_ms: 0.63518
  ...
# Subtest: reads the value a live dev server resolved
ok 74 - reads the value a live dev server resolved
  ---
  duration_ms: 0.174906
  ...
# Subtest: a server started without the flag reads as sign-in on
ok 75 - a server started without the flag reads as sign-in on
  ---
  duration_ms: 0.093865
  ...
# Subtest: agreement passes
ok 76 - agreement passes
  ---
  duration_ms: 0.161762
  ...
# Subtest: divergence fails in either direction
ok 77 - divergence fails in either direction
  ---
  duration_ms: 0.683884
  ...
# Subtest: an unobservable dev server is indeterminate, not agreement
ok 78 - an unobservable dev server is indeterminate, not agreement
  ---
  duration_ms: 0.083531
  ...
# Subtest: a dev server that cannot be reached probes as null
ok 79 - a dev server that cannot be reached probes as null
  ---
  duration_ms: 0.182801
  ...
# Subtest: a server without the endpoint probes as null, not as agreement
ok 80 - a server without the endpoint probes as null, not as agreement
  ---
  duration_ms: 0.145479
  ...
# Subtest: only a divergence warns the smoke verdict
ok 81 - only a divergence warns the smoke verdict
  ---
  duration_ms: 0.725512
  ...
# Subtest: the build side resolves the template's shipped app-env
ok 82 - the build side resolves the template's shipped app-env
  ---
  duration_ms: 0.484408
  ...
# Subtest: the CLI reports rather than silently passing when run via a symlink
ok 83 - the CLI reports rather than silently passing when run via a symlink
  ---
  duration_ms: 98.549424
  ...
# Subtest: injects before </head>
ok 84 - injects before </head>
  ---
  duration_ms: 2.421781
  ...
# Subtest: injects the extensions script without a project id
ok 85 - injects the extensions script without a project id
  ---
  duration_ms: 0.286485
  ...
# Subtest: injects project id on the script and meta when provided
ok 86 - injects project id on the script and meta when provided
  ---
  duration_ms: 0.179576
  ...
# Subtest: does not duplicate grok:app_id
ok 87 - does not duplicate grok:app_id
  ---
  duration_ms: 0.383602
  ...
# Subtest: omits x:creator tags without both creator values
ok 88 - omits x:creator tags without both creator values
  ---
  duration_ms: 0.48113
  ...
# Subtest: injects x:creator tags when both creator values are set
ok 89 - injects x:creator tags when both creator values are set
  ---
  duration_ms: 0.211064
  ...
# Subtest: escapes x:creator values
ok 90 - escapes x:creator values
  ---
  duration_ms: 0.081277
  ...
# Subtest: does not duplicate x:creator tags
ok 91 - does not duplicate x:creator tags
  ---
  duration_ms: 0.291061
  ...
# Subtest: platform chrome overwrites share-card metas and always sets og:title
ok 92 - platform chrome overwrites share-card metas and always sets og:title
  ---
  duration_ms: 0.295563
  ...
# Subtest: does not duplicate twitter:card or og:title
ok 93 - does not duplicate twitter:card or og:title
  ---
  duration_ms: 0.406144
  ...
# Subtest: a baked site.image is treated as a custom card
ok 94 - a baked site.image is treated as a custom card
  ---
  duration_ms: 0.450591
  ...
# Subtest: baked identity does not need a workspace filesystem
ok 95 - baked identity does not need a workspace filesystem
  ---
  duration_ms: 0.268607
  ...
# Subtest: a public card file wins over a baked site without card=custom
ok 96 - a public card file wins over a baked site without card=custom
  ---
  duration_ms: 0.299719
  ...
# Subtest: public/og.png wins when jpg is absent
ok 97 - public/og.png wins when jpg is absent
  ---
  duration_ms: 3.429519
  ...
# Subtest: resolveOgCardAsset: disk file, then bake, then empty (placeholder)
ok 98 - resolveOgCardAsset: disk file, then bake, then empty (placeholder)
  ---
  duration_ms: 0.344353
  ...
# Subtest: snapshotOgIdentity stamps card=custom from a public card file
ok 99 - snapshotOgIdentity stamps card=custom from a public card file
  ---
  duration_ms: 0.27497
  ...
# Subtest: snapshotOgIdentity stamps banner from public/x-banner.jpg
ok 100 - snapshotOgIdentity stamps banner from public/x-banner.jpg
  ---
  duration_ms: 0.238828
  ...
# Subtest: emits x:game:image for a public host when site.banner is set
ok 101 - emits x:game:image for a public host when site.banner is set
  ---
  duration_ms: 0.179683
  ...
# Subtest: does not emit x:game:image without a public host or banner
ok 102 - does not emit x:game:image without a public host or banner
  ---
  duration_ms: 0.162841
  ...
# Subtest: site title Grok App is a real name, not a sentinel
ok 103 - site title Grok App is a real name, not a sentinel
  ---
  duration_ms: 0.184831
  ...
# Subtest: published grok.me slug is still a title fallback
ok 104 - published grok.me slug is still a title fallback
  ---
  duration_ms: 0.245862
  ...
# Subtest: rejects Vercel system hosts as og:image origins
ok 105 - rejects Vercel system hosts as og:image origins
  ---
  duration_ms: 0.084971
  ...
# Subtest: published VITE_PUBLIC_HOSTNAME wins over request Host for og:image
ok 106 - published VITE_PUBLIC_HOSTNAME wins over request Host for og:image
  ---
  duration_ms: 0.199817
  ...
# Subtest: vercel Host without a public hostname emits no og:image
ok 107 - vercel Host without a public hostname emits no og:image
  ---
  duration_ms: 0.090269
  ...
# Subtest: emits og:image for a public host and prefers a custom card
ok 108 - emits og:image for a public host and prefers a custom card
  ---
  duration_ms: 0.132663
  ...
# Subtest: placeholder og:image appends site.color when it is 6-digit hex
ok 109 - placeholder og:image appends site.color when it is 6-digit hex
  ---
  duration_ms: 0.135532
  ...
# Subtest: document title entities are not double-escaped on og:title
ok 110 - document title entities are not double-escaped on og:title
  ---
  duration_ms: 0.062372
  ...
# Subtest: site.json title wins over the host slug
ok 111 - site.json title wins over the host slug
  ---
  duration_ms: 0.061227
  ...
# Subtest: injects into documents with no head element
ok 112 - injects into documents with no head element
  ---
  duration_ms: 0.176389
  ...
# Subtest: streaming injector matches </HEAD> case-insensitively
ok 113 - streaming injector matches </HEAD> case-insensitively
  ---
  duration_ms: 0.336217
  ...
# Subtest: does not duplicate the extensions script
ok 114 - does not duplicate the extensions script
  ---
  duration_ms: 0.12164
  ...
# Subtest: is idempotent
ok 115 - is idempotent
  ---
  duration_ms: 0.073687
  ...
# Subtest: uses the app name in the injected title tag
ok 116 - uses the app name in the injected title tag
  ---
  duration_ms: 0.052361
  ...
# Subtest: streaming injector handles </head> split across chunks
ok 117 - streaming injector handles </head> split across chunks
  ---
  duration_ms: 0.237455
  ...
# Subtest: streaming injector passes post-head chunks through untouched
ok 118 - streaming injector passes post-head chunks through untouched
  ---
  duration_ms: 0.137256
  ...
# Subtest: streaming injector falls back when no </head> is seen
ok 119 - streaming injector falls back when no </head> is seen
  ---
  duration_ms: 0.092386
  ...
# Subtest: detects install query
ok 120 - detects install query
  ---
  duration_ms: 0.171846
  ...
# Subtest: filters non-document paths
ok 121 - filters non-document paths
  ---
  duration_ms: 0.216226
  ...
# Subtest: strips install params from the app link
ok 122 - strips install params from the app link
  ---
  duration_ms: 0.089005
  ...
# Subtest: names the install page from host slug
ok 123 - names the install page from host slug
  ---
  duration_ms: 0.047211
  ...
# Subtest: rejects hosts that are not plain slugs
ok 124 - rejects hosts that are not plain slugs
  ---
  duration_ms: 0.033238
  ...
# Subtest: renders install page markup
ok 125 - renders install page markup
  ---
  duration_ms: 0.38334
  ...
# Subtest: escapes host-derived values in the install page
ok 126 - escapes host-derived values in the install page
  ---
  duration_ms: 0.122814
  ...
# Subtest: renders the AURIC manifest with correct launcher sizes
ok 127 - renders the AURIC manifest with correct launcher sizes
  ---
  duration_ms: 0.222463
  ...
# Subtest: vite config keeps the nitro serverDir wiring
ok 128 - vite config keeps the nitro serverDir wiring
  ---
  duration_ms: 0.154538
  ...
# Subtest: nitro middleware and its bundled assets exist
ok 129 - nitro middleware and its bundled assets exist
  ---
  duration_ms: 0.229723
  ...
# Subtest: vite plugin bakes og identity as a virtual module
ok 130 - vite plugin bakes og identity as a virtual module
  ---
  duration_ms: 0.082587
  ...
# Subtest: L4 click voice, folder prefs, and catalog tests pass
ok 131 - L4 click voice, folder prefs, and catalog tests pass
  ---
  duration_ms: 30.715341
  ...
# Subtest: _migrations keys on basename, not path
ok 132 - _migrations keys on basename, not path
  ---
  duration_ms: 0.693792
  ...
# Subtest: a file already applied from another directory does not re-apply
ok 133 - a file already applied from another directory does not re-apply
  ---
  duration_ms: 0.504211
  ...
# Subtest: pending migrations are returned in name order
ok 134 - pending migrations are returned in name order
  ---
  duration_ms: 9.828383
  ...
# Subtest: non-.sql entries are dropped (readdir also yields the auth/ directory)
ok 135 - non-.sql entries are dropped (readdir also yields the auth/ directory)
  ---
  duration_ms: 0.104091
  ...
# Subtest: the auth schema ships outside the globbed directory
ok 136 - the auth schema ships outside the globbed directory
  ---
  duration_ms: 0.271247
  ...
# Subtest: this workspace's auth schema copy is byte-identical to its source
ok 137 - this workspace's auth schema copy is byte-identical to its source
  ---
  duration_ms: 0.125144
  ...
# Subtest: the copy check reads both files and catches an edit
ok 138 - the copy check reads both files and catches an edit
  ---
  duration_ms: 0.558321
  ...
# Subtest: parsePreviewArgs accepts the two actions
ok 139 - parsePreviewArgs accepts the two actions
  ---
  duration_ms: 2.39783
  ...
# Subtest: parsePreviewArgs rejects missing, unknown and extra arguments
ok 140 - parsePreviewArgs rejects missing, unknown and extra arguments
  ---
  duration_ms: 0.818071
  ...
# Subtest: parsePid reads a pidfile and rejects junk
ok 141 - parsePid reads a pidfile and rejects junk
  ---
  duration_ms: 0.154773
  ...
# Subtest: parsePgid reads the pgrp field past a comm containing spaces
ok 142 - parsePgid reads the pgrp field past a comm containing spaces
  ---
  duration_ms: 0.156982
  ...
# Subtest: parseListenerInodes picks LISTEN sockets on the wanted port only
ok 143 - parseListenerInodes picks LISTEN sockets on the wanted port only
  ---
  duration_ms: 0.266726
  ...
# Subtest: parseListenerInodes reads the tcp6 dump the same way
ok 144 - parseListenerInodes reads the tcp6 dump the same way
  ---
  duration_ms: 0.12227
  ...
# Subtest: looksLikePreviewProcess matches the npm wrapper and its vite child
ok 145 - looksLikePreviewProcess matches the npm wrapper and its vite child
  ---
  duration_ms: 0.305052
  ...
# Subtest: looksLikePreviewProcess spares the sibling scripts and re-used pids
ok 146 - looksLikePreviewProcess spares the sibling scripts and re-used pids
  ---
  duration_ms: 0.123377
  ...
# Subtest: previewOwners trusts port owners and dedupes the pidfile pid
ok 147 - previewOwners trusts port owners and dedupes the pidfile pid
  ---
  duration_ms: 0.305249
  ...
# Subtest: previewOwners adds a pidfile pid whose command line is still the preview
ok 148 - previewOwners adds a pidfile pid whose command line is still the preview
  ---
  duration_ms: 1.744015
  ...
# Subtest: previewOwners drops a stale pidfile pid re-used by another process
ok 149 - previewOwners drops a stale pidfile pid re-used by another process
  ---
  duration_ms: 0.86063
  ...
# Subtest: previewOwners drops a pidfile pid that no longer exists
ok 150 - previewOwners drops a pidfile pid that no longer exists
  ---
  duration_ms: 0.201087
  ...
# Subtest: previewOwners on a free port with no pidfile signals nothing
ok 151 - previewOwners on a free port with no pidfile signals nothing
  ---
  duration_ms: 0.079411
  ...
# Subtest: stopOutcome fails when a pid survives or the port is still held
ok 152 - stopOutcome fails when a pid survives or the port is still held
  ---
  duration_ms: 0.138628
  ...
# Subtest: stopOutcome reports a verified free port
ok 153 - stopOutcome reports a verified free port
  ---
  duration_ms: 0.067741
  ...
# Subtest: stopOutcome fails on a listener whose owner cannot be attributed
ok 154 - stopOutcome fails on a listener whose owner cannot be attributed
  ---
  duration_ms: 0.056648
  ...
# Subtest: terminatePids SIGTERMs live pids and skips dead ones
ok 155 - terminatePids SIGTERMs live pids and skips dead ones
  ---
  duration_ms: 2.330232
  ...
# Subtest: terminatePids escalates to SIGKILL when SIGTERM is ignored
ok 156 - terminatePids escalates to SIGKILL when SIGTERM is ignored
  ---
  duration_ms: 0.28349
  ...
# Subtest: terminatePids reports a pid that survives SIGKILL as stubborn
ok 157 - terminatePids reports a pid that survives SIGKILL as stubborn
  ---
  duration_ms: 0.291882
  ...
# Subtest: terminatePids on a free port signals nothing
ok 158 - terminatePids on a free port signals nothing
  ---
  duration_ms: 0.079178
  ...
# (node:4404) ExperimentalWarning: The MockTimers API is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: preview: a successful sign-out clears the token, then redirects
ok 159 - preview: a successful sign-out clears the token, then redirects
  ---
  duration_ms: 4.910795
  ...
# Subtest: preview: a rejected sign-out still clears the token and redirects
ok 160 - preview: a rejected sign-out still clears the token and redirects
  ---
  duration_ms: 0.287448
  ...
# Subtest: preview: a sign-out that throws synchronously still clears and redirects
ok 161 - preview: a sign-out that throws synchronously still clears and redirects
  ---
  duration_ms: 0.13427
  ...
# Subtest: preview: a sign-out that never settles clears and redirects once the wait expires
ok 162 - preview: a sign-out that never settles clears and redirects once the wait expires
  ---
  duration_ms: 28.347575
  ...
# Subtest: preview: no bearer means nothing to invalidate, so no request is made
ok 163 - preview: no bearer means nothing to invalidate, so no request is made
  ---
  duration_ms: 0.225835
  ...
# Subtest: preview: a stored bearer is still invalidated server-side
ok 164 - preview: a stored bearer is still invalidated server-side
  ---
  duration_ms: 0.107199
  ...
# Subtest: deployed: a confirmed sign-out clears the token, then redirects
ok 165 - deployed: a confirmed sign-out clears the token, then redirects
  ---
  duration_ms: 0.142494
  ...
# Subtest: deployed: a sign-out that never settles throws and does NOT redirect
ok 166 - deployed: a sign-out that never settles throws and does NOT redirect
  ---
  duration_ms: 20.973584
  ...
# Subtest: deployed: a rejected sign-out throws and does NOT redirect
ok 167 - deployed: a rejected sign-out throws and does NOT redirect
  ---
  duration_ms: 1.3606
  ...
# Subtest: deployed: the timeout is distinguishable from a rejection
ok 168 - deployed: the timeout is distinguishable from a rejection
  ---
  duration_ms: 19.975455
  ...
# Subtest: settleWithin reports the outcome and never rejects
ok 169 - settleWithin reports the outcome and never rejects
  ---
  duration_ms: 20.754102
  ...
# Subtest: settleWithin waits its full window, then gives up rather than hanging
ok 170 - settleWithin waits its full window, then gives up rather than hanging
  ---
  duration_ms: 2.612319
  ...
# Subtest: pre-sign-in: the preview clear gives up at the preview bound
ok 171 - pre-sign-in: the preview clear gives up at the preview bound
  ---
  duration_ms: 1.105705
  ...
# Subtest: pre-sign-in: a deployed session gets the deployed window, not the preview one
ok 172 - pre-sign-in: a deployed session gets the deployed window, not the preview one
  ---
  duration_ms: 0.361692
  ...
# Subtest: pre-sign-in: a failed clear never blocks sign-in
ok 173 - pre-sign-in: a failed clear never blocks sign-in
  ---
  duration_ms: 0.162128
  ...
# Subtest: pre-sign-in: the preview skips the request when there is no bearer
ok 174 - pre-sign-in: the preview skips the request when there is no bearer
  ---
  duration_ms: 0.075394
  ...
# Subtest: every sign-out bound comes from one rule
ok 175 - every sign-out bound comes from one rule
  ---
  duration_ms: 0.039619
  ...
# Subtest: the defaults are bounded, and deployed waits longer than preview
ok 176 - the defaults are bounded, and deployed waits longer than preview
  ---
  duration_ms: 9.046866
  ...
# Subtest: keeps VITE_-prefixed string entries
ok 177 - keeps VITE_-prefixed string entries
  ---
  duration_ms: 1.12219
  ...
# Subtest: drops non-VITE keys, non-string values and malformed documents
ok 178 - drops non-VITE keys, non-string values and malformed documents
  ---
  duration_ms: 0.169554
  ...
# Subtest: a missing app-env.json is a clean no-op
ok 179 - a missing app-env.json is a clean no-op
  ---
  duration_ms: 0.779179
  ...
# Subtest: reads the app env from a workspace
ok 180 - reads the app env from a workspace
  ---
  duration_ms: 1.118275
  ...
# Subtest: an explicit process-env override wins over the file
ok 181 - an explicit process-env override wins over the file
  ---
  duration_ms: 1.387907
  ...
# Subtest: the template ships auth off
ok 182 - the template ships auth off
  ---
  duration_ms: 0.674718
  ...
# Subtest: vite loadEnv resolves the wrapped value
ok 183 - vite loadEnv resolves the wrapped value
  ---
  duration_ms: 0.256
  ...
# Subtest: the wrapped command runs with the app env applied
ok 184 - the wrapped command runs with the app env applied
  ---
  duration_ms: 84.096502
  ...
# Subtest: the wrapped command sees an explicit override, not the file value
ok 185 - the wrapped command sees an explicit override, not the file value
  ---
  duration_ms: 64.060161
  ...
# Subtest: the wrapper propagates the command's exit code
ok 186 - the wrapper propagates the command's exit code
  ---
  duration_ms: 47.207697
  ...
# Subtest: a signal-killed command is never reported as success
ok 187 - a signal-killed command is never reported as success
  ---
  duration_ms: 39.736457
  ...
# Subtest: the CLI still runs when invoked through a symlinked path
ok 188 - the CLI still runs when invoked through a symlinked path
  ---
  duration_ms: 71.352256
  ...
# Subtest: parseWriteAtomicArgs needs exactly a staged file and a target
ok 189 - parseWriteAtomicArgs needs exactly a staged file and a target
  ---
  duration_ms: 3.006604
  ...
# Subtest: stagingError refuses a temp inside public/ and a no-op move
ok 190 - stagingError refuses a temp inside public/ and a no-op move
  ---
  duration_ms: 0.283712
  ...
# Subtest: handOver replaces the target and clears the staged file
ok 191 - handOver replaces the target and clears the staged file
  ---
  duration_ms: 0.585864
  ...
# Subtest: handOver creates a missing target directory
ok 192 - handOver creates a missing target directory
  ---
  duration_ms: 0.404293
  ...
# Subtest: an interrupted pass leaves the target on its old bytes, temp-free
ok 193 - an interrupted pass leaves the target on its old bytes, temp-free
  ---
  duration_ms: 1.458387
  ...
# Subtest: a failed hand-over creates no directory for the target it never wrote
ok 194 - a failed hand-over creates no directory for the target it never wrote
  ---
  duration_ms: 0.315688
  ...
# Subtest: a staged file on another filesystem is refused, not copied
ok 195 - a staged file on another filesystem is refused, not copied
  ---
  duration_ms: 0.446107
  ...
# Subtest: cli: hands the file over, and refuses a temp staged in public/
ok 196 - cli: hands the file over, and refuses a temp staged in public/
  ---
  duration_ms: 73.57417
  ...
# Subtest: cli: relative paths follow the script's root, not the caller's cwd
ok 197 - cli: relative paths follow the script's root, not the caller's cwd
  ---
  duration_ms: 29.249417
  ...
# Subtest: every hand-over the og skill prints is one this script accepts
ok 198 - every hand-over the og skill prints is one this script accepts
  ---
  duration_ms: 2.29378
  ...
# Subtest: cli: a missing staged file fails without touching the target
ok 199 - cli: a missing staged file fails without touching the target
  ---
  duration_ms: 59.251354
  ...
1..199
# tests 199
# suites 0
# pass 199
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 787.239981
TAP version 13
# (node:4639) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# (node:4639) ExperimentalWarning: The MockTimers API is an experimental feature and might change at any time
# Subtest: callTool failure memo
    # Subtest: replays an identical failure within the memo window without refetching
    ok 1 - replays an identical failure within the memo window without refetching
      ---
      duration_ms: 3.357226
      ...
    # Subtest: keys by token identity so a reminted token shares the memo entry
    ok 2 - keys by token identity so a reminted token shares the memo entry
      ---
      duration_ms: 0.81952
      ...
    # Subtest: sweeps expired entries on write so unique keys do not accumulate
    ok 3 - sweeps expired entries on write so unique keys do not accumulate
      ---
      duration_ms: 1.29413
      ...
    # Subtest: never memoizes login-required 401s
    ok 4 - never memoizes login-required 401s
      ---
      duration_ms: 0.758261
      ...
    1..4
ok 1 - callTool failure memo
  ---
  duration_ms: 6.891345
  type: 'suite'
  ...
# Subtest: callTool in the workspace preview vs deployed
    # Subtest: returns pending (no loginRequired) when the preview has no token yet
    ok 1 - returns pending (no loginRequired) when the preview has no token yet
      ---
      duration_ms: 0.480856
      ...
    # Subtest: returns a plain error (no sign-in CTA) when a deployed app has no token
    ok 2 - returns a plain error (no sign-in CTA) when a deployed app has no token
      ---
      duration_ms: 0.373251
      ...
    # Subtest: treats a gate 401 in the preview as pending and parks the rejected token
    ok 3 - treats a gate 401 in the preview as pending and parks the rejected token
      ---
      duration_ms: 1.000971
      ...
    # Subtest: keeps loginRequired for a gate 401 on a deployed app
    ok 4 - keeps loginRequired for a gate 401 on a deployed app
      ---
      duration_ms: 0.494942
      ...
    1..4
ok 2 - callTool in the workspace preview vs deployed
  ---
  duration_ms: 2.607131
  type: 'suite'
  ...
# Subtest: callTool
    # Subtest: resolves ok:false for non-serializable args instead of rejecting
    ok 1 - resolves ok:false for non-serializable args instead of rejecting
      ---
      duration_ms: 0.329541
      ...
    1..1
ok 3 - callTool
  ---
  duration_ms: 0.396283
  type: 'suite'
  ...
# Subtest: isLoginRequired
    # Subtest: is true only for login-required failures
    ok 1 - is true only for login-required failures
      ---
      duration_ms: 0.144255
      ...
    1..1
ok 4 - isLoginRequired
  ---
  duration_ms: 0.186996
  type: 'suite'
  ...
# Subtest: redirectToLoginIfRequired
    # Subtest: no-ops when login is not required
    ok 1 - no-ops when login is not required
      ---
      duration_ms: 0.164346
      ...
    # Subtest: navigates to the server-built loginUrl in the browser
    ok 2 - navigates to the server-built loginUrl in the browser
      ---
      duration_ms: 0.08907
      ...
    # Subtest: opens a new tab instead of navigating when framed
    ok 3 - opens a new tab instead of navigating when framed
      ---
      duration_ms: 0.075346
      ...
    # Subtest: falls back to navigation when framed and the popup is blocked
    ok 4 - falls back to navigation when framed and the popup is blocked
      ---
      duration_ms: 0.060522
      ...
    # Subtest: returns false when the result has no loginUrl
    ok 5 - returns false when the result has no loginUrl
      ---
      duration_ms: 0.046893
      ...
    # Subtest: returns false on the server (no window)
    ok 6 - returns false on the server (no window)
      ---
      duration_ms: 0.035811
      ...
    1..6
ok 5 - redirectToLoginIfRequired
  ---
  duration_ms: 0.562221
  type: 'suite'
  ...
# Subtest: GoogleCalendarTools
    # Subtest: does not expose a list_events name
    ok 1 - does not expose a list_events name
      ---
      duration_ms: 0.050296
      ...
    1..1
ok 6 - GoogleCalendarTools
  ---
  duration_ms: 0.079101
  type: 'suite'
  ...
# Subtest: classifyCallToolError
    # Subtest: returns null for successful results
    ok 1 - returns null for successful results
      ---
      duration_ms: 0.122351
      ...
    # Subtest: classifies a pending preview token as pending before any other kind
    ok 2 - classifies a pending preview token as pending before any other kind
      ---
      duration_ms: 0.067244
      ...
    # Subtest: classifies a deployed missing token as error, not a login CTA
    ok 3 - classifies a deployed missing token as error, not a login CTA
      ---
      duration_ms: 0.440515
      ...
    # Subtest: classifies gate loginRequired without a missing-token message as login
    ok 4 - classifies gate loginRequired without a missing-token message as login
      ---
      duration_ms: 0.10191
      ...
    # Subtest: classifies not_connected and failed_precondition
    ok 5 - classifies not_connected and failed_precondition
      ---
      duration_ms: 0.089096
      ...
    # Subtest: classifies scope_denied without claiming a missing grant
    ok 6 - classifies scope_denied without claiming a missing grant
      ---
      duration_ms: 0.05892
      ...
    # Subtest: classifies access_denied
    ok 7 - classifies access_denied
      ---
      duration_ms: 0.040016
      ...
    # Subtest: falls back to a generic error with the raw message
    ok 8 - falls back to a generic error with the raw message
      ---
      duration_ms: 0.099081
      ...
    1..8
ok 7 - classifyCallToolError
  ---
  duration_ms: 1.134212
  type: 'suite'
  ...
# (node:4640) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: readinessProbeDelayMs
    # Subtest: backs off through the schedule and then holds the last delay
    ok 1 - backs off through the schedule and then holds the last delay
      ---
      duration_ms: 0.754084
      ...
    # Subtest: clamps a negative attempt to the first delay
    ok 2 - clamps a negative attempt to the first delay
      ---
      duration_ms: 0.114834
      ...
    1..2
ok 8 - readinessProbeDelayMs
  ---
  duration_ms: 1.52946
  type: 'suite'
  ...
# Subtest: readinessProbeExhausted
    # Subtest: gives up only once the total wait budget has elapsed
    ok 1 - gives up only once the total wait budget has elapsed
      ---
      duration_ms: 0.166516
      ...
    1..1
ok 9 - readinessProbeExhausted
  ---
  duration_ms: 0.24577
  type: 'suite'
  ...
# (node:4646) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: verifyGateIdentityToken
    # Subtest: returns the identity for a valid token
    ok 1 - returns the identity for a valid token
      ---
      duration_ms: 28.834599
      ...
    # Subtest: omits optional claims as nulls
    ok 2 - omits optional claims as nulls
      ---
      duration_ms: 8.508831
      ...
    # Subtest: rejects a wrong audience
    ok 3 - rejects a wrong audience
      ---
      duration_ms: 1.293901
      ...
    # Subtest: rejects a wrong issuer
    ok 4 - rejects a wrong issuer
      ---
      duration_ms: 5.319084
      ...
    # Subtest: rejects an expired token
    ok 5 - rejects an expired token
      ---
      duration_ms: 3.394922
      ...
    # Subtest: rejects a token without exp
    ok 6 - rejects a token without exp
      ---
      duration_ms: 1.225667
      ...
    # Subtest: rejects a token signed by a different key with the same kid
    ok 7 - rejects a token signed by a different key with the same kid
      ---
      duration_ms: 7.411535
      ...
    # Subtest: refetches the JWKS when the kid rotates
    ok 8 - refetches the JWKS when the kid rotates
      ---
      duration_ms: 8.304159
      ...
    1..8
ok 10 - verifyGateIdentityToken
  ---
  duration_ms: 65.518241
  type: 'suite'
  ...
# Subtest: gateIdentityFromHeaders
    # Subtest: verifies the header token end to end and fails closed without it
    ok 1 - verifies the header token end to end and fails closed without it
      ---
      duration_ms: 13.724674
      ...
    # Subtest: activates on a deployed-shaped request without GROK_GATE_ORIGIN
    ok 2 - activates on a deployed-shaped request without GROK_GATE_ORIGIN
      ---
      duration_ms: 4.739652
      ...
    # Subtest: verifies a preview-audience token with no gate env vars via the loopback default
    ok 3 - verifies a preview-audience token with no gate env vars via the loopback default
      ---
      duration_ms: 1.25227
      ...
    # Subtest: rejects a wrong-issuer token in the loopback default mode
    ok 4 - rejects a wrong-issuer token in the loopback default mode
      ---
      duration_ms: 1.107284
      ...
    # Subtest: verifies a preview-audience token when only GROK_GATE_ORIGIN is set
    ok 5 - verifies a preview-audience token when only GROK_GATE_ORIGIN is set
      ---
      duration_ms: 5.964851
      ...
    # Subtest: rejects a preview-audience token when GROK_PROJECT_ID is set
    ok 6 - rejects a preview-audience token when GROK_PROJECT_ID is set
      ---
      duration_ms: 11.436555
      ...
    # Subtest: rejects an app-audience token in preview mode
    ok 7 - rejects an app-audience token in preview mode
      ---
      duration_ms: 5.765039
      ...
    1..7
ok 11 - gateIdentityFromHeaders
  ---
  duration_ms: 44.488888
  type: 'suite'
  ...
# Subtest: gateIdentityEnabled
    # Subtest: is enabled by default with no gate env vars
    ok 1 - is enabled by default with no gate env vars
      ---
      duration_ms: 0.115976
      ...
    # Subtest: is disabled when VITE_AUTH_ENABLED is false
    ok 2 - is disabled when VITE_AUTH_ENABLED is false
      ---
      duration_ms: 0.074529
      ...
    1..2
ok 12 - gateIdentityEnabled
  ---
  duration_ms: 0.271073
  type: 'suite'
  ...
# Subtest: gateTokenAudience
    # Subtest: pins app:<id> when GROK_PROJECT_ID is set, even alongside GROK_GATE_ORIGIN
    ok 1 - pins app:<id> when GROK_PROJECT_ID is set, even alongside GROK_GATE_ORIGIN
      ---
      duration_ms: 0.131896
      ...
    # Subtest: pins preview when GROK_PROJECT_ID is unset
    ok 2 - pins preview when GROK_PROJECT_ID is unset
      ---
      duration_ms: 0.061973
      ...
    1..2
ok 13 - gateTokenAudience
  ---
  duration_ms: 1.474834
  type: 'suite'
  ...
# Subtest: gateIdentityUserInfo
    # Subtest: falls back to a synthetic email and name for sub-only claims
    ok 1 - falls back to a synthetic email and name for sub-only claims
      ---
      duration_ms: 12.236953
      ...
    # Subtest: keeps real claims, lowercasing the email and marking it verified
    ok 2 - keeps real claims, lowercasing the email and marking it verified
      ---
      duration_ms: 0.17563
      ...
    1..2
ok 14 - gateIdentityUserInfo
  ---
  duration_ms: 12.491183
  type: 'suite'
  ...
# Subtest: sessionBoundToGateIdentity
    # Subtest: keeps the session when it is bound to the same gate sub
    ok 1 - keeps the session when it is bound to the same gate sub
      ---
      duration_ms: 0.160577
      ...
    # Subtest: rotates when the session belongs to a different gate sub
    ok 2 - rotates when the session belongs to a different gate sub
      ---
      duration_ms: 0.060738
      ...
    # Subtest: rotates when the session user has no gate-bound account
    ok 3 - rotates when the session user has no gate-bound account
      ---
      duration_ms: 0.056738
      ...
    1..3
ok 15 - sessionBoundToGateIdentity
  ---
  duration_ms: 0.425889
  type: 'suite'
  ...
# (node:4672) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: resolveSignInGateState
    # Subtest: is pending while the session check is in flight, user or not
    ok 1 - is pending while the session check is in flight, user or not
      ---
      duration_ms: 0.433688
      ...
    # Subtest: is signed_in once a user is present
    ok 2 - is signed_in once a user is present
      ---
      duration_ms: 0.099603
      ...
    # Subtest: is signed_out only after the check resolved with no user
    ok 3 - is signed_out only after the check resolved with no user
      ---
      duration_ms: 0.08945
      ...
    1..3
ok 16 - resolveSignInGateState
  ---
  duration_ms: 1.382159
  type: 'suite'
  ...
1..16
# tests 55
# suites 16
# pass 55
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 593.792338

> test:auric
> node --import ./scripts/test-aliases.mjs --experimental-strip-types --test tests/auric.test.ts tests/brainstorm.test.ts

TAP version 13
# (node:4712) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: timer deadlines recover once after a suspended tab and retain the true completion time
ok 1 - timer deadlines recover once after a suspended tab and retain the true completion time
  ---
  duration_ms: 12.730134
  ...
# Subtest: paused countdown is stable and cannot be changed accidentally
ok 2 - paused countdown is stable and cannot be changed accidentally
  ---
  duration_ms: 1.450707
  ...
# Subtest: control lock cannot trap a completed timer alert
ok 3 - control lock cannot trap a completed timer alert
  ---
  duration_ms: 1.88515
  ...
# Subtest: long saved timers round-trip all duration components and clear stale parts
ok 4 - long saved timers round-trip all duration components and clear stale parts
  ---
  duration_ms: 0.903668
  ...
# Subtest: stopwatch ignores duplicate starts and excludes held time
ok 5 - stopwatch ignores duplicate starts and excludes held time
  ---
  duration_ms: 3.105806
  ...
# Subtest: alarms honor seconds, recover when overdue, and queue coincident alarms
ok 6 - alarms honor seconds, recover when overdue, and queue coincident alarms
  ---
  duration_ms: 3.677415
  ...
# Subtest: snooze schedules a full five minutes including seconds
ok 7 - snooze schedules a full five minutes including seconds
  ---
  duration_ms: 4.400356
  ...
# Subtest: heartbeat anchor and settings survive hydration
ok 8 - heartbeat anchor and settings survive hydration
  ---
  duration_ms: 1.646707
  ...
# Subtest: storage skips unchanged snapshots and does not crash when full
ok 9 - storage skips unchanged snapshots and does not crash when full
  ---
  duration_ms: 0.504146
  ...
# Subtest: unreadable saved JSON is preserved and hydration still completes
ok 10 - unreadable saved JSON is preserved and hydration still completes
  ---
  duration_ms: 1.112542
  ...
# Subtest: a failed recovery backup protects the original saved bytes
ok 11 - a failed recovery backup protects the original saved bytes
  ---
  duration_ms: 4.866725
  ...
# Subtest: readability chooses the higher contrast neutral for middle luminance backgrounds
ok 12 - readability chooses the higher contrast neutral for middle luminance backgrounds
  ---
  duration_ms: 0.689431
  ...
# Subtest: font loader waits for binary success, deduplicates, and retries failures
ok 13 - font loader waits for binary success, deduplicates, and retries failures
  ---
  duration_ms: 0.683499
  ...
# Subtest: catalog contains unique stable identities and supported regular or bold weights
ok 14 - catalog contains unique stable identities and supported regular or bold weights
  ---
  duration_ms: 0.234674
  ...
# Subtest: custom accent ink stays readable and malformed hex is rejected
ok 15 - custom accent ink stays readable and malformed hex is rejected
  ---
  duration_ms: 0.388283
  ...
# Subtest: CSV export preserves quoted notes as data and neutralizes formulas
ok 16 - CSV export preserves quoted notes as data and neutralizes formulas
  ---
  duration_ms: 0.471066
  ...
# Subtest: web demo strips heartbeat trio even if an old snapshot listed them installed
ok 17 - web demo strips heartbeat trio even if an old snapshot listed them installed
  ---
  duration_ms: 0.222284
  ...
# Subtest: Chicago masthead clock uses America/Chicago and 12-hour time
ok 18 - Chicago masthead clock uses America/Chicago and 12-hour time
  ---
  duration_ms: 19.77328
  ...
# Subtest: opening the bell does not mark notices read; markKindRead clears one folder only
ok 19 - opening the bell does not mark notices read; markKindRead clears one folder only
  ---
  duration_ms: 0.643214
  ...
# (node:4713) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: floor, schedule and glow changes stay in the draft; Discard keeps committed values
ok 20 - floor, schedule and glow changes stay in the draft; Discard keeps committed values
  ---
  duration_ms: 2.422212
  ...
# Subtest: Apply commits every appearance option and matches the effective preview
ok 21 - Apply commits every appearance option and matches the effective preview
  ---
  duration_ms: 1.850751
  ...
# Subtest: malformed or transparent essential colors fail closed without throwing
ok 22 - malformed or transparent essential colors fail closed without throwing
  ---
  duration_ms: 1.122415
  ...
# Subtest: Restore appearance repairs type size and background without changing user history
ok 23 - Restore appearance repairs type size and background without changing user history
  ---
  duration_ms: 1.823618
  ...
# Subtest: invalid saved timezone recovers and explicit h23 uses midnight 00
ok 24 - invalid saved timezone recovers and explicit h23 uses midnight 00
  ---
  duration_ms: 23.939586
  ...
# Subtest: master activity signals distinguish running, completed, armed, and delivered
ok 25 - master activity signals distinguish running, completed, armed, and delivered
  ---
  duration_ms: 0.597196
  ...
# Subtest: archiving a note preserves recorded time and can be reversed by saving
ok 26 - archiving a note preserves recorded time and can be reversed by saving
  ---
  duration_ms: 9.113712
  ...
# Subtest: task save does not invent completion
ok 27 - task save does not invent completion
  ---
  duration_ms: 0.452936
  ...
# Subtest: device network and battery stay off until explicitly enabled
ok 28 - device network and battery stay off until explicitly enabled
  ---
  duration_ms: 0.411743
  ...
# Subtest: session shell opens home, folders stay visible, paid modules preview instead of installing
ok 29 - session shell opens home, folders stay visible, paid modules preview instead of installing
  ---
  duration_ms: 0.783219
  ...
# Subtest: heartbeat overlay is a single terminal ask and Off disables it
ok 30 - heartbeat overlay is a single terminal ask and Off disables it
  ---
  duration_ms: 3.094814
  ...
# Subtest: selectPalette keeps a stable reference while the look is unchanged
ok 31 - selectPalette keeps a stable reference while the look is unchanged
  ---
  duration_ms: 1.945521
  ...
1..31
# tests 31
# suites 0
# pass 31
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 720.428871

> test:l1
> node --import ./scripts/test-aliases.mjs --experimental-strip-types --test tests/l1-*.test.ts tests/l1_*.test.ts

TAP version 13
# (node:4758) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: rowsCrossed counts each integer row and ignores a hold
ok 1 - rowsCrossed counts each integer row and ignores a hold
  ---
  duration_ms: 0.659237
  ...
# Subtest: span reels are the only drums that hide the aperture
ok 2 - span reels are the only drums that hide the aperture
  ---
  duration_ms: 22.128549
  ...
# Subtest: new L1 files use underscores and the empty lane hooks are archived
not ok 3 - new L1 files use underscores and the empty lane hooks are archived
  ---
  duration_ms: 19.035431
  location: '/tmp/auric_l5_scratch/auric-web/tests/l1_acceptance.test.ts:32:1'
  failureType: 'testCodeFailure'
  error: |-
    styles/folders.css
    
    true !== false
    
  code: 'ERR_ASSERTION'
  name: 'AssertionError'
  expected: false
  actual: true
  operator: 'strictEqual'
  stack: |-
    TestContext.<anonymous> (file:///tmp/auric_l5_scratch/auric-web/tests/l1_acceptance.test.ts:48:12)
    Test.runInAsyncScope (node:async_hooks:211:14)
    Test.run (node:internal/test_runner/test:979:25)
    Test.processPendingSubtests (node:internal/test_runner/test:677:18)
    Test.postRun (node:internal/test_runner/test:1090:19)
    Test.run (node:internal/test_runner/test:1018:12)
    async Test.processPendingSubtests (node:internal/test_runner/test:677:7)
  ...
# Subtest: Pavilion SoundDeck keeps the default aperture selector
ok 4 - Pavilion SoundDeck keeps the default aperture selector
  ---
  duration_ms: 1.041961
  ...
# (node:4760) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: reelFeel plays the house click once and sends the existing vibration pattern
ok 5 - reelFeel plays the house click once and sends the existing vibration pattern
  ---
  duration_ms: 3.566108
  ...
# Subtest: the shipped tick recipes remain valid
ok 6 - the shipped tick recipes remain valid
  ---
  duration_ms: 3.372604
  ...
# (node:4762) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: laser defaults are enabled at full intensity with the default red
ok 7 - laser defaults are enabled at full intensity with the default red
  ---
  duration_ms: 0.949181
  ...
# Subtest: laser intensity clamps and all five presets are available
ok 8 - laser intensity clamps and all five presets are available
  ---
  duration_ms: 0.104167
  ...
# Subtest: laser bloom composites to a deterministic color
ok 9 - laser bloom composites to a deterministic color
  ---
  duration_ms: 0.454864
  ...
# Subtest: reel geometry fits available width and centers the selected row
ok 10 - reel geometry fits available width and centers the selected row
  ---
  duration_ms: 0.13949
  ...
# Subtest: reel geometry handles zero scale and zero-sized measurements
ok 11 - reel geometry handles zero scale and zero-sized measurements
  ---
  duration_ms: 0.142261
  ...
1..11
# tests 11
# suites 0
# pass 10
# fail 1
# cancelled 0
# skipped 0
# todo 0
# duration_ms 562.405083
```

### Lane 2, 3, and 4 unit tests

```
TAP version 13
# (node:4913) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: wheel samples are opaque colors and respond to palette surface changes
ok 1 - wheel samples are opaque colors and respond to palette surface changes
  ---
  duration_ms: 1.757967
  ...
# Subtest: number-color guard enforces the near-black rule and theme contrast
ok 2 - number-color guard enforces the near-black rule and theme contrast
  ---
  duration_ms: 1.085098
  ...
# Subtest: number-color contrast ratios stay pinned to two decimals
ok 3 - number-color contrast ratios stay pinned to two decimals
  ---
  duration_ms: 1.312566
  ...
# Subtest: laser bloom is considered when checking selected-row numeral contrast
ok 4 - laser bloom is considered when checking selected-row numeral contrast
  ---
  duration_ms: 0.371468
  ...
# Subtest: invalid and translucent number colors are refused
ok 5 - invalid and translucent number colors are refused
  ---
  duration_ms: 0.104387
  ...
# Subtest: laser presets stay in brief order
ok 6 - laser presets stay in brief order
  ---
  duration_ms: 0.172699
  ...
# Subtest: laser intensity maps linearly and disabling removes its beam opacity
ok 7 - laser intensity maps linearly and disabling removes its beam opacity
  ---
  duration_ms: 0.289831
  ...
# Subtest: resolved reel numerals stay guard-approved and away from near-black for curated palettes
ok 8 - resolved reel numerals stay guard-approved and away from near-black for curated palettes
  ---
  duration_ms: 15.578386
  ...
# (node:4919) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: L2 state contract defaults and all ten settings persist
ok 9 - L2 state contract defaults and all ten settings persist
  ---
  duration_ms: 3.397821
  ...
# Subtest: state setters clamp, validate, and preserve invalid updates
ok 10 - state setters clamp, validate, and preserve invalid updates
  ---
  duration_ms: 2.874005
  ...
# Subtest: v21 snapshots hydrate with defaults while preserving existing data
ok 11 - v21 snapshots hydrate with defaults while preserving existing data
  ---
  duration_ms: 0.649024
  ...
# Subtest: laser and number choices survive a persistence round trip
ok 12 - laser and number choices survive a persistence round trip
  ---
  duration_ms: 0.842038
  ...
# Subtest: hydration clears stored number colors that no longer pass contrast
ok 13 - hydration clears stored number colors that no longer pass contrast
  ---
  duration_ms: 0.364275
  ...
# Subtest: empty storage still completes hydration with defaults
ok 14 - empty storage still completes hydration with defaults
  ---
  duration_ms: 0.229296
  ...
# (node:4920) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: folder FLIP plans invert only visible tiles that moved
ok 15 - folder FLIP plans invert only visible tiles that moved
  ---
  duration_ms: 0.906012
  ...
# Subtest: folder FLIP keyframes animate transform and opacity only
ok 16 - folder FLIP keyframes animate transform and opacity only
  ---
  duration_ms: 0.173085
  ...
# Subtest: frame budget matches the 60fps gate and rejects the recorded miss
ok 17 - frame budget matches the 60fps gate and rejects the recorded miss
  ---
  duration_ms: 0.328647
  ...
# Subtest: FLIP measurement is one read pass followed by writes
ok 18 - FLIP measurement is one read pass followed by writes
  ---
  duration_ms: 0.110108
  ...
# (node:4949) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: gesture classifier separates taps, long-presses, and early drags
ok 19 - gesture classifier separates taps, long-presses, and early drags
  ---
  duration_ms: 0.553932
  ...
# (node:4961) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: folder metrics clamp size and choose layout columns by available width
ok 20 - folder metrics clamp size and choose layout columns by available width
  ---
  duration_ms: 0.690988
  ...
# Subtest: label hit zone expands to 48px and stays within tile bounds
ok 21 - label hit zone expands to 48px and stays within tile bounds
  ---
  duration_ms: 0.161473
  ...
# Subtest: folder preference hydration clamps values and appends newly installed module ids
ok 22 - folder preference hydration clamps values and appends newly installed module ids
  ---
  duration_ms: 0.647686
  ...
# (node:4973) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: critical spring settles within 500ms without crossing its target
ok 23 - critical spring settles within 500ms without crossing its target
  ---
  duration_ms: 0.551297
  ...
# Subtest: safe-area clamp honors viewport edges and simulated insets
ok 24 - safe-area clamp honors viewport edges and simulated insets
  ---
  duration_ms: 0.188629
  ...
# (node:4984) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: a procedural click is scheduled at the context clock with no lookahead
ok 25 - a procedural click is scheduled at the context clock with no lookahead
  ---
  duration_ms: 0.541526
  ...
# Subtest: the house click starts once at currentTime on the interactive voice
ok 26 - the house click starts once at currentTime on the interactive voice
  ---
  duration_ms: 0.381554
  ...
# Subtest: ten rapid previews retrigger ten oscillators and legacy ids use the house recipe
ok 27 - ten rapid previews retrigger ten oscillators and legacy ids use the house recipe
  ---
  duration_ms: 0.311567
  ...
# Subtest: a missing context does not throw and does not invent a start time
ok 28 - a missing context does not throw and does not invent a start time
  ---
  duration_ms: 0.159191
  ...
# (node:4997) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: Clicker Sounds is free, uses the existing pulses glyph, and preserves the module order
ok 29 - Clicker Sounds is free, uses the existing pulses glyph, and preserves the module order
  ---
  duration_ms: 0.975675
  ...
# Subtest: the seven tick families expose 77 ordered cuts apiece
ok 30 - the seven tick families expose 77 ordered cuts apiece
  ---
  duration_ms: 0.474129
  ...
# Subtest: the house click is the default selection and legacy tick ids still resolve
ok 31 - the house click is the default selection and legacy tick ids still resolve
  ---
  duration_ms: 0.139029
  ...
# (node:5009) ExperimentalWarning: Type Stripping is an experimental feature and might change at any time
# (Use `node --trace-warnings ...` to show where the warning was created)
# Subtest: clicker-sounds reads Lane 3 size, icon style, and rename
ok 32 - clicker-sounds reads Lane 3 size, icon style, and rename
  ---
  duration_ms: 0.557588
  ...
# Subtest: blank names are refused, long names stop at 24, and missing fields use Lane 3 defaults
ok 33 - blank names are refused, long names stop at 24, and missing fields use Lane 3 defaults
  ---
  duration_ms: 0.636314
  ...
# Subtest: a new clicker id appends after a saved order that does not know it yet
ok 34 - a new clicker id appends after a saved order that does not know it yet
  ---
  duration_ms: 0.54356
  ...
1..34
# tests 34
# suites 0
# pass 34
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 577.281863
```

### scripts/l4_click_voice.test.mjs

```
TAP version 13
# Subtest: L4 click voice, folder prefs, and catalog tests pass
ok 1 - L4 click voice, folder prefs, and catalog tests pass
  ---
  duration_ms: 24.896935
  ...
1..1
# tests 1
# suites 0
# pass 1
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 62.056461
```

### Playwright chromium install

```
Downloading Chrome for Testing 153.0.8010.12 (playwright chromium v1243) from https://cdn.playwright.dev/builds/cft/153.0.8010.12/linux64/chrome-linux64.zip
|                                                                                |   0% of 186.8 MiB
|■■■■■■■■                                                                        |  10% of 186.8 MiB
|■■■■■■■■■■■■■■■■                                                                |  20% of 186.8 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■                                                        |  30% of 186.8 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                                                |  40% of 186.8 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                                        |  50% of 186.8 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                                |  60% of 186.8 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                        |  70% of 186.8 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                |  80% of 186.8 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■        |  90% of 186.8 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■| 100% of 186.8 MiB
Chrome for Testing 153.0.8010.12 (playwright chromium v1243) downloaded to /home/ubuntu/.cache/ms-playwright/chromium-1243
Downloading FFmpeg (playwright ffmpeg v1011) from https://cdn.playwright.dev/dbazure/download/playwright/builds/ffmpeg/1011/ffmpeg-linux.zip
|                                                                                |   0% of 2.3 MiB
|■■■■■■■■                                                                        |  10% of 2.3 MiB
|■■■■■■■■■■■■■■■■                                                                |  20% of 2.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■                                                        |  30% of 2.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                                                |  40% of 2.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                                        |  50% of 2.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                                |  60% of 2.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                        |  70% of 2.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                |  80% of 2.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■        |  90% of 2.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■| 100% of 2.3 MiB
FFmpeg (playwright ffmpeg v1011) downloaded to /home/ubuntu/.cache/ms-playwright/ffmpeg-1011
Downloading Chrome Headless Shell 153.0.8010.12 (playwright chromium-headless-shell v1243) from https://cdn.playwright.dev/builds/cft/153.0.8010.12/linux64/chrome-headless-shell-linux64.zip
|                                                                                |   0% of 114.3 MiB
|■■■■■■■■                                                                        |  10% of 114.3 MiB
|■■■■■■■■■■■■■■■■                                                                |  20% of 114.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■                                                        |  30% of 114.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                                                |  40% of 114.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                                        |  50% of 114.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                                |  60% of 114.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                        |  70% of 114.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■                |  80% of 114.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■        |  90% of 114.3 MiB
|■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■| 100% of 114.3 MiB
Chrome Headless Shell 153.0.8010.12 (playwright chromium-headless-shell v1243) downloaded to /home/ubuntu/.cache/ms-playwright/chromium_headless_shell-1243
```

### Lane 1 e2e

```
{"perfNormal":{"medianMs":16.69999999999891,"p95Ms":16.700000000000728},"perfThrottled":{"medianMs":16.69999999999891,"p95Ms":16.700000000000728}}
```

### Lane 2 e2e

```
TAP version 13
# L2 motion median=16.70ms p95=16.70ms frames=62
# L2 apply frames=1 css=\#AABBCC beam=\#AABBCC
# Subtest: Laser & Numbers drafts, applies, discards, persists, and fits all target widths
ok 1 - Laser & Numbers drafts, applies, discards, persists, and fits all target widths
  ---
  duration_ms: 4166.277906
  ...
1..1
# tests 1
# suites 0
# pass 1
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 4498.723923
```

### Lane 3 e2e

```
TAP version 13
# Subtest: L3 layout sheet, rename, size, and icon styles persist
ok 1 - L3 layout sheet, rename, size, and icon styles persist
  ---
  duration_ms: 5051.367132
  ...
# Subtest: L3 layout flip is transform and opacity only and reduced motion is instant
ok 2 - L3 layout flip is transform and opacity only and reduced motion is instant
  ---
  duration_ms: 2446.20075
  ...
# L3 layout FLIP frame interval: mean=15.830ms median=16.700ms p95=16.700ms max=16.800ms samples=[0.0019999999999527063,16.5,16.59999999999991,16.59999999999991,16.59999999999991,16.59999999999991,16.59999999999991,16.59999999999991,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.799999999999955]
# Subtest: L3 body long-press opens size and label long-press handles rename and keyboard parity
ok 3 - L3 body long-press opens size and label long-press handles rename and keyboard parity
  ---
  duration_ms: 5823.645139
  ...
# Subtest: L3 icon styles cover every glyph and etched text stays matte and readable
not ok 4 - L3 icon styles cover every glyph and etched text stays matte and readable
  ---
  duration_ms: 35325.939991
  location: '/tmp/auric_l5_scratch/auric-web/tests/e2e/l3_folders.e2e.mjs:278:1'
  failureType: 'testCodeFailure'
  error: 'page.waitForFunction: Timeout 30000ms exceeded.'
  code: 'ERR_TEST_FAILURE'
  name: 'TimeoutError'
  stack: |-
    TestContext.<anonymous> (/tmp/auric_l5_scratch/auric-web/tests/e2e/l3_folders.e2e.mjs:360:16)
  ...
# Subtest: L3 overflow matrix keeps folder controls within the viewport
ok 5 - L3 overflow matrix keeps folder controls within the viewport
  ---
  duration_ms: 100329.409453
  ...
# Subtest: L3 drag snap-back stays in the viewport at every sampled frame
ok 6 - L3 drag snap-back stays in the viewport at every sampled frame
  ---
  duration_ms: 15606.128105
  ...
# Subtest: L3 reorder persists and reduced-motion snap-back is immediate
ok 7 - L3 reorder persists and reduced-motion snap-back is immediate
  ---
  duration_ms: 3314.709013
  ...
1..7
# tests 7
# suites 0
# pass 6
# fail 1
# cancelled 0
# skipped 0
# todo 0
# duration_ms 100714.087136
```

### Lane 4 e2e

```
TAP version 13
# Subtest: Clicker Sounds lists, selects, previews, and stays within the viewport
not ok 1 - Clicker Sounds lists, selects, previews, and stays within the viewport
  ---
  duration_ms: 30918.7737
  location: '/tmp/auric_l5_scratch/auric-web/tests/e2e/l4_clicker_sounds.e2e.mjs:8:1'
  failureType: 'testCodeFailure'
  error: |-
    locator.click: Timeout 30000ms exceeded.
    Call log:
      - waiting for getByRole('button', { name: 'clicker-sounds unlocked' })
    
  code: 'ERR_TEST_FAILURE'
  name: 'TimeoutError'
  stack: |-
    TestContext.<anonymous> (/tmp/auric_l5_scratch/auric-web/tests/e2e/l4_clicker_sounds.e2e.mjs:63:73)
  ...
1..1
# tests 1
# suites 0
# pass 0
# fail 1
# cancelled 0
# skipped 0
# todo 0
# duration_ms 31238.049985
```

### Integration probe

```
folder_name_count=16
folder_labels=["timer unlocked","stopwatch unlocked","alarm unlocked","House Clicks unlocked","device unlocked","tutorial unlocked","themes locked","sizing locked","opacity locked","icons locked","alarm-sounds locked","timer-sounds locked","haptics locked","heartbeat locked","heartbeat-log locked","logs-exports locked"]
clicker_name=House Clicks
clicker_size=72
clicker_style=etched
preview_delta=1 last_frequency=1346 reelSound=click-02
timer_before={"strips":2,"color":"#FF0000","on":"true","reelNumber":"#40372D"}
wheel_notch_starts=6 delta=1 last_frequency=1346 reelSound=click-02
timer_after_laser={"color":"#00E5FC","opacity":"1","spread":"48px","background":"linear-gradient(rgba(0, 0, 0, 0), color(srgb 0 0.898039 0.988235 / 0.55), rgba(0, 0, 0, 0))"}
reel_look_witness={"beam":1,"hairline":1,"wheelStrips":0}
page_errors=[]
```

### Pixel diffs

```
Lane screenshot baselines (committed on each lane) versus the same files after the integration e2e rewrote them.
Comparator: pngjs, per-pixel RGBA.

app-builder-built-mobile.png 390x844 changed=0/329160 percent=0.0000
l1/laser_demo.png 1280x3846 changed=9275/4922880 percent=0.1884
l1/laser_light.png 1280x3846 changed=0/4922880 percent=0.0000
l1/laser_strip.png 332x49 changed=0/16268 percent=0.0000
l2/beam_on_dark.png 390x844 changed=22795/329160 percent=6.9252
l2/laser_settings.png 390x844 changed=22487/329160 percent=6.8316
l2/number_contrast.png 390x844 changed=13801/329160 percent=4.1928
l3/icons_3d.png SIZE 324x1469 -> 324x1567
l3/icons_etched.png SIZE 324x1469 -> 324x1567
l4/clicker_folder.png 390x844 changed=0/329160 percent=0.0000
note: l4/clicker_folder.png was not rewritten. The L4 e2e failed before it could capture a new shot, so this 0% is the untouched committed file.

Changed-row counts on the red overlay:
l2/laser_settings.png changed_rows 317 of 844
l1/laser_demo.png changed_rows 80 of 3846
l2/number_contrast.png changed_rows 258 of 844

Default-state captures at 390x844, animations disabled.
auric-base server: http://127.0.0.1:8090 (origin/auric-base e39aa55)
integration server: http://127.0.0.1:8080 (scratch d521a24)

base timer probe: {"strips":0,"color":""}
folder_list.png 390x844 vs 390x844 common 390x844 changed=8538/329160 percent=2.5939
timer.png 390x844 vs 390x844 common 390x844 changed=82339/329160 percent=25.0149

Timer row bands with more than 8 changed pixels:
[157,197], [217,251], [288,579], [593,605], [618,621]
The reel block is y 288-579. Masking every changed band leaves 0.1073% elsewhere. That mask is wider than the laser row, so it is not the L2-11 exclusion.
```


## Run 2

**Verdict: FAIL**

The three test blockers from Run 1 are fixed. `npm test` is 296 pass, 0 fail, 0 skipped. Every Playwright end-to-end file passes, including the FLIP gate. The default folder list versus `auric-base` is still **2.5939%** changed pixels (8538/329160). The gate is 1%.

The 0.6662% figure measured by L3+L4 is the chrome band above the list (the new menu button plus the `folders 5` to `folders 6` digit). That band is under 1%. The rest of the diff is folder-title ink on the two visible rows, and that ink is what crosses the gate.

Scratch integration (local only, not pushed): `8c3aa931cbe95a91d6dfffe5c618cf9288b94773`

Built fresh on `origin/auric-base` `e39aa55c73ba85939f488cf649e2f085df111d2a`, then:

| Lane | PR | Head | Scratch merge |
|---|---|---|---|
| L1 | #27 | `142ef18497a82a89c5f1a6f7f6b583db3a8590ce` | fast-forward |
| L2 | #29 | `a79ba616d593ece0f30d6dfb8b4118317e40b45a` | `5980b0e` |
| L3 | #31 | `e35cb9a5b22af896c4b18bca93fc36d29bae83fc` | `0b5e69c` |
| L4 | #33 | `2f46d38096c4af3c2cdd45461d79a75ad871015e` | `8c3aa93` |

### Lane verdicts

| Lane | Verdict | This run |
|---|---|---|
| L1 | PASS | Acceptance test allows `styles/folders.css` and `styles/clicker.css`. `test:l1` is 11/11. Wheel end-to-end passes at median and p95 16.7 ms. |
| L2 | PASS | Head unchanged. Unit coverage and the reel-look Playwright test pass. Settings screenshots still move because the beam shimmer is not frozen. |
| L3 | FAIL | End-to-end is 7/7 and FLIP passes. Default folder list versus `auric-base` is 2.5939%. The blocking pixels are the light-mood title color in `styles/folders.css`. |
| L4 | PASS | End-to-end opens Clicker Sounds with `[data-folder-id="clicker-sounds"]` and passes. The status digit `folders 6` is 61 pixels (0.0185%) and does not cross the 1% gate on its own. |

### What works together

On the integration server the folder grid has 16 tiles, `data-layout="big"`, and two `.laser-strip` elements at `#FF0000` after opening timer. Lane 2's Playwright test applied `#AABBCC` to the beam in 1 frame. Lane 4's Playwright test listed, selected, and previewed a click (latency hint 0.01, output 0.032, twice). Lane 3's icon test accepted the rendered id list. No test was skipped: `AURIC_E2E_URL` and `L3_E2E` were set.

FLIP from the passing L3 test: mean **16.660 ms**, median **16.700 ms**, p95 **16.700 ms**, max **16.700 ms**. Mean is under 16.7 ms and p95 is under 25 ms.

### Merge conflicts resolved in the scratch tree

1. `auric-web/src/components/auric/laser_strip.tsx` was the same add/add as Run 1. Refs render the Lane 1 wheel strip. A call with no props renders the Lane 2 witness. Archived at `qa/archive/v2_laser_strip_add_add_conflict.tsx` and `qa/archive/v2_laser_strip_resolution.tsx`.
2. `auric-web/scripts/test-aliases.mjs` again differed by `await` on `module.register()`. Node v22.14.0 has no `registerHooks`, so the resolution keeps the `await`. Archived at `qa/archive/v2_test_aliases_content_conflict.mjs` and `qa/archive/v2_test_aliases_resolution.mjs`.
3. `folder-grid.tsx` was a content conflict. Lane 4's one-line `data-folder-id` was written against the pre-Lane-3 list, and git tried to splice that old `<ul>` into Lane 3's grid. The resolution keeps Lane 3's file. That grid already sets `data-folder-id={spec.id}` on each `<li>` (line 354), which is the selector Lane 4's test clicks. Archived at `qa/archive/v2_folder_grid_content_conflict.tsx`.

No conflict markers remain. `npm ci --ignore-scripts` on this scratch tree exited 0.

### Check results

| Check | Exit | Result |
|---|---|---|
| `npm ci --ignore-scripts` on the scratch tree | 0 | PASS |
| `npm run typecheck` | 0 | PASS |
| `npm run lint` | 0 | PASS, one existing warning in `termux-chrome.tsx:169` |
| `npm run build` | 0 | PASS. `db:migrate` skipped because `DATABASE_URL` is unset |
| `npm test` | 0 | PASS, 296 tests, 0 fail, 0 skipped |
| Lane 2/3/4 unit tests (not wired into `npm test`) | 0 | PASS, 34 tests, 0 fail, 0 skipped |
| `node --test scripts/l4_click_voice.test.mjs` | 0 | PASS, 1 test, 0 skipped |
| `node tests/e2e/l1_reels.e2e.mjs` | 0 | PASS |
| `AURIC_E2E_URL=http://127.0.0.1:8080 node --test tests/e2e/l2_reel_look.e2e.mjs` | 0 | PASS, 1 test, 0 skipped |
| `L3_E2E=1 AURIC_E2E_URL=http://127.0.0.1:8080 node --test tests/e2e/l3_*.e2e.mjs` | 0 | PASS, 7 tests, 0 skipped |
| `AURIC_E2E_URL=http://127.0.0.1:8080 node --test tests/e2e/l4_clicker_sounds.e2e.mjs` | 0 | PASS, 1 test, 0 skipped |
| FLIP 60fps gate | n/a (inside the L3 test) | PASS, mean 16.660 ms, p95 16.700 ms |
| Pixel diff versus lane screenshot files | n/a | L1 strip and light 0%. L1 demo 0.1884%. L2 settings 4.19–7.37% (shimmer). L3 icon shots grew 1469px to 1567px. L4 file was not rewritten by its test. |
| Pixel diff versus `auric-base` default captures | n/a | Folder list **2.5939% FAIL**. Timer 25.0149% (Lane 1 strips). |

`npm test` runner totals, in order: scripts 199/199, app-data and auth 55/55, `test:auric` 31/31, `test:l1` 11/11.

### Pixel diff by region

Method: pngjs, RGBA channels must match exactly. Viewport 390×844 = 329160 pixels. Animations disabled. `auric-base` at `http://127.0.0.1:8090` (`e39aa55`). Integration at `http://127.0.0.1:8080` (scratch `8c3aa93`). Shots: `qa/shots/v2_base_folder_list.png`, `qa/shots/v2_integration_folder_list.png`, `qa/shots/v2_diff_folder_list.png`.

Folder list: **8538/329160 = 2.5939%**. Four changed regions, and they sum to 8538. Nothing else in the viewport changed.

| Region | Box | Pixels | Percent | Cause |
|---|---|---|---|---|
| Status digit | x 281–288, y 202–213 (8×12) | 61 | 0.0185% | L4. `.tx-ok` reads `[ok] session started · folders 5 · no accounts` on base and `folders 6` on the integration. `clicker-sounds` is a free module. |
| Layout menu | x 309–356, y 346–393 (48×48) | 2132 | 0.6477% | L3. `<button class="tx-folder-menu-button">` text `⋯`, box x=309 y=346 w=48 h=48. The same point on base is the prompt `~ $ ls folders`. Base has no menu button. |
| Timer title | x 101–183, y 428–472 (83×45) | 3046 | 0.9254% | L3. `.tx-folder-name` "timer". See the ink rule below. |
| Stopwatch title | x 101–200, y 525–561 (100×37) | 3299 | 1.0022% | L3. `.tx-folder-name` "stopwatch". Same ink rule. |

Chrome band y 0–396 is 61+2132 = **2193 pixels = 0.6662%**. That is the number L3+L4 reported. It is the menu plus the digit, and it is under 1% by itself.

Title ink is 3046+3299 = **6345 pixels = 1.9276%**. Dropping the digit still leaves 2.5753%. Dropping the menu still leaves 1.9462%. The ink alone is over 1%.

Rows from y 562 through y 843 are **0 changed pixels**. That is not because those titles match. `.tx-actions` (HOME / CLOSE SESSION / MINIMIZE) sits at x=33 y=574.109 w=324 h=194 and covers them. `elementFromPoint(140, 631)` hits the HOME button. The alarm name is at y=630.875 and `clicker-sounds` is at y=728.875. Both compute to the same dark ink, and both are under the action bar in this viewport, so they add no pixels. Device at y=826.875 is the same.

#### Why the titles change

Both pages have `.tx-root[data-mood="light"]` and `--tx-ink: #161816` (rgb 22, 24, 22). The grid is `data-layout="big"`. Title font stays 14px with `text-transform: none`, so the grid-layout 16px uppercase rules do not apply.

Matched `color` rules on the first `.tx-folder-name`, from Chrome's CSS agent:

Base (`8090`):

- `.tx-folder-name` → `var(--tx-ink)`
- `.tx-folder-name` → `#f7fcff`

The second rule wins. Computed color is `rgb(247, 252, 255)`. Computed shadow is the dark shadow from that same rule in `styles.css`.

Integration (`8080`) has those two rules plus:

```css
.tx-root[data-mood="light"] .tx-folder-row .tx-folder-name,
.tx-root[data-mood="light"] .tx-folder-row .tx-folder-state {
  color: var(--tx-ink);
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.62), 0 0 3px rgba(255, 255, 255, 0.18);
}
```

That selector is in Lane 3's `src/styles/folders.css` (lines 163–167). Specificity beats the one-class `#f7fcff` rule. Computed color is `rgb(22, 24, 22)`. Computed shadow is `rgba(255, 255, 255, 0.62) 0px 1px 0px, rgba(255, 255, 255, 0.18) 0px 0px 3px`, which is that rule. The dirty boxes are taller than the 16px text line because the shadow changes with the fill.

The page was already in light mood on `auric-base`. Lane 3's selector is what repaints the titles.

#### Other pixel numbers

Timer viewport: **82339/329160 = 25.0149%**, the same count as Run 1. Base timer has 0 strips and an empty `--laser-color`. Integration timer has 2 strips and `--laser-color: #FF0000`. The largest cluster is x 33–356, y 288–629, 78429 pixels (23.8270%), the reel block where the Lane 1 banks land. This is the strip, not a Lane 2 settings failure.

Lane file diffs after the end-to-end run rewrote screenshots, versus the copies taken before that run:

| File | Result |
|---|---|
| `app-builder-built-mobile.png` | 390×844, 0/329160 = 0.0000% |
| `l1/laser_light.png` | 1280×3846, 0/4922880 = 0.0000% |
| `l1/laser_strip.png` | 332×49, 0/16268 = 0.0000% |
| `l1/laser_demo.png` | 1280×3846, 9275/4922880 = 0.1884% |
| `l2/beam_on_dark.png` | 390×844, 24250/329160 = 7.3672% |
| `l2/laser_settings.png` | 390×844, 22923/329160 = 6.9641% |
| `l2/number_contrast.png` | 390×844, 13801/329160 = 4.1928% |
| `l3/icons_3d.png` | size 324×1469 → 324×1567 |
| `l3/icons_etched.png` | size 324×1469 → 324×1567 |
| `l4/clicker_folder.png` | byte-identical to the committed file |

The Lane 2 percentages move with the unfrozen `reel-look-breathe` phase. Lane 3's icon files are one row taller because `clicker-sounds` is in the grid. Lane 4's end-to-end file does not write `screenshots/l4/clicker_folder.png`, so the 0% result is the untouched committed file.

### Raw output

Full logs are under `auric-web/qa/logs/` with the `v2_` prefix. The blocks below are the complete short logs and the summary lines of the long ones.

#### npm ci

Command: `cd /tmp/auric_l5_scratch_v2/auric-web && npm ci --ignore-scripts`

Exit: 0

```
npm warn deprecated recharts@2.15.4: 1.x and 2.x branches are no longer active. Bump to Recharts v3 to receive latest features and bugfixes. See https://github.com/recharts/recharts/wiki/3.0-migration-guide
npm warn deprecated eslint@9.39.5: This version is no longer supported. Please see https://eslint.org/version-support for other options.

added 425 packages, and audited 426 packages in 4s

94 packages are looking for funding
  run `npm fund` for details

2 high severity vulnerabilities

To address all issues, run:
  npm audit fix

Run `npm audit` for details.
```

#### typecheck

Command: `npm run typecheck`

Exit: 0

```
> typecheck
> tsc --noEmit
```

#### lint

Command: `npm run lint`

Exit: 0

```
> lint
> eslint .


/tmp/auric_l5_scratch_v2/auric-web/src/components/auric/termux-chrome.tsx
  169:17  warning  Fast refresh only works when a file only exports components. Use a new file to share constants or functions between components  react-refresh/only-export-components

✖ 1 problem (0 errors, 1 warning)
```

#### build

Command: `npm run build`

Exit: 0. Full log: `qa/logs/v2_build.txt`. Closing lines:

```
✓ built in 1.03s
ℹ Generated .vercel/output/nitro.json

[nitro] ✔ You can preview this build using npx vite preview
[nitro] ✔ You can deploy this build using npx nitro deploy --prebuilt

> db:migrate
> node scripts/migrate.mjs

[migrate] DATABASE_URL not set — skipping (the PGLite fallback migrates itself).
```

#### npm test

Command: `npm test`

Exit: 0. Full TAP: `qa/logs/v2_npm_test.txt`.

```
# tests 199
# pass 199
# fail 0
# skipped 0

# tests 55
# pass 55
# fail 0
# skipped 0

# tests 31
# pass 31
# fail 0
# skipped 0

# tests 11
# pass 11
# fail 0
# skipped 0
```

#### Lane unit tests

Command: the Lane 2, 3, and 4 unit files that `npm test` does not run.

Exit: 0. Full TAP: `qa/logs/v2_lane_unit.txt`.

```
# tests 34
# pass 34
# fail 0
# skipped 0
```

#### L4 script test

Command: `node --test scripts/l4_click_voice.test.mjs`

Exit: 0

```
TAP version 13
# Subtest: L4 click voice, folder prefs, and catalog tests pass
ok 1 - L4 click voice, folder prefs, and catalog tests pass
1..1
# tests 1
# pass 1
# fail 0
# skipped 0
```

#### L1 end to end

Command: `node tests/e2e/l1_reels.e2e.mjs`

Exit: 0

```
{"perfNormal":{"medianMs":16.69999999999891,"p95Ms":16.700000000000728},"perfThrottled":{"medianMs":16.69999999999891,"p95Ms":16.700000000000728}}
```

#### L2 end to end

Command: `AURIC_E2E_URL=http://127.0.0.1:8080 node --test tests/e2e/l2_reel_look.e2e.mjs`

Exit: 0

```
TAP version 13
# L2 motion median=16.70ms p95=16.70ms frames=61
# L2 apply frames=1 css=\#AABBCC beam=\#AABBCC
# Subtest: Laser & Numbers drafts, applies, discards, persists, and fits all target widths
ok 1 - Laser & Numbers drafts, applies, discards, persists, and fits all target widths
1..1
# tests 1
# pass 1
# fail 0
# skipped 0
```

#### L3 end to end

Command: `L3_E2E=1 AURIC_E2E_URL=http://127.0.0.1:8080 node --test tests/e2e/l3_*.e2e.mjs`

Exit: 0. Full TAP: `qa/logs/v2_l3_e2e.txt`.

```
# L3 layout FLIP frame interval: mean=16.660ms median=16.700ms p95=16.700ms max=16.700ms samples=[16.567999999999984,16.59999999999991,16.59999999999991,16.59999999999991,16.59999999999991,16.59999999999991,16.59999999999991,16.600000000000136,16.699999999999818,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045,16.700000000000045]
ok 4 - L3 icon styles cover every glyph and etched text stays matte and readable
# tests 7
# pass 7
# fail 0
# skipped 0
```

#### L4 end to end

Command: `AURIC_E2E_URL=http://127.0.0.1:8080 node --test tests/e2e/l4_clicker_sounds.e2e.mjs`

Exit: 0

```
TAP version 13
# L4 click latency [{"hint":0.01,"output":0.032},{"hint":0.01,"output":0.032}]
# Subtest: Clicker Sounds lists, selects, previews, and stays within the viewport
ok 1 - Clicker Sounds lists, selects, previews, and stays within the viewport
1..1
# tests 1
# pass 1
# fail 0
# skipped 0
```

#### Folder-list pixel compare

```
folder_list 390x844 changed=8538/329160 percent=2.5939
  x=101-200 y=525-561 100x37 px=3299 pct=1.0022
  x=101-183 y=428-472 83x45 px=3046 pct=0.9254
  x=309-356 y=346-393 48x48 px=2132 pct=0.6477
  x=281-288 y=202-213 8x12 px=61 pct=0.0185
  clusterSum 8538 unclustered 0
timer 390x844 changed=82339/329160 percent=25.0149
```

Mood and matched-style JSON: `qa/logs/v2_mood_base.json`, `qa/logs/v2_mood_integration.json`. Region boxes: `qa/logs/v2_regions_base.json`, `qa/logs/v2_regions_integration.json`. Cluster dump: `qa/logs/v2_pixel_regions.json`.
