# KIDS frontend — canonical request preparation

This phase prepares the KIDS consumer for the GEOSOTAT contract-driven API. It
does not replace existing response rendering or enable production cutover.

## Prepared boundary

- `src/platform/siteContract.js` is the single site/page identity registry.
- `src/platform/requestCases.js` contains declarative request cases.
- `src/platform/platformRequestClient.js` owns URL construction, headers,
  runtime-only token lookup, timeout, cancellation and request IDs.
- Components must call the client/request cases; they must not build canonical
  URLs or SQL themselves.

## Request cases

| Case | Method | Purpose | Page |
|---|---|---|---:|
| `discoverContract` | GET | discover declared pages and revision | site |
| `pageCapabilities` | GET | discover allowed fields/filters/includes | 7–12 |
| `readPage` | GET | compatibility page read | 7–12 |
| `queryPage` | POST | contract-validated query | 7–12 |
| `exportPage` | POST | governed asynchronous export | 7–12 |

## Safety rules

1. The API base URL comes only from the KIDS environment profile.
2. Contract code, revision and page IDs come from the declarative registry.
3. Tokens are read from session storage at runtime and never committed.
4. Missing canonical API configuration fails closed.
5. Every request has JSON accept headers, a request ID, timeout and cancellation.
6. Response parsing/mapping is deliberately deferred to the next agreed phase.
7. The legacy adapter remains the current compatibility path until fixture
   parity, OIDC/CORS and semantic mapping are approved.

## Next agreement gate

Before wiring components to these cases, approve the response envelope mapping
for goals, resources, glossary and statistics. Only then may Phase B replace
the direct legacy `fetch` calls.

## Runtime step-by-step sequence

The following order is normative. A later request must not be sent when an
earlier gate fails.

## Collaborative execution rule

This sequence is executed one step at a time. For every step the status moves
through `PROPOSED → AGREED → IMPLEMENTED → VERIFIED`. The next step may not
start until the current step's purpose, request, inputs, output and acceptance
criteria have been explained and explicitly agreed. The plan is therefore a
shared learning and decision record, not an instruction to change every layer
in one batch.

## Step 0 — Application Bootstrap

ამ ეტაპზე საიტი ჯერ API-ს არ იძახებს. ის მხოლოდ ამოწმებს, იცის თუ არა:

- რომელ API-ს უნდა დაუკავშირდეს;
- რომელი საიტის contract-ს იყენებს;
- რომელი revision არის საჭირო;
- ავტორიზაციის რომელი რეჟიმი მოქმედებს.

კონფიგურაცია მოდის environment profile-იდან:

```env
VITE_PLATFORM_API_URL=https://<approved-api-origin>/api/v1
VITE_SITE_CONTRACT_CODE=KIDS_PORTAL_V1
VITE_SITE_CONTRACT_REVISION=8
VITE_PLATFORM_AUTH_MODE=oidc
```

საიტის startup logic-ის იდეა:

```js
function loadSiteRuntimeConfig() {
  const config = {
    apiBaseUrl: import.meta.env.VITE_PLATFORM_API_URL,
    contractCode: import.meta.env.VITE_SITE_CONTRACT_CODE,
    contractRevision: Number(
      import.meta.env.VITE_SITE_CONTRACT_REVISION
    ),
    authMode: import.meta.env.VITE_PLATFORM_AUTH_MODE,
  };

  if (!config.apiBaseUrl) {
    throw new Error("Canonical API URL is missing");
  }

  if (!config.contractCode) {
    throw new Error("Site contract code is missing");
  }

  if (!Number.isInteger(config.contractRevision)) {
    throw new Error("Invalid contract revision");
  }

  if (!["oidc", "none"].includes(config.authMode)) {
    throw new Error("Unsupported authentication mode");
  }

  return Object.freeze(config);
}
```

ამის შემდეგ იქმნება runtime context:

```js
const runtime = loadSiteRuntimeConfig();

const siteContext = {
  site: "kids",
  contractCode: runtime.contractCode,
  revision: runtime.contractRevision,
  apiBaseUrl: runtime.apiBaseUrl,
};
```

ამ ეტაპის შედეგია მხოლოდ:

```json
{
  "site": "kids",
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "apiBaseUrl": "configured",
  "status": "READY_FOR_AUTH"
}
```

თუ რომელიმე მნიშვნელობა აკლია ან არასწორია:

```text
არ გაიგზავნოს API request
არ ჩაირთოს legacy fallback production-ში
გამოჩნდეს controlled configuration error
დაიწეროს redacted telemetry
```

ამ ეტაპზე ჯერ არ ხდება:

- OIDC token-ის მიღება;
- contract discovery;
- page request;
- data query;
- response mapping.

Step 0-ის სტატუსია: **PROPOSED**.

თუ ამ ლოგიკას ვეთანხმებით, შემდეგ ნაბიჯზე გადავდივართ — **Step 1: OIDC
authentication bootstrap**.

## Step 1 — OIDC Authentication Bootstrap

ამ ეტაპზე საიტი ჯერ მონაცემებს არ ითხოვს. ის მხოლოდ იღებს დამტკიცებულ OIDC
access token-ს, რათა შემდეგ Control Plane-ის discovery request-ის გაგზავნის
უფლება ჰქონდეს.

საიტმა უნდა იცოდეს:

- რომელი OIDC issuer არის დამტკიცებული;
- რომელი client გამოიყენება ბრაუზერისთვის;
- რომელი redirect URI არის რეგისტრირებული;
- რომელი scope/role სჭირდება read-only discovery-ს;
- რომელი site/tenant claim უნდა ჰქონდეს token-ს.

კონფიგურაცია მოდის environment profile-იდან ან approved runtime configuration-
იდან; client secret frontend-ში არასდროს ინახება:

```env
VITE_OIDC_ISSUER=https://<approved-issuer>
VITE_OIDC_CLIENT_ID=kids-frontend
VITE_OIDC_REDIRECT_URI=https://<approved-site>/auth/callback
VITE_OIDC_SCOPE=openid profile READ_RESOURCE
VITE_OIDC_SITE_CLAIM=kids
```

ბრაუზერისთვის გამოიყენება Authorization Code + PKCE flow:

```js
async function bootstrapAuthentication() {
  const oidc = loadOidcRuntimeConfig();

  if (oidc.authMode === "none") {
    if (import.meta.env.MODE === "production") {
      throw new Error("Anonymous authentication is forbidden in production");
    }
    return { status: "AUTHENTICATED_DEV_ONLY", accessToken: null };
  }

  const client = await oidcClient.discover(oidc.issuer);
  const session = await client.signInWithAuthorizationCodePKCE({
    clientId: oidc.clientId,
    redirectUri: oidc.redirectUri,
    scope: oidc.scope,
  });

  validateAccessToken(session.accessToken, {
    issuer: oidc.issuer,
    requiredScope: "READ_RESOURCE",
    site: oidc.siteClaim,
  });

  runtimeTokenStore.set(session.accessToken);
  return { status: "AUTHENTICATED", expiresAt: session.expiresAt };
}
```

ეს კოდი მხოლოდ flow-ის ილუსტრაციაა. რეალური OIDC issuer, client და redirect
URI უნდა იყოს Control Plane/identity owner-ის მიერ დამტკიცებული.

### რა უნდა გადაამოწმოს საიტმა token-ის მიღების შემდეგ

```text
signature algorithm არის asymmetric
issuer ზუსტად ემთხვევა approved issuer-ს
audience/client სწორია
token ვადაგასული არ არის
required scope/role READ_RESOURCE არსებობს
site/tenant claim სწორია
kid მოქმედ JWKS key-ს ემთხვევა
```

ამის შემდეგ იქმნება runtime auth context:

```json
{
  "status": "AUTHENTICATED",
  "authMode": "oidc",
  "scope": ["openid", "profile", "READ_RESOURCE"],
  "site": "kids",
  "tokenStorage": "runtime-only",
  "expiresAt": "<token-expiry>"
}
```

თუ token ვერ მიიღება ან რომელიმე claim/validation ვერ გაივლის:

```text
არ გაიგზავნოს contract discovery request
არ ჩაირთოს anonymous production fallback
გამოჩნდეს controlled authentication error
დაიწეროს redacted security telemetry
```

### Step 1 lifecycle

| Lifecycle event | Execute Step 1? | Reason |
|---|---:|---|
| first application load | yes | creates authenticated runtime session |
| valid session restored | validate only | reuse token if still valid |
| token close to expiry | refresh with PKCE/session mechanism | prevents query interruption |
| token expired | yes | re-authentication is required |
| sign-out | clear runtime token | removes authorization context |
| route change | no | authentication context remains valid |
| contract revision change | no | only data contract must be rediscovered |

Step 1-ის სტატუსია: **PROPOSED**.

თუ ამ authentication წესს ვეთანხმებით, შემდეგი ნაბიჯი იქნება **Step 2:
Contract/Page Discovery** — პირველი რეალური Control Plane request.

## Step 2 — Contract/Page Discovery

ამ ეტაპზე საიტი უკვე ავტორიზებულია, მაგრამ ჯერ არც ერთ მონაცემს არ ითხოვს.
ის Control Plane-ს ეკითხება, რომელი გვერდები და dataset-ებია დამტკიცებული
არჩეული contract revision-ისთვის.

საიტმა უნდა მოითხოვოს:

- contract-ის დამტკიცებული სტატუსი;
- კონკრეტული revision;
- pageId-ები;
- nodeCode/nodeKind;
- datasetCode;
- route/path და title metadata;
- საჭიროების შემთხვევაში page-ის publication state.

პირველი რეალური request:

```http
GET {API_BASE}/platform/contracts/{contractCode}/pages?revision={revision}
Authorization: Bearer {accessToken}
Accept: application/json
Accept-Language: ka
X-Request-ID: {uuid}
```

KIDS-ის შემთხვევაში:

```text
GET /api/v1/platform/contracts/KIDS_PORTAL_V1/pages?revision=8
```

საიტის request code-ის იდეა:

```js
async function discoverContractPages(siteContext, accessToken) {
  const url = new URL(
    `${siteContext.apiBaseUrl}/platform/contracts/${encodeURIComponent(siteContext.contractCode)}/pages`,
  );
  url.searchParams.set("revision", String(siteContext.revision));

  const response = await fetch(url, {
    method: "GET",
    headers: {
      Accept: "application/json",
      "Accept-Language": "ka",
      Authorization: `Bearer ${accessToken}`,
      "X-Request-ID": crypto.randomUUID(),
    },
  });

  if (!response.ok) {
    throw new Error(`Contract discovery failed: ${response.status}`);
  }

  const document = await response.json();
  validateDiscoveredContract(document, siteContext);
  return document;
}
```

