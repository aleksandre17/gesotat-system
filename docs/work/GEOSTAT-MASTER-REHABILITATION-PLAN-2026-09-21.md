---
id: PLAN-MASTER
type: REGISTER
title: Master rehabilitation plan
status: COMPLETE
authority: CANONICAL
scope: how the whole existing system converges to the accepted canonical architecture — responsibilities, dispositions, ordering, protection, elimination and gates
owner: PHASE-005
created: 2026-09-21
updated: 2026-09-21
related: ARCH-CANONICAL, ARCH-BASELINE, ARCH-DOSSIER, AUD-CAPABILITY, STD-BENCH-001, DOC-CAD, REC-CONSOLIDATION, ADR-014, ADR-015, ADR-016
---

# GEOSTAT — MASTER REHABILITATION PLAN

> **`PHASE-005`. Convergence, not construction.** The question this plan answers is not *how do
> we implement `ARCH-CANONICAL`* — it is *how does the existing system arrive at one canonical
> authority per responsibility without losing capability, semantics, integrity, history,
> security or operability.*
>
> **`MIGRATED ≠ DONE`.** A responsibility is rehabilitated only when it is migrated **and** the
> old authority is eliminated or explicitly bounded, **and** unique capability is preserved or
> deliberately retired, **and** references, runtime path, tests and documentation have converged,
> **and** that convergence is verified.

**Owns:** the responsibility inventory, the parallel-authority register, every disposition, the
target convergence map, the ordered execution roadmap, the elimination register, the protection
plan, the decision register and the `PHASE-006` entry contract.
**Does not own:** the target architecture (`ARCH-CANONICAL`), the physical baseline
(`ARCH-BASELINE`, `ARCH-DOSSIER`), capability dispositions (`AUD-CAPABILITY`), quality criteria
(`STD-BENCH-001`), statistical domain decisions (`DOM-STATISTICAL`), or any execution package
(`PHASE-006`).

**This plan implements nothing.** No code was written, no migration executed, no database
modified, no legacy removed, no Access work performed, no UI designed.

> **On this artifact's declared type.** It is registered as `REGISTER`, not `ARCHITECTURE`, and
> not under a new `PLAN` type. `RCP` §5 vocabularies are closed and adding a value is a governed
> change to the standard; extending the set to admit this document would have loosened a check in
> order to pass it. `REGISTER` is also the accurate word — this artifact's authority *is* its
> registers (responsibility, parallel authority, elimination, decision, protection) plus the
> ordering derived from them. **`ARCHITECTURE` stays exclusively `ARCH-CANONICAL`'s**, so there is
> no question where architecture is decided.

---

## 0. Scope boundary — binding, and not recursively expandable

**Authoritative user scope clarification, 2026-09-21.** This section governs every other section
of this plan and every downstream phase. Where any other text in this repository implies a wider
rehabilitation scope, **this section wins.**

| Role | Path | Status |
|---|---|---|
| **PRIMARY TARGET** | `platform/apps/geostat/backend/api` | in scope |
| **CONDITIONAL SUPPORTING** | `platform/apps/geostat/backend/core` | in scope **only** where the API Platform genuinely depends on it, or a responsibility is demonstrably shared/core |
| **PARKED — Admin/Authoring surface** | `platform/apps/geostat/frontend/geostat-system-app` | designated, belongs to `41627` (`W-24`); not executed |
| **PARKED — designated first consumer** | `platform/apps/geostat/frontend/kids` | designated; **no recursive expansion from this location** |

**Explicitly OUT OF SCOPE** — `platform/apps/geostat/backend/mobile` · any other `mobile`
project · any `web` project · any other neighbouring or nested application not explicitly
designated above. This exclusion covers their **source, internal architecture, contracts,
databases, schemas, tables, migrations, documentation and domain requirements**.

> **Repository proximity does not imply scope.** A directory being inside this repository, or
> being reachable from an in-scope module, is **not** a reason to inspect, query, connect to,
> inventory, rehabilitate, migrate, compare against, or derive canonical platform requirements
> from it. Location is not authority — the same rule `DOC-CAD` applies to documents applies to
> applications.

**The single permitted exception, and its exact limit.** Where an out-of-scope module creates a
**build, runtime, deployment or governance dependency that can affect the in-scope API Platform**,
that *boundary* may be established — and nothing beyond it. Evidence gathering stops at the edge:
the dependency is characterised, the **API-side** isolation requirement is derived, and the
out-of-scope module's internals, semantics and data are **never** used as canonical evidence.
`§4` is the one place this exception was exercised.

**Instruction to future agents:** if you find yourself reading an out-of-scope module's business
logic, querying its database, or reasoning about what its data *means*, you have left this plan's
scope. Stop, and record a boundary question instead.

---

## 1. Bootstrap, lifecycle verification and discrepancies

Authority was read in order: `/AGENTS.md` → `CURRENT` → `MANIFEST` → `DOC-CAD` → the phase
cards → `ARCH-CANONICAL` (Part II governs) → `ARCH-BASELINE` → `ARCH-DOSSIER` → the
`canonical-design` gate evidence → `STD-BENCH-001` → `STD-AUTO-001` → `AUD-COMPARATIVE` →
`AUD-CAPABILITY` → `DOM-STATISTICAL` `Q01…Q50` → `ADR-014/015/016` → `docs/platform-decisions.md`
→ `REC-CONSOLIDATION`.

**Lifecycle state confirmed against the repository, not against the prompt:**

| Claim | Repository | Verdict |
|---|---|---|
| `PHASE-001…004` COMPLETE, design entry COMPLETE | `MANIFEST` lifecycle table | ✔ agrees |
| `PHASE-004` ACCEPTED and committed at `50d143f` | `git log 50d143f`; 94 files; `CURRENT`/`MANIFEST`/`CATALOG` all inside that commit | ✔ agrees |
| `PHASE-005` is the next permitted action, gated by `GATE-PLAN` | `MANIFEST` line 30, gate table line 273 — `GATE-PLAN` `READY` | ✔ agrees |

**Two discrepancies found and resolved here, neither architectural:**

1. **`CURRENT` BASELINE row was stale.** It still read `HEAD b46c043 · target 26 modified /
   29 untracked`, describing the tree *before* the `PHASE-004` preservation commit. Corrected to
   the committed baseline. A current-state authority that names a superseded HEAD is a defect
   under RCP §5 regardless of how small.
2. **The prompt's plane list is a floor, and it was short by one.** §4 of the commissioning
   instruction enumerates application, database, logical, contract, operational, knowledge,
   provider and test planes. It does not name **separately deployed sibling applications**. The
   repository contains one (§4 below), and it is the single most consequential discovery of this
   phase. Recorded rather than silently folded into "application".

---

## 2. Method

Responsibilities first, files last — `§5` of the instruction, and the only order that survives
contact with this repository, where **two files with no shared name encode the same authority**
and **one deployed application shares no code at all with the canonical plane**.

1. Reconstruct the responsibility graph from the committed tree (`50d143f`), never from the
   dirty working tree.
2. For each responsibility record: what it is · why it exists · current implementation(s) ·
   current authority source(s) · canonical target owner · unique capability · invariant ·
   runtime consumers · dependencies · migration requirement · elimination condition.
3. Only then classify artifacts.
4. Attack the result before accepting it.

**Evidence base.** 787 committed Java files across three source trees (`api`, `core`, `mobile`),
110 committed platform migrations, the 139-table physical model of `ARCH-BASELINE`, the database
universe of `ARCH-DOSSIER`, and the live SQL Server 2022 catalogue read read-only.

**Scope discipline.** The working tree also carries an unrelated platform security-hardening
change set in flight. It was read for dependency awareness and **never treated as authority**;
every inventory figure in this plan comes from `git` at `50d143f`.

---

## 3. Responsibility inventory — 28 families

`RF` = responsibility family. *Current authority* names what decides the meaning **today**;
*canonical owner* names what must decide it after rehabilitation.

