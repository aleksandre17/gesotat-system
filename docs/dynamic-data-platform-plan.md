# Dynamic Data Platform — ეტაპობრივი გეგმა (ჩანაცვლებულია)

> ეს დოკუმენტი აღწერს ძველ, უსაფრთხო `import-to-existing-table` მიდგომას. სრული Access-driven architecture, რომელიც ქმნის დამტკიცებულ schema/table/index სტრუქტურას და აერთიანებს raw/statistical data-ს, მოცემულია [Access-Driven Data Platform — სრული Master Plan](access-driven-data-platform-master-plan.md)-ში.

## 0. დოკუმენტის სტატუსი

- სტატუსი: **დასაგეგმი / ჯერ არ დაწყებულა**
- მიზანი: არსებული GeoStat სისტემის გვერდით შეიქმნას metadata-driven მონაცემთა და ვიზუალიზაციის ფენა, legacy რეჟიმის შეწყვეტის გარეშე.
- პრინციპი: `core` მართავს metadata-სა და პროცესს; child ბაზები ინახავენ რეალურ მონაცემებს.
- აკრძალვა: ამ პროგრამის ფარგლებში არ იშლება და არ იცვლება მოქმედი legacy endpoint ან child table მიგრაციის გარეშე.

## 1. არსებული სისტემის დადასტურებული სურათი

### 1.1 Core registry

- Primary registry DB: `geostat-system`.
- `page_nodes` შეიცავს 85 node-ს.
- `PAGE` node უკვე ინახავს logical child source-ს: database type + `database-table` სახელით.
- არსებობს MSSQL და MySQL child ბაზები.

### 1.2 რეალური child data-ის ტიპები

| კატეგორია | მაგალითები | დასკვნა |
|---|---|---|
| დიდი fact table | `auto.eoyes` (~10.6 მლნ), `trade1.trade_data` (~4.7 მლნ) | საჭიროა pagination, indexed query და server-side aggregation |
| ნორმალური სტატისტიკური table | FDI, fuel, road, licenses | პირდაპირი table/chart profile შესაძლებელია |
| wide table | `calculator.subgroupindex` (45 column) | პირველ ეტაპზე რჩება wide; chart config ირჩევს series columns |
| reference/lookup | ქვეყანა, სექტორი, საწვავი, რეგიონი | გამოიყენება label, filter და chart legend-ისთვის |
| სპეციალური schema | `geomap.geomap.*` | schema არ შეიძლება იყოს hard-coded `dbo` |

### 1.3 არსებული შეზღუდვები

1. `AccessFileImporter` ამჟამად კითხულობს Access-ის მხოლოდ პირველ table-ს.
2. SQL Server import სტრატეგია `database/table` target-ს `dbo` schema-ს უკავშირებს; `geomap` table-ებისთვის ეს არასწორია.
3. Legacy file upload payload-ში გამოიყენება პირდაპირი connection metadata; ახალ რეჟიმში target უნდა გადაწყვიტოს მხოლოდ `core`-ის დამტკიცებულმა profile-მა.
4. Chart-ის metadata ჯერ არ არის metadata registry-ში; chart data სხვადასხვა ფორმით არსებობს.

## 2. საბოლოო არქიტექტურული გადაწყვეტილებები

