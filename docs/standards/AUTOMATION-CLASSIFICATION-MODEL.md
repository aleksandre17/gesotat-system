---
id: STD-AUTO-001
type: REFERENCE
title: Automation classification model
status: ACTIVE
authority: CANONICAL
scope: target automation class per platform responsibility, and the rule that assigns it
owner: PHASE-002
created: 2026-09-21
updated: 2026-09-21
related: STD-BENCH-001, REC-CONSOLIDATION, REC-PASS3
---

# AUTOMATION CLASSIFICATION MODEL

**Maximum Safe Automation, made decidable.** `REQ-010` requires maximum *safe* automation and
`REQ-011` minimum necessary manual input, but neither says how to tell which is which. This
model supplies the decision rule, the safety preconditions that make `AUTOMATE` legitimate,
and the **target** class of each platform responsibility.

Produced by `PHASE-002`. It classifies; it does not design or implement.

---

## 0. Ownership

**Owns:** the classification vocabulary (§1), the decision procedure (§2), the safety
preconditions for automation (§3), the target classification register `MSA-001…044` (§5),
and the unsafe-automation rules (§6).

**Does not own, and never restates:**

| Responsibility | Canonical owner |
|---|---|
| The **current** automation baseline — what is automated today | `REC-CONSOLIDATION` §10 |
| The design handoff's `MUST BE AUTOMATED` / `MUST REMAIN EXPLICIT HUMAN SEMANTICS` lists | `REC-PASS3` §10 |
| Derivation-over-repetition rule `CANONICAL DECLARATION → VALIDATE → DERIVE → GENERATE → EXECUTE → VERIFY` | `CAD-05` |
| Generic automation philosophy for repository artifacts | `docs/standards/PROJECT-OPERATING-SYSTEM.md` §18 |
| Quality attributes, invariants, evaluation and rejection criteria | `STD-BENCH-001` |
| Proven input patterns A / B / C | `REC-CONSOLIDATION` §3 |

The `Target` column below is **the class the architecture must reach**; the `Current`
question is answered by `REC-CONSOLIDATION` §10 and is deliberately not duplicated here, so
that the two cannot drift apart.

---

## 1. Vocabulary — closed

| Class | Meaning | Test that distinguishes it |
|---|---|---|
| `AUTOMATE` | the output is deterministically derivable from approved authority, and §3 holds in full | remove the human and the result is identical and verifiable |
| `ASSIST` | the machine proposes, validates, pre-fills or detects; a human accepts, corrects or approves | the machine can be *wrong* in a way only a human can judge, but being wrong is visible |
| `KEEP EXPLICIT` | the information could often be inferred, but the **declaration is the authority** and must be authored and readable | inference would be right most of the time — and silently wrong the rest, with no signal |
| `HUMAN DECISION` | the information does not exist until a person decides; there is nothing to derive | no input contains the answer |

**`KEEP EXPLICIT` and `HUMAN DECISION` are not the same thing**, and collapsing them is how
platforms acquire "clever" defaults. `KEEP EXPLICIT` says *do not infer what must be
declared*; `HUMAN DECISION` says *do not manufacture a judgement nobody made*. The first
protects semantics, the second protects accountability.

`AUTOMATE` is the goal, not the default: `BM-REJ-11` rejects an architecture that automates
past the preconditions, and `BM-REJ-10` rejects one that leaves derivable information to be
retyped. **Both directions are defects.**

---

## 2. Decision procedure

Applied to one responsibility at a time. The first `NO` decides.

```text
1. Is the output fully determined by approved authority already in the system?
      NO  -> does the missing input exist anywhere?
              NO  -> HUMAN DECISION
              YES -> ASSIST  (surface the input; let a human bind it)
      YES -> continue

2. Would deriving it silently overwrite or replace a declaration that must stay authoritative?
      YES -> KEEP EXPLICIT
      NO  -> continue

3. Does the action carry an irreversible, approval-bearing or externally visible effect
   (publication, promotion, approval, deletion, release)?
      YES -> ASSIST for the decision; AUTOMATE the execution *after* the decision
      NO  -> continue

4. Can the result be verified independently of the generator
   (digest, round-trip, reconciliation, constraint)?
      NO  -> ASSIST until a verification exists
      YES -> continue

5. Do all safety preconditions in §3 hold?
      NO  -> ASSIST, and record the missing precondition as the entry condition
      YES -> AUTOMATE
```

