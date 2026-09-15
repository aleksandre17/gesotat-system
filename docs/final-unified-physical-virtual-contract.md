# Geostat/KIDS — საბოლოო შეჯერებული ფიზიკური და ვირტუალური კონტრაქტი

**სტატუსი:** normative / implementation-ready / UI-independent  
**ვერსია:** 1.0  
**მიზანი:** ყველა წყაროს, virtual dataset-ის, Access transport-ისა და canonical physical Data Plane-ის ერთი საბოლოო კონტრაქტი.  
**არ არის ამ დოკუმენტის ნაწილი:** UI-ის ვიზუალური დიზაინი. UI მხოლოდ ამ კონტრაქტიდან გენერირდება.

## 1. საბოლოო გადაწყვეტილება

სისტემაში სამი განსხვავებული რამ არ უნდა აირიოს:

1. **Virtual contract dataset** — ბიზნესისა და პანელის მიერ გამოცხადებული ლოგიკური ცხრილი და მისი სრული schema. მაგალითად `KIDS_GOAL`, `KIDS_RESOURCE`, `KIDS_STATISTICAL_INPUT`.
2. **Access transport package** — `.accdb`-ში არსებული staging/transport ობიექტები, რომელთა მიზანია გამოცხადებული virtual კონტრაქტის შეფუთვა და გადატანა. მისი სახელები შეიძლება იყოს `__ent_kids_goal`, `__stat_kids_statistical_input` და ა.შ.; ეს არის transport namespace და არა canonical SQL Data Plane-ის საბოლოო ფიზიკური მოდელი.
3. **Canonical physical Data Plane** — სტაბილური, generic, მრავალწყაროიანი SQL ცხრილები. ახალი საიტის, dataset-ის, ველის, carrier-ის, metric-ის ან relation-ის დამატება აქ ახალი ბიზნეს-ცხრილის შექმნას არ იწვევს; ახალი semantics იწერება metadata/registry-ში და მონაცემი მიდის შესაბამის generic family-ში.

**სავალდებულო წესი:** KIDS-specific Access table-ები არ უნდა აღვიქვათ როგორც საბოლოო canonical SQL table-ები. ისინი შეიძლება დარჩეს Access package-ში, მაგრამ SQL-ში mapping ყოველთვის მიდის generic physical family-ზე.

## 2. ნორმატიული წყაროების პრიორიტეტი

კონფლიქტის შემთხვევაში გამოიყენება შემდეგი რიგი:

1. `final-physical-database-architecture.md` — canonical physical architecture;
2. `unified-canonical-platform-final.md` — platform-wide canonical model;
3. `kids-semantic-onboarding-spec.md` და `final-kids-canonical-model.md` — KIDS virtual/source contract;
4. `access-and-control-plane-responsibility-model.md` — Access boundary and responsibilities;
5. `d.txt`, implementation/acceptance notes — execution constraints;
6. ძველი draft-ები/hand-off-ები — მხოლოდ ისტორიული reference, ახალი წესის override არა.

## 3. ფენები და პასუხისმგებლობები

| ფენა | დანიშნულება | ავტორიტეტი |
|---|---|---|
| Control Plane | contract, schema, version, identity, classification, relation, semantic, quality, privacy, approval, UI metadata | პანელი |
| Shared Data Plane | ingest, raw, entity, statistics, geo, publication, serving | canonical SQL |
| Archive Plane | immutable source payload, checksums, retention, legal hold | archive/object storage |
| Access transport | offline/import/export package, staging, validation report | Access generator/executor |

პანელი ქმნის და ამტკიცებს კონტრაქტს; generator ქმნის შესაბამის Access package-ს; Access ავსებს/აგზავნის მონაცემს; Data Plane ამოწმებს, არეგისტრირებს lineage-ს და materialize-ს აკეთებს მხოლოდ approved semantics-ის მიხედვით.

### 3.1 საბოლოო არქიტექტურული ნახაზი

```text
                    ┌────────────────────────────┐
                    │ Control Plane / Panel       │
                    │ contracts, schema, rules    │
                    │ approval, lineage, UI meta  │
                    └──────────────┬─────────────┘
                                   │ approved version
                    ┌──────────────▼─────────────┐
                    │ Contract Generator         │
                    │ deterministic Access .accdb│
                    └──────────────┬─────────────┘
                                   │ transport package
          source files/API ────────▼──────────────┐
          ┌───────────────────────────────────────┴──┐
          │ Access transport (__gs_, __ent_, __stat_)│
          │ validate → stage → export; no new logic  │
          └──────────────────────┬───────────────────┘
                                 │ ingest + lineage
       ┌─────────────────────────▼─────────────────────────┐
       │ Canonical SQL Data Plane                           │
       │ ingest → raw → entity/statistics/geo → publication │
       └───────────────┬───────────────────────┬───────────┘
                       │                       │
               ┌───────▼────────┐      ┌───────▼──────────┐
               │ Archive Plane   │      │ Serving/exports  │
               │ immutable raw   │      │ snapshots/cache │
               └─────────────────┘      └──────────────────┘
```

## 4. სახელთა სივრცე და იდენტობა

ყველა table/view/contract/field/index/relation-ს აქვს სტაბილური იდენტობა და prefix. Prefix არ არის მხოლოდ ვიზუალური სახელი — ის namespace invariant-ია.

