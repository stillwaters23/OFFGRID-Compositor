# Messenger, Instagram, And WhatsApp Research

*For: OFFGRID-Compositor (a private Android app, sideloaded, meaning installed directly and not from the Google Play Store). Researched October 6, 2026 (web only; nothing was downloaded, installed, or logged in to). Earlier research still applies: Meta shut down Meta Spark (the tool for making third-party camera effects) on January 14, 2025, and no app can draw overlays on someone else's screen during an Instagram, WhatsApp, or Messenger call.*

## Bottom Line

- **The rule:** viewers must see the effects on their phones. The only way to do that with Meta's apps is to **burn the effects in**, meaning they are drawn into the actual video picture (and mixed into the audio) *before* it leaves your device. Meta's apps only show their own built-in filters as native effects, and outside apps can't add new ones.
- **Instagram — Works (for Instagram Live broadcasts).** Instagram Live Producer gives you a stream key (a password-like code for sending a live stream). Anything you send with that key, including burned-in effects, the terminal panel, overlays, and voice effects, is exactly what viewers see. **The catch:** you need a professional (Creator or Business) account that is **public with at least 1,000 followers**, and Live Producer is still "limited access," so it may not show up on your account. The key comes from instagram.com in a desktop browser. **Instagram calls (direct message video calls) are only Partial:** you can screen share from the phone or use a virtual camera on a computer.
- **Messenger — Partial.** Messenger has **no stream key and no API for personal calls**. The Messenger Calling API (a business calling tool) does send real video into Messenger calls, but only from a business Facebook Page, and **the United States is not on its list of countries**. The workable routes are **screen sharing** from the phone during a Messenger call, or a **virtual camera** (a fake webcam) on a computer in a browser call at facebook.com/messages (messenger.com closed in April 2026). For broadcasts, use **Facebook Live** instead (it has RTMP stream keys and an API), then share the link in Messenger.
- **WhatsApp — Partial.** WhatsApp has **no live streaming and no stream key**. The WhatsApp Cloud API (the official developer tool) is for **business messaging only**. Its Calling API can put a business's own audio into calls, but its docs mark video as "planned or in development." Personal-call routes: **screen share** from the phone during a WhatsApp video call, or a virtual camera on a computer (the new WhatsApp Web browser calling from July 2026, or WhatsApp Desktop on Windows with a third-party driver, based on user reports). **Never** use a modified WhatsApp app (like GB WhatsApp); WhatsApp bans accounts that do.
- **Best single route for OFFGRID:** build an **RTMPS output** (RTMPS is the encrypted version of RTMP, the standard way to push a live stream) into OFFGRID itself. Then OFFGRID can stream straight from the phone to **Instagram Live** (using a Live Producer key) and to **Facebook Live** (using a key or the Facebook Live Video API). For calls on all three apps, use **screen share** on the phone, or send OFFGRID's output to a computer and use OBS Studio's Virtual Camera there.

## Words Used In This Report

- **API** (Application Programming Interface): a set of commands one program uses to talk to another program or service.
- **SDK** (Software Development Kit): a ready-made code library from a company that you build into your app.
- **RTMP / RTMPS** (Real-Time Messaging Protocol / RTMP Secure): the standard way to push a live video stream to a service like Instagram Live or Facebook Live. RTMPS is the encrypted version. A **stream key** is the secret code that tells the service which live video your stream belongs to.
- **OBS** (Open Broadcaster Software) Studio: a free computer program that mixes video and effects. It can stream with a stream key, and its **Virtual Camera** shows up as a webcam in other programs.
- **Virtual camera:** a fake webcam that shows a program's output instead of a real camera. Computers support this. Stock Android (the normal, unmodified Android on phones) doesn't let regular apps create one.
- **Burned-in:** the effect is part of the video pixels (or audio) itself, so every viewer sees it on any device. **Native overlay:** a sticker, filter, or caption the app (Instagram, WhatsApp, etc.) draws itself.
- **WebRTC** (Web Real-Time Communication): the technology most video calls use to send live audio and video.
- **Meta App Review:** Meta checks your app before it can use certain permissions with people who don't have a role on the app. **Business Verification:** Meta checks your business's legal identity. **Standard Access / Advanced Access:** Standard is the default and covers people with a role on your app (like you). Advanced covers everyone else, and needs App Review (and usually Business Verification).

## Side-By-Side Table

