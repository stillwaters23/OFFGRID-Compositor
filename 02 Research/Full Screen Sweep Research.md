# Full Screen Sweep Research

*Research date: Tuesday, October 6, 2026 (CT). Prepared for OFFGRID-Compositor scoping. Web research only: nothing was installed, downloaded as software, or logged into.*

**The question:** Royalty's friend reportedly played a full-screen red "sweep" or "kick" animation on Bigo Live, MeetMe, and Tagged, and *every* viewer saw it across their *whole* screen, not just inside the friend's video box. How could that happen?

## Bottom Line

- **There are only two normal ways an effect can show up full-screen for every viewer.** (1) The **platform's own animation** (a big paid gift, a VIP or fan-club entrance effect, a battle banner). The platform sends a short message to every viewer's app, and each app plays the animation itself. (2) The effect is **burned into the broadcaster's video** (drawn onto the picture before it is sent). In a solo stream (not a battle), the broadcaster's video usually fills the whole phone screen, so a full-frame effect *looks* full-screen.
- **The quick test is the chat.** If the red sweep went **over** the chat text and buttons, it was almost certainly a platform animation (or an edited clip). If chat text stayed **on top** of the sweep, it was burned into the video. Chat, buttons, and gift animations are drawn by each viewer's own app on top of the video. A video effect cannot cover them.
- **The same custom red sweep on all three apps points to the burned-in route.** Each platform has its own, different gift artwork. One identical red animation on Bigo, MeetMe, and Tagged fits best with the friend's video being processed before upload (for example, by OBS (Open Broadcaster Software) feeding a virtual camera on MeetMe/Tagged web, or by Bigo's official OBS or PC tools).
- **No confirmed "red carpet" gift was found on Bigo, MeetMe, or Tagged.** TikTok has a "Star of Red Carpet" gift (1,999 coins, about $21), but that is a different platform. Bigo has real red carpets only at its yearly in-person Awards Gala.
- **Mods and hacks are very unlikely.** A modified app only changes what *your* phone shows. Every other viewer's app draws its own screen. Mods also break every platform's terms of service (ToS), risk a ban, and often carry malware. Bigo says outright that "MOD APKs" are scams or security risks.
- **What OFFGRID can do legitimately:** burn a short, full-frame 9:16 (tall phone shape) sweep into Royalty's own outgoing video while in **solo** mode, keep the important art in the middle of the frame, avoid fast flashing, and accept that chat will sit on top. To get the true "over everything" look, send the **platform's own gift** instead.

## How To Read This Document

Each finding is labeled:

- **Confirmed**: stated on an official page (help center, official blog, ToS, app store listing, company press release, or official developer docs).
- **Reported**: said by third-party guide sites, forums, Reddit, or video sites. Useful, but not official. **Warning:** many Bigo "guide" sites (BitTopup, Buffget, Discount Diamond Store, LootBar) sell diamond top-ups. Their numbers often disagree with each other, so treat their prices and timings as rough.
- **Guess**: my own reasoning where no source was found.

**Short glossary**

- **Host / broadcaster / streamer**: the person who is live.
- **Burned in**: drawn directly onto the video picture before it leaves the broadcaster's device, so it is part of the video everyone receives.
- **Overlay (native or app-drawn)**: graphics such as chat, buttons, and gift animations that each viewer's app draws on top of the video on that viewer's own phone.
- **PK ("Player Kill" or "Player Knockout") / Battle / VS**: a timed gift contest between two streamers shown side by side (split screen).
- **OBS (Open Broadcaster Software)**: free PC software for building a video scene with layers and effects.
- **Virtual camera**: a fake webcam made by software (such as OBS) so a website sees the edited video as if it were a normal camera.
- **APK (Android Package)**: an Android app install file. A **mod APK** is an app someone else has changed and re-shared.
- **Userscript**: a small script that changes how a website looks in *your own* browser only.
- **Barrage / bullet message (弾幕, "danmaku")**: a chat message that scrolls across the screen instead of sitting in the chat list.
- **ToS (Terms of Service)**: the rules you agree to when you use an app.
- **9:16**: tall phone-screen shape (for example 1080 pixels wide by 1920 tall).

---

## 1. Official Full-Screen Effects That Every Viewer Sees

