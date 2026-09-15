# Kids statistical contract closure

## Evidence lock

Read-only profiling confirms that `files.chartdata` has 225 source rows. Exhaustive parsing recognizes 43 arrays across the direct JSON and escaped JSON-text encodings; the other 182 values are blank, non-array, or unparseable under this contract. Their periods and dynamic field shapes are retained exactly in the canonical carrier/input tables.

The legacy Access representation may be either direct JSON array text or escaped JSON array text (for example `[{\"year\":\"2020\"}]`). The immutable raw value is retained exactly as received. Only the reviewed `STATISTICAL_WIDE_JSON` projection decodes the escaped representation, then applies the same array/period/dimension validation; it never rewrites the source value.

The observed non-period source keys are `0-12 `, `0-5`, `0-17`, `3-17`, `13-17`, `15-17`, `15-24`, and `15-29`. The trailing space in `0-12 ` is source evidence, not a distinct canonical code: raw payload/key remains unchanged; the lookup candidate is trimmed and then must resolve to an approved age-band alias.

## SDMX-compatible target

The target is a versioned DSD-like structure, not a chart table:

| SDMX role | Kids source | Canonical platform role | Release rule |
|---|---|---|---|
| Dataflow | approved statistical carrier file family | `KIDS_FILES_STATISTICS` | DRAFT until methodology approval |
| Observation key | file source ID + period + age band | raw lineage plus `statistics.series` / `statistics.observation_dimension` | all key components required |
| `TIME_PERIOD` dimension | `year` | observation period | ISO year validation |
| `AGE_GROUP` dimension | JSON dynamic key | versioned classification alias | exact raw key retained; canonical alias required |
| `GOAL_CATEGORY` dimension | `files.category` | `KIDS_GOAL_CATEGORY` classification | proven reference join |
| Measure | JSON numeric value | `statistics.observation.numeric_value` | metric, unit and aggregation required |
| `OBS_STATUS`, `UNIT_MEASURE`, `DECIMALS`, confidentiality/quality attributes | no reliable source value | Core metadata / release attributes | must be explicitly set by steward; never inferred |

Each numeric cell becomes precisely one observation at this grain:

```text
source file ID × reference year × approved age group × approved metric
```

No chart definition stores numerical data. A visualization selects an approved dataflow/metric and published snapshot only.

## Access v3 consequence

The generated Access contract template must contain a `KIDS_FILE_RESOURCE` source table declaration and a DRAFT `STATISTICAL_WIDE_JSON` projection. The projection has no metric/unit values embedded by an author. It binds only to the Control-Plane-issued DSD revision and declares:

```json
{
  "projection_family": "STATISTICAL_WIDE_JSON",
  "dataflow_code": "KIDS_FILES_STATISTICS",
  "dsd_revision": 1,
  "array_path": "chartdata",
  "period_field": "year",
  "pivot_dimension": "AGE_GROUP",
  "raw_key_normalization": "TRIM_FOR_ALIAS_LOOKUP_ONLY",
  "approval_state": "DRAFT"
}
```

The import engine rejects a cell when its source file has no approved metric binding, its key has no published `AGE_GROUP` alias, its period/value is invalid, or the resulting observation key is duplicated. Rejected cells keep raw lineage and quarantine evidence.

## Blocking decisions

The following are explicit, finite decisions—not unspecified implementation work:

1. Metric title, statistical concept, unit, aggregation and confidentiality for each of the 43 source carriers.
2. Published `AGE_GROUP` codelist/version and aliases for all eight observed source keys.
3. `sub_category` codelist/crosswalk, including multi-value token semantics.
4. Meaning of glossary language code `ena`.

Until these decisions are approved, the contract can ingest raw evidence and non-statistical entities but cannot materialize or publish Kids statistical observations.
