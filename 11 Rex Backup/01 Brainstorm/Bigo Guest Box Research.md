# Bigo Guest Box Research: getting OFFGRID effects into a live guest/PK feed

Researched Fri Oct 9, 2026 (CT). Scope: Bigo Live guest seat / multi-guest / PK, plus MeetMe, Tagged, TikTok. Constraints: no screen sharing, no root, keep ban risk low. Hardware: S25 Ultra (main), Galaxy A37 (second phone), and possibly a PC.

## Bottom line (the honest version)

- **On an unrooted Android phone there is no supported way for OFFGRID to act as the camera that Bigo's guest seat uses.** Android's virtual-camera API is a hidden System API. Only system apps that hold a Companion Device Manager role can use it. Samsung's video call effects only work in apps Samsung lists, and Bigo is not on that list. The S25's USB webcam *host* support (using an external camera) is unconfirmed and is not exposed to other apps.
- **A computer is the realistic route for "opponents/guests see my effects live."** TikTok officially lets you use a virtual camera (for example OBS Virtual Camera) in TikTok LIVE Studio, including during co-host and LIVE matches (battles). Bigo has PC tools (BIGO LIVE Streamer, Connector, OBS stream key). But I found **no official source saying that Bigo's PC tools can join someone else's multi-guest seat or run PK**, and OBS stream keys are reportedly limited to official broadcasters.
- **Inside Bigo itself**, the only sanctioned effects are Bigo's own Beauty, Makeup, Filter, AR sticker, background, and Virtual Live avatar features. I found **no public Bigo effect-creator program or SDK** for uploading custom AR. So OFFGRID's segmentation effects (cloak, ghost, invisible box) can't be uploaded to Bigo.

## Ranked options

