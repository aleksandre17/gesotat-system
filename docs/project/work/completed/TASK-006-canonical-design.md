---
id: TASK-006
type: WORK
title: Canonical architecture design
status: COMPLETE
authority: SUPPORTING
phase: PHASE-004
gate: GATE-DESIGN-ENTRY
created: 2026-09-21
updated: 2026-09-21
---

# TASK-006 — derive the canonical architecture

## Objective

A canonical architecture derived from requirements, invariants, standards and evidence, and
proven **non-regressive** against the physical model that exists today.

## Why now

`GATE-DESIGN-ENTRY`'s checks all pass and its three acceptance conditions were decided
(`ADR-014/015/016`, `TASK-005`). `PHASE-004` is the first phase authorised to derive the target.

## required_context

1. `docs/project/CURRENT.md`
2. `docs/project/MANIFEST.md`
3. `docs/work/GEOSTAT-PHYSICAL-RELATIONAL-BASELINE-2026-09-21.md`
4. `docs/work/GEOSTAT-CAPABILITY-INVENTORY-2026-09-21.md`
5. `docs/work/GEOSTAT-ARTIFACT-COMPARATIVE-AUDIT-2026-09-21.md`
6. `docs/standards/ARCHITECTURE-QUALITY-BENCHMARK.md`
7. `docs/standards/AUTOMATION-CLASSIFICATION-MODEL.md`
8. `docs/work/GEOSTAT-API-RECOVERY-CONSOLIDATION-2026-09-20.md`
9. `docs/work/GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md`
10. `docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md`
11. `docs/decisions/ADR-lifecycle-program-separation.md`
12. `docs/decisions/ADR-sdmx-conformance-boundary.md`
13. `docs/decisions/ADR-access-provider-status.md`

## Scope

**In scope** — the canonical architecture, and the physical baseline that bounds it.

**Out of scope** — migration, provider generators, Access physical refinement, Admin/UI, API
implementation, legacy deletion. Design defines contracts and boundaries; it implements nothing.

## State — recorded honestly

**Two things were done, in the wrong order, and the order was corrected mid-phase.**

1. A draft architecture was written to §29 (`ARCH-CANONICAL`): thesis, five alternatives with a
   reasoned choice, a fourteen-concept kernel, six families, identity, grain, three relationship
   families, metadata homes, the raw→published planes, temporality, provenance, contracts,
   classifiers, materialization, providers, the Access boundary, packages, validation,
   generation, automation, confidentiality and standards mapping.
2. An ordering correction then required the **pre-existing physical relational model** to be
   reconstructed before any generalization is adopted. It was right: the draft had been derived
   from Access artifacts and recovery registers, and **no phase had ever inventoried the SQL
   data plane**.

`ARCH-BASELINE` now exists — 113 migrations parsed **and** live read-only introspection of the
three dev databases. It contradicts three sections of the draft, and it produced a finding
nobody had: **19 legacy `dbo.*` tables live in the same databases as the governed control
plane**, including a second migration ledger, a legacy authorization model and a physical
`chart_definitions`.

**Resolved.** Part II (§30–§40) re-derived the six contradicted decisions against physical
evidence — each with old decision, evidence, why it failed, revised decision, preserved
guarantee and falsification test — then completed the walkthroughs, integrity and evolution
tests, coverage and the final architectural test. `ARCH-CANONICAL` is `COMPLETE` and catalogued.

**The revision also answered a question the draft never asked**: how a logical model evolves
without platform DDL while keeping typed relational integrity. The answer was derived from the
strongest pattern already in the database — `entity.localized_text`'s typed narrow table keyed
by `(record, component, discriminator)` — generalized into value-domain-partitioned kernel
tables, with grain enforced by `UNIQUE(structure_revision, business_key_digest)`. Two guarantees
genuinely weaken in the generic kernel (per-field `NOT NULL`, scalar range checks); both are
named in §31.3 and bounded by §31.4, and neither regresses anything that exists, because all 115
live CHECK constraints sit on kernel tables that remain physical.

## Verification

| Check | Command / method | Evidence |
|---|---|---|
| Control plane consistent | `python ops/cli/validation/rcp-verify.py` | exit 0 |
| Governance package | `python ops/cli/validation/engineering-governance.py` | `PASS` |
| Baseline reproducible | re-run the DDL inventory over the chain; re-run the `sys.*` queries | `docs/work/evidence/physical-relational-baseline/` |
| Chain fully applied | live `platform.schema_migration` | 113 applied, last `112_…` |
| **Non-regression** | `ARCH-BASELINE` §6 + `ARCH-DOSSIER` §8 | **clean — 19/19 preserved or strengthened** (`ARCH-CANONICAL` §36) |

## Next action

None in `PHASE-004`. The next lifecycle-permitted phase is `PHASE-005` behind `GATE-PLAN` —
not started.

## Handoff

`PHASE-004` is complete. `PHASE-005` consumes `ARCH-CANONICAL` (Part II governs), bounded by
`ARCH-BASELINE` and `ARCH-DOSSIER`, and inherits six owner decisions, the two named kernel
limitations with their specialization criteria, the unevidenced second-provider property, and
the `REC-CONSOLIDATION` correction (R-7).

**The one thing a successor must not undo:** the specialization boundary. A materialized
per-structure table is a declared, reversible performance decision — the moment it becomes the
default, the architecture is back to schema proliferation and the evolution property is lost.
