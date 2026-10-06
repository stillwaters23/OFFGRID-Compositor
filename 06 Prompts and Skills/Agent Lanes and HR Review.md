# Agent Lanes and HR Review

**Project:** OFFGRID-Compositor  
**Owner:** Royalty  
**Status:** Scope document. Nothing is built yet.  
**Updated:** 2026-10-06 (America/Chicago)

---

## The team

| Lane | Role | Works on |
|---|---|---|
| Agent 01 | Coding agent | One Bootloader step at a time, as assigned by Royalty |
| Agent 02 | Coding agent | One Bootloader step at a time, as assigned by Royalty |
| Agent 03 | Coding agent | One Bootloader step at a time, as assigned by Royalty |
| HR Manager | Review agent | Reviews all three lanes. Writes no feature code. |

Royalty owns the goals and says go for every step. Rex coordinates.

---

## Lane rules (Agents 01, 02, and 03)

1. Before starting any work, read `06 Prompts and Skills/Bootstrap.md` and `06 Prompts and Skills/Bootloader.md`, plus this file.
2. **One agent, one feature, one step at a time.** Each lane works only on the Bootloader step it's assigned.
3. Two lanes never work on the same feature or the same files at the same time.
4. Each lane works on its own branch, named for the lane and step, for example `agent 01 step 3` or as git allows.
5. When a step is done, the lane submits it to the HR Manager with its report-back from Bootstrap. It doesn't merge its own work.
6. A lane doesn't start a new step until the HR Manager returns **PASS** and Royalty assigns the next one.

---

## HR Manager rules

1. Read Bootstrap, Bootloader, this file, and `01 Brainstorm/Feature Scope.md` before reviewing anything.
2. Review every submission against the approved brief: the assigned Bootloader step, its acceptance checks, Feature Scope, and Bootstrap's lock and security rules.
3. Nothing is accepted or merged without the HR Manager's verdict and Royalty's okay.
4. The HR Manager doesn't write feature code and doesn't fix a lane's work for it. It sends the work back with clear reasons.
5. Self-review doesn't count. The HR Manager checks the evidence itself, for example by running the build and tests and looking at the recorded outgoing video.

---

## Verdicts

The HR Manager returns exactly one verdict per submission, always with evidence.

| Verdict | Means | Next |
|---|---|---|
| **PASS** | Every acceptance check is met and no Bootstrap rule is broken. | Royalty okays the merge, then assigns the next step. |
| **FAIL** | One or more checks aren't met, or a rule is broken. | It goes back to the same lane with a fix list. |
| **BLOCKED** | The step can't be finished or judged, for example because of missing access, a platform limit, a needed decision, or a model refusal. | Log it in `07 Logs and Failures` and tell Royalty in one clear line. |

### Evidence required with every verdict
- The step number and feature numbers reviewed
- Each acceptance check, marked met or not met, with proof (build output, test results, screenshots, or a recorded outgoing-video clip)
- Any Bootstrap rule broken, quoted exactly
- Files and paths changed
- For FAIL: a numbered fix list
- For BLOCKED: what's blocking it and what's needed to unblock

### Instant FAIL
- Any credential, API key, or stream key in the repo or the chat
- Any slider instead of a typed number
- A platform-folder effect that doesn't render into the outgoing video (core design rule)
- Toggles that don't match each other
- Work outside the assigned step, or started without Royalty's go
- Any attempt to evade platform anti-cheat, root detection, or app-integrity checks
- Any change to the repository owner's email address or account email

---

## Model fallback

If Claude flags or blocks a lane's task, the HR Manager marks it **BLOCKED** and logs the reason. With Royalty's okay, the task moves to Grok, which follows the same Bootstrap and gets the same HR review.

---

## Review record

Every verdict is logged in `05 Docs/Build Log.md`. FAIL and BLOCKED verdicts are also logged in `07 Logs and Failures`.
