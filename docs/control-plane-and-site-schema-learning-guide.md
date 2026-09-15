# Control Plane და Site Schema — სრული სასწავლო და implementation guide

**მიზანი:** პროგრამისტმა/არქიტექტორმა შეძლოს მთელი სისტემის ლოგიკური ჯაჭვის გონებაში გავლა: რა იქმნება პირველად, რას აღწერს Control Plane, რა არის თითოეული site-ის აუცილებელი/დამატებითი/არასავალდებულო ნაწილი და როგორ უკავშირდება ყველაფერი ერთმანეთს.

**მთავარი გადაწყვეტილება:** ახალი site-ისთვის არ იქმნება ახალი database ან ახალი business table ავტომატურად. Control Plane ქმნის versioned contract-სა და metadata-ს; Data Plane იყენებს generic family schemas-ს. ახალი physical schema საჭიროა მხოლოდ ახალი data family-ის architecture review-ის შემდეგ.

## 1. ყველაზე მოკლე მოდელი

```text
Platform Meta-Contract
  ↓ აღწერს საკუთარ Control Plane-ს
Control Plane Schema
  ↓ ქმნის/ამტკიცებს site contract-ს
Site Contract
  ↓ აღწერს dataset/field/key/index/relation/semantics-ს
Generic Data Plane Family Schema
  ↓ ინახავს კონკრეტულ rows-ს
Publication Snapshot
  ↓
API / export / cache / UI metadata
```

### სამი კატეგორია

| კატეგორია | რას ნიშნავს | მაგალითი |
|---|---|---|
| აუცილებელი | ამის გარეშე site ვერ დარეგისტრირდება ან ვერ გამოქვეყნდება | namespace, product, contract, dataset, field, key, approval |
| დამატებითი | საჭიროა კონკრეტული შესაძლებლობისთვის | classifier, metric, geo, visualization, async export |
| არასავალდებულო | მხოლოდ თუ ამ site-ს რეალურად სჭირდება | multilingual labels, formulas, spatial links, derived cache |

„არასავალდებულო“ არ ნიშნავს დაუკონტროლებელს: თუ capability ჩაირთო, მისი კონტრაქტი, owner, validation და lifecycle სავალდებულო ხდება.

## 2. პირველი კითხვა: რას ვაშენებთ?

**პასუხი:** ვაშენებთ არა KIDS-ის ცალკე ბაზას, არამედ მრავალსაიტიან Data Product Platform-ს. KIDS არის პირველი site profile.

```text
ერთი Control Plane
ერთი shared Data Plane
ერთი Archive Plane
ბევრი site/data product contract
```

## 3. მეორე კითხვა: Control Plane-ს საკუთარი schema სჭირდება?

**დიახ.** მის schema-ს ეწოდება Platform Meta-Contract. იგი versioned migration-ებით იქმნება და საკუთარ თავში აღწერს:

- namespace-ს და prefix-ს;
- contract structure-ს;
- table/field/key/index/relation definition-ს;
- semantic/classification binding-ს;
- quality/privacy/lineage policy-ს;
- approval/publication lifecycle-ს;
- page/API projection/alias-ს;
- migration/checksum/audit-ს.

Control Plane-ის root არის:

```text
versioned migration
  + platform meta-schema
  + schema_migration checksum ledger
  + standards profile
```

## 4. Control Plane-ის აუცილებელი schemas/tables

ყველა ქვემოთ ჩამოთვლილი არის `geostat-system`-ში და მათი prefix/identity არის კანონიკური.

### 4.1 Identity და ownership — ყოველთვის აუცილებელი

#### `platform.contract_namespace`

**როლი:** site-ის სახელთა სივრცე და prefix policy.

ძირითადი ველები:

```text
namespace_id PK
namespace_code UNIQUE
prefix UNIQUE
namespace_type
parent_namespace_id FK NULL
owner_org_id
status
valid_from, valid_to
definition_hash
created_at, created_by, updated_at, updated_by
```

