# Next-session handoff — unified GeoStat platform

## Ground truth at handoff

The broader goal remains active: finish the unified platform (Control/Data/Archive planes, S3, governance, ingestion, classifier/relation/statistical semantics, publication/rollback, and KIDS) without breaking legacy behavior. Do **not** mark the goal complete from this document.

## Completed and verified

### Platform foundation

- SQL Server Control, Data and Archive plane DDL exists in `core/src/main/resources/db/platform/000`–`018`.
- MinIO/S3 original-artifact storage, raw staging, validation/quarantine, snapshot preparation, materialization, publication, rollback and archive/outbox code already exist in the API.
- Production API was repeatedly deployed and last verified healthy:
  `{"status":"UP","db":{"primary":"UP","dataPlane":"UP","archivePlane":"UP","objectStorage":"UP","secondary":"UP"}}`.
- Production deployment must use only `/home/administrator/geostat/backend/api` and `/home/administrator/geostat/backend/infra`. Never edit/delete `/home/administrator/geostat`.

### KIDS canonical Access work (verified r3/r4 generation stages)

- Legacy source facts were audited: `goals_titles=17`, `goals=36`, `files=225`, `glossary=178`; category FKs resolve; `files.sub_category` is source multi-value/N:M; `glossary.ena` remains an explicit unresolved proposal; no invented resource→goal or glossary-translation relation.
- Canonical generator: `ops/scripts/java/KidsPortalCanonicalAccessPackageGenerator.java`.
- It produces explicit typed tables, not a generic key/value model:
  `__cl_*`, `__raw_document`, `kids_goal`, `kids_resource`, `kids_resource_subcategory_assignment`, `kids_glossary_entry`, `kids_statistical_carrier`, `kids_statistical_input`.
- Classifier values/proposals travel with Access (`__cl_scheme/version/item/alias/hierarchy`).
- Chart JSON remains verbatim only in the immutable source artifact. `kids_statistical_carrier` stores its locator/checksum, while parsed cells are stored losslessly in `kids_statistical_input`; they remain DRAFT input, not approved observations.
- Parser discovered 43 parseable JSON arrays, not the earlier manually-maintained 32-item list. Do not restore the 32 hardcoded list.
- Package schema and mocked Control-Plane preview tests exist:
  `KidsPortalCanonicalAccessPackageTest`, `KidsPortalCanonicalPreviewTest`.

### Canonical package orchestration (verified and deployed before r5 local correction)

- `PlatformAccessIngestionService.ingestPackage` creates one batch/immutable artifact and stages all active canonical tables with individual checkpoints.
- Package resume endpoint uses only immutable object storage:
  - `POST /api/v1/platform/access/semantic/preview`
  - `POST /api/v1/platform/access/semantic/ingest`
  - `POST /api/v1/platform/access/semantic/batches/{batchId}/resume`
- `018_package_ingest_idempotency.sql` adds unique `(batch_id,dataset_version_id,source_name)`.
- `PlatformValidationService` was corrected so a multi-table batch becomes `REVIEW_REQUIRED` only after every member load is `VALIDATED`; any rejected member makes the batch `REJECTED`.
- Full `./gradlew.bat :api:test` passed after this orchestration/lifecycle change and it was deployed to production.

### Documentation completed

- `docs/final-access-control-plane-doctrine.md`
- `docs/final-classifier-contract.md`
- `docs/final-kids-canonical-model.md`
- `docs/final-statistical-contract.md`
- `docs/final-import-contract.md`
- `docs/kids-canonical-access-package-contract.md`

## Critical raw-boundary correction — completed in revision 6

The user correctly rejected generator-created JSON inside `__raw_document`.

Correct rule:

- The original uploaded Access artifact in immutable object storage is the raw-byte authority.
- `__raw_document` is **only** a source-row locator (`source_row_key`, `source_table`, `source_primary_key`, `extract_sequence`).
- It must not contain generator-built row JSON, a generated checksum, or a field named `payload_raw`.
- Revision 6 removes `kids_statistical_carrier.payload_raw`; the source artifact is the sole full-payload authority and the carrier stores only a checksum plus lineage.

The completed artifact is `samples/kids-portal-v1-canonical-r7.accdb`, contract revision 7, package 6.0.0. Migrations 022–023 and both migration runners are wired. Revision 7 adds 43 delegated-owner-approved metric/unit/aggregation/quality/confidentiality bindings while preserving revision 6.

## Mandatory next actions, in order

1. To reproduce verification, run:
   ```powershell
   .\gradlew.bat :api:auditKidsCanonicalAccess :api:test --rerun-tasks --console=plain
   ```
   Generation is intentionally separate and refuses overwrite: delete/move only an explicitly verified disposable target before invoking `:api:generateKidsCanonicalAccess`.
2. Inspect r7 with read-only Jackcess and confirm `__raw_document` has four locator columns, `kids_statistical_carrier` has six locator/checksum columns, and all 43 carriers have metric and semantic-binding rows.
3. Build and deploy API only when explicitly authorized. Verify health. Migration 022 must apply successfully; do not alter an already-deployed migration file because checksum-ledger protection will reject drift.
5. Add/finish `AccessClassifierProposalImportService`:
   - 019 currently creates `platform.classifier_proposal` only.
   - The service still must read `__cl_*` after a successful canonical package stage and idempotently write `ITEM`, `ALIAS`, and `HIERARCHY` proposal evidence.
   - It must never auto-approve or overwrite `platform.classification_scheme/version/item/alias`.
   - Integrate it into `SemanticAccessPackageIngestionService` only after successful staging, with batch ID lineage.
6. Add governed proposal review/accept/reject endpoints and service. Acceptance must be explicit, audited and create/revise authoritative classifier records; rejected values remain raw/proposal evidence.
7. Complete authenticated real production E2E only with an authorized `WRITE_RESOURCE` account:
   preview → ingest r7 → validate all 15 loads → prepare snapshots → verify policy gates → publication → rollback. Do not bypass authentication or guess credentials.
8. Audit/finish classifier, relation and SDMX semantic materialization before publication. Revision-6 Control-Plane mappings intentionally remain DRAFT until authoritative semantics are reviewed.
9. Before goal completion, conduct a requirement-by-requirement audit against the active goal. Existing green unit tests and health alone do not prove the full objective.

## Safety and operational notes

- The workspace is intentionally dirty and contains many user/previous changes. Preserve unrelated changes.
- Never use destructive `git reset`, `git checkout`, broad recursive deletion, or overwrite an Access file that the user may have open.
- Use `apply_patch` for source edits.
- Production deploy pattern:
  ```powershell
  .\gradlew.bat :api:bootJar --console=plain
  scp <jar> administrator@192.168.1.199:/home/administrator/geostat/backend/api/app.jar
  ssh administrator@192.168.1.199 "cd /home/administrator/geostat/backend/api && docker-compose -f ops/compose/projects/geostat/docker-compose.prod.yml --env-file ../.env.prod up --build -d api"
  ```
- Health check:
  ```powershell
  ssh administrator@192.168.1.199 "docker inspect -f '{{.State.Health.Status}}' geostat-api; docker exec geostat-api wget -qO- http://localhost:8081/health"
  ```
