# Final KIDS canonical model

## Dataset register

| Dataset code | Family | Grain | Source identity | Lifecycle |
|---|---|---|---|---|
| `__CL_*` / `KIDS_GOAL_CATEGORY` | REFERENCE | one versioned classifier item/alias | `goals_titles.ID` | classifier proposal/snapshot; not a duplicate content entity |
| `KIDS_GOAL` | ENTITY | one goal | `goals.ID` | canonical entity |
| `KIDS_RESOURCE` | ENTITY | one resource/file | `files.ID` | canonical entity |
| `KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT` | RELATION | one resource × source token | `files.ID + token` | conditional, DRAFT until codelist review |
| `KIDS_GLOSSARY_ENTRY` | ENTITY | one source entry | `glossary.ID` | canonical entity |
| `KIDS_RAW_DOCUMENT` | RAW | immutable artifact envelope plus source-row locator | `source_table + source_pk` | immutable-artifact lineage/required |
| `KIDS_STATISTICAL_CARRIER` | RAW | one locator/checksum for a JSON-bearing resource | `files.ID` | conditional/DRAFT |
| `KIDS_STATISTICAL_INPUT` | STATISTICAL | carrier × source cell ordinal | `carrier_code + cell_ordinal` | conditional/DRAFT |
| `KIDS_STATISTICAL_UNIT` | REFERENCE | one governed unit | `unit_code` | revision-7 approved inference |
| `KIDS_STATISTICAL_METRIC` | REFERENCE | one resource-bound metric | `metric_code` | revision-7 approved inference |
| `KIDS_STATISTICAL_SEMANTIC_BINDING` | RELATION | one carrier × semantic binding | `carrier_code` | revision-7 approved inference |

## Keys and declared relations

```text
__CL_ITEM(KIDS_GOAL_CATEGORY).item_code           unique per version
KIDS_GOAL.source_goal_id                          unique
KIDS_RESOURCE.source_resource_id                  unique
KIDS_GLOSSARY_ENTRY.source_glossary_id            unique
KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT
  (source_resource_id, raw_subcategory_token)     unique
KIDS_STATISTICAL_CARRIER.source_resource_id       unique
KIDS_STATISTICAL_INPUT
  (source_resource_id, period_raw, age_group_raw_code) unique
```

```text
KIDS_GOAL.goal_category_code       N:1  __CL_ITEM(KIDS_GOAL_CATEGORY) required
KIDS_RESOURCE.goal_category_code   N:1  __CL_ITEM(KIDS_GOAL_CATEGORY) required
KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT.source_resource_id
                                    N:1  KIDS_RESOURCE        required
KIDS_STATISTICAL_CARRIER.source_resource_id
                                    1:1  KIDS_RESOURCE        required
KIDS_STATISTICAL_INPUT.carrier_key N:1  KIDS_STATISTICAL_CARRIER required
all canonical records              N:1  KIDS_RAW_DOCUMENT    required lineage
```

No `KIDS_RESOURCE → KIDS_GOAL` relation exists: shared category does not prove a direct relationship. No glossary translation relation exists: source contains no translation-group identity.

## Required source fields and transformations

| Source | Canonical treatment |
|---|---|
| `goals_titles.ID/category/title_geo/title_eng` | `__cl_item`/`__cl_alias` for the `KIDS_GOAL_CATEGORY` version; no duplicate content entity |
| `goals.ID/category/title_*/path_*` | entity identity, category assignment, typed localized text and typed locator |
| `files.ID/category/title_*/path_*` | resource identity, category assignment, typed localized text and typed locator |
| `files.sub_category` | immutable raw value plus split/trim/deduplicated assignment tokens; no classifier meaning before approval |
| `files.chartdata` | bytes remain in the immutable source artifact; canonical Access stores a locator/checksum and conditionally extracts input cells after structural parse |
| `glossary.ID/lang/text` | independent entry, source language and content; no invented translation group |

## Integrity rules

1. Every `goals.category` and `files.category` must resolve to a supplied `KIDS_GOAL_CATEGORY` code before semantic publication.
2. Title/locator language codes are explicit BCP-47 aliases (`ka`, `en`); absent values are quality findings, not fabricated strings.
3. `sub_category` token splitting preserves source order/value and rejects duplicate tokens per resource.
4. A `KIDS_STATISTICAL_CARRIER` exists only if its raw payload parses as a JSON array; unparseable content stays raw only.
5. A statistical input cell retains original period, original dimension key and original numeric lexical representation in lineage.
6. Dataset deletion is expressed by a contract-defined tombstone record, never inferred from an omitted snapshot row unless snapshot mode is explicitly declared.

