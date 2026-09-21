---
id: CTRL-MANIFEST
type: CONTROL
title: Project lifecycle manifest
status: ACTIVE
authority: CANONICAL
scope: whole repository
created: 2026-09-20
updated: 2026-09-21
---

# MANIFEST — lifecycle and gates

The single lifecycle authority. `CURRENT` points at a state defined here; this file
defines what the states are and what it takes to leave one.

Status vocabulary is closed (RCP §5): `NOT_STARTED` `READY` `ACTIVE` `BLOCKED` `VERIFYING`
`PASS` `FAIL` `COMPLETE` `SUPERSEDED` `CANCELLED`.

---

## Phases

| ID | Phase | Status | Purpose |
|---|---|---|---|
| `PHASE-001` | RECOVERY | **COMPLETE** | understand the existing system well enough that remaining work is design/implementation, not discovery |
| `PHASE-002` | STANDARDS & ARCHITECTURE QUALITY BENCHMARK | **COMPLETE** | benchmark recovered requirements against external standards before canonical design fixes decisions |
| `PHASE-003` | ARTIFACT COMPARATIVE AUDIT | **COMPLETE** | compare legacy Access · R8 resource package · `KIDS_PACKAGE_candidate_2` · best justified canonical target |
| `PHASE-004` | CANONICAL DESIGN | **COMPLETE** | define the target architecture from requirements, invariants and recovered knowledge |
| `PHASE-005` | MASTER REHABILITATION PLAN | `READY` | dependency-ordered plan with gates — *what* is rehabilitated, in what order, and why that order |
| `PHASE-006` | EXECUTION PACKAGE | `NOT_STARTED` | the machine-followable execution manifest derived from the plan — *how* each step is carried out and verified |
| `PHASE-007` | INDEPENDENT ADVERSARIAL REVIEW | `NOT_STARTED` | hostile review of design and plan by an independent agent/reviewer |
| `PHASE-008` | CORRECTED / APPROVED CANONICAL PLAN | `NOT_STARTED` | incorporate review findings; the approved plan becomes the execution authority |
| `PHASE-009` | PROTECTION | `NOT_STARTED` | build the regression and fitness protection that migration depends on |
| `PHASE-010` | CONTROLLED IMPLEMENTATION / MIGRATION | `NOT_STARTED` | execute under three-gate discipline |
| `PHASE-011` | LEGACY ELIMINATION | `NOT_STARTED` | CAD-13 sequence, per disposition |
| `PHASE-012` | FINAL ARCHITECTURE AUDIT | `NOT_STARTED` | re-run the CAD-18 scoreboard against the rebuilt repository |
| `PHASE-013` | REHABILITATION COMPLETE | `NOT_STARTED` | terminal state; reached only when CAD-18 repository compliance passes |

**This table is the only roadmap.** A second one, anywhere, is a defect (RCP §22).
`PHASE-009 · PROTECTION` is the one phase not named in the commissioning program; it is
required by PASS 3 closure §1 (21 protection requirements) and sits where the program
implies — after the approved plan, before any migration executes.

> **Correction 1, 2026-09-20 (RCP implementation audit).** The first version of this
> manifest carried eight phases and silently omitted **Artifact Comparative Audit**,
> **Execution Package**, **Independent Adversarial Review**, **Corrected/Approved Canonical
> Plan**, **Final Architecture Audit** and **Rehabilitation Complete** — six of the eleven
> steps of the established rehabilitation program. That was semantic loss of the program
> itself. Phase IDs were renumbered; `PHASE-001` is unchanged and no completed state was
> altered.

> **Correction 2, 2026-09-20 (durable-knowledge closure).** Correction 1 restored the
> Execution Package as a *word* but left it **merged into `PHASE-005`** with the Master
> Rehabilitation Plan. The two are distinct deliverables with distinct failure modes: a
> plan can be sound while its execution manifest is unfollowable, and a merged phase can be
> declared complete on the strength of the plan alone. They are now `PHASE-005` and
> `PHASE-006` with separate gates. Phases 006–012 shifted up by one; no completed state was
> altered, and `PHASE-001`…`PHASE-005` keep their identities.
>
> **This is the same defect class twice.** Both times a distinct step survived as
> vocabulary while losing its independent gate. `RCP-511` now enforces phase contiguity and
> a single terminal phase, so a step can no longer be dropped or absorbed without the
> validator failing.

### PHASE-001 · RECOVERY — COMPLETE

