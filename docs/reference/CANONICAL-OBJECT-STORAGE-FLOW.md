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
