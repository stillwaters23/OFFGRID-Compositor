# Lane A QA

**Lane A QA: FAIL**

Reviewed head: `386786d` (`copilot/lane-a-panel-ui`), base `final-build` at `9ec834c`. Checked 2026-10-09 14:19 UTC. This head includes the merge commit from `final-build`, so the original wrong-base/conflict finding is superseded. The merge is clean now, but it dropped important Lane A work. Do not merge until the failures below are fixed.

**CI:** No build on head `386786d`. The older build on `8963433` [completed successfully](https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/37933072983), but does not validate this head. Local `assembleDebug testDebugUnitTest lintDebug` is **BLOCKED**: Gradle cannot resolve `com.android.application:8.13.2` from Google Maven, Maven Central, or the Gradle Plugin Portal.

## Quality Bar

1. **FAIL:** `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/TerminalPanel.kt` remains 2,204 lines, over the lane’s 1,000-line limit; split it into focused files. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/TerminalPanel.kt:1-2204`.
2. **FAIL:** stale, false audio notes remain at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/EffectFolders.kt:134, 348, 494, 515`; replace with accurate mic and soundboard behavior. No CI run validates this head.
3. **FAIL:** naming/contracts are incomplete: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/effects/FxKeys.kt` lacks `fx.cam.on`, `fx.mic.on`, and the `fx.mic.*` contract keys. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/effects/FxKeys.kt:11-80`.
4. **BLOCKED:** no frame-time/profile evidence for panel drawing. The drawable’s render path is here, but timing evidence is absent: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/PanelGlassDrawable.kt:426-494`.
5. **BLOCKED:** no full grid/layout evidence on 360 dp, 412 dp, or target phone widths. `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/test/java/com/offgrid/compositor/terminal/PanelGeometryTest.kt` tests sizing math, not the required complete layout. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/test/java/com/offgrid/compositor/terminal/PanelGeometryTest.kt:7-50`.
6. **BLOCKED:** no screenshots or tests demonstrate eased animations and Remove animations behavior. Related resizing code: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/TerminalPanel.kt:986-1034`.
7. **FAIL:** tests exist for geometry/settings, but no test covers the exact camera/mic keys or `setToggleSetting`; those features are absent on this head. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/test/java/com/offgrid/compositor/terminal/PanelSettingsTest.kt:150-166`.
8. **FAIL:** user-facing audio notes are inaccurate at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/EffectFolders.kt:134, 348, 494, 515`.
9. **BLOCKED:** no error-path test or device evidence for a safe state after panel/action failures; action dispatch is at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/TerminalPanel.kt:835-840`.

## Lane A Requirements

