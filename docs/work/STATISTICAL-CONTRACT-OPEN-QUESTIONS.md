# სტატისტიკური კონტრაქტი — გადაწყვეტილებების რეესტრი (Q01–Q50)

თარიღი: 2026-09-19  
სტატუსი: **50 კითხვიდან 50-ს გადაწყვეტილება ჰყავს. იმპლემენტაცია dev runtime-ზე: [checklist](STATISTICAL-CONTRACT-IMPLEMENTATION-CHECKLIST.md). Release NOT READY.**  
მთავარი დოკუმენტი: [საერთო გეგმა](COMMON-STATISTICAL-CONTRACT-PLAN.md).  
პროცესი: [ინიციალიზაცია და timeline](STATISTICAL-CONTRACT-LIFECYCLE.md).  
Access-ის ორგანიზება: [package-ის გეგმა](ACCESS-PACKAGE-ORGANIZATION-PLAN.md).

გადაწყვეტილება შესრულება არ არის. ეს დოკუმენტი დიზაინის authority-ა. `DONE` ჩაიწერება მხოლოდ reproducible ან runtime evidence-ის ბმულით.

## 0. წესები

### 0.1 ჩანაწერის ფორმა

**ID → გადაწყვეტილება → owner → დასაბუთება / სტანდარტი → contract / ADR → test / evidence → სტატუსი.**

ცხრილებში `დასაბუთება` მიუთითებს §1-ის ფაქტზე (`F#`) ან §7-ის სტანდარტზე (`S#`).

### 0.2 სტატუსის vocabulary (closed)

| სტატუსი | მნიშვნელობა |
|---|---|
| `DECIDED` | საინჟინრო გადაწყვეტილება მიღებულია; evidence იმპლემენტაციის ეტაპზე |
| `DEFAULT` | უსაფრთხო default მიღებულია; კონკრეტულ მნიშვნელობას `OWNER` ან `STEWARD` ადასტურებს |
| `PER-DATASET` | წესი მიღებულია; მნიშვნელობა ივსება dataset-ის ბარათში (§3.2) |
| `DONE` | გადაწყვეტილება + evidence-ის ბმული |

`DEFAULT`-ის შეცვლა იცვლის მნიშვნელობას, არ იცვლის მექანიზმს. შეთანხმებული მიმართულება ძალაში რჩება.

### 0.3 Owner-ის როლები

`OWNER` — პროექტის მფლობელი. `STEWARD` — დარგის / მეთოდოლოგიის პასუხისმგებელი. `ENG` — საინჟინრო გუნდი. `SEC` — უსაფრთხოება.

### 0.4 გადაწყვეტილების პრინციპები

1. **Reuse before create.** იდენტური მნიშვნელობის მეორე registry არ იქმნება.
2. **One authority.** თითო სემანტიკა ერთ ადგილას განისაზღვრება; დანარჩენი projection-ია.
3. **Closed grammar, open registry.** გაფართოება — registry-ის ჩანაწერით, არ — core-code branch-ით (OCP).
4. **Fail closed.** მხარდაჭერის გარეშე შესაძლებლობა — explicit rejection; ჩუმი fallback არ არსებობს.
5. **Lossless.** decimal, period, null/status — დანაკარგის გარეშე.
6. **Deny by default.** tenant / role / confidentiality — server-ზე, execution-time.
7. **Ports and Adapters.** Access, SDMX, CSV — adapter-ები; სემანტიკა provider-agnostic (DIP).

## 1. Evidence base — წაკითხული კოდის ფაქტები

Source review, 2026-09-19, branch `security/hardening-2`. Runtime აუდიტი baseline ეტაპის ნაწილია.

