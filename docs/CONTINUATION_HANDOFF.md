# GeoStat platform — continuation handoff

**Updated:** 2026-09-09  
**Purpose:** this file is the authoritative continuation note for the next session. Read it before changing platform code or databases.

## Current outcome

The unified platform implementation is in the repository and the API test suite passes:

```powershell
.\gradlew.bat :api:test --console=plain
```

The final architecture is three physical planes:

| Plane | Database | Purpose |
|---|---|---|
| Control | `geostat-system` | existing legacy application plus new `platform` metadata/governance schema |
| Active data | `geostat-data` | shared `ingest`, `raw`, `entity`, `statistics`, `geo`, `reference`, `publication`, `serving` schemas |
| Archive | `geostat-archive` | immutable one-year snapshot/artifact lineage |

No legacy Kids table has been altered. Kids is a read-only pilot source, not a new per-site data schema.

## What is already implemented

- SQL Server platform DDL under `core/src/main/resources/db/platform/`.
- Control-plane metadata: product, dataset/version, contracts, contract sources, attributes, relationships, classifications, metrics/dimensions, releases, outbox, leases, validation rules and visualizations.
- Shared physical data families: raw records, entity records/links/classifications, statistical series/observations/dimensions, geo features, reference snapshot, publication snapshots and serving cache.
- Object storage integration through MinIO/S3: ingest, quarantine, archive and export buckets.
- Access and governed SQL Server ingestion adapters, contract selection, batch staging, technical and declared validation, snapshot preparation and semantic materialization.
- Classification mirror, publication outbox/retry, rollback by visibility, archive processing/purge and scheduled import worker.
- Kids pilot seed metadata. Its contract intentionally starts `REVIEW_REQUIRED`; it must be steward-approved before import.
- Production and development MinIO compose service plus health checking of all three SQL planes and storage.
- Deployment/preflight/bootstrap scripts and operations documentation.
- Rejected validation results create an additional restricted quarantine copy. The original artifact remains immutable; `ingest.artifact.quarantine_uri` points to the copy.

## Migration invariant — do not break this

Migration files are checksum-recorded by `PlatformSchemaMigrationRunner`. Never edit a migration already applied to an environment. Add a new numbered file instead.

The most recent additive migration is:

```text
015_resumable_access_ingest.sql
```

Migrations `005` through `015` add quarantine lineage, localized text/locators, metric aliases, non-executable Kids semantic gates, draft SDG aliases, separate Kids logical dataset drafts, portable Control-Plane contract code/revision, the unified Kids site-level contract, provisional statistical metadata, and idempotent resumable Access staging checkpoints. `002_data_plane.sql` was deliberately restored to its original historical definition.

When adding a migration, update both:

1. `api/src/main/java/org/base/api/service/platform/PlatformSchemaMigrationRunner.java`;
2. `scripts/platform-bootstrap.ps1`.

## Runtime status and remaining blocker

The protected `.env.prod` now has the required database, storage and Kids runtime variables. Data and Archive URLs were derived from the established Core MSSQL server with database names `geostat-data` and `geostat-archive`; credentials are not documented here.

The following names remain the required protected runtime configuration set:

```text
DB_DATA_URL
DB_DATA_USER
DB_DATA_PASS
DB_ARCHIVE_URL
DB_ARCHIVE_USER
DB_ARCHIVE_PASS
STORAGE_ENDPOINT
STORAGE_ACCESS_KEY
STORAGE_SECRET_KEY
KIDS_SQL_PASSWORD
```

The template is `.env.example`; do not commit actual credentials. Production compose now explicitly passes `KIDS_SQL_PASSWORD` and all bucket names to the API container.

The real database bootstrap has been successfully executed through migration `005` and verified:

- Control/Data/Archive SQL connectivity: **UP**;
- `ingest.artifact.quarantine_uri`: **present**;
- Kids product seed: **present**;
- Kids contract: **REVIEW_REQUIRED**, with **4** contract sources.

Production MinIO is now deployed as the isolated `backend/infra` compose service on the existing `geostat-net` Docker network. The production API was rebuilt and deployed from `backend/api` only. Its in-container health response confirms `primary`, `dataPlane`, `archivePlane`, `secondary`, and `objectStorage` are all `UP`.

Run preflight again before deployment:

```powershell
.\scripts\platform-preflight.ps1 -EnvironmentFile .env.prod
```

Local WSL remains unavailable, but production MinIO/API deployment was verified through SSH and Docker health checks. No repository or path outside `/home/administrator/geostat/backend/api` and `/home/administrator/geostat/backend/infra` was modified or deleted.

## Exact continuation order

1. Populate the required production variables in the protected `.env.prod`/secret store.
2. Run `scripts/platform-preflight.ps1`; proceed only on `PREFLIGHT=READY`.
3. Run `scripts/platform-bootstrap.ps1 -EnvironmentFile .env.prod`.
4. Deploy from Git Bash/Linux: `scripts/deploy.sh all --prod`.
5. Check API health, including Control/Data/Archive/Storage planes.
6. Steward-review and approve the Kids contract; do **not** manually mark an unreviewed contract active.
7. Run one read-only Kids import, validate, prepare snapshot, materialize semantics, review counts/lineage, then publish.
8. Verify source `kids.dbo` row counts/checksums are unchanged; test rollback and archive retrieval.
9. Only after this pilot passes, migrate further legacy sites one contract at a time.

