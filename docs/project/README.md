---
id: CTRL-README
type: GUIDE
title: Project control plane — entry point
status: ACTIVE
authority: CANONICAL
scope: whole repository
created: 2026-09-20
updated: 2026-09-20
---

# Project control plane

**Start here.** This directory answers *where are we, what is next, and what may I read* —
nothing else.

It implements the **Repository Control Protocol**
(`docs/standards/PROJECT-OPERATING-SYSTEM.md`). That file is the generic protocol; this
directory is this project's instance of it. Keep them separate.

---

## The four artifacts

| File | Owns | Rule |
|---|---|---|
| **`CURRENT.md`** | current state only | the single current-state authority; stays small |
| **`MANIFEST.md`** | lifecycle phases and gates | defines what a state *means* |
| **`CATALOG.md`** | artifact navigation metadata | never duplicates content |
| **`ADOPTION.md`** | mapping of pre-existing artifacts | bootstrap only; retires when mapping completes |

## If you are starting a session

1. `/AGENTS.md` — the operating constitution.
2. `CURRENT.md` — state, next permitted action, forbidden actions, required reads.
3. The active work item, if any, and **only** the artifacts in its `required_context`.

**Do not read the documentation tree by default.** Context is a finite engineering
resource; the required-read set in `CURRENT.md` is deliberately short and is sufficient.

## If you are ending a session

Follow the end protocol in `/AGENTS.md`. The test is simple:

> Could a new agent, with no access to this conversation, continue correctly from the
> repository alone?

If no, the session is not closed.

## How to do common things

**Add a work item.** Create `work/active/TASK-###.md` with the fields in RCP §10 — the
non-negotiable one is `required_context`, which names exactly the canonical artifacts
needed. Register it in `CATALOG.md` and point `CURRENT.md` at it.

**Add a governed document.** Give it a YAML header (RCP §6) with `id`, `type`, `title`,
`status`, `authority`. Register it in `CATALOG.md`. **Before creating it, ask who already
owns that responsibility** — if an owner exists, extend or reference it (CAD-03).

**Supersede something.** Set `superseded_by` on the old artifact, `supersedes` on the new,
update `CATALOG.md`. **Never delete rationale**; navigation points forward, history stays
readable.

**Complete work.** Move it to `work/completed/`, promote any durable conclusion to its
canonical owner *first*, then update `CURRENT.md` and, if a gate changed, `MANIFEST.md`.

**Recover from an inconsistent control plane.** `CURRENT.md` is authoritative for state and
`MANIFEST.md` for lifecycle. If they disagree, `MANIFEST.md` defines the legal states and
`CURRENT.md` must be corrected to one of them — with the correction recorded, not silently
applied.

## What must never happen here

- A second current-state file, manifest or catalog.
- Architecture, findings or decisions written into a control artifact.
- A document whose authority must be guessed from its filename or its date.
- A new mechanism created beside an existing canonical authority (CAD-16).

## Where the knowledge actually lives

Control artifacts route; they do not hold knowledge.

| You want | Go to |
|---|---|
| why this control plane exists at all | `docs/decisions/ADR-repository-control-protocol.md` (ADR-013) |
| which plane owns which kind of knowledge | `CATALOG.md` → *Where each kind of knowledge lives* |
| findings, requirements, registers | `docs/work/GEOSTAT-API-RECOVERY-CONSOLIDATION-2026-09-20.md` |
| who is authoritative for a responsibility | `docs/work/GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md` (CAD-02) |
| what design may assume | `docs/work/GEOSTAT-API-RECOVERY-PASS3-CLOSURE-2026-09-20.md` §10 |
| platform engineering doctrine | `docs/reference/engineering/` |
| evidence behind a conclusion | the completeness audit and checkpoint — **L4, on demand only** |
