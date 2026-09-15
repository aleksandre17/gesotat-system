# GeoStat staging acceptance snapshot

Scope is read-only health and infrastructure verification against the isolated
GeoStat deployment on `192.168.1.199`.

| Check | Result | Evidence |
|---|---|---|
| Primary/data/archive/secondary DB health | PASS | API `/health` reports all four `UP` |
| Object storage health | PASS | API `/health` reports `objectStorage=UP`; MinIO container running |
| API health | PASS | `GET http://127.0.0.1:8083/health` |
| API full regression | PASS | local `:api:test` completed with `BUILD SUCCESSFUL` |
| Technical acceptance runner | PASS | 29/29 checks; production approval remains `NOT_ASSERTED` |
| OTel collector process | PASS | `geostat-otel-collector` running |
| Redis authentication | PASS | authenticated `redis-cli ping` inside isolated `geostat-redis` container returned `PONG` (secret not recorded) |
| API contract query/export replay | NOT_RUN | protected endpoint requires JWT; no test token supplied |
| KIDS ingest/materialization | PASS | KIDS R8 acceptance: batch 3, snapshots 22–36, publication snapshot 13 |
| Row/FK/relation/checksum reconciliation | PASS | KIDS R8 acceptance: 15 members, 880 published observations, 2,224 archive pointers, zero gate failures |
| Backup/restore timed replay | PASS | KIDS R8 acceptance: checksum backup, `RESTORE VERIFYONLY`, DR restore and migration-ledger read |
| SQL Server/MySQL portability | NOT_RUN | provider endpoints not configured |

| Keycloak OIDC discovery | PASS | In-network API-container probe verified the `geostat` issuer discovery document and an RSA/RS256 JWKS signing key; API production binding remains authority-gated |
| Anonymous API authorization | PASS | Remote `GET /health` returned 200 while protected `GET /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/11/query-capabilities` without credentials returned 401 |
| Anonymous denial headers | PASS | Protected 401 response included `X-Content-Type-Options: nosniff` and `Cache-Control: no-cache, no-store`; response variance was explicit via `Vary` headers |
| Repository diff hygiene | PASS | `git diff --check` completed without errors after README whitespace correction |
| Supply-chain scanner availability | NOT_RUN | Local and isolated host checks found no `syft`, `trivy` or Docker Scout; static image pinning remains enforced |
| Evidence bundle regression | PASS | Technical acceptance validates arbitrary output location and repository-root anchored referenced paths; bundle generation/verifier regression PASS |
| Contract revision binding | PASS | Physical table lookup requires the requested site contract revision via `site_contract_dataset` existence guard; API regression `BUILD SUCCESSFUL` |
| Metadata revision binding | PASS | Physical table/index introspection requires the requested site contract revision; API regression `BUILD SUCCESSFUL` |
| Runtime validator revision binding | PASS | Pre-query physical mapping validation requires the requested site contract revision; API regression `BUILD SUCCESSFUL` |
| Include contract surface | PASS | Introspection no longer advertises undeclared hardcoded includes; only revision-scoped declared relations are returned; API regression `BUILD SUCCESSFUL` |
| Release diff hygiene gate | PASS | Release gate executes `git diff --check` and returned PASS |
| Provenance documentation path | PASS | Release inventory now points to canonical `ops/cli/validation/release-gate.ps1`; documentation consistency remains PASS |
| Production evidence readiness | PASS | `build/production-evidence-readiness.json` consolidates eight external gates; summary is 0 PASS, 1 NOT_RUN, 7 NOT_ASSERTED and releaseDecision=`NOT_READY_FOR_PRODUCTION` |
| Release readiness report validation | PASS | Release gate executes and validates the consolidated readiness report; no external gate is promoted to PASS without evidence |
| Immutable evidence bundle | PASS | Latest r31 bundle generated and verified with 8 hashed evidence files and SHA-256 `9c4debcf0fd616325a46090c79d54f74020cbe368d62523c2b662daf0703f1ad` |

No data, schema, container, volume, network or non-GeoStat project was mutated.

## Recheck (2026-09-14T15:52Z)

| Component | Result | Evidence |
|---|---|---|
| MinIO | PASS | isolated `geostat-minio` container state=`running` |
| OpenTelemetry Collector | PASS | isolated `geostat-otel-collector` container state=`running` |
| Redis | PASS | isolated `geostat-redis` container state=`running`; authenticated PING previously recorded above |
| Documentation consistency | PASS | `documentation-consistency.ps1` returned machine-readable PASS |
| Runtime ledger | PASS | 83 migration checksums, 21 contract markers, 269 verified/10 open audit counts |
| Authorization surface preflight | PASS | 31 API controllers checked; global authentication, explicit resource policies and intentional public surfaces validated |
| Full technical acceptance replay | PASS | 21/21 checks, including authorization-surface preflight and full API regression |
| OIDC policy preflight | PASS | Canonical realm/client, role set, disabled direct grants/service accounts and tenant_id mapper validated; runtime API binding remains authority-gated |
| OIDC policy evidence artifact | PASS | `build/oidc-policy-acceptance.json` generated by the acceptance runner with explicit runtimeBinding=`NOT_ASSERTED` |
| Fan-out safety bound | PASS | Relation expansion and generic aggregation input capped at 1,000 rows; full `:api:test` `BUILD SUCCESSFUL` |
| Legacy retirement governance preflight | PASS | Consumer-impact, deprecation window, rollback plan and approval-record schema validated; decision remains `NO_AUTOMATIC_REMOVAL` |
| Release security preflight execution | PASS | Release gate executes authorization and OIDC policy preflights and persists machine-readable diagnostic artifacts; clean release still requires a non-dirty checkout |
| Release evidence content validation | PASS | Release gate validates preflight JSON schema/status and preserves OIDC runtimeBinding=`NOT_ASSERTED`; malformed or overclaiming evidence fails closed |
| Release gate (diagnostic) | PASS | `release-gate.ps1 -AllowDirty`; clean release remains fail-closed because working tree is dirty |
