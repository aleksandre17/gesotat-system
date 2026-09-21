---
id: STD-BENCH-001
type: REFERENCE
title: Architecture quality benchmark
status: ACTIVE
authority: CANONICAL
scope: evaluation criteria for candidate platform architectures
owner: PHASE-002
created: 2026-09-21
updated: 2026-09-21
related: STD-AUTO-001, REC-CONSOLIDATION, DOC-CAD, REC-PASS3
---

# ARCHITECTURE QUALITY BENCHMARK

**The standard against which the future Canonical Architecture, the existing artifacts and
every competing mechanism are judged.** It answers *what a modern long-lived
statistical/data platform must satisfy*. It deliberately does **not** answer *what we should
build* — that is `PHASE-004`, and shaping the benchmark around a preferred design would make
the later evaluation circular.

Produced by `PHASE-002`, guarded by `GATE-STANDARDS` (`docs/project/MANIFEST.md`).

---

## 0. What this artifact owns — and what it must never own

| Owns | ID range |
|---|---|
| Decision hierarchy for resolving criterion conflicts | `BM-DH-01…15` |
| Quality-attribute model | `BM-QA-01…14` |
| Hard acceptance invariants a candidate architecture must satisfy | `BM-INV-01…25` |
| Cross-layer evaluation criteria | `BM-EC-001…089` |
| Rejection criteria | `BM-REJ-01…14` |
| Unresolved **standards** questions | `BM-Q-01…08` |
| The verdict method — how a reviewer produces a comparable result | §2 |

**Does not own, and never restates:**

| Responsibility | Canonical owner |
|---|---|
| Findings, root causes, requirement ledger `REQ-001…041`, invariants `INV-001…014`, automation baseline | `REC-CONSOLIDATION` |
| Authority registry and anti-parallelism rules `CAD-01…18` | `DOC-CAD` |
| Protection requirements, design handoff, true design questions | `REC-PASS3` §1, §10 |
| Layer coverage `L0…L47`, `GOV-R1…R10` | `REC-COVERAGE` |
| Normative requirement catalogue `GOV/SCH/ARC/ING/DAT/API/UI/SEC/EVO/OBS/TST/REL-###` | `docs/reference/engineering/REQUIREMENTS.md` |
| Prohibited implementation patterns | `docs/reference/engineering/ANTI-PATTERNS.md` |
| External standards profile and adoption limits | `docs/reference/engineering/STANDARDS.md` |
| Automation classification | `STD-AUTO-001` |
| Lifecycle, gates, deferred controls | `CTRL-MANIFEST` |
| Project state | `CTRL-CURRENT` |

**Derivation rule (CAD-05/CAD-06 applied to documents).** Every row below that carries a
`Source` column is a **derived acceptance projection**: the source owns the semantics, this
file owns only the *question* and the *falsifier*. If a row and its source disagree, **the
source wins and this row is the defect.** No row here creates a new semantic fact, and no
identifier here is reused from another namespace. **Identifiers are append-only**: a row added
later takes the next free number in its series and keeps the section it belongs to, so a
citation never silently points at a different criterion.

---

## 1. What a benchmark is for, and the failure it prevents

The recovery proved that this repository can produce coherent, individually defensible
mechanisms that are collectively wrong: seven ingestion paths, four chart models, two
grammars, two definitions of "live" (`REC-PASS3` §10). Each was built by someone applying
good judgement locally. What was missing was a **shared, written, falsifiable definition of
better** — so every generation re-derived one, and each derived a different one.

A benchmark is that definition, fixed *before* the target is designed. Its test is:

> Can an independent reviewer evaluate two competing architectures against this document
> **without inventing a missing quality criterion**?

If the reviewer must invent a criterion, the benchmark failed, not the reviewer.

**Best is not most abstract, and not most patterns.** Best is *the strongest justified
semantics and invariants at the lowest justified complexity, with the safest long-term
evolution* — which is why `BM-QA-12` (conceptual economy) is a first-class attribute and
`BM-REJ-01…03` can reject an architecture for excess, not only for deficiency.

---

## 2. Verdict method — how this document is used

A reviewer evaluating a candidate (`PHASE-003` artifact comparison, `PHASE-004` design,
`PHASE-007` adversarial review, `PHASE-012` final audit) produces three results, in this
order. **The order is not cosmetic: a candidate that fails an invariant is not scored on
quality attributes, because a good score would then argue against an invariant.**

### 2.1 Gate 1 — invariants (§5). Binary.

Each `BM-INV` is `HOLDS` / `VIOLATED` / `NOT_DEMONSTRATED`. Any `VIOLATED` ⇒ the candidate is
**REJECTED**. `NOT_DEMONSTRATED` ⇒ **NOT READY**, never a pass. The only legal override is an
approved, time-bounded ADR naming scope, owner, risk, compensating control, expiry and
removal task (`/AGENTS.md` §10) — **an agent may not approve its own override.**

**Scoring is fail-closed and non-additive: no number of passes offsets one violation.**

### 2.2 Gate 2 — criteria (§6). Evidence-bound.

Each `BM-EC` is `PASS` / `PARTIAL` / `FAIL` / `N/A`, and each non-`N/A` verdict names the
evidence that produced it. `N/A` requires a substantive reason, never silence
(`REQUIREMENTS.md` preamble). Evidence obeys `/AGENTS.md` §8: bound to an exact revision,
command, environment and exit code; historical, partial, mocked or documentation-only
evidence is labelled as such and **cannot raise a verdict above `PARTIAL`** — the rule
`REC-COVERAGE` §2 already applies to inspection, applied here to evaluation.

### 2.3 Gate 3 — quality attributes (§4). Rated, then compared.

Each `BM-QA` is rated against its threshold class. Two candidates are compared attribute by
attribute; where they trade against each other, **§3 decides, in writing**. A comparison that
does not name the hierarchy rank it used is not a comparison, it is a preference.

### 2.4 Required output shape

```text
CANDIDATE:            <name, exact revision / artifact digest>
INVARIANTS:           <n> HOLDS · <n> VIOLATED · <n> NOT_DEMONSTRATED
CRITERIA:             <n> PASS · <n> PARTIAL · <n> FAIL · <n> N/A
QUALITY ATTRIBUTES:   <per-attribute rating vs threshold>
REJECTION CRITERIA:   <BM-REJ triggered, with evidence>
VERDICT:              ACCEPTED | ACCEPTED-WITH-BOUNDED-EXCEPTIONS | NOT READY | REJECTED
UNRESOLVED:           <BM-Q rows that blocked a verdict>
```

`ACCEPTED` is never a compliment and never implies runtime or production readiness
(`/AGENTS.md` §8; `MANIFEST` standing obligations).

---

## 3. Decision hierarchy — `BM-DH`

**Source: the `PHASE-002` commissioning instruction, 2026-09-21.** Recorded here because a
benchmark without a tie-break rule ranks by taste while appearing to rank by evidence. It is
an instruction-level authority (`docs/reference/engineering/README.md`, authority paragraph);
its architectural consequence is recorded by this section.

| Rank | Criterion | Outranks everything below it because |
|---|---|---|
| `BM-DH-01` | Proven business/domain requirements | a platform that serves no proven requirement is complexity without purpose |
| `BM-DH-02` | Recovered architectural intent | intent is the only defence against re-solving a solved problem differently |
| `BM-DH-03` | Explicit invariants | an invariant is what stays true while everything else changes |
| `BM-DH-04` | Required external/public contracts and compatibility | a broken external contract breaks somebody else's system, not ours |
| `BM-DH-05` | Semantic/data correctness | wrong numbers served quickly are worse than right numbers served slowly |
| `BM-DH-06` | Security and tenant isolation | a leak is unrecoverable; performance regressions are recoverable |
| `BM-DH-07` | Transactional correctness | partial state is silent corruption |
| `BM-DH-08` | Determinism and reproducibility | a result that cannot be reproduced cannot be audited or defended |
| `BM-DH-09` | Schema/data-model integrity | constraints are the last defence when every layer above has a bug |
| `BM-DH-10` | Performance and scalability | real limits, but they are negotiable and measurable |
| `BM-DH-11` | Failure/recovery semantics | recoverability is a property of design, not of operators |
| `BM-DH-12` | Evolvability and extensibility | a 10-year platform changes more than it runs unchanged |
| `BM-DH-13` | Conceptual simplicity | every concept is paid for by every future reader |
| `BM-DH-14` | Maintainability and operability | the cost that recurs forever |
| `BM-DH-15` | Existing implementation convenience | **last, always** — CAD-17: legacy converges toward the canonical design, never the reverse |

