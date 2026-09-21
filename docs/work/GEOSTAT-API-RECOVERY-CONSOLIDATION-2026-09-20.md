---
id: REC-CONSOLIDATION
type: REGISTER
title: Recovery consolidation baseline
status: COMPLETE
authority: CANONICAL
scope: canonical finding, requirement, authority, lineage and legacy registers
owner: architecture recovery
created: 2026-09-20
updated: 2026-09-20
---

# GEOSTAT API — Recovery Consolidation Baseline

**Date:** 2026-09-20
**Target:** `platform/apps/geostat/backend/api`
**Baseline commit:** `b46c0438d4927851fcd688a8a8e71468aba6cf18` (`security/hardening-2`, dirty: 26 modified + 29 untracked in target)
**Companion artifact:** `GEOSTAT-API-ARCHITECTURE-RECOVERY-CHECKPOINT-2026-09-20.md` (5,302 lines — the evidence record; this file is the reconciled baseline)
**Phase:** consolidation only. No implementation, no redesign, no Master Plan.

This document exists so that no future session needs conversation history. Where this file
and the checkpoint disagree, **this file is current** and the checkpoint is the evidence
trail.

### Canonical governance references — which artifact owns what

| Artifact | Owns |
|---|---|
| **This file** — `GEOSTAT-API-RECOVERY-CONSOLIDATION-2026-09-20.md` | the canonical finding register (§1), requirement ledger (§2), proven patterns (§3), authority/lineage/contract/physical-pattern registers (§4-§7), **Legacy/Duplication Register (§8)**, missing components (§9), **Maximum Safe Automation baseline (§10)**, contradictions (§12), orphans (§13), known gaps (§14) |
| `GEOSTAT-API-RECOVERY-LAYER-COVERAGE-2026-09-20.md` | **whole-architecture coverage** (L0-L47), and the materialized governance rules **GOV-R1 … GOV-R9** |
| `GEOSTAT-API-RECOVERY-COMPLETENESS-AUDIT-2026-09-20.md` | adversarial audit results, per-finding verification evidence, and what each pass did **not** cover |
| `GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md` | **the Authority Registry (CAD-02)** and the permanent anti-parallelism rules **CAD-01 … CAD-18**. §4 of this file is the evidence-time snapshot; the doctrine holds the governed form. |
| `GEOSTAT-API-RECOVERY-PASS3-CLOSURE-2026-09-20.md` | **protection requirements**, the clean-build proof specification, the temporary architecture register, document dispositions, and the **Recovery → Design handoff** |
| `GEOSTAT-API-ARCHITECTURE-RECOVERY-CHECKPOINT-2026-09-20.md` | the evidence trail (batches 1-11E) |

**No register is duplicated across artifacts.** The coverage ledger *references* the Legacy
Register and the Automation Baseline held here rather than restating them.

**GOV-R9 (anti-loss), materialized 2026-09-20:** every future bounded prompt is **additive**
to this baseline unless it explicitly declares `SUPERSEDES: <artifact/rule>`.
Conversation-only instructions are not sufficient long-term governance; any invariant,
requirement, decision, coverage obligation, elimination rule or automation doctrine
discovered in a session **must be persisted into the appropriate artifact before the phase
depending on it can close.**

---

## 0. Identifier reconciliation

Inventoried directly from the checkpoint, not from memory:

| Series | Range | Gaps |
|---|---|---|
| CF | CF-001 … CF-044 (CF-024 split into CF-024a / CF-024b; CF-036/037 added by the completeness audit; **CF-043/044 added 2026-09-20 by the durable-knowledge closure, after `PHASE-001` closed** — recorded here because this register is the canonical owner of findings and a second findings register is forbidden) | none |
| RC | RC-001 … RC-003 | none |
| DR | DR-001 … DR-008 | none |
| INV | INV-001 … INV-014 | none |
| QF | QF-001 … QF-012 | none |
| AMS | AMS-001 | none |
| DR-REC | DR-REC-001 … DR-REC-007 | prior agent's recovery conclusions; superseded in scope by DR-001…DR-008 but **not** contradicted |

**No identifier is unaccounted for. No identifier was invented to fill a gap.**

### 0.1 Verification provenance — stated honestly

Findings CF-001 … CF-020 were **inherited** from the previous agent (GPT-5.6 Sol). This
engagement re-verified a subset against repository evidence. The rest remain inherited and
unverified, and are marked so. This distinction is load-bearing: an inherited finding is
evidence, not authority.

| Verification status | Outstanding |
|---|---|
| **All 20** — CF-001…CF-020 now have independent classifications. See the audit for per-finding evidence. | **none** |

**Completeness-audit PASS 1 CLOSED (2026-09-20): all 10 inherited findings independently
classified.** REJECTED: CF-001, CF-017. REVISED: CF-002, CF-012, CF-019. CONFIRMED:
CF-010, CF-011, CF-018, CF-020. PARTIAL: CF-015. Two carry bounded stated residuals
(CF-015, CF-019); neither can change canonical architecture.

---

## 1. CANONICAL FINDING REGISTER

Status vocabulary: OPEN · CONFIRMED · REVISED · RESOLVED · KNOWN-GAP ·
REQUIRED-BUT-MISSING · MIGRATION-BLOCKER · IMPLEMENTATION-DEFECT · ARCHITECTURAL-DEFECT ·
INHERITED-UNVERIFIED.

### 1.1 Root causes

| ID | Statement | Subsumes | Confidence | Status |
|---|---|---|---|---|
| **RC-001** | **FALSIFICATION SURVIVED (audit):** no trigger, no constraint, no outbox reconciliation and no scheduled job enforces equivalence; CF-023 actively breaks it. "Live" has two authorities: the publication aggregate (`publication.snapshot` + `snapshot_member`) is the intended release boundary, while `publication.dataset_snapshot.status` acts as a second, unowned liveness flag that three read paths trust. The two sets are not equal. | CF-007, CF-021, CF-022, CF-023, CF-025, CF-030 | HIGH | ARCHITECTURAL-DEFECT |
| **RC-002** | **FALSIFICATION SURVIVED (audit):** declarations are hand-authored — 0 of 113 migrations carry a generated-artifact marker and no generator exists; `alreadyEffective()` shows the record is tolerant, not authoritative, which strengthens the claim. Also absorbs the decision-provenance drift of CF-029/CF-036. The migration chain carries three incompatible responsibilities: canonical schema evolution, the only producer of site declarations and activation data, and a partially-registered historical record with checksum rewrites. | CF-011, CF-012, CF-020, rebuild divergence | HIGH | ARCHITECTURAL-DEFECT — reframed by RC-003 |
| **RC-003** | **The platform's two highest meta-levels were never implemented.** `Platform Meta-Contract` (M3) and `Control Plane Schema` (M2, the contract-creating authority) appear in **6 documents, 0 Java, 0 SQL**. Migrations are the substitute for the entire missing upper stack. | RC-002, CF-024a, CF-016, SCH-003 unachievability, rebuild divergence | HIGH | ARCHITECTURAL-DEFECT — deepest |

**RC-003 scope (evidence-based, batch 11D):** canonical registries with **no runtime
producer** — `platform.site_contract_*`, `platform.data_product`,
`platform.classification_*`, and — **added by the completeness audit** —
`platform.provider_capability` (`ProviderCapabilityDiscoveryRunner` only *loads and
validates*, throwing when none is ACTIVE). Registry writable by a **transport artifact** —
`platform.metadata_schema` / `_subject` (CF-033/CF-034).

**Falsification (audit pass 2): SURVIVED on five independent axes** — SQL write verbs across
java/sql/yml (4 writes, all UPDATE, 0 INSERT); JPA (one `@Entity` in the whole api module,
mapping none of these tables; none in core); stored procedures (**none exist** in 113
migrations); startup bootstrapping (7 runners, none a producer); build/CLI tooling.
**Confidence raised to HIGH+.**

**New dependency:** DR-006 decided the numeric envelope must move *into* the provider
capability registry — and that registry has no producer either. DR-006's remediation
therefore depends on RC-003's.

**RC-003 is NOT narrowed by the discovery of `AccessAuthoringAdapter`.** That generator is
*downstream* of an approved declaration; finding good derivation machinery says nothing
about the missing upstream authoring layer. Preserved per instruction 7.

### 1.2 Conflicts — publication and serving (RC-001 family)

