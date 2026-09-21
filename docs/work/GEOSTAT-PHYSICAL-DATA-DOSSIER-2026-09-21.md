---
id: ARCH-DOSSIER
type: EVIDENCE
title: Physical data dossier
status: COMPLETE
authority: CANONICAL
scope: database universe, effective schema, ORM correspondence, legacy boundary, anti-regression matrix and draft contradiction matrix
owner: PHASE-004
created: 2026-09-21
updated: 2026-09-21
related: ARCH-BASELINE, ARCH-CANONICAL, REC-CONSOLIDATION
---

# PHYSICAL DATA DOSSIER

**Purpose, in one sentence.** A Principal Database Architect who has never seen this repository
must be able to reconstruct, challenge and safely redesign the existing physical data
architecture from this document and its machine-readable evidence — **without chat history,
memory, filenames or undocumented convention**.

`ARCH-BASELINE` established *what exists*. This dossier establishes *what it means, who owns it,
what enforces it, and what breaks if it changes*. Together they are the evidence barrier that
`PHASE-004` synthesis may not cross without proving non-regression.

**Evidence, all reproducible:**

| File | Content |
|---|---|
| `evidence/physical-relational-baseline/physical-model.json` | static reconstruction of the 113-migration chain |
| `evidence/physical-relational-baseline/live-tables.txt` | live table list, 3 databases |
| `evidence/physical-relational-baseline/live-schema-full.txt` | **full live introspection**: 1 234 columns, 236 key constraints, 135 FKs, 115 CHECKs, 296 indexes, 20 triggers, 2 modules, 139 row counts |
| `evidence/physical-relational-baseline/live-summary.json` | per-database counts |

**Access discipline.** Read-only `sys.*` catalogue queries on the owner-authorised dev host. No
DDL, no DML, no schema change, nothing written. **No credential entered this session, any
artifact, or any log** — the container's own environment variable was dereferenced inside
`docker exec` on the remote side. Only datasource *roles* and non-secret host/database names
appear anywhere in this repository.

---

## 1. Database universe and authority classification

Four datasources are configured. **They are not peers.**

| Role | Database | Class | Reachable | Inspected |
|---|---|---|---|---|
| `DB_PRIMARY` | `geostat-system` | **NEW — control plane** | yes | **fully** — 93 tables, 851 columns |
| `DB_DATA` | `geostat-data` | **NEW — data plane** | yes | **fully** — 41 tables, 343 columns |
| `DB_ARCHIVE` | `geostat-archive` | **NEW — archive plane** | yes | **fully** — 5 tables, 40 columns |
| `DB_SECONDARY` | `auto` | **LEGACY source system** | **no** — TCP 1433 unreachable from the dev host and from the container network | **not inspected** |

### 1.1 `auto` — configured, unreachable, and barely depended upon

**Reachability, tested:** `192.168.0.230:1433` is closed/unreachable from the dev host
(`192.168.1.199`) and from the container network. The address is on a different subnet from the
server; it is plausibly an office-LAN or decommissioned host. **This dossier therefore cannot
state what `auto` contains, and does not guess.**

**Runtime dependency, measured from code — exactly one:**

| Consumer | Use | Assessment |
|---|---|---|
| `DataSourceConfig` | declares `secondaryDataSource` and `secondaryJdbcTemplate` beans | configuration, not data use |
| `HealthController` | `secondaryJdbc.queryForObject("SELECT 1")` | **a liveness probe, nothing more** |

**No repository, service, query, mapping or migration reads business data from `auto`.** The
grep covered the api and core source trees; `secondaryJdbc` appears in one class.

**Therefore, for the classification the phase instruction asks for:** the new→legacy runtime
dependency count is **1**, and it is `STILL DEPENDED UPON` only in the sense that a health
endpoint will report it down. **`auto` is not a data dependency of the new architecture.**

**What remains `UNRESOLVED`, stated rather than assumed:** whether `auto` holds business
semantics that were never migrated. It cannot be answered without access. **The legacy *data*
that the new system actually uses is not in `auto` at all — it is in `dbo.*` inside
`geostat-system`** (§3), which is a materially different and more urgent fact.

