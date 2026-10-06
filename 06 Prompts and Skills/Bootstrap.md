# Bootstrap

**Project:** OFFGRID-Compositor  
**Owner:** Royalty  
**Status:** Scope document. Nothing is built yet.  
**Updated:** 2026-10-06 (America/Chicago)

---

## What this is

Bootstrap is the lock code for every coding agent on OFFGRID-Compositor. Any coding agent, starting with Claude Opus 5.5, must read this whole document before writing a single line of code, and must follow everything in it exactly. No deviations, no shortcuts, no "improvements" that aren't asked for.

After reading Bootstrap, the agent reads `06 Prompts and Skills/Bootloader.md` and works only on the step it's assigned.

---

## Read order (every session)

1. `06 Prompts and Skills/Bootstrap.md` (this file)
2. `06 Prompts and Skills/Bootloader.md`
3. `06 Prompts and Skills/Agent Lanes and HR Review.md`
4. `06 Prompts and Skills/Credentials Security Rule.md`
5. `01 Brainstorm/Feature Scope.md`
6. The research in `02 Research` for the assigned step
7. The design references in `03 Design/References`

If any of these conflict, Bootstrap wins, then Bootloader, then Feature Scope.

---

## Lock rules

1. **No building until Royalty says go.** Royalty starts each Bootloader step. Don't start the next step on your own.
2. **One feature at a time.** Finish the current step, meet every acceptance check, and report back before touching the next one.
3. **Research before building when the step says so.** If a step is marked "Research first," write the findings to `02 Research` and stop for Royalty's review. If it's marked "Build," follow the given commands.
4. **Core design rule.** In every platform folder, every effect renders into the outgoing video so the other person sees it. Nothing in a platform folder is local-only. OFFGRID-to-OFFGRID calling is the premium lane, where everything is visible to both sides.
5. **Typed numbers, never sliders.** Every numeric setting is a typed number field, usually 0 to 100.
6. **Identical toggles.** All toggles look exactly the same as each other, per `03 Design/References`.
7. **File naming.** Title Case with spaces. No dashes or underscores in names Royalty will see.
8. **Folders.** Put work only in the matching numbered folder: `01 Brainstorm`, `02 Research`, `03 Design`, `04 Build`, `05 Docs`, `06 Prompts and Skills`, `07 Logs and Failures`, `08 Chat History`.
9. **Agent lanes and HR review.** Each coding agent (Agent 01, 02, or 03) works only in its own lane, on one feature at a time. Nothing is accepted until the HR Manager returns PASS with evidence and Royalty okays it. See `Agent Lanes and HR Review.md`.
10. **Plain language in the terminal.** All terminal text talks to Royalty like a 13-year-old new to tech. See `Terminal Plain Language Rule.md`.
11. **Android first.** Targets are Royalty's Galaxy S25 Ultra and Galaxy A37. PC and OBS come later.

---

## Security rules

1. **Hard rule: no credentials outside the phone.** No API keys, passwords, tokens, stream keys, or any other sensitive credentials go in the GitHub repo, the chat, or any documentation. Credentials live only in Android's secure storage on the phone (the Android Keystore). Any agent that puts a credential in the repo is an automatic FAIL in HR review. See `Credentials Security Rule.md`.
2. Never edit or change the repository owner's email address or account email.
3. Test only with a dummy account. Never touch the paid big-gifter account.
4. Don't build anything meant to evade platform anti-cheat, root detection, or app-integrity checks.
5. Don't clone a real person's voice. Voice presets are styles, not copies.
6. ElevenLabs is not used anywhere in this project.

---

## When something goes wrong

- **If Claude flags or blocks a task:** stop, write what was blocked and why to `07 Logs and Failures`, and tell Royalty. With Royalty's okay, the task falls back to Grok, which follows this same Bootstrap.
- **If a build or test fails:** log it in `07 Logs and Failures` with the error, what was tried, and the fix.
- **If something is unclear:** ask Royalty. Don't guess.

---

## Reporting back after each step

- What was built or researched
- Which acceptance checks passed or failed
- Files changed, with paths
- Anything logged in `07 Logs and Failures`
- What the next Bootloader step is (don't start it)
- Submit the report to the HR Manager for a PASS, FAIL, or BLOCKED verdict
