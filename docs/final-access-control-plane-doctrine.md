# Final Access / Control Plane doctrine

## Invariants

1. An Access package is a versioned distribution of one already-authored ingestion contract; it cannot create physical databases, tables, credentials, SQL, publication, or approval.
2. The Control Plane is authoritative for identity, semantic meaning, classifier lifecycle, metric/unit/DSD, policy, approval, publication and rollback.
3. The Data Plane is authoritative for immutable artifacts, raw rows, typed projections, quality evidence, snapshots and serving views; it contains no global governance authority. The immutable uploaded artifact is the raw-byte authority; row locators do not duplicate generated JSON.
4. Every canonical row, relation, classifier assignment and observation has immutable source artifact, source dataset, source key and source-row lineage.
5. A source value is never silently corrected. Normalization is explicit, reversible and recorded as an alias/crosswalk decision.
6. A raw document, canonical entity, statistical observation and chart are different objects. A chart never owns numerical values.
7. Contract changes are additive revisions. Published snapshots are immutable; rollback moves a publication pointer and never mutates history.
8. A package can carry classifier values and proposals, but the package is never the authoritative classifier registry.

## Authority boundary

| Concern | Access package | Control Plane | Data Plane |
|---|---|---|---|
| Source shape and rows | declares and supplies | validates | retains raw source snapshot |
| Dataset identity/grain | carries issued codes | authors/revises/approves | resolves version at load |
| Classifier source values | supplies values, aliases and proposals | owns scheme/version/item lifecycle | stores resolved snapshot references |
| Entity/relation source facts | supplies keys and declared relations | approves semantics | writes typed entity/relation rows |
| Statistical cells | supplies carrier/input values | owns DSD, metric, unit, dimensions | writes observations only after resolution |
| Import execution | none beyond artifact | policy and authorization | chunking, checkpoint, quarantine, idempotency |
| Publication/chart | none | approval and chart definition | immutable published snapshot and serving data |

## Shared deterministic contract

Both sides share only stable codes, declared grain, keys, relation endpoints, classifier references, validation rule identifiers, artifact checksum and contract revision. Database IDs, target schema/table names, secrets, executable expressions and publication commands are forbidden in Access.

## Lifecycle

`DRAFT → PREVIEWED → READY_FOR_REVIEW → APPROVED_CONTRACT → RAW_INGESTED → VALIDATED → SEMANTIC_REVIEW → PUBLISHED → SUPERSEDED`.

`READY_FOR_REVIEW` means structurally valid; it never means published. A partial semantic path may retain raw evidence but cannot publish unresolved classifier/statistical projections.
