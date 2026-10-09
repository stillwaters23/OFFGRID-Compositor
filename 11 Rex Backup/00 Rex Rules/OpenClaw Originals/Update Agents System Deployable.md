# Update Agents System — Deployable Blueprint

> **Version:** 4.0 | **Last Updated:** 2026-07-04

> **Main Agent:** Read this file. Follow the instructions below to construct an Agent Testing System on your system.

> **If these file structures already exist on your system:** Do not recreate them. Follow the paths, read the existing files, and add only what is missing. This deployable is additive, not destructive.

---

## What This Creates

This deployable packages the entire Agent Testing System v4.0 as a single self-contained file. When executed, it creates:

```
[workspace]/
├── HEARTBEAT.md (updated with Read section)
├── AGENTS.md (created if missing, populated at run time)
├── PRIMARY/
│   ├── primary.md (routing updated)
│   └── primary_library/
│       ├── OG/
│       │   └── Update_Agents/
│       │       └── Deployable_Update_Agents.md (OG copy)
│       └── update_agents/
│           ├── ORCHESTRATOR.md
│           ├── TESTS.md
│           ├── CATEGORIES.md
│           ├── RETEST.md
│           ├── README.md
│           ├── RESULTS_TEMPLATE.md
│           ├── GRID_TEMPLATE.md
│           ├── AGENTS.md (project-local grid, populated at run time)
│           └── results/
│               ├── STATE.md (process state)
│               └── test_read_target.txt (test fixture)
```

---

## Instructions

Execute each step in order. Each step is idempotent — if the target already exists and is non-empty, skip.

### Step 1 — Create update_agents folder

```
[workspace]/PRIMARY/primary_library/update_agents/
```

Check if folder exists. If not, create it. Verify creation.

### Step 2 — Create results folder

```
[workspace]/PRIMARY/primary_library/update_agents/results/
```

Check if folder exists. If not, create it. Verify creation.

### Step 3 — Create OG folder

```
[workspace]/PRIMARY/primary_library/OG/Update_Agents/
```

Check if folder exists. If not, create it. Verify creation.

### Step 4 — Write ORCHESTRATOR.md

Read the content below (lines inside the code block). If the file exists and is non-empty, skip this step. Write the content to:

```
[workspace]/PRIMARY/primary_library/update_agents/ORCHESTRATOR.md
```

```markdown
SYSTEM_HOME = [workspace]/PRIMARY/primary_library/update_agents/ — all relative paths resolve against SYSTEM_HOME.

# Orchestrator — Agent Testing System v4.0

## Purpose
Orchestrator coordinates the full agent testing cycle: read all config files, load agent queue, run test batteries per category, apply retest logic, compile results into GRID_TEMPLATE.md, and populate workspace root AGENTS.md.

## Files Used (all relative to SYSTEM_HOME)

| File | Purpose |
|------|---------|
| TESTS.md | Test battery definitions (R1, W1, L1, C1, S1, V1) |
| CATEGORIES.md | Category definitions and test applicability |
| RETEST.md | Retest logic, strike rules, timeout handling |
| RESULTS_TEMPLATE.md | Per-agent results structure |
| GRID_TEMPLATE.md | Final grid compilation format |
| README.md | System overview |
| results/STATE.md | Process state tracking |

## Paths Resolution

All paths are relative to SYSTEM_HOME:
- `SYSTEM_HOME/TESTS.md` — test definitions
- `SYSTEM_HOME/CATEGORIES.md` — category rules
- `SYSTEM_HOME/RETEST.md` — retest logic
- `SYSTEM_HOME/RESULTS_TEMPLATE.md` — per-agent template
- `SYSTEM_HOME/GRID_TEMPLATE.md` — grid template
- `SYSTEM_HOME/results/STATE.md` — process state
- `SYSTEM_HOME/results/{agent_id}.md` — per-agent test file
- `SYSTEM_HOME/results/test_read_target.txt` — R1 test fixture

When spawning sub-agents, convert all relative paths to absolute paths before sending prompts.

## Execution Flow

### Phase 1: Setup

1. Read `results/STATE.md` — confirm Phase is NOT STARTED or RUNNING
2. Set Phase to TESTING
3. Read `CATEGORIES.md` — confirm category definitions and rules
4. Read `TESTS.md` — confirm test battery definitions
5. Read `RETEST.md` — confirm retest and strike rules
6. Set Drift Canary to current timestamp
7. Set Handoff to self (orchestrator)
8. Build agent queue from AGENTS.md — populate queue with untested or UNTESTED/UNRELIABLE agents

### Phase 2: Per-Agent Testing

For each agent in queue:

1. **Read AGENTS.md** — get agent ID, model, type, VRAM, category, status
2. **Skip if VERIFIED** and last test date is within 30 days and config unchanged
3. **Create per-agent test file** by copying RESULTS_TEMPLATE.md to `results/{agent_id}.md`
4. **Fill metadata** — agent_id, model, type, VRAM, category, test date, status
5. **Read TESTS.md** and **CATEGORIES.md** to determine applicable tests
6. **Run each applicable test**:
   - Read the test definition from TESTS.md
   - Spawn sub-agent with EXACT prompt from TESTS.md
   - Use tool path substitutions (absolute paths for Read/Write tests)
   - Capture raw output (first 500 chars)
   - Verify per pass criteria in TESTS.md
   - Record result in per-agent test file with timestamp

7. **After all tests complete for this agent:**
   - Apply RETEST.md logic — check strike counts, schedule retests
   - Set Overall Status to COMPLETE or FAILED
   - Set verification status (VERIFIED, UNRELIABLE, TESTING)
   - Update Drift Canary to current timestamp

### Phase 3: Compile Results

1. Read GRID_TEMPLATE.md — load template structure
2. Fill the grid with each agent's test results (Read/Write/Reason/Code/Search/Vision columns)
3. Fill the Quick Pick table with best available for each task
4. Fill Known Limitations with any FAILED or CANT entries
5. Write compiled grid to `SYSTEM_HOME/AGENTS.md` (project-local)

### Phase 4: Update Root AGENTS.md

1. Read workspace root `AGENTS.md` (or create minimum viable file if missing)
2. Parse existing sections — preserve routing and constraint headers
3. Replace capability data section with compiled grid from GRID_TEMPLATE.md
4. Preserve all non-agent data in AGENTS.md (constraints, quick pick, DO NOT USE, spawnable IDs)

### Phase 5: Handoff & Cleanup

1. Set Phase to DONE
2. Set Handoff to self
3. Set Drift Canary to current timestamp
4. Clear agent queue (empty array)
5. Generate completion summary: agents tested, PASS/FAIL counts, retests needed
6. Write STATE.md with final state
7. If this was a retest cycle after system drift, also re-run all agents to rebuild from scratch

## Handoff Rules

- Handoff to another system: Set Handoff to another system, update routing in primary.md
- Handoff to self (next cycle): Set Handoff to self, set Phase to NOT STARTED, keep results for reference
- Handoff triggered by: user instruction, system drift detection (>3 days since last run), config change detection

## DONE State

When complete, STATE.md should show:
- Phase: DONE
- Agent Queue: (empty)
- Last Agent Tested: {last agent ID}
- Drift Canary: current timestamp
- Handoff: self (or target system)
- Notes: completion summary

If any results are empty or incomplete after a run:
- Do NOT mark Phase as DONE
- Log anomalies in STATE.md notes
- Set Phase back to NOT STARTED
- Re-run from Phase 1
```

