# Managed Access Semantic Package v3

## Purpose

V3 is a self-describing Access input package for the unified canonical platform. It declares semantic intent; it does not declare SQL targets, connection strings, DDL, executable SQL, or permission changes.

```text
Access package declarations + source tables
  → read-only preview / physical-schema validation
  → reviewed Core contract resolution
  → chunked staging and validation
  → raw + typed semantic projections
  → immutable published snapshot / site API
```

The file may propose `DRAFT` metadata. It can never create a published classifier, metric, relationship, visualization, dataset or physical business table.

## Mandatory package tables

| Access table | Grain | Required columns |
|---|---|---|
| `__gs_package` | exactly one package | `product_code`, `contract_code`, `contract_revision`, `package_code`, `package_version` |
| `__gs_dataset` | one logical source dataset | `dataset_code`, `access_table_name`, `data_family`, `business_grain` |
| `__gs_field` | one declared source field | `dataset_code`, `field_name`, `logical_type`, `semantic_role`, `required` |
| `__gs_key` | one field in a logical key | `dataset_code`, `field_name`, `key_role`, `key_order` |
| `__gs_projection` | one typed projection | `projection_code`, `dataset_code`, `projection_family`, `mapping_json`, `approval_state` |

`__gs_relation` is optional but mandatory whenever source datasets have a declared relationship:

| Column | Meaning |
|---|---|
| `relationship_code` | stable semantic relationship code, not SQL constraint name |
| `from_dataset_code`, `from_field` | source endpoint |
| `to_dataset_code`, `to_field` | target endpoint |
| `cardinality` | `ONE_TO_ONE`, `ONE_TO_MANY`, `MANY_TO_ONE`, `MANY_TO_MANY` |
| `required` | whether unresolved endpoint blocks release |

Every non-technical source column must have exactly one `__gs_field` declaration. A declared non-existent column or an undeclared actual column fails preview. This prevents accidental schema drift.

## Optional per-carrier statistical evidence

When a source table carries many possible statistical documents (for example JSON within content rows), `__gs_statistical_binding` may declare the reviewed carrier boundary without inventing statistical meaning:

| Column | Meaning |
|---|---|
| `source_dataset_code` | declared source dataset |
| `source_external_key` | source natural-key value of one carrier |
| `projection_code` | declared `STATISTICAL_WIDE_JSON` projection |
| `dataflow_code` | proposed stable dataflow identity |
| `binding_state` | `DRAFT` or `READY`, never ahead of its projection |
| `evidence_kind` | factual source representation, such as `VERIFIED_JSON_ARRAY_OR_ESCAPED_TEXT` |

This table is a bounded evidence register, not a metric table. A `READY` binding still requires the authoritative Control Plane to resolve its metric, unit, aggregation, DSD and classification aliases. Preview validates dataset/projection compatibility, lifecycle ordering and duplicate bindings without scanning arbitrary source rows, keeping it safe for million-row packages.

## Projection families

| Family | Canonical target |
|---|---|
| `ENTITY` | entity record, localized text, locators, classifications |
| `RELATION` | typed entity relation/link |
| `STATISTICAL` | series, observations, dimensions |
| `STATISTICAL_WIDE_JSON` | approved nested/wide JSON normalized to observations |
| `GEO` | geographic feature and typed links |
| `REFERENCE` | reviewed controlled vocabulary proposal |
| `RAW` | immutable source retention only |

`mapping_json` is JSON declarative metadata, never SQL. Every projection is initially `DRAFT`; only a reviewed projection that resolves to approved Core codes may become `READY`.

## Identifiers

The package uses stable codes, never platform database IDs. `contract_code` and `contract_revision` bind it to the immutable Control-Plane contract that was authored for this product before the file was filled:

```text
product_code: KIDS_PORTAL
contract_code: KIDS_PORTAL_CONTENT_V1
contract_revision: 1
dataset_code: KIDS_FILE_RESOURCE
classification_scheme_code: UN_SDG_GOAL
classification_code: 2
metric_code: CHILD_HEALTH_ABORTIONS_BY_AGE
dimension_code: AGE_GROUP
relationship_code: BELONGS_TO
```

The platform resolves these codes in the authoritative registry and records the immutable resolved version in the publication snapshot.

## Current endpoint

```text
POST /platform/access/semantic/preview
multipart field: file
```

The endpoint is deliberately read-only. It returns declared counts and deterministic validation issues; it does not upload the artifact, create a contract, modify metadata, or write source rows.

The formal separation of Access-authoring responsibilities from Control Plane responsibilities is in [access-and-control-plane-responsibility-model.md](access-and-control-plane-responsibility-model.md). It is a binding design rule, not an optional implementation note.

## Control-Plane-first authoring

The management panel creates the complete site/product contract first: datasets and grain, attributes, relationships, classifications, metrics/dimensions, validation rules, quality thresholds, source bindings, visibility and release policy. It then generates a constrained Access v3 template for that exact `contract_code` and revision.

An Access author fills source rows and preserves the generated declarations. The package is valid only when its declared shape and semantic projection match the approved Control-Plane contract. Access may report a mismatch or propose a future DRAFT through a separate review workflow, but it cannot redefine the contract used by an import.

## Deliberate boundaries

- An Access package is an input contract, not the system of record.
- Raw source data is preserved unchanged, including unknown fields.
- No source-supplied physical table/database/schema name is accepted as a target instruction.
- No arbitrary relation is invented from matching values; declared relationships are validated against keys and later steward-reviewed.
- For million-row packages, preview is metadata/schema-only; ingestion uses chunk/checkpoint processing in the subsequent staged import step.
