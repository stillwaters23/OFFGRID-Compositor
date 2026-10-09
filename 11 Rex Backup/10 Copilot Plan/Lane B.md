# Lane B: Camera, Mic and Recording

**Branch from:** `final-build`. **Branch name:** `lane-b-camera-mic`. **PR into:** `final-build`, titled `Lane B: Camera, Mic and Recording (Do not merge, Rex reviews)`.
**Read first:** `docs/10 Copilot Plan/Copilot Master Plan.md` sections 1 to 6.
**Covers Royalty's phone-test item:** 10 (camera and mic toggles wired to the pipeline, mic permission, Microphone Effects processing, audio in recordings), plus CI fixes and finishing effects Claude left incomplete.

Royalty **now authorizes the microphone** (Oct 9, 2026). Rex voice and typed-to-speech (Step 7) stay paused: no `TextToSpeech`, no `SpeechRecognizer`, no `android.speech`.

## Files You Own

`MainActivity.kt`, `CompositorProcessor.kt`, `CompositorEffect.kt`, `CompositorDrawer.kt`, `GlTools.kt`, `StagePainter.kt`, `StageShaders.kt`, `OverlayRenderer.kt`, `ReplayBuffer.kt`, `SoundBoard.kt`, `GallerySaver.kt`, `EffectsController.kt`, `FaceTracker.kt`, everything in `effects/` **except** `FxKeys.kt`, a new package `audio/`, `app/src/main/AndroidManifest.xml`, `.github/workflows/build-debug-apk.yml`, a new `res/values/strings_audio.xml`, `README.md`, and tests in `app/src/test/.../effects/` and a new `.../audio/`.

Do **not** edit anything in `terminal/`, `FxKeys.kt`, `strings.xml`, or the `live` module. The panel UI for your features (top-bar toggles, 📁 Microphone Effects folder) is built by Lane A. You read its settings by the exact key strings in the master plan contracts; declare them in `audio/MicKeys.kt`.

## Requirements and Acceptance Checks

### B0. CI and Guards First (Do This in Your First Commit)

In `.github/workflows/build-debug-apk.yml`:
- Add `final-build` to `on.pull_request.branches` so every lane PR into `final-build` runs CI.
- Mic guard: replace "no RECORD_AUDIO" with an **allowlist check** on the merged manifest: allowed permissions are exactly `CAMERA`, `RECORD_AUDIO`, plus Lane C's list (`SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CAMERA`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, `INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `NEARBY_WIFI_DEVICES`, `CHANGE_WIFI_MULTICAST_STATE`). Any other permission fails the build. Print the list.
- Voice guard: keep banning `TextToSpeech|SpeechRecognizer|android\.speech`; **stop banning `AudioRecord`** (mic effects need it).
- Run all greps over every module: `04 Build/*/src` instead of only `app/src/main/java`.
- Keep the secret scan and slider guard. Keep publishing the APK only on pushes to `final-build`.
- Update the release notes text: the mic is now allowed and used for videos.

Acceptance: CI runs on your PR into `final-build`; a deliberate test of the allowlist (documented in the PR) fails on an extra permission.

### B1. Manifest and Permission Flow

- Remove the `RECORD_AUDIO` `tools:node="remove"` entry and add `<uses-permission android:name="android.permission.RECORD_AUDIO" />`. Keep `ACCESS_NETWORK_STATE` removed.
- Ask for the mic **only when Royalty turns the mic toggle on** (or first records with mic on), with a plain explanation line on the panel first. If denied: the mic toggle turns back off (call `TerminalPanel.setToggleSetting("fx.mic.on", false)`, which Lane A adds; until Lane A is merged, keep the mic off in your own state and add that one call when you merge `final-build` in), and the record bar says how to allow it in Settings.
- Update the CameraX pipeline comments ("Audio stays off") and README sections that say there is no mic.

### B2. Camera and Mic Toggles Drive the Pipeline (Item 10)

- `fx.cam.on` **off:** unbind the camera use cases so the hardware is released (the green privacy dot goes away). The screen and any recording show OLED black with the drawn effects still running (drive the compositor from a 30 fps timer with a black input when the camera is off) so a stream never shows a frozen face. Status line: "Camera is off. Viewers see black and your effects." **On:** rebind and resume. Face safety rules still apply: if the camera comes back on with the face diamond or a no-face mode active, the first frame is already covered.
- `fx.mic.on` **on:** recordings include audio (B3). **Off:** no audio track or a silent track (pick one and document it); `AudioRecord` is stopped and released (the orange mic dot goes away).
- Toggling during a recording: camera off keeps recording with black frames; mic off writes silence for the rest of the clip. Never crash, never drop the file.