> **SCOPE EXTENSION (`PHASE-005`, 2026-09-21).** The dependency count above is measured over the
> scope this section declares — *"the api and core source trees"*. There is a **third source
> tree**: `platform/apps/geostat/backend/mobile`, a separately deployable application of 177
> committed Java files that reads ten hard-coded `dbo.*` tables from a secondary datasource. The
> count of **1** remains correct for `api` + `core`; it is **not** the whole-repository figure.
> **That third tree is `OUT OF SCOPE` for the rehabilitation programme** (`PLAN-MASTER` §0), so
> `auto` raises **no** open question, evidence gap or workstream here: the API Platform's only
> in-scope concern is the *build/runtime coupling* its own module declares, recorded as
> `PLAN-MASTER` §4.1 and isolated by `W-30`. This paragraph extends the **scope** of §1.1; it
> does not falsify its conclusion, and no canonical decision changes. Retained as evidence that
> the structures exist — **scope exclusion is not evidence deletion.**

---

## 2. Effective schema — live, not migration history

| | `geostat-system` | `geostat-data` | `geostat-archive` | **Total** |
|---|---|---|---|---|
| Tables | 93 | 41 | 5 | **139** |
| Columns | 851 | 343 | 40 | **1 234** |
| PK + UNIQUE constraints | 171 | 55 | 10 | **236** |
| Foreign keys | 98 | 35 | 2 | **135** |
| CHECK constraints | 70 | 45 | 0 | **115** |
| Indexes (incl. PK/UQ backing) | 201 | 83 | 12 | **296** |
| Triggers | 9 | 11 | 0 | **20** |
| Views + procedures | 0 | 2 | 0 | **2** |
| Referential actions | **`NO_ACTION` on all 135 FKs — zero cascades anywhere** | | | |

**Migration chain ↔ live reconciliation:** 113 migrations declared, **113 applied**
(`platform.schema_migration`, last `112_statistical_numeric_envelope_data.sql`). All 120
chain-declared tables exist live; **zero are missing**. The live model is larger by exactly the
19 `dbo.*` tables of §3. **`EFFECTIVE_SCHEMA_RECONSTRUCTED: YES`.**

---

## 3. The legacy substrate inside the new database — and it is the ORM

`ARCH-BASELINE` §1.1 found 19 `dbo.*` tables outside the governed chain. The code answers what
they are:

| Evidence | Result |
|---|---|
| `@Entity` classes | **1 in api, 15 in core** |
| `@Table(name=…)` targets | `chart_definitions`, `data_profiles`, `import_jobs`, `import_job_items`, `import_table_mappings`, `migrations`, `page_nodes`, `permissions`, `roles`, `tokens`, `user_profile`, `users` |
| Governed `platform.*` / `statistics.*` / `entity.*` tables with ORM ownership | **none** |

**The finding, stated plainly: Hibernate maps the legacy schema and nothing else. The entire
governed 120-table architecture is ORM-free and driven by native SQL and `JdbcTemplate`.**

Three consequences a future architect must not have to rediscover:

1. **This is a deliberate architecture, not an omission.** Contract-compiled query plans
   (`ContractPhysicalQueryService`) cannot be expressed as ORM mappings, because the schema a
   query targets is declared at runtime by a contract, not at compile time by a class.
   A canonical design that assumed JPA would contradict a correct existing choice.
2. **Legacy elimination is a code-and-schema pair.** Retiring `dbo.chart_definitions` means
   retiring `ChartDefinition.java`, its repository and its service — `DR-001`'s disposition has
   a precise physical and code inventory now.
3. **Two authorization substrates coexist**: `dbo.users/roles/permissions/role_permissions/
   user_roles/tokens` with JPA entities, alongside the OIDC/ABAC model of `ADR-010`. This is a
   trust-boundary fact, reported to its owners, not resolved here.

