# Core + KIDS (CIDS) — სრული Schema Draft

> სტატუსი: **target design, ჯერ არ არის production-ში შესრულებული**. საფუძვლად უდევს 2026-09-08-ის read-only inspection: Core `geostat-system` და CIDS source `kids`.

## 1. არსებული, დადასტურებული მდგომარეობა

### 1.1 Core — `geostat-system.dbo`

არსებული legacy/system table-ები უცვლელად რჩება:

```text
users, roles, permissions, user_roles, role_permissions, tokens, user_profile
page_nodes, migrations
auto_eoes, main, prices, prices_1, sliders_data
```

უკვე შექმნილი platform foundation table-ები:

```text
data_profiles              1 row
import_table_mappings      1 row
chart_definitions          1 row
import_jobs                0 rows
import_job_items           0 rows
```

არსებული კავშირები:

```text
data_profiles 1 ── * import_table_mappings
data_profiles 1 ── * chart_definitions
import_jobs   1 ── * import_job_items
data_profiles 1 ── * import_job_items
import_table_mappings 1 ── * import_job_items
```

ეს foundation თავსებადობისთვის რჩება, მაგრამ v2 master architecture-ისთვის გაფართოვდება და არ იქნება ერთადერთი metadata model.

### 1.2 KIDS — `kids.dbo`

| Table | Rows | Primary key | არსებული columns |
|---|---:|---|---|
| `files` | 225 | `ID` | `ID`, `category`, `sub_category`, `title_geo`, `title_eng`, `path_geo`, `path_eng`, `chartdata` |
| `glossary` | 178 | `ID` | `ID`, `lang`, `text` |
| `goals` | 36 | `ID` | `ID`, `category`, `title_geo`, `title_eng`, `path_geo`, `path_eng` |
| `goals_titles` | 17 | `ID` | `ID`, `category`, `title_geo`, `title_eng` |

Foreign key და დამატებითი business index **არ არსებობს**. ეს ოთხი table რჩება legacy/read-only source-ად.

---

## 2. საბოლოო საზღვარი: ვინ რას ფლობს

```text
Core: geostat-system
  ფლობს: metadata, policy, version, approval, audit, chart definition, publication pointer
  არ ფლობს: CIDS-ის დიდი raw/statistical row-ები

Child: kids
  ფლობს: CIDS-ის რეალურ content/raw/lookup/statistical row-ებს, staging და published views
  არ ფლობს: password, global user/role, import approval authority
```

## 3. Core v2 target schema

ყველა ახალი Core table განთავსდება `dbo` schema-ში. საიდუმლო connection string/password **არ ინახება** ამ table-ებში; მხოლოდ server-side config key/reference ინახება.

### 3.1 Product და target policy

```text
data_products
  id                         bigint PK
  product_code               nvarchar(120) UQ          -- cids
  page_id                    bigint FK page_nodes(id)  -- CIDS portal PAGE
  title_ka                   nvarchar(255)
  title_en                   nvarchar(255)
  status                     varchar(20)               -- DRAFT/ACTIVE/ARCHIVED
  owner_user_id              bigint FK users(id)
  created_at, updated_at     datetime2

product_targets
  id                         bigint PK
  product_id                 bigint FK data_products(id)
  connection_ref             nvarchar(160)             -- e.g. KIDS_SQLSERVER
  target_database            nvarchar(128)             -- kids
  target_schema              nvarchar(128)             -- cids
  database_policy            varchar(32)               -- FIXED_DATABASE/CREATE_DATABASE_ALLOWED
  ddl_policy                 varchar(32)               -- NONE/CREATE_ONLY/ADDITIVE_ONLY/APPROVED_MIGRATION
  staging_schema             nvarchar(128)             -- cids_staging
  publication_strategy       varchar(32)               -- VIEW_POINTER/SCHEMA_SWAP
  enabled                    bit
```

`data_profiles` რჩება legacy/v1 bridge-ად და მიიღებს `product_id`, `target_id`, `ddl_policy`, `dataset_code` FK-ებს მხოლოდ compatibility migration-ის შემდეგ.

### 3.2 Dataset და schema version