Step 3 is the one most often skipped. It is why publication *execution* may be automatic
while publication *approval* may not, and why `AMS-001` treats the two as different things.

---

## 3. Safety preconditions for `AUTOMATE`

All must hold. A responsibility that satisfies most of them is `ASSIST`, not "`AUTOMATE`
with a caveat".

| # | Precondition | Falsifier |
|---|---|---|
| 1 | **Deterministic** — same approved inputs, same output, independent of ordering, host, locale and clock | two runs differ in any byte that is not a declared timestamp |
| 2 | **Reproducible** — re-runnable later from retained inputs | inputs are not retained, or are mutable |
| 3 | **Traceable** — the output names its source authority, source revision and generator identity/version | the artifact cannot answer "what produced me" (`BM-INV-07`) |
| 4 | **Version-aware** — the generator refuses inputs of an unsupported grammar version rather than guessing | an older revision is silently reinterpreted |
| 5 | **Validatable** — an independent check can confirm correspondence without re-running the generator | the only proof of correctness is the generator itself |
| 6 | **Regenerable** — the output can be discarded and rebuilt without loss | the artifact has become the only home of some fact |
| 7 | **Fail-closed** — ambiguity, an unsupported capability or a failed check stops the operation | a fallback, default or partial result is produced |
| 8 | **Idempotent** — re-execution produces no second logical effect | a retry duplicates the effect |
| 9 | **Bounded** — declared limits on rows, memory, time and output size | an input size exists that exhausts a resource |
| 10 | **Observable** — success and failure are both visible, correlated and actionable | a silent failure mode exists |

**Generation/validation symmetry** is required where the generated artifact re-enters the
platform (package → ingestion): the validator must accept exactly what the generator
produces, and reject what it does not (`BM-INV-21`, `BM-EC-053`). It is **not** required
where generation is terminal (an export that nothing reads back) — demanding it there would
be ceremony, and `BM-REJ-01` would reject it.

---

## 4. How to read the register

`Target` is the class the Canonical Architecture must reach. `Precondition` names what must
exist first — an empty precondition means the class is achievable with what already exists.
`Verification` names how the class is proven, because *"automated"* without a check is only
*"unattended"*.

Rows marked **†** are blocked by a `REQUIRED-BUT-MISSING` authority or component
(`REC-PASS3` §4, `REC-CONSOLIDATION` §9): the
automation cannot be built before its producer exists, and building it earlier would place
the automation itself in the authority's seat — which is exactly how `CF-033` happened.

---

## 5. Target classification register — `MSA`

### 5.1 Control — declaration, grammar, compilation

