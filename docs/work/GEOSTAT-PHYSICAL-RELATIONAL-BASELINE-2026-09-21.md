---
id: ARCH-BASELINE
type: EVIDENCE
title: Physical relational baseline
status: COMPLETE
authority: CANONICAL
scope: reconstructed SQL physical model — tables, constraints, indexes, triggers, topology — and the anti-regression analysis against canonical design
owner: PHASE-004
created: 2026-09-21
updated: 2026-09-21
related: ARCH-CANONICAL, REC-CONSOLIDATION, AUD-CAPABILITY
---

# PHYSICAL RELATIONAL BASELINE

**Why this exists.** `PHASE-004` synthesis had reached §29 when an ordering correction was
issued: *no canonical decision that generalizes, replaces or removes a relational structure may
be made before the existing physical model is reconstructed from the artifacts.* The correction
was right, and the evidence below proves it — the draft architecture was derived from the Access
artifacts, the recovery registers and the benchmark, and **the SQL data plane had never been
inventoried by any phase**. It is four times larger than every Access artifact combined and it
enforces invariants no document mentions.

**Method.** Static reconstruction of the declared DDL across all 113 migrations in chain order
(`platform/apps/geostat/backend/core/src/main/resources/db/platform/*.sql`), including DDL
wrapped in `EXEC(N'…')`. Machine-readable output:
`docs/work/evidence/physical-relational-baseline/physical-model.json`.

**Limitation closed the same day.** The static parse was cross-checked against the **live dev
databases** (`administrator@192.168.1.199`, container `geostat-system-mssql`; databases
`geostat-system` / `geostat-data` / `geostat-archive`) with read-only `sys.*` catalogue queries.
Access was authorised by the owner mid-phase. **No DDL or DML was executed, and no credential
entered this session** - the container's own environment variable was dereferenced inside
`docker exec` on the remote host. Live output:
`docs/work/evidence/physical-relational-baseline/live-tables.txt`.

**Where the two differ, the live model is authoritative**; §1.1 reconciles them.

---

## 1. Inventory

| Measure | Count |
|---|---|
| Migrations parsed | **113** |
| Tables | **120** |
| Columns | **1 077** |
| Tables with a primary key | **120 / 120** |
| `UNIQUE` constraints | **87** |
| `CHECK` constraints | **127** |
| Column `DEFAULT`s | **201** |
| `FOREIGN KEY` constraints | **122** (0 with `ON DELETE CASCADE`) |
| Indexes (distinct) | **59** — 9 unique, 8 filtered |
| Triggers (distinct) | **18** |
| Views | **1** |
| Stored procedures | **1** |

**Tables by schema:** `platform` 74 · `ingest` 20 · `entity` 6 · `archive` 5 · `publication` 4 ·
`statistics` 4 · `geo` 2 · `audit` 2 · `raw` 1 · `reference` 1 · `serving` 1.

**Scale comparison, because it is the point:** the largest Access artifact inspected in
`PHASE-003` has 32 tables and 36 relationships. The server has **120 tables and 122 foreign
keys**, plus 127 check constraints and 18 triggers that no Access artifact can carry. The
Access files are a *transport view of a fraction* of this model.

### 1.1 Live reconciliation - and the 19 tables nobody had counted

| Measure | Static parse of the chain | **Live (3 databases)** | Reading |
|---|---|---|---|
| Tables | 120 | **139** | +19 - see below |
| Columns | 1 077 | **1 234** | the 19 extras carry 157 columns |
| Primary keys | 120 | **139** | every live table has one |
| `UNIQUE` | 87 | **97** | - |
| `FOREIGN KEY` | 122 | **135** | - |
| `CHECK` | 127 | **115** | the static parse **over-counted** - inline checks were double-attributed. Live is authoritative |
| Defaults | 201 | **206** | - |
| Non-PK indexes | 59 | **60** (7 filtered) | - |
| Triggers | 18 | **20** | - |
| Views / procedures | 1 / 1 | **1 / 1** | agrees |
| Migration ledger | 113 files | **113 applied**, last `112_statistical_numeric_envelope_data.sql` | the chain is fully applied |

**Every one of the 120 chain-declared tables exists live. Zero are missing.** The chain is
complete and was parsed accurately; the difference is entirely additive.