`ORM_TABLE_MAPPINGS_CHECKED: 12 named + 4 join/unmapped = 16 of 19`; the remainder
(`sliders_data`, `prices`, `prices_1`, `auto_eoes`, `main`) are reached by the legacy Georgian
controllers through native SQL on the **primary** datasource — i.e. legacy *data* living in the
new control-plane database.

---

## 4. The six previously unclassified structures — resolved

| Structure | Rows | Enforcement | Classification |
|---|---|---|---|
| `geo.feature` | **0** | PK; FK → `raw.source_record`; published-immutable trigger | **SOUND BUT SPECIALIZED, UNEXERCISED** — a complete geo family that has never held a row |
| `geo.feature_link` | **0** | PK `(feature_id, relationship_type_id)`; FK → `geo.feature` | same |
| `serving.metric_cache` | **1 045** | PK; UNIQUE; FK → `publication.snapshot` (`NO_ACTION`) | **DERIVED / CACHE, IN USE** — snapshot-keyed, so it is invalidated by construction rather than by a TTL |
| `audit.event` | **0** | PK | **PROVEN STRUCTURE, UNEXERCISED** — audit infrastructure exists and records nothing |
| `audit.access_decision` | **0** | PK | same — material for `SEC-001`/`OBS-001` |
| `archive.*` (5 tables) | `record` 6 325 · `payload_pointer` 6 325 · `artifact_reference` 454 · `snapshot` 3 · `retention_register` **0** | PKs; FK → `archive.snapshot` | **PROVEN STRENGTH, IN USE** — except `retention_register`, which is empty, so retention is declared and not exercised |

**`STRUCTURES_CLASSIFIED: 139/139`.** Three classes carry a warning rather than a defect:
*structurally complete and never exercised* (`geo`, `audit`, `retention_register`, classifier
hierarchy). Unexercised is not the same as wrong, and it is not the same as proven.

---

## 5. Volume and workload evidence

**Measured live** — no production figures are invented, and dev is dev.

| Table | Rows | What it tells the architect |
|---|---|---|
| `ingest.staged_row` | 28 645 | staging is the highest-volume table; bulk ingestion is real |
| `statistics.observation_dimension` | 13 928 | **1.23 dimension rows per observation** — the vertical dimension model's true cost |
| `raw.source_record` | 13 165 | raw is retained at scale, not sampled |
| `platform.dataset_version` | **11 690** | **anomaly** — this is a *control-plane* table with more rows than there are observations. Consistent with the non-idempotent restart DML already recorded in the AIR register |
| `statistics.observation` | 11 288 | the canonical fact table |
| `archive.record` / `payload_pointer` | 6 325 / 6 325 | 1:1 — the archive keeps a pointer per record |
| `entity.localized_text` | 2 822 | localization is used heavily: ~1.6 rows per entity |
| `entity.resource_locator` | 2 466 | as is resource location |
| `entity.entity_record` | 1 767 | the entity plane is modest |
| `serving.metric_cache` | 1 045 | the cache is populated |

**33 of 139 tables are empty.** Empty is evidence of *unexercised capability*, and this dossier
records which rather than averaging it away.

**Unknown and marked so:** production volumes, query latencies, hot paths under load, batch
sizes in production. `BM-Q-03` remains unresolved and nothing here substitutes for it.

---

## 6. Behaviour hidden below the application

### 6.1 Triggers — 20 live (9 control plane, 11 data plane)

Three families, all enforcing **write-once semantics the application does not have to
implement**: `*_published_immutable` (a row inside a published snapshot cannot be updated or
deleted), `*_approved_immutable` (an approved declaration is frozen), `*_append_only` (history
cannot be rewritten). Full definitions in `live-schema-full.txt`.

### 6.2 Modules — 2, both in `geostat-data`

`publication.usp_kids_r8_reconciliation` (procedure) and
`entity.v_artifact_attachment_reconciliation` (view). **Both are reconciliation**, i.e. the
database is where cross-table agreement is checked.

### 6.3 Contradiction with recovery evidence — preserved, precisely bounded

