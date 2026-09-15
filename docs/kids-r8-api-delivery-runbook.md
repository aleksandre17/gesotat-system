# KIDS R8 API delivery runbook

**Purpose:** implementation-ready handoff for delivering KIDS data through the
contract-driven API. UI is outside this runbook. The API never accepts SQL or
physical table names from a caller.

## 1. Contract and authorization

The consumer selects `KIDS_PORTAL_V1` revision `8` through the approved Control
Plane contract. A machine client must receive the product-scoped read grant
(`geostat.product.KIDS_PORTAL.read`, mapped internally to `READ_RESOURCE`). No
database credentials, Access paths or internal numeric join IDs are sent to the
consumer.

## 2. Page map

| pageId | nodeCode | dataset | endpoint purpose |
|---:|---|---|---|
| 7 | `KIDS_ROOT` | — | site metadata/navigation; data query is rejected |
| 8 | `KIDS_GOALS` | `KIDS_GOAL` | goals collection |
| 9 | `KIDS_RESOURCES` | `KIDS_RESOURCE` | resources and declared relations |
| 10 | `KIDS_GLOSSARY` | `KIDS_GLOSSARY_ENTRY` | glossary entries |
| 11 | `KIDS_STATISTICS` | `KIDS_STATISTICAL_INPUT` | metric/series observations |
| 12 | `KIDS_CLASSIFIERS` | `KIDS_CLASSIFIER_ITEM` | published reference items |

The page map is read from the approved revision; these numbers are the current
KIDS R8 values, not a generic engine assumption.

## 3. Discover before querying

```http
GET /api/v1/platform/contracts/KIDS_PORTAL_V1/pages
GET /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/9/query-capabilities
GET /api/v1/platform/contracts/KIDS_PORTAL_V1/revisions/8/introspection
Authorization: Bearer <product-scoped-token>
```

The discovery response tells the client which fields, filters, relations,
includes, aggregations and response shape are legal. A client should cache it by
`contractCode + revision` and invalidate it when the revision changes.

## 4. Entity/resource request and response

```http
POST /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/9/query
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "filters": {"resourceId": "RES-001", "status": "ACTIVE"},
  "sort": "title",
  "descending": false,
  "page": 1,
  "limit": 100,
  "select": ["external_key", "title", "description"],
  "include": ["classifications", "relationships", "lineage"]
}
```

```json
{
  "pageId": 9,
  "nodeCode": "KIDS_RESOURCES",
  "contractCode": "KIDS_PORTAL_V1",
  "contractRevision": 8,
  "datasetCode": "KIDS_RESOURCE",
  "data": [{
    "key": "RES-001",
    "title": {"ka": "...", "en": "..."},
    "description": {"ka": "...", "en": "..."},
    "classifications": [],
    "relationships": [],
    "lineage": {"sourceRecord": "..."}
  }],
  "pagination": {"page": 1, "limit": 100, "returned": 1},
  "relations": ["classification", "resource_subcategory"]
}
```

The actual fields and relation names are always taken from capability metadata;
unknown fields, undeclared includes and unsafe values fail closed.

## 5. Statistical request and response

```http
POST /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/11/query
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "filters": {
    "metricCode": "KIDS_OBS_VALUE_INPUT",
    "carrierCode": "CARRIER-001",
    "unitCode": "COUNT_EVENT",
    "periodFrom": "2020-01-01",
    "periodTo": "2025-12-31",
    "dimension.AGE_GROUP": "0_4"
  },
  "groupBy": ["TIME_PERIOD", "AGE_GROUP"],
  "aggregation": "SUM",
  "sort": "TIME_PERIOD",
  "page": 1,
  "limit": 500,
  "include": ["lineage", "dimensions"]
}
```

```json
{
  "pageId": 11,
  "nodeCode": "KIDS_STATISTICS",
  "contractCode": "KIDS_PORTAL_V1",
  "contractRevision": 8,
  "datasetCode": "KIDS_STATISTICAL_INPUT",
  "data": [{
    "series": {"carrierCode": "CARRIER-001", "metricCode": "KIDS_OBS_VALUE_INPUT"},
    "dimensions": {"TIME_PERIOD": "2024", "AGE_GROUP": "0_4"},
    "observation": {"value": 123, "unit": "COUNT_EVENT", "aggregation": "SUM"},
    "lineage": {"sourceRecord": "SRC-001"}
  }],
  "pagination": {"page": 1, "limit": 500, "returned": 1},
  "relations": ["raw.source_record", "classification.item"]
}
```

The server validates metric, unit, aggregation, dimensions and filters against
the revision-scoped semantic registry. It does not infer `AGE_GROUP` or any
other dimension from code.

## 6. Raw and cross-family data

Raw lineage is included only when the approved projection and consumer scope
allow it. A public site client receives lineage references, not unrestricted raw
payloads or object-storage credentials. Cross-family includes (for example
resource → classifier or statistical observation → raw lineage) must be declared
in the contract relation registry; otherwise the request returns a validation
error.

## 7. Pagination, caching and consistency

For high-volume collections use the contract query endpoint's opaque cursor
mode. The cursor is signed and bound to contract revision, snapshot and query
fingerprint. Never construct a cursor or SQL predicate manually. Responses are
read from the published snapshot and may be revalidated with the release ETag.

## 8. Error contract

| condition | result |
|---|---|
| no/invalid token | `401` |
| missing product scope/tenant permission | `403` |
| unknown field/filter/include/aggregation | `400` with machine-readable issue |
| missing or non-approved contract revision | fail-closed contract error |
| stale cursor or snapshot mismatch | `409`/validation error; restart from discovery |
| unpublished/staging data request | rejected; no draft leakage |

## 9. Release checklist

- [ ] Consumer receives the approved product-scoped token.
- [ ] Client discovers revision 8 capabilities before sending queries.
- [ ] Entity, relation/include, statistics and export acceptance responses are
  captured with contract revision and snapshot identifiers.
- [ ] Row counts, relation cardinality, checksum and lineage evidence reconcile.
- [ ] Only after steward/publication approval is the response endpoint enabled
  for the production site.

The current repository and staging checks prove the implementation and
contract-only behavior. Production token, tenant policy, publication approval
and external consumer acceptance remain separate authority gates.
