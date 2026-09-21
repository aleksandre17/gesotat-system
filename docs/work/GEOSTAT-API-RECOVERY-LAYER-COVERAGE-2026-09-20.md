---
id: REC-COVERAGE
type: REGISTER
title: Recovery layer coverage ledger
status: COMPLETE
authority: CANONICAL
scope: L0-L47 architectural coverage and governance rules GOV-R1..R10
owner: architecture recovery
created: 2026-09-20
updated: 2026-09-20
---

# GEOSTAT API — Recovery Layer Coverage Ledger

**Created:** 2026-09-20, **during the PASS 2A governance correction.**

> **This artifact did not exist during earlier recovery phases.** It is not backdated and
> it does not reconstruct historical provenance. Earlier phases (checkpoint batches 1-11E,
> consolidation, completeness audit PASS 1 and PASS 2A) ran without it. The statuses below
> are initialized *from* the evidence those phases produced — they are not a claim that
> those phases used this ledger.

**Owns:** whole-architecture coverage. Which architectural responsibilities have been
inspected, and to what depth.
**Does not own:** findings (Consolidation §1), requirements (Consolidation §2), legacy
dispositions (Consolidation §8), evidence history (Checkpoint).

---

## 1. Purpose

Recovery must prove two different things, and PASS 1 proved only the first:

- **A — known findings are accounted for.** PASS 1 closed this: 10/10 inherited findings
  classified, three root causes falsified, producer/consumer and authority-collision
  sweeps complete.
- **B — no material architectural layer, boundary, transformation or lifecycle was
  omitted.** Nothing has proven this. PASS 2A exposed the gap: the entity canonical
  storage model (CF-039) had never been inspected across eleven batches, and it changed
  two requirement statuses.

This ledger exists so that **A is never again mistaken for B**.

## 2. How to read the taxonomy

The L0–L47 rows are **audit coverage categories**, not a claim that the final architecture
should contain 48 software layers. They may be merged, renamed, split or extended when
evidence requires it. **Never force evidence into this taxonomy** — if a material
responsibility does not fit, add a category rather than distort a row.

`INSPECTION STATUS` — `NOT_INSPECTED` · `PARTIAL` · `INSPECTED` · `VERIFIED`.
**`VERIFIED` requires primary implementation/schema/artifact evidence.** Documentation alone
can never raise a row above `PARTIAL`.

Column shorthand: **Auth** authority known · **Prod** producer known · **Cons** consumer
known · **Lin** lineage known · **Inv** invariants known · **Fail** failure semantics known
· **Loss** information loss known · **Auto** automation baseline known · **Leg** legacy /
parallel mechanisms accounted for. `Y` / `P` (partial) / `N`.

## 3. Coverage matrix