### Step 5 — Write TESTS.md

```
[workspace]/PRIMARY/primary_library/update_agents/TESTS.md
```

If the file exists and is non-empty, skip this step. Write the content below:

```markdown
# Test Battery — Agent Testing System v4.0

> **6 tests. Each is a separate spawn (context reset). Each has binary verification.**

---

## Test Matrix

| ID | Name | Capability | Applicable Categories | Timeout (Cloud/Local) |
|----|------|-----------|----------------------|----------------------|
| R1 | Read File | Read | A, D | 30s / 60s |
| W1 | Write File | Write | A, D | 30s / 60s |
| L1 | Reasoning | Reason | A, B, D | 60s / 120s |
| C1 | Code Gen | Code | A, B, D | 60s / 120s |
| S1 | Web Search | Search | A, D | 30s / 60s |
| V1 | Vision | Vision | A, D | 60s / 120s |

---

## R1 — Read File

**Setup:** Orchestrator creates `results/test_read_target.txt` with content:
`The quick brown fox jumps over the lazy dog. Agent score: 95.23.`
Orchestrator substitutes the absolute path when sending the prompt.

**Prompt (send EXACTLY this, replacing {read_target_path} with the absolute path):**
```
Read the file at {read_target_path} and tell me the exact text on the first line. Output only the text, nothing else.
```

**Pass Criteria:**
- Response contains the exact text: "The quick brown fox jumps over the lazy dog. Agent score: 95.23."

**Fail Criteria:**
- Response does not contain the exact text
- Response contains fabricated or altered content
- Agent says it cannot read files (record as CANT, not FAIL)

**Verification:**
Orchestrator compares agent response to the known file content. Exact match = PASS. Any difference = FAIL.

**Hallucination Check:**
If agent adds commentary, changes the text, or invents content not in the file = FAIL.

---

## W1 — Write File

**Setup:** Orchestrator defines the output path as `results/{agent_id}/write_test.txt` and substitutes the absolute path when sending the prompt.

**Prompt (send EXACTLY this, replacing {agent_id} and {write_path} with absolute paths):**
```
Write the text "Hello from {agent_id}" to a file at {write_path}. Confirm when done.
```

**Pass Criteria:**
- File exists at the specified path
- File content exactly matches "Hello from {agent_id}"

**Fail Criteria:**
- File does not exist
- File content does not match
- Agent says it cannot write files (record as CANT, not FAIL)

**Verification:**
Orchestrator reads the file back after agent confirms. Content must match exactly.

**Hallucination Check:**
If agent claims to have written the file but the file does not exist = FAIL (hallucinated completion).

---

## L1 — Reasoning

**Setup:** None required. Pure reasoning test.

**Prompt (send EXACTLY this):**
```
Count the letter 'e' in the word "DEEPSEEK". Then tell me: is that count even or odd? Answer in this exact format: COUNT: X, EVEN_ODD: Y
```

**Pass Criteria:**
- Response contains "COUNT: 4" (the number 4)
- Response contains "EVEN_ODD: EVEN" (the word EVEN)

**Fail Criteria:**
- Wrong count (not 4)
- Wrong even/odd judgment (not EVEN)
- Response does not follow the required format
- Agent refuses to answer

**Verification:**
Orchestrator checks response contains both "4" as the count and "EVEN" as the judgment.

**Expected answer:** COUNT: 4, EVEN_ODD: EVEN

**Correct count:** DEEPSEEK = D, E, E, P, S, E, E, K = 4 e's. 4 is EVEN.

---

## C1 — Code Generation

**Setup:** None required. Agent generates code as text output.

**Prompt (send EXACTLY this):**
```
Write a Python function called "add" that takes two arguments and returns their sum. Output only the Python code, nothing else. No explanation, no markdown formatting.
```

**Pass Criteria:**
- Response contains "def add" (function definition)
- Response contains "return" statement
- Response is valid Python syntax (no obvious syntax errors)

**Fail Criteria:**
- No function definition
- No return statement
- Code is not valid Python (syntax errors, incomplete)
- Agent says it cannot write code (record as CANT, not FAIL)

**Verification:**
Orchestrator checks response text for "def add" and "return". Both present = PASS.

**Note for Handoff Agents (Category B):**
Handoff agents produce code as text. This is the correct behavior. The orchestrator does NOT need to execute the code — only verify the code is syntactically valid Python with the required elements.

---

## S1 — Web Search

**Setup:** Web search tool must be available. If not, skip and record NA.

**Prompt (send EXACTLY this):**
```
Search the web for "What year is it?" and tell me the current year. Output only the year, nothing else.
```

**Pass Criteria:**
- Response contains the current year (orchestrator verifies against system date)

**Fail Criteria:**
- Wrong year (does not match current year per system date)
- No response
- Agent says it cannot search (record as CANT, not FAIL)
- Agent fabricates an answer without searching (hallucination = FAIL)

**Verification:**
Orchestrator checks response contains the current year (per system date).

**Hallucination Check:**
If the agent returns a year without performing a search (no search tool used), this may indicate hallucination. Flag for review.

---

## V1 — Vision

**Setup:** Orchestrator provides an image path. Use any existing test image in the workspace, or create a simple one with image_generate.

**Prompt (send EXACTLY this, replacing {image_path}):**
```
Analyze the image at {image_path}. What shapes and colors do you see? Describe what you observe.
```

**Pass Criteria:**
- Response describes visual content (shapes, colors, objects)
- Response is not empty or a refusal

**Fail Criteria:**
- Response is empty
- Agent refuses to analyze image
- Agent says it cannot process images (record as CANT, not FAIL)
- Agent hallucinates content not present in the image

**Verification:**
Orchestrator checks response contains descriptive language about visual elements (shapes, colors, objects).

**Note:**
Only run this test for agents with vision capability (Category A with VL, or Category C vision specialists).

---

## TIMEOUT HANDLING

If an agent does not respond within the timeout:
- Record as FAIL with classification TIMEOUT
- Schedule retest per RETEST.md with 2x timeout
- Do NOT wait longer than 2x timeout on retest

---

## SKIP RULES

| Condition | Action |
|-----------|--------|
| Agent category excludes test | Record NA, skip |
| Required tool unavailable | Record NA, skip |
| Agent is Category C (specialist) | Run only specialty test |
| Agent is Category D (local main) | Mark MANUAL TEST REQUIRED |
```