Acceptance: logcat or a unit test of the state machine shows camera unbind and rebind and `AudioRecord` start and release on each toggle; a recorded test clip with a camera-off segment and a mic-off segment plays in the gallery.

### B3. Audio in Recordings, with Microphone Effects Applied

CameraX `Recorder.withAudioEnabled()` records the raw mic and cannot apply effects. Build our own path:

- New `audio/MicEngine.kt`: `AudioRecord` at 48 kHz mono 16-bit (`VOICE_RECOGNITION` or `CAMCORDER` source, whichever gives less processing; document the choice), a processing thread, and a lock-free ring buffer.
- New `audio/VoiceFx.kt`: on-device DSP only, no cloud, no ML voice cloning:
  - Pitch shift (0 to 100, 50 natural, about one octave down to one octave up) with a granular or phase-vocoder method that keeps duration.
  - Robot (ring modulator around 30 to 120 Hz plus light bit crush), amount 0 to 100.
  - Echo (delay 50 to 800 ms, feedback 0 to 0.85).
  - Radio (band-pass 300 Hz to 3.4 kHz plus soft clip).
  - Cave (Schroeder or Freeverb style reverb), amount 0 to 100.
  - Mic volume (gain) and noise gate.
  - Each stage is skipped when off. Total added latency under 60 ms.
- New `audio/AvRecorder.kt`: H.264 video from a `MediaCodec` input surface that the compositor draws into (the same way `ReplayBuffer` gets a surface today, see `CompositorProcessor.setReplay`) plus AAC-LC 128 kbps audio from the processed mic, muxed with `MediaMuxer` into MP4 with correct A/V sync (audio timestamps from `AudioRecord` frame counts, video from the stage's presentation times). Use it for all recordings (mic on or off) so one path is tested; keep the CameraX `VideoCapture` fallback only if `AvRecorder` fails to start, and say so on the status line.
- `fx.mic.sounds_in_video` on: mix the soundboard's generated sounds into the recorded audio (SoundBoard already synthesizes them; expose PCM). Also mix the radar ping (finishing feature 17's audio part).
- The **replay buffer** also keeps audio when the mic is on.
- Live monitoring through the speaker is **off** (feedback risk); only with wired or Bluetooth headphones, as a toggle request to Lane A if wanted.
- The panel is still never in the video (the compositor never sees the panel).

Acceptance: unit tests for each DSP stage (pitch 50 is near-identity, echo delay matches the typed value within 5 ms, gate silences low noise, no NaN or clipping beyond full scale); an instrumented or Robolectric test that `AvRecorder` writes an MP4 with one video and one audio track; manual check list in the PR: record 20 s with Robot plus Echo on, play it back, voice is changed and in sync (lip sync within 80 ms).

### B4. Beat Glow (Optional Inside This Lane, Mic Required)

- `audio/BeatDetector.kt`: RMS envelope plus onset detection on the mic stream; exposes a 0 to 1 level and beat events at no more than 8 per second.
- When `fx.mic.beat_glow` is on, the **glow border layer** in the video pulses with the beat (cap brightness changes to 3 flashes per second). The panel's own glow pulsing to audio is a later integration (needs a small public hook in Lane A's code; list it under "Requests for other lanes").

### B5. Finish What Claude Left Incomplete

- Verify every effect in 📁 Effects actually renders on device-size frames and is reachable after Lane A's reorg (keys unchanged). Fix any that do nothing.
- Radar ping and soundboard into the video's audio (B3).
- `ReplayBuffer`: stop allocating a new `ByteArray` per encoded frame (100 s at 4 Mbps is about 50 MB of short-lived heap). Use a pooled ring of direct buffers or a capped byte arena, and cap total memory with a clear limit (for example 64 MB) that lowers the kept seconds and says so.
- `MainActivity.startFiveSecondRecording` is misnamed (records any length); rename and keep behavior.
- M5 performance pass: profile Glow 100 plus Pulse plus 3 effects on an emulator, note frame times in the PR, and remove any per-frame allocation you find in `OverlayRenderer` and the layers.

## Done Means

- Meets the Quality Bar (master plan section 0).
- `./gradlew assembleDebug testDebugUnitTest lintDebug` pass; CI green on the PR; the merged manifest permission list printed in the CI log matches the allowlist.
- No `TextToSpeech`, `SpeechRecognizer` or `android.speech` anywhere.
- PR description: checklist B0 to B5 with evidence, a short recorded sample description, "Requests for other lanes" (for example the mic-denied setter), and the status log entry.
