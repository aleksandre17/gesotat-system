---
id: DOC-CAD
type: REGISTER
title: Canonical Authority & Anti-Parallelism Doctrine
status: ACTIVE
authority: CANONICAL
scope: Authority Registry (CAD-02) and rules CAD-01..CAD-18
owner: architecture recovery
created: 2026-09-20
updated: 2026-09-20
---

# GEOSTAT API — Canonical Authority & Anti-Parallelism Doctrine

**Created:** 2026-09-20, materialized into the repository under GOV-R9.
**Status:** CANONICAL governance. Permanent architectural invariant, not a cleanup task.
**Scope:** `platform/apps/geostat/backend/api` and every responsibility it participates in.

> **This doctrine did not exist during recovery batches 1-11E or PASS 1 / 2A / 2B.** It is
> not backdated. It is materialized now because the recovery produced the evidence that
> makes it necessary: four canonical registries with no producer (RC-003), two definitions
> of "live" (RC-001), seven ingestion mechanisms, four chart mechanisms, two grammars, and
> four correct-but-unwired components.

**Owns:** the Authority Registry (§2) and rules CAD-01 … CAD-18.
**Does not own:** findings, requirements, layer coverage, legacy dispositions, audit
evidence — see the artifact ownership table in the Consolidation header.

---

## 1. CORE INVARIANT — CAD-01

For every material architectural responsibility there **MUST** be exactly one explicitly
identified canonical authority.

```text
ONE RESPONSIBILITY
  -> ONE CANONICAL AUTHORITY
  -> ONE GOVERNED LIFECYCLE
  -> DERIVED REPRESENTATIONS
  -> JUSTIFIED EXECUTORS / PROVIDERS / SPECIALIZATIONS
```

**Never:** one responsibility → multiple independent sources of truth.

Multiple *representations* are permitted **only** when their relationship to the canonical
authority is explicit and either mechanically or procedurally governed.

### Why this invariant, in this repository

The recovery found the invariant violated in four distinct ways, and each is a template
for what CAD-01 forbids:

| Violation shape | Instance |
|---|---|
| Two authorities for one responsibility | RC-001 — `snapshot_member` **and** `dataset_snapshot.status` both decide what is live |
| A transport artifact writing a canonical registry | CF-033 / CF-034 — an Access package defines `metadata_schema` and creates `APPROVED` subjects |
| Configuration acting as semantic authority | CF-038 — provider limits split across registry, Spring properties and the physical planner |
| An authority with no producer at all | RC-003 — `site_contract_*`, `data_product`, `classification_*`, `provider_capability` |

---

## 2. AUTHORITY REGISTRY — CAD-02

**This registry is the doctrine's instrument.** The Consolidation's §4 Authority Register
is the *evidence-time snapshot*; this is the *governed* form with the full column set.
Seeded from recovery evidence; `†` marks a responsibility whose canonical authority is
**required but absent**.

### 2.1 Declaration responsibilities

| Responsibility | Canonical authority | Producer | Lifecycle owner | Derived representations | Executors / providers | Legacy | Correspondence mechanism | Elimination status |
|---|---|---|---|---|---|---|---|---|
| Platform semantic constitution † | `Platform Meta-Contract` | **none** | — | — | — | — | — | REQUIRED-BUT-MISSING (RC-003) |
| Contract-creating authority † | `Control Plane Schema` | **none** | — | — | — | migrations substitute | — | REQUIRED-BUT-MISSING (RC-003) |
| Site serving declaration † | `platform.site_contract_*` | **none** (migrations only) | `SiteContractRevisionApprovalService` | `PackagePlan`, `__gs_*` | serving dispatch, composer | KIDS seed migrations 022/023/054/055/057 | contract checksum; **no package digest** | producer REQUIRED-BUT-MISSING (CF-024a) |
| Dataset semantics | `platform.statistical_contract_draft` | `ContractWorkflow` ✔ | `ContractWorkflow` | `SemanticPlan`, `PhysicalPlan`, `__gs_*`, SDMX | `AccessAuthoringAdapter`, load, charts | — | `revisionDigest` + write-once trigger (PATTERN C) | CANONICAL |
| Source admission | `platform.ingestion_contract` | `StatisticalBindingService` (partial) | migrations (`_revision`) | — | publication gate | — | none | CF-032 |
| Controlled vocabulary † | `platform.classification_*` | **none** | migrations | `__cl_*` in packages | serving, charts | — | FK by `classification_item_id` ✔ | promotion path REQUIRED-BUT-MISSING (CF-016) |
| Vocabulary proposals | `platform.classifier_proposal` | `AccessClassifierProposalImportService` ✔ | — | — | — | — | `state='DRAFT'`, no UPDATE branch ✔ | CANONICAL (PATTERN B) |
| Metadata shape | `platform.metadata_schema` | **Access package** ✘ | package `approval_state` ✘ | `__gs_metadata_schema` | metadata assertions | — | **none** | **ACCIDENTAL SECOND AUTHORITY** (CF-033) |
| Product / tenant registration † | `platform.data_product` | **none** | `ProductTenancyRepository` (tenant key only) | — | everything | — | — | REQUIRED-BUT-MISSING (RC-003) |
| Provider capability † | `platform.provider_capability` | **none** (`ProviderCapabilityDiscoveryRunner` reads, fails closed) | — | — | compiler, physical planner | Spring properties, planner constants | **none** | **THREE AUTHORITIES** (CF-038) |
| Grammar (shape) | `statistical-contract-draft.schema.json` | hand-authored | grammar version `$id` | — | `ContractDraftParser` | — | **no parity corpus** | DR-005 — canonical, unwired |

