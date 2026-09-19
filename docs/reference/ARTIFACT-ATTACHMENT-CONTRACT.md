# Artifact Attachment Contract

**სტატუსი:** CANONICAL DESIGN / IMPLEMENTATION BASELINE  
**Scope:** ნებისმიერი site/provider/data-product; KIDS არის მხოლოდ conformance case.  
**მიზანი:** ფიზიკური ფაილების, source row-ების, canonical entity-ების,
statistical observations-ებისა და API distribution-ის ერთიანი, აგნოსტიკური,
დამტკიცებადი და მარტივად გამოსაყენებელი მოდელი.

ეს კონტრაქტი ეფუძნება immutable object storage-ს, contract-first metadata-ს,
W3C PROV provenance-ს, W3C DCAT 3 distribution semantics-ს და Frictionless
Data Package manifest-ის პრინციპებს.

## 1. ძირითადი პრინციპი

მომხმარებელი ტვირთავს ერთ პაკეტს; პლატფორმა ავტომატურად ქმნის identity-ს,
checksum-ს, artifact registry-ს, row↔artifact relations-ს, snapshot-სა და
governed download-ს. მომხმარებელი არ წერს SQL-ს, bucket path-ს ან ტექნიკურ ID-ს.

```text
package
  → manifest
  → contract/revision validation
  → immutable object upload
  → artifact identity
  → row↔artifact relation
  → canonical locator
  → snapshot
  → publication
  → signed API download
```

## 2. მომხმარებლის პაკეტი

```text
dataset-package.zip
  ├── data.accdb | data.xlsx | data.csv
  ├── files/
  │   ├── report-001.xlsx
  │   ├── report-001.pdf
  │   └── ...
  └── manifest.json       # optional; system can generate it
```

თუ manifest არ არსებობს, სისტემა იყენებს contract-declared key rules-ს და ქმნის
manifest-ს preview ეტაპზე. ნებისმიერი ambiguous match გადადის quarantine-ში.

## 3. Identity model

### 3.1 Row identity

ყოველ business row-ს MUST ჰქონდეს stable, source-owned key:

```text
entity_source_key = source-system | dataset | natural-key
```

Surrogate database ID არის შიდა ტექნიკური მნიშვნელობა და არასდროს არის source
identity-ის შემცვლელი.

### 3.2 Artifact identity

ერთი ფიზიკური ფაილი არის ერთი artifact entity. მისი content identity არის:

```text
artifact_content_id = SHA-256(bytes)
```

Logical identity (მაგ. `annual-report-2026`) და content identity ერთმანეთისგან
გამიჯნულია. იგივე logical file-ის შეცვლილი content ქმნის ახალ immutable version-ს.

### 3.3 Relation identity

ერთი row↔file კავშირი არის ერთი relation edge:

```text
relation_key = entity_source_key | artifact_source_key | role | ordinal
```

Relation count არის რეალური edges-ის რაოდენობა და არა row-count × file-count.

## 4. Access package-ის მინიმალური სტრუქტურა

### 4.1 Entity dataset

```text
__ent_<dataset>
```

```json
{
  "source_entity_id": "row|1001",
  "source_row_key": "source|1001",
  "title": "Record 1001",
  "operation": "UPSERT"
}
```

### 4.2 Artifact envelope

```text
__raw_document
```

მომხმარებელი აწვდის მხოლოდ source identity-სა და original filename-ს. სისტემა
ითვლის/ავსებს checksum-ს, MIME-ს, ზომას, URI-ს, artifact ID-ს, parser state-სა
და validation state-ს.

```json
{
  "source_row_key": "file|1500",
  "original_filename": "report-001.xlsx",
  "source_primary_key": "report-001"
}
```

### 4.3 Generic attachment relation

```text
__rel_entity_artifact
```

```json
{
  "relation_key": "row|1001|file|1500|SUPPORTING_DOCUMENT|1",
  "entity_source_row_key": "row|1001",
  "artifact_source_row_key": "file|1500",
  "role_code": "SUPPORTING_DOCUMENT",
  "is_primary": true,
  "ordinal": 1,
  "operation": "UPSERT"
}
```