`REC-CONSOLIDATION` RC-003 states, as one of five falsification axes, *"stored procedures
(**none exist** in 113 migrations)"*. **False**: the procedure above is created by migration
`073` and altered by `075`.

- **RC-003's conclusion survives** — a reconciliation report produces no registry row, so
  "canonical registries have no runtime producer" still holds on the other four axes.
- **The supporting statement is wrong and must be corrected in its owner.** Routed to
  `REC-CONSOLIDATION`; recorded here so the correction cannot be lost.
- **`DRAFT_BASELINE_CONTRADICTIONS_FOUND` counts this separately from the draft contradictions
  of §8**, because it falsifies *recovery* evidence, not design.

---

## 7. Integrity that exists only in application code

| Invariant | Where it should be | Where it actually is |
|---|---|---|
| `INV-006` one observation per dimension tuple and measure | a unique constraint | **application only** — the PK is a surrogate and dimensions live in a child table, so no constraint can express it (`ARCH-BASELINE` §6.2) |
| `CF-040` declared relation cardinality | a unique constraint on `entity.entity_link` | **nowhere** — `link_id` surrogate PK, no uniqueness on the endpoint pair |
| `RC-001` liveness equivalence (membership ≡ status) | a constraint or reconciliation | **nowhere** — no trigger, no constraint, no job |
| `CF-018` scoped, version-pinned code resolution | a foreign key to an exact version | **application only** in the affected paths |

**`APPLICATION_ONLY_INTEGRITY_RULES_FOUND: 4`**, and each is already a recorded finding — the
dossier's contribution is proving they are *absent from the physical layer*, which is what makes
them regression-relevant rather than merely known.

---

## 8. Anti-regression matrix

**No canonical decision may remove a row from this table without an explicit, evidenced
justification.**

| # | Physical mechanism | Guarantee | Enforcement | Dependent capability | Must survive |
|---|---|---|---|---|---|
| 1 | 139 primary keys | every row is addressable | engine | everything | **yes** |
| 2 | 135 foreign keys, all `NO_ACTION` | referential integrity **without silent cascade deletion** | engine | lineage, membership, classification | **yes — including the absence of cascades** |
| 3 | 236 PK/UNIQUE constraints | identity and candidate keys | engine | grain, idempotency | **yes** |
| 4 | 115 CHECK constraints (39 enumerated domains) | closed value domains, formats, ranges, conditional nullability | engine | state machines, digest validity | **yes — may move to FK-to-version, never to convention** |
| 5 | 7 filtered unique indexes | "one approved/active/open per X" state invariants | engine | approval, idempotency, audit-issue uniqueness | **yes** |
| 6 | 20 triggers | published rows immutable; approved declarations frozen; history append-only | engine | reproducibility, audit, `BM-INV-23` | **yes** |
| 7 | `statistics.observation.valid_from/valid_to/is_current` | **application-time versioning of a fact independent of publication** | columns + application | correction vs restatement; non-snapshot history | **capability yes** — implementation may change if the guarantee is proven preserved |
| 8 | `entity.localized_text` PK `(entity, field, language)` | typed, keyed multilingual text | engine | localization at 2 822 rows | **yes** |
| 9 | `entity.resource_locator` PK `(entity, kind, language)` | typed locators per entity per kind per language | engine | resource resolution at 2 466 rows | **yes** |
| 10 | `entity.entity_classification` PK `(entity, attribute, item)` | classification as a keyed relation | engine | classifier integrity | **yes** |
| 11 | `reference.classification_item_snapshot` PK `(snapshot, item)` | **published snapshots pin codelist content** | engine | reproducibility of a published output | **yes** |
| 12 | `publication.snapshot_member` PK `(snapshot, dataset_version)` | membership is the release boundary | engine | `BM-INV-12` | **yes** |
| 13 | `ingest.artifact_object` UNIQUE `sha256`, UNIQUE `(bucket, object_key)`, CHECK hex-64 | **content identity is the checksum**; one object per content; location is separate | engine | `BM-INV-22`, `AQ-05`'s lesson | **yes** |
| 14 | `platform.contract_structure` UNIQUE `(namespace, code, revision)` + `data_class`, `grain`, `authority_mode`, `lifecycle_policy`, policy refs, `standard_code` | **a structure/grain/authority/policy model already exists** | engine + declaration | the kernel's own responsibility | **yes — the kernel is its evolution** |
| 15 | `platform.site_contract_revision` as a 12-inbound-FK hub | everything binds to an **exact** contract revision | engine | `BM-INV-06` | **yes** |
| 16 | `DECIMAL` numeric envelope (`28,10` → widened by `111/112`) | exact decimal, no float in the canonical path | engine | `BM-INV-09`, `DR-006` | **yes** |
| 17 | `serving.metric_cache` FK → `publication.snapshot` | a cache keyed by the thing that invalidates it | engine | correctness of cached reads | **yes** |
| 18 | `archive.record` ↔ `payload_pointer` 1:1 + FK → `archive.snapshot` | payload separable from record | engine | retention, and the erasure architecture of the draft §22 | **yes** |
| 19 | 0 cascade deletes anywhere | deletion is governed, never implicit | engine | `INV-012`, legacy-removal safety | **yes** |