### Step 6 — Write CATEGORIES.md

```
[workspace]/PRIMARY/primary_library/update_agents/CATEGORIES.md
```

If the file exists and is non-empty, skip this step. Write the content below:

```markdown
# Categories — Agent Testing System v4.0

## Category Definitions

### Category A: Core Agents
- General-purpose agents (both cloud and local inference)
- Must pass ALL applicable tests (R1, W1, L1, C1, S1, V1 as applicable)
- Expected capabilities: Read, Write, Reason, Code, Search, Vision (as applicable)
- Test all 6 tests (minus Vision unless VL capable)

### Category B: Handoff Agents
- Agents that produce code or structured output for orchestrator to execute
- Must pass: R1 (Read), L1 (Reason), C1 (Code)
- Do NOT execute their code — verify the code is syntactically valid (C1)
- Record PASS on C1 if output contains syntactically valid code with expected elements

### Category C: Specialist Agents
- Agents with a single specialization (e.g., Eyes = Vision)
- Run ONLY the specialty test (V1 for Eyes)
- All other tests recorded as NA

### Category D: Local Main Agents
- Local agents (Ollama)
- Pass all applicable tests
- May have VRAM constraints affecting concurrent usage
- Test: R1, W1, L1, C1, S1 (V1 if VL capable)
- Record Vision as NA if not VL-capable

## Category Mapping by Testing Rules

| Category | R1 Read | W1 Write | L1 Reason | C1 Code | S1 Search | V1 Vision |
|----------|---------|----------|-----------|---------|-----------|-----------|
| A | Yes | Yes | Yes | Yes | Yes | Yes (if VL) |
| B | Yes | Yes | Yes | Yes | No | No |
| C | No | No | No | No | No | Only specialty |
| D | Yes | Yes | Yes | Yes | Yes | NA (unless VL) |

## Category Assignment Rules

| Agent Type | Category |
|-----------|----------|
| Cloud general (any cloud model) | A |
| Local general (any local model) | D |
| Code specialist (any coder model) | B or D depending on usage |
| Vision specialist (Eyes) | C |
| Handoff/proxy agents | B |

## Verification Notes

- Category A agents MUST be VERIFIED across all applicable capabilities before being recommended
- Category B agents are verified on capability (not execution fidelity)
- Category C agents are verified on their specialty — other columns are NA
- Category D agents are verified on all applicable tests
```

