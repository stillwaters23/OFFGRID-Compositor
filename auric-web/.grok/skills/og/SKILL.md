# OG share cards

Games, whimsical apps, creative tools, and brand-forward pages ship their own
share card. Plain utilities may keep the platform placeholder. The card, the
X feed banner, and `src/lib/og/site.json` are handed over with
`scripts/write-atomic.mjs` so a reader never sees a half-written file. The
recipes live in `references/`.

The pass keeps `/workspace/.grok/og-pending` fresh while it generates. A marker
older than 10 minutes is stale: the parent treats that as a finished pass, not
as a cue to start another one.

## Brand-asset pass: never wait on the subagent

Launch this pass as soon as the name and palette settle, then keep building.
No `wait_tasks` and never `get_task_output` on that task. Reading the task
output suppresses its completion notification, so the result, failure included,
would reach nobody. Answer without it, and publish again when it wakes you if
the app already shipped with the placeholder.

Self-check the files on disk before reporting a pass:

```text
node scripts/brand-check.mjs --game
node scripts/brand-check.mjs --placeholder-ok
```

`--game` is the canvas and DOM-game gate. `--placeholder-ok` is only for a
plain utility whose favicon and title pass is expected to keep the placeholder.

## What the pass writes

- `public/og.jpg` — 1200×630 JPEG, under 600 KB (`ffmpeg -q:v 4`).
- `public/x-banner.jpg` — 1200×264 JPEG for the X feed card, same size budget.
- `src/lib/og/site.json` — `"card": "custom"`, and `"type": "x:game"` for a game.

Stage temps under `/workspace/.grok/`. Never stage inside `public/`: Vite
copies that directory into the deploy verbatim.
