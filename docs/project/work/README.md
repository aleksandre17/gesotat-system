---
id: CTRL-WORK
type: CONTROL
title: Work item mechanism
status: ACTIVE
authority: CANONICAL
scope: how bounded work is declared, carried and closed
created: 2026-09-20
updated: 2026-09-20
---

# WORK — the bounded work-item mechanism

A work item is the **only** sanctioned unit of change. It exists so that an interruption
costs nothing: everything a successor needs to resume is written down before the work
starts, not reconstructed afterwards.

**There are no work items right now, and that is a legal state.** This file is the
mechanism, not a container to be filled. Inventing placeholder items to make the folder
look populated is forbidden (RCP §9 — lazy instantiation).

---

## 1. States

Three sibling folders, **created on first use, never pre-created empty**:

| Folder | Meaning |
|---|---|
| `active/` | in progress; `status` must be one of `READY` `ACTIVE` `VERIFYING` |
| `blocked/` | cannot proceed; `status` must be `BLOCKED` and the blocker must be named |
| `completed/` | terminal; `status` must be `COMPLETE` `CANCELLED` or `SUPERSEDED` |

**The folder and the status must agree.** `rcp-verify.py` enforces this (RCP-601…603),
because a completed item sitting in `active/` is how a repository starts lying about what
is in flight.

Moving an item between folders is a `git mv` plus a `status` edit — both, always.

## 2. Creating one

1. Copy `_TASK-TEMPLATE.md` to `active/TASK-<nnn>-<kebab-slug>.md`.
2. Take the next free `<nnn>` — numbers are never reused, including by cancelled items.
3. Fill **every** field. A template field left as its placeholder is an incomplete item.
4. Set `CURRENT.md` → `ACTIVE WORK ITEM`. One active item at a time unless the phase
   explicitly permits parallel work.

Leading-underscore files and `README.md` are mechanism, not work, and are skipped by the
validator.

## 3. What makes an item resumable

`required_context` is the load-bearing field. It answers: *what must a successor who knows
nothing read before touching this?* It is a list of paths, ordered, and it is validated —
every path referenced by a work item must exist (RCP-605).

A dependency on another item is written as `` `TASK-nnn` `` in the body; the validator
resolves it across all three folders (RCP-606), so a dangling dependency cannot survive.

## 4. Closure

An item closes on **evidence**, not assertion. Record the command that was run and its
result. `/AGENTS.md` governs this repository's evidence standard, and it is stricter than
this file: a checklist box closes only with an evidence link, and missing evidence means
`OPEN / NOT READY` — never invented completion.

An item that ends without producing its deliverable is `CANCELLED` with the reason, not
quietly deleted. Deleting it destroys the record of why the approach failed, which is
usually the most valuable thing it produced.

## 5. Relationship to governance cards

`docs/work/cards/<id>/governance.json` is the platform protocol's own bounded change
record, mandated by `/AGENTS.md`. **It is not replaced by this mechanism and must not be
duplicated into it.** When a work item drives a change that requires a card, the item
references the card path; the card remains the requirement-mapping and evidence authority.

One responsibility, one authority (CAD-01): cards own requirement mapping and evidence;
work items own scheduling, context and state.
