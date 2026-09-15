# GeoStat — საბოლოო ფიზიკური ბაზის არქიტექტურა

> სტატუსი: **Implementation Baseline**. ამ დოკუმენტში მიღებული გადაწყვეტილებები განსაზღვრავს შემდეგი migration-ების, import engine-ისა და API-ის ფიზიკურ საფუძველს.

## 1. საბოლოო გადაწყვეტილება: სამი ფიზიკური plane

```text
┌──────────────────────────────────────────────────────────────┐
│ geostat-system                                                 │
│ Control Plane / System Entry                                   │
│ users, pages, authorization, metadata, contracts, charts,     │
│ governance, import control, audit, publication decisions      │
└───────────────────────────────┬──────────────────────────────┘
                                │ immutable IDs + events
┌───────────────────────────────▼──────────────────────────────┐
│ geostat-data                                                   │
│ Shared Data Plane                                               │
│ all imported raw, entity, statistical, geo data; immutable    │
│ published classification mirrors; staging and serving          │
│ data; staging; published releases; query-serving structures    │
└──────────────────────────────────────────────────────────────┘
                                │ published immutable snapshot
┌───────────────────────────────▼──────────────────────────────┐
│ geostat-archive                                                │
│ Archive Plane                                                   │
│ one-year immutable snapshot records and artifact lineage       │
└──────────────────────────────────────────────────────────────┘
```

### რატომ არა ყველაფერი `geostat-system`-ში

- identity/page/permission სისტემას და მილიონობით data row-ს სხვადასხვა lifecycle, backup, scaling და access policy აქვს;
- დიდი ingest/query არ უნდა აზიანებდეს login/navigation/Core ადმინისტრირებას;
- მონაცემის storage/partition/restore დამოუკიდებლად იმართება.

### რატომ არა ახალი database თითო საიტზე ან dataset-ზე

- საიტი არის `data_product`, არა database boundary;
- ახალი site/dataset მხოლოდ metadata row-ებსა და data rows-ს ამატებს;
- ერთიანი data plane ამცირებს deployment, backup, integration და governance სირთულეს.

**გამონაკლისი:** მომავალში სამართლებრივი isolation, data sovereignty ან ექსტრემალური მოცულობა შეიძლება გახდეს ცალკე data-plane database-ის მიზეზი. ეს არის ოპერაციული shard, არა ახალი არქიტექტურული მოდელი; schema ზუსტად იგივე რჩება.

## 2. მთავარი პრინციპი — stable physical model, extensible semantic model

```text
ახალი საიტი        → ახალი data_product
ახალი table/source → ახალი dataset + contract
ახალი field        → ახალი attribute definition
ახალი classifier   → ახალი classification scheme/version/item
ახალი relation     → ახალი relationship definition
ახალი metric/chart → ახალი semantic/visualization definition

არცერთი მათგანი არ ქმნის ახალ physical business table-ს.
```

ამით ვფარავთ 90%+ შემთხვევებს. უცნობი/ახალი ფორმატი ვერ „გატეხავს“ სისტემას: ის ჯერ ინახება raw landing-ში, გადის catalog/contract review-ს და მხოლოდ შემდეგ ხდება published data.

## 3. რას არ ვიყენებთ

არ ვიყენებთ:

```text
- ერთ უზარმაზარ დაურეგისტრირებელ key/value EAV table-ს;
- ახალი SQL table-ს თითო source table-ისთვის;
- chart-ის data copy-ს;
- source package-ში arbitrary SQL / connection / DDL-ს;
- silent type coercion-ს ან დაუდეკლარირებელ relation-ს.
```

ამის ნაცვლად გვაქვს ხუთი მკაფიო data family, თითოეულს თავისი წასაკითხი physical table-ები აქვს.

```text
RAW_DOCUMENT       → raw.source_record
ENTITY/RELATIONAL  → entity.entity_record + entity.entity_link
CLASSIFICATION     → Core `reference.classification_*` + Data Plane immutable mirror
STATISTICAL        → statistics.series + statistics.observation + statistics.observation_dimension
GEO                → geo.feature + geo.feature_link
```

## 4. `geostat-system` — Control Plane schema

არსებული `users`, `roles`, `permissions`, `page_nodes`, `tokens` და მოქმედი legacy table-ები უცვლელად რჩება. ახალი platform schema-ები:

გარე წყაროს endpoint/DB/reference აღწერილია `platform.source_connection`-ში; პაროლი ან access token არასოდეს ინახება ამ table-ში — მხოლოდ secret-manager/environment reference.

