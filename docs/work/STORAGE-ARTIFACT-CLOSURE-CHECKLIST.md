# Storage / Artifact Attachment — სრული ხაზის checklist

**Scope:** ფიზიკური ფაილის (artifact) შენახვა Object Storage-ში, მისი მიბმა business
row-ზე, snapshot/publication და governed signed download. Generic capability;
KIDS R8 (`KIDS_RESOURCE`, 719 ფაილი / 532 უნიკალური object) არის conformance case.

**Authority:** `docs/reference/ARTIFACT-ATTACHMENT-CONTRACT.md` §22–§29,
`docs/reference/CANONICAL-OBJECT-STORAGE-FLOW.md`, ADR-008 (`docs/platform-decisions.md`),
`docs/reference/ENGINEERING-QUALITY-DOCTRINE.md`, `AGENTS.md` (change gate 1–10).
**Evidence:** `docs/evidence/kids-r8-resource-artifact-binding-2026-09-18.json` · AIR-2026-010…013.

**სტატუსები:** `DONE` = runtime ან reproducible test ადასტურებს · `READY` = კოდი/migration
deployed dev-ზე, runtime acceptance მოლოდინშია · `OPEN` = გასაკეთებელი · `EXT` = გარე
authority / owner · `N/A` = მიზეზით გამორიცხული.

---

## 0. Baseline (2026-09-18)

| # | ფაქტი | სტატუსი |
|---|---|---|
| 0.1 | MinIO `geostat-minio` UP, 4 private bucket | DONE |
| 0.2 | 532 object ფიზიკურ prefix-ზე `kids/r8/resources/kids-files-r8-sanitized/`; content SHA-256 = key (532/532) | DONE |
| 0.3 | inventory `objectName` ≠ ფიზიკური key | DONE — AIR-2026-010, manifest key-ს ახლიდან ითვლის |
| 0.4 | server-ის `kids-files-r8/files` 713 ფაილი vs inventory 719 | N/A — ლოკალური სარკეა, authority არის bucket + inventory |
| 0.5 | inventory-ს აკლდა MIME / package id / generator / checksum | DONE — manifest v1 |
| 0.6 | presign არ არსებობდა; artifact key UUID-ზე | DONE — port + presign; upload receipt-ის UUID key სხვა grain-ია (ADR-008 §1) |
| 0.7 | `resource_locator` მხოლოდ legacy PATH | DONE — attachment edge; PATH რჩება compatibility-სთვის (AIR-2026-013) |
| 0.8 | meta-schema-ში artifact primitive არ იყო | DONE — 086 |

## 1. Doctrine of Doctrines

- [x] 1.1 Artifact-attachment doctrine — DONE
- [x] 1.2 Blocking gate + upper-layer scope (§22–23) — DONE
- [x] 1.3 ADR-008: identity / attachment / match / gate / distribution / access / retention — DONE
- [x] 1.4 AIR-2026-010…013 — DONE

## 2. Meta-schema / Control Plane — `086_artifact_attachment_meta_schema.sql`

- [x] 2.1 `platform.artifact_policy` (access mode, authority, media types, max bytes, TTL 30–3600, retention, SHA-256) — DONE
- [x] 2.2 `platform.artifact_relation_definition` (dataset version, relation code, role, min/max, ordered, match rule JSON) — DONE
- [x] 2.3 `relationship_type` seed — N/A: `entity_link` არის entity↔entity; attachment ცალკე typed edge-ია, დუბლირებული semantic არ იქმნება
- [x] 2.4 Lifecycle `DRAFT/APPROVED/RETIRED`; approved row immutable (triggers 51020/51021) — DONE
- [x] 2.5 Runner registration + order (`PlatformSchemaMigrationRegistrationTest`); post-apply trigger hardening is additive migration 089 — DONE (runtime 12.2)

## 3. Physical planes — `087_artifact_registry_data_plane.sql`

