---
id: TASK-001
type: WORK
title: Standards and architecture quality benchmark
status: COMPLETE
authority: SUPPORTING
phase: PHASE-002
gate: GATE-STANDARDS
created: 2026-09-21
updated: 2026-09-21
---

# TASK-001 — establish the architecture quality benchmark and close `PHASE-002`

## Objective

The repository holds a written, falsifiable definition of *better* — quality attributes with
measures, hard invariants with falsifiers, evidence-bound evaluation criteria, an automation
classification and a standards disposition matrix — strong enough that an independent
reviewer can evaluate competing architectures without inventing a missing criterion.

## Why now

`PHASE-001` is `COMPLETE` and `GATE-RECOVERY-FINAL` passed (`CTRL-CURRENT`). `MANIFEST`
declares `PHASE-002` `READY` and `GATE-STANDARDS` the governing gate; `PHASE-003` and
`PHASE-004` are explicitly blocked until it passes. `CAD-17` requires the target to be
justified by standards and requirements rather than by legacy shape, which is only possible
if the standard exists **before** the design.

## required_context

1. `docs/project/CURRENT.md`
2. `docs/project/MANIFEST.md`
3. `docs/work/GEOSTAT-API-RECOVERY-CONSOLIDATION-2026-09-20.md`
4. `docs/work/GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md`
5. `docs/work/GEOSTAT-API-RECOVERY-PASS3-CLOSURE-2026-09-20.md`
6. `docs/reference/engineering/STANDARDS.md`
7. `docs/reference/engineering/REQUIREMENTS.md`
8. `docs/reference/engineering/ARCHITECTURE.md`
9. `docs/reference/engineering/ANTI-PATTERNS.md`
10. `docs/work/GEOSTAT-API-RECOVERY-LAYER-COVERAGE-2026-09-20.md`

## Scope

**In scope**

- The benchmark, the automation classification model and the standards disposition matrix.
- The deferred controls `MANIFEST` assigns to `PHASE-002` and whose triggers fired when the
  phase opened: `DEF-01` (validator in CI), `DEF-04` (object-storage `CAD-02` row),
  `DEF-05` (trigger check only), `DEF-08` (recorded, not decided — `TASK-002`).
- Control-plane state: `CATALOG`, `MANIFEST`, `CURRENT`, and this item.

**Out of scope**

- Any part of the Canonical Architecture: physical schemas, authoring format, pattern
  granularity, entity-typing shape, promotion workflow. Those are `REC-PASS3` §10 design
  questions and `PHASE-004` work.
- Application, runtime, migration and artifact changes of any kind.
- Re-opening `PHASE-001` conclusions. Nothing here needed to.
- The `AIR` register: it carries 95 lines of uncommitted third-party work, and editing it
  would absorb that work into this item's completion claim (`/AGENTS.md` §7). The one
  discovery that belongs there is carried by `TASK-002` instead.

## Preconditions

- `PHASE-001` `COMPLETE`, `GATE-RECOVERY-FINAL` `PASS` — met.
- `rcp-verify.py` green at baseline — met: `319 checks, 0 errors, 2 warnings` at `b46c043`.
- No later phase started — verified: no `PHASE-003+` artifact exists.

## Deliverable

- `docs/standards/ARCHITECTURE-QUALITY-BENCHMARK.md` (`STD-BENCH-001`)
- `docs/standards/AUTOMATION-CLASSIFICATION-MODEL.md` (`STD-AUTO-001`)
- Disposition matrix and domain-profile routing in `docs/reference/engineering/STANDARDS.md`
- `DOC-CAD` §2.2/§2.3 object-storage responsibility row (`DEF-04`)
- `.github/workflows/architecture-governance.yml` runs `rcp-verify.py` (`DEF-01`)
- `docs/work/cards/standards-architecture-quality-benchmark/governance.json`, with
  `docs/work/cards/repository-control-protocol/governance.json` set `SUPERSEDED` → this card.
  That card covered four control-plane files this item edits; the change protocol's rule for
  legitimately changing covered source is supersession, not rewriting the old card's hashes.
  Its evidence is untouched, and both suites were re-run before `VERIFIED` was claimed.

## Verification

| Check | Command / method | Evidence required |
|---|---|---|
| Control plane consistent | `python ops/cli/validation/rcp-verify.py` | exit 0, error count 0 |
| Validator still proven | `python -m unittest discover -s ops/tests/governance` | exit 0, no test removed or weakened |
| Governance package structural gate | `python ops/cli/validation/engineering-governance.py` | `status: PASS` |
| No duplicate canonical ownership introduced | `RCP-403` / `RCP-404` inside the validator run | new `CATALOG` rows own distinct responsibilities |
| Every recovered requirement covered | `STD-BENCH-001` §10 | 41 of 41 `REQ` rows mapped |
| Gate conditions satisfied | `STD-BENCH-001` §11 | each row names where it is satisfied |

Evidence bundle: `docs/work/evidence/standards-architecture-quality-benchmark/gate-standards.txt`.

## Dependencies

- none. `TASK-002` is raised **by** this item and does not block it.

## Handoff

`PHASE-002` is closed. The benchmark is the input to `PHASE-003` (Artifact Comparative
Audit), which compares legacy Access, the R8 resource package, `KIDS_PACKAGE_candidate_2`
and the justified canonical target **against `STD-BENCH-001` §2's verdict method** — not
against a fresh set of criteria invented at that time. `GATE-COMPARATIVE` guards it.

Three things a successor should know before using the benchmark:

1. **The invariant gate is binary and separate on purpose** (§2.1). A candidate that fails
   one `BM-INV` is not scored on quality attributes, because a high score would otherwise be
   read as an argument against the invariant.
2. **Eight standards questions are open** (`BM-Q-01…08`), each with a named owner and a safe
   interim rule. `BM-Q-01` (SDMX conformance level), `BM-Q-03` (performance objectives) and
   `BM-Q-05` (ASVS control subset) will block `PHASE-004` acceptance if still unanswered.
3. **`DEF-08` is not resolved** — two live work streams run outside this lifecycle. See
   `TASK-002`; it needs an owner decision, not an agent's.

## Closure record

- **Outcome:** COMPLETE
- **Evidence:** `docs/work/evidence/standards-architecture-quality-benchmark/gate-standards.txt`;
  card `docs/work/cards/standards-architecture-quality-benchmark/governance.json` — `VERIFIED`.
- **Follow-ups raised:** `TASK-002` (`DEF-08` plus the statistical domain-profile
  classification). No `AIR` entry was written, for the reason stated under *Out of scope*;
  `TASK-002` carries it as its first action.
