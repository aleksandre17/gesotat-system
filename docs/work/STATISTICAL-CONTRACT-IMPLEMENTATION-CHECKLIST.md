# სტატისტიკური კონტრაქტი — იმპლემენტაციის checklist

თარიღი: 2026-09-19  
სტატუსი: **Contract → approval → Access file → governed load → existing release gates: DONE on dev runtime. SDMX-CSV, regression, Q14 file budgets, legacy crosswalk proposal — DONE. ღია: real MS Access (human), steward decisions for 62 legacy metrics, official SDMX validation, merge to master. Release — NOT READY.**  
Authority: [საერთო გეგმა](COMMON-STATISTICAL-CONTRACT-PLAN.md), [გადაწყვეტილებების რეესტრი Q01–Q50](STATISTICAL-CONTRACT-OPEN-QUESTIONS.md), [lifecycle](STATISTICAL-CONTRACT-LIFECYCLE.md).

წესი: `[x]` — მხოლოდ evidence-ის ბმულით. `[ ]` — არ არის შესრულებული. Evidence-ის გარეშე პუნქტი არ მონიშნდება.

## 0. Change gate (AGENTS.md) — Increment 1

| # | კითხვა | პასუხი |
|---|---|---|
| 1 | პრობლემა, scope | Declarative draft → immutable semantic plan; Access authoring adapter; wide → long ingestion core. DB, API, UI — scope-ის გარეთ |
| 2 | Layer | Control (compiler); Delivery (Access adapter); Ingestion (normalizer) |
| 3 | Authority | გეგმა §6–§9; რეესტრი Q17–Q39, Q45 |
| 4 | Site / provider agnostic | 2 unrelated dataset, 1 engine, 0 branch. Provider — port-ის უკან |
| 5 | Identity / versioning | Exact `Ref`; SemVer; `semanticDigest` + `revisionDigest` |
| 6 | Failure / idempotency | Pure function: state-ის გარეშე, retry უსაფრთხო. Fail closed |
| 7 | Security | Scope-bound registry port; invisible = unresolved, leak-ის გარეშე |
| 8 | Migration | 0 migration ამ increment-ში. Legacy R8 adapter — untouched |
| 9 | Tests | Unit, negative, property, adapter round-trip — 30 tests |
| 10 | Evidence | [test record](evidence/statistical-contract/increment-1-compiler-core-2026-09-19.md) |

## 1. Increment 1 — compiler core

Code: `platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/statistical/`  
Tests: `…/api/src/test/java/org/base/api/service/platform/statistical/`

