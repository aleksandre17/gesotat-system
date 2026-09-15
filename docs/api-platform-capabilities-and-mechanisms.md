# GeoStat Contract API — შესაძლებლობები და მექანიზმები

**სტატუსი:** პლატფორმის implementation guide (UI/JWT-ისგან დამოუკიდებელი)  
**როლი:** KIDS R8 არის პირველი reference profile; იგივე API მუშაობს ნებისმიერ approved site/dataset/family კონტრაქტზე.

## 1. არქიტექტურული კონტრაქტი

```text
HTTP request
  → content negotiation / authentication boundary
  → approved contract + revision resolution
  → alias/field/operator validation
  → relation graph + family adapter execution
  → quality/privacy/publication boundary
  → projection, include, distinct, pagination
  → ETag + versioned response envelope
```

კლიენტი გადასცემს მხოლოდ `contractCode`, `pageId`, fields, filters, relations და projection options-ს. SQL, physical schema და table name caller-ის კონტრაქტში არ შედის.

## 1.1 სისტემის საწყისი წერტილი, namespace და identity

სისტემა იწყება არა კონკრეტული Access business table-ით, არამედ დამტკიცებული კონტრაქტით:

```text
Source / Access package
  → __gs_* manifest და identity
  → contract/dataset/field/key/index/relation metadata
  → validation და approval
  → canonical physical materialization
  → publication snapshot
  → API response
```

### Access transport prefixes

| Prefix | ოჯახი | დანიშნულება |
|---|---|---|
| `__gs_` | system/governance | contract, schema, field, key, index, relation და policy metadata |
| `__ent_` | entity | ბიზნეს-ობიექტები და master records |
| `__raw_` | raw | უცვლელი source/document/provenance |
| `__stat_` | statistical | series, observations, dimensions, measures |
| `__cl_` | classification | item, alias, hierarchy, version |
| `__rel_` | relation | რეალური link/bridge კავშირები |
| `__geo_` | geospatial | feature და geometry |
| `__pub_` | publication | snapshot/release |
| `__serv_` | serving | cache/read model |
| `__arch_` | archive | immutable archive/payload reference |
| `__audit_` | audit/security | audit trail და approvals |
| `__sys_` | technical system | ტექნიკური სისტემური ობიექტები |
| `__idx_` | index identity | contract-declared index identity |
| `__kids_` | KIDS transport namespace | მხოლოდ KIDS-ის Access/virtual სახელი |

მაგალითად `__raw_kids_statistical_carrier` ნიშნავს: `__raw_` = raw family, `kids` = site namespace, `statistical_carrier` = logical role. ეს არის transport სახელი და არა საბოლოო canonical SQL table.

### Metadata relation და real relation

```text
__gs_relation → როგორ უნდა იყოს კავშირი (contract/metadata)
__rel_*       → რა რეალური კავშირები არსებობს (link/bridge data)
```

`__gs_*` transport rows canonical Control Plane-ში ნორმალიზდება, ხოლო data rows მიდის შესაბამის family store-ში:

```text
__gs_structure → platform.contract_structure
__gs_dataset   → contract dataset identity
__gs_field     → platform.contract_field_definition
__gs_key       → platform.contract_index_definition + index_column
__gs_index     → platform.contract_index_definition + index_column
__gs_relation  → platform.contract_structure_relation
__cl_*         → classification registry/proposals
__raw_*        → raw.document/source_record
__ent_*        → entity.entity_record
__stat_*       → statistics.series/observation/dimension
__rel_*        → entity/relation link tables
```

საწყისი identity არის `sourceSystem + namespace + contractCode/revision + datasetCode`; შემდეგ ემატება table, field, natural/business key, index და relation identity. Access არის contract transport envelope, Control Plane — კონტრაქტის ავტორიტეტი, Data Plane — generic canonical storage, API — approved snapshot-ის contract projection.

## 2. Discovery და contract introspection

```http
GET /api/v1/platform/contracts/{contractCode}/revisions/{revision}
GET /api/v1/platform/contracts/{contractCode}/pages
GET /api/v1/platform/contracts/{contractCode}/pages/{pageId}/query-capabilities
GET /api/v1/platform/contracts/{contractCode}/revisions/{revision}/openapi
GET /api/v1/platform/contracts/{contractCode}/revisions/{revision}/json-schema
GET /api/v1/platform/contracts/{contractCode}/revisions/{revision}/typescript
GET /api/v1/platform/contracts/{contractCode}/compatibility?fromRevision=7&toRevision=8
```

Discovery აბრუნებს მხოლოდ approved metadata-ს: datasets, fields, aliases, relations, allowed operators, aggregations, includes, schema version-ს და response shape-ს.

### Discovery-ის თითოეული ნაწილის დანიშნულება და სარგებელი