### Bigo Live

**Gift animations**

- **Confirmed:** When a viewer sends a gift, "You will see an animation of the gift on the screen." Bigo says "More expensive gifts have more elaborate animations." The official tutorial mentions simple gifts like roses and "big ones (like cars or castles)." [B1][B2]
- **Confirmed:** Official events (Bigo Gala, holiday events) come with "exclusive gift animations" and limited-time gifts. [B3]
- **Reported:** Premium gifts (roughly 1,000+ diamonds) trigger "full-screen takeovers" of about **15 to 25 seconds** that "the whole room sees." [R1][R2] Gifts named most often:
  - **Gala Dragon**, about **9,999 diamonds**, 25-second full-screen animation (several sellers agree on this price). [R1][R2][R3]
  - **Golden Phoenix**, about **6,666 diamonds**. [R1][R3]
  - **Private Jet**, **Yacht**, **Mansion**, **Castle**: full-screen tier, but the reported prices **conflict wildly** (for example, Private Jet is listed as 2,500 diamonds on one seller page and 8,000–9,000 on another). [R2][R4] **Check the in-app gift panel, which shows the real price under each gift.** [B1]
  - **Love Carriage**, reported at **29,999 diamonds**, about 75 seconds, ending in a "rose petal shower" (Valentine's 2026 event gift). [R3]
- **Reported:** Big gifts (one seller says 500+ diamonds) also trigger a short **platform-wide banner** in other rooms. This is a small notice, not the full animation. [R3]
- **Rough price guide (reported):** sellers price diamonds at about $0.021–$0.0235 each, so a 9,999-diamond gift costs roughly **$210–$235**. [R3][R4]
- **No red sweep or red carpet gift was confirmed.** Nothing found describes a Bigo gift as a red carpet or red sweep. A rose-petal ending (Love Carriage) or a car or jet driving across the screen are the closest "sweep across" visuals reported. [R3][R5]

**Entrance effects (VIP, Noble, SVIP, Fan Club, "mounts")**

- **Confirmed:** Super Fan Club members get "a spectacular, full-screen welcome animation when they enter the room." Fan Club levels also unlock "special entry effects." Joining a Fan Club "usually costs just 10 diamonds" (the *Super* tier costs more; the price is not listed). [B3]
- **Confirmed:** Bigo's paid VIP plan includes "exclusive in-app special effects" (details are on the in-app VIP page). [B4] The Task Center can also give "temporary VIP status" and "special entrance effects." [B3]
- **Reported:** VIP members get an **entrance sticker** shown on screen when they enter, colored comments, and VIP-only gifts. [R6] Higher "Noble" tiers are said to get longer entrance animations (5 seconds at Baron up to 25 seconds with "full-screen graphics" at Duke), costing about 1,000 to 60,000 diamonds per month. [R7] Entrance "mounts" (vehicle-style arrivals) are described as status symbols, "the digital equivalent of rolling up to a red-carpet event in a customized limousine." That is a comparison, not a real red-carpet effect. [R5]

**PK (battle) animations and "kick"**

- **Confirmed:** PK is a split-screen gift contest. "The system automatically declares a winner" when the timer ends. [B5] In Multi-Room PK, "a clear visual banner will appear on the screen" for point boosts such as "Double PK Points." [B6] The Play Store listing advertises "exciting penalties for the losing team." [B7]
- **Confirmed:** Hosts can give trusted admins the power to "Kick out and Block" people who misbehave. [B8] **No source describes a full-screen "kick" animation.** A kick removes someone; nothing found says it plays a show for viewers.
- **Reported:** Some guides say losing a PK applies temporary penalty filters (face or color distortion) to the loser's video for 1–2 minutes. Another guide from the same seller says penalties are just agreed-on performances. The sellers disagree with each other. [R8][R9]
- **Guess:** A friend calling something a "kick" may be describing the end-of-PK win/lose moment, since "PK" is often read as "Player Kill." But in a PK the screen is split, so a burned-in effect would only fill half the screen.

**Barrage / bullet messages**

- **Reported:** Bigo has (or had) paid **bullet messages (弾幕)** that normally cost **1 diamond each**; VIPs get some free every day. [R6] A 2017 XDA forum post asked how to send a "Long Bullet Message" on Bigo (no answer given), which suggests the feature has existed for years. [R10]
- **Guess:** These are text messages drawn by each viewer's app, not full-screen red animations.

### MeetMe and Tagged (both run by The Meet Group)

- **Confirmed:** Both have live streaming with virtual gifts. Tagged Live works in the **iPhone app, Android app, and on the Tagged website**. [M1][M2] MeetMe Live lets you "give and receive gifts, battle, and even go on dates." [M3]
- **Confirmed:** **Battles** pair two streamers for "just a few minutes"; the winner earns more diamonds from gifts. [M4]
- **Confirmed:** Levels unlock "special animations," masks, gestures, and backgrounds for streamers, and "exclusive gifts" for viewers. [M5][M6]
- **Confirmed:** **Spotlight items** put a streamer in the "In the Spotlight" list (placement in the app's browse list, not a screen effect). [M7] **Boost items** add XP (experience points) or SP (streamer points), not visuals. [M8] MeetMe Premium's "Boost your profile to the top for 2 hours" is for the dating side, not a live effect. [M3] Trending placement "is not available by request." [M9]
- **Confirmed:** Streamers and their **bouncers** (helpers) can **kick** viewers. A kicked user "may not rejoin the same stream." [M10] **No kick animation is documented.**
- **Reported:** Popular MeetMe gifts include thumbs up, roses, beer mugs, Love Bear, confetti bombs, dice, Mystery Wheels, and "big ticket animations like dragons, castles, yachts." Battles last about 1–3 minutes. **No prices are published** on the help center, and **no red carpet or red sweep gift** was found. [R11]
- **Confirmed (safety):** The Meet Group's Live ToS includes a **photosensitivity (flashing light) warning.** [M11]

### Is Anything Like A Red Carpet Sweep?

- **Confirmed on another platform only:** TikTok's **"Star of Red Carpet"** gift, 1,999 coins (about $20.99), with an animation that "fills a significant portion of the screen." [R12]
- **Not found** on Bigo, MeetMe, or Tagged. **Guess:** the friend's red sweep was either a custom burned-in effect, a big gift whose look was described loosely, or a clip from another app such as TikTok.

---

## 2. How Chat And Effects Are Drawn On Each Viewer's Screen

### Chat Is Drawn On Each Viewer's Phone, On Top Of The Video

- **Confirmed (how the industry builds this):** Tencent's live-streaming developer docs show the standard design: when someone sends a gift, the server sends a small "gift event" message to **every** viewer in the room, and **each viewer's app plays the animation itself** (for example, an SVGA animation file, a common vector animation format) on a layer above the video. Gift notices are added to the chat list the same way. [T1]
- **Confirmed:** The Meet Group runs its live video on Agora's video network, a separate service from its chat and gifting. [M12]
- **Reported:** A MeetMe streamer using OBS said their stream worked, but "whenever I go to VS someone I end up with a white screen." That shows the battle layout is built by the app around the incoming video, not baked into it. [R13]
- **Guess (high confidence):** On all three platforms, chat, buttons, gift animations, and entrance effects are native overlays drawn on each viewer's device. The video sits underneath. **A video effect can never cover the chat or buttons.** Bigo, MeetMe, and Tagged do not publish their internal designs, but every platform that shows gifts to the whole room works this way.

### Does The Broadcaster's Video Fill The Whole Screen?

- **Confirmed:** In Bigo PK the screen is "split in two." [B5] Multi-guest rooms show grids of 4, 6, 9, or 12 boxes. [B2][B3] MeetMe's Guest Box holds up to 4 viewers. [R11]
- **Reported:** In a normal solo Bigo room, "the host's video fills the main screen, while viewer chat appears as an overlay in the lower-left area." [R14]
- **Guess:** MeetMe and Tagged phone apps show solo streams the same way (tall video filling the screen, chat drawn over the bottom). **On the website, the video sits inside a player box**, so a web viewer would see the sweep inside that box, not across the whole browser window.

### Why A Burned-In Effect Can Look Full-Screen

Picture three layers on a viewer's phone in solo mode:

1. **Bottom layer:** the broadcaster's video, stretched to fill the whole screen.
2. **Middle:** nothing; the chat area is see-through.
3. **Top layer:** chat text, hearts, buttons, and any gift or entrance animations, drawn by the viewer's own app.

If the broadcaster burns a red sweep across the **whole video frame**, then for a moment the entire bottom layer turns red. Because the video fills the screen and the chat is see-through, viewers see red **across their whole screen** with chat floating on top. It looks like a full-screen effect, but it is really just the video.

**Limits of this trick (guesses):**

- **It goes under chat, not over it.** Chat text and buttons stay visible on top.
- **Phones may crop the picture.** Apps usually zoom the video to fill the screen, so a wide (16:9) picture from a PC may lose its left and right edges on phones. A tall 9:16 picture avoids that.
- **Battles and guest boxes shrink it.** In PK, Battle, or VS mode the video is only half the screen (or a small box), so the sweep only fills that part.
- **Compression can make it ugly.** Big, fast, solid-red changes are hard for live video compression; at low internet speeds the sweep may look blocky or smeared.
- **There is a delay.** Viewers see it a second or more after it happens.

---

## 3. Unofficial Tools, Mods, And Browser Tricks

**What exists (high level only):**

- **Confirmed:** Bigo says there is "no legitimate BIGO LIVE hack, generator, MOD APK, or cheat." It says mod APKs claim things like "Unlimited Diamonds," "Free VIP membership," "Hidden streaming tools," and "Removed restrictions," and may "Capture the username and password you enter," "Read information stored on your device," or "Install additional software without clear consent." [B9]
- **Reported:** Forum posts ask for tricks such as bypassing Bigo's bullet-message length limit (2017, unanswered). [R10] Third-party "gift viewer" pages exist that show Kik Live and MeetMe gift artwork. [R15] No reputable source was found showing a tool that makes *other viewers* see a full-screen overlay.

**Why these generally can't change what other viewers see:**

- Each viewer's app builds its own screen from the video plus small event messages from the platform's servers. [T1] A modded app or userscript on the friend's phone or browser only changes the **friend's own** screen. To make other people's apps play an animation, you would have to send the platform's servers a message they accept (a real gift, which costs real money) or tamper with the servers. Tampering is hacking, not a feature.
- The one thing every viewer *does* receive unchanged is the **broadcaster's video**. That is why burning the effect into the video (the legitimate route, see Section 5) is the only realistic way for a broadcaster to put a custom effect in front of everyone.

**ToS, ban, and malware risks:**

- **Confirmed (Bigo):** The User Agreement forbids users to "reverse engineer, decompile, disassemble" the service or "modify, adapt, translate or create derivative works based upon the Services." [B10] Bigo can suspend or end VIP status for use that is "unauthorized, abusive, deceptive, fraudulent." [B4]
- **Confirmed (MeetMe/Tagged):** The Meet Group's Live ToS says: "You may not modify the software in any manner or form or to use modified versions of the software. You may not access Live by any means other than through an interface we provide." Banned accounts lose all credits and diamonds. [M11]
- **Confirmed:** Bigo warns that mod APKs can steal logins and install unwanted software. [B9]

---

## 4. Other Explanations

| Explanation | How it would work | Fits "everyone saw it full-screen"? | Notes |
| --- | --- | --- | --- |
| **Friend was the solo host with a burned-in full-frame effect** | Effect drawn onto the friend's video (OBS + virtual camera on MeetMe/Tagged web; Bigo's official OBS option or Bigo LIVE Streamer on PC). Solo layout makes the video fill phone screens. | **Yes on phones**, with chat on top. Web viewers see it inside the player box. | **Confirmed:** Bigo officially supports OBS streaming and lists "custom scenes, overlays" as its purpose. [B11] **Reported:** Bigo's OBS option may only be switched on for some broadcasters. [R16] **Reported:** people stream to MeetMe through OBS. [R13][R17] |
| **Platform's own gift or entrance animation** | Someone sent a big gift, or the friend (or a viewer) entered with a VIP or Super Fan Club full-screen entrance. | **Yes**, and it covers chat. | Confirmed to exist on Bigo [B1][B3]; reported big animations on MeetMe. [R11] Artwork differs per platform, so the "same red sweep on all three" is harder to explain this way. |
| **Screen sharing** | Friend streams their screen (Bigo "Game Live" or BIGO LIVE Connector) while playing a full-screen red video. | **Yes**, same as burned-in video (it *is* the video). | **Confirmed:** Bigo Connector "mirrors your PC screen to your BIGO LIVE broadcast." [B11] |
| **Pre-recorded or edited clip** | A screen recording was edited afterward (or a TikTok clip was mixed in) and shared as "this happened live." | Only *seems* to; no live viewers needed. | Worth asking for an unedited recording from a different viewer's phone. |
| **Platform event or system animation** | A big event (for example, the Bigo Gala) with special banners, or a PK result or boost banner. | **Yes** while it runs, for everyone in that room. | **Confirmed:** event gift animations and PK banners exist. [B3][B6] |
| **Mod, injector, or hack changing viewers' screens** | Would need to make other people's apps draw something. | **Very unlikely.** | See Section 3. ToS, ban, and malware risk. |

**Questions that would settle it quickly:**

1. Did the chat text stay visible **on top** of the red, or did the red cover the chat and buttons?
2. Was the friend live **solo**, or in a battle, PK, or guest box?
3. Did the chat show a line like "X sent [gift name]" at the same moment?
4. Is there a screen recording **from a viewer's phone**, not from the friend's own device?
5. Did MeetMe and Tagged *website* viewers see it across the whole browser window, or only inside the video box?

---

## 5. Ranked Explanations And What OFFGRID Can Do

### Most Likely Explanations (Best Guess, Ranked)

1. **Burned-in full-frame sweep in solo mode (most likely).** Fits "the same red animation on Bigo, MeetMe, and Tagged," because the same video-editing setup (for example, OBS) can feed all three. On phones it fills the whole screen behind see-through chat. *Tell-tale sign:* chat stays on top.
2. **The platform's own big gift or entrance animation (likely on any one app).** Real, full-screen, seen by everyone, and drawn over chat. Less likely to explain an *identical* red sweep on all three apps, since each app has different gift art. *Tell-tale sign:* the red covers chat and a gift or entrance notice appears.
3. **Edited or pre-recorded clip (possible).** Common with social-media "look what I did" clips. *Tell-tale sign:* no viewer can show their own recording.
4. **Screen sharing of a red video (less likely).** Works on Bigo through official tools, but is an awkward way to do it; it is really just a variant of number 1.
5. **A mod or hack that changed viewers' screens (very unlikely).** Viewers' apps draw their own screens; mods can't reach them. ToS, ban, and malware risk.

### What OFFGRID Can Legitimately Do To Get A Similar Look

**Option A: Burned-in full-frame sweep (the main OFFGRID feature)**

- Draw the sweep onto Royalty's **own outgoing video** before it is sent. On MeetMe and Tagged that means the web route (OFFGRID's output feeding a virtual camera such as OBS Virtual Camera in the browser). On Bigo, use the official OBS option if Bigo has enabled it for the account, or Bigo LIVE Streamer on PC. [B11][R16]
- **Use solo mode.** In battles, PK, or guest boxes the video is only part of the screen.
- **Output tall 9:16** (for example 1080×1920) so phones don't crop it. Keep the important art in the **middle** of the frame (guess: the middle ~80% wide and away from the bottom third, where chat sits).
- **Keep it short** (about 1–2 seconds), and **avoid strobing.** The Meet Group's ToS carries a seizure warning [M11], and the W3C (World Wide Web Consortium) accessibility guideline says content should not flash more than **three times per second**. [W1] A smooth wipe is safer than flashing red.
- **Expect chat on top.** That is normal and cannot be changed by a video effect.
- **Test with a second account on a second phone** watching the stream, to see what viewers really see (cropping, delay, compression).
- **Guess (good practice):** don't copy a platform's official gift artwork or text such as "X sent Gala Dragon." Viewers could think a real gift was sent, and Bigo's rules penalize "deceptive" use. [B4]

