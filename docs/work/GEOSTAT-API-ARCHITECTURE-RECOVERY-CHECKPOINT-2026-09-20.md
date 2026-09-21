---
id: REC-CHECKPOINT
type: EVIDENCE
title: Architecture recovery checkpoint
status: COMPLETE
authority: SUPPORTING
scope: batch-by-batch recovery evidence trail
owner: architecture recovery
created: 2026-09-20
updated: 2026-09-20
---

# GEOSTAT API architecture-recovery checkpoint

**Checkpoint date:** 2026-09-20  
**Recovery target:** `platform/apps/geostat/backend/api`  
**Repository root:** `C:\Users\Test-User\IdeaProjects\gesotat-system`  
**Mode:** discovery/recovery/planning only; no rehabilitation was implemented  
**Status:** **INCOMPLETE**  
**Last continuation:** session 2 (Claude Opus 5, 2026-09-20) — new evidence, verifications and conflicts are in section 23; sections 1-22 are preserved as written by the previous agent

> **READ THIS FIRST.** A reconciled baseline now exists:
> **`GEOSTAT-API-RECOVERY-CONSOLIDATION-2026-09-20.md`**. It carries the canonical finding
> register, the requirement ledger (REQ-001…041), the authority / lineage / contract /
> physical-pattern / legacy / missing-component / known-gap registers, the automation
> baseline, the orphan register and the pending evidence inputs.
> **Where that file and this one disagree, that file is current** and this one is the
> evidence trail. A future session should read the consolidation baseline first and use
> this checkpoint only to look up the evidence behind a specific finding.  

This file is a continuation checkpoint, not a master rehabilitation plan and not an
architecture decision. It records the evidence gathered before the owner stopped
further investigation. Statements marked `UNVERIFIED` must not be promoted into
requirements or decisions without additional evidence. Confidence expresses the
strength of the currently inspected evidence, not production readiness.

## 1. Scope boundary and session constraints

- **IN-SCOPE ARTIFACT:** everything physically below
  `platform/apps/geostat/backend/api`.
- **EXTERNAL CONTEXT / DEPENDENCY:** repository artifacts outside that directory,
  including `backend/core`, `backend/mobile`, `docs`, `ops`, deployment files and
  frontend consumers. These were inspected only to explain the target.
- A future plan must not silently prescribe changes outside the target. Such work
  must be labelled `CROSS-BOUNDARY CHANGE REQUIRED`, with reason, compatibility
  consequences and dependent tasks.
- The original recovery session was explicitly read-only. No production/source
  implementation was changed. This checkpoint was created only after the owner
  explicitly requested a persistent recovery record.
- The repository governance protocol normally requires a bounded
  `docs/work/cards/<id>/governance.json`. None was created for the original
  read-only recovery because that would have contradicted the then-current
  no-modification instruction. This is a process evidence gap, not an implicit
  waiver.
- A baseline Gradle command was started and then deliberately interrupted when the
  owner ordered investigation to stop. It may have refreshed ignored Gradle/test
  outputs, but no tracked source change was intentionally made by the recovery
  session.

## 2. Baseline captured so far

### 2.1 Source state

- `HEAD`: `b46c0438d4927851fcd688a8a8e71468aba6cf18`
- `git describe`: `geostat-v1.0.2-oidc-rs256-132-gb46c043-dirty`
- Branch/worktree name observed in evidence documents: `security/hardening-2`.
- Worktree was already materially dirty before recovery began.
- In the target there were **26 modified tracked files** and **29 untracked files**.
- The target contained approximately:
  - 1,471 physical files including ignored/generated/runtime artifacts;
  - 512 tracked files;
  - 399 production Java files, about 20,438 lines;
  - 122 test Java files, about 8,190 lines;
  - 930 ignored files.
- Large ignored/local artifacts below the target included `build/`, `logs/`,
  `storage/`, `tmp/`, `api.jar`, `app.jar` and local `.accdb` files. They are not
  source authority. Their provenance and intended retention remain `UNVERIFIED`.

### 2.2 Dirty target areas observed

Modified or untracked target work included, among other files:

- `build.gradle`
- legacy controllers
- `StatisticalContractController`
- `StatisticalReferenceController`
- `PlatformSchemaMigrationRunner`
- `StatisticalContractConfiguration`
- `ReferenceRegistryService`
- `ContractWorkflow`
- `application-prod.yml`
- related tests
- untracked `LegacySurfaceExceptionHandler`
- untracked `StatisticalChartController`
- untracked `security/legacy/*`
- untracked `service/platform/packaging/*`
- untracked `ChartService`
- untracked `CanonicalObservationReader`
- untracked statistical JSON Schema and tests

External untracked context included Control/Data migration files `106` through
`112` below `backend/core`.

### 2.3 Build/test baseline

Command started:

```text
platform/apps/geostat/backend/gradlew.bat :core:test :api:test --no-daemon
```

Observed before interruption:

- core, API and mobile compilation/test-class preparation were `UP-TO-DATE`;
- `:core:test` began;
- `:api:test` began;
- no test failure had been emitted before interruption;
- the command was interrupted on owner instruction and exited `1` because of the
  interruption.

Therefore:

- current compilation status: **PARTIALLY VERIFIED**;
- current full core test status: **UNVERIFIED**;
- current full API test status: **UNVERIFIED**;
- historical evidence in `AIR-2026-048` reports a reproducible pass of 454 API
  tests with two skipped after forcing one test fork, but that is not a fresh pass
  for this exact worktree.

## 3. Mandatory governance material inspected

The following repository authorities were read before target analysis:

- `docs/reference/CANONICAL-FULL-TREE.md`
- `docs/reference/ENGINEERING-QUALITY-DOCTRINE.md`
- `docs/reference/engineering/README.md`
- `docs/reference/engineering/ARCHITECTURE.md`
- `docs/reference/engineering/REQUIREMENTS.md`
- `docs/reference/engineering/ANTI-PATTERNS.md`
- `docs/reference/engineering/CHANGE-PROTOCOL.md`
- `docs/reference/engineering/STANDARDS.md`
- engineering manifest/agent-entrypoint material linked by the README
- `docs/platform-capability-and-architecture-audit-2026-09-13.md`

The platform audit was read in several sections but not re-read as a byte-complete
single document after command-output truncation. It is therefore classified
`PARTIALLY_INSPECTED`, even though its major findings were captured.

Recovered platform doctrine, **HIGH confidence**:

1. Authority flows `M3 semantic constitution -> M2 contract grammar -> M1
   approved declaration -> M0 instances`.
2. Grammar validation, semantic compilation, approval and execution are separate
   gates.
3. Platform identity is contract-driven, metadata-driven and
   schema/provider/site-agnostic.
4. One concept/semantic responsibility should have one canonical owner.
5. Unsupported capabilities must be rejected explicitly; silent feature loss is
   prohibited.
6. Generated artifacts are projections/evidence, not source of truth.
7. Declared capability must survive producer, compiler, persistence, policy,
   serving/API and operational paths.
8. Relevant layers are Control, Ingestion, Data, Archive, Serving, Security,
   Observability and Delivery.
9. A policy/governance PASS is structural evidence only; it does not prove runtime
   behavior or production readiness.

Primary evidence: the mandatory documents above, especially
`ENGINEERING-QUALITY-DOCTRINE.md`, `engineering/ARCHITECTURE.md`,
`engineering/REQUIREMENTS.md` and `engineering/CHANGE-PROTOCOL.md`.

## 4. Continuation ledger

### INSPECTED

The word `INSPECTED` means enough content was read to support the recorded finding;
it does not mean every line was reviewed unless explicitly stated.

#### Repository and build structure

- target-wide file inventory and package/line counts
- target `build.gradle`
- parent Gradle/module relationship for `core`, `api`, optional `mobile`
- Git status, recent commit sequence and target dirty-state classification
- ignored/generated/runtime artifact inventory

#### Target packages inventoried

- `src/main/java/org/base/api/config`
- `src/main/java/org/base/api/controller`
- `src/main/java/org/base/api/security/legacy`
- `src/main/java/org/base/api/security/tenancy`
- `src/main/java/org/base/api/security/scope`
- `src/main/java/org/base/api/service` legacy area
- `src/main/java/org/base/api/service/artifact` and run/stage/sweep/upload areas
- `src/main/java/org/base/api/service/catalog`
- `src/main/java/org/base/api/service/contract/approval`
- `src/main/java/org/base/api/service/dynamic`
- `src/main/java/org/base/api/service/platform`
- `src/main/java/org/base/api/service/platform/access`
- `src/main/java/org/base/api/service/platform/packaging`
- `src/main/java/org/base/api/service/platform/statistical`
- statistical compiler/model/registry/workflow/access/ingest/chart/export subareas
- `src/main/java/org/base/api/service/publication/gate`
- storage/S3 areas
- target test package distribution

#### Important target classes read or traced

The following were read directly or traced sufficiently through imports, method
signatures, SQL and tests to support the findings below:

- `PlatformSchemaMigrationRunner`
- `StatisticalContractController`
- `StatisticalReferenceController`
- `StatisticalChartController` (untracked)
- `StatisticalContractConfiguration`
- `ReferenceRegistryService`
- `ContractWorkflow`
- statistical contract parser/compiler/model classes
- statistical workflow store/lifecycle classes
- `StatisticalLoadService`
- `WideRowNormalizer`
- `CanonicalObservationWriter`
- `CanonicalObservationReader` (untracked)
- `AccessAuthoringAdapter`
- statistical physical-planning classes
- `ChartService` (untracked)
- managed Access catalog/import services
- semantic Access ingestion services
- platform generic ingestion/materialization/publication service areas
- artifact package/run/stage service areas
- legacy importer/upload/SQL-server conversion areas
- authorization/tenancy policy classes relevant to statistical endpoints

#### External documents/evidence read

- `docs/platform-decisions.md`
- `docs/work/COMMON-STATISTICAL-CONTRACT-PLAN.md`
- `docs/work/STATISTICAL-CONTRACT-IMPLEMENTATION-CHECKLIST.md`
- `docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md`
- relevant statistical lifecycle/organization references linked by those files
- `docs/final-statistical-contract.md`
- `docs/final-unified-physical-virtual-contract.md`
- `docs/final-physical-database-architecture.md`
- `docs/unified-canonical-platform-final.md` status/context
- `docs/managed-access-package-v1.md`
- `docs/managed-access-semantic-package-v3.md`
- `docs/work/ARCHITECTURE-IMPROVEMENT-REGISTER.md`, especially
  `AIR-2026-047` through `AIR-2026-055`
- `docs/work/cards/statistical-contract-authority-alignment/governance.json`
- `docs/work/cards/access-package-composer/governance.json`
- `docs/work/ACCESS-PACKAGE-CANONICAL-STANDARD.md` (major content)
- `docs/work/ACCESS-PACKAGE-COMPOSER-DESIGN.md` (major content)
- `docs/work/ACCESS-PACKAGE-MASTER-CHECKLIST.md` (major content)
- `docs/decisions/ADR-access-package-integrity-automation.md`
- accepted tenancy and legacy-retirement ADR context
- statistical/access runtime evidence referenced in the findings below

#### External migration/persistence context inventoried

- `backend/core/src/main/resources/db/platform` migration set `000` through `112`
- Control, Data and Archive plane table families
- runner registration versus migration-file presence
- detailed content relevant to migrations `022`, `054`, `057`, `084`, `099`,
  `101`, `106` through `112`

### PARTIALLY_INSPECTED

- `docs/platform-capability-and-architecture-audit-2026-09-13.md`: major baseline
  and open gates captured; full uninterrupted pass not completed.
- `docs/work/ARCHITECTURE-IMPROVEMENT-REGISTER.md`: headings and relevant late
  entries captured; every older entry was not read in full.
- `SiteContractRevisionApprovalService`: output was truncated during a grouped read.
- `SiteContractRevisionRepository`: output was truncated during a grouped read.
- `CanonicalPageDataService`: output was truncated during a grouped read.
- `ContractPhysicalQueryService`: output was truncated during a grouped read.
- SQL migration chain: all files were inventoried and important migrations sampled,
  but every statement in all 113 files was not manually reviewed.
- Production Java corpus: all packages/files were inventoried and architecture
  owners sampled; not all 399 files were read line by line.
- Test corpus: all files were inventoried and architecture-relevant tests sampled;
  not all 122 tests were read line by line.
- Access package standard/composer/checklist: major decisions and status captured;
  grouped command output was truncated, so a future session must read each file
  separately from start to end.
- Current Gradle baseline: command interrupted before result.

### NOT_INSPECTED

These are not claims of irrelevance:

- a byte-complete review of every production and test source file
- complete method-by-method call graphs for every controller
- complete SQL/data-access injection and query-cost audit
- complete endpoint-to-test coverage matrix
- complete Spring configuration/profile matrix
- complete live deployment configuration and secret-source audit
- runtime database contents beyond evidence already committed to `docs/evidence`
- current production/dev runtime state
- frontend/mobile consumers of every API contract
- current external callers of legacy endpoints
- complete object-storage bucket/prefix compatibility inventory
- all generated JAR/class contents
- all local `.accdb` contents and provenance
- complete archive/restore/replay behavior
- performance execution against Q14 budgets
- real Microsoft Access compatibility beyond recorded external evidence
- official SDMX conformance validation
- a fresh clean-database migration replay for the present worktree
- a fresh full `:core:test :api:test` result for the present worktree
- review-range engineering-governance validation with an agreed base/head
- current `engineering-governance.py` result for a dedicated recovery card

## 5. Architecture inventory recovered so far

### 5.1 Runtime/build dependencies

Target technology/dependency inventory, **HIGH confidence**:

- Java 17
- Spring Boot 3.2.3
- Spring Web, Security, JPA, Validation, Actuator
- OpenTelemetry/OTLP
- Redis
- Hibernate
- SQL Server-oriented platform SQL and MySQL/SQL Server legacy strategies
- Jackcess 4.0.7
- Apache POI 5.4.0
- Commons CSV
- MinIO/S3-compatible object storage
- Apache Tika
- Avro/Parquet/Hadoop libraries
- external `:core` dependency and optional `:mobile` dependency
- build source sets that also compile selected Java tools from `ops`

### 5.2 Persistence topology

Recovered topology, **HIGH confidence**:

- **Control Plane DB:** `geostat-system`
- **Data Plane DB:** `geostat-data`
- **Archive Plane DB:** `geostat-archive`
- **Object storage:** S3-compatible storage for source/delivery artifacts
- SQL migration source authority currently lives outside the target under
  `backend/core/src/main/resources/db/platform`.
- The target's `PlatformSchemaMigrationRunner` discovers/registers and executes
  those external resources at API startup.

Approximate table-family inventory:

- Control: product, dataset/version, dimension, measure, metric, classifications,
  DSD/components/dataflow/unit, policies, validation, relations, ingestion
  contracts, site-contract definitions, generic contract structure/table/field,
  metadata assertions, provider capabilities, lifecycle/approval data, artifacts,
  statistical references/contracts/charts.
- Data: ingest batches/loads/checkpoints, raw records, entity tables, statistical
  series/observations/dimensions/attributes, geo/reference mirrors, publication,
  serving cache and audit.
- Archive: snapshot/record/artifact reference/payload pointer/retention.

### 5.3 Recovered architecture generations

1. **Pre-platform legacy** — **HIGH confidence**
   - domain-specific controllers for prices, trade, business/social datasets;
   - `ResponseController`, `MSSQLToAccess`, `XlsxToCsvController`;
   - `AccessFileImporter`, `FileUploadService`, MySQL/SQL Server strategies;
   - core JPA models such as `DataProfile`, `ImportTableMapping`, `PageNode`,
     `ChartDefinition`, `ImportJob`;
   - newly added legacy retirement controls are default-off/operator-only.

2. **Managed Access Package v1** — **HIGH confidence**
   - package tables `__gs_package`, `__gs_dataset`, `__gs_chart`,
     `__gs_chart_filter`;
   - imports into older/core JPA profile and mapping tables;
   - exposed through `/imports/access`.

3. **Semantic Access Package v3 and generic platform** — **HIGH confidence**
   - `service.platform.access` parses richer `__gs_*` metadata;
   - generic platform ingestion/materialization;
   - `platform.site_contract_*` contract and canonical page/query services;
   - metadata-driven query/introspection/client schema APIs.

4. **Artifact package subsystem** — **HIGH confidence**
   - typed artifact definitions and manifests;
   - upload sessions/object-store registration;
   - attachment relationships;
   - resumable package-run stages and release gates.

5. **Common statistical contract subsystem** — **HIGH confidence**
   - closed grammar, reference registry, compiler and semantic plan;
   - draft/review/approval workflow;
   - Access authoring adapter;
   - canonical wide-to-long load/writer;
   - SDMX-CSV/export and statistical chart grammar;
   - reuses much existing dataset/metric/dimension/statistics persistence while
     adding statistical references, contract drafts/events and charts.

6. **Package composer prototype/productization path** — **MEDIUM confidence**
   - untracked `service.platform.packaging` introduces `PackagePlan`,
     `PackageContractSource`, `JdbcPackageContractSource` and authoring policy;
   - intent is one plan consumed by generator and loader;
   - current code is incomplete and coexistence with the statistical plan and
     existing package models is unresolved.

## 6. Concepts, contracts and models discovered

### 6.1 Core platform concepts

| Concept | Recovered meaning/owner | Persistence/public form | Confidence |
|---|---|---|---|
| Product | Tenant-owned top-level governance/publication scope | `platform.data_product`; product-scoped APIs | HIGH |
| Dataset | Logical data family belonging to a product | `platform.dataset` | HIGH |
| Dataset version | Versioned implementation/contract binding for a dataset | `platform.dataset_version` | HIGH |
| Dataset snapshot | Immutable loaded candidate/release unit | Data Plane publication tables | HIGH |
| Publication | Product-level serving release containing snapshot members | publication tables/APIs | HIGH |
| Site contract revision | Approved declaration of site datasets, tables, fields, relations, pages and projections | `platform.site_contract_revision` family | HIGH |
| Ingestion contract | Versioned source/admission declaration | Control Plane ingestion tables | HIGH |
| Statistical DSD | Versioned structure defining ordered dimensions/measures/attributes and grain | `platform.statistical_dsd`, `statistical_component` | HIGH |
| Statistical reference | Exact versioned concept/measure/unit/codelist/policy/profile reference | new `platform.statistical_reference` | HIGH |
| Statistical contract draft | Closed JSON declaration compiled into a semantic plan and approved by digest | new draft/event tables and workflow API | HIGH |
| Semantic plan | Compiler result used by Access generation/load/serving | currently reconstructed at runtime; durable authority unresolved | HIGH |
| Metric | Dataset-bound measure/series identity reused by the statistical writer | `platform.metric`, Data Plane `series.metric_id` | HIGH |
| Codelist/classification | Versioned controlled codes and hierarchy | `platform.classification_*` | HIGH |
| Observation | Value/status/time at a dimension tuple and measure | `statistics.observation` plus dimension/attribute tables | HIGH |
| Artifact | Typed object with identity, policy, lineage and retention | artifact Control tables + object storage | HIGH |
| Package run | Resumable staged ingestion orchestration | `ingest.artifact_package_run` and stage history | HIGH |
| Page contract | Metadata-defined serving/query/projection contract | site-contract page/binding tables and page APIs | HIGH |
| Provider capability | Declared adapter/provider limits and features | `platform.provider_capability` | HIGH |

### 6.2 Contract families found

- M3 engineering doctrine and requirement IDs.
- M2 statistical JSON grammar implemented in Java parser/compiler.
- A separate untracked JSON Schema 2020-12 representation of that grammar.
- Site-contract revision declarations.
- Generic physical contract structures/tables/fields/relations.
- Ingestion-contract revisions and source bindings.
- Managed Access Package v1 contract.
- Semantic Access Package v3 contract.
- Artifact manifest/package contract.
- Statistical authoring Access package contract/stamp.
- Chart contracts in three separate model families.
- Security authority properties and `StatisticalContractAccessDecision`.
- Publication/release-gate policies.
- Tenancy/owner scope contracts.

The ownership and precedence between several of these families remain unresolved.

## 7. Runtime and data flows recovered

### 7.1 Statistical contract lifecycle

Recovered flow, **HIGH confidence**:

```text
JSON draft
 -> closed grammar parse
 -> exact statistical-reference resolution
 -> semantic compilation
 -> DRAFT persistence/version + digests
 -> REVIEW_REQUIRED
 -> four-eyes APPROVED receipt bound to revision digest
 -> Access authoring generation and/or governed load
```

Current lifecycle states include `DRAFT`, `REVIEW_REQUIRED`, `APPROVED`,
`SUPERSEDED`, `ROLLED_BACK`. An explicit withdrawal operation is absent even
though an incorrect approved contract may need retirement.

### 7.2 Statistical load flow

Recovered `StatisticalLoadService` flow, **HIGH confidence**:

```text
validate file
 -> normalize wide rows
 -> bind/reuse Control Plane dataset/version/metrics/dimensions
 -> idempotency check by artifact checksum + dataset version
 -> write source object to object storage
 -> one Data Plane transaction:
      ingest batch/artifact/load
      REVIEW_REQUIRED dataset snapshot
      staged/raw rows
      statistical series/observations/dimensions/attributes
```

- Only full-snapshot mode is currently accepted; other mutation modes return an
  explicit unsupported response.
- Cross-plane operation is not atomic. Control definitions and object bytes can
  survive a later Data Plane failure. Definitions are intended to be idempotent;
  orphan objects rely on sweep/reconciliation.
- A durable operation/outbox architecture is documented as desired for larger or
  asynchronous work, but the present bounded synchronous load does not implement
  the full Q41 job pattern.

### 7.3 Canonical statistical write/read

Writer, **HIGH confidence**:

- unpivots a wide row into series/observations;
- series identity includes structure, measure and sorted dimension tuple;
- writes exact `BigDecimal` values, period bounds, status, dimensions and declared
  attributes;
- repeated identical values are idempotent;
- a changed value at the same semantic key is treated as conflict.

Reader, **HIGH confidence**:

- chooses the latest snapshot with status `PUBLISHED`;
- reconstructs wide output from observation numeric values and dimensions;
- adds non-normal observation status;
- currently does **not** read `statistics.observation_attribute`.

### 7.4 Access authoring

Recovered flow, **HIGH confidence**:

- semantic plan -> physical plan;
- Jackcess creates ACCDB tables, typed fields, lookup/codelist data, contract copy,
  stamp and navigation metadata;
- physical planner may vertically split by provider column limit while repeating
  keys/dimensions;
- real Microsoft Access finishing is required for forms/queries/macros not writable
  by Jackcess;
- server load must not trust embedded provenance or executable artifacts.

### 7.5 Site/page serving

Recovered at package/service level, **MEDIUM confidence** because four key classes
were only partially read:

```text
site contract/page binding
 -> contract-aware physical query compiler
 -> entity/raw/statistics source execution
 -> projection/include/policy/tenant enforcement
 -> canonical page response, cursor/keyset or aggregation
```

This flow must be completed by a fresh reading of
`CanonicalPageDataService` and `ContractPhysicalQueryService`.

### 7.6 Schema migration

Recovered flow, **HIGH confidence**:

```text
API startup
 -> target runner's explicit ordered registration
 -> external core SQL resource
 -> plane-specific connection
 -> custom SQL splitting/execution
 -> checksum/application ledger
```

Some pre-existing schemas can be adopted via probes. The runner contains explicit
checksum rewrite exceptions for historically changed migration bodies.

## 8. Recovered intent and candidate invariants

These are candidate entries for a future invariant registry. `CONFIRMED` means
the statement has direct doctrine/contract/code evidence; it does not mean all
implementations conform.

### INV-001 — Authority-chain direction

- **Statement:** M0 runtime data must conform to an approved M1 declaration that
  conforms to M2 grammar and M3 semantics.
- **Status:** CONFIRMED
- **Confidence:** HIGH
- **Evidence:** engineering doctrine/architecture/requirements.
- **Known violation risk:** statistical contracts are recompiled against mutable
  reference lifecycle at read/use time rather than executing an immutable approved
  dependency closure.

### INV-002 — One semantic authority

- **Statement:** one business meaning has one canonical owner; projections/adapters
  must not become competing sources of truth.
- **Status:** CONFIRMED
- **Confidence:** HIGH
- **Evidence:** doctrine, open-question register rules Q29/Q32/Q40.
- **Known violations:** overlapping contract, chart and package representations.

### INV-003 — Closed grammar, open registry

- **Statement:** new site/provider/data-family values extend approved registries,
  not generic core-code branches.
- **Status:** CONFIRMED
- **Confidence:** HIGH
- **Evidence:** statistical decision register, doctrine.
- **Known violation:** KIDS-specific content in generic migration `109` and large
  KIDS seed declarations in migration `022`.

### INV-004 — Exact identity and reference pinning

- **Statement:** semantic references use exact kind/namespace/code/version identity;
  latest/range/wildcard references are rejected; approvals bind the resolved
  dependency closure.
- **Status:** CONFIRMED as intent; implementation completeness UNVERIFIED
- **Confidence:** HIGH for intent, MEDIUM for complete enforcement
- **Evidence:** Q30, compiler/reference code, workflow tests.

### INV-005 — Lossless statistical value semantics

- **Statement:** decimal value, period and value/status pair must survive without
  silent rounding, double conversion or missing-status invention.
- **Status:** CONFIRMED
- **Confidence:** HIGH
- **Evidence:** Q22, Q23, Q34, migrations 111/112, runtime evidence.
- **Unresolved boundary:** canonical numeric precision/scale authority is currently
  contradictory.

### INV-006 — Observation identity

- **Statement:** one wide row represents one ordered dimension tuple; canonical
  storage has one observation per tuple and measure; measure is not itself a
  dimension key component.
- **Status:** CONFIRMED
- **Confidence:** HIGH
- **Evidence:** Q17/Q33 and canonical writer.

### INV-007 — Null value requires status

- **Statement:** a null observation value must carry an explicit observation status;
  zero is a real numeric value.
- **Status:** CONFIRMED
- **Confidence:** HIGH
- **Evidence:** Q22, normalizer and KIDS migration evidence.

### INV-008 — Publication requires explicit governed approval

- **Statement:** no snapshot or data set may become live without approval of exactly
  what is published.
- **Status:** CONFIRMED
- **Confidence:** HIGH
- **Evidence:** doctrine, publication ADRs, release-gate code and AIR-2026-054.
- **Known violation:** product publication currently sweeps all
  `REVIEW_REQUIRED` snapshots.

### INV-009 — Tenant ownership and deny-by-default

- **Statement:** every product has one owner tenant; authorization and tenant scope
  are rechecked at execution; cross-tenant access is denied absent explicit grant.
- **Status:** CONFIRMED
- **Confidence:** HIGH
- **Evidence:** ADR-010, tenancy policy/guard classes, Q08/Q44.

### INV-010 — Immutable/versioned migrations

- **Statement:** applied migrations are append-only/versioned and checksum mismatch
  fails closed unless an explicitly governed repair/adoption procedure exists.
- **Status:** PROBABLE/authority conflict
- **Confidence:** HIGH for doctrine, MEDIUM for intended exception policy
- **Evidence:** engineering doctrine and migration runner.
- **Known conflict:** runner embeds checksum-rewrite exceptions for changed migration
  bodies.

### INV-011 — Provider artifacts are adapters

- **Statement:** Access, CSV and SDMX are adapters/projections; canonical semantics
  remain provider-neutral and server-authoritative.
- **Status:** CONFIRMED
- **Confidence:** HIGH
- **Evidence:** Q07/Q34/Q36/Q47, Access integrity ADR.

### INV-012 — Legacy removal is gated

- **Statement:** default-off plus operator-only legacy controls are not sufficient
  for removal; consumer inventory, behavior parity, evidence and rollback are
  required.
- **Status:** CONFIRMED
- **Confidence:** HIGH
- **Evidence:** legacy retirement ADR/governance and Q15/Q50.

## 9. Contradiction register (current)

### CF-001 — Accepted ADR identifier collision

- **Resolution after checkpoint:** RESOLVED on 2026-09-20 by retaining legacy
  retirement as `ADR-011` and assigning Access package integrity the unique
  `ADR-012` identity. `docs/reference/engineering/decision-registry.json` is now the
  machine-readable identity registry. The evidence below is retained to explain
  why the control was introduced.

- **Evidence A:** `docs/platform-decisions.md` uses ADR-011 for legacy surface
  retirement.
- **Evidence B:** `docs/decisions/ADR-access-package-integrity-automation.md` is also
  titled accepted ADR-011 and additionally records Access header and numeric
  decisions.
- **Impact:** decision references in code, migrations, cards and plans are ambiguous.
- **Risk:** HIGH.
- **Confidence:** HIGH.
- **Required decision:** renumber with stable aliases and repository-wide reference
  migration; do not infer which ADR-011 a reference means.

### CF-002 — Mutation-mode decision contradicts its own changelog/runtime

- **Evidence A:** Q11 table in
  `docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md` declares
  `REPLACE_SCOPE`, `UPSERT` default and `DELETE`.
- **Evidence B:** the same file's change log says v1 is `FULL_SNAPSHOT` only and
  other modes are explicitly refused; runtime follows the latter.
- **Impact:** contract and test expectations are not uniquely defined.
- **Risk:** HIGH.
- **Confidence:** HIGH.

### CF-003 — Numeric envelope has competing authorities

- **Evidence A:** Q34 declares Access/SQL precision <= 28 and
  `DECIMAL(28,10)`.
- **Evidence B:** Access integrity ADR D-3 says scale 16 supersedes 28,10 for
  migrated data.
- **Evidence C:** migrations 111/112 widen model/storage to scale 16 and data
  observation/cache to `DECIMAL(38,16)`.
- **Evidence D:** dirty configuration defaults to max scale 16.
- **Impact:** compiler/provider/storage compatibility and rounding behavior are not
  governed by one authority.
- **Risk:** HIGH.
- **Confidence:** HIGH.

### CF-004 — Approved declaration depends on mutable registry state

- **Evidence A:** doctrine/Q30/Q39 require exact resolved dependency closure pinned
  and digested at approval.
- **Evidence B:** `ContractWorkflow` recompiles the approved document against the
  current registry; superseding references can make an approved contract no longer
  compile.
- **Impact:** M1 is not an immutable executable authority; availability and meaning
  can change without a contract revision.
- **Risk:** HIGH.
- **Confidence:** HIGH.

### CF-005 — Statistical attributes are written but not fully served

- **Evidence A:** writer persists declared observation/measure attributes into
  `statistics.observation_attribute`.
- **Evidence B:** `CanonicalObservationReader` does not read that table.
- **Impact:** contract-declared values disappear from chart/read/export surfaces.
- **Risk:** HIGH where such attributes are used.
- **Confidence:** HIGH.

### CF-006 — Site contract and statistical contract are parallel M1 authorities

- **Evidence A:** site-contract revision/approval defines datasets, structures,
  fields, relations, pages and bindings.
- **Evidence B:** statistical workflow independently approves drafts and binds
  dataset versions/metrics.
- **Evidence C:** no inspected integration made the statistical contract a child of
  or explicit peer to site-contract approval/page binding.
- **Impact:** ownership of dataset semantics and serving binding is unclear.
- **Risk:** HIGH.
- **Confidence:** MEDIUM-HIGH; final integration scan is still required.

### CF-007 — Publication request scope differs from publication effect

- **Evidence:** AIR-2026-054 reports a request naming one snapshot caused all
  product `REVIEW_REQUIRED` snapshots to become members/status `PUBLISHED`, displaced
  unchanged members, and left status residue after rollback.
- **Impact:** approver does not control exactly what becomes live.
- **Risk:** CRITICAL/P0.
- **Confidence:** HIGH from runtime evidence; code re-read still required.

### CF-008 — Statistical controller authorization/read semantics are inconsistent

- **Evidence A:** governance card says HTTP method security was aligned with
  configurable `StatisticalContractAccessDecision`; ordinary read uses existing
  `READ_RESOURCE` boundary.
- **Evidence B:** `ContractWorkflow.get` permits actors carrying
  AUTHOR/APPROVE/IMPORT roles, not a generic read role.
- **Evidence C:** untracked chart controller lacked visible `@PreAuthorize` during
  inspection, while internal decision methods use author-like permissions.
- **Impact:** a reader may pass HTTP and fail workflow, or a chart path may omit the
  intended boundary.
- **Risk:** HIGH security/compatibility.
- **Confidence:** MEDIUM-HIGH; must re-read final dirty sources and tests.

### CF-009 — Grammar has two representations without parity authority

- **Evidence A:** Java parser/compiler is executable M2 grammar.
- **Evidence B:** untracked JSON Schema exists.
- **Evidence C:** implementation checklist says schema-vs-parser equivalence remains
  open.
- **Impact:** two grammars can accept different documents.
- **Risk:** HIGH.
- **Confidence:** HIGH.

### CF-010 — Provider capability claims omit scale-specific capability

- **Evidence A:** provider capability records cover limits such as precision,
  columns, names and index fields.
- **Evidence B:** Access adapter/compiler numeric behavior uses scale rules, but no
  inspected provider capability dimension explicitly represents max scale.
- **Impact:** capability negotiation may approve a plan a provider cannot represent
  exactly.
- **Risk:** MEDIUM-HIGH.
- **Confidence:** MEDIUM; full registry scan needed.

### CF-011 — KIDS-specific declarations exist in generic migration/core path

- **Evidence:** migration 109 applies KIDS carrier/raw-document serving-policy
  decisions; migration 022 combines generic schema with extensive KIDS seeds.
- **Impact:** generic schema evolution and site declaration are coupled.
- **Risk:** HIGH for rebuild/reuse.
- **Confidence:** HIGH.

### CF-012 — Migration immutability versus checksum rewrites

- **Evidence:** runner has hardcoded checksum replacement for migrations
  `011`, `020`, `021`, `055`, `065`, `069`, `070`, `079`, `080`, `097`.
- **Impact:** applied history can be made to match rewritten sources without a
  separate governed repair artifact.
- **Risk:** HIGH audit/rebuild risk.
- **Confidence:** HIGH.

### CF-013 — Duplicate migration object creation

- **Evidence:** `platform.contract_page_binding` is created in both migrations 054
  and 057.
- **Interpretation:** likely historical repair/idempotent replacement, not yet proven.
- **Risk:** MEDIUM.
- **Confidence:** HIGH that duplication exists; LOW on intended classification.

### CF-014 — Approved contract has no withdrawal path

- **Evidence:** AIR-2026-055; incorrect approved 43-measure contract remains
  approved after all pinned references were superseded. Workflow only supersedes
  via successor for the same key.
- **Impact:** known-wrong authority cannot be cleanly retired.
- **Risk:** MEDIUM-HIGH.
- **Confidence:** HIGH.

### CF-015 — Attribute attachment model cannot express code-dependent constants

- **Evidence:** AIR-2026-052; `DIMENSION_GROUP` is treated as dataset-constant and
  not as varying per dimension combination, forcing unit per observation.
- **Impact:** repetition, author burden and incomplete SDMX attachment semantics.
- **Risk:** MEDIUM.
- **Confidence:** HIGH.

### CF-016 — Classification growth lacks governed API

- **Evidence:** AIR-2026-053; binding to a classification version exists but scheme,
  version and item creation required direct SQL for KIDS.
- **Impact:** “open registry” cannot be operated safely; manual SQL bypasses approval.
- **Risk:** HIGH governance/data integrity.
- **Confidence:** HIGH.

### CF-017 — Navigation metadata is not proven portable

- **Evidence:** AIR-2026-051; generated file was structurally readable by Jackcess
  but opened with an empty Access navigation pane. Two candidates awaited user
  verification.
- **Impact:** technically valid artifact can appear damaged to authors.
- **Risk:** MEDIUM delivery/usability.
- **Confidence:** HIGH that issue occurred; resolution UNVERIFIED.

### CF-018 — Statistical measure-reference reconstruction is fragile

- **Evidence:** reader matching uses wire-reference suffix/code/version logic and an
  initial dataset-code lookup before later product scoping.
- **Impact:** ambiguous code/version combinations may bind incorrectly.
- **Risk:** MEDIUM-HIGH.
- **Confidence:** MEDIUM; needs negative characterization tests.

### CF-019 — Input normalization may erase lexical information

- **Evidence:** `WideRowNormalizer` trims string inputs globally.
- **Impact:** desired canonical trimming for codes may unintentionally alter bounded
  text or lineage values.
- **Risk:** MEDIUM.
- **Confidence:** MEDIUM; intended text semantics are UNVERIFIED.

### CF-020 — Migration registration is a second manifest

- **Evidence:** external SQL resources exist in core while target runner maintains
  an explicit ordered registration list and exclusions.
- **Impact:** adding a file does not ensure it is executed; runner/file chain can
  drift, as documented in AIR-2026-027.
- **Risk:** HIGH rebuildability.
- **Confidence:** HIGH.

## 10. Duplication / parallel-architecture register

### PA-001 — Access/package representations

