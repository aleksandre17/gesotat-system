---
id: CTRL-CURRENT
type: CONTROL
title: Current project state
status: ACTIVE
authority: CANONICAL
scope: whole repository
created: 2026-09-20
updated: 2026-09-21
---

# CURRENT

**Project:** GeoStat — a multi-site, **Contract-Driven, Metadata-Driven, Schema-Agnostic**
data-product platform. Declared contracts in a Control Plane drive ingestion, canonical
storage, publication and serving; new sites, providers and schemas are onboarded by
contract, not by changing core code. Identity and engineering law: `/AGENTS.md` and
`docs/reference/ENGINEERING-QUALITY-DOCTRINE.md`. Structure:
`docs/reference/CANONICAL-FULL-TREE.md`. **This file defines none of that — it points.**

**The single current-state authority for this repository.** If any other artifact appears
to state current project state, this one wins and that one is a defect.

This file is a pointer, not a narrative. It holds no history, architecture, findings or
decisions.

---

## State

| Field | Value |
|---|---|
| **LIFECYCLE PHASE** | `PHASE-004` — Canonical Design |
| **STAGE** | canonical architecture derived, revised against physical evidence and closed; 19/19 guarantees preserved or strengthened |
| **STATUS** | `COMPLETE` |
| **LAST COMPLETED GATE** | `GATE-DESIGN-ENTRY` → **PASS** (2026-09-21) |
| **CURRENT GATE** | `GATE-PLAN` → `READY` |
| **ACTIVE WORK ITEM** | none. `TASK-001` … `TASK-006` all `COMPLETE` |
| **CONTROL PLANE** | `RCP PASS` — **620 checks, 0 errors, 2 cohesion warnings, 46 governed artifacts** from the committed state (2026-09-21). Proven by 119 governance tests: `python ops/cli/validation/rcp-verify.py` · `python -m unittest discover -s ops/tests/governance`. A working tree also holding the three gitignored `codex-session-*.md` transcripts reports **626** — six extra cohesion checks over files that are deliberately never committed (`CF-044`). The committed figure is the reproducible one. |
| **BOUNDED CHANGE RECORD** | `docs/work/cards/canonical-design/governance.json` — `VERIFIED`; `python ops/cli/validation/engineering-governance.py` → `PASS` |
| **BASELINE** | `HEAD b46c043` · target 26 modified / 29 untracked · unchanged since recovery batch 1 |
| **LAST UPDATED** | 2026-09-21 |

## Blockers

**None.** `PHASE-004` closed with every baseline contradiction resolved and all 19 physical
anti-regression guarantees preserved or strengthened.

**Seven owner decisions are carried, none blocking `PHASE-005`:** disclosure-control policy ·
erasability classes and retention · `BM-Q-03` performance objectives · `BM-Q-05` ASVS subset ·
reference-data agency assignment · the materialization thresholds of `ARCH-CANONICAL` §31.4 ·
**Unicode NFC enforcement for text grain components** — added by the independent review
correction, owned by `ARCH-CANONICAL` §39 and derived in §31.6 residual 2. It is
**security- and integrity-relevant** and **blocks the first work that persists a text grain
component** — the `PROTECT` stage of that work, `PHASE-009` at the latest. It does not block
`PHASE-005` planning, and an agent may not settle it.

**Seven post-acceptance obligations travel into `PHASE-005`**, owned by `ARCH-CANONICAL` §40 and
§31.6 — the write-path permission model · the digest reconciliation check · DDL-audit detection
of a disabled trigger · executable §31.6 false-digest negative tests · executable §31.7
four-cardinality negative tests · the SQL↔Access correspondence table · the 19 `dbo.*`
elimination inventory. **None is implemented; §31.6 and §31.7 are derived, not executed.**

Two properties are **designed but unevidenced** and must not be reported as proven: a second
provider (`ADR-016` rule 6), and the generic kernel's behaviour at production volume
(`BM-Q-03`).

## Material unknowns

**None that can change canonical architecture.** Five Layer Coverage rows remain `PARTIAL`
(L5, L9, L16, L17, L40); each is design or protection-coverage work, owner recorded in the
Layer Coverage Ledger §3.2.

**The CF-044 transcript uncertainty is closed** (2026-09-20): 22 durable items extracted,
22 canonical owners, 0 unowned — `REC-CONSOLIDATION` §1.4a. No knowledge-loss risk remains
attached to the repository-root transcripts.

Live **platform** obligations (W-01…W-07, C-01…C-14, AIR, production evidence) are open but
belong to a separate program — `MANIFEST` → *Inherited platform obligations*. They gate no
rehabilitation phase.

**Six standards questions remain open** (`BM-Q-03…08`, `STD-BENCH-001` §9), each with an owner,
an interim rule and a resolution timing. **None gates `PHASE-004`**: `BM-Q-03` binds before
`PHASE-010`, `BM-Q-05` before `PHASE-009`, the rest are resolvable during design.

