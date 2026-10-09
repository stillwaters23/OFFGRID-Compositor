# OFFGRID Compositor

Royalty's private Android camera app. It bakes effects into the video Royalty records and streams, and keeps Royalty's face hidden when she wants it hidden. A see-through terminal panel floats on top to control everything. The panel is never in the video.

It runs only on Android phones: the Galaxy S25 Ultra and the Galaxy A37. Nothing needs a computer.

## Install

1. Uninstall the old OFFGRID app first.
2. Open the `step-2-preview` release on GitHub on your phone.
3. Download **OFFGRID Preview.apk** and open it to install.
4. Allow the camera when the app asks. It never asks for the mic.

## Rules this app keeps

- **The camera is the only permission.** No mic, no voice, no speech, no network.
- **No sliders.** Every number is typed from 0 to 100. Every switch is a toggle.
- **The panel is never in the video.** Only the effects are.
- **Your face can stay hidden.** The face diamond starts on. The no-face camera modes never show your face, and no reset ever changes the camera mode.
- **Nothing flashes more than 3 times a second.**
- **Everything stays on your phone.** Videos, face data, and test results never upload.
- **Original art only.** No outside heroes, logos, or names.

## How to use it

The panel opens full screen at the start. Tap **−** to make it small, and tap the small tag to open it again. Drag it anywhere, or pinch it to resize.

Tap a folder to open it. Each folder shows what it does, in plain words. To change a number, type it and tap **Done**. A lit toggle means on and a dim toggle means off. Every folder has a **Reset this folder** button at the bottom.

### 📁 Camera: what viewers see

- **Camera mode:** Live camera, Blackout, Cloak void, Status label (like CLOAKED), or Ghost (the last picture, held still, or a blank box). GHOST ON shows on your screen while ghost is on.
- **📁 Face diamond:** a solid diamond covers your face and follows it. It starts on.
- **📁 Takeover:** you fill the box, zoomed in, with a small box in the corner. **Flip-flop** swaps them.
- **📁 Mirror plus scan:** flips your video and runs a radar scan over your face.

### 📁 Effects: one folder for each effect

Emoji rain, Drawing (draw on the camera picture with a finger), Glow border, Fireworks (they burst when you flip a toggle), Portal, Flying heroes, Roller coaster, Clouds, Beach, Time freeze, Optical illusions, Red sweep, Depth shift, Laser writing, Radar sweep, Echo trail, Quiet mode, and Shatter.

Effects with a **Fire now** button run once when you tap it, like Shatter now or Freeze now. **Quiet mode** can fire another effect the moment its light comes back on.

**📁 Soundboard** gives each effect its own sound. Sounds play on your phone's speaker only. They are not in the video, because the app has no mic.

### 📁 Record

- **Record length:** seconds per video. 0 records until you tap **Stop**.
- **Save to gallery:** each video also goes to your gallery, in Movies › OFFGRID.
- **Replay buffer:** keeps the last moments of your video. Tap **Save replay** to keep a clip.

Tap **Record** on the bar at the bottom. Tap **Play last video** to watch it.

### Panel settings

Size, Display, Glow effects, Colors (hex codes or nine typed colors), Themes, Toggle style (Pure White or Pure Black), **📁 Presets** (save, load, rename, and delete your looks), and Reset.

### The 18 platform folders

Each one has:

- the platform's rules, in plain words;
- **Effects:** Red sweep on kick, **I got kicked** (says what to do, and never rejoins for you), **Open** the app, and **Use safe start**;
- **Tests:** your private test toggles, **📁 Success code**, and **📁 Results**.

**Success code** puts a 6 digit code in your video for 30 seconds. Ask a viewer on another phone what they see, then type it in. A match marks the test as a success, with the date.

### OFFGRID to OFFGRID

**📁 Handshake** makes your 300 character code, makes a new one after a call, and trusts, blocks, or checks a code you copied. Codes are locked in the phone's secure storage.

## Not built, and why

- **Typed-to-speech, Rex's voice, speaking as Rex, and the read-aloud button (8, 38, 39):** voice is paused (Step 7). See `05 Docs/Step 7 Voice Paused.md`.
- **Audio-reactive panel (16), and a border synced to heartbeat or sound (part of 17):** these need the mic, which stays off.
- **IP leak check (25):** it needs the internet permission, and the camera is the only permission.
- **OFFGRID-to-OFFGRID calls (part of 14 and 34):** these need a server. The handshake codes are built.
- **Seeing a gift or a kick by itself:** another app's screen can't be read. Use the **Fire now** and **I got kicked** buttons.

## Where things are

- `04 Build/`: the Android app (Kotlin, CameraX, OpenGL ES 2, Canvas).
- `01 Brainstorm/Feature Scope.md`: every feature.
- `06 Prompts and Skills/Bootloader.md`: the build steps.
- `08 Chat History/Agent Status Log.md`: what was done, and when.
- `09 Grok Handoff/`: notes for another agent picking up the work.

## Building

GitHub builds the app on every push. Unit tests run on the build computer. The guards check that the build has no saved secrets, mic code, voice code, or sliders, and that the camera is the only permission.