**Option B: Send or receive the platform's own gift (the true "over everything" look)**

- This is the only legitimate way to get an animation drawn **over chat** on every viewer's screen. It costs real money (for example, about $210+ for a Gala Dragon on Bigo, reported). [R1][R3] Check the in-app gift panel for current prices and artwork. [B1]
- On Bigo, a VIP or Super Fan Club entrance gives a full-screen welcome when the member enters a room. [B3][B4]

**Option C: Don't**

- Don't use mod APKs, injectors, or scripts that claim to show overlays to all viewers. They can't reach other viewers' apps, they break the ToS of all three platforms, and they put Royalty's accounts, money, and phone at risk. [B9][B10][M11]

---

## Sources

**Official: Bigo Live**

- [B1] Bigo, "How to Send Gifts on Bigo Live: Step-by-Step": https://www.bigo.tv/blog/bigo-live-virtual-gifts
- [B2] Bigo, "BIGO LIVE Tutorial: How to Use BIGO LIVE (Ultimate Guide)": https://www.bigo.tv/blog/use-bigo-live
- [B3] Bigo, "Bigo Live App Features" (Super Fan Club full-screen welcome, events, Task Center): https://www.bigo.tv/blog/bigo-live-app-features
- [B4] Bigo, VIP Service Agreement: https://activity.bigo.tv/live/act/bigo_vip/agreement.html
- [B5] Bigo, "BIGO LIVE PK Explained": https://www.bigo.tv/blog/bigo-live-pk
- [B6] Bigo, "Close PK battle? ... new Room PK System" (on-screen boost banners): https://www.bigo.tv/news-events/new-multi-room-pk-system-guide
- [B7] Google Play, Bigo Live listing: https://play.google.com/store/apps/details?id=sg.bigo.live
- [B8] Bigo, "How to Prevent Harassment on Bigo Live": https://www.bigo.tv/blog/how-to-prevent-harassment-bigo
- [B9] Bigo, "BIGO LIVE Hack: Free Diamonds, MOD APKs & Safety": https://www.bigo.tv/blog/bigo-live-hack
- [B10] Bigo User Agreement (text file the agreement page loads): https://www.bigo.tv/agreement.html and https://www.bigo.tv/assets/css/agreement_lang/lang_en.txt
- [B11] Bigo, "BIGO LIVE PC Guide 2026" (OBS, Streamer, Connector): https://www.bigo.tv/blog/bigo-live-pc
- Bigo, "BIGO Awards Gala 2020-2027" (real red carpet at in-person gala): https://www.bigo.tv/news-events/bigo-awards-gala
- Bigo official YouTube, "BIGO LIVE - fancy gift effects": https://www.youtube.com/watch?v=eoqu5-kmfy0