**Worked example (recovery evidence).** `CF-023` publishes every `REVIEW_REQUIRED` snapshot of
a product while gating one. The convenient fix — loosening `assertApprovedContract` — was
examined and rejected during `AMS-001` falsification: it trades `BM-DH-05`/`BM-DH-06` for
`BM-DH-15`. The hierarchy makes that rejection mechanical rather than a matter of opinion.

---

## 4. Quality-attribute model — `BM-QA`

**Frame:** ISO/IEC 25010 product quality characteristics, **adapted, not adopted whole**
(`STANDARDS.md`). Two adaptations are deliberate and must not be silently reversed:

1. *Functional suitability* is replaced by **semantic correctness** (`BM-QA-01`). For a
   statistical platform, "the function returns a value" is not the property that matters;
   "the value means exactly what it declares" is.
2. **Authority singularity** (`BM-QA-02`) is added. No external quality model contains it.
   It is this repository's dominant observed failure mode (`RC-001`, `RC-003`, `CF-033`,
   `CF-038`, `CF-041`), and an architecture can score well on every 25010 characteristic
   while being unmaintainable because truth has two homes.

Threshold classes: **`REQUIRED`** — a hard gate, failure rejects the candidate ·
**`TARGET`** — measured against a declared budget · **`BOUNDED`** — the candidate must
declare an explicit bound; the *value* is a design choice, the *existence of the bound* is
not.

| ID | Attribute | Meaning in this platform | Measure | Threshold | Evidence type | Standard, or explicit non-adoption | `BM-DH` |
|---|---|---|---|---|---|---|---|
| `BM-QA-01` | Semantic correctness | grain, identity, time, unit, dimension, aggregation and null/status semantics are unambiguous and reproducible | round-trip source→canonical→serve→export comparison with exact values | `REQUIRED` | integration + reconciliation test | SDMX **bounded** (cube-shaped families only), ISO 8601, BCP 47. **No external standard governs cross-family semantics** — local criterion, measured by round trip | 01, 05 |
| `BM-QA-02` | Authority singularity | one responsibility → one canonical authority → derived representations | fitness function over the `CAD-02` registry: every material mechanism classifies, no responsibility has two producers | `REQUIRED` | architecture fitness test | **No external standard exists.** Deliberate local invariant (`CAD-01`, `INV-002`); no quality model contains it | 03 |
| `BM-QA-03` | Data integrity | declared type, nullability, uniqueness, referential integrity and cardinality are enforced *below* the contract, in storage | constraint/negative tests per family; attempt to insert violating data must fail | `REQUIRED` | DB constraint + migration test | **No standard adopted as such.** Constraint enforcement is the provider's SQL implementation; no conformance claimed. Local criterion | 09 |
| `BM-QA-04` | Determinism and reproducibility | identical approved inputs produce byte-identical derived artifacts and digests, independent of ordering, host and clock | regenerate twice, compare digests; clean build ≡ upgraded database | `REQUIRED` | reproducibility test, replay proof | FIPS 180-4 (SHA-256) + RFC 8785 JCS + Unicode NFC — canonical form then digest | 08 |
| `BM-QA-05` | Traceability and provenance | every served value traces backward to source artifact, contract revision, generator and approval; every artifact traces forward | backward-chain walk terminates at a governed declaration, not at "missing" | `REQUIRED` | lineage test over a real value | W3C PROV-O `ADAPT` (concepts, not RDF); W3C DCAT 3 `ADAPT` (dataset / distribution / data-service identity) | 08 |
| `BM-QA-06` | Transactional correctness and failure semantics | every state transition is atomic, idempotent under retry, and has a declared failure and rollback outcome | crash/duplicate/conflict/concurrency/rollback matrix per stateful path | `REQUIRED` | recovery + property test | **No external standard adopted.** Transactional outbox is `CONDITIONAL` and pattern-level only. Local criterion | 07, 11 |
| `BM-QA-07` | Security and tenant isolation | deny-by-default authorization and tenant scope enforced at the data-access boundary, with an exemption register | anonymous / wrong role / wrong tenant / revoked / allowed executed against the real path | `REQUIRED` | behavioural HTTP + use-case test | OWASP ASVS 5.0.0 `CONDITIONAL` — control subset unchosen (`BM-Q-05`); NIST SSDF 1.1 `ADAPT` for the verification practice | 06 |
| `BM-QA-08` | Compatibility and evolvability | producers, stored revisions and consumers evolve without breaking; unsupported capability is rejected, never silently dropped | upgrade from previous supported state; incompatible revision rejected with an actionable error | `REQUIRED` | compatibility + negative test | Semantic Versioning 2.0.0; JSON Schema 2020-12; OpenAPI 3.1.1 — all `ADOPT` | 04, 12 |
| `BM-QA-09` | Performance and bounded resource use | every query, import, export and generation has a declared bound on rows, memory, time and cost | no unbounded materialization; budgets declared and enforced at admission | `TARGET` + `BOUNDED` | performance test + query budget | **No external standard sets objectives.** Values are an owner decision (`BM-Q-03`); the *existence* of a declared bound is the local criterion | 10 |
| `BM-QA-10` | Operability and diagnosability | an operator can determine what happened, why, and how to recover, from correlated evidence | failure-path log/metric/trace evidence; every failure names its next action | `TARGET` | observability test | W3C Trace Context / OpenTelemetry `CONDITIONAL` (due with `W-05`); RFC 9457 `ADOPT` for actionable error shape | 14 |
| `BM-QA-11` | Authoring usability | a domain author can fill a dataset correctly without reading the architecture | mandatory fields, code-list pickers, preflight validation, derived organisation; error messages name the fix | `TARGET` | authoring walkthrough + validation test | **Explicit non-adoption:** ISO 9241 usability is not adopted — no evaluation decision here would change if it were. Local criterion | 01, 14 |
| `BM-QA-12` | Conceptual economy | the architecture becomes *simpler* as capability becomes *stronger* (`REQ-041`) | **comparative, not absolute:** the concept inventory a new engineer must hold to onboard a site, family or provider, and the number of coexisting mechanisms per responsibility. A candidate carrying more concepts than another must name the capability the other cannot deliver; unjustified excess is `BM-REJ-01` | `BOUNDED` (comparative) | concept inventory + `CAD-02` classification | **No external standard exists.** Local criterion from `REQ-041` | 13 |
| `BM-QA-13` | Testability and protectability | every invariant has a test that fails when the invariant is broken | negative test per invariant; protection exists *before* the migration it protects | `REQUIRED` | test-classification ledger (`CAD-10`) | NIST SSDF 1.1 `ADAPT` (verification practices); local test classification `CAD-10` / `GOV-R7` | 03, 12 |
| `BM-QA-14` | Portability and provider neutrality | provider, storage and transport are replaceable behind ports; no provider or site literal in generic core | same contract suite passes on a substituted adapter; static site-literal lint | `REQUIRED` | adapter substitution test | Cockburn ports and adapters `ADOPT` (a pattern, not a standard); ISO/IEC 25010 *portability* characteristic `ADAPT` | 12, 14 |

**Coverage rule for `GATE-STANDARDS`:** every attribute above names either an external
standard (via `STANDARDS.md`) or an explicit local criterion with a measure. **No attribute
is left to judgement without a measure.** §10 proves the reverse direction — that every
recovered requirement lands on at least one attribute or criterion.

**Two attributes deliberately carry no threshold value, and that is not an omission.**
`BM-QA-09` (performance) and `BM-QA-12` (conceptual economy) are the only attributes whose
measure is a number that this phase cannot legitimately fix: the performance objectives are
an owner decision (`BM-Q-03`) and conceptual economy has meaning only between two candidates.
Declaring an invented threshold for either would be a benchmark pretending to knowledge it
does not have. They are therefore `BOUNDED`/comparative: the *existence* of a declared bound
and the *justification of excess* are gate conditions; the numbers are not.

**Standards that are adopted platform-wide but carry no benchmark hook.** `STANDARDS.md`
governs the platform; this benchmark governs architecture evaluation, and the two sets are
not identical. Three adopted sources have **no row above, deliberately**:

| Source | Why it has no evaluation hook here |
|---|---|
| SLSA 1.1 | build and release provenance. Excellent standard, different responsibility: `REL-001` owns release evidence, and an architecture is not judged by its build pipeline |
| NIST SSDF 1.1 | a development *process* framework. Its verification practice is hooked at `BM-QA-07`/`BM-QA-13`; the rest governs how an organisation works, not what an architecture is |
| BCP 14 / RFC 8174 | obligation vocabulary. It makes every other row readable; it is not itself an evaluable property |