| prefix | ობიექტის ოჯახი |
|---|---|
| `__gs_` | geostat/system platform |
| `__cl_` | classification |
| `__stat_` | statistical semantic/observation |
| `__raw_` | raw immutable source carrier |
| `__ent_` | entity/master record |
| `__rel_` | explicit relation/link |
| `__geo_` | geospatial |
| `__pub_` | publication/snapshot |
| `__serv_` | serving/cache |
| `__arch_` | archive |
| `__audit_` | audit/security |
| `__sys_` | technical system |
| `__idx_` | contract-declared index identity |
| `__kids_` | KIDS virtual/Access transport namespace only |

Canonical SQL schema names use the corresponding family schema (`platform`, `classification`, `raw`, `entity`, `relation`, `statistics`, `geo`, `publication`, `serving`, `archive`, `audit`, `system`). Physical SQL tables must not be named after one site or one dataset.

## 4.1 ფიზიკური ბაზების საბოლოო განაწილება

ეს არის deployment-ის ნორმატიული მატრიცა. ცხრილი ერთდროულად არ უნდა შეიქმნას ორ canonical ბაზაში.

### A. `geostat-system` — Control Plane database

ამ ბაზაში ინახება მხოლოდ გამოცხადებული კონტრაქტისა და მართვის metadata:

| Schema | Physical tables |
|---|---|
| `platform` | `contract_namespace`, `contract_structure`, `contract_table_definition`, `contract_field_definition`, `contract_index_definition`, `contract_index_column`, `contract_structure_relation`, `contract_value_domain`, `contract_domain_item`, `contract_semantic_binding`, `contract_classification_binding`, `contract_quality_rule`, `contract_privacy_rule`, `contract_lineage_rule`, `contract_ui_surface`, `contract_object_alias`, `contract_approval`, `contract_migration` |
| `classification` | `system`, `item`, `mapping` — მხოლოდ classification registry/mirror; ფაქტობრივი dataset values ინახება `geostat-data`-ში |
| `system` | `code`, `job_run` |
| `audit` | `event`, `access_decision` |

`geostat-system`-ში არ უნდა ჩაიწეროს source payload, raw document, statistical observation ან KIDS-ის ბიზნეს-ჩანაწერი.

### B. `geostat-data` — Canonical Shared Data Plane database

ამ ბაზაში ინახება ingest-დან მიღებული და კონტრაქტით დამტკიცებული ფაქტობრივი მონაცემი:

| Schema | Physical tables |
|---|---|
| `ingest` | `batch`, `artifact`, `dataset_load`, `staged_row`, `validation_issue`, `load_checkpoint` |
| `raw` | `source_record`, `document` (Access alias: `__raw_document`) |
| `entity` | `entity_record`, `entity_link`, `entity_classification`, `localized_text`, `resource_locator` |
| `statistics` | `series`, `observation`, `observation_dimension`, `observation_attribute`, `revision` |
| `classification` | `system_snapshot`, `item_snapshot`, `mapping_snapshot` — Control Plane registry-ის read-only data snapshot; აქ არ იქმნება მეორე authoritative registry |
| `geo` | `feature`, `feature_link` |
| `publication` | `snapshot`, `dataset_snapshot`, `snapshot_member` |
| `serving` | `metric_cache` |
| `audit` | ingestion/materialization/publication-ის audit events |

`geostat-data`-ში ახალი site/dataset-ისთვის ახალი physical business table არ იქმნება. მონაცემი გადის generic family mapping-ით და dataset identity ინახება `structure_id`/`dataset_code`-ით.

### C. `geostat-archive` — Archive store/database

ეს არის immutable retention boundary:

| Schema | Physical tables |
|---|---|
| `archive` | `retention_register`, `payload_pointer` |

დიდი ან ორიგინალი payload ინახება object/blob storage-ში; `payload_pointer` ინახავს URI-ს, checksum-ს, tier-ს, encryption-სა და immutability-ს. Archive-ში მონაცემი overwrite არ უნდა მოხდეს; correction ყოველთვის ახალი artifact/version-ია.

### D. Access `.accdb` — transport package, არა canonical database

Access ფაილში generator ქმნის `__gs_*` contract metadata-ს, `__ent_*`, `__rel_*`, `__stat_*`, `__raw_*` transport tables-ს და validation/staging ცხრილებს. ეს ობიექტები გამოიყენება import/export-ისთვის და მათი საბოლოო mapping ხდება ზემოთ მოცემულ `geostat-system`, `geostat-data` და `geostat-archive` store-ებში. Access-ში დამატებული KIDS-specific table არ ნიშნავს ახალი canonical SQL table-ის საჭიროებას.

### 4.2 ბაზებს შორის დასაშვები კავშირები

* `geostat-system.platform.contract_structure.structure_id` არის კონტრაქტის ავტორიტეტული იდენტობა; `geostat-data`-ის ingest/raw/entity/statistics rows მას მხოლოდ reference-ად იყენებენ.
* `geostat-data` ინახავს `source_record_id`/`artifact_id` reference-ს; თვითონ ორიგინალი payload ინახება `geostat-archive`-ში.
* `geostat-system`-ის classification/quality/privacy/semantic წესები versioned reference-ით გამოიყენება `geostat-data` materialization-ზე.
* circular write-back დაუშვებელია: Data Plane ვერ ცვლის APPROVED contract-ს; ცვლილება იწყება Control Plane-ში და ახალი contract version-ით ვრცელდება.

### 4.1 Identity tuple

Every governed object has:

`object_id`, `namespace`, `object_type`, `canonical_name`, `display_name`, `version`, `status`, `valid_from`, `valid_to`, `owner_org_id`, `created_at`, `created_by`, `updated_at`, `updated_by`, `definition_hash`.