| # | საკითხი | გადაწყვეტილება | მიზეზი | შემოწმების კრიტერიუმი |
|---|---|---|---|---|
| D1 | სისტემის ევოლუცია | Strangler migration: `LEGACY → HYBRID → DYNAMIC` | მომსახურება არ ჩერდება | არსებული endpoint-ები regression-ის გარეშე მუშაობს |
| D2 | მონაცემის საცავი | Core = metadata/audit; child DB = რეალური rows | მილიონობით ფაქტის core-ში კოპირება საჭირო არაა | existing child table არ გადადის ახალ DB-ში |
| D3 | Access ფაილის ფორმა | Managed Access Package: data tables + `__gs_*` metadata tables | ერთი ფაილი აღწერს import-სა და chart-ებს | ერთი file იტვირთება მრავალ table-ად |
| D4 | Legacy files | metadata table-ის არმქონე ფაილი ძველი გზით მუშავდება | უკუთავსებადობა | legacy upload არ იცვლება |
| D5 | Target routing | Access package მიუთითებს მხოლოდ logical `profile_code`; DB/schema/table არის core-ში | უსაფრთხოება და კონტროლი | ფაილი ვერ წერს დაურეგისტრირებელ target-ში |
| D6 | Table-to-chart კავშირი | Declarative query rule, არა ფიზიკური `row_id ↔ chart_id` | row შეიძლება რამდენიმე chart-ში იყოს და მილიონობით კავშირი არ გვჭირდება | ერთი row სხვადასხვა aggregation/filter chart-ში ჩანს კოპირების გარეშე |
| D7 | Wide tables | თავდაპირველად direct storage + selected columns; შემდეგ optional `UNPIVOT` | ნაკლები რისკი | 45-column table table/chart რეჟიმში მუშაობს |
| D8 | მრავალ-DB ატვირთვა | transaction თითო target table-ზე; import job აერთიანებს სტატუსებს | cross-DB global transaction არასაიმედოა | ერთი table-ის შეცდომა სხვათა audit-ს არ აზიანებს |
| D9 | Schema support | `target_schema` სავალდებულო profile/mapping ველია | `dbo` ყველა ბაზისთვის სწორი არაა | `geomap.geomap.*` import/query მუშაობს |
| D10 | Chart სისტემა | Declarative grammar + adapter registry | ახალი chart type არ მოითხოვს import მოდელის შეცვლას | line/bar/pie/area პირველ ეტაპზე, შემდეგ adapter-ები |
| D11 | უსაფრთხო query | მხოლოდ profile-ში დაშვებული columns, operators და aggregations | SQL injection და არასწორი query თავიდან იცილება | raw SQL frontend/API-დან არ მიიღება |
| D12 | Versioning | immutable import job + profile/chart version | შედეგის აღდგენა და audit | კონკრეტული chart პასუხი უკავშირდება import version-ს |

## 3. Managed Access Package v1

### 3.1 სავალდებულო ტექნიკური table-ები

| Access table | დანიშნულება |
|---|---|
| `__gs_package` | package ID, schema version, release, publisher, checksum |
| `__gs_dataset` | Access data table-ის logical პროფილი და data kind |
| `__gs_chart` | chart-ის data binding და presentation ტიპი |
| `__gs_chart_filter` | chart-ის declarative filter წესები |

### 3.2 არჩევითი ტექნიკური table-ები

| Access table | დანიშნულება |
|---|---|
| `__gs_column` | column-ის semantic role, label, unit, ka/en metadata |
| `__gs_relationship` | source table-ების უსაფრთხო კავშირები |
| `__gs_validation` | package-level data quality წესები |

### 3.3 `__gs_dataset` კონტრაქტი

| ველი | მაგალითი | დანიშნულება |
|---|---|---|
| `dataset_code` | `AUTO_MAIN` | file-ის შიგნით სტაბილური ID |
| `access_table_name` | `auto_main` | Access-ის რეალური table |
| `profile_code` | `cars-current-period` | core-ში დამტკიცებული target profile |
| `data_kind` | `BOTH` | `TABLE`, `STATISTICAL`, `BOTH`, `LOOKUP`, `AUXILIARY` |
| `row_key` | `id` | primary/business key |
| `required` | `true` | ჩავარდნისას publication უნდა დაიბლოკოს თუ არა |

### 3.4 `__gs_chart` კონტრაქტი

| ველი | მაგალითი |
|---|---|
| `chart_code` | `cars_by_fuel` |
| `dataset_code` | `AUTO_MAIN` |
| `chart_type` | `line` |
| `x_field` | `year` |
| `y_field` | `quantity` |
| `series_field` | `fuel` |
| `aggregation` | `SUM` |
| `publication_mode` | `DRAFT` ან `PUBLISH` |

