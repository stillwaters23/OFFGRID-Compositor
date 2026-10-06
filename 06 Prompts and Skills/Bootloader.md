# Bootloader

**Project:** OFFGRID-Compositor  
**Owner:** Royalty  
**Status:** Scope document. Nothing is built yet. The loop starts only when Royalty says go, and Royalty can say stop at any time.  
**Updated:** 2026-10-06 (America/Chicago)

Read `06 Prompts and Skills/Bootstrap.md` first. This is the full pipeline from start to finish, one feature at a time. Each step is marked **Research first** or **Build**. Feature numbers match `01 Brainstorm/Feature Scope.md`.

Each step goes to one lane (Agent 01, 02, or 03). **Agent 04** is the HR Manager and oversees Agents 01, 02, and 03. (`Agent Lanes and HR Review.md` calls this role the HR Manager. It's the same agent.) Every step ends with an Agent 04 review: **PASS**, **FAIL**, or **BLOCKED**, with evidence. A step is done only on PASS. See `Agent Lanes and HR Review.md`.

## Android-only (hard rule)

OFFGRID-Compositor is Android-only. Royalty has no computer. Every step must build, run, and be tested entirely on Android phones. Any method that needs a PC (OBS, stream keys you can only get from a computer, a virtual camera on a computer, or desktop or computer-browser app versions) is marked **not applicable (needs a computer)** and isn't built. Agent 04 marks a step FAIL if it depends on a computer.

## Read everything first (read only)

Before starting any task, each agent (01, 02, and 03) must read **all** documents in the repo:
- `06 Prompts and Skills/Bootstrap.md`
- `06 Prompts and Skills/Bootloader.md`
- `06 Prompts and Skills/Agent Lanes and HR Review.md`
- `06 Prompts and Skills/Folder Guidance Panel.md`
- `06 Prompts and Skills/Terminal Plain Language Rule.md`
- `01 Brainstorm/Feature Scope.md`
- Every research doc in `02 Research`

This is **reading only**. Reading about other features never means working on them. Each agent works only on its assigned step and never starts, changes, or fixes any other task or feature.

## The build loop

1. **Start.** Royalty says go once to start the loop.
2. **Assign.** Agents 01, 02, and 03 each get the next open step in order, one feature per step. Two agents never get the same step.
3. **Build until it's right.** Each agent works its step until the code is right. On **FAIL**, Agent 04 sends it back to the same agent with a fix list, and the agent fixes it. An agent never moves on until its step passes.
4. **Review.** Agent 04 reviews the step against the brief and the step's acceptance checks, including the viewer-side success code.
5. **Pause.** When a step gets **PASS**, the system says **"pause."** During the pause, the agent stops and Agent 04 logs the result in `05 Docs/Build Log.md`.
6. **Next task.** After Agent 04 says PASS and the pause, the agents move on to the next open steps automatically. Royalty doesn't need to say go each time.
7. **Agent scaling.** Only as many agents work as there are open steps. If only two steps are left, only two agents work. If only one is left, only one agent works. Any agent without an assigned step stays idle and doesn't touch the code.
8. **Repeat.** The loop repeats until every step is done.
9. **Leave a note.** When any agent finishes or stops, it appends where it left off to `08 Chat History/Agent Status Log.md`.

**When the loop stops:**
- Royalty says **stop**.
- A step comes back **BLOCKED**. That step is logged in `07 Logs and Failures`, Royalty gets one clear line about it, and the loop waits for Royalty on that step. The other lanes keep going.
- The last step passes. The system says **"Build complete."**

**Required for every step: viewer-side success code.** After a lane builds a feature, Royalty tests it on a dummy phone (Phone 1) while watching from a second device (Phone 2). A one-time success number shows on Phone 2 only if the effect really reached the viewer. Royalty taps Check on Phone 1 and types that number. Agent 04 can't return PASS without this confirmation. See `Agent Lanes and HR Review.md`.

---

## Step 0. Project setup (Build)
- Create the Android project in `04 Build` (Kotlin, CameraX, OpenGL ES).
- Show a live front-camera preview on the phone.
- **Acceptance:** the app installs on the Galaxy S25 Ultra and Galaxy A37 and shows a smooth camera preview. No keys in the repo.

## Step 1. Outgoing video compositor (Research first, then Build)
- Research: the CameraX SurfaceProcessor and OpenGL layering approach, and the RTMP library for sending to a stream key straight from the phone.
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
- Research: confirm stream key access for each platform using the dummy account, on the phone only (any key that can only be gotten on a computer is marked not applicable, like the Instagram Live Producer key), and confirm a maintained Android browser for web-only platforms (Kiwi Browser was reportedly discontinued in early 2025).
- Build: features 15 and 19, filling each platform folder with only what that platform allows.
- **Acceptance:** each folder shows a confirmed status line, and test results are logged.

## Step 11. Screen takeover and flip-flop (Build)
- Feature 12, inside Royalty's outgoing video and in OFFGRID-to-OFFGRID calls.
- **Acceptance:** the flip-flop switch swaps the big and small boxes live.

## Step 12. OFFGRID-to-OFFGRID calling, the premium lane (Research first, then Build)
- Research: WebRTC with a data channel (LiveKit or Stream) and standard encryption for the handshake.
- Build: feature 14 (handshake with rotating codes and blocking), plus the floating text overlay, where everything is visible to both sides.
- **Acceptance:** two OFFGRID phones connect only after the handshake, codes rotate after each call, and blocked contacts can't connect.

## Step 13. Portals and wormholes (Build)
- Feature 20: a swirling portal or wormhole inside Royalty's box, including the disappear-into-the-void transition.
- **Acceptance:** the portal shows in the recorded outgoing video, and size, spin, position, and glow are typed numbers. It passes the viewer-side success code check.

## Step 14. Flying superheroes (Build)
- Feature 21: original OFFGRID flying heroes with a hero picker. No trademarked characters.
- **Acceptance:** the chosen hero flies in the outgoing video, and count, size, and speed are typed numbers. It passes the viewer-side success code check.

## Step 15. Roller coaster (Build)
- Feature 22: an animated coaster track and cars, with a watching view and a rider view.
- **Acceptance:** the coaster moves in the outgoing video, and speed, size, and number of cars are typed numbers. It passes the viewer-side success code check.

## Step 16. Flying through clouds (Build)
- Feature 23: an aerial fly-through-clouds scene, as a background or the full box.
- **Acceptance:** the clouds move in the outgoing video, and speed, thickness, and height are typed numbers. It passes the viewer-side success code check.

## Step 17. Beach theme (Build)
- Feature 24: pure ocean beach scenery that fully replaces the camera image, with no person in frame.
- **Acceptance:** only the beach shows in the outgoing video, with Royalty not visible at all. It passes the viewer-side success code check.

## Step 18. IP leak check (Research first, then Build)
- Feature 25. Research: IP-check services or a self-hosted check, and DNS, WebRTC, and IPv6 leak tests on Android. Build: the checks, the instant red flash and sound alert, and the IP check line in the guidance panel.
- **Acceptance:** a forced leak on a test setup sets off the red flash and sound right away, only on Royalty's phone, and the masked IP shows when Stealth is on. It passes the viewer-side success code check.

## Step 19. Time freeze (Build)
- Feature 26: hold the outgoing video on one frame for a set time, then go back to live. Decide the audio choice with Royalty first.
- **Acceptance:** the viewer sees a frozen frame and then live video, with no dropped stream. It never touches the other person's stream. It passes the viewer-side success code check.

## Step 20. Optical illusions (Build)
- Feature 27: impossible shapes, depth tricks, and shifting patterns, with an illusion picker.
- **Acceptance:** illusions show in the outgoing video, and nothing flashes more than 3 times per second. It passes the viewer-side success code check.

## Step 21. Kick detection and rejoin (Research first, then Build)
- Feature 28. Research: confirm the detection options in `02 Research/Battle Box Kick Research.md`. Build: the "I got kicked" button, the optional video-drop hint, the plain-language message, and the one-tap shortcut to open the platform. No auto-rejoin.
- **Acceptance:** a kick flag and message show on the terminal, the shortcut opens the platform, and nothing taps or rejoins automatically. It passes the viewer-side success code check.

## Step 22. Red sweep kick animation (Build)
- Feature 29: an extra-long, original-design red sweep, with its own toggle in all 11 platform folders. MeetMe and Tagged use the browser route.
- **Acceptance:** the sweep shows in the outgoing video on each tested platform, and length and duration are typed numbers. It passes the viewer-side success code check.

## Step 23. Depth shift (Build)
- Feature 30: a faked push-out-of-the-screen effect using an inner frame, zoom, shadow, and the cut-out of Royalty, then a snap back.
- **Acceptance:** Royalty seems to pop past the frame in the outgoing video, and the timing values are typed numbers. It passes the viewer-side success code check.

## Step 24. Laser calligraphy writing (Build)
- Feature 31: typed text written stroke by stroke in calligraphy by a laser tip, with a glowing trail.
- **Acceptance:** typed words are written live in the outgoing video, and speed, size, glow, and color can be set. It passes the viewer-side success code check.

## Step 25. Radar sweep (Build)
- Feature 32: a rotating radar beam with pings in the stream audio, and face and no-face modes.
- **Acceptance:** both modes work in the outgoing video, the ping is heard, and face data stays on the phone. It passes the viewer-side success code check.

## Step 26. Echo trail (Build)
- Feature 33: fading copies of Royalty's movement for about one second.
- **Acceptance:** the trail shows in the outgoing video, and length, copies, and fade are typed numbers. It passes the viewer-side success code check.

## Step 27. Mirror plus scan (Build, after Step 12)
- Feature 34: in OFFGRID-to-OFFGRID calls only, mirror the other person's face and run the radar face mode on it, with a "scan on" note for them.
- **Acceptance:** both phones see the mirrored, scanned face, the other person sees the note, and no face data leaves the phones. It passes the viewer-side success code check.

## Step 28. Quiet mode (Build)
- Feature 35: the still-and-dark pause, then a small boot light flickers on, with optional chaining into the next effect.
- **Acceptance:** the pause and the light show in the outgoing video, and the chained effect fires on cue. It passes the viewer-side success code check.

## Step 29. Shatter (Build)
- Feature 36: glass cracks, shards fall, then the pieces reassemble, with a selectable background behind them.
- **Acceptance:** the shatter and reassemble show in the outgoing video, and piece count and speeds are typed numbers. It passes the viewer-side success code check.