### Step 7 — Write RETEST.md

```
[workspace]/PRIMARY/primary_library/update_agents/RETEST.md
```

If the file exists and is non-empty, skip this step. Write the content below:

```markdown
# Retest Protocol — Agent Testing System v4.0

## Retest Rules

| Condition | Action |
|-----------|--------|
| Any test FAIL | Record strike, schedule retest |
| 2 strikes on same test | Retest with same config |
| 3 strikes on same test | Mark as FAIL in grid, mark agent as UNRELIABLE |
| TIMEOUT on first attempt | Record FAIL, retest with 2x timeout |
| CANT classification | No retest — record as permanent limitation |
| HALLUCINATION | Record as FAIL with classification HALLUCINATION, no retest (critical) |

## Strike Tracking

- Each test may accumulate up to 3 strikes
- Strikes reset only when retest PASS occurs (strike count returns to 0)
- Maximum 3 strikes before permanent FAIL classification
- Record all strikes in the Retest Queue section of per-agent results

## Retest Logic

### When to Retest
1. **Immediate retest:** After 1 FAIL, wait 5 minutes, then retest with same config
2. **Second retest:** After 2nd FAIL, retest with same config once more
3. **Permanent failure:** After 3rd FAIL, stop testing this capability for this agent

### Retest Priority
1. R1 and W1 retests first (foundational capabilities)
2. L1 retest (reasoning is critical)
3. C1 retest (code quality matters but lower criticality)
4. S1 retest (search depends on external factors)
5. V1 retest (vision-specific, run last)

## Retest Checklist

1. Read the specific test definition from TESTS.md
2. Read RETEST.md for strike count on this test
3. If strikes < 3, spawn retest with same EXACT prompt
4. If timeout, use 2x original timeout
5. Record result, update strike count
6. If strike reaches 3, mark agent capability as FAIL in results
7. Add to GRID_TEMPLATE.md Known Limitations table

## Timeout Exponential Backoff

| Attempt | Timeout Multiplier | Description |
|---------|-------------------|-------------|
| 1st attempt | 1x | Original timeout from TESTS.md |
| 1st retest | 1x | Same timeout after 5-min wait |
| 2nd retest | 1x | Same timeout |
| Timeout fail | 2x | Retry with double timeout on next cycle |
```

### Step 8 — Write RESULTS_TEMPLATE.md

```
[workspace]/PRIMARY/primary_library/update_agents/RESULTS_TEMPLATE.md
```

If the file exists and is non-empty, skip this step. Write the content below:

```markdown
# Results Template — Agent Testing System v4.0

## flow in/out
- **Created by:** Agent Testing System v4.0
- **Used by:** Orchestrator (copies this template per agent)
- **Feeds:** GRID_TEMPLATE.md (final grid compilation)

## How to Use
Orchestrator copies this file to `results/{agent_id}.md` for each agent being tested. Fill in results as tests complete.

## Template

Copy everything below this line for each agent:

---

# Agent Results: {agent_id}

## Metadata
- **Agent ID:** {agent_id}
- **Model:** {model}
- **Type:** {cloud|local}
- **VRAM:** {GB or N/A}
- **Category:** {A|B|C|D}
- **Test Date:** {YYYY-MM-DD}
- **Overall Status:** {TESTING|COMPLETE|FAILED}

## Test Results

### R1 — Read File
- **Status:** {PASS|FAIL|CANT|NA}
- **Classification:** {N/A|TIMEOUT|CRASH|INCOMPLETE|WRONG|HALLUCINATION}
- **Strikes:** {0|1|2|3}
- **Raw Output:** {agent response, first 500 chars}
- **Verification:** {how orchestrator verified}
- **Timestamp:** {HH:MM:SS}
- **Notes:** {any observations}

### W1 — Write File
- **Status:** {PASS|FAIL|CANT|NA}
- **Classification:** {N/A|TIMEOUT|CRASH|INCOMPLETE|WRONG|HALLUCINATION}
- **Strikes:** {0|1|2|3}
- **Raw Output:** {agent response, first 500 chars}
- **Verification:** {how orchestrator verified}
- **Timestamp:** {HH:MM:SS}
- **Notes:** {any observations}

### L1 — Reasoning
- **Status:** {PASS|FAIL|CANT|NA}
- **Classification:** {N/A|TIMEOUT|CRASH|INCOMPLETE|WRONG|HALLUCINATION}
- **Strikes:** {0|1|2|3}
- **Raw Output:** {agent response, first 500 chars}
- **Verification:** {how orchestrator verified}
- **Timestamp:** {HH:MM:SS}
- **Notes:** {any observations}

### C1 — Code Generation
- **Status:** {PASS|FAIL|CANT|NA}
- **Classification:** {N/A|TIMEOUT|CRASH|INCOMPLETE|WRONG|HALLUCINATION}
- **Strikes:** {0|1|2|3}
- **Raw Output:** {agent response, first 500 chars}
- **Verification:** {how orchestrator verified}
- **Timestamp:** {HH:MM:SS}
- **Notes:** {any observations}

### S1 — Web Search
- **Status:** {PASS|FAIL|CANT|NA}
- **Classification:** {N/A|TIMEOUT|CRASH|INCOMPLETE|WRONG|HALLUCINATION}
- **Strikes:** {0|1|2|3}
- **Raw Output:** {agent response, first 500 chars}
- **Verification:** {how orchestrator verified}
- **Timestamp:** {HH:MM:SS}
- **Notes:** {any observations}

### V1 — Vision
- **Status:** {PASS|FAIL|CANT|NA}
- **Classification:** {N/A|TIMEOUT|CRASH|INCOMPLETE|WRONG|HALLUCINATION}
- **Strikes:** {0|1|2|3}
- **Raw Output:** {agent response, first 500 chars}
- **Verification:** {how orchestrator verified}
- **Timestamp:** {HH:MM:SS}
- **Notes:** {any observations}

## Capability Summary

| Capability | Status | Notes |
|-----------|--------|--------|
| Read | {PASS/FAIL/CANT/NA} | {brief} |
| Write | {PASS/FAIL/CANT/NA} | {brief} |
| Reason | {PASS/FAIL/CANT/NA} | {brief} |
| Code | {PASS/FAIL/CANT/NA} | {brief} |
| Search | {PASS/FAIL/CANT/NA} | {brief} |
| Vision | {PASS/FAIL/CANT/NA} | {brief} |

## Retest Queue
{List any tests needing retest with strike count and classification, or "None"}

---
```

### Step 9 — Write GRID_TEMPLATE.md

```
[workspace]/PRIMARY/primary_library/update_agents/GRID_TEMPLATE.md
```

If the file exists and is non-empty, skip this step. Write the content below:

```markdown
# AGENTS.md Grid Template — Agent Testing System v4.0

## flow in/out
- **Created by:** Agent Testing System v4.0
- **Used by:** Orchestrator (fills at end of testing cycle)
- **Feeds:** Workspace root AGENTS.md

## How to Use
After all agents are tested, the orchestrator fills this template with verified results and writes it to both:
1. The project AGENTS.md
2. The workspace root AGENTS.md (capability data section only — preserve routing/constraint sections from existing AGENTS.md)

## Template

Copy everything below this line:

---

# Agent Capability Grid

**Purpose:** Smart routing for orchestrator agent selection. Read this grid to pick the right agent for any task.
**Last Updated:** {YYYY-MM-DD}
**Test System:** Agent Testing System v4.0

## Selection Grid

| ID | Model | Type | VRAM | Read | Write | Reason | Code | Search | Vision | Status | Best For |
|----|-------|------|------|------|-------|--------|------|--------|--------|--------|----------|
| {agent_id} | {model} | {cloud/local} | {GB} | {PASS/FAIL/CANT/NA} | {PASS/FAIL/CANT/NA} | {PASS/FAIL/CANT/NA} | {PASS/FAIL/CANT/NA} | {PASS/FAIL/CANT/NA} | {PASS/FAIL/CANT/NA} | {VERIFIED/TESTING/UNRELIABLE/UNTESTED} | {one-liner} |

## Capability Legend

| Status | Meaning |
|--------|---------|
| PASS | Agent successfully completed this test |
| FAIL | Agent failed this test after 3 strikes (confirmed limitation) |
| CANT | Agent explicitly stated it cannot perform this task (legitimate limitation) |
| NA | Test not applicable to this agent's category |
| VERIFIED | All applicable tests complete, no pending retests |
| TESTING | Tests in progress or retests pending |
| UNRELIABLE | Agent has 2+ confirmed failures across capabilities |
| UNTESTED | Agent has not been tested yet |

## Quick Pick

| Task | First Choice | Fallback |
|------|-------------|----------|
| Read files | {agent_id} | {agent_id} |
| Write files | {agent_id} | {agent_id} |
| Reasoning | {agent_id} | {agent_id} |
| Write code | {agent_id} | {agent_id} |
| Web search | {agent_id} | {agent_id} |
| Image analysis | {agent_id} | {agent_id} |

## Known Limitations

| Agent | Cannot Do | Reason | Confirmed |
|-------|----------|--------|-----------|
| {agent_id} | {capability} | {CANT or FAIL classification} | {date} |

## Deployment Notes

- Free agents prioritized: {list free cloud agents}
- Local VRAM constraint: {total VRAM} — max 2 concurrent, pairing rules apply
- Category B agents (handoff): {list} — code output only, orchestrator executes

---
```

