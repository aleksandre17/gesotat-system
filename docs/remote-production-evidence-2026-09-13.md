# Remote production evidence — 2026-09-13

This note records read-only checks performed against `192.168.1.199`. No secret values, environment contents, or payloads were read.

## Verified

- SSH BatchMode connectivity: PASS.
- Host Docker runtime available: Docker 29.1.3.
- `geostat-api` container: `healthy`, restart count `0`.
- `geostat-minio` container: running.
- Canonical `GET /health`: HTTP 200, `status=UP`; `primary`, `dataPlane`, `archivePlane`, `objectStorage`, and `secondary` all `UP`.
- Deployed Compose validation from `/home/administrator/geostat/backend/api` with its protected `.env.prod`: `docker-compose config --quiet` PASS.

## Recheck — 2026-09-14

- SSH BatchMode connectivity: PASS.
- `geostat-api` and `geostat-mobile`: healthy; no restart loop observed.
- Canonical `GET http://127.0.0.1:8083/health`: HTTP 200, `status=UP`; primary, data, archive, object-storage and secondary checks all `UP`.
- Live `geostat-api` image still has no `org.opencontainers.image.revision` label; this recheck confirms health only and does not close release provenance.
- Read-only runtime contract inspection found only `JWT_*` authentication variables among security-related variables; no OIDC provider, tenant-isolation, or `OTEL_*` runtime variables were exposed by the deployed container. This is evidence that the corresponding hardening/observability gates remain open, not evidence that secrets are absent from all external infrastructure.
- The deploy host inventory was inspected read-only: GEOSOTAT production compose contains only API, mobile and MinIO; no GEOSOTAT-owned OIDC/identity provider, distributed quota store, or OpenTelemetry collector service is present. Other Redis/RabbitMQ containers belong to unrelated co-hosted applications and were not reused.
- 2026-09-14 infrastructure isolation remediation: GEOSOTAT-owned `geostat-platform` compose was deployed under `/home/administrator/geostat/backend/infra/geostat-platform` (parent `infra/ops/compose/projects/geostat/docker-compose.prod.yml` restored to MinIO-only). `geostat-redis`, `geostat-keycloak-db`, `geostat-keycloak` and `geostat-otel-collector` are running; Redis and Keycloak DB are healthy, Keycloak is running, and OTel Collector is running. Existing non-GEOSOTAT containers were not restarted or recreated (inventory remained 28).

## Recheck — 2026-09-15

- Read-only Docker inventory confirms the isolated GEOSOTAT services remain healthy/running: `geostat-redis`, `geostat-keycloak-db`, `geostat-keycloak` and `geostat-otel-collector`; unrelated co-hosted projects were not touched.
- `geostat-api` remains healthy and attached only to `geostat-net`; its runtime environment still exposes no `PLATFORM_RATE_LIMIT_REDIS_ENABLED`, `OTEL_METRICS_ENABLED` or OIDC issuer/JWKS binding. Infrastructure availability therefore does not equal application activation, and B-02/B-04/B-05 remain fail-closed.
- No production configuration was changed during this recheck.
- From the `geostat-api` network namespace, the isolated Keycloak realm discovery document is reachable and returns an issuer/JWKS URI. This proves network/provider readiness only; the API container still has no OIDC runtime binding, so token validation and tenant enforcement remain unasserted.
- Read-only `docker-compose --env-file .env.prod -f docker-compose.prod.yml config --quiet` for the isolated infra stack fails closed because `GRAFANA_ADMIN_PASSWORD` is absent. Redis/Keycloak/Collector containers are running from an earlier deployment, but Prometheus/Grafana durable observability deployment is not currently reproducible until that secret reference is supplied.
- Automated `remote-infra-readiness.ps1` recheck: **4/4 PASS** (authenticated Redis ping, Keycloak readiness, OIDC discovery/JWKS reachability from the API network, OTel Collector running); this is infrastructure evidence only and does not assert API feature activation.

## Provenance gap

- Live `geostat-api` image has no `org.opencontainers.image.revision` label.
- Deployment directory has no `.git` metadata.
- Therefore deployed image/source commit equality is not yet proven. The release must rebuild with `IMAGE_REVISION=$(git rev-parse HEAD)` and verify the live OCI label before publication.