| ✓ | პუნქტი | Q | Evidence (test) |
|---|---|---|---|
| [x] | Exact reference grammar; `latest` / range / wildcard — rejected | Q30 | `referenceIsExactAndRoundTrips` |
| [x] | Closed grammar parser; unknown field, caller SQL — rejected | Q31 | `closedGrammarRejectsWhatItDoesNotName` |
| [x] | `existingStructureRef` XOR `inline` | Q31 | same |
| [x] | Measure semantics wire-ში არ მეორდება | Q32 | `measureSemanticsCannotBeRestatedOnTheWire` |
| [x] | JSON Schema 2020-12 published (shape authority) | Q31 | `resources/contracts/statistical-contract-draft.schema.json` — schema-vs-parser equivalence test: ღია |
| [x] | Reference resolution + pinned dependency closure | Q30 | `dependencyClosureIsPinnedAndIncludesDerivedReferences` |
| [x] | Proposal: preview OK, approval blocked | Q09 | `proposedEntryPreviewsButCannotBeApproved` |
| [x] | Microdata profile — explicit rejection | Q01 | `microdataProfileIsRefusedExplicitly` |
| [x] | Cross-product ref = unresolved, leak-ის გარეშე | Q08, Q44 | `anotherProductsEntriesAreIndistinguishableFromMissingOnes` |
| [x] | Attachment levels; invalid attachment — rejected | Q21 | `rejectsInvalidAttachments…` |
| [x] | Dataset-level attribute — metadata, არ — column | Q21 | `datasetLevelAttributeIsContractMetadataNotAColumn` |
| [x] | Numeric envelope 28 / 10 — compile-time rejection | Q23, Q33 | `numericEnvelopeBeyondCanonicalStorage…` |
| [x] | Constant binding + declared override | Q38 | `quarantineModeKeepsValidRows…`, `rejectsInvalid…` |
| [x] | Required caption language (`ka`) | Q05 | `rejectsInvalid…` (`MISSING_CAPTION`) |
| [x] | RFC 8785 canonical form; NFC; float-ის გარეშე; domain separation | Q39 | `canonicalJsonFollowsRfc8785…` |
| [x] | Deterministic digest; key order / whitespace independent | Q39 | `digestIsDeterministic…` |
| [x] | Caption change ⇒ only `revisionDigest` | Q39, Q45 | `captionChangeMovesOnlyTheRevisionDigest` |
| [x] | Capability negotiation; unknown provider — rejected | Q35 | `unknownProviderIsRefusedNotDefaulted`, `whatTheProviderCannotHold…` |
| [x] | Key-preserving vertical split; status measure-თან | Q35 | `wideStructureSplits…` |
| [x] | Collision-safe, deterministic physical names | Q35 | `reservedAndOverlongNames…` |
| [x] | `row_ref` — only when key > index limit; never identity | Q37 | `keyWiderThanTheProviderIndex…` |
| [x] | Compatibility classifier → existing `compatibility_mode` | Q45 | `classifiesRevisionSteps` |
| [x] | SDMX time periods → closed interval; never guesses | Q19 | `periodsResolveToClosedIntervals…` |
| [x] | Wide row → 1 observation per measure; own status, unit | Q17, Q33 | `oneWideRowBecomesOneObservationPerMeasure…` |
| [x] | zero ≠ missing ≠ unexplained empty | Q22 | `zeroMissingAndUnexplainedEmpty…` |
| [x] | `ATOMIC_REJECT`: whole load, all findings | Q10 | `atomicModeRejectsTheWholeLoad…` |
| [x] | `QUARANTINE_ROWS`: valid rows kept, bad rows named | Q10 | `quarantineModeKeepsValidRows…` |
| [x] | Exact decimal; no rounding; binary float — rejected | Q34 | `decimalsAreExactAndNeverRounded` |
| [x] | Property: key stable under reorder / replay (25 shuffles) | Q39, plan §12 | `keysAreStableUnderReorderAndReplay` |
| [x] | Access: `NUMERIC(28,10)`, caption, lookup; 28-digit round-trip (Jackcess) | Q34, Q36 | `accessTemplateIsTyped…` |
| [x] | Edited / foreign contract stamp — refused | plan §4 | `fileStampedForAnotherRevision…` |
| [x] | Split parts rejoin; incomplete part blocks load | Q35 | `splitPartsRejoinByKey…` |
| [x] | Non-time dataset, different units, 2nd product — same engine | plan §12 | `compilesTwoUnrelatedDatasets…` |

## 2. ღია — შემდეგ increment-ები

### Increment 2 — Control registry adapter (G1)

Evidence: `JdbcStatisticalRegistryTest` (6 tests, H2 `MODE=MSSQLServer`).