### Step 10 — Write README.md

```
[workspace]/PRIMARY/primary_library/update_agents/README.md
```

If the file exists and is non-empty, skip this step. Write the TRIMMED content below (warroom/archive references removed):

```markdown
# Agent Testing System v4.0

## Quick Overview

The Agent Testing System v4.0 is a framework for systematically testing agent capabilities, managing test state, and populating an Agent Capability Grid with verified results.

## System Components

| File | Role |
|------|------|
| ORCHESTRATOR.md | Coordinates the testing cycle |
| TESTS.md | Defines 6 binary tests (Read, Write, Reason, Code, Search, Vision) |
| CATEGORIES.md | Categorizes agents (A/B/C/D) with test applicability rules |
| RETEST.md | Strike-based retest logic and timeout handling |
| RESULTS_TEMPLATE.md | Per-agent results structure |
| GRID_TEMPLATE.md | Agent grid template (compiled from test results) |

## Workflow Summary

1. **Setup:** Read config files, set Phase, build agent queue
2. **Per-Agent Testing:** Apply tests from TESTS.md based on agent category from CATEGORIES.md
3. **Retest Handling:** Apply RETEST.md strike rules
4. **Compilation:** Fill GRID_TEMPLATE.md, propagate to workspace root AGENTS.md
5. **Handoff:** Update STATE.md with Phase = DONE, clear queue, update routing in primary.md

## Testing Principles

- **Baby steps and verification:** One test at a time, verify before proceeding
- **Never trust claims:** Always verify with file I/O or tool output
- **Binary results:** Each test is PASS or FAIL — no partial credit
- **Hallucination is critical:** Fabricated results = FAIL with HALLUCINATION classification
- **Idempotent design:** Every step checks existence before creating

## Categories

- **A (Core):** Full test battery for general-purpose agents
- **B (Handoff):** Code/structured output for orchestrator execution
- **C (Specialist):** Single-specialty test only
- **D (Local):** Full test battery for local inference agents

## Flow

- **Input:** Agent configuration, test battery definitions
- **Output:** Verified Agent Capability Grid in AGENTS.md
- **Feeds:** Routing decisions in ORCHESTRATOR.md and workspace root AGENTS.md

---

## Deployment

To set up on a new system, use the deployable at `PRIMARY/primary_library/OG/Update_Agents/Deployable_Update_Agents.md`.
```

### Step 11 — Write STATE.md (Blank Template)

```
[workspace]/PRIMARY/primary_library/update_agents/results/STATE.md
```

If the file exists and is non-empty, skip this step. Write the BLANK template below (structured, no current run data, Phase set to NOT STARTED, empty queue):

```markdown
# State — Agent Testing System v4.0

## Process State

| Field | Value |
|-------|-------|
| Phase | NOT STARTED |
| Last Agent Tested | (none) |
| Agent Queue | (empty) |
| Last Run Date | (none) |
| Total Agents Tested | 0 |
| Total Agents PASS | 0 |
| Total Agents FAIL | 0 |
| Tests Pending | 0 |
| Retest Queue | (empty) |

## Phases

| Phase | Status | Trigger |
|-------|--------|---------|
| NOT STARTED | Default | Fresh state or reset |
| SETUP | Phase 1 | Read all config files |
| TESTING | Phase 2 | Per-agent testing cycle |
| COMPILE | Phase 3 | Merge per-agent results into GRID_TEMPLATE.md |
| PROPAGATE | Phase 4 | Update workspace root AGENTS.md |
| DONE | Phase 5 | All runs complete, queue cleared |
| RETRY | Exception | Incomplete results — reset to NOT STARTED |

## Drift Canary

- **Value:** (none)
- **Description:** Timestamp of last successful system state. On system drift detection (>3 days since last update), re-run all agents from scratch.
- **Drift Detected:** false

## Handoff Triggers

| Trigger | Action |
|---------|--------|
| User requests system rebuild | Re-run all agents from Phase 1 |
| System drift >3 days | Re-run all agents from Phase 1 |
| Config change detected | Re-run all agents from Phase 1 |
| Manual maintenance | Update specific agent entries |
| Routing handoff | Set Handoff to target system, update primary.md |

## Handoff Mechanism

1. Set Handoff field to target system identifier
2. Update routing in `primary.md`:
   - Add or update line: `update agents → [workspace]/PRIMARY/primary_library/update_agents/ORCHESTRATOR.md`
3. Write Handoff to `results/STATE.md` with timestamp
4. Set Phase to NOT STARTED on target system
5. Verify target system has received and can read all config files

## DONE State

When all testing is complete and verified:
- Phase = DONE
- Agent Queue = (empty)
- Drift Canary = current timestamp
- Handoff = self or target system
- Total Agents PASS reflects final verified count

If incomplete results detected after a run:
- Do NOT mark Phase as DONE
- Log anomalies in Phase notes
- Set Phase back to NOT STARTED
- Re-run from Phase 1
```

