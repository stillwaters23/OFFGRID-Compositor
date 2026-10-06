# Other Android Video Call Apps Research

*For: OFFGRID-Compositor (a private Android app, sideloaded, meaning installed directly and not from the Google Play Store). Researched October 6, 2026 (web only; nothing was downloaded, installed, or logged in to).*

## Bottom Line

- **The rule:** viewers have to see the effects on their phones. That means the effects must be **burned in**, which means drawn into the actual video pixels before they leave your device. Only an app's own built-in filters show up natively, and none of these apps lets an outside app add its own filters.
- **Stock Android has no virtual camera** (a fake webcam that other apps can pick in place of the real one). So on the phone, OFFGRID cannot pose as a camera inside another app. The workable routes on the phone are:
  1. **Screen share:** OFFGRID shows the composited picture full screen, and the call app shares the screen. This works on Android in **Google Meet, Zoom, Microsoft Teams, and Discord**.
  2. **Software Development Kit (SDK, a code library from the app maker):** only **Zoom** lets your own Android app send its own video frames into a real Zoom meeting.
- **On a computer:** every app here except Skype (shut down) has a desktop app or website that can use a virtual camera (for example OBS Studio's Virtual Camera) or screen sharing. That route always works, but it needs a computer.
- **Ratings for OFFGRID:**
  - **Works:** Zoom (best), Discord, Google Meet, Teams Free
  - **Partial** (computer needed): Viber, LINE, Signal, WeChat
  - **No:** Skype (retired May 5, 2025)
- **Platform folders:** make folders for **Zoom, Google Meet, Discord, and Microsoft Teams (Free)**. Skype goes in the Teams folder as a "retired" note. Viber, LINE, Signal, and WeChat can share one "Desktop-Only Apps" folder, or be skipped until someone actually needs them.

## Side-By-Side Table

| App | Developer API/SDK that touches live video? | Streams out (RTMP/stream key)? | Effects reach viewers how? | Works from a sideloaded Android app? | Best route for burned-in effects | OFFGRID rating | Folder? |
|---|---|---|---|---|---|---|---|
| **Zoom** | **Yes.** The Meeting SDK and Video SDK can both send your own video frames | Yes (paid Pro plan or higher, desktop app only) | SDK frames, screen share, or a virtual camera on desktop | **Yes** (the SDK is not tied to the Play Store) | Meeting SDK inside OFFGRID, or Android screen share | **Works** | **Yes** |
| **Google Meet** | Receive only (the Media API reads video; it cannot send). Add-ons are side panels with no camera access | YouTube streaming needs a paid Workspace or Google One 2 TB plan, and must start from a computer | Android screen share, or a virtual camera in desktop Chrome | No send-video API to use | Android "Share screen", or meet.google.com in Chrome with a virtual camera | **Works** (via screen share) | **Yes** |
| **Discord** | No. Embedded App SDK "Activities" run as web pages with no webcam access | No RTMP output | Go Live screen share on Android, or a virtual camera on desktop/web | Not applicable (no video API) | Android Go Live screen share, or discord.com/app in Chrome with a virtual camera | **Works** (via screen share) | **Yes** |
| **Microsoft Teams (Free)** | No for free accounts (developer tools and Azure interoperability are for work/school accounts only) | No (RTMP features are for business/admin-managed accounts) | Android screen share, or a virtual camera on desktop/web | No | Android screen share, or teams.live.com in Edge/Chrome with a virtual camera | **Works** (via screen share) | **Yes** (also holds the Skype note) |
| **Skype** | Retired | Retired | Retired | Retired | Use Teams Free | **No** | No (note inside Teams) |
| **Viber** | Chat bots only (no video) | No | Desktop screen share (desktop only, officially) | Bots only | Viber Desktop screen share | **Partial** | Optional (shared folder) |
| **LINE** | Messaging bots and LIFF mini web apps only. LINE Planet is a separate paid calling product, not LINE calls | No for normal accounts | Screen share in LINE for PC (version 5.23.0+) | Not for LINE calls | LINE for PC screen share | **Partial** | Optional (shared folder) |
| **WeChat** | Mini Program video parts (live-pusher, voip-room) exist but only for approved Chinese-registered businesses in certain categories | Channels (视频号) OBS push needs a verified account with 500+ followers and more | WeChat for Windows screen share (reports) | Not practical | WeChat PC screen share | **Partial/No** | No (unless needed) |
| **Signal** | No official API | No | Signal Desktop screen share | No | Signal Desktop screen share | **Partial** | Optional (shared folder) |

Plain-language notes on terms:
- **API** (Application Programming Interface) is a way for one program to talk to another.
- **RTMP** (Real-Time Messaging Protocol) is the standard way to push a live stream to services like YouTube or Twitch. A **stream key** is the password-like code for that stream.
- **OBS** (Open Broadcaster Software) Studio is a free program that combines video and effects and can act as a virtual camera.

---

## Google Meet

**Confirmed facts (official docs):**
1. **API/SDK:**
   - The **Meet Media API** (a Developer Preview, meaning early access you sign up for, since February 24, 2025) lets an app *receive* live audio and video from a meeting over WebRTC (Web Real-Time Communication). It is **receive-only**: up to three video streams come in, and nothing goes out. It uses "restricted" OAuth permission scopes (OAuth is Google's sign-in permission system), so the app needs Google's review to use it publicly.
   - **Meet Add-ons** (web pages in a side panel or the main stage) work on Android. They do not give access to the camera or to outgoing video.
   - Neither one can inject your own video.
2. **Streaming out:**
   - Streaming to YouTube works only on Enterprise Starter/Standard/Plus, Education Plus, Teaching & Learning Upgrade, Workspace Individual, or Google One with 2 TB+ of storage.
   - The YouTube channel must be approved for live streaming (this can take up to 24 hours).
   - You **must start from a computer**; it cannot be started from a phone.
   - There is no custom RTMP or stream key to OBS. In-house live streaming (inside your own organization) is limited to Enterprise and Education editions.
3. **Effects to viewers:** Only Meet's own effects (backgrounds and filters) go through. Some Samsung phones can use Galaxy Avatar as a camera. Anything else must be burned in, either shared as a screen or fed from a virtual camera on a computer.
4. **Registration:** You need a Google Cloud project, OAuth consent setup, and enrollment in the Developer Preview for the Media API. Restricted scopes need verification.
5. **Sideloaded app:** The APIs don't care how an app was installed, but no API sends video, so this doesn't help.
6. **Realistic route:**
   - On the phone: OFFGRID full screen plus Meet's Android **Share screen**.
   - On a computer: **meet.google.com in Google Chrome** with OBS Virtual Camera.
7. **Bottom line: Works (via screen share).** Give it a **folder**. Best route: the Meet Android app's screen share, with desktop Chrome at https://meet.google.com plus a virtual camera as a backup.

## Zoom

**Confirmed facts (official docs):**
1. **API/SDK, the strongest of all the apps here:**
   - **Meeting SDK for Android** joins real Zoom meetings. Its `ZoomSDKVideoSourceHelper.setExternalVideoSource()` replaces the camera with your own frames, and `setPreProcessor()` lets you edit camera frames before they're sent. That is exactly what burned-in effects need.
   - **Video SDK for Android** can also send raw frames ("custom video effects... watermarks, AR overlays applied per-frame before frames reach remote users"). But Video SDK sessions are **not** normal Zoom meetings: viewers would need an app you build, so it's only useful if you make the viewer side too.
   - Zoom also has RTMS (Real-Time Media Streams), which only *reads* meeting media.
2. **Streaming out:**
   - **Custom Live Streaming Service** (RTMP URL plus stream key) needs a **Pro, Business, Education, or Enterprise** account, a licensed host, the setting turned on, and the **Windows/macOS desktop app**.
   - The Zoom **Live Streaming app** can send to YouTube, Facebook, Twitch, or a custom RTMP target (up to 3 at once).
   - Zoom Events/Webinars Plus can also take **incoming** RTMP from tools like OBS.
3. **Effects to viewers:** Yes, burned in through SDK frames, a virtual camera on desktop, or screen share (Android screen share is supported in the Zoom app).
4. **Registration:**
   - You need a Zoom App Marketplace developer account, a Meeting SDK app, and signed JWT credentials (JWT, JSON Web Token, is a signed login token).
   - **Since March 2, 2026**, joining meetings **hosted outside your own account** needs Zoom's app review **and** a ZAK or "On Behalf Of" (OBF) token tied to a signed-in user, with SDK version 5.17.5 or newer.
   - Meetings **inside your own account** need only the JWT, with no Marketplace review ("internal-only" apps).
5. **Sideloaded app:** **Yes.** The SDK is a library you build into your own APK (Android app file). Nothing about it requires the Play Store. Keep it to meetings you host (your own account) to avoid the review step.
6. **Realistic route:**
   - Build the Meeting SDK into OFFGRID and push composited frames as the camera.
   - Simpler fallback: Android screen share in the official Zoom app.
   - On a computer: the Zoom desktop app with a virtual camera.
7. **Bottom line: Works (best option).** Give it a **folder**. Best route: OFFGRID's own Meeting SDK client for meetings you host. Fallback: the Zoom Android app's screen share. Desktop: the Zoom desktop app, or the web client at https://app.zoom.us in Chrome.

## Skype

**Confirmed facts:** Microsoft **retired Skype on May 5, 2025** and moved users to **Microsoft Teams Free** (you sign in with the same Skype account, and contacts and chats carry over). Skype data could be exported until January 2026.

1-6. Not applicable. The service is gone.
7. **Bottom line: No.** **No folder.** Put a one-line "retired, use Teams Free" note in the Teams folder.

## Microsoft Teams (Free / Personal)

**Confirmed facts (official docs):**
1. **API/SDK:**
   - The Teams developer tools (Microsoft Graph Teams APIs, Live Share, meeting apps) need **work or school (Entra ID) accounts**. Microsoft Q&A answers say personal accounts are not supported.
   - **Azure Communication Services** (Microsoft's paid calling SDK) can join *business* Teams meetings, but **cannot join teams.live.com (personal) meetings**. Microsoft Q&A confirms that the call locator "is not recognized."
2. **Streaming out:** **RTMP-In** and encoder streaming are admin-managed business features. Teams Free has no RTMP or stream-key option.
3. **Effects to viewers:** Only Teams' own backgrounds. Anything else must be burned in through screen share or a virtual camera on a computer.
4. **Registration/limits:** You need a free Microsoft account. Meetings are capped at **60 minutes** and up to 100 people, and Teams Free has no recording.
5. **Sideloaded app:** No usable API.
6. **Realistic route:**
   - On the phone: OFFGRID full screen plus screen share in the Teams Android app.
   - On a computer: **teams.live.com in Microsoft Edge or Google Chrome** (or the desktop app) with OBS Virtual Camera.
7. **Bottom line: Works (via screen share).** Give it a **folder** (this folder also carries the Skype note). Best route: the Teams Android app's screen share, with https://teams.live.com in Edge or Chrome plus a virtual camera on a computer.

## Viber

**Confirmed facts (official docs):**
1. **API:** The Viber REST Bot API is for **chat only** (text, pictures, video files, and so on). There is no calling or video-call API.
2. **Streaming out:** None.
3. **Effects to viewers:** Viber Desktop video calls have **Share screen** (whole screen or one app). Viber Support says this is a desktop feature, not available on mobile.
4. **Registration:** Bot accounts need a Viber partner/commercial approval process. That doesn't matter here, since bots can't do calls.
5. **Sideloaded app:** Not applicable (no video API).
6. **Realistic route:** A computer: Viber Desktop with screen share. (Reports: Viber Desktop may also list virtual cameras; not confirmed in official docs.)
7. **Bottom line: Partial.** **Optional** (shared "Desktop-Only Apps" folder). Route: the Viber Desktop app. There is no web version for calls.

## LINE

**Confirmed facts (official docs):**
1. **API:**
   - The LINE Messaging API (bots) and LIFF (LINE Front-end Framework, small web apps inside LINE) **do not touch calls**.
   - **LINE Planet** (from LY Corporation) is a separate paid calling SDK with screen share. It creates *its own* calls, not calls inside the LINE app, and its screen share doesn't work in mobile browsers.
2. **Streaming out:** No RTMP for normal LINE calls.
3. **Effects to viewers:** **LINE for PC (Windows/Mac) version 5.23.0+** has **Share screen** during video calls (official LINE guide). Reports (unverified): LINE mobile group calls have a limited "share screen" or watch-together feature. Its reliability for live composited video is unknown.
4. **Registration:** You need a LINE Developers account for the bot/LIFF APIs, and a business contract for LINE Planet.
5. **Sideloaded app:** LINE Planet could run in a sideloaded app, but viewers would need your own app, not LINE.
6. **Realistic route:** LINE for PC with screen share (or a virtual camera, which is a report).
7. **Bottom line: Partial.** **Optional** (shared folder). Route: the LINE for PC desktop app.

## WeChat

**Confirmed facts (official docs):**
1. **API:**
   - Mini Programs (small apps that run inside WeChat) have **live-pusher** (push live video) and **voip-room** (calls of up to 10 people) components.
   - Both are limited to **Mainland-China-registered businesses** in approved service categories (education, medical, finance, government, IT/video conferencing, social live streaming, and others) and must be turned on in the Mini Program admin site.
   - They don't touch normal WeChat calls.
2. **Streaming out:**
   - **WeChat Channels (视频号)** supports OBS push streaming with a push address and key.
   - Requirements (official Channels help, Chinese): a **verified** Channels account with at least **10 public videos, 3 past live streams, and 500 followers**.
3. **Effects to viewers:** Reports (news coverage of WeChat PC 4.0.2 beta, not official): WeChat for Windows group video calls have **Share Screen**. Phone calls can only use the camera.
4. **Registration:** Strict. You need Chinese business registration for the video components and identity verification for Channels.
5. **Sideloaded app:** Not practical.
6. **Realistic route:** WeChat for Windows/Mac screen share (report-level), or Channels live via OBS if you meet the follower bar.
7. **Bottom line: Partial/No.** **No folder** unless WeChat contacts specifically need it.

## Discord

**Confirmed facts (official docs):**
1. **API/SDK:** The **Embedded App SDK** runs "Activities" (web apps in a frame inside Discord, including on Android). A Discord developer discussion on GitHub says Activities **have no webcam/video access**. Bots can join voice channels for audio, but the official API doesn't let them send video. **No route through the SDK.**
2. **Streaming out:** No RTMP or stream-key output.
3. **Effects to viewers:**
   - **Go Live / screen share works on Android** (Android 5+, plus sound on Android 10+) in direct-message calls and server voice channels (official Discord support).
   - On a computer or the web, you can pick a virtual camera as your camera. (Reports: OBS Virtual Camera works in the desktop app and in Chrome after a restart.)
4. **Registration:** Activities need a Discord Developer Portal app. Not needed for the screen-share route.
5. **Sideloaded app:** Not applicable. OFFGRID just has to be what's on screen.
6. **Realistic route:** OFFGRID full screen plus Discord Android **Screenshare/Go Live**. On a computer: https://discord.com/app in **Google Chrome** (Chrome is the only browser that shares app audio for Go Live) or the desktop app, with a virtual camera.
7. **Bottom line: Works (via screen share).** Give it a **folder**. Best route: the Discord Android app's Go Live. Desktop/web backup: Chrome at https://discord.com/app.

## Signal

**Confirmed facts (official docs):**
1. **API:** **No official developer API or SDK.** (Reports: unofficial tools like signal-cli exist, but they're for messaging only, not supported for calls, and risky.)
2. **Streaming out:** None.
3. **Effects to viewers:**
   - **Signal Desktop** video calls support **screen sharing** (whole screen or one window; your camera is hidden while sharing; one presenter at a time).
   - Signal Desktop lets you switch cameras, so a virtual camera can be chosen if the operating system shows one (report-level for virtual cameras).
   - Reports: no screen share in the Android app.
4. **Registration:** A phone number is required. No developer program.
5. **Sideloaded app:** No API.
6. **Realistic route:** Signal Desktop with screen share or a virtual camera.
7. **Bottom line: Partial.** **Optional** (shared folder). Route: the Signal Desktop app. There's no web version.

---

## Confirmed Facts Vs. Reports (Summary)

**Confirmed (official docs/help pages):**
- Zoom Meeting SDK external video source on Android, and Video SDK send/raw-data effects.
- Zoom app review plus OBF/ZAK rule from March 2, 2026 for meetings outside your account.
- Zoom Custom Live Streaming needs a paid plan and the desktop app.
- Meet Media API is receive-only and in Developer Preview.
- Meet YouTube streaming editions, and that streaming must start from a computer.
- Meet add-ons work on Android.
- Skype retired May 5, 2025.
- Teams Free has no developer APIs for personal accounts, and Azure Communication Services can't join teams.live.com meetings (Microsoft Q&A).
- Viber's bot API is chat-only, and screen share is desktop-only.
- LINE PC screen share in version 5.23.0+.
- WeChat Mini Program video components are restricted, and Channels OBS push has eligibility rules.
- Discord Android screen share, and Chrome-only audio for browser Go Live.
- Signal Desktop screen share.

**Reports (forums, news, third-party guides; not confirmed by the app maker):**
- Discord Activities have no webcam access (developer discussion on GitHub).
- OBS Virtual Camera works in Discord desktop/web.
- WeChat PC group call screen share (news).
- Viber Desktop and Signal Desktop list virtual cameras.
- LINE mobile has some screen sharing.
- Signal Android has no screen share.

## Sources

- Google Meet Media API overview: https://developers.google.com/workspace/meet/media-api/guides/overview
- Meet Media API concepts (receive-only): https://developers.google.com/workspace/meet/media-api/guides/concepts
- Meet Media API get started (scopes): https://developers.google.com/workspace/meet/media-api/guides/get-started
- Meet REST API release notes: https://developers.google.com/workspace/meet/release-notes
- Meet add-ons on Android: https://workspaceupdates.googleblog.com/2024/06/google-meet-add-ons-available-on-android-devices.html
- Meet add-ons quickstart: https://developers.google.com/workspace/meet/add-ons/guides/quickstart
- Live stream Meet to YouTube: https://support.google.com/meet/answer/12383549
- Meet live stream for hosts: https://support.google.com/meet/answer/9308630
- Admin, turn on Meet live streaming: https://support.google.com/a/answer/9055446
- Zoom Meeting SDK Android ZoomSDKVideoSourceHelper: https://marketplacefront.zoom.us/sdk/meeting/android/us/zoom/sdk/ZoomSDKVideoSourceHelper.html
- Zoom Video SDK Android send raw data: https://developers.zoom.us/docs/video-sdk/android/raw-data/send-raw-data/
- Zoom Video SDK raw data overview: https://developers.zoom.us/docs/video-sdk/android/raw-data/
- Zoom Meeting SDK authorization: https://developers.zoom.us/docs/meeting-sdk/auth/
- Zoom OBF FAQ (March 2, 2026): https://developers.zoom.us/docs/meeting-sdk/obf-faq/
- Zoom OBF transition blog: https://developers.zoom.us/blog/transition-to-obf-token-meetingsdk-apps/
- Zoom submit apps for review: https://developers.zoom.us/docs/distribute/app-submission/submit-apps-review/
- Zoom custom live streaming: https://support.zoom.com/hc/en/article?id=zm_kb&sysparm_article=KB0064210
- Zoom Live Streaming app: https://support.zoom.com/hc/en/article?id=zm_kb&sysparm_article=KB0060769
- Zoom incoming RTMP (Events/Webinars Plus): https://support.zoom.com/hc/en/article?id=zm_kb&sysparm_article=KB0077025
- Skype retirement: https://support.microsoft.com/en-US/Skype/skype-is-retiring-in-may-2025-what-you-need-to-know
- Moving from Skype to Teams Free: https://support.microsoft.com/en-us/teams/free/settings/moving-from-skype-to-microsoft-teams-free
- Teams Free subscriptions/limits: https://support.microsoft.com/en-us/teams/free/get-started/learn-more-about-subscriptions-for-microsoft-teams-free
- Teams RTMP-In (business): https://learn.microsoft.com/en-us/microsoftteams/meetings-rtmp-in
- Azure Communication Services join Teams meeting: https://learn.microsoft.com/en-us/azure/communication-services/concepts/join-teams-meeting
- Microsoft Q&A, ACS can't use teams.live.com: https://learn.microsoft.com/en-au/answers/questions/2124051/azure-communication-service-error-provided-call-lo
- Microsoft Q&A, personal accounts can't connect Teams apps: https://learn.microsoft.com/en-us/answers/questions/2183667/personal-microsoft-accounts-can-t-connect-teams-to
- Viber REST Bot API: https://developers.viber.com/docs/api/rest-bot-api/
- Viber desktop calls / share screen: https://help.viber.com/hc/en-us/articles/9083535709341-Make-voice-and-video-calls-from-your-desktop
- LINE PC video call guide (Japanese): https://guide.line.me/ja/chats-calls-notifications/calls-and-chatlives/videocall-pc.html
- LINE Planet screen share: https://docs.lineplanet.me/web/extended-functions/presentation-screen-share
- LIFF docs: https://developers.line.biz/en/docs/liff/developing-liff-apps/
- WeChat live-pusher: https://developers.weixin.qq.com/miniprogram/en/dev/component/live-pusher.html
- WeChat voip-room: https://developers.weixin.qq.com/miniprogram/en/dev/component/voip-room.html
- WeChat Channels push-stream guide (Chinese): https://findeross.weixin.qq.com/cgi-bin/mmfindernodelivecrmwebbroker-bin/helper-center/pages/6Rudn9LXxi0H1Rto
- Report, WeChat PC 4.0.2 screen share (news): https://www.todaymg.com/a/wei-xin-pcban-4-0-2nei-ce-qun-shi-pin-tong-hua-zhi-chi-gong-xiang-ping-mu-ke-gen-shou-ji-hu-xiang-qian-yi-liao-tian-ji-lu.html
- Discord Activities overview: https://docs.discord.com/developers/activities/overview
- Discord Embedded App SDK: https://docs.discord.com/developers/developer-tools/embedded-app-sdk
- Report, Activities webcam access discussion: https://github.com/discord/discord-api-docs/discussions/7183
- Discord mobile screenshare FAQ: https://support.discord.com/hc/en-us/articles/360058862134--Mobile-Screenshare-FAQ
- Discord Go Live and Screen Share: https://support.discord.com/hc/en-us/articles/360040816151-Go-Live-and-Screen-Share
- Report, OBS Virtual Camera on Discord guide: https://www.aicameratools.com/guides/obs-virtual-camera-discord/
- Signal Desktop screen sharing: https://support.signal.org/hc/en-us/articles/5369688043418-Screen-Sharing-with-Signal-Desktop-Calls
- Signal calling options (switch camera): https://support.signal.org/hc/en-us/articles/5369785769370-Calling-options-Switch-camera-microphone-or-view
