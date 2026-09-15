# Contract-driven / Metadata-driven / Schema-agnostic completion status

**Canonical source:** `docs/contract-driven-metadata-schema-agnostic-completion-plan.md`  
**Status basis:** checklist section 6, current repository evidence, technical acceptance 29/29.  
**Meaning:** `[x]` implementation/test evidence complete; `[-]` implementation exists but
runtime, persistence, external-provider or approval evidence is incomplete; `[ ]` not
closed and no completion claim is allowed.

## Summary

| Category | Count | Meaning |
|---|---:|---|
| Closed `[x]` | 32 | Verified implementation and/or automated test evidence exists |
| Partial `[-]` | 23 | Technical foundation exists; closure evidence or integration remains |
| Open `[ ]` | 21 | Work or external production evidence is still required |

## C-01 — Contract authority

- `[x]` Versioned contract/page/dataset/field/relation/projection baseline.
- `[-]` Immutable checksum/revision core and approval-evidence persistence exist; publication, supersede and rollback binding remain.
- `[-]` Provider-neutral semantic breaking-change analyzer and nested comparison exist; durable contract/policy integration remains.
- `[-]` Approval-state API and durable evidence write path exist; supersede/rollback orchestration and production approval remain.
- `[x]` Documentation drift gate is enforced and PASS evidence is recorded.

**Closable now:** baseline, durable lifecycle orchestration and drift gate.
**Production-only:** external approval, publication authority and signed authority.

## C-02 — Provider capability/discovery

- `[x]` Provider-neutral capability registry core.
- `[x]` Family/operation/feature validation and duplicate/retire fail-closed tests.
- `[-]` Control Plane persistence and ACTIVE-row discovery exist; reload atomically replaces the active set and removes stale providers; production failover binding remains.
- `[-]` Provider lifecycle state machine plus provider-neutral `ProviderHealthSupervisor` (deterministic priority/failover and fail-closed no-candidate behavior) are tested; production health probe/retirement binding remains.
- `[-]` Capability negotiation core is tested; complete runtime binding across external paths remains.

**Production-only:** real provider health, failover and restart evidence.

## C-03 — Independent provider/site

- `[x]` Local contract-only onboarding proof.
- `[x]` Local database replay proof, including non-default schema, composite identity and cross-family relation/projection (`IndependentProviderCompositeAcceptanceTest`).
- `[ ]` Real independent DB/provider binding.
- `[ ]` Full ingest → materialization → publication → API → export → rollback replay.
- `[ ]` Signed independent acceptance report.

**Production-only:** all three open items require a real second provider and signed evidence.

## C-04 — Family lifecycle

- `[x]` ENTITY/STATISTICAL/REFERENCE/RAW/GEO/RELATION adapter baseline.
- `[-]` Provider/family-neutral lifecycle coordinator now uses `LifecycleStateStore` plus SQL-backed `JdbcLifecycleStateStore` (migration 083); restart/idempotency/JDBC regression passes; production deployment binding remains.
- `[x]` Cross-family lifecycle conformance for all six canonical families.
- `[-]` Contract-only cross-family relation/projection acceptance (`ENTITY→REFERENCE`, `RAW→STATISTICAL`) passes; real provider/database execution for every family remains.
- `[x]` Generic engine has no family-specific branch; registry/adapter delegation and regression pass.

**Closable now:** generic implementation and synthetic acceptance. **Production-only:**
physical cross-family replay for every provider/family.

## C-05 — Statistical abstraction

- `[x]` Registry-based dimension/alias resolution.
- `[x]` Canonical statistical adapter boundary.
- `[x]` Alternate relational storage adapter with identifier/registry tests.
- `[-]` Query service uses the adapter port; storage-name-independent capability negotiation remains.
- `[ ]` Provider-independent performance/regression evidence.

## C-06 — Portability matrix

- `[x]` Provider SPI and identifier/connection validation.
- `[x]` Parameterized provider execution boundary.
- `[ ]` SQL Server matrix run.
- `[ ]` MySQL matrix run.
- `[ ]` Non-default schema/composite-key/large-table/schema-evolution runs.
- `[ ]` Reproducible matrix report.

**Status:** planner is implemented and fail-closed; real endpoints/workloads are required.

## C-07 — Keyset/query hardening

- `[x]` HMAC scope, tuple values, fingerprint and snapshot binding.
- `[x]` Ascending/descending tuple and unsafe-identifier tests.
- `[-]` Null-order metadata/validation and explicit provider-neutral SQL predicate branches are implemented and tested; provider catalog integration remains.
- `[-]` Contract-declared stable-index requirement exists; runtime catalog enforcement remains.
- `[-]` Backward cursor and concurrent-write simulation exist; real DB concurrency remains.
- `[-]` Provider-independent benchmark harness exists; real DB workload benchmark remains.

## C-08 — Semantic compatibility

- `[x]` Field and relation compatibility baseline.
- `[-]` Metric/unit/aggregation analyzer and persisted-contract/golden tests exist; steward policy approval remains.
- `[-]` Dimension/code-list analyzer exists; governed corpus approval remains.
- `[-]` Projection/quality/privacy/response-schema analyzer exists; durable policy binding remains.
- `[-]` Breaking/non-breaking guidance exists; complete golden fixture corpus remains.

## C-09 — Legacy boundary

