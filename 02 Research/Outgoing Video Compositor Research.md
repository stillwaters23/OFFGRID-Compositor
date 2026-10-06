# Outgoing Video Compositor Research

**Project:** OFFGRID-Compositor  
**Owner:** Royalty  
**Step:** Bootloader Step 1, Research part only  
**Lane:** Agent 02  
**Date:** 2026-10-06 (America/Chicago)  
**Status:** Research only. Nothing is built. The Build part waits for Royalty's review.  
**Phones:** Galaxy S25 Ultra and Galaxy A37. Android only. Royalty has no computer.

This paper is about one job: take the front camera, draw effects on top of it, and make one finished picture that can leave the phone. Voice, typed-to-speech, and the Rex voice are not in this paper. Those wait for Royalty.

---

## How to read this paper

Each finding has a label:

- **Confirmed:** an official page says it. That means a company help page, Android's own docs, or the library's own page.
- **Reported:** a spec site, a news page, or another guide says it. Useful, but not the maker's own words.
- **Guess:** my own reasoning where no page settled it. The Build plan must not treat a guess as a finished fact.

## Short word list

- **Frame:** one still picture. A video is many frames in a row.
- **Frames per second (fps):** how many pictures go out each second. 30 fps means 30 pictures each second.
- **CameraX:** Google's camera helper for Android apps. Step 0 uses it.
- **Surface:** a patch of memory something can paint on. The camera paints on one. The drawing chip paints on another.
- **OpenGL ES:** Open Graphics Library for Embedded Systems. It is the drawing tool a phone uses to paint pictures with its graphics chip.
- **Graphics chip (GPU):** graphics processing unit. The part of the phone that draws pictures fast.
- **Layer:** one picture stacked on another, like clear sheets on a drawing.
- **Baked in:** the effect is painted into the video itself before the video leaves the phone. Viewers see it because it is part of the picture.
- **RTMP:** Real-Time Messaging Protocol. A common way to push a live video to a site such as YouTube.
- **RTMPS:** the same push, with a lock on the path so other people cannot read it on the way. The S means secure.
- **Stream key:** the secret code that tells a site which live video is yours. It never appears in this paper, the repo, or the chat. It lives only in the phone's secure storage later.
- **Encoder:** the part that shrinks the picture so it can be saved or sent. On Android this is MediaCodec, and it can use a special video chip.
- **H.264:** a common way to shrink video. Sites also call it AVC (Advanced Video Coding).
- **MP4:** a normal video file.
- **Screen share:** you show your phone's screen inside a call. The other people see that screen.
- **SDK:** software development kit. A box of ready-made code from a company.

---

## Bottom line

- **One drawing pass makes the outgoing picture.** CameraX hands each camera frame to one effect. That effect uses OpenGL ES. It paints the camera first, then the effects, then the top layer. The finished frame is what the preview, the saved file, and a later live send all use.
- **There is only one effect slot for the preview.** CameraX will not stack two effects on the same preview. Every future effect has to join the list inside this one drawing pass.
- **Step 1's proof is a file on the phone.** A test shape is painted in that drawing pass. CameraX's recorder saves an MP4. The shape has to be inside the file. A shape drawn only as a normal button on top of the preview does not count.
- **Viewers on other apps get that picture in two phone-only ways.** (1) OFFGRID pushes the finished video with an RTMP library to a stream key the phone can see. (2) OFFGRID fills the screen, and the calling app shares that screen. A fake webcam is not available on a normal Android phone.
- **The recommended sender library, for a later step, is StreamPack 3.2.1.** It is free to use in a private app (Apache License 2.0), it runs on Android, it speaks RTMPS, and it can record a file and send a stream from the same picture. RootEncoder 2.8.1 is the backup. Step 1's build does not add either library yet.
- **Start at a tall 720 by 1280 picture, 30 fps, on both phones.** The S25 Ultra can try a taller 1080 by 1920 picture after the A37 stays smooth. Do not send the full screen size as the video size.
- **This research stops here.** No code in `04 Build`. No voice work.

---

## 1. How every effect becomes one outgoing frame

### What CameraX gives us

**Confirmed.** CameraX can insert one effect into the camera pipeline. The class is `CameraEffect`. The effect holds a `SurfaceProcessor`. The processor takes the original camera frames, changes them, and gives the new frames back to CameraX. Google's page says to use OpenGL or Vulkan to touch those surfaces. Vulkan is a newer drawing tool. This plan uses OpenGL ES, because Step 0 already names it and the official sample path uses it.

