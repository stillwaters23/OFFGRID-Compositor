# Independent QA: voice Phase 0 hygiene, PR 54

Reviewer: independent QA. This note did not change product code. Nothing was merged. `final-build` and `main` were not pushed. History was not rewritten.

| | |
| --- | --- |
| Pull request | [#54](https://github.com/stillwaters23/OFFGRID-Compositor/pull/54), draft, `cursor/voice-phase0-hygiene-ae3d` into `final-build` |
| Head reviewed | `387dd45c5b9e8d6d49b971b3d44fefd69cb5ae95` |
| Base | `origin/final-build` at `541588cb9208e06a9a3292a91af70446abfbd05d` |
| CI | [run 39084968489](https://github.com/stillwaters23/OFFGRID-Compositor/actions/runs/39084968489), conclusion success, head SHA matches |
| Review time | 2026-10-18 UTC |

`git log origin/final-build..387dd45` is one commit: `387dd45 Add voice Phase 0 hygiene files and asset guard.`

## Verdicts

| # | Check | Result |
| --- | --- | --- |
| 1 | No product code or manifest changes. `RECORD_AUDIO` and the merged-manifest check are unchanged. | **PASS** |
| 2 | `repo_hygiene.py` fails on a new `.wav`, `.gguf`, `.onnx`, `.pt`, or `.safetensors`, and on a fake private key or API key. Allowlist is the 3 Rex WAV paths by exact path. Output does not contain the fixture secrets. | **PASS** |
| 3 | The workflow still builds and keeps every existing check. | **PASS** |
| 4 | `THIRD_PARTY_NOTICES.md` covers every Gradle dependency with the published license. `LICENSE` is proprietary, all rights reserved, stillwaters23 2026. | **PASS** |
| 5 | `phase0_plan.md` matches GLM's top 5 and the Gate G1 matrix (on-device spike, second BIGO account, auto-cutoff, watchdog, muted until green), with no Route A code and no sliders. | **BLOCKED** |
| 6 | `./gradlew testDebugUnitTest lintDebug assembleDebug` in `04 Build`. | **PASS** |

## 1. Product code and manifests — PASS

`git diff --name-status origin/final-build...387dd45`:

```
A	.github/scripts/repo_hygiene.py
M	.github/workflows/build-debug-apk.yml
A	LICENSE
A	THIRD_PARTY_NOTICES.md
A	docs/voice/phase0_plan.md
```

463 insertions, 0 deletions. Diff limited to `*.kt`, `*.kts`, `*.java`, `*.xml`, `*.gradle`, and `*.toml` is empty. `04 Build/app/src/main/AndroidManifest.xml` and `04 Build/live/src/main/AndroidManifest.xml` have an empty diff against `origin/final-build`.

`RECORD_AUDIO` is still declared at `04 Build/app/src/main/AndroidManifest.xml:7`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

The merged-manifest step is still named "Check the built app only asks for allowed permissions". After removing the new voice-asset step, `.github/workflows/build-debug-apk.yml` is byte-identical to `origin/final-build`. The `allowed` set in that step is the same string on both sides, and `RECORD_AUDIO` still occurs 4 times in the workflow on both sides. The step still exits if `android.permission.RECORD_AUDIO` is missing (`.github/workflows/build-debug-apk.yml:237-240` on this head).

CI run 39084968489 printed the merged permission list and then `RECORD_AUDIO is allowed.` and `Every permission is on the allowed list.` The list was:

- `android.permission.CAMERA`
- `android.permission.DETECT_SCREEN_RECORDING`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_CAMERA`
- `android.permission.FOREGROUND_SERVICE_MICROPHONE`
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- `android.permission.INTERNET`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.RECORD_AUDIO`
- `android.permission.SYSTEM_ALERT_WINDOW`
- `com.offgrid.compositor.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`

## 2. Asset and secret guard — PASS

Allowlist in `.github/scripts/repo_hygiene.py:21-27` is exactly:

- `06 Prompts and Skills/Rex Voice/Rex 01.wav`
- `06 Prompts and Skills/Rex Voice/Rex 02.wav`
- `06 Prompts and Skills/Rex Voice/Rex 03.wav`

`git ls-files` for `*.wav`, `*.gguf`, `*.onnx`, `*.pt`, and `*.safetensors` returns those three WAV paths and nothing else. A clean run exits 0 and prints `Voice asset and secret check passed.` plus those three paths. CI printed the same pass line.

Temporary untracked files were created under the repo, the script was run, and the files were deleted. They were not committed. `git status` was clean after the run.

| Fixture | Exit | Finding printed |
| --- | --- | --- |
| `qa-hygiene-tmp/new.wav` | 1 | `New WAV file is not allowlisted: qa-hygiene-tmp/new.wav` |
| `qa-hygiene-tmp/m.gguf` | 1 | `Model file is not allowed in git: qa-hygiene-tmp/m.gguf` |
| `qa-hygiene-tmp/m.onnx` | 1 | `Model file is not allowed in git: qa-hygiene-tmp/m.onnx` |
| `qa-hygiene-tmp/m.pt` | 1 | `Model file is not allowed in git: qa-hygiene-tmp/m.pt` |
| `qa-hygiene-tmp/m.safetensors` | 1 | `Model file is not allowed in git: qa-hygiene-tmp/m.safetensors` |
| `qa-hygiene-tmp/Rex 01.wav` (same basename, other directory) | 1 | `New WAV file is not allowlisted: qa-hygiene-tmp/Rex 01.wav` |
| `qa-hygiene-tmp/clip.WAV` | 1 | `New WAV file is not allowlisted: qa-hygiene-tmp/clip.WAV` |
| `06 Prompts and Skills/Rex Voice/Rex 04.wav` | 1 | `New WAV file is not allowlisted: 06 Prompts and Skills/Rex Voice/Rex 04.wav` |
| `qa-hygiene-tmp/note.txt` with a PEM private-key header | 1 | `qa-hygiene-tmp/note.txt:1: private key` |
| `qa-hygiene-tmp/note.txt` with a quoted `api_key` assignment | 1 | `qa-hygiene-tmp/note.txt:1: quoted api key or token` |
| `qa-hygiene-tmp/note.txt` with an `AKIA` access-key id | 1 | `qa-hygiene-tmp/note.txt:1: aws access key id` |

Each failing run was alone, so one extension did not hide another. Stdout and stderr of every run were searched for the fixture secret strings. None of those strings appeared. The finding line is the path, the line number, and the kind name.

## 3. Workflow still builds and keeps every check — PASS

`origin/final-build` has 15 steps. This head has those 15 plus `Check voice assets and obvious secrets`, inserted after `Check the new files for saved secrets`. Steps present on `final-build` and still present here, in the same order:

1. Get the code
2. Set up Java
3. Set up Android tools
4. Check the new files for saved secrets
5. Check capture uses the processed mic path
6. Check voice conversion stays on device
7. Check there are no sliders except BioSliderView
8. Check for OS emoji in the panel sources (warn mode)
9. Build the debug app (`./gradlew testDebugUnitTest assembleDebug --stacktrace`)
10. Run Android lint (`./gradlew lintDebug --stacktrace`)
11. Check the color mix math (`./gradlew testDebugUnitTest --stacktrace`)
12. Check the built app only asks for allowed permissions
13. Name the app file
14. Upload the app file
15. Attach the app file to the preview release

CI run 39084968489 completed every step with success except "Attach the app file to the preview release", which was skipped. That step's condition is unchanged: it runs on a push to `final-build`. This run was a pull request. The build step itself succeeded (`BUILD SUCCESSFUL in 3m 30s`), and lint succeeded (`BUILD SUCCESSFUL in 1m 28s`). The local Gradle run in check 6 also assembled the debug APK.

## 4. Notices and LICENSE — PASS

Direct dependencies were read from `04 Build/build.gradle.kts`, `04 Build/app/build.gradle.kts`, and `04 Build/live/build.gradle.kts`. `project(":live")` is the first-party module the notice names. No other `implementation` or `testImplementation` coordinate is declared. Each published POM was fetched on 2026-10-18.

| Coordinate | POM license name | Notice |
| --- | --- | --- |
| `com.android.tools.build:gradle:8.13.2` (application and library plugins) | The Apache Software License, Version 2.0 | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21` | Apache-2.0 | Apache-2.0 |
| `androidx.core:core-ktx:1.16.0` (app and live) | The Apache Software License, Version 2.0 | Apache-2.0 |
| `androidx.appcompat:appcompat:1.7.1` | The Apache Software License, Version 2.0 | Apache-2.0 |
| `androidx.activity:activity-ktx:1.10.1` | The Apache Software License, Version 2.0 | Apache-2.0 |
| `androidx.camera:camera-core:1.6.2` | Apache-2.0 and BSD-3-Clause | Apache-2.0 and BSD-3-Clause |
| `androidx.camera:camera-camera2:1.6.2` | The Apache Software License, Version 2.0 | Apache-2.0 |
| `androidx.camera:camera-lifecycle:1.6.2` | The Apache Software License, Version 2.0 | Apache-2.0 |
| `androidx.camera:camera-video:1.6.2` | The Apache Software License, Version 2.0 | Apache-2.0 |
| `androidx.camera:camera-view:1.6.2` | The Apache Software License, Version 2.0 | Apache-2.0 |
| `androidx.media3:media3-exoplayer:1.11.1` | The Apache Software License, Version 2.0 | Apache-2.0 |
| `androidx.media3:media3-common:1.11.1` | The Apache Software License, Version 2.0 | Apache-2.0 |
| `androidx.media3:media3-datasource:1.11.1` | The Apache Software License, Version 2.0 | Apache-2.0 |
| `com.google.mediapipe:tasks-vision:0.10.35` | The Apache Software License, Version 2.0 | Apache-2.0 |
| `junit:junit:4.13.2` (app and live) | Eclipse Public License 1.0 | EPL-1.0 |
| `org.robolectric:robolectric:4.17` (app and live) | The MIT License | MIT |

The `camera-core` POM description says: "This library includes code from libyuv, which is licensed under the BSD-3-Clause license." The other CameraX 1.6.2 artifacts publish Apache-2.0 only. That matches `THIRD_PARTY_NOTICES.md`.

`04 Build/gradle/wrapper/gradle-wrapper.properties` pins `gradle-8.14.5-bin.zip`. The Gradle 8.14.5 `LICENSE` file is the Apache License 2.0, as the notice says.

`LICENSE` is 11 lines. It says `Copyright © 2026 stillwaters23`, `All rights reserved.`, and `OFFGRID Compositor is proprietary.`

## 5. Phase 0 plan against GLM and Gate G1 — BLOCKED

The named Gate G1 pieces are in `docs/voice/phase0_plan.md`:

| Required piece | Where |
| --- | --- |
| On-device spike | Opening of the matrix, and rows 1–3 (shell grant, target UID, hidden API) at lines 35–37 |
| Second BIGO account | Prose before the table, and row 12 at line 46 |
| Auto-cutoff | Definition before the table, and row 9 at line 43 |
| Watchdog | Definition before the table, and row 10 at line 44 |
| Muted until green | Definition before the table, and row 11 at line 45 ("Muted until all checks are green") |

The plan says this change adds no Route A code (`docs/voice/phase0_plan.md:3`). The diff adds no Kotlin, manifest, or Gradle product file. The flavor snippet in the plan is documentation of a later `productFlavors` block. It is not in `04 Build/app/build.gradle.kts`.

The plan states the fixed constraint "no sliders" (`docs/voice/phase0_plan.md:17`). This diff adds no slider widget. The workflow step "Check there are no sliders except BioSliderView" is unchanged, and CI printed `No slider other than BioSliderView is in the sources.`

The match to GLM's own top 5 is blocked. The authoring run was given an upload named `GLM_5.3_Voice_Review.md` (read path `GLM_5.3_Voice_Review_3795.md`). That file is not in this checkout, not in git history, and not in the authoring transcript body. Slack and Google Drive searches for the review returned nothing. `docs/voice/phase0_plan.md` has a section titled "GLM top 5 changes" with five numbered paragraphs, and those paragraphs were written by the authoring agent. They could not be compared with GLM's text from here.

## 6. Gradle totals — PASS

Command, from `04 Build`, on this head:

```
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Exit 0. `BUILD SUCCESSFUL in 2m 39s` (115 actionable tasks). The machine had no Android SDK, so the run installed command-line tools, `platforms;android-36`, and `build-tools;36.0.0` under the home directory first. JDK was OpenJDK 21. No SDK file was committed.

Unit-test XML under `app/build/test-results/testDebugUnitTest` and `live/build/test-results/testDebugUnitTest`:

| Module | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| app | 252 | 0 | 0 | 0 |
| live | 39 | 0 | 0 | 0 |
| **Total** | **291** | **0** | **0** | **0** |

Lint XML:

| Module | Errors | Warnings |
| --- | ---: | ---: |
| app | 0 | 140 |
| live | 0 | 30 |

`assembleDebug` wrote `04 Build/app/build/outputs/apk/debug/app-debug.apk` (71,241,071 bytes).

## One-line verdict

BLOCKED: checks 1, 2, 3, 4, and 6 pass; check 5 is blocked because GLM's review text was not available to compare with the plan's top 5.