## Canonical package shape (readable, non-EAV)

The package contains explicit, typed logical tables.  It does **not** use a generic key/value table to absorb an unknown site.

| Logical table | Required canonical columns | Notes |
|---|---|---|
| `kids_goal` | `source_goal_id`, `category_scheme`, `category_version`, `category_code`, `title_ka`, `title_en`, `path_ka`, `path_en`, `source_row_key` | one row per goal; absent localized value remains null |
| `kids_resource` | `source_resource_id`, category reference, typed localized title/path, `source_row_key` | one row per resource; no inferred goal FK |
| `kids_resource_subcategory_assignment` | `source_resource_id`, `scheme_code`, `version_code`, `item_code_or_proposed_token`, `source_token_raw`, `ordinal`, `source_row_key` | explicit N:M; the raw source string remains in raw lineage |
| `kids_glossary_entry` | `source_glossary_id`, `language_scheme`, `language_version`, `language_code_or_proposal`, `entry_text`, `source_row_key` | independent entries, not translations unless a real group key arrives |
| `kids_statistical_carrier` | `carrier_code`, `source_resource_id`, `payload_checksum`, `parse_status`, `source_row_key`, `operation` | locator/checksum only; full chart JSON is not duplicated and is never an observation itself |
| `kids_statistical_input` | `carrier_code`, `cell_ordinal`, `period_raw`, `period_normalized`, `dimension_key_raw`, `age_group_code_or_proposal`, `value_lexical`, `value_decimal`, `source_row_key` | one parsed numeric cell; all source representations remain visible |
| `__stat_unit`, `__stat_metric` | governed unit definitions and one resource-bound metric per carrier | revision 7 stores confidence and rationale without changing observations |
| `kids_statistical_semantic_binding` | `carrier_code`, `metric_code`, `unit_code`, `aggregation`, `obs_status`, `conf_status`, quality/confidentiality policy codes | one explicit semantic contract per carrier; aggregation is `NONE` because age bands overlap |
| `__cl_scheme`, `__cl_version`, `__cl_item`, `__cl_alias`, `__cl_hierarchy` | as specified by the classifier contract | classifier snapshots and proposals carried with the package |
| `__raw_document` | complete 22-field artifact envelope, including locator, checksum, provenance, governance and parser state | stable locator/envelope for the immutable original Access artifact; it never duplicates generated row JSON or a raw payload |

The `__` prefix marks package-control datasets, not an ungoverned extension namespace. Names, columns, primary keys and their semantic type are fixed by the issued contract revision.

## Cardinality, nullability and lifecycle

- Exactly one raw-document row exists for each source row. Canonical rows reference it by `source_row_key`; source-originated rows cannot be orphaned.
- A goal/resource has exactly one category assignment in KIDS v1. A category can classify zero to many goals and resources.
- A resource has zero to many subcategory assignments; the source’s comma-separated field is an import representation, never the canonical relation.
- A resource has zero or one statistical carrier. A carrier has zero to many parsed input cells. A cell has exactly one carrier.
- A glossary entry has exactly one source-language value. It may have no approved `LANGUAGE` classifier item, in which case its proposal remains quarantined from semantic publication.
- `DRAFT` and `UNRESOLVED` facts are raw-retainable and reviewable but are not eligible for semantic publication. `APPROVED` mappings create a new immutable semantic projection; they do not overwrite input facts.

## Explicit KIDS v1 decisions

1. `goals_titles` is transformed into the `KIDS_GOAL_CATEGORY` classifier scope, not retained as a second domain entity.
2. `goals` and `files` remain separate entities because their source identity, lifecycle and content role differ.
3. `files.sub_category` is split only by the declared delimiter contract; token meanings are proposals until a codelist owner approves them.
4. `files.chartdata` is kept exactly as received only in the immutable source artifact. Where structurally parseable, it produces a locator/checksum carrier plus typed input cells. Parsing success does not claim a metric, unit, aggregation or official statistical status.
5. `glossary.ena` is carried as an explicit language proposal/quality exception. It is never silently changed to `en`.
