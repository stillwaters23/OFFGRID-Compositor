# Bootloader

**Project:** OFFGRID-Compositor  
**Owner:** Royalty  
**Status:** Scope document. Nothing is built yet. Each step starts only when Royalty says go.  
**Updated:** 2026-10-06 (America/Chicago)

Read `06 Prompts and Skills/Bootstrap.md` first. This is the full pipeline from start to finish, one feature at a time. Each step is marked **Research first** or **Build**. Feature numbers match `01 Brainstorm/Feature Scope.md`.

Royalty assigns each step to one lane (Agent 01, 02, or 03). Every step ends with an HR Manager review: **PASS**, **FAIL**, or **BLOCKED**, with evidence. A step is done only on PASS plus Royalty's okay. See `Agent Lanes and HR Review.md`.

---

## Step 0. Project setup (Build)
- Create the Android project in `04 Build` (Kotlin, CameraX, OpenGL ES).
- Show a live front-camera preview on the phone.
- **Acceptance:** the app installs on the Galaxy S25 Ultra and Galaxy A37 and shows a smooth camera preview. No keys in the repo.

## Step 1. Outgoing video compositor (Research first, then Build)
- Research: the CameraX SurfaceProcessor and OpenGL layering approach, and the RTMP library for sending to a stream key.
- Build: the layer stack (camera, then effects, then the top layer) that every effect draws into.
- **Acceptance:** a test shape drawn in the compositor shows up in a recorded copy of the outgoing video.

## Step 2. Terminal panel and controls (Build)
- Features 3, 4, 5, and 13: the floating, draggable, about-50%-see-through terminal panel, Termux-style glow folders, identical toggles, and typed 0 to 100 inputs.
- Create the 11 platform folders and the OFFGRID-to-OFFGRID folder, empty for now.
- **Acceptance:** the panel drags anywhere, opacity is a typed number, there are no sliders, and all toggles match the design references.

## Step 3. Emoji rain (Build)
- Feature 2, baked into the outgoing video as the top layer.
- **Acceptance:** emoji rain is visible in the recorded outgoing video, and density and speed are typed numbers.

## Step 4. Drawing effects (Research first, then Build)
- Research: face tracking for face-pinned ink (MediaPipe Face Landmarker).
- Build: feature 1, with screen-fixed and face-pinned ink.
- **Acceptance:** face-pinned ink follows the face in the outgoing video.

## Step 5. Camera modes, cloak, and ghost mode (Research first, then Build)
- Research: the person-cutout and background-plate approach for the cloak. Ask Royalty for more ghost mode details.
- Build: features 9, 10, and 11 (blackout, cloak void, status label).
- **Acceptance:** each mode is selectable and visible in the outgoing video.

## Step 6. Glow border and fireworks (Build)
- Features 17 and 18, baked into the outgoing video.
- **Acceptance:** the border loops, and fireworks fire on a toggle flip.

## Step 7. Audio pipeline: voice effects, typed-to-speech, soundboard (Research first, then Build)
- Research: confirm the Superpowered license, SoundTouch as the backup, and xAI TTS streaming. Keep the API key on a server-side proxy.
- Build: the voice effects presets, feature 8 with the Rex voice, and feature 6.
- **Acceptance:** voice presets, Rex speech, and soundboard sounds are all heard in the outgoing audio.

## Step 8. Audio-reactive terminal UI (Build)
- Feature 16, with the pulse baked into the outgoing video.
- **Acceptance:** the terminal visuals pulse to sound in the recorded outgoing video, with typed 0 to 100 sensitivity.

## Step 9. Replay buffer (Build)
- Feature 7: a rolling 30-second buffer, saved on the phone only.
- **Acceptance:** a saved clip includes the effects. Check battery and storage use on the Galaxy A37.

## Step 10. Platform lanes, test toggles, and status prompts (Research first, then Build)
- Research: confirm stream key access for each platform using the dummy account, and confirm a maintained Android browser for web-only platforms (Kiwi Browser was reportedly discontinued in early 2025).
- Build: features 15 and 19, filling each platform folder with only what that platform allows.
- **Acceptance:** each folder shows a confirmed status line, and test results are logged.

## Step 11. Screen takeover and flip-flop (Build)
- Feature 12, inside Royalty's outgoing video and in OFFGRID-to-OFFGRID calls.
- **Acceptance:** the flip-flop switch swaps the big and small boxes live.

## Step 12. OFFGRID-to-OFFGRID calling, the premium lane (Research first, then Build)
- Research: WebRTC with a data channel (LiveKit or Stream) and standard encryption for the handshake.
- Build: feature 14 (handshake with rotating codes and blocking), plus the floating text overlay, where everything is visible to both sides.
- **Acceptance:** two OFFGRID phones connect only after the handshake, codes rotate after each call, and blocked contacts can't connect.
