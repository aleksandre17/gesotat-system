# Unified Canonical Data Platform — საბოლოო Reference Model (ჩანაცვლებულია)

> ეს დოკუმენტი შეიცავდა metadata-driven graph ვარიანტს, მაგრამ მისი `record_value` ბირთვი ზედმეტად EAV-ს ჰგავდა. საბოლოო, წასაკითხი, data-family-based physical schema მოცემულია [GeoStat — საბოლოო ფიზიკური ბაზის არქიტექტურა](final-physical-database-architecture.md)-ში.

## 1. შეუვალი გადაწყვეტილებები

1. ახალი მონაცემის შემოსვლისას **არ იქმნება ახალი business database ან business table**.
2. ყველა ახალი structure აღწერილია metadata-ით; ფიზიკური platform tables მუდმივია.
3. raw data, relational data, document/JSON data, classification, statistical observation და geo feature ერთი canonical kernel-ით იმართება.
4. visualization არასოდეს ატარებს საკუთარ მონაცემს და არც Access import package-ში არის სავალდებულო.
5. chart არის Core-ში დამოუკიდებელი, versioned query specification: `metric + dimensions + filters + presentation`.
6. classification არ არის „უბრალოდ lookup“: იგი არის versioned, hierarchical, multilingual, alias-მხარდაჭერილი first-class data asset.
7. import ხდება atomic publication-ით; ახალი მონაცემი ან მთლიანად გამოქვეყნდება თავისი ბმებით, ან ძველი published version უცვლელი რჩება.

## 2. სამი დამოუკიდებელი contract

```text
1. Ingestion Contract
   რა ფორმატით, საიდან და რა წესით შემოდის მონაცემი.

2. Semantic Contract
   რას ნიშნავს field, code, relation, dimension, measure, unit, quality.

3. Visualization Contract
   როგორ იკითხება published metric/chart/table/API.
```

ეს contracts შეიძლება ერთ release-ში შეიცვალოს, მაგრამ ერთმანეთისგან დამოუკიდებლად ვერსირდება. ამიტომ Access upload არ შეიცავს chart data-ს და chart configuration არ კოპირებს statistical rows.

## 3. ფიზიკურად უცვლელი data-plane schema

ყოველ child data DB-ში (`kids`, შემდეგ სხვა product DB-ებში) არსებობს ზუსტად ერთი, ერთნაირი platform schema. იგი არ იზრდება ახალი დომენური table-ებით.

```text
platform.ingestion_batch
platform.source_artifact
platform.dataset_snapshot
platform.record
platform.record_value
platform.record_relation
platform.record_quality_result
platform.publication
platform.publication_member
platform.outbox_event
```

### 3.1 `record`

ყველა entity/document/observation/classification item არის record.

```text
record_id                 UUID / bigint PK
dataset_id                Core dataset identifier
snapshot_id               import version
record_kind               ENTITY | DOCUMENT | OBSERVATION | CLASSIFICATION_NODE | GEO_FEATURE
external_key              source business key
payload_json              lossless original payload
payload_hash              deduplication/integrity
valid_from / valid_to     temporal validity
source_row_number         lineage
created_at
```

### 3.2 `record_value`

ყველა field-ის value ინახება typed form-ში და არა გაუკონტროლებელ EAV text-ში.

```text
record_id
attribute_id              Core semantic attribute identifier
ordinal                   array/repeated-value order
value_type                TEXT | DECIMAL | INTEGER | BOOLEAN | DATE | DATETIME | JSON | GEO | BINARY_REF
value_text
value_decimal
value_datetime
value_boolean
value_json
value_code                classification code shortcut
```

Check constraint: ერთი `value_*` carrier შეესაბამება `value_type`-ს. Index-ები: `(attribute_id, value_code)`, `(attribute_id, value_decimal)`, `(attribute_id, value_datetime)`, `(record_id)`.

### 3.3 `record_relation`

ყველა კავშირი ერთიანად:

```text
from_record_id
relation_type_id
to_record_id
ordinal
valid_from / valid_to
source_confidence
```

`relation_type_id` აღწერს: `FOREIGN_KEY`, `PARENT_OF`, `MEMBER_OF`, `LOOKUP_OF`, `DERIVED_FROM`, `SPATIAL_CONTAINS`, `TRANSLATION_OF`, `MANY_TO_MANY` და სხვა. ასე ახალი relation არ ითხოვს ახალ join table-ს.

### 3.4 რატომ არ არის ეს ცუდი EAV