| ID | Name | Status | Auth | Prod | Cons | Lin | Inv | Fail | Loss | Auto | Leg | Material unknowns |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| L0 | Platform Meta-Contract (M3) | VERIFIED | Y | Y | Y | Y | P | N | N | N | Y | none — proven absent (RC-003); 6 docs, 0 code, 0 SQL |
| L1 | Control Plane Schema (M2 contract-creating authority) | VERIFIED | Y | Y | Y | Y | P | N | N | N | Y | none — proven absent as a producer (RC-003) |
| L2 | Contract grammar & schema/parser parity | INSPECTED | Y | Y | Y | P | Y | P | N | P | Y | parity corpus absent (DR-005) |
| L3 | Site contract declaration (M1) | INSPECTED | Y | Y | Y | Y | P | N | N | Y | Y | CF-024a — no producer |
| L4 | Statistical contract declaration (M1) | VERIFIED | Y | Y | Y | Y | Y | Y | P | Y | Y | none material |
| L5 | Ingestion contract declaration | PARTIAL | P | Y | Y | P | N | N | N | P | P | CF-032 — no revision written |
| L6 | Contract approval & revision lifecycle | VERIFIED | Y | Y | Y | Y | Y | Y | N | Y | Y | CF-014 — no withdrawal path |
| L7 | Semantic compilation | VERIFIED | Y | Y | Y | Y | Y | Y | Y | Y | Y | none — CF-004 closed at both layers |
| L8 | Derived plans (Semantic / Package / Physical) | VERIFIED | Y | Y | P | Y | Y | P | P | Y | Y | PackagePlan unwired (DR-003) |
| L9 | Physical pattern / realization profile | PARTIAL | P | P | P | P | N | N | N | P | P | pattern implicit in Java; only 1 of 6 families proven (DR-008) |
| L10 | Provider capabilities | INSPECTED | N | Y | Y | P | N | P | P | N | P | CF-010, CF-038 — three authorities, no producer |
| L11 | Statistical reference registry | VERIFIED | Y | Y | Y | Y | Y | P | N | Y | Y | none — PATTERN C proven here |
| L12 | Classifier registry | INSPECTED | Y | Y | Y | P | P | N | N | N | Y | CF-016 — no runtime producer |
| L13 | Classifier proposal & promotion | VERIFIED | Y | Y | Y | Y | Y | P | N | Y | Y | promotion half absent (CF-016) |
| L14 | Metadata schema registry | VERIFIED | Y | Y | Y | Y | N | N | N | N | Y | CF-033 — transport is the producer |
| L15 | Metadata subjects & assertions | VERIFIED | Y | Y | Y | Y | N | N | P | N | Y | CF-034, QF-012, INV-014 missing |
| L16 | Dimension / measure / metric / unit registry | PARTIAL | P | Y | Y | P | P | N | N | P | P | producer is the statistical binder only |
| L17 | Dataset & dataset-version registry | PARTIAL | P | Y | Y | P | P | N | N | P | P | — |
| L18 | Product & tenant registry | INSPECTED | Y | Y | Y | P | Y | N | N | N | Y | no producer for `data_product` (RC-003) |
| L19 | Access artifact generation | VERIFIED | Y | Y | Y | Y | Y | P | P | Y | Y | metadata plane producer unidentified (KNOWN GAP) |
| L20 | Access self-description (`__gs_*`) | VERIFIED | Y | P | Y | Y | P | N | P | Y | Y | no digest binds artifact to contract (DR-007) |
| L21 | Access authoring / data-entry UX | INSPECTED | Y | Y | Y | Y | P | N | N | Y | Y | CF-017 closed; nav derived automatically |
| L22 | Access package reading | VERIFIED | Y | Y | Y | Y | Y | P | P | Y | Y | none material |
| L23 | Package validation | VERIFIED | Y | Y | Y | Y | Y | P | P | Y | Y | package-internal only; false confidence |
| L24 | Package ↔ contract correspondence | VERIFIED | Y | Y | Y | Y | N | N | Y | N | Y | **MISSING mechanism** (DR-007) |
| L25 | Source / raw ingestion | INSPECTED | P | Y | Y | P | P | P | N | P | P | §7 raw preservation not audited |
| L26 | Staging & technical validation | INSPECTED | P | Y | Y | P | P | P | N | P | P | — |
| L27 | Normalization | INSPECTED | Y | Y | Y | Y | Y | P | Y | P | Y | CF-019 closed |
| L28 | Canonical statistical materialization | VERIFIED | Y | Y | Y | Y | Y | Y | P | Y | Y | CF-005 attributes not read back |
| L29 | Canonical entity materialization | INSPECTED | Y | Y | Y | P | N | N | P | N | P | **CF-039 — untyped `payload_json`** |
| L30 | Canonical relation materialization | INSPECTED | Y | Y | Y | P | P | N | P | N | P | **CF-040 — cardinality unenforced** |
| L31 | Snapshot lifecycle | VERIFIED | N | Y | Y | Y | N | P | N | Y | Y | **CF-025 — no owner, 5 writers, no CHECK** |
| L32 | Release gates | VERIFIED | Y | Y | Y | Y | Y | Y | P | Y | Y | statistical path bypasses the entry state |
| L33 | Publication & membership | VERIFIED | N | Y | Y | Y | N | P | P | Y | Y | **RC-001, CF-023, CF-032** |
| L34 | Rollback & recovery | VERIFIED | P | Y | Y | P | N | P | P | N | P | rollback does not restore snapshot status |
| L35 | Archive & retention | VERIFIED | Y | Y | Y | P | P | P | P | P | P | object-store lifecycle alignment is convention-only |
| L36 | Serving policy & eligibility | VERIFIED | Y | Y | Y | Y | Y | P | P | Y | Y | CF-021 — pre-gate states served |
| L37 | Contract-bound query execution | VERIFIED | Y | Y | Y | Y | Y | P | Y | Y | Y | no test coverage (23.39) |
| L38 | Pagination & keyset | INSPECTED | Y | Y | Y | P | Y | P | P | P | Y | — |
| L39 | Aggregation | VERIFIED | Y | Y | Y | Y | N | N | Y | Y | Y | **CF-027 — truncated at 1000 rows** |
| L40 | Projection & response shaping | PARTIAL | P | Y | Y | P | P | N | P | P | P | CF-028 inert parameters |
| L41 | Chart / visualization declaration | VERIFIED | N | Y | Y | Y | P | N | P | P | Y | **CF-030 — four mechanisms, two liveness models** |
| L42 | Export & SDMX | INSPECTED | Y | Y | N | P | Y | Y | P | P | Y | `SdmxCsvExporter` has **zero callers**; CF-005 blocks wiring it |
| L43 | Caching (serving cache) | INSPECTED | P | Y | P | P | N | N | N | N | P | membership-resolved; not traced end to end |
| L44 | Security: authentication & authorization | INSPECTED | P | Y | Y | P | Y | P | N | N | P | CF-008, CF-031; §18 trace-to-data not done |
| L45 | Tenancy isolation | VERIFIED | Y | Y | Y | P | Y | P | N | N | Y | strong and well tested; §17 scoping sweep pending |
| L46 | Observability & audit evidence | VERIFIED | Y | Y | Y | P | Y | P | P | P | P | publication outbox intent misstates its own effect (CF-023); trace/correlation not inspected |
| L47 | Migration chain & schema evolution | VERIFIED | Y | Y | Y | Y | P | Y | N | Y | Y | **RC-002, CF-012, CF-020, CF-036** |

