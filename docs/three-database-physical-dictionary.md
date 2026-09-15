# სამი რეალური ბაზის ფიზიკური Dictionary და ბიზნეს-ლოგიკა

**სტატუსი:** canonical physical architecture guide  
**ბაზები:** `geostat-system` (Control Plane), `geostat-data` (Data Plane), `geostat-archive` (Archive Plane).  
**მიზანი:** თითოეული schema/table/column-ის მარტივი განმარტება, მაგალითი და კავშირი.

## 1. საერთო სურათი

```text
geostat-system
  აკონტროლებს რას ნიშნავს და რა შეიძლება გამოქვეყნდეს
        ↓ contract/mapping/approval
geostat-data
  ინახავს მიღებულ და გამოქვეყნებულ კანონიკურ მონაცემს
        ↓ immutable pointer/checksum
geostat-archive
  ინახავს ორიგინალ ფაილს და custody/provenance-ს
```

`geostat-system` არის ავტორიტეტი კონტრაქტისთვის; `geostat-data` არის query/publication data; `geostat-archive` არის უცვლელი მტკიცებულება. Cross-database foreign key არ გამოიყენება — ბაზებს შორის კავშირი ხდება immutable numeric identity-ით, contract code-ით, snapshot id-ით და checksum-ით.

## 2. `geostat-system` — Control Plane

### 2.1 `platform.contract_namespace`

| Column | მნიშვნელობა | მაგალითი |
|---|---|---|
| `namespace_id` PK | namespace identity | `12` |
| `namespace_code` | უნიკალური namespace | `KIDS` |
| `prefix` | naming prefix | `__kids_` |
| `namespace_type` | SITE/FAMILY/SYSTEM | `SITE` |
| `parent_namespace_id` | ზედა namespace | `NULL` |
| `owner_org_id` | მფლობელი ორგანიზაცია | `1` |
| `status` | lifecycle | `APPROVED` |
| `valid_from/valid_to` | მოქმედების პერიოდი | `2026-01-01/NULL` |
| `definition_hash` | ცვლილების checksum | `sha256:...` |

**დანიშნულება:** სახელთა სივრცისა და prefix-ის წესის root. ყველა contract/table/field ამ namespace-ს ეკუთვნის.

### 2.2 `platform.contract_structure`

| Column | მნიშვნელობა | მაგალითი |
|---|---|---|
| `structure_id` PK | სტრუქტურის identity | `801` |
| `structure_code` | სტრუქტურის კოდი | `KIDS_RESOURCE_V8` |
| `namespace_id` FK | namespace | `12` |
| `dataset_code` | logical dataset | `KIDS_RESOURCE` |
| `dataset_version` | version | `8` |
| `display_name/description` | ადამიანური აღწერა | `Resources` |
| `contract_type` | ENTITY/STATISTICAL/RAW… | `ENTITY` |
| `canonical_physical_family` | Data Plane family | `ENTITY` |
| `schema_hash` | contract checksum | `sha256:...` |
| `status` | DRAFT/REVIEW/APPROVED/PUBLISHED/RETIRED | `APPROVED` |
| `owner_org_id/steward_org_id` | პასუხისმგებელი მხარეები | `1/2` |
| `quality_policy_code` | quality rule set | `KIDS_DEFAULT_QA` |
| `privacy_policy_code` | privacy rule set | `PUBLIC` |
| `retention_policy_code` | retention | `7Y` |
| `effective_from/effective_to` | version validity | `2026-01-01/NULL` |

**დანიშნულება:** ერთი dataset/contract version-ის root. API მხოლოდ APPROVED/PUBLISHED structure-ს ასრულებს.

### 2.3 Contract metadata tables

#### `platform.contract_table_definition`