| Question | Instagram | Messenger | WhatsApp |
|---|---|---|---|
| **1. Developer API or SDK today** | Instagram Platform (Instagram API with Instagram Login, or with Facebook Login) for **professional accounts only**: posting, comments, messages, insights. The old Basic Display API for personal accounts ended December 4, 2024 | Messenger Platform (business Pages reply to people who message them first). **Messenger Calling API** (business Page voice and video calls over WebRTC) | WhatsApp Business Platform / **Cloud API** (business messaging). **Calling API** (business voice calls over WebRTC; video listed as planned or in development) |
| **Does any API touch live video or calls?** | **Read only.** `live_media` lists your live video *while it's on air* and can read live comments. There's **no API to start or send a live stream**, and no calling API | **Yes, for business Pages only.** The Calling API sends and receives audio and video in calls between a Page and a person. Personal calls: no | **Yes, for business numbers only.** Calls between a business number and a WhatsApp user. Personal calls: no |
| **2. OBS or RTMP stream key** | **Yes:** Live Producer on instagram.com (desktop browser). Professional account, public, 1,000+ followers, and "limited access." The key changes every session | **Messenger itself: no.** Facebook Live (a separate feature) has RTMPS keys and the Live Video API. The account must be 60+ days old with 100+ followers | **None.** No live streaming feature and no stream key |
| **3. Custom text, overlays, effects to viewers** | Burned-in: **yes** (through Live Producer). Native: only Meta's own effects in the app, plus things like titles and pinned comments. No custom native effects since Spark closed | Burned-in: only through screen share or a computer virtual camera in calls. Native: only Meta's own effects | Burned-in: only through screen share or a computer virtual camera in calls. Native: WhatsApp's own 10 filters and 10 backgrounds only |
| **4. Registration and restrictions** | Meta developer account plus an app. Standard Access is enough for **your own** account (no App Review). Rate limit: 4,800 x impressions per 24 hours; 100 API posts per 24 hours | Developer account, app, and Facebook Page. App Review only if other people use your app. Rate limit: 200 x engaged users per 24 hours. Calling only in listed countries (**United States not listed**) | Business portfolio, business phone number, Cloud API app. Calling needs a messaging limit of **2,000+** (reach it through Business Verification). Modified apps (GB WhatsApp, WhatsApp Plus) get banned |
| **5. Can a private, sideloaded Android app use it?** | Yes for your own account (login through a web page, not an Android SDK). But there's no live-sending API, so the useful part is the stream key you paste in | Yes technically, but Calling is for Pages, in listed countries. Facebook Live API: yes (Facebook Login works without the Play Store) | Only as a "business." Cloud API is not for personal chats or calls |
| **6. Best burned-in route** | **OFFGRID streams RTMPS from the phone using the Live Producer key** (or OBS on a computer). Calls: screen share | Calls: screen share on the phone, or OBS Virtual Camera in a browser at facebook.com/messages. Broadcast: Facebook Live through RTMPS | Calls: screen share on the phone, or a virtual camera with WhatsApp Web / Desktop on a computer |
| **7. Rating** | **Works** (Live) / Partial (calls) | **Partial** | **Partial** |

---

## 1. Developer API Or SDK For Third-Party Integration

### Instagram
- **Confirmed:** The **Instagram Platform** is "a collection of APIs that allows your app to access data for Instagram professional accounts, including business and creator accounts." It comes in two setups: **Instagram API with Instagram Login** (no Facebook Page needed) and **Instagram API with Facebook Login** (account linked to a Facebook Page). It covers messaging, comment moderation, publishing posts, and insights. (Meta, Instagram Platform Overview, updated September 28, 2026)
- **Confirmed:** The **Instagram Basic Display API** (the only API for *personal* accounts) **ended December 4, 2024**. Today's API only works with professional accounts. (Meta developer blog; TechCrunch)
- **Confirmed (live video is read only):** `GET /{ig-user-id}/live_media` returns live videos "being broadcast at the time of the request" and can include their comments. There is **no endpoint to create or send a live broadcast**. A `live_comments` webhook (an automatic notice sent to your server) exists, but it requires Advanced Access through App Review. (Meta, IG User Live Media reference; Instagram Webhooks docs)
- **Confirmed:** The publishing API posts images, videos, reels, and carousels. It does not go live. (Meta, Content Publishing)
- **Not found:** No public "Instagram Live API" for sending a stream, and no calling API for Instagram. The only official way to send your own live video to Instagram is Live Producer (section 2).
- **Useful for OFFGRID (Guess):** OFFGRID could check `live_media` with comments every few seconds during your own live and show those comments inside the burned-in terminal panel.