### 2.2 Execution and state responsibilities

| Responsibility | Canonical authority | Producer | Derived / executors | Legacy | Correspondence | Elimination status |
|---|---|---|---|---|---|---|
| Release boundary | `publication.snapshot` + `snapshot_member` | `DataPlanePublicationWriter` | `serving.metric_cache`, archive | `dataset_snapshot.status` ✘ | **none** | **TWO AUTHORITIES** (RC-001) |
| Snapshot lifecycle | `publication.dataset_snapshot.status` | **five writers**, no owner | — | — | no CHECK constraint | **NO OWNER** (CF-025) |
| Gate evidence | `ReleaseGateEvidenceRepository` | `ReleaseGateService` ✔ | — | — | schema + facts digest ✔ | CANONICAL |
| Canonical statistical data | `statistics.*` | `CanonicalObservationWriter` | SDMX export, charts | KIDS carrier/DOUBLE model | typed columns, FKs | CANONICAL |
| Canonical entity data | `entity.entity_record` | materialization | `JSON_VALUE` reads | — | **`payload_json`, untyped** | CF-039 |
| Canonical relations | `entity.entity_link` | materialization | — | — | FKs both ends; **no cardinality** | CF-040 |
| Physical realization | generated `.accdb` | `AccessAuthoringAdapter` ✔ | — | `AccessCatalogService` (v1) | **no digest** (DR-007) | CANONICAL generator |
| Integration events | `platform.outbox_event` | 6 sites | — | — | — | **THREE CATEGORIES** (CF-041) |
| Ingestion | *(no single authority)* | **seven mechanisms** | — | Managed v1 + 4 strategies | — | PA-004, dispositions set |
| Visualization declaration | *(contested)* | **four mechanisms** | — | core `ChartDefinition` | — | **DR-001 decided**, not executed |
| Stored artifact bytes & content identity ‡ | `ingest.artifact_object` (row) + object store (bytes) | `ArtifactRegistry` ✔ — one INSERT site, content-addressed | `ArtifactObjectStore` / `ArtifactObjectInventory` ports with S3/MinIO adapters; `artifact_version`, `artifact_manifest`, attachments, archive, export | — | SHA-256 content identity; byte-size conflict on a repeated digest fails closed; `ArtifactObjectIntegrityAuditService` + reconciliation | CANONICAL — **registry ↔ store lifecycle alignment is convention-only** |

**No material mechanism may exist without being classifiable against this registry.**

### 2.3 ‡ Object-storage flow — classified 2026-09-21 (`DEF-04`)

`CTRL-ADOPTION` §7.1 recorded object-storage flow as an **unregistered responsibility**:
`docs/reference/CANONICAL-OBJECT-STORAGE-FLOW.md` was the only document describing it, and
CAD-02 had no row. Assigning that document `CANONICAL` would have invented an authority the
recovery never verified, so the gap was deferred to `PHASE-002`. It is now closed by
classification, not by promotion.

**Evidence (bounded inspection, 2026-09-21, `HEAD b46c043`).** A write-verb sweep over
`platform/` and `ops/` (`.java`, `.sql`, `.kt`, `.py`, `.ps1`, `.sh`, build outputs excluded)
finds exactly one writer of `ingest.artifact_object` — `ArtifactRegistry.objectId`, which looks
the digest up under `UPDLOCK,HOLDLOCK`, reuses the existing row when the checksum matches,
and throws on a byte-size conflict. Its own comment states the rule the registry enforces:
*"Content identity is the checksum; a second location for the same bytes is not a new
object."* Byte access is behind the `ArtifactObjectStore` / `ArtifactObjectInventory` ports
with S3/MinIO adapters (`service/storage/s3/*`), so the provider is substitutable and no
provider concept reaches canonical semantics.

