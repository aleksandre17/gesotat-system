# Snapshot binding runtime closure — 2026-09-15

## Implemented path

`site_contract_revision_id` and `dataset_code` resolve the contract dataset binding and its `dataset_version_id`. Canonical ENTITY and STATISTICAL queries require that version and select only immutable snapshots in an approved lifecycle state. Entity rows are constrained by `entity_record.dataset_snapshot_id`; statistical rows are constrained by `statistics.series.dataset_snapshot_id`.

## Invariants

- no `MAX(dataset_snapshot_id)` heuristic;
- no KIDS-specific physical-table read after canonical binding;
- missing dataset-version binding fails closed;
- snapshot scope is applied identically to row and count queries;
- projection remains contract-field driven.

## Verification

- API build: PASS (`:api:bootJar`);
- isolated geostat API container recreated from the new artifact: PASS;
- Redis credentials are supplied to both Spring Redis property namespaces;
- production activation/approval evidence remains governed by the release gate and is not fabricated here.