### Messenger
- **Confirmed:** The **Messenger Platform** lets "a business' Facebook Page or Instagram Professional account respond to people who message them." "A person must initiate the conversation." The usual reply window is 24 hours. (Meta, Messenger Platform Overview, updated September 28, 2026)
- **Confirmed (touches calls):** The **Messenger Business Calling API** "enables VoIP communication between consumers and businesses on Messenger" (VoIP means voice over internet). Your app swaps connection details with Meta through the Graph API (Meta's main API), and then "audio and video streams flow directly over the WebRTC peer connection." Features include business-to-person calls (with the person's permission) and person-to-business calls. **Video calling** works too: the business can turn its own video on through a `media_update` request, with VP8, VP9, H.264, H.265, and AV1 video formats. (Meta, Messenger Calling docs, April and March 2026)
- **Confirmed (big limit):** Calling is "currently available for Pages" in a fixed list of countries (Angola, Argentina, Australia, Indonesia, Pakistan, Philippines, Turkey, Ukraine, Vietnam, and others). **The United States, Canada, and the United Kingdom are not on the list.** It works from a **Page** (a business identity), not from your personal Messenger profile.
- **Confirmed:** Messenger has no developer tool for joining or changing a normal person-to-person Messenger call.

### WhatsApp
- **Confirmed:** The **WhatsApp Business Platform / Cloud API** is for businesses. The Get Started guide says it "is for developers building on the WhatsApp Business Platform. If you are a WhatsApp user experiencing issues with your personal account," go to the Help Center instead. (Meta, WhatsApp Cloud API Get Started, June 2026)
- **Confirmed (touches calls):** The **WhatsApp Business Calling API** lets a business number make and receive calls with WhatsApp users using VoIP. Media goes over WebRTC with the Opus audio format. Requirements include: the number must be on Cloud API (not the WhatsApp Business app), a `whatsapp_business_messaging` permission, a **daily messaging limit of at least 2,000**, and calling turned on. (Meta, Cloud API Calling, updated June 26, 2026)
- **Confirmed (video status is mixed):** The Calling overview lists "Video\*, screen share\*" with the note "\* Feature planned or in development. Reach out to your Meta or partner for more details." The Settings API reference has a field to "enable or disable video calling."
- **Report:** In Meta's *Conversations 2026* talk (YouTube), a Meta solutions architect said video calling is new in 2026, needs the H.264 video format, works over WebRTC only, and that screen sharing comes in June 2026. Treat video as **rolling out / partner access**.
- **Confirmed:** **Business-initiated calls** (the business calls the user) are **not available if the business phone number is from the United States, Canada, Egypt, Vietnam, or Nigeria**. User-initiated calls (the user calls the business) work everywhere Cloud API works. A business needs the user's permission before calling them. There's a limit of 10,000 new calls per 24 hours per business number.
- **Confirmed:** There is no API for normal personal WhatsApp calls.

## 2. OBS Or RTMP Stream Key Support

### Instagram (Live Producer)
- **Confirmed:** "Instagram Live Producer will allow you to go live on Instagram using streaming software (OBS, Streamlabs, etc.) through a stream key... This feature is only available on instagram.com." The blog also says: "**Live Producer is limited access at this time.**" Recommended settings: vertical 9:16 picture, 720p at 30 frames per second (60 frames per second also supported). (About Instagram, Live Producer post)
- **Confirmed (how):** On instagram.com, click **Create**, then **Live video**, add a title, and pick **Practice** (test with nobody watching) or **Public**. You get a URL and stream key. "The stream key is not static, and will refresh each time you use Live Producer." In your software, choose **Custom RTMP**, start streaming, then click **Go live** in Live Producer. End the live in Live Producer *first*, or viewers keep seeing the last frame. (Instagram Help Center, "Start a live video on Instagram")
- **Confirmed (who can go live at all):** "Only Instagram users who have a **public account with 1,000 followers or more** will be able to start a Live video." New accounts and accounts with removed posts may be blocked. Instagram added this rule on August 1, 2025. (Instagram Help Center; TechCrunch, August 1, 2025)
- **Confirmed by multiple guides (Report-level for the exact rule):** Live Producer needs a **professional (Creator or Business) account**. Personal accounts don't see it. (PRISM Live Studio guide; Epidemic Sound; Livepush)
- **Countries:** **Not published.** Instagram has never listed countries for Live Producer. Guides say access "can vary by account, region, and rollout." The only reliable test is to look for the option under Create > Live video on instagram.com. (Livepush; Easy4Live — reports)
- **Report:** Mobile streaming apps like Larix Broadcaster can send RTMPS to the Instagram key (server URL + "/" + key). This shows a phone app can be the streamer, as long as the key is copied from instagram.com.

