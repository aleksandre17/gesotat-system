# GeoStat საერთაშორისო სტანდარტებზე დაფუძნებული Reference Architecture

## არქიტექტურული პოზიცია

„საუკეთესო“ არ ნიშნავს ყველა ცნობილ პატერნის ერთად გამოყენებას. ეს წარმოშობს რთულ, ურთიერთსაწინააღმდეგო სისტემას. საუკეთესო ნიშნავს: თითოეული ფენისათვის ავირჩიოთ მისთვის შექმნილი, ურთიერთთავსებადი სტანდარტი და პატერნი; ერთი პასუხისმგებლობა არ ჰქონდეს ორ მოდელს.

ამ პლატფორმის საბოლოო reference architecture არის:

```text
Governed Data Product Platform
 ├─ Control plane: metadata, contract, governance, audit, publication
 ├─ Data plane: landing, canonical domain, statistical semantic, serving
 └─ Experience plane: API, table, chart, export, catalog
```

## 1. სტანდარტების ზუსტი ადგილი

| ფენა | არჩეული საფუძველი | რატომ |
|---|---|---|
| Metadata registry | ISO/IEC 11179 | data element, value domain, identifier და metadata registry-ის canonical საფუძველი |
| Official statistics | SDMX 3.1 / ISO 17369 | dimension, measure, code list, data structure definition, reference metadata და სტატისტიკური გაცვლა |
| Dataset catalog | W3C DCAT 3 | dataset, distribution, service, provenance, checksum, version და catalog discovery |
| Data quality | ISO/IEC 25012 | completeness, accuracy, consistency, timeliness და quality measure-ის მართვა |
| Geo data | OGC API / OGC API Features | feature, geometry, schema და geospatial API interoperability |
| API contract | OpenAPI | versioned, machine-readable REST contract |
| Governance/security | NIST data governance concepts + least privilege | owner, steward, authority, classification, audit და decision rights |

SDMX-ის ამოცანაა statistical data-ის სტრუქტურა და metadata, არა ყველა content/raw table-ის ჩანაცვლება. ISO/IEC 11179 მართავს მონაცემის მნიშვნელობას, მაგრამ არ კარნახობს physical SQL schema-ს. DCAT 3 მართავს discovery/catalog-ს, მაგრამ არ არის import engine. ეს საზღვრები აუცილებელია სწორი კომბინაციისთვის.

## 2. საბოლოო canonical model

```text
Data Product
  ├─ Dataset                 # დამოუკიდებელი მონაცემთა ერთეული
  ├─ Dataset Contract        # schema + quality + lifecycle წესები
  ├─ Distribution            # Access package / API / export
  ├─ Schema Version
  ├─ Data Element            # field-ის საერთაშორისო metadata აღწერა
  ├─ Code List / Value Domain
  ├─ Relation / Key / Index
  ├─ Metric                  # chart-ის გამოთვლადი მნიშვნელობა
  ├─ Dimension               # year, region, age, sex, category…
  ├─ Chart Specification
  └─ Publication Version
```

### გარდაუვალი წესი

```text
Raw record        ≠ Statistical observation
Statistical fact  ≠ Chart
Chart             = Metric + Dimension + Filter + Presentation
```

ამ წესით chart არასოდეს ებმის „row #52“-ს ან `chart_1` table-ს. იგი ებმის გამოქვეყნებულ metric-სა და dimension-ებს. ამიტომ იგივე dataset-იდან შესაძლებელია უსასრულოდ ბევრი table/chart/export without data duplication.

## 3. Data plane — ოთხი მკაცრი ფენა

```text
Access / API source
        ↓
1. Landing    — immutable source snapshot
        ↓
2. Canonical  — relational business/domain model
        ↓
3. Semantic   — dimensions, metrics, statistical facts, governed views
        ↓
4. Serving    — published API/table/chart/export views
```

### Landing

- file checksum, source row number, source package version, ingest timestamp;
- source data და JSON უცვლელად;
- replay, lineage და defect investigation.

### Canonical

- `files`, `glossary`, `goals`, person/event/content მსგავსი raw/business entity table;
- business key, FK და normalized relations;
- არ ხდება სტატისტიკად ხელოვნური გადაკეთება.

### Semantic

- `dimension`, `codelist`, `metric`, `fact` და governed views;
- wide source table შეიძლება დარჩეს canonical-ში, მაგრამ მისგან შეიქმნას normalized analytical fact/view;
- statistical model SDMX-ის DSD ლოგიკას მიჰყვება: dimension + measure + attribute + code list.

### Serving

- მხოლოდ public publication version;
- API-ს აქვს pagination/filter/sort/query limit;
- chart query aggregate-დება DB-ში;
- export table/chart-სთან ერთ version-ზე მუშაობს.

## 4. Core და child database-ის საბოლოო პასუხისმგებლობა

```text
Core / Control plane (geostat-system)
  - Data Product registry
  - Data contract / schema registry
  - Field, codelist, dimension, metric metadata
  - DDL policy / approval / migration plan
  - chart specification / publication version
  - lineage / audit / quality result

Child / Data plane (მაგ. kids)
  - landing source snapshot
  - canonical content/raw/entity tables
  - semantic dimension/fact/view tables
  - staging/versioned physical tables
  - no global users, secrets or publication authority
```

