# Platform activation and verification runbook

This runbook activates the already-created Control/Data/Archive schemas without modifying `kids` legacy tables.

## Required runtime configuration

In the protected production environment, set:

- `DB_DATA_URL`, `DB_DATA_USER`, `DB_DATA_PASS` for `geostat-data`;
- `DB_ARCHIVE_URL`, `DB_ARCHIVE_USER`, `DB_ARCHIVE_PASS` for `geostat-archive`;
- `STORAGE_ENDPOINT`, `STORAGE_ACCESS_KEY`, `STORAGE_SECRET_KEY` for private TLS S3/MinIO;
- `KIDS_SQL_PASSWORD` only in the runtime secret store/environment.

`KIDS_SQL_PASSWORD` is referenced by Core metadata but is never written to SQL Server or committed to this repository.
The production API compose service explicitly passes this variable and all four bucket names; do not place the value in a contract mapping, SQL seed, or source code.

## Preflight

First run the idempotent database bootstrap:

```powershell
.\scripts\platform-bootstrap.ps1 -EnvironmentFile .env.prod
```

It creates Data/Archive databases if absent and applies all platform migrations. Migration files are append-only and checksum-recorded; a schema change is always a new numbered file (for example `005_rejected_artifact_quarantine.sql`), never a rewrite of applied history. API startup runs the same idempotent DDL checks afterwards.

Then run:

```powershell
.\scripts\platform-preflight.ps1 -EnvironmentFile .env.prod
```

Before any release candidate is built, also run the repository-level guards:

```powershell
.\scripts\production-preflight.ps1 -EnvFile .env.prod
.\scripts\audit-checklist-count.ps1
.\scripts\release-gate.ps1
```

The first command validates production configuration without printing secrets; the second prevents audit-status drift; the release gate must pass in a clean checkout (use `-AllowDirty` only for local diagnostics).

It reports only names/statuses, never secret values. It must return `PREFLIGHT=READY` before deployment.

Then, from Linux/WSL/Git Bash:

```bash
scripts/deploy.sh all --prod
```

For source-to-image provenance, the release build must pass the immutable revision explicitly:

```bash
export IMAGE_REVISION="$(git rev-parse HEAD)"
docker-compose --env-file .env.prod -f ops/compose/projects/geostat/docker-compose.prod.yml build --build-arg IMAGE_REVISION="$IMAGE_REVISION" api mobile
docker inspect geostat-api --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}'
```

The inspected value must equal `IMAGE_REVISION`; a missing or different label blocks publication.
The same check can be made fail-closed by the repository gate:

```powershell
.\scripts\release-gate.ps1 -ImageName geostat-api -ExpectedImageRevision $env:IMAGE_REVISION
```

CI must run this form after the image is built and before any publication or rollout.

`deploy.sh` manages MinIO as an infrastructure service: it has no Gradle module or application JAR, but receives a persistent `minio-data` volume and the private Docker network shared with API.

## First Kids pilot

1. A steward reviews `KIDS_PORTAL` mappings and calls `POST /platform/contracts/1/approve`.
2. Execute `POST /platform/sql/ingest?contractSourceId=<id>&sourceConnectionId=1`.
3. Execute validation, snapshot preparation and semantic materialization for every staged source table.
4. Review row counts, rejected rows and lineage; publish only after approval.
5. Verify `kids.dbo` table counts/checksums are unchanged. The source is read-only.

## Rollback and archive

Publication returns an immutable publication snapshot id. `POST /platform/publication/rollback` switches read visibility to a prior snapshot; it does not overwrite rows. Archive handling retains snapshot/artifact lineage for one year.

When validation rejects one or more rows, the service keeps the original ingest object and writes a separate `ingest.artifact.quarantine_uri` copy to the private quarantine bucket. Access is restricted; no rejected source object is silently discarded.

Set an equivalent one-year lifecycle/object-lock rule on the private object-storage buckets. The SQL archive purge worker removes database archive records only after `purge_after`; object bytes must not have a shorter retention than their database lineage.
