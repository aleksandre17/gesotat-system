# Geostat API — სრული Input/Output კონტრაქტი

**სტატუსი:** implementation contract, UI-ისგან დამოუკიდებელი
**ვერსია:** KIDS revision 8 / 2026-09-13
**Base URL:** `http(s)://<host>:8083`

ეს დოკუმენტი აღწერს რას იღებს API, როგორ ამოწმებს, რომელ ფიზიკურ ფენაში წერს და რა სტრუქტურით აბრუნებს შედეგს. კონტრაქტი არ ცვლის `platform.*` registry-ს; იგი მის executable interface-ს წარმოადგენს.

## 1. საერთო წესები

ყველა protected endpoint იღებს:

```http
Authorization: Bearer <JWT>
Accept: application/json
X-Correlation-Id: <რეკომენდებული UUID>
```

`Content-Type` არის `application/json`, გარდა multipart Access/XLSX endpoint-ებისა. ყველა identifier არის დადებითი integer; ყველა code case-sensitive არის registry-ის წესის მიხედვით; კლიენტმა არ უნდა გააკეთოს raw SQL.

API მხოლოდ `PUBLISHED` publication snapshot-ს აბრუნებს. `DRAFT`, `REVIEW_REQUIRED` და `SEMANTIC_REVIEW` მონაცემები write/review workflow-ის შედეგია და public read endpoint-ით არ გამოდის.

სტანდარტული შეცდომა:

```json
{
  "timestamp": "2026-09-11T07:00:00Z",
  "status": "BAD_REQUEST",
  "message": "human-readable summary",
  "errors": ["machine-readable detail"],
  "errorCode": 422,
  "correlationId": "uuid"
}
```

HTTP მნიშვნელობები: `400` malformed input, `401` missing/invalid JWT, `403` permission, `404` unknown resource, `409` idempotency/version conflict, `422` contract/quality/gate failure, `503` retryable storage/worker failure.

უფლებები:

| მოქმედება | authority |
|---|---|
| read published data/health | `READ_RESOURCE` |
| preview/ingest/validate/materialize | `WRITE_RESOURCE` |
| approve/publish/rollback/mapping release | `PUBLISH_RESOURCE` |
| user/role/permission administration | `MANAGE_USERS`, `MANAGE_ROLES`, `MANAGE_PERMISSIONS` |

## 2. Canonical Access package

### `POST /api/v1/platform/access/semantic/preview`

`multipart/form-data`, field `file` (`.accdb`/`.mdb`). Preview-ს side effect არ აქვს.

მიღებული პაკეტი უნდა შეიცავდეს contract manifest-ს (`__gs_*`), revision identity-ს, ყველა declared source locator-ს, field/key/relation/index metadata-ს, classifier/metric/unit registry-ს და payload rows-ს.

აბრუნებს:

```json
{
  "valid": true,
  "contractCode": "KIDS_PORTAL_V1",
  "contractRevision": 7,
  "datasets": [
    {
      "datasetCode": "KIDS_RESOURCE",
      "sourceLocator": "__ent_kids_resource",
      "family": "ENTITY",
      "declaredRows": 225,
      "fields": [
        {"code":"source_resource_id","type":"CODE","role":"NATURAL_KEY","required":true}
      ]
    }
  ],
  "issues": [],
  "schemaHash": "sha256-hex"
}
```

`valid=false` ნიშნავს, რომ ingest არ უნდა გაეშვას. აკრძალულია undeclared table/field, unprefixed canonical table, missing key, wrong type, invalid relation, duplicate key ან checksum drift.

### `POST /api/v1/platform/access/semantic/ingest`

იგივე multipart `file`. ქმნის ერთ artifact/batch-ს და ყველა dataset load-ს checkpoint-ებით.

პასუხი (`PlatformPackageIngestReceipt`; artifact metadata ცალკე archive/DB query-ით იკითხება):

```json
{
  "batchId": 2,
  "artifactId": 2,
  "status": "STAGED",
  "datasetLoads": [
    {
      "datasetLoadId": 23,
      "stagedRows": 880,
      "status": "STAGED"
    }
  ]
}
```