- Managed Package v1 (`__gs_package/dataset/chart/filter`).
- Semantic Package v3 (`__gs_package/dataset/field/key/relation/projection/...`).
- Artifact manifest/package-run envelope.
- Statistical authoring file with contract copy/stamp and DSD-shaped tables.
- New composer `PackagePlan` representation.
- **Classification:** partial migration plus intentional specialization; exact
  boundaries unresolved.
- **Do not merge blindly:** transport envelope, semantic declaration, authoring
  view and artifact manifest are related but not identical responsibilities.

### PA-002 — Visualization authorities

- core `ChartDefinition` / managed-package chart model;
- `platform.visualization_definition` generic metric chart;
- new `platform.statistical_chart` grammar/table/service.
- **Classification:** historical replacement plus possible intentional statistical
  specialization.
- **Gap:** migration 110 comments imply conversion/proposal of legacy charts, but no
  converter was located.

### PA-003 — Contract authorities

- ingestion contract;
- site contract revision;
- generic contract structure/table/field/relation model;
- statistical contract draft/semantic plan;
- Access package declarations.
- **Classification:** partly distinct bounded contracts, partly overlapping source
  of truth; requires an explicit authority map.

### PA-004 — Ingestion paths

- domain-specific legacy imports;
- managed Access v1 import;
- generic row ingestion;
- semantic Access ingestion;
- scheduled SQL ingestion;
- artifact package-run pipeline;
- statistical authoring load.
- **Classification:** many adapters are legitimate specializations, but admission,
  orchestration, receipts, idempotency and lifecycle logic are duplicated.

### PA-005 — Lifecycle/state machines

- `ContractLifecycle` plus persisted store;
- `DataFamilyLifecycle`, including an in-memory default constructor;
- site-contract approval lifecycle;
- statistical `ContractWorkflow`;
- artifact package-run lifecycle;
- API operation lifecycle;
- publication/snapshot lifecycle.
- **Classification:** mostly intentional bounded lifecycles, with duplicated
  transition/CAS/audit vocabulary and dangerous fallback persistence.

### PA-006 — Reference/classifier mechanisms

- classification scheme/version/item/hierarchy/alias;
- classifier proposal registry;
- new statistical-reference registry;
- Access-local codelist/alias projections.
- **Classification:** intended registry plus projections, but proposal/approval and
  exact-reference ownership are incomplete.

### PA-007 — Query/serving mechanisms

- domain-specific legacy controllers/repositories;
- dynamic page/query services;
- generic contract physical query compiler;
- canonical page data service;
- statistical canonical observation reader/chart service;
- SDMX facade/export codecs.
- **Classification:** historical replacement plus specialized presentation adapters;
  consumer migration and parity are not documented end to end.

## 11. Legacy mechanisms and incomplete migrations

### Confirmed legacy candidates

- domain-specific controllers and import/conversion endpoints;
- `MSSQLToAccess` caller-supplied connection behavior;
- `AccessFileImporter` and older JPA profile/mapping path;
- Managed Access Package v1;
- KIDS carrier/cell-ordinal/JSON-path/DOUBLE statistical model;
- carrier-bound `statistical_semantic_binding`;
- core `ChartDefinition` where superseded by contract-driven charts;
- fixed `title_ka/title_en` registry fields where BCP-47 metadata assertions are
  intended for new semantics.

Removal readiness for all items is **NOT ESTABLISHED**.

### Partial migrations

1. **Legacy KIDS statistics -> common statistical contract**
   - First modeled as 43 measures; later corrected to one measure plus INDICATOR
     dimension.
   - Earlier contract/snapshots remain as residue; one wrong contract cannot be
     withdrawn.
   - New correct snapshot was loaded but release behavior is unsafe.

2. **Managed/semantic Access -> canonical composer**
   - Prototype and plan exist; product writer/loader parity and second-site proof are
     incomplete.
   - Existing R8 and new candidate carry different table sets and contract status.

3. **Chart model migration**
   - New statistical chart table/grammar exists; legacy conversion path not found.

4. **Site contract approval governance**
   - migration 099 and approval service add governed approval;
   - parallel `platform.contract_revision_lifecycle` from migration 084 is reportedly
     not written by that path.

5. **Migration chain repair**
   - historical migrations are excluded/adopted/checksum-rewritten;
   - AIR-2026-027 reports the registered chain could not rebuild KIDS R8 at one
     point; current resolution is UNVERIFIED.

6. **Publication release semantics**
   - gates exist and are evaluated, but membership selection/status rollback remain
     inconsistent.

7. **Legacy endpoint retirement**
   - default-off/operator-only controls exist;
   - full consumer inventory, parity and removal gates are not complete.

## 12. Documentation/code/test inconsistencies

- `COMMON-STATISTICAL-CONTRACT-PLAN.md` originally says implementation NOT READY;
  checklist later marks broad dev completion while release remains NOT READY.
- `STATISTICAL-CONTRACT-OPEN-QUESTIONS.md` says all 50 questions have decisions but
  nine defaults still require owner/steward confirmation.
- The same decision register contains the Q11 mutation contradiction.
- Q34 numeric rules are superseded elsewhere without a clean unique ADR chain.
- “Final” documents dated 2026-09-15 coexist:
  - KIDS-specific statistical contract;
  - normative physical/virtual contract;
  - physical database architecture;
  - a document explicitly marked replaced.
  They landed together and cannot be ranked solely by filename or recency.
- `managed-access-package-v1.md` and `managed-access-semantic-package-v3.md`
  document coexisting generations; neither alone describes current runtime.
- `docs/three-database-physical-dictionary.md` appears stale relative to migrations
  086-112. This is `UNVERIFIED` until a full comparison.
- Runtime evidence documents refer to development data and historical commits; they
  do not certify the present dirty worktree or production readiness.
- `AIR-2026-047` says no parallel registry/table family was created, yet a new
  statistical-reference table and separate statistical draft workflow now coexist
  with older registries/contracts. This may be a legitimate extension but the claim
  requires a sharper semantic boundary.
- Governance evidence says default authorization regression passed but explicitly
  leaves non-default configured-authority evidence open.
- Full present-worktree regression was not completed in this recovery session.

## 13. Sources of truth currently discovered

### Sufficiently supported

- M3 platform doctrine: `docs/reference/engineering/*` plus quality doctrine.
- Physical schema source: versioned SQL under
  `backend/core/src/main/resources/db/platform`, subject to the migration integrity
  conflicts already recorded.
- Tenant ownership: product owner tenant plus tenancy ADR/policy enforcement.
- Statistical runtime values: Data Plane series/observation/dimension/attribute
  tables, snapshot-bound.
- Object bytes: object storage with registry identity/digest records.
- Site serving declarations: approved site-contract revision tables.
- Artifact definitions/policies: Control Plane artifact registry.

### Ambiguous or competing

- executable approved statistical plan: stored document/digests versus live
  recompilation;
- dataset semantic authority: site contract versus statistical contract;
- contract grammar: Java parser versus JSON Schema;
- chart authority: three chart models;
- package plan authority: site contract, Access metadata, statistical plan and new
  composer plan;
- migration manifest: resource set versus explicit runner list;
- numeric envelope: decision register versus Access ADR/migrations/config;
- ADR numbering/catalog authority.

## 14. Domain/business knowledge that must not be lost

1. KIDS legacy published statistics contain 43 indicator series over year and age
   group, reconciled to 880 rows/observations in the corrected model.
2. The correct common model is one `OBS_VALUE` measure with INDICATOR as a
   dimension, not 43 measures.
3. The approved R8 Access package contains readable Georgian titles and units that
   were missing from Control Plane registry state; those labels are business
   knowledge, not disposable presentation noise.
4. 221 of 880 legacy values carry 11-16 decimal places. A 10-place scale silently
   loses source information.
5. 3,291 empty cells in the discarded 43-measure pivot required explicit statuses;
   empty is not zero and status must not be invented.
6. Of the legacy metrics, 32 had `SUM` semantics in one migration exercise, while
   the later approved R8 interpretation says the 43 indicator values may not be
   aggregated. This apparent conflict must be reconciled by exact model/version;
   do not flatten it into one rule.
7. Units vary by INDICATOR. Current attachment grammar cannot express this cleanly
   without repetition.
8. Age-group classification version 6 was the version used by the published legacy
   snapshot and was intentionally pinned during reconciliation.
9. Access authors require a file that visibly opens, exposes fillable tables/forms,
   provides lookup guidance and protects system/lineage areas. Structural Jackcess
   readability alone is insufficient acceptance.
10. Access package provenance automation is permitted only in a closed generated
    shape, never trusted by the server and never allowed to become business logic.
11. Legacy R8, its contract and published snapshot must remain untouched until the
    new package has contract revision, semantic mapping, parity, consumer migration
    and rollback evidence.
12. Publication incident residue may include snapshots 53 and 57-61 marked
    `PUBLISHED` without current publication membership. Treat live state as needing
    reconciliation, not cleanup by assumption.
13. Development has historical restart-generated DRAFT dataset-version residue;
    cleanup requires backup and a reviewed migration.
14. The operator service identity historically combined author, publish and admin
    powers, weakening separation of duties even where four-eyes contract approval
    existed.

## 15. Decisions sufficiently supported so far

These are recovery conclusions, not new ADRs.

### DR-REC-001 — Preserve the M3->M0 authority chain

- **Decision:** any rehabilitation must restore declaration-before-instance and
  make execution consume an immutable approved semantic result.
- **Confidence:** HIGH.
- **Evidence:** mandatory doctrine plus statistical decision Q30/Q39.

### DR-REC-002 — Converge; do not add another contract/package abstraction

- **Decision:** first assign ownership among existing site, physical, statistical,
  artifact and package contracts. A new umbrella model is not justified.
- **Confidence:** HIGH.
- **Evidence:** one-authority doctrine and observed parallel representations.

### DR-REC-003 — Keep provider semantics outside canonical domain semantics

- **Decision:** Access/CSV/SDMX remain adapters; provider limits are compiled
  capability constraints, not domain branches.
- **Confidence:** HIGH.
- **Evidence:** Q07, Q34-Q36, Q47 and adapter implementation.

### DR-REC-004 — Use incremental expand/migrate/contract

- **Decision:** protect behavior, introduce/repair canonical authority, migrate
  consumers, validate three gates, then retire legacy.
- **Confidence:** HIGH.
- **Evidence:** Q50, legacy governance and doctrine.

### DR-REC-005 — Do not publish or remove anything during rehabilitation discovery

- **Decision:** publication behavior is currently unsafe and legacy knowledge is not
  fully inventoried.
- **Confidence:** HIGH.
- **Evidence:** AIR-2026-054 and incomplete consumer inventory.

### DR-REC-006 — Statistical attributes are part of canonical behavior

- **Decision:** attributes written under an approved contract must be included in
  read/export/chart behavior or explicitly rejected before load; silent omission is
  invalid.
- **Confidence:** HIGH.
- **Evidence:** schema, writer, reader and no-silent-loss doctrine.

### DR-REC-007 — Release readiness remains NOT READY

- **Decision:** no inspected evidence supports a production-ready claim for the
  present dirty worktree.
- **Confidence:** HIGH.
- **Evidence:** dirty source, incomplete baseline, open P0 publication defect,
  uncommitted migrations/classes, historical audit release gates.

## 16. Unresolved architectural questions

1. Is the statistical contract a child component of a site-contract revision, an
   independent dataset contract referenced by the site contract, or the canonical
   replacement for part of the site contract?
2. What persisted artifact is the immutable approved semantic plan and dependency
   closure? Is recompilation allowed only as verification, never as reinterpretation?
3. Which representation generates the other: Java grammar or JSON Schema?
4. What is the single numeric envelope across semantic definition, Access provider,
   SQL storage and API serialization: precision 28/scale 16, precision 38/scale 16,
   or a provider-specific subset of a wider canonical envelope?
5. What migration/rounding compatibility rule applies to already approved 28,10
   contracts?
6. What is the exact responsibility boundary among Access semantic declaration,
   artifact manifest, authoring layout and composer `PackagePlan`?
7. Which chart model is canonical for generic pages and which, if any, is an
   intentional statistical specialization?
8. How are legacy charts converted, proposed, approved and consumer-migrated?
9. Should attributes varying by a dimension code be modeled as dimension-group
   attachment values, codelist-item attributes or another existing canonical
   primitive?
10. What governed classification proposal/approval API owns scheme/version/item
    creation?
11. What should `WITHDRAWN` mean relative to `SUPERSEDED` and `ROLLED_BACK`?
12. Does ordinary read authority permit retrieving approved statistical contracts
    and charts, independently of author/import/approve authority?
13. How should publication select explicit candidate members, carry forward
    unchanged members, reject candidates and repair snapshot status residue?
14. Must statistical read resolve through current publication membership rather
    than snapshot status? Evidence strongly suggests yes, but implementation impact
    is not yet mapped.
15. What is the governed policy for historically rewritten migration checksums?
16. Is explicit runner registration retained as canonical manifest, generated from
    migration metadata, or eliminated in favor of validated discovery?
17. Which unregistered migrations are intentionally superseded and which represent
    chain drift?
18. Are all current SQL identifier constructions derived only from approved metadata,
    or can a caller influence physical identifiers/expressions?
19. Is global string trimming intentional for every statistical text representation?
20. How are provider max scale, real Access versions and vertical-split constraints
    modeled and tested?
21. Which legacy endpoints have legitimate live consumers, and what exact behavior
    must be protected?
22. Which “final” architecture document is accepted authority versus historical
    evidence?

## 17. Assumptions requiring verification

- `UNVERIFIED`: new `StatisticalChartController` is actually registered in the final
  Spring runtime and not protected indirectly by a class/global rule.
- `UNVERIFIED`: no converter exists for legacy chart definitions.
- `UNVERIFIED`: no later integration binds statistical approval into site-contract
  approval/page binding.
- `UNVERIFIED`: provider capability registry lacks a max-scale feature rather than
  representing it under an uninspected generic key.
- `UNVERIFIED`: reader reference matching can become ambiguous in actual registered
  data.
- `UNVERIFIED`: global trimming alters business-significant text in a supported
  profile.
- `UNVERIFIED`: migration 054/057 duplication is a deliberate repair.
- `UNVERIFIED`: the current registered migration chain can rebuild all three planes
  from empty databases.
- `UNVERIFIED`: the latest dirty source passes all core/API tests.
- `UNVERIFIED`: historical dev evidence corresponds exactly to the current file
  digests.
- `UNVERIFIED`: fixed localized title columns are frozen for every new write path.
- `UNVERIFIED`: all artifact/package object-store operations have bounded retry,
  timeout and orphan cleanup coverage.

## 18. Evidence map for important conclusions

| Conclusion | Primary evidence |
|---|---|
| M3->M0 authority and one owner | `docs/reference/ENGINEERING-QUALITY-DOCTRINE.md`; `docs/reference/engineering/ARCHITECTURE.md`; `REQUIREMENTS.md` |
| Three database topology | `docs/platform-decisions.md` ADR-001; migrations 001/002/003; datasource/runner code |
| Site-contract authority | migration 022; migration 099; `SiteContractRevisionApprovalService`; controller/repository |
| Statistical contract intent | `docs/work/COMMON-STATISTICAL-CONTRACT-PLAN.md`; `STATISTICAL-CONTRACT-OPEN-QUESTIONS.md`; statistical compiler/workflow code |
| Statistical runtime status | `docs/work/STATISTICAL-CONTRACT-IMPLEMENTATION-CHECKLIST.md`; `docs/evidence/statistical-contract-runtime-2026-09-19.json`; AIR-2026-047 |
| Correct KIDS indicator model | AIR-2026-050 correction; `docs/evidence/kids-indicator-model-2026-09-20.json`; Access composer evidence |
| Numeric conflict | Q34; Access integrity ADR D-3; migrations 111/112; numeric-envelope evidence |
| Publication defect | AIR-2026-054; `docs/work/evidence/kids-indicators-exact-end-to-end-2026-09-20.json` |
| Wrong contract cannot withdraw | AIR-2026-055; `ContractWorkflow` transitions |
| Attribute read loss | `CanonicalObservationWriter`; `CanonicalObservationReader`; Data Plane schema |
| Mutable approved plan | `ContractWorkflow`; `ReferenceRegistryService`; Q30/Q39 |
| Grammar duplication | parser/compiler; untracked statistical JSON Schema; checklist open item |
| Access generation constraints | `AccessAuthoringAdapter`; Q34-Q36; Access standard/composer docs |
| Package representations | managed Access v1/v3 docs; artifact services; statistical adapter; packaging prototype |
| Chart representations | core `ChartDefinition`; migration/platform visualization model; migration 110/statistical chart service |
| Migration checksum exceptions | `PlatformSchemaMigrationRunner` |
| ADR collision | `docs/platform-decisions.md`; `docs/decisions/ADR-legacy-surface-retirement.md`; `docs/decisions/ADR-access-package-integrity-automation.md` |
| Release NOT READY | dirty Git baseline; incomplete current tests; open AIR entries; capability audit |

## 19. Areas still requiring investigation

### Highest priority

1. Finish site approval and serving flow by reading, separately and completely:
   - `SiteContractRevisionApprovalService`
   - `SiteContractRevisionRepository`
   - `CanonicalPageDataService`
   - `ContractPhysicalQueryService`
2. Trace statistical contract identity into site-contract bindings, publication and
   page/chart serving, proving whether CF-006 is real or already bridged.
3. Read publication writer/service code corresponding to AIR-2026-054 and map the
   exact transaction/status/member behavior.
4. Establish the final dirty-source authorization behavior for statistical
   contracts/charts with controller, internal policy and tests.
5. Complete migration-chain audit, including every excluded/unregistered file,
   checksum exception and fresh-build evidence.

### Then

6. Read every Access package standard/composer/checklist file separately to EOF.
7. Build an endpoint-to-contract-to-policy-to-test matrix.
8. Build an exact concept/contract/persistence/consumer matrix.
9. Complete query-identifier and cost-control security audit.
10. Inventory frontend/mobile/external consumers and classify legacy behavior.
11. Reconcile documentation authority and decision numbering.
12. Run fresh non-destructive baseline checks and capture exact reports/digests.
13. Only after those steps, construct the requested contradiction, invariant,
    decision, migration and legacy-elimination registers in final form.

## 20. Current analysis progress

- Repository/target structural discovery: **approximately 90%**.
- Major concept and subsystem discovery: **approximately 80%**.
- Detailed end-to-end flow recovery: **approximately 65%**.
- Contradiction/parallel architecture discovery: **approximately 70%**.
- Test/behavior classification: **approximately 40%**.
- Consumer inventory: **approximately 20%**.
- Runtime/production verification: **below 20%**.
- Dependency-ordered rehabilitation master plan: **not started/frozen**; producing it
  now would be premature.

These percentages are orientation only, not completion claims.

## 21. Exact continuation protocol

The next session should:

1. Read this checkpoint first.
2. Re-read the mandatory canonical-tree and engineering-governance entry documents,
   as required for a new session.
3. Capture current `HEAD` and dirty status without modifying files and compare it to
   the baseline in section 2.
4. Do **not** restart broad inventory.
5. Read the following four files individually, from start to EOF, and update the
   site approval/query flow and CF-006 evidence:
   - `platform/apps/geostat/backend/api/src/main/java/org/base/api/service/contract/approval/SiteContractRevisionApprovalService.java`
   - the corresponding `SiteContractRevisionRepository.java`
   - `platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/CanonicalPageDataService.java`
   - `platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ContractPhysicalQueryService.java`
6. Immediately afterward, trace calls from those classes to statistical dataset,
   page binding and publication membership tables. Record whether the statistical
   contract is integrated, parallel or intentionally independent.
7. Preserve any conclusion changes in this checkpoint rather than silently
   overwriting prior reasoning.

## 22. Machine-oriented continuation ledger

```yaml
recovery_target: platform/apps/geostat/backend/api
checkpoint_date: 2026-09-20
baseline_head: b46c0438d4927851fcd688a8a8e71468aba6cf18
status: INCOMPLETE
mode: READ_RECOVER_PLAN_ONLY

INSPECTED:
  - mandatory_engineering_doctrine
  - target_file_and_package_inventory
  - build_dependencies
  - controller_endpoint_inventory
  - statistical_compiler_registry_workflow
  - statistical_access_load_writer_reader
  - access_package_generations
  - artifact_package_pipeline
  - migration_runner_and_key_migrations
  - major_statistical_and_access_plans
  - late_architecture_improvement_register_entries
  - SiteContractRevisionApprovalService          # session 2, to EOF
  - SiteContractRevisionRepository               # session 2, to EOF
  - CanonicalPageDataService                     # session 2, to EOF
  - ContractPhysicalQueryService                 # session 2, to EOF
  - ServingPolicy                                # session 2, to EOF
  - PlatformPublicationService                   # session 2, to EOF
  - DataPlanePublicationWriter                   # session 2, to EOF
  - CanonicalObservationReader                   # session 2, to EOF
  - CanonicalStatisticalQueryAdapter             # session 2, to EOF
  - site_contract_and_publication_write_path_census  # session 2, repository-wide
  - ReleaseGateService                           # session 2, to EOF
  - SnapshotFactsRepository                      # session 2, to EOF
  - dataset_snapshot_status_state_machine_census # session 2, all writers and readers
  - PlatformSnapshotPreparationService           # batch 3, to EOF
  - PublicationControlStore                      # batch 3, to EOF
  - package_run_stage_order_and_callers          # batch 3
  - controller_and_endpoint_census               # batch 3, 43 files
  - LegacySurface_taxonomy_and_gated_controllers # batch 3
  - frontend_consumer_census_kids_and_system_app # batch 3
  - MSSQLToAccess_query_construction             # batch 3, CF-026
  - ContractPageExecutionRegistry                # batch 4, to EOF
  - ContractPageExecutionAdapter                 # batch 4, to EOF
  - PageFamily                                   # batch 4, to EOF
  - PageDataAdapterRegistry                      # batch 4, to EOF (dead)
  - ContractAggregationPlanner                   # batch 4, to EOF
  - CanonicalPageDataController                  # batch 4, to EOF
  - ContractQueryController                      # batch 4, main query method
  - frontend_kids_end_to_end_execution_path      # batch 4, section 23.28

PARTIALLY_INSPECTED:
  - platform_capability_audit_full_text
  - architecture_improvement_register_full_text
  - all_migration_statements
  - all_production_java_lines
  - all_test_java_lines
  - current_core_and_api_test_run

NOT_INSPECTED:
  - full_consumer_inventory
  - complete_endpoint_test_matrix
  - complete_query_security_cost_audit
  - clean_database_rebuild_current_tree
  - current_live_runtime_state
  - production_readiness_evidence_current_revision

FINDINGS:
  - six_architecture_generations_coexist
  - common_statistical_contract_is_substantially_implemented_but_not_release_ready
  - approved_statistical_plan_is_recompiled_against_mutable_registry
  - observation_attributes_are_not_round_tripped_by_reader
  - publication_membership_is_not_explicitly_bounded
  - package_chart_contract_and_lifecycle_authorities_overlap

EVIDENCE:
  - docs/reference/ENGINEERING-QUALITY-DOCTRINE.md
  - docs/reference/engineering/ARCHITECTURE.md
  - docs/reference/engineering/REQUIREMENTS.md
  - docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md
  - docs/work/STATISTICAL-CONTRACT-IMPLEMENTATION-CHECKLIST.md
  - docs/work/ARCHITECTURE-IMPROVEMENT-REGISTER.md
  - docs/decisions/ADR-access-package-integrity-automation.md
  - target_statistical_and_platform_source
  - backend_core_platform_migrations

CONFLICTS:
  - CF-001_ADR_011_collision
  - CF-002_mutation_mode
  - CF-003_numeric_envelope
  - CF-004_mutable_approved_plan
  - CF-005_attribute_read_loss
  - CF-006_parallel_M1_authorities
  - CF-007_publication_scope
  - CF-008_authorization_read_semantics
  - CF-009_dual_grammar
  - CF-010_provider_scale_capability
  - CF-011_site_specific_generic_migration
  - CF-012_migration_checksum_rewrites
  - CF-013_duplicate_migration_object
  - CF-014_no_contract_withdrawal
  - CF-015_dimension_group_attribute_gap
  - CF-016_no_classification_governance_API
  - CF-017_Access_navigation_portability
  - CF-018_reference_reconstruction
  - CF-019_global_string_trim
  - CF-020_duplicate_migration_manifest
  - CF-021_pre_publication_snapshots_served      # session 2, NEW
  - CF-022_two_liveness_resolution_models        # session 2, NEW
  - CF-023_release_gate_scope_vs_effect          # session 2, NEW (exact code for CF-007)
  - CF-024a_site_contract_has_no_governed_producer # session 2; SPLIT in batch 7 — the real gap
  - CF-024b_consumer_side_built_and_blocked_by_024a # batch 7, NEW (derived; see 23.55)
  - CF-025_snapshot_status_state_machine_unowned  # session 2, NEW
  - CF-026_caller_supplied_identifiers_legacy_export # batch 3, NEW
  - CF-027_aggregation_truncated_to_1000_rows     # batch 4, NEW
  - CF-028_five_inert_page_parameters             # batch 4, NEW
  - CF-029_migration_110_documents_absent_converter # batch 6, NEW
  - CF-030_two_chart_authorities_two_liveness_models # batch 6, NEW
  - CF-031_chart_data_bypasses_ServingPolicy      # batch 6, NEW (precondition of CF-008 fix)
  - CF-032_governed_publication_closed_to_statistical # batch 8, NEW, CRITICAL
  - CF-033_access_package_defines_canonical_metadata_schema # batch 11B, NEW, HIGH
  - CF-034_package_creates_APPROVED_PUBLIC_metadata_subject # batch 11C, NEW
  - CF-035_contract_revision_lookup_ignores_approval_status # batch 11C, NEW

INVARIANTS_ADDED:
  - INV-013_derived_plan_never_persisted_as_authority       # batch 7
  - INV-014_assertion_bound_to_exact_schema_revision        # batch 11C (QF-012's fix)

SYMMETRY:                                         # batch 11C, section 23.99
  generation_validation_symmetry: FAIL
  generator_side: derives_from_authority_emits_no_proof
  return_side: validates_package_against_itself_never_against_authority
  only_symmetric_axis: physical_load_mapping      # server looks it up rather than trusting
  access_authoring_role: A_contract_driven_physical_realizer
  access_reader_role: physical_package_reader_correctly_scoped
  site_specific_hardcoding_statistical_adapter: NONE
  hardcoded_literals_are: jet_reserved_words_PROVIDER_INVARIANT
    # materializeMetadata: MERGE platform.metadata_schema from package schema_json;
    # state("READY")->"APPROVED"; metadata_subject inserted as 'APPROVED','PUBLIC';
    # site_contract_revision lookup has NO status='APPROVED' predicate;
    # no transaction around the canonical writes (partial mutation risk)

ACCESS_TRUST_BOUNDARY:                            # batch 11B, section 23.90
  model: C_HYBRID
  data_structural_mapping: SERVER_BOUND           # load targets from site_contract_dataset
  contract_identity: EXISTENCE_CHECKED_NOT_APPROVAL_CHECKED
  metadata_schemas: ACCESS_AUTHORITATIVE
  metadata_subjects: ACCESS_TRIGGERED_HARDCODED_APPROVED_PUBLIC
  metadata_assertions: ACCESS_AUTHORITATIVE_LIFECYCLE
  package_to_contract_binding: MISSING            # no digest/fingerprint/equivalence
  minimum_correct_binding: deterministic_semantic_equivalence_via_PackagePlan_digest
  # CF-004 REVISED batch 8: availability defect, NOT semantic integrity (digest check is real)
  # CF-006 RESOLVED batch 8: layered representation, missing link — not competing authority

ORDERING_CONSTRAINTS:
  - RC-001_before_chart_convergence
  - CF-031_before_or_with_CF-008_correction
  - CF-006_before_CF-024a_remediation
  - CF-023_correction_MUST_bundle_governed_statistical_publication_path  # batch 8, CF-032

ARCHITECTURAL_DECISIONS:                          # batch 6, under decision authority
  DR-001_chart_canonical_authority:
    decision: statistical_chart_grammar_generalised + membership_based_resolution
    confidence: HIGH_direction / MEDIUM_HIGH_generalisation
    dispositions:
      core_ChartDefinition: DEPRECATE_THEN_DELETE_AFTER_MIGRATION
      managed_access_gs_chart: MIGRATE_TRANSPORT / DELETE_DESTINATION
      visualization_definition: MERGE
      statistical_chart: EVOLVE
    must_preserve: declaration_lifecycle_from_visualization_definition
    principal_risk: ChartSpec_coupling_to_SemanticPlan_components
    ordering: RC-001_must_be_settled_first
  DR-007_package_authority:                       # batch 11B
    decision: package_is_SELF_DESCRIBING_never_SELF_AUTHORITATIVE
    rules:
      - server_must_require_status_APPROVED_on_the_named_revision
      - correspondence_by_deterministic_semantic_equivalence_not_self_report
      - package_lifecycle_states_never_map_onto_canonical_states  # remove state()
      - package_may_reference_a_metadata_schema_never_define_one
      - validation_completes_before_any_canonical_mutation_in_one_transaction
    provider_boundary:
      contract_correspondence: contract_layer
      structural_type_feasibility: access_provider
      orchestration_only: ingestion_layer
    confidence: HIGH
    interim_posture: reject_package_supplied_schema_definitions_until_producer_exists
  DR-006_numeric_envelope:                        # batch 10 — CF-003 RESOLVED
    decision: exact_decimal_only_layered_envelope
    declaration: precision_le_28_scale_le_16      # bounded by Access, the weakest provider
    canonical_storage: DECIMAL_38_16              # strict superset of any declared value
    floating_point: NEVER_in_canonical_path       # DOUBLE is legacy-only
    envelope_belongs_in: provider_capability_registry  # closes CF-010
    legacy_28_10: widening_is_lossless_but_truncated_values_need_reload_from_artifact
    constraint_on_CF-027: SQL_pushdown_must_handle_SUM_overflow_over_DECIMAL_38_16
    serialization: one_exact_decimal_form_required # JSON string; today strings vs BigDecimal
    confidence: HIGH
  DR-005_grammar_authority:                       # batch 9
    decision: JSON_Schema_is_canonical_published_shape_M2; parser_is_its_implementation
    equivalence: bidirectional_conformance_corpus_bound_into_CI  # not code generation
    confidence: HIGH_direction / parity_completeness_inherently_bounded
    rejected: [generate_parser_from_schema, generate_schema_from_parser, delete_schema]
    schema_currently_unwired: referenced_only_in_one_javadoc
  DR-004_declaration_authority:                   # batch 8
    decision: KEEP_TWO_DISTINCT_M1_CONCEPTS_link_them_explicitly
    site_contract_owns: serving_and_presentation_declaration
    statistical_contract_owns: dataset_semantics
    meet_at: shared_control_plane_registry
    required_link: site_contract_dataset_references_approved_statistical_revision_digest
    enforcement_point: existing_pluggable_ContractApprovalCheck_list
    confidence: HIGH_separation / MEDIUM_HIGH_linkage
    rejected: merging_the_two_contexts_under_one_lifecycle
  DR-003_package_composer:                        # batch 7
    decision: KEEP_AND_EVOLVE_as_derivation_port_not_producer
    confidence: HIGH
    runtime_reachability: NONE                    # referenced only by its own test
    persistence_effects: NONE                     # reads only, stores nothing
    resolves_CF-024: NO                           # it depends on CF-024 and proves it
    pa_001_classification: TRANSPORT_DERIVED_REPRESENTATION_not_competing_authority
    ordering: CF-024a_must_be_answered_before_wiring
  DR-002_contract_and_chart_read_authority:
    decision: approved_contracts_and_charts_are_read_governed_resources
    confidence: HIGH
    enforcement: use_case_boundary_primary + http_annotation_defence_in_depth
    precondition: CF-031_must_be_repaired_first_or_together

DEAD_OR_VACUOUS_ARCHITECTURE:                     # batch 4, section 23.26
  - PageDataAdapterRegistry_unused_except_ReadContext
  - ContractPageExecutionRegistry_seven_identical_adapters
  - relational_ENTITY_adapter_never_registered
  - PageFamily_ROOT_has_no_adapter

ROOT_CAUSES:
  - RC-001_two_definitions_of_live                # session 2; subsumes CF-007/021/022/023
  - RC-002_migrations_carry_declaration_duty      # batch 9; reframed by RC-003
  - RC-003_upper_meta_levels_never_implemented    # batch 10, DEEPEST
    # Platform Meta-Contract (M3) and Control Plane Schema (M2) exist in 6 docs,
    # 0 java, 0 sql. Migrations substitute for the entire missing upper stack.
    # Subsumes RC-002, CF-024a, the rebuild divergence and SCH-003's unachievability.

LINEAGE_GATE:                                     # batch 11, section 23.88
  DATA_CONTRACT_LINEAGE_GATE: FAIL                # one material unknown: 23.84
  SCHEMA_AUTHORITY: PARTIAL_recovered_but_unimplemented
  SCHEMA_TO_SITE_CONTRACT: MISSING_IMPLEMENTATION
  SITE_CONTRACT_TO_ACCESS: PARTIAL                # declaration edge verified in artifact
  ACCESS_PHYSICAL_MODEL: VERIFIED                 # batch 11, 24 tables from the file
  ACCESS_TO_PLATFORM_LINEAGE: PARTIAL             # re-validation question unresolved
  CONTRACT_HIERARCHY: VERIFIED
  FORWARD_LINEAGE: PARTIAL
  BACKWARD_LINEAGE: PARTIAL                       # stops at site_contract_revision by absence
  INFORMATION_LOSS_AUDIT: PARTIAL
  PLATFORM_META_CONTRACT: ASPIRATIONAL            # 6 docs, 0 code, 0 schema, no ADR
  CONTROL_PLANE_SCHEMA: ASPIRATIONAL_as_producer  # DDL real; creating capability absent
  GRAPH_CONNECTIVITY: PASS_with_classification    # upper nodes are class E, explained absence

ACCESS_PHYSICAL_FACTS:                            # batch 11, read-only from the R8 artifact
  format: V2010
  tables: 24
  declaration_rows: 455                           # nine __gs_* tables
  data_rows: 2177                                 # __ent_/__rel_/__stat_/__cl_/__raw_
  cross_verified: [880_observations, 43_metrics, 43_carriers, 43_bindings]
  package_binding: KIDS_PORTAL_V1_revision_8      # __gs_package, plain value
  package_digest: NONE                            # no checksum binds the claim
  empty_declared_structure: __cl_hierarchy

ATOMIC_MIGRATION_SETS:
  AMS-001_governed_statistical_publication:       # batch 9, section 23.74
    members: [DR-004_link, CF-024a_producer, CF-032_ingestion_revision,
              governed_publication_path, CF-023_elimination]
    precondition: RC-001_answered
    protection_first: publication_writer_service_store_are_untested
    falsification: three_smaller_sequences_attempted_and_rejected

P0_EXPOSURE_PATH:                                 # section 23.15, REVISED by 23.17
  - every_ingestion_path_inserts_REVIEW_REQUIRED_before_any_gate
  - publication_writer_promotes_product_wide_without_gate_evidence
  - control_plane_intent_and_outbox_name_only_one_snapshot   # batch 3
  - status_based_readers_then_serve_ungated_data
  - rollback_does_not_restore_dataset_snapshot_status

REACHABILITY:                                     # batch 3, section 23.18
  SEMANTIC_REVIEW: REACHABLE_IN_NORMAL_FLOW       # CF-021 is live, not latent
  APPROVED: UNREACHABLE_DEAD_VALUE                # obsolete read-path term

INVENTORY_PROGRESS:                               # batch 5 (earlier values in comments)
  consumer_inventory: ~70%                        # unchanged by batch 5
  behavior_classification: ~78%                   # was ~72%
  test_corpus_classification: ~45%                # was ~0%
  test_corpus: 122_files_454_test_methods         # corroborates AIR-2026-048

TEST_COVERAGE_OF_CONFLICTS:                       # batch 5, section 23.37
  CF-021: NOT_COVERED                             # ContractPhysicalQueryService untested
  CF-022: NOT_COVERED
  CF-023: NOT_COVERED                             # publication writer/service/store/controller: zero tests
  CF-024: NOT_COVERED                             # only a test fixture writes site_contract_dataset
  CF-025: ASSERTED_BY_TEST                        # StatisticalLoadServiceTest:151
  CF-026: NOT_COVERED                             # only the gate is tested, not buildSelectQuery
  CF-027: NOT_COVERED
  CF-028: NOT_COVERED
  rehabilitation_blocked_by_tests: only_CF-025_and_partial_migration
  tests_contradicting_recovered_intent: none      # gate tests affirm the intent the writer violates
  api_surface: 43_controllers_38_with_endpoints_~160_endpoints
  frontend_kids: consumes_generic_platform_pages_contracts_exports
  frontend_geostat_system_app: consumes_legacy_test_and_mssql_to_access_only
  frontend_web_legacy: NOT_INSPECTED
  backend_mobile: NOT_INSPECTED
  external_callers: NO_EVIDENCE_IN_REPOSITORY

INVARIANTS:
  - INV-001_authority_chain
  - INV-002_one_semantic_authority
  - INV-003_closed_grammar_open_registry
  - INV-004_exact_pinned_references
  - INV-005_lossless_values
  - INV-006_observation_identity
  - INV-007_null_requires_status
  - INV-008_exact_publication_approval
  - INV-009_tenant_deny_by_default
  - INV-010_versioned_migrations
  - INV-011_provider_as_adapter
  - INV-012_gated_legacy_removal

DECISIONS:
  - DR-REC-001_restore_M3_to_M0
  - DR-REC-002_converge_existing_authorities
  - DR-REC-003_provider_neutral_semantics
  - DR-REC-004_expand_migrate_contract
  - DR-REC-005_no_publish_or_remove_during_recovery
  - DR-REC-006_attributes_are_canonical_behavior
  - DR-REC-007_release_not_ready

UNRESOLVED:
  - statistical_vs_site_contract_ownership
  - immutable_compiled_plan_persistence
  - grammar_generation_direction
  - numeric_envelope
  - package_taxonomy_and_plan_owner
  - chart_canonical_owner
  - classification_approval
  - publication_membership_and_status_repair
  - migration_history_policy
  - legacy_consumer_inventory

NEXT_ACTIONS:                                     # superseded by section 23; kept for provenance
  - finish_four_partially_read_site_query_approval_classes   # DONE session 2
  - trace_statistical_contract_to_site_binding_and_publication # DONE session 2 (CF-006 upgraded)
  - read_publication_writer_for_AIR_2026_054      # DONE session 2 (CF-023)
  - verify_statistical_authorization_boundary     # OPEN
  - complete_migration_chain_rebuild_audit        # OPEN
```