ეს არის generic relation და არა KIDS-specific workaround. მისი contract-ში
უნდა იყოს გამოცხადებული cardinality, allowed roles, ordering და lifecycle.

## 5. Cardinality და attachment semantics

დაშვებულია ყველა კომბინაცია:

```text
ერთი row → ერთი file
ერთი row → ბევრი file
ბევრი row → ერთი file
ბევრი row → ბევრი file
```

მაგალითი:

```json
{
  "rowKey": "row|1001",
  "attachments": [
    {"artifactKey": "file|1500", "role": "PRIMARY", "ordinal": 1},
    {"artifactKey": "file|1501", "role": "SUPPORTING_DOCUMENT", "ordinal": 2}
  ]
}
```

1000 row-ისა და 1500 file-ის შემთხვევაში იქმნება 1500 artifact identity და
მხოლოდ რეალური კავშირების რაოდენობის relation rows. ერთი artifact relation-ში
არ კოპირდება.

## 6. Matching policy

კავშირის დადგენის პრიორიტეტია:

```text
1. explicit manifest rowKeys
2. Access-ის declared source key
3. contract-registered filename/key pattern
4. registered locator rule
5. ambiguous → QUARANTINED
```

filename-only match არასდროს არის საბოლოო authority, თუ ერთზე მეტი candidate
არსებობს. Missing, orphan, duplicate და ambiguous შემთხვევები ცალკე issue-ებად
იქმნება.

## 7. Upload protocol

```text
RECEIVE
  → package checksum
  → contract/revision resolution
  → Access structural validation
  → file type/size validation (malware scan deferred: ADR-009)
  → SHA-256 streaming calculation
  → multipart/resumable object upload
  → artifact manifest persistence
  → row↔artifact resolution
  → FK/duplicate/orphan reconciliation
  → canonical materialization
  → immutable snapshot
  → quality/privacy/semantic gates
  → publication
```

ყველა ეტაპი არის idempotent. Retry იგივე package checksum-ით არ ქმნის duplicate
artifact-ს ან relation-ს.

Upload და staged-inventory import path-ის MIME მნიშვნელობას მხოლოდ გაფართოებიდან
არ ენდობა: content-only detector ბაიტებს ამოწმებს, caller filename/MIME hint-ის
გარეშე, და mismatch-ზე manifest registration-მდე fail-closed ქცევა აქვს. CSV-ის
შიგთავსი detector-მა შეიძლება ზოგად `text/plain`-ად ამოიცნოს; `text/csv` policy-ს
ეს შეესაბამება მხოლოდ ტექსტური payload-ის შემთხვევაში.

Malware scanning ამოღებულია და გადადებულია (ADR-009, `docs/work/DEFERRED-PLANS.md` DP-001);
ClamAV უარყოფილია. MIME შემოწმება malware სკანირება არ არის.

## 8. Object Storage model

Object Storage არის bytes-ის sole authority:

```text
geostat-ingest/
  <site>/<contract>/<revision>/incoming/<sha256>.<ext>

geostat-quarantine/
  <site>/<contract>/<revision>/<issue-id>/<sha256>.<ext>

geostat-archive/
  <site>/<contract>/<revision>/<snapshot-id>/<sha256>.<ext>

geostat-export/
  <operation-id>/<sha256>.<ext>
```

Object key არ ეფუძნება დაუმუშავებელ user filename-ს. Original filename ინახება
metadata-ში; key არის checksum-addressed და path-safe.

## 9. Database canonical model

```text
ingest.package
  package_id, contract_revision_id, package_checksum, status

ingest.artifact
  artifact_id, package_id, object_uri, checksum, byte_size, mime_type, status

raw.source_record
  source_record_id, artifact_id, source_row_number, source_key, payload_hash

entity.entity_record
  entity_id, dataset_snapshot_id, record_type, external_key, payload_json

entity.resource_locator
  locator_id, entity_id, artifact_id/object_uri, checksum, mime_type,
  access_policy_code, is_primary, valid_from, valid_to

entity.entity_link
  link_id, from_entity_id, to_artifact_id/to_entity_id, relation_code,
  role_code, ordinal, validity

publication.dataset_snapshot
  dataset_snapshot_id, dataset_version_id, status, checksum, row_count
```

