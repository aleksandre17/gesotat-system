# GeoStat Access-Driven Data Platform — სრული Master Plan (ჩანაცვლებულია)

> სტატუსი: არქიტექტურული baseline. ეს დოკუმენტი ანაცვლებს ძველი გეგმის იმ გადაწყვეტილებას, რომლის მიხედვითაც Access მხოლოდ უკვე შექმნილ target table-ში წერდა.

Core `geostat-system` და CIDS `kids` ბაზების რეალურ inventory-ზე დაფუძნებული physical schema მოცემულია [Core + KIDS (CIDS) — სრული Schema Draft](core-kids-full-schema-draft.md)-ში.

საერთაშორისო სტანდარტებისა და პატერნების შერჩეული, ურთიერთთავსებადი საფუძველი მოცემულია [International Standards Reference Architecture](international-standards-reference-architecture.md)-ში. საბოლოო „ერთიანი ფიზიკური schema / metadata-driven canonical record graph“ გადაწყვეტილება მოცემულია [Unified Canonical Data Platform — საბოლოო Reference Model](unified-canonical-platform-final.md)-ში.

## 1. მიზანი

ეს არის არა მხოლოდ KIDS-ის იმპორტის გეგმა, არამედ მრავალსაიტიანი, schema-agnostic და contract-driven GeoStat პლატფორმის master plan. KIDS წარმოადგენს პირველ reference implementation-ს; ყველა შემდგომი საიტი, dataset და მონაცემთა ოჯახი უნდა ჩაერთოს იგივე კანონიკური კონტრაქტითა და execution მექანიზმებით, ახალი ბიზნეს-branching-ისა და ახალი ფიზიკური schema-ის აუცილებლობის გარეშე.

ერთმა Managed Access package-მა უნდა შეძლოს ერთდროულად გადაიტანოს:

1. raw / content / operational data (`files`, `glossary`, `goals`);
2. statistical data (dimensions, measures, observations);
3. schema, key, index და relation აღწერა;
4. ვალიდაციის წესები;
5. chart-ის დეკლარაციები და filter-ები.

შედეგი არ არის უბრალოდ `.accdb` export. შედეგია მართვადი **Data Product**, რომლის data, API, table view და chart view ერთი publication version-ით მუშაობს.

```text
Managed Access Package
       ↓
catalog → manifest validation → approved provisioning plan
       ↓
staging schema → data load → quality checks
       ↓
atomic publication → Data Product API → tables / charts / exports
```

## 2. უცვლელი არქიტექტურული პრინციპები

| # | გადაწყვეტილება | მიზეზი |
|---|---|---|
| P1 | Access არის transport + manifest, არა production DB | source ფაილი ვერ ფლობს production connection secret-ს |
| P2 | Core registry არის authority | package ვერ ირჩევს server-ს ან თვითნებურ target-ს |
| P3 | package აღწერს schema-ს, მაგრამ Core profile განსაზღვრავს სად შეიძლება შექმნა | მოქნილობა და უსაფრთხოება ერთად |
| P4 | ყველა import იწყება staging-ში | ნაწილობრივი ჩაწერა public table-ში დაუშვებელია |
| P5 | publish არის ატომური pointer/swap | reader ვერ ხედავს ნახევრად ატვირთულ მონაცემს |
| P6 | raw და statistical data ერთი Data Product model-ის ორი dataset kind-ია | ერთი პლატფორმა, არა ორი პარალელური სისტემა |
| P7 | chart არის declarative metadata | Access-ში SQL, connection string და executable code აკრძალულია |
| P8 | ყველა schema/data/chart ცვლილება versioned და auditableა | rollback და repeatability |
| P9 | ერთი package შეიცავს ბევრ dataset-ს, ერთი dataset გამოიყენება ბევრ chart-ში | მრავალცხრილიანი რეალური დომენების მხარდაჭერა |
| P10 | destructive ცვლილება მხოლოდ explicit policy + backup-ით | production data-ის დაცვა |

## 3. ძირითადი domain მოდელი

```text
DataProduct (მაგ.: CIDS ბავშვების პორტალი)
 ├─ ProductVersion
 ├─ Dataset[]
 │   ├─ DatasetSchemaVersion
 │   ├─ Field[]
 │   ├─ Key[] / Index[] / Relation[]
 │   ├─ DatasetKind = RAW | STATISTICAL | LOOKUP | CONTENT | GEO
 │   ├─ PhysicalTarget
 │   └─ PublicationVersion
 ├─ Dimension[]
 ├─ Measure[]
 ├─ ChartDefinition[]
 └─ ImportJob[]
```

