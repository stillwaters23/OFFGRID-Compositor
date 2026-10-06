# Battle Box Kick Research

*Research date: Tuesday, October 6, 2026 (CT). Prepared for OFFGRID-Compositor scoping. Web research only; nothing was installed, downloaded as software, or logged into.*

## Bottom Line

- **No app can stop a host from kicking Royalty.** On Bigo Live, Tango, TikTok Live, and MeetMe, the host (and sometimes helpers the host appoints) decides who sits in a guest seat or stays in a battle. That decision is carried out by the platform's servers (the company's computers), not by Royalty's phone. The only way to "block" it would be to tamper with the platform, which is out of scope, against every platform's terms of service (ToS), and a fast route to a ban.
- **Rejoining is allowed, but only the normal way:** tap the empty seat or "+" again and send a new request, which the host can accept or deny. If the host *blocked* Royalty (not just kicked), rejoining that host is not possible until they unblock.
- **Do not auto-rejoin.** Using auto-tappers (for example an Android Accessibility Service clicker) to re-request a seat is explicitly banned on TikTok, and on Bigo it falls under broad rules against getting around platform limits and disrupting other users. It also looks like harassment to a host who just removed Royalty, and invites a block or report.
- **Recommended design:** OFFGRID shows an alert ("You were removed from the box") and offers a **one-tap button that opens Bigo** so Royalty can decide whether to request to rejoin by hand. The safest way to trigger the alert is a **manual "I got kicked" button**, optionally helped by low-risk hints (see "Detection Options").

## How To Read This Document

Each finding is labeled:

- **Confirmed** – stated on an official page (help center, official blog, ToS, or Google/Android documentation).
- **Reported** – said by third-party guide sites, complaint sites, or forum/Reddit users. Useful, but not official. Note: several Bigo "guide" sites (BitTopup, Buffget, EnjoyGM, GameBar, LootBar) are businesses that sell top-ups; treat them as low-reliability.
- **Guess** – my own reasoning where no source was found.

Short glossary:

- **Host** – the person running the live stream.
- **Guest seat / battle box / multi-guest seat** – a video box on the host's screen where another user appears.
- **PK ("Player Kill" or "Player Knockout")** – Bigo's name for a timed gift battle between streamers.
- **Server-side** – handled on the company's computers, so the user's phone can't overrule it.
- **ToS** – Terms of Service, the rules you agree to when using an app.
- **Accessibility Service** – an Android feature meant for assistive tools (like screen readers) that can read what's on screen and tap buttons for the user. Auto-clickers misuse it.
- **Notification access** – an Android permission, turned on by the user in Settings, that lets an app read other apps' notifications.

## 1. How Kicks Work On Bigo Live

### Who Can Remove Someone

- **Confirmed:** Bigo's official blog says hosts have "full control over their guest seats" and can "remove a guest at any time by tapping on their icon and selecting the remove option." Hosts can also mute guests. Guests join by tapping an empty "+" seat to send a request, which "the host ... can choose to approve or deny." [S1]
- **Confirmed:** Bigo's official tutorial lists host controls in multi-guest: accept/reject join requests, invite, mute, and "remove guests from the grid." [S2]
- **Confirmed:** In a PK battle, two hosts link their rooms in split screen for a fixed time; the winner is decided automatically when the timer ends. Bigo's blog says quitting mid-PK is "highly discouraged" and "may even lead to penalties in official events", which tells us a host *can* leave a PK early. [S3][S2]
- **Reported:** Hosts can appoint **admins** (moderators). Some guides say admins can mute, kick, or block viewers [S6]; others say only the host can remove someone from a *guest seat*, while admins handle chat and viewers [S5][S7]. The exact split is not documented officially.
- **Reported:** Three levels of action exist: **Mute** (silenced, stays in seat), **Kick** (removed, may come back), **Block** (can't join that host's lives, message them, or view their profile). [S5][S6]

### Is It Server-Side?

- **Guess (high confidence):** Yes. Seat membership is shared live with every viewer, so it must be controlled by Bigo's servers. When the host taps "remove," Bigo's servers drop Royalty's video from the room for everyone. Nothing on Royalty's phone gets a vote. This matches how every multi-person live platform works, but Bigo does not publish technical details.