`entity.resource_locator` გამოიყენება მაშინ, როდესაც attachment entity-ის
resource locator-ია. მრავალმხრივი ან domain-specific კავშირი გამოიყენებს generic
`entity.entity_link`/contract-declared relation dataset-ს.

## 10. Lifecycle

```text
RECEIVED
  → VALIDATING
  → ACCEPTED
  → QUARANTINED | REJECTED
  → SNAPSHOT_CANDIDATE
  → PUBLISHED
  → SUPERSEDED
  → RETIRED
```

Published artifact არ იშლება ჩვეულებრივი ოპერაციით. Supersede ქმნის ახალ
immutable version-ს; rollback აბრუნებს წინა snapshot pointer-ს.

## 11. Integrity და quality პასუხები

სისტემა ავტომატურად პასუხობს:

- checksum ემთხვევა თუ არა object-ს;
- MIME/type სწორია თუ არა extension-თან;
- ზომა ლიმიტშია თუ არა;
- malware/zip-bomb/path-traversal რისკი არსებობს თუ არა;
- duplicate content არის თუ არა;
- ყველა relation target არსებობს თუ არა;
- orphan artifact/row არსებობს თუ არა;
- row-count, file-count და checksum reconciliation ემთხვევა თუ არა;
- artifact snapshot-ის წევრია თუ არა;
- published object ხელმისაწვდომია თუ არა.

## 12. Partial failure policy

თუ 1500 ფაილიდან რომელიმე ვერ აიტვირთა ან relation გაურკვეველია:

```text
package status = QUARANTINED | FAILED
publication = BLOCKED
valid partial rows = not publicly served
issue = explicit, reproducible and retryable
```

Partial publication დასაშვებია მხოლოდ მაშინ, თუ contract-ში წინასწარ არის
გამოცხადებული `partialPublicationPolicy` და steward-ს აქვს დამტკიცებული rule.

## 13. Snapshot binding

საბოლოო identity chain არის:

```text
contract revision
  → dataset version
  → immutable dataset snapshot
  → artifact membership
  → canonical entity/statistical rows
  → locator/relation
  → API projection
```

ერთი snapshot-ის შიგნით row და artifact version არ იცვლება. ახალი ფაილი ან
შეცვლილი checksum ქმნის ახალ snapshot candidate-ს.

## 14. Security და privacy

- deny-by-default;
- tenant/site/product/dataset/field/file-level authorization;
- artifact metadata და bytes-ის განცალკევებული policy;
- short-lived signed URL;
- URL-ში secret ან permanent credential არ იდება;
- private/quarantine bucket public access-ს არ უშვებს;
- download/view audit event სავალდებულოა;
- sensitive artifact-ს აქვს confidentiality class და redaction policy;
- cross-tenant relation fail-closed-ია;
- object storage credentials API response-ში არასდროს გადის.

## 15. API contract

Package ZIP admission uses `POST /api/v1/platform/artifacts/manifests/package`
with required `packageCode`, `contractCode`, `revision`, and `datasetCode`
multipart parameters. The Control Plane must resolve that exact approved
revision/dataset. The archive must contain one `.accdb` whose contract-declared
Access table and required fields are present. The accepted manifest stores
`contractCode`, `contractRevision`, and `datasetVersionId`; those values are
included in its package checksum. ZIP entries are first expanded into a bounded
temporary staging directory and checked for path safety, type, size,
and contract structure; only after the complete archive passes admission are
content objects written and the manifest registered. This prevents a late
structural rejection, such as a missing required Access database, from leaving
accepted content objects behind. Storage failures during the subsequent object
writes can still leave content-addressed orphans, which require the separate
orphan reconciliation control. Inventory import remains a separate governed
path for previously staged content. Package admission still does not bind the
manifest to a snapshot or run relation reconciliation; those remain separate
workflow states and the package orchestration gate remains open. Source test and
remote development boot evidence is recorded in
[`artifact-package-admission-runtime-2026-09-18.json`](../evidence/artifact-package-admission-runtime-2026-09-18.json).
The KIDS R8 remote admission, physical-table binding, and idempotent replay
evidence is recorded in
[`kids-r8-contract-bound-package-admission-runtime-2026-09-18.json`](../evidence/kids-r8-contract-bound-package-admission-runtime-2026-09-18.json).

