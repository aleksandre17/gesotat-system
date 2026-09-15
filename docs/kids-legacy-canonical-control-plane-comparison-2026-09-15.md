# KIDS — Legacy API, R8 Access და Control Plane-ის კანონიკური შედარება

**თარიღი:** 2026-09-15  
**რეჟიმი:** read-only observation / reconciliation preparation  
**კონტრაქტი:** `KIDS_PORTAL_V1`, revision `8`  
**მიზანი:** KIDS-ის არსებული API payload-ების შედარება R8 Access artifact-სა და Control Plane-ის გამოცხადებულ page/dataset/field/relation/projection მოდელთან. ეს დოკუმენტი არ ცვლის არც legacy API-ს, არც Access-ს და არც production contract-ს.

## 1. სამი წყაროს როლი

| წყარო | რას წარმოადგენს | ავტორიტეტი |
|---|---|---|
| Legacy KID API | უკვე არსებული consumer-facing payload-ები (`/api/goals`, `/api/files`, `/api/glossary`) | დაკვირვებული compatibility input; არა ახალი არქიტექტურის source of truth |
| R8 Access artifact | კონტრაქტთან ერთად გადმოსაცემი immutable package, prefixed physical tables-ით | ingestion input და package evidence |
| Control Plane | contract, revision, page, dataset, field, relation, projection და policy registry | runtime source of truth; response-ის საბოლოო shape აქედან განისაზღვრება |

წესი: legacy ფორმა შეიძლება შენარჩუნდეს მხოლოდ versioned compatibility adapter-ში. ახალი consumer არ უნდა დაეყრდნოს legacy field names-ს, category magic numbers-ს ან `chartdata`-ში დამალულ JSON-ს.

## 2. დაკვირვებული legacy API payload-ები

| Request | დაკვირვებული rows | ველები | კანონიკური სამიზნე |
|---|---:|---|---|
| `GET /api/goals` | 36 | `ID`, `category`, `title_geo`, `title_eng`, `path_geo`, `path_eng`, `category_title_geo`, `category_title_eng` | page `8` → `KIDS_GOAL` |
| `GET /api/files?category=1` | 43 | `ID`, `category`, `sub_category`, localized titles/paths, `chartdata` | page `9` resource rows; selected statistical carrier/observation projection → page `11` |
| `GET /api/files?category=2` | 55 | იგივე | page `9` |
| `GET /api/files?category=3` | 43 | იგივე | page `9` |
| `GET /api/files?category=4` | 18 | იგივე | page `9` |
| `GET /api/glossary?lang=ka` | 90 | `ID`, `lang`, `text` | page `10` → `KIDS_GLOSSARY_ENTRY` |
| `GET /api/glossary?lang=en` | 87 | `ID`, `lang`, `text` | page `10` → `KIDS_GLOSSARY_ENTRY` |

`/api/files`-ის დაკვირვებული კატეგორიების ჯამია **159**. ეს არ არის ავტომატურად R8 resource-row count-ის ექვივალენტი: R8 შეიძლება შეიცავდეს დამატებით resource grain-ს, lineage-ს ან category-independent ჩანაწერს. საბოლოო parity მოითხოვს source-key-ებზე დაფუძნებულ reconciliation-ს.

## 3. R8 Access artifact-ის ფაქტობრივი inventory

აუდიტის task-მა (`:api:auditKidsCanonicalAccess`) დაადასტურა:

| Physical table | rows | columns | როლი |
|---|---:|---:|---|
| `__gs_package` | 1 | 8 | package identity/checksum/revision |
| `__gs_page` | 6 | 12 | page registry (7–12) |
| `__gs_dataset` | 15 | 4 | virtual dataset registry |
| `__gs_field` | 122 | 5 | field contract |
| `__gs_key` | 17 | 4 | stable/business keys |
| `__gs_relation` | 21 | 7 | declared relation graph |
| `__gs_projection` | 6 | 5 | response/projection definitions |
| `__gs_metadata_schema` / `__gs_metadata` | 2 / 267 | 5 / 12 | typed, localized governed metadata |
| `__cl_scheme` / `__cl_version` / `__cl_item` / `__cl_alias` | 4 / 4 / 36 / 36 | — | classifier registry and aliases |
| `__raw_document` | 456 | 22 | immutable source/lineage records |
| `__ent_kids_goal` | 36 | 8 | canonical goal entity |
| `__ent_kids_resource` | 225 | 8 | canonical resource entity |
| `__rel_kids_resource_subcategory_assignment` | 230 | 7 | resource ↔ subcategory relation |
| `__ent_kids_glossary_entry` | 178 | 6 | bilingual glossary entity |
| `__raw_kids_statistical_carrier` | 43 | 6 | statistical source/carrier identity |
| `__stat_kids_statistical_input` | 880 | 13 | typed observation cells |
| `__rel_kids_statistical_semantic_binding` | 43 | 10 | carrier ↔ metric/unit/dimension semantics |