Artifact არის immutable raw envelope; retry-ისას იგივე checksum-ს არ უნდა შეეცვალოს.

### `POST /api/v1/platform/access/semantic/batches/{batchId}/resume`

Body არ აქვს. API artifact URI-დან აღადგენს მხოლოდ იმ batch-ს, რომელსაც resumable checkpoint აქვს. terminal/published batch-ზე პასუხი არის `409`/`422` — ახალი batch ჩუმად არ იქმნება.

### `POST /api/v1/platform/access/loads/{datasetLoadId}/resume`

Body არ აქვს. იგივე წესი, მაგრამ legacy platform Access loader-ისთვის; წყარო არის immutable stored artifact.

## 3. Legacy managed import

ეს endpoint-ები მხოლოდ compatibility რეჟიმია და KIDS canonical pipeline-ს არ ცვლის.

### `POST /api/v1/imports/access/preview`

multipart field `file`. აბრუნებს `ManagedImportPreview`-ს: source tables, detected data kind, row counts, approved target mapping, issues და `executable` flag.

### `POST /api/v1/imports/access/execute`

multipart field `file`. აბრუნებს `ManagedImportExecutionResult`: job id, status, per-table counts, rejected/quarantine counts, errors და lineage references.

### `GET /api/v1/imports/access/{jobId}`

აბრუნებს `ManagedImportJobDetails`: job state, timestamps, source artifact, steps, counters, errors, retryability და final output references.

## 4. SQL source ingest

### `POST /api/v1/platform/sql/ingest?contractSourceId=<id>&sourceConnectionId=<id>`

Body არ აქვს. ორივე ID კონტრაქტის registry-დან უნდა იყოს. API თვითონ ამოწმებს active contract source-ს, connection policy-ს, secret reference-ს, row keys-ს და object-storage archival-ს.

აბრუნებს იმავე `PlatformIngestReceipt` envelope-ს, რასაც Access ingest; raw source პირდაპირ public read model-ში არ გადადის.

## 5. Staging, validation, snapshot

### `POST /api/v1/platform/ingestion/stage`

Request:

```json
{
  "contractSourceId": 320,
  "artifactId": 2,
  "datasetLoadId": 23,
  "idempotencyKey": "KIDS_PORTAL_V1|7|23"
}
```

Response `PlatformIngestReceipt`: staged row count, accepted/rejected counts, checkpoint, source locator, checksum და status.

### `POST /api/v1/platform/ingestion/validate/{datasetLoadId}`

Response:

```json
{
  "datasetLoadId": 23,
  "status": "VALIDATED",
  "totalRows": 880,
  "validRows": 880,
  "rejectedRows": 0,
  "issues": [
    {
      "stagedRowId": 0,
      "ruleCode": "",
      "severity": "ERROR",
      "message": "",
      "details": {}
    }
  ]
}
```

ამოწმებს exact schema, required/nullability/type, key uniqueness, relation cardinality/FK, classifier aliases, BCP-47, period, numeric conversion, metric/unit/aggregation, quality/privacy და source lineage-ს. Error row quarantine-ში რჩება; valid rows არ იკარგება.

### `POST /api/v1/platform/ingestion/prepare-snapshot`

Request:

```json
{
  "datasetLoadId": 23,
  "artifactId": 2,
  "checksum": "sha256-hex"
}
```

Response არის `datasetSnapshotId` (positive long). Snapshot ინახავს dataset version, row count, checksum, status და immutable raw records-ს. იგივე dataset load-ზე განმეორება აბრუნებს არსებულ snapshot-ს.

## 6. Semantic materialization

### `POST /api/v1/platform/ingestion/materialize`

Request:

```json
{
  "datasetSnapshotId": 21,
  "contractSourceId": 313
}
```

Response:

```json
{
  "datasetSnapshotId": 21,
  "contractSourceId": 313,
  "family": "STATISTICAL",
  "writtenRows": 880,
  "status": "SEMANTIC_REVIEW"
}
```

Mapping family-ის მიხედვით:

| family | input | canonical output |
|---|---|---|
| `ENTITY` | key/title/localized/locator/classification | `entity.entity_record`, `localized_text`, `resource_locator`, `entity_classification` |
| `CLASSIFICATION` | entity key + scheme/version/code | `entity.entity_classification` |
| `RELATION` | two entity keys + relationship type | `entity.entity_link` |
| `STATISTICAL` / SDMX long | carrier/metric/unit/period/dimension/value | `statistics.series`, `observation`, `observation_dimension` |
| `STATISTICAL_WIDE_JSON` | reviewed array + pivot dimension | same statistics tables |
| `RAW` | artifact/source locator | `raw.source_record` / archive pipeline |
| `REFERENCE` | classifier/registry rows | control-plane registry/proposal pipeline; no fake entity rows |
| `GEO` | GeoJSON + geometry type | `geo.feature` |

Materialization არის transaction-bound, source lineage-ს ინარჩუნებს, published snapshot-ს არ ცვლის და duplicate grain-ს reject/ignore წესით ამუშავებს.

## 7. KIDS statistical output

### `GET /api/v1/platform/metrics/{metricCode}/points?productId=<id>`

Response არის ordered `PublishedMetricPoint[]`:

```json
[
  {"period":"2018-01-01","value":132.0},
  {"period":"2019-01-01","value":141.0}
]
```

მხოლოდ `PUBLISHED` metric, product და observations იკითხება. დაუშვებელია DRAFT metric-ის დაბრუნება.

### Statistical canonical object contract

სრული dimension-aware response-ის canonical shape არის:

```json
[
  {
    "carrierCode": "CARRIER|162",
    "metric": {"code":"KIDS_ABORTIONS_COUNT","unit":"COUNT_EVENT","aggregation":"SUM"},
    "dimensions": {"AGE_GROUP":{"code":"0-17","label":"0-17"}},
    "observations": [
      {"timePeriod":"2018-01-01","value":132,"obsStatus":"A","confStatus":"F"}
    ],
    "lineage": {"publicationSnapshotId":3,"sourceRecordId":1934}
  }
]
```

`value` არის typed numeric value; `value_lexical` და original payload მხოლოდ lineage/raw audit-ში რჩება. Metric/unit/aggregation არ infer-დება response-ის დროს.

## 8. Dynamic page/chart output

### `GET /api/v1/dynamic/pages/{pageId}/data?page=1&limit=100`

Response `DynamicTableResponse` (ზუსტი DTO):

```json
{
  "page":1,
  "limit":100,
  "columns":[{"code":"id","label":"ID","type":"CODE"},{"code":"title","label":"Title","type":"TEXT"}],
  "rows":[{"id":"128","title":"..."}],
  "page":1,
  "limit":100
}
```

`page >= 1`, `1 <= limit <= server maximum`; columns, filters, joins და sorts მოდის site contract-იდან, არა query string-ის თავისუფალი SQL-იდან.

For a governed contract projection, pass `contractCode`; the endpoint delegates to the canonical contract engine:

```http
GET /api/v1/dynamic/pages/11/data?contractCode=KIDS_PORTAL_V1&page=1&limit=100
```

The legacy profile/table mode remains only for backward compatibility.

### `GET /api/v1/dynamic/pages/{pageId}/charts/{chartCode}`

Response `DynamicChartResponse` შეიცავს `chartCode`, `chartType` და ordered `rows` array-ს. Row-ებში კონტრაქტის მიხედვით მოდის dimensions/points; chart config არასოდეს ცვლის metric semantics-ს.

### `GET /api/v1/platform/visualizations/{code}?productId=<id>`

Response `PublishedVisualization` (ზუსტი DTO):

```json
{
  "code":"KIDS_STATISTICS",
  "chartType":"LINE",
  "configJson":"{...}",
  "points":[{"periodStart":"2018-01-01","value":132.0}]
}
```

### `GET /health`

აბრუნებს plane health-ს: `primary`, `secondary`, `dataPlane`, `archivePlane`, `objectStorage`, საერთო `status`.

## 9. Publication და rollback

### `POST /api/v1/platform/publication/publish`

Request:

```json
{
  "productId":1,
  "datasetVersionId":66,
  "datasetSnapshotId":8,
  "checksum":"sha256-hex"
}
```

Publish მხოლოდ მაშინ დასაშვებია, როცა ყველა release-gated dataset snapshot-ს აქვს PASS quality/privacy/lineage/reconciliation, no unresolved classifier/relation, valid checksum და complete mapping. პასუხი:

```json
{
  "releaseId":2,
  "publicationSnapshotId":3,
  "previousPublicationSnapshotId":0,
  "status":"PUBLISHED"
}
```

ერთი product release-ში ყველა მზად dataset member ერთ publication snapshot-ში შედის. Outbox/archive/cache side effect-ები transactional/retry-safe არის.

### `POST /api/v1/platform/publication/rollback`

Request:

```json
{"productId":1,"targetPublicationSnapshotId":3}
```

Response არის `PublicationReceipt` status `ROLLED_BACK`; published historical snapshot immutable რჩება, იცვლება მხოლოდ current publication pointer.

## 10. Contract governance

### `POST /api/v1/platform/contracts/{contractId}/approve`

Body არ აქვს. `PUBLISH_RESOURCE`-ით ამტკიცებს contract lifecycle transition-ს. აბრუნებს contract id, revision, approval actor/time, checksum და resulting lifecycle state-ს.

### `PUT /api/v1/platform/contracts/{contractId}/sources/{contractSourceId}/mapping`

Body არის სრული mapping JSON, partial patch არა:

```json
{
  "approvalState":"READY",
  "family":"ENTITY",
  "recordType":"KIDS_RESOURCE",
  "key":"source_resource_id",
  "localizedText":[{"fieldCode":"TITLE","language":"ka","path":"title_ka","required":true}],
  "locators":[{"kind":"PATH","language":"ka","path":"path_ka","required":false}],
  "qualityPolicy":"KIDS_AGGREGATE_QUALITY_V1",
  "confidentialityPolicy":"KIDS_PUBLIC_AGGREGATE_V1"
}
```

Server ამოწმებს mapping schema, family compatibility, dataset/version identity, referenced fields, policy codes და ETag/concurrent revision-ს. Approved/issued revision mutable არ არის.

## 11. რა არ უნდა გაიგზავნოს

- raw SQL ან table name query parameter-ით;
- დაუდეკლარირებელი Access table/field;
- client-calculated metric/unit/aggregation, როგორც authoritative მნიშვნელობა;
- classifier label code-ის ნაცვლად;
- published snapshot-ის overwrite request;
- secret/access key/password/token request body-ში;
- raw payload public response-ში, თუ კონტრაქტი explicit raw export-ს არ აცხადებს.

## 12. კავშირის/გაერთიანების წესები

Virtual projection-ს შეუძლია raw, entity, classifier და statistical dataset-ის გაერთიანება, თუ registry-ში არსებობს:

`contract_code + contract_revision + dataset_code + source_locator + join_key + cardinality + relation_type`.

Classifier join სრულდება alias→approved versioned item snapshot-ით. Raw+statistical join სრულდება `source_resource_id`/`carrier_code`-ით. შედეგი არის DTO/view/cache; ორიგინალი physical raw rows არ merge-დება და lineage ორივე source-ზე რჩება.

## 13. Client acceptance checklist

კლიენტმა ყოველი response-ისთვის უნდა შეამოწმოს:

1. HTTP status და `errorCode`;
2. `publicationSnapshotId`/contract revision;
3. schema version და content type;
4. stable object keys და array ordering;
5. unit/aggregation/period semantics;
6. `null` განსხვავდება missing field-ისგან;
7. pagination `total/page/limit`;
8. lineage/correlation id;
9. retry მხოლოდ `503`/retryable `409`-ზე;
10. არ შეინახოს ან გამოაჩინოს secret, raw artifact ან private payload.

ეს არის მოქმედი API contract. UI ამ პასუხებს მხოლოდ render-ავს; UI-ს არ ეკუთვნის mapping, join, classifier resolution, metric calculation ან publication გადაწყვეტილება.
