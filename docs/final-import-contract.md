# Final import contract — Access package to governed data

## Package states and modes

Every artifact declares exactly one `load_mode`:

- `SNAPSHOT`: complete declared scope for its dataset set; absence can become deletion only after scope, authority and reconciliation checks succeed.
- `DELTA`: only explicit inserts/updates/tombstones; omission means no change.

Every business table has a declared natural key, `source_row_key`, source operation (`UPSERT` or `TOMBSTONE`) and lineage to the immutable raw document. Tombstones must name the target natural key and reason; they are never inferred in delta mode.

## Deterministic import protocol

```text
upload immutable artifact → checksum + manifest validation → preview
→ authorize contract revision → create load + initial checkpoint
→ stage independently idempotent chunks → raw retention
→ structural/relational/classifier validation → quarantine failures
→ semantic resolution and review → create immutable publication candidate
→ approve/publish pointer OR rollback pointer
```

The artifact is copied to object storage before parsing. Resume always uses that stored immutable artifact, never a workstation path or an altered re-upload.

## Canonical Access execution endpoint

`POST /api/v1/platform/access/semantic/preview` is read-only. `POST /api/v1/platform/access/semantic/ingest` first runs the identical structural and Control-Plane binding checks, then creates **one** `ingest.batch` and **one** immutable artifact for the whole package. It stages every active contract-bound dataset table in contract load order; each `dataset_load` has its own durable checkpoint and can resume independently. A unique data-plane index on `(batch_id, dataset_version_id, source_name)` makes retrying the same package/dataset idempotent.

`POST /api/v1/platform/access/semantic/batches/{batchId}/resume` is package-level resume: it materializes the originally retained object, rechecks its declared package contract, and continues only incomplete dataset checkpoints. It never accepts a replacement file.

Technical validation is also package-safe: a batch enters `REVIEW_REQUIRED` only when **every** member dataset load is `VALIDATED`; any rejected member leaves the package `REJECTED`, and any outstanding member keeps it `STAGING`. A single valid table can never make a multi-table package publishable.

## Idempotency, checkpoint and retry

`idempotency_key = SHA-256(contract_code, contract_revision, package_code, package_version, artifact_checksum, load_mode)`.

The system records a durable checkpoint after each committed chunk: artifact checksum, dataset, chunk ordinal, source row interval, rows staged, rejected rows and deterministic chunk hash. Replaying a completed chunk with the same hash is a no-op; a conflicting hash fails the load. Retry uses exponential backoff only for transient transport/storage/database failures; semantic errors are not retried blindly.

## Validation layers

1. **Manifest:** exact contract code/revision, fixed table names/columns, datatype, required metadata, checksum.
2. **Structural:** natural-key uniqueness, nullability, parseability, bounded row count/type limits.
3. **Relational:** declared FK/cardinality and source-row lineage.
4. **Classifier:** scheme/version/item/alias existence or explicit proposal; no unmarked unresolved value.
5. **Statistical:** carrier parse, cell grain, DSD/metric/unit gate before observation creation.
6. **Policy:** authorization, PII/security classification, retention, disclosure and publication eligibility.

## Quarantine and recoverability

A failing row never disappears. It is written with raw payload, rule identifier, severity, source coordinates and a remediation state. A row-level recoverable defect can quarantine that row while unaffected chunks continue; a contract/manifest or referential-scope defect fails the load atomically before semantic publication. Resolution produces a new projection revision, not a mutation of raw/staged evidence.

## Publication and rollback

Validation produces a candidate snapshot. Approval atomically moves the dataset/dataflow publication pointer to that immutable snapshot. Rollback only moves the pointer to an earlier eligible snapshot and records actor/reason; it does not delete artifacts, raw rows, validation evidence or prior observations.
