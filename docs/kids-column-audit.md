# KIDS canonical column audit (R8)

**Artifact audited:** `platform/apps/geostat/backend/api/kids-portal-v1-canonical-r8-final.accdb`  
**Result:** `PRODUCTION_ACCEPTED` — 25 governed tables, 21 enforced relationships, every table indexed, and no duplicate column names.

## Decision rule

No column is removed merely because its value resembles another column. A column is retained when it represents a different semantic layer: source evidence, normalized identity, typed measure, lineage, governance, or transport idempotency. The Access package is a transport/staging contract; published canonical facts are materialized into the platform `raw`, `entity`, `statistics`, `classification`, and `link` schemas.

## Entity and relation tables

| Physical table | Column groups | Verdict |
|---|---|---|
| `__ent_kids_goal` | stable source identity; classifier reference; localized title/path; lineage; operation | Keep all. `title_ka/title_en` are source-facing localized projections, not a duplicate of the canonical localization table. |
| `__ent_kids_resource` | stable source identity; category; localized title/path; lineage; operation | Keep all. The resource identity is independent from its statistical carrier. |
| `__rel_kids_resource_subcategory_assignment` | relation identity; both endpoints; raw token; ordinal; lineage; operation | Keep all. `source_token_raw` preserves evidence while `subcategory_item_ref` is the governed classifier identity; `ordinal` preserves source order. |
| `__ent_kids_glossary_entry` | entry identity; governed language; raw language token; content; lineage; operation | Keep all. `language_raw` is deliberately distinct from the normalized classifier reference. |

## Raw tables

| Physical table | Purpose | Verdict |
|---|---|---|
| `__raw_document` | immutable artifact envelope and source-row locator | Expanded to the complete envelope: source system/feed, document identity, filename, MIME, byte size, SHA-256 checksum, received time, URI, ingestion batch, provenance, retention, confidentiality, payload/encryption references, parser/validation state, and supersession pointer. No raw payload is duplicated here; the immutable artifact remains the source of truth. |
| `__raw_kids_statistical_carrier` | one resource-bound chart/JSON carrier locator | Keep `payload_checksum`, parse state, and source lineage. Payload bytes remain in the artifact/archive; this table must not become a second payload store. |

## Statistical tables

| Physical table | Column groups | Verdict |
|---|---|---|
| `__stat_kids_statistical_input` | physical `input_key`; natural grain (`carrier_code`, `cell_ordinal`); raw and normalized period; raw and governed dimension; lexical and typed value; JSON location/encoding; lineage; operation | Keep all. The apparent pairs are intentional: raw-vs-governed and lossless-vs-typed representations are required for replay, audit, and safe publication. `input_key` is technical Access identity only; semantic grain is explicitly declared as `carrier_code + cell_ordinal`. |
| `__rel_kids_statistical_semantic_binding` | carrier-to-metric/unit relation; aggregation/status; quality/confidentiality policies; inference method; operation | Keep all. This is a governed semantic relation, not a duplicate of the input cell. |
| `__stat_metric` / `__stat_unit` | metric/unit registry, denominator/scale, inference evidence | Keep all. These are reusable reference entities and are linked by enforced foreign keys. |

## Canonical invariants now enforced

- Every physical table has a strict primary key and appropriate unique natural indexes.
- Every physical table is represented by `__gs_dataset` and every column by `__gs_field`; R8 has 122 declared fields.
- Statistical input keys are `PRIMARY(input_key)` plus `NATURAL(carrier_code,1)` and `NATURAL(cell_ordinal,2)`.
- Raw-document envelope columns are declared in the contract, not hidden implementation details.
- Required columns are checked by the production audit; duplicate column names fail the audit.
- Raw source tokens, lexical values, and JSON paths are never silently overwritten by normalized/typed values.
- Publication code consumes governed metadata and relations; it does not branch on KIDS table names.

## Measured R8 artifact

`__raw_document` 456 rows/22 columns; `__ent_kids_goal` 36/8; `__ent_kids_resource` 225/8; resource-subcategory relation 230/7; glossary 178/6; raw carriers 43/6; statistical input 880/13; semantic bindings 43/10. All passed the read-only structural audit.