**This is the benchmark saying what it must be able to say:** *this standard is excellent,
and it does not belong to this responsibility.* A standard listed with a hook it does not
really have would be cargo cult in its most convincing form — an adopted name with nothing
downstream that would change if it were withdrawn.

---

## 5. Hard invariants — `BM-INV`

**Acceptance conditions.** Each row is a condition the Canonical Architecture must satisfy,
the observation that would falsify it, and the class of mechanism that must enforce it.
**Enforcement class matters as much as the condition**: an invariant defended only by review
is defended by memory.

| ID | The candidate architecture must guarantee | Falsifier — one observation that disproves it | Enforcement class | Source |
|---|---|---|---|---|
| `BM-INV-01` | Every material responsibility has exactly one canonical authority, registered and classifiable | two mechanisms independently write the same semantic fact | fitness function over `CAD-02` | `CAD-01`, `INV-002` |
| `BM-INV-02` | Authority flows M3→M2→M1→M0 only; no lower artifact redefines an upper one | a DTO, generated file, migration, test or database row is the source of a semantic definition | schema/grammar test + static analysis | `SCH-001`, `INV-001`, `ANTI-PATTERNS` |
| `BM-INV-03` | Every canonical registry has exactly one governed producer | a registry exists whose only writer is a migration, a fixture or a transport artifact | producer sweep by write verb, in CI | `RC-003`, `CF-024a` |
| `BM-INV-04` | Exactly one mechanism decides what is live/published | a second field or table is consulted *instead of* the release authority by any read path | DB constraint/trigger + reconciliation | `RC-001`, `INV-008` |
| `BM-INV-05` | Declared type, nullability, uniqueness, referential integrity and cardinality hold in storage, for **every** family | a declared field is stored in an untyped document/JSON column, or a declared cardinality can be violated | DB constraints + migration test | `CF-039`, `CF-040`, `REQ-006/007/008` |
| `BM-INV-06` | Every identity resolution is scoped and revision-pinned; "latest wins" never resolves identity | an `ORDER BY … DESC`, suffix match or unscoped code lookup decides which definition applies | static analysis + negative test | `CF-018`, `QF-012`, `INV-004`, `INV-014` |
| `BM-INV-07` | Every derived artifact identifies source authority, source revision, generator identity/version and a correspondence digest | an artifact cannot answer "which declaration and which generator produced me" | generated-artifact verification | `CAD-06`, `DR-007`, `REQ-026/028` |
| `BM-INV-08` | A transport or provider artifact may *reference* canonical definitions and never *define or approve* them | a package, file or external payload creates or approves a canonical registry row | producer-scoped write constraint | `DR-007`, `REQ-022`, `CF-033/034` |
| `BM-INV-09` | Exact decimal throughout the canonical path; declaration ≤ provider capability ≤ canonical storage; aggregation cannot silently overflow or truncate | a float appears in the canonical path, or a `SUM` overflows/truncates without failing | type + capability check, property test on `SUM` | `DR-006`, `INV-005`, `CF-027` |
| `BM-INV-10` | One observation per dimension tuple and measure; no dimension is privileged and measure is not a key component | two rows share a full dimension key and measure, or one dimension is hardcoded in generic code | unique constraint + property test | `INV-006`, `REQ-017` |
| `BM-INV-11` | A null value carries an explicit status; zero is a real value and never a substitute for missing | a missing value is stored as `0`, or a null exists with no status | release gate + constraint | `INV-007` |
| `BM-INV-12` | The published set ≡ the approved set ≡ the gate-evidenced set; rollback restores exactly that set | publishing one snapshot changes the liveness of another | integration test (publish one, assert one) + runtime integrity check | `INV-008`, `CF-023` |
| `BM-INV-13` | Tenant scope is enforced at the data-access boundary, deny-by-default, and every exemption is declared in a register | a cross-tenant read succeeds, or an exemption exists that no register lists | behavioural test + exemption register | `INV-009`, `QF-003` |
| `BM-INV-14` | Every write path is idempotent under retry with a semantically scoped key; a conflicting payload under the same key fails closed | a retry duplicates an effect, or a different payload silently overwrites under the same key | duplicate/conflict test | `DAT-002`, `REQ-033` |
| `BM-INV-15` | One canonical dataset semantics serves table, chart, API, export and dashboard; presentation is a projection declaration, never a parallel semantic model | two surfaces can return different numbers for the same declared dataset | contract test across surfaces | `REQ-020`, `CF-030` |
| `BM-INV-16` | No caller-supplied SQL, identifiers or expressions reach execution; only closed grammar compiled to bounded plans | a caller-supplied name or expression is concatenated into a query | static analysis + injection test | `API-001`, `SEC-002`, `CF-026` |
| `BM-INV-17` | Migrations are ordered, immutable once applied, replayable; a clean build and an upgraded database converge | a fresh chain produces a database that differs from the upgraded one in an unexplained way | fresh-replay comparison (`REC-PASS3` §2) | `INV-010`, `CF-020`, `DEF-06` |
| `BM-INV-18` | An unsupported declared capability is an explicit, actionable rejection at authoring time — never silent omission | an unknown field, component or capability is accepted and then ignored | version-aware grammar rejection test | `SCH-001`, `UI-001` |
| `BM-INV-19` | Every compatibility bridge and temporary mechanism names owner, callers, exit condition, removal task and maximum lifetime | a bridge exists with no exit condition | register + expiry check | `CAD-11`, `REC-PASS3` §5 |
| `BM-INV-20` | Every read of canonical data passes the serving/publication authority; no surface bypasses it | a read path returns canonical data without consulting the serving policy | architecture fitness test | `CF-021`, `CF-031` |
| `BM-INV-21` | The published grammar and its executable implementation accept exactly the same documents | a document validates against one and is rejected by the other | bidirectional conformance corpus in CI | `DR-005`, `CF-009` |
| `BM-INV-22` | Content identity of a stored artifact is its checksum; a locator is never identity, and lifecycle alignment between the registry and the store is enforced, not conventional | the same bytes register as two objects, or a store object outlives/precedes its registry row unchecked | integrity audit + reconciliation | `CAD-02` object-storage row, `REC-CONSOLIDATION` §10 |
| `BM-INV-23` | Raw source artifacts are retained immutably, and canonical state is re-derivable from them under a corrected revision without consulting the previous canonical state | raw is discarded, mutated, or a correction can only be applied by editing canonical rows in place | immutable store + replay proof | `REQ-028`, `REC-PASS3` §10 (governed reprocessing), `INV-013` |
| `BM-INV-24` | Every canonical dataset declares its row grain, and that grain is enforced by a uniqueness constraint over the **business key** — a surrogate key alone never satisfies it | two rows exist that the declared grain says are the same fact, and no constraint prevented it | unique constraint + duplicate-insert negative test | `REQ-006`, `INV-006`, `BM-EC-002` |
| `BM-INV-25` | A value withheld for confidentiality is **not recoverable** from anything else the platform publishes | a suppressed cell is computable by subtraction from published aggregates, margins or an overlapping dataset | disclosure-recovery test over the **published** set, not the stored set | `Q12`, `CAP-M12`, `D1` |

**`BM-INV-23` and `BM-INV-24` were added by the PHASE-002 acceptance review**, not to raise a
count. Each closes a hole that a plausible candidate passed: an architecture that discards raw
after materialization satisfied every other invariant while making corrected reprocessing
impossible, and a *typed* EAV model (typed value columns, one row per declared field) satisfied
`BM-INV-05` field by field while making the dataset's real grain inexpressible. See §8, F-7.

**Protection ordering.** `REC-PASS3` §1 already names the highest-leverage protections: the
`CAD-02` fitness function first (it defends `BM-INV-01…03` and much of the rest at once),
then membership/status reconciliation (`BM-INV-04`, `BM-INV-12`), then the grammar
conformance corpus (`BM-INV-21`). This benchmark adds no protection requirement of its own
and does not reopen that ordering.

---

## 6. Evaluation criteria — `BM-EC`

Every criterion is a question with a checkable answer and a named evidence method. **A
criterion that could not name an evidence method was not written down** — an unevidenced
criterion is an invitation to score by impression.

`Layer` uses the eight ownership layers of `docs/reference/engineering/ARCHITECTURE.md`
(Control · Ingestion · Data · Archive · Serving · Security · Observability · Delivery).
`L##` refers to the coverage categories of `REC-COVERAGE`, so that a criterion can be traced
to what recovery actually inspected. Those categories are audit coverage, **not** a claim
that the target contains 48 layers.