### What The Kicked User Sees

- **Not confirmed.** No official page describes the exact screen. **Guess:** Royalty's box disappears from the host's grid, Royalty drops back to being a viewer (or leaves the room if kicked from the room entirely), and a short pop-up message may appear. A complaint-site report describes simply being "kicked out of the live" with no explanation. [S8]

## 2. Can A Kicked User Rejoin?

- **Confirmed (Bigo):** The official way in is to tap an empty "+" seat and send a request; the host approves or denies. Nothing on the official pages says a kick prevents a new request. [S1]
- **Reported (Bigo):** A kick is "temporary removal, can rejoin" if the host approves again; a block is permanent until the host undoes it. [S5][S7] Some guides describe a **mic queue** (a waiting list for seats) that hosts can turn on, and say a kicked guest "can re-queue immediately." [S7] No official cooldown time was found.
- **Confirmed (MeetMe, for comparison):** "Users kicked from a stream may not rejoin the same stream but can join future streams started by the streamer." Blocking stops future streams too. [S17]
- **Guess:** A host who kicks Royalty can simply deny the next request or block. There is no feature that forces a host to take someone back.

## 3. Is There Any Legitimate Way To Prevent A Kick?

**No.** Plainly stated:

- The kick is a host decision carried out on the platform's servers. An app on Royalty's phone cannot block a server-side host action **without tampering with the platform** (for example, modifying the Bigo app, interfering with its network traffic, or faking data). That is **out of scope** for OFFGRID and is **against Bigo's ToS**, which forbids users to "remove, circumvent, disable ... or otherwise interfere with ... features that enforce limitations on the use of the Services," to reverse engineer or modify the app, and to "interfere with or disrupt the Services or servers." [S4] Bigo can then "terminate your license ... and block or prevent your future access." [S4]
- **The only legitimate "prevention" is social:** agree on rules with the host beforehand, stay within the host's room rules and Bigo's guidelines, and keep a good relationship with hosts you battle with.

## 4. Auto-Rejoin: Is Automating Taps Allowed?

### What Bigo's Terms Say

- **Confirmed:** Bigo's User Agreement (last updated January 17, 2025, fetched today) does **not** use the words "auto-clicker," "bot," or "script." [S4]
- **Confirmed:** It does forbid, among other things: (iv) getting around "features that enforce limitations on the use of the Services"; (ix) using the Services "in any manner that could interfere with, disrupt, negatively affect or inhibit other users from fully enjoying the Services"; and (iii) harassing users. Breaking these "may result in ... termination or suspension." Section 9 lets Bigo cut off access "in our sole discretion." [S4]
- **Guess:** An auto-rejoin tool aimed at overriding a host's kick fits (iv) and (ix) closely. A host's kick *is* a limitation, and spamming seat requests at a host who just removed you disrupts their room. Bigo does not need a specific "no auto-clickers" line to act.
- **Reported:** Guide sites say "third-party tools" that "automate" actions or "modify the app" are "a primary ban trigger," and that Bigo uses behavior analysis of "interaction timing." [S9] (Low-reliability source.)

### What Android/Google Says

- **Confirmed:** Google Play allows Accessibility Service automation only for "a narrow and clearly understood purpose." It requires a clear in-app disclosure and consent, and forbids apps that "autonomously initiate, plan, and execute actions." Simple "If X happens, do Y" rules are allowed *by Google*. [S10] **But Google's rules are separate from Bigo's.** Passing Google's policy does not make something allowed on Bigo. (For a private sideloaded app, Google Play's review doesn't apply at all, but Bigo's ToS still does.)

### Ban Risk

- **Guess:** **Moderate to high**, and mostly not from detection software. The likely problems: (1) the host blocks and/or reports Royalty for harassment after repeat requests; (2) repeat, rapid, machine-timed requests stand out; (3) some apps check for running Accessibility Services or auto-clickers. The downside (losing the account, gifts, and beans) is far larger than the upside (saving a few taps).

### Honest Recommendation

**Do not build auto-rejoin.** Build:

1. **A removal alert** in OFFGRID ("You were removed from the box").
2. **A one-tap "Open Bigo" shortcut**, a normal app launch that brings Bigo to the front. Royalty then decides whether to tap "+" and request a seat again by hand.
3. Optional: a **cool-down reminder** ("Wait before re-requesting; the host may not want you back right now") to avoid looking like spam.

## Detection Options

How could a separate private app like OFFGRID notice that Royalty was kicked? Ranked from safest to riskiest.

| Option | How It Would Work | Reliability | ToS / Policy Risk |
|---|---|---|---|
| **A. Manual button** | Royalty taps "I got kicked" (on screen, in a floating bubble, or in OFFGRID's notification). OFFGRID then shows the alert and "Open Bigo" button. | **High**: Royalty knows for sure. Costs one tap. | **None.** No interaction with Bigo at all. **Recommended default.** |
| **B. OFFGRID's own video pipeline** | OFFGRID watches its *own* output: if Bigo stops pulling video frames from OFFGRID, or the camera/encoder session closes, OFFGRID flags "possible removal." | **Low to medium.** Depends on how OFFGRID feeds video into Bigo. Bigo may keep the camera open after a kick (so there's no signal), or close it for other reasons like a network drop or Royalty leaving (false alarms). Use as a *hint* that asks "Were you removed?", not as proof. | **Low**, *if* OFFGRID feeds video through normal, allowed Android paths. **Caution:** any method of injecting video into Bigo that modifies Bigo or fakes a camera at the system level could itself break Bigo's "do not modify" rules. Check this separately. |
| **C. Reading Bigo's notifications** | With **notification access** (Royalty turns it on in Android Settings), OFFGRID reads incoming Bigo notifications and looks for removal wording. | **Low.** No source shows that Bigo sends a notification when you're kicked. While Royalty is inside Bigo, the app likely shows an in-app message instead of a system notification (guess). Wording changes by language and app version. | **Low to medium.** Reading your *own* notifications on your *own* phone is an allowed Android feature [S11], and Bigo's ToS doesn't mention it. It is sensitive access, though (it sees *all* notifications), so keep it opt-in and filter to Bigo only. |
| **D. Screen reading (Accessibility Service, read-only)** | OFFGRID's Accessibility Service watches Bigo's screen elements, for example noticing Royalty's seat disappear or the "+" request button reappear. **Reading only, no taps.** | **Medium.** It can work, but it breaks when Bigo updates its layout or language, and some screens may be custom-drawn and unreadable. | **Medium.** Bigo's ToS does not ban this outright, but some apps detect active Accessibility Services and may flag them (guess). Google Play would require a prominent disclosure if published [S10]. Keep it strictly read-only. |
| **E. Screen capture plus image recognition** | Android screen capture (asks Royalty's permission each session) plus on-device image matching to spot the kicked state. | **Low to medium.** Heavy on battery, fragile. | **Medium.** Same gray area as D. Also captures other users' video, which is a privacy concern. Not recommended. |
| **F. Watching Bigo's network traffic** | Intercept or inspect Bigo's data to see the server's "removed" message. | Could be accurate. | **High: do not do this.** It's tampering/reverse engineering under Bigo ToS (iv), (v), (ix). [S4] Out of scope. |

**Recommendation:** Ship **A (manual button)**. Optionally add **B** as a gentle "Were you removed?" prompt if OFFGRID's video path makes it easy. Consider **C** or **D** only as opt-in extras, and **never** pair them with automatic tapping.

## Rejoin Methods By Platform

| Platform | Allowed Rejoin Method (Manual) | Kick vs. Block | Automating the Rejoin |
|---|---|---|---|
| **Bigo Live** | Tap empty "+" seat to send a request; host approves/denies [S1]. Mic queue/waitlist if host enabled it (reported) [S7]. | Kick = can request again (reported); Block = can't join host's lives [S5][S6]. | **Not safe.** Falls under ToS clauses on circumventing limits and disrupting others [S4]. Risk: host block/report, account action. |
| **TikTok Live** | Tap Multi-guest, then Request; host accepts [S13]. Host can also invite you. | Host removes a guest with "Disconnect" (official one-pager) [S12]. Blocked viewers are kept out of that host's LIVE (reported) [S14]. | **Explicitly banned.** TikTok's ToS forbids using "automated scripts to collect information from or otherwise interact with the Services." [S15][S16] |
| **Tango** | Accept/send Live Party or Battle invites (host-driven) [S18][S19]. | Admins "may kick out unwanted viewers" [S20]. Battles: up to 4 users; host invites, others accept/decline [S18]. | **Not safe.** Tango's ToS bars using the service "in any manner which could damage, disable, overburden or impair" it (quoted via ToS;DR) [S21]; viewer bots are "strictly forbidden" [S22]. A specific auto-clicker clause couldn't be verified (Tango's ToS page loads by script). |
| **MeetMe / Tagged** | Tap "+" then "Guest" to request; streamer must accept [S23]. | Kicked users can't rejoin *that* stream but can join future ones; block stops all [S17]. | **Not safe.** Rules bar posting "automatically, systematically, or programmatically" and using "robots" [S24]. Tagged is run by the same company (The Meet Group); not separately checked. |

## 5. Tango And TikTok Live In Brief

- **TikTok Live (Confirmed):** Hosts invite guests and remove them by tapping "X" in the LIVE room, on the settings page, or on the guest's profile ("Disconnect"). [S12] Hosts can limit requests to followers and add moderators who can manage muted and blocked lists. [S13][S14] Kicks are host-controlled; there's no way for a guest to refuse. TikTok's ToS is the clearest of all in banning automated interaction. [S15][S16]
- **TikTok LIVE matches (Reported):** Either side can leave a match early; the match ends while each person's own LIVE continues. [S25] Same conclusion: nothing a guest can block.
- **Tango (Confirmed):** Battles are by invitation, up to 4 broadcasters, winner by coins. [S18] Admins appointed by the host can kick viewers. [S20] Guest invites are managed by the host (from the app or Tango Web). [S19] An official "remove battle participant" page wasn't found, but control is clearly host-side.

## Confirmed vs. Reported vs. Guessed (Summary)

- **Confirmed:** Hosts control guest seats and removal on Bigo, TikTok, MeetMe; guests rejoin only by sending a new request the host must accept; Bigo ToS bans getting around platform limits, modifying the app, and disrupting others; TikTok ToS explicitly bans automated interaction; MeetMe bars programmatic actions and says kicked users can't rejoin the same stream; Google allows Accessibility automation only for narrow, disclosed uses.
- **Reported:** Bigo kick = temporary, block = permanent; Bigo admins' exact powers; Bigo mic-queue "re-queue immediately"; Bigo detects automation by timing patterns; TikTok blocks keep viewers out of the LIVE.
- **Guessed:** Exact on-screen message a kicked Bigo user sees; whether Bigo sends a notification on kick (probably not); that Bigo checks for Accessibility Services; the overall ban-risk level of auto-rejoin.

## Sources

- [S1] Bigo Live official blog, "How to Host a Bigo Live Multi-Guest Stream": https://www.bigo.tv/blog/bigo-live-multi-guest-room
- [S2] Bigo Live official blog, "BIGO LIVE Tutorial: How to Use BIGO LIVE": https://www.bigo.tv/blog/use-bigo-live
- [S3] Bigo Live official blog, "BIGO LIVE PK Explained": https://www.bigo.tv/blog/bigo-live-pk
- [S4] Bigo Live User Agreement (last updated Jan 17, 2025), Sections 5 and 9: https://www.bigo.tv/agreement.html (text loads from https://www.bigo.tv/assets/css/agreement_lang/lang_en.txt)
- [S5] BitTopup (reseller guide; low reliability), "BIGO Live Multi-Guest Rooms 2026": https://bittopup.com/article/BIGO-Live-MultiGuest-Rooms-2026-Complete-Setup-Guide
- [S6] EnjoyGM (reseller guide; low reliability), "How to Prevent Harassment on Bigo Live as Host": https://www.enjoygm.com/blog/bigo-live/how-to-prevent-harassment-on-bigo-live
- [S7] BitTopup (reseller guide; low reliability), "BIGO Live Mic Queue Guide": https://bittopup.com/article/BIGO-Live-Mic-Queue-Guide-Manage-MultiGuest-Rooms-2026 ; also LootBar, "BIGO LIVE Voice Chat Rooms Guide": https://www.lootbar.com/blog/en/bigo-live-voice-chat-rooms-guide-features-tips.html
- [S8] Sikayetvar/Xolvie complaint (user report), "Bigo Host Kicked Me From Live Without Reason": https://www.sikayetvar.com/en/bigolive-us/bigo-host-kicked-me-from-live-without-reason-after-i-sent-gifts
- [S9] Buffget (reseller guide; low reliability), "Bigo Live Account Recovery": https://buffget.com/news/bigo-live-account-recovery-fix-bans-and-frozen-diamonds
- [S10] Google Play Console Help, "Use of the AccessibilityService API": https://support.google.com/googleplay/android-developer/answer/10964491?hl=en
- [S11] Android Developers, NotificationListenerService reference: https://developer.android.com/reference/android/service/notification/NotificationListenerService
- [S12] TikTok official PDF, "TikTok LIVE Multi-guest One-pager" (April 2022): https://sf16-va.tiktokcdn.com/obj/eden-va2/lfpzbf/live_creator_hub/TikTok-LIVE-Multi-guest-One-pager_April-2022.pdf (contents seen via search summary)
- [S13] TikTok LIVE Creators, "Introduction to Multi-guest": https://www.tiktok.com/live/creators/en-US/article/Multi-guest_Introduction_en-US ; TikTok Support (page loads by script): https://support.tiktok.com/en/live-gifts-wallet/tiktok-live/tiktok-live-multi-guest ; TTS Vibes guide (third party): https://ttsvibes.com/blog/how-to-go-live-with-a-friend-on-tiktok
- [S14] TikTok Shop Academy, "LIVE Moderator": https://seller-us.tiktok.com/university/essay?knowledge_id=6837825177814785&lang=en ; TikTok Support, "Moderating on TikTok LIVE": https://support.tiktok.com/en/live-gifts-wallet/tiktok-live/moderating-on-tiktok-live
- [S15] TikTok Terms of Service (rest of world): https://www.tiktok.com/legal/page/row/terms-of-service/en ; (US): https://www.tiktok.com/legal/page/us/terms-of-service/en
- [S16] TikTok Privacy Center, "How we combat scraping": https://www.tiktok.com/privacy/blog/how-we-combat-scraping/en
- [S17] MeetMe Help, "What are bouncers?": https://support.meetme.com/hc/en-us/articles/24009801959570-What-are-bouncers
- [S18] Tango Help Center, "Competition Broadcasts!": https://help.tango.me/en/articles/4874027-competition-broadcasts
- [S19] Tango Help Center, "Stream Like a Pro with 2 Devices": https://help.tango.me/en/articles/11986931-stream-like-a-pro-with-2-devices
- [S20] Tango Help Center, "Set Your Own Admin": https://help.tango.me/en/articles/3580040-set-your-own-admin
- [S21] ToS;DR summary of Tango Terms of Use: https://edit.tosdr.org/points/208071 and https://edit.tosdr.org/documents/20848 ; official page (loads by script): https://www.tango.me/terms-of-use
- [S22] Tango Help Center, "Tango Me Online Streaming Platform Rules and Guidelines": https://help.tango.me/en/articles/8485746-tango-me-online-streaming-platform-rules-and-guidelines
- [S23] MeetMe Help, "How do I become a Guest in someone's stream?": https://support.meetme.com/hc/en-us/articles/24009843353490-How-do-I-become-a-Guest-in-someone-s-stream
- [S24] MeetMe Content and Conduct Policy, item 9: https://terms.meetme.com/content-and-conduct
- [S25] Search-result reports on leaving TikTok LIVE matches (third party, unverified), e.g. https://www.tiktok.com/discover/how-to-leave-a-live-battle-on-tiktok?lang=en

## Gaps And Limits

- Bigo's Community Guidelines and TikTok's support pages load by script and couldn't be read in full; quotes from them come from search snippets only.
- No official Bigo statement on kick cooldowns, what a kicked user sees, or whether a notification is sent.
- Tango's full ToS text couldn't be loaded directly; relied on ToS;DR excerpts.
- Tagged was not checked separately from MeetMe.
