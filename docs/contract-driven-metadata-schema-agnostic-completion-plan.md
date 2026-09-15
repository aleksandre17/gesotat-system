# Contract-Driven, Metadata-Driven, Schema-Agnostic Platform
## სრული completion plan და acceptance checklist

**სტატუსი:** CANONICAL EXECUTION PLAN  
**თარიღი:** 2026-09-14  
**მიზანი:** GEOSOTAT-ის იმ არქიტექტურულ მდგომარეობამდე მიყვანა, რომელსაც სრულად ამართლებს მისი contract-driven, metadata-driven, schema-agnostic, multi-site data-product პლატფორმის იდენტობა.

allowedIncludes` now reflects only revision-scoped declared relations, preserving contract-first/schema-agnostic behavior; API regression passed

## 1. საბოლოო განსაზღვრება

პლატფორმა ჩაითვლება დასრულებულად მხოლოდ მაშინ, როცა ახალი site/provider/data family ერთვება versioned contract, mappings, keys, relations, semantics, policies და approved snapshot-ით — ახალი business table-ის, KIDS-specific branch-ის ან core-code ცვლილების გარეშე.

კანონიკური ხაზი:

```text
contract authoring/approval
  → source receipt and fingerprint
  → staging/validation/quarantine
  → canonical materialization
  → reconciliation/quality/privacy evidence
  → immutable snapshot/publication
  → policy-aware relation-graph query
  → cache/export/API/event response
