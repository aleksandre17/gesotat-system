---
id: ADR-014
type: DECISION
title: Two programs, one authority plane
status: ACTIVE
authority: CANONICAL
scope: the relationship between the rehabilitation lifecycle and the statistical contract/product program
created: 2026-09-21
updated: 2026-09-21
related: CTRL-MANIFEST, DOC-CAD, CTRL-ADOPTION
---

# ADR-014 — Two programs, one authority plane

**Status:** ACCEPTED 2026-09-21. **Decision authority:** the owner's instruction of 2026-09-21,
which directed that this be evaluated and, unless contradicted by stronger evidence, adopted.
**Supersedes nothing.** Closes `DEF-08` and `TASK-002`.

## Context

Two programs run in this repository at the same time.

- The **rehabilitation lifecycle** (`CTRL-MANIFEST`, `PHASE-001…013`) governs the architecture
  itself: what the canonical model is, which authority owns which responsibility, and in what
  order legacy converges on it. It forbids implementation until `PHASE-010`.
- The **statistical contract/product program** (`COMMON-STATISTICAL-CONTRACT-PLAN.md`, the
  `Q01…Q50` decision register, the implementation checklist's increments behind gates
  `G0…G4`, and the Access-package work stream) delivers a live capability. It is implementing
  now, on dev.

Both statements are true of the same repository on the same day. `CTRL-ADOPTION` §8.3 recorded
the coexistence and deferred the reconciliation to `DEF-08`; `PHASE-002` found the scope wider
than recorded — the Access-package stream is that program's delivery arm, and the program owns
the only domain standards profile (`S1…S16`).

## Decision

**The two programs are distinct lifecycles with distinct schedules, operating under one
canonical authority plane. They are not merged, and neither is subordinate to the other's
schedule.**

Concretely, four rules, each mechanically checkable:

1. **One authority plane.** Any change to M3/M2 semantics, to a `CAD-02` responsibility, to the
   authority of a mechanism, or to an approved contract grammar requires the existing decision
   instruments — an ADR plus a bounded governance card — **whichever program makes it**
   (`CAD-01`, `CAD-16`). There is no second authority registry and no second ADR plane.
2. **One roadmap per program, and no gate over the other.** `CTRL-MANIFEST` is the only
   rehabilitation roadmap (RCP §22). `G0…G4` are the statistical program's own delivery gates.
   **Neither program may declare a gate over a phase of the other**, and neither may claim the
   other's state.
3. **One current-state authority.** `CTRL-CURRENT` states where the *rehabilitation* stands. It
   does not state the statistical program's state, and the statistical program does not get a
   second `CURRENT` (ADR-013).
4. **The cut is the meta-level, not the calendar.** The statistical program operates on **M1
   instances and below** — authoring contracts, filling datasets, loading data, generating
   provider artifacts. The rehabilitation lifecycle owns **M2/M3 and the authority chain**. A
   statistical-program change that would alter M2/M3, or create a mechanism beside an existing
   authority, is out of its scope and enters through rule 1.

**Coordination instruments, so that "coordinated" is not merely a word:**

| Instrument | Obligation |
|---|---|
| Shared authority registry | `CAD-02` — every mechanism either program creates must be classifiable against it |
| Shared standards profile | `docs/reference/engineering/STANDARDS.md` is the platform profile; each program may own a **domain profile** that narrows it and may never adopt what the platform profile rejects |
| Cross-reference, not restatement | each program may declare a dependency on the other by reference; neither restates the other's facts |
| Conflict protocol | when the programs disagree on semantics, identity, ownership or lifecycle, `/CLAUDE.md` §3 applies unchanged: record the conflict, mark `ARCHITECTURAL DECISION REQUIRED`, stop dependent work, continue work proven independent |
| **Review condition** | this separation is reviewed when `PHASE-004` fixes a canonical decision the statistical program has already implemented differently, **or** when either program needs a gate the other owns — whichever comes first |

## Validation against the Project Operating System laws

`ONE FACT → ONE CANONICAL OWNER → MANY REFERENCES`: satisfied, because the facts are different
facts. *What the canonical architecture is* has one owner (the rehabilitation lifecycle, through
`PHASE-004` and the ADR plane). *What the statistical product delivers and when* has one owner
(the statistical program). Merging them would not reduce duplicate truth; it would put two
unrelated facts under one owner.

`STATE ≠ WORK ≠ KNOWLEDGE ≠ HISTORY`: satisfied, and this is the decisive test. A merged
lifecycle would force `CTRL-MANIFEST` — a `STATE`/lifecycle artifact — to carry increment-level
delivery `WORK`, which work items own. The separation keeps each concept with its owner.

## Alternatives considered and rejected on evidence

| Alternative | Rejected because |
|---|---|
| **One merged lifecycle** | fails `STATE ≠ WORK`; and it would halt live delivery until `PHASE-010`, a product consequence the protocol has no authority to impose |
| **Two fully independent lifecycles** | fails `ONE FACT → ONE OWNER` at the single point where both touch canonical architecture. Independence is safe for schedules and unsafe for authority |
| **Parent/child (statistical as a sub-lifecycle)** | fails on evidence: the child is *ahead* of the parent — implementing while the parent forbids implementation. Describing it as a child would make the roadmap state something false |
| **Absorb the statistical program into `PHASE-010`** | it predates the lifecycle and has its own approved decisions; absorbing it would either invalidate them or import them unexamined |

## Consequences

- `DEF-08` closes. `TASK-002` closes with its structural half decided here and its residue
  handed to the program that owns it.
- The statistical program's domain standards profile (`S1…S16`) is **canonical for its domain**
  under rule 1's shared-profile clause; `CTRL-ADOPTION` records the classification.
- **`PHASE-004` inherits a live constraint, not a free hand:** the statistical program has
  `DECIDED` entries (`Q01…Q50`) that bind M1-level semantics. Design may supersede them only
  through rule 1 — an ADR that names what it supersedes — never by silently designing past them.
- Two residues stay with the statistical program and are **not** design-entry blockers: the nine
  `DEFAULT` decisions awaiting `OWNER`/`STEWARD` ratification, and the routing defect recorded
  for `AIR-2026-056`.

## Falsification attempt

*Could this be a disguised permanent split that lets divergence accumulate?* It would be, if the
review condition were absent — an unbounded separation is the `CAD-11`/`BM-INV-19` defect applied
to programs rather than to code. The review condition above is what makes it bounded, and it is
the single most likely thing to be ignored: it fires on a **collision**, and collisions are
noticed late. **Confidence: HIGH** that the model is right; **MEDIUM** that the review condition
will fire on time without an explicit check in `PHASE-004`'s own gate.