`table_definition_id PK`, `structure_id FK`, `table_code`, `table_prefix`, `table_role` (`ROOT|CHILD|BRIDGE|CARRIER|DIMENSION|FACT|LOOKUP|VIEW`), `physical_family`, `physical_schema`, `physical_table`, `record_type_code`, `description`, `is_required`, `is_repeatable`, `is_temporal`, `is_statistical`, `is_raw`, `ordinal`, `status`, `definition_hash`.

**მაგალითი:** `table_code=KIDS_RESOURCE`, `physical_schema=entity`, `physical_table=entity_record`, `table_role=ROOT`.

#### `platform.contract_field_definition`

`field_def_id PK`, `table_def_id FK`, `field_code`, `field_prefix`, `ordinal`, `label`, `description`, `data_type`, `logical_type`, `physical_type`, `length`, `precision`, `scale`, `is_nullable`, `is_required`, `is_pk`, `is_natural_key`, `is_fk`, `is_business_key`, `is_measure`, `is_dimension`, `is_attribute`, `is_system_field`, `is_pii`, `is_confidential`, `classification_level`, `unit_code`, `value_domain_code`, `code_list_code`, `reference_structure_id`, `reference_table_code`, `reference_field_code`, `default_expression`, `validation_expression`, `format_pattern`, `min_value`, `max_value`, `allowed_values_json`, `timezone_policy`, `language_policy`, `sensitivity_policy`, `ordinal_path`, `status`, `definition_hash`.

**მაგალითი:** `field_code=resource_id`, `logical_type=CODE`, `is_natural_key=1`, `is_required=1`.

#### `platform.contract_index_definition` / `contract_index_column`

Index definition-ის columns: `index_def_id PK`, `table_def_id FK`, `index_code`, `index_prefix`, `index_type` (`PK|UK|FK|SEARCH|BRIN|GIST|HASH|CHECK`), `is_unique`, `is_clustered`, `is_required`, `predicate_expression`, `include_fields_json`, `ordinal`, `status`, `definition_hash`.

`contract_index_column`: `index_def_id FK`, `field_def_id FK`, `column_order`, `sort_direction`.

**მაგალითი:** `KIDS_RESOURCE__UK_RESOURCE_ID(resource_id ASC)` — duplicate resource identity-ს ბლოკავს და keyset pagination-ს ამყარებს.

#### `platform.contract_structure_relation`

`relation_id PK`, `structure_id FK`, `relation_code`, `relation_prefix`, `relation_type`, `from_table_def_id`, `to_table_def_id`, `from_field_def_id`, `to_field_def_id`, `bridge_table_def_id`, `cardinality_min`, `cardinality_max`, `on_delete`, `on_update`, `is_identifying`, `is_required`, `validity_rule`, `semantic_role`, `status`, `definition_hash`.

**მაგალითი:** `RESOURCE_TO_SUBCATEGORY`, `KIDS_RESOURCE.resource_id → KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT.resource_id`, `ONE_TO_MANY`.

### 2.4 Semantic და classification registry

| Table | ძირითადი columns | დანიშნულება/მაგალითი |
|---|---|---|
| `platform.dimension` | `dimension_id`, `dimension_code`, `classification_scheme_id`, `value_type`, `title_ka`, `title_en` | `AGE_GROUP`, `TIME_PERIOD` |
| `platform.measure` | `measure_id`, `measure_code`, `value_type`, `unit_code`, `decimal_precision`, `aggregation_default`, titles | `COUNT_EVENT`, `COUNT`, `SUM` |
| `platform.metric` | `metric_id`, `metric_code`, `source_dataset_id`, `measure_id`, `aggregation`, `allowed_dimension_set_json`, `status` | `KIDS_OBS_VALUE_INPUT` |
| `platform.metric_formula` | `formula_id`, `metric_id`, `expression_ast_json`, `version`, `approved_by`, `status` | typed formula, არა raw SQL |
| `platform.classification_scheme` | `scheme_id`, `scheme_code`, titles, `standard_reference`, owner | `AGE_GROUP` |
| `platform.classification_version` | `classification_version_id`, `scheme_id`, `version`, `status`, validity | `AGE_GROUP@2026` |
| `platform.classification_item` | `classification_item_id`, `classification_version_id`, `code`, labels, `sort_order`, status | `0-17` |
| `platform.classification_hierarchy` | `parent_item_id`, `child_item_id`, `hierarchy_type`, validity | parent/child category |
| `platform.classification_alias` | `alias_id`, `classification_item_id`, external system/code, validity | SDMX/external code mapping |