- [x] Versioned identity layer: migration `106_statistical_reference_identity.sql` (namespace, SemVer, lifecycle, owner scope; decided version immutable). New semantic registry — 0 — `migrationIsAdditiveIdempotentAndNeutral`.
- [x] Additive measure / component / unit columns (`unit_id`, envelope, `concept_reference_id`, `representation_type`, `base_unit_id`) — same migration.
- [x] Migration registered in runner after 105 — `migrationIsRegisteredAfterItsPredecessor`.
- [x] `JdbcStatisticalRegistry`: same drafts ⇒ same digests as reference registry — `compilesToTheSameDigestsAsTheReferenceRegistry`.
- [x] Scope, lifecycle, retired codes — `scopeLifecycleAndRetiredCodesBehaveAsThePortRequires`.
- [x] Incomplete / ambiguous entry ⇒ fail closed — `incompleteOrAmbiguousEntriesFailClosed`.
- [x] Stored DSD ≡ inline DSD; reuse pins structure version — `storedStructureResolvesAndCompilesLikeTheInlineOne`.
- [x] Migration 106 on real SQL Server: chain 102 / 102, behaviour 14 cases PASS; applied on dev — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [ ] Provider capability persistence. Finding: `platform.provider_capability` (081) models page-size features, not column / index limits; the adapter declaration stays the authority until that registry is extended.
- [ ] Legacy `metric / carrier` → `measure_ref` machine-readable crosswalk (Q46).
- [x] Registry write side: propose (idempotent, definition digest) / approve in dependency order / supersede; scope rules on write; migration `108`; product and GLOBAL HTTP surfaces — `ReferenceRegistryServiceTest` (5), live on dev incl. 409 / 422 — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [ ] DSD registration through the API (stored structures are readable, not yet writable); policy references.
- [ ] Schema-vs-parser equivalence test (JSON Schema validator dependency — supply-chain review).

### Increment 3 — Workflow (G2)

Evidence: `ContractWorkflowReferenceStoreTest` (10), `ContractWorkflowJdbcStoreTest` (11) — one behavioural suite, two substitutable stores.

- [x] State machine on existing lifecycle values; new enum value — 0 (Q40) — `happyPath…`, `editAfterReview…`.
- [x] Compare-and-set version; stale writer loses; expected version mandatory — `staleWritersLoseAndNothingIsOverwritten`.
- [x] Idempotent create per (product, key); different payload ⇒ conflict (Q42) — `createIsIdempotentPerProductAndKey`.
- [x] Save incomplete; submit requires compilable contract — `incompleteDraftSavesButCannotBeSubmitted`.
- [x] Approval bound to reviewed `revisionDigest`; recompiled at execution; stale review rejected — `editAfterReviewVoidsTheReviewedDigest`.
- [x] Proposed dependency: review OK, approval blocked (Q09) — `approvalRequiresApprovedDependenciesAtExecutionTime`.
- [x] Per-call authorization; other product ⇒ NOT_FOUND (no oracle); revoked permission honoured; four-eyes (Q06, Q44) — `authorizationIsPerCall…`.
- [x] Single-person team — explicit policy only — `singlePersonTeamIsAnExplicitPolicyNotADefault`.
- [x] New approval supersedes previous atomically; compatibility verdict returned (Q45) — `newApprovedRevisionSupersedes…`.
- [x] 8 concurrent approvers ⇒ exactly 1 decision — `concurrentApprovalsProduceExactlyOneDecision`.
- [x] Store migration `107_statistical_contract_draft.sql`: CAS version, one APPROVED per dataset (filtered unique index), append-only history, decided text immutable; registered in runner.
- [x] Migration 107 on real SQL Server: behaviour 11 cases PASS (CAS, one APPROVED per dataset, immutability, append-only); applied on dev — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [x] HTTP API `/platform/products/{productCode}/statistical-contracts`: strong `ETag`, `If-Match` → `428` / `412` (weak validator and `*` refused), `Idempotency-Key`, RFC 9457 problems with compiler findings, `no-store` — `StatisticalContractHttpTest` (5). Bug found by the test and fixed: an approver could not read the contract.
- [x] Tenant boundary: route carries `productCode` ⇒ `@TenantScoped` interceptor; `TenantScopeCoverageTest` PASS with the new controller. Contract of another product through a permitted path ⇒ 404.
- [x] `AccessDecision` adapter: tenant guard AND function authority (`WRITE_RESOURCE` / `PUBLISH_RESOURCE`, property-configurable) — `StatisticalContractAccessDecision`. Unit test of the adapter itself: ღია.
- [x] Spring composition root with safe defaults (`allow-self-approval=false`) — `StatisticalContractConfiguration`. Dev start PASS, 7 routes — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [x] Live API on dev (Keycloak token, SQL Server store): 201 + `ETag`, idempotent replay, 409, 428, 412, 422 with findings, 404 across products, 401 anonymous — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [x] Live end-to-end on dev: references registered → contract compiles from the database registry against a real classification (AGE_GROUP, 12 codes) → submit → four-eyes refusal `403` → text frozen in review `409` — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [x] Live four-eyes approval on dev with a second, additive service identity (`geostat-contract-approver`): wrong digest `409`, approval `200`, replay `200`, author-only identity refused — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [x] Authoring file: generated on demand for APPROVED only, `Digest` header = SHA-256 of the body, `no-store`, BCP 47 language check; read-only validation with byte budget `413`; temp files removed — `StatisticalContractHttpTest` (6) + live: Georgian captions, 12-code lookup, good / bad / non-Access files — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [x] Registry drift fails closed: dependency superseded ⇒ approved contract `422`, never reinterpreted — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [ ] Durable generation job on `api_operation` + outbox (Q41, Q43). Current: synchronous, pure, bounded — sufficient under the Q14 budget; the job variant is needed only for files beyond it.
- [ ] Worker-side authorization re-check with stored initiator (Q44).
- [ ] UI wizard.