სავარაუდო response shape:

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "status": "APPROVED",
  "pages": [
    {
      "pageId": 7,
      "nodeCode": "KIDS_ROOT",
      "nodeKind": "SITE_ROOT",
      "path": "/kids"
    },
    {
      "pageId": 8,
      "nodeCode": "KIDS_GOALS",
      "nodeKind": "ENTITY_COLLECTION",
      "datasetCode": "KIDS_GOAL",
      "path": "/kids/goals"
    }
  ]
}
```

საიტის validation logic-ის იდეა:

```js
function validateDiscoveredContract(document, expected) {
  if (document.contractCode !== expected.contractCode) {
    throw new Error("Discovered contract code does not match site context");
  }

  if (document.revision !== expected.revision) {
    throw new Error("Discovered contract revision does not match site context");
  }

  if (document.status !== "APPROVED") {
    throw new Error("Only APPROVED contracts may drive a site");
  }

  if (!Array.isArray(document.pages)) {
    throw new Error("Contract discovery returned no page registry");
  }

  const ids = document.pages.map((page) => page.pageId);
  if (new Set(ids).size !== ids.length) {
    throw new Error("Contract page IDs are not unique");
  }
}
```

ამის შემდეგ იქმნება runtime contract registry:

```js
const contractRegistry = {
  contractCode: "KIDS_PORTAL_V1",
  revision: 8,
  status: "APPROVED",
  pages: new Map(document.pages.map((page) => [page.pageId, page])),
};
```

ამ ეტაპის შედეგია მხოლოდ დამტკიცებული page registry:

```json
{
  "status": "DISCOVERED",
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageCount": 6,
  "dataQueriesAllowed": false
}
```

თუ request დააბრუნებს `401`, `403`, `404`, სხვა revision-ს, `DRAFT`/`REVIEW_REQUIRED`
სტატუსს ან არავალიდურ page registry-ს:

```text
არ გაიგზავნოს capability request
არ გაიგზავნოს data query
არ ჩაირთოს ძველი endpoint ავტომატურად
გამოჩნდეს controlled contract error
დაიწეროს redacted audit telemetry
```

### Step 2 lifecycle

| Lifecycle event | Execute Step 2? | Reason |
|---|---:|---|
| successful authentication | yes | establishes page registry |
| first application load with valid session | yes | no runtime registry exists |
| route change | no | reuse valid registry |
| discovery cache expiry | yes | refresh contract state |
| contract revision change | yes | invalidate old registry |
| token refresh only | no | contract state remains valid |
| sign-out/sign-in | yes | new authenticated session |
| server returns non-approved status | stop | fail-closed governance |

Step 2-ის სტატუსია: **PROPOSED**.

თუ ამ discovery წესს ვეთანხმებით, შემდეგი ნაბიჯი იქნება **Step 3: Page
Capability Discovery** — კონკრეტული page-ის დაშვებული query შესაძლებლობების
მიღება.

## Step 3 — Page Capability Discovery

ამ ეტაპზე საიტმა უკვე იცის, რომელი page-ები არსებობს. ახლა თითოეული აქტიური
page-ისთვის ეკითხება Control Plane-ს, კონკრეტულად რა query შეუძლია ამ page-ს.
მონაცემი ჯერ არ მოითხოვება; მიიღება მხოლოდ capability schema.

საიტმა უნდა გაიგოს:

- რომელი field-ებია საჯაროდ ხელმისაწვდომი;
- რომელი filter/operator-ებია დაშვებული;
- რომელი sort key-ებია მხარდაჭერილი;
- რომელი relation/include-ებია დაშვებული;
- რომელი aggregation/groupBy შეიძლება;
- რა page size/cursor policy მოქმედებს;
- როგორი response shape ბრუნდება;
- რა quality/privacy policy უნდა დაკმაყოფილდეს.

request იგზავნება route-ის გააქტიურებისას, მაგრამ მხოლოდ Step 2-ის registry-დან
მიღებული `pageId`-ით:

```http
GET {API_BASE}/platform/contracts/{contractCode}/pages/{pageId}/query-capabilities?revision={revision}
Authorization: Bearer {accessToken}
Accept: application/json
Accept-Language: ka
X-Request-ID: {uuid}
```

KIDS-ის სტატისტიკის მაგალითი:

```text
GET /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/11/query-capabilities?revision=8
```

request code-ის იდეა:

```js
async function discoverPageCapabilities(siteContext, pageId, accessToken) {
  if (!siteContext.pages.has(pageId)) {
    throw new Error("Page is not declared by the approved contract");
  }

  const url = new URL(
    `${siteContext.apiBaseUrl}/platform/contracts/${encodeURIComponent(siteContext.contractCode)}`
      + `/pages/${pageId}/query-capabilities`,
  );
  url.searchParams.set("revision", String(siteContext.revision));

  const response = await fetch(url, {
    method: "GET",
    headers: {
      Accept: "application/json",
      "Accept-Language": "ka",
      Authorization: `Bearer ${accessToken}`,
      "X-Request-ID": crypto.randomUUID(),
    },
  });

  if (!response.ok) {
    throw new Error(`Page capability discovery failed: ${response.status}`);
  }

  const capabilities = await response.json();
  validatePageCapabilities(capabilities, siteContext, pageId);
  return capabilities;
}
```

სავარაუდო response shape:

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 11,
  "datasetCode": "KIDS_STATISTICAL_INPUT",
  "allowedFields": [
    "carrierCode",
    "metricCode",
    "unitCode",
    "period",
    "value",
    "dimensions"
  ],
  "filters": [
    {"field": "metricCode", "operators": ["EQ", "IN"]},
    {"field": "period", "operators": ["GTE", "LTE"]},
    {"field": "dimension.*", "operators": ["EQ", "IN"]}
  ],
  "relations": ["raw", "classifier", "lineage"],
  "aggregations": ["SUM", "AVG", "COUNT"],
  "groupBy": ["TIME_PERIOD", "AGE_GROUP"],
  "pagination": {
    "maxLimit": 1000,
    "cursor": "KEYSET"
  },
  "responseShape": "SERIES_WITH_OBSERVATIONS"
}
```

საიტის validation logic-ის იდეა:

```js
function validatePageCapabilities(capabilities, expected, pageId) {
  if (capabilities.contractCode !== expected.contractCode) {
    throw new Error("Capability contract mismatch");
  }

  if (capabilities.revision !== expected.revision) {
    throw new Error("Capability revision mismatch");
  }

  if (capabilities.pageId !== pageId) {
    throw new Error("Capability page mismatch");
  }

  if (!Array.isArray(capabilities.allowedFields)) {
    throw new Error("Capability field registry is missing");
  }

  if (!capabilities.responseShape) {
    throw new Error("Capability response shape is missing");
  }
}
```

ამის შემდეგ იქმნება page capability context:

```json
{
  "pageId": 11,
  "datasetCode": "KIDS_STATISTICAL_INPUT",
  "status": "CAPABILITIES_READY",
  "queryAllowed": true,
  "responseShape": "SERIES_WITH_OBSERVATIONS"
}
```

ეს არ ნიშნავს, რომ ნებისმიერი query დაშვებულია. პირიქით, მხოლოდ ამ capability
registry-ში გამოცხადებული field/operator/relation/aggregation შეიძლება შემდეგ
request-ში მოხვდეს.

თუ capability response-ში აღმოჩნდა უცნობი page, სხვა dataset, ცარიელი field
registry, დაუმტკიცებელი aggregation ან contract mismatch:

```text
არ შეიქმნას query form
არ გაიგზავნოს data query
არ მოხდეს UI-ში სავარაუდო field-ის დამატება
გამოჩნდეს controlled capability error
დაიწეროს redacted telemetry
```

### Step 3 lifecycle

| Lifecycle event | Execute Step 3? | Reason |
|---|---:|---|
| page route activation | yes | page-specific capabilities are needed |
| first visit to a page in session | yes | no capability context exists |
| same page re-render | no | reuse page/revision cache |
| route change to another page | yes for new page | capabilities are page-scoped |
| capability cache expiry | yes | refresh policy and field declarations |
| contract revision change | yes | old capabilities are invalid |
| token refresh only | no | authorization context remains valid |
| capability mismatch | stop | query construction is unsafe |

