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

- [ ] 14.1 Package upload orchestration across package checksum, approved contract revision, Access validation, manifest and relation state — OPEN (ZIP entries now stage and pass complete package/Access validation before any accepted content write; full package→manifest→snapshot binding→reconciliation orchestration remains open. Evidence: `docs/evidence/artifact-package-admission-runtime-2026-09-18.json`.)
- [ ] 14.2 Tenant/site/owner authorization is enforced from authenticated claims through dataset and artifact access — OPEN (current resource authority is global; do not treat as production tenant isolation)
- [ ] 14.3 Provenance chain and scheduled missing/orphan/checksum/retention reconciliation jobs — PARTIAL (094 registered-object audit migration applied/read-back PASS; remote dev object audit correctly raised MISSING for one retained synthetic object; scheduled relation audit wrote `ARTIFACT_RECONCILIATION=FAIL` for snapshots 16 and 31, each with 225 entities, 0 attachments, and 450 missing required slots; API health UP. Evidence: `docs/evidence/artifact-integrity-audit-runtime-2026-09-18.json` and `docs/evidence/artifact-relation-integrity-audit-runtime-2026-09-18.json`. Storage orphan listing, locator audit, snapshot/cache parity, retention and zero-drift jobs remain open.)
- [ ] 14.4 One-click operator experience and progress/retry surface — OPEN
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
- [ ] 16.3 Access upload malware admission — REMOVED with the scanner (ADR-009); `c614f07` superseded
- [x] 16.4 New KIDS_RESOURCE review snapshot 52 (version 73, 225 rows, SEMANTIC_REVIEW) via governed ingest/validate/prepare/materialize — DONE (after AIR-2026-015/016 fixes)
- [x] 16.5 Inventory 532/532 VERIFIED (manifest 4), dry-run 450/0 errors, bind 450 + idempotent replay, ARTIFACT_RECONCILIATION PASS, negatives 401/404/409/403 — DONE
- [x] 16.10 Real release-gate evaluation (7 gates, evidence v1 + facts digest); snapshot 52 → REVIEW_REQUIRED — DONE (`c1143c8`, `docs/evidence/release-gate-evaluation-runtime-2026-09-18.json`)
- [ ] 16.8 Publish snapshot 52 + signed download smoke — BLOCKED: PUBLISH_RESOURCE identity (publish.execute role grant needs explicit user permission); browser path also EXT-4
- [x] 16.9 `ops/scripts/shell/artifact-operator-api.sh` — one-command API calls as the operator client; fresh token per call, never printed — DONE
- [ ] 16.6 clamd acceptance tool — REMOVED (ADR-009)
- 14.1–14.5 unchanged: 14.1 OPEN, 14.2 OPEN (tenant claims model not defined — owner decision), 14.3 PARTIAL, 14.4 OPEN, 14.5 OPEN (needs a bound snapshot).

Evidence: `docs/evidence/access-admission-and-snapshot-provenance-runtime-2026-09-18.json`.
- [x] 16.7 Proper OIDC operator identity: Keycloak confidential client `geostat-artifact-operator` (client_credentials, roles `contract.read`/`contract.write`, tenant `geostat`, audience `geostat-api`) — DONE; replaces temporary bootstrap credentials. Evidence: `docs/evidence/artifact-operator-oidc-client-runtime-2026-09-18.json`

Evidence (16.4–16.5): `docs/evidence/kids-r8-artifact-binding-live-runtime-2026-09-18.json`.
