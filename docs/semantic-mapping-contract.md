# Semantic mapping contract

`platform.contract_source.mapping_spec_json` is a reviewed, versioned contract. It is metadata, not executable SQL. A source can therefore add fields or tables without creating a new platform table.

## Entity

```json
{
  "family": "ENTITY",
  "recordType": "CHILD_SERVICE",
  "key": "id",
  "title": "name",
  "classifications": [{
    "attributeId": 501,
    "externalSystemCode": "KIDS_CATEGORY",
    "code": "category_code"
  }]
}
```

The original document is retained in `raw.source_record`; the canonical entity is in `entity.entity_record`; controlled vocabulary values resolve through Core's `classification_alias` to `entity.entity_classification`.

## Relation

```json
{"family":"RELATION","relationshipTypeId":41,"fromKey":"goal_id","toKey":"file_id"}
```

Both endpoints must resolve within the same immutable dataset snapshot. An unresolved endpoint rejects the materialization; it is never silently dropped.

## Statistical observation

```json
{"family":"STATISTICAL","metricId":91,"seriesKey":"municipality_code","periodStart":"period_start","value":"count","dimensions":[{"dimensionId":12,"externalSystemCode":"MUNICIPALITY","code":"municipality_code"}]}
```

The engine writes named SDMX-style structures: `statistics.series`, `statistics.observation` and `statistics.observation_dimension`. Dimensions remain separately governed metadata rather than chart-specific data copies.

## Wide JSON statistical observation

Some legacy sources contain a reviewed JSON array where object keys are dynamic dimension codes. It is normalized only through pre-approved aliases:

```json
{
  "family": "STATISTICAL_WIDE_JSON",
  "arrayPath": "chartdata",
  "periodField": "year",
  "metric": {"externalSystemCode": "KIDS_FILE_METRIC", "code": "id"},
  "seriesKey": "id",
  "pivotDimension": {"dimensionId": 12, "externalSystemCode": "KIDS_AGE_GROUP"}
}
```

The metric code resolves through `platform.metric_alias`; a dynamic key resolves through `platform.classification_alias`. The engine rejects an unknown metric, unknown dimension code, invalid period/value, or duplicate metric/series/period/dimension tuple. It never creates metrics, dimensions or classifications from source data.

## Multiple projections from one source

One immutable raw source may legitimately produce multiple typed projections. Use a reviewed projection list rather than importing the source twice:

```json
{"projections":[
  {"family":"ENTITY","recordType":"KIDS_FILE_RESOURCE","key":"id"},
  {"family":"STATISTICAL_WIDE_JSON","arrayPath":"chartdata","periodField":"year","metric":{"externalSystemCode":"KIDS_FILE_METRIC","code":"id"},"pivotDimension":{"dimensionId":12,"externalSystemCode":"KIDS_AGE_GROUP"}}
]}
```

Raw lineage remains one source record. Entity and statistical projections are typed canonical representations, not duplicate chart data.

## Geography

```json
{"family":"GEO","featureKey":"id","geometry":"geometry","geometryType":"geometry_type"}
```

Geometry is persisted as GeoJSON in `geo.feature`; chart definitions reference published semantic data and do not duplicate it.

## Lifecycle

`STAGING → VALIDATED → REVIEW_REQUIRED → SEMANTIC_REVIEW → PUBLISHED`.

Only a reviewer can call publication. A contract cannot execute arbitrary DDL, connection strings, SQL expressions, or auto-create classifiers.