| ID | Current statement | Confidence | Status |
|---|---|---|---|
| CF-007 | Publication request scope differs from publication effect. **Superseded in precision by CF-023.** | HIGH | REVISED → see CF-023 |
| CF-021 | `ContractPhysicalQueryService.canonicalSource` serves snapshots with `status IN ('SEMANTIC_REVIEW','PUBLISHED','APPROVED')`. `SEMANTIC_REVIEW` is a **pre-gate** state and is a mandatory intermediate state of every governed package run (stages 400→700). `'APPROVED'` is written by nothing. Reachable in normal flow; `frontend/kids` consumes the affected route. | HIGH | IMPLEMENTATION-DEFECT, P0-class |
| CF-022 | Two liveness resolution models coexist. Membership-based: `CanonicalStatisticalQueryAdapter`, `RelationalStatisticalQueryAdapter`, `PlatformServingCacheService`, `PlatformPublicationArchiveService`, migrations 073/075. Status-based: `canonicalSource` (set-valued `IN`, non-deterministic), `ServingPolicy.rawRows`, `CanonicalObservationReader`. | HIGH | ARCHITECTURAL-DEFECT |
| CF-023 | `PlatformPublicationService` validates and gates **one** snapshot; `DataPlanePublicationWriter:28` selects members by `product_id + REVIEW_REQUIRED` ignoring the requested id, and `:31` flips **every** such snapshot to `PUBLISHED` including non-members. `rollback` restores only `publication.snapshot`, never `dataset_snapshot.status`. Gate evidence is evaluated for one snapshot and applied to N. | HIGH | MIGRATION-BLOCKER, P0 |
| CF-025 | **Second instance found (PASS 2A):** `[statistics].observation.observation_status VARCHAR(24) DEFAULT 'VALID'` is likewise unconstrained. `publication.dataset_snapshot.status` is `VARCHAR(24)` with **no CHECK constraint**, default `'PREPARED'` (written by nothing), five independent writers, one unguarded backward transition (`SemanticMaterializationService:76`), and an accepted-on-read value (`'APPROVED'`) no writer produces. | HIGH | ARCHITECTURAL-DEFECT |
| CF-031 | `ChartService.data` → `CanonicalObservationReader` never calls `ServingPolicy`. Latent today because chart endpoints are gated by a stricter workflow authority; **becomes a real escalation the moment CF-008 read authority is corrected**. | HIGH | IMPLEMENTATION-DEFECT — hard precondition of CF-008 |
| CF-032 | `StatisticalBindingService` creates `ingestion_contract` with `contract_code = product.dataset`, **no `contract_revision`** and no `ingestion_contract_revision`. `assertApprovedContract` requires all three to align. `ingestion_contract_revision` has **no Java writer** (only migrations 022/023/071). **Governed publication is structurally closed to statistical datasets**, making CF-023's sweep their de-facto only publication path. | HIGH | MIGRATION-BLOCKER, CRITICAL |

### 1.3 Conflicts — declaration, contract and schema authority

| ID | Current statement | Confidence | Status |
|---|---|---|---|
| CF-003 | **RESOLVED.** Not three competing authorities but one layered envelope: declaration ≤ (28,16) bounded by Access; canonical storage `DECIMAL(38,16)` as a strict superset; legacy `28,10` silently altered source values. See DR-006. | HIGH | RESOLVED |
| CF-004 | **REVISED and narrowed.** `approvedPlan` recompiles **and compares** digests, throwing `DIGEST_MISMATCH`; the Javadoc states drift is "detected instead of silently reinterpreted". **Original claim that *meaning* can change is falsified.** Remaining scope: **availability only** — a superseded reference makes an approved contract stop compiling without any contract change (AIR-2026-055). Residual semantic-drift risk **CLOSED** (batch 9: the digest binds resolved *outputs*, not mutable inputs). | HIGH | REVISED — availability defect |
| CF-006 | **RESOLVED as LAYERED REPRESENTATION, not competing authority.** Site contract owns *serving/presentation*; statistical contract owns *dataset semantics*; both project into the shared Control Plane registry. Neither is a superset. **The defect is the missing link**, not duplication. See DR-004. | HIGH | RESOLVED → DR-004 |
| CF-008 | **VERIFIED and refined.** `StatisticalChartController` has `@TenantScoped` but **no `@PreAuthorize` on any of four endpoints**; `ContractWorkflow.get` requires at least one of AUTHOR/APPROVE/IMPORT, deliberately ("an approver must see what they approve"). A `READ_RESOURCE` holder cannot read an approved chart. **Remediation is the `AccessDecision` mapping, not an annotation.** Precondition: CF-031. | HIGH | IMPLEMENTATION-DEFECT |
| CF-009 | **RESOLVED.** One grammar, two hand-written representations, correctly layered (shape vs semantics), with **no mechanical parity enforcement**. Schema is referenced by exactly one Javadoc — unwired. See DR-005. | HIGH | RESOLVED → DR-005 |
| CF-016 | **REVISED.** Not "no governed API": the **proposal half exists and is correct** (`AccessClassifierProposalImportService` → `classifier_proposal`, `state='DRAFT'`, no UPDATE branch). The **promotion half does not exist** — nothing reads `classifier_proposal`, and there are **zero Java writers** of `platform.classification_*`. Third instance of RC-003. | HIGH | REVISED |
| CF-024a | The M1 site-contract declaration layer has **no governed producer**. Only writers: KIDS seed migrations 022, 023, 054, 055, 057 (+097/098 binding), plus one test fixture. Behaviour-verified: `site_contract_revision` has **2 UPDATEs, 0 INSERTs**. | HIGH | REQUIRED-BUT-MISSING |
| CF-024b | The **entire consumer side** of contract-only onboarding is designed, built and tested (approval, query execution, serving policy, introspection, compatibility, composer). Blocked solely by CF-024a. Evidence: `PackageContractSourceTest.anApprovedRevisionOfAnySiteBecomesOnePlan` passes using a fixture that writes rows production cannot. | HIGH | KNOWN-GAP |
| CF-029 | Migration 110's header states legacy `visualization_definition` rows "are converted and proposed". **No converter exists** — `ChartSuggestions.propose(SemanticPlan)` is a static pure function with no Control Plane access; no class references both chart models. A false claim embedded in an applied, checksummed migration. | HIGH | IMPLEMENTATION-DEFECT |
| CF-035 | `materializeMetadata`'s `site_contract_revision` lookup filters on `contract_code` + `revision` with **no `status='APPROVED'` predicate**. A package may bind to a DRAFT / REVIEW_REQUIRED / SUPERSEDED revision. | HIGH | IMPLEMENTATION-DEFECT |

### 1.4 Conflicts — Access trust boundary

