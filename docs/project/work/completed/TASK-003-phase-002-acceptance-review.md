---
id: TASK-003
type: WORK
title: Adversarial acceptance review of PHASE-002
status: COMPLETE
authority: SUPPORTING
phase: PHASE-002
gate: GATE-STANDARDS
created: 2026-09-21
updated: 2026-09-21
---

# TASK-003 — adversarial acceptance review of `PHASE-002`

## Objective

Decide whether `GATE-STANDARDS: PASS` is **substantively** justified or merely
governance-valid: whether the benchmark could today be used by an independent review board to
compare two materially different candidate architectures without inventing a missing quality
criterion.

## Why now

`GATE-STANDARDS` was declared `PASS` on 2026-09-21 by `TASK-001`, the item that produced the
artifact it judged. A gate whose only reviewer is its author is a gate with one point of
failure, and every validator in this repository exists because that failure mode is real
here. `PHASE-003` inherits the benchmark as its evaluation instrument, so a hole in it becomes
a hole in the comparative audit, then in the design, then in everything after.

## required_context

1. `docs/standards/ARCHITECTURE-QUALITY-BENCHMARK.md`
2. `docs/standards/AUTOMATION-CLASSIFICATION-MODEL.md`
3. `docs/reference/engineering/STANDARDS.md`
4. `docs/work/GEOSTAT-API-RECOVERY-CONSOLIDATION-2026-09-20.md`
5. `docs/work/GEOSTAT-API-RECOVERY-PASS3-CLOSURE-2026-09-20.md`
6. `docs/project/work/completed/TASK-001-standards-architecture-quality-benchmark.md`

## Scope

**In scope**

- Falsifying the benchmark by scoring five adversarial hypothetical candidates against it.
- Auditing criterion quality, invariant coverage, standards dispositions and the automation
  model for real decidability rather than vocabulary.
- Classifying the resolution timing of `BM-Q-01…08`.
- Advancing `TASK-002`'s lifecycle analysis as far as repository evidence allows.
- Repairing only what the review proved broken.

**Out of scope**

- `PHASE-003` in any form. The five candidates are **hypothetical constructions**; no real
  artifact — legacy Access, the R8 package, `KIDS_PACKAGE_candidate_2` — was evaluated,
  compared or scored.
- Designing the Canonical Architecture or answering `REC-PASS3` §10 design questions.
- Deciding `TASK-002`, which needs the owner.
- Adding criteria or invariants for completeness. Two invariants and two criteria were added;
  each closes a hole a candidate walked through.

## Findings

**Two material defects in the quality-attribute model.**

1. The `BM-QA` table carried **no standard per attribute**, while `GATE-STANDARDS`'s headline
   condition — *every material quality attribute has a named standard or an explicit decision
   not to adopt one* — was recorded as satisfied by a sentence asserting the mapping existed.
   That is the documentation-only-evidence defect, committed by the artifact whose purpose is
   to detect it. **The first `PASS` was premature on this condition.** Fixed: one entry per
   attribute, including four honest *no external standard exists* and one explicit
   non-adoption, plus a statement of which adopted standards have no hook here and why.
2. `BM-QA-12` (conceptual economy) was `REQUIRED` with a *count* as its measure and no
   threshold — unusable as a binary gate. Fixed: comparative and `BOUNDED`, with the rule that
   excess concepts must name the capability they buy.

**Five material criterion gaps**, all found by scoring candidates rather than by reading:

| Gap | Found by |
|---|---|
| `BM-EC-021` detected EAV by column type, which **typed** EAV passes | Candidate A |
| No criterion asked whether canonical semantics had been lowered to the weakest provider | Candidate D |
| `BM-EC-073` said "large fixture" — proving it works at one size, nothing about growth | the scalability discrimination test |
| `BM-EC-018` accepted a *declaration* as evidence of legitimate denormalisation | the denormalisation test |
| Nothing tested whether "specialization" was a declared boundary or a word for parallel architecture | the specialization test |

**Two material invariant gaps:** raw retention and re-derivability (`BM-INV-23`), and declared
grain enforced by a business key rather than a surrogate (`BM-INV-24`). Both were passed by
plausible candidates.

**Nine missing automation responsibilities:** contract derivation from an existing artifact,
lineage capture, structural migration authoring, data-correcting migration authoring,
migration execution, documentation navigation, document authority classification, conformance
corpus generation, test classification. Migrations being absent mattered most: `RC-002` is the
second root cause and the model said nothing about them. One rule added (`MSA-R8`) against
derivation that produces only a runtime interpretation.

**Two blocking claims corrected.** `PHASE-002` stated that `BM-Q-01`, `BM-Q-03` and `BM-Q-05`
all block `PHASE-004`. Re-examined: only `BM-Q-01` (and `BM-Q-02`) do. `BM-Q-03` is bounded by
a `TARGET` attribute and binds before `PHASE-010`; `BM-Q-05` selects evidence depth and binds
before `PHASE-009`. Calling a deadline a blocker is how real blockers stop being believed.

**What survived unchanged:** the decision hierarchy, the verdict method's fail-closed
ordering, 18 of 22 original invariants, 79 of 84 original criteria, every standards
disposition, and the ownership model — no parallel authority was found.

## Deliverable

- Repaired `STD-BENCH-001` (§4 standards column, `BM-INV-23/24`, `BM-EC-085/086`, three
  repaired criteria, §8 F-7/F-8, §9 timing classification, §10/§11 updates).
- Repaired `STD-AUTO-001` (`MSA-036…044`, `MSA-R8`).
- `TASK-002` lifecycle-relationship analysis: four models tested against the two Project
  Operating System laws, three eliminated on evidence, the scheduling half left to the owner.
- This record, and the corrections in `CTRL-MANIFEST` and `CTRL-CURRENT`.

## Verification

| Check | Command / method | Evidence required |
|---|---|---|
| Control plane consistent | `python ops/cli/validation/rcp-verify.py` | exit 0 |
| Validator still proven | `python -m unittest discover -s ops/tests/governance` | exit 0, no test weakened |
| Governance package | `python ops/cli/validation/engineering-governance.py` | `status: PASS` |
| Benchmark discriminates | five adversarial candidates scored in `STD-BENCH-001` §8 F-7 | each rejected or penalised for the correct reason |
| Gate re-confirmed | `STD-BENCH-001` §11 | every condition points at an inspectable artifact, not a claim |

Evidence bundle:
`docs/work/evidence/phase-002-acceptance-review/gate-standards-reconfirmation.txt`.

## Dependencies

- `TASK-001` — produced the artifact under review; `COMPLETE`.

## Handoff

`GATE-STANDARDS` is re-confirmed **with the corrections in place**, and the record says
plainly that its first declaration was premature on one condition. `PHASE-003` remains the
next permitted phase and inherits two obligations from this review: score every candidate
against `BM-INV-23/24` and `BM-EC-085/086` as well, and record for each `BM-Q` whether a
different answer would change the verdict.

The benchmark's own residual weakness is stated rather than hidden: five adversarial
candidates is a sample, not a proof. The next reviewer's most useful contribution is a sixth
candidate that passes and should not have.

## Closure record

- **Outcome:** COMPLETE
- **Evidence:** `docs/work/evidence/phase-002-acceptance-review/gate-standards-reconfirmation.txt`;
  card `docs/work/cards/phase-002-acceptance-review/governance.json` — `VERIFIED`.
- **Follow-ups raised:** none new. `TASK-002` advanced but still `BLOCKED` on the owner;
  `AIR-2026-056` still reserved for the domain-profile classification finding.
