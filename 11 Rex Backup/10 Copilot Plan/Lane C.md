# Lane C: Live and Remote

**Branch from:** `final-build`. **Branch name:** `lane-c-live-remote`. **PR into:** `final-build`, titled `Lane C: Live and Remote (Do not merge, Rex reviews)`.
**Read first:** `docs/10 Copilot Plan/Copilot Master Plan.md` sections 1 to 6.
**Covers Royalty's phone-test items:** 12 (using OFFGRID while live on Bigo) and 13 (two-phone remote), plus Royalty's Bigo finding: today OFFGRID takes over the full screen, so it can't be used alongside Bigo.

## Files You Own

A new Gradle **library module** `04 Build/live/` (namespace `com.offgrid.live`) with its own `AndroidManifest.xml`, resources, code and tests; `04 Build/settings.gradle.kts` (add `include(":live")`); `04 Build/app/build.gradle.kts` (only the line `implementation(project(":live"))`); and a new doc `docs/10 Copilot Plan/Bigo Live Guide.md`.

The library's manifest merges into the app, so you declare your own permissions and services there without touching the app manifest (Lane B owns it). The `live` module must not depend on `app` classes; talk to the app through the small interfaces in C9. Read and write panel settings by key through `SharedPreferences` only via an interface the app passes in (C9), never by guessing internal file names.

## The Honest Android Picture (Stock Android 15 and 16, Samsung S25 Ultra, No Root)

1. **No virtual camera for third-party apps.** A normal app cannot register as a camera that Bigo (or any app) can pick. That needs root plus hooking modules, which trips Samsung Knox for good, breaks Samsung Wallet and Secure Folder, and risks Bigo bans. **Not allowed in this project.**
2. **Only one app can use the camera at a time.** When Bigo goes live with its own camera, Bigo owns the front camera. OFFGRID cannot get frames while Bigo has it, and Android blocks camera access from the background anyway.
3. **A floating overlay (draw over other apps) is visible only on Royalty's own screen.** In Bigo's normal camera live, viewers see Bigo's camera, **not** the overlay. So the overlay is a control surface, not a way to put effects into Bigo's camera stream.
4. **The working path to get OFFGRID's effects to Bigo viewers on one phone is Bigo's screen-sharing live mode** (Bigo's "Game" live or any mode that shares the screen with Android's MediaProjection), if Royalty's Bigo account and region offer it: OFFGRID runs full screen showing the finished picture (effects baked in, panel hidden or secure), and Bigo shares the screen. Mic: Bigo usually records the mic itself in that mode, so OFFGRID's voice effects will not reach Bigo viewers (Android does not let two apps share a processed mic). Voice effects work in OFFGRID's own recordings.
5. **Second-device path:** run OFFGRID on one phone and Bigo on another phone pointed at the first phone's screen, or use the two-phone remote (C6 to C8) so the camera phone stays clean and full screen while the remote phone holds every control.
6. **Bigo's rules:** using screen share to show a camera-like feed may be judged differently from games. Royalty must check Bigo's current community guidelines before relying on it, use a dummy account first (standing rule), and never use anything that fakes viewers, gifts or engagement. The app shows this as a plain note in 📁 Live and Remote and in the Bigo platform folder.

Write all of this, in plain words with tap-by-tap steps, in `docs/10 Copilot Plan/Bigo Live Guide.md`.

## Requirements and Acceptance Checks

### C1. Floating Overlay Mode (Draw Over Other Apps)

When Royalty is live in another app, OFFGRID must run as a **small floating overlay**, never full screen unless she chooses it.

