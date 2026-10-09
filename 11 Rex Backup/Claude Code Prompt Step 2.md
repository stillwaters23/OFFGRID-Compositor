You are joining OFFGRID-Compositor fresh. You know nothing yet, so read first.

Repo: https://github.com/stillwaters23/OFFGRID-Compositor (private, owner Royalty). This is NOT the Alzheimer's app. Do not touch any other repo.

WHERE WE ARE (Oct 6, 2026)
- Step 0 done and merged: Kotlin Android app, CameraX front camera preview. Royalty phone-tested it.
- Step 1 done and merged: one CameraX effect paints camera, then effect list, then a yellow test diamond into one picture. The saved 5 second video contains the diamond. Royalty phone-tested it.
- Rex voice training files are in "06 Prompts and Skills/Rex Voice/". Do not touch them.
- Next job: Bootloader Step 2, Terminal panel and controls (Build).

READ FIRST (reading only, change nothing yet)
1. 08 Chat History/Agent Status Log.md
2. 06 Prompts and Skills/Bootstrap.md, Bootloader.md, Credentials Security Rule.md, Agent Lanes and HR Review.md, Terminal Plain Language Rule.md, Folder Guidance Panel.md
3. 01 Brainstorm/Feature Scope.md
4. 03 Design/References (UI spec and toggle and bell images)
5. 02 Research/Outgoing Video Compositor Research.md
6. The code in 04 Build

DO (positive)
- Build Step 2 exactly as Bootloader.md describes, on top of the Step 1 code in 04 Build. Keep the compositor and the yellow diamond test working.
- Look: an original high-tech heads-up display style. Dark frosted glass panel, cyan and gold glow lines, folders that slide out like holograms, a soft rotating ring around the active folder, a soft scan line, and laser-typed terminal text.
- Folders: a side list plus a grid of big folder icons, each showing how many items are inside. Include the 18 platform folders from Feature Scope, with the red and green toggles and the RESULTS line from Folder Guidance Panel.md.
- Terminal text in plain words, per Terminal Plain Language Rule.md.
- Typed values and toggles only. No sliders.
- Kotlin, Android only. Target Galaxy S25 Ultra and Galaxy A37. 720 by 1280 at 30 fps stays the video default.
- Work on a new branch and open a pull request. Keep the GitHub Actions debug APK build passing. Publish the APK as a "step-2-preview" pre-release so Royalty can install it from the phone browser.
- Write install and test steps for Royalty in plain words in the pull request.
- When you finish or stop, add an entry to 08 Chat History/Agent Status Log.md (time America/Chicago, step, what was done, PR link, state, exact next thing to pick up). Also add entries saying Step 1 passed Royalty's phone test and was merged.

DO NOT (negative)
- Do not merge. Rex (Agent 04, the HR manager) gives the final pass. Then Royalty phone-tests.
- Do not start Step 3 or later.
- Do not build Step 7 or anything with voice: no mic, no RECORD_AUDIO, no typed-to-speech, no Rex voice, no read-aloud button (feature 38), no speak-as-Rex (feature 39). Voice is paused until Royalty says go.
- No API keys, passwords, tokens, or stream keys anywhere: repo, docs, logs, chat. Credentials live only in Android secure storage on the phone.
- Nothing that needs a computer. Royalty has no computer.
- No Marvel, Iron Man, or Avengers logos, names, fonts, or artwork. The vibe only, our own design.
- Do not change the repo owner's account or email, repo settings, or other repos.
- No anti-cheat, root-detection, or app-integrity evasion. Do not modify other apps.
- Do not delete or rewrite existing docs. Only add to them where Step 2 needs it.

SELF-CHECK BEFORE YOU REPORT
Go through every DO and DO NOT line above and answer yes or no for each. Confirm:
- the build passed and the APK link works
- the diamond still records into the video
- no mic or voice code was added
- no secrets were added
- the status log was updated
Fix anything that says no before reporting.

REPORT BACK in plain words: what you built, PR link, build link, install link, phone test steps, your self-check list, and anything you were unsure about.