### 2.5 Ingestion, governance, serving metadata

| Table | ძირითადი columns | დანიშნულება |
|---|---|---|
| `platform.source_system` | `source_system_id`, `source_code`, `source_type`, `trust_level`, `enabled` | source-ის იდენტობა |
| `platform.source_connection` | `source_connection_id`, `source_system_id`, `connection_kind`, endpoint/database/user, `secret_reference`, `enabled` | connection metadata; secret value აქ არ ინახება |
| `platform.ingestion_contract` | `contract_id`, source/dataset ids, `format_profile`, `ingestion_method`, `auto_publish`, `status` | source როგორ უნდა შემოვიდეს |
| `platform.contract_revision_source` | source binding id, revision id, locator, target version, mapping JSON, approval state | Access/JSON locator-ის binding |
| `platform.contract_quality_rule` | rule id, structure/version, rule code/type, severity, expression, blocking | completeness, duplicate, range |
| `platform.contract_privacy_rule` | policy/rule, classification level, masking/threshold JSON | public/privacy gate |
| `platform.contract_lineage_rule` | rule code, source/target object, required evidence | provenance policy |
| `platform.contract_approval` | approval id, structure/revision, actor, decision, evidence, timestamp | approve/reject audit |
| `platform.contract_migration` | from/to revision, forward/rollback JSON, breaking flag, hash, status | safe schema evolution |
| `platform.contract_query_alias` | alias id, revision, dataset, alias code, canonical role, family, type, status | `metricCode`, `dimension.*` და family-wide aliases |
| `platform.contract_page_binding` | page binding, node/revision, runtime page id, dataset, projection, status | `pageId → dataset` |
| `platform.api_projection` | projection code, dataset, mapping JSON, approval/revision | public response shape |
| `platform.api_operation` | operation id/type/status, request, result URI/checksum, progress, errors, timestamps | async export/query |
| `platform.schema_migration` | migration id, checksum, applied_at/by | Control Plane schema ledger |

## 3. `geostat-data` — Data Plane

### 3.1 `ingest.*`

| Table | Columns | მაგალითი/ლოგიკა |
|---|---|---|
| `ingest.batch` | `batch_id PK`, `contract_id`, `product_id`, `source_system_id`, `status`, start/finish, requester, checksum | ერთი upload/import transaction |
| `ingest.artifact` | `artifact_id PK`, `batch_id FK`, original name, format, object URI, quarantine URI, checksum, byte size, received time | Access file `kids-r8.accdb` |
| `ingest.dataset_load` | `dataset_load_id PK`, `batch_id FK`, dataset version, source name, source/accepted/rejected counts, status | ერთი source table-ის load |
| `ingest.staged_row` | `staged_row_id PK`, load id, source row number/key, raw JSON, payload hash, validation status/error | row `source_row_number=44` |
| `ingest.validation_issue` | `issue_id`, staged row, rule code, severity, message, quarantined flag | missing natural key |
| `ingest.load_checkpoint` | checkpoint id, load id, last row/key, status, worker, timestamp | resumable load |

### 3.2 `raw.*`

| Table | Columns | დანიშნულება |
|---|---|---|
| `raw.document` | `document_id`, source system, file identity/name, MIME, checksum, received_at, source URI, batch id, provenance, retention, confidentiality, payload reference | უცვლელი artifact envelope |
| `raw.source_record` | `source_record_id PK`, dataset snapshot, source row/key, full `payload_json`, payload hash, artifact id, extracted/valid times | source row-ის უცვლელი ასლი |