`object_id` is immutable UUID/ULID. `canonical_name` is unique inside namespace. Rename creates a new alias, never silent mutation. Version is immutable after approval.

## 5. Control Plane — სრული ფიზიკური metadata tables

ქვემოთ მოცემული ცხრილები არის contract registry-ის canonical physical model. ყველა PK/FK, uniqueness და lifecycle state სავალდებულოა.

### 5.1 `platform.contract_namespace`

`namespace_id PK`, `namespace_code UNIQUE`, `prefix UNIQUE`, `namespace_type`, `parent_namespace_id FK nullable`, `owner_org_id`, `description`, `allowed_object_types_json`, `status`, `valid_from`, `valid_to`, `created_at`, `created_by`, `updated_at`, `updated_by`, `definition_hash`.

### 5.2 `platform.contract_structure`

ერთი virtual dataset/contract-ის root.

`structure_id PK`, `structure_code UNIQUE`, `namespace_id FK`, `dataset_code`, `dataset_version`, `display_name`, `description`, `domain_code`, `granularity`, `contract_type`, `source_system_code`, `source_uri_pattern`, `transport_format`, `canonical_physical_family`, `schema_hash`, `status` (`DRAFT|REVIEW|APPROVED|PUBLISHED|DEPRECATED|RETIRED`), `effective_from`, `effective_to`, `owner_org_id`, `steward_org_id`, `classification_level`, `retention_policy_code`, `quality_policy_code`, `privacy_policy_code`, `lineage_policy_code`, `ui_generation_profile_code`, `created_at`, `created_by`, `updated_at`, `updated_by`, `approved_at`, `approved_by`.

### 5.3 `platform.contract_table_definition`

Virtual table/view/package member.

`table_def_id PK`, `structure_id FK`, `table_code`, `table_prefix`, `table_role` (`ROOT|CHILD|BRIDGE|CARRIER|DIMENSION|FACT|LOOKUP|VIEW`), `physical_family`, `physical_schema`, `physical_table`, `record_type_code`, `description`, `is_required`, `is_repeatable`, `is_temporal`, `is_statistical`, `is_raw`, `ordinal`, `status`, `definition_hash`.

Unique: `(structure_id, table_code, dataset_version)`; prefix must match namespace/table role.

### 5.4 `platform.contract_field_definition`

`field_def_id PK`, `table_def_id FK`, `field_code`, `field_prefix`, `ordinal`, `label`, `description`, `data_type`, `logical_type`, `physical_type`, `length`, `precision`, `scale`, `is_nullable`, `is_required`, `is_pk`, `is_natural_key`, `is_fk`, `is_business_key`, `is_measure`, `is_dimension`, `is_attribute`, `is_system_field`, `is_pii`, `is_confidential`, `classification_level`, `unit_code`, `value_domain_code`, `code_list_code`, `reference_structure_id`, `reference_table_code`, `reference_field_code`, `default_expression`, `validation_expression`, `format_pattern`, `min_value`, `max_value`, `allowed_values_json`, `timezone_policy`, `language_policy`, `sensitivity_policy`, `ordinal_path`, `status`, `definition_hash`.

Unique: `(table_def_id, field_code, dataset_version)` and `(table_def_id, ordinal)`.

### 5.5 `platform.contract_index_definition`

`index_def_id PK`, `table_def_id FK`, `index_code`, `index_prefix`, `index_type` (`PK|UK|FK|SEARCH|BRIN|GIST|HASH|CHECK`), `is_unique`, `is_clustered`, `is_required`, `predicate_expression`, `include_fields_json`, `ordinal`, `status`, `definition_hash`.

### 5.6 `platform.contract_index_column`

`index_column_id PK`, `index_def_id FK`, `field_def_id FK`, `column_ordinal`, `sort_direction`, `nulls_order`, `expression`, `collation`.

### 5.7 `platform.contract_structure_relation`

`relation_id PK`, `structure_id FK`, `relation_code`, `relation_prefix`, `relation_type` (`ONE_TO_ONE|ONE_TO_MANY|MANY_TO_ONE|MANY_TO_MANY|TEMPORAL|HIERARCHICAL|DERIVED`), `from_table_def_id FK`, `to_table_def_id FK`, `from_field_def_id FK`, `to_field_def_id FK`, `bridge_table_def_id FK nullable`, `cardinality_min`, `cardinality_max`, `on_delete`, `on_update`, `is_identifying`, `is_required`, `validity_rule`, `semantic_role`, `status`, `definition_hash`.

### 5.8 `platform.contract_value_domain`

`domain_id PK`, `domain_code UNIQUE`, `domain_type` (`CODELIST|RANGE|REGEX|UNIT|TIME|GEOMETRY|ENUM`), `value_type`, `unit_code`, `validation_expression`, `privacy_rule_code`, `status`, `version`, `definition_hash`.

### 5.9 `platform.contract_domain_item`

`domain_item_id PK`, `domain_id FK`, `item_code`, `item_label`, `item_definition`, `parent_item_id FK nullable`, `sort_order`, `valid_from`, `valid_to`, `status`, `definition_hash`.

### 5.10 `platform.contract_semantic_binding`

`binding_id PK`, `structure_id FK`, `table_def_id FK`, `field_def_id FK nullable`, `semantic_code`, `semantic_version`, `semantic_type` (`DIMENSION|MEASURE|ATTRIBUTE|IDENTIFIER|TIME|GEO|CLASSIFICATION|PROVENANCE`), `concept_scheme`, `concept_code`, `unit_code`, `aggregation_code`, `population_scope`, `quality_rule_code`, `privacy_rule_code`, `source_expression`, `target_physical_family`, `materialization_policy`, `is_approved`, `approved_at`, `approved_by`, `definition_hash`.