### 4.1 `catalog`

```text
catalog.data_product
  product_id PK, product_code UQ, page_id FK, title_ka, title_en,
  owner_user_id FK, lifecycle_status, sensitivity, created_at, updated_at

catalog.dataset
  dataset_id PK, product_id FK, dataset_code UQ-within-product,
  dataset_family, record_identity_policy, business_grain,
  retention_policy, lifecycle_status

catalog.dataset_version
  dataset_version_id PK, dataset_id FK, version, status,
  contract_checksum, effective_from, effective_to, approved_by

catalog.attribute
  attribute_id PK, dataset_version_id FK, attribute_code,
  logical_type, cardinality, role,
  required, searchable, filterable, groupable,
  label_ka, label_en, definition_ka, definition_en,
  unit_code, format_code, json_path NULL

catalog.relationship_type
  relationship_type_id PK, code UQ, category,
  directional, transitive, temporal, description

catalog.dataset_relationship
  relationship_id PK, from_dataset_version_id FK,
  to_dataset_version_id FK, relationship_type_id FK,
  source_attribute_id NULL, target_attribute_id NULL,
  cardinality, required, enforcement_policy, load_priority
```

`attribute` არ არის value table. იგი მხოლოდ readable semantic schema registry-ა: მაგალითად `observation_year`, `age_group`, `value`, `title_ka`.

### 4.2 `reference`

```text
reference.classification_scheme
  scheme_id PK, scheme_code UQ, title_ka, title_en, owner, standard_reference

reference.classification_version
  version_id PK, scheme_id FK, version, status, valid_from, valid_to

reference.classification_item
  item_id PK, version_id FK, code, label_ka, label_en,
  description_ka, description_en, sort_order, status,
  valid_from, valid_to, UQ(version_id, code)

reference.classification_hierarchy
  parent_item_id FK, child_item_id FK, hierarchy_type, valid_from, valid_to

reference.classification_alias
  alias_id PK, item_id FK, external_system_code, external_code, valid_from, valid_to
```

ეს ფარავს ყველა კლასიფიკატორს: ქვეყანა/რეგიონი/მუნიციპალიტეტი, ასაკი, სქესი, საქმიანობა, პროდუქტი, განათლება, მრავალენოვანი label, version, deprecated code და external code mapping.

### 4.3 `semantic`

```text
semantic.dimension
  dimension_id PK, dimension_code UQ, classification_scheme_id NULL,
  value_type, title_ka, title_en

semantic.measure
  measure_id PK, measure_code UQ, value_type, unit_code,
  precision, aggregation_default, title_ka, title_en

semantic.metric
  metric_id PK, metric_code UQ, source_dataset_id FK,
  measure_id FK, calculation_kind, aggregation,
  allowed_dimension_set_json, quality_policy_id, status

semantic.metric_formula
  formula_id PK, metric_id FK, expression_ast_json,
  version, approved_by, status
```

`expression_ast_json` არის typed expression tree; raw SQL არ ინახება.

### 4.4 `ingestion`, `publication`, `visualization`, `governance`

```text
ingestion.source_system
ingestion.source_connector
ingestion.ingestion_contract
ingestion.format_profile
ingestion.mapping_version

publication.release
publication.release_dataset
publication.release_metric

visualization.definition
visualization.version
visualization.filter
visualization.series

governance.quality_rule
governance.approval_request
governance.audit_event
governance.data_access_policy
```

Visualization definition მხოლოდ `metric_id`, dimension, filter და presentation information-ს ინახავს. მასში მონაცემის row/value არ არსებობს.

## 5. `geostat-data` — Shared Data Plane schema

### 5.1 `ingest` — გარედან შემოსული data

```text
ingest.batch
  batch_id PK, contract_id, product_id, source_system_id,
  status, started_at, finished_at, requested_by, checksum

ingest.artifact
  artifact_id PK, batch_id FK, original_name, format,
  object_uri, quarantine_uri NULL, checksum, byte_size, received_at

ingest.dataset_load
  dataset_load_id PK, batch_id FK, dataset_version_id,
  source_name, source_row_count, accepted_count, rejected_count, status

ingest.staged_row
  staged_row_id PK, dataset_load_id FK, source_row_number,
  source_key, raw_payload_json, payload_hash, validation_status, error_json
```

