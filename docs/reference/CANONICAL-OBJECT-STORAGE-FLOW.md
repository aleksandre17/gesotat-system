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