| ID | Responsibility | Current authority (evidence) | Canonical owner | State |
|---|---|---|---|---|
| `RF-01` | Semantic declaration — what a structure *is* | `platform.contract_structure` + `ContractMetadataService` | kernel declaration plane, `ARCH-CANONICAL` §31.2 | **MIXED** — also asserted by 39 mobile strategies and 8 site controllers |
| `RF-02` | Contract lifecycle and approval | `ContractLifecycleOrchestrator`, `ContractApprovalGate`, `platform.site_contract_revision` | same, evolved | **CANONICAL** |
| `RF-03` | Governed record persistence | `statistics.observation`, `entity.entity_record`, typed narrow tables | value-domain kernel, §31.2 | **PARTIAL** — two generic shapes, one typed one untyped (`CF-039`) |
| `RF-04` | Grain identity and uniqueness | application code only (`INV-006`) | `UNIQUE(structure_revision_id, business_key_digest)` + sealed write path, §31.6 | **UNENFORCED** |
| `RF-05` | Relationship semantics and cardinality | `entity.entity_link` (surrogate PK, no endpoint uniqueness — `CF-040`) | three fixed indexes + FK-locked flags, §31.7 | **UNENFORCED** |
| `RF-06` | Classification / reference data | `reference.*`, `classification_item_snapshot`, `PlatformClassificationMirrorService` | same, evolved | **CANONICAL** |
| `RF-07` | Statistical observation plane | `statistics.*` + `StatisticalContractConfiguration` | canonical statistical family `FAM-01` | **MIXED** — competes with hard-coded site calculators |
| `RF-08` | Entity / domain plane | `entity.entity_record.payload_json` | typed value domains | **REGRESSED** — untyped JSON is the known failure mode |
| `RF-09` | Raw / resource / ingestion | `PlatformIngestionService`, `PlatformSqlIngestionService`, `PlatformAccessIngestionService`, `raw.source_record` | canonical ingest | **MIXED** — see `PA-05` |
| `RF-10` | Publication and snapshot | `PlatformPublicationService`, `publication.*`, `usp_kids_r8_reconciliation` | same | **CANONICAL** |
| `RF-11` | Serving / query / projection | `ContractQueryCompiler`, `ContractQueryPlanService`, `ContractProjectionService` | same | **MIXED** — three other serving paths exist |
| `RF-12` | Presentation / visualization semantics | `dbo.chart_definitions` + `ChartDefinitionService` + `DynamicChartService` **and** `PlatformVisualizationQueryService` | canonical projection, §28 | **DUPLICATED** (`PA-03`) |
| `RF-13` | Page / navigation structure | `dbo.page_nodes` tree + `PageStructureService` **and** `ContractPageExecutionRegistry` | contract page binding | **DUPLICATED** (`PA-04`) |
| `RF-14` | Provider adaptation | `ProviderCapabilityRegistry`, `ProviderCapabilityNegotiator`, `RelationalStatisticalQueryAdapter` | same | **CANONICAL, single-provider** |
| `RF-15` | Import / export and codecs | `ExportCodecRegistry`, `ParquetExportCodec`, `XlsxToCsvController`, `MSSQLToAccess` | canonical codec registry | **MIXED** |
| `RF-16` | Authentication | `JwtTokenUtil`, `SignService`, `SignController`, `JwtRequestFilter`, `dbo.users/roles/permissions/tokens` | OIDC + `api/security` | **DUPLICATED** (`PA-02`) |
| `RF-17` | Tenancy and access policy | `api/security/tenancy` (23 files), `ContractPolicyService` | same | **CANONICAL** |
| `RF-18` | Schema migration authority | `platform.schema_migration` + `PlatformSchemaMigrationRunner` **and** `dbo.migrations` + `Migration` entity + `JsonMigrationRepository` | one governed ledger | **DUPLICATED** (`PA-01`) |
| `RF-19` | Archive and retention | `PlatformArchiveService`, `PlatformArchiveRetentionService`, `archive.*` | same | **CANONICAL** |
| `RF-20` | Audit and lineage | `provenance.*`, `raw.source_record` FKs | derivation model, §12 | **PARTIAL** — `audit` schema exists with zero rows, intent unknown |
| `RF-21` | Async operations, jobs, scheduling | `PlatformAsyncOperationService`, `PlatformJobLeaseService`, `PlatformOutboxProcessor`, `PlatformScheduledImportWorker` | same | **CANONICAL** |
| `RF-22` | Caching | `PlatformServingCacheService` (snapshot-bound) | same | **CANONICAL** |
| `RF-23` | Observability and metrics | `PlatformMetricQueryService`, `ProviderHealthSupervisor`, `HealthController` | same | **CANONICAL** |
| `RF-24` | Legacy site-specific serving | 8 controllers: `prices/{CpiCalculator,Inflation,kaleidoskope}`, `sagareo_vachroba/{Fdi,Sagareo}`, `socialuri_statistika/Salarium`, `sazogadoebastan_urtiertoba/ShedarebaPortali`, `biznes_statistika/MobileController` | canonical contract serving | **LEGACY AUTHORITY** |
| `RF-25` | **API↔mobile boundary** *(not the mobile subsystem, which is `OUT OF SCOPE` per `§0`)* | `api/build.gradle` project dependency · `MobileModuleConfig` dormant component scan · 3 API config references | the API module owns its own boundary | **COUPLED — isolate, `W-30`** |
| `RF-26` | Knowledge and documentation authority | `CATALOG`, `ADOPTION`, `DOC-CAD`, the phase artifacts | same | **CANONICAL** |
| `RF-27` | Test and fitness authority | `ops/tests/governance` (119), platform tests, `ops/tests/{security,sql}` | same + new fitness functions | **PARTIAL** |
| `RF-28` | Build, deployment and configuration | `settings.gradle` dynamic module inclusion, per-module `application-*.yml`, `ops/config` | same | **CANONICAL, with a trap** — see `§4` |

**`RESPONSIBILITY_FAMILIES_INVENTORIED: 28` · `CURRENT_AUTHORITIES_MAPPED: 28/28`.**

---

## 4. The boundary finding — an out-of-scope module that reaches into the API build

**`PHASE-005` first recorded this section as a product-decision blocker for API rehabilitation.
That was a scope error and is corrected here.** `platform/apps/geostat/backend/mobile` is
**OUT OF SCOPE** (`§0`). Its datasets, its database, its query semantics and its rehabilitation
are not this programme's concern and must not be inventoried, queried or planned.

**The historical evidence is retained, because scope exclusion is not evidence deletion.** The
module exists, it is separately deployable (`MobileApplication`, its own `Dockerfile`), it holds
177 committed Java files, and it reads ten hard-coded `dbo.*` tables through a secondary
datasource. That is recorded here as a fact about the repository and **nowhere used as canonical
evidence**. No count, invariant, capability or requirement in this plan derives from it.

**`ARCH-DOSSIER` §1.1 remains extended in scope, not falsified.** Its "exactly one runtime
dependency" figure is correct for the `api` and `core` trees, which is the scope that section
declares. The extension stands as a repository fact; it no longer implies any API workstream.

### 4.1 The one question that *is* in scope, and its answer

> *Does the existence or auto-inclusion of the out-of-scope module create a build, runtime,
> deployment or governance dependency that can affect the in-scope API Platform?*

**Answer: YES.** Established from the committed tree at `50d143f`, from build and wiring files
only — **no mobile source, database, schema or domain semantics was inspected to reach it.**

| # | Boundary fact | Evidence | Class |
|---|---|---|---|
| 1 | `settings.gradle` includes **every** sibling directory containing a `build.gradle` unless `-PactiveModules` overrides it, so `:mobile` is in the default build | `settings.gradle` | build |
| 2 | **`api/build.gradle` declares a dependency on `:mobile`** — `if (findProject(':mobile') != null && findProperty('includeMobile') != 'n') { implementation project(':mobile') }`. Both conditions hold by default, so the out-of-scope module is on the API's compile and runtime classpath and is packaged into its artifact | `api/build.gradle` L19–24 | build + packaging |
| 3 | **The API owns a class that can wire the whole module into its own Spring context** — `org.base.api.config.MobileModuleConfig`, `@ConditionalOnProperty(name = "mobile.enabled", matchIfMissing = true)`. Its `@ComponentScan(basePackages = "org.base.mobile")` is **currently commented out**, so no mobile bean loads today — but the switch, the property and the default-true condition are live in API source | `MobileModuleConfig.java` | runtime (**dormant**) |
| 4 | API configuration references the out-of-scope package: `org.base.mobile` logger levels in `application-dev.yml`, `application-prod.yml` and `logback-spring.xml` | 3 resources | configuration/governance |

**Characterisation.** The build and packaging coupling is **active and default-on**. The runtime
coupling is **dormant but re-armable by uncommenting three lines** — and because
`matchIfMissing = true`, it re-arms *silently*, with no configuration change anywhere. That is
the governance risk: an out-of-scope module can enter the in-scope application's runtime without
any decision being recorded.