**Audit result:** all 21 physical tables have primary keys; 21 declared physical relationships are enforced; status `PRODUCTION_ACCEPTED` for the artifact audit. Current approved artifact fingerprint: `2,834,432` bytes, SHA-256 `D62C58C9633766BC597C1EF67F5984FD7104F922C837224374E5D194F6F0BFF7`.

## 4. Semantic mapping — what changes and why

### Goals (page 8)

Legacy `ID/category/title_geo/title_eng/path_*` becomes a typed entity with localized `title`, localized `path`, category relation and lineage. The UI receives a contract response, not database-shaped columns. Observed count matches exactly: **36 ↔ 36**.

### Resources and files (page 9)

Legacy `category` and `sub_category` are transport fields. In R8 they are resolved through declared relations/classifier aliases; `__rel_kids_resource_subcategory_assignment` preserves many-to-many assignments. `chartdata` is retained as raw lineage and is interpreted only by an approved statistical projection. It is not a free-form serving schema.

Observed legacy rows: **159** across categories 1–4. R8 canonical resources: **225**, assignments: **230**. The page-9 runtime query is now snapshot-bound through `dataset_version_id → publication.dataset_snapshot_id → entity.entity_record`. Because the legacy request only exposes categories 1–4, the 66-row delta is currently classified as **filtered legacy subset candidate** (scope mismatch), not as duplicate or deletion. Stable-key reconciliation remains required before final closure; no count is artificially normalized.

### Glossary (page 10)

Legacy language is a query parameter (`lang=ka|en`) and ordering is implicit. R8 stores localized fields and language semantics in the entity/field contract. The read-only stable-key/language/text audit now reports **178 ↔ 178**, `missing=0`, `extra=0`, `textMismatch=0` (`GLOSSARY_PARITY_PASS`). The earlier 177 observation was an incomplete legacy probe, not a canonical data delta.

### Statistics (page 11)

Legacy `chartdata` embeds statistical values inside a file payload. R8 separates:

`carrier (43) → semantic binding (43) → typed statistical input cells (880)`

The Control Plane declares dimensions, measure, unit, aggregation, filters and response shape. Therefore the canonical API can return structured `series/observations/dimensions/lineage` without exposing `chartdata` parsing to the consumer.

## 5. Control Plane request contract

The consumer follows this order:

```text
1. GET /api/v1/platform/contracts/KIDS_PORTAL_V1/revisions/8/introspection
2. GET /api/v1/platform/contracts/KIDS_PORTAL_V1/pages?revision=8
3. GET /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/{pageId}/query-capabilities?revision=8
4. POST /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/{pageId}/query
5. Optional governed export endpoint, using the same contract/projection
```

Capabilities response is the executable boundary: it tells the client which fields, filters, relations/includes, aggregations, sort keys, pagination and response shape are allowed. A client must not invent a table, SQL fragment, legacy category interpretation or unlisted field.

## 6. Reconciliation matrix and status

| Check | Evidence | Status | Next action |
|---|---|---|---|
| Contract/page registry | Access `__gs_page`, `__gs_dataset`, `__gs_field`, `__gs_projection`; Control Plane revision 8 | **OBSERVED/PASS** | verify live introspection payload against artifact checksum |
| Goal cardinality | legacy 36; Access 36 | **PASS** | normalized field parity test |
| Resource cardinality | legacy 159 (categories 1–4); canonical 225 | **PARTIAL — scope delta identified** | stable source-key diff and explicit category-scope report; do not force count equality |
| Glossary cardinality | legacy/canonical stable-key audit 178 ↔ 178 | **PASS** | retain normalized parity evidence and verify live response lineage/privacy |
| Statistical carrier identity | legacy category-1 files 43; Access carriers 43 | **CANDIDATE PASS** | compare stable IDs and carrier metadata |
| Statistical values | legacy embedded `chartdata`; Access 880 typed cells | **OPEN** | shadow projection parity by carrier/period/dimension |
| Relations | legacy endpoints expose no explicit graph; Access declares 21 relations | **PASS structurally** | API cross-family execution acceptance |
| Lineage | absent from legacy response; present in raw/entity/statistical contract | **PASS structurally** | verify response lineage policy and redaction |
| Response shape | legacy arrays; Control Plane contract-shaped envelopes | **MIGRATION REQUIRED** | canonical adapter + shadow comparison |
| Legacy fallback | existing compatibility path and explicit retirement governance | **GOVERNED OPEN** | consumer-impact review, deprecation window, rollback plan |