### Increment 4 — Data plane writer (G3)

Evidence: `CanonicalObservationWriterTest` (5 tests, H2, column shape of `002_data_plane.sql`).

- [x] Contract-driven writer into existing `statistics.*` tables; new table — 0; site / measure literal — 0 — `CanonicalObservationWriter`.
- [x] Writes observation status, `period_end`, attributes, unit, per-measure series (register §4.3 gaps) — `writesWhatTheLegacyWriterLeftAtDefaults`.
- [x] Missing ≠ zero in storage — `missingValueKeepsItsStatusAndNoNumber`.
- [x] Replay = no-op; changed value = conflict; partial load rolls back — `replayIsANoOp…`.
- [x] Non-time dataset; 28-digit decimal survives storage — `nonTimeDatasetAndExactDecimalSurviveStorage`.
- [x] No lineage or no binding ⇒ refused — `aLoadWithoutLineageOrBindingIsRefused`.
- [x] `StatisticalBindingService`: approved plan → existing `dataset`, `dataset_version` (checksum = revision digest), `ingestion_contract`, `metric`, `dimension`; idempotent — `StatisticalLoadServiceTest` (4) + live — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [x] Governed load `POST …/loads`: validate → retain source bytes content-addressed → one Data Plane transaction (batch, artifact, load, PREPARED snapshot, lineage, observations); replay = same receipt; rejected file writes nothing; separate IMPORT duty — tests + live on dev (snapshot 54, `period_end`, coded dimension, MinIO object; 42 published snapshots untouched) — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [x] Hand-over to the existing release gates: the load conforms to what they measure (candidate state `REVIEW_REQUIRED`, staged rows, source-row count); live snapshot 55 ⇒ `releasable=true`, all applicable gates PASS; a published snapshot is still refused — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [x] Gate made to conform to Q22: an empty value with a declared status is not a defect; only an unexplained empty value fails `STATISTICAL_SEMANTICS_VALID` — `ReleaseGateServiceTest` PASS + live.
- [x] Metric identity = dataset × exact measure version (defect found live, fixed) — `revisedMeasureVersionInANewContractRevisionBindsItsOwnMetric`.
- [x] Batched database write: resolve first, then JDBC batches of 1 000 per table; live 2 712 observations in 9 s on dev, dates from 1800 exact on SQL Server — `loadsAcrossSeveralBatchesAndReplaysWithoutWriting`, [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [x] Defect fixed: periods written as `java.sql.Date` shifted historical dates by a day; now `LocalDate` end to end.
- [x] Q11 refined and enforced: a load is a `FULL_SNAPSHOT` (the native unit of the platform); other modes answer `422` explicitly — test + live.
- [ ] Snapshot-delta design for `UPSERT / DELETE` (only if a real dataset needs it).
- [x] Machine-readable legacy crosswalk (Q46): 86 legacy metrics → 1:1 proposed measure references, no merge by name, 0 collisions; 24 registrable after steward sign-off, 62 blocked with typed reasons — `ops/scripts/python/statistical-legacy-crosswalk.py`, [proposal](evidence/statistical-contract/legacy-crosswalk-proposal-2026-09-19.json).
- [ ] Steward decisions for the 62 blocked metrics; registration; value-level semantic diff of a migrated dataset.
- [x] Writer on real SQL Server (dev): observations, dimensions, lineage written under the existing triggers — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).