| # | ფაქტი | წყარო |
|---|---|---|
| F1 | Control registry უკვე არსებობს: `platform.dimension`, `platform.measure`, `platform.metric`, `platform.classification_scheme/version/item/hierarchy/alias` | [001](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/001_control_plane.sql) |
| F2 | DSD registry უკვე არსებობს: `platform.statistical_dataflow`, `statistical_dsd` (`revision`, `observation_grain`), `statistical_component` (`component_role`, `component_order`, `attachment_level`, `constraint_json`) | [022](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/022_kids_complete_site_contract.sql) |
| F3 | Unit / policy registry: `platform.statistical_unit` (`scale_factor DECIMAL(28,10)`, `denominator_text`), `data_quality_policy`, `confidentiality_policy`, carrier-bound `statistical_semantic_binding` | [023](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/023_kids_inferred_statistical_semantics.sql) |
| F4 | Canonical storage long-form: `statistics.series(dataset_snapshot_id, metric_id, series_key_hash, unit_code)` → `statistics.observation(numeric_value DECIMAL(28,10), text_value, boolean_value, observation_status, period_start, period_end)` → `observation_dimension`, `observation_attribute(value_json)` | [002](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/002_data_plane.sql) |
| F5 | Runtime writer `SemanticMaterializationService` ავსებს `period_start` + `numeric_value`-ს. `observation_status`, `period_end`, `observation_attribute` — არ ავსებს | [service](../../platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/SemanticMaterializationService.java) |
| F6 | Access generator — Jackcess. `value_decimal`, `scale_factor` — `DataType.DOUBLE` | [generator](../../ops/scripts/java/KidsPortalCanonicalAccessPackageGenerator.java) |
| F7 | Contract lifecycle enum: `DRAFT, REVIEW_REQUIRED, APPROVED, SUPERSEDED, ROLLED_BACK`. Approval receipt მიბმულია `contract_checksum`-ზე | [084](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/084_contract_revision_lifecycle.sql), [082](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/082_contract_approval_receipt.sql) |
| F8 | Durable ledger: `platform.api_operation` (`QUEUED…CANCELLED`, unique `requested_by + idempotency_key`, `request_hash`), `platform.outbox_event`, `platform.job_lease` | [076](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/076_api_operation_ledger.sql), [079](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/079_api_operation_idempotency.sql), [080](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/080_api_operation_owner_scope.sql) |
| F9 | Resumable import: `ingest.artifact_package_run` (`PENDING, RUNNING, RETRYABLE, BLOCKED, COMPLETED`) + append-only stage history; `ingest.load_checkpoint`; unique `(batch, dataset_version, source)` | [101](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/101_artifact_package_run.sql), [015](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/015_resumable_access_ingest.sql), [018](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/018_package_ingest_idempotency.sql) |
| F10 | `platform.provider_capability` registry არსებობს: `provider / family / operation / feature`, `max_page_size` | [081](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/081_provider_capability_registry.sql) |
| F11 | Tenant scope: product-ს ზუსტად ერთი owner tenant. `TenantAccessPolicy` + `TenantAccessGuard`, deny by default | [ADR-010](../decisions/ADR-tenant-scoped-authorization.md) |
| F12 | SDMX export-ები (`SDMX_JSON 2.0.0`, `SDMX_XML 2.1`) — SDMX-shaped facade; conformance evidence არ არსებობს | [codec registry](../../platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ExportCodecRegistry.java) |
| F13 | Localized metadata BCP 47 `language_tag`-ით: `platform.metadata_assertion`, `entity.localized_text`. Registry-ის ძველ ცხრილებში — fixed `title_ka / title_en` | [062](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/062_universal_metadata_plane.sql), [006](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/006_entity_localization_and_locator.sql) |
| F14 | Classifier proposal workflow: `platform.classifier_proposal` | [019](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/019_classifier_proposal_registry.sql) |
| F15 | Retention: `archive.retention_register` | [036](../../platform/apps/geostat/backend/core/src/main/resources/db/platform/036_final_archive_completion.sql) |

## 2. A — პროდუქტი და სამუშაო პროცესი

