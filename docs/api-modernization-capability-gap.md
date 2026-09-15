# API modernization capability gap

## მიზანი

ეს დოკუმენტი აფიქსირებს არა მხოლოდ KIDS-ის, არამედ მრავალსაიტიანი და მრავალოჯახიანი მონაცემთა პლატფორმის საბოლოო შესაძლებლობებს. KIDS არის პირველი reference implementation; სამიზნე სისტემა არის contract-driven, metadata-driven და schema-agnostic platform, რომელსაც ახალი საიტის, dataset-ის, მონაცემთა ოჯახის ან სტატისტიკური სტანდარტის მიღება უნდა შეეძლოს არსებული კოდის და ფიზიკური სქემის გაფართოების გარეშე.

სისტემური მიზანია უმაღლესი production-grade დონე: კონტრაქტით მართვადი ინტროსპექცია, relation-graph execution, ხარისხისა და კონფიდენციალურობის gates, versioning/compatibility, მრავალფორმატიანი serving/export, უსაფრთხო ოპერაციები, observability, replay/rollback და კონტრაქტზე დაფუძნებული onboarding. არცერთი ბიზნეს-საიტი ან მისი სახელები არ არის execution engine-ის hardcoded ნაწილი.

## მიმდინარე შეჯამება (2026-09-13)

| შესაძლებლობა | სტატუსი | მტკიცებულება |
|---|---|---|
| Contract introspection | `IMPLEMENTED` | `ContractIntrospectionController` + capability endpoints |
| Generic scalar filters | `IMPLEMENTED` | `ContractQueryCompilerTest` |
| Generic statistical dimensions | `IMPLEMENTED` | `dimension.<dimensionCode>` filter/groupBy |
| Dynamic endpoint contract path | `IMPLEMENTED` | canonical-first routing; legacy fallback only |
| Total/hasNext pagination | `IMPLEMENTED` | canonical response pagination |
| Opaque cursor | `IMPLEMENTED` | page/contract query cursor |
| Snapshot-bound signed cursor | `IMPLEMENTED` | HMAC-signed contract/page-bound cursor |
| Full nested query DSL | `IMPLEMENTED (bounded)` | nested `and/or`, scalar operators და contract-bound relation predicates; depth/fan-out guardrails მოქმედებს; distinct და per-include limits ჯერ არ არის საჯარო კონტრაქტის ნაწილი |
| Versioned response metadata | `IMPLEMENTED` | `schema.id`, `schema.version`, `mediaType` |
| Content negotiation boundary | `IMPLEMENTED (baseline)` | governed endpoints enforce JSON/SDMX Accept profiles and emit `Vary` |
| RFC 9457 errors | `IMPLEMENTED` | `ContractApiExceptionHandler` / `application/problem+json` |
| ETag/conditional caching | `IMPLEMENTED` | canonical + contract query responses |
| OpenAPI/typed clients | `IMPLEMENTED (baseline)` | OpenAPI 3.1, JSON Schema 2020-12 და TypeScript client skeleton გენერირდება კონტრაქტიდან |
| SDMX REST compatibility | `IMPLEMENTED (facade)` | governed SDMX-shaped data/dataflow/datastructure/codelist facade; სრული XML/content-negotiation extension ცალკე პროფილია |
| Generic async export API | `IMPLEMENTED` | durable `platform.api_operation` ledger + `/platform/operations`; JSON/CSV/NDJSON შედეგები |

## უკვე არსებული შესაძლებლობები

ჩვენს API-ს უკვე შეუძლია:

- `pageId`-ით კონტრაქტის პოვნა;
- მხოლოდ `APPROVED/PUBLISHED` კონტრაქტისა და snapshot-ის წაკითხვა;
- `filters`, `select`, `include`, `groupBy`, `aggregation`;
- relation graph-ის კონტრაქტით აწყობა;
- entity + classifier + statistical + raw lineage გაერთიანება;
- metric/unit/aggregation-ის კონტრაქტიდან მიღება;
- Access/JSON ingest, validation, quarantine და idempotent replay;
- publication, rollback და cache;
- KIDS-ისთვის `ENTITY`, `STATISTICAL`, `REFERENCE`, `RAW`, `GEO`, `RELATION` ოჯახები;
- SQL-ისა და table name-ის caller-ისგან აკრძალვა.