The physical Access table is resolved through the approved dataset's direct
`contract_table_definition_id` binding. `site_contract_dataset.access_table_name`
may retain a legacy/logical name; it is not a substitute for the versioned
physical definition used to validate package bytes. Missing or ambiguous
approved bindings fail closed before any object or manifest is written.

Contract-bound admission also evaluates every approved relation of the dataset
over the package's own rows before any object is written. Row identity is the
contract's ordered key fields (`site_contract_field.key_role`); the evaluation is
the same `ArtifactMatcher` used for snapshot binding. Any `ERROR` rejects the
package with `422 artifact-relation-preview-blocked` and the exact issues; the
accepted receipt carries the per-relation preview (edges, orphans, warnings).
Upload sessions complete through the same path.

```text
GET  /api/v1/platform/artifacts/contracts/{contractCode}/revisions/{revision}/datasets/{datasetCode}/package-descriptor
     READ_RESOURCE; approved dataset structure (physical table, required and key fields)
     plus approved relation declarations (match rule, cardinality, policy)
POST /api/v1/platform/artifacts/manifests/package/preview
     WRITE_RESOURCE; same parameters as admission; validate-only, nothing stored (§26)
```

Package producers read only the descriptor. The generic assembler
(`ops/scripts/shell/artifact-package-assemble.sh` → Gradle
`:api:assembleArtifactPackage`) packages the Access dataset plus exactly the
files its rows reference, refuses on any preview `ERROR`, builds deterministically
and writes assembly evidence. It contains no contract-specific value.

Large uploads can use the durable session API, also guarded by `WRITE_RESOURCE`
and an OIDC subject plus the configured tenant claim (`tenant_id` by default):

```text
POST   /api/v1/platform/artifacts/upload-sessions
       Idempotency-Key: <client-generated key>
       {packageCode, contractCode, revision, datasetCode, expectedBytes}
POST   /api/v1/platform/artifacts/upload-sessions/{id}/parts/{partNumber}
       Content-Range: bytes <start>-<end>/<expectedBytes>
       X-Checksum-SHA256: <lowercase sha256>
       Content-Type: application/octet-stream
GET    /api/v1/platform/artifacts/upload-sessions/{id}
POST   /api/v1/platform/artifacts/upload-sessions/{id}/complete
DELETE /api/v1/platform/artifacts/upload-sessions/{id}
```

The configured part size, per-tenant reserved-byte ceiling and session TTL are
`PLATFORM_ARTIFACT_UPLOAD_PART_BYTES`,
`PLATFORM_ARTIFACT_MAX_TENANT_RESERVED_UPLOAD_BYTES`,
`PLATFORM_ARTIFACT_MAX_TENANT_ACTIVE_UPLOAD_SESSIONS`, and
`PLATFORM_ARTIFACT_UPLOAD_SESSION_TTL_SECONDS`, and
`PLATFORM_ARTIFACT_UPLOAD_PROCESSING_LEASE_SECONDS`. Synchronous multipart
admission uses `PLATFORM_ARTIFACT_MAX_UPLOAD_BYTES` for the file and
`PLATFORM_ARTIFACT_MAX_MULTIPART_REQUEST_BYTES` for the enclosing request;
startup validation requires the file bounds to agree and the request bound to
leave room for multipart framing. The service stores only hashed
tenant/subject keys in its durable session registry, reserves quota atomically,
and stores parts under server-generated private staging keys. Both reserved
bytes and active session count have per-tenant ceilings. A part is checkpointed
only after storage confirms its declared size and SHA-256; the service verifies
the same size and digest again while streaming staged parts into package
admission. Session
completion streams ordered parts through the same approved-contract ZIP
admission path as the synchronous endpoint. Dependency failures retain parts
and quota in `RETRYABLE`; terminal outcomes release quota, while part cleanup
can retry independently. Legacy non-tenant JWTs cannot use this API. Runtime
storage availability and authenticated HTTP acceptance remain release
gates and are not implied by this contract description.

### Metadata response

