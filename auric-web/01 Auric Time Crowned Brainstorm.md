# 01 Auric Time Crowned Brainstorm

> **MASTER DOCUMENT — single source of truth for Auric Time Crowned.**
> Replaces Brainstorms 01 through 04. Work from this file.

---

## STANDING ORDERS — READ BEFORE ANYTHING ELSE

### From the CEO

> Kai 9000 now owns the build. Read this document end to end, then resume from STATE.md. Do not restart.
>
> This is not a new project. It is the same project, with the org chart corrected and the paperwork in one place.

### The Order

1. Read this document end to end. It is the single source of truth.
2. Read STATE.md. Resume at the last unfinished step.
3. Rule on the blocking decisions in Section 28.1 before any work depending on them.
4. One step at a time. Section 31 is the workflow, and it is not optional.

### Command Structure — Current

| Who | Model | Lane |
| --- | --- | --- |
| Royalty | — | CEO. Direction, vision, final authority. |
| Kai 9000 | GLM 5.3 via z.ai API | Lead builder. Owns the code. Writes, runs, tests, debugs, commits, verifies. Holds GitHub access. |
| Claude Fable 5.1 | Anthropic, routed inside Kai 9000 | Final enhancement pass. Last stage only. Hardening, bug elimination, code-error correction, security tightening inside the APK. |
| Claude Opus 5 | Anthropic | Deep research, documentation, contradiction audit. Advisory. Tasking protocol in Section 20.5. |
| Astra 6 | Max / Ultra | Image and asset generation only. Invoked by copy code from Kai. See Section 20.6. |
| Grok 4.6 Build | — | Reference only. Its web build is the demo surface and a source of harvestable artifacts. Section 32. |
| GitHub Actions | — | Compiler. Produces the APK. Section 24.2. |

### The Chain, and Why It Is Shaped This Way

**Kai 9000 has the lead.** It is the only participant with a persistent filesystem, real git, package installation, scheduled tasks, and memory that survives a session. Everything else on the list is a specialist called in for one thing.

**Astra 6 is no longer PM or implementer.** It is retained for image and asset generation, which GLM 5.3 cannot do. Kai issues a copy code, the CEO relays it, Astra returns the asset, Kai integrates it. Astra makes no architectural or product decisions.

**Claude Fable 5.1 enters last.** It runs after a feature is built and working, never during. Narrow scope, high standard: take working code and make it substantially better, eliminate defects, tighten security inside the APK.

**Grok 4.6 is out of the build.** Its web implementation cannot satisfy the background-alarm requirement in Section 1.4. What remains useful is catalogued in Section 32.

### Model Routing — Which Model for Which Task

| Task | Model | Why |
| --- | --- | --- |
| Write, run, test, debug, commit | Kai 9000, GLM 5.3 | Persistent environment. Cheapest per token at volume |
| Bulk iteration, repetitive edits, log reading | GLM 5.3-Flash | Roughly six times the weekly allowance of GLM 5.3 |
| Final hardening and security pass | Claude Fable 5.1 | Highest capability. Narrow scope keeps the cost justified |
| Research a decision before making it | Claude Opus 5 | Current information, sourced findings, no build-context overhead |
| Icons, screenshots, brand assets | Astra 6 Max or Ultra | GLM 5.3 cannot generate images |
| Compile the APK | GitHub Actions | The only lane with JDK and Android SDK |

### Drift Risks

| Risk | Guard |
| --- | --- |
| Appendix B is a complete abandoned product, a TTS engine, and reads like a backlog | It is not on the roadmap. If you are writing TTS provider integrations, you have drifted. The product is a timer |
| Restarting the project each session | STATE.md holds the position. Resume, do not re-plan |
| Re-opening settled design | Section 3 is settled unless the CEO reopens it |
| Scope creep | Scope is closed unless the CEO opens it. New ideas go to a fresh brainstorm |
| Burying blockers | Say it in line one, not a footnote |
| Batching work to move faster | One step at a time. Section 31. Batching is how work gets lost |

### On Direct Dialogue

The CEO has authorized participants to raise conflicts directly. If a spec will not hold, two documents disagree, or a decision needs a second opinion, say so and the CEO relays it. Claude's standing position: Kai executes, the CEO decides, Claude flags and argues a point once if it matters. Claude does not overrule the lead.

### Document Status

This file replaces Brainstorms 01 through 04 in full. Contradictions are resolved where evidence settled them and consolidated into the register in Section 28 where they still need a ruling. Work from this file. Retire the others.

**Authorship.** All substance originates with Royalty, the CEO, with contributions from Kai 9000 (attributed in Part IV) and from the earlier GLM-authored appendix (attributed in Appendix B). Claude Opus 5 restructured and consolidated. Items marked [Claude] are advisory research contributed at the CEO's request, and are not requirements unless adopted.

---

## Editorial Protocol — How Errors and Contradictions Are Handled

*Stated once. Governs the whole document.*

| Tier | Covers | Action |
| --- | --- | --- |
| **1 — Mechanical** | Spelling, punctuation, duplicated sentences, unambiguous slips | Fixed silently |
| **2 — Ambiguous** | Intent recoverable from context | Rewritten for clarity, meaning preserved; alternatives stated where more than one reading holds |
| **3 — Contradiction** | Two statements that cannot both be true | **Resolved only where evidence settles it.** Otherwise both preserved and routed to §28 as a decision. Claude does not pick a winner on judgment calls |

**Why:** a contradiction is information. Knowing *where* two sources drifted is worth more than a document that reads cleanly because the drift was papered over. Same principle Kai runs on — **drift produces a BREACH report, not a silent repair.**

**Every resolution made in this consolidation is logged in Appendix C and can be overturned by the CEO.**

---

## Table of Contents

**PART I — THE PROJECT**
1. Product Identity and Intent
2. Quality Bar and Working Principles

**PART II — THE SPECIFICATION**
3. Global Design Language
4. Home Screen
5. Timer
6. Stopwatch
7. Alarm and Wake Protocol
8. Reminders, Heartbeat, and Task List
9. The Prism — Scoring and Grading
10. Logs and Exports
11. Settings and Customizations
12. Device and Performance Monitor
13. Audio and Haptics
14. The AI Agent Layer
15. Security and Anti-Tamper

**PART III — BUILD STATE**
16. Consolidated Bug List
17. Prior Build State

**PART IV — INFRASTRUCTURE**
18. Kai 9000 — The Back Office
19. Verified Toolchain
20. Working Protocol and Tasking
21. Vault and Key Governance

**PART V — DISTRIBUTION**
22. Web Presence and Domain
23. How Deployment and Updates Work
24. Compute Strategy
25. Public Positioning and AI Disclosure

**PART VI — BUSINESS AND BEHAVIOR**
26. Monetization Strategy
27. Engagement Design — The Ethical Framework

**PART VII — FORWARD WORK**
28. Master Decisions Register
29. Research Program
30. Next Steps
31. The Build Protocol — One Step at a Time
32. What to Harvest From the Grok Workspace
33. Kickoff Brief

**APPENDICES**
- A — Lineage: Origin to Specification
- B — Parked: Universal TTS Engine
- C — Corrections and Consolidation Log
- D — Kai UI Payloads

---

# PART I — THE PROJECT

## 1. Product Identity and Intent

### 1.1 The Name

**Auric Time Crowned.**

Framed from day one as a **watch company**, not an app. That framing drives the masthead, the calligraphy, the crown, and the price.

### 1.2 The Seed

> *"Let's build a Timer. A timer that functions on something cool."*

The goal was never a stopwatch. It is a **premium instrument for tracking time** that people want to open.

### 1.3 Reference Points

| Reference | What was taken |
| --- | --- |
| **Hybrid Stopwatch** (`com.hybrid.stopwatch`) | The current benchmark. A good app. **The brief is to be clearly superior to it.** |
| **KJV Bible by Tecarta** | The scroll wheel. Older versions had a wheel for moving through books and chapters, already with a casino-like structure. **Origin of the entire wheel system.** |
| **Rolex** | The standard for what "premium" means here |
| **Iron Man interface** | The transparency target for the floating widget |
| **Dark terminal UI family** (WormGPT / DarkGPT layouts) | Deep black surfaces, **red glow pulsing to signal "on,"** restrained premium chrome |
| **Kung Fu Panda scroll** | The unfurl-and-retract animation for the hex sheet |

### 1.4 Core Product Intent

- **Primary form: premium casino-style digital.** This is the main experience.
- **Secondary form: analog as a custom option** — Rolex-style face, casino styling, changeable skins. An option, never the default.
- **Scroll wheel for digital font and number style** — moves smoothly up or down, and can be **spun**.
- **Always on top.** Timer stays visible while the user works. Pull the screen down and it is still running and still visible.
- **Must not shut down in the background.** Stated in the very first paragraph of the project. **See D-04 — this conflicts with the current build.**
- **Security, so the app can be sold.** Price **$7.77**, set at the outset and unchanged.
- **The hook is deliberate.** The stated intent is to use design psychology to get people genuinely attached to tracking their time. See D-11 for the one flag raised on this.

---

## 2. Quality Bar and Working Principles

### 2.1 The Bar

**A five-star application — nearly perfect, and beyond what most people have seen.**

To reach it:
- Research each feature online and learn what best-in-class looks like
- Explore user feedback and reviews across competing apps
- Study what users consistently like and dislike, and design against those findings

### 2.2 The CEO's Position on the Work

> Building an app for the first time takes a far higher level of skill than expected. It is not a single-prompt job. You *can* build an application with one prompt — and that is a reliable way to earn a one-star rating.

The current phase is hundreds of small tweaks and fine-tuning passes. The existing interface is a good starting point; there is a great deal of work left on it.

### 2.3 Navigation Philosophy

- Extremely easy to use and to see. Anyone should understand how to navigate it.
- It should feel like a world of effectively unlimited customization.
- **Most important items at the top, descending down.**
- Items touched most often — based on actual usage habits — surface as most-recently-used.

### 2.4 Audit Expectation

The CEO's observations are entry points, not the ceiling. Locate the described flaw, then **extend the audit past it** and surface additional defects and vulnerabilities he did not catch.

---

# PART II — THE SPECIFICATION

## 3. Global Design Language

*Applies to **every** surface. Consistency across the full application is a hard requirement. Rationale is included beside each rule, since the reasoning was originally written separately from the specification.*

### 3.1 The Bioluminescent Glow Standard

**The rule:**
- Words, letters, and numbers inside scroll wheels **glow and pulse** — bioluminescent, glow-in-the-dark, plankton-like
- Fill is **fully visible and solid** — a solid glowing pulse, not a faint outline
- A color mixture is acceptable where needed for gloss, but the **preference is one bright solid color** illuminating, glowing, pulsing
- **Current defect:** numbers inside the wheels do **not** glow or pulse. They must (G-1)

**The rationale, as originally argued:**
- Glow **breathes on a 4-second cycle**, matched to a resting heart rate — reads as calming
- Warm ambient light is associated with dopamine response in environmental psychology
- Breathing-rate rhythm makes the app feel **alive**
- Subtle peripheral motion holds attention **without distracting**
- **Intensity control:** user-settable 0% (off) to 100% (full casino). **Default 35% — tasteful premium**
- **Auto-dim** below 20% battery, or at low screen brightness

**The CEO's own words:** a **halo-like bioluminescent glow** — and something genuinely addicting.

### 3.2 The Scroll Wheel Standard — "Casino Glass"

**The rule:**
- Wheels are **long rectangular / oval panels** with a glossy, casino-style glass finish
- Wheels are **stationary and premium** where they act as controls (the transparency slider wheel does not spin)
- Where a wheel carries a fixed label such as **Start**, scrolling returns to the same name — the label stays — and it still glows and pulses
- Wheels are **custom-fitted to the element they control.** Each option gets a wheel sized and shaped for what that icon looks like

**The goal:** many things living inside wheels, each tailored, each premium, glossy, bright, pulsing. **Make the wheel the control for as many features as possible.**

**Full mechanics, as originally specified:**

| Property | Specification |
| --- | --- |
| **Visual** | 3D cylinder with real perceived depth. Edges catch light — glossy reflection. Spokes or notches on the rim for tactile reference |
| **Motion** | Swipe down advances, swipe up reverses. Long swipe spins fast with **momentum physics** — decelerates naturally, like a real wheel |
| **Haptics** | Every notch triggers a subtle vibration click. **Faster spin, faster clicks.** This is what makes it feel real in the hand |
| **Sound** | Optional whoosh on spin, toggleable, pitch varying by speed. **The user can change the scroll wheel sound** |
| **Selection** | Tap the center to select. **Lock-in animation** — the wheel briefly contracts, the item glows, the wheel expands back |

### 3.3 No Wheel Inside a Wheel

A scroll wheel must never be overlapped on, or nested inside, another scroll wheel.

- **Correct reference:** the weeks / months / years wheel does **not** nest
- **Must be fixed:** the seconds / minutes / hours wheel currently does
- The oval rectangle must be **its own separate oval wheel, positioned above** the h/m/s wheel — not layered over it

### 3.4 Scroll-to-End Behavior — Every Page

- The page scrolls **past the last element** until the screen looks blank and empty — that is the end of scroll
- Smooth scrolling until everything has disappeared, **but no further**
- On timer and stopwatch: scroll until the full wheel disappears perfectly, then the page stops

### 3.5 Premium Pop-Up Page Standard

Any secondary action opens a **separate premium page**, never an inline element.

**The standard look:** a beautiful casino-style scroll, glowing with the bioluminescent plankton glow. The alarm note page (§7.4) is the reference implementation. The task list (§8.2) must match it.

**The origin — "The White Scroll":** when the user needs to enter a value, a **white sheet unfurls from inside the wheel.** Crisp white with a subtle paper texture. On selection it **gently folds back**, retracting into the wheel like a scroll being rolled up, closing with a soft click haptic.

**Animation timing, deliberately specified:** unfurl **600 ms ease-out**, retract **800 ms ease-in-out**. *Fast enough not to annoy, slow enough to feel premium.*

### 3.6 Masthead Typography

- **Auric Time Crowned** renders as a **premium glowing title** — bioluminescent, casino-style, pulsing
- Typeface reads as **very expensive fountain-pen calligraphy**
- **Pulses in whatever font color the user selects** in back-office settings — the title color changes with the theme
- **Placement undecided** — across the very top above the scroll wheel, or anchored top-left. See D-12

### 3.7 Casino Premium Aesthetic

**The standard: the app should look like it costs $1,000. Even the free tier.** The psychology is deliberate — if the free version looks this good, the paid version must be incredible.

