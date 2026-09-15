# GEOSOTAT სრული სისტემური სასწავლო გზამკვლევი

ეს დოკუმენტი აწყობს პროექტის სრულ ხაზს ბაზებიდან API-მდე. მისი მიზანია მკითხველმა დაინახოს არა მხოლოდ ცხრილები და endpoint-ები, არამედ პასუხისმგებლობები, საზღვრები, მიზეზები და მონაცემის სრული სიცოცხლის ციკლი.

## 0 სისტემის canonical identity და სრული ხაზი

GEOSOTAT არის **contract-driven, metadata-driven, schema-agnostic, multi-site data-product platform**. სისტემის საწყისი წერტილია Control Plane-ში დამტკიცებული, versioned და immutable contract revision. ყველა ქვედა layer ამ revision-ს ასრულებს; არც Access ფაილი, არც კონკრეტული KIDS branch და არც API caller არ არის კონტრაქტის ავტორი.

```text
approved contract
  → receive/fingerprint/stage/validate/quarantine
  → canonical materialization
  → reconciliation + quality/privacy gates
  → approved immutable snapshot
  → policy-aware relation-graph query
  → cache/export/API response
```

ფენების პასუხისმგებლობა მკაცრად იყოფა:

- **Control Plane** — catalog, contract registry, dataset/field/relation/projection metadata, compatibility, policy, identity/tenancy, workflow/approval და operation registry;
- **Ingestion Plane** — artifact receipt, fingerprint, staging, validation, quarantine, mapping, materialization, reconciliation და evidence;
- **Data Plane** — raw/entity/statistics/classification/geo/relation და snapshot მონაცემები;
- **Archive Plane** — immutable source artifact, checksum, provenance, retention, confidentiality და restore evidence;
- **Serving Plane** — contract compiler, authorization, relation graph, provider/family adapters, filters, aggregation, projection, cache, export და API delivery.

Access artifact არის ingest-ის source/transport და არა ცალკე authority. იგი არ უნდა შეიცავდეს credentials-ს, executable logic-ს ან publication authority-ს.

Production-complete სტატუსი მხოლოდ მაშინ არსებობს, როცა ერთდროულად დადასტურებულია reproducible release, authentication/authorization/tenant negative suite, მეორე provider-ის contract-only acceptance, quota/cost overload evidence, trace/SLO/alert evidence, immutable reconciliation bundle, rollback/replay, backup/restore და measured RPO/RTO. დაუმტკიცებელი external input ყოველთვის fail-closed რჩება.

### 0.1 მოქმედი სისტემური გადაწყვეტილებები

- სისტემა არის მრავალსაიტიანი, **contract-driven, metadata-driven, schema-agnostic data-product platform**;
- **Control Plane** აცხადებს კონტრაქტს და პოლიტიკას;
- **Ingestion Plane** იღებს, ამოწმებს, quarantine-ს უკეთებს და ამატერიალიზებს;
- **Data Plane** ინახავს canonical raw/entity/statistics/classification/geo/relation მონაცემებს;
- ყველა ცვლილება უნდა აკმაყოფილებდეს 10-პუნქტიან engineering gate-ს;
- production completion მხოლოდ implementation + automated tests + production evidence + independent review-ის ერთობლიობით ითვლება;
- KIDS-ისთვის საჭირო local/synthetic საფუძველი გაკეთებულია, მაგრამ საბოლოო მიზანი provider/site-agnostic პლატფორმაა;
- W-01–W-07 არის ერთადერთი მოქმედი დარჩენილი scope და ერთმანეთზე დამოკიდებულ execution order-ს მიჰყვება.

## 1 სწავლის სწორი თანმიმდევრობა

1. სამი ფიზიკური ბაზა და მათი პასუხისმგებლობა.
2. ფიზიკური data family-ები და პრეფიქსების კანონიკა.
3. Control Plane-ის meta-contract და საიტის კონტრაქტი.
4. Access artifact და ingest pipeline.
5. canonical entity, classification და statistics materialization.
6. quality, privacy, lineage და reconciliation gates.
7. publication snapshot, serving cache და rollback.
8. contract-driven API და relation graph.
9. KIDS R8-ის მიმდინარე მდგომარეობა და დარჩენილი platform work.

## 2 სამი ფიზიკური ბაზა