```text
datasets
  id                         bigint PK
  product_id                 bigint FK data_products(id)
  dataset_code               nvarchar(120)
  logical_name               nvarchar(160)
  dataset_kind               varchar(20)               -- CONTENT/RAW/LOOKUP/STATISTICAL/GEO
  target_table               nvarchar(128)
  load_mode                  varchar(20)               -- SNAPSHOT_REPLACE/UPSERT/APPEND
  required                   bit
  status                     varchar(20)
  UQ(product_id, dataset_code)

dataset_schema_versions
  id                         bigint PK
  dataset_id                 bigint FK datasets(id)
  version                    int
  manifest_checksum          char(64)
  schema_status              varchar(20)               -- DRAFT/APPROVED/PUBLISHED/SUPERSEDED
  created_by_user_id         bigint FK users(id)
  approved_by_user_id        bigint FK users(id) NULL
  created_at, approved_at    datetime2
  UQ(dataset_id, version)

dataset_fields
  id                         bigint PK
  schema_version_id          bigint FK dataset_schema_versions(id)
  ordinal                    int
  source_field               nvarchar(128)
  target_field               nvarchar(128)
  logical_type               varchar(20)
  database_type              nvarchar(64)
  field_role                 varchar(20)               -- IDENTIFIER/DIMENSION/MEASURE/LABEL/CONTENT/JSON
  nullable                   bit
  max_length, precision, scale int NULL
  label_ka, label_en         nvarchar(255) NULL
  unit                       nvarchar(64) NULL
  is_visible, is_filterable, is_groupable bit
  UQ(schema_version_id, target_field)
```

### 3.3 Key, index და relation metadata

```text
dataset_keys
  id, schema_version_id FK, key_name, key_type (PRIMARY/BUSINESS/UNIQUE), is_enforced

dataset_key_fields
  id, key_id FK, field_id FK dataset_fields, ordinal

dataset_indexes
  id, schema_version_id FK, index_name, is_unique, is_enforced

dataset_index_fields
  id, index_id FK, field_id FK dataset_fields, ordinal, sort_direction

dataset_relations
  id, schema_version_id FK, relation_name,
  from_dataset_id FK datasets, from_field_code,
  to_dataset_id FK datasets, to_field_code,
  cardinality (ONE_TO_ONE/ONE_TO_MANY), is_required, is_enforced
```

`field_id` ყოველთვის ეკუთვნის იმავე `schema_version_id`-ს; ეს DB constraint/trigger-ით ან service validation-ით სავალდებულოდ მოწმდება.

### 3.4 Chart, publication და import audit

```text
chart_definitions_v2
  id                         bigint PK
  product_id                 bigint FK data_products(id)
  chart_code                 nvarchar(120)
  dataset_id                 bigint FK datasets(id)
  chart_type                 varchar(32)
  title_ka, title_en         nvarchar(255)
  status                     varchar(16)               -- DRAFT/REVIEW/PUBLISHED/RETIRED
  UQ(product_id, chart_code)

chart_versions
  id, chart_definition_id FK, version, schema_version_id FK,
  x_field_code, y_field_code, series_field_code, aggregation,
  config_json, created_at, created_by_user_id FK

chart_series
  id, chart_version_id FK, field_code, label_ka, label_en, color, sort_order

chart_filters
  id, chart_version_id FK, field_code, operator, value_json, sort_order

publication_versions
  id                         bigint PK
  product_id                 bigint FK data_products(id)
  version                    int
  status                     varchar(20)               -- DRAFT/REVIEW/PUBLISHED/ROLLED_BACK
  published_by_user_id       bigint FK users(id) NULL
  published_at               datetime2 NULL
  previous_publication_id    bigint FK publication_versions(id) NULL
  UQ(product_id, version)

publication_datasets
  id, publication_id FK, dataset_id FK, schema_version_id FK,
  physical_table_name, row_count, checksum

publication_charts
  id, publication_id FK, chart_version_id FK

import_jobs
  არსებული table ფართოვდება: product_id, publication_id, manifest_version,
  preview_checksum, actor_id, lifecycle/status, requested_ddl_plan_json

import_job_items
  არსებული table ფართოვდება: dataset_id, schema_version_id, staging_table,
  target_physical_table, schema_diff_json, validation_result_json

schema_migration_jobs
  id, import_job_id FK, target_id FK product_targets,
  plan_checksum, ddl_plan_json, status, executed_at, rollback_plan_json

validation_results
  id, import_job_item_id FK, rule_code, severity, field_code,
  source_row_reference, message_ka, message_en, created_at
```

