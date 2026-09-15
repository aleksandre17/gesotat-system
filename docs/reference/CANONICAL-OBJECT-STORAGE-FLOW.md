# Canonical Object Storage Flow

## Authority and location

```text
Linux server: 192.168.1.199
Project: /home/administrator/geostat
Infra: /home/administrator/geostat/backend/infra/geostat-platform
Container: geostat-minio
Network: geostat-net
```

## Bucket tree

```text
geostat-ingest/
  kids/r8/resources/
    <sha256>.xlsx
    <sha256>.xls
    inventory.json

geostat-quarantine/
  rejected/...

geostat-archive/
  snapshots/<snapshot-id>/...

geostat-export/
  operations/<operation-id>/...
```

## Canonical layer chain

```text
KIDS source files
  ↓
Object Storage: immutable bytes + SHA-256
  ↓
ingest.artifact
  artifact_id, object_uri, checksum, byte_size
  ↓
raw.source_record
  source_key, artifact_id, payload_hash, snapshot_id
  ↓
Control Plane
  contract revision → dataset version → storage binding → publication policy
  ↓
publication.dataset_snapshot
  immutable snapshot + status + checksum
  ↓
entity.entity_record
entity.resource_locator
entity.entity_classification
  ↓
API contract projection
  ↓
short-lived signed download URL
  ↓
Frontend
```

## Boundary rules

- Object Storage is the sole authority for original file bytes.
- Data Plane stores identity, typed metadata, locator, checksum and lineage;
  it does not duplicate source binaries.
- Control Plane owns contract, dataset version, publication status and policy.
- Archive Plane owns retention, legal hold and rollback history.
- API exposes only contract-approved metadata and signed URLs.
- Frontend never reconstructs physical paths and never reads MinIO directly.
- Object names are checksum-addressed; original paths remain in `inventory.json`.
- A resource becomes downloadable only after locator, checksum, snapshot and
  publication reconciliation pass.

## KIDS staging state

- 719 source files discovered;
- 532 unique SHA-256 payload objects staged in `geostat-ingest`;
- `inventory.json` preserves every original path and object mapping;
- DB locator binding and final published signed-download projection are the next
  governed step;
- frontend static files remain unchanged until that binding reaches `VERIFIED`.

## სრული განმარტება

ჩვენი Object Storage არის geostat-ის იზოლირებული MinIO/S3 სისტემა.

განთავსება:

```text
Linux server: 192.168.1.199
Project: /home/administrator/geostat
Infra: /home/administrator/geostat/backend/infra/geostat-platform
Container: geostat-minio
Network: geostat-net
```

ფაილის metadata-სა და ბაიტების გაყოფა განზრახულია:

- Object Storage-ში არის თვითონ Excel-ის ბაიტები;
- Data Plane-ში არის resource identity და typed metadata;
- `entity.resource_locator` ინახავს URI-ს, MIME type-ს, checksum-სა და access policy-ს;
- Control Plane წყვეტს, შეიძლება თუ არა resource-ის გამოქვეყნება;
- API არ აბრუნებს ფიზიკურ storage path-ს;
- API აბრუნებს მხოლოდ კონტრაქტით დაშვებულ metadata-ს და დროებით signed URL-ს;
- Frontend პირდაპირ MinIO-ს ან `/files/...` path-ს არ უნდა მიმართავდეს.

ამჟამად შესრულებულია:

- 719 KIDS ფაილის inventory;
- 532 უნიკალური object-ის ingest bucket-ში ატვირთვა;
- checksum-based deduplication;
- `inventory.json`-ის შენახვა;
- frontend-ის static ფაილები ჯერ არ წაშლილა.

დარჩენილი დამაკავშირებელი ნაბიჯია:

```text
inventory.json
  → KIDS_RESOURCE.source_resource_id/source_row_key
  → entity.resource_locator
  → dataset_snapshot
  → API signed download
```

