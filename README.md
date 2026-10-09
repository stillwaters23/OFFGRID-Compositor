# OFFGRID Compositor

Royalty's private Android camera app. It bakes effects into the video Royalty records and streams, and keeps Royalty's face hidden when she wants it hidden. A see-through terminal panel floats on top to control everything. The panel is never in the video.

It runs only on Android phones: the Galaxy S25 Ultra and the Galaxy A37. Nothing needs a computer.

## Install

1. Uninstall the old OFFGRID app first.
2. Open the `step-2-preview` release on GitHub on your phone.
3. Download **OFFGRID Preview.apk** and open it to install.
4. Allow the camera when the app asks. The mic is asked for only when you turn the mic toggle on.

## Rules this app keeps

- **The camera and the mic are the only permissions the app itself needs.** The mic is used for your videos only. No voice, no speech.
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

**📁 Soundboard** gives each effect its own sound. Sounds play on your phone's speaker. With **Sounds in video** on, they are also mixed into the recorded audio.

### 📁 Camera and mic toggles, and 📁 Microphone Effects

The camera toggle lets go of the camera when it is off. Viewers then see black with your effects drawn over it, never a frozen face. The mic toggle starts and releases the mic. Every video is recorded with the app's own recorder (H.264 video, AAC audio). When the mic is off the video has a silent audio track. Microphone Effects change the voice on the phone only: pitch, robot, echo, radio, cave, volume, and a noise gate. The mic is not played through the speaker.

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
- **Audio-reactive panel (16):** the panel's own glow does not follow sound yet. The glow border in the video does follow the mic beat.
- **IP leak check (25):** it needs the internet permission, which is not allowed yet.
- **OFFGRID-to-OFFGRID calls (part of 14 and 34):** these need a server. The handshake codes are built.
- **Seeing a gift or a kick by itself:** another app's screen can't be read. Use the **Fire now** and **I got kicked** buttons.

## Where things are

- `04 Build/`: the Android app (Kotlin, CameraX, OpenGL ES 2, Canvas).
- `01 Brainstorm/Feature Scope.md`: every feature.
- `06 Prompts and Skills/Bootloader.md`: the build steps.
- `08 Chat History/Agent Status Log.md`: what was done, and when.
- `09 Grok Handoff/`: notes for another agent picking up the work.

## Building

GitHub builds the app on every push. Unit tests run on the build computer. The guards check that the build has no saved secrets, voice code, or sliders, and that the merged manifest asks only for permissions on the allowed list. The list is printed in the build log.