- [x] 3.1 `ingest.artifact_object` (SHA-256 UK, location UK, verification state) — DONE
- [x] 3.2 `ingest.artifact_manifest` + `ingest.artifact_version` (append-only, trigger 51023) — DONE
- [x] 3.3 `entity.artifact_attachment` (entity, relation, role, language, ordinal, snapshot, source row) — DONE
- [x] 3.4 PK/UK/FK/CHECK (hex checksum, ordinal ≥ 1, slot uniqueness) — DONE
- [x] 3.5 Published-snapshot immutability (trigger 51022) — DONE
- [x] 3.6 Reconciliation view `entity.v_artifact_attachment_reconciliation` — DONE
- [x] 3.7 Archive plane: archived snapshot keeps attached object references (`PlatformArchiveService` → `archive.artifact_reference`) — DONE (schema უცვლელი)

## 4. Object Storage

- [x] 4.1 Buckets + provisioning — DONE
- [x] 4.2 Port `ArtifactObjectStore` + MinIO adapter (`ObjectStorageService`) — DONE
- [x] 4.3 Content-addressed idempotent put, stat, full SHA-256 verify — DONE
- [x] 4.4 Presigned GET (policy TTL, `Content-Disposition` original name, public endpoint, offline signing) — DONE (test)
- [x] 4.5 Key/prefix/path validation (traversal, charset) — DONE (test)
- [ ] 4.6 Encryption at rest / versioning / object lock — EXT (ADR-006, ops)
- [ ] 4.7 `minio-data` backup/restore — EXT (B-06)
- [x] 4.8 Content-based media verification (Tika) — DONE; malware scanning — DEFERRED/REMOVED (ADR-009, DP-001; ClamAV rejected)

## 5. Contract binding — `088_kids_r8_resource_artifact_binding.sql`

- [x] 5.1 Resolver `contract dataset version → approved relation → approved policy` — DONE
- [x] 5.2 KIDS R8 seed: `PRIMARY_FILE`, PRIMARY, 1..1 per language, `KIDS_PUBLIC_STATISTICAL_FILE` r1 — DONE (seed rows only)
- [x] 5.3 Stable key proof: 225/225 unique non-null; 450/450 exact; 450 distinct objects — DONE

## 6. Package / manifest engine

- [x] 6.1 Manifest v1 (`geostat.artifact-manifest.v1`), canonical sort, package checksum — DONE
- [x] 6.2 Deterministic matcher; issues `UNMATCHED_ROW`, `MISSING_SOURCE_VALUE`, `CASE_MISMATCH`, `ORPHAN_ARTIFACT`, policy, verification, `SLOT_CONFLICT` — DONE
- [x] 6.2a Relation `ordered` semantics are enforced for multi-value bindings — DONE (ordered preserves source order; unordered uses canonical path order)
- [x] 6.3 Dry-run preview (default `dryRun=true`); any ERROR → nothing written — DONE
- [x] 6.4 Inventory import (existing KIDS package) + ZIP package upload — DONE (code); runtime run → 12.3
- [ ] 6.5 Generic package upload contract/revision resolution and Access structural validation — PARTIAL (approved contract/dataset resolution, required Access field checks, checksum-bound manifest identity, migrations 091–092, and SQL Server partial-binding rejection PASS; authenticated upload smoke awaits scoped WRITE_RESOURCE token, EXT-5)
- [x] 6.6 Package checksum uses unambiguous canonical field encoding — DONE (length-prefixed fields; delimiter-collision regression test)

## 7. Ingestion / materialization

- [x] 7.1 Idempotent registration (package checksum, SHA-256 content identity, byte-size conflict check) — DONE
- [x] 7.2 Snapshot binding from approved definitions (generic, no site branch) — DONE
- [x] 7.3 Fail-closed binding — DONE (test)
- [x] 7.4 Legacy `resource_locator PATH` untouched — DONE
- [ ] 7.5 Resumable upload sessions, durable part checkpoints, tenant quota admission and retryable completion — PARTIAL (source + 212-test API suite PASS; migration 093 applied and checksum/tables read back on remote dev; anonymous request 401; authenticated storage/recovery acceptance awaits scoped token and scanner, EXT-5/EXT-6)
- [x] 7.6 Concurrent same-checksum manifest registration replay — DONE (remote SQL Server two-session replay on the registry key-range locking statement returned the same manifest id and exactly one manifest/version/object registry set; retained append-only test row has no attachment and is recorded in evidence; AIR-2026-018; authenticated HTTP replay remains under EXT-5)