| ID | Current statement | Confidence | Status |
|---|---|---|---|
| CF-033 | `materializeMetadata` `MERGE`s `platform.metadata_schema` from the package's own `schema_json`, with `lifecycle_status = state(package approval_state)`. `state("READY") → "APPROVED"`. **The platform's only schema-shaped registry has a transport artifact as its de-facto producer.** | HIGH | ARCHITECTURAL-DEFECT |
| CF-034 | `INSERT platform.metadata_subject(...) VALUES(...,'APPROVED','PUBLIC')` — a package creates an approved, publicly visible canonical subject with no governance step. | HIGH | ARCHITECTURAL-DEFECT |
| CF-026 | `MSSQLToAccess.buildSelectQuery` concatenates caller-supplied `databaseName`/`tableName` into SQL with bracket/backtick quoting and **no identifier validation**. Guards (`DatabaseEndpointGuard`, `HostAllowList`) constrain the **connection**, not the **identifiers**. Canonical fix already exists in-repo: `ContractPhysicalQueryService.identifier()`. **Live consumer: `geostat-system-app`.** | HIGH | IMPLEMENTATION-DEFECT — gated, default-off |
| CF-036 | **NEW (audit).** Migrations `111` and `112` cite **ADR-011** for "owner decision D-3"; `D-3` lives in `ADR-access-package-integrity-automation.md`, which is **ADR-012**. The two migrations implementing DR-006 cite an ADR that does not contain the decision. Same family as CF-029. | HIGH | IMPLEMENTATION-DEFECT |
| CF-037 | **NEW (audit).** `org/base/api/runner/CarsMigrationRunner` is a live `@Component ApplicationRunner` whose `run()` body is entirely commented out; the commented code executed `migration/eoy_2017.sql` via an **unqualified** `@Autowired JdbcTemplate` in a three-datasource application. Shadow startup architecture, in no register. | HIGH | ABANDONED EXPERIMENT |
| CF-038 | **NEW (audit).** Provider limits have **three** authorities: the registry (`max_page_size` only), Spring properties (numeric envelope) and the physical planner (Access column/name limits). No single place is authoritative. Blocks DR-006's instruction to move the envelope into the registry. | HIGH | ACCIDENTAL SECOND AUTHORITY |
| CF-039 | **NEW (PASS 2A).** Entity properties have **no typed canonical representation**: every declared field lives in `entity_record.payload_json` (NVARCHAR(MAX)) and is read back with `JSON_VALUE`. No type, nullability, length, uniqueness or referential integrity is enforced below the contract. Statistical, relational and classifier families are all properly typed. | HIGH | ARCHITECTURAL-DEFECT |
| CF-040 | **NEW (PASS 2A).** Declared relation cardinality is enforced nowhere. `entity_link` has FKs on both endpoints but no uniqueness/cardinality constraint; the contract's `ONE_TO_MANY`/`MANY_TO_MANY` is consulted only by `hasManyChildren()` for presentation shape. | MEDIUM-HIGH | IMPLEMENTATION-DEFECT |
| CF-041 | **NEW (PASS 2B).** `platform.outbox_event` carries three incompatible categories in one table: **command intents** (`PUBLISH_SNAPSHOT`, `ARCHIVE_PUBLICATION`, `PUBLISH_RESOURCE`), a **domain event** (`SITE_CONTRACT_REVISION_APPROVED`), and is additionally relied on as **audit evidence** of what was released — where CF-023 proves it misstates the effect. Different correctness requirements, one mechanism. | MEDIUM-HIGH | ARCHITECTURAL-DEFECT |
| CF-042 | **NEW (PASS 3).** Eight documents assert architectural finality over overlapping responsibilities (`final-statistical-contract`, `final-unified-physical-virtual-contract`, `final-physical-database-architecture`, `unified-canonical-platform-final`, `final-kids-canonical-model`, `final-classifier-contract`, `final-import-contract`, `final-access-control-plane-doctrine`) and **none carries a status marker** — no SUPERSEDED, no HISTORICAL, no successor link. All eight look equally current. Dispositions set in PASS 3 Closure §3.1. | HIGH (agent-executed rehabilitation) | ARCHITECTURAL-DEFECT |
| CF-043 | **NEW (durable-knowledge closure, 2026-09-20 — not a PHASE-001 finding).** CF-042's sibling, and worse: documents assert **current-state and continuation authority**, not just finality. `docs/CONTINUATION_HANDOFF.md` (2026-09-09) states *"this file is the authoritative continuation note for the next session. Read it before changing platform code or databases"*; `docs/production-evidence-handoff.md` states *"This document is the canonical handoff"*; `docs/KIDS-R8-current-status-and-acceptance.md` states *"is the current runtime status authority … and supersedes older R7 execution notes and handoff files"*. With `CTRL-CURRENT` that is **four documents claiming to be what a new session reads first**, over partly overlapping scope, none pointing at the others. Filename-based CF-042 detection missed the whole class because none of them contains `final`/`canonical`/`complete`. Dispositions and the retained-scope split are in `CTRL-ADOPTION` §8. | HIGH (agent-executed rehabilitation) | ARCHITECTURAL-DEFECT |
| CF-044 | **NEW (durable-knowledge closure, 2026-09-20 — not a PHASE-001 finding). RESOLVED 2026-09-20 by extraction audit; see §1.4a.** Three raw AI conversation transcripts totalling **647,373 lines** sit at the repository root. They are classified `HISTORICAL` in `CTRL-ADOPTION` §8 and remain undeleted under `OBL-NO-SILENT-LOSS`. The extraction audit found **0 durable items without a repository owner**, but did expose a routing defect in three obligation-bearing documents — recorded in §1.4a, fixed by reference from `CTRL-MANIFEST`. | MEDIUM | PROCESS-DEFECT — **knowledge-loss risk CLOSED** |

#### 1.4a CF-044 extraction audit — method and outcome

**Method, so the result can be falsified rather than trusted.** The transcripts were
decomposed by turn role: 841 `User` turns (275,680 chars), 2,917 `Assistant` turns
(1.71 M chars) and 10,050 `Activity` turns (tool-call traces). Durable knowledge
originates in `User` turns — requirements, acceptance criteria and constraints; `Assistant`
turns are derivative of files the agent then wrote; `Activity` turns are file-read/edit
traces carrying no authority. The 841 user turns reduce to **164 substantive unique turns**
(228,355 chars) after removing acknowledgements, continuations and verbatim repeats. Those
164 were read in full, and 22 distinct durable items were extracted and each tested against
the repository for a canonical owner.

**Outcome: 22 items, 22 owners, 0 unowned.** Nothing in the transcripts is the sole source
of a durable fact. The transcripts are therefore **not required for correct continuation**,
and their eventual archival under `ARCHIVE_LATER` loses nothing.

| Extracted item | Canonical owner |
|---|---|
| Quality doctrine (contract-first, metadata-driven, schema/provider/site-agnostic, fail-closed, deny-by-default, documentation-as-code, idempotent, observable, testable) | `/AGENTS.md` + `docs/reference/ENGINEERING-QUALITY-DOCTRINE.md` |
| "Panel declares the site canon; Access fills it and carries no logic" | CAD-02 (declaration = `site_contract_*`, Access = executor); REQ ledger §2 |
| `__raw_document` envelope fields; `__gs_`/`__cl_`/`__ent_`/`__rel_`/`__stat_` namespaces | §7 Physical pattern register; Access package contract docs |
| Release gates before publish (quality / privacy / reconciliation) | `REC-PASS3` §1; release-gate tooling |
| **W-01…W-07 remaining production workstreams + dependency order** | `docs/platform-capability-and-architecture-audit-2026-09-13.md` §17 — **was not routed**, now referenced from `CTRL-MANIFEST` |
| **C-01…C-14 completion items, 122 open/partial checklist markers** | `docs/contract-driven-metadata-schema-agnostic-completion-plan.md` — **was unclassified**, now `CTRL-ADOPTION` §4a |
| Keyset pagination, provider capability registry, SDMX/Parquet/ZIP conformance, second-provider proof | `docs/api-modernization-capability-gap.md` — **was unclassified**, now `CTRL-ADOPTION` §4a |
| Docker boundary: containers can close technical/staging acceptance but cannot create release authority, OIDC business policy, approved tenant boundary, official RPO/RTO or deploy permission | `docs/platform-capability-and-architecture-audit-2026-09-13.md` |
| OIDC/JWKS/ABAC, OTel/SLO, backup/DR, per-tenant quota, legacy `dynamic/pages` retirement | same audit + `docs/decisions/ADR-tenant-scoped-authorization.md` + AIR register |
| Host/upstream directory blueprint (`.agents/kit` is a pinned copy, never upstream source) | `/CLAUDE.md` host instructions + `.agents/project/project.json` |

**The defect the audit exposed was routing, not loss.** Three documents holding live
platform obligations were unreachable from the control plane: two carried no
classification at all, and none was referenced by `CTRL-MANIFEST`. Knowledge that exists
but cannot be found is functionally lost the moment the person who remembers it leaves.
Fixed by classification and reference — **no content was copied**, because these documents
remain the canonical owners of those obligations.

### 1.5 Conflicts — serving surface

| ID | Current statement | Confidence | Status |
|---|---|---|---|
| CF-027 | No adapter overrides `aggregate`, so the interface default runs `read(page=1, limit=1000)` then `ContractAggregationPlanner` in memory. **Every aggregation is computed over at most 1000 rows, silently** — no total, no truncation flag. `total()` by contrast issues a real `COUNT_BIG(*)`. Reachable on both page routes. | HIGH | IMPLEMENTATION-DEFECT |
| CF-028 | `metricCode`, `carrierCode`, `periodFrom`, `periodTo`, `ageGroup` are accepted by `GET /platform/pages/{id}/data` and **silently discarded** — their only consumer copies them into a new `ReadContext` that ignores them. **No loss** on `POST .../query`, where they remain in `filters` and are compiler-enforced. | HIGH | IMPLEMENTATION-DEFECT (GET route) |
| CF-030 | Two chart authorities resolve liveness through the two different RC-001 models: `visualization_definition` → membership (Model A); `statistical_chart` → status (Model B). **The same product's data can yield different numbers.** | HIGH | ARCHITECTURAL-DEFECT |
| CF-005 | `CanonicalObservationWriter` persists declared observation/measure attributes to `statistics.observation_attribute`; `CanonicalObservationReader` **does not read that table**. Contract-declared values disappear from chart/read/export surfaces. | HIGH (verified lines 69-95) | IMPLEMENTATION-DEFECT |

### 1.6 Inherited findings NOT re-verified this engagement

Preserved verbatim in intent; **status INHERITED-UNVERIFIED**. A future session must verify
before acting.