### 3.1 Coverage summary

| Status | Count (after PASS 2B closure) |
|---|---|
| VERIFIED | 28 |
| INSPECTED | 15 |
| PARTIAL | 5 |
| NOT_INSPECTED | **0** |
| **Total rows** | **48** |

*(PASS 2A continuation read 24 / 15 / 9 / 0. PASS 2B raised L34, L35, L45, L46 to VERIFIED
and L25, L26, L43, L44 to INSPECTED from primary runtime evidence.)*

**L35, L42 and L46 were inspected in the PASS 2A continuation and are no longer
uninspected.** None was upgraded past `INSPECTED`: each has primary-evidence support for
its authority, producer and consumer, but none has had its information-loss and failure
semantics fully traced.

### 3.2 Remaining PARTIAL rows — explicit ownership (no silent deferral)

**PASS 2A closed, then PASS 2B closed, both on 2026-09-20.** Five rows remain `PARTIAL`.
Each states its exact unknown, its owner, and why it cannot change the recovered canonical
architecture.

| Row | Exact remaining unknown | Owner | Why it cannot change canonical architecture |
|---|---|---|---|
| L5 Ingestion contract | revision governance shape once CF-032 is remediated | **design phase** | the defect and required property are known (AMS-001 member 3); only the design is open |
| L9 Physical pattern | declared pattern for 5 of 6 families | **design phase** | DR-008 fixed the principle; per-family declaration is design output |
| L16 Dimension / measure / metric / unit registry | governed producer | **RC-003 remediation** | the missing-producer question is already the root cause; no new architecture is implied |
| L17 Dataset / dataset-version registry | governed producer | **RC-003 remediation** | same |
| L40 Projection & response shaping | full projection surface enumeration | **PASS 3** | CF-028 bounds the defect; remaining work is enumeration for protection coverage, not discovery |

**No material runtime or data-semantic unknown remains.** Each residual is either a design
output or an enumeration task for protection coverage.

## 4. Automation classification obligation