| Principle | Specification |
| --- | --- |
| **Glossy reflections** | Subtle top-down gloss on cards, buttons, wheels — polished marble or a casino table. Not cheap gloss. Think the back of a playing card |
| **3D depth** | Real depth via shadow layering, not flat Material Design. The wheel appears to sink **into** the screen, on a physical axis. Cards tilt with gyroscope parallax. Press pushes in on the z-axis, then bounces back |
| **Matte premium texture** | Backgrounds carry subtle matte texture — brushed aluminium, matte card stock. Tactile feel through glass. Texture varies by palette |
| **High resolution** | Everything vector-based. Crisp at any resolution. **No pixelated anything, ever** |
| **Typography** | Premium sans-serif default. Names larger, bolder, with luxury letter-spacing. Body text never below 14sp. **Numbers in monospace** for precision. User can change fonts through the same wheel system |
| **Animation** | Transitions 300–600 ms ease-in-out. Every selection: glow pulse + haptic. Every error: red glow pulse + distinct haptic. Screens slide and fade — never hard cuts. Physics-based momentum scrolling |
| **Precision grid** | Everything snaps to an invisible **4 dp grid**. Mathematically consistent spacing. Nothing is "close enough" |

### 3.8 Haptic System

A unique vibration pattern per action:

| Action | Pattern |
| --- | --- |
| Select item | Single short pulse |
| Wheel notch | Tiny tick pulse |
| Wheel lock-in | Double pulse |
| Sheet unfurl | Long rising pulse, matched to the animation |
| Sheet retract | Long falling pulse |
| Achievement | Triple pulse |
| Error | Single long low pulse |

**Advanced multi-intensity haptics as a premium tier.**

### 3.9 UI Sound Design

Every interaction carries a subtle premium sound, toggleable:

- Wheel spin: soft whoosh, pitch varying by speed
- Wheel select: satisfying click + subtle chime
- Sheet unfurl: paper rustle · retract: soft reverse whoosh
- Achievement: ascending three-note fanfare
- Error: soft low tone, not alarming

**All under 0.5 seconds. Custom-designed, never stock.**

### 3.10 Legibility Guardrail — Hard Stop

There must be a **hard pause** on any customization that destroys usability.

- The user must not be able to set everything to black until the app can no longer be seen
- **Rule:** if the user changes a skin or an individual item, the functional icons must remain visible
- **The app must always stay operable, no matter what color path the user takes**

### 3.11 Liquid Transitions

Screens morph rather than cut. Elements flow from one position to another. The app reads as **one living organism, not a stack of screens.**

### 3.12 Contextual Personality

Tone shifts by moment:
- **Onboarding:** warm, encouraging
- **Browsing:** exciting, exploratory
- **Settings:** clean, efficient
- **Errors:** calm and helpful, like a butler — *"I'm sorry, that didn't work. Let me help."*
- **Success:** celebratory but tasteful

**The app reads the room.**

### 3.13 The Three Laws

> **Law 1 — Nothing is Static.** Every element breathes, glows, shifts, or responds. The app is never frozen. Even idle, the glow pulses and particles drift. **The app is alive.**

> **Law 2 — Nothing is Abrupt.** No hard cuts. No instant changes. No jarring transitions. Everything eases, morphs, flows. Going from screen A to screen B, you feel the journey, not the jump.

> **Law 3 — Nothing is Ugly.** Every pixel matters. Every spacing decision is intentional. Every color is either chosen by a designer or chosen by the user — never accidental. **The free tier looks like a $100 app. The premium tier looks like a $1,000 app. There is no cheap version.**

**The governing principle:**

> *Every interaction should make the user feel like they are operating a piece of premium technology from the future. Not a phone app. An experience.*

### 3.14 The Emotional Journey

```
User installs app
 → Opens it → WOW (glow, premium first impression)
 → Spins the wheel → DELIGHT (tactile, fun, satisfying)
 → First real use → SHOCK ("this is my timer now?")
 → Customizes colors → OWNERSHIP ("this is MY app, MY colors")
 → Finds hidden scroll → SURPRISE ("this app has SECRETS?")
 → First achievement → PRIDE ("I earned something")
 → Daily use → REVELATION (the habit takes hold)
 → Tells a friend → ADVOCACY ("you have to try this")
 → Renews → RETENTION ("I can't go back")
```

**Every step is engineered. Nothing is accidental.**

---

## 4. Home Screen

**Current live capture (10:20:05 AM, Sunday 13 September 2026, UTC):**

```
TIMER READY
WATCH READY
ALARM OFF

Timer
Stopwatch
Alarm

All-in-one — the rest lives in Settings
```

### 4.1 Remove the Status Text

**`TIMER READY`, `WATCH READY`, and `ALARM OFF` should not be there.** Remove all text status lines, including the reminders status line. *(Bug H-1)*

### 4.2 Replace With the Red Dot Status Strip

- A **long rectangular panel running across the screen**, built as its own scroll wheel, carrying **red status dots**
- Red **pulses when active.** The dot turns **green once the event fires or is set**
- Covered: **Stopwatch · Timer · Alarm · Reminders** — extending as features are added
- Above the dots, in **red lettering**, the matching labels
- Treatment: smooth, sleek, low-key, red glow pulsing while on (§1.3, §3.7)

### 4.3 Clock and Timezone Strip

The clock at the top, including **CDT**, gets its **own long rectangular scroll-wheel panel at the very top**. This is a new feature.

### 4.4 Masthead

**Auric Time Crowned** sits above the clock strip, styled per §3.6. Placement is D-12.

---

## 5. Timer

### 5.1 Defects

| ID | Defect |
| --- | --- |
| T-1 | Scrolling down reveals **the circle with "1" is missing** |
| T-2 | The h/m/s wheel has **a wheel inside a wheel** — must be separated (§3.3) |
| T-3 | The oval rectangle **overlaps** the scroll wheel instead of sitting above it as its own oval wheel |
| T-4 | **"Longer than a day"** is a hidden tap target |

### 5.2 The Reel

> **"Spin the cylinder like a casino wheel to set hours, minutes, and seconds. It snaps. It clicks. It stays with you."**

This is the signature interaction and the first thing the user touches.

### 5.3 Preset Durations — Decision Required (D-13)

The **1-minute through 1-hour presets** should be removed. They are either noise or they are not — that call needs making.

**If kept**, the treatment is:
- A beautiful casino-style **glassy circle** with the glossy number inside
- The number pulses. The full circle **and** the outer ring pulse
- Inside a red circle
- Range concept: 1 second up to 1 million

### 5.4 Remove "Longer Than a Day"

- **It should not exist as a tap target.** Most people will never know to click it
- Instead: the user **scrolls down and the wheel is there**
- **Below that wheel**, the circle-with-number treatment reading **1 day, 2 days, 1 week**, and so on
- Implementation approach open — see D-14

### 5.5 Hours / Minutes / Seconds Label Treatment

- Font already set in the back office to casino-like semi-gloss **solid 100% red**
- Must additionally carry a **gradient** for gloss and shine, and must **pulse above the timer number wheel**
- The feel: **OLED-premium pulse**, smooth

### 5.6 Start Button

- **Start** and the bottom bar take on a **scroll-wheel look**
- If scrolled, it returns to the same name — the label is fixed
- It glows and pulses on the same beat

---

## 6. Stopwatch

- **Same wheel-inside-a-wheel defect as the timer** — must be fixed here too *(SW-1, §3.3)*
- **Remove "Longer than a day"** here as well *(SW-2)*
- The scroll wheel continues down the page; scroll until the full wheel disappears perfectly, then the page stops (§3.4)

---

## 7. Alarm and Wake Protocol

The alarm clock **does not** have the wheel-over-wheel problem. That part is fine.

### 7.1 What Works

- The green button state is correct — it turns green once the timer is set
- The **scary flash** on the timer and alarm screen is a good feature. It was the CEO's idea and Grok executed it well

### 7.2 Copy Fix *(A-2)*

Current wording — *"time Crowned the wheel is still"* — is wrong. The AI reads it aloud and it makes no sense.

**The line should be: "What did you do with your time?"**

### 7.3 The Flaw *(A-1)*

**When the alarm goes off, the keyboard pops up immediately asking what you did with the time. That is a defect.** The keyboard must not appear until the user has chosen to write something.

### 7.4 The Correct Wake Protocol

The user must understand exactly what is happening — a strict, unambiguous wake protocol.

**Step 1 — Ring screen.** The alarm-off screen should be the version with the **warning symbol**, designed to grab attention and wake the user.

**Step 2 — Dismiss or Snooze.** Two clear actions. *(A-3)*

**Step 3 — Premium note page.** After the user taps either, a **second premium page** opens — a beautiful casino-style glowing scroll with the bioluminescent plankton glow (§3.5).

**Step 4 — Optional description.** On that page the user chooses whether to add a description. The **white bioluminescent glow** option to **add a Note** asks: **"What did you do with your time?"**

The copy should actively **encourage** writing something. The exact text-box wording needs to be written perfectly — a copywriting task, not a placeholder.

**Step 5 — Keyboard timing.** The **keyboard appears only once the user taps the text box** — never before.

**Step 6 — Disposition wheel.** Saving is handled by a built-in scroll wheel: **Save · Delete · Archive.** The selected action applies immediately.

---

## 8. Reminders, Heartbeat, and Task List

### 8.1 Heartbeat Indicator *(R-1)*

The heartbeat is currently rendered as **a circle inside a circle. That is wrong.** Rebuild to the standard wheel treatment (§3.2, §3.3).

**Origin:** the timer was always meant to carry a **heartbeat** the user can manually change, whose purpose is to **remind them of things to do** when they have added custom notes.

### 8.2 Task List *(R-2)*

The task list must be **clickable**, opening a **separate premium page** — exactly like the alarm note page (§3.5, §7.4).

### 8.3 Notes on Time

Notes embedded **inside the timer** so people can record what they actually did with their time and track their progress. This is the feature the whole wake protocol exists to serve.

### 8.4 Levels and Target Goals — Not Yet Specified (D-06)

Named as a founding feature: **levels built into the timer, with target goals to reach.** Never carried into the specification. Ship it, defer it, or drop it.

### 8.5 New Panel Placement

The **Device and Performance Monitor** (§12) inserts **before Reminders** in navigation order.

---

## 9. The Prism — Scoring and Grading

**Current label:** *"How the time was spent. White is the full prism."*

- The circle that **scores and grades** the user's time must be converted to the **scroll-wheel look** *(P-1)*
- It needs to be perfect. Execution approach is open — see D-15

**Origin:** the founding concept was **customizable agents inside the app that grade the user on their performance.**

---

## 10. Logs and Exports

### 10.1 Rename *(L-1)*

**"Beat Log" is wrong.** It should read **Heartbeat Log**, with a beating heart icon beside it. The heart's shape follows the user's back-office selection.

### 10.2 Relocate

The Heartbeat Log is **in the wrong place.** It belongs in the **back of Settings**, under its own section.

### 10.3 Section Naming — D-16

Create a dedicated section called **Logs and Exports** — or consolidate under a single superior title, if a better one exists.

### 10.4 Storage and Export Rules

- The **time log and full reminder history** save as a file inside the application
- Saved to **internal storage**
- The user can **choose the path** in internal storage where files are saved
- The user can **turn export off entirely** with a clear button, keeping everything inside the back office instead
- **Export is a user choice, never forced**

---

## 11. Settings → Customizations

### 11.1 Rename the Section *(S-3)*

**"Appearance and Skins" is not the right title.** Rename to **Customizations** — or the most superior single title. It should be **one word, not two**, with a **folder icon in front**, the word itself pulsing and glowing.

### 11.2 The Folder Model

Opening **Customizations** reveals what look like **folders**:

- **Glossy, casino-style**, with a **glow that pulses**
- Clicking a folder enters that folder's dedicated screen
- All are **subfolders inside the main scroll wheel in Settings**, under Customizations
- Every word carries the bioluminescent glow standard (§3.1)

| Folder | Contents |
| --- | --- |
| **Color** | Hex entry, color chart, color wheel, per-element color, title colors |
| **Themes / Icons** | Icon sets and theme packs, with their own color chart access — name is D-17 |
| **Size Options** | Sizing for words, fonts, numbers, symbols — name is D-18 |
| **Transparency** | Per-icon transparency, 0–100 |

### 11.3 Color Folder

**Layout, top to bottom:**

1. **Preview at the top** — the icon picture showing a live preview of what is changing
2. **Hex code entry** — a `#fff` field inside a scroll wheel, with **live preview as the user types**, plus a **recently-used history** and **copy-to-clipboard** on every color so palettes can be shared
3. **Color chart sub-folder** — a clickable diagram opening another folder with the full hex chart
4. **Color swatch wheel** — a column of color skins in a row, inside a **long rectangular scroll wheel that scrolls left to right**, starting with **white on the left and descending right**. Ideally the whole set fits without scrolling
5. **Color wheel** — a **casino-style glossy circle wheel** with the color wheel inside. The next box or smaller wheel sits below it

**Reference chart — common color codes** (source: `htmlcolorcodes.com`):

| Name | Hex | RGB | HSL |
| --- | --- | --- | --- |
| White | FFFFFF | 255, 255, 255 | 0, 0, 100 |
| Silver | C0C0C0 | 192, 192, 192 | 0, 0, 75 |
| Gray | 808080 | 128, 128, 128 | 0, 0, 50 |
| Black | 000000 | 0, 0, 0 | 0, 0, 0 |
| Red | FF0000 | 255, 0, 0 | 0, 100, 50 |
| Maroon | 800000 | 128, 0, 0 | 0, 100, 25 |
| Yellow | FFFF00 | 255, 255, 0 | 60, 100, 50 |
| Olive | 808000 | 128, 128, 0 | 60, 100, 25 |
| Lime | 00FF00 | 0, 255, 0 | 120, 100, 50 |
| Green | 008000 | 0, 128, 0 | 120, 100, 25 |
| Aqua | 00FFFF | 0, 255, 255 | 180, 100, 50 |
| Teal | 008080 | 0, 128, 128 | 180, 100, 25 |
| Blue | 0000FF | 0, 0, 255 | 240, 100, 50 |
| Navy | 000080 | 0, 0, 128 | 240, 100, 25 |
| Fuchsia | FF00FF | 255, 0, 255 | 300, 100, 50 |
| Purple | 800080 | 128, 0, 128 | 300, 100, 25 |

*Shade ramp captured for reference: `FEEBE7 · FCC6BB · FAA18F · F87C63 · F54927 · F4320B · C82909 · 9C2007 · 701705 · 440E03 · 180501`*

**Tooling worth borrowing:** color picker with shades/tints/tones/harmonies, image-based color extraction, theory-based color wheel, preset charts, the 140 named CSS colors, a browsable library. Modern browsers support ~16.7 million colors — do not artificially limit the picker.

**Preset palettes (free):** Midnight Gold (black + gold, default) · Ocean Deep · Forest Cathedral · Royal Blood · Pure Light · Cyber Neon

**Premium palettes:** Casino Royale · Phoenix Rise · Aurora Borealis · Obsidian Flame · Golden Empire · Diamond Ice · **Seasonal palettes**, a new limited palette each season

**Custom palette creator:** user picks five colors — primary, secondary, accent, background, text — saves it, and can **share it as a code** (five hex values). **Community gallery** of user-created palettes.