### 4.2 Minimum API-side isolation requirement — derived, bounded, API-only

**`W-30`** (Stage C). Every action is inside `backend/api` or the backend build files. **Nothing
in `backend/mobile` is changed, rehabilitated or even read further.**

1. **Sever the build edge** — remove the conditional `implementation project(':mobile')` from
   `api/build.gradle`, or invert its default so inclusion is explicit opt-in rather than opt-out.
2. **Make module inclusion explicit** — `activeModules` must name its modules rather than
   scanning directories (`PR-03`), so an API build cannot silently absorb a sibling.
3. **Remove the dormant runtime switch** — delete `MobileModuleConfig` from the API module. An
   out-of-scope module must not be re-armable into the in-scope runtime by uncommenting code.
4. **Remove the out-of-scope configuration references** — the three `org.base.mobile` logger
   entries in API resources.
5. **Prove it and keep it proven** — `PR-17`: a fitness test failing the build if the API module
   declares a project dependency on `:mobile`, references `org.base.mobile` in source or
   resources, or reintroduces directory-scanned module inclusion.

**Exit criterion:** the API Platform builds, tests, packages and runs with `:mobile` absent from
the settings file entirely. **Deleting or retaining the mobile module then has no effect on the
API Platform** — which is precisely what "out of scope" must mean operationally.

**What `W-30` is not.** It is not mobile rehabilitation, not a migration, not an elimination of
the module, and not a decision about the module's future. It removes an *API-side* coupling.
Whoever owns `backend/mobile` remains free to do anything with it.

---

## 5. Parallel authority register — 11 classes

Discovered by responsibility, not by name collision. Every one carries a disposition.

| ID | One responsibility | Authority A | Authority B (and C, D) | Evidence | Disposition |
|---|---|---|---|---|---|
| `PA-01` | schema migration ledger | `platform.schema_migration` + `PlatformSchemaMigrationRunner` | `dbo.migrations` + `Migration` entity + `JsonMigrationRepository` | `ARCH-DOSSIER` §3; both physically present | **REPLACE** B → `W-22` |
| `PA-02` | authentication and authorization | OIDC + `api/security/tenancy` (23 files) | `JwtTokenUtil` + `SignService` + `SignController` + `JwtRequestFilter` + `dbo.{users,roles,permissions,tokens}` | 7 classes + 4 tables | **REPLACE** B → `W-21` |
| `PA-03` | presentation / chart semantics | `PlatformVisualizationQueryService` | `dbo.chart_definitions` + `ChartDefinition` entity + `ChartDefinitionService` + `DynamicChartService` | `ARCH-CANONICAL` `R-6` names this as the one accepted exception | **REPLACE** B → `W-19` |
| `PA-04` | page / navigation structure | `ContractPageExecutionRegistry` + contract page binding | `dbo.page_nodes` + `PageNode`/`DirectoryNode`/`PageLeafNode` + `PageStructureService` + `TreeBasePagesMigrationRunner` | ORM tree vs declared binding | **REPLACE** B → `W-20` |
| `PA-05` | ingestion definition | `PlatformIngestionService` / `PlatformSqlIngestionService` / `PlatformAccessIngestionService` | `DataProfile` + `ImportJob` + `ImportTableMapping` + `api/service/catalog` (23 files) + `ManagedImportController` | two import models, two audit trails | **MERGE** → `W-18` |
| `PA-06` | serving a dataset to a consumer | canonical contract serving | (B) 8 site controllers · (C) `DynamicDataController` + `api/service/dynamic` | two in-scope parallel paths | **REPLACE B, C** → `W-11`, `W-27`. *(D, the out-of-scope `mobile` application, is removed from this register by `§0`; only its API-side coupling remains, as `W-30`.)* |
| `PA-07` | statistical computation semantics | declared contract + compiler | `CpiCalculator`, `Inflation`, `kaleidoskope`, `Salarium`, `FdiController` — index/inflation logic in controllers | domain maths inside HTTP controllers | **MIGRATE** to declared derivation → `W-16` |
| `PA-08` | ~~dataset semantics (mobile)~~ | — | — | — | **WITHDRAWN — OUT OF SCOPE (`§0`).** Not an API Platform authority; the identifier is retired, not reused |
| `PA-09` | grain / record identity | designed kernel enforcement | application code (`INV-006` application-only) | `ARCH-BASELINE` §6.2 | **EVOLVE** → `W-13` |
| `PA-10` | relationship cardinality | designed fixed-index mechanism | application validation only (`CF-040`) | `entity_link` has no endpoint uniqueness | **EVOLVE** → `W-14` |
| `PA-11` | entity payload semantics | typed value domains | `entity_record.payload_json` | `CF-039` | **REPLACE** → `W-17` |

**`PARALLEL_AUTHORITIES_FOUND: 10` in scope** (`PA-08` withdrawn by `§0`) **· `PARALLEL_AUTHORITIES_WITHOUT_DISPOSITION: 0`.**

One of these was **not** in any prior register: `PA-05`, a second ingestion model, found by asking
*who decides this meaning* rather than *which file looks duplicated*. `PA-08` was also new but is
withdrawn by `§0` — it belonged to an out-of-scope module.

---

## 6. Master rehabilitation matrix

One row per responsibility family. `CAP` cites `AUD-CAPABILITY`; `INV` cites `REC-CONSOLIDATION`
or `STD-BENCH-001`; `G#` cites the 19 baseline guarantees of `ARCH-DOSSIER` §8.