ეს არ არის დაუგეგმავი EAV. იგი არის governed canonical record/value graph:

- ყოველი `attribute_id` წინასწარ რეგისტრირებული, typed და versioned-ია;
- value-ები index/partition-ით არის typed;
- source payload უცვლელად ინახება;
- schema validation import-მდე მუშაობს;
- analytical access ემსახურება materialized semantic projections-ით, არა ყველა UI მოთხოვნაზე raw pivot-ით.

ახალი dataset არ ქმნის ახალ business table-ს; მხოლოდ `dataset`, `attribute`, `classification`, `metric` metadata და შესაბამის record/value/relation rows ემატება.

## 4. Core control-plane schema

Core-ში ინახება მხოლოდ წესები, არა დიდი row-ები:

```text
core.data_product
core.dataset
core.dataset_version
core.attribute
core.attribute_version
core.relation_type
core.classification_scheme
core.classification_version
core.metric
core.metric_component
core.visualization
core.visualization_version
core.ingestion_contract
core.source_connector
core.schema_change_request
core.publication_policy
core.audit_event
```

### 4.1 `dataset`

```text
dataset_id, product_id, code, kind,
record_kind, identity_attribute, grain_definition,
retention_policy, sensitivity, lifecycle_status
```

### 4.2 `attribute`

```text
attribute_id, dataset_id, code, logical_type,
role (IDENTIFIER/DIMENSION/MEASURE/CLASSIFICATION/TEXT/JSON/GEO),
value_domain_id, unit_id, cardinality,
required, repeatable, filterable, groupable, searchable,
label_ka, label_en, definition_ka, definition_en
```

### 4.3 `metric`

```text
metric_id, code, source_dataset_id,
measure_attribute_id, aggregation,
allowed_dimension_set, unit, precision,
calculation_definition, quality_policy
```

Metric შეიძლება იყოს direct measure (`SUM(value)`) ან governed derived metric. arbitrary SQL აკრძალულია; calculation არის typed expression/approved transformation.

## 5. Classification არის სრული domain object

ყოველი კლასიფიკატორი არის `dataset(kind=CLASSIFICATION)` და მისი თითოეული item არის `record(kind=CLASSIFICATION_NODE)`.

```text
Classification Scheme
  → Version
    → Node (code, labels, status, properties)
      → PARENT_OF relation
      → alias / external-code mapping
      → validity period
```

ამით ფარავს:

```text
country / region / municipality
age group / sex / education level
economic activity / occupation / product class
hierarchical category
multilingual label
deprecated code → replacement code
external source code → canonical code
```

Statistical observation classification-ს არ text value-ით, არამედ `record_relation` ან canonical `value_code`-ით ებმის. ამიტომ code change, translation, hierarchy და historical version მართვადია.

## 6. Statistical data — SDMX-compatible cube without new tables

`dataset(kind=STATISTICAL)` განსაზღვრავს grain-ს და components-ს:

```text
Population observation grain:
indicator × period × region × age_group × sex

Measure:
value
```

ერთი observation არის `record(kind=OBSERVATION)`.

```text
record_value / record_relation:
  indicator = CHILD_POPULATION
  period = 2024
  region = GE
  age_group = AGE_0_17
  sex = TOTAL
  value = 929711
```

Dimension, measure, codelist, attribute და observation structure SDMX-ის Data Structure Definition პრინციპს მიჰყვება. ახალი statistical domain ნიშნავს ახალი dataset/attribute metadata-ს, არა ახალ physical fact table-ს.

## 7. Raw/document და statistical data-ის კავშირი

```text
Raw file/document record
  └─ DERIVED_FROM / EXTRACTED_TO
       └─ Statistical observation record
```

მაგალითად `files.chartdata` რჩება როგორც lossless `DOCUMENT` record. მისი curated interpretation ქმნის `OBSERVATION` record-ებს და თითოეულ observation-ს აქვს lineage relation source document/row-მდე. არაფერი დუბლირდება როგორც „chart table“.

## 8. Visualization model — მონაცემის გარეშე

```text
visualization
  → visualization_version
      metric_id
      x_dimension_attribute_id
      series_dimension_attribute_id
      filters
      chart_type
      labels/style/accessibility
      publication_id
```

```text
LINE:   metric grouped by period
BAR:    metric grouped by classification
MAP:    metric grouped by geographic classification
KPI:    metric with latest-period rule
TABLE:  published dataset projection
```