| ID | Inherited statement (abridged) |
|---|---|
| ~~CF-001~~ | **REJECTED** — the second file is titled **ADR-012**; no collision exists. Replaced by **CF-036**. |
| CF-002 | **REVISED** — the register carries its own dated refinement (line 316: v1 is `FULL_SNAPSHOT` only). Not a contradiction; the residual is an unrestated Q11 table row. |
| CF-010 | **CONFIRMED, sharper** — `provider_capability` carries one quantitative limit (`max_page_size`); no precision/scale/column/name dimension. Feeds **CF-038**. |
| CF-011 | **CONFIRMED** — `022` mixes 12 DDL guards with 41 KIDS references. `109` is a generic deny-by-default mechanism plus 2 seed rows; the inherited framing overstated `109`. |
| CF-015 | **CLOSED — NOT SUPPORTED + REQUIRED.** `Attachment` carries the dimension list (group shape is expressible), but `ConstantBinding` holds **one value per component** with no per-combination map. Required by domain evidence (units vary by INDICATOR). → REQUIRED-BUT-MISSING capability. |
| CF-017 | **REJECTED (historical)** — the R8 navigation pane is **populated** (`MSysNavPaneGroups` 34, `GroupCategories` 3 incl. a `Custom` category, `GroupToObjects` 37, `ObjectIDs` 45), and `AccessAuthoringAdapter:253-275` writes all four system tables plus the `NavPane Category` property. Fixed by commit `0ba151b`. Classification: **PROVIDER / UI PRESENTATION METADATA** — write-only, consumed by no platform code, affects no semantics. |
| CF-018 | **CONFIRMED** — suffix match on the wire ref, plus a `dataset_code` lookup with no product scoping: two independent failure modes. |
| CF-019 | **CLOSED — SAFE NORMALIZATION.** Four application sites; BoundedText is bounded to 1..255 and length-checked after trimming. Known non-material behaviour: empty→null collapses "typed a space" with "left blank"; no requirement distinguishes them. |
| CF-020 | **CONFIRMED** — 113 `.sql` files present, 108 `executeAndRecord` calls, **6 unregistered** (`000`, `037`, `039`, `045`, `047`, `056`). `056_activate_kids_r8_supersede_legacy.sql` is architecturally decisive: a clean rebuild never performs R8 activation. **Third state added by the audit:** `alreadyEffective()` adopts a migration without execution when its effect is already present — so **UNREGISTERED ≠ NEVER EFFECTIVE**. |
| CF-012 | **CONFIRMED + REVISED** — **10** documented exceptions (011,020,021,055,065,069,070,079,080,097), each justified in comments, one with a reproducible proof, and **all other drift fails closed**. The accurate defect: the allowlist is Java code, not a governed artifact. (The audit's own earlier count of 5 was wrong.) |
| CF-013 | `platform.contract_page_binding` created in both 054 and 057 — **refined**: the third writer is literally `057_contract_page_binding_idempotent_repair.sql`, supporting "historical idempotent repair" |
| CF-014 | Approved contract has no withdrawal path — **refined**: `ROLLED_BACK` is declared in `ContractWorkflow.State` but **no method produces it**; a dead state, same anti-pattern as CF-025's `'APPROVED'` |
| CF-020 | Migration registration is a second manifest — **refined**: 113 files present, 108 `executeAndRecord` calls, **6 unregistered**, of which `056_activate_kids_r8_supersede_legacy.sql` is architecturally decisive |

### 1.7 Quality findings

| ID | Statement | Location |
|---|---|---|
| QF-001 | Entire `classification_item` ACTIVE set loaded per read; N+1 `observation_dimension` query per observation | `CanonicalObservationReader:63,79` |
| QF-002 | Rows grouped by `Map.toString()` as a semantic key | `CanonicalObservationReader:88` |
| QF-003 | **REVISED, downgraded.** The global default-contract resolution is a **declared, governed exemption** (`@TenantScopeExemption(DEFAULT_CONTRACT)` with reason + enforcing interceptor), not a tenancy hole. Residual: the platform retains a single-primary-site assumption worth an explicit SCH-003 decision. **Open sub-item:** no register of `TenantScopeExemption` instances was found. | `CanonicalPageDataController` |
| QF-004 | `contractPolicy.requireGoverned(rid)` runs *after* the data query executes | `CanonicalPageDataService:53,62` |
| QF-005 | `CASE WHEN '<field>'='value_decimal'` — a Java conditional emitted as a SQL literal comparison; decimal→`nvarchar(128)` round-trip | `ContractPhysicalQueryService:159` |
| QF-006 | The canonical statistical projection reads dimension values from `raw.source_record.payload_json` via `JSON_VALUE`, bypassing `statistics.observation_dimension` which `CanonicalObservationReader` uses — a second read-model divergence beneath CF-022 | `ContractPhysicalQueryService:158-161` |
| QF-007 | Outbox payload assembled by string concatenation rather than the injected `ObjectMapper`; not injectable today (validated SHA-256 hex + longs) | `PublicationControlStore:18` |
| QF-008 | `aggregate` selects the value field by hardcoded name convention (`"value"` / `"numericValue"`) in a schema-agnostic engine | `ContractPageExecutionAdapter:20-21` |
| QF-009 | `ChartService.stored()` catches `RuntimeException` per row and silently drops unparseable declarations — no error, no log, no operator signal | `ChartService:124` |
| QF-010 | `chart()` revalidates the spec against the approved plan; `charts()` does **not** — the list advertises charts the detail endpoint refuses | `ChartService:57-75` |
| QF-011 | `declare()` does UPDATE-then-conditional-INSERT with no `@Transactional`; bounded by `uq_statistical_chart` to a constraint violation rather than duplication | `ChartService:86-94` |
| QF-012 | **FULLY CHARACTERIZED.** Assertions bind to the schema via `ORDER BY s.revision DESC`, ignoring the revision they were authored against. Combined with CF-033, **one package can retroactively change the meaning of another package's assertions.** Required invariant: INV-014. | `PlatformAccessIngestionService:118` |

### 1.8 Decisions

| ID | Decision | Confidence |
|---|---|---|
| **DR-001** | Canonical chart authority = `statistical_chart`'s **grammar-based declaration** generalised beyond the statistical family + `visualization_definition`'s **membership-based resolution**. Dispositions: core `ChartDefinition` DEPRECATE→DELETE; Managed `__gs_chart` MIGRATE transport / DELETE destination; `visualization_definition` MERGE; `statistical_chart` EVOLVE. **Must carry over the declaration lifecycle** (`status`), which `statistical_chart` lacks. Ordering: RC-001 first. | HIGH direction / MEDIUM-HIGH generalisation |
| **DR-002** | Approved contracts and charts are **read-governed resources**. Read requires ordinary read authority + tenant scope; AUTHOR/APPROVE/IMPORT only for mutation. Enforcement stays at the use-case boundary; the HTTP annotation is defence in depth. **Precondition: CF-031.** | HIGH |
| **DR-003** | The package composer is a **derivation port, not a producer**. KEEP and EVOLVE; do not delete as dead code. Runtime reachability: **none** (referenced only by its own test). Classification for PA-001: TRANSPORT/DERIVED REPRESENTATION, not competing authority. Ordering: CF-024a first. | HIGH |
| **DR-004** | Keep site contract and statistical contract as **two distinct M1 concepts**; do not merge. Link them explicitly: a `STATISTICAL`-family site-contract dataset must reference the approved statistical revision by identity **and digest**, enforced through the existing pluggable `ContractApprovalCheck` list. | HIGH separation / MEDIUM-HIGH linkage |
| **DR-005** | The **JSON Schema is the canonical published shape authority (M2)**; `ContractDraftParser` is its executable implementation; equivalence enforced by a **bidirectional conformance corpus bound into CI**, not by code generation. Rejected: generate-parser-from-schema (standards profile says JSON Schema does not generate an engine), generate-schema-from-parser (inverts contract-first), delete. | HIGH direction; parity completeness inherently bounded |
| **DR-006** | Exact decimal only; floating point never in the canonical path. Declaration ≤ (28,16) bounded by the weakest required provider; canonical storage `DECIMAL(38,16)` as a strict superset. **The envelope moves into the provider capability registry** (closes CF-010). `28,10` widening is lossless but **truncated values require reload from the source artifact**. **Constraint on CF-027:** SQL pushdown must handle `SUM` overflow over `DECIMAL(38,16)`. One exact-decimal serialization required (JSON string). | HIGH |
| **DR-007** | A physical package is **SELF-DESCRIBING, never SELF-AUTHORITATIVE**. Rules: require `status='APPROVED'`; prove correspondence by **deterministic semantic equivalence** (reuse the `PackagePlan` digest); package lifecycle states never map onto canonical states (**remove `state()` as a concept**); a package may *reference* a metadata schema, never *define* one; validation completes before any canonical mutation, in one transaction. **Interim posture: reject package-supplied schema definitions until a producer exists.** | HIGH |
| **DR-008** | Contract defines *what must exist and what it means*; a declared **physical pattern** defines *how that semantic family is realized for a provider*; **provider capabilities** constrain faithful realization; the adapter **executes**. Confirmed by `AccessAuthoringAdapter` (EXECUTOR, no site literals). | MEDIUM-HIGH — only one of six families proven |

**Prior agent's DR-REC-001…007** (preserve M3→M0; converge rather than add abstraction;
provider semantics outside canonical semantics; expand/migrate/contract; do not publish or
remove during discovery; attributes are canonical behavior; release NOT READY) are all
**consistent with** DR-001…DR-008 and remain valid.