Every material row must eventually carry one of: `AUTOMATE` · `ASSIST` · `KEEP EXPLICIT` ·
`HUMAN DECISION REQUIRED`, per the Maximum Safe Automation doctrine recorded in the
Consolidation (REQ-010, REQ-011) and its Automation Baseline (Consolidation §10). **That
doctrine is referenced here, not duplicated.**

Classified so far, from evidence:

| Row | Classification | Basis |
|---|---|---|
| L19 Access generation | **AUTOMATE** | deterministic from the compiled plan |
| L21 Access authoring UX / navigation | **AUTOMATE** | derived from the plan (`navigationGroups`) |
| L7 Semantic compilation | **AUTOMATE** | deterministic, digest-verified |
| L13 Classifier promotion | **HUMAN DECISION REQUIRED** | governed review is the point of the boundary |
| L6 Contract approval | **HUMAN DECISION REQUIRED** | four-eyes |
| L25 Source data entry | **KEEP EXPLICIT** | the authoring workflow is the human contribution |
| L3 Site contract declaration | **ASSIST** | derivable in part, but declaration is a human act |

All other rows: **unclassified**.

---

## 5. Governance rules materialized now

> These rules are **created by this governance correction**. They did not exist under this
> wording historically, and nothing here claims otherwise.

### GOV-R1 — Completion rule for legacy elimination

```text
MIGRATED ≠ DONE.

MIGRATED
  + LEGACY ELIMINATED OR EXPLICITLY JUSTIFIED
  + VERIFIED
  = DONE.
```

Every material mechanism in the Consolidation's Legacy/Duplication Register must
eventually carry exactly one disposition: `KEEP` · `EVOLVE` · `MERGE` · `MIGRATE` ·
`REPLACE` · `DEPRECATE` · `DELETE` · `REQUIRED-BUT-MISSING` · `LEGITIMATE-SPECIALIZATION`.
**The register already exists and already carries dispositions — this rule governs it
rather than duplicating it.**

### GOV-R2 — Contradiction closure

Every material contradiction must eventually reach one of: `RESOLVED — canonical side
selected` · `RESOLVED — semantics merged` · `RESOLVED — legitimate specialization with an
explicit boundary` · `SUPERSEDED` · `REMOVED`. **No material unresolved contradiction may
survive final rehabilitation.** Tracked in the Consolidation's contradiction register
(§12), not duplicated here.

### GOV-R3 — Parallel architecture

Two mechanisms serving one responsibility may coexist in the final architecture **only** as
legitimate specializations or explicitly required compatibility. Temporary migration
coexistence **must carry an exit condition**. A new canonical mechanism standing beside an
obsolete *active* mechanism is **not** completion.

### GOV-R4 — Knowledge-preserving elimination

Before `DELETE` or `REPLACE`, account for unique business behaviour, semantic meaning,
data, migration history, compatibility behaviour, provider knowledge, failure handling and
operational knowledge. Obsolete implementation may be deleted **only after** unique
knowledge is preserved or migrated.

### GOV-R5 — Consumer-first removal sequence

```text
canonical replacement -> behaviour/data coverage -> consumer migration
  -> compatibility verification -> regression verification
  -> repository-wide reference check -> disable old path
  -> verify no runtime dependency -> delete -> verify again
```

### GOV-R6 — Documentation and ADRs are in elimination scope

Authoritative-looking stale documentation may not stand beside canonical documentation
without a `HISTORICAL` or `SUPERSEDED` classification. Incorrect decision references of the
**CF-036 class** (migrations 111/112 citing ADR-011 for a decision held in ADR-012) must
eventually be corrected.

### GOV-R7 — Test classification

Every material test must eventually be classified: `CANONICAL BEHAVIOR PROTECTION` ·
`MIGRATION COMPATIBILITY` · `LEGACY BEHAVIOR` · `INCORRECT EXPECTATION` · `OBSOLETE`.
**A passing test is not automatically a canonical requirement.**

### GOV-R8 — Migration history caution

```text
HISTORICAL MIGRATION RECORD  ≠  ACTIVE ARCHITECTURAL MECHANISM
```

Historical migrations are **not** to be deleted for cleanliness. But a clean build and an
upgraded database must eventually converge to canonical-equivalent state — which CF-020
and the unregistered `056_activate_kids_r8_supersede_legacy.sql` currently prevent.