## 8. Snapshot / publication

- [x] 8.1 Gate `ARTIFACT_RECONCILIATION` (cardinality, verification, policy) — DONE
- [x] 8.2 Result + attachment-set checksum → `publication.release_gate_audit` — DONE
- [x] 8.3 Publication enforces latest PASS and unchanged checksum; datasets without declared artifacts unaffected — DONE
- [x] 8.4 Rollback: attachments immutable once published; bytes never deleted by publication — DONE

## 9. API contract (`/api/v1/platform/artifacts`)

- [x] 9.1 `POST manifests/inventory`, `POST manifests/package`, `POST manifests/{id}/verification` (WRITE_RESOURCE) — DONE
- [x] 9.2 `POST snapshots/{id}/attachments`, `POST snapshots/{id}/reconciliation` (WRITE_RESOURCE) — DONE
- [x] 9.3 `GET entities/{type}/{key}`, `GET .../{relation}/{lang}/{ordinal}/download` (READ_RESOURCE; policy; newest PUBLISHED only) — DONE
- [x] 9.4 RFC 9457: 400/403/404/409/422/503, no storage internals, `Cache-Control: no-store` — DONE (test)
- [x] 9.5 Audit log per issuance without URL/signature — DONE
- [x] 9.6 Anonymous → 401 (dev runtime) — DONE

## 10. Consumer / frontend

- [ ] 10.1 Frontend `/files/...` → governed download — DEFERRED (AIR-2026-009/013)
- [ ] 10.2 Static file retirement — blocked until 12.5 on live snapshot

## 11. Tests (`:api:test` 205 tests, 0 failures/errors, 1 skipped; 88 artifact-related tests)

- [x] 11.1 Unit: keys, manifest, matcher, policy — DONE
- [x] 11.2 Property: manifest/matcher order independence (seeded shuffles) — DONE
- [x] 11.3 Negative: traversal, media type, oversize, case-only, unmatched, missing value, restricted authority, 404/403/409/503 — DONE
- [x] 11.4 Conformance: real KIDS package + 225 rows against seeded 088 rule — DONE
- [x] 11.5 Migration registration/order — DONE
- [x] 11.6 Checksum includes API-visible identity/metadata and remains input-order independent — DONE (`ArtifactReconcilerTest`)
- [x] 11.7 Migration 089 lifecycle transition integration on SQL Server — DONE (policy and relation immutability rejection; transaction rollback; AIR-2026-014)
- [x] 11.8 POSIX/UNC and Windows drive paths are rejected before package manifest creation — DONE (`ArtifactManifestGeneratorTest`)
- [x] 11.9 Content-derived media verification rejects extension spoofing on upload/import; CSV and generated XLS/XLSX workbooks are tested, including DOCX→XLSX mismatch — DONE (Tika plus OOXML/OLE workbook structure checks)

## 12. Delivery / runtime evidence (remote dev 192.168.1.199)