### 2026-09-15 API Compose alignment

The GEOSOTAT-only API Compose boundary was aligned from the canonical project
definition. The remote service file now has SHA-256
`fa99de7ec3d8be97f922b4176339f51bb13292cd4c1c5dac630bc2f87c71c652`; the
previous file is preserved as
`/home/administrator/geostat/backend/api/docker-compose.prod.yml.pre-canonical-20260915T090459Z`
with SHA-256
`e3edf23fbe181316fe1aad188192d3de53a3a24ba7c5feb30e4bb5cea5460e8a`.
No container was restarted or rebuilt. The live `geostat-api` image remains
the prior `api-api` image without an OCI revision label, and the remote env
file still does not satisfy the canonical `IMAGE_REVISION`/runtime contract;
production activation remains fail-closed. No sibling project or shared
infrastructure container was modified.

## Reproduction commands (read-only)

```bash
ssh -o BatchMode=yes administrator@192.168.1.199
curl -ksS http://127.0.0.1:8083/health
cd /home/administrator/geostat/backend/api
docker-compose --env-file ../.env.prod -f ops/compose/projects/geostat/docker-compose.prod.yml config --quiet
docker inspect geostat-api --format '{{json .Config.Labels}}'
```

## 2026-09-15 environment-boundary recheck

The host has separate operator environment files. The application file at
`/home/administrator/geostat/backend/.env.prod` contains legacy runtime keys
but no `IMAGE_REVISION`; the isolated infrastructure file at
`/home/administrator/geostat/backend/infra/geostat-platform/.env.prod`
contains Redis/Keycloak keys but no `GRAFANA_ADMIN_PASSWORD`. Key names were
checked without reading values. No credential or fabricated revision was
written: `IMAGE_REVISION` must come from an approved immutable release, and
missing production secrets must be supplied through the operator's secret
management process before the API container can be recreated.

### 2026-09-15 clean-release image build

The repository was committed as clean release candidate
`18d156cfe936b0f4cd59d9e7c9305c47fb269ace`; strict release-gate replay is
PASS with zero working-tree entries. The remote API image was rebuilt from the
uploaded clean-release artifact and its OCI provenance was verified:
`sha256:ff35b3b755793a699d9cbd055e4c899d8652e6574da5eb40f0146a01abc75733`,
`org.opencontainers.image.revision=18d156cfe936b0f4cd59d9e7c9305c47fb269ace`.
The running container still references the prior digest and was not recreated;
signed tag/deploy authority and final runtime configuration remain required
before activation.

### 2026-09-15 final clean-candidate rebuild

After the release documentation commit, the canonical API image was rebuilt
with `IMAGE_REVISION=c782623b601337777d31983b52a2373db237a843`. The resulting
image digest is
`sha256:c9d04c28958a0fb1d7125e380c90905aa029a7ff5d1f6d5b1ef1b69b6f1fce03`,
and inspection confirms the matching OCI revision and source labels. The env
file backup is `/.env.prod.pre-image-revision-20260915T091457Z`. The live
container remains on the previous image until signed release approval and the
final runtime gate authorize recreation.

### 2026-09-15 one-time Grafana secret repair

Because the operator authorized a one-time repair, a random 32-byte
`GRAFANA_ADMIN_PASSWORD` was generated server-side and appended only when the
key was absent. The previous infra env file was preserved at
`/home/administrator/geostat/backend/infra/geostat-platform/.env.prod.pre-grafana-20260915T090939Z`;
the secret value was never printed or recorded. File permissions were set to
`0600`, and the isolated infrastructure Compose validation now passes.
`IMAGE_REVISION` remains intentionally unset until an approved immutable Git
release is selected.
### 2026-09-15 service-root environment split

The shared backend-root `.env.prod` was removed from active use and preserved
as a recoverable `.pre-project-split-*` backup. Missing non-secret/runtime keys
were merged into the owning `backend/api/.env.prod` and `backend/mobile/.env.prod`
files; `IMAGE_REVISION` is owned by `api/.env.prod`. Both service-local Compose
configurations validate successfully, and no frontend/chat project was
modified.