მაგალითი: `KIDS`, prefix `__kids_`, type `SITE`.

#### `platform.data_product`

**როლი:** site-ის ან პროდუქტის ბიზნეს-identity.

```text
product_id PK
product_code UNIQUE
namespace_id FK
title_ka, title_en
owner_user_id / owner_org_id
lifecycle_status
sensitivity
created_at, updated_at
```

#### `platform.contract_structure`

**როლი:** versioned site/data-product contract-ის root.

```text
structure_id PK
structure_code UNIQUE
namespace_id FK
product_id FK
dataset_code
dataset_version
contract_type
schema_standard
canonical_physical_family
schema_hash
status
owner_org_id, steward_org_id
quality_policy_code
privacy_policy_code
retention_policy_code
lineage_policy_code
effective_from, effective_to
definition_hash
```

### 4.2 Dataset structure — აუცილებელი ყოველი dataset-ისთვის

#### `platform.contract_table_definition`

**როლი:** logical dataset/table-ის mapping generic physical family-ზე.

```text
table_definition_id PK
structure_id FK
table_code
table_prefix
table_role (ROOT/CHILD/BRIDGE/CARRIER/DIMENSION/FACT/LOOKUP/VIEW)
physical_family
physical_schema
physical_table
record_type_code
is_required
is_repeatable
is_temporal
is_statistical
is_raw
ordinal
status
definition_hash
```

#### `platform.contract_field_definition`

**როლი:** field-ის სრული semantic და physical აღწერა.

```text
field_def_id PK
table_def_id FK
field_code
field_prefix
ordinal
label / description
data_type, logical_type, physical_type
length, precision, scale
is_nullable, is_required
is_pk, is_natural_key, is_fk, is_business_key
is_measure, is_dimension, is_attribute, is_system_field
is_pii, is_confidential, classification_level
unit_code
value_domain_code, code_list_code
reference_structure_id, reference_table_code, reference_field_code
validation_expression, format_pattern
min_value, max_value, allowed_values_json
timezone_policy, language_policy, sensitivity_policy
status, definition_hash
```

### 4.3 Keys და indexes — აუცილებელი

#### `platform.contract_index_definition`

```text
index_def_id PK
table_def_id FK
index_code
index_prefix
index_type (PK/UK/FK/SEARCH/TEMPORAL/SPATIAL/CHECK)
is_unique, is_clustered, is_required
predicate_expression
include_fields_json
ordinal
status, definition_hash
```

#### `platform.contract_index_column`

```text
index_def_id FK
field_def_id FK
column_order
sort_direction
```

ყოველ dataset-ს უნდა ჰქონდეს მინიმუმ ერთი stable identity: PK ან approved natural/business key. Stable keyset pagination-ს სჭირდება შესაბამისი ordered index.

### 4.4 Relations — აუცილებელი, თუ dataset სხვას უკავშირდება

#### `platform.contract_structure_relation`

```text
relation_id PK
structure_id FK
relation_code
relation_prefix
relation_type
from_table_def_id FK
to_table_def_id FK
from_field_def_id FK
to_field_def_id FK
bridge_table_def_id FK NULL
cardinality_min, cardinality_max
on_delete, on_update
is_identifying, is_required
validity_rule
semantic_role
status, definition_hash
```

დაშვებული ტიპები:

```text
ONE_TO_ONE
ONE_TO_MANY
MANY_TO_ONE
MANY_TO_MANY
TEMPORAL
HIERARCHICAL
DERIVED
```

## 5. Site-ის მინიმალური schema profile

ყველა site-ს სჭირდება შემდეგი მინიმალური contract graph:

```text
namespace
  → data product
    → contract revision
      → at least one dataset
        → fields
          → stable key/index
            → approval
```

### მინიმალური site-ის mandatory checklist