| ID | Responsibility | Target | Why | Precondition | Verification |
|---|---|---|---|---|---|
| `MSA-001` | Authoring a contract's semantics (what a dataset means) | `HUMAN DECISION` | meaning does not exist in any input; it is asserted by a domain owner | — | four-eyes approval record |
| `MSA-002` | Grammar validation of an authored contract | `AUTOMATE` | fully determined by the versioned grammar | — | malformed/unsupported negative corpus |
| `MSA-003` | Semantic resolution and compilation to an immutable plan | `AUTOMATE` | deterministic, digest-verifiable | — | digest equality across runs |
| `MSA-004` | Contract approval | `ASSIST` | approval is an accountable act; the machine may only check preconditions | — | approval binds the resolved closure |
| `MSA-005` | Contract withdrawal / supersession | `ASSIST` † | the decision is human; the effect propagation is mechanical | withdrawal path exists (`CF-014`) | state transition + consumer impact report |
| `MSA-006` | Site serving declaration (which datasets a site presents, how) | `KEEP EXPLICIT` † | a declaration is the authority; inferring it from data would invert M1 | Control Plane Schema producer (`RC-003`) | declaration → plan round trip |
| `MSA-007` | Product / tenant registration | `KEEP EXPLICIT` † | identity and tenancy are asserted, never inferred | product registration producer | tenancy negative test |
| `MSA-008` | Physical pattern per family/provider | `KEEP EXPLICIT` † | pattern is a declared reusable rule; today it is implicit in Java (`DR-008`) | declared pattern model | same pattern reproduces two families |
| `MSA-009` | Provider capability declaration (precision, scale, limits) | `KEEP EXPLICIT` † | capabilities are facts about an external product; discovery is fragile, declaration is auditable | capability producer (`CF-038`) | capability-exceeding declaration rejected |
| `MSA-010` | Physical plan derivation from the semantic plan | `AUTOMATE` | deterministic given plan + capabilities | — | plan digest; no site literal |
| `MSA-011` | Grammar ↔ parser parity check | `AUTOMATE` | mechanical, and drift is otherwise silent | conformance corpus (`DR-005`) | bidirectional corpus in CI |
| `MSA-036` | Deriving a **draft** contract from an existing artifact (a legacy schema, an existing package) | `ASSIST` | the artifact carries semantic decisions that cannot be safely derived — a column name is not a meaning. `PATTERN B` is the proven shape: propose, never promote | — | the proposal is `DRAFT`, carries its source, and no path promotes it without an approver |

### 5.2 Authoring and interchange

| ID | Responsibility | Target | Why | Precondition | Verification |
|---|---|---|---|---|---|
| `MSA-012` | Generating the authoring artifact (tables, keys, relations, codelists) | `AUTOMATE` | proven today: derived from the compiled plan, no site literals (`PATTERN A`) | — | regenerate twice, compare |
| `MSA-013` | Organising the artifact for the author (grouping, navigation) | `AUTOMATE` | derived from the semantic plan; a positive instance of `REQ-010/011/012` | — | derived organisation matches families |
| `MSA-014` | Pre-filling everything derivable in the artifact (codes, units, references) | `AUTOMATE` | manual repetition of derivable data is `BM-REJ-10` | — | filled values equal registry values |
| `MSA-015` | The domain data a person types | `HUMAN DECISION` | the values are the contribution; automating them would invent data | — | value-exact round trip |
| `MSA-016` | Package self-description (what it realises, by whom, from which revision) | `AUTOMATE` | derivable at generation time; its absence is `REQ-026/028` | generator identity carried | artifact answers "what produced me" |
| `MSA-017` | Package ↔ contract correspondence proof | `AUTOMATE` † | deterministic digest comparison; must precede any canonical effect | correspondence mechanism (`DR-007`) | mismatched package rejected |
| `MSA-018` | Package internal validation (shape, completeness) | `AUTOMATE` | mechanical | — | malformed package rejected |
| `MSA-019` | Accepting a package's own metadata *schema definitions* | `KEEP EXPLICIT` † | a transport artifact must never define canonical metadata (`CF-033/034`) | metadata schema producer | package-defined schema is refused |

### 5.3 Ingestion and canonical materialisation

