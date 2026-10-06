# Folder Guidance Panel

**Project:** OFFGRID-Compositor  
**Feature:** 19 in `01 Brainstorm/Feature Scope.md`  
**Status:** Scope only. Nothing is built yet.  
**Updated:** 2026-10-06 (America/Chicago)

---

## What it is

Every platform folder and every effect folder in the terminal panel opens with a read-only guidance panel. It's typed in the OFFGRID laser typewriter font, letter by letter with the hard block cursor `█`, matching the Grok Bot Laser Typewriter UI Spec in `03 Design/References`.

## Template (one per folder)

```
> FOLDER: <platform or effect name>
> CAN DO: <effects and features allowed here>
> CAN'T DO: <what this platform blocks>
> WHERE: <native app | website only> — <exact web address>
> BROWSER: <Chrome | Firefox | Brave | Kiwi Browser> — <download link>
> VPN: ProtonVPN Stealth — exit <country, e.g. Philippines for PH streams>
> ORDER: 1) Create Cloaked number with VPN OFF  2) Turn on Stealth  3) Open the app
> PHONE: <dummy phone 1 | dummy phone 2 | S24 | S25 Ultra | A37>
> IP CHECK: <MASKED ✓ | LEAK ✗> — seen as <IP, country> (feature 25)█
```

## Rules for filling it in

1. Fill each line only from `02 Research` and confirmed results of the platform test toggles (feature 15). If something isn't confirmed, the line says `UNCONFIRMED`.
2. Exact web addresses matter. For example, meetme.com is the MeetMe app's company site, but meetme.video is a separate browser-only site.
3. Browser: Kiwi Browser was reportedly discontinued in early 2025. Confirm a browser is still maintained and get its official download link before listing it. Only link official stores or official sites.
4. VPN: ProtonVPN in Stealth mode. Use a Philippines exit server for PH streams. Always create the Cloaked number with the VPN off first, then turn on Stealth before opening the app.
5. Phone: test with dummy phone 1 or dummy phone 2 and the dummy account. Never risk the paid big-gifter account.
6. No logins, passwords, numbers, keys, or stream keys ever appear in the panel, the repo, or the chat.
7. IP check: shows the live result of the IP leak check (feature 25). Show the masked address and country only, never the real IP.
8. The panel is guidance only. It has no buttons and doesn't change any settings.

## Acceptance checks (for the HR Manager)

- Every platform folder and every effect folder has a panel.
- All lines are present: what, where, browser, VPN, phone, and IP check.
- The text uses the laser typewriter style and cursor.
- Every value is sourced or marked `UNCONFIRMED`.
- No credentials or private numbers anywhere.
