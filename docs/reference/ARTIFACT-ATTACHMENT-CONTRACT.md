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
  → file type/size/malware validation
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
ეს შეესაბამება მხოლოდ ტექსტური payload-ის შემთხვევაში. MIME შემოწმება malware
სკანირება არ არის; production admission-ს სჭირდება ჩართული scanner, quarantine
flow და მისი runtime evidence.

Malware admission იყენებს `ArtifactMalwareScanner` provider boundary-ს და ამჟამად
ClamAV `clamd`-ს `INSTREAM` პროტოკოლით. TCP daemon უნდა იყოს მხოლოდ იზოლირებულ,
სანდო ქსელში, რადგან პროტოკოლი არც peer authentication-ს და არც transport
encryption-ს იძლევა. უცნობი verdict, timeout, daemon error ან დაუკონფიგურებელი
provider ატვირთვას აჩერებს (`503`); აღმოჩენილი საფრთხე ვერ შევა ingest pool-ში,
ხოლო bytes გადადის private `geostat-quarantine` bucket-ში და მისი audit metadata
Data Plane-ში იწერება. Scanner-ის stream maximum უნდა ემთხვეოდეს clamd-ის
`StreamMaxLength`-ს; production-ზე საჭიროა signature freshness, resource და
quarantine retention/cleanup evidence.

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
included in its package checksum. Inventory import remains a separate governed
path for previously staged content.

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
`PLATFORM_ARTIFACT_UPLOAD_PROCESSING_LEASE_SECONDS`. The service stores only hashed
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
storage/clamd availability and authenticated HTTP acceptance remain release
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

## 20. Acceptance checklist

- [ ] package manifest validated;
- [ ] contract revision approved;
- [ ] every row has stable identity;
- [ ] every artifact has checksum identity;
- [ ] every artifact has immutable object URI;
- [ ] every row↔artifact edge is explicit or deterministically resolved;
- [ ] no ambiguous/orphan/duplicate relation remains;
- [ ] MIME/size/malware checks PASS;
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
inventory manifest preserving every original path. The remaining implementation
boundary is deterministic inventory-to-resource binding, persistence of
`entity.resource_locator`/generic attachment relations, snapshot membership,
checksum reconciliation and governed signed-download serving. Only after those
checks reach `VERIFIED` may the frontend's static `public/files` copy be retired.

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
still requires a scoped `WRITE_RESOURCE` token. KIDS binding is a seed row set only (migration 088). Status per
layer and open external gates: `docs/work/STORAGE-ARTIFACT-CLOSURE-CHECKLIST.md`.