```text
[M] namespace + owner
[M] product + lifecycle
[M] contract revision + checksum
[M] dataset identity + family + grain
[M] every field type/role/required flag
[M] PK/natural/business key
[M] required indexes
[M] source binding + mapping
[M] quality policy
[M] privacy classification
[M] approval decision
[M] publication eligibility
```

თუ relation არსებობს, relation definition და ორივე endpoint key ასევე mandatory ხდება.

## 6. Site-ის დამატებითი schema profiles

### 6.1 Classification profile

საჭიროა, თუ dataset-ში კოდები/კატეგორიები/იერარქიაა:

```text
platform.classification_scheme
platform.classification_version
platform.classification_item
platform.classification_hierarchy
platform.classification_alias
```

მხარდაჭერა: version, multilingual label, hierarchy, validity, external mapping, deprecated code.

### 6.2 Statistical profile

საჭიროა, თუ site აქვეყნებს სტატისტიკას:

```text
platform.dimension
platform.measure
platform.metric
platform.metric_formula (თუ formula არსებობს)
```

Data Plane-ში გამოიყენება:

```text
statistics.series
statistics.observation
statistics.observation_dimension
statistics.observation_attribute
statistics.revision (თუ revision history საჭიროა)
```

### 6.3 Raw/provenance profile

ყველა ingest site-ს პრაქტიკულად სჭირდება:

```text
raw.document
raw.source_record
archive.artifact
archive.payload_pointer
```

Raw არის source truth; derived entity/statistics არასოდეს ცვლის მას.

### 6.4 Geo profile

საჭიროა გეომონაცემისთვის:

```text
geo.feature
geo.feature_link
```

კონტრაქტში უნდა გამოცხადდეს geometry type, CRS, bbox, validity და spatial index policy.

### 6.5 Visualization profile

საჭიროა chart/table/export surface-ისთვის:

```text
platform.visualization_definition
platform.visualization_version
platform.api_projection
platform.contract_page_binding
```

Chart არ ინახავს source values-ს. იგი metric/dimension/filter metadata-ს იყენებს.

### 6.6 Async/export profile

საჭიროა დიდი query/export-ისთვის:

```text
platform.api_operation
```

მას აქვს status, progress, request, result URI, checksum, error და cancellation lifecycle.

## 7. არასავალდებულო, მაგრამ გაფართოებადი შესაძლებლობები

| Capability | მხოლოდ როდის იქმნება |
|---|---|
| multilingual metadata | თუ ერთზე მეტი ენაა საჭირო |
| metric formula | თუ metric პირდაპირი value არ არის |
| geo feature link | თუ spatial relation არსებობს |
| serving cache | თუ latency/cache policy ამას მოითხოვს |
| derived projection | თუ public response storage-სგან განსხვავდება |
| temporal validity | თუ მონაცემს effective period აქვს |
| legal hold | თუ archive retention ამას მოითხოვს |
| event/outbox | თუ external consumers-ს push notification სჭირდება |
| tenant isolation | თუ მრავალორგანიზაციული tenancy ჩართულია |

## 8. შექმნის სწორი რიგი

```text
1. Platform migrations ქმნის Control Plane Meta-Schema-ს.
2. იქმნება namespace და owner.
3. იქმნება data product.
4. იქმნება contract revision და checksum.
5. რეგისტრირდება source system/connection/secret reference.
6. იქმნება dataset და მისი business grain.
7. იქმნება table definition და physical family mapping.
8. იქმნება fields და semantic roles.
9. იქმნება PK/natural/business keys.
10. იქმნება indexes, მათ შორის stable sort index.
11. იქმნება relations და bridge definitions.
12. საჭიროებისამებრ იქმნება classifier/dimension/metric/geo profiles.
13. იქმნება quality/privacy/lineage policies.
14. იქმნება page binding და API projection.
15. contract გადის review/approval-ს.
16. source შედის ingest/staging-ში.
17. raw preservation და validation სრულდება.
18. ხდება semantic materialization.
19. იქმნება publication snapshot.
20. snapshot გადის quality/privacy/reconciliation gates-ს.
21. publish და serving cache.
```