**Official: MeetMe, Tagged, The Meet Group**

- [M1] Tagged Help, "Who can use Tagged Live?": https://support.tagged.com/hc/en-us/articles/26446060224018-Who-can-use-Tagged-Live
- [M2] The Meet Group, "Tagged Launches Tagged Live": https://www.themeetgroup.com/tagged-launches-tagged-live/
- [M3] Google Play, MeetMe listing: https://play.google.com/store/apps/details?id=com.myyearbook.m
- [M4] The Meet Group, "Begins to Rollout Battles": https://www.themeetgroup.com/the-meet-group-begins-to-rollout-battles-and-provides-update-on-progress/
- [M5] The Meet Group, "Launches Levels on MeetMe App": https://www.themeetgroup.com/the-meet-group-launches-levels-on-meetme-app/
- [M6] MeetMe Help, "What are levels?": https://support.meetme.com/hc/en-us/articles/24009762860562-What-are-levels
- [M7] MeetMe Help, "What are Spotlight items?": https://support.meetme.com/hc/en-us/articles/24009842444178-What-are-Spotlight-items-and-how-do-they-work
- [M8] Tagged Help, "What are boost items?": https://support.tagged.com/hc/en-us/articles/26445668635282-What-are-boost-items-and-how-do-they-work
- [M9] MeetMe Help, "How do I get on the trending page?": https://support.meetme.com/hc/en-us/articles/24009751560594-How-do-I-get-on-the-trending-page
- [M10] MeetMe Help, "What are bouncers?": https://support.meetme.com/hc/en-us/articles/24009801959570-What-are-bouncers ; Tagged Help, "What are bouncers?": https://support.tagged.com/hc/en-us/articles/26445726443538-What-are-bouncers
- [M11] The Meet Group Live Terms of Service (no modified software; photosensitivity warning): https://terms.meetme.com/terms-of-service
- [M12] Agora, "The Meet Group uses Agora's scalable live video": https://www.agora.io/en/success-stories/the-meet-group/
- Google Play, Tagged listing: https://play.google.com/store/apps/details?id=com.taggedapp