Step 3-ის სტატუსია: **PROPOSED**.

თუ ამ capability წესს ვეთანხმებით, შემდეგი ნაბიჯი იქნება **Step 4: Request
Construction** — capability-ებიდან კონკრეტული query body-ის უსაფრთხოდ აგება.

## Step 4 — Request Construction

ამ ეტაპზე საიტი იღებს მომხმარებლის ან page-ის საჭიროებას და capability
registry-ის მიხედვით აწყობს query body-ს. ეს არის pure construction ეტაპი:
request ჯერ არ იგზავნება API-ში.

საიტმა უნდა გააკეთოს:

- შეამოწმოს, რომ არჩეული field capability-ში არსებობს;
- შეამოწმოს, რომ operator ამ field-ზე დაშვებულია;
- შეამოწმოს, რომ relation/include გამოცხადებულია;
- შეამოწმოს aggregation/groupBy-ის თავსებადობა;
- გამოიყენოს მხოლოდ contract/page registry-დან მიღებული identity;
- დაადოს limit/filter/depth budget;
- არ მიიღოს table name, provider name ან SQL fragment მომხმარებლისგან.

request builder-ის იდეა:

```js
function buildQueryRequest({ siteContext, pageId, capabilities, intent }) {
  assertPageCapability(capabilities, pageId);

  const filters = (intent.filters || []).map((filter) => {
    const declaration = capabilities.filters.find(
      (item) => item.field === filter.field,
    );

    if (!declaration || !declaration.operators.includes(filter.operator)) {
      throw new Error(`Filter is not declared: ${filter.field}/${filter.operator}`);
    }

    return {
      field: filter.field,
      operator: filter.operator,
      value: filter.value,
    };
  });

  const select = (intent.select || []).filter((field) => {
    if (!capabilities.allowedFields.includes(field)) {
      throw new Error(`Field is not declared: ${field}`);
    }
    return true;
  });

  const include = (intent.include || []).filter((relation) => {
    if (!capabilities.relations.includes(relation)) {
      throw new Error(`Relation is not declared: ${relation}`);
    }
    return true;
  });

  return Object.freeze({
    contractCode: siteContext.contractCode,
    revision: siteContext.revision,
    pageId,
    select,
    filters,
    include,
    groupBy: validateGroupBy(intent.groupBy, capabilities),
    aggregation: validateAggregation(intent.aggregation, capabilities),
    limit: boundedLimit(intent.limit, capabilities.pagination.maxLimit),
    cursor: intent.cursor || undefined,
  });
}
```

### მაგალითი — goals page

მომხმარებლის intent:

```json
{
  "select": ["goalId", "categoryCode", "title"],
  "include": ["resources"],
  "limit": 50
}
```

შექმნილი body:

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 8,
  "select": ["goalId", "categoryCode", "title"],
  "include": ["resources"],
  "limit": 50
}
```

### მაგალითი — glossary page

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 10,
  "filters": [
    {"field": "languageCode", "operator": "EQ", "value": "ka"}
  ],
  "select": ["entryId", "label", "definition"],
  "limit": 100
}
```

### მაგალითი — statistics page

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 11,
  "select": ["period", "value", "metricCode", "dimensions"],
  "filters": [
    {"field": "metricCode", "operator": "EQ", "value": "KIDS_OBS_VALUE_INPUT"},
    {"field": "period", "operator": "GTE", "value": "2020"}
  ],
  "include": ["raw", "classifier", "lineage"],
  "groupBy": ["TIME_PERIOD", "AGE_GROUP"],
  "aggregation": {"function": "SUM", "field": "value"},
  "limit": 100
}
```

ამ ეტაპზე `include: ["raw", "statistics", "classifier"]` პირდაპირ არ
ნიშნავს სამი ფიზიკური ცხრილის ხელით join-ს. ეს არის contract-declared
relation intent; relation graph-ს შემდეგ API execution layer ასრულებს.

თუ request builder შეხვდება დაუშვებელ field-ს, operator-ს, relation-ს,
aggregation-ს, ზედმეტ limit-ს ან არასწორ cursor-ს:

```text
request არ შეიქმნას
API არ გამოიძახოს
UI-მ არ დამალოს შეცდომა და არ ჩაანაცვლოს field ვარაუდით
დაბრუნდეს typed validation error
დაიწეროს redacted telemetry
```

### Step 4 lifecycle

| Lifecycle event | Execute Step 4? | Reason |
|---|---:|---|
| user submits a filter/query | yes | construct a new bounded request |
| initial page load with default query | yes | create declared default intent |
| filter/sort/include change | yes | query fingerprint changes |
| page re-render without intent change | no | reuse immutable request |
| next cursor request | yes | construct continuation with same fingerprint |
| capability revision change | invalidate | rebuild from new declarations |
| validation failure | stop | no unsafe request may be sent |

Step 4-ის სტატუსია: **PROPOSED**.

თუ ამ construction წესს ვეთანხმებით, შემდეგი ნაბიჯი იქნება **Step 5: Contract
Query Execution** — უკვე აშენებული request-ის გაგზავნა და API-ის მიერ მისი
ხელახლა შემოწმება.

## Step 5 — Contract Query Execution

ამ ეტაპზე საიტი აგზავნის Step 4-ში აგებულ, capability-ით შემოწმებულ request-ს.
ეს არის პირველი ეტაპი, სადაც API იწყებს მონაცემთა წაკითხვის execution-ს.

საიტმა უნდა გამოიყენოს მხოლოდ platform request client. კომპონენტმა პირდაპირ
`fetch` ან SQL-like URL არ უნდა ააგოს.

request:

```http
POST {API_BASE}/platform/contracts/{contractCode}/pages/{pageId}/query
Authorization: Bearer {accessToken}
Content-Type: application/json
Accept: application/json
Accept-Language: ka
X-Request-ID: {uuid}
```

KIDS statistics-ის მაგალითი:

```text
POST /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/11/query
```

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 11,
  "select": ["period", "value", "metricCode", "dimensions"],
  "filters": [
    {"field": "metricCode", "operator": "EQ", "value": "KIDS_OBS_VALUE_INPUT"}
  ],
  "include": ["raw", "classifier", "lineage"],
  "groupBy": ["TIME_PERIOD", "AGE_GROUP"],
  "aggregation": {"function": "SUM", "field": "value"},
  "limit": 100
}
```