### 1.9 Invariants

| ID | Statement | Status |
|---|---|---|
| INV-001 | M0 conforms to approved M1, conforming to M2 grammar and M3 semantics | CONFIRMED — **unenforceable above M1 (RC-003)** |
| INV-002 | One business meaning, one canonical owner | CONFIRMED — violated at CF-025, CF-033 |
| INV-003 | Closed grammar, open registry; no site-name branching in generic core | CONFIRMED |
| INV-004 | Exact identity and reference pinning; approvals bind the resolved closure | CONFIRMED intent |
| INV-005 | Lossless statistical value semantics | CONFIRMED — see DR-006 |
| INV-006 | One observation per dimension tuple and measure; measure is not a dimension key component | CONFIRMED — upheld in the adapter |
| INV-007 | Null value requires explicit status; zero is a real value | CONFIRMED — **enforced by the release gate**, with the rule stated in a source comment |
| INV-008 | Publication requires explicit governed approval of exactly what is published | CONFIRMED — violated on the read side (CF-021) and write side (CF-023) |
| INV-009 | Tenant ownership and deny-by-default | CONFIRMED — well tested |
| INV-010 | Applied migrations are append-only; checksum mismatch fails closed | PROBABLE / authority conflict (CF-012) |
| INV-011 | Provider artifacts are adapters; canonical semantics stay provider-neutral and server-authoritative | CONFIRMED — violated by CF-033/CF-034 |
| INV-012 | Legacy removal is gated on consumer inventory, parity, evidence and rollback | CONFIRMED |
| **INV-013** | A derived plan (`PackagePlan`, `SemanticPlan`, compiled query plan) may be cached or digested but **must never be persisted as an artifact read *instead of* the declaration it derives from** | CONFIRMED — honoured by the composer and by `approvedPlan` |
| **INV-014** | An assertion is bound to the **exact schema revision** it was authored and validated against; that binding is immutable. A newer revision creates a new binding only through governed migration with re-validation | REQUIRED-BUT-MISSING (QF-012's fix) |

### 1.10 Atomic migration set

| ID | Members (no member independently safe) | Preconditions |
|---|---|---|
| **AMS-001** | 1. DR-004 cross-context link · 2. CF-024a governed producer · 3. CF-032 ingestion-contract revision/governance · 4. a governed publication path reachable by statistical datasets · 5. CF-023 elimination (bounded membership + gate-scope alignment + rollback repair) | RC-001 answered; **protection must precede every member** — the publication writer/service/store are entirely untested |

Three smaller sequences were attempted and rejected; the second ("loosen
`assertApprovedContract`") was identified as the most dangerous because it weakens the only
contract check guarding publication in order to fix a publication defect.

---

## 2. REQUIREMENT LEDGER (human architectural intent)

These are **requirements**, not implementation evidence. They survive regardless of what
the repository currently does.

| ID | Requirement | Current state |
|---|---|---|
| REQ-001 | Contract-driven architecture | partially realized; broken above M1 (RC-003) |
| REQ-002 | Metadata-driven execution where justified | realized in serving dispatch and authoring policy |
| REQ-003 | Semantic-first design | realized in the statistical compiler |
| REQ-004 | Schema agnosticism **without semantic erasure** | upheld — families are typed, not EAV |
| REQ-005 | Provider agnosticism where justified | partial — provider capability registry exists; envelope not yet in it (DR-006) |
| REQ-006 | Typed and relational modelling | upheld |
| REQ-007 | **No naive EAV / key-value architecture** | **PARTIAL (audit correction)** — upheld for statistical/relational/classifier; **violated for entity properties** (CF-039). The earlier 'upheld' rested on the Access *transport*, not the canonical store. |
| REQ-008 | No uncontrolled JSON/blob semantics | **VIOLATED (audit)** — every declared entity field lives in `entity_record.payload_json`; read back via `JSON_VALUE`. CF-039. |
| REQ-009 | No runtime semantic guessing | violated at QF-008 (hardcoded value-field names) |
| REQ-010 | Maximum SAFE automation | partial — see §10 |
| REQ-011 | Minimum necessary manual input | partial — declaration entry is migration-only |
| REQ-012 | Simple data population workflow | realized for statistical authoring |
| REQ-013 | Simple and explicit relations | upheld — `__gs_relation`, contract relations |
| REQ-014 | Reusable physical patterns | PARTIAL — pattern is implicit in Java (DR-008) |
| REQ-015 | Different sites may have different tables **without different architecture** | evidence-supported; unproven for 5 of 6 families |
| REQ-016 | Reusable statistical physical pattern | PARTIAL — adapter is clean; registry and envelope would bite a second site |
| REQ-017 | **Law of No Privileged Dimension** | upheld in the adapter and in INV-006 |
| REQ-018 | Strong classifier / codelist / hierarchy modelling | partial — proposal path correct, promotion absent (CF-016) |
| REQ-019 | Dimensions, metrics, measures, units, grain, observations, attributes, status as first-class semantics | realized; attributes not round-tripped (CF-005) |
| REQ-020 | One canonical dataset semantics must serve TABLE / CHART / API / EXPORT / DASHBOARD **without parallel semantic models** | violated — four chart mechanisms, two liveness models (DR-001, CF-030) |
| REQ-021 | Access is a **contract executor / physical realizer** | confirmed for `AccessAuthoringAdapter` |
| REQ-022 | Access may be SELF-DESCRIBING, never SELF-AUTHORITATIVE | violated at the metadata plane (DR-007, CF-033/034) |
| REQ-023 | Deterministic generation | upheld — content digests, order-independent |
| REQ-024 | Versioned contracts | realized (site + statistical) |
| REQ-025 | Versioned physical patterns | MISSING |
| REQ-026 | Versioned generators where required | MISSING — an artifact cannot answer "which generator made me" |
| REQ-027 | Explicit provider capabilities | partial — registry exists, lacks scale dimension (CF-010) |
| REQ-028 | Round-trip traceability | BROKEN — no digest binds artifact to contract |
| REQ-029 | Strong relational integrity | upheld in schema |
| REQ-030 | Safe schema evolution | partial — CF-012, CF-020, unregistered `056` |
| REQ-031 | Performance and large-data behaviour | partial — CF-027, QF-001 |
| REQ-032 | Transactional correctness | partial — CF-023, `materializeMetadata` has none |
| REQ-033 | Idempotency | strong where implemented (approval replay, checksum idempotency, `MERGE`) |
| REQ-034 | Tenant / security boundaries | strong and well tested |
| REQ-035 | Observability | partial |
| REQ-036 | Testability | partial — the critical path is untested (§ 1.11 of the checkpoint, 23.39) |
| REQ-037 | Replaceability | partial |
| REQ-038 | Composability | partial |
| REQ-039 | 10+ year evolvability | blocked by RC-003 |
| REQ-040 | Easy onboarding of unrelated future sites/datasets/providers | **blocked by RC-003 / CF-024a** |
| REQ-041 | **Architecture must become simpler as capability becomes stronger** | currently violated — six architecture generations coexist |

---

## 3. TWO PROVEN INPUT PATTERNS — do not collapse

| | PATTERN A | PATTERN B |
|---|---|---|
| Shape | authoritative declaration → deterministic derivation → generated realization | external evidence → DRAFT proposal → validation/review → governed promotion → canonical registry |
| Proven by | `AccessAuthoringAdapter` | `AccessClassifierProposalImportService` |
| Use when | content is completely derivable from approved authority | external content carries semantic decisions that cannot safely be derived |
| Evidence of correctness | no site literals; consumes only the compiled plan; EXECUTOR role | writes only `classifier_proposal`; hardcoded `'DRAFT'`; no UPDATE branch; read-only artifact open |

**CF-033/CF-034 are a case of the wrong pattern applied**: metadata schemas and subjects
arrive from outside and are written straight into canonical state, when they belong in
Pattern B. **Their remediation is reuse of an existing in-repository implementation, not
new design.**

### PATTERN C — defence in depth for immutable semantic identity
*(added by the completeness audit, 2026-09-20)*

| | PATTERN C |
|---|---|
| Shape | application-level content digest over a canonical form **+** a database trigger enforcing write-once on that digest |
| Proven by | `CanonicalJson.digest` + `platform.tr_statistical_reference_definition_immutable` (migration `108_statistical_reference_definition_digest.sql`, `THROW 51204, 'The definition of a statistical reference is written once'`) |
| Use when | an identity must be stable across time and no layer may silently redefine it |
| Directly reusable for | **INV-014** (assertion ↔ schema revision binding) and **DR-007**'s package↔contract correspondence requirement |

Three proven patterns now exist in-repository for problems the plan must solve. **None
requires invention.**

---

## 4. AUTHORITY REGISTER

> **Governed form: `GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md` §2 (CAD-02).**
> The table below is the recovery-time snapshot that seeded it and is retained as evidence.
> For any question of *who is authoritative*, the doctrine's registry is current.

Legend: **A** authority · **D** declaration · **DR** derived representation · **P** proposal ·
**PR** physical realization · **T** transport · **C** cache · **V** view/projection.

| Responsibility | Kind | Current authority | Intended authority | Current writer | Current reader | Defect |
|---|---|---|---|---|---|---|
| Platform Meta-Contract | A | **none** | highest semantic authority | — | — | RC-003 — 6 docs, 0 code |
| Control Plane Schema (contract creator) | A | **none** | creates/approves site contract | — | — | RC-003 |
| `data_product` | D | migrations | governed producer | tenant UPDATE only | everything | RC-003 |
| Site contract (`site_contract_*`) | D | **migrations** | governed producer + approval | 2 UPDATEs (status) | serving, composer, artifact resolver | CF-024a |
| Statistical contract | D | `ContractWorkflow` ✔ | same | `statistical_contract_draft` | generation, ingestion, charts | CF-004 (availability) |
| Ingestion contract | D | `StatisticalBindingService` | governed | INSERT without revision | publication gate | CF-032 |
| Classifier registry | D | **migrations** | governed promotion | none | serving, charts | CF-016 |
| Classifier proposals | P | `AccessClassifierProposalImportService` ✔ | same | `classifier_proposal` | **nobody** | CF-016 (no promotion) |
| Metadata schema | D | **Access package** ✘ | governed producer | `materializeMetadata` | ingestion | CF-033 |
| Metadata subjects | D | **Access package** ✘ | governed | `materializeMetadata` (`'APPROVED','PUBLIC'`) | metadata assertions | CF-034 |
| PackagePlan | DR | derived, never persisted ✔ | same | — | **nobody (unwired)** | DR-003 |
| SemanticPlan | DR | derived, digest-verified ✔ | same | — | generation, ingestion, charts | — |
| Physical plan | DR | statistical planner | same | — | `AccessAuthoringAdapter` | — |
| Physical pattern | D | **implicit in Java** | declared metadata | — | — | REQ-025, DR-008 |
| Provider capabilities | D | `platform.provider_capability` | same + scale dimension | — | compiler | CF-010, DR-006 |
| Access artifact | PR/T | self-describing ✔ | self-describing, not authoritative | `AccessAuthoringAdapter` | readers | CF-033/034, no digest |
| Canonical data | — | Data Plane tables ✔ | same | writers | readers | — |
| Snapshot lifecycle | — | **unowned** | one owner | 5 writers | 6 readers | CF-025 |
| Publication | A | `publication.snapshot` + `snapshot_member` | sole liveness authority | `DataPlanePublicationWriter` | 4 membership readers | CF-023 |
| Serving | V | contract page binding + serving policy | same | — | `frontend/kids` | CF-021, CF-027, CF-028 |
| Chart declaration | D | **four mechanisms** | one (DR-001) | 4 paths | 4 paths | CF-030 |

---

## 5. LINEAGE REGISTER

### 5.1 Forward

```text
Platform Meta-Contract            ASPIRATIONAL   (6 docs, 0 code, 0 schema)
  -> Control Plane Schema         ASPIRATIONAL   as contract creator; DDL is real
  -> Site Contract                MIGRATION-ONLY (CF-024a)
  -> Approved revision            VERIFIED       (SiteContractRevisionApprovalService)
  -> PackagePlan (+digest)        VERIFIED       (unwired consumer)
  -> Physical pattern             INFERRED       (implicit in Java)
  -> Provider capabilities        VERIFIED
  -> Generator                    VERIFIED       (AccessAuthoringAdapter, 7/9 __gs_*)
  -> __gs_metadata / _schema      KNOWN GAP      (no identified producer)
  -> Access artifact              PARTIAL        (built by a different generator version)
  -> Source data (human fill)     VERIFIED
  -> Reader + validation          VERIFIED       (parse + package-internal only)
  -> Correspondence check         MISSING        (DR-007)
  -> Ingestion                    VERIFIED       (load mapping is server-bound)
  -> Canonical data               VERIFIED
  -> Snapshot                     VERIFIED       (but CF-025 lifecycle unowned)
  -> Release gates                VERIFIED       (strong; bypassed by statistical entry state)
  -> Publication                  BROKEN         (CF-023, CF-032)
  -> Membership                   VERIFIED       (not consulted by 3 readers — CF-022)
  -> Serving                      PARTIAL        (CF-021)
  -> Query / aggregation          PARTIAL        (CF-027, CF-028)
  -> Presentation                 PARTIAL        (CF-030, DR-001)
  -> Consumer (frontend/kids)     VERIFIED
```

### 5.2 Backward (from a served value)

Served value → serving query → `dataset_snapshot.status` (**not** membership — CF-022) →
snapshot → `statistics.observation` → `raw.source_record` → Access column → `__gs_field`
declaration → `site_contract_*` row → **STOPS: no Control Plane Schema artifact exists**.

The backward chain is complete and explainable to `site_contract_revision`. Above it the
edge is **MISSING IMPLEMENTATION**, proven — not unknown.

---

## 6. CONTRACT FAMILY REGISTER

| Family | Means | Authored by | Approved by | Consumed by | Versioning | Producer exists? |
|---|---|---|---|---|---|---|
| Site contract | how a site's datasets are presented and served | **nobody** (migrations) | `SiteContractRevisionApprovalService` | serving, composer, artifact resolver | integer `revision` per code | **NO** |
| Statistical contract | what a statistical dataset means | `ContractWorkflow` over HTTP | `ContractWorkflow.approve` (four-eyes, digest-bound) | generation, ingestion, charts | CAS `version` per draft; digest per revision | **YES** |
| Ingestion contract | source/admission declaration | `StatisticalBindingService` | migrations only (`_revision`) | publication gate | `contract_revision` (unset by the binder) | partial |
| Generic contract structure | physical table/field/index registry | migrations | lifecycle status | query execution | `revision` | **NO** |
| Metadata schema | shape of metadata assertions | **Access package** | package's own `approval_state` | metadata assertions | `schema_revision` | inverted |
| Package plan | approved site contract in authoring shape | derived | n/a | nobody (unwired) | content digest | derived |
| Chart declaration | what may be drawn | four mechanisms | varies | four surfaces | varies | see DR-001 |

**DR-004 preserved: site contract and statistical contract are NOT collapsed.**

---

## 7. PHYSICAL PATTERN REGISTER

| Mechanism | Responsibility | State |
|---|---|---|
| `artifact_profile` (`__gs_package`) | names the physical realization profile | EXPLICIT (a value, no model) |
| `sourceProfile` / `supportedProfiles` | selects the provider profile | EXPLICIT |
| `ProviderCapabilities` / `platform.provider_capability` | declares provider limits | EXPLICIT, incomplete (CF-010) |
| `PhysicalPlan` + physical planner | provider-shaped realization of a semantic plan | EXPLICIT (statistical only) |
| `PackagePlan.Family` | closed family set (ENTITY, REFERENCE, RELATION, RAW, STATISTICAL, GEO) | EXPLICIT |
| `PageFamily` | the same families on the read side | EXPLICIT (dispatch is vacuous — 7 identical adapters) |
| `AuthoringPolicy.Group` | four package groups from family + role | EXPLICIT, pure, site-agnostic |
| Provider presentation output (Access Navigation Pane) | how the realized artifact is organised for the author | **EXPLICIT and derived** — `AccessAuthoringAdapter.navigationGroups`; write-only, non-semantic |
| **Declared physical pattern per family** | how a family is realized for a provider | **MISSING** — implicit in Java |

**DR-008 preserved.** The missing explicit pattern model is recorded, not designed.

---

## 8. LEGACY / DUPLICATION REGISTER

| Mechanism | Classification | Disposition |
|---|---|---|
| Domain-specific legacy controllers (11, Georgian routes) | OBSOLETE ARCHITECTURE | DEPRECATE → DELETE after consumer migration (INV-012) |
| `MSSQLToAccess` | INCORRECT IMPLEMENTATION + COMPATIBILITY REQUIREMENT | fix identifiers; keep until `geostat-system-app` migrates |
| `/test/*` demo payloads | COMPATIBILITY REQUIREMENT | keep until control-plane UI migrates |
| Managed Access v1 (`__gs_chart`, `__gs_chart_filter`) | OBSOLETE (destination) + COMPATIBILITY (transport) | MIGRATE transport / DELETE destination |
| `ManagedImportExecutionService` + `AccessFileImporter` + `ImportStrategyFactory` / `MySqlImportStrategy` / `SqlServerImportStrategy` / `DatabaseImportStrategy` | OBSOLETE — Managed v1 / legacy DB import (PASS 2B sweep) | DEPRECATE → DELETE after consumer migration |
| Semantic Access v3 | CURRENT | KEEP |
| core `ChartDefinition` + `DynamicChartService` | OBSOLETE | DEPRECATE → DELETE |
| `visualization_definition` | INCORRECT declaration / CORRECT resolution | MERGE (DR-001) |
| `statistical_chart` | LEGITIMATE SPECIALIZATION | EVOLVE (DR-001) |
| `PageDataAdapterRegistry` | ABANDONED EXPERIMENT (except `ReadContext`) | decide: complete or remove |
| `ContractPageExecutionRegistry` (7 identical adapters) | FRAMEWORK PROLIFERATION | complete or collapse — it is the SCH-003 extension point |
| `relational` ENTITY adapter | DEAD CODE | remove with the above decision |
| `service/platform/packaging` composer | LEGITIMATE, UNWIRED | KEEP and EVOLVE (DR-003) |
| Statistical loader vs PackageRun pipeline | PARTIAL MIGRATION | converge (AMS-001 adjacent) |
| `ROLLED_BACK` state, `'APPROVED'` snapshot status, `'PREPARED'` default | DEAD VOCABULARY | remove with lifecycle ownership (CF-025) |
| Java parser vs JSON Schema | TWO REPRESENTATIONS, one grammar | DR-005 |

**Nothing is deleted. All entries are dispositions for the future plan.**

### Elimination governance (GOV-R1 … GOV-R5, materialized 2026-09-20)

Full rule text lives in the Layer Coverage ledger. In force here:

- **GOV-R1 completion rule:** `MIGRATED ≠ DONE`. Done requires
  `MIGRATED + LEGACY ELIMINATED OR EXPLICITLY JUSTIFIED + VERIFIED`.
- **GOV-R3 parallel architecture:** coexistence is permitted only as legitimate
  specialization or explicitly required compatibility; temporary migration coexistence
  **must carry an exit condition**. A canonical mechanism beside an obsolete *active* one
  is not completion.
- **GOV-R4 knowledge-preserving elimination:** account for unique business behaviour,
  semantics, data, migration history, compatibility behaviour, provider knowledge, failure
  handling and operational knowledge **before** `DELETE`/`REPLACE`.
- **GOV-R5 consumer-first sequence:** canonical replacement → behaviour/data coverage →
  consumer migration → compatibility verification → regression verification →
  repository-wide reference check → disable → verify no runtime dependency → delete →
  verify again.
- **GOV-R6:** documentation and ADRs are in elimination scope; stale authoritative-looking
  docs require `HISTORICAL`/`SUPERSEDED` classification, and CF-036-class incorrect
  decision references must be corrected.
- **GOV-R7:** every material test must be classified `CANONICAL BEHAVIOR PROTECTION` /
  `MIGRATION COMPATIBILITY` / `LEGACY BEHAVIOR` / `INCORRECT EXPECTATION` / `OBSOLETE`.
- **GOV-R8:** `HISTORICAL MIGRATION RECORD ≠ ACTIVE ARCHITECTURAL MECHANISM`. Historical
  migrations are not deleted for cleanliness, but clean build and upgraded database must
  eventually converge (blocked today by CF-020 / unregistered `056`).

---

## 9. MISSING COMPONENT REGISTER

| Component | Responsibility | Why required | Confidence |
|---|---|---|---|
| **Control Plane Schema producer** | create and approve site-contract declarations | REQ-040, SCH-003, CF-024a; the composer and all consumers are blocked without it | HIGH |
| **Product registration producer** | create `platform.data_product` | onboarding requires it; today migration-only | HIGH |
| **Classifier promotion path** | `classifier_proposal` → canonical `classification_*` | CF-016; proposals accumulate with no consumer | HIGH |
| **Metadata schema producer** | governed creation of `metadata_schema` | CF-033; today an Access package does it | HIGH |
| **Declared physical pattern model** | per-family realization rules as metadata | REQ-014, REQ-025, DR-008 | MEDIUM-HIGH |
| **Package↔contract correspondence check** | deterministic semantic equivalence before canonical influence | DR-007, REQ-028 | HIGH |
| **Generator identity/version in artifacts** | answer "which generator made me" | REQ-026 | HIGH |
| **Governed publication path for statistical datasets** | close CF-032 | AMS-001 member 4 | HIGH |
| **Contract withdrawal operation** | retire a known-wrong approved contract | CF-014; `ROLLED_BACK` is declared but unreachable | HIGH |
| **Legacy chart converter** | migration 110's stated behaviour | CF-029 | HIGH |
| **Grammar parity corpus** | prevent schema/parser drift | DR-005 | HIGH |
| **Assertion↔schema revision binding** | INV-014 | QF-012 | HIGH |
| **Declaration lifecycle on `statistical_chart`** | a chart must not go live on declaration | DR-001 falsification | HIGH |
| **Per-combination constant value declaration** | declare a constant that varies by dimension combination (e.g. unit per INDICATOR) without per-observation repetition | CF-015; checkpoint §14 item 7 | HIGH |
| **Typed canonical representation for entity properties** | enforce declared type/nullability/uniqueness below the contract | CF-039 | HIGH |

All are **MISSING IMPLEMENTATION** with recovered responsibility and boundaries — not
unknown architecture.

---

## 10. AUTOMATION BASELINE (current state)

| Operation | State |
|---|---|
| Contract resolution (statistical) | AUTOMATIC |
| Contract resolution (site) | MIGRATION-DRIVEN |
| Physical plan creation | AUTOMATIC (statistical only) |
| Table / column / key / relation creation in Access | AUTOMATIC (`AccessAuthoringAdapter`) |
| Type mapping | AUTOMATIC, provider-aware |
| Classifier structure creation in Access | AUTOMATIC (codelist data written) |
| Access Navigation Pane organisation | **AUTOMATIC** — derived from the semantic plan (`navigationGroups`), a positive instance of REQ-010/011/012 |
| Classifier registry growth (server) | **MIGRATION-DRIVEN / MISSING** |
| Metadata creation | **PACKAGE-DRIVEN (inverted)** |
| Package identity | PARTIAL — values present, no digest |
| Provenance | **MISSING** |
| Digest | **MISSING** at the artifact boundary; present for plans |
| Data population | MANUAL (by design — the authoring workflow) |
| Package validation | PARTIAL — internal only |
| Declaration authoring | **MIGRATION-DRIVEN** |
| Publication | AUTOMATIC but INCORRECT (CF-023) |
| Archive retention purge | **AUTOMATIC** — lease-guarded scheduled purge, approved one-year window |
| Object-store lifecycle alignment | **CONVENTION ONLY** — required by Javadoc, enforced nowhere |
| SDMX-CSV export | **AUTOMATIC when wired**; unwired today |
| Scheduled job concurrency control | **AUTOMATE — already done**: 10/10 jobs lease- or CAS-guarded |
| Corrected reprocessing | **REQUIRED-BUT-MISSING** — no governed path; checksum idempotency blocks replay |
| Clean-build/upgrade equivalence check | **ASSIST** — `migration-chain-fresh-replay.sh` exists; no current passing evidence |

---

## 11. DATA / PRESENTATION SEPARATION (REQ-020)

Canonical dataset semantics must serve TABLE, CHART, API, EXPORT and DASHBOARD **from
shared semantics plus presentation declarations** — never from parallel semantic models.

Current violation: four chart mechanisms, two liveness models (CF-030). DR-001 is the
recorded direction; the visualization architecture is **not** designed here.

---

## 12. UNRESOLVED CONTRADICTIONS

**GOV-R2 closure rule (materialized 2026-09-20):** every material contradiction must
eventually reach `RESOLVED — canonical side selected` / `RESOLVED — semantics merged` /
`RESOLVED — legitimate specialization with an explicit boundary` / `SUPERSEDED` /
`REMOVED`. **No material unresolved contradiction may survive final rehabilitation.**

| # | Contradiction | Resolution |
|---|---|---|
| 1 | CF-007 vs CF-023 both describe publication scope | **RESOLVED** — CF-007 is the symptom, CF-023 the mechanism; CF-007 marked REVISED |
| 2 | RC-002 vs RC-003 both explain migration-carried declarations | **RESOLVED** — RC-002 is the observation, RC-003 the cause; RC-002 reframed, not deleted |
| 3 | CF-024 vs CF-024a/b | **RESOLVED** — split; CF-024 alone is retired in favour of the pair |
| 4 | CF-004 original vs revised | **RESOLVED** — the "meaning can change" half is falsified; availability half retained |
| 5 | QF-003 original vs revised | **RESOLVED** — downgraded to an architectural observation |
| 6 | CF-016 original vs revised | **RESOLVED** — proposal half correct, promotion half absent |
| 7 | "Platform Meta-Contract" (docs) vs M3 (doctrine) | **RESOLVED** — the same concept under two names; mapped, neither discarded |
| 8 | `__gs_metadata_schema` (transport) vs `platform.metadata_schema` (registry) vs "Control Plane Schema" (intended layer) | **RESOLVED** — three distinct things; only the third is the missing upper layer |
| 9 | `AccessAuthoringAdapter` emits `__stat_contract`, absent from R8; R8 has `__gs_metadata*`, absent from the adapter | **UNRESOLVED (benign)** — evidence that R8 was built by a different generator version; recorded as KNOWN GAP, not a contradiction in the model |

**No contradiction is hidden. One remains open and is classified.**

---

## 13. ORPHAN REGISTER

| Orphan | Nature |
|---|---|
| `platform.classifier_proposal` | written, **never read** — proposals with no consumer |
| `service/platform/packaging/*` | complete, tested, **never wired** |
| `PageDataAdapterRegistry` | registry with no registrations |
| `relational` ENTITY adapter | constructed, never registered |
| `PageFamily.ROOT` | enum constant with no adapter — fails at request time |
| `ROLLED_BACK` (ContractWorkflow) | declared state, no producer |
| `'APPROVED'` (dataset_snapshot) | accepted on read, written by nobody |
| `'PREPARED'` (dataset_snapshot default) | schema default recognized by nobody |
| `__gs_metadata` / `__gs_metadata_schema` in R8 | physical structures with no identified producer |
| `statistical-contract-draft.schema.json` | published artifact referenced by one Javadoc |
| `platform.contract_revision_lifecycle` (migration 084) | not written by the approval path |
| `SdmxCsvExporter` | correct, deterministic SDMX-3 exporter with **zero callers** — fourth instance of the orphan class (composer, JSON Schema, `classifier_proposal`). Wiring it is blocked by CF-005. |
| `056_activate_kids_r8_supersede_legacy.sql` | present, **unregistered** — activation never replays |

---

## 14. KNOWN GAP REGISTER

| Gap | Why it is a gap and not an unknown |
|---|---|
| R8 metadata-plane provenance | exhaustive behavioural search: only two classes in the codebase create Access tables; responsibility, consumers, trust treatment and defects are all characterized |
| Platform Meta-Contract / Control Plane Schema | proven absent (6 docs, 0 code, 0 SQL); intended responsibility recovered |
| ~~`semanticForm()` / `physicalForm()` field lists~~ | **CLOSED by the completeness audit, 2026-09-20.** Both read line by line; every semantically relevant input is bound, ordering is explicit and deterministic, and in-place reference mutation is blocked by a DB trigger. CF-004 upgraded from INFERENCE to FACT. |
| Generator versioning | proven absent from `__gs_package`'s full column list |
| `TenantScopeExemption` register | no register found; mechanism itself is sound |

---

## 15. PENDING EVIDENCE INPUTS (not judged)

| Path | Purpose |
|---|---|
| `samples/kids-r8-resource-package-8.0.1.zip` | may carry a manifest with generator identity — the one thing that could upgrade `R8_GENERATION_PROVENANCE` from KNOWN-GAP |
| `samples/KIDS_PACKAGE_candidate_2.accdb` | comparison input |
| `platform/apps/geostat/backend/api/kids-portal-v1-canonical-r8-final.accdb` | already inspected read-only (24 tables); retained as reference behaviour |
| ~62 unclassified test files | protection-strategy completeness |
| `frontend/web` `[LEGACY]`, `backend/mobile` | legacy elimination sequencing |

**Explicitly not judged in this phase.**

**Judged in `PHASE-003`, 2026-09-21 — the three artifact inputs only.** The comparative audit
inspected them read-only and materialized the result in `AUD-COMPARATIVE` (identity, comparison,
diff, loss, falsification) and `AUD-CAPABILITY` (capabilities and dispositions). Outcomes that
change what this register recorded as pending:

- `samples/kids-r8-resource-package-8.0.1.zip` — **it does carry a manifest**, and the manifest
  binds contract code, revision and dataset, with a `sha256` per file. **It does not carry
  generator identity**, so `R8_GENERATION_PROVENANCE` stays a `KNOWN-GAP` (§14) and is now also
  `CAP-M02`. 451 of 451 digests verified by recomputation.
- `samples/KIDS_PACKAGE_candidate_2.accdb` — inspected. It implements the per-combination
  constant declaration that §9 records as `REQUIRED-BUT-MISSING` (`CF-015`), through attribute
  attachment levels; see `AUD-COMPARATIVE` §4.1.
- `platform/apps/geostat/backend/api/kids-portal-v1-canonical-r8-final.accdb` — inspected, and a
  second file of the same name in `samples/` was found to be **semantically identical and
  byte-different**, which is direct evidence for `DR-007`/`REQ-028` rather than an inference.

The remaining two rows (test files, `frontend/web` and `backend/mobile`) stay **not judged**:
they are protection-coverage and elimination-sequencing inputs owned by `PHASE-009` and
`PHASE-011`, not representations of the data model.

---

## 16. CONSOLIDATION GATE

```text
KNOWN_FINDINGS_ACCOUNTED_FOR:      PASS   (CF 1-35, RC 1-3, DR 1-8, INV 1-14, QF 1-12,
                                           AMS-001, DR-REC 1-7 — no gaps, none invented)
HUMAN_REQUIREMENTS_ACCOUNTED_FOR:  PASS   (REQ-001 … REQ-041)
AUTHORITY_REGISTER:                COMPLETE
LINEAGE_REGISTER:                  COMPLETE (forward + backward, every edge classified)
CONTRACT_REGISTER:                 COMPLETE (7 families)
MISSING_COMPONENT_REGISTER:        COMPLETE (13 components)
KNOWN_GAPS:                        5, all classified
UNRESOLVED_CONTRADICTIONS:         1 (benign, classified — generator-version mismatch)
ORPHANS:                           12, all recorded
STALE_FINDINGS_REMOVED_OR_REVISED: CF-004, CF-006, CF-007, CF-013, CF-014, CF-016, CF-020,
                                   CF-024, QF-003, RC-002 — all revised with traceability;
                                   none silently deleted
PENDING_EVIDENCE:                  5 inputs, explicitly not judged

CONSOLIDATION_GATE:                PASS
RECOVERY STATUS:                   INCOMPLETE
```

**Why RECOVERY remains INCOMPLETE despite `CONSOLIDATION_GATE: PASS`.** Consolidation
reconciles what is known; it does not close what is outstanding. Three obligations remain:

1. the **regression/behavioral protection strategy** (raw material complete: 23.37, 23.39);
2. the **architectural/canonicality protection strategy and fitness functions**;
3. the **information-loss matrix** on non-numeric axes.

None is an unknown authority boundary. `DATA_CONTRACT_LINEAGE_GATE` remains FAIL solely
because of the **proven absence** of the upper stack (RC-003), which is compatible with
eventual recovery completion under the standard applied throughout this engagement.

**The next phase is an independent FINAL RECOVERY COMPLETENESS AUDIT** designed to find
what this work may have missed. This baseline is its input.

---

## 17. INHERITANCE NOTE FOR THE AUDIT

Two things the audit should probe hardest, because they are this engagement's weakest
links:

1. **Ten inherited findings were never re-verified** (§0.1). Any plan task resting on
   CF-001, CF-002, CF-010, CF-011, CF-012, CF-015, CF-017, CF-018, CF-019 or CF-020 needs
   independent evidence first.
2. **CF-004's closure rests on one declared inference** (§14). If `semanticForm()` omits a
   semantic field, the "no silent reinterpretation" conclusion weakens.
