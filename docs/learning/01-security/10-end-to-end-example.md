# სრული მაგალითი: pageId=8 Goals

## 1. Site startup

Frontend კითხულობს `KIDS_PORTAL_V1`, revision `8` და auth mode `oidc`. თუ რომელიმე აკლია, API request არ იგზავნება.

## 2. Discovery

Client issuer-იდან იგებს authorization და token endpoint-ს. ამავე discovery document-იდან იძებნება `jwks_uri`.

## 3. Login

PKCE ქმნის verifier/challenge წყვილს. Keycloak user-ს ამოწმებს და callback-ზე აბრუნებს ერთჯერად code-ს.

## 4. Token

Client code-სა და verifier-ს ცვლის RS256 access token-ზე. token-ში მოსალოდნელია:

```json
{
  "iss": "http://keycloak:8080/realms/geostat",
  "aud": "geostat-api",
  "sub": "<opaque-subject>",
  "exp": 0,
  "roles": ["contract.read"],
  "tenant": "kids-staging"
}
```

## 5. API request

```http
GET /api/v1/platform/contracts/KIDS_PORTAL_V1/pages/8/query-capabilities?revision=8
Authorization: Bearer <access-token>
```

API ჯერ ამოწმებს token-ს, შემდეგ policy-ს, შემდეგ contract-ს.

## 6. Query

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 8,
  "projection": "default",
  "select": ["id", "title", "path", "category"],
  "page": {"limit": 5}
}
```

## 7. Response

Response shape contract-იდან გენერირდება. API არ აბრუნებს arbitrary DB row-ს; აბრუნებს გამოცხადებულ projection-სა და lineage-ს.

## 8. Failure cases

```text
missing token       → 401
wrong signature     → 401
expired token       → 401
wrong tenant        → 403/404 policy
unapproved revision → deny
unknown field       → validation error
```

## 9. Acceptance

სრული წარმატება მხოლოდ მაშინ გვაქვს, როცა identity, authorization, contract introspection, query, response და evidence ერთ replay-ში PASS-ია.
