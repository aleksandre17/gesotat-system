# Kids portal — canonical semantic onboarding specification

**Status:** mandatory design gate before the first Kids publication.  
**Rule:** importing Kids does not reproduce legacy tables. Every source row is retained unchanged in `raw.source_record`, then materialized only into governed canonical structures.

## Evidence-based source profile

Read-only profiling of `kids.dbo` established the following source facts:

| Source | Rows | Facts | Canonical role |
|---|---:|---|---|
| `goals_titles` | 17 | `ID`, category, Georgian/English titles | controlled classification vocabulary: `KIDS_GOAL_CATEGORY` |
| `goals` | 36 | `category` resolves to `goals_titles.ID` for all rows | governed content entity, classified by goal category |
| `files` | 225 | `category` resolves to `goals_titles.ID` for all rows; bilingual labels, paths and `chartdata` | content/resource entity, classified by goal category; possible statistical carrier |
| `glossary` | 178 | language and text, no stable translation-group key | glossary entity, classified by normalized language; no invented translation relation |

`files.chartdata` is not one homogeneous field. Exhaustive parsing finds 43 array carriers across direct JSON and escaped JSON-text encodings; their structure is a time column (`year`) with dynamic age-band columns such as `0-17`, `15-24`, and `15-29`. The other 182 rows are blank, non-array, or unparseable as this contract. Therefore no generic `chartdata` table or chart-value copy is permitted.

## Canonical target model

```text
Kids SQL source
  → immutable raw.source_record (every original row, including chartdata)
  → governed semantic projections
       ├─ classification scheme/version/item: KIDS_GOAL_CATEGORY
       ├─ entity.entity_record: KIDS_GOAL, KIDS_FILE_RESOURCE, KIDS_GLOSSARY_ENTRY
       ├─ entity.entity_classification: category/language assignments
       ├─ entity.entity_link: only proven source relationships
       └─ statistics.series / observation / observation_dimension
          (only valid chartdata arrays after explicit semantic review)
```

This is a normalized, readable relational model. JSON remains only where it is the immutable source payload or legitimately variable content; it is not used as a key/value substitute for governed statistical or classification concepts.

## Non-statistical model: optimize by business grain, not by legacy table shape

The four legacy tables must not be indiscriminately merged just because several share `title_geo`, `title_eng` and `path_*`. Their business grain differs. They also must not receive separate physical tables: logical datasets are catalog entries, while their records use the shared `entity` family.

| Logical dataset / role | Source grain | Canonical storage | Why this boundary is correct |
|---|---|---|---|
| `KIDS_GOAL_CATEGORY` | one approved category code/label | Core `classification_scheme/version/item` | it is a controlled vocabulary used by multiple content types, not ordinary portal content |
| `KIDS_GOAL` | one goal (`goals.ID`) | `entity.entity_record`, record type `KIDS_GOAL` | goal has its own lifecycle and meaning; it is not a file/resource |
| `KIDS_FILE_RESOURCE` | one resource (`files.ID`) | `entity.entity_record`, record type `KIDS_FILE_RESOURCE` | resources have paths, optional chart payloads and different retention/query needs |
| `KIDS_GLOSSARY_ENTRY` | one source glossary entry (`glossary.ID`) | `entity.entity_record`, record type `KIDS_GLOSSARY_ENTRY` | no reliable cross-language grouping key exists, so independent entries are the truthful grain |
| multilingual title/body | one entity × semantic field × BCP-47 language | `entity.localized_text` | Georgian and English are queryable and governed, without a site-specific `_geo`/`_eng` column set |
| localized navigation/resource path | one entity × locator kind × BCP-47 language | `entity.resource_locator` | path is a typed locator, not an opaque JSON property or a duplicated entity |

`entity.entity_record.title` is a compact default/display projection only. The authoritative multilingual representation is `entity.localized_text`; source payload remains the immutable provenance record.

### Actual cardinality findings and their consequences

- `goals.category` has 12 distinct values; `files.category` has 10; both resolve to the known `goals_titles.ID` set. It is therefore one reusable category classification, not copied text and not N×M entity links.
- `files.sub_category` has five raw encodings: `1`, `2`, `3`, `1,3`, `1,4`. It is a multi-valued encoded field, not a scalar attribute. After steward-approved code meaning is available, split comma-separated values, trim/deduplicate them and create one classification assignment per code. The original string remains raw lineage.
- No source lookup table or declared foreign keys exist for sub-category codes. Until an approved external code list/crosswalk exists, `sub_category` stays raw and receives a quality warning; no fictional classification labels may be created.
- `glossary.lang` contains `ka`, `en`, and one `ena`. Map only approved aliases (`ka` → `ka`, `en` → `en`) to the BCP-47/ISO language scheme. `ena` is a semantic-review exception, not an automatic correction.
- Source titles are unique within `files`, and paths are populated. This supports stable `files.ID` entity keys and two typed localized locators; it does **not** prove a direct file-to-goal relation.