**Four comparative questions** (`AQ-01…AQ-04`, `AUD-COMPARATIVE` §22) are carried into design:
architecture choices the evidence frames but does not settle. `AQ-05` is **resolved** — the
authoritative artifact is the content `746487ce…`, bound by commit, published digest and two
dated runtime records; the api-path file is an unrecorded working copy (`AUD-COMPARATIVE` §23).

**Sixteen capabilities are missing from every artifact** (`CAP-M01…M16`, `AUD-CAPABILITY` §3
and §14) and **four envelope concerns have no owner at all** (tidy/long structures, product-like
objects, declarable `MANY_TO_MANY`, impact analysis — `AUD-CAPABILITY` §6). Design must add
them; recombining what exists is not sufficient.

**One question is for the owner, not the designer:** `D3` — how a legal erasure obligation is
reconciled with immutable snapshots, retained raw and append-only lineage (`AUD-CAPABILITY`
§12). It blocks nothing today and cannot be answered by architecture alone.

## Next permitted action

**`PHASE-005` — Master Rehabilitation Plan**, guarded by `GATE-PLAN`.

It consumes `ARCH-CANONICAL` (**Part II governs where the two parts disagree**), bounded by
`ARCH-BASELINE` and `ARCH-DOSSIER`. It produces a dependency-ordered plan — *what* is
rehabilitated, in what order, and why that order — and it implements nothing.

## Forbidden actions

- Reopening PASS 1 / 2A / 2B / 3 conclusions without **new primary evidence**.
- Beginning rehabilitation implementation, refactoring or legacy deletion.
- Beginning `PHASE-006` before `GATE-PLAN` passes.
- Making a per-structure physical table the default — it is a declared, reversible
  specialization (`ARCH-CANONICAL` §31.4), and defaulting to it loses the evolution property.
- Classifying a physical structure as accidental legacy merely because it is physical.
- Implementing, migrating or generating anything. `PHASE-004` produces a design, not code.
- Adopting any inspected artifact as the canonical model — none passes the invariant gate.
- Weakening, narrowing or re-deriving `STD-BENCH-001` to make a design pass.
- Creating any mechanism that parallels an existing canonical authority — see CAD-16.
- Modifying implementation, migrations, ACCDB/ZIP artifacts or runtime configuration under
  any recovery, benchmark or design work item.

## Required read set

Read in this order. **This is the whole mandatory set** — everything else is reference.

| # | Artifact | Why |
|---|---|---|
| 1 | `/AGENTS.md` | operating constitution and routing |
| 2 | this file | current state |
| 3 | `docs/project/MANIFEST.md` | lifecycle, gates, what is permitted next |
| 4 | `docs/standards/ARCHITECTURE-QUALITY-BENCHMARK.md` | **what "better" means, and how a candidate is judged** |
| 5 | `docs/standards/AUTOMATION-CLASSIFICATION-MODEL.md` | the automation class each responsibility must reach |
| 6 | `docs/work/GEOSTAT-API-RECOVERY-CONSOLIDATION-2026-09-20.md` | findings, requirements, registers |
| 7 | `docs/work/GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md` | authority registry + CAD-01…18 |
| 8 | `docs/work/GEOSTAT-API-RECOVERY-PASS3-CLOSURE-2026-09-20.md` §10 | the Recovery → Design handoff |
| 9 | `docs/work/GEOSTAT-CAPABILITY-INVENTORY-2026-09-21.md` §5 | **what design must preserve, eliminate, reconcile and add** |
| 10 | `docs/decisions/ADR-lifecycle-program-separation.md` | which programme may decide what |
| 11 | `docs/decisions/ADR-sdmx-conformance-boundary.md` | where SDMX applies, and where it must not |
| 12 | `docs/decisions/ADR-access-provider-status.md` | what may and may not depend on Access |
| 13 | `docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md` | **`Q01…Q50` — fifty decisions that already bind M1 semantics** |
| 14 | `docs/work/GEOSTAT-PHYSICAL-RELATIONAL-BASELINE-2026-09-21.md` | **the physical model any canonical decision must not regress** |
| 15 | `docs/work/GEOSTAT-PHYSICAL-DATA-DOSSIER-2026-09-21.md` | **the 19 anti-regression guarantees and the contradiction matrix** |
| 16 | `docs/work/GEOSTAT-CANONICAL-ARCHITECTURE-2026-09-21.md` | **the canonical architecture — Part II governs** |

Evidence artifacts (checkpoint, completeness audit, layer coverage) are **L4** — read to
falsify a specific conclusion, not by default.

## Recorded discrepancy

The prompt that commissioned this control plane asserted *"PASS 3 NOT STARTED"* and
*"Recovery overall INCOMPLETE"*. **The repository shows PASS 3 completed on 2026-09-20 with
`FINAL_RECOVERY_GATE: PASS`.** This file records the repository's evidenced state, because
the same instruction required that established recovery conclusions must not be lost or
rewritten. The discrepancy is noted here rather than silently resolved in either direction.