**`ANTI_REGRESSION_GUARANTEES_RECORDED: 19`.**

---

## 9. Draft ↔ baseline contradiction matrix

`ARCH-CANONICAL` is `BLOCKED`. Every material concept in it, judged against physical evidence:

| Draft concept | Verdict | Evidence |
|---|---|---|
| `K-04/K-06` structure + grain + policy | **ALREADY EXISTS PHYSICALLY** | `platform.contract_structure` carries structure, data class, grain, authority mode, lifecycle policy, policy refs, `standard_code`. The kernel is an **evolution**, and the draft must stop implying novelty |
| Grain enforced by a business-key constraint | **STRENGTHENED BY BASELINE** | today the observation PK is surrogate and `INV-006` is application-only; the draft makes it enforceable — a genuine improvement, with a real migration cost the draft never stated |
| §11 "temporality needs no mechanism; snapshots suffice" | **CONTRADICTED / WOULD REGRESS** | `statistics.observation.valid_from/valid_to/is_current` exists and gives non-snapshot-bound history |
| §4 six families | **INCOMPLETE** | localization, resource locators, classification assignment and snapshot-pinned codelists are existing typed patterns the family model does not express |
| §8 metadata homes | **CONFIRMED** | no generic assertion table exists server-side; the 39 enumerated CHECKs show domains are enforced, not asserted |
| §7 two stored / one derived relationships | **CONFIRMED** | 135 FKs (composition), derivation via `source_record_id` chains, and **no** dependency table anywhere — the draft's "computed, not stored" matches reality |
| §5 content identity is the checksum | **CONFIRMED, STRONGLY** | `ingest.artifact_object` enforces it with a UNIQUE and a hex-64 CHECK |
| §10 raw retained, canonical re-derivable | **CONFIRMED** | `raw.source_record` 13 165 rows with inbound FKs from every instance plane |
| §17 provider capability, one registry | **UNRESOLVED** | `platform.provider_capability` exists; whether one producer writes it is `RC-003`'s open question, unchanged |
| §16 materialization separate from resource | **INCOMPLETE** | `ingest.artifact_object` separates content from location correctly; no table separates a *logical resource* from its materializations |
| §21 disclosure evaluated at the publication gate | **UNRESOLVED** | no physical mechanism for complementary suppression exists — consistent with `CAP-M12`, neither confirmed nor refuted |
| §22 erasable payload separable | **CONFIRMED** | `archive.record` ↔ `archive.payload_pointer` already separates them |
| §28 projections without parallel semantics | **CONTRADICTED IN PRACTICE** | `dbo.chart_definitions` and `dbo.page_nodes` exist physically with ORM ownership — a second presentation substrate the draft must explicitly exclude |

**`DRAFT_BASELINE_CONTRADICTIONS_FOUND: 6`** (1 contradicted-and-would-regress, 1 contradicted
in practice, 3 incomplete, 1 already-exists overstatement) **+ 1 recovery-evidence
contradiction** (§6.3).