The processor has two doors:

1. **Input.** CameraX asks for a surface to write the raw camera frames on. The usual OpenGL way is a `SurfaceTexture` at the size CameraX asks for. When a new frame arrives, the app paints it.
2. **Output.** CameraX hands back one or more surfaces to paint the finished frames on. Those surfaces are the preview and, when video recording is turned on, the recorder.

Sources: [SurfaceProcessor](https://developer.android.com/reference/androidx/camera/core/SurfaceProcessor), [CameraEffect](https://developer.android.com/reference/androidx/camera/core/CameraEffect). Both pages were current when read on 2026-10-06.

**Confirmed.** A `SurfaceProcessor` may target these sets:

- preview only
- preview and video capture together
- preview, video capture, and still photos together

The `CameraEffect` page says other mixes throw an error. The safe mix for Step 1 is **preview and video capture together**. Then the screen and the saved file get the same painted frame.

**Confirmed.** You attach the effect with `UseCaseGroup.Builder.addEffect`. Two effects may not cover the same target. One effect on preview, plus another effect that also covers preview, is an error. So the layer list lives **inside** the one processor. Later features (emoji rain, glow, portals, and the rest) add a layer to that list. They do not add a second `CameraEffect`.

Source: [UseCaseGroup.Builder](https://developer.android.com/reference/androidx/camera/core/UseCaseGroup.Builder).

**Confirmed.** CameraX 1.6.2 is the stable release of `camera-core`, `camera-video`, `camera-view`, and `camera-effects` on the CameraX release page read on 2026-10-06. The same page also lists a 1.7.0 alpha. The build should stay on the stable 1.6.2 line unless Royalty asks otherwise. `camera-effects` includes `OverlayEffect`, a ready-made OpenGL overlay that can draw with a normal Android canvas.

Source: [CameraX releases](https://developer.android.com/jetpack/androidx/releases/camera), [OverlayEffect](https://developer.android.com/reference/androidx/camera/effects/OverlayEffect).

**Guess, with a strong reason.** `OverlayEffect` is enough for a single test shape or a text stamp. It is a weak home for the later effects (portals, shatter, echo trail), because those need their own drawing programs (shaders). Step 1 should build the custom processor, and use the test shape as the first top layer. That way the acceptance file proves the real stack, not a side path we would throw away.

Google's own test app shows a custom processor that registers each output surface and draws the camera texture to all of them: [ToneMappingSurfaceProcessor](https://github.com/androidx/androidx/blob/androidx-main/camera/integration-tests/viewtestapp/src/main/java/androidx/camera/integration/view/ToneMappingSurfaceProcessor.kt). A Google reply on a public question says the same thing: target preview and video capture, then draw the extra art in OpenGL, so the saved video contains it. Source: [CameraX watermark question](https://stackoverflow.com/questions/75936230/unable-to-record-video-with-watermark-over-it-using-android-java-camerax-api).

### How the camera picture becomes a texture

**Confirmed.** A `SurfaceTexture` catches frames and shows them to OpenGL ES as a special texture called `GL_TEXTURE_EXTERNAL_OES`. The drawing program must sample that texture with `samplerExternalOES`. Calling `updateTexImage()` pulls the newest camera frame into that texture. The camera also provides a transform matrix so the picture is turned the right way.

Sources: [SurfaceTexture](https://developer.android.com/reference/android/graphics/SurfaceTexture), [SurfaceTexture architecture](https://source.android.com/docs/core/graphics/arch-st).

**Confirmed.** The processor must also apply the extra transform CameraX computes (`updateTransformMatrix` with the matrix from the input surface). That matrix accounts for rotation and for the crop CameraX chose. If the build skips it, the saved file can come out sideways or zoomed wrong. That is a real risk on a front camera.

Source: [SurfaceProcessor.onOutputSurface](https://developer.android.com/reference/androidx/camera/core/SurfaceProcessor#onOutputSurface(androidx.camera.core.SurfaceOutput)).

### The layer order

Paint back to front. Later sheets cover earlier sheets. See-through parts use blending so the camera still shows through.

| Order | Layer | What it is in OFFGRID | Step 1 |
|---|---|---|---|
| 1, bottom | Camera | The live front camera | Always on |
| 2, middle | Effects | Each effect, in the order Royalty turned them on | Empty list is fine |
| 3, top | Top layer | The thing that must sit over everything, such as emoji rain later, or the private test number | The test shape lives here |

**Confirmed as a drawing rule.** OpenGL draws triangles in the order you send them. To put a see-through picture over the camera, turn blending on and use source-alpha over one-minus-source-alpha. That is the normal "clear sheet" mix. The camera texture is drawn as a rectangle that fills the frame. Each effect is another rectangle, or its own drawing program, in list order. The top layer is last.

**Guess.** Keep the finished picture in one off-screen texture (a framebuffer), then copy that one texture to every output. Copying one finished picture keeps the preview, the file, and a later stream identical. Drawing the whole stack once per output would waste battery and could drift.

**Confirmed risk.** A normal Android view that sits on top of the preview (a button, a label, the future terminal) is **not** inside the video surface. Screen share of the whole app can still show that view, because screen share copies the screen. The saved file and an RTMP send only contain what the processor painted. The core design rule says viewers see the effect. So every effect, including the Step 1 test shape, is painted in the processor. The preview should show that same painted surface, so Royalty's screen matches the file.

### What one frame's trip looks like

1. The front camera writes a frame.
2. OpenGL picks it up as the bottom layer.
3. The effect list paints, in order.
4. The top layer paints last.
5. The finished frame is copied to the preview surface.
6. The same frame is copied to the recorder surface.
7. Later, the same frame can be copied to a live-send surface. Step 1 does not do step 7.

---

## 2. How that picture reaches viewers

Royalty has no computer. Anything that needs a PC is **not applicable (needs a computer)**. That includes OBS (Open Broadcaster Software), a fake webcam on a computer, and a stream key that can only be copied on a computer.

**Confirmed.** Stock Android does not let a normal app pretend to be the camera inside another app. Android's virtual-camera permission is only for special system apps. OFFGRID cannot register itself as WhatsApp's camera, Meet's camera, or Bigo's camera.

Source: already written up in `02 Research/Messenger Instagram WhatsApp Research.md`, from [Android companion app streaming](https://source.android.com/docs/core/permissions/app-streaming).

Three phone paths remain.

### (a) Push the video with an RTMP library

OFFGRID shrinks the finished frames and pushes them to a site's ingest address, plus the stream key. The site then shows that video to viewers. The key is typed on the phone into OFFGRID and stored in Android's secure storage (the Android Keystore). It is never written into the app file, a log, a screenshot, or this repo.

#### Libraries compared

Read on 2026-10-06. Stars and last-push times come from each GitHub repo's public info that day.

| Library | Android? | What it speaks | License | Maintained? | Fits OFFGRID? |
|---|---|---|---|---|---|
| **StreamPack** 3.2.1 | Yes. Kotlin. On Maven Central. | RTMP, RTMPS, and SRT (Secure Reliable Transport, a newer low-delay push). H.264, H.265, VP9, AV1. | Apache License 2.0. A private app may use it. Keep the license notice. | **Confirmed.** Release 3.2.1 on 2026-10-03. Repo pushed 2026-10-05. About 367 stars. | **Best fit for a later step.** It can take a custom video source, it can process frames before the encoder, and it can send and record at the same time. |
| **RootEncoder** 2.8.1 | Yes. Java and Kotlin. From JitPack. | RTMP, RTMPS, RTSP, SRT, and UDP. H.264 and others. OpenGL filters. Can record an MP4 while streaming. | Apache License 2.0. **Confirmed** on the GitHub license field. | **Confirmed.** README on the master branch lists 2.8.1. Repo pushed 2026-09-28. About 3,048 stars. | **Backup.** Larger and older. It wants to own the camera (its own Camera1, Camera2, or CameraX source). Two owners of the front camera will fight. |
| **HaishinKit.kt** 0.18.2 | Yes. Kotlin. Minimum Android 5. | The public module page lists RTMP publish, plus drawing text or a picture on the video. RTMPS was **not** confirmed on that page. | BSD 3-Clause. A private app may use it. Keep the notice. | **Confirmed** as active. Tagged release 0.18.2 on 2026-02-15. Repo pushed 2026-10-02. About 212 stars. | Third choice. Smaller, and the locked RTMPS path is not confirmed here. |
| **NodeMedia client** | Yes. Android. | RTMP and other protocols, hardware encode. | **Commercial.** The repo says a paid license is required. | The 4.1.5 line is published. | Skip. A paid SDK is extra cost and a key to protect. |
| **Larix Broadcaster** | It is a finished Android **app**, not code we drop into OFFGRID. | YouTube's own help lists it as a phone app that can push RTMP. | The app's own terms. An embeddable SDK license was not confirmed. | YouTube still lists it. | Useful as proof that a phone can push RTMP. Do not copy the app into OFFGRID. |

Sources:

- StreamPack repo and license: https://github.com/ThibaultBee/StreamPack
- StreamPack releases (3.2.1 on 2026-10-03, Twitch fix in 3.1.2, Facebook Live fix in 3.1.1): https://github.com/ThibaultBee/StreamPack/releases
- StreamPack custom sources and file-plus-stream: https://github.com/ThibaultBee/StreamPack/blob/main/docs/AdvancedStreamer.md
- RootEncoder repo, version 2.8.1, RTMPS, record-while-streaming, OpenGL filters: https://github.com/pedroSG94/RootEncoder
- HaishinKit.kt: https://github.com/HaishinKit/HaishinKit.kt and https://docs.haishinkit.com/kt/latest/
- NodeMedia commercial note: https://github.com/nodemedia/nodemediaclient-android
- YouTube lists Larix on Android: https://support.google.com/youtube/answer/2907883

**Confirmed.** Apache License 2.0 and BSD 3-Clause both allow a private sideloaded app. The app must keep the license text with the notices. A commercial SDK adds a contract and usually a license key. That key would be a credential and would have to stay in the phone's secure storage. We do not need that for Step 1.

**Confirmed.** StreamPack 3.0.0 added a surface-processor hook so frames can be changed before they reach the encoders. Its advanced guide says you can write your own video source. A source that can paint onto a surface is the preferred kind. It also has a dual output so one picture can go to a file and to RTMP together.

**Guess.** StreamPack can sit **beside** CameraX, not instead of it. CameraX stays the only thing that opens the front camera (that matches Step 0). The processor copies the finished texture onto a surface StreamPack encodes. This is the clean split. It is a guess until the Build tries the custom source and proves StreamPack does not open the camera itself. If that guess fails, RootEncoder's "surface to buffer" encoder is the next thing to try, still without a second camera client.

**Confirmed, and important.** Do not let the RTMP library open the camera if CameraX already has it. A phone gives the camera to one client at a time. A second open fails or steals the preview.

#### What the sites ask for

**Confirmed, YouTube.** YouTube accepts an RTMP or RTMPS push. For H.264 it recommends about 4 megabits per second at 720p30, and about 10 megabits per second at 1080p30. Keyframes about every 2 seconds, and never slower than every 4 seconds. Audio, when a later step adds it, is AAC or MP3, with 128 kilobits per second listed as the usual audio rate. YouTube's help also lists phone encoder apps, so a phone push is a real path.

Source: [YouTube encoder settings](https://support.google.com/youtube/answer/2853702).

**Confirmed, with a limit.** The YouTube Android app's own Go Live button uses YouTube's camera, or a screencast of the phone. It asks for at least 50 subscribers, a verified channel, and a wait that can be 24 hours the first time. That in-app camera path does **not** carry OFFGRID's painted frames. OFFGRID's frames go out only if the phone pushes RTMP, or if YouTube is screencasting a screen that shows OFFGRID.

Source: [YouTube live on Android](https://support.google.com/youtube/answer/9228390).

**Confirmed, Kick.** Kick's help page tells a creator to copy the stream key from the Kick dashboard in a browser, paste it into a phone streaming app, and push RTMPS. The page is written for a phone. The key itself is not copied into this paper.

Source: [Kick, streaming from a mobile phone](https://help.kick.com/en/articles/7135289-streaming-on-kick-from-your-mobile-phone).

**Confirmed from earlier research, still in force.** Instagram's Live Producer key is copied from instagram.com on a computer. That path is **not applicable (needs a computer)**. Zoom's own RTMP out needs the desktop app and a paid plan. That path is **not applicable** too. Details stay in `02 Research/Messenger Instagram WhatsApp Research.md` and `02 Research/Other Android Video Call Apps Research.md`.

**Guess.** Whether YouTube Studio, Twitch, Trovo, Bigo, and Facebook show a stream key in a phone browser is a Step 10 test. This paper does not mark those keys as working on a phone. The library can push to any RTMPS address. The platform folder stays empty until the key can be copied on the phone.

### (b) Screen share inside calling apps

The calling app shares the screen. OFFGRID's job is to be the picture on that screen, full screen, with the effects already painted.

**Confirmed, Android itself.** From Android 14, a screen share can be one app window instead of the whole phone. The status bar, the navigation bar, and notifications stay out of the share. The user picks the app in the system sheet. Sharing the whole screen can show notifications. Sharing one app is the better default.

Sources: [Media projection](https://developer.android.com/media/grow/media-projection), [App screen sharing](https://developer.android.com/about/versions/14/features/app-screen-sharing).

OFFGRID does **not** need to capture the screen itself for this path. The calling app does the capture, after Royalty taps Share and picks OFFGRID. OFFGRID only has to show the finished picture.

**Confirmed routes on Android, from official pages or from the research already in this repo:**

| App | What the phone can do | What is not applicable |
|---|---|---|
| Google Meet | Share the screen from the Meet Android app. A Pixel help page shows "a single app" as a choice, which hides notifications. | A fake webcam in the computer browser. |
| Zoom | The Zoom Android app can share the phone screen. Zoom's page says Android 10 or newer. Both target phones are newer than that. | Zoom's RTMP out, which needs the desktop app. |
| Discord | Go Live / screen share in the Android app. | A fake webcam on the computer. |
| Microsoft Teams (Free) | Screen share in the Android app. Meetings cap at 60 minutes on the free plan. | A fake webcam on the computer. |
| Instagram, Messenger, WhatsApp | Screen share during a video call in the Android app. | Instagram Live Producer (computer). A fake webcam. Modified WhatsApp apps. |
| Viber, LINE, Signal | No Android screen-share route was confirmed. | Their share tools are on a computer, so they stay out. |

Sources:

- Meet on Android: https://support.google.com/meet/answer/9308856?hl=en&co=GENIE.Platform%3DAndroid
- Single-app share write-up: https://support.google.com/pixelphone/answer/2865484
- Zoom screen share, including Android, Android 10 or newer: https://support.zoom.com/hc/en/article?id=zm_kb&sysparm_article=KB0060596
- Discord mobile screen share: https://support.discord.com/hc/en-us/articles/360058862134--Mobile-Screenshare-FAQ
- Instagram, Messenger, and WhatsApp share pages, plus the Teams, Viber, LINE, and Signal notes: `02 Research/Messenger Instagram WhatsApp Research.md` and `02 Research/Other Android Video Call Apps Research.md`

**Confirmed, later, not Step 1.** Zoom's Meeting SDK can take your own video frames inside a meeting you host. That is a second way to send the same finished frames, without screen share. It needs Zoom developer setup. It is already researched. It is not part of the Step 1 build.

**Guess.** On a screen share, viewers see OFFGRID's window, so they also see any buttons that are normal Android views. Paint the real effects in the processor anyway, so the saved file, a later RTMP send, and the screen share all match. During a test, hide extra controls or keep them inside the painted frame.

**Guess, left for a test.** Whether each calling app also sends the phone's app sounds during screen share is not settled. This paper does not design that audio. Voice stays paused.

### (c) A local copy for the Step 1 test

**Confirmed.** CameraX `Recorder` saves the frames from `VideoCapture` to a file, a file descriptor, or the MediaStore. The usual file type is MP4. Audio is off unless the recording turns it on with `withAudioEnabled`. Step 1 leaves audio off. That keeps the voice pause, and the acceptance check is about the picture.

Source: [Recorder](https://developer.android.com/reference/androidx/camera/video/Recorder).

**Confirmed.** If the effect targets video capture, CameraX runs the processor before the recorder encodes. The shape painted in the processor is in the file. A Google reply on the watermark question says this directly. If the effect targets preview only, the file can miss the shape. Step 1 targets preview and video capture together.

**Confirmed.** Under the hood, Android's `MediaCodec` can take a surface as the encoder input (`createInputSurface`). `MediaMuxer` then writes the shrunk video into an MP4. CameraX's recorder already does this. Step 1 should use the recorder rather than a hand-rolled muxer, so Step 0's CameraX path stays in charge.

Sources: [MediaCodec](https://developer.android.com/reference/android/media/MediaCodec), [MediaMuxer](https://developer.android.com/reference/android/media/MediaMuxer).

**The acceptance check, in plain words:**

1. Open the app on a phone.
2. Paint a bright test shape in the top layer of the processor.
3. Record a few seconds to a file in the app's private folder. Do not upload it.
4. Play that file.
5. The shape is visible in the file.
6. A shape that was only a view on top of the preview, and is missing from the file, fails the check.

The viewer-side success code (a number burned into the video and read on a second phone) belongs to the Build part and to later features. This research part cannot produce that code, because nothing is built.

---

## 3. Speed and battery on the two phones

### The phones

**Confirmed, Samsung, Galaxy S25 Ultra.** 6.9 inch screen, 3120 by 1440 pixels, up to 120 Hz (the screen can refresh 120 times a second). Snapdragon 8 Elite for Galaxy. 12 GB of memory. 5,000 mAh battery. Samsung says up to 31 hours of video playback, and a larger vapor chamber to keep the phone cooler while gaming.

Sources: [Samsung, Galaxy S25 Ultra](https://www.samsung.com/us/smartphones/galaxy-s25-ultra/), [Samsung business spec page](https://www.samsung.com/levant/business/smartphones/galaxy-s/galaxy-s25-ultra-sm-s938bzkimea/).

**Reported.** GSMArena lists the graphics chip as Adreno 830, the chip as 3 nm Snapdragon 8 Elite, and the release as February 2025. A third-party device page lists OpenGL ES 3.2 for that graphics chip. Samsung's own pages read here did not print the OpenGL version string.

Sources: [GSMArena, S25 Ultra](https://www.gsmarena.com/samsung_galaxy_s25_ultra-13322.php), [Soft9 device page](https://soft9.us/devices/samsung/pa3q).

**Confirmed, Samsung, Galaxy A37 5G.** Exynos 1480. Samsung says the processor and graphics are up to 12 percent and 19 percent faster than the previous model. 5,000 mAh typical battery (rated minimum 4,905 mAh). Super Fast Charging can reach about 60 percent in 30 minutes with a 45 watt charger. A vapor chamber is there to limit heat. These numbers are Samsung's lab claims, not an OFFGRID test.

Source: [Samsung, Galaxy A37 5G](https://www.samsung.com/us/smartphones/galaxy-a37-5g/).

**Reported.** GSMArena lists the A37 as released 2026-04-09, Android 16, a 6.7 inch 1080 by 2340 screen at 120 Hz, Exynos 1480, Xclipse 530 graphics, 6 to 12 GB of memory, and a 5,000 mAh battery.

Source: [GSMArena, Galaxy A37](https://www.gsmarena.com/samsung_galaxy_a37_5g-14378.php).

### What size to send

The screen size is not the video size. The S25 Ultra screen is 1440 by 3120. The A37 screen is 1080 by 2340. Sending that many pixels would make the graphics chip, the encoder, and the upload work much harder, and most sites do not want it.

| Setting | Both phones at first | S25 Ultra only after a test | Leave out |
|---|---|---|---|
| Picture size | 720 by 1280 (tall 720p) | 1080 by 1920 (tall 1080p) | Full screen size, 4K |
| Frame rate | 30 fps | 60 fps only if the A37 still holds 30 without getting hot | Above 60 |
| Shrink method | H.264, hardware encoder | Same | A software encoder, unless the hardware one fails |
| Live bitrate, later | YouTube's H.264 tip for 720p30 is 4 megabits per second | YouTube's H.264 tip for 1080p30 is 10 megabits per second | A bitrate the phone's upload cannot hold |
| File test | A short MP4 is enough. Bitrate only changes file size. | Same | Uploading the test file |

**Confirmed.** YouTube's table is the source of the 4 and 10 megabit tips. Instagram's Live Producer page, already cited in the older research, asks for a tall 9:16 picture and recommends 720p at 30 fps, with 60 fps also allowed. That matches the tall 720 by 1280 default. The Instagram key itself still needs a computer, so OFFGRID does not use that key.

**Guess.** 720 by 1280 at 30 fps is the right first target for the A37. The Exynos 1480 is a mid-range chip. One full-screen camera draw plus a few see-through layers is a light draw. The costly parts are the screen staying on, the camera staying on, the hardware encode, and, later, the upload. Particle effects (emoji rain, fireworks, shatter) cost more because they paint many see-through pixels. The A37 is the phone that decides the default. If it drops frames or gets hot, the S25 Ultra uses the same lower setting so both phones match.

**Guess.** A 30 fps cap inside the compositor saves battery even though both screens can refresh at 120 Hz. The outgoing video does not need 120 pictures a second. Sites in this project are built around 30.

**Confirmed gap.** Nobody has measured OFFGRID on these phones. Samsung's "31 hours of video" is playback of a movie, not camera plus effects plus encode plus upload. Do not treat it as a stream-time promise. The Step 1 build should watch heat and frame rate on the A37 for a few minutes of recording. A long battery run belongs with the replay-buffer step (Step 9), which already says to check the A37.

**Guess.** Use one off-screen texture at the video size, then scale it up for the on-screen preview. Drawing effects at screen size and shrinking afterward wastes the graphics chip.

---

## 4. Recommended design

1. **CameraX stays the camera owner.** Step 0 opens the front camera. Step 1 adds one effect. Nothing else opens the camera.
2. **One `CameraEffect`, targets preview and video capture.** One `SurfaceProcessor`. OpenGL ES.
3. **Three layers, always in this order:** camera, then the effect list, then the top layer. The test shape is the top layer for the acceptance file.
4. **Paint once, copy to each output.** Preview surface and recorder surface in Step 1. A live-send surface is a later step.
5. **Record with CameraX `Recorder` to a private file.** Audio off. No upload. No stream key in this step.
6. **Default size 720 by 1280 at 30 fps** on both phones. H.264 through the hardware encoder.
7. **When a later step adds a live send, use StreamPack 3.2.1** as the RTMP library, fed by the finished texture, without a second camera. RootEncoder 2.8.1 is the backup if StreamPack cannot take an outside picture. The stream key goes in the Android Keystore at run time.
8. **Calling apps use screen share of OFFGRID, full screen, one app if the sheet offers it.** Zoom's SDK frame path stays a later platform feature.
9. **Computer tools stay out.** OBS, fake webcams, and computer-only keys are not applicable.
10. **No voice in this step.** The recorder does not enable the mic.

---

## 5. Build plan for Step 1

Agent 04 can check the future build against this list. This paper does not do the build.

The build starts only after Royalty reviews this paper and says go, and only in `04 Build`. It should sit on top of Agent 01's Step 0 project. It should not replace that project.

1. Use CameraX stable 1.6.2 (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-video`, `camera-view`). Skip the 1.7 alpha unless a bug forces it.
2. Bind preview, video capture, and one effect in one `UseCaseGroup`. Effect targets: preview and video capture. No second effect.
3. In the processor, draw the camera, then the effect list, then one solid test shape on the top layer. Apply CameraX's transform matrix so the file is upright.
4. Show that painted surface in the preview, so the screen matches the file.
5. Add a record control that uses typed actions only. No sliders. Record a few seconds with `Recorder` to app-private storage. Leave audio off.
6. Play the file back from the phone, or copy it by a means Royalty already uses, and confirm the test shape is in the file.
7. Run that check on the Galaxy S25 Ultra and the Galaxy A37. Watch for dropped frames and heat on the A37 at 720 by 1280, 30 fps.
8. Do not add StreamPack or RootEncoder in this build. Do not type a stream key. Do not add voice, a mic track, or Rex.
9. Do not touch platform apps, mod them, or try to dodge an integrity check. Screen share, when it is tested later, is the calling app's own Share button.

**Pass, when the build exists, means:** the recorded file shows the test shape on both phones. A shape that exists only on the live preview fails.

**This research paper is not that pass.** There is no file yet, and there is no viewer-side success code yet.

---

## 6. What this paper leaves out

- Voice effects, typed-to-speech, the Rex voice, and any mic mix. Paused for Royalty.
- The terminal panel, folders, and toggles. Those are Step 2.
- Face tracking. That is Step 4.
- Which platforms will show a stream key on a phone. That is Step 10, except where an older paper already marked a key as computer-only.
- Zoom's Meeting SDK inside OFFGRID. Already researched. Not this step.
- Any code under `04 Build`.

---

## 7. Open questions

1. Can StreamPack 3.2.1 take a custom video source that does not open the camera, so CameraX stays the only camera client? The docs say custom sources exist. Nobody has tried it in this repo.
2. Which stream keys can Royalty copy on a phone browser, with no computer? Kick's help page describes a phone path. YouTube, Twitch, Trovo, Bigo, and Facebook are still unchecked. Instagram Live Producer and Zoom's desktop RTMP stay not applicable.
3. How many minutes can the A37 record 720 by 1280 at 30 fps, with the camera, the test shape, and the hardware encoder on, before it gets hot or drops frames? There is no measurement yet.
4. Does the A37 front camera give CameraX a steady 720 by 1280 or 1080 by 1920 stream? Untested on the device.
5. The `CameraEffect` page and the `UseCaseGroup` page do not list the legal target sets in exactly the same words. The build should use preview plus video capture, which both pages allow, and should confirm it on both phones.
6. During screen share, do Meet, Zoom, Discord, Teams, Instagram, Messenger, and WhatsApp send the phone's sounds? Unverified. Out of scope while voice is paused.
7. Step 0 is being built at the same time by Agent 01. This paper assumes Step 0 is CameraX plus a front-camera preview and that it does not already install a `CameraEffect`. If Step 0 does install one, the Step 1 build has to extend that effect instead of adding a second one.

---

## Sources

**Android, camera and drawing**

- CameraX releases (stable 1.6.2, including camera-effects): https://developer.android.com/jetpack/androidx/releases/camera
- CameraEffect: https://developer.android.com/reference/androidx/camera/core/CameraEffect
- SurfaceProcessor: https://developer.android.com/reference/androidx/camera/core/SurfaceProcessor
- UseCaseGroup.Builder: https://developer.android.com/reference/androidx/camera/core/UseCaseGroup.Builder
- OverlayEffect: https://developer.android.com/reference/androidx/camera/effects/OverlayEffect
- ToneMappingSurfaceProcessor sample: https://github.com/androidx/androidx/blob/androidx-main/camera/integration-tests/viewtestapp/src/main/java/androidx/camera/integration/view/ToneMappingSurfaceProcessor.kt
- Public answer on baking an overlay into the recording: https://stackoverflow.com/questions/75936230/unable-to-record-video-with-watermark-over-it-using-android-java-camerax-api
- SurfaceTexture: https://developer.android.com/reference/android/graphics/SurfaceTexture
- SurfaceTexture architecture: https://source.android.com/docs/core/graphics/arch-st
- Recorder: https://developer.android.com/reference/androidx/camera/video/Recorder
- MediaCodec: https://developer.android.com/reference/android/media/MediaCodec
- MediaMuxer: https://developer.android.com/reference/android/media/MediaMuxer
- Media projection: https://developer.android.com/media/grow/media-projection
- App screen sharing on Android 14: https://developer.android.com/about/versions/14/features/app-screen-sharing
- Why a normal app cannot be a virtual camera: https://source.android.com/docs/core/permissions/app-streaming

**RTMP libraries**

- StreamPack: https://github.com/ThibaultBee/StreamPack
- StreamPack releases: https://github.com/ThibaultBee/StreamPack/releases
- StreamPack advanced sources: https://github.com/ThibaultBee/StreamPack/blob/main/docs/AdvancedStreamer.md
- RootEncoder: https://github.com/pedroSG94/RootEncoder
- HaishinKit.kt: https://github.com/HaishinKit/HaishinKit.kt
- HaishinKit.kt docs: https://docs.haishinkit.com/kt/latest/
- NodeMedia client (commercial): https://github.com/nodemedia/nodemediaclient-android

**Sites and calling apps**

- YouTube encoder settings: https://support.google.com/youtube/answer/2853702
- YouTube live with an encoder (lists Android apps): https://support.google.com/youtube/answer/2907883
- YouTube live in the Android app: https://support.google.com/youtube/answer/9228390
- Kick on a mobile phone: https://help.kick.com/en/articles/7135289-streaming-on-kick-from-your-mobile-phone
- Google Meet screen share on Android: https://support.google.com/meet/answer/9308856?hl=en&co=GENIE.Platform%3DAndroid
- Share one app from the phone: https://support.google.com/pixelphone/answer/2865484
- Zoom screen share: https://support.zoom.com/hc/en/article?id=zm_kb&sysparm_article=KB0060596
- Discord mobile screen share: https://support.discord.com/hc/en-us/articles/360058862134--Mobile-Screenshare-FAQ
- Earlier OFFGRID papers used for the rest of the calling apps: `02 Research/Other Android Video Call Apps Research.md` and `02 Research/Messenger Instagram WhatsApp Research.md`

**Phones**

- Samsung, Galaxy S25 Ultra: https://www.samsung.com/us/smartphones/galaxy-s25-ultra/
- Samsung business specs, S25 Ultra: https://www.samsung.com/levant/business/smartphones/galaxy-s/galaxy-s25-ultra-sm-s938bzkimea/
- GSMArena, S25 Ultra: https://www.gsmarena.com/samsung_galaxy_s25_ultra-13322.php
- Samsung, Galaxy A37 5G: https://www.samsung.com/us/smartphones/galaxy-a37-5g/
- GSMArena, Galaxy A37: https://www.gsmarena.com/samsung_galaxy_a37_5g-14378.php
