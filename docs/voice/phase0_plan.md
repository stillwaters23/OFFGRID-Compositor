# Voice Phase 0 plan

GLM-5.3's review of the voice changer is **GO WITH CHANGES**. Hygiene starts now. Route A (Shizuku virtual-mic injection) waits for an on-device Gate G1 pass. This repository has no root, LSPosed, or Magisk path, and this change adds no Route A code.

This change is hygiene only. The app's behavior stays as it is on `final-build`.

## GLM top 5 changes

These are the review's top five changes. They direct later phases. They are not implemented in this change.

1. **Live conversion uses the mix plus OFFGRID's own UID exclusion.** Same-phone live conversion is an extension of Route A when BIGO's record is served from the remote submix and the mix's exclusion rule keeps OFFGRID on the real microphone. The AccessibilityService idea is dropped.
2. **UID scoping is a hard arming requirement.** An unscoped mix would redirect every matching capture on the phone. The arming code refuses to arm until the target package and UID are resolved, and the status line shows both. The first spike task resolves which UID holds the capture during a live call, including the case where a container owns BIGO's UID.
3. **Fail-open protection is designed before any Route A code.** If the helper dies mid-call, the other app must not fall through to Royalty's physical microphone. The design is a dead-man's switch, a binder-death watchdog with a loud notification, and mute-until-green. The worst outcome the review names is Royalty's real voice leaving the phone with no warning.
4. **The spike is Gate G1, with a written matrix and a second BIGO account.** Shell grants, hidden APIs, scoping, exclusion, live-while-injecting, late registration, AEC, formats, Bluetooth and wired, kill fallback, and a real call each get a pass or fail in the logs. A partial pass, including "works only when unscoped," is a fail.
5. **The unnamed engineering gaps close before product code depends on them.** Hidden-API compile and R8 keep rules, plus a startup symbol-resolution self-test. A dumb-sink AIDL transport from the unprivileged app. The helper does not inherit MicUp's `MODE_IN_COMMUNICATION`, focus ducking, or `Runtime.exec` patterns. On-device FLAC encode is not a dependency: WAV and FLAC are imported, and transcoding happens on a PC. The battery-optimization screen opens with `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, which needs no extra permission.

What the review left standing: the evidence discipline, the route ranking, the MicUp reuse and licensing conclusions, moving the voice masters out of this repo, and the treatment of RTMP and the container as unknowns. The fixed constraints stay: non-root, no sliders, offline-first, no secrets.

## Gate G1 test matrix

Gate G1 is an on-device spike on the Galaxy S25. It is a standalone spike, outside this app's production path. No Route A code lands in this repository until the gate is recorded as a pass.

Every BIGO call in this matrix uses a **second BIGO account**. Royalty's main account is not the test account.

**Auto-cutoff** means the injected path cuts itself off when the helper dies or a check is not green. The other app receives silence, or the documented cutoff tone. It does not receive the physical microphone.

**Watchdog** means the spike observes binder death of the helper and records that observation with the auto-cutoff result. The later product bar, from the review's Phase 3, is a route-lost notification within 2 seconds. G1 records whether death was detected fast enough to support that bar. Undetected death fails the watchdog row.

**Muted until all checks are green** means arming stays off, and the far side stays muted, until every row below is green. One red row keeps the mute. An unscoped-only result is a red row.

Gate rule, from the review: a pass opens the later Route A product work. A fail shelves Shizuku. The hardware route (Route B) and the in-app mux (Route C) stay available either way.

| # | Check | Pass | Fail |
| --- | --- | --- | --- |
| 1 | On-device spike: shell grant | On the S25, One UI 8, `dumpsys package com.android.shell` shows `MODIFY_AUDIO_ROUTING`. The log file is `shell_perms.txt`. | The shell user lacks the grant. Route A stops. |
| 2 | On-device spike: target UID | During a live call, `ps -A` and `dumpsys media.audio_flinger` name the UID that holds the capture. That UID may be BIGO's, or the container's if BIGO is running inside one. The log file is `target_uid.txt`. | The capturing UID is unknown. |
| 3 | On-device spike: hidden API | A standalone spike builds the preset-matched mix and writes a 440 Hz tone. A harness recording `VOICE_COMMUNICATION` stays above the RMS gate for at least 10 seconds. `dumpsys media.audio_policy` shows the mix. Symbols resolve at startup. | The tone is missing, the mix is missing, or a symbol fails to resolve. |
| 4 | UID scoping | The mix matches the harness UID only. A third app's capture stays on the physical microphone. The log is a three-app log. | Any capture outside the target UID hears the injected audio. This row fails the whole gate, including an otherwise working unscoped mix. |
| 5 | Own-UID exclusion | For 60 seconds the harness records the tone while OFFGRID's excluded capture of the physical microphone stays non-silent. Both streams are non-silent in the log. | Either stream is silent, or the two apps contend for one microphone. |
| 6 | Late registration | The mix is registered after the harness has already started recording. The log says whether the open record re-routes. | The result is unrecorded. Late registration is the recovery path, so an unrecorded result fails the row. |
| 7 | Format, AEC, Bluetooth, wired | One row each: AEC on, a 16 kHz mono client, Bluetooth, and a wired CTIA headset. Each is pass or fail in the log. | A missing row, or an unrecorded device. |
| 8 | 30-minute stability | The tone stays above the RMS gate for 30 minutes. | Dropouts, or the helper dies without the watchdog row recording it. |
| 9 | Auto-cutoff | The helper is killed mid-capture. The harness goes silent, or plays the documented cutoff tone. The physical microphone is not the source. | The harness falls open to the live microphone. |
| 10 | Watchdog | The spike log records binder death of the helper in the same run as row 9, and records the time from kill to cutoff. Undetected death fails. The log notes whether that time can support the later 2-second route-lost notification. | Death is invisible, or the log has no time. |
| 11 | Muted until all checks are green | Rows 1–10 and row 12 are green before anything is unmuted. The spike log contains one explicit "muted until green" line that stays muted while any row is red. | Audio is released while any row is red, or the line is missing. |
| 12 | Second BIGO account | A second account, not Royalty's main account, hears the Rex tone for at least 5 minutes. BIGO does not crash. The pass/fail decision is written down. | The call uses the main account, the far side never hears the tone, BIGO crashes, or no decision is written down. |