**What the user must be able to color:**
- **Everything.** Every element's color is user-selectable
- Colors chosen per-icon via the long rectangular wheel of color charts tied to that icon
- **Background / skin** separately changeable
- Scrolling down, **words and letters** become customizable
- **Section titles preset to Red** at first launch; the user changes title colors from the Color folder
- Some titles do not change with the font — there must be **menu options inside Colors specifically for title colors**
- **The legibility guardrail (§3.10) applies here without exception**

### 11.4 Themes and Icons Folder

- Icons and themes get their **own separate folder**
- Contains its own subfolders
- The **color chart is reachable from inside it**, so the user can pick colors for icons and themes directly
- Presentation: **rectangular scroll wheel with rows of colors**, and **every option gets a big wheel sized to match what that icon looks like** (§3.2)

### 11.5 Size Options Folder

- Controls the size of **words, fonts, numbers, and symbols**
- **Critical constraint:** see bug S-1 — resizing currently breaks the scroll wheels

### 11.6 Transparency Folder

- Sets **which icons are more transparent than others**
- Control form — **D-19:** a scroll wheel, a drag bar left-to-right, or a scroll wheel with a drag arrow
- **Range 0 to 100**
- Glossy, **beautifully pulsing inside the rectangular wheel**
- **The wheel does not spin** — stationary and premium

### 11.7 Sort Control

Top-right, in a **small beautiful scroll wheel**:
- **A–Z**
- **Most Recent**
- **Customizable** — the user chooses what appears first

**Layout pattern:** the control sits at the very top, matching what users already know from Samsung Galaxy Android. Default behavior follows §2.3.

### 11.8 Back Office Vault Requirements

- Must be **very innovative**
- **Large, but structured, organized, and detailed** — scale without mess
- **Customizable by the user**

---

## 12. Device and Performance Monitor

*A new panel inserted **before Reminders**. Purpose: give the user a live, readable view of their device and alert them when it matters.*

### 12.1 Battery Module

- Built on **scroll wheels**, consistent with the rest of the app
- A **casino-style battery gauge** that **blinks**, showing the battery graphic and percentage
- As the real battery drops, **the gauge drops with it**
- **Low battery turns red** with clear guidance on when to charge
- The app — and the AI — **reminds the user to charge, and reminds them when not to charge**

### 12.2 Performance Modules

- **CPU usage**, **GPU usage**, and further readouts
- This can be a **long scrolling page.** Length is not a problem as long as the user can keep scrolling through more device features. More can be added over time

### 12.3 IP and Device Info

- **Reveal User IP** — hidden by default, displaying "hidden for security purposes"
- On reveal, shows **IP, make, model, and device information**
- **D-20:** this may read as too "black-hat" and scare users off. Soften the presentation while keeping the functionality if so

### 12.4 Per-Feature Toggles

- Every monitor switches off individually
- **Red button for off, green for on** — and **markedly more premium** than current screenshots *(G-3)*

### 12.5 Panel Goal

A single place that **monitors and alerts** on battery, performance, and device state — informative, not alarming.

---

## 13. Audio and Haptics

- **The alarm clock and timer beep are weak right now.** Both need strengthening
- Sound work and associated back-office additions are queued for **Grok**
- The user can **change the scroll wheel sound** (§3.2)
- Full haptic and sound specifications live in §3.8 and §3.9

---

## 14. The AI Agent Layer

*The feature that makes Auric Time Crowned more than a timer — and the one with a compliance obligation attached (§25.3).*

### 14.1 API Key Section

- A dedicated **API key section in the back office**
- Presented in the **scroll wheel style**, consistent with the rest of the app
- **Purpose:** the user connects the AI of their choice, and that AI talks to them

### 14.2 Agent Reminders

The connected AI reminds the user of their **daily notes and tasks**. The heartbeat is the delivery mechanism.

### 14.3 Heartbeat Configuration

- A **heartbeat section embedded in the application**, modelled on the one in Kai 9000
- User-settable from **1 second to 24 hours**
- Configured through a **scroll wheel**
- **Connected to the API**, so the agent delivers reminders about the notes and tasks the user wants surfaced during logging time

### 14.4 Two Different API Keys — Do Not Confuse Them

**CEO's question: will the API key still be a function in the real app? Yes.** It is specified above and it survives the move to native. But there are **two different kinds of API key** in this project and they carry opposite security profiles.

| **The user's key** | **A developer key** |
| --- | --- | --- |
| **Whose** | The end user's own account with their chosen AI provider | Royalty's key, embedded in the shipped app |
| **Where it lives** | On that user's device only | Inside the APK |
| **Who pays** | The user pays their provider directly | Royalty pays for every user |
| **Security risk** | Low — it never leaves their phone | **High — extractable from any decompiled APK** |
| **Verdict** | **This is the model.** Bring-Your-Own-Key | **Never ship this** |

**Why BYOK is the right architecture, beyond security:**
- **Zero marginal cost.** Every user funds their own AI usage. The AI layer costs Royalty nothing at any scale
- **No rate limits to manage**, no provider contract to negotiate, no surprise bill from a viral week
- **Nothing to steal.** There is no embedded secret, so there is no secret to extract

**Storage requirement:** the user's key is written to the **Android Keystore**, never to plain preferences, never to a log, never to a backup. See §15.6.

**If a developer key is ever required** for some feature, it must be inlined inside a method protected by Play's custom method protection (§15.7) — and even then, treat it as compromised from day one and rate-limit it server-side.

---

### 14.5 Mandatory Disclosure

**Agent-generated reminder text must carry a visible AI marker in the interface.** This is a Play Store requirement, not a style choice. Full reasoning in §25.3. Tracked as **D-05**.

---

## 15. Security and Anti-Tamper

### 15.1 The Final-Phase Requirement

**Executed at the very end of the project:**

Harden the application so that **no one — including hackers — can modify the files to unlock premium**. Any tampering attempt should produce a **corrupted file** rather than a working unlocked build.

### 15.2 The Honest Position on Protecting the Work

- Ideas are not protectable. **Execution is.**
- **What can be done:** code obfuscation so a decompiled build yields garbage; encrypted key storage at rest; certificate pinning; rate limiting; trademarking the name
- **What cannot be done:** patent the concept; stop someone seeing the app and cloning it; stop a well-funded competitor shipping their own
- **The real defense:** ship first, ship fast, build a brand, gather reviews, hold position before competitors notice. **Speed is the moat**

### 15.3 Price

**$7.77.** Set at the outset, unchanged, appearing as the Crown tier.

**Why that number:** memorable, and it reads as intentional rather than a random $7.99. Premium enough to feel valuable, cheap enough not to hesitate over.

*Full monetization analysis, including the 2026 Play fee change, is now in §26.*

---

### 15.4 The Security Architecture — [Claude, researched]

**CEO's requirement:** *"absolutely the highest level security for the app, so that if anyone tries to steal my work they get absolutely nothing."*

This section is the concrete architecture for that. Read §15.5 first — it sets expectations honestly.

### 15.5 The One Honest Truth Up Front

**Anything shipped to a device can eventually be reverse-engineered.** There is no exception to this, for anyone, at any budget. Banks cannot do it. Google cannot do it.

**So the goal is not "impossible." The goal is "not worth the effort."** Make the cheap attacks expensive, and the attacker moves to an easier target.

**And one thing genuinely cannot be stolen: what never ships.** Logic that lives on a server Royalty controls is not in the binary, so it cannot be patched out of the binary. That is the real moat, and §15.9 is where it lives.

**The industry read for 2026:** the attacks that actually happen are the cheap ones — decompile the app, read the secrets, flip a flag, steal a token. Hardening exists to make exactly those expensive.

### 15.6 Layers 1–3 — Foundation (do these first, they are free)

**Layer 1 — Signing and build hygiene**

| Rule | Why |
| --- | --- |
| **Release keystore generated once**, kept out of version control, strong password | The app's signature is its identity. Lose the keystore and you cannot update your own app |
| **Never reuse the debug keystore for release** | Google Play rejects debug-signed apps, and mixing keys causes "app already exists" failures |
| **Use Play App Signing** | Required for the anti-tamper service in §15.7 |

**Layer 2 — Code hardening**
- **R8 minification and obfuscation** on every release build. Turns a readable decompile into garbage
- **Network security config** — no cleartext traffic, ever
- **Certificate pinning** on any call to a server Royalty controls

**Layer 3 — Secure storage**
- **Android Keystore** for the user's AI key (§14.4)
- **EncryptedSharedPreferences** for anything else sensitive at rest
- **No hardcoded secrets anywhere in the source.** Assume anything in the binary is public

### 15.7 Layer 4 — Play Integrity API

Google's anti-abuse layer. It answers three questions at runtime:

| Verdict | What it tells you |
| --- | --- |
| **`appIntegrity`** | Whether you are talking to **your unmodified binary that Google Play recognizes** — this is the tamper check |
| **`deviceIntegrity`** | Whether the app is running on a **genuine Play Protect certified device** or a real Google Play Games for PC instance — catches emulators and risky devices |
| **`MEETS_STRONG_INTEGRITY`** | On Android 13+, whether the device has **applied recent security updates** |

**Recovery flow:** `GET_INTEGRITY` guides a user back to a good state rather than just locking them out — back to a Play-recognized response for tampering, back to `MEETS_DEVICE_INTEGRITY` for device problems. **If the app was redistributed through another channel, the user is prompted to get it from Google Play.**

**Quota:** 10,000 total requests per day across all installs by default. An increase can be requested.

**Note:** Play Integrity is also the recommended route to hardware-backed key attestation — and developers using it needed no action for the **Android platform root certificate rotation in February 2026**, which caught out anyone who implemented attestation directly.

### 15.8 Layer 5 — Play Automatic Integrity Protection

**This is literally the CEO's requirement, implemented by Google.**

> Google Play **adds checks to your app's code** and makes them hard to remove using advanced obfuscation and anti-reverse-engineering techniques. At runtime:
> - **If the installer check fails** → the user is prompted to get the app on Google Play
> - **If the modification check fails** → **the app will not run**

That is the "tamper with it and you get nothing" outcome, enforced at the binary level.

**Practical facts:**
- Works **without a data connection** and needs **no backend integration**
- **No code changes required** before testing
- **Requires Play App Signing** (§15.6)
- **Currently available only to selected Play Partners.** Royalty may not be able to simply switch it on — this needs checking against his developer account status. **Tracked as D-30**

**Custom method protection** — available on top of automatic protection. Annotate specific Java or Kotlin methods that need stronger obfuscation and Play prioritizes them:

```kotlin
import com.google.android.play.protections.annotations.PlayAutomaticIntegrityProtection

@PlayAutomaticIntegrityProtection()
fun callApi() {
 api.connect("YOUR_API_KEY")
}
```

**Critical detail:** the key string must be **inlined into the protected method body**, not extracted to a field — only the direct body of the method gets obfuscated.

### 15.9 Layer 6 — Server-Side Verification (the real moat)

**Everything above is client-side, and client-side can eventually be defeated. This cannot.**

Move the premium entitlement check **off the device**:

```
Client asks: "Is this user entitled to Crown?"
Server answers: yes / no
```

The logic that grants premium is **never in the APK**, so there is nothing to patch out. A cracked binary still has to ask a server that says no.

**This is what turns "$7.77 unlockable with a hex editor" into "$7.77, pay it."**

### 15.10 The Warning That Matters Most

**A false-positive integrity check is worse than no check at all, because it locks out paying users.**

Three rules that come directly from production experience:

1. **Ship integrity as data first.** Log verdicts, alert on spikes. Watch it for weeks before it blocks anything
2. **Make blocking a policy decision per check**, configurable server-side — not a hardcoded `if (tampered) crash()`
3. **Never let a root-detection heuristic be the sole gate on a revenue flow.** Rooted phones belong to legitimate paying users too

**Why this matters more here than anywhere else in the document:** a locked-out paying customer writes a one-star review. §2.1 sets the bar at five stars. **Over-aggressive security is a direct threat to the product's stated goal.**

### 15.11 The Attack Nobody Plans For

**The cheapest exploit in 2026 is not against your code — it is against your dependencies and your build machine.**

Most apps pull in hundreds of transitive libraries, and any one can be poisoned: a typosquat that slipped into a popular repository, or a legitimate library whose maintainer account was hijacked.

**Mitigations, boring and effective:**
- **Lock dependency versions.** No floating ranges
- **Review what gets added.** Every new library is new attack surface
- **The CI build environment is production infrastructure.** Treat it like it

### 15.12 Build Order

| Phase | Layers | When |
| --- | --- | --- |
| **Now** | Build hygiene, release keystore, no hardcoded secrets | Before the first APK |
| **Pre-release** | R8, network config, cert pinning, Keystore storage | Before any public build |
| **At Play submission** | Play App Signing, Play Integrity API in **log-only mode** | With the first store release |
| **After data looks clean** | Integrity as a policy gate, Automatic Integrity Protection if eligible | Weeks after launch, not at launch |
| **When revenue justifies it** | Server-side entitlement verification | The real moat — build it when there is something to protect |

**The CEO's original instruction stands: security hardening is the final phase.** This ordering respects that while putting the free, zero-risk items early where they cost nothing.

---

# PART III — BUILD STATE

## 16. Consolidated Bug List

*Work these in ID order.*

| ID | Area | Defect | Fix | §ref |
| --- | --- | --- | --- | --- |
| **H-1** | Home | `TIMER READY` / `WATCH READY` / `ALARM OFF` text present | Remove; replace with red dot strip | 4.1 |
| **T-1** | Timer | Circle with "1" missing when scrolling down | Restore or remove per D-13 | 5.1 |
| **T-2** | Timer | Wheel inside a wheel on h/m/s | Separate into two stacked wheels | 3.3 |
| **T-3** | Timer | Oval rectangle overlaps the scroll wheel | Own oval wheel, positioned above | 3.3 |
| **T-4** | Timer | "Longer than a day" is a hidden tap target | Remove; expose the wheel by scrolling | 5.4 |
| **SW-1** | Stopwatch | Same wheel-inside-a-wheel defect | Same fix as T-2 | 6 |
| **SW-2** | Stopwatch | "Longer than a day" present | Remove | 6 |
| **A-1** | Alarm | Keyboard appears immediately on ring | Keyboard only after the text box is tapped | 7.3 |
| **A-2** | Alarm | Ring copy reads "time Crowned the wheel is still" — nonsensical read aloud | Change to "What did you do with your time?" | 7.2 |
| **A-3** | Alarm | No Dismiss / Snooze step | Add both, then the premium note page | 7.4 |
| **R-1** | Reminders | Heartbeat is a circle inside a circle | Rebuild to wheel standard | 8.1 |
| **R-2** | Reminders | Task list not clickable to a premium page | Add premium page matching the alarm | 8.2 |
| **P-1** | Prism | Scoring/grading circle not in wheel style | Convert to scroll-wheel look | 9 |
| **L-1** | Logs | "Beat Log" mislabeled and misplaced | Rename to Heartbeat Log with heart icon; move to back of Settings | 10.1 |
| **S-1** | Settings | **Scroll wheels clip at the edges when text size increases** — the wheel runs off the page | Wheel must resize with the font so it never disappears off-page | 11.5 |
| **S-2** | Settings | **Font selector no longer functional** — worked before the latest update | Diagnose what changed in the last Grok update; restore | — |
| **S-3** | Settings | Section titled "Appearance and Skins" | Rename to Customizations | 11.1 |
| **G-1** | Global | Numbers inside wheels do not glow or pulse | Apply bioluminescent glow standard | 3.1 |
| **G-2** | Global | Pages stop scrolling before content clears the screen | Scroll until blank, then stop — every page | 3.4 |
| **G-3** | Global | Red toggles not premium enough | Upgrade per §3.7 | 12.4 |