- Permission `SYSTEM_ALERT_WINDOW`: ask through `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` with a plain explanation first; if not granted, the overlay row says how to turn it on.
- A window of type `TYPE_APPLICATION_OVERLAY` hosting a new **mini panel** (`live/OverlayPanelView.kt`, Canvas-drawn, same OLED look: black glass, white text, UV glow border, coded power toggles; no sliders):
  - **Move:** drag the top bar.
  - **Resize live:** drag the bottom edge or bottom corners (same feel as Lane A's A2); typed W and H boxes 0 to 100 in its own small settings.
  - **Shrink:** − collapses it to a **bubble** (56 dp glowing orb with the OFFGRID mark) that can be dragged and snaps to the nearest screen edge; tap the bubble to expand; drag it onto a bottom "×" target to close the overlay.
  - **Never covers Bigo full screen unless chosen:** default size 40 by 35 percent of the screen, top right; a "Full screen" option exists but is off and asks to confirm.
  - Quick controls inside: Camera and Mic state, Fire buttons for one-shot effects (Shatter, Freeze, Quiet, Fireworks, Red sweep, Laser writing), camera mode picker, Open OFFGRID, and Close overlay.
  - Touch outside the overlay passes to Bigo (`FLAG_NOT_TOUCH_MODAL`); the keyboard works for typed boxes (toggle `FLAG_NOT_FOCUSABLE` only while a box is being typed in).
  - Mark the overlay window **secure** (`FLAG_SECURE`) so that if any app captures the screen, the overlay is black instead of leaking controls; document that it then appears as a black rectangle in a screen share, so Royalty should collapse it to the bubble while screen sharing.
  - Remember position, size and bubble state.
- Respect gesture areas and rounded corners; never sit under the status bar.

Acceptance: overlay appears over another app (test with any app on an emulator), can be moved, resized, collapsed to a bubble, expanded and closed; taps outside reach the app below; state survives restart; screenshots in the PR.

### C2. Foreground Service

- `live/LiveService.kt`, a foreground service with a persistent notification ("OFFGRID overlay is on", actions: Open, Hide overlay, Stop).
- Types: `specialUse` for the overlay control (with the `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` explanation), plus `camera` and `microphone` **only** while OFFGRID itself is recording or streaming through the screen-share path, started while OFFGRID's activity is visible (Android 14+ while-in-use rules). Permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `FOREGROUND_SERVICE_CAMERA`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`.
- The service stops cleanly when the overlay closes and nothing is recording.

Acceptance: the service survives switching to another app; the notification actions work; no crash on Android 14, 15 and 16 emulators when started from the activity; the service never starts camera or mic from the background.

### C3. Screen Share Friendly Full Screen Output

- A "Clean output" mode in OFFGRID (action row in 📁 Live and Remote; Lane A adds the row, you implement the behavior in C9): hides the record bar and collapses the panel to the overlay bubble so the full-screen picture shows only what viewers should see. One tap on the bubble brings controls back.

Acceptance: in Clean output, a screenshot of the device shows only the finished picture plus the bubble (or nothing if the bubble is hidden).

### C4. Bigo Folder Notes

- Ask Lane A (in "Requests for other lanes") to add the plain Bigo notes from the guide to the Bigo platform folder; you write the exact text in the guide.

### C5. Two Phone Remote: Roles

- Two phones both run OFFGRID. One is **Camera** (runs the camera, effects and recording), the other is **Remote** (shows the live preview and controls effects). Role is the `fx.remote.role` setting (`off`, `camera`, `remote`).

### C6. Pairing With a 100+ Character Key

- Camera phone taps "Make pairing key": generate **128 characters** from `SecureRandom` over a 64-symbol URL-safe alphabet (768 bits). Show it once with a **Copy** button (clipboard marked sensitive with `ClipDescription.EXTRA_IS_SENSITIVE`; clear the clipboard after 60 seconds if it still holds the key).
- Remote phone taps "Paste pairing key"; validate length (at least 100) and alphabet; show a short 6-character fingerprint on both phones so Royalty can confirm they match.
- Store the key only Keystore-sealed (reuse the AES-GCM approach in `Handshake.kt`; copy the pattern into the `live` module). Never log it, never put it in a notification, never put it in the repo. "Forget pairing" deletes it; "New key" rotates it.

Acceptance: unit tests: generated keys are 128 characters, alphabet-only, and two keys never match in 10,000 tries; validation rejects 99 characters; logs contain no key material (test with a log capture).

### C7. Local Network Only, Encrypted

- Discovery and transport, local only, no cloud servers, no relays:
  1. **Same Wi-Fi:** `NsdManager` service `_offgrid._tcp`, TCP socket.
  2. **Wi-Fi Direct** fallback (`WifiP2pManager`) when there is no shared Wi-Fi; `NEARBY_WIFI_DEVICES` with `neverForLocation`.
- Session crypto: each side makes an ephemeral X25519 key pair (Android `KeyPairGenerator("XDH")`, API 33+), exchanges public keys plus 32-byte random nonces, then derives the session key with **HKDF-SHA256(ikm = X25519 shared secret, salt = nonceA || nonceB, info = "OFFGRID remote v1" plus the pairing key)**, so the pairing key authenticates the session and each session has forward secrecy. Confirm with an HMAC-SHA256 over the transcript keyed by a key derived from the pairing key; on mismatch drop the connection and say "Keys don't match".
- Every message is AES-256-GCM with a 96-bit nonce made from a per-direction counter; reject replays and out-of-order counters. Rekey every 2^20 messages or 30 minutes.
- Permissions: `INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `NEARBY_WIFI_DEVICES` (in the `live` manifest). Nothing ever connects outside the local network: bind only to site-local addresses and refuse public IPs.

Acceptance: unit tests for HKDF vectors (RFC 5869 test case 1), GCM round trip, replay rejection, wrong-key rejection; an instrumented loopback test (two endpoints in one process) passes control messages and frames.

### C8. Remote Preview and Control

- Camera phone sends the **finished picture** (effects baked in, never the panel) as H.264, 540p, 24 fps, about 1.5 Mbps, through the encrypted channel; target under 250 ms glass-to-glass on the same Wi-Fi. Audio preview off by default.
- Remote phone shows the preview full width with a compact control panel (reuse `OverlayPanelView`'s look): Camera and Mic toggles, camera mode, Fire buttons, and effect toggles. Control messages are `{key, type, value}` for `fx.` keys plus fire actions; the camera phone applies them through the same settings path as its own panel.
- If the link drops: the camera phone keeps running safely (no settings change), the remote says "Lost the camera phone. Trying again." and retries.

Acceptance: loopback test passes settings and fire actions; manual two-emulator or two-phone check list in the PR.

### C9. Integration Hooks (After A and B Merge Into the Final Build Branch)

The `live` module exposes:

```kotlin
interface OffgridHost {
    fun readSetting(key: String): String?
    fun writeSetting(key: String, value: String)
    fun fire(action: String)
    fun openApp()
    fun frameSurfaceSink(): FrameSink   // for the remote preview encoder
}
```

When Lanes A and B are in `final-build`, merge `final-build` into your branch and add the minimal hooks (under 40 lines, clearly marked `// Lane C hook`): implement `OffgridHost` in a new app-side file `LiveHost.kt`, start or stop the service and overlay from `MainActivity` on the 📁 Live and Remote rows and on `onPause` when the overlay toggle is on, and add one `CompositorProcessor` output surface for the remote encoder (mirror how `setReplay` works). Note each hook in the PR so Rex sees exactly what touched Lane B's files.

## Done Means

- Meets the Quality Bar (master plan section 0).
- `./gradlew assembleDebug testDebugUnitTest lintDebug` pass with the new module; CI green.
- Merged manifest has only the allowlisted permissions (see Lane B, B0).
- PR has the Bigo guide, screenshots of overlay and bubble, the crypto test results, a two-device check list, and a status log entry.