### 6.1 Semantic and statistical modelling

| ID | Criterion | Evidence | Layer | `L##` |
|---|---|---|---|---|
| `BM-EC-001` | Is every concept (dimension, measure, metric, unit, classifier, period, geography) defined exactly once, with stable identity independent of its physical column? | semantic catalogue / table passport per family | Control | L16, L11 |
| `BM-EC-002` | Is row grain declared per dataset and enforced by a unique constraint, not by convention? | constraint + duplicate-insert negative test | Data | L28 |
| `BM-EC-003` | Are dimensions, measures and attributes distinguishable in the model, with measure never part of the dimension key? | schema inspection + property test | Control, Data | L28 |
| `BM-EC-004` | Is observation status a first-class, constrained vocabulary rather than free text with a default? | `CHECK`/FK constraint; absence is itself a finding | Data | L28, L31 |
| `BM-EC-005` | Are units, multipliers and decimals declared per measure and carried to every surface including export? | round-trip to export with exact values | Control, Serving | L28, L42 |
| `BM-EC-006` | Can a declared constant vary by dimension combination (e.g. unit per indicator) without per-observation repetition? | authoring case from `CF-015` domain evidence | Control | L4 |
| `BM-EC-007` | Are classifiers/codelists versioned, with hierarchy and validity, and is a code resolved against the exact version it was authored against? | negative test: resolve against a superseded version | Control | L12, L13 |
| `BM-EC-008` | Are totals/aggregates distinguishable from base observations, so an aggregate cannot be summed again by accident? | aggregation property test | Data, Serving | L39 |
| `BM-EC-009` | Are cube-shaped and non-cube (entity/state/event/product) data both expressible **without forcing either into the other's model**? | two materially different fixtures, one of each shape | Control, Data | L29, L30 |
| `BM-EC-010` | Is SDMX applied only where the data is genuinely cube-shaped, with the boundary written down? | explicit applicability statement + export conformance test | Control | L42 |
| `BM-EC-011` | Are many-to-many relations first-class, with declared cardinality and validity, not emergent from link rows? | cardinality-violating insert must fail | Data | L30 |
| `BM-EC-012` | Are raw, canonical and derived representations distinct, each with its own identity, and is derivation reproducible from raw? | replay from the immutable artifact | Ingestion, Data | L25, L28 |
| `BM-EC-013` | Is semantic identity stable across renames, reorganisation and physical migration? | identity survives a simulated rename/migration | Control | L3, L4 |
| `BM-EC-014` | Is the structural metadata (DSD-equivalent) machine-validatable, not merely documented? | validator run against a malformed declaration | Control | L2 |

### 6.2 Data architecture

| ID | Criterion | Evidence | Layer | `L##` |
|---|---|---|---|---|
| `BM-EC-015` | Does every table have a one-page passport: purpose, grain, keys, owner, lineage, constraints, retention, privacy? | passport per table, mechanically checkable fields | Data | L28–L31 |
| `BM-EC-016` | Are keys explicit (surrogate *and* natural/business), with the natural key constrained? | unique constraint + negative test | Data | L28 |
| `BM-EC-017` | Is referential integrity enforced by the database, not only by the application? | FK inspection; orphan-insert test | Data | L29, L30 |
| `BM-EC-018` | Is every denormalised structure **mechanically derived from the canonical form, never independently written**, and reconcilable against it — rather than a second place the truth can be edited? | for each denormalised structure: its derivation, a reconciliation that would detect divergence, and proof that no write path targets it directly. A declaration alone is not evidence — anything can be declared | Data | L28, L43 |
| `BM-EC-019` | Does a new dataset of a supported family require **zero** core-code change and **zero** new physical schema authored by hand? | two unrelated fixtures onboarded by declaration only | Control | L3, L9 |
| `BM-EC-020` | Is per-dataset schema proliferation bounded by a declared physical pattern rather than by ad-hoc DDL? | physical pattern declaration per family | Data | L9 |
| `BM-EC-021` | Is universal EAV/key-value/blob storage absent from canonical state, in every family — **including its typed disguise**, where each declared field is a row with typed value columns rather than a column? | **the constraint-expressibility test:** pick one declared field and write a database constraint that governs it alone (`CHECK`, `UNIQUE`, `FOREIGN KEY`, `NOT NULL`). If the constraint cannot be expressed without a filtered index over an attribute-name row, the model is EAV whatever its column types are. Column-type inspection alone passes typed EAV and is not sufficient | Data | L29 |
| `BM-EC-022` | Is temporal semantics explicit: business validity, system time, snapshot time — and are they not conflated? | temporal query test across the three | Data | L17, L31 |
| `BM-EC-023` | Is schema evolution expressible without rewriting history, and are applied migrations immutable? | expand→migrate→contract worked example | Delivery | L47 |
| `BM-EC-024` | Are data-quality rules declared as contract metadata and executed, rather than living in code comments? | quality-rule violation blocks publication | Control, Data | L32 |
| `BM-EC-025` | Is information preserved end to end — no silent truncation, rounding, collapse or discard on any axis? | information-loss matrix per axis | Ingestion, Data | L27 |
| `BM-EC-026` | Is canonical representation independent of provider physical layout, with provider specifics confined to adapters? | adapter substitution with identical canonical result | Data, Delivery | L9, L10 |
| `BM-EC-027` | Are indexes and access patterns declared with the contract rather than discovered in production? | index declaration + query-plan evidence | Data | L38 |
| `BM-EC-028` | Are transactional boundaries explicit per operation, and does no operation span an unbounded number of them? | transaction map per write path | Data | L33 |
| `BM-EC-029` | Is bulk processing streaming or chunked, with a declared memory ceiling? | large-import test at the declared ceiling | Ingestion | L25 |

### 6.3 Contracts and metadata

| ID | Criterion | Evidence | Layer | `L##` |
|---|---|---|---|---|
| `BM-EC-030` | Is every contract machine-validatable against a versioned grammar before it can be approved? | malformed/unsupported declaration rejected | Control | L2 |
| `BM-EC-031` | Is contract identity immutable and are revisions append-only, with an approval that binds the resolved closure? | digest-bound approval; drift detected not reinterpreted | Control | L6 |
| `BM-EC-032` | Is compatibility between grammar versions declared, with an incompatible revision rejected rather than coerced? | cross-version negative test | Control | L2 |
| `BM-EC-033` | Does the published shape authority and its executable parser demonstrably accept the same documents? | bidirectional conformance corpus | Control | L2 |
| `BM-EC-034` | Is generated metadata marked as generated and regenerable, never hand-edited into authority? | generator re-run reproduces the artifact byte-for-byte | Control, Delivery | L8, L19 |
| `BM-EC-035` | Is semantic metadata separated from physical metadata, with the physical derived from the semantic? | physical plan derived from semantic plan only | Control | L7, L8 |
| `BM-EC-036` | Are provider capabilities declared in exactly one registry and consulted by the compiler? | capability-exceeding declaration rejected | Control | L10 |
| `BM-EC-037` | Does a contract carry its own provenance: author, approver, time, inputs, digest? | provenance fields present and bound | Control | L6 |
| `BM-EC-038` | Is there a governed path to withdraw or supersede an approved contract known to be wrong? | withdrawal executed and its effect observed | Control | L6 |
| `BM-EC-039` | Can metadata never silently become a second semantic authority — is every registry write producer-scoped? | write-path inventory per registry | Control | L14, L15 |

### 6.4 Lineage and provenance

| ID | Criterion | Evidence | Layer | `L##` |
|---|---|---|---|---|
| `BM-EC-040` | From any served value, can origin, transformations, contract revision, schema version, generator and package be recovered? | backward-chain walk on a real value | Serving, Archive | L42, L35 |
| `BM-EC-041` | Is the entity/activity/agent distinction preserved conceptually (PROV-O adapted), without mandating RDF storage? | lineage model review against the adopted subset | Archive | L35 |
| `BM-EC-042` | Is every derivation reproducible from retained inputs, and proven by re-execution rather than asserted? | replay produces identical digests | Archive, Delivery | L34 |
| `BM-EC-043` | Does every canonical row carry its source record and artifact identity? | FK to source record; orphan check | Data | L28 |
| `BM-EC-044` | Is audit evidence distinguishable from command intent, so an intent can never be read as proof of effect? | outbox/event category separation test | Observability | L46 |
| `BM-EC-045` | Are digests computed over a canonical form with a declared canonicalisation and hash algorithm? | canonicalisation test on reordered input | Control | L8 |