### 3.5 Chart filter კონტრაქტი

`__gs_chart_filter` არ შეიცავს SQL ტექსტს.

| chart_code | field | operator | value |
|---|---|---|---|
| `tbilisi_cars_by_year` | `region` | `EQ` | `Tbilisi` |
| `fuel_2024` | `year` | `EQ` | `2024` |

დაშვებული operators v1: `EQ`, `NE`, `IN`, `BETWEEN`, `IS_NULL`, `NOT_NULL`.

## 4. Core database მოდელი

### 4.1 ახალი ცხრილები

```text
data_profiles
import_table_mappings
chart_definitions
import_jobs
import_job_items
```

### 4.2 პასუხისმგებლობები

| Core table | პასუხისმგებლობა |
|---|---|
| `data_profiles` | Page-ის mode, target DB/schema/table, row key, query whitelist |
| `import_table_mappings` | Access table → profile/target/import mode |
| `chart_definitions` | versioned chart grammar და publication status |
| `import_jobs` | ატვირთვის საერთო lifecycle |
| `import_job_items` | ერთი Access table-ის კონკრეტული შედეგი |

### 4.3 აუცილებელი სტატუსები

```text
ImportJob: RECEIVED, CATALOGED, VALIDATING, VALIDATED, IMPORTING, PARTIAL_SUCCESS,
           SUCCESS, FAILED, CANCELLED

ImportJobItem: MAPPED, UNMAPPED, VALIDATING, IMPORTING, SUCCESS, FAILED,
               SKIPPED

Publication: DRAFT, PENDING, PUBLISHED, ARCHIVED
```

## 5. Import pipeline

```text
1. ფაილის მიღება და checksum
2. Access catalog scan: ყველა table, row count, columns
3. package type-ის ამოცნობა: LEGACY ან MANAGED
4. mapping/profile resolution
5. schema + column + rule validation
6. child DB table import, transaction თითო target table-ზე
7. import audit-ის ჩაწერა core-ში
8. chart definition validation
9. publication ან PENDING მდგომარეობა
10. WebSocket progress + საბოლოო report
```

## 6. მომსახურებები და პატერნები

| კომპონენტი | პატერნი | პასუხისმგებლობა |
|---|---|---|
| `AccessCatalogService` | Adapter | Access table/column metadata-ის ამოკითხვა |
| `ImportOrchestrator` | Facade / Template Method | lifecycle-ის კოორდინაცია |
| `MappingResolver` | Registry | logical profile → target გადაწყვეტა |
| `TargetResolver` | Strategy | MSSQL/MySQL + schema-qualified target |
| `TableImportExecutor` | Strategy | REPLACE/APPEND/UPSERT batch import |
| `ImportAuditService` | Repository | immutable შედეგები და audit |
| `DynamicDataService` | Query Object | page table/filter/pagination API |
| `ChartQueryService` | Strategy + Adapter | chart config → safe aggregate query |
| `ChartAdapterRegistry` | Registry | chart type → response adapter |

## 7. Dynamic API კონტრაქტები

```text
POST /api/v2/imports/access
GET  /api/v2/imports/{jobId}
GET  /api/v2/pages/{pageId}/profile
GET  /api/v2/pages/{pageId}/data
GET  /api/v2/pages/{pageId}/charts
GET  /api/v2/pages/{pageId}/charts/{chartCode}/data
```

`/data` და `/charts/*/data` მუშაობს მხოლოდ `data_profiles` whitelist-ით.

## 8. Chart მხარდაჭერის ეტაპები

### v1

`table`, `line`, `bar`, `stacked-bar`, `area`, `pie`, `donut`.

### v2

`scatter`, `bubble`, `heatmap`, `treemap`, `waterfall`, `radar`, `gauge`.

### v3

`map`, `sankey`, `funnel`, `combined`, `custom adapter`.

ყველა chart გადის field/type validator-ს. Chart არ აიგება, თუ dataset-ს არ აქვს საჭირო dimensions/measures.

