# KIDS complete site contract — revision 6

Migration `022_kids_complete_site_contract.sql` is the authoritative, versioned KIDS site contract. The Control Plane declares the structure and governance; the Access package is its data-bearing executor. The migration is additive: it does not edit revisions 1–5 or their artifacts.

## Control-Plane model

| Registry | Responsibility |
|---|---|
| `site_contract_revision` | immutable contract identity, parent, compatibility, checksum and document |
| `site_contract_node` | canonical site hierarchy and routes |
| `site_contract_dataset` | exact dataset version, Access table, grain, natural key and load order |
| `site_contract_field` | ordered typed fields, requiredness, semantic role and classifier binding |
| `site_contract_relation` | endpoints, cardinality, requiredness and enforcement policy |
| `site_contract_classifier` | version, authority, unknown-value and publication policies |
| `statistical_dataflow` / `statistical_dsd` / `statistical_component` | SDMX-compatible statistical structure |
| `site_contract_gate` | blocking structural, integrity, semantic, lineage and publication rules |
| `ingestion_contract_revision` / `contract_revision_source` | immutable executable bindings for current and historical contract revisions |

All revision-6 dataset bindings point to immutable `dataset_version.version = 6`. Before the active pointer moves, revision 5 and its active sources are snapshotted into the immutable revision registry. Revision 6 is then snapshotted separately. Existing `contract_source` identities are updated in place with `MERGE`, preserving the unique `(contract_id, source_locator)` constraint, while the resolver continues to accept any registered historical revision against its own bindings.

## Site hierarchy

```text
KIDS_ROOT (/kids)
├─ KIDS_GOALS (/goals)             → KIDS_GOAL
├─ KIDS_RESOURCES (/resources)     → KIDS_RESOURCE
├─ KIDS_GLOSSARY (/glossary)       → KIDS_GLOSSARY_ENTRY
├─ KIDS_STATISTICS (/statistics)   → KIDS_STATISTICAL_INPUT
└─ KIDS_CLASSIFIERS (/classifiers) → KIDS_CLASSIFIER_ITEM
```

This is a structural hierarchy only. The source does not prove a direct resource-to-goal relationship or glossary translation groups, so neither is invented.

## Dataset contract

Revision 6 declares 12 datasets and 79 ordered fields across five families:

- REFERENCE: classifier scheme, version, item, alias and hierarchy.
- RAW: immutable-source row locator and statistical-carrier locator/checksum.
- ENTITY: goal, resource and glossary entry.
- RELATION: normalized resource-to-subcategory assignment.
- STATISTICAL: lossless long-form input cell.

Sixteen relations cover classifier ownership, all entity classifications, resource/subcategory and resource/carrier links, carrier/input ownership, and raw lineage for every source-derived record. The Access artifact enforces the same 16 relationships physically.

## Classifier contract

The issued schemes are `KIDS_GOAL_CATEGORY`, `LANGUAGE`, `KIDS_RESOURCE_SUBCATEGORY` and `AGE_GROUP`. Source values are transported as versioned items and aliases. Unknown values create draft proposals; publication requires a published exact version. Trim is permitted only to form an alias lookup key and never rewrites raw source text.

## Statistical contract

Dataflow `KIDS_FILES_STATISTICS` uses DSD `KIDS_FILES_STATISTICS_DSD` revision 1 with observation grain `carrier_code + cell_ordinal`.

| Order | Component | Role | Source/binding |
|---:|---|---|---|
| 1 | `TIME_PERIOD` | dimension | `period_raw` → optional normalized year |
| 2 | `AGE_GROUP` | classified dimension | raw key → versioned classifier item |
| 3 | `OBS_VALUE` | primary measure | lexical value → optional decimal parse; registry measure `KIDS_OBS_VALUE_INPUT` |
| 4–7 | unit, multiplier, decimals, aggregation | series attributes | mandatory Control-Plane bindings; aggregation defaults to `NONE` |
| 8–9 | observation/confidentiality status | observation attributes | mandatory Control-Plane policies |
| 10–12 | carrier, cell key, JSON path | lineage attributes | copied from Access input |

No statistical observation is seeded by migration. Legacy `KIDS_FILE_*` metrics created from a fixed ID list are marked `SUPERSEDED`. The 43 carriers and 880 input cells in Access are derived from actual parseable `files.chartdata`; they remain `DRAFT` until metric, unit, aggregation, classifier aliases, quality and confidentiality are approved.

## Raw-data boundary

The immutable source Access artifact is the sole raw-payload authority. `__raw_document` is a row locator. `kids_statistical_carrier` is a carrier locator plus checksum; it has no `payload_raw` column. The parsed input retains the minimal lexical values and JSON path required for lossless interpretation and audit, without copying the full raw JSON document.

## Blocking gates

Publication is denied unless all seven gates pass: exact declared schema, key uniqueness, resolved required relations, published classifiers, complete statistical semantics, immutable raw lineage and atomic publication with rollback pointer. `ingestion_contract` therefore remains `REVIEW_REQUIRED`, `auto_publish = 0`.

## Runtime inclusion

The migration is registered immediately after revision 5 in both `PlatformSchemaMigrationRunner` and `scripts/platform-bootstrap.ps1`. Application startup or the bootstrap script applies it once and records its SHA-256 in `platform.schema_migration`; checksum drift is rejected.