Core არასოდეს ხდება დიდი fact-data warehouse. Child DB კი არ ხდება metadata/governance-ის source of truth.

## 5. Access package-ის როლი

Access არის **Data Contract Distribution**:

```text
Data tables
  + schema declaration
  + keys/relations/validation
  + statistical dimensions/measures
  + chart specification
  + version/source/provenance
```

მაგრამ Access author-ს არ უნდა ჰქონდეს ხელით სამართავი ყველა ტექნიკური detail. სწორი enterprise UX არის:

```text
Authoring UI / Excel-like builder
  → ქმნის/ვალიდირებს სრულ SDMX/ISO-compatible manifest-ს
  → export-ს აკეთებს Managed Access Package-ად
```

ანუ სრული სტანდარტი შიგნით არსებობს; usability არ არის კომპრომისი, რადგან UI მხოლოდ ამცირებს ხელით შეცდომას და არ ამცირებს contract-ს.

## 6. 1000+ ჩანაწერი და statistical data binding

1000, 100 000 ან მილიონი row არქიტექტურულად ერთნაირად მუშაობს:

```text
fact_population
  indicator_code | period | region | age_group | sex | value

Metric: CHILD_POPULATION = SUM(value)
Dimensions: period, region, age_group, sex
```

| მოთხოვნა | Semantic query |
|---|---|
| Line chart | group by period |
| Bar chart | group by age_group |
| Map | group by region |
| KPI | latest period + aggregate |
| Table | filtered fact rows |

არ ხდება chart data-ის ცალკე კოპირება თითო ვიზუალიზაციისთვის. Index-ები იქმნება fact-ის grain-ისა და გავრცელებული filter/group-by dimension-ების მიხედვით.

## 7. ხარისხის, lineage-ისა და publication-ის contract

ყველა dataset version-ს აქვს:

```text
identity       → product/dataset/version/checksum
meaning        → data element, unit, definition, codelist
structure      → schema, key, relation, grain
quality        → completeness, uniqueness, range, referential validity, timeliness
lineage        → source package, source row, transform version, import job
governance     → owner, steward, classification, retention
publication    → draft/review/published/rolled-back
```

სტატისტიკური quality rule არ არის მხოლოდ `value > 0`; ის მოიცავს codelist validity-ს, time coverage-ს, duplicate series-ს, missing observation-ს, unit/scale compatibility-ს და revision history-ს.

## 8. უსაფრთხო schema provisioning

Access აღწერს სასურველ schema-ს, მაგრამ DDL უფლებას არ ფლობს. Core ასრულებს:

```text
manifest + approved target policy
  → schema diff
  → reviewed DDL plan
  → staging create/load/validate
  → atomic publish or rollback
```

ეს არ არის შეზღუდვა; ეს არის საერთაშორისო ხარისხის უსაფრთხოების საზღვარი. Arbitrary SQL/host/password Access-ში არ უნდა არსებობდეს.

## 9. პრაქტიკული CIDS მაგალითი

```text
CONTENT:      files, glossary, goals, goal_titles
LOOKUP:       age_groups, regions, sexes, indicators
STATISTICAL:  population observations
METRIC:       child population
CHARTS:       population-by-year, age distribution, region map, KPI
```

`files.chartdata` ინახება canonical raw/content ფენაში. მხოლოდ მას შემდეგ, რაც steward დააფიქსირებს indicator, unit, period და dimension semantics-ს, ის materialize-დება statistical fact-ში. ეს იცავს data meaning-ს და არა მხოლოდ bytes-ს.

## 10. საბოლოო არქიტექტურული არჩევანი

ეს არის რეკომენდებული, შეუვიწროებელი კომბინაცია:

```text
DDD/Data Product boundaries
 + ISO/IEC 11179 metadata registry
 + SDMX statistical semantic model
 + DCAT 3 catalog/version/provenance
 + ISO/IEC 25012 quality model
 + layered landing/canonical/semantic/serving data architecture
 + declarative visualization semantic layer
 + policy-controlled schema provisioning
 + OpenAPI / OGC API dissemination
```

ეს მოდელი არ აიძულებს raw data-ს statistical ფორმაში გადასვლას, არ აიძულებს statistical data-ს JSON-ში დარჩენას და არ აკავშირებს chart-ს კონკრეტულ row-სთან. სწორედ ამიტომ ის ინარჩუნებს სისწორეს, მასშტაბირებადობას და გაფართოებადობას ერთდროულად.

## 11. Universal interoperability kernel

პლატფორმის მიზანი არ არის „ყველა ცნობილი ფორმატის ერთი SQL table-ში ჩატევა“. ასეთი მიდგომა აუცილებლად კარგავს relation-ს, hierarchy-ს, JSON structure-ს ან statistical meaning-ს. სწორი საერთო ბირთვი არის metadata/contract model:

```text
Data Product
  → Dataset
    → Distribution            # Access, CSV, XLSX, JSON, XML, API, DB table, SDMX, GeoPackage…
    → Schema Version
    → Field / Data Element
    → Value Domain / Code List
    → Identifier / Key
    → Relation
    → Quality Rule
    → Source / Lineage
    → Transformation
    → Publication
```

ეს ბირთვი აღწერს ყველა მონაცემს, მაგრამ არ კარნახობს ერთ physical representation-ს.

| Source structure | Canonical შენახვა | Semantic გამოყენება |
|---|---|---|
| Relational table | normalized entity/fact table | metric/dimension ან transactional API |
| CSV/XLSX | typed dataset table + original file | იმავე contract-ით |
| Access | package datasets + source snapshot | schema/contract-driven import |
| JSON/document | JSON source snapshot + extracted canonical fields/child datasets | საჭირო metric/field projection |
| XML | source snapshot + schema-mapped datasets | contract-ით |
| SDMX | DSD/codelist/series/observation | statistical semantic layer |
| Geo data | feature/geometry + attributes | OGC-compatible map/feature API |
| External REST/API | versioned extraction snapshot + normalized projection | scheduled/streamed dataset |

### Relation model

Core relation registry უნდა ფარავდეს:

```text
PRIMARY_KEY / UNIQUE_KEY
FOREIGN_KEY
ONE_TO_ONE / ONE_TO_MANY / MANY_TO_MANY
LOOKUP / CODELIST
PARENT_CHILD / HIERARCHY
TEMPORAL_VALIDITY
SPATIAL_REFERENCE
DOCUMENT_PARENT_CHILD
DERIVED_FROM / LINEAGE
```

ამდენად, Access-ის nested/JSON field არ იკარგება: ის raw ფენაში სრულად რჩება, ხოლო საჭიროებისას მისი განმეორებადი ნაწილები გარდაიქმნება child dataset-ად explicit parent key-ით.

## 12. External ingestion standard

გარედან ინფორმაცია არასოდეს წერს პირდაპირ public business table-ში. ყველა input გადის ერთნაირ ingestion gateway-ს:

```text
External file / API / database connector
      ↓
Source registration
      ↓
Immutable landing copy + checksum
      ↓
Format adapter and catalog scan
      ↓
Schema inference + declared data contract comparison
      ↓
Mapping / relation / quality validation
      ↓
Staging load
      ↓
Canonical + semantic transformation
      ↓
Publication approval and atomic serving switch
```

### 12.1 Input adapters

```text
File adapters:       ACCDB/MDB, CSV, XLSX, JSON, XML, GeoJSON, GeoPackage, Parquet
Database adapters:   SQL Server, MySQL, PostgreSQL, Oracle (read-only extraction by default)
Service adapters:    REST/JSON, SDMX REST, OGC API, SOAP/XML where required
Delivery adapters:   upload, secure folder/SFTP, scheduled pull, webhook/event
```

ყოველი adapter აბრუნებს ერთიან `SourceCatalog` contract-ს:

```text
source identity, checksum, format, datasets, fields, types, keys,
row/byte counts, nesting, geometry, source schema, extraction timestamp
```

### 12.2 Import contract

ყოველი external feed წინასწარ რეგისტრირდება:

```text
source_system
source_connection_reference       # secret manager reference, არა password
ingestion_method                  # FILE_UPLOAD/SFTP/PULL_API/DB_EXTRACT/EVENT
format_profile                    # ACCESS_V2/CSV_CONTRACT/SDMX_JSON/OGC_API…
schedule_or_trigger
dataset_mapping
schema_evolution_policy
quality_SLA
retention_policy
owner/steward
```

Unknown file შეიძლება catalog/preview-ად გაიხსნას, მაგრამ ვერ გამოაქვეყნებს მონაცემს მანამ, სანამ არ ექნება დამტკიცებული Data Product + Dataset Contract + target policy.

### 12.3 Schema evolution

```text
Compatible:   ახალი nullable field, ახალი codelist value, ახალი dataset version
Review:       widened type, ახალი relation/index, ახალი measure/dimension
Breaking:     drop/rename, key/grain change, unit/scale meaning change
```

breaking change ყოველთვის ქმნის ახალ schema version-ს; ძველი published version ხელმისაწვდომი რჩება rollback/reproducibility-სთვის.

## Sources

1. [ISO/IEC 11179-1:2023 — Metadata registries framework](https://www.iso.org/standard/78914.html).
2. [SDMX Standards — Technical Specifications 3.1](https://sdmx.org/standards-2/).
3. [What is SDMX?](https://sdmx.org/about-sdmx/welcome/).
4. [W3C DCAT 3 Recommendation](https://www.w3.org/TR/vocab-dcat-3/).
5. [ISO/IEC 25012:2008 — Data quality model](https://www.iso.org/standard/35736.html).
6. [OGC API standards](https://www.ogc.org/standards/).
7. [OpenAPI Specification](https://spec.openapis.org/).
8. [NIST data governance definition](https://csrc.nist.gov/glossary/term/data_governance).