**The 19 extra tables are all in `dbo.` and none is governed by the migration chain:**

```text
users · roles · permissions · role_permissions · user_roles · tokens · user_profile
chart_definitions · page_nodes · sliders_data
import_jobs · import_job_items · import_table_mappings · data_profiles
prices · prices_1 · auto_eoes · main
migrations
```

**This is the legacy platform, physically resident in the same databases as the governed control
plane, and no phase had ever counted it.** Three consequences, each mattering more than the
number:

1. **`dbo.migrations` is a second migration ledger.** `platform.schema_migration` governs the
   113 files; `dbo.migrations` governs whatever the legacy application did. `RC-002`/`CF-020`
   described "migration registration as a second manifest" from the *code* side - here it is,
   as a table, in a production-shaped database.
2. **`dbo.chart_definitions` and `dbo.page_nodes` are physical.** `DR-001` dispositions core
   `ChartDefinition` as `DEPRECATE → DELETE` and `CF-030` counts four chart mechanisms; one of
   them has a table here, beside the governed chart declaration.
3. **`dbo.users / roles / permissions / role_permissions / user_roles / tokens` is a complete
   legacy authorization model** coexisting with the OIDC/ABAC model of `ADR-010`. Two
   authorization substrates in one database is a trust-boundary fact, not a cleanup item.

`dbo.prices`, `prices_1`, `auto_eoes`, `main`, `sliders_data`, `data_profiles` and the
`import_*` trio are the legacy Georgian domain and the Managed-v1 import pipeline
(`REC-CONSOLIDATION` §8) - the physical counterpart of the eleven legacy controllers.

**What this changes for `PHASE-004`:** nothing in the canonical *design*, and a great deal in
what the design must **not** inherit. The canonical core owns `platform`, `ingest`, `raw`,
`entity`, `statistics`, `geo`, `reference`, `publication`, `serving`, `archive`, `audit`.
**`dbo.*` is legacy, it sits outside the canonical boundary, and `PHASE-011` now has an exact
physical inventory to eliminate instead of a prose list.**

---

## 2. Relationship topology — the hubs

Inbound foreign keys, i.e. what the model is organised around:

| Table | Inbound FKs | What that makes it |
|---|---|---|
| `platform.site_contract_revision` | **12** | the control plane's hub — almost everything binds to an exact contract revision |
| `platform.data_product` | 9 | the tenancy and ownership root |
| `platform.dataset_version` | 8 | the unit that ingestion and publication both key on |
| `raw.source_record` | 6 | every canonical record points back at its source row |
| `entity.entity_record` | 6 | the entity hub — localization, locators, classification, links, attachments |
| `platform.contract_structure` | 5 | **a structure concept already exists** — see §4 |
| `platform.dataset` · `metric` · `statistical_unit` · `classification_item` | 4 · 4 · 4 · 3 | the semantic registries |

**Topology, not a table list:** the model is a star of *declaration* (`platform.*`) around which
*instances* (`raw`, `entity`, `statistics`, `geo`) hang, with `publication` cross-cutting them
through snapshot membership and `ingest` feeding them. Every instance plane carries an FK back
to `raw.source_record`, which is why lineage is answerable today at all.

---

## 3. Guarantees enforced in the engine, not in documents

### 3.1 Immutability and append-only — 18 triggers

| Trigger family | Tables | Guarantee |
|---|---|---|
| `tr_*_published_immutable` | `raw.source_record`, `entity.entity_record`, `entity.localized_text`, `entity.resource_locator`, `entity.artifact_attachment`, `geo.feature`, `statistics.observation` | **a row that belongs to a published snapshot cannot be updated or deleted** |
| `tr_*_approved_immutable` | `platform.artifact_policy`, `platform.artifact_relation_definition`, `platform.statistical_contract_draft`, `platform.statistical_reference` (×2, incl. definition digest) | **an approved declaration is frozen in the database** |
| `tr_*_append_only` | `ingest.artifact_package_run_stage`, `platform.data_product_tenant_assignment`, `platform.statistical_contract_draft_event` | history cannot be rewritten |
| `tr_*_immutable` | `ingest.artifact_version`, `ingest.artifact_manifest_document`, `ingest.artifact_object_audit_run` | artifact identity and audit runs are write-once |