### Messenger (Facebook Live)
- **Confirmed:** Messenger has **no stream key** and no way to broadcast into Messenger chats. Facebook Live is a *Facebook* feature, not a Messenger feature.
- **Confirmed (Facebook Live keys):** Go to facebook.com/live/create, choose **Streaming software**, and copy the Server URL and Stream key. A **persistent stream key** (reusable) is optional. Turn on RTMPS. Requirements: account **at least 60 days old**; Page or professional mode profile with **at least 100 followers**. (Facebook Help Center, "Go live on Facebook using streaming software")
- **Confirmed (Facebook Live Video API):** An app can create a live video with `POST /me/live_videos?status=LIVE_NOW` using the `publish_video` permission and Facebook Login. It gets back a `secure_stream_url` (an RTMPS address). Stream URLs must be used within 24 hours and can stream for up to 8 hours. RTMPS is required. (Meta, Live Video API Get Started, updated July 2, 2026; Overview)
- **Practical tie-in to Messenger (Guess):** Go live on Facebook, then paste the live video link into a Messenger chat. Viewers watch on Facebook, not inside the Messenger call.

### WhatsApp
- **Confirmed (by absence):** WhatsApp has **no live broadcast feature and no stream key**. Its only live-video tools are calls (personal) and the Business Calling API (business). No official stream-key or RTMP feature turned up anywhere in WhatsApp's help or developer docs.

## 3. Can Custom Text, Overlays, Or Effects Reach Viewers?

**Burned-in (part of the video itself) always reaches viewers**, because the app just shows the pixels it receives. **Native overlays** are limited to Meta's own built-in effects.