### 3.3 `entity.*`

| Table | Columns | მაგალითი/კავშირი |
|---|---|---|
| `entity.entity_record` | `entity_id PK`, snapshot, external/canonical key, entity/record type, title, payload JSON/hash, source record, validity, `is_current` | resource/goal/glossary record |
| `entity.entity_link` | `link_id PK`, from/to entity ids, relationship type, ordinal, validity, source record, status | goal → resource |
| `entity.entity_classification` | entity id, attribute id, classification item id, validity, source record, composite PK | resource → subcategory |
| `entity.localized_text` | entity id, field code, language tag, text value | `title.ka`, `title.en` |
| `entity.resource_locator` | entity id, locator kind, language, locator | file URL/download URI |

### 3.4 `statistics.*`

| Table | Columns | დანიშნულება/მაგალითი |
|---|---|---|
| `statistics.series` | `series_id PK`, snapshot, metric id, series key hash, unit code, status, source record | ერთი dimensional series |
| `statistics.observation` | `observation_id PK`, series id, period start/end, status, numeric/text/boolean value, source record, validity, current flag | `2024`, value `1532` |
| `statistics.observation_dimension` | observation id, dimension id, classifier item/scalar code, composite PK | `AGE_GROUP=0-17` |
| `statistics.observation_attribute` | observation id, attribute code, value JSON, composite PK | confidentiality/status attribute |
| `statistics.revision` | revision id, observation/series, reason, previous/current value, actor, timestamp | corrected observation history |

**ბიზნეს-ლოგიკა:** series განსაზღვრავს grain-ს; observation ინახავს measure/value-ს; dimension აკავშირებს classifier/time/region-ს; raw source record ინარჩუნებს traceability-ს.

### 3.5 `classification.*`, `geo.*`, `publication.*`, `serving.*`

| Table | Columns | დანიშნულება |
|---|---|---|
| `classification.system_snapshot` | snapshot id, source registry/version, loaded_at, hash | Control Plane mirror identity |
| `classification.item_snapshot` | snapshot, item id, scheme version, code, labels, parent, status | published classifier mirror |
| `classification.mapping_snapshot` | snapshot, external system/code, item id, validity | external mapping mirror |
| `geo.feature` | feature id, snapshot, feature key, GeoJSON, geometry type, bbox, source record, current flag | OGC feature |
| `geo.feature_link` | feature id, entity/classifier id, relationship type, composite key | feature → municipality/category |
| `publication.snapshot` | snapshot id, product/release, status, published_at, previous snapshot | atomic public release |
| `publication.dataset_snapshot` | dataset snapshot id, load/version, status, row count, checksum | dataset member |
| `publication.snapshot_member` | snapshot + dataset version, dataset snapshot, count/checksum, composite PK | release composition |
| `serving.metric_cache` | cache id, snapshot, metric, dimension signature, period, aggregate, refreshed_at | performance-only cache |

## 4. `geostat-archive` — Archive Plane

| Table | Columns | დანიშნულება/მაგალითი |
|---|---|---|
| `archive.artifact` | `artifact_id PK`, source/batch, object URI, checksum, size, MIME, received time, retention/confidentiality | original Access object |
| `archive.payload_pointer` | pointer id, artifact id, storage provider/bucket/key, URI, checksum, encryption, retention, availability | MinIO/S3 pointer |
| `archive.retention_register` | artifact/payload id, retention policy, legal hold, purge_after, state, decision evidence | purge protection |
| `archive.integrity_event` | event id, artifact/pointer, operation, checksum, actor, timestamp, result | custody/integrity audit |

Archive-ში ინახება payload და მისი მტკიცებულება; query/API არ კითხულობს archive-ს პირდაპირ. Data Plane-ში ინახება მხოლოდ `artifact_id`, `source_record_id`, checksum და pointer reference.