| ID | Disposition | Canonical target | Capability preserved | Invariant protected | Migration | Bridge | Elimination target | Exit criteria | Work |
|---|---|---|---|---|---|---|---|---|---|
| `RF-01` | **EVOLVE** | kernel declaration plane | `CAP-004/005`, `CAP-M04` grain as components | `BM-INV-24` | structure rows rehomed, 13 columns mapped (`R-2`) | none | prose `business_grain` | every structure has a component-list grain | `W-15` |
| `RF-02` | **KEEP** | unchanged | `CAP-008/009` | `INV-009` revision immutability, `G14/G15` | none | none | none | regression suite green | `W-15` |
| `RF-03` | **GENERALIZE** | value-domain typed tables | `CAP-001/002/003` | `G3/G5`, `BM-INV-05` | expand→migrate→contract per structure | dual-read during migration | `entity_record.payload_json` | all structures typed; no JSON payload read | `W-12`, `W-17` |
| `RF-04` | **EVOLVE** | §31.6 mechanism | `CAP-M04` | `INV-006` (`PARTIAL`) | digest backfill, sealed write path | none | application-only grain checks | false-digest suite green; reconciliation clean | `W-11`, `W-13` |
| `RF-05` | **EVOLVE** | §31.7 three fixed indexes | `CAP-M10` declarable M:N | `CF-040` closed, `G2` zero cascade | backfill FK-locked flags | none | application-only cardinality checks | 4/4 cardinality suite green | `W-14` |
| `RF-06` | **KEEP** | unchanged | `CAP-012/013` | `G9` snapshot pinning | none | none | none | snapshot semantics unchanged | — |
| `RF-07` | **MIGRATE** | `FAM-01` declared | `CAP-006/007`, `CAP-M13` | `G7` temporality, `BM-INV-23` | site maths → declared derivation | read-only legacy endpoints | `PA-07` controllers | parity proven per indicator | `W-16` |
| `RF-08` | **REPLACE** | typed value domains | entity semantics | `CF-039` closed | payload decomposition | dual-write | `payload_json` column | zero reads of the column | `W-17` |
| `RF-09` | **MERGE** | canonical ingest | `CAP-016/017`, import audit trail | `G13` raw retention | job model unification | `ImportJob` read-only | `DataProfile`/`ImportJob`/`ImportTableMapping` | one job model, one audit trail | `W-18` |
| `RF-10` | **KEEP** | unchanged | `CAP-020/021` | `G11` snapshot immutability | none | none | none | — | — |
| `RF-11` | **EVOLVE** | contract serving | `CAP-022…025` | `BM-INV-13` | adapters for retired paths | compatibility endpoints | `DynamicDataController` | no non-canonical serving path | `W-11`, `W-27` |
| `RF-12` | **REPLACE** | canonical projection | chart definition semantics | `R-6` named exception closes | definitions → declared visualizations | read-only legacy charts | `dbo.chart_definitions` + 4 classes | no canonical projection reads `dbo.*` | `W-19` |
| `RF-13` | **REPLACE** | contract page binding | page tree semantics, ordering | navigation completeness | tree → declared bindings | dual-source read | `dbo.page_nodes` + 4 classes | all pages served from bindings | `W-20` |
| `RF-14` | **KEEP + PROVE** | unchanged | `CAP-028`, `ADR-016` rules | provider capability floor `BM-EC-085` | none | none | none | **second provider evidenced** | `W-25` |
| `RF-15` | **MERGE** | codec registry | xlsx/csv/accdb/parquet round trips | determinism `Q39` | `XlsxToCsv`/`MSSQLToAccess` → codecs | keep endpoints | ad-hoc converters | one registry, all formats | `W-23` |
| `RF-16` | **REPLACE** | OIDC + `api/security` | role/permission model, token lifetime rules | **least privilege; no escalation path** | user/role/permission mapping | **bounded, expiring** dual auth | 7 classes + 4 tables | no legacy principal can authenticate | `W-21` |
| `RF-17` | **KEEP** | unchanged | tenancy scoping | `ADR-tenant-scoped-authorization` (other programme) | none | none | none | — | — |
| `RF-18` | **REPLACE** | `platform.schema_migration` | applied-migration history | **ordered, immutable, replayable** | history reconciliation, then freeze | `dbo.migrations` read-only | `dbo.migrations` + `Migration` + repo | one ledger; fresh replay reproduces | `W-22` |
| `RF-19` | **KEEP** | unchanged | `CAP-030`, retention classes | `G12` | none | none | none | — | — |
| `RF-20` | **EVOLVE** | derivation model | `CAP-026/027`, `CAP-M09` | append-only | lineage edges populated | none | empty `audit` schema *if* abandoned | `OD-06` answered, then act | `W-16`, `OD-06` |
| `RF-21` | **KEEP** | unchanged | lease, outbox, idempotency | `G16` idempotency indexes | none | none | none | — | — |
| `RF-22` | **KEEP** | unchanged | snapshot-bound cache | cache never outlives snapshot | none | none | none | — | — |
| `RF-23` | **EVOLVE** | unchanged + new checks | health, metrics | observability | add reconciliation + DDL-audit signals | none | none | `W-13` signals live | `W-13` |
| `RF-24` | **RETIRE** | canonical serving | **per-indicator maths and edge cases** | loss test mandatory | characterize → declare → parity → retire | read-only window | 8 controllers | parity proven, no traffic | `W-08`, `W-16`, `W-27` |
| `RF-25` | **ISOLATE** | API owns its boundary | none lost — no capability is sourced from an out-of-scope module | **no out-of-scope module may enter the in-scope runtime undeclared** | none — API-side build/config only | none | `:mobile` build edge, `MobileModuleConfig`, 3 config refs | API builds and runs with `:mobile` absent from `settings.gradle` | `W-30` |
| `RF-26` | **EVOLVE** | `CATALOG`/`ADOPTION` | history, supersession lineage | `ONE FACT → ONE OWNER` | classify every doc | none | superseded claims, not files | no doc teaches a superseded model | `W-28` |
| `RF-27` | **EVOLVE** | governance + fitness | 119 governance tests | no test weakened to pass | add fitness functions | none | duplicate/obsolete tests | erosion checks in CI | `W-09`, `W-10` |
| `RF-28` | **EVOLVE** | explicit module set | reproducible build | **no implicit deployable** | make `activeModules` explicit | none | implicit directory scan | deleting code stops the image | `W-27` |

**`LEGACY_RESPONSIBILITIES_CLASSIFIED: 11/11`** (`RF-08`, `12`, `13`, `16`, `18`, `24`, `25` plus
the legacy halves of `RF-09`, `RF-11`, `RF-15`, `RF-01`).
**`TEMPORARY_BRIDGES: 8` · `BRIDGES_WITHOUT_EXIT_CONDITION: 0`** — every bridge above names its
elimination target and exit criterion; `RF-25`'s is not a bridge but an **undecided live
subsystem**, recorded as such rather than disguised as one.

---

## 7. Target physical families — 14 classified

`ARCH-BASELINE` inventories 139 structures. The target is not the existing schema frozen, and
not a rewrite: it is the existing schema **converged onto the kernel**, family by family.

| # | Physical family | Today | Target | Verdict |
|---|---|---|---|---|
| 1 | declaration (`contract_*`, `structure`, `component`) | 13 governed columns already carry structure/grain/authority | kernel declaration plane | **EVOLVE** (`R-2`) |
| 2 | record header | none — identity is per-plane | one `record` with revision + digest + assertion time | **NEW, justified by `RF-04`** |
| 3 | typed value domains | `localized_text`, `resource_locator`, `entity_classification` | generalized to all domains | **GENERALIZE** (`R-3`) |
| 4 | statistical observation | `observation` + `observation_dimension`, temporal columns | kernel record + reference values, temporality kept | **MERGE into kernel** (`R-1`) |
| 5 | entity plane | `entity_record.payload_json` | typed domains | **REPLACE** |
| 6 | relationship | `entity_link`, surrogate PK | relation value table + 3 indexes | **SPECIALIZE** (`R-9`) |
| 7 | reference / classification | `reference.*`, snapshots | unchanged | **KEEP** |
| 8 | raw | `raw.source_record` + FKs from every plane | unchanged | **KEEP** |
| 9 | publication | `publication.*` + 1 procedure | unchanged | **KEEP** |
| 10 | archive / retention | `archive.*`, `retention_register` (0 rows) | unchanged, intent confirmed | **KEEP pending `OD-06`** |
| 11 | provenance | `provenance.*` | derivation edges populated | **EVOLVE** |
| 12 | platform control | 39 enumerated CHECKs, 20 triggers, 8 filtered indexes | untouched | **KEEP** (`R-4`) |
| 13 | legacy `dbo.*` in `geostat-system` | 19 tables, ORM-owned | removed after consumer migration | **REMOVE** |
| 14 | ~~legacy `dbo.*` in `auto`~~ | — | — | **OUT OF SCOPE (`§0`)** — another application's database; not inventoried, not planned, not compared against |

**`TARGET_PHYSICAL_FAMILIES_CLASSIFIED: 13/13` in scope** — all decided. The fourteenth row is
retained above as a struck-through record that the structures exist; it is **out of scope** and
contributes no unresolved item to this programme.

---

## 8. Target convergence map

```text
LEGEND   ▣ canonical   ▢ legacy/parallel   ⇢ migrate   ⊘ eliminate   ▤ bounded bridge

SEMANTICS / CONTRACTS
  ▣ contract_structure ──evolve──▶ ▣ kernel declaration
  ▢ 39 mobile strategies ┐
  ▢ 8 site controllers   ├─⇢ declared structures ─⊘ (after parity)
  ▢ prose grain          ┘

PERSISTENCE
  ▣ typed narrow tables ──generalize──▶ ▣ value-domain kernel
  ▢ payload_json ──decompose──▶ ▣ typed domains ─⊘ column
  ▣ observation(+dimension) ──merge, temporality preserved──▶ ▣ kernel record

VALIDATION
  ▢ app-only grain      ─⇢ engine UNIQUE + sealed write path + trigger ─⊘
  ▢ app-only cardinality─⇢ 3 fixed indexes + FK-locked flags ─⊘
  ▣ 39 platform CHECKs ──unchanged──▶ ▣ (R-4: never becomes a join)

RELATIONSHIPS
  ▢ entity_link (no endpoint uniqueness) ─⇢ relation_value + ux_rel_{pair,source_one,target_one} ─⊘

CLASSIFICATION / REFERENCE
  ▣ reference.* + item snapshots ──unchanged──▶ ▣

STATISTICAL DATA
  ▢ CpiCalculator/Inflation/kaleidoskope/Salarium ─⇢ declared derivation ▤ read-only window ─⊘

ENTITY / DOMAIN DATA
  ▢ JSON payload ─⇢ typed ─⊘

RAW / RESOURCE
  ▣ raw.source_record ──unchanged──▶ ▣   ;  ▢ ImportJob model ─merge─▶ ▣ canonical ingest ─⊘

PUBLICATION / PRESENTATION
  ▣ publication.* ──unchanged──▶ ▣
  ▢ dbo.chart_definitions + 4 classes ─⇢ declared visualization ─⊘
  ▢ dbo.page_nodes + 4 classes ─⇢ contract page binding ─⊘

AUTH / SECURITY
  ▢ JWT+users/roles/permissions/tokens ─⇢ OIDC + tenancy ▤ expiring dual auth ─⊘

MIGRATIONS
  ▢ dbo.migrations + Migration entity ─⇢ platform.schema_migration ▤ read-only ─⊘

PROVIDERS
  ▣ SQL Server ──▶ ▣    ;   ▢ Access ad-hoc converters ─⇢ ▣ codec registry + provider contract

DOCS / GOVERNANCE
  ▣ CATALOG/ADOPTION ──classify every doc──▶ ▣ one fact, one owner, many references

MOBILE SUBSYSTEM
  ▢ 177 files / 10 tables in unreachable `auto` ──BLOCKED──▶ ? (EG-01 → OD-08)
```