### Step 12 — Write test_read_target.txt

```
[workspace]/PRIMARY/primary_library/update_agents/results/test_read_target.txt
```

If the file exists and is non-empty, skip this step. Write content:

```
The quick brown fox jumps over the lazy dog. Agent score: 95.23.
```

### Step 13 — Ask User for Vision Test Image

The V1 (Vision) test requires an image file. Ask the user:

**"The agent testing system needs an image for the vision test (V1). Please provide a path to any image file on your system, or say 'skip' to skip vision testing."**

- If the user provides a path: Save the path to `results/vision_test_image_path.txt` for use during testing.
- If the user says 'skip': Write 'SKIP' to `results/vision_test_image_path.txt`. Vision tests will be marked NA for all agents.
- If no webchat (running from heartbeat/cron): Check if `results/vision_test_image_path.txt` already exists. If not, write 'SKIP' and proceed. Vision tests will be NA until an image is provided.

Verify `results/vision_test_image_path.txt` exists.

### Step 14 — Copy Deployable to OG Folder

Copy this file to:

```
[workspace]/PRIMARY/primary_library/OG/Update_Agents/Deployable_Update_Agents.md
```

Check if file exists and is non-empty. If yes, skip. Otherwise, copy and verify.

### Step 15 — Create/Update HEARTBEAT.md

```
[workspace]/HEARTBEAT.md
```

Check if HEARTBEAT.md exists and is non-empty:
- **If exists and non-empty:** Back up the original to `HEARTBEAT.md.bak`. Then add or update a Read section pointing to `PRIMARY/primary.md` — if a Read section already exists pointing to primary.md, leave it. If the Read section points elsewhere, add the primary.md line.
- **If missing or empty:** Create with minimum content:

```markdown
# HEARTBEAT.md

**Status:** On

## Read
Read this on every heartbeat

"[workspace]/PRIMARY/primary.md"
```

### Step 16 — Create/Update primary.md

```
[workspace]/PRIMARY/primary.md
```

Check if primary.md exists and is non-empty:
- **If exists and non-empty:** Back up the original to `primary.md.bak`. Then check if routing line `update agents → [workspace]/PRIMARY/primary_library/update_agents/ORCHESTRATOR.md` already exists. If it already exists, skip. If it does not exist, add it under the Common Terms → Canonical Paths section. If that section does not exist, create it.
- **If missing or empty:** Create with minimum structure:

```markdown
# primary.md

## Synopsis

Main agent directive layer. Contains system-wide instructions, delegation rules, and strategic context.

## Common Terms → Canonical Paths

- update agents → [workspace]/PRIMARY/primary_library/update_agents/ORCHESTRATOR.md

## Unbreakable Standards

1. Every file goes to a directory.
2. One canonical path per purpose. No duplicates.
3. If in doubt, ask — never guess and create bloat.
```

### Step 17 — Create Stub AGENTS.md (if missing)

```
[workspace]/AGENTS.md
```

Check if AGENTS.md exists and contains an agent grid section:
- **If exists with grid content:** Skip. The grid will be populated by the next update agents run.
- **If missing or no grid content:** Create with minimal grid headers:

```markdown
# AGENTS.md

## Agent Selection Grid

> **Status:** STUB — grid populated by update agents run

## Selection Grid

| ID | Model | Type | VRAM | Read | Write | Reason | Code | Search | Vision | Status | Best For |
|----|-------|------|------|------|-------|--------|------|--------|--------|--------|----------|

## Quick Pick

| Task | First Choice | Fallback |
|------|-------------|----------|

## Notes
- Run update agents system to populate Agent Capability Grid with verified results.
```

---

## Path Substitutions

When executing or reviewing this deployable, apply these substitutions to all relative paths:

| Token | Resolves To |
|-------|-------------|
| `[workspace]` | `C:\Users\macma\.openclaw\workspace` |
| `SYSTEM_HOME` | `[workspace]/PRIMARY/primary_library/update_agents/` |

Conversion rules:
- All Windows backslash paths in source files are converted to forward slashes
- Absolute paths like `C:\Users\macma\...` are replaced with `[workspace]` prefix
- Project paths like `projects/System/Agent Selection/agent-system/` become `PRIMARY/primary_library/update_agents/`
- ORCHESTRATOR.md includes a SYSTEM_HOME directive at the top