## 5. მთავარი კავშირების ნახაზი

```text
platform.contract_structure
  ├─ contract_table_definition → contract_field_definition
  ├─ contract_index_definition → contract_index_column → field
  ├─ contract_structure_relation → from/to dataset + keys
  ├─ contract_quality/privacy/lineage_rule
  └─ contract_page_binding → pageId → dataset/projection

ingest.batch
  ├─ artifact → archive.artifact → archive.payload_pointer
  └─ dataset_load → staged_row → raw.source_record

raw.source_record
  ├─ entity.entity_record → entity links/classifications
  ├─ statistics.series → observation → observation_dimension
  └─ geo.feature

publication.snapshot
  ├─ dataset_snapshot → entity/statistics/geo rows
  ├─ classification mirrors
  └─ serving.metric_cache
```

## 6. სრული ბიზნეს-პროცესი

```text
1. Register source/namespace/product/contract.
2. Declare datasets, fields, keys, indexes, relations and semantics.
3. Approve quality/privacy/lineage policies.
4. Receive Access/JSON/CSV/SQL artifact.
5. Create batch/artifact/dataset_load.
6. Stage and validate rows; quarantine failures.
7. Preserve raw document/source records.
8. Materialize entity/statistics/classification/geo families.
9. Reconcile counts, checksums and relations.
10. Prepare immutable publication snapshot.
11. Pass quality/privacy/steward gates.
12. Publish snapshot and build serving cache.
13. API resolves page → dataset → contract projection → response.
14. Rollback by switching publication pointer, never by mutating history.
```

## 7. გაფართოების წესი

ახალი site/dataset/field/classifier/relation/chart ახალი physical business table-ის გარეშე ემატება: ახალი contract revision + metadata + mapping + data rows. ახალი physical table დასაშვებია მხოლოდ ახალი data family-ის architecture review-ის შემდეგ. ყველა ცვლილება არის additive, versioned, checksum-recorded და rollback-plan-ით.

დამატებითი ნორმატიული წყარო: [Final Physical Database Architecture](final-physical-database-architecture.md) და [Final Unified Physical/Virtual Contract](final-unified-physical-virtual-contract.md).

## 8. ბიზნეს-კავშირები — მარტივი ახსნა

### 8.1 ერთი წინადადებით თითოეული მთავარი ობიექტი

| ობიექტი | მარტივი მნიშვნელობა |
|---|---|
| Source System | საიდან მოვიდა მონაცემი |
| Data Product | ვისთვის/რისთვის არის მონაცემთა პროდუქტი |
| Contract | რა მონაცემი არსებობს და რა წესით უნდა დამუშავდეს |
| Dataset | ერთი ლოგიკური მონაცემთა კოლექცია |
| Field | dataset-ის ერთი თვისება |
| Key | როგორ ვიცნობთ ერთ ჩანაწერს უნიკალურად |
| Index | როგორ ვპოულობთ მას სწრაფად |
| Relation | როგორ უკავშირდება ერთი ჩანაწერი მეორეს |
| Artifact | შემოსული ფაილი ან payload |
| Batch | ერთი მიღების/დამუშავების ოპერაცია |
| Raw Record | ზუსტად ისეთი source row, როგორიც შემოვიდა |
| Entity | წაკითხვადი ბიზნეს-ობიექტი |
| Series | ერთი სტატისტიკური grain-ის განმარტება |
| Observation | კონკრეტულ პერიოდზე მიღებული რიცხვი/მნიშვნელობა |
| Dimension | observation-ის განმასხვავებელი კატეგორია |
| Classifier Item | ოფიციალური კოდი/კატეგორია |
| Snapshot | ერთ მომენტში დამტკიცებული სრული ვერსია |
| Cache | snapshot-ის სწრაფი ასლი, არა წყარო |
| Archive Pointer | სად ინახება ორიგინალი ფაილი და როგორ ვამოწმებთ მის უცვლელობას |