Artifact bytes მიდის დაცულ object storage-ში; DB-ში ინახება immutable URI/checksum. Validation-ზე rejected artifact-ს ემატება ცალკე `quarantine_uri`: ორიგინალი არ იშლება, ხოლო შეზღუდული წვდომის ასლი ინარჩუნებს custody/forensic trail-ს. `staged_row` retention policy-ით იწმინდება მხოლოდ publication/rollback window-ის შემდეგ.

### 5.2 `raw` — უცვლელი source truth

```text
raw.source_record
  source_record_id PK, dataset_snapshot_id, source_row_number,
  source_key, payload_json, payload_hash,
  artifact_id, extracted_at, valid_from, valid_to
```

აქ ინახება ნებისმიერი Access/CSV/JSON/XML/API source row მთლიანად, დაუკარგავად. ეს table **არ** არის chart/query serving layer.

### 5.3 `entity` — raw/operational/relational data

```text
entity.entity_record
  entity_id PK, dataset_snapshot_id, external_key,
  record_type, title, payload_json, payload_hash,
  source_record_id FK, valid_from, valid_to, is_current

entity.entity_link
  link_id PK, from_entity_id FK, relationship_type_id,
  to_entity_id FK, ordinal, valid_from, valid_to, source_record_id

entity.entity_classification
  entity_id FK, attribute_id, classification_item_id,
  valid_from, valid_to, source_record_id,
  PK(entity_id, attribute_id, classification_item_id, valid_from)
```

`entity_record` ინახავს readable business record-ს როგორც structured JSON payload-ს, არა გაბნეულ generic value table-ს. Contract განსაზღვრავს JSON path/type/label-ს. ხშირად გამოყენებული field-ები იღებს governed computed/indexed projection-ს, მაგრამ მხოლოდ performance ოპტიმიზაციისთვის; logical model არ იცვლება.

`entity_link` ფარავს PK/FK, parent-child, many-to-many bridge, document-child, translation, lineage და სხვა კავშირებს. ეს არის პირდაპირ წასაკითხი link table და არა key/value.

### 5.4 `statistics` — SDMX-compatible analytical cube

```text
statistics.series
  series_id PK, dataset_snapshot_id, metric_id,
  series_key_hash, unit_code, status, source_record_id

statistics.observation
  observation_id PK, series_id FK,
  period_start, period_end, observation_status,
  numeric_value decimal(28,10) NULL,
  text_value nvarchar(max) NULL,
  boolean_value bit NULL,
  source_record_id FK, valid_from, valid_to, is_current

statistics.observation_dimension
  observation_id FK,
  dimension_id,
  classification_item_id NULL,
  scalar_code NULL,
  PK(observation_id, dimension_id)

statistics.observation_attribute
  observation_id FK, attribute_code,
  value_json, PK(observation_id, attribute_code)
```

ეს არის fact/dimension bridge, არა generic EAV:

- `observation` ყოველთვის აქვს measure/value და period;
- `observation_dimension` ყოველთვის არის რეგისტრირებული statistical dimension;
- `classification_item_id` ყოველთვის მიდის versioned classifier-ზე;
- `series` აერთიანებს ერთსა და იმავე dimensional grain-ს;
- dataset-ის ახალი statistical field ხდება ახალი metric/dimension metadata, არა ახალი fact table.

ძირითადი index-ები:

```text
series(metric_id, series_key_hash, dataset_snapshot_id)
observation(series_id, period_start, is_current)
observation_dimension(dimension_id, classification_item_id, observation_id)
```

### 5.5 `geo`

```text
geo.feature
  feature_id PK, dataset_snapshot_id, feature_key,
  geometry_geojson, geometry_type, bbox, source_record_id, is_current

geo.feature_link
  feature_id FK, entity_id NULL, classification_item_id NULL,
  relationship_type_id, PK(feature_id, relationship_type_id)
```

Geo payload/CRS/validation profile იყენებს OGC-compatible contract-ს; map metric ებმის `classification_item_id` ან feature link-ს.

### 5.6 `reference` mirror, `publication` და `serving`

კლასიფიკატორის authoritative registry არის `geostat-system.reference.*`. SQL Server cross-database FK-ს არ იძლევა, ამიტომ ყოველი publication-ისას Data Plane იღებს იმავე immutable classification version-ის mirror-ს:

```text
reference.classification_item_snapshot
  snapshot_id, classification_item_id, scheme_version_id,
  code, label_ka, label_en, parent_item_id, status,
  PK(snapshot_id, classification_item_id)
```

`statistics.observation_dimension.classification_item_id` და `entity.entity_classification.classification_item_id` ყოველთვის მოწმდება იმავე `publication.snapshot_id`-ის mirror-ის მიმართ. Core რჩება owner-ად; Data Plane mirror არის მხოლოდ query performance/reproducibility-ისთვის.