### 6.5 Package and interchange

| ID | Criterion | Evidence | Layer | `L##` |
|---|---|---|---|---|
| `BM-EC-046` | Is a package self-describing — able to state what it is, which declaration it realises and which generator built it? | package manifest inspection | Delivery | L20 |
| `BM-EC-047` | Is package↔contract correspondence proven by deterministic semantic equivalence before any canonical influence? | digest comparison blocks a mismatched package | Ingestion | L24 |
| `BM-EC-048` | Is package generation deterministic and regenerable from the approved declaration alone? | regenerate twice, compare | Delivery | L19 |
| `BM-EC-049` | Is package validation performed against the contract, not only against the package itself? | package-internal-only validation is a `FAIL` | Ingestion | L23 |
| `BM-EC-050` | Are package lifecycle states kept strictly separate from canonical lifecycle states? | no mapping function from package state to canonical state | Control | L14 |
| `BM-EC-051` | Is the provider (e.g. Access) one realisation among several, with no provider concept in canonical semantics? | second-provider substitution or a declared boundary | Delivery | L9, L19 |
| `BM-EC-052` | Are portability limits of the provider declared as capabilities rather than discovered at runtime? | capability registry drives the plan | Control | L10 |
| `BM-EC-053` | Is generator/validator symmetry present where justified, and is the asymmetry justified where absent? | corpus passing both directions | Control, Delivery | L2, L23 |
| `BM-EC-085` | Is any canonical semantic capability absent **solely because a provider cannot express it**? Bounding a *declaration* by the weakest provider required for that dataset's round trip is legitimate; lowering *canonical semantics or storage* to that provider is a lowest-common-denominator architecture | for each capability the canonical model lacks, the reason: a requirement says it is unnecessary, or a provider cannot hold it. The second answer is a `FAIL` unless the provider is required for that dataset **and** the canonical store still holds the superset (the `DR-006` shape: declaration ≤ provider ≤ canonical) | Control, Delivery | L9, L10 |

### 6.6 API and presentation

| ID | Criterion | Evidence | Layer | `L##` |
|---|---|---|---|---|
| `BM-EC-054` | Do table, chart, API, export and dashboard derive from one semantic model plus presentation declarations? | one dataset, five surfaces, identical numbers | Serving | L40, L41 |
| `BM-EC-055` | Is the presentation/projection model explicitly separate from the semantic model, and unable to invent semantics? | renderer cannot introduce a dimension, unit or permission | Serving | L40 |
| `BM-EC-056` | Are request/response/error schemas declared, versioned and enforced? | contract test + problem-details shape | Serving | L37 |
| `BM-EC-057` | Are all accepted parameters semantically effective — none accepted and discarded? | parameter inventory vs execution trace | Serving | L40 |
| `BM-EC-058` | Is aggregation computed over the full result set or an explicitly declared bound, never a silent page? | aggregate over > page-size data | Serving | L39 |
| `BM-EC-059` | Is pagination stable under concurrent writes (keyset or snapshot-bound)? | pagination-under-write test | Serving | L38 |
| `BM-EC-060` | Are declared pages/views/charts/maps/exports/locales preserved across migration, proven by a capability crosswalk? | before/after crosswalk, not screenshots | Serving | L40, L41 |
| `BM-EC-061` | Is capability discovery policy-aware, so a caller is not shown what it cannot read? | policy-varied discovery test | Security, Serving | L36, L44 |

### 6.7 Runtime and reliability

| ID | Criterion | Evidence | Layer | `L##` |
|---|---|---|---|---|
| `BM-EC-062` | Does every long-running operation resume from durable state rather than restart from the beginning? | interrupt and resume | Ingestion | L25 |
| `BM-EC-063` | Is every retry bounded, with a declared budget and a terminal failure state? | retry-exhaustion path observed | Ingestion, Delivery | L26 |
| `BM-EC-064` | Is partial failure impossible to mistake for success — is there no catch-and-continue on a canonical write path? | failure injection mid-write | Data | L33, L34 |
| `BM-EC-065` | Is concurrency controlled explicitly (lease, CAS, constraint), never by hoping? | two-writer race test | Data, Delivery | L31, L33 |
| `BM-EC-066` | Is a rerun of the same input deterministic in effect, including the "no change" case? | rerun produces no second logical effect | Ingestion | L26 |
| `BM-EC-067` | Does every failure path emit an actionable, correlated signal naming the next action? | failure-path log/metric/trace | Observability | L46 |

### 6.8 Security and tenancy

| ID | Criterion | Evidence | Layer | `L##` |
|---|---|---|---|---|
| `BM-EC-068` | Is authorization enforced at the use-case/data boundary, with the HTTP annotation as defence in depth only? | denied/allowed executed through the real path | Security | L44 |
| `BM-EC-069` | Is tenant scope carried through every layer, and is every exemption declared and registered? | cross-tenant negative test + exemption register | Security | L45 |
| `BM-EC-070` | Are external inputs (uploads, fetches, identifiers, paths) validated against a closed grammar, with SSRF/path/injection closed? | negative security tests | Security, Ingestion | L25, L44 |
| `BM-EC-071` | Are secrets absent from artifacts, logs, packages and configuration under version control? | secret scan over artifacts and packages | Security | L20 |

### 6.9 Performance and scale

| ID | Criterion | Evidence | Layer | `L##` |
|---|---|---|---|---|
| `BM-EC-072` | Does every query path have an admission-time budget (rows, joins, time, memory) and fail closed when exceeded? | budget-exceeding query rejected | Serving | L37 |
| `BM-EC-073` | Does cost grow as the architecture claims it grows — is there no full-materialisation or N+1 pattern on any path that scales with data volume? | **measured at two materially different volumes** (at least one order of magnitude apart), with the expected growth stated *before* measuring: query count must not grow with row count, and time/memory must track the declared model. A single 'large fixture' run proves that it works at that size and nothing about growth | Serving, Data | L37, L39 |

*(Performance thresholds themselves are `BM-Q-03`: the platform has no SLO authority yet.
The benchmark requires a declared budget; the numbers require an owner.)*

### 6.10 Evolution and migration

| ID | Criterion | Evidence |
|---|---|---|
| `BM-EC-074` | Is migration consumer-first: replacement proven, consumers migrated, then the old path disabled and removed? | `CAD-13` sequence evidence per mechanism |
| `BM-EC-075` | Is every legacy mechanism dispositioned, with unique knowledge preserved before removal? | disposition register + knowledge-preservation record |
| `BM-EC-076` | Is there an anti-corruption boundary at every legacy interface, so legacy shape cannot leak inward? | adapter inventory; no legacy type in the domain |
| `BM-EC-077` | Is every transition observable and reversible, or explicitly justified as irreversible? | rollback executed, or an ADR explaining why not |
| `BM-EC-078` | Is the target free of structure that exists only because legacy had it? | for each structure, a requirement-level justification |
| `BM-EC-086` | Where two mechanisms serve one responsibility family, is the relationship **declared** (specialization · derived representation · adapter · compatibility bridge · temporary migration) with a boundary that says which inputs each one owns — or is *specialization* being used as the word for parallel architecture? | the declared boundary, plus a case that provably falls to exactly one of them. Overlap with no rule, or a boundary nobody can state, is parallel architecture (`CAD-12`); four defensible chart mechanisms is what this looks like from inside |
| `BM-EC-087` | Is a declared suppression **protected** rather than merely flagged - can the architecture identify the complement set whose publication would reveal it? | attempt recovery of a suppressed value from the published set; the attempt must fail, and the complement set must be derivable from declared aggregation relationships rather than from an analyst's memory |
| `BM-EC-088` | Can the set of consumers affected by a contract or source change be **derived from declared state**? | name a dataset, change it, produce the affected consumers without consulting a person. No consumer registry means the answer is no, however good the lineage is |
| `BM-EC-089` | Does canonical meaning survive relocation - can a materialization move provider, table or storage key without editing a contract row? | move one materialization and diff the contract; any contract change is a `FAIL`, because it proves the contract held a location rather than an identity |

### 6.11 Testing and protection