| ID | გადაწყვეტილება | Owner | დასაბუთება | Evidence | სტატუსი |
|---|---|---|---|---|---|
| Q01 | v1 — მხოლოდ aggregated statistics. Microdata — ცალკე profile (`privacy / identity`), v1 compiler-ში explicit rejection | OWNER | გეგმა §1; S11 | negative test: microdata profile → `PROFILE_UNSUPPORTED` | `DECIDED` |
| Q02 | ვალიდაციის ნაკრები — 3 dataset: (a) KIDS legacy aggregate; (b) სხვა product-ის multi-dimension / multi-measure, განსხვავებული unit-ებით; (c) non-time dataset. (b), (c)-ის კონკრეტული არჩევანი — baseline inventory-ის შედეგი | OWNER + STEWARD | გეგმა §11.7 | dataset cards §3.2 | `DEFAULT` |
| Q03 | Target: ACCDB, Access 2016+ / Microsoft 365, 32 და 64 bit. VBA / macro-ის გარეშე (Trust Center block-ის რისკი). Generator file format — Jackcess `V2016`, არ — ძველი | ENG; inventory — OWNER | F6 | open / fill / save მატრიცა ვერსიებზე | `DEFAULT` |
| Q04 | Primary — datasheet (typed table) lookup-ებით. Form — optional, მხოლოდ template-ით (Q36). Form-ის არარსებობა ფუნკციონალს არ ბლოკავს | ENG | Q36; KISS | Q36-ის evidence | `DECIDED` |
| Q05 | `ka` — required; `en` — recommended; დანარჩენი — optional. BCP 47 tag. Language list — profile-ის parameter, არ — column | OWNER | F13; S9 | missing-`ka` rejection test | `DEFAULT` |
| Q06 | 5 გამიჯნული duty: `author`, `steward` (registry), `contract approver`, `importer`, `publisher`. ერთი ადამიანი — რამდენიმე role: დასაშვებია. Four-eyes: approver ≠ ამავე revision-ის ავტორი; publisher ≠ ამავე load-ის importer. Small-team exception — explicit, audited policy flag | OWNER + SEC | F11; S10 (SoD) | role matrix + unauthorized-transition negative suite | `DEFAULT` |
| Q07 | Registry entry-ს `scope`: `GLOBAL` ან `PRODUCT`. Cross-domain standard (time, geo, sex, age, obs / conf status, unit multiplier) — `GLOBAL`, owner — platform steward. დარგის სპეციფიკა — `PRODUCT`, owner — product steward. Promotion `PRODUCT → GLOBAL` — steward review | STEWARD | F1; S1 (cross-domain codelists) | ownership matrix; duplicate-semantics review | `DECIDED` |
| Q08 | Dataset-ს ერთი owner product ⇒ ერთი tenant. Site — consumer; ამავე tenant-ის რამდენიმე site: დასაშვებია binding-ით. Cross-tenant: default DENY; მხოლოდ explicit, audited grant ADR-010-ის ჩარჩოში | SEC | F11 | tenant-negative suite | `DECIDED` |
| Q09 | Author ქმნის proposal; approved registry პირდაპირ არ იცვლება. Contract draft proposal-ზე ref-ით შეიძლება; approval — blocked, until proposal `APPROVED` | STEWARD | F14 | approval-with-draft-ref rejection test | `DECIDED` |
| Q10 | Default — `ATOMIC_REJECT`: ერთი blocking error ⇒ load rejected. `QUARANTINE_ROWS` — opt-in contract-ში; partial load UI-ში ხილული, publication — blocked until quarantine resolved | STEWARD | aggregated tables-ის consistency (totals) ; გეგმა §10 | both-mode tests; silent-drop = 0 | `DEFAULT` |
| Q11 | Mutation mode — explicit, load envelope-ში: `REPLACE_SCOPE` (declared dimension slice-ის full replacement), `UPSERT` (default), `DELETE` (explicit rows + permission + audit). Absent row ≠ delete | ENG + STEWARD | გეგმა §10 | mode-per-mode semantic diff tests | `DECIDED` |
| Q12 | Confidentiality — declared attribute + policy ref, dataset / measure level. Default `conf_status` — NOT generated. Embargo — snapshot publication gate, `publish_not_before`. Policy-ის არარსებობა ⇒ dataset non-public | STEWARD + SEC | F3; S1 (`CL_CONF_STATUS`) | confidentiality-negative suite | `DEFAULT` |
| Q13 | v1 export: `CSV` (platform), `SDMX-CSV 2.0`, `SDMX-JSON 2.0`. `SDMX-ML` — deferred, until named consumer. Access — authoring artifact, არ — public export | OWNER | F12; S1; YAGNI | Q47 conformance matrix | `DEFAULT` |
| Q14 | Budget — per load, design ceiling (measured, არ — assumed): ≤ 200 000 wide rows; ≤ 20 dimensions; ≤ 50 measures; artifact ≤ 250 MB; validation p95 ≤ 120 s; import p95 ≤ 15 min; parallel loads per product = 1. Ceiling-ის ზემოთ — explicit rejection | ENG; მოცულობა — OWNER | Access 2 GB limit; shared host memory incident (AIR-2026-046) | load test report, memory-bounded | `DEFAULT` |
| Q15 | Parallel run: minimum 2 successful release cycles ან 90 days — რომელი გვიან მთავრდება. Exit: consumer inventory = 0 legacy consumer + OWNER sign-off. ძველი adapter — read-only deprecation window-ში | OWNER | გეგმა §11.6–8 | consumer inventory; sign-off record | `DEFAULT` |
| Q16 | მოქმედი `archive.retention_register` policy. Source artifact, approved contract, published snapshot, audit — immutable, retention-governed. Draft — deletable მხოლოდ never-submitted. Change — separate approval | OWNER | F15 | retention register entries | `DECIDED` |

## 3. B — სემანტიკა

Platform-level წესი მიღებულია ქვემოთ. Dataset-level მნიშვნელობას STEWARD ავსებს dataset card-ში (§3.2). Card-ის გარეშე contract `APPROVED` ვერ გახდება.

### 3.1 Platform-level წესები