### GOV-R10 — Canonical authority doctrine (materialized 2026-09-20)

The **Canonical Authority & Anti-Parallelism Doctrine**
(`GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md`) is canonical governance. It
holds the **Authority Registry (CAD-02)** and rules **CAD-01 … CAD-18**.

Two rules bind every future task directly:

- **CAD-03 new-mechanism gate** — before adding any mechanism, name the responsibility and
  its existing authority, then classify the proposal. **If it cannot be classified, stop.**
- **CAD-16 agent execution rule** — every implementation prompt must state *do not create a
  new parallel architectural mechanism*; if the canonical mechanism cannot support the
  requirement, **stop and report the conflict** rather than building beside it.

GOV-R1 and GOV-R5 are restated at full length as **CAD-13**, which is their operative form.
GOV-R6, R7 and R8 are expanded into CAD-07/08, CAD-10 and CAD-09 respectively. Nothing is
superseded.

### GOV-R9 — Anti-loss process rule

**Every future bounded prompt is additive to the canonical repository baseline unless it
explicitly declares `SUPERSEDES: <artifact/rule>`.**

**Conversation-only instructions are not sufficient long-term governance.** Any new
architectural invariant, requirement, decision, coverage obligation, elimination rule or
automation doctrine discovered during a session **must be persisted into the appropriate
canonical repository artifact before the phase depending on it can close.**

*This rule exists because it was violated: PASS 2A was asked to update a coverage matrix
that had only ever existed in conversation. The correct response was to say so and
materialize it — which is what this artifact is.*

---

## 6. Positive counter-examples — proven good mechanisms

Recorded so that later design **reuses what already works** instead of assuming the
platform is uniformly defective.

| Mechanism | What it proves | Reuse target |
|---|---|---|
| `entity_classification` binding by `classification_item_id`, never by label, with temporal validity | the platform models classifier identity correctly | any classifier binding |
| `AccessClassifierProposalImportService` — writes only `classifier_proposal`, hardcoded `'DRAFT'`, no UPDATE branch, read-only artifact open | **PATTERN B** — external evidence → governed promotion | remediation of CF-033 / CF-034 |
| `tr_statistical_reference_definition_immutable` + canonical content digest | **PATTERN C** — semantic identity cannot silently drift | INV-014, DR-007 correspondence |
| `AccessAuthoringAdapter` — no site literals, provider reserved-words correctly isolated | **PATTERN A** — deterministic generation from authority | all family realizations (DR-008) |
| `navigationGroups` deriving Access organisation from the plan | maximum safe derivation applied to usability | authoring experience |
| `ReleaseGateService` — evidence schema check, facts-digest binding, fail-closed | gate design is correct; only the writer violates its scope | publication remediation |
| `SiteContractRevisionApprovalService` — lock, CAS, pluggable checks, checksum evidence, outbox | governed approval done properly | CF-024a producer shape |

---

## 7. PASS 2A status — preserved unchanged

```text
CF-015:                    CLOSED — NOT SUPPORTED + REQUIRED-BUT-MISSING
                           (constant value per declared dimension combination)
CF-019:                    CLOSED — SAFE NORMALIZATION
CF-039:                    canonical entity properties in untyped payload_json;
                           Access transport typing is NOT canonical storage integrity
CF-040:                    declared relation cardinality not enforced canonically
REQ-007:                   PARTIAL
REQ-008:                   VIOLATED
observation_status CHECK:  retained as another instance of the CF-025 defect family

INFORMATION_LOSS_MATRIX:       PARTIAL
IDENTITY/VERSION/PROVENANCE:   NOT YET COMPLETED IN PASS 2A
STATISTICAL_SLICE:             PASS (canonical-storage scope)
ENTITY_RELATIONAL_SLICE:       FAIL — CF-039, CF-040
CLASSIFIER_SLICE:              PASS
PASS_2A_GATE:                  FAIL
RECOVERY STATUS:               INCOMPLETE
```

**Next:** continue PASS 2A from the uninspected sections listed in the Completeness Audit
§1C.8 — and note that this ledger has now added three previously unnamed gaps (L35, L42,
L46) to that list.