---

## Verification Checklist

Complete this checklist as you execute each step. Mark [x] when verified.

### Folders
- [ ] Step 1: `PRIMARY/primary_library/update_agents/` created
- [ ] Step 2: `PRIMARY/primary_library/update_agents/results/` created
- [ ] Step 3: `PRIMARY/primary_library/OG/Update_Agents/` created

### Files (content verified)
- [ ] Step 4: `ORCHESTRATOR.md` — contains SYSTEM_HOME directive, execution flow, handoff rules
- [ ] Step 5: `TESTS.md` — contains all 6 tests (R1, W1, L1, C1, S1, V1) with pass/fail criteria
- [ ] Step 6: `CATEGORIES.md` — contains A/B/C/D category definitions and mapping table
- [ ] Step 7: `RETEST.md` — contains strike rules, retest logic, timeout backoff
- [ ] Step 8: `RESULTS_TEMPLATE.md` — contains per-agent template with all test sections
- [ ] Step 9: `GRID_TEMPLATE.md` — contains grid, legend, quick pick, limitations tables
- [ ] Step 10: `README.md` — contains system overview (warroom/archive refs removed)
- [ ] Step 11: `results/STATE.md` — contains Phases, Drift Canary, Handoff Triggers, Handoff Mechanism, DONE State; Phase is NOT STARTED
- [ ] Step 12: `results/test_read_target.txt` — content matches "The quick brown fox jumps over the lazy dog. Agent score: 95.23."

- [ ] Step 13: `results/vision_test_image_path.txt` — file exists with image path or SKIP

### Filesystem
- [ ] Step 14: OG copy at `OG/Update_Agents/Deployable_Update_Agents.md` — file exists
- [ ] Step 15: `HEARTBEAT.md` — Read section points to PRIMARY/primary.md
- [ ] Step 16: `primary.md` — routing line includes update agents path
- [ ] Step 17: `AGENTS.md` — stub grid with headers exists

### Total: 17 steps, all must be checked off

---

## Usage / Activation

### Activation Phrases

When the orchestrator receives any of these phrases from the user, execute the corresponding action:

| Trigger Phrase | Action |
|----------------|--------|
| "update agents" | Read ORCHESTRATOR.md, execute full testing cycle |
| "update agents deployable" | Read this file, execute setup steps |
| "retest agents" | Read RETEST.md, run retest protocol on agents with strikes |
| "test agents" | Run all tests on all untested/unverified agents |
| "check agents" | Read AGENTS.md, report current status summary |
| "force agents update" | Reset STATE.md to NOT STARTED, clear queue, re-run everything |

### Maintenance

| Task | Frequency | Steps |
|------|-----------|-------|
| View current status | On demand | Read STATE.md — check Phase, Last Run, Queue |
| Full retest cycle | Every 30 days | Reset STATE.md, re-run full testing |
| Quick retest | As needed | Read RETEST.md, test agents with strikes |
| Drift check | Every 3 days | Read STATE.md — compare Drift Canary to today's date |
| Grid regeneration | After config changes | Force full retest on affected agents |
| OG copy backup | After system rebuilds | Copy this deployable to OG folder |

### Troubleshooting

| Problem | Diagnose | Fix |
|---------|----------|-----|
| Agent grid not populating | Check STATE.md Phase | Ensure Phase is NOT DONE, check queue is not empty |
| Testing stops mid-cycle | Check logs for TIMEOUT or CRASH | Read RETEST.md for retry logic, check agent availability |
| Path errors in prompts | Check path substitution | Verify all absolute paths resolved before spawning |
| HEARTBEAT.md missing Read section | Check HEARTBEAT.md exists | Add or recreate with Read section per Step 14 |
| primary.md routing missing | Check primary.md exists | Add or update routing line per Step 15 |
| Agent returns CANT consistently | Read TESTS.md prompts for that test | Verify tool availability matches agent's declared capabilities |
| Retest queue not clearing | Check RETEST.md strike rules | Ensure retest PASS resets strike count to 0 |

### Deactivation

To stop the update agents system:
1. Read STATE.md
2. Set Phase to DONE (do not set to DELETED — keeps results intact)
3. Update primary.md — comment out or remove the routing line
4. HEARTBEAT.md — remove or comment out the Read section

---

## Deployment Complete

If all 16 steps are checked off, the Agent Testing System v4.0 is fully deployed and operational.

**Next actions:**
1. The orchestrator can now execute a testing cycle by reading `ORCHESTRATOR.md`
2. Run "test agents" or "update agents" to begin testing new/updated agents
3. Check `results/STATE.md` for current progress
4. AGENTS.md will be populated with verified results after the first run
5. Schedule periodic retest every 30 days

**System status:** Ready for first run.

---

*Deployable built: 2026-07-04*  
*Source System: Agent Testing System v4.0*  
*Files embedded: ORCHESTRATOR, TESTS, CATEGORIES, RETEST, RESULTS_TEMPLATE, GRID_TEMPLATE, README (trimmed), STATE (blank template)*
