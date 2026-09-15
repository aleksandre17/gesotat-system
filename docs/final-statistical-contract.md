# Final statistical contract — KIDS input to approved observation

## The non-negotiable separation

`files.chartdata` is a source **carrier**. Its parsed JSON records are **statistical input cells**. Neither is an official statistical observation. An approved observation is created only when a versioned SDMX-compatible data-structure definition (DSD), classifier mappings, metric, unit, aggregation and disclosure/quality policy have all been approved.

```text
immutable raw resource row
  → KIDS_STATISTICAL_CARRIER (payload, checksum, parse result)
  → KIDS_STATISTICAL_INPUT (one source cell, typed but unapproved)
  → resolved DSD coordinates + approved semantics
  → immutable statistical observation snapshot
  → chart definition reads an observation snapshot; it never stores measures
```

## Required DSD objects

The Control Plane owns versioned SDMX-compatible objects. The Access package may carry a DRAFT proposal, but cannot approve one.

| Object | KIDS v1 responsibility |
|---|---|
| Dataflow | `KIDS_FILES_STATISTICS`; identifies the statistical collection, provider and release policy |
| DSD | `KIDS_FILES_STATISTICS_DSD` with revision, validity interval and component order |
| Dimension | `TIME_PERIOD`, `AGE_GROUP`, and any carrier-scoped dimension explicitly discovered in JSON; dimensions use versioned classifier references |
| Primary measure | `OBS_VALUE`; decimal lexical input is retained independently from parsed numeric value |
| Attributes | `UNIT_MEASURE`, `UNIT_MULTIPLIER`, `DECIMALS`, `OBS_STATUS`, `CONF_STATUS`, `SOURCE_CARRIER`, `SOURCE_CELL_KEY`, `PROVISIONAL_FLAG` |
| Constraints | allowed component values/series combinations; no unapproved source key becomes a hidden dimension |
| Codelists | `AGE_GROUP`, time representation, units, status and confidentiality schemes, all revisioned |

## Input-cell contract

`kids_statistical_input` has the following fixed grain:

`(carrier_code, cell_ordinal)` — unique and immutable.

It carries `period_raw`, normalized ISO period only when deterministically parseable, `dimension_key_raw`, resolved/proposed `AGE_GROUP` code, `value_lexical`, optional `value_decimal`, JSON path/source encoding and raw-row lineage. `period_raw` and `dimension_key_raw` are never discarded.

The current KIDS source contains age-like keys such as `0-5`, `0-12`, `0-17`, `3-17`, `13-17`, `15-17`, `15-24`, `15-29`, including one `0-12 ` trailing-space variant. The contract permits trim **only for declared alias lookup**; it preserves the raw key and records the alias rule. The year-like values 2017–2025 are input periods, not automatically publication time periods.

## Approval gate

For each carrier family, an approver must choose or approve:

1. metric/concept and measure semantics;
2. unit and multiplier;
3. aggregation method and population/denominator where relevant;
4. the DSD and each dimension/codelist version;
5. parse transformation and quality/disclosure attributes;
6. whether the data is publishable, provisional, restricted or raw-only.

Until all required choices resolve, the carrier and input cells are valid evidence but must remain `DRAFT`/`SEMANTIC_REVIEW`; publication is blocked. This prevents a chart’s title or a JSON property name from being mistaken for official metric semantics.

## Observation identity and revisions

An approved observation is identified by `(dataflow, dsd_revision, ordered dimension coordinates, measure, publication_snapshot)`. A correction creates a new observation/snapshot revision and supersedes the old release; it never updates published values in place. Chart definitions reference `publication_snapshot` plus a query/specification, allowing one observation set to power multiple charts without data duplication.