Each family passes through the same five states — **parallel → protected → canonical
coexistence → cutover → elimination** — and no family may skip `protected`.

---

## 9. Execution roadmap — 29 workstreams, dependency-ordered

Ordering is derived from dependency, not convenience: **evidence before decision, decision
before protection, protection before migration, migration before cutover, cutover before
elimination, reversible before irreversible.**

### Stage A — evidence completion *(changes nothing; fully parallel)*

| ID | Purpose | Prereq | Output | Gate |
|---|---|---|---|---|
| ~~`W-01`~~ | ~~reach `auto`; recover its dataset semantics~~ | — | — | **WITHDRAWN by `§0`** — it would have inventoried an out-of-scope application's database. Identifier retired, not reused |
| `W-02` | SQL↔Access correspondence table | `AUD-COMPARATIVE` | per-object mapping | closes `EG-02`; feeds `W-23` |
| `W-03` | cross-table physical type-consistency sweep | captured 1 234 columns | inconsistency register | closes `EG-03`; feeds `W-12` |
| `W-04` | production workload and volume evidence | ops access | measured profile | closes `EG-04`/`BM-Q-03`; feeds `W-12`, `W-26` |

### Stage B — owner decisions *(no implementation may pre-empt these)*

| ID | Purpose | Prereq | Gate |
|---|---|---|---|
| `W-05` | **NFC text-grain identity decision** (`OD-01`) | §31.6 residual 2 | **blocks `W-13` and any work persisting a text grain component** |
| `W-06` | the six carried owner decisions (`OD-02…07`) | — | each blocks its own consumer only |
| ~~`W-07`~~ | ~~mobile subsystem disposition~~ | — | **WITHDRAWN by `§0`** — not an API Platform decision. Identifier retired, not reused |

### Stage C — protection *(before any migration — `PHASE-009` law)*

| ID | Purpose | Prereq | Gate |
|---|---|---|---|
| `W-08` | characterization tests for every legacy behaviour to be retired (8 controllers, ORM consumers, import model) | — | **no legacy path may be touched until its behaviour is pinned** |
| `W-09` | architecture fitness functions: forbidden dependencies, duplicate-registry detection, `dbo.*` read ban in canonical code, implicit-module ban | — | erosion prevention in CI |
| `W-10` | executable negative suites — §31.6 five false-digest attempts, §31.7 four cardinality attacks | `ARCH-CANONICAL` | **converts derived claims into evidence** |
| `W-11` | write-path permission model (`DENY` on `record`, `EXECUTE`-only procedure, ownership chaining) | — | **`W-13`'s L1 does not exist without it** |

### Stage D — kernel *(expand; additive and reversible)*

| ID | Purpose | Prereq | Gate |
|---|---|---|---|
| `W-12` | stable kernel schema — declaration, record, typed value domains | `W-03`, `W-04` | additive migration only; no existing table altered |
| `W-13` | grain enforcement — sealed write path, derivation trigger, verification trigger, reconciliation validator, DDL-disable audit | `W-05`, `W-10`, `W-11`, `W-12` | `INV-006` may move `PARTIAL → VERIFIED` **only** on green suites |
| `W-14` | relationship cardinality mechanism | `W-10`, `W-12` | 4/4 suites green; `CF-040` closed |
| `W-15` | contract compiler convergence — grain as components, declaration rehoming | `W-12` | every structure compiles |

### Stage E — migrate *(consumer by consumer; each independently reversible)*

| ID | Purpose | Prereq | Bridge | Gate |
|---|---|---|---|---|
| `W-16` | statistical plane + site maths → declared derivation | `W-13`, `W-15`, `W-08` | read-only legacy window | parity per indicator |
| `W-17` | entity plane — decompose `payload_json` | `W-13`, `W-15` | dual-write | zero reads of the column |
| `W-18` | ingestion convergence (`PA-05`) | `W-15` | `ImportJob` read-only | one job model, one audit trail |
| `W-19` | presentation / chart convergence (`PA-03`) | `W-15`, `W-08` | read-only legacy charts | no canonical projection reads `dbo.*` |
| `W-20` | page / navigation convergence (`PA-04`) | `W-15`, `W-08` | dual-source read | every page from a binding |
| `W-21` | **auth convergence** (`PA-02`) | `W-08` | **expiring** dual auth | no legacy principal authenticates; **no escalation path** |
| `W-22` | migration-ledger convergence (`PA-01`) | `W-12` | `dbo.migrations` read-only | fresh replay reproduces the ledger |

### Stage F — providers and surfaces *(parked, routed, not executed)*

| ID | Purpose | Prereq | Lifecycle position |
|---|---|---|---|
| `W-23` | **Access refinement** — provider · authoring surface · round-trip carrier, three roles kept distinct | `W-02`, `W-15`, `ADR-016` | **after** contract compiler convergence, **before** second-provider proof; feeds `W-24` |
| `W-24` | **Admin/UI blueprint `41627`** | `W-15`, `W-23` | **after** the contracts it projects exist; UI is projection/executor, **never authority** |
| `W-25` | second-provider proof (`ADR-016` rule 6) | `W-23` | closes the designed-but-unevidenced property |

### Stage G — eliminate *(irreversible; every step gated by proof of no dependency)*

| ID | Purpose | Prereq | Gate |
|---|---|---|---|
| `W-26` | legacy `dbo.*` elimination — 19 tables + paired ORM | `W-17…W-22` | zero runtime reference proven |
| `W-27` | legacy serving elimination — **the 8 in-scope site controllers and `DynamicDataController` only** | `W-16`, `W-28` | no traffic; consumer inventory closed (`EG-05`). *Rescoped by `§0`: the out-of-scope module is no longer part of this workstream and no longer gates it* |
| `W-28` | documentation convergence | all migrations | no doc teaches a superseded model |

### Stage H — convergence

| ID | Purpose | Prereq | Gate |
|---|---|---|---|
| `W-29` | final whole-system convergence audit | `W-01…W-28` | one authority per responsibility, proven |

**Parallelizable:** all of Stage A; `W-08`/`W-09`/`W-10`/`W-11`/`W-30`; `W-19`/`W-20` with `W-16`/`W-17`.
**Serialization points:** `W-05 → W-13`; `W-11 → W-13`; `W-15 → W-23 → W-24`. **The
`W-01 → W-07 → W-27` chain no longer exists** — `§0` removed both of its first two links, and
`W-27` is now gated only by `W-16`, `W-28` and `EG-05`.
**Irreversible:** `W-26`, `W-27`, and the contract step of every Stage E expand→migrate→contract.
`W-30` is reversible — it removes a coupling and adds a check.

**`MIGRATION_WORKSTREAMS: 28 active`** (30 identifiers registered; `W-01` and `W-07` withdrawn by
`§0` and never reused).

---

## 10. Elimination register

Deletion is not success; **proven convergence is success.** Every entry requires all seven
columns before the removal step may be scheduled.

