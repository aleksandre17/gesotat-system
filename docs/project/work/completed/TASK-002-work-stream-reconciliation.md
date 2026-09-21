---
id: TASK-002
type: WORK
title: Reconcile the parallel work streams and classify the statistical domain profile
status: COMPLETE
authority: SUPPORTING
phase: PHASE-002
gate: GATE-STANDARDS
created: 2026-09-21
updated: 2026-09-21
---

# TASK-002 — reconcile the parallel work streams (`DEF-08`) and classify the statistical domain profile

## Objective

One answer to *which schedule governs this repository*, and one recorded authority for the
statistical-domain standards profile. Either the two live work streams and the rehabilitation
lifecycle merge into one schedule, or the split is explicit, written down, and states what
each side may and may not do to the other.

## Why now

`MANIFEST` sets `DEF-08`'s trigger as *"`PHASE-002` opens, or either work stream needs a gate
the other already owns"*. The phase opened on 2026-09-21, so the trigger has fired. Under
`MANIFEST`'s own reading rule, **a fired trigger that is ignored is a governance defect**, and
the correct action is to record it as a work item rather than resolve it opportunistically
inside unrelated work — which is exactly what this item is.

## Blocker — cleared 2026-09-21

**It needed an owner decision, and it got one.** The owner directed on 2026-09-21 that the
two-programs/one-authority-plane model be evaluated and, unless contradicted by stronger
evidence, adopted. It was evaluated against the two Project Operating System laws and three
alternatives, was not contradicted, and is now **ADR-014**
(`docs/decisions/ADR-lifecycle-program-separation.md`). `DEF-08` closes with it.

## required_context

1. `docs/project/MANIFEST.md`
2. `docs/project/ADOPTION.md`
3. `docs/work/ACCESS-PACKAGE-HANDOFF.md`
4. `docs/work/ACCESS-PACKAGE-MASTER-CHECKLIST.md`
5. `docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md`
6. `docs/work/STATISTICAL-CONTRACT-IMPLEMENTATION-CHECKLIST.md`
7. `docs/reference/engineering/STANDARDS.md`
8. `docs/work/ARCHITECTURE-IMPROVEMENT-REGISTER.md`

## What was found, and why it is larger than `DEF-08` states

`CTRL-ADOPTION` §8.3 describes `DEF-08` as reconciling *the Access-package work stream*. The
`PHASE-002` review found the scope is wider, and the widening is the material part:

1. **The Access-package stream is the delivery arm of a larger programme.** The statistical
   contract programme runs increments 1–6 behind its own gates `G0…G4`
   (`STATISTICAL-CONTRACT-IMPLEMENTATION-CHECKLIST.md`), with a 50-decision register
   (`Q01…Q50`, nine defaults awaiting owner confirmation) as its design authority.
2. **That programme is executing implementation now**, on dev runtime, while the
   rehabilitation lifecycle forbids implementation until `PHASE-010` and forbids design
   until `PHASE-004`. Both statements are currently true of the same repository.
3. **The only domain standards profile lives there.** `S1…S16`
   (`STATISTICAL-CONTRACT-OPEN-QUESTIONS.md` §7) is the sole home of the platform's SDMX,
   ISO 8601, canonicalisation, SemVer, ABAC and idempotency standard selections, and it was
   unreachable from `docs/reference/engineering/STANDARDS.md` until `PHASE-002` added the
   routing.
4. **Its authority classification contradicts its self-description.** `CTRL-ADOPTION` §4
   classifies the `STATISTICAL-CONTRACT-*` group `SUPPORTING`; the register describes itself
   as *"დიზაინის authority"* — the design authority. Under `CTRL-ADOPTION`'s own precedence
   rule the register wins and the document is `SUPPORTING`, which leaves a `SUPPORTING`
   document as the only source of a set of binding standard selections. This is the `CF-043`
   shape — a document whose real authority and declared authority differ — found in a
   register rather than in a filename.

**Nothing above is a contradiction of semantics**, so `/CLAUDE.md` §3 (conflict → stop) is not
triggered for dependent work: `PHASE-002` continued and closed. It is a scheduling and
classification gap, and it is recorded rather than guessed.

## Lifecycle-relationship analysis (added by the `PHASE-002` acceptance review, 2026-09-21)

The question *"one lifecycle or two?"* has four candidate answers. Evaluating them against the
two Project Operating System laws — `ONE FACT → ONE CANONICAL OWNER → MANY REFERENCES` and
`STATE ≠ WORK ≠ KNOWLEDGE ≠ HISTORY` — eliminates three of them on repository evidence, and
the remaining one still needs the owner for its scheduling half.

| Model | What it would mean | Verdict against the two laws |
|---|---|---|
| **One merged lifecycle** | the statistical programme becomes phases of `CTRL-MANIFEST` | **Fails `STATE ≠ WORK`.** `MANIFEST` would have to carry increment-level delivery work, which is what work items own. It also stops live delivery until `PHASE-010`, a consequence the owner may accept but the protocol cannot impose |
| **Two independent lifecycles** | each runs its own roadmap, gates and decisions, neither constrains the other | **Fails `ONE FACT → ONE OWNER`** at exactly one point: both touch canonical architecture. Independence is safe for schedules and unsafe for authority |
| **Parent / child** | the statistical programme is a sub-lifecycle of rehabilitation | **Fails on evidence.** The child is *ahead* of the parent — it is implementing while the parent forbids implementation. A child that outruns its parent is not a child; describing it as one would make the roadmap read as false |
| **Two programmes, one authority plane** | separate schedules and gates; a single owner for architectural authority, which both obey | **Satisfies both laws.** Schedules are `WORK`; the canonical architecture is `KNOWLEDGE`; `CTRL-CURRENT` stays the single `STATE`. Divergence is prevented by an authority boundary rather than by a merged calendar |