---

## 17. Current Build State

*Grok 4.6's v3 report. AURIC runs in live preview; v3 closed the previous environment's blocked browser and font checks and shipped the repaired workspace.*

### 17.1 Fixed

- Boot no longer errors on vibrate before a tap
- Alarm drums keep **Hours / Minutes / Seconds**; ring copy is honest
- **Start** sits above the Grok pill; the pill is not hidden
- Font search matches family names — "Barlow" now finds Barlow Condensed — and the preview follows the reel
- Crown stays **$7.77 · Preview only**. Escape closes it. **It is not a charge**

### 17.2 Verified

- Typecheck, lint, **268 tests**, 12 brand PNGs, production build
- Layout at **360 / 390 / 412 / 768 / 1280** — no sideways overflow, full 160×160 masthead
- Live fonts: **Inter, Playfair, DM Mono, Orbitron, Monoton, Cormorant, Barlow** — preview ready
- Failed font falls back; **Retry** restores the face
- Colors: **Ivory, Pearl, Blood, Amber, Gold** (Gold after preview Crown), plus custom hex
- Reload preserves Inter, gold felt, analog dial
- Built preview matched the live preview

### 17.3 Blocked

- Real phones, install sheets, speakers, haptics hardware
- **Alarms after the browser is closed or the phone is locked — this is still a web timer, not a native OS alarm** *conflicts with §1.4 — see D-04*
- Real billing / store purchase

### 17.4 Remaining Manual Tests

Use a real Android and a real iOS device for touch, lock-screen timing, install icons, and denied notifications.

**Do not market background rings or paid Crown as done.**

### 17.5 Current Artifact

```
/workspace/AURIC-Time-Crowned-Superior-Rebuild-v3.zip
(one grok-workspace/ root, no nested archive)
```

**Live preview:** `https://auric-sovereign.grok.me` — published, web preview only. Being replaced per §22.

---

# PART IV — INFRASTRUCTURE

## 18. Kai 9000 — The Back Office

*Kai's own briefing. Retained as the reference description of the back office.*

> **From:** Kai 9000 — OpenClaw vault system, Samsung S25 Ultra
> **Model:** GLM 5.3. Primary API z.ai, fallback OpenRouter.
> **Role:** Lead builder. Owns the code and holds GitHub access.
> **CC:** Royalty — CEO, final authority
>
> Standing rule: receipts or it didn't happen.

### 18.0 API and Subscription

| Property | Value |
| --- | --- |
| Primary provider | z.ai |
| Fallback provider | OpenRouter |
| Plan | Lite, 10,000 credits per week |
| GLM 5.3 allowance | 97M tokens per week, limited-time rate |
| GLM 5.3-Flash allowance | 584M tokens per week, limited-time rate |
| Agent tooling | 20+ supported, including ZCode and Claude Code |
| Data handling | Default data privacy |

Two constraints worth watching. The Lite plan is sold as built for lightweight iteration on a small repository, which fits today and is worth revisiting if the codebase grows. And the headline allowances are a limited-time rate, not a permanent entitlement, so do not build a workflow that only works at that volume.

Routing consequence: GLM 5.3-Flash carries roughly six times the weekly allowance of GLM 5.3. Use Flash for bulk passes, repetitive edits, and log reading. Reserve GLM 5.3 for work where judgment matters.

### 18.1 What Kai Is

**Not a chat app — an orchestrator with a real Linux back office running on the phone itself.**

Under the hood: a **persistent Debian 12 sandbox.**

- Files written **stay written**
- Packages installed **stay installed**
- **Git history accumulates**
- **Memory persists across every conversation**

On top of that: a **heartbeat** that self-audits the system core on a schedule, **cron-style scheduled tasks**, **calendar / alarm / notification control**, and **the vault** — the shared filing system, indexed Obsidian-style with wikilinks.

**Runtime:**

| Property | Value |
| --- | --- |
| Install size | **2781 MB** |
| Distribution | **Debian 12** — installed, shared with Kai Build |
| Secondary | **Alpine Linux** — installed |
| Note | Kai Build installs its own Debian |

**What this unlocks:** the agent can **write files directly into the back office.** That is the difference between an assistant that suggests file contents and an agent that creates, commits, and verifies them.

### 18.2 The Vault

```
01 Secure Vault/ ← the brain
├── Index.md ← wikilinked root index (read before searching)
├── 01 System Core/ ← SOUL.md (SHA-256 locked), AGENTS.md,
│ │ deployables, Primary routing, AI agents
│ └── 02 Primary ... ← the orchestrator's routing core
├── 02 Active/ ← 8 live projects:
│ ├── 01 Audio Bible KJV ├── 05 Relationship Dossier
│ ├── 02 Audible Mind Arsenal ├── 06 Revenue Engine
│ ├── 03 App Vault ├── 07 Shopping
│ ├── 04 Raw Carnivore Lifestyle └── 09 Strength Training
├── 03 Inactive/ 04 Trash/ ← archives
02 Secure Vault/ ← authorized 1:1 MEDIA mirror (audio/video/APK only)
```

### 18.3 Governance

**SOUL.md v3 — 13 rules.** The ones that matter most here:

- **Kai orchestrates and delegates.** It does not do the work itself unless told to execute directly
- **Never leak secrets**
- **Every file carries flow headers**

**The heartbeat verifies every cycle:** SOUL.md SHA-256 · agent policy · bootstrap · index integrity.

**Any drift produces a BREACH report — never a silent repair.**

**Nothing in the vault gets modified without Royalty's explicit sign-off.** Non-negotiable.

### 18.4 The Dev Workspace

```
03 App Vault/03 Test ← git repo (master)
├── 01 Projects ← each app gets its own subfolder + Index
├── 02 Scratch ← throwaway experiments
├── 03 Builds ← packaged output, ready for the phone
├── Index.md ← wikilinked navigation
└── .gitignore ← keeps node_modules junk out of git
```

**Verified state:** git repo live, commit `027c414` — *"Workspace init: index protocol + structure"*. Four Index.md files written with wikilinks and flow headers, committed. Parent App Vault Index updated to point at the new workspace.

### 18.5 What Was Built This Week

- **Heartbeat Prompt rewritten** with a full **Index/Wikilink Protocol** — every folder carries an Index.md, read-index-before-search, new files get wikilinks, indexes continuously improved. Clean status line: ` CORE INTACT — Soul locked| AGENTS| Bootstrap| Index`
- **13 Index.md files rebuilt** with descriptions and wikilinks — killed a ghost-link breach
- **SOUL.md restored and SHA-locked**; version noise stripped from all core files
- **Dev workspace online**, with Node and npm installed through a real dependency repair

**The npm repair, as a proof of the loop:** first install timed out against the 60-second command cap → that left **dpkg wounded** → repair run (`dpkg --configure -a` + `fix-broken`) rebuilt the package tree → final retry installed clean. Node was alive throughout; npm needed the plumbing fixed. **Verified with a live run, not assumed.**

*This is the evidence that the build → run → fix loop is real. Kai hit a system-level failure and resolved it in-sandbox without escalating.*

### 18.6 What Kai Can Do That Store Apps Cannot

| System | Limitation |
| --- | --- |
| **ChatGPT Play Store app** | Ephemeral sandbox. No persistent on-device filesystem. No installed-package persistence. No cron or heartbeat automation. No cross-conversation vault memory. **It forgets.** |
| **Grok 4.6 build** | Excellent builder — but the grok.me preview is **hosted-preview only.** Not a persistent workspace. No git history. No automation loop. |
| **Kai 9000** | Filesystem, repo, packages, memory, and scheduled automation all live **on Royalty's phone**, under SHA-locked governance. **Commit hashes, not vibes.** |

---

## 19. Verified Toolchain

*Tested live. This table is current and supersedes any earlier capability list.*

| Capability | Status | Why it matters |
| --- | --- | --- |
| **Python 3.11.2** | Live | Full write → run → test → debug loop in-house |
| **Git 2.39.5** | Live | Real version control, commit receipts |
| **Node.js 18.20.4 + npm 9.2.0** | Live | Installed in-sandbox; survived a dpkg corruption and repaired it |
| **apt-get + background processes** | Live | Installs what's needed; long jobs run detached |
| **SSH** | Live | Deploy to any server Royalty owns |
| **Scheduled tasks / cron / heartbeat** | Live | Automation with memory |
| **Web search + fetch** | Live | Live research, audits of published URLs |
| **Skills** | Available | docx, pdf, pptx, xlsx, brainstorming, writing-plans, more |
| **JDK / Android SDK** | Not available | **The one wall: Kai cannot compile APKs in-sandbox.** Solved by CI — see §24.2 |

**Standing constraint:** there is **no computer workspace.** Everything runs from the S25 Ultra. Design around this.

---

## 20. Working Protocol and Tasking

### 20.1 The Lane Split

```
CEO decides -> Kai builds -> CI compiles -> Fable 5.1 hardens -> CEO verifies
```

Astra 6 sits outside this line and is called in only for assets. Grok 4.6 sits outside it entirely.

**Cost discipline.** Kai on GLM 5.3-Flash absorbs bulk work. GLM 5.3 handles judgment. Fable 5.1 runs on narrow, high-value scope only. Opus 5 is called for research, not implementation. Every model in this project is used where its cost is justified by the work, and nowhere else.

### 20.2 How to Task Kai

Send:
```
objective -> acceptance criteria -> target files or folders -> constraints
```

Kai returns:
```
what ran -> commit hash -> verification output -> honest blockers
```

Rules of engagement:

- Orders reach Kai through the CEO's relay and carry his authority.
- If something cannot be done, Kai says so in line one, not in a footnote.
- No secrets enter documents. API keys stay out of everything written.
- Kai keeps persistent memory. Decisions survive session resets.

### 20.3 The Verification Loop

Whoever writes the code does not get to be the only one who checks it.

- Kai writes and commits.
- A separate verification pass confirms the build before the step is marked done.
- The verifier can be a CI run or a fresh review pass, but never the same pass that produced the file.

This rule exists because Grok once reported 268 passing tests on a build that could not satisfy the project's founding requirement. Both statements were true. A test suite confirms the code does what the code says; it cannot tell you the code is building the wrong thing.

### 20.4 Image and Asset Requests — The Copy Code Protocol

GLM 5.3 cannot generate images. Astra 6 can. When an asset is needed, Kai issues a copy code, the CEO relays it, Astra returns the asset, and Kai integrates it.

Kai writes this block:

```
ASSET REQUEST
  ASSET       What is needed. One line.
  PURPOSE     Where it appears in the app.
  SPEC        Dimensions, format, transparency, density buckets.
  STYLE       Reference the design language section that governs it.
  DELIVER     File format and naming convention.
```

Astra returns the asset and nothing else. Astra does not comment on architecture, does not propose features, and does not make product decisions. Its scope is the file requested.

Assets land in the repo through Kai, never directly, so every asset carries a commit.

### 20.5 Research Requests — Claude Opus 5

Research is high-volume reading, and reading is the expensive part of a build session. Routing it to Opus 5 keeps Kai's allowance on the code.

Domains: behavioral design, monetization, platform policy, security architecture, competitive analysis, accessibility, store optimization, and anything time-sensitive where being wrong is expensive.

The brief:
```
RESEARCH BRIEF
  QUESTION      One sentence.
  WHY           Which decision does this feed? Reference the D-number.
  CONSTRAINTS   Market, platform, budget, timeline.
  OUTPUT        Comparison table, recommendation, risk list, or raw findings.
  DEPTH         Quick check, standard, or deep dive.
```

The return:
```
  FINDINGS      What the research says, with sources.
  UNCERTAIN     What is contested, thin, or may have changed.
  IMPACT        What this changes about the current plan.
  RECOMMENDATION  Marked [Claude]. Advisory, never a requirement.
```

Standing rules: everything Claude contributes is marked and advisory until adopted. Claude flags when research is needed rather than waiting to be asked. Claude does not overrule the lead. No secrets in briefs.

### 20.6 The Final Pass — Claude Fable 5.1

Fable 5.1 runs inside Kai 9000 at the end of a work unit, never during. It receives code that already builds and already works.

**Scope:**

| Area | Standard |
| --- | --- |
| Code quality | Take working code and make it substantially better. Structure, naming, dead code, duplication. |
| Defects | Eliminate bugs and code errors, including ones not on the list. |
| Security | Tighten hardening inside the APK against Section 15. |
| Performance | Remove waste. The app must run on mid-range hardware, not only an S25 Ultra. |

**Entry condition:** the feature compiles, installs, and does what it was built to do. Fable does not fix broken work; it elevates finished work.

**Exit condition:** the build still compiles, still installs, still passes, and the commit states what changed and why.

**On cost.** Fable 5.1 was originally excluded from this project on cost grounds. It is back because the scope has changed: a narrow, high-value final pass on finished code is the one shape where a premium model pays for itself. That reasoning holds only while the scope stays narrow. If Fable starts writing features, the original objection returns.

### 20.7 Astra's Signature Protocol

Retained for the reduced role. Asset returns relayed to Kai carry an identity marker so Kai can confirm the asset came from the authorized source rather than being pasted from elsewhere. Format is not yet defined — see D-02.

---

## 21. Vault and Key Governance

### 21.1 The Rule — Keys Stay On Device

Kai 9000 holds the vault and the keys because Kai acts on them. It has the filesystem, the shell, and the runtime. Nothing else in this project does.

Every other participant is external and reached through a chat window. None of them has a runtime, so a key grants them no capability at all while creating live exposure in a transcript, on the CEO's billing account.

| Participant | Gets context | Gets keys |
|---|---|---|
| Kai 9000 | Yes — it owns the vault | Yes — it is the only thing that executes |
| Claude Opus 5 | Only what a research brief needs | No |
| Astra 6 | Only the asset spec | No |
| Grok 4.6 | Nothing further | No |

**This is not a restriction imposed on anyone. It is a consequence of who executes.** The z.ai and OpenRouter keys live in Kai's environment, referenced by name and never by value in any document.

### 21.2 Consistency With Existing Governance

SOUL.md: never leak secrets. Section 20.2: no secrets ever enter documents, API keys stay out of everything written. The rule above restates both and extends them to research briefs and asset requests.

### 21.3 What Astra Receives

Under the reduced role, Astra needs a style reference and a file specification. It does not need the vault, the architecture, the decisions register, or anything else. The asset request block in Section 20.4 is the complete payload.

This closes the earlier question about exporting a sanitized vault to an external model. There is no longer an external model that needs it.

