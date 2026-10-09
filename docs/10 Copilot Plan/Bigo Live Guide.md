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
- Lane A and Lane B: Merge into `final-build` before app-side C9 wiring. The current branch has no Lane A actions or Lane B host/pipeline hooks.
- Lane B: When integrating, keep camera and microphone foreground-service types off for overlay-only use. Start them only while the OFFGRID activity is visible and OFFGRID itself is recording or streaming. The current app manifest removes `RECORD_AUDIO`; review that removal before wiring OFFGRID microphone recording.

## Test record

The library has local unit tests for key generation, HKDF, X25519 key agreement, AES-GCM, replay rejection, public-address rejection, and a loopback control message. Two-device tests, overlay screenshots, performance profiling, and Android 14, 15, and 16 device checks still need a working Android SDK and devices.