| ID | Criterion | Evidence |
|---|---|---|
| `BM-EC-079` | Does every `BM-INV` row have at least one test that fails when the invariant is broken? | negative test per invariant |
| `BM-EC-080` | Is every test classified (canonical behaviour / compatibility / migration / legacy / obsolete)? | test classification ledger (`CAD-10`, `GOV-R7`) |
| `BM-EC-081` | Are architecture fitness functions executed in CI alongside behavioural tests? | CI configuration + run evidence |
| `BM-EC-082` | Is protection in place **before** the migration it protects, not after? | commit/temporal ordering evidence |
| `BM-EC-083` | Are contract, property, adversarial, performance and recovery categories each either exercised or explicitly justified as inapplicable? | test-category matrix with reasons |
| `BM-EC-084` | Is clean-build ≡ upgrade equivalence proven by execution, not assumed? | fresh-replay comparison output |

### 6.12 Application-architecture principles — adopted with boundaries

**No principle is adopted because it is standard practice.** Each row states what it solves
*here*, where it does not apply, what complexity it removes, what it introduces, and the
invariant it protects. A principle whose "where it does not apply" column is empty has not
been thought about.

| Principle | Problem it solves here | Where it does **not** apply | Complexity removed → introduced | Protects | Disposition |
|---|---|---|---|---|---|
| Separation of concerns (8 layers) | seven ingestion paths grew because layer ownership was implicit | not a mandate to split a cohesive module by ceremony | ownership ambiguity → more explicit boundaries to cross | `BM-INV-01` | `ADOPT` |
| Dependency inversion | domain rules currently reachable only through JDBC and Spring | not every class needs an interface; one implementation and no variation means no port | framework lock-in → indirection | `BM-QA-14` | `ADOPT` |
| Ports and adapters | Access, S3/MinIO, SQL Server are replaceable providers behind stable semantics | not for internal modules with no external variability | provider coupling → adapter surface to keep in sync | `BM-INV-08`, `BM-QA-14` | `ADOPT` |
| Contract-driven design | the platform's identity; onboarding by declaration, not by code | does not apply to genuinely new *primitives*, which require grammar evolution | site-specific branching → grammar/compiler machinery | `BM-INV-02` | `ADOPT` |
| Clean Architecture (dependency direction inward) | prevents storage and transport shape from becoming domain shape | not a mandate for four concentric rings or a new package layout | accidental coupling → mapping code | `BM-INV-02` | `ADAPT` |
| DDD: ubiquitous language and bounded contexts | site contract and statistical contract are genuinely different contexts (`DR-004`) | aggregates/repositories are not mandated where a typed table and a query plan are the real model | semantic collision → context-mapping obligations | `BM-INV-01` | `ADAPT` |
| SOLID | substitutability and one-reason-to-change per module | never as a justification for interface proliferation (`ANTI-PATTERNS`) | divergent duplicates → abstraction cost | `BM-QA-12` | `ADAPT` |
| Capability registry / strategy | supported behaviours extend by registry entry, not by `switch` on provider or site | unknown capability must be rejected, not defaulted | core branching → registry lifecycle | `BM-INV-18` | `ADOPT` |
| Transactional outbox | atomic write plus external notification already exists on the publication path | not a general event backbone; not an excuse to make the outbox an audit authority (`CF-041`) | dual-write loss → delivery semantics to operate | `BM-INV-14` | `CONDITIONAL` |
| Anti-corruption layer | legacy Access v1, import strategies and legacy controllers must not shape the target | not between two canonical internal layers | legacy leakage → translation code | `BM-EC-076` | `ADOPT` |
| CQRS / read projections | read and write constraints differ on the serving path | only with a demonstrated need, a rebuild procedure and a declared lag budget | read contention → consistency management | `BM-QA-09` | `CONDITIONAL` |
| Event sourcing as the canonical store | — | the canonical model is typed relational state with immutable snapshots; nothing requires an event log as truth | — → replay, versioning and projection burden | — | `REJECT` |
| Microservice decomposition | — | no scaling, team or deployment evidence requires it; it would multiply the authority-duplication failure already observed | — → distributed transactions, network failure modes | — | `REJECT` |
| Speculative plugin framework | — | `ContractPageExecutionRegistry` already demonstrates seven identical adapters as pure ceremony | — → indirection with no capability | `BM-QA-12` | `REJECT` |
| Universal/generic schema (EAV) | — | destroys typing, constraints, integrity and query guarantees; `CF-039` is the live instance | — → semantic loss | `BM-INV-05` | `REJECT` |

---

## 7. Rejection criteria — `BM-REJ`

A candidate is **rejected** — not merely marked down — when any row below is observed. These
are the failure modes that survive review because each looks locally reasonable.

| ID | Disqualifying condition | Why fatal here | Detection |
|---|---|---|---|
| `BM-REJ-01` | **Overengineering** — a mechanism with no observed variation to absorb | the repository already carries seven identical adapters behind a registry and a composer nothing calls | count the distinct behaviours the abstraction serves; one means delete it |
| `BM-REJ-02` | **Unnecessary abstraction** — indirection that cannot name the invariant it protects | abstraction without an invariant is cost without defence | ask the `BM-INV` it defends; no answer ⇒ reject |
| `BM-REJ-03` | **Accidental rigidity** — a new site/family/provider needs a core-code change | defeats the platform's stated identity (`REQ-015`, `SCH-003`) | onboard a second, unrelated fixture |
| `BM-REJ-04` | **Semantic weakening** — a capability is preserved in name while losing type, constraint, status or precision | this is how `CF-039` and the `28,10` truncation happened | before/after capability crosswalk with values |
| `BM-REJ-05` | **Fake agnosticism** — "schema-agnostic" implemented as schema-less, unrestricted SQL, or one universal table | `ARCHITECTURE.md` names this explicitly; it converts agnosticism into semantic loss | inspect canonical column types per family |
| `BM-REJ-06` | **Metadata magic** — behaviour driven by metadata that is unvalidated, unversioned, or executable | turns data into code and the caller into the architect | grammar closure test; look for eval/expression/SQL in metadata |
| `BM-REJ-07` | **EAV degeneration** — declared fields stored as rows or documents in a generic container | `CF-039`; the fix direction is `CAD-17` (raise the weak family, never lower the strong ones) | column-type inspection across all families |
| `BM-REJ-08` | **Duplicate authority** — a second mechanism can mutate the same semantic truth | the dominant failure of this repository (`RC-001`, `CF-033`, `CF-038`) | `CAD-15` questions 4–7; any `YES` unresolved ⇒ reject |
| `BM-REJ-09` | **Unnecessary provider coupling** — a provider name, limit or literal inside generic core | makes one consumer the architecture (`ENGINEERING-QUALITY-DOCTRINE` cardinal rule) | site/provider-literal lint over generic packages |
| `BM-REJ-10` | **Excessive manual configuration** — deterministically derivable information repeated by hand | `CAD-05`; hand-authored declarations are `RC-002`'s mechanism | for each manual artifact ask what derives it |
| `BM-REJ-11` | **Unsafe automation** — automatic approval, promotion, repair or inference of semantics | automation must never manufacture a decision a human owes (`STD-AUTO-001`) | check every `AUTOMATE` row against its safety preconditions |
| `BM-REJ-12` | **Premature optimisation** — a performance structure with no measurement and a semantic cost | caching/denormalisation that outruns evidence becomes a second read model (`QF-006`) | require the measurement that motivated it |
| `BM-REJ-13` | **Unbounded temporary architecture** — a bridge, allowlist or migration path with no exit condition | the checksum allowlist is the live instance (`REC-PASS3` §5) | every temporary mechanism must produce its exit row |
| `BM-REJ-14` | **Benchmark gaming** — the architecture satisfies the letter of a criterion while defeating its purpose | a benchmark that can be satisfied without being met is worse than none | for each `PASS`, name the failure it would now prevent |

---

## 8. Falsification of this benchmark

