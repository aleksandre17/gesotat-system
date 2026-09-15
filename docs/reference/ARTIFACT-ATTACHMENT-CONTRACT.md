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