| ID | Eliminate | Replacement authority | Unique-capability check | Consumers today | Migration prerequisite | Proof of no dependency | Post-removal test |
|---|---|---|---|---|---|---|---|
| `EL-01` | `dbo.migrations` + `Migration` + `JsonMigrationRepository` | `platform.schema_migration` | applied history, ordering | `JsonMigrationRepository` | `W-22` | grep + runtime trace + fresh replay | replay reproduces ledger |
| `EL-02` | `dbo.{users,roles,permissions,tokens}` + 7 auth classes | OIDC + `api/security/tenancy` | role/permission mapping, token lifetime | `SignController`, filter, 3 services | `W-21` | **no principal authenticates via legacy**; privilege diff | escalation negative tests |
| `EL-03` | `dbo.chart_definitions` + 4 classes | declared visualization | chart types, per-chart options | `DynamicChartService`, `ChartDefinitionService` | `W-19` | canonical projection reads no `dbo.*` | visual parity suite |
| `EL-04` | `dbo.page_nodes` + 4 classes | contract page binding | tree order, leaf/dir semantics | `PageStructureService`, runner | `W-20` | every page served from a binding | navigation parity |
| `EL-05` | `DataProfile`/`ImportJob`/`ImportTableMapping` + catalog services | canonical ingest | import audit trail, mapping rules | `ManagedImport*` (5 services) | `W-18` | one job model in runtime | import replay |
| `EL-06` | 8 legacy site controllers | declared structures + serving | **per-indicator maths, edge cases** | external HTTP consumers | `W-16` + `W-08` | traffic log shows zero | parity per indicator |
| `EL-07` | `DynamicDataController` + `api/service/dynamic` | contract serving | dynamic table/chart shaping | unknown external | `W-11` | consumer inventory | serving parity |
| `EL-08` | `entity_record.payload_json` | typed value domains | arbitrary attribute storage | entity plane readers | `W-17` | zero reads of the column | typed round-trip |
| `EL-09` | remaining 11 `dbo.*` tables | kernel | per-table loss test | ORM pairs | `W-26` | FK + grep + trace | schema diff |
| ~~`EL-10`~~ | ~~`mobile` module + 10 `auto` tables~~ | — | — | — | — | — | **WITHDRAWN by `§0`.** Another application's module and database are not this programme's to eliminate. Identifier retired, not reused |
| `EL-11` | implicit Gradle module inclusion | explicit `activeModules` | reproducible build | build pipeline | `W-27` | build manifest diff | deleting code stops the image |
| `EL-12` | superseded documentation claims | canonical owners | historical evidence **retained** | readers | `W-28` | reference sweep | no superseded teaching |
| `EL-13` | **the API module's coupling to `:mobile`** — build edge, dormant `MobileModuleConfig` scan switch, 3 config references | the API owns its own boundary | none — no capability is sourced from an out-of-scope module | the API build itself | `W-30` | API builds with `:mobile` absent from `settings.gradle` | `PR-17` fitness test |

**`ELIMINATION_TARGETS_DERIVED: 12` in scope** — all schedulable (13 identifiers registered;
`EL-10` withdrawn by `§0`, `EL-13` added for the API-side coupling).

> **The Greenfield and Loss tests were applied to every entry.** `EL-06` is now the only one where
> the loss test returns *substantial*: the 8 site controllers carry per-indicator arithmetic that
> exists in no contract. It therefore gets characterization work (`W-08`) **before** any removal.
> Legacy existence alone justified nothing. `EL-13` passes the loss test trivially — severing a
> build edge removes no capability, because no in-scope capability was ever sourced through it.

---

## 11. Protection plan

Automated enforcement is preferred to documented rules wherever a check can be written.

| ID | Control | Prevents | Where | Workstream |
|---|---|---|---|---|
| `PR-01` | forbidden-dependency check: canonical code may not import legacy ORM or read `dbo.*` | legacy leaking upward | CI fitness test | `W-09` |
| `PR-02` | duplicate-registry detection: one registry per responsibility | new parallel authority | CI | `W-09` |
| `PR-03` | implicit-module ban: `activeModules` must be explicit | deleting code but still shipping it | build check | `W-09` |
| `PR-04` | legacy write prohibition: legacy tables read-only once their bridge opens | two writers, divergent truth | DB permission + test | `W-19…W-22` |
| `PR-05` | compatibility-bridge expiry: every bridge declares a removal checkpoint and fails the build past it | bridges becoming permanent | CI | `W-09` |
| `PR-06` | grain reconciliation validator | silent digest drift | `ops` validator | `W-13` |
| `PR-07` | DDL-audit: trigger disable/alter is alerted | the named §31.6 residual | DB audit | `W-13` |
| `PR-08` | characterization suites for every retirement target | losing unique behaviour | test tree | `W-08` |
| `PR-09` | negative suites — false digest ×5, cardinality ×4 | design claims that never become evidence | test tree | `W-10` |
| `PR-10` | migration guard: no `ALTER` on a governed table outside an approved expand/contract step | uncontrolled schema drift | migration lint | `W-09` |
| `PR-11` | contract validation in CI (existing, extended) | semantics diverging from declaration | CI | `W-15` |
| `PR-12` | governance validation (existing `rcp-verify` + `engineering-governance`) | authority corruption | CI | in force now |
| `PR-17` | **scope-boundary fitness test**: the API module may not declare a project dependency on an out-of-scope module, reference its packages in source or resources, or reintroduce directory-scanned module inclusion | an out-of-scope application silently re-entering the in-scope build or runtime | CI | `W-30` |

`PR-12` is already running; `PR-01…PR-11`, `PR-13`, `PR-15`, `PR-16` and `PR-17` are new and all
land in Stage C, **before** the first migration. `PR-17` is what keeps `§0` enforced by the build
rather than by good intentions.

---

## 12. Data migration and correspondence

Correspondence is derived from **semantics**, never from table name or file hash.

| Class | Meaning | Applies to |
|---|---|---|
| `PRESERVED` | same meaning, same shape | reference, raw, publication, archive |
| `TRANSFORMED` | same meaning, new representation | observation → kernel record; `payload_json` → typed |
| `MIGRATED` | moved to a new owner | chart definitions, page nodes, import mappings |
| `REPLACED` | new authority, old discarded after parity | auth model, migration ledger |
| `STILL_DEPENDED_UPON` | legacy remains live during coexistence | all Stage E bridges |
| `NOT_YET_MIGRATED` | scheduled, not started | Stage E targets |
| `INTENTIONALLY_RETIRED` | capability deliberately dropped, recorded | decided per item in `W-08` |
| `UNRESOLVED` | correspondence unknown | none in scope *(the 10 `auto` tables are out of scope per `§0` and are not a correspondence question for this programme)* |

For every data-bearing structure the execution package must carry: source meaning · target
meaning · transformation · loss risk · validation · reconciliation · rollback · coexistence
requirement · cutover · retirement. **`AQ-05`'s lesson is binding: identity of bytes is not
identity of meaning.**

---

## 13. Failure, rollback and recovery

| Migration class | Failure boundary | Idempotency | Rollback | Resume |
|---|---|---|---|---|
| additive schema (`W-12`) | per migration | yes | drop added objects | trivial |
| backfill (`W-13` digests, `W-14` flags) | batched, bounded | yes, keyed | recompute or null out | checkpointed |
| decomposition (`W-17`) | per structure | yes | dual-write keeps the source | per structure |
| authority switch (`W-19…W-22`) | per consumer | n/a | flip back to the bridge | per consumer |
| contract/drop (`W-26`, `W-27`) | **irreversible** | n/a | **restore from backup only** | none |

**No destructive step may rely on "we have tests."** Every irreversible step requires: proof of
zero runtime dependency, a verified backup, a restore rehearsal, and an explicit owner
authorisation recorded in its card. **`DESTRUCTIVE_STEPS_WITHOUT_ROLLBACK_OR_PROTECTION: 0`** —
the two irreversible workstreams (`W-26`, `W-27`) are each gated by `PR-04`, `PR-08` and a
restore rehearsal.

---

## 14. Performance and scale

| Path | Risk under the kernel | Mitigation | Evidence needed |
|---|---|---|---|
| wide record read | one join per value domain, not per column | bounded domain count; covering indexes | `W-04` |
| grain lookup | single index seek on `(revision, digest)` | by construction | `W-10` |
| relationship traversal | three fixed indexes | by construction | `W-14` |
| classification join | unchanged from today | — | — |
| publication / snapshot | unchanged | — | — |
| migration batches | long locks on backfill | bounded batches, online where possible | `W-04` |
| high-volume observation | **the open question** | §31.4 materialization | **`BM-Q-03` unresolved** |

**Architectural scalability is the point.** Today a new dataset costs a table, an entity, a
repository, a DTO, a migration and — in the mobile subsystem — a hand-written query strategy.
After convergence it costs a declaration. A system fast in SQL but requiring code per dataset is
not scalable; that is precisely the defect `RF-25` demonstrates at scale of 39.

---

## 15. Standards and pattern check