```json
{
  "id": "row|1001",
  "attachments": [
    {
      "artifactId": "artifact|1500",
      "name": "report-001.xlsx",
      "mimeType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "sizeBytes": 482913,
      "sha256": "a1b2...",
      "snapshotId": 13,
      "download": {
        "mode": "SIGNED_URL",
        "expiresInSeconds": 300
      }
    }
  ]
}
```

### Error semantics

```text
400 invalid package/manifest or range/checksum declaration
401 missing authentication
403 policy/tenant denial
404 artifact or row not visible
409 duplicate/version/conflicting idempotency key or upload state
410 retired or expired artifact
422 validation/relation/checksum/quarantine failure
413 package exceeds the configured maximum upload size
507 tenant upload quota exhausted
503 storage or dependency unavailable
```

API არასდროს აბრუნებს raw object path-ს, local filesystem path-ს ან undeclared
artifact-ს.

## 16. Upload reliability

სავალდებულოა:

- streaming SHA-256;
- durable, tenant-scoped resumable part upload for large packages;
- idempotency key package დონეზე;
- retry with bounded backoff;
- partial object cleanup;
- atomic manifest commit;
- optimistic concurrency/ETag package update-ზე;
- storage admission and atomically reserved tenant quota;
- orphan-object scanner;
- retryable job state და checkpoint.

## 17. Retention, archive და DR

- ingest object ინახება validation/replay window-ის განმავლობაში;
- published object გადადის archive retention policy-ში;
- legal hold purge-ს ბლოკავს;
- superseded versions ინარჩუნებს rollback-ს;
- archive object-ს აქვს checksum და snapshot reference;
- backup/restore ამოწმებს object existence-სა და DB locator parity-ს;
- measured RPO/RTO evidence აუცილებელია production closure-ისთვის.

## 18. Provenance

ყოველი artifact და row უნდა იძლეოდეს შემდეგ ჯაჭვს:

```text
source artifact (Entity)
  → ingest/materialization (Activity)
  → contract/steward/service (Agent)
  → canonical row (Entity)
  → snapshot/publication (Activity)
  → API distribution (Entity/Distribution)
```

Provenance ინახავს source, activity, agent, timestamp, checksum, revision,
derivation და supersession კავშირებს.

## 19. Operational controls

აუცილებელია scheduled checks:

- missing object detector;
- orphan DB locator detector;
- orphan storage object detector;
- checksum drift detector;
- relation orphan detector;
- duplicate logical identity detector;
- stale upload cleanup;
- expired signed URL/download audit;
- snapshot-to-cache count/checksum reconciliation;
- contract/documentation zero-drift report.

### Registered object integrity sweep

The API runs a bounded Data Plane sweep over registered content objects. It
checks storage existence first, compares stored byte size, then streams the full
SHA-256 only when the size agrees. Each batch uses the distributed platform job
lease, is limited by candidate count and total bytes, and starts only after
schema migrations complete. Current object state moves to `VERIFIED`, `MISSING`
or `CHECKSUM_MISMATCH`; each batch writes a run receipt, while an open issue per
object aggregates repeat detections and is resolved by a later verified read.
Storage/Data Plane failures leave the object due for retry. This sweep covers
registered Data Plane objects only. Orphan storage enumeration, orphan locator
and relation checks, snapshot/cache parity, and contract/documentation drift
remain separate required controls. Remote development acceptance and explicit
scope limits are recorded in
[`artifact-integrity-audit-runtime-2026-09-18.json`](../evidence/artifact-integrity-audit-runtime-2026-09-18.json).

### Relation reconciliation sweep

The API runs a bounded scheduled reconciliation over snapshots governed by
approved or retired artifact relation definitions. It selects snapshots with
no recent `ARTIFACT_RECONCILIATION` evidence, evaluates declared per-entity and
language cardinalities, appends durable gate evidence, and refreshes a
distributed job lease while processing the batch. Retired definitions remain
eligible so historical snapshots retain their original contract semantics.
A runtime `FAIL` is a data-integrity failure and blocks publication; successful
execution of the job alone does not imply the data passed. On 2026-09-18, the
remote development sweep evaluated snapshots 16 and 31 under dataset version
73: each had 225 entities, zero attachments, and 450 missing required slots
(`PRIMARY_FILE`, English and Georgian). The sweep is operational, while these
historical snapshots are not attachment-ready. See
[`artifact-relation-integrity-audit-runtime-2026-09-18.json`](../evidence/artifact-relation-integrity-audit-runtime-2026-09-18.json).