### 5.11 `platform.contract_classification_binding`

`classification_binding_id PK`, `structure_id FK`, `table_def_id FK`, `field_def_id FK`, `classification_system_code`, `classification_version`, `classification_item_code`, `mapping_type`, `mapping_confidence`, `valid_from`, `valid_to`, `is_required`, `status`.

### 5.12 `platform.contract_quality_rule`

`quality_rule_id PK`, `rule_code UNIQUE`, `rule_type`, `severity`, `expression`, `threshold_value`, `threshold_unit`, `null_policy`, `duplicate_policy`, `quarantine_policy`, `error_message_template`, `owner_org_id`, `status`, `version`.

### 5.13 `platform.contract_privacy_rule`

`privacy_rule_id PK`, `rule_code UNIQUE`, `classification_level`, `allowed_roles_json`, `masking_strategy`, `aggregation_minimum_n`, `suppression_threshold`, `small_cell_rule`, `retention_days`, `export_policy`, `audit_required`, `status`, `version`.

### 5.14 `platform.contract_lineage_rule`

`lineage_rule_id PK`, `structure_id FK`, `source_artifact_pattern`, `source_field_path`, `transformation_expression`, `target_table_def_id FK`, `target_field_def_id FK`, `algorithm_version`, `reproducibility_required`, `status`.

### 5.15 `platform.contract_ui_surface`

UI does not invent schema. It reads:

`ui_surface_id PK`, `structure_id FK`, `table_def_id FK nullable`, `field_def_id FK nullable`, `surface_type`, `component_type`, `label`, `help_text`, `display_order`, `section_code`, `visibility_rule`, `editability_rule`, `required_rule`, `lookup_domain_code`, `format_mask`, `validation_message`, `role_policy_code`, `status`.

### 5.16 `platform.contract_object_alias`

Backward-compatible names are explicit metadata: `alias_id PK`, `object_id`, `object_type`, `namespace_id FK`, `alias_name`, `alias_version`, `valid_from`, `valid_to`, `is_preferred`, `reason`, `created_at`, `created_by`. Aliases never change the immutable object identity.

### 5.17 `platform.contract_approval`

`approval_id PK`, `structure_id FK`, `version`, `approval_type` (`TECHNICAL|BUSINESS|PRIVACY|QUALITY|PUBLICATION`), `decision`, `approver_principal`, `decision_at`, `comment`, `evidence_uri`, `approval_hash`.

### 5.18 `platform.contract_migration`

`migration_id PK`, `structure_id FK`, `from_version`, `to_version`, `migration_type`, `forward_plan_json`, `rollback_plan_json`, `breaking_change_flag`, `executed_at`, `executed_by`, `status`, `migration_hash`.

## 6. Canonical physical Data Plane — complete table families

### 6.1 Ingest family (`ingest.*`)

#### `ingest.batch`

`batch_id PK`, `batch_code UNIQUE`, `source_system_code`, `source_org_id`, `received_at`, `started_at`, `completed_at`, `requested_by`, `ingestion_mode`, `contract_structure_id`, `contract_version`, `status`, `row_count`, `accepted_count`, `rejected_count`, `quarantined_count`, `error_count`, `checksum`, `idempotency_key UNIQUE`, `created_at`, `updated_at`.

#### `ingest.artifact`

`artifact_id PK`, `batch_id FK`, `artifact_uri`, `artifact_name`, `mime_type`, `byte_size`, `checksum_algorithm`, `checksum`, `source_modified_at`, `received_at`, `compression_code`, `encryption_code`, `retention_policy_code`, `confidentiality_level`, `provenance_json`, `status`.

#### `ingest.dataset_load`

`dataset_load_id PK`, `batch_id FK`, `artifact_id FK`, `structure_id FK`, `table_def_id FK`, `target_physical_family`, `load_mode`, `schema_hash`, `started_at`, `completed_at`, `status`, `row_count`, `accepted_count`, `rejected_count`, `error_count`, `idempotency_key UNIQUE`.

#### `ingest.staged_row`

`staged_row_id PK`, `dataset_load_id FK`, `source_row_number`, `source_record_key`, `payload_json`, `payload_hash`, `schema_version`, `validation_status`, `quarantine_reason`, `created_at`.

#### `ingest.validation_issue`

`issue_id PK`, `batch_id FK`, `dataset_load_id FK`, `staged_row_id FK nullable`, `field_code`, `rule_code`, `severity`, `issue_code`, `issue_message`, `raw_value_preview`, `is_resolved`, `resolved_at`, `resolved_by`, `created_at`.

#### `ingest.load_checkpoint`

`checkpoint_id PK`, `batch_id FK`, `dataset_load_id FK`, `checkpoint_type`, `checkpoint_key`, `checkpoint_value`, `created_at`, `worker_id`.

### 6.2 Raw family (`raw.*`)

#### `raw.source_record`

Immutable row-level source envelope: `source_record_id PK`, `batch_id FK`, `artifact_id FK`, `structure_id FK nullable`, `source_system_code`, `source_dataset_code`, `source_record_key`, `payload_format`, `payload_json`, `payload_text`, `payload_binary_ref`, `payload_hash`, `schema_claim`, `received_at`, `observed_at`, `provenance_json`, `retention_policy_code`, `confidentiality_level`, `is_deleted_at_source`, `created_at`.

#### `__raw_document` / `raw.document`