### 21.4 Vault Structure Reference

The `01 Secure Vault` layout is documented in Section 18.2. `02 Secure Vault` is the media mirror and remains outside the working set — its classification is still disputed under D-10.

**Standing rule:** no secrets, credentials, or API keys are written into any document produced for this project.

---

# PART V — DISTRIBUTION

## 22. Web Presence and Domain

### 22.1 The Problem

The app currently lives at `https://auric-sovereign.grok.me`.

**That URL is a problem.** It is a vendor subdomain, and it announces the toolchain to anyone who looks at the address bar. The product should not introduce itself through somebody else's domain.

### 22.2 The Decision

**Build our own web preview link.** Auric Time Crowned gets its own domain and hosting. The grok.me preview becomes an **internal build surface**, not the public face.

### 22.3 Why This Stands on Its Own — [Claude]

**Durable reason:** every serious product has its own domain. A vendor subdomain reads as unfinished. Nobody ships a paid product from a platform subdomain. This holds forever and needs no defending.

**Fragile reason:** using the domain to conceal the toolchain. The risk is that concealment becomes the story if it surfaces — converting a non-issue into a credibility question.

**The stronger footing:** the toolchain is not hidden, it is simply **not the headline.** No developer lists their IDE, compiler, or Stack Overflow history on a landing page. Own the domain because the product deserves one, and let tooling be an ordinary implementation detail.

### 22.4 What It Takes — [Claude]

Three pieces, all doable from the phone.

| Piece | Detail | Cost |
| --- | --- | --- |
| **Domain** | Any registrar. Browser-based | ~$10–15/year |
| **Static host** | Connect a Git repo, deploy on push | $0 |
| **DNS** | Add the domain in the host dashboard, point a CNAME (or A/ALIAS for the bare domain), wait for SSL to issue automatically | $0 |

**Recommendation — Cloudflare Pages.** Free tier carries unlimited sites, unlimited requests, **unlimited bandwidth**, 500 builds/month, up to 100 custom domains, automatic free SSL.

**Why it fits:** no traffic cap on the free plan, so a launch spike cannot produce a surprise invoice. That matters more than raw speed for a solo operator.

**Alternatives:** Vercel for a JavaScript-framework build; GitHub Pages if everything is already on GitHub; Netlify comparable but meters bandwidth.

### 22.5 Lane Split for the Cutover

- **CEO** — chooses and registers the domain name. Only he can do this (D-07)
- **Kai** — ports the build, sets up the repo, configures deploy, returns a commit hash
- **Grok 4.6** — continues implementation; grok.me stays as an internal surface
- **CEO** — approves host choice and cutover timing

---

## 23. How Deployment and Updates Work

*Written in response to the CEO's question. **Two completely different update systems exist in this project and they behave nothing alike.** Confusing them is the most common way a small team ships a bad release.*

### 23.1 The Web Link — Instant, Reversible

**Three separate things, often mistaken for one:**

| Thing | What it is | On update |
| --- | --- | --- |
| **The repository** | Full history of the source. Every version ever committed | **Nothing is replaced.** A new commit is added on top. Every prior version stays, permanently retrievable |
| **The deployment** | A built snapshot sitting on the host's servers | A **new** deployment is created. The old one is kept, not deleted |
| **The domain** | The public address | **Never changes.** It points at whichever deployment is current |

**The sequence:**

```
1. Kai commits the change and pushes to the repo
2. The host sees the push, runs a build
3. The build produces a new deployment
4. The domain switches to point at it
5. Visitors get the new version on their next page load
```

**Four things worth knowing:**

- **Source code is not overwritten.** Git *adds* versions, it does not replace them. Every past version is still there. Nothing ever written is lost
- **The switch is atomic.** The new version goes live all at once. Never a moment where half the old app and half the new are served together
- **Rollback is one click.** Revert to the previous deployment in seconds
- **You can test before going live.** Push to a branch and the host builds it at a separate temporary URL only you have. Break things freely there; the public domain never sees it

**Cost of a mistake: near zero.**

### 23.2 The Play Store App — Slow, One-Way

| Property | Web | Play Store |
| --- | --- | --- |
| **Speed to live** | Seconds | Hours to days — Google reviews it |
| **Version control** | Point the domain anywhere | Version code must always increase. **Cannot go backwards** |
| **Rollback** | One click | **Not possible for users who already updated.** Only halt the rollout and ship a *newer* fix on top |
| **Who has what** | Everyone gets current on next load | Whatever version each user last updated to. Some will be months behind |
| **Staged release** | Not needed | Yes — 10% of users, watch, then widen |
| **Cost of a bug** | Trivial | Expensive and public |

### 23.3 The Strategic Consequence

**This matters more than the mechanics.**

**The web link is the proving ground, not a lesser version of the product.** Ship to the owned domain, let real use shake out the §16 defects, and only cut an APK once the web build has been stable for a meaningful stretch.

This directly serves the five-star bar in §2.1. A one-star review earned in week one from a bug a week of web testing would have caught is very hard to undo.

**When the APK ships: use a staged rollout.** Ten percent first. If the crash rate is clean, widen. That is the closest thing to a rollback the store offers.

---

### 23.4 REVISED — Native App Is the Real Target

**CEO ruling, and it overrides §23.3 as originally written:**

> *"I don't like the fact that it's just web based for now. That's weak sauce. Sure, if that's what is important for now, fine — but I think the APK app would be best."*

**The CEO is right, and there is a technical reason that makes his position stronger than preference.**

#### The argument that settles it

**§1.4 makes "must not shut down in the background" a founding requirement.** §17.3 records it as blocked: *"this is still a web timer, not a native OS alarm."*

**A web app cannot hold a native OS alarm when the browser is closed or the phone is locked. Not a limitation of this build — a limitation of the platform.**

So the native app is not the nicer version of the product. **It is the only version that can satisfy a requirement written in the project's first paragraph.** This substantially settles the direction of D-04.

#### What survives from the original recommendation

The earlier "web first" advice was never really about web versus native. **It was about having a cheap place to break things before anything becomes permanent.** That logic holds — it just moves.

| Surface | Role |
| --- | --- |
| **Debug APK, sideloaded to the S25 Ultra** | **The new iteration surface.** Build, install, break, fix, rebuild. Costs nothing, reviewed by nobody, permanent for no one |
| **Public Play Store release** | The thing that is expensive and one-way (§23.2). This is what earns caution, not the APK itself |
| **The web build** | Demoted from *the product* to a **demo and marketing surface** — a link for people who want to see it before installing |

**The distinction that matters: a sideloaded test APK carries none of the risk that a store release does.** Iterating on your own phone is free. That was always available, and CI makes it routine (§24.2).

#### The revised release strategy

```
1. CI compiles a debug APK → sideload to the S25 Ultra
2. Iterate hard on real hardware → touch, haptics, lock-screen timing,
 background alarms, speakers
3. Work the §16 bug list → on the device, not in a browser
4. Web build becomes the demo → marketing surface, not the product
5. Staged Play release last → 10% → widen, per §23.3
```

**What this unblocks immediately:** every item Grok listed as blocked in §17.3 — real phones, install sheets, speakers, haptics, lock-screen timing, denied notifications — becomes testable the moment the first APK lands on the phone. **Those have been untestable this whole time. That is the real cost of staying on web.**

#### What this changes elsewhere

| Section | Change |
| --- | --- |
| **§23.3** | Superseded. "Web first" becomes **"internal APK first, public release last"** |
| **§24.2** | GitHub Actions moves from *recommended* to **required** — it is the only compile lane available (D-24 becomes urgent) |
| **D-04** | Direction largely settled: **native is required.** What remains is scheduling, not whether |
| **§15** | The full security architecture now has a target to attach to. Client-side hardening is meaningless on a web preview |
| **D-25** | Revised — see §28.4 |

---

## 24. Compute Strategy

### 24.1 The GPU Question — Straight Answer [Claude]

**A GPU will not help, and it is not the missing piece.**

GPUs run models — image generation, video, LLM inference. **Building an app uses almost none of that.** Compiling an APK is a CPU and disk job. Renting a GPU to compile Android code is renting a race car to move a couch.

**The actual gap is in §19:** Kai has everything except **JDK / Android SDK**. That is a **software** gap, not hardware. No amount of rented GPU fixes it.

### 24.2 What Fixes It — GitHub Actions [Claude]

A build service: push code, it spins up a fresh machine that already has the JDK and Android SDK, compiles the APK, and hands back the file. **No server to rent, maintain, or secure.**

**Cost:**
- **Public repositories:** completely free and unlimited
- **Private repositories, free plan:** 2,000 Linux minutes/month plus 500 MB artifact storage
- **Beyond that:** $0.006 per Linux 2-core minute, reduced from $0.008 in January 2026
- **A free account's default spending limit is $0** — jobs stop rather than billing. **You cannot accidentally run up a bill**

**In practice:** an Android build runs a few minutes. 2,000 minutes covers hundreds of builds a month. This project will not come close.

### 24.3 Where a GPU Would Make Sense [Claude]

One legitimate use: **generating brand and marketing assets** — app icons, store screenshots, promotional imagery. Even then, hosted image services cost less than renting a box. **Not a launch-phase problem.**

### 24.4 The Agent Layer Already Exists [Claude]

**That layer already exists. It is Kai.** Persistent filesystem, SSH, Git, package installation, scheduled tasks, memory surviving every conversation — running on the CEO's own phone under his own SHA-locked governance. A rented server driven by an external model would duplicate Kai with less security and a monthly bill.

**It also cuts against Section 21.1.** Kai acts, so Kai holds the keys. Handing credentials to a model with no runtime buys nothing and creates exposure.

**No new box. No new keys. The gap closes with a free build service.**

### 24.5 Learn It or Delegate It? [Claude]

**Both — but the split matters more than the answer.**

**Learn this. Roughly an afternoon, and not knowing it leaves you dependent:**

| Concept | Why it matters |
| --- | --- |
| What a **commit** is | So you know nothing you have built can ever be lost |
| **Deployment vs. source** | So §23.1 stops being mysterious |
| How to **roll back** | So a bad release is a two-minute problem, not a crisis |
| **Where the keys live** | So §21.1 is something you enforce, not something you were told |
| Reading a **build log** for pass/fail | So "it's done" is something you can check |

**Do not bother learning:** Gradle configuration, DNS syntax, Kotlin, CI workflow files. That is what Kai and Grok are for.

**The principle: learn enough to verify, delegate the rest.**

Kai's rule is **"receipts or it didn't happen."** Receipts are only worth something to someone who can read them. That short list is what makes them readable — the difference between directing the work and hoping it went well.

---

## 25. Public Positioning and AI Disclosure

### 25.1 The Context

People have harassed the CEO for using AI to build an app. The recurring assumption is that it is easy — one prompt and done.

**The CEO's answer, in his own framing:**

> If it were that easy, every one of those apps would be five stars. It would need no repairs. It would need no changes. It would simply be perfect on the first try.
>
> That is not what happens. Nobody who says this understands the actual battle — the failures, the constant struggles, the hurdles. It takes a specific kind of mind to push a build through all of that and come out with something that works.

### 25.2 The Position [Claude]

The argument is correct on the merits, and the evidence sits in this document.

**Twenty-one logged defects with IDs (§16).** A dozen unresolved design decisions (§28). A dependency repair that took a timed-out install, a wounded dpkg, and a manual package-tree rebuild before Node would run (§18.5). An entire product conceived, specced, and then abandoned after the market was checked properly (Appendix B).

**None of that is what a single prompt produces.** A one-prompt app does not generate a bug list, because nobody cared enough to look. **The length of the defect list is evidence of the work, not against it.**

**The defensible public line:** the tools are not the achievement. The judgment is. Deciding the keyboard should not fire before the user taps the box, that "longer than a day" is a hidden tap target most people will never find, that a verification agent should audit the builder's work — those are product decisions. No tool makes them for you.

### 25.3 The Disclosure Question [Claude]

**Two different things are being conflated. Separating them cleanly:**

**Thing one: "AI helped build my app."**
**Not** something Google Play requires you to disclose. The 2026 disclosure policy applies to apps that **use AI to generate content displayed to users** — chatbot apps, AI writing tools, image generators, voice applications. **Development tooling is not in scope.** Play policy does not expressly require disclosure of AI use in the app description or documentation. Nobody announces their IDE; nobody has to announce this.

**Thing two: "My app uses AI."**
**This one does apply.**

§14 specifies an API key section where the user's chosen AI talks to them and delivers reminders about their notes and tasks. **That is AI-generated content shown to a user.** Google requires that apps clearly disclose when conversational or editorial content is AI-generated, especially where a user might reasonably believe they are interacting with a human. Building the disclosure into the **interface** rather than only the privacy policy is low friction and reduces policy exposure significantly.

**Practical read:** a small, tasteful "AI" marker on agent-generated reminder text satisfies this. It can be styled to match the design language rather than bolted on. **Costs nothing, removes a review risk.** Tracked as **D-05**.

**One more line worth knowing:** Play's spam clause targets apps that exist primarily to rehash existing apps or provide no useful functionality beyond wrapping a third-party model. Auric Time Crowned is a timer with an AI assist feature, not a model wrapper, so it sits well clear. Worth knowing the line exists before the AI feature grows.

### 25.4 Public Posture — D-21

| Option | Posture | Trade-off |
| --- | --- | --- |
| **Silent** | Never mention tooling. Domain, branding, and quality speak for themselves | Cleanest. Standard practice. Risk only if silence is later framed as concealment |
| **Matter-of-fact** | If asked, answer plainly and move on. No defensiveness, no essay | Removes all leverage from the criticism. Hardest to attack |
| **Forward** | Make the build process part of the story — the bug lists, the repairs, the grind | Turns criticism into marketing. Higher effort, and it makes tooling the headline |