| # | Option | Works for Bigo guest seat? | Quality | Ban risk | What OFFGRID must build |
|---|---|---|---|---|---|
| 1 | **PC + TikTok LIVE Studio + OBS Virtual Camera**, with OFFGRID frames fed in (co-host / LIVE match / multi-guest host) | No (TikTok only) | High (1080p) | **Low**: TikTok documents virtual camera support while co-hosting | An output path from the phone to the PC (see section B) or a desktop port of the compositor |
| 2 | **Effects run on the PC** (MediaPipe desktop / OBS filters / a port of OFFGRID's shader pipeline) → OBS Virtual Camera → LIVE Studio or BIGO LIVE Streamer | TikTok: yes. Bigo: only for your *own* broadcast | High | Low on TikTok. Bigo: unknown, since there's no official statement about virtual cameras | A desktop version of the compositor (MediaPipe has Python/C++ desktop support) |
| 3 | **Bigo PC (BIGO LIVE Streamer "add a camera" / OBS stream key)** with a virtual camera | **Unverified** for guest/PK. Official pages only describe solo broadcast | High | Unknown–moderate (no documented virtual-camera policy; OBS key reportedly limited to official broadcasters) | Same as #2. **Test first:** does Streamer show multi-guest / PK? |
| 4 | **Bigo built-in effects** (Beauty/Makeup/Filter, AR stickers, AI backgrounds, Virtual Live avatar) | Yes (sanctioned) | Medium. Can't do custom cloak/ghost | **None** | Nothing. Use it as a stopgap and design OFFGRID looks around it |
| 5 | **TikTok mobile + Effect House effect** (custom AR effect published to TikTok, usable in TikTok LIVE) | No (TikTok only) | Medium–high. Effect House has segmentation and Portal templates | None (official) | Rebuild the effect in Effect House instead of Kotlin. Effects go through review |
| 6 | **Samsung Video call effects** (background image/blur, auto framing) | **No**: only Samsung-listed apps (Meet, Messenger, Teams, WhatsApp, Zoom, etc.) | Low–medium | None | Nothing possible. Samsung controls the whitelist and offers no third-party effect API |
| 7 | **Second phone (A37) as a USB webcam into the S25** | **No / unverified**. The S25 would need to expose an external camera (FEATURE_CAMERA_EXTERNAL) to Bigo, and there's no evidence it does. Also, Samsung's native webcam mode is reported for S26 (One UI 8.5). I don't know if the A37 has it | — | Low if it worked | Nothing useful. Even if OFFGRID read a UVC camera directly (via a UVCCamera-style library), other apps can't see it |
| 8 | **Android VirtualDeviceManager virtual camera** | **No without system privileges** | — | — | Not possible: hidden @SystemApi, CREATE_VIRTUAL_DEVICE only for system role holders |
| 9 | **Snap Camera Kit** | No. It only renders inside *your own* app | High | n/a | Only useful if OFFGRID becomes its own streaming app |
| 10 | Root / LSPosed "VCAM" hooks, emulators, fake-camera APKs | Technically yes | Varies | **High** (Bigo bans by account *and* device; evading rules = "serious violation") | Rejected by constraints |

Not allowed by your constraints: screen sharing (Bigo Connector "Game Live" mirrors a PC screen into *your own* room, so it's a screen-share route anyway), and pre-recorded video (Bigo explicitly bans passing off pre-recorded video as live).

## A. Details per platform

**Bigo Live**
- Guests join by tapping a "+" seat, and the host approves. Then "your camera or microphone will activate." This is described for the mobile app only. Rooms have 4/6/9/12 seats, and the host's tools include beauty effects. https://www.bigo.tv/blog/bigo-live-multi-guest-room
- Built-in effects: Beauty (skin, face, body), Makeup, Filter tabs, with downloadable looks. https://www.bigo.tv/blog/bigo-live-filters-effects. Virtual Live avatar mode uses only Bigo's built-in avatars, and you **can't import outside 3D models**. https://www.bigo.tv/blog/bigo-live-virtual-live
- PC: BIGO LIVE Streamer (Windows 10+, "External cameras can be utilized", "Add a camera"), Connector (screen/game via phone QR), and OBS stream key. https://www.bigo.tv/blog/bigo-live-pc and https://www.bigo.tv/en/bigo-streamer/. None of these pages mention multi-guest or PK on PC.
- OBS key availability: an OBS-forum user says it's "only offered… to some members… mainly for official broadcasters" (forum report, not official). https://obsproject.com/forum/threads/bigo-live.156872/
- PK is documented as a mobile feature: random, friend, and event PKs. https://www.bigo.tv/blog/bigo-live-pk
- Enforcement: violations can lead to account restrictions, suspension, or **device bans**, and attempts to "evade or circumvent" rules are treated as serious. https://www.bigolive.tv/static-fed/commonrule/community-guidelines.html. Pre-recorded video passed off as live is prohibited. https://www.bigo.tv/blog/bigo-live-community-guidelines-quiz
- **Ban reports for virtual cameras specifically: I found none** (no reliable Reddit, forum, or news reports). That doesn't mean it's safe. It just means there's no evidence either way.

**TikTok**
- LIVE Studio co-host FAQ: "Can I use a virtual camera while co-hosting? Yes… select the virtual camera." Only the "Current scene" layout supports LIVE matches. https://www.tiktok.com/live/studio/help/article/Boost-viewer-engagement/Gaming-Co-host?lang=en
- Co-host, battles, and multi-guest all work in LIVE Studio, and a guide shows adding an OBS virtual camera as the camera source. https://www.toktutorials.com/post/how-to-use-co-host-battles-and-multi-guest-in-tiktok-live-studio-pc-mac
- Access: gaming creators need 1,000 followers and non-gaming creators 10,000. If you stream elsewhere, you can apply instead. https://www.tiktok.com/live/creators/en-US/article/tiktok-live-studio-access_en-US and https://www.tiktok.com/live/studio/help/article/Before-you-go-LIVE/Apply-for-LIVE-access
- A user reports that virtual cameras unlock after going live **3 times for 25 minutes each** in LIVE Studio (one Reddit report). https://www.reddit.com/r/streaming/comments/1nirgeu/need_help_with_tiktok_live_studio_and_virtual/
- Effect House: effects can be used in TikTok LIVE, and there's a Portal template (a world-tracked window into a 3D world). https://effecthouse.tiktok.com/ and https://effecthouse.tiktok.com/learn/guides/tutorials/template-tutorials/portal

**MeetMe / Tagged (The Meet Group)**
- The official help page only describes the in-app guest flow: tap +, choose "Guest", grant camera and mic, and the streamer accepts. https://support.meetme.com/hc/en-us/articles/24009843353490-How-do-I-become-a-Guest-in-someone-s-stream. I didn't find an official desktop/PC guest route. (meetme.video is a different site and may not be official, so I didn't rely on it.) Tagged wasn't researched separately. I assume it works the same way because it's the same company, but I didn't verify that.

**Android / Samsung platform facts**
- VirtualDeviceManager.createVirtualDevice / createVirtualCamera are `@SystemApi` and need CREATE_VIRTUAL_DEVICE, "only available to system apps holding specific roles". The public SDK only has read methods and `isVirtualCameraSupported()` (added in 37.2). https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/companion/virtual/VirtualDeviceManager.java and https://developer.android.com/reference/android/companion/virtual/VirtualDeviceManager. The role (COMPANION_DEVICE_APP_STREAMING) requires going through Google. https://source.android.com/docs/core/permissions/app-streaming
- External USB cameras through Camera2 need the OEM to enable the external camera provider. Stack Overflow users found Samsung phones without it, and apps like "USB Camera" use their own UVC stack that other apps can't see. https://source.android.com/docs/core/camera/external-usb-cameras and https://stackoverflow.com/questions/57846505/accessing-a-usb-camera-using-android-camera2-api
- Phone-as-webcam (UVC device mode) needs a system app and `ro.usb.uvc.enabled`, so third parties can't provide it. https://source.android.com/docs/core/camera/webcam and https://www.esper.io/blog/android-14-adds-support-for-using-your-smartphone-as-a-webcam. Samsung's wired webcam mode arrived with One UI 8.5 on the S26. https://www.sammobile.com/news/one-ui-8-5-use-galaxy-phone-computer-webcam-wired/. S25 owners were still asking for it in July 2026. https://us.community.samsung.com/t5/Galaxy-S25/Galaxy-S25-Series-Native-USB-Webcam-Support/td-p/3626562
- Samsung video call effects only work in listed apps (Duo/Meet, Zoom, Webex, Teams, Messenger, WhatsApp, KakaoTalk, WeChat, etc.). https://r2.community.samsung.com/t5/CamCyclopedia/Video-Call-Effects/ba-p/13246042 and https://www.samsung.com/us/support/answer/ANS10003558/
- Snap Camera Kit: it needs Snap's approval and API token, and the lenses render inside your own app. https://developers.snap.com/camera-kit/integrate-sdk/android/android-configuration and https://www.snap.com/terms/snap-camera-kit
- Caution: a Google Community answer claiming "USB Camera" lets other communication apps pick an external camera is not supported by the other sources. Don't rely on it. https://support.google.com/android/thread/227761301/external-usb-camera-in-communication-app-s?hl=en

## B. Can the S25 Ultra feed a PC *with OFFGRID effects*?

- Standard phone-as-webcam tools send the **raw** camera, not OFFGRID's output: Windows Phone Link "Use as a connected camera" (Windows 11, Android 10+, over Wi-Fi) https://support.microsoft.com/en-us/windows/apps/phonelink/use-your-mobile-device-s-camera, DroidCam-type apps, and Samsung's USB webcam mode (S26 / One UI 8.5, not S25).
- **So OFFGRID has to send its own composited output.** That's doable without root because it's OFFGRID's own app. Options to build:
  1. **Encode and stream from OFFGRID**: GL output surface → MediaCodec H.264 → SRT/RTMP/RTSP/WebRTC (or NDI) to the PC. On the PC: OBS Media Source / NDI source → OBS Virtual Camera → TikTok LIVE Studio (or BIGO LIVE Streamer). Expect about 100–300 ms of extra latency on Wi-Fi. USB tethering or an ADB reverse port over USB helps.
  2. **Or skip the phone**: port the compositor to the desktop (MediaPipe selfie segmentation runs on desktop) and use a normal webcam or the phone as a raw camera.
- Only the viewers of the platform that accepts the virtual camera (TikTok LIVE Studio, confirmed) will see the effects in co-host or battles.

## C. The "penguins / door to Antarctica" TikTok lead

- **I couldn't confidently identify the creator.** Candidates I found:
  - **Matty Jordan (@mattykjordan)**, an Australian at NZ's Scott Base and "Antarctica's biggest internet influencer". He often opens base doors onto real Antarctica and posts penguin content. That's **real footage, not an effect**. https://www.ladbible.com/community/weird/life-antarctica-video-matty-jordan-fear-tiktok-578556-20230824 and https://www.tiktok.com/@mattykjordan/video/7171656885186088193
  - **UK Antarctic Heritage Trust (Port Lockroy)**: a door opens onto snow with penguins outside (14.9M views). Real footage, and the penguins did *not* come inside. https://www.newsweek.com/penguins-antarctica-snow-port-lockroy-tiktok-1857058
  - **Zach King**, the famous edit-magic creator, has an older "penguin loose in the house" video. That's **edited VFX, not live**. https://steemit.com/zachking/@agib/fqoecfji
  - **AI "door/portal" transitions (2026 trend)**: film yourself opening a door, then generate the other side with a video-to-video model (e.g. Seedance 2.0 / 2.5 on Higgsfield). That's **post-produced, not live**. https://hamirev.com/ai-portal-transition-video-claude-seedance/ and https://ariacodez.ai/resources/wonderland-door-effect
- If it was **live during a PK**, the most likely setup is a PC (OBS/LIVE Studio) running a segmentation or keying effect, or a TikTok Effect House world/portal effect. Effect House's Portal template does exactly "a window into a 3D world you can walk into". https://effecthouse.tiktok.com/learn/guides/tutorials/template-tutorials/portal. I couldn't verify which one he uses. Royalty, the video link or creator handle would settle it.
- **Bigo equivalent:** Bigo has built-in AR stickers, AI backgrounds, and Virtual Live avatars, but **no public creator program for custom AR effects** and no import of outside 3D assets (see the Bigo links above). Bigo creator/business contact for asking about partnerships: hibd@bigo.sg (listed on https://www.bigo.tv/blog/bigo-live-support).

## Recommended next steps

1. Find out whether Royalty can get TikTok LIVE Studio access, then prototype: OFFGRID → SRT/NDI → OBS → Virtual Camera → LIVE Studio co-host/battle. This is the lowest-risk path that delivers live effects.
2. On a PC, install BIGO LIVE Streamer and check (a) whether multi-guest/PK appears and (b) whether OBS Virtual Camera can be picked under "Add a camera". Test on a throwaway account first, not the main one.
3. On the S25, quickly check with a small test app: `hasSystemFeature(FEATURE_CAMERA_EXTERNAL)` and plug in a UVC webcam to see if a LENS_FACING_EXTERNAL camera appears. If it doesn't, drop the A37-as-webcam idea.
4. In the meantime, use Bigo's built-in background and AR effects for guest seats.