| ID | Responsibility | Target | Why | Precondition | Verification |
|---|---|---|---|---|---|
| `MSA-020` | Source receipt, fingerprint, quarantine | `AUTOMATE` | mechanical and safety-critical; fail-closed by construction | — | malformed/oversized/interrupted tests |
| `MSA-021` | Mapping source columns to declared fields | `KEEP EXPLICIT` | a mapping is a semantic decision recorded in the contract, not a similarity guess | — | mapping is contract-bound |
| `MSA-022` | Normalisation (trim, case, type coercion within declared rules) | `AUTOMATE` | rules are declared; behaviour is testable | — | property tests incl. empty/null |
| `MSA-023` | Typed canonical materialisation | `AUTOMATE` | deterministic given contract + data | typed storage for every family (`CF-039`) | constraint violations fail, not coerce |
| `MSA-024` | Classifier proposal capture from external evidence | `AUTOMATE` | `PATTERN B` upstream half, proven | — | writes only proposals, never registry |
| `MSA-025` | Classifier promotion into the canonical registry | `ASSIST` † | promotion decides what a code *means* for everyone; it is a stewardship act | promotion path (`CF-016`) | promotion requires an approver identity |
| `MSA-026` | Reprocessing under a corrected revision | `ASSIST` † | the decision to reprocess is human; execution is mechanical and must be replayable | governed reprocess path | replay yields identical digests |
| `MSA-037` | Capturing lineage and provenance for every canonical record | `AUTOMATE` | derivable at write time and worthless if left to discipline; a lineage a human may forget to record is a lineage that is absent exactly when it matters | — | backward-chain walk from a served value terminates at a governed declaration (`BM-INV-23`) |

### 5.4 Release, publication, archive

| ID | Responsibility | Target | Why | Precondition | Verification |
|---|---|---|---|---|---|
| `MSA-027` | Release gate evaluation (quality, privacy, reconciliation) | `AUTOMATE` | deterministic checks producing durable evidence | — | gate evidence digest bound to the snapshot |
| `MSA-028` | Publication approval | `HUMAN DECISION` | releasing a number to the public is an accountable act | — | approval names exactly what is published |
| `MSA-029` | Publication execution after approval | `AUTOMATE` | mechanical, and must publish exactly the approved set | bounded membership (`CF-023`) | publish one, assert one |
| `MSA-030` | Rollback execution | `AUTOMATE` | must restore exactly the prior authoritative state, including every derived flag | rollback restores liveness fully | rollback → state equality assertion |
| `MSA-031` | Archive retention purge | `AUTOMATE` | policy-driven, lease-guarded, already proven | — | purge honours the approved window |
| `MSA-032` | Object-store ↔ registry lifecycle alignment | `AUTOMATE` | convention-only alignment is unenforced today; content identity must stay provable | integrity audit wired to reconciliation | orphan object / orphan row both detected |

### 5.5 Serving, delivery, governance

| ID | Responsibility | Target | Why | Precondition | Verification |
|---|---|---|---|---|---|
| `MSA-033` | Projection/presentation binding (what a page or chart shows) | `KEEP EXPLICIT` | presentation is a declaration; deriving it would create a second semantic model (`BM-INV-15`) | one chart authority (`DR-001`) | declared surfaces return identical numbers |
| `MSA-034` | Export conformance (SDMX-CSV and successors) | `AUTOMATE` | deterministic projection of canonical semantics | attribute round-trip (`CF-005`) | export ≡ canonical values, exactly |
| `MSA-035` | Architecture and governance fitness checks (authority registry, site literals, status markers, reference integrity) | `AUTOMATE` | structural, deterministic, and the highest-leverage protection there is (`REC-PASS3` §1) | — | one negative test per check, run in CI |
| `MSA-038` | Authoring a **structural** schema migration | `AUTOMATE` † | once the physical pattern is declared, a schema change is a derivation from the contract diff, not prose. Hand-authored structural migrations are how declarations ended up inside the migration chain (`RC-002`, `CAD-09`) | declared physical pattern (`MSA-008`) | generated migration reproduces the declared target schema exactly |
| `MSA-039` | Authoring a **data-correcting** migration | `ASSIST` | a correction asserts what the data should have been — a semantic claim. The machine may compute the diff and the affected set; a person owns the claim | — | affected-row inventory, backup and rollback reviewed before execution |
| `MSA-040` | Executing migrations and maintaining the ordered ledger | `AUTOMATE` | ordered, idempotent, fail-closed on checksum; the one place discipline must never be manual | — | clean build ≡ upgraded database (`BM-INV-17`) |
| `MSA-041` | Deriving documentation navigation: indexes, catalogues, cross-references, coverage tables | `AUTOMATE` | deterministically derivable, and a hand-maintained index drifts silently (RCP §18) | — | regenerating the index changes nothing |
| `MSA-042` | Classifying a document's **authority** (canonical / supporting / historical / superseded) | `KEEP EXPLICIT` | authority is asserted, never inferred from recency, filename or detail. `CF-042` is eight documents that all looked current because nobody had to say otherwise | — | every governed document carries an assigned authority; a new one without it fails the build |
| `MSA-043` | Generating the grammar conformance corpus and contract-derived test fixtures | `AUTOMATE` | mechanical from the grammar, and the only defence against silent parser/schema drift | conformance corpus (`DR-005`) | the corpus fails when either side changes alone |
| `MSA-044` | Classifying each test (canonical behaviour / compatibility / migration / legacy / obsolete) | `ASSIST` | the machine can propose from what a test touches; what a test *protects* is a judgement (`CAD-10`, `GOV-R7`). Behavioural test authoring itself stays human | — | no material test is unclassified when legacy is removed |