API-ის execution sequence:

```text
1. authenticate token
2. resolve APPROVED contract revision
3. compile request against declared fields/capabilities
4. enforce tenant/site authorization
5. calculate and admit query cost
6. resolve provider/schema/table mapping from metadata
7. execute bounded physical query
8. execute declared relation graph/includes
9. apply projection, aggregation and response serializer
10. attach lineage, pagination, query cost and ETag
11. return contract-shaped response
```

საიტის მხარეს request code-ის იდეა:

```js
async function executePageQuery(requestBody, accessToken, signal) {
  const response = await platformRequestClient.post(
    `/platform/contracts/${encodeURIComponent(requestBody.contractCode)}`
      + `/pages/${requestBody.pageId}/query`,
    requestBody,
    {
      accessToken,
      signal,
      headers: {
        "Accept-Language": "ka",
        "X-Request-ID": crypto.randomUUID(),
      },
    },
  );

  if (response.status === 304) return { notModified: true };
  if (!response.ok) throw await toTypedPlatformError(response);
  return response.json();
}
```

სავარაუდო response shape:

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "contractRevision": 8,
  "pageId": 11,
  "datasetCode": "KIDS_STATISTICAL_INPUT",
  "responseShape": "SERIES_WITH_OBSERVATIONS",
  "data": [
    {
      "period": "2024",
      "value": 1234,
      "metricCode": "KIDS_OBS_VALUE_INPUT",
      "dimensions": {
        "AGE_GROUP": "0-17"
      }
    }
  ],
  "relations": {
    "raw": [],
    "classifier": [],
    "lineage": []
  },
  "pagination": {
    "limit": 100,
    "nextCursor": null,
    "hasMore": false
  },
  "meta": {
    "queryCost": 12,
    "snapshotId": "<immutable-snapshot-id>"
  }
}
```

საიტი ამ ეტაპზე მხოლოდ response envelope-ს იღებს. საბოლოო view-model,
Highcharts series, localized labels და download actions შემდეგ adapter layer-ში
იქმნება; კომპონენტმა არ უნდა ივარაუდოს response-ის შიდა field-ები.

თუ API დააბრუნებს:

```text
401  → token refresh და მხოლოდ ერთი controlled retry
403  → authorization error; query არ განმეორდეს ავტომატურად
409  → contract/revision conflict; restart at Step 2
422  → request validation error; UI-მ შეცდომა კონკრეტულ field-ზე აჩვენოს
429  → query budget/quota exceeded; retry-after დაიცვას
5xx  → bounded retry policy ან controlled error state
304  → cached response გამოიყენოს
```

API-ის მიერ response-ის დაბრუნება არ ნიშნავს publication-ს. საიტი იღებს მხოლოდ
იმ snapshot/data view-ს, რომელიც contract-ისა და publication policy-ის მიხედვით
ხელმისაწვდომია.

### Step 5 lifecycle

| Lifecycle event | Execute Step 5? | Reason |
|---|---:|---|
| initial page query | yes | obtain the page data |
| filter/sort/include change | yes | new validated query |
| page re-render | no | reuse request/result cache |
| valid next cursor | yes | continue same keyset query |
| `401` after token expiry | once after refresh | controlled retry only |
| `409` revision conflict | no; restart Step 2 | old request is invalid |
| `429` quota response | no immediate loop | respect server retry policy |
| response shape mismatch | stop | adapter/schema defect must be fixed |

Step 5-ის სტატუსია: **PROPOSED**.

თუ ამ execution წესს ვეთანხმებით, შემდეგი ნაბიჯი იქნება **Step 6: Response
Handoff and View-Model Mapping** — contract response-ის უსაფრთხო გადაყვანა UI-ს
მოსახმარ ფორმაში.

## Step 6 — Response Handoff and View-Model Mapping

ამ ეტაპზე API-მ უკვე დააბრუნა contract-shaped response. საიტი ჯერ არ აძლევს ამ
ობიექტს პირდაპირ UI კომპონენტს. ჯერ გადის response validation-ს და შემდეგ
გადადის versioned domain adapter-ში.

საიტმა უნდა შეამოწმოს:

- contract code და revision ემთხვევა runtime context-ს;
- pageId და datasetCode ემთხვევა capability registry-ს;
- `responseShape` მხარდაჭერილია ამ adapter-ის მიერ;
- `data` არის გამოცხადებული collection/object ფორმატში;
- pagination/cursor ტიპები სწორია;
- relation/include keys მხოლოდ მოთხოვნილს შეიცავს;
- lineage/quality/privacy metadata არ იკარგება;
- უცნობი დამატებითი ველები არ არღვევს forward compatibility-ს.

response validation-ის იდეა:

```js
function validatePlatformResponse(response, expected) {
  if (response.contractCode !== expected.contractCode) {
    throw new Error("Response contract mismatch");
  }

  if (response.contractRevision !== expected.revision) {
    throw new Error("Response revision mismatch");
  }

  if (response.pageId !== expected.pageId) {
    throw new Error("Response page mismatch");
  }

  if (response.datasetCode !== expected.datasetCode) {
    throw new Error("Response dataset mismatch");
  }

  if (!response.responseShape || !Object.hasOwn(response, "data")) {
    throw new Error("Response envelope is incomplete");
  }
}
```

ამის შემდეგ იქმნება immutable view-model. ეს არის anti-corruption layer —
API-ს canonical field names და არსებული UI-ის ძველი field names ერთმანეთში არ
ირევა.

### მაგალითი — goals adapter

```js
function mapGoalsResponse(response, language) {
  return response.data.map((item) => ({
    id: item.goalId,
    category: item.categoryCode,
    title: item.title?.[language] || item.title?.ka || "",
    resources: (response.relations?.resources || [])
      .filter((resource) => resource.goalId === item.goalId)
      .map((resource) => ({
        id: resource.resourceId,
        title: resource.title?.[language] || resource.title?.ka || "",
        href: resource.payloadPointer,
      })),
  }));
}
```

### მაგალითი — glossary adapter

```js
function mapGlossaryResponse(response, language) {
  return response.data.map((entry) => ({
    id: entry.entryId,
    text: entry.label?.[language] || entry.label?.ka || "",
    definition: entry.definition?.[language] || entry.definition?.ka || "",
  }));
}
```

### მაგალითი — statistics adapter

```js
function mapStatisticsResponse(response, language) {
  return {
    metric: response.metric,
    unit: response.unit,
    aggregation: response.aggregation,
    dimensions: response.dimensions,
    observations: response.data.map((observation) => ({
      period: observation.period,
      value: observation.value,
      dimensions: observation.dimensions,
      label: observation.label?.[language],
    })),
    lineage: response.relations?.lineage || [],
    pagination: response.pagination,
  };
}
```

ადაპტერი არ უნდა აკეთებდეს:

```text
არ შექმნას SQL
არ მოძებნოს physical table
არ ააგოს payload path ხელით
არ დაასკვნას metric/unit/aggregation სახელიდან
არ წაშალოს lineage ან quality metadata
არ ჩაანაცვლოს null მნიშვნელობა ვარაუდით
```

თუ response ვერ გაივლის validation-ს ან adapter-ს არ აქვს შესაბამისი
`responseShape`:

```text
UI-ში არ მოხვდეს ნაწილობრივ არასწორი data
შედეგი გადავიდეს controlled data-contract error-ში
დაიწეროს redacted telemetry
შენარჩუნდეს raw response მხოლოდ diagnostic boundary-ში
არ ჩაირთოს ძველი response parser ავტომატურად production-ში
```

### Step 6 lifecycle

| Lifecycle event | Execute Step 6? | Reason |
|---|---:|---|
| successful query response | yes | validate and map response |
| `304 Not Modified` | no full remap | reuse validated view-model cache |
| same response replay | no | response fingerprint is unchanged |
| new language | yes | localized view-model changes |
| new page/query | yes | different adapter context |
| response-shape revision change | yes | select versioned adapter |
| validation failure | stop | never render untrusted data |

Step 6-ის სტატუსია: **PROPOSED**.

თუ ამ response handoff წესს ვეთანხმებით, შემდეგი ნაბიჯი იქნება **Step 7:
Pagination and Continuation** — server-issued cursor-ით უსაფრთხო გაგრძელება.

## Step 7 — Pagination and Continuation

ამ ეტაპზე საიტი უკვე იღებს validated view-model-ს, მაგრამ თუ response სრულად
არ დაეტია ერთ გვერდზე, შემდეგი გვერდი უნდა მოითხოვოს მხოლოდ API-ის მიერ
გაცემული cursor-ით.

საიტმა უნდა დაიცვას:

- contract-ში გამოცხადებული stable sort key;
- უცვლელი filter/include/select/groupBy fingerprint;
- server-issued cursor-ის opaque მნიშვნელობა;
- იგივე contract code, revision და pageId;
- contract-ის `maxLimit` და include limits;
- forward/backward მიმართულება მხოლოდ capability-ის დაშვებისას.

response-ში cursor-ის მაგალითი:

```json
{
  "pagination": {
    "limit": 100,
    "hasMore": true,
    "nextCursor": "KS.eyJjb250cmFjdCI6IktJRFNfUE9SVEFMX1YxIiwi...",
    "previousCursor": null,
    "sort": ["period", "observationId"],
    "snapshotId": "snap-2026-09-15-001"
  }
}
```

შემდეგი request აგებულია იგივე query-ისგან, მხოლოდ cursor ემატება:

```js
function buildContinuationIntent(previousRequest, pagination, direction = "forward") {
  const cursor = direction === "backward"
    ? pagination.previousCursor
    : pagination.nextCursor;

  if (!cursor) return null;

  return Object.freeze({
    ...previousRequest,
    cursor,
    direction,
  });
}
```

შემდეგ ეს intent ისევ გადის Step 4-ის construction validation-ს და Step 5-ის
contract query execution-ს. საიტმა cursor-ში encoded values არ უნდა გაშიფროს,
შეცვალოს ან ხელით შექმნას.

მაგალითად, სტატისტიკის შემდეგი გვერდი:

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 11,
  "filters": [
    {"field": "metricCode", "operator": "EQ", "value": "KIDS_OBS_VALUE_INPUT"}
  ],
  "orderBy": [
    {"field": "period", "direction": "ASC"},
    {"field": "observationId", "direction": "ASC"}
  ],
  "cursor": "KS.<opaque-server-issued-value>",
  "direction": "forward",
  "limit": 100
}
```

