# KIDS frontend — complete API request/response catalog

**Observed:** 2026-09-15  
**Current base:** `https://youth-api.geostat.ge`  
**Canonical target:** `KIDS_PORTAL_V1` revision 8 through the GEOSOTAT API

This document records every HTTP data request found in the KIDS frontend and
the observed response contracts. It is the implementation handoff for the
environment and adapter work; it does not authorize production switching.

## 1. Request inventory

The source contains exactly three dynamic `fetch` requests:

| ID | Method | Current path | Parameters | Observed result |
|---|---|---|---|---|
| RQ-01 | GET | `/api/goals` | none | HTTP 200, JSON array, 19,215 bytes in the probe |
| RQ-02 | GET | `/api/files?category={n}` | `category` integer; tested 1–4 | HTTP 200, JSON array; counts 129, 55, 43, 18 respectively |
| RQ-03 | GET | `/api/glossary?lang={code}` | `lang=ka` or `lang=en` | HTTP 200, JSON array; 87 English and 69718-byte Georgian response in probe |

All requests use `VITE_API_URL`, have no authorization header, no pagination,
no correlation ID and no conditional request headers. Components perform local
sorting, grouping, filtering and chart parsing.

## 2. RQ-01 — goals

### Request

```http
GET https://youth-api.geostat.ge/api/goals
Accept: application/json
```

### Response shape

```json
[
  {
    "ID": 1,
    "category": 1,
    "title_geo": "1.2.1_სიღარიბის ...",
    "title_eng": "1.2.1_Share of Population ...",
    "path_geo": "1.2.1_absoluturi-sigaribe.xlsx",
    "path_eng": "1.2.1_Absolute Poverty.xlsx",
    "category_title_geo": "არა სიღარიბეს",
    "category_title_eng": "NO POVERTY"
  }
]
```

The UI groups rows by `category`, assumes categories are numeric, and builds a
download URL from `path_geo/path_eng`. Null, duplicate or unknown categories
are silently dropped or rendered with a generated title.

### Canonical request

```http
POST /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/8/query
Authorization: Bearer <runtime OIDC token>
Content-Type: application/json
```

```json
{
  "select": ["goalId", "categoryCode", "title", "resourceRef"],
  "include": ["resources"],
  "orderBy": [{"field": "categoryCode", "direction": "ASC"}]
}
```

The adapter must map the declared localized title and governed resource pointer
to the existing goal view-model. It must not recreate `/files/goals/...` in the
browser.

## 3. RQ-02 — section files and charts

### Request

```http
GET https://youth-api.geostat.ge/api/files?category=1
Accept: application/json
```

`category` is a required integer in current behavior. Probes returned:

| category | rows |
|---:|---:|
| 1 | 129 |
| 2 | 55 |
| 3 | 43 |
| 4 | 18 |

### Response shape

```json
[
  {
    "ID": 128,
    "category": 1,
    "sub_category": "3",
    "title_geo": "მოსახლეობის რიცხოვნობა ასაკისა და სქესის მიხედვით (აღწერა)",
    "title_eng": "Number of population by age and sex (census)",
    "path_geo": "files/mosaxleoba/01_01_...xlsx",
    "path_eng": "files/mosaxleoba/01_01_...xlsx",
    "chartdata": "[\\n {\\n \"year\": \"1939\", ... }]"
  }
]
```

The frontend parses `chartdata` through multiple repair attempts (escaped
newlines, escaped quotes and trailing commas), converts `sub_category` to
local filter categories, synthesizes a folder URL from `sectionKey`, and then
builds Highcharts series. This is a high-risk boundary: malformed JSON is
converted to `null` and the chart silently disappears.

### Canonical split

Resource metadata belongs to page 9; statistical observations and dimensions
belong to page 11. A canonical request must declare the section/category as a
contract-approved filter and receive typed observations, metric, unit,
aggregation, dimensions, lineage and governed download references. No chart
JSON string or source folder reconstruction is allowed in the final adapter.

## 4. RQ-03 — glossary

### Request

```http
GET https://youth-api.geostat.ge/api/glossary?lang=ka
Accept: application/json
```

`lang` is currently `ka` or `en`. The probe returned an array of objects with
`ID`, `lang` and `text`; the component sorts by `text` locally and performs
case-insensitive client-side search.

```json
[
  {
    "ID": 1,
    "lang": "ka",
    "text": "გადაადგილების, დგომის დარღვევა ..."
  }
]
```

### Canonical request

```http
POST /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/10/query
Authorization: Bearer <runtime OIDC token>
Content-Type: application/json
```

```json
{
  "filters": {"languageCode": "ka"},
  "orderBy": [{"field": "label", "direction": "ASC"}],
  "limit": 100
}
```

The adapter must use the declared localized label field and server pagination;
the browser may filter only the received page unless the contract explicitly
allows search semantics.

## 5. Requests that are not API calls

`Main.jsx` contains static links to publication PDFs, legislation, external
institutions and pyramid services. Local images, fonts and cover PDFs are build
assets. These require resource-governance classification but are not included
in the three dynamic API request contracts above.

## 6. Cross-cutting request controls to add

- runtime-configured base URL; no hardcoded host;
- OIDC bearer token from approved runtime storage;
- timeout, cancellation and correlation ID;
- `Accept`, locale and contract revision headers;
- bounded `limit`, filters and includes;
- typed error envelope and 401/403 handling;
- ETag/conditional requests where supported;
- telemetry with URL/field redaction;
- fixture parity tests for every observed response shape.

## 7. Acceptance sequence

1. Freeze the three observed legacy fixtures.
2. Approve field/relation mappings for pages 8, 9, 10 and 11.
3. Implement a single platform client and three adapters.
4. Run legacy-vs-canonical normalized parity checks.
5. Enable canonical mode in staging with OIDC and CORS.
6. Verify counts, localized values, chart series, downloads and lineage.
7. Approve production switch and retain legacy fallback only for a dated window.

No production cutover is valid until the canonical API origin, browser auth
policy and semantic statistics mapping are approved.