**Outputs:** consolidation baseline · canonical authority doctrine · layer coverage ledger
· completeness audit · PASS 3 closure · architecture recovery checkpoint.

**Stages and gates, all closed:**

| Stage | Gate | Result | Evidence |
|---|---|---|---|
| Batches 1–11E | — | complete | checkpoint §23 |
| Consolidation | — | `PASS` | consolidation §16 |
| PASS 1 — evidence & authority | `GATE-P1` | **PASS** | audit §1A–1B; 10/10 inherited findings, RC falsification ×3, producer & collision sweeps |
| PASS 2A — data semantics | `GATE-P2A` | **PASS** | audit §1C–1F; information-loss matrix, 3 vertical slices |
| PASS 2B — runtime & operational | `GATE-P2B` | **PASS** | audit §1G–1I |
| PASS 3 — protection & falsification | `GATE-RECOVERY-FINAL` | **PASS** | PASS 3 closure; CAD18 recovery-knowledge PASS |

**Exit criteria — all met:** no material layer `NOT_INSPECTED` · no `PARTIAL` whose unknown
can change canonical design · no unexplained parallel authority · all
`REQUIRED-BUT-MISSING` capabilities bounded · all legacy mechanisms dispositioned · all
cross-layer invariants known · recovery knowledge repository-materialized.

### PHASE-002 · STANDARDS & ARCHITECTURE QUALITY BENCHMARK — COMPLETE

**Entry:** `PHASE-001` COMPLETE ✔
**Purpose:** evaluate recovered requirements against external standards *before* canonical
design fixes decisions — so the target is justified, not merely internally consistent.
**Depends on:** `STANDARDS.md` adoption profile; REQ-001…041.
**Gate `GATE-STANDARDS`:** every material quality attribute has a named standard or an
explicit decision not to adopt one. **Result: PASS (2026-09-21)** — condition-by-condition
evidence in `docs/standards/ARCHITECTURE-QUALITY-BENCHMARK.md` §11; work item `TASK-001`.

**Outputs:**

| Output | Where |
|---|---|
| Standards & architecture quality benchmark; quality-attribute model; hard invariants; cross-layer evaluation criteria; rejection criteria; verdict method; unresolved standards questions | `docs/standards/ARCHITECTURE-QUALITY-BENCHMARK.md` (`STD-BENCH-001`) |
| Automation opportunity / classification model | `docs/standards/AUTOMATION-CLASSIFICATION-MODEL.md` (`STD-AUTO-001`) |
| Adopted / adapted / conditional / rejected standards matrix, and domain-profile routing | `docs/reference/engineering/STANDARDS.md` — the existing owner, extended, not duplicated |
| Object-storage responsibility classified (`DEF-04`) | `DOC-CAD` §2.2, §2.3 |
| Control-plane validator in CI (`DEF-01`) | `.github/workflows/architecture-governance.yml` |

**What the phase deliberately did not do:** design anything, choose a physical schema, touch
application or runtime code, migrate or delete legacy, or decide `DEF-08`. `REC-PASS3` §10
keeps the true design questions; `TASK-002` keeps the owner decision.

**Carried forward:** eight unresolved standards questions `BM-Q-01…08`, each with an owner,
an interim rule and a resolution timing (`STD-BENCH-001` §9). **`BM-Q-01` and `BM-Q-02` block
`PHASE-004`** — both decide what the canonical model is obliged to express. `BM-Q-03`
(performance values) binds before `PHASE-010` and `BM-Q-05` (ASVS subset) before `PHASE-009`;
neither blocks design, because the attribute they serve requires a *declared bound* rather
than a particular number. **None blocks `PHASE-003`.**

> **Correction, 2026-09-21 (`PHASE-002` acceptance review, `TASK-003`).** The phase first
> recorded `BM-Q-03` and `BM-Q-05` as `PHASE-004` blockers. Re-examined against the threshold
> classes, that was too strong: `BM-QA-09` is `TARGET`/`BOUNDED`, and the ASVS subset selects
> evidence depth rather than architectural shape. The same review found that the gate's
> headline condition — every quality attribute naming a standard or an explicit
> non-adoption — was satisfied by an assertion rather than by a visible mapping, and that two
> of five adversarial candidate architectures passed a benchmark that should have rejected
> them. **`GATE-STANDARDS`'s first `PASS` was premature on that condition; it is re-confirmed
> with the corrections in place, not retroactively excused.** Two invariants, two criteria,
> nine automation responsibilities and one automation rule were added; three criteria were
> repaired. Evidence:
> `docs/work/evidence/phase-002-acceptance-review/gate-standards-reconfirmation.txt`.