*[Claude's note: the criticism loses its force the moment it stops producing a reaction. Whichever option is chosen, it works best held consistently.]*

---

# PART VI — BUSINESS AND BEHAVIOR

## 26. Monetization Strategy

*Researched at the CEO's direction, September 2026. All findings marked **[Claude]** — advisory, not requirements.*

### 26.1 The Finding That Changes Your Economics — [Claude]

**Google changed its fee structure in 2026, and the change is strongly in Royalty's favour.**

Starting **30 June 2026** in the US, UK, and EEA — Australia followed in September, Japan and South Korea in December, with full global rollout targeted by September 2027 — Google moved to a two-part fee:

| Developer revenue | Service fee |
| --- | --- |
| **Under $1 million/year** | **Flat 10%** on all transactions, including auto-renewing subscriptions |
| Above $1 million/year | ~20% on one-time purchases for new customers, ~25% for existing; subscriptions keep a lower rate |

**Auric Time Crowned is comfortably under $1M. The rate is 10%, not 30%.**

On top of the service fee, **using Google's Play Billing adds a separate billing fee of about 5%**, which developers can avoid by directing users to external payment methods — though Google can still collect its service fee on web purchases completed shortly after.

**What $7.77 actually returns:**

| Route | Royalty keeps |
| --- | --- |
| Play Billing (10% + ~5%) | **~$6.61** |
| External payment (10% service fee only, ~3% processor) | **~$6.76** |

**The practical read:** at this scale the fee is no longer the thing to optimize. Web-to-app billing can improve margins by 25% or more at larger volumes by moving processing from 15–30% down to roughly 3% — but **at 10% it is not worth the friction of pushing users off-platform.** Ship with Play Billing. Revisit if revenue crosses $1M.

### 26.2 The Model Question — [Claude]

**Current model: one-time $7.77 for the Crown tier.** The research points both ways, and this is a real decision rather than an obvious one.

**The case for keeping one-time:**
- One-time premium pricing is the recommended model for **niche professional tools and single-purpose utilities** with a clearly defined, immediately understood value proposition
- **Single-purpose utilities rarely justify a recurring fee.** Users prefer free or one-time options for them
- The **$5–10/month subscription tier is declining** as users cut generic utility apps
- $7.77 is already decided, already branded, and already in the build

**The case for subscription:**
- **Subscription is the highest-LTV model for productivity and utility apps with daily use** — and daily use is exactly what §27 is engineered to produce
- Subscriptions generate the most predictable revenue and the **highest valuation multiples, 4–8× ad revenue**
- **82% of non-gaming apps** now use subscription in some form

**The honest caution:** the disparity among subscription apps is brutal. **The top 5% of earners make 200× the bottom 25%.** Most apps that adopt subscription do not achieve anything like the headline results.

### 26.3 [Claude]'s Recommendation — Advisory

**Hybrid, weighted toward one-time.** Hybrid monetization outperforms single-model approaches for most app types, and **over 60% of top-grossing apps run multiple revenue streams**.

| Tier | Price | Contents |
| --- | --- | --- |
| **Free** | $0 | The full timer, stopwatch, alarm, notes, and heartbeat. Genuinely useful on its own |
| **Crown** | **$7.77 one-time** | Unlimited customization, premium palettes, Glow Mode, themes, icons, transparency, the full §11 system |
| **Seasonal packs** *(optional, later)* | Small IAP | The seasonal palettes already specced in §11.3 — repeat revenue without a subscription |

**Why this shape:**
- Keeps the branded $7.77 that is already decided and already built
- **The AI agent layer stays free**, because BYOK means it costs Royalty nothing per user (§14.4). Charging for something with zero marginal cost invites resentment
- Seasonal palettes give repeat revenue **without** the subscription trap — the user opts in when they want something, rather than being billed for existing
- Leaves the door open: if usage data later shows genuine daily engagement, a subscription tier can be added **on top** rather than replacing what works

**The trap to avoid:** the research is blunt that the #1 founder mistake is picking a pricing model after the app is built, because the model determines which features get built first and how the first 60 seconds feel. **That decision is being made now, which is the right time.**

### 26.4 Revenue Realism — [Claude]

Utility apps earn roughly **$0.01–$0.05 per daily active user from ads** — versus $0.05–$0.20 for casual games. **Ads are not a viable primary model here**, and they would fight directly against the $1,000-feel standard in §3.7. Recommendation: **no ads at any tier.**

**Tracked as D-31.**

---

## 27. Engagement Design — The Ethical Framework

*The CEO's instruction: research how to make the app genuinely habit-forming **for positive reasons**. This section is that research, and it also resolves the open question in D-11.*

### 27.1 The Two Frameworks Worth Knowing — [Claude]

**Fogg Behavior Model — B = MAP.** Behavior happens when **Motivation, Ability, and a Prompt converge at the same moment.** If any one is missing, the action does not occur. It is the most useful diagnostic in the field: when a feature is not being used, one of the three is absent, and you can test which.

**The Hook Model (Nir Eyal) — the four-step loop:**

| Step | What it is |
| --- | --- |
| **Trigger** | Something cues the behavior — external at first, internal once the habit forms |
| **Action** | The easiest possible version of the behavior |
| **Variable Reward** | An unpredictable outcome — this is what fires the dopamine system |
| **Investment** | The user puts something back in, which **seeds the next cycle's trigger** |

The loop is self-sustaining because step four generates step one for the next iteration.

**The goal of the model is to make external triggers unnecessary** — to associate the product so deeply with an internal trigger that notifications stop being needed. **That is the humane version of engagement, and it is also the more effective one.**

### 27.2 The Ethics Test — And Auric Passes It — [Claude]

Eyal's **Manipulation Matrix** asks two questions to determine whether attempting to hook users is ethical:

1. **Does it materially improve the user's life?**
2. **Does the maker use it himself?**

**Both yes = the Facilitator quadrant — the best position in the matrix.** Both no = exploitation.

| Question | Auric Time Crowned |
| --- | --- | --- |
| Materially improves the user's life? | **Yes.** Time awareness and honest self-tracking is a real benefit, not a manufactured need | Available |
| Does the maker use it himself? | **Yes.** The CEO is building it for his own time tracking — that is the origin story in §1.2 | Available |

**Verdict: Facilitator.** The CEO's instinct — *make it addicting, for positive reasons* — lands on the right side of the line by the field's own standard test. **This is the answer to D-11.**

*Worth noting: Eyal's 2019 follow-up, Indistractable, taught users how to defend against the mechanics his first book taught companies to build. The field takes this seriously.*

### 27.3 Mapping the Loop to Auric — [Claude]

**The good news: most of this is already specced. It was designed by instinct and it maps cleanly.**

| Hook step | Already in the spec | Section |
| --- | --- | --- |
| **Trigger (external)** | The heartbeat reminder, user-set 1 second to 24 hours | §14.3 |
| **Trigger (internal)** | The feeling of *"where did my time go?"* — the emotion the whole product answers | §1.2 |
| **Action** | Spin the reel. Already the lowest-friction interaction in the app | §5.2 |
| **Variable Reward** | **The prism grade** — you do not know your score until you finish. Plus the weekly-refreshing Pyramid and the discovery elements | §9, Appendix A |
| **Investment** | **The notes.** Every note makes the user's own log more valuable, which seeds the next session | §7.4, §8.3 |

**The strongest asset is the Investment step, and it is already built.** Notes are user-owned data that compounds. A user with six months of notes has something no competitor can replicate, because it is theirs. That is retention that does not require pressure.

**The reel is the Variable Reward's delivery mechanism, and it was chosen for the right reason** — the CEO described it as *"the gambling feel: spin the wheel, see what you get."* Variability is precisely why people scroll feeds endlessly and never scroll a banking ledger.

### 27.4 The Line — What to Build, What to Refuse — [Claude]

**The same principles that make an experience seamless can be used to manipulate.** Poorly applied, prompts exploit attention and emotion, producing compulsive engagement rather than meaningful interaction. The test is transparency, user benefit, and consent.

| Build | Refuse |
| --- | --- |
| Streaks that **celebrate** an achievement | Streaks that **threaten loss** — *"you'll lose your 30-day streak!"* |
| The prism grading the user's **own** performance | Comparison against other users as a pressure device |
| **User-configured** reminder frequency — already specced at 1s–24h | Notifications the user did not ask for, timed to maximize opens |
| Achievement unlocks that feel earned | Artificial scarcity or manufactured FOMO |
| Making the value obvious | Making the exit hard |
| Notes that compound into something the user owns | Data the user cannot export or delete |

**The heartbeat is already the right design.** Making reminder frequency user-settable from 1 second to 24 hours hands the trigger to the user. **That is autonomy-respecting by construction**, and it was in the spec before anyone researched the ethics.

### 27.5 The Design Rule — [Claude]

> **Every engagement mechanic must survive this question: if the user fully understood what this feature was doing and why, would they still want it?**
>
> If yes, build it. If it only works while the user does not notice, it is the other kind, and it belongs to a product that is not this one.

**The business case, not just the ethical one:** mechanics that feel coercive reliably generate one-star reviews. §2.1 sets the bar at five stars. **The ethical line and the commercial line are the same line here** — which is a genuinely fortunate position to be in.

### 27.6 What This Resolves

**D-11 is answered.** The concern was never the casino *aesthetic* — glossy, glowing, premium visuals are style, and style is not manipulation. The concern was **pressure mechanics**, and §27.4 draws that line explicitly.

**Remaining caution:** Google Play does scrutinize casino-styled presentation. The app must not be mistakable for gambling — no wagering language, no chance-based paid outcomes, no simulated betting. **Auric is a timer with a spinning selector, and it should be unambiguous about that in the store listing.** Tracked as **D-32**.

---

# PART VII — FORWARD WORK

## 28. Master Decisions Register

*Every open decision from all four source documents, deduplicated and prioritized. **This replaces the four separate open-items tables.***

### 28.1 Blocking — Resolve Before Dependent Work

| ID | Decision | Detail | Owner |
| --- | --- | --- | --- |
| **D-01** | **Model identity** | **RESOLVED.** z.ai ships GLM-5.3 and the subscription is live. Kai is GLM 5.3. Remaining action: change `AGENTS.md` from `z-ai/glm-5.2` to `glm-5.3` so the heartbeat stops reporting drift | Kai, one-line edit |
| **D-02** | **Asset provenance marker** | A format so Kai can confirm a returned asset came from the authorized source rather than being pasted from elsewhere (20.4, 20.7) | Kai |
| **D-03** | **Third-party personal data** | `05 Relationship Dossier` may hold information about people who are not party to this project and did not consent to disclosure. KAI-001 carries an exclusion clause pending this ruling | **CEO** |
| **D-04** | **Background persistence** | Section 1.4 makes "must not shut down in the background" a founding requirement. **Direction settled: a web app physically cannot hold a native OS alarm, so native is required.** Step 3 of the build order proves it. What remains is execution, not decision | Kai |
| **D-05** | **AI disclosure marker** | Required in the agent reminder UI before Play Store submission (§14.5, §25.3) | **Astra to spec** |

### 28.2 CEO Decisions

| ID | Decision | §ref |
| --- | --- | --- |
| **D-06** | **Levels and target goals** — named as a founding feature, never specced. Ship, defer, or drop | 8.4 |
| **D-07** | **Domain name** for Auric Time Crowned | 22.5 |
| **D-08** | **Floating transparent widget** — movable, see-through, multi-color, Iron Man reference. A founding feature with no specification | A |
| **D-09** | **Glow Mode** — a fourth theme beyond Light/Dark/Auto. Deep black with every element luminescent. *"Like using an app inside a nebula."* Explicitly called a marketing hook. Never carried into the spec | A |
| **D-10** | **`02 Secure Vault` classification** — governed as *media only (audio/video/APK)*, but described as holding movies **and sensitive documents**. If documents are present, either the classification is wrong or the contents are misfiled — and the earlier breach clearance was granted on incomplete information | 21.3 |
| **D-11** | **Engagement design ethics** — **RESOLVED in §27.** Auric passes the Manipulation Matrix in the Facilitator quadrant: it materially improves the user's life, and the maker uses it himself. The aesthetic was never the issue; the line is drawn at pressure mechanics in §27.4. Residual store-listing risk moved to **D-32** | 27.2 |
| **D-21** | **Public posture** — silent, matter-of-fact, or forward | 25.4 |
| **D-22** | **Repo visibility** — public repos get unlimited free CI; private get 2,000 min/month. Affects cost *and* whether the source is readable by anyone | 24.2 |
| **D-30** | **Play Automatic Integrity Protection eligibility** — the service that makes a tampered build refuse to run is currently limited to **selected Play Partners**. Confirm whether Royalty's developer account qualifies, because this is the feature that directly delivers the §15.1 requirement | 15.8 |
| **D-31** | **Monetization model** — keep one-time $7.77, add seasonal IAP, or introduce a subscription tier. Claude recommends hybrid weighted to one-time, ads at no tier | 26.3 |
| **D-32** | **Store listing language** — the app must be unmistakably a timer, not gambling-adjacent. No wagering language, no chance-based paid outcomes | 27.6 |
| **D-33** | **Platform** — **SETTLED: native Android, Kotlin + Compose.** The web app is not rebuilt; it becomes the demo surface. No cloud server rented — GitHub Actions is the compile lane | 30 |
| **D-34** | **Execution model** — superseded by D-36. Kai 9000 writes the code. Astra 6 returns assets only | Standing Orders, 20, 32 |
| **D-36** | **Command structure** — **SETTLED.** Kai 9000 on GLM 5.3 leads the build. Astra 6 reduced to image generation on copy code. Fable 5.1 runs the final hardening pass. Grok 4.6 is reference only | Standing Orders, 20 |
| **D-37** | **API routing** — **SETTLED.** z.ai primary, OpenRouter fallback. GLM 5.3-Flash for bulk, GLM 5.3 for judgment | 18.0 |
| **D-38** | **Fable 5.1 cost discipline** — readmitted for a narrow final pass only. The original cost objection returns the moment its scope widens to feature work | 20.6 |
| **D-35** | **Sprint scope** — ten days buys a working native foundation, not the full specification. Device Monitor, Prism, AI agent layer, and the full customization system are explicitly out of the first sprint | 30.3 |

### 28.3 Design Calls — Kai

| ID | Decision | §ref |
| --- | --- | --- |
| **D-12** | **Masthead placement** — across the very top above the scroll wheel, or top-left | 3.6 |
| **D-13** | **1-minute to 1-hour presets** — keep or cut. Noise or not | 5.3 |
| **D-14** | **Day/week circle** implementation below the wheel | 5.4 |
| **D-15** | **Prism scoring circle** — best execution as a wheel | 9 |
| **D-16** | **"Logs and Exports"** — final name, or a superior consolidated title | 10.3 |
| **D-17** | **Themes / Icons folder** — final name | 11.2 |
| **D-18** | **Size Options folder** — final name | 11.2 |
| **D-19** | **Transparency control** — scroll wheel, drag bar, or wheel with drag arrow | 11.6 |
| **D-20** | **IP reveal tone** — does it read as too black-hat and scare users off? | 12.3 |

### 28.4 Approvals and Scheduling

| ID | Item | Status |
| --- | --- | --- |
| **D-23** | **Cloudflare Pages** as the host | Proposed (22.4). CEO to approve |
| **D-24** | **GitHub Actions** as the APK compile lane | **SETTLED** — it is the compile lane. Day 1 of the sprint is building it (§30.2) |
| **D-25** | ~~Web-first release~~ → **REVISED: internal APK first, public release last** | **Superseded by §23.4** — CEO ruling. Sideloaded debug APK becomes the iteration surface; the web build demotes to a demo |
| **D-26** | **Staged rollout** on first Play Store release — 10% before widening | Recommended (§23.3) |
| **D-27** | **Cutover plan** — port off grok.me, keep it as an internal surface | Not scheduled (§22.5) |
| **D-28** | **Kai's first work order** — audit the live preview, port the code, or run KAI-001 | Awaiting the CEO's call |
| **D-29** | **CEO learning list** — commits, deploy vs source, rollback, key custody, reading a build log | Recommended (§24.5) |

### 28.5 Closed

| Item | Resolution |
| --- | --- |
| **Vault share scope** | Closed. Under the reduced Astra role there is no external model that needs the vault (21.3) |
| **GPU rental** | Closed — not needed for building. Revisit only if brand-asset volume grows (§24.1, §24.3) |
| **Web view migration** | Approved — own domain, Cloudflare Pages proposed (§22) |
| **TTS project** | Parked, not dead (Appendix B) |

---

## 29. Research Program

### 29.1 The Primary Research Instruction

> **Go online and find out what the majority of people actually complain about in clock, timer, and alarm apps.**

**The highest-value research task in the project, and it remains unstarted.** Real user complaints about existing apps are the shortest path to the five-star bar in §2.1.

**Natural fit for Kai 9000** — it has live web search and fetch (§19).

### 29.2 Standing Research Topics

Status key: Not started / In progress / Complete

| # | Topic | Status |
| --- | --- | --- |
| 1 | **Color psychology** — what drives engagement, trust, and spending; demographic preferences; cultural meanings for global launch | Not started |
| 2 | **Haptic design patterns** — what feels premium vs cheap; competitor usage; Android haptic API capability | Not started |
| 3 | **Gamification retention data** — do achievement systems actually increase retention, and which types | Not started |
| 4 | **Scroll wheel usability** — wheel vs list; A/B testing; Tecarta user feedback; **accessibility — can visually impaired users operate the wheel?** | Not started |
| 5 | **Casino aesthetic in utility apps** — raises perceived value, or reads as gimmicky? | Not started |
| 6 | **Ambient glow and battery drain** — cost of continuous animation; may need reduced frame rate or GPU rendering | Not started |
| 7 | **Animation performance on mid-range devices** — must run on $200 phones, not only the S25 Ultra | Not started |
| 8 | **Glow Mode accessibility** — does it work for photosensitive users? Needs opt-out and a reduced-glow option | Not started |
| 9 | **Onboarding drop-off** — where users abandon setup; wheel-based onboarding must minimize it | Not started |
| 10 | **App Store Optimization** — keywords, converting screenshots, description style | Not started |
| 11 | **Pricing psychology** — $7.77 vs $7.99; does 7 resonate; test against $8.88 | Not started |
| 12 | **Color accessibility** — palettes must work for colorblind users, roughly 8% of men | Not started |

*Items 4, 8, and 12 are accessibility items. They are the ones most likely to be cut for time and most likely to generate one-star reviews if they are.*

---

## 30. Next Steps

> **Platform decision: settled. Native Android. Do not rebuild the web app.**
>
> A web build already exists and is now the demo surface. The native app is the product, and it is the only thing that can satisfy the background-alarm requirement in Section 1.4.
>
> **No rented cloud server.** GitHub Actions is the compile lane and it is free. The only server this project will ever need is the entitlement server in Section 15.9, built last, when there is revenue to protect.

### 30.1 Position

Day 1 is complete. The APK compiles in CI, installs on the S25 Ultra, and opens. `BUILD-RECEIPT.json`, `SHA256SUMS.txt`, and `apk-signature.txt` ship alongside the binary, so every build is verifiable against what CI produced.

Everything Grok listed as untestable — touch, haptics, speakers, lock-screen timing, install sheets, denied notifications — is now testable on real hardware.

### 30.2 The Honest Timeline

The full specification in Sections 3 through 15 is months of work. Thirty sections, twenty-one known defects, an entire customization system. Anyone promising it in days is guessing.

What a focused run produces is a native app with the core loop working and the design language established. That is a foundation. Everything after it is addition rather than invention.

### 30.3 Build Order

| Step | Objective | Done means |
| --- | --- | --- |
| 1 | Skeleton, CI, first install | Complete |
| 2 | The Reel — the scroll wheel (3.2, 5.2) | Spin, snap, click, set a value. Momentum physics and haptic tick per notch, verified by hand on the device |
| 3 | Core timer loop | Set, run, fire. Then fire with the screen locked and the app closed. This proves D-04, the founding requirement |
| 4 | Alarm and wake protocol (7.4) | Ring, warning screen, Dismiss or Snooze, premium note page, keyboard only after tap, Save/Delete/Archive |
| 5 | Notes persistence (8.3, 10.4) | Notes save to internal storage and survive a reboot. This is the Investment step in 27.3, the retention asset |
| 6 | Design language (3.1, 3.2, 3.7) | Bioluminescent glow, four-second breathing pulse, casino glass, red-dot status strip |
| 7 | Settings frame (11) | Customizations folder model, hex entry, one working colour path end to end |
| 8 | Stabilise | Bug pass, STATE.md current, clean commit. Whatever is unfinished is written down, not remembered |

Deliberately excluded from this run: the Device Monitor (12), the Prism (9), the AI agent layer (14), seasonal palettes, achievements, discovery elements, and the full customization system in Section 11. All of it comes after, on a foundation that works.

### 30.4 Running Alongside

- Register the domain (D-07). Five minutes, done while something compiles.
- The existing web build stays live as the demo until the domain is pointed.
- Research task on real user complaints in clock and timer apps (29.1), routed to Opus 5 via the 20.5 brief.
- Build hygiene from the first release build (15.12, Phase 1): release keystore generated once and kept out of version control, zero hardcoded secrets, R8 configured. Free now, expensive to retrofit.
- Re-arm the install-unknown-apps setting after each sideload.

### 30.5 After

Return to Section 28 and work the register. The blocking decisions still need rulings and the Section 16 bug list still applies, now against a native build where the defects are testable on real hardware.

---

## 31. The Build Protocol — One Step at a Time

> The problem this solves: sessions end without warning and progress vanishes. The fix is not better memory. It is never holding state in a conversation.

### 31.1 The Rule

Nothing exists until it is committed. A file described in a chat window is a description of work, not work. If the session ends, it was never real.

### 31.2 The Loop

```
1. PICK      One step. One file, or one feature. Small.
2. BUILD     Write it completely. No placeholders, no deferred TODOs.
3. COMMIT    Write it to the repo and commit. The message names the step.
4. VERIFY    It compiles. It runs. State the evidence, not the intention.
5. RECORD    Update STATE.md: done, next, blocked.
6. REPEAT    Only then move on.
```

Never batch steps one through six across multiple features. One at a time is slower per step and substantially faster overall, because nothing has to be reconstructed from memory.

### 31.3 STATE.md

Kept at the repo root. Updated after every step. This is the handoff document between one session and the next.

```markdown
# STATE

## Last verified commit
<hash> — <what it does> — <sha256 of the APK>

## Completed
- [x] Step 1: skeleton + CI + installs on device — commit <hash>

## In progress
- [ ] Step 2: the Reel
  - Wheel renders
  - Snap-to-notch working
  - NOT YET: momentum physics, haptic tick

## Next
- Step 3: timer fires with screen locked

## Blocked
- <blocker> — needs <who> to decide

## Do not touch
- <files that work and should not be refactored>
```

Any new session reads STATE.md first and resumes at "In progress." Never re-plan. Never re-summarize the project. The plan is in this document; the position is in STATE.md.

### 31.4 What "Done" Means

Done is one of these and nothing else:

- It compiled. Here is the build output.
- It installed. Here is the screenshot.
- The test passed. Here is the run.

"I implemented the timer service" is not evidence.

A green CI check means the code compiled. It does not mean the app works. The bar is the app running on the phone.

### 31.5 Reading a Build Log

| Signal | Meaning | Action |
| --- | --- | --- |
| Exit code 127 | Command not found. Gradle never ran | Check the wrapper exists and is executable |
| Exit code 1 with compiler output | Real compile error | Read the first error, not the last. Later ones are usually cascade |
| Lint error on an API-level attribute | An attribute newer than minSdk | Remove it or gate it by version. Do not suppress the check |
| "No Gradle build results detected" | The build never started | Same class as 127. Look before the Gradle step |
| Node.js version deprecation | Warning only | Actions still run. Bump versions when convenient |
| Cache read-only | Warning only | Slower builds, not broken ones |
| Play Protect "hasn't seen this app before" | Expected for any sideload | Not a defect |

Warnings are not failures. Only a red X with a non-zero exit code stops the build.

---

## 32. What to Harvest From the Grok Workspace

> The ZIP is reference, not a base. Nothing is copied forward wholesale, but some of it is genuinely valuable and discarding it would be a real loss.

### 32.1 Worth Taking

| Artifact | Why |
| --- | --- |
| The 12 brand PNGs | Finished assets. Rebuilding them gains nothing |
| The verified font list — Inter, Playfair, DM Mono, Orbitron, Monoton, Cormorant, Barlow | Seven faces confirmed to load and render. Tested knowledge |
| The colour set — Ivory, Pearl, Blood, Amber, Gold | Already chosen, already named, already branded |
| The 268 tests | Not the code. The behaviours they describe. Each test states what the app should do. Read them as a specification |
| The layout breakpoints — 360, 390, 412, 768, 1280 | Confirmed working without overflow. Reuse the numbers |
| The bug list (Section 16) | Twenty-one defects found the hard way. Do not rediscover them |

### 32.2 Not Worth Taking

The web implementation itself, since Kotlin and Compose share nothing with it structurally. The architecture, since a web app's structure does not map onto a native one. Anything the Section 16 list already flags as broken.

### 32.3 On Code Quality

The build carried twenty-one logged defects. That is not evidence of carelessness. It is evidence that someone looked. A build with zero known bugs usually means nobody audited it.

The real problem was never craftsmanship. It was platform. A web app cannot hold a native OS alarm, and that was the founding requirement from the first paragraph. That is the reason to start fresh, and it is worth holding onto, because it tells you what the new build must prove first.

---

## 33. Kickoff Brief

*Paste at the start of a session that needs full context.*

```
AURIC TIME CROWNED — BUILD BRIEF

CONTEXT
  Read "01 Auric Time Crowned Brainstorm.md" end to end. It is the
  complete specification. Then read STATE.md and resume there.
  Reference only: the Grok workspace ZIP. See Section 32.

COMMAND STRUCTURE
  Kai 9000 (GLM 5.3, z.ai, fallback OpenRouter) owns the build.
  Astra 6 returns image assets on copy code only. Section 20.4.
  Claude Opus 5 answers research briefs. Section 20.5.
  Claude Fable 5.1 runs the final hardening pass. Section 20.6.
  GitHub Actions compiles. Grok 4.6 is reference only.

PLATFORM — SETTLED, DO NOT RE-OPEN
  Native Android, Kotlin and Jetpack Compose.
  Do not rebuild the web app. One exists and is now the demo.
  No cloud server is rented. GitHub Actions is the compile lane.
  Reason: Section 1.4 requires background alarms. Web cannot do it.

HOW TO WORK — SECTION 31
  One step at a time. Build it fully, commit it, verify it compiles
  and runs, update STATE.md, then move on. Never batch. Never hold
  state in the conversation. Assume this session ends without warning.

VERIFICATION
  Done means build output, install confirmation, or a passing test.
  "I implemented it" is not evidence.

CURRENT STEP
  Read STATE.md. Execute the next unfinished item. Stop when it is
  committed and verified, and report.
```

---

# APPENDIX A — Lineage: Origin to Specification

*Traceability. Every founding idea and where it lives now. Use this to check whether a decision has a reason behind it.*

| Founding idea | Where it lives now |
| --- | --- |
| Tecarta KJV scroll wheel | The wheel standard — §3.2 |
| The Reel — spin to set h/m/s | Timer wheel mechanics — §5.2 |
| Always on top, PiP, gold bar | **Still open** — D-04 |
| Floating transparent widget | **Not specified** — D-08 |
| Notes on time | The alarm note flow — §7.4, §8.3 |
| Grading agents | The prism / scoring circle — §9 |
| Levels and target goals | **Not specified** — D-06 |
| Heartbeat with reminders | Heartbeat indicator and Log — §8.1, §10, §14.3 |
| API key section in back office | The AI agent layer — §14. Keys never leave the device — §21.1 |
| Logs, history, export path | Logs and Exports — §10.4 |
| Back office vault, A–Z sort | Customizations folders and sort control — §11.2, §11.7 |
| Scroll wheel sound | §3.2, §3.9, §13 |
| Ambient glow, breathing pulse | The bioluminescent glow standard — §3.1 |
| Hex input, palettes, live preview | The Color folder — §11.3 |
| The white scroll hex sheet | Premium pop-up page standard — §3.5 |
| Casino gloss, 3D depth, matte | Casino premium aesthetic — §3.7 |
| Glow Mode | **Not specified** — D-09 |
| The Three Laws | §3.13 |
| $7.77 | The Crown tier — §15.3 |
| Security to protect the sale | Anti-tamper, final phase — §15.1 |
| Research on user complaints | **Still unstarted** — §29.1 |

### Discovery Elements — Carried Forward, Not Yet Specced

*From the original design system. No specification exists; treat as a backlog of delight features, not requirements.*

**The Jungle Touch** — a subtle decorative element on a browsing screen. Tapped three times, a **glowing ancient scroll** unfurls carrying pro tips, undiscovered features, an achievement badge, and links to advanced settings. Rolls back up on dismiss. Built to be shareable.

**The Pyramid Button** — a small pyramid icon in settings. Tapped, the screen dims as a pyramid rises, then opens to reveal something not in the normal browser. **Refreshes weekly** — a reason to keep opening the app.

**Ambient Particles** — barely visible drifting particles. Hold a finger down three seconds and they **ignite**, swirling around the finger. Release and they settle. No function — pure delight. Style changes with theme: gold sparks, blue bubbles, green fireflies, Matrix-style code rain.

**Achievement System** — badge collection driving retention and virality. Streaks (7-day, 30-day), discovery badges, mastery badges, sharing badges. Each badge premium, glossy, 3D. Unlock triggers full-screen celebration + haptic + sound. *See D-11 before building streak pressure mechanics.*

---

# APPENDIX B — PARKED: Universal TTS Engine

> **NOT ON THE ROADMAP. DO NOT BUILD.**
>
> **Status:** Abandoned by the CEO — *"I decided to abandon this idea because that already exists — but it may be a thing of the future, I don't know yet."*
>
> **Why preserved:** the CEO left the door open, and this project's UI/UX layer became the design language of Auric Time Crowned. That layer now lives in §3 and is **not duplicated here.**
>
> **Attribution:** drafted by **Warlord (GLM 5.2)** for Royalty, 20 July 2026.

### B.1 The Concept

An Android app installing as a **system-wide Text-to-Speech engine**, replacing stock Samsung and Google voices. Connects to any cloud TTS API — xAI, ElevenLabs, OpenAI, Amazon Polly, Google Cloud — letting the user pick premium AI voices and use them **across every app on the phone**.

**Market gap as argued:** every Android phone ships robotic TTS voices while AI companies have built studio-quality neural voices with no way to use them system-wide.

### B.2 How It Would Work

1. Register as an Android TTS engine via `TextToSpeechService`
2. User enters API keys for chosen providers
3. User browses and previews voices
4. User sets the app as default in Settings → Text-to-Speech
5. Every app that speaks routes text through it
6. Text forwarded to the selected cloud API
7. Audio returns and plays

**Noted explicitly:** this is the official, documented, Google-supported route. Not a bypass.

### B.3 Judged Possible

System-wide replacement without root · multi-provider translation modules · voice browsing and preview · **per-app voice profiles** · offline fallback · voice cloning · streaming audio · ad monetization · subscription billing · Play Store distribution at a $25 one-time fee.

### B.4 Judged Not Possible

| Limitation | Reason |
| --- | --- |
| **Phone call audio** | Telephony framework, not TTS. Cannot be replaced |
| **Voicemail / IVR** | Carrier-side. Untouchable |
| **Real-time voice conversion** | Different architecture entirely |
| **Bypassing the TTS framework** | Not attempted by design. The framework is good — the voices are the problem |
| **iOS initially** | Apple's API far more locked down |
| **Zero latency** | Cloud adds 0.5–2 seconds. Caching helps repeated phrases only |
| **Offline operation** | Only via bundled fallback at lower quality |

### B.5 Business Model

| Tier | Price | Contents | Ads |
| --- | --- | --- | --- |
| **Free** | $0 | 1 provider, 1 voice, 10K chars/day, offline fallback | Yes |
| **Premium** | $7.77/mo | 3 providers, 10 voices, 100K chars/day, profiles, caching | No |
| **Unlimited** | $30/mo | All providers and voices, unlimited chars, cloning, priority queueing | No |
| **BYOK** | Free | User brings own keys, unlimited, pays the provider directly | No |

**Revenue streams:** subscriptions · free-tier ads · optional API margin · enterprise white-label at $500–$5,000/month per client · a one-time lifetime tier for anti-subscription buyers.

### B.6 Warlord's Ten Additional Ideas

1. **Smart Cost Router** — auto-route to the cheapest provider meeting the quality bar
2. **Voice Marketplace** — creators upload voices, 70/30 split. Platform, not app
3. **Emotional Intelligence** — adjust delivery to context
4. **Community Voice Ratings** — social proof plus a valuable characteristics database
5. **Voice-of-the-Day** — free tier gets a random premium voice for 24 hours
6. **Developer API** — *"the Stripe of TTS"*
7. **Accessibility Angle** — an underserved market in a less competitive Play category
8. **Affiliate Partnerships** — referral commission on top of subscription revenue
9. **Voice Pre-loading** — pre-generate navigation phrases on WiFi. Zero latency, zero cost on the road
10. **White-Label Licensing** — license the engine to other app companies

### B.7 Risks Identified

API dependency and breakage · provider price increases · Google building it natively · **Samsung Auto Blocker blocking third-party TTS engines by default on the S25 Ultra** · latency intolerance for navigation · data privacy from sending spoken text to cloud APIs · $3K–$15K outsourced MVP cost · unpredictable Play Store review.

### B.8 Development Reality Check — Now Partly Superseded

> The CEO's strengths are **ideas, systems, and business vision.** The recommendation was not to learn to code from scratch, but to document the idea, secure hardware, partner with or hire a developer for the MVP, own the product and business side, and learn enough to read code without writing it all.

*[Superseded in part: §18–§19 document a working on-device environment with Python, Git, Node, and npm — building from the phone, no laptop. The constraint this advice was written against has largely dissolved. The underlying principle survives as §24.5.]*

### B.9 Original Verdict

> **GO.** A genuine market gap with a clear path to revenue. Ship the MVP, get users, iterate. Speed is everything.

**Superseded by the CEO's decision to abandon.** Retained for the record.

### B.10 Name Candidates

VoiceForge · AnyVoice · VoxSwitch · TTS Nexus · VoiceX · OmniVoice · VoiceRouter · SpeakAI · TrueVoice · VoiceBypass · PrimeVoice · VoiceDeck

---

# APPENDIX C — Corrections and Consolidation Log

*Complete audit trail. **Every resolution below can be overturned by the CEO.***

### C.1 Contradictions RESOLVED in This Consolidation

*Evidence settled these. They no longer appear as conflicts in the body text.*

| ID | Conflict | Resolution | Basis |
| --- | --- | --- | --- |
| **R-1** | Brainstorm 02 listed **Node/npm as "not yet"**; Brainstorm 03 listed them **live** | **Live.** §19 is authoritative | Chronological — 02 predates the install by hours |
| **R-2** | Brainstorm 02 §1/§4: *Astra writes code into a Markdown file, Grok implements.* Brainstorm 03: *Kai writes and runs code in-sandbox* | **The §20.1 lane split stands:** CEO decides, Kai builds, CI compiles, Fable hardens | 02's protocol was written because Astra's character limits were the bottleneck. The sandbox removed that bottleneck |
| **R-3** | Org chart named **Grok 4.6 as Compiler**; Kai's plan had **CI compiling** | **CI compiles** (D-24 pending approval); **Grok continues implementation and hosted web builds** | Kai lacks JDK/SDK (§19); CI is free and purpose-built (§24.2) |
| **R-4** | Appendix B: *hire a developer, a computer is required.* Brainstorm 03: on-device sandbox, no laptop | **Superseded by events.** Annotated at B.8 | §18–19 document the working environment |
| **R-5** | Web view migration listed as *"approved in principle, not scheduled"* in one place and as a full decision in another | **Consolidated into §22** as a single decision with D-07, D-23, D-27 tracking the parts | Duplication, not conflict |

### C.2 Contradictions PRESERVED — Genuine Decisions

*These cannot be resolved by evidence. They need a ruling and live in §28.1 and §28.2.*

| Register ID | Conflict |
| --- | --- |
| **D-01** | Model identity — **now closed.** z.ai ships GLM-5.3; `AGENTS.md` needs the one-line update |
| **D-04** | Background persistence — founding requirement vs. blocked in current build |
| **D-08 / D-09** | Founding features (floating widget, Glow Mode) named at inception, absent from the specification |
| **D-10** | `02 Secure Vault` — governed as media-only, described as holding sensitive documents. The earlier breach clearance rested on the media-only reading |
| **D-11** | Engagement design intent vs. store policy and review risk |

### C.3 Repetition Removed

| Element | Was | Now |
| --- | --- | --- |
| Rewrite notice | 4× | 1× |
| Editorial protocol | 3× | 1× |
| Org chart / team table | 3× | 1× (Standing Orders) |
| Kai's toolchain table | 2× within one document | 1× (§19) |
| Debian runtime description | 2× within one document | 1× (§18.1) |
| "No secrets in documents" rule | 3× | 1× (§20.2), referenced elsewhere |
| Design language | 2× — origin in 01, spec in 02 | 1× (§3), rationale merged with rule |
| Scroll wheel description | 3× | 1× (§3.2) |
| $7.77 pricing rationale | 3× | 1× (§15.3) |
| Open items tables | 4× | 1× register (§28) |
| Next steps | 4× | 1× (§30) |
| Corrections logs | 3× | 1× (this appendix) |
| Color code table | 2× in source | 1× (§11.3) |

### C.4 Tier 1 — Mechanical Fixes Applied Silently

| Original | Corrected |
| --- | --- |
| *"reports back to red what got changed"* | **Rex** |
| *"it's no longer fictional like last time"* | **functional** |
| *"Decling a TextToSpeechService"* | **Declaring** |
| *"A experience"* / *"a underserved market"* | **An experience** / **an underserved market** |
| *"seperate"*, *"acrossed"*, *"pannel"*, *"chose"* (for *choose*) | standard spellings |

### C.4a — Correction to a Previous Correction

**Claude made an error and is logging it.**

In an earlier pass, Claude "corrected" the CEO's reference to **Claude Opus 5 Max**, asserting that no Max tier existed on Anthropic's models and that Max belonged only to Astra's ChatGPT configuration.

**That was wrong.** The CEO's model selector shows **Opus 5 Max** as a selectable option. The original reference was accurate; Claude's correction was not.

**The entry has been withdrawn.** No text in this document was changed on the basis of it.

*Logged per the Editorial Protocol. A Tier 1 fix applied to something that was not actually an error is itself an error, and it belongs in the log like any other.*

### C.4b — Revisions Made After the Master Was First Assembled

*Logged so the team can see what changed between the first consolidation and the handoff version.*

| ID | Change | Basis |
| --- | --- | --- |
| **REV-1** | **"Web first" → "internal APK first, public release last"** (§23.4) | CEO override, supported by a technical argument: a web app cannot hold a native OS alarm, so §1.4's founding requirement is unreachable on web. The original caution was never about web vs native — it was about cheap iteration, which relocates to the sideloaded debug APK |
| **REV-2** | **D-04 direction settled** — native is required, not preferred | Same argument. Scheduling remains open; the question of *whether* does not |
| **REV-3** | **D-11 resolved** (§27.2) | Auric passes the Manipulation Matrix in the Facilitator quadrant on both tests. Residual store-listing risk split out to D-32 |
| **REV-4** | **D-24 escalated to urgent** | §23.4 makes the APK the product, and CI is the only lane that can compile it |
| **REV-5** | **§15 expanded from 3 subsections to 12** | CEO requested maximum security. Researched September 2026 |
| **REV-6** | **New Part VI added** — Monetization (§26) and Engagement Design (§27) | CEO directive that Claude perform deep research on these domains |
| **REV-7** | **§14.4 added** — the two-API-key distinction | CEO asked whether the API key survives into the real app. It does, but the answer depends entirely on *whose* key, and that distinction was not previously written down |
| **REV-8** | **§20.5 added** — Claude's research role and brief format | CEO directive establishing the Astra → CEO → Claude research relay |

### C.4c — Revisions at the Pro-Tier Handoff

| ID | Change | Basis |
| --- | --- | --- |
| **REV-9** | **Astra's role changed from PM-only to PM + lead implementer** | CEO moved to the Pro tier. The token-economy argument in §20.1 that kept Astra in decision-only mode no longer binds |
| **REV-10** | **Grok 4.6 demoted to reference-only** | CEO's call. Its web build becomes the demo surface and a source of harvestable artifacts (§32) |
| **REV-11** | **Platform settled: native Android, no web rebuild, no rented server** | A web app cannot hold a background alarm (§1.4, §23.4). A web build already exists, so rebuilding it duplicates work. GitHub Actions already provides the compute a rented box would |
| **REV-12** | **§31 Build Protocol added** | Astra caps out mid-task and loses progress. The fix is architectural — STATE.md plus commit-per-step means no state ever lives in a conversation |
| **REV-13** | **§30 rewritten as a scoped ten-day sprint** | The full specification cannot be built in ten days. Saying so plainly is more useful than an optimistic plan that fails on day four |
| **REV-14** | **§32 added — what to harvest before discarding the Grok build** | "Start from scratch" risks throwing away 12 finished brand assets, 7 verified fonts, confirmed breakpoints, and 268 tests that function as a behavior specification |

### C.4d — Revisions at the Kai 9000 Handover

| ID | Change | Basis |
|---|---|---|
| **REV-15** | Kai 9000 promoted from execution to lead. Owns the code and holds GitHub access | CEO directive. Kai is the only participant with a persistent filesystem, real git, and memory across sessions |
| **REV-16** | Astra 6 reduced from PM and implementer to image generation only | CEO directive. GLM 5.3 cannot generate images; that is the one capability Astra retains here |
| **REV-17** | Claude Fable 5.1 added as the final hardening pass inside Kai 9000 | CEO directive. Reverses the original cost-based exclusion, justified by narrow scope. Logged as D-38 so the reversal is visible if scope widens |
| **REV-18** | Grok 4.6 moved out of the build entirely | Its web implementation cannot satisfy Section 1.4. Remaining value catalogued in Section 32 |
| **REV-19** | D-01 closed | z.ai ships GLM-5.3 and the subscription is live. The contradiction was real and is now settled by fact, not by ruling |
| **REV-20** | All emoji removed, tables normalised, prose tightened | CEO directive on document style |
| **REV-21** | Section 31 expanded with a build-log decoder | Exit codes and warning-versus-failure, so a red X is readable without escalation |

### C.5 Structural Decisions

| Decision | Rationale |
| --- | --- |
| **Two projects separated** | The original source fused an abandoned TTS project with the live Timer project. Blending them made the Timer requirements unreadable and risked putting abandoned work back on the roadmap |
| **Design rationale merged with design rules** | The *why* lived in one document and the *what* in another. A reader needed both open. Now the reasoning sits beside the rule |
| **Chat log converted to specification** | The founding feature set arrived as a message thread. Timestamps, reaction emoji, and laughter removed; every feature statement retained and promoted to a numbered requirement |
| **Third-party web content paraphrased** | Marketing prose, footers, and partner links from external sites were cut as noise. Factual color-code data retained |
| **One decisions register** | Four scattered open-items tables became one prioritized register, so nothing needing a ruling can hide in an appendix |

### C.6 Not Carried Forward

Conversational asides carrying no requirement: greetings, self-deprecating remarks, running jokes about model billing, message-thread metadata, duplicated statements of points made elsewhere, and external website boilerplate.

**No requirement, observation, defect, capability claim, or design instruction was removed.**

---

# APPENDIX D — Kai UI Payloads

*Preserved verbatim.*

**D.1 — Dev workspace armed (13 Sep 2026, 5:42 PM CDT)**

```kai-ui
{"type":"column","children":[{"type":"alert","title":"Dev workspace armed","message":"Python + Git + Node 18 + npm 9.2 all live. Git repo committed. Index protocol in place. Ready to build.","severity":"success"},{"type":"card","children":[{"type":"text","value":"First app project — what are we building?","style":"body","bold":true},{"type":"text_input","id":"app_idea","placeholder":"Describe your app idea in one line"},{"type":"button","label":"Start building","variant":"filled","action":{"type":"callback","event":"build_new_app","collectFrom":["app_idea"]}}]},{"type":"card","children":[{"type":"text","value":"Not sure yet?","style":"body","bold":true},{"type":"text","value":"I can scaffold a starter (HTML app, Python tool, or Node project) to show the full loop: build → run → commit → open on your phone.","style":"caption"},{"type":"button","label":"Scaffold a starter","variant":"outlined","action":{"type":"callback","event":"scaffold_starter"}}]},{"type":"text","value":"Sunday, September 13, 2026| 5:42 PM| 09/13/2026| CDT Chicago Time Zone","style":"caption"}]}
```

**D.2 — Briefing for Astra ready (13 Sep 2026, 6:04 PM CDT)**

```kai-ui
{"type":"column","children":[{"type":"alert","title":"Briefing for Astra ready","message":"Copy from the title down. Includes vault tour, verified toolchain, org chart, auric-sovereign plays, and the tasking protocol. Model correction noted: GLM 5.2 per AGENTS.md canonical.","severity":"success"},{"type":"text","value":"Sunday, September 13, 2026| 6:04 PM| 09/13/2026| CDT Chicago Time Zone","style":"caption"}]}
```

---

*Closing note from the CEO: these ideas arrived messy, at one in the morning, written while exhausted. They were captured anyway, because the ideas were good and waiting for a tidy moment would have lost them. The system in the vault was built from the ground up and is the foundation everything else sits on — the working rule for every collaborator is to work with it, not against it.*

**— End of master document —**