### 3.1 Dataset kind

| Kind | მაგალითი | ფიზიკური model |
|---|---|---|
| `RAW` | event, form, file metadata | source columns უცვლელად |
| `CONTENT` | CIDS `files`, `glossary`, `goals` | მრავალენოვანი ტექსტი/JSON |
| `LOOKUP` | country, age group, category | code + label + hierarchy |
| `STATISTICAL` | indicator/year/region/value | wide ან normalized observation |
| `GEO` | მუნიციპალიტეტი, geometry link | geography key + measures |

`RAW` არ კონვერტირდება ძალით statistical ფორმატში. `STATISTICAL` შეიძლება იყოს ფართო source table, მაგრამ platform-ში აქვს dimension/measure metadata.

## 4. Managed Access Package v2

ყველა v2 ფაილს აქვს data table-ები და ტექნიკური `__gs_*` table-ები. Metadata table-ები package-ის ნაწილია და არ იტვირთება target business schema-ში.

```text
__gs_package
__gs_dataset
__gs_field
__gs_key
__gs_index
__gs_relation
__gs_validation_rule
__gs_chart
__gs_chart_series
__gs_chart_filter
__gs_publication

<რეალური data tables>
```

### 4.1 `__gs_package`

ერთი row.

| Field | აღწერა |
|---|---|
| `package_code` | immutable logical package code |
| `package_version` | semantic version, მაგ. `2.0.0` |
| `product_code` | Core DataProduct code, მაგ. `cids` |
| `manifest_version` | `2` |
| `schema_change_mode` | `CREATE_ONLY`, `ADD_ONLY`, `MIGRATE` |
| `requested_publication` | `DRAFT`, `REVIEW`, `PUBLISH` |
| `source_name` | მონაცემის ორგანიზაცია/წყარო |
| `created_at` | source timestamp |

### 4.2 `__gs_dataset`

ერთი row data table-ზე.

| Field | აღწერა |
|---|---|
| `dataset_code` | package-ში უნიკალური code |
| `access_table_name` | Access data table name |
| `profile_code` | Core-ში წინასწარ დამტკიცებული target policy |
| `dataset_kind` | `RAW`, `CONTENT`, `LOOKUP`, `STATISTICAL`, `GEO` |
| `target_logical_name` | public API-ის logical dataset სახელი |
| `load_mode` | `SNAPSHOT_REPLACE`, `UPSERT`, `APPEND` |
| `required` | required dataset ჩავარდნისას package არ ქვეყნდება |
| `source_row_key` | Access source key |
| `target_schema_policy` | `USE_PROFILE_SCHEMA`, არა connection detail |

### 4.3 `__gs_field`

ერთი row თითო source column-ზე.

| Field | აღწერა |
|---|---|
| `dataset_code`, `source_field` | dataset/Access column |
| `target_field` | target column name |
| `logical_type` | `STRING`, `INTEGER`, `DECIMAL`, `BOOLEAN`, `DATE`, `DATETIME`, `JSON`, `GEO_KEY` |
| `role` | `IDENTIFIER`, `DIMENSION`, `MEASURE`, `LABEL`, `CONTENT`, `JSON`, `SYSTEM` |
| `nullable`, `max_length`, `precision`, `scale` | schema contract |
| `label_ka`, `label_en`, `unit`, `format` | UI/chart semantics |
| `is_filterable`, `is_groupable`, `is_visible` | dynamic API policy |

### 4.4 Key, index, relation

- `__gs_key`: primary/business/composite key; field order.
- `__gs_index`: approved index name, fields, unique flag.
- `__gs_relation`: `from_dataset/from_field → to_dataset/to_field`, cardinality and required flag.

Foreign key რეალურად იქმნება მხოლოდ მაშინ, როცა ორივე target dataset ერთ database/schema boundary-შია და profile ამას უშვებს. სხვა შემთხვევაში relation metadata-only რჩება.

### 4.5 Validation

`__gs_validation_rule` შეიცავს declarative წესებს: `NOT_NULL`, `UNIQUE`, `RANGE`, `REGEX`, `ENUM`, `FOREIGN_KEY_EXISTS`, `ROW_COUNT_MIN`, `ROW_COUNT_CHANGE_MAX_PERCENT`. SQL ტექსტი აკრძალულია.

### 4.6 Charts

`__gs_chart` აღწერს chart-ს: `chart_code`, `dataset_code`, `chart_type`, title-ka/en, x/y/series fields, aggregation, sort, publication mode.

`__gs_chart_series` აღწერს მრავალ measure-ს, label/color/order-ს.