Immutable document envelope fields (Access names may use `__raw_document`; canonical SQL name is `raw.document`):

`document_id PK`, `batch_id FK`, `artifact_id FK`, `source_system`, `file_name`, `document_identity`, `mime_type`, `encoding`, `checksum_algorithm`, `checksum`, `received_at`, `source_created_at`, `source_modified_at`, `source_uri_or_path`, `ingestion_batch_id`, `provenance_json`, `retention_policy_code`, `confidentiality_level`, `original_payload_reference`, `storage_uri`, `byte_size`, `compression_code`, `encryption_code`, `immutable_flag`, `legal_hold_flag`, `created_at`.

`__raw_document` must not contain derived metric values as authoritative facts. It is the unchanged source-artifact envelope only.

### 6.3 Entity family (`entity.*`)

#### `entity.entity_record`

Generic master record for all non-statistical business entities: `entity_id PK`, `entity_type_code`, `namespace_code`, `source_system_code`, `source_record_key`, `canonical_key`, `structure_id FK nullable`, `record_type_code`, `payload_json`, `display_name`, `status`, `valid_from`, `valid_to`, `language_code`, `classification_level`, `provenance_record_id FK`, `record_hash`, `created_at`, `created_by`, `updated_at`, `updated_by`.

#### `entity.entity_link`

`entity_link_id PK`, `from_entity_id FK`, `to_entity_id FK`, `relation_code`, `relation_namespace`, `valid_from`, `valid_to`, `ordinal`, `confidence`, `source_record_id FK`, `is_primary`, `status`, `created_at`.

#### `entity.entity_classification`

`entity_classification_id PK`, `entity_id FK`, `classification_system_code`, `classification_version`, `classification_item_code`, `assignment_type`, `confidence`, `valid_from`, `valid_to`, `source_record_id FK`, `status`.

#### `entity.localized_text`

`localized_text_id PK`, `entity_id FK`, `field_code`, `language_code`, `text_value`, `is_preferred`, `valid_from`, `valid_to`.

#### `entity.resource_locator`

`resource_locator_id PK`, `entity_id FK`, `locator_type`, `uri`, `mime_type`, `checksum`, `title`, `access_policy_code`, `is_primary`, `valid_from`, `valid_to`.

### 6.4 Statistical family (`statistics.*`)

#### `statistics.series`

One declared statistical series: `series_id PK`, `structure_id FK`, `semantic_binding_id FK`, `series_code UNIQUE`, `carrier_code`, `dataset_code`, `indicator_code`, `measure_code`, `unit_code`, `frequency_code`, `time_granularity`, `geo_level_code`, `population_scope`, `classification_system_code`, `classification_version`, `methodology_version`, `aggregation_code`, `confidentiality_level`, `quality_status`, `valid_from`, `valid_to`, `source_record_id FK`, `created_at`, `updated_at`.

#### `statistics.observation`

Fact grain is one series + one period + one complete dimension key: `observation_id PK`, `series_id FK`, `period_start`, `period_end`, `period_code`, `value_numeric`, `value_text`, `value_boolean`, `value_date`, `unit_code`, `status_code`, `observation_status`, `quality_flag`, `suppression_flag`, `confidentiality_level`, `source_record_id FK`, `observation_hash`, `valid_from`, `valid_to`, `created_at`, `updated_at`.

Unique: `(series_id, period_code, dimension_key_hash)`.

#### `statistics.observation_dimension`

`observation_dimension_id PK`, `observation_id FK`, `dimension_code`, `dimension_value_code`, `dimension_value_text`, `dimension_value_entity_id FK nullable`, `classification_system_code`, `classification_version`, `ordinal`, `dimension_key_hash`.

#### `statistics.observation_attribute`

`observation_attribute_id PK`, `observation_id FK`, `attribute_code`, `attribute_value_json`, `unit_code`, `source_record_id FK`, `created_at`.

#### `statistics.revision`

`revision_id PK`, `series_id FK`, `revision_number`, `revision_reason`, `published_at`, `published_by`, `supersedes_revision_id FK nullable`, `revision_hash`, `status`.

Statistical semantics are never inferred from a raw JSON key alone. `metric`, `unit`, `aggregation`, population, quality and confidentiality must be approved in `contract_semantic_binding` before materialization.

### 6.5 Classification family (`classification.*`)

#### `classification.system`

`classification_system_id PK`, `system_code UNIQUE`, `name`, `publisher`, `version`, `language_code`, `valid_from`, `valid_to`, `status`, `definition_hash`.

#### `classification.item`

`classification_item_id PK`, `classification_system_id FK`, `item_code`, `item_label`, `definition`, `parent_item_id FK nullable`, `level_no`, `sort_order`, `valid_from`, `valid_to`, `status`.

#### `classification.mapping`

`mapping_id PK`, `from_system_id FK`, `from_item_id FK`, `to_system_id FK`, `to_item_id FK`, `mapping_type`, `confidence`, `valid_from`, `valid_to`, `methodology_ref`, `status`.

Data Plane read-only snapshots (when local processing requires them) are named `classification.system_snapshot`, `classification.item_snapshot`, and `classification.mapping_snapshot`; they contain the source registry identifiers plus `snapshot_id`, `source_registry_version`, `loaded_at`, and `snapshot_hash`. They never become a second authority and are refreshed from `geostat-system`.

### 6.6 Geo family (`geo.*`)

#### `geo.feature`

`feature_id PK`, `geo_type_code`, `feature_code`, `name`, `geometry`, `srid`, `level_code`, `parent_feature_id FK nullable`, `valid_from`, `valid_to`, `source_record_id FK`, `status`.