### 3.5 აუცილებელი Core index-ები

```text
data_products(product_code) UNIQUE
datasets(product_id, dataset_code) UNIQUE
dataset_schema_versions(dataset_id, version) UNIQUE
dataset_fields(schema_version_id, target_field) UNIQUE
chart_definitions_v2(product_id, chart_code) UNIQUE
publication_versions(product_id, status, version DESC)
import_jobs(product_id, status, created_at DESC)
import_job_items(import_job_id, dataset_id) UNIQUE
validation_results(import_job_item_id, severity)
```

---

## 4. KIDS target schema

### 4.1 Schema boundaries

```text
kids.dbo             legacy source; არ იცვლება
kids.cids_staging    import-ის დროებითი physical table-ები
kids.cids_data       versioned private published data table-ები
kids.cids            stable views და, საჭიროებისას, curated table-ები
```

`cids_staging` და `cids_data` იქმნება მხოლოდ product target provisioning policy-ით. Public API მხოლოდ `cids` schema-ს ხედავს.

### 4.2 CIDS content datasets

ეს არის `kids.dbo` source-ის სემანტიკური v2 target; ძველი სახელები/fields Access manifest-ში source mapping-ით რჩება.

```text
cids_data.content_files__v<N>
  file_id                     int             PK
  category_code               int             NOT NULL
  sub_category_code           nvarchar(11)    NOT NULL
  title_ka                    nvarchar(max)   NOT NULL
  title_en                    nvarchar(max)   NOT NULL
  path_ka                     nvarchar(max)   NOT NULL
  path_en                     nvarchar(max)   NOT NULL
  chart_data_json             nvarchar(max)   NOT NULL
  publication_version_id      bigint          NOT NULL
  imported_at                 datetime2       NOT NULL

cids_data.glossary_entries__v<N>
  glossary_id                 int             PK
  language_code               nvarchar(16)    NOT NULL
  body_text                   nvarchar(max)   NOT NULL
  publication_version_id      bigint          NOT NULL

cids_data.goals__v<N>
  goal_id                     int             PK
  category_code               int             NOT NULL
  title_ka, title_en          nvarchar(max)   NOT NULL
  path_ka, path_en            nvarchar(max)   NOT NULL
  publication_version_id      bigint          NOT NULL

cids_data.goal_titles__v<N>
  goal_title_id               int             PK
  category_code               int             NOT NULL
  title_ka, title_en          nvarchar(max)   NOT NULL
  publication_version_id      bigint          NOT NULL
```

Index-ები:

```text
content_files__v<N>(category_code, sub_category_code)
glossary_entries__v<N>(language_code)
goals__v<N>(category_code)
goal_titles__v<N>(category_code) UNIQUE
```

`chart_data_json` source fidelity-სთვის რჩება; იგი ავტომატურად არ გადაიქცევა უსათაურო `chart_1` table-ად.

### 4.3 CIDS statistical semantic model

ეს table-ები იქმნება მხოლოდ მაშინ, როცა data steward chartdata-ის თითო dataset-ს მისცემს დასახელებას, source-ს, unit-სა და dimension მნიშვნელობებს.

```text
cids_data.indicators__v<N>
  indicator_id                bigint IDENTITY PK
  indicator_code              nvarchar(120) NOT NULL UNIQUE
  title_ka, title_en          nvarchar(255) NOT NULL
  unit_code                   nvarchar(64) NULL
  source_name                 nvarchar(255) NULL
  decimal_precision           tinyint NULL

cids_data.age_groups__v<N>
  age_group_id                bigint IDENTITY PK
  age_group_code              nvarchar(64) NOT NULL UNIQUE    -- AGE_0_17
  label_ka, label_en          nvarchar(128) NOT NULL
  age_from, age_to            smallint NULL

cids_data.regions__v<N>
  region_id                   bigint PK
  region_code                 nvarchar(32) UNIQUE
  label_ka, label_en          nvarchar(255)

cids_data.sexes__v<N>
  sex_id                      tinyint PK
  sex_code                    nvarchar(16) UNIQUE
  label_ka, label_en          nvarchar(64)

cids_data.observations__v<N>
  observation_id              bigint IDENTITY PK
  indicator_id                bigint NOT NULL
  observation_year            smallint NOT NULL
  age_group_id                bigint NULL
  region_id                   bigint NULL
  sex_id                      tinyint NULL
  category_code               nvarchar(64) NULL
  value_decimal               decimal(20,6) NOT NULL
  source_file_id              int NULL
  publication_version_id      bigint NOT NULL
  imported_at                 datetime2 NOT NULL
```