- [x] 12.1 Deploy current source via remote sync (`geostat.ps1 api dev bootstrap api --no-build`); health all UP; 7 routes — DONE (2026-09-18; remote dev only)
- [x] 12.8 Scheduled database workers wait for schema migration completion; JWT token repository uses required constructor injection — full API suite PASS (217 tests, 0 failures/errors, 1 skipped); dev restarted 2026-09-18T10:46:59Z and remained healthy, with no schema/JWT/scheduler errors in the subsequent log window. Evidence: `docs/evidence/platform-schema-readiness-runtime-2026-09-18.json`.
- [x] 12.2 Ledger rows 086–089 read back — DONE (SQL Server ledger checksums read back; 089 applied)
- [ ] 12.3 `POST manifests/inventory` (`packageCode=KIDS_R8_RESOURCES`, `inventoryKey=kids/r8/resources/kids-files-r8-sanitized/inventory.json`, `objectPrefix=kids/r8/resources/kids-files-r8-sanitized/`) → 532 VERIFIED — BLOCKED (inventory import recomputes SHA/MIME and requires malware scan; scanner host is unset and port 3310 is unreachable. Temporary WRITE_RESOURCE bootstrap access was already proven and revoked; scanner evidence is in `platform-schema-readiness-runtime-2026-09-18.json`.)
- [ ] 12.4 Bind (dry-run → write) on the KIDS_RESOURCE REVIEW snapshot + reconciliation PASS — BLOCKED (version 73 currently has only snapshots 16 and 31, both `PUBLISHED`; the binding API correctly rejects them. A governed ingestion/review cycle must create a bindable snapshot first. Evidence: `docs/evidence/artifact-binding-target-preflight-runtime-2026-09-18.json`.)
- [ ] 12.5 Signed download smoke (200, checksum equal) + 403/404/409 — BLOCKED until a new bindable review snapshot, verified manifest, and 450 attachment edges exist; browser TLS/DNS delivery remains under EXT-4.
- [x] 12.6 Evidence JSON + reference docs + ADR + AIR — DONE
- [x] 12.7 Upload bound is configurable across Spring multipart and artifact expansion budgets — DONE (file/request limits are separate configurable settings; startup validation enforces file-size agreement and multipart framing capacity; remote dev booted healthy; `ArtifactConfigurationTest` PASS)

## 13. Remaining generic capability work

- [x] 14.1 Package upload orchestration across package checksum, approved contract revision, dataset validation, manifest and relation state — DONE (runtime: `docs/evidence/kids-r8-package-end-to-end-runtime-2026-09-19.json`). `PackageRunService` sequences pluggable `PackageRunStage` beans `INGEST_DATASET → VALIDATE_LOAD → PREPARE_SNAPSHOT → MATERIALIZE → BIND_ATTACHMENTS → RECONCILE → EVALUATE_RELEASE_GATES`; one run per manifest (idempotent), progress persisted after every stage (migration 101), a run ends at steward review and never publishes. Dataset formats are `PackageDatasetCarrier`/`PackageDatasetIngestor` beans — the engine no longer names Access.
- [ ] 14.2 Tenant/site/owner authorization is enforced from authenticated claims through dataset and artifact access — OPEN (current resource authority is global; do not treat as production tenant isolation)
- [ ] 14.3 Provenance chain and scheduled missing/orphan/checksum/retention reconciliation jobs — PARTIAL (094 registered-object audit migration applied/read-back PASS; remote dev object audit correctly raised MISSING for one retained synthetic object; scheduled relation audit wrote `ARTIFACT_RECONCILIATION=FAIL` for snapshots 16 and 31, each with 225 entities, 0 attachments, and 450 missing required slots; API health UP. Evidence: `docs/evidence/artifact-integrity-audit-runtime-2026-09-18.json` and `docs/evidence/artifact-relation-integrity-audit-runtime-2026-09-18.json`. Storage orphan sweep is READY in source (migration 100, `ArtifactStorageSweepService`: resumable per-scope cursor, lease, grace period, evidence only — never deletes; scopes derived from the registry, `ArtifactStorageSweepServiceTest`); runtime run pending (17.4). Locator audit, snapshot/cache parity, retention and zero-drift jobs remain open.)
- [x] 14.4 One-click operator experience and progress/retry surface — DONE for the API surface (runtime evidence as 14.1): `POST /platform/artifacts/package-runs` (202, idempotent), `GET …/{runId}` (pipeline + append-only stage history), `POST …/{runId}/retry`; `RETRYABLE` (infrastructure) resumes automatically, `BLOCKED` (package/contract) waits for an operator. UI remains open.
- [ ] 14.5 Rollback, replay, load/performance and recovery evidence — OPEN

## 15. Security verification follow-up

- [x] 15.1 Content-derived media type must agree with the declared path type before package storage/manifest registration — DONE (`ArtifactContentTypeVerifier`; content-only Tika detection; PDF spoof and inventory negative tests)
- [ ] 15.2 ClamAV provider / malware admission / quarantine — REMOVED (ADR-009, DP-001)
- [x] 15.3 Migration 090 quarantine evidence persistence — DONE (applied in Data Plane; transactional SQL Server fixture PASS)
- [ ] 15.4 Scanner protocol vectors — REMOVED with the scanner (ADR-009)