| | Burned-in (your own pixels and audio) | Native overlays (drawn by Meta's app) |
|---|---|---|
| **Instagram Live** | **Yes.** Everything in your Live Producer stream (effects, terminal panel, text, voice effects) is what viewers see. Use a vertical 9:16 picture, or Instagram crops the sides | Live title, comments, pinned comments, and Meta's own effects when going live from the phone app. No custom native effects since Meta Spark closed January 14, 2025 |
| **Instagram calls** | Only through screen share (phone) or a virtual camera on a computer (instagram.com calls let you pick a camera in Settings) | Meta's own call effects only |
| **Messenger calls** | Only through screen share (phone) or a virtual camera on a computer at facebook.com/messages | Meta's own effects only |
| **Facebook Live** (Messenger's sibling) | **Yes**, through RTMPS / the Live Video API | Polls (the API can create live polls), comments |
| **WhatsApp calls** | Only through screen share (phone) or a virtual camera on a computer | WhatsApp's own **10 filters, 10 backgrounds**, Touch Up, and Low Light (since October 2024). No way to add your own |

- **Confirmed:** Meta Spark closed January 14, 2025, and third-party effects were removed from Facebook, Instagram, and Messenger. Meta's own effects stayed. (Meta Spark announcement; The Verge; Reuters)
- **Confirmed:** WhatsApp added filters and backgrounds for video calls on October 1, 2024. (WhatsApp Blog)
- **Confirmed:** Instagram's publishing API notes "Filters are not supported." There's no API to add effects. (Content Publishing)
- **Voice effects (Guess, based on how Android works):** On stock Android, OFFGRID can't change the microphone sound that WhatsApp, Messenger, or Instagram records during a call. Voice effects only reach call viewers if the audio goes through a route OFFGRID controls: the RTMPS stream (Instagram Live / Facebook Live), or a computer where OFFGRID's audio becomes a virtual microphone. Whether screen share on Android also sends the phone's app sounds in these apps is **unverified**. Test it before relying on it.

## 4. Registration Requirements And Restrictions

### All Three (Meta Developer Basics)
- **Confirmed:** To register as a Meta developer, you need a Facebook account. You agree to the Platform Terms and Developer Policies, and verify a phone number and email address. (Meta, Register as a Meta Developer)
- **Confirmed:** Business Verification is needed for apps that request **Advanced Access**. "If your app will only be used by app users who have a role on the app itself you do not need to complete verification." (Meta, Business Verification)

### Instagram
- **Confirmed:** "If your app only serves your Instagram professional account or an account you manage, **Standard Access is all your app needs**." Advanced Access needs App Review and Business Verification. (Instagram Platform Overview)
- **Confirmed:** App Review table: "My app is only for a business I own or manage... Standard Access... App Review: **Not required**." (Meta, App Review for Instagram API, June 30, 2026)
- **Confirmed (permissions):** With Instagram Login, permissions are named like `instagram_business_basic` and `instagram_business_manage_comments`. With Facebook Login: `instagram_basic`, `instagram_manage_comments`, `pages_read_engagement`, and others. `live_media` lists `instagram_basic` + `pages_read_engagement`. (Webhooks; Live Media reference)
- **Confirmed (rate limits):** Calls within 24 hours = **4,800 x number of impressions** (how many times the account's content was seen in the last 24 hours). Conversations API: 2 calls per second. Publishing: **100 API-published posts per 24 hours**. Access tokens (login passes) last 1 hour (short-lived) or 60 days (long-lived, renewable). (Instagram Platform Overview; Content Publishing)
- **Confirmed:** Instagram's Terms of Use ban accessing or collecting information "in unauthorized ways," including automated means without permission. Unofficial Instagram clients risk the account. (Instagram Terms of Use)

### Messenger
- **Confirmed:** You need a registered Meta developer account, an app with the Messenger use case, and a Facebook Page linked to the app. Business Verification is needed only "if your app is used by people without a Role." App Review is needed only for Advanced Access: "Not required if you only send and receive messages for your own Facebook Page." Permission: `pages_messaging`. (Messenger Platform Overview)
- **Confirmed (rate limits):** **200 x number of engaged users** per 24 hours. Send API: 300 per second per Page for text, **10 per second for audio or video messages**. Conversations API: 2 per second. (Messenger Platform rate limits, March 23, 2026)
- **Confirmed:** Calling first requires checking that the Page has `messenger_api_calling` turned on, and the Page must be in a listed country. (Messenger Calling)
- **Confirmed (Facebook Live API):** Requires `publish_video` and Facebook Login. Account 60+ days old, 100+ followers.

### WhatsApp
- **Confirmed:** You create a Meta app with the "Connect with customers through WhatsApp" use case and pick or create a **business portfolio**. (Cloud API Get Started)
- **Confirmed (limits):** New business portfolios start at a messaging limit of **250** unique users per 24 hours. You reach **2,000** by verifying your business (one of the "scaling paths"). Calling needs at least 2,000. Throughput: **80 messages per second** by default, up to 1,000. (Messaging Limits; Throughput)
- **Confirmed (policy):** You "may only contact people on WhatsApp if... they have given you their mobile phone number" and "you have received opt-in permission... to receive subsequent messages or calls from you." (WhatsApp Business Messaging Policy)
- **Confirmed (unofficial clients):** WhatsApp's Terms forbid you to "reverse engineer, alter, modify, create derivative works from, decompile, or extract code from our Services" and to "create accounts... through unauthorized or automated means." (WhatsApp Terms of Service)
- **Confirmed (official help pages, read through search summaries because the pages need JavaScript):** A "Temporarily banned" message usually means you're using an **unofficial app such as GB WhatsApp or WhatsApp Plus**. You must switch to the official app or risk a **permanent ban**. (WhatsApp Help Center, "About temporarily banned accounts" and "About unofficial apps"; Indian Express)
- **Practical meaning:** OFFGRID must **never** hook into, modify, or re-package the WhatsApp app.

## 5. Can A Private Android App Outside The Play Store Use These APIs?

- **Facebook Login on Android (Confirmed):** You enter a package name (your app's unique ID) and key hashes (a fingerprint of the key your app is signed with) in the App Dashboard. Meta says it uses the package name "to let people download your app from Google Play if they don't have it installed," but nothing in the setup requires the app to *be* on Google Play. (Facebook Login for Android Quickstart) **Guess:** A sideloaded app signed with your own key works fine for login.
- **Instagram Login (Confirmed):** "Web or mobile Web is the only platform that currently supports Instagram API with Instagram Login." So OFFGRID would open a web login page (for example, a browser tab inside the app), not an Android SDK. (App Review for Instagram API)
- **App Review (Confirmed):** Not needed when the app only serves accounts you own or manage (Standard Access). If you ever wanted Advanced Access, reviewers need "step-by-step instructions... for each platform," test logins, a privacy policy link, an app icon, and screen recordings. **Report:** Developers say an APK (the Android app install file) with install instructions is accepted for Android reviews.
- **Instagram:** Usable for **your own professional account**. But there's no live-sending API, so for live video the API only adds extras (like reading live comments). The real live path is **pasting a Live Producer key into OFFGRID**.
- **Messenger / Facebook:** Facebook Live Video API: **yes**. OFFGRID could log in with Facebook and create the live video itself, then stream RTMPS. Messenger Calling API: technically possible from any app that can run WebRTC, but only for a **Page** in a **listed country** (not the United States). The person on the other end sees a call from a *business*, not from you.
- **WhatsApp:** The Cloud API (messages and calls) is **business messaging only**, needs a separate business phone number, verification to reach the 2,000 limit, and opt-in from each contact. Business-initiated calls are blocked for United States business numbers. Using it to swap your personal WhatsApp calls doesn't fit the platform's purpose. Also, a security point (Guess): Cloud API access tokens shouldn't be stored inside a phone app; Meta's design expects a server.

## 6. Known Methods For Burned-In Effects To Reach Viewers

| Method | Instagram | Messenger | WhatsApp | Notes |
|---|---|---|---|---|
| **Stream key from OFFGRID (RTMPS straight from the phone)** | **Yes** (Live Producer key from instagram.com) | Facebook Live only (key or Live Video API) | No | Best quality: effects and voice effects fully burned in. OFFGRID needs an RTMPS encoder (a part that compresses and sends the live video) |
| **Stream key from OBS on a computer** | **Yes** (official) | Facebook Live only | No | OFFGRID sends its picture to the computer, and OBS streams it |
| **Virtual camera in a browser call (computer)** | instagram.com calls (camera can be picked in Settings) — **Guess** that OBS Virtual Camera appears | facebook.com/messages — **Reports:** OBS Virtual Camera works in the browser, sometimes freezes on a still image after updates | WhatsApp Web calling (launched July 28, 2026, rolling out) — **Guess**, not yet tested in reports. WhatsApp Desktop (Windows) — **Reports:** OBS's built-in Virtual Camera is blocked, but DroidCam's OBS "Virtual Output" plugin works | Needs a computer. Voice effects need a virtual microphone on the computer |
| **Phone camera itself** | Only the real camera, plus Meta's built-in effects | Same | Same | **Confirmed:** Android's virtual camera feature (Android 15 `createVirtualCamera`) needs the `CREATE_VIRTUAL_DEVICE` permission, which is only given to system companion-streaming apps. Regular apps like OFFGRID can't use it |
| **Screen share during a call (phone)** | **Yes** (Instagram video calls have Share screen) | **Yes** (Messenger video/audio calls on Android) | **Yes** (WhatsApp video calls on Android) | OFFGRID shows its picture full screen, and the call app shares the screen. Viewers see it as a screen share, and notifications may show. Audio sharing is unverified |

Sources for the table: the Instagram, Messenger, and WhatsApp Help Center screen share pages; the WhatsApp Blog and Meta Newsroom (web calling); OBS Forums threads (reports); the DroidCam virtual output project page (report); and the Android Open Source Project page on companion app streaming.

- **Confirmed:** The Messenger desktop apps (Windows and Mac) shut down December 15, 2025. messenger.com closed in April 2026 and redirects to **facebook.com/messages**, where browser calls still work. (TechCrunch)
- **Confirmed:** WhatsApp Web now supports audio and video calls (one-on-one and group) with screen sharing, "directly from WhatsApp Web. No app download required." (WhatsApp Blog; Meta Newsroom, July 2026; rolling out gradually)

## 7. Bottom Line For Each Platform

| Platform | Rating | What OFFGRID can realistically do | Route to use |
|---|---|---|---|
| **Instagram** | **Works** (Live) / **Partial** (calls) | Fully burned-in effects, terminal panel, overlays, and voice effects in **Instagram Live**, seen by every viewer on their phone. Calls only through screen share or a computer | **Live Producer key** (copy it on instagram.com) pasted into OFFGRID's RTMPS output. Backup: OBS on a computer. Needs a professional, public account with 1,000+ followers and Live Producer access. Optional: read live comments through `live_media` into the terminal panel |
| **Messenger** | **Partial** | Burned-in video in calls through **screen share** on the phone, or a **virtual camera** in a browser at facebook.com/messages. No stream key, and no API for personal calls. The Calling API is for business Pages in listed countries (not the United States) | Phone: Share screen during the call. Computer: OBS Virtual Camera in Chrome, Edge, or Firefox at facebook.com/messages. For broadcasts: **Facebook Live** through RTMPS or the Live Video API, then share the link in Messenger |
| **WhatsApp** | **Partial** | Burned-in video in calls through **screen share** on the phone, or a computer virtual camera (WhatsApp Web/Desktop). No live streaming at all. Cloud API is business-only (Calling video still "planned or in development") | Phone: Share screen during the WhatsApp video call. Computer: WhatsApp Web calling with a virtual camera (test it), or WhatsApp Desktop with DroidCam virtual output (reports). Never use modified WhatsApp apps |

---

## Confirmed Facts, Reports, And Guesses

### Confirmed (Official Meta Docs, Help Centers, Or Major News Outlets)
- Instagram's API serves professional accounts only. Basic Display ended December 4, 2024.
- Instagram `live_media` is read only (live videos on air right now, plus comments). There's no API to send a live stream.
- Live Producer works on instagram.com with streaming software through Custom RTMP. The key changes each session. It's "limited access."
- Going live on Instagram needs a public account with 1,000+ followers (since August 1, 2025).
- Standard Access (no App Review, no Business Verification) is enough for an app that only serves your own accounts.
- Instagram rate limits: 4,800 x impressions per 24 hours; 100 API posts per 24 hours.
- Messenger Platform is for business Pages. People must message first. Rate limit: 200 x engaged users per 24 hours.
- The Messenger Calling API sends audio and video over WebRTC, for Pages in listed countries only (United States not listed).
- Facebook Live supports RTMPS stream keys (with persistent keys optional) and the Live Video API (`publish_video`). Requirements: 60-day-old account, 100 followers.
- Messenger desktop apps ended December 15, 2025. messenger.com closed in April 2026, replaced by facebook.com/messages.
- WhatsApp Cloud API is for businesses. Calling needs a 2,000 messaging limit. Business-initiated calls are blocked for United States, Canadian, Egyptian, Vietnamese, and Nigerian business numbers. Video and screen share are marked "planned or in development."
- WhatsApp Terms ban modifying or reverse engineering its apps. WhatsApp temporarily bans users of unofficial apps like GB WhatsApp and WhatsApp Plus, and can ban them permanently.
- WhatsApp Web added audio and video calls in July 2026.
- Screen sharing exists in Instagram, Messenger, and WhatsApp video calls on Android.
- Meta Spark closed January 14, 2025. Only Meta's own effects remain.
- Android's virtual camera feature is limited to system companion-streaming apps.

### Reports (Forums, Third-Party Guides, Talks)
- Live Producer needs a Creator or Business account, and country availability varies (third-party guides; no official country list).
- Mobile apps such as Larix Broadcaster can stream to an Instagram Live Producer key.
- OBS Virtual Camera works for Messenger calls in a browser but can freeze to a still image (OBS Forums).
- WhatsApp Desktop on Windows blocks OBS's built-in Virtual Camera, but DroidCam's OBS virtual output works (OBS Forums, DroidCam project).
- WhatsApp Business Calling video launched in 2026 with H.264 over WebRTC only (Meta *Conversations 2026* talk on YouTube; docs still say "planned or in development").
- Meta reviewers accept APK install instructions for Android app reviews (developer reports).

### Guesses (Reasoned, Not Verified)
- A sideloaded OFFGRID signed with your own key can use Facebook Login and the Live Video API without being on the Play Store.
- OFFGRID can stream RTMPS directly from the phone to Instagram Live using a key copied from instagram.com (possibly even from a phone browser in "desktop site" mode — untested).
- OBS Virtual Camera will show up in WhatsApp Web and instagram.com browser calls the same way it does for other browser call sites.
- On stock Android, OFFGRID can't change the microphone audio other apps record. Whether call apps send the phone's sounds during screen share is unknown.
- Polling `live_media` with comments works under Standard Access for your own account, so live comments can appear in the terminal panel.

## Sources

**Official Meta developer docs**
- Instagram Platform overview: https://developers.facebook.com/docs/instagram-platform/overview
- IG User Live Media reference: https://developers.facebook.com/docs/instagram-platform/instagram-graph-api/reference/ig-user/live_media/
- Instagram webhooks (live_comments): https://developers.facebook.com/docs/graph-api/webhooks/getting-started/webhooks-for-instagram/
- Instagram webhooks setup and permissions: https://developers.facebook.com/documentation/instagram-platform/webhooks
- Instagram Content Publishing: https://developers.facebook.com/documentation/instagram-platform/content-publishing
- App Review for Instagram API: https://developers.facebook.com/documentation/instagram-platform/app-review
- Update on Instagram Basic Display API: https://developers.facebook.com/blog/post/2024/09/04/update-on-instagram-basic-display-api/
- Messenger Platform overview: https://developers.facebook.com/documentation/business-messaging/messenger-platform/overview
- Messenger Platform rate limits: https://developers.facebook.com/documentation/business-messaging/messenger-platform/overview/rate-limiting
- Messenger Platform app review: https://developers.facebook.com/documentation/business-messaging/messenger-platform/app-review
- Messenger Calling API: https://developers.facebook.com/documentation/business-messaging/messenger-platform/calling
- Messenger Video Calling: https://developers.facebook.com/documentation/business-messaging/messenger-platform/calling/videocalling
- Messenger call permissions: https://developers.facebook.com/documentation/business-messaging/messenger-platform/calling/biz2consumer/check-permissions
- Live Video API overview: https://developers.facebook.com/docs/live-video-api/overview/
- Live Video API get started: https://developers.facebook.com/documentation/live-video-api/getting-started
- WhatsApp Cloud API get started: https://developers.facebook.com/documentation/business-messaging/whatsapp/get-started
- WhatsApp Cloud API Calling: https://developers.facebook.com/documentation/business-messaging/whatsapp/calling
- WhatsApp business-initiated calls: https://developers.facebook.com/documentation/business-messaging/whatsapp/calling/business-initiated-calls
- WhatsApp Calling API reference: https://developers.facebook.com/documentation/business-messaging/whatsapp/reference/whatsapp-business-phone-number/calling-api
- WhatsApp Settings API (video calling setting): https://developers.facebook.com/documentation/business-messaging/whatsapp/reference/whatsapp-business-phone-number/settings-api
- WhatsApp messaging limits: https://developers.facebook.com/documentation/business-messaging/whatsapp/messaging-limits/
- WhatsApp throughput: https://developers.facebook.com/documentation/business-messaging/whatsapp/throughput/
- Register as a Meta developer: https://developers.facebook.com/docs/development/register
- Business Verification: https://developers.facebook.com/docs/development/release/business-verification
- Facebook Login for Android quickstart: https://developers.facebook.com/docs/facebook-login/android/

**Official help centers, policies, and company blogs**
- Instagram Live Producer (About Instagram): https://about.instagram.com/blog/tips-and-tricks/instagram-live-producer
- Start a live video on Instagram (Help Center): https://www.facebook.com/help/instagram/292478487812558
- Start and manage calls on Instagram (computer): https://help.instagram.com/136585180482293/
- Share your screen in a video call on Instagram: https://help.instagram.com/709752593414437/
- Instagram Terms of Use: https://help.instagram.com/581066165581870/
- Go live on Facebook using streaming software: https://www.facebook.com/help/587160588142067
- Share your screen in a Messenger call: https://www.facebook.com/help/687234652040246/
- WhatsApp: How to share your screen: https://faq.whatsapp.com/1339237313658883
- WhatsApp: About temporarily banned accounts: https://faq.whatsapp.com/1848531392146538
- WhatsApp: About unofficial apps: https://faq.whatsapp.com/1217634902127718
- WhatsApp Terms of Service: https://www.whatsapp.com/legal/terms-of-service
- WhatsApp Business Messaging Policy: https://business.whatsapp.com/policy
- WhatsApp Blog, web calling: https://blog.whatsapp.com/introducing-web-calling-on-whatsapp-plus-more-new-updates
- Meta Newsroom, WhatsApp web calling (July 2026): https://about.fb.com/news/2026/07/whatsapp-web-calling-new-features/
- WhatsApp Blog, filters and backgrounds: https://blog.whatsapp.com/introducing-filters-and-backgrounds-for-video-calls
- Meta Spark announcement: https://spark.meta.com/blog/meta-spark-announcement
- Android Open Source Project, companion app streaming (virtual devices): https://source.android.com/docs/core/permissions/app-streaming

**Reputable articles**
- TechCrunch, Instagram 1,000-follower live rule (August 1, 2025): https://techcrunch.com/2025/08/01/instagram-now-requires-users-to-have-at-least-1000-followers-to-go-live/
- TechCrunch, Messenger desktop app shut down (December 15, 2025): https://techcrunch.com/2025/12/15/facebook-messengers-desktop-app-is-no-more/
- TechCrunch, messenger.com shutting down (February 19, 2026): https://techcrunch.com/2026/02/19/meta-is-shutting-down-messengers-standalone-website/
- TechCrunch, Instagram locks out third-party consumer apps (December 2024): https://techcrunch.com/2024/12/06/instagram-locks-out-developers-of-third-party-consumer-apps/
- The Verge, Meta ends custom face filters: https://www.theverge.com/2024/8/27/24229643/meta-spark-ar-effects-face-filters-shutdown-tiktok-snapchat
- Indian Express, WhatsApp bans unofficial apps: https://indianexpress.com/article/technology/social/whatsapp-will-bans-users-on-unofficial-apps-lists-out-faq-on-how-to-migrate-to-official-app-5616980/

**Reports (forums, third-party guides, talks)**
- Meta *Conversations 2026* WhatsApp Calling API talk (YouTube): https://www.youtube.com/watch?v=evxT7d23wx4
- Livepush, Instagram Live from PC (limited access, no official eligibility rule): https://livepush.io/blog/instagram-live-from-pc/
- PRISM Live Studio guide, Instagram RTMP: https://guide.prismlive.com/desktop/guides/streaming/rtmp-streaming/connecting-instagram-via-rtmp-presets.md
- Epidemic Sound, Live Producer eligibility: https://www.epidemicsound.com/blog/what-is-instagram-live-producer/
- Easy4Live, Instagram Live from PC: https://easy4live.com/go-live-on-instagram-from-pc-ultimate-professional-guide/
- Larix Broadcaster docs (RTMPS from mobile): https://softvelum.com/larix/docs/
- OBS Forums, OBS Virtual Camera works with Messenger in browser: https://obsproject.com/forum/threads/obs-virtual-camera-works-with-messenger-in-internet-browser-but-not-in-desktop-app.150012/
- OBS Forums, Messenger virtual camera shows only a still image: https://obsproject.com/forum/threads/facebook-messenger-and-the-virtual-camera-works-but-only-a-still-image.158541/
- OBS Forums, WhatsApp Desktop and OBS Virtual Camera: https://obsproject.com/forum/threads/new-whatsapp-desktop-application-does-not-work-with-obs-virtual-camera.140414/page-2
- DroidCam OBS virtual output (works with WhatsApp Desktop): https://github.com/dev47apps/droidcam-obs-virtual-output