FK-ები statistical physical table-ში:

```text
observations.indicator_id → indicators.indicator_id
observations.age_group_id → age_groups.age_group_id
observations.region_id    → regions.region_id
observations.sex_id       → sexes.sex_id
```

მთავარი analytical indexes:

```text
observations(indicator_id, observation_year, age_group_id, region_id, sex_id)
observations(observation_year, indicator_id)
UNIQUE(indicator_id, observation_year, age_group_id, region_id, sex_id, category_code)
```

### 4.4 Stable public views

```text
cids.content_files        → SELECT ... FROM cids_data.content_files__v<current>
cids.glossary_entries     → SELECT ... FROM cids_data.glossary_entries__v<current>
cids.goals                → SELECT ... FROM cids_data.goals__v<current>
cids.goal_titles          → SELECT ... FROM cids_data.goal_titles__v<current>
cids.observations         → SELECT ... FROM cids_data.observations__v<current>
```

Publication დროს Core `publication_datasets.physical_table_name` და view pointer იცვლება ერთ controlled transaction-ში. API მკითხველი public view-ს იყენებს; writer მხოლოდ staging/version table-ზე წერს.

### 4.5 Staging pattern

```text
cids_staging.stg_content_files__job_481
cids_staging.stg_glossary_entries__job_481
cids_staging.stg_goals__job_481
cids_staging.stg_goal_titles__job_481
cids_staging.stg_observations__job_481
```

job წარმატების შემდეგ staging table გადადის versioned `cids_data` table-ში ან იტვირთება იქ transaction-ით. ჩავარდნისას staging cleanup მხოლოდ audit/retention policy-ის შემდეგ სრულდება; public view არ იცვლება.

---

## 5. რეალური CIDS package → target mapping

```text
Access source                  Manifest dataset              KIDS published target
-------------------------------------------------------------------------------------------
files                          CIDS_FILES                    cids.content_files
glossary                       CIDS_GLOSSARY                 cids.glossary_entries
goals                          CIDS_GOALS                    cids.goals
goals_titles                   CIDS_GOAL_TITLES              cids.goal_titles
<curated statistics table>     CIDS_OBSERVATIONS             cids.observations
<curated indicator lookup>     CIDS_INDICATORS               cids.indicators
```

`files.chartdata` რჩება `chart_data_json`-ად მანამ, სანამ მას არ ექნება approved statistical manifest. ამის შემდეგ package-ში ჩნდება ახალი, სახელდებული data table, მაგალითად `cids_population_by_age`, და მისგან იქმნება `observations`.

## 6. განხორციელების თანმიმდევრობა

1. Core-ში შევქმნათ `data_products` + `product_targets` და მივაბათ CIDS portal-ის ახალი PAGE node.
2. შევქმნათ `kids.cids_staging`, `kids.cids_data`, `kids.cids` schema-ები.
3. შევიტანოთ CIDS content dataset schema version-ები და package v2.
4. Preview-ით დადასტურდეს Core manifest ↔ Access schema ↔ KIDS DDL plan.
5. staging import, quality checks და versioned publication.
6. მხოლოდ შემდეგ მოვამზადოთ `chartdata`-ის semantic mapping და statistical tables.
7. ბოლო ეტაპზე გამოვაქვეყნოთ Core chart definitions და CIDS API.

## 7. აკრძალვები

- `kids.dbo` legacy table-ების rename/drop/alter აკრძალულია ამ migration-ში;
- Core-ში CIDS raw/statistical rows არ ჩაიწერება;
- Access ვერ შეიცავს target password/host/raw SQL;
- Access manifest ვერ ქმნის DB/table-ს Core target policy/approved DDL plan-ის გარეშე;
- `chartdata` ვერ გარდაიქმნება business dataset-ად title/unit/source/dimension contract-ის გარეშე.