```

## 2. არსებული baseline

- Control/Data/Archive/Serving Plane reference architecture;
- contract registry, introspection, metadata-driven query და response generation;
- ENTITY/STATISTICAL/REFERENCE/RAW/GEO/RELATION family adapters;
- relation graph, declarative filters, aggregation, projection და include limits;
- KIDS R8 reference contract და canonical Access artifact;
- local second-provider contract-only proof და DB replay tests;
- HMAC cursor, tuple key, query fingerprint და snapshot scope;
- OpenAPI 3.1, JSON Schema, TypeScript baseline, SDMX facade, JSON/CSV/NDJSON;
- Redis query-cost/rate-limit adapter და OTel Collector/OTLP wiring;
- evidence bundle generator/verifier, technical acceptance runner და documentation checker.

ეს baseline არ ნიშნავს ყველა production acceptance-ის დასრულებას.

## 3. Canonical completion checklist

თითოეული პუნქტი იხურება მხოლოდ ოთხი პირობით: implementation, automated tests, independent evidence და versioned documentation. `[x]` გამოიყენება მხოლოდ ყველა პირობის შესრულებისას; `[-]` ნიშნავს implementation ნაწილობრივ დასრულებულს; `[ ]` ნიშნავს დარჩენილს.

### C-01 Contract authority და governance

- [-] contract/page/dataset/field/relation/projection/policy-ის immutable revision model;
- [-] metric/unit/aggregation/dimension/code-list/policy/response breaking-change analyzer (provider-neutral semantic core is implemented; contract/policy integration remains);
- [-] approval, supersede, rollback და compatibility workflow-ის lifecycle core (`ContractLifecycle`) და idempotent/fail-closed tests დაემატა; persistence/API orchestration remains;
- [-] contract checksum-ის deterministic SHA-256 binding core (`ContractChecksumBinding`) და constant-time verification tests დაემატა; publication/deploy ledger binding remains;
- [x] documentation-as-code drift gate (`documentation-zero-drift.ps1`) — required artifacts, audit counts, scope markers and implementation/test presence are checked fail-closed; checker PASS evidence recorded.

**Acceptance:** breaking change იბლოკება; approved revision-ის გარეშე materialization/publication ვერ სრულდება.

### C-02 Provider capability registry და discovery

- [x] provider capability registry-ის provider-neutral ბირთვი — family, operation, feature, transaction და page-limit metadata; duplicate/retire/unsafe-metadata tests PASS;
- [ ] runtime provider discovery მხოლოდ approved metadata-ით;
- [-] provider lifecycle core დაემატა `ProviderLifecycle`-ით და provider-neutral `ProviderHealthSupervisor`-ით (priority selection, deterministic failover, no-candidate fail-closed; idempotent/fail-closed tests); production runtime SPI binding და external health evidence remains;
- [-] capability negotiation core დაემატა `ProviderCapabilityNegotiator`-ით: family/operation/features/page-limit fail-closed validation; runtime wiring into every path remains;
- [x] provider failure/failover და restart tests (`ProviderHealthSupervisorTest`);
  provider-neutral selector is deterministic, stateless across restart and
  fail-closed when no approved candidate is healthy. Production probe wiring
  and measured external failover evidence remain under the production gate.

**Acceptance:** ახალი provider core-code branching-ის გარეშე რეგისტრირდება, capability mismatch კი fail-closed ხდება.

### C-03 მეორე დამოუკიდებელი site/provider

- [x] local contract-only onboarding proof;
- [x] local DB replay proof;
- [ ] რეალური განსხვავებული schema/table names;
- [ ] composite key, relation და classifier data;
- [ ] ingest → materialization → publication → API → export → rollback სრული replay;
- [ ] signed independent acceptance report.

**Acceptance:** KIDS-ის სახელების, controller branch-ისა და ახალი business table-ის გარეშე მეორე site სრულად მუშაობს.

### C-04 Data-family interchangeable lifecycle

- [-] ENTITY/STATISTICAL/REFERENCE/RAW/GEO/RELATION adapters;
- [ ] ყველა family-ის ერთიანი onboarding/validation/materialization/publication/rollback lifecycle;
- [x] family capability profile და conformance suite-ის lifecycle ნაწილი (`DataFamilyLifecycleConformanceTest`) — ENTITY/STATISTICAL/REFERENCE/RAW/GEO/RELATION ერთიან transition/quarantine contract-ზე გადის; provider-specific execution conformance ცალკე ღიაა;
- [x] family-specific semantics generic engine-ში აღარ არის hardcoded: `CanonicalPageDataService` მხოლოდ contract metadata-ს orchestrate-ს, ხოლო family execution, aggregation და physical access delegated არის `ContractPageExecutionRegistry`/adapter-ებში; targeted და full API regression PASS.
- [-] contract-only cross-family relation/projection acceptance now covers persisted-contract-shaped metadata, ENTITY→REFERENCE and RAW→STATISTICAL graph execution, nested include/projection and response redaction (`CrossFamilyRelationProjectionAcceptanceTest`, technical-acceptance gate); real provider/database replay for every family remains an external acceptance gate.

**Acceptance:** ერთი lifecycle orchestration ყველა family-ზე ერთნაირი კონტრაქტის წესით მუშაობს.

### C-05 Statistical adapter abstraction

- [-] registry-based dimension/alias resolution და canonical statistical adapter;
- [-] `statistics.*` storage implementation encapsulated იქნა `StatisticalQueryAdapter`/`CanonicalStatisticalQueryAdapter` boundary-ში; alternate provider და capability negotiation remains;
- [ ] alternate statistical storage provider;
- [ ] SDMX measure/unit/aggregation/dimension semantics negotiation;
- [ ] provider-independent regression/performance suite.

**Acceptance:** statistical query contract-ს ასრულებს storage table names-ის ცოდნის გარეშე.

### C-06 Portability matrix

- [-] provider SPI და identifier/connection validation;
- [ ] SQL Server execution matrix;
- [ ] MySQL execution matrix;
- [ ] non-default schema;
- [ ] composite keys და large tables;
- [ ] forward/backward schema evolution;
- [ ] reproducible matrix report.

**Acceptance:** ერთი contract და ერთი query semantics ყველა დამტკიცებულ provider profile-ზე ერთნაირ შედეგს იძლევა.

### C-07 Query/keyset hardening

- [-] HMAC cursor scope, tuple last-seen values, query fingerprint და lexicographic predicate; **snapshot binding implementation closed**: contract revision → `site_contract_dataset.dataset_version_id` → immutable `publication.dataset_snapshot` (approved lifecycle states) → canonical `entity.entity_record`/`statistics.series` rows. Production traversal evidence remains under the release/evidence gate.
- [x] ascending/descending tuple და identifier-safety regression tests;
- [-] null/order direction-ის ერთიანი კონტრაქტული metadata (`StableSortSpec`) და provider-neutral forward/backward tuple comparator დაემატა (`KeysetTupleComparator`); SQL predicate integration remains;
- [-] stable sort/index requirement contract-ში გამოხატულია (`requiredIndexCode`); runtime index introspection/enforcement remains;
- [-] forward და backward tuple predicate generation is implemented/tested; end-to-end cursor traversal remains;
- [ ] concurrent insert/update integration tests;
- [ ] provider-independent high-volume benchmark.

**Acceptance:** cursor არ მეორდება/იკარგება concurrent write-ზე და throughput/capacity envelope დადასტურებულია.

### C-08 Semantic compatibility

- [-] field/relation compatibility baseline;
- [-] metric/unit/aggregation compatibility — provider-neutral scalar comparison and regression tests are present; contract registry/policy integration remains;
- [-] dimension/code-list compatibility (provider-neutral analyzer now evaluates declared `codeLists`; contract registry integration remains);
- [-] projection/quality/privacy/response-schema compatibility — provider-neutral nested-surface comparison is implemented and tested; contract-registry integration remains;
- [-] breaking/non-breaking golden suite — scalar, collection and nested projection/response fixtures PASS; full persisted-contract fixture corpus remains;
- [-] `approvalRequired` და machine-readable migration guidance დაემატა analyzer result-ში; approval workflow binding remains.

**Acceptance:** შეუთავსებელი ცვლილება release-ს ბლოკავს და თავსებად ცვლილებას აქვს machine-readable explanation.

### C-09 Legacy boundary

- [-] governed `dynamic/pages` contract-engine-first path;
- [-] pre-contract profile/table fallback-ის deprecation policy (governance preflight and window defined);
- [x] migration notice და telemetry;
- [-] no-fallback regression test დაემატა (`DynamicDataControllerNoFallbackTest`): approved contract-ის არქონისა და disabled flag-ის დროს legacy reader არ გამოიძახება; full retirement remains;
- [-] controlled retirement მხოლოდ compatibility impact review-ის შემდეგ (preflight მზადაა; approval საჭიროა).

**Acceptance:** legacy path ვერ გახდება ახალი contract query-ის ჩუმი ალტერნატივა.

### C-10 Interoperability

- [-] OpenAPI 3.1, JSON Schema, TypeScript, SDMX facade, JSON/CSV/NDJSON;
- [x] SDMX-JSON provider;
- [x] SDMX-XML provider;
- [x] Parquet provider (Linux/Docker round-trip conformance);
- [x] ZIP package provider;
- [ ] Java/Kotlin/Dart SDK generation;
- [ ] streaming export და progress events;
- [-] round-trip/conformance fixtures — SDMX and Parquet fixtures PASS; SDK
  and streaming fixtures remain.

**Acceptance:** თითოეული format-ის export→import/parse round-trip და schema conformance PASS-ია.

### C-11 Security, tenancy და cost governance

- [-] JWT fail-closed guards, Redis adapter, query-cost admission, bounded limits და telemetry;
- [ ] რეალური OIDC issuer/JWKS და asymmetric validation;
- [ ] RBAC + ABAC + tenant/site/dataset/field isolation;
- [ ] production Redis binding, per-tenant budgets, HA/failover და fairness;
- [ ] expired/wrong-audience/revoked/cross-tenant negative suite;
- [ ] overload/soak evidence და usage telemetry.

**Acceptance:** deny-by-default და shared-state quota ყველა replica-ზე ერთნაირად მოქმედებს.

### C-12 Observability, reconciliation და DR

- [-] OTel Collector, OTLP wiring, redaction hooks, evidence bundle/verifier;
- [ ] durable telemetry backend, traces/log correlation, dashboards და SLO/error budget;
- [ ] alert routes და firing test;
- [ ] source/artifact/contract/snapshot checksum reconciliation;
- [ ] real backup target, timed restore, replay და rollback;
- [ ] measured RPO/RTO.

**Acceptance:** ყველა გამოქვეყნება traceable evidence bundle-ითაა მიბმული და restore/replay გაზომილია.

### C-13 Release და resilience assurance

- [-] release-gate, reproducible build tooling, image revision და technical acceptance runner;
- [ ] approved signed tag/commit და clean checkout deploy;
- [ ] SBOM, dependency/container/IaC/secret scan და provenance attestation;
- [ ] approved load/chaos/DR test window;
- [ ] abort/rollback conditions;
- [ ] signed security/load/chaos/DR reports.

**Acceptance:** critical finding, provenance mismatch ან დაუმტკიცებელი window release-ს fail-closed აჩერებს.

### C-14 Documentation და runtime reconciliation

- [-] audit, capability-gap, authority register და learning guide;
- [x] documentation zero-drift checker;
- [-] migration/runtime inventory, contract markers და audit reconciliation-ის generated ledger დაემატა (`ops/cli/data/generate-runtime-ledger.ps1`); full contract-row extraction და deployment binding remains;
- [ ] R6/R7/legacy claims-ის საბოლოო reconciliation;
- [x] generated API/schema/docs consistency report.

**Acceptance:** დოკუმენტაცია და runtime/migration state ერთმანეთს ავტომატურად ემთხვევა.

## 4. დახურვის რიგი

`C-01 → C-02 → C-03/C-04/C-05 → C-06 → C-07/C-08/C-09 → C-10 → C-11/C-12 → C-13 → C-14`.

## 5. საბოლოო production verdict

პლატფორმა production-complete იქნება მხოლოდ მაშინ, როცა ყველა C-01–C-14 პუნქტი `[x]` გახდება და ექნება versioned, immutable, განმეორებადი evidence. UI ამ plan-ის scope-ში არ შედის. Production authority-ის არქონა ტექნიკური implementation-ის გაკეთებას არ კრძალავს, მაგრამ approval/evidence-ის გამოგონება აკრძალულია.

## 6. ქვე-checklist-ები — თითოეული C-ბლოკის ზუსტი საზღვარი

ეს ქვე-checklist-ები განსაზღვრავს, რა ეკუთვნის ამ completion plan-ს და როგორ უნდა დაიხუროს თითოეული ბლოკი. `[x]` ნიშნავს დადასტურებულ implementation/test-ს, `[-]` — partial implementation-ს, `[ ]` — დარჩენილს.

### C-01 Contract authority

- [x] versioned contract/page/dataset/field/relation/projection baseline;
- [-] immutable checksum/revision model-ის runtime checksum core (`ContractChecksumBinding`) და approval-evidence persistence (`PlatformContractGovernanceService`, migration 082) დადასტურებულია; publication/supersede/rollback binding remains;
- [-] full semantic breaking-change analyzer — provider-neutral scalar, set, nested projection and response-schema comparison is implemented and tested; durable contract-registry/policy workflow binding remains;
- [-] approval state API და durable approval-evidence write path დაემატა (`ContractLifecycle`, `PlatformContractGovernanceController`, migration 082); restart-safe `ContractLifecycleOrchestrator`/`JdbcContractLifecycleStateStore` და migration 084 ახლა approve→supersede/rollback state persistence-სა და idempotent recovery-ს ამატებს; production approval/authority remains;
- [x] generated documentation drift gate binding (`documentation-zero-drift.ps1` is enforced fail-closed and PASS evidence is recorded).

### C-02 Provider capability/discovery

- [x] provider-neutral capability registry core;
- [x] family/operation/feature validation და duplicate/retire fail-closed tests;
- [-] Control Plane persistence schema და ACTIVE-row runtime discovery service დაემატა (`081_provider_capability_registry.sql`, `ProviderCapabilityDiscoveryService`); reload now atomically replaces the ACTIVE set so stale providers cannot survive, startup discovery is guarded and tested, production failover binding remains;
- [-] provider lifecycle state-machine core (`ProviderLifecycle`) და provider-neutral health/failover selector (`ProviderHealthSupervisor`) tests-ით მზადაა; production health probe, runtime binding და retirement orchestration remains.
- [-] capability negotiation core (`ProviderCapabilityNegotiator`) tested and required by technical acceptance; complete runtime binding across external provider paths remains.

### C-03 Independent provider/site

- [x] local second-provider contract-only onboarding proof;
- [x] local database replay proof, including non-default schema, composite identity and cross-family relation/projection (`IndependentProviderCompositeAcceptanceTest`);
- [ ] real independent DB/provider binding;
- [ ] full ingest/materialization/publication/API/export/rollback replay;
- [ ] signed independent acceptance report.

### C-04 Family lifecycle

- [x] ENTITY/STATISTICAL/REFERENCE/RAW/GEO/RELATION adapter baseline;
- [-] provider/family-neutral lifecycle state machine and coordinator (`DataFamilyLifecycle`, `DataFamilyLifecycleOrchestrator`) დაემატა; `LifecycleStateStore`, SQL-backed `JdbcLifecycleStateStore`, migration 083, restart restoration, idempotency, invalid transition and surface-acceptance tests PASS; production deployment binding remains;
- [x] cross-family lifecycle conformance suite (`DataFamilyLifecycleConformanceTest`) — all canonical families share publish/quarantine governance; adapter-specific provider execution conformance remains separate;
- [-] contract-only relation/projection acceptance is covered for cross-family graph and response shaping; provider-specific physical execution acceptance for every family remains open.
- [x] no family-specific generic-engine branch proof: serving and aggregation
  orchestration delegates to `ContractPageExecutionRegistry`; full API and
  synthetic provider acceptance pass.

### C-05 Statistical abstraction

- [x] registry-based dimension and alias resolution;
- [x] canonical statistical adapter boundary;
- [x] alternate storage adapter (`RelationalStatisticalQueryAdapter`) with registry/identifier tests;
- [-] query service აღარ იცნობს `statistics.*` table names-ს და მხოლოდ adapter port-ს იყენებს; capability negotiation independent of storage names remains;
- [ ] provider-independent performance/regression evidence.

### C-06 Portability matrix

- [x] provider SPI and identifier/connection validation;
- [x] parameterized provider execution boundary;
- [ ] SQL Server matrix run;
- [ ] MySQL matrix run;
- [ ] non-default schema/composite key/large-table/schema-evolution runs;
- [ ] reproducible matrix report.

### C-07 Keyset/query hardening

- [x] HMAC scope, tuple values, fingerprint and snapshot binding;
- [x] ascending/descending tuple and unsafe identifier tests;
- [-] null ordering metadata/validation and explicit SQL predicate branches (`StableSortSpec`/`KeysetPredicateBuilder`) are implemented and tested; provider catalog integration remains.
- [-] contract-declared stable index requirement (`requiredIndexCode`); runtime catalog enforcement remains;
- [-] backward cursor and concurrent-write simulation/regression are implemented;
  real DB concurrent-write integration remains;
- [-] provider-independent benchmark harness is implemented; real DB workload
  benchmark remains.

### C-08 Semantic compatibility

- [x] field and relation compatibility baseline;
- [-] provider-neutral metric/unit/aggregation analyzer core, persisted-contract integration and golden corpus are implemented/tested; steward policy approval remains;
- [-] dimension/code-list analyzer (`codeLists` comparison) დაემატა provider-neutral core-ში; complete governed corpus approval remains;
- [-] projection/quality/privacy/response-schema analyzer — nested `projection` and `responseSchema` comparison plus policy scalars are tested; durable contract/policy binding remains;
- [-] breaking/non-breaking baseline და migration guidance დაემატა; სრული golden fixture suite remains.

### C-09 Legacy boundary

- [x] governed dynamic-page contract-engine-first path;
- [-] explicit `platform.legacy.dynamic-pages.enabled` flag, provider-neutral `LegacyFallbackPolicy` and migration/retirement notice tooling are present; steward-approved deprecation window remains;
- [-] legacy fallback usage counter დაემატა `geostat.legacy.dynamic_pages.fallback`-ად; durable dashboard/retirement threshold remains;
- [x] no-fallback regression suite (`DynamicDataControllerNoFallbackTest`) — disabled legacy reader is not invoked when an approved contract path is unavailable; targeted and full API test suites PASS.
- [-] controlled retirement decision (preflight READY_FOR_IMPACT_REVIEW; steward approval and rollback window required).

### C-10 Interoperability

- [x] OpenAPI 3.1, JSON Schema, TypeScript, SDMX facade and JSON/CSV/NDJSON;
- [x] SDMX-JSON/XML providers;
- [x] Parquet/ZIP providers (Parquet Linux/Docker evidence recorded);
- [x] Java/Kotlin/Dart SDK generation (`ContractClientGeneratorService`) and streaming/progress/cancellation primitives;
- [-] round-trip/conformance fixtures exist for SDK/export codecs; external consumer conformance remains.

### C-11 Security/tenancy/cost

- [x] fail-closed JWT guard and bounded query-cost/rate-limit implementation;
- [x] Redis adapter and decision telemetry;
- [-] authorization surface preflight enforces global fail-closed API policy and explicit controller method guards; real OIDC/JWKS token-negative replay remains;
- [ ] real OIDC/JWKS and asymmetric validation;
- [ ] RBAC/ABAC and tenant/site/dataset/field isolation;
- [ ] production Redis HA/fairness/overload evidence;
- [ ] negative authorization suite.

### C-12 Observability/reconciliation/DR

- [x] OTel Collector and OTLP metrics wiring;
- [x] redaction hooks and evidence bundle verifier;
- [ ] durable telemetry backend, traces/log correlation and dashboards;
- [ ] SLO/error budget and alert firing;
- [ ] real backup/restore/replay/rollback;
- [ ] measured RPO/RTO.

### C-13 Release/resilience assurance

- [x] release-gate, image revision, technical acceptance runner;
- [x] reproducible local API regression;
- [ ] approved signed tag/commit and clean production deploy;
- [ ] SBOM/provenance/security scan;
- [ ] approved load/chaos/DR window and abort rules;
- [ ] signed final reports.

### C-14 Documentation/runtime reconciliation

- [x] audit, capability-gap, authority register and learning guide;
- [x] documentation zero-drift checker;
- [-] generated migration/runtime ledger (`ops/cli/data/generate-runtime-ledger.ps1`) now includes migration hashes, contract markers, implementation markers and GEOSOTAT Compose deployment bindings; external runtime deployment attestation remains;
- [ ] R6/R7/legacy claim reconciliation;
- [x] generated API/schema/docs consistency report (`documentation-consistency.ps1`, PASS).

## 7. Execution ledger — 2026-09-14

სრული იზოლირებული checklist-status, თითოეული C-ბლოკის დახურული/partial/open
დაყოფით, ინახება [`contract-plan-isolated-final-status.md`](contract-plan-isolated-final-status.md).

### Checklist/runtime-ledger reconciliation — 2026-09-14T15:53Z

- Audit checklist declaration was reconciled with the machine-counted checklist: **188 verified / 11 open**.
- `audit-checklist-count.ps1`, runtime-ledger generation, runtime-ledger verification and `release-gate.ps1 -AllowDirty` now complete with PASS.
- Technical acceptance replay completed with **20/20 PASS** and API Gradle regression completed with `BUILD SUCCESSFUL`.
- Staging evidence confirms the isolated GeoStat MinIO, OTel Collector and Redis containers are running; no co-hosted project was mutated.
- Remaining `NOT_RUN`/`NOT_ASSERTED` items are intentionally external: protected JWT replay, SQL Server/MySQL workload endpoints, durable telemetry/alert firing, signed release/approval authority and approved production test evidence.

### Authorization surface hardening — 2026-09-14T15:57Z

- Added `authorization-surface-preflight.ps1` as a fail-closed, machine-readable inventory of API controller protection and intentional public liveness/auth surfaces.
- Wired the preflight into both technical acceptance and the release gate; full replay completed **21/21 PASS**.
- This closes static authorization-surface coverage only; real OIDC/JWKS token validation, ABAC/tenant claims and negative token replay remain external acceptance gates.

### OIDC policy contract hardening — 2026-09-14T15:58Z

- Added `oidc-policy-preflight.ps1` and wired it into technical acceptance and release-gate presence checks.
- The preflight validates the canonical realm/client, bearer-only resource-server posture, disabled direct grants/service accounts, required role vocabulary and `tenant_id` access-token mapping.
- Output is explicit: static policy `PASS`, runtime issuer/JWKS and tenant-enforcement evidence `NOT_ASSERTED` until protected-token replay is authorized.

### Legacy retirement governance hardening — 2026-09-14T16:01Z

- `legacy-retirement-preflight.ps1` now validates the consumer-impact section, T+60 deprecation window, idempotent rollback procedure and the pending steward approval record schema.
- Preflight returns `READY_FOR_IMPACT_REVIEW` with `NO_AUTOMATIC_REMOVAL`; no legacy consumer is disabled without an authorized decision.

### Release security-preflight execution — 2026-09-14T16:02Z

- Release gate now executes authorization-surface and OIDC policy preflights (not just presence-checks) and persists their diagnostic artifacts.
- Diagnostic release gate returned PASS; clean checkout and production authority remain fail-closed requirements.

### Release evidence content validation — 2026-09-14T16:03Z

- Release gate now parses the authorization/OIDC preflight artifacts and verifies schema, PASS status and explicit `runtimeBinding=NOT_ASSERTED`; malformed or overclaiming evidence fails closed.

### OIDC runtime reachability check — 2026-09-14T16:05Z

- Read-only remote inspection confirmed `geostat-keycloak` is running on the isolated `geostat-net` network.
- No host port is published, so discovery/JWKS and protected-token replay remain `NOT_ASSERTED` until an approved in-network probe or API runtime binding is available; no network exposure was added automatically.

### Remote anonymous authorization smoke — 2026-09-14T16:07Z

- Read-only staging check confirmed `/health` is public while the protected contract capability endpoint returns HTTP 401 without credentials.
- This is runtime evidence for anonymous fail-closed behavior only; wrong-scope, expired/revoked and cross-tenant token cases remain unasserted.

### Production evidence readiness ledger — 2026-09-14T16:09Z

- Added `production-evidence-readiness.ps1`, executed by technical acceptance, producing one canonical report for release authority, OIDC/tenant, provider, Redis HA, telemetry, DR and resilience gates.
- The report is intentionally fail-closed: `releaseDecision=NOT_READY_FOR_PRODUCTION`, with 1 `NOT_RUN` provider gate and 7 `NOT_ASSERTED` authority/runtime gates.

### Release readiness validation — 2026-09-14T16:10Z

- Release gate now executes and parses the consolidated readiness report, rejecting malformed or overclaiming production status while accepting the explicit `NOT_READY_FOR_PRODUCTION` decision.

### Evidence bundle root fix — 2026-09-14T16:13Z

- Corrected evidence-bundle generator repository-root resolution so referenced paths are canonical repository-relative paths.
- Generation and verification now pass for a five-file immutable evidence bundle with SHA-256 binding; no secrets are serialized.

### Protected denial response hardening — 2026-09-14T16:15Z

- Remote read-only smoke verified the protected contract capability endpoint returns HTTP 401 without credentials and includes `nosniff`, `no-store/no-cache` and explicit `Vary` headers.

### Repository hygiene correction — 2026-09-14T16:17Z

- Removed the final tracked trailing-whitespace defect from the deployment documentation line; `git diff --check` now passes.

### Supply-chain scanner availability audit — 2026-09-14T16:20Z

- Local and isolated-host capability checks found no `syft`, `trivy` or Docker Scout executable.
- Static image pinning remains enforced; SBOM/vulnerability execution is explicitly `NOT_RUN` rather than inferred.

### Evidence bundle path regression hardening — 2026-09-14T16:25Z

- Generator now accepts absolute output locations safely; verifier anchors referenced files to the bundle's declared repository root and rejects traversal.
- Technical acceptance includes an isolated temporary-directory generate/verify regression and returned PASS.

### Physical query revision binding — 2026-09-14T16:29Z

- Contract physical table resolution now requires the requested `site_contract_revision_id` through an `EXISTS` binding against `platform.site_contract_dataset`.
- This prevents a latest-ACTIVE table from being served for a different contract revision; full API regression completed successfully.

### Metadata introspection revision binding — 2026-09-14T16:34Z

- `ContractMetadataService` now applies the same site-contract revision binding to physical table and index introspection.
- Query execution and metadata discovery therefore resolve the same revision-scoped physical contract; full API regression passed.

### Runtime validator revision binding — 2026-09-14T16:39Z

- `ContractRuntimeValidator` now requires a revision-scoped physical mapping before query planning, aligning validation with metadata and execution lookup semantics.
- Full API regression completed successfully.

### Introspection include contract tightening — 2026-09-14T16:44Z

- Removed undeclared hardcoded `classification`, `metadata` and `lineage` include values from introspection.
- `allowedIncludes` now reflects only revision-scoped declared relations, preserving contract-first/schema-agnostic behavior; API regression passed.

### Release hygiene enforcement — 2026-09-14T16:22Z

- Release gate now runs `git diff --check` and fails closed on whitespace/patch hygiene defects; diagnostic execution returned PASS.

### Provenance path reconciliation — 2026-09-14T16:24Z

- Historical release-provenance inventory references were corrected to the canonical `ops/cli/validation/release-gate.ps1` location; no historical counts or authority claims were rewritten.

The following implementation increment is complete and verified locally:

- `ContractLifecycle` is a provider/product-neutral contract-revision state machine (`DRAFT → REVIEW_REQUIRED → APPROVED → SUPERSEDED/ROLLED_BACK`), with idempotent repeats and fail-closed invalid transitions.
- `ContractLifecycleTest` covers the approved/supersede path, review-gate enforcement and terminal-state immutability; `:api:test --tests '*ContractLifecycleTest'` passed.
- `documentation-zero-drift.ps1` now requires both the lifecycle implementation and its test, so documentation/runtime reconciliation cannot pass if this governance kernel disappears.
- `StableSortSpec` centralizes tuple ordering, direction, NULL policy and required-index metadata; unsafe/duplicate declarations are rejected and tested.
- `ContractChecksumBinding` provides deterministic SHA-256 payload binding with constant-time verification; malformed expectations fail closed and are tested.
- `LegacyFallbackPolicy` centralizes contract-first precedence and explicit compatibility-flag enforcement; policy tests pass. Legacy retirement still requires consumer-impact evidence.
- `technical-acceptance.ps1` now executes runtime-ledger generation as a mandatory gate, preventing acceptance from passing when migration/contract/audit inventory drifts.
- `technical-acceptance.ps1` now executes the portability-matrix planner as a mandatory safe gate; unavailable provider endpoints produce explicit `NOT_RUN`, never a false PASS.
- `technical-acceptance.ps1` now requires the provider capability-negotiation guard and its tests before acceptance can pass.
- `technical-acceptance.ps1` now requires provider lifecycle governance implementation and tests before acceptance can pass.
- `ContractOpenApiValidator` validates generated OpenAPI 3.1 info/paths/unique operation IDs without product-specific branching; malformed surfaces fail closed.
- `ContractOpenApiService` now invokes the validator before returning any generated document, making schema validation part of the runtime path rather than a standalone check.
- `technical-acceptance.ps1` now requires the OpenAPI surface validator and its regression tests before acceptance can pass.
- `documentation-zero-drift.ps1` now requires the OpenAPI validator test as well as the implementation, preventing silent loss of schema-surface regression coverage.
- `ContractCompatibilityService` now exposes both revision checksums and `checksumChanged`; any structural breaking diff forces `approvalRequired=true`.
- `verify-runtime-ledger.ps1` now re-hashes every migration, rejects duplicate/missing IDs, verifies audit counts and required markers; generated ledger verification is a separate acceptance gate.
- `release-gate.ps1` now requires and executes runtime-ledger generation plus verification, making provenance checks fail-closed on migration/contract/audit inventory drift.
- `runtime-ledger-integrity-smoke.ps1` proves tampered migration hashes are rejected; this tamper-resistance check is now a technical-acceptance gate.
- `supply-chain-preflight.ps1` inventories and rejects floating Compose image tags; SBOM/vulnerability execution is explicitly `NOT_RUN` until approved registry access and `syft`/`trivy` are available.
- `release-gate.ps1` now requires and executes supply-chain preflight, so unpinned images fail the provenance gate before any deployment attempt.

### Verified closure increment — 2026-09-14T08:43Z

- C-01 documentation-as-code drift gate is closed at the implementation/evidence level: `documentation-zero-drift.ps1` PASS confirms the required contract-governance artifacts and tests are present and the audit ledger is consistent. Production approval and signed release authority remain governed by C-13.
- C-09 no-fallback regression suite is closed at the implementation/test level: `DynamicDataControllerNoFallbackTest` and the full `:api:test` suite PASS; legacy retirement and consumer-impact review remain separate governance decisions.
- Full technical acceptance replay PASS: **20/20** checks, including host layout boundary, provider capability negotiation/lifecycle governance, data-family lifecycle conformance, semantic compatibility surface analysis, provider-neutral keyset tuple semantics, OpenAPI runtime validation, runtime-ledger generation/verification/tamper resistance, contract-only synthetic acceptance, full API regression and documentation consistency.
- Family lifecycle conformance evidence added: every canonical family is tested against the same governed publish and quarantine transitions; technical acceptance now enforces this test artefact.
- Semantic compatibility surface expanded: quality/privacy/response semantics and nested projection/response-schema changes are classified deterministically; malformed nested metadata fails closed and targeted tests PASS. Contract-registry persistence/approval orchestration remains open.
- `ContractCompatibilityService` now reads each revision's declared `contract_document_json`, runs the provider-neutral semantic analyzer, merges semantic breaking changes into the compatibility result, and returns machine-readable semantic guidance/approval flags; persisted-document regression test PASS.
- `KeysetTupleComparator` adds explicit NULL ordering, mixed ASC/DESC semantics and exact reverse traversal at the provider-neutral tuple layer; arity/type/null regression tests PASS and the acceptance runner now requires this artefact.
- Host boundary scaffold and relocation safety are now machine-checked by `scripts/host-layout-check.ps1`: empty upstream `agent-framework/kit`, `.agents` attachment metadata, `platform/apps/geostat/backend`/`platform/apps/geostat/frontend` targets and ops/knowledge boundaries are verified without moving or deleting active source paths.
- Current evidence is intentionally split into `PASS`, `NOT_RUN` and `NOT_ASSERTED`; no production authority, external approval, real provider endpoint, durable telemetry target or measured DR/load result is inferred from local PASS results.

This closes the lifecycle-core implementation slice only. Durable approval evidence, persistence/API orchestration and production authority remain explicitly open under C-01/C-13.

### Local closure replay — 2026-09-14T13:37Z

- Full technical acceptance replay completed: **20/20 PASS**, including host
  layout, provider capability/lifecycle guards, family conformance, semantic
  compatibility, keyset semantics, OpenAPI validation, runtime-ledger
  generation/verification/tamper resistance, synthetic contract-only
  acceptance, full API regression and documentation consistency.
- Backend Gradle build and API test suite completed successfully (`BUILD
  SUCCESSFUL`).
- Portability planner correctly reported four provider profiles as `NOT_RUN`
  because no approved SQL Server/MySQL endpoints are configured; this is not a
  false PASS and remains open under C-03/C-06.
- Production approval remains `NOT_ASSERTED`; no external authority or measured
  production evidence was inferred from this replay.

### Implementation increments — 2026-09-14T14:00Z

- `PageFamily` centralizes contract-declared family identity and replaces direct
  family-string dispatch in `CanonicalPageDataService`; `PageFamilyTest` PASS.
- `ProviderCapabilityRegistry` is Spring-managed and
  `ProviderCapabilityDiscoveryRunner` loads only ACTIVE capabilities at startup
  when `PLATFORM_PROVIDER_DISCOVERY_ENABLED=true`; zero discovered capabilities
  fail closed. Provider capability tests PASS.
- `DataFamilyLifecycleOrchestrator` now applies one idempotent, fail-closed
  lifecycle pipeline to ENTITY, STATISTICAL, REFERENCE, RAW, GEO and RELATION;
  `DataFamilyLifecycleOrchestratorTest` PASS.
- `ContractPhysicalQueryService` now accepts `StableSortSpec` keyset metadata
  and rejects execution unless the required contract index is declared;
  keyset test suite PASS.
- `StatisticalAdapterRegistry` provides explicit provider-to-adapter binding
  with duplicate/unknown-provider rejection; `StatisticalAdapterRegistryTest`
  PASS.

These increments are implementation/test closures only. Real provider,
production endpoint, approval and measured resilience evidence remain governed
by the production-authority-required scope above.

- `ExportCodec` and immutable `ExportCodecRegistry` now provide a single
  provider-neutral encoder boundary for JSON, CSV, NDJSON and ZIP(JSON).
  Unknown formats fail closed; `ExportCodecRegistryTest` verifies encoding and
  ZIP round-trip. SDMX-JSON and SDMX-XML codecs and conformance assertions are
  now included; Parquet is closed for implementation and Linux round-trip
  conformance. SDK codecs remain open until their toolchain and fixtures are
  added.
- Parquet runtime toolchain is pinned in the API build boundary (`avro 1.11.3`,
  `parquet-avro 1.14.3`, `hadoop-common 3.3.6`,
  `hadoop-mapreduce-client-core 3.3.6`).
- `ParquetExportCodec` generates a deterministic nullable-string Avro schema
  from response fields and writes Snappy-compressed Parquet. The Linux
  conformance test covers write/read round-trip; Windows skips it because
  Hadoop LocalFileSystem rejects drive-letter URIs. CI/Linux remains required
  acceptance for this codec was completed in an isolated Docker Gradle
  8.13/JDK 17 workspace on `192.168.1.199`; the write/read round-trip passed.
  No production container, volume, network or other project path was changed.
- A persisted semantic golden corpus is now packaged under
  `api/src/test/resources/semantic-golden-corpus.json`; the analyzer test loads
  additive, scalar-breaking and nested-projection-breaking fixtures and passes
  on Windows and Linux. Contract-registry approval binding remains a separate
  open C-01/C-08 item.
- `KeysetPredicateBuilder` now accepts `StableSortSpec` directly and emits
  provider-neutral mixed ASC/DESC forward/backward tuple predicates, including
  NULL equality handling; targeted regression tests PASS. Runtime catalog
  index enforcement, concurrent-write integration and high-volume benchmark
  remain open.
- `ContractPhysicalQueryService` now executes the `StableSortSpec` overload
  directly, including declared-index lookup, contract field validation,
  mixed-direction ordering and forward/backward predicates; it no longer
  collapses the contract tuple into the legacy boolean-direction path. Compile
  and keyset regression tests PASS. Concurrent-write integration and benchmark
  evidence remain open.
- Added `KeysetConcurrentWriteSimulationTest` for insert-before-cursor
  stability and reverse traversal, plus
  `ops/cli/portability/keyset-benchmark.ps1` with deterministic seed and
  bounded timing output. Both local checks PASS; real database capacity
  measurements remain staging/production evidence.
- `ContractClientGeneratorService.sdk` now generates deterministic Java record,
  Kotlin data-class and Dart model declarations from the same contract-derived
  JSON Schema; unsupported languages fail closed and generator regression tests
  PASS. Package publishing and full SDK round-trip fixtures remain open.
- SDK conformance fixtures now exercise contract-derived Java, Kotlin and Dart
  outputs (including sanitized identifiers); generator test suite PASS.
  Artifact publishing and consumer-side compile/round-trip acceptance remain
  external delivery gates.
- Added `StreamingExportService`, a provider-neutral bounded OutputStream
  boundary with monotonic progress events, empty-payload completion and flush
  semantics. JSON streaming regression passed; transport backpressure and
  external SDK publishing remain delivery concerns.
- Streaming now enforces a caller-supplied byte budget and cooperative
  cancellation/backpressure guard; cancellation and over-budget regression
  tests PASS. Network transport tuning remains deployment-specific.
- `ContractApprovalGate` now combines lifecycle state, SHA-256 document binding
  and semantic compatibility into one fail-closed publication decision;
  unapproved, tampered and breaking revisions are rejected. Targeted gate
  tests PASS; durable DB approval receipt/API orchestration remains open.
- Added migration `082_contract_approval_receipt.sql` and idempotent
  `persistApprovalEvidence` orchestration. Evidence is keyed by
  contract/revision/checksum and stores JSON proof without duplication; API
  compilation and checksum regression PASS. Publication API wiring and
  independent steward signature remain authority gates.
- Access fixture paths were reconciled with the canonical root `samples/`
  boundary, and duplicate `platform` YAML roots were merged so the Spring
  application context loads deterministically. The three KIDS Access fixture
  tests plus the full API regression now complete with **BUILD SUCCESSFUL**.
- Access fixture assertions are now revision-8 truthful: the canonical
  `kids-portal-v1-canonical-r8-final.accdb` package is resolved independently of
  the Gradle worker directory, all entity/raw/statistical/relation tables are
  asserted by their canonical prefixed names, the 22-column `__raw_document`
  provenance envelope is verified, and the generator/audit tasks resolve their
  tools from the authoritative `ops/scripts/java` location. The Spring context
  test uses isolated non-production SQL Server placeholders, explicit Hibernate
  dialect, disabled external schedulers and test-only JWT material; it never
  imports `application-local.yml` credentials.
- Transactional-outbox dispatch is explicitly controlled by
  `platform.outbox.enabled` (production default remains enabled); context
  tests disable it alongside external schedulers so placeholder datasources
  cannot create background retries or contaminate evidence.
- The Spring context acceptance fixture now uses an explicit empty
  `application-test.yml` import, so `application-local.yml` credentials are
  excluded by construction; scheduling is disabled for tests and all four
  datasource dialect/placeholder values are explicit.
- Credential-bearing developer configuration was removed from the API
  production resource tree. A placeholder-only template now lives under
  `ops/config/templates/geostat`; BootJar duplicate-entry handling is explicit,
  and the packaged JAR inventory proves local configuration is absent.
- The same secret-boundary rule was applied to the mobile local override and
  recovered session logs: both resource-level credential files and historical
  dumps were removed; a repository-wide scan reports only `CHANGE_ME` examples
  and environment-variable references.
- The legacy frontend web configuration follows the same rule: JWT/database
  values are environment-bound, `ddl-auto` defaults to validation, and verbose
  security logging is disabled for the production profile.
- A secret-boundary preflight now scans runtime/config scope and is mandatory in
  technical acceptance. Repository-held local/production env files were
  removed; deployment secrets must be injected by the external secret store or
  deployment environment, while placeholder templates remain versioned.
- `production-evidence-readiness.json` now records the secret-boundary gate as
  an explicit `PASS`; all authority-dependent checks remain `NOT_ASSERTED` or
  `NOT_RUN` until their real evidence is supplied.
- Release-gate Git hygiene now distinguishes non-fatal CRLF advice from a real
  `git diff --check` failure; diagnostic execution succeeds without weakening
  the clean-release requirement.
- Readiness summary serialization now has an explicit `FAIL` bucket, so failed
  checks are represented deterministically rather than causing a secondary
  reporting error; production decision remains fail-closed.
- Secret-boundary policy is location-aware: any repository-local
  `shared/secrets/*.env` file is rejected even before value inspection, forcing
  deployment secrets into an external secret manager/environment.
- All local preflight/bootstrap/deploy entrypoints now resolve credentials through
  the `GEOSTAT_ENV_FILE` operator-supplied path (with an untracked runtime-path
  fallback). Compose development uses the same injection contract; no command
  assumes a repository secret file exists.
- A deployment-secret-injection preflight verifies that every operational entrypoint
  honors this boundary and that legacy repository secret paths cannot reappear.
- Release-gate secret scanning now includes the frontend application tree as
  well as backend/runtime sources, keeping the boundary uniform across all
  Geostat services.
- Provider discovery startup now has an explicit fail-closed regression test:
  when discovery is enabled and no ACTIVE capability is returned,
  `ProviderCapabilityDiscoveryRunner` aborts startup. Targeted test PASS;
  multi-path provider negotiation and real failover remain open.
- `DataFamilyLifecycleOrchestrator.acceptSurface` now applies the same
  relation/projection shape gate to every concrete family and rejects malformed
  arrays or null entries before publication. All six family paths are covered
  by a targeted regression test; provider-specific execution and full
  relation semantics remain separate acceptance work.
- `RelationalStatisticalQueryAdapter` now provides an alternate configurable
  relational implementation of the statistical port. Physical table
  identifiers are validated at construction and supplied by provider metadata;
  registry and unsafe-identifier tests PASS. Real provider performance and
  capability-negotiation evidence remain open.
- Portability matrix output now carries a deterministic SHA-256 fingerprint and
  an explicit evidence contract (row-count, checksum, FK, cardinality,
  evolution and capacity). The planner replay produced four honest `NOT_RUN`
  profiles because no approved SQL Server/MySQL endpoints are configured.
- `ops/cli/validation/documentation-consistency.ps1` now generates a
  machine-readable API/schema/documentation consistency report and is wired
  into both documentation-zero-drift and release-gate validation. Local report
  status is PASS with all required artifacts and authority markers present.
- `legacy-retirement-preflight.ps1` now provides a fail-closed retirement
  readiness report: production fallback defaults to disabled, the policy
  boundary and usage telemetry are present, and migration notice is required.
  Local result is `READY_FOR_IMPACT_REVIEW`; no automatic deletion is allowed
  without consumer-impact/steward approval, deprecation window and rollback
  plan.
- Release-gate path bindings were reconciled with the canonical `ops/cli`
  layout (`validation/audit-checklist-count.ps1`, `data/*ledger.ps1`, and
  `validation/supply-chain-preflight.ps1`). Diagnostic `-AllowDirty` replay
  now passes all structural/provenance checks; clean signed release remains
  production-authority gated.

## 8. Current status snapshot — 2026-09-14

The repository now has a canonical multi-plane layout. The Control Plane UI
source is `platform/apps/geostat/frontend/geostat-system-app`; its deployment
Compose, configuration, Nginx and PowerShell tooling are owned by the
corresponding `ops/compose/.../services/control-plane-ui`,
`ops/config/.../services/control-plane-ui` and `ops/scripts/.../control-plane-ui`
boundaries. The legacy `platform/apps/geostat/frontend/web` runtime and old
upload flow remain retained until migration acceptance and consumer-impact
review are complete.

### Implementation-ready closure scope

The following concrete work can be completed with repository code, fixtures,
local containers and automated evidence:

1. Contract persistence/API orchestration: durable revision storage, approval,
   supersede, rollback, checksum binding and publication/deploy-ledger linkage.
2. Runtime provider discovery: ACTIVE capability lookup, startup registration,
   health checks, lifecycle transitions, failover hooks and negotiation on every
   query/materialization path.
3. Family lifecycle: one onboarding → validation → materialization → publish →
   quarantine/rollback orchestration for ENTITY, STATISTICAL, REFERENCE, RAW,
   GEO and RELATION, including cross-family relation/projection acceptance.
4. Statistical abstraction: alternate storage adapter, SDMX measure/unit/
   aggregation/dimension negotiation and provider-neutral regression/performance
   suite.
5. Keyset execution: provider-neutral SQL tuple predicate, runtime index
   introspection/enforcement, backward cursor, concurrent-write integration and
   benchmark harness.
6. Semantic compatibility: persisted-contract integration, complete golden
   fixture corpus, machine-readable migration guidance and approval binding.
7. Legacy retirement tooling: migration notice, usage telemetry, impact review,
   deprecation threshold and controlled removal of the old dynamic-pages/upload
   path.
8. Interoperability: SDMX-JSON/XML, Parquet, ZIP, Java/Kotlin/Dart SDK
   generation, streaming/progress events and round-trip/conformance fixtures.
9. Documentation reconciliation: complete contract-row extraction, deployment
   binding and generated API/schema/documentation consistency reports.

### Production-authority-required closure scope

The following require external endpoints, approved policy, production access or
measured evidence and cannot be honestly closed by local implementation alone:

1. A real independent provider/site: different schema/table names, composite
   keys, relation/classifier data, full ingest → materialization → publication →
   API → export → rollback replay and signed acceptance report.
2. Real SQL Server and MySQL portability runs covering default and non-default
   schemas, composite keys, large tables and schema evolution.
3. Real OIDC issuer/JWKS, asymmetric validation, RBAC/ABAC, tenant/site/
   dataset/field isolation, production Redis binding, HA/failover/fairness and
   expired/wrong-audience/revoked/cross-tenant negative tests.
4. Durable telemetry backend, trace/log correlation, dashboards, SLO/error
   budget, alert routes/firing evidence, real backup/restore/replay/rollback and
   measured RPO/RTO.
5. Approved signed release tag/commit, clean reproducible deploy, deploy
   authority, supply-chain attestation and signed security/load/chaos/DR reports
   from an approved test window.

These items remain explicitly open until their external endpoint, authority,
approval or measured evidence exists. Local PASS results never substitute for
those production gates.


 DataFamilyLifecycle-ზე გადავდივარ: ვამატებ orchestration layer-ს, რომელიც ყველა family-ს ერთსა და იმავე governed transition pipeline-ში ატარებს (RECEIVED → STAGED → VALIDATED → MATERIALIZED → RECONCILED → PUBLISHED, ან     
quarantine/rollback), idempotency-ით და fail-closed წესით. შემდეგ პირდაპირ ტესტით დავადასტურებ.



რაც შეგვიძლია არსებული ბაზით/სერვერით:

- არსებული GeoStat DB-ის read-only შემოწმება;
- migration/contract/schema verification;
- KIDS ingest/materialization replay;
- row/FK/relation/checksum reconciliation;
- Redis/OTel/backup staging tests;
- API და export end-to-end replay;
- keyset და portability harness-ის გაშვება.

### Staging evidence reconciliation — 2026-09-14

The authoritative KIDS R8 acceptance record confirms batch/materialization,
publication, reconciliation, archive object counts, rollback/replay and
checksum-verified backup/DR restore. The live GeoStat health check confirms all
DB planes, object storage and API are UP; authenticated Redis ping returned
`PONG`; full API regression and technical acceptance are PASS. Protected API
query/export and SQL Server/MySQL portability remain NOT_RUN where JWT or
approved provider endpoints are absent.

რაც არსებული ბაზა თავისით ვერ ამტკიცებს:

- მეორე დამოუკიდებელი provider-ის არსებობას;
- steward/business-owner approval-ს;
- approved release authority-ს;
- ოფიციალურ RPO/RTO-სა და load/chaos test window-ს.


მიმდინარე მდგომარეობა ასეთია:

დადასტურებულია:

- GeoStat Docker გარემო და health:
  - primary DB — UP;
  - data plane — UP;
  - archive plane — UP;
  - secondary DB — UP;
  - object storage — UP.

- migration/runtime ledger verification — PASS;
- contract/schema static verification — PASS;
- full API regression — BUILD SUCCESSFUL;
- keyset algorithm/concurrency simulation/benchmark harness — PASS;
- portability planner — უსაფრთხოდ მუშაობს, მაგრამ provider profiles არის NOT_RUN.

ჯერ დარჩენილია რეალურად ჩასატარებელი:

- protected API query + relation/include + export end-to-end replay (JWT საჭიროებაა);
- durable OTel traces/log backend და alert firing evidence;
- რეალურ SQL Server/MySQL endpoint-ზე portability harness;
- publication pointer/cache/rollback-ის ახალი release-ზე evidence bundle;
- production authority: approved release, steward approval, RPO/RTO და test window.

KIDS R8-ის ingest/materialization, reconciliation, archive, rollback/replay და
backup/restore evidence უკვე დადასტურებულია შესაბამისი acceptance record-ით.

### Implementation closure — contract capability introspection (2026-09-14)

- [x] `allowedAggregations` აღარ განისაზღვრება `nodeKind`/KIDS branch-ით.
- [x] introspection ახლა აბრუნებს უნივერსალურ `COUNT`-ს და შესაბამისი dataset-ის
  approved/provisionally-approved `platform.metric.aggregation` registry-ს;
- [x] შედეგი revision-independent family naming-ზე არ არის მიბმული და ახალი
  provider/data-family-ის capability-ს registry-დან იღებს;
- [x] `:api:test` სრული suite: **BUILD SUCCESSFUL**.

ეს ხურავს მხოლოდ introspection-ის aggregation-capability implementation slice-ს.
რეალური provider/production approval, endpoint binding და measured evidence-ის
ღია საკითხები უცვლელი რჩება ზემოთ აღწერილი production-authority scope-ის მიხედვით.

### Query-plan validation closure — 2026-09-14

- [x] `ContractQueryPlanService`-მა hardcoded `SUM/COUNT/AVG/MIN/MAX` whitelist
  მოიშორა და aggregation validation გადაიტანა approved metric registry-ზე;
- [x] `COUNT` დარჩა მხოლოდ უნივერსალურ cardinality ოპერაციად, ხოლო ყველა სხვა
  ოპერაცია უნდა იყოს კონკრეტული dataset-ის კონტრაქტულ/semantic registry-ში;
- [x] validation query revision-scoped contract dataset-თან არის დაკავშირებული;
- [x] სრული `:api:test` suite: **BUILD SUCCESSFUL**.

### Keyset adapter neutrality closure — 2026-09-14

- [x] keyset read path აღარ კრძალავს statistical node-ს `nodeKind`-ის
  მიხედვით; stable-key/profile validation სრულდება declared contract/physical
  adapter layer-ში და არა family-specific branch-ით.
- [x] სრული `:api:test` suite: **BUILD SUCCESSFUL**.

### Response relation projection closure — 2026-09-14

- [x] page response-ის `relations` ველი ახლა იკითხება approved contract
  revision-ის `site_contract_relation` ჩანაწერებიდან;
- [x] hardcoded `raw.source_record`/`publication.dataset_snapshot`/
  `classification.item` სია ამოღებულია, ამიტომ სხვა provider/site-ის graph არ
  ბინძურდება KIDS-ით;
- [x] relation-less dataset უსაფრთხოდ აბრუნებს ცარიელ სიას; სრული API test:
  **BUILD SUCCESSFUL**.

### Cross-family relation/projection acceptance — 2026-09-15

- [x] contract-only end-to-end acceptance დაემატა
  `CrossFamilyRelationProjectionAcceptanceTest`-ში: persisted-contract-shaped
  metadata → generic relation graph → `ENTITY→REFERENCE` და
  `RAW→STATISTICAL` joins → nested include/projection → response redaction;
- [x] test runner-ში ახალი gate ჩაბმულია და technical acceptance replay არის
  **29/29 PASS**;
- [ ] რეალური production provider/database acceptance (ყველა family/provider,
  signed authority და measured evidence) ჯერ არ არის დასტურიანი და fail-closed
  წესით ცრუ PASS-ად არ აღინიშნება.

### Cursor contract-validation closure — 2026-09-14

- [x] `ContractQueryController`-ში keyset cursor resume contract compiler-ს
  გვერდს ვეღარ უვლის; ყველა execution mode ჯერ ამოწმებს approved page/dataset
  contract-ს, შემდეგ ამოწმებს cursor-ს და კითხულობს data plane-ს;
- [x] full `:api:test` regression: **BUILD SUCCESSFUL**.

### Order-by contract validation closure — 2026-09-14

- [x] `orderBy`-ის ყველა field მოწმდება revision-ის declared fields/aliases-ზე;
- [x] sort direction შეზღუდულია მხოლოდ `ASC`/`DESC`-ზე და სხვა მნიშვნელობა
  fail-closed-ად უარყოფილია;
- [x] წესი მოქმედებს cursor და offset execution-ის საერთო compiler ეტაპზე;
- [x] სრული `:api:test`: **BUILD SUCCESSFUL**.

### Include governance closure — 2026-09-14

- [x] `include` და `includeLimits` ახლა მხოლოდ revision-ის declared
  `site_contract_relation.relation_code` მნიშვნელობებს იღებს;
- [x] დაუდეკლარირებელი relation/include fail-closed-ად უარყოფილია და KIDS-ის
  implicit include აღარ არსებობს;
- [x] სრული `:api:test`: **BUILD SUCCESSFUL**.

### Audit checklist reconciliation — 2026-09-14

- [x] local second-provider contract-only acceptance is explicitly marked
  complete; its real external-provider acceptance remains a separate gate;
- [x] query-cost/rate/quota implementation is explicitly marked complete;
  production Redis binding, HA/fairness and measured load evidence remain open;
- [x] canonical audit count reconciled to **219 verified / 10 open**.

### Read-only remote staging smoke closure — 2026-09-14

- [x] დაემატა `ops/cli/validation/remote-staging-smoke.ps1`, რომელიც მხოლოდ
  GEOSOTAT-ის საკუთარ container names-სა და API-ს ამოწმებს და mutation-ს არ
  ასრულებს;
- [x] 192.168.1.199-ზე evidence: 5 GEOSOTAT containers PASS, `/health` HTTP
  200, protected contract endpoint without token HTTP 401;
- [x] machine-readable report: `build/remote-staging-smoke.json`;
- [x] ეს არის staging/read-only evidence და არა OIDC/JWKS, production approval,
  DR, load ან signed release-ის შემცვლელი.

### Keyset revision binding closure — 2026-09-14

- [x] keyset cursor-ის signed snapshot/revision claim ახლა მოწმდება მიმდინარე
  approved page contract revision-თან data-plane execution-მდე;
- [x] superseded contract-ზე გაცემული cursor fail-closed-ად უარყოფილია;
- [x] სრული `:api:test`: **BUILD SUCCESSFUL**.

### Default-contract neutrality closure — 2026-09-14

- [x] Canonical page, SDMX compatibility და async export endpoints აღარ შეიცავს
  `KIDS_PORTAL_V1` hardcoded default-ს;
- [x] contract omission შემთხვევაში default იკითხება `ApprovedContractResolver`-ის
  governance registry-დან, ხოლო approved contract-ის არქონისას request fail-closed-ად
  წყდება;
- [x] სრული `:api:test`: **BUILD SUCCESSFUL**.

### Access locator authority closure — 2026-09-14

- [x] `PlatformAccessIngestionService`-დან KIDS dataset/table-name mapping
  ამოღებულია;
- [x] Access physical table-ის სახელი ახლა მხოლოდ approved contract source
  locator-იდან მოდის; ingestion layer აღარ ასკვნის prefix/family-ს runtime-ში;
- [x] ახალი schema-agnostic runtime preflight (`schema-agnostic-runtime-preflight.ps1`)
  PASS — 188 Java source file, 0 site-literal violation;
- [x] სრული `:api:test`: **BUILD SUCCESSFUL**.

### Revision-scoped aggregation introspection closure — 2026-09-14

- [x] aggregation capability query now joins `site_contract_dataset` and
  requires the requested `site_contract_revision_id`;
- [x] older/newer revision-ის metric registry accidental leakage prevented;
- [x] სრული `:api:test`: **BUILD SUCCESSFUL**.

### Remote probe contract neutrality — 2026-09-14

- [x] remote staging smoke harness-ს protected endpoint-ის contract code ახლა
  explicit parameter-ად მიეწოდება; source-ში KIDS contract literal აღარ არის;
- [x] KIDS staging evidence ხელახლა შესრულდა explicit `-ContractCode`-ით და
  ყველა 7 probe PASS გახდა;
- [x] harness-ის default რეჟიმი სხვა site/provider-ზე მუშაობას არ ზღუდავს.

### SDMX codelist page discovery closure — 2026-09-14

- [x] SDMX codelist facade აღარ იყენებს hardcoded `pageId=12`-ს;
- [x] reference page ახლა იძებნება approved contract-ის active
  `REFERENCE_REGISTRY` binding-იდან;
- [x] სრული `:api:test`: **BUILD SUCCESSFUL**.

### SDMX dimension-key neutrality closure — 2026-09-14

- [x] SDMX data key segments now map to the flow's approved DSD dimension
  components (`component_order`); `AGE_GROUP` literal removed;
- [x] undeclared/no-dimension key usage fails closed instead of silently
  applying a KIDS-specific filter;
- [x] სრული `:api:test`: **BUILD SUCCESSFUL**.

### Keyset snapshot-boundary API closure — 2026-09-14

- [x] snapshot/revision-aware keyset overload is now the only public service
  entry point; unbound legacy overload removed;
- [x] every keyset caller must provide the signed cursor snapshot claim;
- [x] stale/superseded contract cursors fail closed; full API test PASS.

### Current evidence reconciliation — 2026-09-14T18:20Z

- [x] OIDC audience validation is isolated in the provider-neutral
  `OidcAudienceValidator`; positive, negative and invalid-policy tests pass.
- [x] Technical acceptance replay completed with **29/29 PASS**; the report
  includes release artefact checks, remote staging/infrastructure/OIDC
  readiness, provider/family/semantic/keyset checks, OpenAPI/export checks,
  runtime-ledger integrity, supply-chain preflight and full API regression.
- [x] Audit checklist and documentation zero-drift checks both pass at
  **271 verified / 10 open**.
- [x] Evidence bundle r31 was generated and cryptographically verified:
  `build/geostat-staging-evidence-bundle-r31.json`, SHA-256
  `9c4debcf0fd616325a46090c79d54f74020cbe368d62523c2b662daf0703f1ad`.
- [ ] Production-only gates remain open: approved signed release/deploy
  authority, real API OIDC/ABAC/tenant activation and JWT replay, external
  SQL Server/MySQL workload endpoints, production Redis HA/fairness evidence,
  durable OTel/SLO/alerts, measured backup/DR RPO/RTO, and signed
  load/chaos/security/publication approvals. These are not inferred from local
  or staging PASS results.

### Current evidence reconciliation — 2026-09-15T08:18Z

- [x] Technical acceptance replay completed with **32/32 PASS**.
- [x] Runtime ledger regenerated with **278 verified / 10 open**; the open
  count remains fail-closed and is not reduced by local-only tests.
- [x] OTel Collector durability hardening (file-backed queue, persistent
  storage volume and health checks) passed YAML, compose and secret-injection
  preflight checks; this is implementation/staging evidence, not durable
  production-backend or alert-firing evidence.
- [x] Immutable evidence bundle `build/geostat-staging-evidence-bundle-r66.json`
  generated and cryptographically verified (SHA-256
  `acb70db64cbf05decd8b8535d46506e8d76092afa44ad6490a4cc0cd724c32b8`).
- [x] Canonical R8 Access artifact fingerprint was independently revalidated
  and added to the KIDS acceptance/status documents; historical acceptance
  fingerprint drift is explicit and requires exact-byte replay before a new
  production publication.
- [x] Canonical R8 Access physical package test passed against the current
  fingerprint (`KidsPortalCanonicalAccessPackageTest`: primary-key and package
  structure invariants); machine-readable result is retained in the Gradle
  test-results XML.
- [x] Canonical R8 artifact and its CI/build copy are byte-for-byte identical
  (SHA-256 `1930EAD852912858F25D85867FC075AAFFECD7F6704C2540E41E623764D27ACC`).
- [x] KIDS R8 API delivery runbook added with page map, capability discovery,
  entity/statistical/cross-family request-response examples, pagination,
  authorization/error contract and production release checklist.
- [x] `kids-r8-api-delivery-preflight.ps1` is now a mandatory technical-
  acceptance gate; it verifies the exact R8 artifact fingerprint, runbook/API
  markers, technical report and read-only remote staging evidence.
- [x] Technical acceptance replay after the new gate: **33/33 PASS**.
- [x] Release-tree inventory classified the dirty state: the 341 deletions
  are legacy root `api/`/`web/` paths being replaced by the canonical
  `platform/apps/geostat/...` tree, while the 21 untracked groups are the new
  platform/ops/docs/sample boundaries. No deletion was restored or removed
  automatically; release commit assembly remains an authority-controlled step.
- [x] GEOSOTAT-only remote staging smoke replay refreshed: 8/8 checks PASS
  (healthy API/Redis/Keycloak/OTel/MinIO, public health, protected contract
  endpoint rejects missing and malformed tokens with HTTP 401); the probe is
  read-only and does not assert production authorization or publication.
- [x] Remote GEOSOTAT deployment workspace was located without mutation at
  `/home/administrator/geostat/backend/infra/geostat-platform`; this is the
  canonical isolated infrastructure boundary and does not overlap sibling
  projects.
- [ ] Remote production Compose config replay remains blocked fail-closed by
  missing operator secret `GRAFANA_ADMIN_PASSWORD` in `.env.prod`; no secret
  was invented or written. The missing authority/configuration must be supplied
  before any build or deployment action.
- [ ] Remote container provenance is not yet aligned with the canonical
  project boundary: the running `geostat-api` reports Compose project `api`,
  config `/home/administrator/geostat/backend/api/docker-compose.prod.yml`,
  and no `org.opencontainers.image.revision` label. It must not be treated as
  the new reproducible `backend/infra/geostat-platform` release until a clean,
  approved deployment replaces it and records commit/image evidence.
- [ ] Read-only comparison confirms the active remote per-service Compose file
  is byte-different from the repository canonical project Compose (remote
  SHA-256 `e3edf23fbe181316fe1aad188192d3de53a3a24ba7c5feb30e4bb5cea5460e8a`,
  local SHA-256 `B86603F720A309C01786923BE97F35A963D1372D8B81C1BDEAF0A5C26AA2BE33`).
  Deployment migration must therefore be an approved, reversible release step,
  not an implicit overwrite.
- [ ] The isolated remote canonical folder itself exists at
  `/home/administrator/geostat/backend/infra/geostat-platform`, but its
  Compose SHA-256 is `1642c17457c783ea31677e656d2cc3408232fa7737fabd6ade816ec703578d06`
  (different from the repository authority), and its `.env.prod` lacks the
  required `GRAFANA_ADMIN_PASSWORD`; folder presence alone is therefore not
  deployment evidence.
- [ ] Production authority/evidence gates remain open: approved release and
  deploy authority, real OIDC/ABAC/tenant activation, independent external
  provider workload, production Redis HA/fairness, durable telemetry backend
  and alerts, measured backup/DR RPO/RTO, and signed load/chaos/security
  acceptance.

### Production-readiness correction — 2026-09-15

- [x] Production boot artifacts rebuilt from the current workspace
  (`:api:bootJar :mobile:bootJar --rerun-tasks`): `BUILD SUCCESSFUL`.
- [x] Technical acceptance rerun after the build: **33/33 PASS**; runtime
  ledger remains **278 verified / 10 open**.
- [x] KIDS R8 delivery preflight is now schema-stable and cycle-safe: it does
  not depend on a hard-coded technical-check count; the technical runner
  defers that one check to its current report, while standalone execution still
  requires a successful report.
- [x] Fresh immutable evidence bundle
  `build/geostat-staging-evidence-bundle-r69.json` was generated and verified;
  SHA-256 `90a09f91c8d4976a290706d42aeb85b3e255816fb3d6ec07bbc1b33253a92b90`.
- [ ] Production deployment remains intentionally fail-closed: the working
  tree is dirty, the remote API is running from the legacy Compose path,
  operator secret values are not supplied, and signed release/deploy authority
  plus external OIDC/ABAC, DR, load and chaos evidence are not asserted.

### Remote Compose alignment — 2026-09-15

The GEOSOTAT API service boundary was repaired without touching sibling
projects: the canonical service Compose generated from
`ops/compose/projects/geostat/docker-compose.prod.yml` now replaces
`/home/administrator/geostat/backend/api/docker-compose.prod.yml` on the host.
The prior file is retained at
`docker-compose.prod.yml.pre-canonical-20260915T090459Z` with its original
SHA-256 for rollback. The container was deliberately not recreated because the
remote operator environment lacks the required `IMAGE_REVISION` and approved
runtime values; image/provenance and production gates therefore remain open.

### One-time infrastructure secret repair — 2026-09-15

An operator-authorized server-side random `GRAFANA_ADMIN_PASSWORD` was added
only because the isolated infra env lacked that key. The original file was
backed up, permissions were restricted to `0600`, the value was not emitted or
recorded, and isolated infra Compose validation now passes. `IMAGE_REVISION`
remains unset until an approved immutable release commit/tag is selected.

### Clean-candidate provenance — 2026-09-15

The clean release candidate is `c782623b601337777d31983b52a2373db237a843`.
The remote API image rebuilt with that revision has digest
`sha256:c9d04c28958a0fb1d7125e380c90905aa029a7ff5d1f6d5b1ef1b69b6f1fce03`
and matching OCI labels. The live container has not been recreated; signed tag
and deployment authority remain the final activation gates.

### Technical closure replay — 2026-09-16

- [x] C-01 lifecycle/checksum/approval orchestration regression replayed with
  `ContractLifecycle*`, `ContractCompatibility*` and checksum tests; `:api:test`
  completed successfully.
- [x] C-02 provider capability discovery, health supervisor and lifecycle
  regression replayed; deterministic priority/failover and no-candidate
  fail-closed behavior remain covered.
- [x] C-08 persisted semantic golden corpus and contract compatibility fixtures
  replayed; scalar, collection, nested projection and response-schema cases
  passed with machine-readable approval guidance.
- [x] C-10 SDK/export/streaming registry and codec regression replayed;
  JSON/CSV/NDJSON, SDMX-JSON/XML, Parquet, ZIP and bounded streaming tests
  passed in the API suite.
- [x] KIDS page 9 source-key scope reconciliation and page 11 statistical shadow
  parity are recorded as immutable evidence (`missing=0`, `extra=0` within
  declared scope, and `880/880` typed statistical cells).
- [ ] External provider workload, SQL Server/MySQL workload matrix, live
  production authority and steward approval remain `NOT_ASSERTED`; local test
  success is not promoted to production evidence.

### Continuous execution replay — 2026-09-16

- [x] Contract/schema and adapter/API regression chain completed: `:api:test`
  is `BUILD SUCCESSFUL`.
- [x] KIDS data reconciliation chain completed: goals parity, glossary parity,
  statistical shadow parity (`880/880`) and resource scope reconciliation all
  report PASS.
- [x] Documentation consistency, runtime-ledger generation, host-layout and
  schema-agnostic preflights report PASS (`278 verified / 10 open`).
- [x] Secure production overlay and deployment-secret injection boundaries
  report PASS without exposing or inventing credentials.
- [x] Duplicate migration sequence defect fixed: canonical storage binding is
  migration `085`; runtime ledger now contains unique ordered IDs.
- [ ] Local Docker engine is unavailable in this workstation; Docker/staging
  execution is therefore represented by validated Compose/overlay contracts,
  not falsely reported as a container run. Remote production activation still
  requires operator authority and external evidence.