`__gs_chart_filter` აღწერს `EQ`, `NE`, `IN`, `BETWEEN`, `IS_NULL`, `NOT_NULL`; ყველა field უნდა იყოს `is_filterable=true`.

დასაშვები v2 chart ტიპები: `LINE`, `BAR`, `STACKED_BAR`, `AREA`, `STACKED_AREA`, `PIE`, `DONUT`, `SCATTER`, `TABLE`, `KPI`, `MAP`, `HEATMAP`.

## 5. Core Registry და უფლებები

Core-ში ემატება/გაფართოვდება:

```text
data_products
data_product_versions
data_profiles
dataset_schema_versions
dataset_fields
dataset_keys
dataset_indexes
dataset_relations
chart_definitions
chart_versions
import_jobs
import_job_items
schema_migration_jobs
publication_versions
```

### 5.1 Profile არის target provisioning policy

Profile არ არის უბრალოდ table name. მას აქვს:

```text
product_code
target_connection_ref           # secret server-side
target_database_policy          # FIXED_DATABASE | CREATE_DATABASE_ALLOWED
target_schema_policy            # FIXED_SCHEMA | PRODUCT_SCHEMA
target_table_policy             # FIXED_TABLE | VERSIONED_TABLE
ddl_policy                      # NONE | CREATE_ONLY | ADDITIVE_ONLY | APPROVED_MIGRATION
allowed_dataset_kinds
max_rows / max_file_size
allowed_logical_types
publication_required
retention/backup policy
```

`CREATE_DATABASE_ALLOWED` არ არის default. იგი მოითხოვს administrator approval-ს, სახელის template-ს, quota-ს და isolated database account-ს.

### 5.2 როლები

| როლი | უფლება |
|---|---|
| `DATA_AUTHOR` | Access package-ის მომზადება/preview |
| `DATA_STEWARD` | schema/quality განხილვა, DRAFT chart |
| `DATA_ENGINEER` | profile, provisioning policy, migration review |
| `DATA_PUBLISHER` | publication/rollback |
| `DB_ADMIN` | database creation და connection policy |
| `VIEWER` | მხოლოდ published table/chart/API |

## 6. Schema provisioning

### 6.1 რას აკეთებს engine

1. კითხულობს Access-ის ნამდვილ schema-ს და manifest-ის `__gs_field` contract-ს.
2. ადარებს მათ ერთმანეთს; დაუდეკლარირებელი column, განსხვავებული type ან duplicate key აჩერებს import-ს.
3. ადარებს package schema-ს ბოლო published schema version-ს.
4. ქმნის კონკრეტულ DDL plan-ს; plan ჩანს preview-ში.
5. მხოლოდ profile `ddl_policy`-ის ფარგლებში ქმნის database/schema/table/index/constraint-ს.
6. ქმნის staging target-ს, არა public target-ს.

### 6.2 Migration classification

| ცვლილება | Default action |
|---|---|
| ახალი table | `CREATE_ONLY`-ით ავტომატური |
| ახალი nullable column | `ADDITIVE_ONLY`-ით ავტომატური |
| ახალი index | ავტომატური review-ის შემდეგ |
| column type გაფართოება | explicit approval |
| column rename/drop | migration + backup + approval |
| table drop | default აკრძალული |
| database create | მხოლოდ `CREATE_DATABASE_ALLOWED` |

DDL არასოდეს გენერირდება Access-ში შენახული raw SQL-იდან. იგი იქმნება typed manifest + Core policy-ით.

## 7. Import lifecycle

```text
RECEIVED
  → CATALOGED
  → MANIFEST_VALIDATED
  → PROVISIONING_PLANNED
  → APPROVED
  → STAGING_CREATED
  → LOADING
  → QUALITY_CHECKED
  → PUBLISHED | REVIEW_REQUIRED | FAILED | ROLLED_BACK
```

### 7.1 Preview

`POST /api/v2/imports/access/preview` არაფერს წერს child DB-ში. პასუხში აბრუნებს:

- dataset/table/row/column inventory;
- source vs manifest vs current schema diff;
- DDL plan;
- target policy და required approval;
- validation issues;
- chart compilation preview;
- impact summary: create/add/migrate/reject.

### 7.2 Execute

1. immutable file checksum და package version ინახება audit-ში;
2. schema plan ხელახლა მოწმდება approve token-ით;
3. იქმნება versioned staging tables: `stg_<product>_<dataset>_<job>`;
4. data იტვირთება batch-ებით;
5. row count, key, relation, type და domain validation სრულდება;
6. chart queries კომპილირდება და dry-run გადის;
7. required dataset წარმატებისას ხდება publication;
8. ძველი published version რჩება rollback-ისთვის.