#### `geo.feature_link`

`feature_link_id PK`, `from_feature_id FK`, `to_feature_id FK`, `relation_code`, `valid_from`, `valid_to`, `source_record_id FK`, `confidence`.

### 6.7 Publication/serving family (`publication.*`, `serving.*`)

#### `publication.snapshot`

`snapshot_id PK`, `snapshot_code UNIQUE`, `structure_id FK`, `version`, `as_of_time`, `published_at`, `published_by`, `query_definition`, `row_count`, `checksum`, `status`, `retention_policy_code`.

#### `publication.dataset_snapshot`

`dataset_snapshot_id PK`, `snapshot_id FK`, `physical_family`, `physical_object_name`, `schema_hash`, `row_count`, `checksum`.

#### `publication.snapshot_member`

`snapshot_member_id PK`, `snapshot_id FK`, `source_record_id FK nullable`, `entity_id FK nullable`, `observation_id FK nullable`, `member_hash`.

#### `serving.metric_cache`

`cache_id PK`, `metric_code`, `series_id FK`, `dimension_key_hash`, `period_code`, `value_numeric`, `unit_code`, `aggregation_code`, `snapshot_id FK`, `quality_status`, `suppression_flag`, `expires_at`, `created_at`.

### 6.8 Audit, archive and system families

#### `audit.event`

`event_id PK`, `event_time`, `actor_type`, `actor_id`, `action_code`, `object_type`, `object_id`, `structure_id FK nullable`, `batch_id FK nullable`, `before_hash`, `after_hash`, `reason`, `ip_or_client_ref`, `result`, `metadata_json`.

#### `audit.access_decision`

`decision_id PK`, `principal_id`, `object_type`, `object_id`, `purpose_code`, `policy_code`, `decision`, `decided_at`, `expires_at`, `evidence_json`.

#### `archive.retention_register`

`retention_id PK`, `object_type`, `object_id`, `retention_policy_code`, `retain_until`, `legal_hold_flag`, `destruction_eligible_at`, `destruction_event_id FK nullable`, `status`.

#### `archive.payload_pointer`

`payload_pointer_id PK`, `source_record_id FK nullable`, `document_id FK nullable`, `storage_uri`, `storage_tier`, `checksum`, `encryption_code`, `byte_size`, `immutable_flag`, `created_at`.

#### `system.code`

`code_id PK`, `code_system`, `code`, `label`, `definition`, `parent_code_id FK nullable`, `language_code`, `valid_from`, `valid_to`, `status`.

#### `system.job_run`

`job_run_id PK`, `job_code`, `batch_id FK nullable`, `started_at`, `completed_at`, `worker_id`, `status`, `input_hash`, `output_hash`, `error_count`, `log_uri`.

## 7. Virtual KIDS contract — სრული ცხრილები და columns

ეს ცხრილები არის logical contract. ისინი შეიძლება არსებობდეს Access transport-ში ან generated view/materialization-ში; canonical SQL-ში mapping ქვემოთაა.

### 7.1 `KIDS_GOAL`

Columns: `goal_id`, `goal_code`, `goal_version`, `title`, `description`, `domain_code`, `owner_org_id`, `status`, `priority`, `target_date`, `parent_goal_id`, `source_reference`, `created_at`, `updated_at`, `valid_from`, `valid_to`.

Mapping: `entity.entity_record` with `entity_type_code='KIDS_GOAL'`; parent relation via `entity.entity_link` (`PARENT_OF`).

### 7.2 `KIDS_RESOURCE`

Columns: `resource_id`, `resource_code`, `resource_version`, `title`, `description`, `resource_type_code`, `uri`, `mime_type`, `language_code`, `publisher_org_id`, `license_code`, `access_level`, `checksum`, `size_bytes`, `created_at`, `updated_at`, `valid_from`, `valid_to`, `status`.

Mapping: main identity/content in `entity.entity_record`; locator in `entity.resource_locator`; provenance in `raw.source_record`/`raw.document`.

### 7.3 `KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT`

Columns: `assignment_id`, `resource_id`, `subcategory_code`, `classification_system_code`, `classification_version`, `assignment_type`, `confidence`, `valid_from`, `valid_to`, `source_reference`, `status`.

Mapping: `entity.entity_classification` plus `classification.item` and optional `classification.mapping`.

### 7.4 `KIDS_GLOSSARY_ENTRY`

Columns: `glossary_entry_id`, `term_code`, `preferred_term`, `alternative_terms`, `definition`, `scope_note`, `language_code`, `domain_code`, `source_reference`, `status`, `valid_from`, `valid_to`, `created_at`, `updated_at`.

Mapping: `entity.entity_record` (`entity_type_code='KIDS_GLOSSARY_ENTRY'`) and `entity.localized_text` for multilingual labels.

### 7.5 `KIDS_STATISTICAL_CARRIER`

Columns: `carrier_id`, `carrier_code`, `carrier_name`, `source_system_code`, `source_dataset_code`, `artifact_id`, `file_or_document_identity`, `mime_type`, `checksum`, `received_at`, `source_uri`, `ingestion_batch_id`, `provenance`, `retention_policy_code`, `confidentiality_level`, `raw_payload_reference`, `carrier_format`, `schema_claim`, `status`.

Mapping: envelope in `raw.document`; row/payload in `raw.source_record`; ingestion metadata in `ingest.*`. This table does not itself define a metric.

### 7.6 `KIDS_STATISTICAL_INPUT`