## 20. Acceptance checklist

- [ ] package manifest validated;
- [ ] contract revision approved;
- [ ] every row has stable identity;
- [ ] every artifact has checksum identity;
- [ ] every artifact has immutable object URI;
- [ ] every row↔artifact edge is explicit or deterministically resolved;
- [ ] no ambiguous/orphan/duplicate relation remains;
- [ ] MIME/size checks PASS (malware scan deferred: ADR-009);
- [ ] raw lineage is complete;
- [ ] canonical locator is persisted;
- [ ] dataset version and snapshot are bound;
- [ ] row/file/checksum reconciliation PASS;
- [ ] quality/privacy/security gates PASS;
- [ ] signed download policy verified;
- [ ] rollback and replay tested;
- [ ] archive/retention/DR evidence stored;
- [ ] API response matches declared projection;
- [ ] documentation/runtime ledger reconciled.

## 21. Final decision rules

1. File bytes belong only to Object Storage.
2. Identity and relation metadata belong to contract-governed Data Plane tables.
3. Contract and policy belong to Control Plane.
4. Historical retention and rollback belong to Archive Plane.
5. API serves only approved snapshot members.
6. Frontend receives metadata and short-lived signed URLs.
7. Ambiguity blocks publication.
8. Retry must be idempotent.
9. New content creates a new immutable version.
10. No consumer may lower these guarantees.

**Completion criterion:** artifact attachment work is complete only when the
package, artifact, row, relation, snapshot, policy, API response, download,
archive and rollback chain has automated tests and immutable evidence.**

## 22. Blocking implementation gate

Artifact/file-system implementation MUST NOT begin until the upper layers are
ready and verified. The required order is:

```text
Doctrine of Doctrines
  → Meta-schema / Control Plane
  → Physical database planes
  → Object Storage
  → Site contract
  → Access artifact schema
  → Ingestion/materialization
  → Snapshot/publication
  → API contract
  → Consumer/frontend
```

Before creating or migrating attachment tables, relations or generators, the
session must verify:

- Doctrine and cardinal invariants are approved;
- meta-schema declares artifact, locator, relation, snapshot and evidence
  primitives;
- Control/Ingestion/Data/Archive/Serving schemas and ownership are verified;
- private ingest/quarantine/archive/export storage is available;
- contract revision → dataset version binding exists;
- Access prefixes and generic attachment relation are declared;
- rollback, quarantine, checksum and evidence paths are defined.

If any prerequisite is missing, work stops with a blocker record in AIR. No
temporary table, hardcoded KIDS path, manual object key or silent fallback may be
introduced to bypass the gate. The complete session-start route is maintained in
`docs/reference/CANONICAL-FULL-TREE-DESIGN.md`.

## 23. Upper-layer preparation scope

Before attachment/file-system implementation, the following upper-layer work
must be completed and verified in order.

### Doctrine of Doctrines

Freeze platform invariants: contract-first, metadata-driven, no-loss/
no-downgrade, identity, lineage, snapshot, publication, ownership, fail-closed
blockers and evidence requirements. The artifact cardinality rule is part of the
doctrine, not a consumer-specific convention.

### Meta-schema / Control Plane

Declare and version these generic primitives:

```text
contract · contract_revision · dataset · dataset_version · field · key · relation
projection · policy · artifact · artifact_version · artifact_locator · snapshot
publication · evidence
```

Artifact attachment additionally requires declared identity, version, relation
role, cardinality, ordinal, access policy, checksum and snapshot membership.

### Physical database planes

Verify the ownership, schema, constraints and lifecycle of:

```text
Control Plane  — contract/policy/registry/governance
Ingestion Plane — package/artifact/load/staging/checkpoints/issues
Data Plane     — raw/source_record, entity_record, resource_locator,
                 entity_link, statistics/series, statistics/observation
Archive Plane  — immutable snapshots, artifact references, retention/legal hold,
                 rollback
Serving Plane  — cache, projection and response metadata
```

