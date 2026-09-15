# Access package and Control Plane responsibility model

## Non-negotiable rule

The Access file is a **versioned source contract**.  The platform Control Plane is the **authoritative semantic and publication control plane**.  Neither substitutes for the other.

This is a bounded-context / anti-corruption-layer design: source-specific reality stays in the package and raw lineage; canonical meaning, governance and public release stay in Core.  The boundary prevents both uncontrolled schema creation and loss of source fidelity.

## Authority matrix

| Concern | Semantic Access package | Control Plane / management panel | Shared, deterministic contract |
|---|---|---|---|
| Physical source shape | Declares every non-technical Access table, column and source key | Validates against the received artifact | Preview fails on drift: missing/undeclared table column or invalid key |
| Logical datasets | Carries the panel-generated `dataset_code`, family and grain declaration | Authors and approves the Core dataset/version; a future proposal is a separate DRAFT workflow | Same stable codes; no database IDs in files |
| Virtual relational layer | Declares source relationships and cardinality | Validates keys, approves canonical relation type and maps it to entity links | A relation target must be a declared candidate key; no guessed join |
| Classification / codelists | Supplies only source values under panel-issued field bindings | Owns scheme/version/item lifecycle, crosswalk approval and publication | Package references approved bindings only; unresolved codes quarantine/reject the affected projection |
| Statistics | Supplies values under panel-issued observation/wide-JSON bindings | Owns metric, unit, aggregation, dimensions, confidentiality and release status | A metric/dimension is usable only after Core resolution to an approved version |
| Non-statistical entities | Declares entity, text, locator, attribute and relation source fields | Owns canonical entity type, product/dataset ownership and public representation | Raw record plus typed entity projection share one lineage ID |
| Charts / visualizations | Has no publication authority | Owns visualization definition, access policy and snapshot binding | Charts query released observations/entities, never duplicate source data |
| Schema / storage | Cannot name target database/schema/table, send SQL, DDL or credentials | Owns physical shared tables, partitions, indexes, retention and object storage | Import uses only platform-generated load IDs and resolved contracts |
| Import execution | Provides data artifact and source row identity | Owns artifact registration, virus/type checks, chunking, checkpoint/retry, idempotency and quarantine | Each chunk has artifact hash, load ID, source row identity and audit event |
| Approval / release | Can request `DRAFT` or `READY` review state only | Owns steward approval, compatibility checks, immutable snapshot and rollback | `READY` is not publication; only a published snapshot is public |
| Security | Contains no target credentials or executable instructions | Owns client identity, RBAC/ABAC, product scopes, audit and retention | Product/dataset codes plus signed client scope identify access |

## What the Access contract must contain

`__gs_package`, `__gs_page`, `__gs_dataset`, `__gs_field`, `__gs_key`, and `__gs_projection` are mandatory. `__gs_relation` is mandatory when more than one source table participates in a relationship. `__gs_page` carries portable logical page identity (`page_code`/`node_code`); numeric runtime IDs are assigned by Control Plane.

The package must declare every source column exactly once. It must contain the panel-issued `contract_code` + `contract_revision`, stable codes, logical types, semantic roles, key role/order, data family, grain and declarative projections. A mapping is JSON metadata only; SQL, target table names, connection strings, DDL, scripts, permissions and publication commands are prohibited.

## What the management panel must expose

The panel is not an alternate data-entry form for arbitrary rows. It is the governed workflow for: package preview result; Core code resolution; classifier/metric/unit stewardship; relationship review; compatibility diff; quality threshold; artifact/load monitoring; quarantine repair; approval; snapshot publication; rollback; client-product scope management; and audit evidence.

## Lifecycle

```text
CONTROL PLANE -> authors/reviews immutable contract revision
              -> generates constrained Access v3 template
AUTHOR        -> fills source rows; cannot alter contract meaning
              -> read-only preview (shape + contract invariants)
              -> approved immutable contract version
       -> chunked, resumable raw ingestion
       -> typed projections + quality gates
       -> immutable published snapshot
       -> product-scoped site API
```

An Access file cannot autonomously create a new logical structure. A new requirement starts as a DRAFT proposal in the management panel, within the existing generic Core metadata tables; it never creates a new physical business table. This is why the architecture can evolve without becoming an unreadable EAV/key-value store or adding one table per website.

## Enforcement order

1. Package parser verifies required metadata tables and one header.
2. Preview verifies source shape, declarations, allowed logical types/roles, keys, relation endpoints/cardinality and declarative JSON.
3. Core resolver verifies product/dataset ownership plus published classifier, metric, unit and dimension references.
4. Importer validates chunks under the immutable resolved contract; invalid rows are quarantined with source lineage.
5. Publication validates completeness, quality, privacy and compatibility before producing a snapshot.

No later layer weakens an earlier layer. A manually edited package, a management-panel action, and an API caller all travel through the same lifecycle and audit trail.
**Runtime baseline:** KIDS_PORTAL_V1 revision 8. See [KIDS R8 current status and acceptance](KIDS-R8-current-status-and-acceptance.md) for deployed evidence.