### What the repository already determines — no owner decision required

These hold today under existing canonical authority, and the acceptance review records rather
than creates them:

1. **Architectural authority is already single.** Any change to M3/M2 semantics, to a `CAD-02`
   responsibility, or to a canonical authority requires the existing decision mechanism — an
   ADR plus a bounded governance card — no matter which programme makes it (`CAD-01`,
   `CAD-16`, `/CLAUDE.md` §1). Neither programme may create a competing authority.
2. **There is exactly one rehabilitation roadmap.** `CTRL-MANIFEST` (RCP §22). The statistical
   programme's `G0…G4` are its own delivery gates and **may not declare a gate over a
   rehabilitation phase**, which `CTRL-ADOPTION` §8.3 already states.
3. **There is exactly one current-state authority**, `CTRL-CURRENT` (ADR-013). A second
   programme does not get a second `CURRENT`.
4. **M1-instance work is not architecture work.** Authoring contracts, filling datasets and
   loading data exercise the existing architecture; they do not redefine it. That is why the
   two programmes can legitimately run at once at all.

The unresolved part is therefore **narrower than `DEF-08` implies**: not *may they coexist*
(they already do, lawfully) but *what happens when they collide at `PHASE-004`*.

## Decision needed (owner)

| # | Question | If unanswered |
|---|---|---|
| 1 | One schedule or an explicit split between the rehabilitation lifecycle and the statistical-contract programme? | two schedules keep claiming the same repository; the first real collision decides by accident |
| 2 | If split: what may each side do to the other — may the programme change contracts, migrations or canonical schema that `PHASE-004` will design? | design will be handed a target that moved while it was being designed |
| 3 | Is `STATISTICAL-CONTRACT-OPEN-QUESTIONS.md` promoted to `CANONICAL` for statistical-domain standards and decisions, or does `S1…S16` move to an artifact that already is? | a set of binding standard selections keeps living in a `SUPPORTING` document |
| 4 | Who confirms the nine `DEFAULT` decisions (`Q02, Q03, Q05, Q06, Q10, Q12, Q13, Q14, Q15`) awaiting `OWNER` / `STEWARD`? | engineering proceeds on defaults nobody ratified |
| 5 | If `PHASE-004` design contradicts something the statistical programme has already shipped to dev, which one yields? | the collision is resolved by whoever is typing at the time |

**Recommended model, not decided:** *two programmes, one authority plane* — the only one of
the four that satisfies both Project Operating System laws (see the analysis above). Its
structural half is already binding; its scheduling half is questions 1, 2, 4 and 5, which are
product-governance decisions an agent may not take.

## Scope

**In scope**

- Recording the finding, the decision points and their consequences.
- After the decision: updating `MANIFEST` (`DEF-08`), `CTRL-ADOPTION` (classification), and
  `STANDARDS.md` (the known-classification-gap note), plus one `AIR` entry.

**Out of scope**

- Deciding any of the four questions.
- Changing the statistical programme's artifacts, gates or decisions.
- Editing `docs/work/ARCHITECTURE-IMPROVEMENT-REGISTER.md` while it holds uncommitted
  third-party changes — see *First action*.

## First action when unblocked

Write the `AIR` entry (next free identifier: **`AIR-2026-056`**; `AIR-2026-055` is the
highest in use) for the domain-profile classification gap, with the evidence in this item.
It was not written during `PHASE-002` because the register carried 95 lines of uncommitted
work by another author, and editing it would have absorbed that work into `TASK-001`'s
completion claim, which `/AGENTS.md` §7 forbids. **The finding is not lost — it is here, and
it is referenced from `STANDARDS.md` and `STD-BENCH-001` §9.**

## Dependencies

- `TASK-001` — raised this item; complete.

## Handoff

Nothing is blocked *by* this item today: `PHASE-003` may proceed, because the comparative
audit reads the statistical programme's artifacts as evidence rather than as authority.
The collision becomes real at `PHASE-004`, when design fixes decisions that the other
programme is already implementing. **That is the deadline for question 1.**

## Closure record

- **Outcome:** COMPLETE — decided by **ADR-014**, which adopts *two programs, one authority
  plane* with four mechanically checkable rules, five coordination instruments and a review
  condition that fires on the first `PHASE-004` collision.
- **Evidence:** `docs/decisions/ADR-lifecycle-program-separation.md`; registered in
  `docs/platform-decisions.md`; `DEF-08` closed in `CTRL-MANIFEST`.
- **Question 3 answered as a consequence, not a separate decision:**
  `docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md` is now classified `CANONICAL` for the
  statistical domain's decisions and standards (`CTRL-ADOPTION` §4), because ADR-014 rule 1
  establishes that a program owns its domain profile under the shared platform profile. The
  routing defect that `PHASE-002` found is closed at both ends.
- **Residue handed to the statistical program, not to this lifecycle:** the nine `DEFAULT`
  decisions (`Q02, Q03, Q05, Q06, Q10, Q12, Q13, Q14, Q15`) still await `OWNER`/`STEWARD`
  ratification — they bind M1 semantics, which ADR-014 rule 4 places in that program's scope.
- **Follow-ups raised:** `AIR-2026-056` stays reserved for the routing defect's register entry,
  to be written when the AIR register no longer carries another author's uncommitted work.