## 9. პროგრამისტის სწორი კითხვები და პასუხები

### კითხვა: ახალი site-ისთვის ახალი database შევქმნა?

**პასუხი:** არა. გამოიყენე არსებული Control/Data/Archive Plane. ახალი database მხოლოდ isolation/scale/legal მოთხოვნისა და architecture approval-ის შემთხვევაში.

### კითხვა: ახალი dataset-ისთვის ახალი SQL business table შევქმნა?

**პასუხი:** არა, თუ არსებული `ENTITY`, `STATISTICAL`, `RAW`, `CLASSIFICATION` ან `GEO` family ფარავს მას. დაამატე contract metadata და mapping.

### კითხვა: სად ვწერ source-ის ნამდვილ row-ს?

**პასუხი:** `raw.source_record`-ში, artifact/document provenance-ით. Derived object სხვა family-ში materialize-დება.

### კითხვა: სად ვწერ metric/unit/aggregation-ს?

**პასუხი:** Control Plane-ის `dimension/measure/metric` registry-ში; observation-ში მხოლოდ value და period ინახება.

### კითხვა: სად ვწერ relation-ს?

**პასუხი:** relation metadata — `platform.contract_structure_relation`; რეალური links — შესაბამის `entity_link`, bridge ან relation family-ში.

### კითხვა: როდის არის field საჯარო API-ში?

**პასუხი:** მხოლოდ მაშინ, როცა field contract-შია, projection-შია დაშვებული და publication/privacy gate-ები გავლილია.

### კითხვა: როდის შეიძლება ახალი physical schema?

**პასუხი:** მხოლოდ მაშინ, როცა არსებული family-ები ვერ გამოხატავს ახალ data family-ს და architecture review ამტკიცებს ახალ provider-ს.

### კითხვა: როგორ ავიცილო duplicate/გატეხილი relation?

**პასუხი:** stable key + unique index + relation endpoint validation + reconciliation test.

### კითხვა: როგორ ვაკეთებ ცვლილებას production-ში?

**პასუხი:** ახალი immutable contract revision, compatibility diff, migration plan, approval, snapshot rollout; ძველი revision არ იცვლება ადგილზე.

### კითხვა: რა არის API-ის საწყისი?

**პასუხი:** `pageId → approved contract revision → dataset → projection → published snapshot`.

## 10. საბოლოო mental model

```text
რა არის?           contract_structure
რისი ნაწილია?      namespace / data_product
სად ინახება?       table_definition / physical_family
რა თვისებები აქვს? field_definition
როგორ ვიცნობთ?     key + index
ვის უკავშირდება?   relation
რას ნიშნავს?       semantic / classifier / metric
შეიძლება გამოქვეყნება? quality + privacy + approval
რომელ ვერსიაშია?   snapshot / release
როგორ ვკითხულობთ? page binding + projection + API
საიდან დავამტკიცოთ? raw artifact + lineage + checksum
```

თუ ეს კითხვები პასუხგაცემულია Control Plane-ში, site არის მართვადი, გასაგები და API-სთვის executable. თუ პასუხი მხოლოდ კოდში, Access-ში ან UI-შია, არქიტექტურული კონტრაქტი არასრულია.

## 11. საბოლოო მიზანი

Control Plane უნდა იყოს თვითაღწერადი და ყველა site-ისთვის ერთიანი წესების ავტორიტეტი. Site-specific განსხვავება უნდა არსებობდეს მხოლოდ metadata-ში:

```text
new site
  = namespace + product + contract revision
  + datasets + fields + keys + indexes + relations
  + semantics + policies + mappings
```

ეს არის უნივერსალური, schema-agnostic, contract-driven platform-ის საფუძველი. KIDS მხოლოდ პირველი პროფილია; იგივე რიგი და იგივე მექანიზმები გამოიყენება ნებისმიერი მომავალი site-ისთვის.
