# Render probe in-app Copy/Share, 2026-10-16

Branch `render_probe_share` from `final-build`. QA recommendation from `2026_10_15_phase_0_qa_review.md`.

## Built

- **RenderProbe (debuggable only):** ring of the last 200 lines, logcat unchanged; on each bind writes `filesDir/probe/cold_start.txt` (first bind after process start) or `filesDir/probe/resume.txt` (later binds).
- **Panel:** `SYSTEM` > `Diagnostics` > `Render probe` (debug APK only): selectable cold/resume text, **Copy** and **Share** (`ACTION_SEND` text/plain). Refuses to open while recording. No new permissions, no stage drawing, no toast on copy/share.
- **Tests:** `ProbeLineBufferTest`, bind capture cold/resume in `RenderProbeTest`, panel folder presence in `RenderProbePanelTest`.

## Royalty (phone only)

1. Install the CI debug APK. Force stop, open OFFGRID (cold start), use the camera briefly.
2. Open the panel: **SYSTEM** > **Diagnostics** > **Render probe**.
3. Tap **Share** and pick Messages, Gmail, or Notes; or tap **Copy** and paste into chat.
4. For a resume capture: switch away and back (or otherwise trigger a camera rebind), reopen **Render probe**, share again. The resume section updates.