**Classification.** `CANONICAL AUTHORITY` for *content identity and locator*, held by the
registry row; the object store is a `PROVIDER` for bytes; the flow document is
`SUPPORTING` documentation of that arrangement and keeps its `CTRL-ADOPTION` §7 authority —
**a document describing a mechanism is not the mechanism's authority (CAD-07).**

**Residual defect, recorded not resolved.** Registry ↔ store lifecycle alignment is
*convention only* — required by Javadoc and enforced nowhere (`REC-CONSOLIDATION` §10,
`REC-COVERAGE` L35). The integrity audit detects a missing object; nothing prevents a store
object from outliving its registry row. The acceptance condition is `BM-INV-22`
(`STD-BENCH-001` §5); its automation target is `MSA-032` (`STD-AUTO-001` §5.4).

**Confidence: MEDIUM-HIGH.** The sweep used the write-verb method that established `RC-003`,
and the table's DDL is in core migrations `087` / `094` (schema, not data). What is
`UNVERIFIED`: whether any runtime path outside this repository — an operator script, an
ops job or a manual database session — writes the table, which no repository sweep can
answer.

---

## 3. THE RULES

### CAD-03 — New-mechanism gate

Before introducing any new service, repository, table, registry, contract, schema, JSON
definition, migration declaration, Access metadata structure, configuration authority,
importer, exporter, reader, writer, validator, adapter, job, event, document or ADR, the
task **MUST** answer:

1. *What existing responsibility does this belong to?*
2. *Who is already authoritative for it?*

Then classify the proposal as exactly one of: `CANONICAL AUTHORITY` ·
`DERIVED REPRESENTATION` · `EXECUTOR` · `PROVIDER SPECIALIZATION` · `ADAPTER` ·
`COMPATIBILITY BRIDGE` · `TEMPORARY MIGRATION` · `HISTORICAL EVIDENCE` ·
`NEW FUNDAMENTAL CAPABILITY`.

**If it cannot be classified: STOP. Do not add it.**

### CAD-04 — Duplicate-responsibility test

Search repository-wide before creating: Java/Kotlin, SQL, migrations, JSON/YAML,
configuration, docs, ADRs, tests, Access/package structures, scripts, jobs, schemas,
contracts.

**Naming differences do not prove responsibility differences.** Compare *behaviour and
authority*, not class or file names. The recovery's own method is the standard: the
producer sweep that established RC-003 searched by SQL write verb across five axes, not by
class name — and `AccessCatalogService` was eliminated as the R8 generator on **table-set**
evidence, not on its name.

### CAD-05 — Derivation over repetition

If information is deterministically derivable from canonical authority, **derive or
generate it**. Do not repeat it manually in another artifact.

```text
CANONICAL DECLARATION -> VALIDATE -> DERIVE -> GENERATE -> EXECUTE -> VERIFY
```

Manual semantic repetition across Java, SQL, JSON Schema, Access, docs, configuration, API
models or visualization definitions is an architectural smell unless independently
justified. **Current known instance:** the Java parser and the JSON Schema state the same
shape twice with no parity enforcement (CF-009 / DR-005).

### CAD-06 — Derived artifact rule

Every derived artifact must identify, where technically appropriate: source authority ·
source version/revision · generator or compiler version · physical pattern version ·
provider · digest or correspondence evidence.

**A derived artifact MUST NOT silently become a new authority.**

Current gap: generated `.accdb` files carry `contract_code` and `contract_revision` as
**plain self-reported values** with no digest, no generator identity and no generator
version (DR-007, REQ-026, REQ-028).

### CAD-07 — Documentation authority

Architectural documents are classified `CANONICAL` · `SUPPORTING` · `HISTORICAL` ·
`SUPERSEDED` · `MIGRATION-ONLY`.

There must not be multiple apparently-current documents independently defining the same
architectural truth. **A superseded document must not look canonical.** Historical
knowledge may be preserved; its non-authoritative status must be obvious.

Known instances: four "final" documents dated 2026-09-15 coexisting (checkpoint §12);
`Platform Meta-Contract` described as authority in six documents with no implementation.

### CAD-08 — ADR rule

An ADR records a decision. It must not become an independent competing semantic model.
Every active ADR points to the responsibility and authority it governs; superseded
decisions link explicitly to their successor.

**Implementation and migration comments must not claim an ADR that does not govern them.**
Known instance: migrations 111/112 cite ADR-011 for decision D-3, which lives in ADR-012
(CF-036).