## 9. მიგრაციის სტრატეგია

```text
LEGACY
  არსებული endpoint და importer

HYBRID
  ძველი child table + ახალი profile/data/chart API

DYNAMIC
  ახალი Managed Access Package + სრული dynamic API
```

### რეკომენდებული pilot

პირველი პილოტი: `international-ratings.main_economic_indicator`.

მეორე პილოტი: `fdi_new.fdi_data`.

მიზეზი: მცირე მოცულობა, რეალური სტატისტიკური data, chart-ისთვის კარგი dimensions/measures, და დაბალი operational risk; მეორე pilot დამატებით ამოწმებს MySQL target-სა და lookup კავშირებს.

## 10. შესრულების ფაზები

### Phase A — Specification and migrations

- [x] Managed Access Package v1-ის საწყისი schema/კონტრაქტი
- [x] Managed Access Package v1-ის Access authoring specification
- [x] Core DB idempotent SQL Server migration script
- [x] Production migration review და rollout (2026-09-08)
- [x] Java entity/repository/service საწყისი კონტრაქტები
- [ ] OpenAPI და JSON Schema
- [ ] უსაფრთხოებისა და rollback-ის checklist

### Phase B — Core Registry

- [x] Core JPA tables/entities (runtime schema foundation)
- [x] CRUD/API data profile-ისთვის
- [x] CRUD/API mapping და chart definition-ისთვის
- [x] profile/chart versioning (profile/chart ცვლილება ზრდის version-ს; package chart ინახება ახალი DRAFT version-ით)
- [x] role/permission კონტროლი (registry write, managed import write და dynamic read endpoint-ებზე authority შემოწმება)

### Phase C — Multi-table import

- [x] ყველა Access table-ის read-only catalog scan
- [x] managed/legacy mode resolver (ახალი endpoint იღებს მხოლოდ self-describing package-ს; legacy endpoint უცვლელია)
- [x] schema-qualified MSSQL import (target resolver გადასცემს `database/schema/table` ფორმას)
- [x] MySQL target resolution, მათ შორის დეფისიანი database name
- [x] per-table import audit (WebSocket progress რჩება შემდეგ ეტაპად)

### Phase D — Dynamic read layer

- [ ] secure filter/sort/pagination
- [x] read-only, paginated data endpoint
- [ ] export კონტრაქტი
- [ ] large-table indexes/performance tests

### Phase E — Dynamic charts

- [x] v1 chart grammar validation (ტიპი, dataset ref, უსაფრთხო filter operator)
- [x] v1 aggregate chart query API (`line`, `bar`, `stacked-bar`, `area`, `pie`, `donut` definitions)
- [x] chart data API
- [ ] chart/table shared filters

### Phase F — Pilot and rollout

- [x] `main-economic-indicator` pilot profile HYBRID რეჟიმში (2026-09-08)
- [x] API runtime startup/health/authentication acceptance check (2026-09-08)
- [ ] authorized upload → data → chart acceptance test
- [ ] monitoring და rollback drill
- [ ] შემდეგი profile-ების ეტაპობრივი migration

## 11. ხარისხის კარიბჭეები

ფაზა არ გადადის შემდეგ ეტაპზე, სანამ:

1. legacy import regression test არ არის მწვანე;
2. schema/table/column whitelist არ არის შემოწმებული;
3. import job report არ აჩვენებს თითო table-ის შედეგს;
4. import failure არ ტოვებს დაუდასტურებელ publication-ს;
5. დიდი table-ზე pagination და aggregate query-ის performance არ არის გაზომილი;
6. chart response-ის მონაცემი ემთხვევა SQL aggregate control query-ს;
7. rollback გზა დოკუმენტირებული და გამოცდილი არ არის.

## 12. სტანდარტები და მართვის პრინციპები

