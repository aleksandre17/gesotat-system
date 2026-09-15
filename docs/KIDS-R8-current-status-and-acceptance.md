# KIDS R8 Current Status and Acceptance

**Authority:** this addendum is the current runtime status authority for KIDS and supersedes older R7 execution notes and handoff files. The architecture and field definitions remain normative in the referenced contract documents; counts and lifecycle state below are read from the deployed system after migration 071 and 072.

## Current release identity

| Item | Value |
|---|---|
| Contract | `KIDS_PORTAL_V1` |
| Site revision | `8 / APPROVED` |
| Ingestion revision | `8 / APPROVED` |
| Access artifact | `platform/apps/geostat/backend/api/kids-portal-v1-canonical-r8-final.accdb` (canonical generated R8 artifact; source samples remain under `samples/`) |
| Current repository fingerprint | 2,834,432 bytes; SHA-256 `D62C58C9633766BC597C1EF67F5984FD7104F922C837224374E5D194F6F0BFF7` (revalidated 2026-09-15 after page-8 projection approval) |
| Production API | `http://192.168.1.199:8083` |
| API health | `UP` — primary, data, archive, object storage and secondary |
| Bootstrap authentication | Disabled |
| Schema migration ledger | `071` governance, `072` gates, `073` reconciliation, `074` validation, `075` published-scope correction |

## Executed data path

The KIDS package was accepted as batch 3. Fifteen dataset snapshots were created, validated, materialized and included in publication snapshot 13. Published semantic rows include 36 goals, 225 resources, 230 resource-subcategory assignments, 178 glossary entries and 880 statistical observations. The serving cache contains 297 metric entries. Archive contains 2,224 records and 2,224 immutable payload pointers.

## Docker build and artifact handoff

The production API image intentionally contains the application runtime only;
the Access artifact is not copied into the image. A deployment must therefore
submit the exact immutable R8 file through the governed
`POST /platform/access/ingest` multipart endpoint with an approved
`contractSourceId`. The API stores the original in private object storage,
checks the contract-declared locator and checksum, and then performs staging,
validation/quarantine and canonical materialization. This explicit handoff
prevents a stale or unapproved Access file from being silently consumed by a
container. Automatic startup ingest and automatic publication remain disabled
until the corresponding authority gates are approved.

## Governance closure

Migration 071 creates the missing ingestion revision 8 when absent, copies the approved site dataset registry into the R8 source registry, makes prefixed R8 bindings executable, deactivates legacy/unprefixed scheduled bindings, supersedes prior ingestion revisions, and aligns the active ingestion contract with the approved site revision. Publication is fail-closed unless the product has an ACTIVE ingestion contract and matching APPROVED site and ingestion revisions.

Migration 072 records independent PASS evidence for: `SCHEMA_VALID`, `KEYS_VALID`, `RELATIONS_VALID`, `CLASSIFIERS_VALID`, `STATISTICAL_SEMANTICS_VALID`, `RAW_LINEAGE_VALID`, and `PUBLICATION_ATOMIC`.

## Acceptance evidence

- Build: `./gradlew :api:test` — successful.
- Package audit default: R8 artifact is now the default `kidsAccessFile`.
- Ingest/materialization: batch 3 and snapshots 22–36 completed.
- Publication: release 3, publication snapshot 13, status `PUBLISHED`.
- Serving: cache materialized for publication snapshot 13.
- Archive: source records and immutable payload pointers present.
- Runtime: production API healthy after deployment.

## Acceptance results and residual operational checks

Completed in the deployment environment: rollback to snapshot 3 and restoration to snapshot 13; deterministic archive upload and byte-count reconciliation (2,224 SQL records and 2,224 MinIO JSON objects); page queries for collection/dataflow/reference pages 8–12 (HTTP 200; root page 7 correctly rejects a data query); independent gate evidence; generic cache/reconciliation jobs; and a repeated full R8 ingest with the same artifact, which returned the same `batchId=4`, `artifactId=4` and load IDs without duplication. A controlled invalid-value fixture produced one rejected row per statistical load and a private quarantine object. A malformed newer-format artifact was rejected before staging (`Unsupported newer version: 114`), proving the fail-closed preflight boundary.

Completed operator acceptance: SQL Server `BACKUP DATABASE ... WITH CHECKSUM` and `RESTORE VERIFYONLY` succeeded; the backup was restored as `geostat-system-r8-dr` and its schema migration ledger was readable. The reconciliation report now scopes observations to published members and returns `published=1`, `members=15`, `published_stats=880`, `cache=297`, `gate_failures=0`. A controlled invalid-value fixture produced `acceptedRows=879`, `rejectedRows=1` for loads 100 and 101, and the artifact received a private `s3://geostat-quarantine/...` pointer with a corresponding MinIO object. No production publication was changed by these tests. UI is outside this package; JWT implementation is covered, while production OIDC/JWKS authority evidence remains a separate gate.

UI remains outside this package scope. JWT guards and policy fixtures are implemented; production OIDC/JWKS binding and approved tenant claims remain external authority gates.