Required controls are PK/UK/FK, tenant/site ownership, checksum uniqueness,
immutable snapshot binding, artifact-to-row lineage, orphan detection, relation
cardinality, temporal validity and retention policy.

### Object Storage

Provision isolated `ingest`, `quarantine`, `archive` and `export` namespaces.
Every object requires an immutable key, checksum, MIME type, byte size, version,
retention, encryption and access policy. Multipart/resumable upload, idempotent
retry, deduplication, existence/checksum verification, quarantine/archive copy,
backup/restore and signed URL generation must be available.

### Contract binding

The contract must connect:

```text
contract revision
  → dataset version
  → artifact policy
  → row-artifact relation definition
  → immutable snapshot
```

It must declare allowed row targets, artifact roles, cardinality, primary/
supporting/derived semantics, mandatory/optional attachments, ordering, file
type/size rules and publication policy.

### Package and manifest engine

The operator flow is:

```text
package upload
  → automatic inventory
  → automatic manifest
  → deterministic matching
  → preview
  → approval
```

The manifest records package identity, contract/revision, original paths, safe
object keys, checksums, sizes/MIME, row keys, candidate relations, generator
version and package checksum.

### Upper-layer acceptance gate

The following must be `PASS` before Access attachment tables or relation rows are
created:

```text
Doctrine PASS
Meta-schema PASS
Physical schemas PASS
Object Storage PASS
Contract binding PASS
Security policy PASS
Rollback design PASS
Evidence model PASS
```

Only then may the implementation proceed:

```text
Access artifact tables
  → attachment relation rows
  → materialization
  → snapshot
  → API signed-download serving
  → frontend static-file retirement
```

## 24. User responsibilities versus platform responsibilities

### User supplies only

```text
1. business row stable key;
2. source data file (Access/CSV/XLSX/etc.);
3. physical attachments;
4. explicit row↔file mapping only when it cannot be derived safely;
5. contract code/revision and declared semantic choices.
```

The user does **not** enter artifact IDs, object keys, buckets, checksums,
snapshot IDs, signed URLs or SQL. These are platform-owned values.

### Platform creates and verifies

```text
manifest → checksum → artifact identity → object URI → relation edge
→ lineage → canonical locator → snapshot membership → signed distribution
```

## 25. Automatic manifest generation

If `manifest.json` is absent, the platform generates a preview manifest from the
Access package, file inventory and contract-declared key rules. It records:

- every original relative path;
- normalized safe object key;
- SHA-256 and byte size;
- MIME/type;
- source row key and artifact key;
- all deterministic row↔file candidates;
- contract code/revision and dataset version;
- package checksum and generator version.

The generated manifest is presented for review, persisted immutably after
acceptance and reused for idempotent retries. A user-supplied manifest is never
trusted without the same validation pipeline.

## 26. One-click package experience

The supported operator flow is intentionally small:

```text
Select package
  → Validate (preview + exact issues)
  → Confirm
  → Track progress
  → Receive governed result/download links
```

The platform still performs the full pipeline. A simple UX is not a simpler
data model; it is a governed orchestration facade over the complete model.

## 27. Explicit 1000-row / 1500-file rule

For 1000 business rows and 1500 files:

```text
1000 entity identities
1500 artifact identities (or fewer unique content objects)
N relation edges, where N is the actual number of attachments
```

The same physical file may be referenced by many rows without byte duplication.
The same row may reference many files with role and ordinal metadata. No implicit
Cartesian product is generated.

## 28. No-loss and no-downgrade invariant

The migration must preserve original bytes, source paths, source keys, row
ordering where material, lexical values, checksums, relations, provenance and
historical versions. If a target representation cannot preserve a source fact,
the package is blocked and the loss is reported; silent truncation, fabricated
value, forced one-to-one mapping or consumer-specific quality downgrade is
forbidden.

## 29. Current KIDS implementation boundary

The KIDS source inventory has been staged as 719 files and 532 unique
checksum-addressed objects under `geostat-ingest/kids/r8/resources/`, with an
inventory manifest preserving every original path. The conformance rule matches
225 stable resource rows to exactly 450 language-specific file entries and 450
distinct objects in test fixtures; the bucket inventory separately proves all
532 physical objects match their content-addressed keys. Those proofs establish
the row/path rule and stored inventory, but not runtime attachment state. Runtime
import, persisted snapshot bindings, tenant/site authorization, full
reconciliation and governed signed-download acceptance remain open. The
frontend's static `public/files` copy must remain until those checks reach
`VERIFIED`.