API ამოწმებს cursor-ში:

```text
contract code/revision/pageId
query fingerprint
stable sort tuple
snapshot binding
cursor expiry/signature
direction policy
```

თუ რომელიმე მათგანი შეიცვალა:

```text
cursor არ მიიღოს
დაბრუნდეს typed cursor-conflict error
საიტმა თავიდან დაიწყოს Step 2 ან Step 4 შესაბამისი მიზეზის მიხედვით
არ მოხდეს გვერდის ნომერზე უხილავი fallback
```

### Step 7 lifecycle

| Lifecycle event | Execute Step 7? | Reason |
|---|---:|---|
| initial response has `hasMore=true` | yes when user requests next page | continue the same query |
| next-page button/intersection trigger | yes | obtain next cursor result |
| filter/sort/include change | no direct continuation | old cursor is invalid; restart at Step 4 |
| language-only change | reuse cursor only if contract semantics permit | labels may be remapped locally |
| cursor expiry | no retry loop | restart query from first page |
| snapshot expiration | no | request a new snapshot/query |
| concurrent insert/update conflict | follow server conflict policy | never fabricate missing rows |
| no `nextCursor` | stop | dataset is exhausted |

Step 7-ის სტატუსია: **PROPOSED**.

თუ ამ pagination წესს ვეთანხმებით, შემდეგი ნაბიჯი იქნება **Step 8: Governed
Export** — მხოლოდ contract-ის მიერ დაშვებული projection-ის ექსპორტი.

## Step 8 — Governed Export

ამ ეტაპზე საიტი ითხოვს მონაცემების ფაილად გატანას. Export არის ცალკე governed
ოპერაცია და არ არის ჩვეულებრივი query response-ის გვერდის ავლა.

საიტმა ჯერ უნდა გადაამოწმოს capability-ში:

- export მხარდაჭერილია თუ არა;
- რომელი format-ებია დაშვებული (`JSON`, `CSV`, `SDMX`, `Parquet`, `ZIP`);
- რომელი field/relation/projection შეიძლება;
- მაქსიმალური row/byte limit;
- საჭიროა თუ არა asynchronous operation;
- რომელი privacy/quality policy მოქმედებს.

export request:

```http
POST {API_BASE}/platform/operations/exports
Authorization: Bearer {accessToken}
Content-Type: application/json
Accept: application/json
X-Request-ID: {uuid}
Idempotency-Key: {stable-export-key}
```

მაგალითი:

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 11,
  "format": "PARQUET",
  "projection": [
    "period",
    "metricCode",
    "value",
    "dimensions"
  ],
  "filters": [
    {"field": "metricCode", "operator": "EQ", "value": "KIDS_OBS_VALUE_INPUT"}
  ],
  "include": ["lineage"],
  "limit": 100000
}
```

request builder-ის იდეა:

```js
async function requestExport(exportIntent, accessToken) {
  if (!exportIntent.capabilities.exportFormats.includes(exportIntent.format)) {
    throw new Error("Export format is not declared by page capabilities");
  }

  const response = await platformRequestClient.post(
    "/platform/operations/exports",
    exportIntent.body,
    {
      accessToken,
      headers: {
        "X-Request-ID": crypto.randomUUID(),
        "Idempotency-Key": exportIntent.idempotencyKey,
      },
    },
  );

  if (!response.ok) throw await toTypedPlatformError(response);
  return response.json();
}
```

პირველი response არის operation receipt:

```json
{
  "operationId": "op_01J...",
  "status": "ACCEPTED",
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 11,
  "format": "PARQUET",
  "expiresAt": "<UTC timestamp>"
}
```

შემდეგ საიტი operation status-ს ამოწმებს:

```http
GET {API_BASE}/platform/operations/{operationId}
Authorization: Bearer {accessToken}
Accept: application/json
X-Request-ID: {uuid}
```

დასრულებული ოპერაციის response:

```json
{
  "operationId": "op_01J...",
  "status": "SUCCEEDED",
  "result": {
    "downloadUrl": "<short-lived-governed-url>",
    "contentType": "application/vnd.apache.parquet",
    "byteSize": 48231,
    "checksum": "sha256:<digest>",
    "snapshotId": "snap-2026-09-15-001"
  }
}
```

საიტმა უნდა გამოიყენოს მხოლოდ short-lived governed URL, შეამოწმოს checksum და
არ შეინახოს export credential ან მუდმივი object-storage URL. Export-ის შედეგი
არ უნდა ჩაითვალოს publication-ად; ის მხოლოდ approved snapshot-ის წაკითხვადია.

თუ export დააბრუნებს:

```text
403  → privacy/authorization denial; no retry
409  → contract revision or snapshot conflict; restart from Step 2
413  → export too large; reduce declared projection/limit
429  → quota exceeded; obey Retry-After
422  → projection/format not declared; correct intent
5xx  → bounded retry for the same Idempotency-Key
```

### Step 8 lifecycle

| Lifecycle event | Execute Step 8? | Reason |
|---|---:|---|
| user requests export | yes | create governed operation |
| duplicate click with same intent | no new operation | reuse Idempotency-Key |
| operation `ACCEPTED`/`RUNNING` | poll status | do not submit duplicates |
| operation `SUCCEEDED` | download once | verify checksum and expiry |
| operation `FAILED` | stop or explicit retry | preserve failure evidence |
| token refresh | continue with re-authenticated poll | operation ID remains opaque |
| contract revision change | cancel/stop | export binding is revision-specific |
| expired download URL | request a new governed URL | never reconstruct object path |

Step 8-ის სტატუსია: **PROPOSED**.

თუ ამ export წესს ვეთანხმებით, შემდეგი ნაბიჯი იქნება **Step 9: Failure,
Re-bootstrap and Session Lifecycle** — მთლიან flow-ში შეცდომის, token refresh-ის
და contract revision-ის ცვლილების მართვა.

## Step 9 — Failure, Re-bootstrap and Session Lifecycle

ამ ეტაპზე განისაზღვრება, როგორ იქცევა საიტი მაშინ, როცა request, token,
contract, provider, network ან response ვერ აკმაყოფილებს გამოცხადებულ წესებს.
მიზანია fail-closed ქცევა: გაურკვეველი ან არასანდო მდგომარეობა არ უნდა
გადაიქცეს ცრუ მონაცემად.

საიტის შეცდომების pipeline:

```text
HTTP/network error
  ↓
typed platform error
  ↓
retry policy decision
  ↓
recover / refresh / re-bootstrap / stop
  ↓
redacted telemetry + user-safe message
```

### ძირითადი ქცევები

```js
async function handlePlatformFailure(error, context) {
  if (error.status === 401 && !context.retriedAfterRefresh) {
    await auth.refreshToken();
    return { action: "RETRY_ONCE" };
  }

  if (error.status === 409 || error.code === "CONTRACT_REVISION_CONFLICT") {
    runtimeStore.clearCapabilities();
    runtimeStore.clearContractPages();
    return { action: "REBOOTSTRAP_FROM_STEP_2" };
  }

  if (error.status === 429) {
    return { action: "WAIT_RETRY_AFTER" };
  }

  if (error.status >= 500 || error.code === "NETWORK_ERROR") {
    return { action: "BOUNDED_RETRY_OR_CIRCUIT_OPEN" };
  }

  return { action: "CONTROLLED_STOP" };
}
```

### რა არ უნდა გააკეთოს საიტმა

```text
არ გამოიყენოს ძველი response როგორც ახალი contract-ის პასუხი
არ დააბრუნოს ცარიელი/ნაწილობრივი data წარმატებულ პასუხად
არ გაიმეოროს POST უსასრულოდ
არ განაახლოს token ყოველი შეცდომისას უცნობი მიზეზით
არ დამალოს 403/422 contract ან security error
არ შეინახოს raw token ან secret telemetry-ში
არ გააგრძელოს ძველი revision-ით conflict-ის შემდეგ
```

### Re-bootstrap sequence

თუ contract revision შეიცვალა ან runtime registry გაუქმდა, საიტი იწყებს:

```text
Step 0 — validate environment again
Step 1 — validate/refresh authentication
Step 2 — rediscover approved contract/pages
Step 3 — rediscover page capabilities
Step 4 — rebuild current intent
Step 5 — execute a new query
Step 6 — validate/map the new response
```

მიმდინარე route და მომხმარებლის intent შეიძლება შენარჩუნდეს მხოლოდ მაშინ, თუ
ახალი capability იგივე field/operator semantics-ს აცხადებს. სხვა შემთხვევაში
ფორმა reset-დება და მომხმარებელს ეცნობება ცვლილება.

### Session lifecycle

```js
async function startApplicationSession() {
  const runtime = loadSiteRuntimeConfig();       // Step 0
  const auth = await bootstrapAuthentication();  // Step 1
  const contract = await discoverPages(runtime, auth); // Step 2
  runtimeStore.initialize({ runtime, auth, contract });
  return runtimeStore;
}

