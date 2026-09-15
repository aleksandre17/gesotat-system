# KIDS resource artifact storage inventory — 2026-09-15

## Upload result

- source root: `platform/apps/geostat/frontend/kids/public/files`
- discovered files: **719**
- unique SHA-256 payloads: **532**
- object storage: private geostat MinIO, bucket `geostat-ingest`
- object prefix: `kids/r8/resources/`
- stored payload objects: **532** (content-addressed deduplication)
- inventory object: `kids/r8/resources/inventory.json`
- total objects under prefix: **533**

Long or unsafe source filenames are not used as object identity. Each object is
addressed by its SHA-256 plus extension; `inventory.json` preserves the original
relative path, byte size, checksum and governed object name. This prevents path
collisions and makes re-upload idempotent.

## Governance status

The upload is an immutable ingest-stage copy, not publication. Resource rows are
bound only after stable source-key, checksum, dataset-version and snapshot
reconciliation. The frontend remains unchanged and no static file was deleted.

## Next closure

Join `inventory.json` to `KIDS_RESOURCE.source_resource_id` / `source_row_key`,
create `entity.resource_locator` records, verify every object checksum, and
promote only approved snapshot members to governed signed downloads.
