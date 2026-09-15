# Production Authority Input Contract

**Status:** NORMATIVE — values are deployment-specific and must be approved before a production release.

This document separates executable platform mechanisms from external authority decisions. A placeholder, local default, or running container is **not** production evidence. The release gate must fail closed while any required value or approval is absent.

## 1. Required authority record

The record is versioned together with the release and is referenced by its SHA-256 checksum. Secrets are never stored in this document or in Git; only a secret-manager reference is allowed.

| Field | Required value | Owner/approver | Runtime consumer | Evidence required |
|---|---|---|---|---|
| `oidc.issuerUrl` | HTTPS issuer URL for the approved realm | Security owner | API resource server | discovery document, issuer/audience negative tests |
| `oidc.jwksUrl` | JWKS endpoint, normally derived from issuer discovery | Security owner | API key validator | key-rotation test and observed `kid` |
| `oidc.claimMapping` | immutable mapping for subject, roles/scopes and tenant | Security + data owner | authorization policy | mapping matrix and deny-by-default tests |
| `tenancy.policyCode` | approved tenant boundary and cross-tenant deny policy | Data protection owner | query/admission layer | cross-tenant negative suite |
| `quota.provider` | `REDIS` or `SQL` with HA/consistency mode | Platform owner | `RateLimitStore` | failover, fairness and overload evidence |
| `quota.endpoint` | internal TLS endpoint or service binding | Platform owner | API runtime | connectivity/TLS/redaction evidence |
| `quota.secretRef` | secret-manager reference only | Security owner | API runtime | rotation and access audit |
| `otel.endpoint` | OTLP/gRPC or OTLP/HTTP collector endpoint | SRE owner | API telemetry exporter | trace/metric arrival evidence |
| `slo.profile` | availability, latency, error and freshness objectives | SRE + business owner | monitoring/alert rules | 30-day or approved canary evidence |
| `alert.destinations` | approved on-call routes, not personal ad-hoc addresses | SRE owner | alert manager | notification delivery test |
| `backup.target` | encrypted DB/object target and retention class | Operations owner | backup jobs | restore checksum and access audit |
| `recovery.rpo` | approved maximum data loss | Operations + business owner | DR runbook | measured restore/replay evidence |
| `recovery.rto` | approved maximum recovery time | Operations + business owner | DR runbook | timed restore evidence |
| `release.reference` | immutable signed tag or commit | Release owner | build/deploy pipeline | clean checkout, OCI revision label |
| `deploy.authority` | named service identity and approval record | Release owner | deployment control plane | authorization/audit trail |
| `test.window` | UTC start/end and approved scope for load/chaos tests | Operations owner | test runner | signed change window and report |

## 2. Canonical lifecycle

`draft → security_review → data_review → operations_review → approved → bound_to_release → verified → expired/superseded`.

Only `verified` records may satisfy a production release gate. Changing an issuer, tenant policy, quota provider, telemetry destination, recovery objective, release reference or test window creates a new revision; the old record remains immutable history.

## Temporary local/staging authentication mode

Temporary `none`/disabled authentication may be used only by an explicitly
named local or isolated staging overlay for contract development and synthetic
tests. It must never be inherited by production configuration, container image
defaults or a deploy command. The overlay must be clearly labelled
`NON_PRODUCTION_ONLY`, contain no production data, and be rejected by the
production preflight when `ENVIRONMENT=production`.

This mode does not satisfy any production OIDC/ABAC gate and cannot produce
production evidence. Before production activation the authority record must
contain a verified HTTPS issuer, JWKS discovery, audience, claim-to-role and
tenant mapping, negative authorization results, and an authenticated replay.

## 3. Non-negotiable invariants

1. No secret value is committed, logged, rendered in an API response, or placed in an Access artifact.
2. Missing or malformed authority input fails closed; it never falls back to a development default in production.
3. OIDC authorization is deny-by-default and validates issuer, audience, signature, expiry, `kid`, scopes/roles and tenant boundary.
4. Quota state is shared by all API replicas; an in-memory store is diagnostic-only.
5. Telemetry must reach a durable approved backend; a collector `debug` exporter is not production evidence.
6. RPO/RTO are claims only after a timed restore/replay test; a configured cron job is not proof.
7. Publication and deployment require the exact approved contract checksum and release reference.
8. Load or chaos execution is forbidden outside the approved UTC window and must have a rollback/abort condition.

## 4. Current GEOSOTAT evidence boundary (2026-09-14)

The isolated `geostat-platform` stack provides Redis, Keycloak/Postgres and an OTel Collector. This proves infrastructure isolation and process health only. It does **not** prove the API runtime bindings, realm/client/claim policy, distributed quota behavior, durable telemetry, backup recovery, release provenance or production test approval. Those gates remain open until the authority record above is populated and independently evidenced.

The repository also contains a secret-free, opt-in Keycloak policy at `infra/geostat-platform/keycloak/geostat-realm.json`. It defines the bearer-only API client, least-privilege roles and a `tenant_id` claim sourced from the user attribute. Importing this realm is not approval or API OIDC activation; issuer, audience, tenant assignment and negative authorization evidence remain required.

## 5. Release-gate input example (non-secret)

```yaml
authorityRevision: 1
status: APPROVED # becomes VERIFIED only after evidence is attached
oidc:
  issuerUrl: null
  jwksUrl: null
  claimMapping: null
tenancy:
  policyCode: null
quota:
  provider: null
  endpoint: null
  secretRef: null
observability:
  otelEndpoint: null
  sloProfile: null
  alertDestinations: []
recovery:
  backupTarget: null
  rpo: null
  rto: null
release:
  reference: null
  deployAuthority: null
testing:
  windowUtc: null
```

The `null` values are intentional and must not be replaced by guessed production data. The preflight must reject this record until every required field has an approved, auditable value.