Columns: `input_id`, `carrier_id`, `series_code`, `indicator_code`, `measure_code`, `metric_code`, `unit_code`, `aggregation_code`, `frequency_code`, `period_code`, `period_start`, `period_end`, `geography_code`, `geography_level_code`, `dimension_json`, `value_numeric`, `value_text`, `status_code`, `quality_flag`, `suppression_flag`, `confidentiality_level`, `source_row_number`, `source_record_key`, `created_at`.

Mapping: approved rows become `statistics.series`, `statistics.observation`, and `statistics.observation_dimension`; unapproved rows remain staged/raw/quarantined.

### 7.7 `KIDS_STATISTICAL_SEMANTIC_BINDING`

Columns: `binding_id`, `carrier_id`, `input_id`, `metric_code`, `concept_scheme`, `concept_code`, `unit_code`, `aggregation_code`, `population_scope`, `frequency_code`, `dimension_definitions_json`, `quality_rule_code`, `privacy_rule_code`, `classification_system_code`, `classification_version`, `methodology_version`, `materialization_policy`, `approval_status`, `approved_by`, `approved_at`, `definition_hash`.

Mapping: `platform.contract_semantic_binding`; approved statistical output maps to `statistics.series` and facts.

## 8. Access transport package

Access generator MUST create a package manifest and a deterministic table schema from the approved contract. Required transport metadata objects:

`__gs_package_manifest`, `__gs_contract_namespace`, `__gs_contract_structure`, `__gs_contract_table_definition`, `__gs_contract_field_definition`, `__gs_contract_index_definition`, `__gs_contract_index_column`, `__gs_contract_relation`, `__gs_contract_semantic_binding`, `__gs_contract_classification_binding`, `__gs_contract_quality_rule`, `__gs_contract_privacy_rule`, `__gs_lineage_event`, `__gs_validation_issue`, `__gs_load_checkpoint`.

Every generated business/transport table has:

`__row_id PK`, `__contract_structure_id`, `__contract_version`, `__source_record_key`, `__source_row_number`, `__ingestion_batch_id`, `__payload_hash`, `__record_status`, `__valid_from`, `__valid_to`, `__created_at`, `__updated_at`.

Access must not invent columns, relations, metric meanings or classifications. If a declared field cannot be represented, generation fails closed and reports the contract error.

### 8.1 `__gs_*` Access object → canonical physical table

`__gs_*` სახელები არის Access package-ის transport names. მათი საბოლოო ჩანაწერი მიდის შემდეგ canonical Control Plane ცხრილებში:

| Access transport object | Canonical physical destination | შენიშვნა |
|---|---|---|
| `__gs_structure` | `platform.contract_structure` | ერთი contract/dataset version-ის root; ცალკე `gs_structure` SQL table არ იქმნება |
| `__gs_dataset` | `platform.contract_structure` | dataset არის structure-ის dataset identity (`dataset_code`, `dataset_version`); თუ package-ში ცალკე rows აქვს, ისინი იგივე `structure_id`-ზე იკვრება |
| `__gs_field` | `platform.contract_field_definition` | თითო field-ს აქვს `field_def_id`, `table_def_id` და namespace/prefix |
| `__gs_key` | `platform.contract_index_definition` + `platform.contract_index_column` | PK/UK/FK/business-key declaration; key-ის თითო column ცალკე index-column row-ა |
| `__gs_relation` | `platform.contract_structure_relation` | endpoint tables/fields, cardinality, delete/update policy და relation semantics |
| `__gs_index` | `platform.contract_index_definition` + `platform.contract_index_column` | search/unique/temporal/spatial/foreign-key indexes; `__gs_key` მხოლოდ constraint identity-ს აღწერს, `__gs_index` კი access plan-ს |
| `__gs_ui_surface` | `platform.contract_ui_surface` | UI metadata בלבד; business logic ან schema-ს წყარო არ არის |

`__gs_dataset`-ის ცალკე transport არსებობა დასაშვებია compatibility-სთვის, მაგრამ canonical physical დონეზე ის არ უნდა გამრავლდეს redundant dataset table-ად. მისი rows უნდა normalize-დეს `platform.contract_structure`-ში. საჭიროებისას dataset-ის domain-specific properties ინახება `platform.contract_structure`-ის typed columns-ში ან შესაბამის contract extension metadata-ში, არა დაუკონტროლებელ JSON-ში.

Transport-to-canonical loading order არის: `__gs_structure`/`__gs_dataset` → table definitions → fields → keys/indexes → relations → semantic/classification/quality/privacy bindings → UI surface. FK-ის გამო generator-მა უნდა შეინარჩუნოს ეს dependency order.

## 9. Virtual-to-physical mapping matrix

| Virtual object | Access transport | Canonical physical target |
|---|---|---|
| KIDS_GOAL | `__ent_kids_goal` | `entity.entity_record` + `entity.entity_link` |
| KIDS_RESOURCE | `__ent_kids_resource` | `entity.entity_record` + `entity.resource_locator` |
| KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT | `__rel_kids_resource_subcategory_assignment` | `entity.entity_classification` |
| KIDS_GLOSSARY_ENTRY | `__ent_kids_glossary_entry` | `entity.entity_record` + `entity.localized_text` |
| KIDS_STATISTICAL_CARRIER | `__raw_kids_statistical_carrier` | `raw.document` + `raw.source_record` + `ingest.*` |
| KIDS_STATISTICAL_INPUT | `__stat_kids_statistical_input` | `statistics.series` + `statistics.observation` + `statistics.observation_dimension` |
| KIDS_STATISTICAL_SEMANTIC_BINDING | `__stat_kids_statistical_semantic_binding` | `platform.contract_semantic_binding` |

