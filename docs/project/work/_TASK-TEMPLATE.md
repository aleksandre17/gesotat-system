---
id: TASK-TEMPLATE
type: WORK
title: Work item template
status: NOT_STARTED
authority: SUPPORTING
scope: template only — never executed, never counted as work
created: 2026-09-20
updated: 2026-09-20
---

# TASK-nnn — <one line, what changes>

> Copy this file; do not edit it in place. Replace every `<…>` placeholder.
> The front matter above belongs to the template itself. A real item replaces it with:
> `id: TASK-nnn`, its own `title`, a `status` legal for its folder, `authority: SUPPORTING`,
> `phase`, `gate`, `created`, `updated`.

## Objective

<The outcome, in one or two sentences. Not the method — the finished state.>

## Why now

<What makes this item permitted at this point in the lifecycle. Name the phase and gate
from MANIFEST.md. An item that cannot answer this is not ready to start.>

## required_context

<Ordered. Everything a successor who knows nothing must read, and nothing more —
an over-long list is as useless as an empty one. Paths are validated.>

1. `docs/project/CURRENT.md`
2. `docs/project/MANIFEST.md`
3. <the artifact this item actually acts on>

## Scope

**In scope**

- <bounded, checkable>

**Out of scope**

- <name what a reasonable person might assume is included but is not>

## Preconditions

- <state that must already hold; unmet preconditions mean the item belongs in `blocked/`>

## Deliverable

- <the artifact or change that must exist when this closes>

## Verification

| Check | Command / method | Evidence required |
|---|---|---|
| <what> | `<command>` | <exact output or artifact proving it> |

Include `python ops/cli/validation/rcp-verify.py` whenever the item touches the control
plane, and the applicable behavioural checks from `/AGENTS.md` whenever it touches the
platform.

## Dependencies

- <`TASK-nnn`, or "none". Validated: a dependency must exist.>

## Handoff

<Written **during** the work, not at the end: where it stands, what was tried, what the
next concrete step is. If the session ends here, this paragraph is the entire inheritance.>

## Closure record

- **Outcome:** <COMPLETE | CANCELLED | SUPERSEDED> — <reason if not COMPLETE>
- **Evidence:** <link or verbatim result>
- **Follow-ups raised:** <AIR-#### entries, or "none">