| Pattern | Problem it addresses here | Verdict | Cost |
|---|---|---|---|
| Expand → Migrate → Contract | every Stage D/E schema change | **ADOPT** — already repository doctrine | bridge lifetime |
| Strangler fig | the 8 site controllers and `RF-25` | **ADOPT, bounded** — per endpoint, with `PR-05` expiry | dual path while open |
| Parallel run + reconciliation | `W-16` indicator parity, `W-22` ledger | **ADOPT** for parity-critical paths only | compute cost |
| Branch by abstraction | auth swap (`W-21`) | **ADAPT** — an expiring adapter, not a permanent seam | one bounded seam |
| Event sourcing | provenance | **REJECT** — the append-only derivation model already satisfies `CAP-026/027` without an event store | would be unjustified complexity |
| CQRS | serving vs writing | **REJECT as a framework**; the read model is already a declared projection | — |
| Dual-write everywhere | general migration | **REJECT** — bounded dual-write only in `W-17`, where the source column remains authoritative until cutover | divergence risk |
| Feature flags per migration | cutover control | **BOUND** — allowed only with `PR-05` expiry, else they become permanent branches | config sprawl |

Nothing here is adopted for reputation. Each entry names the problem in **this** repository that
justifies it.

---

## 16. Falsification — 30 attacks, 6 of which succeeded

The plan was attacked before acceptance. **Six attacks found real weaknesses**; each is repaired
below and the attack re-run. A plan that survives its first pass unscathed was not attacked hard
enough.

### 16.1 The six that succeeded, and their repairs

| # | Attack | Why it succeeded against the draft | Repair | Re-run |
|---|---|---|---|---|
| **11** | *Logical evolution accidentally requires new DDL* | `PR-10` guarded `ALTER` on governed tables but said nothing about **new** tables. A Stage E migration could quietly create a per-structure table and nobody would fail a check — the exact regression `ARCH-CANONICAL` §31.4 forbids as a default | **`PR-10` extended**: a new physical table for a logical structure fails CI unless it carries an approved §31.4 materialization declaration naming which of the four criteria it meets | **HELD** |
| **25** | *NFC decision bypassed by implementation ordering* | The block was expressed as `W-05 → W-13`. But a Stage E migration (`W-16`, `W-17`) could persist a text grain component **without touching `W-13`**, silently converting today's byte-exact behaviour into permanent policy | **`PR-15` added**: CI fails if any structure declares a **text component inside its grain** while `OD-01` is open. The block now attaches to the *property*, not to one workstream | **HELD** |
| **28** | *A removed authority survives in configuration / runtime wiring* | `settings.gradle` includes every sibling directory with a `build.gradle`. Deleting the mobile source without removing the directory keeps building and shipping the image — deletion would look complete and be false | **`PR-03` added** (implicit-module ban) and **`RF-28` reclassified** from `KEEP` to `EVOLVE`. This attack is why `RF-28` is in the matrix at all | **HELD** |
| **29** | *A legacy table is removed but its ORM path survives* | `EL-09` paired tables with entities in prose; nothing enforced the pairing. A dropped table with a live `@Entity` fails at runtime, not at build | **`PR-13` added**: schema-diff × entity-scan check — no ORM entity may map a table absent from the target schema, and no dropped table may retain a mapping | **HELD** |
| **16** | *Rollback restores code but not data* | Stage E cutover said "flip back to the bridge". That restores the **path** but abandons rows written canonically after the switch | **`§13` amended**: every authority switch requires a reconciliation checkpoint immediately before cutover and a **replay-back procedure** for canonical writes, rehearsed before the switch. No switch without a tested reverse path | **HELD** |
| **13** | *Historical correction becomes impossible* | The correspondence model (`§12`) preserved values but never named **assertion time** as a migration obligation. Collapse the three temporal axes into one during `W-16`/`W-17` and corrections become unrepresentable — the exact defect `R-1` was written to prevent | **`§12` amended**: assertion time and release time are **migration-preserved fields**, not derived; any transformation that cannot carry both is a `PLAN DEVIATION` requiring an owner decision | **HELD** |

### 16.2 The twenty-four that held

| # | Attack | Control that holds it |
|---|---|---|
| 1 | canonical path added but old path still writes | `PR-04` legacy write prohibition, per bridge |
| 2 | old validator disagrees with canonical contract | `PR-11` contract validation; `W-08` characterization pins the disagreement first |
| 3 | old table still read by a hidden job | `EL-*` proof-of-no-dependency = grep **+ runtime trace**, not grep alone; `W-21`'s scheduler inventory |
| 4 | migration preserves rows but loses meaning | `§12` semantic correspondence classes; `AQ-05`'s byte-identity lesson is binding |
| 5 | two migration ledgers remain authoritative | `W-22` exit = fresh replay reproduces the ledger; `EL-01` |
| 6 | auth migration leaves an escalation path | `EL-02` requires a **privilege diff** and escalation negative tests, not just "OIDC works" |
| 7 | presentation keeps old semantic definitions | `EL-03` exit = no canonical projection reads `dbo.*`; `PR-01` |
| 8 | docs still teach superseded architecture | `W-28`, `EL-12`, `RF-26`; supersession recorded, evidence retained |
| 9 | a bridge becomes permanent | `PR-05` expiry check fails the build; **0 bridges without exit condition** |
| 10 | provider behaviour leaks into canonical semantics | `ADR-016`; `RF-14` capability negotiation; `W-23` keeps the three Access roles distinct |
| 12 | generic kernel weakens relational integrity | `R-4` (39 CHECKs stay), §31.3's two named limitations, §31.4 specialization |
| 14 | archive/retention semantics change | `RF-19` `KEEP`; `G12` |
| 15 | failed migration cannot resume | `§13` checkpointed batches, keyed idempotency |
| 17 | legacy test coverage hides a unique behaviour | `W-08` characterization **before** any retirement; the Loss test on every `EL-*` |
| 18 | Access round-trip changes meaning | `ADR-016`; `W-02` correspondence precedes `W-23`; Access is never semantic authority |
| 19 | Admin/UI becomes a second semantic authority | `W-24` scheduled **after** `W-15`; `PR-02` duplicate-registry detection covers UI-side registries |
| 20 | PHASE-005 absorbs security-hardening work | verified: **zero** files under `platform/`, `ops/config`, `ops/tests/{security,sql}` touched |
| **31** | *an agent recursively expands into an excluded neighbouring project because it is reachable from an in-scope module* | **`§0`** states the rule, **`PR-17`** enforces it in CI, and `§4` demonstrates the only permitted exception and where it stops. Added by the scope correction |
| 21 | a capability from the inventory disappears | `§17` traceability, 48/48 |
| 22 | one of `CAP-M01…M16` disappears | `§17`; each is carried by a named workstream |
| 23 | `D1…D7` no longer represented | `§17` |
| 24 | a `Q01…Q50` owner decision is contradicted | `ADR-014` rule 1 — `DECIDED` entries bind M1 semantics; `PR-11` |
| 26 | grain digest becomes business identity | §31.6 authority statement; `PR-06`; the digest is never published or exchanged |
| 27 | relationship cardinality becomes application-only | `W-14` exit = 4/4 engine-enforced suites; `PR-09` |
| 30 | legacy code removed while the table remains an undocumented authority | `EL-09`/`EL-10` require the **table** disposition, not only the code; `PR-13` works in both directions |

**`FALSIFICATION_SCENARIOS_RUN: 31` · `FALSIFICATION_FAILURES_REMAINING: 0`.**
Protection controls after repair: **`PR-01 … PR-17`** (`PR-13`, `PR-15` added above; `PR-14`
folded into `PR-02`; `PR-16` = restore rehearsal, from attack 16; `PR-17` = scope-boundary
fitness test, from attack 31).

---

## 17. Knowledge preservation check

