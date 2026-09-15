# GEOSOTAT production-evidence handoff

This document is the canonical handoff for the ten audit items that cannot be
closed by local implementation tests alone. A gate is closed only when its
listed evidence is produced, independently reviewed where required, and added
to an immutable evidence bundle. No secret value is written to this document.

## Gate matrix

| Gate | Operator input | Required evidence | Closure rule |
|---|---|---|---|
| Release provenance | approved tag/commit and deploy authority | clean checkout build, OCI digest, runtime commit, deploy ledger | tag, digest and runtime revision match; clean status |
| OIDC/JWKS | issuer URL, JWKS URL, audience and approved claim mapping | HTTPS/JWKS probe, signed-token replay, expiry/audience/key-rotation results | asymmetric validation and negative suite pass |
| Tenant ABAC | approved tenant/site/product/dataset/field policy and fixtures | cross-tenant allow/deny matrix, service-to-service identity proof | deny-by-default with no cross-boundary read/write |
| Independent provider | second DB/provider endpoint and credentials | contract-only ingest → materialization → query/export → publication → rollback | no core-code diff; row/FK/relation/checksum evidence |
| Redis quota/HA | production Redis URI, secret reference and budget policy | shared-counter, failover, fairness, 429 and overload reports | atomic limits survive failover and isolate tenants |
| Durable telemetry/SLO | collector backend, retention, SLO and alert destinations | trace/log correlation, redaction, dashboard and alert-firing evidence | error budget and alert routing are operational |
| Backup/DR | backup target plus approved RPO/RTO | timed backup, restore, replay and rollback report | measured RPO/RTO meets approved objective |
| Load/chaos/security | approved test window and abort authority | signed load/soak, chaos, security and supply-chain reports | no unresolved critical finding; rollback tested |
| Publication assurance | steward/business-owner approval | immutable reconciliation bundle, snapshot/cache checksums, publication pointer | publication only from matching approved contracts |
| Governance sign-off | release authority, steward and test-window signatures | signed approval record linked to bundle hash | all external approvals are independently attributable |

## Canonical execution sequence

1. Export the approved operator manifest (including infrastructure secrets such
   as `GEOSTAT_REDIS_PASSWORD` and `GRAFANA_ADMIN_PASSWORD`) into an external
   secret provider and expose its path as `GEOSTAT_ENV_FILE`; never commit the
   resolved file.
2. Run `ops/cli/validation/production-preflight.ps1` and, when configured,
   `ops/cli/validation/oidc-runtime-preflight.ps1`.
3. Run `ops/cli/validation/technical-acceptance.ps1` and the remote read-only
   staging probes. Preserve their JSON outputs unchanged.
4. Execute only the approved provider, load, chaos, DR and security windows;
   capture timestamps, image digest, commit SHA and operator identity.
5. Generate and verify an immutable bundle with
   `ops/cli/data/generate-evidence-bundle.ps1` and
   `ops/cli/data/verify-evidence-bundle.ps1`.
6. Update the audit checklist only after the bundle and signatures satisfy the
   gate matrix. Missing authority or endpoint evidence remains `NOT_ASSERTED`
   or `NOT_RUN`.

## Safety boundary

Local PASS results prove implementation and staging behavior. They do not
authorize production publication, infer business semantics, or substitute for
RPO/RTO, tenant-policy, steward, security, or deploy approval. Any mismatch
fails closed and must be quarantined for review.