## 14. External gates

- EXT-1 Anonymous access for `PUBLIC_WHEN_PUBLISHED` (security matcher) — owner decision; boundary stays authenticated
- EXT-2 Retention/DR, encryption, object lock (B-06, ADR-006)
- EXT-3 Production release provenance (B-01)
- EXT-4 `files.geostat.internal`: TLS SAN + DNS + edge redeploy + `STORAGE_PUBLIC_ENDPOINT` (edge config in repo, `nginx -t` PASS) — AIR-2026-012
- EXT-5 Token with `contract.write`/`contract.read` roles for 12.3–12.5
- ~~EXT-6 clamd endpoint~~ — CLOSED: ClamAV rejected, scanning deferred (ADR-009)

## 16. Session 2026-09-18 (continuation) — status

- [x] 16.1 Snapshot creation path identified (read-only): `POST /platform/access/semantic/ingest` → `POST /platform/ingestion/validate/{loadId}` → `POST /platform/ingestion/prepare-snapshot` (`REVIEW_REQUIRED`) → `POST /platform/ingestion/materialize` (`SEMANTIC_REVIEW`); no direct SQL — DONE
- [x] 16.2 Snapshot provenance hardening (Codex WIP reviewed, tests corrected/extended) — DONE (`6595587`, 231 PASS)
- [x] 16.3 Access upload malware admission — REMOVED with the scanner (ADR-009); malware scanning deferred under DP-001; `c614f07` superseded
- [x] 16.4 New KIDS_RESOURCE review snapshot 52 (version 73, 225 rows, SEMANTIC_REVIEW) via governed ingest/validate/prepare/materialize — DONE (after AIR-2026-015/016 fixes)
- [x] 16.5 Inventory 532/532 VERIFIED (manifest 4), dry-run 450/0 errors, bind 450 + idempotent replay, ARTIFACT_RECONCILIATION PASS, negatives 401/404/409/403 — DONE
- [x] 16.10 Real release-gate evaluation (7 gates, evidence v1 + facts digest); snapshot 52 → REVIEW_REQUIRED — DONE (`c1143c8`, `docs/evidence/release-gate-evaluation-runtime-2026-09-18.json`)
- [x] 16.8 Publish snapshot 52 + signed download smoke — DONE on dev; `releaseId=4`, publication snapshot 14, downloaded `KIDS_RESOURCE/128` SHA-256 matched metadata. Browser hostname remains EXT-4.
- [x] 16.9 `ops/scripts/shell/artifact-operator-api.sh` — one-command API calls as the operator client; fresh token per call, never printed — DONE
- [x] 16.6 clamd acceptance tool — REMOVED (ADR-009); scanning deferred under DP-001
- 14.1–14.5 unchanged: 14.1 OPEN, 14.2 OPEN (tenant claims model not defined — owner decision), 14.3 PARTIAL, 14.4 OPEN, 14.5 OPEN (needs a bound snapshot).

Evidence: `docs/evidence/access-admission-and-snapshot-provenance-runtime-2026-09-18.json`.
- [x] 16.7 Proper OIDC operator identity: Keycloak confidential client `geostat-artifact-operator` (client_credentials, roles `contract.read`/`contract.write`, tenant `geostat`, audience `geostat-api`) — DONE; replaces temporary bootstrap credentials. Evidence: `docs/evidence/artifact-operator-oidc-client-runtime-2026-09-18.json`
- [x] 16.11 Approved site dataset → physical table-definition direct FK; package resolver validates against the versioned physical Access table (`KIDS_RESOURCE` → `__ent_kids_resource`) — DONE (`097`/`098`, targeted tests PASS).
- [x] 16.12 Deterministic single-entry KIDS R8 ZIP created and contract-bound package admission verified on dev; replay is idempotent (`manifestId=5`, 1/1 VERIFIED) — DONE. Evidence: `docs/evidence/kids-r8-contract-bound-package-admission-runtime-2026-09-18.json`.