This mapping is explicit, versioned and testable. A new KIDS dataset follows the same families; it does not require a new canonical SQL table.

## 10. Constraints, indexes and relations

Every physical table MUST have a primary key, immutable object/row identity, ingestion provenance, timestamps and status where applicable. Every foreign key is declared in the contract registry and materialized in SQL. Every logical table has a deterministic prefix and an index inventory.

Mandatory index classes:

* PK on every `*_id`;
* unique natural/business key where declared;
* FK indexes for every relation endpoint;
* `(structure_id, status, valid_from, valid_to)` on governed objects;
* `(batch_id, source_record_key)` on ingest/raw;
* `(series_id, period_code, dimension_key_hash)` on observations;
* classification `(system_id, item_code)`;
* temporal and geospatial indexes where the physical engine supports them;
* hash/checksum index for idempotency and duplicate detection.

No index may be silently omitted: if a generated engine cannot support one, generation must emit a blocking compatibility issue.

## 11. Lifecycle and quality gates

`DRAFT → REVIEW → APPROVED → PUBLISHED → DEPRECATED → RETIRED`.

Only `APPROVED` contract versions can generate a production Access package. Only `PUBLISHED` semantic bindings can materialize statistics. Raw ingestion is append-only. Corrections create a new revision; they do not overwrite the original payload or observation history.

Blocking checks: schema hash, duplicate keys, missing required fields, invalid domains, broken FK, invalid unit/aggregation, unapproved metric, invalid classification version, privacy threshold violation, checksum mismatch, unsupported physical type, and incomplete lineage.

## 12. Statistical semantic minimum

For each carrier/metric the approved contract must state: `metric_code`, human definition, numerator/denominator where relevant, `unit_code`, `aggregation_code`, population scope, frequency, time semantics, geography semantics, classification, missing-value policy, revision policy, quality thresholds, suppression/anonymisation rule, retention and confidentiality. A JSON blob is not a substitute for these typed fields.

## 13. New dataset execution algorithm

1. Register source system, namespace and owner.
2. Register immutable artifact and `raw.document` envelope.
3. Define virtual tables, fields, domains, indexes and relations.
4. Define semantic bindings, classifications, quality, privacy and lineage.
5. Validate hashes, names, prefixes, physical family and compatibility.
6. Submit for review; owner approves semantics and privacy.
7. Generate Access transport package from the approved schema.
8. Ingest to `ingest.*`, retain unchanged data in `raw.*`, quarantine failures.
9. Materialize entities/statistics/geo only through approved mappings.
10. Publish snapshot and serving products with lineage, quality and privacy evidence.

## 14. Final invariant

The panel is the contract authority; Access is a generated transport/execution boundary; canonical SQL is a stable generic Data Plane. Virtual KIDS tables are complete logical interfaces, while physical tables are extensible generic families. No site-specific hardcoded JSON, no hidden metric semantics, no unregistered relation, no missing prefix, no unindexed declared table, and no UI-generated business rule is permitted.

## 15. Execution record

The implementation runner now applies, in addition to the existing 001–033 history:

* `034_final_contract_governance_completion.sql` → `geostat-system`;
* `035_final_data_plane_completion.sql` → `geostat-data`;
* `036_final_archive_completion.sql` → `geostat-archive`.

These migrations are additive and idempotent. The live SQL Server verification completed with Control Plane migration ledger at **36**, and the new governance, Data Plane audit, and Archive retention/payload tables present. Application and test build completed successfully after the change.

The KIDS generator was also corrected so all domain transport tables carry their required family prefixes (`__ent_`, `__rel_`, `__raw_`, `__stat_`). A fresh package was generated from the legacy Access source and production audit returned **`PRODUCTION_ACCEPTED`**: 15 contract datasets, 104 fields, 21 enforced relationships, 36 goals, 225 resources, 230 subcategory assignments, 178 glossary entries, 43 carriers, and 880 statistical input cells.

Classifier evidence handling is implemented by `AccessClassifierProposalImportService`: `__cl_item`, `__cl_alias`, and `__cl_hierarchy` are imported as `DRAFT` proposals keyed by contract revision and package batch. No proposal is auto-promoted; authoritative classification changes remain an explicit governed review action.
# Current runtime status

For the deployed KIDS R8 counts, lifecycle state, publication snapshot and acceptance evidence, see [KIDS-R8-current-status-and-acceptance.md](KIDS-R8-current-status-and-acceptance.md). The execution record below is historical and must not be used as the current ledger.

## Control Plane Meta-Contract — სისტემის არქიტექტურული root

Control Plane-საც აქვს საკუთარი versioned schema და contract. **Platform Meta-Contract** არის კონტრაქტის კონტრაქტი; მისი საწყისი არის versioned SQL migrations, platform meta-schema და migration checksum ledger.

```text
Platform Meta-Contract
  → Control Plane Schema
  → Site/Data Product Contract
  → Dataset Contract
  → Physical Mapping
  → Canonical Data
  → Publication Snapshot
  → API/UI surface
```

Meta-Contract აცხადებს namespace, data product, contract/revision, dataset/field/key/index/relation, semantic/classifier binding, metric/unit/aggregation, quality/privacy/lineage policy, approval/publication lifecycle, migration, page mapping, alias, projection და API capabilities-ს. Lifecycle: `register → validate → version → review → approve → activate → publish → supersede → retire → rollback`.