### PHASE-003 · ARTIFACT COMPARATIVE AUDIT — COMPLETE

**Entry gate `GATE-COMPARATIVE`:** `PHASE-002` COMPLETE ✔ · benchmark evidence-bound ✔ ·
comparison inputs identified ✔ (`REC-CONSOLIDATION` §15). **Result: PASS (2026-09-21)** —
work item `TASK-004`; exit-test evidence in `AUD-COMPARATIVE` §21.

**Outputs:**

| Output | Where |
|---|---|
| Artifact identity, responsibility classification, fifteen-level comparison, semantic structural diff, loss and innovation analysis, falsification, benchmark traceability | `AUD-COMPARATIVE` |
| Capability register with dispositions, required-but-missing capabilities, the `PHASE-004` input package | `AUD-CAPABILITY` |
| The three artifact inputs of `REC-CONSOLIDATION` §15 judged | that register, updated in place |

**What it found, in one line each:** no artifact passes the invariant gate, so none is canonical
· `candidate_2` implements the per-combination constant (`CF-015`) that recovery recorded as
missing · the R8 package manifest verifies 451/451 digests, the strongest measured integrity in
the repository · two files with one name and one declared identity are byte-different and
semantically identical, so a byte digest cannot serve as artifact↔contract correspondence ·
eleven capabilities are missing from **every** artifact.

**What it deliberately did not do:** select an artifact, design anything, or reach outside
repository authority for `KIDS_PACKAGE_candidate_3`, which the control plane does not govern.

### PHASE-004 · CANONICAL DESIGN — COMPLETE

**Mandatory evidence step added 2026-09-21, inside the phase, not as a new phase.** No canonical
decision that generalizes, replaces, merges or removes a relational structure may be made before
the pre-existing physical model is reconstructed from the artifacts. `ARCH-BASELINE` discharges
it: 113 migrations, 120 tables, 1 077 columns, 120 PKs, 122 FKs, 87 UNIQUE, 127 CHECK, 59
indexes (9 unique, 8 filtered), 18 triggers, 1 view, 1 procedure.

**Rule in force for the rest of the phase:** *preserve proven relational strength; generalize
only where real variation requires it.* Replacing a typed guarantee — PK, FK, UNIQUE, CHECK,
type, cardinality — with metadata interpretation, generic edges, JSON or application convention
is a **potential regression** and must be justified against `ARCH-BASELINE` §6 before it is
adopted.

**Result: `GATE-DESIGN-ENTRY` PASS, phase COMPLETE (2026-09-21)** — work item `TASK-006`.

**Outputs:** `ARCH-BASELINE` · `ARCH-DOSSIER` · `ARCH-CANONICAL` (**Part II §30–§40 governs**).

**Non-regression, proven:** 19/19 physical guarantees preserved or strengthened; 6/6 draft
contradictions resolved by re-derivation; 0 unjustified regressions remaining. Two guarantees
weaken in the generic kernel (per-field `NOT NULL`, scalar range checks); both are named,
bounded by declared materialization criteria, and regress nothing that exists.

**The result that shaped everything else:** a stable physical kernel of value-domain-partitioned
typed tables, with the logical model evolving by declaration — derived from
`entity.localized_text`, the strongest pattern already in the database — so an ordinary new
dataset, field, relationship or classifier needs **no platform DDL, no JPA entity and no
repository class**, while grain, referential integrity and cardinality stay engine-enforced.



#### PHASE-004 entry record — retained, superseded by the section above

*This is the admission record written when `PHASE-004` was `READY`. It is kept because the
inputs and constraints it enumerates are what the phase was obliged to consume, and deleting
it would lose that obligation. **It states no current status**; the phase's status is
`COMPLETE`, owned by the section above and by the lifecycle table.*

**Entry gate `GATE-DESIGN-ENTRY`:**

| Check | Status |
|---|---|
| Recovery COMPLETE | ✔ |
| Handoff exists (proven / required / forbidden / missing) | ✔ PASS 3 closure §10 |
| Authority registry populated | ✔ CAD-02 |
| Standards benchmark complete | ✔ `PHASE-002`, `GATE-STANDARDS` PASS 2026-09-21 |
| Artifact comparative audit complete | ✔ `PHASE-003`, `GATE-COMPARATIVE` PASS 2026-09-21 |