- [x] 16.13 Contract-driven package descriptor endpoint, admission-time relation preview (422 fail-closed, before any write), validate-only preview endpoint, generic assembler (`artifact-package-assemble.sh` / `:api:assembleArtifactPackage`) — DONE; `:api:test` 243 PASS.
- [x] 16.14 Full KIDS R8 package (Access + 450 row-referenced files, 451 entries, deterministic) assembled from the dev descriptor → server preview 450/0/0 → admitted manifest 6 (451/451 VERIFIED, replay idempotent) → published snapshot 52 slot-equivalence 450/450, 0 mismatches — DONE. Evidence: `docs/evidence/kids-r8-full-package-admission-runtime-2026-09-18.json`.
- [x] 16.15 Servlet error path: unmapped route recursed to `/error/error/404` → `StackOverflowError` (observed on dev) — DONE in source (root cause: shared `ErrorController` returned view names in a host without a template engine → relative forward → `GlobalExceptionHandler.redirectWeb` forwarded to `/error` again, unbounded). Error endpoint is now terminal: template where the host ships one, RFC 9457 `no-store` body otherwise, no exception/URI exposure; `redirectWeb` never forwards from a non-REQUEST dispatch. `ErrorPathTerminationTest` (6) PASS; `:core:test` 25 PASS. Dev runtime: unmapped route → 404 problem+json (evidence 2026-09-19).

Evidence (16.4–16.5): `docs/evidence/kids-r8-artifact-binding-live-runtime-2026-09-18.json`.

## 17. Session 2026-09-19 — storage line structure (source + tests; `:api:test` 273 PASS, 0 failures, 1 skipped)