## Mapping authoring endpoint

`PUT /platform/contracts/{contractId}/sources/{contractSourceId}/mapping` accepts only a JSON `DRAFT` or `READY` semantic mapping for a non-active contract source, validates minimum projection shape, and emits an outbox audit event. It cannot mutate an active contract. This is the controlled path for the final Kids reviewed projection set.

## Important constraints for the next session

- Preserve the dirty worktree. It contains unrelated user changes and many newly added untracked platform files; never reset, checkout, or broadly delete it.
- Do not expose or store user credentials in code, SQL seeds, documentation, or output.
- Do not change legacy import endpoints/tables while introducing the governed path.
- Do not create a database/table per site, chart, or field. New sites use metadata/contracts; new chart definitions bind semantic metrics/dimensions and never duplicate observation values.
- Any real external operation must be preceded by preflight and must remain within the provided infrastructure scope.

## Primary references

- `docs/final-physical-database-architecture.md`
- `docs/kids-semantic-onboarding-spec.md` — mandatory Kids classification and statistical-normalization gate
- `docs/kids-source-semantic-inventory.md` — factual review inventory; required before a Kids mapping becomes `READY`
- `docs/site-serving-api-contract.md` — public site identity, scope, snapshot, entity/resource and metric API contract
- `docs/managed-access-semantic-package-v3.md` — canonical Access semantic package declaration and read-only preview contract
- `docs/access-and-control-plane-responsibility-model.md` — binding authority boundary between Access packages and Control Plane
- `docs/kids-statistical-contract-closure.md` — factual chart JSON profile, SDMX-compatible DSD target and explicit approval gates
- `docs/kids-steward-decision-register.md` — complete finite semantic decision register and enforcement rule
- `docs/international-standards-reference-architecture.md`
- `docs/semantic-mapping-contract.md`
- `docs/platform-operations-runbook.md`
- `docs/platform-decisions.md`

## Last verification

- `2026-09-10`: r6 and earlier packages are preserved but superseded. The current artifact is `samples/kids-portal-v1-canonical-r7.accdb` (package 6.0.0), with 15 datasets, 104 fields, 21 enforced relationships and 43 explicit metric/unit/policy bindings. Raw carrier JSON is not duplicated.
- `2026-09-09`: production API deployed with independent 500-row data-plane checkpoint transactions, idempotent staged rows, immutable-S3-only resume endpoint, and separate control/data plane lookups. Production health: all five health components UP.

- `2026-09-09`: `:api:test` — **BUILD SUCCESSFUL**.
- `2026-09-09`: migration `006_entity_localization_and_locator.sql` applied and verified in the real Data Plane.
- `2026-09-09`: wide-JSON statistical normalizer and Control-plane metric alias migration `007_metric_alias.sql` added; aliases/metrics are intentionally not auto-seeded before steward review.
- `2026-09-09`: migration `008_kids_semantic_gate.sql` applied. All four Kids source mappings are explicitly `DRAFT`; both approval and materialization reject a non-`READY` semantic mapping.
- `2026-09-09`: migration `009_sdg_goal_draft_classification.sql` applied. It contains 17 verified Kids category aliases under draft `UN_SDG_GOAL` version `2015-2030`; resolver requires a published version, so this seed is non-executable until steward publication.
- `2026-09-09`: migration `010_kids_logical_dataset_drafts.sql` applied. Discovery-only `KIDS_CONTENT` is `SUPERSEDED` with inactive sources; `KIDS_GOAL`, `KIDS_FILE_RESOURCE`, and `KIDS_GLOSSARY_ENTRY` are separate, non-executable draft logical datasets/contracts on the shared physical model.
- `2026-09-09`: Semantic Access Package v3 read-only preview is deployed and verified in production. Its strict package/schema gate validates source declarations, allowed types/roles, key order, keyed relation targets, cardinality and JSON projection form; Core code-resolution and chunked import remain the next implementation phase.
- `2026-09-09`: Access v3 was corrected to the stronger Control-Plane-first model and deployed. A package now binds by `contract_code` and `contract_revision`; preview additionally resolves that identity and every dataset/source binding in the Control Plane. Migration `011_contract_stable_identity.sql` was applied by the healthy production API. Contract approval now supports multiple datasets only when all belong to the same product.
- `2026-09-09`: migration `012_kids_site_level_contract.sql` was applied by the healthy production API. `KIDS_PORTAL_V1` is the single `REVIEW_REQUIRED` site-level multi-dataset contract, spanning `goals_titles`, `goals`, `files`, and `glossary`. It is structurally complete but deliberately non-executable until the existing semantic gates (age bands/metrics/units, sub-category crosswalk, `ena`) are steward-resolved.
- `git diff --check` — no whitespace errors; only existing CRLF conversion warnings.
- Runtime preflight — correctly failed only due to the missing configuration named above.
