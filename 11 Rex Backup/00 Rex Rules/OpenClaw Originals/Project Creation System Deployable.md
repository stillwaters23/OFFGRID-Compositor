# Project Creation System — One-File Deployable

> Purpose: Self-installing deployable for OpenClaw | Instructions for receiving agent below

---

## ⚠️ INSTRUCTIONS FOR THE AGENT READING THIS FILE

**You are the agent receiving this deployable. Your job is to read this entire file, then create the folder structure and all files described below.**

### What To Do:

1. Read this entire file
2. Create the folder structure in `~/.openclaw/workspace/`
3. Create each file with the exact content provided
4. Add routing lines to the user's `primary.md` (or create one if missing)
5. Report completion to the user

### Folder Structure to Create:

```
~/.openclaw/workspace/
├── primary/
│   └── primary_library/
│       └── how_to_create_a_project.md
└── projects/
    └── project_creation/
        ├── Template_v2.md
        └── references/
            ├── project_design.md
            └── WORKFLOW.md
```

---

## FILE 1: `~/.openclaw/workspace/primary/primary_library/how_to_create_a_project.md`

```markdown
# How to Create a Project

> Purpose: User instructions for project creation | Fed by: primary.md routing | Feeds: Template_v2.md execution

---

## Overview

This system turns your ideas into structured, executable projects. The process involves three phases: ideation, preparation, and execution.

---

## Phase 1: Ideation (External Conversation)

**Where:** Claude.ai (recommended) or another long-context AI assistant

**What to do:**

1. Open a conversation with Claude (or similar AI)
2. Describe your idea or problem in detail
3. Ask Claude to break down the solution into:
   - **User tasks** — things you must do
   - **Agent tasks** — things your system will do
4. Refine the steps through conversation until clear and actionable

**Output:** A structured breakdown of steps with clear ownership

---

## Expected Input File Format

Your idea file should follow this structure for best results:

```markdown
# [Project Name]

## Problem
[What problem are we solving? Why do we need this?]

## Solution Overview
[High-level description of the solution]

## User Tasks
- [ ] Task 1: [description]
- [ ] Task 2: [description]

## Agent Tasks
- [ ] Task 1: [description]
- [ ] Task 2: [description]

## Success Criteria
- [ ] Criterion 1
- [ ] Criterion 2

## Notes
[Any additional context, constraints, or preferences]
```

**Minimum required:** Problem + Solution Overview + at least one task list

---

## Phase 2: Preparation (Your System)

**What to do:**

1. Save the breakdown as a `.md` file (e.g., `my_project_idea.md`)
2. Create a folder in your `~/.openclaw/workspace/projects/` directory:
   - Name it something descriptive (e.g., `~/.openclaw/workspace/projects/my_awesome_thing/`)
3. Place your `.md` file inside that folder

**Example structure:**
```
~/.openclaw/workspace/projects/
└── my_awesome_thing/
    └── my_project_idea.md
```

---

## Phase 3: Execution (Your Agent)

**What to say:**

"Create a project" or "Please create a project out of [file path]"

**What happens:**

1. Agent reads your idea file
2. Determines the folder name from the file path
3. Processes it through the project creation template
4. Generates `[folder_name]_PROJECT.md` in the same folder
5. Project is now ready for execution

**Example:**
- File provided: `~/.openclaw/workspace/projects/my_awesome_thing/idea.md`
- Output created: `~/.openclaw/workspace/projects/my_awesome_thing/my_awesome_thing_PROJECT.md`

---

## Summary

| Step | You Do | Agent Does |
|------|--------|------------|
| 1 | Discuss idea with Claude | — |
| 2 | Get structured breakdown | — |
| 3 | Save as `.md` file | — |
| 4 | Create folder in projects/ | — |
| 5 | Place file in folder | — |
| 6 | Say "Create a project" | Reads file, builds `[folder_name]_PROJECT.md` |

---

## Tips

- Be specific in your Claude conversation — vague ideas lead to vague projects
- Break large ideas into smaller projects
- Include success criteria in your breakdown
- Name your folder something meaningful — it becomes the project identifier

---

## Agent Instruction: Offer to Create Project Folder

After reading this document to the user, the agent MUST ask:

> "Would you like me to create a named project folder for you in the Projects directory?"

If the user says **yes**:
1. Ask for the folder name (or accept one they provide)
2. Create the folder at `~/.openclaw/workspace/projects/[folder_name]/`
3. Respond with the full path to the created folder

Example response:
> "Created project folder at: `~/.openclaw/workspace/projects/my_awesome_thing/`"

If the user says **no**:
- Acknowledge and let them know they can create it manually or ask later

---

**Ready to start?** Go have that conversation with Claude, then come back and say "Create a project."
```

---

## FILE 2: `~/.openclaw/workspace/projects/project_creation/Template_v2.md`