| ID | გადაწყვეტილება | დასაბუთება | სტატუსი |
|---|---|---|---|
| Q17 | One wide row = one dimension tuple. Canonical-ში: one observation per (tuple × measure). Unique key = DIMENSION-role component-ების ordered set. Key-ში measure და attribute არ შედის | F2, F4; S1 | `PER-DATASET` |
| Q18 | Same-grain test: measure-ს exactly same dimension set + same population scope. Dimension `N/A` ამ measure-ში ⇒ different grain ⇒ separate dataset + approved relation | გეგმა §9 | `PER-DATASET` |
| Q19 | SDMX time formats: `YYYY`, `YYYY-S#`, `YYYY-Q#`, `YYYY-M##`, `YYYY-W##`, `YYYY-MM-DD`, `start/duration`. Storage: deterministic `period_start / period_end`; lexical original — lineage. Non-time DSD: `TIME_PERIOD` absent, both NULL. One DSD — one declared frequency set | F4, F5; S1, S2 | `DECIDED` |
| Q20 | Dimension default — coded (codelist). Uncoded — მხოლოდ `TIME_PERIOD` ან explicit typed representation (integer / bounded text) facet-ებით. Free text as dimension — rejected | F4 (`scalar_code`); S1 | `DECIDED` |
| Q21 | Attachment levels (closed enum): `DATASET`, `DIMENSION_GROUP`, `OBSERVATION`, `MEASURE`. Authoring table-ში — მხოლოდ `OBSERVATION / MEASURE`. `DATASET / DIMENSION_GROUP` — contract metadata, no row repetition | F2 (`attachment_level`); S1 | `PER-DATASET` |
| Q22 | Value + status = pair. NULL value requires status. `CL_OBS_STATUS`-aligned codelist: zero = numeric `0` (real value); missing = `M` / `O`; not applicable = dedicated code; suppressed = value withheld + `CL_CONF_STATUS`. Empty cell without status — blocking error. Generator default status-ს არ იგონებს | F4, F5; S1 | `DECIDED` |
| Q23 | Measure definition-ის typed, versioned fields: `unit_ref`, `unit_mult`, `decimals`, `precision / scale`, `rounding` (default `HALF_EVEN`, explicit), `denominator_concept_ref`, `base_period`. `denominator_text` — lineage only. Approximate (IEEE) — მხოლოდ declared | F3; S1 (`CL_UNIT_MULT`, `CL_DECIMALS`), S3 | `PER-DATASET` |
| Q24 | Aggregation — per (measure × dimension) versioned rule; default `NONE`. Additive count — `SUM` explicit. Ratio / index / average — never `SUM`. Total / subtotal — hierarchy code-ები; overlapping group — declared, `SUM` blocked | F1 (`aggregation_default`); გეგმა §9 | `PER-DATASET` |
| Q25 | System-derived value — მხოლოდ approved, versioned derivation rule: closed operator set, acyclic dependency, declared inputs. Free expression / SQL — rejected. Derived observation lineage-ში rule revision | გეგმა §6, §9 | `DECIDED` |
| Q26 | Codelist version — pinned in DSD. New version ⇒ new DSD revision. Old → new — machine-readable crosswalk, cardinality typed (`1:1`, `1:n`, `n:1`); `1:n` — no automatic migration. Alias — adapter-only | F1; S11 | `DECIDED` |
| Q27 | Valid combinations — declarative constraint (allowed / excluded cube regions), SDMX content-constraint model. Quality thresholds — `data_quality_policy` ref. Missing combination policy — explicit: `ALLOWED` ან `REQUIRED_COMPLETE` | F2 (`constraint_json`), F3; S1 | `PER-DATASET` |
| Q28 | Merge requires ყველა: same concept, unit, population, methodology, time basis + STEWARD sign-off. Name similarity — not evidence. Ambiguity ⇒ separate measures + crosswalk note | გეგმა §8.2 | `PER-DATASET` |

### 3.2 Dataset card — STEWARD-ის შესავსები template

`docs/work/evidence/statistical-contract/<dataset_code>/dataset-card.md`

| ველი | კითხვა |
|---|---|
| Grain sentence | Q17 |
| Dimensions: code, concept, codelist + version, required | Q17, Q20, Q26 |
| Measures: ref, unit, unit_mult, decimals, rounding, denominator, base period | Q18, Q23 |
| Time: formats / frequency ან `NON_TIME` | Q19 |
| Attributes + attachment level | Q21 |
| Status codelists + null policy | Q22 |
| Aggregation matrix (measure × dimension) | Q24 |
| Derivations | Q25 |
| Constraints + completeness policy | Q27 |
| Legacy metric crosswalk + merge evidence | Q28 |
| Confidentiality / embargo | Q12 |
| Expected volume | Q14 |
| Error mode, mutation mode | Q10, Q11 |