Rows 9, 10, and 11 are the fail-open protection from the review's finding E6. They are part of the gate, so Route A product code is not written ahead of them.

## Foreground service types

`RECORD_AUDIO` stays in `04 Build/app/src/main/AndroidManifest.xml`. The merged-manifest permission check in `.github/workflows/build-debug-apk.yml` stays as it is on `final-build`, including the rule that `RECORD_AUDIO` must be present and the current allowlist of permission names.

The only service in the tree is `com.offgrid.live.LiveService`. It is the floating overlay and live camera service. Its foreground type is `specialUse|camera|microphone`. That declaration belongs to live streaming. It is not a voice service.

A voice service does not exist, so this change adds no service and does not add `mediaPlayback`.

When a voice service is added, that same change does all of the following:

- The service's `android:foregroundServiceType` includes `microphone` and `mediaPlayback`.
- The manifest declares `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. `FOREGROUND_SERVICE_MICROPHONE` is already declared for `LiveService`.
- `android.hardware.microphone` is declared with `android:required="false"`, so a missing microphone does not block install.
- The permission allowlist in `.github/workflows/build-debug-apk.yml` gains `FOREGROUND_SERVICE_MEDIA_PLAYBACK` in that same change. The check fails the build on a permission that is not in the list, so the list and the manifest move together.
- `RECORD_AUDIO` remains declared. The check that requires it remains.

Route C playback is the reason `mediaPlayback` is on that future service. Route A still waits for Gate G1.

## Where the build flavors go

Flavors are not created in this change. Adding them would change the debug task names (`assembleDebug` becomes a flavored task) and would change which permissions the built app merges. Today's single variant stays.

They belong in `04 Build/app/build.gradle.kts`, inside the `android` block, beside `buildTypes`:

```kotlin
flavorDimensions += "distribution"
productFlavors {
    create("offline") {
        isDefault = true
    }
    create("full") {}
}
```

Flavor manifests, when that change is made:

- `04 Build/app/src/offline/AndroidManifest.xml`
- `04 Build/app/src/full/AndroidManifest.xml`

`offline` is the default. It ships with no `INTERNET` permission and no Shizuku permission. Route D (RTMP / OBS) is absent. The later acceptance test is an airplane-mode cold start that plays a test clip.

`full` is the only flavor that carries Route D, and only behind an explicit opt-in. The review keeps Route D out of the offline promise.

`:live` declares `INTERNET` today for the local Send to PC link (`04 Build/live/src/main/AndroidManifest.xml`). The app module also declares `INTERNET` for that link and the direct phone link. An offline manifest has to remove the merged `INTERNET` permission, because a library manifest still merges into the app. That removal is part of the flavor change, not this one. The current build keeps `INTERNET` for the local link.

The Shizuku permission, if Route A is ever armed, belongs only on `full`, and only after Gate G1.

## WAV move plan

The Rex masters are ordinary git blobs today, not Git LFS pointers. Measured sizes: Rex 01 is 19 MB, Rex 02 is 18 MB, Rex 03 is 18 MB.

Tracked paths, exact:

- `06 Prompts and Skills/Rex Voice/Rex 01.wav`
- `06 Prompts and Skills/Rex Voice/Rex 02.wav`
- `06 Prompts and Skills/Rex Voice/Rex 03.wav`

This change leaves those files where they are. It does not delete them, move them, or rewrite history.

Destination: a private repository named `OFFGRID_Voice_Assets`, with Git LFS for the masters.

Later steps, after Royalty's history decision:

1. Create `OFFGRID_Voice_Assets` as a private repository. Confirm with the GitHub API that it is private.
2. Turn on Git LFS in that repository and track `*.wav`. Reference audio lives there. Model weights (`.gguf`, `.onnx`, `.pt`, `.safetensors`) stay out of git; the hygiene check in this repository rejects them.
3. Add the three masters there and confirm `git lfs ls-files` lists them as LFS objects.
4. After that private copy is verified, a normal commit in this repository can remove the three paths. That commit does not rewrite history.
5. The same commit removes those three paths from `ALLOWED_WAV_PATHS` in `.github/scripts/repo_hygiene.py` and from the comment in `.github/workflows/build-debug-apk.yml`. The check then fails if any WAV remains tracked.

Until that commit, the CI allowlist is those three paths and no others. The check is `.github/scripts/repo_hygiene.py`, run from the "Build debug app" workflow.

## Open question for Royalty

The three masters are already in git history. Royalty decides whether that history stays, or whether a separate reviewed history rewrite runs after the private repository holds a verified copy. This phase does not choose, and it does not rewrite history.

A second decision waits until anyone invests in Route A: Royalty accepts or rejects the per-boot Wireless Debugging ritual the review describes. That decision is not required to land this hygiene change.