**Result: READY — every listed check passes, and the three conditions that attached to its
acceptance are now decided** (`TASK-005`, 2026-09-21): `ADR-014` two programs / one authority
plane, `ADR-015` SDMX conformance boundary, `ADR-016` Access provider status. `AQ-05` is
resolved on provenance evidence and blocks nothing.

**Design consumes `AUD-CAPABILITY` §5, not the artifacts.** `PHASE-003` proved that no artifact
passes the invariant gate, so the target is not a copy of any of them, and eleven capabilities
(`CAP-M01…M11`) exist in none.

**`PHASE-004` is a derivation, not a selection.** It is forbidden to frame its task as *choose
the best candidate*: it must be free to produce an architecture materially better than every
artifact inspected, and it must satisfy `STD-BENCH-001`'s invariant gate, which none of them
does. What it consumes, in full:

| Input | Owner |
|---|---|
| Recovered requirements, intent, findings, invariants `INV-001…014` | `REC-CONSOLIDATION` |
| Canonical authority doctrine `CAD-01…18` and the `CAD-02` registry | `DOC-CAD` |
| Standards profile and dispositions | `docs/reference/engineering/STANDARDS.md` |
| Quality attributes, hard invariants `BM-INV-01…24`, criteria, rejection criteria | `STD-BENCH-001` |
| Maximum Safe Automation targets `MSA-001…044` and its rules | `STD-AUTO-001` |
| Comparative evidence, semantic diff, loss and innovation analysis | `AUD-COMPARATIVE` |
| Capability inventory, dispositions, required-but-missing, the input package | `AUD-CAPABILITY` |
| Protection requirements and the design handoff | `REC-PASS3` §1, §10 |
| **Statistical domain decisions `Q01…Q50` and standards profile `S1…S16`** — `DECIDED` entries bind M1 semantics (`ADR-014` rule 1) | `DOM-STATISTICAL` |
| Capability envelope, metadata responsibility model, relationship space, logical/physical separation, governed-chain verdicts | `AUD-CAPABILITY` §6–§11 |
| Binding programme, SDMX-boundary and provider decisions | `ADR-014`, `ADR-015`, `ADR-016` |
| Open questions, each classified by blocking stage | `STD-BENCH-001` §9, `AUD-COMPARATIVE` §22 |

**Two obligations travel into the phase:** `ADR-014`'s review condition fires on the first
collision with the statistical programme's already-`DECIDED` M1 semantics, and `ADR-016`'s
provider rules are unenforceable until provider capability has exactly one producer.

**Sixteen capabilities are required and absent from every artifact** (`CAP-M01…M16`), five of
them added by the design-entry capability-envelope pass. **Four concerns are in the envelope
with no owner at all** — tidy/long structure description, product-like objects, declarable
`MANY_TO_MANY`, and impact analysis. Design must be free to add them; it is not limited to
recombining what exists.

**Constraints carried in:** CAD-17 — legacy converges toward canonical design, never the
reverse. Design must not be shaped around accidental legacy structure.

### PHASE-009 · PROTECTION — NOT_STARTED

**Entry:** `PHASE-008` COMPLETE (approved plan). **Input:** PASS 3 closure §1 (21 protection requirements).
**Hard rule:** protection precedes the migration it protects. The publication writer,
service and store are entirely untested today.

### PHASE-003, 005–008, 010–013

Defined on entry. `PHASE-005` produces the dependency-ordered plan; `PHASE-006` turns it
into an execution manifest and **may not be closed on the plan's evidence**. `PHASE-010`
inherits **AMS-001**: the governed-statistical-publication set is atomic — no member ships
alone. `PHASE-011` follows CAD-13; `MIGRATED ≠ DONE`. `PHASE-012` re-runs the CAD-18
scoreboard recorded in PASS 3 closure §11; `PHASE-013` is reachable only when its
repository-compliance column passes.

---

## Gate register