## 4. C — ინჟინერია

| ID | გადაწყვეტილება | დასაბუთება | Test / evidence | სტატუსი |
|---|---|---|---|---|
| Q29 | Physical mapping — §4.1. New tables: only `measure ↔ component` semantic gap-ები; new registry — 0 | F1–F3, F13 | migration review; duplicate-authority audit | `DECIDED` |
| Q30 | Reference wire syntax — §4.2. `latest`, range, wildcard — rejected. Dependency graph — resolved closure, pinned at approval | S1 (URN model), S4 | resolution + cycle + cross-tenant negative tests | `DECIDED` |
| Q31 | JSON Schema 2020-12: `oneOf[existingStructureRef, inlineStructureDraft]`, `unevaluatedProperties: false`. Attachment — tagged union per level; `DIMENSION_GROUP` requires non-empty dimension subset ⊂ DSD | S5 | negative corpus: both / neither / unknown field / bad subset | `DECIDED` |
| Q32 | Measure component-ში serialized: `measureRef` only. `concept`, `representation`, `unit` — derived, wire-ში forbidden. Resolved values — only in normalized plan (read-only projection). Plan §6-ის alternative — closed | One authority; SRP | schema rejects redundant field; plan equality test | `DECIDED` |
| Q33 | Multi-measure — **supported without new storage model**: measure identity = `series.metric_id`; status / attribute — per observation = per measure. Gaps — §4.3 | F4, F5 | multi-measure round-trip + per-measure status test | `DECIDED` |
| Q34 | Access numeric — Jackcess `DataType.NUMERIC` (Decimal), precision ≤ 28 = SQL `DECIMAL(28,10)` envelope. `DOUBLE` — მხოლოდ declared approximate. Period — TEXT, SDMX lexical, server-validated. Status — coded TEXT. Read path — `BigDecimal`, no double hop | F4, F6; S3 | boundary corpus: 28 digits, scale 10, negative, trailing zeros, overflow rejection; round-trip equality | `DECIDED` |
| Q35 | Naming + limits — §4.4 | F10; Access limits | collision corpus; capability report snapshot | `DECIDED` |
| Q36 | Jackcess PropertyMap: field `Caption`, `Description`, lookup (`DisplayControl`, `RowSourceType`, `RowSource`, `BoundColumn`, `ColumnWidths`, `LimitToList`). Navigation Pane groups, startup view, forms — Jackcess API არ ფარავს ⇒ versioned **template ACCDB** (authored once in Access, checksum-pinned, no VBA), generator copies + fills. Template-ის გარეშე degradation: tables + lookups work, groups absent — not blocking | F6; Q03, Q04 | real Access open / select / save evidence per supported version; template digest in build manifest | `DECIDED` — feasibility spike required first |
| Q37 | Identity = natural dimension tuple. `row_ref` generated only: (a) dimensions > 10 (Access index limit — unique index impossible); (b) `DELETE / UPSERT` lineage needs stable source key. `row_ref` — never business identity, never in semantic key | Access index ≤ 10 fields; გეგმა §9 | duplicate-tuple rejection with and without `row_ref` | `DECIDED` |
| Q38 | `constantBindings[]: {component, value}` — contract-ში, not file-ში. Compiler expands deterministically. Column + constant for same component — rejected. Override — only declared `overridable: true`, row value wins, recorded in lineage. Default ≠ constant: default — only attributes, explicit | გეგმა §7 | expand / collapse round-trip equality | `DECIDED` |
| Q39 | Canonical form — RFC 8785 (JCS); decimals — JSON strings (no IEEE); NFC text; semantic-unordered sets sorted by code; component order preserved. `SHA-256`, domain-separated prefix `geostat.stat-contract.v1`. Two digests: `semanticDigest` (no presentation) + `revisionDigest` (all). ACCDB binary checksum — build-specific, recorded, not compared | S6, S7; F7 | same input × 2 runs × 2 JVM ⇒ equal digest; caption change ⇒ only `revisionDigest` moves | `DECIDED` |
| Q40 | One authoring model → one compiler; UI and API — thin clients. State mapping — §4.5; new enum value — 0. Concurrency: strong `ETag` = draft version; `If-Match` required (`428` absent, `412` stale). Approval receipt binds `revisionDigest`; post-review edit ⇒ receipt void | F7; S8 | transition matrix tests; stale-approval rejection | `DECIDED` |
| Q41 | One transaction: draft + bindings + `api_operation` + `outbox_event`. Durable job: generation, import, export. External IO inside DB transaction — forbidden. Cross-plane (Control ↔ Data) — outbox, no distributed transaction | F8, F9; S12 | crash-between-steps tests; atomic init test | `DECIDED` |
| Q42 | Keys — §4.6. Same key + different `request_hash` ⇒ `409`. Same key + same hash ⇒ stored result | F8, F9; S13 | duplicate / conflicting / concurrent request tests | `DECIDED` |
| Q43 | Retry only transient; exponential backoff + jitter; max attempts — deployment policy; then `BLOCKED`. Schema / authorization / validation error — never retried. Checkpoint — stage + row number (F9). Temp artifacts — job-scoped prefix, cleanup only own prefix; orphan — existing storage sweep. Memory-bounded streaming mandatory | F9; AIR-2026-046 | kill-at-each-stage recovery tests; bounded-heap load test | `DECIDED` |
| Q44 | Enforcement points: (1) API boundary — `TenantAccessGuard`; (2) reference resolution — every ref scope-checked; (3) worker — re-check at execution with stored initiator identity, not `SYSTEM` bypass; (4) serving — snapshot-bound. Confidentiality downgrade — separate authority | F11; S10 | tenant-negative + permission-revoked-mid-job + downgrade-negative suite | `DECIDED` |
| Q45 | SemVer for DSD / contract — §4.7. Existing `compatibility_mode` enum reused | F2 (022 `compatibility_mode`); S4 | compatibility matrix + classifier tests | `DECIDED` |
| Q46 | Machine-readable crosswalk: legacy `metric / carrier / input_key` → `measure_ref / dataset / tuple`, with `mapping_kind`, evidence ref, steward. Semantic diff: values (exact decimal), units, dimensions, statuses, relations, lineage. Row count alone — not acceptance. Ambiguity ⇒ blocked merge | გეგმა §8, §12 | reconciliation report, zero unexplained diffs | `DECIDED` |
| Q47 | Conformance claim — only proven. v1: Information Model profile 3.1 (subset, declared); export `SDMX-CSV 2.0`, `SDMX-JSON 2.0`. Existing facade (F12) — labelled `compatibility`, no conformance claim, until validated. Multi-measure → 2.1 — rejected (`UNSUPPORTED_CONVERSION`), no silent pivot | F12; S1 | conformance matrix; official schema validation; rejection test | `DECIDED` |
| Q48 | Build manifest: source artifact digest, contract `revisionDigest / semanticDigest`, resolved dependency closure, generator version + config, template digest, job id, W3C trace id. Audit — existing `audit.event`. Log schema: codes + ids; observation values / confidential text — never logged | S14, S15 | manifest schema test; log-scrub test | `DECIDED` |
| Q49 | Test pyramid — §4.8, mapped 1:1 to plan §12 checklist | გეგმა §12 | reports under `docs/work/evidence/` | `DECIDED` |
| Q50 | Expand → migrate → contract. Additive reviewed migrations; dev first (prod prototype rule); backup + restore + replay rehearsal; rollback = previous approved contract + routing + snapshot, evidence kept. Legacy adapter retirement — Q15 exit + [legacy retirement governance](../legacy-retirement-governance.md) | project mandate; S16 | rehearsal record; consumer sign-off | `DECIDED` |