### CAD-09 — Migration rule

Historical migrations are **evidence of evolution, not current semantic authority**.

Do not encode new long-term semantic declarations in migrations when a canonical registry,
contract or declaration authority exists or should exist. Migrations may remain immutable
for reproducibility; **their historical presence does not justify preserving their
architectural responsibility.**

This rule is the direct expression of RC-002 and RC-003: 40 of 113 migrations are
KIDS-named and are currently the *only* producer of the site-contract declaration layer.

### CAD-10 — Test rule

Tests are classified as protecting `CANONICAL BEHAVIOR` · `LEGACY BEHAVIOR` ·
`COMPATIBILITY` · `MIGRATION` · `HISTORICAL REGRESSION`.

**A passing test does not make legacy behaviour canonical.** When legacy behaviour is
intentionally removed, obsolete tests are removed or rewritten with it. Known instance:
`StatisticalLoadServiceTest:151` pins a platform lifecycle state inside a loader unit test.

### CAD-11 — Compatibility rule

Every compatibility mechanism requires: what it compatibilizes · why it exists · canonical
side · legacy/external side · owner · **exit condition** — unless permanent external
compatibility is proven.

**Compatibility must not leak backward and redefine canonical architecture.**

### CAD-12 — Parallelism classification

Two mechanisms appearing to serve one responsibility are classified: `LEGITIMATE
SPECIALIZATION` · `DERIVED REPRESENTATION` · `TEMPORARY MIGRATION` · `REQUIRED
COMPATIBILITY` · `ACCIDENTAL DUPLICATION` · `OBSOLETE ARCHITECTURE` · `SHADOW/BYPASS PATH`
· `ABANDONED EXPERIMENT`.

**`UNKNOWN` is not an acceptable permanent state.**

### CAD-13 — Elimination rule

For accidental duplication, obsolete architecture, shadow/bypass paths and abandoned
experiments, in order:

```text
preserve unique knowledge/data/behaviour
 -> migrate consumers
 -> verify canonical replacement
 -> disable old path
 -> repository-wide reference search
 -> remove implementation
 -> remove obsolete configuration
 -> remove obsolete tests
 -> update or remove misleading documentation
 -> verify no active reference remains
```

```text
MIGRATED ≠ DONE.

DONE = MIGRATED
     + OLD AUTHORITY ELIMINATED OR EXPLICITLY JUSTIFIED
     + REFERENCES CLEAN + DOCS CLEAN + TESTS CLEAN
     + VERIFIED.
```

This supersedes nothing: it is GOV-R1 and GOV-R5 stated at full length. Where they differ
in wording, **this is the operative form**.

### CAD-14 — Doc ↔ contract ↔ code ↔ data consistency gate

Final verification compares, per responsibility: canonical documentation · ADR/decision ·
contract/schema · database model · implementation · configuration · migrations · tests ·
generated/physical artifacts · runtime behaviour.

**If two layers independently define contradictory semantic truth: FAIL.**

### CAD-15 — Single-source-of-truth fitness check

A repeatable check asking, for every material responsibility:

1. What is authoritative?
2. Is there exactly one canonical authority?
3. Are all other representations derived, executing, adapting or historical?
4. **Can any secondary representation mutate semantic truth independently?**
5. **Can a developer or AI reasonably mistake a secondary artifact for authority?**
6. **Is there an obsolete competing implementation?**
7. **Is there an unexplained parallel path?**

**Any `YES` to 4, 5, 6 or 7 requires explicit resolution.**

Applied to the current repository, the check returns `YES` at least here:

| Q | Responsibility | Instance |
|---|---|---|
| 4 | metadata shape | an Access package mutates `metadata_schema` (CF-033) |
| 4 | release boundary | `dataset_snapshot.status` mutates liveness independently (RC-001) |
| 5 | site declaration | `__gs_*` in a package looks authoritative; it is transport (DR-007) |
| 5 | provider limits | Spring properties look authoritative; the registry should be (CF-038) |
| 6 | ingestion | Managed v1 + four import strategies remain reachable |
| 6 | charts | four mechanisms, one decided canonical (DR-001) |
| 7 | startup | `CarsMigrationRunner` retains a disabled execution path (CF-037) |

### CAD-16 — AI / agent execution rule

**Every future implementation prompt or task MUST state: DO NOT CREATE A NEW PARALLEL
ARCHITECTURAL MECHANISM.**

Before adding a mechanism an agent must: inspect this Authority Registry · search existing
mechanisms by behaviour · **extend the canonical mechanism when the responsibility is the
same** · create a new mechanism only for a proven new responsibility or a legitimate
specialization.