| Gate | Guards | Result | Failure consequence |
|---|---|---|---|
| `GATE-P1` | evidence & authority recovery | **PASS** | — |
| `GATE-P2A` | data semantics | **PASS** | — |
| `GATE-P2B` | runtime & operational | **PASS** | — |
| `GATE-RECOVERY-FINAL` | recovery closure | **PASS** | — |
| `GATE-STANDARDS` | standards benchmark | **PASS** (2026-09-21, re-confirmed the same day after adversarial acceptance review `TASK-003` found and closed two material holes) — evidence: `STD-BENCH-001` §11 and §8 F-7/F-8; `docs/work/evidence/phase-002-acceptance-review/gate-standards-reconfirmation.txt` | design proceeds unjustified |
| `GATE-COMPARATIVE` | artifact comparative audit | **PASS** (2026-09-21) — evidence: `AUD-COMPARATIVE` §21; `docs/work/evidence/artifact-comparative-audit/gate-comparative.txt` | canonical target chosen without artifact evidence |
| `GATE-DESIGN-ENTRY` | canonical design entry | **PASS** (2026-09-21) — evidence: `ARCH-CANONICAL` §36–§37, `ARCH-DOSSIER` §8 | design rediscovers the system |
| `GATE-PLAN` | master rehabilitation plan (`PHASE-005`) | `READY` | migration ordered by convenience rather than dependency |
| `GATE-EXECUTION-PACKAGE` | execution manifest (`PHASE-006`) | `NOT_STARTED` | an approved plan nobody can actually follow step by step |
| `GATE-REVIEW` | independent adversarial review | `NOT_STARTED` | plan executes unchallenged |
| `GATE-PLAN-APPROVED` | review findings incorporated (`PHASE-008`) | `NOT_STARTED` | review findings noted and then ignored |
| `GATE-PROTECTION` | protection before migration | `NOT_STARTED` | migration without regression safety |
| `GATE-MIGRATION` | three-gate discipline per task | `NOT_STARTED` | uncontrolled regression |
| `GATE-ELIMINATION` | CAD-13 per disposition | `NOT_STARTED` | parallel architecture persists |
| `GATE-FINAL-AUDIT` | CAD-18 repository compliance | `NOT_STARTED` | rehabilitation declared complete while parallel authority remains |
| `GATE-REHABILITATION-COMPLETE` | terminal state (`PHASE-013`) | `NOT_STARTED` | the program is declared finished without its own audit passing |

> `PASS` means the gate's evidence was satisfied. It is never a narrative compliment.

**Every phase has exactly one entry gate.** A phase without one can be entered by
assertion, which is how a step becomes vocabulary — the defect both roadmap corrections
above describe.

## Standing obligations

These hold in **every** phase. They are not scheduled work; they are conditions the
repository must continuously satisfy.

| ID | Obligation | Enforcement |
|---|---|---|
| `OBL-RCP-VERIFY` | `python ops/cli/validation/rcp-verify.py` exits `0` before any session ends and before any claim that the control plane is consistent | deterministic — the validator itself (`CTRL-VERIFY`) |
| `OBL-VERIFY-PROVEN` | every validator check keeps a negative test that injects its defect and asserts it fires | `python -m unittest discover -s ops/tests/governance` (`CTRL-VERIFY-TEST`) — adding a check without a test is an incomplete change |
| `OBL-ONE-AUTHORITY` | no responsibility acquires a second canonical authority | CAD-01…18; RCP-403/701 mechanically, CAD-03 by gate |
| `OBL-NO-SILENT-LOSS` | nothing is moved, merged, superseded or archived until its unique knowledge has a canonical destination | RCP §20; `ADOPTION` dispositions |
| `OBL-EVIDENCE` | a checklist item closes only with evidence; missing evidence means `OPEN / NOT READY` | `/AGENTS.md` |

## Inherited platform obligations

**References, not a backlog.** These predate the rehabilitation lifecycle and belong to the
platform's production-readiness program. They are listed here only so that a fresh agent
can find them; each stays owned and maintained where it already lives, and **nothing below
is restated** (CAD-01).

| Obligation set | Canonical owner | State at adoption |
|---|---|---|
| `W-01…W-07` — release provenance · OIDC/RBAC/ABAC/tenancy · independent provider proof · distributed quota · observability/SLO · reconciliation/DR · security & resilience acceptance | `docs/platform-capability-and-architecture-audit-2026-09-13.md` §17 | declared *"the complete remaining goal scope"* on 2026-09-14, dependency order `W-01 → W-02/W-04/W-05 → W-03 → W-06 → W-07`; **7 open** |
| `C-01…C-14` — contract authority, provider registry, second site, family lifecycle, keyset execution, semantic corpus, legacy retirement, SDK/streaming, documentation reconciliation | `docs/contract-driven-metadata-schema-agnostic-completion-plan.md` | 122 open/partial checklist markers |
| API modernization capability gaps | `docs/api-modernization-capability-gap.md` | open |
| Improvement / technical debt | `docs/work/ARCHITECTURE-IMPROVEMENT-REGISTER.md` | AIR-#### register, 56 entries |
| Production evidence that only a deployed system can close | `docs/production-evidence-handoff.md` | ten audit items |