### Increment 5 — Access UX (G0 spike → G3)

- [ ] Real Access open / select / save evidence, per supported version (Q03, Q36).
- [ ] Template ACCDB spike: Navigation Pane groups, startup view.
- [ ] Georgian captions and lookups verified in real Access UI.

### Increment 6 — Export and release (G4)

- [x] SDMX-CSV 2.0 codec from the plan: one row per key, each measure and its status in its own column, deterministic order, exact decimals — `SdmxCsvExporterTest` (3).
- [ ] SDMX-JSON 2.0 codec; validation of both with official SDMX tooling; serving endpoint over a published snapshot (Q47).
- [x] Multi-measure → 2.1 is refused, never pivoted — `whatTheTargetFormatCannotHoldIsRefusedNotPivoted`.
- [ ] Build manifest, trace, log scrub (Q48).
- [x] Q14 file-path budgets at the design ceiling, off the shared host, 1 GiB heap: 200 000 rows / 400 000 observations — read 0.6 s, normalise 3.1 s (budget 120 s), file 21 MB (budget 250 MB), heap 515 MB — `StatisticalContractPerformanceTest`, [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [ ] Rollout / rollback rehearsal; consumer sign-off (Q15, Q50).

### Regression

- [x] Full `:api:test` regression: 457 ran, 454 passed, 1 skipped, 2 failed — both failures belong to another session's uncommitted legacy-lockdown work — [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).
- [ ] Test-harness stall: with parallel forks one fork never starts its queue, so the single-command run does not end (pre-existing; AIR-2026-048).

### Owner / steward

- [ ] 9 default-ის დადასტურება (რეესტრი §6).
- [ ] Dataset card × 3 validation dataset (რეესტრი §3.2).

## 3. Plan §12 acceptance — მიმდინარე მდგომარეობა

| Plan §12 პუნქტი | მდგომარეობა |
|---|---|
| DSD / schema validation unit + negative | core: PASS (Increment 1); DB-backed: ღია |
| Multi-measure, different units, measure-level status round-trip | core + Jackcess: PASS; canonical DB: ღია |
| Non-time, grain rejection | non-time: PASS; mixed-grain rule — dataset card-ზე დამოკიდებული: ღია |
| invalid code / version, duplicate tuple, missing / zero / suppressed | PASS (core); suppressed + `CONF_STATUS` policy: ღია |
| Decimal boundary / overflow / lexical preservation | PASS (core + Jackcess) |
| Replay / reorder | reorder: PASS; crash / concurrent ingest: ღია |
| Property tests, key stability | PASS |
| Legacy ↔ new reconciliation | ღია |
| Auth / tenant / confidentiality negative | resolution-level: PASS; API / worker: ღია |
| Real Access open / fill / save | ღია — Jackcess round-trip real Access-ს არ ცვლის |
| Performance budgets | ღია |
| Aggregation / derivation rules | ღია |
| SDMX conformance | ღია |
| Release invariant | ღია |