### 4.1 Q29 — logical → physical mapping

| Logical object (plan §6) | Physical authority | Change |
|---|---|---|
| Structure | `platform.statistical_dsd` | reuse; `(dsd_code, revision)` = version |
| Component | `platform.statistical_component` | reuse; `attachment_level` closed enum-ზე |
| Concept (dimension) | `platform.dimension` | reuse |
| Measure definition | `platform.measure` | extend: `unit_id` FK (replaces free `unit_code`), `scale`, `rounding`, methodology refs |
| Dataset-bound measure | `platform.metric` | reuse; `source_resource` coupling → lineage relation |
| Component ↔ measure | `statistical_component.measure_id` | reuse; separate binding table — not needed |
| Unit | `platform.statistical_unit` | extend: `base_unit_id`, controlled `quantity_kind`; `denominator_text` → lineage |
| Representation | `dimension.value_type` / `measure.value_type` + `component.constraint_json` (facets) | reuse; facet schema closed |
| Codelist | `platform.classification_*` | reuse |
| Attribute attachment | `statistical_component` rows, role `ATTRIBUTE` + attachment descriptor in `constraint_json` | reuse |
| Rule binding | `data_quality_policy`, `confidentiality_policy`, `validation_rule` | reuse |
| Dataset binding | `platform.statistical_dataflow` → `dataset` | reuse; carrier-bound `statistical_semantic_binding` — legacy only |
| Labels | `platform.metadata_assertion` (BCP 47) | new entries here; `title_ka / title_en` — frozen, no new columns |
| Proposal | `platform.classifier_proposal` | reuse; generalization to measure / unit — migration design |