## 23. Session 2 continuation — Claude Opus 5, 2026-09-20

Mode unchanged: read, recover, reconcile, plan. No application source, test, schema,
migration or runtime configuration was modified in this session. The only write is
this checkpoint.

Sections 1-22 are the previous agent's record and were not rewritten. Where this
session changes a conclusion, the previous statement is quoted before the revision.

### 23.1 Baseline re-verification — inherited baseline is VERIFIED

| Fact | Section 2 value | Session 2 measurement | Verdict |
|---|---|---|---|
| `HEAD` | `b46c0438d4927851fcd688a8a8e71468aba6cf18` | identical | VERIFIED |
| `git describe` | `geostat-v1.0.2-oidc-rs256-132-gb46c043-dirty` | identical | VERIFIED |
| Branch | `security/hardening-2` | identical | VERIFIED |
| Target modified tracked files | 26 | 26 | VERIFIED |
| Target untracked files | 29 | 29 (`-uall`) | VERIFIED |

Repository-wide status entries: 91. No source change occurred between the two
sessions, so every section 1-22 source-level finding rests on the same bytes and does
not need re-derivation. Build/test baseline remains **UNVERIFIED** — no Gradle run was
started in this session and none should be started before the owner agrees, because a
test run mutates ignored build outputs.

### 23.2 Inherited conclusions checked against repository evidence

| Inherited finding | Session 2 verdict | Basis |
|---|---|---|
| CF-005 attribute read loss | **VERIFIED** | `CanonicalObservationReader:69-95` reads `observation` + `observation_dimension` only; `observation_attribute` is absent from the class |
| CF-006 parallel M1 authorities | **VERIFIED, upgraded MEDIUM-HIGH → HIGH** | zero occurrences of `site_contract*` in the whole statistical package tree and in both statistical controllers |
| CF-007 publication scope | **VERIFIED, exact mechanism located** | `DataPlanePublicationWriter:28,31` — restated with precision as CF-023 |
| CF-013 duplicate migration object | **PLAUSIBLE → likely intentional repair** | the third writer is literally `057_contract_page_binding_idempotent_repair.sql`; contents still unread |
| Partial migration 4 (migration-084 lifecycle table unwritten) | **VERIFIED** | `SiteContractRevisionApprovalService` writes `site_contract_revision` + `site_contract_revision_approval` + `outbox_event` only; `contract_revision_lifecycle` never appears |
| Unresolved Q18 (caller-influenced identifiers) | **partially answered: NO, for this class** | `ContractPhysicalQueryService.identifier()` validates every identifier against `[A-Za-z_][A-Za-z0-9_]*`, max two dot parts; every identifier originates from Control Plane contract rows; filters compile against the approved field allowlist |
| Section 7.5 site/page serving flow (MEDIUM, "must be completed") | **now HIGH** | all four classes read to EOF; flow restated in 23.4 |

Nothing inherited was contradicted. Two findings were made sharper and one symptom
(CF-007) was resolved into a root cause.

### 23.3 What the four previously-truncated classes actually contain

**`SiteContractRevisionApprovalService` (141 lines) — the strongest governance
implementation inspected so far.** One `primaryJdbcTransactionManager` transaction:
`lockRevisions` takes `WITH (UPDLOCK, HOLDLOCK)` so concurrent approvals of one
contract serialize; an already-`APPROVED` target replays its receipt only when the
stored checksum matches, otherwise it refuses as "approved outside the governed
workflow"; all `ContractApprovalCheck` beans are evaluated over measured facts and any
`FAIL` blocks the whole operation; supersede-then-approve uses a compare-and-set
(`WHERE ... AND status='REVIEW_REQUIRED'`) and aborts on a lost race; checksum-bound
evidence JSON (`geostat.contract-approval.v1`) is persisted; an
`outbox_event('SITE_CONTRACT_REVISION','SITE_CONTRACT_REVISION_APPROVED')` row is
appended in the same transaction. **This class is a candidate canonical reference
pattern for the rest of the platform** — it is the only inspected approval path with
locking, CAS, idempotent replay, blocking checks, evidence and outbox together.

**`SiteContractRevisionRepository` (89 lines)** — Control Plane persistence.
`checksumMatchesDocument` is computed in SQL by re-hashing `contract_document_json`
with `HASHBYTES('SHA2_256', ...)`, so drift between the stored checksum and the stored
document is detectable at read time. `foreignProductDatasetCount` exists specifically
to detect a revision binding another product's dataset version — a tenancy invariant
expressed as an approval fact.

**`CanonicalPageDataService` (79 lines, densely formatted)** — orchestration only.
Page metadata is resolved by joining `contract_page_binding` + `site_contract_revision`
+ `site_contract_node` with `r.status='APPROVED' AND b.status='ACTIVE' ORDER BY
r.revision DESC`, so serving does bind to an approved revision. Family dispatch goes
through `ContractPageExecutionRegistry` / `PageFamily.fromDataFamily(...)` — no
site-specific branching in this class. Keyset reads refuse a cursor whose contract
revision is no longer current.

**`ContractPhysicalQueryService` (165 dense lines)** — the generic execution engine.
Bounded by construction: page size clamped to 1000; include fan-out refused above 2000
join keys and above a 1000-row expansion budget; relation recursion capped at depth 8
with a visited-path cycle guard; keyset pagination refused unless the contract declares
the `required=1` index (`contract_index_definition`); sort keys must be declared
fields. Reads are routed by `table_role`/physical-name prefix to either the declared
physical table or a canonical projection.

### 23.4 Completed site/page serving flow (replaces the MEDIUM sketch in 7.5)

```text
contract_page_binding (ACTIVE) + site_contract_revision (APPROVED, highest revision)
 -> site_contract_node / site_contract_dataset  (page identity, dataset code, data family)
 -> ContractPageExecutionRegistry.require(PageFamily.fromDataFamily(...))
 -> ContractPhysicalQueryService
      -> contract_table_definition + contract_structure (APPROVED/ACTIVE, highest revision)
      -> ServingPolicy.require(revisionId, datasetCode)      [servable? authority held?]
      -> contract_field_definition (APPROVED/ACTIVE)          [the only selectable columns]
      -> ContractQueryCompiler.compile(filters, allowedFields, sort)
      -> DATA plane physical table  |  canonicalSource(...)  |  ServingPolicy.rawRows(...)
 -> ContractRelationGraphExecutor.attach (bounded includes)
 -> ContractPolicyService.requireGoverned + gates
 -> ContractProjectionService.apply / applyRows
 -> canonical page response (+ cursor/keyset or aggregation)
```

### 23.5 NEW CF-021 — pre-publication snapshots are served by the canonical read path

- **Evidence A:** `ServingPolicy` states the invariant in its own Javadoc — "only from
  a PUBLISHED snapshot, so serving can never reveal data that has not passed the
  publication gates" — and `rawRows` implements it (`TOP 1 ... WHERE s.status =
  'PUBLISHED' ORDER BY s.dataset_snapshot_id DESC`).
- **Evidence B:** `ContractPhysicalQueryService.canonicalSource(...)` (lines 158-162),
  used for canonical `ENTITY` and `STATISTICAL` reads, selects
  `dataset_snapshot_id IN (SELECT dataset_snapshot_id FROM publication.dataset_snapshot
  WHERE dataset_version_id = ? AND status IN ('SEMANTIC_REVIEW','PUBLISHED','APPROVED'))`.
- **Semantic difference:** `SEMANTIC_REVIEW` and `APPROVED` are pre-publication states;
  `PlatformPublicationService` will only publish from `REVIEW_REQUIRED`, so neither is
  a released state.
- **Impact:** the generic contract page/query surface can return rows that never passed
  the publication gate, while the raw-serving surface of the same platform refuses
  exactly that. Violates INV-008 and the doctrine's no-publication-before-evidence rule.
- **Affected components:** `ContractPhysicalQueryService`, `CanonicalPageDataService`,
  every page/query/aggregate/include/SDMX consumer of the generic surface.
- **Risk:** HIGH (P0 candidate). **Confidence:** HIGH that the query admits those
  states; **UNVERIFIED** whether snapshots currently sit in those states in any live
  database, so actual disclosure is not asserted.
- **Required decision:** one released-state definition for every read path (see CF-022
  and RC-001).

### 23.6 NEW CF-022 — two incompatible definitions of "live" coexist

Membership-based resolution — the publication aggregate:

| Class | Resolution |
|---|---|
| `CanonicalStatisticalQueryAdapter:12-17` | `publication.snapshot (status='PUBLISHED')` → `snapshot_member` → `series` → `observation` |
| `PlatformServingCacheService:26-31` | same join to build `serving.metric_cache` |
| `PlatformPublicationArchiveService:17` | same join for archive scope |
| migrations `073`, `075` | reconciliation reports scoped by `snapshot_member` |

Status-column resolution — the per-dataset flag:

| Class | Resolution |
|---|---|
| `ContractPhysicalQueryService.canonicalSource` | `dataset_snapshot.status IN ('SEMANTIC_REVIEW','PUBLISHED','APPROVED')`, set-valued, no latest selection |
| `ServingPolicy.rawRows` | `TOP 1 dataset_snapshot WHERE status='PUBLISHED' ORDER BY dataset_snapshot_id DESC` |
| `CanonicalObservationReader.read` | `TOP 1 dataset_snapshot WHERE status='PUBLISHED' ORDER BY dataset_snapshot_id DESC` |

- **Semantic difference:** membership answers "what did an approver release"; the status
  column answers "what did some publish run touch". CF-023 proves those two sets are
  not equal, and AIR-2026-054 recorded the divergence in live data (snapshots 53 and
  57-61 `PUBLISHED` without current membership).
- **Additional defect inside the status family:** `canonicalSource` uses `IN (...)`
  over *all* matching snapshots rather than selecting one, so two snapshots of the same
  `dataset_version` in accepted states union their rows — duplicated observations and
  mixed-revision answers, with no deterministic winner.
- **Impact:** the same dataset answers differently depending on which surface a consumer
  calls; a rollback restores membership-based surfaces and leaves status-based surfaces
  serving the rolled-back content.
- **Risk:** HIGH. **Confidence:** HIGH (all six call sites read).
- **Answers unresolved question 14** with direct evidence: membership *is* implemented
  and *is* used by three components; the statistical and generic read paths simply do
  not consult it.

### 23.7 NEW CF-023 — release gates are evaluated for one snapshot and applied to many

This is CF-007 resolved from symptom to mechanism.

- **Evidence A:** `PlatformPublicationService.publish` validates exactly one snapshot —
  `... WHERE dataset_snapshot_id=? AND dataset_version_id=? AND status='REVIEW_REQUIRED'`
  — then runs `releaseGates.requireReleasable(request.datasetSnapshotId())` and
  `artifactGate.requirePassIfDeclared(request.datasetSnapshotId(), ...)` on that one id.
- **Evidence B:** `DataPlanePublicationWriter.publish` line 28 selects members with
  `WHERE b.product_id=? AND s.status='REVIEW_REQUIRED'` (latest per `dataset_version_id`
  via `ROW_NUMBER()`), **never referencing `request.datasetSnapshotId()`**.
- **Evidence C:** line 31 then executes
  `UPDATE publication.dataset_snapshot SET status='PUBLISHED' WHERE dataset_snapshot_id
  IN (... b.product_id=? AND s.status='REVIEW_REQUIRED')` — i.e. **every** review-state
  snapshot of the product, including the older per-version snapshots that were *not*
  added as members by the `rn=1` filter.
- **Evidence D:** `rollback` (lines 36-47) updates only `publication.snapshot`; it never
  restores `publication.dataset_snapshot.status`, so the mass flip survives rollback.
- **Impact, stated precisely:** this is not only unbounded membership. A dataset snapshot
  that never had its release gates or artifact reconciliation evaluated becomes live
  under a *different* snapshot's gate receipt. The approver's evidence does not cover
  what the operation actually released. Evidence D is the exact source of the "status
  residue" in AIR-2026-054.
- **Affected invariants:** INV-008 (publication requires explicit governed approval),
  REL-001, and the doctrine rule that retry/partial paths must not create a second
  logical publication.
- **Risk:** CRITICAL / P0. **Confidence:** HIGH (code read end to end).
- **Knowledge to preserve before any fix:** the `ROW_NUMBER() OVER(PARTITION BY
  dataset_version_id ORDER BY dataset_snapshot_id DESC)` rule encodes a real
  requirement — a product release is multi-dataset and must carry forward one snapshot
  per dataset version. A naive "publish only the requested id" fix would silently drop
  the other datasets from the release. The correct target is an explicit candidate set
  plus carry-forward of unchanged members, which is precisely unresolved question 13.

### 23.8 NEW CF-024 — the site contract M1 layer has no governed producer

- **Evidence A:** repository-wide search for `INSERT`/`MERGE`/`UPDATE` against
  `platform.site_contract_revision`, `platform.site_contract_dataset` and
  `platform.contract_page_binding` finds **no production Java writer anywhere in the
  API target**. Every Java reference is a `SELECT` (`ArtifactPackageContractResolver`,
  `ContractMetadataService`, `ContractIntrospectionService`, `ContractQueryPlanService`,
  `ContractCompatibilityService`, `ContractRuntimeValidator`, `CanonicalPageDataService`,
  `ContractPhysicalQueryService`, `JdbcPackageContractSource`,
  `PlatformAccessIngestionService`) or a migration-registration string in
  `PlatformSchemaMigrationRunner`.
- **Evidence B:** the only writers in the repository are SQL migrations, and they are
  site-named: `022_kids_complete_site_contract.sql`,
  `023_kids_inferred_statistical_semantics.sql`,
  `055_kids_final_page_contract_revision.sql` for revisions/datasets/fields, and
  `054_contract_page_binding.sql`, `055`, `057_contract_page_binding_idempotent_repair.sql`
  for bindings. They use the T-SQL `INSERT platform.x(...)` form without `INTO`, which
  is why a conventional `INSERT INTO` search misses them.
- **Evidence C:** the one remaining writer is a test fixture,
  `PackageContractSourceTest`.
- **Impact:** the M1 declaration layer that the entire generic serving stack consumes
  cannot be authored through the platform. `SiteContractRevisionApprovalService` can
  approve a revision but nothing in the system can *create* one. Onboarding a second
  site therefore requires hand-written, site-named SQL inside the generic migration
  chain — which is the same defect as CF-011, but general rather than incidental.
- **Consequences to carry into the plan:** (a) SCH-003 "contract-only onboarding without
  core-code branching" cannot currently be satisfied, and the platform audit's P1.1
  second-data-product gate is blocked by a *structural* cause, not merely by missing
  work; (b) a clean-database rebuild reproduces only KIDS declarations, which is a
  plausible contributing cause of AIR-2026-027; (c) approval governance sits on top of a
  declaration layer with no admission control.
- **Risk:** HIGH (platform identity, rebuildability, SCH-003). **Confidence:** HIGH for
  the absence of a writer in this worktree; **UNVERIFIED** whether an out-of-repository
  operator procedure exists.
- **Required decision:** whether site-contract authoring becomes a governed API, a
  generated artifact of the package/statistical compiler, or remains an operator SQL
  procedure with explicit governance — this is a prerequisite for CF-006's resolution,
  not a consequence of it.

### 23.9 ROOT CAUSE RC-001 — "live" has two authorities

CF-007/CF-021/CF-022/CF-023 and unresolved question 14 are one root cause, not four
defects:

```text
SYMPTOM       one publish call released six snapshots; rollback left PUBLISHED residue;
              chart/page/export surfaces disagree about the same dataset
ROOT CAUSE    the platform has a publication aggregate (publication.snapshot +
              snapshot_member) intended as the release boundary, AND a per-dataset
              liveness flag (publication.dataset_snapshot.status) that three read paths
              trust instead; the writer maintains the flag product-wide rather than for
              the approved membership, so the two are not equal
CONSEQUENCE   the release boundary is not authoritative; gate evidence does not cover
              the released set; rollback is partial; INV-008 is unenforceable by
              construction rather than by accident
REMEDIATION   one liveness authority — membership — with dataset_snapshot.status demoted
              to per-dataset lifecycle bookkeeping; every read path resolves through the
              current publication; the publish writer bounded to an explicit approved
              candidate set with carry-forward of unchanged members; a reconciliation
              task for existing residue (never an assumption-based cleanup)
```

RC-001 must be settled before CF-005, CF-018 or any chart/export convergence work,
because each of those defines correct behavior in terms of "what is currently served".

### 23.10 Quality findings recorded, not fixed

| ID | Location | Finding |
|---|---|---|
| QF-001 | `CanonicalObservationReader:63` | loads the entire `platform.classification_item` ACTIVE set into a map on every read, and issues one `observation_dimension` query per observation (line 79) — an unbounded Control Plane read plus N+1, against the API-001 bounded-query obligation |
| QF-002 | `CanonicalObservationReader:88` | groups observations by `dimensions.toString()` — a `Map.toString()` rendering used as a semantic key; ordering- and content-fragile |
| QF-003 | `CanonicalPageDataService:36` | the `read(int pageId, ...)` overload resolves a *global* default approved contract with no product or tenant scope — a single-site assumption on a multi-site platform |
| QF-004 | `CanonicalPageDataService:53,62` | `contractPolicy.requireGoverned(rid)` runs *after* the data query has executed; it still prevents the response, but work precedes the governance check |
| QF-005 | `ContractPhysicalQueryService:159` | builds `CASE WHEN '<field>'='value_decimal' THEN CONVERT(nvarchar(128), o.numeric_value) ...` — a Java-side conditional emitted as a SQL literal comparison, and a decimal→string round-trip on a value INV-005 requires to stay exact |
| QF-006 | `ContractPhysicalQueryService:158-161` | the canonical statistical projection reads dimension values from `raw.source_record.payload_json` via `JSON_VALUE`, not from `statistics.observation_dimension`; the canonical dimension model is bypassed on this surface while `CanonicalObservationReader` uses it — a second read-model divergence beneath CF-022 |

QF-005 and QF-006 are recorded as evidence, not as approved change: the correct
behaviour depends on RC-001 and on unresolved question 4 (numeric envelope).

### 23.11 Effect on the inherited registers

- **INV-008** — restated as *violated in implementation on two distinct paths*
  (CF-021 read side, CF-023 write side), not merely "known violation: product
  publication sweeps all REVIEW_REQUIRED snapshots".
- **INV-002** — CF-024 adds a second failure mode: the site contract does not have a
  competing owner, it has *no* owner on the write side.
- **PA-007 query/serving mechanisms** — now carries a concrete divergence axis:
  membership-resolved surfaces versus status-resolved surfaces.
- **Unresolved question 14** — answered as stated in 23.6; the remaining work is impact
  mapping, not discovery.
- **Unresolved question 18** — answered NO for `ContractPhysicalQueryService`; the audit
  must still cover the legacy controllers, `MSSQLToAccess` and the dynamic page services.
- **Unresolved question 13** — the decision is now fully specified by CF-023's evidence,
  including the carry-forward requirement that a naive fix would destroy.

### 23.12 Session 2 scope discipline

No `CROSS-BOUNDARY CHANGE REQUIRED` item is raised yet, but two are foreseeable and are
recorded here so a later session does not discover them late:

1. **CF-024 / CF-011** — site-contract seed data and the KIDS-specific declarations live
   in `backend/core/src/main/resources/db/platform`, outside `TARGET_PROJECT_ROOT`. Any
   canonical fix to how declarations are produced will require changes there.
2. **CF-020** — the migration resource set is external while the ordered runner
   registration is inside the target; reconciling the two manifests cannot be done from
   one side alone.

Both remain EXTERNAL CONTEXT until a plan task is written; neither was modified.

### 23.13 The release-gate subsystem is correct, and that makes CF-023 unambiguous

`ReleaseGateService` (113 lines) and `SnapshotFactsRepository` (97 lines) were read to
EOF. They are **strictly snapshot-scoped and rigorous**, and this materially changes how
CF-023 must be interpreted.

- `evaluate(snapshotId)` loads facts for one snapshot, refuses any snapshot outside
  `EVALUABLE_STATES = {SEMANTIC_REVIEW, REVIEW_REQUIRED}`, evaluates every registered
  `ReleaseGate`, records per-gate evidence, and promotes `SEMANTIC_REVIEW ->
  REVIEW_REQUIRED` only via a guarded CAS (`markReviewRequired`, `WHERE ... AND
  status='SEMANTIC_REVIEW'`).
- `requireReleasable(snapshotId)` is a genuine fail-closed guard: it demands the latest
  evidence of **every** registered gate, rejects evidence whose `schema` is not
  `geostat.release-gate.v1` (so forged or hand-written evidence cannot pass), rejects any
  non-passing result, and rejects evidence whose `factsDigest` no longer equals the
  freshly measured facts — "Snapshot facts changed after X was evaluated; evaluate again".
- `SnapshotFacts` measures real reconciliation facts, including cross-batch and
  cross-snapshot contamination, duplicate and null business keys, dangling entity links,
  and — implementing INV-007 directly — observations with no value of any type while
  still carrying the default status, with the invariant stated in a source comment
  ("an empty value is a defect only when nothing explains it").

**Consequence for the recovery.** The intended publication semantics are therefore not
ambiguous and do not need an architectural decision to *discover*: the gate contract
binds evidence to one snapshot id and to that snapshot's measured facts digest. The
platform's own design says publication is per-snapshot and gate-bound. `DataPlanePublicationWriter`
simply does not honour it. CF-023 is an implementation divergence from a clearly
recovered intent, not a design question — which raises confidence in its remediation
direction from "recommended" to "recovered".

**Verified-consistent (recorded so a later agent does not 'fix' a non-defect):** the gate
counts unexplained empty observations with `o.observation_status='VALID'`, and
`CanonicalObservationWriter.STATUS_NORMAL` is exactly `"VALID"`. The gate and the
statistical writer share one default-status vocabulary. A suspected divergence here was
checked and refuted.

**Gate coverage gap (NEW, MEDIUM-HIGH):** `SnapshotFactsRepository.classificationItems`
collects referenced vocabulary only from `entity.entity_classification`. It never
collects `classification_item_id` values referenced by
`statistics.observation_dimension`. The active-vocabulary gate therefore does not cover
statistical dimension codes: a statistical snapshot referencing retired classification
items is not detected. This repeats the session's broader pattern — the newer statistical
family is less covered by existing controls than the older entity family.

### 23.14 NEW CF-025 — the snapshot liveness state machine has no owner

The column that three read paths use to decide what is live is modelled nowhere.

- **Schema:** `002_data_plane.sql:19` declares
  `status VARCHAR(24) NOT NULL DEFAULT 'PREPARED'` — free text, **no CHECK constraint**,
  no enum, no lifecycle table.
- **Five independent writers**, each hardcoding its own literals:

| Writer | Transition | Guard |
|---|---|---|
| `PlatformSnapshotPreparationService:51` | INSERT (prepared state) | load must be `VALIDATED` |
| `SemanticMaterializationService:76` | `-> 'SEMANTIC_REVIEW'` | **none** — `WHERE dataset_snapshot_id=?` only |
| `SnapshotFactsRepository:76` | `'SEMANTIC_REVIEW' -> 'REVIEW_REQUIRED'` | guarded CAS, gate-owned |
| `StatisticalLoadService:122` | **INSERT directly as `'REVIEW_REQUIRED'`** | n/a — the gate transition is skipped |
| `DataPlanePublicationWriter:31` | `'REVIEW_REQUIRED' -> 'PUBLISHED'` | product-wide, not snapshot-scoped (CF-023) |

- **Read paths accept mutually inconsistent subsets** (23.6), including `'APPROVED'`,
  which a repository-wide search shows **no writer anywhere produces**, while
  `'PREPARED'` — the schema default — is accepted by nothing.
- `SemanticMaterializationService`'s unguarded update can move a snapshot backwards into
  `SEMANTIC_REVIEW` from any state, including `PUBLISHED`.
- **Risk:** HIGH. **Confidence:** HIGH (schema, all five writers and all six readers read).
- **Relation to INV-002:** this is the platform's clearest one-concept-many-owners
  violation, and it sits on its most safety-critical concept.

### 23.15 The consolidated P0 exposure path (CF-023 + CF-025 + CF-022)

Each defect above is individually serious; together they compose one concrete,
source-traceable path by which ungated data becomes live:

```text
1. StatisticalLoadService:122 inserts the snapshot directly as REVIEW_REQUIRED,
   bypassing SEMANTIC_REVIEW — so ReleaseGateService never runs and no gate
   evidence is ever recorded for it.
2. The snapshot now sits in exactly the state PlatformPublicationService treats
   as publishable.
3. An operator publishes some OTHER dataset of the same product. That request is
   correctly validated and gated for its own snapshot id.
4. DataPlanePublicationWriter:28 selects members by product + REVIEW_REQUIRED,
   ignoring the requested id; line 31 flips EVERY REVIEW_REQUIRED snapshot of the
   product to PUBLISHED, including those never added as members.
5. The statistical snapshot is now PUBLISHED with no gate evidence in existence.
6. ServingPolicy.rawRows and CanonicalObservationReader select the newest
   PUBLISHED snapshot and serve it; snapshot_member never listed it, so the
   membership-based surfaces disagree.
7. Rollback restores publication.snapshot only, so the status flip survives.
```

- **Status:** the mechanism is VERIFIED at source level end to end. Whether it has
  already occurred in a live database is **UNVERIFIED by this session**, but
  AIR-2026-054's recorded residue (snapshots 53 and 57-61 `PUBLISHED` without current
  membership) matches this signature exactly.
- **Requirement for the plan:** step 1 and step 4 must both be repaired, and they are
  independent defects. Repairing only the publication writer still leaves statistical
  loads entering a publishable state without gate evaluation; repairing only the
  statistical load still leaves product-wide promotion of other families.
- **Do not treat the live residue as cleanup.** Per section 14 item 12, existing
  `PUBLISHED`-without-membership rows require a reviewed reconciliation with backup,
  never an assumption-based UPDATE.

### 23.17 Batch 3 — REVISION of a session-2 finding (traceability preserved)

- **PREVIOUS FINDING (session 2, section 23.14 table):** `PlatformSnapshotPreparationService:51`
  was recorded as "INSERT (prepared state)", and the `'PREPARED'` schema default was
  described as a value "no component recognizes".
- **NEW EVIDENCE:** the statement reads
  `INSERT INTO publication.dataset_snapshot(dataset_load_id,dataset_version_id,status,row_count,checksum)
  OUTPUT INSERTED.dataset_snapshot_id VALUES(?,?,'REVIEW_REQUIRED',?,?)` — the literal is
  `'REVIEW_REQUIRED'`.
- **CONTRADICTION:** the generic platform ingestion path does **not** start in a
  pre-review state. It inserts directly into the same publishable state as the
  statistical path.
- **IMPACT:** step 1 of the consolidated P0 exposure path (23.15) is **not
  statistical-specific**. Both production ingestion paths place a snapshot in
  `REVIEW_REQUIRED` before any gate has run. `SemanticMaterializationService:76` is
  therefore a **backward** transition (`REVIEW_REQUIRED -> SEMANTIC_REVIEW`), not a
  forward one, and `SnapshotFactsRepository.markReviewRequired` returns the snapshot to
  a state it already occupied.
- **REVISED CONCLUSION:** `'PREPARED'` is written by nothing and is a pure schema
  default, unchanged. But the defect it illustrated is broader than recorded: the
  publishable state is the *entry* state of every ingestion path, and gate evaluation is
  an optional later step rather than a precondition of becoming publishable. CF-025
  stands and is strengthened; the CF-023 exposure is wider than session 2 stated.
- **AFFECTED ITEMS:** CF-025 writer table, section 23.15 step 1, and any future task that
  assumed the statistical loader was the only bypass.

### 23.18 Batch 3 answer — is CF-021 reachable?

The question posed for this batch was whether `SEMANTIC_REVIEW` and `APPROVED` are
reachable through real production flows. The two states have **different answers**.

**`SEMANTIC_REVIEW` — ACTUALLY REACHABLE. CF-021 is a live disclosure path, not latent.**

It is produced by `SemanticMaterializationService:76`, which is reached from two
production entry points:

| Entry point | Path |
|---|---|
| `PlatformIngestionController:60` | HTTP endpoint under `/platform/ingestion` calling `semanticMaterializationService.materialize(request)` |
| `MaterializeStage:40` | stage `MATERIALIZE` of the governed artifact package-run pipeline |

The package-run pipeline order was recovered from `PackageRunStage.order()` and is
enforced by `PackageRunService` (sorted by order, unique codes and orders required):

```text
100 INGEST_DATASET -> 200 VALIDATE_LOAD -> 300 PREPARE_SNAPSHOT -> 400 MATERIALIZE
 -> 500 BIND_ATTACHMENTS -> 600 RECONCILE -> 700 EVALUATE_RELEASE_GATES