### 5.7 `publication` და `serving`

```text
publication.snapshot
  snapshot_id PK, product_id, release_id, status, published_at, previous_snapshot_id

publication.snapshot_member
  snapshot_id FK, dataset_version_id, dataset_snapshot_id, row_count, checksum,
  PK(snapshot_id, dataset_version_id)

serving.metric_cache
  cache_id PK, snapshot_id, metric_id, dimension_signature,
  period_start, period_end, aggregate_value, refreshed_at
```

`serving.metric_cache` არის მხოლოდ performance cache; source of truth რჩება `statistics.*`. Chart data ცალკე არ ინახება.

## 6. Relation და მრავალი source table

ერთი external file/API შეიძლება შეიცავდეს მრავალ table-ს. თითო table ხდება `dataset_load`, მაგრამ ერთ `batch_id`/`snapshot_id`-ს ეკუთვნის.

```text
classifications
  → entity lookup/content records
    → linked entities
      → statistical series/observations
        → derived metrics
          → one atomic publication snapshot
```

Import engine dependency graph-ს იღებს `catalog.dataset_relationship`-იდან. იგი ატვირთავს topological order-ით. ციკლური relation ჯერ deferred link-ად ინახება, ბოლოს კი მთელი graph-ის referential validation სრულდება.

**წესი:** required relation-ის ან required dataset-ის ჩავარდნისას არც ერთი member არ გადადის ახალ published snapshot-ში.

## 7. ახალი მონაცემის მიღება გარედან

```text
Access / CSV / XLSX / JSON / XML / GeoJSON / API / DB connector
      ↓
format adapter
      ↓
ingest.artifact + catalog scan
      ↓
ingestion contract + schema/mapping validation
      ↓
ingest.staged_row
      ↓
raw.source_record
      ↓
entity / statistics / geo + relations
      ↓
quality + release approval
      ↓
publication.snapshot
```

ახალი format მხოლოდ adapter-ს ამატებს. ახალი site/table/field/classifier/relationship მხოლოდ Core metadata-ს ამატებს. ფიზიკური data-plane schema უცვლელი რჩება.

## 8. Migration და ზრდის წესები

| ცვლილება | ჩვეულებრივი ქმედება | ახალი physical table? |
|---|---|---:|
| ახალი site | data product + datasets | არა |
| ახალი raw structure | dataset version + JSON contract | არა |
| ახალი relation | relationship definition + links | არა |
| ახალი classifier | scheme/version/items | არა |
| ახალი statistic | metric/dimension + observations | არა |
| ახალი chart | visualization definition | არა |
| ახალი import format | adapter | არა |
| უკიდურესად დიდი მოცულობა | partition/shard/cache | არა, schema იგივეა |

მხოლოდ სრულიად ახალი **data family** — რომელიც არ არის raw, entity, classification, statistics ან geo — მოითხოვს architecture review-ს. ეს არის explicit extension point და არა მოულოდნელი schema break.

## 9. განხორციელების რიგი

1. შევქმნათ `geostat-data` და მისი 8 schema: `ingest`, `raw`, `entity`, `statistics`, `geo`, `reference`, `publication`, `serving`.
2. `geostat-system`-ში დავამატოთ ერთიანი `platform` schema: catalog, reference, semantic, ingestion, publication, visualization და governance table-ებით.
3. დავამატოთ immutable ID/event სინქრონიზაცია Core → Data Plane.
4. დავწეროთ Access adapter → staging → raw/entity/statistics import.
5. დავამატოთ classification და relation resolver.
6. დავამატოთ atomic publication და semantic query engine.
7. მხოლოდ ამის შემდეგ დავიწყოთ legacy site-ების ეტაპობრივი migration.

## 10. Definition of Done

- [ ] ახალი data product ან dataset ფიზიკურ business table-ს არ ქმნის;
- [ ] raw, entity, classification, statistics და geo ერთ data plane-ში მუშაობს;
- [ ] მრავალtable-იანი import ატომურად ინარჩუნებს relation graph-ს;
- [ ] classifier version/hierarchy/alias/translation მუშაობს;
- [ ] chart metric/dimension-ს ებმის value copy-ის გარეშე;
- [ ] ახალი input format მხოლოდ adapter-ით ერთვება;
- [ ] Core და Data Plane დამოუკიდებლად backup/scale/restore-დება;
- [ ] ყველა change lineage, quality, audit და rollback წესით მართვადია.