Open design detail, owner ENG, Contract stage: registry scope column (`GLOBAL / PRODUCT`, Q07) — additive migration.

### 4.2 Q30 — reference grammar

```text
ref        = kind ":" namespace ":" code "(" version ")"
kind       = "dsd" | "concept" | "measure" | "unit" | "codelist" | "policy" | "profile"
namespace  = ID          ; agency / owner scope, registered in platform.contract_namespace
code       = ID
ID         = ALPHA *( ALPHA / DIGIT / "_" )      ; SDMX-compatible, max 120
version    = 1*DIGIT "." 1*DIGIT "." 1*DIGIT     ; exact, immutable
```

Example: `measure:GEOSTAT:EMPLOYED(1.0.0)`. Runtime numeric ID wire-ში / package-ში არ გადადი. Approval-ზე graph = transitive closure, stored + digested (Q39).

### 4.3 Q33 — storage audit result

| Requirement | State | Action |
|---|---|---|
| N measures, one grain | supported: N series / observations | adapter: wide → long unpivot, deterministic |
| Measure-level status | column exists; writer ignores (F5) | writer fix; status codelist alignment (Q22) |
| Observation / measure attributes | table exists; writer ignores (F5) | writer fix; `value_json` schema-validated per attribute |
| Dataset / dimension-group attributes | no observation-level home needed | contract metadata, snapshot-bound |
| Period range | `period_end` exists; writer ignores (F5) | writer fix (Q19) |
| Non-time | both NULL allowed | uniqueness test without period |
| Exact decimal | `DECIMAL(28,10)` | scale > 10 ან precision > 28 — contract-time rejection |
| Semantic key | `series_key_hash` | definition aligned to plan §9; reorder-stability property test |
| Generic path | service has KIDS-shaped branches | one contract-driven strategy; no per-site branch (OCP) |

Conclusion: multi-measure-ისათვის schema migration — not required. Required: writer completion + measure / unit registry extension (§4.1).

### 4.4 Q35 — physical naming and limits

- Column name = `component_code` (already `ID`-shaped). Table name = `stat_` + structure code.
- Length > 64 ან collision (case-insensitive) ან Access reserved word ⇒ `truncate(55) + "_" + first 8 hex of SHA-256(full ref)`. Deterministic, stable across regenerations.
- Mapping `physical ↔ canonical` — only in plan; canonical identity unchanged.
- Limits, declared in `platform.provider_capability` (feature rows, not code constants): 255 columns / table; 64-char names; 10 fields / index; Decimal precision 28; 2 GB file.
- Columns > 255 ⇒ key-preserving vertical split: each part repeats all dimensions; parts joined by tuple at import; missing part row — blocking error.
- Any unmet requirement ⇒ capability report + `CAPABILITY_EXCEEDED` rejection before approval, never at generation.

### 4.5 Q40 — state mapping

| Lifecycle doc vocabulary | Existing enum (F7, F8, F9) |
|---|---|
| Contract `DRAFT` | `DRAFT` |
| Contract `IN_REVIEW` | `REVIEW_REQUIRED` |
| Contract `APPROVED` | `APPROVED` |
| Contract `DEPRECATED` | `SUPERSEDED` |
| Contract rollback | `ROLLED_BACK` |
| Generation job | `api_operation`: `QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED`; new `operation_type` value — additive migration |
| Ingestion | `artifact_package_run`: `PENDING, RUNNING, RETRYABLE, BLOCKED, COMPLETED` + stage codes |
| Snapshot | existing `publication.dataset_snapshot.status`; exact values — baseline audit |

Lifecycle doc-ის vocabulary — display names; persisted authority — existing enums.

### 4.6 Q42 — idempotency keys and concurrency scopes

| Operation | Key | Concurrency scope |
|---|---|---|
| Create draft | client `Idempotency-Key` + caller + product | product |
| Approve | `revisionDigest` | contract |
| Generate | `revisionDigest` + source profile + generator version | contract revision |
| Import | artifact digest + `revisionDigest` + mutation mode + scope | dataset (single-flight) |
| Publish | snapshot id + release id | product |

### 4.7 Q45 — compatibility classes

| Change | Class | Version |
|---|---|---|
| Caption, order, description | presentation | PATCH; `semanticDigest` unchanged |
| New optional attribute; new code in pinned-compatible codelist minor | backward compatible | MINOR |
| Add / remove dimension; change grain; remove / retype measure; unit change; attachment change; codelist major | breaking | MAJOR ⇒ `BREAKING_NEW_REVISION`, crosswalk required |
| Add measure, same grain | compatible for readers; authoring template regenerated | MINOR |

Classifier — compiler stage output; human override only upward (stricter).

