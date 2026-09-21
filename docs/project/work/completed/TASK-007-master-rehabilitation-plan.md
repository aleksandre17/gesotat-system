---
id: TASK-007
type: WORK
title: Master rehabilitation plan
status: COMPLETE
authority: SUPPORTING
phase: PHASE-005
gate: GATE-PLAN
created: 2026-09-21
updated: 2026-09-21
---

# TASK-007 — derive the master rehabilitation plan

## Objective

A dependency-ordered plan that takes the existing system to the accepted canonical architecture
with one authority per responsibility, no lost capability, and an explicit gate before every
irreversible step. The plan implements nothing.

## Why now

`PHASE-004` is `COMPLETE` and accepted; `GATE-PLAN` is `READY` in `MANIFEST`. `PHASE-005` is the
next permitted action, and it consumes `ARCH-CANONICAL` (Part II governs) bounded by
`ARCH-BASELINE` and `ARCH-DOSSIER`.

## required_context

1. `docs/project/CURRENT.md`
2. `docs/project/MANIFEST.md`
3. `docs/work/GEOSTAT-CANONICAL-ARCHITECTURE-2026-09-21.md`
4. `docs/work/GEOSTAT-PHYSICAL-RELATIONAL-BASELINE-2026-09-21.md`
5. `docs/work/GEOSTAT-PHYSICAL-DATA-DOSSIER-2026-09-21.md`
6. `docs/work/GEOSTAT-CAPABILITY-INVENTORY-2026-09-21.md`
7. `docs/standards/ARCHITECTURE-QUALITY-BENCHMARK.md`
8. `docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md`
9. `docs/decisions/ADR-access-provider-status.md`

## Scope

**In scope**

- responsibility inventory across every system plane, derived from the committed tree `50d143f`
- parallel-authority discovery by responsibility rather than by name collision
- disposition for every responsibility and every parallel authority
- target physical family classification, convergence map, ordered roadmap
- elimination register, protection plan, decision register, `PHASE-006` entry contract
- falsification of the plan before acceptance

**Out of scope**

- any implementation, migration, DDL, DML or legacy deletion
- Access refinement and the Admin/UI blueprint — routed, not executed
- settling any owner decision
- the platform security-hardening work in flight in the working tree

## Preconditions

- `PHASE-004` `COMPLETE` and committed at `50d143f`
- `GATE-PLAN` `READY`
- control plane verifying: `rcp-verify` PASS, `engineering-governance` PASS, 119 tests OK

## Deliverable

- `docs/work/GEOSTAT-MASTER-REHABILITATION-PLAN-2026-09-21.md` (`PLAN-MASTER`)
- `docs/work/evidence/master-rehabilitation-plan/gate-plan.txt`
- `docs/work/cards/master-rehabilitation-plan/governance.json`

## Verification

| Check | Command / method | Evidence required |
|---|---|---|
| control plane intact | `python ops/cli/validation/rcp-verify.py` | `PASS`, 0 errors |
| bounded change record valid | `python ops/cli/validation/engineering-governance.py` | `PASS` |
| governance tests | `python -m unittest discover -s ops/tests/governance` | `OK` |
| no implementation performed | `git status --porcelain` | only `docs/` paths changed by this item |
| database untouched | read-only count of `platform.schema_migration` | 113, unchanged |

## Dependencies

- `TASK-006`

## Handoff

The plan is complete, scope-corrected, and `GATE-PLAN` passes. The inheritance a successor needs,
in one paragraph: **rehabilitation scope is bounded and is not recursively expandable**
(`PLAN-MASTER` §0). The primary target is `backend/api`; `backend/core` is in scope only where the
API genuinely depends on it; `frontend/geostat-system-app` is the parked Admin/Authoring surface
(`41627`) and `frontend/kids` the designated first consumer. `backend/mobile`, other `mobile` and
`web` projects, and any other undesignated neighbouring application are **out of scope** — source,
architecture, contracts, databases, migrations, documentation and domain requirements alike.
Repository proximity does not imply scope. `PHASE-005` initially treated `backend/mobile` as a
product-decision blocker; that was a scope error, and `EG-01`, `OD-08`, `W-01`, `W-07`, `PA-08` and
`EL-10` are withdrawn as a result — retained as withdrawn rows, identifiers retired, never reused,
because scope exclusion is not evidence deletion. **One real boundary obligation survives the
exclusion**, and it is API-side: `api/build.gradle` declares `implementation project(':mobile')`
under conditions that hold by default, and `org.base.api.config.MobileModuleConfig` carries a
`@ConditionalOnProperty(matchIfMissing = true)` component-scan of the excluded module that is
commented out but re-arms silently if uncommented. `W-30` severs the build edge, makes
`activeModules` explicit, deletes that switch and removes three API config references; `PR-17`
fails the build if any of it returns. Its exit criterion is that the API Platform builds, tests
and runs with `:mobile` absent from `settings.gradle` entirely. The other hard ordering constraint
is unchanged: `OD-01`, the NFC text-grain decision, is enforced as a **property-level** block by
`PR-15`, because falsification showed a workstream-level block could be bypassed by migration
ordering.

## Closure record

- **Outcome:** COMPLETE
- **Evidence:** `docs/work/evidence/master-rehabilitation-plan/gate-plan.txt`; `GATE-PLAN` PASS
- **Follow-ups raised:** `EG-02`…`EG-06`, `OD-01`…`OD-07` — all registered in `PLAN-MASTER` §18
  and summarised in `CURRENT`. `EG-01` and `OD-08` withdrawn by the scope correction. One
  correction routed to its owner: `ARCH-DOSSIER` §1.1 scope.