**Official: technical and accessibility references**

- [T1] Tencent RTC docs, "Virtual Gifts" (gift events broadcast to all viewers; each app plays full-screen animation locally): https://trtc.io/document/74603
- [W1] W3C, "Understanding Three Flashes or Below Threshold" (WCAG, Web Content Accessibility Guidelines): https://www.w3.org/WAI/WCAG21/Understanding/three-flashes-or-below-threshold.html

**Reports (third-party, forums, video): not official**

- [R1] Discount Diamond Store (top-up seller), "5 Gifts on Bigo Live That Make the Biggest Visual Impact": https://discountdiamondstore.com/5-gifts-on-bigo-live-that-make-the-biggest-visual-impact-in-a-stream/
- [R2] BitTopup News (top-up seller), "BIGO Live Gift Prices 2025": https://news.bittopup.com/news/bigo-live-gift-prices-2025-complete-diamond-cost-guide
- [R3] Buffget (top-up seller), "Bigo Live Valentine's Gala 2026: Complete Gift Guide": https://buffget.com/news/bigo-live-valentines-gala-2026-complete-gift-guide
- [R4] BitTopup (top-up seller), "BIGO Live Gift Prices: Complete 1-60,000 Diamond Cost Guide": https://bittopup.com/article/BIGO-Live-Gift-Prices-Complete-160000-Diamond-Cost-Guide
- [R5] MyWOP Network blog, "The Grand Entrance: ... Most Iconic Mounts": https://mywopnetwork.com/blogs/11250/The-Grand-Entrance-A-Countdown-of-the-10-Most-Iconic
- [R6] Live Haishin Navi (Japanese guide), "BIGOLIVEのVIPとは" (VIP entrance sticker, 1-diamond bullet messages): https://live-haishin-navi.com/bigolive-vip
- [R7] BitTopup (top-up seller), "Bigo Live Noble Ranks Guide 2026": https://bittopup.com/article/Bigo-Live-Noble-Ranks-Guide-2026-Baron-to-Duke-Costs
- [R8] BitTopup News, "How to Disable Bigo PK Punishment Filters 2026": https://news.bittopup.com/news/how-to-disable-bigo-pk-punishment-filters-2026-quick-fix
- [R9] BitTopup, "How to Disable Bigo Live PK Punishment Effects Fast": https://bittopup.com/article/How-to-Disable-Bigo-Live-PK-Punishment-Effects-Fast
- [R10] XDA Forums, "Bigo Live Long Bullet Message Trick" (2017): https://xdaforums.com/t/bigo-live-long-bullet-message-trick.3554709/
- [R11] Vocal Media (MeetMe streamer), "An Honest Guide; How to Be a MeetMe Streamer": https://vocal.media/01/an-honest-guide-how-to-be-a-meet-me-streamer
- [R12] BetterTok, "Star of Red Carpet Gift" (TikTok): https://bettertok.app/tools/tiktok-gifts/star-of-red-carpet
- [R13] OBS Forums, "Trouble Streaming On MeetMe" (2024): https://obsproject.com/forum/threads/trouble-streaming-on-meetme.175982/
- [R14] YouTube, "How to Use Bigo Live App - Complete Beginners Guide" (room layout; via search summary): https://www.youtube.com/watch?v=1mSPtQP6AFc
- [R15] Blue's Mods, "Kik Live + MeetMe Gift Viewer": https://bluesmods.com/kiklive/
- [R16] OBS Forums, "BIGO LIVE" (2022; OBS option reportedly only enabled for some broadcasters): https://obsproject.com/forum/threads/bigo-live.156872/
- [R17] Reddit r/obs, "Stream LIVE on Plenty of Fish and MeetMe Using OBS" (title and search summary only; Reddit blocked direct reading): https://www.reddit.com/r/obs/comments/11swssx/stream_live_on_plenty_of_fish_and_meetme_using_obs/

*Related OFFGRID doc: "Battle Box Kick Research.md" (how kicks work and why auto-rejoin is risky).*