### 4.8 Q49 — test and measurement map

| Layer | Content |
|---|---|
| Unit | grammar, ref parser, canonicalizer, naming, compatibility classifier |
| Property-based | key stability under reorder; encode / decode equivalence; digest determinism |
| Negative corpus | closed-grammar violations, cross-tenant refs, redundant fields, bad attachments |
| Integration | compiler → Access → fill → import → canonical → export, 3 validation datasets (Q02) |
| Adapter equivalence | same plan, different provider ⇒ same canonical observations |
| Recovery | kill at each stage; duplicate / concurrent requests |
| Security | tenant-negative, role-negative, confidentiality-negative, revoked-mid-job |
| Performance | Q14 budgets, bounded heap, off shared prod host |
| Conformance | SDMX schema validation for declared formats |
| Reconciliation | legacy ↔ new semantic diff |

## 5. რიგითობა — gates

| Gate | Before | Required `DECIDED → DONE` |
|---|---|---|
| G0 Baseline | any design freeze | Q03 inventory, Q14 volumes, Q33 runtime audit, Q36 spike |
| G1 Contract | grammar / compiler code | Q29–Q32, Q35, Q37–Q39, Q45 + dataset cards (Q17–Q28) for 3 datasets |
| G2 Workflow | generation / import code | Q40–Q44, Q10, Q11 |
| G3 Delivery + Ingestion | parallel validation | Q34, Q36, Q46, Q48 |
| G4 Release | production routing | Q47, Q49, Q50, Q15, Q16; every row `DONE` ან `N/A` + rationale |

## 6. OWNER / STEWARD-ის დასადასტურებელი default-ები

Engineering work ამ default-ებით მიმდინარეა; ცვლილება mechanism-ს არ ეხება.

| ID | Default | Confirms |
|---|---|---|
| Q02 | 2nd and 3rd validation dataset | OWNER |
| Q03 | Access 2016+ / M365 | OWNER |
| Q05 | `ka` required, `en` recommended | OWNER |
| Q06 | four-eyes + small-team exception | OWNER |
| Q10 | `ATOMIC_REJECT` | STEWARD |
| Q12 | non-public without policy | STEWARD |
| Q13 | CSV, SDMX-CSV, SDMX-JSON | OWNER |
| Q14 | volume ceilings | OWNER |
| Q15 | 2 cycles / 90 days | OWNER |

## 7. სტანდარტები

| # | Standard | Used for |
|---|---|---|
| S1 | SDMX 3.1 Information Model; SDMX-CSV 2.0, SDMX-JSON 2.0; cross-domain codelists (`CL_OBS_STATUS`, `CL_CONF_STATUS`, `CL_UNIT_MULT`, `CL_DECIMALS`, `CL_FREQ`) | Q17–Q27, Q47 |
| S2 | ISO 8601 | Q19 |
| S3 | IEEE 754 (avoidance scope); SQL exact numeric | Q23, Q34 |
| S4 | Semantic Versioning 2.0.0 | Q30, Q45 |
| S5 | JSON Schema 2020-12 | Q31 |
| S6 | RFC 8785 JSON Canonicalization Scheme | Q39 |
| S7 | FIPS 180-4 (SHA-256); Unicode NFC (UAX #15) | Q39 |
| S8 | RFC 9110 (`ETag`, `If-Match`), RFC 6585 (`428`), RFC 9457 (problem details) | Q40 |
| S9 | BCP 47 (RFC 5646) | Q05 |
| S10 | NIST SP 800-162 (ABAC); OWASP API Security Top 10 (API1, API5); separation of duties | Q06, Q44 |
| S11 | GSIM, GSBPM; ISO/IEC 11179 (metadata registry) | Q01, Q07, Q26 |
| S12 | Transactional outbox; hexagonal architecture; SOLID | Q41, §0.4 |
| S13 | IETF HTTP `Idempotency-Key` header (draft) | Q42 |
| S14 | W3C PROV-O; W3C Trace Context | Q48 |
| S15 | OWASP Logging Cheat Sheet | Q48 |
| S16 | Expand / contract (parallel change) migration pattern | Q50 |

## 8. Change log

| Date | Change |
|---|---|
| 2026-09-19 | Q11 refined after implementation: v1 load mode is `FULL_SNAPSHOT` only (the snapshot is the native immutable unit of the platform); `UPSERT / DELETE / REPLACE_SCOPE` answer `422` until a snapshot-delta design is approved. Q06 extended: `IMPORT` is a separate authority. Evidence: `docs/evidence/statistical-contract-runtime-2026-09-19.json` |
| 2026-09-19 | Open-question list → decision register. 50 / 50 decided; 9 defaults await confirmation; evidence 0 / 50 |