**These do not gate any rehabilitation phase, and no rehabilitation phase closes them.**
Whether the two programs merge, stay parallel or one subsumes the other is `DEF-08`.

> The Docker boundary, recorded in the capability audit, applies to all of them:
> containers can close technical implementation and staging acceptance, but cannot produce
> approved release authority, real OIDC business policy, an approved tenant boundary,
> official RPO/RTO, or permission to deploy to production. Those require a human decision.

## Deferred hardening — with firing triggers

Each row is a control that is **specified and assigned but not yet built**. A deferral
without a trigger is an abandonment, so every row names the condition that makes it due.
The trigger is what makes the deferral survivable; "later" alone does not.

| ID | Deferred control | Owner | **Trigger — becomes due when** |
|---|---|---|---|
| ~~`DEF-01`~~ | ~~run `rcp-verify.py` in CI, not only on demand~~ | — | **CLOSED 2026-09-21.** Trigger fired when `PHASE-002` opened. `.github/workflows/architecture-governance.yml` now runs the validator on every push and pull request to `main`/`master`, beside the governance negative tests and the review-range gate. **Branch protection remains an external control** and is not asserted as installed (`docs/reference/engineering/README.md`) |
| `DEF-02` | write status markers into the 8 `final-*` headers (`ADOPTION` §5) | `PHASE-011` | any one of the eight is edited for content, **or** `PHASE-011` opens |
| `DEF-03` | merge `canonical-directory-blueprint.md` into `CANONICAL-FULL-TREE.md` and delete its stale "normative and enforceable" line (`ADOPTION` §7.1) | `PHASE-011` | either file is edited, **or** a second document claims directory authority |
| ~~`DEF-04`~~ | ~~give object-storage flow a CAD-02 responsibility row, or record why it needs none~~ | — | **CLOSED 2026-09-21.** Trigger fired when `PHASE-002` opened. `DOC-CAD` §2.2 carries the row *Stored artifact bytes & content identity*; §2.3 holds the producer evidence, the classification and the residual defect (registry ↔ store lifecycle alignment is convention-only, now acceptance condition `BM-INV-22`) |
| `DEF-05` | extend RCP-605 to non-`.md` references and to link targets inside prose | `PHASE-003` | a broken reference is found that the current check missed. **Checked 2026-09-21, not fired:** a sweep of every backticked path and markdown link in `docs/project/` and `docs/standards/` found 0 broken references, so there is no evidence the current check misses anything. Re-checked whenever a reference defect is found |
| `DEF-06` | `CLEAN_BUILD_UPGRADE_EQUIVALENCE` proof (spec: PASS 3 closure §2) | `PHASE-009` | **UNPROVEN** — due before the first migration `PHASE-010` executes |
| ~~`DEF-08`~~ | ~~reconcile the Access-package work stream with this lifecycle — one schedule or an explicit split~~ | — | **CLOSED 2026-09-21 by `ADR-014`.** Fired when `PHASE-002` opened; the scope proved wider than `ADOPTION` §8.3 stated. Decided as **two programs, one authority plane**: separate schedules and gates, one authority registry and ADR plane, the cut at the meta-level (M1-and-below vs M2/M3), and a review condition that fires on the first `PHASE-004` collision. `docs/project/work/completed/TASK-002-work-stream-reconciliation.md` |
| ~~`DEF-09`~~ | ~~decide whether the transcripts hold unextracted durable knowledge~~ | — | **CLOSED 2026-09-20.** The extraction audit (`REC-CONSOLIDATION` §1.4a) found 22 durable items, all with canonical owners, and **0 unowned**. `OBL-NO-SILENT-LOSS` no longer blocks archival; the three transcripts carry `ARCHIVE_LATER` under `PHASE-011` as ordinary legacy elimination |
| `DEF-07` | machine-checked expiry on temporary architecture (RCP §22 "unbounded temporary architecture") | `PHASE-006` | the first *new* temporary mechanism is introduced. Today the register in PASS 3 closure §5 records only **recovered** items, which have dispositions rather than expiry dates — there is nothing yet to expire. Structuring it before then would create a second register for a responsibility `REC-PASS3` already owns (CAD-01) |

**Reading rule.** A trigger that has fired and been ignored is a governance defect, not a
backlog item. When a trigger fires mid-task, the correct action is to record it as a work
item under `CTRL-WORK`, not to do it opportunistically inside unrelated work.
