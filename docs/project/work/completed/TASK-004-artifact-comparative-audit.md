---
id: TASK-004
type: WORK
title: Artifact comparative audit
status: COMPLETE
authority: SUPPORTING
phase: PHASE-003
gate: GATE-COMPARATIVE
created: 2026-09-21
updated: 2026-09-21
---

# TASK-004 — compare the candidate artifacts under the accepted benchmark

## Objective

The repository holds, as durable knowledge, what each real artifact contains — capability,
semantic strength, structural mechanism, defect, accidental complexity and reusable innovation —
and what that evidence implies for the Canonical Architecture. **No artifact is selected.**

## Why now

`PHASE-002` is `COMPLETE` and `GATE-STANDARDS` is `PASS`, re-confirmed after the adversarial
acceptance review (`TASK-003`). `MANIFEST` declares `PHASE-003` `READY` behind
`GATE-COMPARATIVE`, and `GATE-DESIGN-ENTRY` lists the comparative audit as the last
outstanding condition for `PHASE-004`.

## required_context

1. `docs/project/CURRENT.md`
2. `docs/project/MANIFEST.md`
3. `docs/standards/ARCHITECTURE-QUALITY-BENCHMARK.md`
4. `docs/standards/AUTOMATION-CLASSIFICATION-MODEL.md`
5. `docs/work/GEOSTAT-API-RECOVERY-CONSOLIDATION-2026-09-20.md`
6. `docs/work/GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md`
7. `docs/work/GEOSTAT-API-RECOVERY-PASS3-CLOSURE-2026-09-20.md`
8. `samples/README.md`

## Scope

**In scope**

- Read-only inspection of the artifacts repository context names: the R8 resource package, the
  `candidate_2` Access file, the current canonical Access representation and the legacy Managed
  v1 package, plus `A-R7` and the `8.0.1` build as lineage evidence.
- Comparison at fifteen levels, capability mining, loss and innovation analysis,
  required-but-missing identification, falsification and the `PHASE-004` input package.

**Out of scope**

- Selecting a canonical artifact, designing the target, choosing schemas or persistence.
- Modifying any source artifact. Every Access file was copied to a scratch directory and opened
  read-only; digests before and after are unchanged.
- The ~62 unclassified test files and `frontend/web` / `backend/mobile` — protection and
  elimination inputs, owned by `PHASE-009` and `PHASE-011`.
- `KIDS_PACKAGE_candidate_3`, which is **not in the repository**. No substitute was inspected in
  its place; the gap is recorded in `AUD-COMPARATIVE` §1.2.

## Preconditions

- `PHASE-002` `COMPLETE`, `GATE-STANDARDS` `PASS` — met.
- Benchmark evidence-bound and accepted — met (`TASK-003`).
- No later phase started — verified before starting.

## Deliverable

- `docs/work/GEOSTAT-ARTIFACT-COMPARATIVE-AUDIT-2026-09-21.md` (`AUD-COMPARATIVE`)
- `docs/work/GEOSTAT-CAPABILITY-INVENTORY-2026-09-21.md` (`AUD-CAPABILITY`)
- `REC-CONSOLIDATION` §15 updated — the three artifact inputs are no longer "not judged"

## Verification

| Check | Command / method | Evidence |
|---|---|---|
| Artifact identity fixed before comparison | `sha256sum` over every artifact | `AUD-COMPARATIVE` §1; `samples/README.md`'s published digest verifies |
| Sources unmodified | digests recomputed after inspection | unchanged |
| Package integrity claim tested, not trusted | recompute all 451 manifest digests and every edge | 451/451 match, 0 dangling |
| Value-level loss tested | exact-decimal multiset comparison of 880 facts | identical |
| Benchmark applied | `STD-BENCH-001` §2 verdict method | `AUD-COMPARATIVE` §19 |
| Control plane consistent | `python ops/cli/validation/rcp-verify.py` | exit 0 |
| Validator proven | `python -m unittest discover -s ops/tests/governance` | exit 0 |
| Governance package | `python ops/cli/validation/engineering-governance.py` | `PASS` |

Evidence bundle: `docs/work/evidence/artifact-comparative-audit/gate-comparative.txt`.

## Dependencies

- `TASK-001`, `TASK-003` — produced and accepted the benchmark this audit applies.

## Handoff

`PHASE-004` may begin behind `GATE-DESIGN-ENTRY`. It consumes `AUD-CAPABILITY` §5, not the raw
artifacts, and it inherits three things that will shape it:

1. **No artifact passes the invariant gate.** The canonical model is not any of them, and the
   audit deliberately did not pick one.
2. **`CF-015` is closed by evidence** — `candidate_2`'s attribute attachment levels implement
   the per-combination constant that `PHASE-001` recorded as missing. `REC-CONSOLIDATION` §9
   still lists it as missing **from the platform**, which remains true; what changed is that a
   working shape now exists to reuse.
3. **Eleven capabilities are missing from every artifact** (`CAP-M01…M11`). Design must add
   them rather than recombine what exists.

The audit's own weakest point is stated in `AUD-COMPARATIVE` §21.1: one provider and one cube
were examined, so `SCH-003`-style generality is evidenced for a single case.

## Closure record

- **Outcome:** COMPLETE
- **Evidence:** `docs/work/evidence/artifact-comparative-audit/gate-comparative.txt`; card
  `docs/work/cards/artifact-comparative-audit/governance.json` — `VERIFIED`.
- **Follow-ups raised:** `AQ-01…AQ-05` carried into `PHASE-004`; `TASK-002` unchanged and still
  `BLOCKED` on the owner.