### 29.1 Implementation status (2026-09-18)

Upper-layer gate items implemented in source and verified by tests: meta-schema
primitives (`platform.artifact_policy`, `platform.artifact_relation_definition`),
physical registry (`ingest.artifact_object|artifact_manifest|artifact_version`,
`entity.artifact_attachment`), content-addressed storage port with presign,
manifest engine, deterministic matcher, reconciliation gate and signed-download
API. Contract-bound ZIP admission now resolves an approved site contract revision
and dataset version, validates the required Access table fields, and persists that
identity into manifest evidence (migrations 091–092). Runtime SQL Server migration
and partial-binding rejection fixture pass. Authenticated package upload smoke
still requires a scoped `WRITE_RESOURCE` token. KIDS binding is a seed row set only (migration 088). The full KIDS R8
package (Access + 450 row-referenced files) was assembled from the descriptor,
previewed, admitted as manifest 6 (451/451 VERIFIED, idempotent replay) and proven
slot-for-slot equal to published snapshot 52:
[`kids-r8-full-package-admission-runtime-2026-09-18.json`](../evidence/kids-r8-full-package-admission-runtime-2026-09-18.json). Status per
layer and open external gates: `docs/work/STORAGE-ARTIFACT-CLOSURE-CHECKLIST.md`.

### 29.2 Implementation status (2026-09-19)

§26 is implemented as a governed package run: `POST /api/v1/platform/artifacts/package-runs {manifestId}` accepts an
admitted, contract-bound manifest once (one run per package checksum) and a lease-guarded worker carries it through
the §7 protocol from row staging to release-gate evaluation, persisting progress after every stage
(`ingest.artifact_package_run`, append-only stage history). `GET …/{runId}` is the progress surface; `POST
…/{runId}/retry` resumes a stopped run at the stage where it stopped. §12 holds: any rejected row, blocked binding,
failed reconciliation or failed gate leaves the run `BLOCKED` and nothing is published — a run never publishes.
§19 gains the storage sweep (objects present in storage but unknown to the registry are recorded as orphans, never
deleted). Dataset formats and storage providers are ports (`PackageDatasetCarrier`, `PackageDatasetIngestor`,
`ArtifactObjectStore`, `ArtifactUploadStagingStore`, `ArtifactObjectInventory`). Source and tests only; runtime
acceptance is checklist item 17.4.

### 29.3 Shipped manifest and end-to-end run (2026-09-19)

A producer may ship `manifest.json` at the package root (`geostat.artifact-package-manifest.v1`): contract identity,
dataset entry, one claim per file (`path`, `sha256`, `bytes`, `mediaType`) and one claim per row↔file edge (`rowKey`,
`relationCode`, `language`, `ordinal`, `path`). It is a claim, not an authority (§25): admission derives its own
manifest and relation plan and accepts the package only when the claim is exact; otherwise nothing is written. The
entry is reserved and is not package content. The package layout under the root is decided by the approved relation
rule (`packageRoot`); the `files/` folder in §2 is illustrative. The complete KIDS R8 package (Access build 8.0.1, 450
row-bound files, shipped manifest) ran end to end on dev to `REVIEW_REQUIRED`:
[`kids-r8-package-end-to-end-runtime-2026-09-19.json`](../evidence/kids-r8-package-end-to-end-runtime-2026-09-19.json).

### 29.4 Attachments declared in package tables (2026-09-19)

§4.2/§4.3 are implemented as match rule type `RELATION_TABLE`. The relation names, in its approved `match_rule_json`,
the relation table and its entity-key, artifact-key and ordinal fields (optionally a role filter and a language
field), the artifact table with its key and file-name fields, and the `packageRoot` the file names live under. Ordinals
are positions: `1..n` without gaps per row and language. One file may serve many rows and one row may have many files
(§27); nothing is inferred. The edges are derived at admission, stored in the accepted manifest document (§25) and
replayed from it when the snapshot is bound, so the preview, the document and the stored attachments are one set.