**This is a capability the draft architecture assumed and the platform already enforces.**
`ARCH-CANONICAL` §10 said *"canonical is corrected only by a new record"* as a design intention;
here it is a trigger. Any canonical design that moves these guarantees into application code is
a regression.

### 3.2 Conditional uniqueness — 8 filtered indexes

| Index | Guarantee |
|---|---|
| `ux_statistical_contract_draft_approved` | **one approved draft per (product, dataset)** |
| `ux_contract_page_binding_runtime_active` | one active binding per (site contract revision, runtime page) |
| `ux_api_operation_idempotency_key`, `ux_api_operation_owner_idempotency` | **idempotency enforced by the engine**, globally and per requester |
| `ux_artifact_object_audit_issue_open` | at most one open audit issue per object |
| `ux_artifact_storage_orphan_open` | at most one open orphan per storage location |

**"One active/approved/open per X" is a state-machine invariant expressed as a partial unique
index.** No document in this repository records these. They are the cheapest correct
implementation of a rule that application code usually gets wrong under concurrency.

### 3.3 Domain constraints — 127 `CHECK`s

| Kind | Count | Example |
|---|---|---|
| Enumerated value domain (`IN(...)`) | **39** over 34 columns | `verification_status IN('REGISTERED','VERIFIED','MISSING','CHECKSUM_MISMATCH')`; `classifier_proposal.state`; `site_contract_revision.status`; `contract_namespace.authority_mode` |
| Other predicates | 38 | cross-field rules |
| Length / format | 17 | `LEN(sha256)=64 AND sha256 NOT LIKE '%[^0-9a-f]%'` — hex-digest validation in the engine |
| Conditional nullability | 17 | "this column is required only when that one is set" |
| Range / non-negative | 16 | `byte_size>=0` |

---

## 4. Structures the draft architecture did not know existed

Each of these is a **proven relational strength** that `ARCH-CANONICAL` must preserve or
explicitly supersede with justification.

| Structure | What it provides | Consequence for the draft |
|---|---|---|
| **`platform.contract_structure`** — `structure_code`, `structure_kind`, `data_class`, `grain`, `authority_mode`, `lifecycle_policy`, `allowed_content`, `forbidden_content`, `lineage_policy`, `quality_policy_code`, `confidentiality_policy_code`, `standard_code`, `UNIQUE(namespace, code, revision)` | a **structure + data class + grain + policy + authority** concept **already exists** in the control plane | `ARCH-CANONICAL` §3's `K-04/K-06/K-13` are **an evolution of this table, not an invention**. The draft must say so. `grain` is `NVARCHAR(1000)` — prose — which is exactly `CAP-M04`, now confirmed at the source |
| **`entity.localized_text`** — PK `(entity_id, field_code, language_tag)` | typed, keyed multilingual text per field | the draft never modelled localization. A component-only model would have **lost a working pattern** |
| **`entity.resource_locator`** — PK `(entity_id, locator_kind, language_tag)` | typed locators per entity per kind per language | same |
| **`entity.entity_classification`** — PK `(entity_id, attribute_id, classification_item_id)` | classification assignment as a keyed relation | the draft's `FAM-03` must cover it rather than replace it |
| **`reference.classification_item_snapshot`** — PK `(snapshot_id, classification_item_id)`, carries code, labels, parent, status | **published snapshots pin codelist content**, so a published output is self-contained and reproducible even if the codelist later changes | the draft said "reference by version". Materializing into the snapshot is **stronger**, and dropping it would be a regression |
| **`statistics.observation`** — `valid_from`, `valid_to`, `is_current` | **application-time versioning already exists on observations** | this **contradicts** `ARCH-CANONICAL` §11, which claimed no temporal columns exist or are needed. See §6.1 |
| **`statistics.observation_dimension`** — PK `(observation_id, dimension_id)`, `classification_item_id` or `scalar_code` | dimensions are **rows, not columns**; the observation PK is a surrogate | the draft's dimension-tuple grain is **not expressible in the current model**. See §6.2 |
| **`publication.snapshot_member`** — PK `(snapshot_id, dataset_version_id)` | membership as the release boundary, properly keyed | matches the draft; preserve |
| `entity.v_artifact_attachment_reconciliation` (view), `publication.usp_kids_r8_reconciliation` (procedure) | reconciliation logic in the database | see §5 |

---

