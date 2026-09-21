---
id: TASK-005
type: WORK
title: Design-entry decisions
status: COMPLETE
authority: SUPPORTING
phase: PHASE-003
gate: GATE-DESIGN-ENTRY
created: 2026-09-21
updated: 2026-09-21
---

# TASK-005 — resolve and materialize the outstanding design-entry decisions

## Objective

Every condition attached to `PHASE-004`'s acceptance is either decided and materialized in the
existing decision plane, or recorded as genuinely unresolved with its owner and blocking stage
named. **No canonical architecture is designed.**

## Why now

`PHASE-003` is `COMPLETE` and `GATE-COMPARATIVE` is `PASS`; `GATE-DESIGN-ENTRY`'s listed checks
pass. Three conditions remained attached to acceptance rather than entry — `TASK-002`/`DEF-08`,
`BM-Q-01`, `BM-Q-02` — plus the comparative provenance question `AQ-05`. Entering `PHASE-004`
with them open would mean designing under decisions somebody else might make differently.

## required_context

1. `docs/project/CURRENT.md`
2. `docs/project/MANIFEST.md`
3. `docs/standards/ARCHITECTURE-QUALITY-BENCHMARK.md`
4. `docs/work/GEOSTAT-ARTIFACT-COMPARATIVE-AUDIT-2026-09-21.md`
5. `docs/work/GEOSTAT-CAPABILITY-INVENTORY-2026-09-21.md`
6. `docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md`
7. `docs/project/ADOPTION.md`
8. `docs/reference/engineering/STANDARDS.md`

## Scope

**In scope**

- Evaluating four proposed conclusions against repository evidence and applicable standards,
  challenging them where evidence is stronger, and materializing the result in the **existing**
  decision plane (`docs/decisions/` + `docs/platform-decisions.md`).
- The design-entry safety check on what `PHASE-004` will consume and how it is framed.

**Out of scope**

- `PHASE-004` in any form: no canonical schema, no table selection, no domain classes, no
  provider architecture, no migration, no UI.
- Re-deciding what an existing owner already decided. `Q47` owns the SDMX conformance claim and
  was adopted by reference, not restated.
- Modifying source Access artifacts — none was opened in this task.

## What changed relative to the proposals

Three of four proposals survived; each gained something the proposal did not name, and one
question was reframed because the repository had already answered part of it.

| Proposal | Outcome |
|---|---|
| Two distinct but coordinated programs under one authority plane | **Adopted** as `ADR-014`, plus what the proposal lacked: a conflict protocol, the meta-level cut (M1-and-below vs M2/M3) that makes the split checkable rather than diplomatic, and a **review condition** — without one, an explicit split is unbounded temporary architecture at programme scale |
| SDMX bounded to the statistical responsibility | **Adopted** as `ADR-015`, but **reframed**: `Q47` already decides the conformance *claim* (Information Model 3.1 subset, `SDMX-CSV 2.0`, `SDMX-JSON 2.0`, conformance matrix, rejection test). Writing a second claim would have created the duplicate authority this programme exists to remove, so `ADR-015` owns only the **boundary** and adopts `Q47` by reference. Research also showed SDMX declares conformance per artefact class rather than as ranked levels, so *"which level?"* has no answer in the standard |
| Access is a supported provider with no semantic authority | **Adopted** as `ADR-016`, plus the distinction the binary framing hid: Access is **three** responsibilities — physical provider, authoring surface, round-trip carrier — and only the first is about a database. The platform's real dependency is on *an* offline typed authoring surface. Also recorded: rules 3–5 are unenforceable until provider capabilities have one producer (`CF-038`, `RC-003`) |
| `AQ-05` classified as provenance, resolved only if provable | **Resolved** on repository evidence alone: commit, published digest, and two dated runtime records bind the content `746487ce…`; the api-path bytes are referenced by nothing. Authority attaches to **content**, and the path lied within two days — `BM-INV-22` demonstrated rather than argued |

## Deliverable

- `docs/decisions/ADR-lifecycle-program-separation.md` (ADR-014)
- `docs/decisions/ADR-sdmx-conformance-boundary.md` (ADR-015)
- `docs/decisions/ADR-access-provider-status.md` (ADR-016)
- Registered in `docs/platform-decisions.md`; classified in `CTRL-ADOPTION`; catalogued
- `AUD-COMPARATIVE` §23 — `AQ-05` resolved, and §10 corrected
- `STD-BENCH-001` §9 — `BM-Q-01`, `BM-Q-02` resolved
- `TASK-002` closed; `DEF-08` closed

## A correction this task made to `PHASE-003`

`AUD-COMPARATIVE` §10 claimed `candidate_2` declares *"every table, including the metadata
tables"* and called the artifact self-complete. Re-measured: **32 of 32 datasets are listed but
only 23 carry field declarations** — the nine `__gs_*` datasets describe every other table and
not themselves. The error was caught by cross-checking `ACCESS-PACKAGE-CANONICAL-STANDARD.md`
§A3-1, which measured the same gap on the sibling artifact. The improvement over `A-R8` is real;
self-completeness was overstatement, and the section now says so.