## 7. დასკვნა

R8 Access და Control Plane უკვე აღწერს უფრო მდიდარ, typed და relation-aware მოდელს, ვიდრე legacy API. Goal-ისთვის პირდაპირი parity დადასტურებულია; resource, glossary და statistical payload-ებისთვის რაოდენობრივი სხვაობები საჭიროებს source-key reconciliation-სა და shadow projection-ს. ეს განსხვავებები არ უნდა “გასწორდეს” მონაცემის ხელოვნური წაშლით ან KIDS-specific branch-ით.

**ამ დოკუმენტის სტატუსი: `OBSERVED → READY_FOR_AGREEMENT`.**  
შემდეგი შეთანხმებული ნაბიჯი არის მხოლოდ delta-classification: ჯერ resource/glossary/source-key parity, შემდეგ statistical shadow projection. UI wiring და legacy endpoint retirement ამ შედარების დამტკიცებამდე არ იცვლება.

## 8. ნაბიჯ-ნაბიჯ განხილვა — ქეისი 1: Goals / pageId 8

### 8.1 წყაროების ჯაჭვი

```text
legacy GET /api/goals
        ↓ compatibility observation
__raw_document (immutable lineage)
        ↓ governed materialization
__ent_kids_goal (36 rows)
        ↓ Control Plane contract
KIDS_PORTAL_V1 / pageId 8 / KIDS_GOAL
        ↓ canonical query
POST /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/8/query
```

### 8.2 ველების შესაბამისობა

| Legacy field | Canonical meaning | კონტრაქტული წესრიგი |
|---|---|---|
| `ID` | stable source identifier / entity key | ინახება lineage-თან ერთად; consumer იღებს contract-defined `id`-ს |
| `title_geo` | localized title, `ka` | მიეწოდება `title.ka`-ში |
| `title_eng` | localized title, `en` | მიეწოდება `title.en`-ში |
| `path_geo` | localized resource/path reference, `ka` | მიეწოდება გამოცხადებულ localized path-ში |
| `path_eng` | localized resource/path reference, `en` | მიეწოდება გამოცხადებულ localized path-ში |
| `category` | category classification/reference | არ ითარგმნება magic number-ად; resolve ხდება declared classifier/relation-ით |
| `category_title_geo` / `category_title_eng` | category label projection | მხოლოდ კონტრაქტით გამოცხადებულ projection-ში; არ ხდება დაუმტკიცებელი field passthrough |

### 8.3 მოთხოვნა და კანონიკური პასუხი

```http
POST /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/8/query
Authorization: Bearer <OIDC access token>
Content-Type: application/json
```

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 8,
  "projection": "default",
  "select": ["id", "title", "path", "category"],
  "sort": [{"field": "id", "direction": "ASC"}],
  "page": {"limit": 100}
}
```

მოსალოდნელი shape (ზუსტი ველების სია საბოლოოდ capability response-იდან უნდა დადასტურდეს):

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 8,
  "datasetCode": "KIDS_GOAL",
  "data": [
    {
      "id": "<stable-id>",
      "title": {"ka": "<ქართული სათაური>", "en": "<English title>"},
      "path": {"ka": "<path>", "en": "<path>"},
      "category": {"code": "<declared-code>", "label": {"ka": "<...>", "en": "<...>"}}
    }
  ],
  "page": {"limit": 100, "nextCursor": "<server-issued-or-null>"},
  "lineage": {"included": true, "policy": "contract-governed"}
}
```

### 8.4 ამ ქეისის acceptance checklist