- SDMX-ის მსუბუქი სემანტიკა: dataset, dimension, measure, codelist, dataflow.
- ISO/IEC 11179-ის პრინციპი: field/indicator-ის განმარტებული, რეგისტრირებადი metadata.
- OpenAPI: public/admin API კონტრაქტები.
- JSON Schema: package და chart definition validation.
- OWASP: secrets არ მიდის Access ფაილში ან frontend payload-ში.
- Auditability: import job, file checksum, profile/chart version, publication status.

## 13. მიღებული ოპერაციული გადაწყვეტილებები

ეს არჩევანი ითვლება საწყის default პოლიტიკად. მათი შეცვლა შესაძლებელია მხოლოდ versioned configuration ცვლილებით და audit ჩანაწერით.

| საკითხი | მიღებული გადაწყვეტილება | მიზეზი |
|---|---|---|
| პირველი pilot | `international-ratings.main_economic_indicator` | მცირე, რეალური სტატისტიკური table; აქვს country/year/measures და chart-ისთვის დაბალი რისკი |
| შემდეგი pilot | `fdi_new.fdi_data` | ამოწმებს MySQL target-ს, lookup კავშირებს და მრავალ chart-ს |
| package owner | domain data owner ქმნის/ავსებს Access package-ს; data steward ამოწმებს metadata-ს | პასუხისმგებლობა დომენზეა, platform იცავს კონტრაქტს |
| profile/mapping owner | data steward ქმნის draft-ს; system admin ამტკიცებს target/connection ცვლილებას | data და ინფრასტრუქტურა განცალკევებულია |
| publication | ყველა ახალი package იწყება `DRAFT`; მხოლოდ `DATA_PUBLISHER` აქვეყნებს | ატვირთვა არ უნდა გახდეს ავტომატურად public |
| unknown table | `UNMAPPED`; არ იწერება არცერთ target DB-ში | არასწორი მონაცემის შეტანისგან დაცვა |
| default import mode | `APPEND` მხოლოდ immutable/event table-ზე; `UPSERT` key-იანი მიმდინარე table-ზე; `REPLACE` მხოლოდ explicit approved snapshot profile-ზე | მონაცემის შემთხვევითი წაშლის თავიდან აცილება |
| row key | პირველად გამოიყენება child target-ის არსებული PK; თუ არ არის, profile-ში განისაზღვრება business/composite key | row identity სტაბილური უნდა იყოს ხელახალ ატვირთვაზეც |
| duplicate policy | `UPSERT`-ში duplicate row key ბლოკავს import-ს, თუ deterministic conflict rule არ არის განსაზღვრული | ჩუმი დუბლირება დაუშვებელია |
| Access package chart config | package ქმნის მხოლოდ chart `DRAFT`; core profile whitelist ადასტურებს fields/types-ს | ფაილი ვერ აქვეყნებს დაუმოწმებელ chart-ს |
| chart row binding | rule/filter/aggregation-ზე დაფუძნებული; არა materialized row-to-chart link | დიდი მოცულობისთვის სწრაფი და არადუბლირებადი მოდელი |
| wide table | v1-ში direct wide storage + explicit series column list; v2-ში opt-in `UNPIVOT` | მოქმედი table-ების რისკის გარეშე მუშაობა |
| დიდი import | asynchronous background job + WebSocket progress + cancellation checkpoint | HTTP timeout და memory overload თავიდან იცილება |
| ერთდროული import | ერთი write lock ერთ target table-ზე; სხვადასხვა target table parallel შესაძლებელია | `REPLACE`/`UPSERT` კონფლიქტების თავიდან აცილება |
| raw Access file | ინახება configurable retention პერიოდით checksum/metadata-სთან ერთად; production default: 90 დღე | audit/rollback და საცავის ბალანსი |
| archive/backup | core ყოველდღიური backup; child DB domain policy; `REPLACE` profile-ს სჭირდება pre-import snapshot/staging swap | აღდგენის რეალური გზა |
| connection secrets | ახალ რეჟიმში მხოლოდ server-side connection profile reference | secrets არ ხვდება Access/frontend payload-ში |
| MSSQL schema | ყველა profile explicit schema-ით; default არ არის `dbo` | `geomap`-ის მსგავსი წყაროების სწორი მხარდაჭერა |
| API lifecycle | ახალი ფუნქციები მხოლოდ `/api/v2`; `/api/v1` legacy რჩება | უკუთავსებადობა |
| legacy migration | თითო profile: `LEGACY → HYBRID → DYNAMIC`, rollback = წინა mode | ეტაპობრივი, უსაფრთხო გადაყვანა |