### 7.3 Publication

DB-ის შესაძლებლობიდან გამომდინარე გამოიყენება ერთ-ერთი:

- stable view (`product.dataset`) → ახალი physical version table;
- synonym/pointer switch;
- staging-to-live transaction rename/swap.

Reader API მხოლოდ publication pointer-ს კითხულობს. ამიტომ table და chart ყოველთვის იმავე publication version-ს ხედავს.

## 8. Raw და statistical data-ის გაერთიანება

### 8.1 Raw/content

`files`, `glossary`, `goals` და სხვა მსგავსი table ინახება როგორც არის. მრავალენოვანი ტექსტი, JSON (`chartdata`) და business ID არ იკარგება.

### 8.2 Statistical

ორი დასაშვები ფორმა:

```text
Wide source:      year | age_0_17 | age_15_24 | age_15_29
Normalized source: year | age_group | value
```

package შეიძლება შეიცავდეს wide source table-ს; manifest `__gs_field.role` განსაზღვრავს measure/dimension-ს. პლატფორმა ქმნის:

- source-faithful physical table;
- სურვილისამებრ generated normalized analytical view;
- ერთიან semantic layer-ს chart/API-ისთვის.

ასე raw data არ ზიანდება, ხოლო chart-ს შეუძლია measure/dimension საფუძველზე მუშაობა.

### 8.3 ერთი row → ბევრი chart

Chart row-სთან არ მიბმება. ChartDefinition მიბმულია dataset + fields-ზე:

```text
cids_population_observation
  ├─ population-by-year (LINE)
  ├─ age-groups-by-year (STACKED_AREA)
  ├─ latest-child-population (KPI)
  └─ age-group-table (TABLE)
```

## 9. CIDS / ბავშვების პორტალის სამიზნე მოდელი

პირდაპირი source export აჩვენებს ოთხ source table-ს: `files`, `glossary`, `goals`, `goals_titles`.

### 9.1 პირველი Data Product

```text
product_code: cids
target database: kids (initially fixed, no auto-create database)
target schema: cids
ddl_policy: CREATE_ONLY შემდეგ ADDITIVE_ONLY
publication: REVIEW_REQUIRED
```

Dataset-ები:

| Dataset | Kind | Key | მიზანი |
|---|---|---|---|
| `cids_files` | CONTENT | `ID` | multilingual chart/content records; `chartdata` JSON preserved |
| `cids_glossary` | CONTENT/LOOKUP | `ID` | glossary entries |
| `cids_goals` | CONTENT | `ID` | goal content |
| `cids_goal_titles` | LOOKUP | `ID` | category title mapping |

შემდეგ ეტაპზე `files.chartdata`-დან იქმნება ცალკე declarative datasets მხოლოდ მას შემდეგ, რაც data steward დაადასტურებს თითო chart-ის title, unit, source, dimensions და measures. JSON-ის ავტომატურად „chart_1“-ად გადაქცევა აკრძალულია.

## 10. API contract

```text
POST /api/v2/imports/access/preview
POST /api/v2/imports/access/{previewId}/approve
POST /api/v2/imports/access/{previewId}/execute
GET  /api/v2/import-jobs/{jobId}
GET  /api/v2/products/{productCode}/datasets/{datasetCode}/data
GET  /api/v2/products/{productCode}/charts/{chartCode}
POST /api/v2/products/{productCode}/publications/{version}/publish
POST /api/v2/products/{productCode}/publications/{version}/rollback
```

Table API იღებს მხოლოდ manifest/profile-ით ნებადართულ projection/filter/sort/page პარამეტრებს. Chart API იყენებს მხოლოდ published declarative definitions.

## 10.1 External ingress და format interoperability

პლატფორმა თავიდანვე მუშაობს adapter contract-ით და არ არის მიბმული მხოლოდ Access-ზე:

```text
ACCDB/MDB, CSV, XLSX, JSON, XML, GeoJSON, GeoPackage, Parquet,
SQL Server/MySQL/PostgreSQL extraction, REST, SDMX REST, OGC API
```

ყველა adapter ერთიან `SourceCatalog` შედეგს აბრუნებს: dataset, field, type, key, relation/nesting, row count, checksum და source schema. შემდეგი flow ყოველთვის ერთია: landing → contract validation → staging → canonical/semantic → publication. დეტალური standards/adapter model მოცემულია [International Standards Reference Architecture](international-standards-reference-architecture.md).

## 11. უსაფრთხოება და გამძლეობა