- [x] legacy `/api/goals` რეალურად აბრუნებს 36 ჩანაწერს.
- [x] R8 `__ent_kids_goal` რეალურად შეიცავს 36 ჩანაწერს.
- [x] ორივე წყაროს stable `ID`-ები source-key დონეზე შედარდა (`auditKidsGoalsParity`).
- [x] localized title/path parity — `ka` და `en` მნიშვნელობების diff: `fieldMismatch=0`.
- [x] category code/label relation-ის parity — classifier reference resolution: `categoryUnresolved=0`, `categoryMismatch=0`.
- [x] Control Plane capability source-ის consistency — `KIDS_GOALS → KIDS_GOAL`, revision 8, 8 fields და `READY` projection დადასტურებულია (`auditKidsGoalsCapability` PASS). Live protected endpoint-ის consumer replay ცალკე დარჩა.
- [ ] canonical query-ის რეალური response და legacy normalized response-ის shadow diff.
- [ ] lineage/privacy policy-ის დამოწმება.

**ქეისი 1-ის სტატუსი: `PARTIAL — key/localized/category parity PASS; API semantics OPEN`.**

**წინა blocker დახურულია:** `KIDS_GOAL_ENTITY` projection კონტრაქტის generator-ში
დამტკიცდა `READY`-ად, ახალი artifact ხელახლა გენერირდა და capability audit PASS-ია.

### 8.5 შესრულებული evidence

```text
Command: ./gradlew.bat :api:auditKidsGoalsParity --rerun-tasks --console=plain
sourceRows=36
canonicalRows=36
missing=0, extra=0, fieldMismatch=0
categoryUnresolved=0, categoryMismatch=0
status=GOALS_PARITY_PASS
```

Capability evidence:

```text
projection=KIDS_GOAL_ENTITY, state=READY
page=KIDS_GOALS, contract=KIDS_PORTAL_V1, revision=8, dataset=KIDS_GOAL
fields=8, readyProjection=true
status=GOALS_CAPABILITY_PASS
```

### 8.6 Live runtime probe

Read-only probe against `http://192.168.1.199:8083/api/v1` returned `401`
consistently for introspection, page listing, page capabilities and page query.
ეს არის სწორი fail-closed ქცევა, მაგრამ runtime response shadow comparison ჯერ
ვერ სრულდება, რადგან მოქმედი OIDC access token/approved test principal არ არის
მოწოდებული.

```text
introspection                  HTTP 401
pages                          HTTP 401
page/8/query-capabilities      HTTP 401
page/8/query                   HTTP 401
```

**Runtime status:** `BLOCKED_BY_RUNTIME_BINDING` — approved test token მიღებულია,
მაგრამ API-მ დააბრუნა `401 Unsupported JWT token`: runtime validator ჯერ HMAC
secret-ს იყენებს, ხოლო Keycloak token არის asymmetric `RS256`. Anonymous bypass,
HMAC secret-ის ხელოვნური ჩანაცვლება ან issuer-ის დაუმტკიცებელი ცვლილება
დაუშვებელია.

ამ ეტაპზე შემდეგ ქეისზე გადასვლა არ შეიძლება. ჯერ უნდა დასრულდეს stable-key, localized-field და category-relation shadow comparison pageId 8-ზე.

### 8.5 Improvement record — რატომ არის ეს ქეისი პლატფორმის გამაძლიერებელი

Goals-ის მარტივმა payload-მაც გამოაჩინა reusable გაუმჯობესებები, რომლებიც ყველა
მომავალ site/provider-ზე უნდა იმუშაოს:

1. **Localized-field normalizer** — `*_geo/*_eng` მსგავსი legacy წყვილები
   ავტომატურად გარდაიქმნას contract-defined locale map-ად; დაკარგული locale
   იყოს explicit validation result და არა ჩუმი fallback.
2. **Source-key parity checker** — stable ID-ების, duplicate-ების და missing /
   extra row-ების ავტომატური კლასიფიკაცია ყველა entity family-ისთვის.
3. **Capability-driven select validation** — consumer-ს მიეწოდოს მხოლოდ
   გამოცხადებული fields/sorts/filters; უცნობი field fail-closed-ად უარყოფილი
   იყოს.
4. **Relation-backed category resolver** — numeric `category` აღარ იყოს
   business rule; classifier/relation registry იყოს ერთადერთი განმარტება.
5. **Generated response mapper** — legacy adapter და canonical response ერთი
   typed intermediate model-ით შეადაროს, რათა parity ტესტი კონკრეტულ KIDS code-ზე
   არ იყოს მიბმული.
6. **Drift evidence** — row-count, key-set hash და localized semantic diff
   release evidence bundle-ში ჩაიწეროს.

ეს გაუმჯობესებები ჯერ **PROPOSED**-ია; მათი implementation მხოლოდ pageId 8-ის
source-key comparison-ის შეთანხმების შემდეგ იწყება.