## 14. ცვლილების ჟურნალი

| თარიღი | ცვლილება | სტატუსი |
|---|---|---|
| 2026-09-08 | პირველი არქიტექტურული გეგმა შეიქმნა არსებული core/child DB inventory-ის საფუძველზე | Draft |

## 15. მონაცემის სრული სიცოცხლის ციკლი

```text
Draft package → Upload → Cataloged → Validating → Validated → Imported → Reconciled
→ Pending publication → Published → Superseded → Archived
```

| მდგომარეობა | წესი |
|---|---|
| `Draft` | Access package ჯერ არ მოქმედებს public მონაცემზე |
| `Validated` | file structure, mapping, column და chart წესები წარმატებით შემოწმდა |
| `Imported` | child target table-ებში transaction-ები დასრულდა |
| `Reconciled` | source/target row count და quality წესები ერთმანეთს შეადარეს |
| `Published` | data/chart public read API-სთვის აქტიურია |
| `Superseded` | ახალი publication ჩანაცვლებს, მაგრამ ძველი audit-ად რჩება |
| `Archived` | public რეჟიმიდან გამორთულია, აღდგენა შესაძლებელია |

## 16. Data quality და reconciliation წესები

### 16.1 სავალდებულო ავტომატური შემოწმებები

- Access table არსებობს package manifest-ში და manifest-ის table არსებობს Access-ში;
- ყველა required column არსებობს და მის type-ს აქვს თავსებადი target type;
- `row_key` არ არის null და არ შეიცავს დაუშვებელ duplicate-ს `UPSERT` რეჟიმში;
- numeric measure შეიცავს რიცხვს, არა ტექსტს ან `NaN`-ს;
- dimension code არსებობს შესაბამის lookup/codelist-ში, როცა ასეთი წესი განსაზღვრულია;
- filter/chart field შედის profile-ის დაშვებულ column-ებში;
- chart-ს აქვს chart type-ისთვის საჭირო მინიმალური fields;
- source row count, inserted row count, rejected row count და skipped row count ანგარიშდება.

### 16.2 არჩევითი დომენური წესები

```text
year BETWEEN 1900 AND მიმდინარე_წელი + 1
quantity >= 0
share BETWEEN 0 AND 100
region code ∈ cl_region
quarter ∈ {1,2,3,4}
```

დომენური წესები ინახება profile-ში და არა Java source-ში.

## 17. Schema evolution

| ცვლილება | რეაქცია |
|---|---|
| ახალი არასავალდებულო Access column | `CATALOGED`, ჯერ არ ჩანს public API-ში |
| ახალი required column | საჭიროა profile version update და approval |
| column type-ის incompatible ცვლილება | import ბლოკდება `FAILED_VALIDATION`-ით |
| table-ის ახალი სახელი | mapping update; ძველი mapping audit-ად რჩება |
| target schema ცვლილება | database migration + profile version update |
| chart field-ის წაშლა | ყველა დამოკიდებული chart გადადის `PENDING` მდგომარეობაში |

არ შეიძლება production child table-ის `ALTER` შესრულდეს მხოლოდ Access file-ის საფუძველზე. Schema ცვლილება არის ცალკე დამტკიცებული migration.

## 18. უსაფრთხოება, წვდომა და კონფიდენციალურობა

### 18.1 როლები