```

State timeline inside that governed run:

- after stage 300 the snapshot is `REVIEW_REQUIRED` with **no gate evidence** — the
  CF-023 collateral-publication window is open;
- between stage 400 and stage 700 the snapshot is `SEMANTIC_REVIEW` — the CF-021
  serving window is open;
- stage 700 returns it to `REVIEW_REQUIRED`, now with evidence.

Every governed package run therefore passes through the CF-021 window. This is normal
flow, not an edge case.

**`APPROVED` — NOT REACHABLE. Dead/obsolete behavior, evidence of an incomplete
lifecycle migration.**

No writer anywhere in the repository sets `publication.dataset_snapshot.status =
'APPROVED'`: the five Java writers use `REVIEW_REQUIRED`, `SEMANTIC_REVIEW` and
`PUBLISHED` only, and no migration writes it. `ContractPhysicalQueryService.canonicalSource`
accepts it regardless. Classification: **obsolete accepted value retained in one read
path after the writing side moved on** — the read path was not migrated with the
lifecycle. It is harmless today and becomes a latent defect the moment anything
reintroduces the value.

**Consequence for the plan:** CF-021 must be treated as a P0-class reachable defect for
the `SEMANTIC_REVIEW` term and as a legacy-cleanup item for the `APPROVED` term. They
are different remediations and must not be merged into one task.

### 23.19 Batch 3 — the governed pipeline exists and the statistical family did not adopt it

The artifact package-run pipeline is a genuine, well-formed orchestration: seven ordered
stages ending in release-gate evaluation, with unique code/order validation at startup.
It is the platform's existing canonical answer to "how does data become publishable".

`StatisticalLoadService` does not use it. It writes `ingest.batch`, `ingest.artifact`,
`ingest.dataset_load` and `publication.dataset_snapshot` itself, inside its own
transaction, and never enters a run. This is the clearest **incomplete migration** found
so far, and it is the inverse of the usual direction: the newest subsystem bypasses the
governed orchestration that the older subsystem already had.

- **Classification:** PARTIAL MIGRATION (not accidental duplication — the statistical
  loader has genuine reasons to differ, but admission, receipts, staging and gate
  sequencing are duplicated rather than reused).
- **Knowledge to preserve:** the statistical loader's single-transaction Data Plane write
  and its checksum-based idempotency are real requirements recorded in section 7.2; a
  migration onto the run pipeline must not lose them.

### 23.20 Batch 3 — NEW CF-026: caller-supplied identifiers in the legacy export surface

- **Evidence:** `MSSQLToAccess.buildSelectQuery` (lines 107-117) builds
  `SELECT * FROM [" + databaseName + "].dbo.[" + tableName + "]` and the MySQL backtick
  equivalent, from `@RequestParam` values, with **no identifier validation** — only
  bracket/backtick quoting, which a `]` or a backtick in the input escapes. The route
  also opens `DriverManager.getConnection(mssqlUrl, user, password)` from caller-supplied
  connection parameters.
- **Existing mitigations:** the class carries `@LegacySurfaceGate(LegacySurface.DATABASE_EXPORT)`
  and calls `validateConnectionInputs(...)` backed by `DatabaseEndpointGuard` and
  `HostAllowList` (untracked). Those constrain **the connection target**, not the
  **identifiers**.
- **Impact:** a doctrine-prohibited pattern — `CLAUDE.md` forbids
  "caller-supplied SQL/table/column/expression" and `ANTI-PATTERNS.md` maps it to
  API-001/SEC-002 — remains present on an operator-gated route.
- **The canonical fix already exists in this codebase:**
  `ContractPhysicalQueryService.identifier()` validates every identifier against
  `[A-Za-z_][A-Za-z0-9_]*` with at most two dot parts. This is a convergence
  opportunity, not a new design.
- **Risk:** HIGH if the family is enabled; **currently default-off**. **Confidence:** HIGH.
- **Do not fix in place without the consumer decision below** — this route has a live
  in-repository consumer (23.22).

### 23.21 Batch 3 — CF-023 corroboration: the audit trail misstates the release

`PublicationControlStore.createIntent` writes `platform.release` plus an
`outbox_event('RELEASE','PUBLISH_SNAPSHOT')` whose payload names exactly one
`datasetSnapshotId` — the requested one. `DataPlanePublicationWriter` then publishes
every `REVIEW_REQUIRED` snapshot of the product. The durable control-plane intent and
the emitted integration event therefore **describe a narrower release than the one that
occurred**. Any downstream consumer, archive job or auditor reading the outbox is misled.

This raises CF-023 from "wrong effect" to "wrong effect plus incorrect evidence", which
matters for REL-001 and for the doctrine rule that evidence must bind to what actually
happened.

Minor quality note (QF-007): the outbox payload is assembled by string concatenation
rather than the injected `ObjectMapper`. The interpolated values are a validated
SHA-256 hex string and three `long`s, so it is not injectable today; it is fragile, not
defective.

### 23.22 Batch 3 — consumer inventory (binding constraint: ~20% -> ~55%)

**API surface census of `TARGET_PROJECT_ROOT`:** 43 controller files, 38 carrying
endpoints, approximately 160 mapped endpoints, plus 5 exception handlers.

Generational grouping by declared base path:

| Generation | Base paths | Controllers |
|---|---|---|
| 1 — pre-platform legacy | `/test`, `/xlsx-to-csv`, `/import`, `/inflation`, `/cpi-calculator`, `/fasebis-kaleidoskopi`, `/khelpasebis-kalkulatori`, `/pui-portali`, `/sagareo-vachrobis-portali`, `/saertashoriso-shedarebis-portali`, `/automobilebis-statistikis-portali` | 11 |
| 2 — managed Access v1 | `/imports/access` | 1 |
| 3 — generic platform | `/platform/{contracts,pages,ingestion,publication,metrics,sql,visualizations,access,operations,site-contracts,products/*/tenant}`, `/dynamic/pages`, `/sdmx` | ~17 |
| 4 — artifact subsystem | `/platform/artifacts`, `/platform/artifacts/package-runs` | 2 |
| 5 — statistical subsystem | `/platform/products/{productCode}/statistical-contracts`, `.../charts`, `.../statistical-references`, `/platform/statistical-references` | 4 |

**Legacy taxonomy already implemented in the target** (`security/legacy/LegacySurface`,
untracked): four families — `ACCESS_UPLOAD`, `SPREADSHEET_CONVERSION`, `DATABASE_EXPORT`,
`DEMO` — each with an operator environment switch and property key, applied via
`@LegacySurfaceGate` to **12 controllers**. The enum's own Javadoc states the invariant:
"no site, domain or page literal appears here, so adding a legacy controller to a family
never branches the engine." This is doctrine-aligned and should be **reused as the
behavior-classification axis**, not replaced.

**In-repository frontend consumers (NEW, and architecturally decisive):**

| Consumer | Endpoints it calls | Generation |
|---|---|---|
| `frontend/kids` | `/platform/pages/`, `/platform/contracts/`, `/platform/operations/exports` | 3 — generic platform |
| `frontend/geostat-system-app` | `/test/`, `/test/dashboardStats`, `/test/revenueChart`, `/test/usersChart`, `/import/mssql-to-access` | 1 — legacy only |
| `frontend/web` `[LEGACY]` | not determined; it is a Gradle/Java module, not a JS app | NOT_INSPECTED |

Two conclusions follow directly:

1. **The CF-021/CF-022 defects have an identified live consumer.** `frontend/kids` calls
   `/platform/pages/`, served by `CanonicalPageDataController` ->
   `CanonicalPageDataService` -> `ContractPhysicalQueryService.canonicalSource` — the
   exact code that accepts `SEMANTIC_REVIEW`. CF-021 is not an abstract query-text
   defect; it is on the path of the platform's flagship portal.
2. **The control-plane UI consumes only legacy surfaces.** `geostat-system-app` calls no
   `/platform/*` endpoint at all. It depends on `LegacySurface.DEMO` (`/test/*`) and on
   `LegacySurface.DATABASE_EXPORT` (`/import/mssql-to-access`, the CF-026 route). This
   partially answers unresolved question 21: those two families have a real, in-repository
   consumer, so retirement requires migrating the control-plane UI first. It also means
   CF-026 cannot simply be deleted.

Remaining consumer gaps: `frontend/web`, `backend/mobile`, and any external/out-of-repo
caller. No evidence about external callers exists in this repository.

### 23.23 Batch 3 — behavior classification (binding constraint: ~40% -> ~60%)

| Behavior | Classification | Basis |
|---|---|---|
| Gate-bound, snapshot-scoped publication (`requireReleasable`, facts digest, evidence schema) | **REQUIRED** | recovered intent, 23.13 |
| Package-run stage order and gate-last sequencing | **REQUIRED** | `PackageRunService` validation, 23.18 |
| Carry-forward of one snapshot per dataset version in a product release | **REQUIRED** | `ROW_NUMBER() PARTITION BY dataset_version_id`, 23.7 |
| Product-wide promotion of every `REVIEW_REQUIRED` snapshot | **INCORRECT** | contradicts the gate contract it is supposed to honour |
| Serving `SEMANTIC_REVIEW` snapshots | **INCORRECT** | contradicts `ServingPolicy`'s stated invariant; reachable per 23.18 |
| Accepting `'APPROVED'` snapshot status on read | **OBSOLETE** | no writer produces it; 23.18 |
| `dataset_snapshot.status` as a liveness authority | **INCORRECT / to be demoted** | RC-001 |
| `publication.snapshot_member` as the release boundary | **REQUIRED / to become sole authority** | RC-001 |
| Caller-supplied identifiers in `buildSelectQuery` | **INCORRECT** | CF-026; doctrine-prohibited |
| Caller-supplied connection target on the export route | **COMPATIBILITY-REQUIRED, gated** | live consumer `geostat-system-app`; `LegacySurface.DATABASE_EXPORT` |
| `/test/*` demo payloads | **COMPATIBILITY-REQUIRED, gated** | live consumer `geostat-system-app`; `LegacySurface.DEMO` |
| Statistical loader's own transaction + checksum idempotency | **REQUIRED** | section 7.2; must survive any migration onto the run pipeline |
| Statistical loader bypassing the run pipeline | **UNRESOLVED — partial migration** | 23.19 |
| Global string trimming in `WideRowNormalizer` | **UNKNOWN — needs characterization** | CF-019, unchanged |
| Observation attributes written but not read | **INCORRECT** | CF-005, DR-REC-006 |

### 23.24 Completeness gate status after batch 3

| Gate question | Status |
|---|---|
| 1-6 responsibilities, concepts, contracts, sources of truth, invariants, flows | substantially answered |
| 7-10 competing implementations, contradictions, legacy, incomplete migrations | answered for publication/serving/ingestion; open for charts, packaging, grammar |
| 11 specialization vs duplication | partially — legacy taxonomy gives the axis for generation 1 |
| 12 documentation/test/contract/code disagreement | partially — test corpus still largely unclassified |
| 13-14 recoverable intent vs undeterminable | improving; RC-001 and gate intent now recovered |
| 15 behavior to protect | ~60% |
| 16-18 what becomes canonical, why, in what order | RC-001 direction is evidence-backed; sequencing not yet drafted |
| 19-21 regression, quality and drift protection | not yet drafted |

Consumer inventory ~55%, behavior classification ~60%. **The gate is still not
satisfied.** The master plan must not be written yet.

### 23.26 Batch 4 — the serving dispatch is a facade, and there is a second dead registry

`ContractPageExecutionRegistry` (53 lines), `ContractPageExecutionAdapter` (24),
`PageFamily` (35) and `PageDataAdapterRegistry` (43) were read to EOF.

**Finding 1 — no family has family-specific behavior.** The registry's Javadoc promises
"provider/family execution ports" whose "family semantics live behind registered
adapters". In fact the constructor loops over seven families and registers **seven
byte-identical anonymous adapters**, each delegating to the same three
`ContractPhysicalQueryService` calls (`rows`, `rowsWithRelationsWhere`, `count`) keyed
only on `dataset_code`. `PageFamily` is computed, dispatched on, and then never
influences execution.

**Finding 2 — the one differentiated adapter is dead code.** Lines 19-32 build a
`relational` ENTITY adapter carrying a considered comment about relation fan-out. It is
assigned to a local variable and **never inserted into the map**; the subsequent loop
registers the generic adapter for `ENTITY` as well. The dead adapter's body is in any
case identical to the loop's, so even its documented intent is not differentiated.

**Finding 3 — `PageFamily.ROOT` has no adapter.** The enum declares eight constants;
the registration array covers seven and omits `ROOT`. A dataset whose contract declares
`data_family = 'ROOT'` resolves to `PageFamily.ROOT` and then fails in `require(...)`
with `IllegalArgumentException("No contract page adapter for ROOT")` at request time,
rather than being rejected at contract authoring. Fail-closed, but at the wrong boundary
and with an operator-hostile error. Whether any contract declares `ROOT` is
**UNVERIFIED**.

**Finding 4 — `PageDataAdapterRegistry` is an abandoned parallel dispatch design.** It is
not a Spring bean, and a repository-wide search finds **zero** calls to its `register` or
`require` methods. The only live part of the file is the nested `ReadContext` record,
which the surviving registry uses. The shell of an earlier dispatch architecture persists
because one record inside it is still referenced.

- **Classification:** EXPERIMENTAL / ABANDONED (`PageDataAdapterRegistry`) plus
  FRAMEWORK PROLIFERATION WITHOUT OBSERVED NEED (`ContractPageExecutionRegistry`), which
  `ANTI-PATTERNS.md` maps to ARC-001/GOV-004 and the doctrine's rule that abstraction is
  not added for its own sake.
- **Do not simply delete either.** The abstraction is the intended extension point for
  provider/family substitution required by SCH-003; the defect is that it is unused, not
  that it is unwanted. Removal versus completion is an architectural decision.

### 23.27 Batch 4 — direct answers to the adapter questions

| Question | Answer | Basis |
|---|---|---|
| Does any adapter override snapshot resolution? | **No** | all seven delegate to `ContractPhysicalQueryService` |
| Does any adapter bypass `canonicalSource`? | **No** | `canonicalSource` is chosen inside `rows`/`count` by `table_role`, not by the adapter |
| Does any adapter change publication/membership semantics? | **No** | none references `publication.*` |
| Does any adapter change serving eligibility? | **No** | `ServingPolicy.require` is inside `ContractPhysicalQueryService`, applied uniformly |
| Does any adapter introduce an alternative source of truth? | **No** | but the dead `PageDataAdapterRegistry` is a structural remnant of one |
| Does any adapter contain site-specific behavior? | **No** — the site-specific vocabulary is one layer above, in the orchestration signature (23.30) | |
| Does any adapter narrow or expand CF-021/CF-022? | **Neither** | they are confirmed to apply **uniformly to every family, including STATISTICAL** |
| Hidden architecture branches? | **Yes, two** | the dead registry and the unregistered `ROOT` family |

**Consequence:** CF-021 and CF-022 are confirmed unchanged and are now known to be
**engine-wide**, not family-scoped. No adapter mitigates them for any consumer.

### 23.28 Batch 4 — the frontend/kids execution path, end to end

```text
frontend/kids
 -> GET /platform/pages/{pageId}/data            CanonicalPageDataController
      @TenantScoped, @PreAuthorize("hasAuthority('READ_RESOURCE')")
      @TenantScopeExemption(DEFAULT_CONTRACT) when no contractCode is supplied
      ETag + private max-age=60 + Vary(Origin, Accept, Accept-Language, Authorization)
 -> CanonicalPageDataService.read(contractCode, pageId, ...)
      contract_page_binding(ACTIVE) + site_contract_revision(APPROVED, max revision)
 -> ContractPageExecutionRegistry.require(PageFamily.fromDataFamily(data_family))
 -> generic anonymous adapter (identical for all families)
 -> ContractPhysicalQueryService.rows(...)
      contract_table_definition / contract_field_definition (APPROVED/ACTIVE)
      ServingPolicy.require(...)
      ContractQueryCompiler.compile(filters, approved field allowlist)
 -> canonicalSource(...) for ENTITY/STATISTICAL tables
      publication.dataset_snapshot WHERE status IN
        ('SEMANTIC_REVIEW','PUBLISHED','APPROVED')          <-- CF-021
```

**Objective 4 is answered: `frontend/kids` does observe the CF-021 disclosure path.**
No adapter, policy or controller between the portal and `canonicalSource` alters the
accepted status set. The portal is served from a query that admits `SEMANTIC_REVIEW`,
which section 23.18 established is a mandatory intermediate state of every governed
package run. `ServingPolicy.require` is enforced on the way, but it governs *which table*
may be served, not *which snapshot*.

The second consumer surface, `POST /platform/contracts/{contractCode}/pages/{pageId}/query`
(`ContractQueryController`), reaches the same `CanonicalPageDataService` and therefore the
same query. It adds real governance the GET route lacks: `ContractQueryPlanService.compileForPage`
before any data-plane access (with an explicit comment that this prevents cursor requests
from bypassing field/aggregation/relation declarations), `QueryAdmissionBudget.cost/admit`,
and fingerprint-bound keyset cursors.

### 23.29 Batch 4 — NEW CF-027: every aggregation is computed over at most 1000 rows

- **Evidence A:** `CanonicalPageDataService.aggregate` constructs
  `new ReadContext(meta, 1, 1000, request.filters(), Map.of(), null×5)`.
- **Evidence B:** none of the seven registered adapters overrides `aggregate`, so the
  interface default in `ContractPageExecutionAdapter:13-23` runs. It calls `read(...)`
  with page 1 and limit 1000, then hands the returned rows to
  `ContractAggregationPlanner.aggregate(rows, groups, field, operation)`.
- **Evidence C:** `ContractAggregationPlanner` (12 lines) buckets the **rows passed to
  it**. It issues no SQL. `SUM`, `COUNT`, `AVG`, `MIN` and `MAX` are all computed in
  memory over that capped list.
- **Evidence D:** `ContractPhysicalQueryService.rows` clamps `limit` to 1000
  unconditionally.
- **Impact:** `SUM` over a dataset with more than 1000 matching rows returns a **silently
  partial result**. The response carries `data`, `groupBy` and `aggregation` but **no
  total, no truncation flag and no warning**. `AVG`, `MIN` and `MAX` are likewise
  computed over an arbitrary first page.
- **Sharpening contrast:** the same adapter's `total()` calls
  `ContractPhysicalQueryService.count`, which issues a real `COUNT_BIG(*)` over the full
  table. The engine is fully capable of server-side aggregation; the aggregate path
  simply does not use it. So pagination totals are correct while aggregates are not.
- **Reachable on both consumer surfaces:** the GET page route and the governed
  `POST .../query` route (`if (request.aggregation() != null || !request.groupBy().isEmpty())
  return respond(pages.aggregate(...))`). `QueryAdmissionBudget` admits the request and
  then the aggregate is taken over a truncated read, which makes the result *look*
  governed.
- **Affected invariants:** INV-005 (lossless value semantics), API-001 (bounded query
  plans must not silently change the answer), and the doctrine's prohibition on silent
  capability loss.
- **Risk:** HIGH, consumer-facing correctness. **Confidence:** HIGH (all four classes
  read). **UNVERIFIED:** whether any contract-declared page currently exceeds 1000 rows —
  section 14 item 1 records 880 KIDS observations, which is below the cap, so KIDS may
  not currently expose the defect. That is a fact about today's data, not about the code.
- **Additional hidden assumption (QF-008):** the default `aggregate` selects the value
  field as `filters().containsKey("value") ? "value" : filters().containsKey("numericValue")
  ? "numericValue" : "value"` — a hardcoded column-name convention in a schema-agnostic
  engine, not a contract-derived measure reference.

### 23.30 Batch 4 — NEW CF-028: five declared page parameters are silently inert

- **Evidence A:** `CanonicalPageDataController` accepts `metricCode`, `carrierCode`,
  `periodFrom`, `periodTo` and `ageGroup` as `@RequestParam` and passes them positionally
  into `CanonicalPageDataService.read(...)`. Only `resourceId`, `goalId` and `glossaryId`
  are placed into the `filters` map.
- **Evidence B:** those five values travel through four `read` overloads into
  `ReadContext`. A repository-wide search for `.metricCode()`, `.carrierCode()`,
  `.periodFrom()`, `.periodTo()` and `.ageGroup()` returns **exactly one consumer**:
  `ContractPageExecutionAdapter.aggregate`, which only copies them into a new
  `ReadContext` before calling `read`, which ignores them.
- **Evidence C:** no registered adapter reads any of the five. The older `dynamic` page
  path does not use them either.
- **Impact, scoped precisely:**
  - On `GET /platform/pages/{pageId}/data` — the route `frontend/kids` consumes — the
    five parameters are **accepted and silently discarded**. A caller believing it has
    filtered by period or age group receives unfiltered data with no error.
  - On `POST /platform/contracts/{code}/pages/{id}/query` there is **no loss**:
    `ContractQueryController` extracts the same five keys out of `request.filters()` to
    fill the positional arguments but **also passes the whole filters map**, so the keys
    remain subject to `ContractQueryCompiler` and the approved field allowlist. Here the
    positional arguments are merely vestigial duplication.
- **Site-specific vocabulary in generic core:** `carrierCode` and `ageGroup` are KIDS
  concepts, and `resourceId`/`goalId`/`glossaryId` are KIDS field names hardcoded as the
  GET route's only working filter surface. A second site with different field names has
  no filter capability on the GET route at all and must use the POST query route. This is
  an SCH-003 constraint expressed in a controller signature rather than in a branch.
- **Classification:** declared-but-unimplemented capability. Whether the five were ever
  wired is **UNVERIFIED** — establishing it requires git archaeology on
  `CanonicalPageDataService`, which this session did not perform.
- **Affected invariants:** UI-001 (explicit unsupported handling; declared capability
  must be preserved or explicitly rejected), INV-003, SCH-003.
- **Risk:** HIGH on the GET route (consumer-facing wrong answers), LOW on the POST route.
  **Confidence:** HIGH.

### 23.31 Batch 4 — REVISION of QF-003 (traceability preserved)

- **PREVIOUS FINDING (session 2, 23.10):** QF-003 recorded that
  `CanonicalPageDataService.read(int pageId, ...)` "resolves a *global* default approved
  contract with no product or tenant scope — a single-site assumption on a multi-site
  platform".
- **NEW EVIDENCE:** `CanonicalPageDataController` carries `@TenantScoped` and, on the
  default-contract branch, an explicit
  `@TenantScopeExemption(value = TenantScopeExemption.Kind.DEFAULT_CONTRACT, reason = "Without
  an explicit contractCode the controller serves the platform's approved default contract;
  the interceptor resolves and enforces that same contract.")`. The controller also
  resolves `contracts.resolve()` itself and rejects a blank result.
- **REVISED CONCLUSION:** this is a **declared and governed exemption with an enforcing
  interceptor**, not an unguarded tenancy hole. QF-003 is downgraded from a tenancy
  concern to an architectural observation: the platform retains a notion of a single
  "approved default contract", which is a legitimate convenience but encodes a
  primary-site assumption worth an explicit decision under SCH-003.
- **Still open:** whether `TenantScopeExemption` instances are inventoried and reviewed
  anywhere. The exemption mechanism itself is good practice; its register was not found.
  Added to unresolved questions.

### 23.32 Batch 4 — inventory progress

**Consumer inventory (~55% -> ~70%).** Added this batch:

| Consumer surface | Route | Governance on the route |
|---|---|---|
| `frontend/kids` | `GET /platform/pages/{pageId}/data` | tenancy, `READ_RESOURCE`, ETag/cache; **no** query-plan compilation, **no** admission budget |
| generic/API callers | `POST /platform/contracts/{code}/pages/{id}/query` | tenancy, `READ_RESOURCE`, plan compilation, admission budget, fingerprint-bound keyset |
| `frontend/kids` | `/platform/contracts/`, `/platform/operations/exports` | not yet traced to controllers |

Notable asymmetry recorded for the plan: the two page-serving routes reach identical
execution but carry **different governance**. The route the flagship portal uses is the
weaker of the two.

Remaining gaps unchanged: `frontend/web` `[LEGACY]`, `backend/mobile`, external callers.

**Behavior classification (~60% -> ~72%).** Added:

| Behavior | Classification |
|---|---|
| Family dispatch via `PageFamily` | **OBSOLETE-AS-IMPLEMENTED** — the abstraction exists, the differentiation does not |
| `relational` ENTITY adapter | **DEAD CODE** |
| `PageDataAdapterRegistry` | **EXPERIMENTAL / ABANDONED**, except `ReadContext` which is **REQUIRED** |
| `ROOT` family without an adapter | **INCORRECT** — failure at request time instead of authoring time |
| In-memory aggregation over ≤1000 rows | **INCORRECT** (CF-027) |
| `total()` via SQL `COUNT_BIG` | **REQUIRED** |
| Five inert page parameters on the GET route | **INCORRECT** (CF-028) |
| The same five on the POST query route | **OBSOLETE** vestigial duplication, no loss |
| Query-plan compilation before data access on the POST route | **REQUIRED** |
| `QueryAdmissionBudget` cost/admit | **REQUIRED** |
| ETag/`Vary`/private caching on page reads | **REQUIRED** |
| `TenantScopeExemption(DEFAULT_CONTRACT)` | **REQUIRED, governed** (23.31) |

### 23.33 Batch 4 — partial migration scope: PackageRun vs StatisticalLoadService

Scope determination only; no remediation designed.

| Responsibility | Governed pipeline | `StatisticalLoadService` | Duplicated? |
|---|---|---|---|
| Source admission / receipt | `IngestDatasetStage` -> `ingest.batch`, `ingest.artifact` | writes `ingest.batch`, `ingest.artifact` itself | yes |
| Technical validation | `ValidateLoadStage` -> `ingest.staged_row.validation_status`, load `VALIDATED` | `WideRowNormalizer` + own quarantine counts | yes, different vocabulary |
| Snapshot creation | `PrepareSnapshotStage` -> `PlatformSnapshotPreparationService` (checksum/provenance checks, `UPDLOCK,HOLDLOCK`, row-count assertion) | direct `INSERT ... 'REVIEW_REQUIRED'` | yes, **without** the provenance assertions |
| Semantic materialization | `MaterializeStage` -> `SemanticMaterializationService` | `CanonicalObservationWriter` | specialized, not duplicate |
| Attachment binding | `BindAttachmentsStage` | none | missing |
| Reconciliation | `ReconcileStage` | none | missing |
| Release gates | `EvaluateReleaseGatesStage` -> `ReleaseGateService` | **none** | bypassed |
| Resumability / stage history | `PackageRunRepository`, stage history, resume from stage | single synchronous transaction | not available |

- **Bypassed contracts/invariants:** INV-008 (gate-bound publication), the snapshot
  provenance assertions in `PlatformSnapshotPreparationService:41-46`, and the run
  pipeline's own ordering contract.
- **Affected consumers:** any consumer of statistically-loaded data, i.e. the statistical
  chart/export surfaces and — once such a dataset is bound to a contract table with
  `table_role = 'STATISTICAL'` — `/platform/pages` and therefore `frontend/kids`.
- **Migration boundary (for a later plan, not decided here):** the statistical loader's
  single Data Plane transaction and checksum idempotency (section 7.2) are REQUIRED
  behavior and constrain any move onto the staged pipeline, whose stages commit
  independently. This tension is the core of the decision and must not be resolved by
  assumption.

### 23.34 Completeness gate status after batch 4

Consumer inventory ~70%; behavior classification ~72%. Gate questions 7-10 are now
answered for serving dispatch as well as publication. Still open and binding:

- gate question 12 — the 122-file test corpus remains unclassified, so no behavior
  protection strategy can be written;
- gate questions 16-18 — canonical sequencing not drafted;
- gate questions 19-21 — regression, quality and drift protection not drafted;
- chart, packaging and grammar convergence (CF-009, PA-001, PA-002) still unexamined at
  source level.

**The gate is not satisfied. The master plan must not be written.**

### 23.36 Batch 5 — test corpus census and the shape of its coverage

**Corpus:** 122 test files, **454 `@Test` methods**. This independently corroborates the
historical `AIR-2026-048` evidence of "454 API tests" quoted in section 2.3, so the
present dirty tree has the same test count as that record.

Distribution: `service/platform` 47, `service/artifact` 22,
`service/platform/statistical` 14, `security/tenancy` 6, `security/legacy` 6,
`controller` 5, `service/platform/access` 4, `config` 4, `service/publication/gate` 2,
`service/contract/approval` 2, `service/catalog` 2, remainder 8.

**The single most important structural finding of this batch is the *shape* of the
coverage, not the gaps themselves.**

`ReleaseGateServiceTest.anyFailingGateKeepsSnapshotOutOfReviewAndBlocksPublication`
proves that the **guard** refuses. Nothing in the corpus proves that the **writer**
honours the scope of that refusal. The release gate is tested thoroughly; the release
writer is not tested at all. That asymmetry is precisely what allowed CF-023 to exist
and to pass CI indefinitely: the suite verifies the part of the publication contract
that is correct and never exercises the part that is wrong. A future plan that treats
"tests pass" as publication safety evidence would repeat the error.

### 23.37 Batch 5 — coverage of each recorded conflict

The task specification asked, for CF-021/CF-023/CF-027/CF-028, whether tests (1)
intentionally assert the incorrect behavior, (2) accidentally encode it, (3) fail to
cover it, (4) contradict recovered intent, or (5) would block correct rehabilitation.

| Conflict | Test evidence | Category |
|---|---|---|
| **CF-021** pre-publication snapshots served | `ContractPhysicalQueryService` — the class containing `canonicalSource` — has **no test**. Only `ContractQueryCompilerTest` matched the search, and it covers a different class. | **(3) not covered** |
| **CF-022** two liveness models | No test compares the membership-based and status-based surfaces. `ServingPolicy.rawRows`, whose Javadoc states the PUBLISHED-only invariant, is **not exercised** by `ServingPolicyTest`. | **(3) not covered** |
| **CF-023** product-wide publication | **Zero tests.** No file references `PublishSnapshotRequest`, `RollbackPublicationRequest`, `PublicationReceipt` or `publication.snapshot_member`. `PlatformPublicationService`, `DataPlanePublicationWriter`, `PublicationControlStore` and `PlatformPublicationController` are entirely untested. | **(3) not covered** |
| **CF-024** site contract has no producer | Consistent with batch 3: the only writer of `site_contract_dataset` in the repository is the test fixture `PackageContractSourceTest`. The production gap is mirrored by the test corpus. | **(3) not covered** |
| **CF-025** snapshot state machine | `StatisticalLoadServiceTest:151` asserts `assertEquals("REVIEW_REQUIRED", ... SELECT status FROM publication.dataset_snapshot ...)`. | **(1) intentionally asserts it** |
| **CF-026** caller-supplied identifiers | `MSSQLToAccess` is referenced only by `LegacySurfaceAuthorityTest`, which covers the gate/authority, not `buildSelectQuery`. No injection or identifier test exists. | **(3) not covered** |
| **CF-027** truncated aggregation | `ContractAggregationPlanner` has **no test**; neither do `ContractPageExecutionRegistry` or `ContractPageExecutionAdapter`. | **(3) not covered** |
| **CF-028** five inert parameters | **No test anywhere** uses `metricCode`, `carrierCode`, `periodFrom`, `periodTo` or `ageGroup`. `CanonicalPageDataService` has no test; the only page-data test is `DynamicDataControllerNoFallbackTest`, which covers the older `dynamic` path. | **(3) not covered** |

**Answers to (4) and (5):**

- **(4) Contradiction with recovered intent:** no test contradicts the recovered intent.
  The gate tests **affirm** the intent (per-snapshot, gate-bound publication) that
  `DataPlanePublicationWriter` violates. Tests and intent agree; the implementation is
  the outlier.
- **(5) Would tests block correct rehabilitation?** For CF-021, CF-023, CF-027 and
  CF-028: **no** — there is nothing to resist, because there is nothing there. For
  CF-025 and the PackageRun/StatisticalLoadService partial migration: **yes** —
  `StatisticalLoadServiceTest:151` will fail the moment statistical loads are routed
  through `SEMANTIC_REVIEW` and the gate transition.

**Consequence for sequencing:** the four highest-risk defects are simultaneously
*unblocked* and *unprotected*. Correcting them requires writing the protection first;
it does not require renegotiating any existing assertion. This is a materially
different, and better, position than the checkpoint previously assumed.

### 23.38 Batch 5 — test classification

**ARCHITECTURAL_INVARIANT_TEST — must survive unchanged**

| Test | Protects |
|---|---|
| `ReleaseGateServiceTest.cleanEntitySnapshotPassesAllGatesAndMovesToReview` | the gated `SEMANTIC_REVIEW -> REVIEW_REQUIRED` transition |
| `...anyFailingGateKeepsSnapshotOutOfReviewAndBlocksPublication` | INV-008 at the guard |
| `...legacyAssertedEvidenceIsNotAccepted` | evidence-schema anti-forgery (`geostat.release-gate.v1`) |
| `...factsDriftAfterEvaluationBlocksPublication` | facts-digest binding |
| `...publishedSnapshotIsNotReEvaluated` | `EVALUABLE_STATES` boundary |
| `PlatformReleaseGatesTest` — `schemaValid`, `keysValid`, `relationsValid`, `classifiersValid`, `statisticalSemanticsValid`, `rawLineageValid` | the six gate implementations |
| `ProductTenancy*` / scope tests — `everyScopeKindIsEnforced`, `ownTenantPassesEveryScopeKind`, `requireThrowsADenialThatCarriesNoObjectAttribute`, `anUnresolvedObjectIsDeniedIdenticallyToAForeignObject` | INV-009, and denial responses that leak no existence oracle |
| `ServingPolicyTest` — `theDataPlaneIsServedAndAnUndeclaredTableIsNot`, `aNonDataTableIsServedOnlyWhenTheContractSaysSo`, `rawDocumentsNeedTheirAuthorityAndAnonymousCallersGetNothing` | deny-by-default serving, authority requirement, anonymous refusal |
| `PageFamilyTest.resolvesDeclaredFamiliesWithoutProductNames` | INV-003 intent — but see coverage gap below |

**REQUIRED_BEHAVIOR_TEST**

`PlatformSnapshotPreparationServiceTest` — `rejectsArtifactThatDoesNotBelongToDatasetLoadBeforeWriting`,
`rejectsMalformedChecksumBeforeDatabaseAccess`, `rejectsChecksumThatDiffersFromTheLoadArtifact`,
`returnsAnExistingSnapshotOnlyWhenItsArtifactProvenanceIsConsistent`,
`replayRejectsRowsFromAnotherArtifact`. These protect the provenance assertions at
`PlatformSnapshotPreparationService:41-46` that the statistical loader bypasses (23.33),
and they are the strongest argument that those assertions are REQUIRED rather than
incidental.

**COMPATIBILITY_REQUIRED_TEST**

The 27 legacy tests across `security/legacy` (`DatabaseEndpointGuardTest` 3,
`HostAllowListTest` 4, `LegacyOperatorAuthorityParityTest` 4, `LegacySurfaceAuthorityTest` 2,
`LegacySurfacePolicyTest` 5, `RemoteFetchGuardTest` 6) and `controller/LegacySurfaceGateTest` 3.
They protect the INV-012 retirement mechanism and the guards that make CF-026's route
operable under control. They must survive until the `DATABASE_EXPORT` and `DEMO`
consumers in `geostat-system-app` are migrated (23.22).

**INCORRECT_BEHAVIOR_TEST / IMPLEMENTATION_COUPLED_TEST**

`StatisticalLoadServiceTest:151`. Classified as **implementation-coupled encoding a
lifecycle decision**: the assertion is a correct description of today's loader, but it
pins a *platform lifecycle state* inside a *loader unit test*. The status a load lands
in is a publication-lifecycle decision owned by the gate subsystem, not a property of
the loader. Whatever the eventual decision on the partial migration, this assertion
should be **rewritten to assert the loader's own contract** (rows, checksum,
idempotency, quarantine counts) and the lifecycle state should be asserted where it is
owned. Note the asymmetry: `PlatformSnapshotPreparationServiceTest` does **not** assert
the inserted status, so the generic path's identical `REVIEW_REQUIRED` insert is not
pinned by any test.

**INSUFFICIENT_COVERAGE**

- `PageFamilyTest` exercises only `fromNodeKind` with four cases. The live dispatch path
  uses `fromDataFamily`, which is **untested**, and the `ROOT`-without-adapter defect
  (23.26) is uncovered.
- `ServingPolicyTest` builds every fixture from KIDS-named table codes
  (`KIDS_STATISTICAL_CARRIER`, `KIDS_RAW_DOCUMENT`, `KIDS_RESOURCE`,
  `KIDS_STATISTICAL_*`). The class under test is genuinely generic, but the test does
  **not** satisfy SCH-003's requirement of "two materially different fixtures", so it is
  not evidence of site-agnosticism.

### 23.39 Batch 5 — protection gaps: REQUIRED behavior with no test

These are recovered REQUIRED behaviors that currently have **no meaningful test
protection**. Each is a protection task the Master Plan must schedule *before* the
corresponding correction.

| Unprotected behavior | Owner class | Related |
|---|---|---|
| Publication is per-snapshot and bounded to approved members | `DataPlanePublicationWriter`, `PlatformPublicationService` | CF-023, INV-008, RC-001 |
| Rollback restores dataset-snapshot state, not only the publication row | `DataPlanePublicationWriter.rollback` | CF-023 |
| Control-plane intent and outbox describe the release that occurred | `PublicationControlStore` | CF-023, REL-001 |
| Only released snapshots are served | `ContractPhysicalQueryService.canonicalSource` | CF-021, INV-008 |
| One deterministic snapshot per read | `canonicalSource` set-valued `IN` | CF-022 |
| Raw serving reads only the newest PUBLISHED snapshot | `ServingPolicy.rawRows` | CF-022 |
| Physical identifiers derive only from approved contract metadata | `ContractPhysicalQueryService.identifier` | Q18, SEC-002 |
| Include fan-out caps (2000 keys / 1000 rows), depth 8, cycle guard | `ContractPhysicalQueryService` | API-001 |
| Keyset requires a contract-declared `required=1` index | `ContractPhysicalQueryService.rowsKeyset` | API-001 |
| Aggregation reflects the whole matching set or declares truncation | `ContractAggregationPlanner`, adapter default | CF-027 |
| Declared page parameters either filter or are rejected | `CanonicalPageDataController`, `CanonicalPageDataService` | CF-028, UI-001 |
| Family dispatch resolves and executes the declared family | `ContractPageExecutionRegistry`, `PageFamily.fromDataFamily` | 23.26 |
| Caller-supplied identifiers cannot escape quoting | `MSSQLToAccess.buildSelectQuery` | CF-026 |
| Site-contract declarations can be produced and approved | no producer exists | CF-024, SCH-003 |

**Concentration observation:** every class on the critical serving-and-release path —
`ContractPhysicalQueryService`, `CanonicalPageDataService`, `DataPlanePublicationWriter`,
`PlatformPublicationService`, `ContractAggregationPlanner`,
`ContractPageExecutionRegistry` — is untested, while the classes *around* them (gates,
tenancy, serving policy, snapshot preparation, contract approval) are well tested. The
untested set is almost exactly the set in which this recovery found P0-class defects.
That correlation is itself evidence for where remaining undiscovered defects are most
likely to be.

### 23.40 Batch 5 — inventory progress

- **Test corpus classification: ~0% -> ~45%.** All nine recorded conflicts now have a
  determined coverage status; the architecturally significant tests in
  `publication/gate`, `contract/approval`, `security/tenancy`, `security/legacy`,
  `platform` serving and `platform/statistical` are classified. Not yet classified: the
  bulk of `service/platform` (47 files) and `service/artifact` (22 files) beyond those
  named above.
- **Consumer inventory: ~70% (unchanged).** No new consumer was revealed by test
  evidence.
- **Behavior classification: ~72% -> ~78%.** Test evidence promoted the snapshot
  provenance assertions and the gate evidence-integrity rules from "recovered intent" to
  "intent with active test protection".

No batch-4 finding was contradicted. CF-021/CF-022 remain engine-wide; the adapters
remain behaviorally identical; the `relational` adapter remains dead;
`PageDataAdapterRegistry` remains abandoned except for `ReadContext`; QF-003 remains a
governed exemption; the PackageRun/StatisticalLoadService transactional tension remains
the core of that migration decision.

### 23.41 Completeness gate status after batch 5

Gate question 12 (documentation/test/contract/code disagreement) is now substantially
answered for the test axis: tests do **not** disagree with recovered intent; they are
absent where the implementation diverges from it.

Still open and binding:

- chart, packaging and grammar convergence (CF-009, PA-001, PA-002) — unexamined at
  source level, and questions 7-8 depend on them;
- gate questions 16-18 — canonical sequencing not drafted;
- gate questions 19-21 — regression, quality and drift protection strategies not
  drafted, though 23.39 now supplies their raw material;
- statistical authorization boundary (CF-008) still unverified against final dirty
  sources;
- remaining ~65 unclassified test files.

**The gate is not satisfied. The master plan must not be written.**

### 23.43 Batch 6 — decision authority adopted

From this batch onward the recovery operates under **Principal Engineering Decision
Authority**: where repository evidence is sufficient, the architectural decision is made
here rather than returned to the user. Decisions carry FACT / INFERENCE / ASSUMPTION /
DECISION labels, a confidence level, and a recorded falsification attempt. Decisions
remain traceable EVIDENCE -> INVARIANT -> CONFLICT -> ROOT CAUSE -> DECISION -> TARGET ->
MIGRATION -> VALIDATION, and none of them is executed during recovery.

### 23.44 Batch 6 — there are four chart mechanisms, not three

FACT. The corpus contains four, not three:

| # | Mechanism | Declaration store | Data resolution | Generation |
|---|---|---|---|---|
| 1 | core `ChartDefinition` + `DynamicChartService` (92 lines) | JPA `chart_definition` via `DataProfile`/`PageNode`, `PublicationStatus.PUBLISHED` on the definition | direct SQL built over a profile schema/table with an allow-list | 1 — pre-platform |
| 2 | `ManagedChartDefinition` / `ManagedChartFilter` | Access `__gs_chart`, `__gs_chart_filter`, imported into generation 1's tables | via generation 1 | 2 — Managed Access v1 |
| 3 | `platform.visualization_definition` + `PlatformVisualizationQueryService` (23 lines) | Control Plane, `status='PUBLISHED'`, `chart_type` + free `config_json`, bound to one `metric_id` | `PlatformMetricQueryService` -> `StatisticalQueryAdapter` port | 3 — generic platform |
| 4 | `platform.statistical_chart` + `ChartService` (148) / `ChartCompiler` / `ChartSpec` / `ChartSuggestions` | Control Plane, grammar document, `contract_revision_digest`, `UNIQUE(product_id, dataset_code, chart_code)` | `CanonicalObservationReader` | 5 — statistical |

### 23.45 Batch 6 — the third-path question: answered NO, but the blast radius grows

The batch-5 protocol asked whether any chart surface resolves snapshot authority through
a **third** path, which would require expanding RC-001.

FACT. It does not. Both chart data paths use one of the two models already recorded in
CF-022:

- **Mechanism 3** -> `PlatformMetricQueryService.points` -> `StatisticalQueryAdapter`.
  **Both** implementations resolve through membership:
  `CanonicalStatisticalQueryAdapter:12-17` and `RelationalStatisticalQueryAdapter:23-25`
  join `publication.snapshot` (`status='PUBLISHED'`) to `publication.snapshot_member` to
  `series` to `observation`. This is **Model A**.
- **Mechanism 4** -> `ChartService.data` -> `CanonicalObservationReader.read`, which
  selects `TOP 1 publication.dataset_snapshot WHERE status='PUBLISHED' ORDER BY
  dataset_snapshot_id DESC`. This is **Model B**.

**DECISION: RC-001 is not expanded in scope, but its blast radius is extended to include
both chart surfaces.** CONFIDENCE: HIGH (all four classes read to EOF).

INFERENCE, and the architecturally significant consequence: **the same product's data,
drawn through the two chart authorities, can yield different numbers.** Model A excludes
dataset snapshots that were flipped to `PUBLISHED` without membership; Model B includes
them. CF-023 proves that such snapshots are produced by the ordinary publish path, and
AIR-2026-054 records exactly that residue in a live database (snapshots 53 and 57-61
`PUBLISHED` without current membership). In that recorded state the two chart surfaces
**would** disagree. This is not hypothetical; it is unobserved only because nobody has
compared them.

### 23.46 Batch 6 — NEW CF-029: migration 110 documents a converter that does not exist

- **Evidence A (FACT):** `110_statistical_chart_declaration.sql` header states: "The
  legacy `platform.visualization_definition` stays untouched: its rows are **converted
  and proposed**, never rewritten in place (AGENTS.md: no silent change of an existing
  surface)."
- **Evidence B (FACT):** `ChartSuggestions.propose(SemanticPlan plan)` is a **static,
  pure function of the semantic plan**. It takes no `JdbcTemplate`, no product and no
  Control Plane access, and therefore cannot read `visualization_definition`. It derives
  proposals from the contract's own components.
- **Evidence C (FACT):** `platform.visualization_definition` is read by exactly one class
  in the target, `PlatformVisualizationQueryService`. `platform.statistical_chart` is
  referenced by exactly two, `ChartService` and the migration runner. **No class
  references both models.**
- **CONCLUSION:** no converter exists anywhere in the repository. PA-002's open item
  ("migration 110 comments imply conversion/proposal of legacy charts, but no converter
  was located") is upgraded from *not located* to **does not exist**, by exhaustive
  search rather than absence of evidence.
- **Why this is worse than an ordinary stale document:** the false claim is embedded in an
  **applied, checksummed migration**. Per CF-012 the runner also carries checksum-rewrite
  exceptions, so schema history is already a weak evidence source; a migration asserting
  behavior that was never built degrades it further.
- **Risk:** MEDIUM-HIGH (governance/evidence integrity). **Confidence:** HIGH.

### 23.47 Batch 6 — NEW CF-030 and the chart quality findings

**CF-030 — two chart authorities, two liveness models, one product.** Stated in 23.45.
Risk HIGH, confidence HIGH. This is the concrete consumer-visible manifestation of
RC-001 and is the strongest single argument for resolving RC-001 before any chart
convergence work.

**CF-031 (NEW) — the chart data path bypasses `ServingPolicy`.** FACT: `ChartService.data`
-> `CanonicalObservationReader.read` reads `statistics.observation`,
`observation_dimension` and `classification_item` directly. Neither class calls
`ServingPolicy.require`/`permits`. The generic page path enforces that check for every
table it serves (23.4). **Today this is latent rather than exploitable**, because the
chart endpoints are gated by a *stricter* workflow authority (23.48). **It becomes a real
escalation the moment chart read authority is corrected.** This creates a hard sequencing
constraint: CF-031 must be repaired **before or together with** the CF-008 correction,
never after. Risk: MEDIUM now, HIGH after CF-008 is fixed. Confidence: HIGH.

**QF-009** — `ChartService.stored()` catches `RuntimeException` per row and returns
`null`, filtered out with the comment "a declaration that no longer parses is simply not
offered". A stored, governed declaration disappears from the API with **no error, no log
and no operator signal**. This is silent capability loss on a governed artifact.

**QF-010** — `chart()` revalidates the spec against the approved plan before serving
("a contract revision may have removed what this picture draws"); `charts()` does **not**.
The list therefore advertises charts that the detail endpoint refuses with
`ChartRejected`. Same class, asymmetric invariant enforcement.

**QF-011** — `ChartService.declare()` performs `UPDATE` then conditional `INSERT` with no
`@Transactional` and on a class that is not a Spring `@Service`. The
`UNIQUE(product_id, dataset_code, chart_code)` constraint from migration 110 bounds the
damage to a constraint violation under concurrency rather than a duplicate row, so this
is a robustness defect, not a data-integrity one.

### 23.48 Batch 6 — CF-008 VERIFIED for the chart surface, and a new dependency

FACT. `StatisticalChartController` carries `@TenantScoped` and `@RestController` but
**no `@PreAuthorize` on any of its four endpoints** (`GET /`, `GET /{chartCode}`,
`PUT /{chartCode}`, `GET /data`). Compare `CanonicalPageDataController`, which carries
`@PreAuthorize("hasAuthority('READ_RESOURCE')")`.

FACT. Authorization is enforced inside `ChartService` instead: `declare()` calls
`workflow.mayAuthor(actor, product)` and throws `FORBIDDEN`; the three read paths call
`workflow.approvedPlan(actor, contractId)` and `workflow.get(actor, contractId)`, which
per the inherited CF-008 evidence admit actors holding AUTHOR/APPROVE/IMPORT authority
rather than a generic read role.

**CF-008 is therefore VERIFIED for the chart controller**, with the asymmetry now
precisely characterised: the HTTP layer advertises **no** authority requirement while the
workflow layer demands an **authoring-class** one. An ordinary reader holding
`READ_RESOURCE` — who may read `/platform/pages` — cannot read an approved chart.

This answers unresolved question 12 at the evidence level: today, ordinary read authority
does **not** permit retrieving approved statistical contracts or charts.

**DECISION (HIGH confidence): approved contracts and approved charts are read-governed
resources.**

- Reading an approved artifact drawn from a PUBLISHED snapshot is a read operation and
  must require the platform's ordinary read authority plus tenant scope — the same
  boundary as `/platform/pages`.
- Authoring/approval authority must be required only for `declare` and lifecycle
  transitions.
- Enforcement stays at the **use-case boundary** (the service), because `ANTI-PATTERNS.md`
  explicitly rejects an annotation check presented as authorization proof. The HTTP
  annotation is defence in depth and a discoverability declaration, not the proof.

*Rationale chain:* REQUIREMENTS (published governed content must be readable by readers)
-> DOMAIN SEMANTICS (an approved chart of an approved contract is published output, not a
draft) -> INVARIANTS (SEC-001 fail-closed at execution boundaries; least privilege) ->
DOCTRINE (deny-by-default, but not by conflating authoring with reading) -> CONTRACTS
(charts are validated against the approved plan on write and on read) -> DECISION.

*Falsification attempts.* (a) *Could drafts leak?* No — all three read paths go through
`approvedPlan`, which is approved-only. (b) *Could a reader see data they may not see?*
**Yes — and this is the finding that survives falsification:** `CanonicalObservationReader`
does not apply `ServingPolicy`, so correcting read authority without CF-031 would grant
observation-level access that bypasses the table-level serving authority the page path
enforces. The decision therefore **carries CF-031 as a precondition**. (c) *Is the current
strictness a deliberate confidentiality control?* No evidence supports that: no ADR, no
policy property and no test asserts it, and the governance card records only that HTTP
method security was aligned with `StatisticalContractAccessDecision`.

**Current least-privilege harm.** The present state is not merely inconvenient: it
pressures operators to grant AUTHOR to people who only need to read, which weakens the
separation of duties recorded in section 14 item 14.

### 23.49 Batch 6 — CANONICAL DECISION: one chart authority

Classification under the canonicality rule, then disposition.

| Mechanism | Class | Disposition |
|---|---|---|
| 1 — core `ChartDefinition` / `DynamicChartService` | **E. obsolete architecture** — no contract, no snapshot governance, no revalidation; queries profile tables directly | **DEPRECATE -> DELETE AFTER MIGRATION**, gated by INV-012 consumer inventory |
| 2 — Managed Access `__gs_chart` | **split**: the Access-side declaration is **C. compatibility requirement** (a real authoring transport); its destination is **E. obsolete** | **MIGRATE** the transport onto the canonical model; **DELETE AFTER MIGRATION** the path into core tables |
| 3 — `platform.visualization_definition` | **F. incorrect implementation of declaration, correct implementation of resolution** | **MERGE** into the canonical model |
| 4 — `platform.statistical_chart` | **A. legitimate specialization** of declaration, and the strongest declaration model present | **EVOLVE** into the canonical model |

**DECISION (HIGH confidence on direction, MEDIUM-HIGH on generalisation):** the canonical
chart authority is the **grammar-based declaration model of `platform.statistical_chart`,
generalised beyond the statistical family, combined with the membership-based data
resolution currently used by `visualization_definition`.** One grammar, one store, one
resolution path.

*Rationale.* The declaration model wins on evidence, not novelty: it is a closed grammar
(mark + encoding channels + component references + titles), validated against the
approved plan **on write and again on read**, bound to the exact
`contract_revision_digest`, constrained in the schema (`ISJSON`, code charset, unique
key, FK to product), and able to *derive* proposals from the contract. It is the
doctrine's closed-grammar/open-registry rule applied to visualisation. Migration 110
states the same principle in its own header: a chart is "a declaration in the chart
grammar ... never a chart type with free configuration".

*Why `visualization_definition`'s declaration model is rejected as canonical:*
`chart_type` plus free `config_json`, with
`PlatformVisualizationQueryService:19-20` falling back to
`metadata.fromConfig(definition[2])` — reading **governance metadata out of the
configuration document**. That inverts the authority direction, letting an M0 artifact
supply M1 meaning. It is the schema-hierarchy inversion the doctrine forbids.

*Falsification attempts.*
1. *Is `statistical_chart` too statistical to be canonical for entity and reference
   pages?* Its `ChartSpec` binds encodings to `SemanticPlan` components. Generalising
   requires encodings to reference **contract fields** through the same approved-field
   surface the query compiler already uses. That is a bounded extension of one coupling,
   not a rewrite — but it is the decision's **principal technical risk** and its
   confidence is MEDIUM-HIGH, not HIGH. It must be proven with a non-statistical fixture
   before the merge is executed.
2. *Does the merge lose behavior?* **Yes, one thing — and this survives falsification:**
   `visualization_definition` has a declaration lifecycle (`status='PUBLISHED'`);
   `platform.statistical_chart` has **no status column at all**. The older model is
   better on this axis. The canonical model **must** carry a declaration lifecycle over;
   otherwise a chart becomes live the instant it is declared. Recorded as a REQUIRED
   behavior the merge must preserve.
3. *Am I preserving mechanism 4 merely because it is newest?* No — mechanisms 1 and 3 were
   each evaluated on their own merits, and mechanism 3 won the resolution axis outright.
4. *Am I creating an A+B compromise?* No. One store, one grammar, one resolution. The
   membership resolution is not "kept alongside"; it is the single correct answer under
   RC-001 and applies to all chart data.

**Consequence for CF-029:** migration 110's promised converter becomes a **migration task
of this decision** — convert `visualization_definition` rows into canonical declarations
offered as **proposals**, never auto-approved. The comment describes the right intent; only
the implementation is absent.

**Ordering constraint (FACT, derived):** RC-001 must be settled first. Choosing the
canonical resolution *is* choosing RC-001's answer; doing chart convergence first would
hard-code Model A into a new store before the publication writer is corrected, leaving
`dataset_snapshot.status` as a second authority under a new name.

### 23.50 Batch 6 — inventory progress

- **Behavior classification: ~78% -> ~84%.** Added: the four chart mechanisms, their
  declaration lifecycles, the two resolution paths, contract revalidation on read
  (REQUIRED), proposal derivation (REQUIRED), free-form `config_json` governance fallback
  (INCORRECT), silent drop of unparseable declarations (INCORRECT), asymmetric list/detail
  validation (INCORRECT).
- **Consumer inventory: ~70% -> ~74%.** `PlatformVisualizationController` (2 endpoints)
  and `StatisticalChartController` (4 endpoints, tenancy-scoped, no HTTP authority) are
  now characterised. `frontend/kids`' use of `/platform/contracts/` and
  `/platform/operations/exports` is still not traced to controllers.
- **Test classification: ~45% -> ~48%.** `ChartGrammarTest` and `ChartHttpTest` exist and
  cover the grammar and the HTTP surface of mechanism 4; mechanisms 1, 2 and 3 have no
  identified chart tests, and no test compares the two resolution paths (CF-030 is
  uncovered).

### 23.51 Completeness gate status after batch 6

Gate questions 7-8 (which chart model is canonical, and how legacy charts are converted)
are now **answered** by the decision in 23.49, with the converter question resolved by
CF-029.

Still open and binding:

- package/contract taxonomy (PA-001, PA-003, unresolved question 6) — the boundary among
  Access semantic declaration, artifact manifest, authoring layout and the untracked
  composer `PackagePlan`;
- grammar duality (CF-009) — Java parser versus the untracked JSON Schema;
- the migration-chain rebuild audit (CF-012, CF-020, CF-024);
- gate questions 16-21 — canonical sequencing and the regression/quality/drift protection
  strategies, for which 23.39 supplies the raw material;
- ~65 unclassified test files; `frontend/web`, `backend/mobile` and external callers.

**The gate is not satisfied. The master plan must not be written.**

### 23.42 Superseded continuation protocol (batch 6, retained for provenance)

1. Read this checkpoint, then sections 23.5-23.9, 23.13-23.15, 23.17-23.24, 23.26-23.34
   and 23.36-23.41 — they carry the newest and highest-risk conclusions. Sections 23.17
   and 23.31 revise earlier statements; read them before trusting the 23.14 writer table
   or the QF-003 entry in 23.10.
2. Do **not** re-read the classes listed in 23.3, 23.13, 23.18, 23.26 and 23.38, do not
   re-measure the baseline, do not reopen `STATUS_NORMAL = "VALID"` (23.13), and do not
   re-derive the test coverage of the nine conflicts (23.37).
3. Continue with the earliest architecturally significant incomplete area, in this order:
   a. **Chart authority convergence** (PA-002, unresolved questions 7-8) — `ChartService`
      (untracked), `platform.statistical_chart`, `platform.visualization_definition`,
      core `ChartDefinition`, migration 110's conversion comment, and `ChartGrammarTest`
      / `ChartHttpTest`. This is the largest remaining unexamined parallel architecture
      and it blocks gate questions 7-8.
   b. Statistical authorization boundary (CF-008): `StatisticalContractController`,
      `StatisticalChartController`, `StatisticalContractAccessDecision` and the untracked
      tests, read as final dirty sources.
   c. Package/contract taxonomy (PA-001, PA-003, unresolved question 6) — the boundary
      among Access semantic declaration, artifact manifest, authoring layout and the
      untracked composer `PackagePlan`.
   d. Migration-chain rebuild audit (CF-012, CF-020, CF-024).
   e. Remaining ~65 unclassified test files, and the remaining consumer gaps
      (`frontend/web` `[LEGACY]`, `backend/mobile`, external callers).

RECOVERY STATUS: INCOMPLETE  
NEXT STARTING POINT: chart authority convergence — read `ChartService` (untracked) and
`StatisticalChartController` (untracked) to EOF under
`platform/apps/geostat/backend/api/src/main/java/org/base/api/`, then compare the three
chart models (core `ChartDefinition`, `platform.visualization_definition`,
`platform.statistical_chart`) and locate or disprove the legacy chart converter that
migration 110's comment implies. Determine which model is canonical for generic pages,
which is an intentional statistical specialization, and whether any chart surface
resolves snapshots differently from the two models recorded in CF-022 — a third
resolution path would widen RC-001.

### 23.35 Superseded continuation protocol (batch 5, retained for provenance)

1. Read this checkpoint, then sections 23.5-23.9, 23.13-23.15, 23.17-23.24 and
   23.26-23.34 — they carry the newest and highest-risk conclusions. Sections 23.17 and
   23.31 revise earlier statements; read them before trusting the 23.14 writer table or
   the QF-003 entry in 23.10.
2. Do **not** re-read the classes listed in 23.3, 23.13, 23.18 and 23.26, do not
   re-measure the baseline, and do not reopen `STATUS_NORMAL = "VALID"` (verified
   consistent, 23.13).
3. Continue with the earliest architecturally significant incomplete area, in this order:
   a. **Test corpus classification** — the 122 test files under `src/test/java`. Map each
      to the behaviors in 23.23 and 23.32: which tests encode REQUIRED behavior, which
      encode behavior already classified INCORRECT (CF-021, CF-023, CF-027, CF-028) and
      would therefore *resist* correction, and which are OBSOLETE. This is now the single
      binding constraint on gate question 12 and on any behavior protection strategy, and
      it is the highest-value remaining batch.
   b. Statistical authorization boundary (CF-008): `StatisticalContractController`,
      `StatisticalChartController`, `StatisticalContractAccessDecision` and the three
      untracked tests, read as final dirty sources.
   c. Chart authority convergence (PA-002, unresolved questions 7-8): `ChartService`,
      `platform.statistical_chart`, `platform.visualization_definition`, core
      `ChartDefinition`, and migration 110's conversion comment.
   d. Migration-chain rebuild audit (CF-012, CF-020, CF-024) including every excluded and
      unregistered file.
   e. Remaining consumer gaps: `frontend/web` `[LEGACY]`, `backend/mobile`, and any
      evidence of out-of-repository callers.
4. Keep writing findings into this file after each batch. Do not begin the master plan
   until the completeness gate in 23.34 is genuinely satisfied; the test corpus is now
   the binding constraint, and RC-001 must be settled before any chart, export or
   attribute convergence work.

*(Batch 5 completed step 3a. Its result is section 23.36-23.41: no test asserts
product-wide publication, `SEMANTIC_REVIEW` visibility, truncated aggregation or the five
inert page parameters — those behaviors are uncovered rather than encoded. Batch 6
completed steps 3b and 3c: see sections 23.43-23.51.)*

## 23A. Batch 7 — the package composer

### 23.52 Artifacts inspected

`service/platform/packaging/PackageContractSource.java` (21), `JdbcPackageContractSource.java`
(89), `PackagePlan.java` (109), `AuthoringPolicy.java` (51), and
`test/.../packaging/PackageContractSourceTest.java` — all to EOF. Plus a repository-wide
reference search for the package and its types.

### 23.53 What the composer actually is — FACT, then classification

FACT. `PackageContractSource` is a **port** whose Javadoc states the authority direction
explicitly: "the approved site contract revision as a package plan. **The Control Plane is
the only authority; a generated file, a script or an adapter never is.** Implementations
fail closed."

FACT. `JdbcPackageContractSource` is a **reader**. Its Javadoc: "Reads the plan from the
Control Plane's site contract tables — **the authority that already exists**
(`site_contract_revision` / `_dataset` / `_field` / `_relation` / `_classifier`). No
statement takes text from a caller: the contract code and revision are bound parameters,
every identifier is a literal of this class." It contains **no INSERT, UPDATE or MERGE**.

FACT. It fails closed on six typed conditions: `NOT_FOUND`, `NOT_APPROVED` (only
`status='APPROVED'` yields a plan), `UNKNOWN_FAMILY` (closed set
`ENTITY, REFERENCE, RELATION, RAW, STATISTICAL, GEO`), `UNDECLARED_KEY` ("a row without
identity cannot be traced, updated or related"), `DANGLING_RELATION` (both endpoints must
be declared fields), `EMPTY_CONTRACT`.

FACT. `PackagePlan` is an immutable record carrying `Identity(productCode, contractCode,
revision, contractChecksum)` and a content digest under the domain
`geostat.package-plan.v1`, computed from a canonical form.

FACT. `AuthoringPolicy` is **pure functions over contract metadata**, with the invariant
stated in its own Javadoc: "no table name, no site, no provider appears here, so a second
site gets the same behaviour with no code change." It derives the authoring class of every
field (`FILL`, `PICK`, `AUTO`, `SYSTEM`, `LINEAGE`) and the package group of every table
(`FILL_IN_DATA`, `IDENTITY_AND_SCHEMA`, `CLASSIFIERS`, `SOURCE_TRAIL`) from family,
semantic role and relation shape alone. A key pointing into the `RAW` family is classified
`LINEAGE` — the system writes it, nobody picks it.

**Classification against the options posed:**

- **B — canonical (partially built) authoring/composition mechanism:** YES, by design. It
  is the correct, platform-general projection of an approved site contract into an
  authoring package, and it is the best-engineered code encountered in this recovery:
  ports-and-adapters applied properly, no caller text in SQL, typed fail-closed refusals,
  a closed family set, content-addressed determinism, and site-agnostic pure policy.
- **F — not wired:** YES, in runtime terms (23.54).
- **A — intended governed producer of site-contract declarations:** **NO.** This is the
  decisive negative and it is stated by the code itself.
- **E — competing representation/authority:** **NO** (23.56).
- **C, D, G:** not supported by evidence.

**The honest classification is a compound one: a correct, complete, unwired *derivation*
port awaiting a producer that does not exist.**

### 23.54 Runtime reachability — zero

FACT. A repository-wide search for `service.platform.packaging`, `PackagePlan`,
`PackageContractSource` and `AuthoringPolicy` returns **five files: the four classes
themselves and their single test.** No controller, no service, no `@Configuration` and no
Spring stereotype constructs any of them (`JdbcPackageContractSource` is a plain `final
class` taking a `JdbcTemplate`, the same shape as `ChartService` — but unlike `ChartService`
nothing instantiates it).

- **Runtime reachability: none.**
- **Persistence effects in production: none.** It writes nothing, and it is never invoked.
- **Consumers: none.**
- **Lifecycle ownership: none** — it owns no state.

### 23.55 CF-024 CONCLUSION — confirmed, split, and strengthened

The batch-7 question was whether this subsystem resolves CF-024. **It does not. It depends
on CF-024 and proves it.**

`JdbcPackageContractSource.approved()` refuses anything whose
`site_contract_revision.status` is not `APPROVED`. Batch 3 established by repository-wide
search that **no production Java path writes `site_contract_revision`,
`site_contract_dataset` or `contract_page_binding`**, and that the only writers are the
KIDS-named seed migrations (022, 023, 054, 055, 057) plus one test fixture. Batch 7 adds
that `PackageContractSourceTest` performs **14 fixture INSERTs** to build the very rows
production cannot create.

**Falsification attempted and failed:** the hypothesis that the semantic Access package is
the intended producer — that `PlatformAccessIngestionService` converts `__gs_*` metadata
into site-contract rows — is refuted by batch 3's evidence that this class only *reads*
`site_contract_dataset` (a `SELECT` at line 236). No class anywhere constructs those rows.

**CF-024 is therefore SPLIT for precision, with traceability:**

- **CF-024a (the real gap, HIGH risk, HIGH confidence):** the M1 site-contract declaration
  layer has **no governed producer**. Declarations can only enter the system as
  hand-written, site-named SQL inside the generic migration chain.
- **CF-024b (NEW, derived):** the **entire consumer side of contract-only onboarding is
  designed, built and tested** — approval (`SiteContractRevisionApprovalService`), query
  execution (`ContractPhysicalQueryService`), serving policy, introspection, compatibility
  analysis, and now the package composer. It is blocked solely by CF-024a.

**The sharpest evidence-backed statement of the impact:** the composer's own test is named
`anApprovedRevisionOfAnySiteBecomesOnePlan`, and it passes. The platform can therefore
**prove site-agnosticism in a test fixture and cannot achieve it in production**, because
no production path can create a site contract for a second site. This is the precise,
structural reason the platform audit's P1.1 "second independent data-product" gate has
remained open, and it reframes that gate from "work not yet done" to "one missing edge".

**Migrations remain the real authority** for the declaration layer despite this
subsystem — unchanged from batch 3.

### 23.56 PA-001 impact — the composer is NOT a competing authority

The task asked whether packaging introduces another representation of the same concepts.
It introduces another **representation**, but **not another authority**, and the
distinction is load-bearing for PA-001.

| Axis | `PackagePlan` |
|---|---|
| Semantic responsibility | an approved site contract expressed in the shape an authoring package needs |
| Source of truth | `platform.site_contract_*` — stated explicitly, not inferred |
| Producer | `JdbcPackageContractSource` (derivation only) |
| Consumer | none yet; intended generator/loader |
| Persistence | **none** — the plan is computed, never stored |
| Lifecycle | none of its own; it borrows the revision's |
| Conversion path | one-way, contract -> plan |
| Runtime authority | **none**, by explicit contract |

**Classification: TRANSPORT / DERIVED REPRESENTATION — not accidental duplication, not a
competing authority.** A stateless, content-digested, one-way projection whose port
contract forbids it from becoming authority is precisely what the doctrine permits:
projections and adapters are not sources of truth.

**Revision to PA-001 (traceable):** PA-001 previously listed "New composer `PackagePlan`
representation" among five package representations with "exact boundaries unresolved", and
section 5.3 item 6 recorded the composer as MEDIUM confidence with "coexistence with the
statistical plan and existing package models unresolved". **Both are now resolved**:
`PackagePlan` is a derived projection of the site contract and does not overlap the
authority of the Access transport envelopes, the artifact manifest or the statistical
semantic plan. The genuine overlap that remains in PA-001 is between the **statistical
semantic plan** and the **site contract** (CF-006), which is unchanged.

### 23.57 Recovered authoring model — one missing edge

```text
   [ NO PRODUCER ]                                    <-- CF-024a: the only missing edge
        |
        v
 platform.site_contract_revision / _dataset / _field / _relation / _classifier   (M1)
   written only by KIDS seed migrations 022, 023, 054, 055, 057
        |
        v
 SiteContractRevisionApprovalService            authority changes hands here
   UPDLOCK/HOLDLOCK, all ContractApprovalCheck beans, CAS on REVIEW_REQUIRED,
   checksum-bound evidence, outbox event
        |
        +--> JdbcPackageContractSource.approved()  -> PackagePlan (+ digest)   [UNWIRED]
        |          fail-closed: NOT_APPROVED / UNKNOWN_FAMILY / UNDECLARED_KEY
        |          / DANGLING_RELATION / EMPTY_CONTRACT
        |              |
        |              v
        |       AuthoringPolicy -> authoring class per field, group per table  [UNWIRED]
        |              |
        |              v
        |       [ Access generator for PackagePlan ]                           [ABSENT]
        |          (AccessAuthoringAdapter exists, but consumes the *statistical*
        |           semantic plan, not PackagePlan)
        |
        +--> ContractPhysicalQueryService / CanonicalPageDataService  -> serving
        |
        +--> package run pipeline -> ingest -> snapshot -> gates -> publication
```

**Where authority changes hands:** exactly once, at
`SiteContractRevisionApprovalService.approve()`. Everything to its right is derivation or
execution. Nothing to its right claims authority, and the composer's port contract says so
in words.

**Missing transitions:** one — *authoring input -> canonical contract*. **Duplicate
authorities:** none introduced by packaging. **Hidden defaults:** none found in packaging.
**Migration-only writers:** the M1 layer, unchanged. **Bypasses:** none in packaging.

### 23.58 DR-003 — decision on the composer

**DECISION (HIGH confidence): KEEP and EVOLVE the composer; it is canonical in design and
must not be deleted as dead code.**

*Rationale chain.* REQUIREMENTS (contract-only onboarding of a second site, SCH-003) ->
DOMAIN SEMANTICS (an authoring package is a *view* of an approved declaration, never a
second declaration) -> INVARIANTS (INV-002 one semantic authority; INV-011 provider
artifacts are adapters) -> DOCTRINE (metadata-driven behavior, no site branching) ->
CONTRACTS (the port forbids the plan from being authority) -> DECISION.

*Falsification attempts.*
1. *Is it dead code that should be deleted?* No. "Unused" is not "unwanted": it is the
   built half of the capability the platform's own audit lists as its top open gap, and it
   is tested. Deleting it would discard the correct design and leave only the defect.
2. *Am I preserving it because it looks sophisticated?* The standard applied was
   behavioural: it is the only inspected component that satisfies port-and-adapter
   separation, no caller text in SQL, typed fail-closed refusal, closed family set,
   order-independent content digest and proven site-agnostic policy simultaneously.
3. *Does it create schema coupling?* It reads five site-contract tables by literal
   identifiers. That is coupling to the M1 schema, which is correct for a projection of
   M1.
4. *Could it become a second source of truth later?* Only if something persists a
   `PackagePlan`. **INV-013 is therefore added below to forbid that.**
5. *Is the four-group `AuthoringPolicy` an Access-specific leak into the platform?* The
   groups are named for the Access package organisation plan, but they are derived from
   family and semantic role with no provider reference. It is a presentation taxonomy
   derived from semantics — acceptable, but it should eventually be declared in the
   contract vocabulary rather than hardcoded in an enum. Recorded as MEDIUM-confidence
   follow-up, not a defect.

**Ordering constraint (derived, FACT):** the composer cannot be wired until CF-024a is
answered, because a generator with no way to obtain a second site's contract adds nothing.
**CF-024a is therefore promoted to the critical path for SCH-003**, ahead of packaging
work.

### 23.59 NEW INV-013 — a derived plan must never be persisted as authority

- **Statement:** a `PackagePlan`, semantic plan, compiled query plan or any other
  derivation of an approved declaration may be cached or digested, but must never become a
  stored artifact that anything reads *instead of* the declaration it derives from.
- **Status:** CONFIRMED as intent; **currently honoured by the composer** (nothing is
  persisted) and **currently violated in spirit by CF-004**, where `ContractWorkflow`
  recompiles an approved statistical document against the mutable registry, making the
  derivation — not the declaration — decide meaning at use time.
- **Evidence:** `PackageContractSource` Javadoc; doctrine's generated-artifacts rule;
  CF-004.
- **Confidence:** HIGH.

This invariant also sharpens unresolved question 2 (what persisted artifact is the
immutable approved semantic plan): the answer must preserve the *declaration* as authority
and treat any compiled plan as a verifiable derivation, never as a substitute.

### 23.60 Test classification additions

`PackageContractSourceTest` is an **ARCHITECTURAL_INVARIANT_TEST** set of six:

| Test | Invariant protected |
|---|---|
| `anApprovedRevisionOfAnySiteBecomesOnePlan` | site-agnosticism (SCH-003) — of the consumer only |
| `whoFillsWhatAndWhereItIsShownComeFromMetadataAlone` | metadata-driven authoring, no site branching |
| `theDigestIsOfTheContentNotOfTheOrderAStoreReturnedIt` | deterministic content addressing, order independence |
| `nothingButAnApprovedRevisionYieldsAPlan` | fail-closed on approval status |
| `aContractThatCannotBeHonouredIsRefusedWhole` | all-or-nothing refusal, no partial plan |
| `anEmptyRevisionIsNotAPackage` | empty-contract refusal |

These must survive unchanged. Note the asymmetry recorded in 23.55: this is the **only**
test in the corpus that asserts site-agnosticism, and it can do so only because its
fixture writes the rows production cannot.

### 23.61 Inventory and gate status after batch 7

- **Behavior classification: ~84% -> ~88%.** Added: contract-to-plan derivation (REQUIRED),
  authoring-class and group derivation (REQUIRED), fail-closed plan refusal (REQUIRED),
  content-digest determinism (REQUIRED), plan persistence (FORBIDDEN, INV-013).
- **Consumer inventory: ~74% (unchanged).** The composer has no consumers; no new
  consumer surface was revealed.
- **Test classification: ~48% -> ~53%.**

Gate question 6 (the boundary among Access semantic declaration, artifact manifest,
authoring layout and composer `PackagePlan`) is **answered** by 23.56. PA-001 is reduced to
its genuine residue, CF-006.

Still open and binding: grammar duality (CF-009); the migration-chain rebuild audit
(CF-012, CF-020); CF-006 statistical-versus-site contract ownership — which is now the
**largest remaining architectural unknown**, because CF-024a's answer depends on deciding
whether the site contract is the single M1 declaration layer or one of two; gate questions
16-21 (sequencing and the protection strategies); ~62 unclassified test files;
`frontend/web`, `backend/mobile`, external callers.

**The gate is not satisfied. The master plan must not be written.**

## 23B. Batch 8 — CF-006 resolved, and the finding that reframes CF-023

### 23.62 Artifacts inspected

`ContractWorkflow.java` (202) to EOF as a final dirty source;
`StatisticalContractConfiguration.java` (144, bean wiring);
`StatisticalBindingService` write census; `StatisticalContractController` surface;
repository-wide writer census for `platform.ingestion_contract_revision`.

### 23.63 CF-006 RESOLVED — layered representation, not competing authority

FACT. `StatisticalBindingService` **writes** the shared canonical Control Plane registry:
`platform.dataset`, `platform.dataset_version`, `platform.source_system`,
`platform.ingestion_contract`, `platform.dimension`, `platform.metric`.

FACT. The site contract **references** that same registry:
`site_contract_dataset.dataset_version_id -> platform.dataset_version -> platform.dataset`.

FACT. The two declarations describe different things:

| Axis | Site contract `site_contract_*` | Statistical contract `statistical_contract_draft` |
|---|---|---|
| Semantic responsibility | how a site's datasets are **presented and served** | what a statistical dataset **means** |
| Domain meaning | Access table names, fields, relations, node tree, page bindings, projections, classifier policy, serving policy | DSD structure, dimensions, measures, units, codelists, grain, attachment |
| Source of truth | `platform.site_contract_*` | `platform.statistical_contract_draft` (+ `_event`) |
| Identity | `contract_code` + `revision` | `draftId`; business key `(productCode, datasetKey)` where `datasetKey = namespace:datasetCode` |
| Lifecycle | `REVIEW_REQUIRED -> APPROVED -> SUPERSEDED` | `DRAFT -> REVIEW_REQUIRED -> APPROVED -> SUPERSEDED` (+ unreachable `ROLLED_BACK`) |
| Versioning | integer `revision` per contract code | monotonic `version` per draft, CAS on every mutation |
| Producer | **none** (CF-024a) | `ContractWorkflow.create/save/submit/approve` over HTTP |
| Approval | `SiteContractRevisionApprovalService` — lock, pluggable checks, checksum evidence, outbox | `ContractWorkflow.approve` — four-eyes, digest-of-what-was-reviewed, compatibility verdict |
| Compilation | none (declarative rows consumed directly) | `StatisticalContractCompiler` -> `SemanticPlan` |
| Runtime authority | serving dispatch, query execution, serving policy | generation, ingestion, charts, export |
| Site scope | site/contract-code scoped | product + dataset scoped; **no site concept at all** |
| Schema scope | physical/Access presentation | semantic structure |
| Extension | new datasets/fields/relations/pages | new components/references within the closed grammar |

**Falsification attempts.** (1) *Is the statistical contract simply a better site contract?*
No — it has no pages, nodes, page bindings, projections or Access layout. (2) *Is the site
contract a superset?* No — it has no DSD, measures, units, grain or codelist policy.
Neither is a superset of the other; they are not two descriptions of one concept.
(3) *Is the shared registry a third authority?* No — it is the canonical model both
declarations project into, and it is where they legitimately meet.

**CF-006 CLASSIFICATION: LAYERED REPRESENTATION over a shared canonical registry — NOT
competing authority, NOT accidental duplication.** The previous MEDIUM-HIGH suspicion of
"parallel M1 authorities" is **narrowed**: they are two *different* M1 concepts, both
legitimate.

**The real defect is not duplication; it is the missing link.** FACT (batch 3): the
statistical package tree contains **zero** references to `site_contract*`. Consequences:
(a) approving a new statistical revision changes dataset semantics with no site-contract
revision, approval or compatibility check; (b) nothing lets a site contract state which
statistical contract revision its STATISTICAL dataset conforms to; (c) the two lifecycles
can diverge silently.

**DECISION DR-004 (HIGH confidence on separation, MEDIUM-HIGH on the linkage mechanism):
keep the two declaration concepts separate and make their relationship explicit and
governed. Do not merge them.**

Merging would put two bounded contexts with different rates of change under one lifecycle,
forcing every semantic revision to become a site revision — a Single-Responsibility and
coupling regression. Instead: a site contract dataset of family `STATISTICAL` must
reference the approved statistical contract by identity **and revision digest**, exactly as
it already references `dataset_version_id`; and the site contract's existing pluggable
`List<ContractApprovalCheck>` mechanism is the natural place to enforce that the referenced
statistical revision is approved and compatible. Both remain projections into the shared
registry.

### 23.64 CF-004 REVISED — narrowed from semantic integrity to availability

- **PREVIOUS FINDING (section 9, CF-004):** "`ContractWorkflow` recompiles the approved
  document against the current registry ... **M1 is not an immutable executable authority;
  availability and meaning can change without a contract revision.**"
- **NEW EVIDENCE (FACT), `ContractWorkflow:157-163`:** `approvedPlan` recompiles **and then
  compares**: `if (!plan.revisionDigest().equals(current.revisionDigest())) throw
  fail(Failure.DIGEST_MISMATCH, "the approved contract no longer compiles to the approved
  revision")`. The Javadoc states the intent: "Generation and ingestion read the contract
  through this method only, so a registry that drifted since approval is **detected instead
  of silently reinterpreted**."
- **CONTRADICTION:** the "meaning can change" half is **falsified**. Recompilation cannot
  silently reinterpret an approved contract; any drift that changes the compiled result is
  detected and refused fail-closed.
- **REVISED CONCLUSION:** CF-004 is a genuine **availability/operability** defect — an
  approved contract can stop working because something else changed — but **not** a
  semantic-integrity defect. Risk downgraded from HIGH to MEDIUM-HIGH. The declaration
  (stored document + stored `revisionDigest`) remains authority throughout.
- **RESIDUAL RISK (open question, not asserted):** drift that does **not** alter
  `revisionDigest` — for example a reference whose descriptive metadata changed while its
  identity and version did not — would pass the check. Whether that is possible depends on
  the exact input set of `revisionDigest`, which this batch did not read. Recorded as
  unresolved question 23.

**INV-013 analysis: `approvedPlan` HONOURS INV-013.** The derived plan never becomes
authority; it is re-verified against the declaration's digest on every use. The batch-7
suspicion that `ContractWorkflow` violates INV-013 "in spirit" is **withdrawn**. INV-013's
one confirmed live violation risk remains the possibility of *persisting* a derived plan,
which nothing currently does.

### 23.65 CF-008 REFINED — the correction is the authority mapping, not an annotation

FACT, `ContractWorkflow:150-151` and `load(...)`: `get()` passes `authority = null`, and
`load` then requires only that the actor hold **at least one** of `AUTHOR`, `APPROVE`,
`IMPORT` for that product, refusing otherwise with `NOT_FOUND` — deliberately, with the
comment "no existence oracle across products". The Javadoc states the intent: "Reading is
open to every role of the product: an approver must see what they approve."

So the inherited CF-008 evidence is **confirmed and now explained**: the restriction is
deliberate and documented, not an oversight, and it is enforced at the use-case boundary
through the `AccessDecision` port ("Port to the platform's authorization (tenant scope and
function-level authority); deny by default").

**DR-002 stands, but its remediation is refined (traceable):** the correction is **not**
to add `@PreAuthorize` as the fix. It is to extend the statistical `Authority` set — or the
`StatisticalContractAccessDecision` mapping — so that the platform's ordinary read
authority admits reading approved contracts and charts, leaving `AUTHOR`/`APPROVE`/`IMPORT`
for mutation and decision. The HTTP annotation remains defence in depth. The CF-031
precondition is unchanged and unweakened.

### 23.66 NEW CF-032 — governed publication is structurally closed to statistical datasets

This is the batch's most consequential finding.

- **Evidence A (FACT):** `StatisticalBindingService:51-53` creates
  `platform.ingestion_contract` with
  `contract_code = productCode + "." + plan.datasetCode()` and column list
  `(source_system_id, dataset_id, format_profile, ingestion_method, auto_publish, status,
  contract_code)` — **no `contract_revision`**, and it creates no
  `ingestion_contract_revision` row.
- **Evidence B (FACT):** `PlatformPublicationService.assertApprovedContract` requires a
  three-way join to succeed: an `ACTIVE` `ingestion_contract`, an **`APPROVED`
  `site_contract_revision` whose `contract_code` equals the ingestion contract's
  `contract_code` and whose `revision` equals `c.contract_revision`**, and an `APPROVED`
  `ingestion_contract_revision` at the same revision.
- **Evidence C (FACT):** nothing in production creates `site_contract_revision` (CF-024a),
  and a repository-wide search shows `platform.ingestion_contract_revision` is written
  **only** by KIDS seed migrations 022, 023 and 071 — **no Java writer exists**.
- **CONCLUSION:** a dataset created by the statistical path can **never** satisfy
  `assertApprovedContract`. `PlatformPublicationService.publish()` is structurally closed
  to it.

**The consequence reframes CF-023.** The only route by which a statistically-loaded
snapshot can reach `PUBLISHED` is the product-wide sweep in
`DataPlanePublicationWriter:31` — publishing some *other* dataset of the same product
(one of KIDS's migration-seeded, properly contracted datasets) flips **every**
`REVIEW_REQUIRED` snapshot of that product to `PUBLISHED`, the statistical one included.

**CF-023 is therefore not merely a defect that *can* leak ungated data: it is the de facto
and only publication mechanism of the entire statistical subsystem.** This also explains
the AIR-2026-054 incident shape exactly — KIDS indicator loads produced statistical
snapshots that could only go live via the sweep, which simultaneously published snapshots
53 and 57-61.

- **Risk:** CRITICAL. **Confidence:** HIGH (all three evidence strands read directly).
- **HARD ORDERING CONSTRAINT (new, binding):** correcting CF-023 in isolation would
  **remove the only working publication path for statistical data** and break the
  subsystem. The CF-023 correction must be delivered **together with** a governed
  publication path for statistically-loaded datasets — which in turn requires CF-006's
  linkage (DR-004) and CF-024a's producer. This is the first discovered dependency that
  forces a *bundle* rather than a sequence.

### 23.67 CF-014 confirmed, and a second dead state

FACT. `ContractWorkflow` produces `DRAFT` (create, save, returnToDraft),
`REVIEW_REQUIRED` (submit), `APPROVED` and `SUPERSEDED` (approve). **No method ever
produces `ROLLED_BACK`**, although the enum declares it. CF-014 (an approved contract
cannot be withdrawn) is confirmed at source level, and `ROLLED_BACK` is a **declared state
with no producer** — structurally the same anti-pattern as the `'APPROVED'`
`dataset_snapshot` status in CF-025. Both belong to one class of defect: **state vocabulary
declared beyond what any writer produces**, which misleads every reader of the model.

### 23.68 Authority transitions — complete chain

```text
AUTHORITATIVE DECLARATION
  site contract rows            <- produced ONLY by KIDS seed migrations        (CF-024a)
  statistical draft document    <- produced by ContractWorkflow over HTTP       (governed)
      |
APPROVED REVISION
  SiteContractRevisionApprovalService.approve()   checksum evidence + outbox
  ContractWorkflow.approve()                      four-eyes + reviewed-digest match
      |
DERIVED PLAN            PackagePlan (digest, never persisted)          INV-013 honoured
COMPILED PLAN           SemanticPlan (recompiled, digest-verified)     INV-013 honoured
      |
EXECUTION STATE         ingest.batch / dataset_load / staged_row
PUBLICATION STATE       publication.dataset_snapshot.status  <-- CF-025 no owner, 5 writers
                        publication.snapshot + snapshot_member
SERVING STATE           status-based readers vs membership-based readers   <-- CF-022
```

**Silent authority transfers found: none from declaration to derived plan.** Both
derivation points (`PackagePlan`, `SemanticPlan`) are digest-verified and non-persisted.
**The authority failure in this platform is not at the derivation boundary — it is at the
publication boundary**, where `dataset_snapshot.status` acts as an unowned second authority
(RC-001, CF-025) and where the governed route is closed to one whole subsystem (CF-032).

### 23.69 CF-024a implications — the canonical target is now determinable

The dependency recorded for this batch holds and is now instantiated:

```text
CF-006 (resolved: two distinct M1 concepts, shared registry, missing link)
   -> canonical M1 declaration authority: site contract OWNS serving declaration;
      statistical contract OWNS dataset semantics; both reference the shared registry;
      site contract references the approved statistical revision by digest
   -> CF-024a governed producer: must produce the SITE contract, and should follow the
      shape ContractWorkflow already proves works — CAS versioning, idempotent create,
      submit/approve separation, four-eyes, digest-of-what-was-reviewed — reusing
      SiteContractRevisionApprovalService as the approval half, which already exists
   -> SCH-003 contract-only onboarding
```

**The platform already contains a working, governed, digest-bound declaration producer.**
It produces only the semantic half. The architectural answer to CF-024a is therefore not a
new invention but the **same proven shape applied to the serving declaration** — with the
composer (DR-003) as its downstream consumer. Confidence: MEDIUM-HIGH; the remaining
uncertainty is the authoring input format, which is a product decision, not an engineering
one.

### 23.70 Coverage and gate status after batch 8

- **Behavior classification: ~88% -> ~92%.** Added the full statistical declaration
  lifecycle, CAS/idempotency/four-eyes semantics, digest-verified recompilation (REQUIRED),
  registry binding writes (REQUIRED), `ROLLED_BACK` (OBSOLETE/dead), and the closed
  governed publication path (INCORRECT).
- **Test classification: ~53% (unchanged).**
- **Consumer inventory: ~74% (unchanged).**

Gate question 7-10 now answered for the declaration layer as well. **Still open and
binding:** grammar duality (CF-009); migration-chain rebuild audit (CF-012, CF-020);
unresolved question 23 (the `revisionDigest` input set); gate questions 16-21 — sequencing
and the protection strategies, which are now the largest remaining work and for which
23.39, 23.66 and 23.69 supply most of the raw material; ~62 unclassified test files;
`frontend/web`, `backend/mobile`, external callers.

**The gate is not satisfied. The master plan must not be written.**

## 23C. Batch 9 — grammar, digest, migration chain, and the atomic migration set

### 23.71 CF-009 RESOLVED — one grammar, two hand-written representations, unenforced parity

FACT. `src/main/resources/contracts/statistical-contract-draft.schema.json` (147 lines) is
JSON Schema 2020-12, `$id: urn:geostat:contract:statistical-contract-draft:1.0.0`, closed
via `unevaluatedProperties: false`. Its own `description` states the division of
responsibility: "Closed grammar of the common statistical contract. **Shape only**:
reference resolution, grain, units and authorization are semantic checks of the compiler.
ContractDraftParser enforces the same shape."

FACT. `ContractDraftParser`'s Javadoc states the same relationship from the other side:
"Closed-grammar parser of the authoring document ... The published JSON Schema
(`resources/contracts/statistical-contract-draft.schema.json`) **states the same shape**."

FACT. A repository-wide search across Java, Gradle, YAML, XML, PowerShell, Python and
shell finds **exactly one reference to the schema file: that Javadoc comment.** The schema
is **not loaded at runtime, not validated against, not referenced by any build, test or CI
path**.

FACT. Neither artifact is generated. Both are hand-written. Both are closed.

| Axis | JSON Schema | `ContractDraftParser` |
|---|---|---|
| Source of truth | published shape contract (M2) | executable shape enforcement |
| Semantic responsibility | shape only, by its own statement | shape only; semantics belong to the compiler |
| Supported constructs | 8 root properties, 4 required | `ROOT` set of the same 8 |
| Validation responsibility | none at runtime — unwired | all runtime shape validation |
| Error semantics | JSON Schema violations (unused) | `ContractIssue` with pointer + code, **all** issues in one round-trip |
| Versioning | `$id` carries grammar version 1.0.0 | none; version is implicit in the code |
| Extension model | add a `$defs` member | add to `ROOT` and a parse branch |
| Consumers | none in this repository | the whole statistical subsystem |
| Test coverage | **none** | via compiler/workflow tests |
| Drift risk | **HIGH and unmitigated** | — |

**CLASSIFICATION: one grammar, two representations, correctly layered (shape versus
semantics), with NO mechanical enforcement of equivalence.** It is **not** duplicated
grammar *authority* — the responsibility split is explicit and consistent on both sides —
and it is **not** an incomplete migration. It is a **parity gap**: two hand-maintained
closed grammars that currently agree and can silently diverge, which the implementation
checklist already records as open.

**"They currently match" is explicitly rejected as sufficient**, per the batch
instruction: nothing prevents a future change to one from leaving the other behind, and
the schema carries a version number that no process increments.

**DR-005 (HIGH confidence on direction; parity completeness inherently bounded):** the
**JSON Schema is the canonical published shape authority (an M2 artifact)**;
`ContractDraftParser` is its executable implementation; **equivalence is enforced by a
bidirectional conformance corpus, not by code generation**.

*Rationale chain.* REQUIREMENTS (external authors and tooling need a machine-readable,
publishable, versioned statement of the authoring shape) -> DOCTRINE (contract-first:
"კონტრაქტი/metadata არის source of truth; კოდი მას მიჰყვება") -> STANDARDS
(`STANDARDS.md` already adopts **JSON Schema 2020-12 Core** for "meta-schema, dialect,
vocabulary and reference separation; SCH-001", with the stated limit that "JSON Schema
does not validate business authorization **or generate an engine**") -> DECISION.

*Falsification attempts.*
1. *Generate the parser from the schema?* Rejected — the standards profile explicitly
   states JSON Schema does not generate an engine, and the parser does more than shape
   validation: it produces pointered `ContractIssue` diagnostics collecting every error in
   one round-trip for the authoring UX (register Q31/Q32). Generation would lose a real
   capability.
2. *Generate the schema from the parser?* Rejected — it inverts contract-first, makes the
   *published* artifact a derivative of an implementation detail, and ties external
   consumers' contract to Java refactoring.
3. *Delete the schema?* Rejected — it is the only publishable machine-readable shape
   artifact, and its dialect is already adopted in the standards profile. "Unused" is not
   "unwanted" (the same reasoning as DR-003).
4. *Is a conformance corpus actually sufficient?* **No, and this is stated honestly:** a
   corpus proves agreement only on its members. It must be bound into the governance/CI
   gate with a rule that every new grammar construct adds fixtures, and property-based
   generation from the schema is the later strengthening step. The *direction* is HIGH
   confidence; *complete* parity is not achievable by testing alone.
5. *Does this create a second authority?* No — it removes ambiguity by naming the schema
   authoritative for shape and the compiler authoritative for semantics, which is exactly
   what both artifacts already declare.

### 23.72 CF-004 residual risk — CLOSED, with one declared inference gap

FACT, `StatisticalContractCompiler:86-96`. The revision digest is computed over:

```text
revision = semanticForm(undigested) + captions + physicalForm(physical)
undigested = profileRef, datasetNamespace, datasetCode, structureRef,
             components (resolved and normalised), policies, errorMode,
             dependencies (the resolved reference closure)
```

**Dependency classification:**

| Semantic dependency | Binding |
|---|---|
| draft document | IMMUTABLE BY CONTRACT — stored, CAS-versioned |
| resolved reference identities and versions (`dependencies`) | **DIGEST-BOUND** |
| normalised component list (concepts, representations, measures, units) | **DIGEST-BOUND** |
| policy refs | **DIGEST-BOUND** (sorted, deduplicated) |
| captions | **DIGEST-BOUND** (sorted) |
| physical plan | **DIGEST-BOUND** via `physicalForm` |
| provider capabilities | UNBOUND input, but **bound through its output**: any capability change that alters the physical plan changes the digest and is detected; one that does not alter it is semantically irrelevant |
| compiler settings (numeric envelope, supported profiles, caption languages) | UNBOUND deployment configuration, but **fail-closed in effect**: a tightened envelope rejects (`NOT_COMPILABLE`), a loosened one cannot change an already-valid plan |
| reference lifecycle status (ACTIVE/SUPERSEDED) | UNBOUND, but affects **resolvability**, not meaning — resolution failure is fail-closed |

**CONCLUSION: the digest binds the compiler's resolved *outputs* rather than its mutable
*inputs*, which is the stronger design.** Any drift that changes the output is detected;
drift that does not change the output cannot, by construction, change the plan's meaning.
**CF-004's residual semantic-reinterpretation risk (unresolved question 23) is therefore
CLOSED.**

**CF-004's final scope is availability only:** an approved contract can stop working
because a pinned reference was superseded elsewhere, producing `NOT_COMPILABLE` or
`DIGEST_MISMATCH` with no contract change. This is real, it is fail-closed, and it is
precisely the AIR-2026-055 situation (the incorrect 43-measure contract whose pinned
references were all superseded). Risk: MEDIUM-HIGH. Confidence: HIGH.

**DECLARED INFERENCE GAP:** `semanticForm()` and `physicalForm()` themselves were not read
line by line; their contents were inferred from the `SemanticPlan` constructor arguments
at the call site. The classification above is INFERENCE, not FACT, for the precise field
list. A future batch should read both methods to confirm no semantic field is omitted.
This is recorded rather than glossed.

### 23.73 Migration-chain audit — clean rebuild and production do NOT converge

FACT. **113 `.sql` files present**; `PlatformSchemaMigrationRunner` contains **108
`executeAndRecord(...)` calls**; **5 checksum-rewrite exception filenames** appear in the
runner. **Six present files are not registered:**

| Unregistered file | Reading |
|---|---|
| `000_create_platform_databases.sql` | BOOTSTRAP — legitimately outside the chain; it creates the databases the chain runs inside |
| `037_prefixed_access_source_locators.sql` | generic-sounding; classification UNRESOLVED |
| `039_kids_r7_source_locator_alignment.sql` | KIDS R7-era |
| `045_kids_r7_classifier_assignment_projection.sql` | KIDS R7-era |
| `047_kids_r7_classifier_owner_approval.sql` | KIDS R7-era |
| `056_activate_kids_r8_supersede_legacy.sql` | **activates KIDS R8 and supersedes legacy** |

**The architecturally decisive one is `056`.** A migration whose name states that it
*activates R8 and supersedes legacy* is **not in the registered chain**. Therefore a
database built from a clean checkout runs every schema migration but **never performs the
R8 activation**. This is a direct, source-level explanation of AIR-2026-027's report that
the registered chain could not rebuild KIDS R8, and it answers the batch question
definitively:

**A clean rebuild and the evolved production database do NOT converge to an equivalent
architectural state.** They converge on *schema* and diverge on *activation and
declaration state*.

FACT. **40 of 113 migrations (35%) are KIDS-named.** Combined with CF-024a (the site
contract has no producer other than migrations) and CF-011 (KIDS content inside generic
migrations), this yields the deepest root cause of the rebuild problem:

**RC-002 (NEW ROOT CAUSE) — the migration chain carries three incompatible
responsibilities at once:** (a) canonical schema evolution, (b) the *only* producer of
site declarations and bootstrap/activation data, and (c) a partially-registered historical
record with checksum-rewrite exceptions. Because (b) rides inside (a), a site's
declaration state cannot be reproduced, versioned, approved or migrated independently of
schema history — and because (c) is partial, not even the historical record replays
completely.

**Classification of the chain, as required:**

| Class | Content |
|---|---|
| CURRENT SCHEMA AUTHORITY | the registered DDL migrations |
| BOOTSTRAP DATA | `000`; database creation |
| COMPATIBILITY / SEED DATA | the KIDS declaration seeds (022, 023, 054, 055, 057, 063, 068, 071, 088, 096) — these are **declaration content masquerading as schema history** |
| OBSOLETE ASSUMPTION | the R7-era unregistered set (039, 045, 047) |
| MISLEADING DOCUMENTATION | migration 110's converter claim (CF-029) |
| INCOMPLETE MIGRATION EVIDENCE | `056` unregistered; 5 checksum rewrites (CF-012); the runner as a second manifest (CF-020) |

Minor discrepancy recorded honestly: 108 `executeAndRecord` calls against 107 registered
candidate files (113 minus 6) implies one call is duplicated or references a
non-`db/platform` resource. Not investigated; flagged for the migration task.

### 23.74 CF-032 / CF-023 — falsification attempted; the ATOMIC ARCHITECTURAL MIGRATION SET stands

The batch instruction required an attempt to find a smaller safe sequence than the
five-part bundle. Three were tried:

1. **"Fix CF-023 first, accept a temporary statistical publication outage."** Rejected.
   The only current publication path for statistical data is the sweep; removing it strands
   every statistically-loaded dataset in `REVIEW_REQUIRED` with no governed route. That
   violates the Monotonic Architectural Quality principle (a phase must not leave the
   system less behaviorally safe) and would require an unbounded outage whose exit depends
   on three other workstreams.
2. **"Loosen `assertApprovedContract` so statistical datasets can publish, then fix
   CF-023."** Rejected, and it is the more dangerous option: weakening the only contract
   check guarding publication in order to fix a publication defect inverts the invariant
   and creates exactly the "compatibility mechanism becoming permanent" anti-pattern.
3. **"Give `StatisticalBindingService` an `ingestion_contract_revision` and a synthetic
   site-contract revision."** Rejected as stated, but it identifies the genuine seam: it
   is a disguised, ungoverned implementation of CF-024a's producer. Done properly — through
   a governed producer — it *is* the answer, which is why it belongs inside the set rather
   than before it.

**CONCLUSION: the bundle is atomic. Recorded as AMS-001.**

```text
AMS-001 — ATOMIC ARCHITECTURAL MIGRATION SET: governed statistical publication
  members (no member is independently safe to ship):
    1. DR-004 cross-context link: site contract dataset references the approved
       statistical revision by identity + digest, enforced by a ContractApprovalCheck
    2. CF-024a governed producer for the site contract declaration
       (shape proven by ContractWorkflow; approval half already exists)
    3. CF-032 ingestion-contract revision/governance for statistically-bound datasets
    4. a governed publication path reachable by statistical datasets
    5. CF-023 elimination: bounded membership + gate-scope alignment + rollback repair
  preconditions: RC-001 answered (membership is the liveness authority)
  protection required first: the publication writer/service/store are entirely
    untested (23.39) — protection must precede every member
  exit condition: a statistically-loaded dataset can be published through
    PlatformPublicationService with its own gate evidence, and no product-wide
    status sweep exists
```

### 23.75 The canonical architectural line — remaining breaks

```text
AUTHORING            statistical: ContractWorkflow (governed)   | site: NONE      <-- BREAK 1 (CF-024a)
CANONICAL DECLARATION statistical_contract_draft | site_contract_* (migration-seeded)
APPROVAL/REVISION    ContractWorkflow.approve   | SiteContractRevisionApprovalService
                     no link between them                                          <-- BREAK 2 (CF-006/DR-004)
DERIVED PLAN         SemanticPlan (digest-verified) | PackagePlan (unwired)         <-- BREAK 3 (DR-003, gated by BREAK 1)
INGESTION            package-run pipeline | StatisticalLoadService (bypasses it)    <-- BREAK 4 (23.19/23.33)
SNAPSHOT             publication.dataset_snapshot.status — five writers, no owner   <-- BREAK 5 (CF-025)
RELEASE GATES        ReleaseGateService — correct, and bypassed by the statistical entry state
PUBLICATION          assertApprovedContract closed to statistical datasets          <-- BREAK 6 (CF-032)
                     DataPlanePublicationWriter publishes product-wide              <-- BREAK 7 (CF-023)
MEMBERSHIP           snapshot_member — correct, and not consulted by three readers  <-- BREAK 8 (CF-022)
SERVING              canonicalSource admits SEMANTIC_REVIEW                         <-- BREAK 9 (CF-021)
QUERY/AGGREGATION    aggregation truncated at 1000 rows                             <-- BREAK 10 (CF-027)
                     five declared page parameters inert                            <-- BREAK 11 (CF-028)
VISUALIZATION        four chart mechanisms, two liveness models                     <-- BREAK 12 (DR-001/CF-030)
CONSUMERS            frontend/kids on the weaker-governed page route
```

**Twelve breaks, reducible to four root causes:** RC-001 (two liveness authorities →
breaks 5, 7, 8, 9, 12), RC-002 (migrations carry declaration duty → breaks 1, 2, 3, 6),
the ingestion bypass (break 4), and the serving-surface defects (breaks 10, 11). No
break is an unexplained competing authority any longer; every one is now classified with
an owner, a cause and a disposition.

### 23.76 Completeness-gate synthesis

| Gate question | Status after batch 9 |
|---|---|
| 1-6 responsibilities, concepts, contracts, sources of truth, invariants, flows | **ANSWERED** |
| 7-11 competing implementations, contradictions, legacy, incomplete migrations, specialization vs duplication | **ANSWERED** — PA-001 reduced to CF-006 and resolved; PA-002 decided (DR-001); PA-003 resolved (DR-004); packaging classified (DR-003); grammar classified (DR-005) |
| 12 documentation/test/contract/code disagreement | **ANSWERED** — tests do not disagree; they are absent where implementation diverges (23.37). Doc-vs-code contradictions catalogued (CF-029, section 12) |
| 13-14 recoverable intent vs undeterminable | **ANSWERED** — intent recovered at every decision point; undeterminable items are product decisions, not engineering ones |
| 15 behavior to protect | **~92%**, with 14 unprotected REQUIRED behaviors enumerated (23.39) |
| 16-18 what becomes canonical, why, in what order | **SUBSTANTIALLY ANSWERED** — DR-001..DR-005, RC-001/RC-002, AMS-001, ordering constraints recorded |
| 19 regression protection | **RAW MATERIAL COMPLETE** (23.37, 23.39), strategy not yet drafted |
| 20 quality protection | **RAW MATERIAL COMPLETE**, strategy not yet drafted |
| 21 drift protection | **PARTIAL** — governance checker exists; fitness functions not yet specified |

**Remaining material unknowns (only these can still alter canonical architecture):**

1. `semanticForm()` / `physicalForm()` exact field lists — the declared inference gap in
   23.72. LOW risk of altering a decision; MEDIUM value.
2. The numeric-envelope authority (CF-003) — unresolved across Q34, the Access ADR and
   migrations 111/112. **This one is material**: it affects schema, compiler settings,
   provider capability and stored precision, and it is the last open item that can change
   the canonical data model.
3. `frontend/web` `[LEGACY]` and `backend/mobile` consumers — material only for legacy
   elimination sequencing, not for canonical architecture.
4. ~62 unclassified test files — material for the protection strategy's completeness, not
   for architecture.

**Explicitly NOT blocking:** the remaining test files and consumer surfaces are
enumeration work whose outcome cannot change a canonical decision.

**The gate is not yet satisfied**, on one material item (CF-003 numeric envelope) plus the
two undrafted protection strategies.

## 23D. Batch 10 — the numeric envelope, and the missing upper stack

### 23.77 CF-003 RESOLVED — the three "competing authorities" are one layered envelope

FACT. Compiler semantic envelope:
`@Value("${platform.statistical-contract.numeric.max-precision:28}")` and
`max-scale:16` -> `NumericEnvelope(28, 16)`.

FACT. Canonical storage: migrations 111/112 use `DECIMAL(38,16)` (3 occurrences) and alter
from `DECIMAL(28,10)` (1). `statistics.observation.numeric_value` exists in both forms
across the chain — the old `DECIMAL(28,10)` and the new `DECIMAL(38,16)`.

FACT. Migration 111's header states the cause and the evidence: "Numeric envelope of an
exact measure (ADR-011, owner decision D-3, 2026-09-20). **Problem: a measure could declare
at most scale 10.** The approved KIDS R8 package carries 221 of 880 published values with
11 to 16 decimals (computed rates); **scale 10 altered them on load.** 'No digit of the
source is lost.'"

**CONCLUSION: CF-003 was mis-recorded as a contradiction.** Q34's `28,10`, ADR D-3's
scale 16 and migration 111/112's `38,16` are **not three competing authorities over one
value** — they are three *layers* of one coherent envelope, recorded in three places and
never reconciled in a single document:

| Layer | Envelope | Why |
|---|---|---|
| Semantic declaration (what a contract may declare) | precision <= 28, scale <= 16 | 28 is the **Microsoft Access** `DECIMAL` ceiling, and Access is a required round-trip provider |
| Canonical storage | `DECIMAL(38,16)` | 38 is the SQL Server ceiling; a declared `28,16` value needs 12 integer + 16 fractional digits, and `38,16` provides 22 + 16 — a **strict superset** with headroom |
| Legacy storage | `DECIMAL(28,10)` | the original choice; scale 10 silently altered source values — an INV-005 violation, now corrected |

**DR-006 (HIGH confidence): the canonical numeric envelope is exact decimal only —
declaration bounded by the weakest required provider, storage a strict superset.**

1. **Exact decimal only.** Floating point is never permitted in the canonical path.
   `DOUBLE` appears three times in the migration chain and belongs exclusively to the
   legacy KIDS statistical model already classified as legacy (section 11).
2. **Declaration envelope: precision <= 28, scale <= 16.**
3. **Canonical storage: `DECIMAL(38,16)`.**
4. **The envelope belongs in the provider capability registry, not in a Spring property.**
   The ceiling of 28 exists *because* Access cannot represent more. Hardcoding it as
   `platform.statistical-contract.numeric.max-precision` states the consequence and hides
   the cause, and it means a deployment can silently widen the envelope beyond what a
   registered provider can round-trip. **This directly answers CF-010** (provider
   capability has no max-scale dimension): max precision *and* max scale must become
   declared capability dimensions, and the compiler's envelope must be derived as the
   minimum across the providers a contract targets.
5. **Compatibility rule for contracts approved at `28,10`:** widening is loss-free and
   requires no re-approval — every `DECIMAL(28,10)` value is exactly representable in
   `DECIMAL(38,16)`. **But widening does not restore digits already lost.** Values
   truncated at load under scale 10 are unrecoverable from the Data Plane and must be
   **reloaded from the immutable source artifact** in object storage, whose checksum
   identity makes that reload verifiable. This is a concrete migration task, not a schema
   change.

*Falsification attempts.*
1. *Is `38,16` sufficient for aggregation?* Today yes, accidentally: CF-027 computes
   aggregates in memory with `BigDecimal`, so no fixed-precision overflow occurs. **When
   CF-027 is corrected by pushing aggregation into SQL, `SUM` over `DECIMAL(38,16)` can
   overflow.** Recorded as a binding constraint on CF-027's remediation — the aggregate
   result type must be widened or the aggregation must remain exact in the application.
   This cross-link would have been missed if the two had been decided separately.
2. *Is the serialization consistent?* **No.** `ContractPhysicalQueryService.canonicalSource`
   emits `CONVERT(nvarchar(128), o.numeric_value)` (QF-005) while the statistical reader
   returns `BigDecimal`. The generic page surface returns strings and the statistical
   surface returns numbers for the same underlying value. `nvarchar(128)` is wide enough
   to be lossless, so this is a **representation inconsistency, not a loss** — but the
   canonical API must declare one exact-decimal serialization. JSON number is unsafe for
   16-scale decimals in consumers that parse to IEEE-754 double; a JSON **string** is the
   defensible canonical form.
3. *Is 28 the right ceiling if Access is later dropped?* Then the envelope should widen —
   which is exactly why point 4 moves it into the capability registry rather than fixing
   it as a constant.

**CF-003: RESOLVED. The canonical data model can be frozen on this axis.**

### 23.78 THE MISSING UPPER STACK — recovered from the repository's own words

The mid-turn correction was right, and the repository names the layer explicitly.
`docs/control-plane-and-site-schema-learning-guide.md` states the intended model:

```text
Platform Meta-Contract
  ↓ აღწერს საკუთარ Control Plane-ს          ("describes its own Control Plane")
Control Plane Schema
  ↓ ქმნის/ამტკიცებს site contract-ს          ("CREATES / APPROVES the site contract")
Site Contract
  ↓ აღწერს dataset/field/key/index/relation/semantics-ს
Generic Data Plane Family Schema
  ↓ ინახავს კონკრეტულ rows-ს
Publication Snapshot
  ↓
API / export / cache / UI metadata
```

**The repository's real name for the highest authority is `Platform Meta-Contract`, and
the contract-creating layer is `Control Plane Schema`.** These are not new names invented
here; they are the terms the platform's own learning guide uses, and they map exactly onto
the doctrine's chain in `docs/reference/engineering/ARCHITECTURE.md`: M3 semantic
constitution = Platform Meta-Contract; M2 contract grammar = Control Plane Schema; M1
approved declarations = Site Contract / statistical contract / ingestion contract; M0 =
rows, snapshots, responses.

**FACT, and this is the decisive measurement.** A repository-wide search for
`meta-contract` / `metacontract` / `meta contract` returns:

- **six documentation files:** `docs/README.md`,
  `docs/reference/ENGINEERING-QUALITY-DOCTRINE.md`,
  `docs/control-plane-and-site-schema-learning-guide.md`,
  `docs/system-complete-learning-guide.md`,
  `docs/api-platform-capabilities-and-mechanisms.md`,
  `docs/final-unified-physical-virtual-contract.md`;
- **zero Java files;**
- **zero SQL files.**

**The highest semantic authority of this platform exists only as prose.** It has no
persisted artifact, no grammar, no validator, no producer, no revision model, no digest and
no runtime representation.

And the layer beneath it — `Control Plane Schema`, whose stated job is *"ქმნის/ამტკიცებს
site contract-ს"*, to **create and approve the site contract** — is exactly the capability
that batch 3 proved absent (CF-024a) and batch 7 proved the composer depends on.

### 23.79 RC-003 — the deepest root cause, superseding RC-002's framing

- **INTENDED ARCHITECTURE:** `Platform Meta-Contract -> Control Plane Schema -> Site
  Contract -> Data Plane Family Schema -> Publication Snapshot -> API`.
- **CURRENT IMPLEMENTATION:** the bottom four layers are built, several of them well. The
  **top two are not implemented at all.**
- **DRIFT:** migrations are not accidentally carrying declaration duty (RC-002's framing).
  **Migrations are the substitute for the entire missing upper stack.**

```text
INTENDED                        IMPLEMENTED
Platform Meta-Contract     ->   documentation only (6 docs, 0 code, 0 schema)
Control Plane Schema       ->   DDL exists; the "creates/approves site contract"
                                capability is ABSENT  (CF-024a)
Site Contract              ->   rows produced ONLY by KIDS seed migrations  (RC-002)
Data Plane Family Schema   ->   built, generic, correct
Publication Snapshot       ->   built, but two liveness authorities  (RC-001)
API / serving              ->   built, with breaks 9-12
```

**RC-003 (NEW, deepest): the platform's two highest meta-levels were never implemented, and
the migration chain stands in for them.** This single cause explains, without further
assumption:

- CF-024a — the producer is missing because the *layer* that produces is missing;
- RC-002 — migrations carry declaration duty because nothing else can;
- the clean-rebuild divergence (23.73) — declaration state lives in migration history;
- SCH-003's unachievability — contract-only onboarding requires the M2 producer;
- why the composer (DR-003) is correct yet unwired — it consumes an M1 layer that has no
  producer;
- why `CLAUDE.md`'s mandate to "preserve the M3 -> M2 -> M1 -> M0 authority chain" has been
  satisfiable only on paper: M3 and M2 exist only on paper.

**Confidence: HIGH.** Falsification attempted: a search for any Java or SQL artifact
carrying the meta-contract concept found none, and the one table whose name suggested it
(`platform.metadata_schema`, migration `062_universal_metadata_plane.sql`) is read by only
two classes — `SemanticAccessPackageReader` and `PlatformAccessIngestionService` — i.e. it
is **Access package metadata for ingestion**, not a meta-model governing contract creation.
That candidate is therefore rejected on evidence, not on naming.

### 23.80 Answers to the lineage-gate questions

| Question | Answer |
|---|---|
| What is the highest semantic authority? | `Platform Meta-Contract` — **documentation only** |
| What model/declaration does it store? | **nothing**; no artifact exists |
| What creates contracts? | **Nothing.** The `Control Plane Schema` layer that should is unimplemented; KIDS seed migrations substitute |
| From what are contracts created? | hand-written SQL in the migration chain |
| Which contract families are generated? | **none**; all are hand-authored or migration-seeded. The statistical contract is *human-authored through a governed workflow* — the only family with a real producer |
| How does Schema participate? | intended as the creating authority; in practice only the physical DDL exists |
| How does Site Contract participate? | M1 serving declaration; consumed everywhere, produced nowhere |
| How does Statistical Contract participate? | M1 semantic declaration, governed producer exists (`ContractWorkflow`), unlinked to the site contract (DR-004) |
| How does Ingestion Contract participate? | created by `StatisticalBindingService` without a revision, which closes governed publication (CF-032) |
| How does approval change authority? | at `SiteContractRevisionApprovalService.approve()` and `ContractWorkflow.approve()`; everything downstream is derivation (23.68) |
| How does a contract become an Access realization? | for the **statistical** contract, via `AccessAuthoringAdapter` from the semantic plan. For the **site** contract, `JdbcPackageContractSource` + `AuthoringPolicy` exist but are **unwired**, and no Access generator consumes `PackagePlan` |
| How does data enter that realization? | a person fills the generated ACCDB; `StatisticalLoadService` reads it back |
| How does it return to the platform? | upload -> normalize -> Control Plane binding -> object storage -> one Data Plane transaction |
| How is provenance preserved end to end? | **partially** — artifact checksums, `contract_revision_digest`, `revisionDigest`, source-record lineage exist; but generated contracts have **no generator-version / source-declaration provenance**, because no contract is generated |

### 23.81 Required gate fields

```text
CF-003: RESOLVED
DR-006: exact decimal only; declaration <= (28,16) bounded by the weakest required
        provider; canonical storage DECIMAL(38,16) as a strict superset; the envelope
        moves into the provider capability registry (closes CF-010); 28,10 widening is
        loss-free but truncated values require reload from the source artifact

DATA_CONTRACT_LINEAGE_GATE: FAIL

SCHEMA_AUTHORITY:            PARTIAL — recovered by name and intent; UNIMPLEMENTED as artifact
SCHEMA_TO_SITE_CONTRACT:     MISSING_IMPLEMENTATION — the stated edge exists only in docs
SITE_CONTRACT_TO_ACCESS:     PARTIAL — statistical path implemented; site path built but unwired
ACCESS_PHYSICAL_MODEL:       PARTIAL — no .accdb artifact inspected in this batch
CONTRACT_HIERARCHY:          VERIFIED — six families mapped with producers and authority
FORWARD_LINEAGE:             PARTIAL — breaks at the top two layers
BACKWARD_LINEAGE:            PARTIAL — not traced from a user-visible value to Access field
INFORMATION_LOSS_AUDIT:      PARTIAL — numeric axis complete (DR-006); other axes catalogued
                             (CF-005 attributes, CF-027 aggregation, CF-028 parameters) but
                             not audited transformation by transformation
GRAPH_CONNECTIVITY:          FAIL — the two highest nodes have no implementing artifact
```

**The completeness gate does not pass, and the reason is architecturally material rather
than clerical: the recovery has established that the top of the intended architecture is
absent.** That is a finding, not an omission — but it means the lineage cannot be verified
end to end, because two of its layers have nothing to verify.

## 23E. Batch 11 — the physical Access model, and RC-003 under falsification

### 23.82 Method note — read-only guarantee

The canonical artifact
`platform/apps/geostat/backend/api/kids-portal-v1-canonical-r8-final.accdb` (2,953,216
bytes) was **copied to the session scratchpad first**, and every inspection ran against the
copy with Jackcess 4.0.7 opened via `setReadOnly(true)`. The repository artifact was never
opened by the tooling. Jackcess was taken from the Gradle cache, not added as a dependency.

### 23.83 The physical Access model — 24 tables, recovered directly

FACT. `FORMAT = V2010 [VERSION_14]`, 24 user tables. Classified by structure, producer,
consumer and content — not by name:

| Structure | Rows | Cols | Classification |
|---|---|---|---|
| `__gs_package` | 1 | 8 | **DECLARATION — package envelope / contract binding** |
| `__gs_dataset` | 15 | 4 | **CONTRACT REPRESENTATION** — dataset declarations |
| `__gs_field` | 122 | 5 | **CONTRACT REPRESENTATION** — field declarations |
| `__gs_key` | 17 | 4 | **CONTRACT REPRESENTATION** — key declarations |
| `__gs_relation` | 21 | 7 | **CONTRACT REPRESENTATION** — relation declarations |
| `__gs_projection` | 6 | 5 | **CONTRACT REPRESENTATION** — projection declarations |
| `__gs_page` | 6 | 12 | **CONTRACT REPRESENTATION** — page declarations |
| `__gs_metadata` | 267 | 12 | **METADATA** — metadata assertions |
| `__gs_metadata_schema` | 2 | 5 | **METADATA SHAPE REGISTRY** (see 23.85) |
| `__cl_scheme` / `__cl_version` / `__cl_item` / `__cl_alias` | 4 / 4 / 36 / 36 | | **CLASSIFIER DATA** (reference) |
| `__cl_hierarchy` | **0** | 4 | **EMPTY contract-defined structure** — declared capability, no content |
| `__ent_kids_glossary_entry` | 178 | 6 | **DATA** — entity family |
| `__ent_kids_goal` | 36 | 8 | **DATA** — entity family |
| `__ent_kids_resource` | 225 | 8 | **DATA** — entity family |
| `__rel_kids_resource_subcategory_assignment` | 230 | 7 | **DATA** — relation family |
| `__rel_kids_statistical_semantic_binding` | 43 | 10 | **LEGACY** — carrier-bound binding (section 11) |
| `__stat_kids_statistical_input` | **880** | 13 | **DATA** — the statistical observations |
| `__stat_metric` | **43** | 10 | **LEGACY** — the 43-measure model |
| `__stat_unit` | 10 | 5 | **DATA** — units |
| `__raw_document` | 456 | 22 | **RAW / SOURCE TRAIL** |
| `__raw_kids_statistical_carrier` | **43** | 6 | **LEGACY** — carrier model |

**The prefix families in the physical file match the doctrine's declared transport
namespaces exactly** (`__raw_`, `__cl_`, `__stat_`, `__ent_`, `__rel_`, `__gs_` —
`ENGINEERING-QUALITY-DOCTRINE.md`, "Data-family design rule"). The doctrine's statement
that a prefix is "a boundary, not a shortcut" is borne out: the file carries one table per
declared family member, not a universal blob.

**Independent cross-verification of recorded domain knowledge (section 14).** The physical
file confirms, without relying on any document: **880** statistical rows (item 1's 880
observations), **43** metrics / **43** carriers / **43** semantic bindings (item 1's 43
indicator series, and item 2's superseded 43-measure model). This is the **legacy R8
package**, i.e. the artifact section 14 item 11 requires be left untouched — which this
recovery has honoured.

### 23.84 Contract tables AND data tables — the stated intent is VERIFIED

The human-provided intent was that the Access file contains both (A) structures realizing
the declared contract and (B) the corresponding data. **Confirmed by direct physical
evidence:**

- **(A)** nine `__gs_*` tables declaring package, dataset, field, key, relation,
  projection, page, metadata and metadata schema — 455 declaration rows in total;
- **(B)** twelve data tables across the `__ent_`, `__rel_`, `__stat_`, `__cl_` and `__raw_`
  families — 2,177 data rows in total.

**`__gs_package` carries the binding to the site contract**, and its single row is the
decisive evidence for the `SITE_CONTRACT -> ACCESS` edge:

```text
product_code     = KIDS_PORTAL
contract_code    = KIDS_PORTAL_V1
contract_revision= 8
package_code     = kids-portal-canonical
package_version  = 8.0.0
load_mode        = SNAPSHOT
source_system    = KIDS_LEGACY_ACCESS
artifact_profile = ACCESS_CANONICAL_R8_PAGE_MANIFEST
```

**So the Access file explicitly declares itself to be the physical realization of
`KIDS_PORTAL_V1` revision 8.** The edge exists and is declared *in the artifact itself*.

**But the binding is a plain value, not a digest.** `contract_revision = 8` is an
unverified claim: there is no `contract_checksum`, no revision digest and no signature
anywhere in `__gs_package`. The artifact asserts conformance; nothing proves it. This is
precisely the risk INV-011 anticipates ("server load must not trust embedded provenance"),
and it is now confirmed at the byte level rather than inferred from code.

**AUTHORITY-INVERSION RISK (recorded, not yet proven exploited):** because the return path
reads declarations *out of the file* (`__gs_dataset`, `__gs_field`, `__gs_relation`), an
Access file whose `__gs_*` tables disagree with the server-side site contract would be
interpreted on its own terms unless the server re-validates against
`site_contract_revision` 8. Whether `PlatformAccessIngestionService` performs that
re-validation was **not verified in this batch** — it is the single most important
remaining question about the Access boundary, and it decides whether
`ACCESS_TO_PLATFORM_LINEAGE` is VERIFIED or BROKEN.

### 23.85 `__gs_metadata_schema` — the batch-10 rejection CONFIRMED by physical evidence

Batch 10 rejected `platform.metadata_schema` as a candidate for the missing Control Plane
Schema on the grounds that only two ingestion classes read it. The physical file settles
it. Its two rows are:

```text
schema_code=CORE_RESOURCE_V1, namespace_code=CORE, schema_revision=1, approval_state=READY
  schema_json={"type":"object","properties":{"title":{"type":"localizedText"},
               "description":{"type":"localizedText"}}}
schema_code=VISUALIZATION_V1, namespace_code=PRESENTATION, schema_revision=1, READY
  schema_json={...,"chartType":{"type":"CODE"}}
```

This is a **shape registry for metadata assertions** — it governs the 267 rows of
`__gs_metadata`, nothing more. It has a revision and an approval state, which makes it a
legitimate, well-formed **bounded specialization**, but it does not create contracts and
does not govern datasets, fields or relations. **It is not the Control Plane Schema.** The
batch-10 rejection was correct and is now evidence-backed rather than inference-backed.

### 23.86 RC-003 — falsification attempted by behavior, survived, STRENGTHENED

The instruction required a search by **behavior, not names**. A census of every SQL write
verb against every `platform.*` table across all of `src/main/java` was taken:

**Decisive results:**

| Table | Writes found | Kind |
|---|---|---|
| `platform.site_contract_revision` | 2 | **UPDATE only** — the `SUPERSEDED` and `APPROVED` status transitions in `SiteContractRevisionRepository`. **ZERO INSERTs.** |
| `platform.site_contract_revision_approval` | 1 | INSERT — approval *evidence*, not the declaration |
| `platform.site_contract_dataset` / `_field` / `_relation` / `_classifier` | **0** | none |
| `platform.contract_page_binding` | **0** | none |
| `platform.contract_source` | 1 | **UPDATE** of `mapping_spec_json` on an existing row |
| `platform.data_product` | 1 | **UPDATE** of `tenant_key` only |

**RC-003 CONFIRMED, confidence HIGH, and strengthened on one axis:** not only is there no
producer for the site contract — **there is no producer for `platform.data_product`
either.** Registering a new product is migration-only, exactly as registering a site
contract is. The missing upper stack is therefore wider than batch 10 stated: it covers
product registration as well as contract authoring.

The only Control Plane rows production code *creates* are operational
(`outbox_event`, `api_operation`, `job_lease`, `release`, `schema_migration`), the
statistical family (`statistical_contract_draft`, `_event`, `statistical_reference`,
`statistical_chart`, `statistical_unit`), the registry rows the statistical binding needs
(`dataset`, `dataset_version`, `metric`, `dimension`, `measure`, `source_system`,
`ingestion_contract`), plus `metadata_subject`, `contract_approval_receipt` and
`data_product_tenant_assignment`.

**Restated precisely: the statistical subsystem is the only part of this platform that can
declare anything at runtime. Everything else is declared by migration.**

### 23.87 SITE_CONTRACT -> ACCESS — the verified path and its break

```text
site_contract_revision (APPROVED)          [rows: migration-produced]
   |  VERIFIED edge — JdbcPackageContractSource.approved(code, revision)
   v
PackagePlan (+ content digest)              [DERIVED, never persisted]
   |  MISSING edge — no Access generator consumes PackagePlan
   v
   X
```

and, separately, the path that **is** implemented:

```text
statistical_contract_draft (APPROVED)
   |  VERIFIED — ContractWorkflow.approvedPlan (digest-verified)
   v
SemanticPlan
   |  VERIFIED — statistical physical planner
   v
PhysicalPlan
   |  VERIFIED — AccessAuthoringAdapter (Jackcess)
   v
.accdb: typed tables, lookup/codelist data, contract copy, stamp, navigation
```

**Conclusion: Access is genuinely the physical realization of a declared contract — but of
the *statistical* contract, not of the *site* contract.** The site-contract branch has a
correct, tested derivation (`PackagePlan`, `AuthoringPolicy`) and **no generator**. The R8
file inspected above was produced by the older Access-package generation path and carries
`__gs_*` declarations that mirror the site contract, which is why the two look unified from
outside.

**Which part of the site contract is realized:** dataset, field, key, relation, projection
and page declarations all appear as `__gs_*` tables — so the *shape* is realized. What is
**not** realized in the artifact: the contract checksum/digest, serving policy, the
approval state, tenancy, and any binding to the statistical contract revision (which does
not exist as a link at all — DR-004).

### 23.88 Gate fields after batch 11

```text
SITE_CONTRACT_TO_ACCESS:    PARTIAL
    declaration edge VERIFIED in the artifact (__gs_package -> KIDS_PORTAL_V1 r8);
    generator edge MISSING for PackagePlan; the implemented generator serves the
    statistical contract
ACCESS_PHYSICAL_MODEL:      VERIFIED
    24 tables inventoried and classified from the physical file, read-only
ACCESS_TO_PLATFORM_LINEAGE: PARTIAL
    reader/provider path known; whether the server re-validates the file's __gs_*
    declarations against site_contract_revision 8 is NOT verified — this is the
    authority-inversion question (23.84)
BACKWARD_LINEAGE:           PARTIAL
    bottom-up trace reaches the Access physical field and the __gs_* declaration;
    it cannot continue above site_contract_revision because the Control Plane
    Schema layer has no artifact (MISSING IMPLEMENTATION, not unexplained)
INFORMATION_LOSS_AUDIT:     PARTIAL
    numeric axis complete (DR-006); provenance axis now has a concrete finding
    (no digest in __gs_package); the remaining categories are catalogued but not
    audited transformation by transformation
PLATFORM_META_CONTRACT:     ASPIRATIONAL
    6 documents, 0 code, 0 schema; no ADR makes it normative; no artifact, producer,
    validator or lifecycle exists. It is a recovered *intent*, not a lost implementation
CONTROL_PLANE_SCHEMA:       ASPIRATIONAL as a contract-creating authority
    the physical Control Plane DDL exists and is real; the "creates/approves the site
    contract" capability the learning guide attributes to it does not
RC-003:                     CONFIRMED, HIGH confidence, strengthened
    behavior-based census: 0 INSERTs into any site-contract declaration table and
    0 producers for platform.data_product
GRAPH_CONNECTIVITY:         PASS-with-classification
    every significant node is now classified A-E; the two upper nodes are class E
    (required-but-missing), which is an explained absence, not an unexplained node
DATA_CONTRACT_LINEAGE_GATE: FAIL
    one material unknown remains: the Access return-path re-validation (23.84)
```

**Why the gate still fails, stated precisely.** Per the batch instruction's own
distinction, `MISSING_IMPLEMENTATION` does not block completion — and the Platform
Meta-Contract and Control Plane Schema are now *proven* missing with their intended
responsibilities recovered, which satisfies that standard. What blocks the gate is
different: **whether Access-supplied `__gs_*` declarations can override server-side
contract authority on ingestion is UNRESOLVED**, and that is an authority question, not an
absence. If the server does not re-validate, a physical artifact can act as semantic
authority — an authority inversion that would materially change the canonical architecture
and the legacy-elimination plan.

## 23F. Batch 11B — the Access trust boundary: MODEL C, with a bounded authority inversion

### 23.89 The decisive code — `PlatformAccessIngestionService.materializeMetadata`

FACT, lines 106-122, quoted in substance:

```java
/** Materializes optional Access metadata into the governed Control Plane after package validation. */
public void materializeMetadata(SemanticAccessPackage pack) {
  Long revisionId = controlPlane.query(
      "SELECT TOP 1 site_contract_revision_id FROM platform.site_contract_revision "
    + "WHERE contract_code=? AND revision=?", ..., pack.contractCode(), pack.contractRevision());
  if (revisionId == null) throw new IllegalArgumentException("Metadata package references unknown contract revision");

  for (SemanticAccessMetadataSchema s : pack.metadataSchemas())
      controlPlane.update("MERGE platform.metadata_schema ... schema_json=s.schema_json,"
        + "lifecycle_status=s.lifecycle_status ...", namespace, s.schemaCode(), s.revision(),
        s.schemaJson(), state(s.approvalState()));

  for (SemanticAccessMetadataAssertion a : pack.metadataAssertions()) {
      if (subject == null) controlPlane.update("INSERT platform.metadata_subject(...,lifecycle_status,visibility) "
        + "VALUES(?,?,?,?,'APPROVED','PUBLIC')", ...);
      Long schema = controlPlane.query("SELECT TOP 1 s.metadata_schema_id ... WHERE n.namespace_code=? "
        + "ORDER BY s.revision DESC", ...);
      controlPlane.update("MERGE platform.metadata_assertion ... lifecycle_status=? ...",
        ..., state(a.lifecycleStatus()), ...);
  }
}
private static String state(String s){ return "READY".equalsIgnoreCase(s)||"APPROVED".equalsIgnoreCase(s) ? "APPROVED" : "DRAFT"; }
```

### 23.90 ANSWER — MODEL C (HYBRID), with the boundary drawn exactly

**The trust boundary runs between the data plane and the metadata plane.**

| Axis | Verdict | Evidence |
|---|---|---|
| **Data / structural mapping** | **SERVER-BOUND** | line 236 resolves load targets from **server-side** `platform.site_contract_dataset` (`CONCAT(N'ACCESS.', sc.access_table_name)`, `natural_key_expression`, `dataset_version_id`). The file's `__gs_dataset` / `__gs_field` / `__gs_key` do **not** decide what is loaded where. |
| **Contract identity** | **SERVER-CHECKED for existence, NOT for approval** | line 108 filters on `contract_code` + `revision` only. **There is no `status='APPROVED'` predicate.** A `DRAFT`, `REVIEW_REQUIRED` or `SUPERSEDED` revision satisfies the lookup. An *unknown* contract is correctly rejected (line 109). |
| **Metadata schemas** | **ACCESS-AUTHORITATIVE** | line 113 `MERGE platform.metadata_schema` writes the **package's own `schema_json`** into the canonical registry, with `lifecycle_status` taken from the **package's own `approval_state`** via `state(...)`. |
| **Metadata subjects** | **ACCESS-TRIGGERED, hardcoded APPROVED/PUBLIC** | line 117 inserts with literal `'APPROVED','PUBLIC'` — a subject created from a package is immediately approved and publicly visible with no governance step. |
| **Metadata assertions** | **ACCESS-AUTHORITATIVE lifecycle** | line 119 uses `state(a.lifecycleStatus())` from the file. |

**`state()` is the inversion in one line.** The R8 artifact inspected in batch 11 carries
`__gs_metadata_schema` rows with `approval_state = READY`. `state("READY")` returns
`"APPROVED"`. **A value typed into an Access file becomes an approval state in the
canonical Control Plane.**

**ACCESS_DECLARATIONS_AUTHORITY: HYBRID.** The inversion is **real, confirmed, and bounded
to the metadata plane**. It does not extend to dataset, field, relation or physical-mapping
semantics, which remain server-bound.

### 23.91 NEW CF-033 — the platform's only schema registry is writable by a transport artifact

The inversion's significance is larger than "some metadata is trusted", and it connects
directly to RC-003.

`platform.metadata_schema` is the **only** schema-shaped registry in the Control Plane —
the very table batch 10 examined as a candidate for the missing `Control Plane Schema`
layer and correctly rejected as a metadata shape registry. Batch 11B now shows that **this
one schema-like registry has an Access package as its de facto producer.**

So the platform's authoring situation is precisely inverted from its doctrine:

- the **site contract** (M1, serving declaration) — no producer at all (CF-024a, RC-003);
- `platform.data_product` — no producer;
- the **metadata schema registry** — a producer exists, and it is a **transport artifact**.

**The one thing a package can create is the one thing a package should never create.**

- **Risk:** HIGH (governance/authority). **Confidence:** HIGH.
- **Not exploited today in a proven way:** the R8 artifact's two schemas (`CORE_RESOURCE_V1`,
  `VISUALIZATION_V1`) are benign. The defect is the capability, not the current content.

### 23.92 Tamper / drift falsification — answered from code

An Access file keeping `contract_code = KIDS_PORTAL_V1`, `contract_revision = 8` while
altering its contents:

| Tampered element | Rejected? | Why |
|---|---|---|
| field type, nullability, relation, cardinality, table mapping, precision/scale in `__gs_*` | **Not by `materializeMetadata`** — these are not consulted there. Load targets come from the server (line 236), so tampering them does not redirect data. Whether the package *reader* validates them structurally is **not verified** (see the residual item below). |
| `__gs_metadata_schema.schema_json` | **NO** — merged into the canonical registry verbatim |
| `__gs_metadata_schema.approval_state` | **NO** — `READY` becomes canonical `APPROVED` |
| metadata assertion `lifecycle_status` | **NO** — package value wins |
| new metadata subject | **NO** — created as `APPROVED`/`PUBLIC` |
| unknown `contract_code` | **YES** — line 109 |
| **non-approved revision** (DRAFT / REVIEW_REQUIRED / SUPERSEDED) | **NO** — approval is never checked |
| cross-site contract identity | **partially** — the code+revision must exist somewhere; no product/tenant scoping is applied at line 108 |

**PACKAGE_TO_CONTRACT_BINDING: MISSING.** A behavioural search found **no** mechanism
proving that a given artifact is the realization of a given approved revision: no content
hash, no revision digest, no manifest fingerprint, no physical-plan digest, no
deterministic regeneration comparison. Batch 11 established that `__gs_package` carries no
digest column; batch 11B establishes that the server never computes one either. The binding
is a **name-and-number assertion, checked for existence only**.

**The minimum correct canonical binding does not require cryptography.** Because
`PackagePlan` already computes a deterministic, order-independent content digest over the
approved declaration (`geostat.package-plan.v1`, DR-003), **deterministic semantic
equivalence is sufficient and is already half-built**: compute the plan digest from the
approved revision, compute the same digest from the package's `__gs_*` declarations, and
require equality before any package metadata influences canonical state. That reuses an
existing, tested mechanism rather than adding one.

### 23.93 Partial-mutation and schema-binding defects

**PARTIAL_MUTATION_RISK: YES.** `materializeMetadata` issues its `MERGE`/`INSERT`
statements as **individual `controlPlane.update` calls with no transaction wrapper**. The
data-plane path in the same class uses `tx.executeWithoutResult(...)` (lines 98, 101);
`materializeMetadata` does not. A failure part-way leaves canonical metadata schemas and
subjects written and the rest not. Canonical mutation therefore begins before validation
completes for the remaining items.

**QF-012 (NEW):** line 118 resolves the assertion's schema with
`ORDER BY s.revision DESC` for the namespace — **ignoring the revision the assertion was
authored against**. An assertion is bound to whatever schema revision is newest at
ingestion time, not to the one it conforms to. This is the same class of defect as CF-004's
availability risk, but here it is silent rather than fail-closed.

**Residual item, declared honestly:** the Javadoc says metadata is materialized "after
package validation". `SemanticAccessPackageReader` was **not read to EOF in this batch**,
so *what* that validation covers structurally is unverified. This does not change the
authority verdict — the metadata writes above are unconditional once the contract lookup
succeeds — but it leaves the structural-validation depth of the reader unquantified.

### 23.94 The bootstrap paradox — resolved without weakening authority

The batch instruction warned against making Access authoritative because the server-side
producer is missing. The evidence supports keeping that line, and the distinction is clean:

- **SELF-DESCRIBING** — the package must carry `__gs_*` so it can be opened, filled,
  validated and exchanged offline. Batch 11 proved it does, and that capability is
  REQUIRED behavior.
- **SELF-AUTHORITATIVE** — the package must not be able to define or approve canonical
  semantics. This is where the current implementation crosses the line, on the metadata
  plane only.

**DR-007 (HIGH confidence): a physical package is self-describing, never
self-authoritative. Correspondence to an approved declaration must be established before
package content influences canonical state.**

Derived rule, stated as the evidence supports it:

1. The server resolves the named contract revision and **requires `status='APPROVED'`**.
   The current absence of that predicate is a defect regardless of everything else.
2. Correspondence is proven by **deterministic semantic equivalence** — the package-plan
   digest of the approved revision must equal the digest computed from the package's own
   declarations — not by trusting a self-reported value.
3. Package-supplied lifecycle and approval states are **never** mapped onto canonical
   lifecycle states. `state()` inverts authority and must be removed as a concept, not
   merely tightened.
4. Metadata schemas are canonical declarations and require the same governed producer as
   any other declaration; a package may *reference* a schema, never *define* one.
5. Validation completes **before** any canonical mutation, in one transaction.

**Provider boundary (instruction 11):** contract correspondence belongs to the **contract
layer**, not the Access adapter; structural/type feasibility belongs to the **Access
provider**; the ingestion layer orchestrates and must not contain either. Today
`PlatformAccessIngestionService` mixes package parsing, contract resolution, canonical
metadata creation, dataset source resolution and ingestion in one class — recorded as an
architectural problem (SRP/ARC-001), not refactored.

**Relationship to RC-003:** this does **not** reduce RC-003. It sharpens it — the missing
producer must be built for the site contract *and* for metadata schemas, and until it
exists the correct interim posture is to **reject** package-supplied schema definitions,
not to accept them because nothing else can create them.

### 23.95 Gate fields after batch 11B

```text
ACCESS_DECLARATIONS_AUTHORITY: HYBRID
    data/structural mapping SERVER-BOUND; metadata plane ACCESS-AUTHORITATIVE
PACKAGE_TO_CONTRACT_BINDING:   MISSING
    no digest, no fingerprint, no equivalence check; existence-only lookup
PRE_INGESTION_VALIDATION:      PARTIAL
    unknown contract rejected; approval status NOT checked; metadata unvalidated
PARTIAL_MUTATION_RISK:         YES
    materializeMetadata performs canonical writes outside any transaction
SITE_CONTRACT_TO_ACCESS:       PARTIAL
    declaration edge verified in the artifact; generator edge missing for PackagePlan
ACCESS_TO_PLATFORM_LINEAGE:    PARTIAL
    data path verified and server-bound; metadata path is an authority inversion (CF-033)
BACKWARD_LINEAGE:              PARTIAL
    complete to site_contract_revision; stops above it by proven absence, not by ignorance
INFORMATION_LOSS_AUDIT:        PARTIAL
DATA_CONTRACT_LINEAGE_GATE:    FAIL
RECOVERY STATUS:               INCOMPLETE
```

**Why the gate still fails, and what changed.** The batch-11 blocker — "is the Access
artifact trusted as semantic authority?" — is now **answered, not open**: HYBRID, with the
boundary mapped field by field. That authority question is closed.

What remains is narrower and is no longer an *unknown authority boundary*: the structural
validation depth of `SemanticAccessPackageReader` (23.93) is unquantified, and the
information-loss matrix is incomplete on the non-numeric axes. Neither can move the
canonical architecture — DR-007 holds whatever the reader turns out to validate — but the
reader question directly determines the **tamper posture** recorded in 23.92, which the
rehabilitation plan must state correctly. That is a material fact about the current system,
so the gate does not yet pass.

## 23G. Batch 11C — generation/validation symmetry: the generator is clean, the return path checks nothing

### 23.96 `SemanticAccessPackageReader` — fully characterized, 56 lines, a pure parser

FACT. The reader performs exactly four kinds of check and no others:

| Check | Evidence |
|---|---|
| required metadata tables exist | `for (String name : REQUIRED) if (!tables.containsKey(name)) throw ... "Missing v3 package metadata table: "` |
| `__gs_package` has exactly one row | `if (packageRows.size() != 1) throw ... "__gs_package must contain exactly one row"` |
| every consumed column is present and non-blank | `required(row, name)` -> `"Missing metadata..."` on null/blank |
| declared integers parse | `integer(row, name)` -> `NumberFormatException` wrapped |

It does **not** validate types against the contract, relations, keys, correspondence,
provenance, digests or data. It is a **physical package reader**, and as such its
responsibility is legitimate and correctly scoped. Reading declarations out of a package is
not itself an authority violation (batch instruction 6) — the violation, where it exists,
is downstream in `materializeMetadata` (CF-033).

**ACCESS_READER_ROLE: PHYSICAL PACKAGE READER — correctly scoped.**

### 23.97 `SemanticAccessPackageValidationService` — 120 lines, entirely package-internal

FACT. The class contains **zero** references to `controlPlane`, `jdbc` or any `SELECT`.
Every check is self-consistency within the package:

- `DUPLICATE_DATASET_CODE`, `DUPLICATE_FIELD`, `DUPLICATE_KEY_ORDER`,
  `DUPLICATE_PROJECTION_CODE`, `DUPLICATE_METADATA_SCHEMA`, `DUPLICATE_METADATA_ASSERTION`,
  `DUPLICATE_STATISTICAL_BINDING`, `BLANK_STATISTICAL_BINDING_KEY`,
  `ACCESS_SCHEMA_READ_FAILURE`;
- closed type vocabulary (`DECIMAL`, `DATE`, `DATETIME`, `BOOLEAN`, `GEOMETRY`, `GEOJSON`,
  `CODE`, `CONTENT`) and role vocabulary (`DIMENSION`, `ATTRIBUTE`, `CLASSIFICATION`,
  `FOREIGN_KEY`, `ENTITY`, `APPROVED`, `DRAFT`).

**STRUCTURAL_VALIDATION_DEPTH: FULLY_CHARACTERIZED.**

```text
FILE VALIDITY           VERIFIED   (Jackcess open)
PACKAGE SHAPE           VERIFIED   (required tables, single package row)
TABLE EXISTENCE         VERIFIED   (metadata tables only)
COLUMN EXISTENCE        VERIFIED   (non-blank on consumed columns)
TYPE COMPATIBILITY      PARTIAL    (closed vocabulary; NOT compared to the contract)
RELATION VALIDITY       PARTIAL    (duplicates only; endpoints NOT resolved)
CONTRACT CORRESPONDENCE NONE
SEMANTIC VALIDITY       NONE
PROVENANCE              NONE
DIGEST/FINGERPRINT      NONE
DATA VALIDITY           NONE       (handled later, in ingestion)
```

**The false confidence is substantial and must be stated plainly in the plan.** A package
passes a rich-looking suite — a dozen typed issue codes, closed vocabularies, duplicate
detection across seven dimensions — that proves the package is **internally well-formed**
and never once asks whether it matches the approved contract it names. An operator reading
"package validation passed" would reasonably conclude correspondence was checked. It was
not. This is the strongest argument in the recovery for DR-007's correspondence rule.

### 23.98 `AccessAuthoringAdapter` — contract-driven, with NO site-specific hardcoding

FACT. A search of the 355-line adapter for `kids`, `cids`, `goal`, `glossary`, `resource`,
`carrier`, `age_group` and `portal` returns **zero matches**.

FACT. Its hardcoded string literals are Access/Jet **reserved words** — `ADD`, `ALTER`,
`AND`, `ANY`, `ASC`, `BETWEEN`, `COLUMN`, `COUNT`, `CREATE`, `CURRENCY`, `DATE`, `DELETE`,
`DESC`, `DISTINCT`, `DROP`, `EXISTS`, `FROM`, … — plus `ACCESS_ACCDB` (artifact profile)
and `GEOSTAT` (namespace).

**Classification of the hardcoding: PROVIDER INVARIANT.** A reserved-word list for
identifier escaping is exactly what belongs in a provider adapter and nowhere else. This is
the correct location for it.

**ACCESS_AUTHORING_ROLE: A — contract-driven physical realizer.** Not a template copier,
not a KIDS generator. **SITE_SPECIFIC_HARDCODING: NONE** on this path.

**Important scope distinction, recorded honestly:** this adapter realizes the **statistical
semantic plan**. The R8 artifact inspected in batch 11 — with its nine `__gs_*` declaration
tables — was produced by the **older Access package generation path**, not by this adapter.
The two generators were not compared in this batch. That comparison is the subject of the
next batch and is why the batch instruction asks for it.

### 23.99 The symmetry matrix — and the asymmetry it exposes

| Property | Generated from | Validated on return against | On mismatch |
|---|---|---|---|
| tables, columns, types, precision/scale, nullability | approved `SemanticPlan` -> `PhysicalPlan` | **package itself only** | nothing |
| keys, relations, cardinality | approved plan | duplicates only | nothing |
| classifiers, dimensions, metrics, units, grain | approved plan + registry | **not validated** | nothing |
| package identity, contract code, contract revision | approved revision | **existence only**, no approval status (23.90) | unknown contract rejected |
| metadata schemas, subjects, assertions | — | **not validated; written INTO canonical state** | CF-033 |
| physical mappings | server `site_contract_dataset` | **server-bound** (the one symmetric axis) | load target resolution fails |
| provider capabilities, profile identity | `ProviderCapabilities` at compile time | **not re-checked** | nothing |
| provenance / digest | **not emitted** (`__gs_package` has no digest) | **not checked** | nothing |

**GENERATION_VALIDATION_SYMMETRY: FAIL.**

The asymmetry is the finding: **the generator derives everything from authority and emits
no proof; the return path validates everything against the package and nothing against
authority.** The single symmetric axis is the physical load mapping, which is server-bound
precisely because the server looks it up itself rather than trusting the file.

### 23.100 Physical pattern model — partially present, and worth naming

Evidence that a pattern architecture already exists in embryo:

| Existing concept | Where | Role |
|---|---|---|
| `artifact_profile` | `__gs_package` (`ACCESS_CANONICAL_R8_PAGE_MANIFEST`) | names the physical realization profile |
| `sourceProfile` / `supportedProfiles` | statistical draft + compiler settings | selects the provider profile |
| `ProviderCapabilities` | compiler, `platform.provider_capability` | declares provider limits |
| `PhysicalPlan` + physical planner | statistical compiler | provider-shaped realization of a semantic plan |
| `PackagePlan.Family` | composer | closed family set (ENTITY, REFERENCE, RELATION, RAW, STATISTICAL, GEO) |
| `PageFamily` | serving dispatch | same families on the read side |
| `AuthoringPolicy.Group` | composer | four package groups derived from family + role |

**PHYSICAL_PATTERN_MODEL: PARTIAL.** Every ingredient of
`CONTRACT + PHYSICAL PATTERN + PROVIDER CAPABILITIES -> REALIZATION` exists, but only the
**statistical** family has an implemented end-to-end pattern. The missing piece is a
*named, declared* physical pattern per family — today the pattern is implicit in Java
(`AccessAuthoringAdapter` for statistical) rather than declared as metadata. Recorded as a
**MISSING CANONICAL ARCHITECTURAL COMPONENT**, not implemented.

**Contract executor principle (instruction 7) — supported by evidence and adopted as
DR-008 (MEDIUM-HIGH):** the contract defines *what must exist and what it means*; a
declared physical pattern defines *how that semantic family is realized for a provider*;
the Access adapter *executes* that plan and may decide only provider-specific
representation within declared capability rules. The evidence supporting it is that the
statistical path already works this way and contains no site logic; the reason confidence
is not HIGH is that only one family has been proven, so the generalization across ENTITY,
REFERENCE, RELATION, RAW and GEO is untested.

**STATISTICAL_REUSABLE_PATTERN: PARTIAL — promising.** The adapter has no site literals and
consumes only the compiled plan, so a second site with different dimensions, metrics,
classifiers, units and grain would exercise the same code path. What is **not** yet proven
is the registry side: CF-016 records that classification scheme/version/item creation
required direct SQL for KIDS, and DR-006 records that the numeric envelope is a Spring
property rather than a provider capability. Both would bite a second site. The **Law of No
Privileged Dimension** is upheld in the adapter itself — no dimension is special-cased —
and INV-006 (measure is not a dimension key component) remains intact.

**Contract differences versus architectural differences (instruction 11): the evidence
supports convergence.** Different site contracts legitimately produce different tables; the
statistical path demonstrates that this requires no architectural difference. The target of
one generation protocol + a small number of declared family patterns + contract-driven
specialization + provider capabilities is **evidence-supported**, with the caveat that
five of six families remain unproven.

### 23.101 DR-007 violations — CF-033 is not the only one

Searching for package declarations that can create, approve, publish, replace, supersede or
reinterpret canonical semantics:

| # | Violation | Verdict |
|---|---|---|
| 1 | `MERGE platform.metadata_schema` from package `schema_json` | **CF-033**, confirmed |
| 2 | `state("READY") -> "APPROVED"` mapping package lifecycle onto canonical lifecycle | **CF-033**, confirmed |
| 3 | `INSERT platform.metadata_subject(... ) VALUES(...,'APPROVED','PUBLIC')` | **NEW, CF-034** — a package *creates* an approved, publicly visible canonical subject |
| 4 | `site_contract_revision` lookup without `status='APPROVED'` | **NEW, CF-035** — a package may bind to a DRAFT / REVIEW_REQUIRED / SUPERSEDED revision |
| 5 | assertion bound to newest schema revision (`ORDER BY s.revision DESC`) | **QF-012**, see below |
| 6 | classifier proposal import (`AccessClassifierProposalImportService`, 53 lines) | **UNRESOLVED** — not read; a package-driven path into the classifier registry is exactly the shape CF-016 describes and must be checked in the next batch |

**DR007_VIOLATIONS: 4 confirmed (CF-033 ×2, CF-034, CF-035), 1 quality defect (QF-012),
1 unexamined candidate.**

### 23.102 PARTIAL_MUTATION_RISK — fully characterized

FACT. `materializeMetadata` performs canonical Control Plane writes in this order, each as
an **independent auto-commit statement with no surrounding transaction**:

```text
1. MERGE platform.metadata_schema        (per schema in the package)
2. INSERT platform.metadata_subject      (per unseen subject) -- 'APPROVED','PUBLIC'
3. MERGE platform.metadata_assertion     (per assertion)
```

- **Which writes can occur:** all three, partially.
- **Transaction boundaries:** none in this method. The data-plane ingest path in the same
  class *does* use `tx.executeWithoutResult(...)`, so the omission is local and visible.
- **What remains after a later failure:** every schema merged before the failure, every
  subject created before it (already `APPROVED`/`PUBLIC`), and every assertion merged
  before it. There is no compensating delete.
- **Retry idempotency:** `MERGE` on schemas and assertions is idempotent by key;
  `INSERT metadata_subject` is guarded by a preceding lookup, so retry is *mostly*
  idempotent — but under concurrency the lookup-then-insert is a race with no unique
  constraint verified in this batch.
- **Cleanup:** none exists.

**PARTIAL_MUTATION_RISK: FULLY_CHARACTERIZED — YES, unbounded within the metadata plane.**

### 23.103 QF-012 — fully characterized, with the required invariant

FACT. `Long schema = controlPlane.query("SELECT TOP 1 s.metadata_schema_id ... WHERE
n.namespace_code=? ORDER BY s.revision DESC", a.namespaceCode())`.

The assertion carries its own `namespaceCode` but the binding **ignores any revision the
assertion was authored against** and takes the newest schema in that namespace.

**Drift mechanism:** ingest package P1 declaring schema revision 1 and assertions conforming
to it; later ingest package P2 declaring revision 2 of the same schema. Every assertion from
P1 is now bound to revision 2 — silently, with no re-validation and no error. Combined with
CF-033 (a package can define the schema), one package can retroactively change the meaning
of another package's assertions.

**Required invariant (recorded, not implemented) — INV-014:** an assertion is bound to the
exact schema revision it was authored and validated against; that binding is immutable for
the life of the assertion. A newer schema revision creates a *new* binding only through an
explicit, governed migration that re-validates the assertions against it. This is the same
principle as INV-004 (exact pinned references) applied to the metadata plane, and its
absence here is why QF-012 is silent where CF-004 is fail-closed.

**QF012: FULLY_CHARACTERIZED.**

### 23.104 Gate fields after batch 11C

```text
ACCESS_AUTHORING_ROLE:          A — contract-driven physical realizer (statistical path)
ACCESS_READER_ROLE:             physical package reader, correctly scoped
GENERATION_LINEAGE:             PARTIAL  (statistical verified; site-contract generator absent)
INGESTION_LINEAGE:              VERIFIED (parse -> internal validation -> server-bound
                                          load mapping -> canonical writes)
GENERATION_VALIDATION_SYMMETRY: FAIL
STRUCTURAL_VALIDATION_DEPTH:    FULLY_CHARACTERIZED
SITE_SPECIFIC_HARDCODING:       NONE (statistical adapter); older package generator not compared
PHYSICAL_PATTERN_MODEL:         PARTIAL  (all ingredients exist; pattern is implicit in Java)
STATISTICAL_REUSABLE_PATTERN:   PARTIAL  (adapter clean; registry + envelope would bite a second site)
DR007_VIOLATIONS:               CF-033 (x2), CF-034, CF-035, QF-012, 1 unexamined
PARTIAL_MUTATION_RISK:          FULLY_CHARACTERIZED
QF012:                          FULLY_CHARACTERIZED
DATA_CONTRACT_LINEAGE_GATE:     FAIL
RECOVERY STATUS:                INCOMPLETE
```

**What changed and what remains.** The batch-11B residual — the reader's validation depth —
is now fully characterized, and it strengthened rather than weakened DR-007. The remaining
gate failure rests on two items that are architecturally material and narrow: the
**classifier proposal import path** (a second possible DR-007 violation, 53 lines, unread)
and the **comparison of the two Access generators** (the statistical adapter versus whatever
produced R8's `__gs_*` tables), which the next batch is explicitly scoped to.

## 23H. Batch 11D — the classifier boundary is correct, and it proves the fix for CF-033

### 23.105 `AccessClassifierProposalImportService` — SAFE PROPOSAL BOUNDARY

FACT, all 53 lines read. Its Javadoc states the rule: "Imports classifier **evidence only**;
promotion into the authoritative registry is **always an explicit review action**." The
implementation honours it exactly:

- it writes **only** to `platform.classifier_proposal` — never to
  `platform.classification_scheme`, `_version`, `_item`, `_alias` or `_hierarchy`;
- every row is inserted with a **hardcoded `state = 'DRAFT'`**;
- the statement is `MERGE ... WHEN NOT MATCHED THEN INSERT` with **no UPDATE branch**, so a
  re-import can never overwrite an existing proposal — idempotent and non-destructive;
- the artifact is opened `setReadOnly(true)`;
- an unresolvable scheme becomes the literal `"UNKNOWN"` rather than an invented code.

**Falsification of DR-007 on this path: FAILED, and that is the correct outcome.** Access
cannot create, approve, activate, publish, supersede or reinterpret a canonical classifier
through this service. Labels, hierarchy, identifiers and aliases all land as `DRAFT`
proposals scoped by `(contract_code, contract_revision, package_batch_id, proposal_type,
subject_key)`.

**CLASSIFIER_PROPOSAL_AUTHORITY: SAFE_PROPOSAL_BOUNDARY.**

### 23.106 Why this is the most useful finding of the batch

This service is **the reference implementation of the pattern CF-033 and CF-034 violate**,
written by the same team, in the same package tree, against the same kind of input:

| Concern | Classifier path (correct) | Metadata path (CF-033/CF-034) |
|---|---|---|
| Target table | dedicated `classifier_proposal` | canonical `metadata_schema` / `metadata_subject` |
| State on import | hardcoded `'DRAFT'` | `state(package value)` -> `'APPROVED'`; subject literal `'APPROVED','PUBLIC'` |
| Overwrite existing | impossible (no UPDATE branch) | `MERGE ... WHEN MATCHED THEN UPDATE` |
| Promotion | explicit review action, by design | none needed — it is already canonical |

**The remediation of CF-033/CF-034 therefore requires no invention.** It is the application
of an existing, correct, in-repository pattern to the metadata plane. This materially
lowers the risk and the design cost of that workstream, and it should be stated in the plan
as reuse rather than as new design.

### 23.107 CF-016 REVISED — the proposal half exists; the promotion half does not

- **OLD CONCLUSION (CF-016):** "binding to a classification version exists but scheme,
  version and item creation required direct SQL for KIDS ... 'open registry' cannot be
  operated safely; manual SQL bypasses approval."
- **NEW EVIDENCE:** (a) the proposal path exists and is correct (23.105); (b) a
  repository-wide search finds **zero** Java writers of `platform.classification_scheme`,
  `_version`, `_item`, `_alias` or `_hierarchy`; (c) `platform.classifier_proposal` is
  referenced by exactly two files — the import service that writes it and the migration
  runner that registers its table. **Nothing reads it.**
- **REVISED CONCLUSION:** CF-016 is not "no governed API". It is sharper and worse:
  **proposals accumulate in `DRAFT` and no promotion path exists to act on them.** The
  canonical classifier registry has **no runtime producer at all**; it can only be
  populated by migration SQL.
- **Relationship to DR-007:** no violation. The boundary is safe; the far side is empty.

**This is a third instance of RC-003.** The missing-producer pattern now covers:

| Canonical registry | Runtime producer |
|---|---|
| `platform.site_contract_*` | **none** (CF-024a) |
| `platform.data_product` | **none** (batch 11) |
| `platform.classification_*` | **none** (CF-016 revised) |
| `platform.metadata_schema` / `_subject` | **an Access package** (CF-033/CF-034) |

Stated plainly: **every canonical registry that should have a governed producer has none,
and the one registry that should not be writable by a transport artifact is.** RC-003's
confidence rises further; its scope is now three registries plus one inversion.

### 23.108 R8 `__gs_*` generator — narrowed, not confirmed

FACT. The Java files that reference `__gs_dataset` / `__gs_field` / `__gs_package` are:

| File | Role |
|---|---|
| `service/catalog/AccessCatalogService` | catalog side — candidate **writer** |
| `service/catalog/ManagedAccessPackageReader` | reader (Managed Access v1) |
| `service/catalog/ManagedDatasetDefinition` | v1 model |
| `service/platform/access/SemanticAccessPackageReader` | reader (Semantic v3) |
| `service/platform/statistical/access/AccessAuthoringAdapter` | statistical generator |

INFERENCE, not yet FACT: the R8 artifact carries the **v3 semantic** declaration set
(`__gs_package/dataset/field/key/relation/projection/page/metadata/metadata_schema`) and
**not** the v1 set (`__gs_chart`, `__gs_chart_filter` are absent from the 24 tables
inventoried in batch 11). The `service/catalog` classes belong to the v1 Managed Access
generation. Therefore the R8 declaration plane was most likely produced by a path that this
batch has **not** positively identified — the candidates are `AccessCatalogService`, the
statistical `AccessAuthoringAdapter`, or an export path not yet read.

**R8_DECLARATION_GENERATOR: PARTIAL.** Narrowed to a candidate set of three; not confirmed.
This is the one item this batch did not close, and it is recorded as such rather than
guessed.

**R8_GENERATION_PROVENANCE: MISSING.** From the artifact itself (batch 11) and from the
code inspected so far: source contract `KIDS_PORTAL_V1` and revision `8` are **asserted**
in `__gs_package`; artifact profile is `ACCESS_CANONICAL_R8_PAGE_MANIFEST`; **generator,
generator version, package plan, physical plan, provider capabilities and digest are all
absent from the artifact and are not emitted by any inspected path.**

### 23.109 Gate fields after batch 11D

```text
CLASSIFIER_PROPOSAL_AUTHORITY: SAFE_PROPOSAL_BOUNDARY
CF016:                         REVISED — proposal half correct; promotion half absent;
                               registry has no runtime producer (third RC-003 instance)
R8_DECLARATION_GENERATOR:      PARTIAL — narrowed to three candidates
R8_GENERATION_PROVENANCE:      MISSING
GS_DECLARATION_SOURCE:         UNKNOWN pending the generator identification
PHYSICAL_PATTERN_COMPONENTS:   PARTIAL
DR008:                         CONFIRMED, MEDIUM-HIGH (unchanged; only one family proven)
CURRENT_AUTOMATION_BASELINE:   PARTIAL
CURRENT_MANUAL_INPUT_BASELINE: PARTIAL
ACCESS_AUTHORITY_GRAPH:        PARTIAL
ACCESS_RECOVERY:               INCOMPLETE
DATA_CONTRACT_LINEAGE_GATE:    FAIL
RECOVERY STATUS:               INCOMPLETE
```

**Closure test (instruction H), answered honestly.** One remaining unknown *can* materially
change the old-versus-new comparison and the canonical Access target: **which component
produced the R8 declaration plane, and whether that path is contract-derived or
template/manual.** Until that is known, `GS_DECLARATION_SOURCE` cannot be classified, the
automation and manual-input baselines cannot be completed, and the planned
OLD ↔ R8 ↔ candidate ↔ canonical comparison would rest on an assumption. That is unknown
architecture, not a known defect, so recovery stays INCOMPLETE — by the standard this
engagement has applied throughout.

**Preserved unchanged:** DR-006, DR-007, DR-008, INV-013, INV-014, CF-033, CF-034, CF-035,
QF-012, RC-003, AMS-001 and every earlier finding. CF-016 is the only item revised, and its
revision is recorded above with old conclusion, new evidence and revised conclusion.

## 23I. Batch 11E — the R8 declaration producer identified, and the metadata plane isolated

### 23.110 `AccessCatalogService` ELIMINATED as the producer — by discriminating evidence

FACT. `AccessCatalogService` references exactly four `__gs_*` tables: `__gs_package`,
`__gs_dataset`, **`__gs_chart`** and **`__gs_chart_filter`** — the **Managed Access v1**
set. FACT. It contains **zero** occurrences of `TableBuilder`, `createTable`, `newTable`
or `addRow`: it does not create Access structures at all.

FACT. The R8 artifact inventoried in batch 11 contains **no** `__gs_chart` and no
`__gs_chart_filter`, and **does** contain `__gs_field`, `__gs_key`, `__gs_relation`,
`__gs_projection`, `__gs_page`, `__gs_metadata` and `__gs_metadata_schema`.

**Eliminated on table-set evidence, not on names**, exactly as the batch instruction
requires. `ACCESS_CATALOG_SERVICE_ROLE: Managed Access v1 catalog reader — not a
generator.`

### 23.111 PRODUCER IDENTIFIED — `AccessAuthoringAdapter`

FACT. A behavioural search for Access table-creation APIs across all of `src/main/java`
returns **exactly two** files using `ColumnBuilder` / `IndexBuilder`:

| File | Relevance |
|---|---|
| `controller/MSSQLToAccess` | the legacy DB export of CF-026 — emits no `__gs_*` |
| **`service/platform/statistical/access/AccessAuthoringAdapter`** | emits the declaration plane |

FACT. `AccessAuthoringAdapter` emits exactly these prefixed tables:

```text
__gs_package  __gs_dataset  __gs_field  __gs_key
__gs_relation __gs_projection __gs_page   __stat_contract
```

**Seven of the nine `__gs_*` tables in the R8 artifact are produced by this adapter**, and
the set matches the v3 semantic shape the R8 file carries. Combined with batch 11C's
finding that this adapter contains **no site-specific literal** and consumes only the
compiled plan, the central classification follows:

**R8_DECLARATION_SOURCE: CONTRACT_DERIVED_WITH_MANUAL_SUPPLEMENTS** — see 23.112 for the
supplement.

**R8_DECLARATION_GENERATOR: VERIFIED for the core declaration plane.**

**GENERATOR AUTHORITY: EXECUTOR.** It reads a derived `SemanticPlan` -> `PhysicalPlan`,
holds no independent semantic definitions, carries no site hardcoding, and copies no
template containing semantics. It executes a plan; it does not decide meaning. This
confirms DR-008's contract-executor principle on the one family where it is implemented.

### 23.112 The metadata plane has NO identified producer — and it is the dangerous one

FACT. Two tables present in the R8 artifact are **absent from the adapter's emitted set**:

| Table | Rows in R8 | Emitted by the adapter? |
|---|---|---|
| `__gs_metadata` | 267 | **no** |
| `__gs_metadata_schema` | 2 | **no** |

FACT, the converse: the adapter emits `__stat_contract`, which is **absent** from the R8
artifact's 24 tables.

Two conclusions follow, and they converge with everything batches 11B-11D established:

1. **The R8 artifact was produced by a different — almost certainly earlier — version of
   this generator**, or by it plus a separate step. The `__stat_contract` mismatch is the
   proof: the current adapter emits a table the artifact does not have.
2. **The metadata plane of the Access package is exactly the part with no identified
   producer** — and it is precisely the plane that
   - is **trusted as canonical authority on ingestion** (CF-033: `MERGE
     platform.metadata_schema` from package `schema_json`),
   - **creates approved, public canonical subjects** (CF-034),
   - and carries the `approval_state = READY` value that `state()` converts into canonical
     `'APPROVED'`.

**The one part of the package whose origin cannot be traced is the one part the platform
trusts most.** That is the sharpest statement of the Access trust problem this recovery has
produced, and it should open the Access section of the rehabilitation plan.

### 23.113 Generator versioning — MISSING, and now provably consequential

FACT. `__gs_package` carries `product_code`, `contract_code`, `contract_revision`,
`package_code`, `package_version`, `load_mode`, `source_system`, `artifact_profile` — and
**no generator identity, no generator version, no plan digest, no build metadata**
(batch 11, confirmed by full column list).

An artifact therefore **cannot answer "which generator logic created me"**. The
`__stat_contract` mismatch above is the concrete cost: the recovery can *detect* that R8
was built by a different generator generation, but cannot *identify* which, and neither can
the platform at ingestion time.

**R8_GENERATOR_VERSIONING: MISSING.** Recorded as a provenance deficiency; no versioning
mechanism is invented here.

### 23.114 v1 versus v3 — vocabulary resolved

| Term | Meaning, from evidence |
|---|---|
| **v1 (Managed Access Package)** | `__gs_package`, `__gs_dataset`, `__gs_chart`, `__gs_chart_filter`. Read by `service/catalog/*`. Imports into the older core JPA profile/mapping tables. Generation: **legacy**. |
| **v3 (Semantic Access Package)** | `__gs_package`, `__gs_dataset`, `__gs_field`, `__gs_key`, `__gs_relation`, `__gs_projection`, `__gs_page` (+ metadata plane). Read by `service/platform/access/SemanticAccessPackageReader`. Written by `AccessAuthoringAdapter`. Generation: **current**. |

These are **real, distinct profile generations**, not misleading naming: they differ in
table set, reader, writer and downstream target. **The R8 artifact is v3.** No v1 -> v3
upgrade path was found in code; the two generations coexist with separate readers, which is
PA-001's residue as already recorded.

### 23.115 Provenance chain — edge by edge

```text
AUTHORITATIVE CONTRACT REVISION        VERIFIED   (statistical contract, ContractWorkflow)
  -> SemanticPlan                      VERIFIED   (digest-checked, 23.64)
  -> PhysicalPlan                      VERIFIED   (statistical physical planner)
  -> physical pattern / profile        INFERRED   (implicit in Java, not declared — 23.100)
  -> ProviderCapabilities              VERIFIED   (compiler input)
  -> GENERATOR                         VERIFIED   (AccessAuthoringAdapter, this batch)
  -> __gs_* declaration plane          VERIFIED for 7 of 9 tables
  -> __gs_metadata / _metadata_schema  KNOWN GAP  (no identified producer)
  -> data structures                   VERIFIED   (typed tables, codelist data)
  -> R8 ARTIFACT                       PARTIAL    (built by a different generator version)
  -> generator identity in artifact    MISSING
  -> digest binding artifact->contract MISSING    (batch 11B)
```

No INFERRED edge has been promoted to VERIFIED.

### 23.116 RC-003 — UNCHANGED, and the reason matters

Instruction 15 asks whether RC-003 narrows now that a real generator is confirmed. **It
does not, and conflating the two would be an error.**

RC-003 is about the absence of producers for **canonical Control Plane registries** — site
contract, `data_product`, `classification_*`. `AccessAuthoringAdapter` is **downstream** of
those: it derives an artifact *from* an approved declaration. Finding a good downstream
generator says nothing about the missing upstream authoring layer. The two concerns remain
separate, exactly as instruction 15 requires.

**RC003: CONFIRMED, HIGH confidence, scope unchanged** (three registries without producers,
one registry writable by a transport artifact).

If anything the Access finding *reinforces* RC-003's shape: the platform builds
high-quality derivation machinery downstream of an authority layer it never built.

### 23.117 The two proven patterns — recorded, and deliberately not merged

Batch 11D recovered one correct pattern; batch 11E confirms a second. They solve different
problems and the plan must choose per family rather than applying one everywhere:

| Pattern | Proven by | Use when |
|---|---|---|
| **Deterministic generation from authority** | `AccessAuthoringAdapter` (this batch) | the content is fully derivable from an approved declaration |
| **External evidence -> DRAFT proposal -> governed promotion -> canonical registry** | `AccessClassifierProposalImportService` (23.105) | the content originates outside the platform and requires human judgement |

**CF-033/CF-034 are a case of the wrong pattern being applied**: metadata schemas and
subjects arrive from outside and are written straight into canonical state, when they
belong in the proposal pattern.

### 23.118 Closure fields

```text
ACCESS_CATALOG_SERVICE_ROLE:  Managed Access v1 catalog reader; not a generator
R8_PROFILE_VERSION:           v3 Semantic Access Package
R8_DECLARATION_GENERATOR:     VERIFIED (AccessAuthoringAdapter) for 7 of 9 __gs_* tables
R8_DECLARATION_SOURCE:        CONTRACT_DERIVED_WITH_MANUAL_SUPPLEMENTS
R8_GENERATOR_VERSIONING:      MISSING
R8_GENERATION_PROVENANCE:     KNOWN_GAP — metadata plane producer unidentified;
                              artifact built by a different generator version
CONTRACT_TO_R8_LINEAGE:       PARTIAL — verified to the declaration plane; known gap at
                              the metadata plane; no digest binds artifact to contract
RC003:                        CONFIRMED, HIGH, scope unchanged
ACCESS_RECOVERY:              COMPLETE
DATA_CONTRACT_LINEAGE_GATE:   FAIL
RECOVERY STATUS:              INCOMPLETE
```

**Why `ACCESS_RECOVERY: COMPLETE` despite the gap.** Applying instruction 18's rule: the
producer question has been answered by exhaustive behavioural search — only two classes in
the entire codebase can create Access tables, one is the legacy export, the other is the
identified generator. The residual metadata-plane origin is a **KNOWN PROVENANCE GAP**, not
unexplained architecture: its responsibility, its consumers, its trust treatment and its
defects (CF-033, CF-034) are all fully characterized. Further searching for a producer that
the evidence indicates is absent would be the indefinite search instruction 13 warns
against.

**Why the overall gate still fails.** `DATA_CONTRACT_LINEAGE_GATE` remains FAIL on items
already recorded and *not* on Access: the upper stack is absent (RC-003), the information-
loss matrix is incomplete on non-numeric axes, and the two protection strategies are
undrafted. Those are the remaining recovery obligations, and none of them is an unknown
authority boundary.

**Note on the sample ZIP (instruction 5).** `samples/kids-r8-resource-package-8.0.1.zip`
was **not** inspected in this batch. The producer question was resolved from code before it
became necessary, and context budget was reserved for checkpoint integrity as strict mode
requires. It remains available evidence for the comparison batch and may carry a manifest
with generator identity — which would be the one thing that could upgrade
`R8_GENERATION_PROVENANCE` from KNOWN_GAP.

## 24. Active continuation protocol — batch 12

Supersedes every earlier protocol section. Sections 23.16, 23.25, 23.35 and 23.42 are
retained only for provenance.

1. Read this checkpoint, then in order: 23.9 (RC-001), 23.15 (the P0 exposure path),
   23.17 and 23.31 (revisions of earlier statements), 23.37 (test coverage of every
   conflict), 23.43-23.51 (decision authority and the chart decision). These carry the
   newest and highest-risk conclusions and the first binding architectural decisions.
2. Do **not** re-read the classes listed in 23.3, 23.13, 23.18, 23.26, 23.38 and 23.44;
   do not re-measure the baseline; do not reopen `STATUS_NORMAL = "VALID"` (23.13); do not
   re-derive test coverage of the nine conflicts (23.37); do not reopen the chart
   canonicality decision (23.49) unless new repository evidence contradicts it.
3. Continue with the earliest architecturally significant incomplete area, in this order:
   a. **Package/contract taxonomy** (PA-001, PA-003, unresolved question 6) — the
      untracked `service/platform/packaging/*` (`PackagePlan`, `PackageContractSource`,
      `JdbcPackageContractSource`, `AuthoringPolicy`) against the Access semantic
      declaration, the artifact manifest and the statistical authoring adapter. This is
      the largest remaining parallel architecture and it is where CF-024's "no governed
      producer" finding may find its intended answer.
   b. **Grammar duality** (CF-009) — the Java parser/compiler versus the untracked
      `src/main/resources/contracts/statistical-contract-draft.schema.json`. Decide which
      generates which.
   c. Migration-chain rebuild audit (CF-012, CF-020, CF-024) including every excluded and
      unregistered file.
   d. Remaining ~65 unclassified test files; `frontend/kids`' use of `/platform/contracts/`
      and `/platform/operations/exports`; `frontend/web` `[LEGACY]`; `backend/mobile`.
4. Keep writing findings into this file after each batch. Do not begin the master plan
   until the completeness gate in 23.51 is genuinely satisfied. RC-001 remains the
   decision that must be settled before chart, export or attribute convergence is
   scheduled, and CF-031 remains a hard precondition of the CF-008 correction.

*(Batch 7 completed step 3a. Result: sections 23.52-23.61. The composer is a correct,
unwired **derivation** port, not a producer; CF-024 is confirmed and split into CF-024a /
CF-024b; PA-001 is reduced to its genuine residue, CF-006.)*

**Revised order for batch 8.** CF-006 is promoted ahead of the grammar and migration
items, because it is now the largest remaining architectural unknown: CF-024a cannot be
answered — and therefore SCH-003 cannot be planned — without first deciding whether the
site contract is the single M1 declaration layer or one of two peers.

1. **CF-006 — statistical contract versus site contract ownership.** Read
   `ContractWorkflow` and `StatisticalContractController` to EOF as final dirty sources,
   together with `StatisticalContractConfiguration` (which wires `ChartService` and is
   therefore the place any composer/statistical bridge would appear). Establish whether
   the statistical contract is (a) a child of a site-contract revision, (b) an independent
   dataset contract the site contract references, or (c) a replacement for part of the
   site contract. Batch 3 proved there is no table-level coupling; batch 8 must establish
   whether there is an intended one.
2. Grammar duality (CF-009) — the Java parser/compiler versus the untracked
   `src/main/resources/contracts/statistical-contract-draft.schema.json`.
3. Migration-chain rebuild audit (CF-012, CF-020).
4. Remaining ~62 unclassified test files; `frontend/kids`' use of `/platform/contracts/`
   and `/platform/operations/exports`; `frontend/web` `[LEGACY]`; `backend/mobile`.

*(Batch 8 completed this step. Result: sections 23.62-23.70. CF-006 resolved as LAYERED
REPRESENTATION with a missing link (DR-004); CF-004 narrowed to availability; INV-013
confirmed honoured; CF-008 remediation refined; NEW CF-032 shows governed publication is
structurally closed to statistical datasets, which reframes CF-023.)*

**Order for batch 9.** The remaining source-level unknowns (CF-009 grammar duality, the
migration-chain audit) are now smaller than the accumulated synthesis debt. Sections
23.39, 23.66 and 23.69 have produced the raw material for gate questions 16-21, which are
the largest unsatisfied part of the completeness gate. Batch 9 should therefore close the
two remaining source unknowns **and** begin consolidating the registers, so that batch 10
can be the Master Plan if the gate is then satisfied.

1. **CF-009 grammar duality** — the Java parser/compiler versus the untracked
   `src/main/resources/contracts/statistical-contract-draft.schema.json`. Determine which
   generates which, whether they can accept different documents, and whether the schema is
   wired into any runtime or CI path or is documentation only. Decide the canonical
   direction under decision authority.
2. **Unresolved question 23** — read `SemanticPlan.revisionDigest()` and the canonical
   digest implementation to determine the exact input set, which decides whether CF-004's
   residual drift risk is real.
3. **Migration-chain rebuild audit** (CF-012, CF-020) — the registered-versus-present file
   set, the checksum-rewrite exception list, and whether a clean rebuild is possible.
4. **Begin register consolidation** — the contradiction, invariant, decision, duplication
   and legacy registers exist as prose across sections 8-23; consolidate them into the
   final register form the Master Plan requires, without yet writing the plan.

*(Batch 9 completed steps 1-4. Result: sections 23.71-23.76. CF-009 resolved with DR-005;
CF-004 residual semantic risk CLOSED with a declared inference gap; migration chain audited
— clean rebuild and production do **not** converge, yielding RC-002; AMS-001 recorded as an
atomic migration set after three falsification attempts; the canonical line's twelve breaks
reduced to four root causes.)*

**Batch 10 — the last material unknown, then the protection strategies.**

1. **CF-003 numeric envelope — the only remaining unknown that can still change the
   canonical data model.** Read `StatisticalContractCompiler.NumericEnvelope` and its
   configuration properties, `AccessAuthoringAdapter`'s numeric handling, migrations 111
   and 112, the Access integrity ADR D-3, and Q34 in the decision register. Under decision
   authority determine the single canonical envelope across semantic definition, Access
   provider, SQL storage and API serialisation, and the compatibility rule for contracts
   already approved at `28,10`. Record as DR-006.
2. **Draft the regression protection strategy** from 23.37 and 23.39: for each of the 14
   unprotected REQUIRED behaviors, the protection method, the order relative to its
   correction, and the one test that must be rewritten (`StatisticalLoadServiceTest:151`).
3. **Draft the quality protection strategy and the drift fitness functions** — what the
   existing `engineering-governance.py` gate can enforce, and what new architecture tests
   would prevent the recurrence of RC-001, RC-002, CF-009 parity and CF-025-class dead
   state vocabulary.
4. Optional, non-blocking: `semanticForm`/`physicalForm` field lists (23.72 gap);
   `frontend/web` and `backend/mobile` consumers; remaining test files.

*(Batch 10 completed workstream A and the upper-layer discovery of workstream B. Result:
sections 23.77-23.81. CF-003 RESOLVED with DR-006. The missing upper stack was recovered
from the repository's own vocabulary — `Platform Meta-Contract` and `Control Plane Schema`
— and proven to exist only as documentation, yielding RC-003.
`DATA_CONTRACT_LINEAGE_GATE: FAIL` for a material architectural reason.)*

**Batch 11 — finish the lineage audit, then the protection strategies.**

The upper-layer question is answered. What remains is the *lower* half of workstream B,
which batch 10 did not reach, plus the two protection strategies.

1. **`SITE_CONTRACT_TO_ACCESS` and `ACCESS_PHYSICAL_MODEL`.** Inspect an actual `.accdb`
   artifact (read-only; `samples/*.accdb` and the R8 package referenced in
   `docs/work/ACCESS-PACKAGE-*`). Classify every significant Access structure as contract
   representation, empty contract-defined structure, data table, metadata, control
   metadata, transport metadata, generated structure, execution state or legacy. Determine
   who creates each and from which declaration. Read `AccessAuthoringAdapter` and the
   statistical physical planner to EOF.
2. **`BACKWARD_LINEAGE`.** Trace one representative KIDS value end to end in reverse:
   API response value -> serving query -> publication membership/status -> snapshot ->
   `statistics.observation` row -> `raw.source_record` payload -> Access column -> Access
   table -> site-contract field declaration -> statistical contract component ->
   (missing) Control Plane Schema. Record every step that cannot be explained.
3. **`INFORMATION_LOSS_AUDIT`.** Complete the per-transformation audit for the eight
   transformations named in the batch-10 instruction, using the numeric axis (DR-006) as
   the worked example and the already-catalogued losses (CF-005, CF-027, CF-028) as
   inputs.
4. **Close the 23.72 inference gap:** read `semanticForm()` and `physicalForm()` line by
   line and either upgrade the CF-004 closure to FACT or retain the stated uncertainty.
5. **Draft the two protection strategies** (regression/behavioral and
   architectural/canonicality), including fitness functions that would prevent recurrence
   of RC-001, RC-003, CF-009 grammar parity, CF-025-class dead state vocabulary, and
   derived artifacts becoming authorities (INV-013).

*(Batch 11 completed this step and more. Result: sections 23.82-23.88.
`ACCESS_PHYSICAL_MODEL: VERIFIED` from the physical file; the contract-tables-plus-data
intent CONFIRMED; `__gs_metadata_schema` rejected as the upper layer on physical evidence;
RC-003 confirmed by behavior-based census and strengthened — `platform.data_product` has no
producer either.)*

**Batch 12 — the last material unknown, then the protection strategies.**

One material unknown blocks the lineage gate, and it is an authority question rather than
an absence:

1. **The Access return-path authority question (23.84).** Read
   `PlatformAccessIngestionService` and `SemanticAccessPackageReader` to EOF. Determine
   whether the server, on ingesting an Access package, **re-validates the file's `__gs_*`
   declarations against the approved `site_contract_revision`** named in `__gs_package`
   (`KIDS_PORTAL_V1` r8), or whether it **reconstructs semantics from the file's own
   declarations**. If the latter, a physical artifact acts as semantic authority — an
   authority inversion that changes the canonical architecture and the legacy-elimination
   plan, and it must be recorded as a new conflict with an ordering constraint. Note that
   `__gs_package` carries **no digest**, so any re-validation must be by content, not by
   checksum.
2. **`AccessAuthoringAdapter` to EOF** — the generator half of the statistical path: which
   tables it creates empty, which it fills, which physical types it selects, how relations
   and codelists are materialised, and what it does **not** represent.
3. **Close the 23.72 inference gap** — read `semanticForm()` and `physicalForm()` line by
   line; upgrade the CF-004 closure to FACT or retain the stated uncertainty.
4. **Draft the two protection strategies** (regression/behavioral and
   architectural/canonicality) with fitness functions that would prevent recurrence of
   RC-001, RC-003, CF-009 grammar parity, CF-025-class dead state vocabulary, and derived
   artifacts becoming authorities (INV-013).

*(Batch 11B answered this question. Result: sections 23.89-23.95. **MODEL C — HYBRID**:
data/structural mapping is server-bound; the metadata plane is access-authoritative.
NEW CF-033 and DR-007; `PACKAGE_TO_CONTRACT_BINDING: MISSING`;
`PARTIAL_MUTATION_RISK: YES`.)*

**Batch 12 — close the last two narrow items, then the protection strategies.**

1. **`SemanticAccessPackageReader` to EOF** — quantify the structural validation depth so
   the tamper posture in 23.92 can be stated correctly: does the reader validate the
   package's `__gs_dataset` / `__gs_field` / `__gs_key` / `__gs_relation` against anything,
   or does it only parse? This does not change DR-007; it determines what the plan must say
   about the current system's resistance to a tampered package.
2. **`AccessAuthoringAdapter` to EOF** — the generator half: which tables are created empty,
   which are filled, which physical types are selected, how relations and codelists are
   materialised, and what is **not** represented. Completes the `SITE_CONTRACT -> ACCESS`
   information-loss row.
3. **Complete the information-loss matrix** for the eight transformations, using DR-006
   (numeric) and 23.84/23.90 (provenance and authority) as worked rows.
4. **Close the 23.72 inference gap** — `semanticForm()` / `physicalForm()` line by line.
5. **Draft the two protection strategies** with fitness functions against RC-001, RC-003,
   CF-009 parity, CF-025-class dead vocabulary, CF-033 authority inversion, and INV-013.

*(Batch 11C completed this step. Result: sections 23.96-23.104. The reader is a pure
parser; validation is entirely package-internal; the statistical generator has **no**
site-specific hardcoding; `GENERATION_VALIDATION_SYMMETRY: FAIL`. NEW CF-034, CF-035,
INV-014, DR-008.)*

**Batch 12 — two narrow items, then the comparison batch can proceed.**

1. **`AccessClassifierProposalImportService` (53 lines) to EOF** — the last unexamined
   candidate DR-007 violation. Determine whether a package can create or approve classifier
   scheme/version/item rows in the canonical registry. Given CF-016 (classifier growth
   required direct SQL) and CF-033 (a package can define metadata schemas), this path is
   the most likely place for a third authority inversion.
2. **Compare the two Access generators** — `AccessAuthoringAdapter` (statistical, clean)
   against whatever produced the R8 artifact's nine `__gs_*` declaration tables. Identify
   the older generator, determine whether it is contract-driven or template-driven, and
   whether it carries site-specific hardcoding. This is the prerequisite evidence for the
   planned OLD ACCESS vs R8 vs candidate_2 vs CANONICAL TARGET comparison.
3. Then, and only then, the comparison batch.

Deferred, non-blocking (they cannot change a canonical decision):
`semanticForm()`/`physicalForm()` field lists (23.72 gap); the remaining information-loss
rows; the two protection strategies; ~62 unclassified test files; `frontend/web` and
`backend/mobile` consumers.

*(Batch 11D completed item 1. Result: sections 23.105-23.109. The classifier boundary is
**safe** and is the reference implementation for fixing CF-033/CF-034; CF-016 REVISED —
the promotion half does not exist and the classifier registry is a third RC-003 instance.
Item 2, the R8 declaration generator, is narrowed to three candidates but not confirmed.)*

**Batch 12 — one item, then the Access recovery closes.**

*(Batch 11E resolved this. Result: sections 23.110-23.118. `AccessCatalogService`
eliminated on table-set evidence; **`AccessAuthoringAdapter` VERIFIED** as the producer of
7 of 9 `__gs_*` tables; the metadata plane isolated as a **KNOWN PROVENANCE GAP** and shown
to be exactly the plane CF-033/CF-034 trust; generator versioning MISSING; v1/v3 vocabulary
resolved. **ACCESS_RECOVERY: COMPLETE.** RC-003 unchanged.)*

**Batch 12 — the remaining recovery obligations. None is an unknown authority boundary.**

Access recovery is closed. What remains are the items recorded as outstanding since
batch 9, in the order of their value to the plan:

1. **Draft the two protection strategies** — regression/behavioral and
   architectural/canonicality — from the raw material in 23.37 (test coverage of every
   conflict), 23.39 (the 14 unprotected REQUIRED behaviors) and 23.99 (the symmetry
   failure). Include fitness functions against RC-001, RC-003, CF-009 grammar parity,
   CF-025-class dead vocabulary, CF-033/CF-034 authority inversion, INV-013 and INV-014.
   This closes completeness-gate questions 19-21, the largest unsatisfied block.
2. **Complete the information-loss matrix** for the eight transformations, using DR-006
   (numeric), 23.84 and 23.90 (provenance and authority) as the worked rows.
3. **Close the 23.72 inference gap** — `semanticForm()` / `physicalForm()` line by line;
   upgrade the CF-004 closure to FACT or retain the stated uncertainty.
4. Optional, non-blocking: the sample ZIP manifest (could upgrade
   `R8_GENERATION_PROVENANCE` from KNOWN_GAP); ~62 unclassified test files;
   `frontend/web` and `backend/mobile` consumers.

After (1)-(3) the completeness gate can be re-evaluated honestly. The remaining
`DATA_CONTRACT_LINEAGE_GATE` failure is attributable to the **proven absence** of the upper
stack (RC-003), which instruction 13 of batch 11B explicitly classifies as compatible with
eventual recovery completion.

RECOVERY STATUS: INCOMPLETE  
NEXT STARTING POINT: draft the **regression/behavioral protection strategy** from
sections 23.37 and 23.39 — for each of the 14 unprotected REQUIRED behaviors, state the
protection method (characterization, contract, integration, property, negative, recovery
or architecture test), its position relative to the corresponding correction, and the
single existing test that must be rewritten (`StatisticalLoadServiceTest:151`). Then draft
the **architectural/canonicality protection strategy** with the fitness functions listed
above. Both are planning artifacts written into this checkpoint; neither is implemented.
