# Bigo Live Guide

## What viewers can see

OFFGRID cannot become a camera that Bigo can select on a normal, unrooted Android phone. Do not root the phone or install a camera-hooking module. That can trip Knox, break Samsung Wallet and Secure Folder, and put the Bigo account at risk.

Android lets one app use the camera at a time. When Bigo is live with its own camera, Bigo owns the camera. OFFGRID cannot read those camera frames. The OFFGRID floating panel is only a private control surface on this phone. Viewers do not see the panel or its effects in Bigo's normal camera live.

The overlay starts small at the top right. It does not cover Bigo's full screen. The Full screen option stays off unless you choose it and confirm. Screen captures show the OFFGRID panel as black. Collapse it to the bubble before sharing the screen.

## One-phone screen-share test

This works only if Bigo offers screen sharing on your account and in your region. Bigo may call this Game live or screen live.

1. Open OFFGRID and allow the camera when Android asks.
2. Open the Live and Remote folder. Turn on the overlay option.
3. If Android asks, read the explanation and tap Open settings.
4. Turn on Allow display over other apps for OFFGRID. Return to OFFGRID.
5. Open the small OFFGRID panel. Set the camera mode and the controls you want.
6. In Bigo, choose its screen-sharing live mode. Do not choose normal camera live for this test.
7. Start with a dummy account and a private test. Confirm that Bigo is sharing the finished OFFGRID picture.
8. Collapse the OFFGRID panel to its bubble before sharing. The panel is private but a screen share can otherwise show it.
9. Check the recording from a second device. Stop if the panel or any private control appears.

Bigo usually records its own microphone in screen-share mode. OFFGRID voice effects will not reach Bigo viewers in that mode. Voice effects work in OFFGRID recordings.

## Two-phone test

Use one phone for OFFGRID's camera and effects. Use a second phone for the OFFGRID remote or Bigo. Both phones must run OFFGRID for the remote link.

1. Connect both phones to the same trusted Wi-Fi network.
2. On the camera phone, choose Camera role and make a pairing key.
3. Show the key once and tap Copy. Keep it private.
4. On the remote phone, choose Remote role and paste the key.
5. Check that the six-character fingerprint matches on both phones.
6. Confirm the link. The remote preview is the finished picture, not the OFFGRID panel.
7. Test camera and microphone controls, camera mode, effect toggles, and each Fire action.
8. If the link drops, the camera phone keeps running. Reconnect only when the remote phone reports it is trying again.
9. To remove access, choose Forget pairing. Choose New key to rotate the key.

The remote preview is video only by default. OFFGRID does not read Bigo chat or any other app's screen. A person watching can tap Fire on the remote, but OFFGRID does not read messages or automate engagement.

## Bigo rules and safety

- Check Bigo's current community guidelines before each test. Rules can change.
- Use a dummy account first. Do not test with a public live until the private test is clear.
- Bigo may judge a camera-like screen share differently from a game. Stop if Bigo says the stream is not allowed.
- Do not fake viewers, gifts, likes, chat, or other engagement.
- Never show your face by surprise. Check the finished picture before sharing it.
- Do not use root, camera hooks, anti-cheat bypasses, or tools that read another app's screen or chat.
- Keep the pairing key private. Forget it when you no longer need remote access.

## Notes for the Bigo folder

Add these plain notes to the Bigo platform folder:

> Bigo normal camera live: OFFGRID's overlay is visible only on this phone. Bigo viewers see Bigo's camera, not OFFGRID's effects.
>
> Bigo screen share: If screen-sharing live is available for this account and region, share OFFGRID's finished picture with the panel collapsed to its bubble. Test with a dummy account first. Bigo usually uses its own microphone, so OFFGRID voice effects do not reach viewers in this mode.
>
> Check Bigo's current community guidelines. Stop if camera-like screen sharing is not allowed. Never fake viewers, gifts, likes, chat, or engagement.

## Requests for other lanes

- Lane A: Add the three notes above to the Bigo platform folder in 📁 Live and Remote.
- Lane B: Add `setExtraOutput(id, surface, width, height)` using its shared `drawFinal` path so remote preview and PC encoder surfaces receive the same finished image without a duplicate compositor renderer. Its current PR branch does not yet expose this method, and it is not in `final-build`.
- Lane B: Expose a read-only processed PCM tap for PC streaming when `fx.mic.on` is on. Until then the PC output remains video-only.
- Lane B: Keep camera and microphone foreground-service types off for overlay-only use. Add them only for stream work started while OFFGRID is visible. `RECORD_AUDIO` is declared by Lane B in the app manifest.

## Send to PC with OBS

This route sends the finished OFFGRID picture over the local network. It does not send the panel or overlay. The Android implementation must use only a local PC address; never use a public streaming relay.

### SRT listener

1. On the PC, install OBS Studio and open it.
2. In OBS, add a **Media Source** and name it `OFFGRID`.
3. Clear **Local File**. Enter `srt://0.0.0.0:9000?mode=listener` as the input, then select **OK**.
4. Allow OBS through the PC firewall on the private network only.
5. On the phone, open 📁 Live and Remote. Set **PC protocol** to SRT, type the PC's local IP address, and keep port 9000 unless the PC listener uses another port.
6. Turn on **Send to PC** and choose **Start PC output**. Check OBS for the finished picture.
7. In OBS, choose **Start Streaming** and connect OBS to Bigo or TikTok Live Studio using the platform's current stream key and server details. Keep the stream key on the PC and never put it in OFFGRID.

### RTMP server

1. On the PC, install and start a local RTMP server, such as MediaMTX. Configure its private-network listener on port 1935.
2. In OBS, add a **Media Source**, clear **Local File**, and enter the local server's stream path, such as `rtmp://127.0.0.1:1935/live/offgrid`.
3. Allow the RTMP server through the PC firewall on the private network only.
4. On the phone, choose RTMP, type the PC's local IP address, and set the port to 1935.
5. Tap **Send to PC**, then **Start PC output**. Confirm the source appears in OBS.
6. Start the Bigo or TikTok stream from OBS or the platform's current PC Live Studio. Follow the platform's current rules and account requirements.

Test SRT and RTMP with a private or dummy stream first. Never share an OBS stream key, SRT passphrase, pairing key, or public IP in screenshots or logs.

## Test record

The library has local unit tests for key generation, HKDF, X25519 key agreement, AES-GCM, replay rejection, public-address rejection, and a loopback control message. Two-device tests, overlay screenshots, performance profiling, and Android 14, 15, and 16 device checks still need a working Android SDK and devices.