| როლი | უფლებები |
|---|---|
| `DATA_UPLOADER` | package ატვირთვა, საკუთარი job-ის ნახვა |
| `DATA_STEWARD` | mapping/profile/chart draft-ის შექმნა და ხარისხის შემოწმება |
| `DATA_PUBLISHER` | validated version-ის გამოქვეყნება/არქივირება |
| `SYSTEM_ADMIN` | connection profile, role, migration და rollback |
| `PUBLIC_READER` | მხოლოდ published table/chart data |

### 18.2 Secrets

- DB URL/user/password არ ინახება Access package-ში;
- frontend არ აგზავნის DB password-ს ახალ API-ში;
- core ინახავს მხოლოდ connection reference-ს; secret ინახება server-side დაცულ კონფიგურაციაში;
- logs, WebSocket status და error response არ აბრუნებს password/connection string-ს.

### 18.3 Audit

audit უნდა პასუხობდეს კითხვებს:

```text
ვინ ატვირთა? რომელი ფაილი? როდის? რომელი profile version?
რომელ child DB/schema/table-ში ჩაიწერა? რამდენი row?
ვინ შეცვალა chart? რომელი version გამოქვეყნდა? როგორ დავაბრუნოთ წინა ვერსია?
```

## 19. Performance და მასშტაბირება

### 19.1 Import

- batch size გამოითვლება driver/column count-ის მიხედვით;
- import სრულდება background worker-ში, HTTP request არ უნდა ელოდოს მილიონობით row-ს;
- WebSocket აჩვენებს package/table/progress სტატუსს;
- file size, timeout და parallel import limits profile/environment-ით იმართება;
- ერთ target table-ზე ერთდროულად დაშვებულია ერთი write lock, რათა `REPLACE` ოპერაციებმა ერთმანეთს არ გადაუაროს.

### 19.2 Read/query

- ყველა dynamic table query არის server-side paginated;
- chart query ყოველთვის aggregate-დება DB-ში და არ მოაქვს raw მილიონობით row frontend-ში;
- profile-ის filter/group-by columns ინდექსდება child DB-ში migration-ით;
- chart response-ს აქვს cache key: profile version + chart version + publication version + filters;
- export დიდ data-ზე არის async job/streaming, არა memory-ში სრულად ჩატვირთვა.

### 19.3 საწყისი performance მიზნები

| ოპერაცია | მიზანი |
|---|---|
| 100-row dynamic table page | p95 ≤ 2 წამი ნორმალური filter-ით |
| chart aggregate query | p95 ≤ 3 წამი ინდექსირებულ profile-ზე |
| მცირე Access package | ზუსტი progress და დასრულების report |
| დიდი import | background რეჟიმი, retry/rollback დეტალური report-ით |

## 20. გამძლეობა, backup და rollback

### 20.1 Import failure

- ერთ target table-ზე ჩავარდნისას მისი transaction rollback-დება;
- სხვა target table-ის წარმატებული შედეგი audit-ში ჩანს;
- publication არ სრულდება, თუ required dataset ჩავარდა;
- retry ქმნის ახალ import attempt-ს; ძველი ისტორია არ იშლება.

### 20.2 Publication rollback

- `published_version_id` იცვლება წინა წარმატებულ version-ზე;
- child table-ში destructive `REPLACE` რეჟიმისთვის აუცილებელია წინასწარ განსაზღვრული backup/staging swap strategy;
- rollback არ კეთდება ხელით SQL-ით audit ჩანაწერის გარეშე.

### 20.3 Backup

- core DB: ყოველდღიური backup + transaction-log policy;
- child DB: დომენზე მორგებული backup policy;
- uploaded package: checksum, metadata და retention policy;
- restore test რეგულარულად მოწმდება test გარემოში.

## 21. Observability და ოპერაციული მონიტორინგი

### 21.1 Metrics

```text
imports_total
imports_failed_total
import_duration_seconds
rows_imported_total
rows_rejected_total
unmapped_tables_total
chart_query_duration_seconds
dynamic_query_duration_seconds
publication_rollback_total
```

### 21.2 Logs და alerts