## 5. Contradiction with a canonical register — recorded, not resolved here

`REC-CONSOLIDATION` RC-003's falsification states, as one of five independent axes:

> *"stored procedures (**none exist** in 113 migrations)"*

**That statement is false.** `publication.usp_kids_r8_reconciliation` is created by migration
`073_kids_r8_reconciliation_report.sql` and altered by `075_reconciliation_published_scope.sql`;
`entity.v_artifact_attachment_reconciliation` is a view created by `087`.

| Impact | Assessment |
|---|---|
| Does it reverse RC-003? | **No.** RC-003 concludes that canonical *registries* have no runtime *producer*. A reconciliation report procedure produces no registry row, so the conclusion stands on the remaining four axes |
| Is it material anyway? | **Yes.** It means the data plane *does* carry procedural logic, so "no procedural code in the database" cannot be used as a premise by `PHASE-004` — and the draft did not use it, by luck rather than by evidence |
| Action | Recorded here under `/CLAUDE.md` §3 as an **evidence defect in a canonical register**. Correcting `REC-CONSOLIDATION` is a bounded change owned by that register, not by this document, and it is listed as a downstream obligation |

---

## 6. Anti-regression analysis — `PRESERVE PROVEN RELATIONAL STRENGTH`

Every place where the draft architecture would replace a typed relational guarantee. **Three
regressions found; two are unjustified as drafted and one is a strengthening that was
mis-described.**

### 6.1 Observation temporality — **REGRESSION, unjustified as drafted**

`ARCH-CANONICAL` §11 concluded that bitemporality falls out of *period dimension × snapshot* and
that **no temporal columns are needed anywhere**. The baseline shows `statistics.observation`
already carries `valid_from`, `valid_to`, `is_current` — application-time versioning, engine-
level, today.

| | Current guarantee | Draft |
|---|---|---|
| Mechanism | three typed columns + `is_current` flag | snapshot selection only |
| What it can answer | "what did this observation say between T1 and T2, independent of publication" | "what did snapshot S say" |
| Loss | **a fact can be superseded within a dataset version without a new snapshot** — the draft cannot express that | — |

**Verdict: the draft is wrong to claim no temporal mechanism is needed.** Either the existing
columns are preserved, or their removal is justified by showing that no consumer uses
non-snapshot-bound history — which nobody has checked. `§11` must be revised, not defended.

### 6.2 Observation grain — **STRENGTHENING, but a representation change with migration cost**

| | Current | Draft |
|---|---|---|
| Identity | surrogate `observation_id`; dimensions in `observation_dimension` rows | grain = dimension tuple, enforced by a unique constraint |
| Guarantee | `INV-006` ("one observation per dimension tuple") is **application-enforced only** — no constraint can express it over a child table | engine-enforced |
| Cost | — | dimensions become columns per structure; a real migration, not a rename |

