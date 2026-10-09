# AURIC — Time, Crowned

Superior Rebuild v2 is the repaired source workspace for the AURIC timer, stopwatch, alarms, reminders, Jewel Reel, and Pavilion customization rooms. The active identity is AURIC 777 Blue Diamond. Operational colors remain user-selectable.

## Import / rebuild

The ZIP contains one `grok-workspace/` directory. Its contents form the application root. Keep `AGENTS.md`, `AGENTS.project.md`, the Grok infrastructure, `startup.sh`, npm scripts, configs, server middleware, and migrations together. The uploaded original ZIP is unchanged.

Use Node 22.15 or later; this release was checked with Node 24.19.0. For a development environment:

```sh
npm ci --ignore-scripts
npm run typecheck
npm run lint
npm test
npm run build
```

The Grok startup contract remains `startup.sh` → `npm run dev` on port 8080. Production previews use the existing `npm run preview:restart` script. Environment injection stays in `scripts/with-app-env.mjs`. Do not replace it with direct Vite calls or commit environment secrets.

Installed dependencies and `.vercel/output` are omitted from this source ZIP. The existing Vite/Nitro build recreates deployment output from source. No runtime image depends on an old generated-only copy. Database migration execution is conditional on the configured database; it was skipped during verification because no `DATABASE_URL` was supplied.

## Documentation

- `AURIC Time Crowned The Ice Pavilion Bible.md`: current product and architecture authority.
- `public/brand/README.md`: authoritative master, PNG exports, icon rules, and provenance.
- `artifacts/qa/REBUILD-REPORT.md`: fixes, verification, limitations, and remaining release checks.
- `artifacts/qa/check-results.json`: machine-readable check results.
- `artifacts/qa/cleanup-inventory.json`: each removed original file and its reason.
- `artifacts/qa/source-preservation.json`: original source accountability and retained-file hashes.

The original Bible and important historical Markdown are preserved. The current Bible takes precedence over historical design plans and generic platform examples for AURIC-specific branding and architecture.

## Release scope

The app stores settings and timing history locally. Keep the existing site origin to retain browser storage during deployment updates. The local PIN is a privacy gate, not encryption or an account system.

Browser/device QA remains required before a paid public launch. This web app cannot guarantee alarms when the browser is closed or suspended; its floating chip stays inside AURIC. Crown is an explicitly labeled pricing preview and does not collect payments. See the QA report for the precise checks that were and were not performed.
