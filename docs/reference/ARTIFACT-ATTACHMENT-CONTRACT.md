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
400 invalid package/manifest
401 missing authentication
403 policy/tenant denial
404 artifact or row not visible
409 duplicate/version/conflicting idempotency key
410 retired or expired artifact
422 validation/relation/checksum/quarantine failure
503 storage or dependency unavailable
```

API არასდროს აბრუნებს raw object path-ს, local filesystem path-ს ან undeclared
artifact-ს.

## 16. Upload reliability

სავალდებულოა:

- streaming SHA-256;
- multipart/resumable upload დიდი ფაილებისთვის;
- idempotency key package დონეზე;
- retry with bounded backoff;
- partial object cleanup;
- atomic manifest commit;
- optimistic concurrency/ETag package update-ზე;
- storage quota და tenant quota;
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

## 23. User responsibilities versus platform responsibilities

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

## 24. Automatic manifest generation

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

## 25. One-click package experience

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

## 26. Explicit 1000-row / 1500-file rule

For 1000 business rows and 1500 files:

```text
1000 entity identities
1500 artifact identities (or fewer unique content objects)
N relation edges, where N is the actual number of attachments
```

The same physical file may be referenced by many rows without byte duplication.
The same row may reference many files with role and ordinal metadata. No implicit
Cartesian product is generated.

## 27. No-loss and no-downgrade invariant

The migration must preserve original bytes, source paths, source keys, row
ordering where material, lexical values, checksums, relations, provenance and
historical versions. If a target representation cannot preserve a source fact,
the package is blocked and the loss is reported; silent truncation, fabricated
value, forced one-to-one mapping or consumer-specific quality downgrade is
forbidden.

## 28. Current KIDS implementation boundary

The KIDS source inventory has been staged as 719 files and 532 unique
checksum-addressed objects under `geostat-ingest/kids/r8/resources/`, with an
inventory manifest preserving every original path. The remaining implementation
boundary is deterministic inventory-to-resource binding, persistence of
`entity.resource_locator`/generic attachment relations, snapshot membership,
checksum reconciliation and governed signed-download serving. Only after those
checks reach `VERIFIED` may the frontend's static `public/files` copy be retired.