| ბაზა | როლი | მთავარი კითხვა | ძირითადი ობიექტები |
|---|---|---|---|
| geostat-system | Control Plane | რა არის ნებადართული | contract, revision, page, dataset, field, key, relation, index, policy, approval |
| geostat-data | Data Plane | რა canonical მონაცემი გვაქვს | ingest, raw locator, entity, statistics, classification, geo, publication, serving |
| geostat-archive | Archive Plane | რა მივიღეთ სინამდვილეში | immutable artifact, payload pointer, checksum, retention, restore evidence |

Access ფაილი მეოთხე authority არ არის. ის არის Control Plane-ის მიერ დამტკიცებული კონტრაქტის data-bearing transport.

## 3 პრეფიქსების კანონიკა

| პრეფიქსი | დანიშნულება | მაგალითი | მკაცრი საზღვარი |
|---|---|---|---|
| `__gs_` | schema და contract metadata | `__gs_field`, `__gs_relation`, `__gs_page` | აღწერს სტრუქტურას და არ არის business fact |
| `__raw_` | source artifact და provenance | `__raw_document`, `__raw_record` | არ შეიცავს დამტკიცებულ derived metric-ს |
| `__ent_` | business entity transport | `__ent_kids_goal` | ერთი გამოცხადებული business grain |
| `__stat_` | statistical transport | `__stat_kids_statistical_input` | measure, dimensions და period |
| `__cl_` | classifier transport | `__cl_item` | versioned scheme და item |
| `__rel_` | ფაქტობრივი კავშირი | `__rel_semantic_binding` | ინახავს link rows-ს და არა relation definition-ს |

`__gs_relation` აცხადებს, როგორ შეიძლება ორი structure-ის დაკავშირება. `__rel_*` ინახავს უკვე არსებულ კონკრეტულ კავშირს.

## 4 Control Plane

Control Plane არის პლატფორმის თვითაღწერა. მისი canonical შექმნის რიგია:

1. namespace და data product;
2. contract structure და immutable revision;
3. dataset და table definitions;
4. field definitions და value domains;
5. primary, unique და stable sort keys;
6. relations და required indexes;
7. semantic, classifier, quality, privacy და lineage bindings;
8. page, UI surface და response projection;
9. approval, migration და publication eligibility.

Control Plane-ს საკუთარი meta-contract სჭირდება, რადგან registry-საც აქვს schema, capabilities, mandatory fields, state transitions და compatibility rules.

## 5 Data Plane

| family | ცხრილები | grain | დანიშნულება |
|---|---|---|---|
| ingest | batch, artifact, dataset_load, staged_row, validation_issue, checkpoint | execution | resumable და idempotent მიღება |
| raw | document, source_record | artifact ან source row | lineage და source truth |
| entity | entity_record, entity_link, entity_classification, localized_text | business object | არასტატიკური canonical data |
| statistics | series, observation, dimension, attribute, revision | measure at dimensional key | SDMX-compatible facts |
| classification | system, version, item, mapping | governed code | controlled vocabulary |
| publication | snapshot, dataset_snapshot, member | immutable release | atomic publish და rollback |
| serving | cache და projection | published response | სწრაფი public read |

Stable physical families ნიშნავს, რომ ახალი საიტი ან dataset metadata-ით ერთვება. ახალი business table მხოლოდ მაშინ იქმნება, როცა ახალი grain არსებული family-ით ვერ გამოიხატება და ეს ADR-ითა და migration-ით დასტურდება.

## 6 Archive Plane და raw საზღვარი

`raw.document` ან Access-ში `__raw_document` არის უცვლელი source artifact-ის envelope. იგი ინახავს source system-ს, document identity-ს, MIME type-ს, checksum-ს, received time-ს, URI-ს, ingestion batch-ს, provenance-ს, retention-ს, confidentiality-სა და payload pointer-ს.

Correction ძველ artifact-ს არ გადაწერს. იქმნება ახალი artifact და version. დამტკიცებული metric, classifier item, observation ან UI projection raw document-ში არ იწერება.

## 7 Access და ingest

სრული შესრულების ხაზი:

1. პანელი ქმნის და ამტკიცებს კონტრაქტს.
2. generator ქმნის ცარიელ Access structure-ს approved registry-დან.
3. source მონაცემი ავსებს მხოლოდ გამოცხადებულ tables/fields-ს.
4. upload ქმნის immutable artifact-ს, batch-სა და dataset loads-ს.
5. staging ამოწმებს schema drift-ს, types-ს, required fields-ს, keys-სა და relations-ს.
6. invalid row გადადის quarantine-ში და original artifact არ იცვლება.
7. valid rows materialize-დება canonical families-ში.
8. lineage აკავშირებს canonical row-ს source artifact-სა და row locator-სთან.
9. gates-ის შემდეგ იქმნება immutable publication snapshot და serving cache.