**Process finding, recorded rather than rationalised:** that document was not in `PHASE-003`'s
`required_context` because it belongs to the other program, and it contained a measured fact
that refined a conclusion. Under `ADR-014` the two programs may reference each other, and the
audit should have. Reading it also produced the strongest corroboration available — two
independent readings of two sibling artifacts agreeing row for row.

## Verification

| Check | Command / method | Evidence |
|---|---|---|
| Control plane consistent | `python ops/cli/validation/rcp-verify.py` | exit 0 |
| Decision plane complete | `RCP-703`/`RCP-704` inside that run | every new ADR carries authority and appears in the register |
| No duplicate canonical ownership | `RCP-404` | each ADR owns one decision; `DOM-STATISTICAL` owns the domain profile |
| Validator proven | `python -m unittest discover -s ops/tests/governance` | exit 0 |
| Governance package | `python ops/cli/validation/engineering-governance.py` | `PASS` |
| Design-entry inputs complete | `CURRENT` required read set vs the ten inputs `PHASE-004` must consume | §*Design-entry safety check* in the evidence bundle |

Evidence bundles: `docs/work/evidence/design-entry-decisions/design-entry.txt` (decisions) and
`docs/work/evidence/design-entry-decisions/capability-envelope.txt` (envelope, standards,
hypotheses, blind spots, twelve falsification scenarios).

## Dependencies

- `TASK-002` (decided here), `TASK-003`, `TASK-004`.

## Handoff

`PHASE-004` may begin. It is framed in `CTRL-MANIFEST` as **deriving** an architecture, never as
choosing a candidate — `PHASE-003` proved no artifact passes the invariant gate. It inherits
three binding decisions (ADR-014/015/016), sixteen required-but-missing capabilities
(`CAP-M01…M16`), four comparative questions, and one constraint that is easy to miss: **ADR-014 rule 1 means the
statistical program's `DECIDED` entries bind M1 semantics, and design may supersede them only
through a naming ADR — never by designing past them.**

Two obligations travel with the decisions: ADR-014's review condition fires on the first
`PHASE-004` collision, and ADR-016's rules 3–5 stay unenforceable until provider capability has
exactly one producer.

## Part 2 — capability envelope, standards and governed-chain hypotheses

The decision half closed the *named* questions. This half tests whether the **problem space** is
complete, because `PHASE-003` derived it from four artifacts and an artifact can only reveal
what somebody already built.

**Materialized in existing owners, no new artifact:** `AUD-CAPABILITY` §6–§16 (envelope,
metadata responsibility model, raw→published chain, relationship space, logical/physical
separation, both hypothesis verdicts, blind spots, twelve falsification scenarios, the 11/11
trace and five additions); `STANDARDS.md` (+8 dispositions); `STD-BENCH-001` (`BM-INV-25`,
`BM-EC-087…089`).

**What the pass actually found, in order of importance:**

1. **A routing defect at the largest scale yet.** `PHASE-004`'s input list did not include the
   statistical domain register. `ADR-014` rule 1 makes its `DECIDED` entries binding on M1
   semantics, and `Q25/Q26/Q39/Q47/Q48` already specify derivation rules, typed codelist
   crosswalks, canonical digests, the conformance claim, and a build manifest carrying generator
   identity and semantic correspondence — the last two being `CAP-M01`/`CAP-M02`, *"missing from
   every artifact"*. Design would have re-decided fifty evidenced questions. **Fixed.**
2. **Four of my own candidate blind spots were falsified by that same register** — measure
   additivity and `SUM`-blocking (`Q24`), missing-value semantics (`Q22`), embargo (`Q12`),
   concurrent authoring (`Q40`). Recorded, because a discovery that turns out to be owned is a
   result, not a wasted pass.
3. **Seven genuine dimensions survived** (`D1…D7`), of which the sharpest is `D1`: nothing in
   the repository protects a suppression — `Q12` and `Q22` declare and flag it, and a suppressed
   cell remains recoverable by subtraction from published totals. Zero occurrences repository-wide.
4. **Both hypotheses were modified, not adopted.** The governed chain is not a chain and not one
   graph: it is three graphs — derivation, dependency, composition — that differ in direction,
   mutability and lifetime, and one universal edge type would constrain none of them. The
   contract lifecycle is missing three stages: approval, correction/reprocessing, retirement.
5. **`DDI-CDI` is the standard the prompt did not name and the envelope needed**: one datum
   described in wide, long, dimensional and key-value structures. It is the model for
   "statistical and non-statistical in one system" — adopted as concepts, rejected as a
   serialization.

**Scope held:** no table, class, storage engine or graph implementation was chosen; no source
artifact was opened in this half.

## Closure record

- **Outcome:** COMPLETE
- **Evidence:** `docs/work/evidence/design-entry-decisions/design-entry.txt`; card
  `docs/work/cards/design-entry-decisions/governance.json` — `VERIFIED`.
- **Follow-ups raised:** none blocking. `AIR-2026-056` still reserved; the nine `DEFAULT`
  statistical decisions stay with their program; `D3` (legal erasure vs immutability) is an
  owner question recorded in `AUD-CAPABILITY` §12 and `CTRL-CURRENT`.