| Item | Carried by | Status |
|---|---|---|
| 43 recovered capabilities + additions = `CAP-001…032`, `CAP-M01…M16` (**48**) | `§6` matrix; every family names its capabilities | **48/48** |
| `Q01…Q50` | `DOM-STATISTICAL` remains owner; `PR-11` enforces; `Q39` scope corrected by `R-8` | preserved |
| `D1…D7` | `D1` §21 · `D2` `R-1` · `D3` `OD-04` · `D4` `RF-20` · `D5` `PR-11` · `D6` `RF-06` · `D7` `RF-14` | **7/7** |
| `BM-INV-01…25`, incl. `BM-INV-23/24/25` | `STD-BENCH-001` owner; `RF-01`/`RF-03`/`RF-09` carry 23/24/25 | preserved |
| 19 physical guarantees | `§6` per-row `G#` citations; `§7` family verdicts | **19/19** |
| ADR boundaries `ADR-014/015/016` | `RF-07`, `RF-14`, `W-23`, `W-25` | preserved |
| SDMX boundary | `ADR-015`; no workstream widens it | preserved |
| DDI-CDI representation insight | `RF-01` structure roles (`tidy/long/wide/dimensional`) | preserved |
| raw → canonical → derived → published | `RF-09`, `RF-03`, `RF-20`, `RF-10` | preserved |
| logical/physical separation | `§7`, `PR-10` | preserved |
| three graph families | `RF-05` composition · `RF-20` derivation · dependency stays computed | preserved |
| contract lifecycle | `RF-02` `KEEP` | preserved |
| temporal semantics | `R-1`; **strengthened** by the attack-13 repair | preserved |
| legal erasure vs immutability | `OD-04` | open, owned |
| disclosure control | `OD-02` | open, owned |
| consumer / impact analysis | `RF-20`, `EL-07` consumer inventory | preserved |
| cross-dataset consistency | `PR-11`, publication gate | preserved |
| reference ownership | `OD-05` | open, owned |
| relationship semantics (13 dimensions) | `RF-05`, `W-14` | preserved |
| provider boundaries | `RF-14`, `W-25` | preserved |
| Access responsibilities (3 roles) | `W-23` | routed |
| Admin/UI parked intent + product law | `W-24` | routed |

**Nothing is superseded silently.** The one supersession recorded this phase is
`ARCH-DOSSIER` §1.1's *scope* (not its conclusion), extended by `§4` and routed to its owner.

---

## 18. Decision and uncertainty register

| ID | Class | Item | Owner | Latest safe decision point | Blocks | Does **not** block |
|---|---|---|---|---|---|---|
| `OD-01` | OWNER / SECURITY | **Unicode NFC for text grain components** | platform/data owner (`ARCH-CANONICAL` §39) | before the **first** persistence of a text grain component | `W-13`, and any Stage E structure with text in grain (`PR-15`) | `W-05` planning, Stage A, `W-12` |
| `OD-02` | OWNER | disclosure-control policy | steward | `PHASE-010` | publication of confidential aggregates | everything else |
| `OD-03` | OWNER | erasability classes and retention | steward | `PHASE-009` | `RF-19` changes | — |
| `OD-04` | OWNER | `D3` legal erasure vs immutable snapshots | legal + steward | `PHASE-009` | erasure implementation | — |
| `OD-05` | OWNER | reference-data agency assignment | steward | `PHASE-006` | `RF-06` authority rows | — |
| `OD-06` | OWNER | are `geo`, `audit`, `retention_register` intended or abandoned? | platform owner | `W-26` | `EL-09` scope | Stage D |
| `OD-07` | OWNER / OPS | §31.4 materialization thresholds | ops | `W-12` | specialization decisions | kernel creation |
| ~~`OD-08`~~ | — | ~~mobile subsystem disposition~~ | — | — | — | **WITHDRAWN by `§0`** — the out-of-scope module's future is not an API Platform decision. Identifier retired, not reused |
| ~~`EG-01`~~ | — | ~~`auto` unreachable~~ | — | — | — | **WITHDRAWN by `§0`** — closing it would have required inventorying an out-of-scope database. **It blocks nothing.** Identifier retired, not reused |
| `EG-02` | EVIDENCE GAP | SQL↔Access correspondence | `W-02` | before `W-23` | `W-23` | — |
| `EG-03` | EVIDENCE GAP | cross-table type consistency | `W-03` | before `W-12` | `W-12` detail | — |
| `EG-04` | EVIDENCE GAP | production volumes / hot paths (`BM-Q-03`) | ops | before `W-26` | performance claims, §31.4 thresholds | correctness work |
| `EG-05` | EVIDENCE GAP | consumer inventory for `DynamicDataController` and the 8 site endpoints | `W-08` | before `EL-06`/`EL-07` | those eliminations | — |
| `EG-06` | EVIDENCE GAP | second provider — designed, unevidenced | `W-25` | before claiming provider-agnosticism | the claim only | the design |
| `AL-01` | ACCEPTED LIMITATION | per-field `NOT NULL` is compiled validation | — | — | nothing | — |
| `AL-02` | ACCEPTED LIMITATION | per-field range/pattern is compiled validation unless materialized | — | — | nothing | — |
| `AL-03` | ACCEPTED LIMITATION | `INV-006` = `PARTIAL` until `W-13` suites are green | — | `W-13` | stronger language | — |
| `IM-01` | IMPLEMENTATION | batch sizes, index fill factors, lock windows | `PHASE-006` | per card | — | — |

**`OWNER_DECISIONS_OPEN: 7` · `EVIDENCE_GAPS_OPEN: 5` · `ACCEPTED_LIMITATIONS: 3`.**

**Nothing was deleted to reach those numbers.** `OD-08` and `EG-01` remain registered as
withdrawn rows so the history stays legible and the identifiers stay retired; `§0` records why.

---

## 19. `PHASE-006` entry contract

`PHASE-005` does not pass because a document exists. `GATE-PLAN` passes only when every row
below is true, and `PHASE-006` may not begin until it does.

| # | Entry condition | Satisfied by | State |
|---|---|---|---|
| 1 | canonical target mapped to every current responsibility | `§3` 28/28 | ✔ |
| 2 | no unexplained parallel authority | `§5` 11 found, 11 disposed | ✔ |
| 3 | every unique capability accounted for | `§17` 48/48 | ✔ |
| 4 | every legacy responsibility classified | `§6` 11/11 | ✔ |
| 5 | dependency graph complete enough for safe ordering | `§9` 29 workstreams, serialization points named | ✔ |
| 6 | destructive steps carry protection and rollback | `§13`, `PR-04`/`PR-08`/`PR-16`; 0 unprotected | ✔ |
| 7 | unresolved decisions routed to owners and gates | `§18` 8 + 6 + 3, each with a blocking scope | ✔ |
| 8 | Access and Admin/UI correctly placed, not executed | `W-23`, `W-24` | ✔ |
| 9 | security-hardening boundary preserved | zero files touched under `platform/`, `ops/config`, `ops/tests/{security,sql}` | ✔ |
| 10 | migration and elimination criteria explicit | `§10` 12 entries, 7 columns each | ✔ |
| 11 | verification strategy explicit | `§11` `PR-01…PR-16` | ✔ |
| 12 | no implementation performed during `PHASE-005` | `git status` — only `docs/` changed | ✔ |
| 13 | **Stage A evidence work scheduled before its dependents** | `§9` ordering (`W-02`…`W-04`) | ✔ |
| 14 | **`OD-01` routed as a property-level block, not a workstream-level one** | `PR-15` | ✔ |
| 15 | **`PHASE-006` must not expand into an out-of-scope project** | `§0` scope boundary · `PR-17` fitness test · `W-30` exit criterion | ✔ — *carried as a hard constraint into `PHASE-006`, replacing the withdrawn `EG-01` constraint* |

Conditions 13–14 were derived here; condition 15 was rewritten by the scope correction. The
boundary question `§4.1` answers **YES**, so `W-30` is a real entry obligation — but it is an
API-side isolation workstream, not a dependency on anything outside the scope of `§0`.

---

## 20. What `PHASE-005` deliberately did not do

No canonical runtime code · no migration executed · no database modified (`platform.schema_migration`
still 113) · no legacy deleted · no Access refinement · no Admin/UI work · no frontend · no
refactor · no owner decision settled · no unrelated working-tree change absorbed · `PHASE-006`
not begun.

**`ARCH-CANONICAL` was not reopened.** Nothing in this plan falsifies a canonical decision.

**Scope correction, 2026-09-21.** `backend/mobile` was initially treated as a product-decision
blocker for API rehabilitation. It is **out of scope** (`§0`). `EG-01`, `OD-08`, `W-01`, `W-07`,
`PA-08` and `EL-10` are **withdrawn**; `W-27` is rescoped to in-scope serving only; `W-30`,
`EL-13`, `PR-17` and falsification attack 31 are added to hold the boundary the build actually
creates. The evidence that the module exists is **retained, not deleted** — scope exclusion is
not evidence deletion — and no withdrawn identifier is reused.