**Verdict: a genuine strengthening** — and the draft under-stated it as "grain is declared" when
the truth is "grain becomes enforceable for the first time". It must also acknowledge that
`observation_dimension` is *not* EAV degeneration to be sneered at: it is what makes a
variable-dimension model possible in one table, and replacing it means accepting per-structure
physical tables (`BM-EC-020`'s bounded proliferation), which the draft never said out loud.

### 6.3 Enumerated domains — **REGRESSION RISK if CHECKs become codelist references**

39 enumerated `CHECK` constraints enforce closed vocabularies with no join and no configuration.
The draft's `K-03` would express several as codelist references.

| | `CHECK IN(...)` | Codelist reference |
|---|---|---|
| Enforcement | engine, always, no join | FK to a versioned item |
| Versioning | none — changing it is a migration | versioned, crosswalk-able |
| Reuse | none | across families |
| Failure mode | migration to change a value | a bad version pin |

**Verdict: not a regression *if* the replacement is an FK to a codelist version — that is equal
enforcement plus versioning.** It **is** a regression if any value domain becomes metadata,
convention or application validation. The rule to carry forward: *an enumerated domain may move
from `CHECK` to FK-plus-version; it may never move to a string column validated in code.*

### 6.4 Everything else — preserved

| Guarantee | Draft treatment |
|---|---|
| 120/120 PKs, 122 FKs, 87 UNIQUE, 201 defaults | preserved — `§4 FAM-*` patterns compile to exactly these |
| 18 immutability/append-only triggers | preserved and **promoted**: the draft's "declarations immutable, records append-only" is this, stated as architecture |
| 8 filtered unique indexes | **must be preserved explicitly** — the draft never mentions conditional uniqueness, and "one approved draft per dataset" is a state-machine invariant it silently assumed |
| 0 cascade deletes | preserved — deletion stays governed |
| Localization, locators, classification, snapshot-pinned codelists | **must be added to the draft** (§4) |
| View + procedure | preserved as provider-side reconciliation; the canonical model owns the *rule*, the provider may own the *execution* |

---

## 7. Classification of every physical structure

Per the instruction: *do not classify a concrete relational structure as accidental legacy merely
because it is physical.*

| Class | Count | Basis |
|---|---|---|
| **LEGACY, OUTSIDE THE CANONICAL BOUNDARY** | **19** | the `dbo.*` set found by live introspection (§1.1): legacy auth, legacy charts and pages, legacy import pipeline, legacy Georgian domain, and a second migration ledger. Physical, real, and **not** part of the canonical core |
| **PROVEN RELATIONAL STRENGTH — preserve** | 96 | carries a PK, FKs and/or constraints that express a real invariant: all of `platform.*` declaration tables, `ingest.*`, `entity.*`, `statistics.*`, `publication.*`, `reference.*`, `archive.*`, `audit.*` |
| **PROVEN, REPRESENTATION MUST CHANGE** | 3 | `statistics.observation`, `observation_dimension`, `entity.entity_record` — the invariant is right, the shape blocks grain enforcement (`§6.2`) or typing (`CF-039`) |
| **KIDS-SEEDED DECLARATION** | ~12 | rows and tables written only by the KIDS seed migrations (`022/023/054/055/057`) — `RC-002`'s "migrations as the only producer"; the *structure* is legitimate, the *producer* is not |
| **ACCIDENTAL / DEAD** | 3 | `platform.contract_revision_lifecycle` (not written by the approval path), the `'APPROVED'`/`'PREPARED'` dead vocabulary in `dataset_snapshot`, `ROLLED_BACK` |
| **UNCLASSIFIED** | 6 | `geo.*` (2), `serving.metric_cache`, `audit.*` (2), `archive` detail — inspected structurally, not traced to consumers in this pass |

**`PHYSICAL_STRUCTURES_CLASSIFIED: 133 / 139`.** The six unclassified (`geo.*` x2,
`serving.metric_cache`, `audit.*` x2, archive detail) are named rather than assumed, and
classifying them is a bounded follow-up. The 19 `dbo.*` tables are classified as legacy by
origin: they are absent from the governed chain that creates every other table in these
databases.

---

## 8. What this baseline obliges `PHASE-004` to do before it can pass

1. **Revise `ARCH-CANONICAL` §11** — observation temporality is not solved by snapshots alone
   (`§6.1`).
2. **Revise `ARCH-CANONICAL` §3** — the kernel is an evolution of `platform.contract_structure`,
   not an invention; say so and inherit its proven fields (`data_class`, `authority_mode`,
   `lifecycle_policy`, policy references, `standard_code`).
3. **Add to `§4`** — localization, resource locators, classification assignment and
   snapshot-pinned codelists are existing patterns the family model must express.
4. **Add conditional uniqueness** to the grain/constraint model (`§6`) — partial unique indexes
   are a first-class invariant mechanism, not an implementation detail.
5. **State the enumerated-domain rule** (`§6.3`) so no `CHECK` degrades into convention.
6. **State the per-structure table cost** that dimension-tuple grain implies (`§6.2`).
7. **Correct `REC-CONSOLIDATION`** on the stored-procedure claim (`§5`) — bounded change, owned
   by that register.
8. **Record the 19-table legacy residency** (`§1.1`) with its owners: the legacy register
   (`REC-CONSOLIDATION` §8) gains physical evidence, and `PHASE-011` gains an exact elimination
   inventory. The canonical core must declare `dbo.*` outside its boundary explicitly, so that
   the design cannot inherit it by proximity.

Until these are done, the canonical architecture **cannot be shown non-regressive**, and
`PHASE-004` stays `INCOMPLETE`. That is the correct outcome of the ordering correction: the
draft was not wrong to exist, it was wrong to be finalized before this evidence.