Retry duplicate artifact-ს, batch-ს, staged row-ს, proposal-ს, entity-ს, observation-ს, relation-ს ან snapshot-ს არ ქმნის.

## 8 statistics classification და metadata

სტატისტიკური მოდელი ერთმანეთისგან გამოყოფს metric-ს, unit-ს, aggregation-ს, series key-ს, observation-ს, dimensions-სა და attributes-ს. Dimension-ის code classification item-ს უკავშირდება. Observation-ის lineage raw source record-ს უკავშირდება.

Metadata პირველი კლასის domain-ია. Title, description, locale, provenance, quality, confidentiality და presentation hints versioned assertions-ად ინახება. ერთი და იგივე metadata business table-ებში განმეორებით hardcode არ უნდა იყოს.

## 9 Contract driven API

API caller-ისგან არ იღებს SQL-ს ან physical table name-ს. იგი იღებს contract/page identity-ს და კონტრაქტით ნებადართულ query-ს.

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 11,
  "where": {"field": "metricCode", "op": "EQ", "value": "KIDS_OBS_VALUE_INPUT"},
  "include": ["dimensions.classifierItem", "raw.lineage"],
  "orderBy": [{"field": "period", "direction": "ASC"}],
  "page": {"size": 100, "after": null}
}
```

ძრავა ასრულებს introspection-ს, allowlist validation-ს, relation graph planning-ს, filters/aggregation-ს, privacy policy-ს, snapshot binding-სა და response shaping-ს.

## 10 KIDS R8 მიმდინარე მდგომარეობა

მიმდინარე runtime authority არის `KIDS-R8-current-status-and-acceptance.md`. ძველ R7 handoff-ებში არსებული `statistics.series = 0` ჩანაწერები ისტორიულია.

| მაჩვენებელი | დადასტურებული შედეგი |
|---|---|
| Contract | KIDS_PORTAL_V1 revision 8 APPROVED |
| datasets | 15 |
| goals | 36 |
| resources | 225 |
| assignments | 230 |
| glossary | 178 |
| statistical observations | 880 |
| serving cache | 297 |
| publication snapshot | 13 PUBLISHED |
| archive | 2,224 records და 2,224 payload pointers |

Rollback, idempotent replay, quarantine, backup/restore და DR replay დადასტურებულია. UI და JWT hardening ამ acceptance package-ის ფარგლებს გარეთ დარჩა.

## 11 გაკეთებული და დარჩენილი

დასრულებულია სამი plane, contract registry, KIDS R8 ingest/materialization/publication, relation execution, introspection, bounded query DSL, signed cursor, ETag, RFC 9457 errors, OpenAPI/JSON Schema/TypeScript baseline, SDMX facade და JSON/CSV/NDJSON async export.

Implementation-level rate/quota/cost control, metadata negotiation, provider lifecycle, Java/Kotlin/Dart generators, SDMX/Parquet/ZIP conformance scaffolding, stable-key profiles და documentation drift automation უკვე დაფარულია. დარჩენილია მხოლოდ production authority/evidence: approved release/tag, რეალური OIDC/JWKS/ABAC tenant policy, დაცული production replay, SQL Server/MySQL workload endpoints, durable telemetry/alert firing, signed publication evidence და დამტკიცებული load/chaos/DR ფანჯარა. UI ამ guide-ის ფარგლებს გარეთაა.

## 12 კითხვები და პასუხები

### რა არის სისტემის საწყისი წერტილი

Approved და versioned contract. მანამდე არსებობს source, მაგრამ canonical/public meaning ჯერ არ არსებობს.

### რატომ არის სამი database

Governance, mutable processing და immutable evidence განსხვავებულ lifecycle-სა და უსაფრთხოების წესს ითხოვს.

### pageId და resourceId ერთია

არა. pageId არის Control Plane-ის navigation surface. resourceId არის Data Plane-ის კონკრეტული business row identity. მათ contract mapping აკავშირებს.

### შეიძლება raw და statistics ერთ response-ში

კი, თუ approved relation/lineage path და privacy policy ამას უშვებს.

### როგორ ემატება ახალი საიტი

რეგისტრირდება namespace, product, contract, datasets, fields, keys, relations, policies, pages და source bindings. Generic engine-ში site-specific branch არ ემატება.

### როდის ითვლება სამუშაო დასრულებულად

როცა არსებობს deploy, automated test, runtime receipt, row/checksum reconciliation, security/quality evidence და გამოცდილი rollback.