| რა ბრუნდება | რა არის | რისთვის გამოიყენება | მთავარი სარგებელი |
|---|---|---|---|
| `pageId` | UI/API resource-ის სტაბილური იდენტობა | სწორი გვერდის/რესურსის არჩევა | URL და კლიენტი არ არის ფიზიკურ table name-ზე დამოკიდებული |
| `datasetCode` | ლოგიკური მონაცემთა პროდუქტი | query-ის semantic target | ერთი გვერდი შეიძლება სხვა physical provider-ზე გადავიდეს კონტრაქტის შეცვლის გარეშე |
| `fields` | კონტრაქტით გამოცხადებული data elements | `select`, `sort`, `where`, validation | typo, undeclared field და accidental data exposure იბლოკება |
| `aliases` | სტანდარტული/კლიენტისთვის მოსახერხებელი სახელები | metric, unit, period, dimension და family-neutral filters | სხვადასხვა წყაროს სახელები ერთ canonical vocabulary-ში ერთიანდება |
| `relations` | approved dataset-to-dataset კავშირები | `include`, nested filter, graph traversal | entity, raw, classifier და statistical მონაცემები ერთ response-ში ერთიანდება |
| `allowedIncludes` | დასაშვები relation paths | response enrichment | კლიენტი წინასწარ ხედავს, რომელი დამატებითი ობიექტის მოთხოვნა შეუძლია |
| `allowedFilters`/operators | დასაშვები predicate-ები | დინამიკური search/filter ფორმები | UI და სხვა კლიენტები თვითონ იგებენ რა query-ის აგება შეიძლება |
| `allowedAggregations` | კონტრაქტით ნებადართული aggregate ოპერაციები | chart/table summary და analytics | `SUM/COUNT/AVG` არ გამოიყენება არასწორ metric-ზე |
| `responseSchema`/`schema.id` | პასუხის machine-readable shape/version | deserialization და contract testing | revision-ის ცვლილება კლიენტის მოულოდნელად გატეხვის გარეშე კონტროლდება |
| `projection` | field/relation-ის საჯარო response mapping | სხვადასხვა consumer-ისთვის view | internal storage არ ხდება public API contract |
| `quality/privacy gates` | publication-ის წინაპირობები | მხოლოდ დამტკიცებული მონაცემის serving | დაუმტკიცებელი, უხარისხო ან კონფიდენციალური row public response-ში არ გადის |
| `pagination capabilities` | offset/keyset, limit, cursor წესები | დიდი dataset-ის ნაწილებად წაკითხვა | სტაბილური performance და repeatable traversal |

### პრაქტიკული გამოყენების pattern

კლიენტი ჯერ კითხულობს `query-capabilities`-ს, შემდეგ ამ metadata-ით აგებს საკუთარ query-ს:

```text
capabilities
  → field/filter/include არჩევა
  → contract validation
  → query execution
  → schema-versioned response
```

ამიტომ ფორმის, ცხრილის, ჩარტის ან სხვა consumer-ის კოდი არ უნდა შეიცავდეს KIDS-ის ველების, relation-ების ან aggregation-ების hardcoded სიას. კლიენტი ამ სიას ყოველ revision-ზე discovery-დან იღებს და capability-ის არქონის შემთხვევაში ფუნქციას ავტომატურად მალავს ან არ აგზავნის.

## 3. Generic query

```http
POST /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/8/query
Accept: application/json
Accept-Language: ka
Accept-Profile: urn:geostat:kids:resource:v8
Content-Type: application/json
```

```json
{
  "filters": {"status": "ACTIVE"},
  "where": {
    "and": [
      {"field":"title", "op":"CONTAINS", "value":"ბავშვი"},
      {"field":"resourceId", "op":"IN", "value":["R-1","R-2"]}
    ]
  },
  "select": ["resourceId", "title", "description"],
  "include": ["classification", "raw.lineage"],
  "distinct": true,
  "includeLimits": {"classification": 20},
  "limit": 100
}
```

პასუხის ძირითადი envelope:

```json
{
  "pageId": 8,
  "contractCode": "KIDS_PORTAL_V1",
  "contractRevision": 8,
  "datasetCode": "KIDS_RESOURCE",
  "data": [],
  "pagination": {"mode":"OFFSET","page":1,"limit":100,"returned":0,"total":0,"hasNext":false},
  "schema": {"id":"urn:geostat:kids_portal_v1:kids_resource:v8","version":"8","mediaType":"application/json"},
  "governanceGates": []
}
```

## 4. Relation graph და mixed-family query

```json
{
  "where": {
    "and": [
      {"field":"metricCode","op":"EQ","value":"KIDS_OBS_VALUE_INPUT"},
      {"relation":{"name":"dimensions","where":{"field":"AGE_GROUP","op":"EQ","value":"0-17"}}}
    ]
  },
  "include": ["dimensions", "classification", "raw.lineage"]
}
```