**Distribution of targets:** `AUTOMATE` 26 · `ASSIST` 7 · `KEEP EXPLICIT` 8 · `HUMAN
DECISION` 3. Ten rows carry **†** — they are gated on an authority or component that does
not exist yet, which is the same bottleneck `RC-003` describes, seen from the automation
side.

---

## 6. Unsafe automation — rules that override the register

Even where §2 and §3 permit `AUTOMATE`, these are prohibited. Each is the automated form of
a defect the recovery already found.

| Rule | Prohibited | Because |
|---|---|---|
| `MSA-R1` | Automatic **approval** of a contract, package, classifier, schema or publication | approval is accountability; automating it removes the only human in the chain (`CF-033/034` is this defect without the word "automation") |
| `MSA-R2` | Inferring a schema, type, grain, unit or relation from observed data | `REQ-009`: no runtime semantic guessing. Inference is right often enough to be trusted and wrong often enough to corrupt |
| `MSA-R3` | Automatic repair or backfill of canonical data | a repair is a semantic claim; it belongs in a governed reprocess with evidence (`MSA-026`) |
| `MSA-R4` | Silent fallback or default when a capability is unsupported | `BM-INV-18`: unsupported means explicit rejection, never quiet omission |
| `MSA-R5` | Treating a generated artifact as authority because it was generated correctly | `CAD-06`; correctness of derivation says nothing about direction of authority |
| `MSA-R6` | Automation that cannot be verified except by re-running itself | a generator that is its own oracle proves only self-consistency |
| `MSA-R7` | Automation without a declared resource bound | an unbounded automated path is an availability incident waiting for a large input |
| `MSA-R8` | Derivation that produces only a **runtime interpretation** — semantics that exist while executing and nowhere else | this is how `DECLARE ONCE → DERIVE` degenerates into `DECLARE GENERIC METADATA → HIDE ALL SEMANTICS IN RUNTIME INTERPRETATION`. A derivation must land in an inspectable, named artifact — a schema, a constraint, an immutable plan — that can be read, diffed and tested without running the engine. If the only way to learn what a field means is to execute the interpreter, the meaning has no owner |

---

## 7. How this model is evaluated

`STD-BENCH-001` §6.12 and `BM-REJ-10/11` are the evaluation hooks. A candidate architecture
is judged against this model by two questions, both evidence-bound:

1. **For every `AUTOMATE` row:** do all ten preconditions of §3 hold, and is there a check
   that is independent of the generator?
2. **For every `KEEP EXPLICIT` / `HUMAN DECISION` row:** is the declaration or decision
   actually reachable by the person who owns it — or does it exist only as a migration, a
   fixture or a database edit?

The second question is the one this repository failed: five of the eight `KEEP EXPLICIT` rows
are marked **†** precisely because their human path was never built, and migrations became
the substitute (`RC-002`, `RC-003`). **Automation maturity is not measured by how much runs
unattended, but by whether the things that must not be automated have a real home.**