- ყველა log ატარებს `importJobId`, `importJobItemId`, `pageId`, `profileCode` correlation ID-ს;
- alert: import failure, repeated validation failure, chart query timeout, child DB unavailable;
- health endpoint ამოწმებს core DB-სა და მხოლოდ აუცილებელ child connection profile-ებს.

## 22. ტესტირების სტრატეგია

| დონე | რა მოწმდება |
|---|---|
| Unit | mapping, filter grammar, chart validation, schema resolver, hyphenated database target resolution |
| Integration | MSSQL/MySQL import, transaction, schema-qualified table, profile API |
| Contract | Managed Access Package schema და public API პასუხები |
| Migration | legacy upload/endpoint მუშაობს ახალ release-ზე |
| Performance | `auto.eoyes`, `trade_data` pagination/aggregation profile |
| Security | role checks, forbidden columns, SQL injection attempts, secret masking |
| UAT | data steward ატვირთავს package-ს და publisher აქვეყნებს chart-ს |

სავალდებულო test fixture-ები:

```text
ერთი-table legacy Access
managed multi-table Access
wide-table Access
MSSQL dbo target
MSSQL geomap schema target
MySQL target
invalid mapping / invalid chart / partial failure
```

## 23. Deployment, environments და configuration

| გარემო | დანიშნულება |
|---|---|
| Local | developer unit/integration development |
| Test | managed package და migration regression |
| Stage | production-like child DB schema/performance validation |
| Production | დამტკიცებული migrations და published profiles |

- DB migration გამოიყენებს versioned migration tool-ს; `ddl-auto=update` არ უნდა იყოს schema ცვლილების production კონტროლი;
- feature flags page/profile დონეზე ინახება core-ში;
- rollout არის canary: ერთი profile → მონიტორინგი → შემდეგი profile;
- deployment plan აუცილებლად შეიცავს rollback version-ს.

## 24. API, i18n და მომხმარებლის გამოცდილება

- API ვერსირდება `/api/v2`; legacy `/api/v1` რჩება უცვლელი;
- ყველა profile/column/chart label-ს აქვს `ka` და `en` მნიშვნელობა;
- measure-ს აქვს unit, decimal precision, null-display და source note;
- dynamic table-ის column order, visibility, default sort და allowed filters მოდის profile-დან;
- chart response აბრუნებს title, unit, legend label, source, update timestamp და accessibility description-ს;
- CSV/XLSX export იყენებს იმავე profile/filters-ს, რასაც table view;
- UI-ში ჩანს import publication status და `UNMAPPED` table-ების actionable report.

## 25. Governance და პასუხისმგებლობები

| საკითხი | პასუხისმგებელი |
|---|---|
| Access package data-ის სისწორე | დომენის data owner |
| code list/measure definition | data steward |
| profile/mapping/chart draft | data steward |
| production publication | data publisher |
| DB migration/connection/security | system administrator |
| platform code და quality gates | development team |

ყოველი published profile უნდა ფლობდეს: owner, source, update frequency, retention, sensitivity, last-reviewed date.

## 26. Definition of Done

პირველი pilot დასრულებულად ჩაითვლება მხოლოდ მაშინ, როცა:

- [ ] Managed Access Package v1 ნიმუში არსებობს და validator გადის;
- [ ] ერთი file-იდან მინიმუმ ერთი `TABLE`, ერთი `STATISTICAL` და ერთი `LOOKUP` table იტვირთება;
- [ ] ერთი data row declarative წესით მინიმუმ ორ chart query-ში მონაწილეობს;
- [ ] table, chart და export იმავე publication version-ს იყენებს;
- [ ] `UNMAPPED`, validation failure და partial import UI/API report-ში მკაფიოდ ჩანს;
- [ ] `geomap`-ის არადეფოლტური schema ანალოგიურ ტესტში მუშაობს;
- [ ] legacy upload და endpoint regression test წარმატებულია;
- [ ] rollback სცენარი შესრულებული და დოკუმენტირებულია;
- [ ] performance/security/UAT ხარისხის კარიბჭეები გავლილია.