**If the canonical mechanism cannot support the required behaviour: STOP and report the
architectural conflict.** Do not work around it by creating a parallel implementation.

This rule exists because the repository is direct evidence of what happens without it:
multiple AI-assisted generations each added a coherent mechanism beside an existing one,
and the result is seven ingestion paths, four chart models, two grammars and two
definitions of "live".

### CAD-17 — Canonical design rule

Canonical design derives from proven requirements + domain semantics + invariants +
recovered valid knowledge + engineering standards.

**It must not be shaped around accidental legacy structure.**

```text
LEGACY CONVERGES TOWARD CANONICAL DESIGN.
CANONICAL DESIGN DOES NOT CONVERGE TOWARD LEGACY FOR IMPLEMENTATION CONVENIENCE.
```

Worked example from the recovery: CF-039 (entity properties in untyped `payload_json`) is
resolved by **raising entity storage to the typing the statistical, relational and
classifier families already have** — not by lowering the others to documents, and not by
collapsing all families into one physical table.

### CAD-18 — Final rehabilitation gate

Rehabilitation is **not** complete merely because the canonical implementation exists. It
is complete only when:

- every material responsibility has an identified canonical authority;
- all material parallel mechanisms are classified;
- accidental competing authorities are eliminated;
- temporary migration paths have exited or carry explicit active exit conditions;
- compatibility paths are justified;
- historical artifacts are clearly non-authoritative;
- obsolete docs no longer appear current;
- obsolete tests no longer protect removed architecture;
- migrations are not mistaken for active semantic authority;
- generated artifacts trace to canonical sources;
- runtime paths agree with declared authority;
- the repository-wide contradiction sweep passes.

**Final invariant:**

> A developer or AI entering this repository must be able to determine, for any material
> architectural responsibility, **one** authoritative answer to:
> **"Where does the truth for this responsibility live?"**
>
> **If the repository gives two independent answers, rehabilitation is NOT COMPLETE.**

---

## 4. Current compliance status

Measured against CAD-18 using recovery evidence. **This is the starting position, not a
plan.**

| Gate condition | Status |
|---|---|
| Every responsibility has an identified canonical authority | **FAIL** — 5 responsibilities have none (RC-003) |
| Parallel mechanisms classified | **PASS** — PA-001…PA-007 + PASS 2B sweep, no `UNKNOWN` remains |
| Accidental competing authorities eliminated | **FAIL** — RC-001, CF-033, CF-034, CF-038, CF-041 |
| Temporary migrations have exit conditions | **PARTIAL** — statistical loader classified, no exit condition set |
| Compatibility paths justified | **PASS** — legacy surface taxonomy with per-family switches |
| Historical artifacts clearly non-authoritative | **FAIL** — migrations are the only declaration producer |
| Obsolete docs no longer appear current | **FAIL** — four coexisting "final" documents |
| Obsolete tests classified | **PARTIAL** — ~62 unclassified |
| Migrations not mistaken for authority | **FAIL** — RC-002 / RC-003 |
| Generated artifacts trace to canonical sources | **FAIL** — no digest, no generator identity (DR-007) |
| Runtime agrees with declared authority | **FAIL** — CF-021, CF-023, CF-028 |
| Contradiction sweep passes | **PARTIAL** — one benign unresolved item |

**CAD-18: NOT COMPLETE.** Expected — the repository is entering rehabilitation, not
leaving it. This table is the scoreboard the final gate will re-run.

---

## 5. Relationship to existing governance

**Additive under GOV-R9.** This doctrine supersedes nothing.

| Existing rule | Relationship |
|---|---|
| GOV-R1 `MIGRATED ≠ DONE` | restated at full length as **CAD-13**; CAD-13 is the operative form |
| GOV-R2 contradiction closure | reinforced by **CAD-14** |
| GOV-R3 parallel architecture | generalized by **CAD-12** |
| GOV-R4 knowledge-preserving elimination | first step of **CAD-13** |
| GOV-R5 consumer-first removal | sequence inside **CAD-13** |
| GOV-R6 docs/ADR in elimination scope | expanded into **CAD-07** and **CAD-08** |
| GOV-R7 test classification | expanded into **CAD-10** |
| GOV-R8 historical migration record | expanded into **CAD-09** |
| GOV-R9 anti-loss | the rule under which this artifact exists |

**INV-002** ("one business meaning has one canonical owner") is the invariant; this
doctrine is its enforcement machinery. **INV-013** (derived plans never persisted as
authority) is CAD-06 applied to plans.