---

## 10. Anomaly and debt register

| # | Anomaly | Evidence |
|---|---|---|
| 1 | `platform.dataset_version` holds 11 690 rows — more than there are observations | live count; consistent with recorded non-idempotent restart DML |
| 2 | 33 of 139 tables are empty, including the whole `geo` family, both `audit` tables and `retention_register` | live counts |
| 3 | Two authorization substrates in one database (`dbo.*` JPA + OIDC/ABAC) | ORM mapping + `ADR-010` |
| 4 | Second migration ledger `dbo.migrations` alongside `platform.schema_migration` | live catalogue + `Migration.java` |
| 5 | Observation grain unenforceable in the current shape | surrogate PK + child dimension table |
| 6 | `entity.entity_link` has no endpoint uniqueness — cardinality unenforced | live keys |
| 7 | No physical mechanism reconciles membership with `dataset_snapshot.status` | no trigger, constraint or job found |
| 8 | Audit tables exist and are empty | live counts |
| 9 | `auto` configured but unreachable; a health probe depends on it | TCP test + `HealthController` |
| 10 | Legacy Georgian domain data (`prices`, `auto_eoes`, `main`, …) lives in the **new** control-plane database | live catalogue |

**`PHYSICAL_ANOMALIES_FOUND: 10`.** None was fixed; this is an evidence pass.

---

## 11. Falsification of this dossier

| Attack | Result |
|---|---|
| Can a table exist live without being accounted for? | **No** — 139/139 enumerated from `sys.tables` across all reachable databases |
| Can executable code reference a structure outside the chain? | **Yes, and it does** — 16 of the 19 `dbo.*` tables have ORM mappings (§3). Found, not assumed |
| Can an FK-like invariant exist only in Java? | **Yes** — 4 recorded in §7 |
| Can a trigger or procedure hide semantics? | **Checked** — all 20 triggers and both modules captured with definitions |
| Can a migration-era structure be mistaken for current? | **No** — the live catalogue is authoritative and every chain table was confirmed present |
| Can one semantic concept use inconsistent physical types? | **Not yet answered** — 1 234 columns are captured with types, but a cross-table type-consistency sweep was **not** run. Recorded as unresolved |
| Can a current table have unknown ownership? | **No for the 139** — every table is in a named schema with a known plane; ORM ownership resolved |
| Can the new architecture still depend on `auto`? | **Yes, once** — a health probe. Measured |
| Can a strong guarantee be missing from the anti-regression matrix? | **Possible** — the matrix has 19 rows built from constraints, triggers and indexes; a guarantee expressed only in a CHECK *predicate* could be missed. Bounded, not eliminated |
| Can an Access capability have no SQL correspondence? | **Not answered here** — `SQL_ACCESS_CORRESPONDENCE_MAPPED: NO`. `PHASE-003` mapped the Access side; the SQL↔Access correspondence table is a remaining obligation |
| Can the blocked draft still remove a proven guarantee? | **Yes — and §9 names exactly where** |

---

## 12. What remains unresolved

| # | Unresolved | Why it cannot be closed here |
|---|---|---|
| 1 | Contents and semantics of `auto` | network-unreachable; no credentialed route exists from the dev host |
| 2 | SQL ↔ Access correspondence table | `PHASE-003` mapped Access; the pairing is a bounded follow-up |
| 3 | Cross-table physical type consistency sweep | data captured (1 234 columns), analysis not run |
| 4 | Per-table narrative dossier for all 139 | the machine-readable dossier is complete; prose exists for the ~40 architecturally material tables |
| 5 | Production volumes and hot paths | dev only; `BM-Q-03` unresolved |
| 6 | Whether `geo`, `audit` and `retention_register` are intended or abandoned | structurally complete, zero rows, no code evidence either way |

**`UNRESOLVED_PHYSICAL_SEMANTICS: 6`.** Each is named, bounded and assigned — none is hidden
behind an average.
