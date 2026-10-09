# War Room System — Deployable Blueprint

> **Version:** 1.1 | **Last Updated:** 2026-06-17

> **Main Agent:** Read this file. Follow the instructions below to construct a War Room system on your system.

---

> **If these file structures already exist on your system:** Do not recreate them. Follow the paths, read the existing files, and add only what is missing. This deployable is additive, not destructive.

---

## What This Creates

A War Room framework for multi-agent critique and analysis:
- Main agent orchestrates critique sessions
- Sub-agents provide structured suggestions
- **Centralized project output** in `[workspace]/revenue-engine/war_room/`
- Each target file gets its own reusable project folder
- Metadata tracks session history for future reference
- Works with local, cloud, or hybrid agent setups

---

## System Overview

```
[workspace]/
├── HEARTBEAT.md                    (updated with Read section)
├── PRIMARY/
│   ├── primary.md                  (index file with routing)
│   └── primary-library-of-solutions/
│       ├── OG/                     (deployables stored here)
│       │   └── Deployable_War_Room.md
│       └── War_Room/
│           └── war_room_master.md  (War Room workflow)
└── revenue-engine/
    └── war_room/                   (centralized output)
        └── [war_room_name]_war_room/  (e.g., auth_review_war_room)
            ├── [war_room_name]_war_room.md
            ├── [war_room_name]_war_room_meta.md
            └── sessions/
```

---

## Instructions

### Step 1: Create the folder structure

Create the following directories in your workspace:

1. `[workspace]/PRIMARY/` (if it doesn't exist)
2. `[workspace]/PRIMARY/primary-library-of-solutions/` (if it doesn't exist)
3. `[workspace]/PRIMARY/primary-library-of-solutions/War_Room/` (if it doesn't exist)
4. `[workspace]/PRIMARY/primary-library-of-solutions/OG/` (deployables stored here)
5. `[workspace]/revenue-engine/` (if it doesn't exist)
6. `[workspace]/revenue-engine/war_room/` (centralized output for all War Room projects)

---

### Step 2: Create or Update HEARTBEAT.md

If `HEARTBEAT.md` does not exist, create it. If it exists, add the following section:

```markdown
# HEARTBEAT.md

**Status:** On

## Read
Read this on every heartbeat

"[workspace]/PRIMARY/primary.md"
```

Replace `[workspace]` with your actual workspace path.

---

### Step 3: Create or Update [workspace]/PRIMARY/primary.md

If `[workspace]/PRIMARY/primary.md` does not exist, create it with this structure:

```markdown
# PRIMARY

## Synopsis

HEARTBEAT.md reads this file every heartbeat. This is the control layer index. User-defined phrases map to action files.

## Common Terms → Canonical Paths
- war room → [workspace]/PRIMARY/primary-library-of-solutions/War_Room/war_room_master.md
- war room projects → [workspace]/revenue-engine/war_room/

## Role & Scope
Main agent directive layer. Contains system-wide instructions, delegation rules, and strategic context.

## Unbreakable Standards
1. Every file goes to a directory.
2. One canonical path per purpose. No duplicates.
3. Every file gets a flow header: who, what feeds it, what it feeds.
4. HEARTBEAT.md must fit on one page. Cut, don't expand.
```

If `[workspace]/PRIMARY/primary.md` already exists, add these lines to the `## Common Terms → Canonical Paths` section:

```markdown
- war room → [workspace]/PRIMARY/primary-library-of-solutions/War_Room/war_room_master.md
- war room projects → [workspace]/revenue-engine/war_room/
```

---

### Step 4: Place this deployable file

Copy this file (`Deployable_War_Room.md`) to:

`[workspace]/PRIMARY/primary-library-of-solutions/OG/Deployable_War_Room.md`

---

### Step 5: Create war_room_master.md

Create `[workspace]/PRIMARY/primary-library-of-solutions/War_Room/war_room_master.md` with the following content:

```markdown
# WAR ROOM MASTER TEMPLATE

## flow in/out
- **Created by:** Deployable_War_Room.md
- **Purpose:** Multi-agent critique and analysis system
- **Feeds:** War Room output files alongside target files

---

## INSTRUCTIONS FOR MAIN AGENT: WAR ROOM ORCHESTRATOR

**OBJECTIVE:**
When given a target file path, you are the **Main Agent Commander**. Read this file, execute the workflow, and create a **War Room project** in `[workspace]/revenue-engine/war_room/[name]_war_room/`

---

### STEP 1: TARGET FILE & WAR ROOM NAME

Ask the user:
1. **Target file path** to analyze
2. **War Room name** for this session (e.g., "auth_system", "api_review", "landing_page")

*If the user already provided a path in their command (e.g., "War Room on [path]"), ask only for the War Room name.*

**Naming convention:** The War Room name becomes the project folder and file prefix.
- Example: User says "auth_system" → folder is `auth_system_war_room/`
- Output files: `auth_system_war_room.md`, `auth_system_war_room_meta.md`

---

### STEP 2: PRE-FLIGHT VALIDATION

Before proceeding, verify:

1. **Target file exists?** If not, report error and ask for corrected path.
2. **War Room name collision?** If `[workspace]/revenue-engine/war_room/[name]_war_room/` already exists:
   - Ask user: "A War Room named '[name]' already exists. Choose:"
     - **Overwrite** — Replace existing critique
     - **New version** — Create `[name]_v2_war_room`
     - **Cancel** — Stop and choose a different name
3. **Output directory writable?** Ensure `revenue-engine/war_room/` exists or can be created.

---

### STEP 3: AGENT SELECTION (Capability-Based)

**Read your system's AGENTS.md or agent roster.** Select agents based on capabilities needed for the target file.

**Capability Categories:**
| Capability | Use For | Look For |
|------------|---------|----------|
| **Read** | File analysis, content review | Agents marked ✅ for Read |
| **Write** | Creating output, editing files | Agents marked ✅ for Write |
| **Reasoning** | Logic analysis, critique, synthesis | Agents marked ✅ for Reasoning |
| **VL (Visual Language)** | Image/diagram analysis | Agents marked ✅ for VL or Vision |
| **Code** | Code review, script analysis | Agents marked ✅ for Code |

**Selection Rules:**
1. **Always include at least one agent with Reasoning** — This is your primary critic
2. **Match capabilities to target file type** — VL for images, Code for scripts, etc.
3. **Select 2-4 agents** depending on complexity
4. **You (Main Agent) critique first**, then assign sections to others
5. **No sub-agents available?** Perform multiple passes yourself as separate critiques

**If your system does NOT have an agent roster:**
- Ask user what capabilities they need: "Should I focus on reasoning, code review, or visual analysis?"
- Proceed with Main Agent only if no sub-agents available

---

### STEP 4: CRITIQUE & COLLECT

1. **Main Agent critiques first** — Read the target file and provide your initial analysis
2. **Assign sections to sub-agents** — If you have sub-agents, distribute the work
3. **Collect all critiques** — Whether from sub-agents or your own additional passes

**Rule:** Agents MUST write output as they go to prevent context loss.

---

### STEP 5: SUGGESTION FORMAT (Required for All Critiques)

Every critique — whether from Main Agent or sub-agents — MUST use this format:

```
Severity: 🔴/🟡/🟢/💡
Issue: [Specific problem found]
Impact: [Why it matters - what breaks or who is affected]
Solution: [Concrete fix - specific steps or code]
Alternative: [One other approach the user could take]
```

**Severity Key:**
- 🔴 **Critical** — Breaks functionality, security risk, or data loss
- 🟡 **Warning** — Works but has problems; should be fixed
- 🟢 **Good** — Works well; minor improvement possible
- 💡 **Suggestion** — Optional enhancement or optimization

---

### STEP 6: GENERATE WAR ROOM PROJECT

Create a **War Room project folder** using the War Room name:

`[workspace]/revenue-engine/war_room/[war_room_name]_war_room/`

Inside that folder, create:
- `[war_room_name]_war_room.md` — The critique file
- `[war_room_name]_war_room_meta.md` — Metadata for reuse
- `sessions/` — Subdirectory for future re-runs (create now, even if empty)

**Why a project folder?** This makes the War Room output reusable. Future sessions can reference past critiques, track fixes applied, and build institutional knowledge.

**War Room Project Structure:**
```
[workspace]/revenue-engine/war_room/
└── [war_room_name]_war_room/
    ├── [war_room_name]_war_room.md      ← Main critique output
    ├── [war_room_name]_war_room_meta.md ← Reuse metadata
    └── sessions/                         ← Optional: future re-runs
        └── session_YYYY-MM-DD.md
```

**Example:** User names it "auth_review" →
```
[workspace]/revenue-engine/war_room/
└── auth_review_war_room/
    ├── auth_review_war_room.md
    ├── auth_review_war_room_meta.md
    └── sessions/
```

**Use this EXACT structure for the main critique file:**

```markdown
## flow in/out
- **Analyzed by:** [Main Agent Name]
- **Created on:** [Date/Time]
- **Trigger:** User requested War Room critique
- **Agents Used:** [List agents by name + capability, e.g., "Laguna (Reasoning), Eyes (VL)"]

## synopsis
[High-level summary of the target file's purpose, strengths, and immediate risks - 2-3 sentences max]

## target
`[Full file path of the file being analyzed]`

## agents

### Main Agent Critique
[Your initial critique using the Suggestion Format below]

| Severity | Issue | Impact | Solution | Alternative |
|----------|-------|--------|----------|-------------|
| [🔴/🟡/🟢/💡] | [problem] | [why it matters] | [fix] | [other approach] |

### [Agent Name] Critique
[Sub-agent critiques using the same table format - repeat for each agent]

## Main Agent Review
[Your synthesis. Look at all critiques and suggestions. Create a "Best Suggestion" path by merging the best parts of the original idea with the best parts of the critiques. This is your final recommendation.]

### Recommended Actions (Priority Order)
1. [Highest priority fix]
2. [Next priority]
3. [Continue as needed]

### Optional Enhancements
- [Nice-to-have improvements that aren't critical]
```

**Create the metadata file for reuse:**

`[war_room_name]_war_room_meta.md`:

```markdown
## War Room Project Metadata

## Target
- **File:** `[Full path to target file]`
- **War Room Name:** [war_room_name]
- **Type:** [code / config / documentation / other]
- **First Analyzed:** [Date/Time]
- **Last Analyzed:** [Date/Time]

## Stats
- **Total Sessions:** 1
- **Issues Found:** [count by severity: 🔴 X, 🟡 X, 🟢 X, 💡 X]
- **Issues Resolved:** 0
- **Issues Remaining:** [total - resolved]

## Session History
| Date | Agents | Issues | Notes |
|------|--------|--------|-------|
| [Date] | [agent list] | [count] | [brief note] |

## Reuse Instructions
1. To re-analyze: "War Room on [target path]" with name "[war_room_name]"
2. To check fixes: Compare current file to issues in `[war_room_name]_war_room.md`
3. To add session: Create `sessions/session_YYYY-MM-DD.md`
```

---

### STEP 7: REPORT TO USER

Output a summary to the user telling them:
1. Where the War Room file was created
2. The number of issues found by severity (🔴/🟡/🟢/💡)
3. Your top 3 recommended actions
4. Ask: *"Is this the correct path, or do you want to adjust the approach?"*

---

## NOTES

- **No sub-agents?** No problem. Run multiple critique passes yourself — one pass focusing on structure, another on content, another on logic.
- **Target file doesn't exist?** Report error and ask user to verify path.
- **Complex file?** Break into sections and tackle one at a time. Save progress between sections.
- **Multiple agents with same capability?** Pick the one with best test score or fastest response time.
- **Archive old War Rooms?** Move completed projects to `[workspace]/revenue-engine/war_room/archive/` to keep the main folder clean.

---

## ACTIVATION

User says: **"War Room"** or **"Run a War Room on [file path]"**

Main agent reads this file and executes the workflow.
```

---

## Verification

After creating all files, confirm:

- [ ] `[workspace]/PRIMARY/` directory exists
- [ ] `[workspace]/PRIMARY/primary-library-of-solutions/` directory exists
- [ ] `[workspace]/PRIMARY/primary-library-of-solutions/War_Room/` directory exists
- [ ] `[workspace]/PRIMARY/primary-library-of-solutions/OG/` directory exists (deployables)
- [ ] `[workspace]/PRIMARY/primary.md` exists with War Room routing entry
- [ ] `[workspace]/HEARTBEAT.md` exists with Read section pointing to `[workspace]/PRIMARY/primary.md`
- [ ] `[workspace]/PRIMARY/primary-library-of-solutions/War_Room/war_room_master.md` exists
- [ ] `[workspace]/PRIMARY/primary-library-of-solutions/OG/Deployable_War_Room.md` exists (this deployable)
- [ ] `[workspace]/revenue-engine/war_room/` directory exists (for centralized output)

**After first War Room run, also verify:**
- [ ] `[war_room_name]_war_room/sessions/` directory exists

---

## Usage

Once deployed, your user can invoke this system:

| Phrase | Action |
|--------|--------|
| "War Room" | Main agent asks for target file path |
| "War Room on [path]" | Main agent immediately begins critique of specified file |
| "Run a War Room on [path]" | Same as above |
| "War Room projects" | Lists all War Room projects in `[workspace]/revenue-engine/war_room/` |

**Output Location:** 

When the user provides a target file and names the War Room (e.g., "api_security"):
- **Project folder:** `[workspace]/revenue-engine/war_room/api_security_war_room/`
- **Main critique:** `[workspace]/revenue-engine/war_room/api_security_war_room/api_security_war_room.md`
- **Reuse metadata:** `[workspace]/revenue-engine/war_room/api_security_war_room/api_security_war_room_meta.md`

This centralized structure makes all War Room outputs:
- Easy to find (one location)
- Reusable (metadata tracks history)
- Buildable (future sessions add to same project)

---

## Agent-Agnostic Design

This deployable works on any OpenClaw system regardless of:

- **Local vs Cloud agents** — Selection is capability-based, not location-based
- **No sub-agents** — Main agent can run all critique passes solo
- **Agent roster** — If AGENTS.md exists with capability grid, use it; if not, ask user what capabilities they need

---

## Deployment Complete

Report to your user: "War Room system deployed and ready. Say 'War Room' to begin a critique session."