- [x] 17.1 Object Storage boundary split by responsibility (AIR-2026-028): narrow ports `ArtifactObjectStore`, `ArtifactUploadStagingStore`, `ArtifactObjectInventory`; S3 adapters in `service/storage/s3` bound by `S3StorageConfiguration` (`S3StorageProperties`, `S3Clients`); `ObjectStorageService` keeps only ingestion-side operations. The port no longer has "not supported" default methods; the staging namespace is configuration (`STORAGE_STAGING_PREFIX`), not a literal.
- [x] 17.2 Dataset carrier strategy (AIR-2026-029): `extension.equals("accdb")` removed from `ArtifactPackageService`; `PackageDatasetCarriers` resolves the format bean.
- [x] 17.3 Storage orphan sweep and package run pipeline — see 14.3 / 14.1 / 14.4.
- [ ] 17.4 Runtime acceptance on dev — PARTIAL (2026-09-19, `docs/evidence/storage-line-runtime-acceptance-2026-09-19.json`): redeployed, health ALL UP, migrations 099–101 applied at startup; unmapped route → `404 application/problem+json` `no-store` (16.15 DONE at runtime); approval preview on `KIDS_PORTAL_V1` r8 → 422 with `CHECKSUM_INTEGRITY` PASS and nothing written; package run 1 (manifest 6): 202, idempotent replay, `INGEST_DATASET → VALIDATE_LOAD (replayed) → PREPARE_SNAPSHOT (52)` COMPLETED, `MATERIALIZE` BLOCKED `SNAPSHOT_NOT_MATERIALIZABLE` (`PUBLISHED`) — identical content resolves to its published snapshot and is never rewritten; operator retry resumed at the blocked stage only. Storage sweep completed a full cycle on dev (scope 2: 451 objects listed, 0 orphans, nothing deleted). OPEN: a `COMPLETED` run needs a package with new dataset content; SQL trigger fixture (needs DB credentials).
- [x] 17.7 Platform defects exposed by the acceptance and fixed: unexpected exceptions were not logged (AIR-2026-034); `JacksonConfig` captured every untyped JSON value and recursed (AIR-2026-035, `JacksonConfigTest`). `:api:test` 276 PASS.
- [x] 17.5 Upload session line restructured and unit-tested (AIR-2026-032): `UploadSessionStatus` (state rules in one enum instead of string comparisons), `UploadSession`, `UploadSessionRepository` (all SQL, statements unchanged), `ArtifactUploadSessionService` (orchestration only, injected `Clock`). `ArtifactUploadSessionServiceTest` (11): idempotent start, quota fail-closed, range/part integrity, replay, digest rejection, lost-session cleanup, assemble→commit→quota release→checkpoint cleanup, retryable vs rejected completion, owner isolation, expiry, quota-accounting invariant, schema-ready gate. `:api:test` 273 PASS, 0 failures, 1 skipped.
- [x] 17.6 Last format literal removed from the engine (AIR-2026-033): `.accdb` media type moved to data (`artifact-media-types.properties`, mapping value unchanged so manifest checksums are stable); five duplicate SHA-256/hex helpers replaced by `Sha256`.
- [ ] 17.8 Documentation conformance gaps of the generic package model (found 2026-09-19 comparing `samples/kids-r8-resource-package.zip` with ARTIFACT-ATTACHMENT-CONTRACT): (a) only the `SOURCE_PATH` match rule exists; the explicit relation-table rule of §4.2/§4.3 (`__raw_document`, `__rel_entity_artifact`: N:M, role, ordinal, primary) is not implemented, so §27 (1000 rows / 1500 files) is unproven; (b) manifest v1 does not record source row keys or row↔file candidates required by §25 (they exist in the preview report and in attachment edges); (c) §2 shows an illustrative `files/` folder while the authority for package paths is the approved relation rule (`packageRoot`). The KIDS ZIP itself conforms to its approved contract (§24.4 derived mapping). — OPEN
- [x] 17.9 End-to-end on dev with a complete package (Access build 8.0.1 + 450 row-bound files + shipped `manifest.json`): preview 450/0/0 → admitted manifest 7 (451/451 VERIFIED) → package run 2 `COMPLETED` in one attempt (ingest batch 9 → load 142, 225 rows → snapshot 53 → materialized 225 → bound 450 → `ARTIFACT_RECONCILIATION` PASS → gates releasable, `REVIEW_REQUIRED`). Attachment checksum `0737e9c2…` equals published snapshot 52. Snapshot 53 is not published (steward decision). Evidence: `docs/evidence/kids-r8-package-end-to-end-runtime-2026-09-19.json`.
- [x] 17.10 Shipped manifest (§2, §25; closes 17.8b for the package side): `manifest.json` (`geostat.artifact-package-manifest.v1`: file claims + row↔file edge claims) is written by the assembler and verified at admission against the server-derived manifest and relation plan; any difference blocks before a write; the manifest is not package content. `ArtifactPackageRelationPreviewTest` (exact / misstated edge / ghost file / wrong schema). `:api:test` 278 PASS. 17.8a (explicit relation-table match rule) stays OPEN.
- [x] 17.11 Accepted manifest document (§25; closes 17.8b): migration 102 `ingest.artifact_manifest_document` (immutable pointer, trigger 51102) + `ArtifactManifestDocuments` (canonical JSON, content-addressed under `platform.artifacts.manifest-document-prefix`, outside the content pool so the storage sweep does not see it as an orphan; recorded once per manifest, checksum-verified on read); `GET /platform/artifacts/manifests/{id}/document`. Runtime on dev: manifest 7 → 451 file claims, 450 edges. `ArtifactManifestDocumentsTest` (3); `:api:test` 281 PASS.
- [ ] 17.12 Explicit relation-table match rule (17.8a, contract §4.2/§4.3/§27) — OPEN, design decision recorded in AIR-2026-037: the edges live in a second dataset of the package, so preview and snapshot binding must both read it; that requires a package run over several datasets of one batch, not a new rule class alone.
- [x] 17.13 Migration chain reproducibility (AIR-2026-027/038): strict script execution, corrected scripts, `054`/`055` registered with an adoption probe, repair `103`. Empty-database replay 98/98, r8 `APPROVED`; dev redeployed clean. Evidence: `docs/evidence/migration-chain-reproducibility-2026-09-19.json`. `:api:test` 283 PASS.
- [x] 17.14 Signed download regression after the storage refactor: `GET …/KIDS_RESOURCE/128/PRIMARY_FILE/ka/1/download` → signed URL → 200, 12879 bytes, SHA-256 equal to metadata (dev network).