### Relations: represent facts, avoid synthetic graph noise

`goals` and `files` share membership in `KIDS_GOAL_CATEGORY`. This is represented by `entity.entity_classification`. Do not generate a pairwise `file → goal` link merely because two records have the same category: it would create false semantics and a potentially quadratic graph. A direct `entity.entity_link` is allowed only when a future source field or approved crosswalk explicitly identifies both endpoints and a named relationship type.

## Required controlled vocabularies

Before activation, create and review the following in Core metadata:

1. `KIDS_GOAL_CATEGORY` — source code is `goals_titles.ID`; Georgian and English titles become versioned item labels. It is the authoritative resolution target for `goals.category` and `files.category`.
2. `LANGUAGE` — source values in `glossary.lang` must resolve through aliases to an ISO 639 language scheme. Unknown values reject the affected semantic projection; they are never silently treated as free text classification.
3. `AGE_GROUP` — every dynamic chart column must be explicitly mapped to a published age-group item/version. A string such as `15-24` is not itself an approved classifier until the steward maps it.
4. Where `files.sub_category` represents a controlled vocabulary, create a named scheme after its value profile and business meaning are approved. Until then it remains source payload only; it must not be falsely promoted to a classifier.

## Proven relationships

The only currently proven relational join is:

```text
goals.category        → goals_titles.ID
files.category        → goals_titles.ID
```

It is represented as classification assignment, not as duplicated category text. If a product owner requires navigation/ownership graph semantics as well, define one named relationship type and generate `entity.entity_link` from the same proven keys. No relation between `files` and `goals`, or between glossary translations, may be invented until a stable source key or approved crosswalk is supplied.

## Statistical normalization rule for `chartdata`

For each approved valid JSON chart carrier, flatten using this grain:

```text
one observation = one source file × one period × one age group × one metric
```

| Source component | Canonical concept |
|---|---|
| `files.ID` | lineage key and approved indicator/metric registration key |
| `title_geo` / `title_eng` | multilingual metric/catalog labels; never numeric data duplicates |
| array item `year` | `TIME_PERIOD` / observation period |
| dynamic property name | `AGE_GROUP` classification alias |
| dynamic property value | `statistics.observation.numeric_value` after numeric validation |
| `files.category` | category dimension or attached resource classification, as formally approved |

Each approved indicator is a metadata `metric`, not a physical table. Each visualization references the metric/dimensions and published observations; it never stores another copy of numerical values. Null, nonnumeric, unknown age band, duplicate period/dimension tuple, or unregistered indicator moves the source item to semantic review/rejection with lineage preserved.

The implementation uses `platform.metric_alias` for `files.ID` → approved metric resolution and `platform.classification_alias` for age-band code → approved `AGE_GROUP` resolution. It accepts one source mapping with an explicit `projections` list, so `files` materializes both a resource entity and its approved observations without reading or copying the legacy table a second time.

## Required semantic projections

One physical source may have more than one semantic projection. This is intentional and is not duplicate data: raw remains the source truth; projections are typed governed views.

| Source | Projection | Target | Status before first import |
|---|---|---|---|
| `goals_titles` | category vocabulary loader | Core classification scheme/item/version | steward review required |
| `goals` | goal content | entity + category assignment | mapping review required |
| `files` | file/content resource | entity + category assignment | mapping review required |
| `files` where `chartdata` is approved JSON | wide-JSON statistical normalizer | statistics series/observation/dimensions | implementation + metric/classifier review required |
| `glossary` | glossary content | entity + language assignment | language alias review required |

The current `KIDS_CONTENT` seed is therefore a safe registration baseline, **not** the final semantic contract. It must remain `REVIEW_REQUIRED` until the projection set is approved and implemented.

## Acceptance gates

1. Profile every distinct `sub_category`, `lang`, chart JSON key and chart row shape.
2. Steward approves classification versions, aliases, relationship semantics, metric catalogue and units.
3. Implement the governed wide-JSON statistical projection; it must not dynamically create tables, metrics or classifications during import.
4. Run import in staging; validate technical, referential and semantic rules.
5. Compare source counts/checksums; review rejected items and lineage.
6. Publish an immutable snapshot only after reviewer approval; verify read visibility and rollback.

## Non-negotiable constraints

- No per-chart, per-Kids-table or per-site physical business table.
- No direct use of legacy `chartdata` as a serving/chart data source.
- No automatic classifier or relation creation from unreviewed values.
- No overwriting published observations; correction is a new snapshot/release.
- No write to `kids.dbo`; all source access remains read-only.
