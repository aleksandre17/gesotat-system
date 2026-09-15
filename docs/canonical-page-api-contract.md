# Canonical page API (backend contract)

UI is intentionally out of scope. The server exposes the approved `KIDS_PORTAL_V1` revision through a stable page identity and never accepts table names or SQL from a caller.

`GET /api/v1/platform/pages/{pageId}/data` requires `READ_RESOURCE`. Supported KIDS page IDs are 7 (root), 8 (goals), 9 (resources), 10 (glossary), 11 (statistics), and 12 (classifiers). The resolver selects the highest `APPROVED` revision from `platform.site_contract_revision`, then resolves the node and its dataset. A missing or non-approved contract is an error; no legacy `dbo.page_nodes` contract is inferred.

Common query parameters: `page`, `limit` (capped at 1000). Statistics additionally accepts `metricCode`, `carrierCode`, `periodFrom`, `periodTo`, and `ageGroup`; dates are ISO `yyyy-MM-dd`. All values are bound parameters and are validated before execution.

The contract-generic query endpoint is `POST /api/v1/platform/contracts/{contractCode}/pages/{pageId}/query` (also requires `READ_RESOURCE`). Its JSON body is:

```json
{"filters":{"resourceId":"RES-001","status":"ACTIVE"},"sort":"title","descending":false,
 "groupBy":["period","ageGroup"],"aggregation":"SUM","page":1,"limit":100,
 "select":["external_key","title"],"include":["observations.numericValue","observations.dimensions.scalarCode"]}
```

`filters`, `sort`, `select`, `include`, `groupBy`, and `aggregation` are checked against the approved contract metadata; unknown identifiers and unsafe SQL are rejected. Entity aliases (`resourceId`, `goalId`, `glossaryId`) resolve to the canonical entity `external_key`. `include` is a nested response allow-list (identity and lineage keys remain available), while `select` is a root-field allow-list. `groupBy`/`aggregation` are executed by the family-neutral aggregation planner for statistical dataflows.

Contract discovery is available without exposing physical tables or SQL:

```http
GET /api/v1/platform/contracts/{contractCode}/revisions/{revision}/introspection
GET /api/v1/platform/contracts/{contractCode}/pages
GET /api/v1/platform/contracts/{contractCode}/pages/{pageId}/query-capabilities
```

The capability document is the machine-readable source for allowed fields, relations, includes, aggregations and response schema identity. Filter values may use the declarative operators `EQ`, `NE`, `GT`, `GTE`, `LT`, `LTE`, `BETWEEN`, `IN`, `CONTAINS`, `STARTS_WITH`, `IS_NULL` and `NOT_NULL`.

The response envelope is:

```json
{
  "pageId": 11,
  "nodeCode": "KIDS_STATISTICS",
  "contractCode": "KIDS_PORTAL_V1",
  "contractRevision": 8,
  "datasetCode": "KIDS_STATISTICAL_INPUT",
  "path": "/kids/statistics",
  "data": [],
  "pagination": {"page": 1, "limit": 100, "returned": 0},
  "relations": ["raw.source_record", "publication.dataset_snapshot", "classification.item"]
}
```

Entity pages return canonical `entity.entity_record` rows (including immutable payload and raw source lineage) from published snapshots. Statistics return series/observation/age-dimension rows from `statistics.series`, `statistics.observation`, and `statistics.observation_dimension`, restricted to published snapshots. Classifiers return the published `reference.classification_item_snapshot` registry. Publication status is therefore an enforced read boundary, not a UI convention.

The page contract itself remains in control-plane tables (`site_contract_revision`, `site_contract_node`, `contract_page_binding`, dataset/field/relation/projection registries) and in the canonical Access transport (`__gs_page`, `__gs_*`). Access is an ingest artifact; API reads materialized canonical data and does not execute Access logic.

Cross-cutting titles, descriptions, semantic annotations, accessibility text, provenance and distribution hints are governed by the Universal Metadata Plane (`platform.metadata_subject`, `metadata_schema`, `metadata_assertion`, `metadata_relation`). They are extensible through registered namespaces and versioned schemas; they are not hidden in raw payloads. See [universal-metadata-plane.md](universal-metadata-plane.md).

Runtime safety is enforced by `ContractQueryCompiler`: filters and sort fields must exist in the declared contract field set and values are always parameters. `ContractPhysicalQueryService` resolves only approved DATA-plane table/field mappings and recursively materializes declared relation graphs (bounded depth with cycle detection); no caller can submit a table name or SQL fragment. `ContractRelationGraphExecutor` provides database-neutral cardinality-aware attachment. These primitives are intentionally independent of KIDS names; onboarding another site is a new approved contract and physical mapping, not a controller branch.