არც `__gs_chart` და არც სხვა chart table არ შეიცავს value rows. visualization registry მხოლოდ ამბობს როგორ წაიკითხოს უკვე არსებული published semantic data.

## 9. External ingestion — ყველა ფორმატისთვის ერთიანი gateway

```text
File / DB / API / event
  → Source Adapter
  → Universal Source Catalog
  → Ingestion Contract validation
  → staging snapshot
  → canonical records / values / relations
  → quality + lineage
  → publication
```

Input adapters:

```text
Access, CSV, XLSX, JSON, XML, Parquet, GeoJSON, GeoPackage,
SQL Server, MySQL, PostgreSQL, REST, SDMX REST, OGC API, SFTP, webhook
```

ყველა adapter აბრუნებს ერთსა და იმავე internal envelope-ს:

```text
artifact + checksum + source schema + records + values + relations + extraction metadata
```

ახალი format ემატება adapter plug-in-ით; Core/data-plane schema არ იცვლება.

## 10. Multi-dataset და atomic dependency graph

ერთი import batch შეიძლება შეიცავდეს ასობით dataset-ს. მანიფესტი ქმნის dependency graph-ს:

```text
classifications → entities/lookups → documents → observations → derived metrics → publication
```

Engine:

1. ციკლებსა და missing relation-ს წინასწარ პოულობს;
2. topological order-ით იტვირთება;
3. თითო dataset-ს staging snapshot-ში წერს;
4. referential/quality checks სრულდება სრული graph-ის დონეზე;
5. ყველა required member წარმატებისას აქვეყნებს ერთ `publication_id`-ად;
6. partial failure არ ცვლის ბოლო published graph-ს.

თუ relation ციკლურია, engine ქმნის records-ს დროებითი deferred relation მდგომარეობით და მხოლოდ შემდეგ ამოწმებს მთლიან graph-ს; დაუკმაყოფილებელი link publication-ს აჩერებს.

## 11. Performance და ზრდა

```text
partition: dataset_id + publication/snapshot + time
index: attribute/type-specific value indexes
cache: published metric/dimension aggregate cache
storage: source artifact ცალკე object storage + immutable reference
query: semantic compiler → prepared, bounded queries
```

1000 record, 1 million record და მრავალი source ერთსა და იმავე platform tables-ში ინახება. იზრდება partition/index/cache, არა business table count.

## 12. Access-ის საბოლოო ფორმა

Access არ არის platform schema-ის ასლი. იგი არის source artifact.

სავალდებულოდ მხოლოდ:

```text
__package       identity, version, source, contract reference
__dataset       source dataset identity and declared kind
__mapping       source field → registered semantic attribute
__link          source record/key → source record/key relation declaration
<source tables>
```

Classification, metric, visualization და schema policy ჩვეულებრივ უკვე Core registry-ში არსებობს და Access მათ code-ებით მიუთითებს. ახალი concept მხოლოდ review workflow-ით რეგისტრირდება; Access ვერ ქმნის ჩუმად ახალ meaning-ს.

ეს არის სრულფასოვანი სტანდარტი და არა გამარტივება: package პატარაა, რადგან რთული governance ცენტრალიზებულია და versioned-ია.

## 13. Actors და პასუხისმგებლობა

| Actor | პასუხისმგებლობა |
|---|---|
| Data Owner | meaning, publication, retention |
| Data Steward | attribute/classification/quality/metric approval |
| Source Adapter | format → universal envelope |
| Contract Resolver | source → registered semantic contract |
| Relation Resolver | key/code/hierarchy/link resolution |
| Quality Engine | structure, type, value, relation, statistical checks |
| Publication Engine | atomic snapshot/publication/rollback |
| Semantic Query Engine | metric/dimension query compilation |
| Visualization Service | published metric rendering contract |
| DB Administrator | platform storage, partition, backup, access policy |

## 14. Definition of Done

- [ ] ახალი raw/JSON/relational/statistical source ერთ fixed data-plane schema-ში იტვირთება;
- [ ] ახალი dataset/field/relation/classification ფიზიკურ business table-ს არ ქმნის;
- [ ] classifier hierarchy, alias, translation, validity და version მუშაობს;
- [ ] multi-dataset dependency graph ატომურად ქვეყნდება;
- [ ] statistical cube და raw lineage ერთმანეთთან traceableა;
- [ ] visualization value copy-ის გარეშე metric/dimension-ს ებმის;
- [ ] ახალი input format მხოლოდ adapter-ის დამატებით მუშაობს;
- [ ] schema/data/publication rollback და audit სრულად მუშაობს.