ამის დასრულების შემდეგ frontend-ის არსებული `public/files` სტრუქტურა აღარ იქნება
runtime authority და მისი უსაფრთხოდ retirement შესაძლებელი გახდება.

## ფაილის ატვირთვა და 1000-row lineage

### ატვირთვის პროცესი

```text
ფაილი
  → SHA-256 checksum
  → malware/type/size validation
  → private Object Storage
  → ingest.artifact
  → Access/resource identity
  → canonical snapshot
  → publication
  → signed download URL
```

ფაილის ბაიტები Access-ში არ ინახება.

### Access-ში ფაილის identity

რესურსის ცხრილია `__ent_kids_resource`. მისი კანონიკური ფორმაა:

```json
{
  "source_resource_id": "resource|128",
  "category_item_ref": "KIDS_GOAL_CATEGORY|3",
  "title_ka": "მოსახლეობის რიცხოვნობა ასაკისა და სქესის მიხედვით",
  "title_en": "Population by age and sex",
  "path_ka": "object://geostat-ingest/kids/r8/resources/a1b2...xlsx",
  "path_en": "object://geostat-ingest/kids/r8/resources/a1b2...xlsx",
  "source_row_key": "files|128",
  "operation": "UPSERT"
}
```

`source_resource_id` არის ფაილის/resource-ის სტაბილური identity.

### Raw lineage

`__raw_document` ინახავს artifact-ის lineage-ს და არა ბიზნეს-ცხრილის დუბლირებულ
მონაცემს:

```json
{
  "source_row_key": "files|128",
  "original_filename": "population-by-age.xlsx",
  "mime_type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "byte_size": 482913,
  "checksum": "a1b2...",
  "source_uri": "s3://geostat-ingest/kids/r8/resources/a1b2....xlsx",
  "payload_reference": "s3://geostat-ingest/kids/r8/resources/a1b2....xlsx",
  "ingestion_batch": "kids-r8",
  "parser_status": "PARSED",
  "validation_status": "VALID"
}
```

### ერთი ფაილი და 1000 row

თუ ერთი Excel ფაილი შეიცავს 1000 observation row-ს:

```text
ერთი ფაილი
  → ერთი resource entity
  → ერთი artifact identity
  → 1000 raw/source row
  → 1000 typed canonical row
```

ფაილი 1000-ჯერ არ მეორდება. ყველა row მიუთითებს იმავე artifact identity-ზე და
საკუთარ `source_row_number/source_key`-ზე:

```json
{
  "resourceId": "resource|128",
  "artifact": {
    "artifactId": 44,
    "objectUri": "s3://geostat-ingest/kids/r8/resources/a1b2.xlsx",
    "sha256": "a1b2..."
  },
  "rows": [
    {
      "sourceRowNumber": 1,
      "sourceKey": "files|128|row|1",
      "observationId": "obs|128|1"
    },
    {
      "sourceRowNumber": 2,
      "sourceKey": "files|128|row|2",
      "observationId": "obs|128|2"
    }
  ]
}
```

### საბოლოო კავშირი

```text
KIDS_RESOURCE
  source_resource_id
      ↓
entity.entity_record
  entity_id
      ↓
entity.resource_locator
  object_uri + checksum + access_policy
      ↓
publication.dataset_snapshot
  dataset_snapshot_id
      ↓
statistics.series / statistics.observation
  source_record_id + dataset_snapshot_id
      ↓
API response
  resource metadata + observations + governed download
```

ეს მოდელი უზრუნველყოფს ერთი ფაილის მრავალ row-ზე მიბმას, ზუსტ lineage-ს,
checksum integrity-ს, immutable snapshot-ს, idempotent re-upload-ს, duplicate
detection-სა და rollback-ს. API აბრუნებს მხოლოდ კონტრაქტით დამტკიცებულ metadata-ს,
typed data-სა და დროებით signed URL-ს.