### 8.2 კავშირების სრული მატრიცა

| From | To | კავშირის აზრი | როდის იქმნება |
|---|---|---|---|
| `source_system` | `ingestion_contract` | ამ წყაროს მიღების წესები | source onboarding-ზე |
| `ingestion_contract` | `contract_structure` | source რომელი კონტრაქტით მუშავდება | contract approval-ზე |
| `contract_structure` | `contract_table_definition` | dataset-ის რომელი physical family გამოიყენება | mapping approval-ზე |
| `contract_table_definition` | `contract_field_definition` | ცხრილში რომელი field-ებია დაშვებული | schema declaration-ზე |
| `field_definition` | `index_definition` | რომელი field ქმნის PK/UK/search/index-ს | key/index approval-ზე |
| `contract_structure_relation` | dataset/table/field | რომელი endpoint-ები და cardinality აკავშირებს | relation approval-ზე |
| `contract_structure` | `quality/privacy/lineage_rule` | ამ მონაცემს რა gates აქვს | governance setup-ზე |
| `contract_page_binding` | contract/dataset/projection | `pageId` რომელი მონაცემის public entry point-ია | serving contract-ზე |
| `batch` | `artifact` | ერთი მიღების ოპერაცია რომელი ფაილით შესრულდა | upload-ზე |
| `artifact` | `archive.artifact` | ორიგინალი ფაილის immutable ასლი | archive ingest-ზე |
| `batch` | `dataset_load` | ერთი batch-ის რომელი dataset იტვირთება | multi-table ingest-ზე |
| `dataset_load` | `staged_row` | source row რომელ load-ს ეკუთვნის | staging-ზე |
| `staged_row` | `validation_issue` | რატომ ვერ გაიარა row-მა validation | validation failure-ზე |
| `staged_row` | `raw.source_record` | valid source row-ის უცვლელი შენახვა | raw materialization-ზე |
| `raw.source_record` | `entity.entity_record` | source row-ის ბიზნეს-ობიექტად გარდაქმნა | entity materialization-ზე |
| `raw.source_record` | `statistics.series/observation` | source row-ის სტატისტიკურ მნიშვნელობად გარდაქმნა | statistical materialization-ზე |
| `raw.source_record` | `geo.feature` | source row-ის გეო-ობიექტად გარდაქმნა | geo materialization-ზე |
| `entity.entity_record` | `entity.entity_link` | ორი ბიზნეს-ობიექტის კავშირი | relation materialization-ზე |
| `entity.entity_record` | `entity.entity_classification` | ობიექტის classifier კატეგორიასთან მიბმა | classification resolution-ზე |
| `statistics.series` | `statistics.observation` | series-ის კონკრეტული პერიოდული მნიშვნელობები | statistical load-ზე |
| `observation` | `observation_dimension` | observation რომელი dimension code-ებით განისაზღვრება | cube materialization-ზე |
| `observation_dimension` | `classification.item_snapshot` | dimension value-ის ოფიციალური label/code | publication snapshot-ზე |
| `classification registry` | `classification.*_snapshot` | authoritative classifier-ის read-only mirror | publish-ზე |
| `dataset_snapshot` | `publication.snapshot` | dataset რომელი public release-ის წევრია | publication-ზე |
| `publication.snapshot` | `serving.metric_cache` | გამოქვეყნებული მონაცემის სწრაფი aggregate | cache build-ზე |
| `publication.snapshot` | API response | რომელი immutable version უნდა დაბრუნდეს | API read-ზე |

### 8.3 KIDS-ის რეალური მაგალითი

```text
Access __ent_kids_resource
  → ingest.artifact / ingest.dataset_load
  → ingest.staged_row
  → raw.source_record
  → entity.entity_record (resource)
  → entity.entity_classification (subcategory)
  → entity.entity_link (goal/resource relation)
  → publication.dataset_snapshot
  → pageId=9 API response
```

სტატისტიკური მაგალითი:

```text
Access __stat_kids_statistical_input
  → raw.source_record
  → statistics.series (metric + grain)
  → statistics.observation (period + value)
  → statistics.observation_dimension (AGE_GROUP=0-17)
  → classification.item_snapshot
  → publication.snapshot
  → pageId=11 API response
```

### 8.4 რატომ არ ვაკავშირებთ ყველაფერს პირდაპირ ყველაფერთან

- Control Plane აღწერს წესს, Data Plane ინახავს მონაცემს, Archive ინახავს მტკიცებულებას.
- Raw record ყოველთვის ინარჩუნებს source truth-ს და არ იცვლება derived entity/statistical მნიშვნელობით.
- Classification-ის owner არის Control Plane; Data Plane-ში მხოლოდ იმავე publication version-ის mirror გამოიყენება.
- Cache არასოდეს არის source of truth; მისი წაშლა მონაცემს არ აქრობს.
- Relation იწერება მხოლოდ მაშინ, როცა ორივე endpoint და key კონტრაქტშია გამოცხადებული.
- API აბრუნებს მხოლოდ published snapshot-ს, ამიტომ ერთ response-ში გაერთიანებული ყველა family ერთი ვერსიისაა.

### 8.5 მონაცემის ცხოვრების ციკლი ბიზნეს ენაზე

```text
ვიღებთ ფაილს
  → ვიგებთ ვინ გამოგზავნა და რომელი კონტრაქტით უნდა წავიკითხოთ
  → ვინახავთ ორიგინალს უცვლელად
  → ვამოწმებთ ყველა row-სა და relation-ს
  → ვყოფთ raw/entity/statistics/classification/geo მნიშვნელობებად
  → ვამოწმებთ ხარისხს, კონფიდენციალურობას და რაოდენობებს
  → ვქმნით immutable snapshot-ს
  → ვაქვეყნებთ
  → API აბრუნებს მხოლოდ ამ snapshot-ის კანონიკურ projection-ს
```

### 8.6 როგორ უნდა იფიქრო ახალ dataset-ზე

ახალი dataset-ის დამატებისას ყოველთვის უპასუხე ამ 10 კითხვას:

1. ვინ არის source owner და რა არის source identity?
2. რა არის dataset-ის business meaning და grain?
3. როგორ განვასხვავებთ ერთ ჩანაწერს (natural/business key)?
4. რომელი fields არის required, dimension, measure ან attribute?
5. რომელ classifier/version-ს იყენებს?
6. რომელ dataset-ებთან აქვს relation და რა cardinality?
7. რომელი raw evidence ინარჩუნებს provenance-ს?
8. რა quality/privacy/retention წესები აქვს?
9. რომელი publication snapshot-ით გახდება public?
10. რომელი page/projection/API response უნდა წარმოიქმნას?

თუ ამ კითხვებზე პასუხი კონტრაქტშია, ახალი dataset უკვე სისტემის ნაწილია; თუ პასუხი მხოლოდ Java-ში, Access-ში ან UI-შია, კონტრაქტი არასრულია.

## 9. საბოლოო მიზანი

საბოლოო მიზანია არა მხოლოდ სამი ბაზის ან KIDS-ის შენახვა, არამედ თვითაღწერადი, contract-driven data platform, სადაც:

```text
ნებისმიერი approved source
  → ერთიანი კონტრაქტით
  → ერთიანი validation/materialization წესით
  → ერთიანი relation/publication მოდელით
  → ერთიანი API/export/cache მექანიზმით
  → audit-ირებადი და rollback-ადი შედეგით
```

KIDS არის პირველი reference implementation. პლატფორმის ხარისხი იზომება იმით, რამდენად მარტივად ერთვება მეორე საიტი იგივე Control Plane/Data Plane/Archive Plane მექანიზმებით, ახალი ბიზნეს-კოდის და ახალი ფიზიკური business table-ების გარეშე.