- **A1 FAIL:** full-bleed sizing math has tests, but no rendered-border/corner test or emulator screenshot proves the required insets and lit edge pixels. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/test/java/com/offgrid/compositor/terminal/PanelGeometryTest.kt:15-23`. Add rendered-corner coverage and the requested device screenshot.
- **A2 FAIL:** `ResizeGrip` remains, contrary to the requirement to remove it, and edge/corner resize plus border-thickness acceptance is not implemented. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/TerminalPanel.kt:93, 302-312, 843, 1181-1197, 2179`. Remove the grip and references, implement the specified zones/thickness behavior, and add focused tests.
- **A3 FAIL:** no glow-curve test or frame-time evidence is present; the drawable draws the panel at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/PanelGlassDrawable.kt:426-494`. Add the 0/50/71/100 curve test and profile results.
- **A4 FAIL:** the required quick-toggle top bar is absent; `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/TopBar.kt` and `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/QuickToggleView.kt` were dropped by merge commit `77beeb9`. The remaining panel setup still constructs a resize grip and Back orb rather than the required camera/mic quick-toggle layout: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/TerminalPanel.kt:93-100`. Restore the views and add the requested alignment test.
- **A5 BLOCKED:** no evidence verifies live light/dark system-theme changes and persistence of custom colors. Add focused theme tests.
- **A6 PASS:** the explicitly requested side list was removed; `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/SideDrawer.kt` is deleted and the removed settings have a migration test. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/test/java/com/offgrid/compositor/terminal/PanelSettingsTest.kt:160-166`.
- **A7 BLOCKED:** no scroll-thumb positioning/drag test or requested screen recording is available; the view implementation is at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/GlowScrollBar.kt:12-101`.
- **A8 BLOCKED:** a Back orb exists at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/BackOrbView.kt:9-55`, but its position, accessibility behavior, and disabled Home state lack the required layout/device evidence.
- **A9 BLOCKED:** the start-look settings test exists, but there is no evidence for UV Black Light theme application/reset on device. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/test/java/com/offgrid/compositor/terminal/PanelSettingsTest.kt:31-49`.
- **A10 FAIL:** `TerminalPanel.setToggleSetting(key: String, on: Boolean)` no longer exists on head `386786d`; the Camera/Mic top-bar controls and `fx.cam.on`/`fx.mic.on` keys are also absent. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/TerminalPanel.kt:1-2204` and `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/effects/FxKeys.kt:11-80`. Restore the API and controls, with a test that verifies the exact keys.
- **A11 FAIL:** microphone effects folder and exact `fx.mic.*` contract rows are absent; Live/Remote folder actions are absent too. Add the microphone folder/keys; Lane C owns Live/Remote. Evidence: `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/PanelTree.kt:213-221` and `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/effects/FxKeys.kt:11-80`.
- **A12 FAIL:** the four stale audio notes remain at `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/EffectFolders.kt:134, 348, 494, 515`, and `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/TerminalPanel.kt:1-2204` exceeds the limit. Correct the notes and complete the split.

## Cross-Lane Findings

- **Setter correction:** Round 1 said Lane A delivered `setToggleSetting`; that is stale. It is absent on this head. Restore the public method before Lane B calls it.
- **EffectFolders correction:** Round 1 said Lane A did not change `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/EffectFolders.kt`; that is stale. It did update the file, but the notes at lines 134, 348, 494, and 515 still incorrectly say audio is not recorded/included.
- **Panel glow hook:** No public hook reading `MicPipeline.beats.level` is present or wired.
- **Base/merge:** The head now has `final-build` as its merge base, but merge commit `77beeb9` removed Lane A's `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/TopBar.kt`, `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/QuickToggleView.kt`, setter, camera/mic keys and other work. Restore the Lane A changes without dropping the base work.

## Rules

- **Permissions PASS:** this lane does not edit the manifest; `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/AndroidManifest.xml:4-5`.
- **No TTS PASS; no sliders PASS:** source greps returned no matches; the older CI run also passed the guards, but is not a build of this head.
- **No WebView PASS; no Marvel names PASS:** repository source greps returned no matches.
- **Panel not in recordings PASS:** this lane changes terminal UI only; it does not add panel drawing to the compositor/recorder path.
- **No secrets PASS:** no credential or key literals found in the reviewed change.
- **No unapproved deletions PASS:** `/home/runner/work/OFFGRID-Compositor/OFFGRID-Compositor/04 Build/app/src/main/java/com/offgrid/compositor/terminal/SideDrawer.kt` was explicitly requested to be removed; no other file deletion is part of the Lane A diff.
- **Start look BLOCKED:** settings tests cover start defaults, but no screenshot verifies the complete OLED theme/cursor rendering.
- **Platform safety BLOCKED:** target-device animation/face-safety checks are not supplied.

## Files Outside Lane A Ownership

None. The append-only status log is shared. The current defects are missing Lane A-owned files/contracts, not edits outside the lane.

## Needs Royalty's Phone

Verify 100/100 edge-to-edge rendering, rounded-corner border pixels, resizing zones, font sizes, scroll behavior, Back orb accessibility, animation settings, and frame times on the target phones. Keep verdict FAIL until the code and test gaps above are fixed.