Relation-ის გაშვება დასაშვებია მხოლოდ approved `__gs_relation`/Control Plane relation-ით, declared endpoint field-ებით და `PRIMARY/UNIQUE/NATURAL` target key-ით. მხარდაჭერილია `ONE_TO_ONE`, `ONE_TO_MANY`, `MANY_TO_ONE`, `MANY_TO_MANY`; many-to-many კავშირი link/relation dataset-ით სრულდება.

## 5. Stable keyset pagination

პირველი მოთხოვნა stable sort-ით:

```json
{
  "sort":"updatedAt",
  "orderBy":[
    {"field":"updatedAt","direction":"ASC"},
    {"field":"resourceId","direction":"ASC"}
  ],
  "limit":100
}
```

პასუხში ბრუნდება `pagination.nextCursor`. Cursor-ში ხელმოწერილია contract/page, snapshot, query fingerprint, direction, sort tuple და `lastSeen` values.

```json
{
  "pagination": {
    "mode":"KEYSET",
    "direction":"FORWARD",
    "returned":100,
    "nextCursor":"KS.<signed-token>"
  }
}
```

შემდეგი მოთხოვნა გადასცემს იმავე query-ს და `cursor`-ს. execution იყენებს parameterized lexicographic predicate-ს:

```sql
(k1 > :lastK1) OR (k1 = :lastK1 AND k2 > :lastK2)
```

DESC მიმართულება იყენებს საპირისპირო operator-ს. sort keys უნდა იყოს კონტრაქტში გამოცხადებული და შესაბამისი stable index უნდა არსებობდეს.

## 6. Statistical და SDMX facade

```http
GET /api/v1/sdmx/data/{flow}/{key}
GET /api/v1/sdmx/dataflow/{flow}
GET /api/v1/sdmx/datastructure/{dataset}
GET /api/v1/sdmx/codelist/{scheme}
```

სტატისტიკური dataflow აბრუნებს series/observations/dimensions-ს, metric/unit/aggregation-ს, classifier-ს და raw lineage-ს. SDMX endpoint არის governed facade; ახალი flow ერთვება მხოლოდ კონტრაქტითა და DSD/dimension metadata-ით.

## 7. Export და asynchronous operations

```http
POST /api/v1/platform/operations/exports?pageId=8
GET  /api/v1/platform/operations/{operationId}
POST /api/v1/platform/operations/{operationId}/cancel
```

```json
{"format":"CSV","filters":{"status":"ACTIVE"},"include":["raw.lineage"]}
```

მხარდაჭერილი baseline formats: `JSON`, `CSV`, `NDJSON`. Operation ინახება `platform.api_operation` ledger-ში, აქვს status/progress/error/checksum/result URI და idempotent retry boundary.

## 8. Error, cache და safety mechanisms

- RFC 9457 `application/problem+json` envelope;
- `400` validation, `401/403` security, `404` resource, `409` revision/idempotency, `406` representation, `422` contract/gate, `503` retryable infrastructure;
- `ETag`, `If-None-Match`, `304`, `Cache-Control`, `Vary`;
- cursor HMAC signing და query fingerprint binding;
- maximum page/include/graph budgets;
- identifier allow-list და parameterized values;
- caller SQL/table access აკრძალულია.

მაგალითი:

```json
{
  "type":"https://api.geostat.ge/problems/contract-field-not-allowed",
  "title":"Contract field is not allowed",
  "status":400,
  "code":"CONTRACT_FIELD_NOT_ALLOWED",
  "detail":"Field 'internalSecret' is not declared by the approved contract",
  "correlationId":"uuid"
}
```

## 9. Lifecycle და onboarding

```text
contract draft → review → APPROVED
source binding/mapping → validation → staging
→ semantic materialization → quality/privacy gates
→ snapshot → publication → serving cache
```

ახალი საიტი იყენებს `contract-only onboarding`-ს: contract revision, fields, keys, indexes, relations, aliases, semantics, policies და mappings რეგისტრირდება Control Plane-ში; runtime engine-ში საიტის კოდი არ ემატება.

## 10. დაკავშირებული authoritative დოკუმენტები

- [API სრული Input/Output კონტრაქტი](api-complete-input-output-contract.md)
- [API modernization capability gap](api-modernization-capability-gap.md)
- [API contract execution audit](api-contract-execution-audit.md)
- [Final unified physical/virtual contract](final-unified-physical-virtual-contract.md)

## 11. Control Plane Meta-Contract

Control Plane თვითონაც იმართება versioned Meta-Contract-ით. მისი registry მოიცავს namespace, structure, table, field, index, relation, semantic/classification binding, quality/privacy/lineage rule, approval, migration, alias, page binding, projection, operation და schema-migration ობიექტებს. ეს განსაზღვრავს პლატფორმის შესაძლებლობებს და მის lifecycle-ს: `register → validate → version → review → approve → activate → publish → supersede → retire → rollback`.