- Access package-ში არ არის host, user, password, SQL ან executable expression;
- identifier-ები whitelist/regex + profile schema policy-ით;
- value-ები ყოველთვის prepared statement-ით;
- DDL plan ხელმოწერილია preview checksum-ზე; execute ვერ იყენებს შეცვლილ package-ს;
- თითო job-ს აქვს correlation ID, checksum, actor, schema plan, row counts და errors;
- staging failure არ ცვლის published version-ს;
- DDL migration-მდე backup snapshot; rollback არის version pointer switch;
- upload size, row count, column count, text length, execution timeout და concurrency quota profile-ით.

## 12. განხორციელების ეტაპები

### Phase 0 — Corrective design

- [ ] ძველი გეგმის „pre-existing table only“ გადაწყვეტილების გაუქმება;
- [ ] Managed Access Package v2 schema/JSON Schema/OpenAPI contract;
- [ ] CIDS Data Product და ოთხი dataset profile-ის დამტკიცება;
- [ ] DDL policy და production DBA approval workflow.

### Phase 1 — Core metadata

- [ ] `data_products`, schema version, fields, keys, indexes, relations, publication version entities/migrations;
- [ ] profile provisioning-policy გაფართოება;
- [ ] immutable versioning და approval model;
- [ ] admin UI/API CRUD.

### Phase 2 — Access v2 parser and planner

- [ ] სრული `__gs_*` manifest reader;
- [ ] Access physical schema vs manifest validator;
- [ ] schema-diff / DDL-plan engine;
- [ ] chart grammar compiler;
- [ ] preview endpoint და approval token.

### Phase 3 — Provisioning and import

- [ ] MSSQL schema/table/index/constraint generator;
- [ ] MySQL equivalent;
- [ ] isolated database creation policy;
- [ ] staging load, typed conversion, batch/retry;
- [ ] snapshot replace/upsert/append policies;
- [ ] transaction, backup, rollback and job audit.

### Phase 4 — Semantic and chart layer

- [ ] wide-to-normalized analytical view generator;
- [ ] dataset table API: projection/filter/sort/pagination/export;
- [ ] declarative chart query compiler for all v2 chart types;
- [ ] chart/table same publication version and cache policy.

### Phase 5 — CIDS pilot

- [ ] build true CIDS v2 package from `kids` source;
- [ ] create `cids` target schema from package plan;
- [ ] load 4 datasets into staging and publish;
- [ ] catalog `chartdata` semantics with data steward;
- [ ] publish first approved CIDS charts;
- [ ] UAT: upload → preview → approval → import → table → chart → rollback.

### Phase 6 — rollout

- [ ] MSSQL and MySQL contract tests;
- [ ] one raw/content, one lookup, one statistical, one geo product;
- [ ] performance tests on large child datasets;
- [ ] monitoring, backup restore drill, security review;
- [ ] canary product-by-product production rollout.

## 13. Definition of Done

სისტემა დასრულებულია მხოლოდ მაშინ, როცა:

- [ ] ერთი v2 Access file ქმნის დამტკიცებულ target schema/table/index-ებს;
- [ ] იმავე ფაილში RAW, CONTENT, LOOKUP და STATISTICAL dataset-ები ერთად იტვირთება;
- [ ] schema violation preview-ზე ჩანს და live DB არ იცვლება;
- [ ] import staging-ში სრულდება და atomic publish აქვს;
- [ ] ერთი dataset მინიმუმ ორ published chart-ში გამოიყენება;
- [ ] table/chart/export ერთ publication version-ს იყენებს;
- [ ] CIDS `files`, `glossary`, `goals`, `goals_titles` რეალურად იტვირთება ამ flow-ით;
- [ ] CIDS-ის მინიმუმ ერთი `chartdata` სემანტიკურად გარდაიქმნება declarative chart-ად;
- [ ] MSSQL + MySQL + non-default schema coverage არსებობს;
- [ ] rollback, backup restore, authorization და audit UAT გავლილია;
- [ ] legacy endpoint-ები regression-ით უცვლელად მუშაობს.

## 14. რაც არ უნდა გავაკეთოთ

- არ შევქმნათ `chart_1`, `chart_2` ტიპის უსემანტიკო business tables;
- არ გადავაქციოთ ყველა raw table იძულებით observation ფორმატში;
- არ მივცეთ Access-ს arbitrary SQL/host/password/DDL;
- არ შევიტანოთ package პირდაპირ public table-ში staging-ის გარეშე;
- არ გამოვაცხადოთ სისტემა დასრულებულად მხოლოდ იმიტომ, რომ importer row-ებს წერს.