Applied to the benchmark's own riskiest claims: `OBSERVATION → HYPOTHESIS → EVIDENCE →
FALSIFICATION ATTEMPT → CONCLUSION → CONFIDENCE → DECISION`.

**F-1. "Typed canonical storage is required for every family."**
*Falsification attempt:* a document/JSON store for the entity family could be the deliberate,
correct choice for sparse, heterogeneous properties.
*Evidence:* `REC-PASS3` §12 searched for that justification and found none — no ADR, no
requirement, no test expressing intent; `REQ-006/007/008` state the opposite; the
statistical, relational and classifier families are already typed, so the platform's own
practice contradicts it. **CONCLUSION: holds. CONFIDENCE: HIGH.** *Decision:* `BM-INV-05`
stands; the physical shape (per-family tables vs typed projection) stays a `PHASE-004`
design question, not a benchmark decision.

**F-2. "Exactly one liveness authority."**
*Falsification attempt:* the second mechanism could be a legitimate derived cache.
*Evidence:* `REC-PASS3` §13 re-challenged exactly this: three readers consult status
*instead of* membership, which is the definition of an authority, not a cache.
**CONCLUSION: holds. CONFIDENCE: HIGH.**

**F-3. "SDMX is the right semantic frame."**
*Falsification attempt:* if SDMX were the platform information model, entity, relation,
resource and geo families would have to be expressed as cubes.
*Evidence:* four of six families in `PackagePlan.Family` are not cube-shaped; `SdmxCsvExporter`
exists at the export boundary only. **CONCLUSION: bounded adoption only — statistical
exchange and cross-domain codelists, at the contract and export boundary.
CONFIDENCE: MEDIUM-HIGH** (bounded by `BM-Q-01`: no conformance level is agreed).
*Decision:* `BM-EC-010` requires the boundary to be written down rather than assumed.

**F-4. "Maximum safe automation is a quality, not a risk."**
*Falsification attempt:* automation applied to classifier promotion, publication approval or
canonical repair would remove human semantic decisions and create silent corruption.
*Evidence:* `REC-PASS3` §10 already fixes those as human semantics; `PATTERN B` exists
precisely because external content carries decisions that cannot be safely derived.
**CONCLUSION: automation is a quality only under the preconditions in `STD-AUTO-001`;
unconditional automation is `BM-REJ-11`. CONFIDENCE: HIGH.**

**F-5. "This benchmark is complete enough for an independent reviewer."**
*Falsification attempt:* name a criterion a reviewer would have to invent.
*Evidence:* the candidates found are (a) performance *numbers* — `BM-Q-03`, owner decision;
(b) security control subset — `BM-Q-05`; (c) localisation depth — `BM-Q-07`; (d) period
semantics — `BM-Q-08`. Each is recorded as an unresolved standards question with a safe
interim default, so the reviewer inherits a named gap rather than an invented criterion.
Design choices (authoring format, physical pattern granularity, entity typing shape,
promotion workflow, metadata-schema placement) are **not** benchmark gaps: `REC-PASS3` §10
owns them as true design questions. **CONCLUSION: sufficient, with four named gaps.
CONFIDENCE: MEDIUM-HIGH.**

**F-6. "The benchmark itself is not accidental complexity."**
*Falsification attempt:* 14 attributes + 22 invariants + 84 criteria may be ceremony.
*Evidence:* every criterion names an evidence method, and §10 shows each maps to at least one
recovered requirement; rows that could name no evidence method were not written.
**Residual risk, stated honestly:** the criteria set is large enough that a reviewer under
time pressure will sample it. *Decision:* §2 makes the invariant gate binary and separate, so
sampling degrades the criteria score, not the safety gate. **CONFIDENCE: MEDIUM.**

**F-7. "The benchmark can reject a plausible but wrong architecture." — FALSIFIED ONCE, THEN
REPAIRED (PHASE-002 acceptance review, 2026-09-21).**
*Falsification attempt:* five deliberately plausible candidates were scored against the
benchmark as first written. Three were rejected for the right reasons — a universal JSON/EAV
engine (`BM-INV-05`), per-surface semantic models (`BM-INV-15`), and rigid per-dataset schemas
(`BM-EC-019/020`, `BM-REJ-03`). **Two got through.**

| Hole | How it passed | Repair |
|---|---|---|
| *Typed* EAV — one row per declared field with typed value columns, an attribute registry and FKs | `BM-EC-021`'s evidence method was column-type inspection, which typed EAV passes; `BM-INV-05` was checked field by field, and each field did have a type | `BM-EC-021` now uses the constraint-expressibility test; `BM-INV-24` requires the declared grain to be enforced by a business-key constraint, which EAV cannot express |
| Lowest-common-denominator provider abstraction — clean ports, declared capabilities, and canonical semantics quietly reduced to what the weakest provider holds | every portability criterion passed: the provider *was* one realisation among several, capabilities *were* declared. Nothing asked whether the canonical model had been lowered to meet them | `BM-EC-085` asks, for every absent capability, whether a requirement or a provider caused the absence — and fails the second answer |

*Conclusion:* the benchmark as first published had two material holes, both in the
**detection method** rather than the principle. A criterion whose evidence method is weaker
than its question is the most dangerous kind, because it reads as rigorous and passes the
thing it names. **CONFIDENCE after repair: MEDIUM-HIGH** — five candidates is a sample, not a
proof, and the next reviewer should add candidates rather than trust this row.

**F-8. "The gate condition was satisfied."**
*Falsification attempt:* `GATE-STANDARDS` requires that every material quality attribute name
a standard or an explicit non-adoption. Did §4 do that?
*Evidence:* **No.** The table had no standards column; a sentence beneath it asserted the
mapping existed. The gate's headline condition was satisfied by claim rather than by an
inspectable mapping — the documentation-only-evidence defect this repository has found
repeatedly, committed by the artifact whose job is to detect it.
*Decision:* the column now exists per attribute, including four honest "no external standard
exists" entries and one explicit non-adoption (ISO 9241), plus the statement of which adopted
standards have no hook here and why. **The first `GATE-STANDARDS: PASS` (2026-09-21) was
premature on this condition; it is re-confirmed only with this correction in place.**

---

---

## 9. Unresolved standards questions — `BM-Q`

Genuinely unresolved, **standards-level** questions. Design questions are not listed here;
`REC-PASS3` §10 owns those. Each row names who must answer, because an unowned question is a
question that gets answered accidentally by whoever implements first.

| ID | Question | Why material | Answer owner | **Resolution timing** | Interim rule the benchmark applies |
|---|---|---|---|---|---|
| ~~`BM-Q-01`~~ **RESOLVED 2026-09-21 → `ADR-015`** | ~~What SDMX conformance level is required?~~ The question had no answer in the standard: SDMX declares conformance per artefact class, not as ranked levels. The **boundary** is decided (statistical responsibility only); the **claim** was already owned by `Q47` | decides whether DSD concepts constrain the canonical model or only the export boundary | `OWNER` + `STEWARD` | `MUST RESOLVE BEFORE PHASE-004` | export-boundary conformance only; canonical model stays provider- and standard-neutral (`BM-EC-010`) |
| ~~`BM-Q-02`~~ **RESOLVED 2026-09-21 → `ADR-016`** | ~~Permanent or transitional?~~ Both framings were traps. Access is a supported, potentially long-lived provider with **no canonical semantic authority**, and no assumption of permanence | Access sets the 28-digit precision ceiling that `DR-006` propagates into the declaration envelope | `OWNER` | `MUST RESOLVE BEFORE PHASE-004` | treat as permanent; declaration ≤ weakest required provider (`BM-INV-09`) |
| `BM-Q-03` | What are the performance objectives (latency, throughput, volume ceilings) for query, import, export and generation? | `BM-QA-09` can require a budget but cannot invent its value; no SLO authority exists (W-05 open) | `OWNER` + Ops | `EXTERNAL BUSINESS/POLICY DECISION` — due before `PHASE-010` | every path declares a bound and fails closed; the number is provisional until owned |
| `BM-Q-04` | Is concurrent multi-site tenancy in scope for the canonical target, or is single-primary-site permanent? | `QF-003` shows a single-primary-site assumption; the answer changes the isolation model | `OWNER` | `CAN BE RESOLVED DURING CANONICAL DESIGN` | design for tenant-scoped isolation regardless; do not rely on a single primary site |
| `BM-Q-05` | Which version-qualified OWASP ASVS control subset applies? | `STANDARDS.md` forbids a blanket level claim; without a subset, security evidence is unfalsifiable | `SEC` | `EXTERNAL BUSINESS/POLICY DECISION` — due before `PHASE-009` | `BM-EC-068…071` remain mandatory; ASVS control IDs attach when chosen |
| `BM-Q-06` | Is there an external standard the platform must follow for non-cube (entity/resource/product) data, or is it a local model? | prevents forcing statistical vocabulary onto object-style data — or missing a real obligation | `STEWARD` | `CAN BE RESOLVED DURING CANONICAL DESIGN` | treat as a local typed model; `BM-EC-009` forbids forcing either shape into the other |
| `BM-Q-07` | What localisation depth is required — which fields are translatable, and is `und` an acceptable canonical fallback? | BCP 47 is enforced in code today but no artifact declares the *policy* | `OWNER` | `IMPLEMENTATION-TIME DECISION` (per contract) | BCP 47 tags mandatory; translatable fields declared per contract; `und` allowed only where already implemented |
| `BM-Q-08` | Which standard is canonical for period semantics — ISO 8601 intervals or reporting-period codes — when both are expressible? | two representations of the same period are a duplicate-authority risk at the most-used dimension | `STEWARD` | `CAN BE RESOLVED DURING CANONICAL DESIGN` | both may be *carried*; exactly one is canonical per dataset and declared in the contract |

**`MATERIAL_UNRESOLVED_STANDARDS_QUESTIONS: 6`** (was 8). `BM-Q-01` and `BM-Q-02` were resolved
on 2026-09-21 by `ADR-015` and `ADR-016` — the two that gated `PHASE-004` acceptance. The rows
stay in the table with their resolution, because deleting a question deletes the record that it
was ever open.

None of the remaining six blocks `GATE-STANDARDS` and **none blocks `PHASE-003`**: every row has an owner and an interim rule, and the comparative audit
scores artifacts under those rules while recording where a different answer would change a
verdict. That sensitivity note is mandatory — an audit whose ranking silently depends on an
unanswered question is an audit that will be re-run.

**Timing corrected by the acceptance review (2026-09-21).** `PHASE-002` first stated that
`BM-Q-01`, `BM-Q-03` and `BM-Q-05` all block `PHASE-004` acceptance. Re-examined against the
threshold classes, that was too strong for two of them:

- `BM-Q-03` (performance objectives) — `BM-QA-09` is `TARGET`/`BOUNDED`, not `REQUIRED`. A
  design is acceptable when every path **declares** a bound and fails closed; the *values* are
  an operations and business decision, and they bind before migration executes, not before
  the design is judged.
- `BM-Q-05` (ASVS control subset) — `BM-EC-068…071` and `SEC-001/002` are mandatory whatever
  subset is chosen. The subset selects the *depth of security evidence*, which is
  `PHASE-009` protection work, not a shape the design must know.
- `BM-Q-01` (SDMX conformance) and `BM-Q-02` (Access status) were `PHASE-004` blockers and are
  **now resolved** — `ADR-015` and `ADR-016`. What they leave behind is not a question but two
  obligations: multi-measure is inside the SDMX boundary and **unproven** (`CAP-M07`), and
  `ADR-016`'s provider rules are unenforceable until provider capability has one producer.

Loosening two of three is not a relaxation to make a gate pass — it is the difference between
a blocker and a deadline, and calling a deadline a blocker is how real blockers stop being
believed.

**The one non-standards item this section carried is closed.** The statistical-domain standards
profile (`S1…S16`) lived in a register classified `SUPPORTING` while describing itself as a
design authority, and two live work streams ran outside the rehabilitation lifecycle (`DEF-08`).
**`ADR-014` decided the programme relationship and `CTRL-ADOPTION` now classifies that register
`CANONICAL` for its domain** — the routing defect is closed at both ends. See
`docs/project/work/completed/TASK-002-work-stream-reconciliation.md`.

---

## 10. Requirement coverage — proof that nothing recovered was dropped

`GATE-STANDARDS` requires that every material quality attribute has a named standard or an
explicit decision not to adopt one. The reverse obligation is just as important: **every
recovered requirement must land somewhere in this benchmark, or be explicitly out of its
scope.** `REQ-001…041` are owned by `REC-CONSOLIDATION` §2 and are not restated here — only
mapped.

| `REQ` | Lands on | | `REQ` | Lands on |
|---|---|---|---|---|
| 001 | `BM-INV-02`, `BM-EC-030/031` | | 022 | `BM-INV-08`, `BM-EC-050` |
| 002 | `BM-EC-019`, `BM-EC-055` | | 023 | `BM-QA-04`, `BM-EC-048` |
| 003 | `BM-QA-01`, `BM-EC-001` | | 024 | `BM-EC-031/032` |
| 004 | `BM-REJ-05`, `BM-EC-021` | | 025 | `BM-EC-020`, `BM-INV-07` |
| 005 | `BM-QA-14`, `BM-EC-026/036/085` | | 026 | `BM-INV-07`, `BM-EC-046` |
| 006 | `BM-INV-05/24`, `BM-EC-016/017` | | 027 | `BM-EC-036/052` |
| 007 | `BM-REJ-07`, `BM-EC-021`, `BM-INV-24` | | 028 | `BM-INV-07/23`, `BM-EC-047` |
| 008 | `BM-INV-05/24`, `BM-EC-021` | | 029 | `BM-EC-017/011` |
| 009 | `BM-INV-18`, `BM-REJ-06` | | 030 | `BM-INV-17`, `BM-EC-023` |
| 010 | `STD-AUTO-001`, `BM-REJ-11` | | 031 | `BM-QA-09`, `BM-EC-072/073` (two-scale) |
| 011 | `STD-AUTO-001`, `BM-REJ-10` | | 032 | `BM-QA-06`, `BM-EC-028/064` |
| 012 | `BM-QA-11`, `BM-EC-006` | | 033 | `BM-INV-14`, `BM-EC-066` |
| 013 | `BM-EC-011` | | 034 | `BM-QA-07`, `BM-EC-068/069` |
| 014 | `BM-EC-020`, `BM-EC-051` | | 035 | `BM-QA-10`, `BM-EC-067` |
| 015 | `BM-REJ-03`, `BM-EC-019` | | 036 | `BM-QA-13`, `BM-EC-079…084` |
| 016 | `BM-EC-020/052` | | 037 | `BM-QA-14`, `BM-EC-026/085` |
| 017 | `BM-INV-10` | | 038 | `BM-EC-054/055` |
| 018 | `BM-EC-007` | | 039 | `BM-QA-08`, `BM-EC-074…078` |
| 019 | `BM-QA-01`, `BM-EC-003/004/005` | | 040 | `BM-REJ-03`, `BM-EC-019` |
| 020 | `BM-INV-15`, `BM-EC-054` | | 041 | `BM-QA-12`, `BM-REJ-01/02` |
| 021 | `BM-EC-051` | | | |

**41 of 41 mapped. 0 dropped.** `BM-INV-25` and `BM-EC-087…089` were added on 2026-09-21 by the
design-entry capability-envelope pass; each closes a falsification scenario that failed with no home
(`AUD-CAPABILITY` §13 - scenarios 12, 11 and 7). Layer coverage: every ownership layer of `ARCHITECTURE.md`
carries at least one criterion — Control 26, Data 22, Serving 13, Delivery 11, Ingestion 9,
Security 5, Archive 3, Observability 2 (criteria tagged with two layers count in both; the
twelve rows of §6.10–§6.11 are cross-layer by construction and carry no single owner tag).
**Observability and Archive are the thinnest columns, and that is a stated weakness of this
benchmark, not a claim that those layers matter less** — `BM-Q-03` (no SLO authority) and
`OBL-PROD-EVIDENCE` are why: several of their criteria can only be written once objectives
exist. The five
`PARTIAL` rows of `REC-COVERAGE` §3.2 (L5, L9, L16, L17, L40) are each addressed:
`BM-EC-030/038` (L5), `BM-EC-020/052` (L9), `BM-EC-001` (L16), `BM-EC-022` (L17),
`BM-EC-055/057` (L40).

---

## 11. Gate binding

`GATE-STANDARDS` passes when, and only when:

| Check | Where satisfied |
|---|---|
| Every material quality attribute has a named standard or an explicit non-adoption decision | §4, *Standard, or explicit non-adoption* column — one entry per attribute, including four "no external standard exists" and one explicit non-adoption; adopted standards with no hook here are named with the reason |
| Standards are dispositioned `ADOPT` / `ADAPT` / `CONDITIONAL` / `REJECT` with rationale | `STANDARDS.md` §*Disposition matrix* |
| A quality-attribute model exists with measures and thresholds | §4 |
| Cross-layer evaluation criteria exist, evidence-bound | §6 |
| An automation opportunity/classification model exists | `STD-AUTO-001` |
| Hard invariants for the later Canonical Architecture are stated and falsifiable | §5 |
| Anti-pattern / rejection criteria exist | §7 |
| Unresolved standards questions are explicit, owned and bounded | §9 |
| No parallel authority was introduced | §0 ownership table; `RCP-403/404`; `rcp-verify.py` |
| A later independent reviewer can evaluate competing architectures without inventing criteria | §2 verdict method + §8 F-5 |
| The benchmark rejects plausible but wrong architectures for the right reasons | §8 F-7 — five adversarial candidates scored; two holes found and repaired |
| The gate's own claims survive falsification | §8 F-8 — the first `PASS` was premature on the standards-mapping condition; re-confirmed with the correction in place |

**`PASS` means the evidence was satisfied. It is never a narrative compliment, and it
certifies nothing about runtime or production readiness.**