ეს შეესაბამება OData-ს `filter/select/order/expand` მოდელს, GraphQL-ის schema-driven query იდეას და SDMX-ის dimension-aware სტატისტიკურ წვდომას.

## API-ში დასამატებელი შესაძლებლობები და შესრულების სტატუსი

### 1. Contract introspection — IMPLEMENTED

საჭიროა ოფიციალური endpoint-ები, რომლითაც კლიენტი პროგრამულად მიიღებს:

- აქტიურ `pageId`-ებს;
- dataset/field-ების სიას;
- დაშვებულ filter-ებს;
- relation/include path-ებს;
- დაშვებულ aggregation-ებს;
- response schema-ს ვერსიას.

```http
GET /api/v1/platform/contracts/KIDS_PORTAL_V1/revisions/8
GET /api/v1/platform/contracts/KIDS_PORTAL_V1/pages
GET /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/11/query-capabilities
```

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "status": "APPROVED",
  "pages": [
    {
      "pageId": 11,
      "datasetCode": "KIDS_STATISTICAL_INPUT",
      "allowedFilters": [
        "metricCode",
        "carrierCode",
        "periodFrom",
        "periodTo",
        "AGE_GROUP"
      ],
      "allowedIncludes": [
        "carrier",
        "metric",
        "unit",
        "dimensions",
        "classifierItem",
        "raw.lineage"
      ],
      "allowedAggregations": ["COUNT", "SUM"],
      "responseSchema": "urn:geostat:kids:statistics:v8"
    }
  ]
}
```

### 2. სრული query DSL — PARTIALLY IMPLEMENTED

განხორციელებულია declarative scalar/filter operators, nested boolean groups და contract-bound relation predicates. უსაფრთხო bounded execution-ის ფარგლებში:

- `IN`, `NE`, `GT/GTE/LT/LTE`, `BETWEEN`, `IS_NULL/NOT_NULL`;
- `CONTAINS/STARTS_WITH`;
- nested `AND/OR` ჯგუფები და relation predicate;
- relation graph-ის მაქსიმალური სიღრმე/row budget.

`distinct` და თითოეულ include-ზე ცალკე limit მომავალში დაემატება მხოლოდ ახალი კონტრაქტის capability-ად; ისინი caller-ისთვის დაუმტკიცებელი SQL შესაძლებლობა არ არის.

```json
{
  "where": {
    "and": [
      {
        "field": "carrierCode",
        "op": "IN",
        "value": ["CARRIER|162", "CARRIER|163"]
      },
      {
        "field": "period",
        "op": "BETWEEN",
        "value": ["2020-01-01", "2024-12-31"]
      },
      {
        "relation": "dimensions",
        "where": {
          "field": "AGE_GROUP",
          "op": "EQ",
          "value": "0-17"
        }
      }
    ]
  },
  "select": ["carrierCode", "value", "period"],
  "include": ["metric", "unit", "dimensions.classifierItem"],
  "orderBy": [
    {"field": "period", "direction": "ASC"}
  ],
  "page": {
    "size": 100,
    "after": "opaque-cursor"
  }
}
```

### 3. Cursor pagination — PARTIALLY IMPLEMENTED

`total`, `hasNext` და HMAC-signed cursor განხორციელებულია. generic physical datasets-ზე დაემატა multi-column keyset tuple, query fingerprint და snapshot-bound cursor; statistical family-სთვის keyset მხოლოდ მაშინ აქტიურდება, როცა კონტრაქტი stable-key profile-ს აცხადებს.

```json
{
  "data": [],
  "pagination": {
    "mode": "CURSOR",
    "size": 100,
    "nextCursor": "opaque-token",
    "hasNext": true,
    "snapshotId": 13
  }
}
```

### 4. HTTP caching და conditional requests — IMPLEMENTED

საჭიროა:

```http
ETag
If-None-Match
Last-Modified
If-Modified-Since
Cache-Control
304 Not Modified
```

`ETag` უნდა ეფუძნებოდეს:

```text
hash(contractCode + revision + pageId + query + publicationSnapshotId)
```

### 5. სტანდარტული error contract — IMPLEMENTED

API-მ უნდა გამოიყენოს RFC 9457-ის `application/problem+json` ფორმატი.

```json
{
  "type": "https://api.geostat.ge/problems/contract-field-not-allowed",
  "title": "Contract field is not allowed",
  "status": 400,
  "detail": "Field 'secretField' is not declared by the approved contract",
  "instance": "/api/v1/platform/contracts/KIDS_PORTAL_V1/pages/11/query",
  "code": "CONTRACT_FIELD_NOT_ALLOWED",
  "correlationId": "uuid",
  "contractCode": "KIDS_PORTAL_V1",
  "contractRevision": 8,
  "violations": [
    {
      "path": "$.select[0]",
      "field": "secretField"
    }
  ]
}
```

### 6. სრული metadata negotiation — PARTIALLY IMPLEMENTED

Response schema/version metadata და contract metadata უკვე ბრუნდება. `Accept-Language`/`Accept-Profile` კონტრაქტის envelope-ში ჯერ advisory metadata-ად რჩება; მკაცრი media-type negotiation ცალკე compatibility profile-ია.

```http
Accept-Language: ka
Accept: application/json
Accept-Profile: urn:geostat:kids:statistics:v8
```

```json
{
  "schema": {
    "id": "urn:geostat:kids:statistics:v8",
    "version": "8",
    "mediaType": "application/json"
  },
  "locale": "ka",
  "metadata": {
    "title": "...",
    "description": "..."
  }
}
```

### 7. OpenAPI და typed-client generation — PARTIALLY IMPLEMENTED

კონტრაქტიდან OpenAPI 3.1, JSON Schema Draft 2020-12 და TypeScript baseline generation განხორციელებულია. Java/Kotlin/Dart SDK-ები შემდგომი გენერატორის პროფილებია.

კონტრაქტიდან ავტომატურად უნდა გენერირდებოდეს:

- OpenAPI 3.1;
- JSON Schema;
- TypeScript types;
- Java/Kotlin client;
- Dart client;
- validation schemas;
- example requests/responses.

### 8. SDMX-compatible statistical API — IMPLEMENTED (facade)

```http
GET /api/v1/sdmx/data/KIDS_STATISTICAL_INPUT/
GET /api/v1/sdmx/data/KIDS_STATISTICAL_INPUT/0-17.CARRIER162
GET /api/v1/sdmx/dataflow/KIDS_PORTAL_V1
GET /api/v1/sdmx/datastructure/KIDS_STATISTICAL_INPUT
GET /api/v1/sdmx/codelist/AGE_GROUP
```

```http
Accept: application/vnd.sdmx.data+json;version=2.1
Accept: text/csv
Accept: application/xml
```

### 9. Async API operations — IMPLEMENTED (JSON export baseline)

Ingest/materialization/replay workflow-ებთან ერთად დაემატა durable generic export operation resource, progress/status polling, cancellation და checksum-იანი JSON artifact. დამატებითი ფორმატები და event push channels ცალკე interoperability extension-ია.

დიდი query, export, ingest ან materialization უნდა შესრულდეს ასინქრონულად.

```http
POST /api/v1/platform/exports
GET  /api/v1/platform/operations/{operationId}
POST /api/v1/platform/operations/{operationId}/cancel
```

```json
{
  "operationId": "op-123",
  "type": "EXPORT",
  "status": "RUNNING",
  "progress": 42,
  "result": null
}
```

დასრულებული ოპერაცია:

```json
{
  "operationId": "op-123",
  "status": "SUCCEEDED",
  "download": {
    "uri": "s3://geostat-exports/...",
    "expiresAt": "2026-09-13T20:00:00Z",
    "checksum": "sha256:..."
  }
}
```

### 10. Export და bulk response — IMPLEMENTED (baseline)

მხარდაჭერილია:

- JSON;
- CSV;
- NDJSON;
- large-result asynchronous export.

SDMX-JSON/XML, Parquet და ZIP მხოლოდ ცალკე format-provider-ის დამატებით ჩაირთვება; ამჟამინდელი API მათ არ აცხადებს, ამიტომ არ ქმნის ყალბ დაპირებას.

```http
POST /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/11/export
```

```json
{
  "format": "SDMX_JSON",
  "where": {
    "carrierCode": "CARRIER|162"
  },
  "include": [
    "metric",
    "unit",
    "dimensions",
    "raw.lineage"
  ]
}
```

### 11. API version compatibility — IMPLEMENTED (field/relation baseline)

Revision-to-revision compatibility endpoint განხორციელებულია; სრული relation/projection/semantic breaking-change analyzer ჯერ გასაფართოებელია.

```http
GET /api/v1/platform/contracts/KIDS_PORTAL_V1/compatibility
```

```json
{
  "fromRevision": 7,
  "toRevision": 8,
  "compatibility": "BACKWARD_COMPATIBLE",
  "breakingChanges": [],
  "addedFields": ["metadata", "lineage"],
  "deprecatedFields": []
}
```

### 12. Rate limit, quota და cost control — TODO

საჭიროა:

- per-client rate limit;
- query complexity limit;
- maximum relation depth;
- maximum include fan-out;
- maximum export size;
- timeout budget;
- concurrency quota.

## შესრულებული ცვლილებები ამ რევიზიაში

- დაემატა `GET /api/v1/platform/contracts/{contractCode}/revisions/{revision}/introspection`;
- დაემატა `GET /api/v1/platform/contracts/{contractCode}/pages`;
- დაემატა `GET /api/v1/platform/contracts/{contractCode}/pages/{pageId}/query-capabilities`;
- introspection აბრუნებს მხოლოდ approved contract metadata-ს და არ ამხელს SQL-სა და ფიზიკურ table identifiers-ს;
- response pagination-ს დაემატა `total` და `hasNext`, სადაც შესაბამისი family count ხელმისაწვდომია;
- declarative filter compiler-ს დაემატა `EQ`, `NE`, `GT`, `GTE`, `LT`, `LTE`, `BETWEEN`, `IN`, `CONTAINS`, `STARTS_WITH`, `IS_NULL`, `NOT_NULL`;
- სტატისტიკურ query-ს დაემატა კონტრაქტის მიხედვით `dimension.<dimensionCode>` filter და `groupBy` მხარდაჭერა; dimension ID registry-დან resolve-დება და არა KIDS-specific constant-ით;
- `GET /dynamic/pages/{pageId}/data` ახლა პირველ რიგში contract engine-ს იყენებს; `contractCode` explicit override-ადაა დაშვებული, ძველი profile რეჟიმი მხოლოდ pre-contract გვერდების backward compatibility-სთვისაა;
- დაემატა ამ ოპერატორების unit tests;
- დაემატა `GET /api/v1/platform/contracts/{contractCode}/revisions/{revision}/openapi`;
- დაემატა `GET /api/v1/platform/contracts/{contractCode}/compatibility?fromRevision=&toRevision=`;
- დაემატა contract-backed OpenAPI 3.1 document და revision compatibility baseline;
- დაემატა `/json-schema` და `/typescript` contract client-generation endpoints;
- დაემატა RFC 9457 `ProblemDetail` handler governed contract endpoints-ზე;
- დაემატა ETag/If-None-Match/Cache-Control response headers და `304 Not Modified`;
- დაემატა SDMX-shaped `/sdmx/data`, `/sdmx/dataflow`, `/sdmx/datastructure` და `/sdmx/codelist` facade;
- cursor გახდა HMAC-signed და contract/page scope-ზე მიბმული; secret მოდის `PLATFORM_CURSOR_SIGNING_SECRET`-იდან;
- დაემატა versioned `platform.contract_query_alias` registry (migration 077); introspection და validator alias-ებს უკვე registry-დან კითხულობს;
- დაემატა durable `platform.api_operation` ledger (migration 076) და `/platform/operations/exports` background JSON export;
- async operation submit-ს დაემატა validated `Idempotency-Key`, canonical request fingerprint და migration 079-ის unique registry; retries აბრუნებს თავდაპირველ operation-ს, ხოლო key reuse განსხვავებული payload-ით fail-closed არის;
- compatibility diff ახლა ადარებს fields-ის type/required ცვლილებებს და relation add/remove ცვლილებებს;
- დაემატა nested `where` compiler (`and`/`or`) და contract-bound physical execution path;
- contract და page responses-ს დაემატა `ETag`, `If-None-Match`, `Cache-Control` და `304 Not Modified`;
- governed contract endpoint-ებისთვის დაემატა RFC 9457 `application/problem+json` error envelope;
- `./gradlew :api:test` წარმატებით დასრულდა.

## არქიტექტურული შეზღუდვა

API ახლა metadata-driven query discovery-სა და declarative filtering-ს იყენებს. დარჩენილი შეზღუდვები არის hardening/standard-interoperability დონეზე:

- `ContractQueryController` კონტრაქტზე მუშაობს;
- სტატისტიკური execution იყენებს pluggable canonical statistical family adapter-ს (`statistics.series`, `statistics.observation`, `statistics.observation_dimension`); business/KIDS field names აღარ არის dimension resolution-ის წყარო, თუმცა canonical statistical storage adapter-ის ჩანაცვლება ჯერ hardening-ად რჩება;
- `CanonicalPageDataService` ოჯახების მიხედვით adapter branching-ს იყენებს;
- `dynamic/pages` endpoint-ის governed გვერდები უკვე contract engine-ით სრულდება; ძველი profile/table reader მხოლოდ pre-contract compatibility fallback-ია;
- ძირითადი სტატისტიკური aliases registry-შია; ყველა სამომავლო family-სთვის alias registry-ის seed/governance პოლიტიკის სრული გატანა დარჩენილია;
- pagination-ში `total` family-dependent არის; page და keyset cursor-ები HMAC-ით contract/page/query scope-ზეა ხელმოწერილი. stable-key profile კონტრაქტით უნდა გამოცხადდეს.

ეს ნიშნავს, რომ სისტემა არის **კონტრაქტზე დაფუძნებული პლატფორმა pluggable family adapters-ით**. ლოგიკური query path schema-agnostic არის; დარჩენილი სამუშაოები ეხება სტანდარტიზებულ discovery/error/cache/export/interop შესაძლებლობებს და არა KIDS-ის business branching-ს.

## მიზნობრივი საბოლოო სახელწოდება

**Contract-Driven, Schema-Agnostic Data Platform with Introspection, Relation-Graph Execution and Versioned Data-Product Serving**

KIDS-ის კონტრაქტი არის ამ პლატფორმის პირველი დამტკიცებული პროფილი და არა არქიტექტურის საზღვარი. ნებისმიერი სხვა საიტი ერთვება მხოლოდ versioned contract, mappings, keys, relations, semantics, quality/privacy policies და approved publication snapshot-ის მიწოდებით.

## დარჩენილი სამუშაოების პრიორიტეტი

1. Java/Kotlin/Dart generated SDK profiles;
2. SDMX XML/SDMX-JSON/Parquet/ZIP format providers და progress event channels;
3. სრული relation/projection/semantic compatibility analyzer;
4. alias registry-ის seed/governance ყველა data family-ზე;
5. high-volume stable-key profile-ების დამატება იმ dataset-ებისთვის, რომლებიც ჯერ stable sort/index metadata-ს არ აცხადებენ.

## სრული პროექტის გაერთიანებული სტატუსი

### დასრულებული baseline

- Control Plane / Data Plane / Archive Plane reference architecture;
- KIDS R8 contract და საბოლოო canonical Access artifact;
- Access → ingest → validation → materialization → publication pipeline-ის ძირითადი მექანიზმები;
- contract introspection და metadata-driven query engine;
- relation graph execution და ENTITY/STATISTICAL/REFERENCE/RAW/GEO/RELATION family adapters;
- nested `AND/OR`, relation predicates, `distinct`, include limits;
- HMAC cursor, keyset tuple, query fingerprint და snapshot scope;
- RFC 9457 errors, ETag/304/cache/Vary;
- OpenAPI 3.1, JSON Schema 2020-12 და TypeScript baseline generator;
- SDMX facade და durable asynchronous operation/export ledger;
- JSON/CSV/NDJSON export;
- semantic compatibility baseline და ყველა approved/published family-ის alias seed;
- migrations `076`, `077`, `078`;
- production API build/deploy და `healthy` health verification;
- ძველი generated KIDS Access არტეფაქტების retirement და ერთი საბოლოო R8 artifact.

### მიმდინარე gap — implementation და production evidence-ის გამიჯვნა

ზემოთ ჩამოთვლილი onboarding, provider lifecycle, keyset, semantic compatibility,
interoperability, rate/cost და documentation მექანიზმები implementation დონეზე
დაფარულია და technical acceptance-ით მოწმდება. დარჩენილი gap აღარ არის KIDS-ის
კოდის გაფართოება; ის არის რეალური production evidence:

1. approved release commit/tag და clean reproducible deploy;
2. რეალური OIDC/JWKS, RBAC/ABAC და tenant policy binding;
3. დაცული production API/export replay დამტკიცებული JWT-ით;
4. SQL Server/MySQL portability workload endpoints და large-table evidence;
5. durable telemetry backend, SLO/error-budget და alert firing;
6. signed publication evidence, backup/restore, DR და rollback replay;
7. approved load/chaos/security test window და deploy authority.

KIDS R8 არის reference profile, ხოლო პლატფორმის საბოლოო მიზანი provider/site-
agnostic contract-driven execution-ია. UI ამ დოკუმენტის scope-ის გარეთ რჩება;
JWT-ის implementation არსებობს, თუმცა production OIDC authority evidence ცალკე
gate-ად რჩება.

## პლატფორმის სისტემური acceptance მიზანი

ახალი საიტი უნდა დაემატოს `contract-only onboarding`-ით: კონტრაქტის დამტკიცების, mappings/keys/relations/semantics-ის რეგისტრაციისა და მონაცემთა ingest-ის შემდეგ იგივე API query, relation, publication, export, cache, audit და rollback მექანიზმები უნდა ამუშავდეს ახალი ბიზნეს-კოდის, KIDS-specific branching-ის და ახალი ფიზიკური ცხრილების შექმნის გარეშე. ნებისმიერი გამონაკლისი უნდა გამოცხადდეს ცალკე family adapter-ად და დამტკიცებულ capability profile-ად.


"ამ ნაწილს ვაფართოებ პრაქტიკული განმარტებებით: თითოეულ discovery/API მექანიზმს მივუწერე მისი დანიშნულება, ბიზნეს/ტექნიკური სარგებელი და გამოყენების შემთხვევა, რათა დოკუმენტი მხოლოდ schema reference არ იყოს და implementation      
guide-ადაც გამოდგეს."

## 12. Schema/provider-agnostic closure checklist (canonical)

Production authority-ის გარეშე დაუხურავი საკითხების ერთიანი register: [`production-blockers-and-required-authority.md`](production-blockers-and-required-authority.md).

ეს არის platform-level gap-ის მოქმედი checklist. თითოეული პუნქტი იხურება მხოლოდ implementation + automated test + independent evidence-ით; local/synthetic proof production acceptance-ს არ ანაცვლებს.

| ID | სამუშაო | შესრულებული baseline | დარჩენილი სამუშაო | დახურვის evidence | სტატუსი |
|---|---|---|---|---|---|
| A-01 | მეორე provider/site-ის სრული acceptance | local contract-only onboarding და DB replay tests | რეალური მეორე DB/provider: ingest → materialization → publication → API → export → rollback | signed end-to-end acceptance report; core-code diff empty | IMPLEMENTATION COMPLETE; PRODUCTION OPEN |
| A-02 | Provider interchangeability | pluggable family adapters და contract-bound query plan | capability registry, runtime provider discovery და provider lifecycle | provider matrix + discovery/restart/failover tests | IMPLEMENTATION PARTIAL; PRODUCTION OPEN |
| A-03 | Family lifecycle interchangeability | ENTITY/STATISTICAL/REFERENCE/RAW/GEO/RELATION adapters | ყველა family-ის ერთიანი onboarding/query/publication/rollback lifecycle | cross-family contract suite | IMPLEMENTATION PARTIAL; PRODUCTION OPEN |
| A-04 | Portability matrix | SQL identifier/input validation და provider SPI | SQL Server + MySQL + non-default schema + composite/large-table/schema evolution runs | reproducible matrix report | IMPLEMENTATION PARTIAL; PRODUCTION OPEN |
| A-05 | Statistical adapter hardening | canonical statistical adapter, registry-based dimensions/aliases | storage adapter abstraction-ის სრული separation, capability negotiation და provider-independent tests | alternate storage adapter + regression/performance evidence | IMPLEMENTATION PARTIAL; PRODUCTION OPEN |
| A-06 | Legacy fallback retirement | governed `dynamic/pages` contract engine-first path | pre-contract profile/table fallback-ის controlled deprecation/removal და migration notice | no-fallback test + compatibility release note | IMPLEMENTATION OPEN — backward compatibility intentionally retained |
| A-07 | Stable keyset hardening | HMAC cursor, tuple key, query fingerprint და snapshot scope | provider-independent benchmark, real index verification, backward cursor და concurrent-write tests | forward/backward/concurrency/throughput report | IMPLEMENTATION PARTIAL; PRODUCTION OPEN |
| A-08 | Semantic compatibility | field/relation compatibility baseline | metric/unit/aggregation/dimension/code-list/policy/projection/response breaking-change analyzer | breaking/non-breaking golden suite | IMPLEMENTATION PARTIAL; PRODUCTION OPEN |
| A-09 | Interoperability | OpenAPI, JSON Schema, TypeScript baseline, SDMX facade, JSON/CSV/NDJSON | SDMX-JSON/XML, Parquet, ZIP, Java/Kotlin/Dart SDK, streaming/progress events | format conformance + SDK contract tests | IMPLEMENTATION PARTIAL; PRODUCTION OPEN |
| A-10 | Documentation/runtime reconciliation | audit, gap და runtime evidence ფაილები | R6/R7/legacy claims-ის საბოლოო გადამოწმება და generated status ledger | zero-drift documentation check | IMPLEMENTATION PARTIAL; PRODUCTION OPEN |

### Checklist execution order

`A-01 → A-02 → A-03/A-04/A-05 → A-06 → A-07/A-08 → A-09 → A-10`.

არცერთი A-* პუნქტი არ უნდა მოინიშნოს დასრულებულად მხოლოდ დოკუმენტის არსებობის გამო; სტატუსი იცვლება მხოლოდ შესაბამისი machine-readable evidence-ისა და განმეორებადი ტესტის შემდეგ.

## 13. Partial closure ledger — portability, provider, keyset და formats

ქვემოთ მოცემული ჩანაწერები გამოყოფს ახლავე დახურულ implementation ნაწილს იმ acceptance ნაწილისგან, რომელიც რეალურ გარემოს ან conformance evidence-ს მოითხოვს.

| ID | ახლავე დახურული implementation ნაწილი | დარჩენილი acceptance ნაწილი | საბოლოო სტატუსი |
|---|---|---|---|
| A-01/P | second-provider contract-only model, composite-key/relation/classifier synthetic replay და core-code independence test | რეალური დამოუკიდებელი provider-ის ingest/materialization/publication/export/rollback | IMPLEMENTATION COMPLETE; PRODUCTION ACCEPTANCE OPEN |
| A-04/P | provider SPI; identifier/connection validation; SQL Server-compatible parameterized execution path; local portability assertions | SQL Server + MySQL + non-default schema + large-table + schema-evolution matrix run | IMPLEMENTATION PARTIAL; MATRIX EVIDENCE OPEN |
| A-07/P | HMAC cursor scope; tuple last-seen values; query fingerprint; snapshot binding; lexicographic predicate builder; bounded sort metadata | რეალური DB workload benchmark; index verification; backward traversal; concurrent insert/update integration | IMPLEMENTATION PARTIAL; BENCHMARK EVIDENCE OPEN |
| A-09/P | OpenAPI 3.1, JSON Schema, TypeScript client baseline, SDMX-shaped facade და JSON/CSV/NDJSON export | SDMX-JSON/XML, Parquet და ZIP providers; round-trip/conformance fixtures; Java/Kotlin/Dart SDK | IMPLEMENTATION PARTIAL; INTEROPERABILITY EVIDENCE OPEN |

ეს partial closure-ები ჩაითვლება სრულად დახურულად მხოლოდ მაშინ, როცა შესაბამისი acceptance evidence დაემატება; production authority-ის არქონა ამ implementation ცვლილებების გაკეთებას არ ბლოკავს.