function endApplicationSession() {
  runtimeStore.clearCapabilities();
  runtimeStore.clearContractPages();
  runtimeStore.clearQueries();
  auth.clearRuntimeToken();
}
```

### Observability and audit

ყოველი მნიშვნელოვანი failure/recovery event ტოვებს მხოლოდ redacted ინფორმაციას:

```json
{
  "event": "contract_query_failure",
  "site": "kids",
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 11,
  "status": 422,
  "requestId": "<uuid>",
  "retryAction": "CONTROLLED_STOP"
}
```

არ ჩაიწეროს token, cookie, secret, raw PII, SQL ან payload-ის სრული შინაარსი.

### Step 9 lifecycle

| Lifecycle event | Action |
|---|---|
| first load | run Steps 0–2 |
| route activation | run Step 3, then 4–6 |
| query failure | apply typed retry/stop policy |
| token expiry | refresh once, then re-authenticate |
| contract conflict | clear registry and restart at Step 2 |
| sign-out | clear all runtime state |
| tab close/session expiry | discard token and transient cache |
| app upgrade | restart at Step 0 |

Step 9-ის სტატუსია: **PROPOSED**.

ამით runtime request lifecycle დასრულებულია. ყველა ნაბიჯი გადადის
`AGREED → IMPLEMENTED → VERIFIED` მდგომარეობაში მხოლოდ ცალკე შეთანხმებისა და
evidence-ის შემდეგ.

#### Step 0 lifecycle

| Lifecycle event | Execute Step 0? | Reason |
|---|---:|---|
| first page load / hard refresh | yes | establishes a new runtime context |
| browser tab restored from discard | yes | memory/runtime state may be gone |
| client-side route change | no | the same site context remains valid |
| React component render/mount | no | prevents duplicate bootstrap work |
| OIDC token refresh | no | only authentication state changes |
| contract revision change detected | yes, then continue at Step 2 | invalidates the runtime context |
| explicit sign-out/sign-in | yes | creates a new authenticated session |
| service worker/app version upgrade | yes | validates the new build's environment |

The bootstrap promise is single-flight: concurrent components await the same
startup result rather than executing the validation more than once. Its result
is held in an in-memory runtime store and is never treated as a persistent
source of contract truth.

### Step 1 — authentication bootstrap

**When:** immediately after environment validation.  
**Owner:** auth adapter.  
**Action:** obtain an OIDC access token with the approved `READ_RESOURCE`
scope/role and the correct site/tenant claims. Store it only in the approved
runtime mechanism. A token is never placed in a URL or committed environment
file.

### Step 2 — contract/page discovery

**When:** once per session or when the discovery cache expires.  
**Owner:** platform request client.  
**Request:**

```http
GET {API_BASE}/platform/contracts/{contractCode}/pages?revision={revision}
Authorization: Bearer {token}
Accept: application/json
Accept-Language: ka
X-Request-ID: {uuid}
```

**Use:** validate that the response is `APPROVED`, has the requested revision,
and contains unique page IDs and declared datasets. Store the result in a
runtime contract store; do not render data yet.

### Step 3 — page capability discovery

**When:** on route activation, once per page/revision and until capability TTL
expires.  
**Owner:** page controller through the platform client.  
**Request:**

```http
GET {API_BASE}/platform/contracts/{contractCode}/pages/{pageId}/query-capabilities?revision={revision}
Authorization: Bearer {token}
Accept: application/json
X-Request-ID: {uuid}
```

**Use:** configure allowed fields, filters, relations, includes, ordering,
aggregation, pagination and response shape. Unsupported controls are hidden;
they are never sent speculatively.

### Step 4 — request construction

**When:** after capability validation and a user/application data intent.  
**Owner:** page-specific adapter, not a visual component.  
**Action:** construct a request only from capability-declared field and
operator names. The request contains contract code/revision, page identity,
bounded filters, selected fields, declared includes and pagination.

### Step 5 — contract query

**When:** after Step 4 succeeds.  
**Owner:** platform request client.  
**Request:**

```http
POST {API_BASE}/platform/contracts/{contractCode}/pages/{pageId}/query
Authorization: Bearer {token}
Content-Type: application/json
Accept: application/json
X-Request-ID: {uuid}
```

The API recompiles and authorizes the request before any data-plane access.
The browser never submits a table name, provider name or SQL fragment.

### Step 6 — response handoff (later agreed phase)

The raw response is handed to a versioned adapter which validates the declared
response shape, maps localized labels/resources/observations to a view-model,
and preserves lineage, quality and pagination metadata. No response mapping is
implemented in the current preparation phase.

### Step 7 — pagination and continuation

The adapter uses only a server-issued cursor or the contract-declared page
fields. It never calculates a cursor from UI indexes and never changes sort
keys between requests. A continuation request repeats Steps 4–5 with the
verified cursor and the same query fingerprint.

### Step 8 — export (optional)

**When:** only when the page capability declares export.  
**Request:** `POST {API_BASE}/platform/operations/exports` with contract code,
revision, page ID and a bounded projection. The response is an operation ID;
the client polls the operation endpoint and downloads only the governed result.

### Step 9 — failure and re-bootstrap

`401` triggers token refresh and a single controlled retry. `403`, contract
mismatch, capability mismatch, quality/privacy denial or timeout does not
fall back silently; the page enters a controlled error state and emits a
redacted telemetry event. A changed revision invalidates the runtime contract
cache and restarts at Step 2.
