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
| **LIFECYCLE PHASE** | `PHASE-005` — Master Rehabilitation Plan |
| **STAGE** | rehabilitation plan derived and scope-corrected — 28 responsibility families, 10 in-scope parallel authorities disposed, 28 active workstreams, 12 in-scope elimination targets, 17 protection controls |
| **STATUS** | `COMPLETE` |
| **LAST COMPLETED GATE** | `GATE-PLAN` → **PASS** (2026-09-21) |
| **CURRENT GATE** | `GATE-EXECUTION-PACKAGE` → `READY` |
| **ACTIVE WORK ITEM** | none. `TASK-001` … `TASK-007` all `COMPLETE` |
| **CONTROL PLANE** | `RCP PASS` — **659 checks, 0 errors, 2 cohesion warnings, 48 governed artifacts** from the committed state `7b00623`, verified in a clean checkout. A working tree also holding the three gitignored `codex-session-*.md` transcripts reports **665**; those six cohesion checks are over files that are never committed (`CF-044`), so the committed figure is the reproducible one. Proven by 119 governance tests: `python ops/cli/validation/rcp-verify.py` · `python -m unittest discover -s ops/tests/governance` |
| **BOUNDED CHANGE RECORD** | `docs/work/cards/master-rehabilitation-plan/governance.json` — `VERIFIED`; `python ops/cli/validation/engineering-governance.py` → `PASS` |
| **BASELINE** | `HEAD 7b00623` — the `PHASE-005` closure commit on `security/hardening-2`, derived from `50d143f`. The working tree additionally carries the platform security-hardening change set in flight (another author, ~55 paths under `platform/`, `ops/config`, `ops/tests/{security,sql}`, the AIR register and three `docs/evidence/*.json` files from the statistical/Access programme); it is **not** rehabilitation output and was neither absorbed nor modified. |
| **LAST UPDATED** | 2026-09-21 |

## Blockers

**None blocking `PHASE-006`.** `PHASE-005` closed with 28/28 responsibilities mapped, 11/11
parallel authorities disposed, 48/48 capabilities traced and 0 falsification failures remaining.

**Rehabilitation scope is bounded and is not recursively expandable** — `PLAN-MASTER` §0.
Primary target `platform/apps/geostat/backend/api`; `backend/core` in scope only where the API
genuinely depends on it; `frontend/geostat-system-app` is the parked Admin/Authoring surface
(`41627`, `W-24`); `frontend/kids` is the designated first consumer. **`backend/mobile`, other
`mobile`/`web` projects and any other undesignated neighbouring application are OUT OF SCOPE** —
source, architecture, contracts, databases, migrations, documentation and domain requirements
alike. **Repository proximity does not imply scope.**

**One in-scope boundary obligation follows from that.** The API module declares a build
dependency on the out-of-scope `:mobile` module and owns a dormant switch
(`MobileModuleConfig`, `@ConditionalOnProperty(matchIfMissing = true)`) able to component-scan it
into the API's own runtime by uncommenting three lines. `W-30` severs the coupling **on the API
side only**; `PR-17` keeps it severed. Nothing in the out-of-scope module is inspected,
rehabilitated or used as canonical evidence.

**Seven owner decisions are carried, none blocking `PHASE-006` as a whole** — each blocks only
its own consumer, enumerated in `PLAN-MASTER` §18. All seven carried from `PHASE-004`: disclosure-control policy ·
erasability classes and retention · `BM-Q-03` performance objectives · `BM-Q-05` ASVS subset ·
reference-data agency assignment · the materialization thresholds of `ARCH-CANONICAL` §31.4 ·
**Unicode NFC enforcement for text grain components** — added by the independent review
correction, owned by `ARCH-CANONICAL` §39 and derived in §31.6 residual 2. It is
**security- and integrity-relevant** and **blocks the first work that persists a text grain
component** — the `PROTECT` stage of that work, `PHASE-009` at the latest. It does not block
`PHASE-005` planning, and an agent may not settle it. **`PHASE-005` strengthened its routing**:
`PR-15` makes it a **property-level** block — CI fails if any structure declares a text component
inside its grain while the decision is open — because falsification showed a workstream-level
block could be bypassed by migration ordering (`PLAN-MASTER` §16.1 attack 25).

`PHASE-005` briefly carried an eighth (`OD-08`, the mobile subsystem's future) and an eighth
evidence gap (`EG-01`, reaching `auto`). **Both are withdrawn by the scope correction** — they
concerned an out-of-scope application. The rows are retained as withdrawn in `PLAN-MASTER` §18 so
the history stays legible, and the identifiers are retired rather than reused.

**All seven `PHASE-004` obligations are scheduled, not merely carried** — `PLAN-MASTER` §9 places
them at `W-11` (permission model), `W-13` (reconciliation, DDL audit), `W-10` (both negative
suites), `W-02` (SQL↔Access) and `W-26` (`dbo.*` elimination). **None is implemented; §31.6 and
§31.7 remain derived, not executed, and `INV-006` stays `PARTIAL` until `W-13`'s suites are
green** (`AL-03`).

**Five evidence gaps are open and owned** (`EG-02`…`EG-06`, `PLAN-MASTER` §18): SQL↔Access
correspondence · cross-table type consistency · production volumes (`BM-Q-03`) · consumer
inventory for the legacy serving endpoints · second-provider proof.

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

**`PHASE-006` — Execution Package**, guarded by `GATE-EXECUTION-PACKAGE`.

It consumes `PLAN-MASTER` and turns the 29 ordered workstreams into machine-followable execution
cards. It implements nothing itself. **Three constraints travel with it**, from `PLAN-MASTER` §19
conditions 13–15: Stage A evidence work is scheduled before its dependents; `OD-01` is enforced as
a property-level block (`PR-15`); and **`PHASE-006` may not expand into an out-of-scope project**
(`§0`, enforced by `PR-17`).


## Forbidden actions

- Reopening PASS 1 / 2A / 2B / 3 conclusions without **new primary evidence**.
- Beginning rehabilitation implementation, refactoring or legacy deletion.
- Inspecting, querying, inventorying, migrating, comparing against or deriving canonical
  requirements from any out-of-scope project or its database (`PLAN-MASTER` §0) — including
  reachability from an in-scope module, which is not a reason.
- Persisting a text component inside a declared grain while `OD-01` is open (`PR-15`).
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
| 17 | `docs/work/GEOSTAT-MASTER-REHABILITATION-PLAN-2026-09-21.md` | **how the existing system converges to it — dispositions, ordering, gates** |

Evidence artifacts (checkpoint, completeness audit, layer coverage) are **L4** — read to
falsify a specific conclusion, not by default.

## Recorded discrepancy

The prompt that commissioned this control plane asserted *"PASS 3 NOT STARTED"* and
*"Recovery overall INCOMPLETE"*. **The repository shows PASS 3 completed on 2026-09-20 with
`FINAL_RECOVERY_GATE: PASS`.** This file records the repository's evidenced state, because
the same instruction required that established recovery conclusions must not be lost or
rewritten. The discrepancy is noted here rather than silently resolved in either direction.