- `[x]` Contract-engine-first dynamic-page path.
- `[-]` Feature flag, provider-neutral fallback policy and retirement notice exist; approved deprecation window remains.
- `[-]` Fallback telemetry exists; durable dashboard and retirement threshold remain.
- `[x]` No-fallback regression suite.
- `[-]` Controlled retirement decision requires impact review, steward approval and rollback window.

## C-10 — Interoperability

- `[x]` OpenAPI 3.1, JSON Schema, TypeScript, SDMX facade and JSON/CSV/NDJSON.
- `[x]` SDMX-JSON/XML providers.
- `[x]` Parquet/ZIP providers with Linux/Docker round-trip evidence.
- `[x]` Java/Kotlin/Dart SDK generation and streaming/progress/cancellation primitives.
- `[-]` SDK/export round-trip fixtures exist; external consumer conformance remains.

## C-11 — Security/tenancy/cost

- `[x]` Fail-closed JWT guard, bounded query-cost/rate-limit implementation.
- `[x]` Redis adapter and decision telemetry.
- `[-]` Authorization preflight and controller guards exist; real OIDC/JWKS negative replay remains.
- `[ ]` Real OIDC/JWKS asymmetric validation.
- `[ ]` RBAC/ABAC and tenant/site/dataset/field isolation.
- `[ ]` Production Redis HA/fairness/overload evidence.
- `[ ]` Negative authorization suite.

## C-12 — Observability/reconciliation/DR

- `[x]` OTel Collector and OTLP metrics wiring.
- `[x]` Redaction hooks and evidence-bundle verifier.
- `[ ]` Durable telemetry backend, trace/log correlation and dashboards.
- `[ ]` SLO/error budget and alert firing.
- `[ ]` Real backup/restore/replay/rollback.
- `[ ]` Measured RPO/RTO.

## C-13 — Release/resilience assurance

- `[x]` Release gate, image revision and technical acceptance runner.
- `[x]` Reproducible local API regression.
- `[ ]` Approved signed tag/commit and clean production deploy.
- `[ ]` SBOM/provenance/security scan.
- `[ ]` Approved load/chaos/DR window and abort rules.
- `[ ]` Signed security/load/chaos/DR reports.

## C-14 — Documentation/runtime reconciliation

- `[x]` Audit, capability-gap, authority register and learning guide.
- `[x]` Documentation zero-drift checker.
- `[-]` Generated migration/runtime ledger now includes migration hashes, contract markers, implementation markers and GEOSOTAT Compose deployment bindings; external runtime deployment attestation remains.
- `[ ]` R6/R7/legacy claims final reconciliation.
- `[x]` Generated API/schema/docs consistency report.

## Final isolation

### Can be closed with current repository authority

The `[x]` items above, plus the remaining partial items that require only local code,
tests and generated evidence (notably persistence-backed lifecycle wiring, contract
registry integration and full golden fixtures).

### Partially closed

All `[-]` items: the implementation baseline is present, but a named integration,
runtime binding, provider workload, steward approval or immutable evidence artifact is
still missing.

### Cannot be closed now

The `[ ]` items requiring external authority or unavailable infrastructure: real
independent provider, SQL Server/MySQL workload endpoints, production OIDC policy,
Redis HA, durable observability, backup/DR targets, approved release authority,
approved test window and signed production reports. These remain fail-closed and are
tracked in `docs/production-blockers-and-required-authority.md` (B-01–B-07).

## Latest local execution evidence — 2026-09-15

- `:api:test --no-daemon --rerun-tasks`: **BUILD SUCCESSFUL**;
- cross-family relation/projection acceptance: **PASS**;
- technical acceptance: **29/29 PASS**;
- documentation zero-drift and checklist consistency: **PASS**.

This evidence closes the local implementation/test side only. It does not promote
the external production items above to `VERIFIED`.

Additional local closure evidence: `DataFamilyLifecycleOrchestratorTest` now proves
state restoration across orchestrator instances through the persistence port, and
`KeysetPredicateBuilderTest` proves explicit null-order SQL branches.

Runtime-ledger evidence: `generate-runtime-ledger.ps1` **PASS** (84 migrations,
21 contract markers, deployment bindings extracted).

Semantic closure evidence: `SemanticGoldenSurfaceAcceptanceTest` covers additive
and breaking changes across metric/unit/aggregation, dimensions/code-lists,
filters/includes, projection/response schema and policy surfaces; targeted test
**PASS** and the fixture is a mandatory technical-acceptance artifact.

The runtime ledger now treats both independent-provider and semantic-golden
acceptance tests as mandatory implementation markers; ledger regeneration **PASS**.

Persisted projection evidence: `PersistedProjectionIntegrationTest` loads an
approved revision from `platform.api_projection`, applies its nested field map to
the response, preserves identity fields and redacts undeclared data; targeted
integration test **PASS**.

Immutable evidence bundle: `build/geostat-staging-evidence-bundle-r66.json`
(`sha256=acb70db64cbf05decd8b8535d46506e8d76092afa44ad6490a4cc0cd724c32b8`),
containing the technical-acceptance report, runtime ledger, production-readiness
report, remote readiness, secure-overlay/secret-boundary evidence and production-
blocker register; verifier **PASS**. Latest technical acceptance is **32/32 PASS**;
the runtime ledger records **278 verified / 10 open** and production approval
remains **NOT_ASSERTED**.