```markdown
# Project Creation Template

> Purpose: Template for project creation workflow | Fed by: User idea files | Feeds: Generated project files

---

## This Project

Create a project file at `~/.openclaw/workspace/projects/[folder_name]/` called `[folder_name]_PROJECT.md`

**Naming Convention:** The output file is named after the folder that contains it.

**Example:** If the user provides a file at `~/.openclaw/workspace/projects/my_awesome_thing/idea.md`, the output is `~/.openclaw/workspace/projects/my_awesome_thing/my_awesome_thing_PROJECT.md`

---

## Synopsis

[user describes problem and why we need a solution] Write what your project is about here.

---

## Before You Start

**This section must always be included in every project.**

### Context Limit Protocol

If context approaches 90% of limit:

1. Stop at the nearest clean completion point
2. Write a session summary to your memory system
   - Include: what was completed, files written, decisions made, current state
3. Write a continuation file with remaining incomplete tasks
   - Must be self-contained — a fresh session should need only this file and the memory summary to continue
4. Report to user and end session cleanly

Never truncate mid-task without saving state first.

---

## Project

1. Determine the folder name from the user-provided file path
2. Create `[folder_name]_PROJECT.md` in that same folder
3. Read and follow: `~/.openclaw/workspace/projects/project_creation/references/project_design.md`
4. Read and follow: `~/.openclaw/workspace/projects/project_creation/references/WORKFLOW.md`
5. Assign appropriate agents from your system based on task requirements

---

## Note

Write a note to the user, and to yourself, up to three paragraphs. Explain why you did this the way that you did. When you look back at this project you will understand what your intentions were. Also list any alternative choices and why you didn't choose them.

---

## Encouragement

You will be building big projects. Use all of your agents, work together. You have everything you need, and if you don't, you have the tools to create what you need.

Good luck! Impress me!

---

## Rules

- No actions requiring user involvement without permission
- Ideas must be self-contained and executable by agents alone
- If an issue is encountered and steps must be renumbered, do it
- Do not create circular loops
- Save progress periodically without being asked

---

## Status

Report "project ready!" on completion.
Report "We couldn't do it" on abort.
```

---

## FILE 3: `~/.openclaw/workspace/projects/project_creation/references/project_design.md`

```markdown
# Project Design Principles

> Purpose: Design standards for projects | Fed by: Template_v2.md | Feeds: Generated projects

---

## Structure

Every project should have:

1. **Clear objective** — What are we building and why?
2. **Defined scope** — What's in and what's out?
3. **Success criteria** — How do we know when it's done?
4. **Agent assignments** — Who does what?
5. **File manifest** — What files will be created?

---

## Documentation Standards

- Every file begins with: who created it, why, what it does, and what it feeds
- No orphan files — everything connects to something
- Version notes when updating existing files

---

## Communication

- Report progress at key milestones
- Flag blockers immediately
- Summarize decisions made

---

## Quality Checks

- Verify file creation before marking complete
- Cross-reference with original requirements
- Test or validate output when possible
```

---

## FILE 4: `~/.openclaw/workspace/projects/project_creation/references/WORKFLOW.md`

```markdown
# Project Workflow

> Purpose: Standard workflow for project execution | Fed by: Template_v2.md | Feeds: Generated projects

---

## Standard Flow

1. **Receive idea** — User or upstream system provides concept
2. **Clarify scope** — Ask questions if requirements are ambiguous
3. **Plan structure** — Determine folders, files, agents needed
4. **Assign agents** — Match tasks to capabilities
5. **Execute** — Build in order of dependencies
6. **Verify** — Confirm outputs match requirements
7. **Report** — Summarize what was created

---

## Handoff Protocol

When passing work to another agent:

- Provide complete context (don't assume prior knowledge)
- Specify exact deliverables
- Set clear success criteria
- Define timeout or escalation path

---

## Progress Saving

- Save state every 30 minutes minimum
- Write checkpoint files for long-running tasks
- Enable clean handoff to future sessions

---

## Error Handling

- Log failures with context
- Attempt one retry with adjusted approach
- Escalate to user if blocked twice
- Never silently fail and mark complete
```

---

## STEP 5: Add Routing to primary.md

After creating all files, the agent must add these routing lines to the user's `primary.md`:

**Add to the Routing section:**

```markdown
- Create a project → ~/.openclaw/workspace/projects/project_creation/Template_v2.md
- How do I create a project → ~/.openclaw/workspace/primary/primary_library/how_to_create_a_project.md
```

If the user does not have a `primary.md`, create one at `~/.openclaw/workspace/primary/primary.md` with this content:

```markdown
# Primary

> Purpose: Master index for Primary Library and routing | Feeds: All routed actions

---

## File Paths

- Primary folder: ~/.openclaw/workspace/primary/
- Primary Library: ~/.openclaw/workspace/primary/primary_library/

---

## Primary Library Contents

- [How to Create a Project](~/.openclaw/workspace/primary/primary_library/how_to_create_a_project.md)

---

## Routing

- Create a project → ~/.openclaw/workspace/projects/project_creation/Template_v2.md
- How do I create a project → ~/.openclaw/workspace/primary/primary_library/how_to_create_a_project.md

---

## Role & Scope

Main agent directive layer. Contains system-wide instructions and routing.

---

## Delegation Check

Before starting any task:
1. Is this work I should do? → NO (unless composing a chat reply)
2. Which agent fits? → Check your AGENTS.md
3. Spawn that agent → sessions_spawn
4. Wait and collect results
```

---

## FINAL INSTRUCTIONS FOR THE AGENT

After reading this entire file, you must:

1. ✅ Create folder: `~/.openclaw/workspace/primary/primary_library/` (if not exists)
2. ✅ Create folder: `~/.openclaw/workspace/projects/project_creation/references/` (if not exists)
3. ✅ Create FILE 1 at the specified path
4. ✅ Create FILE 2 at the specified path
5. ✅ Create FILE 3 at the specified path
6. ✅ Create FILE 4 at the specified path
7. ✅ Add routing lines to primary.md (or create it if missing)
8. ✅ Report to user: "Project Creation System deployed successfully. Say 'How do I create a project?' to test it."

---

**End of Deployable File**
