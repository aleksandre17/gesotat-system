# KIDS statistical semantics — revision 7

Revision 7 records the user's delegated business-owner authorization of evidence-based semantic inference on 2026-09-10. It preserves revision 6 and all 880 source observation values unchanged.

## Decision method

Each of the 43 parseable carriers was profiled from its Georgian/English resource title, workbook path, periods, age keys, numeric range and integer/decimal shape. Every decision is stored with `TITLE_RANGE_STANDARD_INFERENCE`, confidence and rationale. Metric codes are stable semantic identities—not observation data or hard-coded chart values.

## Unit decisions

| Carrier group | Unit |
|---|---|
| abortions and suicides | `COUNT_EVENT` |
| disease incidence/case resources | `COUNT_CASE` |
| beneficiaries, children, registered persons and IDPs | `PERSON` |
| labour-force population and resident visitors explicitly reported in thousands | `THOUSAND_PERSONS` |
| domestic visits explicitly reported in thousands | `THOUSAND_VISITS` |
| average nights per visit | `AVERAGE_NIGHTS` |
| maternal mortality | `PER_100000_LIVE_BIRTHS` |
| HIV incidence | `PER_1000_UNINFECTED_POPULATION` |
| suicide mortality | `PER_100000_POPULATION` |
| prevalence, shares, literacy and early-leaver rate | `PERCENT` |

`KIDS_LABOUR_FORCE_POPULATION` has the lowest confidence (`0.85`) because its English title omits the displayed unit; workbook context and value magnitude support thousand persons. All other bindings have confidence `0.95–0.99`.

## Aggregation

All metrics use `NONE`. The observed age groups overlap (`0–17`, `15–24`, `15–29`, `15+`, etc.), so summing or averaging cells across age bands would double-count or distort the indicator. Any future aggregation requires a new DSD/contract revision with mutually exclusive dimensions or an explicit formula.

## Quality policy

`KIDS_AGGREGATE_QUALITY_V1` requires:

- four-digit period and a published age-group alias;
- finite numeric value with minimum zero;
- percent values within 0–100;
- integer expectation for count units;
- uniqueness at carrier × period × age-group grain;
- complete source lineage;
- rejection of cross-age aggregation.

## Confidentiality policy

`KIDS_PUBLIC_AGGREGATE_V1` classifies the supplied cells as public aggregates with no microdata or direct identifier. Source publication is preserved. Counts below five are flagged for review but are not silently changed or suppressed because the supplied public source already publishes them. DSD defaults are `OBS_STATUS=A` and `CONF_STATUS=F`.

## Physical and Control-Plane representation

- `__stat_unit`: ten governed units.
- `__stat_metric`: 43 resource-bound metrics with source titles, unit, confidence and rationale.
- `kids_statistical_semantic_binding`: 43 carrier-to-metric/unit/policy bindings.
- `platform.metric_semantic_profile` and `platform.statistical_semantic_binding`: authoritative Control-Plane equivalents.
- `023_kids_inferred_statistical_semantics.sql`: DSD revision 2 and complete site/ingestion contract revision 7.

## R8 input-cell column doctrine

`__stat_kids_statistical_input` is a lossless source-to-semantic transport table, not the published fact table. Its apparently repeated pairs are different representations with different authority:

| Columns | Role | Why both are retained |
|---|---|---|
| `input_key` | physical primary identifier | deterministic Access row identity |
| `carrier_code` + `cell_ordinal` | semantic natural grain | the SDMX-compatible DSD key; explicitly declared as `NATURAL(1,2)` in R8 |
| `period_raw` / `period_normalized` | source lexeme / normalized period | raw evidence is immutable; normalized value is queryable |
| `dimension_key_raw` / `age_group_item_ref` | source token / resolved classifier reference | preserves original token while linking the governed vocabulary |
| `value_lexical` / `value_decimal` | lossless lexical value / typed measure | failed or ambiguous parses remain auditable and never become fabricated numbers |
| `json_path`, `source_encoding`, `source_row_key`, `operation` | lineage and replay controls | identify the exact source location and transformation |

The published canonical representation remains `statistics.series`, `statistics.observation` and `statistics.observation_dimension`; the Access input table is intentionally not collapsed into that model. R8 changes only the declaration of roles and keys: `input_key` is the physical primary identifier, while `carrier_code + cell_ordinal` is the governed natural grain. No information is discarded and no semantic duplicate is treated as an independent measure.
